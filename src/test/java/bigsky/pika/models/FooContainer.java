package bigsky.pika.models;

import bigsky.pika.PikaORM;

public class FooContainer {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS foo_containers (
                id INTEGER PRIMARY KEY
            );
            """;

    private long id;

    public PikaORM.PikaManyQuery<Foo> getFoos() {
        return PikaORM.get().loadMany(this, Foo.class);
    }

    public long getId() {
        return id;
    }
}
