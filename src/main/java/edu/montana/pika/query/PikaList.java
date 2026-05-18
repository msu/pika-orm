package edu.montana.pika.query;

import edu.montana.pika.util.PikaIterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

public class PikaList<T> extends ArrayList<T> implements PikaIterable<T> {

    public PikaList() {
    }

    public PikaList(Collection<? extends T> c) {
        super(c);
    }

    public T last() {
        if (this.size() == 0) {
            return null;
        } else {
            return this.get(this.size() - 1);
        }
    }

    public T lastWhere(Predicate<? super T> predicate) {
        for (int i = this.size() - 1; i >= 0; i--) {
            var t = this.get(i);
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

    public PikaList<T> copy() {
        return new PikaList<>(this);
    }
}
