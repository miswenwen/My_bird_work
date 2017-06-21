package com.bird.accesscontrol;

import java.io.File;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public final class DataUtil {

	private static final String PREFERENCE_FILE = "fingerprint_config";
	private static final String SECURE_MODE_KEY ="secure_mode";
	private static final String SECURITY_LEVEL_KEY = "security_level";
	private static final String TEMPLATE_DIR = "tpl";
	
	public static final String NO_SHORTCUT = "";
	
	private  File mTemplateDir = null;		
	private  Map<String, String> mTemplateMap = null;
	
	public static int getSecuritLevel(Context context) {
		SharedPreferences settings = context.getSharedPreferences(
				PREFERENCE_FILE, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
		
		return settings.getInt(SECURITY_LEVEL_KEY, 338);
	}
	
	public static void storeSecurityLevel(Context context, int level) {
			SharedPreferences settings = context.getSharedPreferences(
					PREFERENCE_FILE, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(SECURITY_LEVEL_KEY, level);		
			editor.commit();
	}
	
	private File getTemplateDir(Context context) {
		if (mTemplateDir == null) {
			mTemplateDir = new File(context.getFilesDir().getAbsolutePath() + "/" + TEMPLATE_DIR);
			mTemplateDir.mkdir();
		}
		return mTemplateDir;
	}
	
	private  Map<String, String> getTemplateMap(Context context) {
		if (mTemplateMap == null) {
			mTemplateMap = new HashMap<String, String>();
			File[] files = getTemplateDir(context).listFiles();
			SharedPreferences settings = context.getSharedPreferences(
					PREFERENCE_FILE, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
			
			for (File f: files) {
				String absPath = f.getAbsolutePath();
				mTemplateMap.put(absPath, settings.getString(absPath, NO_SHORTCUT));
			}
		}
		return mTemplateMap;
	}
	
	public static int templateNameToIndex(String name) {
		String dir = new String("/" + TEMPLATE_DIR + "/" + "finger");
		int index = name.lastIndexOf(dir);
		if (index < 0)
			return -1;
		
		index += dir.length();
		String fingerIndex = name.substring(index, name.length());
		
		try {
			return Integer.valueOf(fingerIndex);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	
	public String[] getTemplateList(Context context) {
		return getTemplateMap(context).keySet().toArray(new String[0]);
	}
	
	public String getPathForFinger(Context context, int finger) {
		return getTemplateDir(context).getAbsolutePath() + "/" + "finger" + finger;
	}
	
	public void invalidateTemplateData() {
		mTemplateMap = null;
	}
	
	public String getShortcut(Context context, int finger) {
		return getTemplateMap(context).get(getPathForFinger(context, finger));
	}
	
	public boolean hasTemplate(Context context, int finger) {
		return getTemplateMap(context).containsKey(getPathForFinger(context, finger));
	}
	
	public boolean setShortcut(Context context, int finger , String packageName) {
		String templateName = getPathForFinger(context, finger);
		if (getTemplateMap(context).containsKey(templateName)) {
			SharedPreferences settings = context.getSharedPreferences(
					PREFERENCE_FILE, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(templateName, packageName != null ? packageName : NO_SHORTCUT);
			editor.commit();
			invalidateTemplateData();
			return true;
		}
		return false;
	}
	
	public void deleteTemplate(Context context, int finger) {
		String name = getPathForFinger(context, finger);
		File f = new File(name);
		setShortcut(context, finger, null);
		f.delete();
		invalidateTemplateData();
	}

}
