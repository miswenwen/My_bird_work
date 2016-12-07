package com.yunos.alicontacts.database.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.util.AliTextUtils;

public final class NumberServiceHelper {

    private static final String TAG = "NumberServiceHelper";

    public static final Uri BATCH_NUMINFO_QUERY_URI = Uri.parse("content://com.yunos.numberservice/numberinfo/batch");
    public static final int BATCH_NUMINFO_COLUMN_NUMBER = 0;
    public static final int BATCH_NUMINFO_COLUMN_PROVINCE = 1;
    public static final int BATCH_NUMINFO_COLUMN_AREA = 2;
    public static final int BATCH_NUMINFO_COLUMN_SERVER_TAG_NAME = 3;
    public static final int BATCH_NUMINFO_COLUMN_MARK_TAG_COUNT = 4;
    public static final int BATCH_NUMINFO_COLUMN_USER_TAG_NAME = 5;
    public static final int BATCH_NUMINFO_COLUMN_YP_NAME = 6;
    public static final int BATCH_NUMINFO_COLUMN_YP_LOGO_URI = 7;
    public static final int BATCH_NUMINFO_COLUMN_YP_SHOP_ID = 8;

    public static final String SINGLE_NUMINFO_QUERY_URI_STR = "content://com.yunos.numberservice/numberinfo/single";
    public static final Uri SINGLE_NUMINFO_QUERY_URI = Uri.parse(SINGLE_NUMINFO_QUERY_URI_STR);
    public static final int SINGLE_NUMINFO_COLUMN_PROVINCE = 0;
    public static final int SINGLE_NUMINFO_COLUMN_AREA = 1;
    public static final int SINGLE_NUMINFO_COLUMN_SERVER_TAG_NAME = 2;
    public static final int SINGLE_NUMINFO_COLUMN_MARKED_COUNT = 3;
    public static final int SINGLE_NUMINFO_COLUMN_USER_TAG_NAME = 4;
    public static final int SINGLE_NUMINFO_COLUMN_YP_NAME = 5;
    public static final int SINGLE_NUMINFO_COLUMN_YP_LOGO_URI = 6;
    public static final int SINGLE_NUMINFO_COLUMN_YP_SHOP_ID = 7;

    public static final Uri LOCATION_BATCH_QUERY_URI = Uri.parse("content://com.yunos.numberservice.location/batch");
    public static final int LOCATION_BATCH_COLUMN_NUMBER = 0;
    public static final int LOCATION_BATCH_COLUMN_PROVINCE = 1;
    public static final int LOCATION_BATCH_COLUMN_AREA = 2;

    public static final Uri LOCATION_SINGLE_QUERY_URI = Uri.parse("content://com.yunos.numberservice.location/single");
    public static final int LOCATION_SINGLE_COLUMN_PROVINCE = 0;
    public static final int LOCATION_SINGLE_COLUMN_AREA = 1;

    public static final Uri MARK_URI = Uri.parse("content://com.yunos.numberservice.numbertag/mark");
    private static final String MARK_KEY_NUMBER = "number";
    private static final String MARK_KEY_TAG_NAME = "tag_type";
    private static final int RETRY_COUNT_FOR_MARK_NUMBER = 5;
    private static final long MARK_NUMBER_WAIT_INTERVAL = 1000;

    private static final String QUERY_PARAM_KEY_NUMBERS = "numbers";
    private static final String QUERY_PARAM_KEY_SOURCE = "source";
    private static final String QUERY_PARAM_VALUE_SOURCE = "Contact";

    private NumberServiceHelper() { }

    public static Uri getBatchNumInfoQueryInfoForNumbers(String numbers) {
        return BATCH_NUMINFO_QUERY_URI.buildUpon()
                .appendQueryParameter(QUERY_PARAM_KEY_NUMBERS, numbers.toString())
                .appendQueryParameter(QUERY_PARAM_KEY_SOURCE, QUERY_PARAM_VALUE_SOURCE).build();
    }

    public static Uri getSingleNumberInfoQueryForNumber(String number) {
        return Uri.withAppendedPath(SINGLE_NUMINFO_QUERY_URI, number).buildUpon()
                .appendQueryParameter(QUERY_PARAM_KEY_SOURCE, QUERY_PARAM_VALUE_SOURCE).build();
    }

    public static Uri getBatchLocationQueryUriForNumbers(String numbers) {
        return LOCATION_BATCH_QUERY_URI.buildUpon()
                .appendQueryParameter(QUERY_PARAM_KEY_NUMBERS, numbers.toString())
                .appendQueryParameter(QUERY_PARAM_KEY_SOURCE, QUERY_PARAM_VALUE_SOURCE).build();
    }

    public static Uri getSingleLocationQueryUriForNumber(String number) {
        return Uri.withAppendedPath(LOCATION_SINGLE_QUERY_URI , number).buildUpon()
                .appendQueryParameter(QUERY_PARAM_KEY_SOURCE, QUERY_PARAM_VALUE_SOURCE).build();
    }

    public static boolean markNumberWithRetryInBackground(Context context, String number, String tagName) {
        boolean result = false;
        ContentValues cv = new ContentValues(2);
        cv.put(MARK_KEY_NUMBER, number);
        if (!TextUtils.isEmpty(tagName)) {
            cv.put(MARK_KEY_TAG_NAME, tagName);
        }
        Log.i(TAG, "markNumberWithRetry: number="+AliTextUtils.desensitizeNumber(number)+"; tagName="+tagName);
        ContentResolver resolver = context.getContentResolver();
        int retry = 0;
        while ((retry++) < RETRY_COUNT_FOR_MARK_NUMBER) {
            try {
                Uri markUri = resolver.insert(MARK_URI, cv);
                if (markUri == null) {
                    Log.i(TAG, "markNumberWithRetry: got null for mark on try "+retry);
                } else {
                    result = true;
                    Log.i(TAG, "markNumberWithRetry: success on try "+retry);
                    break;
                }
            } catch (SQLiteException sqle) {
                Log.e(TAG, "markNumberWithRetry: failed to mark number on try "+retry, sqle);
            }
            if (retry < RETRY_COUNT_FOR_MARK_NUMBER) {
                sleep(MARK_NUMBER_WAIT_INTERVAL);
            }
        }
        return result;
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ie) {
            // ignore.
        }
    }

}
