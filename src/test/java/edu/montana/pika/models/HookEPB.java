package edu.montana.pika.models;

import edu.montana.pika.bean.PikaBean;

public class HookEPB extends PikaBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS hook_epbs (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                note TEXT,
                stamp TEXT
            );
            """;

    private Long id;
    private String name;
    private String note;
    private String stamp;
    private transient boolean skipInsert;

    private HookEPB() {}

    public HookEPB(String name) {
        this.name = name;
    }

    @Override
    protected void validation() {
        require("name");
    }

    @Override
    public boolean beforeInsert() {
        if (skipInsert) {
            return false;
        }
        stamp = "inserted";
        return true;
    }

    // does not call super, to show that dirty-column removal still runs
    @Override
    public boolean beforeUpdate() {
        stamp = "updated";
        return true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNote() {
        return note;
    }

    public String getStamp() {
        return stamp;
    }

    public void setSkipInsert(boolean skipInsert) {
        this.skipInsert = skipInsert;
    }
}
