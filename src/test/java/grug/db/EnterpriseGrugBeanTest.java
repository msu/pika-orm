package grug.db;

import grug.db.models.SampleEgb;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnterpriseGrugBeanTest extends TestBase {

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
        long id = sampleModel.insert();
        assertTrue(sampleModel.hasErrors());
        assertEquals(GrugORM.INSERT_FAILED, id);
    }

}
