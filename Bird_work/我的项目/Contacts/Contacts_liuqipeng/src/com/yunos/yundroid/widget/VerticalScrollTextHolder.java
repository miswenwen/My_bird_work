
package com.yunos.yundroid.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.HorizontalScrollTextHolder.OnItemClickListener;
import com.yunos.yundroid.widget.HorizontalScrollTextHolder.OnTouchInputListener;

class VerticalScrollTextHolder extends LinearLayout {
    private Context mContext;
    private HorizontalScrollTextHolder.OnItemClickListener mOnItemClickListener;
    private String[] mItemData;
    private int[] mPrePosition;
    private int mTop;
    private HorizontalScrollTextHolder.OnTouchInputListener mOnTouchInputListener;
    private int mIndexCharMarginBottom;
    private int mIndexTextHeight;
    private int mFilterDataScrollPadding;
    private float mFisheyeTextSize;
    private int mFisheyeMaxHeight;
    private int mVerticalWidth;
    private int mFisheyeChildItemWidth;
    private int mFisheyeChildItemHeight;

    private ScrollView mVerticalScroll;

    private TextView mIndexText;
    private LinearLayout mScrollLinearLayout;

    public VerticalScrollTextHolder(Context context) {
        super(context);
        mContext = context;
        initDimension();
    }

    public VerticalScrollTextHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initDimension();
    }

    public VerticalScrollTextHolder(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initDimension();
    }

    private void initDimension() {
        mIndexCharMarginBottom = getResources().getDimensionPixelSize(
                R.dimen.fisheye_index_char_margin_bottom);
        mIndexTextHeight = getResources().getDimensionPixelSize(R.dimen.fisheye_index_char_height);
        mFilterDataScrollPadding = getResources().getDimensionPixelSize(
                R.dimen.fisheye_filter_data_padding);
        mFisheyeTextSize = getResources().getDimension(R.dimen.fisheye_child_text_size);
        mFisheyeMaxHeight = getResources().getDimensionPixelSize(R.dimen.fisheye_max_height);
        mVerticalWidth = getResources().getDimensionPixelSize(R.dimen.fisheye_index_char_width);
        mFisheyeChildItemWidth = getResources().getDimensionPixelSize(
                R.dimen.fisheye_child_item_width);
        mFisheyeChildItemHeight = getResources().getDimensionPixelSize(
                R.dimen.fisheye_child_item_height);
    }

    public void setIndexText(String text) {
        mIndexText.setText(text);
        mIndexText.setVisibility(View.VISIBLE);
    }

    public void refresh(int centerCoordinate) {
        init();
        if (mItemData == null) {
            LayoutParams indxLp = (LayoutParams) mIndexText.getLayoutParams();
            int topMargin = centerCoordinate - mIndexTextHeight / 2;
            if (topMargin + mIndexTextHeight > getHeight()) {
                topMargin = getHeight() - mIndexTextHeight;
            } else if (topMargin < 0) {
                topMargin = 0;
            }
            indxLp.bottomMargin = 0;
            indxLp.topMargin = topMargin;
            mIndexText.setLayoutParams(indxLp);
            mVerticalScroll.setVisibility(View.GONE);
            return;
        }

        for (int i = 0; i < mItemData.length; i++) {
            TextView textView = new TextView(mContext);
            textView.setTag(i);
            textView.setClickable(true);
            textView.setText(mItemData[i]);
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFisheyeTextSize);
            textView.setTextColor(getResources().getColorStateList(R.color.fisheye_child_text_color));
            textView.setBackgroundResource(R.drawable.fisheye_child_data_item_bg);
            textView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    OnItemClicked(v);
                }
            });
            LayoutParams txLP = new LayoutParams(mFisheyeChildItemWidth, mFisheyeChildItemHeight);
            textView.setLayoutParams(txLP);
            mScrollLinearLayout.addView(textView);
        }
        mVerticalScroll.scrollTo(0, 0);
        mVerticalScroll.setVisibility(View.VISIBLE);
        updatePosition(centerCoordinate);
    }

    public void setItemData(String[] data) {
        mItemData = data;
    }

    public void setPostion(int[] position) {
        mPrePosition = position;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    public void clear() {
        setVisibility(View.GONE);
        if (mScrollLinearLayout != null) {
            mScrollLinearLayout.removeAllViews();
        }
    }

    private void init() {
        setOrientation(VERTICAL);
        setVisibility(View.VISIBLE);
        if (mIndexText == null) {
            mIndexText = new TextView(mContext);
            mIndexText.setBackgroundResource(R.drawable.fisheye_index_char_bg);
            mIndexText.getBackground().setColorFilter(getResources().getColor(R.color.fish_eye_bg_color), android.graphics.PorterDuff.Mode.SRC_ATOP);
            mIndexText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFisheyeTextSize);
            mIndexText.setTextColor(getResources().getColor(R.color.fisheye_index_text_color));
            mIndexText.setVisibility(View.INVISIBLE);
            mIndexText.setGravity(Gravity.CENTER);
            mIndexText.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    hasInput(event);
                    return true;
                }
            });

            LayoutParams txtLp = new LayoutParams(mVerticalWidth, mIndexTextHeight);
            txtLp.bottomMargin = mIndexCharMarginBottom;
            mIndexText.setLayoutParams(txtLp);
            // mIndexText.measure(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            // mIndexTextHeight = mIndexText.getMeasuredHeight();
            addView(mIndexText);
        }
        if (mVerticalScroll == null) {
            mVerticalScroll = new ScrollView(mContext);
            mVerticalScroll.setBackgroundResource(R.drawable.fisheye_textholder_vertical);
            mVerticalScroll.getBackground().setColorFilter(getResources().getColor(R.color.fish_eye_bg_color), android.graphics.PorterDuff.Mode.SRC_ATOP);
            mVerticalScroll.setHorizontalScrollBarEnabled(false);
            mVerticalScroll.setVerticalScrollBarEnabled(false);
            mVerticalScroll.setOverScrollMode(OVER_SCROLL_NEVER);
            LayoutParams scLp = new LayoutParams(mVerticalWidth, LayoutParams.WRAP_CONTENT);
            mVerticalScroll.setLayoutParams(scLp);
            mVerticalScroll.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    hasInput(event);
                    return false;
                }
            });
            addView(mVerticalScroll);
        }
        if (mScrollLinearLayout == null) {
            mScrollLinearLayout = new LinearLayout(mContext);
            mScrollLinearLayout.setOrientation(LinearLayout.VERTICAL);
            mScrollLinearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            mScrollLinearLayout.setPadding(mFilterDataScrollPadding, mFilterDataScrollPadding,
                    mFilterDataScrollPadding, mFilterDataScrollPadding);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            mVerticalScroll.addView(mScrollLinearLayout, lp);
        }
    }

    private void updatePosition(int centerCoordinate) {
        // calculate scroll position
        int barHeight = getHeight();
        int scrollHeight = mFilterDataScrollPadding * 2 + mItemData.length
                * mFisheyeChildItemHeight;
        if (scrollHeight > mFisheyeMaxHeight) {
            scrollHeight = mFisheyeMaxHeight;
        }
        int top = centerCoordinate - scrollHeight / 2;
        int bottom = centerCoordinate + scrollHeight / 2;
        if (top < 0) {
            top = 0;
            bottom = scrollHeight;
        } else if (bottom > barHeight) {
            top = barHeight - scrollHeight;
            bottom = barHeight;
        }
        // index text position
        int idxMarginTop = 0;
        LayoutParams indxLp = (LayoutParams) mIndexText.getLayoutParams();
        if (top > mIndexCharMarginBottom + mIndexTextHeight) {
            idxMarginTop = top - mIndexCharMarginBottom - mIndexTextHeight;
        } else {
            top = mIndexCharMarginBottom + mIndexTextHeight;
            bottom = scrollHeight;
        }
        indxLp.topMargin = idxMarginTop;
        indxLp.bottomMargin = mIndexCharMarginBottom;
        mIndexText.setLayoutParams(indxLp);

        // set scrollview height && scrollview layout height
        LayoutParams scrollLp = (LinearLayout.LayoutParams) mVerticalScroll.getLayoutParams();
        scrollLp.height = scrollHeight;
        mVerticalScroll.setLayoutParams(scrollLp);

        mTop = top;
        mBottom = bottom;
    }

    private void OnItemClicked(View v) {
        int i = (Integer) v.getTag();
        int top = v.getTop();
        int bottom = v.getBottom();
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClicked(genNewPosition(mPrePosition, i), mTop
                    + mIndexCharMarginBottom + ((top + bottom) / 2 - mVerticalScroll.getScrollY()));
        }
    }

    private int[] genNewPosition(int[] oldPosition, int pos) {
        if (oldPosition == null) {
            return new int[] {
                pos
            };
        }

        int[] newPosition = new int[oldPosition.length + 1];
        System.arraycopy(oldPosition, 0, newPosition, 0, oldPosition.length);
        newPosition[oldPosition.length] = pos;
        return newPosition;
    }

    private void hasInput(MotionEvent ev) {
        if (mOnTouchInputListener != null) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                mOnTouchInputListener.setHasInputFlag(true);
            } else if (action == MotionEvent.ACTION_UP) {
                mOnTouchInputListener.setHasInputFlag(false);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        hasInput(ev);
        return super.onInterceptTouchEvent(ev);
    }

    public void setOnTouchInputListener(OnTouchInputListener l) {
        mOnTouchInputListener = l;
    }
}
