package bigsky.pika.query;

import bigsky.pika.PikaORM;
import bigsky.pika.util.PikaIterable;
import bigsky.pika.mapping.ColumnsSpec;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QueryResult<T> implements PikaIterable<T> {

    private final PikaORM orm;

    // query specification
    private final String sql;
    private final Map<String, Object> args;
    private final Class resultClass;
    private final ColumnsSpec columnSpec;

    // results
    private PikaList<T> results;
    private List<T> readOnlyResults;

    public QueryResult(PikaORM orm, String sql, Map<String, Object> args, Class resultClass, ColumnsSpec columnSpec, PikaList<T> resultList) {
        this.orm = orm;
        this.sql = sql;
        this.args = args;
        this.resultClass = resultClass;
        this.columnSpec = columnSpec;
        this.results = resultList;
        this.readOnlyResults = Collections.unmodifiableList(results);
    }

    public QueryResult<T> copy() {
        return new QueryResult<>(orm, sql, args, resultClass, columnSpec, results.copy());
    }

    public void reload() {
        results = new PikaList<>();
        readOnlyResults = Collections.unmodifiableList(results);
        orm.select(sql, args, resultClass, columnSpec, results);
    }

    public PikaList<T> toList() {
        return results;
    }

    public Iterator<T> iterator() {
        return readOnlyResults.iterator();
    }

    public List<T> getAsReadOnlyList() {
        return readOnlyResults;
    }
}
