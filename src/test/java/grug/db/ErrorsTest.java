package grug.db;

import grug.db.models.HasCustomizedMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ErrorsTest extends TestBase {

    GrugORM orm = null;

    @BeforeEach
    public void setUp() throws IOException {
        orm = initDBFileAndORM();
        orm.exec(HasCustomizedMetadata.DDL);
    }

    @Test
    public void testMissingParamsErrorsCorrectly() {
        try {
            orm.selectRaw(":foo", Map.of());
            fail("Should have failed because no :foo was supplied");
        } catch (Exception e){
            assertInstanceOf(IllegalStateException.class, e);
            assertEquals("No value found for variable :foo in {}", e.getMessage());
        }
    }
}
