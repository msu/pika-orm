package bigsky.pika.features.model;

import bigsky.pika.bean.PikaRecordLifecycle;

public class TransactionDemo implements PikaRecordLifecycle {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS transaction_demos (
                id INTEGER PRIMARY KEY,
                name TEXT,
                int_value INTEGER
            );
            """;

    public long id;
    public String name;
    public Integer intValue;


    public TransactionDemo() {
    }

    public TransactionDemo(String name, Integer intValue) {
        this.name = name;
        this.intValue = intValue;
    }

    // throws if value is less than 0
    public boolean beforeInsert() {
        if (intValue < 0) {
            throw new IllegalStateException("Value cannot be less than zero");
        }
        return true;
    }

}
