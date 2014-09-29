package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.View;

import com.cleancoder.learning.toucheshandler.OrientationHelper;
import com.cleancoder.learning.toucheshandler.TaggedLogger;

/**
 * Created by lsemenov on 23.09.2014.
 */
public class OnStopScrollingObserver implements Runnable {

    public static interface Listener {
        void onScrollStopped(Speedometer speedometer);
    }

    public static Builder builder() {
        // Use this static method instead of Builder's constructor
        // in order not to write the keyword  << new >>  every time
        return new Builder();
    }

    public static class Builder {
        private Listener listener;
        private Long delay;
        private OrientationHelper orientationHelper;
        private View view;

        private Builder() {
            // empty constructor
        }

        public Builder delay(Long delay) {
            this.delay = delay;
            return this;
        }

        public Builder listener(Listener listener) {
            this.listener = listener;
            return this;
        }

        public Builder helper(OrientationHelper orientationHelper) {
            this.orientationHelper = orientationHelper;
            return this;
        }

        public Builder view(View view) {
            this.view = view;
            return this;
        }

        public OnStopScrollingObserver build() {
            checkPredicates();
            OnStopScrollingObserver observer = new OnStopScrollingObserver(view, orientationHelper);
            observer.setListener(listener);
            observer.setDelay(delay);
            return observer;
        }

        private void checkPredicates() {
            checkRequiredArgumentsAreNotNull();
        }

        private void checkRequiredArgumentsAreNotNull() {
            for (Object arg : requiredArguments()) {
                if (arg == null) {
                    throw new IllegalStateException("Some of required arguments are not defined");
                }
            }
        }

        private Object[] requiredArguments() {
            return new Object[] { view, orientationHelper};
        }

    }


    public static class Speedometer {
        private long totalDelta;
        private long totalTime;
        private long lastTimeStamp;

        private Speedometer() {
            totalDelta = 0;
            totalTime = 0;
            lastTimeStamp = getTimeStamp();
        }

        private void addDelta(int delta) {
            totalDelta += delta;
            long timeStamp = getTimeStamp();
            totalTime += (timeStamp - lastTimeStamp);
            lastTimeStamp = timeStamp;
        }

        private long getTimeStamp() {
            return System.nanoTime();
        }

        public double getSpeed() {
            long absoluteTotalDelta = Math.abs(totalDelta);
            return ((double) absoluteTotalDelta) / totalTime;
        }
    }


    private static final long DEFAULT_DELAY = 100;

    private static final Listener DUMMY_LISTENER = new Listener() {
        @Override
        public void onScrollStopped(Speedometer speedometer) {
            // dummy implementation
        }
    };


    private final OrientationHelper orientationHelper;
    private final View view;
    private int initialPosition;
    private Listener listener;
    private long delay;
    private boolean isFirstIteration;
    private boolean isStarted;
    private Speedometer speedometer;

    private OnStopScrollingObserver(View view, OrientationHelper orientationHelper) {
        this.view = view;
        this.orientationHelper = orientationHelper;
        this.listener = DUMMY_LISTENER;
        this.delay = DEFAULT_DELAY;
        this.isFirstIteration = true;
        this.isStarted = false;
    }

    public void setDelay(Long newDelay) {
        this.delay = (newDelay != null) ? newDelay : DEFAULT_DELAY;
    }

    public void setListener(Listener newListener) {
        this.listener = (newListener != null) ? newListener : DUMMY_LISTENER;
    }

    @Override
    public void run() {
        if (isFirstIteration) {
            initOnFirstIteration();
            isFirstIteration = false;
        }
        int newPosition = getScrollCoordinate();
        int delta = initialPosition - newPosition;
        TaggedLogger.LEONID.debug(OnStopScrollingObserver.class.getName() + ".run():  delta=" + delta);
        if (delta == 0) {
            listener.onScrollStopped(speedometer);
            isFirstIteration = true;
        } else {
            speedometer.addDelta(delta);
            initialPosition = getScrollCoordinate();
            view.postDelayed(this, delay);
        }
    }

    private void initOnFirstIteration() {
        speedometer = new Speedometer();
    }

    private int getScrollCoordinate() {
        return orientationHelper.getScrollCoordinate(view);
    }

    public void startDelayed() {
        if (isStarted) {
            throw new IllegalStateException("This observer is already started");
        }
        isStarted = true;
        initialPosition = getScrollCoordinate();
        view.postDelayed(this, delay);
    }

}
