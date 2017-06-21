/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;

public class AccessControlReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();

		if (action.equals(Intent.ACTION_BOOT_COMPLETED))
		{
			context.stopService(new Intent(context, AccessControlService.class));

			if (Pref.getEnableAccessControl(context)|| Pref.getEnableAccessControlPassword(context))//lvhuaiyi add Pref.getEnableAccessControlPassword(context)
			{
				context.startService(new Intent(context, AccessControlService.class));
			}
		}
		//lvhuaiyi add begin
		if(action.equals("android.intent.action.APP_LOCK_START")){
		  if(Pref.getEnableAccessControl(context)|| Pref.getEnableAccessControlPassword(context)){
		  	boolean mServiceRunning = isRunnning(context);
		  	if(mServiceRunning){
		  		if(!Utils.allowedProtectPackage.isEmpty()) Utils.allowedProtectPackage.clear();
		  	}
		  	if(!mServiceRunning){
		  	  context.startService(new Intent(context, AccessControlService.class));
		  	}
		  }
		}
		//lvhuaiyi add end
	}
	//lvhuaiyi add begin
	public boolean isRunnning(Context context)  
	{
    ActivityManager mManager=(ActivityManager)context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);  
	  ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) mManager.getRunningServices(20);  
	  for(int i = 0 ; i<runningService.size();i++)  
	  {  
	    if(runningService.get(i).service.getClassName().toString().equals("com.bird.accesscontrol.AccessControlService"))  
	    {  
	      return true;  
	    }  
	  }  
	  return false;  
	}
	 //lvhuaiyi add end
}
