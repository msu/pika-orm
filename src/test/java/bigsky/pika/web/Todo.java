package bigsky.pika.web;

import bigsky.pika.PikaORM.EnterprisePikaBean;
import bigsky.pika.PikaORM.PikaClassFinder;

import java.util.Date;

public class Todo extends EnterprisePikaBean {

    Long id;
    String title;
    String description;
    Date dueDate;
    Boolean completed;

    public Todo(){}

    public Todo(String title, String description) {
        this.title = title;
        this.description = description;
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

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public static PikaClassFinder<Todo> find() {
        return find(Todo.class);
    }

}
