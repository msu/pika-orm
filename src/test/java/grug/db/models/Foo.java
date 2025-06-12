package grug.db.models;

import grug.db.GrugORM;

public class Foo {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS foo (
                id INTEGER PRIMARY KEY,
                foo_id INTEGER NOT NULL
            );
            """;

    private Long id;
    private long foo_id;

    public FooContainer getFooContainer() {
        return GrugORM.getDefault().load1(this, FooContainer.class, "foo_id");
    }

    public void setFooContainerId(long fooId) {
        this.foo_id = fooId;
    }
}
