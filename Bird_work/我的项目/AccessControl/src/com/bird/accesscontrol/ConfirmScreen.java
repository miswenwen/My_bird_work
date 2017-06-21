package com.bird.accesscontrol;

import java.lang.reflect.Field;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bird.widget.MyActivity;
//import com.fingerprints.fpc1080Mobile.DataUtil;
//import com.fingerprints.fpc1080Mobile.SensorNative;
//import com.fingerprints.fpc1080Mobile.SensorNative.SWIPE_QUALITY;

import java.util.List;
import android.app.ActivityManager.RecentTaskInfo;


public class ConfirmScreen extends MyActivity {

	private ImageView fingerShowImage;
	private TextView fingerShowText;
	private TextView passwordShow;	
	private String password;
	//private Handler mHandler;
	private AnimationDrawable mVerifyAnimation;
	private static String[] mTemplateList = new String[1];
	private boolean mHasEnrol = false;
	private DataUtil mDataUtil;
	private static int mMaxFingerVerify = 0;
	@Override
	public void onCreate(Bundle paramBundle)
	{
		super.onCreate(paramBundle);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.confirm_screen);
		fingerShowImage = (ImageView) findViewById(R.id.finger_show);
		fingerShowText = (TextView) findViewById(R.id.finger_show_info);
		passwordShow = (TextView) findViewById(R.id.password_show_info);
		mVerifyAnimation = (AnimationDrawable) fingerShowImage.getBackground();
		password = PasswordUtils.getPasswordSharedPreferences(this, null);
		passwordShow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				SensorNative.abort();
				showPasswordConfirm();
			}
		});	
		mDataUtil = new DataUtil();
		mHasEnrol = mDataUtil.hasTemplate(this, Pref.HANDFINGERINDEX);
		mTemplateList[0]=mDataUtil.getPathForFinger(this, Pref.HANDFINGERINDEX);  
		//mHandler = new Handler(this);
		if(mHasEnrol){
			fingerShowText.setText(R.string.entry_by_fingerPrint);			
			startFingerVerify();
		}else{
			fingerShowText.setTextColor(Color.RED);
			fingerShowText.setText(R.string.fingerPrint_no_enroled);		
	  }
	}

	private void showPasswordConfirm() {
		LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View passwordConfirmView = inflater.inflate(R.layout.confirm_password, null); 
		TextView mConfirmPassword= (TextView)passwordConfirmView.findViewById(R.id.textview_confrim_password);
		EditText mConfirmPasswordEdit = (EditText)passwordConfirmView.findViewById(R.id.confirm_password);
		final EditText mPasswordEdit = (EditText) passwordConfirmView.findViewById(R.id.input_password);
		mConfirmPassword.setVisibility(View.GONE);
		mConfirmPasswordEdit.setVisibility(View.GONE);
		AlertDialog.Builder mPasswordBuilder = new AlertDialog.Builder(this).setCancelable(false)
		    .setPositiveButton(R.string.app_lock_ok, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String inputPassword = mPasswordEdit.getText().toString().trim();
				    if(!confirmPassword(password, inputPassword)){
				    	noCloseDialog(dialog);
				    }else{
				    	HideKeyboard(passwordConfirmView);
				    	dealConfirmProtectSuccess();
				    	closeDialog(dialog);
				    	finish();
				    }
				}
		    })
		    .setNegativeButton(R.string.app_lock_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//dealConfirmProtectFail();
          if(mMaxFingerVerify<5){
              if(mHasEnrol) reStartFingerVerify();
              closeDialog(dialog);
          }else{
              dealConfirmProtectFail();
              finish();
          }
				}
			}).setView(passwordConfirmView);
		AlertDialog mPasswordDialog = mPasswordBuilder.create();
		mPasswordDialog.show();			
	}
	private boolean confirmPassword(String password,String inputPassword){
		if(inputPassword == null ||inputPassword.equals("")||!inputPassword.equals(password)){
			showInfo(R.string.app_lock_pwd_missmatch);
			return false;
		}else{
			return true;
		}
	}
	private void noCloseDialog(DialogInterface dialog){
		try{  
		    Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");  
		    field.setAccessible(true);  
		    field.set(dialog, false);  
		}catch(Exception e) {  
		    e.printStackTrace();  
		}		
	}
	private void closeDialog(DialogInterface dialog){	
		try{  
		    Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");  
		    field.setAccessible(true);  
		    field.set(dialog, true); 
		    //finish();
		}catch(Exception e) {  
		    e.printStackTrace();  
		}		
	}	
	private void dealConfirmProtectFail(){
		String packageName = getIntent().getStringExtra(Utils.EXTRA_PACKAGE_NAME);
		ActivityManager mActivityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		if(packageName != null){
		  //removeTask(packageName);
			//mActivityManager.killBackgroundProcesses(packageName);
			mActivityManager.forceStopPackage(packageName);
			/*lvhuaiyi remove
			Intent backhome= new Intent("android.intent.action.MAIN");	
			backhome.addCategory("android.intent.category.HOME");
			backhome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);			
			startActivity(backhome);*/
		}
	}
	private void dealConfirmProtectSuccess(){
		String packageName = getIntent().getStringExtra(Utils.EXTRA_PACKAGE_NAME);
		if(packageName != null && Pref.getEnableAccessControlPassword(this))
		{
			Utils.allowedProtectPackage.add(packageName);	
		}
	}	
	private void showInfo(int id){
		PasswordUtils.showInfo(this,id);
	}
    private void HideKeyboard(View v)
    {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);     
        if(imm.isActive()){     
          imm.hideSoftInputFromWindow(v.getApplicationWindowToken(),0);     
        }    
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK){
			dealConfirmProtectFail();
			finish();
			return true;
		}else{
			return super.onKeyDown(keyCode, event);
		}
	}

	
	SensorMsgHandler mSensorHander = new SensorMsgHandler();
    class SensorMsgHandler extends Handler {     
        @Override
        public void handleMessage(Message msg) {
            SensorMessageHandler(msg);
        }
    }
	
	public void SensorMessageHandler(Message msg) {
		// TODO Auto-generated method stub
        switch (msg.what) {
        case SensorNative.MSG_FAILURE:
        	fingerShowText.setTextColor(Color.RED);
        	mMaxFingerVerify++;
        	if(mMaxFingerVerify<5){
        	    fingerShowText.setText(R.string.entry_by_fingerPrint_error);
        	    reStartFingerVerify();
        	}else{
              fingerShowText.setText(R.string.overflow_finger_verify_max);
              SensorNative.abort();
              showPasswordConfirm();         		
        	}
          break;
        case SensorNative.MSG_FINGER_PRESENT:
            break;
        case SensorNative.MSG_SWIPE_PROGRESS:
        	fingerShowText.setTextColor(Color.BLACK);
        	fingerShowText.setText(R.string.verifying_fingerprint);  
        	fingerShowImage.setBackgroundResource(R.anim.enrolling_scroll_anim);
        	fingerShowImage.setBackgroundResource(R.anim.finger_normal);
        	mVerifyAnimation = (AnimationDrawable) fingerShowImage.getBackground();
        	mVerifyAnimation.start();         	
            break;
        case SensorNative.MSG_PROCESSING:
            break;
        case SensorNative.MSG_SUCCESSFUL:
        	fingerShowText.setTextColor(Color.BLACK);
        	fingerShowText.setText(R.string.entry_by_fingerPrint_succeed);        	
        	dealConfirmProtectSuccess();
        	finish();
            break;
        case SensorNative.MSG_WAITING_FOR_SWIPE:
            break;
        case SensorNative.MSG_GOOD_SWIPE:
            break;
        case SensorNative.MSG_QUESTIONABLE_SWIPE:     
            break;
        case SensorNative.MSG_WRONG_FINGER:
            break;
       // case SensorNative.MSG_DEBUG_CAPTURED_BITMAP:
       //     break;
        default:
            break;
        }        
	}	
	   
    public void startFingerVerify() {
        SensorNative.startVerify(mSensorHander, mTemplateList);     
    }	
    public void reStartFingerVerify() {
        SensorNative.startVerify(mSensorHander, mTemplateList);     
    }

    @Override
    public void onResume() {
        //if (mHandler == null) {
         //   mHandler = new Handler(this);
        //}
        if(mHasEnrol){  
        	  mMaxFingerVerify = 0;
            reStartFingerVerify();
        }
        super.onResume();
        SensorNative.SetSecurityThreshold(DataUtil.getSecuritLevel(this));
    }
    @Override
    public void onPause() {
      	if(mHasEnrol){
            SensorNative.abort();
        }
        super.onPause();
    }    
    private void removeTask(String packageName){
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return;    
        }	
    	  try{
            List<RecentTaskInfo> list = am.getRecentTasks(2, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
            if(list.size() == 2){
          	    RecentTaskInfo info = list.get(1);
                Intent intent = new Intent(info.baseIntent);
                if (info.origActivity != null) {
                    intent.setComponent(info.origActivity);
                }
                if(intent.getComponent().getPackageName().equals(packageName)){
                    am.removeTask(info.persistentId); 
                }else{
                    goHome();	
                }
            }else{
                goHome();	
            }
        }catch(SecurityException e){
    		    android.util.Log.e("ConfirmScreen", "no permission " +e);
    	  }        
    }

    private void goHome(){
	  		Intent backhome= new Intent("android.intent.action.MAIN");	
		  	backhome.addCategory("android.intent.category.HOME");
			  backhome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);			
			  startActivity(backhome);		  	
    }      
    
}
