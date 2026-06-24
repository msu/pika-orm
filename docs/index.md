---
layout: landing
title: "PikaORM — The lightweight MicroORM for Java"
description: "PikaORM — the lightweight, minimal MicroORM for Java. No config files, zero magic, just clean builder-style API over plain SQL."
permalink: /
---

<section class="hero">
  <div class="hero-logo-badge">
    <img src="{{ '/assets/img/pika-icon.png' | relative_url }}" alt="PikaORM logo" class="hero-logo">
  </div>

  <h1 class="hero-wordmark">PikaORM</h1>

  <p class="hero-lead">PikaORM is a simple ORM for Java, designed for the Montana State Databases class. It follows the
    ActiveRecord pattern, mapping java classes to database tables.  It features a fluent query API, validation, life-cycle
    callbacks and allows you to drop to raw SQL whenever you want.  It does not use config files or annotations.</p>

  <div class="hero-actions">
    <a class="btn btn-primary" href="{{ '/pages/get-started/' | relative_url }}">Get Started</a>
    <a class="btn btn-ghost" href="https://github.com/msu/pika-orm" target="_blank" rel="noopener noreferrer">View on GitHub</a>
  </div>
</section>

<section class="hero-code">
<div class="hero-code-inner" markdown="1">

```java
import edu.montana.pika.bean.EnterprisePikaBean;

// ActiveRecord style classes map to tables, e.g. `todos`  
public class Todo extends PikaBean {
    private String title;
    private String description;
    private Boolean completed = false;

    public Todo() {}

    public void setTitle(String title)             { this.title = title; }
    public void setDescription(String description)  { this.description = description; }

    @Override
    protected void validation() {
        require("title");
    }
}

// Connect once and run your migrations — the default ORM backs every bean
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
        .makeDefaultORM()
        .withMigrations(new AppMigrations()) // Migrations keep your schema up to date
        .applyMigrations();

// The object persists itself: validate, then INSERT
Todo todo = new Todo();
todo.setTitle("Read the docs");
todo.setDescription("Finish the PikaORM guide");
todo.saveOrThrow();

// Query with the fluent builder...
PikaList<Todo> active = orm.query(Todo.class)
        .where("completed = :done", "done", false)
        .fetchList();

// ...or drop down to raw SQL whenever you need to
PikaList<Todo> rows = orm.select("SELECT * FROM todos", Todo.class).toList();
```

</div>
</section>
