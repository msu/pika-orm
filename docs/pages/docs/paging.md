---
layout: default
title: "Paging"
description: "Slice large queries into pages with PikaORM, with totals and ready-made page links."
active_page: paging
permalink: /pages/paging/
---

# Paging

Do not pull a whole table into memory. `page()` and `pageSize()` slice a query into pages, and the query tracks the totals you need to render page controls.

Paging is 1-indexed: the first page is page 1.

## Page a query

```java
PikaList<User> users = User.find()
        .where("active = :a", "a", true)
        .page(3)
        .pageSize(20)
        .fetchList();
```

Leave off `pageSize()` and Pika uses the default, which is 20 (configurable below).

## Page metadata

When you run a paged query, Pika also runs a `COUNT(*)` with the same `where` conditions, so it can tell you how many rows and pages exist. Read the totals straight off the query:

```java
PikaClassQuery<User> q = User.find().where("active = :a", "a", true).page(2);
PikaList<User> users = q.fetchList();

long total    = q.totalCount();    // matching rows across all pages
long pages    = q.totalPages();    // total pages at the current page size
boolean first = q.isFirstPage();
boolean last  = q.isLastPage();
```

## Page links

Building "next" and "previous" links means rewriting the `page=` query parameter by hand. Pika does it for you:

```java
String url  = "/users?sort=desc&page=2";
String next = q.nextPageURL(url);       // /users?sort=desc&page=3
String prev = q.previousPageURL(url);   // /users?sort=desc&page=1
```

Pass a second argument if your page parameter is not named `page`.

## Defaults

Set the default page size when you build the ORM. If your database has unusual offset syntax, override that too.

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
        .withDefaultPageSize(100)
        .withOffsetClause("LIMIT {0} OFFSET {1}")  // {0} = limit, {1} = offset
        .makeDefaultORM();
```
