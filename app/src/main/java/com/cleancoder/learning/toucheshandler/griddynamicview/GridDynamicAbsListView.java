package com.cleancoder.learning.toucheshandler.griddynamicview;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListAdapter;
import android.widget.Scroller;

import com.cleancoder.learning.toucheshandler.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public abstract class GridDynamicAbsListView extends GridDynamicAdapterView<ListAdapter> implements ViewTreeObserver.OnTouchModeChangeListener,InteraptListener {

    public static interface OnTouchListener {
        void onTouchEvent(MotionEvent event);
        void onInterceptTouchEvent(MotionEvent event);
    }

    public static final int TRANSCRIPT_MODE_DISABLED = 0;
    public static final int TRANSCRIPT_MODE_NORMAL = 1;
    public static final int TRANSCRIPT_MODE_ALWAYS_SCROLL = 2;
    static final int TOUCH_MODE_REST = -1;

    static final int TOUCH_MODE_DOWN = 0;
    static final int TOUCH_MODE_TAP = 1;
    static final int TOUCH_MODE_DONE_WAITING = 2;
    static final int TOUCH_MODE_SCROLL = 3;
    static final int TOUCH_MODE_FLING = 4;
    static final int LAYOUT_NORMAL = 0;
    static final int LAYOUT_FORCE_TOP = 1;
    static final int LAYOUT_SET_SELECTION = 2;
    static final int LAYOUT_FORCE_BOTTOM = 3;
    static final int LAYOUT_SPECIFIC = 4;
    static final int LAYOUT_SYNC = 5;

    static final int LAYOUT_MOVE_SELECTION = 6;
    static final int SCROLL_VERTICAL = 0;
    static final int SCROLL_HORIZONTAL = 1;

    int mLayoutMode = LAYOUT_NORMAL;

    AdapterDataSetObserver mDataSetObserver;
    ListAdapter mAdapter;

    boolean mDrawSelectorOnTop = false;

    Rect mSelectorRect = new Rect();

    final RecycleBin mRecycler = new RecycleBin();

    int mSelectionLeftPadding = 0;
    int mSelectionTopPadding = 0;
    int mSelectionRightPadding = 0;
    int mSelectionBottomPadding = 0;
    Rect mListPadding = new Rect();
    int mWidthMeasureSpec = 0;


    boolean mCachingStarted;
    int mMotionPosition;
    int mMotionX;
    int mMotionY;


    int mTouchMode = TOUCH_MODE_REST;
    private VelocityTracker mVelocityTracker;

    protected int	  mNeedScroll = 0;
    int 			  mSelectedTop = 0;
    boolean 		  mStackFromBottom;
    boolean 		  mScrollingCacheEnabled;

    private OnScrollListener mOnScrollListener;
    private boolean mSmoothScrollbarEnabled = true;

    private Rect mTouchFrame;
    int mResurrectToPosition = INVALID_POSITION;

    private ContextMenuInfo mContextMenuInfo = null;

    private static final int TOUCH_MODE_UNKNOWN = -1;
    private static final int TOUCH_MODE_ON = 0;
    private static final int TOUCH_MODE_OFF = 1;

    private int mLastTouchMode = TOUCH_MODE_UNKNOWN;

    private static final boolean PROFILE_SCROLLING = false;
    private boolean mScrollProfilingStarted = false;

    private static final boolean PROFILE_FLINGING = false;
    private boolean mFlingProfilingStarted = false;

    private CheckForLongPress mPendingCheckForLongPress;

    private Runnable mPendingCheckForTap;

    private CheckForKeyLongPress mPendingCheckForKeyLongPress;

    private GridDynamicAbsListView.PerformClick mPerformClick;

    private int mTranscriptMode;
    private int mCacheColorHint;
    private boolean mIsChildViewEnabled;
    private int mLastScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    private int mTouchSlop;
    private float mDensityScale;

    private Runnable mClearScrollingCache;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private boolean mScrollVerticallyPortrait;
    private boolean mScrollVerticallyLandscape;

    protected boolean mScrollVertically;

    protected boolean mPortraitOrientation;

    protected TouchHandler mTouchHandler;

    final boolean[] mIsScrap = new boolean[1];

    private int mActivePointerId = INVALID_POINTER;

    private static final int INVALID_POINTER = -1;
    protected static boolean  mListDestroyed = false;
    protected static TouchHandler.CaruselRunnable mCaruselRunnable;


    public boolean canInterapt()
    {

        if(!mIsVertical)
        {
            if(mTouchMode == TOUCH_MODE_SCROLL)
                return false;

        }

        return true;
    }

    public interface OnScrollListener
    {

        public static int SCROLL_STATE_IDLE = 0;
        public static int SCROLL_STATE_TOUCH_SCROLL = 1;
        public static int SCROLL_STATE_FLING = 2;

        public void onScrollStateChanged(GridDynamicAbsListView view, int scrollState);

        public void onScroll(GridDynamicAbsListView view, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount);
    }

    public GridDynamicAbsListView(Context context) {
        super(context);
        initAbsListView();
        setupScrollInfo();
        mListDestroyed = false;
    }

    public GridDynamicAbsListView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.absListViewStyle);
        mListDestroyed = false;
    }

    public GridDynamicAbsListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mListDestroyed = false;
        initAbsListView();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.DynamicGridAbsListView, defStyle, 0);

        mDrawSelectorOnTop = a.getBoolean(
                R.styleable.DynamicGridAbsListView_drawSelectorOnTop, false);

        boolean stackFromBottom = a.getBoolean(R.styleable.DynamicGridAbsListView_stackFromBottom, false);
        setStackFromBottom(stackFromBottom);

        boolean scrollingCacheEnabled = a.getBoolean(R.styleable.DynamicGridAbsListView_scrollingCache, true);
        setScrollingCacheEnabled(scrollingCacheEnabled);


        int transcriptMode = a.getInt(R.styleable.DynamicGridAbsListView_transcriptMode,
                TRANSCRIPT_MODE_DISABLED);
        setTranscriptMode(transcriptMode);

        int color = a.getColor(R.styleable.DynamicGridAbsListView_cacheColorHint, 0);
        setCacheColorHint(color);

        boolean smoothScrollbar = a.getBoolean(R.styleable.DynamicGridAbsListView_smoothScrollbar, true);
        setSmoothScrollbarEnabled(smoothScrollbar);

        int scrollDirection = a.getInt(R.styleable.DynamicGridAbsListView_scrollDirectionPortrait, SCROLL_VERTICAL);
        mScrollVerticallyPortrait = (scrollDirection == SCROLL_VERTICAL);

        scrollDirection = a.getInt(R.styleable.DynamicGridAbsListView_scrollDirectionLandscape, SCROLL_VERTICAL);
        mScrollVerticallyLandscape = (scrollDirection == SCROLL_VERTICAL);

        a.recycle();

        setupScrollInfo();
    }

    private void initAbsListView() {

        setClickable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
        setAlwaysDrawnWithCacheEnabled(false);
        setScrollingCacheEnabled(true);

        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mDensityScale = getContext().getResources().getDisplayMetrics().density;
        mPortraitOrientation = (getResources().getConfiguration().orientation !=
                Configuration.ORIENTATION_LANDSCAPE);
        mScrollVertically = true;


    }



    private void setupScrollInfo() {
        mScrollVertically = mPortraitOrientation ? mScrollVerticallyPortrait: mScrollVerticallyLandscape;

        if(mTouchHandler==null)
        {
            if (mScrollVertically) {
                mTouchHandler = new VerticalTouchHandler();
                //setVerticalScrollBarEnabled(true);
                //setHorizontalScrollBarEnabled(false);
                setIsVertical(true);
            } else {
                mTouchHandler = new HorizontalTouchHandler();
                //setVerticalScrollBarEnabled(false);
                //setHorizontalScrollBarEnabled(true);
                setIsVertical(false);
            }
        }

    }

    private boolean orientationChanged() {
        boolean temp = mPortraitOrientation;
        mPortraitOrientation = (getResources().getConfiguration().orientation !=
                Configuration.ORIENTATION_LANDSCAPE);

        boolean result = (temp != mPortraitOrientation);
        if (result) {
            setupScrollInfo();
            mRecycler.scrapActiveViews();
        }

        return result;
    }


    public void setSmoothScrollbarEnabled(boolean enabled) {
        mSmoothScrollbarEnabled = enabled;
    }

    public boolean isSmoothScrollbarEnabled() {
        return mSmoothScrollbarEnabled;
    }


    public void setOnScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
        invokeOnItemScrollListener();
    }

    void invokeOnItemScrollListener() {

        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(this, mFirstPosition, getChildCount(), mItemCount);
        }
    }

    public boolean isScrollingCacheEnabled() {
        return mScrollingCacheEnabled;
    }

    public void setScrollingCacheEnabled(boolean enabled) {
        if (mScrollingCacheEnabled && !enabled) {
            mTouchHandler.clearScrollingCache();
        }
        mScrollingCacheEnabled = enabled;
    }

    @Override
    public void getFocusedRect(Rect r) {
        View view = getSelectedView();
        if (view != null && view.getParent() == this)
        {
            view.getFocusedRect(r);
            offsetDescendantRectToMyCoords(view, r);
        } else {

            super.getFocusedRect(r);
        }
    }

    public boolean isStackFromBottom() {
        return mStackFromBottom;
    }

    public void setStackFromBottom(boolean stackFromBottom) {
        if (mStackFromBottom != stackFromBottom) {
            mStackFromBottom = stackFromBottom;
            requestLayoutIfNecessary();
        }
    }

    void requestLayoutIfNecessary() {
        if (getChildCount() > 0) {
            resetList();
            requestLayout();
            invalidate();
        }
    }

    static class SavedState extends BaseSavedState {
        long selectedId;
        long firstId;
        int viewTop;
        int position;
        int height;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            selectedId = in.readLong();
            firstId = in.readLong();
            viewTop = in.readInt();
            position = in.readInt();
            height = in.readInt();

        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeLong(selectedId);
            out.writeLong(firstId);
            out.writeInt(viewTop);
            out.writeInt(position);
            out.writeInt(height);

        }

        @Override
        public String toString() {
            return "GridDynamicListView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " selectedId=" + selectedId
                    + " firstId=" + firstId
                    + " viewTop=" + viewTop
                    + " position=" + position
                    + " height=" + height + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {

        //TODO save positions	
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        mTouchMode = TOUCH_MODE_REST;

        boolean haveChildren = getChildCount() > 0;
        long selectedId = getSelectedItemId();
        ss.selectedId = selectedId;
        ss.height = getHeight();

        if (selectedId >= 0) {

            ss.viewTop = mSelectedTop;
            ss.position = getSelectedItemPosition();
            ss.firstId = INVALID_POSITION;
        } else {
            if (haveChildren) {

                View v = getChildAt(0);
                if(mScrollVertically) {
                    ss.viewTop = v.getTop();
                } else {
                    ss.viewTop = v.getLeft();
                }
                ss.position = mFirstPosition;
                ss.firstId = mAdapter.getItemId(mFirstPosition);
            } else {
                ss.viewTop = 0;
                ss.firstId = INVALID_POSITION;
                ss.position = 0;
            }
        }
        invalidate();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        mDataChanged = true;

        mSyncSize = ss.height;

        if (ss.selectedId >= 0) {
            mNeedSync = true;
            mSyncRowId = ss.selectedId;
            mSyncPosition = ss.position;
            mSpecificTop = ss.viewTop;
            mSyncMode = SYNC_SELECTED_POSITION;
        } else if (ss.firstId >= 0) {
            setSelectedPositionInt(INVALID_POSITION);

            setNextSelectedPositionInt(INVALID_POSITION);
            mNeedSync = true;
            mSyncRowId = ss.firstId;
            mSyncPosition = ss.position;
            mSpecificTop = ss.viewTop;
            mSyncMode = SYNC_FIRST_POSITION;
        }

        requestLayout();
    }


    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
    {

        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus && mSelectedPosition < 0 && !isInTouchMode()) {
            resurrectSelection();
        }
    }

    @Override
    public void requestLayout() {
        if (!mBlockLayoutRequests && !mInLayout) {
            super.requestLayout();
        }
    }

    public void destroyList()
    {
        mTouchHandler.destroyListRunnables();
    }

    public void resetList() {
        removeAllViewsInLayout();
        mFirstPosition = 0;
        mDataChanged = false;
        mNeedSync = false;
        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;
        setSelectedPositionInt(INVALID_POSITION);
        setNextSelectedPositionInt(INVALID_POSITION);
        mSelectedTop = 0;
        mSelectorRect.setEmpty();
        invalidate();

        if(mCaruselRunnable!=null)
        {
            mCaruselRunnable.stop();
            mCaruselRunnable= null;
        }

    }

    @Override
    protected int computeVerticalScrollExtent() {
        final int count = getChildCount();
        if (count > 0 && mScrollVertically) {
            if (mSmoothScrollbarEnabled) {
                int extent = count * 100;

                View view = getChildAt(0);
                final int top = view.getTop();
                int height = view.getHeight();
                if (height > 0) {
                    extent += (top * 100) / height;
                }

                view = getChildAt(count - 1);
                final int bottom = view.getBottom();
                height = view.getHeight();
                if (height > 0) {
                    extent -= ((bottom - getHeight()) * 100) / height;
                }

                return extent;
            } else {
                return 1;
            }
        }
        return 0;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        final int firstPosition = mFirstPosition;
        final int childCount = getChildCount();
        if (firstPosition >= 0 && childCount > 0 && mScrollVertically) {
            if (mSmoothScrollbarEnabled) {
                final View view = getChildAt(0);
                final int top = view.getTop();
                int height = view.getHeight();
                if (height > 0) {
                    return Math.max(firstPosition * 100 - (top * 100) / height +
                            (int)((float)getScrollY() / getHeight() * mItemCount * 100), 0);
                }
            } else {
                int index;
                final int count = mItemCount;
                if (firstPosition == 0) {
                    index = 0;
                } else if (firstPosition + childCount == count) {
                    index = count;
                } else {
                    index = firstPosition + childCount / 2;
                }
                return (int) (firstPosition + childCount * (index / (float) count));
            }
        }
        return 0;
    }

    @Override
    protected int computeVerticalScrollRange() {
        int result;
        if (!mScrollVertically) {
            result = 0;
        } else if (mSmoothScrollbarEnabled) {
            result = Math.max(mItemCount * 100, 0);
        } else {
            result = mItemCount;
        }
        return result;
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        final int count = getChildCount();
        if (count > 0 && !mScrollVertically) {
            if (mSmoothScrollbarEnabled) {
                int extent = count * 100;

                View view = getChildAt(0);
                final int left = view.getLeft();
                int width = view.getWidth();
                if (width > 0) {
                    extent += (left * 100) / width;
                }

                view = getChildAt(count - 1);
                final int right = view.getRight();
                width = view.getWidth();
                if (width > 0) {
                    extent -= ((right - getWidth()) * 100) / width;
                }

                return extent;
            } else {
                return 1;
            }
        }
        return 0;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        final int firstPosition = mFirstPosition;
        final int childCount = getChildCount();
        if (firstPosition >= 0 && childCount > 0 && !mScrollVertically) {
            if (mSmoothScrollbarEnabled) {
                final View view = getChildAt(0);
                final int left = view.getLeft();
                int width = view.getWidth();
                if (width > 0) {
                    return Math.max(firstPosition * 100 - (left * 100) / width +
                            (int)((float)getScrollX() / getWidth() * mItemCount * 100), 0);
                }
            } else {
                int index;
                final int count = mItemCount;
                if (firstPosition == 0) {
                    index = 0;
                } else if (firstPosition + childCount == count) {
                    index = count;
                } else {
                    index = firstPosition + childCount / 2;
                }
                return (int) (firstPosition + childCount * (index / (float) count));
            }
        }
        return 0;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        int result;
        if (mScrollVertically) {
            result = 0;
        } else if (mSmoothScrollbarEnabled) {
            result = Math.max(mItemCount * 100, 0);
        } else {
            result = mItemCount;
        }
        return result;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        final int count = getChildCount();
        final float fadeEdge = super.getTopFadingEdgeStrength();
        if (count == 0 || !mScrollVertically) {
            return fadeEdge;
        } else {
            if (mFirstPosition > 0) {
                return 1.0f;
            }

            final int top = getChildAt(0).getTop();
            final float fadeLength = getVerticalFadingEdgeLength();
            int paddintTop = getPaddingTop();
            return top < paddintTop ? -(top - paddintTop) / fadeLength : fadeEdge;
        }
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        final int count = getChildCount();
        final float fadeEdge = super.getBottomFadingEdgeStrength();
        if (count == 0 || !mScrollVertically) {
            return fadeEdge;
        } else {
            if (mFirstPosition + count - 1 < mItemCount - 1) {
                return 1.0f;
            }

            final int bottom = getChildAt(count - 1).getBottom();
            final int height = getHeight();
            final float fadeLength = getVerticalFadingEdgeLength();
            int paddingBottom = getPaddingBottom();
            return bottom > height - paddingBottom ?
                    (bottom - height + paddingBottom) / fadeLength : fadeEdge;
        }
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        final int count = getChildCount();
        final float fadeEdge = super.getLeftFadingEdgeStrength();
        if (count == 0 || mScrollVertically) {
            return fadeEdge;
        } else {
            if (mFirstPosition > 0) {
                return 1.0f;
            }

            final int left = getChildAt(0).getLeft();
            final float fadeLength = getHorizontalFadingEdgeLength();
            int paddingLeft = getPaddingLeft();
            return left < paddingLeft ? -(left - paddingLeft) / fadeLength : fadeEdge;
        }
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        final int count = getChildCount();
        final float fadeEdge = super.getRightFadingEdgeStrength();
        if (count == 0 || mScrollVertically) {
            return fadeEdge;
        } else {
            if (mFirstPosition + count - 1 < mItemCount - 1) {
                return 1.0f;
            }

            final int right = getChildAt(count - 1).getRight();
            final int width = getWidth();
            final float fadeLength = getHorizontalFadingEdgeLength();
            int paddingRight = getPaddingRight();
            return right > width - paddingRight ?
                    (right - width + paddingRight) / fadeLength : fadeEdge;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        orientationChanged();

        final Rect listPadding = mListPadding;
        listPadding.left = mSelectionLeftPadding + getPaddingLeft();
        listPadding.top = mSelectionTopPadding + getPaddingTop();
        listPadding.right = mSelectionRightPadding + getPaddingRight();
        listPadding.bottom = mSelectionBottomPadding + getPaddingBottom();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (orientationChanged()) {
            setupScrollInfo();
        }
        super.onLayout(changed, l, t, r, b);
        mInLayout = true;
        if (changed) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).forceLayout();
            }
            mRecycler.markChildrenDirty();
        }

        layoutChildren();
        mInLayout = false;
    }


    protected void layoutChildren() {
    }

	/*
	void updateScrollIndicators() {
		if (mScrollUp != null && mScrollVertically) {
			boolean canScrollUp;
		
			canScrollUp = mFirstPosition > 0;

			if (!canScrollUp) {
				if (getChildCount() > 0) {
					View child = getChildAt(0);
					canScrollUp = child.getTop() < mListPadding.top;
				}
			}

			mScrollUp.setVisibility(canScrollUp ? View.VISIBLE : View.INVISIBLE);
		}

		if (mScrollDown != null && mScrollVertically) {
			boolean canScrollDown;
			int count = getChildCount();

			canScrollDown = (mFirstPosition + count) < mItemCount;

			if (!canScrollDown && count > 0) {
				View child = getChildAt(count - 1);
				canScrollDown = child.getBottom() > getBottom() - mListPadding.bottom;
			}

			mScrollDown.setVisibility(canScrollDown ? View.VISIBLE : View.INVISIBLE);
		}

		if (mScrollLeft != null && !mScrollVertically) {
			boolean canScrollLeft;
			
			canScrollLeft = mFirstPosition > 0;

			if (!canScrollLeft) {
				if (getChildCount() > 0) {
					View child = getChildAt(0);
					canScrollLeft = child.getLeft() < mListPadding.left;
				}
			}

			mScrollLeft.setVisibility(canScrollLeft ? View.VISIBLE : View.INVISIBLE);
		}

		if (mScrollRight != null && !mScrollVertically) {
			boolean canScrollRight;
			int count = getChildCount();

			canScrollRight = (mFirstPosition + count) < mItemCount;

			if (!canScrollRight && count > 0) {
				View child = getChildAt(count - 1);
				canScrollRight = child.getRight() > getRight() - mListPadding.right;
			}

			mScrollRight.setVisibility(canScrollRight ? View.VISIBLE : View.INVISIBLE);
		}
	}*/

    @Override
    public View getSelectedView() {
        if (mItemCount > 0 && mSelectedPosition >= 0) {
            return getChildAt(mSelectedPosition - mFirstPosition);
        } else {
            return null;
        }
    }

    public int getListPaddingTop() {
        return mListPadding.top;
    }

    public int getListPaddingBottom() {
        return mListPadding.bottom;
    }

    public int getListPaddingLeft() {
        return mListPadding.left;
    }

    public int getListPaddingRight() {
        return mListPadding.right;
    }

    View obtainView(int position, boolean[] isScrap) {
        isScrap[0] = false;
        View scrapView;

        scrapView = mRecycler.getScrapView(position);

        View child;
        if (scrapView != null) {

            child = mAdapter.getView(position, scrapView, this);


            if (child != scrapView) {
                mRecycler.addScrapView(scrapView);
                if (mCacheColorHint != 0) {
                    child.setDrawingCacheBackgroundColor(mCacheColorHint);
                }

            } else {
                isScrap[0] = true;
                child.onFinishTemporaryDetach();
            }
        } else {
            child = mAdapter.getView(position, null, this);
            if (mCacheColorHint != 0) {
                child.setDrawingCacheBackgroundColor(mCacheColorHint);
            }

        }

        return child;
    }

    void positionSelector(View sel) {

        final Rect selectorRect = mSelectorRect;
        selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
        positionSelector(selectorRect.left, selectorRect.top, selectorRect.right,
                selectorRect.bottom);

        final boolean isChildViewEnabled = mIsChildViewEnabled;
        if (sel.isEnabled() != isChildViewEnabled) {
            mIsChildViewEnabled = !isChildViewEnabled;
            refreshDrawableState();
        }
    }

    private void positionSelector(int l, int t, int r, int b) {
        mSelectorRect.set(l - mSelectionLeftPadding, t - mSelectionTopPadding, r
                + mSelectionRightPadding, b + mSelectionBottomPadding);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (getChildCount() > 0) {
            mDataChanged = true;
            rememberSyncState();
        }

    }


    boolean touchModeDrawsInPressedState() {

        switch (mTouchMode) {
            case TOUCH_MODE_TAP:
            case TOUCH_MODE_DONE_WAITING:
                return true;
            default:
                return false;
        }
    }

    boolean shouldShowSelector() {
        return (hasFocus() && !isInTouchMode()) || touchModeDrawsInPressedState();
    }

	/*
	public void setScrollIndicators(View up, View down, View left, View right) {
		mScrollUp = up;
		mScrollDown = down;
		mScrollLeft = left;
		mScrollRight = right;
	}*/

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {

        if (mIsChildViewEnabled) {
            return super.onCreateDrawableState(extraSpace);
        }

        final int enabledState = ENABLED_STATE_SET[0];

        int[] state = super.onCreateDrawableState(extraSpace + 1);
        int enabledPos = -1;
        for (int i = state.length - 1; i >= 0; i--) {
            if (state[i] == enabledState) {
                enabledPos = i;
                break;
            }
        }

        if (enabledPos >= 0) {
            System.arraycopy(state, enabledPos + 1, state, enabledPos,
                    state.length - enabledPos - 1);
        }

        return state;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final ViewTreeObserver treeObserver = getViewTreeObserver();
        if (treeObserver != null) {
            treeObserver.addOnTouchModeChangeListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mRecycler.clear();
        final ViewTreeObserver treeObserver = getViewTreeObserver();
        if (treeObserver != null) {
            treeObserver.removeOnTouchModeChangeListener(this);
        }
    }

    ContextMenuInfo createContextMenuInfo(View view, int position, long id) {
        return new AdapterContextMenuInfo(view, position, id);
    }

    private class WindowRunnnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return hasWindowFocus() && getWindowAttachCount() == mOriginalAttachCount;
        }
    }

    private class PerformClick extends WindowRunnnable implements Runnable {
        View mChild;
        int mClickMotionPosition;

        public void run() {

            if (mDataChanged) return;

            final ListAdapter adapter = mAdapter;
            final int motionPosition = mClickMotionPosition;
            if (adapter != null && mItemCount > 0 &&
                    motionPosition != INVALID_POSITION &&
                    motionPosition < adapter.getCount() && sameWindow()) {
                performItemClick(mChild, motionPosition, adapter.getItemId(motionPosition));
            }
        }
    }

    private class CheckForLongPress extends WindowRunnnable implements Runnable {
        public void run() {
            final int motionPosition = mMotionPosition;
            final View child = getChildAt(motionPosition - mFirstPosition);
            if (child != null) {
                final int longPressPosition = mMotionPosition;
                final long longPressId = mAdapter.getItemId(mMotionPosition);

                boolean handled = false;
                if (sameWindow() && !mDataChanged) {
                    handled = performLongPress(child, longPressPosition, longPressId);
                }
                if (handled) {
                    mTouchMode = TOUCH_MODE_REST;
                    setPressed(false);
                    child.setPressed(false);
                } else {
                    mTouchMode = TOUCH_MODE_DONE_WAITING;
                }

            }
        }
    }

    private class CheckForKeyLongPress extends WindowRunnnable implements Runnable {
        public void run() {
            if (isPressed() && mSelectedPosition >= 0) {
                int index = mSelectedPosition - mFirstPosition;
                View v = getChildAt(index);

                if (!mDataChanged) {
                    boolean handled = false;
                    if (sameWindow()) {
                        handled = performLongPress(v, mSelectedPosition, mSelectedRowId);
                    }
                    if (handled) {
                        setPressed(false);
                        v.setPressed(false);
                    }
                } else {
                    setPressed(false);
                    if (v != null) v.setPressed(false);
                }
            }
        }
    }

    private boolean performLongPress(final View child,
                                     final int longPressPosition, final long longPressId) {
        boolean handled = false;

        if (mOnItemLongClickListener != null) {
            handled = mOnItemLongClickListener.onItemLongClick(GridDynamicAbsListView.this, child,
                    longPressPosition, longPressId);
        }
        if (!handled) {
            mContextMenuInfo = createContextMenuInfo(child, longPressPosition, longPressId);
            handled = super.showContextMenuForChild(GridDynamicAbsListView.this);
        }
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }

    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getPositionForView(originalView);
        if (longPressPosition >= 0) {
            final long longPressId = mAdapter.getItemId(longPressPosition);
            boolean handled = false;

            if (mOnItemLongClickListener != null) {
                handled = mOnItemLongClickListener.onItemLongClick(GridDynamicAbsListView.this, originalView,
                        longPressPosition, longPressId);
            }
            if (!handled) {
                mContextMenuInfo = createContextMenuInfo(
                        getChildAt(longPressPosition - mFirstPosition),
                        longPressPosition, longPressId);
                handled = super.showContextMenuForChild(originalView);
            }

            return handled;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!isEnabled()) {
                    return true;
                }
                if (isClickable() && isPressed() &&
                        mSelectedPosition >= 0 && mAdapter != null &&
                        mSelectedPosition < mAdapter.getCount()) {

                    final View view = getChildAt(mSelectedPosition - mFirstPosition);
                    if (view != null) {
                        performItemClick(view, mSelectedPosition, mSelectedRowId);
                        view.setPressed(false);
                    }
                    setPressed(false);
                    return true;
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {

    }

    public int pointToPosition(int x, int y) {
        Rect frame = mTouchFrame;
        if (frame == null) {
            mTouchFrame = new Rect();
            frame = mTouchFrame;
        }

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return mFirstPosition + i;
                }
            }
        }
        return INVALID_POSITION;
    }

    public long pointToRowId(int x, int y) {
        int position = pointToPosition(x, y);
        if (position >= 0) {
            return mAdapter.getItemId(position);
        }
        return INVALID_ROW_ID;
    }

    final class CheckForTap implements Runnable {
        public void run() {
            if (mTouchMode == TOUCH_MODE_DOWN) {
                mTouchMode = TOUCH_MODE_TAP;
                final View child = getChildAt(mMotionPosition - mFirstPosition);
                if (child != null && !child.hasFocusable()) {
                    mLayoutMode = LAYOUT_NORMAL;

                    if (!mDataChanged) {
                        layoutChildren();
                        child.setPressed(true);
                        positionSelector(child);
                        setPressed(true);

                        final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
                        final boolean longClickable = isLongClickable();

                        if (longClickable) {
                            if (mPendingCheckForLongPress == null) {
                                mPendingCheckForLongPress = new CheckForLongPress();
                            }
                            mPendingCheckForLongPress.rememberWindowAttachCount();
                            postDelayed(mPendingCheckForLongPress, longPressTimeout);
                        } else {
                            mTouchMode = TOUCH_MODE_DONE_WAITING;
                        }
                    } else {
                        mTouchMode = TOUCH_MODE_DONE_WAITING;
                    }
                }
            }
        }
    }


    public boolean startScrollIfNeeded(int delta) {
        return mTouchHandler.startScrollIfNeeded(delta);
    }


    public void onTouchModeChanged(boolean isInTouchMode) {
        mTouchHandler.onTouchModeChanged(isInTouchMode);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {

        return super.dispatchTouchEvent(motionEvent);
    }


    private final Collection<OnTouchListener> onTouchListeners = new HashSet<OnTouchListener>();

    public void addOnTouchListener(OnTouchListener onTouchListener) {
        onTouchListeners.add(onTouchListener);
    }

    public void removeOnTouchListener(OnTouchListener onTouchListener) {
        onTouchListeners.remove(onTouchListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for (OnTouchListener onTouchListener : onTouchListeners) {
            onTouchListener.onTouchEvent(event);
        }
        return mTouchHandler.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        for (OnTouchListener onTouchListener : onTouchListeners) {
            onTouchListener.onInterceptTouchEvent(event);
        }
        return mTouchHandler.onInterceptTouchEvent(event);
    }

    @Override
    public void addTouchables(ArrayList<View> views) {
        final int count = getChildCount();
        final int firstPosition = mFirstPosition;
        final ListAdapter adapter = mAdapter;

        if (adapter == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (adapter.isEnabled(firstPosition + i)) {
                views.add(child);
            }
            child.addTouchables(views);
        }
    }

    void reportScrollStateChange(int newState) {
        if (newState != mLastScrollState) {
            if (mOnScrollListener != null) {
                mOnScrollListener.onScrollStateChanged(this, newState);
                mLastScrollState = newState;
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        mTouchHandler.onWindowFocusChanged(hasWindowFocus);
    }


    public void smoothScrollToPosition(int position) {
        mTouchHandler.smoothScrollToPosition(position);
    }

    public void smoothScrollToPosition(int position, int boundPosition) {
        mTouchHandler.smoothScrollToPosition(position, boundPosition);
    }

    public void smoothScrollBy(int distance, int duration) {
        mTouchHandler.smoothScrollBy(distance, duration);
    }

    int getHeaderViewsCount() {
        return 0;
    }

    int getFooterViewsCount() {
        return 0;
    }


    abstract void fillGap(boolean down);

    void hideSelector() {
        if (mSelectedPosition != INVALID_POSITION) {
            if (mLayoutMode != LAYOUT_SPECIFIC) {
                mResurrectToPosition = mSelectedPosition;
            }
            if (mNextSelectedPosition >= 0 && mNextSelectedPosition != mSelectedPosition) {
                mResurrectToPosition = mNextSelectedPosition;
            }
            setSelectedPositionInt(INVALID_POSITION);
            setNextSelectedPositionInt(INVALID_POSITION);
            mSelectedTop = 0;
            mSelectorRect.setEmpty();
        }
    }

    int reconcileSelectedPosition() {
        int position = mSelectedPosition;
        if (position < 0) {
            position = mResurrectToPosition;
        }
        position = Math.max(0, position);
        position = Math.min(position, mItemCount - 1);
        return position;
    }


    abstract int findMotionRowY(int y);
    int findClosestMotionRowY(int y) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return INVALID_POSITION;
        }

        final int motionRow = findMotionRowY(y);
        return motionRow != INVALID_POSITION ? motionRow : mFirstPosition + childCount - 1;
    }

    abstract int findMotionRowX(int x);

    int findClosestMotionRow(int x) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return INVALID_POSITION;
        }

        final int motionRow = findMotionRowX(x);
        return motionRow != INVALID_POSITION ? motionRow : mFirstPosition + childCount - 1;
    }


    public void invalidateViews() {
        mDataChanged = true;
        rememberSyncState();
        requestLayout();
        invalidate();
    }

    abstract void setSelectionInt(int position);

    boolean resurrectSelection() {
        return mTouchHandler.resurrectSelection();
    }

    @Override
    protected void handleDataChanged() {
        int count = mItemCount;
        if (count > 0) {

            int newPos;

            int selectablePos;

            if (mNeedSync) {

                mNeedSync = false;

                if (mTranscriptMode == TRANSCRIPT_MODE_ALWAYS_SCROLL ||
                        (mTranscriptMode == TRANSCRIPT_MODE_NORMAL &&
                                mFirstPosition + getChildCount() >= mOldItemCount)) {
                    mLayoutMode = LAYOUT_FORCE_BOTTOM;
                    return;
                }

                switch (mSyncMode) {
                    case SYNC_SELECTED_POSITION:
                        if (isInTouchMode()) {

                            mLayoutMode = LAYOUT_SYNC;
                            mSyncPosition = Math.min(Math.max(0, mSyncPosition), count - 1);

                            return;
                        } else {


                            newPos = findSyncPosition();
                            if (newPos >= 0) {

                                selectablePos = lookForSelectablePosition(newPos, true);
                                if (selectablePos == newPos) {

                                    mSyncPosition = newPos;
                                    int size = mIsVertical ? getHeight() : getWidth();
                                    if (mSyncSize == size) {

                                        mLayoutMode = LAYOUT_SYNC;
                                    } else {

                                        mLayoutMode = LAYOUT_SET_SELECTION;
                                    }

                                    setNextSelectedPositionInt(newPos);
                                    return;
                                }
                            }
                        }
                        break;
                    case SYNC_FIRST_POSITION:

                        mLayoutMode = LAYOUT_SYNC;
                        mSyncPosition = Math.min(Math.max(0, mSyncPosition), count - 1);

                        return;
                }
            }

            if (!isInTouchMode()) {

                newPos = getSelectedItemPosition();

                if (newPos >= count) {
                    newPos = count - 1;
                }
                if (newPos < 0) {
                    newPos = 0;
                }

                selectablePos = lookForSelectablePosition(newPos, true);

                if (selectablePos >= 0) {
                    setNextSelectedPositionInt(selectablePos);
                    return;
                } else {

                    selectablePos = lookForSelectablePosition(newPos, false);
                    if (selectablePos >= 0) {
                        setNextSelectedPositionInt(selectablePos);
                        return;
                    }
                }
            } else {

                if (mResurrectToPosition >= 0) {
                    return;
                }
            }

        }

        mLayoutMode = mStackFromBottom ? LAYOUT_FORCE_BOTTOM : LAYOUT_FORCE_TOP;
        mSelectedPosition = INVALID_POSITION;
        mSelectedRowId = INVALID_ROW_ID;
        mNextSelectedPosition = INVALID_POSITION;
        mNextSelectedRowId = INVALID_ROW_ID;
        mNeedSync = false;
        checkSelectionChanged();
    }

    static int getDistance(Rect source, Rect dest, int direction) {
        int sX, sY; // source x, y
        int dX, dY; // dest x, y
        switch (direction) {
            case View.FOCUS_RIGHT:
                sX = source.right;
                sY = source.top + source.height() / 2;
                dX = dest.left;
                dY = dest.top + dest.height() / 2;
                break;
            case View.FOCUS_DOWN:
                sX = source.left + source.width() / 2;
                sY = source.bottom;
                dX = dest.left + dest.width() / 2;
                dY = dest.top;
                break;
            case View.FOCUS_LEFT:
                sX = source.left;
                sY = source.top + source.height() / 2;
                dX = dest.right;
                dY = dest.top + dest.height() / 2;
                break;
            case View.FOCUS_UP:
                sX = source.left + source.width() / 2;
                sY = source.top;
                dX = dest.left + dest.width() / 2;
                dY = dest.bottom;
                break;
            case View.FOCUS_FORWARD:
            case View.FOCUS_BACKWARD:
                sX = source.right + source.width() / 2;
                sY = source.top + source.height() / 2;
                dX = dest.left + dest.width() / 2;
                dY = dest.top + dest.height() / 2;
                break;
            default:
                throw new IllegalArgumentException("direction must be one of "
                        + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, "
                        + "FOCUS_FORWARD, FOCUS_BACKWARD}.");
        }
        int deltaX = dX - sX;
        int deltaY = dY - sY;
        return deltaY * deltaY + deltaX * deltaX;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new GridDynamicAbsListView.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof GridDynamicAbsListView.LayoutParams;
    }

    public void setTranscriptMode(int mode) {
        mTranscriptMode = mode;
    }

    public int getTranscriptMode() {
        return mTranscriptMode;
    }

    public void setScrollDirectionPortrait(int direction) {
        boolean tempDirection = mScrollVerticallyPortrait;
        mScrollVerticallyPortrait = (direction == SCROLL_VERTICAL);
        if (tempDirection != mScrollVerticallyPortrait) {
            setupScrollInfo();

            resetList();
            mRecycler.clear();
        }
    }


    public int getScrollDirectionPortrait() {
        return mScrollVerticallyPortrait ? SCROLL_VERTICAL : SCROLL_HORIZONTAL;
    }


    public void setScrollDirectionLandscape(int direction) {
        boolean tempDirection = mScrollVerticallyLandscape;
        mScrollVerticallyLandscape = (direction == SCROLL_VERTICAL);
        if (tempDirection != mScrollVerticallyLandscape) {
            setupScrollInfo();

            resetList();
            mRecycler.clear();
        }
    }

    public int getScrollDirectionLandscape() {
        return mScrollVerticallyLandscape ? SCROLL_VERTICAL : SCROLL_HORIZONTAL;
    }

    @Override
    public int getSolidColor() {
        return mCacheColorHint;
    }

    public void setCacheColorHint(int color) {
        if (color != mCacheColorHint) {
            mCacheColorHint = color;
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).setDrawingCacheBackgroundColor(color);
            }
            mRecycler.setCacheColorHint(color);
        }
    }

    public int getCacheColorHint() {
        return mCacheColorHint;
    }

    public void reclaimViews(List<View> views) {
        int childCount = getChildCount();
        RecyclerListener listener = mRecycler.mRecyclerListener;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            GridDynamicAbsListView.LayoutParams lp = (GridDynamicAbsListView.LayoutParams) child.getLayoutParams();

            if (lp != null && mRecycler.shouldRecycleViewType(lp.viewType)) {
                views.add(child);
                if (listener != null) {

                    listener.onMovedToScrapHeap(child);
                }
            }
        }
        mRecycler.reclaimScrapViews(views);
        removeAllViewsInLayout();
    }

    protected boolean checkConsistency(int consistency) {
        boolean result = true;

        final boolean checkLayout = true;

        if (checkLayout) {

            final View[] activeViews = mRecycler.mActiveViews;
            int count = activeViews.length;
            for (int i = 0; i < count; i++) {
                if (activeViews[i] != null) {
                    result = false;
                    Log.d("Consistency",
                            "AbsListView " + this + " has a view in its active recycler: " +
                                    activeViews[i]);
                }
            }

            final ArrayList<View> scrap = mRecycler.mCurrentScrap;
            if (!checkScrap(scrap)) result = false;
            final ArrayList<View>[] scraps = mRecycler.mScrapViews;
            count = scraps.length;
            for (int i = 0; i < count; i++) {
                if (!checkScrap(scraps[i])) result = false;
            }
        }

        return result;
    }

    private boolean checkScrap(ArrayList<View> scrap) {
        if (scrap == null) return true;
        boolean result = true;

        final int count = scrap.size();
        for (int i = 0; i < count; i++) {
            final View view = scrap.get(i);
            if (view.getParent() != null) {
                result = false;

            }
            if (indexOfChild(view) >= 0) {
                result = false;
            }
        }

        return result;
    }

    public void setRecyclerListener(RecyclerListener listener) {
        mRecycler.mRecyclerListener = listener;
    }

    public void setHorisontalRescrollerEnabled(boolean isEnable)
    {
        mIsCaruselScroller = isEnable;
        mTouchHandler.centerViewPosition(1);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        int viewType;
        boolean recycledHeaderFooter;
        boolean forceAdd;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, int viewType) {
            super(w, h);
            this.viewType = viewType;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public static interface RecyclerListener {

        void onMovedToScrapHeap(View view);
    }

    class RecycleBin {
        private RecyclerListener mRecyclerListener;
        private int mFirstActivePosition;
        private View[] mActiveViews = new View[0];
        private ArrayList<View>[] mScrapViews;

        private int mViewTypeCount;

        private ArrayList<View> mCurrentScrap;

        public void setViewTypeCount(int viewTypeCount) {
            if (viewTypeCount < 1) {
                throw new IllegalArgumentException("Can't have a viewTypeCount < 1");
            }

            ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
            for (int i = 0; i < viewTypeCount; i++) {
                scrapViews[i] = new ArrayList<View>();
            }
            mViewTypeCount = viewTypeCount;
            mCurrentScrap = scrapViews[0];
            mScrapViews = scrapViews;
        }

        public void markChildrenDirty() {
            if (mViewTypeCount == 1) {
                final ArrayList<View> scrap = mCurrentScrap;
                final int scrapCount = scrap.size();
                for (int i = 0; i < scrapCount; i++) {
                    scrap.get(i).forceLayout();
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> scrap = mScrapViews[i];
                    final int scrapCount = scrap.size();
                    for (int j = 0; j < scrapCount; j++) {
                        scrap.get(j).forceLayout();
                    }
                }
            }
        }

        public boolean shouldRecycleViewType(int viewType) {
            return viewType >= 0;
        }


        void clear() {
            if (mViewTypeCount == 1) {
                final ArrayList<View> scrap = mCurrentScrap;
                final int scrapCount = scrap.size();
                for (int i = 0; i < scrapCount; i++) {
                    removeDetachedView(scrap.remove(scrapCount - 1 - i), false);
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> scrap = mScrapViews[i];
                    final int scrapCount = scrap.size();
                    for (int j = 0; j < scrapCount; j++) {
                        removeDetachedView(scrap.remove(scrapCount - 1 - j), false);
                    }
                }
            }
        }

        void fillActiveViews(int childCount, int firstActivePosition) {
            if (mActiveViews.length < childCount) {
                mActiveViews = new View[childCount];
            }
            mFirstActivePosition = firstActivePosition;

            final View[] activeViews = mActiveViews;
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                GridDynamicAbsListView.LayoutParams lp = (GridDynamicAbsListView.LayoutParams) child.getLayoutParams();

                if (lp != null && lp.viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {

                    activeViews[i] = child;
                }
            }
        }

        View getActiveView(int position) {
            int index = position - mFirstActivePosition;
            final View[] activeViews = mActiveViews;
            if (index >=0 && index < activeViews.length) {
                final View match = activeViews[index];
                activeViews[index] = null;
                return match;
            }
            return null;
        }


        View getScrapView(int position) {
            ArrayList<View> scrapViews;
            if (mViewTypeCount == 1) {
                scrapViews = mCurrentScrap;
                int size = scrapViews.size();
                if (size > 0) {
                    return scrapViews.remove(size - 1);
                } else {
                    return null;
                }
            } else {
                int whichScrap = mAdapter.getItemViewType(position);
                if (whichScrap >= 0 && whichScrap < mScrapViews.length) {
                    scrapViews = mScrapViews[whichScrap];
                    int size = scrapViews.size();
                    if (size > 0) {
                        return scrapViews.remove(size - 1);
                    }
                }
            }
            return null;
        }


        void addScrapView(View scrap) {
            GridDynamicAbsListView.LayoutParams lp = (GridDynamicAbsListView.LayoutParams) scrap.getLayoutParams();
            if (lp == null) {
                return;
            }

            int viewType = lp.viewType;
            if (!shouldRecycleViewType(viewType)) {
                if (viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                    removeDetachedView(scrap, false);
                }
                return;
            }

            if (mViewTypeCount == 1) {
                scrap.onStartTemporaryDetach();
                mCurrentScrap.add(scrap);
            } else {
                scrap.onStartTemporaryDetach();
                mScrapViews[viewType].add(scrap);
            }

            if (mRecyclerListener != null) {
                mRecyclerListener.onMovedToScrapHeap(scrap);
            }

        }


        void scrapActiveViews() {
            final View[] activeViews = mActiveViews;
            final boolean hasListener = mRecyclerListener != null;
            final boolean multipleScraps = mViewTypeCount > 1;

            ArrayList<View> scrapViews = mCurrentScrap;
            final int count = activeViews.length;
            for (int i = count - 1; i >= 0; i--) {
                final View victim = activeViews[i];
                if (victim != null) {
                    int whichScrap = ((GridDynamicAbsListView.LayoutParams) victim.getLayoutParams()).viewType;

                    activeViews[i] = null;

                    if (!shouldRecycleViewType(whichScrap)) {

                        if (whichScrap != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                            removeDetachedView(victim, false);
                        }
                        continue;
                    }

                    if (multipleScraps) {
                        scrapViews = mScrapViews[whichScrap];
                    }
                    victim.onStartTemporaryDetach();
                    scrapViews.add(victim);

                    if (hasListener) {
                        mRecyclerListener.onMovedToScrapHeap(victim);
                    }

                }
            }

            pruneScrapViews();
        }


        private void pruneScrapViews() {
            final int maxViews = mActiveViews.length;
            final int viewTypeCount = mViewTypeCount;
            final ArrayList<View>[] scrapViews = mScrapViews;
            for (int i = 0; i < viewTypeCount; ++i) {
                final ArrayList<View> scrapPile = scrapViews[i];
                int size = scrapPile.size();
                final int extras = size - maxViews;
                size--;
                for (int j = 0; j < extras; j++) {
                    removeDetachedView(scrapPile.remove(size--), false);
                }
            }
        }

        void reclaimScrapViews(List<View> views) {
            if (mViewTypeCount == 1) {
                views.addAll(mCurrentScrap);
            } else {
                final int viewTypeCount = mViewTypeCount;
                final ArrayList<View>[] scrapViews = mScrapViews;
                for (int i = 0; i < viewTypeCount; ++i) {
                    final ArrayList<View> scrapPile = scrapViews[i];
                    views.addAll(scrapPile);
                }
            }
        }


        void setCacheColorHint(int color) {
            if (mViewTypeCount == 1) {
                final ArrayList<View> scrap = mCurrentScrap;
                final int scrapCount = scrap.size();
                for (int i = 0; i < scrapCount; i++) {
                    scrap.get(i).setDrawingCacheBackgroundColor(color);
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> scrap = mScrapViews[i];
                    final int scrapCount = scrap.size();
                    for (int j = 0; j < scrapCount; j++) {
                        scrap.get(i).setDrawingCacheBackgroundColor(color);
                    }
                }
            }

            final View[] activeViews = mActiveViews;
            final int count = activeViews.length;
            for (int i = 0; i < count; ++i) {
                final View victim = activeViews[i];
                if (victim != null) {
                    victim.setDrawingCacheBackgroundColor(color);
                }
            }
        }
    }

    abstract class TouchHandler {

        protected FlingRunnable 	 mFlingRunnable;
        protected PositionScroller 	 mPositionScroller;

        int mMotionCorrection;

        public void destroyListRunnables()
        {
            mListDestroyed = true;

            if(mFlingRunnable!=null)
            {
                mFlingRunnable.stop();
                removeCallbacks(mFlingRunnable);

                mFlingRunnable.destroy();
                mFlingRunnable = null;
            }

            if(mCaruselRunnable!=null)
            {
                mCaruselRunnable.stop();
                removeCallbacks(mCaruselRunnable);

                mCaruselRunnable = null;
            }

        }

        public void onWindowFocusChanged(boolean hasWindowFocus) {

            final int touchMode = isInTouchMode() ? TOUCH_MODE_ON : TOUCH_MODE_OFF;

            if (!hasWindowFocus) {
                setChildrenDrawingCacheEnabled(false);
                if (mFlingRunnable != null) {
                    removeCallbacks(mFlingRunnable);

                    mFlingRunnable.endFling();

                    if (getScrollY() != 0) {
                        scrollTo(getScrollX(), 0);
                        //mScrollY = 0;
                        invalidate();
                    }
                }

                if (touchMode == TOUCH_MODE_OFF) {
                    // Remember the last selected element
                    mResurrectToPosition = mSelectedPosition;
                }
            } else {

                if (touchMode != mLastTouchMode && mLastTouchMode != TOUCH_MODE_UNKNOWN) {

                    if (touchMode == TOUCH_MODE_OFF) {

                        resurrectSelection();

                    } else {
                        hideSelector();
                        mLayoutMode = LAYOUT_NORMAL;
                        layoutChildren();
                    }
                }
            }

            mLastTouchMode = touchMode;
        }


        public boolean startScrollIfNeeded(int delta) {


            final int distance = Math.abs(delta);
            if (distance > mTouchSlop) {
                createScrollingCache();
                mTouchMode = TOUCH_MODE_SCROLL;
                mMotionCorrection = delta;
                final Handler handler = getHandler();

                if (handler != null) {
                    handler.removeCallbacks(mPendingCheckForLongPress);
                }
                setPressed(false);
                View motionView = getChildAt(mMotionPosition - mFirstPosition);
                if (motionView != null) {
                    motionView.setPressed(false);
                }

                reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                requestDisallowInterceptTouchEvent(true);
                return true;
            }

            return false;
        }


        public void onTouchModeChanged(boolean isInTouchMode) {
            if (isInTouchMode) {

                hideSelector();

                if (getHeight() > 0 && getChildCount() > 0) {

                    layoutChildren();
                }
            }
        }


        void reportScrollStateChange(int newState) {
            if (newState != mLastScrollState) {
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrollStateChanged(GridDynamicAbsListView.this, newState);
                    mLastScrollState = newState;
                }
            }
        }


        public void smoothScrollToPosition(int position) {
            if (mPositionScroller == null) {
                mPositionScroller = getPositionScroller();
            }
            mPositionScroller.start(position);
        }


        public void smoothScrollToPosition(int position, int boundPosition) {
            if (mPositionScroller == null) {
                mPositionScroller = getPositionScroller();
            }
            mPositionScroller.start(position, boundPosition);
        }

        public void smoothScrollBy(int distance, int duration)
        {
            if(mListDestroyed)
                return;

            if (mFlingRunnable != null)
            {
                mFlingRunnable.endFling();
                mFlingRunnable.startScroll(distance, duration);
            }
        }


        protected void createScrollingCache() {
            if (mScrollingCacheEnabled && !mCachingStarted) {
                setChildrenDrawnWithCacheEnabled(true);
                setChildrenDrawingCacheEnabled(true);
                mCachingStarted = true;
            }
        }

        protected void clearScrollingCache() {
            if (mClearScrollingCache == null) {
                mClearScrollingCache = new Runnable() {
                    public void run() {
                        if (mCachingStarted) {
                            mCachingStarted = false;
                            setChildrenDrawnWithCacheEnabled(false);
                            if ((GridDynamicAbsListView.this.getPersistentDrawingCache() & PERSISTENT_SCROLLING_CACHE) == 0) {
                                setChildrenDrawingCacheEnabled(false);
                            }
                            if (!isAlwaysDrawnWithCacheEnabled()) {
                                invalidate();
                            }
                        }
                    }
                };
            }
            post(mClearScrollingCache);
        }

        abstract boolean trackMotionScroll(int delta, int incrementalDelta);
        abstract boolean resurrectSelection();
        abstract void	 centerViewPosition(int position);

        public abstract boolean onTouchEvent(MotionEvent ev);


        public abstract boolean onInterceptTouchEvent(MotionEvent ev);

        protected abstract PositionScroller getPositionScroller();

        protected abstract FlingRunnable getFlingRunnable();
        protected abstract CaruselRunnable getCaruselRunnable();

        protected class CaruselRunnable implements Runnable
        {
            public void start()
            {
                postDelayed(this, 5000);
            }

            public void stop()
            {
                removeCallbacks(this);
            }


            @Override
            public void run() {

                if(!mListDestroyed)
                {
                    if(mFlingRunnable!=null)
                        mFlingRunnable.centerViews(getChildCount()-1);

                    start();

                }else

                    removeCallbacks(this);

            }
        }


        protected abstract class FlingRunnable implements Runnable {

            protected final Scroller mScroller;
            protected Runnable mCheckFlywheel;

            abstract public void centerViews(int position);

            public boolean isScrollingInDirection(float xvel, float yvel) {
                final int dx = mScroller.getFinalX() - mScroller.getStartX();
                final int dy = mScroller.getFinalY() - mScroller.getStartY();
                return !mScroller.isFinished() && Math.signum(xvel) == Math.signum(dx) &&
                        Math.signum(yvel) == Math.signum(dy);
            }


            FlingRunnable() {
                mScroller = new Scroller(getContext());
            }

            abstract void destroy();

            abstract void flywheelTouch();

            abstract void start(int initialVelocity);

            abstract void startScroll(int distance, int duration);

            protected void endFling() {
                mTouchMode = TOUCH_MODE_REST;

                reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                clearScrollingCache();

                removeCallbacks(this);

                if (mCheckFlywheel != null) {
                    removeCallbacks(mCheckFlywheel);
                }
                if (mPositionScroller != null) {
                    removeCallbacks(mPositionScroller);
                }

                mScroller.abortAnimation();
            }

            public abstract void run();
            public abstract void stop();
        }

        abstract class PositionScroller implements Runnable {
            protected static final int SCROLL_DURATION = 400;

            protected static final int MOVE_DOWN_POS = 1;
            protected static final int MOVE_UP_POS = 2;
            protected static final int MOVE_DOWN_BOUND = 3;
            protected static final int MOVE_UP_BOUND = 4;

            protected boolean mVertical;
            protected int mMode;
            protected int mTargetPos;
            protected int mBoundPos;
            protected int mLastSeenPos;
            protected int mScrollDuration;
            protected final int mExtraScroll;

            PositionScroller() {
                mExtraScroll = ViewConfiguration.get(mContext).getScaledFadingEdgeLength();
            }

            void start(int position) {
                final int firstPos = mFirstPosition;
                final int lastPos = firstPos + getChildCount() - 1;

                int viewTravelCount = 0;
                if (position <= firstPos) {
                    viewTravelCount = firstPos - position + 1;
                    mMode = MOVE_UP_POS;
                } else if (position >= lastPos) {
                    viewTravelCount = position - lastPos + 1;
                    mMode = MOVE_DOWN_POS;
                } else {

                    return;
                }

                if (viewTravelCount > 0) {
                    mScrollDuration = SCROLL_DURATION / viewTravelCount;
                } else {
                    mScrollDuration = SCROLL_DURATION;
                }
                mTargetPos = position;
                mBoundPos = INVALID_POSITION;
                mLastSeenPos = INVALID_POSITION;

                post(this);
            }

            void start(int position, int boundPosition) {
                if (boundPosition == INVALID_POSITION) {
                    start(position);
                    return;
                }

                final int firstPos = mFirstPosition;
                final int lastPos = firstPos + getChildCount() - 1;

                int viewTravelCount = 0;
                if (position <= firstPos) {
                    final int boundPosFromLast = lastPos - boundPosition;
                    if (boundPosFromLast < 1) {

                        return;
                    }

                    final int posTravel = firstPos - position + 1;
                    final int boundTravel = boundPosFromLast - 1;
                    if (boundTravel < posTravel) {
                        viewTravelCount = boundTravel;
                        mMode = MOVE_UP_BOUND;
                    } else {
                        viewTravelCount = posTravel;
                        mMode = MOVE_UP_POS;
                    }
                } else if (position >= lastPos) {
                    final int boundPosFromFirst = boundPosition - firstPos;
                    if (boundPosFromFirst < 1) {

                        return;
                    }

                    final int posTravel = position - lastPos + 1;
                    final int boundTravel = boundPosFromFirst - 1;
                    if (boundTravel < posTravel) {
                        viewTravelCount = boundTravel;
                        mMode = MOVE_DOWN_BOUND;
                    } else {
                        viewTravelCount = posTravel;
                        mMode = MOVE_DOWN_POS;
                    }
                } else {

                    return;
                }

                if (viewTravelCount > 0) {
                    mScrollDuration = SCROLL_DURATION / viewTravelCount;
                } else {
                    mScrollDuration = SCROLL_DURATION;
                }
                mTargetPos = position;
                mBoundPos = boundPosition;
                mLastSeenPos = INVALID_POSITION;

                post(this);
            }

            void stop() {
                removeCallbacks(this);
            }

            public abstract void run();
        }


    }

    class VerticalTouchHandler extends TouchHandler {


        int mMotionViewOriginalTop;
        int mLastY;
        int mMotionViewNewTop;

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (!isEnabled()) {

                return isClickable() || isLongClickable();
            }


            final int action = ev.getAction();

            View v;
            int deltaY;

            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(ev);

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    final int x = (int) ev.getX();
                    final int y = (int) ev.getY();
                    int motionPosition = pointToPosition(x, y);
                    if (!mDataChanged) {
                        if ((mTouchMode != TOUCH_MODE_FLING) && (motionPosition >= 0)
                                && (getAdapter().isEnabled(motionPosition))) {

                            mTouchMode = TOUCH_MODE_DOWN;
                            if (mPendingCheckForTap == null) {
                                mPendingCheckForTap = new CheckForTap();
                            }
                            postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                        } else {
                            if (ev.getEdgeFlags() != 0 && motionPosition < 0) {

                                return false;
                            }

                            if (mTouchMode == TOUCH_MODE_FLING) {

                                createScrollingCache();
                                mTouchMode = TOUCH_MODE_SCROLL;
                                mMotionCorrection = 0;
                                motionPosition = findMotionRowY(y);
                                mFlingRunnable.flywheelTouch();
                            }
                        }
                    }

                    if (motionPosition >= 0) {

                        v = getChildAt(motionPosition - mFirstPosition);
                        mMotionViewOriginalTop = v.getTop();
                    }
                    mMotionX = x;
                    mMotionY = y;
                    mMotionPosition = motionPosition;
                    mLastY = Integer.MIN_VALUE;
                    break;
                }

                case MotionEvent.ACTION_MOVE: {
                    final int y = (int) ev.getY();
                    deltaY = y - mMotionY;
                    switch (mTouchMode) {
                        case TOUCH_MODE_DOWN:
                        case TOUCH_MODE_TAP:
                        case TOUCH_MODE_DONE_WAITING:

                            startScrollIfNeeded(deltaY);
                            break;
                        case TOUCH_MODE_SCROLL:
                            if (PROFILE_SCROLLING) {
                                if (!mScrollProfilingStarted) {

                                    mScrollProfilingStarted = true;
                                }
                            }

                            if (y != mLastY) {
                                deltaY -= mMotionCorrection;
                                int incrementalDeltaY = mLastY != Integer.MIN_VALUE ? y - mLastY : deltaY;


                                boolean atEdge = false;
                                if (incrementalDeltaY != 0) {
                                    atEdge = trackMotionScroll(deltaY, incrementalDeltaY);
                                }

                                if (atEdge && getChildCount() > 0) {

                                    int motionPosition = findMotionRowY(y);
                                    if (motionPosition >= 0) {
                                        final View motionView = getChildAt(motionPosition - mFirstPosition);
                                        mMotionViewOriginalTop = motionView.getTop();
                                    }
                                    mMotionY = y;
                                    mMotionPosition = motionPosition;
                                    invalidate();
                                }
                                mLastY = y;
                            }
                            break;
                    }

                    break;
                }

                case MotionEvent.ACTION_UP: {
                    switch (mTouchMode) {
                        case TOUCH_MODE_DOWN:
                        case TOUCH_MODE_TAP:
                        case TOUCH_MODE_DONE_WAITING:
                            final int motionPosition = mMotionPosition;
                            final View child = getChildAt(motionPosition - mFirstPosition);
                            if (child != null && !child.hasFocusable()) {
                                if (mTouchMode != TOUCH_MODE_DOWN) {
                                    child.setPressed(false);
                                }

                                if (mPerformClick == null) {
                                    mPerformClick = new PerformClick();
                                }

                                final GridDynamicAbsListView.PerformClick performClick = mPerformClick;
                                performClick.mChild = child;
                                performClick.mClickMotionPosition = motionPosition;
                                performClick.rememberWindowAttachCount();

                                mResurrectToPosition = motionPosition;

                                if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP) {
                                    final Handler handler = getHandler();
                                    if (handler != null) {
                                        handler.removeCallbacks(mTouchMode == TOUCH_MODE_DOWN ?
                                                mPendingCheckForTap : mPendingCheckForLongPress);
                                    }
                                    mLayoutMode = LAYOUT_NORMAL;
                                    if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
                                        mTouchMode = TOUCH_MODE_TAP;
                                        setSelectedPositionInt(mMotionPosition);
                                        layoutChildren();
                                        child.setPressed(true);
                                        positionSelector(child);
                                        setPressed(true);
                                        postDelayed(new Runnable() {
                                            public void run() {
                                                child.setPressed(false);
                                                setPressed(false);
                                                if (!mDataChanged) {
                                                    post(performClick);
                                                }
                                                mTouchMode = TOUCH_MODE_REST;
                                            }
                                        }, ViewConfiguration.getPressedStateDuration());
                                    } else {
                                        mTouchMode = TOUCH_MODE_REST;
                                    }
                                    return true;
                                } else if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
                                    post(performClick);
                                }
                            }
                            mTouchMode = TOUCH_MODE_REST;
                            break;
                        case TOUCH_MODE_SCROLL:
                            final int childCount = getChildCount();
                            if (childCount > 0) {
                                if (mFirstPosition == 0 && getChildAt(0).getTop() >= mListPadding.top &&
                                        mFirstPosition + childCount < mItemCount &&
                                        getChildAt(childCount - 1).getBottom() <=
                                                getHeight() - mListPadding.bottom) {
                                    mTouchMode = TOUCH_MODE_REST;
                                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                                } else {
                                    final VelocityTracker velocityTracker = mVelocityTracker;
                                    velocityTracker.computeCurrentVelocity(500);
                                    final int initialVelocity = (int) velocityTracker.getYVelocity();

                                    if (Math.abs(initialVelocity) > mMinimumVelocity) {
                                        if (mFlingRunnable == null) {
                                            mFlingRunnable = new VerticalFlingRunnable();
                                        }
                                        reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);

                                        mFlingRunnable.start(-initialVelocity);
                                    } else {
                                        mTouchMode = TOUCH_MODE_REST;
                                        reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                                    }
                                }
                            } else {
                                mTouchMode = TOUCH_MODE_REST;
                                reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                            }
                            break;
                    }

                    setPressed(false);
                    invalidate();

                    final Handler handler = getHandler();
                    if (handler != null) {
                        handler.removeCallbacks(mPendingCheckForLongPress);
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }

                    mActivePointerId = INVALID_POINTER;

                    if (PROFILE_SCROLLING) {
                        if (mScrollProfilingStarted) {

                            mScrollProfilingStarted = false;
                        }
                    }
                    break;
                }

                case MotionEvent.ACTION_CANCEL: {
                    mTouchMode = TOUCH_MODE_REST;
                    setPressed(false);
                    View motionView = GridDynamicAbsListView.this.getChildAt(mMotionPosition - mFirstPosition);
                    if (motionView != null) {
                        motionView.setPressed(false);
                    }
                    clearScrollingCache();

                    final Handler handler = getHandler();
                    if (handler != null) {
                        handler.removeCallbacks(mPendingCheckForLongPress);
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }

                    mActivePointerId = INVALID_POINTER;
                    break;
                }

            }

            return true;
        }



        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            int action = ev.getAction();
            View v;

            int x = (int) ev.getX();
            int y = (int) ev.getY();
            int motionPosition = findMotionRowY(y);


            for(int i = 0 ; i< getChildCount();i++)
            {
                View child = getChildAt(i);
                if(child instanceof InteraptListener)
                {
                    if(!((InteraptListener) child).canInterapt())
                        return false;
                }

            }


            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    int touchMode = mTouchMode;

                    if (touchMode != TOUCH_MODE_FLING && motionPosition >= 0) {

                        v = getChildAt(motionPosition - mFirstPosition);
                        mMotionViewOriginalTop = v.getTop();
                        mMotionX = x;
                        mMotionY = y;
                        mMotionPosition = motionPosition;
                        mTouchMode = TOUCH_MODE_DOWN;
                        clearScrollingCache();
                    }
                    mLastY = Integer.MIN_VALUE;
                    initOrResetVelocityTracker();
                    mVelocityTracker.addMovement(ev);
                    if (touchMode == TOUCH_MODE_FLING) {
                        return true;
                    }
                    break;
                }

                case MotionEvent.ACTION_MOVE: {
                    switch (mTouchMode) {
                        case TOUCH_MODE_DOWN:
                            if (startScrollIfNeeded(y - mMotionY)) {
                                return true;
                            }
                            break;
                    }
                    break;
                }

                case MotionEvent.ACTION_UP: {
                    mTouchMode = TOUCH_MODE_REST;
                    mActivePointerId = INVALID_POINTER;
                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    break;
                }
            }

            return false;
        }

        @Override
        boolean trackMotionScroll(int deltaY, int incrementalDeltaY) {

            final int childCount = getChildCount();
            if (childCount == 0) {
                return true;
            }

            final int firstTop = getChildAt(0).getTop();
            final int lastBottom = getChildAt(childCount - 1).getBottom();

            final Rect listPadding = mListPadding;

            // FIXME account for grid vertical spacing too?
            final int spaceAbove = listPadding.top - firstTop;
            final int end = getHeight() - listPadding.bottom;
            final int spaceBelow = lastBottom - end;

            final int height = getHeight() - getPaddingBottom() - getPaddingTop();
            if (deltaY < 0) {
                deltaY = Math.max(-(height - 1), deltaY);
            } else {
                deltaY = Math.min(height - 1, deltaY);
            }

            if (incrementalDeltaY < 0) {
                incrementalDeltaY = Math.max(-(height - 1), incrementalDeltaY);
            } else {
                incrementalDeltaY = Math.min(height - 1, incrementalDeltaY);
            }

            final int firstPosition = mFirstPosition;

            if (firstPosition == 0 && firstTop >= listPadding.top && deltaY >= 0) {

                return true;
            }

            if (firstPosition + childCount == mItemCount && lastBottom <= end && deltaY <= 0) {

                return true;
            }

            final boolean down = incrementalDeltaY < 0;

            final boolean inTouchMode = isInTouchMode();
            if (inTouchMode) {
                hideSelector();
            }

            final int headerViewsCount = getHeaderViewsCount();
            final int footerViewsStart = mItemCount - getFooterViewsCount();

            int start = 0;
            int count = 0;

            if (down) {
                final int top = listPadding.top - incrementalDeltaY;
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    if (child.getBottom() >= top) {
                        break;
                    } else {
                        count++;
                        int position = firstPosition + i;
                        if (position >= headerViewsCount && position < footerViewsStart) {
                            mRecycler.addScrapView(child);

                        }
                    }
                }
            } else {
                final int bottom = getHeight() - listPadding.bottom - incrementalDeltaY;
                for (int i = childCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);
                    if (child.getTop() <= bottom) {
                        break;
                    } else {
                        start = i;
                        count++;
                        int position = firstPosition + i;
                        if (position >= headerViewsCount && position < footerViewsStart) {
                            mRecycler.addScrapView(child);

                        }
                    }
                }
            }

            mMotionViewNewTop = mMotionViewOriginalTop + deltaY;

            mBlockLayoutRequests = true;

            if (count > 0) {
                detachViewsFromParent(start, count);
            }
            offsetChildrenTopAndBottom(incrementalDeltaY);

            if (down) {
                mFirstPosition += count;
            }

            invalidate();

            final int absIncrementalDeltaY = Math.abs(incrementalDeltaY);
            if (spaceAbove < absIncrementalDeltaY || spaceBelow < absIncrementalDeltaY) {
                fillGap(down);
            }

            if (!inTouchMode && mSelectedPosition != INVALID_POSITION) {
                final int childIndex = mSelectedPosition - mFirstPosition;
                if (childIndex >= 0 && childIndex < getChildCount()) {
                    positionSelector(getChildAt(childIndex));
                }
            }

            mBlockLayoutRequests = false;

            invokeOnItemScrollListener();

            return false;
        }

        @Override
        boolean resurrectSelection() {
            final int childCount = getChildCount();

            if (childCount <= 0) {
                return false;
            }

            int selectedTop = 0;
            int selectedPos;
            int childrenTop = mListPadding.top;
            int childrenBottom = getBottom() - getTop() - mListPadding.bottom;
            final int firstPosition = mFirstPosition;
            final int toPosition = mResurrectToPosition;
            boolean down = true;

            if (toPosition >= firstPosition && toPosition < firstPosition + childCount) {
                selectedPos = toPosition;

                final View selected = getChildAt(selectedPos - mFirstPosition);
                selectedTop = selected.getTop();
                int selectedBottom = selected.getBottom();

                if (selectedTop < childrenTop) {
                    selectedTop = childrenTop + getVerticalFadingEdgeLength();
                } else if (selectedBottom > childrenBottom) {
                    selectedTop = childrenBottom - selected.getMeasuredHeight()
                            - getVerticalFadingEdgeLength();
                }
            } else {
                if (toPosition < firstPosition) {

                    selectedPos = firstPosition;
                    for (int i = 0; i < childCount; i++) {
                        final View v = getChildAt(i);
                        final int top = v.getTop();

                        if (i == 0) {

                            selectedTop = top;

                            if (firstPosition > 0 || top < childrenTop) {
                                childrenTop += getVerticalFadingEdgeLength();
                            }
                        }
                        if (top >= childrenTop) {

                            selectedPos = firstPosition + i;
                            selectedTop = top;
                            break;
                        }
                    }
                } else {
                    final int itemCount = mItemCount;
                    down = false;
                    selectedPos = firstPosition + childCount - 1;

                    for (int i = childCount - 1; i >= 0; i--) {
                        final View v = getChildAt(i);
                        final int top = v.getTop();
                        final int bottom = v.getBottom();

                        if (i == childCount - 1) {
                            selectedTop = top;
                            if (firstPosition + childCount < itemCount || bottom > childrenBottom) {
                                childrenBottom -= getVerticalFadingEdgeLength();
                            }
                        }

                        if (bottom <= childrenBottom) {
                            selectedPos = firstPosition + i;
                            selectedTop = top;
                            break;
                        }
                    }
                }
            }

            mResurrectToPosition = INVALID_POSITION;
            removeCallbacks(mFlingRunnable);
            mTouchMode = TOUCH_MODE_REST;
            clearScrollingCache();
            mSpecificTop = selectedTop;
            selectedPos = lookForSelectablePosition(selectedPos, down);
            if (selectedPos >= firstPosition && selectedPos <= getLastVisiblePosition()) {
                mLayoutMode = LAYOUT_SPECIFIC;
                setSelectionInt(selectedPos);
                invokeOnItemScrollListener();
            } else {
                selectedPos = INVALID_POSITION;
            }
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);

            return selectedPos >= 0;
        }



        @Override
        protected PositionScroller getPositionScroller() {
            return new VerticalPositionScroller();
        }

        @Override
        protected FlingRunnable getFlingRunnable()
        {
            return new VerticalFlingRunnable();
        }

        private class VerticalFlingRunnable extends FlingRunnable {

            protected int mLastFlingY;

            @Override
            void start(int initialVelocity) {
                int initialY = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
                mLastFlingY = initialY;
                mScroller.fling(0, initialY, 0, initialVelocity,
                        0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
                mTouchMode = TOUCH_MODE_FLING;
                post(this);

                if (PROFILE_FLINGING) {
                    if (!mFlingProfilingStarted) {

                        mFlingProfilingStarted = true;
                    }
                }
            }

            @Override
            void startScroll(int distance, int duration) {
                int initialY = distance < 0 ? Integer.MAX_VALUE : 0;
                mLastFlingY = initialY;
                mScroller.startScroll(0, initialY, 0, distance, duration);
                mTouchMode = TOUCH_MODE_FLING;
                post(this);
            }

            @Override
            public void run() {
                switch (mTouchMode) {
                    default:
                        return;

                    case TOUCH_MODE_FLING: {
                        if (mItemCount == 0 || getChildCount() == 0) {
                            endFling();
                            return;
                        }

                        final Scroller scroller = mScroller;
                        boolean more = scroller.computeScrollOffset();
                        final int y = scroller.getCurrY();

                        int delta = mLastFlingY - y;

                        if (delta > 0) {

                            mMotionPosition = mFirstPosition;
                            final View firstView = getChildAt(0);
                            mMotionViewOriginalTop = firstView.getTop();

                            delta = Math.min(getHeight() - getPaddingBottom() - getPaddingTop() - 1, delta);
                        } else {

                            int offsetToLast = getChildCount() - 1;
                            mMotionPosition = mFirstPosition + offsetToLast;

                            final View lastView = getChildAt(offsetToLast);
                            mMotionViewOriginalTop = lastView.getTop();

                            delta = Math.max(-(getHeight() - getPaddingBottom() - getPaddingTop() - 1), delta);
                        }

                        final boolean atEnd = trackMotionScroll(delta, delta);

                        if (more && !atEnd) {
                            invalidate();
                            mLastFlingY = y;
                            post(this);
                        } else {
                            endFling();

                            if (PROFILE_FLINGING) {
                                if (mFlingProfilingStarted) {

                                    mFlingProfilingStarted = false;
                                }
                            }
                        }
                        break;
                    }
                }

            }

            private static final int FLYWHEEL_TIMEOUT = 40; // milliseconds

            public void flywheelTouch() {
                if(mCheckFlywheel == null) {
                    mCheckFlywheel = new Runnable() {
                        public void run() {
                            final VelocityTracker vt = mVelocityTracker;
                            if (vt == null) {
                                return;
                            }

                            vt.computeCurrentVelocity(500, mMaximumVelocity);
                            final float yvel = -vt.getYVelocity();

                            if (Math.abs(yvel) >= mMinimumVelocity
                                    && isScrollingInDirection(0, yvel)) {

                                postDelayed(this, FLYWHEEL_TIMEOUT);
                            } else {
                                endFling();
                                mTouchMode = TOUCH_MODE_SCROLL;
                                reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                            }
                        }
                    };
                }
                postDelayed(mCheckFlywheel, FLYWHEEL_TIMEOUT);
            }

            @Override
            public void centerViews(int position) {

            }

            @Override
            public void stop()
            {
                removeCallbacks(this);
            }

            @Override
            void destroy() {
                // TODO Auto-generated method stub

            }
        }


        class VerticalPositionScroller extends PositionScroller {
            @Override
            public void run() {
                final int listHeight = getHeight();
                final int firstPos = mFirstPosition;

                switch (mMode) {
                    case MOVE_DOWN_POS: {
                        final int lastViewIndex = getChildCount() - 1;
                        final int lastPos = firstPos + lastViewIndex;

                        if (lastViewIndex < 0) {
                            return;
                        }

                        if (lastPos == mLastSeenPos) {

                            post(this);
                            return;
                        }

                        final View lastView = getChildAt(lastViewIndex);
                        final int lastViewHeight = lastView.getHeight();
                        final int lastViewTop = lastView.getTop();
                        final int lastViewPixelsShowing = listHeight - lastViewTop;
                        final int extraScroll = lastPos < mItemCount - 1 ? mExtraScroll : mListPadding.bottom;

                        smoothScrollBy(lastViewHeight - lastViewPixelsShowing + extraScroll,
                                mScrollDuration);

                        mLastSeenPos = lastPos;
                        if (lastPos < mTargetPos) {
                            post(this);
                        }
                        break;
                    }

                    case MOVE_DOWN_BOUND: {
                        final int nextViewIndex = 1;
                        final int childCount = getChildCount();

                        if (firstPos == mBoundPos || childCount <= nextViewIndex
                                || firstPos + childCount >= mItemCount) {
                            return;
                        }
                        final int nextPos = firstPos + nextViewIndex;

                        if (nextPos == mLastSeenPos) {

                            post(this);
                            return;
                        }

                        final View nextView = getChildAt(nextViewIndex);
                        final int nextViewHeight = nextView.getHeight();
                        final int nextViewTop = nextView.getTop();
                        final int extraScroll = mExtraScroll;
                        if (nextPos < mBoundPos) {
                            smoothScrollBy(Math.max(0, nextViewHeight + nextViewTop - extraScroll),
                                    mScrollDuration);

                            mLastSeenPos = nextPos;

                            post(this);
                        } else  {
                            if (nextViewTop > extraScroll) {
                                smoothScrollBy(nextViewTop - extraScroll, mScrollDuration);
                            }
                        }
                        break;
                    }

                    case MOVE_UP_POS: {
                        if (firstPos == mLastSeenPos) {

                            post(this);
                            return;
                        }

                        final View firstView = getChildAt(0);
                        if (firstView == null) {
                            return;
                        }
                        final int firstViewTop = firstView.getTop();
                        final int extraScroll = firstPos > 0 ? mExtraScroll : mListPadding.top;

                        smoothScrollBy(firstViewTop - extraScroll, mScrollDuration);

                        mLastSeenPos = firstPos;

                        if (firstPos > mTargetPos) {
                            post(this);
                        }
                        break;
                    }

                    case MOVE_UP_BOUND: {
                        final int lastViewIndex = getChildCount() - 2;
                        if (lastViewIndex < 0) {
                            return;
                        }
                        final int lastPos = firstPos + lastViewIndex;

                        if (lastPos == mLastSeenPos) {

                            post(this);
                            return;
                        }

                        final View lastView = getChildAt(lastViewIndex);
                        final int lastViewHeight = lastView.getHeight();
                        final int lastViewTop = lastView.getTop();
                        final int lastViewPixelsShowing = listHeight - lastViewTop;
                        mLastSeenPos = lastPos;
                        if (lastPos > mBoundPos) {
                            smoothScrollBy(-(lastViewPixelsShowing - mExtraScroll), mScrollDuration);
                            post(this);
                        } else {
                            final int bottom = listHeight - mExtraScroll;
                            final int lastViewBottom = lastViewTop + lastViewHeight;
                            if (bottom > lastViewBottom) {
                                smoothScrollBy(-(bottom - lastViewBottom), mScrollDuration);
                            }
                        }
                        break;
                    }

                    default:
                        break;
                }
            }
        }


        @Override
        void centerViewPosition(int position) {
            // TODO Auto-generated method stub

        }



        @Override
        protected CaruselRunnable getCaruselRunnable() {

            return new CaruselRunnable();
        }
    }



    class HorizontalTouchHandler extends TouchHandler {

        int mMotionViewOriginalLeft;
        int mLastX;
        int mMotionViewNewLeft;



        @Override
        protected FlingRunnable getFlingRunnable()
        {
            return new HorizontalFlingRunnable();
        }


        @Override
        protected PositionScroller getPositionScroller() {
            return new HorizontalPositionScroller();
        }


        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev)
        {
            int action = ev.getAction();
            View v;

            //Log.e("action intercept", "intercept");

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    //Log.e("action intercept", "intercept Down");
                    int touchMode = mTouchMode;

                    final int x = (int) ev.getX();
                    final int y = (int) ev.getY();

                    int motionPosition = findMotionRowX(x);
                    if (touchMode != TOUCH_MODE_FLING && motionPosition >= 0) {

                        v = getChildAt(motionPosition - mFirstPosition);
                        mMotionViewOriginalLeft = v.getLeft();
                        mMotionX = x;
                        mMotionY = y;
                        mMotionPosition = motionPosition;
                        mTouchMode = TOUCH_MODE_DOWN;
                        clearScrollingCache();
                    }
                    mLastX = Integer.MIN_VALUE;
                    initOrResetVelocityTracker();
                    mVelocityTracker.addMovement(ev);
                    if (touchMode == TOUCH_MODE_FLING) {
                        return true;
                    }
                    break;
                }

                case MotionEvent.ACTION_MOVE: {
                    //Log.e("action intercept", "intercept Move");
                    switch (mTouchMode) {
                        case TOUCH_MODE_DOWN:
                            final int x = (int) ev.getX();
                            if (startScrollIfNeeded(x - mMotionX)) {
                                return true;
                            }
                            break;
                    }
                    break;
                }

                case MotionEvent.ACTION_UP:
                {
                    //Log.e("action intercept", "intercept Up");
                    mTouchMode = TOUCH_MODE_REST;
                    mActivePointerId = INVALID_POINTER;
                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    break;
                }

            }

            return false;
        }


        public void scrollToCenter(int deltaX)
        {

            if (mFlingRunnable == null) {
                mFlingRunnable = new HorizontalFlingRunnable();
            }

            if(deltaX>0)
            {
                mFlingRunnable.centerViews(0);

            }else
            {
                mFlingRunnable.centerViews(1);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {


            if (!isEnabled()) {

                return isClickable() || isLongClickable();
            }


            final int action = ev.getAction();

            View v;
            int deltaX;

            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(ev);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                {


                    if (mCaruselRunnable == null)
                    {
                        if(!mListDestroyed)
                            mCaruselRunnable = getCaruselRunnable();
                    }

                    if(mCaruselRunnable!=null)
                        mCaruselRunnable.stop();

                    //Log.e("action move", "Down");
                    final int x = (int) ev.getX();
                    final int y = (int) ev.getY();
                    int motionPosition = pointToPosition(x, y);
                    if (!mDataChanged) {
                        if ((mTouchMode != TOUCH_MODE_FLING) && (motionPosition >= 0)
                                && (getAdapter().isEnabled(motionPosition))) {

                            mTouchMode = TOUCH_MODE_DOWN;
                            if (mPendingCheckForTap == null) {
                                mPendingCheckForTap = new CheckForTap();
                            }
                            postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                        } else {
                            if (ev.getEdgeFlags() != 0 && motionPosition < 0) {

                                return false;
                            }

                            if (mTouchMode == TOUCH_MODE_FLING) {

                                createScrollingCache();
                                mTouchMode = TOUCH_MODE_SCROLL;
                                mMotionCorrection = 0;
                                motionPosition = findMotionRowX(x);
                                mFlingRunnable.flywheelTouch();
                            }
                        }
                    }

                    if (motionPosition >= 0) {
                        // Remember where the motion event started
                        v = getChildAt(motionPosition - mFirstPosition);
                        mMotionViewOriginalLeft = v.getLeft();
                    }
                    mMotionX = x;
                    mMotionY = y;
                    mMotionPosition = motionPosition;
                    mLastX = Integer.MIN_VALUE;
                    break;
                }

                case MotionEvent.ACTION_MOVE:
                {
                    if (mCaruselRunnable == null)
                    {
                        if(!mListDestroyed)
                            mCaruselRunnable = getCaruselRunnable();
                    }

                    if(mCaruselRunnable!=null)
                        mCaruselRunnable.stop();


                    //Log.e("action move", "Move");
                    final int x = (int) ev.getX();
                    deltaX = x - mMotionX;
                    switch (mTouchMode) {
                        case TOUCH_MODE_DOWN:
                        case TOUCH_MODE_TAP:
                        case TOUCH_MODE_DONE_WAITING:

                            startScrollIfNeeded(deltaX);
                            break;
                        case TOUCH_MODE_SCROLL:
                            if (PROFILE_SCROLLING) {
                                if (!mScrollProfilingStarted) {

                                    mScrollProfilingStarted = true;
                                }
                            }

                            if (x != mLastX) {
                                deltaX -= mMotionCorrection;
                                int incrementalDeltaX = mLastX != Integer.MIN_VALUE ? x - mLastX : deltaX;

                                boolean atEdge = false;

                                if (incrementalDeltaX != 0)
                                {
                                    atEdge = trackMotionScroll(deltaX, incrementalDeltaX);
                                }


                                if (atEdge && getChildCount() > 0) {

                                    int motionPosition = findMotionRowX(x);
                                    if (motionPosition >= 0) {
                                        final View motionView = getChildAt(motionPosition - mFirstPosition);
                                        mMotionViewOriginalLeft = motionView.getLeft();
                                    }

                                    mMotionX = x;
                                    mMotionPosition = motionPosition;
                                    invalidate();
                                }
                                mLastX = x;
                            }
                            break;
                    }

                    break;
                }

                case MotionEvent.ACTION_UP: {

                    //Log.e("action move", "Up");
                    switch (mTouchMode) {
                        case TOUCH_MODE_DOWN:
                        case TOUCH_MODE_TAP:
                        case TOUCH_MODE_DONE_WAITING:
                            final int motionPosition = mMotionPosition;
                            final View child = getChildAt(motionPosition - mFirstPosition);
                            if (child != null && !child.hasFocusable()) {
                                if (mTouchMode != TOUCH_MODE_DOWN) {
                                    child.setPressed(false);
                                }

                                if (mPerformClick == null) {
                                    mPerformClick = new PerformClick();
                                }

                                final GridDynamicAbsListView.PerformClick performClick = mPerformClick;
                                performClick.mChild = child;
                                performClick.mClickMotionPosition = motionPosition;
                                performClick.rememberWindowAttachCount();

                                mResurrectToPosition = motionPosition;

                                if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP) {
                                    final Handler handler = getHandler();
                                    if (handler != null) {
                                        handler.removeCallbacks(mTouchMode == TOUCH_MODE_DOWN ?
                                                mPendingCheckForTap : mPendingCheckForLongPress);
                                    }
                                    mLayoutMode = LAYOUT_NORMAL;
                                    if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
                                        mTouchMode = TOUCH_MODE_TAP;
                                        setSelectedPositionInt(mMotionPosition);
                                        layoutChildren();
                                        child.setPressed(true);
                                        positionSelector(child);
                                        setPressed(true);
                                        postDelayed(new Runnable() {
                                            public void run() {
                                                child.setPressed(false);
                                                setPressed(false);
                                                if (!mDataChanged) {
                                                    post(performClick);
                                                }
                                                mTouchMode = TOUCH_MODE_REST;
                                            }
                                        }, ViewConfiguration.getPressedStateDuration());
                                    } else {
                                        mTouchMode = TOUCH_MODE_REST;
                                    }
                                    return true;
                                } else if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
                                    post(performClick);
                                }
                            }

                            mTouchMode = TOUCH_MODE_REST;
                            break;
                        case TOUCH_MODE_SCROLL:
                            final int childCount = getChildCount();
                            if (childCount > 0) {
                                if (mFirstPosition == 0 && getChildAt(0).getLeft() >= mListPadding.left &&
                                        mFirstPosition + childCount < mItemCount &&
                                        getChildAt(childCount - 1).getRight() <=
                                                getWidth() - mListPadding.right) {
                                    mTouchMode = TOUCH_MODE_REST;
                                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                                } else {

                                    if(!mIsCaruselScroller)
                                    {
                                        final VelocityTracker velocityTracker = mVelocityTracker;
                                        velocityTracker.computeCurrentVelocity(500);
                                        final int initialVelocity = (int) velocityTracker.getXVelocity();

                                        if (Math.abs(initialVelocity) > mMinimumVelocity) {
                                            if (mFlingRunnable == null) {
                                                if(!mListDestroyed)
                                                    mFlingRunnable = new HorizontalFlingRunnable();
                                            }
                                            reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);

                                            if(mFlingRunnable!=null)
                                                mFlingRunnable.start(-initialVelocity);
                                        } else {

                                            if(!mIsCaruselScroller)
                                            {
                                                final int x = (int) ev.getX();
                                                deltaX = x - mMotionX;
                                                scrollToCenter(deltaX);
                                            }

                                            mTouchMode = TOUCH_MODE_REST;
                                            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                                        }
                                    }else
                                    {
                                        final int x = (int) ev.getX();
                                        deltaX = x - mMotionX;
                                        scrollToCenter(deltaX);
                                    }
                                }
                            } else {
                                mTouchMode = TOUCH_MODE_REST;
                                reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                            }
                            break;
                    }

                    setPressed(false);

                    invalidate();

                    final Handler handler = getHandler();
                    if (handler != null) {
                        handler.removeCallbacks(mPendingCheckForLongPress);
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }

                    mActivePointerId = INVALID_POINTER;

                    if (PROFILE_SCROLLING) {
                        if (mScrollProfilingStarted) {

                            mScrollProfilingStarted = false;
                        }
                    }
                    break;
                }

                case MotionEvent.ACTION_CANCEL: {
                    //Log.e("action move", "Cancel");

                    mTouchMode = TOUCH_MODE_REST;
                    setPressed(false);
                    View motionView = GridDynamicAbsListView.this.getChildAt(mMotionPosition - mFirstPosition);
                    if (motionView != null) {
                        motionView.setPressed(false);
                    }
                    clearScrollingCache();

                    final Handler handler = getHandler();
                    if (handler != null) {
                        handler.removeCallbacks(mPendingCheckForLongPress);
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }

                    mActivePointerId = INVALID_POINTER;
                    break;
                }
            }

            return true;
        }


        @Override
        boolean resurrectSelection() {
            final int childCount = getChildCount();

            if (childCount <= 0) {
                return false;
            }

            int selectedLeft = 0;
            int selectedPos;
            int childrenLeft = mListPadding.top;
            int childrenRight = getRight() - getLeft() - mListPadding.right;
            final int firstPosition = mFirstPosition;
            final int toPosition = mResurrectToPosition;
            boolean down = true;

            if (toPosition >= firstPosition && toPosition < firstPosition + childCount) {
                selectedPos = toPosition;

                final View selected = getChildAt(selectedPos - mFirstPosition);
                selectedLeft = selected.getLeft();
                int selectedRight = selected.getRight();

                if (selectedLeft < childrenLeft) {
                    selectedLeft = childrenLeft + getHorizontalFadingEdgeLength();
                } else if (selectedRight > childrenRight) {
                    selectedLeft = childrenRight - selected.getMeasuredWidth()
                            - getHorizontalFadingEdgeLength();
                }
            } else {
                if (toPosition < firstPosition) {

                    selectedPos = firstPosition;
                    for (int i = 0; i < childCount; i++) {
                        final View v = getChildAt(i);
                        final int left = v.getLeft();

                        if (i == 0) {

                            selectedLeft = left;

                            if (firstPosition > 0 || left < childrenLeft) {

                                childrenLeft += getHorizontalFadingEdgeLength();
                            }
                        }
                        if (left >= childrenLeft) {

                            selectedPos = firstPosition + i;
                            selectedLeft = left;
                            break;
                        }
                    }
                } else {
                    final int itemCount = mItemCount;
                    down = false;
                    selectedPos = firstPosition + childCount - 1;

                    for (int i = childCount - 1; i >= 0; i--) {
                        final View v = getChildAt(i);
                        final int left = v.getLeft();
                        final int right = v.getRight();

                        if (i == childCount - 1) {
                            selectedLeft = left;
                            if (firstPosition + childCount < itemCount || right > childrenRight) {
                                childrenRight -= getHorizontalFadingEdgeLength();
                            }
                        }

                        if (right <= childrenRight) {
                            selectedPos = firstPosition + i;
                            selectedLeft = left;
                            break;
                        }
                    }
                }
            }

            mResurrectToPosition = INVALID_POSITION;
            removeCallbacks(mFlingRunnable);
            mTouchMode = TOUCH_MODE_REST;
            clearScrollingCache();
            mSpecificTop = selectedLeft;
            selectedPos = lookForSelectablePosition(selectedPos, down);
            if (selectedPos >= firstPosition && selectedPos <= getLastVisiblePosition()) {
                mLayoutMode = LAYOUT_SPECIFIC;
                setSelectionInt(selectedPos);
                invokeOnItemScrollListener();
            } else {
                selectedPos = INVALID_POSITION;
            }
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);

            return selectedPos >= 0;
        }


        @Override
        boolean trackMotionScroll(int delta, int incrementalDelta) {

            final int childCount = getChildCount();
            if (childCount == 0) {
                return true;
            }

            final int firstLeft = getChildAt(0).getLeft();
            final int lastRight = getChildAt(childCount - 1).getRight();

            final Rect listPadding = mListPadding;

            final int spaceAbove = listPadding.left - firstLeft;
            final int end = getWidth() - listPadding.right;
            final int spaceBelow = lastRight - end;

            final int width = getWidth() - getPaddingRight() - getPaddingLeft();
            if (delta < 0) {
                delta = Math.max(-(width - 1), delta);
            } else {
                delta = Math.min(width - 1, delta);
            }

            if (incrementalDelta < 0) {
                incrementalDelta = Math.max(-(width - 1), incrementalDelta);
            } else {
                incrementalDelta = Math.min(width - 1, incrementalDelta);
            }

            final int firstPosition = mFirstPosition;

            if (firstPosition == 0 && firstLeft >= listPadding.left && delta >= 0) {

                return true;
            }

            if (firstPosition + childCount == mItemCount && lastRight <= end && delta <= 0) {

                return true;
            }

            final boolean down = incrementalDelta < 0;

            final boolean inTouchMode = isInTouchMode();
            if (inTouchMode) {
                hideSelector();
            }

            final int headerViewsCount = getHeaderViewsCount();
            final int footerViewsStart = mItemCount - getFooterViewsCount();

            int start = 0;
            int count = 0;

            if (down) {
                final int left = listPadding.left - incrementalDelta;
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    if (child.getRight() >= left) {
                        break;
                    } else {
                        count++;
                        int position = firstPosition + i;
                        if (position >= headerViewsCount && position < footerViewsStart) {
                            mRecycler.addScrapView(child);

                        }
                    }
                }
            } else {
                final int right = getWidth() - listPadding.right - incrementalDelta;
                for (int i = childCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);
                    if (child.getLeft() <= right) {
                        break;
                    } else {
                        start = i;
                        count++;
                        int position = firstPosition + i;
                        if (position >= headerViewsCount && position < footerViewsStart) {
                            mRecycler.addScrapView(child);

                        }
                    }
                }
            }


            mMotionViewNewLeft = mMotionViewOriginalLeft + delta;
            mBlockLayoutRequests = true;

            if (count > 0) {
                detachViewsFromParent(start, count);
            }
            offsetChildrenLeftAndRight(incrementalDelta);

            if (down) {
                mFirstPosition += count;
            }


            invalidate();

            final int absIncrementalDelta = Math.abs(incrementalDelta);
            if (spaceAbove < absIncrementalDelta|| spaceBelow < absIncrementalDelta) {
                fillGap(down);
            }

            //TODO dalay here

            if (!inTouchMode && mSelectedPosition != INVALID_POSITION) {
                final int childIndex = mSelectedPosition - mFirstPosition;
                if (childIndex >= 0 && childIndex < getChildCount()) {
                    positionSelector(getChildAt(childIndex));
                }
            }

            mBlockLayoutRequests = false;

            invokeOnItemScrollListener();

            return false;
        }


        @Override
        void centerViewPosition(int position)
        {

            if (mFlingRunnable == null)
            {
                if(!mListDestroyed)
                    mFlingRunnable = getFlingRunnable();
            }

            if (mFlingRunnable != null)
                mFlingRunnable.centerViews(position);
        }


        @Override
        protected CaruselRunnable getCaruselRunnable() {

            return new CaruselRunnable();
        }


        private class HorizontalFlingRunnable extends FlingRunnable {

            protected int 	  mLastFlingX;
            private   CenterViewsRunnable mCentreRunnable;

            protected int 	  mSummDelta  = 0;

            private class CenterViewsRunnable implements Runnable
            {
                int mPosition = 0;


                public void start(int position)
                {

                    mPosition = position;
                    post(this);
                }

                public void stop()
                {
                    removeCallbacks(this);
                }

                @Override
                public void run() {

                    if(mIsCaruselScroller)
                    {
                        int initialX = 0;
                        mLastFlingX = initialX;
                        mTouchMode = TOUCH_MODE_FLING;

                        int scrollOffset = getScrollOffset(mPosition);
                        mNeedScroll = scrollOffset;
                        invalidate();
                        mSummDelta  =  0;

                        smoothScrollBy(scrollOffset, 1000);
                    };
                }

            }

            public void centerViews(final int position)
            {

                if(mCentreRunnable==null)
                {
                    if(!mListDestroyed)
                        mCentreRunnable = new CenterViewsRunnable();
                }

                if(mCentreRunnable!=null)
                {
                    mCentreRunnable.stop();
                    mCentreRunnable.start(position);
                }

            }



            @Override
            void start(int initialVelocity) {
                int initialX = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
                mLastFlingX = initialX;
                mScroller.fling(initialX, 0, initialVelocity, 0,
                        0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
                mTouchMode = TOUCH_MODE_FLING;
                post(this);

                mSummDelta   = 0 ;
                mNeedScroll  = 0 ;

                if (PROFILE_FLINGING) {
                    if (!mFlingProfilingStarted) {

                        mFlingProfilingStarted = true;
                    }
                }
            }

            @Override
            void startScroll(int distance, int duration) {
                int initialX = distance < 0 ? Integer.MAX_VALUE : 0;
                mLastFlingX = initialX;
                mScroller.startScroll(initialX, 0, distance, 0, duration);
                mTouchMode = TOUCH_MODE_FLING;

                post(this);
            }

            public void scrollToPosition()
            {

                final Scroller scroller = mScroller;
                boolean more = scroller.computeScrollOffset();
                final int x = scroller.getCurrX();
                int delta = (mLastFlingX - x)*-1;

                if (delta > 0) {

                    mMotionPosition = mFirstPosition;
                    final View firstView = getChildAt(0);
                    mMotionViewOriginalLeft = firstView.getLeft();

                } else {

                    int offsetToLast = getChildCount() - 1;
                    mMotionPosition = mFirstPosition + offsetToLast;
                    final View lastView = getChildAt(offsetToLast);
                    mMotionViewOriginalLeft = lastView.getLeft();
                }

                mSummDelta+=delta;
                final boolean atEnd = trackMotionScroll(delta, delta);

                if (more && !atEnd)
                {

                    invalidate();
                    mLastFlingX = x;
                    post(this);

                } else
                {
                    mNeedScroll = mNeedScroll -mSummDelta;
                    mSummDelta = 0;

                    if(mNeedScroll!=0)
                    {
                        invalidate();
                        mLastFlingX = x;
                        smoothScrollBy(mNeedScroll, 1000);
                    }
                    else
                    {
                        endFling();

                        if (PROFILE_FLINGING) {
                            if (mFlingProfilingStarted)
                            {
                                mFlingProfilingStarted = false;
                            }
                        }
                    }

                }
            }

            public int getScrollOffset()
            {
                return getScrollOffset(-1);
            }

            public int getScrollOffset(int position)
            {
                if(!mIsCaruselScroller)
                {
                    return 0;
                }

                if (mCaruselRunnable == null)
                {
                    if(!mListDestroyed)
                        mCaruselRunnable = getCaruselRunnable();
                }

                if(mCaruselRunnable!=null)
                {
                    mCaruselRunnable.stop();
                    mCaruselRunnable.start();
                }
                int needScroll = 0;

                if(position==-1)
                {

                    final View lastView  = getChildAt(1);
                    final View firstView = getChildAt(0);

                    if(firstView!=null || lastView!=null)
                    {
                        int first_view_offset = firstView.getLeft();
                        int last_view_offset  = lastView.getLeft();

                        int originalLeft = mSummDelta < 0 ? last_view_offset : first_view_offset;
                        int offset = (mLayoutWidth/2 - originalLeft);
                        int ret = offset-lastView.getWidth()/2;
                        return  ret;
                    }

                }else
                {
                    final View lastView  = getChildAt(position);

                    if(lastView!=null)
                    {
                        int last_view_offset  = lastView.getLeft();
                        int originalLeft =  last_view_offset;

                        int offset = (mLayoutWidth/2 - originalLeft);
                        int ret = offset-lastView.getWidth()/2;
                        return  ret;
                    }
                }


                return 0;
            };

            @Override
            public void run() {

                switch (mTouchMode) {
                    default:
                        return;

                    case TOUCH_MODE_FLING: {
                        if (mItemCount == 0 || getChildCount() == 0) {
                            endFling();
                            return;
                        }

                        if(mIsCaruselScroller && mNeedScroll!=0)
                        {
                            scrollToPosition();
                            return;
                        }

                        final Scroller scroller = mScroller;
                        boolean more = scroller.computeScrollOffset();
                        final int x = scroller.getCurrX();

                        int delta = mLastFlingX - x;
                        if (delta > 0) {

                            mMotionPosition = mFirstPosition;
                            final View firstView = getChildAt(0);
                            mMotionViewOriginalLeft = firstView.getLeft();


                            delta = Math.min(getWidth() - getPaddingRight() - getPaddingLeft() - 1, delta);
                        } else {

                            int offsetToLast = getChildCount() - 1;
                            mMotionPosition = mFirstPosition + offsetToLast;

                            final View lastView = getChildAt(offsetToLast);
                            mMotionViewOriginalLeft = lastView.getLeft();

                            delta = Math.max(-(getWidth() - getPaddingRight() - getPaddingLeft() - 1), delta);
                        }



                        mSummDelta += delta ;



                        final boolean atEnd = trackMotionScroll(delta, delta);

                        if (more && !atEnd) {
                            invalidate();
                            mLastFlingX = x;
                            post(this);
                        } else {
                            endFling();

                            if (PROFILE_FLINGING) {
                                if (mFlingProfilingStarted) {

                                    mFlingProfilingStarted = false;
                                }
                            }
                        }
                        break;
                    }
                }

            }


            private static final int FLYWHEEL_TIMEOUT = 40; // milliseconds

            public void flywheelTouch() {
                if(mCheckFlywheel == null) {
                    mCheckFlywheel = new Runnable() {
                        public void run() {
                            final VelocityTracker vt = mVelocityTracker;
                            if (vt == null) {
                                return;
                            }

                            vt.computeCurrentVelocity(500, mMaximumVelocity);
                            final float xvel = -vt.getXVelocity();

                            if (Math.abs(xvel) >= mMinimumVelocity
                                    && isScrollingInDirection(0, xvel)) {

                                postDelayed(this, FLYWHEEL_TIMEOUT);
                            } else {
                                endFling();
                                mTouchMode = TOUCH_MODE_SCROLL;
                                reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                            }
                        }
                    };
                }
                postDelayed(mCheckFlywheel, FLYWHEEL_TIMEOUT);
            }



            @Override
            public void stop() {

                removeCallbacks(this);

                if(mCentreRunnable!=null)
                {
                    mCentreRunnable.stop();
                    mCentreRunnable = null;

                }

            }



            @Override
            void destroy() {

                if(mCentreRunnable!=null)
                {
                    mCentreRunnable.stop();
                    mCentreRunnable = null;
                }

            }
        }


        class HorizontalPositionScroller extends PositionScroller {
            @Override
            public void run() {
                final int listWidth = getWidth();
                final int firstPos = mFirstPosition;

                switch (mMode) {
                    case MOVE_DOWN_POS: {
                        final int lastViewIndex = getChildCount() - 1;
                        final int lastPos = firstPos + lastViewIndex;

                        if (lastViewIndex < 0) {
                            return;
                        }

                        if (lastPos == mLastSeenPos) {

                            post(this);
                            return;
                        }

                        final View lastView = getChildAt(lastViewIndex);
                        final int lastViewWidth = lastView.getWidth();
                        final int lastViewLeft = lastView.getLeft();
                        final int lastViewPixelsShowing = listWidth - lastViewLeft;
                        final int extraScroll = lastPos < mItemCount - 1 ? mExtraScroll : mListPadding.right;

                        smoothScrollBy(lastViewWidth - lastViewPixelsShowing + extraScroll,
                                mScrollDuration);

                        mLastSeenPos = lastPos;
                        if (lastPos < mTargetPos) {
                            post(this);
                        }
                        break;
                    }

                    case MOVE_DOWN_BOUND: {
                        final int nextViewIndex = 1;
                        final int childCount = getChildCount();

                        if (firstPos == mBoundPos || childCount <= nextViewIndex
                                || firstPos + childCount >= mItemCount) {
                            return;
                        }
                        final int nextPos = firstPos + nextViewIndex;

                        if (nextPos == mLastSeenPos) {
                            post(this);
                            return;
                        }

                        final View nextView = getChildAt(nextViewIndex);
                        final int nextViewWidth = nextView.getWidth();
                        final int nextViewLeft = nextView.getLeft();
                        final int extraScroll = mExtraScroll;
                        if (nextPos < mBoundPos) {
                            smoothScrollBy(Math.max(0, nextViewWidth + nextViewLeft - extraScroll),
                                    mScrollDuration);

                            mLastSeenPos = nextPos;

                            post(this);
                        } else  {
                            if (nextViewLeft > extraScroll) {
                                smoothScrollBy(nextViewLeft - extraScroll, mScrollDuration);
                            }
                        }
                        break;
                    }

                    case MOVE_UP_POS: {
                        if (firstPos == mLastSeenPos) {
                            post(this);
                            return;
                        }

                        final View firstView = getChildAt(0);
                        if (firstView == null) {
                            return;
                        }
                        final int firstViewLeft = firstView.getLeft();
                        final int extraScroll = firstPos > 0 ? mExtraScroll : mListPadding.left;

                        smoothScrollBy(firstViewLeft - extraScroll, mScrollDuration);

                        mLastSeenPos = firstPos;

                        if (firstPos > mTargetPos) {
                            post(this);
                        }
                        break;
                    }

                    case MOVE_UP_BOUND: {
                        final int lastViewIndex = getChildCount() - 2;
                        if (lastViewIndex < 0) {
                            return;
                        }
                        final int lastPos = firstPos + lastViewIndex;

                        if (lastPos == mLastSeenPos) {
                            post(this);
                            return;
                        }

                        final View lastView = getChildAt(lastViewIndex);
                        final int lastViewWidth = lastView.getWidth();
                        final int lastViewLeft = lastView.getLeft();
                        final int lastViewPixelsShowing = listWidth - lastViewLeft;
                        mLastSeenPos = lastPos;
                        if (lastPos > mBoundPos) {
                            smoothScrollBy(-(lastViewPixelsShowing - mExtraScroll), mScrollDuration);
                            post(this);
                        } else {
                            final int right = listWidth - mExtraScroll;
                            final int lastViewRight = lastViewLeft + lastViewWidth;
                            if (right > lastViewRight) {
                                smoothScrollBy(-(right - lastViewRight), mScrollDuration);
                            }
                        }
                        break;
                    }

                    default:
                        break;
                }
            }
        }

    }


    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }



}