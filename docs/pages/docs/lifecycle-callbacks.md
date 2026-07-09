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

    @Override
    public boolean beforeInsert() {
        createdAt = LocalDateTime.now();   // stamp the row on the way in
        return true;
    }

    @Override
    public boolean beforeUpdate(Map<String, Object> values) {
        values.put("updated_at", LocalDateTime.now());   // add a column to this UPDATE
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
    default boolean beforeUpdate(Map<String, Object> values) { return true; }
    default boolean beforeDelete() { return true; }

    // post-operation
    default void afterInsert() {}
    default void afterSelect() {}
    default void afterUpdate() {}
    default void afterDelete() {}
}
```

## Order

- **Insert**: `validate` then `beforeInsert` then the SQL then `afterInsert` (the generated primary key is set by the time `afterInsert` runs).
- **Update**: `validate` then `beforeUpdate(values)` then the SQL then `afterUpdate`. The `values` map is the exact column payload about to be sent; mutate it to add or drop columns.
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
