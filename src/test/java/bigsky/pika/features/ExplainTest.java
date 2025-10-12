package bigsky.pika.features;

import bigsky.pika.models.SampleEgb;
import org.junit.jupiter.api.Test;

import static bigsky.pika.TestBase.initTestDb;

public class ExplainTest {

    @Test
    public void testBasicExplain() {
        initTestDb(SampleEgb.DDL);
        var explain = SampleEgb.find().byQuery().where("str_val IS NOT NULL").explain();
        System.out.println(explain.toString("\n"));
    }

}
