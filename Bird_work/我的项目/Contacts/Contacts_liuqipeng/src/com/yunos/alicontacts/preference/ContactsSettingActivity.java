
package com.yunos.alicontacts.preference;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
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

/* YUNOS BEGIN */
//##modules(AliContacts) ##author: hongwei.zhw
//##BugID:(8161644) ##date:2016.4.18
//##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
import android.os.Build;
/* YUNOS END */
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BasePreferenceActivity;
import com.yunos.alicontacts.activities.ContactSelectionActivity;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.util.FeatureOptionAssistant;
import com.yunos.common.CloudSyncReceiver;
import com.yunos.common.UsageReporter;

import java.util.List;
import com.yunos.alicontacts.group.GroupMemberShipActivity;
import com.yunos.alicontacts.group.GroupMemberManagementActivity;


public class ContactsSettingActivity extends BasePreferenceActivity {
    /**
     * The action for the contacts settings activity.
     */
    public static final String CONTACTS_SETTINGS_ACTION =
            "com.yunos.alicontacts.action.CONTACTS_SETTINGS_ACTION";

    public static final String TAG = "ContactsSettingActivity";

    private HeaderAdapter mHeaderAdapter;

    /** Profile projection, used to retrieve my personal profile info. */
    private static final String[] PROFILE_PROJECTION = new String[] {
            Contacts._ID, // 0
            Contacts.DISPLAY_NAME_PRIMARY, // 1
            Contacts.LOOKUP_KEY, // 2
    };
    private static final int PROFILE_CONTACT_ID = 0;
    private static final int PROFILE_CONTACT_DISPLAY_NAME = 1;
    private static final int PROFILE_CONTACT_LOOKUP_KEY = 2;

    /** My vCard data */
    private ProfileData mProfileData;

    private String mSyncContactSummary = "";
    private String mSyncCalllogSummary = "";

    /** The key for display favorite contacts on/off */
    public static final String CONTACT_DISPLAY_FAVORITE_PREFERENCE = "display_favorite_contact_preference";
    /** The key for auto favorite contacts on/off */
    public static final String CONTACT_AUTO_FAVORITE_PREFERENCE = "auto_favorite_contact_preference";
    /** The key for contact photo on/off */
    public static final String CONTACT_PHOTO_ONOFF_PREFERENCE = "contact_photo_onoff_preference";
    /** The key for fisheye on/off */
    public static final String CONTACT_FISH_EYES_ORIENTATION_PREFERENCE = "fish_eyes_orientation_preference";

    /** preference switch item value stored by mOnOffList */
    private static class SwitchData {
        boolean onOff;
        String prefKey;

        SwitchData(boolean onOff, String prefKey) {
            this.onOff = onOff;
            this.prefKey = prefKey;
        }
    }

    public static final boolean DEFAULT_DISPLAY_FAVORITE_ON_OFF
            = FeatureOptionAssistant.DEFAULT_DISPLAY_FAVORITES_IN_CONTACTS_LIST;
    public static final boolean DEFAULT_AUTO_FAVORITE_ON_OFF = false;

    private static final SparseArray<SwitchData> mHeaderItemSwitchList;
    static {
        mHeaderItemSwitchList = new SparseArray<SwitchData>(3);
        mHeaderItemSwitchList.put(R.id.display_favorite_switch,
                new SwitchData(DEFAULT_DISPLAY_FAVORITE_ON_OFF, CONTACT_DISPLAY_FAVORITE_PREFERENCE));
        mHeaderItemSwitchList.put(R.id.favorite_switch, new SwitchData(DEFAULT_AUTO_FAVORITE_ON_OFF,
                CONTACT_AUTO_FAVORITE_PREFERENCE));
        mHeaderItemSwitchList.put(R.id.photo_switch, new SwitchData(FeatureOptionAssistant.DEFAULT_PHOTO_ON_OFF,
                CONTACT_PHOTO_ONOFF_PREFERENCE));
    }

    /**
     * mReportList is used to mapping usage report information for click each
     * item.
     */
    private static final SparseArray<String> mReportList;
    static {
        mReportList = new SparseArray<String>();
        mReportList.put(R.id.myvcard,
                UsageReporter.ContactsSettingsPage.SETTING_ENTER_CLICK_MY_VCARD);
        mReportList.put(R.id.cloud_sync, UsageReporter.ContactsSettingsPage.SETTING_SYNC);
        mReportList.put(R.id.calllog_sync,
                UsageReporter.ContactsSettingsPage.SETTING_BACKUP_CALLLOG);
        mReportList.put(R.id.import_contacts,
                UsageReporter.ContactsSettingsPage.SETTING_ENTER_IMPORT_CONTACTS);
        mReportList.put(R.id.export_contacts,
                UsageReporter.ContactsSettingsPage.SETTING_ENTER_EXPORT_CONTACTS);
        mReportList.put(R.id.display_favorite_switch,
                UsageReporter.ContactsSettingsPage.TURN_ON_DISPLAY_FAVORITE);
        mReportList.put(R.id.favorite_switch,
                UsageReporter.ContactsSettingsPage.TURN_ON_RECOMMEND_FAVORITE);
        mReportList.put(R.id.photo_switch,
                UsageReporter.ContactsSettingsPage.TURN_ON_SHOW_HEAD_ICON);
        mReportList.put(R.id.security, UsageReporter.ContactsSettingsPage.SETTING_ENTER_BLACK_LIST);
        mReportList.put(R.id.duplicate_remove,
                UsageReporter.ContactsSettingsPage.SETTING_ENTER_ORGANIZE_CONTACTS);
    }

    private SharedPreferences mDefaultPrefs;
    private OnSharedPreferenceChangeListener mCloudSyncTimeListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, "mCloudSyncTimeListener key:" + key);
            if (CloudSyncReceiver.CONTACT_SYNC_LASTTIME_PREFERENCE.equals(key)) {
                String contactsLastTime = getLastTimeSyncContacts();
                if (mSyncContactSummary == null || !mSyncContactSummary.equals(contactsLastTime)) {
                    mSyncContactSummary = contactsLastTime;
                    invalidateHeaders();
                }
            } else if (CloudSyncReceiver.CALLLOG_SYNC_LASTTIME_PREFERENCE.equals(key)) {
                String calllogLastTime = getLastTimeBackupCalllog();
                if (mSyncCalllogSummary == null || !mSyncCalllogSummary.equals(calllogLastTime)) {
                    mSyncCalllogSummary = calllogLastTime;
                    invalidateHeaders();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
        setTitle2(getString(R.string.contacts_settings));
        showBackKey(true);

        mDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        initHeaderItemSwitchValue();
        mDefaultPrefs.registerOnSharedPreferenceChangeListener(mCloudSyncTimeListener);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings, target);
        updateHeader(target);
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter == null) {
            super.setListAdapter(null);
        } else {
            mHeaderAdapter = new HeaderAdapter(this, getHeaders());
            super.setListAdapter(mHeaderAdapter);
        }

    }

    @Override
    public void onHeaderClick(Header header, int position) {
        /**
         * In settings.xml configuration file we use android:fragment="category"
         * to mark as PreferenceCategory view. so in this statements, we should
         * filter android:fragment="category" label first.
         */
        if (HeaderAdapter.HEADER_CATEGORY.equals(header.fragment)) {
            return;
        }

        if (mHeaderAdapter != null && !mHeaderAdapter.checkEnabled(header)) {
            return;
        }

        super.onHeaderClick(header, position);

        clickHeaderItem(header);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UsageReporter.onResume(this, null);
        loadProfileContact();
        syncCategoryCloudSyncTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        UsageReporter.onPause(this, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDefaultPrefs != null && mCloudSyncTimeListener != null) {
            mDefaultPrefs.unregisterOnSharedPreferenceChangeListener(mCloudSyncTimeListener);
        }
    }

    private void clickHeaderItem(Header header) {
        int headerId = (int) header.id;

        if (headerId == R.id.import_contacts) {
            SimContactUtils.launch(this, SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT);
        } else if (headerId == R.id.export_contacts) {
            SimContactUtils.launch(this, SimContactUtils.SIM_MULTI_SELECT_ACTION_EXPORT);
        } else if (HeaderAdapter.isHeaderItemSwitchType(headerId)) {
            SwitchData sw = mHeaderItemSwitchList.get(headerId);
            boolean checked = sw.onOff;
            final String key = sw.prefKey;
            sw.onOff = !checked;

            writeHeaderItemSwitchValue(key, !checked);

            invalidateHeaders();
        } else if(headerId ==  R.id.group_sendsms){
             Intent intent = new Intent(this,GroupMemberShipActivity.class);
             startActivity(intent);
        } else if(headerId ==  R.id.group_member){
             Intent intent = new Intent(this, GroupMemberManagementActivity.class);
             startActivity(intent);
        }

        sendUsageReport(headerId);
    }

    private void sendUsageReport(int headerId) {
        String report = mReportList.get(headerId);

        if (HeaderAdapter.isHeaderItemSwitchType(headerId)) {
            boolean checked = mHeaderItemSwitchList.get(headerId).onOff;
            if (!checked) {
                if (headerId == R.id.display_favorite_switch) {
                    report = UsageReporter.ContactsSettingsPage.TURN_OFF_DISPLAY_FAVORITE;
                } else if (headerId == R.id.favorite_switch) {
                    report = UsageReporter.ContactsSettingsPage.TURN_OFF_RECOMMEND_FAVORITE;
                } else if (headerId == R.id.photo_switch) {
                    report = UsageReporter.ContactsSettingsPage.TURN_OFF_SHOW_HEAD_ICON;
                }
            }
        }

        UsageReporter.onClick(this, null, report);
    }

    private void syncCategoryCloudSyncTime() {
        String contactsLastTime = getLastTimeSyncContacts();
        String calllogLastTime = getLastTimeBackupCalllog();

        if (mSyncContactSummary == null || !mSyncContactSummary.equals(contactsLastTime)
                || mSyncCalllogSummary == null || !mSyncCalllogSummary.equals(calllogLastTime)) {
            AccountManager am = AccountManager.get(ContactsSettingActivity.this);
            Account[] accounts = am.getAccountsByType(AccountType.YUNOS_ACCOUNT_TYPE);
            if (accounts != null && accounts.length == 1) {
                mSyncContactSummary = contactsLastTime;
                mSyncCalllogSummary = calllogLastTime;
            } else {
                mSyncContactSummary = "";
                mSyncCalllogSummary = "";
            }

            invalidateHeaders();
        }

    }

    private String getLastTimeSyncContacts() {
        String timeStr = "";
        long time = readFromDefaultSharedPreference(this,
                CloudSyncReceiver.CONTACT_SYNC_LASTTIME_PREFERENCE, 0);
        Log.d(TAG, "getLastTimeSyncContacts: time = " + time);
        if (time != 0) {
            CharSequence dateValue = AliCallLogExtensionHelper.dateFormat("yyyy-MM-dd kk:mm", time);
            timeStr = dateValue.toString();
        }
        return timeStr;
    }

    private String getLastTimeBackupCalllog() {
        String timeStr = "";
        long time = readFromDefaultSharedPreference(this,
                CloudSyncReceiver.CALLLOG_SYNC_LASTTIME_PREFERENCE, 0);
        Log.d(TAG, "getLastTimeBackupCalllog: time = " + time);
        if (time != 0) {
            CharSequence dateValue = AliCallLogExtensionHelper.dateFormat("yyyy-MM-dd kk:mm", time);
            timeStr = dateValue.toString();
        }
        return timeStr;
    }

    private void initHeaderItemSwitchValue() {
        if (mHeaderItemSwitchList != null) {

            int size = mHeaderItemSwitchList.size();
            for (int i = 0; i < size; i++) {
                boolean defaultValue = mHeaderItemSwitchList.valueAt(i).onOff;
                String key = mHeaderItemSwitchList.valueAt(i).prefKey;

                boolean value = mDefaultPrefs.getBoolean(key, defaultValue);

                mHeaderItemSwitchList.valueAt(i).onOff = value;
            }
        }
    }

    private void writeHeaderItemSwitchValue(String prefKey, boolean checked) {

        writeBooleanToDefaultSharedPreference(this, prefKey, checked);
    }

    private void loadProfileContact() {
        new LoadProfileTask().execute(null, null, null);
    }

    private class ProfileData {
        Uri lookupUri;
        String name;
    }

    private class LoadProfileTask extends AsyncTask<Void, Void, ProfileData> {

        @Override
        protected ProfileData doInBackground(Void... params) {
            ProfileData profile = null;

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(Profile.CONTENT_URI, PROFILE_PROJECTION,
                        null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    profile = new ProfileData();
                    long contactId = cursor.getLong(PROFILE_CONTACT_ID);
                    profile.name = cursor.getString(PROFILE_CONTACT_DISPLAY_NAME);
                    String lookupKey = cursor.getString(PROFILE_CONTACT_LOOKUP_KEY);

                    profile.lookupUri = Contacts.getLookupUri(contactId, lookupKey);
                }

            } catch (Exception e) {
                Log.e(TAG, "LoadProfileTask", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return profile;
        }

        @Override
        protected void onPostExecute(ProfileData result) {
            if (mProfileData == null || !mProfileData.equals(result)) {
                mProfileData = result;
                invalidateHeaders();
            }

        }

    }

    private void updateHeader(List<Header> target) {
        int i = 0;
        while (i < target.size()) {
            Header header = target.get(i);
            long id = header.id;

            if (id == R.id.myvcard) {
                if (mProfileData != null) {
                    header.summary = mProfileData.name;
                    header.intent = new Intent(Intent.ACTION_VIEW, mProfileData.lookupUri);
                }
            } else if (id == R.id.cloud_sync) {
                header.summary = mSyncContactSummary;
            } else if (id == R.id.calllog_sync) {
                header.summary = mSyncCalllogSummary;
            }else if( id == R.id.category_group || id == R.id.group_sendsms ||id == R.id.group_member){
               if(!getResources().getBoolean(R.bool.config_default_support_group_custom)){
                     target.remove(i);
                     continue;
               }
            }
            /* YUNOS BEGIN */
            //##modules(AliContacts) ##author: hongwei.zhw
            //##BugID:(8161644) ##date:2016.4.18
            //##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
            if (Build.YUNOS_CARRIER_CMCC) {
                 if (id == R.id.category_cloud_sync || id == R.id.cloud_sync || id == R.id.calllog_sync) {
                     target.remove(i);
                     continue;
                 }
            }
            /* YUNOS END */
            i++;
        }
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        public static final String HEADER_CATEGORY = "category";

        public static final int HEADER_TYPE_CATEGORY = 0;
        public static final int HEADER_TYPE_NORMAL = 1;
        public static final int HEADER_TYPE_SWITCH = 2;
        public static final int HEADER_TYPE_BUTTON = 3;
        private static final int HEADER_TYPE_COUNT = HEADER_TYPE_BUTTON + 1;

        private LayoutInflater mInflater;
        final Resources mResources;

        private static class HeaderViewHolder {
            TextView mTitle;
            TextView mSummary;
            Switch mSwitch;
        }

        public static int getHeaderType(Header header) {
            /**
             * In settings.xml configuration file we use
             * android:fragment="category" to mark as PreferenceCategory view.
             * so in this statements, we should filter
             * android:fragment="category" label first.
             */
            if (HEADER_CATEGORY.equals(header.fragment)) {
                return HEADER_TYPE_CATEGORY;
            } else if (isHeaderItemSwitchType(header.id)) {
                return HEADER_TYPE_SWITCH;
            } else {
                return HEADER_TYPE_NORMAL;
            }
        }

        public static boolean isHeaderItemSwitchType(long id) {
            if ((id == R.id.display_favorite_switch)
                    || (id == R.id.favorite_switch)
                    || (id == R.id.photo_switch)) {
                return true;
            }
            return false;
        }

        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mResources = context.getResources();
        }

        @Override
        public boolean isEnabled(int position) {
            if (getItemViewType(position) == HEADER_TYPE_CATEGORY) {
                return false;
            }
            return true;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            Header header = getItem(position);
            int headerType = getHeaderType(header);
            View view = null;

            if (convertView == null) {
                holder = new HeaderViewHolder();

                switch (headerType) {
                    case HEADER_TYPE_CATEGORY:
                        view = mInflater.inflate(R.xml.settings_category, parent, false);
                        holder.mTitle = (TextView) view;
                        break;
                    case HEADER_TYPE_SWITCH:
                    case HEADER_TYPE_NORMAL:
                        view = mInflater.inflate(R.xml.settings_item, parent, false);
                        holder.mTitle = (TextView)
                                view.findViewById(R.id.title);
                        holder.mSummary = (TextView)
                                view.findViewById(R.id.summary);
                        holder.mSwitch = (Switch) view.findViewById(R.id.switchWidget);

                        if (headerType == HEADER_TYPE_SWITCH) {
                            holder.mSwitch.setVisibility(View.VISIBLE);
                        }
                        break;
                    default:
                        Log.e(TAG, "prepare view headerType error!!!");
                        break;
                }

                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    holder.mTitle.setText(header.getTitle(mResources));

                    break;
                case HEADER_TYPE_SWITCH:
                case HEADER_TYPE_NORMAL:
                    holder.mTitle.setEnabled(checkEnabled(header));
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
                        boolean checked = mHeaderItemSwitchList.get((int) header.id).onOff;
                        holder.mSwitch.setChecked(checked);
                        holder.mSwitch.setEnabled(checkEnabled(header));
                    }

                    break;
                default:
                    Log.e(TAG, "prepare data headerType error!!!");
                    break;
            }

            if (holder.mSummary != null) {
                holder.mSummary.setEnabled(checkEnabled(header));
            }
            return view;
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        @Override
        public int getViewTypeCount() {
            return HEADER_TYPE_COUNT;
        }

        public boolean checkEnabled(Header header) {
            long headerId = header.id;
            if (headerId == R.id.favorite_switch) {
                SwitchData sw = mHeaderItemSwitchList.get(R.id.display_favorite_switch);
                if (!sw.onOff) {
                    return false;
                }
            }
            return true;
        }

    }

    public static boolean readFishEyeOrientation(Context context) {
        // return
        // ContactsSettingActivity.readBooleanFromDefaultSharedPreference(context,
        // ContactsSettingActivity.FISH_EYES_ORIENTATION_PREFERENCE, true);
        return true;
    }

    public static boolean readShowContactsHeadIconPreference(Context context) {
        return ContactsSettingActivity.readBooleanFromDefaultSharedPreference(context,
                ContactsSettingActivity.CONTACT_PHOTO_ONOFF_PREFERENCE,
                FeatureOptionAssistant.DEFAULT_PHOTO_ON_OFF);
    }

    public static boolean readBooleanFromDefaultSharedPreference(Context context, String key,
            boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, defaultValue);
    }

    public static void writeBooleanToDefaultSharedPreference(Context context, String key,
            boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(key, value).commit();
    }

    public static long readFromDefaultSharedPreference(Context context, String key,
            long defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(key, defaultValue);
    }
}
