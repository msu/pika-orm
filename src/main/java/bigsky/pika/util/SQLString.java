package bigsky.pika.util;

public class SQLString {
    private final String sql;

    public SQLString(String sql) {
        this.sql = sql;
    }

    public String toString() {
        return sql;
    }
}
