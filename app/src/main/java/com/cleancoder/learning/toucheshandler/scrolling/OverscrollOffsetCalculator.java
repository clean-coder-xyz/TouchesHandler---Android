package com.cleancoder.learning.toucheshandler.scrolling;

import com.cleancoder.learning.toucheshandler.TaggedLogger;

/**
 * Created by lsemenov on 17.09.2014.
 */
public class OverscrollOffsetCalculator {
    private static final int LOG_BASE = 64;

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

    public int addOffsetAndCalculateOverscrollOffset(int offsetOutOfEdge) {
        totalOffset += offsetOutOfEdge;
        TaggedLogger.LEONID.debug("offsetOutOfEdge:  " + offsetOutOfEdge);
        TaggedLogger.LEONID.debug("totalOffset():    " + totalOffset());
        return calculate();
    }

    private int calculate() {
        int realOffset = totalOffset();
        if (realOffset == 0) {
            return 0;
        }
        int sign = 1;
        if (realOffset < 0) {
            sign = -1;
            realOffset = -realOffset;
        }
        double log = Math.log(realOffset + LOG_BASE) / Math.log(LOG_BASE);
        double coefficient = 1.0 / log;
        double overscrolling = Math.ceil(realOffset * coefficient * coefficient);
        int result = (int) overscrolling;
        TaggedLogger.LEONID.debug("OverscrollOffsetCalculator.calculate():  " + result);
        return sign * result;
    }

    private int totalOffset() {
        return base + totalOffset;
    }

    public void setBase(int base) {
        this.base = base;
    }

}
