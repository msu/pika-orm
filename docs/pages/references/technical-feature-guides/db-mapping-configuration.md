---
layout: default
title: "Database Mapping Configuration"
description: "PikaORM default conventions for class-to-table, field-to-column, primary keys, foreign keys, UUID, and version columns."
active_page: db-mapping-configuration
permalink: /pages/db-mapping-configuration/
---

# Database Mapping Configuration

PikaORM embraces "convention over configuration." Out of the box, it assumes your database schema follows standard SQL naming conventions, while your Java code follows standard Java conventions. PikaORM automatically translates between the two.

## Default Conventions

When you do not explicitly define a mapping for a class, PikaORM uses the following rules:

### 1. Class-to-Table Mapping
The Java `ClassName` is converted to `snake_case` and then pluralized.
- `User` -> `users`
- `BlogPost` -> `blog_posts`
- `Category` -> `categories`

### 2. Field-to-Column Mapping
The Java `fieldName` is converted to `snake_case`.
- `firstName` -> `first_name`
- `createdAt` -> `created_at`
- `isActive` -> `is_active`

### 3. Primary Key Convention
PikaORM looks for a field exactly named `id` (case-sensitive) to act as the primary key. If your class does not have an `id` field, PikaORM will not know how to update or delete the record by identity.

### 4. Foreign Key Convention
When resolving relationships using `.loadMany()`, PikaORM assumes the foreign key column on the child table is the parent's singular table name plus `_id`.
- If a `User` has many `Post`s, PikaORM expects the `posts` table to have a `user_id` column.

### 5. UUID Convention
If a field is named `uuid`, PikaORM assumes it is a UUID column. If the field is null during an `INSERT`, PikaORM will automatically generate a `java.util.UUID.randomUUID().toString()` for it.

### 6. Version Convention
If a field is named `version`, PikaORM assumes it is an optimistic concurrency control column. It will automatically increment this integer field on every `UPDATE`.

## Global Mapping Overrides

If your entire database uses a different convention (for example, if you are connecting to a legacy database where tables are singular and columns are `CamelCase`), you can override PikaORM's defaults globally at startup.

You configure these overrides on the `PikaORM` builder before calling `.makeDefaultORM()`.

```java
PikaORM orm = new PikaORM("jdbc:mysql://localhost/legacy_db")
    // Override table naming: keep singular, exact class name
    .withDefaultTableMapping(clazz -> clazz.getSimpleName())
    
    // Override column naming: just use the exact Java field name
    .withDefaultColumnMapping(field -> field.getName())
    
    // Override the default primary key field name
    .withDefaultIdField("primaryKey")
    
    // Override the default foreign key generation
    .withDefaultFkColumn(parentClass -> parentClass.getSimpleName().toLowerCase() + "Id")
    
    // Override default version column name
    .withDefaultVersionColumnName("opt_lock_version")
    
    // Opt-out of UUID auto-generation entirely
    .withNoDefaultUUIDField()
    
    .makeDefaultORM();
```

## Class-Specific Overrides

Global overrides are useful when the entire database follows a different standard. However, if you only need to customize the mapping for a single class (or need to map complex data types like JSON or collections), you should use the **Custom Mapping Pattern**.

Any class can define a `public static Mapping mapping()` method. If this method is present, PikaORM completely ignores the global conventions for this class and uses your defined `Mapping` object instead.

See the [Custom Field Mapping](/pages/custom-field-mapping/) guide for details on building class-specific mappings.

### External Classes

If you are trying to map a class that you do not own (e.g., from a third-party library) and therefore cannot add a static `mapping()` method to it, you can register a mapping for it directly on the ORM instance:

```java
orm.withMapping(ThirdPartyClass.class, new Mapping() {
    @Override
    public String mapToTable() {
        return "third_party_table";
    }
    // ... override mapField as needed
});
```
