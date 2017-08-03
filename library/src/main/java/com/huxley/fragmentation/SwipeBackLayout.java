package com.huxley.fragmentation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentationHack;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.huxley.fragmentation.swipeback.core.ISwipeBackActivity;
import com.huxley.library.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class SwipeBackLayout extends FrameLayout {

    public static final int EDGE_LEFT = ViewDragHelper.EDGE_LEFT;

    public static final int EDGE_RIGHT = ViewDragHelper.EDGE_RIGHT;

    public static final int EDGE_ALL = EDGE_LEFT | EDGE_RIGHT;

    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;

    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;

    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;
    private static final float DEFAULT_PARALLAX = 0.33f;
    private static final int FULL_ALPHA = 255;
    private static final float DEFAULT_SCROLL_THRESHOLD = 0.4f;
    private static final int OVERSCROLL_DISTANCE = 10;

    private float mScrollFinishThreshold = DEFAULT_SCROLL_THRESHOLD;

    private ViewDragHelper mHelper;

    private float mScrollPercent;
    private float mScrimOpacity;

    private FragmentActivity mActivity;
    private View             mContentView;
    private ISupportFragment mFragment;
    private Fragment         mPreFragment;

    private Drawable mShadowLeft;
    private Drawable mShadowRight;
    private Rect mTmpRect = new Rect();

    private int mEdgeFlag;
    private boolean mEnable = true;
    private int mCurrentSwipeOrientation;
    private float mParallaxOffset = DEFAULT_PARALLAX;

    private boolean mCallOnDestroyView;

    private boolean mInLayout;

    private int mContentLeft;
    private int mContentTop;

    private List<OnSwipeListener> mListeners;

    public SwipeBackLayout(Context context) {
        this(context, null);
    }

    public SwipeBackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeBackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHelper = ViewDragHelper.create(this, new ViewDragCallback());
        setShadow(R.drawable.shadow_left, EDGE_LEFT);
        setEdgeOrientation(EDGE_LEFT);
    }

    public void setScrollThresHold(float threshold) {
        if (threshold >= 1.0f || threshold <= 0) {
            throw new IllegalArgumentException("Threshold value should be between 0 and 1.0");
        }
        mScrollFinishThreshold = threshold;
    }

    public void setParallaxOffset(float offset) {
        this.mParallaxOffset = offset;
    }

    public void setEdgeOrientation(@EdgeOrientation int orientation) {
        mEdgeFlag = orientation;
        mHelper.setEdgeTrackingEnabled(orientation);

        if (orientation == EDGE_RIGHT || orientation == EDGE_ALL) {
            setShadow(R.drawable.shadow_right, EDGE_RIGHT);
        }
    }

    @IntDef({EDGE_LEFT, EDGE_RIGHT, EDGE_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface EdgeOrientation {
    }

    public void setShadow(Drawable shadow, int edgeFlag) {
        if ((edgeFlag & EDGE_LEFT) != 0) {
            mShadowLeft = shadow;
        } else if ((edgeFlag & EDGE_RIGHT) != 0) {
            mShadowRight = shadow;
        }
        invalidate();
    }

    public void setShadow(int resId, int edgeFlag) {
        setShadow(getResources().getDrawable(resId), edgeFlag);
    }

    public void addSwipeListener(OnSwipeListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
    }

    public void removeSwipeListener(OnSwipeListener listener) {
        if (mListeners == null) {
            return;
        }
        mListeners.remove(listener);
    }

    public interface OnSwipeListener {

        void onDragStateChange(int state);

        void onEdgeTouch(int oritentationEdgeFlag);

        void onDragScrolled(float scrollPercent);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean isDrawView = child == mContentView;
        boolean drawChild = super.drawChild(canvas, child, drawingTime);
        if (isDrawView && mScrimOpacity > 0 && mHelper.getViewDragState() != ViewDragHelper.STATE_IDLE) {
            drawShadow(canvas, child);
            drawScrim(canvas, child);
        }
        return drawChild;
    }

    private void drawShadow(Canvas canvas, View child) {
        final Rect childRect = mTmpRect;
        child.getHitRect(childRect);

        if ((mCurrentSwipeOrientation & EDGE_LEFT) != 0) {
            mShadowLeft.setBounds(childRect.left - mShadowLeft.getIntrinsicWidth(), childRect.top, childRect.left, childRect.bottom);
            mShadowLeft.setAlpha((int) (mScrimOpacity * FULL_ALPHA));
            mShadowLeft.draw(canvas);
        } else if ((mCurrentSwipeOrientation & EDGE_RIGHT) != 0) {
            mShadowRight.setBounds(childRect.right, childRect.top, childRect.right + mShadowRight.getIntrinsicWidth(), childRect.bottom);
            mShadowRight.setAlpha((int) (mScrimOpacity * FULL_ALPHA));
            mShadowRight.draw(canvas);
        }
    }

    private void drawScrim(Canvas canvas, View child) {
        final int baseAlpha = (DEFAULT_SCRIM_COLOR & 0xff000000) >>> 24;
        final int alpha = (int) (baseAlpha * mScrimOpacity);
        final int color = alpha << 24;

        if ((mCurrentSwipeOrientation & EDGE_LEFT) != 0) {
            canvas.clipRect(0, 0, child.getLeft(), getHeight());
        } else if ((mCurrentSwipeOrientation & EDGE_RIGHT) != 0) {
            canvas.clipRect(child.getRight(), 0, getRight(), getHeight());
        }
        canvas.drawColor(color);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mInLayout = true;
        if (mContentView != null)
            mContentView.layout(mContentLeft, mContentTop,
                    mContentLeft + mContentView.getMeasuredWidth(),
                    mContentTop + mContentView.getMeasuredHeight());
        mInLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void computeScroll() {
        mScrimOpacity = 1 - mScrollPercent;
        if (mScrimOpacity >= 0) {
            if (mHelper.continueSettling(true)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
            if (mPreFragment != null && mPreFragment.getView() != null && mHelper.getCapturedView() != null) {
                if (mCallOnDestroyView) {
                    mPreFragment.getView().setLeft(0);
                    return;
                }

                int leftOffset = (int) ((mHelper.getCapturedView().getLeft() - getWidth()) * mParallaxOffset * mScrimOpacity);
                mPreFragment.getView().setLeft(leftOffset > 0 ? 0 : leftOffset);
            }
        }
    }

    public void internalCallOnDestroyView() {
        mCallOnDestroyView = true;
    }

    public void setFragment(final ISupportFragment fragment, View view) {
        this.mFragment = fragment;
        mContentView = view;
    }

    public void hiddenFragment() {
        if (mPreFragment != null && mPreFragment.getView() != null) {
            mPreFragment.getView().setVisibility(GONE);
        }
    }

    public void attachToActivity(FragmentActivity activity) {
        mActivity = activity;
        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.windowBackground
        });
        int background = a.getResourceId(0, 0);
        a.recycle();

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        ViewGroup decorChild = (ViewGroup) decor.getChildAt(0);
        decorChild.setBackgroundResource(background);
        decor.removeView(decorChild);
        addView(decorChild);
        setContentView(decorChild);
        decor.addView(this);
    }

    public void attachToFragment(ISupportFragment fragment, View view) {
        addView(view);
        setFragment(fragment, view);
    }

    private void setContentView(View view) {
        mContentView = view;
    }

    public void setEnableGesture(boolean enable) {
        mEnable = enable;
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            boolean dragEnable = mHelper.isEdgeTouched(mEdgeFlag, pointerId);
            if (dragEnable) {
                if (mHelper.isEdgeTouched(EDGE_LEFT, pointerId)) {
                    mCurrentSwipeOrientation = EDGE_LEFT;
                } else if (mHelper.isEdgeTouched(EDGE_RIGHT, pointerId)) {
                    mCurrentSwipeOrientation = EDGE_RIGHT;
                }

                if (mListeners != null && !mListeners.isEmpty()) {
                    for (OnSwipeListener listener : mListeners) {
                        listener.onEdgeTouch(mCurrentSwipeOrientation);
                    }
                }

                if (mPreFragment == null) {
                    if (mFragment != null) {
                        List<Fragment> fragmentList = FragmentationHack.getActiveFragments(((Fragment) mFragment).getFragmentManager());
                        if (fragmentList != null && fragmentList.size() > 1) {
                            int index = fragmentList.indexOf(mFragment);
                            for (int i = index - 1; i >= 0; i--) {
                                Fragment fragment = fragmentList.get(i);
                                if (fragment != null && fragment.getView() != null) {
                                    fragment.getView().setVisibility(VISIBLE);
                                    mPreFragment = fragment;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    View preView = mPreFragment.getView();
                    if (preView != null && preView.getVisibility() != VISIBLE) {
                        preView.setVisibility(VISIBLE);
                    }
                }
            }
            return dragEnable;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int ret = 0;
            if ((mCurrentSwipeOrientation & EDGE_LEFT) != 0) {
                ret = Math.min(child.getWidth(), Math.max(left, 0));
            } else if ((mCurrentSwipeOrientation & EDGE_RIGHT) != 0) {
                ret = Math.min(0, Math.max(left, -child.getWidth()));
            }
            return ret;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);

            if ((mCurrentSwipeOrientation & EDGE_LEFT) != 0) {
                mScrollPercent = Math.abs((float) left / (mContentView.getWidth() + mShadowLeft.getIntrinsicWidth()));
            } else if ((mCurrentSwipeOrientation & EDGE_RIGHT) != 0) {
                mScrollPercent = Math.abs((float) left / (mContentView.getWidth() + mShadowRight.getIntrinsicWidth()));
            }
            mContentLeft = left;
            mContentTop = top;
            invalidate();

            if (mListeners != null && !mListeners.isEmpty()
                    && mHelper.getViewDragState() == STATE_DRAGGING && mScrollPercent <= 1 && mScrollPercent > 0) {
                for (OnSwipeListener listener : mListeners) {
                    listener.onDragScrolled(mScrollPercent);
                }
            }

            if (mScrollPercent > 1) {
                if (mFragment != null) {
                    if (mCallOnDestroyView) return;

                    if (mPreFragment instanceof ISupportFragment) {
                        ((ISupportFragment) mPreFragment).getSupportDelegate().mLockAnim = true;
                    }
                    if (!((Fragment)mFragment).isDetached()) {
                        mFragment.getSupportDelegate().mLockAnim = true;
                        mFragment.getSupportDelegate().pop();
                        ((Fragment)mFragment).getFragmentManager().executePendingTransactions();
                        mFragment.getSupportDelegate().mLockAnim = false;
                    }
                    if (mPreFragment instanceof ISupportFragment) {
                        ((ISupportFragment) mPreFragment).getSupportDelegate().mLockAnim = false;
                    }
                } else {
                    if (!mActivity.isFinishing()) {
                        mActivity.finish();
                        mActivity.overridePendingTransition(0, 0);
                    }
                }
            }
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            if (mFragment != null) {
                return 1;
            }
            if (mActivity instanceof ISwipeBackActivity && ((ISwipeBackActivity) mActivity).swipeBackPriority()) {
                return 1;
            }
            return 0;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final int childWidth = releasedChild.getWidth();

            int left = 0, top = 0;
            if ((mCurrentSwipeOrientation & EDGE_LEFT) != 0) {
                left = xvel > 0 || xvel == 0 && mScrollPercent > mScrollFinishThreshold ? (childWidth
                        + mShadowLeft.getIntrinsicWidth() + OVERSCROLL_DISTANCE) : 0;
            } else if ((mCurrentSwipeOrientation & EDGE_RIGHT) != 0) {
                left = xvel < 0 || xvel == 0 && mScrollPercent > mScrollFinishThreshold ? -(childWidth
                        + mShadowRight.getIntrinsicWidth() + OVERSCROLL_DISTANCE) : 0;
            }

            mHelper.settleCapturedViewAt(left, top);
            invalidate();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mListeners != null && !mListeners.isEmpty()) {
                for (OnSwipeListener listener : mListeners) {
                    listener.onDragStateChange(state);
                }
            }
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            super.onEdgeTouched(edgeFlags, pointerId);
            if ((mEdgeFlag & edgeFlags) != 0) {
                mCurrentSwipeOrientation = edgeFlags;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mEnable) return super.onInterceptTouchEvent(ev);
        try {
            return mHelper.shouldInterceptTouchEvent(ev);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEnable) return super.onTouchEvent(event);
        mHelper.processTouchEvent(event);
        return true;
    }
}
