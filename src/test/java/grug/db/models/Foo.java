package grug.db.models;

import grug.db.GrugORM;

public class Foo {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS foo (
                id INTEGER PRIMARY KEY,
                foo_container_id INTEGER NOT NULL
            );
            """;

    private Long id;
    private long fooContainerId;

    public FooContainer getFooContainer() {
        return GrugORM.get().load1(this, FooContainer.class);
    }

    public void setFooContainerId(long id) {
        this.fooContainerId = id;
    }
}
