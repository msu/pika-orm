package edu.montana.pika.web;

import edu.montana.pika.PikaORM;
import edu.montana.pika.query.PikaClassQuery;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.montana.pika.logging.PikaLogger.Level.DEBUG;

public class TodoMVCDemo {

    private static final int PAGE_SIZE = 5;

    public static void main(String[] args) throws Exception {
        ensureDbDir();

        PikaORM orm = new PikaORM("jdbc:sqlite:test/web.db")
                .withLogLevel(DEBUG)
                .makeDefaultORM()
                .withMigrations(new WebAppMigrations())
                .applyMigrations();

        if (Todo.find().count() == 0) {
            orm.insertAll(
                    completed(new Todo("Read the PikaORM README", "Skim the quick start")),
                    new Todo("Wire up TodoMVC routes", "Show off CRUD, filters, transactions"),
                    new Todo("Ship it", "Demo to the team"));
        }

        var app = Javalin.create()
                .get("/", TodoMVCDemo::index)
                .get("/stats", TodoMVCDemo::stats)
                .get("/todos/{id}", TodoMVCDemo::detail)
                .post("/todos", TodoMVCDemo::create)
                .post("/todos/{id}/toggle", TodoMVCDemo::toggle)
                .post("/todos/{id}/edit", TodoMVCDemo::edit)
                .post("/todos/{id}/delete", TodoMVCDemo::delete)
                .post("/todos/{id}/comments", TodoMVCDemo::addComment)
                .post("/todos/clear-completed", TodoMVCDemo::clearCompleted)
                .post("/todos/toggle-all", TodoMVCDemo::toggleAll)
                .start(7070);

        System.out.println("TodoMVC running at http://localhost:" + app.port());
    }

    private static Todo completed(Todo t) {
        t.setCompleted(true);
        return t;
    }

    // ---- routes ----

    private static void index(Context ctx) {
        String filter = ctx.queryParamAsClass("filter", String.class).getOrDefault("all");
        String q = ctx.queryParamAsClass("q", String.class).getOrDefault("");
        long pageNum = ctx.queryParamAsClass("page", Long.class).getOrDefault(1L);
        String error = ctx.queryParam("error");

        List<String> conditions = new ArrayList<>();
        Map<String, Object> args = new HashMap<>();
        switch (filter) {
            case "active" -> {
                conditions.add("(completed = :c OR completed IS NULL)");
                args.put("c", false);
            }
            case "completed" -> {
                conditions.add("completed = :c");
                args.put("c", true);
            }
        }
        if (!q.isEmpty()) {
            conditions.add("title LIKE :q");
            args.put("q", "%" + q + "%");
        }
        String whereClause = conditions.isEmpty() ? "1=1" : String.join(" AND ", conditions);

        PikaClassQuery<Todo> query = Todo.find()
                .where(whereClause, args)
                .orderBy("id")
                .page(pageNum)
                .pageSize(PAGE_SIZE);

        long total = Todo.find().count();
        long done = Todo.find().where("completed = :c", Map.of("c", true)).count();
        long active = total - done;

        ctx.html(renderIndex(query, filter, q, pageNum, active, done, error));
    }

    private static void detail(Context ctx) {
        Todo todo = findTodo(ctx);
        if (todo == null) {
            ctx.status(404).result("Not found");
            return;
        }
        boolean conflict = "1".equals(ctx.queryParam("conflict"));
        String validationError = ctx.queryParam("error");
        ctx.html(renderDetail(todo, conflict, validationError));
    }

    private static void create(Context ctx) {
        Todo todo = new Todo();
        todo.setFieldsFrom(ctx::formParam, "title", "description", "dueDate");
        if (todo.save()) {
            ctx.redirect("/todos/" + todo.getId());
        } else {
            ctx.redirect("/?error=" + url(todo.getErrorString("title")));
        }
    }

    private static void toggle(Context ctx) {
        Todo todo = findTodo(ctx);
        if (todo != null) {
            todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
            todo.save();
        }
        ctx.redirect(backTo(ctx));
    }

    private static void edit(Context ctx) {
        Todo todo = findTodo(ctx);
        if (todo == null) {
            ctx.redirect("/");
            return;
        }
        // version is intentionally part of the allowlist so the form's snapshot
        // version is restored before save(); PikaORM then verifies it matches the DB.
        todo.setFieldsFrom(ctx::formParam, "title", "description", "dueDate", "version");
        if (todo.save()) {
            ctx.redirect("/todos/" + todo.getId());
        } else if (todo.hasErrors()) {
            ctx.redirect("/todos/" + todo.getId() + "?error=" + url(todo.getErrorString("title")));
        } else {
            ctx.redirect("/todos/" + todo.getId() + "?conflict=1");
        }
    }

    private static void delete(Context ctx) {
        Todo todo = findTodo(ctx);
        if (todo != null) todo.delete();
        ctx.redirect("/");
    }

    private static void addComment(Context ctx) {
        Todo todo = findTodo(ctx);
        if (todo == null) {
            ctx.redirect("/");
            return;
        }
        Comment c = new Comment();
        c.setTodoId(todo.getId());
        c.setFieldsFrom(ctx::formParam, "body");
        c.save();
        ctx.redirect("/todos/" + todo.getId());
    }

    private static void clearCompleted(Context ctx) {
        PikaORM.get().inTransaction(() -> {
            for (Todo t : Todo.find().where("completed = :c", Map.of("c", true))) {
                t.delete();
            }
        });
        ctx.redirect(backTo(ctx));
    }

    private static void toggleAll(Context ctx) {
        PikaORM.get().inTransaction(() -> {
            long total = Todo.find().count();
            long done = Todo.find().where("completed = :c", Map.of("c", true)).count();
            boolean markAll = done < total;
            for (Todo t : Todo.find().all()) {
                t.setCompleted(markAll);
                t.save();
            }
        });
        ctx.redirect(backTo(ctx));
    }

    private static void stats(Context ctx) {
        var rows = PikaORM.get().select("""
                SELECT t.id        AS id,
                       t.title     AS title,
                       t.completed AS completed,
                       COUNT(c.id) AS comment_count
                FROM todos t
                LEFT JOIN comments c ON c.todo_id = t.id
                GROUP BY t.id
                ORDER BY comment_count DESC, t.id
                """, StatsRow.class).toList();
        ctx.html(renderStats(rows));
    }

    // ---- helpers ----

    private static Todo findTodo(Context ctx) {
        try {
            return Todo.find().byId(Long.parseLong(ctx.pathParam("id")));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String backTo(Context ctx) {
        String filter = ctx.queryParam("filter");
        String q = ctx.queryParam("q");
        String page = ctx.queryParam("page");
        StringBuilder sb = new StringBuilder("/");
        String sep = "?";
        if (filter != null) { sb.append(sep).append("filter=").append(url(filter)); sep = "&"; }
        if (q != null)      { sb.append(sep).append("q=").append(url(q));           sep = "&"; }
        if (page != null)   { sb.append(sep).append("page=").append(url(page)); }
        return sb.toString();
    }

    private static String contextQuery(String filter, String q, long pageNum) {
        StringBuilder sb = new StringBuilder();
        String sep = "?";
        if (!"all".equals(filter)) { sb.append(sep).append("filter=").append(url(filter)); sep = "&"; }
        if (!q.isEmpty())          { sb.append(sep).append("q=").append(url(q));           sep = "&"; }
        if (pageNum > 1)           { sb.append(sep).append("page=").append(pageNum); }
        return sb.toString();
    }

    // ---- rendering ----

    public record StatsRow(Long id, String title, Boolean completed, Long commentCount) {}

    private static String renderIndex(PikaClassQuery<Todo> query, String filter, String q, long pageNum,
                                      long active, long done, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append(head("todos"));
        sb.append("<h1>todos</h1>");

        if (error != null && !error.isEmpty()) {
            sb.append("<div class=\"error\">").append(esc(error)).append("</div>");
        }

        sb.append("""
                <form class="new" method="post" action="/todos">
                  <input type="text" name="title" placeholder="What needs doing?" autofocus>
                  <input type="text" name="description" placeholder="(optional notes)">
                  <button type="submit">Add</button>
                </form>
                """);

        sb.append("<form class=\"search\" method=\"get\" action=\"/\">");
        if (!"all".equals(filter)) {
            sb.append("<input type=\"hidden\" name=\"filter\" value=\"").append(esc(filter)).append("\">");
        }
        sb.append("<input type=\"text\" name=\"q\" placeholder=\"search title\" value=\"").append(esc(q)).append("\">")
                .append("<button type=\"submit\">Search</button>");
        if (!q.isEmpty()) {
            sb.append(" <a href=\"/?filter=").append(esc(filter)).append("\">clear</a>");
        }
        sb.append("</form>");

        String ctxQs = contextQuery(filter, q, pageNum);
        sb.append("<form method=\"post\" action=\"/todos/toggle-all").append(ctxQs).append("\">")
                .append("<button type=\"submit\">Toggle all</button></form>");

        List<Todo> rows = query.fetchList();

        sb.append("<ul>");
        if (rows.isEmpty()) {
            sb.append("<li><span class=\"meta\">nothing here</span></li>");
        } else {
            for (Todo t : rows) {
                boolean isDone = Boolean.TRUE.equals(t.getCompleted());
                sb.append("<li").append(isDone ? " class=\"done\"" : "").append(">");
                sb.append("<form method=\"post\" action=\"/todos/").append(t.getId()).append("/toggle").append(ctxQs).append("\">")
                        .append("<button type=\"submit\">").append(isDone ? "[x]" : "[ ]").append("</button></form>");
                sb.append("<span class=\"title\"><a href=\"/todos/").append(t.getId()).append("\">").append(esc(t.getTitle())).append("</a>");
                if (t.getDescription() != null && !t.getDescription().isEmpty()) {
                    sb.append(" <span class=\"meta\">").append(esc(t.getDescription())).append("</span>");
                }
                sb.append("</span>");
                sb.append("<form method=\"post\" action=\"/todos/").append(t.getId()).append("/delete\">")
                        .append("<button class=\"danger\" type=\"submit\">x</button></form>");
                sb.append("</li>");
            }
        }
        sb.append("</ul>");

        // pagination strip
        sb.append("<div class=\"pager\">");
        if (query.hasPreviousPage()) {
            sb.append("<a href=\"/").append(pageUrl(filter, q, pageNum - 1)).append("\">&laquo; prev</a>");
        } else {
            sb.append("<span class=\"disabled\">&laquo; prev</span>");
        }
        sb.append("<span>page ").append(pageNum).append("</span>");
        if (query.hasNextPage()) {
            sb.append("<a href=\"/").append(pageUrl(filter, q, pageNum + 1)).append("\">next &raquo;</a>");
        } else {
            sb.append("<span class=\"disabled\">next &raquo;</span>");
        }
        sb.append("</div>");

        sb.append("<div class=\"footer\">");
        sb.append("<span>").append(active).append(" left</span>");
        sb.append("<span class=\"filters\">");
        sb.append(filterLink("all", filter, q, "All"));
        sb.append(filterLink("active", filter, q, "Active"));
        sb.append(filterLink("completed", filter, q, "Completed"));
        sb.append("</span>");
        sb.append("<form method=\"post\" action=\"/todos/clear-completed").append(ctxQs).append("\">")
                .append("<button type=\"submit\"").append(done == 0 ? " disabled" : "").append(">Clear completed (").append(done).append(")</button>")
                .append("</form>");
        sb.append("</div>");

        sb.append("<p><a href=\"/stats\">stats</a></p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String renderDetail(Todo t, boolean conflict, String validationError) {
        StringBuilder sb = new StringBuilder();
        sb.append(head("todo #" + t.getId()));
        sb.append("<p><a href=\"/\">&laquo; back</a></p>");
        sb.append("<h1>").append(esc(t.getTitle())).append("</h1>");

        if (conflict) {
            sb.append("<div class=\"error\">Another change happened first (version conflict). Reload and try again.</div>");
        }
        if (validationError != null && !validationError.isEmpty()) {
            sb.append("<div class=\"error\">").append(esc(validationError)).append("</div>");
        }

        sb.append("<form method=\"post\" action=\"/todos/").append(t.getId()).append("/edit\" class=\"edit\">");
        sb.append("<input type=\"hidden\" name=\"version\" value=\"").append(t.getVersion()).append("\">");
        sb.append("<label>title <input type=\"text\" name=\"title\" value=\"").append(esc(t.getTitle())).append("\"></label>");
        sb.append("<label>description <input type=\"text\" name=\"description\" value=\"").append(esc(t.getDescription())).append("\"></label>");
        sb.append("<label>due date <input type=\"date\" name=\"dueDate\" value=\"").append(esc(formatForDateInput(t.getDueDate()))).append("\" placeholder=\"yyyy-MM-dd\"></label>");
        sb.append("<button type=\"submit\">Save (v").append(t.getVersion()).append(")</button>");
        sb.append("</form>");

        sb.append("<p class=\"meta\">created ").append(esc(formatDate(t.getCreatedAt()))).append("</p>");

        sb.append("<h2>comments (").append(t.getComments().size()).append(")</h2>");
        sb.append("<ul class=\"comments\">");
        boolean any = false;
        for (Comment c : t.getComments()) {
            any = true;
            sb.append("<li>").append(esc(c.getBody()))
                    .append(" <span class=\"meta\">").append(esc(formatDate(c.getCreatedAt()))).append("</span></li>");
        }
        if (!any) sb.append("<li><span class=\"meta\">no comments yet</span></li>");
        sb.append("</ul>");

        sb.append("<form method=\"post\" action=\"/todos/").append(t.getId()).append("/comments\">");
        sb.append("<input type=\"text\" name=\"body\" placeholder=\"add a comment\" required>");
        sb.append("<button type=\"submit\">Add</button>");
        sb.append("</form>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String renderStats(List<StatsRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(head("stats"));
        sb.append("<p><a href=\"/\">&laquo; back</a></p>");
        sb.append("<h1>stats</h1>");
        sb.append("<p class=\"meta\">comments per todo via <code>orm.select(sql, StatsRow.class)</code></p>");
        sb.append("<table><thead><tr><th>#</th><th>title</th><th>completed</th><th>comments</th></tr></thead><tbody>");
        for (StatsRow r : rows) {
            sb.append("<tr><td>").append(r.id())
                    .append("</td><td><a href=\"/todos/").append(r.id()).append("\">").append(esc(r.title())).append("</a></td>")
                    .append("<td>").append(Boolean.TRUE.equals(r.completed()) ? "yes" : "no").append("</td>")
                    .append("<td>").append(r.commentCount()).append("</td></tr>");
        }
        sb.append("</tbody></table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String head(String title) {
        return """
                <!doctype html>
                <html><head><meta charset="utf-8"><title>PikaORM TodoMVC - %s</title>
                <style>
                  body { font: 14px system-ui, sans-serif; max-width: 720px; margin: 2rem auto; color: #222; }
                  h1 { font-weight: 200; font-size: 2.5rem; color: #b83f45; text-align: center; }
                  h2 { font-weight: 300; color: #444; }
                  form.new, form.search { display: flex; gap: .5rem; margin-bottom: 1rem; }
                  form.new input[type=text], form.search input[type=text] { flex: 1; padding: .5rem; font-size: 1rem; }
                  ul { list-style: none; padding: 0; border: 1px solid #ddd; }
                  ul.comments { background: #fafafa; }
                  li { display: flex; align-items: center; gap: .5rem; padding: .5rem .75rem; border-bottom: 1px solid #eee; }
                  li:last-child { border-bottom: none; }
                  li.done .title { text-decoration: line-through; color: #999; }
                  .title { flex: 1; }
                  .title a { color: inherit; text-decoration: none; }
                  .meta { color: #888; font-size: .85rem; margin-left: .5rem; }
                  .footer { display: flex; justify-content: space-between; align-items: center; padding: .5rem .75rem; border: 1px solid #ddd; border-top: none; font-size: .9rem; color: #555; }
                  .pager { display: flex; justify-content: center; gap: 1rem; padding: .5rem; }
                  .pager .disabled { color: #ccc; }
                  .filters a { margin: 0 .25rem; padding: 2px 6px; text-decoration: none; color: #555; border: 1px solid transparent; border-radius: 3px; }
                  .filters a.on { border-color: #ce4646; color: #b83f45; }
                  button { background: none; border: none; cursor: pointer; color: #777; }
                  button.danger { color: #b83f45; }
                  .error { background: #fdd; color: #900; padding: .5rem; border: 1px solid #f99; margin-bottom: 1rem; }
                  form.edit { display: grid; gap: .5rem; padding: 1rem; border: 1px solid #ddd; }
                  form.edit label { display: flex; gap: .5rem; align-items: center; }
                  form.edit input { flex: 1; padding: .25rem; }
                  table { width: 100%%; border-collapse: collapse; }
                  th, td { text-align: left; padding: .4rem .5rem; border-bottom: 1px solid #eee; }
                </style></head><body>
                """.formatted(esc(title));
    }

    private static String filterLink(String key, String current, String q, String label) {
        StringBuilder href = new StringBuilder("/?filter=").append(key);
        if (!q.isEmpty()) href.append("&q=").append(url(q));
        String cls = key.equals(current) ? " class=\"on\"" : "";
        return "<a href=\"" + href + "\"" + cls + ">" + label + "</a>";
    }

    private static String pageUrl(String filter, String q, long pageNum) {
        StringBuilder sb = new StringBuilder();
        String sep = "?";
        if (!"all".equals(filter)) { sb.append(sep).append("filter=").append(url(filter)); sep = "&"; }
        if (!q.isEmpty())          { sb.append(sep).append("q=").append(url(q));           sep = "&"; }
        if (pageNum > 1)           { sb.append(sep).append("page=").append(pageNum); }
        return sb.toString();
    }

    private static String formatDate(Object d) {
        return d == null ? "" : d.toString();
    }

    private static String formatForDateInput(java.util.Date d) {
        if (d == null) return "";
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String url(String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static void ensureDbDir() throws IOException {
        Files.createDirectories(Path.of("test"));
    }
}
