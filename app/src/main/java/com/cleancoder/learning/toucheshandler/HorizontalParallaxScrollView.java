package com.cleancoder.learning.toucheshandler;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import com.cleancoder.learning.toucheshandler.scrolling.ParallaxScrollController;
import com.cleancoder.learning.toucheshandler.scrolling.ScrollableToBounds;
import com.cleancoder.learning.toucheshandler.scrolling.SimpleMotionEvent;

/**
 * Created by lsemenov on 18.09.2014.
 */
public class HorizontalParallaxScrollView extends HorizontalScrollView implements ScrollableToBounds {

    private final Object LOCK_SCROLL_SPEED = new Object();

    private float scrollSpeed;
    private ViewGroup mLayout;
    private ParallaxScrollController parallaxScrollController;

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
        synchronized (LOCK_SCROLL_SPEED) {
            scrollSpeed = 0.0f;
        }
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        parallaxScrollController =
                new ParallaxScrollController(context, OrientationHelpers.HORIZONTAL, this, this);
        mLayout = parallaxScrollController.prepareView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        parallaxScrollController.handleTouch(SimpleMotionEvent.from(event));
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

    @Override
    public void scrollToStart() {
        scroll(0, getScrollY());
    }

    @Override
    public void scrollToEnd() {
        scroll(getChildAt(getChildCount() - 1).getWidth(), getScrollY());
    }

    @Override
    public boolean isScrolledToStart() {
        return getScrollX() == 0;
    }

    @Override
    public boolean isScrolledToEnd() {
        View childView = getChildAt(getChildCount() - 1);
        return (childView.getRight()) == (getWidth() + getScrollX());
    }


}
