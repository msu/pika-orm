---
layout: default
title: "How It Works"
description: "How PikaORM uses Java reflection to map plain objects to rows and back, with no annotations."
active_page: how-it-works
permalink: /pages/how-it-works/
---

# How It Works

PikaORM has no annotations and no config files. It works out how to map your classes to tables at runtime, using Java reflection. This page shows the core of how. You do not need it to use Pika, but if you are learning how an ORM is built, start here.

## The idea

An ORM has two jobs: turn an object into a row to save it, and turn a row back into an object to read it. With no annotations to consult, Pika inspects the class itself:

- to write a row, it reads each field's value off your object;
- to read a row, it makes a new object and sets each field from a column.

Both directions use `java.lang.reflect`.

## Reading an object's fields

To build an `INSERT` or `UPDATE`, Pika needs the values in your object. It lists the fields and reads each one:

```java
for (Field field : object.getClass().getDeclaredFields()) {
    field.setAccessible(true);          // reach private fields, no getter needed
    Object value = field.get(object);   // the value to put in the SQL
}
```

It also walks up the superclass chain, so fields inherited from `PikaBean` come along. `setAccessible(true)` is why your fields, getters, and setters can all be private or absent: Pika reads the field directly.

## Making an object from a row

To map a query result, Pika needs a blank instance to fill. It finds your no-arg constructor and calls it:

```java
Constructor<?> ctor = clazz.getDeclaredConstructor();
ctor.setAccessible(true);
Object row = ctor.newInstance();        // a fresh, empty object
```

This is why every bean needs a no-arg constructor (it may be private). Then Pika sets each field from its column:

```java
field.setAccessible(true);
field.set(row, value);                  // write the column value into the field
```

The raw column value runs through the [Coercion System]({{ '/pages/mapping/' | relative_url }}) first, so a JDBC `Integer` lands cleanly in your `Long` field.

## Names come from conventions

Reflection gives Pika each field's name. Conventions turn that name into a column (`dueDate` becomes `due_date`) and the class name into a table (`Todo` becomes `todos`). That convention layer is the whole "mapping", which is why there are no annotations. You override it in [Mapping & Coercion]({{ '/pages/mapping/' | relative_url }}).

## It is pluggable

All of the reflection sits behind a small `Reflector` interface: read a field, set a field, make an instance. `StandardReflector` is the default implementation.

```java
public interface Reflector {
    Object get(Field field, Object from);
    void set(Field field, Object object, Object value);
    Object make(Constructor constructor, Object[] args);
}
```

```java
public class StandardReflector implements Reflector {
    public Object get(Field field, Object from)        { return field.get(from); }
    public void   set(Field field, Object o, Object v) { field.set(o, v); }
    public Object make(Constructor c, Object[] args)   { return c.newInstance(args); }
}
```

(Exception handling trimmed for clarity.)

## Where to look in the source

- `mapping/Reflector.java` and `mapping/StandardReflector.java` -- the reflection calls.
- `mapping/Mapping.java` -- field discovery and instantiation.
- `mapping/FieldMapping.java` -- per-field read/write and the name-to-column mapping.
