package com.example.liuapidemos.broadcast;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent arg1) {
		// TODO Auto-generated method stub
		Toast.makeText(context, "shit123", Toast.LENGTH_SHORT).show();

		AlertDialog.Builder mBuilder = new AlertDialog.Builder(context);
		mBuilder.setTitle("Warning");
		mBuilder.setMessage("You are forced to be offline. Please try to login again.");
		mBuilder.setCancelable(false);
		Log.e("sdf", "sdf1");
		mBuilder.setPositiveButton("OK", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int arg1) {
				// TODO Auto-generated method stub
				ActivityCollector.finishAll();
				Intent mIntent = new Intent(context, BroadcastMainActivity.class);
				mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(mIntent);
				Log.e("sdf", "sdf2");
			}
		});
		AlertDialog mAlertDialog = mBuilder.create();
		mAlertDialog.getWindow().setType(
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		mAlertDialog.show();
		Log.e("sdf", "sdf3");
			}

}
