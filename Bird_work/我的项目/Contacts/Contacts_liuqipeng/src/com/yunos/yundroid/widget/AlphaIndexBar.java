
package com.yunos.yundroid.widget;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.AlphaIndexButton.OnTouchAlphaIndexListener;
import com.yunos.yundroid.widget.FishEyeView.FishEyeOrientation;

import yunos.support.v4.view.ViewPager;

public class AlphaIndexBar extends RelativeLayout {
    private Context mContext;

    private int mCurrentFocus = -1;
    private int mItemCount;
    private boolean[] mEnableMasks;
    private int mBarHeight = 30;
    private int mBarWidth = LayoutParams.MATCH_PARENT;

    private OnItemClickListener mOnItemClickListener;
    private AlphaIndexBarAdapter mAdapter;
    private OnTouchInputListener mOnTouchInputListener;

    private String[] mAlphaIndex;
    private Handler mHandler = new Handler();
    private boolean mHasDetectStarted;
    private boolean mMoveDone;
    private int mCurrentCenter;

    private AlphaIndexButton mAlphaIndexButton;

    public static interface OnItemClickListener {
        void onItemClicked(int[] position, int centerX);
    }

    public static interface OnTouchInputListener {
        void setHasInputFlag(boolean hasInput);

        void onChildFocusChange(int fromPos, int toPos);
    }

    public static interface AlphaIndexBarAdapter {
        void onAlphaIndexBarCreate(AlphaIndexBar alphaBar);
    }

    public AlphaIndexBar(Context context) {
        super(context);
        mContext = context;
    }

    public AlphaIndexBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public AlphaIndexBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    public void setAdapter(AlphaIndexBarAdapter adapter) {
        mAdapter = adapter;
        create();
    }

    public void setAlphaBarSize(int width, int height) {
        mBarWidth = width;
        mBarHeight = height;
    }

    public void setOnTouchInputListener(OnTouchInputListener l) {
        mOnTouchInputListener = l;
    }

    public void setAlphaIndex(String[] index) {
        mAlphaIndex = index;
        mItemCount = mAlphaIndex.length == 0 ? 1 : mAlphaIndex.length;
        if (this.mAlphaIndexButton != null) {
            mAlphaIndexButton.setAlphaIndex(index);
        }
    }

    private void create() {
        mAlphaIndexButton = new AlphaIndexButton(mContext);
        mAdapter.onAlphaIndexBarCreate(this);
        LayoutParams barLayoutParams = new LayoutParams(mBarWidth, mBarHeight);
        int rule = FishEyeView.orientation == FishEyeOrientation.Vertical ? ALIGN_PARENT_RIGHT
                : ALIGN_PARENT_BOTTOM;
        barLayoutParams.addRule(rule);
        addView(mAlphaIndexButton, barLayoutParams);
        mItemCount = mAlphaIndex.length;
        mAlphaIndexButton.setClickable(true);
//        if (FishEyeView.orientation != FishEyeOrientation.Vertical) {
//            mAlphaIndexButton.setBackgroundResource(R.drawable.fisheye_alpha_bar_horizontal_bg);
//        }
        mAlphaIndexButton.setOnTouchAlphaIndexListener(new OnTouchAlphaIndexListener() {

            @Override
            public void onTouchAlphaIndexDown(int index) {
                handleTouchDown(index);
            }

            @Override
            public void onTouchAlphaIndexUp(int index) {
                handleTouchUp(index);

            }

            @Override
            public void onTouchAlphaIndexMove(int index) {
                handleTouchMove(index);
            }

        });
    }

    public void clear() {
        mCurrentFocus = -1;
    }

    protected void handleTouchMove(int pos) {
        if (mCurrentFocus != pos) {
            if (mOnTouchInputListener != null) {
                mOnTouchInputListener.onChildFocusChange(mCurrentFocus, pos);
            }
            mCurrentFocus = pos;
            updateTextView();
        }
        startDetected();
        if (mOnTouchInputListener != null) {
            mOnTouchInputListener.setHasInputFlag(true);
        }
    }

    protected void handleTouchUp(int pos) {
        if (mOnTouchInputListener != null) {
            mOnTouchInputListener.setHasInputFlag(false);
            // ZH: free PeopleActivity2 ViewPager for handling touch event
            if (mParentViewPager != null) {
                mParentViewPager.requestDisallowInterceptTouchEvent(false);
            }
        }
    }

    protected void handleTouchDown(int pos) {
        if (mCurrentFocus != pos) {
            if (mOnTouchInputListener != null) {
                mOnTouchInputListener.onChildFocusChange(mCurrentFocus, pos);
            }

            mCurrentFocus = pos;
            updateTextView();
            if (mOnTouchInputListener != null) {
                mOnTouchInputListener.setHasInputFlag(true);
            }

            // ZH: Stop PeopleActivity2 ViewPager for handling touch event
            if (mParentViewPager != null) {
                mParentViewPager.requestDisallowInterceptTouchEvent(true);
            }
        }
        startDetected();
    }

    private void updateTextView() {
        if (mCurrentFocus >= 0 && mCurrentFocus < mItemCount) {
            if (!isEnableMask(mCurrentFocus)) {
                return;
            }
            layoutTextView(mCurrentFocus);
        }
    }

    private void layoutTextView(int pos) {
        int barLength = 0;
        int itemLength = 0;
        // int top = 0;
        // int left = 0;
        // int right = 0;

        if (FishEyeView.orientation == FishEyeOrientation.Vertical) {
            barLength = mAlphaIndexButton.getHeight();
            itemLength = barLength / (mItemCount + 1);
        } else {
            barLength = mAlphaIndexButton.getWidth();
            itemLength = barLength / (mItemCount + 1);
        }
        mCurrentCenter = (mCurrentFocus + 1) * itemLength;

        /*
         * NOTE: Because some MTK modifications in View, the mTextView.layout()
         * cannot position the view at expected position. From MTK response
         * (BugFree 96301), we have to set layout parameters to make the
         * mTextView to be displayed at correct position.
         */
//        if (FishEyeView.orientation == FishEyeOrientation.Horizon) {
//            LayoutParams st = (RelativeLayout.LayoutParams) mTextView.getLayoutParams();
//            st.setMargins(left, top, 0, 0);
//            mTextView.setLayoutParams(st);
//            // mTextView.layout(left, top, right, bottom);
//        }
    }

    private void notifyListener(int pos) {
        if (pos < 0 || pos >= mItemCount) {
            return;
        }

        if (isEnableMask(pos) && mOnItemClickListener != null) {
            int[] position = new int[1];
            position[0] = pos;
            mOnItemClickListener.onItemClicked(position, mCurrentCenter);
        }
    }

    private void startDetected() {
        if (!mHasDetectStarted) {
            mMoveDone = true;
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mHasDetectStarted = false;
                    if (mMoveDone) {
                        notifyListener(mCurrentFocus);
                    } else {
                        startDetected();
                    }
                }
            });
            mHasDetectStarted = true;
        }
    }

    private ViewPager mParentViewPager;

    public void setParentViewPager(ViewPager pager) {
        mParentViewPager = pager;
    }

    public void setEnableMask(boolean[] mask) {
        mEnableMasks = mask;
        mAlphaIndexButton.setEnableMask(mask);
        reDrawButton();
    }

    public boolean[] getEnableMask() {
        return mEnableMasks;
    }

    public boolean isEnableMask(int pos) {
        if (mEnableMasks != null && mEnableMasks[pos]) {
            return true;
        }
        return false;
    }
    public void reDrawButton(){
        mAlphaIndexButton.invalidate();
    }
}
