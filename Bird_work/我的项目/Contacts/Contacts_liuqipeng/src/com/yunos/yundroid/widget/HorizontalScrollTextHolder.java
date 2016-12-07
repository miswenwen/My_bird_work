
package com.yunos.yundroid.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;

public class HorizontalScrollTextHolder extends LinearLayout {
    public static interface OnItemClickListener {
        void onItemClicked(int[] position, int centerX);
    }

    public static interface OnTouchInputListener {
        void setHasInputFlag(boolean hasInput);
    }

    private Context mContext;
    private OnItemClickListener mOnItemClickListener;
    private String[] mItemData;
    private int[] mPrePosition;
    private int mLeft;
    private OnTouchInputListener mOnTouchInputListener;

    private int mIndexCharMarginBottom;
    private int mIndexTextWidth;
    private int mFilterDataScrollPadding;
    private float mFisheyeTextSize;
    private int mHorizontalHeight;
    private int mScrollViewMaxWidth;
    private int mFisheyeChildItemWidth;
    private int mFisheyeChildItemHeight;

    private HorizontalScrollView mScrollView;
    private TextView mIndexCharTextView;

    public HorizontalScrollTextHolder(Context context) {
        super(context);
        mContext = context;
        initDimension();
    }

    public HorizontalScrollTextHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initDimension();
    }

    public HorizontalScrollTextHolder(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initDimension();
    }

    private void initDimension() {
        mIndexTextWidth = getResources().getDimensionPixelSize(R.dimen.fisheye_index_char_width);
        mFisheyeTextSize = getResources().getDimension(R.dimen.fisheye_child_text_size);
        mHorizontalHeight = getResources().getDimensionPixelSize(R.dimen.fisheye_index_char_height);
        mScrollViewMaxWidth = getResources().getDimensionPixelSize(R.dimen.fisheye_max_width);

        mFisheyeChildItemWidth = getResources().getDimensionPixelSize(
                R.dimen.fisheye_child_item_width);
        mFisheyeChildItemHeight = getResources().getDimensionPixelSize(
                R.dimen.fisheye_child_item_height);
        mFilterDataScrollPadding = getResources().getDimensionPixelSize(
                R.dimen.fisheye_filter_data_padding);
        mIndexCharMarginBottom = getResources().getDimensionPixelSize(
                R.dimen.fisheye_index_char_margin_bottom);
    }

    public void setIndexText(String text) {
        mIndexCharTextView.setText(text);
        mIndexCharTextView.setVisibility(View.VISIBLE);
    }

    public void refresh(int centerCoordinate) {
        init();
        if (mItemData == null) {
            LayoutParams indxLp = (LayoutParams) mIndexCharTextView.getLayoutParams();
            int leftMargin = centerCoordinate - mIndexTextWidth / 2;
            if (leftMargin + mIndexTextWidth > getWidth()) {
                leftMargin = getWidth() - mIndexTextWidth;
            } else if (leftMargin < 0) {
                leftMargin = 0;
            }
            indxLp.rightMargin = 0;
            indxLp.leftMargin = leftMargin;
            mIndexCharTextView.setLayoutParams(indxLp);
            mScrollView.setVisibility(View.GONE);
            return;
        }
        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);

        LayoutParams textViewLp = new LayoutParams(mFisheyeChildItemWidth, mFisheyeChildItemHeight);
        // textViewLp.leftMargin = mTextHolderPadding;
        // textViewLp.rightMargin = mTextHolderPadding;

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
            linearLayout.addView(textView, textViewLp);
        }
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        mScrollView.addView(linearLayout, lp);
        mScrollView.scrollTo(0, 0);
        mScrollView.setVisibility(View.VISIBLE);
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
        setVisibility(GONE);
        if (mScrollView != null) {
            mScrollView.removeAllViews();
        }
    }

    private void init() {
        setOrientation(VERTICAL);
        setVisibility(VISIBLE);
        if (mScrollView == null) {
            mScrollView = new HorizontalScrollView(mContext);
            mScrollView.setBackgroundResource(R.drawable.fisheye_textholder_vertical);
            mScrollView.setHorizontalScrollBarEnabled(false);
            mScrollView.setOverScrollMode(OVER_SCROLL_NEVER);
            mScrollView.setPadding(mFilterDataScrollPadding, mFilterDataScrollPadding,
                    mFilterDataScrollPadding, mFilterDataScrollPadding);
            LayoutParams scLp = new LayoutParams(mScrollViewMaxWidth, mHorizontalHeight);
            scLp.bottomMargin = mIndexCharMarginBottom;
            mScrollView.setLayoutParams(scLp);
            mScrollView.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    hasInput(event);
                    return false;
                }
            });
            addView(mScrollView);
        }
        if (mIndexCharTextView == null) {
            mIndexCharTextView = new TextView(mContext);
            mIndexCharTextView.setBackgroundResource(R.drawable.fisheye_index_char_bg);
            mIndexCharTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFisheyeTextSize);
            mIndexCharTextView.setTextColor(getResources().getColor(
                    R.color.fisheye_index_text_color));
            mIndexCharTextView.setGravity(Gravity.CENTER);
            LayoutParams txtLp = new LayoutParams(mHorizontalHeight, mIndexTextWidth);
            txtLp.bottomMargin = mIndexCharMarginBottom;
            mIndexCharTextView.setLayoutParams(txtLp);
            mIndexCharTextView.setVisibility(View.GONE);
            mIndexCharTextView.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    hasInput(event);
                    return true;
                }
            });
            addView(mIndexCharTextView);
        }
    }

    private void updatePosition(int centerCoordinate) {
        // update index char textview position
        int barWidth = getWidth();
        int indexLeft = centerCoordinate - mIndexTextWidth / 2;
        if (indexLeft + mIndexTextWidth > barWidth) {
            indexLeft = barWidth - mIndexTextWidth;
        }
        LayoutParams txtLp = (LayoutParams) mIndexCharTextView.getLayoutParams();
        txtLp.leftMargin = indexLeft;
        mIndexCharTextView.setLayoutParams(txtLp);

        // scroll margin to left && calculate scroll width
        int newScrollWidth = mFilterDataScrollPadding * 2 + mItemData.length
                * mFisheyeChildItemWidth;
        if (newScrollWidth > mScrollViewMaxWidth) {
            newScrollWidth = mScrollViewMaxWidth;
        }
        int left = centerCoordinate - newScrollWidth / 2;
        int right = centerCoordinate + newScrollWidth / 2;
        if (left < 0) {
            left = 0;
            right = newScrollWidth;
        } else if (right > barWidth) {
            right = barWidth;
            left = barWidth - newScrollWidth;
        }
        LayoutParams scLp = (LayoutParams) mScrollView.getLayoutParams();
        scLp.width = newScrollWidth;
        scLp.leftMargin = left;
        mScrollView.setLayoutParams(scLp);
        mLeft = left;
        mRight = right;
    }

    private void OnItemClicked(View v) {
        int i = (Integer) v.getTag();
        int left = v.getLeft();
        int right = v.getRight();
        if (mOnItemClickListener != null) {
            int scrollX = mScrollView.getScrollX();
            mOnItemClickListener.onItemClicked(genNewPosition(mPrePosition, i), mLeft
                    + mFilterDataScrollPadding + ((left + right) / 2 - scrollX));
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
