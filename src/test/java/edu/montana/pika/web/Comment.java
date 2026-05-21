package edu.montana.pika.web;

import edu.montana.pika.bean.EnterprisePikaBean;
import edu.montana.pika.query.PikaClassFinder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Comment extends EnterprisePikaBean {

    Long id;
    Long todoId;
    String body;
    String createdAt;

    public Comment() {
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public Comment(Long todoId, String body) {
        this();
        this.todoId = todoId;
        this.body = body;
    }

    public Long getId() { return id; }

    public Long getTodoId() { return todoId; }
    public void setTodoId(Long todoId) { this.todoId = todoId; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getCreatedAt() { return createdAt; }

    public Todo getTodo() {
        return load(Todo.class);
    }

    public static PikaClassFinder<Comment> find() {
        return find(Comment.class);
    }

    @Override
    protected void validation() {
        if (body == null || body.trim().isEmpty()) {
            addError("body", "Body is required");
        }
        if (todoId == null) {
            addError("todoId", "todoId is required");
        }
    }
}
