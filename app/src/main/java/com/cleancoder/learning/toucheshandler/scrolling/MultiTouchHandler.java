package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.cleancoder.learning.toucheshandler.OrientationHelper;
import com.cleancoder.learning.toucheshandler.TaggedLogger;
import com.cleancoder.learning.toucheshandler.ViewUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by lsemenov on 17.09.2014.
 */
public class MultiTouchHandler implements OnStopScrollingObserver.Listener {

    private final Object LOCK_METHOD_IS_THERE_NO_TOUCHES = new Object();
    private final Object LOCK_TOUCH_HANDLING = new Object();
    private final OrientationHelper orientationHelper;
    private final Set<Integer> pointers;
    private boolean isScrollingOutOfStartEdgeStarted;
    private boolean isScrollingOutOfEndEdgeStarted;
    private Map<Integer,Integer> lastTouchedCoordinates;
    private Offset startOffset;
    private Offset endOffset;
    private OverscrollOffsetCalculator startOverscrollCalculator;
    private OverscrollOffsetCalculator endOverscrollCalculator;
    private ViewGroup scrollView;
    private ScrollableToBounds scrollViewScrollableToBounds;

    public MultiTouchHandler(OrientationHelper orientationHelper,
                             Offset startOffset,
                             Offset endOffset,
                             ViewGroup scrollView) {
        this.orientationHelper = orientationHelper;
        this.pointers = new HashSet<Integer>();
        this.isScrollingOutOfStartEdgeStarted = false;
        this.isScrollingOutOfEndEdgeStarted = false;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.startOverscrollCalculator = new OverscrollOffsetCalculator();
        this.endOverscrollCalculator = new OverscrollOffsetCalculator();
        this.scrollView = scrollView;
        this.scrollViewScrollableToBounds = (ScrollableToBounds) scrollView;
        this.lastTouchedCoordinates = new HashMap<Integer, Integer>();
        // TODO
    }

    public void handleTouch(SimpleMotionEvent simpleMotionEvent) {
        synchronized (LOCK_TOUCH_HANDLING) {
            switch (simpleMotionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    TaggedLogger.LEONID.debug("DOWN");
                    onDown(simpleMotionEvent);
                    break;
                case MotionEvent.ACTION_MOVE:
                    TaggedLogger.LEONID.debug("MOVE");
                    onMove(simpleMotionEvent);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    TaggedLogger.LEONID.debug("UP");
                    onUp(simpleMotionEvent);
                    break;
            }
            int coordinate = (int) orientationHelper.getCoordinate(simpleMotionEvent);
            lastTouchedCoordinates.put(simpleMotionEvent.getId(), coordinate);
            TaggedLogger.LEONID.debug("Last touched coordinate:  " + coordinate);
        }
    }

    private void onDown(SimpleMotionEvent simpleMotionEvent) {
        TaggedLogger.LEONID.debug("onDown()");
        int id = simpleMotionEvent.getId();
        if (pointers.contains(id)) {
            return;
        }
        ViewUtils.stopCollapsing(startOffset.getView());
        ViewUtils.stopCollapsing(endOffset.getView());
        pointers.add(simpleMotionEvent.getId());
    }

    public void onUp(SimpleMotionEvent simpleMotionEvent) {
        TaggedLogger.LEONID.debug("onUp()");
        int id = simpleMotionEvent.getId();
        if (!pointers.contains(id)) {
            return;
        }
        pointers.remove(id);
        lastTouchedCoordinates.remove(id);
        if (isThereNoTouches()) {
            if (!isScrollingOutOfEdgeStarted()) {
                startOnStopScrollingObserverTask();
            }
            hideOffsetsIfDisplayed();
        }
    }

    private boolean isScrollingOutOfEdgeStarted() {
        return isScrollingOutOfStartEdgeStarted || isScrollingOutOfEndEdgeStarted;
    }

    private boolean isThereNoTouches() {
        synchronized (LOCK_METHOD_IS_THERE_NO_TOUCHES) {
            return pointers.isEmpty();
        }
    }

    private void startOnStopScrollingObserverTask() {
        OnStopScrollingObserver observer = OnStopScrollingObserver.builder()
                .helper(orientationHelper)
                .view(scrollView)
                .listener(this)
                .delay(100L)
                .build();
        observer.startDelayed();
    }

    @Override
    public void onScrollStopped(OnStopScrollingObserver.Speedometer speedometer) {
        if (isThereNoTouches()) {
            if (scrollViewScrollableToBounds.isScrolledToStart()) {
                showBouncingOffEdge(startOffset, speedometer.getSpeed());
            } else if (scrollViewScrollableToBounds.isScrolledToEnd()) {
                showBouncingOffEdge(endOffset, speedometer.getSpeed());
            }
        }
    }

    private void showBouncingOffEdge(Offset offset, double speed) {
        int delta = deltaFromSpeed(speed);
        int expandDuration = 5 * delta;
        TaggedLogger.LEONID.debug("Show bouncing off the edge:  delta=" + delta);
        BouncingEffect bouncingEffect = new BouncingEffect(expandDuration, delta, orientationHelper);
        bouncingEffect.start(offset, scrollViewScrollableToBounds);
    }

    private static int deltaFromSpeed(double speed) {
        double normalSpeed = speed * 1000000;
        return (int) (normalSpeed * Math.log(normalSpeed));
    }

    private void hideOffsetsIfDisplayed() {
        hideStartOffsetIfDisplayed();
        hideEndOffsetIfDisplayed();
    }

    private void hideStartOffsetIfDisplayed() {
        isScrollingOutOfStartEdgeStarted = false;
        startOverscrollCalculator.onStop();
        startOffset.hideIfDisplayed();
    }

    private void hideEndOffsetIfDisplayed() {
        isScrollingOutOfEndEdgeStarted = false;
        endOverscrollCalculator.onStop();
        endOffset.hideIfDisplayed();
    }

    public void onMove(SimpleMotionEvent simpleMotionEvent) {
        TaggedLogger.LEONID.debug("onMove()");
        Integer lastTouchedCoordinate = lastTouchedCoordinates.get(simpleMotionEvent.getId());
        if (lastTouchedCoordinate == null) {
            return;
        }
        int coordinate = (int) orientationHelper.getCoordinate(simpleMotionEvent);
        int delta = coordinate - lastTouchedCoordinate;
        simpleMotionEvent.setDelta(delta);
        if (scrollViewScrollableToBounds.isScrolledToEnd()) {
            onOutOfEndEdge(simpleMotionEvent);
        } else if (scrollViewScrollableToBounds.isScrolledToStart()) {
            onOutOfStartEdge(simpleMotionEvent);
        } else {
            hideOffsetsIfDisplayed();
        }
    }

    private void onOutOfStartEdge(SimpleMotionEvent simpleMotionEvent) {
        TaggedLogger.LEONID.debug("onOutOfStartEdge()");
        hideEndOffsetIfDisplayed();
        if (!isScrollingOutOfStartEdgeStarted) {
            onStartScrollingOutOfStartEdge();
        } else {
            View offsetView = startOffset.getView();
            if (ViewUtils.isViewCollapsingUsingAndNotCompleted(offsetView)) {
                ViewUtils.setViewCollapsingUnused(offsetView);
                int base = orientationHelper.getViewSize(offsetView);
                startOverscrollCalculator.setBase(base);
            }
            int overscrollOffset =
                    startOverscrollCalculator.addOffsetAndCalculateOverscrollOffset(simpleMotionEvent.getDelta());
            if (overscrollOffset <= 0) {
                hideStartOffsetIfDisplayed();
                return;
            }
            startOffset.set(overscrollOffset);
            scrollViewScrollableToBounds.scrollToStart();
        }
    }

    private void onStartScrollingOutOfStartEdge() {
        isScrollingOutOfStartEdgeStarted = true;
        startOverscrollCalculator.onStart();
    }

    private void onOutOfEndEdge(SimpleMotionEvent simpleMotionEvent) {
        TaggedLogger.LEONID.debug("onOutOfEndEdge()");
        hideStartOffsetIfDisplayed();
        if (!isScrollingOutOfEndEdgeStarted) {
            onStartScrollingOutOfEndEdge();
        } else {
            View offsetView = endOffset.getView();
            if (ViewUtils.isViewCollapsingUsingAndNotCompleted(offsetView)) {
                int base = orientationHelper.getViewSize(offsetView);
                int realBase = -base;
                endOverscrollCalculator.setBase(realBase);
                ViewUtils.setViewCollapsingUnused(offsetView);
            }
            int overscrollOffset = endOverscrollCalculator.addOffsetAndCalculateOverscrollOffset(simpleMotionEvent.getDelta());
            int realOverscrollOffset = -overscrollOffset;
            if (realOverscrollOffset <= 0) {
                hideEndOffsetIfDisplayed();
                return;
            }
            endOffset.set(realOverscrollOffset);
            scrollViewScrollableToBounds.scrollToEnd();
        }
    }

    private void onStartScrollingOutOfEndEdge() {
        isScrollingOutOfEndEdgeStarted = true;
        endOverscrollCalculator.onStart();
    }

}
