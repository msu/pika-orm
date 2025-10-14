package bigsky.pika.relationships;

import bigsky.pika.TestBase;
import bigsky.pika.relationships.models.Foo;
import bigsky.pika.relationships.models.FooContainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OnetoNTest extends TestBase {

    @Test
    void testBasicOnetoNRelationship() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer fooContainer = new FooContainer();
        orm.insert(fooContainer);
        for (int i = 0; i < 10; i++) {
            Foo newFoo = new Foo();
            newFoo.setFooContainerId(fooContainer.getId());
            orm.insert(newFoo);
        }

        List<Foo> foos = fooContainer.getFoos().toList();
        assertEquals(10, foos.size());
    }

    @Test
    void testBasicNtoOneRelationship() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer fooContainer = new FooContainer();
        orm.insert(fooContainer);

        Foo newFoo = new Foo();
        newFoo.setFooContainerId(fooContainer.getId());
        orm.insert(newFoo);

        FooContainer fromDb = newFoo.getFooContainer();

        assertEquals(fooContainer.getId(), fromDb.getId());

    }

    @Test
    void testCanAddElementTo() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer fooContainer = new FooContainer();
        orm.insert(fooContainer);
        for (int i = 0; i < 10; i++) {
            Foo newFoo = new Foo();
            fooContainer.getFoos().addAndSave(newFoo);
        }

        List<Foo> foos = fooContainer.getFoos().toList();
        assertEquals(10, foos.size());
    }

    @Test
    void testCanAddElementToAfter() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer fooContainer = new FooContainer();
        orm.insert(fooContainer);
        for (int i = 0; i < 10; i++) {
            Foo newFoo = new Foo();
            fooContainer.getFoos().addAndSave(newFoo);
        }

        List<Foo> foos = fooContainer.getFoos().toList();
        assertEquals(10, foos.size());

        fooContainer.getFoos().addAndSave(new Foo());
        foos = fooContainer.getFoos().toList();
        assertEquals(11, foos.size());
    }

    @Test
    void testCanAddDirectly() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer fooContainer = new FooContainer();
        orm.insert(fooContainer);
        for (int i = 0; i < 10; i++) {
            Foo foo = fooContainer.getFoos().create();
            orm.insert(foo);
        }

        List<Foo> foos = fooContainer.getFoos().toList();
        assertEquals(10, foos.size());
    }

    @Test
    void testEmptyRelationship() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer fooContainer = new FooContainer();
        orm.insert(fooContainer);

        List<Foo> foos = fooContainer.getFoos().toList();
        assertEquals(0, foos.size());
    }

    @Test
    void testMultipleContainersWithChildren() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer container1 = new FooContainer();
        orm.insert(container1);

        FooContainer container2 = new FooContainer();
        orm.insert(container2);

        for (int i = 0; i < 5; i++) {
            Foo foo = new Foo();
            foo.setFooContainerId(container1.getId());
            orm.insert(foo);
        }

        for (int i = 0; i < 3; i++) {
            Foo foo = new Foo();
            foo.setFooContainerId(container2.getId());
            orm.insert(foo);
        }

        assertEquals(5, container1.getFoos().toList().size());
        assertEquals(3, container2.getFoos().toList().size());
    }

    @Test
    void testReloadAfterModification() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer fooContainer = new FooContainer();
        orm.insert(fooContainer);

        fooContainer.getFoos().addAndSave(new Foo());
        assertEquals(1, fooContainer.getFoos().totalCount());

        fooContainer.getFoos().addAndSave(new Foo());
        assertEquals(2, fooContainer.getFoos().totalCount());

        fooContainer.getFoos().addAndSave(new Foo());
        assertEquals(3, fooContainer.getFoos().totalCount());
    }

    @Test
    void testNtoOneReturnsCorrectParent() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer container1 = new FooContainer();
        orm.insert(container1);

        FooContainer container2 = new FooContainer();
        orm.insert(container2);

        Foo foo1 = new Foo();
        foo1.setFooContainerId(container1.getId());
        orm.insert(foo1);

        Foo foo2 = new Foo();
        foo2.setFooContainerId(container2.getId());
        orm.insert(foo2);

        assertEquals(container1.getId(), foo1.getFooContainer().getId());
        assertEquals(container2.getId(), foo2.getFooContainer().getId());
        assertNotEquals(foo1.getFooContainer().getId(), foo2.getFooContainer().getId());
    }

    @Test
    void testCreateMethodSetsRelationship() {
        var orm = initTestDb(FooContainer.DDL, Foo.DDL);

        FooContainer fooContainer = new FooContainer();
        orm.insert(fooContainer);

        Foo foo = fooContainer.getFoos().create();
        orm.insert(foo);

        FooContainer parent = foo.getFooContainer();
        assertNotNull(parent);
        assertEquals(fooContainer.getId(), parent.getId());
    }

}
