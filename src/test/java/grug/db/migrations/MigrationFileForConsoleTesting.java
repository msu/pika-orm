package grug.db.migrations;

import grug.db.GrugORM;

public class MigrationFileForConsoleTesting extends GrugORM.Migrations {

    @Override
    public void migrations()
    {
        add(this::addMigrationDemoTable);
        add(this::addFooTable);
        add(this::addBarTable);
    }

    public GrugMigration addBarTable() {
        return makeMigration("add bar table")
                .up("""
                        CREATE TABLE IF NOT EXISTS bar (
                            id INTEGER PRIMARY KEY,
                            str_val TEXT
                        );
                        """)
                .down("""
                        DROP TABLE bar;
                        """);
    }

    public GrugMigration addFooTable() {
        return makeMigration("add foo table")
                .up("""
                        CREATE TABLE IF NOT EXISTS foo (
                            id INTEGER PRIMARY KEY,
                            str_val TEXT
                        );
                        """)
                .down("""
                        DROP TABLE foo;
                        """);
    }

    public GrugMigration addMigrationDemoTable() {
        return makeMigration("migration1")
                .up("""
                        CREATE TABLE IF NOT EXISTS migration_demo_model (
                            id INTEGER PRIMARY KEY,
                            str_val TEXT,
                            int_val INTEGER,
                            bool_val INTEGER,
                            date_val INTEGER
                        );
                        """)
                .down("""
                        DROP TABLE migration_demo_model;
                        """);
    }


}
