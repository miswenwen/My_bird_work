/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import java.lang.reflect.Field;

import com.bird.widget.MyActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

public class ModifyPassword extends MyActivity {
	private EditText mCurPassword;
	private EditText mNewPassword1;
	private EditText mNewPassword2;
	private View mPasswordView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.password_background);
		initview();
	}
	private void initview(){
		LayoutInflater  inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);   
		mPasswordView = inflater.inflate(R.layout.modify_password, null);
		mCurPassword = (EditText)mPasswordView.findViewById(R.id.input_cur_password);
		mNewPassword1 = (EditText)mPasswordView.findViewById(R.id.input_new_password);
		mNewPassword2 = (EditText)mPasswordView.findViewById(R.id.confirm_new_password);
		AlertDialog.Builder mPasswordBuilder = new AlertDialog.Builder(this).setCancelable(false)
				.setPositiveButton(R.string.app_lock_ok, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String mCurPasswordText = mCurPassword.getText().toString().trim();
						String mNewPasswordText1 = mNewPassword1.getText().toString().trim();
						String mNewPasswordText2 = mNewPassword2.getText().toString().trim();						
						if(!confirmPassword(mCurPasswordText,mNewPasswordText1,mNewPasswordText2)){
							noCloseDialog(dialog);
						}else{
							closeDialog(dialog);
						}
					}})
				.setNegativeButton(R.string.app_lock_cancel,  new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							closeDialog(dialog);
						}
						
					}).setView(mPasswordView);
		AlertDialog mPasswordDialog = mPasswordBuilder.create();
		mPasswordDialog.show();
	}   
	private boolean confirmPassword(String curPasswordText,String inputPasswordText1,String inputPasswordText2){
		String password = PasswordUtils.getPasswordSharedPreferences(this, null);
		if(curPasswordText == null ||inputPasswordText1 == null || inputPasswordText2 == null ||
		   curPasswordText.equals("") ||inputPasswordText1.equals("") || inputPasswordText2.equals("")){
			showInfo(R.string.app_lock_pwd_empty);
			return false;
		}else if(!inputPasswordText1.equals(inputPasswordText2)){
			showInfo(R.string.app_lock_chang_password_error2);
            return false;			
		}else if(!curPasswordText.equals(password)){
			showInfo(R.string.app_lock_chang_password_error1);
            return false;
		}else{
			showInfo(R.string.app_lock_reset_pwd_succeed);
			PasswordUtils.setPasswordEditor(this, inputPasswordText1);
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
		    finish();
		}catch(Exception e) {  
		    e.printStackTrace();  
		}		
	}
	private void showInfo(int id){
		PasswordUtils.showInfo(this,id);
	}	
}