/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import android.database.sqlite.SQLiteOpenHelper;

import com.bird.widget.MyProvider;

public class AccessControlProvider extends MyProvider {

	@Override
	protected SQLiteOpenHelper CreateOpenHelper() {
		// TODO Auto-generated method stub
		return new AccessControlOpenHelper(getContext());
	}
	
}