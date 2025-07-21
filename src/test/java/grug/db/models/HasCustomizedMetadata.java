package grug.db.models;

import com.google.gson.Gson;
import grug.db.GrugORM;

import java.lang.reflect.Field;
import java.util.*;

public class HasCustomizedMetadata {

    public static String DDL = """
            CREATE TABLE IF NOT EXISTS foos (
                id INTEGER PRIMARY KEY,
                json TEXT
            );
            """;

    public static GrugORM.Mapping mapping() {
        Gson gson = new Gson();
        return new GrugORM.Mapping() {
            @Override
            public String mapToTable() {
                return "foos";
            }
            @Override
            public GrugORM.FieldMapping mapField(Field field) {
                return switch (field.getName()) {
                    case "ignoreMe" -> ignore(field);
                    case "myId" -> map(field).toColumn("id").asId();
                    case "json" -> map(field).asType(String.class).transformForDB(gson::toJson)
                            .transformFromDB(val -> gson.fromJson(String.valueOf(val), Map.class));
                    default -> defaultMapping(field);
                };
            }
        };
    }

    String ignoreMe;
    long myId;
    Map json;

    public void setMap(Map foo) {
        this.json = foo;
    }

    public Map getMap() {
        return json;
    }

    public Object getId() {
        return myId;
    }
}
