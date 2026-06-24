---
layout: default
title: "Migrations"
description: "Define and evolve your database schema in Java with PikaORM migrations and the migration console."
active_page: migrations
permalink: /pages/migrations/
---

# Migrations

You define your schema in Java, not in a loose pile of `.sql` files. Each change is a migration with an `up` (apply it) and a `down` (roll it back). Pika tracks which migrations have run, so applying them is safe to do on every startup.

## Define migrations

Extend `Migrations`, override `migrations()`, and register each change with `add()`. They run in the order you add them.

```java
import edu.montana.pika.migrations.Migrations;
import edu.montana.pika.migrations.PikaMigration;

public class AppMigrations extends Migrations {

    @Override
    public void migrations() {
        add(this::createUsers);
        add(this::addEmailToUsers);
    }

    public PikaMigration createUsers() {
        return makeMigration("001_create_users")
                .up("""
                    CREATE TABLE IF NOT EXISTS users (
                        id   INTEGER PRIMARY KEY,
                        name TEXT
                    );
                    """)
                .down("DROP TABLE users;");
    }

    public PikaMigration addEmailToUsers() {
        return makeMigration("002_add_email")
                .up("ALTER TABLE users ADD COLUMN email TEXT;")
                .down("ALTER TABLE users DROP COLUMN email;");
    }
}
```

A migration is a unique name, an `up` block, a `down` block, and an optional `description()`. You write the SQL; Pika runs it. Put several statements in one block by separating them with semicolons, and Pika runs them together in a single transaction.

Watch your target database's limits. SQLite, for example, cannot `DROP COLUMN`, so that second `down` only works where the database supports it.

## Apply on startup

Register your migrations on the ORM and call `applyMigrations()`. Pika runs every migration that has not run yet, in order.

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
        .withMigrations(new AppMigrations())
        .applyMigrations();
```

The first time it runs, Pika creates a `pika_migrations` table and records each migration's name as it applies. On later runs it skips anything already recorded, so calling `applyMigrations()` on every startup is safe.

## The console

Auto-applying on startup is convenient but blunt. While developing, boot the interactive console instead and step through migrations by hand.

```java
public static void main(String[] args) {
    PikaORM orm = new PikaORM("jdbc:sqlite:app.db").makeDefaultORM();

    AppMigrations migrations = new AppMigrations();
    orm.withMigrations(migrations);
    migrations.console();   // takes over stdin and waits for commands
}
```

```text
migrations > show
[APPLIED] 001_create_users
[PENDING] 002_add_email

migrations > up
Applying: 002_add_email
```

| Command | What it does |
|---------|--------------|
| `show` | List every migration and whether it is applied or pending. |
| `all` | Apply all pending migrations, in order. |
| `up` | Apply the next pending migration only. |
| `down` | Roll back the most recently applied migration (runs its `down`). |
| `help` | List the commands. |
| `exit` / `quit` | Leave the console. |
