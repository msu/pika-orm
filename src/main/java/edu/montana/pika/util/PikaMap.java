package edu.montana.pika.util;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PikaMap<K, V> extends AbstractMap<K, V> implements PikaIterable<Map.Entry<K, V>> {

    private final Map<K, V> delegate;
    private final Supplier<? extends V> onMiss;

    //=================================================================
    // Constructors, etc.
    //=================================================================
    public PikaMap() {
        this(new LinkedHashMap<>());
    }

    public PikaMap(Map<K, V> c) {
        this(c, null);
    }

    private PikaMap(Map<K, V> c, Supplier<? extends V> onMiss) {
        this.delegate = c;
        this.onMiss = onMiss;
    }

    //=================================================================
    // Map implementation
    //=================================================================

    @Override
    public V get(Object key) {
        if (onMiss == null || delegate.containsKey(key)) {
            return delegate.get(key);
        }
        return onMiss.get();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public V replace(K key, V value) {
        return delegate.replace(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return delegate.merge(key, value, remappingFunction);
    }

    @Override
    public V put(K key, V value) {
        return delegate.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public PikaSet<Entry<K, V>> entrySet() {
        return new PikaSet<>(delegate.entrySet());
    }

    @Override
    public PikaSet<K> keySet() {
        return new PikaSet<>(delegate.keySet());
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return delegate.entrySet().iterator();
    }

    //=================================================================
    // Extension methods
    //=================================================================

    /**
     * Returns a view of this map where get() returns value for a missing key.
     * A miss does not change the map. Use only an immutable value, because
     * every miss returns the same object. For a new object on each miss, use
     * onMiss(Supplier).
     */
    public PikaMap<K, V> onMiss(V value) {
        return new PikaMap<>(delegate, () -> value);
    }

    /**
     * Returns a view of this map where get() calls supplier for a missing key.
     * A miss does not change the map. To store the value, use computeIfAbsent().
     */
    public PikaMap<K, V> onMiss(Supplier<? extends V> supplier) {
        return new PikaMap<>(delegate, supplier);
    }

    public PikaMap<K, V> filterAsMap(Predicate<? super Map.Entry<K, V>> filter) {
        PikaMap<K, V> mappedResult = new PikaMap<>();
        for (Map.Entry<K, V> t : this) {
            if (filter.test(t)) {
                mappedResult.put(t.getKey(), t.getValue());
            }
        }
        return mappedResult;
    }

    public PikaMap<K, V> filterByValues(Predicate<? super V> filter) {
        PikaMap<K, V> mappedResult = new PikaMap<>();
        for (Map.Entry<K, V> t : this) {
            if (filter.test(t.getValue())) {
                mappedResult.put(t.getKey(), t.getValue());
            }
        }
        return mappedResult;
    }

    public PikaMap<K, V> filterByKeys(Predicate<? super K> filter) {
        PikaMap<K, V> mappedResult = new PikaMap<>();
        for (Map.Entry<K, V> t : this) {
            if (filter.test(t.getKey())) {
                mappedResult.put(t.getKey(), t.getValue());
            }
        }
        return mappedResult;
    }

    public <Q> PikaMap<K, Q> mapValues(Function<V, Q> mapper) {
        PikaMap<K, Q> mappedResult = new PikaMap<>();
        for (Map.Entry<K, V> t : this) {
            mappedResult.put(t.getKey(), mapper.apply(t.getValue()));
        }
        return mappedResult;
    }

    public PikaMap<K, V> copy() {
        return new PikaMap<>(new LinkedHashMap<>(delegate));
    }

    public PikaMap<K, V> concat(PikaMap<K, V> other) {
        PikaMap<K, V> pikaMap = new PikaMap<>();
        pikaMap.putAll(this);
        pikaMap.putAll(other);
        return pikaMap;
    }
}
