# GrugORM

GrugORM is an [object-relational mapping tool](https://en.wikipedia.org/wiki/Object%E2%80%93relational_mapping) for Java.

It is designed to:

* Be relatively light-weight
* Be implemented in a single file
* Be understandable by the average Java developer
* Not force any particular pattern of development on you

GrugORM is configured entirely in code, rather than using config files.  If you wish to configure GrugORM via
properties files or whatever, you can write that code yourself.

## Quick Start

Here is an example of GrugORM in action:

```java
// a POJO model class
class MyModel {
    long id;
    String str;
}

public static void main(String[] args) {
    // create an ORM with a connection string
    var orm = new GrugORM("jdbc:sqlite:demo.db");

    // create a new model object
    var model = new MyModel();
    model.str = "Hello GrugORM";

    // save it to the database, get the resulting generated id
    var id = orm.insert(model);

    // load the model from db by id
    var fromDb = orm.find(MyModel.class, id);

    // print out "Hello GrugORM"
    System.out.println(fromDb.str);
}
```

The `MyModel` class in this example is a [POJO](https://en.wikipedia.org/wiki/Plain_old_Java_object) and doesn't know
anything about the backing database.

In this case the schema for the table that holds this POJO would look like this:

```sql
CREATE TABLE my_model(
    id      INTEGER PRIMARY KEY,
    str_val VARCHAR NOT NULL
);
```
