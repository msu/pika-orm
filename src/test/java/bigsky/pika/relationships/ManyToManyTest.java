package bigsky.pika.relationships;

import bigsky.pika.TestBase;
import bigsky.pika.relationships.models.Course;
import bigsky.pika.relationships.models.Enrollment;
import bigsky.pika.relationships.models.Student;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ManyToManyTest extends TestBase {

    @Test
    void testBasicManyToManyRelationship() throws IOException {
        var orm = initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student student = new Student();
        student.setName("Alice");
        orm.insert(student);

        Course math = new Course();
        math.setTitle("Mathematics");
        orm.insert(math);

        Course science = new Course();
        science.setTitle("Science");
        orm.insert(science);

        student.getCourses().addAndSave(math);
        student.getCourses().addAndSave(science);

        assertEquals(2, student.getCourses().toList().size());
        assertEquals(1, math.getStudents().toList().size());
    }

    @Test
    void testBidirectionalManyToMany() throws IOException {
        initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student alice = new Student();
        alice.setName("Alice");
        alice.save();

        Student bob = new Student();
        bob.setName("Bob");
        bob.save();

        Course math = new Course();
        math.setTitle("Mathematics");
        math.save();

        math.getStudents().addAndSave(alice);
        math.getStudents().addAndSave(bob);

        assertEquals(1, alice.getCourses().toList().size());
        assertEquals(1, bob.getCourses().toList().size());
        assertEquals(2, math.getStudents().toList().size());
    }

    @Test
    void testAddMethodReturnsJoinObject() throws IOException {
        initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student student = new Student();
        student.setName("Charlie");
        student.save();

        Course course = new Course();
        course.setTitle("History");
        course.save();

        Enrollment enrollment = student.getCourses().addAndSave(course);

        assertNotNull(enrollment);
        assertEquals(student.getStudentId(), enrollment.getStudentId());
        assertEquals(course.getCourseId(), enrollment.getCourseId());
        assertNotNull(enrollment.getEnrollmentId());
    }

    @Test
    void testJoinTableWithAdditionalData() throws IOException {
        initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student student = new Student();
        student.setName("Diana");
        student.save();

        Course course = new Course();
        course.setTitle("Physics");
        course.save();

        Enrollment enrollment = student.getCourses().addAndSave(course);
        enrollment.setGrade("A");
        enrollment.save();

        // Reload and verify
        Enrollment found = Enrollment.find().firstWhere("student_id=:sid AND course_id=:cid",
            "sid", student.getStudentId(), "cid", course.getCourseId());
        assertEquals("A", found.getGrade());
    }

    @Test
    void testReloadAfterModification() throws IOException {
        var orm = initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student student = new Student();
        student.setName("Eve");
        student.save();

        Course course1 = new Course();
        course1.setTitle("Biology");
        course1.save();

        Course course2 = new Course();
        course2.setTitle("Chemistry");
        course2.save();

        student.getCourses().addAndSave(course1);
        assertEquals(1, student.getCourses().toList().size());

        student.getCourses().addAndSave(course2);
        assertEquals(2, student.getCourses().toList().size());
    }

    @Test
    void testFindByIdThroughRelationship() throws IOException {
        initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student student = new Student();
        student.setName("Frank");
        student.save();

        Course course = new Course();
        course.setTitle("Art");
        course.save();

        student.getCourses().addAndSave(course);
        Course found = student.getCourses().findById(course.getCourseId());
        assertNotNull(found);
        assertEquals("Art", found.getTitle());
    }

    @Test
    void testWhereClauseOnRelationship() throws IOException {
        initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student student = new Student();
        student.setName("Grace");
        student.save();

        Course math = new Course();
        math.setTitle("Mathematics");
        math.save();

        Course art = new Course();
        art.setTitle("Art");
        art.save();

        student.getCourses().addAndSave(math);
        student.getCourses().addAndSave(art);

        var mathCourses = student.getCourses()
            .where("courses.title = :title", "title", "Mathematics")
            .fetch();

        assertEquals(1, mathCourses.toList().size());
        assertEquals("Mathematics", mathCourses.toList().get(0).getTitle());
    }

    @Test
    void testAddUnsavedObjectThrowsException() throws IOException {
        initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student student = new Student();
        student.setName("Henry");
        student.save();

        Course unsavedCourse = new Course();
        unsavedCourse.setTitle("Unsaved");

        assertThrows(IllegalStateException.class, () -> {
            student.getCourses().addAndSave(unsavedCourse);
        });
    }

    @Test
    void testAddToUnsavedParentThrowsException() throws IOException {
        initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student unsavedStudent = new Student();
        unsavedStudent.setName("Irene");

        Course course = new Course();
        course.setTitle("Music");
        course.save();

        assertThrows(IllegalStateException.class, () -> {
            unsavedStudent.getCourses().addAndSave(course);
        });
    }

    @Test
    void testMultipleManyToManyRelationships() throws IOException {
        initTestDb(Student.DDL, Course.DDL, Enrollment.DDL);

        Student student1 = new Student();
        student1.setName("Jack");
        student1.save();

        Student student2 = new Student();
        student2.setName("Kate");
        student2.save();

        Course course = new Course();
        course.setTitle("Programming");
        course.save();

        student1.getCourses().addAndSave(course);
        student2.getCourses().addAndSave(course);

        assertEquals(1, student1.getCourses().toList().size());
        assertEquals(1, student2.getCourses().toList().size());
        assertEquals(2, course.getStudents().toList().size());
    }
}