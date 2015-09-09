package me.isvic.titans.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.Scroller;
import android.widget.TextView;

import me.isvic.titans.R;
import me.isvic.titans.utils.SchedulingUtils;
import me.isvic.titans.utils.TitansLog;

/**
 * Created by Vic on 15/9/5.
 */
public class MultiShrinkScroller extends FrameLayout {

    private static final String TAG = MultiShrinkScroller.class.getSimpleName();

    /**
     * 1000 pixels per millisecond. Ie, 1 pixel per second.
     */
    private static final int PIXELS_PER_SECOND = 1000;

    /**
     * Length of the acceleration animations. This value was taken from ValueAnimator.java.
     */
    private static final int EXIT_FLING_ANIMATION_DURATION_MS = 250;

    /**
     * In portrait mode, the height:width ratio of the photo's starting height.
     */
    private static final float INTERMEDIATE_HEADER_HEIGHT_RATIO = 0.6f;

    private static final float SPRING_DAMPENING_FACTOR = 0.01f;

    private float[] mLastEventPosition = { 0, 0 };
    private VelocityTracker mVelocityTracker;
    private boolean mIsBeingDragged = false;
    private boolean mReceivedDown = false;
    private boolean mIsFullscreenDownwardsFling = false;

    private boolean mHasEverTouchedTheTop;
    private boolean mIsTouchDisabledForDismissAnimation;

    private final Context mContext;

    private View mViewContent;
    private View mToolbarParent;
    private View mTransparentView;
    private View mTitleGradientView;
    private View mActionBarGradientView;
    private TextView mLargeTextView;

    private final Scroller mScroller;
    private final EdgeEffect mEdgeGlowBottom;
    private final EdgeEffect mEdgeGlowTop;
    private final int mTouchSlop;
    private final int mMaximumVelocity;
    private final int mMinimumVelocity;
    private final int mTransparentStartHeight;
    private final float mToolbarElevation;
    private final int mMaximumTitleMargin;
    private final int mDismissDistanceOnScroll;
    private final int mDismissDistanceOnRelease;
    private final int mSnapToTopSlopHeight;
    private final int mActionBarSize;

    private int mMinimumPortraitHeaderHeight;
    private int mMaximumPortraitHeaderHeight;

    private int mMaximumHeaderHeight;
    private int mMinimumHeaderHeight;
    private int mIntermediateHeaderHeight;
    private boolean mIsOpenContactSquare;

    private SlideUpScrollerListener mListener;


    private final int[] mGradientColors = new int[] {0,0x88000000};
    private GradientDrawable mTitleGradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, mGradientColors);
    private GradientDrawable mActionBarGradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP, mGradientColors);

    public interface SlideUpScrollerListener {
        void onScrolledOffBottom();

        void onStartScrollOffBottom();

        void onTransparentViewHeightChange(float ratio);

        void onEntranceAnimationDone();

        void onEnterFullscreen();

        void onExitFullscreen();
    }

    private final Animator.AnimatorListener mSnapToBottomListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mListener != null) {
                // Due to a rounding error, after the animation finished we haven't fully scrolled
                // off the screen. Lie to the listener: tell it that we did scroll off the screen.
                mListener.onScrolledOffBottom();
                // No other messages need to be sent to the listener.
                mListener = null;
            }
        }
    };

    private static final Interpolator sInterpolator = new Interpolator() {

        /**
         * {@inheritDoc}
         */
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public MultiShrinkScroller(Context context) {
        this(context, null);
    }

    public MultiShrinkScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiShrinkScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mEdgeGlowBottom = new EdgeEffect(context);
        mEdgeGlowTop = new EdgeEffect(context);
        mContext = context;

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        setFocusable(false);
        // Drawing must be enabled in order to support EdgeEffect
        setWillNotDraw(/* willNotDraw = */ false);

        mScroller = new Scroller(context, sInterpolator);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        mTransparentStartHeight = (int) getResources().getDimension(
                R.dimen.quickcontact_starting_empty_height);
        mToolbarElevation = getResources().getDimension(
                R.dimen.quick_contact_toolbar_elevation);
        mMaximumTitleMargin = (int) getResources().getDimension(
                R.dimen.quickcontact_title_initial_margin);

        mDismissDistanceOnScroll = (int) getResources().getDimension(
                R.dimen.quickcontact_dismiss_distance_on_scroll);
        mDismissDistanceOnRelease = (int) getResources().getDimension(
                R.dimen.quickcontact_dismiss_distance_on_release);
        mSnapToTopSlopHeight = (int) getResources().getDimension(
                R.dimen.quickcontact_snap_to_top_slop_height);

        final TypedArray attributeArray = context.obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        mActionBarSize = attributeArray.getDimensionPixelSize(0, 0);
        mMinimumHeaderHeight = mActionBarSize;
        // This value is approximately equal to the portrait ActionBar size. It isn't exactly the
        // same, since the landscape and portrait ActionBar sizes can be different.
        mMinimumPortraitHeaderHeight = mMinimumHeaderHeight;
        attributeArray.recycle();
    }

    public void initialize(SlideUpScrollerListener listener, boolean isOpenContactSquare) {
        mListener = listener;
        mIsOpenContactSquare = isOpenContactSquare;

        mViewContent = findViewById(R.id.scroll_content);
        mToolbarParent = findViewById(R.id.toolbar_parent);
        mTransparentView = findViewById(R.id.transparent_view);
        mLargeTextView = (TextView) findViewById(R.id.large_title);

        mTitleGradientView = findViewById(R.id.title_gradient);
        mActionBarGradientView = findViewById(R.id.action_bar_gradient);

        SchedulingUtils.doOnPreDraw(this, false, new Runnable() {
            @Override
            public void run() {
                mMaximumHeaderHeight = mToolbarParent.getWidth();
                mIntermediateHeaderHeight = (int) (mMaximumHeaderHeight
                        * INTERMEDIATE_HEADER_HEIGHT_RATIO);
                setHeaderHeight(getMaximumScrollableHeaderHeight());
                mViewContent.getLayoutParams().height = getHeight();
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        // The only time we want to intercept touch events is when we are being dragged.
        return shouldStartDrag(event);
    }



    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            // Examine the fling results in order to activate EdgeEffect and halt flings.
            final int oldScroll = getScroll();
            scrollTo(0, mScroller.getCurrY());
            final int delta = mScroller.getCurrY() - oldScroll;
            final int distanceFromMaxScrolling = getMaximumScrollUpwards() - getScroll();
            if (delta > distanceFromMaxScrolling && distanceFromMaxScrolling > 0) {
                mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
            }
            if (mIsFullscreenDownwardsFling && getTransparentViewHeight() > 0) {
                // Halt the fling once QuickContact's top is on screen.
                scrollTo(0, getScroll() + getTransparentViewHeight());
                mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                mScroller.abortAnimation();
                mIsFullscreenDownwardsFling = false;
            }
            if (!awakenScrollBars()) {
                // Keep on drawing until the animation has finished.
                postInvalidateOnAnimation();
            }
            if (mScroller.getCurrY() >= getMaximumScrollUpwards()) {
                // Halt the fling once QuickContact's bottom is on screen.
                mScroller.abortAnimation();
                mIsFullscreenDownwardsFling = false;
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight();

        if (!mEdgeGlowBottom.isFinished()) {
            final int restoreCount = canvas.save();

            // Draw the EdgeEffect on the bottom of the Window (Or a little bit below the bottom
            // of the Window if we start to scroll upwards while EdgeEffect is visible). This
            // does not need to consider the case where this MultiShrinkScroller doesn't fill
            // the Window, since the nested ScrollView should be set to fillViewport.
            canvas.translate(-width + getPaddingLeft(),
                    height + getMaximumScrollUpwards() - getScroll());

            canvas.rotate(180, width, 0);
            mEdgeGlowBottom.setSize(width, height);
            if (mEdgeGlowBottom.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!mEdgeGlowTop.isFinished()) {
            final int restoreCount = canvas.save();
            mEdgeGlowTop.setSize(width, height);
            if (mEdgeGlowTop.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }
    }

    private boolean shouldStartDrag(MotionEvent event) {
        if (mIsTouchDisabledForDismissAnimation)
            return false;

        if (mIsBeingDragged) {
            mIsBeingDragged = false;
            return false;
        }

        switch (event.getAction()) {
            // If we are in the middle of a fling and there is a down event, we'll steal it and
            // start a drag.
            case MotionEvent.ACTION_DOWN:
                updateLastEventPosition(event);
                if (!mScroller.isFinished()) {
                    startDrag();
                    return true;
                } else {
                    mReceivedDown = true;
                }
                break;

            // Otherwise, we will start a drag if there is enough motion in the direction we are
            // capable of scrolling.
            case MotionEvent.ACTION_MOVE:
                if (motionShouldStartDrag(event)) {
                    updateLastEventPosition(event);
                    startDrag();
                    return true;
                }
                break;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsTouchDisabledForDismissAnimation) return true;

        final int action = event.getAction();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        if (!mIsBeingDragged) {
            if (shouldStartDrag(event)) {
                return true;
            }

            if (action == MotionEvent.ACTION_UP && mReceivedDown) {
                mReceivedDown = false;
                return performClick();
            }
            return true;
        }
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                final float delta = updatePositionAndComputeDelta(event);
                scrollTo(0, getScroll() + (int) delta);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopDrag(action == MotionEvent.ACTION_CANCEL);
                mReceivedDown = false;
        }
        return true;
    }

    @Override
    public void scrollTo(int x, int y) {
        final int delta = y - getScroll();
        boolean wasFullscreen = getScrollNeededToBeFullScreen() <= 0;
        if (delta > 0) {
            scrollUp(delta);
        } else {
            scrollDown(delta);
        }
//        updatePhotoTintAndDropShadow();
//        updateHeaderTextSizeAndMargin();
        final boolean isFullscreen = getScrollNeededToBeFullScreen() <= 0;
        mHasEverTouchedTheTop |= isFullscreen;
        if (mListener != null) {
            if (wasFullscreen && !isFullscreen) {
                mListener.onExitFullscreen();
            } else if (!wasFullscreen && isFullscreen) {
                mListener.onEnterFullscreen();
            }
            if (!isFullscreen || !wasFullscreen) {
                mListener.onTransparentViewHeightChange(
                        getTransparentHeightRatio(getTransparentViewHeight()));
            }
        }
    }

    private void scrollUp(int delta) {
        if (getTransparentViewHeight() != 0) {
            final int originalValue = getTransparentViewHeight();
            setTransparentViewHeight(getTransparentViewHeight() - delta);
            setTransparentViewHeight(Math.max(0, getTransparentViewHeight()));
            delta -= originalValue - getTransparentViewHeight();
        }
        final ViewGroup.LayoutParams toolbarLayoutParams
                = mToolbarParent.getLayoutParams();
        if (toolbarLayoutParams.height > getFullyCompressedHeaderHeight()) {
            final int originalValue = toolbarLayoutParams.height;
            toolbarLayoutParams.height -= delta;
            toolbarLayoutParams.height = Math.max(toolbarLayoutParams.height,
                    getFullyCompressedHeaderHeight());
            mToolbarParent.setLayoutParams(toolbarLayoutParams);
            delta -= originalValue - toolbarLayoutParams.height;
        }
//        mScrollView.scrollBy(0, delta);
    }

    private void scrollDown(int delta) {
//        if (mScrollView.getScrollY() > 0) {
//            final int originalValue = mScrollView.getScrollY();
//            mScrollView.scrollBy(0, delta);
//            delta -= mScrollView.getScrollY() - originalValue;
//        }
        final ViewGroup.LayoutParams toolbarLayoutParams = mToolbarParent.getLayoutParams();
        if (toolbarLayoutParams.height < getMaximumScrollableHeaderHeight()) {
            final int originalValue = toolbarLayoutParams.height;
            toolbarLayoutParams.height -= delta;
            toolbarLayoutParams.height = Math.min(toolbarLayoutParams.height,
                    getMaximumScrollableHeaderHeight());
            mToolbarParent.setLayoutParams(toolbarLayoutParams);
            delta -= originalValue - toolbarLayoutParams.height;
        }
        setTransparentViewHeight(getTransparentViewHeight() - delta);
    }

    private int getFullyCompressedHeaderHeight() {
        return Math.min(Math.max(mToolbarParent.getLayoutParams().height - getOverflowingChildViewSize(),
                mMinimumHeaderHeight), getMaximumScrollableHeaderHeight());
    }

    private int getOverflowingChildViewSize() {
        final int usedScrollViewSpace = mViewContent.getHeight();
        return -getHeight() + usedScrollViewSpace + mToolbarParent.getLayoutParams().height;
    }

    private int getMaximumScrollableHeaderHeight() {
        return mIsOpenContactSquare ? mMaximumHeaderHeight : mIntermediateHeaderHeight;
    }

    private void startDrag() {
        mIsBeingDragged = true;
        mScroller.abortAnimation();
    }

    private void stopDrag(boolean cancelled) {
        mIsBeingDragged = false;
        if (!cancelled && getChildCount() > 0) {
            final float velocity = getCurrentVelocity();
            if (velocity > mMinimumVelocity || velocity < -mMinimumVelocity) {
                fling(-velocity);
                onDragFinished(mScroller.getFinalY() - mScroller.getStartY());
            } else {
                onDragFinished(/* flingDelta = */ 0);
            }
        } else {
            onDragFinished(/* flingDelta = */ 0);
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }

        mEdgeGlowBottom.onRelease();
    }

    private void onDragFinished(int flingDelta) {
        if (getTransparentViewHeight() <= 0) {
            // Don't perform any snapping if quick contacts is full screen.
            return;
        }
        if (!snapToTopOnDragFinished(flingDelta)) {
            // The drag/fling won't result in the content at the top of the Window. Consider
            // snapping the content to the bottom of the window.
            snapToBottomOnDragFinished();
        }
    }

    private void fling(float velocity) {
        // For reasons I do not understand, scrolling is less janky when maxY=Integer.MAX_VALUE
        // then when maxY is set to an actual value.
        mScroller.fling(0, getScroll(), 0, (int) velocity, 0, 0, -Integer.MAX_VALUE,
                Integer.MAX_VALUE);
        if (velocity < 0 && mTransparentView.getHeight() <= 0) {
            mIsFullscreenDownwardsFling = true;
        }
        invalidate();
    }

    private boolean snapToTopOnDragFinished(int flingDelta) {
        if (!mHasEverTouchedTheTop) {
            // If the current fling is predicted to scroll past the top, then we don't need to snap
            // to the top. However, if the fling only flings past the top by a tiny amount,
            // it will look nicer to snap than to fling.
            final float predictedScrollPastTop = getTransparentViewHeight() - flingDelta;
            if (predictedScrollPastTop < -mSnapToTopSlopHeight) {
                return false;
            }

            if (getTransparentViewHeight() <= mTransparentStartHeight) {
                // We are above the starting scroll position so snap to the top.
                mScroller.forceFinished(true);
                smoothScrollBy(getTransparentViewHeight());
                return true;
            }
            return false;
        }
        if (getTransparentViewHeight() < mDismissDistanceOnRelease) {
            mScroller.forceFinished(true);
            smoothScrollBy(getTransparentViewHeight());
            return true;
        }
        return false;
    }

    private void smoothScrollBy(int delta) {
        if (delta == 0) {
            // Delta=0 implies the code calling smoothScrollBy is sloppy. We should avoid doing
            // this, since it prevents Views from being able to register any clicks for 250ms.
            throw new IllegalArgumentException("Smooth scrolling by delta=0 is "
                    + "pointless and harmful");
        }
        int scroll = getScroll();
        mScroller.startScroll(0, getScroll(), 0, delta);
        invalidate();
    }

    private void snapToBottomOnDragFinished() {
        if (mHasEverTouchedTheTop) {
            if (getTransparentViewHeight() > mDismissDistanceOnRelease) {
                scrollOffBottom();
            }
            return;
        }
        if (getTransparentViewHeight() > mTransparentStartHeight) {
            scrollOffBottom();
        }
    }

    public void scrollOffBottom() {
        mIsTouchDisabledForDismissAnimation = true;
        final Interpolator interpolator = new AcceleratingFlingInterpolator(mContext,
                EXIT_FLING_ANIMATION_DURATION_MS, getCurrentVelocity(),
                getScrollUntilOffBottom());
        mScroller.forceFinished(true);
        ObjectAnimator translateAnimation = ObjectAnimator.ofInt(this, "scroll",
                getScroll() - getScrollUntilOffBottom());
        translateAnimation.setRepeatCount(0);
        translateAnimation.setInterpolator(interpolator);
        translateAnimation.setDuration(EXIT_FLING_ANIMATION_DURATION_MS);
        translateAnimation.addListener(mSnapToBottomListener);
        translateAnimation.start();
        if (mListener != null) {
            mListener.onStartScrollOffBottom();
        }
    }

    /**
     * @param scrollToCurrentPosition if true, will scroll from the bottom of the screen to the
     * current position. Otherwise, will scroll from the bottom of the screen to the top of the
     * screen.
     */
    public void scrollUpForEntranceAnimation(boolean scrollToCurrentPosition) {
        final int currentPosition = getScroll();
        final int bottomScrollPosition = currentPosition
                - (getHeight() - getTransparentViewHeight()) + 1;
        final Interpolator interpolator = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.linear_out_slow_in);
        final int desiredValue = currentPosition + (scrollToCurrentPosition ? currentPosition
                : getTransparentViewHeight());
        final ObjectAnimator animator = ObjectAnimator.ofInt(this, "scroll", bottomScrollPosition,
                desiredValue);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedValue().equals(desiredValue) && mListener != null) {
                    mListener.onEntranceAnimationDone();
                }
            }
        });
        animator.start();
    }

    private boolean motionShouldStartDrag(MotionEvent event) {
        final float deltaY = event.getY() - mLastEventPosition[1];
        return deltaY > mTouchSlop || deltaY < -mTouchSlop;
    }

    private float updatePositionAndComputeDelta(MotionEvent event) {
        final int VERTICAL = 1;
        final float position = mLastEventPosition[VERTICAL];
        updateLastEventPosition(event);
        float elasticityFactor = 1;
        if (position < mLastEventPosition[VERTICAL] && mHasEverTouchedTheTop) {
            // As QuickContacts is dragged from the top of the window, its rate of movement will
            // slow down in proportion to its distance from the top. This will feel springy.
            elasticityFactor += mTransparentView.getHeight() * SPRING_DAMPENING_FACTOR;
        }
        return (position - mLastEventPosition[VERTICAL]) / elasticityFactor;
    }

    private void updateLastEventPosition(MotionEvent event) {
        mLastEventPosition[0] = event.getX();
        mLastEventPosition[1] = event.getY();
    }

    /**
     * Required for animation.
     */
    public void setScroll(int scroll) {
        scrollTo(0, scroll);
    }

    /**
     * Required for animation.
     * Returns the total amount scrolled inside the nested ScrollView + the amount of shrinking
     * performed on the ToolBar. This is the value inspected by animators.
     */
    public int getScroll() {
        return mTransparentStartHeight - getTransparentViewHeight()
                + getMaximumScrollableHeaderHeight() - getHeaderHeight();
    }

    private int getScroll_ignoreOversizedHeaderForSnapping() {
        return mTransparentStartHeight - getTransparentViewHeight()
                + Math.max(getMaximumScrollableHeaderHeight() - getHeaderHeight(), 0);
    }

    private int getScrollUntilOffBottom() {
        return getHeight() + getScroll_ignoreOversizedHeaderForSnapping()
                - mTransparentStartHeight;
    }

    public int  getScrollNeededToBeFullScreen() {
        return getTransparentViewHeight();
    }

    private int getTransparentViewHeight() {
        return mTransparentView.getLayoutParams().height;
    }

    private void setTransparentViewHeight(int height) {
        mTransparentView.getLayoutParams().height = height;
        mTransparentView.setLayoutParams(mTransparentView.getLayoutParams());
    }

    public int getHeaderHeight() {
        return mToolbarParent.getLayoutParams().height;
    }

    public void setHeaderHeight(int height) {
        final ViewGroup.LayoutParams toolbarLayoutParams = mToolbarParent.getLayoutParams();
        toolbarLayoutParams.height = height;
        mToolbarParent.setLayoutParams(toolbarLayoutParams);
//        updatePhotoTintAndDropShadow();
//        updateHeaderTextSizeAndMargin();
    }

    /**
     * Change the height of the header/toolbar. Do *not* use this outside animations. This was
     * designed for use by {@link #prepareForShrinkingScrollChild}.
     */
    public void setToolbarHeight(int delta) {
        final ViewGroup.LayoutParams toolbarLayoutParams = mToolbarParent.getLayoutParams();
        toolbarLayoutParams.height = delta;
        mToolbarParent.setLayoutParams(toolbarLayoutParams);

//        updatePhotoTintAndDropShadow();
//        updateHeaderTextSizeAndMargin();
    }

    public int getToolbarHeight() {
        return mToolbarParent.getLayoutParams().height;
    }

    private float getCurrentVelocity() {
        if (mVelocityTracker == null) {
            return 0;
        }
        mVelocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND, mMaximumVelocity);
        return mVelocityTracker.getYVelocity();
    }

    private float getTransparentHeightRatio(int transparentHeight) {
        final float heightRatio = (float) transparentHeight / getHeight();
        // Clamp between [0, 1] in case this is called before height is initialized.
        return 1.0f - Math.max(Math.min(1.0f, heightRatio), 0f);
    }

    private int getMaximumScrollUpwards() {
//        return mTransparentStartHeight
//                // How much the ScrollView can scroll. 0, if child is smaller than ScrollView.
//                + Math.max(0, mScrollViewChild.getHeight() - getHeight());
        return mTransparentStartHeight
                + Math.max(0, mViewContent.getHeight() - getHeight());
    }

    private static class AcceleratingFlingInterpolator implements Interpolator {

        private final float mStartingSpeedPixelsPerFrame;
        private final float mDurationMs;
        private final int mPixelsDelta;
        private final float mNumberFrames;
        private final Context mInterplatorContext;

        public AcceleratingFlingInterpolator(Context context, int durationMs,
                                             float startingSpeedPixelsPerSecond, int pixelsDelta) {
            mInterplatorContext = context;
            mStartingSpeedPixelsPerFrame = startingSpeedPixelsPerSecond / getRefreshRate();
            mDurationMs = durationMs;
            mPixelsDelta = pixelsDelta;
            mNumberFrames = mDurationMs / getFrameIntervalMs();
        }

        @Override
        public float getInterpolation(float input) {
            final float animationIntervalNumber = mNumberFrames * input;
            final float linearDelta = (animationIntervalNumber * mStartingSpeedPixelsPerFrame)
                    / mPixelsDelta;
            // Add the results of a linear interpolator (with the initial speed) with the
            // results of a AccelerateInterpolator.
            if (mStartingSpeedPixelsPerFrame > 0) {
                return Math.min(input * input + linearDelta, 1);
            } else {
                // Initial fling was in the wrong direction, make sure that the quadratic component
                // grows faster in order to make up for this.
                return Math.min(input * (input - linearDelta) + linearDelta, 1);
            }
        }

        private float getRefreshRate() {
            Display display = ((WindowManager) mInterplatorContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            return display.getRefreshRate();
        }

        private long getFrameIntervalMs() {
            return (long)(1000 / getRefreshRate());
        }
    }
}
