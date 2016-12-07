package com.yunos.yundroid.widget;

import yunos.support.v4.view.ViewPager;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

// In search mode(Contacts/Yellowpage), we forbid user to scroll the view page
// So we inherit ViewPager class to control the scroll of it.
public class CustomViewPager extends ViewPager {

    private static final String TAG = "CustomViewPager";

    private boolean isCanScroll = true;

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Use this function to control the scroll of the view page
    public void setScanScroll(boolean isCanScroll){
        this.isCanScroll = isCanScroll;
    }

    /*@Override
    public void scrollTo(int x, int y){
        if (isCanScroll){
            super.scrollTo(x, y);
        }
    }*/

    // The following two functions used to handle the click on the indication bar
    @Override
    public void setCurrentItem(int item) {
        if (isCanScroll) {
            super.setCurrentItem(item);
        }
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (isCanScroll) {
            super.setCurrentItem(item, smoothScroll);
        }
    }

    // The following two functions used to handle the touch and move event
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isCanScroll) {
            // This is an known issue, please see
            // http://code.google.com/p/android/issues/detail?id=64553
            try {
                return super.onInterceptTouchEvent(ev);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "onInterceptTouchEvent exception:", e);
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isCanScroll) {
            // This is an known issue, please see
            // http://code.google.com/p/android/issues/detail?id=64553
            try {
                return super.onTouchEvent(ev);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "onTouchEvent exception:", e);
            }
        }
        return false;
    }
}

