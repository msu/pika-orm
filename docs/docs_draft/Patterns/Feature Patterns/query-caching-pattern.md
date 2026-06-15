---
title: "Query Caching Pattern"
layout: default
---

# Query Caching Pattern

PikaORM includes a thread-local, in-memory query cache specifically designed for the **Web Request Lifecycle**. 

When building web applications, it is common to render a page that loops over a collection of objects (like `Artists`) and asks for their relations (like `Albums`). Without caching, this can lead to massive duplication of identical queries.

## The Web Request Lifecycle Pattern

The optimal way to use PikaORM's query cache is to enable it at the very beginning of an HTTP request and disable it at the very end. This ensures that any duplicated relationship loads during the rendering of that specific request hit the in-memory cache instead of the database.

### Using a Servlet Filter (or Middleware)

If you are using a standard Java web framework (like Spring, Javalin, or raw Servlets), you can implement this pattern using a Filter or Interceptor.

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

The query cache is **dumb**; it does not automatically invalidate itself if you update a record. Because the cache only lives for the duration of a single web request, this is rarely an issue. 

However, if your controller handles a form submission (an `UPDATE` or `INSERT`) and *then* renders a page that queries the database, you might read stale data from the cache.

To handle this, clear the cache after write operations:

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

## What Gets Cached?

It's important to understand that `startQueryCaching()` does **not** cache arbitrary `SELECT` queries. 

It specifically caches **Relationship Loads**:
- `orm.loadMany()`
- `orm.loadManyThrough()`
- `orm.load()`
- `orm.loadReverse()`

This is intentional. Arbitrary queries are too complex to cache reliably without a distributed cache (like Redis). Relationship loads have predictable, deterministic keys (e.g., "Find all Albums where artist_id = 5"), making them perfect for request-scoped caching.
