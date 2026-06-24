---
layout: default
title: "Streaming"
description: "Process result sets too large for memory by streaming rows from the database with PikaORM."
active_page: streaming
permalink: /pages/streaming/
---

# Streaming

`find()` loads every matching row into a list. When the result set is too big to hold in memory, stream it instead. Pika pulls rows one at a time from the JDBC `ResultSet` into a Java `Stream`, so memory stays flat no matter how many rows there are.

## Hold the connection open

A stream is lazy: the query runs as you consume it, so the database connection has to stay open the whole time. Wrap streaming work in `establishConnection()` with try-with-resources. When the block exits, Pika closes the connection and the `ResultSet`.

```java
try (var conn = orm.establishConnection()) {
    long active = orm.stream(User.class)
            .where("active = :a", Map.of("a", true))
            .count();   // terminal operation runs the query
}
```

`orm.stream(User.class)` returns a stream finder. `.all()` streams every row; `.where(clause, args)` filters in the database first. Both hand you a Java `Stream`, so the usual terminal operations (`forEach`, `count`, `findFirst`, `collect`) drive the query.

Filter in the database, not in memory:

```java
// Good: the database filters
orm.stream(User.class).where("active = :a", Map.of("a", true));

// Wasteful: pulls every row over the wire, then filters in the JVM
orm.stream(User.class).all().filter(User::isActive);
```

## Three ways to get a stream

- `orm.stream(User.class)` for simple filtered streams (shown above).
- `orm.query(User.class)...stream()` to build a full query (joins, ordering) and stream the result. See [Querying]({{ '/pages/querying/' | relative_url }}).
- `orm.stream(sql)` for raw SQL, streamed as `ResultMap` rows. See [Plain Java Objects]({{ '/pages/plain-objects/' | relative_url }}).

## Example: export to CSV

A dataset bigger than RAM is exactly what streaming is for.

```java
public void exportUsers(String path) throws IOException {
    try (var conn = orm.establishConnection();
         FileWriter out = new FileWriter(path)) {

        out.write("id,name,email\n");

        orm.stream(User.class).all().forEach(u -> {
            try {
                out.write(u.getId() + "," + u.getName() + "," + u.getEmail() + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

## When not to stream

If you only need a page of rows, do not stream. Use [Paging]({{ '/pages/paging/' | relative_url }}) (`.page(n).fetchList()`). Streaming is for chewing through large, continuous sets end to end.

## Errors

Java's Stream API does not allow checked exceptions in its lambdas, so anything thrown while iterating the `ResultSet` comes back wrapped in a `RuntimeException`.
