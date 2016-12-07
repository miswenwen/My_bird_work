package com.bird.settings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import android.os.SystemProperties;
import android.content.Intent;
import com.bird.settings.sensornative.GSensorNative;

public class GSensorCalibrationActivity extends Activity {
	private static final String TAG = "GSensorCalibration";
	private static final int DATA_X = 0;
	private static final int DATA_Y = 1;
	private static final int DATA_Z = 2;
	private static final int ORIENTATION_UNKNOWN = -1;

	private static final float CENTER_X = 0;
	private static final float CENTER_Y = 0;
	private static final float LIMIT = 9;
	private static final int AVARAGE_COUNT = 4;

	private Context mContext;
	private Handler mHandler = new Handler();

	private Button mButton;
	private SensorManager mSensorManager;
	private Sensor mGSensor;
	private ImageView mImageViewX, mImageViewY;
	private Animation mAnimH, mAnimV;
	private TextView mTv1,mTv2;
	
	private float mPrevX, mPrevY;
	private Resources mRes;
	private int count;
	private float mSumX,mSumY;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gsensor_calibration);

		mContext = this;

		mButton = (Button) findViewById(R.id.cali);
		mButton.setOnClickListener(mOnClickListener);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mGSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		mSensorManager.registerListener(mSensorListener, mGSensor,
				SensorManager.SENSOR_DELAY_FASTEST);

		mAnimH = AnimationUtils.loadAnimation(mContext, R.drawable.bubble_move_h);
		mAnimV = AnimationUtils.loadAnimation(mContext, R.drawable.bubble_move_v);

		mImageViewX = (ImageView) findViewById(R.id.imageView1);
		mImageViewY = (ImageView) findViewById(R.id.imageView2);

		mPrevX = CENTER_X;
		mPrevY = CENTER_Y;


		mTv1=(TextView) findViewById(R.id.textView1);
		mTv2=(TextView) findViewById(R.id.textView2);
		
		mRes = getResources();
		mSumX=0;
		mSumY=0;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mSensorManager.registerListener(mSensorListener, mGSensor,
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mSensorManager.unregisterListener(mSensorListener);
		
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mSensorManager.unregisterListener(mSensorListener);		
	}


	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mButton.getText().equals(
					mRes.getString(R.string.gsensor_calibration))) {
				mSensorManager.unregisterListener(mSensorListener);
				mImageViewX.setAnimation(null);
				mImageViewY.setAnimation(null);

				mButton.setEnabled(false);
				
				mTv1.setText(R.string.gsensor_hint3);
				mTv2.setText(R.string.gsensor_hint4);
				mHandler.postDelayed(new Runnable() {
					public void run() {
						mAnimH.setAnimationListener(mAnimationListener);										
						mImageViewX.startAnimation(mAnimH);
						mImageViewY.startAnimation(mAnimV);
						
						GSensorNative.gsensor_calibration();						
					}
				}, 1000);



			} else if (mButton.getText().equals(
				mRes.getString(R.string.gsensor_ok))) {

				//if (android.os.SystemProperties.getBoolean("ro.specialrestore",false)){
				//    Log.v(TAG, "start specialdata guard service: specialguard to save the data");
				//    // ningzhiyu 20130524 special data restore
				//    android.os.SystemProperties.set("ctl.start", "specialguard:backup gsensor");   
				//}

//BIRD_BACKUP_SENSOR, add start by shenzhiwang, 20160406
        if(SystemProperties.getBoolean("ro.bdfun.backup_sensor", false)) {
            Intent bintent = new Intent("bird.intent.receiver.backup_sensor");
            sendBroadcast(bintent);
        }
//BIRD_BACKUP_SENSOR, add end by shenzhiwang, 20160406
				finish();	
			}
		}

	};

	private SensorEventListener mSensorListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent event) {
			float[] values = event.values;
			float X = values[DATA_X];
			float Y = values[DATA_Y];
			float Z = values[DATA_Z];

			Log.v(TAG, "X=" + X + " Y=" + Y + " Z=" + Z);

			float x, y;
			if (X > LIMIT) {
				X = LIMIT;
			} else if (X < -LIMIT) {
				X = -LIMIT;
			}

			if (Y > LIMIT) {
				Y = LIMIT;
			} else if (Y < -LIMIT) {
				Y = -LIMIT;
			}
			
			mSumX+=X;
			mSumY+=Y;
			
			if (count >= AVARAGE_COUNT) {
				
				count = 0;
				X=mSumX/AVARAGE_COUNT;
				Y=mSumY/AVARAGE_COUNT;
				
				x = CENTER_X + X * 95 / LIMIT;
				y = CENTER_Y - Y * 95 / LIMIT;
				// if(mPrevX-x>5||x-mPrevX>5)
				{
					Animation anim;

					anim = new TranslateAnimation(mPrevX, x, 0, 0);
					anim
							.setInterpolator(new AccelerateDecelerateInterpolator());
					anim.setDuration(50);
					anim.setFillAfter(true);
					mImageViewX.startAnimation(anim);
				}

				// if(mPrevY-y>5||y-mPrevY>5)

				{
					Animation anim;

					anim = new TranslateAnimation(0, 0, mPrevY, y);
					anim
							.setInterpolator(new AccelerateDecelerateInterpolator());
					anim.setDuration(50);
					anim.setFillAfter(true);
					mImageViewY.startAnimation(anim);
				}

				mPrevX = x;
				mPrevY = y;
				
				
				mSumX=0;
				mSumY=0;
			}else{
				count++;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	};

	AnimationListener mAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationEnd(Animation animation) {
			// TODO Auto-generated method stub
			mTv1.setText(R.string.gsensor_hint5);
			mTv2.setVisibility(View.INVISIBLE);
			
			mButton.setText(R.string.gsensor_ok);
			mButton.setEnabled(true);

			mSensorManager.registerListener(mSensorListener, mGSensor,
					SensorManager.SENSOR_DELAY_FASTEST);

		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub

		}

	};

}
