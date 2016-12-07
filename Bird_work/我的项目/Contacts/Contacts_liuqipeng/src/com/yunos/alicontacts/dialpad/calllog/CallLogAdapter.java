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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yunos.alicontacts.CallDetailActivity;
import com.yunos.alicontacts.CallDetailTagDialogActivity;
import com.yunos.alicontacts.CallUtil;
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.aliutil.android.common.widget.GroupingListAdapter;
import com.yunos.alicontacts.dialpad.CallLogFragment;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.AliTextUtils;
import com.yunos.alicontacts.util.ClipboardUtils;
import com.yunos.alicontacts.util.FeatureOptionAssistant;
import com.yunos.alicontacts.util.YunOSFeatureHelper;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionSheet;

import java.util.ArrayList;

/**
 * Adapter class to fill in data for the Call Log.
 */
public class CallLogAdapter extends GroupingListAdapter implements OnClickListener {

    private static final String LOGTAG = "CallLogAdapter";
    private static final long ONE_MINUTE = 60 * 1000;
    public static final String TAG_DIVIDER = " | ";

    boolean mIsInBlackList;
    private ActionSheet mLongClickPopupDialog;
    private AlertDialog mConformDeleteDialog;
    private DialListener mDialListener;

    public interface DialListener {
        void callNumberDirectly(final String number);

        void editBeforeCall(final String number);
    }

    public interface DataChangeListener {
        public void onCallLogDataAdded(String number, long callID);
    }

    private static DataChangeListener sDataChangeListener = null;

    public static void setCursorDataChangeListener(DataChangeListener newListener) {
        sDataChangeListener = newListener;
    }

    private long mLastChangeCallLogId = -1;

    private final Activity mActivity;

    public static final int MODE_VIEW = 1;
    public static final int MODE_SELECT = 2;
    private int modeCurrent = MODE_VIEW;
    private Cursor mCursor;
    private long[] mIdsList;
    private boolean[] mCheckedList;
    private int mCheckedCount = 0;

    private int mRedToWhiteColor;
    private int mBlackToWhiteColor;
    private int mGreyToWhiteColor;

    private String mRingTimeFormat;
    private String mCallLogTimeJustnow;

    private StringBuilder mIdsBuilder;

    // Just for bindView, reduce the amount of object creation/destroy
    private StringBuilder mStringBuilderTmp = new StringBuilder(128);

    public static class ViewHolder {
        public ImageButton detailIcon;
        public TextView name;
        public TextView labelAndNumber;
        public ImageView type;
        public TextView date;
        public int viewType;
        public Uri contactUri;
        public View divider;
        public CheckBox checkBox;
        public int itemPosition;
        public ImageView callTypeIndicator;
        public ImageView callFeaturesIndicator;
        public CallInfo callInfo;
        public String displayName;
        public boolean isContact;
        public boolean isYP;
        public String tagName;
        public int markedCount;
    }

    private boolean mLoading = false;
    private static final int REDRAW = 1;

    private static boolean sMultSimEnabled;

    private LayoutInflater mInflater;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REDRAW:
                    // remove duplicate message in looper
                    this.removeMessages(REDRAW);
                    notifyDataSetChanged();
                    break;
                default:
                    Log.w(LOGTAG, "mHandler.handleMessage: unrecognized what " + msg.what);
                    break;
            }
        }
    };

    public CallLogAdapter(Activity activity) {
        super(activity);

        mActivity = activity;

        Resources resources = mActivity.getResources();
        initFontColor(resources);
        initFormatString(resources);

        mIdsBuilder = new StringBuilder();

        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setDialListener(DialListener listener) {
        mDialListener = listener;
    }

    public void setMultSimEnabled(boolean enable) {
        sMultSimEnabled = enable;
    }

    public void setLoading(boolean loading) {
        mLoading = loading;
    }

    public boolean isLoading() {
        return mLoading;
    }

    @Override
    public boolean isEmpty() {
        if (mLoading) {
            // We don't want the empty state to show when loading.
            return false;
        } else {
            return super.isEmpty();
        }
    }

    @Override
    protected void addGroups(Cursor cursor) {
        mCursor = cursor;

        if (modeCurrent == MODE_SELECT) {
            initCheckedStatus(cursor);
            return;
        }

        int count = cursor.getCount();
        if (count == 0) {
            return;
        }

        int groupItemCount = 1;

        cursor.moveToFirst();

        String latestNumber = cursor.getString(CallerViewQuery.NUMBER);
        final long firstCallId = cursor.getLong(CallerViewQuery.ID);

        if (sDataChangeListener != null && mLastChangeCallLogId != firstCallId) {
            sDataChangeListener.onCallLogDataAdded(latestNumber, firstCallId);
            mLastChangeCallLogId = firstCallId;
        }

        while (cursor.moveToNext()) {
            final String currentNumber = cursor.getString(CallerViewQuery.NUMBER);
            final boolean sameNumber = equalNumbers(latestNumber, currentNumber);

            // Group adjacent calls with the same number. Make an exception
            // for the latest item if it was a missed call. We don't want
            // a missed call to be hidden inside a group.
            if (sameNumber) {
                groupItemCount++;
            } else {
                if (groupItemCount > 1) {
                    addGroup(cursor.getPosition() - groupItemCount, groupItemCount, false);
                }

                groupItemCount = 1;

                // The current entry is now the first in the group.
                latestNumber = currentNumber;
            }
        }

        if (groupItemCount > 1) {
            addGroup(count - groupItemCount, groupItemCount, false);
        }
    }

    boolean equalNumbers(String number1, String number2) {
        return PhoneNumberUtils.compare(number1, number2);
    }

    @Override
    protected View newStandAloneView(Context context, ViewGroup parent) {
        View view = null;
        if (modeCurrent == MODE_SELECT) {
            view = mInflater.inflate(R.layout.dialer_search_item_multiselect_view, parent, false);
        } else {
            view = mInflater.inflate(R.layout.dialer_search_item_view, parent, false);
        }

        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindStandAloneView(View view, Context context, Cursor cursor, int position) {
        switch (modeCurrent) {
            case MODE_SELECT:
                bindMultiSelectView(view, cursor, position);
                break;
            case MODE_VIEW:
                bindCallLogView(view, cursor, 1, position);
                break;
            default:
                Log.w(LOGTAG, "bindStandAloneView: unrecognized modeCurrent " + modeCurrent);
                break;
        }
    }

    @Override
    protected View newChildView(Context context, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.dialer_search_item_view, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, int position) {
        switch (modeCurrent) {
            case MODE_SELECT:
                bindMultiSelectView(view, cursor, position);
                break;
            case MODE_VIEW:
                bindCallLogView(view, cursor, 1, position);
                break;
            default:
                Log.w(LOGTAG, "bindChildView: unrecognized modeCurrent " + modeCurrent);
                break;
        }
    }

    @Override
    protected View newGroupView(Context context, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.dialer_search_item_view, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, int groupSize,
            boolean expanded, int position) {
        switch (modeCurrent) {
            case MODE_SELECT:
                bindMultiSelectView(view, cursor, position);
                break;
            case MODE_VIEW:
                bindCallLogView(view, cursor, groupSize, position);
                break;
            default:
                Log.w(LOGTAG, "bindGroupView: unrecognized modeCurrent " + modeCurrent);
                break;
        }
    }

    private void findAndCacheViews(View view) {
        // Get the views to bind to.
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.name = (TextView) view.findViewById(R.id.name);
        viewHolder.labelAndNumber = (TextView) view.findViewById(R.id.labelAndNumber);
        viewHolder.date = (TextView) view.findViewById(R.id.date);
        viewHolder.detailIcon = (ImageButton) view.findViewById(R.id.icon_detail);
        viewHolder.checkBox = (CheckBox) view.findViewById(R.id.gd_checkbox);
        viewHolder.callTypeIndicator = (ImageView) view.findViewById(R.id.call_type_indicator);
        viewHolder.callFeaturesIndicator = (ImageView) view.findViewById(R.id.call_features_indicator);
        viewHolder.callInfo = new CallInfo();
        view.setTag(viewHolder);
    }

    public void bindMultiSelectView(View view, Cursor cursor, int position) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.date.setVisibility(View.VISIBLE);
        viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
        viewHolder.name.setVisibility(View.VISIBLE);
        viewHolder.checkBox.setVisibility(View.VISIBLE);

        int simId = -1;
        if (sMultSimEnabled) {
            int subId = cursor.getInt(CallerViewQuery.SIMID);
            simId = SimUtil.getSlotId(subId);
        }

        String number = cursor.getString(CallerViewQuery.NUMBER);
        final int type = cursor.getInt(CallerViewQuery.TYPE);
        final int features = cursor.getInt(CallerViewQuery.FEATURES);
        long date = cursor.getLong(CallerViewQuery.DATE);
        String countryIso = cursor.getString(CallerViewQuery.COUNTRY_ISO);

        viewHolder.checkBox.setChecked(mCheckedList[position]);

        if (number == null) {
            number = "";
            Log.w(LOGTAG, "bindMultiSelectView() ## call number from callog is null!!! CallID = "
                    + cursor.getLong(CallerViewQuery.ID));
        }
        final String formatNumber = ContactsUtils.formatPhoneNumberWithCurrentCountryIso(number, mActivity);

        final ContactInfo localCallsContactInfo = ContactInfo.fromLocalCallsCursor(cursor);
        ContactInfo cachedContactInfo = ContactInfoHelper.getInstance(mActivity.getApplicationContext())
                .getAndCacheContactInfo(number, countryIso, localCallsContactInfo);
        boolean isStrange = false;
        String labelAndNumberString = "";
        String nameStr = "";
        String contactName = cachedContactInfo.name;
        String ypName = cursor.getString(CallerViewQuery.SHOP_NAME);
        String province = cursor.getString(CallerViewQuery.LOC_PROVINCE);
        String area = cursor.getString(CallerViewQuery.LOC_AREA);
        long ringTime = cursor.getLong(CallerViewQuery.RING_TIME);
        viewHolder.tagName = cursor.getString(CallerViewQuery.TAG_NAME);
        viewHolder.markedCount = cursor.getInt(CallerViewQuery.MARKED_COUNT);
        String location = AliTextUtils.makeLocation(province, area);
        if (TextUtils.isEmpty(contactName)) {
            if (!TextUtils.isEmpty(ypName) && !FeatureOptionAssistant.isInternationalSupportted()) {
                nameStr = ypName;
                if (TextUtils.isEmpty(location)) {
                    labelAndNumberString = formatNumber;
                } else {
                    labelAndNumberString = formatNumber + " " + location;
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
            if (TextUtils.isEmpty(location) || FeatureOptionAssistant.isInternationalSupportted()) {
                labelAndNumberString = formatNumber;
            } else {
                labelAndNumberString = formatNumber + " " + location;
            }
            isStrange = false;
        }

        setNameViewStyle(viewHolder, type, ringTime, nameStr);
        long duration = cursor.getLong(CallerViewQuery.DURATION);
        bindCallTypeAndFreaturesView(viewHolder, cursor, duration, type, features, simId, isStrange, labelAndNumberString);

        // for unknown number;private number; payed number
        setSpecialNumberLabel(number, viewHolder.name);

        String dateString;
        long currentTime = System.currentTimeMillis();
        if (currentTime - date < ONE_MINUTE) {
            dateString = mCallLogTimeJustnow;
        } else {
            dateString = DateUtils.getRelativeTimeSpanString(date, currentTime,
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE).toString();
            dateString = dateString.replaceAll("/", "-");
        }
        viewHolder.date.setText(dateString);
    }

    private void setNameViewStyle(ViewHolder viewHolder, int type, long ringTime, String nameStr) {
        int color = viewHolder.name.getCurrentTextColor();
        if (type == Calls.MISSED_TYPE) {
            if (ringTime != 0) {
                String ringStr = AliCallLogExtensionHelper.formatCallTypeLabel(mActivity,
                        mRingTimeFormat, -1, ringTime);
                StringBuilder bdStr = new StringBuilder();
                bdStr.append('(').append(ringStr).append(')');
                SpannableString ss = new SpannableString(nameStr + bdStr);
                int len = nameStr.length() + bdStr.length();
                ss.setSpan(new ForegroundColorSpan(mRedToWhiteColor), 0, nameStr.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                float textSize = viewHolder.labelAndNumber.getTextSize();
                ss.setSpan(new AbsoluteSizeSpan((int) textSize), nameStr.length(), len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new ForegroundColorSpan(mGreyToWhiteColor), nameStr.length(), len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                viewHolder.name.setText(ss);
            } else {
                if (color != mRedToWhiteColor) {
                    viewHolder.name.setTextColor(mRedToWhiteColor);
                }
                viewHolder.name.setText(nameStr);
            }
        } else {
            if (color != mBlackToWhiteColor) {
                viewHolder.name.setTextColor(mBlackToWhiteColor);
            }
            viewHolder.name.setText(nameStr);
        }
    }

    public void bindCallLogView(View view, Cursor cursor, int count, int position) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        int simId = -1;
        if (sMultSimEnabled) {
            int subId = cursor.getInt(CallerViewQuery.SIMID);
            simId = SimUtil.getSlotId(subId);
        }

        final long callId = cursor.getLong(CallerViewQuery.ID);
        String number = cursor.getString(CallerViewQuery.NUMBER);
        final int type = cursor.getInt(CallerViewQuery.TYPE);
        final int features = cursor.getInt(CallerViewQuery.FEATURES);
        long date = cursor.getLong(CallerViewQuery.DATE);
        String countryIso = cursor.getString(CallerViewQuery.COUNTRY_ISO);

        /**
         * Bug:5322921. APR Null Exception. Avoid exception when number is null
         * in very special case.
         */
        if (number == null) {
            number = "";
            Log.w(LOGTAG, "bindCallLogView() ## call number from callog is null!!! CallID = "
                    + callId);
        }
        final String formatNumber = ContactsUtils.formatPhoneNumberWithCurrentCountryIso(number, mActivity);

        final ContactInfo localCallsContactInfo = ContactInfo.fromLocalCallsCursor(cursor);
        ContactInfo cachedContactInfo = ContactInfoHelper.getInstance(mActivity.getApplicationContext())
                .getAndCacheContactInfo(number, countryIso, localCallsContactInfo);
        boolean isStrange = false;

        String labelAndNumberString = "";
        String nameStr = "";
        String contactName = cachedContactInfo.name;
        String ypName = cursor.getString(CallerViewQuery.SHOP_NAME);
        String province = cursor.getString(CallerViewQuery.LOC_PROVINCE);
        String area = cursor.getString(CallerViewQuery.LOC_AREA);
        viewHolder.tagName = cursor.getString(CallerViewQuery.TAG_NAME);
        viewHolder.markedCount = cursor.getInt(CallerViewQuery.MARKED_COUNT);
        String location = AliTextUtils.makeLocation(province, area);
        viewHolder.isYP = !TextUtils.isEmpty(ypName);
        if (TextUtils.isEmpty(contactName)) {
            viewHolder.isContact = false;
            if (viewHolder.isYP) {
                nameStr = ypName;
                viewHolder.displayName = ypName;
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
                viewHolder.displayName = formatNumber;
                if (!FeatureOptionAssistant.isInternationalSupportted()) {
                    labelAndNumberString = location;
                }
            }
            isStrange = true;
        } else {
            nameStr = contactName;
            viewHolder.displayName = contactName;
            viewHolder.isContact = true;
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
        long ringTime = cursor.getLong(CallerViewQuery.RING_TIME);
        long duration = cursor.getLong(CallerViewQuery.DURATION);
        // set group count
        if (count > 1) {
            mIdsBuilder.delete(0, mIdsBuilder.length());
            mStringBuilderTmp.append(" (").append(count).append(')');

            int i = 0;
            // here we use cursor when move finish, be care of cursor move out
            // of
            // index. i < count && cursor.moveToNext() we use '&&' to cut off
            // cursor.moveToNext() run, when i < count is false.
            do {
                mIdsBuilder.append(cursor.getString(CallerViewQuery.ID));
                mIdsBuilder.append(',');
                i++;
            } while (i < count && cursor.moveToNext());

            mIdsBuilder.deleteCharAt(mIdsBuilder.length() - 1);// Remove the
                                                               // last comma.
            viewHolder.callInfo.strIds = mIdsBuilder.toString();
        } else {
            viewHolder.callInfo.strIds = Long.toString(callId);
        }

        setNameViewStyle(viewHolder, type, ringTime, mStringBuilderTmp.toString());
        bindCallTypeAndFreaturesView(viewHolder, cursor, duration, type, features, simId, isStrange, labelAndNumberString);
        // for unknown number;private number; payed number
        setSpecialNumberLabel(number, viewHolder.name);

        String dateString;
        long currentTime = System.currentTimeMillis();
        if (currentTime - date < ONE_MINUTE) {
            dateString = mCallLogTimeJustnow;
        } else {
            dateString = DateUtils.getRelativeTimeSpanString(date, currentTime,
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE).toString();
            dateString = dateString.replaceAll("/", "-");
        }
        viewHolder.date.setText(dateString);
        viewHolder.callInfo.id = callId;
        viewHolder.callInfo.number = number;
        viewHolder.callInfo.isFirstItem = (position == 0);
        viewHolder.detailIcon.setTag(viewHolder.callInfo);
        viewHolder.detailIcon.setOnClickListener(this);
    }

    private void initFontColor(Resources res) {
        mRedToWhiteColor = res.getColor(R.drawable.aui_ic_color_red_to_white);
        mBlackToWhiteColor = res.getColor(R.drawable.aui_ic_color_black_to_white);
        mGreyToWhiteColor = res.getColor(R.drawable.aui_ic_color_grey_to_white);
    }

    private void initFormatString(Resources res) {
        mRingTimeFormat = res.getString(R.string.ringing_time);
        mCallLogTimeJustnow = res.getString(R.string.calllog_time_just_now);
    }

    private void bindCallTypeAndFreaturesView(ViewHolder viewHolder, Cursor cursor, long duration, int type, int features,
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
        viewHolder.callTypeIndicator.setImageResource(resId);

        if ((features & CallerViewQuery.CALL_FEATURES_BIT_VIDEO) != 0) {
            viewHolder.callFeaturesIndicator.setImageResource(R.drawable.ic_callhistory_facetime_normal);
            viewHolder.callFeaturesIndicator.setVisibility(View.VISIBLE);
        } else if ((features & CallerViewQuery.CALL_FEATURES_BIT_HD) != 0) {
            viewHolder.callFeaturesIndicator.setImageResource(R.drawable.ic_callhistory_hd_normal);
            viewHolder.callFeaturesIndicator.setVisibility(View.VISIBLE);
        } else {
            viewHolder.callFeaturesIndicator.setVisibility(View.GONE);
        }

        // Bind tag type if any tag type marked.
        bindTagTypeView(cursor, viewHolder.labelAndNumber, isStrange, labelAndNumberString);
    }

    private void bindTagTypeView(Cursor cursor, TextView tv, boolean isStrange,
            String labelAndNumberString) {
        mStringBuilderTmp.setLength(0);
        mStringBuilderTmp.append(labelAndNumberString);
        String tagName = cursor.getString(CallerViewQuery.TAG_NAME);
        int markedCount = cursor.getInt(CallerViewQuery.MARKED_COUNT);
        if (isStrange) {

            if (!TextUtils.isEmpty(tagName)) {
                if (!TextUtils.isEmpty(labelAndNumberString)) {
                    mStringBuilderTmp.append(TAG_DIVIDER);
                }
                if (markedCount > 0) {
                    tagName = ((markedCount == 1) ? mActivity.getString(
                            R.string.system_tag_type_parameter_for_one, tagName) : mActivity
                            .getString(R.string.system_tag_type_parameter, markedCount, tagName));
                }
                mStringBuilderTmp.append(tagName);
            }
        }

        tv.setText(mStringBuilderTmp.toString());
    }

    private void setSpecialNumberLabel(String number, TextView tv) {
        int resId = AliCallLogExtensionHelper.getSpecialNumber(number);
        if (resId > 0) {
            tv.setText(resId);
        }
    }

    @Override
    public void addGroup(int cursorPosition, int size, boolean expanded) {
        super.addGroup(cursorPosition, size, expanded);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.icon_detail:
            case R.id.icon_detail_container:
                final CallInfo callInfo = (CallInfo) v.getTag();
                final String strId = callInfo.strIds;
                String[] arrayIds = strId.split(",");
                Intent intent = new Intent(mActivity, CallDetailActivity.class);
                if (arrayIds.length == 1) {
                    intent.setData(ContentUris.withAppendedId(Calls.CONTENT_URI,
                            Long.parseLong(arrayIds[0])));
                } else {
                    long[] ids = new long[arrayIds.length];
                    // Copy the ids of the rows in the group.
                    for (int index = 0; index < arrayIds.length; ++index) {
                        ids[index] = Long.parseLong(arrayIds[index]);
                    }
                    intent.putExtra(CallDetailActivity.EXTRA_CALL_LOG_IDS, ids);
                }
                intent.putExtra(CallDetailActivity.EXTRA_NEED_UPDATE_CALLLOG, callInfo.isFirstItem);
                mActivity.startActivity(intent);
                UsageReporter.onClick(null, CallLogFragment.USAGE_REPORTER_NAME,
                        UsageReporter.DialpadPage.DP_ENTER_DETAIL);
                break;
            default:
                Log.w(LOGTAG, "onClick: unrecognized id " + id);
                break;
        }
    }

    public int getModeCurrent() {
        return modeCurrent;
    }

    public void setModeCurrent(int modeCurrent) {
        this.modeCurrent = modeCurrent;
    }

    public int getCheckedCount() {
        return mCheckedCount;
    }

    public ArrayList<Long> getCheckedIds() {
        if (modeCurrent != MODE_SELECT || mCheckedList == null) {
            return new ArrayList<Long>(0);
        }
        ArrayList<Long> result = new ArrayList<Long>(mCheckedCount);
        for (int i = 0; i < mCheckedList.length; i++) {
            if (mCheckedList[i]) {
                result.add(mIdsList[i]);
            }
        }
        return result;
    }

    public boolean toggleChecked(int position, long id, View view) {
        if (modeCurrent != MODE_SELECT || mCheckedList == null) {
            return false;
        }
        mCheckedList[position] = !mCheckedList[position];
        boolean checked = mCheckedList[position];
        if (checked) {
            mCheckedCount++;
        } else {
            mCheckedCount--;
        }
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.checkBox.setChecked(checked);
        return checked;
    }

    public void setAllChecked(boolean checked) {
        if (modeCurrent != MODE_SELECT || mCheckedList == null) {
            return;
        }
        for (int i = 0; i < mCheckedList.length; i++) {
            mCheckedList[i] = checked;
        }
        mCheckedCount = checked ? mCheckedList.length : 0;
        notifyDataSetChanged();
    }

    private void initCheckedStatus(Cursor cursor) {
        cursor.moveToPosition(-1);
        int count = cursor.getCount();
        long[] ids = new long[count];
        boolean[] checkedStatus = new boolean[count];
        for (int i = 0; i < count; i++) {
            cursor.moveToNext();
            ids[i] = cursor.getLong(CallerViewQuery.ID);
        }
        int checkedCount = 0;
        if (mCheckedList != null) {
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < mIdsList.length; j++) {
                    if (mIdsList[j] == ids[i]) {
                        if (mCheckedList[j]) {
                            checkedStatus[i] = true;
                            checkedCount++;
                        }
                        break;
                    }
                }
            }
        }
        mCheckedList = checkedStatus;
        mIdsList = ids;
        mCheckedCount = checkedCount;
    }

    public OnItemClickListener itemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(LOGTAG, "OnItemClickListener.onItemClick: clicked at position " + position);
            Object obj = null;
            Object viewTag = view.getTag();
            if (viewTag instanceof ViewHolder) {
                obj = ((ViewHolder) viewTag).detailIcon.getTag();
            }

            if (obj instanceof CallInfo) {
                CallInfo info = (CallInfo) obj;
                mDialListener.callNumberDirectly(info.number);
                UsageReporter.onClick(null, CallLogFragment.USAGE_REPORTER_NAME,
                        UsageReporter.DialpadPage.DP_MO_FROM_CALLLOG);
            }
            return;
        }
    };

    public OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Object obj = null;
            int callId = -1;
            if (view.getTag() instanceof ViewHolder) {
                obj = ((ViewHolder) view.getTag()).detailIcon.getTag();
                callId = (int) ((ViewHolder) view.getTag()).callInfo.id;
            } else {
                return false;
            }

            String longClickNumber = null;
            String strId = null;
            if (obj instanceof CallInfo) {
                CallInfo info = (CallInfo) obj;
                longClickNumber = info.number;
                strId = info.strIds;
            } else {
                return false;
            }
            showPopupMenu(view, longClickNumber, position, strId, callId);
            return true;
        }
    };

    private void showPopupMenu(final View view, final String longClickNumber, final int position,
            final String strId, final int callId) {

        if (mShowLongClickActionMenuListener != null) {
            mShowLongClickActionMenuListener.onShowMenu();
        }

        boolean callable = PhoneNumberHelper.canPlaceCallsTo(longClickNumber);
        int blackListStringId = R.string.calllog_blacklist_add;

        if (AliCallLogExtensionHelper.PLATFORM_YUNOS && callable) {
            mIsInBlackList = YunOSFeatureHelper.isBlack(mActivity, longClickNumber);
            if (mIsInBlackList) {
                blackListStringId = R.string.calllog_blacklist_remove;
            }
        }

        Resources resource = mActivity.getResources();
        ViewHolder holder = (ViewHolder) view.getTag();
        boolean isAContactOrYP = holder.isContact || holder.isYP;
        final String tagName = holder.tagName;
        final int markedCount = holder.markedCount;
        ArrayList<String> items = new ArrayList<String>(8);
        class PopupMenuClickListener implements ActionSheet.CommonButtonListener {
            private int[] mIndexToReal;

            public PopupMenuClickListener(int[] table) {
                mIndexToReal = table;
            }

            @Override
            public void onDismiss(ActionSheet as) {
            }

            @Override
            public void onClick(int which) {
                int what = mIndexToReal[which];
                Log.i(LOGTAG, "PopupMenuClickListener.onClick: which="+which+"; what="+what);
                switch (what) {
                    case 0: // Make common call.
                        mDialListener.callNumberDirectly(longClickNumber);
                        UsageReporter.onClick(null, CallLogFragment.USAGE_REPORTER_NAME,
                                UsageReporter.DialpadPage.DP_LC_MAKE_CALL);
                        break;
                    case 1: // copy phone number
                        ClipboardUtils.copyText(mActivity, null, longClickNumber, true);
                        UsageReporter.onClick(mActivity, null, UsageReporter.DialpadPage.DP_LC_TO_COPY_NUMBER);
                        break;
                    case 4: // delete
                        AlertDialog.Builder build = new AlertDialog.Builder(mActivity);
                        build.setMessage(mActivity.getString(R.string.confirm_delete));
                        build.setPositiveButton(R.string.delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mActivity.getContentResolver().delete(
                                                Calls.CONTENT_URI,
                                                Calls._ID + " IN (" + strId + ")", null);
                                        UsageReporter.onClick(null,
                                                CallLogFragment.USAGE_REPORTER_NAME,
                                                UsageReporter.DialpadPage.DP_LC_DELETE_CALLLOG);
                                    }
                                });
                        build.setNegativeButton(R.string.no, null);
                        mConformDeleteDialog = build.create();
                        mConformDeleteDialog.show();
                        mConformDeleteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                Color.RED);
                        break;
                    case 5:// edit before call
                        mDialListener.editBeforeCall(longClickNumber);
                        UsageReporter.onClick(null, CallLogFragment.USAGE_REPORTER_NAME,
                                UsageReporter.DialpadPage.DP_LC_EDIT_BEFORE_CALL);
                        break;
                    case 6: // add black list
                        handleBlackList(longClickNumber);
                        UsageReporter.onClick(null, CallLogFragment.USAGE_REPORTER_NAME,
                                UsageReporter.DialpadPage.DP_LC_ADD_BLACKLIST);
                        break;
                    case 7: // Left for mark
                        Intent intent = new Intent(CallDetailTagDialogActivity.ACTION_MARK_TAG);
                        intent.setClass(mActivity.getApplicationContext(), CallDetailTagDialogActivity.class);
                        intent.putExtra(CallDetailTagDialogActivity.EXTRA_KEY_NUMBER, longClickNumber);
                        intent.putExtra(CallDetailTagDialogActivity.EXTRA_KEY_TAG_NAME, tagName);
                        intent.putExtra(CallDetailTagDialogActivity.EXTRA_KEY_MARKED_COUNT, markedCount);
                        mActivity.startActivity(intent);
                        UsageReporter.onClick(null, CallLogFragment.USAGE_REPORTER_NAME,
                                UsageReporter.DialpadPage.DP_LC_MARK_NUM);
                        break;
                    case 8: // Make video call
                        Intent videoCallIntent = CallUtil.getVideoCallIntent(mActivity, longClickNumber, mActivity.getClass().getCanonicalName());
                        mActivity.startActivity(videoCallIntent);
                        UsageReporter.onClick(null, CallLogFragment.USAGE_REPORTER_NAME,
                                UsageReporter.DialpadPage.DP_LC_MAKE_VIDEO_CALL);
                        break;
                    default:
                        Log.w(LOGTAG,
                                "mLongClickPopupDialog.CommonButtonListener.onClick: unrecognized which "
                                        + which);
                        break;
                }
            }
        }
        ;

        int[] table = null;
        if (!callable) {
            table = new int[] {
                1, 4
            };
            items.add(resource.getString(R.string.copy_phone_number));
            items.add(resource.getString(R.string.menu_delete_call_log));
        } else {
            // FIXME: in Spreadtrum, we might need to get volte attach state.
            boolean canMakeVideoCall = SimUtil.isVideoCallEnabled(mActivity)
                    && AliCallLogExtensionHelper.canPlaceVolteVideoCallByNumber(longClickNumber);
            table = canMakeVideoCall ?
                    new int[] { 0, 8, 1, 4, 5, 6, 7 }
                    : new int[] { 0, 1, 4, 5, 6, 7 };
            items.add(resource.getString(R.string.call_other));
            if (canMakeVideoCall) {
                items.add(resource.getString(R.string.make_video_call));
            }
            items.add(resource.getString(R.string.copy_phone_number));
            items.add(resource.getString(R.string.menu_delete_call_log));
            items.add(resource.getString(R.string.edit_before_call));
            items.add(resource.getString(blackListStringId));
            if (!FeatureOptionAssistant.isInternationalSupportted() && !isAContactOrYP) {
                items.add(resource.getString(R.string.mark_number));
            }
        }

        mLongClickPopupDialog = new ActionSheet(mActivity);

        mLongClickPopupDialog
                .setCommonButtons(items, null, null, new PopupMenuClickListener(table));
        mLongClickPopupDialog.show(view);
    }

    private void handleBlackList(String longClickNumber) {
        if (AliCallLogExtensionHelper.PLATFORM_YUNOS) {
            boolean result = false;
            if (mIsInBlackList) {
                result = YunOSFeatureHelper.removeBlack(mActivity, longClickNumber);
                if (result) {
                    mIsInBlackList = false;
                    Toast.makeText(mActivity, R.string.contact_detail_removeFromBlackListOK,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, R.string.contact_detail_removeFromBlackListFail,
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                int type = 3;// type 3: block call and sms
                result = YunOSFeatureHelper.addBlack(mActivity, longClickNumber, type);
                if (result) {
                    mIsInBlackList = true;
                    Toast.makeText(mActivity, R.string.contact_detail_addToBlackListOK,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, R.string.contact_detail_addToBlackListFail,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void clearDialog() {
        if (mLongClickPopupDialog != null) {
            mLongClickPopupDialog.dismiss();
            mLongClickPopupDialog = null;
        }
        if (mConformDeleteDialog != null) {
            mConformDeleteDialog.dismiss();
            mConformDeleteDialog = null;
        }
    }

    public interface ShowLongClickActionMenuListener {
        void onShowMenu();
    }

    ShowLongClickActionMenuListener mShowLongClickActionMenuListener;

    public void setShowLongClickActionMenuListener(ShowLongClickActionMenuListener listener) {
        mShowLongClickActionMenuListener = listener;
    }

    @Override
    public long getItemId(int position) {
        if (MODE_SELECT == modeCurrent && mCursor != null) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(CallerViewQuery.ID);
        }
        return super.getItemId(position);
    }

    public void forceUpdateData() {
        if (mHandler != null) {
            if (mHandler.hasMessages(REDRAW)) {
                // last message is also in looper, so omit this operation
                return;
            }
            mHandler.sendEmptyMessage(REDRAW);
        }
    }

    /**
     * This method must be called in main thread, or the mCursor might be
     * accessed synchronized.
     *
     * @return The last out going call, or null if not available.
     */
    public String queryLastOutCall() {
        // for 500 call logs in A800 user build,
        // to find last out call by query db, 14.5 ms.
        // to find last out call by go through 500 rows of mCursor, 3.8 ms.
        int count = mCursor == null ? -1 : mCursor.getCount();
        if (count < 1) {
            return null;
        }
        int pos = 0, type;
        while (pos < count) {
            mCursor.moveToPosition(pos++);
            type = mCursor.getInt(CallerViewQuery.TYPE);
            if (type == Calls.OUTGOING_TYPE) {
                return mCursor.getString(CallerViewQuery.NUMBER);
            }
        }
        return null;
    }

    @Override
    public Object getItem(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(CallerViewQuery.NUMBER);
    }

}
