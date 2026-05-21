package edu.montana.pika.utils;

import edu.montana.pika.PikaORM;
import edu.montana.pika.TestBase;
import edu.montana.pika.mapping.Mapping;
import edu.montana.pika.query.ResultMap;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ResultMapTest extends TestBase {

    public ResultMap setUp() {
        var orm = new PikaORM(() -> null);
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("age", 30);
        data.put("salary", 50000L);
        data.put("height", 5.9f);
        data.put("weight", 175.5);
        data.put("balance", new BigDecimal("1000.50"));
        data.put("active", true);
        data.put("birthdate", new Date());
        data.put("count", (short) 5);
        var resultMap = new ResultMap(orm, data);
        return resultMap;
    }

    @Test
    public void testGetString() {
        var resultMap = setUp();
        assertEquals("John", resultMap.getString("name"));
        assertNull(resultMap.getString("nonexistent"));
    }

    @Test
    public void testGetInteger() {
        var resultMap = setUp();
        assertEquals(30, resultMap.getInteger("age"));
    }

    @Test
    public void testGetLong() {
         var resultMap = setUp();
        assertEquals(50000L, resultMap.getLong("salary"));
    }

    @Test
    public void testGetFloat() {
         var resultMap = setUp();
        assertEquals(5.9f, resultMap.getFloat("height"));
    }

    @Test
    public void testGetDouble() {
         var resultMap = setUp();
        assertEquals(175.5, resultMap.getDouble("weight"));
    }

    @Test
    public void testGetBigDecimal() {
         var resultMap = setUp();
        assertEquals(new BigDecimal("1000.50"), resultMap.getBigDecimal("balance"));
    }

    @Test
    public void testGetBoolean() {
         var resultMap = setUp();
        assertTrue(resultMap.getBoolean("active"));
    }

    @Test
    public void testGetDate() {
         var resultMap = setUp();
        assertNotNull(resultMap.getDate("birthdate"));
        assertInstanceOf(Date.class, resultMap.getDate("birthdate"));
    }

    @Test
    public void testGetShort() {
         var resultMap = setUp();
        assertEquals((short) 5, resultMap.getShort("count"));
    }

    @Test
    public void testGetWithType() {
         var resultMap = setUp();
        assertEquals("John", resultMap.get("name", String.class));
        assertEquals(30, resultMap.get("age", Integer.class));
    }

    @Test
    public void testAsStringCoercion() {
         var resultMap = setUp();
        assertEquals("30", resultMap.asString("age"));
        assertEquals("John", resultMap.asString("name"));
    }

    @Test
    public void testAsIntegerCoercion() {
        Map<String, Object> data = new HashMap<>();
        data.put("stringNum", "42");
        ResultMap map = new ResultMap(new PikaORM(()-> null), data);

        assertEquals(42, map.asInteger("stringNum"));
    }

    @Test
    public void testToCaseInsensitiveMap() {
         var resultMap = setUp();
        ResultMap caseInsensitive = resultMap.toCaseInsensitiveMap();

        assertEquals("John", caseInsensitive.getString("name"));
        assertEquals("John", caseInsensitive.getString("NAME"));
        assertEquals("John", caseInsensitive.getString("Name"));
    }

    @Test
    public void testMapInterfaceMethods() {
         var resultMap = setUp();
        assertEquals(9, resultMap.size());
        assertFalse(resultMap.isEmpty());
        assertTrue(resultMap.containsKey("name"));
        assertTrue(resultMap.containsValue("John"));
        assertEquals("John", resultMap.get("name"));
    }

    @Test
    public void testKeySet() {
         var resultMap = setUp();
        assertTrue(resultMap.keySet().contains("name"));
        assertTrue(resultMap.keySet().contains("age"));
        assertEquals(9, resultMap.keySet().size());
    }

    @Test
    public void testValues() {
         var resultMap = setUp();
        assertTrue(resultMap.values().contains("John"));
        assertTrue(resultMap.values().contains(30));
        assertEquals(9, resultMap.values().size());
    }

    @Test
    public void testEntrySet() {
         var resultMap = setUp();
        assertEquals(9, resultMap.entrySet().size());
        assertTrue(resultMap.entrySet().stream().anyMatch(e -> e.getKey().equals("name") && e.getValue().equals("John")));
    }

    @Test
    public void testToString() {
         var resultMap = setUp();
        String str = resultMap.toString();
        assertNotNull(str);
        assertTrue(str.contains("name"));
    }

    @Test
    public void testUnmodifiable() {
         var resultMap = setUp();
        assertThrows(UnsupportedOperationException.class, () -> resultMap.put("newKey", "newValue"));
        assertThrows(UnsupportedOperationException.class, () -> resultMap.remove("name"));
        assertThrows(UnsupportedOperationException.class, () -> resultMap.clear());
        assertThrows(UnsupportedOperationException.class, () -> resultMap.putAll(new HashMap<>()));
    }

    @Test
    public void testEmptyResultMap() {
        ResultMap empty = new ResultMap(null, new HashMap<>());

        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertNull(empty.getString("any"));
    }

    @Test
    public void testMappingForResultMapInitializesFieldMaps() {
        var orm = new PikaORM(() -> null);
        Mapping mapping = new Mapping();
        mapping.setOrm(orm);
        mapping.setClass(ResultMap.class);

        assertNotNull(mapping.fieldNameToMapping);
        assertTrue(mapping.fieldNameToMapping.isEmpty());
        assertNull(mapping.getFieldMappingForFieldName("anything"));
    }
}