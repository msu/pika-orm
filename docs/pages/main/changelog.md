---
layout: default
title: "Changelog"
description: "Release history and changelog for PikaORM."
active_page: changelog
permalink: /pages/changelog/
---

# Change Log
> A full track of changes/release for PikaORM. For the most up-to-date view, or to see individual code changes, please reference the [Github Page](https://github.com/msu/pika-orm)
> 
> For a list of dependencies and versions please refer to our official [Maven Package](https://central.sonatype.com/artifact/edu.montana.cs.pika/pika-orm/versions) page. 

## V0.1.2

- Fix: limit and offset values no longer get locale grouping separators. Before this fix, an offset of 1000 or more gave SQL such as `OFFSET 1,000`, and the database rejected it. Custom clauses from `withOffsetClause(...)` also get the fix.
- Add: `PikaORM.formatLimitOffset(limit, offset)` formats the limit/offset clause.
- Fix: enum coercion and the `join(String)` keyword check now use `Locale.ROOT`. Before this fix, a Turkish default locale changed `i` to a dotted capital I. Enum values such as `active` did not resolve, and `join("join ...")` got a second `JOIN`.

## V0.1.1

- Fix: `getErrors(field)`, `getErrorString(field)` and `getGeneralErrors()` no longer add an empty entry to the error map. Before this fix, `hasError(field)` and `hasErrors()` returned true after these calls.

## V1.0.0

- PikaORM publicly launched! 
- First iteration documentation released

> [!WARNING]
>
> This is the first public release of PikaORM for public usage so bear with us. Please report any bugs or documentation inaccuracies with a new issue and we will attempt to promptly fix, thanks :3

