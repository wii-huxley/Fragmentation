package com.huxley.fragmentation;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentationHack;
import android.view.MotionEvent;

import com.huxley.fragmentation.anim.DefaultVerticalAnimator;
import com.huxley.fragmentation.anim.FragmentAnimator;
import com.huxley.fragmentation.debug.DebugStackDelegate;

public class SupportActivityDelegate {
    private ISupportActivity mSupport;
    private FragmentActivity mActivity;

    boolean mPopMultipleNoAnim = false;
    boolean mFragmentClickable = true;

    private TransactionDelegate mTransactionDelegate;
    private FragmentAnimator mFragmentAnimator;
    private int mDefaultFragmentBackground = 0;
    private DebugStackDelegate mDebugStackDelegate;

    public SupportActivityDelegate(ISupportActivity support) {
        if (!(support instanceof Activity))
            throw new RuntimeException("Must extends FragmentActivity/AppCompatActivity");
        this.mSupport = support;
        this.mActivity = (FragmentActivity) support;
    }

    public ExtraTransaction extraTransaction() {
        return new ExtraTransaction.ExtraTransactionImpl<>(getTopFragment(), getTransactionDelegate(), true);
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        mTransactionDelegate = getTransactionDelegate();
        mDebugStackDelegate = new DebugStackDelegate(mActivity);

        mFragmentAnimator = mSupport.onCreateFragmentAnimator();
        mDebugStackDelegate.onCreate(Fragmentation.getDefault().getMode());
    }

    public TransactionDelegate getTransactionDelegate() {
        if (mTransactionDelegate == null) {
            mTransactionDelegate = new TransactionDelegate(mSupport);
        }
        return mTransactionDelegate;
    }

    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        mDebugStackDelegate.onPostCreate(Fragmentation.getDefault().getMode());
    }

    public FragmentAnimator getFragmentAnimator() {
        return mFragmentAnimator.copy();
    }

    public void setFragmentAnimator(FragmentAnimator fragmentAnimator) {
        this.mFragmentAnimator = fragmentAnimator;

        for (Fragment fragment : FragmentationHack.getActiveFragments(getSupportFragmentManager())) {
            if (fragment instanceof ISupportFragment) {
                ISupportFragment iF = (ISupportFragment) fragment;
                SupportFragmentDelegate delegate = iF.getSupportDelegate();
                if (delegate.mAnimByActivity) {
                    delegate.mFragmentAnimator = fragmentAnimator.copy();
                    if (delegate.mAnimHelper != null) {
                        delegate.mAnimHelper.notifyChanged(delegate.mFragmentAnimator);
                    }
                }
            }
        }
    }

    public FragmentAnimator onCreateFragmentAnimator() {
        return new DefaultVerticalAnimator();
    }

    public void setDefaultFragmentBackground(@DrawableRes int backgroundRes) {
        mDefaultFragmentBackground = backgroundRes;
    }

    public int getDefaultFragmentBackground() {
        return mDefaultFragmentBackground;
    }

    public void showFragmentStackHierarchyView() {
        mDebugStackDelegate.showFragmentStackHierarchyView();
    }

    public void logFragmentStackHierarchy(String TAG) {
        mDebugStackDelegate.logFragmentRecords(TAG);
    }

    public void onBackPressed() {
        if (!mFragmentClickable) {
            mFragmentClickable = true;
        }

        // 获取activeFragment:即从栈顶开始 状态为show的那个Fragment
        ISupportFragment activeFragment = SupportHelper.getActiveFragment(getSupportFragmentManager());
        if (mTransactionDelegate.dispatchBackPressedEvent(activeFragment)) return;

        mSupport.onBackPressedSupport();
    }

    public void onBackPressedSupport() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            pop();
        } else {
            ActivityCompat.finishAfterTransition(mActivity);
        }
    }

    public void onDestroy() {
        mDebugStackDelegate.onDestroy();
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 防抖动(防止点击速度过快)
        return !mFragmentClickable;
    }

    public void loadRootFragment(int containerId, ISupportFragment toFragment) {
        loadRootFragment(containerId, toFragment, true, false);
    }

    public void loadRootFragment(int containerId, ISupportFragment toFragment, boolean addToBackStack, boolean allowAnimation) {
        mTransactionDelegate.loadRootTransaction(getSupportFragmentManager(), containerId, toFragment, addToBackStack, allowAnimation);
    }

    public void loadMultipleRootFragment(int containerId, int showPosition, ISupportFragment... toFragments) {
        mTransactionDelegate.loadMultipleRootTransaction(getSupportFragmentManager(), containerId, showPosition, toFragments);
    }

    public void showHideFragment(ISupportFragment showFragment) {
        showHideFragment(showFragment, null);
    }

    public void showHideFragment(ISupportFragment showFragment, ISupportFragment hideFragment) {
        mTransactionDelegate.showHideFragment(getSupportFragmentManager(), showFragment, hideFragment);
    }

    public void start(ISupportFragment toFragment) {
        start(toFragment, ISupportFragment.STANDARD);
    }

    public void start(ISupportFragment toFragment, @ISupportFragment.LaunchMode int launchMode) {
        mTransactionDelegate.dispatchStartTransaction(getSupportFragmentManager(), getTopFragment(), toFragment, 0, launchMode, TransactionDelegate.TYPE_ADD);
    }

    public void startForResult(ISupportFragment toFragment, int requestCode) {
        mTransactionDelegate.dispatchStartTransaction(getSupportFragmentManager(), getTopFragment(), toFragment, requestCode, ISupportFragment.STANDARD, TransactionDelegate.TYPE_ADD_RESULT);
    }

    public void startWithPop(ISupportFragment toFragment) {
        mTransactionDelegate.dispatchStartTransaction(getSupportFragmentManager(), getTopFragment(), toFragment, 0, ISupportFragment.STANDARD, TransactionDelegate.TYPE_ADD_WITH_POP);
    }

    public void replaceFragment(ISupportFragment toFragment, boolean addToBackStack) {
        mTransactionDelegate.dispatchStartTransaction(getSupportFragmentManager(), getTopFragment(), toFragment, 0, ISupportFragment.STANDARD, addToBackStack ? TransactionDelegate.TYPE_REPLACE : TransactionDelegate.TYPE_REPLACE_DONT_BACK);
    }

    public void pop() {
        mTransactionDelegate.back(getSupportFragmentManager());
    }

    public void popTo(Class<?> targetFragmentClass, boolean includeTargetFragment) {
        popTo(targetFragmentClass, includeTargetFragment, null);
    }

    public void popTo(Class<?> targetFragmentClass, boolean includeTargetFragment, Runnable afterPopTransactionRunnable) {
        popTo(targetFragmentClass, includeTargetFragment, afterPopTransactionRunnable, 0);
    }

    public void popTo(Class<?> targetFragmentClass, boolean includeTargetFragment, Runnable afterPopTransactionRunnable, int popAnim) {
        mTransactionDelegate.popTo(targetFragmentClass.getName(), includeTargetFragment, afterPopTransactionRunnable, getSupportFragmentManager(), popAnim);
    }

    private FragmentManager getSupportFragmentManager() {
        return mActivity.getSupportFragmentManager();
    }

    private ISupportFragment getTopFragment() {
        return SupportHelper.getTopFragment(getSupportFragmentManager());
    }
}