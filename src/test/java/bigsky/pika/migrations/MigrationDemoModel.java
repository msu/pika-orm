package grug.db.migrations;

import grug.db.GrugORM.Interfaces.GrugRecordLifecycle;

import java.util.Date;

public class MigrationDemoModel implements GrugRecordLifecycle {

    private Long id;
    private String strVal;
    private Integer intVal;
    private Boolean boolVal;
    private Date dateVal;

    public MigrationDemoModel() {}

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
