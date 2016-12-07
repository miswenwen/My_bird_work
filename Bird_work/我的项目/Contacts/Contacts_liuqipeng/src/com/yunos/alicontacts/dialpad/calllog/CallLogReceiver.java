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
 * limitations under the License
 */

package com.yunos.alicontacts.dialpad.calllog;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.VoicemailContract;
import android.telecom.TelecomManager;
import android.util.Log;

/**
 * Receiver for call log events.
 * <p>
 * It is currently used to handle {@link VoicemailContract#ACTION_NEW_VOICEMAIL} and
 * {@link Intent#ACTION_BOOT_COMPLETED}.
 */
public class CallLogReceiver extends BroadcastReceiver {
    private static final String TAG = "CallLogReceiver";
    public static final String ACTION_CLEAR_ALL_MISSED_CALLS = "com.yunos.intent.action.CLEAR_ALL_MISSED_CALLS";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "onReceive: action="+action);
        if (ACTION_CLEAR_ALL_MISSED_CALLS.equals(action)) {
            handleClearAllMissedCalls(context, intent);
        } else {
            Log.w(TAG, "onReceive: could not handle: " + intent);
        }
    }

    private void handleClearAllMissedCalls(final Context context, Intent intent) {
        Log.d(TAG, "CallLogReceiver.handleClearAllMissedCalls: ");
        if (!AliCallLogExtensionHelper.PLATFORM_YUNOS) {
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                cancelMissedCallNotifications(context);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    @SuppressLint("InlinedApi") // for Context.TELECOM_SERVICE
    private void cancelMissedCallNotifications(Context context) {
        TelecomManager tm
                = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if (tm == null) {
            Log.e(TAG, "cancelMissedCallNotifications: can not get TelecomManager from context "+context);
            return;
        }
        // NOTE: Keep these logs, we had a bug that reported:
        // the missed call count on the corner of dial icon was dismissed after about 1 minute.
        Log.i(TAG, "cancelMissedCallNotifications: after cleared new flag in db, sending notification.");
        tm.cancelMissedCallsNotification();
        Log.i(TAG, "cancelMissedCallNotifications: sent cancel missed calls notification.");
    }

}
