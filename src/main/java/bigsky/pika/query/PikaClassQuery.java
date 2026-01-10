package bigsky.pika.query;

import bigsky.pika.PikaORM;
import bigsky.pika.util.PikaIterable;
import bigsky.pika.mapping.Mapping;

import java.text.MessageFormat;
import java.util.Collections;
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
