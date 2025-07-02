package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleEgb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggingTest extends TestBase {

    @Test
    void testDefaultLogger() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        PrintStream original = System.out;
        try {
            ByteArrayOutputStream tmpOutBuffer = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(tmpOutBuffer);
            System.setOut(tmpOut);
            assertEquals(1, orm.insert(sampleModel));
            String loggedMessage = new String(tmpOutBuffer.toByteArray());
            String expectedLogMessage = "INSERT SQL: INSERT INTO sample_model (bool_val, date_val, int_val, str_val) VALUES (?, ?, ?, ?)\n" +
                    "  Args:[true, 3921-02-01, 10, foo]\n";
            assertStringContains(loggedMessage, expectedLogMessage);
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void testCustomLogger() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
        Logger logger = LoggerFactory.getLogger(LoggingTest.class);
        orm.logQueries(); // log at INFO so they appear in the logs

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
        String loggedMessage = captureStdErr(() -> {
            assertEquals(1, orm.insert(sampleModel));
        });
        String expectedLogMessage = "INSERT SQL: INSERT INTO sample_model (bool_val, date_val, int_val, str_val) VALUES (?, ?, ?, ?)\n" +
                "  Args:[true, 3921-02-01, 10, foo]\n";
        assertStringContains(loggedMessage, expectedLogMessage);
    }

}
