
package com.yunos.alicontacts.database.tables;

public class CallerNumberTable {
    public static final String TABLE_CALLER_NUMBER = "caller_number";
    public static final String COLUMN_ID = "_id"; // INTEGER PRIMARY KEY autoincrement,
    public static final String COLUMN_FORMATTED_NUMBER = "formatted_number"; // TEXT, unique
    public static final String COLUMN_NAME = "name"; // TEXT,
    public static final String COLUMN_TAG_NAME = "tag_name"; // TEXT
    public static final String COLUMN_SHOP_ID = "shop_id"; // TEXT,
    public static final String COLUMN_SHOP_NAME = "shop_name"; // TEXT,
    public static final String COLUMN_SHOP_URL = "shop_url"; // TEXT,
    public static final String COLUMN_SHOP_LOGO_NAME = "shop_logo_name";// TEXT,
    public static final String COLUMN_LOC_PROVINCE = "loc_province"; // TEXT,
    public static final String COLUMN_LOC_AREA = "loc_area"; // TEXT,
    public static final String COLUMN_TIMESTAMP = "timestamp"; // LONG,
    public static final String COLUMN_PEROID = "period"; // LONG,
    public static final String COLUMN_MARKED_COUNT = "marked_count"; // INTEGER,
    public static final String COLUMN_INCOMING_COUNT = "incoming_count"; // INTEGER NOT NULL DEFAULT 0,
    public static final String COLUMN_SOURCE = "source"; // INTEGER,
    public static final String COLUMN_DATA1 = "data1"; // INTEGER,
    public static final String COLUMN_DATA2 = "data2"; // INTEGER,
    public static final String COLUMN_DATA3 = "data3"; // TEXT,
    public static final String COLUMN_DATA4 = "data4"; // TEXT,
    public static final String COLUMN_DATA5 = "data5"; // TEXT,

}
