package grug.db.models;

import grug.db.GrugORM;
import grug.db.GrugORM.Interfaces.GrugRecordLifecycle;

import java.io.Serializable;

public class TransactionDemo implements GrugRecordLifecycle {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS transaction_demos (
                id INTEGER PRIMARY KEY,
                name TEXT,
                value INTEGER
            );
            """;

    public long id;
    public String name;
    public Integer value;


    public TransactionDemo() {
    }

    public TransactionDemo(String name, Integer value) {
        this.name = name;
        this.value = value;
    }

    // throws if value is less than 0
    public boolean beforeInsert() {
        if (value < 0) {
            throw new IllegalStateException("Value cannot be less than zero");
        }
        return true;
    }

}
