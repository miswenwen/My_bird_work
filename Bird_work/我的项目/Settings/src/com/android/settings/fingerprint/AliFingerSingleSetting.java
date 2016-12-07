package com.android.settings.fingerprint;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;

import android.util.Base64;
import android.util.Log;
import com.android.settings.R;
import android.view.View;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;

import android.provider.ContactsContract.Data;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import java.util.ArrayList;
import java.util.List;
import android.provider.Settings;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;

import android.hardware.fingerprint.FingerprintManager.RemovalCallback;

import com.android.settings.SubSettings;

public class AliFingerSingleSetting extends SubSettings {

    private static final String TAG = "AliFingerSingleSetting";

    public static final int MENU_ID_DELETE = 0;
    public static final int MAX_FINGER_NAME = 128;
    private FingerprintManager mFingerprintManager;
    Fingerprint mFingerprint = null;
    private AliFingerSinglePreferenceFragment mSettingsPreferenceFragment;
	
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, AliFingerSinglePreferenceFragment.class.getName());
        return modIntent;
    }
	
    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AliFingerSinglePreferenceFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        mFingerprintManager = (FingerprintManager) this.getSystemService(
                Context.FINGERPRINT_SERVICE);
        setTitle(mFingerprint.getName());
    }

    private void initData() {
        mFingerprint = (Fingerprint)getIntent().getExtras().getParcelable(AliFingerprintSettingsPreference.FINGERPRINT);
        if(mFingerprint == null) finish();
    }

}
