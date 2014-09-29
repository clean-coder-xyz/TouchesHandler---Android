package com.cleancoder.learning.toucheshandler;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by lsemenov on 12.09.2014.
 */
public class ExtendedArrayList<E> extends ArrayList<E> {

    public static <T> ExtendedArrayList<T> withSize(int size) {
        ExtendedArrayList<T> arrayList = new ExtendedArrayList<T>(size);
        arrayList.resizeIfNeed(size);
        return arrayList;
    }

    public ExtendedArrayList(int capacity) {
        super(capacity);
    }

    public ExtendedArrayList() {
        super();
    }

    public ExtendedArrayList(Collection<? extends E> collection) {
        super(collection);
    }


    public void resizeIfIndexOutOfBounds(int index) {
        if (index >= size()) {
            resize(index + 1);
        }
    }

    public void resizeIfNeed(int newSize) {
        if (newSize > size()) {
            resize(newSize);
        }
    }

    public void resize(int newSize) {
        ensureCapacity(newSize);
        int numberOfElementsToAdd = newSize - size();
        for (int i = 0; i < numberOfElementsToAdd; ++i) {
            add(null);
        }
    }

}
