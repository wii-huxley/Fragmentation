package com.huxley.fragmentation.swipeback.core;

import android.support.annotation.FloatRange;
import android.view.View;

import com.huxley.fragmentation.SwipeBackLayout;


public interface ISwipeBackFragment {

    View attachToSwipeBack(View view);

    SwipeBackLayout getSwipeBackLayout();

    void setSwipeBackEnable(boolean enable);

    void setParallaxOffset(@FloatRange(from = 0.0f, to = 1.0f) float offset);
}
