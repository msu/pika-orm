package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryBuilderTest extends TestBase {

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(SampleModel.DDL);
        orm.exec(SampleRecord.DDL);
    }

    @Test
    void testBasicQueryBuilder() {

        for (int i = 0; i < 10 ; i++) {
            SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }

        var query = orm.query(SampleModel.class)
                .where("date_val < :val")
                .with("val", new Date(2050, 1, 1));

        List<SampleModel> results = query.run();

        assertEquals(10, results.size());

    }

    @Test
    void testBasicQueryBuilderWithStaticMethod() {

        for (int i = 0; i < 10 ; i++) {
            SampleRecord sampleModel = new SampleRecord("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }

        var query = SampleRecord
                .where("date_val < :val")
                .with("val", new Date(2050, 1, 1));

        List<SampleRecord> results = query.run();

        assertEquals(10, results.size());
    }

}
