package com.android.settings.fingerprint.support;

//import com.bird.fingerprint.dao.FpsTable;
import android.hardware.fingerprint.FingerprintManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

public class AppData extends Application {
	private static Context sContext;

	@Override
	public void onCreate() {
		super.onCreate();
		
		sContext = getApplicationContext();
//		chmodDir();

		Settings.System.putString(getContentResolver(), PackagesConstant.SETTINGS_LAST_LOCK_APP_PACKAGENAME,
				PackagesConstant.FINGERPRINTUNLCOK_PACKAGENAME);
	}

	public static Context getContext() {
		return sContext;
	}

/*
	private int safe_lock = FingerprintManager.MSG_RES.MSG_REG_OK;

	public int getSafe_lock() {
		return safe_lock;
	}

	public void setSafe_lock(int safe_lock) {
		this.safe_lock = safe_lock;
	}

	private int finger_count = 0;

	public int getFinger_count() {

		return finger_count;
	}

	public void setFinger_count(int finger_count) {
		this.finger_count = finger_count;
	}

	public static void  chmodDir() {
		String filenameString = FingerprintManager.FP_DATA_DIR;
		String command = "chmod 777 " + filenameString;
		Runtime runtime = Runtime.getRuntime();
		try {
			Process proc = runtime.exec(command);
		} catch (Exception e) {
			// TODO: handle exception
		}

	}
*/
}
