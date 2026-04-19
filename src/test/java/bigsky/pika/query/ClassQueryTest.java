package bigsky.pika.query;

import bigsky.pika.TestBase;
import bigsky.pika.models.SampleModel;
import bigsky.pika.models.SampleEPB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

    // --- OR / grouping / IN / LIKE / aggregates ---

    private void seedMixed(bigsky.pika.PikaORM orm) {
        orm.insert(new SampleModel("alpha", 1, true, new Date(121, 0, 1)));
        orm.insert(new SampleModel("alpha", 5, false, new Date(121, 0, 1)));
        orm.insert(new SampleModel("beta",  2, true, new Date(121, 0, 1)));
        orm.insert(new SampleModel("beta",  8, false, new Date(121, 0, 1)));
        orm.insert(new SampleModel("gamma", 3, true, new Date(121, 0, 1)));
    }

    @Test
    void testOrWhere() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        var results = orm.query(SampleModel.class)
                .where("str_val = :a", "a", "alpha")
                .orWhere("str_val = :b", "b", "gamma")
                .fetchList();
        assertEquals(3, results.size());
    }

    @Test
    void testGroupWithOrWhereAndAnd() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        // (str = alpha OR str = beta) AND bool = true --> 2 rows
        var results = orm.query(SampleModel.class)
                .group()
                    .where("str_val = :a", "a", "alpha")
                    .orWhere("str_val = :b", "b", "beta")
                .endGroup()
                .where("bool_val = :t", "t", true)
                .fetchList();
        assertEquals(2, results.size());
    }

    @Test
    void testOrGroup() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        // str = gamma OR (bool = true AND int_val = 1) --> gamma row + alpha/1 row = 2
        var results = orm.query(SampleModel.class)
                .where("str_val = :g", "g", "gamma")
                .orGroup()
                    .where("bool_val = :t", "t", true)
                    .where("int_val = :n", "n", 1)
                .endGroup()
                .fetchList();
        assertEquals(2, results.size());
    }

    @Test
    void testUnbalancedGroupThrows() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        var q = orm.query(SampleModel.class).group().where("str_val = :a", "a", "alpha");
        IllegalStateException e = assertThrows(IllegalStateException.class, q::fetchList);
        assertTrue(e.getMessage().contains("Unbalanced"));
    }

    @Test
    void testEndGroupWithoutOpenThrows() {
        var orm = initTestDb(SampleModel.DDL);
        var q = orm.query(SampleModel.class);
        assertThrows(IllegalStateException.class, q::endGroup);
    }

    @Test
    void testWhereIn() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        var results = orm.query(SampleModel.class)
                .whereIn("int_val", List.of(1, 5, 8))
                .fetchList();
        assertEquals(3, results.size());
    }

    @Test
    void testWhereInEmptyReturnsNothing() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        var results = orm.query(SampleModel.class)
                .whereIn("int_val", List.of())
                .fetchList();
        assertEquals(0, results.size());
    }

    @Test
    void testWhereNotIn() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        var results = orm.query(SampleModel.class)
                .whereNotIn("str_val", List.of("alpha", "beta"))
                .fetchList();
        assertEquals(1, results.size());
    }

    @Test
    void testWhereLike() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        var results = orm.query(SampleModel.class)
                .whereLike("str_val", "a%")
                .fetchList();
        assertEquals(2, results.size());
    }

    @Test
    void testCount() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        assertEquals(5L, orm.find(SampleModel.class).count());
        assertEquals(2L, orm.find(SampleModel.class).where("str_val = :a", "a", "alpha").count());
    }

    @Test
    void testSumAvgMinMax() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        // 1 + 5 + 2 + 8 + 3 = 19
        assertEquals(19.0, orm.find(SampleModel.class).sum("int_val"));
        assertEquals(19.0 / 5.0, orm.find(SampleModel.class).avg("int_val"));
        assertEquals(1, ((Number) orm.find(SampleModel.class).min("int_val")).intValue());
        assertEquals(8, ((Number) orm.find(SampleModel.class).max("int_val")).intValue());
    }

    @Test
    void testAggregatesRespectWhere() {
        var orm = initTestDb(SampleModel.DDL);
        seedMixed(orm);
        long aCount = orm.find(SampleModel.class)
                .where("str_val = :s", Map.of("s", "alpha"))
                .count();
        assertEquals(2L, aCount);
    }

}
