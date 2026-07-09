---
layout: default
title: "Mapping & Coercion"
description: "How PikaORM maps classes to tables, how to override the conventions, and how to coerce database values into your field types."
active_page: mapping
permalink: /pages/mapping/
---

# Mapping & Coercion

Pika maps classes to tables and columns by convention. You only write a mapping when your schema breaks from those conventions or a field holds a type the database does not store directly.

## Default conventions

With no mapping defined, Pika applies these rules:

- **Class to table**: the class name becomes `snake_case` and is pluralized. `User` to `users`, `BlogPost` to `blog_posts`.
- **Field to column**: the field name becomes `snake_case`. `firstName` to `first_name`.
- **Primary key**: a field named exactly `id` is the primary key. Without one, Pika cannot update or delete by identity.
- **Foreign key**: the child's FK column is the parent's singular table name plus `_id`. A `User` with many `Post`s expects `user_id` on `posts`.
- **UUID**: a field named `uuid` gets a generated `UUID.randomUUID().toString()` on insert if it is null.
- **Version**: a field named `version` is an optimistic-concurrency column, bumped on every update.

## Override the conventions

### For the whole database

If your schema follows a different standard everywhere, override the defaults on the builder. Each takes a function, so the rule can depend on the class or field.

```java
PikaORM orm = new PikaORM("jdbc:mysql://localhost/legacy_db")
    .withDefaultTableMapping(clazz -> clazz.getSimpleName())   // singular, exact class name
    .withDefaultColumnMapping(field -> field.getName())        // exact Java field name
    .withDefaultIdField(clazz -> "primaryKey")                 // PK field name
    .withDefaultFkColumn(clazz -> clazz.getSimpleName().toLowerCase() + "Id")
    .makeDefaultORM();
```

### For one class

Give the class a `public static Mapping mapping()`. When it is present, Pika uses it instead of the conventions for that class, keeping the mapping next to the fields it describes.

For a class you do not own, register the mapping on the ORM instead:

```java
orm.withMapping(ThirdPartyClass.class, "third_party_table");
```

## Custom field mapping

Inside a `Mapping`, override `mapField(Field)` and build each field's mapping with `map(field)`. The `FieldMapping` methods:

- `toColumn(name)` -- use a specific column name.
- `asType(Class)` -- store the field as a different type (usually `String.class`).
- `asId()` / `asVersionColumn()` -- mark the primary key or version column.
- `transformForDB(fn)` / `transformFromDB(fn)` -- convert the value on the way out and back in.

Route fields with a `switch`, and always end with `defaultMapping(field)` so new simple fields keep mapping automatically. Use `ignore(field)` to skip one.

### Storing a complex type in one column

`asType(String.class)` plus the two transforms store any field as a single column. Here a `Map` is kept as JSON:

```java
import com.google.gson.Gson;
import edu.montana.pika.mapping.Mapping;
import edu.montana.pika.mapping.FieldMapping;

public class UserPrefs {
    long userId;
    Map<String, Object> settings;

    public static Mapping mapping() {
        Gson gson = new Gson();
        return new Mapping() {
            @Override public String mapToTable() { return "user_prefs"; }

            @Override public FieldMapping mapField(Field field) {
                return switch (field.getName()) {
                    case "userId"   -> map(field).toColumn("id").asId();
                    case "settings" -> map(field)
                            .toColumn("json_settings")
                            .asType(String.class)
                            .transformForDB(gson::toJson)
                            .transformFromDB(v -> gson.fromJson(String.valueOf(v), Map.class));
                    default -> defaultMapping(field);
                };
            }
        };
    }
}
```

The same pattern serializes a `List` or `Set`, for example joining a `List<String>` into a comma-separated column with `transformForDB(v -> String.join(",", (List<String>) v))` and splitting it back in `transformFromDB`.

## Coercion

When you read a row, JDBC drivers hand back values in their own Java types (one driver returns `Integer` where another returns `Long`). Coercion converts each raw value into the type your field declares. It runs in this order, first match wins:

1. `null`, or an empty string targeting a non-string field, becomes `null`.
2. Each coercer you registered with `withCoercion()`, in order.
3. The value is already the target type: returned as is.
4. Enums, strings, numerics, temporals (`LocalDate`, `LocalDateTime`, `Date`), booleans.

If nothing matches, it throws `IllegalArgumentException`.

### Custom coercers (and LocalDate)

Your coercers run before the built-ins, so you can override how a type is read. Return `null` to defer to the next one.

Pika reads `LocalDate` from ISO strings like `2026-01-31` already. If a column uses another format, register a coercer:

```java
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

DateTimeFormatter US = DateTimeFormatter.ofPattern("MM/dd/yyyy");

PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .withCoercion((targetType, raw) -> {
        if (targetType == LocalDate.class && raw instanceof String s) {
            return LocalDate.parse(s, US);
        }
        return null;   // not ours, let Pika's defaults handle it
    })
    .makeDefaultORM();
```

Coercion only covers the database-to-Java direction. To control how the `LocalDate` is *written*, set the column type and transforms in the field mapping. Together these are the full round trip for a custom format:

```java
case "birthday" -> map(field)
        .asType(String.class)
        .transformForDB(d -> ((LocalDate) d).format(US))
        .transformFromDB(s -> LocalDate.parse((String) s, US));
```

### sloppyCoerce()

`sloppyCoerce()` is the forgiving version: if normal coercion fails and the value is not already a string, it stringifies the value and tries once more. `PikaBean.setFieldsFrom()` uses it to bind raw form parameters into typed fields without blowing up on a bad cast.
