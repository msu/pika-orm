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
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "UnusedReturnValue", "UnnecessaryLocalVariable"})
public class GrugORM {

    public static final int INSERT_FAILED = -1;

    public static final String SQL_VARS_PATTERN = "(:[\\w][\\d\\w]*)";

    private static GrugORM DEFAULT_ORM = null;

    private static final ThreadLocal<ConnectionInfo> CURRENT_CONNECTION = new ThreadLocal<>();

    private static final ForceThrower FORCE_THROWER = generateForceThrower();

    private final Callable<Connection> connectionSource;

    // Logger stuff
    private GrugLogger.Level internalLoggerLevel = GrugLogger.Level.INFO;
    private GrugLogger logger = new DefaultLogger();
    private boolean logQueries = false;

    // Mapping stuff
    private final ConcurrentHashMap<Class, Mapping> mappings = new ConcurrentHashMap<Class, Mapping>();

    // Default mapping logic
    private Function<Class, String> defaultClassToTableMapping = aClass -> snakeCase(aClass.getSimpleName());
    private Function<Field, String> defaultFieldToColumnMapping = field -> snakeCase(field.getName());
    private Function<Class, String> defaultIdFieldName = aClass -> "id";
    private Function<Class, String> defaultFkColumnName = aClass -> snakeCase(aClass.getSimpleName()) + "_id";
    private Function<Class, String> defaultVersionFieldName = aClass -> "version";
    private Function<Class, Function<Object, Object>> defaultVersionIncrementer = aClass -> (previousValue) -> {
        if(previousValue == null) {
            return 1;
        } else {
            return ((Long) previousValue) + 1;
        }
    };

    // paging configuration
    private int defaultPageSize = 20;
    private String limitOffsetClause = "LIMIT {0} OFFSET {1}";

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

    public GrugORM withDefaultTableMapping(Function<Class, String> val) {
        defaultClassToTableMapping = val;
        return this;
    }

    public GrugORM withDefaultColumnMapping(Function<Field, String> val) {
        defaultFieldToColumnMapping = val;
        return this;
    }

    public GrugORM withDefaultIdField(Function<Class, String> val) {
        defaultIdFieldName = val;
        return this;
    }

    public GrugORM withDefaultFkColumn(Function<Class, String> val) {
        defaultFkColumnName = val;
        return this;
    }

    public GrugORM withDefaultVersionColumnName(Function<Class, String> val) {
        defaultVersionFieldName = val;
        return this;
    }

    public GrugORM withDefaultVersionIncrementer(Function<Class, Function<Object, Object>> val) {
        defaultVersionIncrementer = val;
        return this;
    }

    public GrugORM withNoDefaultVersionColumn() {
        defaultVersionFieldName = _ -> null;
        return this;
    }

    public GrugORM withDefaultPageSize(int pageSize) {
        this.defaultPageSize = pageSize;
        return this;
    }

    public GrugORM withOffsetClause(String offsetClause) {
        this.limitOffsetClause = offsetClause;
        return this;
    }

    public GrugORM logQueries() {
        this.logQueries = true;
        return this;
    }

    public GrugORM makeDefaultORM() {
        setDefaultORM(this);
        return this;
    }

    private GrugLogger getLogger() {
        return logger;
    }

    //====================================================================
    // default orm management
    //====================================================================

    public static GrugORM get() {
        GrugORM defaultORM = getDefault();
        if (defaultORM == null) {
            throw new IllegalStateException("No default GrugORM found");
        }
        return defaultORM;
    }

    private static GrugORM getDefault() {
        return DEFAULT_ORM;
    }

    public static void setDefaultORM(GrugORM orm) {
        DEFAULT_ORM = orm;
    }

    //====================================================================
    // 1-N and N-1 functionality
    //====================================================================

    public <T> ResultList<T> loadN(Object parent, Class<T> classOfN) {
        Mapping mapping = getMapping(parent.getClass());
        String fkName = mapping.getDefaultForeignKeyColumnName();
        return loadN(parent, classOfN, fkName);
    }

    public <T> ResultList<T> loadN(Object parent, Class<T> classOfN, String foreignKeyColumnOnN) {
        Mapping mapping = getMapping(parent.getClass());
        Object ownerPkValue = mapping.getId(parent);
        return findAllBy(classOfN, foreignKeyColumnOnN, ownerPkValue);
    }

    public <T> T load1(Object child, Class<T> classOfParent) {
        Mapping mapping = getMapping(classOfParent);
        String fkName = mapping.getDefaultForeignKeyColumnName();
        return load1(child, classOfParent, fkName);
    }

    public <T> T load1(Object child, Class<T> classOfParent, String foreignKeyColumn) {
        Mapping metaData = getMapping(child.getClass());
        Object parentPkValue = metaData.getValueForColumn(child, foreignKeyColumn);
        return find(classOfParent, parentPkValue);
    }

    public <T> GrugFinder<T> finder(Class<T> classToFind) {
        return new GrugFinder<>(classToFind);
    }

    public class GrugFinder<T>  {
        Class<T> classToFind;
        public GrugFinder(Class<T> classToFind) {
            this.classToFind = classToFind;
        }
        public T byId(Object id) {
            return get().find(classToFind, id);
        }
        public T byKey(String col, Object value) {
            return get().find(classToFind, col, value);
        }
        public ResultList<T> all() {
            return get().findAll(classToFind);
        }
        public ResultList<T> where(String whereClause, Map<String, Object> args) {
            return get().findWhere(classToFind, whereClause, args);
        }
        public ResultList<T> bySQL(String sql, Map<String, Object> args) {
            return get().select(sql, args, classToFind);
        }
        public GrugQuery<T> byQuery() {
            return get().query(classToFind);
        }
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

        public void startTransaction() {
            if (transactionCount == 0) {
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
                    safely(() -> conn.setAutoCommit(true));
                }
                close();
            } else {
                logger.log(GrugLogger.Level.ERROR, "No current transaction for connection {}", uuid);
            }
        }
    }

    private Connection createConnection() {
        return safely(connectionSource);
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
            throw rethrow(e);
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
        Mapping mapping = getMapping(clazz);
        return find(clazz, mapping.getIdColumn(), pk);
    }

    public <T> T find(Class<T> clazz, String column, Object val) {
        Mapping mapping = getMapping(clazz);
        String sql = "SELECT * FROM " + mapping.getTableName() + " WHERE " + column + "=?";
        logger.log(getQueryLogLevel(), "Find SQL: {}\n  Arg:{}", sql, val);
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement ps = conn.prepareStatement(sql);
            int parameterIndex = 1;
            setValueForQuery(ps, parameterIndex, val);
            ResultSet resultSet = time(ps::executeQuery);
            if (resultSet.next()) {
                T obj = mapping.newObjectFromResult(resultSet);
                if (obj instanceof GrugRecordLifecycle lifecycle) {
                    lifecycle.afterSelect();
                }
                return obj;
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in find() with SQL {} & values {}: {}", sql, val, e.getMessage());
            throw rethrow(e);
        }
    }

    public <T> ResultList<T> findAll(Class<T> clazz) {
        return findWhere(clazz, "true=true", Map.of());
    }

    public <T> ResultList<T> findAllBy(Class<T> clazz, String column, Object val) {
        return findWhere(clazz, column + "=:val ", Map.of("val", val));
    }

    public <T> ResultList<T> findWhere(Class<T> clazz, String whereClause, Map<String, Object> args) {
        Mapping metaData = getMapping(clazz);
        String tableName = metaData.getTableName();
        String selectClause = "SELECT * FROM " + tableName + " WHERE ";
        String sql = selectClause + whereClause;
        return select(sql, args, clazz);
    }

    public <T> ResultList<T> select(String query, Class<T> resultClass) {
        return select(query, Map.of(), resultClass);
    }

    public ResultList<ResultMap> selectRaw(String sql, Map<String, Object> args) {
        return select(sql, args, ResultMap.class);
    }

    public <T> ResultList<T> select(String sql, Map<String, Object> args, Class resultClass) {
        Mapping mapping = getMapping(resultClass);
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            ArrayList<Object> vals = new ArrayList<>();
            String updatedSql = updateSqlVars(sql, args, vals);//SQL, Argument Map, Blank Value list to be filled
            logger.log(getQueryLogLevel(), "Select SQL: {}\n  Args:{}", updatedSql, vals);
            PreparedStatement ps = conn.prepareStatement(updatedSql);
            for (int i = 0; i < vals.size(); i++) {
                Object val = vals.get(i);
                setValueForQuery(ps, i + 1, val);
            }
            ResultSet resultSet = time(ps::executeQuery);
            ResultList<T> result = new ResultList<>();
            while (resultSet.next()) {
                T object = mapping.newObjectFromResult(resultSet);
                if (object instanceof GrugRecordLifecycle lifecycle) {
                    lifecycle.afterSelect();
                }
                result.add(object);
            }
            return result;
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, """
                    Exception in select() with SQL
                    {}\s
                    with args {}: {}""", indent(2, sql), args, e.getMessage());
            throw rethrow(e);
        }
    }

    public long insert(Object object) {
        if (object instanceof GrugRecordLifecycle lifecycle) {
            if (!lifecycle.validate()) {
                return INSERT_FAILED;
            }
        }
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        String keyCol = mapping.getIdColumn();
        Map<String, Object> values = mapping.toDatabaseMap(object);
        values.remove(keyCol);
        if (object instanceof GrugRecordLifecycle lifecycle) {
            if (!lifecycle.beforeInsert()) {
                return INSERT_FAILED;
            }
        }
        Object newVersionValue = null;
        if (mapping.hasVersionColumn()) {
            newVersionValue = mapping.incrementVersion(values);
        }
        long id = insert(mapping.getTableName(), values);
        if (mapping.hasVersionColumn() && id != INSERT_FAILED) {
            mapping.updateVersionValue(object, newVersionValue);
        }
        if (object instanceof GrugRecordLifecycle lifecycle) {
            lifecycle.afterInsert();
        }
        if (!mapping.isReadOnly()) {
            mapping.setId(object, id);
        }
        return id;
    }

    public long[] insertAll(Object[] items) {
        return insertAll(List.of(items));
    }

    public long[] insertAll(Collection<Object> items) { // TODO - look into the setID as i was having some issues and weirdness with it
        long[] ids = new long[items.size()];
        int count = 0;
        for (Object o : items) {
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
            for (String _ : values.keySet()) {
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
        logger.log(getQueryLogLevel(), "INSERT SQL: {}\n  Args:{}", insertString, values.values());
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(insertString);
            int col = 1;
            for (Object o : values.values()) {
                setValueForQuery(preparedStatement, col++, o);
            }
            int updated = time(preparedStatement::executeUpdate);
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                return INSERT_FAILED;
            }
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in insert() with SQL {} & args {}: {}", insertString, values.values(), e.getMessage());
            throw rethrow(e);
        }
    }

    public boolean update(Object object) {
        if (object instanceof GrugRecordLifecycle lifecycle) {
            if (!lifecycle.validate()) {
                return false;
            }
        }
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        String tableName = mapping.getTableName();
        String keyCol = mapping.getIdColumn();
        Map<String, Object> valuesToUpdate = mapping.toDatabaseMap(object);
        Object keyVal = valuesToUpdate.remove(keyCol); // remove the key
        if (object instanceof GrugRecordLifecycle lifecycle) {
            if (!lifecycle.beforeUpdate()) {
                return false;
            }
        }
        String versionColumn = null;
        Object currentVersionValue = null;
        Object nextVersionValue = null;
        if (mapping.hasVersionColumn()) {
            versionColumn = mapping.getVersionColumn();
            currentVersionValue = mapping.getCurrentVersion(valuesToUpdate);
            nextVersionValue = mapping.incrementVersion(valuesToUpdate);
        }
        boolean update = update(tableName, keyCol, keyVal, versionColumn, currentVersionValue, valuesToUpdate);
        if(mapping.hasVersionColumn() && update) {
            mapping.updateVersionValue(object, nextVersionValue);
        }
        if (object instanceof GrugRecordLifecycle lifecycle) {
            lifecycle.afterUpdate();
        }
        return update;
    }

    private boolean update(String tableName, String keyCol, Object keyVal, String versionCol, Object verionVal, Map<String, Object> values) {
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
        if (versionCol != null) {
            sb.append(" AND ").append(versionCol).append("=?");
        }

        String updateSQL = sb.toString();
        logger.log(getQueryLogLevel(), "UPDATE SQL: {}\n  Args:{}", updateSQL, values.values());
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(updateSQL);
            int col = 1;
            for (Object o : values.values()) {
                setValueForQuery(preparedStatement, col++, o);
            }
            setValueForQuery(preparedStatement, col, keyVal);
            if (versionCol != null) {
                col++;
                setValueForQuery(preparedStatement, col, verionVal);
            }
            int i = time(preparedStatement::executeUpdate);
            return i == 1;
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in update() with SQL {} & args {}: {}", updateSQL, values.values(), e.getMessage());
            throw rethrow(e);
        }
    }

    public boolean delete(Object object) {
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        String tableName = mapping.getTableName();
        String keyCol = mapping.getIdColumn();
        Map<String, Object> valuesToUpdate = mapping.toDatabaseMap(object);
        Object keyVal = valuesToUpdate.get(keyCol);
        if (object instanceof GrugRecordLifecycle lifecycle) {
            if (!lifecycle.beforeDelete()) {
                return false;
            }
        }
        boolean delete = delete(tableName, keyCol, keyVal);
        if (object instanceof GrugRecordLifecycle lifecycle) {
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
        logger.log(getQueryLogLevel(), "DELETE SQL: {}\n  Args:{}", deleteSQL, List.of(keyVal));
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            PreparedStatement preparedStatement = conn.prepareStatement(sb.toString());
            setValueForQuery(preparedStatement, 1, keyVal);
            int i = time(preparedStatement::executeUpdate);
            return i == 1;
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in update() with SQL {} & value {}: {}", deleteSQL, keyVal, e.getMessage());
            throw rethrow(e);
        }
    }

    public void reload(Object object) {
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        Object fromDb = find(clazz, mapping.getIdColumn(), mapping.getId(object));
        mapping.copyValues(object, fromDb);
    }

    public boolean exec(String sql) {
        if (sql.isBlank()) {
            logger.log(GrugLogger.Level.WARN, "SQL is blank, will not be executed!");
            return false;
        }
        try (ConnectionInfo ci = getOrCreateConnectionInfo()) {
            Connection conn = ci.conn;
            logger.log(getQueryLogLevel(), "EXECUTING RAW SQL: {}\n", sql);
            //noinspection SqlSourceToSinkFlow
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            boolean execute = time(preparedStatement::execute);
            return execute;
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in exec() with SQL {}: {}", sql, e.getMessage());
            throw rethrow(e);
        }
    }

    // utilities
    private <T> T time(Callable<T> query) {
        long start = System.currentTimeMillis();
        try {
            try {
                return query.call();
            } catch (Exception e) {
                throw rethrow(e);
            }
        } finally {
            long end = System.currentTimeMillis();
            logger.log(getQueryLogLevel(), "Query took {}ms", end - start);
        }
    }

    private GrugLogger.Level getQueryLogLevel() {
        if (logQueries) {
            return GrugLogger.Level.INFO;
        } else {
            return GrugLogger.Level.DEBUG;
        }
    }

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
                throw new IllegalStateException("No value found for variable :" + match + " in " + args);
            }
        }
        matcher.appendTail(finalSql);
        return finalSql.toString();
    }

    public static String decapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    public static String indent(int spaces, String str) {
        return Arrays.stream(str.split("\n"))
                .map(s -> " ".repeat(spaces) + s )
                .collect(Collectors.joining("\n"));
    }
    public static String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
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
        Mapping mapping = getMapping(clazz);
        return query(mapping.getTableName()).select(clazz);
    }

    public GrugQuery<?> query(String baseTable) {
        return new GrugQuery<>(baseTable);
    }

    public interface Interfaces {

        interface GrugLogger {
            enum Level {
                ERROR, WARN, INFO, DEBUG, TRACE
            }
            void log(Level level, String msg, Object... args);
        }

        interface GrugRecordLifecycle {

            default boolean validate() {
                return true;
            }

            default boolean beforeInsert() {
                return true;
            }

            default boolean beforeUpdate() {
                return true;
            }

            default boolean beforeDelete() {
                return true;
            }

            default void afterInsert() {
            }

            default void afterSelect() {
            }

            default void afterUpdate() {
            }

            default void afterDelete() {
            }
        }
    }

    public static class EnterpriseGrugBean implements GrugRecordLifecycle {
        private transient boolean persisted;
        private final transient Map<String, List<String>> errors = new LinkedHashMap<>();

        protected static GrugORM orm() {
            return GrugORM.get();
        }

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }

        public void clearErrors() {
            errors.clear();
        }

        public void addError(String error) {
            var errorList = getErrorList(null);
            errorList.add(error);
        }

        public void addError(String field, String error) {
            var errorList = getErrorList(field);
            errorList.add(error);
        }

        public List<String> getClassErrors() {
            return getErrorList(null);
        }

        public List<String> getFieldErrors(String field) {
            return getErrorList(field);
        }

        private List<String> getErrorList(String key) {
            return errors.computeIfAbsent(key, _ -> new ArrayList<>());
        }

        public final boolean validate() {
            clearErrors();
            validation();
            return !hasErrors();
        }

        protected void validation() {
            // override in subclasses
        }

        @Override
        public void afterSelect() {
            this.persisted = true;
        }

        public long insert() {
            if (persisted) {
                throw new IllegalStateException("This record is already persisted!");
            }
            long id = orm().insert(this);
            this.persisted = true;
            return id;
        }

        public boolean update() {
            if (!persisted) {
                throw new IllegalStateException("This record has not been persisted!");
            }
            return orm().update(this);
        }

        public boolean save() {
            if (persisted) {
                return update();
            } else {
                return insert() > 0;
            }
        }

        public boolean delete() {
            return orm().delete(this);
        }

        protected <T> ResultList<T> loadN(Class<T> of) {
            return orm().loadN(this, of);
        }

        protected <T> T load1(Class<T> of) {
            return orm().load1(this, of);
        }

        public void reload() {
            orm().reload(this);
        }
    }

    public class GrugQuery<T> {

        private final String baseTable;
        private Class<?> resultClass;
        private Set<String> tableNames = new HashSet<>();
        private final StringBuilder whereClause = new StringBuilder();
        private final Map<String, Object> valMap = new TreeMap<>();
        private final List<String> joins = new ArrayList<>();
        private final List<String> orderBys = new ArrayList<>();
        private boolean orderByDesc = false;

        private int pageSize = -1;
        private int page = -1;


        public GrugQuery(String tableName) {
            this.baseTable = tableName;
            tableNames.add(tableName);
        }

        public GrugQuery<T> where(String condition) {
            if (!whereClause.isEmpty()) {
                whereClause.append(" AND ");
            }
            whereClause.append(condition);
            return this;
        }

        public <Q> GrugQuery<Q> select(Class<Q> clazz) {
            this.resultClass = clazz;
            //noinspection unchecked
            return (GrugQuery<Q>) this;
        }

        public ResultList<T> execute() {
            String sql = generateSQL();
            return GrugORM.this.select(sql, valMap, resultClass);
        }

        private String generateSQL() {
            String sql = "SELECT * FROM " + baseTable;
            if(!joins.isEmpty()) {
                sql += "\n" + String.join("\n", joins);
            }
            if(!whereClause.isEmpty()) {
                sql += "\nWHERE " + whereClause;
            }
            if (!orderBys.isEmpty()){
                sql += "\nORDER BY " + String.join(", ", orderBys);
                if(orderByDesc){
                    sql += " DESC";
                }
            }
            if (page != -1) {
                int limit;
                if (pageSize == -1) {
                    limit = defaultPageSize;
                } else {
                    limit = pageSize;
                }
                int offset = (page - 1) * limit;
                sql += "\n" + MessageFormat.format(limitOffsetClause, limit, offset);
            } else if (pageSize != -1) {
                int offset = 0;
                int limit = pageSize;
                sql += "\n" + MessageFormat.format(limitOffsetClause, limit, offset);
            }
            return sql;
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

        //TODO - add some sort of parameterization for joins
        public GrugQuery<T> join(Class owner, Class owned) {//maybe do some sort of parameter for outer or more spesific joins
            Mapping ownerMapping = getMapping(owner);
            Mapping ownedMapping = getMapping(owned);
            String ownerTable = ownerMapping.getTableName();//artist
            String ownedTable = ownedMapping.getTableName();//album
            String ownerIdColumn = ownerMapping.getIdColumn();//artistId
            String fkColumn = ownerMapping.getDefaultForeignKeyColumnName();
            String joinBuilder = "JOIN " + ownerTable + " ON " + ownerTable + "." + ownerIdColumn + " = " + ownedTable + "." + fkColumn;
            // join artists on artists.ArtistId = albums.ArtistId is the desired test string
            return join(joinBuilder);
        }

        public GrugQuery<T> join(String joinSql) {
            if(!joinSql.contains("JOIN")) {
                joinSql = "JOIN " + joinSql;
            }
            this.joins.add(joinSql);
            return this;
        }

        public GrugQuery<T> orderBy(String[] columns, Boolean desc) {//this orderby is for if you would like to control if the orderby is desc (Default ascending)
            Collections.addAll(this.orderBys, columns);
            if(desc){
                this.orderByDesc = true;
            }
            return this;
        }


        public GrugQuery<T> orderBy(String... columns) {//this orderby is just if you dont care about ASC|DESC and just want to put a bunch of columns
            Collections.addAll(this.orderBys, columns);
            return this;
        }
        public GrugQuery<T> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public GrugQuery<T> page(int page) {
            this.page = page;
            return this;
        }

        public String toString() {
            return generateSQL() + "\nVals:" + this.valMap;
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
                if (level.ordinal() == Level.ERROR.ordinal()) {
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
        if (val == null) {
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

    private static Object getValueFromResultSet(String columnName, Class targetType, ResultSet resultSet) {
        Object fieldVal = null;
        try {
            if (targetType == String.class) {
                fieldVal = resultSet.getString(columnName);
            } else if (targetType == Integer.class || targetType == int.class) {
                fieldVal = resultSet.getInt(columnName);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                fieldVal = resultSet.getBoolean(columnName);
            } else if (targetType == Long.class || targetType == long.class) {
                fieldVal = resultSet.getLong(columnName);
            } else if (targetType == Double.class || targetType == double.class) {
                fieldVal = resultSet.getDouble(columnName);
            } else if (targetType.isEnum()) {
                // enums deserialize as strings
                String strValue = resultSet.getString(columnName);
                fieldVal = Enum.valueOf(targetType, strValue);
            } else if (targetType == Date.class) {
                Timestamp timestamp = resultSet.getTimestamp(columnName);
                if (timestamp != null) {
                    fieldVal = new Date(timestamp.getTime());
                }
            } else {
                fieldVal = resultSet.getObject(columnName, targetType);
            }
        } catch (SQLException e) {
            throw rethrow(e);
        }
        return fieldVal;
    }

    private Mapping getMapping(Class<?> clazz) {
        return mappings.computeIfAbsent(clazz, aClass -> {
            Mapping mapping = new Mapping();
            mapping.setOrm(this);
            mapping.setClass(aClass);
            return mapping;
        });
    }

    public void withMapping(Class classToMap, Mapping mapping) {
        mapping.setOrm(this);
        mapping.setClass(classToMap);
        mappings.put(classToMap, mapping);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public static class Mapping {

        GrugORM orm;
        private Class classForTable;
        private RecordComponent[] recordComponents;
        private String tableName;
        private Map<String, FieldMapping> fieldNameToMapping;
        private Map<String, FieldMapping> columnToMapping;
        private FieldMapping idMapping;
        private Constructor constructor;
        private FieldMapping versionMapping;

        protected Mapping() {}

        public void setOrm(GrugORM orm) {
            this.orm = orm;
        }

        protected void setClass(Class aClass) {
            this.classForTable = aClass;
            if (aClass == ResultMap.class) {
                return; // special case
            } else {
                this.tableName = mapToTable();
                fieldNameToMapping = new LinkedHashMap<>();
                columnToMapping = new LinkedHashMap<>();
                for (Field field : getAllFields(aClass)) {
                    FieldMapping fieldMapping = mapField(field);
                    if (fieldMapping != null) {
                        fieldNameToMapping.put(fieldMapping.getFieldName(), fieldMapping);
                        columnToMapping.put(fieldMapping.getColumnName(), fieldMapping);
                    }
                }
            }

            if (classForTable.isRecord()) {
                recordComponents = classForTable.getRecordComponents();
                Constructor[] constructors = classForTable.getDeclaredConstructors();
                constructor = constructors[0];
            } else {
                recordComponents = null;
                Constructor[] constructors = classForTable.getDeclaredConstructors();
                for (Constructor c : constructors) {
                    if (c.getParameterTypes().length == 0) {
                        c.setAccessible(true);
                        this.constructor = c;
                        break;
                    }
                }
                if (constructor == null) {
                    throw new IllegalStateException("Class " + classForTable.getName() + " does not have an empty constructor, please add one.  It can be private.");
                }
            }

            idMapping = resolveIdMapping();
            versionMapping = resolveVersionMapping();
        }

        private FieldMapping resolveVersionMapping() {
            FieldMapping versionMapping = null;
            for (FieldMapping mapping : fieldNameToMapping.values()) {
                if (mapping.isVersionProperty()) {
                    if(versionMapping == null) {
                        versionMapping = mapping;
                    } else {
                        throw new IllegalStateException("Cannot have more than one field as the version column: " + versionMapping.getFieldName() +
                                " and " + mapping.getFieldName() + " are both ids!");
                    }
                }
            }
            if(versionMapping == null) {
                String versionFieldName = orm.defaultVersionFieldName.apply(classForTable);
                versionMapping = fieldNameToMapping.get(versionFieldName);
            }
            return versionMapping;
        }

        private FieldMapping resolveIdMapping() {
            FieldMapping idMapping = null;
            for (FieldMapping mapping : fieldNameToMapping.values()) {
                if (mapping.isId()) {
                    if(idMapping == null) {
                        idMapping = mapping;
                    } else {
                        throw new IllegalStateException("Cannot have more than one field as the id column: " + idMapping.getFieldName() +
                                " and " + mapping.getFieldName() + " are both ids!");
                    }
                }
            }
            if(idMapping == null) {
                String idFieldName = orm.defaultIdFieldName.apply(classForTable);
                idMapping = fieldNameToMapping.get(idFieldName);
            }
            return idMapping;
        }

        protected final FieldMapping ignore(Field field) {
            return null;
        }

        protected final FieldMapping map(Field field) {
            return new FieldMapping(orm, field);
        }

        protected final FieldMapping defaultMapping(Field field) {
            if (shouldIgnore(field)) {
                return ignore(field);
            } else {
                return map(field);
            }
        }

        protected FieldMapping mapField(Field field) {
            return defaultMapping(field);
        }

        public String mapToTable() {
            return orm.defaultClassToTableMapping.apply(classForTable);
        }

        private static List<Field> getAllFields(Class aClass) {
            List<Field> fieldsToReturn = new ArrayList<>();
            while (aClass != null) {
                Field[] fields = aClass.getDeclaredFields();
                fieldsToReturn.addAll(Arrays.asList(fields));
                aClass = aClass.getSuperclass();
            }
            return fieldsToReturn;
        }

        public final String getTableName() {
            return this.tableName;
        }

        public Map<String, Object> toDatabaseMap(Object object) {
            Map<String, Object> values = new TreeMap<>();
            for (FieldMapping mapping : fieldNameToMapping.values()) {
                String columnName = mapping.getColumnName();
                Object value = mapping.getValueForDatabaseFrom(object);
                values.put(columnName, value);
            }
            return values;
        }

        @SuppressWarnings({"unchecked"})
        public <T> T newObjectFromResult(ResultSet resultSet) throws Exception {
            if (classForTable == ResultMap.class) {
                ResultMap resultMap = new ResultMap();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int i = metaData.getColumnCount();
                for (int j = 1; j <= i; j++) {
                    String columnName = metaData.getColumnName(j);
                    Object value = resultSet.getObject(columnName);
                    resultMap.put(columnName, value);
                }
                return (T) resultMap;
            } else {
                T object;
                // if it's a record use the generated constructor
                if (recordComponents != null) {
                    Object[] args = new Object[recordComponents.length];
                    for (int i = 0; i < recordComponents.length; i++) {
                        RecordComponent recordComponent = recordComponents[i];
                        FieldMapping mapping = fieldNameToMapping.get(recordComponent.getName());
                        Object val = mapping.getValueFromDatabase(resultSet);
                        args[i] = val;
                    }
                    object = (T) constructor.newInstance(args);
                } else {
                    // otherwise use fields
                    object = (T) constructor.newInstance();
                        for (FieldMapping fieldMapping : fieldNameToMapping.values()) {
                            try {
                            fieldMapping.mapFromDatabase(object, resultSet);
                            } catch (Exception e) {
                                orm.getLogger().log(GrugLogger.Level.ERROR, "Could not map field {} on {}, available columns:{}, error:{}",
                                        fieldMapping.getFieldName(), classForTable.getSimpleName(), getColumns(resultSet), e.getMessage());
                                throw rethrow(e);
                            }
                        }
                }
                return object;
            }
        }

        private String getColumns(ResultSet resultSet) {
            try {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                String cols = "[";
                for (int i = 1; i <= columnCount; i++) {
                    cols += metaData.getColumnName(i);
                    if (i < columnCount) {
                        cols += ",";
                    }
                }
                cols += "]";
                return cols;
            } catch (SQLException e) {
                throw rethrow(e);
            }
        }

        protected boolean shouldIgnore(Field field) {
            return Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers());
        }

        public void setId(Object object, long id) {
            getIdMapping().setFieldValue(object, id);
        }

        public Object getId(Object object) {
            return getIdMapping().getFieldValue(object);
        }

        public boolean isReadOnly() {
            return classForTable.isRecord();
        }

        public String getIdColumn() {
            return getIdMapping().getColumnName();
        }

        private FieldMapping getIdMapping() {
            if(idMapping == null) {
                throw new IllegalStateException("The class " + classForTable.getName() + " has no id column");
            } else {
                return idMapping;
            }
        }

        public Object getValueForColumn(Object child, String foreignKeyColumn) {
            FieldMapping mapping = columnToMapping.get(foreignKeyColumn);
            return mapping.getFieldValue(child);
        }

        public String getDefaultForeignKeyColumnName() {
            return orm.defaultFkColumnName.apply(classForTable);
        }

        public Object incrementVersion(Map<String, Object> valuesToUpdate) {
            return versionMapping.incrementVersion(valuesToUpdate);
        }

        public boolean hasVersionColumn() {
            return versionMapping != null;
        }

        public Object getCurrentVersion(Map<String, Object> values) {
            return versionMapping.getValueFromDBMap(values);
        }

        public String getVersionColumn() {
            return versionMapping.getColumnName();
        }

        public void updateVersionValue(Object object, Object nextVersionValue) {
            versionMapping.updateVersionValue(object, nextVersionValue);
        }

        public void copyValues(Object to, Object from) {
            for (FieldMapping mapping : fieldNameToMapping.values()) {
                mapping.setFieldValue(to, mapping.getFieldValue(from));
            }
        }
    }

    public static class FieldMapping {
        GrugORM orm;
        Field mappedField;
        String columnName;
        boolean idColumn;
        boolean versionColumn;
        Function<Object, Object> toDatabaseValue;
        Function<Object, Object> fromDatabaseValue;
        Class dbStorageType;
        Function<Object, Object> versionInrementer;

        public FieldMapping(GrugORM orm, Field mappedField) {
            mappedField.setAccessible(true);
            this.orm = orm;
            this.mappedField = mappedField;
            this.columnName = orm.defaultFieldToColumnMapping.apply(mappedField);
            this.versionInrementer = orm.defaultVersionIncrementer.apply(mappedField.getDeclaringClass());
            this.dbStorageType = mappedField.getType();
        }
        public String getFieldName() {
            return mappedField.getName();
        }

        public String getColumnName() {
            return columnName;
        }

        public Field getField() {
            return mappedField;
        }

        public Object getValueForDatabaseFrom(Object object) {
            Object o = safely(() -> mappedField.get(object));
            if (toDatabaseValue != null) {
                o = toDatabaseValue.apply(o);
            }
            return o;
        }

        public void setFieldValue(Object object, Object val) {
            safely(() -> mappedField.set(object, val));
        }

        public Object getFieldValue(Object object) {
            return safely(() -> mappedField.get(object));
        }

        public void mapFromDatabase(Object object, ResultSet resultSet) {
            Object fromDb = getValueFromDatabase(resultSet);
            setFieldValue(object, fromDb);
        }

        private Object getValueFromDatabase(ResultSet resultSet) {
            Object value = getValueFromResultSet(columnName, dbStorageType, resultSet);
            if (fromDatabaseValue != null) {
                value = fromDatabaseValue.apply(value);
            }
            return value;
        }

        public FieldMapping asId() {
            this.idColumn = true;
            return this;
        }

        public FieldMapping asVersionColumn() {
            this.versionColumn = true;
            return this;
        }

        public FieldMapping withVersionIcrementer(Function<Object, Object> versionIncrementer) {
            this.versionInrementer = versionIncrementer;
            return this;
        }

        public FieldMapping toColumn(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public FieldMapping asType(Class<String> dbClass) {
            this.dbStorageType = dbClass;
            return this;
        }

        public FieldMapping transformForDB(Function<Object, Object> func) {
            this.toDatabaseValue = func;
            return this;
        }

        public FieldMapping transformFromDB(Function<Object, Object> func) {
            this.fromDatabaseValue = func;
            return this;
        }

        public boolean isId() {
            return idColumn;
        }

        public boolean isVersionProperty() {
            return versionColumn;
        }

        public Object incrementVersion(Map<String, Object> values) {
            Object value = getValueFromDBMap(values);
            Object updatedValue = versionInrementer.apply(value);
            values.put(columnName, updatedValue);
            return updatedValue;
        }

        public Object getValueFromDBMap(Map<String, Object> values) {
            return values.get(columnName);
        }

        public void updateVersionValue(Object object, Object nextVersionValue) {
            safely(() -> mappedField.set(object, nextVersionValue));
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
        private LinkedHashMap<String, GrugMigration> migrations;
        private GrugORM orm;

        public void setORM(GrugORM orm) {
            this.orm = orm;
        }

        public GrugORM getORM() {
            if (orm != null) {
                return orm;
            }
            if (GrugORM.getDefault() != null) {
                return GrugORM.getDefault();
            }
            throw new IllegalStateException("ORM has not been set and there is no default ORM, don't know what database to migrate!");
        }

        protected void add(Supplier<GrugMigration> migrationCallable) {
            add(migrationCallable.get());
        }

        protected void add(GrugMigration migration) {
            String migrationName = migration.getName();
            if (migrations.containsKey(migrationName)) {
                throw new IllegalArgumentException("Migration " + migrationName + " already exists!");
            }
            migrations.put(migrationName, migration);
        }

        /**
         * @return the initial pre-migrations schema for the database
         */
        protected String initialSchema() {
            return "";
        }

        public abstract void migrations();

        public static GrugMigration makeMigration(String name) {
            GrugMigration migration = new GrugMigration(name);
            return migration;
        }

        public void console() {
            GrugORM orm = getORM();
            orm.exec(GrugMigration.DDL);
            Console console = System.console();
            label:
            while (true) {
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
            orm.exec(GrugMigration.DDL);
            var mergedMigrations = loadMigrations(orm);

            StringBuilder sb = new StringBuilder("All Migrations:\n");
            String formatString = "%-30.30s | %-15.15s | %-30.30s | %-30.30s | %-30.30s | %-30.30s\n";
            sb.append(String.format(formatString, "name", "status", "applied", "description", "up", "down"));
            sb.append("-------------------------------------------------------------------------------------------------------------------------------------------------------\n");
            for (GrugMigration value : mergedMigrations.values()) {
                sb.append(String.format(formatString,
                        value.getName(), value.getStatus(), value.appliedAtForDisplay(), value.description, value.upForDisplay(), value.downForDisplay()));
            }
            return sb.toString();
        }

        public void up() {
            GrugORM orm = getORM();
            orm.exec(GrugMigration.DDL);
            var mergedMigrations = loadMigrations(orm);

            var values = new ResultList<>(mergedMigrations.values());
            var firstUnappliedMigration = values.firstWhere(GrugMigration::isPending);
            if (firstUnappliedMigration != null) {
                firstUnappliedMigration.runUp(orm);
            } else {
                orm.getLogger().log(GrugLogger.Level.WARN, "No pending migrations were found in migrations file to apply");
            }
        }

        public void down() {
            GrugORM orm = getORM();
            orm.exec(GrugMigration.DDL);
            var mergedMigrations = loadMigrations(orm);

            var values = new ResultList<>(mergedMigrations.values());
            var lastAppliedMigration = values.lastWhere(GrugMigration::isApplied);
            if (lastAppliedMigration != null) {
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
            orm.exec(GrugMigration.DDL);
            var mergedMigrations = loadMigrations(orm);
            for (GrugMigration migration : mergedMigrations.values()) {
                if (!migration.isApplied()) {
                    migration.runUp(orm);
                }
            }
        }

        private LinkedHashMap<String, GrugMigration> loadMigrations(GrugORM orm) {

            migrations = new LinkedHashMap<>();
            migrations();
            // compute migrations with persisted migrations merged in
            ResultList<GrugMigration> persistedMigrations = orm.findAll(GrugMigration.class);
            var mergedMigrations = new LinkedHashMap<>(migrations);
            for (GrugMigration persistedMigration : persistedMigrations.copy()) {
                GrugMigration existingMigration = mergedMigrations.get(persistedMigration.getName());
                if (existingMigration != null) {
                    if (!existingMigration.equals(persistedMigration)) {
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

            if (!persistedMigrations.isEmpty()) {
                orm.getLogger().log(GrugLogger.Level.WARN,
                        "The following migrations have been found in the database, but are not in the current migration file:\n" +
                                persistedMigrations.join("\n"));
            }
            return mergedMigrations;
        }

        public static final class GrugMigration {

            public static final String DDL = """
                    CREATE TABLE IF NOT EXISTS grug_migration (
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

            private GrugMigration() {
            }

            public GrugMigration(String name) {
                this.name = name;
            }

            public GrugMigration description(String description) {
                this.description = description;
                return this;
            }

            public GrugMigration up(String up) {
                this.up = up;
                return this;
            }

            public GrugMigration down(String down) {
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
                GrugMigration migration = (GrugMigration) o;
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
                return "Migration{id=%d, appliedAt=%d, name='%s', description='%s', up='%s', down='%s', status=%s}".formatted(id, appliedAt, name, description, up, down, status);
            }

            public Object getDebugString() {
                return "{down='%s', up='%s'}".formatted(down, up);
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
                if (appliedAt == null) {
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

    //========================================================================================
    // GrugORM Results Objects
    //========================================================================================

    public static class ResultMap extends LinkedHashMap<String, Object> {
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            return (T) this.get(key);
        }
    }

    public static class ResultList<T> extends ArrayList<T> {

        public ResultList() {
        }

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
                if (filter.test(t)) {
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
                if (i < thisSize - 1) {
                    builder.append(separator);
                }
            }
            return builder.toString();
        }

        public T first() {
            if (this.size() > 0) {
                return this.get(0);
            } else {
                return null;
            }
        }

        public T firstWhere(Predicate<? super T> predicate) {
            for (T t : this) {
                if (predicate.test(t)) {
                    return t;
                }
            }
            return null;
        }

        public T last() {
            if (this.size() == 0) {
                return null;
            } else {
                return this.getLast();
            }
        }

        public T lastWhere(Predicate<? super T> predicate) {
            for (T t : this.reversed()) {
                if (predicate.test(t)) {
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
            throw rethrow(e);
        }
    }

    private static <T> T safely(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    private static RuntimeException rethrow(Exception e) {
        FORCE_THROWER.throwException(e);
        return new RuntimeException(e); // never hit
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