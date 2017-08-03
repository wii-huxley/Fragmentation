package com.huxley.fragmentation.swipeback.core;


import com.huxley.fragmentation.SwipeBackLayout;

public interface ISwipeBackActivity {

    SwipeBackLayout getSwipeBackLayout();

    void setSwipeBackEnable(boolean enable);

    boolean swipeBackPriority();
}
