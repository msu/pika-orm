package edu.montana.pika.relationships.models;

import edu.montana.pika.bean.PikaBean;
import edu.montana.pika.query.PikaClassFinder;
import edu.montana.pika.bean.PikaManyThroughRelation;

public class Student extends PikaBean {
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

    public PikaManyThroughRelation<Enrollment, Course> getCourses() {
        return loadManyThrough(Enrollment.class, Course.class);
    }

    public static PikaClassFinder<Student> find() {
        return find(Student.class);
    }

    public static final String DDL = """
            CREATE TABLE students (
                id INTEGER PRIMARY KEY,
                name VARCHAR(100) NOT NULL
            );
            """;
}
