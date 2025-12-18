package bigsky.pika.utils;

import bigsky.pika.query.PikaList;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PikaListTest {

    @Test
    public void testLast() {
        PikaList<String> list = new PikaList<>();
        assertNull(list.last());

        list.add("first");
        assertEquals("first", list.last());

        list.add("second");
        assertEquals("second", list.last());

        list.add("third");
        assertEquals("third", list.last());
    }

    @Test
    public void testLastWhere() {
        PikaList<Integer> list = new PikaList<>(Arrays.asList(1, 2, 3, 4, 5, 6));

        assertEquals(6, list.lastWhere(n -> n % 2 == 0));
        assertEquals(5, list.lastWhere(n -> n % 2 == 1));
        assertNull(list.lastWhere(n -> n > 10));
    }

    @Test
    public void testCopy() {
        PikaList<String> original = new PikaList<>(Arrays.asList("a", "b", "c"));
        PikaList<String> copy = original.copy();

        assertEquals(original, copy);
        assertNotSame(original, copy);

        copy.add("d");
        assertEquals(3, original.size());
        assertEquals(4, copy.size());
    }

    @Test
    public void testMap() {
        PikaList<Integer> numbers = new PikaList<>(Arrays.asList(1, 2, 3, 4));
        PikaList<String> strings = numbers.map(n -> "num" + n);

        assertEquals(Arrays.asList("num1", "num2", "num3", "num4"), strings);
    }

    @Test
    public void testFilter() {
        PikaList<Integer> numbers = new PikaList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        PikaList<Integer> evens = numbers.filter(n -> n % 2 == 0);

        assertEquals(Arrays.asList(2, 4, 6), evens);
    }

    @Test
    public void testToSet() {
        PikaList<String> list = new PikaList<>(Arrays.asList("a", "b", "a", "c"));
        Set<String> set = list.toSet();

        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testToList() {
        PikaList<String> list = new PikaList<>(Arrays.asList("a", "b", "c"));
        PikaList<String> newList = list.toList();

        assertEquals(list, newList);
        assertNotSame(list, newList);
    }

    @Test
    public void testToMap() {
        PikaList<String> words = new PikaList<>(Arrays.asList("apple", "apricot", "banana", "blueberry"));
        Map<Character, List<String>> grouped = words.toMap(w -> w.charAt(0));

        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList("apple", "apricot"), grouped.get('a'));
        assertEquals(Arrays.asList("banana", "blueberry"), grouped.get('b'));
    }

    @Test
    public void testToOrderedMap() {
        PikaList<String> words = new PikaList<>(Arrays.asList("banana", "apple", "blueberry", "apricot"));
        TreeMap<Character, List<String>> grouped = words.toOrderedMap(w -> w.charAt(0));

        List<Character> keys = new ArrayList<>(grouped.keySet());
        assertEquals(Arrays.asList('a', 'b'), keys);
    }

    @Test
    public void testToOrderedMapWithComparator() {
        PikaList<String> words = new PikaList<>(Arrays.asList("banana", "apple", "blueberry", "apricot"));
        TreeMap<Character, List<String>> grouped = words.toOrderedMap(w -> w.charAt(0), Comparator.reverseOrder());

        List<Character> keys = new ArrayList<>(grouped.keySet());
        assertEquals(Arrays.asList('b', 'a'), keys);
    }

    @Test
    public void testToDistinctMap() {
        PikaList<String> words = new PikaList<>(Arrays.asList("apple", "apricot", "banana"));
        Map<Character, String> distinctMap = words.toDistinctMap(w -> w.charAt(0));

        assertEquals(2, distinctMap.size());
        assertEquals("apricot", distinctMap.get('a')); // last one wins
        assertEquals("banana", distinctMap.get('b'));
    }

    @Test
    public void testToOrderedDistinctMap() {
        PikaList<String> words = new PikaList<>(Arrays.asList("banana", "apple"));
        TreeMap<Character, String> distinctMap = words.toOrderedDistinctMap(w -> w.charAt(0));

        List<Character> keys = new ArrayList<>(distinctMap.keySet());
        assertEquals(Arrays.asList('a', 'b'), keys);
    }

    @Test
    public void testToOrderedDistinctMapWithComparator() {
        PikaList<String> words = new PikaList<>(Arrays.asList("banana", "apple"));
        TreeMap<Character, String> distinctMap = words.toOrderedDistinctMap(w -> w.charAt(0), Comparator.reverseOrder());

        List<Character> keys = new ArrayList<>(distinctMap.keySet());
        assertEquals(Arrays.asList('b', 'a'), keys);
    }

    @Test
    public void testToString() {
        PikaList<String> list = new PikaList<>(Arrays.asList("a", "b", "c"));
        assertEquals("a, b, c", list.toString(", "));
        assertEquals("a|b|c", list.toString("|"));

        PikaList<String> empty = new PikaList<>();
        assertEquals("", empty.toString(", "));
    }

    @Test
    public void testConstructorFromCollection() {
        List<String> original = Arrays.asList("x", "y", "z");
        PikaList<String> pikaList = new PikaList<>(original);

        assertEquals(3, pikaList.size());
        assertEquals("x", pikaList.get(0));
        assertEquals("y", pikaList.get(1));
        assertEquals("z", pikaList.get(2));
    }
}