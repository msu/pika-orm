package grug.db;

import grug.db.models.HasDate;
import grug.db.models.HasEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static grug.db.models.HasEnum.MyEnum.BAR;
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
        assertEquals(date, fromDb.getDate());
    }

}
