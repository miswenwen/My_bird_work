package com.yunos.alicontacts.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class PluginStatusReceiver extends BroadcastReceiver {

	private static final String TAG = "PluginStatusReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
			String packageName = intent.getData().getSchemeSpecificPart();
			Log.i(TAG, "install " + packageName);

			(new PluginAsyncTask()).execute(context);
		}
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
			String packageName = intent.getData().getSchemeSpecificPart();
			Log.i(TAG, "uninstall " + packageName);

			(new PluginAsyncTask()).execute(context);
		}
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
			String packageName = intent.getData().getSchemeSpecificPart();
			Log.i(TAG, "replace " + packageName);

			(new PluginAsyncTask()).execute(context);
		}

	}

	private class PluginAsyncTask extends AsyncTask<Context, Void, Void> {
		@Override
		protected Void doInBackground(Context... params) {
			final Context context = params[0];
			PluginManager.getInstance(context).initPlugins();
			return null;
		}

		public void execute(Context context) {
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, context);
		}
	}

}
