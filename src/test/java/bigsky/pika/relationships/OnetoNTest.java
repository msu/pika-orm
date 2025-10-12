package bigsky.pika.relationships;

import bigsky.pika.TestBase;
import bigsky.pika.models.Foo;
import bigsky.pika.models.FooContainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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


}
