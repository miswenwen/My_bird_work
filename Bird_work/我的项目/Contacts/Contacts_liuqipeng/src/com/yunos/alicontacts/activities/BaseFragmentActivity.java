
package com.yunos.alicontacts.activities;

import android.app.ActionBar;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.yunos.alicontacts.R;

import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick2;
import hwdroid.widget.ActionBar.ActionBarView.OnOptionMenuClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick2;
import hwdroid.widget.ActionBar.ActionBarView.OnTitle2ItemClick;
import hwdroid.widget.FooterBar.FooterBar;
import yunos.support.v4.app.FragmentActivity;

public abstract class BaseFragmentActivity extends FragmentActivity {

    private static final boolean DEBUG = true;
    private static final String TAG = "BaseFragmentActivity";
    private ActionBarView mActionBarView;
    private BaseMainViewHost mMainHost;
    private int mHeaderColor;
    protected SystemBarColorManager mSystemBarColorManager;

    public BaseFragmentActivity() {
        super();
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        ensureLayout();
        super.onRestoreInstanceState(savedInstanceState);
    }

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
            mActionBarView.setTitleColor(getResources().getColor(R.color.activity_header_text_color));
            mActionBarView.setBackgroundResource(R.drawable.actionbar_view_line);
        }
        setSystemBar(R.color.title_color);
    }

    protected void setSystemBar(int color) {
        setSystemBar(color, getActionBar() != null);
    }

    protected void setSystemBar(int color, boolean hasActionbar) {
        mHeaderColor = color;
        setSystemBar(color, hasActionbar, getResources().getBoolean(R.bool.contact_dark_mode));
    }

    protected void setSystemBar(int color, boolean hasActionbar, boolean isDarkMode) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (mSystemBarColorManager == null) {
            mSystemBarColorManager = new SystemBarColorManager(this);
        }
        mSystemBarColorManager.setViewFitsSystemWindows(this, hasActionbar);
        mSystemBarColorManager.setStatusBarColor(getResources().getColor(color));
        mSystemBarColorManager.setStatusBarDarkMode(this, isDarkMode);
    }

    public void setSearchBar() {
        setSystemBar(R.color.aui_bg_color_white, getActionBar() != null, true);
    }

    public void restoreSearchBar() {
        setSystemBar(mHeaderColor, getActionBar() != null, getResources().getBoolean(R.bool.contact_dark_mode));
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ensureLayout();
        if (DEBUG) {
            Log.d(TAG, "onPostCreate() called."); // This will check whether
                                                  // setContentView
                                                  // called.(first
                                                  // check init called)
        }
    }

    public int createLayout() {
        return com.yunos.alicontacts.R.layout.base_content_normal;
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

    public void addFooterView(View v) {
    }

    public void onContentChanged() {
        super.onContentChanged();

        onPreContentChanged();
        onPostContentChanged();
    }

    public void onPreContentChanged() {
        if (DEBUG) {
            Log.d(TAG, "onPreContentChanged() called."); // This will check
                                                         // whether
                                                         // onContentChanged
                                                         // called.(second check
                                                         // callback called
                                                         // based
                                                         // first check)
        }
        mMainHost = (BaseMainViewHost) findViewById(com.yunos.alicontacts.R.id.base_action_bar_host);
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
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), 0);
                if (activityInfo.labelRes != 0) {
                    setTitle2(this.getResources().getText(activityInfo.labelRes));
                }
            } catch (NameNotFoundException e) {
                // Do nothing
                android.util.Log.e("BaseFragmentActivity", "onPostContentChanged() exception", e);
            }
        }

        // getWindow().getDecorView().setBackgroundResource(R.drawable.hw_actionbar_background);
    }

    /**
     * set actionbar title.
     *
     * @param title
     */

    public void setTitle2(CharSequence title) {
        ensureLayout();
        if (mActionBarView != null) {
            mActionBarView.setTitle(title);
        }
    }

    /**
     * set actionbar title.
     *
     * @param title
     */

    public void setTitle2(CharSequence title, OnTitle2ItemClick click) {
        ensureLayout();
        if (mActionBarView != null) {
            mActionBarView.setTitle(title);
            mActionBarView.setOnTitle2ItemClickListener(click);
        }
    }

    /**
     * set actionbar sub title.
     *
     * @param title
     */

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
        setActionBarView(view, new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
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
    public View getActionBarView() {
        return mActionBarView;
    }

    /**
     * get footerbar view. user the view instance, you can set any custom view.
     */
    public FooterBar getFooterBarImpl() {
        if (mMainHost != null) {
            return mMainHost.getFooterBarImpl();
        } else {
            return null;
        }
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

        ImageView view = new ImageView(this);
        view.setImageResource(R.drawable.actionbar_back_selector);
        mActionBarView.addLeftItem(view);
        mActionBarView.setOnLeftWidgetItemClickListener(new OnLeftWidgetItemClick() {

            @Override
            public void onLeftWidgetItemClick() {
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
