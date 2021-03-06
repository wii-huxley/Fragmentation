package com.huxley.fragmentationsample.demo_flow.ui.fragment_swipe_back;

import android.support.v7.widget.Toolbar;
import android.view.View;

import com.huxley.fragmentation.swipeback.SwipeBackFragment;
import com.huxley.fragmentationsample.R;

/**
 * Created by YoKeyword on 16/4/21.
 */
public class BaseSwipeBackFragment extends SwipeBackFragment {

    void _initToolbar(Toolbar toolbar) {
        toolbar.setTitle("SwipeBackActivity's Fragment");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _mActivity.onBackPressed();
            }
        });
    }
}
