package bigsky.pika.mapping;

import bigsky.pika.PikaORM;
import bigsky.pika.logging.PikaLogger;
import bigsky.pika.query.ResultMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import static bigsky.pika.PikaORM.EMPTY_ARRAY;
import static bigsky.pika.PikaORM.rethrow;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class Mapping {

    PikaORM orm;
    private Class classForTable;
    private RecordComponent[] recordComponents;
    private String tableName;
    private Set<String> columnsInDb;
    public Map<String, FieldMapping> fieldNameToMapping;
    private Map<String, FieldMapping> columnToMapping;
    private FieldMapping idMapping;
    private FieldMapping uuidMapping;
    private Constructor constructor;
    private FieldMapping versionMapping;

    public Mapping() {
    }

    public void setOrm(PikaORM orm) {
        this.orm = orm;
    }

    public void setClass(Class aClass) {
        this.classForTable = aClass;
        if (aClass == ResultMap.class) {
            return; // special case
        } else {
            this.tableName = mapToTable();
            fieldNameToMapping = new LinkedHashMap<>();
            columnToMapping = new LinkedHashMap<>();
            for (Field field : getAllFields(aClass)) {
                FieldMapping fieldMapping = mapField(field);
                if (fieldMapping != null) {
                    fieldNameToMapping.put(fieldMapping.getFieldName(), fieldMapping);
                    columnToMapping.put(fieldMapping.getColumnName(), fieldMapping);
                }
            }
        }

        if (classForTable.isRecord()) {
            recordComponents = classForTable.getRecordComponents();
            Constructor[] constructors = classForTable.getDeclaredConstructors();
            constructor = constructors[0];
            constructor.setAccessible(true);
        } else {
            recordComponents = null;
            Constructor[] constructors = classForTable.getDeclaredConstructors();
            for (Constructor c : constructors) {
                if (c.getParameterTypes().length == 0) {
                    c.setAccessible(true);
                    this.constructor = c;
                    break;
                }
            }
            if (constructor == null) {
                throw new IllegalStateException("Class " + classForTable.getName() + " does not have an empty constructor, please add one.  It can be private.");
            }
        }

        idMapping = resolveIdMapping();
        uuidMapping = resolveUUIDMapping();
        versionMapping = resolveVersionMapping();
    }


    private FieldMapping resolveVersionMapping() {
        FieldMapping versionMapping = null;
        for (FieldMapping mapping : fieldNameToMapping.values()) {
            if (mapping.isVersionProperty()) {
                if (versionMapping == null) {
                    versionMapping = mapping;
                } else {
                    throw new IllegalStateException("Cannot have more than one field as the version column: " + versionMapping.getFieldName() +
                            " and " + mapping.getFieldName() + " are both ids!");
                }
            }
        }
        if (versionMapping == null) {
            String versionFieldName = orm.getDefaultVersionFieldName().apply(classForTable);
            versionMapping = fieldNameToMapping.get(versionFieldName);
        }
        return versionMapping;
    }

    private FieldMapping resolveIdMapping() {
        FieldMapping idMapping = null;
        for (FieldMapping mapping : fieldNameToMapping.values()) {
            if (mapping.isId()) {
                if (idMapping == null) {
                    idMapping = mapping;
                } else {
                    throw new IllegalStateException("Cannot have more than one field as the id column: " + idMapping.getFieldName() +
                            " and " + mapping.getFieldName() + " are both ids!");
                }
            }
        }
        if (idMapping == null) {
            String idFieldName = orm.getDefaultIdFieldName().apply(classForTable);
            idMapping = fieldNameToMapping.get(idFieldName);
        }
        return idMapping;
    }

    private FieldMapping resolveUUIDMapping() {
        FieldMapping uuidMapping = null;
        for (FieldMapping mapping : fieldNameToMapping.values()) {
            if (mapping.isUUID()) {
                if (uuidMapping == null) {
                    uuidMapping = mapping;
                } else {
                    throw new IllegalStateException("Cannot have more than one field as the uuid column: " + uuidMapping.getFieldName() +
                            " and " + mapping.getFieldName() + " are both uuids!");
                }
            }
        }
        if (uuidMapping == null) {
            String idFieldName = orm.getDefaultUUIDFieldName().apply(classForTable);
            uuidMapping = fieldNameToMapping.get(idFieldName);
        }
        return uuidMapping;
    }

    protected final FieldMapping ignore(Field field) {
        return null;
    }

    protected final FieldMapping map(Field field) {
        return new FieldMapping(orm, field);
    }

    protected final FieldMapping defaultMapping(Field field) {
        if (shouldIgnore(field)) {
            return ignore(field);
        } else {
            FieldMapping map = map(field);
            if (columnExists(map.getColumnName())) {
                return map;
            } else {
                orm.getLogger().log(PikaLogger.Level.WARN, "The field {} on class {} does not map to a database column.  Available options: {}", field.getName(), classForTable.getName(), columnsInDb);
                return ignore(field);
            }
        }
    }

    private boolean columnExists(String columnName) {
        return columnsInDb == null || columnsInDb.contains(columnName.toLowerCase());
    }

    protected FieldMapping mapField(Field field) {
        return defaultMapping(field);
    }

    public String mapToTable() {
        return orm.getDefaultClassToTableMapping().apply(classForTable);
    }

    private static List<Field> getAllFields(Class aClass) {
        List<Field> fieldsToReturn = new ArrayList<>();
        while (aClass != null) {
            Field[] fields = aClass.getDeclaredFields();
            fieldsToReturn.addAll(Arrays.asList(fields));
            aClass = aClass.getSuperclass();
        }
        return fieldsToReturn;
    }

    public final String getTableName() {
        return this.tableName;
    }

    public Map<String, Object> toDatabaseMap(Object object) {
        Map<String, Object> values = new TreeMap<>();
        for (FieldMapping mapping : fieldNameToMapping.values()) {
            String columnName = mapping.getColumnName();
            Object value = mapping.getValueForDatabaseFrom(object);
            values.put(columnName, value);
        }
        return values;
    }

    @SuppressWarnings({"unchecked"})
    public <T> T newObjectFromResult(PikaORM orm, ResultSet resultSet, ColumnsSpec columnSpec) throws Exception {
        if (classForTable == ResultMap.class) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int i = metaData.getColumnCount();
            HashMap<String, Object> rawMap = new HashMap<>();
            for (int j = 1; j <= i; j++) {
                String tableName = metaData.getTableName(j);
                String columnName = metaData.getColumnName(j);
                if (columnSpec.accept(tableName, columnName)) {
                    Object value = resultSet.getObject(columnName);
                    rawMap.put(columnName, value);
                }
            }
            ResultMap resultMap = new ResultMap(orm, rawMap);
            return (T) resultMap;
        } else {
            T object;
            // if it's a record use the generated constructor
            if (recordComponents != null) {
                Object[] args = new Object[recordComponents.length];
                for (int i = 0; i < recordComponents.length; i++) {
                    RecordComponent recordComponent = recordComponents[i];
                    FieldMapping mapping = fieldNameToMapping.get(recordComponent.getName());
                    Object val = null;
                    if (columnSpec.accept(getTableName(), mapping.getColumnName())) {
                        val = mapping.getValueFromDatabase(resultSet);
                    }
                    args[i] = val;
                }
                object = (T) orm.getReflector().make(constructor, args);
            } else {
                // otherwise use fields
                object = (T) orm.getReflector().make(constructor, EMPTY_ARRAY);
                for (FieldMapping fieldMapping : fieldNameToMapping.values()) {
                    try {
                        if (columnSpec.accept(getTableName(), fieldMapping.getColumnName())) {
                            fieldMapping.mapFromDatabase(object, resultSet);
                        }
                    } catch (Exception e) {
                        orm.getLogger().log(PikaLogger.Level.ERROR, "Could not map field {} on {}, available columns:{}, error:{}",
                                fieldMapping.getFieldName(), classForTable.getSimpleName(), getColumns(resultSet), e.getMessage());
                        throw rethrow(e);
                    }
                }
            }
            return object;
        }
    }

    private String getColumns(ResultSet resultSet) {
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            StringBuilder cols = new StringBuilder("[");
            for (int i = 1; i <= columnCount; i++) {
                cols.append(metaData.getColumnName(i));
                if (i < columnCount) {
                    cols.append(",");
                }
            }
            cols.append("]");
            return cols.toString();
        } catch (SQLException e) {
            throw rethrow(e);
        }
    }

    protected boolean shouldIgnore(Field field) {
        return Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers());
    }

    public void setId(Object object, long id) {
        getIdMapping().setFieldValue(object, id);
    }

    public Object getId(Object object) {
        return getIdMapping().getFieldValue(object);
    }

    public boolean isReadOnly() {
        return classForTable.isRecord();
    }

    public String getIdColumn() {
        return getIdMapping().getColumnName();
    }

    public String getUUIDColumn() {
        return getUUIDMapping().getColumnName();
    }

    public FieldMapping getIdMapping() {
        if (idMapping == null) {
            throw new IllegalStateException("The class " + classForTable.getName() + " has no id column");
        } else {
            return idMapping;
        }
    }

    public FieldMapping getUUIDMapping() {
        if (uuidMapping == null) {
            throw new IllegalStateException("The class " + classForTable.getName() + " has no id column");
        } else {
            return uuidMapping;
        }
    }

    public Object getValueForColumn(Object child, String foreignKeyColumn) {
        FieldMapping mapping = columnToMapping.get(foreignKeyColumn);
        if(mapping == null) {
            throw new IllegalArgumentException(foreignKeyColumn + " is not a valid foreign key column on " + child);
        }
        return mapping.getFieldValue(child);
    }

    public String getDefaultForeignKeyColumnName() {
        return orm.getDefaultFkColumnName().apply(classForTable);
    }

    public Object incrementVersion(Map<String, Object> valuesToUpdate) {
        return versionMapping.incrementVersion(valuesToUpdate);
    }

    public boolean hasVersionColumn() {
        return versionMapping != null;
    }

    public Object getCurrentVersion(Map<String, Object> values) {
        return versionMapping.getValueFromDBMap(values);
    }

    public String getVersionColumn() {
        return versionMapping.getColumnName();
    }

    public void updateVersionValue(Object object, Object nextVersionValue) {
        versionMapping.updateVersionValue(object, nextVersionValue);
    }

    public void copyValues(Object to, Object from) {
        for (FieldMapping mapping : fieldNameToMapping.values()) {
            mapping.setFieldValue(to, mapping.getFieldValue(from));
        }
    }

    public boolean hasColumn(String columnName) {
        return columnToMapping.containsKey(columnName);
    }

    public boolean hasIdColumn() {
        return idMapping != null;
    }

    public boolean hasUUIDColumn() {
        return uuidMapping != null;
    }

    public FieldMapping getFieldMappingForFieldName(String field) {
        return fieldNameToMapping.get(field);
    }

    public FieldMapping getFieldMappingForColumn(String field) {
        return columnToMapping.get(field);
    }

    public Object newInstance() {
        try {
            return orm.getReflector().make(constructor, PikaORM.EMPTY_ARRAY);
        } catch (Exception e) {
            throw rethrow(e);
        }
    }
}
