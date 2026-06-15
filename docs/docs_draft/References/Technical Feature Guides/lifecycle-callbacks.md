---
title: "Lifecycle Callbacks"
layout: default
---

# Lifecycle Callbacks

PikaORM allows your domain objects to hook into the database execution pipeline. You do this by implementing the `PikaRecordLifecycle` interface. 

*(Note: If your class extends `EnterprisePikaBean`, it implements this interface automatically, and you simply override the methods you need).*

## Available Hooks

The interface provides default implementations for all methods (returning `true` or doing nothing), so you only need to implement the specific hooks you care about.

```java
public interface PikaRecordLifecycle {
    
    // Returning false from any of these boolean methods 
    // silently aborts the operation.
    default boolean validate() { return true; }
    default boolean beforeInsert() { return true; }
    default boolean beforeUpdate(Map<String, Object> updateValues) { return true; }
    default boolean beforeDelete() { return true; }
    
    // Post-operation hooks
    default void afterInsert() {}
    default void afterSelect() {}
    default void afterUpdate() {}
    default void afterDelete() {}
}
```

## Execution Sequence

### INSERT Flow

1. `validate()`: Check business rules before any DB interaction.
2. `beforeInsert()`: Last chance to modify fields (e.g., setting a `createdAt` timestamp) or abort.
3. *PikaORM builds and executes the INSERT SQL statement.*
4. `afterInsert()`: Runs after the row is created and the primary key has been generated and populated on the object.

### UPDATE Flow

1. `validate()`
2. `beforeUpdate(Map updateValues)`: Runs right before the SQL is built. The map contains the exact key-value pairs that are about to be sent to the database. You can mutate this map to add/remove columns from the update payload.
3. *PikaORM builds and executes the UPDATE SQL statement.*
4. `afterUpdate()`

### DELETE Flow

1. `beforeDelete()`: Often used to manually cascade deletions to child records or prevent deletion based on business rules.
2. *PikaORM executes the DELETE SQL statement.*
3. `afterDelete()`

### SELECT Flow

1. *PikaORM executes the SELECT SQL statement and instantiates your object.*
2. `afterSelect()`: Runs immediately after PikaORM has finished mapping data from the `ResultSet` into your fields. This is commonly used to take a snapshot of the object's original state (which is how the dirty-field optimization works in `EnterprisePikaBean`).

## Abort Semantics

The pre-operation hooks (`validate`, `beforeInsert`, `beforeUpdate`, `beforeDelete`) all return a `boolean`. 

If you return `false` from any of these methods, PikaORM will **silently abort** the database operation. It will not execute the SQL, and the `orm.insert()`, `orm.update()`, or `orm.delete()` method will simply return `0` or `false` (indicating no rows were affected). No exception is thrown by the ORM.

If you want an exception to be thrown to halt an HTTP request or trigger a transaction rollback, you should explicitly throw a `RuntimeException` (or `IllegalStateException`) from within the hook.

```java
public class User implements PikaRecordLifecycle {
    
    private String username;

    @Override
    public boolean beforeInsert() {
        if (username == null || username.isBlank()) {
            // Throwing halts the entire transaction
            throw new IllegalArgumentException("Username is required");
        }
        return true; 
    }
}
```
