package grug.db;

import grug.db.GrugORM.Interfaces.GrugLogger;
import grug.db.GrugORM.Interfaces.GrugRecordLifecycle;
import grug.db.GrugORM.Interfaces.SafeAutoCloseable;

import java.io.Console;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GrugORM {

    public static final int INSERT_FAILED = -1;

    public static final String SQL_VARS_PATTERN = "(:[\\w][\\w]*)";

    private static GrugORM DEFAULT_ORM = null;

    // TODO Do we need a remove() call on this at some point?
    private static final ThreadLocal<ConnectionSession> CURRENT_SESSION = new ThreadLocal<>();

    private static final Consumer<Exception> FORCE_THROWER = generateForceThrower();

    private final Callable<Connection> connectionSource;

    // Logger stuff
    private GrugLogger.Level internalLoggerLevel = GrugLogger.Level.INFO;
    private GrugLogger logger = new DefaultLogger();
    private boolean logQueries = false;

    // Mapping stuff
    private final ConcurrentHashMap<Class, Mapping> mappings = new ConcurrentHashMap<>();

    // Coercions
    public static final Object NULL_SENTINEL = new Object();
    List<BiFunction<Class, Object, Object>> coercers = new ArrayList<>();

    // Default mapping logic
    private Function<Class, String> defaultClassToTableMapping = aClass -> {
        String className = aClass.getSimpleName();
        String plural = TextTools.pluralize(className);
        return TextTools.snakeCase(plural);
    };
    private Function<Field, String> defaultFieldToColumnMapping = field -> TextTools.snakeCase(field.getName());
    private Function<Class, String> defaultIdFieldName = aClass -> "id";
    private Function<Class, String> defaultFkColumnName = aClass -> TextTools.snakeCase(aClass.getSimpleName()) + "_id";
    private Function<Class, String> defaultVersionFieldName = aClass -> "version";
    private Function<Class, Function<Object, Object>> defaultVersionIncrementer = aClass -> previousValue -> {
        if (previousValue == null) {
            return 1;
        } else {
            return ((Long) previousValue) + 1;
        }
    };

    // paging configuration
    private int defaultPageSize = 20;
    private String limitOffsetClause = "LIMIT {0} OFFSET {1}";

    // SQLite Quirks
    private boolean sqlLiteQuirks = false;

    // associated migrations file
    private Migrations migrations;

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
        this.migrations = migrations;
        return this;
    }

    public GrugORM applyMigrations() {
        if (migrations != null) {
            migrations.applyAll();
        }
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

    public GrugORM withSQLiteQuirks() {
        this.sqlLiteQuirks = true;
        return this;
    }

    public GrugORM withCoercion(BiFunction<Class, Object, Object> coercion) {
        coercers.add(coercion);
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
    // Coercion System
    //====================================================================

    public Object coerce(Class targetClass, Object value) {
        for (BiFunction<Class, Object, Object> coercer : coercers) {
            Object result = coercer.apply(targetClass, value);
            if (result != null) {
                if (result == NULL_SENTINEL) {
                    result = null;
                }
                return targetClass.cast(result);
            }
        }
        Object result;
        result = defaultCoercions(targetClass, value);
        if(result == null) {
            throw new IllegalArgumentException("No coercions found from object of type " +
                    value.getClass().getSimpleName() + " with value " + value + " to class " +
                    targetClass.getSimpleName());
        }
        return result;
    }

    private Object sloppyCoerce(Class targetClass, Object value) {
        try {
            return coerce(targetClass, value);
        } catch (Exception e) {
            if (!(value instanceof String)) {
                try {
                    // return as string
                    return coerce(targetClass, String.valueOf(value));
                } catch (Exception _) {
                    // ignore, rethrow original exception
                }
            }
            throw rethrow(e);
        }
    }

    private Object defaultCoercions(Class targetType, Object value) {
        if(targetType.isInstance(value)) {
            return value;
        } else if (targetType.isEnum()) {
            return Enum.valueOf(targetType, String.valueOf(value));
        } else if (targetType == String.class) {
            return String.valueOf(value);
        } else if ((targetType == Short.class || targetType == short.class) && value instanceof String s) {
            return Short.valueOf(s);
        } else if ((targetType == Integer.class || targetType == int.class) && value instanceof String s) {
            return Integer.valueOf(s);
        } else if ((targetType == Long.class || targetType == long.class) && value instanceof String s) {
            return Long.valueOf(s);
        } else if ((targetType == Float.class || targetType == float.class) && value instanceof String s) {
            return Float.valueOf(s);
        } else if ((targetType == Double.class || targetType == double.class) && value instanceof String s) {
            return Double.valueOf(s);
        } else if (targetType == BigInteger.class && value instanceof String s) {
            return new BigInteger(s);
        } else if (targetType == BigDecimal.class && value instanceof String s) {
            return new BigDecimal(s);
        } else if (targetType == Date.class && value instanceof String s) {
            try {
                return new Date(Long.parseLong(s));
            } catch (NumberFormatException _) {
                // if the value is not a long, try to parse it as a date string
                return safely(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(s));
            }
        } else if (targetType == Boolean.class) {
            if (value == null) {
                return false;
            } else if (Boolean.FALSE.equals(value)) {
                return false;
            } else if (value instanceof String s) {
                return !"false".equalsIgnoreCase(s);
            } else if (value instanceof Number n && n.intValue() == 0) {
                return false;
            } else {
                return true;
            }
        }
        return null;
    }

    //====================================================================
    // One to Many & Many to One & Many to Many functionality
    //====================================================================

    public <T> QueryResult<T> loadManyThrough(Object initialObject, Class<?> joinClass, Class<T> classOfMany) {
        Mapping mapping = getMapping(initialObject.getClass());
        return query(classOfMany)
                .join(joinClass).thenJoin(initialObject.getClass())
                .where(mapping.tableName + "." + mapping.getIdColumn() + "=:id")
                .withVar("id", mapping.getId(initialObject))
                .fetch();
    }

    public <T> QueryResult<T> loadMany(Object objectOfOne, Class<T> classOfMany) {
        Mapping mapping = getMapping(objectOfOne.getClass());
        String fkName = mapping.getDefaultForeignKeyColumnName();
        return loadMany(objectOfOne, classOfMany, fkName);
    }

    public <T> QueryResult<T> loadMany(Object objectOfOne, Class<T> classOfMany, String foreignKeyColumnOnMany) {
        Mapping mapping = getMapping(objectOfOne.getClass());
        Object ownerPkValue = mapping.getId(objectOfOne);
        return find(classOfMany).allBy(foreignKeyColumnOnMany, ownerPkValue);
    }

    public <T> T load(Object objectWithFk, Class<T> classToLoad) {
        Mapping mapping = getMapping(classToLoad);
        String fkName = mapping.getDefaultForeignKeyColumnName();
        return load(objectWithFk, classToLoad, fkName);
    }

    public <T> T load(Object objectWithFk, Class<T> classToLoad, String foreignKeyColumn) {
        Mapping metaData = getMapping(objectWithFk.getClass());
        Object parentPkValue = metaData.getValueForColumn(objectWithFk, foreignKeyColumn);
        return find(classToLoad).byId(parentPkValue);
    }

    //====================================================================
    // Main entrypoints into the query layer
    //====================================================================

    public <T> GrugListFinder<T> find(Class<T> classToFind) {
        return new GrugListFinder<>(classToFind);
    }

    public <T> GrugStreamFinder<T> stream(Class<T> classToFind) {
        return new GrugStreamFinder<>(classToFind);
    }

    public <T> GrugClassQuery<T> query(Class<T> baseClass) {
        return new GrugClassQuery<>(baseClass);
    }

    public GrugQuery<ResultMap> queryBuilder(String baseTable) {
        return new GrugQuery<>(baseTable);
    }

    public class GrugListFinder<T> {
        Class<T> classToFind;

        public GrugListFinder(Class<T> classToFind) {
            this.classToFind = classToFind;
        }

        public T byId(Object id) {
            Mapping mapping = getMapping(classToFind);
            String column = mapping.getIdColumn();
            Mapping mapping1 = getMapping(classToFind);
            String sql = "SELECT * FROM " + mapping1.getTableName() + " WHERE " + column + "=:arg " + MessageFormat.format(limitOffsetClause, 1, 0);
            QueryResult<T> result = select(sql, Map.of("arg", id), classToFind);
            return result.first();
        }

        public T byKey(String col, Object value) {
            Mapping mapping = getMapping(classToFind);
            String sql = "SELECT * FROM " + mapping.getTableName() + " WHERE " + col + "=:arg " + MessageFormat.format(limitOffsetClause, 1, 0);
            QueryResult<T> result = select(sql, Map.of("arg", value), classToFind);
            return result.first();
        }

        public QueryResult<T> all() {
            Mapping metaData = getMapping(classToFind);
            String tableName = metaData.getTableName();
            String selectClause = "SELECT * FROM " + tableName + " WHERE ";
            String sql = selectClause + "true=true";
            return select(sql, Map.of(), classToFind);
        }

        public QueryResult<T> allBy(String column, Object val) {
            Mapping metaData = getMapping(classToFind);
            String tableName = metaData.getTableName();
            String selectClause = "SELECT * FROM " + tableName + " WHERE ";
            String sql = selectClause + column + "=:val ";
            return select(sql, Map.of("val", val), classToFind);
        }

        public QueryResult<T> where(String whereClause, Map<String, Object> args) {
            Mapping metaData = getMapping(classToFind);
            String tableName = metaData.getTableName();
            String selectClause = "SELECT * FROM " + tableName + " WHERE ";
            String sql = selectClause + whereClause;
            return select(sql, args, classToFind);
        }

        public T firstWhere(String whereClause, Map<String, Object> args) {
            Mapping metaData = getMapping(classToFind);
            String tableName = metaData.getTableName();
            String selectClause = "SELECT * FROM " + tableName + " WHERE ";
            String sql = selectClause + whereClause + " " + MessageFormat.format(limitOffsetClause, 1, 0);
            return select(sql, args, classToFind).first();
        }

        public QueryResult<T> bySQL(String sql, Map<String, Object> args) {
            return get().select(sql, args, classToFind);
        }

        public GrugClassQuery<T> byQuery() {
            return query(classToFind);
        }
    }

    public class GrugStreamFinder<T> {
        Class<T> classToFind;

        public GrugStreamFinder(Class<T> classToFind) {
            this.classToFind = classToFind;
        }

        public Stream<T> byId(Object id) {
            Mapping mapping = getMapping(classToFind);
            String column = mapping.getIdColumn();
            Mapping mapping1 = getMapping(classToFind);
            String sql = "SELECT * FROM " + mapping1.getTableName() + " WHERE " + column + "=:arg " + MessageFormat.format(limitOffsetClause, 1, 0);
            return stream(sql, Map.of("arg", id), classToFind);
        }

        public Stream<T> byKey(String col, Object value) {
            Mapping mapping = getMapping(classToFind);
            String sql = "SELECT * FROM " + mapping.getTableName() + " WHERE " + col + "=:arg " + MessageFormat.format(limitOffsetClause, 1, 0);
            return stream(sql, Map.of("arg", value), classToFind);
        }

        public Stream<T> all() {
            Mapping metaData = getMapping(classToFind);
            String tableName = metaData.getTableName();
            String selectClause = "SELECT * FROM " + tableName + " WHERE ";
            String sql = selectClause + "true=true";
            return stream(sql, Map.of(), classToFind);
        }

        public Stream<T> allBy(String column, Object val) {
            Mapping metaData = getMapping(classToFind);
            String tableName = metaData.getTableName();
            String selectClause = "SELECT * FROM " + tableName + " WHERE ";
            String sql = selectClause + column + "=:val ";
            return stream(sql, Map.of("val", val), classToFind);
        }

        public Stream<T> where(String whereClause, Map<String, Object> args) {
            Mapping metaData = getMapping(classToFind);
            String tableName = metaData.getTableName();
            String selectClause = "SELECT * FROM " + tableName + " WHERE ";
            String sql = selectClause + whereClause;
            return stream(sql, args, classToFind);
        }

        public Stream<T> bySQL(String sql, Map<String, Object> args) {
            return stream(sql, args, classToFind);
        }

        public GrugClassQuery<T> byQuery() {
            return query(classToFind);
        }
    }

    //====================================================================
    // Connection management
    //====================================================================

    private class ConnectionSession implements SafeAutoCloseable {

        ConnectionSession previous;
        Connection conn;
        int openCount;
        int transactionCount;
        UUID uuid;
        ArrayList<PreparedStatement> preparedStatements = new ArrayList<>();
        ArrayList<ResultSet> resultSets = new ArrayList<>();

        public ConnectionSession(ConnectionSession previousConnection) {
            this.conn = getNewRawConnection();
            this.previous = previousConnection;
            this.uuid = UUID.randomUUID();
            this.openCount = 0;
            this.transactionCount = 0;
        }

        public void incrementOpenCount() {
            openCount++;
            logger.log(GrugLogger.Level.DEBUG, "Incremented open count on connection {}: {}", uuid, "*".repeat(openCount));
        }

        public void close() {
            openCount--;
            logger.log(GrugLogger.Level.DEBUG, "Decremented open count on connection {}: {}", uuid, "*".repeat(openCount));
            if (openCount == 0) { // if we are back at the top level of the connection count we close the connection
                logger.log(GrugLogger.Level.INFO, "Closing connection {} on Thread {}", uuid, Thread.currentThread().getName());
                try {
                    for (var rs : resultSets) {
                        try {
                            if (!rs.isClosed()) {
                                rs.close();
                            }
                        } catch (SQLException e) { /* swallow */ }
                    }
                    for (var ps : preparedStatements) {
                        try {
                            if (!ps.isClosed()) {
                                ps.close();
                            }
                        } catch (SQLException e) { /* swallow */ }
                    }
                } finally {
                    // always try to close the connection no matter what
                    try {
                        safely(() -> conn.close());
                    } finally {
                        CURRENT_SESSION.set(this.previous);
                    }
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

        public void finishTransaction() {
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

        private ResultSet execute(PreparedStatement ps) {
            ResultSet resultSet = time(ps::executeQuery);
            resultSets.add(resultSet);
            return resultSet;
        }

        private PreparedStatement prepareStatement(String updatedSql, Collection<Object> vals) throws SQLException {
            return prepareStatement(updatedSql, vals, null);
        }

        private PreparedStatement prepareStatement(String updatedSql, Collection<Object> vals, String[] keyCols) throws SQLException {
            PreparedStatement ps;
            if (keyCols != null) {
                ps = conn.prepareStatement(updatedSql, keyCols);
            } else {
                ps = conn.prepareStatement(updatedSql);
            }
            preparedStatements.add(ps);
            int offset = 1;
            for (Object val : vals) {
                setValueForQuery(ps, offset, val);
                offset++;
            }
            return ps;
        }

    }

    public Connection getNewRawConnection() {
        return safely(connectionSource);
    }

    private ConnectionSession getOrCreateSession() {
        ConnectionSession connectionSession = getCurrentSession();
        if (connectionSession == null) {
            connectionSession = pushNewSession();
        }
        connectionSession.incrementOpenCount();
        return connectionSession;
    }

    private static ConnectionSession getCurrentSession() {
        return CURRENT_SESSION.get();
    }

    public SafeAutoCloseable establishConnection() {
        ConnectionSession connectionSession = pushNewSession();
        connectionSession.incrementOpenCount();
        return connectionSession;
    }

    private ConnectionSession pushNewSession() {
        ConnectionSession currentSession = getCurrentSession();
        ConnectionSession newSession = new ConnectionSession(currentSession);
        logger.log(GrugLogger.Level.INFO, "Created a new connection for Thread {} w/ID {}", Thread.currentThread().getName(), newSession.uuid);
        CURRENT_SESSION.set(newSession);
        return newSession;
    }

    //====================================================================
    // Transaction management
    //====================================================================

    public void withTransaction(Runnable runnable) {
        try {
            startTransaction();
            runnable.run();
            commitTransaction();
        } catch (Exception e) {
            rollBackTransaction();
            throw rethrow(e);
        }
    }

    public <T> T withTransaction(Callable<T> runnable) {
        try {
            startTransaction();
            T result = runnable.call();
            commitTransaction();
            return result;
        } catch (Exception e) {
            rollBackTransaction();
            throw rethrow(e);
        }
    }

    public void startTransaction() {
        ConnectionSession connectionSession = getOrCreateSession();
        connectionSession.startTransaction();
    }

    public void maybeCommitTransaction() {
        ConnectionSession connectionSession = getCurrentSession();
        if (connectionSession.isInTransaction()) {
            connectionSession.finishTransaction();
        }
    }

    public void commitTransaction() {
        ConnectionSession connectionSession = getCurrentSession();
        if (connectionSession == null) {
            logger.log(GrugLogger.Level.ERROR, "No current connection for transaction.");
        } else {
            connectionSession.finishTransaction();
        }
    }

    public void rollBackTransaction() {
        ConnectionSession connectionSession = getCurrentSession();
        if (connectionSession == null) {
            logger.log(GrugLogger.Level.ERROR, "No current connection for transaction.");
        } else {
            connectionSession.rollBackTransaction();
        }
    }

    //====================================================================
    // Database interaction
    //====================================================================

    public QueryResult<ResultMap> select(String sql) {
        return select(sql, Map.of());
    }

    public <T> QueryResult<T> select(String sql, Class<T> resultClass) {
        return select(sql, Map.of(), resultClass);
    }

    public QueryResult<ResultMap> select(String sql, Map<String, Object> args) {
        return select(sql, args, ResultMap.class);
    }

    public <T> QueryResult<T> select(String sql, Map<String, Object> args, Class<T> resultClass) {
        return select(sql, args, resultClass, (List<String>) null);
    }

    public <T> QueryResult<T> select(String sql, Map<String, Object> args, Class<T> resultClass, String... colsToMap) {
        return select(sql, args, resultClass, Arrays.asList(colsToMap));
    }

    public <T> QueryResult<T> select(String sql, Map<String, Object> args, Class<T> resultClass, List<String> colsToMap) {
        return select(sql, args, resultClass, new ColumnsSpec(colsToMap));
    }

    private <T> QueryResult<T> select(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec) {
        BetterList<T> resultList = new BetterList<>();
        QueryResult<T> queryResult = new QueryResult<>(this, sql, args, resultClass, columnSpec, resultList);
        select(sql, args, resultClass, columnSpec, resultList);
        return queryResult;
    }

    private <T> void select(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec, BetterList<T> results) {
        Mapping mapping = getMapping(resultClass);
        ArrayList<Object> vals = new ArrayList<>();
        String updatedSql = updateSqlVars(sql, args, vals);
        logger.log(getQueryLogLevel(), "Select SQL: {}\n  Args:{}", updatedSql, vals);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(updatedSql, vals);
             var resultSet = session.execute(ps)) {
            while (resultSet.next()) {
                T result = mapping.newObjectFromResult(this, resultSet, columnSpec);
                if (result instanceof GrugRecordLifecycle lifecycle) {
                    lifecycle.afterSelect();
                }
                results.add(result);
            }
        } catch (Exception e) {
            throw handleSelectException(sql, args, e);
        }
    }

    public Stream<ResultMap> stream(String sql) {
        return stream(sql, Map.of(), ResultMap.class);
    }

    public <T> Stream<T> stream(String sql, Class<T> resultClass) {
        return stream(sql, Map.of(), resultClass);
    }

    public Stream<ResultMap> stream(String sql, Map<String, Object> args) {
        return stream(sql, args, ResultMap.class);
    }

    public <T> Stream<T> stream(String sql, Map<String, Object> args, Class resultClass) {
        return stream(sql, args, resultClass, (List<String>) null);
    }

    public <T> Stream<T> stream(String sql, Map<String, Object> args, Class resultClass, String... colsToMap) {
        return stream(sql, args, resultClass, Arrays.asList(colsToMap));
    }

    public <T> Stream<T> stream(String sql, Map<String, Object> args, Class resultClass, List<String> colsToMap) {
        return stream(sql, args, resultClass, new ColumnsSpec(colsToMap));
    }

    private <T> Stream<T> stream(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec) {
        var session = getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("You must manually establish a connection with establishConnection() and manage closing the connection yourself before streaming results");
        }
        Mapping mapping = getMapping(resultClass);
        ArrayList<Object> vals = new ArrayList<>();
        String updatedSql = updateSqlVars(sql, args, vals);//SQL, Argument Map, Blank Value list to be filled
        logger.log(getQueryLogLevel(), "Select SQL: {}\n  Args:{}", updatedSql, vals);
        try {
            PreparedStatement ps = session.prepareStatement(updatedSql, vals);
            ResultSet rs = session.execute(ps);
            return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED) {
                public boolean tryAdvance(Consumer<? super T> action) {
                    try {
                        if (rs.next()) {
                            T result = mapping.newObjectFromResult(GrugORM.this, rs, columnSpec);
                            if (result instanceof GrugRecordLifecycle lifecycle) {
                                lifecycle.afterSelect();
                            }
                            action.accept(result);
                            return true;
                        } else {
                            rs.close();
                            return false;
                        }
                    } catch (Exception e) {
                        throw rethrow(e);
                    }
                }
            }, false);
        } catch (Exception e) {
            throw handleSelectException(sql, args, e);
        }
    }

    private RuntimeException handleSelectException(String sql, Map args, Exception e) {
        logger.log(GrugLogger.Level.ERROR, """
                Exception in select() with SQL
                {}\s
                with args {}: {}""", TextTools.indent(2, sql), args, e.getMessage());
        throw rethrow(e);
    }

    public long insert(Object object) {
        if (object instanceof GrugRecordLifecycle lifecycle && !lifecycle.validate()) {
            return INSERT_FAILED;
        }
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        String keyCol = mapping.getIdColumn();
        Map<String, Object> values = mapping.toDatabaseMap(object);
        values.remove(keyCol);
        if (object instanceof GrugRecordLifecycle lifecycle && !lifecycle.beforeInsert()) {
            return INSERT_FAILED;
        }
        Object newVersionValue = null;
        // TODO - remove this?  It's an insert...
        if (mapping.hasVersionColumn()) {
            newVersionValue = mapping.incrementVersion(values);
        }
        long id = insert(mapping.getTableName(), values, keyCol);
        if (mapping.hasVersionColumn() && id != INSERT_FAILED) {
            mapping.updateVersionValue(object, newVersionValue);
        }
        if (!mapping.isReadOnly() && id != INSERT_FAILED) {
            mapping.setId(object, id);
        }
        if (object instanceof GrugRecordLifecycle lifecycle) {
            lifecycle.afterInsert();
        }
        return id;
    }

    public void insertAll(Object... items) {
        insertAll(List.of(items));
    }




    public void insertAll(Collection<Object> items){//philosophy here is is that we aren't promising anything, and if you want ID's out of your inserts that you can do a loop. This is just a simple helper and you can write your own sql if you want a better solution
        Class<?> prevClass = null; //this little pointer system allows us to check after each object if the last object was the same class, and if not we throw a logger arror
        Mapping mappingClass = null;
        String tableName = null;
        String[] keyCol = new String[1];
        Map<String, Object> values = null;//not sure if this is standard practice, as this gets traded hands a lot in this function
        ArrayList<Object> queryValues = new ArrayList<>();
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        for (Object insertionInstance : items) {
            Class<?> clazz = insertionInstance.getClass();
            if(prevClass != null && !prevClass.equals(clazz)){//easy check for not all same class mass insertions
                logger.log(GrugLogger.Level.ERROR, "Exception in insertAll() during mass insertion, not all Classes are the same");
                throw new java.lang.RuntimeException("insertAll Class type failed");
            }
            if(prevClass == null){//first pass
                prevClass = clazz;
                mappingClass = getMapping(clazz);
                tableName = mappingClass.tableName;
                keyCol[0] = mappingClass.getIdColumn();
                values = mappingClass.toDatabaseMap(insertionInstance);
                values.remove(keyCol[0]);
                sb.append(tableName);
                sb.append(" (");
                sb.append(String.join(", ", values.keySet()));
                sb.append(") VALUES ");//this is where pluggable work will be done if needed.
            }
            if(values == null){
                values = mappingClass.toDatabaseMap(insertionInstance);
                values.remove(keyCol[0]);
            }
            if (values.isEmpty()) { //need to look at case when one of the objects doesnt have all the values....
                    sb.append(" (");//fix the bitstream .toArray function and check that its the same output, this value should produce the right output
                    sb.append(Arrays.stream(values.keySet().toArray(new String[0])).map(_ -> "DEFAULT").collect(Collectors.joining(", ")));
                    sb.append(")");
                } else {
                    sb.append(" (");
                    sb.append(values.keySet().stream().map(_ -> "?").collect(Collectors.joining(", ")));//sets each new row with the insertionInstance values from field
                    sb.append(")");
                    queryValues.addAll(values.values());
            }
            sb.append(", ");
            values = null;//just to clean up variable to trigger that if statement, this stops repeating computations
        }
        String insertString = sb.toString();
        insertString = insertString.substring(0, insertString.length() - 2);//could use an edge case here
        Collection<Object> queryValuesCollection = queryValues;
        logger.log(getQueryLogLevel(), "MASS INSERT SQL: {}\n  Args:{}", insertString, queryValuesCollection);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(insertString, queryValuesCollection, keyCol)) {
            time(ps::executeUpdate);
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in insertAll() with SQL {} & args {}: {}", insertString, queryValues, e.getMessage());
            throw rethrow(e);
        }
       }

    private long insert(String tableName, Map<String, Object> values, String... keyCols) {
        if (!(values instanceof LinkedHashMap<String, Object>)) {
            values = new LinkedHashMap<>(values);
        }
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(tableName);
        if (values.isEmpty()) {
            if (sqlLiteQuirks) {
                sb.append(" DEFAULT VALUES");
            } else {
                sb.append(" (");
                sb.append(String.join(", ", keyCols));
                sb.append(") VALUES (");
                sb.append(Arrays.stream(keyCols).map(_ -> "DEFAULT").collect(Collectors.joining(", ")));
                sb.append(")");
            }
        } else {
            sb.append(" (");
            sb.append(String.join(", ", values.keySet()));
            sb.append(") VALUES (");
            sb.append(values.keySet().stream().map(_ -> "?").collect(Collectors.joining(", ")));
            sb.append(")");
        }
        String insertString = sb.toString();
        Collection<Object> queryValues = values.values();
        logger.log(getQueryLogLevel(), "INSERT SQL: {}\n  Args:{}", insertString, queryValues);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(insertString, queryValues, keyCols)) {
            time(ps::executeUpdate);
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                return INSERT_FAILED;
            }
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in insert() with SQL {} & args {}: {}", insertString, queryValues, e.getMessage());
            throw rethrow(e);
        }
    }
    public boolean update(Object object) {
        if (object instanceof GrugRecordLifecycle lifecycle && !lifecycle.validate()) {
            return false;
        }
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        String tableName = mapping.getTableName();
        String keyCol = mapping.getIdColumn();
        Map<String, Object> valuesToUpdate = mapping.toDatabaseMap(object);
        Object keyVal = valuesToUpdate.remove(keyCol); // remove the key
        if (object instanceof GrugRecordLifecycle lifecycle && !lifecycle.beforeUpdate()) {
            return false;
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
        if (mapping.hasVersionColumn() && update) {
            mapping.updateVersionValue(object, nextVersionValue);
        }
        if (object instanceof GrugRecordLifecycle lifecycle) {
            lifecycle.afterUpdate();
        }
        return update;
    }
    //reference for bulk UPDATEALL
    private boolean update(String tableName, String keyCol, Object keyVal, String versionCol, Object versionVal, Map<String, Object> values) {
        if (!(values instanceof TreeMap<String, Object>)) {
            values = new TreeMap<>(values);
        }
        StringBuilder sb = new StringBuilder("UPDATE ");
        sb.append(tableName);
        sb.append(" SET ");
        sb.append(values.keySet().stream().map(col -> col + "=?").collect(Collectors.joining(", ")));
        sb.append(" WHERE ");
        sb.append(keyCol).append("=?");
        if (versionCol != null) {
            sb.append(" AND ").append(versionCol).append("=?");
        }

        String updateSQL = sb.toString();
        logger.log(getQueryLogLevel(), "UPDATE SQL: {}\n  Args:{}", updateSQL, values.values());

        // construct final values collection
        ArrayList<Object> finalValues = new ArrayList<>(values.values());
        finalValues.add(keyVal);
        if (versionCol != null) {
            finalValues.add(versionVal);
        }

        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(updateSQL, finalValues)) {
            int i = time(ps::executeUpdate);
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
        if (object instanceof GrugRecordLifecycle lifecycle && !lifecycle.beforeDelete()) {
            return false;
        }
        boolean delete = delete(tableName, keyCol, keyVal);
        if (object instanceof GrugRecordLifecycle lifecycle) {
            lifecycle.afterDelete();
        }
        return delete;
    }

    private boolean delete(String tableName, String keyCol, Object keyVal) {
        String deleteSQL = "DELETE FROM " + tableName + " WHERE " + keyCol + "=?";
        logger.log(getQueryLogLevel(), "DELETE SQL: {}\n  Args:{}", deleteSQL, List.of(keyVal));
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(deleteSQL, List.of(keyVal))) {
            int i = time(ps::executeUpdate);
            return i == 1;
        } catch (Exception e) {
            logger.log(GrugLogger.Level.ERROR, "Exception in delete() with SQL {} & value {}: {}", deleteSQL, keyVal, e.getMessage());
            throw rethrow(e);
        }
    }

    public void reload(Object object) {
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        Object fromDb = find(clazz).byId(mapping.getId(object));
        mapping.copyValues(object, fromDb);
    }

    public boolean exec(String sql) {
        return exec(sql, Map.of());
    }

    public boolean exec(String sql, Map<String, Object> args) {
        ArrayList<Object> vals = new ArrayList<>();
        String updatedSql = updateSqlVars(sql, args, vals);
        if (sql.isBlank()) {
            logger.log(GrugLogger.Level.WARN, "SQL is blank, will not be executed!");
            return false;
        }
        logger.log(getQueryLogLevel(), "EXECUTING RAW SQL: {} with args {}\n", sql, args);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(sql, vals)) {
            boolean result = time(ps::execute);
            return result;
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

    public static class TextTools {

        private TextTools() {
            // utility class, no instantiation
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
                    .map(s -> " ".repeat(spaces) + s)
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

        // an extremely simplified english pluralization algorithm based
        // on https://blob.perl.org/tpc/1998/User_Applications/Algorithmic%20Approach%20Plurals/Algorithmic_Plurals.html
        static LinkedHashMap<Pattern, String> INFLECTIONS = new LinkedHashMap<>();

        private static void addInflection(String suffix, String replacement) {
            INFLECTIONS.put(Pattern.compile(".*" + suffix + "$"), replacement);
        }

        static {
            addInflection("[ch](h)", "hes");
            addInflection("(ss)", "sses");
            addInflection("[aeo]l(f)", "ves");
            addInflection("[^d]ea(f)", "ves");
            addInflection("ar(f)", "ves");
            addInflection("[nlw]i(fe)", "ves");
            addInflection("[aeiou](y)", "ys");
            addInflection("(y)", "ies");
        }

        public static String pluralize(String noun) {
            for (Map.Entry<Pattern, String> inflection : INFLECTIONS.entrySet()) {
                Matcher matcher = inflection.getKey().matcher(noun);
                if (matcher.matches()) {
                    StringBuilder result = new StringBuilder(noun);
                    result.replace(matcher.start(1), matcher.end(1), inflection.getValue());
                    return result.toString();
                }
            }
            return noun + "s"; // default to appending 's'
        }
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

        interface SafeAutoCloseable extends AutoCloseable {
            void close();
        }

        interface BetterIterable<T> extends Iterable<T> {

            //==============================================================================
            // Stream alternative (i hate streams)
            //==============================================================================

            default <Q> BetterList<Q> map(Function<T, Q> mapper) {
                BetterList<Q> mappedResult = new BetterList<>();
                for (T t : this) {
                    mappedResult.add(mapper.apply(t));
                }
                return mappedResult;
            }

            default Set<T> toSet() {
                LinkedHashSet<T> ts = new LinkedHashSet<>();
                forEach(ts::add);
                return ts;
            }

            default BetterList<T> toList() {
                BetterList<T> ts = new BetterList<>();
                forEach(ts::add);
                return ts;
            }

            default <K> Map<K, List<T>> toMap(Function<T, K> mapper) {
                Map<K, List<T>> mappedResult = new HashMap<>();
                for (T t : this) {
                    mappedResult
                            .computeIfAbsent(mapper.apply(t), _ -> new ArrayList<>())
                            .add(t);
                }
                return mappedResult;
            }

            default <K> TreeMap<K, List<T>> toOrderedMap(Function<T, K> mapper) {
                TreeMap<K, List<T>> mappedResult = new TreeMap<>();
                for (T t : this) {
                    mappedResult
                            .computeIfAbsent(mapper.apply(t), _ -> new ArrayList<>())
                            .add(t);
                }
                return mappedResult;
            }

            default <K> TreeMap<K, List<T>> toOrderedMap(Function<T, K> mapper, Comparator<? super K> comparator) {
                TreeMap<K, List<T>> mappedResult = new TreeMap<>(comparator);
                for (T t : this) {
                    mappedResult
                            .computeIfAbsent(mapper.apply(t), _ -> new ArrayList<>())
                            .add(t);
                }
                return mappedResult;
            }

            default <K> Map<K, T> toDistinctMap(Function<T, K> mapper) {
                Map<K, T> mappedResult = new HashMap<>();
                for (T t : this) {
                    mappedResult.put(mapper.apply(t), t);
                }
                return mappedResult;
            }

            default <K> TreeMap<K, T> toOrderedDistinctMap(Function<T, K> mapper) {
                TreeMap<K, T> mappedResult = new TreeMap<>();
                for (T t : this) {
                    mappedResult.put(mapper.apply(t), t);
                }
                return mappedResult;
            }

            default <K> TreeMap<K, T> toOrderedDistinctMap(Function<T, K> mapper, Comparator<? super K> comparator) {
                TreeMap<K, T> mappedResult = new TreeMap<>(comparator);
                for (T t : this) {
                    mappedResult.put(mapper.apply(t), t);
                }
                return mappedResult;
            }

            default BetterList<T> filter(Predicate<? super T> filter) {
                BetterList<T> mappedResult = new BetterList<>();
                for (T t : this) {
                    if (filter.test(t)) {
                        mappedResult.add(t);
                    }
                }
                return mappedResult;
            }

            default String join(String separator) {
                StringBuilder builder = new StringBuilder();
                int i = 0;
                for (T t : this) {
                    if (i != 0) {
                        builder.append(separator);
                    }
                    builder.append(t);
                    i++;
                }
                return builder.toString();
            }

            default T first() {
                for (T t : this) {
                    return t;
                }
                return null;
            }

            default T firstWhere(Predicate<? super T> predicate) {
                for (T t : this) {
                    if (predicate.test(t)) {
                        return t;
                    }
                }
                return null;
            }

            default boolean hasMatch(Predicate<? super T> predicate) {
                for (T t : this) {
                    if (predicate.test(t)) {
                        return true;
                    }
                }
                return false;
            }

            default boolean hasNoMatch(Predicate<? super T> predicate) {
                for (T t : this) {
                    if (predicate.test(t)) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    public static class EnterpriseGrugBean implements GrugRecordLifecycle {

        private transient boolean persisted;
        private final transient Map<String, List<String>> errors = new LinkedHashMap<>();

        public boolean hasErrors() {
            return !errors.isEmpty();
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

        protected <T> QueryResult<T> loadManyThrough(Class<?> through, Class<T> to) {
            return orm().loadManyThrough(this, through, to);
        }

        protected <T> QueryResult<T> loadMany(Class<T> of) {
            return orm().loadMany(this, of);
        }

        protected <T> QueryResult<T> loadMany(Class<T> of, String fkColumn) {
            return orm().loadMany(this, of, fkColumn);
        }

        protected <T> T load(Class<T> of) {
            return orm().load(this, of);
        }

        protected <T> T load(Class<T> of, String fkColumn) {
            return orm().load(this, of, fkColumn);
        }

        public void reload() {
            orm().reload(this);
        }

        public void setFieldsFrom(Map<String, String> map, String... fields) {
            setFieldsFrom(map::get, fields);
        }

        public void setFieldsFrom(UnaryOperator<String> supplier, String... fields) {
            for (String col : fields) {
                String str = supplier.apply(col);
                setValueFromString(col, str);
            }
        }

        private void setValueFromString(String col, String str) {
            Mapping mapping = orm().getMapping(this.getClass());
            FieldMapping fieldMapping = mapping.getFieldMapping(col);
            if(fieldMapping == null) {
                throw new IllegalArgumentException("No field '" + str + "' found on " + this.getClass().getSimpleName());
            }
            Class fieldType = fieldMapping.getType();
            fieldMapping.setFieldValue(this, orm().coerce(fieldType, str));
        }

        public boolean isPersisted() {
            return persisted;
        }

        @Override
        public String toString() {
            var mapping = orm().getMapping(this.getClass());
            var fieldMappings = mapping.fieldNameToMapping;
            var sb = new StringBuilder(this.getClass().getSimpleName()).append("{");
            boolean first = true;
            fieldMappings.forEach((name, fieldMapping) -> {
                sb.append(name);
                sb.append(":");
                Object fieldValue = fieldMapping.getFieldValue(this);
                if(fieldValue instanceof String) {
                    sb.append("\"").append(fieldValue).append("\"");
                } else {
                    sb.append(fieldValue);
                }
                sb.append(", ");
            });
            sb.delete(sb.length() - 2, sb.length());
            sb.append("}");
            return sb.toString();
        }

        protected static <T> GrugListFinder<T> find(Class<T> c) {
            return orm().find(c);
        }

        protected static GrugORM orm() {
            return GrugORM.get();
        }

    }

    public enum SortOrder {
        ASC, DESC;
    }

    public enum JoinType {
        INNER, OUTER, LEFT, RIGHT;
    }

    private record OrderBy(String col, SortOrder direction) {

        public String toString() {
            return col + " " + (direction == null ? "" : direction.name());
        }
    }

    public class GrugQuery<T> {

        private final String baseTable;
        private boolean distinct;
        private List<String> columns;
        private String columnPrefix;
        private final StringBuilder whereClause = new StringBuilder();
        private final Map<String, Object> valMap = new TreeMap<>();
        private final List<String> joins = new ArrayList<>();
        private final List<OrderBy> orderBys = new ArrayList<>();
        protected Class<T> resultClass;

        private int pageSize = -1;
        private int page = -1;

        public GrugQuery(String baseTable) {
            this.baseTable = baseTable;
            this.resultClass = (Class<T>) ResultMap.class;
        }

        public GrugQuery<T> where(String condition) {
            if (!whereClause.isEmpty()) {
                whereClause.append(" AND ");
            }
            whereClause.append(condition);
            return this;
        }

        public GrugQuery<T> where(String condition, Map<String, Object> vars) {
            return where(condition).withVars(vars);
        }

        public GrugQuery<T> select(String... columns) {
            return select(Arrays.asList(columns));
        }

        public GrugQuery<T> select(List<String> columns) {
            this.columns = columns;
            return this;
        }

        public GrugQuery<T> withColumnPrefix(String columnPrefix) {
            this.columnPrefix = columnPrefix;
            return this;
        }

        public GrugQuery<T> distinct() {
            this.distinct = true;
            return this;
        }

        public <Q> GrugQuery<Q> withResult(Class<Q> clazz) {
            this.resultClass = (Class) clazz;
            //noinspection unchecked
            return (GrugQuery<Q>) this;
        }

        public QueryResult<T> fetch() {
            String sql = generateSQL();
            return GrugORM.this.select(sql, valMap, resultClass, columns);
        }

        public BetterList<T> fetchAsList() {
            String sql = generateSQL();
            QueryResult<T> select = GrugORM.this.select(sql, valMap, resultClass, columns);
            return select.getRawList();
        }

        public T fetchFirst() {
            String sql = generateSQLNoLimit() + " " + MessageFormat.format(limitOffsetClause, 1, 0);
            return GrugORM.this.select(sql, valMap, resultClass, columns).first();
        }

        public Stream<T> stream() {
            String sql = generateSQL();
            return GrugORM.this.stream(sql, valMap, resultClass, new ColumnsSpec(columns));
        }

        private String generateSQL() {
            String sql = generateSQLNoLimit();
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

        private String generateSQLNoLimit() {
            String sql = generateSelectClause();
            if (!joins.isEmpty()) {
                sql += "\n" + String.join("\n", joins);
            }
            if (!whereClause.isEmpty()) {
                sql += "\nWHERE " + whereClause;
            }
            if (!orderBys.isEmpty()) {
                sql += "\nORDER BY " + String.join(", ", orderBys.stream().map(OrderBy::toString).toList());
            }
            return sql;
        }

        private String generateSelectClause() {
            StringBuilder selectClause = new StringBuilder("SELECT ");
            if (distinct) {
                selectClause.append("DISTINCT ");
            }
            String prefix = columnPrefix != null ? columnPrefix + "." : "";
            if (columns != null) {
                selectClause.append(columns.stream().map(s -> prefix + s).collect(Collectors.joining(", ")));
            } else {
                selectClause.append(prefix).append("*");
            }
            selectClause.append(" FROM ").append(baseTable);
            return selectClause.toString();
        }

        public GrugQuery<T> withVars(Map<String, Object> vals) {
            for (Map.Entry<String, Object> stringObjectEntry : vals.entrySet()) {
                withVar(stringObjectEntry.getKey(), stringObjectEntry.getValue());
            }
            return this;
        }

        public GrugQuery<T> withVar(String name, Object value) {
            if (valMap.containsKey(name)) {
                throw new IllegalStateException("Value " + name + " already exists in query!");
            }
            valMap.put(name, value);
            return this;
        }

        public GrugQuery<T> join(String joinSql) {
            if (!joinSql.toUpperCase().contains("JOIN")) {
                joinSql = "JOIN " + joinSql;
            }
            this.joins.add(joinSql);
            return this;
        }

        public GrugQuery<T> orderBy(String column) {
            return orderBy(column, null);
        }

        public GrugQuery<T> orderBy(String column, SortOrder direction) {
            this.orderBys.add(new OrderBy(column, direction));
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

    public class GrugClassQuery<T> implements Callable<QueryResult<T>> {

        private final GrugQuery<T> query;
        private final Class classToFind;
        private Class lastJoinedClass;

        public GrugClassQuery(Class<T> classToFind) {
            this.classToFind = classToFind;
            Mapping mappingForClassToFind = getMapping(classToFind);
            query = new GrugQuery<>(mappingForClassToFind.getTableName())
                    .withResult(classToFind)
                    .withColumnPrefix(mappingForClassToFind.getTableName())
                    .distinct();
            this.setLastJoinedClass(classToFind);
        }

        public GrugClassQuery<T> join(Class classToJoin) {
            setLastJoinedClass(classToFind);
            return thenJoin(null, classToJoin);
        }

        public GrugClassQuery<T> join(JoinType type, Class classToJoinTo) {
            setLastJoinedClass(classToFind);
            return thenJoin(type, classToJoinTo);
        }

        public GrugClassQuery<T> thenJoin(Class classToJoinTo) {
            return thenJoin(null, classToJoinTo);
        }

        private GrugClassQuery<T> thenJoin(JoinType type, Class classToJoinTo) {
            Class hasFk = resolveFkClass(getLastJoinedClass(), classToJoinTo);
            Class hasId = hasFk == classToJoinTo ? getLastJoinedClass() : classToJoinTo;
            return join(type, classToJoinTo, hasId, hasFk);
        }

        private Class resolveFkClass(Class class1, Class class2) {
            Mapping class1Mapping = getMapping(class1);
            Mapping class2Mapping = getMapping(class2);
            if (class1Mapping.hasColumn(class2Mapping.getDefaultForeignKeyColumnName())) {
                return class1;
            }
            if (class2Mapping.hasColumn(class1Mapping.getDefaultForeignKeyColumnName())) {
                return class2;
            }
            throw new IllegalStateException(MessageFormat.format("Cannot determine a foreign key relationship between {0} and {1}, please use an explicit join", class1.getSimpleName(), class2.getSimpleName()));
        }

        public GrugClassQuery<T> join(JoinType type, Class classToJoin, Class hasId, Class hasFk) {
            Mapping hasIdMapping = getMapping(hasId);
            Mapping hasFkMapping = getMapping(hasFk);
            Mapping classToJoinMapping = getMapping(classToJoin);
            String idTable = hasIdMapping.getTableName();
            String fkTable = hasFkMapping.getTableName();
            String joinedTable = classToJoinMapping.getTableName();
            String idColumn = hasIdMapping.getIdColumn();
            String fkColumn = hasIdMapping.getDefaultForeignKeyColumnName();
            String joinType;
            if (type != null) {
                joinType = type.name() + " JOIN ";
            } else {
                joinType = "JOIN ";
            }
            String sqlString = joinType + joinedTable + " ON " + idTable + "." + idColumn + " = " + fkTable + "." + fkColumn;
            setLastJoinedClass(classToJoin);
            query.join(sqlString);
            return this;
        }

        public GrugClassQuery<T> where(String condition) {
            query.where(condition);
            return this;
        }

        public GrugClassQuery<T> where(String condition, Map<String, Object> vals) {
            query.where(condition, vals);
            return this;
        }

        public QueryResult<T> fetch() {
            return query.fetch();
        }

        public BetterList<T> fetchAsList() {
            return query.fetch().getRawList();
        }

        public T fetchFirst() {
            return query.fetchFirst();
        }

        public Stream<T> stream() {
            return query.stream();
        }

        public GrugClassQuery<T> withVars(Map<String, Object> vals) {
            query.withVars(vals);
            return this;
        }

        public GrugClassQuery<T> withVar(String name, Object value) {
            query.withVar(name, value);
            return this;
        }

        public GrugClassQuery<T> join(String joinSql) {
            query.join(joinSql);
            return this;
        }

        public GrugClassQuery<T> orderBy(String column) {
            query.orderBy(column);
            return this;
        }

        public GrugClassQuery<T> orderBy(String column, SortOrder direction) {
            query.orderBy(column, direction);
            return this;
        }

        public GrugClassQuery<T> pageSize(int pageSize) {
            query.pageSize(pageSize);
            return this;
        }

        public GrugClassQuery<T> page(int page) {
            query.page(page);
            return this;
        }

        public GrugQuery<T> raw() {
            return query;
        }

        public Class getLastJoinedClass() {
            return lastJoinedClass;
        }

        public void setLastJoinedClass(Class lastJoinedClass) {
            this.lastJoinedClass = lastJoinedClass;
        }

        public GrugClassQuery<T> withCols(String... cols) {
            query.select(cols);
            return this;
        }

        public GrugClassQuery<T> withCols(List<String> cols) {
            query.select(cols);
            return this;
        }

        public QueryResult<T> call() throws Exception {
            return fetch();
        }
    }

    private class DefaultLogger implements GrugLogger {
        // thank you slf4j for using a non-standard logging format, very cool
        final Pattern parens = Pattern.compile("\\{}");


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
            case Short s -> ps.setShort(parameterIndex, s);
            case Integer i -> ps.setInt(parameterIndex, i);
            case Long l -> ps.setLong(parameterIndex, l);
            case Float f -> ps.setDouble(parameterIndex, f);
            case Double d -> ps.setDouble(parameterIndex, d);
            case BigDecimal bd -> ps.setBigDecimal(parameterIndex, bd);
            case String str -> ps.setString(parameterIndex, str);
            case Time d -> ps.setTime(parameterIndex, d);
            case Timestamp ts -> ps.setTimestamp(parameterIndex, ts);
            case Date d -> ps.setTimestamp(parameterIndex, new Timestamp(d.getTime()));
            case LocalDate ld -> ps.setDate(parameterIndex, java.sql.Date.valueOf(ld));
            case LocalDateTime ldt -> ps.setTimestamp(parameterIndex, Timestamp.valueOf(ldt));
            case Blob blob -> ps.setBlob(parameterIndex, blob);
            case NClob nclob -> ps.setNClob(parameterIndex, nclob);
            case Clob clob -> ps.setClob(parameterIndex, clob);
            case Byte b -> ps.setByte(parameterIndex, b);
            case byte[] bytes -> ps.setBytes(parameterIndex, bytes);
            case URL url -> ps.setURL(parameterIndex, url);
            default -> ps.setObject(parameterIndex, val);
        }
    }

    private Mapping getMapping(Class<?> clazz) {
        return mappings.computeIfAbsent(clazz, aClass -> {
            Mapping mapping = new Mapping();
            try {
                Method mappingMethod = aClass.getMethod("mapping");
                if (Modifier.isStatic(mappingMethod.getModifiers())) {
                    try {
                        mapping = (Mapping) mappingMethod.invoke(null);
                    } catch (Exception e) {
                        throw rethrow(e);
                    }
                }
            } catch (NoSuchMethodException e) {
            }
            mapping.setOrm(this);
            mapping.setClass(aClass);
            return mapping;
        });
    }

    public GrugORM withMapping(Class classToMap, Mapping mapping) {
        mapping.setOrm(this);
        mapping.setClass(classToMap);
        mappings.put(classToMap, mapping);
        return this;
    }

    public GrugORM withMapping(Class classToMap, String tableName) {
        return withMapping(classToMap, new Mapping(){
            public String mapToTable() {
                return tableName;
            }
        });
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

        protected Mapping() {
        }

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
                    if (versionMapping == null) {
                        versionMapping = mapping;
                    } else {
                        throw new IllegalStateException("Cannot have more than one field as the version column: " + versionMapping.getFieldName() +
                                " and " + mapping.getFieldName() + " are both ids!");
                    }
                }
            }
            if (versionMapping == null) {
                String versionFieldName = orm.defaultVersionFieldName.apply(classForTable);
                versionMapping = fieldNameToMapping.get(versionFieldName);
            }
            return versionMapping;
        }

        private FieldMapping resolveIdMapping() {
            FieldMapping idMapping = null;
            for (FieldMapping mapping : fieldNameToMapping.values()) {
                if (mapping.isId()) {
                    if (idMapping == null) {
                        idMapping = mapping;
                    } else {
                        throw new IllegalStateException("Cannot have more than one field as the id column: " + idMapping.getFieldName() +
                                " and " + mapping.getFieldName() + " are both ids!");
                    }
                }
            }
            if (idMapping == null) {
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
        private <T> T newObjectFromResult(GrugORM orm, ResultSet resultSet, ColumnsSpec columnSpec) throws Exception {
            if (classForTable == ResultMap.class) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int i = metaData.getColumnCount();
                HashMap<String, Object> rawMap = new HashMap<>();
                for (int j = 1; j <= i; j++) {
                    String tableName = metaData.getTableName(j);
                    String columnName = metaData.getColumnName(j);
                    if (columnSpec.accept(tableName, columnName)) {
                        Object value = resultSet.getObject(columnName);
                        rawMap.put(columnName, value);
                    }
                }
                ResultMap resultMap = new ResultMap(orm, rawMap);
                return (T) resultMap;
            } else {
                T object;
                // if it's a record use the generated constructor
                if (recordComponents != null) {
                    Object[] args = new Object[recordComponents.length];
                    for (int i = 0; i < recordComponents.length; i++) {
                        RecordComponent recordComponent = recordComponents[i];
                        FieldMapping mapping = fieldNameToMapping.get(recordComponent.getName());
                        Object val = null;
                        if (columnSpec.accept(getTableName(), mapping.getColumnName())) {
                            val = mapping.getValueFromDatabase(resultSet);
                        }
                        args[i] = val;
                    }
                    object = (T) constructor.newInstance(args);
                } else {
                    // otherwise use fields
                    object = (T) constructor.newInstance();
                    for (FieldMapping fieldMapping : fieldNameToMapping.values()) {
                        try {
                            if (columnSpec.accept(getTableName(), fieldMapping.getColumnName())) {
                                fieldMapping.mapFromDatabase(object, resultSet);
                            }
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
                StringBuilder cols = new StringBuilder("[");
                for (int i = 1; i <= columnCount; i++) {
                    cols.append(metaData.getColumnName(i));
                    if (i < columnCount) {
                        cols.append(",");
                    }
                }
                cols.append("]");
                return cols.toString();
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
            if (idMapping == null) {
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

        public boolean hasColumn(String columnName) {
            return columnToMapping.containsKey(columnName);
        }

        public boolean hasIdColumn() {
            return idMapping != null;
        }

        public FieldMapping getFieldMapping(String field) {
            return fieldNameToMapping.get(field);
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
        Function<Object, Object> versionIncrementer;

        public FieldMapping(GrugORM orm, Field mappedField) {
            mappedField.setAccessible(true);
            this.orm = orm;
            this.mappedField = mappedField;
            this.columnName = orm.defaultFieldToColumnMapping.apply(mappedField);
            this.versionIncrementer = orm.defaultVersionIncrementer.apply(mappedField.getDeclaringClass());
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
                    //noinspection unchecked
                    fieldVal = resultSet.getObject(columnName, targetType);
                }
            } catch (SQLException e) {
                throw rethrow(e);
            }
            return fieldVal;
        }

        public FieldMapping asId() {
            this.idColumn = true;
            return this;
        }

        public FieldMapping asVersionColumn() {
            this.versionColumn = true;
            return this;
        }

        public FieldMapping withVersionIncrementer(UnaryOperator<Object> versionIncrementer) {
            this.versionIncrementer = versionIncrementer;
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

        public FieldMapping transformForDB(UnaryOperator<Object> func) {
            this.toDatabaseValue = func;
            return this;
        }

        public FieldMapping transformFromDB(UnaryOperator<Object> func) {
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
            Object updatedValue = versionIncrementer.apply(value);
            values.put(columnName, updatedValue);
            return updatedValue;
        }

        public Object getValueFromDBMap(Map<String, Object> values) {
            return values.get(columnName);
        }

        public void updateVersionValue(Object object, Object nextVersionValue) {
            safely(() -> mappedField.set(object, nextVersionValue));
        }

        public Class getType() {
            return mappedField.getType();
        }
    }

    private static class ColumnsSpec {

        private record Column(String column, String table, String alias) {
        }

        List<Column> columns = new ArrayList<>();
        boolean acceptAll = true;

        public ColumnsSpec(List<String> cols) {
            if (cols != null) {
                acceptAll = false;
                for (String col : cols) {
                    String[] colAlias = col.split(" (as|AS) ");
                    String start = colAlias[0];
                    String alias = null;
                    if (colAlias.length == 2) {
                        alias = colAlias[1].strip();
                    }
                    String[] tableSplit = start.split("\\.");
                    String table = null;
                    String column;
                    if (tableSplit.length == 2) {
                        table = tableSplit[0].strip();
                        column = tableSplit[1].strip();
                    } else {
                        column = start.strip();
                    }
                    columns.add(new Column(column, table, alias));
                }
            }
        }

        public boolean accept(String tableName, String columnName) {
            if (acceptAll) {
                return true;
            }
            for (Column column : columns) {
                if (column.table == null) {
                    if (columnName.equals(column.alias)) {
                        return true;
                    } else if (columnName.equals(column.column)) {
                        return true;
                    }
                } else {
                    if (columnName.equals(column.alias)) {
                        return true;
                    } else if (tableName.equals(column.table) && (columnName.equals(column.column) || "*".equals(column.column))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    //========================================================================================
    // Migrations System
    //========================================================================================

    public abstract static class Migrations {

        public static final String HELP_MSG = """
                Migrations Commands
                
                  show      - show all migrations
                  up        - apply one pending migration
                  down      - back out the latest migration
                  all       - apply all pending migrations
                  exit/quit - exit this tool
                  help/?    - show this help message
                """;
        private LinkedHashMap<String, GrugMigration> migrationsMap;
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
            if (migrationsMap.containsKey(migrationName)) {
                throw new IllegalArgumentException("Migration " + migrationName + " already exists!");
            }
            migrationsMap.put(migrationName, migration);
        }

        /**
         * @return the initial pre-migrations schema for the database
         */
        protected String initialSchema() {
            return "";
        }

        public abstract void migrations();

        public static GrugMigration makeMigration(String name) {
            return new GrugMigration(name);
        }

        public void console() {
            getORM();
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
                    console.printf(new BetterList<>(mergedMigrations.values()).join("\n"));
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
            getORM();
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
            getORM();
            orm.exec(GrugMigration.DDL);
            var mergedMigrations = loadMigrations(orm);

            var values = new BetterList<>(mergedMigrations.values());
            var firstUnappliedMigration = values.firstWhere(GrugMigration::isPending);
            if (firstUnappliedMigration != null) {
                firstUnappliedMigration.runUp(orm);
            } else {
                orm.getLogger().log(GrugLogger.Level.WARN, "No pending migrations were found in migrations file to apply");
            }
        }

        public void down() {
            getORM();
            orm.exec(GrugMigration.DDL);
            var mergedMigrations = loadMigrations(orm);

            var values = new BetterList<>(mergedMigrations.values());
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
            getORM();
            orm.exec(GrugMigration.DDL);
            var mergedMigrations = loadMigrations(orm);
            for (GrugMigration migration : mergedMigrations.values()) {
                if (!migration.isApplied()) {
                    migration.runUp(orm);
                }
            }
        }

        private LinkedHashMap<String, GrugMigration> loadMigrations(GrugORM orm) {

            migrationsMap = new LinkedHashMap<>();
            migrations();
            // compute migrations with persisted migrations merged in
            BetterList<GrugMigration> persistedMigrations = orm.find(GrugMigration.class).all().toList();
            var mergedMigrations = new LinkedHashMap<>(migrationsMap);
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
                    CREATE TABLE IF NOT EXISTS grug_migrations (
                        id INTEGER PRIMARY KEY,
                        applied_at DATETIME,
                        name VARCHAR UNIQUE NOT NULL,
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
                orm.withTransaction(() -> {
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
                orm.withTransaction(() -> {
                    String[] upSqlSplitOnSemicolons = getDownSqlSplitOnSemicolons();
                    for (String sql : upSqlSplitOnSemicolons) {
                        orm.exec(sql);
                    }
                    orm.delete(this);
                });
            }


            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                GrugMigration migration = (GrugMigration) o;
                return Objects.equals(name, migration.name) && Objects.equals(up, migration.up) && Objects.equals(down, migration.down);
            }


            public int hashCode() {
                return Objects.hash(name, up, down);
            }

            public MigrationStatus getStatus() {
                return status;
            }


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

    @SuppressWarnings("NullableProblems")
    public static class ResultMap implements Map<String, Object> {
        private final GrugORM orm;
        private Map<String, Object> result;

        public ResultMap(GrugORM orm, Map<String, Object> backingMap) {
            this.orm = orm;
            result = Collections.unmodifiableMap(backingMap);
        }

        // automatic down-casting helpers
        // TODO is the type parameter necessary here?
        public <T> T get(String key, Class<T> type) {
            return (T) result.get(key);
        }

        public String getString(String key) {
            return this.get(key, String.class);
        }

        public Short getShort(String key) {
            return this.get(key, Short.class);
        }

        public Integer getInteger(String key) {
            return this.get(key, Integer.class);
        }

        public Long getLong(String key) {
            return this.get(key, Long.class);
        }

        public Float getFloat(String key) {
            return this.get(key, Float.class);
        }

        public Double getDouble(String key) {
            return this.get(key, Double.class);
        }

        public BigDecimal getBigDecimal(String key) {
            return this.get(key, BigDecimal.class);
        }

        public Date getDate(String key) {
            return this.get(key, Date.class);
        }

        public Boolean getBoolean(String key) {
            return this.get(key, Boolean.class);
        }

        // the 'as' methods will attempt to coerce a value to the given type

        public String asString(String key) {
            return (String) orm.sloppyCoerce(String.class, get(key));
        }

        public Short asShort(String key) {
            return (Short) orm.sloppyCoerce(Short.class, get(key));
        }

        public Integer asInteger(String key) {
            return (Integer) orm.sloppyCoerce(Integer.class, get(key));
        }

        public Long asLong(String key) {
            return (Long) orm.sloppyCoerce(Long.class, get(key));
        }

        public Float asFloat(String key) {
            return (Float) orm.sloppyCoerce(Float.class, get(key));
        }

        public Double asDouble(String key) {
            return (Double) orm.sloppyCoerce(Double.class, get(key));
        }

        public BigDecimal asBigDecimal(String key) {
            return (BigDecimal) orm.sloppyCoerce(BigDecimal.class, get(key));
        }

        public Date asDate(String key) {
            return (Date) orm.sloppyCoerce(Date.class, get(key));
        }

        public Boolean asBoolean(String key) {
            return (Boolean) orm.sloppyCoerce(Boolean.class, get(key));
        }

        // create a case-insensitive version of the results, some dbs are ugly w/ the cases
        public ResultMap toCaseInsensitiveMap() {
            TreeMap<String, Object> caseInsensitiveMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveMap.putAll(this);
            return new ResultMap(orm, caseInsensitiveMap);
        }

        public int size() {
            return result.size();
        }

        public boolean isEmpty() {
            return result.isEmpty();
        }

        public boolean containsKey(Object key) {
            return result.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return result.containsValue(value);
        }

        public Object get(Object key) {
            return result.get(key);
        }

        public Object put(String key, Object value) {
            return result.put(key, value);
        }

        public Object remove(Object key) {
            return result.remove(key);
        }

        public void putAll(Map<? extends String, ?> m) {
            result.putAll(m);
        }

        public void clear() {
            result.clear();
        }

        public Set<String> keySet() {
            return result.keySet();
        }

        public Collection<Object> values() {
            return result.values();
        }

        public Set<Entry<String, Object>> entrySet() {
            return result.entrySet();
        }
    }

    public static class BetterList<T> extends ArrayList<T> implements Interfaces.BetterIterable<T> {

        public BetterList() {
        }

        public BetterList(Collection<? extends T> c) {
            super(c);
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

        public BetterList<T> copy() {
            return new BetterList<>(this);
        }
    }

    public static class QueryResult<T> implements Interfaces.BetterIterable<T> {

        private final GrugORM orm;

        // query specification
        private final String sql;
        private final Map<String, Object> args;
        private final Class resultClass;
        private final ColumnsSpec columnSpec;

        // results
        private BetterList<T> results;
        private List<T> readOnlyResults;

        private QueryResult(GrugORM orm, String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec, BetterList<T> resultList) {
            this.orm = orm;
            this.sql = sql;
            this.args = args;
            this.resultClass = resultClass;
            this.columnSpec = columnSpec;
            this.results = resultList;
            this.readOnlyResults = Collections.unmodifiableList(results);
        }

        public QueryResult<T> copy() {
            return new QueryResult<>(orm, sql, args, resultClass, columnSpec, results.copy());
        }

        public void reload() {
            results = new BetterList<>();
            readOnlyResults = Collections.unmodifiableList(results);
            orm.select(sql, args, resultClass, columnSpec, results);
        }

        public BetterList<T> getRawList() {
            return results;
        }

        public Iterator<T> iterator() {
            return readOnlyResults.iterator();
        }

        public List<T> getAsReadOnlyList() {
            return readOnlyResults;
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
        FORCE_THROWER.accept(e);
        return new RuntimeException(e); // never hit
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

    private static Consumer<Exception> generateForceThrower() {
        var tmpClass = new ClassLoader(GrugORM.class.getClassLoader()) {
            public Class defineClass() {
                byte[] bytes = Base64.getDecoder().decode("yv66vgAAADQAEgEAGGdydWcvZGIvRm9yY2VUaHJvd2VySW1wbAcAAQEAEGphdmEvbGFuZy9PYmplY3QHAAMBABtqYXZhL3V0aWwvZnVuY3Rpb24vQ29uc3VtZXIHAAUBABVGb3JjZVRocm93ZXJJbXBsLmphdmEBAAY8aW5pdD4BAAMoKVYMAAgACQoABAAKAQAGYWNjZXB0AQAVKExqYXZhL2xhbmcvT2JqZWN0OylWAQATamF2YS9sYW5nL1Rocm93YWJsZQcADgEABENvZGUBAApTb3VyY2VGaWxlACEAAgAEAAEABgAAAAIAAQAIAAkAAQAQAAAAEQABAAEAAAAFKrcAC7EAAAAAAAEADAANAAEAEAAAABEAAQACAAAABSvAAA+/AAAAAAABABEAAAACAAc=");
                return defineClass("grug.db.ForceThrowerImpl", bytes, 0, bytes.length);
            }
        }.defineClass();
        try {
            //noinspection
            return (Consumer) tmpClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}