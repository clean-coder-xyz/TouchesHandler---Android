package com.cleancoder.learning.toucheshandler.superscrolling;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.cleancoder.learning.toucheshandler.HorizontalParallaxScrollView;

/**
 * Created by lsemenov on 08.09.2014.
 */
public class MagneticScrollController {

    private final Context context;
    private final PuzzleGalleryHelper helper;
    private final TouchesHandler touchHandler;
    private final ViewGroup scrollView;
    private final Offset startOffset;
    private final Offset endOffset;

    public MagneticScrollController(Context context, PuzzleGalleryHelper helper, HorizontalParallaxScrollView scrollView) {
        this.context = context;
        this.helper = helper;
        this.scrollView = (ViewGroup) scrollView;
        this.startOffset = new Offset(context, helper);
        this.endOffset = new Offset(context, helper);
        this.touchHandler = new TouchesHandler(helper, startOffset, endOffset, scrollView);
    }

    public LinearLayout prepareView() {
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setBackgroundResource(helper.getBackgroundResource());
        rootLayout.setOrientation(helper.getOrientation());
        LinearLayout contentLayout = prepareContentLayout();
        rootLayout.addView(startOffset.getView());
        rootLayout.addView(contentLayout);
        rootLayout.addView(endOffset.getView());
        helper.setContentToScrollView(scrollView, rootLayout);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        return contentLayout;
    }

    private LinearLayout prepareContentLayout() {
        LinearLayout contentLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = helper.createLayoutParamsForMainPart();
        layoutParams.weight = 1;
        contentLayout.setLayoutParams(layoutParams);
        contentLayout.setOrientation(helper.getOrientation());
        return contentLayout;
    }

    public void handleTouch(MotionEvent event) {
        touchHandler.handleTouch(event);
    }

}
