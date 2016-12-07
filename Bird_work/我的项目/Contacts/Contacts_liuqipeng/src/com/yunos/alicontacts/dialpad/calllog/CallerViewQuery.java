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

import com.yunos.alicontacts.database.tables.CallerNumberTable;
import com.yunos.alicontacts.database.tables.CallsTable;
import com.yunos.alicontacts.sim.SimUtil;

/**
 * The query for the call log table.
 */
public final class CallerViewQuery {
    public static final String TABLE_VIEW_CALLER = "view_caller";

    // Constants for VoLTE calls features.
    public static final int CALL_FEATURES_BIT_VIDEO = 0x01;
    public static final int CALL_FEATURES_BIT_HD = 0x02;

    // calls column
    public static final String CALL_ID = CallsTable.COLUMN_ID;
    public static final String COLUMN_NUMBER = CallsTable.COLUMN_NUMBER;
    public static final String COLUMN_DATE = CallsTable.COLUMN_DATE;
    public static final String COLUMN_DURATION = CallsTable.COLUMN_DURATION;
    public static final String COLUMN_TYPE = CallsTable.COLUMN_TYPE;
    public static final String COLUMN_FEATURES = CallsTable.COLUMN_FEATURES;
    public static final String COLUMN_COUNTRY_ISO = CallsTable.COLUMN_COUNTRY_ISO;
    public static final String COLUMN_VOICEMAIL_URI = CallsTable.COLUMN_VOICEMAIL_URI;
    public static final String COLUMN_GEO_LOCATION = CallsTable.COLUMN_LOCATION;
    public static final String COLUMN_NAME = CallsTable.COLUMN_NAME;
    public static final String COLUMN_NUMBER_TYPE = CallsTable.COLUMN_NUMBER_TYPE;
    public static final String COLUMN_NUMBER_LABEL = CallsTable.COLUMN_NUMBER_LABEL;
    public static final String COLUMN_LOOKUP_URI = CallsTable.COLUMN_LOOKUP_URI;
    public static final String COLUMN_MATCHED_NUM = CallsTable.COLUMN_MATCHED_NUM;
    public static final String COLUMN_NORMALIZED_NUM = CallsTable.COLUMN_NORMALIZED_NUM;
    public static final String COLUMN_PHOTO_ID = CallsTable.COLUMN_PHOTO_ID;
    public static final String COLUMN_PHOTO_URI = CallsTable.COLUMN_PHOTO_URI;
    public static final String COLUMN_FORMATTED_NUM = CallsTable.COLUMN_FORMATTED_NUM;
    public static final String COLUMN_IS_READ = CallsTable.COLUMN_IS_READ;
    public static final String COLUMN_RING_TIME = CallsTable.COLUMN_RING_TIME;
    public static final String COLUMN_PHONE_RECORD_PATH = CallsTable.COLUMN_PHONE_RECORD_PATH;
    public static final String COLUMN_PHONE_RECORD_COMMENTS = CallsTable.COLUMN_PHONE_RECORD_COMMENTS;
    public static final String COLUMN_ALI_CALLTYPE = CallsTable.COLUMN_ALI_CALLTYPE;
    public static final String COLUMN_NEW = CallsTable.COLUMN_NEW;
    // caller_number column
    public static final String COLUMN_LOC_PROVINCE = CallerNumberTable.COLUMN_LOC_PROVINCE;
    public static final String COLUMN_LOC_AREA = CallerNumberTable.COLUMN_LOC_AREA;
    public static final String COLUMN_SHOP_NAME = CallerNumberTable.COLUMN_SHOP_NAME;
    public static final String COLUMN_TAG_NAME = CallerNumberTable.COLUMN_TAG_NAME;
    public static final String COLUMN_MARKED_COUNT = CallerNumberTable.COLUMN_MARKED_COUNT;
    public static final String COLUMN_SIMID = CallsTable.COLUMN_SIMID;
    public static final String COLUMN_ICCID = CallsTable.COLUMN_ICCID;

    public static final int ID = 0;
    public static final int NUMBER = 1;
    public static final int DATE = 2;
    public static final int DURATION = 3;
    public static final int TYPE = 4;
    public static final int FEATURES = 5;
    public static final int COUNTRY_ISO = 6;
    public static final int VOICEMAIL_URI = 7;
    public static final int LOCATION = 8;
    public static final int NAME = 9;
    public static final int NUMBER_TYPE = 10;
    public static final int NUMBER_LABEL = 11;
    public static final int LOOKUP_URI = 12;
    public static final int MATCHED_NUM = 13;
    public static final int NORMALIZED_NUM = 14;
    public static final int PHOTO_ID = 15;
    public static final int PHOTO_URI = 16;
    public static final int FORMATTED_NUM = 17;
    public static final int IS_READ = 18;
    public static final int RING_TIME = 19;
    public static final int PHONE_RECORD_PATH = 20;
    public static final int PHONE_RECORD_COMMENTS = 21;
    public static final int ALI_CALLTYPE = 22;
    public static final int NEW = 23;
    // caller_number column
    public static final int LOC_PROVINCE = 24;
    public static final int LOC_AREA = 25;
    public static final int SHOP_NAME = 26;
    public static final int TAG_NAME = 27;
    public static final int MARKED_COUNT = 28;
    public static final int SIMID = 29;
    public static final int ICCID = 30;

    public static final String[] _PROJECTION = new String[] {
        CALL_ID,
        COLUMN_NUMBER,
        COLUMN_DATE,
        COLUMN_DURATION,
        COLUMN_TYPE,
        COLUMN_FEATURES,
        COLUMN_COUNTRY_ISO,
        COLUMN_VOICEMAIL_URI,
        COLUMN_GEO_LOCATION,
        COLUMN_NAME,
        COLUMN_NUMBER_TYPE,
        COLUMN_NUMBER_LABEL,
        COLUMN_LOOKUP_URI,
        COLUMN_MATCHED_NUM,
        COLUMN_NORMALIZED_NUM,
        COLUMN_PHOTO_ID,
        COLUMN_PHOTO_URI,
        COLUMN_FORMATTED_NUM,
        COLUMN_IS_READ,
        COLUMN_RING_TIME,
        COLUMN_PHONE_RECORD_PATH,
        COLUMN_PHONE_RECORD_COMMENTS,
        COLUMN_ALI_CALLTYPE,
        COLUMN_NEW,
        // caller_number column
        COLUMN_LOC_PROVINCE,
        COLUMN_LOC_AREA,
        COLUMN_SHOP_NAME,
        COLUMN_TAG_NAME,
        COLUMN_MARKED_COUNT
    };

    public static final String[] _PROJECTION_MULIISIM = new String[] {
        CALL_ID,
        COLUMN_NUMBER,
        COLUMN_DATE,
        COLUMN_DURATION,
        COLUMN_TYPE,
        COLUMN_FEATURES,
        COLUMN_COUNTRY_ISO,
        COLUMN_VOICEMAIL_URI,
        COLUMN_GEO_LOCATION,
        COLUMN_NAME,
        COLUMN_NUMBER_TYPE,
        COLUMN_NUMBER_LABEL,
        COLUMN_LOOKUP_URI,
        COLUMN_MATCHED_NUM,
        COLUMN_NORMALIZED_NUM,
        COLUMN_PHOTO_ID,
        COLUMN_PHOTO_URI,
        COLUMN_FORMATTED_NUM,
        COLUMN_IS_READ,
        COLUMN_RING_TIME,
        COLUMN_PHONE_RECORD_PATH,
        COLUMN_PHONE_RECORD_COMMENTS,
        COLUMN_ALI_CALLTYPE,
        COLUMN_NEW,
        // caller_number column
        COLUMN_LOC_PROVINCE,
        COLUMN_LOC_AREA,
        COLUMN_SHOP_NAME,
        COLUMN_TAG_NAME,
        COLUMN_MARKED_COUNT,
        COLUMN_SIMID,
        COLUMN_ICCID
    };

    public static String[] getProjection() {
        if (SimUtil.MULTISIM_ENABLE) {
            return _PROJECTION_MULIISIM;
        } else {
            return _PROJECTION;
        }
    }
}