package edu.montana.pika.bean;

public interface PikaRecordLifecycle {

    default boolean validate() {
        return true;
    }

    default boolean beforeInsert() {
        return true;
    }

    default boolean beforeUpdate() {
        return true;
    }

    default boolean beforeDelete() {
        return true;
    }

    default void afterInsert() {
    }

    default void afterSelect() {
    }

    default void afterUpdate() {
    }

    default void afterDelete() {
    }
}
