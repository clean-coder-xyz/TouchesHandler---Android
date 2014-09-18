package com.cleancoder.learning.toucheshandler;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.cleancoder.learning.toucheshandler.superscrolling.MagneticScrollController;
import com.cleancoder.learning.toucheshandler.superscrolling.PuzzleGalleryHelpers;

/**
 * Created by lsemenov on 18.09.2014.
 */
public class HorizontalParallaxScrollView extends HorizontalScrollView {

    private LinearLayout mLayout;
    private MagneticScrollController magneticScrollController;

    public HorizontalParallaxScrollView(Context context) {
        super(context);
        init(context);
    }

    public HorizontalParallaxScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HorizontalParallaxScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        magneticScrollController =
                new MagneticScrollController(context, PuzzleGalleryHelpers.HORIZONTAL, this);
        mLayout = magneticScrollController.prepareView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        magneticScrollController.handleTouch(event);
        return super.onTouchEvent(event);
    }

    public void addItemView(View itemView) {
        mLayout.addView(itemView);
    }

    private final Object LOCK_SCROLLING = new Object();
    private boolean needToScroll = false;
    private int scrollToX;
    private int scrollToY;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        synchronized (LOCK_SCROLLING) {
            scrollIfNeed();
        }
    }

    private void scrollIfNeed() {
        if (needToScroll) {
            scrollTo(scrollToX, scrollToY);
            needToScroll = false;
        }
    }

    public void scroll(int scrollToX, int scrollToY) {
        synchronized (LOCK_SCROLLING) {
            this.scrollToX = scrollToX;
            this.scrollToY = scrollToY;
            this.needToScroll = true;
            requestLayout();
        }
    }

    public void scrollToStart() {
        scroll(0, getScrollY());
    }

    public void scrollToEnd() {
        scroll(getChildAt(getChildCount() - 1).getWidth(), getScrollY());
    }

}
