package edu.montana.pika.relationships.models;

import edu.montana.pika.PikaORM;

public class Foo {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS foos (
                id INTEGER PRIMARY KEY,
                foo_container_id INTEGER NOT NULL
            );
            """;

    private Long id;
    private long fooContainerId;

    public FooContainer getFooContainer() {
        return PikaORM.get().load(this, FooContainer.class);
    }

    public void setFooContainerId(long id) {
        this.fooContainerId = id;
    }
}
