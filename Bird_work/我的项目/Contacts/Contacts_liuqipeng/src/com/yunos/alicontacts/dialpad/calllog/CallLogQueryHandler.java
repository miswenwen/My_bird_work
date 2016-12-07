/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunos.alicontacts.dialpad.calllog;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.util.Log;

import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.sim.SimUtil;

import java.lang.ref.WeakReference;
import java.util.Arrays;

/** Handles asynchronous queries to the call log. */
public class CallLogQueryHandler extends Handler {

    private static final String TAG = "CallLogQueryHandler";

    public static final int QUERY_CALLS_ALL = 60;
    public static final int QUERY_CALLS_MISSED = 61;
    public static final int QUERY_CALLS_INCOMING = 62;
    public static final int QUERY_CALLS_OUTGOING = 63;
    public static final int QUERY_CALLS_SIM1 = 64;
    public static final int QUERY_CALLS_SIM2 = 65;
    public static final int QUERY_CALLS_NEW_MISSED = 66;

    private static final int QUERY_COMPLETE = 0;

    /**
     * Call type similar to Calls.INCOMING_TYPE used to specify all types
     * instead of one particular type.
     */
    public static final int CALL_TYPE_ALL = -1;

    private final WeakReference<Listener> mListener;

    private Handler mWorkerThreadHandler;
    private static Looper sLooper = null;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case QUERY_COMPLETE:
                    Cursor cursor = (Cursor) (msg.obj);
                    onQueryComplete(msg.arg1, null, cursor);
                    break;
                default:
                    Log.w(TAG, "handleMessage: unrecognized what "+msg.what);
                    break;
            }
        }
    };

    public CallLogQueryHandler(Listener listener) {
        synchronized (CallLogQueryHandler.class) {
            if (sLooper == null) {
                HandlerThread thread = new HandlerThread("CallLogQueryHandler");
                thread.start();

                sLooper = thread.getLooper();
            }
        }
        mWorkerThreadHandler = new Handler(sLooper);
        mListener = new WeakReference<Listener>(listener);
    }

    private void queryLocalCalls(int type) {
        Log.d(TAG, "queryLocalCalls: type = " + type);
        String where = null;
        String[] whereArgs = null;
        switch (type) {
            case QUERY_CALLS_ALL:
                break;
            case QUERY_CALLS_MISSED:
                where = Calls.TYPE + " = ?";
                whereArgs = new String[] {
                    Integer.toString(Calls.MISSED_TYPE)
                };
                break;
            case QUERY_CALLS_INCOMING:
                where = Calls.TYPE + " = ?";
                whereArgs = new String[] {
                    Integer.toString(Calls.INCOMING_TYPE)
                };
                break;
            case QUERY_CALLS_OUTGOING:
                where = Calls.TYPE + " = ?";
                whereArgs = new String[] {
                    Integer.toString(Calls.OUTGOING_TYPE)
                };
                break;
            // cmcc customization

            /* YUNOS BEGIN */
            //##modules(AliContacts) ##author: hongwei.zhw
            //##BugID:(8152701) ##date:2016.4.20
            //##descrpition: add the sim1/sim2 call log filter
            case QUERY_CALLS_SIM1:
                if (SimUtil.MULTISIM_ENABLE) {
                    where = CallerViewQuery.COLUMN_SIMID + " = ?";
                    int subId = SimUtil.getSubId(0);
                    whereArgs = new String[] {String.valueOf(subId)};

                }
                break;
            case QUERY_CALLS_SIM2:
                if (SimUtil.MULTISIM_ENABLE) {

                    where = CallerViewQuery.COLUMN_SIMID + " = ?";
                    int subId = SimUtil.getSubId(1);
                    whereArgs = new String[] {String.valueOf(subId)};
                }
                break;
            /* YUNOS END */
            /*case QUERY_CALLS_UNREAD:
                    where = CallerViewQuery.COLUMN_SIMID + " =?";
                    whereArgs = new String[] {
                        "1"
                    };
                }
                break;*/
            case QUERY_CALLS_NEW_MISSED:
                where = Calls.NEW + " = ? AND " + Calls.TYPE + " = ?";
                whereArgs = new String[] {
                    "1", Integer.toString(Calls.MISSED_TYPE)
                };
                break;
            default:
                Log.e(TAG, "queryLocalCalls: default error.");
                break;
        }
        AliCallLogExtensionHelper.log(TAG, "queryLocalCalls: where = " + where + " whereArgs = "
                + Arrays.toString(whereArgs));

        Cursor cursor = CallLogManager.getInstance(ActivityThread.currentApplication())
                .queryAliCalls(CallerViewQuery.getProjection(), where, whereArgs,
                        Calls.DEFAULT_SORT_ORDER);
        Message msg = mHandler.obtainMessage(QUERY_COMPLETE);
        msg.obj = cursor;
        msg.arg1 = type;
        msg.sendToTarget();
    }

    public void queryCalls(final int type) {
        if (mWorkerThreadHandler != null) {
            mWorkerThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    queryLocalCalls(type);
                }
            });
        }
    }

    protected synchronized void onQueryComplete(int token, Object cookie, Cursor cursor) {
        boolean cursorUsed = false;
        // added by xiaodong.lxd
        if (token == QUERY_CALLS_ALL || token == QUERY_CALLS_MISSED
                || token == QUERY_CALLS_INCOMING || token == QUERY_CALLS_OUTGOING
                || token == QUERY_CALLS_SIM1 || token == QUERY_CALLS_SIM2
                || token == QUERY_CALLS_NEW_MISSED) {
            cursorUsed |= updateAdapterData(cursor);
        } else {
            Log.w(TAG, "Unknown query completed: ignoring: " + token);
        }

        if (!cursorUsed && cursor != null) {
            cursor.close();
        }
    }

    /**
     * Updates the adapter in the call log fragment to show the new cursor data.
     *
     * @return If the combinedCursor is referred in this method. The caller is
     *         responsible for closing the unused cursor.
     */
    private boolean updateAdapterData(Cursor combinedCursor) {
        boolean used = false;
        final Listener listener = mListener.get();
        if (listener != null) {
            used |= listener.onCallsFetched(combinedCursor);
        }
        return used;
    }

    /** Listener to completion of various queries. */
    public interface Listener {

        /**
         * Called when {@link CallLogQueryHandler#fetchCalls(int)}complete.
         *
         * @return If the combinedCursor is referenced in this method. The
         *         caller is responsible for closing the unused cursor.
         */
        boolean onCallsFetched(Cursor combinedCursor);
    }
}
