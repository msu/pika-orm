---
layout: default
title: "CRUD"
description: "Define a PikaBean and create, read, update, and delete records with the active record API."
active_page: crud
permalink: /pages/crud/
---

# CRUD

PikaORM provides an optional active-record base class called `PikaBean`.

While you can use PikaORM with plain Java classes by passing them into `orm.insert(obj)` and `orm.update(obj)`, extending `PikaBean` wires the domain object directly to the default `PikaORM.get()` instance. This allows the object to manage its own persistence, validation, and lifecycle.

## Key Features

- **Active Record Methods**: Call `.save()`, `.insert()`, `.update()`, or `.delete()` directly on the object.
- **Smart Save**: `save()` automatically chooses whether to `insert` or `update` based on the bean's internal persistence state.
- **Change Tracking (Dirty-field optimization)**: Tracks original values and only updates columns that have actually changed.
- **Built-in Validation**: Field-level error handling using `require()` and `requireUnique()`.
- **Web Form Binding**: Safely bind string maps from web requests directly to fields using `setFieldsFrom()`.

## Basic Usage

### Creating an PikaBean

```java
import edu.montana.pika.bean.PikaBean;

public class User extends PikaBean {
    private String name;
    private String email;
    private Integer age;
    
    // Default constructor is required
    public User() {}
    
    // getters and setters...
    
    @Override
    protected void validation() {
        // Built-in validation helpers
        require("name");
        require("email");
        requireUnique("email");
        
        // Custom validation logic
        if (age != null && age < 18) {
            addError("age", "User must be 18 or older");
        }
    }
}
```

### CRUD Operations

```java
// Create new record
User user = new User();
user.setName("John Doe");
user.setEmail("john@example.com");

if (user.validate()) {
    // Automatically uses PikaORM.get()
    Long id = user.insert(); // Returns generated ID
}

// Alternatively, use save() which figures out if it's new
user.save();

// Update existing record
User existing = User.find(User.class).byId(1L);
existing.setName("Jane Doe");
existing.save(); // Automatically calls update() since it was loaded from the DB

// Delete record
existing.delete();
```

## Advanced Features

### Dirty-Field Optimization

When an PikaBean is loaded from the database via a SELECT query, PikaORM takes a snapshot of its fields (`originalValues`).

When you call `update()` or `save()`, PikaORM's `beforeUpdate` lifecycle hook compares the current field values against the `originalValues` map. Any fields whose values are identical are stripped from the `UPDATE` payload. This drastically reduces the size of SQL statements and prevents overwriting changes made by other processes to unrelated columns.

If no fields have changed, `update()` returns `false` and skips the database round-trip entirely.

### Validation and `saveOrThrow()`

PikaBean contains a built-in error map.

```java
User user = new User();
user.setEmail("invalid-email");

if (!user.validate()) {
    if (user.hasErrors()) {
        System.out.println("Validation failed: " + user.getErrorString("email"));
    }
}
```

If you prefer exceptions over boolean checks (e.g., in a web framework where an exception handler returns a 400 Bad Request), use `saveOrThrow()`:

```java
User user = new User();
user.setName(""); // Fails the require("name") validation

// Throws IllegalStateException if validation fails
user.saveOrThrow(); 
```

### Web Form Binding (`setFieldsFrom`)

When processing form submissions, you often have a `Map<String, String[]>`, `Map<String, String>`, or a form-data provider. Extracting these and parsing strings to integers or dates is tedious.

PikaBean provides `setFieldsFrom()` to securely bind map values to your object using PikaORM's Coercion System:

```java
// Simulated web request form data
Map<String, Object> formData = Map.of(
    "name", "Alice",
    "age", "25", // String that needs to become an Integer
    "isAdmin", "true"
);

User user = new User();

// ONLY bind the specific fields we permit.
// This prevents Mass Assignment Vulnerabilities (e.g., if the user tried 
// to inject "isAdmin=true", it is ignored because it's not in the allowlist).
user.setFieldsFrom(formData, "name", "age");

user.saveOrThrow();
```

### Relationship Loading

PikaBean provides shortcuts to PikaORM's relationship methods:

```java
// Instead of orm.loadMany(this, Order.class)
PikaManyRelation<Order> orders = user.loadMany(Order.class);

// Many-to-many
PikaManyThroughRelation<UserRole, Role> roles = 
    user.loadManyThrough(UserRole.class, Role.class);

// Belongs-to / single relation
Company company = user.load(Company.class);
```
