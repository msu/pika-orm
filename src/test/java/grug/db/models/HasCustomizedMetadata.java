package grug.db.models;

import java.util.*;

public class HasCustomizedMetadata {
    public static String DDL = """
            CREATE TABLE IF NOT EXISTS foos (
                id INTEGER PRIMARY KEY,
                json TEXT
            );
            """;;
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
