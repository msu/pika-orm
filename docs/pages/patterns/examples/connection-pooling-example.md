---
title: "Connection Pooling with HikariCP"
layout: default
---

# Connection Pooling with HikariCP

By default, PikaORM creates a new `Connection` via `DriverManager.getConnection()` (or the URL you provide) for each top-level query or transaction. For a production web application, this is inefficient. You should use a connection pool to maintain open connections to the database.

PikaORM makes integrating a connection pool simple. The constructor accepts a `Callable<Connection>` (often expressed as a lambda) that it calls whenever it needs a database connection.

This guide demonstrates how to configure **HikariCP**, the industry-standard connection pool for Java, with PikaORM.

## Example Configuration

First, ensure you have HikariCP included in your project dependencies.

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConfig {
    
    private static HikariDataSource dataSource;
    
    public static PikaORM setupORM() {
        // 1. Configure the connection pool
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/myapp");
        config.setUsername("dbuser");
        config.setPassword("dbpass");
        
        // Typical HikariCP optimizations
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        
        // 2. Instantiate the data source
        dataSource = new HikariDataSource(config);
        
        // 3. Pass the data source's getConnection method to PikaORM
        PikaORM orm = new PikaORM(() -> dataSource.getConnection())
                .withLogLevel(PikaLogger.Level.INFO)
                .makeDefaultORM();
                
        return orm;
    }
    
    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
```

## How It Works

1. **`dataSource.getConnection()`**: Every time PikaORM starts a new operation (or the outermost transaction block), it evaluates the lambda `() -> dataSource.getConnection()`. HikariCP intercepts this and hands PikaORM a connection from the pool.
2. **Session Lifecycle**: PikaORM manages a thread-local `ConnectionSession`. It uses the pooled connection to execute queries.
3. **Releasing Connections**: When the operation finishes (or the transaction commits/rolls back), PikaORM closes the `ConnectionSession`. This invokes `.close()` on the JDBC `Connection`. HikariCP intercepts the `close()` call and returns the connection to the pool rather than physically terminating it.

## Best Practices

- **Only create one `HikariDataSource`**: The data source should be a singleton instantiated once during application startup.
- **Graceful Shutdown**: Remember to call `dataSource.close()` when your application shuts down to cleanly terminate all database connections.
- **PikaORM Defaults**: When using a connection pool, you generally do not need to call `establishConnection()` manually except for streaming results. The pool ensures connections are rapidly available.
