package com.cleancoder.learning.toucheshandler.griddynamicview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.GridLayoutAnimationController;
import android.widget.ListAdapter;

import com.cleancoder.learning.toucheshandler.R;
import com.cleancoder.learning.toucheshandler.OrientationHelper;
import com.cleancoder.learning.toucheshandler.OrientationHelpers;
import com.cleancoder.learning.toucheshandler.scrolling.ScrollableToBounds;


public class GridDynamicView extends GridDynamicAbsListView implements ScrollableToBounds
{
	private boolean mBlockVerticalScroll 	 = false;
	private boolean mBlockHorizontallScroll  = false;

	public static final int NO_STRETCH = 0;
	public static final int STRETCH_SPACING = 1;
	public static final int STRETCH_COLUMN_WIDTH = 2;
	public static final int STRETCH_SPACING_UNIFORM = 3;

	public static final int AUTO_FIT = -1;

	private int mNumColumns = AUTO_FIT;
	private int mNumRows = AUTO_FIT;

	private int mHorizontalSpacing = 0;
	private int mRequestedHorizontalSpacing;
	private int mVerticalSpacing = 0;
	private int mRequestedVerticalSpacing;
	private int mStretchMode = STRETCH_COLUMN_WIDTH;
	private int mColumnWidth;
	private int mRequestedColumnWidth;
	private int mRequestedNumColumns;
	private int mRowHeight;
	private int mRequestedRowHeight;
	private int mRequestedNumRows;

	private View mReferenceView = null;
	private View mReferenceViewInSelectedRow = null;

	private int mGravity = Gravity.LEFT;

	private final Rect mTempRect = new Rect();

	protected GridBuilder mGridBuilder = null;

	public GridDynamicView(Context context) {
		super(context);
		setupGridType();
	}

	public GridDynamicView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.gridViewStyle);
	}

	public GridDynamicView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.DynamicGridView, defStyle, 0);

		int hSpacing = a.getDimensionPixelOffset(
				R.styleable.DynamicGridView_horizontalSpacing, 0);
		setHorizontalSpacing(hSpacing);

		int vSpacing = a.getDimensionPixelOffset(
				R.styleable.DynamicGridView_verticalSpacing, 0);
		setVerticalSpacing(vSpacing);

		int index = a.getInt(R.styleable.DynamicGridView_stretchMode, STRETCH_COLUMN_WIDTH);
		if (index >= 0) {
			setStretchMode(index);
		}

		int columnWidth = a.getDimensionPixelOffset(R.styleable.DynamicGridView_columnWidth, -1);
		if (columnWidth > 0) {
			setColumnWidth(columnWidth);
		}

		int rowHeight = a.getDimensionPixelOffset(R.styleable.DynamicGridView_rowHeight, -1);
		if (rowHeight > 0) {
			setRowHeight(rowHeight);
		}

		int numColumns = a.getInt(R.styleable.DynamicGridView_numColumns, 1);
		setNumColumns(numColumns);

		int numRows = a.getInt(R.styleable.DynamicGridView_numRows, 1);
		setNumRows(numRows);

		index = a.getInt(R.styleable.DynamicGridView_gravity, -1);
		if (index >= 0) {
			setGravity(index);
		}

		a.recycle();

		setupGridType();
	}

	private void setupGridType() {
		if (mScrollVertically) {
			mGridBuilder = new VerticalGridBuilder();
		} else {
			mGridBuilder = new HorizontalGridBuilder();
		}
	}

	@Override
	public ListAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setAdapter(ListAdapter adapter)
	{
		if (null != mAdapter) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}

		resetList();
		mRecycler.clear();
		mAdapter = adapter;

		mOldSelectedPosition = INVALID_POSITION;
		mOldSelectedRowId = INVALID_ROW_ID;

		if (mAdapter != null) {
			mOldItemCount = mItemCount;
			mItemCount = mAdapter.getCount();
			mDataChanged = true;
			checkFocus();

			mDataSetObserver = new AdapterDataSetObserver();
			mAdapter.registerDataSetObserver(mDataSetObserver);

			mRecycler.setViewTypeCount(mAdapter.getViewTypeCount());

			int position;
			if (mStackFromBottom) {
				position = lookForSelectablePosition(mItemCount - 1, false);
			} else {
				position = lookForSelectablePosition(0, true);
			}
			setSelectedPositionInt(position);
			setNextSelectedPositionInt(position);
			checkSelectionChanged();
		} else {
			checkFocus();
			checkSelectionChanged();
		}

		requestLayout();
	}

	@Override
	int lookForSelectablePosition(int position, boolean lookDown) {
		final ListAdapter adapter = mAdapter;
		if (adapter == null || isInTouchMode()) {
			return INVALID_POSITION;
		}

		if (position < 0 || position >= mItemCount) {
			return INVALID_POSITION;
		}
		return position;
	}


	@Override
	void fillGap(boolean down)
	{
		mGridBuilder.fillGap(down);
	}

	@Override
	int findMotionRowY(int y) {
		final int childCount = getChildCount();
		if (childCount > 0) {

			final int numColumns = mNumColumns;
			if (!mStackFromBottom) {
				for (int i = 0; i < childCount; i += numColumns) {
					if (y <= getChildAt(i).getBottom()) {
						return mFirstPosition + i;
					}
				}
			} else {
				for (int i = childCount - 1; i >= 0; i -= numColumns) {
					if (y >= getChildAt(i).getTop()) {
						return mFirstPosition + i;
					}
				}
			}
		}
		return INVALID_POSITION;
	}

	@Override
	int findMotionRowX(int x) {
		final int childCount = getChildCount();
		if (childCount > 0) {
			final int numRows = mNumRows;
			if (!mStackFromBottom) {
				for (int i = 0; i < childCount; i += numRows) {
					if (x <= getChildAt(i).getRight()) {
						return mFirstPosition + i;
					}
				}
			} else {
				for (int i = childCount - 1; i >= 0; i -= numRows) {
					if (x >= getChildAt(i).getLeft()) {
						return mFirstPosition + i;
					}
				}
			}
		}
		return INVALID_POSITION;
	}


    @Override
    public void requestLayout() {
        super.requestLayout();
    }

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if ((mScrollVertically && !(mGridBuilder instanceof VerticalGridBuilder))
			|| (!mScrollVertically && !(mGridBuilder instanceof HorizontalGridBuilder)) ) {
			setupGridType();
		}
		// Sets up mListPadding
		mGridBuilder.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void attachLayoutAnimationParameters(View child,
			ViewGroup.LayoutParams params, int index, int count) {

		GridLayoutAnimationController.AnimationParameters animationParams =
			(GridLayoutAnimationController.AnimationParameters) params.layoutAnimationParameters;

		if (animationParams == null) {
			animationParams = new GridLayoutAnimationController.AnimationParameters();
			params.layoutAnimationParameters = animationParams;
		}

		animationParams.count = count;
		animationParams.index = index;
		animationParams.columnsCount = mNumColumns;
		animationParams.rowsCount = count / mNumColumns;

		if (!mStackFromBottom) {
			animationParams.column = index % mNumColumns;
			animationParams.row = index / mNumColumns;
		} else {
			final int invertedIndex = count - 1 - index;

			animationParams.column = mNumColumns - 1 - (invertedIndex % mNumColumns);
			animationParams.row = animationParams.rowsCount - 1 - invertedIndex / mNumColumns;
		}
	}

	public void updateData()
	{
		mGridBuilder.layoutChildren(true);
	}


	@Override
	protected void layoutChildren()
	{

		final boolean blockLayoutRequests = mBlockLayoutRequests;
		if (!blockLayoutRequests) {
			mBlockLayoutRequests = true;
		}

		try {
			super.layoutChildren();

			invalidate();

			if (mAdapter == null) {
				resetList();
				invokeOnItemScrollListener();
				return;
			}

			mGridBuilder.layoutChildren();

		} finally {

			if (!blockLayoutRequests) {
				mBlockLayoutRequests = false;
			}
		}
	}


	@Override
	public void setSelection(int position) {
		if (!isInTouchMode()) {
			setNextSelectedPositionInt(position);
		} else {
			mResurrectToPosition = position;
		}
		mLayoutMode = LAYOUT_SET_SELECTION;
		requestLayout();
	}

	@Override
	void setSelectionInt(int position) {
		mGridBuilder.setSelectionInt(position);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return commonKey(keyCode, 1, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return commonKey(keyCode, repeatCount, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return commonKey(keyCode, 1, event);
	}

	private boolean commonKey(int keyCode, int count, KeyEvent event) {
		if (mAdapter == null) {
			return false;
		}

		if (mDataChanged) {
			layoutChildren();
		}

		boolean handled = false;
		int action = event.getAction();

		if (action != KeyEvent.ACTION_UP) {
			if (mSelectedPosition < 0) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
				case KeyEvent.KEYCODE_DPAD_CENTER:
				case KeyEvent.KEYCODE_SPACE:
				case KeyEvent.KEYCODE_ENTER:
					resurrectSelection();
					return true;
				}
			}

			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				if (!event.isAltPressed()) {
					handled = mGridBuilder.arrowScroll(FOCUS_LEFT);
				} else {
					handled = fullScroll(FOCUS_UP);
				}
				break;

			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if (!event.isAltPressed()) {
					handled = mGridBuilder.arrowScroll(FOCUS_RIGHT);
				} else {
					handled = fullScroll(FOCUS_DOWN);
				}
				break;

			case KeyEvent.KEYCODE_DPAD_UP:
				if (!event.isAltPressed()) {
					handled = mGridBuilder.arrowScroll(FOCUS_UP);

				} else {
					handled = fullScroll(FOCUS_UP);
				}
				break;

			case KeyEvent.KEYCODE_DPAD_DOWN:
				if (!event.isAltPressed()) {
					handled = mGridBuilder.arrowScroll(FOCUS_DOWN);
				} else {
					handled = fullScroll(FOCUS_DOWN);
				}
				break;

			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER: {


				return true;
			}

			case KeyEvent.KEYCODE_SPACE:

				if (!event.isShiftPressed()) {
					handled = pageScroll(FOCUS_DOWN);
				} else {
					handled = pageScroll(FOCUS_UP);
				}

				break;
			}
		}

		if (handled) {
			return true;
		} else {
			switch (action) {
			case KeyEvent.ACTION_DOWN:
				return super.onKeyDown(keyCode, event);
			case KeyEvent.ACTION_UP:
				return super.onKeyUp(keyCode, event);
			case KeyEvent.ACTION_MULTIPLE:
				return super.onKeyMultiple(keyCode, count, event);
			default:
				return false;
			}
		}
	}


	boolean pageScroll(int direction) {
		int nextPage = -1;

		if (direction == FOCUS_UP) {
			nextPage = Math.max(0, mSelectedPosition - getChildCount() - 1);
		} else if (direction == FOCUS_DOWN) {
			nextPage = Math.min(mItemCount - 1, mSelectedPosition + getChildCount() - 1);
		}

		if (nextPage >= 0) {
			setSelectionInt(nextPage);
			invokeOnItemScrollListener();

			return true;
		}

		return false;
	}


	public boolean fullScroll(int direction) {
		boolean moved = false;
		if (direction == FOCUS_UP) {
			mLayoutMode = LAYOUT_SET_SELECTION;
			setSelectionInt(0);
			invokeOnItemScrollListener();
			moved = true;
		} else if (direction == FOCUS_DOWN) {
			mLayoutMode = LAYOUT_SET_SELECTION;
			setSelectionInt(mItemCount - 1);
			invokeOnItemScrollListener();
			moved = true;
		}

		if (moved) {
			//awakenScrollBars();
		}

		return moved;
	}



	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		
		int closestChildIndex = -1;
		if (gainFocus && previouslyFocusedRect != null) {
			previouslyFocusedRect.offset(getScrollX(), getScrollY());

			Rect otherRect = mTempRect;
			int minDistance = Integer.MAX_VALUE;
			final int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				// only consider view's on appropriate edge of grid
				if (!mGridBuilder.isCandidateSelection(i, direction)) {
					continue;
				}

				final View other = getChildAt(i);
				other.getDrawingRect(otherRect);
				offsetDescendantRectToMyCoords(other, otherRect);
				int distance = getDistance(previouslyFocusedRect, otherRect, direction);

				if (distance < minDistance) {
					minDistance = distance;
					closestChildIndex = i;
				}
			}
		}

		if (closestChildIndex >= 0) {
			setSelection(closestChildIndex + mFirstPosition);
		} else {
			requestLayout();
		}
	}

	public void setGravity(int gravity) {
		if (mGravity != gravity) {
			mGravity = gravity;
			requestLayoutIfNecessary();
		}
	}


	public void setHorizontalSpacing(int horizontalSpacing) {
		if (horizontalSpacing != mRequestedHorizontalSpacing) {
			mRequestedHorizontalSpacing = horizontalSpacing;
			requestLayoutIfNecessary();
		}
	}


	public void setVerticalSpacing(int verticalSpacing) {
		if (verticalSpacing != mRequestedVerticalSpacing) {
			mRequestedVerticalSpacing = verticalSpacing;
			requestLayoutIfNecessary();
		}
	}

	public void setStretchMode(int stretchMode) {
		if (stretchMode != mStretchMode) {
			mStretchMode = stretchMode;
			requestLayoutIfNecessary();
		}
	}

	public int getStretchMode() {
		return mStretchMode;
	}

	public void setColumnWidth(int columnWidth) {
		if (columnWidth != mRequestedColumnWidth) {
			mRequestedColumnWidth = columnWidth;
			requestLayoutIfNecessary();
		}
	}


	public void setRowHeight(int rowHeight) {
		if (rowHeight != mRequestedRowHeight) {
			mRequestedRowHeight = rowHeight;
			requestLayoutIfNecessary();
		}
	}

	public void setNumColumns(int numColumns) {
		if (numColumns != mRequestedNumColumns) {
			mRequestedNumColumns = numColumns;
			requestLayoutIfNecessary();
		}
	}


	public void setNumRows(int numRows) {
		if (numRows != mRequestedNumRows) {
			mRequestedNumRows = numRows;
			requestLayoutIfNecessary();
		}
	}



	@Override
	protected int computeVerticalScrollExtent()
	{
		final int count = getChildCount();
		if (count > 0 && mScrollVertically) {
			final int numColumns = mNumColumns;
			final int rowCount = (count + numColumns - 1) / numColumns;

			int extent = rowCount * 100;

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
		}
		return 0;
	}

	@Override
	protected int computeVerticalScrollOffset() {
		if (mFirstPosition >= 0 && getChildCount() > 0 && mScrollVertically) {
			final View view = getChildAt(0);
			final int top = view.getTop();
			int height = view.getHeight();
			if (height > 0) {
				final int numColumns = mNumColumns;
				final int whichRow = mFirstPosition / numColumns;
				final int rowCount = (mItemCount + numColumns - 1) / numColumns;
				return Math.max(whichRow * 100 - (top * 100) / height +
                        (int) ((float) getScrollY() / getHeight() * rowCount * 100), 0);
			}
		}
		return 0;
	}

	@Override
	protected int computeVerticalScrollRange() {
		// TODO: Account for vertical spacing too
		if (!mScrollVertically) {
			return 0;
		}
		final int numColumns = mNumColumns;
		final int rowCount = (mItemCount + numColumns - 1) / numColumns;
		return Math.max(rowCount * 100, 0);
	}

	@Override
	protected int computeHorizontalScrollExtent() {
		final int count = getChildCount();
		if (count > 0 && !mScrollVertically) {
			final int numRows = mNumRows;
			final int columnCount = (count + numRows - 1) / numRows;

			int extent = columnCount * 100;

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
		}
		return 0;
	}

	@Override
	protected int computeHorizontalScrollOffset() {
		if (mFirstPosition >= 0 && getChildCount() > 0 && !mScrollVertically) {
			final View view = getChildAt(0);
			final int left = view.getLeft();
			int width = view.getWidth();
			if (width > 0) {
				final int numRows = mNumRows;
				final int whichColumn = mFirstPosition / numRows;
				final int columnCount = (mItemCount + numRows - 1) / numRows;
				return Math.max(whichColumn * 100 - (left * 100) / width +
                        (int) ((float) getScrollX() / getWidth() * columnCount * 100), 0);
			}
		}
		return 0;
	}

	@Override
	protected int computeHorizontalScrollRange() {
		// TODO: Account for horizontal spacing too
		if (mScrollVertically) {
			return 0;
		}
		final int numRows = mNumRows;
		final int columnCount = (mItemCount + numRows - 1) / numRows;
		return Math.max(columnCount * 100, 0);
	}


	private abstract class GridBuilder {

		protected abstract View makeAndAddView(int position, int y, boolean flow, int childrenLeft,
				boolean selected, int where);

		protected abstract void fillGap(boolean down);


		protected abstract void onMeasure(int widthMeasureSpec, int heightMeasureSpec);

		protected abstract void layoutChildren();
		protected abstract void layoutChildren(boolean updated);

		protected abstract void setSelectionInt(int position);

		protected abstract boolean arrowScroll(int direction);

		protected abstract boolean isCandidateSelection(int childIndex, int direction);
	}

	private class VerticalGridBuilder extends GridBuilder {


		@Override
		protected View makeAndAddView(int position, int y, boolean flow, int childrenLeft,
				boolean selected, int where) {
			View child;

			if (!mDataChanged) {

				child = mRecycler.getActiveView(position);
				if (child != null) {

					setupChild(child, position, y, flow, childrenLeft, selected, true, where);

					return child;
				}
			}

			child = obtainView(position, mIsScrap);

			setupChild(child, position, y, flow, childrenLeft, selected, mIsScrap[0], where);

			return child;
		}

		@Override
		protected void fillGap(boolean down) {

			final int numColumns = mNumColumns;
			final int verticalSpacing = mVerticalSpacing;

			final int count = getChildCount();

			if (down) {
				final int startOffset = count > 0 ?
						getChildAt(count - 1).getBottom() + verticalSpacing : getListPaddingTop();
						int position = mFirstPosition + count;
						if (mStackFromBottom) {
							position += numColumns - 1;
						}
						fillDown(position, startOffset);
						correctTooHigh(numColumns, verticalSpacing, getChildCount());
			} else {
				final int startOffset = count > 0 ?
						getChildAt(0).getTop() - verticalSpacing : getHeight() - getListPaddingBottom();
						int position = mFirstPosition;
						if (!mStackFromBottom) {
							position -= numColumns;
						} else {
							position--;
						}
						fillUp(position, startOffset);
						correctTooLow(numColumns, verticalSpacing, getChildCount());
			}
		}

		private View fillDown(int pos, int nextTop) {

			View selectedView = null;

			final int end = (getBottom() - getTop()) - mListPadding.bottom;

			while (nextTop < end && pos < mItemCount) {
				View temp = makeRow(pos, nextTop, true);
				if (temp != null) {
					selectedView = temp;
				}


				nextTop = mReferenceView.getBottom() + mVerticalSpacing;

				pos += mNumColumns;
			}

			return selectedView;
		}

		private View makeRow(int startPos, int y, boolean flow) {

			final int columnWidth = mColumnWidth;
			final int horizontalSpacing = mHorizontalSpacing;

			int last;
			int nextLeft = mListPadding.left +
			((mStretchMode == STRETCH_SPACING_UNIFORM) ? horizontalSpacing : 0);

			if (!mStackFromBottom) {
				last = Math.min(startPos + mNumColumns, mItemCount);
			} else {
				last = startPos + 1;
				startPos = Math.max(0, startPos - mNumColumns + 1);

				if (last - startPos < mNumColumns) {
					nextLeft += (mNumColumns - (last - startPos)) * (columnWidth + horizontalSpacing);
				}
			}

			View selectedView = null;

			final boolean hasFocus = shouldShowSelector();
			final boolean inClick = touchModeDrawsInPressedState();
			final int selectedPosition = mSelectedPosition;

			View child = null;
			for (int pos = startPos; pos < last; pos++) {

				boolean selected = pos == selectedPosition;

				final int where = flow ? -1 : pos - startPos;
				child = makeAndAddView(pos, y, flow, nextLeft, selected, where);

				nextLeft += columnWidth;
				if (pos < last - 1) {
					nextLeft += horizontalSpacing;
				}

				if (selected && (hasFocus || inClick)) {
					selectedView = child;
				}
			}

			mReferenceView = child;

			if (selectedView != null) {
				mReferenceViewInSelectedRow = mReferenceView;
			}

			return selectedView;
		}

		private View fillUp(int pos, int nextBottom) {

			View selectedView = null;

			final int end = mListPadding.top;

			while (nextBottom > end && pos >= 0) {

				View temp = makeRow(pos, nextBottom, false);
				if (temp != null) {
					selectedView = temp;
				}

				nextBottom = mReferenceView.getTop() - mVerticalSpacing;

				mFirstPosition = pos;

				pos -= mNumColumns;
			}

			if (mStackFromBottom) {
				mFirstPosition = Math.max(0, pos + 1);
			}

			return selectedView;
		}

		private View fillFromTop(int nextTop) {

			mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
			mFirstPosition = Math.min(mFirstPosition, mItemCount - 1);
			if (mFirstPosition < 0) {
				mFirstPosition = 0;
			}
			mFirstPosition -= mFirstPosition % mNumColumns;
			return fillDown(mFirstPosition, nextTop);
		}

		private View fillFromBottom(int lastPosition, int nextBottom) {

			lastPosition = Math.max(lastPosition, mSelectedPosition);
			lastPosition = Math.min(lastPosition, mItemCount - 1);

			final int invertedPosition = mItemCount - 1 - lastPosition;
			lastPosition = mItemCount - 1 - (invertedPosition - (invertedPosition % mNumColumns));

			return fillUp(lastPosition, nextBottom);
		}

		private View fillSelection(int childrenTop, int childrenBottom) {

			final int selectedPosition = reconcileSelectedPosition();
			final int numColumns = mNumColumns;
			final int verticalSpacing = mVerticalSpacing;

			int rowStart;
			int rowEnd = -1;

			if (!mStackFromBottom) {
				rowStart = selectedPosition - (selectedPosition % numColumns);
			} else {
				final int invertedSelection = mItemCount - 1 - selectedPosition;

				rowEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
				rowStart = Math.max(0, rowEnd - numColumns + 1);
			}

			final int fadingEdgeLength = getVerticalFadingEdgeLength();
			final int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, rowStart);

			final View sel = makeRow(mStackFromBottom ? rowEnd : rowStart, topSelectionPixel, true);
			mFirstPosition = rowStart;

			final View referenceView = mReferenceView;

			if (!mStackFromBottom) {
				fillDown(rowStart + numColumns, referenceView.getBottom() + verticalSpacing);
				pinToBottom(childrenBottom);
				fillUp(rowStart - numColumns, referenceView.getTop() - verticalSpacing);
				adjustViewsUpOrDown();
			} else {
				final int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom,
						fadingEdgeLength, numColumns, rowStart);
				final int offset = bottomSelectionPixel - referenceView.getBottom();
				offsetChildrenTopAndBottom(offset);
				fillUp(rowStart - 1, referenceView.getTop() - verticalSpacing);
				pinToTop(childrenTop);
				fillDown(rowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
				adjustViewsUpOrDown();
			}

			return sel;
		}



		private View fillSpecific(int position, int top) {

			final int numColumns = mNumColumns;

			int motionRowStart;
			int motionRowEnd = -1;

			if (!mStackFromBottom) {
				motionRowStart = position - (position % numColumns);
			} else {
				final int invertedSelection = mItemCount - 1 - position;

				motionRowEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
				motionRowStart = Math.max(0, motionRowEnd - numColumns + 1);
			}

			final View temp = makeRow(mStackFromBottom ? motionRowEnd : motionRowStart, top, true);

			mFirstPosition = motionRowStart;

			final View referenceView = mReferenceView;

			if (referenceView == null) {
				return null;
			}

			final int verticalSpacing = mVerticalSpacing;

			View above;
			View below;

			if (!mStackFromBottom) {
				above = fillUp(motionRowStart - numColumns, referenceView.getTop() - verticalSpacing);
				adjustViewsUpOrDown();
				below = fillDown(motionRowStart + numColumns, referenceView.getBottom() + verticalSpacing);

				final int childCount = getChildCount();
				if (childCount > 0) {
					correctTooHigh(numColumns, verticalSpacing, childCount);
				}
			} else {
				below = fillDown(motionRowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
				adjustViewsUpOrDown();
				above = fillUp(motionRowStart - 1, referenceView.getTop() - verticalSpacing);

				final int childCount = getChildCount();
				if (childCount > 0) {
					correctTooLow(numColumns, verticalSpacing, childCount);
				}
			}

			if (temp != null) {
				return temp;
			} else if (above != null) {
				return above;
			} else {
				return below;
			}
		}

		private void correctTooHigh(int numColumns, int verticalSpacing, int childCount) {

			final int lastPosition = mFirstPosition + childCount - 1;
			if (lastPosition == mItemCount - 1 && childCount > 0) {

				final View lastChild = getChildAt(childCount - 1);

				final int lastBottom = lastChild.getBottom();
				final int end = (getBottom() - getTop()) - mListPadding.bottom;

				int bottomOffset = end - lastBottom;

				final View firstChild = getChildAt(0);
				final int firstTop = firstChild.getTop();

				if (bottomOffset > 0 && (mFirstPosition > 0 || firstTop < mListPadding.top))  {
					if (mFirstPosition == 0) {

						bottomOffset = Math.min(bottomOffset, mListPadding.top - firstTop);
					}

					offsetChildrenTopAndBottom(bottomOffset);
					if (mFirstPosition > 0) {

						fillUp(mFirstPosition - (mStackFromBottom ? 1 : numColumns),
								firstChild.getTop() - verticalSpacing);

						adjustViewsUpOrDown();
					}
				}
			}
		}

		private void correctTooLow(int numColumns, int verticalSpacing, int childCount) {

			if (mFirstPosition == 0 && childCount > 0) {

				final View firstChild = getChildAt(0);
				final int firstTop = firstChild.getTop();
				final int start = mListPadding.top;
				final int end = (getBottom() - getTop()) - mListPadding.bottom;

				int topOffset = firstTop - start;
				final View lastChild = getChildAt(childCount - 1);
				final int lastBottom = lastChild.getBottom();
				final int lastPosition = mFirstPosition + childCount - 1;


				if (topOffset > 0 && (lastPosition < mItemCount - 1 || lastBottom > end))  {
					if (lastPosition == mItemCount - 1 ) {

						topOffset = Math.min(topOffset, lastBottom - end);
					}


					offsetChildrenTopAndBottom(-topOffset);
					if (lastPosition < mItemCount - 1) {

						fillDown(lastPosition + (!mStackFromBottom ? 1 : numColumns),
								lastChild.getBottom() + verticalSpacing);
						adjustViewsUpOrDown();
					}
				}
			}
		}

		private View fillFromSelection(int selectedTop, int childrenTop, int childrenBottom) {

			final int fadingEdgeLength = getVerticalFadingEdgeLength();
			final int selectedPosition = mSelectedPosition;
			final int numColumns = mNumColumns;
			final int verticalSpacing = mVerticalSpacing;

			int rowStart;
			int rowEnd = -1;

			if (!mStackFromBottom) {
				rowStart = selectedPosition - (selectedPosition % numColumns);
			} else {
				int invertedSelection = mItemCount - 1 - selectedPosition;

				rowEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
				rowStart = Math.max(0, rowEnd - numColumns + 1);
			}

			View sel;
			View referenceView;

			int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, rowStart);
			int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom, fadingEdgeLength,
					numColumns, rowStart);

			sel = makeRow(mStackFromBottom ? rowEnd : rowStart, selectedTop, true);

			mFirstPosition = rowStart;

			referenceView = mReferenceView;
			adjustForTopFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
			adjustForBottomFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);

			if (!mStackFromBottom) {
				fillUp(rowStart - numColumns, referenceView.getTop() - verticalSpacing);
				adjustViewsUpOrDown();
				fillDown(rowStart + numColumns, referenceView.getBottom() + verticalSpacing);
			} else {
				fillDown(rowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
				adjustViewsUpOrDown();
				fillUp(rowStart - 1, referenceView.getTop() - verticalSpacing);
			}


			return sel;
		}


		private int getBottomSelectionPixel(int childrenBottom, int fadingEdgeLength,
				int numColumns, int rowStart) {

			int bottomSelectionPixel = childrenBottom;
			if (rowStart + numColumns - 1 < mItemCount - 1) {
				bottomSelectionPixel -= fadingEdgeLength;
			}
			return bottomSelectionPixel;
		}


		private int getTopSelectionPixel(int childrenTop, int fadingEdgeLength, int rowStart) {

			int topSelectionPixel = childrenTop;
			if (rowStart > 0) {
				topSelectionPixel += fadingEdgeLength;
			}
			return topSelectionPixel;
		}

		private void adjustForBottomFadingEdge(View childInSelectedRow,
				int topSelectionPixel, int bottomSelectionPixel) {

			if (childInSelectedRow.getBottom() > bottomSelectionPixel) {


				int spaceAbove = childInSelectedRow.getTop() - topSelectionPixel;

				int spaceBelow = childInSelectedRow.getBottom() - bottomSelectionPixel;
				int offset = Math.min(spaceAbove, spaceBelow);

				offsetChildrenTopAndBottom(-offset);
			}
		}

		private void adjustForTopFadingEdge(View childInSelectedRow,
				int topSelectionPixel, int bottomSelectionPixel)
		{

			if (childInSelectedRow.getTop() < topSelectionPixel) {

				int spaceAbove = topSelectionPixel - childInSelectedRow.getTop();
				int spaceBelow = bottomSelectionPixel - childInSelectedRow.getBottom();
				int offset = Math.min(spaceAbove, spaceBelow);

				offsetChildrenTopAndBottom(offset);
			}
		}

		private void determineColumns(int availableSpace) {
			final int requestedHorizontalSpacing = mRequestedHorizontalSpacing;
			final int stretchMode = mStretchMode;
			final int requestedColumnWidth = mRequestedColumnWidth;
			mVerticalSpacing = mRequestedVerticalSpacing;

			if (mRequestedNumColumns == AUTO_FIT) {
				if (requestedColumnWidth > 0) {

					mNumColumns = (availableSpace + requestedHorizontalSpacing) /
					(requestedColumnWidth + requestedHorizontalSpacing);
				} else {

					mNumColumns = 2;
				}
			} else {

				mNumColumns = mRequestedNumColumns;
			}

			if (mNumColumns <= 0) {
				mNumColumns = 1;
			}

			switch (stretchMode) {
			case NO_STRETCH:

				mColumnWidth = requestedColumnWidth;
				mHorizontalSpacing = requestedHorizontalSpacing;
				break;

			default:
				int spaceLeftOver = 0;
				switch (stretchMode) {
				case STRETCH_COLUMN_WIDTH:

					spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) -
					((mNumColumns - 1) * requestedHorizontalSpacing);
					mColumnWidth = requestedColumnWidth + spaceLeftOver / mNumColumns;
					mHorizontalSpacing = requestedHorizontalSpacing;
					break;

				case STRETCH_SPACING:

					spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) -
					((mNumColumns - 1) * requestedHorizontalSpacing);
					mColumnWidth = requestedColumnWidth;
					if (mNumColumns > 1) {
						mHorizontalSpacing = requestedHorizontalSpacing +
						spaceLeftOver / (mNumColumns - 1);
					} else {
						mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver;
					}
					break;

				case STRETCH_SPACING_UNIFORM:

					spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) -
					((mNumColumns + 1) * requestedHorizontalSpacing);
					mColumnWidth = requestedColumnWidth;
					if (mNumColumns > 1) {
						mHorizontalSpacing = requestedHorizontalSpacing +
						spaceLeftOver / (mNumColumns + 1);
					} else {
						mHorizontalSpacing = ((requestedHorizontalSpacing * 2) + spaceLeftOver) / 2;
					}
					break;
				}

				break;
			}
		}

		private View moveSelection(int delta, int childrenTop, int childrenBottom) {

			final int fadingEdgeLength = getVerticalFadingEdgeLength();
			final int selectedPosition = mSelectedPosition;
			final int numColumns = mNumColumns;
			final int verticalSpacing = mVerticalSpacing;

			int oldRowStart;
			int rowStart;
			int rowEnd = -1;

			if (!mStackFromBottom) {
				oldRowStart = (selectedPosition - delta) - ((selectedPosition - delta) % numColumns);

				rowStart = selectedPosition - (selectedPosition % numColumns);
			} else {
				int invertedSelection = mItemCount - 1 - selectedPosition;

				rowEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
				rowStart = Math.max(0, rowEnd - numColumns + 1);

				invertedSelection = mItemCount - 1 - (selectedPosition - delta);
				oldRowStart = mItemCount - 1 - (invertedSelection - (invertedSelection % numColumns));
				oldRowStart = Math.max(0, oldRowStart - numColumns + 1);
			}

			final int rowDelta = rowStart - oldRowStart;

			final int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, rowStart);
			final int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom, fadingEdgeLength,
					numColumns, rowStart);

			mFirstPosition = rowStart;

			View sel;
			View referenceView;

			if (rowDelta > 0) {

				final int oldBottom = mReferenceViewInSelectedRow == null ? 0 :
					mReferenceViewInSelectedRow.getBottom();

				sel = makeRow(mStackFromBottom ? rowEnd : rowStart, oldBottom + verticalSpacing, true);
				referenceView = mReferenceView;

				adjustForBottomFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
			} else if (rowDelta < 0) {

				final int oldTop = mReferenceViewInSelectedRow == null ?
						0 : mReferenceViewInSelectedRow .getTop();

				sel = makeRow(mStackFromBottom ? rowEnd : rowStart, oldTop - verticalSpacing, false);
				referenceView = mReferenceView;

				adjustForTopFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
			} else {

				final int oldTop = mReferenceViewInSelectedRow == null ?
						0 : mReferenceViewInSelectedRow .getTop();

				sel = makeRow(mStackFromBottom ? rowEnd : rowStart, oldTop, true);
				referenceView = mReferenceView;
			}

			if (!mStackFromBottom)
			{
				fillUp(rowStart - numColumns, referenceView.getTop() - verticalSpacing);
				adjustViewsUpOrDown();
				fillDown(rowStart + numColumns, referenceView.getBottom() + verticalSpacing);

			} else {

				fillDown(rowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
				adjustViewsUpOrDown();
				fillUp(rowStart - 1, referenceView.getTop() - verticalSpacing);

			}

			return sel;
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int widthMode = MeasureSpec.getMode(widthMeasureSpec);
			int heightMode = MeasureSpec.getMode(heightMeasureSpec);
			int widthSize = MeasureSpec.getSize(widthMeasureSpec);
			int heightSize = MeasureSpec.getSize(heightMeasureSpec);

			if (widthMode == MeasureSpec.UNSPECIFIED) {
				if (mColumnWidth > 0) {
					widthSize = mColumnWidth + mListPadding.left + mListPadding.right;
				} else {
					widthSize = mListPadding.left + mListPadding.right;
				}
				widthSize += getVerticalScrollbarWidth();
			}

			int childWidth = widthSize - mListPadding.left - mListPadding.right;
			determineColumns(childWidth);

			int childHeight = 0;

			mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
			final int count = mItemCount;
			if (count > 0) {
				final View child = obtainView(0, mIsScrap);

				GridDynamicAbsListView.LayoutParams p = (GridDynamicAbsListView.LayoutParams)child.getLayoutParams();
				if (p == null) {
					p = new GridDynamicAbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT, 0);
					child.setLayoutParams(p);
				}
				p.viewType = mAdapter.getItemViewType(0);
				p.forceAdd = true;

				int childHeightSpec = getChildMeasureSpec(
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
				int childWidthSpec = getChildMeasureSpec(
						MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY), 0, p.width);
				child.measure(childWidthSpec, childHeightSpec);

				childHeight = child.getMeasuredHeight();

				if (mRecycler.shouldRecycleViewType(p.viewType)) {
					mRecycler.addScrapView(child);
				}
			}

			if (heightMode == MeasureSpec.UNSPECIFIED) {
				heightSize = mListPadding.top + mListPadding.bottom + childHeight +
				getVerticalFadingEdgeLength() * 2;
			}

			if (heightMode == MeasureSpec.AT_MOST) {
				int ourSize =  mListPadding.top + mListPadding.bottom;

				final int numColumns = mNumColumns;
				for (int i = 0; i < count; i += numColumns) {
					ourSize += childHeight;
					if (i + numColumns < count) {
						ourSize += mVerticalSpacing;
					}
					if (ourSize >= heightSize) {
						ourSize = heightSize;
						break;
					}
				}
				heightSize = ourSize;
			}

			setMeasuredDimension(widthSize, heightSize);
			mWidthMeasureSpec = widthMeasureSpec;

		}

		@Override
		protected void layoutChildren()
		{

			final int childrenTop = mListPadding.top;
			final int childrenBottom = getBottom() - getTop() - mListPadding.bottom;

			int childCount = getChildCount();
			int index;
			int delta = 0;

			View sel;
			View oldSel = null;
			View oldFirst = null;
			View newSel = null;


			switch (mLayoutMode)
			{
				case LAYOUT_SET_SELECTION:
					index = mNextSelectedPosition - mFirstPosition;
					if (index >= 0 && index < childCount) {
						newSel = getChildAt(index);
					}
					break;
				case LAYOUT_FORCE_TOP:
				case LAYOUT_FORCE_BOTTOM:
				case LAYOUT_SPECIFIC:
				case LAYOUT_SYNC:
					break;
				case LAYOUT_MOVE_SELECTION:
					if (mNextSelectedPosition >= 0) {
						delta = mNextSelectedPosition - mSelectedPosition;
					}
					break;
				default:

					index = mSelectedPosition - mFirstPosition;
					if (index >= 0 && index < childCount) {
						oldSel = getChildAt(index);
					}

					oldFirst = getChildAt(0);
			}


			boolean dataChanged = mDataChanged;
			if (dataChanged) {
				handleDataChanged();
			}

			if (mItemCount == 0) {
				resetList();
				invokeOnItemScrollListener();
				return;
			}

			setSelectedPositionInt(mNextSelectedPosition);

			final int firstPosition = mFirstPosition;
			final RecycleBin recycleBin = mRecycler;

			if (dataChanged)
			{
				for (int i = 0; i < childCount; i++) {
					recycleBin.addScrapView(getChildAt(i));
				}
			} else {
				recycleBin.fillActiveViews(childCount, firstPosition);
			}

			detachAllViewsFromParent();

			switch (mLayoutMode) {
			case LAYOUT_SET_SELECTION:
				if (newSel != null) {
					sel = fillFromSelection(newSel.getTop(), childrenTop, childrenBottom);
				} else {
					sel = fillSelection(childrenTop, childrenBottom);
				}
				break;
			case LAYOUT_FORCE_TOP:
				mFirstPosition = 0;
				sel = fillFromTop(childrenTop);
				adjustViewsUpOrDown();
				break;
			case LAYOUT_FORCE_BOTTOM:
				sel = fillUp(mItemCount - 1, childrenBottom);
				adjustViewsUpOrDown();
				break;
			case LAYOUT_SPECIFIC:
				sel = fillSpecific(mSelectedPosition, mSpecificTop);
				break;
			case LAYOUT_SYNC:
				sel = fillSpecific(mSyncPosition, mSpecificTop);
				break;
			case LAYOUT_MOVE_SELECTION:

				sel = moveSelection(delta, childrenTop, childrenBottom);
				break;
			default:
				if (childCount == 0) {
					if (!mStackFromBottom) {
						setSelectedPositionInt(mAdapter == null || isInTouchMode() ?
								INVALID_POSITION : 0);
						sel = fillFromTop(childrenTop);
					} else {
						final int last = mItemCount - 1;
						setSelectedPositionInt(mAdapter == null || isInTouchMode() ?
								INVALID_POSITION : last);
						sel = fillFromBottom(last, childrenBottom);
					}
				} else {
					if (mSelectedPosition >= 0 && mSelectedPosition < mItemCount) {
						sel = fillSpecific(mSelectedPosition, oldSel == null ?
								childrenTop : oldSel.getTop());
					} else if (mFirstPosition < mItemCount)  {
						sel = fillSpecific(mFirstPosition, oldFirst == null ?
								childrenTop : oldFirst.getTop());
					} else {
						sel = fillSpecific(0, childrenTop);
					}
				}
				break;
			}

			recycleBin.scrapActiveViews();

			if (sel != null) {
				positionSelector(sel);
				mSelectedTop = sel.getTop();
			} else if (mTouchMode > TOUCH_MODE_DOWN && mTouchMode < TOUCH_MODE_SCROLL) {
				View child = getChildAt(mMotionPosition - mFirstPosition);
				if (child != null) positionSelector(child);
			} else {
				mSelectedTop = 0;
				mSelectorRect.setEmpty();
			}

			mLayoutMode = LAYOUT_NORMAL;
			mDataChanged = false;
			mNeedSync = false;
			setNextSelectedPositionInt(mSelectedPosition);

			//updateScrollIndicators();

			if (mItemCount > 0) {
				checkSelectionChanged();
			}

			invokeOnItemScrollListener();/**/

		}


		private void adjustViewsUpOrDown() {
			final int childCount = getChildCount();

			if (childCount > 0) {
				int delta;
				View child;

				if (!mStackFromBottom) {


					child = getChildAt(0);
					delta = child.getTop() - mListPadding.top;
					if (mFirstPosition != 0) {

						delta -= mVerticalSpacing;
					}
					if (delta < 0) {

						delta = 0;
					}
				} else {

					child = getChildAt(childCount - 1);
					delta = child.getBottom() - (getHeight() - mListPadding.bottom);

					if (mFirstPosition + childCount < mItemCount) {
						delta += mVerticalSpacing;
					}

					if (delta > 0) {

						delta = 0;
					}
				}

				if (delta != 0) {
					offsetChildrenTopAndBottom(-delta);
				}
			}
		}


		private void setupChild(View child, int position, int y, boolean flow, int childrenLeft,
				boolean selected, boolean recycled, int where) {
			boolean isSelected = selected && shouldShowSelector();
			final boolean updateChildSelected = isSelected != child.isSelected();
			final int mode = mTouchMode;
			final boolean isPressed = mode > TOUCH_MODE_DOWN && mode < TOUCH_MODE_SCROLL &&
			mMotionPosition == position;
			final boolean updateChildPressed = isPressed != child.isPressed();

			boolean needToMeasure = !recycled || updateChildSelected || child.isLayoutRequested();

			GridDynamicAbsListView.LayoutParams p = (GridDynamicAbsListView.LayoutParams)child.getLayoutParams();
			if (p == null) {
				p = new GridDynamicAbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT, 0);
			}
			p.viewType = mAdapter.getItemViewType(position);

			if (recycled && !p.forceAdd) {
				attachViewToParent(child, where, p);
			} else {
				p.forceAdd = false;
				addViewInLayout(child, where, p, true);
			}

			if (updateChildSelected) {
				child.setSelected(isSelected);
				if (isSelected) {
					requestFocus();
				}
			}

			if (updateChildPressed) {
				child.setPressed(isPressed);
			}

			if (needToMeasure) {
				int childHeightSpec = ViewGroup.getChildMeasureSpec(
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);

				int childWidthSpec = ViewGroup.getChildMeasureSpec(
                        MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY), 0, p.width);
				child.measure(childWidthSpec, childHeightSpec);
			} else {
				cleanupLayoutState(child);
			}

			final int w = child.getMeasuredWidth();
			final int h = child.getMeasuredHeight();

			int childLeft;
			final int childTop = flow ? y : y - h;

			switch (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
			case Gravity.LEFT:
				childLeft = childrenLeft;
				break;
			case Gravity.CENTER_HORIZONTAL:
				childLeft = childrenLeft + ((mColumnWidth - w) / 2);
				break;
			case Gravity.RIGHT:
				childLeft = childrenLeft + mColumnWidth - w;
				break;
			default:
				childLeft = childrenLeft;
				break;
			}

			if (needToMeasure) {
				final int childRight = childLeft + w;
				final int childBottom = childTop + h;
				child.layout(childLeft, childTop, childRight, childBottom);
			} else {
				child.offsetLeftAndRight(childLeft - child.getLeft());
				child.offsetTopAndBottom(childTop - child.getTop());
			}

			if (mCachingStarted) {
				child.setDrawingCacheEnabled(true);
			}
		}

		private void pinToTop(int childrenTop) {
			if (mFirstPosition == 0) {
				final int top = getChildAt(0).getTop();
				final int offset = childrenTop - top;
				if (offset < 0) {
					offsetChildrenTopAndBottom(offset);
				}
			}
		}

		private void pinToBottom(int childrenBottom) {
			final int count = getChildCount();
			if (mFirstPosition + count == mItemCount) {
				final int bottom = getChildAt(count - 1).getBottom();
				final int offset = childrenBottom - bottom;
				if (offset > 0) {
					offsetChildrenTopAndBottom(offset);
				}
			}
		}

		@Override
		protected void setSelectionInt(int position) {
			int previousSelectedPosition = mNextSelectedPosition;

			setNextSelectedPositionInt(position);
			this.layoutChildren();

			final int next = mStackFromBottom ? mItemCount - 1  - mNextSelectedPosition :
				mNextSelectedPosition;
			final int previous = mStackFromBottom ? mItemCount - 1
					- previousSelectedPosition : previousSelectedPosition;

			final int nextRow = next / mNumColumns;
			final int previousRow = previous / mNumColumns;

			if (nextRow != previousRow) {

			}

		}



		@Override
		protected boolean arrowScroll(int direction) {
			final int selectedPosition = mSelectedPosition;
			final int numColumns = mNumColumns;

			int startOfRowPos;
			int endOfRowPos;

			boolean moved = false;

			if (!mStackFromBottom) {
				startOfRowPos = (selectedPosition / numColumns) * numColumns;
				endOfRowPos = Math.min(startOfRowPos + numColumns - 1, mItemCount - 1);
			} else {
				final int invertedSelection = mItemCount - 1 - selectedPosition;
				endOfRowPos = mItemCount - 1 - (invertedSelection / numColumns) * numColumns;
				startOfRowPos = Math.max(0, endOfRowPos - numColumns + 1);
			}

			switch (direction) {
			case FOCUS_UP:
				if (startOfRowPos > 0) {
					mLayoutMode = LAYOUT_MOVE_SELECTION;
					setSelectionInt(Math.max(0, selectedPosition - numColumns));
					moved = true;
				}
				break;
			case FOCUS_DOWN:
				if (endOfRowPos < mItemCount - 1) {
					mLayoutMode = LAYOUT_MOVE_SELECTION;
					setSelectionInt(Math.min(selectedPosition + numColumns, mItemCount - 1));
					moved = true;
				}
				break;
			case FOCUS_LEFT:
				if (selectedPosition > startOfRowPos) {
					mLayoutMode = LAYOUT_MOVE_SELECTION;
					setSelectionInt(Math.max(0, selectedPosition - 1));
					moved = true;
				}
				break;
			case FOCUS_RIGHT:
				if (selectedPosition < endOfRowPos) {
					mLayoutMode = LAYOUT_MOVE_SELECTION;
					setSelectionInt(Math.min(selectedPosition + 1, mItemCount - 1));
					moved = true;
				}
				break;
			}

			if (moved) {
				playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
				invokeOnItemScrollListener();
			}

			if (moved) {

			}

			return moved;
		}

		@Override
		protected boolean isCandidateSelection(int childIndex, int direction) {
			final int count = getChildCount();
			final int invertedIndex = count - 1 - childIndex;

			int rowStart;
			int rowEnd;

			if (!mStackFromBottom) {
				rowStart = childIndex - (childIndex % mNumColumns);
				rowEnd = Math.max(rowStart + mNumColumns - 1, count);
			} else {
				rowEnd = count - 1 - (invertedIndex - (invertedIndex % mNumColumns));
				rowStart = Math.max(0, rowEnd - mNumColumns + 1);
			}

			switch (direction) {
			case View.FOCUS_RIGHT:

				return childIndex == rowStart;
			case View.FOCUS_DOWN:

				return rowStart == 0;
			case View.FOCUS_LEFT:

				return childIndex == rowEnd;
			case View.FOCUS_UP:

				return rowEnd == count - 1;
			case View.FOCUS_FORWARD:

				return childIndex == rowStart && rowStart == 0;
			case View.FOCUS_BACKWARD:

				return childIndex == rowEnd && rowEnd == count - 1;
			default:
				throw new IllegalArgumentException("direction must be one of "
				  + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, "
				  + "FOCUS_FORWARD, FOCUS_BACKWARD}");
			}
		}

		@Override
		protected void layoutChildren(boolean updated) {
			// TODO Auto-generated method stub

		}

	}



	private class HorizontalGridBuilder extends GridBuilder {


		@Override
		protected View makeAndAddView(int position, int x, boolean flow, int childrenTop,
				boolean selected, int where) {
			View child;

			if (!mDataChanged) {

				child = mRecycler.getActiveView(position);
				if (child != null) {

					setupChild(child, position, x, flow, childrenTop, selected, true, where);

					return child;
				}
			}


			child = obtainView(position, mIsScrap);
			setupChild(child, position, x, flow, childrenTop, selected, mIsScrap[0], where);

			return child;
		}

		@Override
		protected void fillGap(boolean right) {
			final int numRows = mNumRows;
			final int horizontalSpacing = mHorizontalSpacing;

			final int count = getChildCount();

			if (right) {
				final int startOffset = count > 0 ?
						getChildAt(count - 1).getRight() + horizontalSpacing : getListPaddingLeft();
						int position = mFirstPosition + count;
						if (mStackFromBottom) {
							position += numRows - 1;
						}
						fillRight(position, startOffset);
						correctTooLeft(numRows, horizontalSpacing, getChildCount());
			} else {
				final int startOffset = count > 0 ?
						getChildAt(0).getLeft() - horizontalSpacing : getWidth() - getListPaddingRight();
						int position = mFirstPosition;
						if (!mStackFromBottom) {
							position -= numRows;
						} else {
							position--;
						}
						fillLeft(position, startOffset);
						correctTooRight(numRows, horizontalSpacing, getChildCount());
			}
		}

		private View fillRight(int pos, int nextLeft) {

			View selectedView = null;

			final int end = (getRight() - getLeft()) - mListPadding.right;

			while (nextLeft < end && pos < mItemCount) {
				View temp = makeColumn(pos, nextLeft, true);
				if (temp != null) {
					selectedView = temp;
				}

				nextLeft = mReferenceView.getRight() + mHorizontalSpacing;

				pos += mNumRows;
			}

			return selectedView;
		}

		private View makeColumn(int startPos, int x, boolean flow) {

			final int rowHeight = mRowHeight;
			final int verticalSpacing = mVerticalSpacing;

			int last;
			int nextTop = mListPadding.top +
			((mStretchMode == STRETCH_SPACING_UNIFORM) ? verticalSpacing : 0);

			if (!mStackFromBottom) {
				last = Math.min(startPos + mNumRows, mItemCount);
			} else {
				last = startPos + 1;
				startPos = Math.max(0, startPos - mNumRows + 1);

				if (last - startPos < mNumRows) {
					nextTop += (mNumRows - (last - startPos)) * (rowHeight + verticalSpacing);
				}
			}

			View selectedView = null;

			final boolean hasFocus = shouldShowSelector();
			final boolean inClick = touchModeDrawsInPressedState();
			final int selectedPosition = mSelectedPosition;

			View child = null;
			for (int pos = startPos; pos < last; pos++) {

				boolean selected = pos == selectedPosition;

				final int where = flow ? -1 : pos - startPos;
				child = makeAndAddView(pos, x, flow, nextTop, selected, where);

				nextTop += rowHeight;
				if (pos < last - 1) {
					nextTop += verticalSpacing;
				}

				if (selected && (hasFocus || inClick)) {
					selectedView = child;
				}
			}

			mReferenceView = child;

			if (selectedView != null) {
				mReferenceViewInSelectedRow = mReferenceView;
			}

			return selectedView;
		}

		private View fillLeft(int pos, int nextRight) {

			View selectedView = null;

			final int end = mListPadding.left;

			while (nextRight > end && pos >= 0) {

				View temp = makeColumn(pos, nextRight, false);
				if (temp != null) {
					selectedView = temp;
				}

				nextRight = mReferenceView.getLeft() - mHorizontalSpacing;

				mFirstPosition = pos;

				pos -= mNumRows;
			}

			if (mStackFromBottom) {
				mFirstPosition = Math.max(0, pos + 1);
			}

			return selectedView;
		}

		private View fillFromTop(int nextLeft) {

			mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
			mFirstPosition = Math.min(mFirstPosition, mItemCount - 1);
			if (mFirstPosition < 0) {
				mFirstPosition = 0;
			}
			mFirstPosition -= mFirstPosition % mNumRows;
			return fillRight(mFirstPosition, nextLeft);
		}

		private View fillFromBottom(int lastPosition, int nextRight) {

			lastPosition = Math.max(lastPosition, mSelectedPosition);
			lastPosition = Math.min(lastPosition, mItemCount - 1);

			final int invertedPosition = mItemCount - 1 - lastPosition;
			lastPosition = mItemCount - 1 - (invertedPosition - (invertedPosition % mNumRows));

			return fillLeft(lastPosition, nextRight);
		}

		private View fillSelection(int childrenLeft, int childrenRight) {

			final int selectedPosition = reconcileSelectedPosition();
			final int numRows = mNumRows;
			final int horizontalSpacing = mHorizontalSpacing;

			int columnStart;
			int columnEnd = -1;

			if (!mStackFromBottom) {
				columnStart = selectedPosition - (selectedPosition % numRows);
			} else {
				final int invertedSelection = mItemCount - 1 - selectedPosition;

				columnEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numRows));
				columnStart = Math.max(0, columnEnd - numRows + 1);
			}

			final int fadingEdgeLength = getHorizontalFadingEdgeLength();
			final int leftSelectionPixel = getLeftSelectionPixel(childrenLeft, fadingEdgeLength, columnStart);

			final View sel = makeColumn(mStackFromBottom ? columnEnd : columnStart, leftSelectionPixel, true);
			mFirstPosition = columnStart;

			final View referenceView = mReferenceView;

			if (!mStackFromBottom) {
				fillRight(columnStart + numRows, referenceView.getRight() + horizontalSpacing);
				pinToRight(childrenRight);
				fillLeft(columnStart - numRows, referenceView.getLeft() - horizontalSpacing);
				adjustViewsLeftOrRight();
			} else {
				final int rightSelectionPixel = getRightSelectionPixel(childrenRight,
						fadingEdgeLength, numRows, columnStart);
				final int offset = rightSelectionPixel - referenceView.getRight();
				offsetChildrenLeftAndRight(offset);
				fillLeft(columnStart - 1, referenceView.getLeft() - horizontalSpacing);
				pinToLeft(childrenLeft);
				fillRight(columnEnd + numRows, referenceView.getRight() + horizontalSpacing);
				adjustViewsLeftOrRight();
			}

			return sel;
		}

		private View fillSpecific(int position, int left) {

			final int numRows = mNumRows;

			int motionColumnStart;
			int motionColumnEnd = -1;

			if (!mStackFromBottom) {

				motionColumnStart = position - (position % numRows);
			} else {

				final int invertedSelection = mItemCount - 1 - position;

				motionColumnEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numRows));
				motionColumnStart = Math.max(0, motionColumnEnd - numRows + 1);
			}

			final View temp = makeColumn(mStackFromBottom ? motionColumnEnd : motionColumnStart, left, true);
			mFirstPosition = motionColumnStart;

			final View referenceView = mReferenceView;

			if (referenceView == null) {
				return null;
			}

			final int horizontalSpacing = mHorizontalSpacing;

			View leftOf;
			View rightOf;

			if (!mStackFromBottom) {
				leftOf = fillLeft(motionColumnStart - numRows, referenceView.getLeft() - horizontalSpacing);
				adjustViewsLeftOrRight();
				rightOf = fillRight(motionColumnStart + numRows, referenceView.getRight() + horizontalSpacing);
				final int childCount = getChildCount();
				if (childCount > 0) {
					correctTooLeft(numRows, horizontalSpacing, childCount);
				}
			} else {
				rightOf = fillRight(motionColumnEnd + numRows, referenceView.getRight() + horizontalSpacing);
				adjustViewsLeftOrRight();
				leftOf = fillLeft(motionColumnStart - 1, referenceView.getLeft() - horizontalSpacing);

				final int childCount = getChildCount();
				if (childCount > 0) {
					correctTooRight(numRows, horizontalSpacing, childCount);
				}
			}

			if (temp != null) {
				return temp;
			} else if (leftOf != null) {
				return leftOf;
			} else {
				return rightOf;
			}
		}


		private void correctTooLeft(int numRows, int horizontalSpacing, int childCount) {

			final int lastPosition = mFirstPosition + childCount - 1;
			if (lastPosition == mItemCount - 1 && childCount > 0) {

				final View lastChild = getChildAt(childCount - 1);

				final int lastRight = lastChild.getRight();

				final int end = (getRight() - getLeft()) - mListPadding.right;
				int rightOffset = end - lastRight;

				final View firstChild = getChildAt(0);
				final int firstLeft = firstChild.getLeft();

				if (rightOffset > 0 && (mFirstPosition > 0 || firstLeft < mListPadding.left))  {
					if (mFirstPosition == 0)
					{

						rightOffset = Math.min(rightOffset, mListPadding.left - firstLeft);
					}

					offsetChildrenLeftAndRight(rightOffset);
					if (mFirstPosition > 0) {

						fillLeft(mFirstPosition - (mStackFromBottom ? 1 : numRows),
								firstChild.getLeft() - horizontalSpacing);

						adjustViewsLeftOrRight();
					}
				}
			}
		}

		private void correctTooRight(int numRows, int horizontalSpacing, int childCount) {

			if (mFirstPosition == 0 && childCount > 0) {

				final View firstChild = getChildAt(0);
				final int firstLeft = firstChild.getLeft();
				final int start = mListPadding.left;
				final int end = (getRight() - getLeft()) - mListPadding.right;

				int leftOffset = firstLeft - start;
				final View lastChild = getChildAt(childCount - 1);
				final int lastRight = lastChild.getRight();
				final int lastPosition = mFirstPosition + childCount - 1;

				if (leftOffset > 0 && (lastPosition < mItemCount - 1 || lastRight > end))  {
					if (lastPosition == mItemCount - 1 ) {

						leftOffset = Math.min(leftOffset, lastRight - end);
					}


					offsetChildrenLeftAndRight(-leftOffset);
					if (lastPosition < mItemCount - 1) {

						fillRight(lastPosition + (!mStackFromBottom ? 1 : numRows),
								lastChild.getRight() + horizontalSpacing);
						adjustViewsLeftOrRight();
					}
				}
			}
		}

		private View moveSelection(int delta, int childrenLeft, int childrenRight) {

			final int fadingEdgeLength = getHorizontalFadingEdgeLength();
			final int selectedPosition = mSelectedPosition;
			final int numRows = mNumRows;
			final int horizontalSpacing = mHorizontalSpacing;

			int oldColumnStart;
			int columnStart;
			int columnEnd = -1;

			if (!mStackFromBottom) {
				oldColumnStart = (selectedPosition - delta) - ((selectedPosition - delta) % numRows);

				columnStart = selectedPosition - (selectedPosition % numRows);
			} else {
				int invertedSelection = mItemCount - 1 - selectedPosition;

				columnEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numRows));
				columnStart = Math.max(0, columnEnd - numRows + 1);

				invertedSelection = mItemCount - 1 - (selectedPosition - delta);
				oldColumnStart = mItemCount - 1 - (invertedSelection - (invertedSelection % numRows));
				oldColumnStart = Math.max(0, oldColumnStart - numRows + 1);
			}

			final int rowDelta = columnStart - oldColumnStart;

			final int leftSelectionPixel = getLeftSelectionPixel(childrenLeft, fadingEdgeLength, columnStart);
			final int rightSelectionPixel = getRightSelectionPixel(childrenRight, fadingEdgeLength,
					numRows, columnStart);

			mFirstPosition = columnStart;

			View sel;
			View referenceView;

			if (rowDelta > 0) {

				final int oldRight = mReferenceViewInSelectedRow == null ? 0 :
					mReferenceViewInSelectedRow.getRight();

				sel = makeColumn(mStackFromBottom ? columnEnd : columnStart, oldRight + horizontalSpacing, true);
				referenceView = mReferenceView;

				adjustForRightFadingEdge(referenceView, leftSelectionPixel, rightSelectionPixel);
			} else if (rowDelta < 0) {

				final int oldTop = mReferenceViewInSelectedRow == null ?
						0 : mReferenceViewInSelectedRow.getLeft();

				sel = makeColumn(mStackFromBottom ? columnEnd : columnStart, oldTop - horizontalSpacing, false);
				referenceView = mReferenceView;

				adjustForLeftFadingEdge(referenceView, leftSelectionPixel, rightSelectionPixel);
			} else {

				final int oldTop = mReferenceViewInSelectedRow == null ?
						0 : mReferenceViewInSelectedRow.getLeft();

				sel = makeColumn(mStackFromBottom ? columnEnd : columnStart, oldTop, true);
				referenceView = mReferenceView;
			}

			if (!mStackFromBottom) {
				fillLeft(columnStart - numRows, referenceView.getLeft() - horizontalSpacing);
				adjustViewsLeftOrRight();
				fillRight(columnStart + numRows, referenceView.getRight() + horizontalSpacing);
			} else {
				fillRight(columnStart + numRows, referenceView.getRight() + horizontalSpacing);
				adjustViewsLeftOrRight();
				fillLeft(columnStart - 1, referenceView.getLeft() - horizontalSpacing);
			}

			return sel;
		}



		@Override
		protected void layoutChildren(boolean updated)
		{

			final int childrenLeft = mListPadding.left;
			final int childrenRight = getRight() - getLeft() - mListPadding.right;

			int childCount = getChildCount();
			int index;
			int delta = 0;

			View sel;
			View oldSel = null;
			View oldFirst = null;
			View newSel = null;
			
			switch (mLayoutMode) {
			case LAYOUT_SET_SELECTION:
				index = mNextSelectedPosition - mFirstPosition;
				if (index >= 0 && index < childCount) {
					newSel = getChildAt(index);
				}
				break;
			case LAYOUT_FORCE_TOP:
			case LAYOUT_FORCE_BOTTOM:
			case LAYOUT_SPECIFIC:
			case LAYOUT_SYNC:
				break;
			case LAYOUT_MOVE_SELECTION:
				if (mNextSelectedPosition >= 0) {
					delta = mNextSelectedPosition - mSelectedPosition;
				}
				break;
			default:

				index = mSelectedPosition - mFirstPosition;
				if (index >= 0 && index < childCount) {
					oldSel = getChildAt(index);
				}

				oldFirst = getChildAt(0);
			}

			boolean dataChanged = mDataChanged;
			if (dataChanged) {
				handleDataChanged();
			}

			if (mItemCount == 0) {
				resetList();
				invokeOnItemScrollListener();
				return;
			}

			setSelectedPositionInt(mNextSelectedPosition);

			final int firstPosition = mFirstPosition;
			final RecycleBin recycleBin = mRecycler;


			if (dataChanged) {
				for (int i = 0; i < childCount; i++) {
					recycleBin.addScrapView(getChildAt(i));
				}
			} else {
				recycleBin.fillActiveViews(childCount, firstPosition);
			}

			detachAllViewsFromParent();


			switch (mLayoutMode)
			{
			case LAYOUT_SET_SELECTION:
				if (newSel != null) {
					sel = fillFromSelection(newSel.getLeft(), childrenLeft, childrenRight);
				} else {
					sel = fillSelection(childrenLeft, childrenRight);
				}
				break;
			case LAYOUT_FORCE_TOP:
				mFirstPosition = 0;
				sel = fillFromTop(childrenLeft);
				adjustViewsLeftOrRight();
				break;
			case LAYOUT_FORCE_BOTTOM:
				sel = fillRight(mItemCount - 1, childrenRight);
				adjustViewsLeftOrRight();
				break;
			case LAYOUT_SPECIFIC:
				sel = fillSpecific(mSelectedPosition, mSpecificTop);
				break;
			case LAYOUT_SYNC:
				sel = fillSpecific(mSyncPosition, mSpecificTop);
				break;
			case LAYOUT_MOVE_SELECTION:

				sel = moveSelection(delta, childrenLeft, childrenRight);
				break;
			default:
				if (childCount == 0) {
					if (!mStackFromBottom) {
						setSelectedPositionInt(mAdapter == null || isInTouchMode() ?
								INVALID_POSITION : 0);
						sel = fillFromTop(childrenLeft);
					} else {
						final int last = mItemCount - 1;
						setSelectedPositionInt(mAdapter == null || isInTouchMode() ?
								INVALID_POSITION : last);
						sel = fillFromBottom(last, childrenRight);
					}
				} else {
					if (mSelectedPosition >= 0 && mSelectedPosition < mItemCount) {
						sel = fillSpecific(mSelectedPosition, oldSel == null ?
								childrenLeft : oldSel.getLeft());
					} else if (mFirstPosition < mItemCount)  {
						sel = fillSpecific(mFirstPosition, oldFirst == null ?
								childrenLeft : oldFirst.getLeft());
					} else {
						sel = fillSpecific(0, childrenLeft);
					}
				}
				break;
			}

			recycleBin.scrapActiveViews();

			if (sel != null) {
				positionSelector(sel);
				mSelectedTop = sel.getLeft();
			} else if (mTouchMode > TOUCH_MODE_DOWN && mTouchMode < TOUCH_MODE_SCROLL) {
				View child = getChildAt(mMotionPosition - mFirstPosition);
				if (child != null) positionSelector(child);
			} else {
				mSelectedTop = 0;
				mSelectorRect.setEmpty();
			}

			mLayoutMode = LAYOUT_NORMAL;
			mDataChanged = false;
			mNeedSync = false;
			setNextSelectedPositionInt(mSelectedPosition);

			//updateScrollIndicators();

			if (mItemCount > 0) {
				checkSelectionChanged();
			}

			invokeOnItemScrollListener();
		}



		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int widthMode = MeasureSpec.getMode(widthMeasureSpec);
			int heightMode = MeasureSpec.getMode(heightMeasureSpec);
			int widthSize = MeasureSpec.getSize(widthMeasureSpec);
			int heightSize = MeasureSpec.getSize(heightMeasureSpec);

			if (heightMode == MeasureSpec.UNSPECIFIED) {
				if (mRowHeight > 0) {
					heightSize = mRowHeight + mListPadding.top + mListPadding.bottom;
				} else {
					heightSize = mListPadding.top + mListPadding.bottom;
				}
				heightSize += getHorizontalScrollbarHeight();
			}

			int childHeight = heightSize - mListPadding.top - mListPadding.bottom;
			determineRows(childHeight);

			int childWidth = 0;

			mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
			final int count = mItemCount;
			if (count > 0) {
				final View child = obtainView(0, mIsScrap);

				GridDynamicAbsListView.LayoutParams p = (GridDynamicAbsListView.LayoutParams)child.getLayoutParams();
				if (p == null) {
					p = new GridDynamicAbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.FILL_PARENT, 0);
					child.setLayoutParams(p);
				}
				p.viewType = mAdapter.getItemViewType(0);
				p.forceAdd = true;

				int childHeightSpec = getChildMeasureSpec(
						MeasureSpec.makeMeasureSpec(mRowHeight, MeasureSpec.UNSPECIFIED), 0, p.height);
				int childWidthSpec = getChildMeasureSpec(
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY), 0, p.width);
				child.measure(childWidthSpec, childHeightSpec);

				childWidth = child.getMeasuredWidth();

				if (mRecycler.shouldRecycleViewType(p.viewType)) {
					mRecycler.addScrapView(child);
				}
			}

			if (widthMode == MeasureSpec.UNSPECIFIED) {
				widthSize = mListPadding.left + mListPadding.right + childWidth +
				getHorizontalFadingEdgeLength() * 2;
			}

			if (widthMode == MeasureSpec.AT_MOST) {
				int ourSize =  mListPadding.left + mListPadding.right;

				final int numRows = mNumRows;
				for (int i = 0; i < count; i += numRows) {
					ourSize += childWidth;
					if (i + numRows < count) {
						ourSize += mHorizontalSpacing;
					}
					if (ourSize >= widthSize) {
						ourSize = widthSize;
						break;
					}
				}
				widthSize = ourSize;
			}

			setMeasuredDimension(widthSize, heightSize);
			mWidthMeasureSpec = widthMeasureSpec;

		}


		private void determineRows(int availableSpace) {
			final int requestedVerticalSpacing = mRequestedVerticalSpacing;
			final int stretchMode = mStretchMode;
			final int requestedRowHeight = mRequestedRowHeight;
			mHorizontalSpacing = mRequestedHorizontalSpacing;

			if (mRequestedNumRows == AUTO_FIT) {
				if (requestedRowHeight > 0) {

					mNumRows = (availableSpace + mRequestedVerticalSpacing) /
					(requestedRowHeight + mRequestedVerticalSpacing);
				} else {

					mNumRows = 2;
				}
			} else {

				mNumRows = mRequestedNumRows;
			}

			if (mNumRows <= 0) {
				mNumRows = 1;
			}

			switch (stretchMode) {
			case NO_STRETCH:

				mRowHeight = mRequestedRowHeight;
				mVerticalSpacing = mRequestedVerticalSpacing;
				break;

			default:
				int spaceLeftOver = 0;
				switch (stretchMode) {
				case STRETCH_COLUMN_WIDTH:
					spaceLeftOver = availableSpace - (mNumRows * requestedRowHeight) -
					((mNumRows - 1) * requestedVerticalSpacing);

					mRowHeight = requestedRowHeight + spaceLeftOver / mNumRows;
					mVerticalSpacing = requestedVerticalSpacing;
					break;

				case STRETCH_SPACING:
					spaceLeftOver = availableSpace - (mNumRows * requestedRowHeight) -
					((mNumRows - 1) * requestedVerticalSpacing);

					mRowHeight = requestedRowHeight;
					if (mNumRows > 1) {
						mVerticalSpacing = requestedVerticalSpacing +
						spaceLeftOver / (mNumRows - 1);
					} else {
						mVerticalSpacing = requestedVerticalSpacing + spaceLeftOver;
					}
					break;

				case STRETCH_SPACING_UNIFORM:

					spaceLeftOver = availableSpace - (mNumRows * requestedRowHeight) -
					((mNumRows + 1) * requestedVerticalSpacing);
					mRowHeight = requestedRowHeight;
					if (mNumRows > 1) {
						mVerticalSpacing = requestedVerticalSpacing + spaceLeftOver / (mNumRows + 1);
					} else {
						mVerticalSpacing = ((requestedVerticalSpacing * 2) + spaceLeftOver) / 2;
					}
					break;
				}

				break;
			}

		}

		private View fillFromSelection(int selectedLeft, int childrenLeft, int childrenRight) {

			final int fadingEdgeLength = getHorizontalFadingEdgeLength();
			final int selectedPosition = mSelectedPosition;
			final int numRows = mNumRows;
			final int horizontalSpacing = mHorizontalSpacing;

			int columnStart;
			int columnEnd = -1;

			if (!mStackFromBottom) {
				columnStart = selectedPosition - (selectedPosition % numRows);
			} else {
				int invertedSelection = mItemCount - 1 - selectedPosition;

				columnEnd = mItemCount - 1 - (invertedSelection - (invertedSelection % numRows));
				columnStart = Math.max(0, columnEnd - numRows + 1);
			}

			View sel;
			View referenceView;

			int leftSelectionPixel = getLeftSelectionPixel(childrenLeft, fadingEdgeLength, columnStart);
			int rightSelectionPixel = getRightSelectionPixel(childrenRight, fadingEdgeLength,
					numRows, columnStart);

			sel = makeColumn(mStackFromBottom ? columnEnd : columnStart, selectedLeft, true);
			mFirstPosition = columnStart;

			referenceView = mReferenceView;
			adjustForLeftFadingEdge(referenceView, leftSelectionPixel, rightSelectionPixel);
			adjustForRightFadingEdge(referenceView, leftSelectionPixel, rightSelectionPixel);

			if (!mStackFromBottom) {
				fillLeft(columnStart - numRows, referenceView.getLeft() - horizontalSpacing);
				adjustViewsLeftOrRight();
				fillRight(columnStart + numRows, referenceView.getRight() + horizontalSpacing);
			} else {
				fillRight(columnEnd + numRows, referenceView.getRight() + horizontalSpacing);
				adjustViewsLeftOrRight();
				fillLeft(columnStart - 1, referenceView.getLeft() - horizontalSpacing);
			}


			return sel;
		}

		private int getRightSelectionPixel(int childrenRight, int fadingEdgeLength,
				int numColumns, int rowStart) {
			// Last pixel we can draw the selection into
			int rightSelectionPixel = childrenRight;
			if (rowStart + numColumns - 1 < mItemCount - 1) {
				rightSelectionPixel -= fadingEdgeLength;
			}
			return rightSelectionPixel;
		}


		private int getLeftSelectionPixel(int childrenLeft, int fadingEdgeLength, int rowStart) {
			// first pixel we can draw the selection into
			int leftSelectionPixel = childrenLeft;
			if (rowStart > 0) {
				leftSelectionPixel += fadingEdgeLength;
			}
			return leftSelectionPixel;
		}


		private void adjustForRightFadingEdge(View childInSelectedRow,
				int leftSelectionPixel, int rightSelectionPixel) {


			if (childInSelectedRow.getRight() > rightSelectionPixel) {

				int spaceToLeft = childInSelectedRow.getLeft() - leftSelectionPixel;
				int spaceToRight = childInSelectedRow.getRight() - rightSelectionPixel;
				int offset = Math.min(spaceToLeft, spaceToRight);

				offsetChildrenLeftAndRight(-offset);
			}
		}

		private void adjustForLeftFadingEdge(View childInSelectedRow,
				int leftSelectionPixel, int rightSelectionPixel)
		{
			if (childInSelectedRow.getLeft() < leftSelectionPixel) {

				int spaceToLeft = leftSelectionPixel - childInSelectedRow.getLeft();
				int spaceToRight = rightSelectionPixel - childInSelectedRow.getRight();
				int offset = Math.min(spaceToLeft, spaceToRight);

				offsetChildrenLeftAndRight(offset);
			}
		}


		private void adjustViewsLeftOrRight() {
			final int childCount = getChildCount();

			if (childCount > 0) {
				int delta;
				View child;

				if (!mStackFromBottom) {
					child = getChildAt(0);
					delta = child.getLeft() - mListPadding.left;
					if (mFirstPosition != 0) {

						delta -= mHorizontalSpacing;
					}
					if (delta < 0) {

						delta = 0;
					}
				} else {

					child = getChildAt(childCount - 1);
					delta = child.getRight() - (getWidth() - mListPadding.right);

					if (mFirstPosition + childCount < mItemCount) {
						delta += mHorizontalSpacing;
					}

					if (delta > 0) {

						delta = 0;
					}
				}

				if (delta != 0) {
					offsetChildrenLeftAndRight(-delta);
				}
			}
		}


		private void setupChild(View child, int position, int x, boolean flow, int childrenTop,
				boolean selected, boolean recycled, int where) {
			boolean isSelected = selected && shouldShowSelector();
			final boolean updateChildSelected = isSelected != child.isSelected();
			final int mode = mTouchMode;
			final boolean isPressed = mode > TOUCH_MODE_DOWN && mode < TOUCH_MODE_SCROLL &&
			mMotionPosition == position;
			final boolean updateChildPressed = isPressed != child.isPressed();

			boolean needToMeasure = !recycled || updateChildSelected || child.isLayoutRequested();


			GridDynamicAbsListView.LayoutParams p = (GridDynamicAbsListView.LayoutParams)child.getLayoutParams();
			if (p == null) {
				p = new GridDynamicAbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.FILL_PARENT, 0);
			}
			p.viewType = mAdapter.getItemViewType(position);

			if (recycled && !p.forceAdd) {
				attachViewToParent(child, where, p);
			} else {
				p.forceAdd = false;
				addViewInLayout(child, where, p, true);
			}

			/*
			if (updateChildSelected) {
				child.setSelected(isSelected);
				if (isSelected) {
					requestFocus();
				}
			}

			if (updateChildPressed) {
				child.setPressed(isPressed);
			}*/

			
			if (needToMeasure) {
				int childWidthSpec = ViewGroup.getChildMeasureSpec(
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.width);

				int childHeightSpec = ViewGroup.getChildMeasureSpec(
                        MeasureSpec.makeMeasureSpec(mRowHeight, MeasureSpec.EXACTLY), 0, p.height);
				child.measure(childWidthSpec, childHeightSpec);
			} else {
				cleanupLayoutState(child);
			}

			final int w = child.getMeasuredWidth();
			final int h = child.getMeasuredHeight();

			final int childLeft = flow ? x : x - w;
			int childTop;

			switch (mGravity & Gravity.VERTICAL_GRAVITY_MASK) {
			case Gravity.TOP:
				childTop = childrenTop;
				break;
			case Gravity.CENTER_HORIZONTAL:
				childTop = childrenTop + ((mRowHeight - h) / 2);
				break;
			case Gravity.RIGHT:
				childTop = childrenTop + mRowHeight - h;
				break;
			default:
				childTop = childrenTop;
				break;
			}

			if (needToMeasure) {
				final int childRight = childLeft + w;
				final int childBottom = childTop + h;
				child.layout(childLeft, childTop, childRight, childBottom);
			} else {
				child.offsetLeftAndRight(childLeft - child.getLeft());
				child.offsetTopAndBottom(childTop - child.getTop());
			}

			if (mCachingStarted) {
				//child.setDrawingCacheEnabled(true);
			}
		}

		private void pinToLeft(int childrenLeft) {
			if (mFirstPosition == 0) {
				final int left = getChildAt(0).getLeft();
				final int offset = childrenLeft - left;
				if (offset < 0) {
					offsetChildrenLeftAndRight(offset);
				}
			}
		}

		private void pinToRight(int childrenRight) {
			final int count = getChildCount();
			if (mFirstPosition + count == mItemCount) {
				final int right = getChildAt(count - 1).getRight();
				final int offset = childrenRight - right;
				if (offset > 0) {
					offsetChildrenLeftAndRight(offset);
				}
			}
		}

		@Override
		protected void setSelectionInt(int position) {
			int previousSelectedPosition = mNextSelectedPosition;

			setNextSelectedPositionInt(position);
			this.layoutChildren();

			final int next = mStackFromBottom ? mItemCount - 1  - mNextSelectedPosition :
				mNextSelectedPosition;
			final int previous = mStackFromBottom ? mItemCount - 1
					- previousSelectedPosition : previousSelectedPosition;

			final int nextColumn = next / mNumRows;
			final int previousColumn = previous / mNumRows;

			if (nextColumn != previousColumn) {

			}

		}


		@Override
		protected boolean arrowScroll(int direction) {
			final int selectedPosition = mSelectedPosition;
			final int numRows = mNumRows;

			int startOfColumnPos;
			int endOfColumnPos;

			boolean moved = false;

			if (!mStackFromBottom) {
				startOfColumnPos = (selectedPosition / numRows) * numRows;
				endOfColumnPos = Math.min(startOfColumnPos + numRows - 1, mItemCount - 1);
			} else {
				final int invertedSelection = mItemCount - 1 - selectedPosition;
				endOfColumnPos = mItemCount - 1 - (invertedSelection / numRows) * numRows;
				startOfColumnPos = Math.max(0, endOfColumnPos - numRows + 1);
			}

			switch (direction) {
			case FOCUS_LEFT:
				if (startOfColumnPos > 0) {
					mLayoutMode = LAYOUT_MOVE_SELECTION;
					setSelectionInt(Math.max(0, selectedPosition - numRows));
					moved = true;
				}
				break;
			case FOCUS_RIGHT:
				if (startOfColumnPos < mItemCount - 1) {
					mLayoutMode = LAYOUT_MOVE_SELECTION;
					setSelectionInt(Math.min(selectedPosition + numRows, mItemCount - 1));
					moved = true;
				}
				break;
			case FOCUS_UP:
				if (selectedPosition > startOfColumnPos) {
					mLayoutMode = LAYOUT_MOVE_SELECTION;
					setSelectionInt(Math.max(0, selectedPosition - 1));
					moved = true;
				}
				break;
			case FOCUS_DOWN:
				if (selectedPosition < endOfColumnPos) {
					mLayoutMode = LAYOUT_MOVE_SELECTION;
					setSelectionInt(Math.min(selectedPosition + 1, mItemCount - 1));
					moved = true;
				}
				break;
			}

			if (moved) {
				playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
				invokeOnItemScrollListener();
			}

			if (moved) {
				//awakenScrollBars();
			}

			return moved;
		}


		@Override
		protected boolean isCandidateSelection(int childIndex, int direction) {
			final int count = getChildCount();
			final int invertedIndex = count - 1 - childIndex;
			final int numRows = mNumRows;

			int columnStart;
			int columnEnd;

			if (!mStackFromBottom) {
				columnStart = childIndex - (childIndex % numRows);
				columnEnd = Math.max(columnStart + numRows - 1, count);
			} else {
				columnEnd = count - 1 - (invertedIndex - (invertedIndex % numRows));
				columnStart = Math.max(0, columnEnd - numRows + 1);
			}

			switch (direction) {
			case View.FOCUS_RIGHT:
				// coming from left, selection is only valid if it is on left
				// edge
				return childIndex == columnStart;
			case View.FOCUS_DOWN:
				// coming from top; only valid if in top row
				return columnStart == 0;
			case View.FOCUS_LEFT:
				// coming from right, must be on right edge
				return childIndex == columnStart;
			case View.FOCUS_UP:
				// coming from bottom, need to be in last row
				return columnStart == count - 1;
			case View.FOCUS_FORWARD:
				// coming from top-left, need to be first in top row
				return childIndex == columnStart && columnStart == 0;
			case View.FOCUS_BACKWARD:
				// coming from bottom-right, need to be last in bottom row
				return childIndex == columnEnd && columnEnd == count - 1;
			default:
				throw new IllegalArgumentException("direction must be one of "
						+ "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, "
						+ "FOCUS_FORWARD, FOCUS_BACKWARD}.");

			}
		}

		@Override
		protected void layoutChildren() {
			layoutChildren(false);

		}

	}


    public void scrollToEnd() {
        int count = mAdapter.getCount();
        if (count > 0) {
            setSelection(count - 1);
        }
    }


    public void scrollToStart() {
        if (mAdapter.getCount() > 0) {
            setSelection(0);
        }
    }

    private OrientationHelper orientationHelper = null;

    public OrientationHelper getOrientationHelper() {
        if (orientationHelper == null) {
            orientationHelper = createOrientationHelper();
        }
        return orientationHelper;
    }

    private OrientationHelper createOrientationHelper() {
        if (mScrollVertically) {
            return OrientationHelpers.VERTICAL;
        } else {
            return OrientationHelpers.HORIZONTAL;
        }
    }

    public boolean isScrolledToStart() {
        if (getFirstVisiblePosition() != 0) {
            return false;
        }
        View edgeChild = getChildAt(0);
        return getOrientationHelper().getStartCoordinate(edgeChild) >= 0;
    }

    public boolean isScrolledToEnd() {
        if (getLastVisiblePosition() != mAdapter.getCount() - 1) {
            return false;
        }
        View edgeChild = getChildAt(getChildCount() - 1);
        OrientationHelper orientationHelper = getOrientationHelper();
        return orientationHelper.getEndCoordinate(edgeChild) <= orientationHelper.getViewSize(this);
    }

}

