package grug.db;

import grug.db.GrugORM.Interfaces.GrugRecord;

import java.lang.reflect.Field;
import java.util.Date;

public class SampleModel implements GrugRecord {

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

    @Override
    public Object transformFromResultSet(Field field, Object fieldVal) {
        if(field.getName().equals("dateVal")) {
            return new Date(Long.parseLong(fieldVal.toString()));
        }
        return fieldVal;
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

    public static GrugORM.GrugQuery<SampleModel> where(String str){
        return GrugORM.getDefault().query(SampleModel.class).where(str);
    }
}
