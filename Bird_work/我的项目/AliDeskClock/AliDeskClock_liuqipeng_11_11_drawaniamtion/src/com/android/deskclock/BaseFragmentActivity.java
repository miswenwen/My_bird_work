package com.android.deskclock;

import android.app.ActionBar;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import yunos.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.android.deskclock.R;

import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionBar.ActionBarView.OnBackKeyItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick2;
import hwdroid.widget.ActionBar.ActionBarView.OnOptionMenuClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick2;
import hwdroid.widget.ActionBar.ActionBarView.OnTitle2ItemClick;
import hwdroid.widget.FooterBar.FooterBar;

public abstract class BaseFragmentActivity extends FragmentActivity {
    private ActionBarView mActionBarView;
    private BaseMainViewHost mMainHost;

    public BaseFragmentActivity() {
        super();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        ensureLayout();
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = this.getActionBar();

        if (null != actionBar) {
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);

            mActionBarView = new ActionBarView(this);
            setActionBarView(mActionBarView);
            mActionBarView.setBackgroundResource(hwdroid.R.drawable.hw_actionbar_background);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ensureLayout();
    }

    public int createLayout() {
        return R.layout.content_normal;
    }

    /**
     * Call this method to ensure a layout has already been inflated and
     * attached to the top-level View of this Activity.
     */
    public void ensureLayout() {
        if (!verifyLayout()) {
            setContentView(createLayout());
        }
    }

    /**
     * Verify the given layout contains everything needed by this Activity. A
     * BaseFragmentActivity, for instance, manages an {@link ActionBarHost}. As
     * a result this method will return true of the current layout contains such
     * a widget.
     *
     * @return true if the current layout fits to the current Activity widgets
     *         requirements
     */
    protected boolean verifyLayout() {
        return mMainHost != null;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        onPreContentChanged();
        onPostContentChanged();
    }

    public void onPreContentChanged() {
        mMainHost = (BaseMainViewHost) findViewById(R.id.hw_action_bar_host);
        if (mMainHost == null) {
            throw new RuntimeException("<HWDroid> hasn't R.id.hw_action_bar_host");
        }
    }

    public void onPostContentChanged() {

        boolean titleSet = false;

        if (!titleSet) {
            // No title has been set via the Intent. Let's look in the
            // ActivityInfo
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(
                        getComponentName(), 0);
                if (activityInfo.labelRes != 0) {
                    setTitle2(this.getResources().getText(activityInfo.labelRes));
                }
            } catch (NameNotFoundException e) {
                // Do nothing
            }
        }

        // getWindow().getDecorView().setBackgroundResource(R.drawable.hw_actionbar_background);
    }

    public void setTitle2(CharSequence title) {
        ensureLayout();
        if (mActionBarView != null) {
            mActionBarView.setTitle(title);
        }
    }

    public void setTitle2(CharSequence title, OnTitle2ItemClick click) {
        ensureLayout();
        if (mActionBarView != null) {
            mActionBarView.setTitle(title);
            mActionBarView.setOnTitle2ItemClickListener(click);
        }
    }

    public void setSubTitle2(CharSequence title) {
        ensureLayout();
        if (mActionBarView != null) {
            mActionBarView.setSubTitle(title);
        }
    }

    /**
     * set action bar custom view
     *
     * @param view
     * @param params
     */
    public void setActionBarView(ActionBarView view, ActionBar.LayoutParams params) {
        if (getActionBar() != null) {
            getActionBar().setCustomView(view, params);
        } else {
            mActionBarView = view;
        }
    }

    /**
     * set action bar custom view by the default params.
     *
     * @param view
     */
    public void setActionBarView(ActionBarView view) {
        setActionBarView(view, new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }

    /**
     * get activity view's contentview.
     */
    public FrameLayout getContentView() {
        ensureLayout();
        return mMainHost.getContentView();
    }

    /**
     * get ActionBarView view. user the view instance, you can set any custom
     * view.
     */
    public ActionBarView getActionBarView() {
        return mActionBarView;
    }

    /**
     * get footerbar view. user the view instance, you can set any custom view.
     */
    public FooterBar getFooterBarImpl() {
        return mMainHost.getFooterBarImpl();
    }

    /**
     * <p>
     * Set the activity content from a layout resource. The resource will be
     * inflated, adding all top-level views to the activity.
     * </p>
     * <p>
     * This method is an equivalent to setContentView(int) that automatically
     * wraps the given layout in an {@link ActionBarHost} if needed..
     * </p>
     *
     * @param resID Resource ID to be inflated.
     * @see #setActionBarContentView(View)
     * @see #setActionBarContentView(View, LayoutParams)
     */
    public void setActivityContentView(int resID) {
        final FrameLayout contentView = getContentView();
        contentView.removeAllViews();
        LayoutInflater.from(this).inflate(resID, contentView);
    }

    /**
     * <p>
     * Set the activity content to an explicit view. This view is placed
     * directly into the activity's view hierarchy. It can itself be a complex
     * view hierarchy.
     * </p>
     * <p>
     * This method is an equivalent to setContentView(View, LayoutParams) that
     * automatically wraps the given layout in an {@link ActionBarHost} if
     * needed.
     * </p>
     *
     * @param view The desired content to display.
     * @param params Layout parameters for the view.
     * @see #setActionBarContentView(View)
     * @see #setActionBarContentView(int)
     */
    public void setActivityContentView(View view, LayoutParams params) {
        final FrameLayout contentView = getContentView();
        contentView.removeAllViews();
        contentView.addView(view, params);
    }

    /**
     * <p>
     * Set the activity content to an explicit view. This view is placed
     * directly into the activity's view hierarchy. It can itself be a complex
     * view hierarchy.
     * </p>
     * <p>
     * This method is an equivalent to setContentView(View) that automatically
     * wraps the given layout in an {@link ActionBarHost} if needed.
     * </p>
     *
     * @param view The desired content to display.
     * @see #setActionBarContentView(int)
     * @see #setActionBarContentView(View, LayoutParams)
     */
    public void setActivityContentView(View view) {
        final FrameLayout contentView = getContentView();
        contentView.removeAllViews();
        contentView.addView(view);
    }

    @SuppressWarnings("deprecation")
    public void setActionBarBackgroudResource(int resId) {
        Drawable d = this.getResources().getDrawable(resId);
        mActionBarView.setBackgroundDrawable(d);
        // getWindow().getDecorView().setBackgroundDrawable(d);
    }

    /**
     * set the back widget's status. if true, will set the back widget visible.
     * if false, will set the back widget gone. if you call showBackKey() and
     * setLeftWidgetView() together, will show the laster widget.
     *
     * @param show
     */
    public void showBackKey(boolean show) {
        if (mActionBarView == null)
            return;

        mActionBarView.showBackKey(show, new OnBackKeyItemClick() {

            @Override
            public void onBackKeyItemClick() {
                onBackKey();
            }
        });
    }

    public void onBackKey() {
        finish();
    }

    public void setLeftWidgetView(View v) {
        if (mActionBarView != null && v != null) {
            mActionBarView.addLeftItem(v);
        }
    }

    public void setRightWidgetView(View v) {
        if (mActionBarView != null && v != null) {
            mActionBarView.addRightItem(v);
        }
    }

    public void removeLeftWidgetView() {
        if (mActionBarView == null)
            return;
        mActionBarView.removeLeftItem();
    }

    public void removeRightWidgetView() {
        if (mActionBarView == null)
            return;
        mActionBarView.removeRightItem();
    }

    public void setLeftWidgetClickListener(OnLeftWidgetItemClick click) {
        if (mActionBarView == null)
            return;
        mActionBarView.setOnLeftWidgetItemClickListener(click);
    }

    public void setRightWidgetClickListener(OnRightWidgetItemClick click) {
        if (mActionBarView == null)
            return;
        mActionBarView.setOnRightWidgetItemClickListener(click);
    }

    /**
     * set listener on the left side of {@link ActionBarView}
     *
     * @param click
     */
    public void setLeftWidgetClickListener2(OnLeftWidgetItemClick2 click) {
        if (mActionBarView == null)
            return;
        mActionBarView.setOnLeftWidgetItemClickListener2(click);
    }

    /**
     * set listener on the right side of {@link ActionBarView}
     *
     * @param click
     */
    public void setRightWidgetClickListener2(OnRightWidgetItemClick2 click) {
        if (mActionBarView == null)
            return;
        mActionBarView.setOnRightWidgetItemClickListener2(click);
    }

    public void setOptionTitle2(CharSequence title) {
        ensureLayout();
        if (mActionBarView != null) {
            mActionBarView.setOptionTitle(title);
        }
    }

    public void setOptionItems(CharSequence[] optionItem, OnOptionMenuClick click) {
        ensureLayout();
        if (mActionBarView != null) {
            mActionBarView.setOptionItems(optionItem, click);
        }
    }

    public void setLeftWidgetItemEnabled(boolean enabled) {
        if (mActionBarView != null) {
            mActionBarView.setLeftWidgetItemEnable(enabled);
        }
    }

    public void setRightWidgetItemEnabled(boolean enabled) {
        if (mActionBarView != null) {
            mActionBarView.setRightWidgetItemEnable(enabled);
        }
    }

    public boolean isLeftWidgetItemEnabled() {
        if (mActionBarView != null) {
            return mActionBarView.isLeftWidgetItemEnabled();
        }

        return false;
    }

    public boolean isRightWidgetItemEnabled() {
        if (mActionBarView != null) {
            return mActionBarView.isRightWidgetItemEnabled();
        }

        return false;
    }

}
