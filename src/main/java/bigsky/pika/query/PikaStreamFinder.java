package bigsky.pika.query;

import bigsky.pika.PikaORM;
import bigsky.pika.mapping.Mapping;

import java.text.MessageFormat;
import java.util.Map;
import java.util.stream.Stream;

public class PikaStreamFinder<T> {
    private final PikaORM orm;
    Class<T> classToFind;

    public PikaStreamFinder(PikaORM orm, Class<T> classToFind) {
        this.orm = orm;
        this.classToFind = classToFind;
    }

    public Stream<T> byId(Object id) {
        Mapping mapping = orm.getMapping(classToFind);
        String column = mapping.getIdColumn();
        Mapping mapping1 = orm.getMapping(classToFind);
        String sql = "SELECT * FROM " + mapping1.getTableName() + "\nWHERE " + column + "=:arg " + MessageFormat.format(orm.getLimitOffsetClause(), 1, 0);
        return orm.stream(sql, Map.of("arg", id), classToFind);
    }

    public Stream<T> byKey(String col, Object value) {
        Mapping mapping = orm.getMapping(classToFind);
        String sql = "SELECT * FROM " + mapping.getTableName() + "\nWHERE " + col + "=:arg " + MessageFormat.format(orm.getLimitOffsetClause(), 1, 0);
        return orm.stream(sql, Map.of("arg", value), classToFind);
    }

    public Stream<T> all() {
        Mapping metaData = orm.getMapping(classToFind);
        String tableName = metaData.getTableName();
        String sql = "SELECT * FROM " + tableName;
        return orm.stream(sql, Map.of(), classToFind);
    }

    public Stream<T> allBy(String column, Object val) {
        Mapping metaData = orm.getMapping(classToFind);
        String tableName = metaData.getTableName();
        String selectClause = "SELECT * FROM " + tableName + "\nWHERE ";
        String sql = selectClause + column + "=:val ";
        return orm.stream(sql, Map.of("val", val), classToFind);
    }

    public Stream<T> where(String whereClause, Map<String, Object> args) {
        Mapping metaData = orm.getMapping(classToFind);
        String tableName = metaData.getTableName();
        String selectClause = "SELECT * FROM " + tableName + "\nWHERE ";
        String sql = selectClause + whereClause;
        return orm.stream(sql, args, classToFind);
    }

    public Stream<T> bySQL(String sql, Map<String, Object> args) {
        return orm.stream(sql, args, classToFind);
    }

    public PikaClassQuery<T> byQuery() {
        return orm.query(classToFind);
    }
}
