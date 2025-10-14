package bigsky.pika.core;

import bigsky.pika.TestBase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class CoercionsTest extends TestBase {

    // Null handling
    @Test
    void testCoerceNullReturnsNull() {
        var orm = initTestDb();
        assertNull(orm.coerce(String.class, null));
        assertNull(orm.coerce(Integer.class, null));
        assertNull(orm.coerce(Long.class, null));
    }

    // Same type (no conversion)
    @Test
    void testCoerceSameType() {
        var orm = initTestDb();
        String str = "hello";
        assertEquals(str, orm.coerce(String.class, str));

        Integer num = 42;
        assertEquals(num, orm.coerce(Integer.class, num));

        Long lng = 100L;
        assertEquals(lng, orm.coerce(Long.class, lng));
    }

    // String conversions
    @Test
    void testCoerceToString() {
        var orm = initTestDb();
        assertEquals("42", orm.coerce(String.class, 42));
        assertEquals("true", orm.coerce(String.class, true));
        assertEquals("3.14", orm.coerce(String.class, 3.14));
    }

    // Integer conversions
    @Test
    void testCoerceStringToInteger() {
        var orm = initTestDb();
        assertEquals(42, orm.coerce(Integer.class, "42"));
        assertEquals(-10, orm.coerce(Integer.class, "-10"));
    }

    @Test
    void testCoerceShortToInteger() {
        var orm = initTestDb();
        assertEquals(100, orm.coerce(Integer.class, (short) 100));
    }

    // Long conversions
    @Test
    void testCoerceStringToLong() {
        var orm = initTestDb();
        assertEquals(1000L, orm.coerce(Long.class, "1000"));
        assertEquals(-500L, orm.coerce(Long.class, "-500"));
    }

    @Test
    void testCoerceShortToLong() {
        var orm = initTestDb();
        assertEquals(200L, orm.coerce(Long.class, (short) 200));
    }

    @Test
    void testCoerceIntegerToLong() {
        var orm = initTestDb();
        assertEquals(5000L, orm.coerce(Long.class, 5000));
    }

    // Short conversions
    @Test
    void testCoerceStringToShort() {
        var orm = initTestDb();
        assertEquals((short) 10, orm.coerce(Short.class, "10"));
        assertEquals((short) -5, orm.coerce(Short.class, "-5"));
    }

    // Float conversions
    @Test
    void testCoerceStringToFloat() {
        var orm = initTestDb();
        assertEquals(3.14f, orm.coerce(Float.class, "3.14"));
        assertEquals(-2.5f, orm.coerce(Float.class, "-2.5"));
    }

    // Double conversions
    @Test
    void testCoerceStringToDouble() {
        var orm = initTestDb();
        assertEquals(3.14159, orm.coerce(Double.class, "3.14159"));
        assertEquals(-2.71828, orm.coerce(Double.class, "-2.71828"));
    }

    @Test
    void testCoerceFloatToDouble() {
        var orm = initTestDb();
        assertEquals(3.14, orm.coerce(Double.class, 3.14f), 0.01);
    }

    // BigInteger conversions
    @Test
    void testCoerceStringToBigInteger() {
        var orm = initTestDb();
        assertEquals(new BigInteger("123456789012345"),
                orm.coerce(BigInteger.class, "123456789012345"));
    }

    // BigDecimal conversions
    @Test
    void testCoerceStringToBigDecimal() {
        var orm = initTestDb();
        assertEquals(new BigDecimal("123.456"),
                orm.coerce(BigDecimal.class, "123.456"));
    }

    // Boolean conversions
    @Test
    void testCoerceToBooleanFromString() {
        var orm = initTestDb();
        assertTrue(orm.coerce(Boolean.class, "true"));
        assertTrue(orm.coerce(Boolean.class, "yes"));
        assertTrue(orm.coerce(Boolean.class, "anything"));
        assertFalse(orm.coerce(Boolean.class, "false"));
        assertFalse(orm.coerce(Boolean.class, "FALSE"));
    }

    @Test
    void testCoerceToBooleanFromNumber() {
        var orm = initTestDb();
        assertTrue(orm.coerce(Boolean.class, 1));
        assertTrue(orm.coerce(Boolean.class, 100));
        assertTrue(orm.coerce(Boolean.class, -1));
        assertFalse(orm.coerce(Boolean.class, 0));
    }

    @Test
    void testCoerceToBooleanFromBoolean() {
        var orm = initTestDb();
        assertTrue(orm.coerce(Boolean.class, true));
        assertFalse(orm.coerce(Boolean.class, false));
    }

    // Empty string and "null" handling for numbers
    @Test
    void testCoerceEmptyStringToNumberReturnsNull() {
        var orm = initTestDb();
        assertNull(orm.coerce(Integer.class, ""));
        assertNull(orm.coerce(Long.class, ""));
        assertNull(orm.coerce(Short.class, ""));
        assertNull(orm.coerce(Float.class, ""));
        assertNull(orm.coerce(Double.class, ""));
    }

    @Test
    void testCoerceNullStringToNumberReturnsNull() {
        var orm = initTestDb();
        assertNull(orm.coerce(Integer.class, "null"));
        assertNull(orm.coerce(Long.class, "null"));
        assertNull(orm.coerce(Short.class, "null"));
    }

    // Enum conversions
    enum TestEnum {
        FOO, BAR, BAZ
    }

    @Test
    void testCoerceStringToEnum() {
        var orm = initTestDb();
        assertEquals(TestEnum.FOO, orm.coerce(TestEnum.class, "foo"));
        assertEquals(TestEnum.BAR, orm.coerce(TestEnum.class, "BAR"));
        assertEquals(TestEnum.BAZ, orm.coerce(TestEnum.class, "baz"));
    }

    // LocalDateTime conversions
    @Test
    void testCoerceStringToLocalDateTime() {
        var orm = initTestDb();
        LocalDateTime result = orm.coerce(LocalDateTime.class, "2023-12-25 14:30:45.123");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
    }

    // Date conversions
    @Test
    void testCoerceStringToDate() {
        var orm = initTestDb();
        Date result = orm.coerce(Date.class, "2023-12-25 14:30:45");
        assertNotNull(result);
    }

    @Test
    void testCoerceLongStringToDate() {
        var orm = initTestDb();
        long timestamp = System.currentTimeMillis();
        Date result = orm.coerce(Date.class, String.valueOf(timestamp));
        assertNotNull(result);
        assertEquals(timestamp, result.getTime());
    }

    // Error cases
    @Test
    void testCoerceInvalidStringToIntegerThrows() {
        var orm = initTestDb();
        assertThrows(NumberFormatException.class, () -> {
            orm.coerce(Integer.class, "not-a-number");
        });
    }

    @Test
    void testCoerceInvalidStringToLongThrows() {
        var orm = initTestDb();
        assertThrows(NumberFormatException.class, () -> {
            orm.coerce(Long.class, "invalid");
        });
    }

    @Test
    void testCoerceInvalidStringToShortThrows() {
        var orm = initTestDb();
        assertThrows(NumberFormatException.class, () -> {
            orm.coerce(Short.class, "xyz");
        });
    }

    @Test
    void testCoerceInvalidStringToFloatThrows() {
        var orm = initTestDb();
        assertThrows(NumberFormatException.class, () -> {
            orm.coerce(Float.class, "not-a-float");
        });
    }

    @Test
    void testCoerceInvalidStringToDoubleThrows() {
        var orm = initTestDb();
        assertThrows(NumberFormatException.class, () -> {
            orm.coerce(Double.class, "invalid-double");
        });
    }

    @Test
    void testCoerceUnsupportedConversionThrows() {
        var orm = initTestDb();
        assertThrows(IllegalArgumentException.class, () -> {
            orm.coerce(Integer.class, new Object());
        });
    }

    // Primitive types
    @Test
    void testCoerceToPrimitiveInt() {
        var orm = initTestDb();
        assertEquals(42, orm.coerce(int.class, "42"));
        assertEquals(100, orm.coerce(int.class, (short) 100));
    }

    @Test
    void testCoerceToPrimitiveLong() {
        var orm = initTestDb();
        assertEquals(1000L, orm.coerce(long.class, "1000"));
        assertEquals(500L, orm.coerce(long.class, 500));
    }

    @Test
    void testCoerceToPrimitiveShort() {
        var orm = initTestDb();
        assertEquals((short) 10, orm.coerce(short.class, "10"));
    }

    @Test
    void testCoerceToPrimitiveFloat() {
        var orm = initTestDb();
        assertEquals(3.14f, orm.coerce(float.class, "3.14"));
    }

    @Test
    void testCoerceToPrimitiveDouble() {
        var orm = initTestDb();
        assertEquals(2.718, orm.coerce(double.class, "2.718"));
        assertEquals(3.14, orm.coerce(double.class, 3.14f), 0.01);
    }

    // Edge cases
    @Test
    void testCoerceZeroString() {
        var orm = initTestDb();
        assertEquals(0, orm.coerce(Integer.class, "0"));
        assertEquals(0L, orm.coerce(Long.class, "0"));
        assertEquals((short) 0, orm.coerce(Short.class, "0"));
    }

    @Test
    void testCoerceLargeNumbers() {
        var orm = initTestDb();
        assertEquals(Integer.MAX_VALUE, orm.coerce(Integer.class, String.valueOf(Integer.MAX_VALUE)));
        assertEquals(Long.MAX_VALUE, orm.coerce(Long.class, String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void testCoerceNegativeNumbers() {
        var orm = initTestDb();
        assertEquals(-42, orm.coerce(Integer.class, "-42"));
        assertEquals(-1000L, orm.coerce(Long.class, "-1000"));
        assertEquals(-3.14f, orm.coerce(Float.class, "-3.14"));
        assertEquals(-2.718, orm.coerce(Double.class, "-2.718"));
    }
}
