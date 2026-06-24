---
layout: default
title: "Cheat Sheet"
description: "Every common PikaORM operation at a glance, bean-first."
active_page: cheat-sheet
permalink: /pages/cheat-sheet/
---

# Cheat Sheet

Every common operation at a glance. `Todo` is a `PikaBean` with a static `find()`; `orm` is your `PikaORM` instance.

## Define a bean

| | |
|---|---|
| Bean | `public class Todo extends PikaBean { ... }` |
| Query entry point | `public static PikaClassFinder<Todo> find() { return find(Todo.class); }` |

## Create, update, delete

| Task | Code |
|---|---|
| Insert | `todo.save()` or `todo.insert()` (returns the generated id) |
| Insert many | `orm.insertAll(a, b, c)` |
| Update | `todo.save()` or `todo.update()` |
| Delete | `todo.delete()` |

## Read

| Task | Code |
|---|---|
| By primary key | `Todo.find().byId(1L)` |
| By any column | `Todo.find().byKey("title", "Read the docs")` |
| All rows | `Todo.find().all().toList()` |
| First match | `Todo.find().firstWhere("completed = :d", "d", false)` |

## Query

| Task | Code |
|---|---|
| Filter | `Todo.find().where("completed = :d", "d", false).fetchList()` |
| IN / LIKE / BETWEEN | `Todo.find().byQuery().whereIn("priority", ids).fetchList()` |
| Order | `Todo.find().where(...).orderBy("priority", DESC).fetchList()` |
| Aggregate | `Todo.find().count()`, `Todo.find().avg("priority")` |
| Join | `Todo.find().join(Project.class).where("projects.name = :n", "n", "Docs").fetchList()` |
| Page | `Todo.find().where(...).page(2).pageSize(20).fetchList()` |
| Page totals | `query.totalCount()`, `query.totalPages()`, `query.hasNextPage()` |
| Raw SQL | `orm.select("SELECT ...", Todo.class).toList()` |

## Relationships (accessors on the bean)

| Task | Code |
|---|---|
| One-to-many | `loadMany(Album.class)` |
| Many-to-many | `loadManyThrough(PlaylistTrack.class, Track.class)` |
| Belongs-to | `load(Artist.class)` |
| One-to-one reverse | `loadReverse(Profile.class)` |

## Transactions

| Task | Code |
|---|---|
| Run in a transaction | `orm.inTransaction(() -> { ... })` |
| Returning a value | `var x = orm.inTransaction(() -> { ...; return v; })` |
| Require a transaction | `orm.joinTransaction(() -> { ... })` |
| Separate connection | `orm.forceTransaction(() -> { ... })` |

## Setup

| Task | Code |
|---|---|
| Connect | `new PikaORM("jdbc:sqlite:app.db").makeDefaultORM()` |
| Migrations | `.withMigrations(new AppMigrations()).applyMigrations()` |
| Connection pool | `new PikaORM(pool::getConnection)` |
| Caching | `orm.startQueryCaching()` / `orm.endQueryCaching()` |
| Logging | `.logQueries().withLogLevel(PikaLogger.Level.INFO)` |
