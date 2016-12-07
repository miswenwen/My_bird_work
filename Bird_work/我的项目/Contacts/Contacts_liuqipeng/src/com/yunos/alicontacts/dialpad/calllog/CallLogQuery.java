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

import android.provider.CallLog.Calls;

import com.yunos.alicontacts.database.tables.CallsTable;
import com.yunos.alicontacts.platform.PDConstants;
import com.yunos.alicontacts.sim.SimUtil;

/**
 * <p>The query for the call log table.</p>
 * <p>IMPORTANT: The projection and column indexes are defined for calls table in ContactsProvider.
 */
public final class CallLogQuery {
    private static final String[] _PROJECTION = new String[] {
            Calls._ID, // 0
            Calls.NUMBER, // 1
            Calls.DATE, // 2
            Calls.DURATION, // 3
            Calls.TYPE, // 4
            Calls.FEATURES, // 5
            Calls.COUNTRY_ISO, // 6
            Calls.VOICEMAIL_URI, // 7
            Calls.GEOCODED_LOCATION, // 8
            Calls.CACHED_NAME, // 9
            Calls.CACHED_NUMBER_TYPE, // 10
            Calls.CACHED_NUMBER_LABEL, // 11
            Calls.CACHED_LOOKUP_URI, // 12
            Calls.CACHED_MATCHED_NUMBER, // 13
            Calls.CACHED_NORMALIZED_NUMBER, // 14
            Calls.CACHED_PHOTO_ID, // 15
            PDConstants.CALLS_TABLE_COLUMN_PHOTO_URI, // 16
            Calls.CACHED_FORMATTED_NUMBER, // 17
            Calls.IS_READ, // 18
            CallsTable.COLUMN_RING_TIME, // 19
            CallsTable.COLUMN_PHONE_RECORD_PATH, // 20
            CallsTable.COLUMN_NEW //21
    };

    private static final String[] _PROJECTION_MULIISIM = new String[] {
            Calls._ID, // 0
            Calls.NUMBER, // 1
            Calls.DATE, // 2
            Calls.DURATION, // 3
            Calls.TYPE, // 4
            Calls.FEATURES, // 5
            Calls.COUNTRY_ISO, // 6
            Calls.VOICEMAIL_URI, // 7
            Calls.GEOCODED_LOCATION, // 8
            Calls.CACHED_NAME, // 9
            Calls.CACHED_NUMBER_TYPE, // 10
            Calls.CACHED_NUMBER_LABEL, // 11
            Calls.CACHED_LOOKUP_URI, // 12
            Calls.CACHED_MATCHED_NUMBER, // 13
            Calls.CACHED_NORMALIZED_NUMBER, // 14
            Calls.CACHED_PHOTO_ID, // 15
            PDConstants.CALLS_TABLE_COLUMN_PHOTO_URI, // 16
            Calls.CACHED_FORMATTED_NUMBER, // 17
            Calls.IS_READ, // 18
            CallsTable.COLUMN_RING_TIME, // 19
            CallsTable.COLUMN_PHONE_RECORD_PATH, // 20
            CallsTable.COLUMN_NEW, // 21
            SimUtil.CALLS_TABLE_SUBSCRIPTION_COLUMN_NAME // 22
    };

    public static final int ID = 0;
    public static final int NUMBER = 1;
    public static final int DATE = 2;
    public static final int DURATION = 3;
    public static final int CALL_TYPE = 4;
    public static final int FEATURES = 5;
    public static final int COUNTRY_ISO = 6;
    public static final int VOICEMAIL_URI = 7;
    public static final int GEOCODED_LOCATION = 8;
    public static final int CACHED_NAME = 9;
    public static final int CACHED_NUMBER_TYPE = 10;
    public static final int CACHED_NUMBER_LABEL = 11;
    public static final int CACHED_LOOKUP_URI = 12;
    public static final int CACHED_MATCHED_NUMBER = 13;
    public static final int CACHED_NORMALIZED_NUMBER = 14;
    public static final int CACHED_PHOTO_ID = 15;
    public static final int CACHED_PHOTO_URI = 16;
    public static final int CACHED_FORMATTED_NUMBER = 17;
    public static final int IS_READ = 18;
    public static final int RING_TIME = 19;
    public static final int PHONE_RECORD_PATH = 20;
    public static final int NEW = 21;
    public static final int SUB_ID = 22;

    public static String[] getProjection() {
        if (SimUtil.MULTISIM_ENABLE) {
            return _PROJECTION_MULIISIM;
        } else {
            return _PROJECTION;
        }
    }
}
