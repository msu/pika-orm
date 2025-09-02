# Pika ORM Logger: SQL Query Logging and Debugging

## Overview

Pika ORM includes a flexible logging system that helps you monitor and debug SQL queries executed by the ORM. The logger captures INSERT, UPDATE, DELETE, and SELECT statements along with their parameters, making it easy to understand what's happening at the database level.

## The `PikaLogger` Interface

```java
interface PikaLogger {
    enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE
    }
    
    void log(Level level, String msg, 			                              Object...args);
}
```

The logger interface is simple but powerful, allowing you to integrate with any logging framework or create custom logging behavior.

## Default Logging Behavior

By default, Pika ORM logs SQL queries to `System.out` when logging is enabled. The format includes:

- **SQL Statement**: The actual SQL being executed
- **Parameters**: The bound parameter values

### Default Logger Example

```java
@Test
void demonstrateDefaultLogging() {
    // init the ORM
    PikaORM orm = new PikaORM("jdbc:sqlite:test/web.db") // DB connection string
            .withLogLevel(TRACE)
            .makeDefaultORM()
            .withMigrations(new SampleModel())
            .applyMigrations();
    
    // Enable default logging (logs to System.out)
    orm.logQueries();
    
    SampleModel model = new SampleModel("example", 42, true, new Date());
    long id = orm.insert(model);
    
    // Output will show:
    // INSERT SQL: INSERT INTO sample_models (bool_val, date_val, int_val, str_val) VALUES (?, ?, ?, ?)
    //   Args:[true, 2021-02-01, 42, example]
}
```

## Custom Logger Integration

### SLF4J Integration

The most common use case is integrating with SLF4J for enterprise logging:

```java
@Test
void setupSLF4JLogging() {
    // init the ORM
    PikaORM orm = new PikaORM("jdbc:sqlite:test/web.db") // DB connection string
            .withLogLevel(INFO)
            .makeDefaultORM()
            .withMigrations(new SampleModel())
            .applyMigrations();
    
    Logger logger = LoggerFactory.getLogger(LoggingTest.class); //Pika's Logger class
    
    // Enable query logging at INFO level
    orm.logQueries();
    
    // Set up SLF4J logger adapter
    orm.withLogger((level, msg, args) -> {
        switch (level) {
            case TRACE -> logger.trace(msg, args);
            case DEBUG -> logger.debug(msg, args);
            case INFO -> logger.info(msg, args);
            case WARN -> logger.warn(msg, args);
            case ERROR -> logger.error(msg, args);
        }
    });
    
    // Now all SQL queries will be logged through SLF4J
    SampleModel model = new SampleModel("test", 100, false, new Date());
    orm.insert(model);
}
```

### Custom Logger Implementation

You can create completely custom logging behavior:

```java
// Custom logger that formats queries differently
orm.withLogger((level, msg, args) -> {
    String timestamp = Instant.now().toString();
    System.out.printf("[%s] %s - %s%n", timestamp, level, msg);
    
    if (args != null && args.length > 0) {
        System.out.printf("    Parameters: %s%n", Arrays.toString(args));
    }
});

// File-based logging
orm.withLogger((level, msg, args) -> {
    try (FileWriter writer = new FileWriter("sql-queries.log", true)) {
        writer.write(String.format("%s [%s]: %s%n", 
            LocalDateTime.now(), level, msg));
        if (args != null && args.length > 0) {
            writer.write(String.format("  Args: %s%n", Arrays.toString(args)));
        }
    } catch (IOException e) {
        System.err.println("Failed to write to log file: " + e.getMessage());
    }
});
```

## Logger Output Format

The logger outputs SQL queries in a consistent format:

```
INSERT SQL: INSERT INTO sample_models (bool_val, date_val, int_val, str_val) VALUES (?, ?, ?, ?)
  Args:[true, 3921-02-01, 10, foo]
```

## Configuration Methods

### `logQueries()`

Enables query logging at the INFO level:

```java
var orm = initTestDb(SampleModel.DDL);
orm.logQueries(); // Enable logging with default System.out logger
```

### `withLogger(PikaLogger logger)`

Sets a custom logger implementation:

```java
orm.withLogger((level, msg, args) -> {
    // Your custom logging logic here
    customLoggingFramework.log(level.toString(), msg, args);
});
```

### Combining Methods

```java
var orm = initTestDb(SampleModel.DDL);
orm.logQueries()           // Enable logging
   .withLogger(myLogger);  // Use custom logger implementation
```

## Practical Usage Scenarios

### Development Debugging

```java
@Test
void debugComplexQuery() {
    // init the ORM
    PikaORM orm = new PikaORM("jdbc:sqlite:test/web.db") // DB connection string
            .withLogLevel(INFO)
            .makeDefaultORM()
            .withMigrations(new SampleModel())
            .applyMigrations();
    
    orm.logQueries(); // Enable for debugging
    
    // Insert test data
    new SampleModel("foo", 10, true, new Date(2021, 1, 1));
    orm.insert(SampleModel);
    }
    
    // Complex query - logging will show the actual SQL
    try (var conn = orm.establishConnection()) {
        List<SampleModel> results = orm.stream(SampleModel.class)
            .where("int_val = :threshold AND bool_val = :flag", 
                   Map.of("threshold", 20, "flag", true))
            .toList();
    }
    
    // Output shows:
    // SELECT SQL: SELECT * FROM sample_models WHERE int_val > ? AND bool_val = ?
    //   Args:[20, true]
}
```

### Production Monitoring

```java
// Production-ready logging setup
public void setupProductionLogging(PikaORM orm) {
    Logger sqlLogger = LoggerFactory.getLogger("someloggerclasshere");
    
    orm.logQueries(); // Enable query logging
    
    orm.withLogger((level, msg, args) -> {
        // Only log slow queries or errors in production
        if (level == PikaLogger.Level.ERROR || 
            level == PikaLogger.Level.WARN) {
            sqlLogger.warn("SQL Issue - {}", msg, args);
        } else if (msg.contains("SELECT") && args.length > 5) {
            // Log complex SELECT queries for monitoring
            sqlLogger.info("Complex Query - {}", msg, args);
        }
    });
}
```

### Testing and Validation

```java
@Test
void validateGeneratedSQL() {
    var orm = initTestDb(SampleModel.DDL);
    
    // Capture logging output for testing
    List<String> loggedMessages = new ArrayList<>();
    orm.logQueries();
    orm.withLogger((level, msg, args) -> {
        loggedMessages.add(msg + " | " + Arrays.toString(args));
    });
    
    // Perform operation
    SampleModel model = new SampleModel("test", 123, true, new Date());
    orm.insert(model);
    
    // Validate the SQL was correct
    assertTrue(loggedMessages.get(0).contains("INSERT INTO sample_models"));
    assertTrue(loggedMessages.get(0).contains("[true, "));
    assertTrue(loggedMessages.get(0).contains("123"));
    assertTrue(loggedMessages.get(0).contains("test]"));
}
```

## Integration with Popular Logging Frameworks

### Log4j2 Integration

```java
org.apache.logging.log4j.Logger log4jLogger = 
    LogManager.getLogger("PikaSQL");

orm.withLogger((level, msg, args) -> {
    Level log4jLevel = switch(level) {
        case ERROR -> Level.ERROR;
        case WARN -> Level.WARN;
        case INFO -> Level.INFO;
        case DEBUG -> Level.DEBUG;
        case TRACE -> Level.TRACE;
    };
    log4jLogger.log(log4jLevel, msg, args);
});
```

## Best Practices

### Development Environment

```java
// Full logging for development
orm.logQueries()
   .withLogger((level, msg, args) -> {
       System.out.printf("[%s] %s%n", level, msg);
       if (args.length > 0) {
           System.out.printf("  Parameters: %s%n", Arrays.toString(args));
       }
   });
```

### Production Environment Example

```java
// Selective logging for production
Logger prodLogger = LoggerFactory.getLogger("pika.production"); //could be anything

orm.withLogger((level, msg, args) -> {
    // Only log warnings and errors in production
    if (level == PikaLogger.Level.ERROR || level == PikaLogger.Level.WARN) {
        prodLogger.error("Database issue: {} - Args: {}", msg, Arrays.toString(args));
    }
    
    // Log long-running queries for performance monitoring
    if (msg.contains("SELECT") && args.length > 10) {
        prodLogger.info("Complex query detected: {}", msg;
    }
});
```

