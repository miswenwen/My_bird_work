
package com.yunos.alicontacts.activities;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.yunos.alicontacts.R;

import hwdroid.app.HWPreferenceActivity;
import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick;

import java.util.List;

public class BasePreferenceActivity extends HWPreferenceActivity {

    private ActionBarView mActionBarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setPadding(0, 0, 0, 0);
        setSystemBar(R.color.title_color);
    }

    public void showBackKey(boolean show) {
        if (mActionBarView == null)
            return;

        ImageView view = new ImageView(this);
        view.setImageResource(R.drawable.actionbar_back_selector);
        mActionBarView.addLeftItem(view);
        mActionBarView.setOnLeftWidgetItemClickListener(new OnLeftWidgetItemClick() {

            @Override
            public void onLeftWidgetItemClick() {
                finish();
            }
        });
    }

    @Override
    public void loadHeadersFromResource(int resid, List<Header> target) {
        super.loadHeadersFromResource(resid, target);
    }

    protected void setSystemBar(int color) {
        setSystemBar(getResources().getColor(color), getActionBar() == null ? false : true);
    }

    protected void setSystemBar(int color, boolean hasActionbar) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, hasActionbar);
        systemBarManager.setStatusBarColor(color);
        systemBarManager.setStatusBarDarkMode(this, getResources().getBoolean(R.bool.contact_dark_mode));
    }

    /**
     * init actionbar.
     */
    public void initActionBar() {
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayUseLogoEnabled(false);
            bar.setHomeButtonEnabled(false);
            bar.setDisplayShowTitleEnabled(false);
            bar.setDisplayShowHomeEnabled(false);
            bar.setDisplayShowCustomEnabled(true);
            mActionBarView = new ActionBarView(this);
            mActionBarView.setTitleColor(getResources().getColor(R.color.activity_header_text_color));
            mActionBarView.setBackgroundResource(R.drawable.actionbar_view_line);
            bar.setCustomView(mActionBarView, new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
    }

    /**
     * set title text
     *
     * @param title
     */
    public void setTitle2(CharSequence title) {
        if (mActionBarView != null) {
            mActionBarView.setTitle(title);
        }
    }

    public ActionBarView getActionBarView() {
        return mActionBarView;
    }
}
