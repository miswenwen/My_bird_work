package com.yunos.alicontacts.list.fisheye;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.dialpad.smartsearch.HanziUtil;
import com.yunos.alicontacts.dialpad.smartsearch.PinyinSearch;

public class FishEyeContactsCache {

    public static final String TAG = "FishEyeContactsCache";

    private Name[] mCache;
    private boolean mQueryedPinyin = false;

    public FishEyeContactsCache(Cursor cursor, int nameColIdx) {
        int count = 0;
        if (cursor != null) {
            count = cursor.getCount();
        }
        Log.d(TAG, "FishEyeContactsCache: count="+count);
        mCache = new Name[count];
        if (count == 0) {
            return;
        }
        fillCacheFromCursor(cursor, nameColIdx);
    }

    public Name getContact(int position) {
        return mCache[position];
    }

    public int getCount() {
        return mCache.length;
    }

    public void queryPinyinForAll(Context context) {
        if (mQueryedPinyin) {
            return;
        }
        PinyinSearch.initHanziPinyinForAllChars(context);
        String name;
        for (Name n : mCache) {
            name = n == null ? null : n.nameSource;
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            char firstChar = name.charAt(0);
            String firstCharPinyin = getPinyinForChar(firstChar);
            if (!TextUtils.isEmpty(firstCharPinyin)) {
                n.xing = String.valueOf(firstChar);
                n.pyIndex = Character.toUpperCase(firstCharPinyin.charAt(0));
            } else {
                n.xing = null;
                n.pyIndex = '*';
            }
        }
        mQueryedPinyin = true;
    }

    public void clean() {
        mCache = new Name[0];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("FishEyeContactsCache:{mCache size:").append(mCache.length)
          .append("; mQueryedPinyin:").append(mQueryedPinyin).append('}');
        return sb.toString();
    }

    private void fillCacheFromCursor(Cursor cursor, int nameColIdx) {
        cursor.moveToPosition(-1);
        int pos;
        Name n;
        while (cursor.moveToNext()) {
            pos = cursor.getPosition();
            n = new Name();
            n.pos = pos;
            n.nameSource = cursor.getString(nameColIdx);
            mCache[pos] = n;
        }
    }

    private String getPinyinForChar(char ch) {
        int code = ch;
        if (HanziUtil.isHanziCharCode(code)) {
            return PinyinSearch.getHanziPinyinDataForCharCode(code, 0);
        } else {
            return null;
        }
    }

}
