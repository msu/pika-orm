package bigsky.pika.relationships.models;

import bigsky.pika.PikaORM;

public class Course extends PikaORM.EnterprisePikaBean {
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

    public PikaORM.PikaManyThroughQuery<Enrollment, Student> getStudents() {
        return loadManyThrough(Enrollment.class, Student.class);
    }

    public static PikaORM.PikaClassFinder<Course> find() {
        return find(Course.class);
    }

    public static final String DDL = """
            CREATE TABLE courses (
                id INTEGER PRIMARY KEY,
                title VARCHAR(100) NOT NULL
            );
            """;
}
