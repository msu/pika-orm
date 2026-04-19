package bigsky.pika.mapping;

import bigsky.pika.PikaORM;
import bigsky.pika.logging.PikaLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static bigsky.pika.PikaORM.rethrow;
import static bigsky.pika.PikaORM.safely;

public class FieldMapping {
    PikaORM orm;
    Field mappedField;
    String columnName;
    boolean idColumn;
    boolean uuidColumn;
    boolean versionColumn;
    Function<Object, Object> toDatabaseValue;
    Function<Object, Object> fromDatabaseValue;
    Class dbStorageType;
    Function<Object, Object> versionIncrementer;
    Supplier<Object> uuidGenerator;

    public FieldMapping(PikaORM orm, Field mappedField) {
        mappedField.setAccessible(true);
        this.orm = orm;
        this.mappedField = mappedField;
        this.columnName = orm.getDefaultFieldToColumnMapping().apply(mappedField);
        this.versionIncrementer = orm.getDefaultVersionIncrementer().apply(mappedField.getDeclaringClass());
        this.uuidGenerator = orm.getDefaultUUIDGenerator().apply(mappedField.getDeclaringClass());
        this.dbStorageType = mappedField.getType();
    }

    public String getFieldName() {
        return mappedField.getName();
    }

    public String getColumnName() {
        return columnName;
    }

    public Field getField() {
        return mappedField;
    }

    public Object getValueForDatabaseFrom(Object object) {
        Object o = orm.getReflector().get(mappedField, object);
        if (toDatabaseValue != null) {
            o = toDatabaseValue.apply(o);
        }
        return o;
    }

    public void setFieldValue(Object object, Object val) {
        orm.getReflector().set(mappedField, object, val);
    }

    public boolean trySetViaSetter(Object target, Object value) {
        String fieldName = mappedField.getName();
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method match = null;
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(setterName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (isAcceptableArgument(paramType, value)) {
                match = method;
                break;
            }
        }
        if (match == null) {
            return false;
        }
        try {
            match.setAccessible(true);
            match.invoke(target, value);
            return true;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw rethrow(cause);
        } catch (IllegalAccessException e) {
            throw rethrow(e);
        }
    }

    private static boolean isAcceptableArgument(Class<?> paramType, Object value) {
        if (value == null) {
            return !paramType.isPrimitive();
        }
        if (paramType.isInstance(value)) {
            return true;
        }
        if (!paramType.isPrimitive()) {
            return false;
        }
        if (paramType == int.class) return value instanceof Integer;
        if (paramType == long.class) return value instanceof Long;
        if (paramType == double.class) return value instanceof Double;
        if (paramType == boolean.class) return value instanceof Boolean;
        if (paramType == float.class) return value instanceof Float;
        if (paramType == short.class) return value instanceof Short;
        if (paramType == byte.class) return value instanceof Byte;
        if (paramType == char.class) return value instanceof Character;
        return false;
    }

    public Object getFieldValue(Object object) {
        return orm.getReflector().get(mappedField, object);
    }

    public void mapFromDatabase(Object object, ResultSet resultSet) {
        Object fromDb = getValueFromDatabase(resultSet);
        setFieldValue(object, fromDb);
    }

    public Object getValueFromDatabase(ResultSet resultSet) {
        Object value = getValueFromResultSet(columnName, dbStorageType, resultSet);
        if (fromDatabaseValue != null) {
            value = fromDatabaseValue.apply(value);
        }
        return value;
    }

    private Object getValueFromResultSet(String columnName, Class targetType, ResultSet resultSet) {
        Object fieldVal = null;
        try {
            if (targetType == String.class) {
                fieldVal = resultSet.getString(columnName);
            } else if (targetType == Integer.class || targetType == int.class) {
                fieldVal = resultSet.getInt(columnName);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                fieldVal = resultSet.getBoolean(columnName);
            } else if (targetType == Long.class || targetType == long.class) {
                fieldVal = resultSet.getLong(columnName);
            } else if (targetType == Double.class || targetType == double.class) {
                fieldVal = resultSet.getDouble(columnName);
            } else if (targetType.isEnum()) {
                // enums deserialize as strings
                String strValue = resultSet.getString(columnName);
                if (strValue != null && !strValue.isEmpty()) {
                    fieldVal = orm.getReflector().enumValueOf(targetType, strValue);
                }
            } else if (targetType == Date.class) {
                Timestamp timestamp = resultSet.getTimestamp(columnName);
                if (timestamp != null) {
                    fieldVal = new Date(timestamp.getTime());
                }
            } else {
                //noinspection unchecked
                fieldVal = resultSet.getObject(columnName, targetType);
            }
        } catch (SQLException e) {
            if (columnExists(resultSet, columnName)) {
                throw rethrow(e);
            } else {
                orm.getLogger().log(PikaLogger.Level.WARN, "Cannot find column {} in table for {}", columnName, targetType);
            }
        }
        return fieldVal;
    }

    private boolean columnExists(ResultSet resultSet, String columnName) {
        try {
            resultSet.findColumn(columnName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public FieldMapping asId() {
        this.idColumn = true;
        return this;
    }

    public FieldMapping asVersionColumn() {
        this.versionColumn = true;
        return this;
    }

    public FieldMapping withVersionIncrementer(UnaryOperator<Object> versionIncrementer) {
        this.versionIncrementer = versionIncrementer;
        return this;
    }

    public FieldMapping withUUIDGenerator(Supplier<Object> uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
        return this;
    }

    public FieldMapping toColumn(String columnName) {
        this.columnName = columnName;
        return this;
    }

    public FieldMapping asType(Class<String> dbClass) {
        this.dbStorageType = dbClass;
        return this;
    }

    public FieldMapping transformForDB(UnaryOperator<Object> func) {
        this.toDatabaseValue = func;
        return this;
    }

    public FieldMapping transformFromDB(UnaryOperator<Object> func) {
        this.fromDatabaseValue = func;
        return this;
    }

    public boolean isId() {
        return idColumn;
    }

    public boolean isUUID() {
        return uuidColumn;
    }

    public boolean isVersionProperty() {
        return versionColumn;
    }

    public Object incrementVersion(Map<String, Object> values) {
        Object value = getValueFromDBMap(values);
        Object updatedValue = versionIncrementer.apply(value);
        values.put(columnName, updatedValue);
        return updatedValue;
    }

    public Object generateUUID() {
        return uuidGenerator.get();
    }

    public Object getValueFromDBMap(Map<String, Object> values) {
        return values.get(columnName);
    }

    public void updateVersionValue(Object object, Object nextVersionValue) {
        safely(() -> mappedField.set(object, nextVersionValue));
    }

    public Class getType() {
        return mappedField.getType();
    }
}
