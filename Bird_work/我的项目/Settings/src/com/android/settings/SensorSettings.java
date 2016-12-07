package com.android.settings;

import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.util.Log;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;
public class SensorSettings extends SettingsPreferenceFragment {
    private static final String TAG = "SensorSettings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
        addPreferencesFromResource(R.xml.sensor_settings);
    }
    @Override
        protected int getMetricsCategory() {
            return MetricsLogger.DISPLAY;
        }
}
