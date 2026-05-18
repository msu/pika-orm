package edu.montana.pika.features.model;

import edu.montana.pika.bean.EnterprisePikaBean;

public class OptimisticBean extends EnterprisePikaBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS optimistic_beans (
                 id INTEGER PRIMARY KEY,
                 version INTEGER NOT NULL,
                 str TEXT
             );
            """;

    long id;
    long version;
    String str;

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }
}
