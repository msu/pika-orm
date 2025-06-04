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

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {

        orm = initDBFileAndORM();

        orm.exec("""
                CREATE TABLE IF NOT EXISTS has_enum (
                    id INTEGER PRIMARY KEY,
                    my_enum TEXT NOT NULL
                );
                """);

        orm.exec("""
                CREATE TABLE IF NOT EXISTS has_date (
                    id INTEGER PRIMARY KEY,
                    date INTEGER NOT NULL
                );
                """);
    }

    @Test
    void enumsSerializeAndDeserialize() {
        HasEnum hasEnum = new HasEnum();
        hasEnum.setMyEnum(BAR);
        long id = orm.insert(hasEnum);

        HasEnum fromDb = orm.find(HasEnum.class, id);
        assertEquals(BAR, fromDb.getMyEnum());
    }

    @Test
    void datesSerializeAndDeserialize() {
        HasDate hasDate = new HasDate();
        Date date = new Date();
        hasDate.setDate(date);
        long id = orm.insert(hasDate);

        HasDate fromDb = orm.find(HasDate.class, id);
        assertEquals(date, fromDb.getDate());
    }

}
