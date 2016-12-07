
package com.yunos.alicontacts.dialpad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;

public class IconTextButton extends LinearLayout {
    private static final String TAG = "IconTextButton";

    private ImageView mIconView;
    private TextView mTextView;

    public IconTextButton(Context context) {
        super(context);
        initViews();
    }

    public IconTextButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        initViews();
    }

    public IconTextButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    private void initViews() {
        LayoutInflater.from(this.getContext()).inflate(R.layout.icon_text_btn_layout, this, true);
        mIconView = (ImageView) this.findViewById(R.id.icon);
        mTextView = (TextView) this.findViewById(R.id.text);
    }

    public void setIconImage(int resId) {
        mIconView.setImageResource(resId);
    }

    public void setText(String text) {
        mTextView.setText(text);
    }
}
