package bigsky.pika.query;

import bigsky.pika.models.chinook.pojos.Album;
import org.junit.jupiter.api.Test;

import static bigsky.pika.PikaORM.*;
import static bigsky.pika.integration.ChinookTest.configureOrm;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PikaQueryBuilderTest {

    @Test
    void testRawQueryBuilderCanSelectColumnsWithResultObject() {
        var orm = configureOrm();
        var query = orm.queryBuilder("Albums")
                .select("Title")
                .where("Title LIKE '%A%'")
                .withResult(Album.class);

        var results = query.fetchList();
        assertEquals(264, results.size());

        assertEquals("For Those About To Rock We Salute You", results.first().getTitle());
        assertNull(results.first().getAlbumId());
        assertNull(results.first().getArtistId());
    }

    @Test
    void testRawQueryBuilderCanSelectColumnsWithSingleResultObject() {
        var orm = configureOrm();
        var result = orm.queryBuilder("Albums")
                .select("Title")
                .where("Title = 'For Those About To Rock We Salute You'")
                .withResult(Album.class)
                .fetchFirst();

        assertEquals("For Those About To Rock We Salute You", result.getTitle());
        assertNull(result.getAlbumId());
        assertNull(result.getArtistId());
    }

    @Test
    void testRawQueryBuilderCanSelectColumnsWithResultMap() {
        var orm = configureOrm();
        var query = orm.queryBuilder("Albums")
                .select("Title")
                .where("Title LIKE '%A%'");

        var results = query.fetchList();
        assertEquals(264, results.size());

        assertEquals("For Those About To Rock We Salute You", results.first().get("Title"));
        assertEquals(1, results.first().size());
    }

    @Test
    void testRawQueryBuilderCanSelectColumnsWithAliasesInResultMap() {
        var orm = configureOrm();
        var query = orm.queryBuilder("Albums")
                .select("Title as AlbumTitle")
                .where("Title LIKE '%A%'");

        var results = query.fetchList();
        assertEquals(264, results.size());


        assertEquals("For Those About To Rock We Salute You", results.first().get("AlbumTitle"));
        assertEquals(1, results.first().size());
    }

    @Test
    void testRawQueryBuilderCanSelectColumnsWitTableQualificationWithAliasesInResultMap() {
        var orm = configureOrm();
        var query = orm.queryBuilder("Albums")
                .select("Albums.Title as AlbumTitle")
                .where("Title LIKE '%A%'");

        var results = query.fetchList();
        assertEquals(264, results.size());


        assertEquals("For Those About To Rock We Salute You", results.first().get("AlbumTitle"));
        assertEquals(1, results.first().size());
    }

    @Test
    void testRawQueryBuilderCanSelectColumnsWitTableQualificationAndStarInResultMap() {
        var orm = configureOrm();
        var query = orm.queryBuilder("albums")
                .select("albums.*", "tracks.Name")
                .join("Tracks on albums.AlbumId = tracks.TrackId")
                .where("Title LIKE '%A%'");

        var results = query.fetchList();
        assertEquals(264, results.size());

        assertEquals(4, results.first().size());
        assertEquals("For Those About To Rock We Salute You", results.first().getString("Title"));
        assertEquals("For Those About To Rock (We Salute You)", results.first().getString("Name"));
    }

    @Test
    void testRawQueryBuilderCanRemapAColumn() {
        var orm = configureOrm();
        var query = orm.queryBuilder("albums")
                .select("tracks.Name as Title") // remap tracks.Name to the album title (insane !!!)
                .join("Tracks on albums.AlbumId = tracks.TrackId")
                .where("albums.Title LIKE '%A%'")
                .withResult(Album.class);

        var results = query.fetchList();
        assertEquals(264, results.size());

        assertEquals("For Those About To Rock (We Salute You)", results.first().getTitle());
    }

    //More verbose SQL query, that lets you touch raw SQL more

    @Test
    void testRawQueryBuilderCanJoinWithoutResultClass() {//not resultMapping to a class, just POJO hashmap stuff
        var orm = configureOrm();
        var query = orm.queryBuilder("Albums")
                .join("Artists on artists.artistId = albums.artistId")
                .where("artists.Name LIKE '%AC/DC%'");

        var results = query.fetchList();
        assertEquals(2, results.size());

        System.out.println(results);//LinkedHashMaps Generic!

        assertEquals("For Those About To Rock We Salute You", results.first().getString("Title"));

    }

    @Test
    void testRawQueryBuilderJoinWithResultClass() {//This has a result map which will be mapped to Albums, we have more specific access to class methods now and are working with objects
        var orm = configureOrm();
        var query = orm.queryBuilder("Albums")
                .join("Artists on artists.artistId = albums.artistId")
                .where("artists.Name LIKE '%AC/DC%'")
                .withResult(Album.class);

        var results = query.fetchList();
        assertEquals(2, results.size());

        System.out.println(results);//Album Objects

        assertEquals("For Those About To Rock We Salute You", results.first().getTitle());

    }

    @Test
    void testRawQueryBuilderOrderByPagingWithResultClass() {//This has a result map which will be mapped to Albums, we have more specific access to class methods now and are working with objects
        var orm = configureOrm();
        var query = orm.queryBuilder("Albums")
                .select("Title", "Artists.Name as artistname")
                .join("Artists on artists.artistId = albums.artistId")
                .where("artistname LIKE '%Led Zeppelin%'")
                .orderBy("Title", SortOrder.DESC);

        var results = query.fetchList();

        assertEquals(14, results.size());
        assertEquals("Led Zeppelin", results.first().getString("artistname"));
        assertEquals("The Song Remains The Same (Disc 2)", results.first().getString("Title"));
    }

    
}
