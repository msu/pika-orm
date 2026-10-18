---
layout: default
title: "Lifecycle Callbacks"
description: "Run code at each step of a record's insert, update, delete, and select with PikaORM lifecycle hooks."
active_page: lifecycle-callbacks
permalink: /pages/lifecycle-callbacks/
---

# Lifecycle Callbacks

Hooks let an object run code at each step of its own insert, update, delete, and select. A `PikaBean` already implements them, so override only the ones you want. The classic use is timestamps:

```java
public class Post extends PikaBean {
    String title;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Override
    public boolean beforeInsert() {
        createdAt = LocalDateTime.now();   // stamp the row on the way in
        return true;
    }

    @Override
    public boolean beforeUpdate() {
        updatedAt = LocalDateTime.now();   // a field set here is in the UPDATE
        return true;
    }
}
```

(If you are not using `PikaBean`, implement `PikaRecordLifecycle` directly. It is the same interface.)

## The hooks

Every method has a default, so you implement only what you need.

```java
public interface PikaRecordLifecycle {
    // pre-operation: return false to abort
    default boolean validate() { return true; }
    default boolean beforeInsert() { return true; }
    default boolean beforeUpdate() { return true; }
    default boolean beforeDelete() { return true; }

    // post-operation
    default void afterInsert() {}
    default void afterSelect() {}
    default void afterUpdate() {}
    default void afterDelete() {}
}
```

## Order

- **Insert**: `validate` then `beforeInsert` then the SQL then `afterInsert`. Pika reads the fields after `beforeInsert`, so a field set in `beforeInsert` is in the INSERT. The generated primary key is set when `afterInsert` runs.
- **Bulk insert** (`insertAll`): `validate` then `beforeInsert` for each item, then one SQL statement. If a hook returns `false`, Pika skips that item. Pika does not call `afterInsert`, and does not set the generated ids.
- **Update**: `validate` then `beforeUpdate` then the SQL then `afterUpdate`. Pika reads the fields after `beforeUpdate`, so a field set in `beforeUpdate` is in the UPDATE. For a `PikaBean`, Pika then removes the columns that did not change.
- **Delete**: `beforeDelete` then the SQL then `afterDelete`.
- **Select**: the SQL then `afterSelect`, which runs once fields are mapped (this is where `PikaBean` snapshots state for dirty tracking).

## Aborting

Return `false` from any pre-operation hook and Pika silently skips the operation: no SQL runs, and `insert` / `update` / `delete` report no rows affected. Nothing is thrown.

To actually stop the request or roll back a transaction, throw instead:

```java
@Override
public boolean beforeDelete() {
    if (locked) throw new IllegalStateException("cannot delete a locked post");
    return true;
}
```

For plain field checks, use [Validation & Forms]({{ '/pages/validation/' | relative_url }}). Reach for hooks when you need timestamps, cascades, or logic tied to one specific step.
