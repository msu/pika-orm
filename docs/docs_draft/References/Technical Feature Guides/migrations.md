---
title: "Migrations"
layout: default
---

# Migrations

PikaORM includes a built-in migration engine. It allows you to define and manage your database schema entirely in Java code, ensuring that your database structure evolves consistently alongside your application logic.

## Defining Migrations

Migrations are defined by creating a class that extends `edu.montana.pika.migrations.Migrations`. You override the `migrations()` method and call `add()` to register discrete units of work.

```java
import edu.montana.pika.migrations.Migrations;
import edu.montana.pika.migrations.PikaMigration;

public class AppMigrations extends Migrations {

    @Override
    public void migrations() {
        // Order matters! Migrations run in the order they are added.
        add(this::createUsersTable);
        add(this::addEmailToUsers);
    }

    public PikaMigration createUsersTable() {
        return makeMigration("001_create_users")
                .description("Creates the initial users table")
                .up("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY,
                        name TEXT
                    );
                    """)
                .down("DROP TABLE users;");
    }

    public PikaMigration addEmailToUsers() {
        return makeMigration("002_add_email")
                .up("ALTER TABLE users ADD COLUMN email TEXT;")
                .down("ALTER TABLE users DROP COLUMN email;"); // Note: SQLite does not support DROP COLUMN
    }
}
```

Each migration has:
- A unique **name** (used to track if it has been applied).
- An **up** block: The SQL to execute when applying the migration.
- A **down** block: The SQL to execute when rolling back the migration.
- An optional **description**.

*Note: You can execute multiple SQL statements inside a single `up` or `down` block by separating them with semicolons. They will be executed sequentially within a single transaction.*

## Applying Migrations

You register your migrations class with the ORM during startup.

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .withMigrations(new AppMigrations())
    .applyMigrations(); // Automatically runs all pending migrations
```

### The `pika_migrations` Tracking Table

When you call `applyMigrations()`, PikaORM automatically creates a table called `pika_migrations` in your database. 

It checks the `name` of every registered migration against this table. If a migration is not in the table, it is marked as `PENDING`. PikaORM then iterates through the pending migrations in order, executes their `up` SQL within a transaction, and inserts a record into `pika_migrations` marking it as `APPLIED`.

Because of this tracking table, calling `applyMigrations()` is **idempotent** and safe to run on every application startup.

## Interactive Migration Console

During development, automatically running migrations on startup can be dangerous if you make a mistake. PikaORM includes an interactive CLI to give you granular control.

```java
public static void main(String[] args) {
    PikaORM orm = new PikaORM("jdbc:sqlite:app.db").makeDefaultORM();
    
    AppMigrations migrations = new AppMigrations();
    orm.withMigrations(migrations);
    
    // Boot the console instead of starting the web server
    migrations.console(); 
}
```

When you run this code, it hijacks `System.in` and presents a prompt:

```text
migrations > show
[APPLIED] 001_create_users - Creates the initial users table
[PENDING] 002_add_email

migrations > up
Applying: 002_add_email
Successfully applied migration.

migrations > down
Rolling back: 002_add_email
Successfully rolled back migration.

migrations > all
Applying: 002_add_email
Successfully applied migration.
```

### Console Commands

| Command | Action |
|---------|--------|
| `show`  | Tabular display of all defined migrations and their current status (`APPLIED` or `PENDING`). |
| `all`   | Applies all pending migrations in order. |
| `up`    | Applies only the *next* single pending migration. |
| `down`  | Executes the `down` SQL for the most recently applied migration and removes it from the tracking table. |
| `exit` / `quit` | Terminates the console session. |
