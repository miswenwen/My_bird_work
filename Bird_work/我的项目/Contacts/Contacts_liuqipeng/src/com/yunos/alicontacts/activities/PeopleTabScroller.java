
package com.yunos.alicontacts.activities;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class PeopleTabScroller extends Scroller {
    private int mDuration = 0;

    public PeopleTabScroller(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public PeopleTabScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
        // TODO Auto-generated constructor stub
    }

    public PeopleTabScroller(Context context, Interpolator interpolator, boolean flywheel) {
        super(context, interpolator, flywheel);
        // TODO Auto-generated constructor stub
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        super.startScroll(startX, startY, dx, dy, mDuration);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy) {
        super.startScroll(startX, startY, dx, dy, mDuration);
    }
}
