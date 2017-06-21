package com.bird.accesscontrol;

//import com.fingerprints.fpc1080Mobile.DataUtil;
//import com.fingerprints.fpc1080Mobile.SensorNative;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.SystemProperties;
import android.content.Intent;
import android.util.Log;

public class EnrolStart extends Activity {
    private final static String TAG = "Accesscontrol.EnrolStart";
	private String mHandFingerString;
	private static DataUtil mDataUtil;
	private ImageView mEnrolStartImage;
	private RelativeLayout mEnrollingLayout;
	private TextView mEnrollingTextView;
	private TextView mEnrollStartTextView;
	private AnimationDrawable mEnrollingScrollAnimation;
	private int mGoodSwip = 0;
	private ImageView mEnrollingScroll;
	//private Handler mHandler;
	private int mStatus = -1;
	private static String[] mTemplateList = new String[1];
	private boolean mStopped = false;
	private boolean mFingerEnroled = false;
	private String[]mVerifiedResult;

	
    public EnrolStart() {
       // mHandler = new Handler(this);
    }

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(SystemProperties.getBoolean("ro.bird.fingerprint.before", false))
        setContentView(R.layout.enrol_start_layout);
        else
        setContentView(R.layout.enrol_start_layout_back);
        mEnrolStartImage = (ImageView)findViewById(R.id.enrol_start_image);
        mEnrollingLayout = (RelativeLayout)findViewById(R.id.enrolling_layout);
        mEnrollingTextView = (TextView)findViewById(R.id.enrolling_textView);
        mEnrollStartTextView = (TextView)findViewById(R.id.enrol_start_prompt);
        mEnrollingScroll = (ImageView)findViewById(R.id.enrolling_scroll);
        mEnrollingScrollAnimation = (AnimationDrawable) mEnrollingScroll.getBackground();
        mGoodSwip = 0;    

        final Resources res = getResources();
        mVerifiedResult=res.getStringArray(R.array.app_lock_modify_arrays);
        mDataUtil = new DataUtil();
        mHandFingerString = getIntent().getStringExtra("handandfinger");
        setTitle(mHandFingerString);
        mFingerEnroled = mDataUtil.hasTemplate(this, Pref.HANDFINGERINDEX);
        mTemplateList[0]=mDataUtil.getPathForFinger(this, Pref.HANDFINGERINDEX);
        Log.d(TAG,"onCreated ,mFingerEnroled ="+mFingerEnroled);
        if(mFingerEnroled){
            startFingerVerify();
        }else{
        	  // startFingerEnrol();
        	  startActivityForResult(new Intent(this, ConfirmPassword.class).putExtra(Utils.EXTRA_PREF, Pref.FINGERPRINT_PROTECT), Utils.REQUEST_CONFIRM_PASSWORD);
        }
    }

	protected void onActivityResult(int resquestcode, int resultcode, Intent data){  
        Log.d(TAG,"onActivityResult,resultcode ="+resultcode);
        if(resquestcode==Utils.REQUEST_CONFIRM_PASSWORD 
        && resultcode==RESULT_OK 
        && data.getStringExtra(Utils.EXTRA_PREF).equals(Pref.FINGERPRINT_PROTECT)) {
            Log.d(TAG,"onActivityResult,finish.");
			finish();
		}
	}	
	
    @Override
    protected void onResume() {
        super.onResume();       
        mGoodSwip = 0;  
        if(!mFingerEnroled){
            mDataUtil.invalidateTemplateData();
            reStartFingerEnrol();
        }else{
            mStopped = false;
            startFingerVerify();        	
        }       
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mFingerEnroled) mStopped = true;
        SensorNative.abort();
    }

    public void startFingerEnrol() { 
        String templateName;
        templateName = mDataUtil.getPathForFinger(this, Pref.HANDFINGERINDEX);
        Log.d(TAG,"startFingerEnrol ");
        
        SensorNative.startEnrol(mSensorHander, templateName, Pref.HANDFINGERINDEX);       
    }
    
    public void reStartFingerEnrol() {
        String templateName;
        templateName = mDataUtil.getPathForFinger(this, Pref.HANDFINGERINDEX);
        Log.d(TAG,"reStartFingerEnrol ");
        SensorNative.startEnrol(mSensorHander, templateName, Pref.HANDFINGERINDEX);       
    }    
    
    public void startFingerVerify() {
        if (mStopped) return;
        SensorNative.startVerify(mSensorHander, mTemplateList);     
    }    
    
    public void reStartFingerVerify() {
        if (mStopped) return;    	
        SensorNative.startVerify(mSensorHander, mTemplateList);     
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
        	if(mFingerEnroled){
        		mEnrollingTextView.setText(R.string.enrol_verify_try_again);
        		reStartFingerVerify();
        	}else{
        	    reStartFingerEnrol();
        	}
            break;
        case SensorNative.MSG_FINGER_PRESENT:
            break;
        case SensorNative.MSG_SWIPE_PROGRESS:
            if(mEnrolStartImage.getVisibility() == View.VISIBLE 
        	&& mEnrollingLayout.getVisibility() == View.GONE){
        		mEnrolStartImage.setVisibility(View.GONE);
        		mEnrollingLayout.setVisibility(View.VISIBLE);
        		mEnrollStartTextView.setText(R.string.enrol_starting_prompt_text);
        	}
            
          mEnrollingScroll.setBackgroundResource(R.anim.enrolling_scroll_anim);
          if(mGoodSwip == 0){
              mEnrollingScroll.setBackgroundResource(R.anim.finger_normal);
          }else if(mGoodSwip == 1){
             mEnrollingScroll.setBackgroundResource(R.anim.finger_one);
          }else if(mGoodSwip == 2){
              mEnrollingScroll.setBackgroundResource(R.anim.finger_two);
          }else{
              mEnrollingScroll.setBackgroundResource(R.anim.finger_normal);
          } 
          mEnrollingScrollAnimation = (AnimationDrawable) mEnrollingScroll.getBackground();
          mEnrollingScrollAnimation.start();         
        	if(!mEnrollingTextView.getText().equals(getResources().getString(R.string.enrolling_fingerprint))){
        		mEnrollingTextView.setText(R.string.enrolling_fingerprint);
        	}
            break;
        case SensorNative.MSG_PROCESSING:
            break;
        case SensorNative.MSG_SUCCESSFUL:
        	if(mFingerEnroled){
        	    //finish();
              mDataUtil.invalidateTemplateData();
              mEnrollingScroll.setBackgroundResource(R.anim.finger_verify);
              mEnrollingScrollAnimation = (AnimationDrawable) mEnrollingScroll.getBackground();
              mEnrollingScrollAnimation.start();
		      AlphaAnimation mForAnimationTime=new AlphaAnimation(1.0f,1.0f); 		 
		      mForAnimationTime.setDuration(1100); 
              mEnrollingScroll.startAnimation(mForAnimationTime); 
              mForAnimationTime.setAnimationListener(new AnimationListener() {
			            @Override
			            public void onAnimationEnd(Animation arg0) {
			                showVerified();
			            }
			            @Override
			            public void onAnimationRepeat(Animation arg0) {
			            }
			            @Override
			            public void onAnimationStart(Animation arg0) {
			            }        	
			            });                                        
        	}else{
        		  mGoodSwip++;
              if(mGoodSwip == 3){
                	mDataUtil.invalidateTemplateData();
                  mEnrollingScroll.setBackgroundResource(R.anim.finger_three_after);
                  mEnrollingScrollAnimation = (AnimationDrawable) mEnrollingScroll.getBackground();
                  mEnrollingScrollAnimation.start();
		              AlphaAnimation mForAnimationTime=new AlphaAnimation(1.0f,1.0f); 		 
		              mForAnimationTime.setDuration(750); 
                  mEnrollingScroll.startAnimation(mForAnimationTime); 
                  mForAnimationTime.setAnimationListener(new AnimationListener() {
			                @Override
			                public void onAnimationEnd(Animation arg0) {
			                    showEnrolSucceed();
			                }
			                @Override
			                public void onAnimationRepeat(Animation arg0) {
			                }
			                @Override
			                public void onAnimationStart(Animation arg0) {
			                }        	
			                });                                        
              }

        	}
            break;
        case SensorNative.MSG_WAITING_FOR_SWIPE:
        	if(mEnrollingLayout.getVisibility() == View.VISIBLE){
        		if(mFingerEnroled){
        		    mEnrollingTextView.setText(R.string.enrol_verify_try_again);
        		}else{
        		    mEnrollingTextView.setText(R.string.enrol_fingerprint_try_again);
        		}
        	}
            switch (mStatus) {
            case -1:
        		if(mFingerEnroled){
        		    mEnrollingTextView.setText(R.string.enrol_verify_try_again);
        		}else{
        		    mEnrollingTextView.setText(R.string.enrol_fingerprint_try_again);
        		}
                break;
            case SensorNative.MSG_GOOD_SWIPE:
            	mEnrollingTextView.setText(R.string.enrol_fingerprint_swipe_again);
                break;
            case SensorNative.MSG_QUESTIONABLE_SWIPE:
                //SWIPE_QUALITY swipe_quality = SensorNative.getImageQuality();
                //mEnrollingTextView.setText(getQualityString(swipe_quality));
                break;
            case SensorNative.MSG_WRONG_FINGER:
            	mEnrollingTextView.setText(R.string.wrong_finger);
                break;
            };
            break;
        case SensorNative.MSG_GOOD_SWIPE:
            mGoodSwip++;
            if(mGoodSwip == 0){
                mEnrollingScroll.setBackgroundResource(R.anim.finger_normal);
            }else if(mGoodSwip == 1){
                mEnrollingScroll.setBackgroundResource(R.anim.finger_one_after);
            }else if(mGoodSwip == 2){
                mEnrollingScroll.setBackgroundResource(R.anim.finger_two_after);
            }
            mEnrollingScrollAnimation = (AnimationDrawable) mEnrollingScroll.getBackground();
            mEnrollingScrollAnimation.start();        
            mStatus = SensorNative.MSG_GOOD_SWIPE;
            break;
        case SensorNative.MSG_QUESTIONABLE_SWIPE:     
            mStatus = SensorNative.MSG_QUESTIONABLE_SWIPE;
            break;
        case SensorNative.MSG_WRONG_FINGER:
            mStatus = SensorNative.MSG_WRONG_FINGER;
            break;
            /*
        case SensorNative.MSG_DEBUG_CAPTURED_BITMAP:
        	if(mEnrolStartImage.getVisibility() == View.VISIBLE 
        	&& mEnrollingLayout.getVisibility() == View.GONE){
        		mEnrolStartImage.setVisibility(View.GONE);
        		mEnrollingLayout.setVisibility(View.VISIBLE);
        		mEnrollStartTextView.setText(R.string.enrol_starting_prompt_text);
        	}
            break;  */
        default:
            break;
        }        
	}	
	
    private int getQualityString(int swipe_quality) {
        switch(swipe_quality) {
        //case SWIPE_GOOD:
        //    return R.string.swipe_good;
        case SensorNative.SWIPE_TOO_FAST:
            return R.string.swipe_too_fast;
        case SensorNative.SWIPE_TOO_MUCH_ANGLE:
            return R.string.swipe_too_skewed;
        case SensorNative.SWIPE_TOO_SHORT:
            return R.string.swipe_too_short;
        case SensorNative.SWIPE_NOT_CENTERED:
            return R.string.swipe_not_centered;
        default:
            return R.string.try_again;
        }
    }


	private void showVerified(){
		final Builder builder = new Builder(this); 
		builder.setTitle(R.string.enrol_verify_modify_title);
		builder.setCancelable(false); 
		builder.setItems(mVerifiedResult, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item){
				
				switch (item){
				    case 0:
				    	onDeleteFinger();
						finish();
					    break;
				    case 1:
				    	onReAcquireFinger();
				    	break;
				    default:
				    	break;
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				builder.create().dismiss();
				finish();
			}			
		});
		builder.show();
	}
	
	private void showEnrolSucceed(){
		final Builder builder = new Builder(this); 
		builder.setTitle(R.string.enrol_succeed_title);
		builder.setCancelable(false); 
		builder.setMessage(R.string.enrol_succeed_message);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				builder.create().dismiss(); 
                finish();
			}			
		});		
		builder.show();
	}	

    private void onDeleteFinger() {
    	mDataUtil.deleteTemplate(this, Pref.HANDFINGERINDEX);
    }	
    
    private void onReAcquireFinger(){
    	setTitle(getString(R.string.enrol_activity));
    	onDeleteFinger();
    	mFingerEnroled = false;
    	reStartFingerEnrol();
		mEnrolStartImage.setVisibility(View.VISIBLE);
		mEnrollingLayout.setVisibility(View.GONE);
		mEnrollStartTextView.setText(R.string.enrol_start_prompt_text);
    }
	
}
