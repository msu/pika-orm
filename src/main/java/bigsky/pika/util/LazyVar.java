package bigsky.pika.util;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

public class LazyVar<T> implements Callable<T> {
    private volatile boolean set = false;
    private ReentrantLock lock = new ReentrantLock();
    private final Callable<T> valueProducer;
    private T value = null;

    public LazyVar(Callable<T> valueProducer) {
        this.valueProducer = valueProducer;
    }

    @Override
    public T call() throws Exception {
        if (!set) {
            lock.lock();
            try {
                if(!set) {
                    value = valueProducer.call();
                    set = true;
                }
            } finally {
                lock.unlock();
            }
        }
        return value;
    }

    public T get() {
        try {
            return call();
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    private static <E extends Throwable> RuntimeException rethrow(Throwable e) throws E {
        throw (E) e;
    }
}
