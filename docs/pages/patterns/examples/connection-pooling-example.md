---
layout: default
title: "Connection Pooling"
description: "Back PikaORM with a HikariCP connection pool by passing a connection supplier to the constructor."
active_page: connection-pooling
permalink: /pages/connection-pooling/
---

# Connection Pooling

Given a URL, PikaORM opens a fresh JDBC connection for each operation. For a server you want a pool instead. The `PikaORM` constructor takes a `Callable<Connection>`, so hand it your pool's `getConnection`.

Add the HikariCP dependency, then:

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:postgresql://localhost:5432/myapp");
config.setUsername("dbuser");
config.setPassword("dbpass");
config.setMaximumPoolSize(10);

HikariDataSource pool = new HikariDataSource(config);

PikaORM orm = new PikaORM(pool::getConnection)   // hand Pika a pooled connection per operation
        .makeDefaultORM();
```

Pika calls the supplier whenever it needs a connection (each top-level query or transaction) and calls `close()` when the operation finishes. Hikari hands out a pooled connection, and on `close()` it takes the connection back rather than tearing it down.

Two things to remember:

- Create the `HikariDataSource` once, at startup, and share it. It is the pool.
- Close it on shutdown (`pool.close()`) to release the connections.

For tests and small apps where a pool is overkill, the URL constructor (`new PikaORM("jdbc:...")`) is fine.
