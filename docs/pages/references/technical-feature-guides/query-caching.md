---
layout: default
title: "Query Caching"
description: "PikaORM thread-local query caching: startQueryCaching, endQueryCaching, clearQueryCache, and what gets cached."
active_page: query-caching
permalink: /pages/query-caching/
---

# Query Caching

PikaORM includes a thread-local, in-memory query cache. It caches **relationship queries** for the duration of a single unit of work (such as an HTTP web request), reducing N+1 query overhead.

## API Methods

The caching system is controlled via the `PikaORM` instance. Because the cache is backed by a `ThreadLocal`, these methods only affect the cache for the thread that calls them.

### `startQueryCaching()`
Initializes and enables the cache for the current thread. Any subsequent relationship loads on this thread will check the cache first.

### `endQueryCaching()`
Disables the cache and clears all stored data for the current thread. **You must call this in a `finally` block** if you use `startQueryCaching()`, otherwise you will leak memory across thread pools.

### `clearQueryCache()`
Empties the current cache but leaves caching enabled. Call this immediately after performing an `INSERT`, `UPDATE`, or `DELETE` on the same thread, ensuring that subsequent reads do not return stale cached data.

### `logCaching()` / `doNotLogCaching()`
Enables or disables logging of cache hits and misses. Useful for verifying that your cache is working and your N+1 queries are actually being eliminated.

## What is Cached?

The cache does **not** cache arbitrary `orm.query()` or `orm.select()` calls. It specifically intercepts the relationship loaders:

1. `loadMany()`
2. `loadManyThrough()`
3. `load()`
4. `loadReverse()`

Arbitrary queries are too complex to cache reliably without a distributed cache (like Redis). Relationship loads have predictable, deterministic keys (e.g., "Find all Albums where artist_id = 5"), making them ideal for request-scoped caching.

### Cache Keys

When a relationship is loaded, PikaORM constructs a deterministic key based on the identities of the objects involved.

- **`LoadKey`**: Used for `load(obj, Class)`. Keyed by the foreign key value, target class, and foreign key column.
- **`LoadReverseKey`**: Used for `loadReverse(obj, Class)`. Keyed by the parent object, target class, and foreign key column.
- **`LoadManyKey`**: Used for `loadMany(one, ManyClass)`. Keyed by the parent object identity, foreign key column, and target class.
- **`LoadManyThroughKey`**: Used for `loadManyThrough(one, JoinClass, ManyClass)`. Keyed by the parent object, join class, and target class.

Because the keys are based on exact foreign key matches, identical relationship requests yield the exact same result set, making it safe to cache for the duration of a request.

## Practical Pattern

The query cache is designed for the **web request lifecycle**. When rendering a page that loops over a collection of objects (like `Artists`) and asks for their relations (like `Albums`), caching avoids massive duplication of identical queries.

Enable the cache at the very beginning of an HTTP request and disable it at the very end, so duplicated relationship loads during that request hit the in-memory cache instead of the database.

### Using a Servlet Filter (or Middleware)

With a standard Java web framework (like Spring, Javalin, or raw Servlets), implement this pattern using a Filter or Interceptor.

```java
import javax.servlet.*;
import java.io.IOException;

public class PikaQueryCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        PikaORM orm = PikaORM.get(); // Access your global instance
        
        try {
            // 1. Enable caching for the current thread (this web request)
            orm.startQueryCaching();
            
            // 2. Proceed with the rest of the web request (controllers, rendering, etc.)
            chain.doFilter(request, response);
            
        } finally {
            // 3. ALWAYS disable caching in a finally block to prevent memory leaks 
            // across pooled web server threads
            orm.endQueryCaching();
        }
    }
}
```

### Clearing the Cache After Writes

The query cache does not automatically invalidate itself when you update a record. Because the cache only lives for the duration of a single request, this is rarely an issue. But if a controller handles a form submission (an `UPDATE` or `INSERT`) and *then* renders a page that queries the database, you might read stale data from the cache. Clear the cache after write operations:

```java
public void updateProfile(long userId, String newName) {
    User user = orm.find(User.class).byId(userId);
    user.setName(newName);
    
    if (orm.update(user)) {
        // We just mutated data. Clear the request-scoped cache so 
        // subsequent selects in this request hit the database again.
        orm.clearQueryCache();
    }
}
```
