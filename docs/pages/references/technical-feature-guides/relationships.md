---
layout: default
title: "Relationships"
description: "PikaORM Relationships: loadMany, loadManyThrough, load, and loadReverse for One-to-Many, Many-to-Many, and Belongs-To."
active_page: relationships
permalink: /pages/relationships/
---

# Relationships

PikaORM handles relationships without requiring complex annotations. Relationships are resolved dynamically at runtime by reading your Java objects and using standard foreign key conventions.

There are four primary relationship methods available on the `PikaORM` instance. If your class extends `PikaBean`, these methods are also available directly on the object.

## One-to-Many (`loadMany`)

Used when one object is the parent of many child objects. For example, an `Artist` has many `Album`s. PikaORM assumes the `albums` table has a foreign key column named `artist_id`.

```java
Artist artist = orm.find(Artist.class).byId(1L);

// Returns a relation object, does NOT immediately query the database
PikaManyRelation<Album> albumsRelation = orm.loadMany(artist, Album.class);

// Triggers the query: SELECT * FROM albums WHERE artist_id = 1
List<Album> albums = albumsRelation.toList();
```

### `PikaManyRelation` Methods

The `PikaManyRelation` object provides several utilities to manage the relationship without writing manual UPDATE/INSERT queries.

- **`toList()`**: Executes the query and returns the children.
- **`size()`**: Executes a `SELECT count(*)` query to return the number of children.
- **`add(Album)`**: Sets the `artist_id` on the given album to the parent's ID, but does not save it.
- **`addAndSave(Album)`**: Sets the foreign key and immediately inserts/updates the child in the database.
- **`create()`**: Instantiates a new `Album` with the `artist_id` already populated.
- **`toQuery()`**: Returns a `PikaClassQuery<Album>` pre-filtered by the foreign key, allowing you to add additional `.where()` or `.orderBy()` clauses before fetching.

## Many-to-Many (`loadManyThrough`)

Used when two entities are related via a join table. For example, a `Playlist` has many `Track`s through a `PlaylistTrack` join table.

```java
Playlist playlist = orm.find(Playlist.class).byId(1L);

PikaManyThroughRelation<PlaylistTrack, Track> tracksRelation = 
    orm.loadManyThrough(playlist, PlaylistTrack.class, Track.class);

// SELECT tracks.* FROM tracks 
// JOIN playlist_tracks ON tracks.id = playlist_tracks.track_id 
// WHERE playlist_tracks.playlist_id = 1
List<Track> tracks = tracksRelation.toList();
```

### `PikaManyThroughRelation` Methods

- **`toList()`**: Executes the JOIN query and returns the child objects.
- **`size()`**: Executes a count query.
- **`add(Track)`**: Creates and returns a new join-table object (`PlaylistTrack`) linking the playlist and track, but does not save it.
- **`addAndSave(Track)`**: Creates the join-table object and immediately saves it to the database.
- **`remove(Track)`**: Deletes the join-table row that links the two objects.
- **`toQuery()`**: Returns a `PikaClassQuery<Track>` pre-configured with the necessary JOINs.

## Belongs-To (`load`)

Used when an object holds the foreign key to another object. For example, an `Album` has an `artist_id` and belongs to an `Artist`.

```java
Album album = orm.find(Album.class).byId(1L);

// Reads album.getArtistId() and fetches the corresponding Artist
Artist artist = orm.load(album, Artist.class);
```

## Has-One Reverse (`loadReverse`)

Used when the foreign key is on the other table, but it is a 1-to-1 relationship rather than 1-to-N.

```java
User user = orm.find(User.class).byId(1L);

// Looks for a UserProfile where user_id = 1
UserProfile profile = orm.loadReverse(user, UserProfile.class);
```

## Avoiding N+1 Queries

Relationship methods execute a database query immediately upon fetching the results (`.toList()`). If you loop over 50 artists and call `.loadMany(artist, Album.class).toList()`, you will execute 50 queries.

To learn how to bulk-load relationships effectively in PikaORM, read the [N+1 Query Avoidance]({{ '/pages/n-plus-1-avoidance/' | relative_url }}) pattern guide.
