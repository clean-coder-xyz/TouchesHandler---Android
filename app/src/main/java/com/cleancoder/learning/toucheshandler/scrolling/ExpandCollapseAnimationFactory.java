package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.View;

import com.cleancoder.learning.toucheshandler.OrientationHelper;

/**
 * Created by lsemenov on 23.09.2014.
 */
public class ExpandCollapseAnimationFactory {

    public static ExpandCollapseAnimation newDeceleratingCollapseAnimation(View view,
                                                   int initialSize, OrientationHelper orientationHelper) {
        return new ExpandCollapseAnimation(view, orientationHelper, prepareAnimationHelper(initialSize));
    }

    private static BaseExpandCollapseAnimation.Helper prepareAnimationHelper(int initialSize) {
        return DeceleratedCollapseAnimationHelper.builder()
                .totalDistance(initialSize)
                .acceleratedDistance(420)
                .initialVelocity(1.6f)
                .accelerationPercentagePerSecond(-85)
                .build();
    }

    public static ExpandCollapseAnimation newLinearExpandAnimation(View view,
                                                   int targetSize, OrientationHelper orientationHelper) {
        float density = view.getContext().getResources().getDisplayMetrics().density;
        return new ExpandCollapseAnimation(view, orientationHelper,
                            new LinearExpandAnimationHelper(density, targetSize, orientationHelper));
    }

}
