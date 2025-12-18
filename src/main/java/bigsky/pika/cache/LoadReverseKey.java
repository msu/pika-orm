package bigsky.pika.cache;

public record LoadReverseKey(Object objectWithPk, Class classToLoad, String foreignKeyColumn) {
}
