package grug.db.web;

import grug.db.GrugORM;
import org.jetbrains.annotations.NotNull;

public class WebAppMigrations extends GrugORM.Migrations {

    @Override
    public void migrations() {
        add(this::initialTodoSchema);
    }

    @NotNull
    public GrugMigration initialTodoSchema() {
        return makeMigration("Todo Schema")
                .up("""
                        CREATE TABLE IF NOT EXISTS todo (
                            id INTEGER PRIMARY KEY,
                            title TEXT,
                            description TEXT,
                            due_date TEXT,
                            completed INTEGER
                        );""")
                .down("DROP TABLE todo");
    }
}
