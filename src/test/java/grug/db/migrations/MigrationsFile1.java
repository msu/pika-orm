package grug.db.migrations;

import grug.db.GrugORM;

public class MigrationsFile1 extends GrugORM.Migrations {

    @Override
    public void migrations() {
        add(this::addMigrationDemoTable);
    }

    public GrugMigration addMigrationDemoTable() {
        return makeMigration("migration1")
                .up("""
                        CREATE TABLE IF NOT EXISTS migration_demo_models (
                            id INTEGER PRIMARY KEY,
                            str_val TEXT,
                            int_val INTEGER,
                            bool_val INTEGER,
                            date_val INTEGER
                        );
                        """)
                .down("""
                        DROP TABLE migration_demo_models;
                        """);
    }


}
