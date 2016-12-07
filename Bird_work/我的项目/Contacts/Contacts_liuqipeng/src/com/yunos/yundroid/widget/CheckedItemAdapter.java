package com.yunos.yundroid.widget;

import java.util.List;

import android.content.Context;
import android.widget.BaseAdapter;

import com.yunos.yundroid.widget.item.Item;

public abstract class CheckedItemAdapter extends BaseAdapter{

    private boolean mSelectedArray[];
    private int mSelectedCount;

    protected void initSelectedArray(int count) {
        if(count > 0) {
            mSelectedArray = new boolean[count];
        } else {
            mSelectedArray = null;
        }
    }

    protected void initSelectedArray(Item[] mItems) {
        if(mItems.length > 0 && mItems != null ) {
            mSelectedArray = new boolean[mItems.length];
        } else {
            mSelectedArray = null;
        }
    }

    protected void initSelectedArray(List<Item> mItems) {
        //if(mItems.size() > 0 && mItems != null ) {
        if(mItems != null && !mItems.isEmpty()) {
            mSelectedArray = new boolean[mItems.size()];
        } else {
            mSelectedArray = null;
        }
    }

    public void setSelectedItem(int pos) {
        if(mSelectedArray == null) return;

        mSelectedArray[pos] = !mSelectedArray[pos];

        if(mSelectedArray[pos]) {
            mSelectedCount ++;
        } else {
            mSelectedCount --;
        }
    }

    public void doAllSelected(boolean select) {
        if(mSelectedArray == null) return;

        for (int i = 0; i < mSelectedArray.length; i++) {
            mSelectedArray[i] = select;
        }

        if(select) {
            mSelectedCount = mSelectedArray.length;
        } else {
            mSelectedCount = 0;
        }
    }

    public void doAllSelected() {
        if(mSelectedArray == null) return;

        boolean select = false;

        for (int i = 0; i < mSelectedArray.length; i++) {
            if(mSelectedArray[i] == select) {
                select = true;
                break;
            }
        }

        doAllSelected(select);
    }

    public boolean[] getSelectedArray() {
        return mSelectedArray;
    }

    public boolean getSelectedStatus(int pos) {
        if(mSelectedArray.length > pos && pos >= 0 && mSelectedArray != null) {
            return mSelectedArray[pos];
        } else {
            return false;
        }
    }

    public int getSelectedCounts() {
        return mSelectedCount;
    }

}
