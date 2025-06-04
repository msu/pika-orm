package grug.db.models;

import grug.db.GrugORM;
import grug.db.GrugORM.Interfaces.GrugRecord;

import java.util.Date;

/**
 * This is a sample GrugRecord that has the additional functionality found on that interface
 */
public class SampleRecord extends SampleModel implements GrugRecord {

    public SampleRecord() {
    }

    public SampleRecord(String strVal, Integer intVal, Boolean boolVal, Date dateVal) {
        super(strVal, intVal, boolVal, dateVal);
    }

    public static GrugORM.GrugQuery<SampleRecord> where(String str){
        return GrugORM.getDefault().query(SampleRecord.class).where(str);
    }

}
