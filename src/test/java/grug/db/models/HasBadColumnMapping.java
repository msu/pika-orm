package grug.db.models;

public class HasBadColumnMapping {
    public static String DDL = """
            CREATE TABLE IF NOT EXISTS has_bad_column_mapping (
                id INTEGER PRIMARY KEY,
                foo TEXT
            );
            """;;
    Long id;
    String bar;

    public void setBar(String x) {
        this.bar = x;
    }
}
