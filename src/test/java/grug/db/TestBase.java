package grug.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static grug.db.GrugORM.Interfaces.GrugLogger.Level.TRACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class TestBase {

    public static GrugORM initTestDb(String... ddl) {
        try {
            // remove old db if it exists
            Path path = Path.of("test", "test.db");
            if (Files.exists(path)) {
                Files.delete(path);
            }

            path.toFile().getParentFile().mkdirs();

            GrugORM grugORM = new GrugORM("jdbc:sqlite:test/test.db")
                    .withLogLevel(TRACE)
                    .makeDefaultORM();
            for (String ddlToRun : ddl) {
                grugORM.exec(ddlToRun);
            }
            return grugORM;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String captureStdErr(Runnable runnable) {
        PrintStream original = System.err;
        try {
            ByteArrayOutputStream tmpOutBuffer = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(tmpOutBuffer);
            System.setErr(tmpOut);
            runnable.run();
            return tmpOutBuffer.toString();
        } finally {
            System.setErr(original);
        }
    }

    public static void assertStringContains(String string, String expectedString) {
        assertTrue(string.contains(expectedString), "Did not find " + expectedString + " in " + string);
    }

}
