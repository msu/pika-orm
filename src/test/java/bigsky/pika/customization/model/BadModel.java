package bigsky.pika.customization.model;

import bigsky.pika.bean.EnterprisePikaBean;

public class BadModel extends EnterprisePikaBean {
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
