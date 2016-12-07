package com.android.settings.fingerprint.support;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.android.settings.fingerprint.support.PackagesConstant;
import android.hardware.fingerprint.FingerprintManager;

public class PreferenceUtils {
	public static String KEY_ISSAFETY = "key_issafety";
	public static String KEY_NEED_PWD = "key_need_pwd";
	public static String KEY_PWD = "key_pwd";
	public static String VLUE_ERROR_PWD = "value_error_pwd";
	public static String SYSTEM_SETTINGS_FP_SCREENLOCK = "com_bird_fingerprint_usedto_screenlock";//FingerprintManager.SETTINGS_KEY_USEDTO_SCREENLOCK;
	public static String SYSTEM_SETTINGS_FP_APPLOCK = "com_bird_fingerprint_usedto_applock";//FingerprintManager.SETTINGS_KEY_USEDTO_APPLOCK;
	
	public static int isSafety() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppData.getContext());
		return preferences.getInt(KEY_ISSAFETY, 0);
	}

	public static void setSafety(int isSafe) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppData.getContext());
		preferences.edit().putInt(KEY_ISSAFETY, isSafe).commit();
	}

	public static boolean isNeedPwd() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppData.getContext());
		return preferences.getBoolean(KEY_NEED_PWD, true);
	}
	
	public static void enablePwd(boolean needPwd) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppData.getContext());
		preferences.edit().putBoolean(KEY_NEED_PWD, needPwd).commit();
	}

	public static void putPwd(String pwd) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppData.getContext());
		preferences.edit().putString(KEY_PWD, pwd).commit();
	}

	public static String getPwd() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppData.getContext());
		return preferences.getString(KEY_PWD, VLUE_ERROR_PWD);
	}

	public static void setSettingFpScreenLockOffOn(boolean value) {
		Settings.System.putInt(AppData.getContext().getContentResolver(),SYSTEM_SETTINGS_FP_SCREENLOCK, value == true ? 1 : 0);
		
		if(value == false /* && isSettingFpAppLockOn() == false*/) {
            
		}
	}
	
	public static boolean isSettingFpScreenLockOn() {
		int able = Settings.System.getInt(AppData.getContext().getContentResolver(), SYSTEM_SETTINGS_FP_SCREENLOCK, 0);
		return able == 1 ? true : false;
	}

	public static void setSettingFpAppLockOffOn(boolean value) {
	    if (value == false) {
			Settings.System.putInt(AppData.getContext().getContentResolver(), SYSTEM_SETTINGS_FP_APPLOCK, 0);
			Settings.System.putString(AppData.getContext().getContentResolver(), PackagesConstant.SETTINGS_LAST_LOCK_APP_PACKAGENAME,"");
			if (isSettingFpScreenLockOn() == false) {
		        
			}

	    } else {
	    	Settings.System.putInt(AppData.getContext().getContentResolver(), SYSTEM_SETTINGS_FP_APPLOCK, 1);
	    }
	}
	
	public static boolean isSettingFpAppLockOn() {
		int able = Settings.System.getInt(AppData.getContext().getContentResolver(), SYSTEM_SETTINGS_FP_APPLOCK, 0);
		return able == 1 ? true : false;
	}
	
	public static void setLockUIStatus(boolean lock) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppData.getContext());
		preferences.edit().putBoolean("lockuirunning", lock).commit();
	}
	
	public static boolean getLockUIStatus() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppData.getContext());
		return preferences.getBoolean("lockuirunning", false);
	}
	
}
