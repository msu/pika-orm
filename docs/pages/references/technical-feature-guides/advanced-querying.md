---
layout: default
title: "Advanced Querying"
description: "PikaORM query types: PikaClassFinder, PikaClassQuery, PikaQueryBuilder, raw SQL, and joins explained."
active_page: querying
permalink: /pages/querying/
---

# Advanced Querying

## Overview

PikaORM provides several querying mechanisms designed for different use cases and levels of abstraction. This documentation covers the distinctions between the core query components and when to use each approach.

## Query Types and Components

### 1. PikaClassFinder

**Purpose**: High-level, convenient finder interface for common query patterns.

**Key Features**:
- Simple method-based querying (no SQL required for basic operations)
- Map-based parameter binding
- Built-in methods for common operations (`byId`, `firstWhere`, `where`, `all`)
- Collection parameter support (automatic IN clause handling)
- Direct `toList()` execution

**Usage Patterns**:

#### Single Record Lookup
```java
SampleModel fromDb = orm.find(SampleModel.class).byId(id);
```

#### First Match Query
```java
SampleModel result = orm.find(SampleModel.class)
    .firstWhere("str_val=:val", Map.of("val", "bar"));
```

#### Filtered Collections
```java
List<SampleModel> results = orm.find(SampleModel.class)
    .where("int_val=:val", Map.of("val", 10))
    .toList();
```

#### Collection Parameters (IN clauses)
```java
var results = orm.find(SampleModel.class)
    .where("str_val in :strs", Map.of("strs", List.of("foo", "bar")))
    .toList();
```

#### All Records
```java
List<SampleModel> results = orm.find(SampleModel.class).all().toList();
```

**When to Use PikaClassFinder**:
- For simple, common query patterns
- When you prefer method-based over SQL-based querying
- For quick lookups and basic filtering
- When working with collection parameters (IN clauses)

### 2. PikaClassQuery

**Purpose**: Type-safe, fluent SQL-based querying interface for specific entity classes.

**Key Features**:
- Strongly typed to a specific model class
- Fluent method chaining for building complex queries
- Named parameter support with `:paramName` syntax
- Query plan analysis with `explain()` method
- Direct integration with ORM instance

**Usage Patterns**:

#### Basic Query Construction
```java
var query = orm.query(SampleModel.class)
    .where("date_val < :val")
    .withVar("val", new Date(2050, 1, 1));
List<SampleModel> results = query.fetchList();
```

#### Chained Conditions
```java
var query = orm.query(SampleModel.class)
    .where("date_val < :val")
    .withVar("val", new Date(150, 1, 1))
    .where("date_val > :val2")
    .withVar("val2", new Date(20, 1, 1));
List<SampleModel> results = query.fetchList();
```

#### Query Analysis
```java
var explain = SampleEgb.find().byQuery()
    .where("str_val IS NOT NULL")
    .explain();
```

#### Static Method Integration (for PikaBean-enabled models)
```java
var query = SampleEgb.find().byQuery()
    .where("date_val < :val")
    .withVar("val", new Date(2050, 1, 1));
List<SampleEgb> results = query.fetchList();
```

**When to Use PikaClassQuery**:
- When you need fine-grained SQL control
- For complex queries with multiple conditions
- When you need query performance analysis (`explain()`)
- For building dynamic queries with conditional logic

### 3. PikaQueryBuilder

**Purpose**: Fluent SQL query construction with optional result mapping for complex queries.

**Key Features**:
- Fluent SQL building without writing raw SQL strings
- Support for JOINs, column selection, ordering, and paging
- Dual result modes: `ResultMap` (generic) or typed entity mapping
- Table and column aliasing support
- Built-in pagination with metadata

**Usage Patterns**:

#### Basic Column Selection with Entity Mapping
```java
var query = orm.queryBuilder("Albums")
    .select("Title")
    .where("Title LIKE '%A%'")
    .withResult(Album.class);
var results = query.fetchList();
```

#### Generic ResultMap Queries
```java
var query = orm.queryBuilder("Albums")
    .select("Title as AlbumTitle")
    .where("Title LIKE '%A%'");
var results = query.fetchList(); // Returns List<ResultMap>
```

#### Complex JOINs with Mixed Columns
```java
var query = orm.queryBuilder("albums")
    .select("albums.*", "tracks.Name")
    .join("Tracks on albums.AlbumId = tracks.TrackId")
    .where("Title LIKE '%A%'");
var results = query.fetchList();
```

#### Column Remapping
```java
var query = orm.queryBuilder("albums")
    .select("tracks.Name as Title") // Remap tracks.Name to Album.title
    .join("Tracks on albums.AlbumId = tracks.TrackId")
    .where("albums.Title LIKE '%A%'")
    .withResult(Album.class);
```

#### Ordering and Pagination
```java
var query = orm.queryBuilder("Albums")
    .select("Title", "Artists.Name as artistname")
    .join("Artists on artists.artistId = albums.artistId")
    .where("artistname LIKE '%Led Zeppelin%'")
    .orderBy("Title", SortOrder.DESC);
```

**When to Use PikaQueryBuilder**:
- For complex multi-table queries with JOINs
- When you need column selection and aliasing
- For building queries programmatically without raw SQL
- When you want both generic and typed result options

### 4. Raw SQL Query Interface

**Purpose**: Direct SQL execution with flexible result mapping.

**Key Features**:
- Full SQL control with named parameters
- Multiple result mapping options: `ResultMap`, Records, Entity classes
- Support for complex queries (JOINs, GROUP BY, etc.)
- Case-insensitive result mapping

**Usage Patterns**:

#### Generic `ResultMap` output Query
```java
var results = orm.select(
    "SELECT * FROM sample_models WHERE int_val=:x ORDER BY id", 
    Map.of("x", 10))
    .toList();
ResultMap first = results.first();
ResultMap insensitive = first.toCaseInsensitiveMap();
```

#### Record-Based Mapping
```java
record SampleModelGroupByQuery(String strVal, Long sum) {
    public static QueryResult<SampleModelGroupByQuery> exec() {
        return PikaORM.get().select("""
            SELECT str_val, sum(int_val) as sum
            FROM sample_models
            GROUP BY str_val
            ORDER BY str_val""", SampleModelGroupByQuery.class);
    }
}

var results = SampleModelGroupByQuery.exec().toList();
```

#### Entity Class Mapping
```java
var result = orm.select("SELECT * FROM Todos", Todo.class).toList();
```

## Choosing the Right Query Approach

| Query Type | Best For | Complexity | Type Safety | SQL Control | JOINs | Pagination |
|---|---|---|---|---|---|---|
| **PikaClassFinder** | Simple lookups, basic filtering | Low | High | Low | No | No |
| **PikaClassQuery** | Complex entity queries, performance analysis | Medium | High | Medium | No | Yes |
| **PikaQueryBuilder** | Multi-table queries, flexible column selection | Medium-High | Medium | High | Yes | No |
| **Raw SQL** | Maximum control, complex reporting | High | Low | Manually | Manually | Manually |

## Joins in Queries

### Basic Entity Joins
```java
// Join from Album to Artist
var query = orm.query(Album.class)
    .join(Artist.class)
    .where("artists.name = 'AC/DC'");
var albums = query.fetchList();

// Join from Artist to Album  
var query = orm.query(Artist.class)
    .join(Album.class)
    .where("albums.Title LIKE 'A%'");
var artists = query.fetchList();
```

### Join Types
```java
// Inner join (default)
orm.query(Artist.class).join(Album.class);

// Left join
import static edu.montana.pika.query.JoinType.LEFT;
orm.query(Artist.class).join(LEFT, Album.class);
```

### Chained Joins
```java
// Chain multiple joins
var query = orm.query(Artist.class)
    .join(Album.class)
    .thenJoin(Track.class)
    .where("tracks.Name LIKE 'A%'");
var artists = query.fetchList();
```

> [!NOTE]
> Using `thenJoin` allows Pika to infer the join condition from the most recently joined table (e.g., joining Track onto Album).

### Custom Join Clauses
```java
// For complex joins like self-joins
var query = orm.query(Employee.class)
    .join("employees AS boss ON employees.ReportsTo = boss.EmployeeID")
    .where("boss.Email = :email")
    .withVar("email", "andrew@chinookcorp.com");
```