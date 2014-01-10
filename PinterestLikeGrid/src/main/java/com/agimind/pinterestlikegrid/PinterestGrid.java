package com.agimind.pinterestlikegrid;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Pinterest Like Grid View
 */
public class PinterestGrid extends ViewGroup {
    private static final String TAG = "PinterestGrid";

    private static final int ADD_TO_BOTTOM = 1;
    private static final int ADD_TO_TOP = 2;
    private static final int GET_LOWEST = 3;
    private static final int GET_TOPPER = 4;

    private static final int INVALID_POSITION = -1;
    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DRAGGING = 1;
    private static final int TOUCH_MODE_FLINGING = 2;
    private static final int TOUCH_MODE_DOWN = 3;
    private static final int TOUCH_MODE_TAP = 4;
    private static final int TOUCH_MODE_DONE_WAITING = 5;
    private static final int TOUCH_MODE_REST = 6;

    private int colsNum;
    private ArrayList<Column> columns;
    private HeaderViewListAdapter adapter;
    private Rect listPadding = new Rect();
    private int colWidth;
    private int itemMargin;
    private OnItemClickListener onItemClickListener;
    private boolean dataChanged = false;
    private int itemsCount;
    private int firstPosition;
    private int touchMode;
    private final VelocityTracker velocityTracker = VelocityTracker.obtain();
    private int maximumVelocity;
    private float lastTouchY;
    private float touchRemainderY;
    private int activePointerId;
    private int touchSlop;
    private int flingVelocity;
    private OverScroller scroller;
    private Rect touchFrame;
    private PerformClick performClick;
    private RecycleBin recycler = new RecycleBin();
    private SparseArray<LayoutRecord> layoutRecords = new SparseArray<LayoutRecord>();
    private boolean populating;

    private GestureDetectorCompat gestureDetector;
    private GestureDetector.SimpleOnGestureListener gestureDetectorListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, String.format("%s", e.toString()));
            scroller.forceFinished(true);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, String.format("distance %s %s", distanceX, distanceY));
            int viewPortBottom = getHeight() - getPaddingBottom();
            int areaBottom = getAreaBottom();
            int viewPortTop = 0;
            int areaTop = getAreaTop();

            int offset = (int) -distanceY;

            boolean topStopper = viewPortTop < areaTop + offset;
            if(topStopper){
                offset = Math.abs(viewPortTop - areaTop);
            }

            boolean bottomStopper = viewPortBottom > areaBottom + offset;
            if(bottomStopper){
                offset = -Math.abs(viewPortBottom - areaBottom);
            }

            offsetChildren(offset);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, String.format("velocity %s %s", velocityX, velocityY));
//            fling((int) -velocityX, (int) -velocityY);
            return true;
        }
    };

    private void fling(int velocityX, int velocityY){
        scroller.forceFinished(true);
        int startY = (int) 0;
        scroller.fling(0, 20, 0, velocityY, 0, 0, 0, 3000, 0, 0);
        ViewCompat.postInvalidateOnAnimation(this);
    }


    public PinterestGrid(Context context) {
        super(context);
        init(context);
    }

    public PinterestGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PinterestGrid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context){
        colsNum = 3;
        listPadding.set(10,10,10,10);
        itemMargin = 10;

        final ViewConfiguration vc = ViewConfiguration.get(getContext());
        touchSlop = vc.getScaledTouchSlop();
        maximumVelocity = vc.getScaledMaximumFlingVelocity();
        flingVelocity = vc.getScaledMinimumFlingVelocity();
        scroller = new OverScroller(context);
        
        gestureDetector = new GestureDetectorCompat(context, gestureDetectorListener);

        initColumns();
    }

    private void initColumns() {
        columns = new ArrayList<Column>();
        for (int i = 0; i < colsNum; i++) {
            columns.add(new Column(i));
        }
    }

    public void setAdapter(HeaderViewListAdapter adapter){
        this.adapter = adapter;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        colWidth = (getMeasuredWidth() - listPadding.left - listPadding.right - itemMargin*(colsNum - 1)) / colsNum;
        for (Column column : columns) {
            column.setLayout(colWidth);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        Log.d(TAG, "onLayout");
//        if (!changed){
//            return;
//        }

        layoutChildren();
        //fillUp(firstPosition);
        fillDown(firstPosition + getChildCount(), 0);

//        itemsCount = this.adapter.getCount();
//        for (int i = 0; i < itemsCount; i++) {
//            // todo: get cashed view
//            View child = adapter.getView(i, null, null);
//            ViewGroup.LayoutParams lp = child.getLayoutParams();
//            if (lp == null){
//                lp = generateDefaultLayoutParams();
//            }
//
//            if (adapter.getItemViewType(i) == AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER){
//                Column col = getNextColToAdd(GET_LOWEST);
//
//                lp.width = LayoutParams.MATCH_PARENT;
//                lp.height = LayoutParams.WRAP_CONTENT;
//                addViewInLayout(child, -1, lp);
//
//                int childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
//                int childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
//
//                child.measure(childWidthSpec, childHeightSpec);
//
//                int left = listPadding.left;
//                int top = col.bottomEnd + itemMargin;
//                int right = getMeasuredWidth() - listPadding.right;
//                int bottom = top + child.getMeasuredHeight();
//
//                child.layout(left, top, right, bottom);
//            } else {
//                Column col = getNextColToAdd(ADD_TO_BOTTOM);
//
//                addViewInLayout(child, -1, lp);
//
//                int childWidthSize = colWidth;
//                int childWidthSpec = MeasureSpec.makeMeasureSpec(colWidth, MeasureSpec.EXACTLY);
//
//                int childHeightSize = lp.height;
//                int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);
//
//                child.measure(childWidthSpec, childHeightSpec);
//
//                int left = col.left;
//                int top = col.bottomEnd + itemMargin;
//                int right = col.right;
//                int bottom = top + child.getMeasuredHeight();
//
//                child.layout(left, top, right, bottom);
//
//                col.bottomEnd = bottom;
//            }
//        }
    }

//    private void fillUp(int fromPosition, int overhang) {
//        int gridTop = getPaddingTop();
//        int fillTo = gridTop - overhang;
//
//        Column column = getNextColToAdd(ADD_TO_TOP);
//        int position = fromPosition;
//        while(column.topEnd > fillTo && position >= 0){
//            View child = obtainView(position, null);
//            if (child == null) continue;
//
//            LayoutParams lp = (LayoutParams) child.getLayoutParams();
//
//            addView(child);
//
//            measureChild(child, lp);
//
//            //todo...
//
//            position--;
//            column = getNextColToAdd(ADD_TO_TOP);
//        }
//    }

    private int fillDown(int fromPosition, int overhang) {
        int gridBottom = getHeight() - getPaddingBottom();
        final int fillTo = gridBottom + overhang;

        Column column = getCol(ADD_TO_BOTTOM);
        int position = fromPosition;
        itemsCount = adapter.getCount();
        while(column.bottomEnd < fillTo && position < adapter.getCount()){
            View child = obtainView(position, null);
            if (child == null) continue;

            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            addView(child);

            if (lp.isFooterOrHeader()){
                measureFooter(child, lp);

                int lowest = getCol(GET_LOWEST).bottomEnd;

                int left = listPadding.left;
                int top = lowest + itemMargin;
                int right = getMeasuredWidth() - listPadding.right;
                int bottom = top + child.getMeasuredHeight();

                child.layout(left, top, right, bottom);
            } else {
                measureChild(child, lp);

                LayoutRecord rec = layoutRecords.get(position);
                if (rec == null){
                    rec = new LayoutRecord(child.getMeasuredWidth(), child.getMeasuredHeight(), column, position);
                    layoutRecords.put(position, rec);
                }

                int left = column.left;
                int top = column.bottomEnd + itemMargin;
                int right = column.right;
                int bottom = top + child.getMeasuredHeight();

                child.layout(left, top, right, bottom);

                column.bottomEnd = bottom;
            }

            position++;
            column = getCol(ADD_TO_BOTTOM);
        }

        int lowestView = getAreaBottom();

        return lowestView - gridBottom;
    }

    private int getAreaBottom() {
        Column column;
        int lowestView = 0;
        View lastView = getChildAt(getChildCount() - 1);
        LayoutParams lastViewLp = (LayoutParams) lastView.getLayoutParams();
        if (lastViewLp.isFooterOrHeader()){
            lowestView = lastView.getBottom();
        } else {
            column = getCol(GET_LOWEST);
            lowestView = column.bottomEnd;
        }
        return lowestView;
    }

    private int getAreaTop() {
        Column column = getCol(GET_TOPPER);
        return column.topEnd;
    }

    private void resetCols(){
        for (Column column : columns) {
            column.bottomEnd = 0;
            column.topEnd = 0;
        }
    }

    private void layoutChildren() {
        resetCols();

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final boolean needsLayout = child.isLayoutRequested();

            if(lp.isFooterOrHeader()){
                if (needsLayout){
                    measureFooter(child, lp);
                }

                int lowest = getCol(GET_LOWEST).bottomEnd;

                int left = listPadding.left;
                int top = lowest + itemMargin;
                int right = getMeasuredWidth() - listPadding.right;
                int bottom = top + child.getMeasuredHeight();

                child.layout(left, top, right, bottom);

            } else {
                if (needsLayout){
                    measureChild(child, lp);
                }

                LayoutRecord rec = layoutRecords.get(firstPosition + i);
                assert rec != null;
                Column column = rec.column;

                int left = column.left;
                int top = column.bottomEnd + itemMargin;
                int right = column.right;
                int bottom = top + child.getMeasuredHeight();

                child.layout(left, top, right, bottom);

                column.bottomEnd = bottom;
            }

        }

    }

    private void measureChild(View child, LayoutParams lp){
        int childWidthSize = colWidth;
        int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);

        int childHeightSize = lp.height;
        int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

        child.measure(childWidthSpec, childHeightSpec);
    }

    private void measureFooter(View child, LayoutParams lp){
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.WRAP_CONTENT;

        int childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);

        child.measure(childWidthSpec, childHeightSpec);
    }


    private Column getCol(int mode) {
        Column result = columns.get(0);

        switch (mode){
            case ADD_TO_BOTTOM:
                for (Column column : columns) {
                    if (result.bottomEnd > column.bottomEnd){
                        result = column;
                    }
                }
                break;
            case GET_LOWEST:
                for (Column column : columns) {
                    if (result.bottomEnd < column.bottomEnd){
                        result = column;
                    }
                }
                break;
            case GET_TOPPER:
                for (Column column : columns) {
                    if (result.topEnd < column.topEnd){
                        result = column;
                    }
                }
                break;
            case ADD_TO_TOP:
                for (Column column : columns) {
                    if (result.topEnd > column.topEnd){
                        result = column;
                    }
                }
                break;
        }

        return result;
    }

    /**
     * Obtain a populated view from the adapter. If optScrap is non-null and is not
     * reused it will be placed in the recycle bin.
     *
     * @param position position to get view for
     * @param optScrap Optional scrap view; will be reused if possible
     * @return A new view, a recycled view from mRecycler, or optScrap
     */
    final View obtainView(int position, View optScrap) {
        View view = recycler.getTransientStateView(position);
        if (view != null) {
            return view;
        }

        if(position >= adapter.getCount()){
            view = null;
            return null;
        }

        // Reuse optScrap if it's of the right type (and not null)
        final int optType = optScrap != null ? ((LayoutParams) optScrap.getLayoutParams()).viewType : -1;
        final int positionViewType = adapter.getItemViewType(position);
        final View scrap = optType == positionViewType ?
                optScrap : recycler.getScrapView(positionViewType);

        view = adapter.getView(position, scrap, this);

        if (view != scrap && scrap != null) {
            // The adapter didn't use it; put it back.
            recycler.addScrap(scrap);
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();

        if (view.getParent() != this) {
            if (lp == null) {
                lp = generateDefaultLayoutParams();
            } else if (!checkLayoutParams(lp)) {
                lp = generateLayoutParams(lp);
            }
        }

        final LayoutParams sglp = (LayoutParams) lp;
        sglp.position = position;
        sglp.viewType = positionViewType;

        //Set the updated LayoutParam before returning the view.
        view.setLayoutParams(sglp);
        return view;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        velocityTracker.addMovement(ev);
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                velocityTracker.clear();
                // todo mScroller.abortAnimation();
                lastTouchY = ev.getY();
                activePointerId = MotionEventCompat.getPointerId(ev, 0);
                touchRemainderY = 0;
                if (touchMode == TOUCH_MODE_FLINGING) {
                    // Catch!
                    touchMode = TOUCH_MODE_DRAGGING;
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
                if (index < 0) {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id " +
                            activePointerId + " - did StaggeredGridView receive an inconsistent " +
                            "event stream?");
                    return false;
                }
                final float y = MotionEventCompat.getY(ev, index);
                final float dy = y - lastTouchY + touchRemainderY;
                final int deltaY = (int) dy;
                touchRemainderY = dy - deltaY;

                if (Math.abs(dy) > touchSlop) {
                    touchMode = TOUCH_MODE_DRAGGING;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return gestureDetector.onTouchEvent(ev);
//        velocityTracker.addMovement(ev);
//        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
//
//        int motionPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
//
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                touchMode = TOUCH_MODE_DOWN;
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
//                if (index < 0) {
//                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id " +
//                            activePointerId + " - did StaggeredGridView receive an inconsistent " +
//                            "event stream?");
//                    return false;
//                }
//                final float y = MotionEventCompat.getY(ev, index);
//                final float dy = y - lastTouchY + touchRemainderY;
//                final int deltaY = (int) dy;
//                touchRemainderY = dy - deltaY;
//
//                if (Math.abs(dy) > touchSlop) {
//                    touchMode = TOUCH_MODE_DRAGGING;
//                }
//
//                if (touchMode == TOUCH_MODE_DRAGGING) {
//                    lastTouchY = y;
//
//                    if (!trackMotionScroll(deltaY, true)) {
//                        // Break fling velocity if we impacted an edge.
//                        velocityTracker.clear();
//                    }
//                }
//
//                break;
//
//            case MotionEvent.ACTION_CANCEL:
//                break;
//
//            case MotionEvent.ACTION_UP: {
//                velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
//                final float velocity = VelocityTrackerCompat.getYVelocity(velocityTracker, activePointerId);
//                final int prevTouchMode = touchMode;
//
//                if (Math.abs(velocity) > flingVelocity) { // TODO
//                    touchMode = TOUCH_MODE_FLINGING;
//                    scroller.fling(0, 0, 0, (int) velocity, 0, 0,
//                            Integer.MIN_VALUE, Integer.MAX_VALUE);
//                    lastTouchY = 0;
//                    invalidate();
//                } else {
//                    touchMode = TOUCH_MODE_IDLE;
//                }
//
//                if (!dataChanged && adapter!=null && motionPosition != INVALID_POSITION && adapter.isEnabled(motionPosition)) {
//                    // TODO : handle
//                    touchMode = TOUCH_MODE_TAP;
//                } else {
//                    touchMode = TOUCH_MODE_REST;
//                }
//
//                switch(prevTouchMode){
//                    case TOUCH_MODE_DOWN:
//                    case TOUCH_MODE_TAP:
//                    case TOUCH_MODE_DONE_WAITING:
//                        final View child = getChildAt(motionPosition - firstPosition);
//                        final float x = ev.getX();
//                        final boolean inList = x > getPaddingLeft() && x < getWidth() - getPaddingRight();
//                        if (child != null && !child.hasFocusable() && inList) {
//                            if (touchMode != TOUCH_MODE_DOWN) {
//                                child.setPressed(false);
//                            }
//
//                            if (performClick == null) {
//                                invalidate();
//                                performClick = new PerformClick();
//                            }
//
//                            final PerformClick performClick = this.performClick;
//                            performClick.mClickMotionPosition = motionPosition;
//                            performClick.rememberWindowAttachCount();
//
//                            performClick.run();
//                        }
//
//                        touchMode = TOUCH_MODE_REST;
//                }
//            } break;
//        }
//        return true;
    }

    public void computeScroll() {
        super.computeScroll();

        boolean needsInvalidate = false;

        if(scroller.computeScrollOffset()){

        }
    }

    private int getEntireAreaHeight(){
        final int count = itemsCount;
        int height = 0;
        for (int i = 0; i < count; i++) {
            LayoutRecord rec = layoutRecords.get(i);
            height += rec.height + itemMargin;
        }
        return height;
    }

    /**
     *
     * @param deltaY Pixels that content should move by
     * @return true if the movement completed, false if it was stopped prematurely.
     */
    private boolean trackMotionScroll(int deltaY, boolean allowOverScroll) {
        final boolean contentFits = contentFits();
        final int allowOverhang = Math.abs(deltaY);

        final int overScrolledBy;
        final int movedBy;
        if (!contentFits) {
            final int overhang;
            final boolean up;
            populating = true;
            if (deltaY > 0) {
                overhang = 0; //fillUp(mFirstPosition - 1, allowOverhang)+ mItemMargin;
                up = true;
            } else {
                overhang = fillDown(firstPosition + getChildCount(), allowOverhang);
                up = false;
            }
            movedBy = Math.min(overhang, allowOverhang);
            offsetChildren(up ? movedBy : -movedBy);
            recycleOffscreenViews();
            populating = false;
            overScrolledBy = allowOverhang - overhang;
            invalidate();
        } else {
            overScrolledBy = allowOverhang;
            movedBy = 0;
        }

//        if (allowOverScroll) {
//            final int overScrollMode = ViewCompat.getOverScrollMode(this);
//
//            if (overScrollMode == ViewCompat.OVER_SCROLL_ALWAYS ||
//                    (overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS && !contentFits)) {
//                if (overScrolledBy > 0) {
//                    EdgeEffectCompat edge = deltaY > 0 ? mTopEdge : mBottomEdge;
//                    edge.onPull((float) Math.abs(deltaY) / getHeight());
//                    invalidate();
//                }
//            }
//        }

        return deltaY == 0 || movedBy != 0;
    }

    private final boolean contentFits() {
        if (firstPosition != 0 || getChildCount() != itemsCount) {
            return false;
        }

        Column topmost = getCol(GET_TOPPER);
        Column bottommost = getCol(GET_LOWEST);

        return topmost.topEnd >= getPaddingTop() && bottommost.bottomEnd <= getHeight() - getPaddingBottom();
    }

    final void offsetChildren(int offset) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            child.layout(child.getLeft(), child.getTop() + offset,
                    child.getRight(), child.getBottom() + offset);
        }

        for (Column column : columns) {
            column.topEnd += offset;
            column.bottomEnd += offset;
        }
    }

    /**
     * Important: this method will leave offscreen views attached if they
     * are required to maintain the invariant that child view with index i
     * is always the view corresponding to position mFirstPosition + i.
     */
    private void recycleOffscreenViews() {
        final int height = getHeight();
        final int clearAbove = -itemMargin;
        final int clearBelow = height + itemMargin;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getTop() <= clearBelow)  {
                // There may be other offscreen views, but we need to maintain
                // the invariant documented above.
                break;
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!lp.isFooterOrHeader()){
                final LayoutRecord rec = layoutRecords.get(firstPosition + i);
                Column column = rec.column;
                column.bottomEnd = child.getTop() + itemMargin;
            }

            removeViewAt(i);
            recycler.addScrap(child);
        }

        while (getChildCount() > 0) {
            final View child = getChildAt(0);
            if (child.getBottom() >= clearAbove) {
                // There may be other offscreen views, but we need to maintain
                // the invariant documented above.
                break;
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!lp.isFooterOrHeader()){
                final LayoutRecord rec = layoutRecords.get(firstPosition);
                Column column = rec.column;
                column.topEnd = child.getBottom();
            }

            removeViewAt(0);
            recycler.addScrap(child);
            firstPosition++;
        }
    }


    /**
     * Maps a point to a position in the list.
     *
     * @param x X in local coordinate
     * @param y Y in local coordinate
     * @return The position of the item which contains the specified point, or
     *         {@link #INVALID_POSITION} if the point does not intersect an item.
     */
    public int pointToPosition(int x, int y) {
        Rect frame = touchFrame;
        if (frame == null) {
            touchFrame = new Rect();
            frame = touchFrame;
        }

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return firstPosition + i;
                }
            }
        }
        return INVALID_POSITION;
    }

    public boolean performItemClick(View view, int position, long id) {
        if (onItemClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            if (view != null) {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
            onItemClickListener.onItemClick(this, view, position, id);
            return true;
        }

        return false;
    }


    private class PerformClick extends WindowRunnnable implements Runnable {
        int mClickMotionPosition;

        public void run() {
            // The data has changed since we posted this action in the event queue,
            // bail out before bad things happen
            if (dataChanged) return;

            final ListAdapter wrappedAdapter = adapter.getWrappedAdapter();
            final int motionPosition = mClickMotionPosition;
            if (adapter != null && adapter.getCount() > 0 &&
                    motionPosition != INVALID_POSITION &&
                    motionPosition < adapter.getCount() && sameWindow()) {
                final View view = getChildAt(motionPosition - firstPosition);
                // If there is no view, something bad happened (the view scrolled off the
                // screen, etc.) and we should cancel the click
                if (view != null) {
                    performItemClick(view, motionPosition, adapter.getItemId(motionPosition));
                }
            }
        }
    }


    /**
     * A base class for Runnables that will check that their view is still attached to
     * the original window as when the Runnable was created.
     *
     */
    private class WindowRunnnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return hasWindowFocus() && getWindowAttachCount() == mOriginalAttachCount;
        }
    }

    private class Column {
        int order;
        int bottomEnd;
        int topEnd;
        int left;
        int right;

        private Column(int order) {
            this.order = order;
            bottomEnd = 0;
            topEnd = 0;
        }

        public void setLayout(int width) {
            left = listPadding.left + (width + itemMargin) * order;
            right = left + width;
        }
    }

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been clicked.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    /**
     * @return The callback to be invoked with an item in this AdapterView has
     *         been clicked, or null id no callback has been set.
     */
    public final OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public interface OnItemClickListener {

        /**
         * Callback method to be invoked when an item in this AdapterView has
         * been clicked.
         * <p>
         * Implementers can call getItemAtPosition(position) if they need
         * to access the data associated with the selected item.
         *
         * @param parent The AdapterView where the click happened.
         * @param view The view within the AdapterView that was clicked (this
         *            will be a view provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id The row id of the item that was clicked.
         */
        void onItemClick(PinterestGrid parent, View view, int position, long id);
    }

    private class RecycleBin {
        private Map<Integer, ArrayList<View>> mScrapViews = new HashMap<Integer, ArrayList<View>>();
        private int mMaxScrap;
        private SparseArray<View> mTransientStateViews;

        public void clear() {
            if (mScrapViews != null){
                mScrapViews.clear();
            }
            clearTransientViews();
        }

        public void clearTransientViews() {
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        public void addScrap(View v) {
            final LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (ViewCompat.hasTransientState(v)) {
                if (mTransientStateViews == null) {
                    mTransientStateViews = new SparseArray<View>();
                }
                mTransientStateViews.put(lp.position, v);
                return;
            }

            final int childCount = getChildCount();
            if (childCount > mMaxScrap) {
                mMaxScrap = childCount;
            }


            ArrayList<View> scrap = mScrapViews.get(lp.viewType);
            if (scrap == null){
                scrap = new ArrayList<View>();
                mScrapViews.put(lp.viewType, scrap);
            }

            if (scrap.size() < mMaxScrap) {
                scrap.add(v);
            }
        }

        public View getTransientStateView(int position) {
            if (mTransientStateViews == null) {
                return null;
            }

            final View result = mTransientStateViews.get(position);
            if (result != null) {
                mTransientStateViews.remove(position);
            }
            return result;
        }

        public View getScrapView(int type) {
            ArrayList<View> scrap = mScrapViews.get(type);
            if (scrap == null || scrap.isEmpty()) {
                return null;
            }

            final int index = scrap.size() - 1;
            final View result = scrap.get(index);
            scrap.remove(index);
            return result;
        }
    }

    public class LayoutRecord {
        int width;
        int height;
        Column column;
        int position;

        public LayoutRecord(int width, int height, Column column, int position) {
            this.width = width;
            this.height = height;
            this.column = column;
            this.position = position;
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        private static final int[] LAYOUT_ATTRS = new int[] {
                android.R.attr.layout_span
        };

        private static final int SPAN_INDEX = 0;

        /**
         * The number of columns this item should span
         */
        public int span = 1;

        /**
         * Item position this view represents
         */
        int position;

        /**
         * Type of this view as reported by the adapter
         */
        int viewType;

        /**
         * The column this view is occupying
         */
        int column;

        /**
         * The stable ID of the item this view displays
         */
        long id = -1;

        public LayoutParams(int height) {
            super(MATCH_PARENT, height);

            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with height FILL_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            if (this.width != MATCH_PARENT) {
                Log.w(TAG, "Inflation setting LayoutParams width to " + this.width +
                        " - must be MATCH_PARENT");
                this.width = MATCH_PARENT;
            }
            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Inflation setting LayoutParams height to MATCH_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }

            TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            span = a.getInteger(SPAN_INDEX, 1);
            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);

            if (this.width != MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with width " + this.width +
                        " - must be MATCH_PARENT");
                this.width = MATCH_PARENT;
            }
            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with height MATCH_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }
        }

        public boolean isFooterOrHeader(){
            return viewType == AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
        }
    }
}
