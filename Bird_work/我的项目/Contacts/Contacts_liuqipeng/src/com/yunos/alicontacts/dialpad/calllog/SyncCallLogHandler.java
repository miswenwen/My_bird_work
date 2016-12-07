
package com.yunos.alicontacts.dialpad.calllog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.util.Log;

import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.database.CallLogManager.CallLogChangeListener;
import com.yunos.alicontacts.database.tables.CallsTable;
import com.yunos.alicontacts.database.util.NumberNormalizeUtil;
import com.yunos.alicontacts.platform.PDUtils;
import com.yunos.alicontacts.util.AliTextUtils;

import java.util.HashSet;

public final class SyncCallLogHandler extends Handler {
    private static final String TAG = "SyncCallLogHandler";

    public static final int CALL_LOG_COUNT_LIMIT = 500;
    public static final String CALL_LOG_DELETE_TO_LIMIT_SELECTION
            = CallLog.Calls._ID + " IN (SELECT " + CallLog.Calls._ID + " FROM calls "
                + "ORDER BY " + CallLog.Calls.DEFAULT_SORT_ORDER
                + " LIMIT -1 OFFSET " + CALL_LOG_COUNT_LIMIT + ")";

    public static final int WHAT_SYNC_CALL_LOG = 1;

    private Context mContext;
    private HandlerThread mSyncCallsThread = null;
    private CallLogManager mCallLogManager;

    private SyncCallLogHandler(Context context, HandlerThread thread) {
        super(thread.getLooper());
        this.mContext = context.getApplicationContext();
        this.mCallLogManager = CallLogManager.getInstance(mContext);
        this.mSyncCallsThread = thread;
    }

    public static SyncCallLogHandler createHandler(Context context) {
        HandlerThread thread = new HandlerThread("thread_sync");
        thread.start();
        return new SyncCallLogHandler(context, thread);
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handleMessage: what="+msg.what);
        switch (msg.what) {
            case WHAT_SYNC_CALL_LOG:
                // Maybe we have accumulated several sync requests here.
                // But actually we only need to do once sync here.
                // This is not UI thread, so I prefer to go through message
                // queue,
                // and delete the same messages here instead of at the time send
                // the message,
                // that thread might be UI thread.
                removeMessages(WHAT_SYNC_CALL_LOG);
                handleSyncCallLog();
                break;
            default:
                Log.w(TAG, "handleMessage: unsupported message: " + msg.what);
                break;
        }
    }

    public void destroy() {
        mSyncCallsThread.quit();
    }

    private void handleSyncCallLog() {
        Log.i(TAG, "handleSyncCallLog: start");
        Cursor callsCursor = null;
        Cursor aliCallsCursor = null;
        try {
            // Get all calls from ContactsProvider
            callsCursor = stripCallLogAndReadCalls();
            // Get all calls id cached in calls table.
            aliCallsCursor = mCallLogManager.queryAliCalls(CallerViewQuery.getProjection(), null,
                    null, CallsTable.COLUMN_ID + " DESC");

            if (callsCursor == null || callsCursor.getCount() == 0) {
                Log.i(TAG, "handleSyncCallLog: callsCursor is empty.");
                // calls is empty, need to clear cached calls
                // in calls table.
                if (aliCallsCursor != null && aliCallsCursor.getCount() > 0) {
                    mCallLogManager.clearCalls();
                }
            } else if (aliCallsCursor == null || aliCallsCursor.getCount() == 0) {
                Log.i(TAG, "handleSyncCallLog: aliCallsCursor is empty.");
                // ali calls is null, need to insert all
                // calls into cached calls table.
                mCallLogManager.insertCalls(callsCursor);
            } else {
                Log.i(TAG, "handleSyncCallLog: syncItemsWithCallsTableCachedCallsTable.");
                syncItemsWithCallsTableAliCallsTable(callsCursor, aliCallsCursor);
            }

            Log.i(TAG, "handleSyncCallLog: end.");
        } catch (Exception e) {
            Log.e(TAG, "handleSyncCallLog: got error.", e);
        } finally {
            if (callsCursor != null) {
                callsCursor.close();
            }
            if (aliCallsCursor != null) {
                aliCallsCursor.close();
            }
            Log.i(TAG, "handleSyncCallLog: end finally.");
        }
    }

    private Cursor stripCallLogAndReadCalls() {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        cursor = resolver.query(Calls.CONTENT_URI, CallLogQuery.getProjection(),
                null, null, Calls._ID + " DESC");
        int count = cursor == null ? -1 : cursor.getCount();
        Log.i(TAG, "stripCallLogAndReadCalls: call log count="+count);
        if (count > CALL_LOG_COUNT_LIMIT) {
            Log.w(TAG, "stripCallLogAndReadCalls: excceeds limit, delete exipred call logs.");
            cursor.close();
            resolver.delete(Calls.CONTENT_URI, CALL_LOG_DELETE_TO_LIMIT_SELECTION, null);
            cursor = resolver.query(Calls.CONTENT_URI, CallLogQuery.getProjection(),
                    null, null, Calls._ID + " DESC");
        }
        return cursor;
    }

    // sync calls table with ali calls table
    private void syncItemsWithCallsTableAliCallsTable(Cursor callsCursor, Cursor aliCallsCursor) {
        if (callsCursor == null || aliCallsCursor == null) {
            return;
        }

        int callsCursorCount = callsCursor.getCount();
        int aliCallsCursorCount = aliCallsCursor.getCount();
        Log.d(TAG, "[CALL]syncItemsWithCallsTableAliCallsTable: callsCursorCount = "
                + callsCursorCount + " aliCallsCursorCount = " + aliCallsCursorCount);

        callsCursor.moveToFirst();
        aliCallsCursor.moveToFirst();
        long callsId;
        long aliCallsId;
        int count = callsCursor.getCount();
        HashSet<String> numbersToAdd = new HashSet<String>(count);
        boolean callUpdated = false;
        while (!callsCursor.isAfterLast() && !aliCallsCursor.isAfterLast()) {
            callsId = callsCursor.getLong(CallLogQuery.ID);
            aliCallsId = aliCallsCursor.getLong(CallerViewQuery.ID);
            String callLogNum = NumberNormalizeUtil.normalizeNumber(
                    callsCursor.getString(CallLogQuery.NUMBER), true);
            if (!numbersToAdd.contains(callLogNum)) {
                numbersToAdd.add(callLogNum);
            }
            if (callsId == aliCallsId) {
                if (isNeedUpdateAliCallsTable(callsCursor, aliCallsCursor)) {
                    Log.d(TAG, "Need to insert, callId=" + callsId);
                    mCallLogManager.insertCall(callsCursor);
                    callUpdated = true;
                }
                callsCursor.moveToNext();
                aliCallsCursor.moveToNext();
            } else if (callsId > aliCallsId) {
                Log.d(TAG, "[CALL]syncItemsWithCallsTableAliCallsTable: insertCalls callsId = "
                        + callsId + " cachedCallsId = " + aliCallsId);
                // Insert the new item which is in calls table but not in cached
                // calls table.
                mCallLogManager.insertCall(callsCursor);
                callUpdated = true;
                callsCursor.moveToNext();
            } else {
                // remove the old call log item which is in cached calls
                // table but not in calls table.
                Log.d(TAG,
                        "[CALL]syncItemsWithCallsTableAliCallsTable: removeCalls cachedCallsId = "
                                + aliCallsId + " callsId = " + callsId);
                mCallLogManager.removeCall(aliCallsId);
                callUpdated = true;
                aliCallsCursor.moveToNext();
            }
        }
        // callsCursor has not been traversed to end, insert the rest new items
        // into ali calls table.
        while (!callsCursor.isAfterLast()) {
            callsId = callsCursor.getLong(0);
            Log.d(TAG, "[CALL]syncItemsWithCallsTableAliCallsTable: insertCalls callsId = "
                    + callsId);
            // Insert the new item which is in calls table but not in ali calls
            // table.
            mCallLogManager.insertCall(callsCursor);
            String callLogNum = NumberNormalizeUtil.normalizeNumber(
                    callsCursor.getString(CallLogQuery.NUMBER), true);
            if (!numbersToAdd.contains(callLogNum)) {
                numbersToAdd.add(callLogNum);// add new number info.
            }
            callsCursor.moveToNext();
        }

        // aliCallsCursor has not been traversed to end, remove the rest new
        // items
        // from ali calls table.
        while (!aliCallsCursor.isAfterLast()) {
            aliCallsId = aliCallsCursor.getLong(0);
            Log.d(TAG, "[CALL]syncItemsWithCallsTableAliCallsTable: removeCalls cachedCallsId = "
                    + aliCallsId);
            // Insert the new item which is in calls table but not in cached
            // calls table.
            mCallLogManager.removeCall(aliCallsId);
            aliCallsCursor.moveToNext();
        }
        boolean callerNumberUpdated = mCallLogManager.updateCallerNumber(numbersToAdd);
        int notifyFlag = 0;
        if (callUpdated) {
            notifyFlag |= CallLogChangeListener.CHANGE_PART_CALL_LOG;
        }
        if (callerNumberUpdated) {
            notifyFlag |= CallLogChangeListener.CHANGE_PART_NUMBER_INFO;
        }
        if (notifyFlag != 0) {
            mCallLogManager.notifyCallsTableChange(notifyFlag);
        }
    }

    private boolean isNeedUpdateAliCallsTable(Cursor callsCursor, Cursor aliCallsCursor) {
        // new state changes
        if (aliCallsCursor.getInt(CallerViewQuery.NEW) !=
                callsCursor.getInt(CallLogQuery.NEW)) {
            return true;
        }

        // If call time does not exist, it means this record need sync
        if (aliCallsCursor.getLong(CallerViewQuery.DATE) != callsCursor.getLong(CallLogQuery.DATE)) {
            return true;
        }

        // Contact lookup Uri changes
        if (!AliTextUtils.equalsLoosely(aliCallsCursor.getString(CallerViewQuery.LOOKUP_URI),
                callsCursor.getString(CallLogQuery.CACHED_LOOKUP_URI))) {
            return true;
        }

        // phone record path changes
        if (!AliTextUtils.equalsLoosely(aliCallsCursor.getString(CallerViewQuery.PHONE_RECORD_PATH),
                callsCursor.getString(CallLogQuery.PHONE_RECORD_PATH))) {
            return true;
        }

        // photo uri changes
        if ((Build.VERSION.SDK_INT >= 23)
                && (!AliTextUtils.equalsLoosely(aliCallsCursor.getString(CallerViewQuery.PHOTO_URI),
                        PDUtils.getPhotoUriFromCallLogQuery(callsCursor)))) {
            return true;
        }

        // photo id changes
        if (aliCallsCursor.getLong(CallerViewQuery.PHOTO_ID) !=
                callsCursor.getLong(CallLogQuery.CACHED_PHOTO_ID)) {
            return true;
        }

        // Contact name changes
        if (!AliTextUtils.equalsLoosely(aliCallsCursor.getString(CallerViewQuery.NAME),
                callsCursor.getString(CallLogQuery.CACHED_NAME))) {
            return true;
        }

        // number changes
        if (!AliTextUtils.equalsLoosely(aliCallsCursor.getString(CallerViewQuery.NUMBER),
                callsCursor.getString(CallLogQuery.NUMBER))) {
            return true;
        }

        // phone number type changes
        if (aliCallsCursor.getInt(CallerViewQuery.NUMBER_TYPE) !=
                callsCursor.getInt(CallLogQuery.CACHED_NUMBER_TYPE)) {
            return true;
        }

        // phone number label changes
        if (aliCallsCursor.getInt(CallerViewQuery.NUMBER_LABEL) !=
                callsCursor.getInt(CallLogQuery.CACHED_NUMBER_LABEL)) {
            return true;
        }

        // phone number normalized changes
        if (aliCallsCursor.getInt(CallerViewQuery.NORMALIZED_NUM) !=
                callsCursor.getInt(CallLogQuery.CACHED_NORMALIZED_NUMBER)) {
            return true;
        }

        // phone ring time changes
        if (aliCallsCursor.getInt(CallerViewQuery.RING_TIME) !=
                callsCursor.getInt(CallLogQuery.RING_TIME)) {
            return true;
        }

        return false;
    }

}
