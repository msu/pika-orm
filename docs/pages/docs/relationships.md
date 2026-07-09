---
layout: default
title: "Relationships"
description: "Model one-to-many, many-to-many, belongs-to, and one-to-one relationships with PikaORM."
active_page: relationships
permalink: /pages/relationships/
---

# Relationships

Pika resolves relationships at runtime from foreign key conventions, with no annotations or mapping files to write. You expose a relationship by adding an accessor to your bean that calls one of four helpers: `loadMany`, `loadManyThrough`, `load`, or `loadReverse`.

If you are not using beans, the same methods exist on the ORM instance (`orm.loadMany(artist, Album.class)`). See [Plain Java Objects]({{ '/pages/plain-objects/' | relative_url }}).

## One-to-many (loadMany)

An `Artist` has many `Album`s. Pika expects the `albums` table to carry an `artistId` foreign key. Add an accessor that names the child type:

```java
public class Artist extends PikaBean {
    Long id;
    String name;

    public PikaManyRelation<Album> getAlbums() {
        return loadMany(Album.class);
    }
}
```

`getAlbums()` returns a relation, not a list, and does not touch the database until you ask for results:

```java
Artist artist = Artist.find().byId(1L);
PikaList<Album> albums = artist.getAlbums().toList();   // SELECT * FROM albums WHERE artist_id = 1
```

The relation also manages the foreign key for you, so you never set `artistId` by hand:

```java
artist.getAlbums().addAndSave(existingAlbum);  // sets artist_id and saves the album

Album draft = artist.getAlbums().create();     // new Album with artist_id already set
draft.setTitle("Back in Black");
draft.save();

int count = artist.getAlbums().size();         // SELECT count(*)
```

To filter the children, `toQuery()` hands you a normal query already scoped to the foreign key:

```java
PikaList<Album> recent = artist.getAlbums().toQuery()
        .where("year > :y", "y", 2000)
        .fetchList();
```

## Many-to-many (loadManyThrough)

A `Playlist` has many `Track`s through a `PlaylistTrack` join table. Name the join table and the target:

```java
public class Playlist extends PikaBean {
    Long id;
    String name;

    public PikaManyThroughRelation<PlaylistTrack, Track> getTracks() {
        return loadManyThrough(PlaylistTrack.class, Track.class);
    }
}
```

Reading and editing the relationship go through the join table automatically:

```java
Playlist p = Playlist.find().byId(1L);
PikaList<Track> tracks = p.getTracks().toList();   // joins through playlist_tracks

p.getTracks().addAndSave(track);   // inserts the join row linking playlist and track
p.getTracks().remove(track);       // deletes that join row (leaves both records alone)
```

## Belongs-to (load)

The flip side of one-to-many: an `Album` holds the `artistId` and belongs to one `Artist`. `load()` reads the foreign key on the current object and fetches the parent.

```java
public class Album extends PikaBean {
    Long id;
    String title;
    Long artistId;

    public Artist getArtist() {
        return load(Artist.class);   // reads this.artistId, returns that Artist
    }
}
```

## One-to-one reverse (loadReverse)

Like belongs-to, but the foreign key lives on the other table and there is exactly one match. A `User` has one `UserProfile`, where `profiles.user_id` points back at the user:

```java
public class User extends PikaBean {
    Long id;

    public UserProfile getProfile() {
        return loadReverse(UserProfile.class);   // finds the profile whose user_id = this.id
    }
}
```

## Watch for N+1

Each accessor runs its query the moment you read it. Loop over 50 artists calling `getAlbums().toList()` and you have run 51 queries. [N+1 Avoidance]({{ '/pages/n-plus-1-avoidance/' | relative_url }}) shows how to bulk-load instead.
