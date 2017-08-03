package com.huxley.fragmentation;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.animation.Animation;

import com.huxley.fragmentation.anim.FragmentAnimator;
import com.huxley.fragmentation.helper.internal.AnimatorHelper;
import com.huxley.fragmentation.helper.internal.ResultRecord;
import com.huxley.fragmentation.helper.internal.TransactionRecord;
import com.huxley.fragmentation.helper.internal.VisibleDelegate;

public class SupportFragmentDelegate {
    static final int STATUS_UN_ROOT = 0;
    static final int STATUS_ROOT_ANIM_DISABLE = 1;
    static final int STATUS_ROOT_ANIM_ENABLE = 2;

    private int mRootStatus = STATUS_UN_ROOT;

    boolean          mIsSharedElement;
    FragmentAnimator mFragmentAnimator;
    AnimatorHelper   mAnimHelper;
    boolean          mLockAnim;

    private Handler mHandler;
    private boolean mFirstCreateView = true;
    private boolean mReplaceMode;
    private boolean mIsHidden = true;
    int mContainerId;

    private TransactionDelegate mTransactionDelegate;
    TransactionRecord mTransactionRecord;
    // SupportVisible
    private VisibleDelegate mVisibleDelegate;
    Bundle mNewBundle;
    private Bundle mSaveInstanceState;

    private   ISupportFragment mSupportF;
    private   Fragment         mFragment;
    protected FragmentActivity _mActivity;
    private   ISupportActivity mSupport;
    boolean mAnimByActivity = true;
    EnterAnimListener mEnterAnimListener;

    public SupportFragmentDelegate(ISupportFragment support) {
        if (!(support instanceof Fragment))
            throw new RuntimeException("Must extends Fragment");
        this.mSupportF = support;
        this.mFragment = (Fragment) support;
    }

    public ExtraTransaction extraTransaction() {
        if (mTransactionDelegate == null)
            throw new RuntimeException(mFragment.getClass().getSimpleName() + " not attach!");

        return new ExtraTransaction.ExtraTransactionImpl<>(mSupportF, mTransactionDelegate, false);
    }

    public void onAttach(Activity activity) {
        if (activity instanceof ISupportActivity) {
            this.mSupport = (ISupportActivity) activity;
            this._mActivity = (FragmentActivity) activity;
            mTransactionDelegate = mSupport.getSupportDelegate().getTransactionDelegate();
        } else {
            throw new RuntimeException(activity.getClass().getSimpleName() + " must impl ISupportActivity!");
        }
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        getVisibleDelegate().onCreate(savedInstanceState);

        Bundle bundle = mFragment.getArguments();
        if (bundle != null) {
            mRootStatus = bundle.getInt(TransactionDelegate.FRAGMENTATION_ARG_ROOT_STATUS, STATUS_UN_ROOT);
            mIsSharedElement = bundle.getBoolean(TransactionDelegate.FRAGMENTATION_ARG_IS_SHARED_ELEMENT, false);
            mContainerId = bundle.getInt(TransactionDelegate.FRAGMENTATION_ARG_CONTAINER);
            mReplaceMode = bundle.getBoolean(TransactionDelegate.FRAGMENTATION_ARG_REPLACE, false);
        }

        if (savedInstanceState == null) {
            getFragmentAnimator();
        } else {
            mSaveInstanceState = savedInstanceState;
            mFragmentAnimator = savedInstanceState.getParcelable(TransactionDelegate.FRAGMENTATION_STATE_SAVE_ANIMATOR);
            mIsHidden = savedInstanceState.getBoolean(TransactionDelegate.FRAGMENTATION_STATE_SAVE_IS_HIDDEN);
            mContainerId = savedInstanceState.getInt(TransactionDelegate.FRAGMENTATION_ARG_CONTAINER);
        }

        // Fix the overlapping BUG on pre-24.0.0
        processRestoreInstanceState(savedInstanceState);
        mAnimHelper = new AnimatorHelper(_mActivity.getApplicationContext(), mFragmentAnimator);
    }

    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if ((mSupport.getSupportDelegate().mPopMultipleNoAnim || mLockAnim)) {
            if (transit == FragmentTransaction.TRANSIT_FRAGMENT_CLOSE && enter) {
                return mAnimHelper.getNoneAnimFixed();
            }
            return mAnimHelper.getNoneAnim();
        }
        if (transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
            if (enter) {
                Animation enterAnim;
                if (mRootStatus == STATUS_ROOT_ANIM_DISABLE) {
                    enterAnim = mAnimHelper.getNoneAnim();
                } else {
                    enterAnim = mAnimHelper.enterAnim;
                }
                fixAnimationListener(enterAnim);
                return enterAnim;
            } else {
                return mAnimHelper.popExitAnim;
            }
        } else if (transit == FragmentTransaction.TRANSIT_FRAGMENT_CLOSE) {
            return enter ? mAnimHelper.popEnterAnim : mAnimHelper.exitAnim;
        } else {
            if (mIsSharedElement && enter) {
                compatSharedElements();
            }

            Animation fixedAnim = mAnimHelper.getViewPagerChildFragmentAnimFixed(mFragment, enter);
            if (fixedAnim != null) return fixedAnim;

            return null;
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        getVisibleDelegate().onSaveInstanceState(outState);
        outState.putParcelable(TransactionDelegate.FRAGMENTATION_STATE_SAVE_ANIMATOR, mFragmentAnimator);
        outState.putBoolean(TransactionDelegate.FRAGMENTATION_STATE_SAVE_IS_HIDDEN, mFragment.isHidden());
        outState.putInt(TransactionDelegate.FRAGMENTATION_ARG_CONTAINER, mContainerId);
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getVisibleDelegate().onActivityCreated(savedInstanceState);

        View view = mFragment.getView();
        if (view != null) {
            view.setClickable(true);
            setBackground(view);
        }

        if (savedInstanceState != null || mRootStatus != STATUS_UN_ROOT || (mFragment.getTag() != null
                && mFragment.getTag().startsWith("android:switcher:")) || (mReplaceMode && !mFirstCreateView)) {
            notifyEnterAnimEnd();
        }

        if (mFirstCreateView) {
            mFirstCreateView = false;
        }
    }

    public void onResume() {
        getVisibleDelegate().onResume();
    }

    public void onPause() {
        getVisibleDelegate().onPause();
    }

    public void onDestroyView() {
        mSupport.getSupportDelegate().mFragmentClickable = true;
        getVisibleDelegate().onDestroyView();
    }

    public void onDestroy() {
        mTransactionDelegate.handleResultRecord(mFragment);
    }

    public void onHiddenChanged(boolean hidden) {
        getVisibleDelegate().onHiddenChanged(hidden);
    }

    public void setUserVisibleHint(boolean isVisibleToUser) {
        getVisibleDelegate().setUserVisibleHint(isVisibleToUser);
    }

    public void enqueueAction(Runnable runnable) {
        getHandler().postDelayed(runnable, mAnimHelper == null ? 0 : mAnimHelper.enterAnim.getDuration());
    }

    public void onEnterAnimationEnd(Bundle savedInstanceState) {
    }

    public void onLazyInitView(@Nullable Bundle savedInstanceState) {
    }

    public void onSupportVisible() {
    }

    public void onSupportInvisible() {
    }

    final public boolean isSupportVisible() {
        return getVisibleDelegate().isSupportVisible();
    }

    public FragmentAnimator onCreateFragmentAnimator() {
        return mSupport.getFragmentAnimator();
    }

    public FragmentAnimator getFragmentAnimator() {
        if (mSupport == null)
            throw new RuntimeException("Fragment has not been attached to Activity!");

        if (mFragmentAnimator == null) {
            mFragmentAnimator = mSupportF.onCreateFragmentAnimator();
            if (mFragmentAnimator == null) {
                mFragmentAnimator = mSupport.getFragmentAnimator();
            }
        }
        return mFragmentAnimator;
    }

    public void setFragmentAnimator(FragmentAnimator fragmentAnimator) {
        this.mFragmentAnimator = fragmentAnimator;
        if (mAnimHelper != null) {
            mAnimHelper.notifyChanged(fragmentAnimator);
        }
        mAnimByActivity = false;
    }

    public void setFragmentResult(int resultCode, Bundle bundle) {
        Bundle args = mFragment.getArguments();
        if (args == null || !args.containsKey(TransactionDelegate.FRAGMENTATION_ARG_RESULT_RECORD)) {
            return;
        }

        ResultRecord resultRecord = args.getParcelable(TransactionDelegate.FRAGMENTATION_ARG_RESULT_RECORD);
        if (resultRecord != null) {
            resultRecord.resultCode = resultCode;
            resultRecord.resultBundle = bundle;
        }
    }

    public void onFragmentResult(int requestCode, int resultCode, Bundle data) {
    }

    public void onNewBundle(Bundle args) {
    }

    public void putNewBundle(Bundle newBundle) {
        this.mNewBundle = newBundle;
    }

    public boolean onBackPressedSupport() {
        return false;
    }

    public void hideSoftInput() {
        SupportHelper.hideSoftInput(mFragment.getView());
    }

    public void showSoftInput(View view) {
        SupportHelper.showSoftInput(view);
    }

    public void loadRootFragment(int containerId, ISupportFragment toFragment) {
        loadRootFragment(containerId, toFragment, true, false);
    }

    public void loadRootFragment(int containerId, ISupportFragment toFragment, boolean addToBackStack, boolean allowAnim) {
        mTransactionDelegate.loadRootTransaction(getChildFragmentManager(), containerId, toFragment, addToBackStack, allowAnim);
    }

    public void loadMultipleRootFragment(int containerId, int showPosition, ISupportFragment... toFragments) {
        mTransactionDelegate.loadMultipleRootTransaction(getChildFragmentManager(), containerId, showPosition, toFragments);
    }

    public void showHideFragment(ISupportFragment showFragment) {
        showHideFragment(showFragment, null);
    }

    public void showHideFragment(ISupportFragment showFragment, ISupportFragment hideFragment) {
        mTransactionDelegate.showHideFragment(getChildFragmentManager(), showFragment, hideFragment);
    }

    public void start(ISupportFragment toFragment) {
        start(toFragment, ISupportFragment.STANDARD);
    }

    public void start(final ISupportFragment toFragment, @ISupportFragment.LaunchMode int launchMode) {
        mTransactionDelegate.dispatchStartTransaction(mFragment.getFragmentManager(), mSupportF, toFragment, 0, launchMode, TransactionDelegate.TYPE_ADD);
    }

    public void startForResult(ISupportFragment toFragment, int requestCode) {
        mTransactionDelegate.dispatchStartTransaction(mFragment.getFragmentManager(), mSupportF, toFragment, requestCode, ISupportFragment.STANDARD, TransactionDelegate.TYPE_ADD_RESULT);
    }

    public void startWithPop(ISupportFragment toFragment) {
        mTransactionDelegate.dispatchStartTransaction(mFragment.getFragmentManager(), mSupportF, toFragment, 0, ISupportFragment.STANDARD, TransactionDelegate.TYPE_ADD_WITH_POP);
    }

    public void replaceFragment(ISupportFragment toFragment, boolean addToBackStack) {
        mTransactionDelegate.dispatchStartTransaction(mFragment.getFragmentManager(), mSupportF, toFragment, 0, ISupportFragment.STANDARD, addToBackStack ? TransactionDelegate.TYPE_REPLACE : TransactionDelegate.TYPE_REPLACE_DONT_BACK);
    }

    public void startChild(ISupportFragment toFragment) {
        startChild(toFragment, ISupportFragment.STANDARD);
    }

    public void startChild(final ISupportFragment toFragment, @ISupportFragment.LaunchMode int launchMode) {
        mTransactionDelegate.dispatchStartTransaction(getChildFragmentManager(), getTopFragment(), toFragment, 0, launchMode, TransactionDelegate.TYPE_ADD);
    }

    public void startChildForResult(ISupportFragment toFragment, int requestCode) {
        mTransactionDelegate.dispatchStartTransaction(getChildFragmentManager(), getTopFragment(), toFragment, requestCode, ISupportFragment.STANDARD, TransactionDelegate.TYPE_ADD_RESULT);
    }

    public void startChildWithPop(ISupportFragment toFragment) {
        mTransactionDelegate.dispatchStartTransaction(getChildFragmentManager(), getTopFragment(), toFragment, 0, ISupportFragment.STANDARD, TransactionDelegate.TYPE_ADD_WITH_POP);
    }

    public void replaceChildFragment(ISupportFragment toFragment, boolean addToBackStack) {
        mTransactionDelegate.dispatchStartTransaction(getChildFragmentManager(), getTopFragment(), toFragment, 0, ISupportFragment.STANDARD, addToBackStack ? TransactionDelegate.TYPE_REPLACE : TransactionDelegate.TYPE_REPLACE_DONT_BACK);
    }

    public void pop() {
        mTransactionDelegate.back(mFragment.getFragmentManager());
    }

    public void popChild() {
        mTransactionDelegate.back(getChildFragmentManager());
    }

    public void popTo(Class<?> targetFragmentClass, boolean includeTargetFragment) {
        getChildFragmentManager().popBackStack();
        popTo(targetFragmentClass, includeTargetFragment, null);
    }

    public void popTo(Class<?> targetFragmentClass, boolean includeTargetFragment, Runnable afterPopTransactionRunnable) {
        popTo(targetFragmentClass, includeTargetFragment, afterPopTransactionRunnable, 0);
    }

    public void popTo(Class<?> targetFragmentClass, boolean includeTargetFragment, Runnable afterPopTransactionRunnable, int popAnim) {
        mTransactionDelegate.popTo(targetFragmentClass.getName(), includeTargetFragment, afterPopTransactionRunnable, mFragment.getFragmentManager(), popAnim);
    }

    public void popToChild(Class<?> targetFragmentClass, boolean includeTargetFragment) {
        popToChild(targetFragmentClass, includeTargetFragment, null);
    }

    public void popToChild(Class<?> targetFragmentClass, boolean includeTargetFragment, Runnable afterPopTransactionRunnable) {
        popToChild(targetFragmentClass, includeTargetFragment, afterPopTransactionRunnable, 0);
    }

    public void popToChild(Class<?> targetFragmentClass, boolean includeTargetFragment, Runnable afterPopTransactionRunnable, int popAnim) {
        mTransactionDelegate.popTo(targetFragmentClass.getName(), includeTargetFragment, afterPopTransactionRunnable, getChildFragmentManager(), popAnim);
    }

    private FragmentManager getChildFragmentManager() {
        return mFragment.getChildFragmentManager();
    }

    private ISupportFragment getTopFragment() {
        return SupportHelper.getTopFragment(getChildFragmentManager());
    }

    private void processRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            FragmentTransaction ft = mFragment.getFragmentManager().beginTransaction();
            if (mIsHidden) {
                ft.hide(mFragment);
            } else {
                ft.show(mFragment);
            }
            ft.commitAllowingStateLoss();
        }
    }

    private void fixAnimationListener(Animation enterAnim) {
        mSupport.getSupportDelegate().mFragmentClickable = false;
        // AnimationListener is not reliable.
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyEnterAnimEnd();
            }
        }, enterAnim.getDuration());

        if (mEnterAnimListener != null) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    mEnterAnimListener.onEnterAnimStart();
                    mEnterAnimListener = null;
                }
            });
        }
    }

    private void compatSharedElements() {
        notifyEnterAnimEnd();
    }

    public void setBackground(View view) {
        if ((mFragment.getTag() != null && mFragment.getTag().startsWith("android:switcher:")) ||
                mRootStatus != STATUS_UN_ROOT ||
                view.getBackground() != null) {
            return;
        }

        int defaultBg = mSupport.getSupportDelegate().getDefaultFragmentBackground();
        if (defaultBg == 0) {
            int background = getWindowBackground();
            view.setBackgroundResource(background);
        } else {
            view.setBackgroundResource(defaultBg);
        }
    }

    private int getWindowBackground() {
        TypedArray a = _mActivity.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.windowBackground
        });
        int background = a.getResourceId(0, 0);
        a.recycle();
        return background;
    }

    private void notifyEnterAnimEnd() {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mFragment == null) return;
                mSupportF.onEnterAnimationEnd(mSaveInstanceState);
            }
        });
        mSupport.getSupportDelegate().mFragmentClickable = true;
    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    public VisibleDelegate getVisibleDelegate() {
        if (mVisibleDelegate == null) {
            mVisibleDelegate = new VisibleDelegate(mSupportF);
        }
        return mVisibleDelegate;
    }

    public FragmentActivity getActivity() {
        return _mActivity;
    }

    interface EnterAnimListener {
        void onEnterAnimStart();
    }
}