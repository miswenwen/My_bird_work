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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.item.HeaderIconTextItem;
import com.yunos.yundroid.widget.item.Item;

public class HeaderIconTextItemView extends LinearLayout implements ItemView {

    Context mContext;
    protected TextView mHeaderView;
    protected TextView mTextView;
    protected FrameLayout mIcon;
    boolean mIconRemoved;
    private View mDivider;
    private float mPortraitTextSize;
    private int mPortraitTextColor;
    private ImageView mRBIndicator;

    private Class<?> mItemViewType;

    @Override
    public void setViewType(Class<?> type) {
        mItemViewType = type;
    }

    @Override
    public Class<?> getViewType() {
        return mItemViewType;
    }

    public HeaderIconTextItemView(Context context) {
        this(context, null);
    }

    public HeaderIconTextItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public void prepareItemView() {
        mTextView = (TextView) findViewById(R.id.gd_text);
        mHeaderView = (TextView) findViewById(R.id.gd_separator_text);
        View gdIcon = findViewById(R.id.gd_icon);
        if (gdIcon instanceof ViewStub) {
            mIcon = (FrameLayout) ((ViewStub) gdIcon).inflate();
        } else {
            mIcon = (FrameLayout) gdIcon;
        }
        mHeaderView = (TextView) findViewById(R.id.gd_separator_text);
        mDivider = findViewById(R.id.divider_id);
    }

    @Override
    public void setObject(Item object) {
        final HeaderIconTextItem item = (HeaderIconTextItem) object;
        setHeaderTextView(item.mHeaderText);
        setTextView(item.mText);
        setIcon(item.mIcon);
    }

    @Override
    public void setHeaderTextView(String text) {
        if (mHeaderView == null)
            return;

        if (!TextUtils.isEmpty(text)) {
            mHeaderView.setText(text);
            mHeaderView.setVisibility(View.VISIBLE);
            mDivider.setVisibility(View.VISIBLE);
        } else {
            mHeaderView.setVisibility(View.GONE);
            mDivider.setVisibility(View.GONE);
        }
    }

    @Override
    public void setTextView(String text) {
        if (mTextView == null)
            return;
        mTextView.setText(text);
    }

    @Override
    public void setSubtextView(String text) {
    }

    @Override
    public void setIcon(ImageView view) {
        if (mIcon == null) {
            return;
        }
        mIcon.removeAllViews();
        mIcon.addView(view);
        ImageView v = new ImageView(mContext);
        v.setImageResource(R.drawable.contact_list_avatar_border_selector);
        mIcon.addView(v);
        if (mRBIndicator != null) {
            mIcon.addView(mRBIndicator);
        }
    }

    @Override
    public void setIcon(String text, int backtround) {
        if (mIcon == null) {
            return;
        }
        if (mPortraitTextSize == 0) {
            mPortraitTextSize = mContext.getResources().getDimensionPixelSize(R.dimen.portrait_text_size_small);
        }
        if (mPortraitTextColor == 0) {
            mPortraitTextColor = mContext.getResources().getColor(R.color.aui_primary_txt_color_white);
        }
        mIcon.removeAllViews();
        TextView textView = new TextView(mContext);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(mPortraitTextColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPortraitTextSize);
        textView.setText(text);
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.portrait_background, null);
        drawable.setColorFilter(mContext.getResources().getColor(backtround), PorterDuff.Mode.SRC_ATOP);
        textView.setBackground(drawable);
        mIcon.addView(textView);
        if (mRBIndicator != null) {
            mIcon.addView(mRBIndicator);
        }
    }

    @Override
    public void setRBIconIndicator(ImageView ind) {
        if (mIcon == null) {
            return;
        }
        if (mRBIndicator != null) {
            mIcon.removeView(mRBIndicator);
        }
        mRBIndicator = ind;
        if (mRBIndicator == null) {
            return;
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.BOTTOM);
        mRBIndicator.setLayoutParams(params);
        mIcon.addView(mRBIndicator);
    }

    @Override
    public void removeIcon() {
        if (!mIconRemoved && mIcon != null) {
            mIconRemoved = true;
            /*
             * mIcon.removeAllViews(); android.view.ViewGroup.LayoutParams
             * params = mIcon.getLayoutParams(); params.width = 0; params.height
             * = 0; mIcon.setLayoutParams(params);
             */
            mIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public void showIcon() {
        if (mIconRemoved && mIcon != null) {
            mIconRemoved = false;
            /*
             * android.view.ViewGroup.LayoutParams params =
             * mIcon.getLayoutParams(); params.width = mIconWidth; params.height
             * = mIconHeight; mIcon.setLayoutParams(params);
             */
            mIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public ImageView getIcon() {
        return null;
    }

    @Override
    public void setCheckBox(boolean status) {
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
