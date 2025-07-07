package grug.db.models;

import grug.db.GrugORM;

import java.util.List;

public class FooContainer {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS foo_containers (
                id INTEGER PRIMARY KEY
            );
            """;

    private long id;

    public List<Foo> getFoos() {
        return GrugORM.get().loadN(this, Foo.class);
    }

    public long getId() {
        return id;
    }
}
