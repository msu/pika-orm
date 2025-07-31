package bigsky.pika;

import org.junit.jupiter.api.Test;

import static bigsky.pika.PikaORM.TextTools.pluralize;
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

}
