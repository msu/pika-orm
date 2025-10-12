package bigsky.pika.core;

import bigsky.pika.TestBase;
import bigsky.pika.models.SampleModel;
import bigsky.pika.models.SampleEgb;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

public class DeleteTest extends TestBase {

    @Test
    public void testBasicDelete() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);

        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        orm.insert(sampleModel);

        assertEquals(1, orm.find(SampleModel.class).all().toList().size());

        orm.delete(sampleModel);

        assertEquals(0, orm.find(SampleModel.class).all().toList().size());
    }

    @Test
    public void testDeleteById() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model = new SampleModel("test", 10, true, new Date(2021, 1, 1));
        orm.insert(model);
        long id = model.getId();

        assertNotNull(orm.find(SampleModel.class).byId(id));

        orm.delete(model);

        assertNull(orm.find(SampleModel.class).byId(id));
    }

    @Test
    public void testDeleteMultipleRecords() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model1 = new SampleModel("first", 1, true, new Date(2021, 1, 1));
        SampleModel model2 = new SampleModel("second", 2, true, new Date(2021, 1, 1));
        SampleModel model3 = new SampleModel("third", 3, true, new Date(2021, 1, 1));

        orm.insert(model1);
        orm.insert(model2);
        orm.insert(model3);

        assertEquals(3, orm.find(SampleModel.class).all().toList().size());

        orm.delete(model1);
        assertEquals(2, orm.find(SampleModel.class).all().toList().size());

        orm.delete(model2);
        assertEquals(1, orm.find(SampleModel.class).all().toList().size());

        orm.delete(model3);
        assertEquals(0, orm.find(SampleModel.class).all().toList().size());
    }

    @Test
    public void testDeleteDoesNotAffectOtherRecords() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model1 = new SampleModel("first", 1, true, new Date(2021, 1, 1));
        SampleModel model2 = new SampleModel("second", 2, true, new Date(2021, 1, 1));
        SampleModel model3 = new SampleModel("third", 3, true, new Date(2021, 1, 1));

        orm.insert(model1);
        orm.insert(model2);
        orm.insert(model3);

        orm.delete(model2);

        assertEquals(2, orm.find(SampleModel.class).all().toList().size());
        assertNotNull(orm.find(SampleModel.class).byId(model1.getId()));
        assertNull(orm.find(SampleModel.class).byId(model2.getId()));
        assertNotNull(orm.find(SampleModel.class).byId(model3.getId()));
    }

    @Test
    public void testDeleteSpecificRecord() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model1 = new SampleModel("keep", 1, true, new Date(2021, 1, 1));
        SampleModel model2 = new SampleModel("delete", 2, true, new Date(2021, 1, 1));

        orm.insert(model1);
        orm.insert(model2);

        orm.delete(model2);

        var remaining = orm.find(SampleModel.class).all().toList();
        assertEquals(1, remaining.size());
        assertEquals("keep", remaining.get(0).getStrVal());
    }

    @Test
    public void testDeleteAllRecords() {
        var orm = initTestDb(SampleModel.DDL);

        for (int i = 0; i < 5; i++) {
            orm.insert(new SampleModel("model" + i, i, true, new Date(2021, 1, 1)));
        }

        assertEquals(5, orm.find(SampleModel.class).all().toList().size());

        var allModels = orm.find(SampleModel.class).all().toList();
        for (SampleModel model : allModels) {
            orm.delete(model);
        }

        assertEquals(0, orm.find(SampleModel.class).all().toList().size());
    }

    @Test
    public void testDeleteFromTableWithMultipleRecords() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel target = new SampleModel("target", 100, true, new Date(2021, 1, 1));
        orm.insert(target);

        for (int i = 0; i < 10; i++) {
            orm.insert(new SampleModel("other" + i, i, true, new Date(2021, 1, 1)));
        }

        assertEquals(11, orm.find(SampleModel.class).all().toList().size());

        orm.delete(target);

        assertEquals(10, orm.find(SampleModel.class).all().toList().size());
        assertNull(orm.find(SampleModel.class).byId(target.getId()));
    }

    @Test
    public void testDeleteFirstRecord() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel first = new SampleModel("first", 1, true, new Date(2021, 1, 1));
        SampleModel second = new SampleModel("second", 2, true, new Date(2021, 1, 1));
        SampleModel third = new SampleModel("third", 3, true, new Date(2021, 1, 1));

        orm.insert(first);
        orm.insert(second);
        orm.insert(third);

        orm.delete(first);

        assertEquals(2, orm.find(SampleModel.class).all().toList().size());
        assertNull(orm.find(SampleModel.class).byId(first.getId()));
        assertNotNull(orm.find(SampleModel.class).byId(second.getId()));
        assertNotNull(orm.find(SampleModel.class).byId(third.getId()));
    }

    @Test
    public void testDeleteLastRecord() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel first = new SampleModel("first", 1, true, new Date(2021, 1, 1));
        SampleModel second = new SampleModel("second", 2, true, new Date(2021, 1, 1));
        SampleModel third = new SampleModel("third", 3, true, new Date(2021, 1, 1));

        orm.insert(first);
        orm.insert(second);
        orm.insert(third);

        orm.delete(third);

        assertEquals(2, orm.find(SampleModel.class).all().toList().size());
        assertNotNull(orm.find(SampleModel.class).byId(first.getId()));
        assertNotNull(orm.find(SampleModel.class).byId(second.getId()));
        assertNull(orm.find(SampleModel.class).byId(third.getId()));
    }

    @Test
    public void testDeleteMiddleRecord() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel first = new SampleModel("first", 1, true, new Date(2021, 1, 1));
        SampleModel second = new SampleModel("second", 2, true, new Date(2021, 1, 1));
        SampleModel third = new SampleModel("third", 3, true, new Date(2021, 1, 1));

        orm.insert(first);
        orm.insert(second);
        orm.insert(third);

        orm.delete(second);

        assertEquals(2, orm.find(SampleModel.class).all().toList().size());
        assertNotNull(orm.find(SampleModel.class).byId(first.getId()));
        assertNull(orm.find(SampleModel.class).byId(second.getId()));
        assertNotNull(orm.find(SampleModel.class).byId(third.getId()));
    }

    @Test
    public void testDeleteAndReinsert() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model = new SampleModel("test", 10, true, new Date(2021, 1, 1));
        orm.insert(model);
        long originalId = model.getId();

        orm.delete(model);
        assertEquals(0, orm.find(SampleModel.class).all().toList().size());

        SampleModel newModel = new SampleModel("reinserted", 20, false, new Date(2021, 2, 1));
        orm.insert(newModel);

        assertEquals(1, orm.find(SampleModel.class).all().toList().size());
        assertNotNull(orm.find(SampleModel.class).byId(newModel.getId()));
    }

    @Test
    public void testDeleteWithDifferentDataTypes() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel withNull = new SampleModel(null, 100, true, new Date(2021, 1, 1));
        SampleModel withEmpty = new SampleModel("", -50, false, new Date(2020, 12, 31));
        SampleModel withData = new SampleModel("data", 0, true, new Date(2022, 6, 15));

        orm.insert(withNull);
        orm.insert(withEmpty);
        orm.insert(withData);

        assertEquals(3, orm.find(SampleModel.class).all().toList().size());

        orm.delete(withNull);
        assertEquals(2, orm.find(SampleModel.class).all().toList().size());

        orm.delete(withEmpty);
        assertEquals(1, orm.find(SampleModel.class).all().toList().size());

        orm.delete(withData);
        assertEquals(0, orm.find(SampleModel.class).all().toList().size());
    }

}
