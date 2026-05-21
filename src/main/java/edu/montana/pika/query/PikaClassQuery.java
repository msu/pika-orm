package edu.montana.pika.query;

import edu.montana.pika.PikaORM;
import edu.montana.pika.util.PikaIterable;
import edu.montana.pika.mapping.Mapping;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class PikaClassQuery<T> implements Callable<QueryResult<T>>, PikaIterable<T> {

    private final PikaORM orm;
    private final PikaQuery<T> query;
    private final Class classToFind;
    private Class lastJoinedClass;

    public PikaClassQuery(PikaORM orm, Class<T> classToFind) {
        this.orm = orm;
        this.classToFind = classToFind;
        Mapping mappingForClassToFind = orm.getMapping(classToFind);
        query = new PikaQuery<>(orm, mappingForClassToFind.getTableName())
                .withResult(classToFind)
                .withColumnPrefix(mappingForClassToFind.getTableName());
        this.setLastJoinedClass(classToFind);
    }

    public PikaClassQuery<T> join(Class classToJoin) {
        setLastJoinedClass(classToFind);
        return thenJoin(null, classToJoin);
    }

    public PikaClassQuery<T> join(JoinType type, Class classToJoinTo) {
        setLastJoinedClass(classToFind);
        return thenJoin(type, classToJoinTo);
    }

    public PikaClassQuery<T> thenJoin(Class classToJoinTo) {
        return thenJoin(null, classToJoinTo);
    }

    private PikaClassQuery<T> thenJoin(JoinType type, Class classToJoinTo) {
        Class hasFk = resolveFkClass(getLastJoinedClass(), classToJoinTo);
        Class hasId = hasFk == classToJoinTo ? getLastJoinedClass() : classToJoinTo;
        return join(type, classToJoinTo, hasId, hasFk);
    }

    private Class resolveFkClass(Class class1, Class class2) {
        Mapping class1Mapping = orm.getMapping(class1);
        Mapping class2Mapping = orm.getMapping(class2);
        if (class1Mapping.hasColumn(class2Mapping.getDefaultForeignKeyColumnName())) {
            return class1;
        }
        if (class2Mapping.hasColumn(class1Mapping.getDefaultForeignKeyColumnName())) {
            return class2;
        }
        throw new IllegalStateException(MessageFormat.format("Cannot determine a foreign key relationship between {0} and {1}, please use an explicit join", class1.getSimpleName(), class2.getSimpleName()));
    }

    public PikaClassQuery<T> join(JoinType type, Class classToJoin, Class hasId, Class hasFk) {
        Mapping hasIdMapping = orm.getMapping(hasId);
        Mapping hasFkMapping = orm.getMapping(hasFk);
        Mapping classToJoinMapping = orm.getMapping(classToJoin);
        String idTable = hasIdMapping.getTableName();
        String fkTable = hasFkMapping.getTableName();
        String joinedTable = classToJoinMapping.getTableName();
        String idColumn = hasIdMapping.getIdColumn();
        String fkColumn = hasIdMapping.getDefaultForeignKeyColumnName();
        String joinType;
        if (type != null) {
            joinType = type.name() + " JOIN ";
        } else {
            joinType = "JOIN ";
        }
        String sqlString = joinType + joinedTable + " ON " + idTable + "." + idColumn + " = " + fkTable + "." + fkColumn;
        setLastJoinedClass(classToJoin);
        // since we are joining mark the query as distinct
        query.distinct().join(sqlString);
        return this;
    }

    public PikaClassQuery<T> where(String condition) {
        query.where(condition);
        return this;
    }

    public PikaClassQuery<T> where(String whereClause, Object val) {
        query.where(whereClause, val);
        return this;
    }

    public PikaClassQuery<T> where(String whereClause, String arg, Object val) {
        return where(whereClause, Map.of(arg, val));
    }

    public PikaClassQuery<T> where(String whereClause, String arg, Object val, String arg2, Object val2) {
        return where(whereClause, Map.of(arg, val, arg2, val2));
    }

    public PikaClassQuery<T> where(String condition, Map<String, Object> vals) {
        query.where(condition, vals);
        return this;
    }

    public PikaClassQuery<T> orWhere(String condition) {
        query.orWhere(condition);
        return this;
    }

    public PikaClassQuery<T> orWhere(String whereClause, Object val) {
        query.orWhere(whereClause, val);
        return this;
    }

    public PikaClassQuery<T> orWhere(String whereClause, String arg, Object val) {
        query.orWhere(whereClause, arg, val);
        return this;
    }

    public PikaClassQuery<T> orWhere(String whereClause, String arg, Object val, String arg2, Object val2) {
        query.orWhere(whereClause, arg, val, arg2, val2);
        return this;
    }

    public PikaClassQuery<T> orWhere(String condition, Map<String, Object> vals) {
        query.orWhere(condition, vals);
        return this;
    }

    public PikaClassQuery<T> whereIn(String column, Collection<?> values) {
        query.whereIn(column, values);
        return this;
    }

    public PikaClassQuery<T> whereNotIn(String column, Collection<?> values) {
        query.whereNotIn(column, values);
        return this;
    }

    public PikaClassQuery<T> orWhereIn(String column, Collection<?> values) {
        query.orWhereIn(column, values);
        return this;
    }

    public PikaClassQuery<T> orWhereNotIn(String column, Collection<?> values) {
        query.orWhereNotIn(column, values);
        return this;
    }

    public PikaClassQuery<T> whereLike(String column, String pattern) {
        query.whereLike(column, pattern);
        return this;
    }

    public PikaClassQuery<T> orWhereLike(String column, String pattern) {
        query.orWhereLike(column, pattern);
        return this;
    }

    public PikaClassQuery<T> whereBetween(String column, Object low, Object high) {
        query.whereBetween(column, low, high);
        return this;
    }

    public PikaClassQuery<T> orWhereBetween(String column, Object low, Object high) {
        query.orWhereBetween(column, low, high);
        return this;
    }

    public PikaClassQuery<T> group() {
        query.group();
        return this;
    }

    public PikaClassQuery<T> orGroup() {
        query.orGroup();
        return this;
    }

    public PikaClassQuery<T> endGroup() {
        query.endGroup();
        return this;
    }

    public long count() {
        return query.count();
    }

    public Double sum(String column) {
        return query.sum(column);
    }

    public Double avg(String column) {
        return query.avg(column);
    }

    public Object min(String column) {
        return query.min(column);
    }

    public Object max(String column) {
        return query.max(column);
    }

    public QueryResult<T> fetch() {
        return query.fetch();
    }

    public T fetchFirst() {
        return query.fetchFirst();
    }

    public Stream<T> stream() {
        return query.stream();
    }

    public PikaClassQuery<T> withVars(Map<String, Object> vals) {
        query.withVars(vals);
        return this;
    }

    public PikaClassQuery<T> withVar(String name, Object value) {
        query.withVar(name, value);
        return this;
    }

    public PikaClassQuery<T> join(String joinSql) {
        query.join(joinSql);
        return this;
    }

    public PikaClassQuery<T> orderBy(String column) {
        query.orderBy(column);
        return this;
    }

    public PikaClassQuery<T> orderBy(String column, SortOrder direction) {
        query.orderBy(column, direction);
        return this;
    }

    public PikaClassQuery<T> pageSize(int pageSize) {
        query.pageSize(pageSize);
        return this;
    }

    public PikaClassQuery<T> page(long page) {
        query.page(page);
        return this;
    }

    public PikaQuery<T> toRawQuery() {
        return query;
    }

    public Class getLastJoinedClass() {
        return lastJoinedClass;
    }

    public void setLastJoinedClass(Class lastJoinedClass) {
        this.lastJoinedClass = lastJoinedClass;
    }

    public PikaClassQuery<T> withCols(String... cols) {
        query.select(cols);
        return this;
    }

    public PikaClassQuery<T> withCols(List<String> cols) {
        query.select(cols);
        return this;
    }

    public QueryResult<T> call() throws Exception {
        return fetch();
    }

    @Override
    public Iterator<T> iterator() {
        return fetch().iterator();
    }

    public long getPage() {
        return this.query.getPage();
    }

    public boolean isFirstPage() {
        return this.query.isFirstPage();
    }

    public boolean isLastPage() {
        return this.query.isLastPage();
    }

    public boolean hasNextPage() {
        return this.query.hasNextPage();
    }

    public boolean hasPreviousPage() {
        return this.query.hasPreviousPage();
    }

    public long nextPageNumber() {
        return this.query.nextPageNumber();
    }

    public long previousPageNumber() {
        return this.query.previousPageNumber();
    }

    public String nextPageURL(String url) {
        return this.query.nextPageURL(url);
    }

    public String nextPageURL(String url, String paramName) {
        return this.query.nextPageURL(url, paramName);
    }

    public String nextPageURL(URL url) {
        return this.query.nextPageURL(url);
    }

    public String nextPageURL(URL url, String paramName) {
        return this.query.nextPageURL(url, paramName);
    }

    public String previousPageURL(String url) {
        return this.query.previousPageURL(url);
    }

    public String previousPageURL(String url, String paramName) {
        return this.query.previousPageURL(url, paramName);
    }

    public String previousPageURL(URL url) {
        return this.query.previousPageURL(url);
    }

    public String previousPageURL(URL url, String paramName) {
        return this.query.previousPageURL(url, paramName);
    }

    public long totalPages() {
        return this.query.totalPages();
    }

    public long totalCount() {
        return this.query.totalCount();
    }

    public void reload() {
        query.reload();
    }

    public PikaList<T> fetchList() {
        return query.fetchList();
    }

    public QueryResult<ResultMap> explain() {
        return query.explain();
    }

    public QueryResult<ResultMap> explain(String suffix) {
        return query.explain(suffix);
    }

    public String generateSQL() {
        return query.generateSQL();
    }
}
