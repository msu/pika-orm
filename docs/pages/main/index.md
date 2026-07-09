---
layout: landing
title: "PikaORM — The lightweight MicroORM for Java"
description: "A lightweight ActiveRecord ORM for Java with a fluent query API and direct access to SQL."
permalink: /
---

<section class="hero">
  <h1 class="visually-hidden">PikaORM</h1>
  <div class="hero-banner">
    <div class="svg-container"></div>
    <div class="hero-banner-text">
      <p class="hero-wordmark">PikaORM</p>
      <p class="hero-lead">PikaORM is a simple ORM for Java, designed for the <a href="https://www.cs.montana.edu">Montana State University</a> Databases class. It follows the ActiveRecord pattern, mapping java classes to database tables. It features a fluent query API, validation, life-cycle callbacks and allows you to drop to raw SQL if you need to.</p>
      <p class="hero-lead">Pika does not use config files or annotations.</p>
    </div>
  </div>
</section>

<section class="docs-grid">
  <h2>Documentation</h2>
  <div class="docs-cards">
    <a class="docs-card" href="{{ '/pages/get-started/' | relative_url }}">
      <h3>Get Started</h3>
      <p>Install, define a bean, run your first queries.</p>
    </a>
    <a class="docs-card" href="{{ '/pages/crud/' | relative_url }}">
      <h3>CRUD</h3>
      <p>Create, read, update, and delete with the active record API.</p>
    </a>
    <a class="docs-card" href="{{ '/pages/querying/' | relative_url }}">
      <h3>Querying</h3>
      <p>Filtering, ordering, joins, and aggregates.</p>
    </a>
    <a class="docs-card" href="{{ '/pages/migrations/' | relative_url }}">
      <h3>Migrations</h3>
      <p>Define and evolve your schema in code.</p>
    </a>
    <a class="docs-card" href="{{ '/pages/relationships/' | relative_url }}">
      <h3>Relationships</h3>
      <p>Load related records without N+1.</p>
    </a>
    <a class="docs-card" href="{{ '/pages/cheat-sheet/' | relative_url }}">
      <h3>Cheat Sheet</h3>
      <p>Every operation at a glance.</p>
    </a>
  </div>
</section>

<section class="hero-code">
  <h2>Sample Code</h2>

<div class="hero-code-inner" markdown="1">
<div class="hero-code-title">Todo.java</div>

```java
// ActiveRecord style classes map to tables, e.g. `todos`  
public class Todo extends PikaBean {
    private String title;
    private String description;
    private Boolean completed = false;

    public Todo() {}

    public void setTitle(String title)              { this.title = title; }
    public void setDescription(String description)  { this.description = description; }
    //  etc...

    // A standard typed entry point for queries against Todos
    public static PikaClassFinder<Todo> find() {
        return find(Todo.class);
    }

    // simple validation mechanism
    @Override
    protected void validation() {
        require("title");
    }
}


PikaORM orm = new PikaORM("jdbc:sqlite:app.db") // connect to a database
        .makeDefaultORM()                       // make this the standard connection
        .withMigrations(new AppMigrations())    // migrations keep your schema up to date
        .applyMigrations();

// PikaBeans support simple CRUD
Todo todo = new Todo();
todo.setTitle("Read the docs");
todo.setDescription("Finish the PikaORM guide");
todo.saveOrThrow();

// A nice fluent API for querying
Todo first = Todo.find().byId(1);
PikaList<Todo> active = Todo.find()
        .where("completed = :done", "done", false)
        .fetchList(); // convert to a list for in-memory work

// Drop down to raw SQL when you want to
PikaList<Todo> rows = orm.select("SELECT * FROM todos", Todo.class).fetchList();
```

</div>
</section>
