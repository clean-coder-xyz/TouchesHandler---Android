package com.cleancoder.learning.toucheshandler.scrolling;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Created by lsemenov on 08.09.2014.
 */
public interface PuzzleGalleryHelper {
    float getCoordinate(SimpleMotionEvent event);
    int getBackgroundResource();
    int getViewSize(View view);
    int getOrientation();
    LinearLayout.LayoutParams createLayoutParamsForMainPart();
    ViewGroup.LayoutParams createLayoutParams();
    void collapse(View view);
    void setContentToScrollView(ViewGroup scrollView, View contentView);
    void setSize(View view, int size);
}
