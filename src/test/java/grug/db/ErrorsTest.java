package grug.db;

import grug.db.models.HasBadColumnMapping;
import grug.db.models.HasCustomizedMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ErrorsTest extends TestBase {

    @Test
    public void testMissingParamsErrorsCorrectly() {
        var orm = initTestDb(HasCustomizedMetadata.DDL, HasBadColumnMapping.DDL);
        try {
            orm.select(":foo", Map.of());
            fail("Should have failed because no :foo was supplied");
        } catch (Exception e){
            assertInstanceOf(IllegalStateException.class, e);
            assertEquals("No value found for variable :foo in {}", e.getMessage());
        }
    }

    @Test
    public void testBadColumnGivesGoodErrorOnSelect() {
        var orm = initTestDb(HasCustomizedMetadata.DDL, HasBadColumnMapping.DDL);
        String stdErr = captureStdErr(() -> {
            try {
                orm.exec("INSERT INTO has_bad_column_mapping (foo) VALUES ('bar')");
                orm.find(HasBadColumnMapping.class).all();
                fail("Should have failed because no field bar is on the table");
            } catch (Exception e) {
                // exception should be swallowed to allow stderr to complete
            }
        });
        assertStringContains(stdErr, "Could not map field bar on HasBadColumnMapping, available columns:[id,foo], error:no such column: 'bar'");
    }

}
