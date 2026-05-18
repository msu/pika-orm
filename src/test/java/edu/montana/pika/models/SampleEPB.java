package edu.montana.pika.models;


import edu.montana.pika.bean.EnterprisePikaBean;
import edu.montana.pika.query.PikaClassFinder;

import java.util.Date;

public class SampleEPB extends EnterprisePikaBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS sample_epbs (
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

    private SampleEPB() {}

    public SampleEPB(String strVal, Integer intVal, Boolean boolVal, Date dateVal) {
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

    public static PikaClassFinder<SampleEPB> find() {
        return orm().find(SampleEPB.class);
    }

    public Long getId() {
        return id;
    }

    public String getStrVal() {
        return strVal;
    }

    public void setStrVal(String s) {
        this.strVal = s;
    }
}
