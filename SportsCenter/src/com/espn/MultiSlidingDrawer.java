/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.espn;

//import android.R;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.espn.ScoreCenter.R;

/**
 * MultiSlidingDrawer hides content out of the screen and allows the user to drag a handle
 * to bring the content on screen. MultiSlidingDrawer can be used vertically or horizontally.
 *
 * A special widget composed of two children views: the handle, that the users drags,
 * and the content, attached to the handle and dragged with it.
 *
 * MultiSlidingDrawer should be used as an overlay inside layouts. This means MultiSlidingDrawer
 * should only be used inside of a FrameLayout or a RelativeLayout for instance. The
 * size of the SlidingDrawer defines how much space the content will occupy once slid
 * out so MultiSlidingDrawer should usually use match_parent for both its dimensions.
 *
 * Inside an XML layout, MultiSlidingDrawer must define the id of the handle and of the
 * content:
 *
 * <pre class="prettyprint">
 * &lt;MultiSlidingDrawer
 *     android:id="@+id/drawer"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *
 *     android:handle="@+id/handle"
 *     android:content="@+id/content"&gt;
 *
 *     &lt;ImageView
 *         android:id="@id/handle"
 *         android:layout_width="88dip"
 *         android:layout_height="44dip" /&gt;
 *
 *     &lt;GridView
 *         android:id="@id/content"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent" /&gt;
 *
 * &lt;/MultiSlidingDrawer&gt;
 * </pre>
 *
 * @attr ref android.R.styleable#MultiSlidingDrawer_content
 * @attr ref android.R.styleable#MultiSlidingDrawer_handle
 * @attr ref android.R.styleable#MultiSlidingDrawer_topOffset
 * @attr ref android.R.styleable#MultiSlidingDrawer_bottomOffset
 * @attr ref android.R.styleable#MultiSlidingDrawer_orientation
 * @attr ref android.R.styleable#MultiSlidingDrawer_allowSingleTap
 * @attr ref android.R.styleable#MultiSlidingDrawer_animateOnClick
 */
public class MultiSlidingDrawer extends ViewGroup {
	private static final int ANCHOR_TOP = 0;
	private static final int ANCHOR_RIGHT = 1;
	private static final int ANCHOR_BOTTOM = 2;
	private static final int ANCHOR_LEFT = 3;
	
		
	private int getEffectiveTop(View view, int anchor) {
		anchor = anchor % 4;
		switch (anchor) {
		case ANCHOR_TOP:
			return view.getBottom();
		case ANCHOR_RIGHT:
			return view.getLeft();
		case ANCHOR_BOTTOM:
			return view.getTop();
		case ANCHOR_LEFT:
			return view.getRight();
		}
		
		throw new IllegalStateException("invalid anchor value");
	}
	
	public int getEffectiveTop(View view) {
		return getEffectiveTop(view, mAnchor);
	}
	
	public int getEffectiveRight(View view) {
		return getEffectiveTop(view, mAnchor + 1);
	}
	
	public int getEffectiveBottom(View view) {
		return getEffectiveTop(view, mAnchor + 2);
	}
	
	public int getEffectiveLeft(View view) {
		return getEffectiveTop(view, mAnchor + 3);
	}
	
	public int getEffectiveHeight(View view) {
		if (mAnchor == ANCHOR_TOP || mAnchor == ANCHOR_BOTTOM)
			return view.getHeight();
		return view.getWidth();
	}
	
	public int getEffectiveWidth(View view) {
		if (mAnchor == ANCHOR_TOP || mAnchor == ANCHOR_BOTTOM)
			return view.getWidth();
		return view.getHeight();
	}

	
    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;

    private static final int TAP_THRESHOLD = 6;
    private static final float MAXIMUM_TAP_VELOCITY = 100.0f;
    private static final float MAXIMUM_MINOR_VELOCITY = 150.0f;
    private static final float MAXIMUM_MAJOR_VELOCITY = 200.0f;
    private static final float MAXIMUM_ACCELERATION = 2000.0f;
    private static final int VELOCITY_UNITS = 1000;
    private static final int MSG_ANIMATE = 1000;
    private static final int ANIMATION_FRAME_DURATION = 1000 / 60;

    private static final int EXPANDED_FULL_OPEN = -10001;
    private static final int COLLAPSED_FULL_CLOSED = -10002;
   

    private ArrayList<Integer> mHandleIds;
    private ArrayList<Integer> mContentIds;

    private ArrayList<View> mHandles;
    private ArrayList<View> mContents;
    
    private ArrayList<Integer> minHandleTops;
    private ArrayList<Integer> maxHandleTops;

    private final Rect mInvalidate = new Rect();
    private final Rect frame = new Rect();
    private int mTracking;	// -1 = not tracking, else index of item being tracked
    private boolean mLocked;

    private VelocityTracker mVelocityTracker;

    private boolean mVertical;
    private int mAnchor;
    private int mExpanded; // -1 = not expanded, else the index of expanded view
    private int mTopOffset;

    private OnDrawerOpenListener mOnDrawerOpenListener;
    private OnDrawerCloseListener mOnDrawerCloseListener;
    private OnDrawerScrollListener mOnDrawerScrollListener;

    private final Handler mHandler = new SlidingHandler();
    private float mAnimatedAcceleration;
    private float mAnimatedVelocity;
    private float mAnimationPosition;
    private long mAnimationLastTime;
    private long mCurrentAnimationTime;
    private int mTouchDelta;
    private int mAnimating;  
    private boolean mAllowSingleTap;
    private boolean mAnimateOnClick;
    private boolean mLockOpen;	// if true, first drawer will always be open
    private boolean mLockClosed; // if true, last drawer will always be closed

    private final int mTapThreshold;
    private final int mMaximumTapVelocity;
    private final int mMaximumMinorVelocity;
    private final int mMaximumMajorVelocity;
    private final int mMaximumAcceleration;
    private final int mVelocityUnits;

    /**
     * Callback invoked when the drawer is opened.
     */
    public static interface OnDrawerOpenListener {
        /**
         * Invoked when the drawer becomes fully open.
         */
        public void onDrawerOpened(int idx);
    }

    /**
     * Callback invoked when the drawer is closed.
     */
    public static interface OnDrawerCloseListener {
        /**
         * Invoked when the drawer becomes fully closed.
         */
        public void onDrawerClosed(int idx);
    }

    /**
     * Callback invoked when the drawer is scrolled.
     */
    public static interface OnDrawerScrollListener {
        /**
         * Invoked when the user starts dragging/flinging the drawer's handle.
         */
        public void onScrollStarted();

        /**
         * Invoked when the user stops dragging/flinging the drawer's handle.
         */
        public void onScrollEnded();
    }

    /**
     * Creates a new MultiSlidingDrawer from a specified set of attributes defined in XML.
     *
     * @param context The application's environment.
     * @param attrs The attributes defined in XML.
     */
    public MultiSlidingDrawer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Creates a new MultiSlidingDrawer from a specified set of attributes defined in XML.
     *
     * @param context The application's environment.
     * @param attrs The attributes defined in XML.
     * @param defStyle The style to apply to this widget.
     */
    public MultiSlidingDrawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.v("MSD", "msd constructor");
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiSlidingDrawer, defStyle, 0);

        mHandleIds = new ArrayList<Integer>();
        mContentIds = new ArrayList<Integer>();
        mHandles = new ArrayList<View>();
        mContents = new ArrayList<View>();
        minHandleTops = new ArrayList<Integer>();
        maxHandleTops = new ArrayList<Integer>();
        
        mTracking = -1;
        mExpanded = -1;
        mAnimating = -1;
        
        mAnchor = a.getInt(R.styleable.MultiSlidingDrawer_anchor, -1);
        int orientation = a.getInt(R.styleable.MultiSlidingDrawer_orientation, -1);
        if (mAnchor != -1 && orientation != -1) {
        	throw new IllegalArgumentException("May not specify both orientation and anchor");
        }
        
        if (orientation != -1) {
        	mVertical = orientation == ORIENTATION_VERTICAL;
        	mAnchor = mVertical ? ANCHOR_BOTTOM : ANCHOR_RIGHT;
        } 
        
        if (mAnchor == -1) {
        	mAnchor = ANCHOR_BOTTOM; // default if nothing is specified
        }
        
        mVertical = (mAnchor == ANCHOR_BOTTOM || mAnchor == ANCHOR_TOP) ?  true : false;        
        
        mTopOffset = (int) a.getDimension(R.styleable.MultiSlidingDrawer_topOffset, 0.0f);
        mAllowSingleTap = a.getBoolean(R.styleable.MultiSlidingDrawer_allowSingleTap, true);
        mAnimateOnClick = a.getBoolean(R.styleable.MultiSlidingDrawer_animateOnClick, true);        
        mLockOpen = a.getBoolean(R.styleable.MultiSlidingDrawer_lockOpen, false);
        mLockClosed = a.getBoolean(R.styleable.MultiSlidingDrawer_lockClosed, false);

        // TODO: Can this be an array in resources?
        int handleId = a.getResourceId(R.styleable.MultiSlidingDrawer_handle1, 0);
        int contentId = a.getResourceId(R.styleable.MultiSlidingDrawer_content1, 0);
        
        if (handleId == 0) {
            throw new IllegalArgumentException("A handle attribute is required and must refer "
                    + "to a valid child.");
        }
        
        if (contentId == 0) {
            throw new IllegalArgumentException("A content attribute is required and must refer "
                    + "to a valid child.");
        }
        
        mHandleIds.add(handleId);
        mContentIds.add(contentId);
        
        handleId = a.getResourceId(R.styleable.MultiSlidingDrawer_handle2, 0);
        contentId = a.getResourceId(R.styleable.MultiSlidingDrawer_content2, 0);
        
        if (handleId != 0 && contentId != 0)
        {
        	mHandleIds.add(handleId);
            mContentIds.add(contentId);
        }

        handleId = a.getResourceId(R.styleable.MultiSlidingDrawer_handle3, 0);
        contentId = a.getResourceId(R.styleable.MultiSlidingDrawer_content3, 0);
        
        if (handleId != 0 && contentId != 0)
        {
        	mHandleIds.add(handleId);
            mContentIds.add(contentId);
        }
        
        handleId = a.getResourceId(R.styleable.MultiSlidingDrawer_handle4, 0);
        contentId = a.getResourceId(R.styleable.MultiSlidingDrawer_content4, 0);
        
        if (handleId != 0 && contentId != 0)
        {
        	mHandleIds.add(handleId);
            mContentIds.add(contentId);
        }              
        
        final float density = getResources().getDisplayMetrics().density;
        mTapThreshold = (int) (TAP_THRESHOLD * density + 0.5f);
        mMaximumTapVelocity = (int) (MAXIMUM_TAP_VELOCITY * density + 0.5f);
        mMaximumMinorVelocity = (int) (MAXIMUM_MINOR_VELOCITY * density + 0.5f);
        mMaximumMajorVelocity = (int) (MAXIMUM_MAJOR_VELOCITY * density + 0.5f);
        mMaximumAcceleration = (int) (MAXIMUM_ACCELERATION * density + 0.5f);
        mVelocityUnits = (int) (VELOCITY_UNITS * density + 0.5f);

        a.recycle();

        setAlwaysDrawnWithCacheEnabled(false);
    }

    @Override
    protected void onFinishInflate() {    	
    	for (int i = 0; i <  mHandleIds.size(); i++)
    	{       		
    		final View handle = findViewById(mHandleIds.get(i));	        
	        if (handle == null) {
	            throw new IllegalArgumentException("The handle attribute must refer to an"
	                    + " existing child.");
	        }
	        mHandles.add(handle);
	        if (!(i == 0 && mLockOpen || i == mHandleIds.size() - 1 && mLockClosed)) {
	        	handle.setOnClickListener(new DrawerToggler());
	        }
	        
	        mContents.add(findViewById(mContentIds.get(i)));
	        if (mContents.get(i) == null) {
	            throw new IllegalArgumentException("The content attribute must refer to an"
	                    + " existing child.");
	        }
	        
	        mContents.get(i).setVisibility(View.GONE);
	        
	        // Placeholders
	        maxHandleTops.add(0);
	        minHandleTops.add(0);
	        
	        if (mLockOpen) {
	        	openDrawer(0);
	        }
    	}
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("MultiSlidingDrawer cannot have UNSPECIFIED dimensions");
        }

        // Only 1 of these 2 will be used
        int contentHeight = heightSpecSize;
        int contentWidth = widthSpecSize;
        
        for (View handle : mHandles)
        {
        	measureChild(handle, widthMeasureSpec, heightMeasureSpec);
        	contentHeight -= handle.getMeasuredHeight();
        	contentWidth -= handle.getMeasuredWidth();
        }
        
        for (View content : mContents)
        {
	        if (mVertical) {
	        	content.measure(MeasureSpec.makeMeasureSpec(widthSpecSize, MeasureSpec.EXACTLY),
	                    MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY));
	        } else {
	        	content.measure(MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY),
	                    MeasureSpec.makeMeasureSpec(heightSpecSize, MeasureSpec.EXACTLY));
	        }
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final long drawingTime = getDrawingTime();
             

        if (mTracking > -1 || mAnimating > -1) {
        	Bitmap cache;
        	int contentIdx;        	
        	int idx = mAnimating > -1 ? mAnimating : mTracking;
        	
        	// Upper bitmap, if multiple are being drawn
        	// May have a 2nd bitmap to display
            if (mExpanded > -1 && idx > 0) {
            	contentIdx = Math.min(idx - 1, mExpanded);
            	cache = mContents.get(contentIdx).getDrawingCache();
                if (cache != null) {
                    canvas.drawBitmap(cache, 0, getEffectiveBottom(mHandles.get(contentIdx)), null);               
                } 
                else {
                    canvas.save(); 

                	int offset = minHandleTops.get(contentIdx);
                	canvas.translate(0, getEffectiveTop(mHandles.get(contentIdx)) - offset);                	     	

                    drawChild(canvas, mContents.get(contentIdx), drawingTime);
                    canvas.restore();
                }            	
            }
        	
        	// Lower bitmap
        	contentIdx = Math.max(idx, mExpanded);
            cache = mContents.get(contentIdx).getDrawingCache();
            if (cache != null) {
                canvas.drawBitmap(cache, 0, getEffectiveBottom(mHandles.get(idx)), null);               
            } 
            else {
            	// TODO: what does this do? it seems to be hit a lot...what is this case?
            	// Seem to be seeing the effects of this more on the downward motion
                canvas.save(); 

            	int offset = minHandleTops.get(contentIdx);
            	canvas.translate(0, getEffectiveTop(mHandles.get(contentIdx)) - offset);                	     	

                drawChild(canvas, mContents.get(contentIdx), drawingTime);
                canvas.restore();
            }                        
        } else if (mExpanded > -1) {
            drawChild(canvas, mContents.get(mExpanded), drawingTime);
        }
        
        for (View handle : mHandles) {
        	drawChild(canvas, handle, drawingTime);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mTracking > -1) {
            return;
        }
        
        // TODO: how do we handle children of varying widths and heights? Force uniformity with first?
        // For now, we will call layout for each content based on the values of its associated handle.
        final int width = r - l;
        final int height = b - t;

        int childLeft = width;
        int childTop = height;
        
        for (int i = mHandles.size() - 1; i >= 0; i--)
    	{
	        final View handle = mHandles.get(i);
	
	        int handleWidth = handle.getMeasuredWidth();
	        int handleHeight = handle.getMeasuredHeight();	        
	
	        final View content = mContents.get(i);
	        
	        // TODO: what if we're animating?  any change?	        
	        
	        if (mVertical) {
	        	// We center handle, but not content?
	        	childLeft = (width - handleWidth) / 2; 
		        if (mExpanded == i) {
		        	// Two ways to do this - we could either move up the size of the displayed content, or 
		        	// set explicitly relative to t based on the number of non-expanded handles above us
		        	//  - we're going to do the latter
		        	childTop = 0;
		        	for (int j = i; j >= 0; j--)
		        	{
		        		childTop += mHandles.get(j).getMeasuredHeight();
		        	}
		        }
		        
		        // childTop should currently point to the bottom of the current handle, adjust appropriately 
		        childTop -= handleHeight;
		        content.layout(0, childTop + handleHeight, content.getMeasuredWidth(),
		        		childTop + handleHeight + content.getMeasuredHeight());
	        }
	        else {	// horizontal	        	
	        	childTop = (height - handleHeight) / 2;
	        	childLeft = 0;
	        	if (mExpanded == i) {
	        		for (int j = i; j >= 0; j--)
		        	{
		        		childLeft += mHandles.get(j).getMeasuredWidth();
		        	}
	        	}
	        	content.layout(childLeft + handleWidth, 0, childLeft + handleWidth + content.getMeasuredWidth(),
	        			content.getMeasuredHeight()); 
	        }	        		        
	        
	        handle.layout(childLeft, childTop, childLeft + handleWidth, childTop + handleHeight);
    	}
        
        // Re-calc max offsets        
    	int maxTop = getEffectiveHeight(this);
    	for (int i = mHandles.size() - 1; i >= 0; i--) {
    		final View handle = mHandles.get(i);
    		maxTop -= getEffectiveHeight(handle);
	        maxHandleTops.set(i, maxTop);	
    	}
        
        int minTop = 0;
        for (int i = 0; i < mHandles.size(); i++) {
    		final View handle = mHandles.get(i);
    		minHandleTops.set(i, minTop);
    		minTop += getEffectiveHeight(handle);	        	
    	}
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mLocked) {
            return false;
        }

        final int action = event.getAction();

        float x = event.getX();
        float y = event.getY();
        
        for (int i = 0; i < mHandles.size(); i++)
        {
	        final View handle = mHandles.get(i);
	
	        handle.getHitRect(frame);
	        if (mTracking == -1 && !frame.contains((int) x, (int) y)) {
	            continue;
	        }
	        
	        if (i == 0 && mLockOpen) {
	        	return false;
	        }
	        
	        if (i == mHandles.size() - 1 && mLockClosed) {
	        	return false;
	        }
	
	        Log.v("MSD", "hit found for index " + i);
	        if (action == MotionEvent.ACTION_DOWN) {
	            mTracking = i;
	
	            handle.setPressed(true);
	            // Must be called before prepareTracking()
	            prepareContent(i);
	
	            // Must be called after prepareContent()
	            if (mOnDrawerScrollListener != null) {
	                mOnDrawerScrollListener.onScrollStarted();
	            }
	

                final int top = getEffectiveTop(handle);
                mTouchDelta = (int) y - top;
                prepareTracking(top, i);

	            mVelocityTracker.addMovement(event);
	        }
	
	        return true;
        }
        
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mLocked) {
            return true;
        }

        if (mTracking > -1) {
            mVelocityTracker.addMovement(event);
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    moveHandle((int) (mVertical ? event.getY() : event.getX()) - mTouchDelta, mTracking);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(mVelocityUnits);

                    float yVelocity = velocityTracker.getYVelocity();
                    float xVelocity = velocityTracker.getXVelocity();
                    boolean negative;

                    final boolean vertical = mVertical;
                    if (vertical) {
                        negative = yVelocity < 0;
                        if (xVelocity < 0) {
                            xVelocity = -xVelocity;
                        }
                        if (xVelocity > mMaximumMinorVelocity) {
                            xVelocity = mMaximumMinorVelocity;
                        }
                    } else {
                        negative = xVelocity < 0;
                        if (yVelocity < 0) {
                            yVelocity = -yVelocity;
                        }
                        if (yVelocity > mMaximumMinorVelocity) {
                            yVelocity = mMaximumMinorVelocity;
                        }
                    }

                    // TODO: rework this method to use the 'getEffective****' pattern
                    float velocity = (float) Math.hypot(xVelocity, yVelocity);
                    if (negative) {
                        velocity = -velocity;
                    }

                    final int top = getEffectiveTop(mHandles.get(mTracking));
                    final int bottom = getEffectiveBottom(mHandles.get(mTracking));
                    
                    int precedingHandleHeight = 0;
                    int succeedingHandleHeight = 0;
                    
                    for (int i = 0; i < mHandles.size(); i++)
                    {
                    	if (i < mTracking)
                    	{
                    		precedingHandleHeight += vertical ? mHandles.get(i).getMeasuredHeight() : mHandles.get(i).getMeasuredWidth();
                    	}
                    	else if(i > mTracking)
                    	{
                    		succeedingHandleHeight += vertical ? mHandles.get(i).getMeasuredHeight() : mHandles.get(i).getMeasuredWidth();
                    	}	                    
                    }
                    
                    boolean bTap;
                    if (mTracking <= mExpanded) {
                    	// count from the top
                    	bTap = Math.abs(precedingHandleHeight - top) < mTapThreshold;                 			
                    }
                    else {
                    	// count from the bottom
                    	bTap = Math.abs((getEffectiveHeight(this) - succeedingHandleHeight) - bottom) < mTapThreshold;
                    }

                    if (Math.abs(velocity) < mMaximumTapVelocity && bTap && mAllowSingleTap) {
                        playSoundEffect(SoundEffectConstants.CLICK);

                        if (mTracking <= mExpanded) {
                        	Log.v("MSD", "Closing index " + mTracking);
                            animateClose(top, mTracking);
                        } else {
                        	Log.v("MSD", "Opening index " + mTracking);
                            animateOpen(top, mTracking);
                        }
                    }
                    else {
                        performFling(top, velocity, false, mTracking);
                    }
                }
                break;
            }
        }

        return mTracking > -1 || mAnimating > -1 || super.onTouchEvent(event);
    }

    private void animateClose(int position, int idx) {
        prepareTracking(position, idx);
        performFling(position, mMaximumAcceleration, true, idx);
    }

    private void animateOpen(int position, int idx) {
        prepareTracking(position, idx);
        performFling(position, -mMaximumAcceleration, true, idx);
    }

    private void performFling(int position, float velocity, boolean always, int idx) {
        mAnimationPosition = position;
        mAnimatedVelocity = velocity;
        
        final View hIdx = mHandles.get(idx);
        Log.v("MSD", "Flinging index " + idx + " to position " + position + ", 'always' = " + always);
        int midContent = (getEffectiveHeight(this) - getCombinedHandleHeight()) / 2; // this is mid if nothing is expanded
    	
        if (idx <= mExpanded) {
        	// Going down
        	if (idx > 0) {
       			midContent += getEffectiveBottom(mHandles.get(idx - 1));
        	}
        	
            if (always || (velocity > mMaximumMajorVelocity ||
                    (position > midContent && velocity > -mMaximumMajorVelocity))) {
                // We are expanded and are now going to animate away.
                mAnimatedAcceleration = mMaximumAcceleration;
                if (velocity < 0) {
                    mAnimatedVelocity = 0;
                }
            } 
            else {
            	// We are expanded, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the expanded position.
                mAnimatedAcceleration = -mMaximumAcceleration;
                if (velocity > 0) {
                    mAnimatedVelocity = 0;
                }
            }
        } 
        else {
        	// Going up (idx > mExpanded)

        	if (idx < mHandles.size() - 1) {
        		midContent = getEffectiveTop(mHandles.get(idx + 1)) - midContent;
        	}
        	else {        		
        		midContent = getEffectiveHeight(this) - midContent;
        	}
        	midContent -= getEffectiveHeight(hIdx);
        	
            if (always || velocity < -mMaximumMajorVelocity ||
                    (position < midContent && velocity < mMaximumMajorVelocity)) {
                // We are collapsed, and they moved enough to allow us to expand.
                mAnimatedAcceleration = -mMaximumAcceleration;
                if (velocity > 0) {
                    mAnimatedVelocity = 0;
                }
            } 
            else {
                // We are collapsed, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the collapsed position.
                mAnimatedAcceleration = mMaximumAcceleration;
                if (velocity < 0) {
                    mAnimatedVelocity = 0;
                }
            }
        }

        long now = SystemClock.uptimeMillis();
        mAnimationLastTime = now;
        mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
        mAnimating = idx;
        mHandler.removeMessages(MSG_ANIMATE);
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE), mCurrentAnimationTime);
        stopTracking();
    }

    private void prepareTracking(int position, int idx) {
        mTracking = idx;
        mVelocityTracker = VelocityTracker.obtain();
        if (mTracking > mExpanded) { 
        	// opening
            mAnimatedAcceleration = mMaximumAcceleration;
            mAnimatedVelocity = mMaximumMajorVelocity;
            
            int offset = 0;
            for (int i = idx; i < mHandles.size(); i++)
            {
            	offset += getEffectiveHeight(mHandles.get(i));        
            }
            mAnimationPosition = getEffectiveHeight(this) - offset;
            moveHandle((int) mAnimationPosition, idx);
            mAnimating = idx;
            mHandler.removeMessages(MSG_ANIMATE);
            long now = SystemClock.uptimeMillis();
            mAnimationLastTime = now;
            mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
            mAnimating = idx;
        } else {
        	// closing
            if (mAnimating > -1) {
                mAnimating = -1;
                mHandler.removeMessages(MSG_ANIMATE);
            }
            moveHandle(position, idx);
        }
    }

    private void moveHandle(int position, int idx) {
        if (position == EXPANDED_FULL_OPEN) {
        	for (int i = 0; i <= Math.max(idx, mExpanded); i++) {
        		final View handle = mHandles.get(i);
        		if (getEffectiveTop(handle) != minHandleTops.get(i)) {
        			handle.offsetTopAndBottom(minHandleTops.get(i) - getEffectiveTop(handle));
        		}
        	}
        	// TODO: don't invalidate all.
        	invalidate();
        } else if (position == COLLAPSED_FULL_CLOSED) {
        	for (int i = mExpanded > -1 ? Math.min(idx, mExpanded + 1) : 0; i < mHandles.size(); i++) {
        		final View handle = mHandles.get(i);
        		if (getEffectiveTop(handle) != maxHandleTops.get(i)) {
        			handle.offsetTopAndBottom(maxHandleTops.get(i) - getEffectiveTop(handle));
        		}            			           			
        	}
        	// TODO: don't invalidate all.
        	invalidate();
        } 
        else {
        	final View hIdx = mHandles.get(idx);
        	
        	int deltaY = position - getEffectiveTop(mHandles.get(idx));
        	if (getEffectiveTop(hIdx) + deltaY < minHandleTops.get(idx)) {
    			deltaY = minHandleTops.get(idx) - getEffectiveTop(hIdx);
    		}
    		else if (getEffectiveTop(hIdx) + deltaY > maxHandleTops.get(idx)) {
    			deltaY = maxHandleTops.get(idx) - getEffectiveTop(hIdx);
    		}
        		
        	hIdx.getHitRect(mInvalidate);
        	
        	if (deltaY > 0) {
        		mInvalidate.bottom += deltaY;
        	}
        	else {
        		mInvalidate.top += deltaY;
        	}
        	
        	mInvalidate.bottom = getEffectiveHeight(this);
        	for (int i = 0; i < mHandles.size(); i++) {
        		if (i < idx && i > mExpanded) {
        			mInvalidate.top -= getEffectiveHeight(mHandles.get(i));
        		}        		
        	}
        	
        	if (idx > mExpanded) {
        		if (idx + 1 < mHandles.size())
        			mInvalidate.bottom = getEffectiveTop(mHandles.get(idx + 1));

        		for (int i = mExpanded + 1; i <= idx; i++) {
        			final View handle = mHandles.get(i);
	                handle.offsetTopAndBottom(deltaY);
            	}
        	}
        	else {
        		// idx <= mExpanded
        		if (mExpanded + 1 < mHandles.size())
        			mInvalidate.bottom = getEffectiveTop(mHandles.get(mExpanded + 1));
        		for (int i = idx; i <= mExpanded; i++) {
        			final View handle = mHandles.get(i);
        			handle.offsetTopAndBottom(deltaY);        			
        		}
        	}

            invalidate(mInvalidate);          	
        }
    }

    private void prepareContent(int idx) {
        if (mAnimating > -1) {
            return;
        }

        // There are up to 2 contents we care about: the one that is currently open, and the one being revealed,
        // which is either the one being tracked or the one above (before) the one being tracked, depending on whether
        // we are opening or closing.
        
        // Something changed in the content, we need to honor the layout request
        // before creating the cached bitmap

    	View content;
    	View handle;
    	if (idx < mExpanded && idx > 0) {
    		content = mContents.get(idx - 1);
    		handle = mHandles.get(idx - 1);
    	}
    	else {        		
    		content = mContents.get(idx);
    		handle = mHandles.get(idx);
    	}
    	
		if (content.isLayoutRequested() && idx > -1) {
    		int height = getEffectiveHeight(this) - getEffectiveHeight(handle) - mTopOffset;
    		content.measure(MeasureSpec.makeMeasureSpec(getEffectiveWidth(this), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    		content.layout(0, mTopOffset + getEffectiveHeight(handle), content.getMeasuredWidth(),
                    mTopOffset + getEffectiveHeight(handle) + content.getMeasuredHeight());
		}
		
        // Try only once... we should really loop but it's not a big deal
        // if the draw was canceled, it will only be temporary anyway
        content.getViewTreeObserver().dispatchOnPreDraw();
        // if (!content.isHardwareAccelerated()) // not avaialble in 2.3 
        content.buildDrawingCache();

        content.setVisibility(View.GONE);

		
		if (mExpanded > -1 && mExpanded != idx) {
			content = mContents.get(mExpanded);
			handle = mHandles.get(mExpanded);
			if (content.isLayoutRequested()) {
        		int height = getEffectiveHeight(this) - getEffectiveHeight(handle) - mTopOffset;
        		content.measure(MeasureSpec.makeMeasureSpec(getEffectiveWidth(this), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        		content.layout(0, mTopOffset + getEffectiveHeight(handle), content.getMeasuredWidth(),
                        mTopOffset + getEffectiveHeight(handle) + content.getMeasuredHeight());
    		}
			
	        // Try only once... we should really loop but it's not a big deal
	        // if the draw was canceled, it will only be temporary anyway
	        content.getViewTreeObserver().dispatchOnPreDraw();
	        // if (!content.isHardwareAccelerated()) // not available in 2.3 
	        content.buildDrawingCache();

	        content.setVisibility(View.GONE);
		}      
    }

    private void stopTracking() {
        mHandles.get(mTracking).setPressed(false);
        mTracking = -1;

        if (mOnDrawerScrollListener != null) {
            mOnDrawerScrollListener.onScrollEnded();
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void doAnimation() {
    	final int idx = mAnimating;
        if (mAnimating > -1) {
            incrementAnimation();            

            if (mAnimationPosition >= maxHandleTops.get(mAnimating) - 1) {            	
                mAnimating = -1;
                closeDrawer(idx);
            } else if (mAnimationPosition < minHandleTops.get(mAnimating)) {
                mAnimating = -1;
                openDrawer(idx);
            } else {
                moveHandle((int) mAnimationPosition, idx);
                mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE),
                        mCurrentAnimationTime);
            }
        }
    }

    private void incrementAnimation() {
        long now = SystemClock.uptimeMillis();
        float t = (now - mAnimationLastTime) / 1000.0f;                   // ms -> s
        final float position = mAnimationPosition;
        final float v = mAnimatedVelocity;                                // px/s
        final float a = mAnimatedAcceleration;                            // px/s/s
        mAnimationPosition = position + (v * t) + (0.5f * a * t * t);     // px
        mAnimatedVelocity = v + (a * t);                                  // px/s
        mAnimationLastTime = now;                                         // ms
    }

    /**
     * Toggles the drawer open and close. Takes effect immediately.
     *
     * @see #open()
     * @see #close()
     * @see #animateClose()
     * @see #animateOpen()
     * @see #animateToggle()
     */
    public void toggle(int idx) {
        if (mExpanded == -1) {
            openDrawer(idx);
        } else {
            closeDrawer(idx);
        }
        invalidate();
        requestLayout();
    }

    /**
     * Toggles the drawer open and close with an animation.
     *
     * @see #open()
     * @see #close()
     * @see #animateClose()
     * @see #animateOpen()
     * @see #toggle()
     */
    public void animateToggle(int idx) {
        if (mExpanded == -1) {
            animateOpen(idx);
        } else {
            animateClose(idx);
        }
    }

    /**
     * Opens the drawer immediately.
     *
     * @see #toggle()
     * @see #close()
     * @see #animateOpen()
     */
    public void open(int idx) {
        openDrawer(idx);
        invalidate();
        requestLayout();

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    /**
     * Closes the drawer immediately.
     *
     * @see #toggle()
     * @see #open()
     * @see #animateClose()
     */
    public void close(int idx) {
        closeDrawer(idx);
        invalidate();
        requestLayout();
    }

    /**
     * Closes the drawer with an animation.
     *
     * @see #close()
     * @see #open()
     * @see #animateOpen()
     * @see #animateToggle()
     * @see #toggle()
     */
    public void animateClose(int idx) {
        prepareContent(idx);
        final OnDrawerScrollListener scrollListener = mOnDrawerScrollListener;
        if (scrollListener != null) {
            scrollListener.onScrollStarted();
        }
        animateClose(getEffectiveTop(mHandles.get(idx)), idx);

        if (scrollListener != null) {
            scrollListener.onScrollEnded();
        }
    }

    /**
     * Opens the drawer with an animation.
     *
     * @see #close()
     * @see #open()
     * @see #animateClose()
     * @see #animateToggle()
     * @see #toggle()
     */
    public void animateOpen(int idx) {
        prepareContent(idx);
        final OnDrawerScrollListener scrollListener = mOnDrawerScrollListener;
        if (scrollListener != null) {
            scrollListener.onScrollStarted();
        }
        animateOpen(getEffectiveTop(mHandles.get(idx)), idx);

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        if (scrollListener != null) {
            scrollListener.onScrollEnded();
        }
    }

    private void closeDrawer(int idx) {
        moveHandle(COLLAPSED_FULL_CLOSED, idx);	// This will move handles below idx as well

        if (mExpanded == -1) {
            return;
        }
        
        if (idx <= mExpanded) {
        	mContents.get(mExpanded).setVisibility(View.GONE);
        	mContents.get(mExpanded).destroyDrawingCache();
        	
        	if (idx != 0) { // we are exposing a new content with this drawer close
        		mContents.get(idx - 1).setVisibility(View.VISIBLE);
        		mContents.get(idx - 1).destroyDrawingCache();        		
        	}

        	mExpanded = idx - 1;       		
        }
        else {
        	mContents.get(mExpanded).setVisibility(View.VISIBLE);
        	mContents.get(mExpanded).destroyDrawingCache();
        	mContents.get(idx).setVisibility(View.GONE);
        	mContents.get(idx).destroyDrawingCache();
        }
        
        if (mOnDrawerCloseListener != null) {
            mOnDrawerCloseListener.onDrawerClosed(idx);
        }
    }

    private void openDrawer(int idx) {
    	moveHandle(EXPANDED_FULL_OPEN, idx);
    	
    	if (idx > mExpanded && mExpanded > -1) {
    		mContents.get(mExpanded).setVisibility(View.GONE);
        	mContents.get(mExpanded).destroyDrawingCache();
    	}
    	else if (idx < mExpanded && idx > 0) {
    		mContents.get(idx - 1).setVisibility(View.GONE);
        	mContents.get(idx - 1).destroyDrawingCache();
    	}
        
    	mExpanded = Math.max(mExpanded, idx);
    	
    	mContents.get(mExpanded).setVisibility(View.VISIBLE);
        mContents.get(mExpanded).destroyDrawingCache();        

        if (mOnDrawerOpenListener != null) {
            mOnDrawerOpenListener.onDrawerOpened(idx);
        }
    }

    /**
     * Sets the listener that receives a notification when the drawer becomes open.
     *
     * @param onDrawerOpenListener The listener to be notified when the drawer is opened.
     */
    public void setOnDrawerOpenListener(OnDrawerOpenListener onDrawerOpenListener) {
        mOnDrawerOpenListener = onDrawerOpenListener;
    }

    /**
     * Sets the listener that receives a notification when the drawer becomes close.
     *
     * @param onDrawerCloseListener The listener to be notified when the drawer is closed.
     */
    public void setOnDrawerCloseListener(OnDrawerCloseListener onDrawerCloseListener) {
        mOnDrawerCloseListener = onDrawerCloseListener;
    }

    /**
     * Sets the listener that receives a notification when the drawer starts or ends
     * a scroll. A fling is considered as a scroll. A fling will also trigger a
     * drawer opened or drawer closed event.
     *
     * @param onDrawerScrollListener The listener to be notified when scrolling
     *        starts or stops.
     */
    public void setOnDrawerScrollListener(OnDrawerScrollListener onDrawerScrollListener) {
        mOnDrawerScrollListener = onDrawerScrollListener;
    }

    /**
     * Returns the handle of the drawer.
     *
     * @return The View reprenseting the handle of the drawer, identified by
     *         the "handle" id in XML.
     */
    public View getHandle(int idx) {
        return mHandles.get(idx);
    }

    /**
     * Returns the content of the drawer.
     *
     * @return The View reprenseting the content of the drawer, identified by
     *         the "content" id in XML.
     */
    public View getContent(int idx) {
        return mContents.get(idx);
    }

    /**
     * Unlocks the MultiSlidingDrawer so that touch events are processed.
     *
     * @see #lock() 
     */
    public void unlock() {
        mLocked = false;
    }

    /**
     * Locks the MultiSlidingDrawer so that touch events are ignores.
     *
     * @see #unlock()
     */
    public void lock() {
        mLocked = true;
    }

    /**
     * Indicates whether the drawer is currently fully opened.
     *
     * @return True if the drawer is opened, false otherwise.
     */
    public boolean isOpened() {
        return mExpanded > -1;
    }

    /**
     * Indicates whether the drawer is scrolling or flinging.
     *
     * @return True if the drawer is scroller or flinging, false otherwise.
     */
    public boolean isMoving() {
        return mTracking > -1 || mAnimating > -1;
    }

    // TODO: these should use getWidth/getHeight, and not getMeasured, right?
    // Also, these can likely be optimized, since handle width/height is unlikely to change much
    private int getCombinedHandleHeight() {
    	int height = 0;
    	for (View handle : mHandles) {
    		height += getEffectiveHeight(handle);
    	}
    	return height;
    }
    
    private int getCombinedHandleWidth() {
    	int width = 0;
    	for (View handle : mHandles) {
    		width += getEffectiveWidth(handle);
    	}
    	return width;    
    }
    
    private class DrawerToggler implements OnClickListener {
        public void onClick(View v) {
        	int idx = mHandles.indexOf(v);
            if (mLocked || idx < 0 || idx == 0 && mLockOpen) {
                return;
            }
            // mAllowSingleTap isn't relevant here; you're *always*
            // allowed to open/close the drawer by clicking with the
            // trackball.            

            if (mAnimateOnClick) {
                animateToggle(idx);
            } else {
                toggle(idx);
            }
        }
    }

    private class SlidingHandler extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_ANIMATE:
                    doAnimation();
                    break;
            }
        }
    }
}
