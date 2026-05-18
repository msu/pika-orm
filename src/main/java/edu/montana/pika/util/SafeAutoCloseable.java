package edu.montana.pika.util;

public interface SafeAutoCloseable extends AutoCloseable {
    void close();
}
