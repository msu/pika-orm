package bigsky.pika.utils;

import org.junit.jupiter.api.Test;

import static bigsky.pika.PikaORM.TextTools.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextToolTest {

    @Test
    public void testPluralization() {
        assertEquals("foos", pluralize("foo"));
        assertEquals("stays", pluralize("stay"));
        assertEquals("stories", pluralize("story"));
        assertEquals("wives", pluralize("wife"));
        assertEquals("lives", pluralize("life"));
        assertEquals("wolves", pluralize("wolf"));
        assertEquals("churches", pluralize("church"));
        assertEquals("classes", pluralize("class"));
    }

    @Test
    public void testCamelCase() {
        assertEquals("firstName", camelCase("first_name"));
        assertEquals("userId", camelCase("user_id"));
        assertEquals("httpStatus", camelCase("http_status"));
        assertEquals("a", camelCase("a"));
        assertEquals("camelCase", camelCase("camel_case"));
    }

    @Test
    public void testSnakeCase() {
        assertEquals("first_name", snakeCase("firstName"));
        assertEquals("user_id", snakeCase("userId"));
        assertEquals("last_http_status", snakeCase("lastHTTPStatus"));
        assertEquals("http_status", snakeCase("HTTPStatus"));
        assertEquals("a", snakeCase("a"));
        assertEquals("snake_case", snakeCase("snakeCase"));
        assertEquals("sample_model", snakeCase("SampleModel"));
    }

    @Test
    public void testCapitalize() {
        assertEquals("Hello", capitalize("hello"));
        assertEquals("A", capitalize("a"));
        assertEquals("", capitalize(""));
        assertEquals(null, capitalize(null));
        assertEquals("Already", capitalize("Already"));
    }

    @Test
    public void testDecapitalize() {
        assertEquals("hello", decapitalize("Hello"));
        assertEquals("a", decapitalize("A"));
        assertEquals("", decapitalize(""));
        assertEquals(null, decapitalize(null));
        assertEquals("already", decapitalize("already"));
    }

    @Test
    public void testHumanize() {
        assertEquals("First Name", humanize("firstName"));
        assertEquals("User Id", humanize("user_id"));
        assertEquals("Http Status", humanize("httpStatus"));
        assertEquals("Employee Name", humanize("employee_name"));
    }

    @Test
    public void testIndent() {
        assertEquals("  hello", indent(2, "hello"));
        assertEquals("    hello\n    world", indent(4, "hello\nworld"));
        assertEquals("  ", indent(2, ""));
    }

}
