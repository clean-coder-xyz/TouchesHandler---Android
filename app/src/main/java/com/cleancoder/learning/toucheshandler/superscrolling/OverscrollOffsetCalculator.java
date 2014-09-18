package com.cleancoder.learning.toucheshandler.superscrolling;

/**
 * Created by lsemenov on 17.09.2014.
 */
public class OverscrollOffsetCalculator {
    private int base;
    private int totalOffset;

    public OverscrollOffsetCalculator() {
        base = 0;
        totalOffset = 0;
    }

    public void onStart() {
        base = 0;
        totalOffset = 0;
    }

    public void onStop() {
        base = 0;
        totalOffset = 0;
    }

    public int calculateOverscrollOffset(int offsetOutOfEdge) {
        totalOffset += offsetOutOfEdge;
        return calculate();
    }

    private int calculate() {
        return totalOffset();
    }

    private int totalOffset() {
        return base + totalOffset;
    }

    public void setBase(int base) {
        this.base = base;
    }

}
