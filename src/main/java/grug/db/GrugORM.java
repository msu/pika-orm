package grug.db;

import grug.db.GrugORM.Interfaces.GrugLogger;
import grug.db.GrugORM.Interfaces.GrugRecordLifecycle;

import java.io.Console;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.sql.*;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"rawtypes", "UnusedReturnValue", "UnnecessaryLocalVariable"})
public class GrugORM {

    public static final String SQL_VARS_PATTERN = "(:[\\w][\\d\\w]*)";
    private static GrugORM DEFAULT_ORM = null;
    private static final ThreadLocal<ConnectionInfo> CURRENT_CONNECTION = new ThreadLocal<>();
    private static final ForceThrower FORCE_THROWER = generateForceThrower();

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
        this.logger = logger;
        // custom loggers get everything by default
        this.withLogLevel(GrugLogger.Level.TRACE);
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
    // 1-N and N-N functionality
    //====================================================================

    public <T> List<T> loadN(Object owner, Class<T> nClass, String backPointerColumn) {
        DBMetaData metaData = getDBMetaData(owner.getClass());
        Object ownerPkValue = metaData.getId(owner);
        return findAllBy(nClass, backPointerColumn, ownerPkValue);
    }

    public <T> T load1(Object owner, Class<T> nClass, String backPointerColumn) {
        DBMetaData metaData = getDBMetaData(owner.getClass());
        Object ownerPkValue = metaData.getValueForDBCol(owner, backPointerColumn);
        return find(nClass, ownerPkValue);
    }

    public <T> ResultList<T> select(String query, Class<T> resultClass) {
        return select(query, Map.of(), resultClass);
    }

    //====================================================================
    // Connection management
    //====================================================================

    private class ConnectionInfo implements AutoCloseable {

        ConnectionInfo previous;
        Connection conn;
        int openCount;
        int transactionCount;
        UUID uuid;

        public ConnectionInfo(Connection connection, ConnectionInfo previousConnection) {
            this.conn = connection;
            this.previous = previousConnection;
            this.uuid = UUID.randomUUID();
            this.openCount = 0;
            this.transactionCount = 0;
        }

        public void incrementOpenCount() {
            openCount++;
            logger.log(GrugLogger.Level.DEBUG, "Incremented open count on connection {}: {}", uuid, "*".repeat(openCount));
        }

        @Override
        public void close() {
            openCount--;
            logger.log(GrugLogger.Level.DEBUG, "Decremented open count on connection {}: {}", uuid, "*".repeat(openCount));
            if (openCount == 0) { // if we are back at the top level of the connection count we close the connection
                logger.log(GrugLogger.Level.INFO, "Closing connection {} on Thread {}", uuid, Thread.currentThread().getName());
                try {
                    safely(() -> conn.close());
                } finally {
                    CURRENT_CONNECTION.set(this.previous);
                }
            }
        }

        public boolean isInTransaction() {
            return transactionCount > 0;
        }

        public void startTransaction()  {
            if(transactionCount == 0) {
                safely(() -> conn.setAutoCommit(false));
                logger.log(GrugLogger.Level.INFO, "Starting new transaction for connection {}", uuid);
            } else {
                logger.log(GrugLogger.Level.INFO, "Existing transaction for connection {}, joining it", uuid);
            }
            transactionCount++;
        }

        public void commitTransaction() {
            if (isInTransaction()) {
                transactionCount--;
                if (transactionCount == 0) {
                    logger.log(GrugLogger.Level.INFO, "Transaction for connection {} completed, committing", uuid);
                    safely(() -> conn.commit());
                } else {
                    logger.log(GrugLogger.Level.INFO, "Nested transaction detected for connection {}, deferring commit", uuid);
                }
                close();
            } else {
                logger.log(GrugLogger.Level.ERROR, "No current transaction for connection {}", uuid);
            }
        }

        public void rollBackTransaction() {
            if (isInTransaction()) {
                logger.log(GrugLogger.Level.INFO, "Rolling back transaction for connection {}", uuid);
                safely(() -> conn.rollback());
                transactionCount--;
                if (transactionCount == 0) { // restore autocommit on last transaction scope
                    logger.log(GrugLogger.Level.INFO, "Restoring autoCommit for connection {}", uuid);
                    safely(()->conn.setAutoCommit(true));
                }
                close();
            } else {
                logger.log(GrugLogger.Level.ERROR, "No current transaction for connection {}", uuid);
            }
        }
    }

    private Connection createConnection() {
        return safely(() -> connectionSource.call());
    }

    private ConnectionInfo getOrCreateConnectionInfo() {
        ConnectionInfo connectionInfo = getCurrentConnection();
        if (connectionInfo == null) {
            connectionInfo = pushNewConnection();
        }
        connectionInfo.incrementOpenCount();
        return connectionInfo;
    }

    private static ConnectionInfo getCurrentConnection() {
        ConnectionInfo connectionInfo = CURRENT_CONNECTION.get();
        return connectionInfo;
    }

    private ConnectionInfo pushNewConnection() {
        ConnectionInfo previousConnection = getCurrentConnection();
        Connection connection = createConnection();
        ConnectionInfo newConnectionInfo = new ConnectionInfo(connection, previousConnection);
        logger.log(GrugLogger.Level.INFO, "Created a new connection for Thread {} w/ID {}", Thread.currentThread().getName(), newConnectionInfo.uuid);
        CURRENT_CONNECTION.set(newConnectionInfo);
        return newConnectionInfo;
    }

    //====================================================================
    // Transaction management
    //====================================================================

    public void inTransaction(Runnable runnable) {
            try {
                startTransaction();
                runnable.run();
                commitTransaction();
            } catch (Exception e) {
                rollBackTransaction();
                rethrow(e);
            }
    }

    public void startTransaction() {
        ConnectionInfo connectionInfo = getOrCreateConnectionInfo();
        connectionInfo.startTransaction();
    }

    public void commitTransaction() {
        ConnectionInfo connectionInfo = getCurrentConnection();
        if (connectionInfo == null) {
            logger.log(GrugLogger.Level.ERROR, "No current connection for transaction.");
        } else {
            connectionInfo.commitTransaction();
        }
    }

    public void rollBackTransaction() {
        ConnectionInfo connectionInfo = getCurrentConnection();
        if (connectionInfo == null) {
            logger.log(GrugLogger.Level.ERROR, "No current connection for transaction.");
        } else {
            connectionInfo.rollBackTransaction();
        }
    }


    //====================================================================
    // Database interaction
    //====================================================================

    public <T> T find(Class<T> clazz, Object pk) {
        DBMetaData dbMetaData = getDBMetaData(clazz);
        return find(clazz, dbMetaData.getIdColumnName(), pk);
    }

    public <T> T find(Class<T> clazz, String key, Object val) {
        DBMetaData dbMetaData = getDBMetaData(clazz);
        String sql = "SELECT * FROM " + dbMetaData.getTableName() + " WHERE " + key + "=?";
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement ps = conn.prepareStatement(sql);
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
            logger.log(GrugLogger.Level.ERROR, "Exception in find() with SQL {} & values {}: {}", sql, val, e.getMessage());
            rethrow(e);
            return null;
        }
    }

    public <T> ResultList<T> findAll(Class<T> clazz) {
        return selectWhere(clazz, "true=true", Map.of());
    }

    public <T> ResultList<T> findAllBy(Class<T> clazz, String column, Object val) {
        return selectWhere(clazz, column + "=:val ", Map.of("val", val));
    }

    public <T> ResultList<T> selectWhere(Class<T> clazz, String whereClause, Map<String, Object> args) {
        DBMetaData metaData = getDBMetaData(clazz);
        String tableName = metaData.getTableName();
        String selectClause = "SELECT * FROM " + tableName + " WHERE ";
        String sql = selectClause + whereClause;
        return select(sql, args, clazz);
    }

    public ResultList<ResultMap> select(String sql, Map<String, Object> args) {
        return select(sql, args, ResultMap.class);
    }

    public <T> ResultList<T> select(String sql, Map<String, Object> args, Class resultClass) {
        DBMetaData dbMetaData = getDBMetaData(resultClass);
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            ArrayList<Object> vals = new ArrayList<>();
            String updatedSql = updateSqlVars(sql, args, vals);//SQL, Argument Map, Blank Value list to be filled
            logger.log(GrugLogger.Level.INFO, "Select SQL: {}\n  Args:{}", updatedSql, vals);
            PreparedStatement ps = conn.prepareStatement(updatedSql);
            for (int i = 0; i < vals.size(); i++) {
                Object val = vals.get(i);
                setValueForQuery(ps, i + 1, val);
            }
            ResultSet resultSet = ps.executeQuery();
            ResultList<T> result = new ResultList<>();
            while (resultSet.next()) {
                T object = dbMetaData.newObjectFromResult(resultSet);
                result.add(object);
            }
            return result;
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in select() with SQL {} & args {}: {}", sql, args, e.getMessage());
            rethrow(e);
            return null;
        }
    }

    public long insert(Object object) {
        Class<?> clazz = object.getClass();
        DBMetaData metaData = getDBMetaData(clazz);
        String keyCol = metaData.getIdColumnName();
        Map<String, Object> values = metaData.asDBMap(object);
        values.remove(keyCol);
        if (object instanceof GrugRecordLifecycle lifecycle) {
            if (!lifecycle.beforeInsert()) {
                return -1; // TODO flag value?
            }
        }
        long id = insert(metaData.getTableName(), values);
        if (object instanceof GrugRecordLifecycle lifecycle) {
            lifecycle.afterInsert();
        }
        if (!metaData.isReadOnly()) {
            metaData.setId(object, id);
        }
        return id;
    }

    public long[] insertAll(Collection<Object> items) { // TODO - look into the setID as i was having some issues and weirdness with it
        long[] ids = new long[items.size()];
        int count = 0;
        for (Object o : (Collection<?>) items) {
            ids[count] = insert(o);
            count++;
        }
        return ids;
    }

    private long insert(String tableName, Map<String, Object> values) {
        if (!(values instanceof LinkedHashMap<String, Object>)) {
            values = new LinkedHashMap<>(values);
        }
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(tableName);
        if (values.isEmpty()) {
            sb.append(" DEFAULT VALUES");
        } else {
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
        }
        String insertString = sb.toString();
        logger.log(GrugLogger.Level.INFO, "INSERT SQL: {}\n  Args:{}", insertString, values.values());
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
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
            logger.log(GrugLogger.Level.ERROR, "Exception in insert() with SQL {} & args {}: {}", insertString, values.values(), e.getMessage());
            rethrow(e);
            return 0;
        }
    }

    public boolean update(Object object) {
        Class<?> clazz = object.getClass();
        DBMetaData dbMetaData = getDBMetaData(clazz);
        String tableName = dbMetaData.getTableName();
        String keyCol = dbMetaData.getIdColumnName();
        Map<String, Object> valuesToUpdate = dbMetaData.asDBMap(object);
        Object keyVal = valuesToUpdate.remove(keyCol); // remove the key
        if(object instanceof GrugRecordLifecycle lifecycle) {
            if (!lifecycle.beforeUpdate()) {
                return false;
            }
        }
        boolean update = update(tableName, keyCol, keyVal, valuesToUpdate);
        if(object instanceof GrugRecordLifecycle lifecycle) {
            lifecycle.afterUpdate();
        }
        return update;
    }

    private boolean update(String tableName, String keyCol, Object keyVal, Map<String, Object> values) {
        if (!(values instanceof TreeMap<String, Object>)) {
            values = new TreeMap<>(values);
        }
        StringBuilder sb = new StringBuilder("UPDATE ");
        sb.append(tableName);
        sb.append(" SET ");
        boolean first = true;
        for (String name : values.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(name).append(" = ?");
        }
        sb.append(" WHERE ");
        sb.append(keyCol).append("=?");
        String updateSQL = sb.toString();
        logger.log(GrugLogger.Level.INFO, "UPDATE SQL: {}\n  Args:{}", updateSQL, values.values());
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(updateSQL);
            int col = 1;
            for (Object o : values.values()) {
                setValueForQuery(preparedStatement, col++, o);
            }
            setValueForQuery(preparedStatement, col, keyVal);
            return preparedStatement.executeUpdate() == 1;
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in update() with SQL {} & args {}: {}", updateSQL, values.values(), e.getMessage());
            rethrow(e);
            return false;
        }
    }

    public boolean delete(Object object) {
        Class<?> clazz = object.getClass();
        DBMetaData dbMetaData = getDBMetaData(clazz);
        String tableName = dbMetaData.getTableName();
        String keyCol = dbMetaData.getIdColumnName();
        Map<String, Object> valuesToUpdate = dbMetaData.asDBMap(object);
        Object keyVal = valuesToUpdate.get(keyCol);
        if(object instanceof GrugRecordLifecycle lifecycle) {
            if (!lifecycle.beforeDelete()) {
                return false;
            }
        }
        boolean delete = delete(tableName, keyCol, keyVal);
        if(object instanceof GrugRecordLifecycle lifecycle) {
            lifecycle.afterDelete();
        }
        return delete;
    }

    private boolean delete(String tableName, String keyCol, Object keyVal) {
        StringBuilder sb = new StringBuilder("DELETE FROM ");
        sb.append(tableName);
        sb.append(" WHERE ");
        sb.append(keyCol).append("=?");
        String deleteSQL = sb.toString();
        logger.log(GrugLogger.Level.INFO, "DELETE SQL: {}\n  Args:{}", deleteSQL, List.of(keyVal));
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(sb.toString());
            setValueForQuery(preparedStatement, 1, keyVal);
            return preparedStatement.executeUpdate() == 1;
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in update() with SQL {} & value {}: {}", deleteSQL, keyVal, e.getMessage());
            rethrow(e);
            return false;
        }
    }

    public boolean exec(String sql) {
        if (sql.isBlank()) {
            logger.log(GrugLogger.Level.WARN, "SQL is blank, will not be executed!");
            return false;
        }
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            logger.log(GrugLogger.Level.INFO, "EXECUTING RAW SQL: {}\n", sql);
            //noinspection SqlSourceToSinkFlow
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            return preparedStatement.execute();
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in exec() with SQL {}: {}", sql, e.getMessage());
            rethrow(e);
            return false;
        }
    }

    // utilities

    private String updateSqlVars(String sql, Map<String, Object> args, List<Object> argList) {
        Pattern compile = Pattern.compile(SQL_VARS_PATTERN);
        Matcher matcher = compile.matcher(sql);
        StringBuilder finalSql = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group().substring(1);
            if (args.containsKey(match)) {
                Object valueForMatch = args.get(match);
                StringBuilder replacementSb = new StringBuilder();
                if (valueForMatch instanceof Collection c) {
                    replacementSb.append("(");
                    int size = c.size();
                    while (size > 0) {
                        if (size != c.size()) {
                            replacementSb.append(",");
                        }
                        replacementSb.append("?");
                        size--;
                    }
                    argList.addAll(c);
                    replacementSb.append(")");
                } else {
                    replacementSb.append("?");
                    argList.add(valueForMatch);
                }
                matcher.appendReplacement(finalSql, replacementSb.toString());
            } else {
                throw new IllegalArgumentException("No value found for variable " + match + " in " + args);
            }
        }
        matcher.appendTail(finalSql);
        return finalSql.toString();
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
        return new GrugQuery<>(clazz);
    }

    public interface Interfaces {

        interface GrugLogger {
            enum Level {
                ERROR, WARN, INFO, DEBUG, TRACE;
            }

            void log(Level level, String msg, Object... args);
        }

        interface GrugRecordLifecycle {
            default boolean beforeInsert() {
                return true;
            }
            default boolean beforeUpdate() {
                return true;
            }
            default boolean beforeDelete() {
                return true;
            }

            default void afterInsert() {}
            default void afterUpdate() {}
            default void afterDelete() {}
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
        private final StringBuilder whereClause = new StringBuilder();
        private final Map<String, Object> valMap = new TreeMap<>();

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
            //noinspection unchecked
            return (List<T>) selectWhere(clazz, whereClause.toString(), valMap);
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
            default -> ps.setObject(parameterIndex, val);
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
            if (timestamp == null) {
                fieldVal = null;
            } else {
                fieldVal = new Date(timestamp.getTime());
            }
        } else {
            fieldVal = resultSet.getObject(fieldName, fieldType);
        }

        return fieldVal;
    }

    private DBMetaData getDBMetaData(Class<?> clazz) {
        return metadataCache.computeIfAbsent(clazz, DBMetaData::new);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private class DBMetaData {

        public static final String DEFAULT_ID_COL_NAME = "id";

        Class classForTable;
        private RecordComponent[] recordComponents;
        private String tableName;
        Map<String, Field> fields;
        Map<String, String> fieldNameToColumnNames;
        Map<String, String> columnNameToFieldNames;
        private String idColumnName;
        private Field idField;

        public DBMetaData(Class aClass) {
            this.classForTable = aClass;
            if(aClass == ResultMap.class) {
                // do nothing
            } else {
                String name = aClass.getSimpleName();
                this.tableName = snakeCase(name);

                fields = new LinkedHashMap<>();
                fieldNameToColumnNames = new HashMap<>();
                columnNameToFieldNames = new HashMap<>();

                idColumnName = DEFAULT_ID_COL_NAME;
                for (Field field : getAllFields(aClass)) {
                    if (!shouldIgnore(field)) {
                        field.setAccessible(true);
                        fields.put(field.getName(), field);
                        String fieldName = field.getName();
                        String columnName = snakeCase(fieldName);
                        fieldNameToColumnNames.put(fieldName, columnName);
                        columnNameToFieldNames.put(columnName, fieldName);
                        if(columnName.equals(idColumnName)) {
                            idField = field;
                        }
                    }
                }
            }
            if (classForTable.isRecord()) {
                recordComponents = classForTable.getRecordComponents();
            } else {
                recordComponents = null;
            }
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
            for (Field field : fields.values()) {
                String fieldName = field.getName();
                String columnName = fieldNameToColumnNames.get(fieldName);
                Object value = safely(() -> field.get(object));
                values.put(columnName, value);
            }
            return values;
        }

        @SuppressWarnings({"unchecked", "deprecation"})
        public <T> T newObjectFromResult(ResultSet resultSet) throws Exception {
            if(classForTable == ResultMap.class) {
                ResultMap resultMap = new ResultMap();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int i  = metaData.getColumnCount();
                for (int j = 1; j <= i; j++) {
                    String columnName = metaData.getColumnName(j);
                    Object value = resultSet.getObject(columnName);
                    resultMap.put(columnName, value);
                }
                return (T) resultMap;
            } else {
                T object;
                // if it's a record use the generated constructor
                if(recordComponents != null) {
                    Object[] args = new Object[recordComponents.length];
                    for (int i = 0; i < recordComponents.length; i++) {
                        RecordComponent recordComponent = recordComponents[i];
                        String fieldName = snakeCase(recordComponent.getName());
                        Object val = getValueFromQuery(fieldName, recordComponent.getType(), resultSet);
                        args[i] = val;
                    }
                    Constructor[] constructors = classForTable.getDeclaredConstructors();
                    Constructor recordConstructor = constructors[0];
                    object = (T) recordConstructor.newInstance(args);
                } else {
                    // otherwise use fields
                    object = (T) classForTable.newInstance();
                    for (Field field : fields.values()) {
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
                }
                return object;
            }
        }

        // TODO - make pluggable
        private static boolean shouldIgnore(Field field) {
            return Modifier.isStatic(field.getModifiers());
        }

        public void setId(Object object, long id) {
            safely(() -> idField.set(object, id));
        }

        public Object getId(Object object) {
            return safely(() -> idField.get(object));
        }

        public Object getValueForDBCol(Object owner, String columnName) {
            String fieldName = columnNameToFieldNames.get(columnName);
            Field field = fields.get(fieldName);
            return safely(() -> field.get(owner));
        }

        public boolean isReadOnly() {
            return !classForTable.isRecord();
        }
    }

    //========================================================================================
    // Migrations System
    //========================================================================================

    public static abstract class Migrations {

        public static final String HELP_MSG = """
                Migrations Commands
                
                  show      - show all migrations
                  up        - apply one pending migration
                  down      - back out the latest migration
                  all       - apply all pending migrations
                  exit/quit - exit this tool
                  help/?    - show this help message
                """;
        private LinkedHashMap<String, Migration> migrations;
        private GrugORM orm;

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
            String migrationName = migration.getName();
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

        public abstract void migrations();

        public static Migration makeMigration(String name) {
            Migration migration = new Migration(name);
            return migration;
        }

        public void console() {
            GrugORM orm = getORM();
            orm.exec(Migration.DDL);
            Console console = System.console();
            label:
            while(true) {
                String cmd = console.readLine("migrations > ").strip();
                //noinspection IfCanBeSwitch
                if (cmd.equals("show")) {
                    console.printf(show());
                } else if (cmd.equals("raw")) {
                    var mergedMigrations = loadMigrations(orm);
                    console.printf(new ResultList<>(mergedMigrations.values()).join("\n"));
                } else if (cmd.equals("up")) {
                    up();
                } else if (cmd.equals("down")) {
                    down();
                } else if (cmd.equals("all")) {
                    applyAll();
                    console.printf("All pending migrations have been applied");
                } else if (cmd.equals("help") || cmd.equals("?")) {
                    console.printf(HELP_MSG);
                } else if (cmd.equals("exit") || cmd.equals("quit")) {
                    break label;
                } else {
                    console.printf("Unknown command : " + cmd + "\n");
                    console.printf(HELP_MSG);
                }
            }
        }

        private String show() {
            GrugORM orm = getORM();
            orm.exec(Migration.DDL);
            var mergedMigrations = loadMigrations(orm);

            StringBuilder sb = new StringBuilder("All Migrations:\n");
            String formatString = "%-30.30s | %-15.15s | %-30.30s | %-30.30s | %-30.30s | %-30.30s\n";
            sb.append(String.format(formatString, "name", "status", "applied", "description", "up", "down"));
            sb.append("-------------------------------------------------------------------------------------------------------------------------------------------------------\n");
            for (Migration value : mergedMigrations.values()) {
                sb.append(String.format(formatString,
                        value.getName(), value.getStatus(), value.appliedAtForDisplay(), value.description, value.upForDisplay(), value.downForDisplay()));
            }
            return sb.toString();
        }

        public void up() {
            GrugORM orm = getORM();
            orm.exec(Migration.DDL);
            var mergedMigrations = loadMigrations(orm);

            var values = new ResultList<>(mergedMigrations.values());
            var firstUnappliedMigration = values.firstWhere(Migration::isPending);
            if(firstUnappliedMigration != null) {
                firstUnappliedMigration.runUp(orm);
            } else {
                orm.getLogger().log(GrugLogger.Level.WARN, "No pending migrations were found in migrations file to apply");
            }
        }

        public void down() {
            GrugORM orm = getORM();
            orm.exec(Migration.DDL);
            var mergedMigrations = loadMigrations(orm);

            var values = new ResultList<>(mergedMigrations.values());
            var lastAppliedMigration = values.lastWhere(Migration::isApplied);
            if(lastAppliedMigration != null) {
                lastAppliedMigration.runDown(orm);
            } else {
                orm.getLogger().log(GrugLogger.Level.WARN, "No applied migrations were found in migrations file to back out");
            }
        }

        /**
         * Applies all outstanding migrations in the order they are declared
         */
        public void applyAll() {
            GrugORM orm = getORM();
            orm.exec(Migration.DDL);
            var mergedMigrations = loadMigrations(orm);
            for (Migration migration : mergedMigrations.values()) {
                if (!migration.isApplied()) {
                    migration.runUp(orm);
                }
            }
        }

        private LinkedHashMap<String, Migration> loadMigrations(GrugORM orm) {

            migrations = new LinkedHashMap<>();
            migrations();
            // compute migrations with persisted migrations merged in
            ResultList<Migration> persistedMigrations = orm.findAll(Migration.class);
            var mergedMigrations = new LinkedHashMap<>(migrations);
            for (Migration persistedMigration : persistedMigrations.copy()) {
                Migration existingMigration = mergedMigrations.get(persistedMigration.getName());
                if (existingMigration != null) {
                    if(!existingMigration.equals(persistedMigration)) {
                        orm.getLogger().log(GrugLogger.Level.WARN, MessageFormat.format("""
                                        Migration {0} has different content in the codebase and in the database:
                                        
                                        DB Content:
                                        {1}
                                        
                                        Code Content:
                                        {2}
                                        
                                        This may be due to ongoing development but should not be the case in production.
                                        """,
                                existingMigration.name,
                                persistedMigration.getDebugString(),
                                existingMigration.getDebugString()
                                ));
                    }
                    // update ID
                    existingMigration.id = persistedMigration.id;
                    existingMigration.status = persistedMigration.status;
                    persistedMigrations.remove(persistedMigration);
                }
            }

            if(!persistedMigrations.isEmpty()) {
                orm.getLogger().log(GrugLogger.Level.WARN,
                        "The following migrations have been found in the database, but are not in the current migration file:\n" +
                        persistedMigrations.join("\n"));
            }
            return mergedMigrations;
        }

        public static final class Migration {

            public static final String DDL = """
                    CREATE TABLE IF NOT EXISTS migration (
                        id INTEGER PRIMARY KEY,
                        applied_at INTEGER,
                        name VARCHAR,
                        description VARCHAR,
                        up VARCHAR,
                        down VARCHAR,
                        status VARCHAR
                    );
                    """;

            private Long id;
            private Long appliedAt;
            private String name;
            private String description;
            private String up;
            private String down;
            private MigrationStatus status = MigrationStatus.PENDING;

            private Migration(){}

            public Migration(String name) {
                this.name = name;
            }

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

            public boolean isPending() {
                return status == MigrationStatus.PENDING;
            }

            public String[] getUpSqlSplitOnSemicolons() {
                return this.up.split(";");
            }

            public String[] getDownSqlSplitOnSemicolons() {
                return this.down.split(";");
            }

            void runUp(GrugORM orm) {
                orm.inTransaction(() -> {
                    String[] upSqlSplitOnSemicolons = getUpSqlSplitOnSemicolons();
                    for (String sql : upSqlSplitOnSemicolons) {
                        orm.exec(sql);
                    }
                    this.status = MigrationStatus.APPLIED;
                    this.appliedAt = new Date().getTime();
                    if (this.id == null) {
                        orm.insert(this);
                    } else {
                        orm.update(this);
                    }
                });
            }

            void runDown(GrugORM orm) {
                orm.inTransaction(() -> {
                    String[] upSqlSplitOnSemicolons = getDownSqlSplitOnSemicolons();
                    for (String sql : upSqlSplitOnSemicolons) {
                        orm.exec(sql);
                    }
                    orm.delete(this);
                });
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

            @Override
            public String toString() {
                return "Migration{" +
                        "id=" + id +
                        ", appliedAt=" + appliedAt +
                        ", name='" + name + '\'' +
                        ", description='" + description + '\'' +
                        ", up='" + up + '\'' +
                        ", down='" + down + '\'' +
                        ", status=" + status +
                        '}';
            }

            public Object getDebugString() {
                return "{" +
                        "down='" + down + '\'' +
                        ", up='" + up + '\'' +
                        '}';
            }

            public Object upForDisplay() {
                String[] lines = up.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    lines[i] = lines[i].strip();
                }
                return String.join(" ", lines);
            }

            public Object downForDisplay() {
                String[] lines = down.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    lines[i] = lines[i].strip();
                }
                return String.join(" ", lines);
            }

            public Object appliedAtForDisplay() {
                if(appliedAt == null) {
                    return null;
                } else {
                    return new Date(appliedAt);
                }
            }
        }

        public enum MigrationStatus {
            PENDING,
            APPLIED,
            SKIPPED
        }
    }

    private GrugLogger getLogger() {
        return logger;
    }

    //========================================================================================
    // GrugORM Result List
    //========================================================================================

    public static class ResultMap extends LinkedHashMap<String, Object> {
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            return (T) this.get(key);
        }
    }
    public static class ResultList<T> extends ArrayList<T> {

        public ResultList() {}

        public ResultList(Collection<T> values) {
            super(values);
        }

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

        public String join(String separator) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
                T t = this.get(i);
                builder.append(t);
                if(i < thisSize - 1) {
                    builder.append(separator);
                }
            }
            return builder.toString();
        }

        public T first() {
            if(this.size() > 0) {
                return this.get(0);
            } else {
                return null;
            }
        }

        public T firstWhere(Predicate<? super T> predicate){
            for (T t : this) {
                if(predicate.test(t)) {
                    return t;
                }
            }
            return null;
        }

        public T last() {
            if(this.size() == 0) {
                return null;
            } else {
                return this.getLast();
            }
        }

        public T lastWhere(Predicate<? super T> predicate){
            for (T t : this.reversed()) {
                if(predicate.test(t)) {
                    return t;
                }
            }
            return null;
        }

        public ResultList<T> copy() {
            return new ResultList<>(this);
        }
    }

    //================================================================================================
    // Stuff to clean up java's checked exception garbage
    //================================================================================================

    private static void safely(RunnableWithException callable) {
        try {
            callable.run();
        } catch (Exception e) {
            rethrow(e);
        }
    }

    private static <T> T safely(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            rethrow(e);
            return null;
        }
    }

    private static void rethrow(Exception e) {
        FORCE_THROWER.throwException(e);
    }

    public interface ForceThrower {
        void throwException(Throwable throwable);
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

    private static ForceThrower generateForceThrower() {
        var tmpClass = new ClassLoader(GrugORM.class.getClassLoader()) {
            public Class defineClass() {
                byte[] bytes = Base64.getDecoder().decode("yv66vgAAADQAEAEAGGdydWcvZGIvRm9yY2VUaHJvd2VySW1wbAcAAQEAEGphdmEvbGFuZy9PYmplY3QHAAMBABxncnVnL2RiL0dydWdPUk0kRm9yY2VUaHJvd2VyBwAFAQAVRm9yY2VUaHJvd2VySW1wbC5qYXZhAQAGPGluaXQ+AQADKClWDAAIAAkKAAQACgEADnRocm93RXhjZXB0aW9uAQAYKExqYXZhL2xhbmcvVGhyb3dhYmxlOylWAQAEQ29kZQEAClNvdXJjZUZpbGUAIQACAAQAAQAGAAAAAgABAAgACQABAA4AAAARAAEAAQAAAAUqtwALsQAAAAAAAQAMAA0AAQAOAAAADgABAAIAAAACK78AAAAAAAEADwAAAAIABw==");
                return defineClass("grug.db.ForceThrowerImpl", bytes, 0, bytes.length);
            }
        }.defineClass();
        try {
            //noinspection deprecation
            return (ForceThrower) tmpClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}