---
layout: default
title: "N+1 Query Avoidance"
description: "PikaORM N+1 pattern: bulk loading with WHERE IN, query caching, and JOIN-based solutions to avoid N+1 queries."
active_page: n-plus-1
permalink: /pages/n-plus-1-avoidance/
---

# N+1 Query Avoidance Pattern

The **N+1 Query Problem** is the most common performance issue in applications using an ORM. It occurs when you fetch a list of N objects, and then loop over that list to load a relationship for each object, resulting in 1 query for the initial list, plus N additional queries.

## The Problem

```java
// 1 query to get 50 artists
PikaList<Artist> artists = orm.query(Artist.class).page(1).pageSize(50).fetchList();

for (Artist artist : artists) {
    // 50 separate queries to get their albums! (Total: 51 queries)
    PikaList<Album> albums = artist.getAlbums(); 
    System.out.println(artist.getName() + " has " + albums.size() + " albums");
}
```

If you render a table of 50 artists on a webpage, making 51 database round-trips will severely degrade performance.

## Solution 1: Query Caching (The Easy Way)

If you have multiple components on a page requesting the same relationships, wrapping the request in PikaORM's Query Cache will ensure each relation is loaded at most once per parent.

```java
orm.startQueryCaching();
try {
    for (Artist artist : artists) {
        // Still 51 queries the first time, but if another component 
        // asks for the same albums, it hits the in-memory cache.
        PikaList<Album> albums = artist.getAlbums();
    }
} finally {
    orm.endQueryCaching();
}
```
*Note: This does not solve the N+1 problem for the initial load, but it prevents it from becoming 2N+1 or worse.* See the [Query Caching Pattern](/pages/query-caching-pattern/) for more details.

## Solution 2: Bulk Loading with WHERE IN (The Performant Way)

To truly solve N+1, you must reduce the number of queries. The most effective pattern in PikaORM is to extract the IDs of the parent objects and load all children in a single `WHERE IN` query.

```java
// 1. Fetch 50 artists
PikaList<Artist> artists = orm.query(Artist.class).page(1).pageSize(50).fetchList();

// 2. Extract their IDs into a List
List<Long> artistIds = new ArrayList<>();
for (Artist artist : artists) {
    artistIds.add(artist.getId());
}

// 3. Fetch ALL related albums in a single query (The "+1" query is gone!)
PikaList<Album> allAlbums = orm.query(Album.class)
    .whereIn("artist_id", artistIds)
    .fetchList();

// 4. (Optional) Group the albums in memory if you need to access them by artist
Map<Long, List<Album>> albumsByArtist = new HashMap<>();
for (Album album : allAlbums) {
    albumsByArtist.computeIfAbsent(album.getArtistId(), k -> new ArrayList<>()).add(album);
}

// Now you can iterate over artists and use the in-memory map
for (Artist artist : artists) {
    List<Album> myAlbums = albumsByArtist.getOrDefault(artist.getId(), Collections.emptyList());
    System.out.println(artist.getName() + " has " + myAlbums.size() + " albums");
}
```

This reduces the database round-trips from **51** to exactly **2**, regardless of how many artists you are loading.

## Solution 3: Using JOINS

If you only need a specific flattened view of the data (e.g., Artist Name and Album Title), using a `.join()` is often the most direct approach:

```java
PikaList<Album> albums = orm.query(Album.class)
    .join(Artist.class) // Join albums -> artists
    .fetchList();
    
for (Album album : albums) {
    // Both album and artist data are available from the single query result
    System.out.println(album.getTitle() + " by Artist ID " + album.getArtistId());
}
```
