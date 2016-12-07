
package com.yunos.alicontacts.list;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
import com.yunos.alicontacts.CallDetailTagDialogActivity;
import com.bird.contacts.BirdFeatureOption;
//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ContactEditorActivity;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.platform.PDConstants;
import com.yunos.alicontacts.sim.SimContactEditorFragment;
import com.yunos.alicontacts.sim.SimContactLoadService;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimStateReceiver;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.Constants;
import com.yunos.alicontacts.util.FeatureOptionAssistant;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import yunos.support.v4.content.AsyncTaskLoader;
import yunos.support.v4.content.Loader;
import yunos.support.v4.content.Loader.OnLoadCompleteListener;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AccountFilterManager {
    private static final String TAG = "AccountFilterManager";

    public static final int INVALID_REQUEST_CODE = Integer.MIN_VALUE;

    public static final int SETTINGS_DEFAULT_SHOULD_SYNC = 1;
    public static final int SETTINGS_DEFAULT_VISIBLE = 0;

    private static AccountFilterManager sInstance = null;
    private Context mAppContext;
    private SimStateReceiver.SimStateListener mSimStateListener;

    // In A800, 5000 ms is not enough to wait account data loaded on phone start period.
    // So use a bit longer time for timeout.
    public static final long TIMEOUT_WAIT_LOADED = 8000;
    private static final int ACCOUNT_TYPE_LOCAL = 0x01;
    private static final int ACCOUNT_TYPE_YUNOS = 0x02;
    private static final int ACCOUNT_TYPE_SIM = 0x04;
    private static final int ACCOUNT_TYPE_SIM1 = 0x08;
    private static final int ACCOUNT_TYPE_SIM2 = 0x10;

    private static final String[] SETTINGS_PROJECTION = {
        Settings.ACCOUNT_NAME,
        Settings.ACCOUNT_TYPE
    };

    private static final String[] GROUPS_PROJECTION = {
        Groups.TITLE
    };

    private static final int ACCOUNT_NAME = 0;
    private static final int ACCOUNT_TYPE = 1;

    private static final String SETTINGS_SELECTION =
            Settings.UNGROUPED_VISIBLE + "=1 AND " + Settings.SHOULD_SYNC + "=1 AND ("
                    + Settings.ACCOUNT_TYPE + "='" + SimContactUtils.SIM_ACCOUNT_TYPE + "'"
                    + " OR " + Settings.ACCOUNT_TYPE + "='" + AccountType.YUNOS_ACCOUNT_TYPE + "'"
                    + " OR " + Settings.ACCOUNT_TYPE + "='" + AccountType.LOCAL_ACCOUNT_TYPE + "')";

    private static final String SETTINGS_SELECTION_FOR_ENSURE_SIM_RECORDS =
            Settings.ACCOUNT_TYPE + "='" + SimContactUtils.SIM_ACCOUNT_TYPE + "'";

    private static final String GROUPS_SELECTION =
            Groups.DELETED + "=0 AND " + Groups.GROUP_VISIBLE + "=1";

    private AtomicBoolean mLoaded = new AtomicBoolean(false);
    private WritableAccountList mAccountList;
    private AlertDialog mAccountSelectDialog;

    public interface AccountSetLoaderListener {
        public void onAccountListLoaded(WritableAccountList accountSet);
    }

    public interface AccountSelectListener {
        public void onAccountSelected(WritableAccount account);
        public void onAccountSelectionCanceled();
        public void onLoadTimeout();
    }

    private AccountFilterManager(Context appContext) {
        mAppContext = appContext;
        mSimStateListener = new SimStateReceiver.SimStateListener() {
            @Override
            public void onSimStateChanged(int slot, String state) {
                Log.i(TAG, "onSimStateChanged: slot="+slot);
                loadAccountList(null);
            }
        };
        SimStateReceiver.registSimStateListener(mSimStateListener);
    }

    /**
     * retrieve instance of class.
     *
     * @param context should be getApplicationCotnext() to avoid memory leak.
     */
    public static synchronized AccountFilterManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AccountFilterManager(context.getApplicationContext());
        }

        return sInstance;
    }

    public static class WritableAccount {
        public final String accountName;
        public final String accountType;
        public final String accountDataSet;

        private final int mLabelResId;

        public WritableAccount(String name, String type, String dataSet, int labelResId) {
            accountName = name;
            accountType = type;
            accountDataSet = dataSet;
            this.mLabelResId = labelResId;
        }

        public static WritableAccount createWritableAccount(Context context, String name,
                String type, String dataSet, int labelResId) {
            return new WritableAccount(name, type, dataSet, labelResId);
        }

        public String getTitle(Context context) {
            String label = context.getString(mLabelResId);
            String title = context.getString(R.string.contact_account_format, label);
            return title;
        }

    }

    public static class WritableAccountList {
        public final ArrayList<WritableAccount> mWritableAccounts = Lists.newArrayList();

        public void addYunAccount(Context context, String name, String type, String dataSet, int labelResId) {
            // We always put local account at index 0.
            if (mWritableAccounts.size() > 0) {
                WritableAccount local = mWritableAccounts.get(0);
                if (AccountType.LOCAL_ACCOUNT_NAME.equals(local.accountName)
                        && AccountType.LOCAL_ACCOUNT_TYPE.equals(local.accountType)) {
                    mWritableAccounts.remove(0);
                }
            }
            mWritableAccounts.add(WritableAccount.createWritableAccount(context, name, type, dataSet, labelResId));
        }

        public WritableAccountList clonePhoneAccountList() {
            WritableAccountList dup = new WritableAccountList();
            for (WritableAccount account : mWritableAccounts) {
                if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.accountType)) {
                    continue;
                }
                dup.mWritableAccounts.add(account);
            }
            return dup;
        }

    }

    public void loadAccountList(final AccountSetLoaderListener listener) {
        WritableAccountsLoader loader = new WritableAccountsLoader(mAppContext);
        Log.d(TAG, "getWritableAccountSet() currentThread: " + Thread.currentThread());

        loader.registerListener(0, new OnLoadCompleteListener<WritableAccountList>() {

            @Override
            public void onLoadComplete(Loader<WritableAccountList> paramLoader,
                    WritableAccountList accountList) {
                Log.d(TAG, "WritableAccountsLoader onLoadComplete()");
                mAccountList = accountList;
                synchronized (mLoaded) {
                    mLoaded.set(true);
                    mLoaded.notifyAll();
                }
                if (listener != null) {
                    listener.onAccountListLoaded(accountList);
                }
            }

        });

        loader.startLoading();
    }

    public boolean waitLoaded(long timeout) {
        if (mLoaded.get()) {
            return true;
        }
        synchronized (mLoaded) {
            try {
                mLoaded.wait(timeout);
            } catch (InterruptedException ie) {
                // ignore
                Log.w(TAG, "waitLoaded: interrupted during wait.", ie);
            }
        }
        Log.i(TAG, "waitLoaded: after wait, loaded="+mLoaded.get());
        return mLoaded.get();
    }

    public WritableAccountList getAccountList() {
        return mAccountList;
    }

    private static class WritableAccountsLoader extends AsyncTaskLoader<WritableAccountList> {
        private Context mContext;

        public WritableAccountsLoader(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public WritableAccountList loadInBackground() {
            final ContentResolver resolver = mContext.getContentResolver();

            final WritableAccountList accountSet = new WritableAccountList();

            Cursor groupsCursor = null;
            boolean shouldShowLocal = false;
            boolean hasYunOS = false;
            try {
                groupsCursor = resolver.query(
                        Groups.CONTENT_URI,
                        GROUPS_PROJECTION,
                        GROUPS_SELECTION,
                        null,
                        null); // sort order is not necessary in this case.
                shouldShowLocal = groupsCursor != null ? groupsCursor.getCount() > 0 : false;
            } catch (SQLiteException e) {
                Log.e(TAG, "loadInBackground() query groups Exception:", e);
            } finally {
                if (groupsCursor != null) {
                    groupsCursor.close();
                }
            }

            int bitMask = querySettingsAndEnsureSimRecords(resolver, accountSet);
            if ((bitMask & ACCOUNT_TYPE_YUNOS) != 0) {
                hasYunOS = true;
            }
            if ((bitMask & ACCOUNT_TYPE_LOCAL) != 0) {
                shouldShowLocal = true;
            }

            if (shouldShowLocal && !hasYunOS) {
                accountSet.mWritableAccounts.add(WritableAccount.createWritableAccount(mContext,
                        AccountType.LOCAL_ACCOUNT_NAME, AccountType.LOCAL_ACCOUNT_TYPE, null,
                        R.string.contact_account_local));
            }

            if (!SimUtil.isAirplaneModeOn(mContext)) {
                if (SimUtil.MULTISIM_ENABLE) {
                    if ((bitMask & ACCOUNT_TYPE_SIM1) != 0 && SimUtil.isSimAvailable(SimUtil.SLOT_ID_1)) {
                        accountSet.mWritableAccounts.add(WritableAccount.createWritableAccount(
                                mContext, SimContactUtils.SIM_ACCOUNT_NAME_SIM1,
                                SimContactUtils.SIM_ACCOUNT_TYPE, null,
                                R.string.contact_account_sim1));
                    }

                    if ((bitMask & ACCOUNT_TYPE_SIM2) != 0 && SimUtil.isSimAvailable(SimUtil.SLOT_ID_2)) {
                        accountSet.mWritableAccounts.add(WritableAccount.createWritableAccount(
                                mContext, SimContactUtils.SIM_ACCOUNT_NAME_SIM2,
                                SimContactUtils.SIM_ACCOUNT_TYPE, null,
                                R.string.contact_account_sim2));
                    }

                } else {
                    if ((bitMask & ACCOUNT_TYPE_SIM) != 0 && SimUtil.isSimAvailable()) {
                        accountSet.mWritableAccounts.add(WritableAccount.createWritableAccount(
                                mContext, SimContactUtils.SIM_ACCOUNT_NAME,
                                SimContactUtils.SIM_ACCOUNT_TYPE, null,
                                R.string.contact_account_sim));
                    }
                }
            }

            return accountSet;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        private int querySettingsAndEnsureSimRecords(ContentResolver resolver, final WritableAccountList accountSet) {
            ensureSimRecords(resolver);
            Cursor settingsCursor = null;
            int bitMask = 0x00;
            String name = null, type = null;
            boolean yunOSAccountState = ContactsUtils.getLoginState(mContext);
            String yunOSLoginId = ContactsUtils.getLoginId(mContext);
            try {
                settingsCursor = resolver.query(
                        Settings.CONTENT_URI,
                        SETTINGS_PROJECTION,
                        SETTINGS_SELECTION,
                        null,
                        null);

                if (settingsCursor != null) {
                    Log.d(TAG, "settingsCursor :" + settingsCursor.getCount());
                    while (settingsCursor.moveToNext()) {
                        name = settingsCursor.getString(ACCOUNT_NAME);
                        type = settingsCursor.getString(ACCOUNT_TYPE);
                        if (AccountType.YUNOS_ACCOUNT_TYPE.equals(type) && yunOSAccountState
                                && name != null && name.equals(yunOSLoginId)) {
                            bitMask |= ACCOUNT_TYPE_YUNOS;
                            accountSet.addYunAccount(mContext, name, type, null, R.string.contact_account_local);
                        } else if (SimContactUtils.SIM_ACCOUNT_NAME_SIM1.equals(name)) {
                            bitMask |= ACCOUNT_TYPE_SIM1;
                        } else if (SimContactUtils.SIM_ACCOUNT_NAME_SIM2.equals(name)) {
                            bitMask |= ACCOUNT_TYPE_SIM2;
                        } else if (SimContactUtils.SIM_ACCOUNT_NAME.equals(name)) {
                            bitMask |= ACCOUNT_TYPE_SIM;
                        } else if (AccountType.LOCAL_ACCOUNT_TYPE.equals(type)) {
                            bitMask |= ACCOUNT_TYPE_LOCAL;
                        }
                    }
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "querySettingsAndEnsureSimRecords: query setttings Exception:", e);
            } finally {
                if (settingsCursor != null) {
                    settingsCursor.close();
                }
            }
            return bitMask;
        }

        /**
         * If the config specifies sim contacts to be displayed by default and
         * we don't find sim records in settings, then we shall insert sim records in settings
         * with ungrouped_visible set to 1.
         * Because we will never delete sim records from settings, so once the sim records is inserted,
         * the next time we won't do insert again here.
         * @param resolver
         */
        private void ensureSimRecords(ContentResolver resolver) {
            if (FeatureOptionAssistant.isDefaultShowSimContacts()) {
                try {
                    int simRecordsCount = getSimRecordsCount(resolver);
                    Log.i(TAG, "ensureSimRecords: sim records count="+simRecordsCount);
                    if (simRecordsCount > 0) {
                        return;
                    }
                    ContentValues values = new ContentValues(4);
                    if (SimUtil.MULTISIM_ENABLE) {
                        insertSettingsForSim(resolver, SimContactUtils.SIM_ACCOUNT_NAME_SIM1, values);
                        insertSettingsForSim(resolver, SimContactUtils.SIM_ACCOUNT_NAME_SIM2, values);
                    } else {
                        insertSettingsForSim(resolver, SimContactUtils.SIM_ACCOUNT_NAME, values);
                    }
                } catch (SQLiteException e) {
                    // in case the storage is full, don't block major features.
                    Log.e(TAG, "ensureSimRecords: got exception.", e);
                }
            }
        }

        private int getSimRecordsCount(ContentResolver resolver) {
            Cursor simRecords = null;
            try {
                simRecords = resolver.query(
                        Settings.CONTENT_URI,
                        new String[] { Settings.ACCOUNT_NAME },
                        SETTINGS_SELECTION_FOR_ENSURE_SIM_RECORDS,
                        null,
                        null);
                return simRecords == null ? -1 : simRecords.getCount();
            } finally {
                if (simRecords != null) {
                    simRecords.close();
                }
            }
        }

        private void insertSettingsForSim(ContentResolver resolver, String name, ContentValues values) {
            int retry = 0;
            do {
                try {
                    Log.i(TAG, "insertSettingsForSim: name="+name); // sim name contains no sensitive info.
                    values.clear();
                    values.put(Settings.ACCOUNT_NAME, name);
                    values.put(Settings.ACCOUNT_TYPE, SimContactUtils.SIM_ACCOUNT_TYPE);
                    values.put(Settings.SHOULD_SYNC, SETTINGS_DEFAULT_SHOULD_SYNC);
                    values.put(Settings.UNGROUPED_VISIBLE, 1);
                    resolver.insert(Settings.CONTENT_URI, values);
                    return;
                } catch (IllegalArgumentException iae) {
                    // for bug [ https://k3.alibaba-inc.com/issue/8366578?versionId=1262816 ],
                    // we might failed to resolve the Settings.CONTENT_URI.
                    // We will ignore fail and retry in this case.
                    Log.e(TAG, "insertSettingsForSim: got exception during insert sim settings.", iae);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // ignore.
                }
            } while (retry++ < Constants.RETRY_COUNT_FOR_WAIT_DB_READY);
        }

    }

    public void dismissAccountSelectDialog() {
        if (mAccountSelectDialog != null) {
            mAccountSelectDialog.dismiss();
            mAccountSelectDialog = null;
        }
    }

    public void selectAccount(Activity activity, final AccountSelectListener listener) {
        selectAccountInternal(activity, mAccountList, listener);
    }

    public void selectPhoneAccount(Activity activity, final AccountSelectListener listener) {
        WritableAccountList accountList = mAccountList == null ? null : mAccountList.clonePhoneAccountList();
        selectAccountInternal(activity, accountList, listener);
    }

    private void selectAccountInternal(
            Activity activity, final WritableAccountList accountList, final AccountSelectListener listener) {
        if ((activity == null) || activity.isFinishing() || activity.isDestroyed() || (listener == null)) {
            Log.w(TAG, "selectAccountInternal: activity or listener is null, or activity is not active.");
            return;
        }

        if ((accountList == null) || (accountList.mWritableAccounts.size() == 0)) {
            Log.w(TAG, "selectAccountInternal: null or empty account list. maybe accounts is not loaded.");
            listener.onAccountSelected(null);
            return;
        }

        int size = accountList.mWritableAccounts.size();
        Log.i(TAG, "selectAccountInternal: accounts size="+size);
        WritableAccount account = accountList.mWritableAccounts.get(0);
        if (size == 1) {
            listener.onAccountSelected(account);
            return;
        }

        final SelectAccountDialogAdapter adapter = new SelectAccountDialogAdapter(activity, accountList);
		//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER modified by liuqipeng 20160918 start
		final Activity thisActivity=activity;
		//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
        ListView listView = new ListView(activity);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (adapter != null) {
                    WritableAccount account = adapter.getItem(position);
                    listener.onAccountSelected(account);
                }
                dismissAccountSelectDialog();
				//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
 				if(BirdFeatureOption.BIRD_DOOV_INCALL_MARK_NUMBER){
					if(thisActivity instanceof CallDetailTagDialogActivity)
					thisActivity.finish();
		            }
				}
				//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
        });
        dismissAccountSelectDialog();
        mAccountSelectDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.choose_contact_position).setView(listView).create();
        mAccountSelectDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.onAccountSelectionCanceled();
            }
        });
        mAccountSelectDialog.show();
    }

    public void createNewContactWithPhoneNumberOrEmailAsync(
            final Activity activity, final String phoneNumber, final String email, final int reqCode) {
        final AccountSelectListener listener = new AccountSelectListener() {
            @Override
            public void onAccountSelected(WritableAccount account) {
                if (!checkSimWritable(activity, account)) {
                    return;
                }
                Intent intent = getNewContactIntent(activity, phoneNumber, email, account);
                if (reqCode == INVALID_REQUEST_CODE) {
                    activity.startActivity(intent);
                } else {
                    activity.startActivityForResult(intent, reqCode);
                }
            }

            @Override
            public void onAccountSelectionCanceled() {
                // do nothing;
            }

            @Override
            public void onLoadTimeout() {
                // do nothing;
            }
        };
        waitLoadedAndSelectAccount(activity, listener, false);
    }

    public void waitLoadedAndSelectAccount(
            final Activity activity, final AccountSelectListener listener, final boolean onlyPhoneAccount) {
        if (activity == null) {
            Log.w(TAG, "waitLoadedAndSelectAccount: activity is null. quit.");
            return;
        }
        if (mLoaded.get()) {
            if (onlyPhoneAccount) {
                selectPhoneAccount(activity, listener);
            } else {
                selectAccount(activity, listener);
            }
            return;
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                final boolean loaded = waitLoaded(TIMEOUT_WAIT_LOADED);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loaded) {
                            if (onlyPhoneAccount) {
                                selectPhoneAccount(activity, listener);
                            } else {
                                selectAccount(activity, listener);
                            }
                        } else {
                            Toast.makeText(activity, R.string.toast_contact_storage_busy, Toast.LENGTH_SHORT).show();
                            listener.onLoadTimeout();
                        }
                    }
                });
            }
        };
        thread.start();
    }

    public void createNewPhoneContactWithExtrasAsync(
            final Activity activity, final Bundle extras, final int reqCode) {
        final AccountSelectListener listener = new AccountSelectListener() {
            @Override
            public void onAccountSelected(WritableAccount account) {
                Intent intent = getNewPhoneContactIntent(activity, extras, account);
                if (reqCode == INVALID_REQUEST_CODE) {
                    activity.startActivity(intent);
                } else {
                    activity.startActivityForResult(intent, reqCode);
                }
            }

            @Override
            public void onAccountSelectionCanceled() {
                // do nothing;
            }

            @Override
            public void onLoadTimeout() {
                // do nothing;
            }
        };
        waitLoadedAndSelectAccount(activity, listener, true);
    }

    public void createNewContactInContactEditor(final ContactEditorActivity activity, final Bundle extras) {
        final boolean canAddToSim = SimContactUtils.canAddToSimContact(extras);
        final AccountSelectListener listener = new AccountSelectListener() {
            @Override
            public void onAccountSelected(WritableAccount account) {
                if (!checkSimWritable(activity, account)) {
                    return;
                }
                Intent intent = activity.getIntent();
                if (account == null) {
                    intent.putExtra(PDConstants.INTENTS_INSERT_EXTRA_ACCOUNT,
                            new Account(AccountType.LOCAL_ACCOUNT_NAME, AccountType.LOCAL_ACCOUNT_TYPE));
                } else if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.accountType)) {
                    Intent simIntent;
                    if (canAddToSim) {
                        simIntent = getNewContactIntent(activity, extras.getString(Insert.PHONE), extras.getString(Insert.EMAIL), account);
                    } else {
                        simIntent = getNewPhoneContactIntent(activity, extras, account);
                    }
                    activity.startActivity(simIntent);
                    activity.finish();
                    return;
                } else {
                    intent.putExtra(PDConstants.INTENTS_INSERT_EXTRA_ACCOUNT,
                            new Account(account.accountName, account.accountType));
                }
                activity.doCreate(intent);
                activity.doResume();
            }

            @Override
            public void onAccountSelectionCanceled() {
                activity.finish();
            }

            @Override
            public void onLoadTimeout() {
                activity.finish();
            }
        };
        waitLoadedAndSelectAccount(activity, listener, !canAddToSim);
    }

    private Intent getNewContactIntent(final Activity activity,
            String phoneNumber, String email,
            WritableAccount account) {
        if ((account != null) && SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.accountType)) {
            Intent intent = new Intent(SimContactUtils.ACTION_INSERT_SIM_CONTACTS);

            if (SimContactUtils.SIM_ACCOUNT_NAME_SIM1.equals(account.accountName)
                    || SimContactUtils.SIM_ACCOUNT_NAME.equals(account.accountName)) {
                intent.putExtra(SimUtil.SLOT_KEY, SimUtil.SLOT_ID_1);
            } else {
                intent.putExtra(SimUtil.SLOT_KEY, SimUtil.SLOT_ID_2);
            }

            if (!TextUtils.isEmpty(phoneNumber)) {
                intent.putExtra(SimContactEditorFragment.INTENT_EXTRA_PHONE_NUMBER, phoneNumber);
            }
            if (!TextUtils.isEmpty(email)) {
                intent.putExtra(SimContactEditorFragment.INTENT_EXTRA_EMAIL, email);
            }
            return intent;
        } else {
            Intent origIntent = activity.getIntent();
            Bundle extras = origIntent == null ? null : origIntent.getExtras();
            if (extras == null) {
                extras = new Bundle();
            }
            if (!TextUtils.isEmpty(phoneNumber)) {
                extras.putString(Insert.PHONE, phoneNumber);
            }
            if (!TextUtils.isEmpty(email)) {
                extras.putString(Insert.EMAIL, email);
            }
            return getNewPhoneContactIntent(activity, extras, account);
        }
    }

    private Intent getNewPhoneContactIntent(final Activity activity, Bundle extras, WritableAccount account) {
        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        intent.setClass(activity, ContactEditorActivity.class);
        if (account == null) {
            intent.putExtra(PDConstants.INTENTS_INSERT_EXTRA_ACCOUNT,
                    new Account(AccountType.LOCAL_ACCOUNT_NAME, AccountType.LOCAL_ACCOUNT_TYPE));
        } else {
            intent.putExtra(PDConstants.INTENTS_INSERT_EXTRA_ACCOUNT,
                    new Account(account.accountName, account.accountType));
        }
        if (extras != null) {
            intent.putExtras(extras);
        }

        return intent;
    }

    private boolean checkSimWritable(final Activity activity, final WritableAccount account) {
        boolean writable = true;
        int slot;
        if ((account != null) && SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.accountType)) {
            if (SimContactUtils.SIM_ACCOUNT_NAME_SIM1.equals(account.accountName)
                    || SimContactUtils.SIM_ACCOUNT_NAME.equals(account.accountName)) {
                slot = SimUtil.SLOT_ID_1;
            } else {
                slot = SimUtil.SLOT_ID_2;
            }
            if (SimUtil.isAirplaneModeOn(mAppContext)
                    || (SimUtil.MULTISIM_ENABLE ? (!SimUtil.isSimAvailable(slot)) : (!SimUtil.isSimAvailable()))
                    || (SimContactLoadService.getSimLoadedCount(slot) == SimContactLoadService.NOT_LOADED_COUNT)) {
                writable = false;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, R.string.sim_error_not_ready, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        return writable;
    }

    private static class ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView subTitle;

        public ViewHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.icon);
            name = (TextView) view.findViewById(R.id.name);
            subTitle = (TextView) view.findViewById(R.id.sub_title);
        }
    }

    private class SelectAccountDialogAdapter extends BaseAdapter {
        private final WritableAccountList mAccountSet;
        private LayoutInflater mInflater;

        public SelectAccountDialogAdapter(final Activity activity, WritableAccountList accountSet) {
            mAccountSet = accountSet;
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mAccountSet.mWritableAccounts.size();
        }

        @Override
        public WritableAccount getItem(int position) {
            return mAccountSet.mWritableAccounts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final WritableAccount account = getItem(position);
            String yunOSId = null;
            if (AccountType.YUNOS_ACCOUNT_TYPE.equals(account.accountType)) {
                yunOSId = ContactsUtils.getLoginState(mAppContext) ? ContactsUtils.getLoginName(mAppContext) : null;
            }
            ViewHolder viewHolder = null;
            final boolean hasSubTitle = !TextUtils.isEmpty(yunOSId);

            if (convertView == null) {
                if (hasSubTitle) {
                    convertView = mInflater.inflate(R.layout.create_contact_dialog_item_view, null, false);
                } else {
                    convertView = mInflater.inflate(R.layout.create_contact_dialog_item_view_single_line, null, false);
                }
                viewHolder = new ViewHolder(convertView);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                if ((viewHolder.subTitle == null) && hasSubTitle) {
                    convertView = mInflater.inflate(R.layout.create_contact_dialog_item_view, null, false);
                } else if ((viewHolder.subTitle != null) && (!hasSubTitle)) {
                    convertView = mInflater.inflate(R.layout.create_contact_dialog_item_view_single_line, null, false);
                }
                viewHolder = new ViewHolder(convertView);
            }
            convertView.setTag(viewHolder);

            viewHolder.name.setText(account.getTitle(mAppContext));

            int resId = R.drawable.ic_group_normal;

            if (viewHolder.subTitle != null) {
                viewHolder.subTitle.setText(yunOSId);
            }
            if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.accountType)) {
                if (SimUtil.MULTISIM_ENABLE) {
                    if (SimContactUtils.SIM_ACCOUNT_NAME_SIM2.equals(account.accountName)) {
                        resId = R.drawable.ic_list_card2_normal;
                    } else {
                        resId = R.drawable.ic_list_card1_normal;
                    }
                } else {
                    resId = R.drawable.ic_list_card_normal;
                }
            }
            viewHolder.icon.setBackgroundResource(resId);

            return convertView;
        }

    }
}
