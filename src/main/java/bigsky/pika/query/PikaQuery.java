package bigsky.pika.query;

import bigsky.pika.PikaORM;
import bigsky.pika.util.PikaIterable;
import bigsky.pika.mapping.ColumnsSpec;
import bigsky.pika.util.LazyVar;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PikaQuery<T> implements Callable<QueryResult<T>>, PikaIterable<T> {

    private final PikaORM orm;
    private final String baseTable;
    private boolean distinct;
    private List<String> columns;
    private String columnPrefix;
    private final StringBuilder whereClause = new StringBuilder();
    private boolean needsConnective = false;
    private int groupDepth = 0;
    private final Map<String, Object> valMap = new TreeMap<>();
    private final List<String> joins = new ArrayList<>();
    private final List<OrderBy> orderBys = new ArrayList<>();
    protected Class<T> resultClass;
    private AtomicInteger integer =  new AtomicInteger(1);

    private int pageSize = -1;
    private long page = -1;

    // result caches
    private LazyVar<QueryResult<T>> fetchResult;
    private LazyVar<T> fetchFirstResult;
    private LazyVar<Long> totalCountResult;

    public PikaQuery(PikaORM orm, String baseTable) {
        this.orm = orm;
        this.baseTable = baseTable;
        this.resultClass = (Class<T>) ResultMap.class;
        initResults();
    }

    public PikaQuery<T> where(String condition) {
        appendClause(" AND ", condition);
        return this;
    }

    public PikaQuery<T> where(String whereClause, String arg, Object val) {
        return where(whereClause, Map.of(arg, val));
    }

    public PikaQuery<T> where(String whereClause, String arg, Object val, String arg2, Object val2) {
        return where(whereClause, Map.of(arg, val, arg2, val2));
    }

    public PikaQuery<T> where(String condition, Map<String, Object> vars) {
        return where(condition).withVars(vars);
    }

    public PikaQuery<T> where(String condition, Object val) {
        String varName = "VAR_" + integer.getAndIncrement();
        return where(condition + " :" + varName).withVar(varName, val);
    }

    public PikaQuery<T> orWhere(String condition) {
        appendClause(" OR ", condition);
        return this;
    }

    public PikaQuery<T> orWhere(String whereClause, String arg, Object val) {
        return orWhere(whereClause, Map.of(arg, val));
    }

    public PikaQuery<T> orWhere(String whereClause, String arg, Object val, String arg2, Object val2) {
        return orWhere(whereClause, Map.of(arg, val, arg2, val2));
    }

    public PikaQuery<T> orWhere(String condition, Map<String, Object> vars) {
        return orWhere(condition).withVars(vars);
    }

    public PikaQuery<T> orWhere(String condition, Object val) {
        String varName = "VAR_" + integer.getAndIncrement();
        return orWhere(condition + " :" + varName).withVar(varName, val);
    }

    public PikaQuery<T> whereIn(String column, Collection<?> values) {
        return addInClause(" AND ", column, "IN", values);
    }

    public PikaQuery<T> whereNotIn(String column, Collection<?> values) {
        return addInClause(" AND ", column, "NOT IN", values);
    }

    public PikaQuery<T> orWhereIn(String column, Collection<?> values) {
        return addInClause(" OR ", column, "IN", values);
    }

    public PikaQuery<T> orWhereNotIn(String column, Collection<?> values) {
        return addInClause(" OR ", column, "NOT IN", values);
    }

    public PikaQuery<T> whereLike(String column, String pattern) {
        String varName = "VAR_" + integer.getAndIncrement();
        return where(column + " LIKE :" + varName).withVar(varName, pattern);
    }

    public PikaQuery<T> orWhereLike(String column, String pattern) {
        String varName = "VAR_" + integer.getAndIncrement();
        return orWhere(column + " LIKE :" + varName).withVar(varName, pattern);
    }

    public PikaQuery<T> group() {
        openGroup(" AND ");
        return this;
    }

    public PikaQuery<T> orGroup() {
        openGroup(" OR ");
        return this;
    }

    public PikaQuery<T> endGroup() {
        if (groupDepth == 0) {
            throw new IllegalStateException("endGroup() called without a matching group() or orGroup().");
        }
        whereClause.append(")");
        needsConnective = true;
        groupDepth--;
        return this;
    }

    private void openGroup(String connective) {
        if (needsConnective) {
            whereClause.append(connective);
        }
        whereClause.append("(");
        needsConnective = false;
        groupDepth++;
    }

    private void appendClause(String connective, String condition) {
        if (needsConnective) {
            whereClause.append(connective);
        }
        whereClause.append(condition);
        needsConnective = true;
    }

    private PikaQuery<T> addInClause(String connective, String column, String op, Collection<?> values) {
        if (values.isEmpty()) {
            // IN () is invalid SQL; emit a constant so the query still executes predictably
            appendClause(connective, op.equals("IN") ? "1=0" : "1=1");
            return this;
        }
        StringBuilder sb = new StringBuilder(column).append(' ').append(op).append(" (");
        boolean first = true;
        for (Object v : values) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            String varName = "VAR_" + integer.getAndIncrement();
            sb.append(':').append(varName);
            withVar(varName, v);
        }
        sb.append(')');
        appendClause(connective, sb.toString());
        return this;
    }

    public PikaQuery<T> select(String... columns) {
        return select(Arrays.asList(columns));
    }

    public PikaQuery<T> select(List<String> columns) {
        this.columns = columns;
        return this;
    }

    public PikaQuery<T> withColumnPrefix(String columnPrefix) {
        this.columnPrefix = columnPrefix;
        return this;
    }

    public PikaQuery<T> distinct() {
        this.distinct = true;
        return this;
    }

    public <Q> PikaQuery<Q> withResult(Class<Q> clazz) {
        this.resultClass = (Class) clazz;
        //noinspection unchecked
        return (PikaQuery<Q>) this;
    }

    public String generateSQL() {
        String sql = generateSQLNoLimit();
        if (page != -1) {
            int limit;
            if (pageSize == -1) {
                limit = orm.getDefaultPageSize();
            } else {
                limit = pageSize;
            }
            long offset = (page - 1) * limit;
            sql += "\n" + MessageFormat.format(orm.getLimitOffsetClause(), limit, offset);
        } else if (pageSize != -1) {
            int offset = 0;
            int limit = pageSize;
            sql += "\n" + MessageFormat.format(orm.getLimitOffsetClause(), limit, offset);
        }
        return sql;
    }

    private String generateSQLNoLimit() {
        if (groupDepth != 0) {
            throw new IllegalStateException(
                    "Unbalanced query groups: " + groupDepth + " group()/orGroup() call(s) are missing a matching endGroup().");
        }
        String sql = generateSelectClause();
        if (!joins.isEmpty()) {
            sql += "\n" + String.join("\n", joins);
        }
        if (!whereClause.isEmpty()) {
            sql += "\nWHERE " + whereClause;
        }
        if (!orderBys.isEmpty()) {
            sql += "\nORDER BY " + String.join(", ", orderBys.stream().map(OrderBy::toString).toList());
        }
        return sql;
    }

    private String generateSelectClause() {
        StringBuilder selectClause = new StringBuilder("SELECT ");
        if (distinct) {
            selectClause.append("DISTINCT ");
        }
        String prefix = columnPrefix != null ? columnPrefix + "." : "";
        if (columns != null) {
            selectClause.append(columns.stream().map(s -> prefix + s).collect(Collectors.joining(", ")));
        } else {
            selectClause.append(prefix).append("*");
        }
        selectClause.append(" FROM ").append(baseTable);
        return selectClause.toString();
    }

    public PikaQuery<T> withVars(Map<String, Object> vals) {
        for (Map.Entry<String, Object> stringObjectEntry : vals.entrySet()) {
            withVar(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        }
        return this;
    }

    public PikaQuery<T> withVar(String name, Object value) {
        if (valMap.containsKey(name)) {
            throw new IllegalStateException("Value " + name + " already exists in query!");
        }
        valMap.put(name, value);
        return this;
    }

    public PikaQuery<T> join(String joinSql) {
        if (!joinSql.toUpperCase().contains("JOIN")) {
            joinSql = "JOIN " + joinSql;
        }
        this.joins.add(joinSql);
        return this;
    }

    public PikaQuery<T> orderBy(String column) {
        return orderBy(column, null);
    }

    public PikaQuery<T> orderBy(String column, SortOrder direction) {
        this.orderBys.add(new OrderBy(column, direction));
        return this;
    }

    public PikaQuery<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PikaQuery<T> page(long page) {
        this.page = page;
        return this;
    }

    public String toString() {
        return generateSQL() + "\nVals:" + this.valMap;
    }

    public long getPage() {
        return page;
    }

    public boolean isFirstPage() {
        if (page > 0) {
            return page == 1;
        } else {
            return false;
        }
    }

    public boolean isLastPage() {
        if (page > 0) {
            return page == totalPages();
        } else {
            return false;
        }
    }

    // SOURCE: STOLEN FROM JAVA 18 SRC
    private static long ceilDiv(long x, long y) {
        final long q = x / y;
        // if the signs are the same and modulo not zero, round up
        if ((x ^ y) >= 0 && (q * y != x)) {
            return q + 1;
        }
        return q;
    }

    public long totalPages() {
        if (page > 0) {
            long totalCount = totalCount();
            long finalPageSize = pageSize;
            if (finalPageSize == -1) {
                finalPageSize = orm.getDefaultPageSize();
            }
            return ceilDiv(totalCount, finalPageSize);
        } else {
            return 1;
        }
    }

    public QueryResult<ResultMap> explain() {
        return explain("");
    }

    public QueryResult<ResultMap> explain(String suffix) {
        String sql = "EXPLAIN " + suffix + " " + generateSQL();
        return orm.select(sql, valMap, ResultMap.class, columns);
    }

    // actual queries
    private void initResults() {
        fetchResult = new LazyVar<>(() ->{
            String sql = generateSQL();
            return orm.select(sql, valMap, resultClass, columns);
        });
        fetchFirstResult = new LazyVar<>(() -> {
            String sql = generateSQLNoLimit() + " " + MessageFormat.format(orm.getLimitOffsetClause(), 1, 0);
            return orm.select(sql, valMap, resultClass, columns).first();
        });
        totalCountResult = new LazyVar<>(() -> {
            String sql = "SELECT COUNT(*) as total FROM (" + generateSQLNoLimit() + ") T" + integer.getAndIncrement();
            var result = orm.select(sql, valMap).first();
            return result.asLong("total");
        });
    }

    public long totalCount() {
        return totalCountResult.get();
    }

    public long count() {
        return totalCount();
    }

    public Double sum(String column) {
        return aggregate("SUM", column).asDouble("agg");
    }

    public Double avg(String column) {
        return aggregate("AVG", column).asDouble("agg");
    }

    public Object min(String column) {
        return aggregate("MIN", column).get("agg");
    }

    public Object max(String column) {
        return aggregate("MAX", column).get("agg");
    }

    private ResultMap aggregate(String func, String column) {
        String sql = "SELECT " + func + "(" + column + ") as agg FROM (" + generateSQLNoLimit() + ") T" + integer.getAndIncrement();
        return orm.select(sql, valMap).first();
    }

    public QueryResult<T> fetch() {
        return fetchResult.get();
    }

    public PikaList<T> fetchList() {
        QueryResult<T> select = fetch();
        return select.toList();
    }

    public T fetchFirst() {
        return fetchFirstResult.get();
    }

    public void reload() {
        initResults();
    }

    public Stream<T> stream() {
        String sql = generateSQL();
        return orm.stream(sql, valMap, resultClass, new ColumnsSpec(columns));
    }

    @Override
    public Iterator<T> iterator() {
        return fetch().iterator();
    }

    @Override
    public QueryResult<T> call() throws Exception {
        return fetch();
    }
}
