package edu.montana.pika.query;

import edu.montana.pika.TestBase;
import edu.montana.pika.PikaORM;
import edu.montana.pika.models.SampleModel;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class PagingTest  extends TestBase {

    private void seedPages(PikaORM orm, int count) {
        for (int i = 0; i < count; i++) {
            SampleModel m = new SampleModel();
            m.setStrVal("sample " + i);
            m.setBoolVal(true);
            m.setDateVal(new Date());
            m.setIntVal(i);
            orm.insert(m);
        }
    }

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
        var sampleModels = orm.find(SampleModel.class).page(1).pageSize(10).orderBy("id").fetchList();

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
        var sampleModels = orm.find(SampleModel.class).page(10).pageSize(10).orderBy("id").fetchList();

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
        var sampleModels = orm.find(SampleModel.class).page(10).pageSize(10).orderBy("id");

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
        var sampleModels = orm.find(SampleModel.class)
                .where("str_val LIKE :s")
                .withVar("s", "sample%") // should match all
                .page(10).pageSize(10).orderBy("id");

        assertFalse(sampleModels.isFirstPage());
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
        var sampleModels = orm.find(SampleModel.class).page(11).pageSize(10).orderBy("id");

        assertEquals(5, sampleModels.fetchList().size());
        assertFalse(sampleModels.isFirstPage());
        assertTrue(sampleModels.isLastPage());
    }

    // --- hasNextPage / hasPreviousPage / nextPageNumber / previousPageNumber / nextPageURL / previousPageURL ---

    @Test
    void testHasNextAndPreviousPageOnFirstPage() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(1).pageSize(10).orderBy("id");
        assertTrue(q.hasNextPage());
        assertFalse(q.hasPreviousPage());
    }

    @Test
    void testHasNextAndPreviousPageOnMiddlePage() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10).orderBy("id");
        assertTrue(q.hasNextPage());
        assertTrue(q.hasPreviousPage());
    }

    @Test
    void testHasNextAndPreviousPageOnLastPage() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(3).pageSize(10).orderBy("id");
        assertFalse(q.hasNextPage());
        assertTrue(q.hasPreviousPage());
    }

    @Test
    void testNavigationOnUnpagedQueryReturnsFalse() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 5);
        var q = orm.find(SampleModel.class).all();
        assertFalse(q.hasNextPage());
        assertFalse(q.hasPreviousPage());
    }

    @Test
    void testNextAndPreviousPageNumber() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10);
        assertEquals(3L, q.nextPageNumber());
        assertEquals(1L, q.previousPageNumber());
    }

    @Test
    void testPreviousPageNumberClampsAtOne() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 5);
        var q = orm.find(SampleModel.class).page(1).pageSize(10);
        assertEquals(1L, q.previousPageNumber());
    }

    @Test
    void testNextPageURLAppendsWhenNoQueryString() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10);
        assertEquals("/todos?page=3", q.nextPageURL("/todos"));
    }

    @Test
    void testNextPageURLReplacesExistingPageParam() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10);
        assertEquals("/todos?page=3", q.nextPageURL("/todos?page=2"));
    }

    @Test
    void testNextPageURLPreservesOtherParamsAndOrder() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10);
        assertEquals("/todos?status=open&page=3&sort=name",
                q.nextPageURL("/todos?status=open&page=2&sort=name"));
    }

    @Test
    void testNextPageURLAppendsPageWhenOtherParamsExist() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10);
        assertEquals("/todos?status=open&page=3",
                q.nextPageURL("/todos?status=open"));
    }

    @Test
    void testNextPageURLPreservesFragment() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10);
        assertEquals("/todos?page=3#list", q.nextPageURL("/todos?page=2#list"));
    }

    @Test
    void testPreviousPageURLReplacesParam() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(3).pageSize(10);
        assertEquals("/todos?page=2", q.previousPageURL("/todos?page=3"));
    }

    @Test
    void testNextPageURLCustomParamName() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10);
        assertEquals("/todos?p=3", q.nextPageURL("/todos?p=2", "p"));
    }

    @Test
    void testNextPageURLAcceptsUrlObject() throws Exception {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(2).pageSize(10);
        URL url = new URL("http://example.com/todos?page=2");
        assertEquals("http://example.com/todos?page=3", q.nextPageURL(url));
    }

    @Test
    void testChangingPageInvalidatesCachedResults() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(1).pageSize(10).orderBy("id");

        var firstPage = q.page(1).fetchList();
        assertEquals("sample 0", firstPage.first().getStrVal());

        var secondPage = q.page(2).fetchList();
        assertEquals("sample 10", secondPage.first().getStrVal());
    }

    @Test
    void testChangingPageSizeInvalidatesCachedResults() {
        var orm = initTestDb(SampleModel.DDL);
        seedPages(orm, 25);
        var q = orm.find(SampleModel.class).page(1).pageSize(10).orderBy("id");

        assertEquals(5, q.pageSize(5).fetchList().size());
        assertEquals(15, q.pageSize(15).fetchList().size());
    }

}
