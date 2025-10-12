package bigsky.pika.customization;

import bigsky.pika.TestBase;
import bigsky.pika.models.BadModel;
import bigsky.pika.models.SampleEgb;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

public class EnterprisePikaBeanTest extends TestBase {

    @Test
    void testInsertAsRecord() {
        initTestDb(SampleEgb.DDL);
        SampleEgb sampleModel = new SampleEgb("foo", 10, true, new Date(2021, 1, 1));
        long id = sampleModel.insert();
        assertEquals(1, id);
    }

    @Test
    void testFieldValidation() {
        initTestDb(SampleEgb.DDL);
        SampleEgb sampleModel = new SampleEgb("foo", -10, true, new Date(2021, 1, 1));
        Long id = sampleModel.insert();
        assertTrue(sampleModel.hasErrors());
        assertEquals(null, id);
    }

    @Test
    void testUnmappedFieldDoesNotCauseError() {
        initTestDb(BadModel.DDL);
        BadModel badModel = new BadModel();
        badModel.insert();
        assertNotNull(badModel.getId());
        assertNull(badModel.getUnmappedField());
    }

}
