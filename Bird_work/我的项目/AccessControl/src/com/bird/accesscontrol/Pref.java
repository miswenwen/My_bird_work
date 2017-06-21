/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.bird.widget.MyPref;

import android.content.Context;
import android.util.Log;
import android.os.SystemProperties;

public class Pref extends MyPref {
	private static final String TAG = "Pref";
	public static final String ENABLE_ACCESS_CONTROL = "enable_access_control";
	public static final String PROTECT_APPLICATIONGS = "protect_applicationgs";
	public static final String PROTECT_GUARDED_MODE = "protect_guarded_mode";
	public static final String MAKE_PATTERN_VISIBLE = "make_pattern_visible";
	public static final String VIBRATE_ON_TOUCH = "vibrate_on_touch";
	private static final String PASSWORD = "password";
	//lvhuaiyi add begin
	public static final String ENABLE_ACCESS_CONTROL_PASSWORD = "enable_access_control_password";
	public static final String SELECR_PROTECT_APPLICATION = "select_protect_applications";
	public static final String PASSWORD_PROTECT = "password_protect_applicationgs";
	public static final String FINGERPRINT_PROTECT = "fingerprint_protect_applicationgs";
	public static boolean PASSWORD_NUMBER_TYPE =true ;
	public static boolean FINGERPRIINT_TYPE = PASSWORD_NUMBER_TYPE && (SystemProperties.get("ro.bdfun.fingerprint_support","false").equals("true"));
	public static int HANDFINGERINDEX = 0;
	//lvhuaiyi add end
	
	public static boolean getEnableAccessControl(Context context)
	{
		return getBoolean(context, ENABLE_ACCESS_CONTROL, false);
	}

	public static void setEnableAccessControl(Context context, boolean value)
	{
		setBoolean(context, ENABLE_ACCESS_CONTROL, value);
	}

	//lvhuaiyi add begin
	public static boolean getEnableAccessControlPassword(Context context)
	{
		return getBoolean(context, ENABLE_ACCESS_CONTROL_PASSWORD, false);
	}
	
	public static void setEnableAccessControlPassword(Context context, boolean value)
	{
		setBoolean(context, ENABLE_ACCESS_CONTROL_PASSWORD, value);
	}	
	//lvhuaiyi add end
	
	public static boolean getProtectGuardedMode(Context context)
	{
		return getBoolean(context, PROTECT_GUARDED_MODE, false);
	}

	public static void setProtectGuardedMode(Context context, boolean value)
	{
		setBoolean(context, PROTECT_GUARDED_MODE, value);
	}
	
	public static boolean getMakePatternVisible(Context context)
	{
		return getBoolean(context, MAKE_PATTERN_VISIBLE, false);
	}

	public static void setMakePatternVisible(Context context, boolean value)
	{
		setBoolean(context, MAKE_PATTERN_VISIBLE, value);
	}
	
	public static boolean getVibrateOnTouch(Context context)
	{
		return getBoolean(context, VIBRATE_ON_TOUCH, false);
	}

	public static void setVibrateOnTouch(Context context, boolean value)
	{
		setBoolean(context, VIBRATE_ON_TOUCH, value);
	}

	public static boolean checkPassword(Context context, byte[] value)
	{
		byte[] password = getPassword(context);

		return (value != null) && Arrays.equals(password, value);
	}

	public static byte[] getPassword(Context context)
	{
//		return PreferenceManager.getDefaultSharedPreferences(context).getString(PASSWORD, "01246");
		byte[] result = null;

		try {
			FileInputStream in = context.openFileInput(PASSWORD);
			result = new byte[in.available()];
			in.read(result);
			in.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "WL_DEBUG load:"+e);
		} catch (IOException e) {
			Log.e(TAG, "WL_DEBUG save:"+e);
		}
		
		return result;
	}

	public static void setPassword(Context context, byte[] value)
	{
//		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PASSWORD, value).commit();
		try {
			FileOutputStream out = context.openFileOutput(PASSWORD, Context.MODE_PRIVATE);
			out.write(value);
			out.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "WL_DEBUG save:"+e);
		} catch (IOException e) {
			Log.e(TAG, "WL_DEBUG save:"+e);
		}
	}
}