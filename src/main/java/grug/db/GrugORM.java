package grug.db;

import grug.db.GrugORM.Interfaces.GrugLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrugORM {

    public static final String SQL_VARS_PATTERN = "(:[a-zA-Z_]+)";
    private static GrugORM DEFAULT_ORM = null;
    private static final ThreadLocal<ConnectionInfo> CURRENT_CONNECTION = new ThreadLocal<>();

    private Callable<Connection> connectionSource = null;

    // default to a stdout logger @ INFO
    private GrugLogger.Level internalLoggerLevel = GrugLogger.Level.INFO;
    private GrugLogger logger = new DefaultLogger();

    private final ConcurrentHashMap<Class, DBMetaData> metadataCache = new ConcurrentHashMap<Class, DBMetaData>();

    //====================================================================
    // constructors & builders
    //====================================================================

    public GrugORM(Callable<Connection> connectionSource) {
        this.connectionSource = connectionSource;
    }

    public GrugORM(String connectionString) {
        this(() -> DriverManager.getConnection(connectionString));
    }

    public GrugORM withLogger(GrugLogger logger) {
        // custom loggers get everything by default
        this.withLogLevel(GrugLogger.Level.TRACE)
                .logger = logger;
        return this;
    }

    public GrugORM withLogLevel(Object level) {
        internalLoggerLevel = GrugLogger.Level.valueOf(String.valueOf(level));
        return this;
    }

    public GrugORM withMigrations(Migrations migrations) {
        migrations.setORM(this);
        return this;
    }

    public GrugORM makeDefaultORM() {
        setDefaultORM(this);
        return this;
    }

    //====================================================================
    // default orm management
    //====================================================================

    public static GrugORM getDefaultOrThrow() {
        GrugORM defaultORM = getDefault();
        if (defaultORM == null) {
            throw new IllegalStateException("No default GrugORM found");
        }
        return defaultORM;
    }

    public static GrugORM getDefault() {
        return DEFAULT_ORM;
    }

    public static void setDefaultORM(GrugORM orm) {
        DEFAULT_ORM = orm;
    }


    //====================================================================
    // Connection management
    //====================================================================

    private record ConnectionInfo(Connection conn, AtomicInteger count,
                                  ConnectionInfo previous) implements AutoCloseable {
        public void increment() {
            count.incrementAndGet();
        }

        @Override
        public void close() throws Exception {
            int val = count.decrementAndGet();
            if (val == 0) {
                conn.close();
                CURRENT_CONNECTION.set(this.previous());
            }
        }
    }

    public Connection getNewConnection() {
        try {
            return connectionSource.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private ConnectionInfo getConnectionInfo() {
        ConnectionInfo connectionInfo = CURRENT_CONNECTION.get();
        if (connectionInfo == null) {
            Connection connection = getNewConnection();
            connectionInfo = new ConnectionInfo(connection, new AtomicInteger(0), null);
            CURRENT_CONNECTION.set(connectionInfo);
        }
        connectionInfo.increment();
        return connectionInfo;
    }

    //====================================================================
    // Database interaction
    //====================================================================

    public long insert(Object object) {
        Class<?> clazz = object.getClass();
        DBMetaData metaData = getDBMetaData(clazz);
        Map<String, Object> values = metaData.asDBMap(object);
        return insert(metaData.getTableName(), values);
    }

    public <T> T find(Class<T> clazz, Object pk) {
        DBMetaData dbMetaData = getDBMetaData(clazz);
        return find(clazz, dbMetaData.getIdColumnName(), pk);
    }

    public <T> T find(Class<T> clazz, String key, Object val) {
        DBMetaData dbMetaData = getDBMetaData(clazz);
        try (ConnectionInfo ci = getConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + dbMetaData.getTableName() + " WHERE " + key + "=?");
            int parameterIndex = 1;
            setValueForQuery(ps, parameterIndex, val);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                T obj = dbMetaData.newObjectFromResult(resultSet);
                return obj;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> ResultList<T> findAll(Class<T> clazz) {
        return findAll(clazz, "true=true", Map.of());
    }

    public <T> ResultList<T> findAll(Class<T> clazz, String whereClause, Map<String, Object> values) {
        String name = clazz.getSimpleName();
        String tableName = snakeCase(name);
        return select(clazz, "SELECT * FROM " + tableName + " WHERE " + whereClause, values);
    }

    public <T> ResultList<T> select(Class<T> clazz, String sql, Map<String, Object> args) {
        try (ConnectionInfo ci = getConnectionInfo()) {
            Connection conn = ci.conn;
            ArrayList<Object> vals = new ArrayList<>();
            String updatedSql = updateSqlVars(sql, args, vals);
            logger.log(GrugLogger.Level.INFO, "Select SQL: {}\n  Args:{}", updatedSql, vals);
            PreparedStatement ps = conn.prepareStatement(updatedSql);
            for (int i = 0; i < vals.size(); i++) {
                Object val = vals.get(i);
                setValueForQuery(ps, i + 1, val);
            }
            ResultSet resultSet = ps.executeQuery();
            ResultList<T> result = new ResultList<>();
            while (resultSet.next()) {
                DBMetaData dbMetaData = getDBMetaData(clazz);
                T object = dbMetaData.newObjectFromResult(resultSet);
                result.add(object);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String updateSqlVars(String sql, Map<String, Object> args, List<Object> argList) {
        Pattern compile = Pattern.compile(SQL_VARS_PATTERN);
        Matcher matcher = compile.matcher(sql);
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int end;
        while (matcher.find()) {
            String match = matcher.group().substring(1);
            if (args.containsKey(match)) {
                end = matcher.start();
                sb.append(sql.substring(start, end));
                sb.append("?");
                start = matcher.end();
                argList.add(args.get(match));
            } else {
                throw new IllegalArgumentException("No value found for variable " + match + " in " + args);
            }
        }
        sb.append(sql.substring(start));
        return sb.toString();
    }

    public void save(Object object) {
        Class<?> clazz = object.getClass();
        DBMetaData dbMetaData = getDBMetaData(clazz);
        String tableName = dbMetaData.getTableName();
        String keyCol = dbMetaData.getIdColumnName();
        Map<String, Object> valuesToUpdate = dbMetaData.asDBMap(object);
        Object keyVal = valuesToUpdate.remove(keyCol); // remove the key
        if (keyVal == null) {
            insert(tableName, valuesToUpdate);
        } else {
            update(tableName, keyCol, keyVal, valuesToUpdate);
        }
    }

    public boolean update(Object object) {
        Class<?> clazz = object.getClass();
        DBMetaData dbMetaData = getDBMetaData(clazz);
        String tableName = dbMetaData.getTableName();
        String keyCol = dbMetaData.getIdColumnName();
        Map<String, Object> valuesToUpdate = dbMetaData.asDBMap(object);
        Object keyVal = valuesToUpdate.remove(keyCol); // remove the key
        return update(tableName, keyCol, keyVal, valuesToUpdate);
    }

    public boolean delete(Object object) {
        Class<?> clazz = object.getClass();
        DBMetaData dbMetaData = getDBMetaData(clazz);
        String tableName = dbMetaData.getTableName();
        String keyCol = dbMetaData.getIdColumnName();
        Map<String, Object> valuesToUpdate = dbMetaData.asDBMap(object);
        Object keyVal = valuesToUpdate.get(keyCol);
        return delete(tableName, keyCol, keyVal);
    }

    // TODO replace TreeMap w/ LinkedHashMap
    private long insert(String tableName, Map<String, Object> values) {
        if (!(values instanceof TreeMap<String, Object>)) {
            values = new TreeMap<>(values);
        }
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(tableName);
        sb.append(" (");
        boolean first = true;
        for (String name : values.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(name);
        }
        sb.append(") VALUES (");
        first = true;
        for (String string : values.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("?");
        }
        sb.append(")");
        String insertString = sb.toString();
        logger.log(GrugLogger.Level.INFO, "INSERT SQL: {}\n  Args:{}", insertString, values.values());
        try (ConnectionInfo ci = getConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(insertString);
            int col = 1;
            for (Object o : values.values()) {
                setValueForQuery(preparedStatement, col++, o);
            }
            int updated = preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                return -1;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean update(String tableName, String keyCol, Object keyVal, Map<String, Object> values) {
        if (!(values instanceof TreeMap<String, Object>)) {
            values = new TreeMap<>(values);
        }
        StringBuilder sb = new StringBuilder("UPDATE ");
        sb.append(tableName);
        sb.append(" SET (");
        boolean first = true;
        for (String name : values.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(name).append("=?");
        }
        sb.append(") WHERE ");
        sb.append(keyCol).append("=?");
        try (ConnectionInfo ci = getConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(sb.toString());
            int col = 1;
            for (Object o : values.values()) {
                setValueForQuery(preparedStatement, col++, o);
            }
            setValueForQuery(preparedStatement, col, keyVal);
            return preparedStatement.executeUpdate() == 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean delete(String tableName, String keyCol, Object keyVal) {
        StringBuilder sb = new StringBuilder("DELETE FROM ");
        sb.append(tableName);
        sb.append(" WHERE ");
        sb.append(keyCol).append("=?");
        try (ConnectionInfo ci = getConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(sb.toString());
            setValueForQuery(preparedStatement, 1, keyVal);
            return preparedStatement.executeUpdate() == 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void exec(String sql) {
        try (ConnectionInfo ci = getConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String snakeCase(String string) {
        StringBuilder result = new StringBuilder();
        char[] charArray = string.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (Character.isUpperCase(c)) {
                if (i != 0) {
                    result.append("_");
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public <T> GrugQuery<T> query(Class<T> clazz) {
        return new GrugQuery<T>(clazz);
    }

    public interface Interfaces {

        interface GrugLogger {
            enum Level {
                ERROR, WARN, INFO, DEBUG, TRACE;
            }

            void log(Level level, String msg, Object... args);
        }

        interface BeforeSet {
            default Object beforeSet(Field field, Object value) {
                return value;
            }
        }

        interface AfterSet {
            default void afterSet(Field field, Object value) {
            }
        }

        interface GrugRecord extends BeforeSet, AfterSet {
            default long insert() {
                GrugORM orm = getDefaultOrThrow();
                return orm.insert(this);
            }
        }
    }

    public class GrugQuery<T> {
        private final Class<?> clazz;
        private StringBuilder whereClause = new StringBuilder();
        private Map<String, Object> valMap = new TreeMap<>();

        public GrugQuery(Class<?> clazz) {
            this.clazz = clazz;
        }

        public GrugQuery<T> where(String condition) {
            if (!whereClause.isEmpty()) {
                whereClause.append(" AND ");
            }
            whereClause.append(condition);
            return this;
        }

        List<T> run() {
            return (List<T>) findAll(clazz, whereClause.toString(), valMap);
        }

        public GrugQuery<T> with(Map<String, Object> vals) {
            for (Map.Entry<String, Object> stringObjectEntry : vals.entrySet()) {
                with(stringObjectEntry.getKey(), stringObjectEntry.getValue());
            }
            return this;
        }

        public GrugQuery<T> with(String name, Object value) {
            if (valMap.containsKey(name)) {
                throw new IllegalStateException("Value " + name + " already exists in query!");
            }
            valMap.put(name, value);
            return this;
        }
    }

    private class DefaultLogger implements GrugLogger {
        // thank you slf4j for using a non-standard logging format, very cool
        final Pattern parens = Pattern.compile("\\{}");

        @Override
        public void log(Level level, String msg, Object... args) {
            if (level.ordinal() <= internalLoggerLevel.ordinal()) {
                String logMsg = "[" + Instant.now() + "] " + level + ": " + msg;
                if (args.length > 0) {
                    int index = 0;
                    Matcher matcher = parens.matcher(logMsg);
                    StringBuilder fixedString = new StringBuilder();
                    while (matcher.find()) {
                        matcher.appendReplacement(fixedString, "{" + index + "}");
                        index = index + 1;
                    }
                    matcher.appendTail(fixedString);
                    logMsg = MessageFormat.format(fixedString.toString(), args);
                }
                if (level.ordinal() <= Level.ERROR.ordinal()) {
                    System.err.println(logMsg);
                } else {
                    System.out.println(logMsg);
                }
            }
        }
    }

    //==================================================================
    //  Metadata stuff
    //==================================================================

    private void setValueForQuery(PreparedStatement ps, int parameterIndex, Object val) throws SQLException {
        if(val == null) {
            ps.setNull(parameterIndex, Types.NULL);
            return;
        }

        // enums serialize as strings
        if (val instanceof Enum<?> e) {
            val = e.name();
        }

        switch (val) {
            case Boolean b -> ps.setBoolean(parameterIndex, b);
            case Integer i -> ps.setInt(parameterIndex, i);
            case Long l -> ps.setLong(parameterIndex, l);
            case Float f -> ps.setDouble(parameterIndex, f);
            case Double d -> ps.setDouble(parameterIndex, d);
            case String str -> ps.setString(parameterIndex, str);
            case Time d -> ps.setTime(parameterIndex, d);
            case Date d -> {
                Timestamp timestamp = new Timestamp(d.getTime());
                ps.setTimestamp(parameterIndex, timestamp);
            }
            case Blob blob -> ps.setBlob(parameterIndex, blob);
            case null, default -> ps.setObject(parameterIndex, val);
        }
    }

    private Object getValueFromQuery(String fieldName, Class fieldType, ResultSet resultSet) throws Exception {
        Object fieldVal;

        if (fieldType == String.class) {
            fieldVal = resultSet.getString(fieldName);
        } else if (fieldType == Integer.class || fieldType == int.class) {
            fieldVal = resultSet.getInt(fieldName);
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            fieldVal = resultSet.getBoolean(fieldName);
        } else if (fieldType == Long.class || fieldType == long.class) {
            fieldVal = resultSet.getLong(fieldName);
        } else if (fieldType == Double.class || fieldType == double.class) {
            fieldVal = resultSet.getDouble(fieldName);
        } else if (fieldType.isEnum()) {
            // enums deserialize as strings
            String strValue = resultSet.getString(fieldName);
            fieldVal = Enum.valueOf(fieldType, strValue);
        } else if (fieldType == Date.class) {
            Timestamp timestamp = resultSet.getTimestamp(fieldName);
            fieldVal = new Date(timestamp.getTime());
        } else {
            fieldVal = resultSet.getObject(fieldName, fieldType);
        }

        return fieldVal;
    }

    private DBMetaData getDBMetaData(Class<?> clazz) {
        return metadataCache.computeIfAbsent(clazz, DBMetaData::new);
    }

    private class DBMetaData {
        public static final String DEFAULT_ID_COL_NAME = "id";

        Class classForTable;
        private String tableName;
        ArrayList<Field> fields;
        Map<String, String> fieldNameToColumnNames;
        Map<String, String> columnNameToFieldNames;
        private String idColumnName;

        public DBMetaData(Class aClass) {
            classForTable = aClass;

            String name = aClass.getSimpleName();
            this.tableName = snakeCase(name);

            fields = new ArrayList<>();
            fieldNameToColumnNames = new HashMap<>();
            columnNameToFieldNames = new HashMap<>();

            for (Field field : getAllFields(aClass)) {
                if (!shouldIgnore(field)) {
                    field.setAccessible(true);
                    fields.add(field);
                    String fieldName = field.getName();
                    String columnName = snakeCase(fieldName);
                    fieldNameToColumnNames.put(fieldName, columnName);
                    columnNameToFieldNames.put(columnName, fieldName);
                }
            }

            idColumnName = DEFAULT_ID_COL_NAME;
        }

        private static List<Field> getAllFields(Class aClass) {
            List<Field> fieldsToReturn = new ArrayList<>();
            while (aClass != null) {
                Field[] fields = aClass.getDeclaredFields();
                List<Field> tmpList = Arrays.asList(fields);
                fieldsToReturn.addAll(tmpList);
                aClass = aClass.getSuperclass();
            }
            return fieldsToReturn;
        }

        public String getTableName() {
            return this.tableName;
        }

        public String getIdColumnName() {
            return idColumnName;
        }

        public Map<String, Object> asDBMap(Object object) {
            Map<String, Object> values = new TreeMap<>();
            for (Field field : fields) {
                try {
                    String fieldName = field.getName();
                    String columnName = fieldNameToColumnNames.get(fieldName);
                    Object value = field.get(object);
                    values.put(columnName, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return values;
        }


        public <T> T newObjectFromResult(ResultSet resultSet) throws Exception {
            T object = (T) classForTable.newInstance();
            for (Field field : fields) {
                // ignore static fields always
                if (shouldIgnore(field)) {
                    continue;
                }
                String fieldName = snakeCase(field.getName());
                Object val = getValueFromQuery(fieldName, field.getType(), resultSet);

                if (object instanceof Interfaces.BeforeSet beforeSet) {
                    val = beforeSet.beforeSet(field, val);
                }

                field.set(object, val);

                if (object instanceof Interfaces.AfterSet afterSet) {
                    afterSet.afterSet(field, val);
                }

            }
            return object;

        }

        // TODO - make pluggable
        private static boolean shouldIgnore(Field field) {
            return Modifier.isStatic(field.getModifiers());
        }
    }

    //========================================================================================
    // Migrations System
    //========================================================================================

    public static abstract class Migrations {

        private final LinkedHashMap<String, Migration> migrations = new LinkedHashMap<>();
        private GrugORM orm;

        protected Migrations() {
            migrations(); // init migrations
        }

        public void setORM(GrugORM orm) {
            this.orm = orm;
        }

        public GrugORM getORM() {
            if(orm != null) {
                return orm;
            }
            if(GrugORM.getDefault() != null) {
                return GrugORM.getDefault();
            }
            throw new IllegalStateException("ORM has not been set and there is no default ORM, don't know what database to migrate!");
        }

        protected void add(Supplier<Migration> migrationCallable) {
            add(migrationCallable.get());
        }

        protected void add(Migration migration) {
            String migrationName = migration.getClass().getSimpleName();
            if(migrations.containsKey(migrationName)) {
                throw new IllegalArgumentException("Migration " + migrationName + " already exists!");
            }
            migrations.put(migrationName, migration);
        }


        /**
         * @return the initial pre-migrations schema for the database
         */
        public String initialSchema() {
            return "";
        };

        public void migrations() {
            throw new RuntimeException("Override the migrations() method and add migrations to this migration file!");
        }

        public static Migration makeMigration(String name) {
            Migration migration = new Migration();
            migration.name = name;
            return migration;
        }

        /**
         * Applies all outstanding migrations in the order they are declared
         */
        public void apply() {
            GrugORM orm = getORM();
            orm.exec(Migration.DDL);

            // compute migrations with persisted migrations merged in
            LinkedHashMap<String, Migration> mergedMigrations = new LinkedHashMap<>(migrations);
            ResultList<Migration> persistedMigrations = orm.findAll(Migration.class);
            for (Migration persistedMigration : persistedMigrations) {
                Migration existingMigration = mergedMigrations.get(persistedMigration.getName());
                if (existingMigration != null) {
                    if(!existingMigration.equals(persistedMigration)) {
                        // TODO log warning inconsistent migrations found
                    }
                    // update ID
                    existingMigration.id = persistedMigration.id;
                    persistedMigrations.remove(persistedMigration);
                }
            }

            if(persistedMigrations.size() > 0) {
                // TODO log warning: persisted migrations that are not found in the DB
            }

            for (Migration migration : mergedMigrations.values()) {
                if (!migration.isApplied()) {
                    migration.runUp(orm);
                }
            }
        }

        public static final class Migration {

            public static final String DDL = """
                    CREATE TABLE IF NOT EXISTS migration (
                        id INTEGER,
                        created_at INTEGER,
                        name VARCHAR,
                        description VARCHAR,
                        up VARCHAR,
                        down VARCHAR,
                        status VARCHAR
                    );
                    """;

            private Long id;
            private Long createdAt;
            private String name;
            private String description;
            private String up;
            private String down;
            private MigrationStatus status;

            public Migration description(String description) {
                this.description = description;
                return this;
            }

            public Migration up(String up) {
                this.up = up;
                return this;
            }

            public Migration down(String down) {
                this.down = down;
                return this;
            }

            public String getName() {
                return name;
            }

            public boolean isApplied() {
                return status == MigrationStatus.APPLIED;
            }

            void runUp(GrugORM orm) {
                orm.exec(this.up);
                this.status = MigrationStatus.APPLIED;
                if (this.id == null) {
                    orm.insert(this);
                } else {
                    orm.update(this);
                }
            }

            void runDown(GrugORM orm) {
                orm.exec(this.down);
                orm.delete(this);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Migration migration = (Migration) o;
                return Objects.equals(name, migration.name) && Objects.equals(up, migration.up) && Objects.equals(down, migration.down);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, up, down);
            }

            public MigrationStatus getStatus() {
                return status;
            }
        }

        public enum MigrationStatus {
            PENDING,
            APPLIED,
            SKIPPED
        }
    }

    //========================================================================================
    // GrugORM Query Result
    //========================================================================================

    public static class ResultList<T> extends ArrayList<T> {

        public <Q> ResultList<Q> map(Function<T, Q> mapper) {
            ResultList<Q> mappedResult = new ResultList<>();
            for (T t : this) {
                mappedResult.add(mapper.apply(t));
            }
            return mappedResult;
        }

        public Set<T> toSet() {
            return new HashSet<>(this);
        }

        public ResultList<T> filter(Predicate<? super T> filter) {
            ResultList<T> mappedResult = new ResultList<>();
            for (T t : this) {
                if(filter.test(t)) {
                    mappedResult.add(t);
                }
            }
            return mappedResult;
        }
    }

}