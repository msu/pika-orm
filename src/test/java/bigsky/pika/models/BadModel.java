package bigsky.pika.models;

import bigsky.pika.PikaORM;

public class BadModel extends PikaORM.EnterprisePikaBean {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS bad_models (
                id INTEGER PRIMARY KEY
            );
            """;

    private Long id;
    private Long unmappedField;

    public Long getId() {
        return id;
    }
    public Long getUnmappedField() {
        return unmappedField;
    }
}
