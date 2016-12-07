package com.yunos.alicontacts.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.database.tables.CallerNumberTable;
import com.yunos.alicontacts.database.tables.CallsTable;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class AliContactsDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "AliContactsDatabaseHelper";

    // database definition begin.
    private static final String CREATE_CALLS_TABLE =
            "CREATE TABLE " + CallsTable.TABLE_CALLS + " ("
            + CallsTable.COLUMN_ID + " INTEGER NOT NULL,"
            + CallsTable.COLUMN_NUMBER + " TEXT,"
            + CallsTable.COLUMN_PRES + " INTEGER NOT NULL DEFAULT 1,"
            + CallsTable.COLUMN_DATE + " INTEGER,"
            + CallsTable.COLUMN_DURATION + " INTEGER,"
            + CallsTable.COLUMN_TYPE + " INTEGER,"
            + CallsTable.COLUMN_FEATURES + " INTEGER NOT NULL DEFAULT 0,"
            + CallsTable.COLUMN_NEW + " INTEGER,"
            + CallsTable.COLUMN_NAME + " TEXT,"
            + CallsTable.COLUMN_NUMBER_TYPE + " INTEGER,"
            + CallsTable.COLUMN_NUMBER_LABEL + " TEXT,"
            + CallsTable.COLUMN_COUNTRY_ISO + " TEXT,"
            + CallsTable.COLUMN_VOICEMAIL_URI + " TEXT,"
            + CallsTable.COLUMN_IS_READ + " INTEGER,"
            + CallsTable.COLUMN_LOCATION + " TEXT,"
            + CallsTable.COLUMN_LOOKUP_URI + " TEXT,"
            + CallsTable.COLUMN_MATCHED_NUM + " TEXT,"
            + CallsTable.COLUMN_NORMALIZED_NUM + " TEXT,"
            + CallsTable.COLUMN_PHOTO_ID + " INTEGER NOT NULL DEFAULT 0,"
            + CallsTable.COLUMN_PHOTO_URI + " TEXT,"
            + CallsTable.COLUMN_FORMATTED_NUM + " TEXT,"
            + CallsTable.COLUMN_DATA + " TEXT,"
            + CallsTable.COLUMN_HAS_CONTENT + " INTEGER,"
            + CallsTable.COLUMN_MIME_TYPE + " TEXT,"
            + CallsTable.COLUMN_SOURCE_DATA + " TEXT,"
            + CallsTable.COLUMN_SOURCE_PACKAGE + " TEXT,"
            + CallsTable.COLUMN_STATE + " INTEGER,"
            + CallsTable.COLUMN_SIMID + " INTEGER,"
            + CallsTable.COLUMN_RING_TIME + " INTEGER NOT NULL DEFAULT 1,"
            + CallsTable.COLUMN_PHONE_RECORD_PATH + " TEXT,"
            + CallsTable.COLUMN_PHONE_RECORD_COMMENTS + " TEXT,"
            + CallsTable.COLUMN_ICCID + " TEXT,"
            + CallsTable.COLUMN_ALI_CALLTYPE + " INTEGER,"
            + CallsTable.COLUMN_DATA1 + " INTEGER,"
            + CallsTable.COLUMN_DATA2 + " TEXT,"
            + "PRIMARY KEY (" + CallsTable.COLUMN_ID + " ASC));";

    private static final String CREATE_CALLER_NUMBER_TABLE =
            "CREATE TABLE " + CallerNumberTable.TABLE_CALLER_NUMBER + " ("
            + CallerNumberTable.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + CallerNumberTable.COLUMN_FORMATTED_NUMBER + " TEXT,"
            + CallerNumberTable.COLUMN_NAME + " TEXT,"
            + CallerNumberTable.COLUMN_TAG_NAME + " TEXT,"
            + CallerNumberTable.COLUMN_SHOP_ID + " INTEGER,"
            + CallerNumberTable.COLUMN_SHOP_NAME + " TEXT,"
            + CallerNumberTable.COLUMN_SHOP_URL + " TEXT,"
            + CallerNumberTable.COLUMN_SHOP_LOGO_NAME + " TEXT,"
            + CallerNumberTable.COLUMN_LOC_PROVINCE + " TEXT,"
            + CallerNumberTable.COLUMN_LOC_AREA + " TEXT,"
            + CallerNumberTable.COLUMN_TIMESTAMP + " INTEGER,"
            + CallerNumberTable.COLUMN_PEROID + " INTEGER,"
            + CallerNumberTable.COLUMN_MARKED_COUNT + " INTEGER,"
            + CallerNumberTable.COLUMN_INCOMING_COUNT + " INTEGER NOT NULL DEFAULT 0,"
            + CallerNumberTable.COLUMN_SOURCE + " INTEGER,"
            + CallerNumberTable.COLUMN_DATA1 + " INTEGER,"
            + CallerNumberTable.COLUMN_DATA2 + " INTEGER,"
            + CallerNumberTable.COLUMN_DATA3 + " TEXT,"
            + CallerNumberTable.COLUMN_DATA4 + " TEXT,"
            + CallerNumberTable.COLUMN_DATA5 + " TEXT,"
            + "UNIQUE (" + CallerNumberTable.COLUMN_FORMATTED_NUMBER + " ASC));";

    private static final String CREATE_CALLER_VIEW_TABLE =
            "CREATE VIEW " + Views.CALLER + " AS SELECT "
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_ID
                + " AS " + CallerViewColumns.CALL_ID + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_NUMBER
                + " AS " + CallerViewColumns.COLUMN_NUMBER + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_DATE
                + " AS " + CallerViewColumns.COLUMN_DATE + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_DURATION
                + " AS " + CallerViewColumns.COLUMN_DURATION + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_TYPE
                + " AS " + CallerViewColumns.COLUMN_TYPE + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_FEATURES
                + " AS " + CallerViewColumns.COLUMN_FEATURES + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_COUNTRY_ISO
                + " AS " + CallerViewColumns.COLUMN_COUNTRY_ISO + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_VOICEMAIL_URI
                + " AS " + CallerViewColumns.COLUMN_VOICEMAIL_URI + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_LOCATION
                + " AS " + CallerViewColumns.COLUMN_LOCATION + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_NAME
                + " AS " + CallerViewColumns.COLUMN_NAME + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_NUMBER_TYPE
                + " AS " + CallerViewColumns.COLUMN_NUMBER_TYPE + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_NUMBER_LABEL
                + " AS " + CallerViewColumns.COLUMN_NUMBER_LABEL + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_LOOKUP_URI
                + " AS " + CallerViewColumns.COLUMN_LOOKUP_URI + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_MATCHED_NUM
                + " AS " + CallerViewColumns.COLUMN_MATCHED_NUM + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_NORMALIZED_NUM
                + " AS " + CallerViewColumns.COLUMN_NORMALIZED_NUM + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_PHOTO_ID
                + " AS " + CallerViewColumns.COLUMN_PHOTO_ID + ","
                + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_PHOTO_URI
                + " AS " + CallerViewColumns.COLUMN_PHOTO_URI + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_FORMATTED_NUM
                + " AS " + CallerViewColumns.COLUMN_FORMATTED_NUM + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_IS_READ
                + " AS " + CallerViewColumns.COLUMN_IS_READ + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_RING_TIME
                + " AS " + CallerViewColumns.COLUMN_RING_TIME + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_SIMID
                + " AS " + CallerViewColumns.COLUMN_SIMID + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_ICCID
                + " AS " + CallerViewColumns.COLUMN_ICCID + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_PHONE_RECORD_PATH
                + " AS " + CallerViewColumns.COLUMN_PHONE_RECORD_PATH + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_PHONE_RECORD_COMMENTS
                + " AS " + CallerViewColumns.COLUMN_PHONE_RECORD_COMMENTS + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_ALI_CALLTYPE
                + " AS " + CallerViewColumns.COLUMN_ALI_CALLTYPE + ","
            + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_NEW
                + " AS " + CallerViewColumns.COLUMN_NEW + ","
            + CallerNumberTable.TABLE_CALLER_NUMBER + "." + CallerNumberTable.COLUMN_LOC_PROVINCE
                + " AS " + CallerViewColumns.COLUMN_LOC_PROVINCE + ","
            + CallerNumberTable.TABLE_CALLER_NUMBER + "." + CallerNumberTable.COLUMN_LOC_AREA
                + " AS " + CallerViewColumns.COLUMN_LOC_AREA + ","
            + CallerNumberTable.TABLE_CALLER_NUMBER + "." + CallerNumberTable.COLUMN_SHOP_NAME
                + " AS " + CallerViewColumns.COLUMN_SHOP_NAME + ","
            + CallerNumberTable.TABLE_CALLER_NUMBER + "." + CallerNumberTable.COLUMN_TAG_NAME
                + " AS " + CallerViewColumns.COLUMN_TAG_NAME + ","
            + CallerNumberTable.TABLE_CALLER_NUMBER + "." + CallerNumberTable.COLUMN_MARKED_COUNT
                + " AS " + CallerViewColumns.COLUMN_MARKED_COUNT
            + " FROM " + CallsTable.TABLE_CALLS + " LEFT OUTER JOIN " + CallerNumberTable.TABLE_CALLER_NUMBER
            + " ON (" + CallsTable.TABLE_CALLS + "." + CallsTable.COLUMN_FORMATTED_NUM
                + "=" + CallerNumberTable.TABLE_CALLER_NUMBER + "." + CallerNumberTable.COLUMN_FORMATTED_NUMBER + ");";
    // database definition end.

    public static final int DATABASE_VERSION = 20;

    private static final String SQL_QUERY_CALL_LOG_EX =
            "select "
                    + CallsTable.COLUMN_ID + ","
                    + CallsTable.COLUMN_TYPE + ","
                    + CallsTable.COLUMN_PHONE_RECORD_PATH + ","
                    + CallsTable.COLUMN_RING_TIME
                    + " from " + CallsTable.TABLE_CALLS;

    private static final int INDEX_ID = 0;
    private static final int INDEX_TYPE = 1;
    private static final int INDEX_PHONE_RECORD_PATH = 2;
    private static final int INDEX_RING_TIME = 3;

    public static AtomicBoolean sIsDatabaseReady = new AtomicBoolean(false);

    private static volatile AliContactsDatabaseHelper sAliContactsDatabaseHelper = null;

    private Context mContext = null;

    /**
     * Get instance of AliContactsDatabaseHelper
     *
     * @param context - ApplicationContext
     * @return
     */
    /*YunOS BEGIN PB*/
    //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
    //##BugID:(8438191) ##date:2016-6-22 09:00
    //##description:synchronized cause anr
    public static AliContactsDatabaseHelper getInstance(Context context) {
        if (sAliContactsDatabaseHelper == null) {
            synchronized(AliContactsDatabaseHelper.class){
            if (sAliContactsDatabaseHelper == null) {
		            if (context == null) {
		                return null;
		            } else {
		                sAliContactsDatabaseHelper = new AliContactsDatabaseHelper(context);
		                // call getReadableDatabase() to make the tables and views created (onCreate/onUpgrade called).
		                sAliContactsDatabaseHelper.getReadableDatabase();
		                sIsDatabaseReady.set(true);
		            }
	            }
            }
        }
        return sAliContactsDatabaseHelper;
    }
/*
    public static AliContactsDatabaseHelper getSingletonEnumInstance(Context context) {
        return SingletonEnum.instance.getInstance(context);
    }

    public enum SingletonEnum {
        instance;
        SingletonEnum() {
        } 

        public AliContactsDatabaseHelper getInstance(Context context) {
	        if (sAliContactsDatabaseHelper == null) {
	            if (context == null) {
	                return null;
	            } else {
	                sAliContactsDatabaseHelper = new AliContactsDatabaseHelper(context);
	                sAliContactsDatabaseHelper.getReadableDatabase();
	                sIsDatabaseReady.set(true);
	            }
	        }
	        return sAliContactsDatabaseHelper;
        }
    }
    */
    /*YUNOS END PB*/

    private AliContactsDatabaseHelper(Context context) {
        super(context, DatabaseConstants.NEW_DATABASE_NAME, null, DATABASE_VERSION);
        Log.i(TAG, "AliContactsDatabaseHelper: constructor.");
        mContext = context;
    }

    //this will read old calls table's callId, phone record path, ring time for new database.
    private void saveOldData(Context context) {
        File callLogExDatabase =
                context.getDatabasePath(DatabaseConstants.OLD_DATABASE_NAME);
        SQLiteDatabase db = null;
        Cursor oldDataCursor = null;
        final String whereId = "_id=";
        try {
            ContentResolver resolver = context.getContentResolver();
            if (callLogExDatabase.exists()) {
                Log.i(TAG, "saveOldData, find old Callog phone record and ring time from Database");
                db = context.openOrCreateDatabase(
                        DatabaseConstants.OLD_DATABASE_NAME,
                        Context.MODE_PRIVATE,
                        null);
                oldDataCursor = db.rawQuery(SQL_QUERY_CALL_LOG_EX, null);
                if (oldDataCursor != null) {
                    int count = oldDataCursor.getCount();
                    Log.i(TAG, "saveOldData, oldDataCurosr count = " + count);
                    if (count > 0) {
                        int type;
                        String recordPath;
                        long ringTime;
                        long id;
                        for (oldDataCursor.moveToFirst(); !oldDataCursor.isAfterLast(); oldDataCursor
                                .moveToNext()) {
                            type = oldDataCursor.getInt(INDEX_TYPE);
                            recordPath = oldDataCursor.getString(INDEX_PHONE_RECORD_PATH);
                            ringTime = oldDataCursor.getLong(INDEX_RING_TIME);
                            if ((type != Calls.MISSED_TYPE || ringTime == 1) && TextUtils.isEmpty(recordPath)) {
                                continue;
                            }
                            ContentValues cv = new ContentValues();
                            id = oldDataCursor.getLong(INDEX_ID);
                            if (type == Calls.MISSED_TYPE) {
                                recordPath = null;
                            }
                            cv.put(CallsTable.COLUMN_PHONE_RECORD_PATH, recordPath);
                            cv.put(CallsTable.COLUMN_RING_TIME,ringTime);
                            try {
                                int result = resolver.update(Calls.CONTENT_URI, cv, whereId + id, null);
                                Log.d(TAG, "saveOldData() update id : " + whereId + ", result : " + result);
                            } catch (SQLiteException e) {
                                Log.e(TAG, "saveOldData() error", e);
                            }
                        }
                    }
                }
            }
        } catch (SQLiteException e) {
            // give up backup old data, e.g. phone record path, ring time.
            Log.e(TAG, "saveOldData: got exception.", e);
        } finally {
            if (db != null) {
                db.close();
            }
            if (oldDataCursor != null) {
                oldDataCursor.close();
            }
        }
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate db = " + db);
        saveOldData(mContext);
        mContext.deleteDatabase(DatabaseConstants.OLD_DATABASE_NAME);
        sIsDatabaseReady.set(createNewTables(db));
    }

    private boolean createNewTables(SQLiteDatabase db) {
        db.execSQL(CREATE_CALLS_TABLE);
        db.execSQL(CREATE_CALLER_NUMBER_TABLE);
        db.execSQL(CREATE_CALLER_VIEW_TABLE);
        return true;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: oldVersion=" + oldVersion + ", newVersion=" + newVersion);
        // The db with version < 17 has a different name,
        // so we won't get here with oldVersion < 17.
        if (oldVersion < 20) {
            upgradeToVersion20(db);
            oldVersion = 20;
        }
        sIsDatabaseReady.set(true);
    }

    /**
     * Upgrade to version 18, 19 and 20 uses the same logic.
     * @param db
     */
    private void upgradeToVersion20(SQLiteDatabase db) {
        Log.i(TAG, "upgradeToVersion20:");
        // table calls is a mirror of the calls table in ContactsProvider,
        // so it is safe to re-create it. The same to table caller_number.
        db.execSQL("DROP VIEW IF EXISTS " + Views.CALLER + ";");
        db.execSQL("DROP TABLE IF EXISTS " + CallsTable.TABLE_CALLS + ";");
        db.execSQL("DROP TABLE IF EXISTS " + CallerNumberTable.TABLE_CALLER_NUMBER + ";");
        db.execSQL(CREATE_CALLS_TABLE);
        db.execSQL(CREATE_CALLER_NUMBER_TABLE);
        db.execSQL(CREATE_CALLER_VIEW_TABLE);
    }

    public static File getDatabaseFile(Context context) {
        File ypDBFile = context.getDatabasePath(DatabaseConstants.OLD_DATABASE_NAME);
        return ypDBFile;
    }

    // Begin views
    public interface Views {
        public static final String CALLER = "view_caller";
    }

    public interface CallerViewColumns {
        // calls column
        public static final String CALL_ID = CallsTable.COLUMN_ID;
        public static final String COLUMN_NUMBER = CallsTable.COLUMN_NUMBER;
        public static final String COLUMN_DATE = CallsTable.COLUMN_DATE;
        public static final String COLUMN_DURATION = CallsTable.COLUMN_DURATION;
        public static final String COLUMN_TYPE = CallsTable.COLUMN_TYPE;
        public static final String COLUMN_FEATURES = CallsTable.COLUMN_FEATURES;
        public static final String COLUMN_COUNTRY_ISO = CallsTable.COLUMN_COUNTRY_ISO;
        public static final String COLUMN_VOICEMAIL_URI = CallsTable.COLUMN_VOICEMAIL_URI;
        public static final String COLUMN_LOCATION = CallsTable.COLUMN_LOCATION;
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
        public static final String COLUMN_SIMID = CallsTable.COLUMN_SIMID;
        public static final String COLUMN_ICCID = CallsTable.COLUMN_ICCID;
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
    }

    public static Integer readIntOrNull(Cursor c, int colIndex) {
        if (c.isNull(colIndex)) {
            return null;
        } else {
            return c.getInt(colIndex);
        }
    }

    public static Long readLongOrNull(Cursor c, int colIndex) {
        if (c.isNull(colIndex)) {
            return null;
        } else {
            return c.getLong(colIndex);
        }
    }
}
