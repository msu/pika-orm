package grug.db;

import grug.db.models.HasEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
    }


        @Test
    void enumsSerializeAndDeserialize() {
            HasEnum hasEnum = new HasEnum();
            hasEnum.setMyEnum(BAR);
            long id = orm.insert(hasEnum);

            HasEnum fromDb = orm.find(HasEnum.class, id);
            assertEquals(BAR, fromDb.getMyEnum());
        }

}
