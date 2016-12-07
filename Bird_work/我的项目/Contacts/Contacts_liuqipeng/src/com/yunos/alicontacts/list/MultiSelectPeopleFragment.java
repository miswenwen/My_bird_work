/*
 *
 * Copyright (C) 2010 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.Serializable;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.activities.BaseActivity.OnAllCheckedListener;
import com.yunos.alicontacts.activities.BaseFragmentActivity;
import com.yunos.alicontacts.activities.ContactSelectionActivity;
import com.yunos.alicontacts.list.ContactListAdapter.ViewHolder;
import com.yunos.alicontacts.preference.ContactsSettingActivity;
import com.yunos.alicontacts.sim.SimContactCache;
import com.yunos.alicontacts.sim.SimContactLoadService;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.common.DebugLog;
import com.yunos.common.UsageReporter;
import com.yunos.yundroid.widget.item.HeaderIconTextCBItem;
import com.yunos.yundroid.widget.itemview.HeaderIconTextCBItemView;
import com.yunos.yundroid.widget.itemview.ItemView;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.ProgressDialog;
import yunos.support.v4.content.Loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import com.yunos.alicontacts.vcard.ExportVCardActivity;

public class MultiSelectPeopleFragment extends ContactEntryListFragment<ContactEntryListAdapter> {

    private static final String TAG = "MultiSelectPeopleFragment";

    private static final String ACTION = "action";
    private Context mAppContext;
    private int mCount;
    private int mCheckedCount;

    /**
     * This is a map from contact id to raw contact id.
     * When we want to check and delete sim contacts, we need raw contact id to search it in cache.
     * But when we want to delete contacts in phone db, contact id is needed.
     */
    @SuppressLint("UseSparseArrays")
    private final HashMap<Long, Long> mSelectedContactIds = new HashMap<Long, Long>();

    private static final String KEY_CHECKED_LIST = "checkedList";

    private int mAction;
    private int mPosition;

    private ProgressDialog mWorkProgress;
    private DeleteContactsThread mDeleteThread;
    private DeleteGroupMembersThread mDelGroupMemThread;
    private AddGroupMembersThread mAddGroupMemThread;
    private String[] mGroupMemIds;
    private long mGroupID;
    private volatile boolean mCancelWorkFlag;
    private static final int DELETE_COUNT_EACH_TIME = 20;
    private static volatile boolean sIsDelete = false;

    private TextView mBtn;
    private View mFooter;

    private ContactListFilter mCurrentFilter;

    public static interface ContactBatchDeleteStatusListener {
        public void onDeleteStatusChanged(boolean isDeleting);
    }
    public static ArrayList<ContactBatchDeleteStatusListener> sContactBatchDeleteStatusListeners
            = new ArrayList<ContactBatchDeleteStatusListener>();

    public static boolean addContactBatchDeleteStatusListener(
            ContactBatchDeleteStatusListener listener) {
        synchronized (sContactBatchDeleteStatusListeners) {
            if (!sContactBatchDeleteStatusListeners.contains(listener)) {
                sContactBatchDeleteStatusListeners.add(listener);
                return true;
            }
            return false;
        }
    }

    public static boolean removeContactBatchDeleteStatusListener(
            ContactBatchDeleteStatusListener listener) {
        synchronized (sContactBatchDeleteStatusListeners) {
            return sContactBatchDeleteStatusListeners.remove(listener);
        }
    }

    public static boolean isDeletingContacts() {
        return sIsDelete;
    }

    public static void notifyContactBatchDeleteStatusChange(boolean isDeleting) {
        Log.i(TAG, "notifyContactBatchDeleteStatusChange: isDeleting="+isDeleting);
        sIsDelete = isDeleting;
        ContactBatchDeleteStatusListener[] listeners;
        synchronized (sContactBatchDeleteStatusListeners) {
            listeners = sContactBatchDeleteStatusListeners.toArray(
                    new ContactBatchDeleteStatusListener[sContactBatchDeleteStatusListeners.size()]);
        }
        for (ContactBatchDeleteStatusListener l : listeners) {
            l.onDeleteStatusChanged(isDeleting);
        }
    }

    public void setupGroups(String[] ids, long groupID) {
        mGroupMemIds = ids;
        mGroupID = groupID;
    }

    public void setFilter(ContactListFilter filter) {
        mCurrentFilter = filter;
    }

    public MultiSelectPeopleFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_CONTACT_SHORTCUT);
    }

    public static MultiSelectPeopleFragment newInstance(int action, int position) {
        MultiSelectPeopleFragment f = new MultiSelectPeopleFragment();
        Bundle args = new Bundle();
        args.putInt(ACTION, action);
        args.putInt(ContactSelectionActivity.EXTRA_LIST_POSITION, position);
        f.setArguments(args);
        return f;
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        MultiSelectContactListAdapter adapter = new MultiSelectContactListAdapter(getActivity());
        adapter.setSectionHeaderDisplayEnabled(true);
        // adapter.setDisplayPhotos(true);
        adapter.setFilter(mCurrentFilter);
        adapter.setDisplayPhotos(ContactsSettingActivity.readShowContactsHeadIconPreference(getActivity()));
        // adapter.setQuickContactEnabled(false);
        /*
         * if (mAction ==
         * ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_DELETE) { } else
         */if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_RM_FROM_GROUP) {
            adapter.setGroupMembersIds(mGroupMemIds[DefaultContactBrowseListFragment.MEM_CONTACT_ID], true);
        } else if ((mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_ADD_TO_GROUP)) {
            adapter.updateAddGroupMemFilterIDs(mGroupMemIds[DefaultContactBrowseListFragment.MEM_CONTACT_ID], true);
        } /*
           * else if ((mAction ==
           * ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_STARRED)) { }
           */
            /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
            Log.d(TAG,"mAction="+mAction);
            if(mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_DELETE){
                adapter.setReadOnlyContactsVisibility(false);
            }
            /// @}
        return adapter;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((BaseActivity) getActivity()).showAllCheckBox(new OnAllCheckedListener() {

            @Override
            public void onAllChecked(boolean checked) {
                selectedAll(checked);
                if (checked) {
                    UsageReporter.onClick(getActivity(), null,
                            UsageReporter.ContactsListPage.MULTI_SELECT_PEOPLE_SELECT_ALL);
                }
            }
        });

        // ((BaseFragmentActivity) getActivity()).showBackKey(true);
        updateFooterBar();
        mAppContext = getActivity().getApplicationContext();
    }

    private void updateFooterBar() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.delete_footer_item, null);
        mFooter = v;
        mBtn = (TextView) v.findViewById(R.id.footer_delete_btn);
        mBtn.setEnabled(false);
        mBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_DELETE) {
                    doDelAction();
                } else if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_ADD_TO_GROUP) {
                    setupProgressDlg(getString(R.string.add_group_members));
                    mAddGroupMemThread = new AddGroupMembersThread();
                    mAddGroupMemThread.start();
                } else if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_RM_FROM_GROUP) {
                    setupProgressDlg(getString(R.string.rm_group_members));
                    mDelGroupMemThread = new DeleteGroupMembersThread();
                    mDelGroupMemThread.start();
                    /*YunOS BEGIN PB*/
                    //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
                    //##BugID:(8466294) ##date:2016-7-22 09:00
                    //##description:suppot export some contacts to vcard
                }else if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_EXPORT) {
                    Intent intent = new Intent();
                    intent.setClass(getActivity(),ExportVCardActivity.class);
                    String contactIds = "(";
                    int count = mSelectedContactIds.size();
                    Long[] ids = mSelectedContactIds.keySet().toArray(new Long[count]);
                    int i = 0;
                    for (; i < count - 1; i++) {
                        Long contactId = ids[i];
                        Long rawContactId = mSelectedContactIds.get(contactId);
                        if (rawContactId == null) {
                            Log.i(TAG, "deleteSimOrPhoneContacts: raw contact id can not be found for contact id "+contactId);
                            continue;
                        }
                        contactIds += contactId.toString();
                        contactIds += ",";
                    }
                    if(i == count -1  && mSelectedContactIds.get(ids[i]) != null){
                        contactIds += ids[i];
                    }
                    contactIds += ")";
                    intent.putExtra("id",contactIds );
                    Log.d(TAG,"pf contactIds:"+contactIds);
                    startActivity(intent);
                }
                /*YUNOS END PB*/
            }
        });
        if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_DELETE) {
            mBtn.setText(getString(R.string.remove));
        } else if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_RM_FROM_GROUP) {
            mBtn.setText(getString(R.string.done));
            mBtn.setCompoundDrawablesWithIntrinsicBounds(null,getResources().getDrawable(R.drawable.delete_selector),null,null);
        } else if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_ADD_TO_GROUP) {
            mBtn.setText(getString(R.string.done));
            mBtn.setCompoundDrawablesWithIntrinsicBounds(null,getResources().getDrawable(R.drawable.add_selector),null,null);
            /*YunOS BEGIN PB*/
            //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
            //##BugID:(8466294) ##date:2016-7-22 09:00
            //##description:suppot export some contacts to vcard
        } else if (mAction == ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_EXPORT) {
            mBtn.setText(getString(R.string.export_vcard));
            mBtn.setCompoundDrawablesWithIntrinsicBounds(null,getResources().getDrawable(R.drawable.bt_export_selector),null,null);
        }
        /*YUNOS END PB*/
        ((BaseFragmentActivity) getActivity()).addFooterView(v);
    }

    private void removeMembersFromGroup(ContentResolver resolver, Long[] contactIdsToRemove,
            long groupId) {
        if (contactIdsToRemove == null) {
            return;
        }
        for (long contactId : contactIdsToRemove) {
            if (mCancelWorkFlag) {
                return;
            }
            // Apply the delete operation on the data row for the given raw
            // contact's
            // membership in the given group. If no contact matches the provided
            // selection, then
            // nothing will be done. Just continue to the next contact.
            resolver.delete(
                    Data.CONTENT_URI,
                    Data.CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND "
                            + GroupMembership.GROUP_ROW_ID + "=?",
                    new String[] {
                            String.valueOf(contactId), GroupMembership.CONTENT_ITEM_TYPE,
                            String.valueOf(groupId)
                    });

            if (mWorkProgress != null) {
                mWorkProgress.incrementProgressBy(1);
            }

        }
    }

    private void addMembersToGroup(ContentResolver resolver, Long[] contactIdsToAdd, long groupId) {
        if (contactIdsToAdd == null) {
            return;
        }
        for (long contactId : contactIdsToAdd) {
            if (mCancelWorkFlag) {
                return;
            }
            Long rawContactId = mSelectedContactIds.get(contactId);
            try {
                final ArrayList<ContentProviderOperation> rawContactOperations = new ArrayList<ContentProviderOperation>();

                // Build an assert operation to ensure the contact is not
                // already in the group
                final ContentProviderOperation.Builder assertBuilder = ContentProviderOperation
                        .newAssertQuery(Data.CONTENT_URI);
                assertBuilder.withSelection(Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE
                        + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
                        new String[] {
                                String.valueOf(rawContactId), GroupMembership.CONTENT_ITEM_TYPE,
                                String.valueOf(groupId)
                        });
                assertBuilder.withExpectedCount(0);
                rawContactOperations.add(assertBuilder.build());

                // Build an insert operation to add the contact to the group
                final ContentProviderOperation.Builder insertBuilder = ContentProviderOperation
                        .newInsert(Data.CONTENT_URI);
                insertBuilder.withValue(Data.RAW_CONTACT_ID, rawContactId);
                insertBuilder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
                insertBuilder.withValue(GroupMembership.GROUP_ROW_ID, groupId);
                rawContactOperations.add(insertBuilder.build());

                // Apply batch
                if (!rawContactOperations.isEmpty()) {
                    resolver.applyBatch(ContactsContract.AUTHORITY, rawContactOperations);
                }
            } catch (RemoteException e) {
                // Something went wrong, bail without success
                Log.e(TAG, "Problem persisting user edits for raw contact ID " + rawContactId, e);
            } catch (OperationApplicationException e) {
                // The assert could have failed because the contact is already
                // in the group,
                // just continue to the next contact
                Log.w(TAG, "Assert failed in adding raw contact ID " + rawContactId
                        + ". Already exists in group " + groupId, e);
            }
            if (mWorkProgress != null) {
                mWorkProgress.incrementProgressBy(1);
            }
        }
    }

    private void setupProgressDlg(String title) {
        mWorkProgress = new ProgressDialog(getActivity());
        mWorkProgress.setMessage(title);
        mWorkProgress.setMax(mCheckedCount);
        mWorkProgress.setProgress(0);
        mWorkProgress.setCancelable(false);
        mWorkProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mCancelWorkFlag = true;
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        mWorkProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        mWorkProgress.show();
    }

    private void doDelAction() {
        if (mCheckedCount == 0) {
            return;
        }

        String message = "";
        final int delSize = mSelectedContactIds.size();
        if (delSize > 1) {
            if (delSize == mCount) {
                message = getResources().getString(R.string.deletesAllConfirmation);
            } else {
                message = getResources().getString(R.string.deletesConfirmation, delSize);
            }
        } else if (delSize == 1) {
            message = getResources().getString(R.string.deleteConfirmation);
        }

        AlertDialog.Builder build = new AlertDialog.Builder(getActivity());
        build.setMessage(message);
        build.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (sIsDelete) {
                    Log.w(TAG, "doDelAction.onClick: delete in in progress. skip.");
                    Toast.makeText(getActivity(), R.string.deleting_progress_and_wait_text, Toast.LENGTH_SHORT).show();
                    return;
                }
                mShowDeleteIndicator = false;
                // don't show progressbar if delete less contacts.
                if (delSize >= DELETE_COUNT_EACH_TIME) {
                    setupProgressDlg(getString(R.string.delete_message));
                }

                ContactBrowseListFragment.CONTACTS_LIST_LONG_OPERATION.set(true);
                notifyContactBatchDeleteStatusChange(true);
                mDeleteThread = new DeleteContactsThread();
                mDeleteThread.start();

                if (delSize < DELETE_COUNT_EACH_TIME && getActivity() != null) {
                    getActivity().finish();
                }
                UsageReporter.onClick(getActivity(), null,
                        UsageReporter.ContactsListPage.MULTI_SELECT_PEOPLE_DELETE);
            }
        });
        build.setNegativeButton(R.string.no,null);
        AlertDialog dialog = build.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    private class DeleteContactsThread extends Thread {
        /**
         * Time in milliseconds between two wait operations. This is used to
         * avoid pause delete thread too frequently.
         */
        private static final long WAIT_INTERVAL_IN_MILLIS = 1000;
        /**
         * MAX-time in milliseconds for a single wait. This is used to pause
         * delete thread, and let the contact list get enough time to refresh.
         */
        private static final long MAX_WAIT_TIME_IN_MILLIS = 1000;
        private long mLastSleepTime = 0;

        @Override
        public void run() {
            try {
                deleteContactsInternal();
            } finally {
                ContactBrowseListFragment.CONTACTS_LIST_LONG_OPERATION.set(false);
                notifyContactBatchDeleteStatusChange(false);
            }
        }

        public void deleteContactsInternal() {
            if (mAppContext == null) {
                Log.e(TAG, "deleteContactsInternal: mAppContext is NULL!!!");
                return;
            }

            final ContentResolver contentResolver = mAppContext.getContentResolver();
            if (contentResolver == null) {
                Log.e(TAG, "deleteContactsInternal: contentResolver is NULL!!!");
                return;
            }

            boolean isThrowException = false;
            int reloadSimContactsFlags = checkNeedReloadSimContacts();
            try {
                deleteSimOrPhoneContacts(contentResolver);
            } catch (Exception e) {
                isThrowException = true;
                Log.e(TAG, "deleteContactsInternal: delete contacts Exception", e);
            } finally {
                notifyContactEntryListChanged();
                if (mCheckedCount >= DELETE_COUNT_EACH_TIME && mWorkProgress != null) {
                    mWorkProgress.dismiss();
                }

                /**
                 * we ignore SIM contacts deleting exception, generally
                 * speaking, if isThrowException is true, we think system is in
                 * extreme bad condition, such as system data area is full.
                 */
                Activity activity = getActivity();
                if (isThrowException && (activity != null)
                        && (!activity.isFinishing()) && (!activity.isDestroyed())) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mAppContext, R.string.toast_delete_contacts_fail, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                reloadSimContact(reloadSimContactsFlags);
            }
        }

        /**
         * Check if sim contacts have been loaded to cache.
         * If the sim contacts are not loaded to cache, and we select to show sim contacts,
         * then we will reload sim contacts after multiple select and delete.
         * Because the sim contacts that are not loaded, might not be deleted completely from sim card.
         * @return If the sim contacts need to be reload for sim1 or sim2.
         * If sim1 need to be reloaded, then bit 0 is set to 1.
         * If sim2 need to be reloaded, then bit 1 is set to 1.
         * For single sim mode, only bit 0 is used.
         */
        private int checkNeedReloadSimContacts() {
            int simContactsLoaded = 0x00;
            final int simContactsFullyLoaded;
            if (SimUtil.MULTISIM_ENABLE) {
                simContactsFullyLoaded = 0x03;
                int loadedCount = SimContactLoadService.getSimLoadedCount(SimUtil.SLOT_ID_1);
                if ((loadedCount != SimContactLoadService.NOT_LOADED_COUNT)
                        && (loadedCount != SimContactLoadService.SCHEDULE_LOAD_COUNT)) {
                    simContactsLoaded |= 0x01;
                }
                loadedCount = SimContactLoadService.getSimLoadedCount(SimUtil.SLOT_ID_2);
                if ((loadedCount != SimContactLoadService.NOT_LOADED_COUNT)
                        && (loadedCount != SimContactLoadService.SCHEDULE_LOAD_COUNT)) {
                    simContactsLoaded |= 0x02;
                }
            } else {
                simContactsFullyLoaded = 0x01;
                int loadedCount = SimContactLoadService.getSimLoadedCount(SimUtil.SLOT_ID_1);
                if ((loadedCount != SimContactLoadService.NOT_LOADED_COUNT)
                        && (loadedCount != SimContactLoadService.SCHEDULE_LOAD_COUNT)) {
                    simContactsLoaded = 0x01;
                }
            }
            if (simContactsLoaded == simContactsFullyLoaded) {
                Log.i(TAG, "checkNeedReloadSimContacts: sim contacts fully loaded to cache.");
                return 0x00;
            }

            AccountFilterManager afm = AccountFilterManager.getInstance(mAppContext);
            int simContactsNeedReload = 0x00;
            if (afm.waitLoaded(AccountFilterManager.TIMEOUT_WAIT_LOADED)) {
                AccountFilterManager.WritableAccountList accountList = afm.getAccountList();
                int accountCount = accountList == null ? -1 : accountList.mWritableAccounts.size();
                for (int i = 0; i < accountCount; i++) {
                    AccountFilterManager.WritableAccount account = accountList.mWritableAccounts.get(i);
                    if (SimContactUtils.SIM_ACCOUNT_TYPE.equals(account.accountType)) {
                        if (SimUtil.MULTISIM_ENABLE) {
                            if (SimContactUtils.SIM_ACCOUNT_NAME_SIM1.equals(account.accountName)
                                    && ((simContactsLoaded & 0x01) == 0)) {
                                simContactsNeedReload |= 0x01;
                            } else if (SimContactUtils.SIM_ACCOUNT_NAME_SIM2.equals(account.accountName)
                                    && ((simContactsLoaded & 0x02) == 0)) {
                                simContactsNeedReload |= 0x02;
                            }
                        } else {
                            simContactsNeedReload = 0x01;
                            break;
                        }
                    }
                }
            } else {
                // we can NOT ensure the sim account selected, in case it is selected,
                // the delete action might not work completely,
                // so we need to notify the user this information.
                simContactsNeedReload = SimUtil.MULTISIM_ENABLE ? 0x03 : 0x01;
            }

            if (simContactsNeedReload != 0) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mAppContext, R.string.sim_contacts_not_loaded_for_del_contacts, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            Log.i(TAG, "checkNeedReloadSimContacts: return "+simContactsNeedReload);
            return simContactsNeedReload;
        }

        /**
         * Reload sim contacts according to calculated flags.
         * @param reloadSimContactsFlags In dual sim mode, bit 0 for reload sim1, bit 1 for reload sim2.
         * In single sim mode, bit 0 for reload.
         */
        private void reloadSimContact(int reloadSimContactsFlags) {
            if (SimUtil.isAirplaneModeOn(mAppContext)) {
                Log.w(TAG, "reloadSimContact: AirPlaneMode on, ignore.");
                return;
            }
            Log.i(TAG, "reloadSimContact: reloadSimContactsFlags="+reloadSimContactsFlags);
            if (SimUtil.MULTISIM_ENABLE) {
                if (((reloadSimContactsFlags & 0x01) != 0)
                        && SimUtil.isSimAvailable(SimUtil.SLOT_ID_1)) {
                    Log.i(TAG, "reloadSimContact: sim not loaded before delete. reload sim contacts on sub1.");
                    SimContactLoadService.notifyReloadSimContacts(mAppContext, SimUtil.SLOT_ID_1);
                }
                if (((reloadSimContactsFlags & 0x02) != 0)
                        && SimUtil.isSimAvailable(SimUtil.SLOT_ID_2)) {
                    Log.i(TAG, "reloadSimContact: sim not loaded before delete. reload sim contacts on sub2.");
                    SimContactLoadService.notifyReloadSimContacts(mAppContext, SimUtil.SLOT_ID_2);
                }
            } else {
                if (((reloadSimContactsFlags & 0x01) != 0) && SimUtil.isSimAvailable()) {
                    Log.i(TAG, "reloadSimContact: sim not loaded before delete. reload sim contacts.");
                    SimContactLoadService.notifyReloadSimContacts(mAppContext, SimUtil.SLOT_ID_1);
                }
            }
        }

        private void deleteSimOrPhoneContacts(ContentResolver resolver) {
            int batchCount = 0;
            ArrayList<String> ids = new ArrayList<String>();
            StringBuilder selection = new StringBuilder();
            int count = mSelectedContactIds.size();
            Long[] contactIds = mSelectedContactIds.keySet().toArray(new Long[count]);
            for (int i = 0; i < count; i++) {
                if (mCancelWorkFlag) {
                    notifyContactEntryListChanged();
                    return;
                }
                Long contactId = contactIds[i];
                Long rawContactId = mSelectedContactIds.get(contactId);
                if (rawContactId == null) {
                    Log.i(TAG, "deleteSimOrPhoneContacts: raw contact id can not be found for contact id "+contactId);
                    continue;
                }
                Log.i(TAG, "deleteSimOrPhoneContacts: delete contact id "+contactId+"; raw contact id "+rawContactId);
                SimContactCache.SimContact simContact
                        = SimContactCache.getSimContactByRawContactIdWithoutSimId(rawContactId);
                int result = 1;
                if (simContact != null) {
                    result = deleteSimContactFromSimCard(simContact);
                    Log.i(TAG, "deleteSimOrPhoneContacts: found in sim contact cache. delete on sim. result="+result);
                }
                if ((contactId > 0) && (result > 0)) {
                    Log.i(TAG, "deleteSimOrPhoneContacts: prepare to delete "+contactId+" from phone db.");
                    mSelectedContactIds.remove(contactId);
                    ids.add(String.valueOf(contactId));
                    if (selection.length() == 0) {
                        selection.append(RawContacts.CONTACT_ID + " IN (");
                    } else {
                        selection.append(',');
                    }
                    selection.append('?');
                    batchCount++;
                }

                if ((batchCount >= DELETE_COUNT_EACH_TIME) || ((i == (count - 1)) && (batchCount > 0))) {
                    String[] batchIds = ids.toArray(new String[ids.size()]);
                    int delCount = resolver.delete(RawContacts.CONTENT_URI,
                            selection.append(')').toString(),
                            batchIds);
                    Log.d(TAG, "deleteSimOrPhoneContacts: delete in phone db. delCount="+delCount);

                    if (mWorkProgress != null) {
                        mWorkProgress.incrementProgressBy(batchCount);
                    }

                    batchCount = 0;
                    ids.clear();
                    selection.setLength(0);
                    checkWait();
                }
            }
        }

        private int deleteSimContactFromSimCard(SimContactCache.SimContact simContact) {
            ContentValues values = simContact.toContentValues();
            int result;
            if (SimUtil.MULTISIM_ENABLE) {
                result = SimUtil.delete(mAppContext, simContact.slotId, values);
            } else {
                result = SimUtil.delete(mAppContext, values);
            }
            if (result > 0) {
                SimContactCache.deleteSimContact(simContact);
            }
            return result;
        }

        private void checkWait() {
            Activity activity = getActivity();
            if ((activity == null) || activity.isFinishing() || activity.isDestroyed()) {
                long now = System.currentTimeMillis();
                if ((now - mLastSleepTime) > WAIT_INTERVAL_IN_MILLIS) {
                    try {
                        synchronized (ContactBrowseListFragment.CONTACTS_LIST_LONG_OPERATION) {
                            ContactBrowseListFragment.CONTACTS_LIST_LONG_OPERATION.wait(MAX_WAIT_TIME_IN_MILLIS);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "checkWait() throw Exception", e);
                    }
                    // we have wait for some time, so do NOT use the value from
                    // now.
                    mLastSleepTime = System.currentTimeMillis();
                }
            }
        }
    }

    // YUNOS BEGIN
    // #description:BugID:123797:When contacts deleted at lock screen state, the
    // contacts list cannot be refreshed. It needs to be notified again when
    // contacts deleted completed.
    // #author:fangjun.lin
    // #date: 2014-05-28
    private void notifyContactEntryListChanged() {
        if (mAppContext != null) {
            boolean changed = ContactsSettingActivity.readBooleanFromDefaultSharedPreference(mAppContext,
                    ContactEntryListFragment.KEY_PREFS_CONTACTS_CHANGED, false);
            Log.i(TAG, "[DELETE] notifyContactEntryListChanged changed = " + changed);
            ContactsSettingActivity.writeBooleanToDefaultSharedPreference(mAppContext,
                    ContactEntryListFragment.KEY_PREFS_CONTACTS_CHANGED, !changed);
        }
    }

    // YUNOS END

    private class DeleteGroupMembersThread extends Thread implements
            DialogInterface.OnCancelListener {
        @Override
        public void run() {
            Long[] contactsIDs = mSelectedContactIds.keySet().toArray(new Long[mSelectedContactIds.size()]);
            removeMembersFromGroup(getActivity().getContentResolver(), contactsIDs, mGroupID);

            mWorkProgress.dismiss();
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

    private class AddGroupMembersThread extends Thread implements DialogInterface.OnCancelListener {
        @Override
        public void run() {
            Long[] contactIds = mSelectedContactIds.keySet().toArray(new Long[mSelectedContactIds.size()]);
            addMembersToGroup(getActivity().getContentResolver(), contactIds, mGroupID);

            mWorkProgress.dismiss();
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

    private class MultiSelectContactListAdapter extends DefaultContactListAdapter {

        public MultiSelectContactListAdapter(Context context) {
            super(context);
        }

        @Override
        protected View newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
            HeaderIconTextCBItem item = new HeaderIconTextCBItem();
            HeaderIconTextCBItemView itemView = (HeaderIconTextCBItemView) item.newView(context, parent);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.view = itemView;
            itemView.setTag(viewHolder);
            return itemView;
        }

        @Override
        protected void bindCheckBox(ItemView view, int pos) {
            super.bindCheckBox(view, pos);
            if (getAdapter() != null) {
                int position = pos;
                if (isSearchMode()) {
                    // if search mode position 0 is search result item
                    position++;
                }
                long contactId = getContactId(position);
                if (mSelectedContactIds.containsKey(contactId)) {
                    view.setCheckBox(true);
                } else {
                    view.setCheckBox(false);
                }
            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onItemClick(parent, view, position, id);
        ContactEntryListAdapter adapter = getAdapter();

        if (adapter == null || !(adapter instanceof MultiSelectContactListAdapter)) {
            Log.e(TAG, "onItemClick() adapter is null or not MultiSelectContactListAdapter instance!!!");
            return;
        }

        long contactId = ((MultiSelectContactListAdapter) adapter).getContactId(position);
        boolean checked = false;
        if (mSelectedContactIds.containsKey(contactId)) {
            mSelectedContactIds.remove(contactId);
            mCheckedCount--;
        } else {
            long rawContactId = ((MultiSelectContactListAdapter) adapter).getRawContactId(position);
            mSelectedContactIds.put(contactId, rawContactId);

            // int indexInSim;
            // if (isSearchMode()) {
            // // in search mode, the first item is search result title.
            // indexInSim = mSimList[position-1];
            // } else {
            // indexInSim = mSimList[position];
            // }
            // mSelectedIds.put(contactId, indexInSim);
            checked = true;
            mCheckedCount++;
        }

        updateCheckStatus();
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        ItemView newCache = (ItemView) viewHolder.view;
        newCache.setCheckBox(checked);
    }

    @Override
    protected void onItemClick(int position, long id) {
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    @Override
    protected void showCount(int partitionIndex, Cursor data) {
        super.showCount(partitionIndex, data);
        if (sIsDelete) {
            Log.d(TAG, "showCount mIsDelete return");
            return;
        }

        mCount = data.getCount();

        mCheckedCount = 0;

        long contactId = 0;
        for (int i = 0; i < mCount; i++) {
            int position = i;
            if (isSearchMode()) {
                // if search mode position 0 is search result item
                position++;
            }

            contactId = ((MultiSelectContactListAdapter) getAdapter()).getContactId(position);
            if (mSelectedContactIds.containsKey(contactId)) {
                mCheckedCount++;
            }
        }

        updateCheckStatus();
    }

    private void updateCheckStatus() {
        BaseActivity activity = (BaseActivity) getActivity();
        activity.setTitle2(getResources().getString(R.string.contact_picker_title, mCheckedCount));
        if (mCount == 0) {
            activity.setAllCheckBoxChecked(false);
            activity.setAllCheckBoxEnabled(false);
            activity.hideAllCheckBox();
        } else {
            activity.showAllCheckBox();
            activity.setAllCheckBoxChecked(mCheckedCount == mCount);
            activity.setAllCheckBoxEnabled(true);
        }
        if (isSearchMode()) {
            mFooter.setVisibility(View.GONE);
        } else {
            mFooter.setVisibility(View.VISIBLE);
            int checkedCount = mSelectedContactIds.size();
            boolean enabled = checkedCount > 0;
            mBtn.setEnabled(enabled);
        }
    }

    @Override
    public void selectedAll(boolean all) {
        ContactEntryListAdapter adapter = getAdapter();

        if (adapter == null || !(adapter instanceof MultiSelectContactListAdapter)) {
            Log.w(TAG, "selectedAll() adapter is null or not MultiSelectContactListAdapter instance!!!");
            return;
        }

        if (sIsDelete) {
            Log.w(TAG, "selectedAll: delete in in progress. skip.");
            Toast.makeText(getActivity(), R.string.deleting_progress_and_wait_text, Toast.LENGTH_SHORT).show();
            Activity activity = getActivity();
            if (activity instanceof BaseActivity) {
                CheckBox checkbox = ((BaseActivity) activity).getAllCheckBox();
                if (checkbox != null) {
                    checkbox.setChecked(!all);
                }
            }
            return;
        }

        if (all) {
            for (int position = 0; position < mCount; position++) {
                long contactId = ((MultiSelectContactListAdapter) adapter).getContactId(position);
                long rawContactId = ((MultiSelectContactListAdapter) adapter).getRawContactId(position);
                mSelectedContactIds.put(contactId, rawContactId);
            }
        } else {
            mSelectedContactIds.clear();
        }

        mCheckedCount = all ? mCount : 0;
        // check all
        updateCheckStatus();
        getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Bundle args = getArguments();
        if (args != null) {
            mAction = args.getInt(ACTION);
            mPosition = args.getInt(ContactSelectionActivity.EXTRA_LIST_POSITION);
        } else {
            mAction = -1;
            mPosition = 0;
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        // Navigate to new position after loading finished
        final ListView list = MultiSelectPeopleFragment.this.getListView();
        list.clearFocus();
        list.post(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if ((activity == null) || activity.isFinishing() || activity.isDestroyed()) {
                    Log.i(TAG, "onLoadFinished: in post runnable, the activity is not active.");
                    return;
                }
                int topPadding = 0;
                if (!isSectionFirstPosition(mPosition)) {
                    topPadding = mHeaderViewHeight;
                }
                list.setSelectionFromTop(mPosition, topPadding);
                DebugLog.d(TAG, "[ListPos]SetSelection success,pos=" + mPosition);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getAdapter().setDisplayPhotos(ContactsSettingActivity.readShowContactsHeadIconPreference(getActivity()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int count = mSelectedContactIds.size();
        long[] restoreCheckedIdArray = new long[count * 2];
        if (count > 0) {
            Iterator<Long> iter = mSelectedContactIds.keySet().iterator();
            int i = 0;
            // the array stores data in following format:
            // [contactId_1, rawContactId_1, contactId_2, rawContactId_2, ..., contactId_n, rawContactId_n, ...]
            while (iter.hasNext()) {
                Long contactId = iter.next();
                Long rawContactId = mSelectedContactIds.get(contactId);
                restoreCheckedIdArray[i++] = contactId;
                restoreCheckedIdArray[i++] = rawContactId;
            }
        }
        outState.putLongArray(KEY_CHECKED_LIST, restoreCheckedIdArray);
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        if (savedState == null) {
            return;
        }
        super.restoreSavedState(savedState);
        long[] restoreCheckedIdArray = savedState.getLongArray(KEY_CHECKED_LIST);
        if (restoreCheckedIdArray == null) {
            return;
        }
        mSelectedContactIds.clear();
        int count = restoreCheckedIdArray.length / 2;
        for (int i = 0; i < count; i++) {
            mSelectedContactIds.put(restoreCheckedIdArray[i * 2], restoreCheckedIdArray[i * 2 + 1]);
        }
    }

}
