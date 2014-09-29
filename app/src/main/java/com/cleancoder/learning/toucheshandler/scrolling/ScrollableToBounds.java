package com.cleancoder.learning.toucheshandler.scrolling;

/**
 * Created by lsemenov on 18.09.2014.
 */
public interface ScrollableToBounds {
    void scrollToStart();
    void scrollToEnd();
    boolean isScrolledToStart();
    boolean isScrolledToEnd();
}
