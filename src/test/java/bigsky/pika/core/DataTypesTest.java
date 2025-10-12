package bigsky.pika.core;

import bigsky.pika.TestBase;
import bigsky.pika.models.HasDate;
import bigsky.pika.models.HasEnum;
import bigsky.pika.models.HasUUID;
import bigsky.pika.models.SampleModel;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static bigsky.pika.models.HasEnum.MyEnum.BAR;
import static bigsky.pika.models.HasEnum.MyEnum.FOO;
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
    void testStringUUIDSerializeAndDeserialize() {
        var orm = initTestDb(HasUUID.DDL);
        HasUUID hasUUID = new HasUUID();
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        hasUUID.setUUID(uuid);
        long id = orm.insert(hasUUID);

        HasUUID fromDb = orm.find(HasUUID.class).byId(id);
        assertEquals(uuid, fromDb.getUUID());
    }

    @Test
    void testStringUUIDUpdate() {
        var orm = initTestDb(HasUUID.DDL);
        HasUUID hasUUID = new HasUUID();
        String uuid1 = "550e8400-e29b-41d4-a716-446655440000";
        hasUUID.setUUID(uuid1);
        long id = orm.insert(hasUUID);

        String uuid2 = "123e4567-e89b-12d3-a456-426614174000";
        hasUUID.setUUID(uuid2);
        orm.update(hasUUID);

        HasUUID fromDb = orm.find(HasUUID.class).byId(id);
        assertEquals(uuid2, fromDb.getUUID());
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

}
