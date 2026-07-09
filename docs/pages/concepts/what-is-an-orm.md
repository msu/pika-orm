---
layout: default
title: "What is an ORM?"
description: "Learn what an Object Relational Mapper does, why it exists, and how PikaORM puts it into practice."
active_page: what-is-an-orm
permalink: /pages/what-is-an-orm/
---

# What is an ORM?

An **ORM**, or **Object Relational Mapper**, is a tool that acts as a bridge between your application's code (written in Java) and your database (which speaks SQL).

## The Problem ORMs Solve

In a relational database, data is stored in tables and rows. In an object-oriented language like Java, data is represented as classes and objects. These two systems are fundamentally different. 

When you read a row from the database using raw SQL, you get back raw tabular data. To use it in your application, you have to manually extract each column and construct your Java objects.

### Without an ORM (Raw JDBC)

To fetch a user and turn it into a Java object without an ORM, you have to manage the connection, write the SQL string, map the parameters, execute the query, and manually map the result set to your object fields:

```java
public User getUserById(long id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        pstmt.setLong(1, id);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            return user;
        }
        return null;
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}
```

This code is tedious, repetitive, and prone to errors (like misspelling a column name in the `getString` call).

### With an ORM (PikaORM)

An ORM handles the boilerplate for you. It knows how your `User` class corresponds to the `users` table, so it generates the SQL, executes it, and populates the object automatically.

```java
public User getUserById(long id) {
    return orm.find(User.class).byId(id);
}
```

The ORM dynamically translates the method call into the appropriate SQL query, runs it against the database, and maps the resulting row back into an instance of your `User` class.

## Why use an ORM?

- **Reduced Boilerplate**: You write significantly less code. The ORM handles connections, statements, and result sets.

- **Type Safety**: Your code deals with typed Java objects (`User`, `Product`) rather than untyped strings and raw database primitives.

- **Refactoring**: If you change a field name in your Java class, modern IDEs can refactor the code automatically. 

- **Security**: ORMs automatically parameterize queries, which protects your application against SQL injection attacks.

## When to use Raw SQL vs. the ORM

While ORMs are fantastic for standard CRUD (Create, Read, Update, Delete) operations, they can struggle with highly complex queries, such as massive reporting queries involving many joins, aggregations, or database-specific features.

PikaORM is designed to give you the best of both worlds. You use the simple object-oriented methods for 95% of your work, but when you need to write a complex custom query, you can execute raw SQL directly through the ORM and still have it map the results to your Java objects.

## ORM Criticisms

ORM's are critiqued for a number of problems they create for bloated SQL querying, anti-design pattern philosophy, and complexity issues. The largest problems generally discussed are the [N+1 problem](https://stackoverflow.com/questions/97197/what-is-the-n1-selects-problem-in-orm-object-relational-mapping), as well as the [Object-Relational-Impedence-Mismatch](https://en.wikipedia.org/wiki/Object%E2%80%93relational_impedance_mismatch), which are important to note when using any ORM technology. The goal of this project was to minimize the impact of these pitfalls within Pika, so that it may be a truly pragmatic ORM. 

We [address the N+1 problem within its own example document]({{ '/pages/n-plus-1-avoidance' | relative_url  }}) if you are interested in avoiding the worse kinds of querying bloating. 

## Next Steps

Ready to see how PikaORM puts this into practice? Jump into [Get Started]({{ '/pages/get-started/' | relative_url }}) to install it and run your first queries.
