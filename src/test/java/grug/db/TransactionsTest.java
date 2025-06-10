package grug.db;

import grug.db.models.TransactionDemo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TransactionsTest extends TestBase {

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws Exception {
        orm = initDBFileAndORM();
        orm.exec(TransactionDemo.DDL);
    }

    @Test
    public void testBasicInTransactionWithBadVal() {
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
        assertNull(orm.find(TransactionDemo.class, "name", "foo"));
        assertNull(orm.find(TransactionDemo.class, "name", "bar"));
    }

    @Test
    public void testBasicInTransactionWithGoodVals() {
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
        assertNotNull(orm.find(TransactionDemo.class, "name", "foo"));
        assertNotNull(orm.find(TransactionDemo.class, "name", "bar"));
    }

    @Test
    public void testNestedTransactionsJoinWithBadVal() {
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
        assertNull(orm.find(TransactionDemo.class, "name", "foo"));
        assertNull(orm.find(TransactionDemo.class, "name", "bar"));
    }

    @Test
    public void testNestedTransactionsJoinWithGoodVals() {
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
        assertNotNull(orm.find(TransactionDemo.class, "name", "foo"));
        assertNotNull(orm.find(TransactionDemo.class, "name", "bar"));
    }

    @Test
    public void testNestedTransactionsJoinWithBadValAfterNestedInstructionFinishes() {
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
        assertNull(orm.find(TransactionDemo.class, "name", "foo"));
        assertNull(orm.find(TransactionDemo.class, "name", "bar"));
    }

}
