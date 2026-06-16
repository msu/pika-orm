---
layout: default
title: "Paging"
description: "PikaORM Paging: built-in pagination API with metadata, total counts, and URL helpers for web frameworks."
active_page: paging
permalink: /pages/paging/
---

# Paging

When querying large tables, you should never load the entire dataset into memory at once. PikaORM provides a robust, built-in pagination API on both `PikaClassQuery` and `PikaQueryBuilder`.

## Basic Pagination

Pagination is 1-indexed (the first page is page 1, not 0). 

```java
// Fetch the 3rd page of users, with 20 users per page
PikaList<User> users = orm.query(User.class)
    .where("is_active = :active", "active", true) //Another way to assign a variable
    .page(3)
    .pageSize(20)
    .fetchList();
```

If you do not specify a `pageSize`, PikaORM defaults to `50`.

## Pagination Metadata

When you execute a paginated query, PikaORM automatically runs a background `COUNT(*)` query matching your current `.where()` conditions. This allows the resulting `QueryResult` (or the query builder itself) to provide accurate metadata for rendering UI elements.

```java
PikaClassQuery<User> query = orm.query(User.class).page(2);
QueryResult<User> result = query.fetch();

// Total number of rows matching the query across all pages
long totalUsers = query.totalCount();

// Total number of pages available (totalCount / pageSize)
long maxPages = query.totalPages();

// Boolean helpers for UI rendering
boolean isFirst = query.isFirstPage(); // false (we are on page 2)
boolean isLast = query.isLastPage();
```

## URL Helpers for Web Frameworks

Rendering "Next" and "Previous" links in a web template often requires tedious string manipulation to update query parameters. PikaORM provides built-in URL string manipulators that automatically update or append the `page=` parameter to a given URL.

```java
String currentUrl = "/admin/users?sort=desc&page=2";

// Returns "/admin/users?sort=desc&page=3"
String nextUrl = query.nextPageURL(currentUrl);

// Returns "/admin/users?sort=desc&page=1"
String prevUrl = query.previousPageURL(currentUrl);
```

## Global Pagination Defaults

You can configure the global default page size and the exact SQL dialect syntax for offsets when setting up the ORM:

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .withDefaultPageSize(100) // Change the default from 50
    // Provide a custom offset clause if you are using an obscure database dialect
    // {0} is replaced by the limit, {1} is replaced by the offset
    .withOffsetClause("LIMIT {0} OFFSET {1}") 
    .makeDefaultORM();
```
