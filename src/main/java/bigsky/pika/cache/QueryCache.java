package bigsky.pika.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class QueryCache {
    ConcurrentHashMap<Object, Object> cache = new ConcurrentHashMap<>();
    public <T> T cache(Object key, Supplier<T> supplier) {
        return (T) cache.computeIfAbsent(key, o ->  supplier.get());
    }

    public void clear() {
        cache.clear();
    }
}
