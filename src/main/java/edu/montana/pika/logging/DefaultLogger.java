package edu.montana.pika.logging;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultLogger implements PikaLogger {
    private final Supplier<PikaLogger.Level> internalLoggerLevel;
    final Pattern parens = Pattern.compile("\\{}");

    public DefaultLogger(PikaLogger.Level internalLoggerLevel) {
        this(() -> internalLoggerLevel);
    }

    public DefaultLogger(Supplier<PikaLogger.Level> internalLoggerLevel) {
        this.internalLoggerLevel = internalLoggerLevel;
    }

    public void log(Level level, String msg, Object... args) {
        if (level.ordinal() <= internalLoggerLevel.get().ordinal()) {
            String logMsg = "[" + Instant.now() + "] " + level + ": " + msg;
            if (args.length > 0) {
                int index = 0;
                Matcher matcher = parens.matcher(logMsg);
                StringBuilder fixedString = new StringBuilder();
                while (matcher.find()) {
                    matcher.appendReplacement(fixedString, "{" + index + "}");
                    index = index + 1;
                }
                matcher.appendTail(fixedString);
                logMsg = MessageFormat.format(fixedString.toString(), args);
            }
            if (level.ordinal() == Level.ERROR.ordinal()) {
                System.err.println(logMsg);
            } else {
                System.out.println(logMsg);
            }
        }
    }
}
