package bigsky.pika.cache;

public record LoadKey(Object objectWithFk, Class classToLoad, String foreignKeyColumn) {}
