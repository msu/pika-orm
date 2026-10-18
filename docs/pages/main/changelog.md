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

## Unreleased

- Fix: a field set in `beforeInsert()` is now in the INSERT. Before this fix, Pika read the fields before the hook ran.
- Breaking: `beforeUpdate(Map<String, Object>)` is now `beforeUpdate()`. Pika reads the fields after the hook, so a field set in `beforeUpdate()` is in the UPDATE. To add a column, set its field.
- Change: `PikaORM.update()` removes the unchanged columns of a `PikaBean` after `beforeUpdate()`. An override of `beforeUpdate()` no longer turns off this removal.
- Change: `insertAll` calls `validate()` and `beforeInsert()` for each item, and skips an item if a hook returns `false`. It makes UUIDs for UUID columns. It does not call `afterInsert()`, and does not set the generated ids.
- Fix: `PikaBean.getOriginalValue(Field)` and `getOriginalValue(String)` accept a field name. Before this fix, they accepted only a column name. A column name still works.

## V0.1.3

- Add: `PikaMap` and `PikaSet` collection classes.
- Add: `PikaMap.onMiss(value)` and `PikaMap.onMiss(supplier)` return a view with a default for missing keys. A miss does not change the map.
- Add: new `PikaIterable` methods: `flatMap`, `filter(Class)`, `filterOutNull`, `skip`, `take`, `reverse`, `unique`, `uniqueBy`, `sorted`, `sortedBy`, `tap`, `firstOrThrow()`, `last`, `lastOrThrow`, `firstWhereOrThrow`, `indexOf`, `fold`, `all`, `asMap`, `toDistinctMap(keyFn, valueFn)` and `toString(prefix, delim, suffix)`.
- Add: new `PikaList` methods: `get(index, default)`, `get(index, supplier)`, `getLast`, `removeLast` and `concat`.
- Change: `toMap` and `toOrderedMap` return `PikaMap<K, PikaList<T>>`. `toDistinctMap` and `toOrderedDistinctMap` return `PikaMap<K, T>`. `toSet` returns `PikaSet<T>`.
- Change: `toDistinctMap` keeps keys in insertion order.
- Breaking: if code assigns the result of `toMap` or `toOrderedMap` to `Map<K, List<T>>` or `TreeMap`, it does not compile. Use `var` or `PikaMap<K, PikaList<T>>`. If code assigns the result of `toOrderedDistinctMap` to `TreeMap`, use `PikaMap`.

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

