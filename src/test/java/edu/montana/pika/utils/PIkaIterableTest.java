package edu.montana.pika.utils;

import edu.montana.pika.query.PikaList;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PIkaIterableTest {

    // map() tests
    @Test
    void testMapTransformsElements() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        PikaList<String> result = list.map(String::valueOf);

        assertEquals(3, result.size());
        assertEquals("1", result.get(0));
        assertEquals("2", result.get(1));
        assertEquals("3", result.get(2));
    }

    @Test
    void testMapOnEmptyList() {
        PikaList<Integer> list = new PikaList<>();
        PikaList<String> result = list.map(String::valueOf);
        assertEquals(0, result.size());
    }

    // toSet() tests
    @Test
    void testToSetCreatesSet() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);
        list.add(1);

        Set<Integer> result = list.toSet();

        assertEquals(2, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
    }

    @Test
    void testToSetMaintainsOrder() {
        PikaList<Integer> list = new PikaList<>();
        list.add(3);
        list.add(1);
        list.add(2);

        Set<Integer> result = list.toSet();

        assertInstanceOf(LinkedHashSet.class, result);
        Iterator<Integer> it = result.iterator();
        assertEquals(3, it.next());
        assertEquals(1, it.next());
        assertEquals(2, it.next());
    }

    // toList() tests
    @Test
    void testToListCreatesNewList() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);

        PikaList<Integer> result = list.toList();

        assertEquals(2, result.size());
        assertNotSame(list, result);
    }

    // toMap() tests
    @Test
    void testToMapGroupsByKey() {
        PikaList<String> list = new PikaList<>();
        list.add("apple");
        list.add("banana");
        list.add("apricot");

        Map<Character, List<String>> result = list.toMap(s -> s.charAt(0));

        assertEquals(2, result.size());
        assertEquals(2, result.get('a').size());
        assertEquals(1, result.get('b').size());
    }

    @Test
    void testToMapOnEmptyList() {
        PikaList<String> list = new PikaList<>();
        Map<Integer, List<String>> result = list.toMap(String::length);
        assertEquals(0, result.size());
    }

    // toOrderedMap() tests
    @Test
    void testToOrderedMapCreatesTreeMap() {
        PikaList<String> list = new PikaList<>();
        list.add("c");
        list.add("a");
        list.add("b");

        TreeMap<String, List<String>> result = list.toOrderedMap(s -> s);

        assertInstanceOf(TreeMap.class, result);
        Iterator<String> keys = result.keySet().iterator();
        assertEquals("a", keys.next());
        assertEquals("b", keys.next());
        assertEquals("c", keys.next());
    }

    @Test
    void testToOrderedMapWithComparator() {
        PikaList<String> list = new PikaList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        TreeMap<String, List<String>> result = list.toOrderedMap(s -> s, Comparator.reverseOrder());

        Iterator<String> keys = result.keySet().iterator();
        assertEquals("c", keys.next());
        assertEquals("b", keys.next());
        assertEquals("a", keys.next());
    }

    // toDistinctMap() tests
    @Test
    void testToDistinctMapCreatesUniqueKeys() {
        PikaList<String> list = new PikaList<>();
        list.add("apple");
        list.add("banana");
        list.add("apricot");

        Map<Character, String> result = list.toDistinctMap(s -> s.charAt(0));

        assertEquals(2, result.size());
        assertTrue(result.containsKey('a'));
        assertTrue(result.containsKey('b'));
    }

    @Test
    void testToDistinctMapLastValueWins() {
        PikaList<String> list = new PikaList<>();
        list.add("apple");
        list.add("apricot");

        Map<Character, String> result = list.toDistinctMap(s -> s.charAt(0));

        assertEquals("apricot", result.get('a'));
    }

    // toOrderedDistinctMap() tests
    @Test
    void testToOrderedDistinctMapCreatesTreeMap() {
        PikaList<String> list = new PikaList<>();
        list.add("c");
        list.add("a");
        list.add("b");

        TreeMap<String, String> result = list.toOrderedDistinctMap(s -> s);

        assertInstanceOf(TreeMap.class, result);
        Iterator<String> keys = result.keySet().iterator();
        assertEquals("a", keys.next());
        assertEquals("b", keys.next());
        assertEquals("c", keys.next());
    }

    @Test
    void testToOrderedDistinctMapWithComparator() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        TreeMap<Integer, Integer> result = list.toOrderedDistinctMap(i -> i, Comparator.reverseOrder());

        Iterator<Integer> keys = result.keySet().iterator();
        assertEquals(3, keys.next());
        assertEquals(2, keys.next());
        assertEquals(1, keys.next());
    }

    // filter() tests
    @Test
    void testFilterKeepsMatchingElements() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        PikaList<Integer> result = list.filter(i -> i % 2 == 0);

        assertEquals(2, result.size());
        assertTrue(result.contains(2));
        assertTrue(result.contains(4));
    }

    @Test
    void testFilterOnEmptyList() {
        PikaList<Integer> list = new PikaList<>();
        PikaList<Integer> result = list.filter(i -> i > 0);
        assertEquals(0, result.size());
    }

    // toString() tests
    @Test
    void testToStringWithSeparator() {
        PikaList<String> list = new PikaList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        String result = list.toString(", ");

        assertEquals("a, b, c", result);
    }

    @Test
    void testToStringOnEmptyList() {
        PikaList<String> list = new PikaList<>();
        String result = list.toString(", ");
        assertEquals("", result);
    }

    @Test
    void testToStringWithSingleElement() {
        PikaList<String> list = new PikaList<>();
        list.add("only");

        String result = list.toString(", ");

        assertEquals("only", result);
    }

    // first() tests
    @Test
    void testFirstReturnsFirstElement() {
        PikaList<String> list = new PikaList<>();
        list.add("first");
        list.add("second");

        String result = list.first();

        assertEquals("first", result);
    }

    @Test
    void testFirstOnEmptyListReturnsNull() {
        PikaList<String> list = new PikaList<>();
        String result = list.first();
        assertNull(result);
    }

    // firstWhere() tests
    @Test
    void testFirstWhereReturnsFirstMatch() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        Integer result = list.firstWhere(i -> i > 2);

        assertEquals(3, result);
    }

    @Test
    void testFirstWhereReturnsNullWhenNoMatch() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);

        Integer result = list.firstWhere(i -> i > 10);

        assertNull(result);
    }

    @Test
    void testFirstWhereOnEmptyList() {
        PikaList<Integer> list = new PikaList<>();
        Integer result = list.firstWhere(i -> i > 0);
        assertNull(result);
    }

    // hasMatch() tests
    @Test
    void testHasMatchReturnsTrueWhenMatch() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        boolean result = list.hasMatch(i -> i == 2);

        assertTrue(result);
    }

    @Test
    void testHasMatchReturnsFalseWhenNoMatch() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);

        boolean result = list.hasMatch(i -> i > 10);

        assertFalse(result);
    }

    @Test
    void testHasMatchOnEmptyList() {
        PikaList<Integer> list = new PikaList<>();
        boolean result = list.hasMatch(i -> i > 0);
        assertFalse(result);
    }

    // hasNoMatch() tests
    @Test
    void testHasNoMatchReturnsTrueWhenNoMatch() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);

        boolean result = list.hasNoMatch(i -> i > 10);

        assertTrue(result);
    }

    @Test
    void testHasNoMatchReturnsFalseWhenMatch() {
        PikaList<Integer> list = new PikaList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        boolean result = list.hasNoMatch(i -> i == 2);

        assertFalse(result);
    }

    @Test
    void testHasNoMatchOnEmptyList() {
        PikaList<Integer> list = new PikaList<>();
        boolean result = list.hasNoMatch(i -> i > 0);
        assertTrue(result);
    }
}
