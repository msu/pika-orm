package grug.db;

import grug.db.models.SampleModel;
import grug.db.models.SampleEgb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsertTest extends TestBase {

    @Test
    void testInsert() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        long id = orm.insert(sampleModel);
        assertEquals(1, id);
    }

}
