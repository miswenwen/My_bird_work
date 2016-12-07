package com.android.settings.fingerprint;

import android.net.Uri;
import android.os.SystemProperties;
import android.database.Cursor;
import android.content.Context;
import android.util.Log;
import java.util.List;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;

public class AliFingerprintUtils {
    private static final String TAG = "AliFingerprintUtils";
	
    public static final int FINGERPRINT_OPEN_UNLOCK = 1;
    public static final int FINGERPRINT_CLOSE_UNLOCK = 0;

    public static final int FINGERQUICK_TYPE_DISABLE = 2;
    public static final int FINGERQUICK_TYPE_DIALOUT = 0;
    public static final int FINGERQUICK_TYPE_STARTAPP = 1;
    public static final int FINGERQUICK_TYPE_FINGERPAY = 3;

    public static final String COLUMN_FINGER_ID = "fingerid";
    public static final String COLUMN_OPERATION = "operation";
    public static final String COLUMN_TARGET = "target";
    public static final String COLUMN_DATA = "data";

    private static final Uri FINGERQUICK_URI = Uri.parse("content://authentication.information/fingerquick");

    public static Cursor getFingerQuickSetting(Context context, int fingerid) {
        String selection = COLUMN_FINGER_ID+ "=?";
        String[] selectionArgs = new String[]{String.valueOf(getEncryptId(fingerid))};
        Cursor cursor = context.getContentResolver().query(FINGERQUICK_URI, null, selection, selectionArgs, null);
        if(cursor == null) {
            return null;
        } else {
            if(cursor.getCount() <= 0) {
                cursor.close();
                return null;
            }
            if(cursor.getCount() > 1) {
                Log.e(TAG, "cursor count is greater than 1");
            }
            cursor.moveToFirst();
            return cursor;
        }
    }

    public static Cursor getFingerQuickSettingList(Context context) {
        Cursor cursor = context.getContentResolver().query(FINGERQUICK_URI, null, null, null, null);
        if(cursor == null) {
            return null;
        } else {
            if(cursor.getCount() <= 0) {
                cursor.close();
                return null;
            }
            if(cursor.getCount() > 1) {
                Log.e(TAG, "cursor count is " + cursor.getCount());
            }
            cursor.moveToFirst();
            return cursor;
        }
    }

    public static void setFingerQuickSetting(Context context, int fingerid, int operation, String target, String data) {
        boolean insert = true;
        String selection = COLUMN_FINGER_ID+ "=?";
        String[] selectionArgs = new String[]{String.valueOf(getEncryptId(fingerid))};

        ContentValues values = new ContentValues();
        values.put(COLUMN_FINGER_ID, getEncryptId(fingerid));
        values.put(COLUMN_OPERATION, operation);
        values.put(COLUMN_TARGET, target);
        values.put(COLUMN_DATA, data);

        Cursor cursor = getFingerQuickSetting(context, fingerid);
        if(cursor != null) {
            if(cursor.getCount() > 1) {
                //delete
                int count = context.getContentResolver().delete(FINGERQUICK_URI, selection, selectionArgs);
                Log.e(TAG, "delete count is " + count);
                insert = true;
            } else {
                //update
                int count = context.getContentResolver().update(FINGERQUICK_URI, values, selection, selectionArgs);
                Log.e(TAG, "update count is " + count);
                insert = false;
            }
            cursor.close();
        }

        if(insert) {
            Uri uri= context.getContentResolver().insert(FINGERQUICK_URI, values);
            if(uri == null) {
                Log.e(TAG, "insert into fingerquick return null!");
            } else {
                Log.e(TAG, "insert into fingerquick return uri:" + uri);
            }
        }
    }
    public static int deleteFingerQuickSetting(Context context, int fingerid) {
        String selection = COLUMN_FINGER_ID+ "=?";
        String[] selectionArgs = new String[]{String.valueOf(getEncryptId(fingerid))};
        int count = context.getContentResolver().delete(FINGERQUICK_URI, selection, selectionArgs);
        Log.e(TAG, "delete count is " + count);
        return count;
    }
    public static int CheckAndDeleteFingerquickSettingByPackageName(Context context, String pn) {
        String selection = COLUMN_TARGET+ "=?";
        String[] selectionArgs = new String[]{pn};
        int count = context.getContentResolver().delete(FINGERQUICK_URI, selection, selectionArgs);
        Log.e(TAG, "delete count is " + count + " for packagename:" + pn);
        return count;
    }

    public static String getApplicationName(Context context, String packageName, String activityName) {
        String appName = null;
        try {
            final PackageManager pm = context.getPackageManager();
            if(activityName == null || activityName.isEmpty()) {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                appName= (String) pm.getApplicationLabel(ai);
            } else {
                ActivityInfo ai = pm.getActivityInfo(new ComponentName(packageName, activityName), 0);
                appName= ai.loadLabel(pm).toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            appName = null;
        }
        return appName;
    }
    public static Drawable getApplicationIcon(Context context, String packageName, String activityName) {
        Drawable appIcon = null;
        try {
            final PackageManager pm = context.getPackageManager();
            if(activityName == null || activityName.isEmpty()) {
                appIcon= pm.getApplicationIcon(packageName);
            } else {
                appIcon= pm.getActivityIcon(new ComponentName(packageName, activityName));
            }
        } catch (PackageManager.NameNotFoundException e) {
            appIcon = null;
        }
        return appIcon;
    }

    public static List<ResolveInfo> getAllPackages(PackageManager pkgm){
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = pkgm.queryIntentActivities(mainIntent,0);
        return apps;
    }

     public static String getContactNameByPhoneNumber(Context context, String address) {
         String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME,
                 ContactsContract.CommonDataKinds.Phone.NUMBER };

         Cursor cursor = context.getContentResolver().query(
                 ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                 projection, // Which columns to return.
                 ContactsContract.CommonDataKinds.Phone.NUMBER + " = '"
                         + address + "'", // WHERE clause.
                 null, // WHERE clause value substitution
                 null); // Sort order.

         if (cursor == null) {
             Log.d(TAG, "get People null for number:" + address);
             return null;
         }
         String name = null;
         for (int i = 0; i < cursor.getCount(); i++) {
             cursor.moveToPosition(i);
             int nameFieldColumnIndex = cursor
                     .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
             name = cursor.getString(nameFieldColumnIndex);
         }
         if(null != cursor){
             cursor.close();
         }
         return name ;
     }

     private static int leftShiftOp(int value, int len) {
        return (value << len ) | (value >>> (Integer.SIZE - len));
     }
     public static int getEncryptId(int origin) {
        return leftShiftOp(origin, 5) ^ 0x5a5a5a5a;
     }
     public static int getDecryptyId(int enId) {
        return  leftShiftOp(enId ^ 0x5a5a5a5a, Integer.SIZE - 5);
     }
}

