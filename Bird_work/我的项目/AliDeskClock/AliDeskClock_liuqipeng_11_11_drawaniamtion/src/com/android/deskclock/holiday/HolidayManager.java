package com.android.deskclock.holiday;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.deskclock.Log;

public final class HolidayManager {
    public static final String TAG = "HolidayManager";

    public static final String ACTION_LEGAL_HOLIDAY_CHANGED = "com.yunos.calendar.LEGAL_HOLIDAY_CHANGED";
    public static final String SHARED_PREFERENCE_NAME = "com.yunos.content.holiday_preferences";

    public static final String KEY_VERSION = "version";
    public static final String KEY_YEARS = "years";
    public static final String KEY_YEAR_HOLIDAY_PREFIX = "year_holiday_";
    public static final String KEY_YEAR_WORKDAY_PREFIX = "year_workday_";

    public static final String SEPARATOR = ",";

    public static class HolidayByYear {
        private int mYear;
        private int[] mMonthHolidayData;
        private int[] mMonthWorkdayData;

        public HolidayByYear(int year, int[] monthHolidayData, int[] monthWorkData) {
            mYear = year;
            mMonthHolidayData = monthHolidayData;
            mMonthWorkdayData = monthWorkData;
        }

        public int getYear() {
            return mYear;
        }

        public boolean isHoliday(int month, int monthDay) {
            return (mMonthHolidayData[month] & 1 << (monthDay - 1)) != 0;
        }

        public boolean isWorkday(int month, int monthDay) {
            return (mMonthWorkdayData[month] & 1 << (monthDay - 1)) != 0;
        }
    }

    private static class HolidayCache {
        public int version = -1;
        public Set<Integer> yearSet = new HashSet<Integer>();
        public SparseArray<HolidayByYear> yearMap = new SparseArray<HolidayByYear>();
    }

    private Context mContext;
    private HolidayCache mCache;
    private Object mCacheLock = new Object();
    private Set<ContentObserver> mObserverSet = new HashSet<ContentObserver>();


    private static volatile HolidayManager sInstance;

    public static HolidayManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (HolidayManager.class) {
                if (sInstance == null) {
                    HolidayManager service = new HolidayManager(context);
                    service.refresh();
                    sInstance = service;
                }
            }
        }
        return sInstance;
    }

    private HolidayManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public int getVersion() {
        synchronized (mCacheLock) {
            return mCache.version;
        }
    }

    public boolean isHoliday(int year, int month, int monthDay) {
        HolidayByYear yearData = getHolidayByYear(year);
        if (yearData == null) {
            Log.d("--------------");
            return false;
        }
        return yearData.isHoliday(month, monthDay);
    }

    public boolean isWorkday(int year, int month, int monthDay) {
        HolidayByYear yearData = getHolidayByYear(year);
        if (yearData == null) {
            return false;
        }
        return yearData.isWorkday(month, monthDay);
    }

    public HolidayByYear getHolidayByYear(int year) {
        synchronized (mCacheLock) {
            boolean hasYearData = mCache.yearSet.contains(year);
            if (!hasYearData) {
                return null;
            }

            HolidayByYear yearData = mCache.yearMap.get(year);
            if (yearData == null) {
                SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
                yearData = readHolidayByYear(sp, year);
                mCache.yearMap.put(year, yearData);
            }
            return yearData;
        }
    }

    public void refresh() {
        refresh(true);
    }

    public void refresh(boolean lazy) {
        Log.d("refresh holiday info, lazy:" + lazy);
        try {
            HolidayCache newCache = new HolidayCache();
            SharedPreferences sp = getSharedPreferences();
            if (lazy) {
                readVersion(sp, newCache);
                readYears(sp, newCache);
            } else {
                readAll(sp, newCache);
            }
            synchronized (mCacheLock) {
                mCache = newCache;
            }
            dispatchChange();
        } catch (Exception e) {
            Log.e("refresh holiday info failed!", e);
        }
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
    }

    private void readAll(SharedPreferences sp, HolidayCache cache) {
        readVersion(sp, cache);
        readYears(sp, cache);
        for (int year : cache.yearSet) {
            HolidayByYear data = readHolidayByYear(sp, year);
            cache.yearMap.put(year, data);
        }
    }

    private void readVersion(SharedPreferences sp, HolidayCache cache) {
        cache.version = sp.getInt(KEY_VERSION, -1);
    }

    private void readYears(SharedPreferences sp, HolidayCache cache) {
        readYears(sp, cache.yearSet);
    }

    private void readYears(SharedPreferences sp, Set<Integer> yearSet) {
        String yearsStr = sp.getString(KEY_YEARS, null);
        if (TextUtils.isEmpty(yearsStr)) {
            return;
        }
        String[] years = yearsStr.split(SEPARATOR);
        for (String y : years) {
            if (TextUtils.isEmpty(y)) {
                continue;
            }
            int year = Integer.parseInt(y);
            yearSet.add(year);
        }
    }

    private HolidayByYear readHolidayByYear(SharedPreferences sp, int year) {
        int[] holidayData = readHolidayByYear(sp, year, true);
        int[] workdayData = readHolidayByYear(sp, year, false);
        HolidayByYear yearData = new HolidayByYear(year, holidayData, workdayData);
        return yearData;
    }

    private int[] readHolidayByYear(SharedPreferences sp, int year, boolean isHoliday) {
        String yearKey = isHoliday ? getHolidayKey(year) : getWorkdayKey(year);
        String yearDataStr = sp.getString(yearKey, null);
        int[] monthData = new int[12];
        Arrays.fill(monthData, 0);
        if (!TextUtils.isEmpty(yearDataStr)) {
            String[] monthDataStrs = yearDataStr.split(SEPARATOR);
            for (int i = 0; i < 12 && i < monthDataStrs.length; i ++) {
                monthData[i] = Integer.parseInt(monthDataStrs[i]);
            }
        }
        return monthData;
    }

    public void saveHoliday(int version, List<HolidayByYear> data) {
        SharedPreferences sp = getSharedPreferences();
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        int size = data.size();
        int[] years = new int[size];
        for (int i = 0; i < size; i ++) {
            HolidayByYear yearData = data.get(i);
            years[i] = yearData.mYear;
            saveHolidayByYear(editor, yearData);
        }
        saveYears(editor, years);
        saveVersion(editor, version);
        editor.commit();

        refresh();
    }

    private void saveVersion(SharedPreferences.Editor editor, int version) {
        editor.putInt(KEY_VERSION, version);
    }

    private void saveYears(SharedPreferences.Editor editor, int[] years) {
        String value = joinIntArray(years, SEPARATOR);
        editor.putString(KEY_YEARS, value);
    }

    private void saveHolidayByYear(SharedPreferences.Editor editor, HolidayByYear yearData) {
        saveHolidayByYear(editor, yearData, true);
        saveHolidayByYear(editor, yearData, false);
    }

    private void saveHolidayByYear(SharedPreferences.Editor editor,
            HolidayByYear yearData, boolean isHoliday) {
        int year = yearData.mYear;
        String key = null;
        int[] data = null;
        if (isHoliday) {
            key = getHolidayKey(year);
            data = yearData.mMonthHolidayData;
        } else {
            key = getWorkdayKey(year);
            data = yearData.mMonthWorkdayData;
        }
        String value = joinIntArray(data, SEPARATOR);
        editor.putString(key, value);
    }

    private String getHolidayKey(int year) {
        return KEY_YEAR_HOLIDAY_PREFIX + year;
    }

    private String getWorkdayKey(int year) {
        return KEY_YEAR_WORKDAY_PREFIX + year;
    }

    private static String joinIntArray(int[] values, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i ++) {
            if (i != 0) {
                sb.append(sep);
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    public void registerContentObserver(ContentObserver observer) {
        synchronized (mObserverSet) {
            mObserverSet.add(observer);
        }
    }

    public void unregisterContentObserver(ContentObserver observer) {
        synchronized (mObserverSet) {
            mObserverSet.remove(observer);
        }
    }

    public void dispatchChange() {
        Log.i("dispatchChange() holiday");
        mContext.sendBroadcast(new Intent(ACTION_LEGAL_HOLIDAY_CHANGED));

        Set<ContentObserver> observers = new HashSet<ContentObserver>();
        synchronized (mObserverSet) {
            observers.addAll(mObserverSet);
        }
        for (ContentObserver o : observers) {
            o.dispatchChange(false, null);
        }
    }

}
