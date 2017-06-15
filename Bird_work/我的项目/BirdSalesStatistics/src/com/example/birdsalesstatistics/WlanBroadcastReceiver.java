package com.example.birdsalesstatistics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.widget.Toast;

public class WlanBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
//			Toast.makeText(context, "wifi可以用了", Toast.LENGTH_SHORT).show();
			doSome(context);
		} else if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
//			Toast.makeText(context, "wifi不能用了", Toast.LENGTH_SHORT).show();
		}
	}
	private void doSome(Context context) {
		// TODO Auto-generated method stub
//		Intent mIntent = new Intent(context, MainActivity.class);
//		mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		context.startActivity(mIntent);
		Intent mIntent=new Intent(context,SalesService.class);
		context.startService(mIntent);
	}
}
