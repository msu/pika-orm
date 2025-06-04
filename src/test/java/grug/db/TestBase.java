package grug.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static grug.db.GrugORM.Interfaces.GrugLogger.Level.TRACE;

public abstract class TestBase {



    protected GrugORM initDBFileAndORM() throws IOException {
        // remove old db if it exists
        Path path = Path.of("test", "test.db");
        if (Files.exists(path)) {
            Files.delete(path);
        }

        path.toFile().getParentFile().mkdirs();

        return new GrugORM("jdbc:sqlite:test/test.db")
                .withLogLevel(TRACE)
                .makeDefaultORM();
    }

}
