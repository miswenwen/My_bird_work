//PSensorCalibration, add by shenzhiwang, 20160407

package com.bird.settings;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.settings.R;
import android.app.Activity;
import android.os.Bundle;
import com.bird.settings.sensornative.ProximitySensorNative;
import android.os.SystemProperties;

public class PSensorCalibrationActivity extends Activity {
    private final static String TAG = "PSensorCalibrationActivity";

    private final static int NONE = 0;
    private final static int SUCCEED = 1;
    private final static int BUSY = 2;
    private final static int FAIL = 3;

    boolean isProximityOccupied(){
        int state = SystemProperties.getInt("sys.proximity.state", 0);
        Log.i(TAG, "proximity check state = " + state);
        return state != 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        int result = NONE;

        if(isProximityOccupied()) {
            Log.i(TAG, "occupied");
            result = BUSY;
        } else {
            result = ProximitySensorNative.calibrateSensor() ? SUCCEED : FAIL;
        }

        Intent intent = new Intent();
        intent.putExtra("result", result);
        setResult(RESULT_OK, intent);
        
//BIRD_BACKUP_SENSOR, add start by shenzhiwang, 20160406
        if(SystemProperties.getBoolean("ro.bdfun.backup_sensor", false)) {
            Intent bintent = new Intent("bird.intent.receiver.backup_sensor");
            sendBroadcast(bintent);
        }
//BIRD_BACKUP_SENSOR, add end by shenzhiwang, 20160406
        finish();
    }
}
