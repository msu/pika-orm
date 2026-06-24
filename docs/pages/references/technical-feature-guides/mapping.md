---
layout: default
title: "Mapping & Coercion"
description: "How PikaORM maps Java classes to tables and columns, how to override the conventions, and how type coercion works."
active_page: mapping
permalink: /pages/mapping/
---

# Mapping & Coercion

PikaORM maps Java classes to tables and columns by convention. You only configure a mapping when your schema departs from those conventions or when a field holds a complex type.

## Default Conventions

When you do not define a mapping for a class, PikaORM applies these rules:

- **Class to table**: `ClassName` becomes `snake_case` and is pluralized. `User` -> `users`, `BlogPost` -> `blog_posts`, `Category` -> `categories`.
- **Field to column**: `fieldName` becomes `snake_case`. `firstName` -> `first_name`, `createdAt` -> `created_at`.
- **Primary key**: a field named exactly `id` (case-sensitive) is the primary key. Without it, PikaORM cannot update or delete by identity.
- **Foreign key**: for `.loadMany()`, the child table's FK column is the parent's singular table name plus `_id`. A `User` with many `Post`s expects a `user_id` column on `posts`.
- **UUID**: a field named `uuid` is treated as a UUID column. If null on `INSERT`, PikaORM generates `java.util.UUID.randomUUID().toString()`.
- **Version**: a field named `version` is treated as an optimistic-concurrency column and is auto-incremented on every `UPDATE`.

## Overriding the Defaults

### Global Overrides

If your whole database follows a different standard (for example a legacy schema with singular tables and `CamelCase` columns), override the defaults on the `PikaORM` builder before `.makeDefaultORM()`:

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

### Class-Specific Overrides

Any class can define a `public static Mapping mapping()` method. If present, PikaORM ignores the global conventions for that class and uses the returned `Mapping`. This keeps mapping logic encapsulated in the class it belongs to.

For a class you do not own (e.g. from a third-party library) and cannot add a static method to, register the mapping on the ORM instance:

```java
orm.withMapping(ThirdPartyClass.class, new Mapping() {
    @Override
    public String mapToTable() {
        return "third_party_table";
    }
    // ... override mapField as needed
});
```

## Custom Field Mapping

`mapToTable()` overrides the table name; `mapField(Field)` controls per-field mapping. Build a field mapping with `map(field)` and chain the `FieldMapping` methods:

```java
public class FieldMapping {
    // Core configuration methods
    FieldMapping toColumn(String columnName)           // Set custom column name
    FieldMapping asType(Class<?> dbClass)              // Set database storage type
    FieldMapping asId()                                // Mark as ID column
    FieldMapping asVersionColumn()                     // Mark as version column
    
    // Data transformation methods
    FieldMapping transformForDB(UnaryOperator<Object> func)    // Java -> Database
    FieldMapping transformFromDB(UnaryOperator<Object> func)   // Database -> Java
    FieldMapping withVersionIncrementer(UnaryOperator<Object> incrementer)
}
```

### Serializing a Complex Type

Use `asType()` plus `transformForDB` / `transformFromDB` to store a complex field in a single column. This stores a `Map` as a JSON string:

```java
import com.google.gson.Gson;
import edu.montana.pika.mapping.Mapping;
import edu.montana.pika.mapping.FieldMapping;

public class UserPreferences {

    private long userId;
    private Map<String, Object> settings;

    // Standard getters/setters...
    public long getUserId() { return userId; }
    public Map<String, Object> getSettings() { return settings; }
    
    public static Mapping mapping() {
        Gson gson = new Gson();
        
        return new Mapping() {
            @Override
            public String mapToTable() {
                return "user_prefs_table"; 
            }
            
            @Override
            public FieldMapping mapField(Field field) {
                return switch (field.getName()) {
                    case "userId" -> map(field)
                                        .toColumn("id")
                                        .asId();
                    
                    case "settings" -> map(field)
                                        .toColumn("json_settings")
                                        .asType(String.class)
                                        .transformForDB(gson::toJson)
                                        .transformFromDB(val -> gson.fromJson(String.valueOf(val), Map.class));
                    
                    default -> defaultMapping(field);
                };
            }
        };
    }
}
```

Notes on the pattern:

- Return an inline `new Mapping() { ... }` and override only `mapToTable` and `mapField`.
- Route fields with a `switch` on `field.getName()`. Use `ignore(field)` to skip a field.
- Always include a `default -> defaultMapping(field)` case so newly added simple fields map automatically.
- `transformForDB` / `transformFromDB` define how a value transitions between the Java type and the SQL type set by `asType()`.

### Collection Serialization

The same hooks serialize `List` or `Set` fields, e.g. as delimited strings. The mapping below also marks an optimistic-locking column with a custom incrementer:

```java
public static class MyMapping extends Mapping {
    @Override
    protected FieldMapping mapField(Field field) {
        switch (field.getName()) {
            case "tags":
                return map(field)
                    .toColumn("tags_csv")
                    .asType(String.class)
                    .transformForDB(this::serializeStringList)
                    .transformFromDB(this::deserializeStringList);
                    
            case "categoryIds":
                return map(field)
                    .toColumn("category_ids")
                    .asType(String.class)
                    .transformForDB(this::serializeLongList)
                    .transformFromDB(this::deserializeLongList);
                    
            case "keywords":
                return map(field)
                    .toColumn("keywords_set")
                    .asType(String.class)
                    .transformForDB(this::serializeStringSet)
                    .transformFromDB(this::deserializeStringSet);
                    
            case "version":
                return map(field)
                    .toColumn("version_num")
                    .asVersionColumn()
                    .withVersionIncrementer(v -> v == null ? 1 : ((Integer) v) + 1);
                    
            default:
                return defaultMapping(field);
        }
    }
    
    private String serializeStringList(Object obj) {
        if (obj == null) return null;
        List<String> list = (List<String>) obj;
        return String.join(",", list);
    }
    
    private List<String> deserializeStringList(Object csv) {
        if (csv == null || ((String) csv).trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(((String) csv).split(",")));
    }
    
    private String serializeLongList(Object obj) {
        if (obj == null) return null;
        List<Long> list = (List<Long>) obj;
        return list.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }
    
    private List<Long> deserializeLongList(Object csv) {
        if (csv == null || ((String) csv).trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(((String) csv).split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Long::valueOf)
            .collect(Collectors.toList());
    }
    
    private String serializeStringSet(Object obj) {
        if (obj == null) return null;
        Set<String> set = (Set<String>) obj;
        return String.join(",", set);
    }
    
    private Set<String> deserializeStringSet(Object csv) {
        if (csv == null || ((String) csv).trim().isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(((String) csv).split(",")));
    }
}
```

The ORM detects an inner `Mapping` class (or static `mapping()` method) automatically; insert and read the entity as usual:

```java
PikaORM orm = new PikaORM("jdbc:sqlite:web.db")
        .makeDefaultORM();

HasCustomizedMetadata entity = new HasCustomizedMetadata();
entity.setMap(Map.of("foo", 1.0, "bar", 2.0));

orm.insert(entity);

var retrieved = orm.find(HasCustomizedMetadata.class).byId(entity.getId());
```

## Coercion

JDBC drivers return values in their own Java types (e.g. an SQLite driver may return `Integer` where Postgres returns `Long`). PikaORM's coercion system converts each raw value into the type declared by your Java field via `coerce(Class<?> targetClass, Object value)`.

The pipeline runs in order:

1. **Null check**: a `null` value, or an empty string `""` targeting a non-String field, returns `null`.
2. **Custom coercers**: each coercer registered with `withCoercion()` is tried; the first non-null result wins.
3. **Passthrough**: if the value is already an instance of `targetClass`, it is returned untouched.
4. **Enums**: the value is stringified, upper-cased, and passed to `Enum.valueOf()`.
5. **Strings**: returns `String.valueOf(value)`.
6. **Numerics**: coerces to `Short`, `Integer`, `Long`, `Float`, `Double`, `BigInteger`, or `BigDecimal`.
7. **Temporals**: parses strings or epoch numbers into `LocalDate`, `LocalDateTime`, or `java.util.Date`.
8. **Booleans**: `null`, `0`, `"0"`, and `"false"` resolve to `false`; any other non-null value to `true`.

If every step fails, it throws an `IllegalArgumentException`.

### Custom Coercers

Inject your own coercion logic globally with `withCoercion(BiFunction)`. Return `null` to defer to the next coercer:

```java
import java.util.Currency;

PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .withCoercion((targetClass, rawValue) -> {
        if (targetClass == Currency.class) {
            return Currency.getInstance(String.valueOf(rawValue));
        }
        return null; 
    })
    .makeDefaultORM();
```

### sloppyCoerce()

`sloppyCoerce()` is a tolerant variant: if normal coercion throws and the input is not already a String, it converts the value to a `String` and runs the pipeline again. `EnterprisePikaBean.setFieldsFrom(Map)` uses it internally to bind raw HTTP form parameters into typed fields without fatal casting exceptions.
