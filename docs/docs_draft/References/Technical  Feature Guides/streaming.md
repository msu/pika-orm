# Pika Streams

## Overview

Pika Streams provide a powerful abstraction layer that combines the familiar Java Stream API with database query execution. Unlike standard Java Streams that operate on in-memory collections, Pika Streams execute queries lazily against your database, allowing you to process large datasets efficiently without loading entire result sets into memory.

## Key Differences from Standard Java Streams

### Standard Java Streams

- Operate on in-memory collections
- Process data that's already loaded into the JVM heap
- Limited by available memory for large datasets
- Immediate data availability

### Pika Streams

- Execute database queries on-demand
- Stream results directly from the database
- Memory-efficient processing of large result sets
- Lazy evaluation with database-level optimization
- Automatic object-relational mapping of results

## Getting Started with Pika Streams

Pika Streams are accessed through the main ORM instance using the `stream()` method:

```java
// Get a stream finder for your entity class
PikaStreamFinder<SampleModel> streamFinder = orm.stream(SampleModel.class);

// Use within a connection context for proper resource management
try (var conn = orm.establishConnection()) {
    Stream<SampleModel> results = streamFinder.all();
    // Process your stream...
}
```

### Connection Management

Pika Streams work within the ORM's connection context. Always use them within a try-with-resources block or established connection to ensure proper cleanup:

```java
try (var conn = orm.establishConnection()) {
    List<SampleModel> models = orm.stream(SampleModel.class)
        .where("int_val=:val", Map.of("val", 10))
        .toList();
}
```

## The PikaStreamFinder Class

The `PikaStreamFinder<T>` serves as your primary interface for creating database-backed streams. It provides several factory methods to generate streams based on common query patterns.

## Query Methods

### Single Record Queries

#### `byId(Object id)`

Retrieves a single entity by its primary key identifier.

```java
try (var conn = orm.establishConnection()) {
    Stream<SampleModel> modelStream = orm.stream(SampleModel.class).byId(123);
    SampleModel model = modelStream.findFirst().orElse(null);
}
// Executes: SELECT * FROM sample_model WHERE id = 123 LIMIT 1 OFFSET 0
```

#### `byKey(String column, Object value)`

Finds a single entity by any column value.

```java
try (var conn = orm.establishConnection()) {
    Stream<SampleModel> modelStream = orm.stream(SampleModel.class)
        .byKey("str_val", "specificValue");
    SampleModel model = modelStream.findFirst().get();
}
// Executes: SELECT * FROM sample_model WHERE str_val=:arg LIMIT 1 OFFSET 0
```

### Collection Queries

#### `all()`

Retrieves all records from the entity's table.

```java
try (var conn = orm.establishConnection()) {
    List<SampleModel> allModels = orm.stream(SampleModel.class)
        .all()
        .toList();
}
// Executes: SELECT * FROM sample_model
```

#### `allBy(String column, Object value)`

Finds all records matching a specific column value.

```java
try (var conn = orm.establishConnection()) {
    List<SampleModel> matchingModels = orm.stream(SampleModel.class)
        .allBy("int_val", 10)
        .toList();
}
// Executes: SELECT * FROM sample_model WHERE int_val=:val
```

### Dynamic Queries

#### `where(String whereClause, Map<String, Object> args)`

Allows custom `WHERE` clauses just like the normal `query` method building.

```java
try (var conn = orm.establishConnection()) {
    // Find models with specific integer value
    List<SampleModel> results = orm.stream(SampleModel.class)
        .where("int_val=:val", Map.of("val", 10))
        .toList();
    
    // Date range queries
    List<SampleModel> futureModels = orm.stream(SampleModel.class)
        .where("date_val > :val", Map.of("val", new Date(2050, 1, 1)))
        .toList();
    
    // LIKE queries for pattern matching
    List<SampleModel> matchingPattern = orm.stream(SampleModel.class)
        .where("str_val like :val", Map.of("val", "%b%"))
        .toList();
}
// Executes: SELECT * FROM sample_model WHERE [your_where_clause]
```

#### `bySQL(String sql, Map<String, Object> args)`

Execute completely custom SQL queries while maintaining stream benefits.

```java
try (var conn = orm.establishConnection()) {
    String customQuery = """
            SELECT s.* FROM sample_model s 
            WHERE s.bool_val = true AND s.int_val = :minVal
            ORDER BY s.date_val DESC
            """;
        
    List<SampleModel> customResults = orm.stream(SampleModel.class)
        .bySQL(customQuery, Map.of("minVal", 5))
        .toList();
}
```

#### `byQuery()`

Returns a `PikaClassQuery<T>` for building complex queries programmatically.

```java
try (var conn = orm.establishConnection()) {
    List<SampleModel> complexQuery = orm.stream(SampleModel.class)
        .where("int_val=:var", Map.of("var", 10))
        .orderBy("date_val")
        .toList();
}
```

## Practical Examples from Real Usage

### Basic Entity Retrieval

```java
@Test
void retrieveAndValidateEntity() {
    var orm = initTestDb(SampleModel.DDL);
    
    // Insert test data
    SampleModel original = new SampleModel("test_value", 42, true, new Date());
    long id = orm.insert(original);
    
    // Retrieve using stream
    try (var conn = orm.establishConnection()) {
        SampleModel retrieved = orm.stream(SampleModel.class)
            .all()
            .findFirst() //grabs the first row it finds ad returns just the one
            .get()
            .orElseThrow();
            
        // Validate the retrieved entity matches original
        assertEquals(retrieved.getStrVal(), original.getStrVal());
        assertEquals(retrieved.getIntVal(), original.getIntVal());
        assertEquals(retrieved.getBoolVal(), original.getBoolVal());
        
        // Handle database precision for dates (e.g., MariaDB rounds to seconds)
        assertEquals(
            retrieved.getDateVal().toInstant().truncatedTo(ChronoUnit.SECONDS),
            original.getDateVal().toInstant().truncatedTo(ChronoUnit.SECONDS)
        );
    }
}
```

### Bulk Data Processing

```java
@Test
void processBulkData() {
    
    try (var conn = orm.establishConnection()) {
        // Query with filtering
        List<SampleModel> evenRecords = orm.stream(SampleModel.class)
            .where("int_val % 2 = :remainder", Map.of("remainder", 0))
            .toList();
            
        assertEquals(5, evenRecords.size());
        
        // Pattern matching
        List<SampleModel> recordsWithPattern = orm.stream(SampleModel.class)
            .where("str_val like :pattern", Map.of("pattern", "record_%"))
            .toList();
            
        assertEquals(10, recordsWithPattern.size());
        
        // Get all records
        List<SampleModel> allRecords = orm.stream(SampleModel.class)
            .all()
            .toList();
            
        assertEquals(10, allRecords.size());
    }
}
```

## Best Practices

### Resource Cleanup

Pika Streams automatically manage database connections and result sets when used within the ORM's connection context:

```java
// Proper resource management - connection auto-closed
try (var conn = orm.establishConnection()) {
    List<SampleModel> results = orm.stream(SampleModel.class)
        .where("int_val = :threshold", Map.of("threshold", 100))
        .limit(50)
        .toList();
} // Connection automatically closed here
```

### Query Optimization

```java
// Prefer specific database-level filtering
try (var conn = orm.establishConnection()) {
    // Better: Database does the filtering
    List<SampleModel> highValueModels = orm.stream(SampleModel.class)
        .where("int_val = :min", Map.of("min", 100))
        .toList();
    
    // Less efficient: Loads all data then filters in memory
    List<SampleModel> highValueModels2 = orm.stream(SampleModel.class)
        .all()
        .filter(model -> model.getIntVal() > 100)
        .toList();
}
```

### Working with Dates and Database Precision

```java
try (var conn = orm.establishConnection()) {
    Date searchDate = new Date();
    
    List<SampleModel> recentModels = orm.stream(SampleModel.class)
        .where("date_val >= :since", Map.of("since", searchDate))
        .toList();
    
    // When comparing dates retrieved from database, account for precision differences
    recentModels.forEach(model -> {
        Date dbDate = model.getDateVal();
        // MariaDB/MySQL may truncate to seconds, so compare accordingly
        boolean isSameSecond = dbDate.toInstant().truncatedTo(ChronoUnit.SECONDS)
            .equals(searchDate.toInstant().truncatedTo(ChronoUnit.SECONDS));
    });
}
```

## Performance Considerations

- **Lazy Evaluation**: Streams are not executed until a terminal operation (like `toList()`, `findFirst()` `get()`, `forEach()`) is called
- **Connection Context**: Always use streams within a connection context (`try (var conn = orm.establishConnection())`)
- **Database Optimization**: Leverage database indexes for columns used in `WHERE` clauses
- **Result Set Size**: Consider using `limit()` for large datasets to prevent memory issues
- **Query Specificity**: Use specific `WHERE` clauses instead of loading all data and filtering in memory
- **Date Precision**: Be aware of database-specific date/time precision (e.g., MariaDB rounds DATETIME to nearest second)

## Error Handling

Pika Streams will throw appropriate exceptions for:

- Invalid SQL syntax in `WHERE` clauses
- Database connectivity issues
- Mapping errors between database columns and entity fields
- Parameter binding failures

## Testing Patterns

When testing with Pika Streams, follow these patterns for reliable tests:

```java
@Test
void testStreamQuery() {
    PikaORM orm = new 	PikaORM("jdbc:sqlite:test/web.db") // DB connection string
            .withLogLevel(TRACE)
            .makeDefaultORM()
            .withMigrations(new SampleModel())
            .applyMigrations();
    
    // Insert known test data
    SampleModel testModel = new SampleModel("test", 100, true, new Date());
    long id = orm.insert(testModel);
    
    // Test the stream query
    try (var conn = orm.establishConnection()) {
        List<SampleModel> results = orm.stream(SampleModel.class)
            .where("int_val = :val", Map.of("val", 100))
            .toList();
        
        assertEquals(1, results.size());
        assertEquals("test", results.get(0).getStrVal());
    }
}
```