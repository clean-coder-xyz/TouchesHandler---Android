package com.cleancoder.learning.toucheshandler;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cleancoder.learning.toucheshandler.scrolling.SimpleMotionEvent;

/**
 * Created by lsemenov on 08.09.2014.
 */
public class OrientationHelpers {


    public static final OrientationHelper VERTICAL = new OrientationHelper() {

        @Override
        public int getScrollCoordinate(View view) {
            return view.getScrollY();
        }

        @Override
        public void setSize(View view, int size) {
            ViewUtils.setHeight(view, size);
        }

        @Override
        public ViewGroup.LayoutParams createOffsetLayoutParams() {
            return new ViewGroup.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public Animation newDeceleratingCollapseAnimation(View view) {
            return ViewUtils.newDeceleratingCollapseAnimationVertical(view, this);
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
        public int getStartCoordinate(View view) {
            return view.getTop();
        }

        @Override
        public int getEndCoordinate(View view) {
            return view.getBottom();
        }

    };



    public static final OrientationHelper HORIZONTAL = new OrientationHelper() {

        @Override
        public int getScrollCoordinate(View view) {
            return view.getScrollX();
        }

        @Override
        public void setSize(View view, int size) {
            ViewUtils.setWidth(view, size);
        }

        @Override
        public ViewGroup.LayoutParams createOffsetLayoutParams() {
            return new ViewGroup.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT);
        }

        @Override
        public Animation newDeceleratingCollapseAnimation(View view) {
            return ViewUtils.newDeceleratingCollapseAnimationHorizontal(view, this);
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
        public int getStartCoordinate(View view) {
            return view.getLeft();
        }

        @Override
        public int getEndCoordinate(View view) {
            return view.getRight();
        }

    };

}
