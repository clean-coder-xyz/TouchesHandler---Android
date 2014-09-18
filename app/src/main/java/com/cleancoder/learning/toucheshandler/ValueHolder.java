package com.cleancoder.learning.toucheshandler;

/**
 * Created by lsemenov on 18.09.2014.
 */
public class ValueHolder<T> {

    private T value;

    public ValueHolder() {
        this.value = null;
    }

    public ValueHolder(T value) {
        this.value = value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return ValueHolder.class.getName() + "<" + value.getClass().getName() + "> (" + value + ")";
    }
}
