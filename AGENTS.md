# AGENTS.md

## Collection method names

`PikaIterable`, `PikaList`, `PikaMap` and `PikaSet` share code with the Notch collections. PikaORM uses different names for some methods.

- Do not add `groupBy`. In an ORM, `groupBy` means SQL `GROUP BY`.
- If you port changes from Notch, keep the new behavior. Use the PikaORM names in this table.

| Notch | PikaORM | Returns |
|---|---|---|
| `groupBy(fn)` | `toMap(fn)` | `PikaMap<K, PikaList<T>>` |
| `groupBy(fn, cmp)` | `toOrderedMap(fn, cmp)` | `PikaMap<K, PikaList<T>>` (sorted) |
| `toMap(fn)` | `toDistinctMap(fn)` | `PikaMap<K, T>` |
| `toMap(keyFn, valueFn)` | `toDistinctMap(keyFn, valueFn)` | `PikaMap<K, V>` |
| `toMap(fn, cmp)` | `toOrderedDistinctMap(fn, cmp)` | `PikaMap<K, T>` (sorted) |

`toOrderedMap(fn)` and `toOrderedDistinctMap(fn)` use the natural order of the keys.
