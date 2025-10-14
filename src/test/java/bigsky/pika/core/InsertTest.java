package bigsky.pika.core;

import bigsky.pika.TestBase;
import bigsky.pika.models.SampleModel;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

public class InsertTest extends TestBase {

    @Test
    void testInsert() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        long id = orm.insert(sampleModel);
        assertEquals(1, id);
    }

   /*
   Bulk insertion compatability
   ALL BUT MARIADB (Not done with the compatability changes I believe)
    */
    @Test
    void testInsertAll(){//new test for the bulk insert
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel2 = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel3 = new SampleModel("zee", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel4 = new SampleModel("hoo", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel5 = new SampleModel("daw", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel6 = new SampleModel("jaw", 10, true, new Date(2021, 1, 1));

        orm.insertAll(sampleModel, sampleModel2, sampleModel3, sampleModel4, sampleModel5, sampleModel6);

        var query = orm.query(SampleModel.class)
                .where("date_val = :val")
                .withVar("val", new Date(2021, 1, 1));

        var result = query.toList();

        assertEquals(6, result.size());
    }

    @Test//check with error stuff
    void testFailInsertAllMultiClass(){
        var orm = initTestDb(SampleModel.DDL);
        try {
            SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel2 = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel3 = new SampleModel("zee", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel4= new SampleModel("hoo", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel5 = new SampleModel("daw", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel6 = new SampleModel("jaw", 10, true, new Date(2021, 1, 1));
            Object sampleModel7 = new Object();
            orm.insertAll(sampleModel, sampleModel2, sampleModel3, sampleModel4, sampleModel5, sampleModel6, sampleModel7);
            fail("Should fail as there are more than one class being mass inserted at once");
        } catch (IllegalStateException e){
            assertEquals("All values passed to insertAll() must be the same type!  Expected SampleModel but found Object", e.getMessage());
        }
    }

    @Test
    void testInsertSetsIdOnModel() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        assertNull(sampleModel.getId());

        long id = orm.insert(sampleModel);

        assertNotNull(sampleModel.getId());
        assertEquals(id, sampleModel.getId());
    }

    @Test
    void testInsertMultipleRecords() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model1 = new SampleModel("first", 1, true, new Date(2021, 1, 1));
        SampleModel model2 = new SampleModel("second", 2, false, new Date(2021, 2, 1));
        SampleModel model3 = new SampleModel("third", 3, true, new Date(2021, 3, 1));

        long id1 = orm.insert(model1);
        long id2 = orm.insert(model2);
        long id3 = orm.insert(model3);

        assertEquals(1, id1);
        assertEquals(2, id2);
        assertEquals(3, id3);

        assertEquals(3, orm.find(SampleModel.class).all().toList().size());
    }

    @Test
    void testInsertAllWithSingleRecord() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model = new SampleModel("single", 99, true, new Date(2021, 1, 1));
        orm.insertAll(model);

        assertEquals(1, orm.find(SampleModel.class).all().toList().size());
    }

    @Test
    void testInsertPreservesAllFieldValues() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel original = new SampleModel("testStr", 42, false, new Date(2021, 5, 15));
        long id = orm.insert(original);

        SampleModel retrieved = orm.find(SampleModel.class).byId(id);

        assertEquals("testStr", retrieved.getStrVal());
        assertEquals(42, retrieved.getIntVal());
        assertEquals(false, retrieved.getBoolVal());
        assertNotNull(retrieved.getDateVal());
    }

    @Test
    void testInsertAllEmptyArray() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel[] emptyArray = new SampleModel[0];
        orm.insertAll(emptyArray);

        assertEquals(0, orm.find(SampleModel.class).all().toList().size());
    }

    @Test
    void testInsertWithDifferentDataTypes() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel withTrue = new SampleModel("test1", 100, true, new Date(2021, 1, 1));
        SampleModel withFalse = new SampleModel("test2", -50, false, new Date(2020, 12, 31));

        long id1 = orm.insert(withTrue);
        long id2 = orm.insert(withFalse);

        SampleModel retrieved1 = orm.find(SampleModel.class).byId(id1);
        SampleModel retrieved2 = orm.find(SampleModel.class).byId(id2);

        assertTrue(retrieved1.getBoolVal());
        assertFalse(retrieved2.getBoolVal());
        assertEquals(100, retrieved1.getIntVal());
        assertEquals(-50, retrieved2.getIntVal());
    }

    @Test
    void testInsertAllPreservesOrder() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel m1 = new SampleModel("alpha", 1, true, new Date(2021, 1, 1));
        SampleModel m2 = new SampleModel("beta", 2, true, new Date(2021, 1, 1));
        SampleModel m3 = new SampleModel("gamma", 3, true, new Date(2021, 1, 1));

        orm.insertAll(m1, m2, m3);

        var results = orm.find(SampleModel.class).all().toList();
        assertEquals("alpha", results.get(0).getStrVal());
        assertEquals("beta", results.get(1).getStrVal());
        assertEquals("gamma", results.get(2).getStrVal());
    }
}
