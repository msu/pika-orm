package grug.db.chinook;

import grug.db.GrugORM;
import grug.db.chinook.pojos.*;
import org.junit.jupiter.api.Test;

import static grug.db.GrugORM.*;
import static grug.db.GrugORM.Interfaces.GrugLogger.Level.DEBUG;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChinookTest {

    @Test
    void bootstrapTest() {
        GrugORM grugORM = makeORM();
        var artists = grugORM.findAll(Artist.class);
        assertEquals(275, artists.size());
    }

    @Test
    void testJoin() {
        GrugORM grugORM = makeORM();
        var acDc = grugORM.find(Artist.class, 1);
        assertEquals("AC/DC", acDc.getName());
        var acDcAlbums = grugORM.loadN(acDc, Album.class);
        assertEquals(2, acDcAlbums.size());
    }

    @Test
    void testQueryJoin() {
        GrugORM grugORM = makeORM();
        var query = grugORM.query(Album.class)
                .join(Artist.class, Album.class)
                .where("artists.name = 'AC/DC'");
        ResultList<Album> acDcAlbums = query.run();
        assertEquals(2, acDcAlbums.size());
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
