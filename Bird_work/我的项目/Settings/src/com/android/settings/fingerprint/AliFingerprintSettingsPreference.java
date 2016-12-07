package com.android.settings.fingerprint;

import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;
import android.preference.Preference;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.android.settings.R;

import com.android.settings.ChooseLockSettingsHelper;

public class AliFingerprintSettingsPreference extends Preference {

    private static int FINGERPRINT_SIZE = 5;
    public static final String FINGERPRINT = "fingerprint";
    private Context mContext;
    private AliFingerprintUser mUser;
    private ArrayList<String> mNameList;
//    private String mPayFingerName;
	private View mView;

    public AliFingerprintSettingsPreference(Context context, AliFingerprintUser user, ArrayList<String> list) {
        super(context);
        setLayoutResource(R.layout.ali_fingerprint_item);
        mContext = context;
        mUser = user;
        mNameList=list;
//        mPayFingerName = payFingerName;
    }
	
	public AliFingerprintUser getUser() {
		return mUser;
	}
	
	public boolean isUserEqualsNull() {
		return mUser == null;
	}

    @Override
    protected void onClick() {
        if(mUser == null ) {
			/*
        	FingerprintManager fingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
            if(fingerprintManager.getEnrolledFingerprints().size() < FINGERPRINT_SIZE) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings",
                        FingerprintEnrollEnrolling.class.getName());
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startActivityForResult(intent, ADD_FINGERPRINT_REQUEST);
            } else {
                Toast.makeText(mContext,R.string.fingerprint_settings_listFullTips,Toast.LENGTH_SHORT).show();
            }
			*/
        } else {
			/*
            Intent intent  = new Intent(mContext, AliFingerSingleSetting.class);
            intent.putExtra(FINGERPRINT, mUser.getFingerPrint());
            mContext.startActivity(intent);
			*/
        }
    }
	
	public View getView() { return mView; }	

	@Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if(null == mUser){
            final TextView textview_title = (TextView) view.findViewById(R.id.textView_title);
            textview_title.setText(mContext.getString(R.string.fingerprint_settings_addtext));
            final TextView textview_tip = (TextView) view.findViewById(R.id.textView_tip);
            return;
        }
        final TextView textview_title = (TextView) view.findViewById(R.id.textView_title);
        textview_title.setText(mUser.getFingerPrint().getName());
        final TextView textview_tip = (TextView) view.findViewById(R.id.textView_tip);
        int operation = mUser.getFingerQuickOperation();
        String target = mUser.getFingerQuickTarget();
        String data = mUser.getFingerQuickTargetData();
        if(operation == AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT) {
            if(target == null || target.isEmpty()) {
                textview_tip.setText(mContext.getString(R.string.fingerprint_click_setting));
            } else {
                String displayName = AliFingerprintUtils.getContactNameByPhoneNumber(mContext, target);
                if(displayName != null && !displayName.isEmpty()) {
                    textview_tip.setText(/*mContext.getString(R.string.fingerquick_settings_dial) + ": " + */target + "(" + displayName + ")");
                } else {
                    textview_tip.setText(/*mContext.getString(R.string.fingerquick_settings_dial) + ": " + */target);
                }
            }
        } else if(operation == AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP) {
            String appName = AliFingerprintUtils.getApplicationName(mContext, target, data);
            if(appName == null || appName.isEmpty()) {
                textview_tip.setText(mContext.getString(R.string.fingerprint_click_setting));
            } else {
                textview_tip.setText(/*mContext.getString(R.string.fingerquick_settings_startapp) + ": " + */appName);
            }
        } else {
            textview_tip.setText(mContext.getString(R.string.fingerprint_click_setting));
        }
		mView = view;
    }

}
