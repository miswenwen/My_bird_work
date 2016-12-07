package com.android.deskclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class AlarmListView extends ListView {

    private boolean isScrollable = false;
    private ListViewMoveListener mListViewMoveListener;
    public interface ListViewMoveListener{
        void onListViewMoveListener(View v, MotionEvent event);
    }

    public void setListViewMoveListener(ListViewMoveListener lisener){
        mListViewMoveListener = lisener;
    }

    public AlarmListView(Context context) {
        super(context);
    }

    public AlarmListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isScrollable == false) {
            if (mListViewMoveListener != null) {
                mListViewMoveListener.onListViewMoveListener(this, ev);
            }
            return false;
        } else {
            return super.onInterceptTouchEvent(ev);
        }

    }

    public boolean isScrollable() {
        return isScrollable;
    }

    public void setScrollable(boolean isScrollable) {
        this.isScrollable = isScrollable;
    }

}
