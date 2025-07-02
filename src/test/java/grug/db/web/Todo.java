package grug.db.web;

import grug.db.GrugORM;
import grug.db.GrugORM.EnterpriseGrugBean;
import grug.db.GrugORM.GrugFinder;

import java.util.Date;

public class Todo extends EnterpriseGrugBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS todo (
                id INTEGER PRIMARY KEY,
                title TEXT,
                description TEXT,
                due_date TEXT,
                completed INTEGER
            );
            """;
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

    public static GrugFinder<Todo> find() {
        return orm().find(Todo.class);
    }

}
