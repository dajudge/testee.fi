package com.dajudge.testee.utils;

/**
 * Containing something mutable.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class MutableContainer<T> {
    private T object;

    public MutableContainer() {
    }

    public MutableContainer(final T object) {
        this.object = object;
    }

    public T getObject() {
        return object;
    }

    public void setObject(final T object) {
        this.object = object;
    }
}
