package bigsky.pika.customization;

import bigsky.pika.TestBase;
import bigsky.pika.customization.model.BadModel;
import bigsky.pika.models.SampleEPB;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EnterprisePikaBeanTest extends TestBase {

    // Basic CRUD operations
    @Test
    void testInsertAsRecord() {
        initTestDb(SampleEPB.DDL);
        SampleEPB sampleModel = new SampleEPB("foo", 10, true, new Date(2021, 1, 1));
        long id = sampleModel.insert();
        assertEquals(1, id);
    }

    @Test
    void testUpdate() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("original", 10, true, new Date(2021, 1, 1));
        model.insert();

        model.setStrVal("updated");
        boolean result = model.update();

        assertTrue(result);
        SampleEPB reloaded = SampleEPB.find().byId(model.getId());
        assertEquals("updated", reloaded.getStrVal());
    }

    @Test
    void testUpdateThrowsIfNotPersisted() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        assertThrows(IllegalStateException.class, model::update);
    }

    @Test
    void testInsertThrowsIfAlreadyPersisted() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));
        model.insert();

        assertThrows(IllegalStateException.class, model::insert);
    }

    @Test
    void testSaveInsertsNewRecord() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("new", 10, true, new Date(2021, 1, 1));

        boolean result = model.save();

        assertTrue(result);
        assertNotNull(SampleEPB.find().byId(model.getId()));
    }

    @Test
    void testSaveUpdatesExistingRecord() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("original", 10, true, new Date(2021, 1, 1));
        model.insert();

        model.setStrVal("updated");
        boolean result = model.save();

        assertTrue(result);
        SampleEPB reloaded = SampleEPB.find().byId(model.getId());
        assertEquals("updated", reloaded.getStrVal());
    }

    @Test
    void testSaveOrThrowSucceeds() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        assertDoesNotThrow(model::saveOrThrow);
    }

    @Test
    void testSaveOrThrowFailsWithValidationError() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", -10, true, new Date(2021, 1, 1));

        assertThrows(IllegalStateException.class, model::saveOrThrow);
    }

    @Test
    void testDelete() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));
        model.insert();
        Long id = model.getId();

        boolean result = model.delete();

        assertTrue(result);
        assertNull(SampleEPB.find().byId(id));
    }

    // Validation
    @Test
    void testFieldValidation() {
        initTestDb(SampleEPB.DDL);
        SampleEPB sampleModel = new SampleEPB("foo", -10, true, new Date(2021, 1, 1));
        Long id = sampleModel.insert();
        assertTrue(sampleModel.hasErrors());
        assertEquals(null, id);
    }

    @Test
    void testValidateReturnsTrueWhenValid() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        assertTrue(model.validate());
        assertFalse(model.hasErrors());
    }

    @Test
    void testValidateReturnsFalseWhenInvalid() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", -10, true, new Date(2021, 1, 1));

        assertFalse(model.validate());
        assertTrue(model.hasErrors());
    }

    @Test
    void testClearErrors() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", -10, true, new Date(2021, 1, 1));
        model.validate();

        assertTrue(model.hasErrors());
        model.clearErrors();
        assertFalse(model.hasErrors());
    }

    // Error handling - general errors
    @Test
    void testAddGeneralError() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        model.addError("Something went wrong");

        assertTrue(model.hasErrors());
        assertTrue(model.getGeneralErrors().contains("Something went wrong"));
    }

    @Test
    void testGetGeneralErrors() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        model.addError("Error 1");
        model.addError("Error 2");

        assertEquals(2, model.getGeneralErrors().size());
    }

    // Error handling - field errors
    @Test
    void testAddFieldError() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        model.addError("fieldName", "Field error message");

        assertTrue(model.hasError("fieldName"));
        assertTrue(model.getErrors("fieldName").contains("Field error message"));
    }

    @Test
    void testHasErrorForField() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", -10, true, new Date(2021, 1, 1));
        model.validate();

        assertTrue(model.hasError("intVal"));
        assertFalse(model.hasError("strVal"));
    }

    @Test
    void testGetErrorsForField() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        model.addError("field1", "Error 1");
        model.addError("field1", "Error 2");

        assertEquals(2, model.getErrors("field1").size());
    }

    @Test
    void testGetErrorString() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        model.addError("field1", "Error 1");
        model.addError("field1", "Error 2");

        String errorString = model.getErrorString("field1");
        assertTrue(errorString.contains("Error 1"));
        assertTrue(errorString.contains("Error 2"));
    }

    @Test
    void testGetAllFieldErrors() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));

        model.addError("field1", "Error 1");
        model.addError("field2", "Error 2");
        model.addError("General error");

        Map<String, ?> fieldErrors = model.getAllFieldErrors();
        assertEquals(2, fieldErrors.size());
        assertTrue(fieldErrors.containsKey("field1"));
        assertTrue(fieldErrors.containsKey("field2"));
    }

    // Original values tracking
    @Test
    void testGetOriginalValueAfterInsert() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("original", 10, true, new Date(2021, 1, 1));
        model.insert();

        assertEquals("original", model.getOriginalValue("str_val"));
        assertEquals(10, model.getOriginalValue("int_val"));
    }

    @Test
    void testGetOriginalValueAfterUpdate() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("original", 10, true, new Date(2021, 1, 1));
        model.insert();

        model.setStrVal("updated");
        model.update();

        assertEquals("updated", model.getOriginalValue("str_val"));
    }

    @Test
    void testUpdateOnlyChangedValues() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));
        model.insert();

        // Don't change anything, update should still work
        boolean result = model.update();
        assertTrue(result);
    }

    // Reload
    @Test
    void testReload() {
        var orm = initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("original", 10, true, new Date(2021, 1, 1));
        model.insert();
        Long id = model.getId();

        // Modify directly in database
        orm.exec("UPDATE sample_epbs SET str_val = 'changed' WHERE id = " + id);

        model.reload();

        assertEquals("changed", model.getStrVal());
    }

    // Static finder
    @Test
    void testStaticFind() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", 10, true, new Date(2021, 1, 1));
        model.insert();

        SampleEPB found = SampleEPB.find().byId(model.getId());
        assertNotNull(found);
        assertEquals("test", found.getOriginalValue("str_val"));
    }

    @Test
    void testStaticFindReturnsNull() {
        initTestDb(SampleEPB.DDL);

        SampleEPB found = SampleEPB.find().byId(999L);
        assertNull(found);
    }

    // Unmapped field handling
    @Test
    void testUnmappedFieldDoesNotCauseError() {
        initTestDb(BadModel.DDL);
        BadModel badModel = new BadModel();
        badModel.insert();
        assertNotNull(badModel.getId());
        assertNull(badModel.getUnmappedField());
    }

    // Multiple records
    @Test
    void testMultipleRecordsIndependent() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model1 = new SampleEPB("first", 10, true, new Date(2021, 1, 1));
        SampleEPB model2 = new SampleEPB("second", 20, false, new Date(2021, 2, 1));

        model1.insert();
        model2.insert();

        model1.setStrVal("updated1");
        model1.update();

        SampleEPB reloaded1 = SampleEPB.find().byId(model1.getId());
        SampleEPB reloaded2 = SampleEPB.find().byId(model2.getId());

        assertEquals("updated1", reloaded1.getOriginalValue("str_val"));
        assertEquals("second", reloaded2.getOriginalValue("str_val"));
    }

    @Test
    void testValidationDoesNotAffectOtherRecords() {
        initTestDb(SampleEPB.DDL);
        SampleEPB valid = new SampleEPB("valid", 10, true, new Date(2021, 1, 1));
        SampleEPB invalid = new SampleEPB("invalid", -10, true, new Date(2021, 1, 1));

        valid.insert();
        invalid.insert();

        assertFalse(valid.hasErrors());
        assertTrue(invalid.hasErrors());
    }

    // Save behavior with validation
    @Test
    void testSaveReturnsFalseWithValidationError() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", -10, true, new Date(2021, 1, 1));

        boolean result = model.save();

        assertFalse(result);
        assertTrue(model.hasErrors());
    }

    @Test
    void testInsertReturnsFalseWithValidationError() {
        initTestDb(SampleEPB.DDL);
        SampleEPB model = new SampleEPB("test", -10, true, new Date(2021, 1, 1));

        Long id = model.insert();

        assertNull(id);
        assertTrue(model.hasErrors());
    }

}
