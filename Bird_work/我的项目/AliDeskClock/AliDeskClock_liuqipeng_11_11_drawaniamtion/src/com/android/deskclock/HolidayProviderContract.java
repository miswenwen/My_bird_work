package com.android.deskclock;
import android.net.Uri;


public final class HolidayProviderContract {
    public static final String AUTHORITY = "com.android.calendar";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String ACTION_HOLIDAY_CHANGED = "com.yunos.provider.calendar.HOLIDAY_CHANGED";
    public static final String METHOD_READ_HOLIDAY_DATA = "readHolidayData";
    public static final String KEY_HOLIDAY_VERSION = "holidayVersion";
    public static final String KEY_HOLIDAY_DATA = "holidayData";
}

