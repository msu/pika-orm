package grug.db;

import grug.db.models.SampleGrugRecord;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GrugRecordTest {

    @Test
    void testInsertAsRecord() {
        SampleGrugRecord sampleModel = new SampleGrugRecord("foo", 10, true, new Date(2021, 1, 1));
        long id = sampleModel.insert();
        assertEquals(1, id);
    }


}
