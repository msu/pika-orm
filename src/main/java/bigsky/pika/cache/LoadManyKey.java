package bigsky.pika.cache;

public record LoadManyKey(Object one, Class classOfMany, String manyFk) {
    @Override
    public String toString() {
        return "loadMany(" + classOfMany.getSimpleName() + " via " + manyFk + ")";
    }
}
