package com.android.deskclock;

import hwdroid.widget.FooterBar.FooterBar;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.deskclock.R;

public class BaseMainViewHost extends LinearLayout {

    private FrameLayout mContentView;
    private FooterBar mFooterBar;

    public BaseMainViewHost(Context context) {
        this(context, null);
    }

    public BaseMainViewHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContentView = (FrameLayout) findViewById(R.id.hw_action_bar_content_view);
        if (mContentView == null || !(mContentView instanceof FrameLayout)) {
            throw new IllegalArgumentException("No FrameLayout with the id R.id.hw_action_bar_content_view found in the layout.");
        }

        mFooterBar = (FooterBar) findViewById(R.id.hw_footer_bar);
        if (mFooterBar == null || !(mFooterBar instanceof FooterBar)) {
            throw new IllegalArgumentException("No FooterBar with the id R.id.hw_footer_bar found in the layout.");
        }

    }

    public FrameLayout getContentView() {
        return mContentView;
    }

    public FooterBar getFooterBarImpl() {
        return mFooterBar;
    }
}
