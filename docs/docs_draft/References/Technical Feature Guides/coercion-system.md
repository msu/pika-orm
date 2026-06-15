---
title: "Coercion System"
layout: default
---

# Coercion System

JDBC drivers return data in their own specific Java types (e.g., an SQLite driver might return a `java.lang.Integer` while a Postgres driver returns a `java.lang.Long`). PikaORM uses a Coercion System to convert the raw database value into the specific type declared by your Java field.

## The Coercion Pipeline

When mapping a result set to a Java object, PikaORM passes every value through its `coerce(Class<?> targetClass, Object value)` pipeline.

The pipeline executes in this order:

1. **Null Check**: If the database value is `null`, or if it is an empty string `""` targeting a non-String field, it immediately returns `null`.
2. **Custom Coercers**: It loops through any custom coercers you have registered via `withCoercion()`. If one returns a non-null result, that result is used.
3. **Passthrough**: If the raw value is already an instance of the `targetClass`, it passes it through untouched.
4. **Enums**: If the target class is an `Enum`, it attempts to convert the database value to a string, upper-cases it, and calls `Enum.valueOf()`.
5. **Strings**: If the target class is `String`, it returns `String.valueOf(value)`.
6. **Numerics**: It coerces the value (usually converting to a string intermediate) into `Short`, `Integer`, `Long`, `Float`, `Double`, `BigInteger`, or `BigDecimal`.
7. **Temporals**: It converts strings or epoch numbers into `LocalDate`, `LocalDateTime`, or `java.util.Date` using a flexible multi-format parser.
8. **Booleans**: It resolves `null`, `0`, `"0"`, and `"false"` to `false`. Any other non-null value is coerced to `true`.

If all steps fail, it throws an `IllegalArgumentException`.

## Custom Coercions

If your application uses specific value types (e.g., JodaTime dates, or custom `Currency` objects stored as strings), you can inject your own coercion logic into the pipeline.

You do this globally during ORM setup using `withCoercion(BiFunction)`:

```java
import java.util.Currency;

PikaORM orm = new PikaORM("jdbc:sqlite:app.db")
    .withCoercion((targetClass, rawValue) -> {
        // We only care about coercing TO a Currency object
        if (targetClass == Currency.class) {
            return Currency.getInstance(String.valueOf(rawValue));
        }
        
        // Return null to tell PikaORM to move to the next coercer in the pipeline
        return null; 
    })
    .makeDefaultORM();
```

## `sloppyCoerce()`

PikaORM has a tolerant variant of the coercion pipeline called `sloppyCoerce()`. 

If the primary coercion fails (throws an exception) and the input value is not already a String, `sloppyCoerce` converts the raw value to a `String` and runs the entire pipeline again.

This is extremely useful for **Web Form Binding**. When you use `EnterprisePikaBean.setFieldsFrom(Map)`, it internally uses `sloppyCoerce` to safely parse raw HTTP string parameters into your strongly-typed Java fields without throwing fatal casting exceptions.
