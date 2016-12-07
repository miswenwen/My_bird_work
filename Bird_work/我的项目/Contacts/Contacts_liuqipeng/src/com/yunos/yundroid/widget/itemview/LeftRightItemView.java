/*
 * Copyright (C) 2013 YunOS (http://www.yunos.com)
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
import com.yunos.yundroid.widget.item.Item;
import com.yunos.yundroid.widget.item.LeftRightItem;

public class LeftRightItemView extends LinearLayout implements ItemView {

    protected TextView mLeftTextView;
    protected TextView mRightTextView;

    private Class<?> mItemViewType;

    @Override
    public void setViewType(Class<?> type) {
        mItemViewType = type;
    }

    @Override
    public Class<?> getViewType() {
        return mItemViewType;
    }

    public LeftRightItemView(Context context) {
        this(context, null);
    }

    public LeftRightItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LeftRightItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void prepareItemView() {
        mLeftTextView = (TextView) findViewById(R.id.gd_left_text);
        mRightTextView = (TextView) findViewById(R.id.gd_right_text);
    }

    @Override
    public void setObject(Item object) {
        final LeftRightItem item = (LeftRightItem) object;
        setTextView(item.mLeftText);
        setSubtextView(item.mRightText);
    }

    public void destroy() {
        mLeftTextView = null;
        mRightTextView = null;
    }

    @Override
    public void setTextView(String text) {
        mLeftTextView.setText(text);
    }

    @Override
    public void setSubtextView(String text) {
        mRightTextView.setText(text);
        if(TextUtils.isEmpty(text)){
            mRightTextView.setVisibility(View.GONE);
        }else{
            mRightTextView.setVisibility(View.VISIBLE);
        }
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
        if (mLeftTextView != null) {
            mLeftTextView.setTextColor(color);
        }
    }

    @Override
    public void setTextViewDefaultAppearance(int resId) {
        if (mLeftTextView != null) {
            mLeftTextView.setTextAppearance(mContext, resId);
        }
    }

}
