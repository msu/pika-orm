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


    @Test
    void testInsertAll(){//new test for the bulk insert
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel2 = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel3 = new SampleModel("zee", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel4= new SampleModel("hoo", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel5 = new SampleModel("daw", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel6 = new SampleModel("jaw", 10, true, new Date(2021, 1, 1));

        long ids[] = orm.insertAll(sampleModel, sampleModel2, sampleModel3, sampleModel4, sampleModel5, sampleModel6);

        assertEquals(6, ids.length);
        assertEquals(sampleModel.getId(), ids[0]);
    }

}
