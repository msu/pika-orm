package bigsky.pika.core;

import bigsky.pika.PikaORM;
import bigsky.pika.PikaORM.QueryResult;
import bigsky.pika.TestBase;
import bigsky.pika.models.SampleModel;
import bigsky.pika.models.SampleEPB;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SelectTest extends TestBase {

    @Test
    void testFind() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel sampleModel = new SampleModel("bar", 10, true, new Date());
        long id = orm.insert(sampleModel);

        SampleModel fromDb = orm.find(SampleModel.class).byId(id);

        assertEquals(id, sampleModel.getId());
        assertEquals(fromDb.getStrVal(), sampleModel.getStrVal());
        Date dateFromDb = fromDb.getDateVal();
        Date dateFromModel = sampleModel.getDateVal();
        // mariadb rounds DATETIME to the nearest second
        assertEquals(dateFromDb.toInstant().truncatedTo(ChronoUnit.SECONDS), dateFromModel.toInstant().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(fromDb.getBoolVal(), sampleModel.getBoolVal());
        assertEquals(fromDb.getIntVal(), sampleModel.getIntVal());
    }

    @Test
    void testFindFirst() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel sampleModel = new SampleModel("bar", 10, true, new Date());
        SampleModel sampleModel2 = new SampleModel("bar", 11, true, new Date());
        orm.insertAll(sampleModel, sampleModel2);

        SampleModel result = orm.find(SampleModel.class).firstWhere("str_val=:val", Map.of("val", "bar"));
        assertEquals(sampleModel.getIntVal(), result.getIntVal());
    }

    @Test
    void testFindAll() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        for (int i = 0; i < 10 ; i++) {
            SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }

        List<SampleModel> results =
                orm.find(SampleModel.class)
                        .where("int_val=:val", Map.of("val", 10)).toList();

        assertEquals(10, results.size());

        results = orm.find(SampleModel.class).where("date_val > :val", Map.of("val", new Date(2050, 1, 1))).toList();

        assertEquals(0, results.size());

        results = orm.find(SampleModel.class).where("str_val like :val", Map.of("val", "%b%")).toList();
        assertEquals(10, results.size());

        results = orm.find(SampleModel.class).all().toList();
        assertEquals(10, results.size());
    }

    @Test
    void testSelectWhere() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2  = new SampleModel("bar", 10, true, new Date());
        SampleModel m3 = new SampleModel("baz", 10, true, new Date());

        SampleModel[] sampleModels = new SampleModel[] {m1, m2, m3};
        orm.insertAll(sampleModels);
        var results = orm.find(SampleModel.class).where(//we are looking to create the query something like this, select * in sample model where str_val in (?) and (?) (for foo and bar)
                "str_val in :strs",
                Map.of("strs", List.of("foo", "bar")))
                .toList();//we can safely assume that when there is a map with collection inside of it we need to iterate over the list and create arguments and insertions for all parameters
                //TODO - should this test account for multiple of these collections? say there are 2 maps with different collections, we would possibly want to create question marks for all?
        assertEquals(2, results.size());
    }

    @Test
    void testGenericSelect() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2  = new SampleModel("bar", 10, true, new Date());
        SampleModel m3 = new SampleModel("baz", 10, true, new Date());

        SampleModel[] sampleModels = new SampleModel[] {m1, m2, m3};
        orm.insertAll(sampleModels);
        var results = orm.select("SELECT * FROM sample_models WHERE int_val=:x ORDER BY id", Map.of("x", 10)).toList();

        System.out.println(results);

        assertEquals(3, results.size());
        PikaORM.ResultMap first = results.get(0);


        PikaORM.ResultMap insensitive = first.toCaseInsensitiveMap();
        assertEquals("foo", insensitive.get("str_val"));
        assertEquals(10, insensitive.get("int_val"));
        assertEquals(true, insensitive.asBoolean("bool_val"));
        // mariadb rounds DATETIME to the nearest second
        assertEquals(m1.getDateVal().toInstant().truncatedTo(ChronoUnit.SECONDS), insensitive.asDate("date_val").toInstant().truncatedTo(ChronoUnit.SECONDS));
    }

    record SampleModelGroupByQuery(String strVal, Long sum) {
        public static QueryResult<SampleModelGroupByQuery> exec() {
            return PikaORM.get().select("""
                            SELECT str_val, sum(int_val) as sum
                            FROM sample_models
                            GROUP BY str_val
                            ORDER BY str_val""", SampleModelGroupByQuery.class);
        }
    }

    @Test
    void testGenericSelectWitRecord() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);

        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2  = new SampleModel("bar", 10, true, new Date());
        SampleModel m3 = new SampleModel("foo", 10, true, new Date());

        SampleModel[] sampleModels = new SampleModel[] {m1, m2, m3};
        orm.insertAll(sampleModels);

        var results = SampleModelGroupByQuery.exec().toList();

        System.out.println(results);

        assertEquals(2, results.size());

        SampleModelGroupByQuery first = results.get(0);
        assertEquals("bar", first.strVal());
        assertEquals(10, first.sum());

        SampleModelGroupByQuery second = results.get(1);
        assertEquals("foo", second.strVal());
        assertEquals(20, second.sum());
    }

    @Test
    void testFindByIdReturnsNull() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel result = orm.find(SampleModel.class).byId(999L);
        assertNull(result);
    }

    @Test
    void testFindFirstReturnsNullWhenNoMatch() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel sampleModel = new SampleModel("bar", 10, true, new Date());
        orm.insert(sampleModel);

        SampleModel result = orm.find(SampleModel.class).firstWhere("str_val=:val", Map.of("val", "nonexistent"));
        assertNull(result);
    }

    @Test
    void testWhereWithMultipleConditions() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2 = new SampleModel("foo", 20, true, new Date());
        SampleModel m3 = new SampleModel("bar", 10, true, new Date());
        orm.insertAll(m1, m2, m3);

        var results = orm.find(SampleModel.class)
                .where("str_val=:str AND int_val=:int", Map.of("str", "foo", "int", 10))
                .toList();

        assertEquals(1, results.size());
        assertEquals("foo", results.get(0).getStrVal());
        assertEquals(10, results.get(0).getIntVal());
    }

    @Test
    void testWhereWithBooleanCondition() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2 = new SampleModel("bar", 20, false, new Date());
        SampleModel m3 = new SampleModel("baz", 30, true, new Date());
        orm.insertAll(m1, m2, m3);

        var results = orm.find(SampleModel.class)
                .where("bool_val=:val", Map.of("val", true))
                .toList();

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(SampleModel::getBoolVal));
    }

    @Test
    void testSelectWithEmptyResult() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        var results = orm.select("SELECT * FROM sample_models WHERE int_val=:x", Map.of("x", 999)).toList();
        assertEquals(0, results.size());
    }

    @Test
    void testSelectWithNoParameters() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        orm.insert(m1);

        var results = orm.select("SELECT * FROM sample_models").toList();
        assertEquals(1, results.size());
    }

    @Test
    void testFindAllReturnsEmptyListWhenTableEmpty() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        List<SampleModel> results = orm.find(SampleModel.class).all().toList();
        assertEquals(0, results.size());
    }

    @Test
    void testWhereWithInClauseEmptyList() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        orm.insert(m1);

        var results = orm.find(SampleModel.class)
                .where("str_val in :strs", Map.of("strs", List.of()))
                .toList();

        assertEquals(0, results.size());
    }

    @Test
    void testWhereWithLikePattern() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("football", 10, true, new Date());
        SampleModel m2 = new SampleModel("basket", 20, true, new Date());
        SampleModel m3 = new SampleModel("foodie", 30, true, new Date());
        orm.insertAll(m1, m2, m3);

        var results = orm.find(SampleModel.class)
                .where("str_val like :pattern", Map.of("pattern", "foo%"))
                .toList();

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> m.getStrVal().startsWith("foo")));
    }

    @Test
    void testWhereWithNumericComparison() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        for (int i = 1; i <= 5; i++) {
            orm.insert(new SampleModel("test", i * 10, true, new Date()));
        }

        var results = orm.find(SampleModel.class)
                .where("int_val > :val", Map.of("val", 30))
                .toList();

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> m.getIntVal() > 30));
    }

    @Test
    void testSelectPreservesOrder() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("charlie", 30, true, new Date());
        SampleModel m2 = new SampleModel("alice", 10, true, new Date());
        SampleModel m3 = new SampleModel("bob", 20, true, new Date());
        orm.insertAll(m1, m2, m3);

        var results = orm.select("SELECT * FROM sample_models ORDER BY str_val ASC").toList();

        assertEquals(3, results.size());
        assertEquals("alice", results.get(0).get("str_val"));
        assertEquals("bob", results.get(1).get("str_val"));
        assertEquals("charlie", results.get(2).get("str_val"));
    }

    @Test
    void testMultipleInClauses() {
        var orm = initTestDb(SampleModel.DDL, SampleEPB.DDL);
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2 = new SampleModel("bar", 20, true, new Date());
        SampleModel m3 = new SampleModel("baz", 30, true, new Date());
        SampleModel m4 = new SampleModel("foo", 40, true, new Date());
        orm.insertAll(m1, m2, m3, m4);

        var results = orm.find(SampleModel.class)
                .where("str_val in :strs AND int_val in :ints",
                        Map.of("strs", List.of("foo", "bar"), "ints", List.of(10, 20)))
                .toList();

        assertEquals(2, results.size());
    }

}
