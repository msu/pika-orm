package edu.montana.pika.util;

import edu.montana.pika.query.PikaList;

import java.util.*;
import java.util.function.*;

public interface PikaIterable<T> extends Iterable<T> {

    //==============================================================================
    // Transformations (eager, return PikaList)
    //==============================================================================

    default <R> PikaList<R> map(Function<T, R> mapper) {
        var out = new PikaList<R>();
        for (T t : this) {
            out.add(mapper.apply(t));
        }
        return out;
    }

    default <U> PikaList<U> flatMap(Function<T, Iterable<U>> mapper) {
        var out = new PikaList<U>();
        for (T t : this) {
            for (U u : mapper.apply(t)) {
                out.add(u);
            }
        }
        return out;
    }

    default PikaList<T> filter(Predicate<? super T> predicate) {
        var out = new PikaList<T>();
        for (T t : this) {
            if (predicate.test(t)) {
                out.add(t);
            }
        }
        return out;
    }

    default <U> PikaList<U> filter(Class<U> clazz) {
        var out = new PikaList<U>();
        for (T t : this) {
            if (clazz.isInstance(t)) {
                out.add(clazz.cast(t));
            }
        }
        return out;
    }

    default PikaList<T> filterOutNull() {
        return filter(Objects::nonNull);
    }

    default PikaList<T> skip(int n) {
        var out = new PikaList<T>();
        int i = 0;
        for (T t : this) {
            if (i++ >= n) out.add(t);
        }
        return out;
    }

    default PikaList<T> take(int n) {
        var out = new PikaList<T>();
        int i = 0;
        for (T t : this) {
            if (i++ >= n) break;
            out.add(t);
        }
        return out;
    }

    default PikaList<T> reverse() {
        var out = toList();
        Collections.reverse(out);
        return out;
    }

    default PikaList<T> unique() {
        return uniqueBy(t -> t);
    }

    default PikaList<T> uniqueBy(Function<T, Object> keyFn) {
        var seen = new HashSet<>();
        var out = new PikaList<T>();
        for (T t : this) {
            if (seen.add(keyFn.apply(t))) {
                out.add(t);
            }
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default PikaList<T> sorted() {
        var out = toList();
        out.sort((a, b) -> ((Comparable) a).compareTo(b));
        return out;
    }

    default PikaList<T> sorted(Comparator<? super T> comparator) {
        var out = toList();
        out.sort(comparator);
        return out;
    }

    default <R extends Comparable<R>> PikaList<T> sortedBy(Function<T, R> keyFn) {
        return sorted(Comparator.comparing(keyFn));
    }

    default <R> PikaList<T> sortedBy(Function<T, R> keyFn, Comparator<? super R> comparator) {
        return sorted(Comparator.comparing(keyFn, comparator));
    }

    //==============================================================================
    // Side-effect (returns this)
    //==============================================================================

    default PikaIterable<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        for (T t : this) {
            action.accept(t);
        }
        return this;
    }

    //==============================================================================
    // Reductions to a single value
    //==============================================================================

    default T first() {
        for (T t : this) {
            return t;
        }
        return null;
    }

    default T firstOrThrow() {
        for (T t : this) {
            return t;
        }
        throw new NoSuchElementException();
    }

    default T firstOrThrow(String msg) {
        T val = first();
        if (val != null) {
            return val;
        }
        throw new IllegalStateException("no record found: " + msg);
    }

    default T last() {
        T last = null;
        for (T t : this) {
            last = t;
        }
        return last;
    }

    default T lastOrThrow() {
        var iterator = iterator();
        if (!iterator.hasNext()) throw new NoSuchElementException();
        T last;
        do {
            last = iterator.next();
        } while (iterator.hasNext());
        return last;
    }

    default T firstWhere(Predicate<? super T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) return t;
        }
        return null;
    }

    default T firstWhereOrThrow(Predicate<? super T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) return t;
        }
        throw new NoSuchElementException();
    }

    default int indexOf(T value) {
        return indexOf(t -> Objects.equals(t, value));
    }

    default int indexOf(Predicate<? super T> predicate) {
        int i = 0;
        for (T t : this) {
            if (predicate.test(t)) return i;
            i++;
        }
        return -1;
    }

    // no count() here, PikaQuery.count() is a SQL count that returns long

    default <A> A fold(A initial, BiFunction<A, T, A> acc) {
        A out = initial;
        for (T t : this) {
            out = acc.apply(out, t);
        }
        return out;
    }

    //==============================================================================
    // Booleans
    //==============================================================================

    default boolean hasMatch(Predicate<? super T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) return true;
        }
        return false;
    }

    default boolean hasNoMatch(Predicate<? super T> predicate) {
        for (T t : this) {
            if (predicate.test(t)) return false;
        }
        return true;
    }

    default boolean all(Predicate<? super T> predicate) {
        for (T t : this) {
            if (!predicate.test(t)) return false;
        }
        return true;
    }

    //==============================================================================
    // Collect to collection
    //==============================================================================

    default PikaList<T> toList() {
        var out = new PikaList<T>();
        for (T t : this) {
            out.add(t);
        }
        return out;
    }

    default PikaSet<T> toSet() {
        var out = new PikaSet<T>();
        for (T t : this) {
            out.add(t);
        }
        return out;
    }

    //==============================================================================
    // Collect to map
    //==============================================================================

    default <K> PikaMap<K, PikaList<T>> toMap(Function<T, K> keyFn) {
        var map = new PikaMap<K, PikaList<T>>();
        for (T t : this) {
            map.computeIfAbsent(keyFn.apply(t), k -> new PikaList<>()).add(t);
        }
        return map;
    }

    default <K> PikaMap<K, PikaList<T>> toOrderedMap(Function<T, K> keyFn) {
        return toOrderedMap(keyFn, null);
    }

    default <K> PikaMap<K, PikaList<T>> toOrderedMap(Function<T, K> keyFn, Comparator<? super K> comparator) {
        var tree = new TreeMap<K, PikaList<T>>(comparator);
        for (T t : this) {
            tree.computeIfAbsent(keyFn.apply(t), k -> new PikaList<>()).add(t);
        }
        return new PikaMap<>(tree);
    }

    default <K> PikaMap<K, T> toDistinctMap(Function<T, K> keyFn) {
        var map = new PikaMap<K, T>();
        for (T t : this) {
            map.put(keyFn.apply(t), t);
        }
        return map;
    }

    default <K, V> PikaMap<K, V> toDistinctMap(Function<T, K> keyFn, Function<T, V> valueFn) {
        var map = new PikaMap<K, V>();
        for (T t : this) {
            map.put(keyFn.apply(t), valueFn.apply(t));
        }
        return map;
    }

    default <K> PikaMap<K, T> toOrderedDistinctMap(Function<T, K> keyFn) {
        return toOrderedDistinctMap(keyFn, null);
    }

    default <K> PikaMap<K, T> toOrderedDistinctMap(Function<T, K> keyFn, Comparator<? super K> comparator) {
        var tree = new TreeMap<K, T>(comparator);
        for (T t : this) {
            tree.put(keyFn.apply(t), t);
        }
        return new PikaMap<>(tree);
    }

    default <R> PikaMap<T, R> asMap(Function<T, R> valueFn) {
        var map = new PikaMap<T, R>();
        for (T t : this) {
            map.put(t, valueFn.apply(t));
        }
        return map;
    }

    //==============================================================================
    // Strings
    //==============================================================================

    default String toString(String delim) {
        var out = new StringBuilder();
        int i = 0;
        for (T t : this) {
            if (i++ > 0) out.append(delim);
            out.append(t);
        }
        return out.toString();
    }

    default String toString(String prefix, String delim, String suffix) {
        return prefix + toString(delim) + suffix;
    }
}
