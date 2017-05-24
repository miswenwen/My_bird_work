package com.example.birdsalesstatistics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast; 

public class NetworkChangeReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager connectionManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isAvailable()) {
			Log.e("liuqipeng", "network is available");
			Toast.makeText(context, "network is available", Toast.LENGTH_SHORT)
					.show();
			SharedPreferences mPreferences=context.getSharedPreferences("data", Context.MODE_PRIVATE);
			boolean firstDo=mPreferences.getBoolean("firstdo", true);
			if (firstDo) {
				doSome(context);
			}
		} else {
			Log.e("liuqipeng", "network is unavailabll");
			Toast.makeText(context, "network is unavailable",
					Toast.LENGTH_SHORT).show();
		}

	}

	private void doSome(Context context) {
//		Intent mIntent = new Intent(context, MainActivity.class);
//		mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		context.startActivity(mIntent);
		Intent mIntent=new Intent(context,SalesService.class);
		context.startService(mIntent);
	}
}
