package bigsky.pika.web;

import bigsky.pika.PikaORM;
import io.javalin.Javalin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static bigsky.pika.logging.PikaLogger.Level.TRACE;

public class DemoServer {

    public static void main(String[] args) throws Exception {

        clearOldDb();

        // init the ORM
        PikaORM orm = new PikaORM("jdbc:sqlite:test/web.db")
                .withLogLevel(TRACE)
                .makeDefaultORM()
                .withMigrations(new WebAppMigrations())
                .applyMigrations();

        // insert some data
        orm.insertAll(new Todo("Todo 1", "This is todo 1"),
                new Todo("Todo 2", "This is todo 2"),
                new Todo("Todo 3", "This is todo 3"));

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

    private static void clearOldDb() throws IOException {
        // remove old db if it exists
        Path path = Path.of("test", "web.db");
        if (Files.exists(path)) {
            Files.delete(path);
        }
        path.toFile().getParentFile().mkdirs();
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
