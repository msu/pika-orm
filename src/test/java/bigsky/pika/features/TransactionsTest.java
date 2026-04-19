package bigsky.pika.features;

import bigsky.pika.TestBase;
import bigsky.pika.features.model.TransactionDemo;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class TransactionsTest extends TestBase {

    @Test
    public void testBasicInTransactionWithBadVal() {
        var orm = initTestDb(TransactionDemo.DDL);

        TransactionDemo foo = new TransactionDemo("foo", 10);
        TransactionDemo bar = new TransactionDemo("bar", -10); // bad value

        try {
            orm.withTransaction(() -> {
                orm.insert(foo);
                orm.insert(bar);
            });
        } catch (Exception e) {
            e.printStackTrace();
            // ignore :)
        }

        // neither value should be in the db
        assertNull(orm.find(TransactionDemo.class).byKey("name", "foo"));
        assertNull(orm.find(TransactionDemo.class).byKey( "name", "bar"));
    }

    @Test
    public void testBasicInTransactionWithGoodVals() {
        var orm = initTestDb(TransactionDemo.DDL);

        TransactionDemo foo = new TransactionDemo("foo", 10);
        TransactionDemo bar = new TransactionDemo("bar", 20); // bad value

        try {

            var x =  orm.withTransaction(() -> {
                orm.insert(foo);
                orm.insert(bar);
                return foo;
            });
        } catch (Exception e) {
            e.printStackTrace();
            // ignore :)
        }

        // neither value should be in the db
        assertNotNull(orm.find(TransactionDemo.class).byKey( "name", "foo"));
        assertNotNull(orm.find(TransactionDemo.class).byKey( "name", "bar"));
    }

    @Test
    public void testNestedTransactionsJoinWithBadVal() {
        var orm = initTestDb(TransactionDemo.DDL);

        TransactionDemo foo = new TransactionDemo("foo", 10);
        TransactionDemo bar = new TransactionDemo("bar", -10); // bad value

        try {
            orm.withTransaction(() -> {
                orm.insert(foo);
                orm.withTransaction(() -> {
                    orm.insert(bar);
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            // ignore :)
        }

        // neither value should be in the db
        assertNull(orm.find(TransactionDemo.class).byKey( "name", "foo"));
        assertNull(orm.find(TransactionDemo.class).byKey( "name", "bar"));
    }

    @Test
    public void testNestedTransactionsJoinWithGoodVals() {
        var orm = initTestDb(TransactionDemo.DDL);

        TransactionDemo foo = new TransactionDemo("foo", 10);
        TransactionDemo bar = new TransactionDemo("bar", 20); // bad value

        try {
            orm.withTransaction(() -> {
                orm.insert(foo);
                orm.withTransaction(() -> {
                    orm.insert(bar);
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            // ignore :)
        }

        // neither value should be in the db
        assertNotNull(orm.find(TransactionDemo.class).byKey( "name", "foo"));
        assertNotNull(orm.find(TransactionDemo.class).byKey( "name", "bar"));
    }

    @Test
    public void testNestedTransactionsJoinWithBadValAfterNestedInstructionFinishes() {
        var orm = initTestDb(TransactionDemo.DDL);

        TransactionDemo foo = new TransactionDemo("foo", 10);
        TransactionDemo bar = new TransactionDemo("bar", -10); // bad value

        try {
            orm.withTransaction(() -> {
                orm.withTransaction(() -> {
                    orm.insert(foo);
                });
                orm.insert(bar); // insert bad after inner transaction
            });
        } catch (Exception e) {
            e.printStackTrace();
            // ignore :)
        }

        // neither value should be in the db
        assertNull(orm.find(TransactionDemo.class).byKey( "name", "foo"));
        assertNull(orm.find(TransactionDemo.class).byKey( "name", "bar"));
    }

    // --- inTransaction / joinTransaction / forceTransaction ---

    @Test
    public void testIsInTransactionFalseByDefault() {
        var orm = initTestDb(TransactionDemo.DDL);
        assertFalse(orm.isInTransaction());
    }

    @Test
    public void testInTransactionCommits() {
        var orm = initTestDb(TransactionDemo.DDL);
        orm.inTransaction(() -> {
            assertTrue(orm.isInTransaction());
            orm.insert(new TransactionDemo("a", 1));
        });
        assertFalse(orm.isInTransaction());
        assertNotNull(orm.find(TransactionDemo.class).byKey("name", "a"));
    }

    @Test
    public void testInTransactionRollsBackOnException() {
        var orm = initTestDb(TransactionDemo.DDL);
        try {
            orm.inTransaction(() -> {
                orm.insert(new TransactionDemo("a", 1));
                throw new RuntimeException("boom");
            });
        } catch (Exception ignored) {}
        assertNull(orm.find(TransactionDemo.class).byKey("name", "a"));
    }

    @Test
    public void testInTransactionReturnsValue() {
        var orm = initTestDb(TransactionDemo.DDL);
        String result = orm.inTransaction(() -> "ok");
        assertEquals("ok", result);
    }

    @Test
    public void testInTransactionJoinsExisting() {
        var orm = initTestDb(TransactionDemo.DDL);
        try {
            orm.inTransaction(() -> {
                orm.insert(new TransactionDemo("outer", 1));
                orm.inTransaction(() -> {
                    orm.insert(new TransactionDemo("inner", 1));
                    throw new RuntimeException("rollback inner and outer");
                });
            });
        } catch (Exception ignored) {}
        assertNull(orm.find(TransactionDemo.class).byKey("name", "outer"));
        assertNull(orm.find(TransactionDemo.class).byKey("name", "inner"));
    }

    @Test
    public void testJoinTransactionThrowsOutsideTransaction() {
        var orm = initTestDb(TransactionDemo.DDL);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> orm.joinTransaction(() -> {}));
        assertTrue(e.getMessage().contains("active transaction"));
    }

    @Test
    public void testJoinTransactionJoinsExisting() {
        var orm = initTestDb(TransactionDemo.DDL);
        orm.inTransaction(() -> {
            orm.joinTransaction(() -> {
                orm.insert(new TransactionDemo("joined", 1));
            });
        });
        assertNotNull(orm.find(TransactionDemo.class).byKey("name", "joined"));
    }

    @Test
    public void testJoinTransactionReturnsValue() {
        var orm = initTestDb(TransactionDemo.DDL);
        Integer result = orm.inTransaction(() -> orm.joinTransaction(() -> 42));
        assertEquals(42, result);
    }

    @Test
    public void testForceTransactionCommits() {
        var orm = initTestDb(TransactionDemo.DDL);
        orm.forceTransaction(() -> orm.insert(new TransactionDemo("forced", 1)));
        assertFalse(orm.isInTransaction());
        assertNotNull(orm.find(TransactionDemo.class).byKey("name", "forced"));
    }

    @Test
    public void testForceTransactionRollsBackOnException() {
        var orm = initTestDb(TransactionDemo.DDL);
        try {
            orm.forceTransaction(() -> {
                orm.insert(new TransactionDemo("forced", 1));
                throw new RuntimeException("boom");
            });
        } catch (Exception ignored) {}
        assertNull(orm.find(TransactionDemo.class).byKey("name", "forced"));
    }

    @Test
    public void testForceTransactionKeepsOuterTransactionAlive() {
        var orm = initTestDb(TransactionDemo.DDL);
        orm.inTransaction(() -> {
            assertTrue(orm.isInTransaction());
            orm.forceTransaction(() -> {
                assertTrue(orm.isInTransaction()); // in the forced inner tx
            });
            assertTrue(orm.isInTransaction()); // back in the outer, still alive
        });
        assertFalse(orm.isInTransaction());
    }

    @Test
    public void testForceTransactionSurvivesOuterRollback() {
        var orm = initTestDb(TransactionDemo.DDL);
        try {
            orm.inTransaction(() -> {
                // outer performs no writes so SQLite's rollback is a no-op and
                // doesn't fight the inner connection over locks
                orm.forceTransaction(() -> {
                    orm.insert(new TransactionDemo("independent", 1));
                });
                throw new RuntimeException("outer aborts");
            });
        } catch (Exception ignored) {}
        assertNotNull(orm.find(TransactionDemo.class).byKey("name", "independent"));
    }

    @Test
    public void testForceTransactionReturnsValue() {
        var orm = initTestDb(TransactionDemo.DDL);
        String result = orm.forceTransaction(() -> "ok");
        assertEquals("ok", result);
    }

}
