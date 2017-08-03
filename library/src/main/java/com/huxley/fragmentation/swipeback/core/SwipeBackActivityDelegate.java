package com.huxley.fragmentation.swipeback.core;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ViewGroup;

import com.huxley.fragmentation.ISupportActivity;
import com.huxley.fragmentation.SwipeBackLayout;

public class SwipeBackActivityDelegate {
    private FragmentActivity mActivity;
    private SwipeBackLayout  mSwipeBackLayout;

    public SwipeBackActivityDelegate(ISwipeBackActivity swipeBackActivity) {
        if (!(swipeBackActivity instanceof FragmentActivity) || !(swipeBackActivity instanceof ISupportActivity))
            throw new RuntimeException("Must extends FragmentActivity/AppCompatActivity and implements ISupportActivity");
        mActivity = (FragmentActivity) swipeBackActivity;
    }

    public void onCreate(Bundle savedInstanceState) {
        onActivityCreate();
    }

    public void onPostCreate(Bundle savedInstanceState) {
        mSwipeBackLayout.attachToActivity(mActivity);
    }

    public SwipeBackLayout getSwipeBackLayout() {
        return mSwipeBackLayout;
    }

    public void setSwipeBackEnable(boolean enable) {
        mSwipeBackLayout.setEnableGesture(enable);
    }

    public boolean swipeBackPriority() {
        return mActivity.getSupportFragmentManager().getBackStackEntryCount() <= 1;
    }

    private void onActivityCreate() {
        mActivity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mActivity.getWindow().getDecorView().setBackgroundDrawable(null);
        mSwipeBackLayout = new SwipeBackLayout(mActivity);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mSwipeBackLayout.setLayoutParams(params);
    }
}
