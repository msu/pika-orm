package edu.montana.pika.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static edu.montana.pika.PikaORM.safely;

public class StandardReflector implements Reflector {
    @Override
    public Object make(Constructor constructor, Object[] args) {
        return safely(() -> constructor.newInstance(args));
    }

    @Override
    public Object get(Field field, Object from) {
        return safely(() -> field.get(from));
    }

    @Override
    public void set(Field field, Object object, Object val) {
        safely(() -> field.set(object, val));
    }
}
