//bird, imei by adb, add by shenzhiwang @20160705
package com.bird.settings;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.os.Handler;
import com.bird.hide.Utils;
import android.provider.Settings;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.settings.R;

public class BirdIMEIService extends Service {
    private final static String TAG = "BirdIMEIService";

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    Handler service = new Handler();
    Runnable runService = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "runService begin");
            final Context context = getApplicationContext();
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei_invalid = context.getResources().getString(R.string.imei_invalid);
            String value = "";
            boolean isOnePhone = telephonyManager.getPhoneCount() == 1;
            for (int slot = 0; slot < telephonyManager.getPhoneCount(); slot++) {
                String imei = telephonyManager.getDeviceId(slot);
                Log.i(TAG, "slot=" + slot + ",imei=" + imei + ",isOnePhone=" + isOnePhone);

                if(isOnePhone) {
                    value = (TextUtils.isEmpty(imei) ? imei_invalid : imei); 
                    setSystemProperty("persist.sys.imei", value);
                } else {
                    value = (TextUtils.isEmpty(imei) ? imei_invalid : imei) + "  "; 
                    setSystemProperty("persist.sys.imei" + (slot + 1), value);
                }
            }
            Log.i(TAG, "runService end");
        }
    };

    private void  setSystemProperty(String key, String value) {
        SystemProperties.set(key, value);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate BirdIMEIService");
        service.postDelayed(runService, 10);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}

