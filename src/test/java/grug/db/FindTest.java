package grug.db;

import grug.db.GrugORM.QueryResult;
import grug.db.models.SampleModel;
import grug.db.models.SampleEgb;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FindTest extends TestBase{

    @Test
    void testFind() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
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
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
        SampleModel sampleModel = new SampleModel("bar", 10, true, new Date());
        SampleModel sampleModel2 = new SampleModel("bar", 11, true, new Date());
        orm.insertAll(sampleModel, sampleModel2);

        SampleModel result = orm.find(SampleModel.class).firstWhere("str_val=:val", Map.of("val", "bar"));
        assertEquals(sampleModel.getIntVal(), result.getIntVal());
    }

    @Test
    void testFindAll() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
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
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
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
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2  = new SampleModel("bar", 10, true, new Date());
        SampleModel m3 = new SampleModel("baz", 10, true, new Date());

        SampleModel[] sampleModels = new SampleModel[] {m1, m2, m3};
        orm.insertAll(sampleModels);
        var results = orm.select("SELECT * FROM sample_models WHERE int_val=:x ORDER BY id", Map.of("x", 10)).toList();

        System.out.println(results);

        assertEquals(3, results.size());
        GrugORM.ResultMap first = results.getFirst();


        GrugORM.ResultMap insensitive = first.toCaseInsensitiveMap();
        assertEquals("foo", insensitive.get("str_val"));
        assertEquals(10, insensitive.get("int_val"));
        assertEquals(true, insensitive.asBoolean("bool_val"));
        // mariadb rounds DATETIME to the nearest second
        assertEquals(m1.getDateVal().toInstant().truncatedTo(ChronoUnit.SECONDS), insensitive.asDate("date_val").toInstant().truncatedTo(ChronoUnit.SECONDS));
    }

    record SampleModelGroupByQuery(String strVal, Long sum) {
        public static QueryResult<SampleModelGroupByQuery> exec() {
            return GrugORM.get().select("""
                            SELECT str_val, sum(int_val) as sum
                            FROM sample_models
                            GROUP BY str_val
                            ORDER BY str_val""", SampleModelGroupByQuery.class);
        }
    }

    @Test
    void testGenericSelectWitRecord() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);

        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2  = new SampleModel("bar", 10, true, new Date());
        SampleModel m3 = new SampleModel("foo", 10, true, new Date());

        SampleModel[] sampleModels = new SampleModel[] {m1, m2, m3};
        orm.insertAll(sampleModels);

        var results = SampleModelGroupByQuery.exec().toList();

        System.out.println(results);

        assertEquals(2, results.size());

        SampleModelGroupByQuery first = results.getFirst();
        assertEquals("bar", first.strVal());
        assertEquals(10, first.sum());

        SampleModelGroupByQuery second = results.get(1);
        assertEquals("foo", second.strVal());
        assertEquals(20, second.sum());
    }

}
