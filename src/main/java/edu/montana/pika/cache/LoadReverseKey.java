package edu.montana.pika.cache;

public record LoadReverseKey(Object objectWithPk, Class classToLoad, String foreignKeyColumn) {
    @Override
    public String toString() {
        return "loadReverse(" + classToLoad.getSimpleName() + " via " + foreignKeyColumn + ")";
    }
}
