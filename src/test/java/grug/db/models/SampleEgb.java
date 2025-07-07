package grug.db.models;

import grug.db.GrugORM;
import grug.db.GrugORM.EnterpriseGrugBean;

import java.util.Date;

public class SampleEgb extends EnterpriseGrugBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS sample_egbs (
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

    public static GrugORM.GrugClassQuery<SampleEgb> where(String str) {
        return GrugORM.get().query(SampleEgb.class).where(str);
    }

}
