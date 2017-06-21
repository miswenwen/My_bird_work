package com.bird.accesscontrol;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PasswordUtils {
	private static SharedPreferences mSharedPreferences = null;
	private static Editor mEditor = null;
	private static String APP_LOCK_PASS = "app_lock_password";
	
	private static SharedPreferences getSharedPreferenceObject(Context context){
		if(mSharedPreferences == null)
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return mSharedPreferences;
	}

	private static Editor getEditorObject(Context context){
		if(mEditor == null)
			mEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		return mEditor;
	} 


	public static void setPasswordEditor(Context context,String value){
		getEditorObject(context).putString(APP_LOCK_PASS, value).commit();
	}	

	public static String getPasswordSharedPreferences(Context context,String defaultValue){
		return getSharedPreferenceObject(context).getString(APP_LOCK_PASS, defaultValue);
	}		
	public static void removeSharedPreferences(Context context){
		getEditorObject(context).remove(APP_LOCK_PASS).commit();
	}	
	public static void showInfo(Context context,int id){
		String mInfo = context.getString(id);
		Toast.makeText(context.getApplicationContext(), mInfo,Toast.LENGTH_SHORT).show();
	}	
	public static boolean isHome(Context context,String packageName){
		boolean isHome = false;
		PackageManager mPackageManager = context.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		List<ResolveInfo> mLauchers = mPackageManager.queryIntentActivities(intent,PackageManager.PERMISSION_GRANTED);
		for(int i = 0;i<mLauchers.size();i++){
			if(mLauchers.get(i).activityInfo.packageName.equals(packageName)){
				isHome = true;
				break;
			}else{
				isHome = false;
			}
		}
		return isHome;
	}
	
}
