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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.item.Item;
import com.yunos.yundroid.widget.item.SubtextItem;

/**
 * View representation of the {@link SubtextItem}.
 *
 * @author Cyril Mottier
 */
public class SubtextItemView extends LinearLayout implements ItemView {

    protected TextView mTextView;
    protected TextView mSubtextView;

    private Class<?> mItemViewType;

    @Override
    public void setViewType(Class<?> type) {
        mItemViewType = type;
    }

    @Override
    public Class<?> getViewType() {
        return mItemViewType;
    }

    public SubtextItemView(Context context) {
        this(context, null);
    }

    public SubtextItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void prepareItemView() {
        mTextView = (TextView) findViewById(R.id.gd_text);
        mSubtextView = (TextView) findViewById(R.id.gd_subtext);
    }

    @Override
    public void setObject(Item object) {
        final SubtextItem item = (SubtextItem) object;
        mTextView.setText(item.mText);
        mSubtextView.setText(item.subtext);
    }

    @Override
    public void setTextView(String text) {
        mTextView.setText(text);
    }

    @Override
    public void setSubtextView(String text) {
        mSubtextView.setText(text);
    }

    @Override
    public void setHeaderTextView(String text) {
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
