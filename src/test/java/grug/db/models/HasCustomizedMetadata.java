package grug.db.models;

import java.util.*;

public class HasCustomizedMetadata {
    public static String DDL = """
            CREATE TABLE IF NOT EXISTS foo (
                id INTEGER PRIMARY KEY,
                json TEXT
            );
            """;;
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
