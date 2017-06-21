/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;


import java.lang.reflect.Field;

import com.bird.widget.MyActivity;
//import com.fingerprints.fpc1080Mobile.DataUtil;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import java.util.List;
import android.app.ActivityManager.RecentTaskInfo;

public class ConfirmPassword extends MyActivity 
{

	private View mPasswordView;
	private TextView mPassword;
	private TextView mConfirmPassword;
	private EditText mConfirmPasswordEdit;
	private EditText mPasswordEdit;
	@Override
	public void onCreate(Bundle paramBundle)
	{
		super.onCreate(paramBundle);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.password_background);
		String password = PasswordUtils.getPasswordSharedPreferences(this, null);
		initview(password);
	}

	private void initview(final String password){
		LayoutInflater  inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);   
		mPasswordView = inflater.inflate(R.layout.confirm_password, null); 
		mPassword= (TextView)mPasswordView.findViewById(R.id.textview_password);
		mConfirmPassword= (TextView)mPasswordView.findViewById(R.id.textview_confrim_password);
		mConfirmPasswordEdit = (EditText)mPasswordView.findViewById(R.id.confirm_password);
		mPasswordEdit = (EditText) mPasswordView.findViewById(R.id.input_password);
		if(password != null){
			mPassword.setText(R.string.verify_password);
			mConfirmPassword.setVisibility(View.GONE);
			mConfirmPasswordEdit.setVisibility(View.GONE);
		}
		AlertDialog.Builder mPasswordBuilder = new AlertDialog.Builder(this).setCancelable(false)
				.setPositiveButton(R.string.app_lock_ok, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String inputPassword = mPasswordEdit.getText().toString().trim();
						String confirmPassword = mConfirmPasswordEdit.getText().toString().trim();
						HideKeyboard();
						if(password != null){
							if(!confirmPassword(password,inputPassword)){
								noCloseDialog(dialog);
							}else{
								dealConfirmProtectSuccess();					
								closeDialog(dialog);
							}
						}else{
							if(!enrolPassword(inputPassword,confirmPassword)){
								noCloseDialog(dialog);
							}else{
								closeDialog(dialog);
							}
						}
					}})
				.setNegativeButton(R.string.app_lock_cancel,  new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int arg1) {
							HideKeyboard();
							String mFrom = getIntent().getStringExtra(Utils.EXTRA_PREF);	
						    if(mFrom != null && (mFrom.equals(Pref.SELECR_PROTECT_APPLICATION)||mFrom.equals(Pref.FINGERPRINT_PROTECT)))
						    setResult(RESULT_OK, getIntent());								
						    if(mFrom == null)
						    dealConfirmProtectFail();
						    closeDialog(dialog);					
						}
					}).setView(mPasswordView);
		AlertDialog mPasswordDialog = mPasswordBuilder.create();
		mPasswordDialog.show();		
	}
	
	private boolean enrolPassword(String inputPassword,String confirmPassword){
		if(inputPassword == null || confirmPassword == null){
			showInfo(R.string.app_lock_pwd_empty);
			return false;
		}else if(inputPassword.equals("") || confirmPassword.equals("")){
			showInfo(R.string.app_lock_pwd_empty);
			return false;
		}else if(!inputPassword.equals(confirmPassword)){
			showInfo(R.string.app_lock_chang_password_error3);
			return false;
		}else{
			PasswordUtils.setPasswordEditor(this, inputPassword);	
			return true;
		}
		
	}
	private boolean confirmPassword(String password,String inputPassword){
		String mFrom = getIntent().getStringExtra(Utils.EXTRA_PREF);		
		if(inputPassword == null ||inputPassword.equals("")||!inputPassword.equals(password)){
			showInfo(R.string.app_lock_pwd_missmatch);
			return false;
		}else{
			if(mFrom != null && mFrom.equals(Pref.ENABLE_ACCESS_CONTROL_PASSWORD)){
		    	Data.emptyAccessControl(this);
				PasswordUtils.removeSharedPreferences(this);
				DataUtil mDataUtil = new DataUtil();
				mDataUtil.deleteTemplate(this, Pref.HANDFINGERINDEX);
			}
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
		String mFrom = getIntent().getStringExtra(Utils.EXTRA_PREF);
		
		try{  
		    Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");  
		    field.setAccessible(true);  
		    field.set(dialog, true); 
		    if(mFrom != null && mFrom.equals(Pref.ENABLE_ACCESS_CONTROL_PASSWORD))
		    {	
		    	setResult(RESULT_CANCELED, getIntent());
		    }
		    finish();
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
			/*Intent backhome= new Intent("android.intent.action.MAIN");	
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
  private void HideKeyboard()
  {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);     
      if(imm.isActive()) {     
        imm.hideSoftInputFromWindow(mPasswordView.getApplicationWindowToken(),0);     
       }    
  }
  private void removeTask(String packageName){
      final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
      if (am == null) {
         return;    
      }	
  	  try{
          List<RecentTaskInfo> list = am.getRecentTasks(2, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
          List<ActivityManager.RunningTaskInfo> mRunningTasks = am.getRunningTasks(2); 
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
  		    android.util.Log.e("ConfirmPassword", "no permission " +e);
  	  }        
  }
  
  private void goHome(){
			Intent backhome= new Intent("android.intent.action.MAIN");	
			backhome.addCategory("android.intent.category.HOME");
			backhome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);			
			startActivity(backhome);		  	
  }    
}
