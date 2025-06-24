package grug.db.chinook;

import grug.db.GrugORM;
import grug.db.chinook.beans.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static grug.db.GrugORM.Interfaces.GrugLogger.Level.DEBUG;
import static grug.db.GrugORM.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChinookBeanTest {

    @BeforeEach
    void setupOrm() {
        makeORM();
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
                .join(ArtistBean.class, AlbumBean.class)
                .where("artists.name IN :artists")
                .with("artists", List.of("AC/DC", "Santana"));
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

    public GrugORM makeORM() {
        return new GrugORM("jdbc:sqlite:db/chinook.db")
                .withLogLevel(DEBUG)
                .withDefaultFkColumn(aClass -> removeBeanSuffix(aClass.getSimpleName()) + "Id")
                .withDefaultIdField(aClass -> decapitalize(removeBeanSuffix(aClass.getSimpleName())) + "Id")
                .withDefaultColumnMapping(field -> capitalize(field.getName()))
                .withDefaultTableMapping(aClass -> snakeCase(removeBeanSuffix(aClass.getSimpleName())) + "s")
                .makeDefaultORM();
    }

    public static String removeBeanSuffix(String name) {
        return name.replace("Bean", "");
    }

}
