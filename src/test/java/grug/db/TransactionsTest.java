package grug.db;

import grug.db.models.TransactionDemo;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TransactionsTest extends TestBase {

    @Test
    public void testBasicInTransactionWithBadVal() {
        var orm = initTestDb(TransactionDemo.DDL);

        TransactionDemo foo = new TransactionDemo("foo", 10);
        TransactionDemo bar = new TransactionDemo("bar", -10); // bad value

        try {
            orm.inTransaction(() -> {
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
            orm.inTransaction(() -> {
                orm.insert(foo);
                orm.insert(bar);
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
            orm.inTransaction(() -> {
                orm.insert(foo);
                orm.inTransaction(() -> {
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
            orm.inTransaction(() -> {
                orm.insert(foo);
                orm.inTransaction(() -> {
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
            orm.inTransaction(() -> {
                orm.inTransaction(() -> {
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

}
