/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class Data {
	public static final String DATABASE_NAME = "AccessControl.db";
	public static final String TABLE_ACCESS_CONTROL = "access_control";
	public static final String KEY_PACKAGE_NAME = "package_name";
	public static final String PACKAGE = "com.bird.accesscontrol";
	public static final Uri CONTENT_URI_ACCESS_CONTROL = Uri.parse("content://" + PACKAGE + "/" + TABLE_ACCESS_CONTROL);
	
	public static boolean isInAccessControl(Context context, String package_name)
	{
		boolean result = false;

		Cursor cursor = context.getContentResolver().query(
				CONTENT_URI_ACCESS_CONTROL, null, 
				KEY_PACKAGE_NAME + " = '" + package_name + "'", null, null);
		
		if (cursor != null)
		{
			result = cursor.getCount()!=0;
			cursor.close();
		}
		
		return result;
	}

	public static void insertAccessControl(Context context, String package_name)
	{
		boolean need_insert = !isInAccessControl(context, package_name);
		
		if (need_insert)
		{
			ContentValues values = new ContentValues();
			values.put(KEY_PACKAGE_NAME, package_name);
			context.getContentResolver().insert(CONTENT_URI_ACCESS_CONTROL, values);
		}
	}
	
	public static void deleteAccessControl(Context context, String package_name)
	{
		context.getContentResolver().delete(CONTENT_URI_ACCESS_CONTROL, 
			KEY_PACKAGE_NAME + " = '" + package_name + "'", null);
	}
	
	public static ArrayList<String> loadAccessControl(Context context)
	{
		ArrayList<String> result = new ArrayList<String>();
		Cursor cursor = context.getContentResolver().query(CONTENT_URI_ACCESS_CONTROL, null, null, null, null);

		if (cursor != null)
		{
			while (cursor.moveToNext())
			{
				result.add(cursor.getString(cursor.getColumnIndex(KEY_PACKAGE_NAME)));
			}

			cursor.close();
		}
		
		return result;
	}
	//lvhuaiyi add begin
	public static void emptyAccessControl(Context context)
	{
		context.getContentResolver().delete(CONTENT_URI_ACCESS_CONTROL, null, null);
	}	
	//lvhuaiyi add end
}