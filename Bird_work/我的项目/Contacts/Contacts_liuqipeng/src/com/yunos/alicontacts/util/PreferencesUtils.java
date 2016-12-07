package com.yunos.alicontacts.util;

import android.annotation.NonNull;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;

public final class PreferencesUtils {

    private PreferencesUtils() { }

    /**
     * Commit a long value to SharedPreferences in background thread.
     * The commit might be executed in a later time,
     * so consider the effect of read dirty data when call this method.
     * <p>This method must be invoked on the UI thread.
     * @param pref
     * @param key
     * @param value
     */
    public static void commitLongSharedPreferencesInBackground(
            @NonNull final SharedPreferences pref, @NonNull final String key, final long value) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Editor editor = pref.edit();
                editor.putLong(key, value);
                editor.commit();
                return null;
            }

        };
        task.execute();
    }

    /**
     * Commit an int value to SharedPreferences in background thread.
     * The commit might be executed in a later time,
     * so consider the effect of read dirty data when call this method.
     * <p>This method must be invoked on the UI thread.
     * @param pref
     * @param key
     * @param value
     */
    public static void commitIntSharedPreferencesInBackground(
            @NonNull final SharedPreferences pref, @NonNull final String key, final int value) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Editor editor = pref.edit();
                editor.putInt(key, value);
                editor.commit();
                return null;
            }

        };
        task.execute();
    }

}
