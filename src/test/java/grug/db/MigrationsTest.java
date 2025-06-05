package grug.db;

import grug.db.GrugORM.Migrations.Migration;
import grug.db.GrugORM.Migrations.MigrationStatus;
import grug.db.migrations.MigrationDemoModel;
import grug.db.migrations.MigrationsFile1;
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

        var migrationsInDb = orm.findAll(Migration.class);

        assertEquals(1, migrationsInDb.size());
        assertEquals(MigrationStatus.APPLIED, migrationsInDb.getFirst().getStatus());
    }


    public static void main(String[] args) throws IOException {
        var orm = initDBFileAndORM();
        MigrationFileForConsoleTesting migrations = new MigrationFileForConsoleTesting();
        orm.withMigrations(migrations);
        migrations.console();
    }
}
