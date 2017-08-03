package com.huxley.fragmentation;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentationHack;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.List;

public class SupportHelper {
    private static final long SHOW_SPACE = 200L;

    private SupportHelper() {
    }

    public static void showSoftInput(final View view) {
        if (view == null || view.getContext() == null) return;
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        view.requestFocus();
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
            }
        }, SHOW_SPACE);
    }

    public static void hideSoftInput(View view) {
        if (view == null || view.getContext() == null) return;
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showFragmentStackHierarchyView(ISupportActivity support) {
        support.getSupportDelegate().showFragmentStackHierarchyView();
    }

    public static void logFragmentStackHierarchy(ISupportActivity support, String TAG) {
        support.getSupportDelegate().logFragmentStackHierarchy(TAG);
    }

    public static ISupportFragment getTopFragment(FragmentManager fragmentManager) {
        List<Fragment> fragmentList = FragmentationHack.getActiveFragments(fragmentManager);
        if (fragmentList == null) return null;

        for (int i = fragmentList.size() - 1; i >= 0; i--) {
            Fragment fragment = fragmentList.get(i);
            if (fragment instanceof ISupportFragment) {
                return (ISupportFragment) fragment;
            }
        }
        return null;
    }

    public static ISupportFragment getPreFragment(Fragment fragment) {
        FragmentManager fragmentManager = fragment.getFragmentManager();
        if (fragmentManager == null) return null;

        List<Fragment> fragmentList = FragmentationHack.getActiveFragments(fragmentManager);
        if (fragmentList == null) return null;

        int index = fragmentList.indexOf(fragment);
        for (int i = index - 1; i >= 0; i--) {
            Fragment preFragment = fragmentList.get(i);
            if (preFragment instanceof ISupportFragment) {
                return (ISupportFragment) preFragment;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ISupportFragment> T findFragment(FragmentManager fragmentManager, Class<T> fragmentClass) {
        return findStackFragment(fragmentClass, null, fragmentManager);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ISupportFragment> T findFragment(FragmentManager fragmentManager, String fragmentTag) {
        return findStackFragment(null, fragmentTag, fragmentManager);
    }

    public static ISupportFragment getActiveFragment(FragmentManager fragmentManager) {
        return getActiveFragment(fragmentManager, null);
    }

    @SuppressWarnings("unchecked")
    static <T extends ISupportFragment> T findStackFragment(Class<T> fragmentClass, String toFragmentTag, FragmentManager fragmentManager) {
        Fragment fragment = null;

        if (toFragmentTag == null) {
            List<Fragment> fragmentList = FragmentationHack.getActiveFragments(fragmentManager);
            if (fragmentList == null) return null;

            int sizeChildFrgList = fragmentList.size();

            for (int i = sizeChildFrgList - 1; i >= 0; i--) {
                Fragment brotherFragment = fragmentList.get(i);
                if (brotherFragment instanceof ISupportFragment && brotherFragment.getClass().getName().equals(fragmentClass.getName())) {
                    fragment = brotherFragment;
                    break;
                }
            }
        } else {
            fragment = fragmentManager.findFragmentByTag(toFragmentTag);
            if (fragment == null) return null;
        }
        return (T) fragment;
    }

    private static ISupportFragment getActiveFragment(FragmentManager fragmentManager, ISupportFragment parentFragment) {
        List<Fragment> fragmentList = FragmentationHack.getActiveFragments(fragmentManager);
        if (fragmentList == null) {
            return parentFragment;
        }
        for (int i = fragmentList.size() - 1; i >= 0; i--) {
            Fragment fragment = fragmentList.get(i);
            if (fragment instanceof ISupportFragment) {
                if (fragment.isResumed() && !fragment.isHidden() && fragment.getUserVisibleHint()) {
                    return getActiveFragment(fragment.getChildFragmentManager(), (ISupportFragment) fragment);
                }
            }
        }
        return parentFragment;
    }
}
