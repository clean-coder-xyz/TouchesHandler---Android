package com.cleancoder.learning.toucheshandler.griddynamicview;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;


public abstract class GridDynamicAdapterView<T extends Adapter> extends ViewGroup {

	public static final int ITEM_VIEW_TYPE_IGNORE = -1;
	public static final int ITEM_VIEW_TYPE_HEADER_OR_FOOTER = -2;

	protected Context mContext = null;
	protected boolean mIsVertical = true;

	int mFirstPosition = 0;
	int mSpecificTop;
	int mSyncPosition;
	long mSyncRowId = INVALID_ROW_ID;
	long mSyncSize;
	boolean mNeedSync = false;
	int mSyncMode;
	private int mLayoutHeight;
	
	int mLayoutWidth;
	static final int SYNC_SELECTED_POSITION = 0;
	static final int SYNC_FIRST_POSITION = 1;
	static final int SYNC_MAX_DURATION_MILLIS = 100;
	boolean mInLayout = false;

	OnItemSelectedListener mOnItemSelectedListener;
	OnItemClickListener	   mOnItemClickListener;
	OnItemLongClickListener mOnItemLongClickListener;
	
	public boolean mDataChanged;
	int mNextSelectedPosition = INVALID_POSITION;
	long mNextSelectedRowId = INVALID_ROW_ID;
	int mSelectedPosition = INVALID_POSITION;
	long mSelectedRowId = INVALID_ROW_ID;
	private View mEmptyView;
	
	public int mItemCount;
	int mOldItemCount;

	public static final int INVALID_POSITION = -1;
	public static final long INVALID_ROW_ID = Long.MIN_VALUE;
	int mOldSelectedPosition = INVALID_POSITION;
	long mOldSelectedRowId = INVALID_ROW_ID;

	private SelectionNotifier mSelectionNotifier;

	boolean mBlockLayoutRequests = false;
	protected boolean mIsCaruselScroller = false;

	public GridDynamicAdapterView(Context context) {
		super(context);
		mContext = context;
	}

	public GridDynamicAdapterView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public GridDynamicAdapterView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	public interface OnItemClickListener {
		void onItemClick(GridDynamicAdapterView<?> parent, View view, int position, long id);
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mOnItemClickListener = listener;
	}

	public final OnItemClickListener getOnItemClickListener() {
		return mOnItemClickListener;
	}

	
	public boolean performItemClick(View view, int position, long id) {
		if (mOnItemClickListener != null) 
		{
			playSoundEffect(SoundEffectConstants.CLICK);
			mOnItemClickListener.onItemClick(this, view, position, id);
			return true;
		}

		return false;
	}

	
	public interface OnItemLongClickListener {
			boolean onItemLongClick(GridDynamicAdapterView<?> parent, View view, int position, long id);
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		if (!isLongClickable()) {
			setLongClickable(true);
		}
		mOnItemLongClickListener = listener;
	}

	public final OnItemLongClickListener getOnItemLongClickListener() {
		return mOnItemLongClickListener;
	}

	public interface OnItemSelectedListener {
	
		void onItemSelected(GridDynamicAdapterView<?> parent, View view, int position, long id);
		void onNothingSelected(GridDynamicAdapterView<?> parent);
	}

	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		mOnItemSelectedListener = listener;
	}

	public final OnItemSelectedListener getOnItemSelectedListener() {
		return mOnItemSelectedListener;
	}

	public static class AdapterContextMenuInfo implements ContextMenu.ContextMenuInfo {

		public AdapterContextMenuInfo(View targetView, int position, long id) {
			this.targetView = targetView;
			this.position = position;
			this.id = id;
		}

		
		public View targetView;
		public int position;
		public long id;
	}

	public abstract T getAdapter();
	public abstract void setAdapter(T adapter);

	
	@Override
	public void addView(View child) {
		throw new UnsupportedOperationException("addView(View) is not supported in AdapterView");
	}

	
	@Override
	public void addView(View child, int index) {
		throw new UnsupportedOperationException("addView(View, int) is not supported in AdapterView");
	}

	@Override
	public void addView(View child, LayoutParams params) {
		throw new UnsupportedOperationException("addView(View, LayoutParams) "
				+ "is not supported in AdapterView");
	}

	
	@Override
	public void addView(View child, int index, LayoutParams params) {
		throw new UnsupportedOperationException("addView(View, int, LayoutParams) "
				+ "is not supported in AdapterView");
	}

	
	@Override
	public void removeView(View child) {
		throw new UnsupportedOperationException("removeView(View) is not supported in AdapterView");
	}

	@Override
	public void removeViewAt(int index) {
		throw new UnsupportedOperationException("removeViewAt(int) is not supported in AdapterView");
	}

	
	@Override
	public void removeAllViews() {
		throw new UnsupportedOperationException("removeAllViews() is not supported in AdapterView");
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		mLayoutHeight = getHeight();
		mLayoutWidth = getWidth();
	}

	
	public int getSelectedItemPosition() {
		return mNextSelectedPosition;
	}

	public long getSelectedItemId() {
		return mNextSelectedRowId;
	}
	
	public abstract View getSelectedView();

	
	public Object getSelectedItem() {
		T adapter = getAdapter();
		int selection = getSelectedItemPosition();
		if (adapter != null && adapter.getCount() > 0 && selection >= 0) {
			return adapter.getItem(selection);
		} else {
			return null;
		}
	}

	
	public int getCount() {
		return mItemCount;
	}

	public int getPositionForView(View view) {
		View listItem = view;
		try {
			View v;
			while (!(v = (View) listItem.getParent()).equals(this)) {
				listItem = v;
			}
		} catch (ClassCastException e) {
			// We made it up to the window without find this list view
			return INVALID_POSITION;
		}

		// Search the children for the list item
		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			if (getChildAt(i).equals(listItem)) {
				return mFirstPosition + i;
			}
		}

		// Child not found!
		return INVALID_POSITION;
	}

	
	public int getFirstVisiblePosition() {
		return mFirstPosition;
	}

	public int getLastVisiblePosition() {
		return mFirstPosition + getChildCount() - 1;
	}

	public abstract void setSelection(int position);
	public void setEmptyView(View emptyView) {
		mEmptyView = emptyView;

		final T adapter = getAdapter();
		final boolean empty = ((adapter == null) || adapter.isEmpty());
		updateEmptyStatus(empty);
	}

	public View getEmptyView() {
		return mEmptyView;
	}

	boolean isInFilterMode() {
		return false;
	}

	/*
	@Override
	public void setFocusable(boolean focusable) {
		final T adapter = getAdapter();
		final boolean empty = adapter == null || adapter.getCount() == 0;

		mDesiredFocusableState = focusable;
		if (!focusable) {
			mDesiredFocusableInTouchModeState = false;
		}

		super.setFocusable(focusable && (!empty || isInFilterMode()));
	}

	@Override
	public void setFocusableInTouchMode(boolean focusable) {
		final T adapter = getAdapter();
		final boolean empty = adapter == null || adapter.getCount() == 0;

		mDesiredFocusableInTouchModeState = focusable;
		if (focusable) {
			mDesiredFocusableState = true;
		}

		super.setFocusableInTouchMode(focusable && (!empty || isInFilterMode()));
	}*/

	void checkFocus() {
		/*
		final T adapter = getAdapter();
		final boolean empty = adapter == null || adapter.getCount() == 0;
		final boolean focusable = !empty || isInFilterMode();
		
		
		super.setFocusableInTouchMode(focusable && mDesiredFocusableInTouchModeState);
		super.setFocusable(focusable && mDesiredFocusableState);
		if (mEmptyView != null) {
			updateEmptyStatus((adapter == null) || adapter.isEmpty());
		}*/
	}

	private void updateEmptyStatus(boolean empty) 
	{
		
		if (isInFilterMode()) {
			empty = false;
		}

		if (empty) {
			if (mEmptyView != null) {
				mEmptyView.setVisibility(View.VISIBLE);
				setVisibility(View.GONE);
			} else {
				
				setVisibility(View.VISIBLE);
			}
			
			
			if (mDataChanged) {
				this.layout(getLeft(), getTop(), getRight(), getBottom());
			}
		} else {
			if (mEmptyView != null) mEmptyView.setVisibility(View.GONE);
			setVisibility(View.VISIBLE);
		}
	}

	public Object getItemAtPosition(int position) {
		T adapter = getAdapter();
		return (adapter == null || position < 0) ? null : adapter.getItem(position);
	}

	public long getItemIdAtPosition(int position) {
		T adapter = getAdapter();
		return (adapter == null || position < 0) ? INVALID_ROW_ID : adapter.getItemId(position);
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		throw new RuntimeException("Don't call setOnClickListener for an AdapterView. "
				+ "You probably want setOnItemClickListener instead");
	}

	
	@Override
	protected void dispatchSaveInstanceState(SparseArray<Parcelable> container)
	{
		dispatchFreezeSelfOnly(container);
	}

	
	@Override
	protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container)
	{
		dispatchThawSelfOnly(container);
	}

	class AdapterDataSetObserver extends DataSetObserver {

		private Parcelable mInstanceState = null;

		@Override
		public void onChanged() 
		{
			
			mDataChanged = true;
			mOldItemCount = mItemCount;
			mItemCount = getAdapter().getCount();
		
			
			if (GridDynamicAdapterView.this.getAdapter().hasStableIds() && mInstanceState != null
					&& mOldItemCount == 0 && mItemCount > 0) {
				GridDynamicAdapterView.this.onRestoreInstanceState(mInstanceState);			
				mInstanceState = null;
			} else {
				rememberSyncState();
			}
			
			checkFocus();
			requestLayout();
			
		}

		@Override
		public void onInvalidated() 
		{
			
			mDataChanged = true;

			if (GridDynamicAdapterView.this.getAdapter().hasStableIds()) {				
				mInstanceState = GridDynamicAdapterView.this.onSaveInstanceState();
			}

			
			mOldItemCount = mItemCount;
			mItemCount = 0;
			mSelectedPosition = INVALID_POSITION;
			mSelectedRowId = INVALID_ROW_ID;
			mNextSelectedPosition = INVALID_POSITION;
			mNextSelectedRowId = INVALID_ROW_ID;
			mNeedSync = false;
			checkSelectionChanged();

			checkFocus();
			requestLayout();
		}

		public void clearSavedState() {
			mInstanceState = null;
		}
	}

	private class SelectionNotifier extends Handler implements Runnable {
		public void run() {
			if (mDataChanged) {
			
				post(this);
				
			} else {
				fireOnSelected();
			}
		}
	}

	void selectionChanged() {
		if (mOnItemSelectedListener != null) {
			if (mInLayout || mBlockLayoutRequests) {
			
				if (mSelectionNotifier == null) {
					mSelectionNotifier = new SelectionNotifier();
				}
				mSelectionNotifier.post(mSelectionNotifier);
			} else {
				fireOnSelected();
			}
		}
	}

	private void fireOnSelected() {
		if (mOnItemSelectedListener == null)
			return;

		int selection = this.getSelectedItemPosition();
		if (selection >= 0) {
			View v = getSelectedView();
			mOnItemSelectedListener.onItemSelected(this, v, selection,
					getAdapter().getItemId(selection));
		} else {
			mOnItemSelectedListener.onNothingSelected(this);
		}
	}

	
	@Override
	protected boolean canAnimate() {
		return super.canAnimate() && mItemCount > 0;
	}

	void handleDataChanged() {
		final int count = mItemCount;
		boolean found = false;

		if (count > 0) {

			int newPos;

			
			if (mNeedSync) {
		
				mNeedSync = false;

				newPos = findSyncPosition();
				if (newPos >= 0) {
					
					int selectablePos = lookForSelectablePosition(newPos, true);
					if (selectablePos == newPos) {
						
						setNextSelectedPositionInt(newPos);
						found = true;
					}
				}
			}
			if (!found) {
				
				newPos = getSelectedItemPosition();

				
				if (newPos >= count) {
					newPos = count - 1;
				}
				if (newPos < 0) {
					newPos = 0;
				}

				int selectablePos = lookForSelectablePosition(newPos, true);
				if (selectablePos < 0) {
					
					selectablePos = lookForSelectablePosition(newPos, false);
				}
				if (selectablePos >= 0) {
					setNextSelectedPositionInt(selectablePos);
					checkSelectionChanged();
					found = true;
				}
			}
		}
		if (!found) {
			
			mSelectedPosition = INVALID_POSITION;
			mSelectedRowId = INVALID_ROW_ID;
			mNextSelectedPosition = INVALID_POSITION;
			mNextSelectedRowId = INVALID_ROW_ID;
			mNeedSync = false;
			checkSelectionChanged();
		}
	}

	void checkSelectionChanged() 
	{
		if ((mSelectedPosition != mOldSelectedPosition) || (mSelectedRowId != mOldSelectedRowId)) {
			selectionChanged();
			mOldSelectedPosition = mSelectedPosition;
			mOldSelectedRowId = mSelectedRowId;
		}
	}


	int findSyncPosition() {
		int count = mItemCount;

		if (count == 0) {
			return INVALID_POSITION;
		}

		long idToMatch = mSyncRowId;
		int seed = mSyncPosition;
		
		if (idToMatch == INVALID_ROW_ID) {
			return INVALID_POSITION;
		}

		
		seed = Math.max(0, seed);
		seed = Math.min(count - 1, seed);

		long endTime = SystemClock.uptimeMillis() + SYNC_MAX_DURATION_MILLIS;

		long rowId;

		int first = seed;
		int last = seed;
		boolean next = false;
		boolean hitFirst;
		boolean hitLast;
		
		T adapter = getAdapter();
		if (adapter == null) {
			return INVALID_POSITION;
		}

		while (SystemClock.uptimeMillis() <= endTime) {
			rowId = adapter.getItemId(seed);
			if (rowId == idToMatch) {
				
				return seed;
			}

			hitLast = last == count - 1;
			hitFirst = first == 0;

			if (hitLast && hitFirst) {
				
				break;
			}

			if (hitFirst || (next && !hitLast)) {
				
				last++;
				seed = last;
				
				next = false;
			} else if (hitLast || (!next && !hitFirst)) {
				
				first--;
				seed = first;
				
				next = true;
			}

		}

		return INVALID_POSITION;
	}

	
	int lookForSelectablePosition(int position, boolean lookDown) {
		return position;
	}

	
	void setSelectedPositionInt(int position) {
		mSelectedPosition = position;
		mSelectedRowId = getItemIdAtPosition(position);
	}

	
	void setNextSelectedPositionInt(int position) {
		mNextSelectedPosition = position;
		mNextSelectedRowId = getItemIdAtPosition(position);
		// If we are trying to sync to the selection, update that too
		if (mNeedSync && mSyncMode == SYNC_SELECTED_POSITION && position >= 0) {
			mSyncPosition = position;
			mSyncRowId = mNextSelectedRowId;
		}
	}

	
	void rememberSyncState() {
		
		if (getChildCount() > 0) {
			mNeedSync = true;
			if (mIsVertical) {
				mSyncSize = mLayoutHeight;
			} else {
				mSyncSize = mLayoutWidth;
			}
			if (mSelectedPosition >= 0) {
				// Sync the selection state
				View v = getChildAt(mSelectedPosition - mFirstPosition);
				mSyncRowId = mNextSelectedRowId;
				mSyncPosition = mNextSelectedPosition;
				if (v != null) {
					if (mIsVertical) {
						mSpecificTop = v.getTop();
					} else {
						mSpecificTop = v.getLeft();
					}
				}
				mSyncMode = SYNC_SELECTED_POSITION;
			} else {
				
				View v = getChildAt(0);
				T adapter = getAdapter();
				if (mFirstPosition >= 0 && mFirstPosition < adapter.getCount()) {
					mSyncRowId = adapter.getItemId(mFirstPosition);
				} else {
					mSyncRowId = NO_ID;
				}
				mSyncPosition = mFirstPosition;
				if (v != null) {
					if (mIsVertical) {
						mSpecificTop = v.getTop();
					} else {
						mSpecificTop = v.getLeft();
					}
				}
				mSyncMode = SYNC_FIRST_POSITION;
			}
		}
	}

	
	public void offsetChildrenTopAndBottom(int offset) {
		final int count = getChildCount();

		for (int i = 0; i < count; i++) {
			final View v = getChildAt(i);
			v.offsetTopAndBottom(offset);
		}
	}

	
	public void offsetChildrenLeftAndRight(int offset) {
		final int count = getChildCount();

		int caruselW = 0;
		
		if(mIsCaruselScroller)
		{			
				final View v = getChildAt(0);
				if(v!=null)
					caruselW += v.getWidth()*3; 			
		}
		
		for (int i = 0; i < count; i++) 
		{
			final View v = getChildAt(i);
			v.offsetLeftAndRight(offset);
			
			if(mIsCaruselScroller)
			{
				int middleCaruselW = caruselW/2;
				int middleScreenW  = getWidth()/2;
				int leftOffset = v.getLeft();
				int middleView = leftOffset+v.getWidth()/2;
				int delta = Math.abs(middleScreenW - middleView);
				
				float offsetPercent = 0;
				
				if(delta==0)
				{
					offsetPercent = 0;
					
				}else
				{
                    /*
					offsetPercent = (float) delta / middleCaruselW;

					if(v instanceof PagerPackageView)
					{
						((PagerPackageView) v).offsetScrolled(Math.min(1, Math.max(0, 1 - offsetPercent)));
					}
					*/
				}				
				
			}
		}
	}

	protected void setIsVertical(boolean vertical) {
		mIsVertical = vertical;
	}

	protected boolean isVertical() {
		return mIsVertical;
	}
	

}
