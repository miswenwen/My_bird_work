
package com.yunos.yundroid.widget;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.preference.ContactsSettingActivity;

import yunos.support.v4.view.ViewPager;

public class FishEyeView extends LinearLayout {
    private static final String TAG = "FishEyeView";

    public static FishEyeOrientation orientation = FishEyeOrientation.Horizon;

    private Context mContext;
    private FishEyeViewAdapter mAdapter;
    private OnChildClickListener mOnChildClickListener;

    private int mDepth = 1;
    private int mPreDeepth;

    private int mCurrentCenterCoordinate;
    private int mCurrentPosition = -1;

    private AlphaIndexBar mAlphaIndexBar;
    private View[] mTextHolder;
    private RelativeLayout[] mRelativeLayout;

    private boolean mIsTextHoldersAdded;
    private boolean mHasDetectStarted;
    private Handler mDetectHandler = new Handler();
    private String[] mAlphaIndex;
    
    private int mAlphaBarWidth;
    private int mAlphaBarHeight;

    public enum FishEyeOrientation {
        Horizon, Vertical
    }

    public static interface FishEyeViewAdapter {
        void onFishEyeViewCreate(FishEyeView fisheye);
    }

    public static interface OnChildClickListener {
        void onChildClick(int[] position);
        void onChildClear();
    }

    public FishEyeView(Context context) {
        super(context);
        mContext = context;
    }

    public FishEyeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public FishEyeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public void setDepth(int depth) {
        mDepth = depth;
    }

    public void setOrientation(FishEyeOrientation o) {
        orientation = o;
        initAlphaBar();
    }

    public void setOnChildClickListener(OnChildClickListener l) {
        mOnChildClickListener = l;
    }

    public void setAdapter(FishEyeViewAdapter adapter) {
        mAdapter = adapter;
        create();
    }

    public void updateAlphaEnableStates(boolean[] mask) {
        mAlphaIndexBar.setEnableMask(mask);
    }

    public void setAlphaIndex(String[] index) {
        mAlphaIndex = index;
        if(mAlphaIndexBar != null){
            mAlphaIndexBar.setAlphaIndex(mAlphaIndex);
        }
    }

    public void onNotifyDataDone(int result, int[] position, String[] data) {
        if (position.length < mDepth) {
            refreshFishBar(position, data);
        }
    }

    public void initOrientation() {
        boolean value = ContactsSettingActivity.readFishEyeOrientation(mContext);
        if (value) {
            orientation = FishEyeOrientation.Vertical;
        }
        initAlphaBar();
    }

    private void initAlphaBar() {
        if (orientation == FishEyeOrientation.Vertical) {
            mAlphaBarWidth = getResources().getDimensionPixelSize(R.dimen.fisheye_alpha_bar_width);
            mAlphaBarHeight = LayoutParams.MATCH_PARENT;
        } else {
            mAlphaBarWidth = LayoutParams.MATCH_PARENT;
            mAlphaBarHeight = getResources()
                    .getDimensionPixelSize(R.dimen.fisheye_alpha_bar_height);
        }
    }
    private void create() {
        mAdapter.onFishEyeViewCreate(this);
        if (mDepth > 1) {
            createAlphaBar();
        }
    }

    public boolean isTextHoldersAdded() {
        return mIsTextHoldersAdded;
    }

    public void createTextHolder() {
        if (mIsTextHoldersAdded) {
            Log.w(TAG, "createTextHolder: views has been inited. Return here.");
            return;
        }

        mTextHolder = new View[mDepth];
        mRelativeLayout = new RelativeLayout[mDepth];

        int paddingRight = getResources().getDimensionPixelSize(R.dimen.fisheye_filter_margin_right);
        for (int i = 1; i < mDepth; i++) {
            mRelativeLayout[i] = new RelativeLayout(mContext);
            if (FishEyeView.orientation == FishEyeOrientation.Vertical) {
                VerticalScrollTextHolder vScrollTextHodler = new VerticalScrollTextHolder(mContext);
                mTextHolder[i] = vScrollTextHodler;
                LayoutParams vLp = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
                vScrollTextHodler.setLayoutParams(vLp);
                vScrollTextHodler.setPadding(0, 0, paddingRight, 0);
                vScrollTextHodler
                        .setOnItemClickListener(new HorizontalScrollTextHolder.OnItemClickListener() {

                            @Override
                            public void onItemClicked(int[] position, int centerX) {
                                OnChildItemClicked(position, centerX);
                            }
                        });

                vScrollTextHodler
                        .setOnTouchInputListener(new HorizontalScrollTextHolder.OnTouchInputListener() {

                            @Override
                            public void setHasInputFlag(boolean hasInput) {
                                childInputDetect(hasInput);
                            }
                        });
            } else {
                HorizontalScrollTextHolder hScrollTextHolder = new HorizontalScrollTextHolder(
                        mContext);
                mTextHolder[i] = hScrollTextHolder;
                LayoutParams hLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                hScrollTextHolder.setLayoutParams(hLp);
                hScrollTextHolder
                        .setOnItemClickListener(new HorizontalScrollTextHolder.OnItemClickListener() {

                            @Override
                            public void onItemClicked(int[] position, int centerX) {
                                OnChildItemClicked(position, centerX);
                            }
                        });

                hScrollTextHolder
                        .setOnTouchInputListener(new HorizontalScrollTextHolder.OnTouchInputListener() {

                            @Override
                            public void setHasInputFlag(boolean hasInput) {
                                childInputDetect(hasInput);
                            }
                        });
            }
            mRelativeLayout[i].addView(mTextHolder[i]);
            addView(mRelativeLayout[i],0);
            mIsTextHoldersAdded = true;
        }
    }

    private void createAlphaBar() {
        mAlphaIndexBar = new AlphaIndexBar(mContext);
        mAlphaIndexBar.setParentViewPager(mParentViewPager);
        LayoutParams indexBarLP = new LayoutParams(mAlphaBarWidth, mAlphaBarHeight);
        int layoutOrien = VERTICAL;
        if (orientation == FishEyeOrientation.Vertical) {
            indexBarLP = new LayoutParams(mAlphaBarWidth, mAlphaBarHeight);
            //indexBarLP.leftMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            layoutOrien = HORIZONTAL;
        }
        setOrientation(layoutOrien);
        mAlphaIndexBar.setLayoutParams(indexBarLP);
        addView(mAlphaIndexBar);
        mAlphaIndexBar.setAdapter(new AlphaIndexBar.AlphaIndexBarAdapter() {
            @Override
            public void onAlphaIndexBarCreate(AlphaIndexBar alphaBar) {
                alphaBar.setAlphaBarSize(mAlphaBarWidth, mAlphaBarHeight);
                alphaBar.setAlphaIndex(mAlphaIndex);
            }
        });

        mAlphaIndexBar.setOnItemClickListener(new AlphaIndexBar.OnItemClickListener() {

            @Override
            public void onItemClicked(int[] position, int centerX) {
                OnChildItemClicked(position, centerX);
            }
        });

        mAlphaIndexBar.setOnTouchInputListener(new AlphaIndexBar.OnTouchInputListener() {

            @Override
            public void setHasInputFlag(boolean hasInput) {
                childInputDetect(hasInput);
            }

            @Override
            public void onChildFocusChange(int fromPos, int toPos) {
                mCurrentPosition = toPos;
                if(!mAlphaIndexBar.isEnableMask(toPos)){
                    reset();
                }
            }
        });
    }

    private void OnChildItemClicked(int[] position, int centerX) {
        mCurrentCenterCoordinate = centerX;
        if (mOnChildClickListener != null) {
            mOnChildClickListener.onChildClick(position);
        }
    }

    private void refreshFishBar(final int[] position, final String[] data) {
        if (mTextHolder == null || mRelativeLayout == null) {
            Log.w(TAG, "Fisheye is not ready. mTextHolder is null.");
            return;
        }
        HorizontalScrollTextHolder hHolder = null;
        VerticalScrollTextHolder vHolder = null;

        int currentDeepth = position.length;
        for (int i = mPreDeepth; i >= currentDeepth; i--) {
            if (mTextHolder[i] != null) {
                if (FishEyeView.orientation == FishEyeOrientation.Vertical) {
                    ((VerticalScrollTextHolder) mTextHolder[i]).clear();
                } else {
                    ((HorizontalScrollTextHolder) mTextHolder[i]).clear();
                }
            }
        }
        mPreDeepth = currentDeepth;

        if (mTextHolder[currentDeepth] == null) {
            Log.w(TAG, "or mTextHolder[currentDeepth] is null. currentDeepth is "
                    + currentDeepth);
            return;
        }

        if (FishEyeView.orientation == FishEyeOrientation.Vertical) {
            vHolder = (VerticalScrollTextHolder) mTextHolder[currentDeepth];
            vHolder.setItemData(data);
            vHolder.setPostion(position);
            vHolder.refresh(mCurrentCenterCoordinate);
            if(mCurrentPosition != -1 && currentDeepth == 1){
                vHolder.setIndexText(mAlphaIndex[mCurrentPosition]);
            }
        } else {
            hHolder = (HorizontalScrollTextHolder) mTextHolder[currentDeepth];
            hHolder.setItemData(data);
            hHolder.setPostion(position);
            hHolder.refresh(mCurrentCenterCoordinate);
            if(mCurrentPosition != -1 && currentDeepth == 1){
                hHolder.setIndexText(mAlphaIndex[mCurrentPosition]);
            }
        }
    }

    public void childInputDetect(boolean hasInput) {
        if (hasInput) {
            if (mHasDetectStarted) {
                mHasDetectStarted = false;
                mDetectHandler.removeCallbacks(mDetectedInputRunable);
            }
        } else {
            if (!mHasDetectStarted) {
                mDetectHandler.postDelayed(mDetectedInputRunable, 3000);
                mHasDetectStarted = true;
            }
        }
    }

    private Runnable mDetectedInputRunable = new Runnable() {

        @Override
        public void run() {
            reset();
            mHasDetectStarted = false;
        }
    };

    public void changeFishEyeOrientation(FishEyeOrientation o) {
        for (int i = 1; i < mDepth; i++) {
            mRelativeLayout[i].removeAllViews();
            removeView(mRelativeLayout[i]);
        }
        mCurrentPosition = -1;
        mPreDeepth = 0;
        mIsTextHoldersAdded = false;
        mAlphaIndexBar.removeAllViews();
        boolean[] mask = mAlphaIndexBar.getEnableMask();
        removeView(mAlphaIndexBar);
        createAlphaBar();
        updateAlphaEnableStates(mask);
        createTextHolder();
    }

    public void reset() {
        for (int i = mPreDeepth; i > 0; i--) {
            if (orientation == FishEyeOrientation.Vertical) {
                ((VerticalScrollTextHolder) mTextHolder[i]).clear();
            } else {
                ((HorizontalScrollTextHolder) mTextHolder[i]).clear();
            }
        }
        mCurrentPosition = -1;
        mAlphaIndexBar.clear();
        if(mOnChildClickListener != null){
            mOnChildClickListener.onChildClear();
        }
    }

    private ViewPager mParentViewPager;

    public void setParentViewPager(ViewPager pager) {
        mParentViewPager = pager;
    }
}
