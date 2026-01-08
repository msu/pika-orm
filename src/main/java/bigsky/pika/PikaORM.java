package bigsky.pika;

import bigsky.pika.bean.PikaManyRelation;
import bigsky.pika.bean.PikaManyThroughRelation;
import bigsky.pika.bean.PikaRecordLifecycle;
import bigsky.pika.cache.*;
import bigsky.pika.logging.DefaultLogger;
import bigsky.pika.logging.PikaLogger;
import bigsky.pika.mapping.*;
import bigsky.pika.migrations.Migrations;
import bigsky.pika.query.*;
import bigsky.pika.query.PikaList;
import bigsky.pika.query.QueryResult;
import bigsky.pika.query.ResultMap;
import bigsky.pika.session.ConnectionSession;
import bigsky.pika.util.RunnableWithException;
import bigsky.pika.util.SQLString;
import bigsky.pika.util.SafeAutoCloseable;
import bigsky.pika.util.TextTools;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PikaORM {

    public static final Pattern SQL_VARS_PATTERN = Pattern.compile("(?<!\\\\):(\\w+)");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd[[ ]['T']HH:mm[:ss][XXX]]");

    private static PikaORM DEFAULT_ORM = null;

    private static final ThreadLocal<ConnectionSession> CURRENT_SESSION = new ThreadLocal<>();
    private static final ThreadLocal<AtomicLong> QUERY_COUNT = new ThreadLocal<>();
    private static final ThreadLocal<QueryCache> QUERY_CACHE = new ThreadLocal<>();

    public static Object[] EMPTY_ARRAY = new Object[0];

    private final Callable<Connection> connectionSource;

    // Logger stuff
    private PikaLogger.Level internalLoggerLevel = PikaLogger.Level.INFO;
    private PikaLogger logger = null;  // Initialized in constructor
    private boolean logQueries = false;

    // Mapping stuff
    private final ConcurrentHashMap<Class, Mapping> mappings = new ConcurrentHashMap<>();

    // Coercions
    public static final Object NULL_SENTINEL = new Object();
    List<BiFunction<Class, Object, Object>> coercers = new ArrayList<>();

    // Default mapping logic
    private Function<Class, String> defaultClassToTableMapping = aClass -> {
        String className = aClass.getSimpleName();
        String snakeCase = TextTools.snakeCase(className);
        String plural = TextTools.pluralize(snakeCase);
        return plural;
    };
    private Function<Field, String> defaultFieldToColumnMapping = field -> TextTools.snakeCase(field.getName());

    private Function<Class, String> defaultIdFieldName = aClass -> "id";

    private Function<Class, String> defaultUUIDFieldName = aClass -> "uuid";
    private Function<Class, Supplier<Object>> defaultUUIDGenerator = aClass -> () -> UUID.randomUUID().toString();

    private Function<Class, String> defaultFkColumnName = aClass -> TextTools.snakeCase(aClass.getSimpleName()) + "_id";

    private Function<Class, String> defaultVersionFieldName = aClass -> "version";
    private Function<Class, Function<Object, Object>> defaultVersionIncrementer = aClass -> previousValue -> {
        if (previousValue == null) {
            return 1;
        } else {
            return ((Long) previousValue) + 1;
        }
    };

    private Reflector reflector = new StandardReflector();

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

    public PikaORM(Callable<Connection> connectionSource) {
        this.connectionSource = connectionSource;
        this.logger = new DefaultLogger(internalLoggerLevel);
        getMapping(Migrations.PikaMigration.class); // register pika migrations before any customizations are made
    }

    public PikaORM(String connectionString) {
        this(() -> DriverManager.getConnection(connectionString));
    }

    public PikaORM withLogger(PikaLogger logger) {
        this.logger = logger;
        // custom loggers get everything by default
        this.withLogLevel(PikaLogger.Level.TRACE);
        return this;
    }

    public PikaORM withLogLevel(Object level) {
        internalLoggerLevel = PikaLogger.Level.valueOf(String.valueOf(level));
        return this;
    }

    public PikaORM withMigrations(Migrations migrations) {
        migrations.setORM(this);
        this.migrations = migrations;
        return this;
    }

    public PikaORM applyMigrations() {
        if (migrations != null) {
            migrations.applyAll();
        }
        return this;
    }

    public PikaORM withDefaultTableMapping(Function<Class, String> val) {
        defaultClassToTableMapping = val;
        return this;
    }

    public PikaORM withDefaultColumnMapping(Function<Field, String> val) {
        defaultFieldToColumnMapping = val;
        return this;
    }

    public PikaORM withDefaultIdField(Function<Class, String> val) {
        defaultIdFieldName = val;
        return this;
    }

    public PikaORM withDefaultUUIDField(Function<Class, String> val) {
        defaultUUIDFieldName = val;
        return this;
    }

    public PikaORM withDefaultFkColumn(Function<Class, String> val) {
        defaultFkColumnName = val;
        return this;
    }

    public PikaORM withDefaultVersionColumnName(Function<Class, String> val) {
        defaultVersionFieldName = val;
        return this;
    }

    public PikaORM withDefaultVersionIncrementer(Function<Class, Function<Object, Object>> val) {
        defaultVersionIncrementer = val;
        return this;
    }

    public PikaORM withNoDefaultVersionColumn() {
        defaultVersionFieldName = clazz -> null;
        return this;
    }

    public PikaORM withDefaultPageSize(int pageSize) {
        this.defaultPageSize = pageSize;
        return this;
    }

    public PikaORM withOffsetClause(String offsetClause) {
        this.limitOffsetClause = offsetClause;
        return this;
    }

    public PikaORM withSQLiteQuirks() {
        this.sqlLiteQuirks = true;
        return this;
    }

    public PikaORM withCoercion(BiFunction<Class, Object, Object> coercion) {
        coercers.add(coercion);
        return this;
    }

    public PikaORM withReflector(Reflector reflector) {
        this.reflector = reflector;
        return this;
    }

    public PikaORM logQueries() {
        this.logQueries = true;
        return this;
    }

    public boolean getLogQueries() {
        return logQueries;
    }

    public void doNotLogQueries() {
        logQueries = false;
    }

    public void clearMappings() {
        mappings.clear();
        logger.log(PikaLogger.Level.INFO, "Cleared ORM mappings cache");
    }

    public PikaORM makeDefaultORM() {
        setDefaultORM(this);
        return this;
    }


    // Getters for extracted classes
    public PikaLogger getLogger() {
        return logger;
    }

    public Reflector getReflector() {
        return reflector;
    }

    public Function<Field, String> getDefaultFieldToColumnMapping() {
        return defaultFieldToColumnMapping;
    }

    public Function<Class, Function<Object, Object>> getDefaultVersionIncrementer() {
        return defaultVersionIncrementer;
    }

    public Function<Class, Supplier<Object>> getDefaultUUIDGenerator() {
        return defaultUUIDGenerator;
    }

    public Function<Class, String> getDefaultClassToTableMapping() {
        return defaultClassToTableMapping;
    }

    public Function<Class, String> getDefaultIdFieldName() {
        return defaultIdFieldName;
    }

    public Function<Class, String> getDefaultUUIDFieldName() {
        return defaultUUIDFieldName;
    }

    public Function<Class, String> getDefaultFkColumnName() {
        return defaultFkColumnName;
    }

    public Function<Class, String> getDefaultVersionFieldName() {
        return defaultVersionFieldName;
    }

    public String getLimitOffsetClause() {
        return limitOffsetClause;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public Mapping getMapping(Class<?> clazz) {
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

    // Methods for ConnectionSession
    public void setCurrentSession(ConnectionSession session) {
        CURRENT_SESSION.set(session);
    }

    public void setValueForQuery(PreparedStatement ps, int parameterIndex, Object val) throws SQLException {
        if (val == null) {
            ps.setNull(parameterIndex, Types.NULL);
            return;
        }

        // enums serialize as strings
        if (val instanceof Enum<?> e) {
            val = e.name();
        }

        if (val instanceof Boolean b) {
            ps.setBoolean(parameterIndex, b);
        } else if (val instanceof Short s) {
            ps.setShort(parameterIndex, s);
        } else if (val instanceof Integer i) {
            ps.setInt(parameterIndex, i);
        } else if (val instanceof Long l) {
            ps.setLong(parameterIndex, l);
        } else if (val instanceof Float f) {
            ps.setDouble(parameterIndex, f);
        } else if (val instanceof Double d) {
            ps.setDouble(parameterIndex, d);
        } else if (val instanceof BigDecimal bd) {
            ps.setBigDecimal(parameterIndex, bd);
        } else if (val instanceof String str) {
            ps.setString(parameterIndex, str);
        } else if (val instanceof Time d) {
            ps.setTime(parameterIndex, d);
        } else if (val instanceof Timestamp ts) {
            ps.setTimestamp(parameterIndex, ts);
        } else if (val instanceof Date d) {
            ps.setTimestamp(parameterIndex, new Timestamp(d.getTime()));
        } else if (val instanceof LocalDate ld) {
            ps.setDate(parameterIndex, java.sql.Date.valueOf(ld));
        } else if (val instanceof LocalDateTime ldt) {
            ps.setTimestamp(parameterIndex, Timestamp.valueOf(ldt));
        } else if (val instanceof Blob blob) {
            ps.setBlob(parameterIndex, blob);
        } else if (val instanceof NClob nclob) {
            ps.setNClob(parameterIndex, nclob);
        } else if (val instanceof Clob clob) {
            ps.setClob(parameterIndex, clob);
        } else if (val instanceof Byte b) {
            ps.setByte(parameterIndex, b);
        } else if (val instanceof byte[] bytes) {
            ps.setBytes(parameterIndex, bytes);
        } else if (val instanceof URL url) {
            ps.setURL(parameterIndex, url);
        } else {
            ps.setObject(parameterIndex, val);
        }
    }

    //====================================================================
    // default orm management
    //====================================================================

    public static PikaORM get() {
        PikaORM defaultORM = getDefault();
        if (defaultORM == null) {
            throw new IllegalStateException("No default PikaORM found");
        }
        return defaultORM;
    }

    public static PikaORM getDefault() {
        return DEFAULT_ORM;
    }

    public static void setDefaultORM(PikaORM orm) {
        DEFAULT_ORM = orm;
    }

    //====================================================================
    // Coercion System
    //====================================================================

    public <T> T coerce(Class<T> targetClass, Object value) {
        if (value == null) {
            return null;
        }
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
        if (result == null) {
            throw new IllegalArgumentException("No coercions found from object of type " +
                    value.getClass().getSimpleName() + " with value " + value + " to class " +
                    targetClass.getSimpleName());
        }
        if (result == NULL_SENTINEL) result = null;
        return (T) result;
    }

    public Object sloppyCoerce(Class targetClass, Object value) {
        try {
            return coerce(targetClass, value);
        } catch (Exception e) {
            if (!(value instanceof String)) {
                try {
                    // return as string
                    return coerce(targetClass, String.valueOf(value));
                } catch (Exception ex) {
                    // ignore, rethrow original exception
                }
            }
            throw rethrow(e);
        }
    }

    private Object defaultCoercions(Class targetType, Object value) {
        if (targetType.isInstance(value)) {
            return value;
        } else if (targetType.isEnum()) {
            return Enum.valueOf(targetType, String.valueOf(value).toUpperCase());
        } else if (targetType == String.class) {
            return String.valueOf(value);
        } else if (Number.class.isAssignableFrom(targetType) && ("".equals(value) || "null".equals(value))) {
            return NULL_SENTINEL;
        } else if ((targetType == Short.class || targetType == short.class) && value instanceof String s) {
            return Short.valueOf(s);
        } else if ((targetType == Integer.class || targetType == int.class) && value instanceof String s) {
            return Integer.valueOf(s);
        } else if ((targetType == Integer.class || targetType == int.class) && value instanceof Short s) {
            return Integer.valueOf(s);
        } else if ((targetType == Long.class || targetType == long.class) && value instanceof String s) {
            return Long.valueOf(s);
        } else if ((targetType == Long.class || targetType == long.class) && value instanceof Short s) {
            return Long.valueOf(s);
        } else if ((targetType == Long.class || targetType == long.class) && value instanceof Integer i) {
            return Long.valueOf(i);
        } else if ((targetType == Float.class || targetType == float.class) && value instanceof String s) {
            return Float.valueOf(s);
        } else if ((targetType == Double.class || targetType == double.class) && value instanceof String s) {
            return Double.valueOf(s);
        } else if ((targetType == Double.class || targetType == double.class) && value instanceof Float f) {
            return Double.valueOf(f);
        } else if (targetType == BigInteger.class && value instanceof String s) {
            return new BigInteger(s);
        } else if (targetType == BigDecimal.class && value instanceof String s) {
            return new BigDecimal(s);
        } else if (targetType == LocalDateTime.class && value instanceof String s) {
            return safely(() -> {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            });
        } else if (targetType == LocalDate.class && value instanceof String s) {
            try {
                return LocalDate.parse(s);
            } catch (DateTimeException e) {
                throw new IllegalArgumentException("Unable to convert string to LocalDate: " + value);
            }
        } else if (targetType == Date.class && value instanceof String s) {
            try {
                return new Date(Long.parseLong(s));
            } catch (NumberFormatException nfe) {
                // if the value is not a long, try to parse it as a date string
                TemporalAccessor parsedDate = DATE_TIME_FORMATTER.parse(s);
                try {
                    // Try to convert to Instant first (works if all fields present)
                    Instant instant = Instant.from(parsedDate);
                    return Date.from(instant);
                } catch (DateTimeException e) {
                    // If that fails, try LocalDateTime
                    try {
                        LocalDateTime localDateTime = LocalDateTime.from(parsedDate);
                        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                    } catch (DateTimeException e2) {
                        // If that fails, try LocalDate (date only, no time)
                        try {
                            LocalDate localDate = LocalDate.from(parsedDate);
                            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                        } catch (DateTimeException e3) {
                            throw new IllegalArgumentException("Unable to convert temporal to Date: " + parsedDate);
                        }
                    }
                }

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

    public <J, T> PikaManyThroughRelation<J, T> loadManyThrough(Object one, Class<J> joinClass, Class<T> classOfMany) {
        return maybeCache(new LoadManyThroughKey(one, joinClass, classOfMany), () -> {
            Mapping oneMapping = getMapping(one.getClass());
            String oneFk = oneMapping.getDefaultForeignKeyColumnName();
            Mapping manyMapping = getMapping(classOfMany);
            String manyFk = manyMapping.getDefaultForeignKeyColumnName();
            return new PikaManyThroughRelation<>(one, oneFk, joinClass, classOfMany, manyFk, this);
        });
    }

    public <T> PikaManyRelation<T> loadMany(Object one, Class<T> classOfMany) {
        Mapping mapping = getMapping(one.getClass());
        String fkName = mapping.getDefaultForeignKeyColumnName();
        return loadMany(one, classOfMany, fkName);
    }

    public <T> PikaManyRelation<T> loadMany(Object one, Class<T> classOfMany, String manyFk) {
        return maybeCache(new LoadManyKey(one, classOfMany, manyFk), () -> new PikaManyRelation<>(one, classOfMany, manyFk, this));
    }

    public <T> T load(Object objectWithFk, Class<T> classToLoad) {
        Mapping mapping = getMapping(classToLoad);
        String fkName = mapping.getDefaultForeignKeyColumnName();
        return load(objectWithFk, classToLoad, fkName);
    }

    public <T> T load(Object objectWithFk, Class<T> classToLoad, String foreignKeyColumn) {
        return maybeCache(new LoadKey(objectWithFk, classToLoad, foreignKeyColumn), () -> {
            Mapping metaData = getMapping(objectWithFk.getClass());
            Object parentPkValue = metaData.getValueForColumn(objectWithFk, foreignKeyColumn);
            return find(classToLoad).byId(parentPkValue);
        });
    }

    public <T> T loadReverse(Object objectWithPk, Class<T> classToLoad) {
        Mapping mapping = getMapping(objectWithPk.getClass());
        String fkName = mapping.getDefaultForeignKeyColumnName();
        return loadReverse(objectWithPk, classToLoad, fkName);
    }

    public <T> T loadReverse(Object objectWithPk, Class<T> classToLoad, String foreignKeyColumn) {
        return maybeCache(new LoadReverseKey(objectWithPk, classToLoad, foreignKeyColumn), () -> {
            Mapping metaData = getMapping(objectWithPk.getClass());
            Object parentPkValue = metaData.getId(objectWithPk);
            return find(classToLoad).byKey(foreignKeyColumn, parentPkValue);
        });
    }

    private <T> T maybeCache(Object key, Supplier<T> supplier) {
        QueryCache queryCache = getQueryCache();
        if (queryCache != null) {
            return queryCache.cache(key, supplier);
        } else {
            return supplier.get();
        }
    }

    //====================================================================
    // Main entrypoints into the query layer
    //====================================================================

    public <T> PikaClassFinder<T> find(Class<T> classToFind) {
        return new PikaClassFinder<>(this, classToFind);
    }

    public <T> PikaStreamFinder<T> stream(Class<T> classToFind) {
        return new PikaStreamFinder<>(this, classToFind);
    }

    public <T> PikaClassQuery<T> query(Class<T> baseClass) {
        return new PikaClassQuery<>(this, baseClass);
    }

    public PikaQuery<ResultMap> queryBuilder(String baseTable) {
        return new PikaQuery<>(this, baseTable);
    }

    public void startThreadQueryCount() {
        QUERY_COUNT.set(new AtomicLong(0));
    }

    public void incrementThreadQueryCount() {
        AtomicLong count = QUERY_COUNT.get();
        if (count != null) {
            count.incrementAndGet();
        }
    }

    public long getThreadQueryCount() {
        AtomicLong count = QUERY_COUNT.get();
        if (count == null) {
            return 0;
        } else {
            return count.longValue();
        }
    }

    //====================================================================
    // Cache management
    //====================================================================

    public void startQueryCaching() {
        QUERY_CACHE.set(new QueryCache(logger, logQueries));
    }

    public void endQueryCaching() {
        QUERY_CACHE.remove();
    }

    public void clearQueryCache() {
        QueryCache queryCache = QUERY_CACHE.get();
        if(queryCache != null) {
            queryCache.clear();
        };
    }

    public QueryCache getQueryCache() {
        return QUERY_CACHE.get();
    }

    public SafeAutoCloseable suppressQueries() {
        boolean originalValue = logQueries;
        logQueries = false;
        return () -> logQueries = originalValue;
    }


    //====================================================================
    // Connection management
    //====================================================================


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
        ConnectionSession newSession = new ConnectionSession(this, currentSession);
        logger.log(PikaLogger.Level.DEBUG, "Created a new connection for Thread {} w/ID {}", Thread.currentThread().getName(), newSession.getUUID());
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
            logger.log(PikaLogger.Level.ERROR, "No current connection for transaction.");
        } else {
            connectionSession.finishTransaction();
        }
    }

    public void rollBackTransaction() {
        ConnectionSession connectionSession = getCurrentSession();
        if (connectionSession == null) {
            logger.log(PikaLogger.Level.ERROR, "No current connection for transaction.");
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
        PikaList<T> resultList = new PikaList<>();
        QueryResult<T> queryResult = new QueryResult<>(this, sql, args, resultClass, columnSpec, resultList);
        select(sql, args, resultClass, columnSpec, resultList);
        return queryResult;
    }

    public <T> void select(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec, PikaList<T> results) {
        Mapping mapping = getMapping(resultClass);
        ArrayList<Object> vals = new ArrayList<>();
        String updatedSql = updateSqlVars(sql, args, vals);
        logQuery("Issuing SQL Query:", sql, args);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(updatedSql, vals);
             var resultSet = session.execute(ps)) {
            while (resultSet.next()) {
                T result = mapping.newObjectFromResult(this, resultSet, columnSpec);
                if (result instanceof PikaRecordLifecycle lifecycle) {//this throws a warn
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

    public <T> Stream<T> stream(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec) {
        var session = getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("You must manually establish a connection with establishConnection() and manage closing the connection yourself before streaming results");
        }
        Mapping mapping = getMapping(resultClass);
        ArrayList<Object> vals = new ArrayList<>();
        String updatedSql = updateSqlVars(sql, args, vals);//SQL, Argument Map, Blank Value list to be filled
        logQuery("Issuing SQL Query:", sql, args);
        try {
            PreparedStatement ps = session.prepareStatement(updatedSql, vals);
            ResultSet rs = session.execute(ps);
            return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED) {
                public boolean tryAdvance(Consumer<? super T> action) {
                    try {
                        if (rs.next()) {
                            T result = mapping.newObjectFromResult(PikaORM.this, rs, columnSpec);
                            if (result instanceof PikaRecordLifecycle lifecycle) {
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
        logger.log(PikaLogger.Level.ERROR, """
                Exception in select() with SQL
                {}\s
                with args {}: {}""", TextTools.indent(2, sql), args, e.getMessage());
        throw rethrow(e);
    }

    public Long insert(Object object) {
        if (object instanceof PikaRecordLifecycle lifecycle && !lifecycle.validate()) {
            return null;
        }
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        Map<String, Object> values = mapping.toDatabaseMap(object);//this is the place to check?
        String keyCol = null;
        if (mapping.hasIdColumn()) {
            keyCol = mapping.getIdColumn();
            values.remove(keyCol);
        }
        if (object instanceof PikaRecordLifecycle lifecycle && !lifecycle.beforeInsert()) {
            return null;
        }
        Object newVersionValue = null;
        Object uuid = null;
        FieldMapping uuidFieldMapping = null;
        if (mapping.hasUUIDColumn()) {
            uuidFieldMapping = mapping.getUUIDMapping();
            String uuidColumn = uuidFieldMapping.getColumnName();
            if (values.get(uuidColumn) == null) {
                uuid = uuidFieldMapping.generateUUID();
                values.putIfAbsent(uuidColumn, uuid);
            }
        }
        // TODO - remove this?  It's an insert...
        if (mapping.hasVersionColumn()) {
            newVersionValue = mapping.incrementVersion(values);
        }
        Long id;
        if (keyCol != null) {
            id = insert(mapping.getTableName(), values, keyCol);
        } else {
            id = insert(mapping.getTableName(), values);
        }
        if (mapping.hasVersionColumn() && id != null) {
            mapping.updateVersionValue(object, newVersionValue);
        }
        if (!mapping.isReadOnly() && id != null) {
            mapping.setId(object, id);
        }
        if (uuidFieldMapping != null && uuid != null) {
            uuidFieldMapping.setFieldValue(object, uuid);
        }
        if (object instanceof PikaRecordLifecycle lifecycle) {
            lifecycle.afterInsert();
        }
        return id;
    }

    public void insertAll(Object... items) {
        insertAll(List.of(items));
    }

    public void insertAll(List<Object> items) {
        if (items.isEmpty()) {
            return;
        }

        // Metadata
        Object templateItem = items.get(0);
        Class<?> templateClass = templateItem.getClass();
        Mapping mapping = getMapping(templateClass);
        Set<String> columns = mapping.toDatabaseMap(templateItem).keySet();
        String tableName = mapping.getTableName();
        String idColumn = mapping.getIdColumn();
        columns.remove(idColumn);

        // Query builder
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(tableName).append(" (")
                .append(String.join(", ", columns)).append(") VALUES ");

        // Map to
        List<Object> values = new ArrayList<>();
        for (Object item : items) {
            if (!templateClass.isInstance(item)) {
                throw new IllegalStateException("All values passed to insertAll() must be the same type!  Expected " +
                        templateClass.getSimpleName() + " but found " + item.getClass().getSimpleName());
            }

            Map<String, Object> mapForItem = mapping.toDatabaseMap(item);
            mapForItem.remove(idColumn);

            Collection<Object> valuesForItem = mapForItem.values();
            values.addAll(valuesForItem);

            sb.append("(").append("?, ".repeat(valuesForItem.size()));
            sb.delete(sb.length() - 2, sb.length());
            sb.append("), ");
        }
        sb.delete(sb.length() - 2, sb.length());

        String insertString = sb.toString();

        logQuery("Bulk Insert SQL:", insertString, Map.of());
        logger.log(getQueryLogLevel(), "BULK INSERT SQL: {}\n  Args:{}", insertString, values);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(insertString, values)) {
            time(ps::executeUpdate);
        } catch (Exception e) {
            logger.log(PikaLogger.Level.ERROR, "Exception in insertAll() with SQL {} & args {}: {}", insertString, values, e.getMessage());
            throw rethrow(e);
        }
    }

    private Long insert(String tableName, Map<String, Object> values, String... keyCols) {
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
                sb.append(Arrays.stream(keyCols).map(col -> "DEFAULT").collect(Collectors.joining(", ")));
                sb.append(")");
            }
        } else {
            sb.append(" (");
            sb.append(String.join(", ", values.keySet()));
            sb.append(") VALUES (");
            sb.append(values.keySet().stream().map(key -> "?").collect(Collectors.joining(", ")));
            sb.append(")");
        }
        String insertString = sb.toString();
        Collection<Object> queryValues = values.values();
        logger.log(getQueryLogLevel(), "INSERT SQL: {}\n  Args:{}", insertString, queryValues);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(insertString, queryValues, keyCols == null ? new String[0] : keyCols)) {
            time(ps::executeUpdate);
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (keyCols.length > 0 && generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.log(PikaLogger.Level.ERROR, "Exception in insert() with SQL {} & args {}: {}", insertString, queryValues, e.getMessage());
            throw rethrow(e);
        }
    }

    public boolean update(Object object) {
        if (object instanceof PikaRecordLifecycle lifecycle && !lifecycle.validate()) {
            return false;
        }
        Class<?> clazz = object.getClass();
        Mapping mapping = getMapping(clazz);
        String tableName = mapping.getTableName();
        String keyCol = mapping.getIdColumn();
        Map<String, Object> valuesToUpdate = mapping.toDatabaseMap(object);
        Object keyVal = valuesToUpdate.remove(keyCol); // remove the key
        String versionColumn = null;
        Object currentVersionValue = null;
        Object nextVersionValue = null;
        if (mapping.hasVersionColumn()) {
            versionColumn = mapping.getVersionColumn();
            currentVersionValue = mapping.getCurrentVersion(valuesToUpdate);
            nextVersionValue = mapping.incrementVersion(valuesToUpdate);
        }
        if (object instanceof PikaRecordLifecycle lifecycle && !lifecycle.beforeUpdate(valuesToUpdate)) {
            return false;
        }
        boolean update = update(tableName, keyCol, keyVal, versionColumn, currentVersionValue, valuesToUpdate);
        if (mapping.hasVersionColumn() && update) {
            mapping.updateVersionValue(object, nextVersionValue);
        }
        if (object instanceof PikaRecordLifecycle lifecycle) {
            lifecycle.afterUpdate();
        }
        return update;
    }

    private boolean update(String tableName, String keyCol, Object keyVal, String versionCol, Object versionVal, Map<String, Object> values) {
        if (values.isEmpty()) {
            // nothing to update
            return true;
        }
        if (!(values instanceof TreeMap<String, Object>)) {
            values = new TreeMap<>(values);
        }
        StringBuilder sb = new StringBuilder("UPDATE ");
        sb.append(tableName);
        sb.append(" SET ");
        sb.append(values.keySet().stream().map(col -> col + "=?").collect(Collectors.joining(", ")));
        sb.append(" \nWHERE ");
        sb.append(keyCol).append("=?");
        if (versionCol != null) {
            sb.append(" AND ").append(versionCol).append("=?");
        }

        String updateSQL = sb.toString();

        // construct final values collection
        ArrayList<Object> finalValues = new ArrayList<>(values.values());
        finalValues.add(keyVal);
        if (versionCol != null) {
            finalValues.add(versionVal);
        }

        logQuery("Update SQL: ", updateSQL, values);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(updateSQL, finalValues)) {
            int i = time(ps::executeUpdate);
            return i == 1;
        } catch (Exception e) {
            logger.log(PikaLogger.Level.ERROR, "Exception in update() with SQL {} & args {}: {}", updateSQL, values.values(), e.getMessage());
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
        if (object instanceof PikaRecordLifecycle lifecycle && !lifecycle.beforeDelete()) {
            return false;
        }
        boolean delete = delete(tableName, keyCol, keyVal);
        if (object instanceof PikaRecordLifecycle lifecycle) {
            lifecycle.afterDelete();
        }
        return delete;
    }

    private boolean delete(String tableName, String keyCol, Object keyVal) {
        String deleteSQL = "DELETE FROM " + tableName + "\nWHERE " + keyCol + "=?";
        logQuery("Delete SQL:", deleteSQL, Map.of(keyCol, keyVal));
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(deleteSQL, List.of(keyVal))) {
            int i = time(ps::executeUpdate);
            return i == 1;
        } catch (Exception e) {
            logger.log(PikaLogger.Level.ERROR, "Exception in delete() with SQL {} & value {}: {}", deleteSQL, keyVal, e.getMessage());
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
        if (sql.isBlank()) {
            logger.log(PikaLogger.Level.WARN, "SQL is blank, will not be executed!");
            return false;
        }

        ArrayList<Object> vals = new ArrayList<>();
        String updatedSql;
        if (args.isEmpty()) {
            updatedSql = sql;
        } else {
            updatedSql = updateSqlVars(sql, args, vals);
        }
        logQuery("Executing Raw SQL:", updatedSql, args);
        try (var session = getOrCreateSession();
             var ps = session.prepareStatement(updatedSql, vals)) {
            boolean result = time(ps::execute);
            return result;
        } catch (Exception e) {
            logger.log(PikaLogger.Level.ERROR, "Exception in exec() with SQL {}: {}", sql, e.getMessage());
            throw rethrow(e);
        }
    }

    private void logQuery(String msg, String sql, Map<String, Object> args) {
        if (args.isEmpty()) {
            logger.log(getQueryLogLevel(), "{}\n{}", msg, new SQLString(sql));
        } else {
            logger.log(getQueryLogLevel(), "{}\n{}\nARGS:{}", msg, new SQLString(sql), args);
        }
    }

    // utilities
    public <T> T time(Callable<T> query) {
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

    public PikaLogger.Level getQueryLogLevel() {
        if (logQueries) {
            return PikaLogger.Level.INFO;
        } else {
            return PikaLogger.Level.DEBUG;
        }
    }

    private String updateSqlVars(String sql, Map<String, Object> args, List<Object> argList) {
        Matcher matcher = SQL_VARS_PATTERN.matcher(sql);
        StringBuilder finalSql = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group(1);
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


    //==================================================================
    //  Metadata stuff
    //==================================================================

    public PikaORM withMapping(Class classToMap, Mapping mapping) {
        mapping.setOrm(this);
        mapping.setClass(classToMap);
        mappings.put(classToMap, mapping);
        return this;
    }

    public PikaORM withMapping(Class classToMap, String tableName) {
        return withMapping(classToMap, new Mapping() {
            public String mapToTable() {
                return tableName;
            }
        });
    }

    public static void safely(RunnableWithException callable) {
        try {
            callable.run();
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public static <T> T safely(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public static <E extends Throwable> RuntimeException rethrow(Throwable e) throws E {
        throw (E) e;
    }

}