/**    
* @Title:GroupMemberShipActivity.java
* @Package:com.yunos.alicontacts.group
* @Description:show the group list to send sms
* @author:xingnuan.cxn@alibaba-inc.com
* @date:2016-04-19
* @version V1.0
*/

package com.yunos.alicontacts.group;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.net.Uri;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.google.common.collect.Lists;
import com.yunos.alicontacts.ContactSaveService;
import com.yunos.alicontacts.GroupMetaDataLoader;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.editor.GroupMembershipView;
import com.yunos.alicontacts.editor.GroupMembershipView.GroupSelectionItem;
import com.yunos.alicontacts.model.RawContactDelta;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.common.UiTools;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import android.provider.ContactsContract;
import yunos.support.v4.util.LongSparseArray;
import java.util.HashSet;
import java.util.Iterator;
import android.content.ContentUris;

import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.util.Constants;  
import java.util.ArrayList;

public class GroupMemberShipActivity extends ListActivity implements OnItemClickListener, 
        LoaderCallbacks<ArrayList<GroupSelectionItem>> {
    private static final String TAG = "GroupMemberShipActivity";
    public static final String EXTRA_GROUP_DATA_KEY = "extraGroupDataKey";
    public static final String EXTRA_STATE = "extraState";
    private int mMaxPickNumber = 1000;

    private GroupListAdapter mAdapter;
    private ArrayList<GroupSelectionItem> mItems;
    private RawContactDelta mState = null;
    private EditText mGroupEditText;
    private static LongSparseArray<Object> mDataSparseArray;
    private static LongSparseArray<Object> mCheckSparseArray;
    private AlertDialog mAlertDialog;
    private static ArrayList<Long> mGroupToSendSmsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
        Intent intent = getIntent();
        int itemsKey = intent.getIntExtra(EXTRA_GROUP_DATA_KEY, -1);
        if (itemsKey > 0) {
            mItems = ExtraGroupDataManager.removeExtraGroupData(itemsKey);
        }

        mState = getIntent().getParcelableExtra(EXTRA_STATE);
        if (mItems != null) {
            mAdapter = new GroupListAdapter(this, mItems);
            getListView().setAdapter(mAdapter);
        } else {
            getLoaderManager().initLoader(0, null, this);
        }
        if(mDataSparseArray==null){
             mDataSparseArray = new LongSparseArray<Object>();
         }
        if(mCheckSparseArray==null){
             mCheckSparseArray = new LongSparseArray<Object>();
         }
         if(mGroupToSendSmsList==null){
            mGroupToSendSmsList = new ArrayList<Long>();
         }

        getListView().setOnItemClickListener(this);
    }

    @Override
    protected void onDestroy() {
        mDataSparseArray=null;
        mCheckSparseArray=null;
        if(mGroupToSendSmsList!=null){
        	mGroupToSendSmsList.clear();
        	mGroupToSendSmsList=null;
        }
        if(mAlertDialog!=null){
            mAlertDialog.dismiss();
            mAlertDialog=null;
        }
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAdapter != null) {
            GroupSelectionItem item = mAdapter.getItem(position);
            if (item != null ) {
                item.setChecked(!item.isChecked());
                boolean isCheck = item.isChecked();
                ViewHolder holder = (ViewHolder) view.getTag();
                if (holder != null) {
                    holder.checkboxView.setChecked(isCheck);
                }
                if(mCheckSparseArray!=null){
                    mCheckSparseArray.put(item.getGroupId(),isCheck);
                }
                if(mGroupToSendSmsList!=null){
                    if(isCheck){
                       mGroupToSendSmsList.add(item.getGroupId());
                    }else{
                        mGroupToSendSmsList.remove(item.getGroupId());
                    }
                }
            }
             mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<ArrayList<GroupSelectionItem>> onCreateLoader(int id, Bundle args) {
        return new GroupListLoader(this, mState);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<GroupSelectionItem>> loader, ArrayList<GroupSelectionItem> data) {
        if (mItems != data) {
            mItems = data;
            if (mAdapter == null) {
                mAdapter = new GroupListAdapter(this, mItems);
                getListView().setAdapter(mAdapter);
            }

            mAdapter.updateListData(data);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<GroupSelectionItem>> loader) {
        if (mAdapter != null) {
            mAdapter.updateListData(null);
        }
    }

    protected static class GroupListLoader extends AsyncTaskLoader<ArrayList<GroupSelectionItem>> {
    private final Context mContext;
    private final RawContactDelta mState;

    private static final String CONTACTS_IN_GROUP_SELECT =
        ContactsContract.Data._ID + " IN "
        +"(SELECT " + ContactsContract.Data._ID + " FROM data WHERE data.mimetype_id IN (SELECT mimetypes._id FROM mimetypes WHERE mimetype = 'vnd.android.cursor.item/phone_v2')  AND "
        +  " data.raw_contact_id " + " IN "
               /*     + "(SELECT " + ContactsContract.RawContacts._ID
                    + " FROM raw_contacts" 
                    + " WHERE " + ContactsContract.RawContacts.DELETED + "= 0 AND "  + ContactsContract.RawContacts._ID + " IN " */
                            + "(SELECT " + ContactsContract.Data.RAW_CONTACT_ID
                            + " FROM data"
                            + " WHERE data.data1 = ? AND data.mimetype_id IN (SELECT mimetypes._id FROM mimetypes WHERE mimetype = 'vnd.android.cursor.item/group_membership')  ) )";

        private final static String[] GROUP_PROJECTION = new String[] {
                Groups._ID, Groups.TITLE, Groups.SUMMARY_COUNT/*,Groups.DATA_SETGroups.ACCOUNT_NAME, Groups.ACCOUNT_TYPE, 
                Groups.GROUP_VISIBLE, Groups.SHOULD_SYNC, Groups.NOTES*/
        };

        private final static String GROUP_SELECTION = Groups.DELETED + "=0";

        public GroupListLoader(Context context, RawContactDelta state) {
            super(context);
            mContext = context;
            mState = state;
        }

        @Override
        public ArrayList<GroupSelectionItem> loadInBackground() {
            ArrayList<GroupSelectionItem> items = Lists.newArrayList();

            Cursor cursor = null;
            try {
                String selection = Groups.DELETED + "=0";
                String[] selectionArgs = null;
                final Uri.Builder groupsUri = Groups.CONTENT_SUMMARY_URI.buildUpon();

                cursor = mContext.getContentResolver().query(groupsUri.build(), GROUP_PROJECTION,
                    GROUP_SELECTION, null, GroupMetaDataLoader.GROUPS_SORT_ORDER);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long groupId = cursor.getLong(0);
                        String title = cursor.getString(1);
                        long groupCount = cursor.getLong(2);
                        if(groupCount>0){
	                        String[] DATA_PROJECTION = new String[] {ContactsContract.Data._ID};
	                        String data_selection =  CONTACTS_IN_GROUP_SELECT;
	                        selectionArgs = new String[] {String.valueOf(groupId)};
	                        Cursor cursordata = null;
                           try{
                                 cursordata = mContext.getContentResolver().query(
                                    ContactsContract.Data.CONTENT_URI, DATA_PROJECTION,
                                    data_selection,
                                    selectionArgs,
                                    null);
                                 if (cursordata != null) {
                                     int size  = cursordata.getCount();
                                     long[] dataids = new long[size];
                                     int index = 0;
                                     while (cursordata.moveToNext()) {
                                        dataids[index++] = cursordata.getLong(0);
                                     }
                                     boolean ischeck = false;
                                     if(mCheckSparseArray != null && null == mCheckSparseArray.get(groupId)){
                                          mCheckSparseArray.put(groupId,ischeck);
                                     }

                                     if(mDataSparseArray!=null){
                                         mDataSparseArray.put(groupId,dataids);
                                     }
                                }
                            } catch (Exception e){ 
                                Log.e(TAG, "load data Exception", e);
                            } finally {
                                if (cursordata != null) {
                                    cursordata.close();
                                }
                            }
                        }
                        String str_count = "("+String.valueOf(groupCount)+")";
                        title += str_count;

                        if(mCheckSparseArray!=null){
                            boolean isCheck = false;
                            if(null == mCheckSparseArray.get(groupId)){
                                mCheckSparseArray.put(groupId,isCheck);
                            }else {
                                isCheck = (boolean)mCheckSparseArray.get(groupId);
                            }
                            items.add(new GroupSelectionItem(groupId, title, isCheck));
                            if(mGroupToSendSmsList!=null){
	                            if(isCheck&&!mGroupToSendSmsList.contains(groupId)){
	                                mGroupToSendSmsList.add(groupId);
	                            }else if(!isCheck&&mGroupToSendSmsList.contains(groupId)){
	                               mGroupToSendSmsList.remove(groupId);
	                            }
                            }
                       }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,"loadInBackground() Exception", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return items;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        protected void onReset() {
            onStopLoading();
        }
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            ActionBarView actionBarView = new ActionBarView(this);
            actionBar.setCustomView(actionBarView);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBarView.setTitleColor(getResources().getColor(R.color.activity_header_text_color));
            actionBarView.setBackgroundColor(getResources().getColor(R.color.title_color));
            actionBarView.setTitle(getString(R.string.choose_group));
            ImageView backView = new ImageView(this);
            backView.setImageResource(R.drawable.actionbar_back_selector);
            actionBarView.addLeftItem(backView);
            actionBarView.setOnLeftWidgetItemClickListener(new OnLeftWidgetItemClick() {
                @Override
                public void onLeftWidgetItemClick() {
                    finish();
                }
            });
            ImageView sendView = new ImageView(this);
            sendView.setImageResource(R.drawable.done_selector);
            actionBarView.addRightItem(sendView);
            actionBarView.setOnRightWidgetItemClickListener(new OnRightWidgetItemClick() {
                @Override
                public void onRightWidgetItemClick() {
                   sendSmsByGroup();
                }
            });
        }
        setSystembarColor(getResources().getColor(R.color.title_color), actionBar != null);
    }

    private void sendSmsByGroup() {
        boolean ischeck = false;
        int size =0;
        HashSet<Long> mSelectedIds =  new HashSet<Long>();
        mSelectedIds.clear();
        if(mGroupToSendSmsList!=null){
            int count = mGroupToSendSmsList.size();
            Log.d(TAG,"count:"+count);
            for(int i = 0;i<count;i++){
                ischeck = true;
                long[] cids = null;
                if(mDataSparseArray!=null){
                    cids = (long[])mDataSparseArray.get((long)mGroupToSendSmsList.get(i));
                    if(cids != null){
                        int cidcount=cids.length;
                        for(int k = 0; k<cidcount;k++){
                           Log.d(TAG,"cid:"+cids[k]);
                           mSelectedIds.add(cids[k]);
                        }
                    }
                }
            }
        }
       size = mSelectedIds.size();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog mAlertDialog = builder.setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                }).create();
        mAlertDialog.setTitle(getString(R.string.group_sendsms_fail_title));
        if(size==0){
            if(ischeck){
                mAlertDialog.setMessage(getString(R.string.group_sendsms_fail_empty));
            }else{
                mAlertDialog.setMessage(getString(R.string.group_sendsms_fail_uncheck));
            }
            mAlertDialog.show();
            return;
        }else if (size > mMaxPickNumber) {
            mAlertDialog.setMessage(getString(R.string.too_many_recipients));
            mAlertDialog.show();
            return;
        }
        Uri[] uris = new Uri[size];
        Iterator<Long> iter = mSelectedIds.iterator();
        int i = 0;
        while (iter.hasNext()) {
            uris[i] = ContentUris.withAppendedId(Phone.CONTENT_URI, iter.next());
            i++;
        }
        Intent res = new Intent();
        res.setClassName(ContactsUtils.MMS_PACKAGE, ContactsUtils.MMS_COMPOSE_ACTIVITY_NAME);
        res.setAction(Intent.ACTION_SEND);
        res.putExtra(Constants.EXTRA_PHONE_URIS, uris);
        startActivity(res);
    }

    private void setSystembarColor(int color, boolean showActionBar) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, showActionBar);
        systemBarManager.setStatusBarColor(color);
        systemBarManager.setStatusBarDarkMode(this, getResources().getBoolean(R.bool.contact_dark_mode));
    }

    private boolean hasSameGroupNameInList(final String groupName) {
        if (TextUtils.isEmpty(groupName)) {
            return false;
        }

        if (mItems != null) {
            int size = mItems.size();
            for (int i = 0; i < size; i++) {
                if (groupName.equals(mItems.get(i).toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    class ViewHolder {
        private final TextView nameView;
        private final CheckBox checkboxView;

        public ViewHolder(final Context context, View view) {
            nameView = (TextView) view.findViewById(R.id.name);
            checkboxView = (CheckBox) view.findViewById(R.id.checkbox);
            checkboxView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    GroupSelectionItem item = (GroupSelectionItem) v.getTag();
                    if (item != null) {
                        item.setChecked(!item.isChecked());
                        boolean isCheck = item.isChecked();
                        if(mCheckSparseArray!=null){   
                            mCheckSparseArray.put(item.getGroupId(),isCheck);
                        }
                        if(mGroupToSendSmsList!=null){
                            if(isCheck){
                                mGroupToSendSmsList.add(item.getGroupId());
                            }else{
                                mGroupToSendSmsList.remove(item.getGroupId());
                            }
                        }
                    }
                }
            });
        }
    }

    private class GroupListAdapter extends BaseAdapter {
        private Context mContext;
        private LayoutInflater mInflater;
        private ArrayList<GroupSelectionItem> mItems;

        public GroupListAdapter(Context context, ArrayList<GroupSelectionItem> items) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mItems = items;
        }

        public void updateListData(final ArrayList<GroupSelectionItem> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return (mItems == null) ? 0 : mItems.size();
        }

        @Override
        public GroupSelectionItem getItem(int position) {
            if (mItems != null) {
                return mItems.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;

            GroupSelectionItem item = getItem(position);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.group_membership_list_item, parent, false);
                viewHolder = new ViewHolder(mContext, convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.checkboxView.setTag(item);
            viewHolder.checkboxView.setChecked(item.isChecked());
            viewHolder.nameView.setText(item.toString());
            return convertView;
        }
    }

    /**
     * This class is used to manage the group data be handled in GroupManagementActivity.
     * If we pass the group data in Intent instance, which used in startActivity(),
     * then we might get android.os.TransactionTooLargeException on large amount of groups.
     * But we don't want to query all the group data again in GroupManagementActivity,
     * because we have already read the data before call startActivity().
     * So we need a manager to manage the life cycle of the group data
     * that will be used in GroupManagementActivity.
     */
    public static class ExtraGroupDataManager {
        private static int sGlobalSeq = 0;
        private static SparseArray<ArrayList<GroupSelectionItem>> sGlobalDataContainer
                = new SparseArray<ArrayList<GroupSelectionItem>>();

        /**
         * Put a list of group data for GroupManagementActivity to use.
         * @param data
         * @return the key to remove the group data from the manager.
         */
        public static synchronized int putExtraGroupData(ArrayList<GroupSelectionItem> data) {
            int currentSeq = sGlobalSeq++;
            if (sGlobalSeq < 0) {
                // very very little possibility that sGlobalSeq will overflow,
                // we still want to handle it.
                sGlobalSeq = 0;
            }
            sGlobalDataContainer.put(currentSeq, data);
            Log.i(TAG, "putExtraGroupData: put extra data with key "+currentSeq);
            return currentSeq;
        }

        /**
         * Retrieve and remove the group data in the manager by a key.
         * @param key The key generated by putExtraGroupData().
         * @return The group data corresponding to the key, null on not exist.
         */
        public static synchronized ArrayList<GroupSelectionItem> removeExtraGroupData(int key) {
            ArrayList<GroupSelectionItem> result = sGlobalDataContainer.get(key);
            sGlobalDataContainer.remove(key);
            Log.i(TAG, "removeExtraGroupData: retrieve extra group data with key "+key
                    +"; result="+(result == null ? -1 : result.size()));
            return result;
        }
    }
}
