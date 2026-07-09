---
layout: default
title: "Caching"
description: "PikaORM's request-scoped, thread-local cache for relationship loads, and how to wire it into a web request."
active_page: caching
permalink: /pages/caching/
---

# Caching

Pika has a thread-local, in-memory query cache. It caches relationship loads, the queries behind `getAlbums()` and friends, for the span of one unit of work, usually a single web request. Within that span, loading the same relationship twice hits memory instead of the database.

It does not cache arbitrary `find()...fetchList()` or raw `select()` calls. Only the four relationship loaders are cached: `loadMany`, `loadManyThrough`, `load`, and `loadReverse`. Each has a deterministic key built from the foreign key and target class, so the same request always gets the same rows back, which is what makes caching them safe.

## Turn it on for a request

The cache is per thread, so enable it at the start of a request and always disable it in a `finally`. Skip the `finally` and you leak the cache onto a pooled thread.

```java
PikaORM orm = PikaORM.get();
try {
    orm.startQueryCaching();
    // handle the request: controllers, relationship loads, rendering
} finally {
    orm.endQueryCaching();   // disables and clears the cache for this thread
}
```

In a servlet app that is a `Filter`; in another framework, the equivalent interceptor or middleware:

```java
public class PikaQueryCacheFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        PikaORM orm = PikaORM.get();
        try {
            orm.startQueryCaching();
            chain.doFilter(req, res);
        } finally {
            orm.endQueryCaching();
        }
    }
}
```

Now a page that loops over 50 artists calling `artist.getAlbums()` reuses the cached result instead of firing the same load again and again.

## Clear after a write

The cache does not invalidate itself. It only lives for one request, so this is usually a non-issue. But if a request writes data and then reads it back, clear the cache after the write so the read does not return a stale row:

```java
public void rename(long userId, String name) {
    User user = User.find().byId(userId);
    user.setName(name);
    if (user.update()) {
        orm.clearQueryCache();   // empties the cache, leaves caching on
    }
}
```

## Logging hits and misses

`logCaching()` logs every cache hit and miss; `doNotLogCaching()` turns it back off. Use it to confirm the cache is actually eliminating the duplicate loads you expect.

Caching is one way to cut repeated relationship queries. Bulk-loading is the other, see [N+1 Avoidance]({{ '/pages/n-plus-1-avoidance/' | relative_url }}).
