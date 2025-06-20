package grug.db;

import grug.db.GrugORM.Migrations.GrugMigration;
import grug.db.GrugORM.Migrations.MigrationStatus;
import grug.db.migrations.*;
import grug.db.migrations.MigrationFileForConsoleTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MigrationsTest extends TestBase {

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
    }

    @Test
    void basicMigrationWorksEndToEnd() {
        // migrate the database
        MigrationsFile1 migrations = new MigrationsFile1();
        orm.withMigrations(migrations);
        migrations.applyAll();

        // insert a new model based on the migration
        MigrationDemoModel migrationDemoModel = new MigrationDemoModel();
        Date dateVal = new Date();
        migrationDemoModel.setDateVal(dateVal);

        long id = orm.insert(migrationDemoModel);

        MigrationDemoModel fromDB = orm.find(MigrationDemoModel.class, id);
        assertEquals(dateVal, fromDB.getDateVal());
    }

    @Test
    void basicMigrationWorks() {
        // migrate the database
        MigrationsFile1 migrations = new MigrationsFile1();
        orm.withMigrations(migrations);
        migrations.applyAll();

        var migrationsInDb = orm.findAll(GrugMigration.class);

        assertEquals(1, migrationsInDb.size());
        assertEquals(MigrationStatus.APPLIED, migrationsInDb.getFirst().getStatus());
    }

    @Test
    void multipleStatementsWork() {
        // migrate the database
        MultiStatementMigration migrations = new MultiStatementMigration();
        orm.withMigrations(migrations);
        migrations.applyAll();

        var demoModel = orm.findAll(MigrationDemoModel.class);

        assertEquals(1, demoModel.size());
        assertEquals("foo", demoModel.getFirst().getStrVal());
    }

    /*
     * A way to play around w/ the command line
     */
    public static void main(String[] args) throws IOException {
        var orm = initDBFileAndORM();
        MigrationFileForConsoleTesting migrations = new MigrationFileForConsoleTesting();
        orm.withMigrations(migrations);
        migrations.console();
    }
}
