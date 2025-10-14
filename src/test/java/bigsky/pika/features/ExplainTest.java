package bigsky.pika.features;

import bigsky.pika.models.SampleEPB;
import org.junit.jupiter.api.Test;

import static bigsky.pika.TestBase.initTestDb;

public class ExplainTest {

    @Test
    public void testBasicExplain() {
        initTestDb(SampleEPB.DDL);
        var explain = SampleEPB.find().byQuery().where("str_val IS NOT NULL").explain();
        System.out.println(explain.toString("\n"));
    }

}
