/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import java.text.Collator;
import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageManager;

public class Utils {
	public static final int REQUEST_CODE_ASK = 0;
	public static final int REQUEST_CODE_SET_PASSWORD = REQUEST_CODE_ASK + 1;
	public static final int MIN_LOCK_PATTERN_SIZE = 4;
	public static final int REQUEST_CONFIRM_PASSWORD = 5;//lvhuaiyi add
	public static final int REQUEST_ENROL_PASSWORD = 6;//lvhuaiyi add
	public static final String EXTRA_PACKAGE_NAME = "PackageName";
	public static final String EXTRA_PREF = "Pref";
	public static final String PACKAGE_GUEST_MODE = "com.nbbsw.guestmode";
	public static ArrayList<String> allowedProtectPackage = new ArrayList<String>();
	public static final Collator sCollator = Collator.getInstance();
	
	private static boolean isPackageInstalled(Context context, String packageName)
	{
		PackageManager pm = context.getPackageManager();
		boolean result = false;

		try {
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			result = true;
		} catch (PackageManager.NameNotFoundException e) {
			result = false;
		}
		
		return result;
	}
	
	public static boolean isPackageGuestModeInstalled(Context context)
	{
		return isPackageInstalled(context, PACKAGE_GUEST_MODE);
	}
}