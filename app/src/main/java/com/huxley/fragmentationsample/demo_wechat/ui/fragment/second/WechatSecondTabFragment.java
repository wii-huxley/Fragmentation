package com.huxley.fragmentationsample.demo_wechat.ui.fragment.second;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.huxley.fragmentationsample.R;
import com.huxley.fragmentationsample.demo_wechat.adapter.WechatPagerFragmentAdapter;
import com.huxley.fragmentationsample.demo_wechat.base.BaseMainFragment;

/**
 * Created by YoKeyword on 16/6/30.
 */
public class WechatSecondTabFragment extends BaseMainFragment {
    private TabLayout mTab;
    private Toolbar   mToolbar;
    private ViewPager mViewPager;

    public static WechatSecondTabFragment newInstance() {

        Bundle args = new Bundle();

        WechatSecondTabFragment fragment = new WechatSecondTabFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wechat_fragment_tab_second, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mTab = (TabLayout) view.findViewById(R.id.tab);
        mViewPager = (ViewPager) view.findViewById(R.id.viewPager);

        mToolbar.setTitle(R.string.contact);

        mTab.addTab(mTab.newTab().setText(R.string.all));
        mTab.addTab(mTab.newTab().setText(R.string.stranger));
    }

    @Override
    public void onLazyInitView(@Nullable Bundle savedInstanceState) {
        super.onLazyInitView(savedInstanceState);
        mViewPager.setAdapter(new WechatPagerFragmentAdapter(getChildFragmentManager()
                , getString(R.string.all), getString(R.string.stranger)));
        mTab.setupWithViewPager(mViewPager);
    }
}
