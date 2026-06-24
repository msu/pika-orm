---
layout: default
title: "N+1 Avoidance"
description: "Spot and fix the N+1 query problem in PikaORM with caching, bulk WHERE IN loading, or a join."
active_page: n-plus-1
permalink: /pages/n-plus-1-avoidance/
---

# N+1 Avoidance

N+1 is the classic ORM performance trap: one query to load a list, then one more query per item to load a relationship. Fifty artists turns into fifty-one round trips.

## The problem

```java
PikaList<Artist> artists = Artist.find().page(1).pageSize(50).fetchList();   // 1 query

for (Artist artist : artists) {
    PikaList<Album> albums = artist.getAlbums().toList();   // +1 query each, 50 more
    System.out.println(artist.getName() + ": " + albums.size());
}
```

Render that as a table and you have made 51 trips to the database for one page.

## Cache repeated loads

If the same relationship gets asked for more than once in a request (several page components, a template that loops twice), [Caching]({{ '/pages/caching/' | relative_url }}) loads each one at most once:

```java
orm.startQueryCaching();
try {
    for (Artist artist : artists) {
        artist.getAlbums().toList();   // loaded once per artist, then served from memory
    }
} finally {
    orm.endQueryCaching();
}
```

This does not remove the first load of each relationship, so it is still N+1 on a cold cache. It stops repeat work, not the initial fan-out.

## Bulk-load with WHERE IN

To actually collapse the query count, load every child in one query. Grab the parent ids, fetch all their albums with an `IN`, then group them in memory:

```java
PikaList<Artist> artists = Artist.find().page(1).pageSize(50).fetchList();   // 1

List<Long> ids = new ArrayList<>();
for (Artist a : artists) ids.add(a.getId());

PikaList<Album> albums = Album.find().byQuery()
        .whereIn("artist_id", ids)
        .fetchList();                                                        // 1 more

Map<Long, List<Album>> byArtist = new HashMap<>();
for (Album album : albums) {
    byArtist.computeIfAbsent(album.getArtistId(), k -> new ArrayList<>()).add(album);
}

for (Artist artist : artists) {
    List<Album> theirs = byArtist.getOrDefault(artist.getId(), List.of());
    System.out.println(artist.getName() + ": " + theirs.size());
}
```

Two queries total, no matter how many artists. This is the workhorse fix.

## Join for a flat view

If you just need the rows flattened together (artist name next to album title), a [join]({{ '/pages/querying/' | relative_url }}) does it in one query:

```java
PikaList<Album> albums = Album.find()
        .join(Artist.class)
        .fetchList();
```
