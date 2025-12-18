package bigsky.pika.query;

import bigsky.pika.PikaORM;
import bigsky.pika.util.PikaIterable;
import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.mapping.FieldMapping;
import bigsky.pika.mapping.Mapping;

import java.util.Iterator;
import java.util.Map;

public class PikaManyQuery<T> implements PikaIterable<T> {

    private final Mapping mappingForOne;
    private final Mapping mappingForMany;
    private final Object one;
    private final Class<T> classOfMany;
    private final String manyFk;
    private final PikaORM orm;
    private QueryResult<T> result;

    public PikaManyQuery(Object one,
                         Class<T> classOfMany,
                         String manyFk,
                         PikaORM orm) {
        this.mappingForOne = orm.getMapping(one.getClass());
        this.mappingForMany = orm.getMapping(classOfMany);
        this.one = one;
        this.classOfMany = classOfMany;
        this.manyFk = manyFk;
        this.orm = orm;
    }

    public void reload() {
        result = null;
    }

    @Override
    public Iterator<T> iterator() {
        if(result == null) {
            result = toClassQuery().fetch();
        }
        return result.iterator();
    }

    public void add(T newMember) {
        Object id = mappingForOne.getId(one);
        if (id == null) {
            throw new IllegalStateException(one + " must be saved to the database to add " + newMember);
        }
        FieldMapping fieldMapping = mappingForMany.getFieldMappingForColumn(manyFk);
        if (fieldMapping == null) {
            throw new IllegalStateException(" I don't know how to map " + newMember + " to a many relationship with " + one);
        }
        fieldMapping.setFieldValue(newMember, id);
    }

    public void addAndSave(T newMember) {
        add(newMember);
        if (newMember instanceof EnterprisePikaBean epb) {
            epb.save();
        } else {
            Object idForNewMember = mappingForMany.getId(newMember);
            if (idForNewMember == null) {
                orm.insert(newMember);
            } else {
                orm.update(newMember);
            }
        }
        reload();
    }

    public PikaClassQuery<T> toClassQuery() {
        Object id = mappingForOne.getId(one);
        PikaClassQuery<T> classQuery = orm.query(classOfMany).where(manyFk + "=:pikaFKValue", "pikaFKValue", id);
        return classQuery;
    }

    public PikaClassQuery<T> where(String condition) {
        return toClassQuery().where(condition);
    }

    public PikaClassQuery<T> where(String whereClause, String arg, Object val) {
        return toClassQuery().where(whereClause, arg, val);
    }

    public PikaClassQuery<T> where(String whereClause, String arg, Object val, String arg2, Object val2) {
        return toClassQuery().where(whereClause, arg, val, arg2, val2);
    }

    public PikaClassQuery<T> where(String condition, Map<String, Object> vals) {
        return toClassQuery().where(condition, vals);
    }

    public PikaClassQuery<T> orderBy(String col) {
        return orderBy(col, null);
    }

    public PikaClassQuery<T> orderBy(String col, SortOrder direction) {
        return toClassQuery().orderBy(col, direction);
    }

    public T create() {
        T newMember = (T) mappingForMany.newInstance();
        add(newMember);
        return newMember;
    }

    public T findById(long manyId) {
        String idCol = mappingForMany.getIdColumn();
        return findBy(idCol, manyId);
    }

    private T findBy(String col, Object val) {
        return toClassQuery().where(col + "=:val", Map.of("val", val)).fetchFirst();
    }

    public long totalCount() {
        return toClassQuery().totalCount();
    }

    public PikaClassQuery<T> page(long page) {
        return toClassQuery().page(page);
    }

    public long totalPages() {
        return toClassQuery().totalPages();
    }
}
