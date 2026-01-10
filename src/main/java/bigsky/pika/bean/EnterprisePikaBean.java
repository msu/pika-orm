package bigsky.pika.bean;

import bigsky.pika.PikaORM;
import bigsky.pika.mapping.FieldMapping;
import bigsky.pika.mapping.Mapping;
import bigsky.pika.query.PikaClassFinder;
import bigsky.pika.query.PikaList;
import bigsky.pika.util.TextTools;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class EnterprisePikaBean implements PikaRecordLifecycle {

    private transient boolean persisted;
    private final transient Map<String, PikaList<String>> errors = new LinkedHashMap<>();
    private transient Map<String, Object> originalValues = Collections.emptyMap();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void clearErrors() {
        errors.clear();
    }

    public void addError(String error) {
        var errorList = getErrorList(null);
        errorList.add(error);
    }

    public void addError(Field field, String error) {
        addError(field.getName(), error);
    }

    public void addError(String field, String error) {
        var errorList = getErrorList(field);
        errorList.add(error);
    }

    public PikaList<String> getGeneralErrors() {
        return getErrorList(null);
    }


    public PikaList<String> getErrors(String field) {
        return getErrorList(field);
    }

    public Map<String, PikaList<String>> getAllFieldErrors() {
        var copy = new HashMap<>(errors);
        copy.remove(null);
        var ordered = new TreeMap<>(copy);
        return ordered;
    }

    public String getErrorString(String field) {
        return getErrorList(field).stream().collect(Collectors.joining(", "));
    }

    public boolean hasError(Field field) {
        return hasError(field.getName());
    }

    public boolean hasError(String field) {
        return errors.containsKey(field);
    }

    private PikaList<String> getErrorList(String key) {
        return errors.computeIfAbsent(key, val -> new PikaList<>());
    }

    public final boolean validate() {
        clearErrors();
        validation();
        return !hasErrors();
    }

    protected void validation() {
        // override in subclasses
    }

    protected void require(String... fields) {
        Arrays.stream(fields).forEach(this::require);
    }

    protected void requireUnique(String field) {
        Object existingBean = find(this.getClass()).byKey(field, this.getValueForField(field));
        if (existingBean != null && !this.isIdEquivalent(existingBean)) {
            addError(field, field + " already exists");
        }
    }

    protected void require(String field) {
        var mapping = orm().getMapping(getClass());
        var fieldMapping = mapping.getFieldMappingForFieldName(field);
        Object fieldValue = fieldMapping.getFieldValue(this);
        if(fieldValue == null || (fieldValue instanceof String s && s.isEmpty() )) {
            // TODO pluggable error messages
            this.addError(field, TextTools.humanize(field) + " is required");
        }
    }

    public void afterSelect() {
        markPersisted();
    }

    private void markPersisted() {
        this.persisted = true;
        Mapping mapping = orm().getMapping(this.getClass());
        originalValues = mapping.toDatabaseMap(this);
    }

    public Object getOriginalValue(Field f) {
        return getOriginalValue(f.getName());
    }

    public Object getOriginalValue(String fieldName) {
        return originalValues.get(fieldName);
    }

    @Override
    public boolean beforeUpdate(Map<String, Object> valuesToUpdate) {
        Iterator<String> keys = valuesToUpdate.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (originalValues.containsKey(key)) {
                // remove any elements that are equal to their original value, making update unnecessary
                if(Objects.equals(originalValues.get(key), valuesToUpdate.get(key))) {
                    keys.remove();
                }
            }
        }
        return true;
    }

    public Long insert() {
        if (persisted) {
            throw new IllegalStateException("This record is already persisted!");
        }
        Long id = orm().insert(this);
        if(id != null) {
            markPersisted();
        }
        return id;
    }

    public boolean update() {
        if (!persisted) {
            throw new IllegalStateException("This record has not been persisted!");
        }
        boolean update = orm().update(this);
        markPersisted();
        return update;
    }

    public boolean save() {
        try {
            saveOrThrow();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void saveOrThrow() {
        boolean ok;
        if (persisted) {
            ok = update();
        } else {
            ok = insert() != null;
        }

        if (!ok) {
            throw new IllegalStateException("This record has not been persisted!\n" + getErrorString());
        }
    }

    private String getErrorString() {
        PikaList<String> errorKeys = new PikaList<>(errors.keySet());
        errorKeys.removeIf(s -> s == null);
        if(!errorKeys.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (errorKeys.contains(null)) {
                sb.append("  Errors:\n");
                sb.append("    ").append(getErrorList(null).toString(", ")).append("\n");
                errorKeys.remove(null);
            }
            if (!errorKeys.isEmpty()) {
                sb.append("  Field Errors:\n");
                errorKeys.sort(Comparator.naturalOrder());
                for (String errorKey : errorKeys) {
                    sb.append("    ").append(errorKey).append(": ").append(getErrorString(errorKey)).append("\n");
                }
            }
            return sb.toString();
        }
        return "";
    }

    public boolean delete() {
        return orm().delete(this);
    }

    protected <J, T> PikaManyThroughRelation<J, T> loadManyThrough(Class<J> through, Class<T> to) {
        return orm().loadManyThrough(this, through, to);
    }

    protected <T> PikaManyRelation<T> loadMany(Class<T> of) {
        return orm().loadMany(this, of);
    }

    protected <T> PikaManyRelation<T> loadMany(Class<T> of, String fkColumn) {
        return orm().loadMany(this, of, fkColumn);
    }

    protected <T> T load(Class<T> of) {
        return orm().load(this, of);
    }

    protected <T> T load(Class<T> of, String fkColumn) {
        return orm().load(this, of, fkColumn);
    }

    protected <T> T loadReverse(Class<T> of) {
        return orm().loadReverse(this, of);
    }

    protected <T> T loadReverse(Class<T> of, String fkColumn) {
        return orm().loadReverse(this, of, fkColumn);
    }

    public void reload() {
        orm().reload(this);
    }

    public <T extends EnterprisePikaBean> T setFieldsFrom(Map<String, String> map, String... fields) {
        return setFieldsFrom(map::get, fields);
    }

    public <T extends EnterprisePikaBean> T setFieldsFrom(UnaryOperator<String> supplier, String... fields) {
        for (String col : fields) {
            String str = supplier.apply(col);
            try {
                setValueFromString(col, str);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not set " + col + " to " + str + ":" + e.getMessage(), e);
            }
        }
        return (T) this;
    }

    private void setValueFromString(String col, String str) {
        Mapping mapping = orm().getMapping(this.getClass());
        FieldMapping fieldMapping = mapping.getFieldMappingForFieldName(col);
        if (fieldMapping == null) {
            fieldMapping = mapping.getFieldMappingForFieldName(TextTools.camelCase(col));
            if(fieldMapping == null) {
                throw new IllegalArgumentException("No field '" + col + "' found on " + this.getClass().getSimpleName());
            }
        }
        Class fieldType = fieldMapping.getType();
        fieldMapping.setFieldValue(this, orm().coerce(fieldType, str));
    }

    protected Object getValueForField(String col) {
        Mapping mapping = orm().getMapping(this.getClass());
        return mapping.getFieldMappingForFieldName(col).getFieldValue(this);
    }

    public boolean isIdEquivalent(Object object) {
        if(object instanceof EnterprisePikaBean) {
            if(object.getClass().equals(this.getClass())) {
                Mapping mapping = orm().getMapping(this.getClass());
                Object myId = mapping.getId(this);
                Object thatId = mapping.getId(object);
                return myId != null && myId.equals(thatId);
            }
        }
        return false;
    }

    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public String toString() {
        var mapping = orm().getMapping(this.getClass());
        var fieldMappings = mapping.fieldNameToMapping;
        var sb = new StringBuilder(this.getClass().getSimpleName()).append("{");
        fieldMappings.forEach((name, fieldMapping) -> {
            sb.append(name);
            sb.append(":");
            Object fieldValue = fieldMapping.getFieldValue(this);
            if (fieldValue instanceof String) {
                sb.append("\"").append(fieldValue).append("\"");
            } else {
                sb.append(fieldValue);
            }
            sb.append(", ");
        });
        sb.delete(sb.length() - 2, sb.length());
        sb.append("}");
        return sb.toString();
    }

    protected static <T> PikaClassFinder<T> find(Class<T> c) {
        return orm().find(c);
    }

    protected static PikaORM orm() {
        return PikaORM.get();
    }

}
