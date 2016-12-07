
package com.yunos.alicontacts.preference;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BasePreferenceActivity;
import com.yunos.common.UsageReporter;

import java.util.List;

public class PhoneSettingActivity extends BasePreferenceActivity {
    private static final String TAG = "PhoneSettingActivity";
    private static final String PHONE_PACKAGE = "com.android.phone";
    private static final String CALL_SETTINGS_CLASS_NAME = "com.android.phone.settings.PhoneAccountSettingsActivity";

    private static final SparseArray<String> sReportList;
    static {
        sReportList = new SparseArray<String>();
        sReportList.put(R.id.general_setting,
                UsageReporter.PhoneSettingsPage.PHONE_SETTINGS_CLICK_GENERAL_SETTINGS);
        sReportList.put(R.id.phone_settings,
                UsageReporter.PhoneSettingsPage.PHONE_SETTINGS_CLICK_CALL_SETTINGS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
        setTitle2(getString(R.string.phone_settings_label));
        showBackKey(true);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.phone_settings, target);
        updateHeaders(target);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UsageReporter.onResume(this, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UsageReporter.onPause(this, null);
    }

    private void updateHeaders(List<Header> target) {
        for(Header header : target) {
            long id = header.id;
            if (id == R.id.phone_settings) {
                header.intent = getCallSettingsIntent();
            }
        }
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter != null) {
            super.setListAdapter(new HeaderAdapter(this, getHeaders()));
            return;
        }
        super.setListAdapter(null);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        try {
            super.onHeaderClick(header, position);
        } catch (ActivityNotFoundException aEx) {
            Log.e(TAG, "onHeaderClick :", aEx);
        }

        sendUsageReport(header.id);
    }

    private void sendUsageReport(long headerId) {
        String report = sReportList.get((int) headerId);
        UsageReporter.onClick(null, TAG, report);
    }

    /** Returns an Intent to launch Call Settings screen */
    private Intent getCallSettingsIntent() {
        final Intent intent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
        intent.setClassName(PHONE_PACKAGE, CALL_SETTINGS_CLASS_NAME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private class HeaderAdapter extends ArrayAdapter<Header> {
        private final LayoutInflater mInflater;
        private final Resources mResources;

        private class HeaderViewHolder {
            TextView mTitle;
            TextView mSummary;
        }

        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mResources = context.getResources();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            Header header = getItem(position);
            View view = null;

            if (convertView == null) {
                holder = new HeaderViewHolder();

                view = mInflater.inflate(R.xml.settings_item, parent, false);
                holder.mTitle = (TextView)
                        view.findViewById(R.id.title);
                holder.mSummary = (TextView)
                        view.findViewById(R.id.summary);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            //holder.mTitle.setEnabled(isEnabled(position));
            holder.mTitle.setText(header.getTitle(mResources));
            CharSequence summary = header.getSummary(mResources);
            if (!TextUtils.isEmpty(summary) || !TextUtils.isEmpty(header.summary)) {
                holder.mSummary.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(header.summary)) {
                    holder.mSummary.setText(header.summary);
                } else {
                    holder.mSummary.setText(summary);
                }
            } else {
                holder.mSummary.setVisibility(View.GONE);
            }

            return view;
        }

    }

}
