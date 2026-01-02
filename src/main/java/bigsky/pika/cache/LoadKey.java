package bigsky.pika.cache;

public record LoadKey(Object objectWithFk, Class classToLoad, String foreignKeyColumn) {
    @Override
    public String toString() {
        return "load(" + classToLoad.getSimpleName() + " via " + foreignKeyColumn + ")";
    }
}
