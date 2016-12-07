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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.yunos.alicontacts.ContactsApplication;
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.dialpad.calllog.CallLogQueryHandler;
import com.yunos.alicontacts.dialpad.calllog.CallLogReceiver;
import com.yunos.alicontacts.dialpad.calllog.CallerViewQuery;
import com.yunos.alicontacts.dialpad.calllog.ContactInfo;
import com.yunos.alicontacts.dialpad.calllog.ContactInfoHelper;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.AliTextUtils;
import com.yunos.alicontacts.util.FeatureOptionAssistant;

import java.util.ArrayList;

public class GadgetCallLogService extends RemoteViewsService {
    private static final String TAG = "GadgetCallLogService";
    private static final boolean DEBUG = true;

    private static final long ONE_MINUTE = 60 * 1000;
    public static final String TAG_DIVIDER = " | ";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (DEBUG) Log.d(TAG, "service in onGetViewFactory");
        return new ListRemoteViewsFactory(getApplicationContext(), intent);
    }

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "service in onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "service in onDestory");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) Log.d(TAG, "service in onBind");
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (DEBUG) Log.d(TAG, "service in onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        if (DEBUG) Log.d(TAG, "service in onRebind");
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags,int startId) {
        if (DEBUG) Log.d(TAG, "service in onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public static class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory, CallLogQueryHandler.Listener {

        private Context mContext;
        private Intent mIntent;
        private CallLogManager mCallLogManager;
        private AliCallLogChangeListener mCallLogListener;
        private CallLogQueryHandler mCallLogQueryHandler;
        private ArrayList<CallLogInfo> mCallLogCache = new ArrayList<CallLogInfo>();
        private final ContactInfoHelper mContactInfoHelper;

        private String mRingTimeFormat;
        private String mCallLogTimeJustnow;

        private int mRedToWhiteColor;
        private int mBlackToWhiteColor;
        private int mGreyToWhiteColor;

        StringBuilder mStringBuilderTmp = new StringBuilder(128);

        /**
         * Instantiated by RemoteViewsService.onGetViewFactory
         */
        protected ListRemoteViewsFactory(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "Factory ListRemoteViewsFactory() : " + intent);
            mContext = context;
            mContactInfoHelper = ContactInfoHelper.getInstance(mContext);
            mIntent = intent;
        }

        private class AliCallLogChangeListener implements CallLogManager.CallLogChangeListener {
            @Override
            public void onCallLogChange(int changedPart) {
                Log.d(TAG, "Factory AliCallLogChangeListener onCallLogChange");
                startCallsQuery(mContext);
            }
        }

        @Override
        public void onCreate() {
            Log.d(TAG, "Factory onCreate()");

            mCallLogManager = CallLogManager.getInstance(mContext);
            mCallLogManager.requestSyncCalllogsByInit();
            mCallLogListener = new AliCallLogChangeListener();
            mCallLogManager.registCallsTableChangeListener(mCallLogListener);

            mCallLogQueryHandler = new CallLogQueryHandler(ListRemoteViewsFactory.this);
            int listType = mIntent == null ?
                    -1 : mIntent.getIntExtra(GadgetProvider.EXTRA_LIST_TYPE, -1);
            boolean showMissed = (listType == GadgetProvider.EXTRA_LIST_TYPE_MISSED);

            startCallsQuery(showMissed);

            Resources resources = mContext.getResources();
            initFormatString(resources);
            initFontColor(resources);
        }

        @Override
        public void onDataSetChanged() {
            if (DEBUG) Log.d(TAG, "Factory onDataSetChanged()");
        }

        @Override
        public void onDestroy() {
            if (DEBUG) Log.d(TAG, "Factory onDestroy()");

            mCallLogManager.unRegistCallsTableChangeListener(mCallLogListener);

            synchronized (mCallLogCache) {
                mCallLogCache.clear();
            }

            final Intent intentClear = new Intent(CallLogReceiver.ACTION_CLEAR_ALL_MISSED_CALLS);
            intentClear.setClass(mContext, CallLogReceiver.class);
            mContext.sendBroadcast(intentClear);
        }

        @Override
        public RemoteViews getLoadingView() {
            if (DEBUG) Log.d(TAG, "Factory getLoadingView()");
            RemoteViews views = new RemoteViews(mContext.getPackageName(),
                    R.layout.gadget_loading);
            ContactsApplication.overlayDyncColorResIfSupport(views);

            return views;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (DEBUG) Log.d(TAG, "Factory getViewAt() " + position);
            CallLogInfo callLoginfo;
            synchronized (mCallLogCache) {
                if (mCallLogCache.size() == 0) {
                    Log.e(TAG, "Factory getViewAt() NO Data");
                    return null;
                }
                if (position < 0 || position >= getCount()) {
                    Log.e(TAG, "Factory getViewAt() invalid position:" + position);
                    return null;
                }
                callLoginfo = mCallLogCache.get(position);
            }

            String pkgName = mContext.getPackageName();

            RemoteViews views = new RemoteViews(pkgName, R.layout.gadget_calllog_item_view);
            ContactsApplication.overlayDyncColorResIfSupport(views);

            if (position == 0) {
                views.setViewVisibility(R.id.top_divider, View.INVISIBLE);
            } else {
                views.setViewVisibility(R.id.top_divider, View.VISIBLE);
            }

            int simId = -1;
            if (SimUtil.MULTISIM_ENABLE) {
                int subId = callLoginfo.subId;
                simId = SimUtil.getSlotId(subId);
            }

            String number = callLoginfo.number;
            final int type = callLoginfo.type;
            final int features = callLoginfo.features;
            long date = callLoginfo.date;
            String countryIso = callLoginfo.countryIso;

            /**
             * Bug:5322921. APR Null Exception. Avoid exception when number is null
             * in very special case.
             */
            if (number == null) {
                number = "";
                Log.w(TAG, "getViewAt: ## call number from callog is null!!! CallID = " + callLoginfo.callId);
            }
            final String formatNumber = ContactsUtils.formatPhoneNumberWithCurrentCountryIso(number, mContext);

            final ContactInfo localCallsContactInfo = getContactInfoFromCallLog(callLoginfo);
            final ContactInfo cachedContactInfo = mContactInfoHelper.getAndCacheContactInfo(
                    number, countryIso, localCallsContactInfo);
            boolean isStrange = false;

            String labelAndNumberString = "";
            String nameStr = "";
            String contactName = cachedContactInfo.name;
            String ypName = callLoginfo.ypName;
            String province = callLoginfo.province;
            String area = callLoginfo.area;
            String location = AliTextUtils.makeLocation(province, area);
            boolean  isYP = !TextUtils.isEmpty(ypName);
            if (TextUtils.isEmpty(contactName)) {
                if (isYP) {
                    nameStr = ypName;
                    if (TextUtils.isEmpty(location)) {
                        labelAndNumberString = formatNumber;
                    } else {
                        mStringBuilderTmp.setLength(0);
                        if (!FeatureOptionAssistant.isInternationalSupportted()) {
                            mStringBuilderTmp.append(formatNumber).append(' ').append(location);
                        } else {
                            mStringBuilderTmp.append(formatNumber);
                        }
                        labelAndNumberString = mStringBuilderTmp.toString();
                    }
                } else {
                    nameStr = formatNumber;
                    if (!FeatureOptionAssistant.isInternationalSupportted()) {
                        labelAndNumberString = location;
                    }
                }
                isStrange = true;
            } else {
                nameStr = contactName;
                if (TextUtils.isEmpty(location)) {
                    labelAndNumberString = formatNumber;
                } else {
                    mStringBuilderTmp.setLength(0);
                    if (!FeatureOptionAssistant.isInternationalSupportted()) {
                        mStringBuilderTmp.append(formatNumber).append(' ').append(location);
                    } else {
                        mStringBuilderTmp.append(formatNumber);
                    }
                    labelAndNumberString = mStringBuilderTmp.toString();
                }
                isStrange = false;
            }

            mStringBuilderTmp.setLength(0);
            mStringBuilderTmp.append(nameStr);
            long ringTime = callLoginfo.ringTime;
            long duration = callLoginfo.duration;

            if (callLoginfo.groupItemCount > 1) {
                mStringBuilderTmp.append(" (").append(callLoginfo.groupItemCount).append(')');
            }
            setNameViewStyle(views, type, ringTime, mStringBuilderTmp.toString());
            bindCallTypeAndFreaturesView(views, callLoginfo, duration, type, features,
                    simId, isStrange, labelAndNumberString);
            // for unknown number;private number; payed number
            setSpecialNumberLabel(number, views);

            String dateString;
            long currentTime = System.currentTimeMillis();
            if (currentTime - date < ONE_MINUTE) {
                dateString = mCallLogTimeJustnow;
            } else {
                dateString = DateUtils.getRelativeTimeSpanString(date, currentTime,
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE).toString();
                dateString = dateString.replaceAll("/", "-");
            }
            views.setTextViewText(R.id.date, dateString);

            final Intent fillInIntent = GadgetProvider.getCallDirectly(mContext, number);
            views.setOnClickFillInIntent(R.id.numInfo, fillInIntent);
            return views;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getCount() {
            return mCallLogCache.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        public void startCallsQuery(final Context context) {
            new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    return GadgetProvider.getCalllogUnreadCount(context);
                }

                @Override
                protected void onPostExecute(Integer result) {
                    super.onPostExecute(result);
                    Intent updateIntent =
                            new Intent(GadgetProvider.ACTION_CONTACTS_APPWIDGET_UPDATE);
                    updateIntent.putExtra(GadgetProvider.EXTRA_UNREAD_COUNT, result);
                    updateIntent.setClass(context, GadgetProvider.class);
                    context.sendBroadcast(updateIntent);
                    startCallsQuery(result > 0);
                }
            }.execute();
        }

        public void startCallsQuery(boolean showMissed) {
            if (mCallLogQueryHandler == null) return;
            if (DEBUG) Log.d(TAG, "startCallsQuery : " + showMissed);
            mCallLogQueryHandler.queryCalls(showMissed ?
                    CallLogQueryHandler.QUERY_CALLS_NEW_MISSED : CallLogQueryHandler.QUERY_CALLS_ALL);
        }

        private void setNameViewStyle(RemoteViews views,
                int type, long ringTime, String nameStr) {
            if (type == Calls.MISSED_TYPE) {
                if (ringTime != 0) {
                    String ringStr = AliCallLogExtensionHelper.formatCallTypeLabel(mContext,
                            mRingTimeFormat, -1, ringTime);
                    StringBuilder bdStr = new StringBuilder();
                    bdStr.append('(').append(ringStr).append(')');
                    SpannableString ss = new SpannableString(nameStr + bdStr);
                    int len = nameStr.length() + bdStr.length();
                    ss.setSpan(new ForegroundColorSpan(mRedToWhiteColor), 0, nameStr.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    float textSize = viewHolder.labelAndNumber.getTextSize();
//                    ss.setSpan(new AbsoluteSizeSpan((int) textSize), nameStr.length(), len,
//                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new ForegroundColorSpan(mGreyToWhiteColor), nameStr.length(), len,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    views.setTextViewText(R.id.name, ss);
                } else {
                    views.setTextColor(R.id.name, mRedToWhiteColor);
                    views.setTextViewText(R.id.name, nameStr);
                }
            } else {
                views.setTextColor(R.id.name, mBlackToWhiteColor);
                views.setTextViewText(R.id.name, nameStr);
            }
        }

        private void bindCallTypeAndFreaturesView(RemoteViews views, CallLogInfo callLogInfo,
                long duration, int type, int features,
                int simId, boolean isStrange, String labelAndNumberString) {
            int resId = 0;
            if (type == Calls.MISSED_TYPE) {
                if (simId == SimUtil.SLOT_ID_1) {
                    resId = R.drawable.ic_dial_doublecard_miss1;
                } else if (simId == SimUtil.SLOT_ID_2) {
                    resId = R.drawable.ic_dial_doublecard_miss2;
                } else {
                    resId = R.drawable.ic_dial_miss;
                }
            } else if (type == Calls.INCOMING_TYPE) {
                if (simId == SimUtil.SLOT_ID_1) {
                    resId = duration == 0 ? R.drawable.ic_dial_doublecard_stop1
                            : R.drawable.ic_dial_doublecard_received1;
                } else if (simId == SimUtil.SLOT_ID_2) {
                    resId = duration == 0 ? R.drawable.ic_dial_doublecard_stop2
                            : R.drawable.ic_dial_doublecard_received2;
                } else {
                    resId = duration == 0 ? R.drawable.ic_dial_stop : R.drawable.ic_dial_received;
                }
            } else if (type == Calls.OUTGOING_TYPE) {
                if (simId == SimUtil.SLOT_ID_1) {
                    resId = R.drawable.ic_dial_doublecard_called1;
                } else if (simId == SimUtil.SLOT_ID_2) {
                    resId = R.drawable.ic_dial_doublecard_called2;
                } else {
                    resId = R.drawable.ic_dial_called;
                }
            }
            views.setImageViewResource(R.id.call_type_indicator, resId);

            if ((features & CallerViewQuery.CALL_FEATURES_BIT_VIDEO) != 0) {
                views.setImageViewResource(R.id.call_features_indicator,
                        R.drawable.ic_callhistory_facetime_normal);
                views.setViewVisibility(R.id.call_features_indicator, View.VISIBLE);
            } else if ((features & CallerViewQuery.CALL_FEATURES_BIT_HD) != 0) {
                views.setImageViewResource(R.id.call_features_indicator,
                        R.drawable.ic_callhistory_hd_normal);
                views.setViewVisibility(R.id.call_features_indicator, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.call_features_indicator, View.GONE);
            }

            // Bind tag type if any tag type marked.
            bindTagTypeView(callLogInfo, views, isStrange, labelAndNumberString);
        }

        private void bindTagTypeView(CallLogInfo callLogInfo, RemoteViews views, boolean isStrange,
                String labelAndNumberString) {
            mStringBuilderTmp.setLength(0);
            mStringBuilderTmp.append(labelAndNumberString);
            String tagName = callLogInfo.tagName;
            int markedCount = callLogInfo.markedCount;
            if (isStrange) {
                if (!TextUtils.isEmpty(tagName)) {
                    if (!TextUtils.isEmpty(labelAndNumberString)) {
                        mStringBuilderTmp.append(TAG_DIVIDER);
                    }
                    if (markedCount > 0) {
                        tagName = ((markedCount == 1) ? mContext.getString(
                                R.string.system_tag_type_parameter_for_one, tagName) : mContext
                                .getString(R.string.system_tag_type_parameter, markedCount, tagName));
                    }
                    mStringBuilderTmp.append(tagName);
                }
            }

            views.setTextViewText(R.id.labelAndNumber, mStringBuilderTmp.toString());
        }

        private void setSpecialNumberLabel(String number, RemoteViews views) {
            int resId = AliCallLogExtensionHelper.getSpecialNumber(number);
            if (resId > 0) {
                views.setTextViewText(R.id.name, mContext.getString(resId));
            }
        }

        /** Returns the contact information as stored in the call log. */
        private ContactInfo getContactInfoFromCallLog(CallLogInfo callLogInfo) {
            // Fill the same content as ContactInfo.fromLocalCallsCursor(Cursor);
            ContactInfo info = new ContactInfo();
            info.lookupUri = callLogInfo.lookupUri;
            info.name = callLogInfo.name;
            info.type = callLogInfo.numberType;
            info.label = callLogInfo.label;
            info.number = callLogInfo.matchedNumber;
            info.normalizedNumber = callLogInfo.normalizedNumber;
            info.photoId = callLogInfo.photoId;
            info.photoUri = callLogInfo.photoUri;
            info.formattedNumber = callLogInfo.formattedNumber;
            return info;

        }

        private void initFormatString(Resources res) {
            mRingTimeFormat = res.getString(R.string.ringing_time);
            mCallLogTimeJustnow = res.getString(R.string.calllog_time_just_now);
        }

        private void initFontColor(Resources res) {
            mRedToWhiteColor = res.getColor(R.drawable.aui_ic_color_red_to_white);
            mBlackToWhiteColor = res.getColor(R.drawable.aui_ic_color_black_to_white);
            mGreyToWhiteColor = res.getColor(R.drawable.aui_ic_color_grey_to_white);
        }

        @Override
        public boolean onCallsFetched(Cursor cursor) {
            int count = cursor == null ? -1 : cursor.getCount();
            Log.i(TAG, "onCallsFetched: count="+count);
            if (count <= 0) {
                synchronized (mCallLogCache) {
                    mCallLogCache.clear();
                }
                // Refresh list view now
                notifyRefresh();
                return false;
            }

            if (count == 1) {
                cursor.moveToFirst();
                CallLogInfo info = getValueFromCursor(cursor);
                info.groupItemCount = 1;
                synchronized (mCallLogCache) {
                    mCallLogCache.clear();
                    mCallLogCache.add(info);
                }
                // Refresh list view now
                notifyRefresh();
                return false;
            }

            ArrayList<CallLogInfo> tmpCache = new ArrayList<CallLogInfo>(count);
            cursor.moveToFirst();
            String latestNumber = cursor.getString(CallerViewQuery.NUMBER);
            int groupItemCount = 1;
            CallLogInfo info = getValueFromCursor(cursor);
            while (cursor.moveToNext()) {
                final String currentNumber = cursor.getString(CallerViewQuery.NUMBER);
                final boolean sameNumber = equalNumbers(latestNumber, currentNumber);
                if (sameNumber) {
                    groupItemCount++;
                    continue;
                }
                info.groupItemCount = groupItemCount;
                tmpCache.add(info);
                groupItemCount = 1;
                latestNumber = currentNumber;
                info = getValueFromCursor(cursor);
                if (cursor.isLast()) {
                    info.groupItemCount = groupItemCount;
                    tmpCache.add(info);
                }
            }

            if (groupItemCount > 1) {
                info.groupItemCount = groupItemCount;
                tmpCache.add(info);
            }

            synchronized (mCallLogCache) {
                mCallLogCache.clear();
                mCallLogCache.addAll(tmpCache);
                Log.d(TAG, "onCallsFetched X: " + mCallLogCache.size());
            }

            // Refresh list view now
            notifyRefresh();
            return false;
        }

        private void notifyRefresh() {
            // Refresh list view now
            AppWidgetManager widgetMgr = AppWidgetManager.getInstance(mContext);
            int[] ids = widgetMgr.getAppWidgetIds(GadgetProvider.getComponentName(mContext));
            widgetMgr.notifyAppWidgetViewDataChanged(ids, R.id.call_log_list);
            return;
        }

        private static CallLogInfo getValueFromCursor(final Cursor cursor) {
            CallLogInfo info = new CallLogInfo();
            info.subId = cursor.getInt(CallerViewQuery.SIMID);
            info.callId = cursor.getLong(CallerViewQuery.ID);
            info.number = cursor.getString(CallerViewQuery.NUMBER);
            info.type = cursor.getInt(CallerViewQuery.TYPE);
            info.features = cursor.getInt(CallerViewQuery.FEATURES);
            info.date = cursor.getLong(CallerViewQuery.DATE);
            info.countryIso = cursor.getString(CallerViewQuery.COUNTRY_ISO);
            info.ypName = cursor.getString(CallerViewQuery.SHOP_NAME);
            info.province = cursor.getString(CallerViewQuery.LOC_PROVINCE);
            info.area = cursor.getString(CallerViewQuery.LOC_AREA);
            info.tagName = cursor.getString(CallerViewQuery.TAG_NAME);
            info.markedCount = cursor.getInt(CallerViewQuery.MARKED_COUNT);
            info.ringTime = cursor.getLong(CallerViewQuery.RING_TIME);
            info.duration = cursor.getLong(CallerViewQuery.DURATION);

            info.lookupUri = cursor.getString(CallerViewQuery.LOOKUP_URI);
            info.name = cursor.getString(CallerViewQuery.NAME);
            info.numberType = cursor.getInt(CallerViewQuery.NUMBER_TYPE);
            info.label = cursor.getString(CallerViewQuery.NUMBER_LABEL);
            info.matchedNumber = cursor.getString(CallerViewQuery.MATCHED_NUM);
            info.normalizedNumber = cursor.getString(CallerViewQuery.NORMALIZED_NUM);
            info.photoId = cursor.getLong(CallerViewQuery.PHOTO_ID);
            info.photoUri = cursor.getString(CallerViewQuery.PHOTO_URI);
            info.formattedNumber = cursor.getString(CallerViewQuery.FORMATTED_NUM);
            return info;
        }

        private static boolean equalNumbers(final String number1, final String number2) {
            return PhoneNumberUtils.compare(number1, number2);
        }

        private static class CallLogInfo {
            int subId;
            long callId;
            String number;
            int type;
            int features;
            long date;
            String countryIso;
            String ypName;
            String province;
            String area;
            String tagName;
            int markedCount;
            long ringTime;
            long duration;

            String lookupUri;
            String name;
            int numberType;
            String label;
            String matchedNumber;
            String normalizedNumber;
            long photoId;
            String photoUri;
            String formattedNumber;

            int groupItemCount;

            public CallLogInfo() {
            }

            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append("CallLogInfo [subId=").append(subId);
                builder.append(", callId=").append(callId);
                builder.append(", number=").append(number);
                builder.append(", type=").append(type);
                builder.append(", features=").append(features);
                builder.append(", date=").append(date);
                builder.append(", countryIso=").append(countryIso);
                builder.append(", ypName=").append(ypName);
                builder.append(", province=").append(province);
                builder.append(", area=").append(area);
                builder.append(", tagName=").append(tagName);
                builder.append(", markedCount=").append(markedCount);
                builder.append(", ringTime=").append(ringTime);
                builder.append(", duration=").append(duration);
                builder.append(", groupItemCount=").append(groupItemCount);
                builder.append(", lookupUri=").append(lookupUri);
                builder.append(", name=").append(name);
                builder.append(", numberType=").append(numberType);
                builder.append(", label=").append(label);
                builder.append(", matchedNumber=").append(matchedNumber);
                builder.append(", normalizedNumber=").append(normalizedNumber);
                builder.append(", photoId=").append(photoId);
                builder.append(", photoUri=").append(photoUri);
                builder.append(", formattedNumber=").append(formattedNumber);
                builder.append("]");
                return builder.toString();
            }
        }

    }
}
