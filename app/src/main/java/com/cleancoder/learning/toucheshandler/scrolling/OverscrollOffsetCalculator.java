package com.cleancoder.learning.toucheshandler.scrolling;

import com.cleancoder.learning.toucheshandler.TaggedLogger;

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
        TaggedLogger.LEONID.debug("offsetOutOfEdge:  " + offsetOutOfEdge);
        TaggedLogger.LEONID.debug("totalOffset():    " + totalOffset());
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
