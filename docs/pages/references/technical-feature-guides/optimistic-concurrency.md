---
layout: default
title: "Optimistic Concurrency Control"
description: "PikaORM Optimistic Concurrency: prevent lost updates and race conditions using version column tracking."
active_page: optimistic-concurrency
permalink: /pages/optimistic-concurrency/
---

# Optimistic Concurrency Control

In concurrent applications, two users might try to edit the same record at the same time. If they both load the record, modify it, and save it, the second save will blindly overwrite the first user's changes.

PikaORM prevents this using **Optimistic Concurrency Control (OCC)**.

## How it Works

When OCC is active for a class, PikaORM expects the database table to have an integer column that tracks the version of the row.

1. You load the record (e.g., `version = 1`).
2. You modify the record.
3. You tell PikaORM to update it. PikaORM generates an SQL statement like this:
   ```sql
   UPDATE table SET ..., version = 2 WHERE id = ? AND version = 1
   ```
4. If another process updated the record between steps 1 and 3, the `version` in the database will be `2`. The `WHERE version = 1` clause will fail to match any rows.
5. PikaORM notices that 0 rows were updated and returns `false` from `orm.update()`. If you are using `EnterprisePikaBean`'s `.saveOrThrow()`, it throws a `ConcurrentModificationException`.

## Enabling OCC

By default, PikaORM looks for a field named exactly `version` on your domain object. If it finds one, OCC is automatically enabled for that class.

```java
public class Article {
    private long id;
    private String title;
    
    // The presence of this field automatically enables OCC
    private Integer version;
    
    // getters and setters...
}
```

When you insert a new `Article`, PikaORM sets the version to `1`. Every time you update it, PikaORM increments the version.

## Customizing the Version Column

If your database uses a different column name for tracking versions (e.g., `opt_lock`), you can override the default globally:

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .withDefaultVersionColumnName("opt_lock")
    .makeDefaultORM();
```

Alternatively, you can customize it per-class using the Custom Mapping Pattern:

```java
public static Mapping mapping() {
    return new Mapping() {
        @Override
        public FieldMapping mapField(Field field) {
            if (field.getName().equals("optLock")) {
                return map(field)
                    .toColumn("opt_lock")
                    .asVersionColumn() // Marks this field as the OCC tracker
                    .withVersionIncrementer(v -> v == null ? 1 : ((Integer) v) + 1);
            }
            return defaultMapping(field);
        }
    };
}
```

## Disabling OCC

If you have a field named `version` that is meant for something else (like an API version string) and you do not want PikaORM to use it for concurrency control, you can opt-out globally:

```java
PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .withNoDefaultVersionColumn()
    .makeDefaultORM();
```
