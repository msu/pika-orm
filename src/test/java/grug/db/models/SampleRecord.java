package grug.db.models;

import grug.db.GrugORM;
import grug.db.GrugORM.Interfaces.GrugRecord;

import java.util.Date;

/**
 * This is a sample GrugRecord that has the additional functionality found on that interface
 */
public class SampleRecord extends SampleModel implements GrugRecord {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS sample_record (
                id INTEGER PRIMARY KEY,
                str_val TEXT NOT NULL,
                int_val INTEGER NOT NULL,
                bool_val INTEGER NOT NULL,
                date_val INTEGER NOT NULL
            );
            """;


    public SampleRecord() {
    }

    public SampleRecord(String strVal, Integer intVal, Boolean boolVal, Date dateVal) {
        super(strVal, intVal, boolVal, dateVal);
    }

    public static GrugORM.GrugQuery<SampleRecord> where(String str) {
        return GrugORM.getDefault().query(SampleRecord.class).where(str);
    }

}
