package bigsky.pika;

import bigsky.pika.models.HasCustomizedMetadata;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomizationTest extends TestBase {

    @Test
    public void testTableCustomizations() {
        var orm = initTestDb(HasCustomizedMetadata.DDL);

        HasCustomizedMetadata custom = new HasCustomizedMetadata();
        custom.setMap(Map.of("foo", 1.0, "bar", 2.0));

        orm.insert(custom);
        var fromDb = orm.find(HasCustomizedMetadata.class).byId(custom.getId());
        assertEquals(1L, fromDb.getId());
        assertEquals(fromDb.getMap(), custom.getMap());
    }

}
