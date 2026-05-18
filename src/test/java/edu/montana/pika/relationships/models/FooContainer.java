package edu.montana.pika.relationships.models;

import edu.montana.pika.PikaORM;
import edu.montana.pika.bean.PikaManyRelation;

public class FooContainer {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS foo_containers (
                id INTEGER PRIMARY KEY
            );
            """;

    private long id;

    public PikaManyRelation<Foo> getFoos() {
        return PikaORM.get().loadMany(this, Foo.class);
    }

    public long getId() {
        return id;
    }
}
