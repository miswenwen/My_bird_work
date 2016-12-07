
package com.yunos.yundroid.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.FishEyeView.FishEyeOrientation;

public class AlphaIndexButton extends View {

    private String[] mAlphaIndex;
    private Paint mPaint = new Paint();

    private int mSelectIndex = -1;
    private OnTouchAlphaIndexListener mOnTouchListener;

    private int mEnableFontColor;
    private int mDisableFontColor;
    private float mTextSize;

    private int mWidth = 0;
    private int mHeight = 0;

    float mInterval = 0;

    private boolean[] mEnableMasks;

    public AlphaIndexButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public AlphaIndexButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlphaIndexButton(Context context) {
        super(context);
        init();
    }

    public void setOnTouchAlphaIndexListener(OnTouchAlphaIndexListener listener) {
        this.mOnTouchListener = listener;
    }

    public void setAlphaIndex(String[] index) {
        mAlphaIndex = index;
    }

    private void init() {
        mEnableFontColor = getResources().getColor(R.color.fisheye_alpha_enable_text_color);
        mDisableFontColor = getResources().getColor(R.color.fisheye_alpha_disable_text_color);
        mTextSize = getResources().getDimensionPixelSize(R.dimen.fish_eye_alpha_text_size);
    }

    public void setEnableMask(boolean[] mask) {
        mEnableMasks = mask;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mAlphaIndex == null || mAlphaIndex.length == 0) {
            return;
        }
        if (mWidth == 0 && mHeight == 0) {
            mWidth = getWidth();
            mHeight = getHeight();
        }
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setTextSize(mTextSize);
        mPaint.setAntiAlias(true);

        FontMetrics fontMetrics = mPaint.getFontMetrics();
        float fontHeight = fontMetrics.descent - fontMetrics.ascent;
        int count = mAlphaIndex.length + 1;
        float xPosition = 0;
        float yPosition = 0;

        if (FishEyeView.orientation == FishEyeOrientation.Vertical) {
            mInterval = (float) mHeight / count;
        } else {
            yPosition = mHeight / 2 + fontHeight / 2;
            mInterval = (float) mWidth / count;
        }
        for (int i = 0; i < mAlphaIndex.length; i++) {
            if (mEnableMasks != null && mEnableMasks[i]) {
                mPaint.setColor(mEnableFontColor);
                mPaint.setAlpha(255);
            } else {
                mPaint.setColor(mDisableFontColor);
                mPaint.setAlpha(178);
            }

            if (FishEyeView.orientation == FishEyeOrientation.Vertical) {
                xPosition = mWidth / 2 - mPaint.measureText(mAlphaIndex[i]) / 2;
                yPosition += mInterval;
            } else {
                xPosition += mInterval;
            }
            canvas.drawText(mAlphaIndex[i], xPosition, yPosition, mPaint);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int index = -1;// touch char index

        if (mWidth == 0 && mHeight == 0) {
            mWidth = getWidth();
            mHeight = getHeight();
        }

        float charPosition = 0;
        float fingerPosition = 0;
        if (FishEyeView.orientation == FishEyeOrientation.Vertical) {
            index = (int) (y / mHeight * mAlphaIndex.length);
            fingerPosition = y;
        } else {
            index = (int) (x / mWidth * mAlphaIndex.length);
            fingerPosition = x;
        }
        charPosition = (index + 1) * mInterval;
        int adjustIdx = index;
        if (fingerPosition > charPosition + mInterval / 2) {
            adjustIdx++;
        } else if (fingerPosition < charPosition - mInterval / 2) {
            adjustIdx--;
        }
        if (adjustIdx >= 0 && adjustIdx < mAlphaIndex.length) {
            index = adjustIdx;
        }
        if (index >= 0 && index < mAlphaIndex.length) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mSelectIndex = index;
                    if (mOnTouchListener != null) {
                        mOnTouchListener.onTouchAlphaIndexDown(mSelectIndex);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mSelectIndex != index) {
                        mSelectIndex = index;
                        if (mOnTouchListener != null) {
                            mOnTouchListener.onTouchAlphaIndexMove(mSelectIndex);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (mOnTouchListener != null) {
                        mOnTouchListener.onTouchAlphaIndexUp(mSelectIndex);
                    }
                    mSelectIndex = -1;
                    break;
                default:
                    break;
            }
        } else {
            mSelectIndex = -1;
            if (mOnTouchListener != null) {
                mOnTouchListener.onTouchAlphaIndexUp(mSelectIndex);
            }
        }
        invalidate();
        return true;
    }

    public interface OnTouchAlphaIndexListener {
        public void onTouchAlphaIndexDown(int index);

        public void onTouchAlphaIndexUp(int index);

        public void onTouchAlphaIndexMove(int index);
    }

}
