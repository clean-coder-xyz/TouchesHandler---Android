package com.cleancoder.learning.toucheshandler;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import com.cleancoder.learning.toucheshandler.scrolling.SimpleMotionEvent;

/**
 * Created by lsemenov on 08.09.2014.
 */
public interface OrientationHelper {
    float getCoordinate(SimpleMotionEvent event);
    int getBackgroundResource();
    int getViewSize(View view);
    int getOrientation();
    LinearLayout.LayoutParams createLayoutParamsForMainPart();
    ViewGroup.LayoutParams createOffsetLayoutParams();
    Animation newDeceleratingCollapseAnimation(View view);
    void setContentToScrollView(ViewGroup scrollView, View contentView);
    void setSize(View view, int size);
    int getScrollCoordinate(View view);
    int getStartCoordinate(View view);
    int getEndCoordinate(View view);
}
