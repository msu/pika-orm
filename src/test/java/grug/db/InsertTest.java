package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsertTest extends TestBase {

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(SampleModel.DDL);
        orm.exec(SampleRecord.DDL);
    }

    @Test
    void testInsert() {
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        long id = orm.insert(sampleModel);
        assertEquals(1, id);
    }

    @Test
    void testInsertAsRecord() {
        SampleRecord sampleModel = new SampleRecord("foo", 10, true, new Date(2021, 1, 1));
        long id = sampleModel.insert();
        assertEquals(1, id);
    }

}
