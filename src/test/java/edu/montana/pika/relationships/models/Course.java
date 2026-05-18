package edu.montana.pika.relationships.models;

import edu.montana.pika.bean.EnterprisePikaBean;
import edu.montana.pika.query.PikaClassFinder;
import edu.montana.pika.bean.PikaManyThroughRelation;

public class Course extends EnterprisePikaBean {
    Long id;
    String title;

    public Long getCourseId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PikaManyThroughRelation<Enrollment, Student> getStudents() {
        return loadManyThrough(Enrollment.class, Student.class);
    }

    public static PikaClassFinder<Course> find() {
        return find(Course.class);
    }

    public static final String DDL = """
            CREATE TABLE courses (
                id INTEGER PRIMARY KEY,
                title VARCHAR(100) NOT NULL
            );
            """;
}
