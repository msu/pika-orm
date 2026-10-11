package edu.montana.pika.utils;

import edu.montana.pika.query.PikaList;
import edu.montana.pika.util.PikaMap;
import edu.montana.pika.util.PikaSet;
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

        assertInstanceOf(PikaSet.class, result);
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

        PikaMap<Character, PikaList<String>> result = list.toMap(s -> s.charAt(0));

        assertEquals(2, result.size());
        assertEquals(2, result.get('a').size());
        assertEquals(1, result.get('b').size());
    }

    @Test
    void testToMapOnEmptyList() {
        PikaList<String> list = new PikaList<>();
        PikaMap<Integer, PikaList<String>> result = list.toMap(String::length);
        assertEquals(0, result.size());
    }

    // toOrderedMap() tests
    @Test
    void testToOrderedMapCreatesTreeMap() {
        PikaList<String> list = new PikaList<>();
        list.add("c");
        list.add("a");
        list.add("b");

        PikaMap<String, PikaList<String>> result = list.toOrderedMap(s -> s);

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

        PikaMap<String, PikaList<String>> result = list.toOrderedMap(s -> s, Comparator.reverseOrder());

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

        PikaMap<Character, String> result = list.toDistinctMap(s -> s.charAt(0));

        assertEquals(2, result.size());
        assertTrue(result.containsKey('a'));
        assertTrue(result.containsKey('b'));
    }

    @Test
    void testToDistinctMapLastValueWins() {
        PikaList<String> list = new PikaList<>();
        list.add("apple");
        list.add("apricot");

        PikaMap<Character, String> result = list.toDistinctMap(s -> s.charAt(0));

        assertEquals("apricot", result.get('a'));
    }

    // toOrderedDistinctMap() tests
    @Test
    void testToOrderedDistinctMapCreatesTreeMap() {
        PikaList<String> list = new PikaList<>();
        list.add("c");
        list.add("a");
        list.add("b");

        PikaMap<String, String> result = list.toOrderedDistinctMap(s -> s);

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

        PikaMap<Integer, Integer> result = list.toOrderedDistinctMap(i -> i, Comparator.reverseOrder());

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

    // onMiss() tests
    @Test
    void testOnMissValue() {
        PikaList<String> list = new PikaList<>(List.of("apple", "apricot", "banana"));
        PikaMap<Character, Integer> lengths = list.toDistinctMap(s -> s.charAt(0), String::length).onMiss(0);

        assertEquals(7, lengths.get('a'));
        assertEquals(0, lengths.get('z'));
        assertFalse(lengths.containsKey('z'));
        assertEquals(2, lengths.size());
    }

    @Test
    void testOnMissSupplier() {
        PikaList<String> list = new PikaList<>(List.of("apple", "banana"));
        PikaMap<Character, PikaList<String>> grouped = list.toMap(s -> s.charAt(0)).onMiss(PikaList::new);

        assertEquals(1, grouped.get('a').size());
        assertTrue(grouped.get('z').isEmpty());
        assertNotSame(grouped.get('z'), grouped.get('z'));
        assertEquals(2, grouped.size());
    }

    @Test
    void testOnMissIsAView() {
        PikaMap<String, Integer> map = new PikaMap<>();
        PikaMap<String, Integer> view = map.onMiss(0);

        map.put("a", 1);
        assertEquals(1, view.get("a"));
        view.put("b", 2);
        assertEquals(2, map.get("b"));
        assertNull(map.get("c"));
    }

    @Test
    void testOnMissGetOrDefault() {
        PikaMap<String, Integer> map = new PikaMap<String, Integer>().onMiss(0);
        assertEquals(5, map.getOrDefault("a", 5));
    }

    @Test
    void testMapClear() {
        PikaMap<String, Integer> map = new PikaMap<>();
        map.put("a", 1);
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    void testMapViewsAreLive() {
        PikaMap<String, Integer> map = new PikaMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        map.keySet().remove("a");
        map.entrySet().removeIf(e -> e.getValue() == 2);
        assertEquals(List.of("c"), new ArrayList<>(map.keySet()));
    }

    @Test
    void testSetCopyIsSeparate() {
        PikaSet<String> set = new PikaList<>(List.of("b", "a")).toSet();
        PikaSet<String> copy = set.copy();
        copy.add("c");

        assertEquals(2, set.size());
        assertEquals(List.of("b", "a", "c"), new ArrayList<>(copy));
    }

    // Notch methods
    @Test
    void testFlatMapTakeSkipUnique() {
        PikaList<List<Integer>> nested = new PikaList<>(List.of(List.of(1, 2), List.of(2, 3)));
        PikaList<Integer> flat = nested.flatMap(l -> l);

        assertEquals(List.of(1, 2, 2, 3), flat);
        assertEquals(List.of(1, 2), flat.take(2));
        assertEquals(List.of(2, 3), flat.skip(2));
        assertEquals(List.of(1, 2, 3), flat.unique());
        assertEquals(List.of(3, 2, 2, 1), flat.reverse());
    }

    @Test
    void testSortedByFoldAll() {
        PikaList<String> list = new PikaList<>(List.of("ccc", "a", "bb"));

        assertEquals(List.of("a", "bb", "ccc"), list.sortedBy(String::length));
        assertEquals(6, list.fold(0, (n, s) -> n + s.length()));
        assertTrue(list.all(s -> !s.isEmpty()));
        assertEquals(2, list.indexOf("bb"));
        assertEquals("[ccc, a, bb]", list.toString("[", ", ", "]"));
    }

    @Test
    void testOnMissComputeIfAbsent() {
        PikaMap<String, Integer> map = new PikaMap<String, Integer>().onMiss(0);
        assertEquals(Integer.valueOf(5), map.computeIfAbsent("a", k -> 5));
        assertEquals(Integer.valueOf(5), map.get("a"));
        assertEquals(1, map.size());
    }

    @Test
    void testOnMissPutIfAbsent() {
        PikaMap<String, Integer> map = new PikaMap<String, Integer>().onMiss(0);
        assertNull(map.putIfAbsent("a", 7));
        assertEquals(Integer.valueOf(7), map.get("a"));
    }

    @Test
    void testOnMissReplace() {
        PikaMap<String, Integer> map = new PikaMap<String, Integer>().onMiss(0);
        assertNull(map.replace("a", 9));
        assertFalse(map.replace("a", 0, 9));
        assertFalse(map.containsKey("a"));
        assertEquals(0, map.size());
    }

    @Test
    void testOnMissComputeIfPresent() {
        PikaMap<String, Integer> map = new PikaMap<String, Integer>().onMiss(0);
        assertNull(map.computeIfPresent("a", (k, v) -> v + 1));
        assertFalse(map.containsKey("a"));
    }

    @Test
    void testOnMissCompute() {
        PikaMap<String, Integer> map = new PikaMap<String, Integer>().onMiss(0);
        map.compute("a", (k, v) -> v == null ? 10 : v + 1);
        assertEquals(Integer.valueOf(10), map.get("a"));
    }

    @Test
    void testOnMissMerge() {
        PikaMap<String, Integer> map = new PikaMap<String, Integer>().onMiss(0);
        map.merge("a", 3, (a, b) -> a * 100 + b);
        assertEquals(Integer.valueOf(3), map.get("a"));
        map.merge("a", 4, (a, b) -> a * 100 + b);
        assertEquals(Integer.valueOf(304), map.get("a"));
    }
}
