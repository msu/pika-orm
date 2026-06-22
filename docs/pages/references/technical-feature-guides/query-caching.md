---
layout: default
title: "Query Caching API"
description: "PikaORM thread-local query caching: startQueryCaching, endQueryCaching, clearQueryCache, and what gets cached."
active_page: query-caching
permalink: /pages/query-caching/
---

# Query Caching API

PikaORM includes a thread-local, in-memory query cache. It is designed to cache **relationship queries** for the duration of a single unit of work (such as an HTTP web request), drastically reducing N+1 query overhead.

## API Methods

The caching system is controlled via the `PikaORM` instance. Because the cache is backed by a `ThreadLocal`, these methods only affect the cache for the thread that calls them.

### `startQueryCaching()`
Initializes and enables the cache for the current thread. Any subsequent relationship loads on this thread will check the cache first.

### `endQueryCaching()`
Disables the cache and clears all stored data for the current thread. **You must call this in a `finally` block** if you use `startQueryCaching()`, otherwise you will leak memory across thread pools.

### `clearQueryCache()`
Empties the current cache but leaves caching enabled. This is critical to call immediately after performing an `INSERT`, `UPDATE`, or `DELETE` on the same thread, ensuring that subsequent reads do not return stale cached data.

### `logCaching()` / `doNotLogCaching()`
Enables or disables logging of cache hits and misses. This is extremely useful for verifying that your cache is working and your N+1 queries are actually being eliminated.

## What is Cached?

The cache does **not** cache arbitrary `orm.query()` or `orm.select()` calls. It specifically intercepts the relationship loaders:

1. `loadMany()`
2. `loadManyThrough()`
3. `load()`
4. `loadReverse()`

### Cache Keys

When a relationship is loaded, PikaORM constructs a deterministic key based on the identities of the objects involved.

- **`LoadKey`**: Used for `load(obj, Class)`. Keyed by the foreign key value, target class, and foreign key column.
- **`LoadReverseKey`**: Used for `loadReverse(obj, Class)`. Keyed by the parent object, target class, and foreign key column.
- **`LoadManyKey`**: Used for `loadMany(one, ManyClass)`. Keyed by the parent object identity, foreign key column, and target class.
- **`LoadManyThroughKey`**: Used for `loadManyThrough(one, JoinClass, ManyClass)`. Keyed by the parent object, join class, and target class.

Because the keys are based on exact foreign key matches, PikaORM can guarantee that identical relationship requests yield the exact same result set, making it safe to cache for the duration of a request.

## Example Usage

See the [Query Caching Pattern]({{ '/pages/query-caching-pattern/' | relative_url }}) guide for a practical example of wrapping a web request in a caching block.
