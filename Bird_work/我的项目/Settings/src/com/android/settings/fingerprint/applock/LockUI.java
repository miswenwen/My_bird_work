package com.android.settings.fingerprint.applock;

import com.android.settings.fingerprint.support.PackagesConstant;
import com.android.settings.R;
import com.android.settings.fingerprint.support.PreferenceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.os.CancellationSignal;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;

import android.app.admin.DevicePolicyManager;

import android.util.SparseBooleanArray;

import com.android.internal.widget.LockPatternUtils;

import android.os.RemoteException;

import android.util.ArraySet;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;

import com.android.settings.ChooseLockSettingsHelper;

import android.content.res.Resources;

import android.telephony.TelephonyManager;

public class LockUI extends Activity implements  OnClickListener {

    private static final int KEYGUARD_REQUEST = 1551;
	private String PWD = "1992";
	private String TAG = "LockUI-->";
	private String mGlobalPackageNameString = null;
	private final int BIRD_FINGER_SEND_METCH = 1;
	
	private boolean DEBUG = true;

	private int failCount = 0;
	private static final int MAXFAILLIMIT = 3;
	private Button btn_toDigital = null;
	private ViewGroup fingerLayout = null;
	private PowerManager mPm;
	
    private FingerprintManager.AuthenticationCallback mAuthenticationCallback
            = new AuthenticationCallback() {

        @Override
        public void onAuthenticationFailed() {
			Log.i(TAG, "onAuthenticationFailed");
            handleFingerprintAuthFailed();
        };

        @Override
        public void onAuthenticationSucceeded(AuthenticationResult result) {
			Log.i(TAG, "onAuthenticationSucceeded");
			int fingerId = result.getFingerprint().getFingerId();
            mHandler.obtainMessage(MSG_FINGER_AUTH_SUCCESS, fingerId, 0).sendToTarget();
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
			Log.i(TAG, "onAuthenticationHelp");
            handleFingerprintHelp(helpMsgId, helpString.toString());
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
			Log.i(TAG, "onAuthenticationError "+errString.toString()+" errMsgId: "+errMsgId);
            handleFingerprintError(errMsgId, errString.toString());
        }

        @Override
        public void onAuthenticationAcquired(int acquireInfo) {
			Log.i(TAG, "onAuthenticationAcquired");
            handleFingerprintAcquired(acquireInfo);
        }
    };
	
    private CancellationSignal mFingerprintCancelSignal;	
    private FingerprintManager mFpm;
	private Context mContext;
	
    private static final int MATCH_SUCCESS = 1;
    private static final int MATCH_FAIL = 2;
    private static final int MATCH_AGAIN = 3;
	private boolean mInFingerprintLockout;
	
	private final Runnable mFingerprintLockoutReset = new Runnable() {
		@Override
		public void run() {
			mInFingerprintLockout = false;
			retryFingerprint();
		}
	};	
	
	private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
	private static final int MSG_FINGER_AUTH_SUCCESS = 1001;
	private static final int MSG_FINGER_AUTH_FAIL = 1002;
	private static final int MSG_FINGER_AUTH_ERROR = 1003;
	private static final int MSG_FINGER_AUTH_HELP = 1004;	
	
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			android.util.Log.i(TAG,"msg.what: "+msg.what);
			switch (msg.what) {
				case MSG_REFRESH_FINGERPRINT_TEMPLATES:
					retryFingerprint();
				break;
				case MSG_FINGER_AUTH_SUCCESS:
					mFingerprintCancelSignal = null;
					retryFingerprint();
					handleFingerprintAuthenticated();
				break;
				case MSG_FINGER_AUTH_FAIL:
					// No action required... fingerprint will allow up to 5 of these
				break;
				case MSG_FINGER_AUTH_ERROR:
					handleError(msg.arg1 /* errMsgId */, (CharSequence) msg.obj /* errStr */ );
				break;
				case MSG_FINGER_AUTH_HELP: {
					// Not used
				}
				break;
			}
		};
	};
	private static final long LOCKOUT_DURATION = 30000; // time we have to wait for fp to reset, ms
	/**
	 * @param errMsgId
	 */
	protected void handleError(int errMsgId, CharSequence msg) {
		mFingerprintCancelSignal = null;
		android.util.Log.i(TAG,"errMsgId: "+errMsgId);
		switch (errMsgId) {
			case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
				return; // Only happens if we get preempted by another activity. Ignored.
			case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
				mInFingerprintLockout = true;
				// We've been locked out.  Reset after 30s.
				if (!mHandler.hasCallbacks(mFingerprintLockoutReset)) {
					mHandler.postDelayed(mFingerprintLockoutReset,
							LOCKOUT_DURATION);
				}
				// Fall through to show message
			default:
				// Activity can be null on a screen rotation.
				//showToast(msg.toString());
			break;
		}
		retryFingerprint(); // start again
	}	
	
    private Handler lockUIHandler = new Handler() {
		public void handleMessage(Message msg) {
		    switch(msg.what) {
		        case MATCH_SUCCESS:
					Log.i(TAG, "onFingerprintAuthenticated userId: "+PreferenceUtils.getLockUIStatus());
					showFinalConfirmation();
		            break;
		        
		        case MATCH_FAIL:
		            if (PreferenceUtils.getLockUIStatus() == true) {
                        failCount++;
                        showToast(getString(R.string.fp_tryagain));
                        Animation shake = AnimationUtils.loadAnimation(LockUI.this,R.anim.shake);
						findViewById(R.id.id_lockui_fp_iv).startAnimation(shake); 
						 
                        if (failCount >= MAXFAILLIMIT) {
							runKeyguardConfirmation(KEYGUARD_REQUEST);
							failCount = 0;
                        }
		            }
		            break;
		            
		        case MATCH_AGAIN:
    		        if (mPm.isScreenOn()) {
						
    		        }
					
                    break;
                    
		        default:
		            break;
		    }
		}
	};
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			android.util.Log.i(TAG,"receiver getAction() "+intent.getAction());
			if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				android.util.Log.i(TAG,TAG+" onReceive ACTION_SCREEN_OFF");
				Settings.System.putString(getContentResolver(), "com_bird_already_unlocked_packagesname", "");
				finish();
			}
		}
	};
	
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		android.util.Log.i(TAG,"onActivityResult requestCode: "+requestCode+" resultCode: "+resultCode);
        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK) {
            showFinalConfirmation();
        } else {
            establishInitialState();
        }
    }
	
    private void establishInitialState() {
		android.util.Log.i(TAG,"establishInitialState");
    }	
	
	private void registerReceiver() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		mContext.registerReceiver(receiver, filter);
	}
	
	private void unRegisterReceiver() {
		mContext.unregisterReceiver(receiver);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		Log.e(TAG, "onCreate");
		mGlobalPackageNameString = getIntent().getStringExtra("packagename");
		mPm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
		
		setContentView(R.layout.activity_lock_ui);

		ActionBar bar = getActionBar();
        if (bar != null) {
			bar.setElevation(0);
		}

        mFpm = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);

		fingerLayout = (ViewGroup) findViewById(R.id.fingerlayout);
		
		btn_toDigital = (Button) findViewById(R.id.toDigital);
		btn_toDigital.setOnClickListener(this);

		Log.e(TAG, "LockUI.getIntent() packageName:"+ mGlobalPackageNameString);
		
		registerReceiver();
	}
	
	private void retryFingerprint() {
		android.util.Log.i(TAG,"retryFingerprint mInFingerprintLockout: "+mInFingerprintLockout);
		if (!mInFingerprintLockout) {
			mFingerprintCancelSignal = new CancellationSignal();
			mFpm.authenticate(null, mFingerprintCancelSignal, 0, mAuthenticationCallback, null);
		}
	}	

    private boolean runKeyguardConfirmation(int request) {
        Resources res = getResources();
		long challenge = 0l;
        return new ChooseLockSettingsHelper(this).launchConfirmationActivity(
                request, res.getText(R.string.lockui_title),null,null,challenge);
    }
	
    public boolean isUnlockWithFingerprintPossible(int userId) {
        return mFpm != null && mFpm.isHardwareDetected() && mFpm.getEnrolledFingerprints(userId).size() > 0;
    }
	
    private void handleFingerprintAuthFailed() {
        handleFingerprintHelp(-1, mContext.getString(R.string.fingerprint_not_recognized));
    }

    private void handleFingerprintAcquired(int acquireInfo) {
        if (acquireInfo != FingerprintManager.FINGERPRINT_ACQUIRED_GOOD) {
            return;
        }
    }
    private void handleFingerprintAuthenticated() {
		lockUIHandler.sendEmptyMessage(MATCH_SUCCESS);
    }

    private void handleFingerprintHelp(int msgId, String helpString) {
		Message msg = lockUIHandler.obtainMessage(MATCH_FAIL);
	    lockUIHandler.sendMessage(msg);
		showToast(helpString);
    }

    private void handleFingerprintError(int msgId, String errString) {
		if (msgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
			showToast(errString);
		}
    }

    private void onFingerprintAuthenticated(int userId) {
		Log.i(TAG, "onFingerprintAuthenticated userId: "+userId);
		lockUIHandler.sendEmptyMessage(MATCH_SUCCESS);
    }

	@Override
	public void onClick(View v) {
		android.util.Log.i(TAG,"onClick");
		switch (v.getId()) {
        case R.id.toDigital:
			android.util.Log.i(TAG,"onClick "+runKeyguardConfirmation(KEYGUARD_REQUEST));
		    if (mToast != null) {
		        mToast.cancel();
		    }
            break;		
		default:
			break;
		}
		
	}
	
    private void showFinalConfirmation() {
		android.util.Log.i(TAG,"showFinalConfirmation");
		if (PreferenceUtils.getLockUIStatus() == true) {
			if (mGlobalPackageNameString != null && !mGlobalPackageNameString.equals("fingerprint_clear_data")) {
				Settings.System.putString(getContentResolver(), PackagesConstant.SETTINGS_LAST_LOCK_APP_PACKAGENAME, mGlobalPackageNameString);
				String allTemp = getUnlockedApp();
				Log.i(TAG, "allTemp = " + allTemp);
				Settings.System.putString(getContentResolver(), "com_bird_already_unlocked_packagesname", allTemp);
			}

			if (mGlobalPackageNameString.equals("fingerprint_clear_data")) {
				Settings.System.putInt(getContentResolver(),  "com_bird_fingerprint_need_password", 0);
			}

			PreferenceUtils.setLockUIStatus(false);
			failCount = 0;
			finish();
		}
    }
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.e(TAG, "onResume");
		PreferenceUtils.setLockUIStatus(true);
		failCount = 0;
		retryFingerprint();
		String appString = Settings.System.getString(getContentResolver(),"com_bird_fingerprintunlock_needlockapp_package");
		 
		 boolean needFinish = true;
         if (appString != null) {
	        String[] appsStrings = appString.split("\\|");
             for (int i = 0; i < appsStrings.length ;i++) { 
            	 Log.d(TAG, "onResume appsStrings[i]"+appsStrings[i]);
                 if (mGlobalPackageNameString.equals(appsStrings[i])) {
					 Log.d(TAG, "onResume needFinish "+needFinish);
                	 needFinish = false;
                     return;
                 }
             }
         }

		Log.d(TAG, "onResume PreferenceUtils.isSettingFpAppLockOn() "+PreferenceUtils.isSettingFpAppLockOn());
		Log.d(TAG, "onResume needFinish-> "+needFinish);
		Log.d(TAG, "onResume mGlobalPackageNameString-> "+mGlobalPackageNameString);
		
		if ((PreferenceUtils.isSettingFpAppLockOn() == false || needFinish == true) 
		    && !"fingerprint_clear_data".equals(mGlobalPackageNameString)) {
			    finish();
		}
	}

    Toast mToast = null;
	public void showToast(String text) {
        if (mToast == null) {
	        mToast = Toast.makeText(LockUI.this, text,Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);  
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
	}
	
	public void intentToHome() {
		Intent i= new Intent(Intent.ACTION_MAIN); 
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
		i.addCategory(Intent.CATEGORY_HOME); 
		startActivity(i);
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			intentToHome();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private String getUnlockedApp() {
	    List<String> existLockAppsList = new ArrayList<String>();
	    String temp = Settings.System.getString(getContentResolver(), "com_bird_already_unlocked_packagesname");
	    if (temp != null) {
	        String[] appsStrings = temp.split("\\|");
			existLockAppsList = Arrays.asList(appsStrings);
			if (existLockAppsList.contains(mGlobalPackageNameString)) {
			    return temp;
			} else {
			    return temp + mGlobalPackageNameString + "|";
			}
	    } else {
	        return "";
	    }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.e(TAG, "onPause");
		stopFingerprint();
	}
	
	private void stopFingerprint() {
		Log.d(TAG, "stopFingerprint mFingerprintCancelSignal != null is "+(mFingerprintCancelSignal != null)
				+" mFingerprintCancelSignal.isCanceled(): "+mFingerprintCancelSignal.isCanceled());
		if (mFingerprintCancelSignal != null && !mFingerprintCancelSignal.isCanceled()) {
			mFingerprintCancelSignal.cancel();
		}
		mFingerprintCancelSignal = null;
	}	
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.e(TAG, "onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.e(TAG, "onDestroy");
	}

}
