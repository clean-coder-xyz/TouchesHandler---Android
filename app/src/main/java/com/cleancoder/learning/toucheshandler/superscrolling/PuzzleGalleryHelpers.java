package com.cleancoder.learning.toucheshandler.superscrolling;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cleancoder.learning.toucheshandler.ViewUtils;

/**
 * Created by lsemenov on 08.09.2014.
 */
public class PuzzleGalleryHelpers {


    public static final PuzzleGalleryHelper VERTICAL = new PuzzleGalleryHelper() {
        @Override
        public void setSize(View view, int size) {
            ViewUtils.setHeight(view, size);
        }

        @Override
        public ViewGroup.LayoutParams createLayoutParams() {
            return new ViewGroup.LayoutParams(1, FrameLayout.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public void collapse(View view) {
            ViewUtils.collapseVertical(view, MagneticEffectConstants.COLLAPSING_DURATION_COEFFICIENT);
        }

        @Override
        public float getCoordinate(SimpleMotionEvent event) {
            return event.getY();
        }

        @Override
        public void setContentToScrollView(ViewGroup scrollView, View contentView) {
            scrollView.addView(contentView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }

        @Override
        public int getOrientation() {
            return LinearLayout.VERTICAL;
        }

        @Override
        public LinearLayout.LayoutParams createLayoutParamsForMainPart() {
            return new LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, 0);
        }

        @Override
        public int getBackgroundResource() {
            return android.R.drawable.editbox_dropdown_light_frame;
        }

        @Override
        public int getViewSize(View view) {
            return view.getHeight();
        }

        @Override
        public int getScrollLength(View view) {
            return view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
        }
    };



    public static final PuzzleGalleryHelper HORIZONTAL = new PuzzleGalleryHelper() {
        @Override
        public void setSize(View view, int size) {
            ViewUtils.setWidth(view, size);
        }

        @Override
        public ViewGroup.LayoutParams createLayoutParams() {
            return new ViewGroup.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, 1);
        }

        @Override
        public void collapse(View view) {
            ViewUtils.collapseHorizontal(view, MagneticEffectConstants.COLLAPSING_DURATION_COEFFICIENT);
        }

        @Override
        public float getCoordinate(SimpleMotionEvent event) {
            return event.getX();
        }

        @Override
        public void setContentToScrollView(ViewGroup scrollView, View contentView) {
            scrollView.addView(contentView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public int getOrientation() {
            return LinearLayout.HORIZONTAL;
        }

        @Override
        public LinearLayout.LayoutParams createLayoutParamsForMainPart() {
            return new LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public int getBackgroundResource() {
            return android.R.drawable.editbox_dropdown_light_frame;
        }

        @Override
        public int getViewSize(View view) {
            return view.getWidth();
        }

        @Override
        public int getScrollLength(View view) {
            return view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        }
    };

}
