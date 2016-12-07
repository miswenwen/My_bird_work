package com.yunos.alicontacts.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;

public class PluginPlatformPrefs {
	private static final String TAG = "PluginPlatformPrefs";

	public static final String SHARED_PREFS = "PluginPlatformPrefs";

	public static final String CMCC = "CMCC";
	public static final String UNICOM = "unicom";
	public static final String TELECOM = "telecom";
	public static final String CONTACTS_LIMIT = "contacts_limit";
	public static final String HANDLE_SIM_CONTACTS = "handle_sim_contacts";
	public static final String LAYOUT_PLUGIN_HEADER = "layout_plugin:";
	public static final String PLUGIN_SKIP_PROMPT_HEADER = "plugin_skip_prompt:";

	// type for operators : cmcc, cu and ct
    public static final int OPERATOR_TYPE_NONE = 0; // check not done
    public static final int OPERATOR_TYPE_NO_CUSTOM = 1;
    public static final int OPERATOR_TYPE_CMCC = 2;
    public static final int OPERATOR_TYPE_CU = 3;
    public static final int OPERATOR_TYPE_CT = 4;

    public static int OPERATOR_CUSTOM_TYPE = OPERATOR_TYPE_NONE;

    public static void checkOperator() {
	    if(OPERATOR_CUSTOM_TYPE != OPERATOR_TYPE_NONE)
	        return;
	    // read from system environment variable to decide if operator
        // customization need
        //Log.i("TimeLog", "Before get  properties");
        String marketStr = SystemProperties.get("ro.yunos.carrier.custom");
        if (CMCC.equalsIgnoreCase(marketStr)) {
            OPERATOR_CUSTOM_TYPE = OPERATOR_TYPE_CMCC;
        } else {
            // ??? now only CMCC is support;
            // if other operators are added, create new case here
            OPERATOR_CUSTOM_TYPE = OPERATOR_TYPE_NO_CUSTOM;
        }
        //Log.i("TimeLog", "after get properties");
	}

	public static boolean isCMCC() {
//		int value = getIntValue(context, CMCC);
//		if (value == -1)
//			return false;
//		return true;
	    return (OPERATOR_CUSTOM_TYPE == OPERATOR_TYPE_CMCC);
	}

	public static boolean isUNICOM() {
//		int value = getIntValue(context, UNICOM);
//		if (value == -1)
//			return false;
//		return true;
	    return (OPERATOR_CUSTOM_TYPE == OPERATOR_TYPE_CU);
	}

	public static boolean isTELECOM() {
//		int value = getIntValue(context, TELECOM);
//		if (value == -1)
//			return false;
//		return true;
		return (OPERATOR_CUSTOM_TYPE == OPERATOR_TYPE_CT);
	}

	public static boolean isOperator() {
		return isCMCC() || isUNICOM() || isTELECOM();
	}

	public static int getContactLimit(Context context) {
		return getIntValue(context, CONTACTS_LIMIT);
	}

	public static boolean handleSimContact(Context context) {
		int value = getIntValue(context, HANDLE_SIM_CONTACTS);
		if (value == -1)
			return false;
		return true;
	}

	public static boolean skipLayoutPluginPrompt(Context context, String key) {
		String realkey = PLUGIN_SKIP_PROMPT_HEADER + key;
		boolean existed = findKey(context, realkey);
		if (!existed) {
			setValue(context, realkey, false);
			return false;
		}

		return getBooleanValue(context, realkey);
	}

	public static void setPluginPromptStatus(Context context, String key,
			boolean value) {
		String realkey = PLUGIN_SKIP_PROMPT_HEADER + key;
		setValue(context, realkey, value);
		return;
	}

	public static boolean installedLayoutPlugin(Context context, String key) {
		String realkey = LAYOUT_PLUGIN_HEADER + key;
		boolean existed = findKey(context, realkey);
		if (!existed) {
			setValue(context, realkey, false);
			return false;
		}

		return getBooleanValue(context, realkey);
	}

	public static void setLayoutPluginStatus(Context context, String key,
			boolean value) {
		String realkey = LAYOUT_PLUGIN_HEADER + key;
		setValue(context, realkey, value);
		return;
	}

	public static void setValue(Context context, String key, String value) {
		SharedPreferences sp = context.getApplicationContext()
				.getSharedPreferences(SHARED_PREFS, 0);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static void setValue(Context context, String key, int value) {
		SharedPreferences sp = context.getApplicationContext()
				.getSharedPreferences(SHARED_PREFS, 0);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	public static void setValue(Context context, String key, boolean value) {
		SharedPreferences sp = context.getApplicationContext()
				.getSharedPreferences(SHARED_PREFS, 0);
		SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	private static int getIntValue(Context context, String key) {
		SharedPreferences sp = context.getApplicationContext()
				.getSharedPreferences(SHARED_PREFS, 0);
		return sp.getInt(key, -1);
	}

	private static boolean getBooleanValue(Context context, String key) {
		SharedPreferences sp = context.getApplicationContext()
				.getSharedPreferences(SHARED_PREFS, 0);
		return sp.getBoolean(key, false);
	}

	private static boolean findKey(Context context, String key) {
		SharedPreferences sp = context.getApplicationContext()
				.getSharedPreferences(SHARED_PREFS, 0);
		if (sp.contains(key))
			return true;
		return false;
	}

}
