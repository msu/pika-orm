package bigsky.pika;

import bigsky.pika.models.SampleModel;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class PagingTest  extends TestBase {

    @Test
    void testBasicPagingWorksFirstPage(){
        var orm = initTestDb(SampleModel.DDL);
        for (int i = 0; i < 100; i++) {
            SampleModel sampleModel = new SampleModel();
            sampleModel.setStrVal("sample " + i);
            sampleModel.setBoolVal(true);
            sampleModel.setDateVal(new Date());
            sampleModel.setIntVal(i);
            orm.insert(sampleModel);
        }

        // first page
        var sampleModels = orm.find(SampleModel.class).byQuery().page(1).pageSize(10).orderBy("id").fetchAsList();

        assertEquals(10, sampleModels.size());
        assertEquals("sample 0", sampleModels.first().getStrVal());
        assertEquals("sample 9", sampleModels.last().getStrVal());
    }

    @Test
    void testBasicPagingWorksLastPage(){
        var orm = initTestDb(SampleModel.DDL);
        for (int i = 0; i < 100; i++) {
            SampleModel sampleModel = new SampleModel();
            sampleModel.setStrVal("sample " + i);
            sampleModel.setBoolVal(true);
            sampleModel.setDateVal(new Date());
            sampleModel.setIntVal(i);
            orm.insert(sampleModel);
        }

        // first page
        var sampleModels = orm.find(SampleModel.class).byQuery().page(10).pageSize(10).orderBy("id").fetchAsList();

        assertEquals(10, sampleModels.size());
        assertEquals("sample 90", sampleModels.first().getStrVal());
        assertEquals("sample 99", sampleModels.last().getStrVal());
    }

    @Test
    void testFirstAndLastPageWorksOnLastPage(){
        var orm = initTestDb(SampleModel.DDL);
        for (int i = 0; i < 100; i++) {
            SampleModel sampleModel = new SampleModel();
            sampleModel.setStrVal("sample " + i);
            sampleModel.setBoolVal(true);
            sampleModel.setDateVal(new Date());
            sampleModel.setIntVal(i);
            orm.insert(sampleModel);
        }

        // first page
        var sampleModels = orm.find(SampleModel.class).byQuery().page(10).pageSize(10).orderBy("id");

        assertFalse(sampleModels.isFirstPage());
        assertTrue(sampleModels.isLastPage());
    }

    @Test
    void testFirstAndLastPageWorksOnLastPageWithQuery(){
        var orm = initTestDb(SampleModel.DDL);
        for (int i = 0; i < 100; i++) {
            SampleModel sampleModel = new SampleModel();
            sampleModel.setStrVal("sample " + i);
            sampleModel.setBoolVal(true);
            sampleModel.setDateVal(new Date());
            sampleModel.setIntVal(i);
            orm.insert(sampleModel);
        }

        // first page
        var sampleModels = orm.find(SampleModel.class).byQuery()
                .where("str_val LIKE :s")
                .withVar("s", "sample%") // should match all
                .page(10).pageSize(10).orderBy("id");

        assertFalse(sampleModels.isFirstPage());
        assertTrue(sampleModels.isLastPage());
    }

    @Test
    void testFirstAndLastPageWorksOnLastPageHalfFull(){
        var orm = initTestDb(SampleModel.DDL);
        for (int i = 0; i < 105; i++) {
            SampleModel sampleModel = new SampleModel();
            sampleModel.setStrVal("sample " + i);
            sampleModel.setBoolVal(true);
            sampleModel.setDateVal(new Date());
            sampleModel.setIntVal(i);
            orm.insert(sampleModel);
        }

        // first page
        var sampleModels = orm.find(SampleModel.class).byQuery().page(11).pageSize(10).orderBy("id");

        assertEquals(5, sampleModels.fetchAsList().size());
        assertFalse(sampleModels.isFirstPage());
        assertTrue(sampleModels.isLastPage());
    }

}
