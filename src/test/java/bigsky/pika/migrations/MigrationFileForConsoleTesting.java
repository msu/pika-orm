package bigsky.pika.migrations;

public class MigrationFileForConsoleTesting extends Migrations {

    @Override
    public void migrations()
    {
        add(this::addMigrationDemoTable);
        add(this::addFooTable);
        add(this::addBarTable);
    }

    public PikaMigration addBarTable() {
        return makeMigration("add bar table")
                .up("""
                        CREATE TABLE IF NOT EXISTS bars (
                            id INTEGER PRIMARY KEY,
                            str_val TEXT
                        );
                        """)
                .down("""
                        DROP TABLE bars;
                        """);
    }

    public PikaMigration addFooTable() {
        return makeMigration("add foo table")
                .up("""
                        CREATE TABLE IF NOT EXISTS foos (
                            id INTEGER PRIMARY KEY,
                            str_val TEXT
                        );
                        """)
                .down("""
                        DROP TABLE foos;
                        """);
    }

    public PikaMigration addMigrationDemoTable() {
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
