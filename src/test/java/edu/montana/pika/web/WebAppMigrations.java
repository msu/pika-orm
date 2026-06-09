package edu.montana.pika.web;

import edu.montana.pika.migrations.Migrations;
import org.jetbrains.annotations.NotNull;

public class WebAppMigrations extends Migrations {

    @Override
    public void migrations() {
        add(this::initialTodoSchema);
        add(this::addVersionColumn);
        add(this::addCreatedAtColumn);
        add(this::createCommentsTable);
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

    @NotNull
    public PikaMigration addVersionColumn() {
        return makeMigration("Todo Optimistic Version")
                .up("ALTER TABLE todos ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                .down("");
    }

    @NotNull
    public PikaMigration addCreatedAtColumn() {
        return makeMigration("Todo Created At")
                .up("ALTER TABLE todos ADD COLUMN created_at TEXT")
                .down("");
    }

    @NotNull
    public PikaMigration createCommentsTable() {
        return makeMigration("Comments Table")
                .up("""
                        CREATE TABLE IF NOT EXISTS comments (
                            id INTEGER PRIMARY KEY,
                            todo_id INTEGER NOT NULL,
                            body TEXT NOT NULL,
                            created_at TEXT,
                            FOREIGN KEY (todo_id) REFERENCES todos(id)
                        );""")
                .down("DROP TABLE comments");
    }
}
