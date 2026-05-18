package edu.montana.pika.migrations;

public class MigrationsFile1 extends Migrations {

    @Override
    public void migrations() {
        add(this::addMigrationDemoTable);
    }

    public PikaMigration addMigrationDemoTable() {
        return makeMigration("migration1")
                .up("""
                        CREATE TABLE IF NOT EXISTS migration_demo_models (
                            id INTEGER PRIMARY KEY,
                            str_val TEXT,
                            int_val INTEGER,
                            bool_val BOOLEAN,
                            date_val DATETIME
                        );
                        """)
                .down("""
                        DROP TABLE migration_demo_models;
                        """);
    }


}
