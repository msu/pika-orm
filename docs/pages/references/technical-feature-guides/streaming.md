---
layout: default
title: "Streaming API"
description: "PikaORM Streaming: memory-efficient lazy database queries using PikaStreamFinder and the Java Stream API."
active_page: streaming
permalink: /pages/streaming/
---

# Pika Streams

## Overview

Pika Streams provide a powerful abstraction layer that combines the familiar Java Stream API with database query execution. 

Unlike standard Java Streams that operate on in-memory collections, Pika Streams execute queries lazily against your database. They stream results row-by-row directly from the JDBC `ResultSet`, allowing you to process massive datasets efficiently without loading the entire result set into JVM memory.

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

### Connection Management (Crucial)

Unlike standard `orm.find()` operations which automatically checkout and release a connection for the duration of the query, Streams are **lazy**. The database connection must remain open while you process the stream. 

**You must always use streams within an explicit connection context** (e.g., a try-with-resources block calling `establishConnection()`) to ensure the underlying connection and `ResultSet` are properly closed when the stream completes.

```java
try (var conn = orm.establishConnection()) {
    long activeUsersCount = orm.stream(SampleModel.class)
        .where("is_active = :val", Map.of("val", true))
        .stream()
        .count(); // Terminal operation executes the query
}
```

## The PikaStreamFinder Class

The `PikaStreamFinder<T>` serves as your primary interface for creating database-backed streams.

### Stream Filtering

`PikaStreamFinder` supports pushing WHERE clauses directly into the database before the stream begins:

```java
try (var conn = orm.establishConnection()) {
    // Push filtering to the database (Efficient)
    Stream<SampleModel> activeModels = orm.stream(SampleModel.class)
        .where("is_active = :val", Map.of("val", true))
        .stream();
        
    // Standard Java stream filtering (Less Efficient)
    Stream<SampleModel> activeModelsMemory = orm.stream(SampleModel.class)
        .all()
        .filter(model -> model.isActive()); 
}
```

Whenever possible, use `.where()` to let the database do the filtering rather than pulling all rows over the network and filtering via `.filter()` in memory.

### PikaStreamFinder vs PikaClassQuery.stream() vs orm.stream(sql)

There are three ways to get a Stream in PikaORM:

1. **`orm.stream(Class)`**: Returns a `PikaStreamFinder`. The simplest way to start a stream with basic filtering.
2. **`orm.query(Class).stream()`**: Returns a standard Java `Stream`. Use this when you need complex querying (JOINs, GROUP BY, OrderBy) before streaming the results.
3. **`orm.stream(String sql)`**: Raw SQL streaming. Returns a `Stream<ResultMap>`. Use this for massive custom reporting queries.

## Practical Example: Data Export

Streams shine when exporting data (e.g., to a CSV file) where the dataset exceeds available RAM.

```java
public void exportUsersToCsv(String filePath) throws IOException {
    try (var conn = orm.establishConnection();
         FileWriter writer = new FileWriter(filePath)) {
         
        writer.write("ID,Name,Email\n");
        
        orm.stream(User.class)
            .all() // Starts the stream
            .forEach(user -> {
                try {
                    writer.write(user.getId() + "," + user.getName() + "," + user.getEmail() + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }
}
```

## Performance Considerations

- **Lazy Evaluation**: Streams are not executed until a terminal operation (like `findFirst()`, `forEach()`, or `count()`) is called.
- **Connection Context**: Always use streams within a connection context (`try (var conn = orm.establishConnection())`).
- **Database Optimization**:`.where()` before calling `.stream()` to push filtering down to the database level.
- **Limit/Pagination**: If you only need a subset of data, do not stream. Use `orm.query(Class).page().fetchList()` instead. Streams are for processing large continuous sets.

## Error Handling

When using streams, exceptions that occur during JDBC iteration are wrapped in `RuntimeException` (since Java's Stream API does not permit checked exceptions in its functional interfaces). 

```java
try (var conn = orm.establishConnection()) {
    SampleModel retrieved = orm.stream(SampleModel.class)
        .all()
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No records found"));
}
```