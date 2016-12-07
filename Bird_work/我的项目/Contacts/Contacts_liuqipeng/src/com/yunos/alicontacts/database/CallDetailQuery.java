package com.yunos.alicontacts.database;

import com.yunos.alicontacts.dialpad.calllog.CallerViewQuery;

public class CallDetailQuery {
    public static final String[] CALL_LOG_PROJECTION = new String[] {
        CallerViewQuery.CALL_ID,
        CallerViewQuery.COLUMN_DATE,
        CallerViewQuery.COLUMN_DURATION,
        CallerViewQuery.COLUMN_NUMBER,
        CallerViewQuery.COLUMN_NORMALIZED_NUM,
        CallerViewQuery.COLUMN_FORMATTED_NUM,
        CallerViewQuery.COLUMN_TYPE,
        CallerViewQuery.COLUMN_FEATURES,
        CallerViewQuery.COLUMN_COUNTRY_ISO,
        CallerViewQuery.COLUMN_NAME,
        CallerViewQuery.COLUMN_LOOKUP_URI,
        CallerViewQuery.COLUMN_MATCHED_NUM,
        CallerViewQuery.COLUMN_NUMBER_TYPE,
        CallerViewQuery.COLUMN_NUMBER_LABEL,
        CallerViewQuery.COLUMN_PHOTO_ID,
        CallerViewQuery.COLUMN_PHOTO_URI,
        CallerViewQuery.COLUMN_LOC_PROVINCE,
        CallerViewQuery.COLUMN_LOC_AREA,
        CallerViewQuery.COLUMN_TAG_NAME,
        CallerViewQuery.COLUMN_SHOP_NAME,
        CallerViewQuery.COLUMN_RING_TIME,
        CallerViewQuery.COLUMN_MARKED_COUNT,
        CallerViewQuery.COLUMN_PHONE_RECORD_PATH
    };

    public static final String[] CALL_LOG_PROJECTION_MULTISIM = new String[] {
        CallerViewQuery.CALL_ID,
        CallerViewQuery.COLUMN_DATE,
        CallerViewQuery.COLUMN_DURATION,
        CallerViewQuery.COLUMN_NUMBER,
        CallerViewQuery.COLUMN_NORMALIZED_NUM,
        CallerViewQuery.COLUMN_FORMATTED_NUM,
        CallerViewQuery.COLUMN_TYPE,
        CallerViewQuery.COLUMN_FEATURES,
        CallerViewQuery.COLUMN_COUNTRY_ISO,
        CallerViewQuery.COLUMN_NAME,
        CallerViewQuery.COLUMN_LOOKUP_URI,
        CallerViewQuery.COLUMN_MATCHED_NUM,
        CallerViewQuery.COLUMN_NUMBER_TYPE,
        CallerViewQuery.COLUMN_NUMBER_LABEL,
        CallerViewQuery.COLUMN_PHOTO_ID,
        CallerViewQuery.COLUMN_PHOTO_URI,
        CallerViewQuery.COLUMN_LOC_PROVINCE,
        CallerViewQuery.COLUMN_LOC_AREA,
        CallerViewQuery.COLUMN_TAG_NAME,
        CallerViewQuery.COLUMN_SHOP_NAME,
        CallerViewQuery.COLUMN_RING_TIME,
        CallerViewQuery.COLUMN_MARKED_COUNT,
        CallerViewQuery.COLUMN_PHONE_RECORD_PATH,
        CallerViewQuery.COLUMN_SIMID
    };

    public static final int ID_COLUMN_INDEX = 0;
    public static final int DATE_COLUMN_INDEX = 1;
    public static final int DURATION_COLUMN_INDEX = 2;
    public static final int NUMBER_COLUMN_INDEX = 3;
    public static final int NORMALIZED_NUM_COLUMN_INDEX = 4;
    public static final int FORMATTED_NUM_COLUMN_INDEX = 5;
    public static final int CALL_TYPE_COLUMN_INDEX = 6;
    public static final int CALL_FEATURES_COLUMN_INDEX = 7;
    public static final int COUNTRY_ISO_COLUMN_INDEX = 8;
    public static final int CONTACT_NAME_COLUMN_INDEX = 9;
    public static final int CONTACT_LOOKUP_URI_COLUMN_INDEX = 10;
    public static final int CONTACT_MATCHED_NUM_COLUMN_INDEX = 11;
    public static final int CONTACT_NUMBER_TYPE_COLUMN_INDEX = 12;
    public static final int CONTACT_NUMBER_LABEL_COLUMN_INDEX = 13;
    public static final int CONTACT_PHOTO_ID_COLUMN_INDEX = 14;
    public static final int CONTACT_PHOTO_URI_COLUMN_INDEX = 15;
    public static final int LOC_PROVINCE_COLUMN_INDEX = 16;
    public static final int LOC_AREA_COLUMN_INDEX = 17;
    public static final int TAG_NAME_COLUMN_INDEX = 18;
    public static final int YP_NAME_COLUMN_INDEX = 19;
    public static final int RING_TIME_COLUMN_INDEX = 20;
    public static final int MARKED_COUNT_COLUMN_INDEX = 21;
    public static final int RECORD_PATH_COLUMN_INDEX = 22;
    public static final int SUB_COLUMN_INDEX = 23;

}
