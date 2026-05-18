package edu.montana.pika.query;

import edu.montana.pika.PikaORM;

import java.math.BigDecimal;
import java.util.*;

@SuppressWarnings("NullableProblems")
public class ResultMap implements Map<String, Object> {
    private final PikaORM orm;
    private Map<String, Object> result;

    public ResultMap(PikaORM orm, Map<String, Object> backingMap) {
        this.orm = orm;
        result = Collections.unmodifiableMap(backingMap);
    }

    // automatic down-casting helpers
    // TODO is the type parameter necessary here?
    public <T> T get(String key, Class<T> type) {
        return (T) result.get(key);
    }

    public String getString(String key) {
        return this.get(key, String.class);
    }

    public Short getShort(String key) {
        return this.get(key, Short.class);
    }

    public Integer getInteger(String key) {
        return this.get(key, Integer.class);
    }

    public Long getLong(String key) {
        return this.get(key, Long.class);
    }

    public Float getFloat(String key) {
        return this.get(key, Float.class);
    }

    public Double getDouble(String key) {
        return this.get(key, Double.class);
    }

    public BigDecimal getBigDecimal(String key) {
        return this.get(key, BigDecimal.class);
    }

    public Date getDate(String key) {
        return this.get(key, Date.class);
    }

    public Boolean getBoolean(String key) {
        return this.get(key, Boolean.class);
    }

    // the 'as' methods will attempt to coerce a value to the given type

    public String asString(String key) {
        return (String) orm.sloppyCoerce(String.class, get(key));
    }

    public Short asShort(String key) {
        return (Short) orm.sloppyCoerce(Short.class, get(key));
    }

    public Integer asInteger(String key) {
        return (Integer) orm.sloppyCoerce(Integer.class, get(key));
    }

    public Long asLong(String key) {
        return (Long) orm.sloppyCoerce(Long.class, get(key));
    }

    public Float asFloat(String key) {
        return (Float) orm.sloppyCoerce(Float.class, get(key));
    }

    public Double asDouble(String key) {
        return (Double) orm.sloppyCoerce(Double.class, get(key));
    }

    public BigDecimal asBigDecimal(String key) {
        return (BigDecimal) orm.sloppyCoerce(BigDecimal.class, get(key));
    }

    public Date asDate(String key) {
        return (Date) orm.sloppyCoerce(Date.class, get(key));
    }

    public Boolean asBoolean(String key) {
        return (Boolean) orm.sloppyCoerce(Boolean.class, get(key));
    }

    // create a case-insensitive version of the results, some dbs are ugly w/ the cases
    public ResultMap toCaseInsensitiveMap() {
        TreeMap<String, Object> caseInsensitiveMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveMap.putAll(this);
        return new ResultMap(orm, caseInsensitiveMap);
    }

    public int size() {
        return result.size();
    }

    public boolean isEmpty() {
        return result.isEmpty();
    }

    public boolean containsKey(Object key) {
        return result.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return result.containsValue(value);
    }

    public Object get(Object key) {
        return result.get(key);
    }

    public Object put(String key, Object value) {
        return result.put(key, value);
    }

    public Object remove(Object key) {
        return result.remove(key);
    }

    public void putAll(Map<? extends String, ?> m) {
        result.putAll(m);
    }

    public void clear() {
        result.clear();
    }

    public Set<String> keySet() {
        return result.keySet();
    }

    public Collection<Object> values() {
        return result.values();
    }

    public Set<Entry<String, Object>> entrySet() {
        return result.entrySet();
    }

    public String toString() {
        return result.toString();
    }
}
