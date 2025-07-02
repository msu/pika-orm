package grug.db;

import grug.db.chinook.pojos.Album;
import org.junit.jupiter.api.Test;

import static grug.db.GrugORM.*;
import static grug.db.GrugORM.Interfaces.GrugLogger.Level.DEBUG;
import static grug.db.TestBase.initDBFileAndORM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GrugQueryBuilderTest {

    @Test
    void testRawQueryBuilderCanSelectColumnsWithResultObject() {
        var orm = makeORM();
        var query = orm.queryBuilder("Albums")
                .select("Title")
                .where("Title LIKE '%A%'")
                .withResult(Album.class);

        var results = query.execute();
        assertEquals(264, results.size());

        assertEquals("For Those About To Rock We Salute You", results.first().getTitle());
        assertNull(results.first().getAlbumId());
        assertNull(results.first().getArtistId());
    }

    @Test
    void testRawQueryBuilderCanSelectColumnsWithResultMap() {
        var orm = makeORM();
        var query = orm.queryBuilder("Albums")
                .select("Title")
                .where("Title LIKE '%A%'");

        var results = query.execute();
        assertEquals(264, results.size());

        assertEquals("For Those About To Rock We Salute You", results.first().get("Title"));
        assertEquals(1, results.first().size());
    }


    @Test
    void testRawQueryBuilderCanJoinWithoutResultClass() {//not resultMapping to a class, just POJO hashmap stuff
        var orm = makeORM();
        var query = orm.queryBuilder("Albums")
                .join("Artists on artists.artistId = albums.artistId")
                .where("artists.Name LIKE '%AC/DC%'");

        var results = query.execute();
        assertEquals(2, results.size());

        System.out.println(results);//LinkedHashMaps Generic!

        assertEquals("For Those About To Rock We Salute You", results.first().get("Title"));

    }

    @Test
    void testRawQueryBuilderJoinWithResultClass() {//This has a result map which will be mapped to Albums, we have more spesific access to class methods now and are working with objects
        var orm = makeORM();
        var query = orm.queryBuilder("Albums")
                .join("Artists on artists.artistId = albums.artistId")
                .where("artists.Name LIKE '%AC/DC%'")
                .withResult(Album.class);

        ResultList<Album> results = query.execute();
        assertEquals(2, results.size());

        System.out.println(results);//Album Objects

        assertEquals("For Those About To Rock We Salute You", results.first().getTitle());

    }

    @Test
    void testRawQueryBuilderOrderByPagingWithResultClass() {//This has a result map which will be mapped to Albums, we have more spesific access to class methods now and are working with objects
        var orm = makeORM();
        var query = orm.queryBuilder("Albums")//TODO - problem with this query, not returning with results the artists.name column
                .select("Title", "Artists.Name as artistname")//JDBC doesn't give tools with api to be able to make this work
                .join("Artists on artists.artistId = albums.artistId")//aliasing and just using artists.name should be working
                .where("artistname LIKE '%Led Zeppelin%'")
                .orderBy("Title", SortOrder.DESC);

        var results = query.execute();

        System.out.println(results);
        assertEquals(2, results.size());



    }


    public GrugORM makeORM() {
        return new GrugORM("jdbc:sqlite:db/chinook.db")
                .withLogLevel(DEBUG)
                .withDefaultFkColumn(aClass -> aClass.getSimpleName() + "Id")
                .withDefaultIdField(aClass -> decapitalize(aClass.getSimpleName()) + "Id")
                .withDefaultColumnMapping(field -> capitalize(field.getName()))
                .withDefaultTableMapping(aClass -> snakeCase(aClass.getSimpleName()) + "s");
    }

}
