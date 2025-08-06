package bigsky.pika.models;

import bigsky.pika.PikaORM;
import bigsky.pika.PikaORM.EnterprisePikaBean;

import java.util.Date;

public class SampleEgb extends EnterprisePikaBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS sample_egbs (
                id INTEGER PRIMARY KEY,
                str_val TEXT NOT NULL,
                int_val INTEGER NOT NULL,
                bool_val BOOLEAN NOT NULL,
                date_val DATE NOT NULL
            );
            """;

    private Long id;
    private String strVal;
    private Integer intVal;
    private Boolean boolVal;
    private Date dateVal;

    private SampleEgb() {}

    public SampleEgb(String strVal, Integer intVal, Boolean boolVal, Date dateVal) {
        this.strVal = strVal;
        this.intVal = intVal;
        this.boolVal = boolVal;
        this.dateVal = dateVal;
    }

    @Override
    protected void validation() {
        if (intVal < 0) {
            addError("intVal", "intVal must be greater than or equal to zero!");
        }
    }

    public static PikaORM.PikaClassFinder<SampleEgb> find() {
        return orm().find(SampleEgb.class);
    }

    public void setStrVal(String s) {
        this.strVal = s;
    }
}
