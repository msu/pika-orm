---
layout: default
title: "Logging and Errors"
description: "PikaORM Logging: flexible SQL query logging, log levels, temporary suppression, and custom logger integration."
active_page: logging
permalink: /pages/logging/
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

If you are using the [Query Caching System]({{ '/pages/query-caching/' | relative_url }}), you can explicitly log cache hits and misses. This is incredibly useful for debugging N+1 queries.

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

## Connecting to Standard Logging Interfaces

To route PikaORM logs into a central logging framework (SLF4J, Log4j2, or `java.util.logging`), use the `.withLogger()` method with a lambda that switches on the PikaORM `Level` enum.

### SLF4J Integration

This is the most common pattern.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSetup {
    
    private static final Logger logger = LoggerFactory.getLogger("PikaORM");

    public static void init() {
        PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
            .logQueries() // Enable query logging
            .withLogger((level, msg, args) -> {
                switch (level) {
                    case TRACE -> logger.trace(msg, args);
                    case DEBUG -> logger.debug(msg, args);
                    case INFO  -> logger.info(msg, args);
                    case WARN  -> logger.warn(msg, args);
                    case ERROR -> logger.error(msg, args);
                }
            })
            .makeDefaultORM();
    }
}
```

### Log4j2 Integration

The pattern for Log4j2 is nearly identical to SLF4J.

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

public class DatabaseSetup {
    
    private static final Logger logger = LogManager.getLogger("PikaORM");

    public static void init() {
        PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
            .logQueries()
            .withLogger((pikaLevel, msg, args) -> {
                Level log4jLevel = switch (pikaLevel) {
                    case TRACE -> Level.TRACE;
                    case DEBUG -> Level.DEBUG;
                    case INFO  -> Level.INFO;
                    case WARN  -> Level.WARN;
                    case ERROR -> Level.ERROR;
                };
                logger.log(log4jLevel, msg, args);
            })
            .makeDefaultORM();
    }
}
```

### Java Util Logging (JUL)

JUL requires mapping PikaORM's levels to JUL's standard levels (`FINER`, `FINE`, `INFO`, `WARNING`, `SEVERE`).

```java
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseSetup {
    
    private static final Logger logger = Logger.getLogger("PikaORM");

    public static void init() {
        PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
            .logQueries()
            .withLogger((pikaLevel, msg, args) -> {
                Level julLevel = switch (pikaLevel) {
                    case TRACE -> Level.FINER;
                    case DEBUG -> Level.FINE;
                    case INFO  -> Level.INFO;
                    case WARN  -> Level.WARNING;
                    case ERROR -> Level.SEVERE;
                };
                
                // Format the message with arguments if present
                if (args != null && args.length > 0) {
                    // Quick string formatting for JUL
                    msg = String.format(msg.replace("?", "%s"), args);
                }
                
                logger.log(julLevel, msg);
            })
            .makeDefaultORM();
    }
}
```
