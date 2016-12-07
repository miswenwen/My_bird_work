
package com.yunos.alicontacts.plugin.duplicateremove;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseFragmentActivity;
import com.yunos.alicontacts.plugin.duplicateremove.widget.ButtonClickListener;
import com.yunos.alicontacts.plugin.duplicateremove.widget.MultiText2ButtonItem;
import com.yunos.alicontacts.plugin.duplicateremove.widget.Text2ButtonItem;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.widget.ItemAdapter;
import hwdroid.widget.item.Item;
import hwdroid.widget.item.SeparatorItem;
import hwdroid.widget.item.TextItem;
import hwdroid.widget.itemview.ItemView;
import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicateRemoveActivity extends BaseFragmentActivity implements CheckContact.CheckCallback,
        ButtonClickListener {
    private static final String TAG = "DuplicateRemoveActivity";

    private static final int MSG_CHECK_END = 32;
    private static final int MSG_CHECK_START = 33;
    private static final int MSG_CHECK_PROGRESS = 34;

    private static final int REQUEST_CODE_INCOMPLETE = 101;
    private static final int REQUEST_CODE_REPEAT = 102;
    private static final int DELETE_COUNT_EACH_TIME = 200;
    public static final String INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED = "finishActivityOnSaveCompleted";
    private static final String DELETE_LIST = "delete_list";
    private Handler mHandler;
    private Context mContext;

    private List<List<? extends BaseContactEntry>> mRepeatGroups = new ArrayList<List<? extends BaseContactEntry>>();
    private List<? extends BaseContactEntry> mInCompleteList;
    private List<BaseContactEntry> mIdenticalList = new ArrayList<BaseContactEntry>();
    private List<Long> mIdenticalDataIds = new ArrayList<Long>();

    private List<List<Item>> mItemGroups = new ArrayList<List<Item>>();
    private List<Item> mAdapterItems = new ArrayList<Item>();
    private ItemAdapter mAdpater;
    private Item mEditingItem;// item which is editing or merging inside Contacts Application
    private Item mIdenticalResultItem;// item which show the information for identical remove
    private Item mRepeatHeaderItem;
    private Item mIncompleteHeaderItem;
    private int mRepeatItemCount;
    private int mIncompleteItemCount;

    // private CheckRepeatContact mCheckWorker;
    private CheckContact mCheckWorker;

    // private TextView mTextView;
    private FragmentManager mFragmentManager;
    private Resources mRes;

    private ProgressDialog mProgressDialog;
    private boolean mDeleteWorkRunning;
    private DeleteContactsThread mDeleteThread;

    private AlertDialog mCancelDialog;
    private AlertDialog mCheckDialog;

    private static class CheckHandler extends Handler {
        private DuplicateRemoveActivity mActivity;
        public CheckHandler(DuplicateRemoveActivity activity) {
            mActivity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "handleMessage : msg is " + msg);
            ProgressDialog dialog = null;
            if((mActivity == null) || mActivity.isFinishing() || mActivity.isDestroyed()) {
                // if this activity is in finishing or already destroyed
                // no messages will be handled
                return;
            }
            switch (msg.what) {
                case MSG_CHECK_END:
                    mActivity.startDeleteContacts();
                    dialog = mActivity.mProgressDialog;
                    if (dialog != null) {
                        // dismiss progress dialog
                        dialog.dismiss();
                        dialog = null;
                    }
                    mActivity.displayCompareResult();
                    break;
                case MSG_CHECK_START:
                    // create ProgressDialog
                    dialog = new ProgressDialog(mActivity);
                    mActivity.mProgressDialog = dialog;
                    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    dialog.setMessage(mActivity.mRes.getString(R.string.progressbar_msg));
                    dialog.setProgressNumberFormat(null);
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setOnCancelListener(new OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface dialog) {
                            // if user cancel work, this plugin will exit
                            mActivity.finish();
                        }
                    });
                    dialog.setMax(100);
                    dialog.show();
                    break;
                case MSG_CHECK_PROGRESS:
                    dialog = mActivity.mProgressDialog;
                    if(dialog != null) {
                      dialog.setProgress(msg.arg1*100/msg.arg2);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // this.setActivityContentView(R.layout.activity_main);
        mRes = this.getResources();
        this.setTitle2(mRes.getString(R.string.dup_rm_activity_title));
        this.showBackKey(true);

        mContext = this;
        mDeleteWorkRunning = false;
        mHandler = new CheckHandler(this);

        IntroductionFragment fragment = new IntroductionFragment();
        mFragmentManager = this.getSupportFragmentManager();
        mFragmentManager.beginTransaction().add(android.R.id.content, fragment).commit();
        mFragmentManager.executePendingTransactions();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        if(mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        stopCheck();
        if (mCancelDialog != null) {
            mCancelDialog.dismiss();
            mCancelDialog = null;
        }
        if (mCheckDialog != null) {
            mCheckDialog.dismiss();
            mCheckDialog = null;
        }
        super.onDestroy();
    }

    // YunOS BEGIN PB
    // ##module:(Contacts)  ##author:shihuai.wg@alibaba-inc.com
    // ##BugID:(8261789)  ##date:2016-05-16
    @Override
    public void onBackKey() {
        if (mDeleteWorkRunning) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setCancelable(true)
                    .setPositiveButton(android.R.string.ok, new hwdroid.dialog.DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(hwdroid.dialog.DialogInterface dialog, int which) {
                            finish();
                        }
                    }).setNegativeButton(mRes.getString(android.R.string.cancel), null)
                    .setMessage(R.string.dialog_msg_exit_when_delete_not_finish);

            mCancelDialog = builder.create();
            mCancelDialog.show();
        } else {
            finish();
        }
    }
    // YunOS END PB

    /**
     * Fix bug : 5731702 -- two fragments show overlap
     * by tianyuan.ty
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // if this activity on background, system may destroy it for resource limit,
        // and when back to front, system will recreate it , which lead overlap.
        // comment the following code will cause no state information will be reserved
        // and used in onCreate() when recreate activity.
        //super.onSaveInstanceState(outState);
    }

    /**
     * Stop all work thread and related progress dialog
     */
    private void stopCheck() {
        Log.v(TAG, "stopCheck()");
        if(mCheckWorker != null) {
            mCheckWorker.cancelCheck();
        }
        if (mProgressDialog != null) {
            // dismiss progress dialog
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        if(mDeleteThread != null) {
            mDeleteThread.interrupt();
            mDeleteThread.mCancelDeleteWorkFlag = true;
            mDeleteThread = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // check if delete thread finish work
            if (mDeleteWorkRunning) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setCancelable(true)
                        .setPositiveButton(android.R.string.ok, new hwdroid.dialog.DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(hwdroid.dialog.DialogInterface dialog, int which) {
                                finish();
                            }
                        }).setNegativeButton(mRes.getString(android.R.string.cancel), null)
                        .setMessage(R.string.dialog_msg_exit_when_delete_not_finish);

                mCancelDialog = builder.create();
                mCancelDialog.show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    // callback function implementation for Interface
    // CheckRepeatContact.CheckCallback
    @Override
    public void removeIdenticalContacts(List<? extends BaseContactEntry> identical_set) {
        Log.v(TAG, "RemoveIdenticalContacts");
        // testOutputOnScreen("RemoveIdenticalContacts\n");
        // Iterator<? extends BaseContactEntry> iter = identical_set.iterator();
        // Log.v(TAG, "Identical -- ");
        // testOutputOnScreen("Identical --\n");
        // while(iter.hasNext()) {
        // showOneContactWithContactID(iter.next().mID);
        // }
        // Log.v(TAG,
        // "************************************************************************");
        // testOutputOnScreen("\n\n");
        // mDeletedCount += identical_set.size();
        this.mIdenticalList.addAll(identical_set);
    }

    @Override
    public void mergeReleateContacts(List<? extends BaseContactEntry> related_set) {
        Log.v(TAG, "mergeReleateContacts");
        // testOutputOnScreen("mergeReleateContacts : " +
        // related_set + "\n");
        // Iterator<? extends BaseContactEntry> iter = related_set.iterator();
        // Log.v(TAG, "Releated -- ");
        // testOutputOnScreen("Releated --\n");
        // while (iter.hasNext()) {
        // showOneContactWithContactID(iter.next().mID);
        // }
        // Log.v(TAG,
        // "************************************************************************");
        // testOutputOnScreen("\n\n");

        if (related_set != null)
            this.mRepeatGroups.add(related_set);
    }

    @Override
    public void removeIdenticalPhoneNumberDataRow(List<Long> identical_data_ids_set) {
        Log.v(TAG, "removeIdenticalPhoneNumberDataRow : identical_data_ids_set = "
                + identical_data_ids_set.toString());
        mIdenticalDataIds.addAll(identical_data_ids_set);
    }

    @Override
    public void notifyCheckStart( ) {
        mIdenticalList.clear();
        mIdenticalDataIds.clear();
        mItemGroups.clear();

        if(mHandler != null) {
            mHandler.sendEmptyMessage(MSG_CHECK_START);
        }
    }

    @Override
    public void notifyCheckEnd(List<? extends BaseContactEntry> inCompleteList) {
        // send check end message
        mInCompleteList = inCompleteList;

        if(mHandler != null)
            mHandler.sendEmptyMessage(MSG_CHECK_END);
    }

    @Override
    public void notifyCheckProgress(int current, int total) {
        ProgressDialog dialog = mProgressDialog;
        if(dialog != null) {
            //dialog.setProgress(current*100/total);
            mHandler.obtainMessage(MSG_CHECK_PROGRESS, current, total).sendToTarget();
        }
    }

    // private void testOutputOnScreen(String outStr) {
    // if (outStr != null && outStr.length() > 0) {
    // mHandler.obtainMessage(1, outStr).sendToTarget();
    // }
    // }

    private void startCheck(View v) {
        // Log.v(TAG, "startCheck() start at :  " + System.currentTimeMillis());
        if (mCheckWorker != null) {
            return; // have run
        }

        // popup dialog to notify user of risk for merging contacts
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mRes.getString(R.string.check_dialog_title));
        final View customView = LayoutInflater.from(this).inflate(R.layout.check_confirm_dialog_custom_view, null);
        final CheckBox checkbox = (CheckBox) customView.findViewById(R.id.checkbox_backup);
        builder.setView(customView);
        builder.setPositiveButton(android.R.string.ok, new hwdroid.dialog.DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(hwdroid.dialog.DialogInterface dialog, int which) {
                        // begin check
                        mCheckWorker = new CheckContact(DuplicateRemoveActivity.this, DuplicateRemoveActivity.this);
                        mCheckWorker.startCheck(checkbox != null ? checkbox.isChecked() : false);
                    }
                }).setNegativeButton(android.R.string.cancel, null);
        mCheckDialog = builder.create();
        mCheckDialog.show();
    }

    private void displayCompareResult() {
        Log.v(TAG, "displayCompareResult() start at :  " + System.currentTimeMillis());
        List<List<? extends BaseContactEntry>> repeatGroups = mRepeatGroups;
        List<List<Item>> itemsGroups = mItemGroups;
        List<Item> adapterItems = mAdapterItems;

        adapterItems.clear();
        itemsGroups.clear();

        // information for deleting duplicate contacts
        int deleteSize = mIdenticalList.size();
        if (deleteSize > 0) {
            mIdenticalResultItem = new TextItem(String.format(mRes.getString(R.string.delete_info),
                    deleteSize));
            mIdenticalResultItem.setEnabled(false);
            mAdapterItems.add(mIdenticalResultItem);
        }

        // list for repeat result groups
        int size = repeatGroups.size();
        Map<String, String> values = new HashMap<String, String>(1);
        values.put(UsageReporter.DuplicateRemove.SIMILAR_CONTACT_NUMBER, String.valueOf(size));
        UsageReporter.commitEvent(UsageReporter.DuplicateRemove.MERGE_CONTACTS, values);
        if (size > 0) {
            // Section header for repeat results
            mRepeatHeaderItem = new SeparatorItem(mRes.getString(R.string.section_header_repeat));
            mAdapterItems.add(mRepeatHeaderItem);
            for (int i = 0; i < size; i++) {
                List<? extends BaseContactEntry> repeat = repeatGroups.get(i);
                MultiText2ButtonItem item = new MultiText2ButtonItem(repeat, this,
                        (i == (size - 1)));
                adapterItems.add(item);
            }
            mRepeatItemCount = size;
        }

        // list for incomplete result
        List<BaseContactEntry> list = new ArrayList<BaseContactEntry>(mInCompleteList.size() + 1);
        list.addAll(mInCompleteList);
        // list.addAll(mOnlyNameList);
        size = list.size();
        if (size > 0) {
            mIncompleteHeaderItem = new SeparatorItem(
                    mRes.getString(R.string.section_header_incomplete));
            adapterItems.add(mIncompleteHeaderItem);
            for (int i = 0; i < size; i++) {
                BaseContactEntry entry = list.get(i);
                adapterItems.add(new Text2ButtonItem(entry, this));
            }
            mIncompleteItemCount = size;
        }

        // using the suitable fragment
        if (adapterItems == null || adapterItems.isEmpty()) {
            mFragmentManager.beginTransaction()
                    .replace(android.R.id.content, new NothingFragment()).commitAllowingStateLoss();
        } else {
            mAdpater = new ItemAdapter(this, adapterItems);
            mAdpater.setNotifyOnChange(true);
            // replace Introduction Fragment with Result ListFragment
            ResultFragment fragment = new ResultFragment();
            fragment.setAdapter(mAdpater);
            mFragmentManager.beginTransaction().replace(android.R.id.content, fragment)
                    .commitAllowingStateLoss();
        }
        mFragmentManager.executePendingTransactions();

    }

    /**
     * handle click for button on repeat group view
     */
    @Override
    public void onClick(Item item) {
        if (item instanceof Text2ButtonItem) {
            Log.v(TAG, "click for button on incomplete view");
            // start Contacts APP's ContactEditorActivity to edit this contact
            Intent intent = getViewContactIntent(item);
            customStartActivityForResult(intent, REQUEST_CODE_INCOMPLETE, item);
        } else if (item instanceof MultiText2ButtonItem) {
            Log.v(TAG, "click for button on repeat group view");
            Intent intent = getAddContactIntent(item);
            customStartActivityForResult(intent, REQUEST_CODE_REPEAT, item);
            UsageReporter.onClick(null,TAG, UsageReporter.DuplicateRemove.MERGE_SIMILAR_CONTACTS);
        }
    }

    void customStartActivityForResult(Intent intent, int requestCode, Item item) {
        if (intent != null) {
            intent.putExtra(INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            startActivityForResult(intent, requestCode);
            mEditingItem = item;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // super.onActivityResult(arg0, arg1, arg2);
        Log.v(TAG, "onActivityResult: resultCode = " + requestCode + ", resultCode = " + resultCode);

        switch (requestCode) {
            case REQUEST_CODE_INCOMPLETE:
                if ((resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED)
                        && mEditingItem != null) {
                    Text2ButtonItem incomplete_item = (Text2ButtonItem) mEditingItem;
                    boolean willDelete = false;
                    BaseContactEntry incomplete_entry = incomplete_item.mContactEntry;

                    if (incomplete_entry != null && incomplete_entry.mID > 0
                            && !CheckContact.isInCompleteContacts(mContext, incomplete_entry.mID)) {
                        willDelete = true;
                    }

                    if (willDelete) {
                        mAdpater.remove(mEditingItem);
                        mIncompleteItemCount--;
                    }
                }
                windowRestCheck();
                mEditingItem = null;
                break;
            case REQUEST_CODE_REPEAT:
                if (resultCode == Activity.RESULT_OK && mEditingItem != null) {
                    // startDeleteContacts(((MultiText2ButtonItem)mEditingItem).mContactEntries);
                    mAdpater.remove(mEditingItem);
                    mRepeatItemCount--;
                    windowRestCheck();
                }
                mEditingItem = null;
                break;

        }
    }

    private void windowRestCheck() {
        if (mIncompleteItemCount <= 0 && mIncompleteHeaderItem != null) {
            mAdpater.remove(mIncompleteHeaderItem);
            mIncompleteHeaderItem = null;
        } else if (mRepeatItemCount <= 0 && mRepeatHeaderItem != null) {
            mAdpater.remove(mRepeatHeaderItem);
            mRepeatHeaderItem = null;
        }

        if (mIdenticalResultItem == null && mIncompleteHeaderItem == null
                && mRepeatHeaderItem == null) {
            // nothing on window, should using NothingFragment
            mFragmentManager.beginTransaction()
                    .replace(android.R.id.content, new NothingFragment()).commitAllowingStateLoss();
            mFragmentManager.executePendingTransactions(); // must call in main thread
        }
    }

    private Intent getAddContactIntent(Item item) {
        MultiText2ButtonItem multi_item = (MultiText2ButtonItem) item;
        List<? extends BaseContactEntry> group = multi_item.mContactEntries;

        Intent createIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
        createIntent.setClassName("com.yunos.alicontacts",
                "com.yunos.alicontacts.activities.ContactEditorActivity");

        createIntent.putExtra(ContactsContract.Intents.Insert.NAME, group.get(0).mDisplayName);
        ArrayList<ContentValues> valuesList = mCheckWorker.getDataContentValueList(group);
        createIntent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, valuesList);

        // add extra list of contact id that need to be deleted
        int size = group.size();
        long[] delete_items = new long[size];
        for (int i = 0; i < size; i++) {
            delete_items[i] = group.get(i).mID;
        }
        createIntent.putExtra(DELETE_LIST, delete_items);

        return createIntent;
    }

    private Intent getViewContactIntent(Item item) {
        Text2ButtonItem incomplete_item = (Text2ButtonItem) item;
        BaseContactEntry incomplete_entry = incomplete_item.mContactEntry;

        Uri uri = Contacts.getLookupUri(incomplete_entry.mID, incomplete_entry.mLookupKey);
        Intent createIntent = new Intent(Intent.ACTION_VIEW, uri);
        // createIntent.setClassName("com.yunos.alicontacts",
        // "com.yunos.alicontacts.activities.ContactEditorActivity");
        return createIntent;
    }

    private long[] getIdsFromBaseContactEntryList(List<BaseContactEntry> enrtryList) {
        if (enrtryList == null) {
            return null;
        }

        int size = enrtryList.size();
        long ids[] = new long[size];
        for (int i = 0; i < size; i++) {
            ids[i] = enrtryList.get(i).mID;
        }

        return ids;
    }

    private void startDeleteContacts() {
        Log.v(TAG, "startDeleteContacts()");
        mDeleteThread = new DeleteContactsThread();
        mDeleteThread.setContactsIdsForDelete(getIdsFromBaseContactEntryList(mIdenticalList));
        mDeleteThread.setDatasIdsForDelete(mIdenticalDataIds.toArray(new Long[0]));
        mDeleteThread.start();
    }

    /*********************
     * Test Code
     */
    // private void dumpCursor(Cursor cursor, String name) {
    // Log.v(TAG,"Cursor dump for " + name + " :");
    //
    // if(cursor != null && cursor.moveToFirst()) {
    // do {
    // Log.v(TAG, cursor.getLong(0) + " : " + cursor.getString(1));
    //
    // } while(cursor.moveToNext());
    // }
    // }

    // private void showOneContactWithContactID(Long id) {
    // /*
    // * Cursor cursor = mResolver.query(AllPhonesQuery.CONTENT_URI,
    // * AllPhonesQuery.PROJECTION, Phone.CONTACT_ID + " = " + id, null,
    // * null); if(cursor.moveToFirst()) { Log.i(TAG, " " + cursor.getLong(0)
    // * + " : " + cursor.getString(1) + "  " + cursor.getString(3));
    // * testOutputOnScreen(" " + cursor.getLong(0) + " : " +
    // * cursor.getString(1) + "  " + cursor.getString(3) + "\n"); }
    // * cursor.close();
    // */
    //
    // }

    /**
     * Fragment for introduce the functions of contacts clean-up
     *
     * @author tianyuan.ty
     */
    public static class IntroductionFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.dup_rm_introduct_layout, container, false);

            Button button = (Button) layout.findViewById(R.id.btn_start_check);
            if (button != null) {
                button.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        ((DuplicateRemoveActivity) getActivity()).startCheck(arg0);
                    }

                });
            }

            return layout;

        }

    }

    public static class ResultFragment extends Fragment {
        private ListView mListView;
        private ItemAdapter mAdapter;

        public ResultFragment() {

        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.v(TAG, "ResultFragment##onCreateView()");
            mListView = new ListView(this.getActivity());
            if (mAdapter != null)
                mListView.setAdapter(mAdapter);

            mListView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    ItemView view = (ItemView) arg1;

                    ItemAdapter adapter = (ItemAdapter) mListView.getAdapter();
                    Item item = (Item) adapter.getItem(arg2);
                    item.setChecked(!item.isChecked());
                    view.setObject(item);
                }
            });

            return mListView;
        }

        @Override
        public void onStart() {
            super.onStart();
            Log.v(TAG, "ResultFragment#onStart() start at :  " + System.currentTimeMillis());
        }

        public void setAdapter(ItemAdapter adapter) {
            mAdapter = adapter;
            if (mListView != null) {
                mListView.setAdapter(mAdapter);
            }
        }
    }

    /**/
    public static class NothingFragment extends Fragment {
        private View mEmptyView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mEmptyView = inflater.inflate(R.layout.empty_result_layout, container, false);
            return mEmptyView;
        }
    }

    /**
     * Delete contacts thread
     */
    private class DeleteContactsThread extends Thread {
        public long[] mContactsIds;
        public Long[] mDataIds;
        public boolean mCancelDeleteWorkFlag;

        public DeleteContactsThread() {
            mCancelDeleteWorkFlag = false;
        }

        public void setContactsIdsForDelete(long[] ids) {
            mContactsIds = ids;
        }

        public void setDatasIdsForDelete(Long[] ids) {
            mDataIds = ids;
        }

        @Override
        public void run() {
            mDeleteWorkRunning = true;
            int dataLen = (mDataIds == null) ? 0 : mDataIds.length;
            int contactLen = (mContactsIds == null) ? 0 : mContactsIds.length;
            if ((dataLen + contactLen) == 0) {
                Log.e(TAG, "ERROR: DeleteContactsThread: list == null or contain not element.");
                mDeleteWorkRunning = false;
                return;
            }

            ContentResolver contentResolver = mContext.getContentResolver();
            if (contentResolver == null) {
                Log.e(TAG, "ERROR: DeleteContactsThread: getContentResolver() == null!!!");
                mDeleteWorkRunning = false;
                return;
            }
            long startTime = System.currentTimeMillis();

            //Arrays.sort(mContactsIds);

            // delete duplicate phone number in DATA table
            StringBuilder ids = new StringBuilder();
            int idCount = 0;
            for (int i = 0; i < dataLen; i++) {
                if (mCancelDeleteWorkFlag) {
                    Log.w(TAG, "[dupl_ty] DeleteContactsThread.run() exit for mCancelWorkFlag is true!!!");
                    mDeleteWorkRunning = false;
                    return;
                }

                if (ids.length() > 0) {
                    ids.append(",");
                }
                ids.append(mDataIds[i]);

                // Log.v(TAG, "DeleteContactsThread: current phone number data ids : " + ids.toString());

                // Do delete operation per DELETE_COUNT_EACH_TIME data row.
                if ((++idCount) >= DELETE_COUNT_EACH_TIME || i >= (dataLen - 1)) {
                    Log.v(TAG, "DeleteContactsThread: delete phone data : " + ids.toString());
                    contentResolver.delete(Data.CONTENT_URI, Data._ID + " IN (" + ids.toString()
                            + ")", null);

                    idCount = 0;
                    ids.delete(0, ids.length());
                }
            }


            // delete duplicate contacts
            ids.delete(0, ids.length());
            idCount = 0;
            // ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(30);
            for (int i = 0; i < contactLen; i++) {
                if (mCancelDeleteWorkFlag) {
                    Log.w(TAG, "[dupl_ty] DeleteContactsThread.run() exit for mCancelWorkFlag is true!!!");
                    mDeleteWorkRunning = false;
                    return;
                }

                if (ids.length() > 0) {
                    ids.append(",");
                }
                ids.append(mContactsIds[i]);
//                ops.add(ContentProviderOperation.newDelete(
//                        ContentUris.withAppendedId(RawContacts.CONTENT_URI, mContactsIds[i])).build());
                // Log.v(TAG, "DeleteContactsThread: current ids : " + ids.toString());

                // Do delete operation per DELETE_COUNT_EACH_TIME contacts.
                if ((++idCount) >= DELETE_COUNT_EACH_TIME || i >= (contactLen - 1)) {
                    Log.v(TAG, "DeleteContactsThread: delete contacts : " + ids.toString());
                    contentResolver.delete(RawContacts.CONTENT_URI, RawContacts.CONTACT_ID
                            + " IN (" + ids.toString() + ")", null);

//                        try {
//                            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
//                        } catch (RemoteException  e) {
//                            Log.e(TAG, "Delete with Bach Operation Exception with RemoteException!");
//                            e.printStackTrace();
//                        } catch (OperationApplicationException e) {
//                            Log.e(TAG, "Delete with Bach Operation Exception with OperationApplicationException!");
//                            e.printStackTrace();
//                        } finally {
//                            ops.clear();
//                        }

                    idCount = 0;
                    ids.delete(0, ids.length());
                }

            }

            Log.v(TAG, "DeleteContactsThread consume : " + (System.currentTimeMillis() - startTime));
            mDeleteWorkRunning = false;
        }

    }

}
