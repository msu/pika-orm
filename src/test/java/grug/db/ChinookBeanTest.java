package grug.db;

import grug.db.models.chinook.beans.AlbumBean;
import grug.db.models.chinook.beans.ArtistBean;
import grug.db.models.chinook.beans.EmployeeBean;
import grug.db.models.chinook.beans.TrackBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static grug.db.GrugORM.Interfaces.GrugLogger.Level.DEBUG;
import static grug.db.GrugORM.*;
import static grug.db.GrugORM.SortOrder.*;
import static grug.db.TestBase.copyFileTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChinookBeanTest {

    @BeforeEach
    void setupOrm() {
        configureOrm();
    }

    @Test
    void bootstrapTest() {
        var artists = ArtistBean.find().all();
        assertEquals(275, artists.size());
    }

    @Test
    void testJoin() {
        var acDc = ArtistBean.find().byId(1);
        assertEquals("AC/DC", acDc.getName());
        var acDcAlbums = acDc.getAlbums();
        assertEquals(2, acDcAlbums.size());
    }

    @Test
    void testQueryJoin() {
        var query = AlbumBean.find().byQuery()
                .join(ArtistBean.class)
                .where("artists.name IN :artists")
                .withVar("artists", List.of("AC/DC", "Santana"));
        ResultList<AlbumBean> acDcAlbums = query.execute();
        assertEquals(5, acDcAlbums.size());
    }

    @Test
    void testPaging() {
        var query = AlbumBean.find().byQuery()
                .pageSize(20);
        ResultList<AlbumBean> firstTwentyAlbums = query.execute();
        assertEquals(20, firstTwentyAlbums.size());
    }


    @Test
    void testMultiPaging() {
        var query = AlbumBean.find().byQuery()
                .pageSize(20)
                .page(2);
        ResultList<AlbumBean> multiPageQuery = query.execute();
        assertEquals("Prenda Minha",multiPageQuery.first().getTitle());
    }


    @Test
    void testOrderBy() {
        var query = AlbumBean.find().byQuery()
                .join(ArtistBean.class)
                .where("artists.name IN :artists")
                .withVar("artists", List.of("AC/DC", "Santana"))
                .orderBy("AlbumId");
        ResultList<AlbumBean> acDcAlbums = query.execute();
        assertEquals("For Those About To Rock We Salute You", acDcAlbums.first().getTitle());
    }

    @Test
    void testOrderByDesc() {
        var query = AlbumBean.find().byQuery()
                .join(ArtistBean.class)
                .where("artists.name IN :artists")
                .withVar("artists", List.of("AC/DC", "Santana"))
                .orderBy("AlbumId", DESC);
        ResultList<AlbumBean> acDcAlbums = query.execute();
        assertEquals("Santana Live", acDcAlbums.first().getTitle());
    }

    @Test
    void testSelfJoinWithBean() {
        EmployeeBean rootEmployee = EmployeeBean.find().byId(1);
        ResultList<EmployeeBean> reports = rootEmployee.getReports();
        assertEquals(2, reports.size());
    }

    @Test
    void testTwoWayJoin() {
        var result = AlbumBean.find().byQuery()
                .join(TrackBean.class)
                .join(ArtistBean.class)
                .where("tracks.Name LIKE 'A%' AND artists.Name LIKE 'A%'")
                        .execute();

        assertEquals(6, result.size());
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
                .makeDefaultORM();
    }

    public static String removeBeanSuffix(String name) {
        return name.replace("Bean", "");
    }

}
