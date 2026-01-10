package bigsky.pika.bean;

import bigsky.pika.PikaORM;
import bigsky.pika.query.PikaClassQuery;
import bigsky.pika.query.QueryResult;
import bigsky.pika.util.PikaIterable;
import bigsky.pika.mapping.FieldMapping;
import bigsky.pika.mapping.Mapping;

import java.util.Iterator;
import java.util.Map;

public class PikaManyRelation<T> implements PikaIterable<T> {

    private final Mapping mappingForOne;
    private final Mapping mappingForMany;
    private final Object one;
    private final Class<T> classOfMany;
    private final String manyFk;
    private final PikaORM orm;
    private QueryResult<T> result;

    public PikaManyRelation(Object one,
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
        maybeLoadResult();
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

    public PikaClassQuery<T> toQuery() {
        Object id = mappingForOne.getId(one);
        PikaClassQuery<T> classQuery = orm.query(classOfMany).where(manyFk + "=:pikaFKValue", "pikaFKValue", id);
        return classQuery;
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
        return toQuery().where(col + "=:val", Map.of("val", val)).fetchFirst();
    }

    public int size() {
        maybeLoadResult(); // load results
        return result.size();
    }

    private void maybeLoadResult() {
        // only load for persisted elements
        Object id = mappingForOne.getId(one);
        if(result == null && id != null) {
            result = toQuery().fetch();
        }
    }

}
