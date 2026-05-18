package edu.montana.pika.util;

import edu.montana.pika.query.PikaList;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public interface PikaIterable<T> extends Iterable<T> {

    default <Q> PikaList<Q> map(Function<T, Q> mapper) {
        PikaList<Q> mappedResult = new PikaList<>();
        for (T t : this) {
            mappedResult.add(mapper.apply(t));
        }
        return mappedResult;
    }

    default Set<T> toSet() {
        LinkedHashSet<T> ts = new LinkedHashSet<>();
        forEach(ts::add);
        return ts;
    }

    default PikaList<T> toList() {
        PikaList<T> ts = new PikaList<>();
        forEach(ts::add);
        return ts;
    }

    default <K> Map<K, List<T>> toMap(Function<T, K> mapper) {
        Map<K, List<T>> mappedResult = new LinkedHashMap<>();
        for (T t : this) {
            mappedResult
                    .computeIfAbsent(mapper.apply(t), val -> new ArrayList<>())
                    .add(t);
        }
        return mappedResult;
    }

    default <K> TreeMap<K, List<T>> toOrderedMap(Function<T, K> mapper) {
        TreeMap<K, List<T>> mappedResult = new TreeMap<>();
        for (T t : this) {
            mappedResult
                    .computeIfAbsent(mapper.apply(t), val -> new ArrayList<>())
                    .add(t);
        }
        return mappedResult;
    }

    default <K> TreeMap<K, List<T>> toOrderedMap(Function<T, K> mapper, Comparator<? super K> comparator) {
        TreeMap<K, List<T>> mappedResult = new TreeMap<>(comparator);
        for (T t : this) {
            mappedResult
                    .computeIfAbsent(mapper.apply(t), val -> new ArrayList<>())
                    .add(t);
        }
        return mappedResult;
    }

    default <K> Map<K, T> toDistinctMap(Function<T, K> mapper) {
        Map<K, T> mappedResult = new HashMap<>();
        for (T t : this) {
            mappedResult.put(mapper.apply(t), t);
        }
        return mappedResult;
    }

    default <K> TreeMap<K, T> toOrderedDistinctMap(Function<T, K> mapper) {
        TreeMap<K, T> mappedResult = new TreeMap<>();
        for (T t : this) {
            mappedResult.put(mapper.apply(t), t);
        }
        return mappedResult;
    }

    default <K> TreeMap<K, T> toOrderedDistinctMap(Function<T, K> mapper, Comparator<? super K> comparator) {
        TreeMap<K, T> mappedResult = new TreeMap<>(comparator);
        for (T t : this) {
            mappedResult.put(mapper.apply(t), t);
        }
        return mappedResult;
    }

    default PikaList<T> filter(Predicate<? super T> filter) {
        PikaList<T> mappedResult = new PikaList<>();
        for (T t : this) {
            if (filter.test(t)) {
                mappedResult.add(t);
            }
        }
        return mappedResult;
    }

    default String toString(String separator) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (T t : this) {
            if (i != 0) {
                builder.append(separator);
            }
            builder.append(t);
            i++;
        }
        return builder.toString();
    }

    default T first() {
        for (T t : this) {
            return t;
        }
        return null;
    }

    default T firstOrThrow(String msg) {
        T val = first();
        if (val != null) {
            return val;
        }
        throw new IllegalStateException("no record found: " + msg);
    }

    default T firstWhere(Predicate<? super T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

    default boolean hasMatch(Predicate<? super T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) {
                return true;
            }
        }
        return false;
    }

    default boolean hasNoMatch(Predicate<? super T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) {
                return false;
            }
        }
        return true;
    }
}
