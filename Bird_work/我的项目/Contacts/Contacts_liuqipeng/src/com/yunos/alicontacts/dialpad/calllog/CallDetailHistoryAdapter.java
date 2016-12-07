/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.yunos.alicontacts.dialpad.calllog;

import android.content.Context;
import android.content.Intent;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/* YUNOS BEGIN */
//##modules(AliContacts) ##author: hongwei.zhw
//##BugID:(8161644) ##date:2016.4.18
//##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
import android.os.Build;
/* YUNOS END */
import com.yunos.alicontacts.CallDetailActivity;
import com.yunos.alicontacts.PhoneCallDetails;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.plugins.PluginPlatformPrefs;
import com.yunos.alicontacts.util.DateUtils;
import com.yunos.common.DebugLog;
import com.yunos.common.UsageReporter;

/**
 * Adapter for a ListView containing history items from the details of a call.
 */
public class CallDetailHistoryAdapter extends BaseAdapter {
    private static final String TAG = "CallDetailHistoryAdapter";
    /** The top element is a blank header, which is hidden under the rest of the UI. */
    //private static final int VIEW_TYPE_HEADER = 0;
    /** Each history item shows the detail of a call. */
    private static final int VIEW_TYPE_HISTORY_ITEM = 1;
    private static final String ONLY_YEAR_FORMAT = "yyyy";
    private static final String MONTH_DAY_FORMAT = "MM-dd ";
    private static final String FULL_FORMAT = "yyyy-MM-dd ";

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final PhoneCallDetails[] mPhoneCallDetails;
    public CallDetailHistoryAdapter(Context context, LayoutInflater layoutInflater,
            CallTypeHelper callTypeHelper, PhoneCallDetails[] phoneCallDetails) {
        mContext = context;
        mLayoutInflater = layoutInflater;
        mPhoneCallDetails = phoneCallDetails;
    }

    @Override
    public int getCount() {
        return mPhoneCallDetails.length;
    }

    @Override
    public Object getItem(int position) {
/*        if (position == 0) {
            return null;
        }*/
        return mPhoneCallDetails[position];
    }

    @Override
    public long getItemId(int position) {
/*        if (position == 0) {
            return -1;
        }*/
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
//        if (position == 0) {
//            return VIEW_TYPE_HEADER;
//        }
        return VIEW_TYPE_HISTORY_ITEM;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Make sure we have a valid convertView to start with
//        final View result  = convertView == null
//                ? mLayoutInflater.inflate(R.layout.ali_call_detail_history_item, parent, false)
//                : convertView;

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.ali_call_detail_history_item, parent,
                    false);
            viewHolder = new ViewHolder();
            viewHolder.callTypeTextView = (TextView) convertView.findViewById(R.id.call_type_text);
            viewHolder.tvDuration = (TextView)convertView.findViewById(R.id.call_detail_listitem_duration);
            viewHolder.tvDate = (TextView)convertView.findViewById(R.id.call_detail_listitem_calldate);
            viewHolder.recordBtn = (ImageView)convertView.findViewById(R.id.call_detail_listitem_record_btn);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final PhoneCallDetails details = mPhoneCallDetails[position];
        final int callType = details.callType;
        final int callFeatures = details.callFeatures;
        if ((callFeatures & CallerViewQuery.CALL_FEATURES_BIT_VIDEO) != 0) {
            int pxFor8dp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
            viewHolder.callTypeTextView.setCompoundDrawablePadding(pxFor8dp);
            viewHolder.callTypeTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, R.drawable.ic_callhistory_facetime_normal, 0);
        } else if ((callFeatures & CallerViewQuery.CALL_FEATURES_BIT_HD) != 0) {
            int pxFor8dp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
            viewHolder.callTypeTextView.setCompoundDrawablePadding(pxFor8dp);
            viewHolder.callTypeTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, R.drawable.ic_callhistory_hd_normal, 0);
        } else {
            viewHolder.callTypeTextView.setCompoundDrawablePadding(0);
            viewHolder.callTypeTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, 0, 0);
        }

        // BEGIN: set recording button visibility, added by fangjun.lin
        DebugLog.d(TAG, "phoneRecordPath = " + details.mPhoneRecordPath);
        if (TextUtils.isEmpty(details.mPhoneRecordPath)) {
            viewHolder.recordBtn.setVisibility(View.GONE);
        } else {
            viewHolder.recordBtn.setVisibility(View.VISIBLE);

            viewHolder.recordBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, CallLogRecordActivity.class);
                    intent.putExtra(CallLogRecordActivity.EXTRA_RECORD_PATH, details.mPhoneRecordPath);
                    intent.putExtra(CallLogRecordActivity.EXTRA_CALL_URI, details.callUri);
                    mContext.startActivity(intent);
                    UsageReporter.onClick(null, CallDetailActivity.class.getSimpleName(), UsageReporter.DialpadPage.CALL_RECORD);
                }

            });
        }
        // END

        viewHolder.tvDuration.setVisibility(View.VISIBLE);

        CharSequence dateValue;
        String timeFormat = DateFormat.getTimeFormatString(mContext);
        if (AliCallLogExtensionHelper.dateFormat(ONLY_YEAR_FORMAT, details.date).equals(
                AliCallLogExtensionHelper.dateFormat(ONLY_YEAR_FORMAT, System.currentTimeMillis()))) {
            dateValue = AliCallLogExtensionHelper.dateFormat(MONTH_DAY_FORMAT, timeFormat, details.date);
        } else {
            dateValue = AliCallLogExtensionHelper.dateFormat(FULL_FORMAT, timeFormat, details.date);
        }
        viewHolder.tvDuration.setText(dateValue); //Excahnge date and duration positon
        viewHolder.tvDate.setText(
                DateUtils.getCalllogItemDurationString(mContext, callType, details.duration));

        if(callType == Calls.MISSED_TYPE) {
            //tvDate.setVisibility(View.GONE);
            //viewHolder.tvDuration.setVisibility(View.GONE);
            viewHolder.tvDate.setTextColor(mContext.getResources().getColor(
                    R.drawable.aui_ic_color_red_to_white));
        } else {
            //tvDate.setVisibility(View.VISIBLE);
            //viewHolder.tvDuration.setVisibility(View.VISIBLE);
            viewHolder.tvDate.setTextColor(mContext.getResources().getColor(
                    R.color.call_detail_history_item_txt_color));
        }
        if(PluginPlatformPrefs.isCMCC()) {
            viewHolder.tvDuration.setVisibility(View.GONE);
        }

        /* YUNOS BEGIN */
        //##modules(AliContacts) ##author: hongwei.zhw
        //##BugID:(8161644) ##date:2016.4.18
        //##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
        if (Build.YUNOS_CARRIER_CMCC) {
            viewHolder.tvDuration.setVisibility(View.VISIBLE);
            viewHolder.tvDate.setVisibility(View.GONE);
        }
        /* YUNOS END */
        bindCallTypeView(viewHolder.callTypeTextView, callType, details.slotId, details.ringTime);
        return convertView;
    }

    private void bindCallTypeView(TextView textview, int type, int simId, long ringTime) {
        if(mContext == null) {
            Log.e(TAG, "bindCallTypeView: mContext is null.");
            return;
        }
        if (type == Calls.MISSED_TYPE) {
            // Check isRingOnce
            String format2 = mContext.getString(R.string.ringing_time);
            if (ringTime == 0) {
                String format = mContext.getString(R.string.calllog_detail_calltype_missed);
                String defaultFormat = mContext.getString(R.string.missed_call);
                textview.setText(AliCallLogExtensionHelper.formatCallTypeLabel(mContext, format,
                        defaultFormat, simId));
            } else {
                textview.setText(AliCallLogExtensionHelper.formatCallTypeLabel(mContext, format2,
                        simId, ringTime));
            }
            textview.setTextColor(mContext.getResources().getColor(
                    R.drawable.aui_ic_color_red_to_white));
        } else if (type == Calls.INCOMING_TYPE) {
            String format = mContext.getString(R.string.calllog_detail_calltype_in);
            String defaultFormat = mContext.getString(R.string.incoming_call);
            textview.setText(AliCallLogExtensionHelper.formatCallTypeLabel(mContext, format,
                    defaultFormat, simId));
            textview.setTextColor(mContext.getResources().getColor(
                    R.color.call_detail_history_item_txt_color));
        } else if (type == Calls.OUTGOING_TYPE) {
            String format = mContext.getString(R.string.calllog_detail_calltype_out);
            String defaultFormat = mContext.getString(R.string.outgoing_call);
            textview.setText(AliCallLogExtensionHelper.formatCallTypeLabel(mContext, format,
                    defaultFormat, simId));
            textview.setTextColor(mContext.getResources().getColor(
                    R.color.call_detail_history_item_txt_color));
        }
    }

    // BEGIN: user view holder, added by fangjun.lin
    public static class ViewHolder {
        TextView callTypeTextView;
        TextView tvDuration;
        TextView tvDate;
        ImageView recordBtn;
    }
    // END
}
