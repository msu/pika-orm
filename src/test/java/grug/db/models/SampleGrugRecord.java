package grug.db.models;

import grug.db.GrugORM;
import grug.db.GrugORM.EnterpriseGrugBean;

import java.util.Date;

public class SampleGrugRecord extends EnterpriseGrugBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS sample_grug_record (
                id INTEGER PRIMARY KEY,
                str_val TEXT NOT NULL,
                int_val INTEGER NOT NULL,
                bool_val INTEGER NOT NULL,
                date_val INTEGER NOT NULL
            );
            """;

    private Long id;
    private String strVal;
    private Integer intVal;
    private Boolean boolVal;
    private Date dateVal;

    private SampleGrugRecord() {}

    public SampleGrugRecord(String strVal, Integer intVal, Boolean boolVal, Date dateVal) {
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

    public static GrugORM.GrugClassQuery<SampleGrugRecord> where(String str) {
        return GrugORM.get().query(SampleGrugRecord.class).where(str);
    }

}
