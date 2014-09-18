package com.cleancoder.learning.toucheshandler.superscrolling;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.cleancoder.learning.toucheshandler.TaggedLogger;
import com.cleancoder.learning.toucheshandler.ViewUtils;


/**
 * Created by lsemenov on 17.09.2014.
 */
public class Offset {
    private final View view;
    private final PuzzleGalleryHelper helper;

    public Offset(Context context, PuzzleGalleryHelper helper) {
        this.view = createView(context, helper);
        this.helper = helper;
    }

    private static View createView(Context context, PuzzleGalleryHelper helper) {
        TextView view = new TextView(context);
        view.setLayoutParams(helper.createLayoutParams());
        return view;
    }

    public void hideIfDisplayed() {
        if (ViewUtils.isVisible(view) && helper.getViewSize(view) > 0) {
            helper.collapse(view);
        }
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
