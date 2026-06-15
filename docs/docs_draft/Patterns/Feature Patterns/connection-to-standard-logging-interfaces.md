---
title: "Connecting to Standard Logging Interfaces"
layout: default
---

# Connecting to Standard Logging Interfaces

PikaORM logs its queries and internal operations through a simple `PikaLogger` interface. By default, when you call `.logQueries()`, it outputs to `System.out`. 

In an enterprise environment, you will likely want to route PikaORM's logs into your central logging framework (like SLF4J, Log4j2, or `java.util.logging`). You can do this easily using `.withLogger()`.

## SLF4J Integration

This is the most common pattern. You create an SLF4J logger and provide a lambda to `withLogger` that switches on the PikaORM `Level` enum.

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

## Log4j2 Integration

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

## Java Util Logging (JUL)

If you are using the built-in `java.util.logging` framework, mapping the levels requires mapping PikaORM's levels to JUL's standard levels (e.g., `FINE`, `INFO`, `WARNING`, `SEVERE`).

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

## Temporary Log Suppression

Sometimes you want logging enabled globally, but need to disable it for a specific noisy operation (like a massive bulk insert or background health check).

You can use the `suppressQueries` builder pattern:

```java
// Queries executed inside this block will NOT be logged
orm.suppressQueries(() -> {
    orm.insertAll(hugeListOfItems);
});
```

This applies only to the current thread for the duration of the lambda execution, leaving global logging safely intact.
