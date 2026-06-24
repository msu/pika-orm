package edu.montana.pika.customization.model;

import edu.montana.pika.bean.PikaBean;

public class BadModel extends PikaBean {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS bad_models (
                id INTEGER PRIMARY KEY
            );
            """;

    private Long id;
    transient private Long unmappedField;

    public Long getId() {
        return id;
    }
    public Long getUnmappedField() {
        return unmappedField;
    }
}
