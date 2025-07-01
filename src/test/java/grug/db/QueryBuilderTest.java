package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleGrugRecord;
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
        orm.exec(SampleGrugRecord.DDL);
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
                .withVar("val", new Date(2050, 1, 1));

        List<SampleModel> results = query.execute();

        assertEquals(10, results.size());

    }

    @Test
    void testBasicQueryBuilderWithStaticMethod() {

        for (int i = 0; i < 10 ; i++) {
            SampleGrugRecord sampleModel = new SampleGrugRecord("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
        }

        var query = SampleGrugRecord
                .where("date_val < :val")
                .withVar("val", new Date(2050, 1, 1));

        List<SampleGrugRecord> results = query.execute();

        assertEquals(10, results.size());
    }

    @Test
    void testChainedQueryBuilder() {

        for (int i = 0; i < 10 ; i++) {
            SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(21, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }
        var query = orm.query(SampleModel.class)
                .where("date_val < :val")
                .withVar("val", new Date(150, 1, 1))
                .where("date_val > :val2")
                .withVar("val2", new Date(20, 1, 1));

        List<SampleModel> results = query.execute();//this problem is 100% isolated within the run query as everything is building correctly

        assertEquals(10, results.size());

    }

}
