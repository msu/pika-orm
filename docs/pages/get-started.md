---
layout: default
title: "Get Started with PikaORM"
description: "PikaORM - the lightweight, minimal MicroORM for Java. No config files, zero magic, pure SQL power."
active_page: get-started
permalink: /pages/get-started/
---


PikaORM is a lightweight MicroORM for Java: no config files, no annotations, just a fluent builder API and SQL when you want it. See the [Philosophy of Pika]({{ '/pages/philosophy/' | relative_url }}) for the why.

## Core Principles

- **Concision** - No config files or annotations. Map and query with a plain builder API over plain Java classes.
- **Exposes SQL** - Drop to raw SQL anytime through multiple entry points.
- **Dual paradigm** - A SQL-native side and a POJO side. Mix them as your domain needs.

## A First Example

```java
public class Todo {
    Long id;
    String title;
    String description;

    public Todo() {}
    public Todo(String title, String description) {
        this.title = title;
        this.description = description;
    }
}

// Connect and apply schema migrations
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
        .makeDefaultORM()
        .withMigrations(new AppMigrations())
        .applyMigrations();

// Insert a row
orm.insert(new Todo("Read docs", "Finish the PikaORM guide"));

// Fluent query
PikaList<Todo> active = orm.query(Todo.class)
        .where("completed = :val", "val", false)
        .fetchList();
```

## Supported Databases

- **SQLite** - Use `.withSQLiteQuirks()` on ORM configuration for small corner cases.
- **H2** - Supports In-Memory, Oracle, PostgreSQL, and SQLServer dialects.
- **MariaDB** - Supported with standard JDBC usage.

## Where to Next

- New to databases? [What are Databases?]({{ '/pages/what-are-databases/' | relative_url }}) then [What is an ORM?]({{ '/pages/what-is-an-orm/' | relative_url }})
- Want a full walkthrough? [Web Quickstart]({{ '/pages/quickstart/' | relative_url }})
- Feature docs? [Guides]({{ '/pages/querying/' | relative_url }})
- Examples? [Avoiding N+1]({{ '/pages/n-plus-1-avoidance/' | relative_url }})
