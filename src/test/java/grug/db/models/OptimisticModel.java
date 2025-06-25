package grug.db.models;

import grug.db.GrugORM.EnterpriseGrugBean;

public class OptimisticModel extends EnterpriseGrugBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS optimistic_model (
                 id INTEGER PRIMARY KEY,
                 version INTEGER NOT NULL,
                 str TEXT
             );
            """;

    long id;
    long version;
    String str;

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }
}
