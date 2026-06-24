---
layout: default
title: "Plain Java Objects"
description: "Use PikaORM without the PikaBean base class: CRUD, queries, and raw SQL straight off the ORM instance."
active_page: plain-objects
permalink: /pages/plain-objects/
---

# Plain Java Objects

You do not have to extend `PikaBean`. Any plain class works. Instead of the bean's `save()` and `find()`, you call the same operations on the ORM and pass the object in. This is the SQL-native side of Pika.

A plain class maps by the same [conventions]({{ '/pages/mapping/' | relative_url }}). It needs a no-arg constructor and fields; Pika reads and writes the fields directly.

```java
public class Todo {
    Long id;
    String title;
    Boolean completed = false;

    public Todo() {}
    public Todo(String title) { this.title = title; }

    public void setTitle(String title) { this.title = title; }
}
```

## CRUD through the ORM

```java
Todo t = new Todo("Read the docs");
long id = orm.insert(t);               // INSERT, returns the generated key

Todo found = orm.find(Todo.class).byId(1L);

found.setTitle("Read them again");
boolean updated = orm.update(found);   // UPDATE by primary key

orm.delete(found);                     // DELETE by primary key
```

Insert many rows in one statement with `insertAll`:

```java
orm.insertAll(new Todo("a"), new Todo("b"), new Todo("c"));
```

## Querying

`orm.find(Class)` and `orm.query(Class)` return the same builders the bean's `find()` does, so everything in [Querying]({{ '/pages/querying/' | relative_url }}) works here too:

```java
PikaList<Todo> open = orm.query(Todo.class)
        .where("completed = :done", "done", false)
        .fetchList();
```

## Raw SQL

When you want full control, write the SQL yourself. With no mapped class you get `ResultMap` rows, each a map keyed by column with typed getters:

```java
PikaList<ResultMap> rows = orm.select("SELECT id, title FROM todos").toList();
for (ResultMap row : rows) {
    long id      = row.getLong("id");
    String title = row.getString("title");
}
```

Pass a class and Pika maps each row onto it:

```java
PikaList<Todo> todos = orm.select("SELECT * FROM todos WHERE completed = 0", Todo.class).toList();
```

You can also map a query straight onto a Java `record`, which is handy for aggregates and reports. That lives on the [Querying]({{ '/pages/querying/' | relative_url }}) page under Query records.

Named parameters work in raw SQL too, supplied as a map:

```java
orm.select("SELECT * FROM todos WHERE title = :t",
           Map.of("t", "Read the docs"), Todo.class).toList();
```
