package edu.montana.pika.customization;

import edu.montana.pika.TestBase;
import edu.montana.pika.models.HookEPB;
import edu.montana.pika.models.SampleEPB;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LifecycleHookTest extends TestBase {

    @Test
    void fieldSetInBeforeInsertIsInRow() {
        var orm = initTestDb(HookEPB.DDL);
        HookEPB bean = new HookEPB("a");
        bean.saveOrThrow();

        HookEPB reloaded = orm.find(HookEPB.class).byId(bean.getId());
        assertEquals("inserted", reloaded.getStamp());
    }

    @Test
    void fieldSetInBeforeUpdateIsInRow() {
        var orm = initTestDb(HookEPB.DDL);
        HookEPB bean = new HookEPB("a");
        bean.saveOrThrow();

        bean.setName("b");
        bean.saveOrThrow();

        HookEPB reloaded = orm.find(HookEPB.class).byId(bean.getId());
        assertEquals("b", reloaded.getName());
        assertEquals("updated", reloaded.getStamp());
    }

    @Test
    void beforeUpdateOverrideKeepsUnchangedColumnRemoval() {
        var orm = initTestDb(HookEPB.DDL);
        HookEPB bean = new HookEPB("a");
        bean.saveOrThrow();

        // change a column in the database that the bean does not change
        orm.exec("UPDATE hook_epbs SET note = 'from db' WHERE id = " + bean.getId());
        bean.setName("b");
        bean.saveOrThrow();

        HookEPB reloaded = orm.find(HookEPB.class).byId(bean.getId());
        assertEquals("b", reloaded.getName());
        assertEquals("from db", reloaded.getNote());
    }

    @Test
    void getOriginalValueTakesFieldName() throws Exception {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("original", 10, true, new Date(2021, 1, 1));
        model.insert();
        model.setStrVal("changed");

        assertEquals("original", model.getOriginalValue("strVal"));
        assertEquals("original", model.getOriginalValue(SampleEPB.class.getDeclaredField("strVal")));
        assertEquals("original", model.getOriginalValue("str_val"));
    }

    @Test
    void insertAllRunsBeforeInsert() {
        var orm = initTestDb(HookEPB.DDL);
        orm.insertAll(new HookEPB("a"), new HookEPB("b"));

        List<HookEPB> rows = orm.find(HookEPB.class).all().toList();
        assertEquals(2, rows.size());
        for (HookEPB row : rows) {
            assertEquals("inserted", row.getStamp());
        }
    }

    @Test
    void insertAllSkipsItemIfBeforeInsertReturnsFalse() {
        var orm = initTestDb(HookEPB.DDL);
        HookEPB skipped = new HookEPB("b");
        skipped.setSkipInsert(true);
        orm.insertAll(new HookEPB("a"), skipped, new HookEPB("c"));

        assertEquals(2, orm.find(HookEPB.class).count());
    }

    @Test
    void insertAllSkipsItemIfValidateFails() {
        var orm = initTestDb(HookEPB.DDL);
        HookEPB invalid = new HookEPB(null);
        orm.insertAll(new HookEPB("a"), invalid);

        assertEquals(1, orm.find(HookEPB.class).count());
        assertTrue(invalid.hasError("name"));
    }

    @Test
    void insertAllWithAllItemsSkippedDoesNothing() {
        var orm = initTestDb(HookEPB.DDL);
        orm.insertAll(new HookEPB(null), new HookEPB(null));

        assertEquals(0, orm.find(HookEPB.class).count());
    }
}
