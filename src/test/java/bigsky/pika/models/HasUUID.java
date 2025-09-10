package bigsky.pika.models;

public final class HasUUID {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS has_uuids (
                id INTEGER PRIMARY KEY,
                uuid TEXT NOT NULL
            );
            """;
    private long id;
    private String uuid;

    public long getId() {
        return id;
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String string) {
        this.uuid = string;
    }
}
