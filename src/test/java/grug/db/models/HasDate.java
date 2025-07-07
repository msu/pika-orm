package grug.db.models;

import java.util.Date;

public final class HasDate {
    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS has_dates (
                id INTEGER PRIMARY KEY,
                date INTEGER NOT NULL
            );
            """;

    private long id;
    private Date date;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
