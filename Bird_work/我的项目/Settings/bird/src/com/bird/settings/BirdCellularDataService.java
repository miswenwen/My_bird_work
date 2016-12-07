package com.bird.settings;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.os.SystemProperties;
import android.os.ServiceManager;
import android.os.Handler;
import java.util.List;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.content.ContentResolver;

public class BirdCellularDataService extends Service {
    private final static String TAG = "BirdCellularDataService";
	private final static String BIRD_AUTO_CELLULAR_DATA = "bird_auto_cellular_data";
	private final static String BIRD_AUTO_CELLULAR_DATA_ICCID = "bird_auto_cellular_data_iccid";

	private Context mContext;
	private ContentResolver mContentResolver = null;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    Handler service = new Handler();
    Runnable runService = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "run begin");
            final SubscriptionManager mSubscriptionManager = SubscriptionManager.from(mContext);

            List<SubscriptionInfo> subList = mSubscriptionManager.getActiveSubscriptionInfoList();
            if(subList == null) {
                Log.i(TAG, "subList == null");
                return;
            }

            Log.i(TAG, "subList.size:" + subList.size());
            if(subList.size() == 0) {
                Settings.System.putString(mContentResolver, BIRD_AUTO_CELLULAR_DATA_ICCID, "");
            }
            
            for (SubscriptionInfo record : subList) {
                if(record != null) {
                    if(BirdFeatureOption.BIRD_GEMINI_DEFAULT_DATA_ON_ALWAYS) {
                        String iccId = Settings.System.getString(mContentResolver, BIRD_AUTO_CELLULAR_DATA_ICCID);
                        Log.i(TAG, "iccId:" + iccId + ",record.iccId:" + record.getIccId());

                        if(iccId != null && iccId.equals(record.getIccId())) {
                            Log.i(TAG, "break");
                            break;
                        }
                        Settings.System.putString(mContentResolver, BIRD_AUTO_CELLULAR_DATA_ICCID, record.getIccId());
                    } else {
                        Settings.System.putInt(mContentResolver, BIRD_AUTO_CELLULAR_DATA, 1);
                    }

                    int subId = record.getSubscriptionId();
					int dataSubId = mSubscriptionManager.getDefaultDataSubId();
                    Log.i(TAG, "subId=" + subId + ",dataSubId=" + dataSubId);
                    if(dataSubId < 0) {
						Log.i(TAG, "setDefaultDataSubId=" + subId);
                        mSubscriptionManager.setDefaultDataSubId(subId);
                    } else {
						Log.i(TAG, "DO NOT setDefaultDataSubId!");
                    }

                    //try {
                    //    Thread.sleep(60000);
                    //} catch (InterruptedException e) {}

                    TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                    mTelephonyManager.setDataEnabled(true);
                    break;
                }
            }

            Log.i(TAG, "run end");
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
		
        mContext = this;
        mContentResolver = getContentResolver();

        final boolean autuCellularData = (Settings.System.getInt(mContentResolver, BIRD_AUTO_CELLULAR_DATA, 0) != 0);
        Log.i(TAG, "autuGprsConnection=" + autuCellularData);
        if(autuCellularData) {
            return;
        }

        service.postDelayed(runService, 60000);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}

