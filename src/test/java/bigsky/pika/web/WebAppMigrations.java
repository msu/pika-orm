package bigsky.pika.web;

import bigsky.pika.migrations.Migrations;
import org.jetbrains.annotations.NotNull;

public class WebAppMigrations extends Migrations {

    @Override
    public void migrations() {
        add(this::initialTodoSchema);
    }

    @NotNull
    public PikaMigration initialTodoSchema() {
        return makeMigration("Todo Schema")
                .up("""
                        CREATE TABLE IF NOT EXISTS todos (
                            id INTEGER PRIMARY KEY,
                            title TEXT,
                            description TEXT,
                            due_date TEXT,
                            completed INTEGER
                        );""")
                .down("DROP TABLE todos");
    }
}
