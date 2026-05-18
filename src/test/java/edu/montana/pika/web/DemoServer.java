package edu.montana.pika.web;

import edu.montana.pika.PikaORM;
import io.javalin.Javalin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.montana.pika.logging.PikaLogger.Level.TRACE;

public class DemoServer {

    public static void main(String[] args) throws Exception {

        ensureDbDir();

        // init the ORM; migrations are idempotent so this is safe on every restart
        PikaORM orm = new PikaORM("jdbc:sqlite:test/web.db")
                .withLogLevel(TRACE)
                .makeDefaultORM()
                .withMigrations(new WebAppMigrations())
                .applyMigrations();

        // seed demo data only when the table is empty so restarts don't duplicate it
        if (Todo.find().count() == 0) {
            orm.insertAll(new Todo("Todo 1", "This is todo 1"),
                    new Todo("Todo 2", "This is todo 2"),
                    new Todo("Todo 3", "This is todo 3"));
        }

        // create the web app
        var app = Javalin.create()
                .get("/", ctx -> ctx.html(renderTodos()))
                .post("/", ctx -> {
                    Todo newTodo = new Todo(ctx.formParam("title"), ctx.formParam("description"));
                    if (newTodo.save()) {
                        ctx.redirect("/todo/" + newTodo.getId());
                    } else {
                        // TODO finish CRUD app
                    }
                })
                .start(7070);
    }

    private static void ensureDbDir() throws IOException {
        Files.createDirectories(Path.of("test"));
    }

    private static String renderTodos() {
        StringBuilder sb = new StringBuilder("<html><body><h2>Todos</h2><ul>");
        for (Todo todo : Todo.find().all()) {
            sb.append("<li><a href='/todo/").append(todo.getId()).append("'>")
                    .append(todo.getTitle())
                    .append(" - ")
                    .append(todo.getDescription())
                    .append("</a></li>");
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }


}
