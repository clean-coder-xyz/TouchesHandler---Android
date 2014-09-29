package com.cleancoder.learning.toucheshandler.scrolling;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

import com.cleancoder.learning.toucheshandler.OrientationHelper;
import com.cleancoder.learning.toucheshandler.TaggedLogger;
import com.cleancoder.learning.toucheshandler.ViewUtils;


/**
 * Created by lsemenov on 17.09.2014.
 */
public class Offset {
    private final View view;
    private final OrientationHelper helper;
    private boolean isStartOffset;
    private boolean isEndOffset;

    public Offset(Context context, OrientationHelper helper) {
        this.view = createView(context, helper);
        this.helper = helper;
    }

    public void setIsStartOffset(boolean isStartOffset) {
        this.isStartOffset = isStartOffset;
    }

    public void setIsEndOffset(boolean isEndOffset) {
        this.isEndOffset = isEndOffset;
    }

    public boolean isStartOffset() {
        return isStartOffset && !isEndOffset;
    }

    public boolean isEndOffset() {
        return isEndOffset && !isStartOffset;
    }

    private static View createView(Context context, OrientationHelper helper) {
        TextView view = new TextView(context);
        view.setLayoutParams(helper.createOffsetLayoutParams());
        view.setBackgroundColor(0xffff5050);
        return view;
    }

    public void hideIfDisplayed() {
        if (ViewUtils.isVisible(view) && helper.getViewSize(view) > 0) {
            hide();
        }
    }

    private void hide() {
        Animation animation = helper.newDeceleratingCollapseAnimation(view);
        view.startAnimation(animation);
    }

    public void set(int size) {
        TaggedLogger.LEONID.debug("Offset.set(" + size + ")");
        ViewUtils.undoCollapse(view);
        ViewUtils.setVisible(view, true);
        helper.setSize(view, size);
    }

    public View getView() {
        return view;
    }

    public int getSize() {
        return helper.getViewSize(view);
    }

}
