package edu.montana.pika.web;


import edu.montana.pika.bean.EnterprisePikaBean;
import edu.montana.pika.bean.PikaManyRelation;
import edu.montana.pika.query.PikaClassFinder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Todo extends EnterprisePikaBean {

    Long id;
    String title;
    String description;
    String dueDate;
    Boolean completed;
    long version;
    String createdAt;

    public Todo(){
        this.completed = false;
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public Todo(String title, String description) {
        this();
        this.title = title;
        this.description = description;
    }

    @Override
    protected void validation() {
        if (title == null || title.trim().isEmpty()) {
            addError("title", "Title is required");
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public long getVersion() {
        return version;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public PikaManyRelation<Comment> getComments() {
        return loadMany(Comment.class);
    }

    public static PikaClassFinder<Todo> find() {
        return find(Todo.class);
    }

}
