package com.cleancoder.learning.toucheshandler;

/**
 * Created by lsemenov on 18.09.2014.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import com.cleancoder.learning.toucheshandler.scrolling.ExpandCollapseAnimation;
import com.cleancoder.learning.toucheshandler.scrolling.ExpandCollapseAnimationFactory;

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
            public void handle(ExpandCollapseAnimation collapseAnimation) {
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
            public void handle(ExpandCollapseAnimation collapseAnimation) {
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

    public static boolean isEndEdgeOfScrollViewHasBeenReached(View scrollView) {
        if (scrollView instanceof HorizontalScrollView) {
            return isRightEdgeOfScrollViewHasBeenReached((HorizontalScrollView) scrollView);
        } else if (scrollView instanceof ScrollView) {
            return isBottomEdgeOfScrollViewHasBeenReached((ScrollView) scrollView);
        }
        throw new IllegalArgumentException("Argument <scrollView> " +
                scrollView + " is not ScrollView nor HorizontalScrollView");
    }

    public static boolean isRightEdgeOfScrollViewHasBeenReached(HorizontalScrollView scrollView) {
        View childView = scrollView.getChildAt(scrollView.getChildCount() - 1);
        return (childView.getRight()) == (scrollView.getWidth() + scrollView.getScrollX());
    }

    public static boolean isBottomEdgeOfScrollViewHasBeenReached(ScrollView scrollView) {
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


    public static Animation newDeceleratingCollapseAnimationVertical(final View view, OrientationHelper helper) {
        final int initialHeight = view.getMeasuredHeight();
        return ExpandCollapseAnimationFactory.newDeceleratingCollapseAnimation(view, initialHeight, helper);
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
            public void handle(ExpandCollapseAnimation collapseAnimation) {
                collapseAnimation.stop();
            }

            @Override
            public void onThereIsNoAnimationCollapse() { }
        });
    }

    public static Animation newDeceleratingCollapseAnimationHorizontal(final View view, OrientationHelper helper) {
        final int initialWidth = view.getMeasuredWidth();
        return ExpandCollapseAnimationFactory.newDeceleratingCollapseAnimation(view, initialWidth, helper);
    }

    private static interface CollapseAnimationHandler {
        void handle(ExpandCollapseAnimation collapseAnimation);
        void onThereIsNoAnimationCollapse();
    }

    public static void undoCollapse(final View view) {
        handleCollapseAnimation(view, new CollapseAnimationHandler() {
            @Override
            public void handle(ExpandCollapseAnimation collapseAnimation) {
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
        if (animation instanceof ExpandCollapseAnimation) {
            handler.handle((ExpandCollapseAnimation) animation);
        } else {
            handler.onThereIsNoAnimationCollapse();
        }
    }

    public static ExpandCollapseAnimation newLinearExpandAnimation(View view, OrientationHelper helper, int targetSize) {
        return ExpandCollapseAnimationFactory.newLinearExpandAnimation(view, targetSize, helper);
    }


}
