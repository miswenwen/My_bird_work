
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

import java.util.ArrayList;

public class GroupManagementActivity extends ListActivity implements OnItemClickListener, ContactSaveService.Listener,
        LoaderCallbacks<ArrayList<GroupSelectionItem>> {
    private static final String TAG = "GroupManagementActivity";

    public static final String ACTION_GROUP_MANAGEMENT = "android.intent.action.GROUP_MANAGEMENT";
    public static final String EXTRA_GROUP_DATA_KEY = "extraGroupDataKey";
    public static final String EXTRA_STATE = "extraState";

    private GroupListAdapter mAdapter;
    private ArrayList<GroupSelectionItem> mItems;
    private RawContactDelta mState = null;

    private AlertDialog mGroupDeleteDialog;
    private AlertDialog mGroupEditDialog;
    private EditText mGroupEditText;

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

        getListView().setOnItemClickListener(this);
        ContactSaveService.registerListener(this);
    }

    @Override
    public void onServiceCompleted(Intent callbackIntent) {
        Log.d(TAG, "onServiceCompleted() callbackIntent:" + callbackIntent);
        if (callbackIntent != null) {
            boolean successed = callbackIntent.getBooleanExtra(ContactSaveService.EXTRA_OPERATE_GROUP_RESULT, false);
            Log.d(TAG, "onServiceCompleted() successed:" + successed);
            if (successed) {
                getLoaderManager().restartLoader(0, null, this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        ContactSaveService.unregisterListener(this);

        super.onDestroy();
        if (mGroupEditDialog != null) {
            mGroupEditDialog.dismiss();
            mGroupEditDialog = null;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAdapter != null) {
            GroupSelectionItem item = mAdapter.getItem(position);
            if (item != null && item.getGroupId() == GroupMembershipView.CREATE_NEW_GROUP_GROUP_ID) {
                createGroupEditDialog(this, R.string.insertGroupDescription, null);
            }
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

        private final static String[] GROUP_PROJECTION = new String[] {
                Groups._ID, Groups.TITLE
        };

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
                if (mState != null) {
                    String accountName = mState.getAccountName();
                    String accountType = mState.getAccountType();
                    if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                        // If the account is YunOS account and the first sync is scheduled running a bit time later,
                        // then we might have some unsynced local groups and newly created YunOS groups to be displayed.
                        // If the account is local account and we have just logged out YunOS account,
                        // the database update might not be performed immediately,
                        // so we will also have both YunOS groups and local groups in this case.
                        if (AccountType.LOCAL_ACCOUNT_TYPE.equals(accountType)) {
                            selection = Groups.DELETED + "=0 AND (("
                                    + Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?) OR "
                                    + Groups.ACCOUNT_TYPE + "=?)";
                            selectionArgs = new String[] {
                                    accountName, accountType, AccountType.YUNOS_ACCOUNT_TYPE
                            };
                        } else if (AccountType.YUNOS_ACCOUNT_TYPE.equals(accountType)) {
                            selection = Groups.DELETED + "=0 AND (("
                                    + Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?) OR ("
                                    + Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?))";
                            selectionArgs = new String[] {
                                    accountName, accountType,
                                    AccountType.LOCAL_ACCOUNT_NAME, AccountType.LOCAL_ACCOUNT_TYPE
                            };
                        } else {
                            selection = Groups.DELETED + "=0 AND "
                                    + Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?";
                            selectionArgs = new String[] {
                                    accountName, accountType
                            };
                        }
                    }
                }

                cursor = mContext.getContentResolver().query(
                        Groups.CONTENT_URI, GROUP_PROJECTION,
                        selection,
                        selectionArgs,
                        GroupMetaDataLoader.GROUPS_SORT_ORDER);

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long groupId = cursor.getLong(0);
                        String title = cursor.getString(1);

                        items.add(new GroupSelectionItem(groupId, title, false));
                    }
                }

                items.add(new GroupSelectionItem(GroupMembershipView.CREATE_NEW_GROUP_GROUP_ID, mContext
                        .getString(R.string.create_group_item_label), false));
            } catch (Exception e) {
                Log.e(TAG, "loadInBackground() Exception", e);
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
            actionBarView.setTitle(getString(R.string.group_management));
            ImageView view = new ImageView(this);
            view.setImageResource(R.drawable.actionbar_back_selector);
            actionBarView.addLeftItem(view);
            actionBarView.setOnLeftWidgetItemClickListener(new OnLeftWidgetItemClick() {

                @Override
                public void onLeftWidgetItemClick() {
                    finish();
                }
            });
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

    private void showDeleteGroupDialog(final Context context, final GroupSelectionItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mGroupDeleteDialog = builder.setTitle(R.string.delete_group).setMessage(R.string.delete_group_dialog_message)
                .setCancelable(true).setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (item != null) {
                            Intent intent = ContactSaveService.createGroupDeletionIntent(context, item.getGroupId());
                            Intent callbackIntent = new Intent(context, GroupManagementActivity.class);
                            intent.putExtra(ContactSaveService.EXTRA_CALLBACK_INTENT, callbackIntent);
                            context.startService(intent);
                        }
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                }).create();

        mGroupDeleteDialog.show();
    }

    private void createGroupEditDialog(final Context context, int titleResId, final GroupSelectionItem item) {
        if (mGroupEditDialog != null && mGroupEditDialog.isShowing()) {
            mGroupEditDialog.dismiss();
            mGroupEditDialog = null;
        }

        mGroupEditText = (EditText) LayoutInflater.from(this).inflate(R.layout.create_group_edit, null);
        if (item != null) {
            mGroupEditText.setText(item.toString());
        }

        showGroupEditDialog(titleResId, mGroupEditText, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mGroupEditText != null) {
                    // if mGroupName is null, means create new group, or update
                    // group.
                    String newGroupName = mGroupEditText.getText().toString();

                    boolean isInsert = (item == null);

                    if (!TextUtils.isEmpty(newGroupName)) {
                        if (hasSameGroupNameInList(newGroupName)) {
                            Toast.makeText(GroupManagementActivity.this, R.string.create_same_name_group_error,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (isInsert) {
                            AccountWithDataSet account = null;
                            if (mState != null) {
                                String accountName = mState.getAccountName();
                                String accountType = mState.getAccountType();

                                if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                                    account = new AccountWithDataSet(accountName, accountType, mState.getDataSet());
                                }
                            }

                            Intent intent = ContactSaveService.createNewGroupIntent(context, account, newGroupName,
                                    null, GroupManagementActivity.class, null);
                            context.startService(intent);

                        } else {
                            if (!newGroupName.equals(item.toString())) {

                                Intent intent = ContactSaveService.createGroupRenameIntent(context, item.getGroupId(),
                                        newGroupName, GroupManagementActivity.class, null);
                                context.startService(intent);
                            }
                        }
                    } else {
                        Toast.makeText(GroupManagementActivity.this, R.string.no_group_name, Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                if (dialog != null) {
                    dialog.dismiss();
                }

                UiTools.closeSoftInput(GroupManagementActivity.this);
            }
        });

        UsageReporter.onClick(GroupManagementActivity.class, UsageReporter.GroupListPage.CREATE_GROUP);
    }

    private void showGroupEditDialog(int titleRid, EditText edit, DialogInterface.OnClickListener listener) {

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int margin = getResources().getDimensionPixelSize(R.dimen.custom_tag_dialog_edit_margin_left_right);
        p.gravity = Gravity.CENTER;
        p.setMarginStart(margin);
        p.setMarginEnd(margin);
        edit.setLayoutParams(p);
        edit.setFocusable(true);
        edit.setFocusableInTouchMode(true);
        edit.requestFocus();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mGroupEditDialog = builder.setCancelable(true).setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        UiTools.closeSoftInput(GroupManagementActivity.this);
                    }
                }).create();
        builder.setView(edit);
        mGroupEditDialog.setTitle(titleRid);
        mGroupEditDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mGroupEditDialog.show();
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
        private final ImageView editView;
        private final ImageView removeView;

        public ViewHolder(final Context context, View view) {
            nameView = (TextView) view.findViewById(R.id.name);
            editView = (ImageView) view.findViewById(R.id.edit);
            removeView = (ImageView) view.findViewById(R.id.remove);

            editView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    GroupSelectionItem item = (GroupSelectionItem) v.getTag();
                    if (item != null) {
                        createGroupEditDialog(context, R.string.editGroupDescription, item);
                    }
                }
            });

            removeView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    GroupSelectionItem item = (GroupSelectionItem) v.getTag();
                    if (item != null) {
                        showDeleteGroupDialog(context, item);
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
                convertView = mInflater.inflate(R.layout.group_management_list_item, parent, false);
                viewHolder = new ViewHolder(mContext, convertView);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.editView.setTag(item);
            viewHolder.removeView.setTag(item);

            viewHolder.nameView.setText(item.toString());

            if (item.getGroupId() == GroupMembershipView.CREATE_NEW_GROUP_GROUP_ID) {
                viewHolder.editView.setVisibility(View.GONE);
                viewHolder.removeView.setVisibility(View.GONE);
                viewHolder.nameView.setGravity(Gravity.CENTER);
            } else {
                viewHolder.editView.setVisibility(View.VISIBLE);
                viewHolder.removeView.setVisibility(View.VISIBLE);
                viewHolder.nameView.setGravity(Gravity.CENTER_VERTICAL);
            }

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
