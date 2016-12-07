package com.android.settings.data;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import com.android.settings.sim.SimDialogActivity;
import android.util.Log;
import android.preference.SwitchPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.telephony.SubscriptionInfo;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import com.mediatek.settings.sim.TelephonyUtils;
import com.android.internal.telephony.TelephonyIntents;
import android.os.Handler;
public final class DataUsageSettings extends RestrictedSettingsFragment implements Indexable {

    private static final String DISALLOW_CONFIG_DATA = "no_config_data";
    private Context mContext;
    private static final String KEY_CELLULAR_DATA = "mobile_data_network";
    private static final String KEY_MOBILE_DATA = "mobile_data_work";
    private SubscriptionManager mSubscriptionManager;
    private SwitchPreference mMobileDataSwitchPreference;
    private TelephonyManager mTelephonyManager;
    private AlertDialog mDialog;
    private SubscriptionInfo mSubscriptionInfo;
    private Preference mPreference;
    private boolean mIsOpenAirplane=false;
    private int mSubId;
    private Handler mHandler;
    private Runnable mRun;
    //private boolean mIsAirplaneModeOn = false;
    public DataUsageSettings() {
        super(DISALLOW_CONFIG_DATA);
    }
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DATA;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("tianjianwei", "mReceiver action = " + action);
            if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                updateDataValues();
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mIsOpenAirplane = intent.getBooleanExtra("state", false);
                updateAirPlaneValues();
            }
        }
    };


    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        mContext = getActivity();

        mIsOpenAirplane = TelephonyUtils.isAirplaneModeOn(mContext);

        mSubscriptionManager = SubscriptionManager.from(getActivity());
        mTelephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.data_settings);

        mPreference = (Preference)findPreference(KEY_CELLULAR_DATA);
        mMobileDataSwitchPreference = (SwitchPreference)findPreference(KEY_MOBILE_DATA);
        //final int mSubId = mSubscriptionManager.getDefaultDataSubId();
        updateDataValues();
        updateAirPlaneValues();
        createDialog();

        mHandler = new Handler();        
        mRun = new Runnable() {
	    @Override
            public void run() {
	        // TODO Auto-generated method stub
            mMobileDataSwitchPreference.setEnabled(true);		
	    }
	};
	//mHandler.postDelayed(mRun, 3000);
        /*mMobileDataSwitchPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                final int mSubId = mSubscriptionManager.getDefaultDataSubId();
                if(!val){
    	            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    mDialog = builder.setMessage(mContext.getResources().getString(R.string.close_mobile_data_work))
                        .setNegativeButton(mContext.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface arg0, int arg1) {
		                // TODO Auto-generated method stub
		      	        android.util.Log.e("tianjianwei","取消弹框！！！");
                                mMobileDataSwitchPreference.setChecked(true);
	                    }
		        })
		        .setPositiveButton(mContext.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		                // TODO Auto-generated method stub
		                setMobileDataEnabled(mSubId,val);
                                mMobileDataSwitchPreference.setChecked(val);				
		            }
		        });
                    mDialog.create().show();
                }else{
                   setMobileDataEnabled(mSubId,val);
                   mMobileDataSwitchPreference.setChecked(val);	
                }
                return true; 
            }
        });*/

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        //intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    private void createDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mDialog = builder.setMessage(mContext.getResources().getString(R.string.close_mobile_data_work))
                 .setNegativeButton(mContext.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
	             @Override
	             public void onClick(DialogInterface arg0, int arg1) {
		         // TODO Auto-generated method stub
		      	 android.util.Log.e("tianjianwei","取消弹框！！！");
                         mMobileDataSwitchPreference.setChecked(true);
	             }
		 })
		 .setPositiveButton(mContext.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
		     @Override
		     public void onClick(DialogInterface arg0, int arg1) {
		         // TODO Auto-generated method stub
		         setMobileDataEnabled(mSubId,false);
                         mMobileDataSwitchPreference.setChecked(false);				
		     }
		 }).create();
    }

    private void updateDataValues(){
        //final int mSubId = mSubscriptionManager.getDefaultDataSubId();
        mSubscriptionInfo = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (mSubscriptionInfo == null) {
            mMobileDataSwitchPreference.setEnabled(false);
            mPreference.setSummary(R.string.sim_calls_ask_first_prefs_title);
        }else{
            if(mIsOpenAirplane){
                mMobileDataSwitchPreference.setEnabled(false);
            }else{
                mMobileDataSwitchPreference.setEnabled(true);
            }
            if(mMobileDataSwitchPreference.isChecked()){
                mTelephonyManager.setDataEnabled(true);
            }
            mPreference.setSummary(mSubscriptionInfo.getDisplayName());
        }
    }

    private void updateAirPlaneValues(){
        if(mIsOpenAirplane){
            mMobileDataSwitchPreference.setEnabled(false);
            mPreference.setEnabled(false);
        }else{
            mMobileDataSwitchPreference.setEnabled(true);
            mPreference.setEnabled(true);
        }
    }

    private void setMobileDataEnabled(int subId, boolean enabled) {
        mTelephonyManager.setDataEnabled(subId, enabled);
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        final Context context = mContext;
        Intent intent = new Intent(context, SimDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (findPreference(KEY_CELLULAR_DATA) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(intent);
        }
        if(findPreference(KEY_MOBILE_DATA) == preference){
            mSubId = mSubscriptionManager.getDefaultDataSubId();
            if(!((SwitchPreference)preference).isChecked()){
                //mDialog.show();
                setMobileDataEnabled(mSubId,false);
            }else{
                setMobileDataEnabled(mSubId,true);
            }
            preference.setEnabled(false);
            mHandler.postDelayed(mRun, 3000);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
