
package com.yunos.alicontacts.sim;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BasePreferenceActivity;
import com.yunos.common.UsageReporter;

import java.util.List;

public class SimMultiSelectActivity extends BasePreferenceActivity implements
        SimStateReceiver.SimStateListener {
    private static final String TAG = "MutiSIMSelectActivity";

    private int mActionMode;
    private HeaderAdapter mAdapter;

    private static final SparseArray<String> sReportList;
    static {
        sReportList = new SparseArray<String>();
        sReportList.put(R.id.import_vcard,
                UsageReporter.ContactsSettingsPage.SETTING_IMPORT_FROM_VCARD);
        sReportList.put(R.id.export_vcard,
                UsageReporter.ContactsSettingsPage.SETTING_EXPORT_TO_VCARD);
    }

    @Override
    public void onSimStateChanged(int slot, String state) {
        invalidateHeaders();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mActionMode = intent.getIntExtra(SimContactUtils.SIM_MULTI_SELECT_ACTION, -1);
        Log.d(TAG, "onCreate() mActionMode:" + mActionMode);

        super.onCreate(savedInstanceState);
        initActionBar();
        showBackKey(true);

        switch (mActionMode) {
            case SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT:
            case SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT_FROM_SIM:
                setTitle2(getString(R.string.contacts_settings_import_title));
                break;
            case SimContactUtils.SIM_MULTI_SELECT_ACTION_EXPORT:
                setTitle2(getString(R.string.contacts_settings_export_title));
                break;
            default:
                Log.d(TAG, "onCreate() mActionMode:"+mActionMode+", finish!!!");
                finish();
                return;
        }

        SimStateReceiver.registSimStateListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UsageReporter.onResume(null, TAG);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UsageReporter.onPause(null, TAG);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SimStateReceiver.unregistSimStateListener(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.layout.sim_multi_select_layout, target);
        updateHeaders(target);
    }

    private void updateHeaders(List<Header> target) {
        int i = 0;
        while (i < target.size()) {
            Header header = target.get(i);
            long id = header.id;
            switch (mActionMode) {
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT:
                    if (id == R.id.export_vcard) {
                        target.remove(i);
                        continue;
                    }

                    if (!SimUtil.MULTISIM_ENABLE && id == R.id.sim_card2) {
                        target.remove(i);
                        continue;
                    }
                    break;
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_EXPORT:
                    if (id == R.id.import_vcard) {
                        target.remove(i);
                        continue;
                    }

                    if (!SimUtil.MULTISIM_ENABLE && id == R.id.sim_card2) {
                        target.remove(i);
                        continue;
                    }
                    break;
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT_FROM_SIM:
                    if (id == R.id.import_vcard || id == R.id.export_vcard) {
                        target.remove(i);
                        continue;
                    }
                    break;
                default:
                    Log.d(TAG, "updateHeaders() mActionMode:"+mActionMode);
                    break;
            }

            if (id == R.id.sim_card1 || id == R.id.sim_card2) {
                int subscription = (id == R.id.sim_card1) ? SimUtil.SLOT_ID_1
                        : SimUtil.SLOT_ID_2;
                header.title = SimContactUtils.getSimListItemTitle(this, subscription);
            }

            i++;
        }
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter != null) {
            mAdapter = new HeaderAdapter(this, getHeaders());
            super.setListAdapter(mAdapter);
            return;
        }
        super.setListAdapter(null);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        long headerId = header.id;

        if(mAdapter != null && !mAdapter.getItemEnable(position)) {
            return;
        }

        if (headerId == R.id.sim_card1 || headerId == R.id.sim_card2) {
            Intent intent = null;

            switch (mActionMode) {
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT:
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT_FROM_SIM:
                    intent = new Intent(SimContactUtils.ACTION_IMPORT_CONTACTS);
                    break;
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_EXPORT:
                    intent = new Intent(SimContactUtils.ACTION_EXPORT_CONTACTS);
                    break;
                default:
                    break;
            }

            int subscription = SimUtil.SLOT_ID_1;
            if (headerId == R.id.sim_card2) {
                subscription = SimUtil.SLOT_ID_2;
            }

            if (intent != null) {
                intent.putExtra(SimUtil.SLOT_KEY, subscription);
                startActivity(intent);
            }
        } else {
            if (headerId == R.id.export_vcard && (SimContactUtils.getContactsTotalCount(this) == 0)) {
                Toast.makeText(this, R.string.export_empty_contacts, Toast.LENGTH_SHORT).show();
            } else {
                super.onHeaderClick(header, position);
            }
        }

        sendUsageReport(headerId);
    }

    private void sendUsageReport(long headerId) {
        String report = sReportList.get((int) headerId);

        if (headerId == R.id.sim_card1 || headerId == R.id.sim_card2) {
            switch (mActionMode) {
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT:
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT_FROM_SIM:
                    if (SimUtil.MULTISIM_ENABLE) {
                        report = (headerId == R.id.sim_card1) ? UsageReporter.ContactsSettingsPage.SETTING_ENTER_IMPORT_FROM_SIM1
                                : UsageReporter.ContactsSettingsPage.SETTING_ENTER_IMPORT_FROM_SIM2;
                    } else {
                        report = UsageReporter.ContactsSettingsPage.SETTING_ENTER_IMPORT_FROM_SIM;
                    }
                    break;
                case SimContactUtils.SIM_MULTI_SELECT_ACTION_EXPORT:
                    if (SimUtil.MULTISIM_ENABLE) {
                        report = (headerId == R.id.sim_card1) ? UsageReporter.ContactsSettingsPage.SETTING_ENTER_EXPORT_TO_SIM1
                                : UsageReporter.ContactsSettingsPage.SETTING_ENTER_EXPORT_TO_SIM2;
                    } else {
                        report = UsageReporter.ContactsSettingsPage.SETTING_ENTER_EXPORT_TO_SIM;
                    }
                    break;
                default:
                    Log.d(TAG, "sendUsageReport() mActionMode:"+mActionMode);
                    break;
            }
        }

        UsageReporter.onClick(null, TAG, report);
    }

    private class HeaderAdapter extends ArrayAdapter<Header> {
        private LayoutInflater mInflater;
        final Resources mResources;

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
                holder.mSummary.setVisibility(View.GONE);

                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            holder.mTitle.setEnabled(getItemEnable(position));
            if (!TextUtils.isEmpty(header.title)) {
                holder.mTitle.setText(header.title);
            } else {
                holder.mTitle.setText(header.getTitle(mResources));
            }

            return view;
        }

        public boolean getItemEnable(int position) {
            Header header = getItem(position);
            long id = header.id;

            if (SimUtil.isAirplaneModeOn(SimMultiSelectActivity.this)
                    && ((id == R.id.sim_card1) || (id == R.id.sim_card2))) {
                return false;
            }
            if (id == R.id.sim_card1
                    && !SimUtil.isSimAvailable(SimUtil.SLOT_ID_1)) {
                return false;
            } else if (id == R.id.sim_card2
                    && !SimUtil.isSimAvailable(SimUtil.SLOT_ID_2)) {
                return false;
            }

            return true;
        }

    }

}
