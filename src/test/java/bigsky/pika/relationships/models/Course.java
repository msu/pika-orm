package bigsky.pika.relationships.models;

import bigsky.pika.PikaORM;
import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.query.PikaClassFinder;
import bigsky.pika.query.PikaManyThroughQuery;

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

    public PikaManyThroughQuery<Enrollment, Student> getStudents() {
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
