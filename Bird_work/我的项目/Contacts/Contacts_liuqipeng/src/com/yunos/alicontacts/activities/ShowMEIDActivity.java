/*
 * add by lichengfeng fix *#06# error @20160321
 */

package com.yunos.alicontacts.activities;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.yunos.alicontacts.R;


import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.DialogInterface.OnKeyListener;

import android.app.Activity;
import com.android.internal.telephony.PhoneFactory;
//import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
import android.view.Window;
import android.os.Bundle;
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.internal.telephony.PhoneConstants;
import android.telephony.TelephonyManager;

public class ShowMEIDActivity extends Activity {

    private static final String TAG = "ShowMEIDActivity";
   

    @Override
    protected void onCreate(Bundle savedState) {
        Log.d(TAG, "ShowMEIDActivity onCreate called");
		requestWindowFeature(Window.FEATURE_NO_TITLE); 

        super.onCreate(savedState);
		
		    setSystembarColor(getResources().getColor(R.color.title_color), false);
		
		    String imeiStr = "";
		    
		    imeiStr = "";
		    TelephonyManager telephonyManager =
                (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
		    String mImei1 = telephonyManager.getDeviceId(0);
		    if (mImei1 == null) {
		     	mImei1 = " ";
		    }
		    String mImei2 = telephonyManager.getDeviceId(1);
	    	if (mImei2 == null) {
			      mImei2 = " ";
		    }
			  imeiStr += "MEID: "+ getDeviceId()+"\nIMEI1: "+mImei1+"\nIMEI2: "+mImei2;

			Log.d(TAG, "ShowMEIDActivity onCreate imeiStr"+imeiStr);
		if (!imeiStr.equals("") && imeiStr != null) {
			showDialog(imeiStr,this);
		}
    }
	
	private void setSystembarColor(int color, boolean isSearch) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, false);
        systemBarManager.setStatusBarColor(color);
        systemBarManager.setStatusBarDarkMode(this, isSearch);
    }
	
	private void showDialog(String imeiStr,Context context) {
		new AlertDialog.Builder(context)
                .setTitle(R.string.imei)
                .setMessage(imeiStr)
                .setPositiveButton(android.R.string.ok, 
					new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ShowMEIDActivity.this.finish();
                                    }
                                })
                .setCancelable(false).create().show();
	}

    @Override
    protected void onStart() {
        Log.d(TAG, "ShowMEIDActivity onStart called");
        super.onStart();
    }
	
	@Override
    protected void onResume() {
        Log.d(TAG, "ShowMEIDActivity onResume called");
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "ShowMEIDActivity onDestroy called");
		super.onDestroy();
    }


   //add by fangbin to get MEID begin 20160608 
   public static String getDeviceId() {
     String meid = null;
     boolean shouldRetry = false;
     TelephonyManager mTelephonyManager = TelephonyManager.getDefault();
     TelephonyManagerEx mTelephonyManagerEx = TelephonyManagerEx.getDefault();
     try{
     if (mTelephonyManagerEx.getPhoneType(PhoneConstants.SIM_ID_1) == TelephonyManager.PHONE_TYPE_CDMA) {
         Log.d(TAG, "get SIM_ID_1");
         meid =PhoneFactory.getPhone(PhoneConstants.SIM_ID_1).getMeid();
     } else if (mTelephonyManagerEx.getPhoneType(PhoneConstants.SIM_ID_2) == TelephonyManager.PHONE_TYPE_CDMA) {
         Log.d(TAG, "get SIM_ID_2");
         meid = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2).getMeid();
     }
     if(meid == null){
         meid =PhoneFactory.getPhone(PhoneConstants.SIM_ID_1).getMeid();
     }
     if(meid == null){
         meid =PhoneFactory.getPhone(PhoneConstants.SIM_ID_2).getMeid();
     }
     }catch(Exception e){
         e.printStackTrace();
         //shouldRetry =true;
         Log.d(TAG, " getPhone failed");
     }
     /*if(shouldRetry){
         Log.d(TAG, " retry: makeDefaultPhone");
         PhoneFactory.makeDefaultPhone(context);
     if (mTelephonyManagerEx.getPhoneType(PhoneConstants.SIM_ID_1) == TelephonyManager.PHONE_TYPE_CDMA) {
         Log.d(TAG, "get SIM_ID_1");
         meid =PhoneFactory.getPhone(PhoneConstants.SIM_ID_1).getMeid();
     } else if (mTelephonyManagerEx.getPhoneType(PhoneConstants.SIM_ID_2) == TelephonyManager.PHONE_TYPE_CDMA) {
         Log.d(TAG, "get SIM_ID_2");
         meid = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2).getMeid();
     }
     }*/
     if (meid != null) {
         Log.d(TAG, "get meid != null");
         meid = meid.toUpperCase();
     }else{
         Log.d(TAG, "get meid == null");
     }
     Log.d(TAG, "meid = "+meid);
     return meid;
  }
   //add by fangbin to get MEID end 20160608 
}