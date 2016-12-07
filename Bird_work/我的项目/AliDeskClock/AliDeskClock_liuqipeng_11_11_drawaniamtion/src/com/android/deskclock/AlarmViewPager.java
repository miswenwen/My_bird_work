package com.android.deskclock;

import android.content.Context;
import yunos.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class AlarmViewPager extends ViewPager{

    private boolean isScrollable = false;

    public AlarmViewPager(Context context) {
        super(context);
    }

    public AlarmViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isScrollable == false) {
            return false;
        } else {
            //YUNOS BEGIN PB
            //7820409 shangyue.zsy
            //2016/01/22
            try{
                return super.onTouchEvent(ev);
            }catch (IllegalArgumentException ex){
                ex.printStackTrace();
            }
        }
        return false;
        //YUNOS END PB
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isScrollable == false) {
            return false;
        } else {
            //YUNOS BEGIN PB
            //7820409 shangyue.zsy
            //2016/01/22
            try{
                return super.onInterceptTouchEvent(ev);
            }catch (IllegalArgumentException ex){
                ex.printStackTrace();
            }
        }
        return false;
        //YUNOS END PB
    }

    public boolean isScrollable() {
        return isScrollable;
    }

    public void setScrollable(boolean isScrollable) {
        this.isScrollable = isScrollable;
    }
}
