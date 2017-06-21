/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import com.bird.ninekeylock.NineKeyLockListener;
import com.bird.ninekeylock.NineKeyLockView;
import com.bird.widget.MyActivity;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;

public class ConfirmLockPattern extends MyActivity 
	implements NineKeyLockListener
{
	private ActivityManager mActivityManager;
	private Vibrator mVibrator;
	private String packageName = null;
	private TextView headerText;
	private NineKeyLockView lockPattern;
	private PackageManager mPackageManager;

	private void checkFail()
	{
		headerText.setText(R.string.lockpattern_need_to_unlock_wrong);
	}

	private void checkPass()
	{
		if(packageName != null && (Pref.getEnableAccessControl(this)||Pref.getEnableAccessControlPassword(this)))//lvhuaiyi add Pref.getEnableAccessControlPassword(this)
		{
			Utils.allowedProtectPackage.add(packageName);
		}
		setResult(RESULT_CANCELED, getIntent());
		finish();
	}

	private void goHome()
	{
		if(packageName != null)
		{
			mActivityManager.killBackgroundProcesses(packageName);
			Intent backhome= new Intent("android.intent.action.MAIN");	
			backhome.addCategory("android.intent.category.HOME");
			backhome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);			
			startActivity(backhome);
		}
		else
		{
			setResult(RESULT_OK, getIntent());
		}

		finish();
	}
	
	private void setNewPackage()
	{
		packageName = getIntent().getStringExtra(Utils.EXTRA_PACKAGE_NAME);
		
		if (packageName != null)
		{
			try {
				String label = mPackageManager.getApplicationInfo(packageName, 0)
					.loadLabel(mPackageManager).toString();
				headerText.setText(label
					+ getString(R.string.lockpassword_confirm_your_pattern_sub_title_2)
					+ getString(R.string.lockpassword_confirm_your_pattern_sub_title));
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			headerText.setText(R.string.lockpassword_confirm_your_pattern_sub_title);
		}
	}

	public void onBackPressed()
	{
		goHome();
	}

	public void onCreate(Bundle paramBundle)
	{
		super.onCreate(paramBundle);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.confirm_lock_pattern);

		mActivityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
		headerText = (TextView)findViewById(R.id.headerText);
		mPackageManager = getPackageManager();

		setNewPackage();

		lockPattern = (NineKeyLockView)findViewById(R.id.lockPattern);
		lockPattern.setListener(this);
		lockPattern.setMakePatternVisible(Pref.getMakePatternVisible(this));
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		boolean result = false;

		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			goHome();
			result = true;
		}

		if (!result)
		{
			result = super.onKeyDown(keyCode, event);
		}

		return result;
	}

	@Override
	public void beforeInput(NineKeyLockView nineKeyLockView) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInput(NineKeyLockView nineKeyLockView) {
		nineKeyLockView.checkPassed(Pref.checkPassword(this, nineKeyLockView.getPassword()));
	}

	@Override
	public void beforeCheck(NineKeyLockView nineKeyLockView, boolean passed) {
		if (passed)
		{
			checkPass();
		}
		else
		{
			checkFail();
		}
	}

	@Override
	public void afterCheck(NineKeyLockView nineKeyLockView, boolean passed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddHit(NineKeyLockView nineKeyLockView) {
		if (Pref.getVibrateOnTouch(this))
		{
			mVibrator.vibrate(50);
		}
	}
}