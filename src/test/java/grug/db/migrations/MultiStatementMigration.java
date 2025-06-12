package grug.db.migrations;

import grug.db.GrugORM;

public class MultiStatementMigration extends GrugORM.Migrations {

    @Override
    public void migrations() {
        add(this::addMigrationDemoTable);
    }

    public Migration addMigrationDemoTable() {
        return makeMigration("migration1")
                .up("""
                        CREATE TABLE IF NOT EXISTS migration_demo_model (
                            id INTEGER PRIMARY KEY,
                            str_val TEXT,
                            int_val INTEGER,
                            bool_val INTEGER,
                            date_val INTEGER
                        );
                        INSERT INTO migration_demo_model(str_val) VALUES ('foo');
                        """)
                .down("""
                        DROP TABLE migration_demo_model;
                        """);
    }


}
