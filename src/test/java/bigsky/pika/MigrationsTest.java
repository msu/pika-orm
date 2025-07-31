package bigsky.pika;

import bigsky.pika.PikaORM.Migrations.PikaMigration;
import bigsky.pika.PikaORM.Migrations.MigrationStatus;
import bigsky.pika.migrations.MigrationDemoModel;
import bigsky.pika.migrations.MigrationsFile1;
import bigsky.pika.migrations.MultiStatementMigration;
import bigsky.pika.migrations.MigrationFileForConsoleTesting;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(MigrationStatus.APPLIED, migrationsInDb.getFirst().getStatus());
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
        assertEquals("foo", demoModel.getFirst().getStrVal());
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
