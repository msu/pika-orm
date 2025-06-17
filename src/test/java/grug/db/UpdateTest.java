package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleGrugRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateTest extends TestBase {

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(SampleModel.DDL);
        orm.exec(SampleGrugRecord.DDL);
    }

    @Test
    public void testBasicUpdate() {
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        orm.insert(sampleModel);

        sampleModel.setStrVal("bar");
        orm.update(sampleModel);

        SampleModel fromDb = orm.find(SampleModel.class, sampleModel.getId());

        assertEquals("bar", fromDb.getStrVal());
    }

}
