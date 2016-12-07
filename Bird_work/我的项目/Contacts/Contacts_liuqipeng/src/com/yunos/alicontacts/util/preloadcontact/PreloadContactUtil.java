package com.yunos.alicontacts.util.preloadcontact;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.util.Log;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.util.PreferencesUtils;

/**
 * This class will add preload contacts to DB and check if them are still in DB periodically.
 * The preload contacts information is stored in asset resources.
 */
public class PreloadContactUtil {

    private static final String TAG = "PreloadContactUtil";

    /** The asset path, which contains the preload contacts information. */
    public static final String PRELOAD_CONTACT_ASSET_PATH = "preload/contacts.json";
    public static final String PRELOAD_CONTACT_FILE_PATH = "/system/etc/contacts.json";
    public static final String PRELOAD_CONTACT_PROP_KEY_NAME = "persist.contacts.preload";
    /**
     * The buffer size used to read asset resource.
     * The size for a vcard text with avatar info is about 10K,
     * so initial the output buffer to a bit larger than 3 contacts data.
     */
    public static final int ASSET_READ_BUF_SIZE = 32 * 1024;
    /** The preload contacts asset file must be encoded in UTF-8. */
    public static final String PRELOAD_CONTACT_ASSET_FILE_CHAR_SET = "UTF-8";

    /**
     * We check if the preload contacts are deleted by user every month.
     * Because the tester might test some feature and set date one month later,
     * so here we use a bit larger value to avoid confuse the tester.
     */
    private static final long CHECK_PRELOAD_CONTACT_CYCLE = 36 * 24 * 60 * 60 * 1000L;
    private static final String SHARED_PREF_KEY_LAST_CHECK_PRELOAD_CONTACT_TIME = "last_check_preload_contact_time";
    private static long mLastCheckPreloadContactTime = 0;

    /**
     * Check if the preload contacts are still in DB. If not, insert the preload contacts.
     * @param context The database operations will be performed via this context.
     * @param prefs The latest check time will be saved in the shared preferences.
     */
    public static void checkPreloadContact(Context context, SharedPreferences prefs) {
        boolean support = SystemProperties.getBoolean(PRELOAD_CONTACT_PROP_KEY_NAME, false);
        if(!support) {
            support =context.getResources().getBoolean(R.bool.config_support_preload_contact);
        }
        if (!support) {
            Log.i(TAG, "checkPreloadContact: not support.");
            return;
        }
        if (mLastCheckPreloadContactTime == 0) {
            mLastCheckPreloadContactTime
                    = prefs.getLong(SHARED_PREF_KEY_LAST_CHECK_PRELOAD_CONTACT_TIME, 0);
        }

        long now = System.currentTimeMillis();
        Log.d(TAG, "checkPreloadContact: last check at "+mLastCheckPreloadContactTime+"; now "+now);
        if (now - mLastCheckPreloadContactTime > CHECK_PRELOAD_CONTACT_CYCLE) {
            startCheck(context);
            PreferencesUtils.commitLongSharedPreferencesInBackground(
                    prefs, SHARED_PREF_KEY_LAST_CHECK_PRELOAD_CONTACT_TIME, now);
            mLastCheckPreloadContactTime = now;
        }
    }

    private static void startCheck(Context context) {
        new CheckPreloadContactThread(context).start();
    }

}
