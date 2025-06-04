package grug.db.models;

import java.util.Objects;

public final class HasEnum {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS has_enum (
                id INTEGER PRIMARY KEY,
                my_enum TEXT NOT NULL
            );
            """;
    private int id;
    private MyEnum myEnum;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MyEnum getMyEnum() {
        return myEnum;
    }

    public void setMyEnum(MyEnum myEnum) {
        this.myEnum = myEnum;
    }

    public enum MyEnum {
        FOO,
        BAR
    }
}
