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

package com.yunos.alicontacts.list;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorEntityIterator;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.google.common.collect.Lists;
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.GroupMetaDataLoader;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.group.GroupManager;
import com.yunos.alicontacts.model.RawContactDelta.ValuesDelta;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.model.account.AccountTypeLoaderManager;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimStateReceiver;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.BitmapUtil;
import com.yunos.alicontacts.util.EmptyService;
import com.yunos.alicontacts.util.WeakAsyncTask;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Shows a list of all available {@link Groups} available, letting the user
 * select which ones they want to be visible.
 */
public class CustomContactListFilterActivity extends Activity implements OnClickListener, OnChildClickListener,
        OnGroupClickListener, OnGroupCollapseListener {
    private static final String TAG = "CustomContactListFilterActivity";

    private AccountTypeLoaderManager mAccountLoader;
    private AccountTypeLoaderManager.LoadCallBack mAccountLoadCallback;
    private ExpandableListView mList;
    private DisplayAdapter mAdapter;
    private ImageView mSaveView;
    SimStateReceiver.SimStateListener mSimStateListener;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate:");
        super.onCreate(icicle);
        initSaveCancelActionBar();
        setContentView(R.layout.contact_list_filter_custom);

        mList = (ExpandableListView) findViewById(android.R.id.list);
        mList.setOnGroupClickListener(this);
        mList.setOnChildClickListener(this);
        mList.setOnGroupCollapseListener(this);

        mAdapter = new DisplayAdapter(this);
        mList.setAdapter(mAdapter);
        initAccountLoader();
        initSimStateListener();
    }

    private void initAccountLoader() {
        mAccountLoader = new AccountTypeLoaderManager(getApplicationContext());
        mAccountLoadCallback = new AccountTypeLoaderManager.LoadCallBack() {
            @Override
            public void onLoadFinished(AccountSet data) {
                CustomContactListFilterActivity activity = CustomContactListFilterActivity.this;
                if (activity.isFinishing() || activity.isDestroyed()) {
                    Log.w(TAG, "LoadCallBack.onLoadFinished: activity is not active. quit.");
                    return;
                }
                Log.i(TAG, "LoadCallBack.onLoadFinished: data="+data);
                mAdapter.setAccounts(data);
                invalidListView();
            }
        };
        mAccountLoader.loadAccountSet(this, mAccountLoadCallback);
    }

    private void initSimStateListener() {
        mSimStateListener = new SimStateReceiver.SimStateListener() {
            @Override
            public void onSimStateChanged(int slot, String state) {
                // sometimes, when airplane mode off, the sim is not ready immediately,
                // so we have to wait the loaded state and refresh accounts again.
                // and when we hot eject or plug a sim card, we need to reload.
                if ((SimUtil.DUMMY_SLOT_FOR_AIRPLANE_MODE_ON == slot)
                        || (SimUtil.DUMMY_SLOT_FOR_AIRPLANE_MODE_OFF == slot)
                        || SimContactUtils.INTENT_VALUE_ICC_ABSENT.equals(state)
                        || SimContactUtils.INTENT_VALUE_ICC_NOT_READY.equals(state)
                        || SimContactUtils.INTENT_VALUE_ICC_LOADED.equals(state)) {
                    Log.i(TAG, "onSimStateChanged: reload account data for airplane mode or sim state changed.");
                    mAccountLoader.reloadAccountSet(CustomContactListFilterActivity.this);
                }
            }
        };
        SimStateReceiver.registSimStateListener(mSimStateListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        UsageReporter.onResume(null, UsageReporter.AccountsSupport.ACCOUNT_GROUP_FILTER_PAGE_NAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        UsageReporter.onPause(null, UsageReporter.AccountsSupport.ACCOUNT_GROUP_FILTER_PAGE_NAME);
    }

    /**
     * Entry holding any changes to {@link Groups} or {@link Settings} rows,
     * such as {@link Groups#SHOULD_SYNC} or {@link Groups#GROUP_VISIBLE}.
     */
    protected static class GroupDelta extends ValuesDelta {
        private boolean mUngrouped = false;

        private GroupDelta() {
            super();
        }

        /**
         * Build {@link GroupDelta} from the {@link Settings} row for the given
         * {@link Settings#ACCOUNT_NAME}, {@link Settings#ACCOUNT_TYPE}, and
         * {@link Settings#DATA_SET}.
         */
        public static GroupDelta fromSettings(ContentResolver resolver, String accountName, String accountType,
                String dataSet, boolean accountHasGroups) {
            final Uri.Builder settingsUri = Settings.CONTENT_URI.buildUpon()
                    .appendQueryParameter(Settings.ACCOUNT_NAME, accountName)
                    .appendQueryParameter(Settings.ACCOUNT_TYPE, accountType);
            if (dataSet != null) {
                settingsUri.appendQueryParameter(Settings.DATA_SET, dataSet);
            }
            final Cursor cursor = resolver.query(settingsUri.build(), new String[] {
                    Settings.SHOULD_SYNC, Settings.UNGROUPED_VISIBLE
            }, null, null, null);

            try {
                final ContentValues values = new ContentValues();
                values.put(Settings.ACCOUNT_NAME, accountName);
                values.put(Settings.ACCOUNT_TYPE, accountType);
                values.put(Settings.DATA_SET, dataSet);

                if (cursor != null && cursor.moveToFirst()) {
                    // Read existing values when present
                    values.put(Settings.SHOULD_SYNC, cursor.getInt(0));
                    values.put(Settings.UNGROUPED_VISIBLE, cursor.getInt(1));
                    return fromBefore(values).setUngrouped();
                } else {
                    // Nothing found, so treat as create
                    values.put(Settings.SHOULD_SYNC, AccountFilterManager.SETTINGS_DEFAULT_SHOULD_SYNC);
                    values.put(Settings.UNGROUPED_VISIBLE, AccountFilterManager.SETTINGS_DEFAULT_VISIBLE);
                    return fromAfter(values).setUngrouped();
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        public static GroupDelta fromBefore(ContentValues before) {
            final GroupDelta entry = new GroupDelta();
            entry.mBefore = before;
            entry.mAfter = new ContentValues();
            return entry;
        }

        public static GroupDelta fromAfter(ContentValues after) {
            final GroupDelta entry = new GroupDelta();
            entry.mBefore = null;
            entry.mAfter = after;
            return entry;
        }

        protected GroupDelta setUngrouped() {
            mUngrouped = true;
            return this;
        }

        @Override
        public boolean beforeExists() {
            return mBefore != null;
        }

        public boolean getShouldSync() {
            return getAsInteger(mUngrouped ? Settings.SHOULD_SYNC : Groups.SHOULD_SYNC,
                    AccountFilterManager.SETTINGS_DEFAULT_SHOULD_SYNC) != 0;
        }

        public boolean getVisible() {
            return getAsInteger(mUngrouped ? Settings.UNGROUPED_VISIBLE : Groups.GROUP_VISIBLE,
                    AccountFilterManager.SETTINGS_DEFAULT_VISIBLE) != 0;
        }

        public void putShouldSync(boolean shouldSync) {
            put(mUngrouped ? Settings.SHOULD_SYNC : Groups.SHOULD_SYNC, shouldSync ? 1 : 0);
        }

        public void putVisible(boolean visible) {
            put(mUngrouped ? Settings.UNGROUPED_VISIBLE : Groups.GROUP_VISIBLE, visible ? 1 : 0);
        }

        public CharSequence getTitle(Context context) {
            if (mUngrouped) {
                return context.getText(R.string.contact_ungroup_contacts);
            } else {
                String title = getAsString(Groups.TITLE);

                if (!TextUtils.isEmpty(title)) {
                    title += context.getString(R.string.contact_account_group_count,
                            getAsInteger(Groups.SUMMARY_COUNT));
                }

                return title;
            }
        }

        /**
         * Build a possible {@link ContentProviderOperation} to persist any
         * changes to the {@link Groups} or {@link Settings} row described by
         * this {@link GroupDelta}.
         */
        public ContentProviderOperation buildDiff() {
            if (isInsert()) {
                // Only allow inserts for Settings
                if (mUngrouped) {
                    mAfter.remove(mIdColumn);
                    if (mAfter.containsKey(Settings.UNGROUPED_VISIBLE)
                            && mAfter.getAsInteger(Settings.UNGROUPED_VISIBLE) == 1) {
                        Log.i(TAG, "buildDiff: in insert branch, got ungrouped_visible 1");
                    }
                    return ContentProviderOperation.newInsert(Settings.CONTENT_URI).withValues(mAfter).build();
                } else {
                    throw new IllegalStateException("Unexpected diff");
                }
            } else if (isUpdate()) {
                if (mUngrouped) {
                    String accountName = this.getAsString(Settings.ACCOUNT_NAME);
                    String accountType = this.getAsString(Settings.ACCOUNT_TYPE);
                    String dataSet = this.getAsString(Settings.DATA_SET);
                    StringBuilder selection = new StringBuilder(Settings.ACCOUNT_NAME + "=? AND "
                            + Settings.ACCOUNT_TYPE + "=?");
                    String[] selectionArgs;
                    if (dataSet == null) {
                        selection.append(" AND " + Settings.DATA_SET + " IS NULL");
                        selectionArgs = new String[] {
                                accountName, accountType
                        };
                    } else {
                        selection.append(" AND " + Settings.DATA_SET + "=?");
                        selectionArgs = new String[] {
                                accountName, accountType, dataSet
                        };
                    }
                    Log.i(TAG, "buildDiff: in update branch, got ungrouped_visible 1");
                    return ContentProviderOperation.newUpdate(Settings.CONTENT_URI)
                            .withSelection(selection.toString(), selectionArgs).withValues(mAfter).build();
                } else {
                    Log.i(TAG, "buildDiff: in update branch, for normal group");
                    return ContentProviderOperation.newUpdate(addCallerIsSyncAdapterParameter(Groups.CONTENT_URI))
                            .withSelection(Groups._ID + "=" + this.getId(), null).withValues(mAfter).build();
                }
            } else {
                Log.i(TAG, "biuldDiff: no diff.");
                return null;
            }
        }

        /**
         * Apply modifications in another GroupDelta object.
         * This is used when the original data changed,
         * and we make some modifications on original data.
         * In this case, we will re-query the latest data,
         * and apply the modifications made on old data.
         * @param that old data that might contain changes.
         * @return The impact to AccountDisplay.mGroupCheckedCount.
         * If a group is modified from unchecked to checked, return 1.
         * If a group is modified from checked to unchecked, return -1.
         * If the checked status is unchanged, return 0.
         */
        public int applyModifications(GroupDelta that) {
            boolean thisVisible = getVisible();
            boolean thatVisible = that.getVisible();
            if (thisVisible == thatVisible) {
                return 0;
            }
            putVisible(thatVisible);
            if (thisVisible) {
                return -1;
            }
            return 1;
        }

    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    /**
     * {@link Comparator} to sort by {@link Groups#_ID}.
     */
    private static Comparator<GroupDelta> sIdComparator = new Comparator<GroupDelta>() {
        @Override
        public int compare(GroupDelta object1, GroupDelta object2) {
            final Long id1 = object1.getId();
            final Long id2 = object2.getId();
            if (id1 == null && id2 == null) {
                return 0;
            } else if (id1 == null) {
                return -1;
            } else if (id2 == null) {
                return 1;
            } else if (id1 < id2) {
                return -1;
            } else if (id1 > id2) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    /**
     * Set of all {@link AccountDisplay} entries, one for each source.
     */
    public static class AccountSet extends ArrayList<AccountDisplay> {
        private static final long serialVersionUID = 1L;

        private final static String[] GROUP_PROJECTION = new String[] {
                Groups._ID, Groups.TITLE, Groups.ACCOUNT_NAME, Groups.ACCOUNT_TYPE, Groups.DATA_SET,
                Groups.SUMMARY_COUNT, Groups.GROUP_VISIBLE, Groups.SHOULD_SYNC, Groups.NOTES
        };

        private final static String GROUP_SELECTION = Groups.DELETED + "=0";

        public ArrayList<ContentProviderOperation> buildDiff() {
            final ArrayList<ContentProviderOperation> diff = Lists.newArrayList();
            for (AccountDisplay account : this) {
                account.buildDiff(diff);
            }
            return diff;
        }

        public boolean add(Context context, AccountDisplay accountDisplay) {
            buildAccountGroupData(context.getContentResolver(), accountDisplay);
            return super.add(accountDisplay);
        }

        private void buildAccountGroupData(final ContentResolver resolver, final AccountDisplay accountDisplay) {
            if (resolver == null || accountDisplay == null) {
                Log.e(TAG, "buildAccountGroupData() maybe account is null!!!");
                return;
            }

            final Uri.Builder groupsUri = Groups.CONTENT_SUMMARY_URI.buildUpon();
            groupsUri.appendQueryParameter(Groups.ACCOUNT_NAME, accountDisplay.mName).appendQueryParameter(
                    Groups.ACCOUNT_TYPE, accountDisplay.mType);
            if (accountDisplay.mDataSet != null) {
                groupsUri.appendQueryParameter(Groups.DATA_SET, accountDisplay.mDataSet).build();
            }

            final EntityIterator iterator = new GroupIteratorImpl(resolver.query(groupsUri.build(), GROUP_PROJECTION,
                    GROUP_SELECTION, null, GroupMetaDataLoader.GROUPS_SORT_ORDER));

            try {
                // Create entries for each known group
                int groupCount = 0;
                while (iterator.hasNext()) {
                    final ContentValues values = iterator.next().getEntityValues();
                    final GroupDelta group = GroupDelta.fromBefore(values);
                    accountDisplay.addGroup(group);

                    if (group.getVisible()) {
                        accountDisplay.mGroupCheckedCount++;
                    }

                    groupCount++;
                }

                boolean hasGroup = groupCount > 0;
                accountDisplay.mUngrouped = GroupDelta.fromSettings(resolver, accountDisplay.mName,
                        accountDisplay.mType, accountDisplay.mDataSet, hasGroup);
                if (hasGroup) {
                    accountDisplay.addGroup(accountDisplay.mUngrouped);
                    if (accountDisplay.mUngrouped.getVisible()) {
                        accountDisplay.mGroupCheckedCount++;
                    }

                    groupCount++;
                }

                accountDisplay.mGroupTotalCount = groupCount;

                Log.d(TAG, "buildAccountGroupData() total:" + groupCount + " accountDisplay.mGroupCheckedCount:"
                        + accountDisplay.mGroupCheckedCount + ", accountDisplay.mUngrouped.getVisible():"
                        + accountDisplay.mUngrouped.getVisible());

                // initial allChecked value.
                if (groupCount > 0) {
                    accountDisplay.mAllChecked = (accountDisplay.mGroupCheckedCount == groupCount);
                } else {
                    accountDisplay.mAllChecked = accountDisplay.mUngrouped.getVisible();
                }

            } catch (Exception e) {
                Log.e(TAG, "buildAccountGroupData() Exception:", e);
            } finally {
                iterator.close();
            }
        }

        private class GroupIteratorImpl extends CursorEntityIterator {
            public GroupIteratorImpl(Cursor cursor) {
                super(cursor);
            }

            @Override
            public Entity getEntityAndIncrementCursor(Cursor cursor) throws RemoteException {
                final ContentValues values = new ContentValues(8);
                DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, values, Groups._ID);
                DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Groups.TITLE);
                DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Groups.ACCOUNT_NAME);
                DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Groups.ACCOUNT_TYPE);
                DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Groups.DATA_SET);
                DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, Groups.SUMMARY_COUNT);
                DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, values, Groups.GROUP_VISIBLE);
                DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Groups.SHOULD_SYNC);
                DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, values, Groups.NOTES);
                cursor.moveToNext();
                return new Entity(values);
            }
        }

        private void saveReport(Context context) {
            int count = size();
            HashMap<String, String> localAccountReport = new HashMap<String, String>();
            HashMap<String, String> simAccountsReport = new HashMap<String, String>();
            HashMap<String, String> otherAccountsReport = new HashMap<String, String>();
            for (int i = 0; i < count; i++) {
                AccountDisplay account = get(i);
                if (AccountType.LOCAL_ACCOUNT_TYPE.equals(account.mType)
                        || AccountType.YUNOS_ACCOUNT_TYPE.equals(account.mType)) {
                    fillLocalAccountReport(account, localAccountReport);
                } else if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.mType)) {
                    fillSimAccountReport(account, simAccountsReport);
                } else {
                    fillOtherAccountReport(account, otherAccountsReport);
                }
            }
            UsageReporter.serializeForPeriodReport(context,
                    UsageReporter.AccountsSupport.EVENT_ID_LOCAL_ACCOUNT_CHECKED, localAccountReport);
            UsageReporter.serializeForPeriodReport(context,
                    UsageReporter.AccountsSupport.EVENT_ID_SIM_ACCOUNT_CHECKED, simAccountsReport);
            UsageReporter.serializeForPeriodReport(context,
                    UsageReporter.AccountsSupport.EVENT_ID_OTHER_ACCOUNTS_CHECKED, otherAccountsReport);
        }

        private static final String GROUP_CONTACT_COUNT_KEY = "summ_count";
        private static final String GROUP_VISIBLE_KEY = "group_visible";
        private static final String GROUP_CHECKED_VALUE = "on";
        private static final String GROUP_UNCHECKED_VALUE = "off";

        private void fillLocalAccountReport(AccountDisplay account, HashMap<String, String> report) {
            report.put("all", account.mAllChecked ? GROUP_CHECKED_VALUE : GROUP_UNCHECKED_VALUE);
            fillGroupsForLocalAccount(account.mUnsyncedGroups, report);
            fillGroupsForLocalAccount(account.mSyncedGroups, report);
        }

        // only report the check status for default groups and all checked.
        private static void fillGroupsForLocalAccount(ArrayList<GroupDelta> groups, HashMap<String, String> report) {
            int count = groups == null ? -1 : groups.size();
            for (int i = 0; i < count; i++) {
                GroupDelta group = groups.get(i);
                String groupNotes = group.getAsString(Groups.NOTES);
                if (!GroupManager.DEFAULT_GROUP_FLAG.equals(groupNotes)) {
                    continue;
                }
                String groupName = group.getAsString(Groups.TITLE);
                report.put(groupName, getGroupChecked(group));
                report.put(groupName+"_num", group.getAsString(GROUP_CONTACT_COUNT_KEY));
            }
        }

        private static String getGroupChecked(GroupDelta group) {
            return "0".equals(group.getAsString(GROUP_VISIBLE_KEY))
                    ? GROUP_UNCHECKED_VALUE : GROUP_CHECKED_VALUE;
        }

        private void fillSimAccountReport(AccountDisplay account, HashMap<String, String> report) {
            report.put(account.mName, account.mAllChecked ? GROUP_CHECKED_VALUE : GROUP_UNCHECKED_VALUE);
        }

        private void fillOtherAccountReport(AccountDisplay account, HashMap<String, String> report) {
            boolean checked = account.mAllChecked;
            if (!checked) {
                if ((account.mUngrouped != null)
                        && (!"0".equals(account.mUngrouped.getAsString(GROUP_VISIBLE_KEY)))) {
                    checked = true;
                }
                if (!checked) {
                    checked = isAnyGroupChecked(account.mSyncedGroups);
                }
                if (!checked) {
                    checked = isAnyGroupChecked(account.mUnsyncedGroups);
                }
            }
            report.put(account.mType, checked ? GROUP_CHECKED_VALUE : GROUP_UNCHECKED_VALUE);
        }

        private static boolean isAnyGroupChecked(ArrayList<GroupDelta> groups) {
            int count = groups == null ? -1 : groups.size();
            for (int i = 0; i < count; i++) {
                GroupDelta group = groups.get(i);
                if (!"0".equals(group.getAsString(GROUP_VISIBLE_KEY))) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * {@link GroupDelta} details for a single {@link AccountWithDataSet},
     * usually shown as children under a single expandable group.
     */
    public static class AccountDisplay {
        public final String mName;
        public final String mType;
        public final String mDataSet;

        public final CharSequence mTitle;
        public final Drawable mIcon;
        public boolean mAllChecked;

        public int mGroupTotalCount;
        public int mGroupCheckedCount;

        public GroupDelta mUngrouped;
        public ArrayList<GroupDelta> mSyncedGroups = Lists.newArrayList();
        public ArrayList<GroupDelta> mUnsyncedGroups = Lists.newArrayList();

        /**
         * Build an {@link AccountDisplay} covering all {@link Groups} under the
         * given {@link AccountWithDataSet}.
         */
        public AccountDisplay(Context context, String accountName, String accountType, String dataSet,
                int labelResId, int iconResId) {
            mName = accountName;
            mType = accountType;
            mDataSet = dataSet;
            final String label = context.getString(labelResId);
            mTitle = context.getString(R.string.contact_account_format, label);
            mIcon = context.getResources().getDrawable(iconResId, null);
        }

        public AccountDisplay(Context context, String accountName, String accountType, String dataSet,
                String title, Drawable icon) {
            mName = accountName;
            mType = accountType;
            mDataSet = dataSet;
            mTitle = title;
            mIcon = icon;
        }

        /**
         * Check if this AccountDisplay object can merge changes from that AccountDisplay object.
         * Only the one AccountDisplay object can only merge changes from another AccountDisplay
         * object that represents the same account.
         * Local phone account and YunOS account are treated as the same account.
         * @param that
         * @return
         */
        public boolean canMergeModifications(AccountDisplay that) {
            if (that == null) {
                return false;
            }
            // 1. compare local and sim accounts.
            if ((AccountType.LOCAL_ACCOUNT_TYPE.equals(that.mType)
                        || AccountType.YUNOS_ACCOUNT_TYPE.equals(that.mType))
                    && (AccountType.LOCAL_ACCOUNT_TYPE.equals(mType)
                        || AccountType.YUNOS_ACCOUNT_TYPE.equals(mType))) {
                return true;
            }
            if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(that.mType)
                    && SimContactUtils.SIM_ACCOUNT_TYPE.equals(mType)) {
                return TextUtils.equals(that.mName, mName);
            }
            // 2. compare other accounts.
            return TextUtils.equals(that.mType, mType)
                    && TextUtils.equals(that.mName, mName)
                    && TextUtils.equals(that.mDataSet, mDataSet);
        }

        /**
         * Merge changes that we have made in that AccountDisplay object to this one.
         * @param that From which we will read changes, can NOT be null.
         */
        public void applyModifications(AccountDisplay that) {
            if (mUngrouped != null) {
                int checkedCountChange = mUngrouped.applyModifications(that.mUngrouped);
                if (mGroupTotalCount > 0) {
                    mGroupCheckedCount += checkedCountChange;
                } else {
                    mAllChecked = mUngrouped.getVisible();
                }
            }
            applyModificationsForGroups(mSyncedGroups, that.mSyncedGroups);
            applyModificationsForGroups(mUnsyncedGroups, that.mUnsyncedGroups);
            if (mGroupTotalCount > 0) {
                mAllChecked = mGroupCheckedCount == mGroupTotalCount;
            }
        }

        /**
         * Merge the changes for each group in the lists.
         * @param groups
         * @param thatGroups
         */
        private void applyModificationsForGroups(ArrayList<GroupDelta> groups, ArrayList<GroupDelta> thatGroups) {
            if ((groups == null) || (thatGroups == null)) {
                return;
            }
            int groupsCount = groups.size();
            int thatGroupsCount = groups.size();
            int groupsIdx = 0, thatGroupsIdx = 0;
            long groupId, thatGroupId;
            GroupDelta group, thatGroup;
            while ((groupsIdx < groupsCount) && (thatGroupsIdx < thatGroupsCount)) {
                group = groups.get(groupsIdx);
                if (group == mUngrouped) {
                    groupsIdx++;
                    continue;
                }
                thatGroup = thatGroups.get(thatGroupsIdx);
                groupId = group.getId();
                thatGroupId = thatGroup.getId();
                if (groupId < thatGroupId) {
                    groupsIdx++;
                    continue;
                } else if (groupId > thatGroupId) {
                    thatGroupsIdx++;
                    continue;
                }
                mGroupCheckedCount += group.applyModifications(thatGroup);
                groupsIdx++;
                thatGroupsIdx++;
            }
        }

        /**
         * Add the given {@link GroupDelta} internally, filing based on its
         * {@link GroupDelta#getShouldSync()} status.
         */
        private void addGroup(GroupDelta group) {
            if (group.getShouldSync()) {
                mSyncedGroups.add(group);
            } else {
                mUnsyncedGroups.add(group);
            }
        }

        /**
         * Set the {@link GroupDelta#putShouldSync(boolean)} value for all
         * children {@link GroupDelta} rows.
         */
        public void setShouldSync(boolean shouldSync) {
            final Iterator<GroupDelta> oppositeChildren = shouldSync ? mUnsyncedGroups.iterator() : mSyncedGroups
                    .iterator();
            while (oppositeChildren.hasNext()) {
                final GroupDelta child = oppositeChildren.next();
                setShouldSync(child, shouldSync, false);
                oppositeChildren.remove();
            }
        }

        public void setShouldSync(GroupDelta child, boolean shouldSync) {
            setShouldSync(child, shouldSync, true);
        }

        /**
         * Set {@link GroupDelta#putShouldSync(boolean)}, and file internally
         * based on updated state.
         */
        public void setShouldSync(GroupDelta child, boolean shouldSync, boolean attemptRemove) {
            child.putShouldSync(shouldSync);
            if (shouldSync) {
                if (attemptRemove) {
                    mUnsyncedGroups.remove(child);
                }
                mSyncedGroups.add(child);
                Collections.sort(mSyncedGroups, sIdComparator);
            } else {
                if (attemptRemove) {
                    mSyncedGroups.remove(child);
                }
                mUnsyncedGroups.add(child);
            }
        }

        /**
         * Build set of {@link ContentProviderOperation} to persist any user
         * changes to {@link GroupDelta} rows under this
         * {@link AccountWithDataSet}.
         */
        public void buildDiff(ArrayList<ContentProviderOperation> diff) {
            for (GroupDelta group : mSyncedGroups) {
                final ContentProviderOperation oper = group.buildDiff();
                if (oper != null) {
                    diff.add(oper);
                    // NOTE: compare using "==" instead of equals,
                    // because the ungrouped in mSyncedGroups is the same instance as mUngrouped.
                    if (group == mUngrouped) {
                        makeUpForYunOSUngroupedOperation(diff);
                    }
                }
            }

            for (GroupDelta group : mUnsyncedGroups) {
                final ContentProviderOperation oper = group.buildDiff();
                if (oper != null) {
                    diff.add(oper);
                    // NOTE: compare using "==" instead of equals,
                    // because the ungrouped in mUnsyncedGroups is the same instance as mUngrouped.
                    if (group == mUngrouped) {
                        makeUpForYunOSUngroupedOperation(diff);
                    }
                }
            }

            if (!mSyncedGroups.contains(mUngrouped) && !mUnsyncedGroups.contains(mUngrouped)) {
                final ContentProviderOperation oper = mUngrouped.buildDiff();
                if (oper != null) {
                    diff.add(oper);
                    makeUpForYunOSUngroupedOperation(diff);
                }
            }
        }

        /**
         * In the UI "Local Contacts" maps to 2 account types in settings table of contacts db:
         * "Local Phone Account" and "com.aliyun.tyid".
         * So when we update ungrouped_visible for one type, then we need to update the same column for the other.
         * This method checks if the operation from mUngrouped change, and decides if we need to
         * make up another operation to update the other record in settings table.
         * @param ops The operations list.
         */
        private void makeUpForYunOSUngroupedOperation(
                ArrayList<ContentProviderOperation> ops) {
            // 1. check if the account is local phone account or yunos account.
            // 2. check if the account sets the value of ungrouped_visible.
            // If all above match, then we need to create a new operation
            // and set ungrouped_visible to the same value for the other account.
            if (mUngrouped == null) {
                Log.w(TAG, "makeUpForYunOSUngroupedOperation: mUngrouped is null, ignore.");
                return;
            }
            String accountType = mUngrouped.getAsString(Settings.ACCOUNT_TYPE);
            if ((!AccountType.LOCAL_ACCOUNT_TYPE.equals(accountType))
                    && (!AccountType.YUNOS_ACCOUNT_TYPE.equals(accountType))) {
                Log.w(TAG, "makeUpForYunOSUngroupedOperation: not local or yunos type, ignore.");
                return;
            }
            Integer ungroupedVisible = mUngrouped.getAsInteger(Settings.UNGROUPED_VISIBLE);
            if (ungroupedVisible == null) {
                Log.w(TAG, "makeUpForYunOSUngroupedOperation: not ungrouped, ignore.");
                return;
            }
            String selection = Settings.ACCOUNT_TYPE + "=?";
            String[] selectionArgs = new String[1];
            if (AccountType.LOCAL_ACCOUNT_TYPE.equals(accountType)) {
                selectionArgs[0] = AccountType.YUNOS_ACCOUNT_TYPE;
            } else {
                selectionArgs[0] = AccountType.LOCAL_ACCOUNT_TYPE;
            }
            ContentValues values = new ContentValues(1);
            values.put(Settings.UNGROUPED_VISIBLE, ungroupedVisible);
            // Use update operation, in case we do NOT have the other account in settings table,
            // then we will do nothing and the data is not changed.
            // This is what we want, so do NOT use insert here.
            ContentProviderOperation op = ContentProviderOperation.newUpdate(Settings.CONTENT_URI)
                    .withSelection(selection, selectionArgs)
                    .withValues(values)
                    .build();
            ops.add(op);
            Log.i(TAG, "makeUpForYunOSUngroupedOperation: set "+Settings.UNGROUPED_VISIBLE
                    +" to "+ungroupedVisible+" for "+selectionArgs[0]);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AccountDisplay:{type=\"").append(mType)
              .append("\"; name=\"").append(mName)
              .append("\"; title=\"").append(mTitle)
              .append("\"}");
            return sb.toString();
        }
    }

    /**
     * {@link ExpandableListAdapter} that shows {@link GroupDelta} settings,
     * grouped by {@link AccountWithDataSet} type. Shows footer row when any
     * groups are unsynced, as determined through
     * {@link AccountDisplay#mUnsyncedGroups}.
     */
    protected static class DisplayAdapter extends BaseExpandableListAdapter {
        private Context mContext;
        private LayoutInflater mInflater;
        private AccountSet mAccounts;

        public DisplayAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setAccounts(AccountSet accounts) {
            AccountSet oldAccounts = mAccounts;
            mAccounts = accounts;
            applyModifications(oldAccounts);
            notifyDataSetChanged();
        }

        private void applyModifications(AccountSet oldAccounts) {
            if ((oldAccounts == null) || oldAccounts.isEmpty()
                    || (mAccounts == null) || mAccounts.isEmpty()) {
                return;
            }
            int newSize = mAccounts.size();
            int oldSize = oldAccounts.size();
            for (int i = 0; i < newSize; i++) {
                AccountDisplay newAccount = mAccounts.get(i);
                AccountDisplay oldAccount = null;
                // normally, we won't have many accounts, so use for-loop inside should be OK.
                for (int j = 0; j < oldSize; j++) {
                    AccountDisplay tmpOld = oldAccounts.get(j);
                    if (newAccount.canMergeModifications(tmpOld)) {
                        oldAccount = tmpOld;
                        break;
                    }
                }
                if (oldAccount == null) {
                    continue;
                }
                newAccount.applyModifications(oldAccount);
            }
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            final AccountDisplay account = (AccountDisplay) getGroup(groupPosition);
            String yunOSId = null;
            if (AccountType.YUNOS_ACCOUNT_TYPE.equals(account.mType)) {
                yunOSId = ContactsUtils.getLoginState(mContext) ? ContactsUtils.getLoginName(mContext) : null;
            }
            final boolean hasSubTitle = !TextUtils.isEmpty(yunOSId);
            final View view = makeGroupItemView(convertView, parent, hasSubTitle);
            final AccountViewHolder groupCacheView = (AccountViewHolder) view.getTag();

            fillGroupItemView(groupCacheView, account, yunOSId);
            setGroupItemSeparator(groupPosition, groupCacheView, account);

            return view;
        }

        private View makeGroupItemView(View convertView, ViewGroup parent, boolean hasSubTitle) {
            final View view;
            final AccountViewHolder groupCacheView;
            if (convertView == null) {
                if (hasSubTitle) {
                    view = mInflater.inflate(R.layout.custom_contact_list_filter_account, parent, false);
                } else {
                    view = mInflater.inflate(R.layout.custom_contact_list_filter_account_single_line, parent, false);
                }
                groupCacheView = new AccountViewHolder(view, true);
                view.setTag(groupCacheView);
            } else {
                AccountViewHolder oldCacheView = (AccountViewHolder) convertView.getTag();
                if ((oldCacheView.mSubTitle == null) && hasSubTitle) {
                    view = mInflater.inflate(R.layout.custom_contact_list_filter_account, parent, false);
                    groupCacheView = new AccountViewHolder(view, true);
                    view.setTag(groupCacheView);
                } else if ((oldCacheView.mSubTitle != null) && (!hasSubTitle)) {
                    view = mInflater.inflate(R.layout.custom_contact_list_filter_account_single_line, parent, false);
                    groupCacheView = new AccountViewHolder(view, true);
                    view.setTag(groupCacheView);
                } else {
                    view = convertView;
                    groupCacheView = (AccountViewHolder) view.getTag();
                }
            }
            return view;
        }

        private void fillGroupItemView(AccountViewHolder groupCacheView, AccountDisplay account, String yunOSId) {
            Drawable icon = account.mIcon;
            if (icon != null) {
                Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                bitmap = BitmapUtil.circleMaskBitmap(bitmap);
                icon = new BitmapDrawable(mContext.getResources(), bitmap);
            }

            groupCacheView.mIcon.setImageDrawable(icon);
            groupCacheView.mTitle.setText(account.mTitle);
            if (AccountType.YUNOS_ACCOUNT_TYPE.equals(account.mType)) {
                String label = mContext.getString(R.string.contact_account_local);
                groupCacheView.mTitle.setText(mContext.getString(R.string.contact_account_format, label));
                if (groupCacheView.mSubTitle != null) {
                    groupCacheView.mSubTitle.setText(yunOSId);
                }
            }
            groupCacheView.mCheckBox.setChecked(account.mAllChecked);
        }

        private void setGroupItemSeparator(int groupPosition, AccountViewHolder groupCacheView, AccountDisplay account) {
            // Because the view can be reused, so set separator to visible first
            // in case it is set to gone in previous life cycle.
            // Then set it to gone according to its position in this life cycle.
            groupCacheView.mTopSeparator.setVisibility(View.VISIBLE);
            // The first group (phone contacts) and the second sim group does NOT need top separator.
            if (groupPosition == 0) {
                groupCacheView.mTopSeparator.setVisibility(View.GONE);
            } else if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.mType)) {
                // for a sim account, the position must be no less than 1.
                AccountDisplay prevAccount = (AccountDisplay) getGroup(groupPosition-1);
                if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(prevAccount.mType)) {
                    groupCacheView.mTopSeparator.setVisibility(View.GONE);
                }
            }

            groupCacheView.mBottomSeparator.setVisibility(View.VISIBLE);
            // If a group has no children and it is not the last group, then it does NOT need bottom separator.
            if ((getChildrenCount(groupPosition) == 0) && ((groupPosition+1) < getGroupCount())) {
                groupCacheView.mBottomSeparator.setVisibility(View.GONE);
            }
            // If a group is sim account and the next group is also sim account,
            // then it DOES need bottom separator.
            if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.mType) && ((groupPosition+1) < getGroupCount())) {
                AccountDisplay nextAccount = (AccountDisplay) getGroup(groupPosition+1);
                if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(nextAccount.mType)) {
                    groupCacheView.mBottomSeparator.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                ViewGroup parent) {
            final View view;
            final AccountViewHolder childCacheView;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.custom_contact_list_filter_group, parent, false);
                childCacheView = new AccountViewHolder(view, false);
                view.setTag(childCacheView);
            } else {
                view = convertView;
                childCacheView = (AccountViewHolder) view.getTag();
            }

            final GroupDelta child = (GroupDelta) getChild(groupPosition, childPosition);
            if (child != null) {
                // Handle normal group, with title and checkbox
                final CharSequence groupTitle = child.getTitle(mContext);
                childCacheView.mTitle.setText(groupTitle);
                childCacheView.mCheckBox.setChecked(child.getVisible());
            }

            return view;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            final AccountDisplay account = mAccounts.get(groupPosition);
            final boolean validChild = childPosition >= 0 && childPosition < account.mSyncedGroups.size();
            if (validChild) {
                return account.mSyncedGroups.get(childPosition);
            } else {
                return null;
            }
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            final GroupDelta child = (GroupDelta) getChild(groupPosition, childPosition);
            if (child != null) {
                final Long childId = child.getId();
                return childId != null ? childId : Long.MIN_VALUE;
            } else {
                return Long.MIN_VALUE;
            }
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            // Count is any synced groups, plus possible footer
            final AccountDisplay account = mAccounts.get(groupPosition);
            final boolean anyHidden = !account.mUnsyncedGroups.isEmpty();
            return account.mSyncedGroups.size() + (anyHidden ? 1 : 0);
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mAccounts.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            if (mAccounts == null) {
                return 0;
            }
            return mAccounts.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    private AlertDialog mPopupDialog;
    private AlertDialog mNoCheckPopupDialog;
    /** {@inheritDoc} */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save:
                if (anyGroupChecked()) {
                    doSaveAction();
                } else {
                    if (mNoCheckPopupDialog == null) {
                        AlertDialog.Builder build = new AlertDialog.Builder(this);
                        build.setMessage(getString(R.string.no_group_checked));
                        build.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        mNoCheckPopupDialog = build.create();
                    }
                    mNoCheckPopupDialog.show();
                }
                break;
            case R.id.cancel:
                finish();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SimStateReceiver.unregistSimStateListener(mSimStateListener);
        if ((mPopupDialog != null) && mPopupDialog.isShowing()) {
            mPopupDialog.dismiss();
        }
        mPopupDialog = null;
        if ((mNoCheckPopupDialog != null) && mNoCheckPopupDialog.isShowing()) {
            mNoCheckPopupDialog.dismiss();
        }
        mNoCheckPopupDialog = null;
    }

    private boolean anyGroupChecked() {
        int groupCount = mAdapter.getGroupCount();
        if (mAdapter == null || groupCount == 0) {
            return false;
        } else {
            for (int i = 0; i < groupCount; i++) {
                AccountDisplay account = (AccountDisplay) mAdapter.getGroup(i);
                if (account.mAllChecked) {
                    return true;
                }
                int childCount = mAdapter.getChildrenCount(i);
                for (int j = 0; j < childCount; j++) {
                    GroupDelta child = (GroupDelta) mAdapter.getChild(i, j);
                    if (child.getVisible()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Handle any clicks on {@link ExpandableListAdapter} children, which
     * usually mean toggling its visible state.
     */
    @Override
    public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        final AccountViewHolder childCacheView = (AccountViewHolder) view.getTag();
        final GroupDelta child = (GroupDelta) mAdapter.getChild(groupPosition, childPosition);

        if (child != null) {
            final AccountDisplay accountDisplay = (AccountDisplay) mAdapter.getGroup(groupPosition);
            childCacheView.mCheckBox.toggle();

            final boolean isChecked = childCacheView.mCheckBox.isChecked();
            child.putVisible(isChecked);

            if (isChecked) {
                accountDisplay.mGroupCheckedCount++;
            } else {
                accountDisplay.mGroupCheckedCount--;
            }

            Log.d(TAG, "onChildClick() isChecked:" + isChecked + ", accountDisplay.mGroupCheckedCount:"
                    + accountDisplay.mGroupCheckedCount + ", total:" + accountDisplay.mGroupTotalCount);

            if (accountDisplay.mGroupTotalCount > 0) {
                accountDisplay.mAllChecked = (accountDisplay.mGroupCheckedCount == accountDisplay.mGroupTotalCount);
            }

            mAdapter.notifyDataSetChanged();
            return false;
        }

        return true;
    }

    private void doSaveAction() {
        if (mAdapter == null || mAdapter.mAccounts == null) {
            finish();
            return;
        }

        setResult(RESULT_OK);

        final ArrayList<ContentProviderOperation> diff = mAdapter.mAccounts.buildDiff();
        if (diff.isEmpty()) {
            finish();
            return;
        }

        setActionBarDoneEnable(false);

        new UpdateTask(this, diff, mAdapter.mAccounts).execute();
    }

    /**
     * Background task that persists changes to {@link Groups#GROUP_VISIBLE},
     * showing spinner dialog to user while updating.
     */
    public static class UpdateTask extends WeakAsyncTask<Void, Void, Void, Activity> {
        // 500 is sqlite limit, I don't want to test if it uses > or >=, so I use 499 for my limit.
        private static final int BATCH_OP_LIMIT = 499;
        private ProgressDialog mProgress;
        private ArrayList<ContentProviderOperation> mDiff;
        private AccountSet mAccounts;

        public UpdateTask(Activity target, ArrayList<ContentProviderOperation> diff, AccountSet accounts) {
            super(target);
            mDiff = diff;
            mAccounts = accounts;
        }

        /** {@inheritDoc} */
        @Override
        protected void onPreExecute(Activity target) {
            final Context context = target;

            mProgress = ProgressDialog.show(context, null, context.getText(R.string.savingDisplayGroups));

            // Before starting this task, start an empty service to protect our
            // process from being reclaimed by the system.
            context.startService(new Intent(context, EmptyService.class));
        }

        /** {@inheritDoc} */
        @Override
        protected Void doInBackground(Activity target, Void... params) {
            final Context context = target;
            // final ContentValues values = new ContentValues();
            final ContentResolver resolver = context.getContentResolver();

            try {
                ArrayList<ArrayList<ContentProviderOperation>> diffArray = splitDiff();
                for (ArrayList<ContentProviderOperation> diff : diffArray) {
                    resolver.applyBatch(ContactsContract.AUTHORITY, diff);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Problem saving display groups", e);
            } catch (OperationApplicationException e) {
                Log.e(TAG, "Problem saving display groups", e);
            }
            AccountFilterManager.getInstance(context).loadAccountList(null);
            return null;
        }

        private ArrayList<ArrayList<ContentProviderOperation>> splitDiff() {
            ArrayList<ArrayList<ContentProviderOperation>> result = new ArrayList<ArrayList<ContentProviderOperation>>();
            final int totalSize = mDiff.size();
            int offset = 0, count;
            while (offset < totalSize) {
                count = totalSize - offset;
                if (count > BATCH_OP_LIMIT) {
                    count = BATCH_OP_LIMIT;
                }
                ArrayList<ContentProviderOperation> subList = new ArrayList<ContentProviderOperation>(count);
                for (int i = 0; i < count; i++) {
                    subList.add(mDiff.get(offset + i));
                }
                result.add(subList);
                offset += count;
            }
            return result;
        }

        /** {@inheritDoc} */
        @Override
        protected void onPostExecute(Activity target, Void result) {
            try {
                mProgress.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing progress dialog", e);
            }
            target.finish();

            mAccounts.saveReport(target);
            // Stop the service that was protecting us
            target.stopService(new Intent(target, EmptyService.class));
        }

    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        if (mList != null) {
            mList.expandGroup(groupPosition);
        }
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        final AccountViewHolder groupCacheView = (AccountViewHolder) v.getTag();

        if (groupCacheView != null) {
            final AccountDisplay accountDisplay = (AccountDisplay) mAdapter.getGroup(groupPosition);

            groupCacheView.mCheckBox.toggle();

            final boolean isChecked = groupCacheView.mCheckBox.isChecked();
            accountDisplay.mAllChecked = isChecked;
            accountDisplay.mGroupCheckedCount = isChecked ? accountDisplay.mGroupTotalCount : 0;

            Log.d(TAG, "onGroupClick() groupPosition:" + groupPosition + ", isChecked:" + isChecked + ", checkedCount:"
                    + accountDisplay.mGroupCheckedCount);

            setAccountAllChecked(accountDisplay, isChecked);
        }

        return false;
    }

    private static class AccountViewHolder {
        public final View mTopSeparator;
        public final View mBottomSeparator;
        public final ImageView mIcon;
        public final TextView mTitle;
        public final TextView mSubTitle;
        public final CheckBox mCheckBox;

        public AccountViewHolder(View view, boolean isGroup) {
            mTopSeparator = view.findViewById(R.id.full_separator);
            mBottomSeparator = view.findViewById(R.id.part_separator);
            mIcon = (ImageView) view.findViewById(R.id.icon);
            if (mIcon != null) {
                if (isGroup) {
                    mIcon.setVisibility(View.VISIBLE);
                } else {
                    mIcon.setVisibility(View.GONE);
                }
            }
            mTitle = (TextView) view.findViewById(R.id.title);
            mSubTitle = (TextView) view.findViewById(R.id.sub_title);
            mCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
        }
    }

    private void initSaveCancelActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(R.layout.contacts_actionbar_cancel_done);
            actionBar.setDisplayShowCustomEnabled(true);
            ImageView cancelView = (ImageView) findViewById(R.id.cancel);
            mSaveView = (ImageView) findViewById(R.id.save);
            cancelView.setOnClickListener(this);
            mSaveView.setOnClickListener(this);

            TextView titleView = (TextView) findViewById(R.id.title);
            titleView.setText(R.string.activity_title_contacts_filter);
        }

        setSystembarColor(getResources().getColor(R.color.title_color), actionBar != null);
    }

    private void setSystembarColor(int color, boolean showActionBar) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, showActionBar);
        systemBarManager.setStatusBarColor(color);
        systemBarManager.setStatusBarDarkMode(this, getResources().getBoolean(R.bool.contact_dark_mode));
    }

    private void setActionBarDoneEnable(boolean enabled) {
        if (mSaveView != null) {
            mSaveView.setEnabled(enabled);
        }
    }

    private void setAccountAllChecked(final AccountDisplay accountDisplay, boolean checked) {
        final ArrayList<GroupDelta> synced = accountDisplay.mSyncedGroups;
        for (GroupDelta delta : synced) {
            delta.putVisible(checked);
        }
        final ArrayList<GroupDelta> unsynced = accountDisplay.mUnsyncedGroups;
        for (GroupDelta delta : unsynced) {
            delta.putVisible(checked);
        }

        accountDisplay.mUngrouped.putVisible(checked);
    }

    private void invalidListView() {
        if (mList != null && mAdapter != null) {
            int count = mAdapter.getGroupCount();
            for (int i = 0; i < count; i++) {
                mList.expandGroup(i);
            }
        }
    }
}
