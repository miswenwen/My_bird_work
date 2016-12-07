
package com.yunos.alicontacts.xiaoyun;

import android.content.Context;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class XiaoYunGestureHelper {
    private static final String TAG = "XiaoYunGestureHelper";
    private static final String XIAOYUN_ACTION = "com.yunos.xiaoyun.helper";
    private static final String XIAOYUN_SERVICE_PACKAGE_NAME = "com.yunos.assistant";
    private static final String XIAOYUN_SERVICE_CLASS_NAME = "com.yunos.assistant.ui.floating.FloatingWindowService";
    private final int CONTACTS_COUNT = 10;
    private Context mContext;
    private ListView mListView;
    private GestureDetector mGestureDetector;

    public XiaoYunGestureHelper(Context context, ListView listView) {
        mContext = context;
        mListView = listView;
    }

    public void monitorGesture() {
        mGestureDetector = new GestureDetector(mContext, new XiaoYunGestureListener());
        mListView.setTextFilterEnabled(true);
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return false;
            }
        });
    }

    private void callXiaoYun() {
        try {
            if (mListView == null || mListView.getCount() < CONTACTS_COUNT) {
                return;
            }
            Intent intent = new Intent();
            intent.setAction(XIAOYUN_ACTION);
            intent.setClassName(XIAOYUN_SERVICE_PACKAGE_NAME, XIAOYUN_SERVICE_CLASS_NAME);
            mContext.startService(intent);
        } catch (SecurityException e) {
            android.util.Log.e(TAG, "callXiaoYun() error !", e);
        }
    }

    class XiaoYunGestureListener implements GestureDetector.OnGestureListener {
        private XiaoYunHelper myQueue = XiaoYunHelper.getInstance();
        private boolean mIsClearOnDown = false;
        private long mStartTime = 0;

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onDown(MotionEvent e) {
            mStartTime = System.currentTimeMillis();
            if (mIsClearOnDown) {
                myQueue.clear();
                mIsClearOnDown = false;
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (myQueue.onFling(mStartTime, velocityY)) {
                callXiaoYun();
                myQueue.clear();
                mIsClearOnDown = true;
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }
    }
}
