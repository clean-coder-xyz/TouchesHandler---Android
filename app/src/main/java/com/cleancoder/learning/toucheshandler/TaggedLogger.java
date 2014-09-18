package com.cleancoder.learning.toucheshandler;

import android.util.Log;

/**
 * Created by lsemenov on 18.09.2014.
 */
public class TaggedLogger {

    public static final TaggedLogger LEONID = new TaggedLogger("Leonid");

    private final String tag;

    public static TaggedLogger forClass(Class<?> someClass) {
        return new TaggedLogger(someClass.getName());
    }

    public static TaggedLogger forInstance(Object instance) {
        return forClass(instance.getClass());
    }

    public TaggedLogger(String tag) {
        this.tag = tag;
    }

    public void debug(String message) {
        Log.d(tag, message);
    }

    public void info(String message) {
        Log.i(tag, message);
    }

    public void exception(String message) {
        Log.e(tag, message);
    }

    public void exception(String message, Throwable exception) {
        Log.e(tag, message, exception);
    }

}
