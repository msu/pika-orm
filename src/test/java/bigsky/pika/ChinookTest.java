package bigsky.pika;

import bigsky.pika.models.chinook.pojos.Album;
import bigsky.pika.models.chinook.pojos.Artist;
import bigsky.pika.models.chinook.pojos.Employee;
import bigsky.pika.models.chinook.pojos.Track;
import org.junit.jupiter.api.Test;

import static bigsky.pika.PikaORM.*;
import static bigsky.pika.PikaORM.Interfaces.PikaLogger.Level.DEBUG;
import static bigsky.pika.PikaORM.JoinType.LEFT;
import static bigsky.pika.TestBase.copyFileTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChinookTest {

    @Test
    void bootstrapTest() {
        PikaORM pikaORM = configureOrm();
        var artists = pikaORM.find(Artist.class).all().toList();
        assertEquals(275, artists.size());
    }

    @Test
    void testJoin() {
        PikaORM pikaORM = configureOrm();
        var acDc = pikaORM.find(Artist.class).byId(1);
        assertEquals("AC/DC", acDc.getName());
        var acDcAlbums = pikaORM.loadMany(acDc, Album.class).toList();
        assertEquals(2, acDcAlbums.size());
    }

    @Test
    void testQueryJoinTo() {
        PikaORM pikaORM = configureOrm();
        var query = pikaORM.query(Album.class)
                .join(Artist.class)
                .where("artists.name = 'AC/DC'");
        var acDcAlbums = query.fetchList();
        assertEquals(2, acDcAlbums.size());
    }

    @Test
    void testQueryJoinFrom() {
        PikaORM pikaORM = configureOrm();
        var query = pikaORM.query(Artist.class)
                .join(Album.class)
                .where("albums.Title LIKE 'A%'");
        var acDcAlbums = query.fetchList();
        assertEquals(25, acDcAlbums.size());
    }

    @Test
    void testQuerySelfJoinUsingRawString() {
        PikaORM pikaORM = configureOrm();
        var query = pikaORM.query(Employee.class)
                .join("employees AS boss ON employees.ReportsTo = boss.EmployeeID")
                .where("boss.Email = :email").withVar("email", "andrew@chinookcorp.com");
        var andrewsEmployees = query.fetchList();
        assertEquals(2, andrewsEmployees.size());
    }

    @Test
    void testQueryLeftJoin() {
        PikaORM pikaORM = configureOrm();

        // default inner join should produce 204 artists w/albums
        var query = pikaORM.query(Artist.class)
                .join(Album.class);
        assertEquals(204, query.fetchList().size());

        // left join should produce all 275 artists
        var query2 = pikaORM.query(Artist.class)
                .join(LEFT, Album.class);
        assertEquals(275, query2.fetchList().size());
    }

    @Test
    void testMultiTableJoin() {
        PikaORM pikaORM = configureOrm();

        // inner join should produce 85 artists w/albums w/tracks that start with an A
        var query = pikaORM.query(Artist.class)
                .join(Album.class)
                .thenJoin(Track.class)
                .where("tracks.Name LIKE 'A%'");

        assertEquals(85, query.fetchList().size());
    }


    public static PikaORM configureOrm() {
        copyFileTo("dbs/chinook.original", "test/chinook.db");
        return new PikaORM("jdbc:sqlite:test/chinook.db")
                .withLogLevel(DEBUG)
                .withDefaultColumnMapping(field -> TextTools.capitalize(field.getName()))
                .withDefaultFkColumn(aClass -> aClass.getSimpleName() + "Id")
                .withDefaultIdField(aClass -> TextTools.decapitalize(aClass.getSimpleName()) + "Id");
    }

}
