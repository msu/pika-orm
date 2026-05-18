package edu.montana.pika.features;

import edu.montana.pika.TestBase;
import edu.montana.pika.features.model.OptimisticBean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OptimisticConcurrencyTest extends TestBase {

    @Test
    public void testBasicOptimisticConcurrency() {
        var orm = initTestDb(OptimisticBean.DDL);
        OptimisticBean model = new OptimisticBean();
        model.setStr("foo");
        model.save();
        assertEquals(1, model.getId());
        assertEquals(1, model.getVersion());
    }

    @Test
    public void testVersionUpdatesAfterSecondUpdate() {
        var orm = initTestDb(OptimisticBean.DDL);
        OptimisticBean model = new OptimisticBean();
        model.setStr("foo");
        model.save();
        assertEquals(1, model.getId());
        assertEquals(1, model.getVersion());

        model.setStr("bar");
        model.save();
        assertEquals(1, model.getId());
        assertEquals(2, model.getVersion());
    }

    @Test
    public void testOptimisticUpdateDoesNotUpdateWhenVersionIsntCurrent() {
        var orm = initTestDb(OptimisticBean.DDL);
        OptimisticBean originalModel = new OptimisticBean();
        originalModel.setStr("foo");
        originalModel.save();

        OptimisticBean fromDb = orm.find(OptimisticBean.class).byId(originalModel.getId());
        assertEquals(1, fromDb.getId());
        assertEquals(1, fromDb.getVersion());
        assertEquals("foo", fromDb.getStr());

        // update original object
        originalModel.setStr("bar");
        originalModel.save();

        fromDb.setStr("doh");
        // should not save due to optimistic concurrency failure
        assertFalse(fromDb.save());

        // reload and save
        fromDb.reload();
        fromDb.setStr("doh");
        assertTrue(fromDb.save());
    }
}
