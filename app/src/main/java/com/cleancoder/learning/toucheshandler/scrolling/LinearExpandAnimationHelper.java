package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.View;
import android.view.ViewGroup;

import com.cleancoder.learning.toucheshandler.OrientationHelper;

/**
 * Created by lsemenov on 23.09.2014.
 */
public class LinearExpandAnimationHelper implements BaseExpandCollapseAnimation.Helper {
    private final int duration;
    private final int targetSize;
    private final OrientationHelper orientationHelper;

    public LinearExpandAnimationHelper(float density, int targetSize, OrientationHelper orientationHelper) {
        this.targetSize = targetSize;
        this.orientationHelper = orientationHelper;
        this.duration = (int) (targetSize / density);
    }

    @Override
    public float getOffsetSize(float interpolatedTime) {
        return (int) (targetSize * interpolatedTime);
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public void setViewOnAnimationStopped(View view) {
        orientationHelper.setSize(view, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
