package bigsky.pika.query;

import bigsky.pika.PikaORM;
import bigsky.pika.mapping.Mapping;

import java.text.MessageFormat;
import java.util.Map;

public class PikaClassFinder<T> {
    private final PikaORM orm;
    Class<T> classToFind;

    public PikaClassFinder(PikaORM orm, Class<T> classToFind) {
        this.orm = orm;
        this.classToFind = classToFind;
    }

    public T byId(Object id) {
        if(id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        Mapping mapping = orm.getMapping(classToFind);
        String column = mapping.getIdColumn();
        Mapping mapping1 = orm.getMapping(classToFind);
        String sql = "SELECT * FROM " + mapping1.getTableName() + "\nWHERE " + column + "=:arg " + MessageFormat.format(orm.getLimitOffsetClause(), 1, 0);
        QueryResult<T> result = orm.select(sql, Map.of("arg", id), classToFind);
        return result.first();
    }

    public T byKey(String col, Object value) {
        Mapping mapping = orm.getMapping(classToFind);
        String sql = "SELECT * FROM " + mapping.getTableName() + "\nWHERE " + col + "=:arg " + MessageFormat.format(orm.getLimitOffsetClause(), 1, 0);
        QueryResult<T> result = orm.select(sql, Map.of("arg", value), classToFind);
        return result.first();
    }

    public PikaClassQuery<T> all() {
        return byQuery();
    }

    public PikaClassQuery<T> allBy(String column, Object val) {
        String sql = column + "=:val ";
        return where(sql, Map.of("val", val));
    }

    public PikaClassQuery<T> where(String whereClause) {
        return where(whereClause, Map.of());
    }

    public PikaClassQuery<T> where(String whereClause, String arg, Object val) {
        return where(whereClause, Map.of(arg, val));
    }

    public PikaClassQuery<T> where(String whereClause, String arg, Object val, String arg2, Object val2) {
        return where(whereClause, Map.of(arg, val, arg2, val2));
    }

    public PikaClassQuery<T> where(String whereClause, Map<String, Object> args) {
        return all().where(whereClause, args);
    }

    public PikaClassQuery<T> where(String whereClause, Object arg) {
        return all().where(whereClause, arg);
    }

    public T firstWhere(String whereClause, String arg, Object val) {
        return firstWhere(whereClause, Map.of(arg, val));
    }

    public T firstWhere(String whereClause, String arg, Object val, String arg2, Object val2) {
        return firstWhere(whereClause, Map.of(arg, val, arg2, val2));
    }

    public T firstWhere(String whereClause, Map<String, Object> args) {
        Mapping metaData = orm.getMapping(classToFind);
        String tableName = metaData.getTableName();
        String selectClause = "SELECT * FROM " + tableName + "\nWHERE ";
        String sql = selectClause + whereClause + " " + MessageFormat.format(orm.getLimitOffsetClause(), 1, 0);
        return orm.select(sql, args, classToFind).first();
    }

    public QueryResult<T> bySQL(/* language=sql */ String sql, Map<String, Object> args) {
        return orm.select(sql, args, classToFind);
    }

    public T first() {
        return all().first();
    }

    public long totalCount() {
        return all().totalCount();
    }

    public PikaClassQuery<T> page(long i) {
        return all().page(i);
    }

    public PikaClassQuery<T> join(Class clazz) {
        return all().join(clazz);
    }

    public PikaClassQuery<T> byQuery() {
        return orm.query(classToFind);
    }
}
