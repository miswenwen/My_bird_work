/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class AccessControlOpenHelper extends SQLiteOpenHelper {
	public AccessControlOpenHelper(Context context) {
		super(context, Data.DATABASE_NAME, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " +
				Data.TABLE_ACCESS_CONTROL + " (" +
				BaseColumns._ID + " integer primary key autoincrement, " +
				Data.KEY_PACKAGE_NAME + " text);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + Data.TABLE_ACCESS_CONTROL);
		onCreate(db);
	}
}