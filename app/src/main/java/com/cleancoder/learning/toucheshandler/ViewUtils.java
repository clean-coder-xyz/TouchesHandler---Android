package com.cleancoder.learning.toucheshandler;

/**
 * Created by lsemenov on 18.09.2014.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

public class ViewUtils {

    public static final View inflate(final Context pContext, final int pLayoutID){
        return LayoutInflater.from(pContext).inflate(pLayoutID, null);
    }

    public static final View inflate(final Context pContext, final int pLayoutID, final ViewGroup pViewGroup){
        return LayoutInflater.from(pContext).inflate(pLayoutID, pViewGroup, true);
    }

    public static void setViewCollapsingUnused(View view) {
        handleCollapseAnimation(view, new CollapseAnimationHandler() {
            @Override
            public void handle(CollapseAnimation collapseAnimation) {
                collapseAnimation.setUsed(false);
            }

            @Override
            public void onThereIsNoAnimationCollapse() { }
        });
    }

    public static boolean isViewCollapsingUsingAndNotCompleted(View view) {
        if (!ViewUtils.isVisible(view) || (view.getWidth() <= 0) || (view.getHeight() <= 0)) {
            return false;
        }
        final ValueHolder<Boolean> viewCollapsingStartedButNotCompleted = new ValueHolder<Boolean>(false);
        handleCollapseAnimation(view, new CollapseAnimationHandler() {
            @Override
            public void handle(CollapseAnimation collapseAnimation) {
                if (!collapseAnimation.isCompleted() && collapseAnimation.isUsed()) {
                    viewCollapsingStartedButNotCompleted.setValue(true);
                }
            }

            @Override
            public void onThereIsNoAnimationCollapse() {
            }
        });
        return viewCollapsingStartedButNotCompleted.getValue();
    }

    public static void setSize(View view, int width, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams(width, height);
            view.setLayoutParams(layoutParams);
        } else {
            layoutParams.width = width;
            layoutParams.height = height;
            view.requestLayout();
        }
    }

    public static void setWidth(View view, int width) {
        setSize(view, width, view.getHeight());
    }

    public static void setHeight(View view, int height) {
        setSize(view, view.getWidth(), height);
    }

    public static boolean isEndEdgeOfScrollViewHasBeenReached(View scrollView, int delta) {
        if (scrollView instanceof HorizontalScrollView) {
            return isRightEdgeOfScrollViewHasBeenReached((HorizontalScrollView) scrollView, delta);
        } else if (scrollView instanceof ScrollView) {
            return isBottomEdgeOfScrollViewHasBeenReached((ScrollView) scrollView, delta);
        }
        throw new IllegalArgumentException("Argument <scrollView> " +
                scrollView + " is not ScrollView nor HorizontalScrollView");
    }

    public static boolean isRightEdgeOfScrollViewHasBeenReached(HorizontalScrollView scrollView, int delta) {
        View childView = scrollView.getChildAt(scrollView.getChildCount() - 1);
        return (childView.getRight()) == (scrollView.getWidth() + scrollView.getScrollX());
    }

    public static boolean isBottomEdgeOfScrollViewHasBeenReached(ScrollView scrollView, int delta) {
        View childView = scrollView.getChildAt(scrollView.getChildCount() - 1);
        return (childView.getBottom()) == (scrollView.getHeight() + scrollView.getScrollY());
    }

    public static boolean isStartEdgeOfScrollViewHasBeenReached(View scrollView) {
        if (scrollView instanceof HorizontalScrollView) {
            return isLeftEdgeOfScrollViewHasBeenReached((HorizontalScrollView) scrollView);
        } else if (scrollView instanceof ScrollView) {
            return isTopEdgeOfScrollViewHasBeenReached((ScrollView) scrollView);
        }
        throw new IllegalArgumentException("Argument <scrollView> " +
                scrollView + " is not ScrollView nor HorizontalScrollView");
    }

    public static boolean isLeftEdgeOfScrollViewHasBeenReached(HorizontalScrollView scrollView) {
        return scrollView.getScrollX() == 0;
    }

    public static boolean isTopEdgeOfScrollViewHasBeenReached(ScrollView scrollView) {
        return scrollView.getScrollY() == 0;
    }


    public static void collapseVertical(final View view, float durationCoefficient) {
        final int initialHeight = view.getMeasuredHeight();
        Animation animation = new CollapseAnimationVertical(view, initialHeight);
        float density = view.getContext().getResources().getDisplayMetrics().density;
        int duration = (int) (durationCoefficient * initialHeight / density);
        animation.setDuration(duration);
        view.startAnimation(animation);
    }

    public static void setVisible(View view, boolean visible) {
        int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        view.setVisibility(visibility);
    }

    public static boolean isVisible(View view) {
        return (view.getVisibility() == View.VISIBLE);
    }

    public static void stopCollapsing(View view) {
        handleCollapseAnimation(view, new CollapseAnimationHandler() {
            @Override
            public void handle(CollapseAnimation collapseAnimation) {
                collapseAnimation.stop();
            }

            @Override
            public void onThereIsNoAnimationCollapse() { }
        });
    }


    private static abstract class CollapseAnimation extends Animation {
        public abstract boolean isStopped();
        public abstract void stop();
        public abstract void setUsed(boolean unused);
        public abstract boolean isUsed();
        public abstract boolean isCompleted();
    }


    private static class CollapseAnimationVertical extends CollapseAnimation {
        private final int initialHeight;
        private final View view;
        private boolean completed;
        private boolean stopped;
        private boolean used;

        public CollapseAnimationVertical(View view, int initialHeight) {
            this.view = view;
            this.initialHeight = initialHeight;
            this.stopped = false;
            this.used = true;
            this.completed = false;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (!isUsed() || isStopped()) {
                return;
            }
            if(interpolatedTime == 1){
                view.setVisibility(View.GONE);
                completed = true;
            } else{
                ViewUtils.setHeight(view, initialHeight - (int) (initialHeight * interpolatedTime));
            }
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public void setUsed(boolean used) {
            this.used = used;
        }

        @Override
        public boolean isUsed() {
            return used;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }
    }


    private static class CollapseAnimationHorizontal extends CollapseAnimation {
        private final int initialWidth;
        private final View view;
        private boolean completed;
        private boolean stopped;
        private boolean used;

        public CollapseAnimationHorizontal(View view, int initialWidth) {
            this.view = view;
            this.initialWidth = initialWidth;
            this.stopped = false;
            this.used = true;
            this.completed = false;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (!isUsed() || isStopped()) {
                return;
            }
            if(interpolatedTime == 1){
                view.setVisibility(View.GONE);
                completed = true;
            } else{
                ViewUtils.setWidth(view, initialWidth - (int)(initialWidth * interpolatedTime));
            }
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public void setUsed(boolean used) {
            this.used = used;
        }

        @Override
        public boolean isUsed() {
            return used;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }
    }


    public static void collapseHorizontal(final View view, float durationCoefficient) {
        final int initialWidth = view.getMeasuredWidth();
        Animation animation = new CollapseAnimationHorizontal(view, initialWidth);
        float density = view.getContext().getResources().getDisplayMetrics().density;
        int duration = (int) (durationCoefficient * initialWidth / density);
        animation.setDuration(duration);
        view.startAnimation(animation);
    }

    private static interface CollapseAnimationHandler {
        void handle(CollapseAnimation collapseAnimation);
        void onThereIsNoAnimationCollapse();
    }

    public static void undoCollapse(final View view) {
        handleCollapseAnimation(view, new CollapseAnimationHandler() {
            @Override
            public void handle(CollapseAnimation collapseAnimation) {
                collapseAnimation.stop();
            }

            @Override
            public void onThereIsNoAnimationCollapse() {
                // skip
            }
        });
    }

    private static void handleCollapseAnimation(View view, CollapseAnimationHandler handler) {
        Animation animation = view.getAnimation();
        if (animation instanceof CollapseAnimation) {
            handler.handle((CollapseAnimation) animation);
        } else {
            handler.onThereIsNoAnimationCollapse();
        }
    }


}
