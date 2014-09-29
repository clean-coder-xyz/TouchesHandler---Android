package com.cleancoder.learning.toucheshandler.griddynamicview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.cleancoder.learning.toucheshandler.OrientationHelper;
import com.cleancoder.learning.toucheshandler.OrientationHelpers;
import com.cleancoder.learning.toucheshandler.TaggedLogger;
import com.cleancoder.learning.toucheshandler.scrolling.ParallaxScrollController;
import com.cleancoder.learning.toucheshandler.scrolling.SimpleMotionEvent;

/**
 * Created by lsemenov on 29.09.2014.
 */
public class ParallaxGridDynamicView extends FrameLayout {

    private final GridDynamicView gridDynamicView;
    private ParallaxScrollController parallaxScrollController;

    public ParallaxGridDynamicView(Context context) {
        super(context);
        gridDynamicView = new GridDynamicView(context);
        init();
    }

    public ParallaxGridDynamicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gridDynamicView = new GridDynamicView(context, attrs);
        init();
    }

    public ParallaxGridDynamicView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        gridDynamicView = new GridDynamicView(context, attrs, defStyle);
        init();
    }

    private void init() {
        parallaxScrollController = new ParallaxScrollController(getContext(), getOrientationHelper(), this, gridDynamicView);
        parallaxScrollController.setContentLayout(gridDynamicView);
        parallaxScrollController.prepareView();
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            setLayoutParams(layoutParams);
        } else {
            layoutParams.width = LayoutParams.WRAP_CONTENT;
            layoutParams.height = LayoutParams.WRAP_CONTENT;
        }
        gridDynamicView.addOnTouchListener(new GridDynamicAbsListView.OnTouchListener() {
            final Object LOCK_COUNT = new Object();
            int count = 0;

            @Override
            public void onTouchEvent(MotionEvent event) {
                synchronized (LOCK_COUNT) {
                    if (count % 2 == 0) {
                        parallaxScrollController.handleTouch(SimpleMotionEvent.from(event));
                    }
                    ++count;
                }
            }

            @Override
            public void onInterceptTouchEvent(MotionEvent event) {

            }
        });
    }

    private OrientationHelper getOrientationHelper() {
        return OrientationHelpers.HORIZONTAL;
    }

    public GridDynamicView getGridDynamicView() {
        return gridDynamicView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        TaggedLogger.LEONID.debug("onTouch()");
        gridDynamicView.onTouchEvent(event);
        return false;
    }

}
