# PikaORM

### PikaORM(Callable<Connection> connectionSource)

```java
PikaORM(Callable<Connection> connectionSource)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `connectionSource`: _Describe connectionSource_

**Returns**:  
_Describe the return value..._

---

### PikaORM(String connectionString)

```java
PikaORM(String connectionString)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `connectionString`: _Describe connectionString_

**Returns**:  
_Describe the return value..._

---

### withLogger(PikaLogger logger)

```java
withLogger(PikaLogger logger)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `logger`: _Describe logger_

**Returns**:  
_Describe the return value..._

---

### withLogLevel(Object level)

```java
withLogLevel(Object level)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `level`: _Describe level_

**Returns**:  
_Describe the return value..._

---

### withMigrations(Migrations migrations)

```java
withMigrations(Migrations migrations)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `migrations`: _Describe migrations_

**Returns**:  
_Describe the return value..._

---

### applyMigrations()

```java
applyMigrations()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### withDefaultTableMapping(Function<Class, String> val)

```java
withDefaultTableMapping(Function<Class, String> val)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `val`: _Describe val_

**Returns**:  
_Describe the return value..._

---

### withDefaultColumnMapping(Function<Field, String> val)

```java
withDefaultColumnMapping(Function<Field, String> val)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `val`: _Describe val_

**Returns**:  
_Describe the return value..._

---

### withDefaultIdField(Function<Class, String> val)

```java
withDefaultIdField(Function<Class, String> val)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `val`: _Describe val_

**Returns**:  
_Describe the return value..._

---

### withDefaultUUIDField(Function<Class, String> val)

```java
withDefaultUUIDField(Function<Class, String> val)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `val`: _Describe val_

**Returns**:  
_Describe the return value..._

---

### withDefaultFkColumn(Function<Class, String> val)

```java
withDefaultFkColumn(Function<Class, String> val)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `val`: _Describe val_

**Returns**:  
_Describe the return value..._

---

### withDefaultVersionColumnName(Function<Class, String> val)

```java
withDefaultVersionColumnName(Function<Class, String> val)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `val`: _Describe val_

**Returns**:  
_Describe the return value..._

---

### withDefaultVersionIncrementer(Function<Class, Function<Object, Object>> val)

```java
withDefaultVersionIncrementer(Function<Class, Function<Object, Object>> val)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `val`: _Describe val_

**Returns**:  
_Describe the return value..._

---

### withNoDefaultVersionColumn()

```java
withNoDefaultVersionColumn()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### withDefaultPageSize(int pageSize)

```java
withDefaultPageSize(int pageSize)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `pageSize`: _Describe pageSize_

**Returns**:  
_Describe the return value..._

---

### withOffsetClause(String offsetClause)

```java
withOffsetClause(String offsetClause)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `offsetClause`: _Describe offsetClause_

**Returns**:  
_Describe the return value..._

---

### withSQLiteQuirks()

```java
withSQLiteQuirks()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### withCoercion(BiFunction<Class, Object, Object> coercion)

```java
withCoercion(BiFunction<Class, Object, Object> coercion)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `coercion`: _Describe coercion_

**Returns**:  
_Describe the return value..._

---

### withReflector(Reflector reflector)

```java
withReflector(Reflector reflector)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `reflector`: _Describe reflector_

**Returns**:  
_Describe the return value..._

---

### logQueries()

```java
logQueries()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getLogQueries()

```java
getLogQueries()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### doNotLogQueries()

```java
doNotLogQueries()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### logCaching()

```java
logCaching()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getLogCaching()

```java
getLogCaching()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### doNotLogCaching()

```java
doNotLogCaching()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### clearMappings()

```java
clearMappings()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### makeDefaultORM()

```java
makeDefaultORM()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getLogger()

```java
getLogger()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getReflector()

```java
getReflector()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultFieldToColumnMapping()

```java
getDefaultFieldToColumnMapping()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultVersionIncrementer()

```java
getDefaultVersionIncrementer()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultUUIDGenerator()

```java
getDefaultUUIDGenerator()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultClassToTableMapping()

```java
getDefaultClassToTableMapping()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultIdFieldName()

```java
getDefaultIdFieldName()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultUUIDFieldName()

```java
getDefaultUUIDFieldName()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultFkColumnName()

```java
getDefaultFkColumnName()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultVersionFieldName()

```java
getDefaultVersionFieldName()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getLimitOffsetClause()

```java
getLimitOffsetClause()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefaultPageSize()

```java
getDefaultPageSize()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getMapping(Class<?> clazz)

```java
getMapping(Class<?> clazz)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `clazz`: _Describe clazz_

**Returns**:  
_Describe the return value..._

---

### setCurrentSession(ConnectionSession session)

```java
setCurrentSession(ConnectionSession session)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `session`: _Describe session_

**Returns**:  
_Describe the return value..._

---

### setValueForQuery(PreparedStatement ps, int parameterIndex, Object val)

```java
setValueForQuery(PreparedStatement ps, int parameterIndex, Object val)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `ps`: _Describe ps_
- `parameterIndex`: _Describe parameterIndex_
- `val`: _Describe val_

**Returns**:  
_Describe the return value..._

---

### get()

```java
get()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getDefault()

```java
getDefault()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### setDefaultORM(PikaORM orm)

```java
setDefaultORM(PikaORM orm)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `orm`: _Describe orm_

**Returns**:  
_Describe the return value..._

---

### coerce(Class<T> targetClass, Object value)

```java
coerce(Class<T> targetClass, Object value)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `targetClass`: _Describe targetClass_
- `value`: _Describe value_

**Returns**:  
_Describe the return value..._

---

### sloppyCoerce(Class targetClass, Object value)

```java
sloppyCoerce(Class targetClass, Object value)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `targetClass`: _Describe targetClass_
- `value`: _Describe value_

**Returns**:  
_Describe the return value..._

---

### defaultCoercions(Class targetType, Object value)

```java
defaultCoercions(Class targetType, Object value)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `targetType`: _Describe targetType_
- `value`: _Describe value_

**Returns**:  
_Describe the return value..._

---

### loadManyThrough(Object one, Class<J> joinClass, Class<T> classOfMany)

```java
loadManyThrough(Object one, Class<J> joinClass, Class<T> classOfMany)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `one`: _Describe one_
- `joinClass`: _Describe joinClass_
- `classOfMany`: _Describe classOfMany_

**Returns**:  
_Describe the return value..._

---

### loadMany(Object one, Class<T> classOfMany)

```java
loadMany(Object one, Class<T> classOfMany)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `one`: _Describe one_
- `classOfMany`: _Describe classOfMany_

**Returns**:  
_Describe the return value..._

---

### loadMany(Object one, Class<T> classOfMany, String manyFk)

```java
loadMany(Object one, Class<T> classOfMany, String manyFk)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `one`: _Describe one_
- `classOfMany`: _Describe classOfMany_
- `manyFk`: _Describe manyFk_

**Returns**:  
_Describe the return value..._

---

### load(Object objectWithFk, Class<T> classToLoad)

```java
load(Object objectWithFk, Class<T> classToLoad)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `objectWithFk`: _Describe objectWithFk_
- `classToLoad`: _Describe classToLoad_

**Returns**:  
_Describe the return value..._

---

### load(Object objectWithFk, Class<T> classToLoad, String foreignKeyColumn)

```java
load(Object objectWithFk, Class<T> classToLoad, String foreignKeyColumn)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `objectWithFk`: _Describe objectWithFk_
- `classToLoad`: _Describe classToLoad_
- `foreignKeyColumn`: _Describe foreignKeyColumn_

**Returns**:  
_Describe the return value..._

---

### loadReverse(Object objectWithPk, Class<T> classToLoad)

```java
loadReverse(Object objectWithPk, Class<T> classToLoad)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `objectWithPk`: _Describe objectWithPk_
- `classToLoad`: _Describe classToLoad_

**Returns**:  
_Describe the return value..._

---

### loadReverse(Object objectWithPk, Class<T> classToLoad, String foreignKeyColumn)

```java
loadReverse(Object objectWithPk, Class<T> classToLoad, String foreignKeyColumn)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `objectWithPk`: _Describe objectWithPk_
- `classToLoad`: _Describe classToLoad_
- `foreignKeyColumn`: _Describe foreignKeyColumn_

**Returns**:  
_Describe the return value..._

---

### maybeCache(Object key, Supplier<T> supplier)

```java
maybeCache(Object key, Supplier<T> supplier)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `key`: _Describe key_
- `supplier`: _Describe supplier_

**Returns**:  
_Describe the return value..._

---

### find(Class<T> classToFind)

```java
find(Class<T> classToFind)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `classToFind`: _Describe classToFind_

**Returns**:  
_Describe the return value..._

---

### stream(Class<T> classToFind)

```java
stream(Class<T> classToFind)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `classToFind`: _Describe classToFind_

**Returns**:  
_Describe the return value..._

---

### query(Class<T> baseClass)

```java
query(Class<T> baseClass)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `baseClass`: _Describe baseClass_

**Returns**:  
_Describe the return value..._

---

### queryBuilder(String baseTable)

```java
queryBuilder(String baseTable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `baseTable`: _Describe baseTable_

**Returns**:  
_Describe the return value..._

---

### startThreadQueryCount()

```java
startThreadQueryCount()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### incrementThreadQueryCount()

```java
incrementThreadQueryCount()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getThreadQueryCount()

```java
getThreadQueryCount()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### startQueryCaching()

```java
startQueryCaching()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### endQueryCaching()

```java
endQueryCaching()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### clearQueryCache()

```java
clearQueryCache()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getQueryCache()

```java
getQueryCache()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### suppressQueries()

```java
suppressQueries()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getNewRawConnection()

```java
getNewRawConnection()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getOrCreateSession()

```java
getOrCreateSession()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### getCurrentSession()

```java
getCurrentSession()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### establishConnection()

```java
establishConnection()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### pushNewSession()

```java
pushNewSession()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### withTransaction(Runnable runnable)

```java
withTransaction(Runnable runnable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `runnable`: _Describe runnable_

**Returns**:  
_Describe the return value..._

---

### withTransaction(Callable<T> runnable)

```java
withTransaction(Callable<T> runnable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `runnable`: _Describe runnable_

**Returns**:  
_Describe the return value..._

---

### inTransaction(Runnable runnable)

```java
inTransaction(Runnable runnable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `runnable`: _Describe runnable_

**Returns**:  
_Describe the return value..._

---

### inTransaction(Callable<T> callable)

```java
inTransaction(Callable<T> callable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `callable`: _Describe callable_

**Returns**:  
_Describe the return value..._

---

### joinTransaction(Runnable runnable)

```java
joinTransaction(Runnable runnable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `runnable`: _Describe runnable_

**Returns**:  
_Describe the return value..._

---

### joinTransaction(Callable<T> callable)

```java
joinTransaction(Callable<T> callable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `callable`: _Describe callable_

**Returns**:  
_Describe the return value..._

---

### forceTransaction(Runnable runnable)

```java
forceTransaction(Runnable runnable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `runnable`: _Describe runnable_

**Returns**:  
_Describe the return value..._

---

### forceTransaction(Callable<T> callable)

```java
forceTransaction(Callable<T> callable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `callable`: _Describe callable_

**Returns**:  
_Describe the return value..._

---

### isInTransaction()

```java
isInTransaction()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### requireActiveTransaction()

```java
requireActiveTransaction()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### startTransaction()

```java
startTransaction()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### maybeCommitTransaction()

```java
maybeCommitTransaction()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### commitTransaction()

```java
commitTransaction()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### rollBackTransaction()

```java
rollBackTransaction()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### select(String sql)

```java
select(String sql)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_

**Returns**:  
_Describe the return value..._

---

### select(String sql, Class<T> resultClass)

```java
select(String sql, Class<T> resultClass)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `resultClass`: _Describe resultClass_

**Returns**:  
_Describe the return value..._

---

### select(String sql, Map<String, Object> args)

```java
select(String sql, Map<String, Object> args)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_

**Returns**:  
_Describe the return value..._

---

### select(String sql, Map<String, Object> args, Class<T> resultClass)

```java
select(String sql, Map<String, Object> args, Class<T> resultClass)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_

**Returns**:  
_Describe the return value..._

---

### select(String sql, Map<String, Object> args, Class<T> resultClass, String... colsToMap)

```java
select(String sql, Map<String, Object> args, Class<T> resultClass, String... colsToMap)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_
- `colsToMap`: _Describe colsToMap_

**Returns**:  
_Describe the return value..._

---

### select(String sql, Map<String, Object> args, Class<T> resultClass, List<String> colsToMap)

```java
select(String sql, Map<String, Object> args, Class<T> resultClass, List<String> colsToMap)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_
- `colsToMap`: _Describe colsToMap_

**Returns**:  
_Describe the return value..._

---

### select(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec)

```java
select(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_
- `columnSpec`: _Describe columnSpec_

**Returns**:  
_Describe the return value..._

---

### select(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec, PikaList<T> results)

```java
select(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec, PikaList<T> results)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_
- `columnSpec`: _Describe columnSpec_
- `results`: _Describe results_

**Returns**:  
_Describe the return value..._

---

### stream(String sql)

```java
stream(String sql)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_

**Returns**:  
_Describe the return value..._

---

### stream(String sql, Class<T> resultClass)

```java
stream(String sql, Class<T> resultClass)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `resultClass`: _Describe resultClass_

**Returns**:  
_Describe the return value..._

---

### stream(String sql, Map<String, Object> args)

```java
stream(String sql, Map<String, Object> args)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_

**Returns**:  
_Describe the return value..._

---

### stream(String sql, Map<String, Object> args, Class resultClass)

```java
stream(String sql, Map<String, Object> args, Class resultClass)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_

**Returns**:  
_Describe the return value..._

---

### stream(String sql, Map<String, Object> args, Class resultClass, String... colsToMap)

```java
stream(String sql, Map<String, Object> args, Class resultClass, String... colsToMap)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_
- `colsToMap`: _Describe colsToMap_

**Returns**:  
_Describe the return value..._

---

### stream(String sql, Map<String, Object> args, Class resultClass, List<String> colsToMap)

```java
stream(String sql, Map<String, Object> args, Class resultClass, List<String> colsToMap)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_
- `colsToMap`: _Describe colsToMap_

**Returns**:  
_Describe the return value..._

---

### stream(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec)

```java
stream(String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `resultClass`: _Describe resultClass_
- `columnSpec`: _Describe columnSpec_

**Returns**:  
_Describe the return value..._

---

### handleSelectException(String sql, Map args, Exception e)

```java
handleSelectException(String sql, Map args, Exception e)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `e`: _Describe e_

**Returns**:  
_Describe the return value..._

---

### insert(Object object)

```java
insert(Object object)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `object`: _Describe object_

**Returns**:  
_Describe the return value..._

---

### insertAll(Object... items)

```java
insertAll(Object... items)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `items`: _Describe items_

**Returns**:  
_Describe the return value..._

---

### insertAll(List<Object> items)

```java
insertAll(List<Object> items)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `items`: _Describe items_

**Returns**:  
_Describe the return value..._

---

### insert(String tableName, Map<String, Object> values, String... keyCols)

```java
insert(String tableName, Map<String, Object> values, String... keyCols)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `tableName`: _Describe tableName_
- `values`: _Describe values_
- `keyCols`: _Describe keyCols_

**Returns**:  
_Describe the return value..._

---

### update(Object object)

```java
update(Object object)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `object`: _Describe object_

**Returns**:  
_Describe the return value..._

---

### update(String tableName, String keyCol, Object keyVal, String versionCol, Object versionVal, Map<String, Object> values)

```java
update(String tableName, String keyCol, Object keyVal, String versionCol, Object versionVal, Map<String, Object> values)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `tableName`: _Describe tableName_
- `keyCol`: _Describe keyCol_
- `keyVal`: _Describe keyVal_
- `versionCol`: _Describe versionCol_
- `versionVal`: _Describe versionVal_
- `values`: _Describe values_

**Returns**:  
_Describe the return value..._

---

### delete(Object object)

```java
delete(Object object)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `object`: _Describe object_

**Returns**:  
_Describe the return value..._

---

### delete(String tableName, String keyCol, Object keyVal)

```java
delete(String tableName, String keyCol, Object keyVal)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `tableName`: _Describe tableName_
- `keyCol`: _Describe keyCol_
- `keyVal`: _Describe keyVal_

**Returns**:  
_Describe the return value..._

---

### reload(Object object)

```java
reload(Object object)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `object`: _Describe object_

**Returns**:  
_Describe the return value..._

---

### exec(String sql)

```java
exec(String sql)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_

**Returns**:  
_Describe the return value..._

---

### exec(String sql, Map<String, Object> args)

```java
exec(String sql, Map<String, Object> args)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_

**Returns**:  
_Describe the return value..._

---

### logQuery(String msg, String sql, Map<String, Object> args)

```java
logQuery(String msg, String sql, Map<String, Object> args)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `msg`: _Describe msg_
- `sql`: _Describe sql_
- `args`: _Describe args_

**Returns**:  
_Describe the return value..._

---

### time(Callable<T> query)

```java
time(Callable<T> query)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `query`: _Describe query_

**Returns**:  
_Describe the return value..._

---

### getQueryLogLevel()

```java
getQueryLogLevel()
```

**Description**:  
_Describe what this method does..._

**Parameters**:    
_None_

**Returns**:  
_Describe the return value..._

---

### updateSqlVars(String sql, Map<String, Object> args, List<Object> argList)

```java
updateSqlVars(String sql, Map<String, Object> args, List<Object> argList)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `sql`: _Describe sql_
- `args`: _Describe args_
- `argList`: _Describe argList_

**Returns**:  
_Describe the return value..._

---

### withMapping(Class classToMap, Mapping mapping)

```java
withMapping(Class classToMap, Mapping mapping)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `classToMap`: _Describe classToMap_
- `mapping`: _Describe mapping_

**Returns**:  
_Describe the return value..._

---

### withMapping(Class classToMap, String tableName)

```java
withMapping(Class classToMap, String tableName)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `classToMap`: _Describe classToMap_
- `tableName`: _Describe tableName_

**Returns**:  
_Describe the return value..._

---

### safely(RunnableWithException callable)

```java
safely(RunnableWithException callable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `callable`: _Describe callable_

**Returns**:  
_Describe the return value..._

---

### safely(Callable<T> callable)

```java
safely(Callable<T> callable)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `callable`: _Describe callable_

**Returns**:  
_Describe the return value..._

---

### rethrow(Throwable e)

```java
rethrow(Throwable e)
```

**Description**:  
_Describe what this method does..._

**Parameters**:  
- `e`: _Describe e_

**Returns**:  
_Describe the return value..._

---

