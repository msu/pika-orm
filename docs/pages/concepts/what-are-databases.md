---
layout: default
title: "What are Databases?"
description: "A beginner's guide to understanding databases, tables, SQL, and how they relate to PikaORM."
active_page: what-are-databases
permalink: /pages/what-are-databases/
---

# What in the world is a Database?

Fundamentally, the idea of storing data requires neat organization and space. Everything is data, and rarely do objects in a system not have a relationship to one another.

> Take a moment without thinking too hard about an environment or system you use every day. Could be your job, the grocery store, a school, etc. Think about the relationships between things. In a grocery store, food is organized by similar sections: dry foods and wet foods are grouped together respectively. In school, you have students grouped by classes, organized by last names, who are then further grouped by an assigned teacher and subject. General organization tells us that it is useful to group similar things, and group things by relation. 

This thought example is the exact process behind database theory. Sparing any highly technical discussion, the implementation of most databases is shockingly analog to how most humans intuitively try to group things within a given system, with some syntactic and computational flavoring. This idea led to the creation of DBMS (Database Management Systems), which are queried mostly using a language called SQL (Structured Query Language). SQL is the language behind manipulating data within a database. 

Before we dive into DBMS and SQL, here are two general prefaces:

- SQL is not the only style for working with DBMS, and there are multitudes of different types of database systems for different kinds of organizational problems. We are diving into SQL because it lends itself well to understanding how databases work generally, and because it is the most commonly used system that serves as the backbone of most ORM (Object Relational Mapping) technology, and thus transitively PikaORM.
- If you are seeking an explanation for *how* databases work internally, you will be slightly disappointed by this guide, as this is intended to pragmatically understand the usage of a database. If you would like to learn more, Wikipedia does a great job with an in-depth overview and is a solid [starting place](https://en.wikipedia.org/wiki/Database).

Let's dive in.

## How databases organize data

Databases structure information to make it easy to find, update, and manage. In a relational database, data is stored in **tables**.

- **Tables**: Think of a table as a spreadsheet. Each table holds a specific category of information (e.g., `users`, `products`, `orders`).
- **Rows (Records)**: Each row in a table represents a single item or record. In a `users` table, one row represents one specific user.
- **Columns (Fields)**: Columns define the attributes of the records. A `users` table might have columns for `first_name`, `last_name`, and `email`.
- **Primary Keys**: Every row needs a unique identifier so the database can distinguish it from others, even if two users have the same name. This unique identifier is called the primary key (typically an `id` column).
- **Foreign Keys**: To represent relationships between data, tables use foreign keys. An `orders` table might have a `user_id` column. This column stores the primary key of a user, linking the order to the specific person who made it.

## What is SQL? How and why is it used?

SQL (Structured Query Language) is the standard language used to interact with relational databases. It allows you to perform operations on the data stored in the tables.

The core operations in SQL are often referred to as **CRUD** (Create, Read, Update, Delete):

1. **Create (INSERT)**: Adds new rows to a table.
   ```sql
   INSERT INTO users (first_name, email) VALUES ('Jane', 'jane@example.com');
   ```

2. **Read (SELECT)**: Retrieves data from one or more tables.
   ```sql
   SELECT first_name, email FROM users WHERE email = 'jane@example.com';
   ```

3. **Update (UPDATE)**: Modifies existing rows.
   ```sql
   UPDATE users SET first_name = 'Janet' WHERE id = 1;
   ```

4. **Delete (DELETE)**: Removes rows from a table.
   ```sql
   DELETE FROM users WHERE id = 1;
   ```

## What to get a real tutorial?

[Check out our recommend guide for learning SQL through SQLite (A smaller in memory and effecient SQL variant)](https://www.sqltutorial.org/)

## So I get SQL. What is an ORM?

Now that you understand databases and SQL, you might wonder how this fits into writing a Java application. That is exactly what an ORM handles.

Head over to [What is an ORM?]({{ '/pages/what-is-an-orm/' | relative_url }}) to learn how Object Relational Mapping connects your Java code to your database.