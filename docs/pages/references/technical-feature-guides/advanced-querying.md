---
layout: default
title: "Querying"
description: "Filter, order, aggregate, and join with PikaORM's fluent query API."
active_page: querying
permalink: /pages/querying/
---

# Querying

`find()` covers the simple stuff: `byId`, `byKey`, counts. Add a `where()` and you get a fluent `PikaClassQuery` to chain conditions, ordering, and joins onto, then run with `fetchList()` for a list, `fetchFirst()` for one row, or an aggregate. When a query does not start with a condition (just an ordering, an `IN`, a join), start it with `byQuery()`.

Lookups (`byId`, `byKey`, `all`) are covered in [CRUD]({{ '/pages/crud/' | relative_url }}). This page is everything past that. The examples use a `Todo` bean with `title`, `completed`, and `priority` fields.

## Filter

`where()` takes a SQL fragment with named `:params`. Chain another `where()` to AND, or `orWhere()` to OR.

```java
PikaList<Todo> open = Todo.find()
        .where("completed = :done", "done", false)
        .fetchList();
```

Bind a parameter three ways. Use whichever reads best:

```java
Todo.find().where("title = :t", "t", "Read the docs").fetchList();          // inline
Todo.find().where("title = :t", Map.of("t", "Read the docs")).fetchList();  // map
Todo.find().where("title = :t").withVar("t", "Read the docs").fetchList();  // withVar
```

Conditions are SQL, so the column names are your table's columns, not the Java field names.

## Common conditions

The query has shortcuts for the conditions you write most. They build the SQL and bind the values for you. Reach the query with `byQuery()` (or chain them after a `where()`):

```java
Todo.find().byQuery().whereLike("title", "%docs%").fetchList();
Todo.find().byQuery().whereIn("priority", List.of(1, 2)).fetchList();
Todo.find().byQuery().whereBetween("priority", 1, 3).fetchList();
```

## Order

`orderBy()` defaults to ascending; pass a `SortOrder` for descending. It lives on the query, so start with `where()` or `byQuery()`:

```java
import static edu.montana.pika.query.SortOrder.DESC;

PikaList<Todo> byPriority = Todo.find()
        .where("completed = :done", "done", false)
        .orderBy("priority", DESC)
        .fetchList();
```

## One row

`firstWhere()` returns the first match (or `null`) with the condition inline. Off a longer query, `fetchFirst()` does the same.

```java
Todo byTitle = Todo.find().firstWhere("title = :t", "t", "Read the docs");
Todo next    = Todo.find().where("completed = :d", "d", false).orderBy("priority", DESC).fetchFirst();
```

## Aggregate

Skip loading rows when you only want a number. These run in SQL straight off `find()`:

```java
long open  = Todo.find().count();                                  // every row
long undone = Todo.find().where("completed = :d", "d", false).count();
Double avg = Todo.find().avg("priority");
Object top = Todo.find().max("priority");
```

`count()` returns a `long`; `sum()` and `avg()` return `Double`; `min()` and `max()` return `Object`.

## Join

`join(Class)` infers the foreign key from your naming conventions (here, `todos.projectId` points at `projects.id`). Refer to the joined table by its table name in the condition.

```java
PikaList<Todo> docsTodos = Todo.find()
        .join(Project.class)
        .where("projects.name = :n", "n", "Docs")
        .fetchList();
```

The result is still `PikaList<Todo>`: the join filters the todos, it does not change what you get back.

`thenJoin()` chains a join off the table you just joined, rather than off the root:

```java
Todo.find()
        .join(Project.class)
        .thenJoin(Team.class)      // joins Team to Project, not to Todo
        .where("teams.name = :n", "n", "Platform")
        .fetchList();
```

For an outer join, or a join Pika cannot infer (self-joins, unconventional keys), start with `byQuery()` and pass the `JoinType` or the raw clause:

```java
import static edu.montana.pika.query.JoinType.LEFT;

Todo.find().byQuery().join(LEFT, Project.class).fetchList();

Todo.find().byQuery()
        .join("todos parent ON parent.id = todos.parent_id")
        .where("parent.title = :t", "t", "Launch")
        .fetchList();
```

## Query records

Not every query maps to an entity. For an aggregate, a report, or a custom join, map the rows to a Java `record` with `orm.select(sql, RecordClass)`. Pika matches each record component to a column by the usual convention (`titleCount` to `title_count`), so alias your columns to line up, then calls the record's canonical constructor.

```java
record TitleCount(String title, long count) {}

PikaList<TitleCount> counts = orm.select("""
        SELECT title, COUNT(*) AS count
        FROM todos
        GROUP BY title""", TitleCount.class).toList();
```

The tidy version keeps the SQL with the record, as a static method that returns the typed `QueryResult`. The record becomes a self-contained query:

```java
record TitleCount(String title, long count) {
    static QueryResult<TitleCount> run() {
        return PikaORM.get().select("""
                SELECT title, COUNT(*) AS count
                FROM todos
                GROUP BY title""", TitleCount.class);
    }
}

PikaList<TitleCount> counts = TitleCount.run().toList();
```

`PikaORM.get()` is the default ORM, so the record needs nothing passed in. This keeps reporting queries next to the type they produce.

## See the generated SQL

`explain()` runs the database's query planner and returns its output. Reach for it when a query is slower than you expect.

```java
Todo.find().where("completed = :d", "d", false).explain();
```

## Going further

Paging large result sets has its own page: [Paging]({{ '/pages/paging/' | relative_url }}). For raw SQL and using the ORM without beans, see [Plain Java Objects]({{ '/pages/plain-objects/' | relative_url }}).
