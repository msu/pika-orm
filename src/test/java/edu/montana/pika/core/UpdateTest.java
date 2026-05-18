package edu.montana.pika.core;

import edu.montana.pika.TestBase;
import edu.montana.pika.models.SampleModel;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateTest extends TestBase {

    @Test
    public void testBasicUpdate() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        orm.insert(sampleModel);

        sampleModel.setStrVal("bar");
        orm.update(sampleModel);

        SampleModel fromDb = orm.find(SampleModel.class).byId(sampleModel.getId());

        assertEquals("bar", fromDb.getStrVal());
    }

    @Test
    public void testUpdateMultipleFields() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("original", 100, true, new Date(2021, 1, 1));
        orm.insert(model);

        model.setStrVal("updated");
        model.setIntVal(200);
        model.setBoolVal(false);
        orm.update(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());

        assertEquals("updated", fromDb.getStrVal());
        assertEquals(200, fromDb.getIntVal());
        assertFalse(fromDb.getBoolVal());
    }

    @Test
    public void testUpdatePreservesUnchangedFields() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("original", 100, true, new Date(2021, 5, 10));
        orm.insert(model);

        model.setStrVal("changed");
        orm.update(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());

        assertEquals("changed", fromDb.getStrVal());
        assertEquals(100, fromDb.getIntVal());
        assertTrue(fromDb.getBoolVal());
        assertNotNull(fromDb.getDateVal());
    }

    @Test
    public void testUpdateToNull() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 50, true, new Date(2021, 1, 1));
        orm.insert(model);

        model.setStrVal(null);
        orm.update(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertNull(fromDb.getStrVal());
    }

    @Test
    public void testUpdateMultipleRecords() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model1 = new SampleModel("first", 1, true, new Date(2021, 1, 1));
        SampleModel model2 = new SampleModel("second", 2, true, new Date(2021, 1, 1));
        SampleModel model3 = new SampleModel("third", 3, true, new Date(2021, 1, 1));

        orm.insert(model1);
        orm.insert(model2);
        orm.insert(model3);

        model1.setStrVal("updated1");
        model2.setStrVal("updated2");
        model3.setStrVal("updated3");

        orm.update(model1);
        orm.update(model2);
        orm.update(model3);

        assertEquals("updated1", orm.find(SampleModel.class).byId(model1.getId()).getStrVal());
        assertEquals("updated2", orm.find(SampleModel.class).byId(model2.getId()).getStrVal());
        assertEquals("updated3", orm.find(SampleModel.class).byId(model3.getId()).getStrVal());
    }

    @Test
    public void testUpdateBooleanField() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 10, true, new Date(2021, 1, 1));
        orm.insert(model);

        model.setBoolVal(false);
        orm.update(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertFalse(fromDb.getBoolVal());

        model.setBoolVal(true);
        orm.update(model);

        fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertTrue(fromDb.getBoolVal());
    }

    @Test
    public void testUpdateIntegerField() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 10, true, new Date(2021, 1, 1));
        orm.insert(model);

        model.setIntVal(999);
        orm.update(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals(999, fromDb.getIntVal());
    }

    @Test
    public void testUpdateToNegativeNumber() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 100, true, new Date(2021, 1, 1));
        orm.insert(model);

        model.setIntVal(-50);
        orm.update(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals(-50, fromDb.getIntVal());
    }

    @Test
    public void testUpdateDateField() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 10, true, new Date(2021, 1, 1));
        orm.insert(model);

        Date newDate = new Date(2022, 12, 31);
        model.setDateVal(newDate);
        orm.update(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertNotNull(fromDb.getDateVal());
    }

    @Test
    public void testMultipleUpdatesToSameRecord() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("original", 1, true, new Date(2021, 1, 1));
        orm.insert(model);

        model.setIntVal(2);
        orm.update(model);
        assertEquals(2, orm.find(SampleModel.class).byId(model.getId()).getIntVal());

        model.setIntVal(3);
        orm.update(model);
        assertEquals(3, orm.find(SampleModel.class).byId(model.getId()).getIntVal());

        model.setIntVal(4);
        orm.update(model);
        assertEquals(4, orm.find(SampleModel.class).byId(model.getId()).getIntVal());
    }

    @Test
    public void testUpdateDoesNotAffectOtherRecords() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model1 = new SampleModel("first", 1, true, new Date(2021, 1, 1));
        SampleModel model2 = new SampleModel("second", 2, true, new Date(2021, 1, 1));

        orm.insert(model1);
        orm.insert(model2);

        model1.setStrVal("updated");
        orm.update(model1);

        SampleModel fromDb1 = orm.find(SampleModel.class).byId(model1.getId());
        SampleModel fromDb2 = orm.find(SampleModel.class).byId(model2.getId());

        assertEquals("updated", fromDb1.getStrVal());
        assertEquals("second", fromDb2.getStrVal());
    }

    @Test
    public void testUpdateEmptyStringValue() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("original", 10, true, new Date(2021, 1, 1));
        orm.insert(model);

        model.setStrVal("");
        orm.update(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals("", fromDb.getStrVal());
    }

}
