package bigsky.pika;

import bigsky.pika.models.SampleModel;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateTest extends TestBase {
    @Test
    public void testBasicUpdate() {
        var orm = initTestDb(SampleModel.DDL);
        SampleModel sampleModel = new SampleModel("foo", 10, true, new Date(2021, 1, 1));
        orm.insert(sampleModel);

        sampleModel.setStrVal("bar");
        orm.update(sampleModel);

        SampleModel fromDb = orm.find(SampleModel.class).byId(sampleModel.getId());

        assertEquals("bar", fromDb.getStrVal());
    }

}
