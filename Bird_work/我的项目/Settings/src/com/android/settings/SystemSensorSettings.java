package com.android.settings;

import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.preference.SwitchPreference;
import android.util.Log;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.provider.Settings;

import com.bird.settings.BirdFeatureOption;
import com.android.internal.logging.MetricsLogger;
public class SystemSensorSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSensorSettings";

	private static final String SYS_SENSOR_ROOT_KEY = "root_preference_sys_sensor";

	private static final String THREE_FINGER_PRINTSCREEN_KEY = "three_finger_printscreen";
	

	private static final String BIRD_THREE_FINGER_PRINTSCREEN = "bird_three_finger_printscreen";
	private static final int BIRD_THREE_FINGER_PRINTSCREEN_CLOSE = 0;
	private static final int BIRD_THREE_FINGER_PRINTSCREEN_OPEN = 1;

    private SwitchPreference mChangeWallpaperPreference;
    private SwitchPreference mScreenShotPreference;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.system_sensor_settings);
		
		PreferenceGroup parentPreference = (PreferenceGroup)findPreference(SYS_SENSOR_ROOT_KEY);	

		if (BirdFeatureOption.BIRD_MULTI_TOUCH && BirdFeatureOption.BIRD_3POINT_TOUCH_SCREENSHOT) {
            mScreenShotPreference = (SwitchPreference) findPreference(THREE_FINGER_PRINTSCREEN_KEY);
            mScreenShotPreference.setOnPreferenceChangeListener(this);
        } else {
            parentPreference.removePreference(findPreference(THREE_FINGER_PRINTSCREEN_KEY));
        }
    }

	private void updateState() {
       

		if(mScreenShotPreference != null) { 
			int threefingerMode = Settings.System.getInt(getContentResolver(),
                   BIRD_THREE_FINGER_PRINTSCREEN , BIRD_THREE_FINGER_PRINTSCREEN_CLOSE);
            mScreenShotPreference.setChecked(threefingerMode != BIRD_THREE_FINGER_PRINTSCREEN_CLOSE);
		}
    }
	
	 @Override
    public void onResume() {
        super.onResume();
        updateState();
    }
     @Override
        protected int getMetricsCategory() {
            return MetricsLogger.DISPLAY;
        }
	@Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
		
		
		if (preference == mScreenShotPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), BIRD_THREE_FINGER_PRINTSCREEN,
                    auto ? BIRD_THREE_FINGER_PRINTSCREEN_OPEN: BIRD_THREE_FINGER_PRINTSCREEN_CLOSE);
        }  
		
        return true;
    }
}
