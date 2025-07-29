package grug.db;

import com.google.gson.Gson;
import grug.db.GrugORM.FieldMapping;
import grug.db.GrugORM.Mapping;
import grug.db.models.HasCustomizedMetadata;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
