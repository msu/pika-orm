package grug.db;

import grug.db.chinook.beans.AlbumBean;
import grug.db.chinook.pojos.Album;
import grug.db.chinook.pojos.Artist;
import grug.db.chinook.pojos.Employee;
import grug.db.models.SampleGrugRecord;
import grug.db.models.SampleModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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

    //More verbose SQL query, that lets you touch raw SQL more
    public GrugORM makeORM() {
        return new GrugORM("jdbc:sqlite:db/chinook.db")
                .withLogLevel(DEBUG)
                .withDefaultFkColumn(aClass -> aClass.getSimpleName() + "Id")
                .withDefaultIdField(aClass -> decapitalize(aClass.getSimpleName()) + "Id")
                .withDefaultColumnMapping(field -> capitalize(field.getName()))
                .withDefaultTableMapping(aClass -> snakeCase(aClass.getSimpleName()) + "s");
    }

}
