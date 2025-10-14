package bigsky.pika.customization;

import bigsky.pika.TestBase;
import bigsky.pika.customization.model.HasUUID;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UUIDTest extends TestBase {

    @Test
    void uuidIsGeneratedAndSet() {
        var orm = initTestDb(HasUUID.DDL);
        var hasUUID = new HasUUID();
        assertNull(hasUUID.getUUID());
        long id = orm.insert(hasUUID);
        assertNotNull(hasUUID.getUUID());
    }

    @Test
    void uuidIsGeneratedAndSaved() {
        var orm = initTestDb(HasUUID.DDL);
        var hasUUID = new HasUUID();
        assertNull(hasUUID.getUUID());
        long id = orm.insert(hasUUID);

        HasUUID hasUUID2 = orm.find(HasUUID.class).byId(id);
        assertNotNull(hasUUID2.getUUID());
        assertEquals(hasUUID.getUUID(), hasUUID2.getUUID());
    }

    @Test
    void existingUUIDIsNotModified() {
        var orm = initTestDb(HasUUID.DDL);
        var hasUUID = new HasUUID();
        String preSetUUID = UUID.randomUUID().toString();
        hasUUID.setUUID(preSetUUID);
        long id = orm.insert(hasUUID);
        assertEquals(preSetUUID, hasUUID.getUUID());
    }

}
