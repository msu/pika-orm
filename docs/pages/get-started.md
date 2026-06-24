---
layout: default
title: "Get Started with PikaORM"
description: "Install PikaORM, define a schema and a bean, and run CRUD and queries in a few minutes."
active_page: get-started
permalink: /pages/get-started/
---

# Get Started

PikaORM is a lightweight ActiveRecord ORM for Java. Map a plain class to a table, get CRUD and a fluent query API for free, and drop to raw SQL whenever you want. No config files, no annotations. See the [Philosophy of Pika]({{ '/pages/philosophy/' | relative_url }}) for the reasoning.

## Install

Add PikaORM to your project (it targets Java 17):

```xml
<dependency>
    <groupId>edu.montana</groupId>
    <artifactId>pika-orm</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Add a JDBC driver for your database too — for SQLite:

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.49.1.0</version>
</dependency>
```

## Define your schema

Schema lives in code. Extend `Migrations` and add one `PikaMigration` per change:

```java
import edu.montana.pika.migrations.Migrations;
import edu.montana.pika.migrations.PikaMigration;

public class AppMigrations extends Migrations {
    @Override
    public void migrations() {
        add(this::createTodos);
    }

    public PikaMigration createTodos() {
        return makeMigration("001_create_todos")
                .up("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id        INTEGER PRIMARY KEY,
                        title     TEXT,
                        completed INTEGER
                    );
                    """)
                .down("DROP TABLE todos;");
    }
}
```

See [Migrations]({{ '/pages/migrations/' | relative_url }}) for rollbacks and the interactive CLI.

## Define a bean

A domain object is a plain class that extends `PikaBean`. Fields map to columns by convention — class `Todo` maps to table `todos`, field `dueDate` to column `due_date`. A static `find()` gives you a typed entry point for queries.

```java
import edu.montana.pika.bean.PikaBean;
import edu.montana.pika.query.PikaClassFinder;

public class Todo extends PikaBean {
    Long id;
    String title;
    Boolean completed = false;

    public Todo() {}
    public Todo(String title) { this.title = title; }

    public void setCompleted(Boolean completed) { this.completed = completed; }

    public static PikaClassFinder<Todo> find() {
        return find(Todo.class);
    }
}
```

## Connect

Create the ORM once at startup and apply your migrations. `makeDefaultORM()` registers it as the default instance that backs every bean.

```java
import edu.montana.pika.PikaORM;

PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
        .makeDefaultORM()
        .withMigrations(new AppMigrations())
        .applyMigrations();
```

## CRUD

Beans persist themselves:

```java
// Create
Todo todo = new Todo("Read the docs");
todo.save();                  // INSERT

// Read by primary key
Todo found = Todo.find().byId(1L);

// Update — save() knows it is an existing row
found.setCompleted(true);
found.save();                 // UPDATE

// Delete
found.delete();
```

## Query

`find()` returns a fluent builder:

```java
import edu.montana.pika.query.PikaList;

PikaList<Todo> open = Todo.find()
        .where("completed = :done", "done", false)
        .fetchList();
```

## Where to next

- [CRUD]({{ '/pages/crud/' | relative_url }}) — the full create/read/update/delete surface
- [Querying]({{ '/pages/querying/' | relative_url }}) — filtering, ordering, joins, and aggregates
- [Validation & Forms]({{ '/pages/validation/' | relative_url }}) — validating beans and binding web form data
- [Cheat Sheet]({{ '/pages/cheat-sheet/' | relative_url }}) — every operation at a glance
