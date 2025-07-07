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

        Gson gson = new Gson();

        orm.withMapping(HasCustomizedMetadata.class,
                new Mapping(){
                    public String mapToTable() {
                        return "foos";
                    }
                    public FieldMapping mapField(Field field) {
                        return switch (field.getName()) {
                            case "ignoreMe" -> ignore(field);
                            case "myId" -> map(field).toColumn("id").asId();
                            case "json" -> map(field).asType(String.class).transformForDB(gson::toJson)
                                    .transformFromDB((val) -> gson.fromJson(String.valueOf(val), Map.class));
                            default -> defaultMapping(field);
                        };
                    }
                });

        HasCustomizedMetadata custom = new HasCustomizedMetadata();
        custom.setMap(Map.of("foo", 1.0, "bar", 2.0));

        orm.insert(custom);
        var fromDb = orm.find(HasCustomizedMetadata.class).byId(custom.getId());
        assertEquals(fromDb.getId(), 1L);
        assertEquals(fromDb.getMap(), custom.getMap());
    }

}
