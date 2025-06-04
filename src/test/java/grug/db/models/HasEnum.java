package grug.db.models;

import java.util.Objects;

public final class HasEnum {
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
