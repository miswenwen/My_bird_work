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
package com.yunos.yundroid.widget.itemview;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.item.HeaderText2Item;
import com.yunos.yundroid.widget.item.Item;

public class HeaderText2ItemView extends LinearLayout implements ItemView {

    private TextView mHeaderView;
    private TextView mTextView;
    private TextView mSubTitle;
    private HeaderText2Item mHeaderText2Item;

    private Class<?> mItemViewType;

    @Override
    public void setViewType(Class<?> type) {
        mItemViewType = type;
    }

    @Override
    public Class<?> getViewType() {
        return mItemViewType;
    }

    public HeaderText2ItemView(Context context) {
        this(context, null);
    }

    public HeaderText2ItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void prepareItemView() {
        mHeaderView = (TextView) findViewById(R.id.gd_separator_text);
        mTextView = (TextView) findViewById(R.id.gd_text);
        mSubTitle = (TextView) findViewById(R.id.gd_subtext);
    }

    @Override
    public void setObject(Item object) {
        mHeaderText2Item = (HeaderText2Item) object;
        setHeaderTextView(mHeaderText2Item.mHeaderText);
        setTextView(mHeaderText2Item.mText);
        setSubtextView(mHeaderText2Item.subtext);
    }

    @Override
    public void setTextView(String text) {
        mTextView.setText(text);
        if (mHeaderText2Item != null) {
            mHeaderText2Item.mText = text;
        }
    }

    @Override
    public void setSubtextView(String text) {
        mSubTitle.setText(text);
        if (mHeaderText2Item != null) {
            mHeaderText2Item.subtext = text;
        }
    }

    @Override
    public void setHeaderTextView(String text) {
        if (!TextUtils.isEmpty(text)) {
            mHeaderView.setText(text);
            mHeaderView.setVisibility(View.VISIBLE);
        } else {
            mHeaderView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setIcon(ImageView view) {
    }

    @Override
    public void setIcon(String text, int textColor) {
    }

    @Override
    public void setRBIconIndicator(ImageView ind) {
    }

    @Override
    public void setCheckBox(boolean status) {
    }

    @Override
    public void removeIcon() {
    }

    @Override
    public ImageView getIcon() {
        return null;
    }

    @Override
    public void setCheckBox() {
    }

    @Override
    public void setCustomView(int viewId) {
    }

    @Override
    public View getCustomView() {
        return null;
    }

    @Override
    public void setDividerVisible(boolean visible) {
    }

    @Override
    public void showIcon() {
    }

    @Override
    public void setTextViewColor(int color) {
        if (mTextView != null) {
            mTextView.setTextColor(color);
        }
    }

    @Override
    public void setTextViewDefaultAppearance(int resId) {
        if (mTextView != null) {
            mTextView.setTextAppearance(mContext, resId);
        }
    }
}
