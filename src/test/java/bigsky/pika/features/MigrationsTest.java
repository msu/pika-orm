package bigsky.pika.features;

import bigsky.pika.PikaORM;
import bigsky.pika.migrations.Migrations.PikaMigration;
import bigsky.pika.migrations.Migrations.MigrationStatus;
import bigsky.pika.TestBase;
import bigsky.pika.migrations.MigrationDemoModel;
import bigsky.pika.migrations.MigrationsFile1;
import bigsky.pika.migrations.MultiStatementMigration;
import bigsky.pika.migrations.MigrationFileForConsoleTesting;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

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
