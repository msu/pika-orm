# PikaORM

> Ah yes, just what we need: another ORM...

PikaORM is an [object-relational mapping tool](https://en.wikipedia.org/wiki/Object%E2%80%93relational_mapping) for Java.

PikaORM is designed differently than most other Java ORM tools:

* It does not require any code-generation
* It works with [POJOs](https://en.wikipedia.org/wiki/Plain_old_Java_object)
* It is configured entirely in code, no config files
* It doesn't hide the underlying SQL from you
* The source is in one file, and can be copied-and-pasted into your project if you wish to tweak or fork it

## Quick Start

Here is an example of PikaORM in action:

```java
// a POJO model class
class MyModel {
    long id;
    String str;
}

public static void main(String[] args) {
    // create an ORM with a connection string
    var orm = new PikaORM("jdbc:sqlite:demo.db");

    // create a new model object
    var model = new MyModel();
    model.str = "Hello PikaORM";

    // save it to the database, get the resulting generated id
    var id = orm.insert(model);

    // load the model from db by id
    var fromDb = orm.find(MyModel.class).byId(id);

    // print out "Hello PikaORM"
    System.out.println(fromDb.str);
}
```

The `MyModel` class in this example is a POJO and doesn't know
anything about the backing database.

In this case the schema for the table that holds this POJO would look like this:

```sql
CREATE TABLE my_models (
    id      INTEGER PRIMARY KEY,
    str_val VARCHAR NOT NULL
);
```

## Configuring PikaORM

### Default Mapping

* `ClassName` -> `class_names`
* `fieldName` -> `field_name`
* `id` -> id column/field name
* `<table_name>_id` -> foreign key column/field name
* `version` -> optimistic concurrency column/field name

### Logging

## CRUD with POJOS

## CRUD with EnterprisePikaBeans

### Validation in EnterprisePikaBeans

## Raw Queries

### PikaQueryBuilder

### Defining Custom Query Records

## Transactions In PikaORM

## Migrations

## Using PikaORM with a Web Framework