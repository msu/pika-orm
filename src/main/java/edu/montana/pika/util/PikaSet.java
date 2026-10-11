package edu.montana.pika.util;

import java.util.*;
import java.util.function.Predicate;

public class PikaSet<T> extends AbstractSet<T> implements PikaIterable<T> {

    private final Set<T> delegate;

    //=================================================================
    // Constructors, etc.
    //=================================================================
    public PikaSet() {
        this(new LinkedHashSet<>());
    }

    /// wraps `c` if it is a Set, so changes go through to `c`. Otherwise copies it
    public PikaSet(Collection<? extends T> c) {
        if (c instanceof Set s) {
            delegate = s;
        } else {
            delegate = new LinkedHashSet<>(c);
        }
    }

    //=================================================================
    // Set implementation
    //=================================================================

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean add(T t) {
        return delegate.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    //=================================================================
    // Extension methods
    //=================================================================

    public PikaSet<T> filterAsSet(Predicate<? super T> filter) {
        PikaSet<T> mappedResult = new PikaSet<>();
        for (T t : this) {
            if (filter.test(t)) {
                mappedResult.add(t);
            }
        }
        return mappedResult;
    }

    public PikaSet<T> copy() {
        return new PikaSet<>(new LinkedHashSet<>(delegate));
    }

    public PikaSet<T> union(PikaSet<T> other) {
        PikaSet<T> pikaSet = new PikaSet<>();
        pikaSet.addAll(this);
        pikaSet.addAll(other);
        return pikaSet;
    }

    public PikaSet<T> intersect(PikaSet<T> other) {
        PikaSet<T> pikaSet = new PikaSet<>();
        for (T t : this) {
            if (other.contains(t)) {
                pikaSet.add(t);
            }
        }
        return pikaSet;
    }
}
