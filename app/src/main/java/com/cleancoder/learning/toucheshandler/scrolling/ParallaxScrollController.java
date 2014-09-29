package com.cleancoder.learning.toucheshandler.scrolling;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.cleancoder.learning.toucheshandler.OrientationHelper;

/**
 * Created by lsemenov on 08.09.2014.
 */
public class ParallaxScrollController {

    private final Context context;
    private final OrientationHelper helper;
    private final MultiTouchHandler touchHandler;
    private final ViewGroup container;
    private final ViewGroup scrollView;
    private final Offset startOffset;
    private final Offset endOffset;
    private ViewGroup contentLayout;

    public ParallaxScrollController(Context context, OrientationHelper helper,
                                    ViewGroup container, ViewGroup scrollView) {
        this.context = context;
        this.helper = helper;
        this.container = container;
        this.scrollView = scrollView;
        this.startOffset = new Offset(context, helper);
        this.startOffset.setIsStartOffset(true);
        this.endOffset = new Offset(context, helper);
        this.endOffset.setIsEndOffset(true);
        this.touchHandler = new MultiTouchHandler(helper, startOffset, endOffset, scrollView);
    }

    public void setContentLayout(ViewGroup view) {
        if (contentLayout != null) {
            throw new IllegalStateException("Content layout was already setted");
        }
        contentLayout = view;
    }

    public ViewGroup prepareView() {
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setBackgroundResource(helper.getBackgroundResource());
        rootLayout.setOrientation(helper.getOrientation());
        ViewGroup contentLayout = getContentLayout();
        rootLayout.addView(startOffset.getView());
        rootLayout.addView(wrapContentLayout(contentLayout));
        rootLayout.addView(endOffset.getView());
        helper.setContentToScrollView(container, rootLayout);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        return contentLayout;
    }

    private View wrapContentLayout(ViewGroup contentLayout) {
        RelativeLayout frameLayout = new RelativeLayout(context);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frameLayout.addView(contentLayout);
        return frameLayout;
    }

    private ViewGroup getContentLayout() {
        if (contentLayout == null) {
            contentLayout = prepareContentLayout();
        }
        return contentLayout;
    }

    private LinearLayout prepareContentLayout() {
        LinearLayout contentLayout = new LinearLayout(context);
        setContentLayoutParams(contentLayout);
        contentLayout.setOrientation(helper.getOrientation());
        return contentLayout;
    }

    private void setContentLayoutParams(ViewGroup contentLayout) {
        LinearLayout.LayoutParams layoutParams = helper.createLayoutParamsForMainPart();
        layoutParams.weight = 1;
        contentLayout.setLayoutParams(layoutParams);
    }

    public void handleTouch(SimpleMotionEvent event) {
        touchHandler.handleTouch(event);
    }

}
