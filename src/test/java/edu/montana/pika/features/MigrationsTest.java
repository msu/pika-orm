package edu.montana.pika.features;

import edu.montana.pika.PikaORM;
import edu.montana.pika.migrations.Migrations.PikaMigration;
import edu.montana.pika.migrations.Migrations.MigrationStatus;
import edu.montana.pika.TestBase;
import edu.montana.pika.migrations.MigrationDemoModel;
import edu.montana.pika.migrations.Migrations;
import edu.montana.pika.migrations.MigrationsFile1;
import edu.montana.pika.migrations.MultiStatementMigration;
import edu.montana.pika.migrations.MigrationFileForConsoleTesting;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MigrationsTest extends TestBase {

    @Test
    void basicMigrationWorksEndToEnd() {
        var orm = initTestDb();
        // migrate the database
        MigrationsFile1 migrations = new MigrationsFile1();
        orm.withMigrations(migrations);
        migrations.applyAll();

        // insert a new model based on the migration
        MigrationDemoModel migrationDemoModel = new MigrationDemoModel();
        Date dateVal = new Date();
        migrationDemoModel.setDateVal(dateVal);

        long id = orm.insert(migrationDemoModel);

        MigrationDemoModel fromDB = orm.find(MigrationDemoModel.class).byId(id);
        assertEquals(dateVal, fromDB.getDateVal());
    }

    @Test
    void basicMigrationWorks() {
        var orm = initTestDb();
        // migrate the database
        MigrationsFile1 migrations = new MigrationsFile1();
        orm.withMigrations(migrations);
        migrations.applyAll();

        var migrationsInDb = orm.find(PikaMigration.class).all().toList();

        assertEquals(1, migrationsInDb.size());
        assertEquals(MigrationStatus.APPLIED, migrationsInDb.get(0).getStatus());
    }

    @Test
    void applyAllIsIdempotentSameInstance() {
        var orm = initTestDb();
        MigrationsFile1 migrations = new MigrationsFile1();
        orm.withMigrations(migrations);

        migrations.applyAll();
        var afterFirst = orm.find(PikaMigration.class).all().toList();
        assertEquals(1, afterFirst.size());
        MigrationStatus firstStatus = afterFirst.get(0).getStatus();

        migrations.applyAll(); // should be a no-op
        var afterSecond = orm.find(PikaMigration.class).all().toList();
        assertEquals(1, afterSecond.size());
        assertEquals(firstStatus, afterSecond.get(0).getStatus());
    }

    @Test
    void applyAllIsIdempotentAcrossOrmInstances() throws IOException {
        // Simulates the real DemoServer pattern: process restart against the same DB file.
        Path dbFile = Path.of("test", "migrations_idempotent.db");
        Files.createDirectories(dbFile.getParent());
        Files.deleteIfExists(dbFile);

        try {
            PikaORM first = new PikaORM("jdbc:sqlite:" + dbFile).withSQLiteQuirks().makeDefaultORM();
            MigrationsFile1 migrationsFirst = new MigrationsFile1();
            first.withMigrations(migrationsFirst);
            migrationsFirst.applyAll();
            assertEquals(1, first.find(PikaMigration.class).all().toList().size());

            PikaORM second = new PikaORM("jdbc:sqlite:" + dbFile).withSQLiteQuirks().makeDefaultORM();
            MigrationsFile1 migrationsSecond = new MigrationsFile1();
            second.withMigrations(migrationsSecond);
            migrationsSecond.applyAll(); // second process — should skip the already-applied migration
            var persisted = second.find(PikaMigration.class).all().toList();
            assertEquals(1, persisted.size());
            assertEquals(MigrationStatus.APPLIED, persisted.get(0).getStatus());
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    void multipleStatementsWork() {
        var orm = initTestDb();
        // migrate the database
        MultiStatementMigration migrations = new MultiStatementMigration();
        orm.withMigrations(migrations);
        migrations.applyAll();

        var demoModel = orm.find(MigrationDemoModel.class).all().toList();

        assertEquals(1, demoModel.size());
        assertEquals("foo", demoModel.get(0).getStrVal());
    }

    @Test
    void upThenDownRemovesMigration() {
        var orm = initTestDb();
        MigrationsFile1 migrations = new MigrationsFile1();
        orm.withMigrations(migrations);

        migrations.up();
        assertEquals(1, orm.find(PikaMigration.class).count());

        migrations.down();
        assertEquals(0, orm.find(PikaMigration.class).count());
    }

    static class NoDownMigrations extends Migrations {
        @Override
        public void migrations() {
            add(makeMigration("no down").up("CREATE TABLE no_downs (id INTEGER PRIMARY KEY)"));
        }
    }

    @Test
    void showWorksWithNoDown() {
        var orm = initTestDb();
        NoDownMigrations migrations = new NoDownMigrations();
        orm.withMigrations(migrations);

        String out = runConsole(migrations, "show\n");

        assertStringContains(out, "no down");
    }

    @Test
    void downWithNoDownThrowsClearError() {
        var orm = initTestDb();
        NoDownMigrations migrations = new NoDownMigrations();
        orm.withMigrations(migrations);
        migrations.up();

        var e = assertThrows(IllegalStateException.class, migrations::down);
        assertEquals("Migration no down has no down SQL. Add down(...) to the migration to back it out.", e.getMessage());
        assertEquals(1, orm.find(PikaMigration.class).count());
    }

    @Test
    void showPrintsAppliedDateAfterUp() {
        var orm = initTestDb();
        MigrationsFile1 migrations = new MigrationsFile1();
        orm.withMigrations(migrations);

        String before = showRow(runConsole(migrations, "show\n"), "migration1");
        assertFalse(before.contains("null"), before);

        migrations.up();
        String after = showRow(runConsole(migrations, "show\n"), "migration1");
        assertFalse(after.contains("null"), after);
        // Date.toString() ends with the year
        assertStringContains(after, String.valueOf(java.time.Year.now().getValue()));
    }

    @Test
    void upAndDownLogTheMigrationName() {
        var orm = initTestDb();
        MigrationsFile1 migrations = new MigrationsFile1();
        orm.withMigrations(migrations);
        List<String> messages = new ArrayList<>();
        orm.withLogger((level, msg, args) -> messages.add(level + " " + msg));

        migrations.up();
        migrations.down();

        assertTrue(messages.contains("INFO Applying migration migration1"), messages.toString());
        assertTrue(messages.contains("INFO Backing out migration migration1"), messages.toString());
    }

    // the console output also has log lines, so find the row for one migration
    private static String showRow(String output, String name) {
        return output.lines().filter(line -> line.startsWith(name + " ")).findFirst().orElseThrow();
    }

    private static String runConsole(Migrations migrations, String input) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            System.setOut(new PrintStream(out));
            migrations.console();
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        return out.toString();
    }

    @Test
    void consoleWorksWithoutATerminal() {
        var orm = initTestDb();
        MigrationFileForConsoleTesting migrations = new MigrationFileForConsoleTesting();
        orm.withMigrations(migrations);

        // no "exit", so the console must also stop at the end of input
        String out = runConsole(migrations, "help\nall\n");

        assertStringContains(out, "Migrations Commands");
        assertEquals(3, orm.find(PikaMigration.class).count());
    }

    /*
     * A way to play around w/ the command line
     */
    public static void main(String[] args) throws IOException {
        var orm = initTestDb();
        MigrationFileForConsoleTesting migrations = new MigrationFileForConsoleTesting();
        orm.withMigrations(migrations);
        migrations.console();
    }
}
