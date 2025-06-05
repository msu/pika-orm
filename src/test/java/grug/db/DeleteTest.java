package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeleteTest extends TestBase {

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(SampleModel.DDL);
        orm.exec(SampleRecord.DDL);
    }

    @Test
    public void testBasicUpdate() {
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        orm.insert(sampleModel);

        assertEquals(1, orm.findAll(SampleModel.class).size());

        orm.delete(sampleModel);

        assertEquals(0, orm.findAll(SampleModel.class).size());

    }

}
