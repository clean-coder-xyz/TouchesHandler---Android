package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.View;
import android.view.animation.Transformation;

import com.cleancoder.learning.toucheshandler.OrientationHelper;

/**
 * Created by lsemenov on 22.09.2014.
 */
public class ExpandCollapseAnimation extends BaseExpandCollapseAnimation {

    public static interface OnTransformationAppliedListener {
        void onTransformationApplied();
    }

    private static final OnTransformationAppliedListener DUMMY_ON_CHANGE_LISTENER = new OnTransformationAppliedListener() {
        @Override
        public void onTransformationApplied() {
            // skip
        }
    };

    private final Helper animationHelper;
    private final OrientationHelper orientationHelper;
    private final View view;
    private boolean completed;
    private boolean stopped;
    private boolean used;
    private OnTransformationAppliedListener onTransformationAppliedListener;


    public ExpandCollapseAnimation(View view, OrientationHelper orientationHelper, Helper helper) {
        this.view = view;
        this.orientationHelper = orientationHelper;
        this.stopped = false;
        this.used = true;
        this.completed = false;
        this.animationHelper = helper;
        this.onTransformationAppliedListener = DUMMY_ON_CHANGE_LISTENER;
        setDuration(animationHelper.getDuration());
    }

    public void setOnTransformationAppliedListener(OnTransformationAppliedListener onTransformationAppliedListener) {
        this.onTransformationAppliedListener = onTransformationAppliedListener;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (!isUsed() || isStopped()) {
            return;
        }
        if (interpolatedTime == 1){
            animationHelper.setViewOnAnimationStopped(view);
            completed = true;
            onTransformationAppliedListener.onTransformationApplied();
        } else{
            orientationHelper.setSize(view, calculateSize(interpolatedTime));
            onTransformationAppliedListener.onTransformationApplied();
        }
    }

    private int calculateSize(float interpolatedTime) {
        int size = (int) animationHelper.getOffsetSize(interpolatedTime);
        return size;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean willChangeBounds() {
        return true;
    }

    public void stop() {
        stopped = true;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isCompleted() {
        return completed;
    }
}
