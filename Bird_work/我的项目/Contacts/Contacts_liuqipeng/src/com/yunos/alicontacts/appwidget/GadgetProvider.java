/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunos.alicontacts.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.widget.RemoteViews;

import com.yunos.alicontacts.ContactsApplication;
import com.yunos.alicontacts.R;
import com.yunos.common.UsageReporter;

import java.util.Arrays;

/**
 * Simple widget to show next upcoming contacts event.
 */
public class GadgetProvider extends AppWidgetProvider {
    public static final String TAG = "GadgetProvider";
    static final boolean DEBUG = true;

    public static String EXTRA_FROM_GADGET = "widget_extra_from_gadget";
    public static String EXTRA_USER_TRACKER_INFO = "widget_extra_user_tracker_info";
    public static String EXTRA_LIST_TYPE = "widget_extra_list_type";
    public static String EXTRA_UNREAD_COUNT = "widget_extra_unread_count";
    public static int EXTRA_LIST_TYPE_MISSED = 100;
    public static int EXTRA_LIST_TYPE_ALL = 200;

    public static String ACTION_CONTACTS_APPWIDGET_UPDATE =
            "com.yunos.alicontacts.appwidget.action.APPWIDGET_UPDATE";

    private class UpdateTask extends AsyncTask<Void, Void, Integer> {
        final Context mContext;
        final Intent mIntent;

        public UpdateTask(Context c, Intent i) {
            mContext = c;
            mIntent = i;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return getCalllogUnreadCount(mContext);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            performUpdate(mContext, appWidgetManager,
                    appWidgetManager.getAppWidgetIds(getComponentName(mContext)),
                    mIntent, result);
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        if (DEBUG) Log.d(TAG, "onReceive(), action=" + action);

        if (ACTION_CONTACTS_APPWIDGET_UPDATE.equals(action)) {
            int unreadCount = intent.getIntExtra(EXTRA_UNREAD_COUNT, 0);
            if (DEBUG) Log.d(TAG, "ACTION_CONTACTS_APPWIDGET_UPDATE : " + unreadCount);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            performUpdate(context, appWidgetManager,
                    appWidgetManager.getAppWidgetIds(getComponentName(context)),
                    intent, unreadCount);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        if (DEBUG) Log.d(TAG, "onAppWidgetOptionsChanged ");
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onEnabled(final Context context) {
        if (DEBUG) Log.d(TAG, "onEnabled ");
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        if (DEBUG) Log.d(TAG, "onDisabled ");
        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (DEBUG) Log.d(TAG, "onDeleted(" + Arrays.toString(appWidgetIds) + ") ");
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (DEBUG) Log.d(TAG, "onUpdate ");
        new UpdateTask(context, null).execute();
    }

    private void performUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds, Intent intent, int unreadCount) {
        Log.i(TAG, "performUpdate(" + Arrays.toString(appWidgetIds) +
                ") , unreadCount = " + unreadCount);

        Resources res = context.getResources();

        // Launch over to service so it can perform update
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.gadget);
            ContactsApplication.overlayDyncColorResIfSupport(views);

            //Header
            views.setInt(R.id.header, "setBackgroundColor", res.getColor(R.color.appwidget_bg));
            if (unreadCount > 0) {
                views.setTextViewText(R.id.title, res.getString(R.string.gadget_title_missed_calls));
            }

            //List
            Intent listIntent = new Intent(context, GadgetCallLogService.class);
            listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            listIntent.setData(Uri.parse(listIntent.toUri(Intent.URI_INTENT_SCHEME)));

            if (unreadCount > 0) {
                listIntent.putExtra(EXTRA_LIST_TYPE, EXTRA_LIST_TYPE_MISSED);
            } else {
                listIntent.putExtra(EXTRA_LIST_TYPE, EXTRA_LIST_TYPE_ALL);
            }

            views.setRemoteAdapter(R.id.call_log_list, listIntent);

            views.setEmptyView(R.id.call_log_list, R.id.empty_view);
            int color = res.getColor(R.color.appwidget_btn_bg_color_normal);
            views.setInt(R.id.empty_icon, "setImageResource", R.drawable.card_icon_empty_selector);
            views.setInt(R.id.empty_icon, "setColorFilter", color);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.call_log_list);
            views.setOnClickPendingIntent(R.id.empty_view, null);

            // Each list item will call setOnClickExtra() to let the list know
            // which item is selected by a user.
            PendingIntent viewItemPiTemplate = getPendingIntentTemplate(context);
            views.setPendingIntentTemplate(R.id.call_log_list, viewItemPiTemplate);

            // Footer
            PendingIntent createEventPi = getDialPendingIntent(context);
            views.setOnClickPendingIntent(R.id.footer, createEventPi);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, GadgetProvider.class);
    }

    private static PendingIntent getDialPendingIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_FROM_GADGET, true);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Build a {@link PendingIntent} to launch the Contacts app. This should be used
     * in combination with {@link RemoteViews#setPendingIntentTemplate(int, PendingIntent)}.
     */
    private static PendingIntent getPendingIntentTemplate(Context context) {
        Intent launchIntent = new Intent(Intent.ACTION_CALL);
        Bundle bundle = new Bundle();
        bundle.putString("user_tracker_app_key", UsageReporter.APP_KEY);
        bundle.putString("user_tracker_page_name", GadgetProvider.TAG);
        bundle.putString("user_tracker_widget_name", UsageReporter.GadgetPage.GADGET_PHONE_MAKE_CALL);
        launchIntent.putExtra(EXTRA_USER_TRACKER_INFO, bundle);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Intent getCallDirectly(Context context, String number) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("tel:" + number));
        intent.putExtra(EXTRA_FROM_GADGET, true);
        return intent;
    }

    /*
     * Notice: widget showing maybe early than contacts app init,
     * So we cannot checkout app's provider.
     */
    public static int getCalllogUnreadCount(Context context) {
        Cursor cursor = null;
        ContentResolver resolver = context.getContentResolver();
        cursor = resolver.query(CallLog.Calls.CONTENT_URI, new String[]{CallLog.Calls._ID},
                CallLog.Calls.TYPE + "=? AND " + CallLog.Calls.NEW + "=1",
                new String[]{String.valueOf(CallLog.Calls.MISSED_TYPE)},
                CallLog.Calls.DEFAULT_SORT_ORDER);
        int count = cursor == null ? -1 : cursor.getCount();
        Log.i(TAG, "getCalllogUnreadCount: " + count);

        return count;
    }
}
