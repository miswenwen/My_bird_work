/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.InstrumentedActivity;
import com.android.settings.R;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.NavigationBar;

/**
 * Base activity for all fingerprint enrollment steps.
 */
public abstract class FingerprintEnrollBase extends InstrumentedActivity
        implements View.OnClickListener {
    static final int RESULT_FINISHED = FingerprintSettings.RESULT_FINISHED;
    static final int RESULT_SKIP = FingerprintSettings.RESULT_SKIP;
    static final int RESULT_TIMEOUT = FingerprintSettings.RESULT_TIMEOUT;

    protected byte[] mToken;
	
	/*add by lichengfeng custom fingerprint 20160620 begin*/
	protected String mFingerprintReenrollName;
	protected String mIntentName;
	public static final String INTENT_NAME = com.android.settings.SecuritySettings.class.getName();
	public static final String FINGERPRINT_INTENT_NAME = "fingerprint_intent_name";
	public static final String FINGERPRINT_REENROLL_NAME = "fingerprint_reenroll_name";
	/*add by lichengfeng custom fingerprint 20160620 end*/	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_FingerprintEnroll);
		
		/*add by lichengfeng custom fingerprint 20160620 begin*/
		Intent intent = getIntent();
        mToken = intent.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        mFingerprintReenrollName = intent.getStringExtra(FINGERPRINT_REENROLL_NAME);
		mIntentName = intent.getStringExtra(FINGERPRINT_INTENT_NAME);
		android.util.Log.i("lcf_finger", "FingerprintEnrollFinish onCreate mToken:"+mToken);
		
        if (savedInstanceState != null) {
			if (mToken == null) {
				mToken = savedInstanceState.getByteArray(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
			}
            
			if (mFingerprintReenrollName == null) {
				mFingerprintReenrollName = savedInstanceState.getString(FINGERPRINT_REENROLL_NAME);
			}
			
			if (mIntentName == null) {
				mIntentName = savedInstanceState.getString(FINGERPRINT_INTENT_NAME);
			}
        }		
		/*add by lichengfeng custom fingerprint 20160620 end*/
		
		/*add by lichengfeng custom fingerprint 20160620 end*/				
		/* removed by lichengfeng form <google source code> 20160728 
		mToken = intent.getByteArrayExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        if (savedInstanceState != null && mToken == null) {
            mToken = savedInstanceState.getByteArray(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        }
		*/
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
		/*add by lichengfeng custom fingerprint 20160620 begin*/
		outState.putString(FINGERPRINT_REENROLL_NAME, mFingerprintReenrollName);
		outState.putString(FINGERPRINT_INTENT_NAME, mIntentName);
		/*add by lichengfeng custom fingerprint 20160620 end*/
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initViews();
    }

    protected void initViews() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getNavigationBar().setVisibility(View.GONE);
        Button nextButton = getNextButton();
        if (nextButton != null) {
            nextButton.setOnClickListener(this);
        }
    }

    protected NavigationBar getNavigationBar() {
        return (NavigationBar) findViewById(R.id.suw_layout_navigation_bar);
    }

    protected SetupWizardLayout getSetupWizardLayout() {
        return (SetupWizardLayout) findViewById(R.id.setup_wizard_layout);
    }

    protected void setHeaderText(int resId, boolean force) {
        TextView layoutTitle = (TextView) getSetupWizardLayout().findViewById(
                R.id.suw_layout_title);
        CharSequence previousTitle = layoutTitle.getText();
        CharSequence title = getText(resId);
        if (previousTitle != title || force) {
            if (!TextUtils.isEmpty(previousTitle)) {
                layoutTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            }
            getSetupWizardLayout().setHeaderText(title);
            setTitle(title);
        }
    }

    protected void setHeaderText(int resId) {
        setHeaderText(resId, false /* force */);
    }

    protected Button getNextButton() {
        return (Button) findViewById(R.id.next_button);
    }

    @Override
    public void onClick(View v) {
        if (v == getNextButton()) {
            onNextButtonClick();
        }
    }

    protected void onNextButtonClick() {
    }

    protected Intent getEnrollingIntent() {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", FingerprintEnrollEnrolling.class.getName());
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        return intent;
    }
	/*add by lichengfeng fingerprint 20160728 begin*/
    protected Intent getFingerprintSettingsIntent() {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", FingerprintSettings.class.getName());
		intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
		intent.putExtra(FINGERPRINT_INTENT_NAME, FingerprintEnrollFinish.class.getName());
        return intent;
    }
	/*add by lichengfeng fingerprint 20160728 end*/
}
