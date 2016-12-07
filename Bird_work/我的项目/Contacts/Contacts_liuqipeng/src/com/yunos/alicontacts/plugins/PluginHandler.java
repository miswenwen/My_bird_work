package com.yunos.alicontacts.plugins;

import android.content.Context;
import android.util.Log;

import com.yunos.alicontacts.interfaces.AliContactsPlatformInterface;

public class PluginHandler implements AliContactsPlatformInterface {
	private static final String TAG = "PluginHandler";

	@Override
	public void handleSIMContacts(Context arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG, "handleSIMContacts");
		PluginPlatformPrefs.setValue(arg0,
				PluginPlatformPrefs.HANDLE_SIM_CONTACTS, 0);
	}

	@Override
	public void setCMCCMode(Context arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG, "setCMCCMode");
		PluginPlatformPrefs.setValue(arg0,
				PluginPlatformPrefs.HANDLE_SIM_CONTACTS, 0);
	}

	@Override
	public void setContactsLimitSize(Context arg0, int arg1) {
		// TODO Auto-generated method stub
		Log.i(TAG, "setContactsLimitSize " + arg1);
		PluginPlatformPrefs.setValue(arg0, PluginPlatformPrefs.CONTACTS_LIMIT,
				arg1);
	}

	@Override
	public void setTELECOMMode(Context arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG, "setTELECOMMode");
		PluginPlatformPrefs.setValue(arg0, PluginPlatformPrefs.TELECOM, 0);
	}

	@Override
	public void setUNICOMMode(Context arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG, "setUNICOMMode");
		PluginPlatformPrefs.setValue(arg0, PluginPlatformPrefs.UNICOM, 0);
	}
}
