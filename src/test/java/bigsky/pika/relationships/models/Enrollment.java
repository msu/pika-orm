package bigsky.pika.relationships.models;

import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.query.PikaClassFinder;

public class Enrollment extends EnterprisePikaBean {
    Long id;
    Long studentId;
    Long courseId;
    String grade;

    public Long getEnrollmentId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public static PikaClassFinder<Enrollment> find() {
        return find(Enrollment.class);
    }

    public static final String DDL = """
            CREATE TABLE enrollments (
                id INTEGER PRIMARY KEY,
                student_id INTEGER NOT NULL,
                course_id INTEGER NOT NULL,
                grade VARCHAR(2)
            );
            """;
}
