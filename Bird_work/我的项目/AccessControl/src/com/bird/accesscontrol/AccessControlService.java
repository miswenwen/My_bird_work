/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import java.util.List;

import com.bird.widget.MyService;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
//liuqipeng add
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.provider.Settings;
import java.util.TreeMap;
import android.content.Context;
import java.util.SortedMap;
import android.os.Build;
import android.util.Log;
//liuqipeng end
public class AccessControlService extends MyService {
	private ActivityManager mActivityManager;
	private static final int intGetTaskCounter = 1;
	private Intent Turn2ConfrimPassword;
	private Bundle mBundle = new Bundle();
	private KeyguardManager mKeyguardManager;
	private boolean isClear = false;

	public void init()
	{
		if(!Pref.getEnableAccessControl(this) && !Pref.getEnableAccessControlPassword(this))//lvhuaiyi add !Pref.getEnableAccessControlPassword(this)
		{	        
			android.os.Process.killProcess(android.os.Process.myPid());
			stopSelf();
		}
		else
		{
			mActivityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE); 
			mKeyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
			if(!Pref.PASSWORD_NUMBER_TYPE)//lvhuaiyi add this line
			Turn2ConfrimPassword = new Intent(this, ConfirmLockPattern.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
			//lvhuaiyi add begin
			else if(Pref.FINGERPRIINT_TYPE)
			Turn2ConfrimPassword = new Intent(this, ConfirmScreen.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);				
			else
			Turn2ConfrimPassword = new Intent(this, ConfirmPassword.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);								
			//lvhuaiyi add end
			Utils.allowedProtectPackage.clear();
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		init();
		super.onCreate();
		startHandler();
	}
//liuqipeng add
public String getTopActivtyFromLolipopOnwards(){
    String topPackageName = "";
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        // We get usage stats for the last 10 seconds
        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 2000, time);
        // Sort the stats by the last time used
        if(stats != null) {
            SortedMap<Long,UsageStats> mySortedMap = new TreeMap<Long,UsageStats>();
            for (UsageStats usageStats : stats) {
                mySortedMap.put(usageStats.getLastTimeUsed(),usageStats);
            }
            if(mySortedMap != null && !mySortedMap.isEmpty()) {
                topPackageName =  mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                Log.e("TopPackage Name",topPackageName);
            }
        }
    }
	return topPackageName;
}
//liuqipeng end
	@Override
	protected void onUpdate() {
		//liuqipeng log off begin
		/*
		String topPackageName;
		List<ActivityManager.RunningTaskInfo> mRunningTasks = mActivityManager.getRunningTasks(intGetTaskCounter);
		boolean isLocked = mKeyguardManager.inKeyguardRestrictedInputMode();
		
		if (isLocked)
		{
			if (!isClear)
			{
				Utils.allowedProtectPackage.clear();
				isClear = true;
			}
		}
		else
		{
			if (isClear)
			{
				isClear = false;
			}
		}

		for (ActivityManager.RunningTaskInfo amTask : mRunningTasks)
		{
			topPackageName = amTask.topActivity.getPackageName();
			//lvhuaiyi add begin
			if(PasswordUtils.isHome(getApplicationContext(),topPackageName)){
				if(!Utils.allowedProtectPackage.isEmpty())
				Utils.allowedProtectPackage.clear();
			}
			//lvhuaiyi add end
			if(Data.isInAccessControl(this, topPackageName)
				&& !Utils.allowedProtectPackage.contains(topPackageName)
				&& !amTask.topActivity.getClassName().equals(ConfirmLockPattern.class.getName())
				&& !amTask.topActivity.getClassName().equals(ConfirmPassword.class.getName())
				&& !amTask.topActivity.getClassName().equals("com.nbbsw.theme.ThemeSwitchActivity")
				&& !amTask.topActivity.getClassName().contains("com.android.incallui")
				&& !amTask.topActivity.getClassName().equals("com.baidu.baidumaps.WelcomeScreen")
				&& !amTask.topActivity.getClassName().equals(ConfirmScreen.class.getName()))//lvhuaiyi add this line
			{
				mBundle.putString(Utils.EXTRA_PACKAGE_NAME, topPackageName);
				Turn2ConfrimPassword.putExtras(mBundle);
				startActivity(Turn2ConfrimPassword);
				break;
			}
		}
		*/
		//liuqipeng log off end
		//liuqipeng add begin
		String topPackageName;

		boolean isLocked = mKeyguardManager.inKeyguardRestrictedInputMode();
		
		if (isLocked)
		{
			if (!isClear)
			{
				Utils.allowedProtectPackage.clear();
				isClear = true;
			}
		}
		else
		{
			if (isClear)
			{
				isClear = false;
			}
		}
		topPackageName = getTopActivtyFromLolipopOnwards();
		if(Data.isInAccessControl(this, topPackageName)
			&& !Utils.allowedProtectPackage.contains(topPackageName)
			&& !topPackageName.equals(ConfirmLockPattern.class.getName())
			&& !topPackageName.equals(ConfirmPassword.class.getName())
			&& !topPackageName.equals("com.nbbsw.theme.ThemeSwitchActivity")
			&& !topPackageName.contains("com.android.incallui")
			&& !topPackageName.equals("com.baidu.baidumaps.WelcomeScreen")
			&& !topPackageName.equals(ConfirmScreen.class.getName()))
		{
			mBundle.putString(Utils.EXTRA_PACKAGE_NAME, topPackageName);
			Turn2ConfrimPassword.putExtras(mBundle);
			startActivity(Turn2ConfrimPassword);
		}
		//liuqipeng add end
	}
}
