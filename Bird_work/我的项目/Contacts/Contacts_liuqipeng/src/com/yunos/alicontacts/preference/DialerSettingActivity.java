
package com.yunos.alicontacts.preference;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BasePreferenceActivity;
import com.yunos.alicontacts.sim.SimStateReceiver;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.FeatureOptionAssistant;
import com.yunos.common.UsageReporter;

import java.util.List;

public class DialerSettingActivity extends BasePreferenceActivity {
    private static final String TAG = "DialerSettingActivity";

    public static final String PREFS_KEY_AUTO_MARK = "prefs_key_auto_mark";
    public static final String DIALER_SETTINGS = "dialer_settings";

    private static final SparseArray<String> sReportList;
    static {
        sReportList = new SparseArray<String>();
        sReportList.put(R.id.dualcard_settings, UsageReporter.DialpadPage.DUAL_CARD_SETTING);
        sReportList.put(R.id.tag_auto_mark_settings,
                UsageReporter.DialpadPage.TURN_ON_AUTO_MARK_TAG);
        sReportList.put(R.id.quick_dial,
                UsageReporter.ContactsSettingsPage.QUICK_CALL_SETTING);
    }

    public static final boolean DEFAULT_AUTO_MARK_TAG = true;
    private boolean mAutoMarkTag = DEFAULT_AUTO_MARK_TAG;
    private SharedPreferences mPrefs;
    private SimStateReceiver.SimStateListener mSimStateReceiver = new SimStateReceiver.SimStateListener() {
        @Override
        public void onSimStateChanged(int slot, String state) {
            checkAirplaneModeAsync();
        }
    };

    private HeaderAdapter mHeaderAdapter;
    private boolean mIsAirplaneMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
        setTitle2(getString(R.string.general_settings_label));
        showBackKey(true);

        mPrefs = getSharedPreferences(DIALER_SETTINGS, Context.MODE_PRIVATE);
        mAutoMarkTag = isAutoMarkTag();
        SimStateReceiver.registSimStateListener(mSimStateReceiver);
        checkAirplaneModeAsync();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.dialer_settings, target);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SimStateReceiver.unregistSimStateListener(mSimStateReceiver);
    }

    private void checkAirplaneModeAsync() {
        AsyncTask<Void, Void, Void> checkTask = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                Log.i(TAG, "checkAirplaneModeAsync.doInBackground: before read airplane mode.");
                int airplaneMode = Settings.System.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
                Log.i(TAG, "checkAirplaneModeAsync.doInBackground: after read airplane mode. airplaneMode="+airplaneMode);
                boolean isAipplaneMode = airplaneMode == 1;
                if (isAipplaneMode != mIsAirplaneMode) {
                    mIsAirplaneMode = isAipplaneMode;
                    postNotifyDataSetChanged();
                }
                return null;
            }
        };
        checkTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void postNotifyDataSetChanged() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mHeaderAdapter != null) {
                    mHeaderAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void updateHeaders(List<Header> target) {
        int i = 0;
        while (i < target.size()) {
            Header header = target.get(i);
            long id = header.id;
            if (id == R.id.dualcard_settings && !SimUtil.MULTISIM_ENABLE) {
                // NOTE: if anyone wants to display sim management (dualcard_settings) in single card phone,
                // then noCardInserted() need to handle SimUtil.MULTISIM_ENABLE.
                target.remove(i);
                continue;
            } else if (FeatureOptionAssistant.isInternationalSupportted()
                    && id == R.id.tag_auto_mark_settings) {
                target.remove(i);
                continue;
            }

            i++;
        }
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter != null) {
            mHeaderAdapter = new HeaderAdapter(this, getHeaders());
            super.setListAdapter(mHeaderAdapter);
            return;
        }
        super.setListAdapter(null);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        try {
            super.onHeaderClick(header, position);
            if (header.id == R.id.tag_auto_mark_settings) {
                mAutoMarkTag = !mAutoMarkTag;
                setAutoMarkTag();
                invalidateHeaders();
            }
        } catch (ActivityNotFoundException aEx) {
            Log.e(TAG, "onHeaderClick :", aEx);
        }
        sendUsageReport(header.id);
    }

    private void sendUsageReport(long headerId) {
        String report = sReportList.get((int) headerId);

        if (headerId == R.id.tag_auto_mark_settings && !mAutoMarkTag) {
            report = UsageReporter.DialpadPage.TURN_OFF_AUTO_MARK_TAG;
        }

        UsageReporter.onClick(null, TAG, report);
    }

    private boolean isAutoMarkTag() {
        return mPrefs.getBoolean(PREFS_KEY_AUTO_MARK, mAutoMarkTag);
    }

    private void setAutoMarkTag() {
        Editor editor = mPrefs.edit();
        editor.putBoolean(PREFS_KEY_AUTO_MARK, mAutoMarkTag);
        editor.apply();
    }

    private class HeaderAdapter extends ArrayAdapter<Header> {
        public static final int HEADER_TYPE_NORMAL = 1;
        public static final int HEADER_TYPE_SWITCH = 2;

        private final LayoutInflater mInflater;
        private final Resources mResources;

        private class HeaderViewHolder {
            TextView mTitle;
            TextView mSummary;
            Switch mSwitch;
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
            int headerType = getHeaderType(header);
            View view = null;

            if (convertView == null) {
                holder = new HeaderViewHolder();

                view = mInflater.inflate(R.xml.settings_item, parent, false);
                holder.mTitle = (TextView)
                        view.findViewById(R.id.title);
                holder.mSummary = (TextView)
                        view.findViewById(R.id.summary);
                holder.mSwitch = (Switch) view.findViewById(R.id.switchWidget);

                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            holder.mTitle.setEnabled(isEnabled(position));
            holder.mTitle.setText(header.getTitle(mResources));
            CharSequence summary = header.getSummary(mResources);
            int viewHeight;
            if (!TextUtils.isEmpty(summary) || !TextUtils.isEmpty(header.summary)) {
                holder.mSummary.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(header.summary)) {
                    holder.mSummary.setText(header.summary);
                } else {
                    holder.mSummary.setText(summary);
                }
                viewHeight = mResources.getDimensionPixelSize(R.dimen.listview_item_height_dual_line);
            } else {
                holder.mSummary.setVisibility(View.GONE);
                viewHeight = mResources.getDimensionPixelSize(R.dimen.listview_item_height);
            }
            view.getLayoutParams().height = viewHeight;

            if (headerType == HEADER_TYPE_SWITCH) {
                holder.mSwitch.setVisibility(View.VISIBLE);
                if (header.id == R.id.tag_auto_mark_settings) {
                    holder.mSwitch.setChecked(mAutoMarkTag);
                }
            } else {
                holder.mSwitch.setVisibility(View.GONE);
            }

            return view;
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        public int getHeaderType(Header header) {

            if (header.id == R.id.tag_auto_mark_settings) {
                return HEADER_TYPE_SWITCH;
            } else {
                return HEADER_TYPE_NORMAL;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            Header header = getItem(position);
            if (header.id == R.id.dualcard_settings
                    && (mIsAirplaneMode || noCardInserted())) {
                return false;
            }
            return true;
        }

        private boolean noCardInserted() {
            // For single card phone, the sim management item is removed,
            // so don't need to check SimUtil.MULTISIM_ENABLE here.
            Log.i(TAG, "noCardInserted: before check card status.");
            boolean cardStatus = (!SimUtil.hasIccCard(SimUtil.SLOT_ID_1))
                    && (!SimUtil.hasIccCard(SimUtil.SLOT_ID_2));
            Log.i(TAG, "noCardInserted: after check card status. result="+cardStatus);
            return cardStatus;
        }

    }

}
