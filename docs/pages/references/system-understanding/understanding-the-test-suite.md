---
layout: default
title: "Understanding the Test Suite"
description: "How to use the PikaORM test suite as an executable reference for complex API usage patterns and multi-dialect testing."
active_page: understanding-test-suite
permalink: /pages/understanding-test-suite/
---

# Understanding the Test Suite

PikaORM has a comprehensive test suite that serves two purposes:
1. Validating the framework's correctness across multiple database engines.
2. Acting as a reference for complex API usage patterns.

If you want to see exactly how a feature is meant to be used, reading the tests is the best way to do so. And allow users to understand the architecture to modularly add their own features if they wish.

## Test Suite Organization

The test suite is organized by feature area under `src/test/java/edu/montana/pika/`:

| Package / Test | Description |
|---|---|
| `TestBase` | Base class handling database initialization and cross-dialect testing. |
| `integration.ChinookBeanTest` | Massive end-to-end test using the standard Chinook database schema. Validates complex queries, relationships, and EPB. |
| `relationships.*` | Tests for `OneToNTest` and `ManyToManyTest`. |
| `query.*` | Tests for the `PikaClassQuery` builder, caching, and raw SQL queries. |
| `mapping.*` | Tests for default mappings, coercion logic, and `ColumnsSpec`. |
| `migrations.*` | Tests validating the migration engine and interactive console. |

## The `TestBase` Class and Database Modes

Most test classes extend `TestBase`. This base class exposes a `DatabaseMode` enum that allows the test suite to run the exact same tests against multiple underlying database engines.

The supported modes in `TestBase.DatabaseMode` are:
- `SQLITE` (Default)
- `H2` (In-memory)
- `H2_POSTGRES` (PostgreSQL dialect)
- `H2_SQLSERVER` (SQL Server dialect)
- `H2_ORACLE` (Oracle dialect)
- `MARIADB` (Requires a local MariaDB instance)

### How `initTestDb` Works

Tests initialize the ORM by calling `initTestDb(String... ddl)`. This method performs several critical setup steps:

1. Translates the provided DDL (which is written in SQLite dialect) into the target dialect (e.g., replacing `INTEGER PRIMARY KEY` with `SERIAL PRIMARY KEY` for Postgres).
2. Clears out the previous database state.
3. Instantiates `PikaORM`.
4. Sets the logging level to `TRACE`.
5. Executes the DDL.
6. Returns the configured ORM instance.

Example usage in a test:
```java
@Test
void myFeatureTest() {
    // 1. Initialize DB with the schema
    var orm = initTestDb(SampleModel.DDL);
    
    // 2. Perform test...
    orm.insert(new SampleModel("test", 1, true, new Date()));
}
```

## Common Test Entities

The test suite uses a few standardized classes for testing:

1. **`SampleModel`**: A simple POJO with basic fields (String, Integer, Boolean, Date). It includes a static `DDL` constant for easy table creation.
2. **`SampleEgb`**: An `EnterprisePikaBean` subclass used to test the Active Record pattern, lifecycle hooks, and dirty-field optimization.
3. **Chinook Entities**: The `integration` package uses standard Chinook database entities (e.g., `Artist`, `Album`, `Track`) to test complex relationships and foreign key resolution.
