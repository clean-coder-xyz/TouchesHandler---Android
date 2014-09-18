package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.MotionEvent;
import android.view.View;

import com.cleancoder.learning.toucheshandler.HorizontalParallaxScrollView;
import com.cleancoder.learning.toucheshandler.TaggedLogger;
import com.cleancoder.learning.toucheshandler.ViewUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by lsemenov on 17.09.2014.
 */
public class TouchesHandler implements TouchHandler {

    private final Object LOCK_TOUCH_HANDLING = new Object();
    private final PuzzleGalleryHelper puzzleGalleryHelper;
    private final Set<Integer> pointers;
    private boolean isScrollingOutOfStartEdgeStarted;
    private boolean isScrollingOutOfEndEdgeStarted;
    private Map<Integer,Integer> lastTouchedCoordinates;
    private Offset startOffset;
    private Offset endOffset;
    private OverscrollOffsetCalculator startOverscrollCalculator;
    private OverscrollOffsetCalculator endOverscrollCalculator;
    private HorizontalParallaxScrollView scrollView;

    public TouchesHandler(PuzzleGalleryHelper puzzleGalleryHelper,
                          Offset startOffset,
                          Offset endOffset,
                          HorizontalParallaxScrollView scrollView) {
        this.puzzleGalleryHelper = puzzleGalleryHelper;
        this.pointers = new HashSet<Integer>();
        this.isScrollingOutOfStartEdgeStarted = false;
        this.isScrollingOutOfEndEdgeStarted = false;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.startOverscrollCalculator = new OverscrollOffsetCalculator();
        this.endOverscrollCalculator = new OverscrollOffsetCalculator();
        this.scrollView = scrollView;
        this.lastTouchedCoordinates = new HashMap<Integer, Integer>();
        // TODO
    }

    @Override
    public void handleTouch(MotionEvent event) {
        synchronized (LOCK_TOUCH_HANDLING) {
            SimpleMotionEvent simpleMotionEvent = SimpleMotionEvent.from(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    onDown(simpleMotionEvent);
                    break;
                case MotionEvent.ACTION_MOVE:
                    onMove(simpleMotionEvent);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    onUp(simpleMotionEvent);
                    break;
            }
            int coordinate = (int) puzzleGalleryHelper.getCoordinate(simpleMotionEvent);
            lastTouchedCoordinates.put(simpleMotionEvent.getId(), coordinate);
            TaggedLogger.LEONID.debug("Last touched coordinate:  " + coordinate);
        }
    }

    private void onDown(SimpleMotionEvent simpleMotionEvent) {
        ViewUtils.stopCollapsing(startOffset.getView());
        ViewUtils.stopCollapsing(endOffset.getView());
        pointers.add(simpleMotionEvent.getId());
    }

    public void onUp(SimpleMotionEvent simpleMotionEvent) {
        int id = simpleMotionEvent.getId();
        pointers.remove(id);
        lastTouchedCoordinates.remove(id);
        if (pointers.isEmpty()) {
            hideOffsetsIfDisplayed();
        }
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
        Integer lastTouchedCoordinate = lastTouchedCoordinates.get(simpleMotionEvent.getId());
        if (lastTouchedCoordinate == null) {
            return;
        }
        int coordinate = (int) puzzleGalleryHelper.getCoordinate(simpleMotionEvent);
        int delta = coordinate - lastTouchedCoordinate;
        simpleMotionEvent.setDelta(delta);
        if (ViewUtils.isEndEdgeOfScrollViewHasBeenReached(scrollView, delta)) {
            onOutOfEndEdge(simpleMotionEvent);
        } else if (ViewUtils.isStartEdgeOfScrollViewHasBeenReached(scrollView)) {
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
                int base = puzzleGalleryHelper.getViewSize(offsetView);
                startOverscrollCalculator.setBase(base);
            }
            int overscrollOffset = startOverscrollCalculator.calculateOverscrollOffset(simpleMotionEvent.getDelta());
            if (overscrollOffset <= 0) {
                hideStartOffsetIfDisplayed();
                return;
            }
            startOffset.set(overscrollOffset);
            scrollView.scrollToStart();
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
                int base = puzzleGalleryHelper.getViewSize(offsetView);
                int realBase = -base;
                endOverscrollCalculator.setBase(realBase);
                ViewUtils.setViewCollapsingUnused(offsetView);
            }
            int overscrollOffset = endOverscrollCalculator.calculateOverscrollOffset(simpleMotionEvent.getDelta());
            int realOverscrollOffset = -overscrollOffset;
            if (realOverscrollOffset <= 0) {
                hideEndOffsetIfDisplayed();
                return;
            }
            endOffset.set(realOverscrollOffset);
            scrollView.scrollToEnd();
        }
    }

    private void onStartScrollingOutOfEndEdge() {
        isScrollingOutOfEndEdgeStarted = true;
        endOverscrollCalculator.onStart();
    }

}
