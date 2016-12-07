package com.android.settings.fingerprint;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AliFingerDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "birdfinger.db";
    private static final int DATABASE_VERSION = 1;

    public static final String FINGER_QUICK_TABLE_NAME = "fingerquick";
    public static final String COLUMN_FINGER_ID = "fingerid";
    public static final String COLUMN_OPERATION = "operation";
    public static final String COLUMN_TARGET = "target";
    public static final String COLUMN_DATA = "data";
    public AliFingerDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + FINGER_QUICK_TABLE_NAME +
                "(" + COLUMN_FINGER_ID +" INTEGER PRIMARY KEY, " + COLUMN_OPERATION +
                " INTEGER, " + COLUMN_TARGET + " TEXT, " + COLUMN_DATA + " TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("DROP TABLE IF EXISTS person");
        //onCreate(db);
    }
}

