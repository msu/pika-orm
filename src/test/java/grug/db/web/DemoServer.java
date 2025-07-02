package grug.db.web;

import grug.db.GrugORM;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import static grug.db.TestBase.initTestDb;

public class DemoServer {

    public static void main(String[] args) {

        var orm = initTestDb(Todo.DDL);

        orm.insertAll(new Todo("Todo 1", "This is todo 1"),
                new Todo("Todo 2", "This is todo 2"),
                new Todo("Todo 3", "This is todo 3"));

        var app = Javalin.create(/*config*/)
                .get("/", context -> context.html(renderTodos()))
                .start(7070);
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
