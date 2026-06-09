package edu.montana.pika.core;

import edu.montana.pika.PikaORM;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PikaORM#parseDateString}. The helper is the shared core of the
 * Date string-to-Date fallback used by both coerce() and FieldMapping's Date read path.
 */
public class DateParsingTest {

    @Test
    void nullReturnsNull() {
        assertNull(PikaORM.parseDateString(null));
    }

    @Test
    void epochMillisAsString() {
        Date d = PikaORM.parseDateString("1779396033628");
        assertEquals(1779396033628L, d.getTime());
    }

    @Test
    void negativeEpochMillisAsString() {
        Date d = PikaORM.parseDateString("-1000");
        assertEquals(-1000L, d.getTime());
    }

    @Test
    void isoLocalDateTimeWithoutFraction() {
        Date d = PikaORM.parseDateString("2026-05-21T14:40:33");
        LocalDateTime expected = LocalDateTime.of(2026, 5, 21, 14, 40, 33);
        assertEquals(Date.from(expected.atZone(ZoneId.systemDefault()).toInstant()), d);
    }

    @Test
    void isoLocalDateTimeWithMillis() {
        Date d = PikaORM.parseDateString("2026-05-21T14:40:33.628");
        LocalDateTime expected = LocalDateTime.of(2026, 5, 21, 14, 40, 33, 628_000_000);
        assertEquals(Date.from(expected.atZone(ZoneId.systemDefault()).toInstant()), d);
    }

    @Test
    void isoOffsetDateTime() {
        Date d = PikaORM.parseDateString("2026-05-21T14:40:33+00:00");
        LocalDateTime expected = LocalDateTime.of(2026, 5, 21, 14, 40, 33);
        assertEquals(Date.from(expected.toInstant(ZoneOffset.UTC)), d);
    }

    @Test
    void sqlTextSpaceSeparated() {
        Date d = PikaORM.parseDateString("2026-05-21 14:40:33");
        LocalDateTime expected = LocalDateTime.of(2026, 5, 21, 14, 40, 33);
        assertEquals(Date.from(expected.atZone(ZoneId.systemDefault()).toInstant()), d);
    }

    @Test
    void sqlTextWithMicrosecondFraction() {
        Date d = PikaORM.parseDateString("2026-05-21 14:40:33.123456");
        // truncate to milliseconds since Date precision is millis
        LocalDateTime expected = LocalDateTime.of(2026, 5, 21, 14, 40, 33, 123_456_000);
        Date expectedDate = Date.from(expected.atZone(ZoneId.systemDefault()).toInstant());
        assertEquals(expectedDate.toInstant().truncatedTo(ChronoUnit.MILLIS),
                d.toInstant().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void dateOnly() {
        Date d = PikaORM.parseDateString("2026-05-21");
        LocalDate expected = LocalDate.of(2026, 5, 21);
        assertEquals(Date.from(expected.atStartOfDay(ZoneId.systemDefault()).toInstant()), d);
    }

    @Test
    void garbageThrows() {
        assertThrows(IllegalArgumentException.class, () -> PikaORM.parseDateString("not a date"));
    }

    @Test
    void emptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> PikaORM.parseDateString(""));
    }
}
