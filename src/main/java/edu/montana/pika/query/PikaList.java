package edu.montana.pika.query;

import edu.montana.pika.util.PikaIterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PikaList<T> extends ArrayList<T> implements PikaIterable<T> {

    public PikaList() {
    }

    public PikaList(Collection<? extends T> c) {
        super(c);
    }

    /// returns the value at `index` or returns the `defaultValue` if it is out of bounds
    public T get(int index, T defaultValue) {
        if (index >= 0 && index < size()) {
            return get(index);
        } else {
            return defaultValue;
        }
    }

    public T get(int index, Supplier<T> defaultValue) {
        if (index >= 0 && index < size()) {
            return get(index);
        } else {
            return defaultValue.get();
        }
    }

    public T getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return get(size() - 1);
        }
    }

    public T removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return remove(size() - 1);
        }
    }

    @Override
    public T last() {
        return isEmpty() ? null : get(size() - 1);
    }

    @Override
    public PikaList<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        for (T t : this) {
            action.accept(t);
        }
        return this;
    }

    public T lastWhere(Predicate<? super T> predicate) {
        for (int i = size() - 1; i >= 0; i--) {
            T t = get(i);
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

    public PikaList<T> copy() {
        return new PikaList<>(this);
    }

    public PikaList<T> concat(PikaList<T> other) {
        PikaList<T> ts = new PikaList<>();
        ts.addAll(this);
        ts.addAll(other);
        return ts;
    }
}
