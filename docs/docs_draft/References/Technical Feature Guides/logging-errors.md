---
title: "Logging and Errors"
layout: default
---

# Logging and Errors

PikaORM includes a flexible logging system that captures SQL statements, their bound parameters, and internal ORM operations. This makes it easy to monitor what is happening at the database level.

## The `PikaLogger` Interface

At its core, PikaORM logs events through a simple functional interface:

```java
public interface PikaLogger {
    enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE
    }
    
    void log(Level level, String msg, Object... args);
}
```

## Configuration Methods

### Enabling Default Logging

By default, PikaORM is silent. To turn on SQL logging to `System.out`, use the `.logQueries()` method when building the ORM instance.

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .logQueries() // Enables default System.out logger at INFO level
    .makeDefaultORM();
```

Output format example:
```text
[INFO] SELECT SQL: SELECT * FROM sample_models WHERE int_val > ? AND bool_val = ?
  Args: [20, true]
```

### Setting Log Levels

You can control the verbosity of the ORM using `.withLogLevel()`.

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .logQueries()
    .withLogLevel(PikaLogger.Level.TRACE)
    .makeDefaultORM();
```

- **ERROR / WARN**: Exceptions and failed operations.
- **INFO**: Standard executed SQL queries.
- **DEBUG**: Transaction boundary markers (Start/Commit/Rollback).
- **TRACE**: Detailed mappings, connection pooling details, and coercion fallbacks.

### Query Cache Logging

If you are using the [Query Caching System]({{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/query-caching.md), you can explicitly log cache hits and misses. This is incredibly useful for debugging N+1 queries.

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .logCaching() // Log cache operations
    .makeDefaultORM();
```

You can disable it dynamically later with `orm.doNotLogCaching()`.

## Temporary Log Suppression

Sometimes you want logging enabled globally, but need to disable it for a specific noisy operation (like a massive bulk insert during a data migration).

You can use the `suppressQueries` method, which accepts a `Runnable` or `Callable`.

```java
// Queries executed inside this block will NOT be logged
orm.suppressQueries(() -> {
    for (Data data : massiveDataset) {
        orm.insert(data);
    }
});

// Logging is automatically re-enabled here
```

This suppression is Thread-Local. It applies only to the current thread for the duration of the lambda execution, leaving global logging safely intact for other concurrent web requests.

## Custom Loggers (SLF4J, Log4j2)

To route PikaORM logs to a standard enterprise logging framework, use the `.withLogger()` method. 

See the [Connection to Standard Logging Interfaces]({{ site.baseurl }}/docs_draft/Patterns/Feature%20Patterns/connection-to-standard-logging-interfaces.md) pattern guide for complete code examples on wiring PikaORM to SLF4J, Log4j2, and `java.util.logging`.
