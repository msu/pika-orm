package bigsky.pika.query;

import bigsky.pika.TestBase;
import bigsky.pika.models.SampleModel;
import bigsky.pika.models.SampleEPB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassQueryTest extends TestBase {

    @BeforeEach
    public void setUp() throws IOException {
    }

    @Test
    void testBasicQueryBuilder() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);

        for (int i = 0; i < 10 ; i++) {
            SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }

        var query = orm.query(SampleModel.class)
                .where("date_val < :val")
                .withVar("val", new Date(2050, 1, 1));

        List<SampleModel> results = query.fetchList();

        assertEquals(10, results.size());

    }

    @Test
    void testBasicQueryBuilderWithStaticMethod() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);

        for (int i = 0; i < 10 ; i++) {
            SampleEPB sampleModel = new SampleEPB("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
        }

        var query = SampleEPB.find()
                .where("date_val < :val")
                .withVar("val", new Date(2050, 1, 1));

        List<SampleEPB> results = query.fetchList();

        assertEquals(10, results.size());
    }

    @Test
    void testChainedQueryBuilder() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);

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

        List<SampleModel> results = query.fetchList();//this problem is 100% isolated within the run query as everything is building correctly

        assertEquals(10, results.size());

    }

}
