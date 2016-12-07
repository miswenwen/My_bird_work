
package com.yunos.alicontacts.group;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.ContactSaveService;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.model.account.AccountType;

import java.util.Locale;

public final class GroupManager {
    private static final String TAG = "GroupManager";
    public static final String DEFAULT_GROUP_FLAG = "default_group";

    private static final String OLD_GROUP_NAME = "System Group: My AliContacts";

    private static final String PREFS_NAME_DEFAULT_GROUP = "com.yunos.alicontacts.default_group";
    private static final String PREFS_KEY_CREATED_DEFAULT_GROUP = "created_default_group";

    private static final String PREFS_KEY_LOCALE_FOR_GROUP_NAME = "locale_for_group_name";

    private static final String PREFS_KEY_GROUP_ID_FAMILY = "group_id_family";
    private static final String PREFS_KEY_GROUP_NAME_FAMILY = "group_name_family";

    private static final String PREFS_KEY_GROUP_ID_FRIEND = "group_id_friend";
    private static final String PREFS_KEY_GROUP_NAME_FRIEND = "group_name_friend";

    private static final String PREFS_KEY_GROUP_ID_COLLEAGUE = "group_id_colleague";
    private static final String PREFS_KEY_GROUP_NAME_COLLEAGUE = "group_name_colleague";

    private Context mContext;
    private SharedPreferences mDefaultGroupPrefs;
    private static GroupManager INSTANCE = null;

    private GroupManager(Context context) {
        mContext = context;
        mDefaultGroupPrefs = mContext.getSharedPreferences(
                PREFS_NAME_DEFAULT_GROUP, Context.MODE_PRIVATE);
    }

    public static synchronized GroupManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new GroupManager(context);
        }
        return INSTANCE;
    }

    public void checkDefaultGroupsOnInitial() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.i(TAG, "[LongTermTrackingLog] checkDefaultGroupsOnInitial");
                removeOldDefaultGroupForFOTA();
                if (!hasDefaultGroup()) {
                    createDefaultGroups();
                } else {
                    checkLocaleForDefaultGroups();
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private boolean hasDefaultGroup() {
        // If we have created default group, then we shall have the flag set in shared preferences.
        boolean createdDefaultGroup = mDefaultGroupPrefs.getBoolean(PREFS_KEY_CREATED_DEFAULT_GROUP, false);
        if (createdDefaultGroup) {
            return true;
        }

        // We don't find the flag in shared preferences, this might have 2 reasons:
        // 1. we really have not created default groups yet.
        // 2. the contacts app data has been cleared.
        // So we have to check the database in this case.
        Uri queryUri = Groups.CONTENT_URI
                .buildUpon()
                .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, String.valueOf(1))
                .build();
        final Cursor cursor = mContext.getContentResolver().query(
                queryUri,
                new String[] { Groups._ID },
                Groups.NOTES + "='"+ DEFAULT_GROUP_FLAG + "'",
                null,
                null);
        try {
            if (null != cursor && cursor.getCount() > 0) {
                return true;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        // TODO: Here we still have a logic hole:
        //       The 3rd party apps can delete the default groups,
        //       and we will consider the default groups have never been created.
        return false;
    }

    private synchronized void createDefaultGroups() {
        Log.i(TAG, "[LongTermTrackingLog] createDefaultGroups");
        ContentResolver resolver = mContext.getContentResolver();
        createDefaultGroup(
                resolver,
                mContext.getResources().getString(R.string.family_group_label),
                PREFS_KEY_GROUP_ID_FAMILY,
                PREFS_KEY_GROUP_NAME_FAMILY);
        createDefaultGroup(
                resolver,
                mContext.getResources().getString(R.string.friend_group_label),
                PREFS_KEY_GROUP_ID_FRIEND,
                PREFS_KEY_GROUP_NAME_FRIEND);
        createDefaultGroup(
                resolver,
                mContext.getResources().getString(R.string.colleague_group_label),
                PREFS_KEY_GROUP_ID_COLLEAGUE,
                PREFS_KEY_GROUP_NAME_COLLEAGUE);
        Editor editor = mDefaultGroupPrefs.edit();
        editor.putBoolean(PREFS_KEY_CREATED_DEFAULT_GROUP, true);
        editor.putString(PREFS_KEY_LOCALE_FOR_GROUP_NAME, getCurrentLocaleInfo());
        editor.commit();
    }

    private void createDefaultGroup(
            ContentResolver resolver,
            String groupName,
            String prefKeyId,
            String prefKeyName) {
        ContentValues values = new ContentValues(2);
        values.put(Groups.TITLE, groupName);
        values.put(Groups.NOTES, DEFAULT_GROUP_FLAG);
        values.put(Groups.GROUP_VISIBLE, 1);
        values.put(Groups.ACCOUNT_NAME, AccountType.LOCAL_ACCOUNT_NAME);
        values.put(Groups.ACCOUNT_TYPE, AccountType.LOCAL_ACCOUNT_TYPE);
        Uri groupUri = resolver.insert(Groups.CONTENT_URI, values);
        if (groupUri == null) {
            Log.e(TAG, "Couldn't create group with lable" + groupName);
            return;
        }
        Log.i(TAG, "[LongTermTrackingLog] createDefaultGroup for " + groupName);
        long groupId = Long.parseLong(groupUri.getLastPathSegment());
        Editor editor = mDefaultGroupPrefs.edit();
        editor.putLong(prefKeyId, groupId);
        editor.putString(prefKeyName, groupName);
        editor.commit();
    }

    private String getCurrentLocaleInfo() {
        Locale loc = Locale.getDefault();
        return loc.getLanguage()+loc.getCountry();
    }

    public synchronized void checkLocaleForDefaultGroups() {
        String currentLocale = getCurrentLocaleInfo();
        String dbGroupLocale = mDefaultGroupPrefs.getString(PREFS_KEY_LOCALE_FOR_GROUP_NAME, null);
        if (currentLocale.equals(dbGroupLocale)) {
            return;
        }
        ContentResolver resolver = mContext.getContentResolver();
        long familyGroupId = mDefaultGroupPrefs.getLong(PREFS_KEY_GROUP_ID_FAMILY, -1);
        String familyGroupNameOld = mDefaultGroupPrefs.getString(PREFS_KEY_GROUP_NAME_FAMILY, null);
        String familyGroupNameNew = mContext.getResources().getString(R.string.family_group_label);
        updateGroupNameForNewLocale(
                resolver,
                familyGroupId,
                familyGroupNameOld,
                familyGroupNameNew,
                PREFS_KEY_GROUP_NAME_FAMILY);

        long friendGroupId = mDefaultGroupPrefs.getLong(PREFS_KEY_GROUP_ID_FRIEND, -1);
        String friendGroupNameOld = mDefaultGroupPrefs.getString(PREFS_KEY_GROUP_NAME_FRIEND, null);
        String friendGroupNameNew = mContext.getResources().getString(R.string.friend_group_label);
        updateGroupNameForNewLocale(
                resolver,
                friendGroupId,
                friendGroupNameOld,
                friendGroupNameNew,
                PREFS_KEY_GROUP_NAME_FRIEND);

        long colleagueGroupId = mDefaultGroupPrefs.getLong(PREFS_KEY_GROUP_ID_COLLEAGUE, -1);
        String colleagueGroupNameOld = mDefaultGroupPrefs.getString(PREFS_KEY_GROUP_NAME_COLLEAGUE, null);
        String colleagueGroupNameNew = mContext.getResources().getString(R.string.colleague_group_label);
        updateGroupNameForNewLocale(
                resolver,
                colleagueGroupId,
                colleagueGroupNameOld,
                colleagueGroupNameNew,
                PREFS_KEY_GROUP_NAME_COLLEAGUE);

        Editor editor = mDefaultGroupPrefs.edit();
        editor.putString(PREFS_KEY_LOCALE_FOR_GROUP_NAME, currentLocale);
        editor.commit();
    }

    private void updateGroupNameForNewLocale(
            ContentResolver resolver,
            long groupId,
            String groupNameOld,
            String groupNameNew,
            String prefsKeyName) {
        if ((groupId == -1) || TextUtils.isEmpty(groupNameOld)) {
            Log.w(TAG, "updateGroupNameForNewLocale: invalid old value. groupId="+groupId+"; groupName="+groupNameOld);
            return;
        }
        ContentValues cv = new ContentValues(1);
        cv.put(Groups.TITLE, groupNameNew);
        String where =
                  Groups._ID + "=? AND "
                + Groups.TITLE + "=? AND "
                + Groups.NOTES + "=?";
        String[] selectionArgs = new String[] {
                String.valueOf(groupId),
                groupNameOld,
                DEFAULT_GROUP_FLAG
        };
        resolver.update(Groups.CONTENT_URI, cv, where, selectionArgs);
        Editor editor = mDefaultGroupPrefs.edit();
        editor.putString(prefsKeyName, groupNameNew);
        editor.commit();
    }

    private void removeOldDefaultGroupForFOTA() {
        // fota : rm "Old Default Group"
        String groupName = OLD_GROUP_NAME;
        final Cursor cursor = mContext.getContentResolver().query(Groups.CONTENT_URI, null,
                Groups.TITLE + "='" + groupName + "' and " + Groups.DELETED + "=0", null, null);
        try {
            if (cursor != null && cursor.moveToNext()) {
                long groupId = cursor.getLong(cursor.getColumnIndex(Groups._ID));
                Log.i(TAG, "[LongTermTrackingLog] removeOldDefaultGroupForFOTA: id=" + groupId);
                mContext.startService(ContactSaveService.createGroupDeletionIntent(mContext,
                        groupId));
            }
        } catch (Exception e) {
            Log.e(TAG, "delete old default group : " + e.getMessage(), e);
        } finally {
            if (null != cursor)
                cursor.close();
        }
        return;
    }

    public boolean hasGroup(String label) {
        String selection = Groups.DELETED + "=0 and " + Groups.TITLE + "=?";
        String[] selectionArgs = new String[1];
        selectionArgs[0] = label;
        final Cursor cursor = mContext.getContentResolver().query(
                Groups.CONTENT_URI, null, selection, selectionArgs,
                null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } finally {
            if (null != cursor)
                cursor.close();
        }
        return false;
    }

}
