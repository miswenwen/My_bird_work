package com.yunos.yundroid.widget.itemview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.item.Item;
import com.yunos.yundroid.widget.item.TextItem;

public class SeparatorItemView2 extends LinearLayout implements ItemView {

    protected TextView mTextView;

    private Class<?> mItemViewType;

    @Override
    public void setViewType(Class<?> type) {
        mItemViewType = type;
    }

    @Override
    public Class<?> getViewType() {
        return mItemViewType;
    }

    public SeparatorItemView2(Context context) {
        this(context, null);
    }

    public SeparatorItemView2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeparatorItemView2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void prepareItemView() {
        mTextView = (TextView) findViewById(R.id.gd_text);
    }

    @Override
    public void setObject(Item object) {
        final TextItem item = (TextItem) object;
        setTextView(item.mText);
    }

    @Override
    public void setTextView(String text) {
        mTextView.setText(text);
    }

    @Override
    public void setSubtextView(String text) {
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
