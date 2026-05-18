package edu.montana.pika.cache;

import edu.montana.pika.logging.PikaLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class QueryCache {
    ConcurrentHashMap<Object, Object> cache = new ConcurrentHashMap<>();
    private final PikaLogger logger;
    private final boolean logCacheHits;

    public QueryCache(PikaLogger logger, boolean logCacheHits) {
        this.logger = logger;
        this.logCacheHits = logCacheHits;
    }

    public <T> T cache(Object key, Supplier<T> supplier) {
        boolean wasInCache = cache.containsKey(key);

        T result = (T) cache.computeIfAbsent(key, o -> supplier.get());

        if (logCacheHits && logger != null) {
            if (wasInCache) {
                logger.log(PikaLogger.Level.INFO, "Cache HIT: {}", key);
            } else {
                logger.log(PikaLogger.Level.INFO, "Cache MISS: {}", key);
            }
        }

        return result;
    }

    public void clear() {
        cache.clear();
    }
}
