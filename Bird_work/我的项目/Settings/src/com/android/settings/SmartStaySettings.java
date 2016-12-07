/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface; //--
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;  //---
import android.preference.PreferenceGroup;  //--
import android.preference.PreferenceScreen; //--
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.security.KeyStore;
import android.service.trust.TrustAgentService;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable; // --
import com.android.settings.search.SearchIndexableRaw;

import com.mediatek.settings.ext.IPermissionControlExt;
import com.mediatek.settings.ext.IPplSettingsEntryExt; //--
import com.mediatek.settings.ext.IMdmPermissionControlExt;
import com.mediatek.settings.ext.IDataProtectionExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;//--

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.preference.PreferenceCategory;//lvhuaiyi add for Fingerprint

import android.os.SystemProperties;//bird add
import com.bird.settings.BirdFeatureOption;//add by hanyang



/**
 * Gesture lock pattern settings.
 */
public class SmartStaySettings extends SettingsPreferenceFragment
        implements  Indexable,Preference.OnPreferenceChangeListener{
   
    static final String TAG = "SmartStaySettings";
    private static final String KEY_SMART_STAY = "smart_stay_preference";    
    CheckBoxPreference mstmartstay;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        initPrefereceScreen();
	} 	
    private void initPrefereceScreen() {        
        addPreferencesFromResource(R.xml.smart_stay_settings);     
        mstmartstay=(CheckBoxPreference)findPreference(KEY_SMART_STAY); 
		int model = android.provider.Settings.System.getInt(getActivity().getContentResolver(),"smart_stay_model_movies",0);
        boolean isChecked = model == 1;
        mstmartstay.setChecked(isChecked);  
        mstmartstay.setOnPreferenceChangeListener(this);
    }    
     @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {        
        Boolean isCheck = (Boolean)newValue;
		android.provider.Settings.System.putInt(getActivity().getContentResolver(),"smart_stay_model_movies",isCheck?1:0);
        return true;
    }
	
	@Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.METRICS_SMART_STAY;
    }
}
