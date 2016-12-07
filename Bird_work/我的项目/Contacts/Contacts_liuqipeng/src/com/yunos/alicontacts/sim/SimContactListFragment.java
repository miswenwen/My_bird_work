
package com.yunos.alicontacts.sim;

import android.accounts.Account;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Entity;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.list.AccountFilterManager;
import com.yunos.alicontacts.list.AccountFilterManager.AccountSelectListener;
import com.yunos.alicontacts.list.AccountFilterManager.WritableAccount;
import com.yunos.alicontacts.list.AccountFilterManager.WritableAccountList;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.ProgressDialog;
import hwdroid.widget.FooterBar.FooterBar;
import yunos.support.v4.app.ListFragment;
import yunos.support.v4.app.LoaderManager.LoaderCallbacks;
import yunos.support.v4.content.CursorLoader;
import yunos.support.v4.content.Loader;
import yunos.support.v4.widget.CursorAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SimContactListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        BaseActivity.OnAllCheckedListener {
    private static final String TAG = "SimContactListFragment";

    /** mark page name for UserReport backward compatibility */
    private static final String PAGE_IMPORT_CONTACTS = "ImportSimContacts";
    private static final String PAGE_EXPORT_CONTACTS = "ExportSimContacts";
    private String mPage;

    private static final String BUNDLE_KEY_EXPORTED_PHONE_COUNT = "exported_phone_count";
    private static final String BUNDLE_KEY_TOTAL_CHECKED_COUNT = "total_checked_count";
    private static final String BUNDLE_KEY_PARTIAL_EXPORTED = "partial_exported";
    private static final String BUNDLE_KEY_SIM_CONTACTS_FULL = "sim_contacts_full";

    private static final String[] EXPORT_CONTACT_PROJECTION = new String[] {
            ContactsContract.Contacts._ID,
            RawContacts.DISPLAY_NAME_PRIMARY
    };
    private static final int EXPORT_CONTACT_COLUMN_CONTACT_ID = 0;
    private static final int EXPORT_CONTACT_COLUMN_DISPLAY_NAME_PRIMARY = 1;

    private static final String[] EXPORT_CONTACT_DETAIL_PROJECTION = new String[] {
            Entity.MIMETYPE,
            Entity.DATA1
    };
    private static final int EXPORT_CONTACT_DETAIL_COLUMN_MIMETYPE = 0;
    private static final int EXPORT_CONTACT_DETAIL_COLUMN_DATA = 1;

    private static final String EXPORT_CONTACT_DETAIL_SELECTION = RawContacts.DELETED + "=0 AND ("
            + Entity.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "' OR "
            + Entity.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "') AND " + RawContacts.CONTACT_ID
            + "=?";

    // time to wait account loaded, we need to select position to import contacts.
    private static final int TIMEOUT_WAIT_LOAD_ACCOUNT = 5000;

    /** */
    private static final int QUERY_SIM_TOKEN = 0;

    /** when canceled, return canceled value. */
    public static final int ACTION_CANCELED = -15;
    /** action failed and catched by exceptions. */
    public static final int ACTION_EXCEPTION = -16;

    private static final int MSG_IMPORT_SIM_CONTACTS = 1;
    private static final int MSG_EXPORT_SIM_CONTACTS = 2;

    public static final int ACTION_MODE_IMPORT_CONTACTS = 1;
    public static final int ACTION_MODE_EXPORT_CONTACTS = 2;
    private int mActionMode;
    private int mSlot;

    private static final int BATCH_COUNT = 40;

    private BaseActivity mActivity;
    private Context mContext;
    private SimNotificationListener mSimNotificationListener;

    private SimListAdapter mAdapter = null;
    private TextView mEmptyView;

    private int mTotalCount;
    private int mCheckedCount;
    private boolean[] mCheckedItems;

    private static final int BUTTON_1 = 0;
    private static final int BUTTON_2 = 1;

    private AlertDialog mDeleteConfirmDialog;
    private ProgressDialog mProgressDialog;
    private QuerySimHandler mQuerySimHandler;
    private LayoutInflater mInflater;
    private View mButtonFooterView;
    private TextView mDoneBtn;

    private ArrayList<ContentValues> mSimList = new ArrayList<ContentValues>();

    private boolean mIs2gSim;
    private static boolean[] sIsActing = new boolean[] {
            false, false
    };
    private boolean mIsCanceled = false;
    private static boolean isEmailFullFlag = false;
    private boolean mIsNeedForceReload;

    public static boolean isActing(int slot) {
        return sIsActing[slot];
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (BaseActivity) activity;
        mContext = mActivity.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = mActivity.getIntent();
        mActionMode = processActionMode(intent);

        if (SimUtil.MULTISIM_ENABLE) {
            mSlot = intent.getIntExtra(SimUtil.SLOT_KEY, SimUtil.SLOT_ID_1);
            mIs2gSim = SimUtil.is2gSim(mSlot);
        } else {
            mIs2gSim = SimUtil.is2gSim();
        }

        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSimNotificationListener = new SimNotificationListener(mContext);

        Log.d(TAG, "onCreate() intent:" + intent + ", mActionMode:" + mActionMode
                + ", mSlot:" + mSlot + ", mIs2gSim:" + mIs2gSim);
        configActionBarView(intent);
        mIsNeedForceReload = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.sim_contact_list_fragment, container, false);

        mEmptyView = (TextView) view.findViewById(R.id.empty_text);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated()");
        startLoading(mActionMode);
    }

    @Override
    public void onResume() {
        super.onResume();
        switch (mActionMode) {
            case ACTION_MODE_IMPORT_CONTACTS:
                mPage = PAGE_IMPORT_CONTACTS;
                break;
            case ACTION_MODE_EXPORT_CONTACTS:
                mPage = PAGE_EXPORT_CONTACTS;
                break;
            default:
                Log.d(TAG, "onResume() mActionMode:" + mActionMode);
                break;
        }

        UsageReporter.onResume(null, mPage);
        Log.d(TAG, "onResume() mActionMode:" + mActionMode + ", mIsNeedForceReload:" + mIsNeedForceReload);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() mActionMode:" + mActionMode);
        if (mPage != null) {
            UsageReporter.onPause(null, mPage);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() mActionMode:" + mActionMode);
        mIsNeedForceReload = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsNeedForceReload = false;
        if (mQuerySimHandler != null) {
            Log.d(TAG, "onDestroy() mQuerySimHandler cancel operation...");
            mQuerySimHandler.cancelOperation(QUERY_SIM_TOKEN);
        }

        mAdapter = null;
        if (mDeleteConfirmDialog != null) {
            mDeleteConfirmDialog.dismiss();
            mDeleteConfirmDialog = null;
        }
    }

    private boolean isActvityExist() {
        if (getActivity() == null ||  mActivity == null || mActivity.isFinishing()) {
            Log.e(TAG, "isActvityExist() mActivity is NULL!!!");
            return false;
        }

        return true;
    }

    @Override
    public void onAllChecked(boolean checked) {
        if (mCheckedItems != null) {
            Arrays.fill(mCheckedItems, checked);
        }
        mActivity.setAllCheckBoxChecked(checked);
        mCheckedCount = checked ? mTotalCount : 0;
        Log.d(TAG, "onAllChecked() checked:" + checked + ", mCheckedCount:" + mCheckedCount);

        updateMultiSelectTitle(mCheckedCount);

        updateFooterBarButton2Enable();

        invalidListView();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        /** click list view items. */
        ViewHolder holder = (ViewHolder) v.getTag();
        boolean checked = mCheckedItems[position] = !mCheckedItems[position];

        Log.d(TAG, "onListItemClick() position:" + position + ", checked:" + checked);
        holder.mCheckBox.setChecked(checked);

        updateMultiSelectTitle(checked ? mCheckedCount++ : mCheckedCount--);
        mActivity.setAllCheckBoxChecked(mCheckedCount == mTotalCount);

        updateFooterBarButton2Enable();
    }

    private void startLoading(int loadId) {
        int resId = R.string.contacts_loading;
        if (loadId == ACTION_MODE_EXPORT_CONTACTS) {
            resId = R.string.contact_list_loading;
        }
        showProgressBar(resId);

        /** query SIM records first when show export contacts list. */
        if (loadId == ACTION_MODE_EXPORT_CONTACTS) {
            querySimRecords();
        } else {
            getLoaderManager().initLoader(loadId, null, this);
        }
    }

    /**
     * QuerySimHandler is used before exporting phone contacts to SIM records.
     * Why should we query SIM records first when export? Because when mobile
     * phone boot/reboot first time, SIM records is not loaded to caches in
     * framework process, when it loads, it will be time-consuming. The proper
     * process is: when phone boot/reboot, SIM is power on, after a series of
     * processes, framework layer receives SIM ICCID LOADED message, at this
     * moment, framework send a command to read ADN records and load ADN records
     * to ADN caches. When applications read/write SIM records, read/write ADN
     * caches will be OK. But YunOS framework hasn't this SIM loading process,
     * so application should query SIM records first.
     */
    private void querySimRecords() {
        final Uri uri;
        if (SimUtil.MULTISIM_ENABLE) {
            uri = SimUtil.getUri(mSlot);
        } else {
            uri = SimUtil.getUri();
        }
        mQuerySimHandler = new QuerySimHandler(mContext.getContentResolver());
        mQuerySimHandler.startQuery(QUERY_SIM_TOKEN, null, uri, null, null, null, null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        switch (id) {
            case ACTION_MODE_IMPORT_CONTACTS: {
                Uri uri = null;
                if (SimUtil.MULTISIM_ENABLE) {
                    uri = SimUtil.getUri(mSlot);
                } else {
                    uri = SimUtil.getUri();
                }
                Log.d(TAG, "onCreateLoader() uri:" + uri + ", id:" + id);
                return new CursorLoader(mContext, uri, null, null, null, null);
            }
            case ACTION_MODE_EXPORT_CONTACTS: {

                /**
                 * filter contacts exception Local and YunOS account contacts
                 * first.
                 */
                Cursor cursor = null;
                String selection = null;
                try {
                    cursor = mContext.getContentResolver().query(
                            RawContacts.CONTENT_URI,
                            new String[] {
                                RawContacts._ID
                            },
                            RawContacts.DELETED + "=0 AND (" + RawContacts.ACCOUNT_TYPE + "='"
                                    + AccountType.LOCAL_ACCOUNT_TYPE + "' OR " + RawContacts.ACCOUNT_TYPE + "='"
                                    + AccountType.YUNOS_ACCOUNT_TYPE + "')", null, null);

                    if (cursor != null) {
                        StringBuilder sb = new StringBuilder();
                        while (cursor.moveToNext()) {
                            if (sb.length() > 0) {
                                sb.append(',');
                            }

                            sb.append(cursor.getLong(0));
                        }

                        if (sb.length() > 0) {
                            selection = Contacts.NAME_RAW_CONTACT_ID + " IN (" + sb.toString() + ")";
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "onCreateLoader() export contacts Exception", e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                if (selection == null) {
                    // we don't find contacts in local/yun accounts,
                    // so make the CursorLoader return nothing.
                    selection = "0=1";
                }
                return new CursorLoader(mContext, ContactsContract.Contacts.CONTENT_URI, EXPORT_CONTACT_PROJECTION,
                        selection, null, Contacts.SORT_KEY_PRIMARY);
            }
            default:
                Log.d(TAG, "onCreateLoader() mActionMode:" + mActionMode);
                return null;
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        dismissProgressBar();

        int loaderId = loader.getId();
        Log.d(TAG, "onLoadFinished() loaderId:" + loaderId);

        if (cursor != null && cursor.getCount() > 0) {
            mTotalCount = cursor.getCount();
            Log.d(TAG, "onLoadFinished() mTotalCount:" + mTotalCount);

            mCheckedItems = new boolean[mTotalCount];

            if (mAdapter == null) {
                mAdapter = new SimListAdapter(mActivity, cursor,
                        loaderId);

                setListAdapter(mAdapter);
            } else {
                mAdapter.swapCursor(cursor);
            }

            /**
             * As AllCheckBox click listener can register only once, or it will
             * not run OK.
             */
            final CheckBox checkbox = mActivity.getAllCheckBox();
            if (checkbox == null) {
                mActivity.showAllCheckBox(this);
            }

        } else {
            mTotalCount = 0;

            if (mAdapter != null) {
                mAdapter.swapCursor(null);
            }

            if (mEmptyView != null) {
                mEmptyView.setVisibility(View.VISIBLE);
                if (mActionMode == ACTION_MODE_EXPORT_CONTACTS) {
                    mEmptyView.setText(mContext.getString(R.string.listTotalAllContactsZero));
                } else {
                    mEmptyView.setText(mContext.getString(R.string.contacts_empty_or_sim_not_ready));
                }

            }
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        if (mActivity != null) {
            mActivity.setAllCheckBoxChecked(false);
            if (mTotalCount > 0) {
                mActivity.showAllCheckBox();
            } else {
                mActivity.hideAllCheckBox();
            }
        }
        mCheckedCount = 0;
        updateFooterBarButton2Enable();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null) {
            mAdapter.swapCursor(null);
        }
    }

    private void showProgressBar(int resId) {
        if (!isActvityExist()) {
            Log.e(TAG, "showProgressBar() mActivity is NULL!!!");
            return;
        }

        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.VISIBLE);
            mEmptyView.setText(mContext.getString(resId));
        }
    }

    private void dismissProgressBar() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    public int processActionMode(Intent intent) {
        int actionMode = ACTION_MODE_IMPORT_CONTACTS;
        String action = intent.getAction();
        if (SimContactUtils.ACTION_IMPORT_CONTACTS.equals(action)) {
            actionMode = ACTION_MODE_IMPORT_CONTACTS;
        } else if (SimContactUtils.ACTION_EXPORT_CONTACTS.equals(action)) {
            actionMode = ACTION_MODE_EXPORT_CONTACTS;
        }

        return actionMode;
    }

    private void configActionBarView(Intent intent) {
        mActivity.showBackKey(true);
        String title = "";
        switch (mActionMode) {
            case ACTION_MODE_IMPORT_CONTACTS:
                title = mContext.getString(R.string.multiselect_title,
                        mCheckedCount);

                setupFooterButtonView(R.string.xxsim_importSimEntry);
                break;
            case ACTION_MODE_EXPORT_CONTACTS:
                title = mContext.getString(R.string.multiselect_title,
                        mCheckedCount);

                setupFooterButtonView(R.string.write_sim_import_default_button);
                break;
            default:
                Log.d(TAG, "configActionBarView() mActionMode:" + mActionMode);
                break;
        }
        mActivity.setTitle2(title);
    }

    private void initDoneFooterView() {
        if (mButtonFooterView == null) {
            mButtonFooterView = LayoutInflater.from(getActivity()).inflate(R.layout.import_export_footer_item,
                    mActivity.getFooterBarImpl());
            mDoneBtn = (TextView) mButtonFooterView.findViewById(R.id.footer_text_id);
            mDoneBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onHandleFooterBarItemClick(BUTTON_2);
                }
            });
        }
    }

    private void setupFooterButtonView(int rightStrResId) {
        if (!isActvityExist()) {
            Log.e(TAG, "setupFooterBarButton() mActivity is NULL!!!");
            return;
        }

        FooterBar footerBar = mActivity.getFooterBarImpl();
        if (footerBar != null) {
            footerBar.removeAllViews();
            initDoneFooterView();
            if (mButtonFooterView != null) {
                // getResources().getColorStateList(R.drawable.footer_delete_selector)
                mDoneBtn.setText(rightStrResId);
                if(rightStrResId == R.string.write_sim_import_default_button) {
                    mDoneBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.bt_export_selector , 0, 0);
                }

                updateFooterBarButton2Enable();
                //footerBar.addView(mButtonFooterView);
                footerBar.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateMultiSelectTitle(int selectedCount) {
        String title = mContext.getString(R.string.multiselect_title,
                mCheckedCount);
        mActivity.setTitle2(title);
    }

    public boolean onHandleFooterBarItemClick(int id) {
        switch (mActionMode) {
            case ACTION_MODE_IMPORT_CONTACTS:
                if (BUTTON_1 == id && mActivity != null) {
                    mActivity.finish();
                } else {
                    importSimContacts();
                }
                break;
            case ACTION_MODE_EXPORT_CONTACTS:
                if (BUTTON_1 == id && mActivity != null) {
                    mActivity.finish();
                } else {
                    exportSimContacts();
                }
                break;
            default:
                Log.d(TAG, "onHandleFooterBarItemClick() mActionMode:" + mActionMode);
                break;
        }

        return true;
    }

    private void importSimContacts() {
        Log.d(TAG, "Click to importSimContacts");
        sIsActing[mSlot] = true;

        setupProgressDlg(R.string.importingSimContactsTitle);

        prepareCheckedSimList();

        new ImportSimContactsThread().start();

        UsageReporter
                .onClick(
                        null,
                        mPage,
                        UsageReporter.ContactsSettingsPage.SETTING_START_IMPORT
                                + ((mSlot == SimUtil.SLOT_ID_1) ? UsageReporter.ContactsSettingsPage.SIM_CARD1
                                        : UsageReporter.ContactsSettingsPage.SIM_CARD2));
    }

    private void prepareCheckedSimList() {
        if (mCheckedItems == null || mAdapter == null) {
            Log.d(TAG, "prepareCheckedSimList() mCheckedItems or mAdapter is NULL!!!");
            return;
        }

        mSimList.clear();
        for (int i = 0; i < mCheckedItems.length; i++) {
            if (mCheckedItems[i]) {
                ContentValues values = new ContentValues();

                Cursor c = (Cursor) mAdapter.getItem(i);
                if (SimUtil.IS_PLATFORM_MTK) {
                    values.put(SimUtil.SIM_INDEX, c.getString(SimUtil.SIM_INDEX_COLUMN));
                }

                final String name = c.getString(SimUtil.SIM_NAME_COLUMN);
                final String number = c.getString(SimUtil.SIM_NUMBER_COLUMN);
                if (!TextUtils.isEmpty(name)) {
                    values.put(SimUtil.SIM_NAME, name);
                } else {
                    values.put(SimUtil.SIM_NAME, "");
                }

                if (!TextUtils.isEmpty(number)) {
                    values.put(SimUtil.SIM_NUMBER, number);
                } else {
                    values.put(SimUtil.SIM_NUMBER, "");
                }

                if (!mIs2gSim) {
                    String anr = c.getString(SimUtil.SIM_ANR_COLUMN);
                    String emails = c.getString(SimUtil.SIM_EMAILS_COLUMN);
                    if (!TextUtils.isEmpty(anr)) {
                        if (anr.endsWith(SimContactUtils.SPLIT_COMMA) && (ACTION_MODE_IMPORT_CONTACTS == mActionMode)) {
                            anr = anr.substring(0, anr.length() - 1);
                        }
                        values.put(SimUtil.SIM_ANR, anr);
                    } else {
                        values.put(SimUtil.SIM_ANR, "");
                    }

                    if (!TextUtils.isEmpty(emails)) {
                        if (emails.endsWith(SimContactUtils.SPLIT_COMMA) && (ACTION_MODE_IMPORT_CONTACTS == mActionMode)) {
                            emails = emails.substring(0, emails.length() - 1);
                        }
                        values.put(SimUtil.SIM_EMAILS, emails);
                    } else {
                        values.put(SimUtil.SIM_EMAILS, "");
                    }
                }

                mSimList.add(values);
            }
        }
    }

    private void exportSimContacts() {
        Log.d(TAG, "Click to exportSimContacts");
        if (mAdapter == null || mCheckedItems == null) {
            Log.d(TAG, "exportSimContacts() mAdapter or mCheckedItems is NULL!!!");
            return;
        }

        sIsActing[mSlot] = true;

        int nonameCount = 0;
        final ArrayList<ExportContactsData> exportList = new ArrayList<ExportContactsData>();
        for (int i = 0; i < mCheckedItems.length; i++) {
            if (mCheckedItems[i]) {
                Cursor c = (Cursor) mAdapter.getItem(i);
                long contactId = c.getLong(EXPORT_CONTACT_COLUMN_CONTACT_ID);
                String displayName = c
                        .getString(EXPORT_CONTACT_COLUMN_DISPLAY_NAME_PRIMARY);
                if (TextUtils.isEmpty(displayName)) {
                    Log.w(TAG, "exportSimContacts() contactId:" + contactId + " displayName is NULL, ignore it.");
                    nonameCount++;
                    continue;
                }
                exportList.add(new ExportContactsData(contactId, displayName));
            }
        }

        if (exportList.size() > 0) {
            setupProgressDlg(R.string.write_to_sim_dialog_title);
            new ExportSimContactsThread(exportList).start();
        }

        if (nonameCount > 0) {
            String msgFormat = getResources().getQuantityText(R.plurals.sim_export_skip_noname, nonameCount).toString();
            String msg = String.format(msgFormat, nonameCount);
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
        UsageReporter
                .onClick(
                        null,
                        mPage,
                        UsageReporter.ContactsSettingsPage.SETTING_START_EXPORT
                                + ((mSlot == SimUtil.SLOT_ID_1) ? UsageReporter.ContactsSettingsPage.SIM_CARD1
                                        : UsageReporter.ContactsSettingsPage.SIM_CARD2));
    }

    private void setupProgressDlg(int titleResId) {
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setMessage(mContext.getString(titleResId));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(mCheckedCount);
        mProgressDialog.setProgress(0);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE,
                mContext.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                        Log.d(TAG, "[setupProgressDlg] onClick() cancel");
                        mIsCanceled = true;
                    }
                });
        mProgressDialog
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mCheckedCount = 0;
                    }
                });
        mProgressDialog.show();
    }

    /**
     * when in SIM list delete mode screen, or Import/Export screen,
     * Delete/Import/Export button should be disabled when mCheckedCount is
     * zero, or should be enabled.
     */
    private void updateFooterBarButton2Enable() {
        if (mDoneBtn != null) {
            mDoneBtn.setEnabled(mCheckedCount > 0);
        }
    }

    private void invalidListView() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private static int sCurrentJobId = (int) (System.nanoTime() & 0x0FFFFFFF);
    private static SparseArray<CancelListener> sJobCancelListeners = new SparseArray<CancelListener>();

    private static synchronized int accquireJobId() {
        return sCurrentJobId++;
    }

    private static void registerCancelListener(int jobId, CancelListener l) {
        synchronized (sJobCancelListeners) {
            sJobCancelListeners.put(jobId, l);
        }
    }

    private static void unregisterCancelListener(int jobId) {
        synchronized (sJobCancelListeners) {
            sJobCancelListeners.remove(jobId);
        }
    }

    public static boolean isJobRunning(int jobId) {
        synchronized (sJobCancelListeners) {
            return sJobCancelListeners.get(jobId) != null;
        }
    }

    public static boolean cancelJob(int jobId) {
        CancelListener l = null;
        synchronized (sJobCancelListeners) {
            l = sJobCancelListeners.get(jobId);
        }
        if (l != null) {
            l.onExternalCancel();
            return true;
        }
        return false;
    }

    private interface CancelListener {
        public void onExternalCancel();
    }

    private class ImportSimContactsThread extends Thread implements CancelListener, AccountSelectListener {

        private Account mAccount = null;
        ImportSimContactsThread() {
            super("ImportSimContactsThread");
        }

        @Override
        public void run() {
            if (mContext == null || mSimList == null || mSimList.size() == 0) {
                Log.e(TAG, "[ImportSimContactsThread] run() mSimList or mContext is NULL!!!");
                quitEarly();
                return;
            }

            if ((mAccount == null) && (!loadAccount(mContext))) {
                Log.e(TAG, "[ImportSimContactsThread] run() Cannot determine account!!!");
                quitEarly();
                return;
            }

            final ContentResolver resolver = mContext.getContentResolver();
            if (resolver == null) {
                Log.e(TAG, "[ImportSimContactsThread] run() resolver is NULL!!!");
                quitEarly();
                return;
            }

            int count = 0;
            int currentCount = 0;
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            final int checkedCount = mSimList.size();
            boolean isThrowException = false;
            final int jobId = accquireJobId();
            try {
                registerCancelListener(jobId, this);
                int batchCount = BATCH_COUNT;
                if (checkedCount <= BATCH_COUNT) {
                    batchCount = checkedCount / 3;
                }

                for (ContentValues values : mSimList) {
                    if (mIsCanceled) {
                        Log.d(TAG, "[ImportSimContactsThread] run() canceled.");
                        break;
                    }

                    addSimRecordToOperationList(operationList, values);

                    count++;
                    currentCount++;

                    if (count >= batchCount) {
                        resolver.applyBatch(ContactsContract.AUTHORITY, operationList);

                        if (mProgressDialog != null) {
                            mProgressDialog.incrementProgressBy(count);
                        }

                        if (mSimNotificationListener != null) {
                            String displayName = values.getAsString(SimUtil.SIM_NAME);

                            mSimNotificationListener.onProcessing(ACTION_MODE_IMPORT_CONTACTS,
                                    displayName, mSlot, jobId, checkedCount, currentCount);
                        }

                        operationList.clear();
                        count = 0;
                    }
                }

                if (count > 0) {
                    resolver.applyBatch(ContactsContract.AUTHORITY, operationList);

                    if (mProgressDialog != null) {
                        mProgressDialog.incrementProgressBy(count);
                    }

                    if (mSimNotificationListener != null) {
                        ContentValues values = mSimList.get(mSimList.size() - 1);
                        String displayName = "";
                        if (values != null) {
                            displayName = values.getAsString(SimUtil.SIM_NAME);
                        }

                        mSimNotificationListener.onProcessing(ACTION_MODE_IMPORT_CONTACTS,
                                displayName, mSlot, jobId, checkedCount, currentCount);
                    }

                    operationList.clear();
                    count = 0;
                }

            } catch (Exception e) {
                isThrowException = true;
                Log.d(TAG, "[ImportSimContactsThread] Exception:", e);
            } finally {
                Log.d(TAG, "[ImportSimContactsThread] run() finished");
                unregisterCancelListener(jobId);
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }

                if (mSimNotificationListener != null) {
                    mSimNotificationListener.onProcessQuit(ACTION_MODE_IMPORT_CONTACTS, jobId);
                }

                Message msg = mHandler.obtainMessage(MSG_IMPORT_SIM_CONTACTS);
                if (isThrowException) {
                    msg.arg1 = ACTION_EXCEPTION;
                } else if (mIsCanceled) {
                    msg.arg1 = ACTION_CANCELED;
                    mIsCanceled = false;
                } else {
                    msg.arg1 = 0;
                }

                msg.arg2 = currentCount;
                msg.obj = Integer.valueOf(checkedCount);

                mHandler.sendMessage(msg);

                sIsActing[mSlot] = false;
            }
        }

        private void quitEarly() {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            sIsActing[mSlot] = false;
        }

        private boolean loadAccount(Context context) {
            final List<WritableAccount> accounts = new ArrayList<WritableAccount>();
            AccountFilterManager afm = AccountFilterManager.getInstance(context);
            boolean loaded = afm.waitLoaded(TIMEOUT_WAIT_LOAD_ACCOUNT);
            if (!loaded) {
                mAccount = new Account(AccountType.LOCAL_ACCOUNT_NAME, AccountType.LOCAL_ACCOUNT_TYPE);
                return true;
            }
            // import sim contacts, only allow non-SIM accounts.
            WritableAccountList accountSet = afm.getAccountList();
            WritableAccount[] array = accountSet.mWritableAccounts.toArray(
                    new WritableAccount[accountSet.mWritableAccounts.size()]);
            for (WritableAccount account : array) {
                if (!SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.accountType)) {
                    accounts.add(account);
                }
            }
            int count = accounts.size();
            if (count == 0) {
                mAccount = new Account(AccountType.LOCAL_ACCOUNT_NAME, AccountType.LOCAL_ACCOUNT_TYPE);
                return true;
            }
            if (count == 1) {
                WritableAccount account = accounts.get(0);
                mAccount = new Account(account.accountName, account.accountType);
                return true;
            }
            afm.selectPhoneAccount(getActivity(), this);
            return false;
        }

        private void addSimRecordToOperationList(ArrayList<ContentProviderOperation> operationList,
                ContentValues values) {
            String anrs = null;
            String emails = null;
            String name = values.getAsString(SimUtil.SIM_NAME);
            String number = values.getAsString(SimUtil.SIM_NUMBER);

            /** raw_contact_id */
            int backReferenceIndex = operationList.size();
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newInsert(RawContacts.CONTENT_URI);
            builder.withValue(RawContacts.ACCOUNT_NAME, mAccount.name);
            builder.withValue(RawContacts.ACCOUNT_TYPE, mAccount.type);
            // disable aggregation, to avoid the imported contact disappears (it might join into another contact).
            builder.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
            operationList.add(builder.build());

            /** display_name */
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, backReferenceIndex);
            builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(StructuredName.DISPLAY_NAME, name);
            operationList.add(builder.build());

            /** number */
            if (!TextUtils.isEmpty(number)) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, backReferenceIndex);
                builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                builder.withValue(Phone.TYPE, Phone.TYPE_MOBILE);
                builder.withValue(Phone.NUMBER, number);
                builder.withValue(Data.IS_PRIMARY, 1);
                operationList.add(builder.build());
            }

            if (!mIs2gSim) {
                anrs = values.getAsString(SimUtil.SIM_ANR);
                emails = values.getAsString(SimUtil.SIM_EMAILS);

                /** anr number */
                if (!TextUtils.isEmpty(anrs)) {
                    builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                    builder.withValueBackReference(Phone.RAW_CONTACT_ID,
                            backReferenceIndex);
                    builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                    builder.withValue(Phone.TYPE, Phone.TYPE_HOME);
                    builder.withValue(Phone.NUMBER, anrs);
                    operationList.add(builder.build());
                }

                /** email */
                if (!TextUtils.isEmpty(emails)) {
                    builder = ContentProviderOperation
                            .newInsert(Data.CONTENT_URI);
                    builder.withValueBackReference(Email.RAW_CONTACT_ID,
                            backReferenceIndex);
                    builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                    builder.withValue(Email.TYPE, Email.TYPE_WORK);
                    builder.withValue(Email.ADDRESS, emails);
                    builder.withValue(Data.IS_PRIMARY, 1);
                    operationList.add(builder.build());
                }
            }

            // restore aggregation mode to default.
            builder = ContentProviderOperation.newUpdate(RawContacts.CONTENT_URI);
            builder.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DEFAULT);
            builder.withSelection(RawContacts._ID + "=?", new String[1]);
            builder.withSelectionBackReference(0, backReferenceIndex);
            operationList.add(builder.build());

        }

        @Override
        public void onExternalCancel() {
            mIsCanceled = true;
        }

        @Override
        public void onAccountSelected(WritableAccount account) {
            ImportSimContactsThread another = new ImportSimContactsThread();
            another.mAccount = new Account(account.accountName, account.accountType);
            another.start();
        }

        @Override
        public void onAccountSelectionCanceled() {
            // maybe do nothing...
        }

        @Override
        public void onLoadTimeout() {
            // maybe do nothing...
        }
    }

    private class ExportSimContactsThread extends Thread implements CancelListener {
        private ArrayList<ExportContactsData> mList;

        ExportSimContactsThread(ArrayList<ExportContactsData> list) {
            super("ExportSimContactsThread");
            mList = list;
        }

        @Override
        public void run() {
            if (mContext == null || mList == null || mList.size() == 0) {
                Log.e(TAG, "[ExportSimContactsThread] run() mContext is NULL!!!");
                sIsActing[mSlot] = false;
                return;
            }

            final ContentResolver resolver = mContext.getContentResolver();
            if (resolver == null) {
                Log.e(TAG, "[ExportSimContactsThread] run() resolver is NULL!!!");
                sIsActing[mSlot] = false;
                return;
            }

            int phoneCount = 0;
            int simCount = 0;
            int result[] = new int[] {SimUtil.ERROR_SIM_NO_ERROR, 0};
            final int checkedCount = mList.size();
            boolean needReloadSimContacts = false;
            boolean partialExported = false;
            boolean isSimFull = false;
            final int jobId = accquireJobId();
            try {
                registerCancelListener(jobId, this);
                final LinkedList<String> phoneList = new LinkedList<String>();
                final LinkedList<String> emailList = new LinkedList<String>();
                final ContentValues values = new ContentValues();
                isEmailFullFlag = false;

                for (ExportContactsData data : mList) {
                    if (mIsCanceled) {
                        Log.d(TAG, "[ExportSimContactsThread] run() mIsCanceled:" + mIsCanceled);
                        break;
                    }

                    prepareContactData(resolver, data.contactId, phoneList, emailList);

                    needReloadSimContacts = true;

                    if (mProgressDialog != null) {
                        mProgressDialog.incrementProgressBy(1);
                    }

                    if (phoneList.size() == 0 && emailList.size() == 0) {
                        Log.d(TAG, "[ExportSimContactsThread] run() data:" + data
                                + " number is NULL!!!");
                        continue;
                    } else {
                        result = exportOneContactToSim(data.displayName, phoneList, emailList,
                                values);

                        /**
                         * result[1] means the total count of insert successful.
                         */
                        simCount += result[1];
                        if (result[1] > 0) {
                            phoneCount++;
                        }

                        if (isResultError(result[0])) {
                            Log.e(TAG, "[ExportSimContactsThread] run() ERROR!!!");
                            /* YUNOS BEGIN PB */
                            //##email:caixiang.zcx@alibaba-inc.com
                            //##BugID:(8206447) ##date:2016/05/12
                            //##description:show sim storage full message in qcom plateform
                            if (SimUtil.IS_PLATFORM_QCOMM
                                    && (SimContactUtils.getSimContactsCount(mContext, mSlot) >= SimUtil.getMSimCardMaxCount(mSlot))) {
                                isSimFull = true;
                            }
                            /* YUNOS END PB */
                            if (result[1] > 0) {
                                partialExported = true;
                            }
                            break;
                        }

                        if (mSimNotificationListener != null) {
                            mSimNotificationListener.onProcessing(ACTION_MODE_EXPORT_CONTACTS,
                                    data.displayName, mSlot, jobId, checkedCount, phoneCount);
                        }

                    }

                }
            } catch (Exception e) {
                Log.e(TAG, "[ExportSimContactsThread] Exception", e);
            } finally {
                Log.d(TAG, "[ExportSimContactsThread] run() finished, phoneCount:" + phoneCount
                        + ", simCount:" + simCount);
                unregisterCancelListener(jobId);
                if (needReloadSimContacts) {
                    SimContactLoadService.notifyReloadSimContacts(mContext, mSlot);
                }
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }

                if (mSimNotificationListener != null) {
                    mSimNotificationListener.onProcessQuit(ACTION_MODE_EXPORT_CONTACTS, jobId);
                }

                if (mIsCanceled) {
                    mIsCanceled = false;
                    result[0] = ACTION_CANCELED;
                }

                Message msg = mHandler.obtainMessage(MSG_EXPORT_SIM_CONTACTS);
                msg.arg1 = result[0];
                msg.arg2 = simCount;
                Bundle count = new Bundle(2);
                count.putInt(BUNDLE_KEY_EXPORTED_PHONE_COUNT, phoneCount);
                count.putInt(BUNDLE_KEY_TOTAL_CHECKED_COUNT, checkedCount);
                count.putBoolean(BUNDLE_KEY_PARTIAL_EXPORTED, partialExported);
                /* YUNOS BEGIN PB */
                //##email:caixiang.zcx@alibaba-inc.com
                //##BugID:(8206447) ##date:2016/05/12
                //##description:show sim storage full message in qcom plateform
                count.putBoolean(BUNDLE_KEY_SIM_CONTACTS_FULL, isSimFull);
                /* YUNOS END PB */
                msg.obj = count;
                mHandler.sendMessage(msg);

                sIsActing[mSlot] = false;
            }
        }

        private void prepareContactData(final ContentResolver resolver, long contactId,
                List<String> phoneList, List<String> emailList) {

            Cursor cursor = null;
            try {
                cursor = resolver.query(RawContactsEntity.CONTENT_URI,
                        EXPORT_CONTACT_DETAIL_PROJECTION,
                        EXPORT_CONTACT_DETAIL_SELECTION, new String[] {
                            String.valueOf(contactId)
                        }, null);

                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        String mimeType = cursor.getString(EXPORT_CONTACT_DETAIL_COLUMN_MIMETYPE);
                        String data = cursor.getString(EXPORT_CONTACT_DETAIL_COLUMN_DATA);

                        if (!TextUtils.isEmpty(data) && data.endsWith(SimContactUtils.SPLIT_COMMA)) {
                            data = data.substring(0, data.length() - 1);
                        }

                        if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                            phoneList.add(data);
                        } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                            emailList.add(data);
                        }

                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "addContactDetailToList Exception", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private int[] exportOneContactToSim(String displayName, List<String> phoneList,
                List<String> emailList, ContentValues values) {
            /**
             * result[0] return insert result, if isResultError() return true,
             * means insert operation will abort. result[1] return insert
             * successful total count.
             */
            int result[] = new int[] {
                    SimUtil.ERROR_SIM_NO_ERROR, 0
            };
            final String name = SimContactUtils.trimSimName(displayName);

            while (phoneList.size() > 0) {
                result[0] = actuallyInsertOneSimRecord(name, phoneList, emailList, values);
                if (isResultError(result[0])) {
                    return result;
                } else if (result[0] == SimUtil.ERROR_SIM_NO_ERROR) {
                    result[1]++;
                }
            }

            while (!mIs2gSim && emailList.size() > 0) {
                result[0] = actuallyInsertOneSimRecord(name, phoneList, emailList, values);
                if (isResultError(result[0])) {
                    return result;
                } else if (result[0] == SimUtil.ERROR_SIM_NO_ERROR) {
                    result[1]++;
                }
            }

            phoneList.clear();
            emailList.clear();
            return result;
        }

        /**
         * @return true if SIM insert fail.
         * @param result is the result of SIM insert.
         * Now SIM card have these insert fail return type as below:
         * ERROR_SIM_NO_ERROR = 1;
         * ERROR_SIM_UNKNOWN = 0;
         * ERROR_SIM_NUMBER_TOO_LONG = -1;
         * ERROR_SIM_TEXT_TOO_LONG = -2;
         * ERROR_SIM_STORAGE_FULL = -3;
         * ERROR_SIM_NOT_READY = -4;
         * ERROR_SIM_PASSWORD_ERROR = -5;
         * ERROR_SIM_ANR_TOO_LONG = -6;
         * ERROR_SIM_GENERIC_FAILURE = -10;
         * ERROR_SIM_ADN_LIST_NOT_EXIST = -11;
         * ERROR_SIM_EMAIL_FULL = -12;
         * ERROR_SIM_EMAIL_TOO_LONG = -13;
         * ERROR_SIM_QCOM_ERROR = -14;
         * We think, when SIM batch inserted, if return value in
         * isResultError method, SIM batch insert will abort.
         */
        private boolean isResultError(int result) {
            return (result == SimUtil.ERROR_SIM_STORAGE_FULL
                    || result == SimUtil.ERROR_SIM_NOT_READY
                    || result == SimUtil.ERROR_SIM_PASSWORD_ERROR
                    || result == SimUtil.ERROR_SIM_ADN_LIST_NOT_EXIST
                    || result == SimUtil.ERROR_SIM_ANR_SAVE_FAILURE
                    /* YUNOS BEGIN PB */
                    //##email:caixiang.zcx@alibaba-inc.com
                    //##BugID:(8206447) ##date:2016/05/12
                    //##description:show sim storage full message in qcom plateform
                    || (result == SimUtil.ERROR_SIM_UNKNOWN && SimUtil.IS_PLATFORM_QCOMM));
                    /* YUNOS END PB */
        }

        private int actuallyInsertOneSimRecord(String actualName, List<String> phoneList,
                List<String> emailList, ContentValues values) {
            values.clear();
            values.put(SimUtil.SIM_NAME, actualName);

            String data;
            if (!phoneList.isEmpty()) {
                data = phoneList.remove(0);
                String number = SimContactUtils.trimSimNumber(data);
                values.put(SimUtil.SIM_NUMBER, number);
            }

            if (!mIs2gSim) {
                if (!phoneList.isEmpty()) {
                    data = phoneList.remove(0);
                    String anr = SimContactUtils.trimSimNumber(data);
                    values.put(SimUtil.SIM_ANR, anr);
                }

                if (!emailList.isEmpty()) {
                    data = emailList.remove(0);
                    String email = SimContactUtils.trimSimEmail(data);

                    if (!TextUtils.isEmpty(email)
                            && email.length() <= SimContactUtils.SIM_EMAIL_MAX_LENGTH
                            && !isEmailFullFlag) {
                        /* YUNOS BEGIN PB */
                        //##email:caixiang.zcx@alibaba-inc.com
                        //##BugID:(8254263) ##date:2016/05/20
                        //##description:do not export email,when sim card email storage full.
                        if (SimUtil.IS_PLATFORM_QCOMM && SimUtil.getSpareEmailCount(mSlot) <= 0) {
                            isEmailFullFlag = true;
                        } else {
                            values.put(SimUtil.SIM_EMAILS, email);
                        }
                        /* YUNOS END PB */
                    }
                }
            }

             Uri uri;
            if (SimUtil.MULTISIM_ENABLE) {
                uri = SimUtil.insert(mContext, mSlot, values);
            } else {
                uri = SimUtil.insert(mContext, values);
            }

            int result = processSimError(uri);
            Log.d(TAG, "[export] actuallyInsertOneSimRecord() values:[" + values + "], uri:" + uri
                    + ", result:" + result);
/*YunOS BEGIN PB*/
//##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
//##BugID:(8252759) ##date:2016-5-12 09:00
//##description:when export to sim,the compacity of email Achieve  threshold will interrupt export
            if(result == SimUtil.ERROR_SIM_EMAIL_FULL && !isEmailFullFlag){
                values.put(SimUtil.SIM_EMAILS,"");
                if (SimUtil.MULTISIM_ENABLE) {
                    uri = SimUtil.insert(mContext, mSlot, values);
                } else {
                    uri = SimUtil.insert(mContext, values);
                }
                isEmailFullFlag=true;
                result = processSimError(uri);
                 Log.d(TAG, "[export] emailfull actuallyInsertOneSimRecord() values:[" + values + "], uri:" + uri
                    + ", result:" + result);
            }
/*YUNOS END PB*/
            return result;
        }

        private int processSimError(Uri uri) {
            int errorType = SimUtil.ERROR_SIM_NO_ERROR;

            if (uri != null) {
                int errorPosition = uri.toString().indexOf(SimContactUtils.SAVE_SIM_CONTACTS_ERROR);
                if (errorPosition != -1) {
                    final String error = uri.getLastPathSegment();
                    try {
                        errorType = Integer.valueOf(error);
                    } catch (NumberFormatException e) {
                        errorType = SimUtil.ERROR_SIM_UNKNOWN;
                        Log.e(TAG, "[processSimError] NumberFormatException:", e);
                    }
                }
            } else {
                errorType = SimUtil.ERROR_SIM_UNKNOWN;
            }

            return errorType;
        }

        @Override
        public void onExternalCancel() {
            mIsCanceled = true;
        }
    }

    private class QuerySimHandler extends AsyncQueryHandler {
        public QuerySimHandler(ContentResolver resolver) {
            super(resolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (token == QUERY_SIM_TOKEN) {
                Log.d(TAG, "[QuerySimHandler] onQueryComplete() start query phone contacts...");
                if (isActvityExist()) {
                    getLoaderManager().initLoader(ACTION_MODE_EXPORT_CONTACTS, null,
                            SimContactListFragment.this);
                }
            }
        }
    }

    private class ExportContactsData {
        final long contactId;
        final String displayName;

        ExportContactsData(final long contactId, final String displayName) {
            this.contactId = contactId;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return "[ExportContactsData]:(" + contactId + ", " + displayName + ")";
        }

    }

    private class ViewHolder {
        TextView mNameView;
        TextView mNumberView;
        CheckBox mCheckBox;
    }

    private class SimListAdapter extends CursorAdapter {
        private int mActionMode;

        public SimListAdapter(Context context, Cursor c, int actionMode) {
            super(context, c, false);
            mActionMode = actionMode;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            switch (mActionMode) {
                case ACTION_MODE_IMPORT_CONTACTS: {
                    final ViewHolder holder = (ViewHolder) view.getTag();

                    String name = cursor.getString(SimUtil.SIM_NAME_COLUMN);
                    String number = cursor.getString(SimUtil.SIM_NUMBER_COLUMN);

                    if (!TextUtils.isEmpty(name)) {
                        holder.mNameView.setText(name);
                    } else {
                        holder.mNameView.setText(R.string.missing_name);
                    }

                    holder.mNumberView.setText(number);

                }
                    break;
                case ACTION_MODE_EXPORT_CONTACTS: {
                    final ViewHolder holder = (ViewHolder) view.getTag();
                    String displayName = cursor
                            .getString(EXPORT_CONTACT_COLUMN_DISPLAY_NAME_PRIMARY);

                    if (!TextUtils.isEmpty(displayName)) {
                        holder.mNameView.setText(displayName);
                    } else {
                        holder.mNameView.setText(R.string.missing_name);
                    }
                }
                    break;
                default:
                    Log.d(TAG, "bindView() mActionMode:" + mActionMode);
                    break;
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            ViewHolder holder = new ViewHolder();
            final View view = mInflater.inflate(R.layout.sim_contact_list_item, parent, false);
            holder.mNameView = (TextView) view.findViewById(R.id.name);
            holder.mNumberView = (TextView) view.findViewById(R.id.number);
            holder.mCheckBox = (CheckBox) view.findViewById(R.id.checkbox);

            switch (mActionMode) {
                case ACTION_MODE_IMPORT_CONTACTS:
                    holder.mCheckBox.setVisibility(View.VISIBLE);
                    break;
                case ACTION_MODE_EXPORT_CONTACTS:
                    holder.mNumberView.setVisibility(View.GONE);
                    holder.mCheckBox.setVisibility(View.VISIBLE);
                    break;
                default:
                    Log.d(TAG, "newView() mActionMode:" + mActionMode);
                    break;
            }

            view.setTag(holder);
            return view;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            ViewHolder holder = (ViewHolder) view.getTag();

            /*YunOS BEGIN PB*/
          //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
          //##BugID:(8216984) ##date:2016-5-2 09:00
          //##description:ArrayIndexOutOfBoundsException
            if(mCheckedItems.length <= position){
                //yuncloud data,and export to sim,so the data in the export list grow up gradually.
                holder.mCheckBox.setChecked(false);
            }else{
                holder.mCheckBox.setChecked(mCheckedItems[position]);
            }
            /*YUNOS END PB*/
            return view;
        }

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!isActvityExist() || isDetached()) {
                Log.e(TAG, "handleMessage() mActivity is NULL!!!");
                return;
            }

            mCheckedCount = 0;

            switch (msg.what) {
                case MSG_IMPORT_SIM_CONTACTS: {
                    final int result = msg.arg1;
                    final int count = msg.arg2;
                    final int total = ((Integer) msg.obj).intValue();
                    if (result == ACTION_EXCEPTION) {
                        Toast.makeText(mContext, mContext.getString(R.string.sim_import_fail), Toast.LENGTH_SHORT).show();
                    } else if (msg.arg1 == ACTION_CANCELED) {
                        Toast.makeText(
                                mContext,
                                total == 1 ? mContext.getString(R.string.sim_import_cancel_only_one) : mContext.getString(
                                        R.string.sim_import_cancel, count), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(
                                mContext,
                                total == 1 ? mContext.getString(R.string.sim_import_result_for_one) : mContext.getString(
                                        R.string.sim_import_result, total), Toast.LENGTH_SHORT).show();
                    }

                    if (mActivity != null) {
                        mActivity.finish();
                    }
                }
                    break;
                case MSG_EXPORT_SIM_CONTACTS: {
                    int result = msg.arg1;
                    int simCount = msg.arg2;
                    Bundle count = (Bundle) msg.obj;
                    int phoneCount = count.getInt(BUNDLE_KEY_EXPORTED_PHONE_COUNT);
                    int checkedCount = count.getInt(BUNDLE_KEY_TOTAL_CHECKED_COUNT);
                    boolean partialExported = count.getBoolean(BUNDLE_KEY_PARTIAL_EXPORTED);
                    /* YUNOS BEGIN PB */
                    //##email:caixiang.zcx@alibaba-inc.com
                    //##BugID:(8206447) ##date:2016/05/12
                    //##description:show sim storage full message in qcom plateform
                    boolean isSimFull = count.getBoolean(BUNDLE_KEY_SIM_CONTACTS_FULL);
                    /* YUNOS END PB */

                    Log.e(TAG, "handleMessage() result:" + result);
                    if (result == ACTION_CANCELED) {
                        showExportResultDialog(phoneCount, simCount, checkedCount, partialExported);
                        return;
                    } else if (result == SimUtil.ERROR_SIM_STORAGE_FULL) {
                        if (simCount > 0) {
                            showExportResultDialog(phoneCount, simCount, checkedCount, partialExported);
                            return;
                        } else {
                            Toast.makeText(mContext,
                                    mContext.getString(R.string.export_to_sim_full),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else if (result == SimUtil.ERROR_SIM_NOT_READY
                            || result == SimUtil.ERROR_SIM_PASSWORD_ERROR
                            || result == SimUtil.ERROR_SIM_GENERIC_FAILURE
                            || result == SimUtil.ERROR_SIM_ADN_LIST_NOT_EXIST
                            || result == SimUtil.ERROR_SIM_ANR_SAVE_FAILURE) {
                        Toast.makeText(mContext, mContext.getString(R.string.export_to_sim_failed),
                                Toast.LENGTH_SHORT).show();
                    } else if (result == SimUtil.ERROR_SIM_UNKNOWN) {
                        /* YUNOS BEGIN PB */
                        //##email:caixiang.zcx@alibaba-inc.com
                        //##BugID:(8206447) ##date:2016/05/12
                        //##description:show sim storage full message in qcom plateform
                        if (simCount > 0) {
                            showExportResultDialog(phoneCount, simCount, checkedCount,
                                    partialExported);
                            return;
                        } else {
                            if (SimUtil.IS_PLATFORM_QCOMM && isSimFull) {
                                Toast.makeText(mContext,
                                        mContext.getString(R.string.export_to_sim_full),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mContext,
                                        mContext.getString(R.string.sim_export_fail),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        /* YUNOS END PB */
                    } else {

                        if (simCount > 0) {
                            Toast.makeText(
                                    mContext,
                                    makeExportSuccessMessage(phoneCount, simCount),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, mContext.getString(R.string.sim_export_fail),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (mActivity != null) {
                        mActivity.finish();
                    }
                }
                    break;
                default:
                    Log.d(TAG, "handleMessage() mActionMode:" + mActionMode);
                    break;
            }

        }

    };

    private void showExportResultDialog(int phoneCount, int simCount, int checkedCount, boolean partialExported) {
        if (!isActvityExist()) {
            Log.e(TAG, "showCancelDialog() mActivity is NULL!!!");
            return;
        }

        String message = makeExportResultMessage(phoneCount, simCount, checkedCount, partialExported);
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mActivity.finish();
                                return;
                            }
                        })
                .setPositiveButton(R.string.contacts_settings_yunsync,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                         /*YunOS BEGIN PB*/
                        //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
                        //##BugID:(8258946) ##date:2016-5-13 09:00
                        //##description:remove  syncContacts which will cause contacts crash without com.aliyun.xiayunmi
                         if (!Build.YUNOS_CARRIER_CMCC) {
                                Intent intent = new Intent(
                                        "com.aliyun.xiaoyunmi.action.SELECT_SYNC");
                                intent.setData(Uri
                                        .parse("yunmi://sync_select?type=contact"));
                                intent.putExtra("synctype", "synctype");
                                startActivity(intent);
                         }
                        /*YUNOS END PB*/
                                mActivity.finish();
                                return;
                            }
                        }).create();
        dialog.show();
    }

    private String makeExportSuccessMessage(int phoneCount, int simCount) {
        Resources res = getResources();
        String messagePartPhoneFormat =
                res.getQuantityString(R.plurals.sim_export_confirm_sync_part_phone, phoneCount);
        String messagePartSimFormat =
                res.getQuantityString(R.plurals.sim_export_confirm_sync_part_sim, simCount);
        StringBuilder result = new StringBuilder();
        result.append(String.format(messagePartPhoneFormat, phoneCount))
                .append(String.format(messagePartSimFormat, simCount));
        return result.toString();
    }

    private String makeExportResultMessage(int phoneCount, int simCount, int checkedCount, boolean partialExported) {
        int remainCount = checkedCount - phoneCount;
        if (partialExported) {
            remainCount++;
        }
        Resources res = getResources();
        String messagePartPhoneFormat =
                res.getQuantityString(R.plurals.sim_export_confirm_sync_part_phone, phoneCount);
        String messagePartSimFormat =
                res.getQuantityString(R.plurals.sim_export_confirm_sync_part_sim, simCount);
        String messagePartRemainFormat =
                res.getQuantityString(R.plurals.sim_export_confirm_sync_part_sync,
                        remainCount);
        StringBuilder result = new StringBuilder();
        result.append(String.format(messagePartPhoneFormat, phoneCount))
                .append(String.format(messagePartSimFormat, simCount))
                .append(String.format(messagePartRemainFormat, remainCount));
        return result.toString();
    }

}
