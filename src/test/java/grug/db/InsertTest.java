package grug.db;

import grug.db.models.HasEnum;
import grug.db.models.OptimisticBean;
import grug.db.models.SampleModel;
import grug.db.models.SampleEgb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class InsertTest extends TestBase {

    @Test
    void testInsert() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        long id = orm.insert(sampleModel);
        assertEquals(1, id);
    }

   /*
   Bulk insertion compatability
   ALL BUT MARIADB (Not done with the compatability changes I believe)
    */
    @Test
    void testInsertAll(){//new test for the bulk insert
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel2 = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel3 = new SampleModel("zee", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel4 = new SampleModel("hoo", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel5 = new SampleModel("daw", 10, true, new Date(2021, 1, 1));
        SampleModel sampleModel6 = new SampleModel("jaw", 10, true, new Date(2021, 1, 1));

        orm.insertAll(sampleModel, sampleModel2, sampleModel3, sampleModel4, sampleModel5, sampleModel6);

        var query = orm.query(SampleModel.class)
                .where("date_val = :val")
                .withVar("val", new Date(2021, 1, 1));
        GrugORM.ResultList<SampleModel> result = query.execute();
        assertEquals(6, result.size());
    }

    @Test//check with error stuff
    void testFailInsertAllMultiClass(){


        var orm = initTestDb(SampleModel.DDL);
        try {
            SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel2 = new SampleModel("bar", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel3 = new SampleModel("zee", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel4= new SampleModel("hoo", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel5 = new SampleModel("daw", 10, true, new Date(2021, 1, 1));
            SampleModel sampleModel6 = new SampleModel("jaw", 10, true, new Date(2021, 1, 1));
            OptimisticBean sampleModel7 = new OptimisticBean();
            orm.insertAll(sampleModel, sampleModel2, sampleModel3, sampleModel4, sampleModel5, sampleModel6, sampleModel7);
            fail("Should fail as there are more than one class being mass inserted at once");
        } catch (RuntimeException e){
            assertEquals("insertAll Class type failed", e.getMessage());
        }




    }

}
