package com.yunos.alicontacts.xiaoyun;

import java.lang.Math;
import java.util.LinkedList;
import java.util.List;

public class XiaoYunHelper {
    private static final String TAG = "XiaoYunHelper";

    private static final long INTERVAL_TIME = 10000;
    private static final long QUICK_VELOCITY = 10;
    private static final int MAX_QUEUE_SIZE = 12;
    private static final int QUICK_FLING_TIMES = 10;
    private static final int QUICK_FLING_CONTINUE_TIMES = 1;

    private static final Object[] mLock = new Object[0];
    private static XiaoYunHelper mInstance = null;
    private List<XiaoYunHelperData> mData = new LinkedList<XiaoYunHelperData>();

    public static XiaoYunHelper getInstance() {
        synchronized (mLock) {
            return mInstance == null ? mInstance = new XiaoYunHelper() : mInstance;
        }
    }

    private XiaoYunHelper () {}

    private XiaoYunHelperData enqueue(XiaoYunHelperData data) {
        if (mData.size() >= MAX_QUEUE_SIZE) {
            XiaoYunHelperData result = dequeue();
            mData.add(data);
            return result;
        } else {
            mData.add(data);
            return null;
        }
    }

    private XiaoYunHelperData dequeue(){
        if (mData.size() < 1){
            return null;
        } else{
            return mData.remove(0);
        }
    }

    public void clear(){
        mData.clear();
    }

    public boolean onFling(long startTime, float velocityY) {
        enqueue(new XiaoYunHelperData(startTime, velocityY));
        int size = mData.size();
        if (size < QUICK_FLING_TIMES) return false;

        XiaoYunHelperData data;
        int quickFlingTimes = 0;
        int quickFlingContinueTimes = 0;
        boolean quickFlingTwice = false;
        boolean directionUp = false, directionDown = false;
        for (int index = 0; index < size;) {
            data = mData.get(index);
            if ((startTime - data.startTime) > INTERVAL_TIME) {
                //Removed more than 5 seconds item.
                mData.remove(0);
                size = mData.size();
            } else {
                if (size < QUICK_FLING_TIMES) return false;
                if (Math.abs(data.velocityY) > QUICK_VELOCITY) {
                    quickFlingTimes++;
                    quickFlingContinueTimes++;
                } else {
                    quickFlingContinueTimes = 0;
                }
                if (quickFlingContinueTimes >= QUICK_FLING_CONTINUE_TIMES) {
                    quickFlingTwice = true;
                }
                if (data.velocityY > 0) {
                    directionDown = true;
                } else {
                    directionUp = true;
                }
                index++;
            }
        }
        return (quickFlingTimes >= QUICK_FLING_TIMES) && (quickFlingTwice || (directionDown && directionUp));
    }

    public String toString(){
        if (mData.size() == 0){
            return "";
        } else{
            StringBuilder result = new StringBuilder();
            for (int i = 0 ; i < mData.size(); i++){
                result.append("\r\n no." + i + "=" + mData.get(i).startTime + "," + mData.get(i).velocityY);
            }
            return result.toString();
        }
    }

    private class XiaoYunHelperData {
        public long startTime;
        public float velocityY;
        public XiaoYunHelperData(long startTime, float velocityY){
            this.startTime = startTime;
            this.velocityY = velocityY;
        }
    }
}
