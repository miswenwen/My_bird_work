package com.bird.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.bird.settings.sensornative.ProximitySensorNative;

// @ { ningzhiyu 20160329 work around proximity calibration issue
import android.os.SystemProperties;
import android.widget.Toast;
// @ } end
import android.content.Intent;

public class ProximityPreference extends DialogPreference{
	private static final String TAG = "ProximitySensorCali";

	private onCalibration mListener;
	private Context mContext;
	
	public interface onCalibration{
		public void onSuccess();
		public void onFail(boolean busy);// ningzhiyu work around proximity calibration issue, add busy
	}
	
	public ProximityPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	boolean isProximityOccupied(){
		int state = SystemProperties.getInt("sys.proximity.state", 0);
		Log.i(TAG, "proximity check state="+state);
		return state != 0;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		// @ { ningzhiyu work around proximity calibration issue
		if (isProximityOccupied()){//ningzhiyu
			  //Toast.makeText(mContext, "proximity is occupied,", Toast.LENGTH_LONG).show();
			  if(mListener != null){
				    mListener.onFail(true);
			  }
			  return;
		}
		// @ }end
		
		switch(which){
			case DialogInterface.BUTTON_POSITIVE:
				enableCalibration();
				dialog.cancel();
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				dialog.cancel();
				break;
		}
	}
	
	public void setOnCalibration(onCalibration listener){
		mListener = listener;
	}

	private void enableCalibration(){
		if(ProximitySensorNative.calibrateSensor()){
			if(mListener != null){
				mListener.onSuccess();
				
//BIRD_BACKUP_SENSOR, add start by shenzhiwang, 20160406
        if(SystemProperties.getBoolean("ro.bdfun.backup_sensor", false)) {
            Intent bintent = new Intent("bird.intent.receiver.backup_sensor");
            mContext.sendBroadcast(bintent);
        }
//BIRD_BACKUP_SENSOR, add end by shenzhiwang, 20160406
			}
			Log.i(TAG, "proximity calibration succeed.");
		}else{
			if(mListener != null){
				mListener.onFail(false);// ningzhiyu work around proximity calibration issue, add busy
			}
			Log.i(TAG, "proximity calibration failed.");
		}
	}
}
