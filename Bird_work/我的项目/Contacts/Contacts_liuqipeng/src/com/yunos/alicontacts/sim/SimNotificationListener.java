
package com.yunos.alicontacts.sim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.yunos.alicontacts.R;

public class SimNotificationListener {
    private static final String TAG = "SimNotificationListener";
    private final Context mContext;
    private final NotificationManager mNotificationManager;

    /* package */ static final String SIM_NOTIFICATION_TAG = "sim";

    public SimNotificationListener(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    private static Notification constructProgressNotification(
            Context context, int type,
            String tickerText, String description, String content,
            int slot, int jobId, int totalCount, int currentCount) {

        final Uri uri = (new Uri.Builder())
                .scheme("invalidscheme")
                .authority("invalidauthority")
                .appendQueryParameter(CancelActivity.JOB_ID, String.valueOf(jobId))
                .appendQueryParameter(CancelActivity.SLOT, String.valueOf(slot))
                .appendQueryParameter(CancelActivity.TYPE, String.valueOf(type)).build();
        final Intent intent = new Intent(context, CancelActivity.class);
        intent.setData(uri);
        final Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true)
                .setProgress(totalCount, currentCount, totalCount == -1)
                .setTicker(tickerText)
                .setContentTitle(description)
                .setContentText(content)
                .setSmallIcon(type == SimContactListFragment.ACTION_MODE_IMPORT_CONTACTS
                        ? android.R.drawable.stat_sys_download
                        : android.R.drawable.stat_sys_upload)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        type == SimContactListFragment.ACTION_MODE_IMPORT_CONTACTS
                                ? R.drawable.vcard_import
                                : R.drawable.vcard_export))
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));

        return builder.build();
    }

    public void onProcessing(int type, String displayName, int slot, int jobId, int totalCount,
            int currentCount) {

        String description;
        switch (type) {
            case SimContactListFragment.ACTION_MODE_IMPORT_CONTACTS:
                description = mContext.getString(R.string.sim_import_process_description,
                        displayName);
                break;
            case SimContactListFragment.ACTION_MODE_EXPORT_CONTACTS:
                description = mContext.getString(R.string.sim_export_process_description,
                        displayName);
                break;
            default:
                Log.d(TAG, "onProcessing() type:" + type);
                return;
        }

        String content = null;
        if (totalCount > 0) {
            content = mContext.getString(R.string.percentage,
                    String.valueOf(currentCount * 100 / totalCount));
        }

        final Notification notification = constructProgressNotification(
                mContext, type, description, description, content,
                slot, jobId, totalCount, currentCount);
        mNotificationManager.notify(SIM_NOTIFICATION_TAG, jobId, notification);
    }

    public void onProcessQuit(int type, int jobId) {
        mNotificationManager.cancel(SIM_NOTIFICATION_TAG, jobId);
    }
}
