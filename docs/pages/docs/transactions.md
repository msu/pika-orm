---
layout: default
title: "Transactions"
description: "Run work in a transaction with PikaORM: nesting, isolated transactions, and required transactions."
active_page: transactions
permalink: /pages/transactions/
---

# Transactions

Wrap work in `inTransaction()`. Pika commits when the block finishes and rolls back if it throws. You do not touch the connection, commit, or rollback yourself.

## Run in a transaction

Pass a block that returns nothing, or one that returns a value:

```java
orm.inTransaction(() -> {
    new User("Alice").save();
    new User("Bob").save();
});   // commits here; if either save threw, neither row is written

User admin = orm.inTransaction(() -> {
    User u = new User("Charlie");
    u.save();
    new Role("ADMIN").save();
    return u;
});
```

If the block throws, Pika rolls back everything in it and rethrows the exception.

## Nesting

Call `inTransaction()` inside another and the inner block joins the outer one. Pika counts the nesting and issues a single commit when the outermost block finishes. One failure anywhere rolls back the whole thing.

```java
orm.inTransaction(() -> {            // opens the transaction
    foo.save();

    orm.inTransaction(() -> {        // joins it, no separate commit
        bar.save();
    });

});                                  // foo and bar commit together
```

## Escaping the current transaction

`forceTransaction()` runs on its own connection and ignores any transaction on the thread. It commits on its own, so its work survives even when the surrounding transaction rolls back. Reach for it when something must persist no matter what, like an audit log.

```java
orm.inTransaction(() -> {
    foo.save();

    orm.forceTransaction(() -> {
        new AuditLog("created foo").save();   // commits immediately, on its own connection
    });

    throw new RuntimeException();   // rolls back foo; the audit log stays
});
```

## Requiring a transaction

`joinTransaction()` runs its block only if a transaction is already open, and throws `IllegalStateException` if one is not. Use it in helper methods that must never run on their own.

```java
public void ship(Order order) {
    orm.joinTransaction(() -> {
        order.setStatus("SHIPPED");
        order.save();
    });
}
```

## Checking status

```java
boolean active = orm.isInTransaction();
```
