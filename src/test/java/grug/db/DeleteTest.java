package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleEgb;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeleteTest extends TestBase {

    @Test
    public void testBasicUpdate() {
        var orm = initTestDb(SampleModel.DDL, SampleEgb.DDL);

        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        orm.insert(sampleModel);

        assertEquals(1, orm.find(SampleModel.class).all().size());

        orm.delete(sampleModel);

        assertEquals(0, orm.find(SampleModel.class).all().size());

    }



}
