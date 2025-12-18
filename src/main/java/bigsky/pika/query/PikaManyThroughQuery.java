package bigsky.pika.query;

import bigsky.pika.PikaORM;
import bigsky.pika.util.PikaIterable;
import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.mapping.FieldMapping;
import bigsky.pika.mapping.Mapping;

import java.util.Iterator;
import java.util.Map;

public class PikaManyThroughQuery<J, T> implements PikaIterable<T> {

    private final PikaORM orm;
    private final Class<?> joinClass;
    private final Object one;
    private final String oneFk;
    private final Mapping oneMapping;
    private final Class<T> classOfMany;
    private final String manyFk;
    private QueryResult<T> result;

    public PikaManyThroughQuery(Object one, String oneFk, Class<J> joinClass, Class<T> classOfMany, String manyFk, PikaORM orm) {
        this.one = one;
        this.oneFk = oneFk;
        this.oneMapping = orm.getMapping(one.getClass());
        this.joinClass = joinClass;
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

    public J add(T newMember) {
        Object initialObjectId = oneMapping.getId(one);
        if (initialObjectId == null) {
            throw new IllegalStateException(" The owning object of a 1-to-Many relationship must be saved to the database to add elements to it.");
        }
        Mapping newMemberMapping = orm.getMapping(newMember.getClass());
        Object idOfNewMember = newMemberMapping.getId(newMember);
        if (idOfNewMember == null) {
            throw new IllegalStateException("The object being added to a 1-to-Many relationship through another table must be saved to the database before it can be added");
        }
        Mapping joinObjectMapping = orm.getMapping(joinClass);

        J instance = (J) joinObjectMapping.newInstance();

        FieldMapping fieldMappingForInitialObject = joinObjectMapping.getFieldMappingForColumn(oneFk);
        fieldMappingForInitialObject.setFieldValue(instance, initialObjectId);

        FieldMapping fieldMappingForNewMember = joinObjectMapping.getFieldMappingForColumn(manyFk);
        fieldMappingForNewMember.setFieldValue(instance, idOfNewMember);

        return instance;
    }

    public J addAndSave(T newMember) {
        J obj = add(newMember);
        if (obj instanceof EnterprisePikaBean epb) {
            epb.save();
        } else {
            orm.insert(newMember);
        }
        reload();
        return obj;
    }

    private PikaClassQuery<T> toClassQuery() {
        PikaClassQuery<T> classQuery = orm.query(classOfMany)
                .join(joinClass)
                .thenJoin(one.getClass())
                .where(oneMapping.getTableName() + "." + oneMapping.getIdColumn() + "=:pikaId")
                .withVar("pikaId", oneMapping.getId(one));
        return classQuery;
    }

    public T findById(long manyId) {
        Mapping mappingForMany = orm.getMapping(classOfMany);
        String idCol = mappingForMany.getIdColumn();
        return findBy(mappingForMany.getTableName() + "." + idCol, manyId);
    }

    private T findBy(String col, Object val) {
        return toClassQuery().where(col + "=:val", Map.of("val", val)).fetchFirst();
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

    public void remove(T element) {
        Mapping elementToRemove = orm.getMapping(element.getClass());

        String initialObjectFk = oneMapping.getDefaultForeignKeyColumnName();
        String elementToRemoveFk = elementToRemove.getDefaultForeignKeyColumnName();

        Mapping joinClassMapping = orm.getMapping(joinClass);


        PikaClassQuery<?> joinObjects = orm.query(joinClass)
                .where(joinClassMapping.getTableName() + "." + initialObjectFk + "=:pikaId")
                .withVar("pikaId", oneMapping.getId(one))
                .where(joinClassMapping.getTableName() + "." + elementToRemoveFk + "=:elementId")
                .withVar("elementId", elementToRemove.getId(element));
        for (Object joinObject : joinObjects) {
            if(joinObject instanceof EnterprisePikaBean epb) {
                epb.delete();
            } else {
                orm.delete(joinObject);
            }
        }
    }
}
