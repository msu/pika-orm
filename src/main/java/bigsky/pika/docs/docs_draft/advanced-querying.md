# Query types in PikaORM

> This page as just been dedicated to more specific details about the several query types using the different query classes `PikaQuery` `PikaClassFinder` `PikaQueryBuilder` `PikaManyThroughQuery` ` PikaManyQuery` ` PikaClassQuery` what they are, how to use them, and why!

### Collection Parameter Handling

The finder interface automatically handles collections in parameters, converting them to SQL IN clauses:

```java
// Single collection parameter
var results = orm.find(SampleModel.class)
    .where("str_val in :strs", Map.of("strs", List.of("foo", "bar")))
    .toList();
// Generates: WHERE str_val in (?, ?)
```

### Result Mapping Strategies

#### ResultMap (Generic)

For dynamic result handling with type conversion utilities:

```java
PikaORM.ResultMap first = results.getFirst();
PikaORM.ResultMap insensitive = first.toCaseInsensitiveMap();
assertEquals("foo", insensitive.get("str_val"));
assertEquals(10, insensitive.get("int_val"));
assertEquals(true, insensitive.asBoolean("bool_val"));
```

#### Record Mapping (Type-Safe)

For strongly-typed query results with custom logic:

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
```

### Query Performance Analysis

Use the `explain()` method for query optimization:

~~~java
var explain = SampleEgb.find().byQuery()
    .where("str_val IS NOT NULL")
    .explain();
System.out.println(explain.toString("\n"));
```# Pika ORM Advanced Querying Documentation

## Overview

The Pika ORM provides several querying mechanisms designed for different use cases and levels of abstraction. This documentation covers the distinctions between the core query components and when to use each approach.

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
~~~

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

#### Static Method Integration (for EGB-enabled models)

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

#### Generic `PikaORM.ResultMap` output Query

```java
var results = orm.select(
    "SELECT * FROM sample_models WHERE int_val=:x ORDER BY id", 
    Map.of("x", 10))
    .toList();
PikaORM.ResultMap first = results.getFirst();
PikaORM.ResultMap insensitive = first.toCaseInsensitiveMap();
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

| Query Type                            | Best For                                       | Complexity  | Type Safety | SQL Control | JOINs    | Pagination |
| ------------------------------------- | ---------------------------------------------- | ----------- | ----------- | ----------- | -------- | ---------- |
| **PikaClassFinder**                   | Simple lookups, basic filtering                | Low         | High        | Low         | No       | No         |
| **PikaClassQuery**                    | Complex entity queries, performance analysis   | Medium      | High        | Medium      | No       | Yes        |
| **PikaQueryBuilder**                  | Multi-table queries, flexible column selection | Medium-High | Medium      | High        | Yes      | No         |
| **Raw SQL** (`select()` and `exec()`) | Maximum control, complex reporting             | High        | Low         | Manually    | Manually | Manually   |

### Choose your Query Type!

**Use `PikaClassFinder` when**:

- You need simple CRUD operations
- Working with single-table queries
- You prefer method-based over SQL syntax
- You need collection parameter support (IN clauses)

**Use `PikaClassQuery` when**:

- You need more SQL control than finder methods provide
- Building dynamic queries with conditional logic
- You want query performance analysis with `explain()`
- You need pagination with metadata (`isFirstPage()`, `isLastPage()`)

**Use `PikaQueryBuilder` when**:

- You need multi-table JOINs
- You want fluent SQL construction without raw SQL strings
- You need flexible column selection and aliasing
- You want to switch between generic and typed result mapping
- Building complex queries programmatically

**Use Raw SQL Interface (using `select` or `exec()`)when**:

- You need maximum SQL control and optimization
- Building reporting or analytics queries with complex aggregations
- You need database-specific features or stored procedures
- You need custom result mapping to DTOs or records

## Method Details

--------------------------

### `PikaClassFinder` Important Methods:

---------------

#### `byId(Object id)`

- Retrieves a single entity by its primary key.

#### `firstWhere(String condition, Map<String, Object> params)`

- Returns the first entity matching the given condition.

#### `where(String condition, Map<String, Object> params)`

- Filters entities based on the condition. Supports collection parameters for IN clauses.

#### `all()`

- Retrieves all entities of the specified type.

#### `toList()`

- Executes the finder query and returns results as a `PikaList<query_class>` which is a custom iterable.

-------------

### `PikaClassQuery` Important Methods:

--------------------

#### `where(String condition)`

- Adds a WHERE clause condition to the query. Multiple calls are chained with AND logic.

#### `withVar(String name, Object value)`

- Binds a named parameter to a value for use in WHERE conditions. Uses the `:paramName` syntax in conditions.

#### `page(int pageNumber)`

- Sets the page number for pagination (1-based indexing).

#### `pageSize(int size)`

- Sets the number of records per page.

#### `orderBy(String column)`

- Adds an ORDER BY clause to the query.

#### `fetchList()`

- Executes the query and returns a `List<T>` of the specified entity type.

#### `explain()`

- Returns query execution plan analysis for performance optimization.

#### `isFirstPage()` / `isLastPage()`

- Returns pagination metadata indicating if current page is first/last.

-----------------------

### `PikaQueryBuilder` Important Methods:

-------------

#### `select(String... columns)`

Specifies columns to select. Supports aliases and table qualification.

#### `join(String joinClause)`

Adds a JOIN clause (supports all JOIN types).

#### `where(String condition)`

Adds a WHERE clause condition.

#### `orderBy(String column, SortOrder order)`

Adds an ORDER BY clause with sort direction.

#### `withResult(Class<T> resultClass)`

Specifies the result should be mapped to entity objects instead of ResultMap.

#### `fetchList()`

Executes the query and returns results (type depends on `withResult()` usage).

#### `fetchFirst()`

Executes the query and returns the first result only.

------------

### Raw SQL Methods:

----------

#### `select(String sql, Map<String, Object> params)`

- Executes SQL and returns `QueryResult<ResultMap>` for generic result handling.

#### `select(String sql, Class<T> resultClass)`

- Executes SQL and maps results to specified entity class or record type.

#### `exec(String sql)`

- The most bare bones SQL execution method, no validation or transaction safety, use with *Caution*. Recommended to use with transaction lambda.

## Advanced Feature Examples

### Pagination Support

`PikaClassQuery` provides built-in pagination with metadata:

```java
// First page (1-based indexing)
var sampleModels = orm.find(SampleModel.class).byQuery()
 .page(1).pageSize(10).orderBy("id").fetchList();

// Check pagination status
var queryResult = orm.find(SampleModel.class).byQuery()
    .page(10).pageSize(10).orderBy("id");
boolean isFirst = queryResult.isFirstPage(); // false
boolean isLast = queryResult.isLastPage();   // depends on total records

// Works with WHERE conditions
var filteredQuery = orm.find(SampleModel.class).byQuery()
    .where("str_val LIKE :s")
    .withVar("s", "sample%")
    .page(10).pageSize(10).orderBy("id");
```

### Column Selection and Aliasing  with `PikaQueryBuilder`

`PikaQueryBuilder` offers sophisticated column handling:

```java
// Basic column selection
orm.queryBuilder("Albums").select("Title").where("Title LIKE '%A%'")

// Column aliasing
orm.queryBuilder("Albums").select("Title as AlbumTitle")

// Table qualification
orm.queryBuilder("Albums").select("Albums.Title as AlbumTitle")

// Mixed columns with wildcards
orm.queryBuilder("albums")
    .select("albums.*", "tracks.Name")
    .join("Tracks on albums.AlbumId = tracks.TrackId")

// Column remapping to different entity fields
orm.queryBuilder("albums")
    .select("tracks.Name as Title") // Maps tracks.Name to Album.title field
    .join("Tracks on albums.AlbumId = tracks.TrackId")
    .withResult(Album.class)
```

### Result Mapping Flexibility (QueryBuilder)

Switch between generic and typed results:

```java
// Generic ResultMap (default)
var genericResults = orm.queryBuilder("Albums")
    .select("Title")
    .fetchList(); // Returns List<ResultMap>

// Typed entity mapping
var typedResults = orm.queryBuilder("Albums")
    .select("Title") 
    .withResult(Album.class)
    .fetchList(); // Returns List<Album>

// Single result
var singleResult = orm.queryBuilder("Albums")
    .select("Title")
    .where("Title = 'For Those About To Rock We Salute You'")
    .withResult(Album.class)
    .fetchFirst(); // Returns Album
```

# Pika ORM One-to-Many Relationships

## Setup

Declare relationships using `loadMany()` in the parent entity:

```java
public class FooContainer {
    private long id;
    
    public PikaORM.PikaManyQuery<Foo> getFoos() {
        return PikaORM.get().loadMany(this, Foo.class); //Foo.class being the other side of 1-N
    }
}
```

## Basic Usage

### Load Related Entities

```java
List<Foo> foos = fooContainer.getFoos().toList();
```

### Navigate Back to Parent

```java
FooContainer parent = foo.getFooContainer(); // Assumes getFooContainer() method exists
```

## Creating Related Entities

### Add and Save (Immediate Persistence)

```java
Foo newFoo = new Foo();
fooContainer.getFoos().addAndSave(newFoo);
// Automatically sets foreign key and saves to database
```

### Create (Deferred Persistence)

```java
Foo foo = fooContainer.getFoos().create(); // Foreign key pre-populated
orm.insert(foo); // Manual save required
```

### Manual Assignment (Traditional)

```java
Foo newFoo = new Foo();
newFoo.setFooContainerId(fooContainer.getId());
orm.insert(newFoo);
```

## Complete Example

```java
// Setup
FooContainer container = new FooContainer();
orm.insert(container);

// Add entities using different patterns
for (int i = 0; i < 5; i++) {
    container.getFoos().addAndSave(new Foo()); // Immediate save
}

for (int i = 0; i < 5; i++) {
    Foo foo = container.getFoos().create(); // Deferred save
    orm.insert(foo);
}

// Load all related entities
List<Foo> allFoos = container.getFoos().toList(); // Returns 10 entities
```

## Key Methods

- `toList()` - Load all related entities
- `addAndSave(entity)` - Add entity with immediate persistence
- `create()` - Create entity with foreign key pre-set

## Notes

- Foreign key columns should follow pattern: `{parentEntityName}Id`
- `toList()` executes a query each time called
- `addAndSave()` provides automatic foreign key management

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

> Allows `join` inferenced from just your POJO class

### Join Types

```java
// Inner join (default)
orm.query(Artist.class).join(Album.class); // Returns 204 artists with albums

// Left join
orm.query(Artist.class).join(LEFT, Album.class); // Returns all 275 artists
```

> Uses the same logic of `left` and `right` joins as seen in native SQL

```java
// Chain multiple joins
var query = orm.query(Artist.class)
    .join(Album.class)
    .thenJoin(Track.class)
    .where("tracks.Name LIKE 'A%'");
var artists = query.fetchList();
```

> Using `thenJoin` allows Pika to make enough assumptions about the joins to allow smooth table joining.

```java
// For complex joins like self-joins
var query = orm.query(Employee.class)
    .join("employees AS boss ON employees.ReportsTo = boss.EmployeeID")
    .where("boss.Email = :email")
    .withVar("email", "andrew@chinookcorp.com");
```

> Can always default to your own `join` statement in query build up if simply joining tables is not specific enough

## Notes

- Be careful about your custom mapping layout when using more complex joins, may get weird.
- `toList()` executes a query each time called
- `addAndSave()` provides automatic foreign key management
- Joins automatically handle foreign key relationships between entities.



*Note: This documentation is in progress and will by dynamically updated over time, bear with us please!*