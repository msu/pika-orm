# Transactions in PikaORM

> Transactions are handled very simplistically in PikaORM, using LAMDA's as the core function



```java
		 // init the ORM
        PikaORM orm = new PikaORM("jdbc:sqlite:test/web.db") // DB connection string
                .withLogLevel(TRACE)
                .makeDefaultORM()
                .withMigrations(new TransactionDemo())
                .applyMigrations();

        TransactionDemo foo = new TransactionDemo("foo", 10);
        TransactionDemo bar = new TransactionDemo("bar", -10); // bad value

        try {
            orm.withTransaction(() -> {
                orm.insert(foo);
                orm.withTransaction(() -> {
                    orm.insert(bar);
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            // ignore :)
        }
```

> This is a full example of a transaction included with a nested transaction. Because of the bad value in the nested transaction, the whole function should fail, including the top level. You can uses these to insure transaction safety for your queries. 



Additionally we recommend using the `PikaRecordLifeCycle` interface to ensure even safety with transactions, and put users in the habit of validation more. An example of the `TransactionDemo` class is shown below

```java
public class TransactionDemo extends PikaORM.Migrations implements PikaRecordLifecycle  {

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

    @Override
    public void migrations() {
        add(this::addTranscationDemoTable);
    }

    public PikaORM.Migrations.PikaMigration addTranscationDemoTable() {
        return makeMigration("transactionDemo")
                .up(""" 
                    CREATE TABLE IF NOT EXISTS transaction_demos (
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    int_value INTEGER
                    );
                    """)
                .down("""
                        DROP TABLE transaction_demos;
                        """);
    }
}

```

> `beforeInsert()` is a `PikaRecordLifeCycle` class that adds an extra layer of verification in the transaction cycle. 
