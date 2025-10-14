package bigsky.pika.relationships.models;

import bigsky.pika.PikaORM;

public class Student extends PikaORM.EnterprisePikaBean {
    Long id;
    String name;

    public Long getStudentId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PikaORM.PikaManyThroughQuery<Enrollment, Course> getCourses() {
        return loadManyThrough(Enrollment.class, Course.class);
    }

    public static PikaORM.PikaClassFinder<Student> find() {
        return find(Student.class);
    }

    public static final String DDL = """
            CREATE TABLE students (
                id INTEGER PRIMARY KEY,
                name VARCHAR(100) NOT NULL
            );
            """;
}
