package com.yunos.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.yunos.alicontacts.group.GroupManager;

public class LocaleChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if ((intent == null) || (!Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction()))) {
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                GroupManager gm = GroupManager.getInstance(context.getApplicationContext());
                gm.checkLocaleForDefaultGroups();
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}
