package edu.montana.pika.models;

import edu.montana.pika.bean.PikaRecordLifecycle;

import java.util.Date;

public class SampleModel implements PikaRecordLifecycle {

    public static String DDL = """
            CREATE TABLE IF NOT EXISTS sample_models (
                id INTEGER PRIMARY KEY,
                str_val TEXT,
                int_val INTEGER NOT NULL,
                bool_val BOOLEAN NOT NULL,
                date_val DATETIME NOT NULL
            );
            """;

    static String SHOULD_NOT_BE_PERSISTED = "Should not be persisted";

    private Long id;
    private String strVal;
    private Integer intVal;
    private Boolean boolVal;
    private Date dateVal;

    public SampleModel() {}

    public SampleModel(String strVal, Integer intVal, Boolean boolVal, Date dateVal) {
        this.setStrVal(strVal);
        this.setIntVal(intVal);
        this.setBoolVal(boolVal);
        this.setDateVal(dateVal);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStrVal() {
        return strVal;
    }

    public void setStrVal(String strVal) {
        this.strVal = strVal;
    }

    public Integer getIntVal() {
        return intVal;
    }

    public void setIntVal(Integer intVal) {
        this.intVal = intVal;
    }

    public Boolean getBoolVal() {
        return boolVal;
    }

    public void setBoolVal(Boolean boolVal) {
        this.boolVal = boolVal;
    }

    public Date getDateVal() {
        return dateVal;
    }

    public void setDateVal(Date dateVal) {
        this.dateVal = dateVal;
    }

}
