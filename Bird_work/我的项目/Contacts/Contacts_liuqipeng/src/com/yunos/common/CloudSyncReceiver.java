
package com.yunos.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class CloudSyncReceiver extends BroadcastReceiver {
    private static final String TAG = "CloudSyncReceiver";
    public static final String ACTION_CALLLOG_SYNC_COMPLETED = "com.yunos.alicontacts.CALLLOG_SYNC_COMPLETED";
    public static final String ACTION_CONTACTS_SYNC_COMPLETED = "com.yunos.alicontacts.CONTACTS_SYNC_COMPLETED";
    public static final String EXTRA_KEY_LASTTIME = "extra_key_lasttime";
    public static final String CALLLOG_SYNC_LASTTIME_PREFERENCE = "calllog_sync_lasttime_preference";
    public static final String CONTACT_SYNC_LASTTIME_PREFERENCE = "contact_sync_lasttime_preference";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "LFJ onReceive: action = " + action);
        if (action.equals(ACTION_CALLLOG_SYNC_COMPLETED)) {
            long value = intent.getLongExtra(EXTRA_KEY_LASTTIME, 0);
            Log.d(TAG, "LFJ onReceive: value = " + value);
            this.writeToDefaultSharedPreference(context, CALLLOG_SYNC_LASTTIME_PREFERENCE, value);
        }

        if (action.equals(ACTION_CONTACTS_SYNC_COMPLETED)) {
            long value = intent.getLongExtra(EXTRA_KEY_LASTTIME, 0);
            Log.d(TAG, "LFJ onReceive: value = " + value);
            this.writeToDefaultSharedPreference(context, CONTACT_SYNC_LASTTIME_PREFERENCE, value);
        }
    }

    public static void writeToDefaultSharedPreference(Context context, String key, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(key, value).commit();
    }
}
