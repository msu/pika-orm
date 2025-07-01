package grug.db.chinook;

import grug.db.GrugORM;
import grug.db.chinook.pojos.*;
import org.junit.jupiter.api.Test;

import static grug.db.GrugORM.*;
import static grug.db.GrugORM.Interfaces.GrugLogger.Level.DEBUG;
import static grug.db.GrugORM.JoinType.LEFT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChinookTest {

    @Test
    void bootstrapTest() {
        GrugORM grugORM = makeORM();
        var artists = grugORM.find(Artist.class).all();
        assertEquals(275, artists.size());
    }

    @Test
    void testJoin() {
        GrugORM grugORM = makeORM();
        var acDc = grugORM.find(Artist.class).byId(1);
        assertEquals("AC/DC", acDc.getName());
        var acDcAlbums = grugORM.loadN(acDc, Album.class);
        assertEquals(2, acDcAlbums.size());
    }

    @Test
    void testQueryJoinTo() {
        GrugORM grugORM = makeORM();
        var query = grugORM.query(Album.class)
                .join(Artist.class)
                .where("artists.name = 'AC/DC'");
        ResultList<Album> acDcAlbums = query.execute();
        assertEquals(2, acDcAlbums.size());
    }

    @Test
    void testQueryJoinFrom() {
        GrugORM grugORM = makeORM();
        var query = grugORM.query(Artist.class)
                .join(Album.class)
                .where("albums.Title LIKE 'A%'");
        ResultList<Artist> acDcAlbums = query.execute();
        assertEquals(25, acDcAlbums.size());
    }

    @Test
    void testQuerySelfJoinUsingRawString() {
        GrugORM grugORM = makeORM();
        var query = grugORM.query(Employee.class)
                .join("employees AS boss ON employees.ReportsTo = boss.EmployeeID")
                .where("boss.Email = :email").with("email", "andrew@chinookcorp.com");
        ResultList<Employee> andrewsEmployees = query.execute();
        assertEquals(2, andrewsEmployees.size());
    }

    @Test
    void testQueryLeftJoin() {
        GrugORM grugORM = makeORM();

        // default inner join should produce 204 artists w/albums
        var query = grugORM.query(Artist.class)
                .join(Album.class);
        assertEquals(204, query.execute().size());

        // left join should produce all 275 artists
        var query2 = grugORM.query(Artist.class)
                .join(LEFT, Album.class);
        assertEquals(275, query2.execute().size());
    }

    @Test
    void testMultiTableJoin() {
        GrugORM grugORM = makeORM();

        // inner join should produce 85 artists w/albums w/tracks that start with an A
        var query = grugORM.query(Artist.class)
                .join(Album.class)
                .thenJoin(Track.class)
                .where("tracks.Name LIKE 'A%'");

        assertEquals(85, query.execute().size());
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
