package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.View;
import android.view.animation.Animation;

import com.cleancoder.learning.toucheshandler.OrientationHelper;
import com.cleancoder.learning.toucheshandler.ViewUtils;

/**
 * Created by lsemenov on 23.09.2014.
 */
public class BouncingEffect {
    private final int expandDuration;
    private final int delta;
    private final OrientationHelper helper;

    public BouncingEffect(int expandDuration, int delta, OrientationHelper helper) {
        this.expandDuration = expandDuration;
        this.delta = delta;
        this.helper = helper;
    }

    public void start(final Offset offset, final ScrollableToBounds scrollView) {
        final View offsetView = offset.getView();
        helper.setSize(offsetView, 0);
        ViewUtils.setVisible(offsetView, true);
        final Animation collapseAnimation = helper.newDeceleratingCollapseAnimation(offsetView);
        collapseAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                helper.setSize(offsetView, 0);
            }

            @Override
            public void onAnimationStart(Animation animation) {
                // skip
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // skip
            }
        });
        ExpandCollapseAnimation expandAnimation = ViewUtils.newLinearExpandAnimation(offsetView, helper, delta);
        if (offset.isEndOffset()) {
            expandAnimation.setOnTransformationAppliedListener(new ExpandCollapseAnimation.OnTransformationAppliedListener() {
                @Override
                public void onTransformationApplied() {
                    scrollView.scrollToEnd();
                }
            });
        }
        expandAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                offsetView.startAnimation(collapseAnimation);
            }

            @Override
            public void onAnimationStart(Animation animation) {
                // skip
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // skip
            }
        });
        offsetView.startAnimation(expandAnimation);
    }

}
