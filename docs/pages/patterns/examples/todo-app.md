---
layout: default
title: "Todo App (Javalin)"
description: "A small end-to-end Todo web app: PikaORM beans and CRUD with paging, served by Javalin."
active_page: todo-app
permalink: /pages/todo-app/
---

# Todo App (Javalin)

A working todo list backed by a SQLite database: add todos, check them off, delete them, and page through them. It is three short files and no DAO layer, no config, no annotations.

You write the table as a migration, the row as a `Todo` bean, and four routes wired to [Javalin](https://javalin.io). Because the bean persists and validates itself, the route handlers stay a few lines each.

Add Javalin and a JDBC driver alongside PikaORM (see [Get Started]({{ '/pages/get-started/' | relative_url }})):

```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>6.7.0</version>
</dependency>
```

## 1. The schema

```java
public class TodoMigrations extends Migrations {
    @Override
    public void migrations() {
        add(this::createTodos);
    }

    public PikaMigration createTodos() {
        return makeMigration("001_create_todos")
                .up("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id          INTEGER PRIMARY KEY,
                        title       TEXT,
                        description TEXT,
                        completed   INTEGER
                    );""")
                .down("DROP TABLE todos;");
    }
}
```

## 2. The bean

```java
import edu.montana.pika.bean.PikaBean;
import edu.montana.pika.query.PikaClassFinder;

public class Todo extends PikaBean {
    Long id;
    String title;
    String description;
    Boolean completed = false;

    public Todo() {}

    public Long getId()            { return id; }
    public String getTitle()       { return title; }
    public Boolean getCompleted()  { return completed; }
    public void setCompleted(Boolean c) { this.completed = c; }

    @Override
    protected void validation() {
        require("title");
    }

    public static PikaClassFinder<Todo> find() {
        return find(Todo.class);
    }
}
```

## 3. The app

```java
import edu.montana.pika.PikaORM;
import edu.montana.pika.query.PikaClassQuery;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class TodoApp {

    static final int PAGE_SIZE = 10;

    public static void main(String[] args) {
        new PikaORM("jdbc:sqlite:todo.db")
                .makeDefaultORM()
                .withMigrations(new TodoMigrations())
                .applyMigrations();

        Javalin.create()
                .get("/", TodoApp::list)
                .post("/todos", TodoApp::create)
                .post("/todos/{id}/toggle", TodoApp::toggle)
                .post("/todos/{id}/delete", TodoApp::delete)
                .start(7070);
    }

    // GET / -- one page of todos
    static void list(Context ctx) {
        long page = ctx.queryParamAsClass("page", Long.class).getOrDefault(1L);

        PikaClassQuery<Todo> query = Todo.find()
                .orderBy("id")
                .page(page)
                .pageSize(PAGE_SIZE);

        ctx.html(render(query.fetchList(), page, query.hasPreviousPage(), query.hasNextPage()));
    }

    // POST /todos -- create from the form
    static void create(Context ctx) {
        Todo todo = new Todo();
        todo.setFieldsFrom(ctx::formParam, "title", "description");  // only these fields
        todo.save();                                                 // validates, then INSERTs
        ctx.redirect("/");
    }

    // POST /todos/{id}/toggle -- flip completed
    static void toggle(Context ctx) {
        Todo todo = Todo.find().byId(Long.parseLong(ctx.pathParam("id")));
        if (todo != null) {
            todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
            todo.save();
        }
        ctx.redirect("/");
    }

    // POST /todos/{id}/delete
    static void delete(Context ctx) {
        Todo todo = Todo.find().byId(Long.parseLong(ctx.pathParam("id")));
        if (todo != null) todo.delete();
        ctx.redirect("/");
    }

    // Minimal HTML. A real app would escape user input and use a template.
    static String render(java.util.List<Todo> todos, long page, boolean prev, boolean next) {
        StringBuilder sb = new StringBuilder("""
                <form method="post" action="/todos">
                  <input name="title" placeholder="What needs doing?">
                  <button>Add</button>
                </form>
                <ul>
                """);
        for (Todo t : todos) {
            sb.append("<li>")
              .append("<form method=post action=/todos/").append(t.getId()).append("/toggle>")
              .append("<button>").append(Boolean.TRUE.equals(t.getCompleted()) ? "[x]" : "[ ]").append("</button></form> ")
              .append(t.getTitle())
              .append(" <form method=post action=/todos/").append(t.getId()).append("/delete><button>x</button></form>")
              .append("</li>");
        }
        sb.append("</ul><div>");
        if (prev) sb.append("<a href='/?page=").append(page - 1).append("'>prev</a> ");
        sb.append("page ").append(page);
        if (next) sb.append(" <a href='/?page=").append(page + 1).append("'>next</a>");
        return sb.append("</div>").toString();
    }
}
```

Run it and open [http://localhost:7070](http://localhost:7070).

See [Paging]({{ '/pages/paging/' | relative_url }}) for the paging API and [Validation & Forms]({{ '/pages/validation/' | relative_url }}) for `setFieldsFrom`.

## Full version

The repo ships a larger TodoMVC build of this with search, filters, comments, and optimistic-concurrency conflict handling:

- [`TodoMVCDemo.java`](https://github.com/msu/pika-orm/blob/main/src/test/java/edu/montana/pika/web/TodoMVCDemo.java)
- [`Todo.java`](https://github.com/msu/pika-orm/blob/main/src/test/java/edu/montana/pika/web/Todo.java)
- [`WebAppMigrations.java`](https://github.com/msu/pika-orm/blob/main/src/test/java/edu/montana/pika/web/WebAppMigrations.java)
