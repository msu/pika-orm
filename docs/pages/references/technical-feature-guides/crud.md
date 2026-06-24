---
layout: default
title: "CRUD"
description: "Define a PikaBean and create, read, update, and delete records with the active record API."
active_page: crud
permalink: /pages/crud/
---

# CRUD

A `PikaBean` is a plain class that maps to a table and saves itself. Extend it, add fields, and you get `insert()`, `find()`, `update()`, and `delete()` against the default ORM.

Set the default ORM once at startup with `makeDefaultORM()` (see [Get Started]({{ '/pages/get-started/' | relative_url }})). Every bean uses it.

## Define a bean

```java
import edu.montana.pika.bean.PikaBean;
import edu.montana.pika.query.PikaClassFinder;

public class User extends PikaBean {
    Long id;
    String name;
    String email;

    public User() {}

    public void setName(String name)   { this.name = name; }
    public void setEmail(String email) { this.email = email; }

    // Typed entry point for queries. One line of boilerplate per bean.
    public static PikaClassFinder<User> find() {
        return find(User.class);
    }
}
```

Fields map to columns by convention: `User` to table `users`, `email` to column `email`. Pika reads and writes fields directly, so getters and setters are optional (add them for your own code).

## Create

Make a new bean, set its fields, and call `insert()`. Pika runs an `INSERT` and hands you back the generated primary key. It also sets the `id` field on the object.

```java
User u = new User();
u.setName("Ada");
u.setEmail("ada@example.com");

long id = u.insert();   // INSERT, returns the generated key (and sets u.id)
```

## Read

`find()` is your entry point for queries. The two everyday reads are by primary key and by some other column:

```java
User u   = User.find().byId(1L);                          // by primary key
User ada = User.find().byKey("email", "ada@example.com"); // by any column
PikaList<User> all = User.find().all().toList();          // every row
```

`byId` and `byKey` return a single bean, or `null` if nothing matches. `all()` gives you every row. For filtering, ordering, and joins, see [Querying]({{ '/pages/querying/' | relative_url }}).

## Update

Load a bean, change it, and call `update()`. Pika writes an `UPDATE` that targets the row by its primary key.

```java
User u = User.find().byId(1L);
u.setName("Ada Lovelace");
u.update();             // UPDATE by primary key
```

Pika remembers the values the bean had when you loaded it and only writes the columns you actually changed. If nothing changed, `update()` does no SQL and returns `false`.

## Delete

Load a bean and call `delete()`. Pika deletes the row by its primary key.

```java
User u = User.find().byId(1L);
u.delete();             // DELETE by primary key
```

## save(): insert or update

When you do not want to track whether an object is new, call `save()`. It runs `insert()` for a fresh object and `update()` for one that came from the database.

```java
User u = new User();
u.setName("Grace");
u.save();               // new object -> INSERT

u.setName("Grace Hopper");
u.save();               // already persisted -> UPDATE
```

Reach for `save()` when you do not want to track which case you are in; use `insert()` / `update()` when you do.

Validating a bean before it saves and binding web form data onto it are covered in [Validation & Forms]({{ '/pages/validation/' | relative_url }}). Loading related records is covered in [Relationships]({{ '/pages/relationships/' | relative_url }}).
