package com.huxley.fragmentationsample.demo_flow;

import android.os.Bundle;

import com.huxley.fragmentation.SwipeBackLayout;
import com.huxley.fragmentation.anim.DefaultHorizontalAnimator;
import com.huxley.fragmentation.anim.FragmentAnimator;
import com.huxley.fragmentation.swipeback.SwipeBackActivity;
import com.huxley.fragmentationsample.R;
import com.huxley.fragmentationsample.demo_flow.ui.fragment_swipe_back.FirstSwipeBackFragment;

/**
 * Created by YoKeyword on 16/4/19.
 */
public class SwipeBackSampleActivity extends SwipeBackActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe_back);

        if (findFragment(FirstSwipeBackFragment.class) == null) {
            loadRootFragment(R.id.fl_container, FirstSwipeBackFragment.newInstance());
        }

        getSwipeBackLayout().setEdgeOrientation(SwipeBackLayout.EDGE_ALL);
    }

    /**
     * 限制SwipeBack的条件,默认栈内Fragment数 <= 1时 , 优先滑动退出Activity , 而不是Fragment
     *
     * @return true: Activity可以滑动退出, 并且总是优先;  false: Activity不允许滑动退出
     */
    @Override
    public boolean swipeBackPriority() {
        return super.swipeBackPriority();
    }

    public FragmentAnimator onCreateFragmentAnimator() {
        return new DefaultHorizontalAnimator();
    }
}
