package grug.db;

import com.google.gson.Gson;
import grug.db.GrugORM.DBMetaData;
import grug.db.models.HasCustomizedMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class CustomizationTest extends TestBase{

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(HasCustomizedMetadata.DDL);
    }

    @Test
    public void testTableNameCustomization() {
        orm.withMapping(HasCustomizedMetadata.class,
                new DBMetaData(){
                    @Override
                    public String getTableName() {
                        return "foo";
                    }

                    // TODO - the id column is named id in the database, but myId in the java class

                    // TODO - serialize the json Map field to a string on the way in to the db and deserialize it on the way out
                });


        HasCustomizedMetadata custom = new HasCustomizedMetadata();
        custom.setMap(Map.of("foo", 1, "bar", 2));

        // demo of how to make a map into a JSON string and back
        Gson gson = new Gson();
        String jsonString = gson.toJson(custom.getMap());
        System.out.printf("jsonString: " + jsonString);
        Map fromString = gson.fromJson(jsonString, Map.class);


        orm.insert(custom);
        orm.find(HasCustomizedMetadata.class, custom.getId());
    }

}
