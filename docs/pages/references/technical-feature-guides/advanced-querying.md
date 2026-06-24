---
layout: default
title: "Querying"
description: "Filter, order, aggregate, and join with PikaORM's fluent query API."
active_page: querying
permalink: /pages/querying/
---

# Querying

`find()` returns a fluent query builder. Chain conditions, ordering, and joins onto it, then run it with `fetchList()` for a list, `fetchFirst()` for one row, or an aggregate like `count()`.

Simple lookups (`byId`, `byKey`, `all`) are covered in [CRUD]({{ '/pages/crud/' | relative_url }}). This page is everything past that. The examples use a `Todo` bean with `title`, `completed`, and `priority` fields.

## Filter

`where()` takes a SQL fragment with named `:params`. Chain more `where()` calls to AND them together, or `orWhere()` to OR.

```java
PikaList<Todo> open = Todo.find()
        .where("completed = :done", "done", false)
        .fetchList();
```

You can bind a parameter three ways. Use whichever reads best:

```java
Todo.find().where("title = :t", "t", "Read the docs").fetchList();          // inline
Todo.find().where("title = :t", Map.of("t", "Read the docs")).fetchList();  // map
Todo.find().where("title = :t").withVar("t", "Read the docs").fetchList();  // withVar
```

Conditions are SQL, so the column names are your table's columns, not the Java field names.

## Common conditions

Pika has shortcuts for the conditions you write most. They build the SQL and bind the values for you.

```java
Todo.find().whereLike("title", "%docs%").fetchList();
Todo.find().whereIn("priority", List.of(1, 2)).fetchList();
Todo.find().whereBetween("priority", 1, 3).fetchList();
```

## Order

`orderBy()` defaults to ascending. Pass a `SortOrder` for descending.

```java
import static edu.montana.pika.query.SortOrder.DESC;

PikaList<Todo> byPriority = Todo.find()
        .orderBy("priority", DESC)
        .fetchList();
```

## One row

`fetchFirst()` returns the first match (or `null`). `firstWhere()` is the same thing with the condition inline.

```java
Todo next = Todo.find().where("completed = :d", "d", false).fetchFirst();
Todo byTitle = Todo.find().firstWhere("title = :t", "t", "Read the docs");
```

## Aggregate

Skip loading rows when you only want a number. These run the aggregate in SQL and return the result.

```java
long open    = Todo.find().where("completed = :d", "d", false).count();
Double avg   = Todo.find().avg("priority");
Object top   = Todo.find().max("priority");
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

The result is still `PikaList<Todo>` -- the join filters the todos, it does not change what you get back.

For an outer join, pass a `JoinType`:

```java
import static edu.montana.pika.query.JoinType.LEFT;

Todo.find().join(LEFT, Project.class).fetchList();
```

`thenJoin()` chains a join off the table you just joined, rather than off the root:

```java
Todo.find()
        .join(Project.class)
        .thenJoin(Team.class)      // joins Team to Project, not to Todo
        .where("teams.name = :n", "n", "Platform")
        .fetchList();
```

When Pika cannot infer the relationship (self-joins, unconventional keys), pass the join clause as a string:

```java
Todo.find()
        .join("todos parent ON parent.id = todos.parent_id")
        .where("parent.title = :t", "t", "Launch")
        .fetchList();
```

## See the generated SQL

`explain()` runs the database's query planner and returns its output. Reach for it when a query is slower than you expect.

```java
Todo.find().where("completed = :d", "d", false).explain();
```

## Going further

Paging large result sets has its own page: [Paging]({{ '/pages/paging/' | relative_url }}). For full SQL control or results that are not beans, see [Plain Java Objects]({{ '/pages/plain-objects/' | relative_url }}).
