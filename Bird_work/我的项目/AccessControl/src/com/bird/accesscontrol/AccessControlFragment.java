/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

//import com.fingerprints.fpc1080Mobile.DataUtil;//lvhuaiyi add

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
//liuqipeng add
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.provider.Settings;
import android.content.Context;
import java.util.List;
//liuqipeng end
public class AccessControlFragment extends PreferenceFragment implements OnPreferenceChangeListener {
	private boolean isPackageGuestModeInstalled;
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			setCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL, !getCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL));
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//liuqipeng add
		if(!isNoSwitch()){
	        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);   
        	startActivity(intent); 
		}
		//liuqipeng end
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		findPreference(Pref.ENABLE_ACCESS_CONTROL).setOnPreferenceChangeListener(this);
		findPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD).setOnPreferenceChangeListener(this);//lvhuaiyi add
		boolean value = getCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL);
		PreferenceScreen root = getPreferenceScreen();
		isPackageGuestModeInstalled = Utils.isPackageGuestModeInstalled(getActivity());

		if (isPackageGuestModeInstalled)
		{
			findPreference(Pref.PROTECT_GUARDED_MODE).setOnPreferenceChangeListener(this);
			findPreference(Pref.PROTECT_GUARDED_MODE).setEnabled(value);
		}
		else
		{
			root.removePreference(findPreference(Pref.PROTECT_GUARDED_MODE));
		}
		
		findPreference(Pref.PROTECT_APPLICATIONGS).setEnabled(value);
		findPreference(Pref.MAKE_PATTERN_VISIBLE).setEnabled(value);
		findPreference(Pref.VIBRATE_ON_TOUCH).setEnabled(value);
		//lvhuaiyi add begin
		DataUtil mDataUtil = new DataUtil();
		if(Pref.PASSWORD_NUMBER_TYPE){
			if(findPreference(Pref.ENABLE_ACCESS_CONTROL)!=null)
			root.removePreference(findPreference(Pref.ENABLE_ACCESS_CONTROL));
			if(findPreference(Pref.PROTECT_APPLICATIONGS)!=null)
			root.removePreference(findPreference(Pref.PROTECT_APPLICATIONGS));
			if(findPreference(Pref.PROTECT_GUARDED_MODE)!=null)
			root.removePreference(findPreference(Pref.PROTECT_GUARDED_MODE));
			if(findPreference(Pref.MAKE_PATTERN_VISIBLE)!=null)
			root.removePreference(findPreference(Pref.MAKE_PATTERN_VISIBLE));
			if(findPreference(Pref.VIBRATE_ON_TOUCH)!=null)
			root.removePreference(findPreference(Pref.VIBRATE_ON_TOUCH));
			Preference passwordProtect = (Preference)findPreference(Pref.PASSWORD_PROTECT);
			Preference fingerprintProtect = (Preference)findPreference(Pref.FINGERPRINT_PROTECT);			
		  if(PasswordUtils.getPasswordSharedPreferences(getActivity(), null) == null){
          setCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD, false);
      }else{
          setCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD, true);
      }
		  if(!getCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD)){
		      passwordProtect.setSummary(R.string.password_protect_summary);
		      findPreference(Pref.SELECR_PROTECT_APPLICATION).setEnabled(false);
		      findPreference(Pref.PASSWORD_PROTECT).setEnabled(false);	
		      if(fingerprintProtect!=null){
		          fingerprintProtect.setSummary(R.string.fingerprint_protect_summary);
		          fingerprintProtect.setEnabled(false);
		      }		          
		  }else{
		      passwordProtect.setSummary(R.string.password_modify_summary);
		      findPreference(Pref.SELECR_PROTECT_APPLICATION).setEnabled(true);
		      findPreference(Pref.PASSWORD_PROTECT).setEnabled(true);
		      if(fingerprintProtect!=null){
		          fingerprintProtect.setSummary(R.string.fingerprint_modify_summary);	
		      	  fingerprintProtect.setEnabled(true);
		      }    
		  }
			if(!Pref.FINGERPRIINT_TYPE && fingerprintProtect!=null){
			    root.removePreference(fingerprintProtect);    
		  }
		}else{
			if(findPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD)!=null)
			root.removePreference(findPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD));
			if(findPreference(Pref.SELECR_PROTECT_APPLICATION)!=null)
			root.removePreference(findPreference(Pref.SELECR_PROTECT_APPLICATION));
			if(findPreference(Pref.PASSWORD_PROTECT)!=null)
			root.removePreference(findPreference(Pref.PASSWORD_PROTECT));
			if(findPreference(Pref.FINGERPRINT_PROTECT)!=null)
			root.removePreference(findPreference(Pref.FINGERPRINT_PROTECT));		
		}		
		//lvhuaiyi add end
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		String key = preference.getKey();
		Activity activity = getActivity();

		if (key.equals(Pref.PROTECT_APPLICATIONGS)) {
			startActivity(new Intent(activity, ProtectApplicationsActivity.class));
		}
		else if (key.equals(Pref.ENABLE_ACCESS_CONTROL))
		{
			boolean value = Pref.getEnableAccessControl(activity);

			if (!value)
			{
				startActivityForResult(new Intent(activity, ConfirmLockPattern.class)
					.putExtra(Utils.EXTRA_PREF, Pref.ENABLE_ACCESS_CONTROL), 
					Utils.REQUEST_CODE_ASK);
			}
			else
			{
				startActivityForResult(new Intent(activity, ChooseLockPattern.class), 
					Utils.REQUEST_CODE_SET_PASSWORD);
			}
			mHandler.sendEmptyMessage(0);
		}
		else if (key.equals(Pref.PROTECT_GUARDED_MODE))
		{
			setCheckBoxPreference(Pref.PROTECT_GUARDED_MODE, !getCheckBoxPreference(Pref.PROTECT_GUARDED_MODE));
			startActivityForResult(new Intent(activity, ConfirmLockPattern.class)
				.putExtra(Utils.EXTRA_PREF, Pref.PROTECT_GUARDED_MODE), 
				Utils.REQUEST_CODE_ASK);
		}
		//lvhuaiyi add begin
		else if (key.equals(Pref.ENABLE_ACCESS_CONTROL_PASSWORD))
		{
			startActivityForResult(new Intent(activity, ConfirmPassword.class)
				.putExtra(Utils.EXTRA_PREF, Pref.ENABLE_ACCESS_CONTROL_PASSWORD), 
				Utils.REQUEST_CONFIRM_PASSWORD);
		}
		else if(key.equals(Pref.SELECR_PROTECT_APPLICATION)){
			startActivity(new Intent(activity, ProtectApplicationsActivity.class));			
		}
		else if(key.equals(Pref.PASSWORD_PROTECT)){
			startActivity(new Intent(activity, ModifyPassword.class));	
		}
		else if(key.equals(Pref.FINGERPRINT_PROTECT)){
			String value = "";
			DataUtil mDataUtil = new DataUtil();
			if(!mDataUtil.hasTemplate(getActivity(), Pref.HANDFINGERINDEX)){
				value = getString(R.string.enrol_activity);
			}else{
				value = getString(R.string.verify_activity);
			}
			startActivity(new Intent(activity, EnrolStart.class).putExtra("handandfinger", value));	
		}
		//lvhuaiyi add end

	    return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean result = false;
		String key = preference.getKey();
		Activity activity = getActivity();

		if (key.equals(Pref.ENABLE_ACCESS_CONTROL)) {
			boolean value = (Boolean)newValue;
			activity.stopService(new Intent(activity, AccessControlService.class));

			if (value)
			{
				activity.startService(new Intent(activity, AccessControlService.class));
			}

			if (isPackageGuestModeInstalled)
			{
				findPreference(Pref.PROTECT_GUARDED_MODE).setEnabled(value);
			}

			findPreference(Pref.PROTECT_APPLICATIONGS).setEnabled(value);
			findPreference(Pref.MAKE_PATTERN_VISIBLE).setEnabled(value);
			findPreference(Pref.VIBRATE_ON_TOUCH).setEnabled(value);
			result = true;
		}
		else if (key.equals(Pref.PROTECT_GUARDED_MODE))
		{
			boolean value = (Boolean)newValue;
			
			if (value)
			{
				Data.insertAccessControl(activity, Utils.PACKAGE_GUEST_MODE);
			}
			else
			{
				Data.deleteAccessControl(activity, Utils.PACKAGE_GUEST_MODE);
			}
			result = true;
		}	
		//lvhuaiyi add begin
		else if (key.equals(Pref.ENABLE_ACCESS_CONTROL_PASSWORD)) {
			boolean value = (Boolean)newValue;
			activity.stopService(new Intent(activity, AccessControlService.class));
			Preference passwordProtect = (Preference)findPreference(Pref.PASSWORD_PROTECT);
			if (value)
			{
				activity.startService(new Intent(activity, AccessControlService.class));
			}
		  findPreference(Pref.SELECR_PROTECT_APPLICATION).setEnabled(value);
		  passwordProtect.setEnabled(value);	
		  if(PasswordUtils.getPasswordSharedPreferences(getActivity(), null) == null){
		      passwordProtect.setSummary(R.string.password_protect_summary);  
		  }else{
		      passwordProtect.setSummary(R.string.password_modify_summary);		        
		  }
		  if(Pref.FINGERPRIINT_TYPE){
    		  Preference fingerprintProtect = (Preference)findPreference(Pref.FINGERPRINT_PROTECT);		
    		  fingerprintProtect.setEnabled(value);    		  	
		      DataUtil mDataUtil = new DataUtil();
		      if(!mDataUtil.hasTemplate(getActivity(), Pref.HANDFINGERINDEX)){
		    	    fingerprintProtect.setSummary(R.string.fingerprint_protect_summary);	
		      }else{
		    	    fingerprintProtect.setSummary(R.string.fingerprint_modify_summary);	
		      }
		  }
			result = true;
		}		
		//lvhuaiyi add end
		return result;
	}

	private boolean setCheckBoxPreference(CharSequence key, boolean newValue)
	{
		CheckBoxPreference checkBoxPreference = (CheckBoxPreference)findPreference(key);
		checkBoxPreference.setChecked(newValue);
		
		return onPreferenceChange(checkBoxPreference, newValue);
	}
	
	private boolean getCheckBoxPreference(CharSequence key)
	{
		CheckBoxPreference checkBoxPreference = (CheckBoxPreference)findPreference(key);
		
		return checkBoxPreference.isChecked();
	}

	public void onActivityResult(int resquestcode, int resultcode, Intent data)
	{
		switch (resquestcode)
		{
			case Utils.REQUEST_CODE_ASK:
				switch (resultcode)
				{
					case Activity.RESULT_CANCELED:
						String pref = data.getStringExtra(Utils.EXTRA_PREF);
						if (pref.equals(Pref.ENABLE_ACCESS_CONTROL))
						{
							setCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL, false);
						}
						else if (pref.equals(Pref.PROTECT_GUARDED_MODE))
						{
							setCheckBoxPreference(Pref.PROTECT_GUARDED_MODE, !getCheckBoxPreference(Pref.PROTECT_GUARDED_MODE));
						}
						break;

					default:
						break;
				}
				break;

			case Utils.REQUEST_CODE_SET_PASSWORD:
				switch (resultcode)
				{
					case Activity.RESULT_CANCELED:
						setCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL, true);
						break;

					default:
						break;
				}
				break;
      //lvhuaiyi add begin
			case Utils.REQUEST_CONFIRM_PASSWORD:
				switch (resultcode)
				{
					case Activity.RESULT_CANCELED:
						if(PasswordUtils.getPasswordSharedPreferences(getActivity(), null) != null)
						    setCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD, true);
						else
							setCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD, false);
						break;
					default:
						break;
				}
				break;	
			case Utils.REQUEST_ENROL_PASSWORD:
				switch (resultcode)
				{
					case Activity.RESULT_CANCELED:
						setCheckBoxPreference(Pref.ENABLE_ACCESS_CONTROL_PASSWORD, true);
						break;

					default:
						break;
				}
				break;				
			//lvhuaiyi add end	
			default:
				break;
		}
	}
	//lvhuaiyi add begin
	@Override
    public void onResume(){
    	if(Pref.PASSWORD_NUMBER_TYPE && Pref.FINGERPRIINT_TYPE){
		      Preference fingerprintProtect = (Preference)findPreference(Pref.FINGERPRINT_PROTECT);	
	        DataUtil mDataUtil = new DataUtil();
	        if(!mDataUtil.hasTemplate(getActivity(), Pref.HANDFINGERINDEX)){
	    	      fingerprintProtect.setSummary(R.string.fingerprint_protect_summary);	
	        }else{
	    	      fingerprintProtect.setSummary(R.string.fingerprint_modify_summary);	
	        }
	    }
    	super.onResume();
    }
	//lvhuaiyi add end
	//liuqipeng add
	private boolean isNoSwitch() {   
		long ts = System.currentTimeMillis();   
		UsageStatsManager usageStatsManager = (UsageStatsManager)getActivity().getSystemService(Context.USAGE_STATS_SERVICE);   
		List queryUsageStats = usageStatsManager.queryUsageStats(   
		        UsageStatsManager.INTERVAL_BEST, 0, ts);   
		if (queryUsageStats == null || queryUsageStats.isEmpty()) {   
		    return false;  
		}   
		return true;  
	} 
	//liuqipeng end
}
