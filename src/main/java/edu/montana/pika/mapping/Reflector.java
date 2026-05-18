package edu.montana.pika.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public interface Reflector {
    Object make(Constructor constructor, Object[] args);
    Object get(Field field, Object from);
    void set(Field field, Object object, Object val);

    default <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name) {
        return Enum.valueOf(enumType, name);
    }
}
