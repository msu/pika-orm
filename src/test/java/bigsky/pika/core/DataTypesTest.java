package bigsky.pika.core;

import bigsky.pika.TestBase;
import bigsky.pika.models.HasDate;
import bigsky.pika.models.HasEnum;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static bigsky.pika.models.HasEnum.MyEnum.BAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataTypesTest extends TestBase {

    @Test
    void enumsSerializeAndDeserialize() {
        var orm = initTestDb(HasEnum.DDL, HasDate.DDL);
        HasEnum hasEnum = new HasEnum();
        hasEnum.setMyEnum(BAR);
        long id = orm.insert(hasEnum);

        HasEnum fromDb = orm.find(HasEnum.class).byId(id);
        assertEquals(BAR, fromDb.getMyEnum());
    }

    @Test
    void datesSerializeAndDeserialize() {
        var orm = initTestDb(HasEnum.DDL, HasDate.DDL);
        HasDate hasDate = new HasDate();
        Date date = new Date();
        hasDate.setDate(date);
        long id = orm.insert(hasDate);

        HasDate fromDb = orm.find(HasDate.class).byId(id);
        // mariadb rounds DATETIME to the nearest second
        assertEquals(date.toInstant().truncatedTo(ChronoUnit.SECONDS), fromDb.getDate().toInstant().truncatedTo(ChronoUnit.SECONDS));
    }

}
