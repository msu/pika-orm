package bigsky.pika.logging;

public interface PikaLogger {
    enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE
    }

    void log(Level level, String msg, Object... args);
}