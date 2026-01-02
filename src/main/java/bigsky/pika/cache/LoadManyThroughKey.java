package bigsky.pika.cache;

public record LoadManyThroughKey(Object one, Class joinClass, Class classOfMany) {
    @Override
    public String toString() {
        return "loadManyThrough(" + classOfMany.getSimpleName() + " via " + joinClass.getSimpleName() + ")";
    }
}
