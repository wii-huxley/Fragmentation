
package com.huxley.fragmentation.swipeback;

import android.os.Bundle;

import com.huxley.fragmentation.SwipeBackLayout;
import com.huxley.fragmentation.base.SupportActivity;
import com.huxley.fragmentation.swipeback.core.ISwipeBackActivity;
import com.huxley.fragmentation.swipeback.core.SwipeBackActivityDelegate;

public class SwipeBackActivity extends SupportActivity implements ISwipeBackActivity {
    final SwipeBackActivityDelegate mDelegate = new SwipeBackActivityDelegate(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDelegate.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDelegate.onPostCreate(savedInstanceState);
    }

    @Override
    public SwipeBackLayout getSwipeBackLayout() {
        return mDelegate.getSwipeBackLayout();
    }

    @Override
    public void setSwipeBackEnable(boolean enable) {
        mDelegate.setSwipeBackEnable(enable);
    }

    @Override
    public boolean swipeBackPriority() {
        return mDelegate.swipeBackPriority();
    }
}
