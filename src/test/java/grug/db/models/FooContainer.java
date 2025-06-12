package grug.db.models;

import grug.db.GrugORM;

import java.util.List;

public class FooContainer {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS foo_container (
                id INTEGER PRIMARY KEY
            );
            """;

    private long id;

    public List<Foo> getFoos() {
        return GrugORM.getDefault().loadN(this, Foo.class, "foo_id");
    }

    public long getId() {
        return id;
    }
}
