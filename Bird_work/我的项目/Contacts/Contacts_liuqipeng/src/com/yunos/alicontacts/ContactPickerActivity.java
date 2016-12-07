
package com.yunos.alicontacts;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.yunos.alicontacts.widget.FullHeightLinearLayout;

import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;

public class ContactPickerActivity extends ContactsActivity {

    protected FullHeightLinearLayout mRootLyaout;
    protected View mActionBarView;
    private ImageView mLeft;
    private TextView mTitle;
    private FrameLayout mRightItem;
    private OnAllCheckedListener mAllCheckedListener;
    private OnRightWidgetItemClick mRightListener;

    @Override
    public void setRightWidgetClickListener(OnRightWidgetItemClick click) {
        mRightListener = click;
    }

    @Override
    public void showBackKey(boolean show) {
        if (mLeft != null) {
            mLeft.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setRightWidgetView(View v) {
        if (mRightItem != null) {
            mRightItem.removeAllViews();
            mRightItem.addView(getAllCheckBox());
        }
    }

    @Override
    public void showAllCheckBox(OnAllCheckedListener listener) {
        super.showAllCheckBox(null);
        mAllCheckedListener = listener;
        if (mRightItem != null) {
            mRightItem.removeAllViews();
            mRightItem.addView(getAllCheckBox());
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (mTitle != null) {
            mTitle.setText(title);
        }
    }

    @Override
    public void setTitle2(CharSequence title) {
        if (mTitle != null) {
            mTitle.setText(title);
        }
    }

    @Override
    public View getActionBarView() {
        return mActionBarView;
    }

    @Override
    public void setActivityContentView(int resID) {
        super.setActivityContentView(resID);
        mRootLyaout = (FullHeightLinearLayout) findViewById(R.id.root_layout);
        mActionBarView = LayoutInflater.from(this).inflate(R.layout.actionbar_header_view, null, false);
        //View divider = new View(this);
        //divider.setBackgroundColor(getResources().getColor(R.color.header_line_color));
        //ViewGroup.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
        //divider.setLayoutParams(params);
        //mRootLyaout.addView(divider, 0);
        mRootLyaout.addView(mActionBarView, 0);
        mLeft = (ImageView) mActionBarView.findViewById(R.id.left_item);
        mLeft.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackKey();
            }
        });
        mTitle = (TextView) findViewById(R.id.title);
        mRightItem = (FrameLayout) findViewById(R.id.right_item);
        mRightItem.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mAllCheckedListener != null) {
                    if (getAllCheckBox() != null && getAllCheckBox().getVisibility() != View.VISIBLE) {
                        return;
                    }
                    boolean checked = !getAllCheckBox().isChecked();
                    getAllCheckBox().setChecked(checked);
                    if (mAllCheckedListener != null) {
                        mAllCheckedListener.onAllChecked(checked);
                    }
                } else {
                    if (mRightListener != null) {
                        mRightListener.onRightWidgetItemClick();
                    }
                }
            }
        });
    }
}
