package com.huxley.fragmentation.swipeback;

import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.view.View;

import com.huxley.fragmentation.SwipeBackLayout;
import com.huxley.fragmentation.base.SupportFragment;
import com.huxley.fragmentation.swipeback.core.ISwipeBackFragment;
import com.huxley.fragmentation.swipeback.core.SwipeBackFragmentDelegate;

public class SwipeBackFragment extends SupportFragment implements ISwipeBackFragment {
    final SwipeBackFragmentDelegate mDelegate = new SwipeBackFragmentDelegate(this);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDelegate.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDelegate.onViewCreated(view, savedInstanceState);
    }

    @Override
    public View attachToSwipeBack(View view) {
        return mDelegate.attachToSwipeBack(view);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        mDelegate.onHiddenChanged(hidden);
    }

    public SwipeBackLayout getSwipeBackLayout() {
        return mDelegate.getSwipeBackLayout();
    }

    public void setSwipeBackEnable(boolean enable) {
        mDelegate.setSwipeBackEnable(enable);
    }

    public void setParallaxOffset(@FloatRange(from = 0.0f, to = 1.0f) float offset) {
        mDelegate.setParallaxOffset(offset);
    }

    @Override
    public void onDestroyView() {
        mDelegate.onDestroyView();
        super.onDestroyView();
    }
}