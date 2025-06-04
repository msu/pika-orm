package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BootstrapTest extends TestBase{

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {

        orm = initDBFileAndORM();

        orm.exec("""
                CREATE TABLE IF NOT EXISTS sample_model (
                    id INTEGER PRIMARY KEY,
                    str_val TEXT NOT NULL,
                    int_val INTEGER NOT NULL,
                    bool_val INTEGER NOT NULL,
                    date_val INTEGER NOT NULL
                );
                """);

        orm.exec("""
                CREATE TABLE IF NOT EXISTS sample_record (
                    id INTEGER PRIMARY KEY,
                    str_val TEXT NOT NULL,
                    int_val INTEGER NOT NULL,
                    bool_val INTEGER NOT NULL,
                    date_val INTEGER NOT NULL
                );
                """);
    }

    @Test
    void testInsert() {
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        assertEquals(1, orm.insert(sampleModel));
    }

    @Test
    void testInsertAsRecord() {
        SampleRecord sampleModel = new SampleRecord("foo", 10, true, new Date(2021, 1, 1));
        assertEquals(1, sampleModel.insert());
    }

    @Test
    void testFind() {
        SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
        long id = orm.insert(sampleModel);
        sampleModel.setId(id);

        SampleModel fromDb = orm.find(SampleModel.class, "id", id);

        assertEquals(id, sampleModel.getId());

        assertEquals(fromDb.getStrVal(), sampleModel.getStrVal());
        assertEquals(fromDb.getDateVal(), sampleModel.getDateVal());
        assertEquals(fromDb.getBoolVal(), sampleModel.getBoolVal());
        assertEquals(fromDb.getIntVal(), sampleModel.getIntVal());
    }

    @Test
    void testFindAll() {
        for (int i = 0; i < 10 ; i++) {
            SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }

        List<SampleModel> results =
                orm.findAll(SampleModel.class,
                        "int_val=:val",
                        Map.of("val", 10));

        assertEquals(10, results.size());

        results = orm.findAll(SampleModel.class, "date_val > :val", Map.of("val", new Date(2050, 1, 1)));

        assertEquals(0, results.size());

        results = orm.findAll(SampleModel.class, "str_val like :val", Map.of("val", "%b%"));
        assertEquals(10, results.size());

        results = orm.findAll(SampleModel.class);
        assertEquals(10, results.size());
    }

    @Test
    void testDefaultLogger() {
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        PrintStream original = System.out;
        try {
            ByteArrayOutputStream tmpOutBuffer = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(tmpOutBuffer);
            System.setOut(tmpOut);
            assertEquals(1, orm.insert(sampleModel));
            String loggedMessage = new String(tmpOutBuffer.toByteArray());
            String expectedSuffix = "INSERT SQL: INSERT INTO sample_model (bool_val, date_val, id, int_val, str_val) VALUES (?, ?, ?, ?, ?)\n" +
                    "  Args:[true, 3921-02-01, null, 10, foo]\n";
            assertTrue(loggedMessage.endsWith(expectedSuffix));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void testCustomLogger() {
        Logger logger = LoggerFactory.getLogger(BootstrapTest.class);

        // SLF4J logger adapter
        orm.withLogger((level, msg, args) -> {
            switch (level) {
                case TRACE -> logger.trace(msg, args);
                case DEBUG -> logger.debug(msg, args);
                case INFO -> logger.info(msg, args);
                case WARN -> logger.warn(msg, args);
                case ERROR -> logger.error(msg, args);
                case null, default -> {
                }
            }
        });
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));

        // for some reason simple logger uses syserror for logging by default
        PrintStream original = System.err;
        try {
            ByteArrayOutputStream tmpOutBuffer = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(tmpOutBuffer);
            System.setErr(tmpOut);
            assertEquals(1, orm.insert(sampleModel));
            String loggedMessage = new String(tmpOutBuffer.toByteArray());
            String expectedOutput = "[main] INFO grug.db.GrugORMBootstrapTest - INSERT SQL: INSERT INTO sample_model (bool_val, date_val, id, int_val, str_val) VALUES (?, ?, ?, ?, ?)\n" +
                    "  Args:[true, 3921-02-01, null, 10, foo]\n";
            assertEquals(expectedOutput, loggedMessage);
        } finally {
            System.setErr(original);
        }
    }

    @Test
    void testBasicQueryBuilder() {

        for (int i = 0; i < 10 ; i++) {
            SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }

        var query = orm.query(SampleModel.class)
                .where("date_val < :val")
                .with("val", new Date(2050, 1, 1));

        List<SampleModel> results = query.run();

        assertEquals(10, results.size());

    }

    @Test
    void testBasicQueryBuilderWithStaticMethod() {

        for (int i = 0; i < 10 ; i++) {
            SampleRecord sampleModel = new SampleRecord("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }

        var query = SampleRecord
                .where("date_val < :val")
                .with("val", new Date(2050, 1, 1));

        List<SampleRecord> results = query.run();

        assertEquals(10, results.size());
    }

}
