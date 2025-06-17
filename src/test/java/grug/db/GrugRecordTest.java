package grug.db;

import grug.db.models.HasCustomizedMetadata;
import grug.db.models.SampleGrugRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrugRecordTest extends TestBase {

    GrugORM orm;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(SampleGrugRecord.DDL);
    }

    @Test
    void testInsertAsRecord() {
        SampleGrugRecord sampleModel = new SampleGrugRecord("foo", 10, true, new Date(2021, 1, 1));
        long id = sampleModel.insert();
        assertEquals(1, id);
    }

    @Test
    void testFieldValidation() {
        SampleGrugRecord sampleModel = new SampleGrugRecord("foo", -10, true, new Date(2021, 1, 1));
        long id = sampleModel.insert();
        assertTrue(sampleModel.hasErrors());
        assertEquals(GrugORM.INSERT_FAILED, id);
    }

}
