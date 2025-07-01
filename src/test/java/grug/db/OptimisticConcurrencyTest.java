package grug.db;

import grug.db.models.OptimisticModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class OptimisticConcurrencyTest extends TestBase {

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(OptimisticModel.DDL);
    }

    @Test
    public void testBasicOptimisticConcurrency() {
        OptimisticModel model = new OptimisticModel();
        model.setStr("foo");
        model.save();
        assertEquals(1, model.getId());
        assertEquals(1, model.getVersion());
    }

    @Test
    public void testVersionUpdatesAfterSecondUpdate() {
        OptimisticModel model = new OptimisticModel();
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
        OptimisticModel originalModel = new OptimisticModel();
        originalModel.setStr("foo");
        originalModel.save();

        OptimisticModel fromDb = orm.find(OptimisticModel.class).byId(originalModel.getId());
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
