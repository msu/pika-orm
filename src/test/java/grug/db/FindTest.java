package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FindTest extends TestBase{

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(SampleModel.DDL);
        orm.exec(SampleRecord.DDL);
    }

    @Test
    void testFind() {
        SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
        long id = orm.insert(sampleModel);

        SampleModel fromDb = orm.find(SampleModel.class, "id", id);

        assertEquals(id, sampleModel.getId());
        assertEquals(fromDb.getStrVal(), sampleModel.getStrVal());
        assertEquals(fromDb.getDateVal(), sampleModel.getDateVal());
        assertEquals(fromDb.getBoolVal(), sampleModel.getBoolVal());
        assertEquals(fromDb.getIntVal(), sampleModel.getIntVal());
    }

    @Test
    void testFindAll() {
        for (int i = 0; i < 10 ; i++) {
            SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }

        List<SampleModel> results =
                orm.findAll(SampleModel.class,
                        "int_val=:val",
                        Map.of("val", 10));

        assertEquals(10, results.size());

        results = orm.findAll(SampleModel.class, "date_val > :val", Map.of("val", new Date(2050, 1, 1)));

        assertEquals(0, results.size());

        results = orm.findAll(SampleModel.class, "str_val like :val", Map.of("val", "%b%"));
        assertEquals(10, results.size());

        results = orm.findAll(SampleModel.class);
        assertEquals(10, results.size());
    }

}
