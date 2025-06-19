package grug.db;

import com.google.gson.Gson;
import grug.db.GrugORM.DBMetaData;
import grug.db.models.HasCustomizedMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
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
                new DBMetaData(){//these are replaced in the class at compile time, and overides happen before instantiation
                    @Override
                    public String determineTableName(String name) {
                        return "foo";
                    }

                    @Override
                    public String determineIdColumnName() {
                        return "id";
                    }

                    @Override
                    protected void setClass(Class aClass){
                        super.setClass(aClass);//call parent stuff
                        //then apply the custom mappings!

                        remapField("myId", "id");//idk why this isnt picking up in metadb
                    }

                    @Override
                    public Object transformForDatabase(Field field, Object value){
                        if("json".equals(field.getName()) && value instanceof Map){
                            try{
                                Gson gson = new Gson();
                                String jsonString = gson.toJson(value);
                                return jsonString;
                            } catch (Exception e){
                                throw new RuntimeException("Failed to Serialise BOIIII");
                            }
                        }
                        return super.transformForDatabase(field, value);
                    }

                    @Override
                    public Object transformFromDatabase(Field field, Object value){
                        if("json".equals(field.getName()) && value instanceof Map){
                            try{
                                Gson gson = new Gson();//we want to now deserialise to a basic map??
                                Map fromString = gson.fromJson( (String) value, Map.class);//taken from Carson!
                                return fromString;
                            } catch (Exception e){
                                throw new RuntimeException("Failed to DESerialise BOIIII");
                            }
                        }
                        return super.transformForDatabase(field, value);

                    }



                    // TODO - the id column is named id in the database, but myId in the java class
                    //^^ for this class I think we want to make a two way set and back really of the field mapping as that is what is changed with the column structure
                    // TODO - serialize the json Map field to a string on the way in to the db and deserialize it on the way out
                });



        HasCustomizedMetadata custom = new HasCustomizedMetadata();
        custom.setMap(Map.of("foo", 1, "bar", 2));

        // demo of how to make a map into a JSON string and back
        Gson gson = new Gson();
        String jsonString = gson.toJson(custom.getMap());
        System.out.printf("jsonString: " + jsonString);


        orm.insert(custom);
        orm.find(HasCustomizedMetadata.class, custom.getId());
    }

}
