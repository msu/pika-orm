package grug.db;

import grug.db.GrugORM.ResultList;
import grug.db.models.SampleModel;
import grug.db.models.SampleGrugRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FindTest extends TestBase{

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(SampleModel.DDL);
        orm.exec(SampleGrugRecord.DDL);
    }

    @Test
    void testFind() {
        SampleModel sampleModel = new SampleModel("bar", 10, true, new Date());
        long id = orm.insert(sampleModel);

        SampleModel fromDb = orm.find(SampleModel.class).byId(id);

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
                orm.find(SampleModel.class)
                        .where("int_val=:val", Map.of("val", 10));

        assertEquals(10, results.size());

        results = orm.find(SampleModel.class).where("date_val > :val", Map.of("val", new Date(2050, 1, 1)));

        assertEquals(0, results.size());

        results = orm.find(SampleModel.class).where("str_val like :val", Map.of("val", "%b%"));
        assertEquals(10, results.size());

        results = orm.find(SampleModel.class).all();
        assertEquals(10, results.size());
    }

    @Test
    void testSelectWhere() {
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2  = new SampleModel("bar", 10, true, new Date());
        SampleModel m3 = new SampleModel("baz", 10, true, new Date());

        SampleModel[] sampleModels = new SampleModel[] {m1, m2, m3};
        orm.insertAll(sampleModels);
        var results = orm.find(SampleModel.class).where(//we are looking to create the query something like this, select * in sample model where str_val in (?) and (?) (for foo and bar)
                "str_val in :strs",
                Map.of("strs", List.of("foo", "bar")));//we can safely assume that when there is a map with collection inside of it we need to iterate over the list and create arguements and insertions for all parameters
                //TODO - should this test account for multiple of these collections? say there are 2 maps with different collections, we would possibly want to create question marks for all?
        assertEquals(2, results.size());
    }

    @Test
    void testGenericSelect() {
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2  = new SampleModel("bar", 10, true, new Date());
        SampleModel m3 = new SampleModel("baz", 10, true, new Date());

        SampleModel[] sampleModels = new SampleModel[] {m1, m2, m3};
        orm.insertAll(sampleModels);
        var results = orm.select("SELECT * FROM sample_model WHERE int_val=:x ORDER BY id", Map.of("x", 10));

        System.out.println(results);

        assertEquals(3, results.size());
        GrugORM.ResultMap first = results.getFirst();


        assertEquals("foo", first.get("str_val"));
        assertEquals(10, first.get("int_val"));
        assertEquals(1, first.get("bool_val"));
    }

    record SampleModelGroupByQuery(String strVal, Long sum) {
        public static ResultList<SampleModelGroupByQuery> exec() {
            return GrugORM.get().select("""
                            SELECT str_val, sum(int_val) as sum
                            FROM sample_model
                            GROUP BY str_val
                            ORDER BY str_val""", SampleModelGroupByQuery.class);
        }
    }

    @Test
    void testGenericSelectWitRecord() {
        SampleModel m1 = new SampleModel("foo", 10, true, new Date());
        SampleModel m2  = new SampleModel("bar", 10, true, new Date());
        SampleModel m3 = new SampleModel("foo", 10, true, new Date());

        SampleModel[] sampleModels = new SampleModel[] {m1, m2, m3};
        orm.insertAll(sampleModels);

        var results = SampleModelGroupByQuery.exec();

        System.out.println(results);

        assertEquals(2, results.size());

        SampleModelGroupByQuery first = results.get(0);
        assertEquals("bar", first.strVal());
        assertEquals(10, first.sum());

        SampleModelGroupByQuery second = results.get(1);
        assertEquals("foo", second.strVal());
        assertEquals(20, second.sum());
    }

}
