package bigsky.pika.relationships.models;

import bigsky.pika.PikaORM;
import bigsky.pika.query.PikaManyQuery;

public class FooContainer {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS foo_containers (
                id INTEGER PRIMARY KEY
            );
            """;

    private long id;

    public PikaManyQuery<Foo> getFoos() {
        return PikaORM.get().loadMany(this, Foo.class);
    }

    public long getId() {
        return id;
    }
}
