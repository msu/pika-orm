package bigsky.pika.errors;

import bigsky.pika.TestBase;
import bigsky.pika.errors.model.HasBadColumnMapping;
import bigsky.pika.models.SampleModel;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ErrorsTest extends TestBase {

    @Test
    public void testMissingParamsErrorsCorrectly() {
        var orm = initTestDb(HasBadColumnMapping.DDL);
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
        var orm = initTestDb(HasBadColumnMapping.DDL);
        String stdErr = captureStdErr(() -> {
            try {
                orm.exec("INSERT INTO has_bad_column_mappings (foo) VALUES ('bar')");
                orm.find(HasBadColumnMapping.class).all();
                fail("Should have failed because no field bar is on the table");
            } catch (Exception e) {
                // exception should be swallowed to allow stderr to complete
            }
        });
        assertStringContains(stdErr, "Could not map field bar on HasBadColumnMapping, available columns:[id,foo], error:no such column: 'bar'");
    }

    @Test
    public void testMissingRequiredParameterInWhere() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            orm.find(SampleModel.class).where("str_val = :name", Map.of());
            fail("Should throw exception for missing parameter");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No value found for variable :name"));
        }
    }

    @Test
    public void testMultipleMissingParameters() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            orm.find(SampleModel.class).where("str_val = :name AND int_val = :value", Map.of());
            fail("Should throw exception for missing parameters");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No value found for variable"));
        }
    }

    @Test
    public void testWrongParameterName() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            orm.find(SampleModel.class).where("str_val = :name", Map.of("wrong_name", "value"));
            fail("Should throw exception for wrong parameter name");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No value found for variable :name"));
        }
    }

    @Test
    public void testEmptyQueryString() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            orm.select("", Map.of());
            fail("Should throw exception for empty query");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testInvalidSQLSyntax() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            orm.select("SELECT * FORM sample_models", Map.of());
            fail("Should throw exception for invalid SQL");
        } catch (Exception e) {
            assertTrue(e.getMessage().toLowerCase().contains("syntax") ||
                      e.getMessage().toLowerCase().contains("error") ||
                      e.getMessage().toLowerCase().contains("near"));
        }
    }

    @Test
    public void testInsertWithoutRequiredField() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            SampleModel model = new SampleModel();
            model.setStrVal("test");
            // Missing required fields: int_val, bool_val, date_val
            orm.insert(model);
            fail("Should throw exception for missing required fields");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testUpdateNonExistentRecord() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model = new SampleModel("test", 10, true, new Date());
        model.setId(999L);

        // Should not throw, but also shouldn't update anything
        orm.update(model);

        SampleModel result = orm.find(SampleModel.class).byId(999L);
        assertNull(result);
    }

    @Test
    public void testDeleteNonExistentRecord() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model = new SampleModel("test", 10, true, new Date());
        model.setId(999L);

        // Should not throw exception when deleting non-existent record
        orm.delete(model);
    }

    @Test
    public void testFindByNonExistentId() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel result = orm.find(SampleModel.class).byId(999L);
        assertNull(result);
    }

    @Test
    public void testSelectFromNonExistentTable() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            orm.select("SELECT * FROM non_existent_table", Map.of());
            fail("Should throw exception for non-existent table");
        } catch (Exception e) {
            assertTrue(e.getMessage().toLowerCase().contains("no such table") ||
                      e.getMessage().toLowerCase().contains("doesn't exist") ||
                      e.getMessage().toLowerCase().contains("error"));
        }
    }

    @Test
    public void testWhereWithNullParameter() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model = new SampleModel("test", 10, true, new Date());
        orm.insert(model);

        // Should handle null parameter gracefully
        HashMap<String, Object> args = new HashMap<>();
        args.put("val", null);
        var results = orm.find(SampleModel.class)
            .where("str_val = :val", args)
            .toList();

        assertNotNull(results);
    }

    @Test
    public void testMalformedParameterSyntax() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            // Missing colon before parameter name
            orm.find(SampleModel.class).where("str_val = name", Map.of("name", "test"));
            fail("Should handle malformed parameter syntax");
        } catch (Exception e) {
            assertInstanceOf(SQLException.class, e);
        }
    }

    @Test
    public void testExtraParametersIgnored() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model = new SampleModel("test", 10, true, new Date());
        orm.insert(model);

        // Extra parameters should be ignored, not cause errors
        var results = orm.find(SampleModel.class)
            .where("str_val = :name", Map.of("name", "test", "extra", "ignored"))
            .toList();

        assertNotNull(results);
    }

    @Test
    public void testInsertAllWithEmptyArray() {
        var orm = initTestDb(SampleModel.DDL);

        // Should handle empty array gracefully
        orm.insertAll();

        assertEquals(0, orm.find(SampleModel.class).all().toList().size());
    }

    @Test
    public void testSelectWithUnmatchedQuotes() {
        var orm = initTestDb(SampleModel.DDL);

        try {
            orm.select("SELECT * FROM sample_models WHERE str_val = 'unclosed", Map.of());
            fail("Should throw exception for unmatched quotes");
        } catch (Exception e) {
            assertInstanceOf(SQLException.class, e);
        }
    }

    @Test
    public void testDoubleParameterPrefix() {
        var orm = initTestDb(SampleModel.DDL);

        SampleModel model = new SampleModel("test", 10, true, new Date());
        orm.insert(model);

        try {
            orm.find(SampleModel.class)
                .where("str_val = ::name", Map.of("name", "test"))
                .toList();
        } catch (Exception e) {
            assertInstanceOf(SQLException.class, e);
        }
    }

}
