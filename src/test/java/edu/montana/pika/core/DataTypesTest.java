package edu.montana.pika.core;

import edu.montana.pika.TestBase;
import edu.montana.pika.core.model.HasDate;
import edu.montana.pika.core.model.HasEnum;
import edu.montana.pika.models.SampleModel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static edu.montana.pika.core.model.HasEnum.MyEnum.BAR;
import static edu.montana.pika.core.model.HasEnum.MyEnum.FOO;
import static org.junit.jupiter.api.Assertions.*;

public class DataTypesTest extends TestBase {

    @Test
    void enumsSerializeAndDeserialize() {
        var orm = initTestDb(HasEnum.DDL, HasDate.DDL);
        HasEnum hasEnum = new HasEnum();
        hasEnum.setMyEnum(BAR);
        long id = orm.insert(hasEnum);

        HasEnum fromDb = orm.find(HasEnum.class).byId(id);
        assertEquals(BAR, fromDb.getMyEnum());
    }

    @Test
    void datesSerializeAndDeserialize() {
        var orm = initTestDb(HasEnum.DDL, HasDate.DDL);
        HasDate hasDate = new HasDate();
        Date date = new Date();
        hasDate.setDate(date);
        long id = orm.insert(hasDate);

        HasDate fromDb = orm.find(HasDate.class).byId(id);
        // mariadb rounds DATETIME to the nearest second
        assertEquals(date.toInstant().truncatedTo(ChronoUnit.SECONDS), fromDb.getDate().toInstant().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void testEnumFooValue() {
        var orm = initTestDb(HasEnum.DDL);
        HasEnum hasEnum = new HasEnum();
        hasEnum.setMyEnum(FOO);
        long id = orm.insert(hasEnum);

        HasEnum fromDb = orm.find(HasEnum.class).byId(id);
        assertEquals(FOO, fromDb.getMyEnum());
    }

    @Test
    void testEnumUpdateValue() {
        var orm = initTestDb(HasEnum.DDL);
        HasEnum hasEnum = new HasEnum();
        hasEnum.setMyEnum(FOO);
        long id = orm.insert(hasEnum);

        hasEnum.setMyEnum(BAR);
        orm.update(hasEnum);

        HasEnum fromDb = orm.find(HasEnum.class).byId(id);
        assertEquals(BAR, fromDb.getMyEnum());
    }

    @Test
    void testMultipleEnumRecords() {
        var orm = initTestDb(HasEnum.DDL);

        HasEnum enum1 = new HasEnum();
        enum1.setMyEnum(FOO);
        orm.insert(enum1);

        HasEnum enum2 = new HasEnum();
        enum2.setMyEnum(BAR);
        orm.insert(enum2);

        HasEnum enum3 = new HasEnum();
        enum3.setMyEnum(FOO);
        orm.insert(enum3);

        assertEquals(FOO, orm.find(HasEnum.class).byId(enum1.getId()).getMyEnum());
        assertEquals(BAR, orm.find(HasEnum.class).byId(enum2.getId()).getMyEnum());
        assertEquals(FOO, orm.find(HasEnum.class).byId(enum3.getId()).getMyEnum());
    }

    @Test
    void testDateUpdate() {
        var orm = initTestDb(HasDate.DDL);
        HasDate hasDate = new HasDate();
        Date originalDate = new Date(2021, 1, 1);
        hasDate.setDate(originalDate);
        long id = orm.insert(hasDate);

        Date newDate = new Date(2022, 12, 31);
        hasDate.setDate(newDate);
        orm.update(hasDate);

        HasDate fromDb = orm.find(HasDate.class).byId(id);
        assertEquals(newDate.toInstant().truncatedTo(ChronoUnit.SECONDS),
                     fromDb.getDate().toInstant().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void testMultipleDateRecords() {
        var orm = initTestDb(HasDate.DDL);

        HasDate date1 = new HasDate();
        date1.setDate(new Date(2020, 1, 1));
        orm.insert(date1);

        HasDate date2 = new HasDate();
        date2.setDate(new Date(2021, 6, 15));
        orm.insert(date2);

        HasDate date3 = new HasDate();
        date3.setDate(new Date(2022, 12, 31));
        orm.insert(date3);

        assertNotNull(orm.find(HasDate.class).byId(date1.getId()).getDate());
        assertNotNull(orm.find(HasDate.class).byId(date2.getId()).getDate());
        assertNotNull(orm.find(HasDate.class).byId(date3.getId()).getDate());
    }

    @Test
    void testIntegerDataType() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 42, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals(42, fromDb.getIntVal());
    }

    @Test
    void testNegativeInteger() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", -999, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals(-999, fromDb.getIntVal());
    }

    @Test
    void testZeroInteger() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 0, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals(0, fromDb.getIntVal());
    }

    @Test
    void testBooleanTrueValue() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 10, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertTrue(fromDb.getBoolVal());
    }

    @Test
    void testBooleanFalseValue() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("test", 10, false, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertFalse(fromDb.getBoolVal());
    }

    @Test
    void testTextDataType() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("This is a test string", 10, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals("This is a test string", fromDb.getStrVal());
    }

    @Test
    void testEmptyStringValue() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel("", 10, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals("", fromDb.getStrVal());
    }

    @Test
    void testNullStringValue() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel model = new SampleModel(null, 10, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertNull(fromDb.getStrVal());
    }

    @Test
    void testLongStringValue() {
        var orm = initTestDb(SampleModel.DDL);
        String longString = "a".repeat(1000);
        SampleModel model = new SampleModel(longString, 10, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals(longString, fromDb.getStrVal());
    }

    @Test
    void testSpecialCharactersInString() {
        var orm = initTestDb(SampleModel.DDL);
        String specialChars = "Hello! @#$%^&*()_+-={}[]|:;\"'<>,.?/~`";
        SampleModel model = new SampleModel(specialChars, 10, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals(specialChars, fromDb.getStrVal());
    }

    @Test
    void testUnicodeCharactersInString() {
        var orm = initTestDb(SampleModel.DDL);
        String unicode = "Hello 世界 🌍 Привет";
        SampleModel model = new SampleModel(unicode, 10, true, new Date());
        orm.insert(model);

        SampleModel fromDb = orm.find(SampleModel.class).byId(model.getId());
        assertEquals(unicode, fromDb.getStrVal());
    }

    // Regression for FieldMapping's Date read-fallback. With a TEXT-affinity column,
    // sqlite-jdbc serializes Timestamps as millis-as-text but its own getTimestamp
    // refuses to parse that form back ("Error parsing time stamp"). The fallback path
    // in FieldMapping.readDateValue recovers via getString + PikaORM.parseDateString.
    @Test
    void testDateReadFromTextMillisColumn() {
        Assumptions.assumeTrue(getMode() == DatabaseMode.SQLITE);
        var orm = initTestDb("CREATE TABLE has_dates (id INTEGER PRIMARY KEY, date TEXT)");
        long millis = 1779396033628L;
        orm.exec("INSERT INTO has_dates (id, date) VALUES (1, '" + millis + "')");
        HasDate fromDb = orm.find(HasDate.class).byId(1);
        assertNotNull(fromDb);
        assertEquals(millis, fromDb.getDate().getTime());
    }

    @Test
    void testDateReadFromIsoTextColumn() {
        Assumptions.assumeTrue(getMode() == DatabaseMode.SQLITE);
        var orm = initTestDb("CREATE TABLE has_dates (id INTEGER PRIMARY KEY, date TEXT)");
        orm.exec("INSERT INTO has_dates (id, date) VALUES (1, '2026-05-21T14:40:33.628')");
        HasDate fromDb = orm.find(HasDate.class).byId(1);
        assertNotNull(fromDb);
        LocalDateTime expected = LocalDateTime.of(2026, 5, 21, 14, 40, 33, 628_000_000);
        assertEquals(Date.from(expected.atZone(ZoneId.systemDefault()).toInstant()), fromDb.getDate());
    }

    @Test
    void testDateReadFromSqlSpaceTextColumn() {
        Assumptions.assumeTrue(getMode() == DatabaseMode.SQLITE);
        var orm = initTestDb("CREATE TABLE has_dates (id INTEGER PRIMARY KEY, date TEXT)");
        orm.exec("INSERT INTO has_dates (id, date) VALUES (1, '2026-05-21 14:40:33')");
        HasDate fromDb = orm.find(HasDate.class).byId(1);
        assertNotNull(fromDb);
        LocalDateTime expected = LocalDateTime.of(2026, 5, 21, 14, 40, 33);
        assertEquals(Date.from(expected.atZone(ZoneId.systemDefault()).toInstant()), fromDb.getDate());
    }

}
