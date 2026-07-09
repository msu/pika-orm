---
layout: default
title: "Optimistic Concurrency"
description: "Prevent lost updates in PikaORM with a version column and optimistic concurrency control."
active_page: optimistic-concurrency
permalink: /pages/optimistic-concurrency/
---

# Optimistic Concurrency

Two users load the same row, both edit it, both save. With no protection the second save silently overwrites the first. Optimistic concurrency control (OCC) catches that.

## How it works

Give the table an integer `version` column. Pika scopes every update to the version it read:

```sql
UPDATE posts SET ..., version = 2 WHERE id = ? AND version = 1
```

If someone else changed the row first, its version is already 2, so `WHERE version = 1` matches nothing and the update touches 0 rows. Pika sees that and `update()` returns `false`. (Through `saveOrThrow()`, a failed save throws `IllegalStateException`.)

## Turn it on

Add a field named `version`. That alone enables OCC for the class.

```java
public class Article extends PikaBean {
    long id;
    String title;
    Integer version;   // the presence of this field turns on OCC
}
```

Pika sets `version` to 1 on insert and bumps it on every update. Handle a conflict by checking the return value:

```java
Article a = Article.find().byId(1L);
a.setTitle("edited");
if (!a.update()) {
    // someone changed this row first: reload and retry, or tell the user
}
```

## Customize the column

For a different column name everywhere:

```java
new PikaORM("jdbc:sqlite:app.db")
    .withDefaultVersionColumnName(clazz -> "opt_lock")
    .makeDefaultORM();
```

Or per class, in the [mapping]({{ '/pages/mapping/' | relative_url }}):

```java
@Override
public FieldMapping mapField(Field field) {
    if (field.getName().equals("optLock")) {
        return map(field)
                .toColumn("opt_lock")
                .asVersionColumn()
                .withVersionIncrementer(v -> v == null ? 1 : ((Integer) v) + 1);
    }
    return defaultMapping(field);
}
```

## Turn it off

If you have a `version` field that means something else (an API version, say), opt out so Pika leaves it alone:

```java
new PikaORM("jdbc:sqlite:app.db")
    .withNoDefaultVersionColumn()
    .makeDefaultORM();
```
