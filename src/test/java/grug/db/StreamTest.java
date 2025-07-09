package grug.db;

import grug.db.GrugORM.ResultList;
import grug.db.models.SampleEgb;
import grug.db.models.SampleModel;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StreamTest extends TestBase{

    @Test
    void testStream() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
        SampleModel sampleModel = new SampleModel("bar", 10, true, new Date());
        long id = orm.insert(sampleModel);
        try (var _ = orm.establishConnection()) {
            var stream = orm.find(SampleModel.class).allAsStream();
            var fromDb = stream.findFirst().get();

            assertEquals(id, sampleModel.getId());
            assertEquals(fromDb.getStrVal(), sampleModel.getStrVal());
            Date dateFromDb = fromDb.getDateVal();
            Date dateFromModel = sampleModel.getDateVal();
            // mariadb rounds DATETIME to the nearest second
            assertEquals(dateFromDb.toInstant().truncatedTo(ChronoUnit.SECONDS), dateFromModel.toInstant().truncatedTo(ChronoUnit.SECONDS));
            assertEquals(fromDb.getBoolVal(), sampleModel.getBoolVal());
            assertEquals(fromDb.getIntVal(), sampleModel.getIntVal());
        }
    }

    @Test
    void testFindAll() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);
        for (int i = 0; i < 10 ; i++) {
            SampleModel sampleModel = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            long id = orm.insert(sampleModel);
            sampleModel.setId(id);
        }
        try (var _ = orm.establishConnection()) {

            List<SampleModel> results =
                    orm.find(SampleModel.class)
                            .whereAsStream("int_val=:val", Map.of("val", 10))
                            .toList();

            assertEquals(10, results.size());

            results = orm.find(SampleModel.class)
                    .whereAsStream("date_val > :val", Map.of("val", new Date(2050, 1, 1)))
                    .toList();

            assertEquals(0, results.size());

            results = orm.find(SampleModel.class)
                    .whereAsStream("str_val like :val", Map.of("val", "%b%"))
                    .toList();
            assertEquals(10, results.size());

            results = orm.find(SampleModel.class).allAsStream().toList();
            assertEquals(10, results.size());
        }
    }
}
