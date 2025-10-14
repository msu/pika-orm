package bigsky.pika.core.model;

import java.util.Objects;

public final class HasEnum {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS has_enums (
                id INTEGER PRIMARY KEY,
                my_enum TEXT NOT NULL
            );
            """;
    private long id;
    private MyEnum myEnum;

    public long getId() {
        return id;
    }

    public void setId(long id) {
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
