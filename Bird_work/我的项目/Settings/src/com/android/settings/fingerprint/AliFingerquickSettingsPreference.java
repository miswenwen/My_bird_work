package com.android.settings.fingerprint;

import android.preference.Preference;
import android.content.Context;
import com.android.settings.R;
import android.util.AttributeSet;

public class AliFingerquickSettingsPreference extends Preference {

    public AliFingerquickSettingsPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.ali_fingerquick_item);
    }
    public AliFingerquickSettingsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.ali_fingerquick_item);
    }
    public AliFingerquickSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.ali_fingerquick_item);
    }
}
