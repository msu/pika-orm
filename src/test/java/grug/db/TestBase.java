package grug.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import static grug.db.GrugORM.Interfaces.GrugLogger.Level.TRACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class TestBase {

    public enum DatabaseMode {
        SQLITE,
        H2,
    }

    private static DatabaseMode MODE = DatabaseMode.SQLITE;
    public static void setMode(DatabaseMode mode) {
        MODE = mode;
    }
    public static DatabaseMode getMode() {
        return MODE;
    }

    public static void copyFileTo(String from, String to) {
        Path source = Path.of(from);
        Path target = Path.of(to);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static GrugORM initTestDb(String... ddl) {
        try {

            List<String> ddlAsList = Arrays.asList(ddl);
            GrugORM grugORM;
            // remove old db if it exists

            if(MODE == DatabaseMode.H2) {
                // make compatible w/h2
                ddlAsList = ddlAsList.stream().map(
                        str -> str.replace("PRIMARY KEY", "auto_increment DEFAULT ON NULL")
                ).toList();
                grugORM = new GrugORM("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;");
                grugORM.exec("DROP ALL OBJECTS"); // clear database
            } else {
                Path path = Path.of("test/test.db");
                if (Files.exists(path)) {
                    Files.delete(path);
                }
                Files.createDirectories(path.getParent());                grugORM = new GrugORM("jdbc:sqlite:./test/test.db");
            }

            // set trace level and make default
            grugORM.withLogLevel(TRACE)
            .makeDefaultORM();

            // execute DDL
            for (String ddlToRun : ddlAsList) {
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
