package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.View;
import android.view.animation.Animation;

/**
 * Created by lsemenov on 23.09.2014.
 */
public class BaseExpandCollapseAnimation extends Animation {

    public static interface Helper {
        float getOffsetSize(float interpolatedTime);
        int getDuration();
        void setViewOnAnimationStopped(View view);
    }

}
