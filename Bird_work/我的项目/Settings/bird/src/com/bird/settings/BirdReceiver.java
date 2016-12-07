package com.bird.settings;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.provider.Settings;
import android.location.LocationManager;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import java.io.File;
import com.bird.hide.Utils;
import android.media.MediaScannerConnection;//ningzhiyu BIRD_FILES_PRESET

/**
 * BluetoothDiscoveryReceiver updates a timestamp when the
 * Bluetooth adapter starts or finishes discovery mode. This
 * is used to decide whether to open an alert dialog or
 * create a notification when we receive a pairing request.
 *
 * <p>Note that the discovery start/finish intents are also handled
 * by {@link BluetoothEventManager} to update the UI, if visible.
 */
public final class BirdReceiver extends BroadcastReceiver {
    private static final String TAG = "BirdReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received: " + action);

        if(action.equals("android.intent.action.BOOT_COMPLETED")) {

//@ { bird, imei by adb, add by shenzhiwang @20160705
            if(BirdFeatureOption.BIRD_IMEI_BY_ABD) {
                Intent intentService = new Intent(context, BirdIMEIService.class);
                context.startService(intentService);
            }
//@ }

//@ { bird, imei by adb, add by shenzhiwang @20160705
            if(BirdFeatureOption.BIRD_GEMINI_DEFAULT_DATA_ON) {
                Intent intentService=new Intent(context, BirdCellularDataService.class);
                context.startService(intentService);
            }
//@ }

//BIRD_FILES_PRESET, add start by shenzhiwang, 20160629
/* 
            if(BirdFeatureOption.BIRD_FILES_PRESET){
                Log.i(TAG, "BIRD_FILES_PRESET");
                final Context context_inner = context;

                final boolean first = (Settings.System.getInt(context.getContentResolver(), "bird_filse_preset", 0) == 0);
                if(first) {
		    		Intent intentService=new Intent(context,BirdCopyService.class);
                    context.startService(intentService);
                }
            }
*/
//BIRD_FILES_PRESET, add end by shenzhiwang, 20160629

        }else if(BirdFeatureOption.BIRD_MMITEST_DOOV_TEST_SENSOR && action.equals("bird.intent.action.StartGSensorCalibration")){
            Intent intentSGSC=new Intent(context,GSensorCalibrationActivity.class);
            intentSGSC.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentSGSC);
        }
    }
}
