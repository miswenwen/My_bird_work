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

import android.view.View;
import android.widget.CheckBox;

import com.yunos.alicontacts.R;

import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import hwdroid.widget.FooterBar.FooterBarButton;
import hwdroid.widget.FooterBar.FooterBarType.OnFooterItemClick;

public class BaseActivity extends BaseFragmentActivity{

    protected final static int BUT1_ID = 0;
    protected final static int BUT2_ID = 1;

    protected FooterBarButton mFooterBarButton;
    private CheckBox mAllCheckBox;

    public interface OnAllCheckedListener {
        void onAllChecked(boolean checked);
    }

    public BaseActivity() {
        super();
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
                if (mAllCheckBox.getVisibility() != View.VISIBLE) {
                    return;
                }
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

    public CheckBox getAllCheckBox() {
        return mAllCheckBox;
    }

    public void setAllCheckBoxChecked(boolean isChecked) {
        if (mAllCheckBox != null) mAllCheckBox.setChecked(isChecked);
    }

    public void hideAllCheckBox() {
        if (mAllCheckBox != null) {
            mAllCheckBox.setVisibility(View.GONE);
        }
    }

    public void showAllCheckBox() {
        if (mAllCheckBox != null) {
            mAllCheckBox.setVisibility(View.VISIBLE);
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
            }});

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
            }});

        mFooterBarButton.addItem(BUT1_ID, btn1Str);
        mFooterBarButton.addItem(BUT2_ID, btn2Str);

        mFooterBarButton.updateItems();

        getFooterBarImpl().addView(mFooterBarButton);
        getFooterBarImpl().setVisibility(View.VISIBLE);
    }

    public void setFooterBarButtonEnable(int id, boolean enable) {
        if (mFooterBarButton != null) mFooterBarButton.setItemEnable(id, enable);
    }

    public boolean onHandleFooterBarItemClick(int id) {
        return false;
    }

}
