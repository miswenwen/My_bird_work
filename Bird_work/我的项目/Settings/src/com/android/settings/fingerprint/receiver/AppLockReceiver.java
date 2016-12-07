package com.android.settings.fingerprint.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.settings.fingerprint.support.PackagesConstant;

public class AppLockReceiver extends BroadcastReceiver {
	private static final String TAG = "AppLockReceiver";
	private String INTENT_ADD_ACTIVITY_ACTION = "com.bird.fingerprint.lockui";
	private String INTENT_UPDATE_LAST_PACKAGE_NAME = "com.bird.fingerprint.update.last_package_name";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(INTENT_ADD_ACTIVITY_ACTION)) {
		    String packageName = intent.getStringExtra("packagename");
		    
            Intent target = new Intent();
            target.putExtra("packagename", packageName);
            target.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            target.setClassName("com.android.settings", "com.android.settings.fingerprint.applock.LockUI");
            context.startActivity(target);
            Log.i(TAG, "---start intent---" + packageName);
		} else if (intent.getAction().equals(INTENT_UPDATE_LAST_PACKAGE_NAME)) {
		    String packageName = intent.getStringExtra("packagename");
		    Settings.System.putString(context.getContentResolver(), PackagesConstant.SETTINGS_LAST_LOCK_APP_PACKAGENAME,
								packageName);

            String allTemp = getUnlockedApp(context, packageName);
			Log.i(TAG, "allTemp = " + allTemp);
			Settings.System.putString(context.getContentResolver(), "com_bird_already_unlocked_packagesname", allTemp);
		}
	}
	
	private String getUnlockedApp(Context context, String mGlobalPackageNameString) {
	    List<String> existLockAppsList = new ArrayList<String>();
	    String temp = Settings.System.getString(context.getContentResolver(), "com_bird_already_unlocked_packagesname");
	    if (temp != null && !temp.equals("")) {
	        String[] appsStrings = temp.split("\\|");
			existLockAppsList = Arrays.asList(appsStrings);
			if (existLockAppsList.contains(mGlobalPackageNameString)) {
			    return temp;
			} else {
			    return temp + mGlobalPackageNameString + "|";
			}
	    } else {
	        return "";
	    }
	}
	
}
