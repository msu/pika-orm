---
layout: default
title: "Logging & Errors"
description: "Turn on SQL logging in PikaORM, set the level, route logs to SLF4J or Log4j2, and silence noisy blocks."
active_page: logging
permalink: /pages/logging/
---

# Logging & Errors

Pika is silent by default. Turn on SQL logging with `logQueries()`, set how much you want with `withLogLevel()`, and route it wherever you like with `withLogger()`.

## Turn on logging

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
        .logQueries()                          // log SQL to System.out
        .withLogLevel(PikaLogger.Level.TRACE)  // how much detail
        .makeDefaultORM();
```

`logQueries()` prints each statement and its bound parameters:

```text
[INFO] SELECT SQL: SELECT * FROM todos WHERE completed = ?
  Args: [false]
```

Levels, from least to most detail:

- `ERROR` / `WARN` -- failed operations and exceptions.
- `INFO` -- the SQL Pika runs.
- `DEBUG` -- transaction begin, commit, and rollback.
- `TRACE` -- field mappings, connection details, coercion fallbacks.

## Route logs to your framework

`withLogger()` takes a lambda that Pika calls for every log event. Switch on Pika's `Level` and forward to SLF4J, Log4j2, `java.util.logging`, or anything else.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

Logger log = LoggerFactory.getLogger("PikaORM");

PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
        .logQueries()
        .withLogger((level, msg, args) -> {
            switch (level) {
                case TRACE -> log.trace(msg, args);
                case DEBUG -> log.debug(msg, args);
                case INFO  -> log.info(msg, args);
                case WARN  -> log.warn(msg, args);
                case ERROR -> log.error(msg, args);
            }
        })
        .makeDefaultORM();
```

Log4j2 and `java.util.logging` work the same way: map Pika's `Level` to the framework's level inside the lambda. JUL has no `{}` placeholders, so format the message with its `args` yourself before passing it on.

The logger is a one-method interface:

```java
public interface PikaLogger {
    enum Level { ERROR, WARN, INFO, DEBUG, TRACE }
    void log(Level level, String msg, Object... args);
}
```

## Cache logging

If you use [Caching]({{ '/pages/caching/' | relative_url }}), `logCaching()` logs every cache hit and miss, which is handy for confirming your N+1 queries are actually gone. Turn it off again with `doNotLogCaching()`.

```java
new PikaORM("jdbc:sqlite:app.db").logCaching().makeDefaultORM();
```

## Silence a noisy block

To keep logging on everywhere but quiet one operation, like a bulk import, wrap it with `suppressQueries()`. It returns a closeable, so use try-with-resources. Suppression is per thread and ends when the block does.

```java
try (var s = orm.suppressQueries()) {
    for (Data row : hugeDataset) {
        orm.insert(row);   // not logged
    }
}
```
