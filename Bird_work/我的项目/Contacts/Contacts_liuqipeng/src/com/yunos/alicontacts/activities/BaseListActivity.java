/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yunos.alicontacts.activities;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.yunos.alicontacts.R;

import com.aliyun.ams.systembar.SystemBarColorManager;
import hwdroid.app.HWListActivity;
import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import hwdroid.widget.FooterBar.FooterBarButton;
import hwdroid.widget.FooterBar.FooterBarType.OnFooterItemClick;
public class BaseListActivity extends HWListActivity {

    protected final static int BUT1_ID = 0;
    protected final static int BUT2_ID = 1;

    protected FooterBarButton mFooterBarButton;
    private CheckBox mAllCheckBox;

    public interface OnAllCheckedListener {
        void onAllChecked(boolean checked);
    }

    public BaseListActivity() {
        super();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBarView bar = getActionBarView();
        if(bar != null){
            bar.setTitleColor(getResources().getColor(R.color.activity_header_text_color));
            bar.setBackgroundResource(R.drawable.actionbar_view_line);
        }

        setSystemBar(R.color.title_color);
    }

    @Override
    public int createLayout() {
        int resID = super.createLayout();
        return resID;
    }

    @Override
    public void showBackKey(boolean show) {
        ActionBarView bar = getActionBarView();
        if (bar == null)
            return;

        ImageView view = new ImageView(this);
        view.setImageResource(R.drawable.actionbar_back_selector);
        bar.addLeftItem(view);
        bar.setOnLeftWidgetItemClickListener(new OnLeftWidgetItemClick() {

            @Override
            public void onLeftWidgetItemClick() {
                onBackKey();
            }
        });
    }
    protected void setSystemBar(int color) {
        setSystemBar(getResources().getColor(color), getActionBarView() == null ? false : true);
    }

    protected void setSystemBar(int color, boolean hasActionbar) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, hasActionbar);
        systemBarManager.setStatusBarColor(color);
        systemBarManager.setStatusBarDarkMode(this, getResources().getBoolean(R.bool.contact_dark_mode));
    }


    public void showAllCheckBox(final OnAllCheckedListener listener) {
        if (mAllCheckBox == null) {
            mAllCheckBox = new CheckBox(this);
            mAllCheckBox.setClickable(false);
            mAllCheckBox.setButtonDrawable(R.drawable.header_btn_checkbox);
        }

        this.setRightWidgetClickListener(new OnRightWidgetItemClick() {

            @Override
            public void onRightWidgetItemClick() {
                boolean checked = !mAllCheckBox.isChecked();
                mAllCheckBox.setChecked(checked);
                if (listener != null) {
                    listener.onAllChecked(checked);
                }
            }
        });

        mAllCheckBox.setVisibility(View.VISIBLE);
        this.setRightWidgetView(mAllCheckBox);
    }

    public void setAllCheckBoxChecked(boolean isChecked) {
        if (mAllCheckBox != null) {
            mAllCheckBox.setChecked(isChecked);
        }
    }

    public void hideAllCheckBox() {
        if (mAllCheckBox != null) {
            mAllCheckBox.setVisibility(View.GONE);
        }
    }

    public void setAllCheckBoxEnabled(boolean enabled) {
        if (mAllCheckBox != null) {
            mAllCheckBox.setEnabled(enabled);
        }
    }

    public void setFooterBarButton(String btn1Str) {
        mFooterBarButton = new FooterBarButton(this);
        mFooterBarButton.setOnFooterItemClick(new OnFooterItemClick() {
            @Override
            public void onFooterItemClick(View view, int id) {

            }
        });

        mFooterBarButton.addItem(BUT1_ID, btn1Str);

        mFooterBarButton.updateItems();

        getFooterBarImpl().addView(mFooterBarButton);
        getFooterBarImpl().setVisibility(View.VISIBLE);
    }

    public void setFooterBarButton(String btn1Str, String btn2Str) {
        mFooterBarButton = new FooterBarButton(this);
        mFooterBarButton.setOnFooterItemClick(new OnFooterItemClick() {
            @Override
            public void onFooterItemClick(View view, int id) {
                onHandleFooterBarItemClick(id);
            }
        });

        mFooterBarButton.addItem(BUT1_ID, btn1Str);
        mFooterBarButton.addItem(BUT2_ID, btn2Str);

        mFooterBarButton.updateItems();

        getFooterBarImpl().addView(mFooterBarButton);
        getFooterBarImpl().setVisibility(View.VISIBLE);
    }

    public void setFooterBarButtonEnable(int id, boolean enable) {
        mFooterBarButton.setItemEnable(id, enable);
    }

    public boolean onHandleFooterBarItemClick(int id) {
        return false;
    }

}
