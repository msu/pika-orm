package grug.db;

import grug.db.models.chinook.beans.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static grug.db.GrugORM.Interfaces.GrugLogger.Level.DEBUG;
import static grug.db.GrugORM.*;
import static grug.db.GrugORM.SortOrder.*;
import static grug.db.TestBase.copyFileTo;
import static org.junit.jupiter.api.Assertions.*;

public class ChinookBeanTest {

    @BeforeEach
    void setupOrm() {
        configureOrm();
    }

    @Test
    void bootstrapTest() {
        var artists = ArtistBean.find().all().toList();
        assertEquals(275, artists.size());
    }

    @Test
    void testJoin() {
        var acDc = ArtistBean.find().byId(1);
        assertEquals("AC/DC", acDc.getName());
        var acDcAlbums = acDc.getAlbums().toList();
        assertEquals(2, acDcAlbums.size());
    }

    @Test
    void testQueryJoin() {
        var query = AlbumBean.find().byQuery()
                .join(ArtistBean.class)
                .where("artists.name IN :artists")
                .withVar("artists", List.of("AC/DC", "Santana"));
        var acDcAlbums = query.execute().toList();
        assertEquals(5, acDcAlbums.size());
    }

    @Test
    void testPaging() {
        var query = AlbumBean.find().byQuery()
                .pageSize(20);
        var firstTwentyAlbums = query.execute().toList();
        assertEquals(20, firstTwentyAlbums.size());
    }


    @Test
    void testMultiPaging() {
        var query = AlbumBean.find().byQuery()
                .pageSize(20)
                .page(2);
        var multiPageQuery = query.execute();
        assertEquals("Prenda Minha",multiPageQuery.first().getTitle());
    }


    @Test
    void testOrderBy() {
        var query = AlbumBean.find().byQuery()
                .join(ArtistBean.class)
                .where("artists.name IN :artists")
                .withVar("artists", List.of("AC/DC", "Santana"))
                .orderBy("AlbumId");
        var acDcAlbums = query.execute();
        assertEquals("For Those About To Rock We Salute You", acDcAlbums.first().getTitle());
    }

    @Test
    void testOrderByDesc() {
        var query = AlbumBean.find().byQuery()
                .join(ArtistBean.class)
                .where("artists.name IN :artists")
                .withVar("artists", List.of("AC/DC", "Santana"))
                .orderBy("AlbumId", DESC);
        var acDcAlbums = query.execute();
        assertEquals("Santana Live", acDcAlbums.first().getTitle());
    }

    @Test
    void testSelfJoinWithBean() {
        EmployeeBean rootEmployee = EmployeeBean.find().byId(1);
        var reports = rootEmployee.getReports().toList();
        assertEquals(2, reports.size());
    }

    @Test
    void testTwoWayJoin() {
        var result = AlbumBean.find().byQuery()
                .join(TrackBean.class)
                .join(ArtistBean.class)
                .where("tracks.Name LIKE 'A%' AND artists.Name LIKE 'A%'")
                        .executeAsList();

        assertEquals(6, result.size());
    }

    @Test
    void testNtoNLoad() {
        var playlist = PlaylistBean.find().byId(3);
        assertEquals("TV Shows", playlist.getName());
        var tracks = playlist.getTracks().toList();
        assertEquals(213, tracks.size());
    }

    @Test
    void testNtoNLoadTheOtherWay() {
        var track = TrackBean.find().byId(3);
        assertEquals("Fast As a Shark", track.getName());
        var playlists = track.getPlaylists().toList();
        System.out.println(playlists);
        assertEquals(4, playlists.size());
    }

    @Test
    void testInsertNtoNEntityWithNoPrimaryKey() {

        var playlist = PlaylistBean.find().byId(3);
        assertEquals("TV Shows", playlist.getName());
        var tracks = playlist.getTracks();
        assertFalse(tracks.hasMatch(trackBean -> trackBean.getTrackId() == 3));

        PlaylistTrackBean.associate(playlist, TrackBean.find().byId(3));

        tracks.reload();
        assertTrue(tracks.hasMatch(trackBean -> trackBean.getTrackId() == 3));
    }

    @Test
    void testRemoveNtoNEntityWithNoPrimaryKey() {

        var playlist = PlaylistBean.find().byId(3);
        assertEquals("TV Shows", playlist.getName());
        var tracks = playlist.getTracks();
        var firstTrack = tracks.first();

        PlaylistTrackBean.unassociate(playlist, firstTrack);

        tracks = playlist.getTracks();
        assertFalse(tracks.hasMatch(trackBean -> Objects.equals(trackBean.getTrackId(), firstTrack.getTrackId())));
    }

    public static GrugORM configureOrm() {
        copyFileTo("dbs/chinook.original", "test/chinook.db");
        return new GrugORM("jdbc:sqlite:test/chinook.db")
                .withLogLevel(DEBUG)
                .withDefaultFkColumn(aClass -> removeBeanSuffix(aClass.getSimpleName()) + "Id")
                .withDefaultIdField(aClass -> {
                    String className = aClass.getSimpleName();
                    String strippedClassName = removeBeanSuffix(className);
                    return TextTools.decapitalize(strippedClassName) + "Id";
                })
                .withDefaultColumnMapping(field -> TextTools.capitalize(field.getName()))
                .withDefaultTableMapping(aClass -> {
                    String className = aClass.getSimpleName();
                    String strippedClassName = removeBeanSuffix(className);
                    String plural = TextTools.pluralize(strippedClassName);
                    return TextTools.snakeCase(plural);
                })
                .withMapping(PlaylistTrackBean.class, "playlist_track") // for some reason this table isn't plural
                .makeDefaultORM();
    }

    public static String removeBeanSuffix(String name) {
        return name.replace("Bean", "");
    }

}
