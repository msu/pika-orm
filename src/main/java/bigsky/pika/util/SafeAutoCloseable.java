package bigsky.pika.util;

public interface SafeAutoCloseable extends AutoCloseable {
    void close();
}
