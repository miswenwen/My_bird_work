/**
* @Title:GroupMemberManagementActivity.java
* @Package:com.yunos.alicontacts.group
* @Description:manage group member
* @author:shihuai.wg@alibaba-inc.com
* @date:2016-04-21
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
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
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
import com.yunos.alicontacts.GroupMetaDataLoader;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.editor.GroupMembershipView.GroupSelectionItem;
import com.yunos.alicontacts.model.RawContactDelta;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import java.util.ArrayList;

public class GroupMemberManagementActivity extends ListActivity implements OnItemClickListener, 
        LoaderCallbacks<ArrayList<GroupSelectionItem>> {
    private static final String TAG = "GroupMemberManagementActivity";

    private GroupListAdapter mAdapter;
    private ArrayList<GroupSelectionItem> mItems;
    private RawContactDelta mState = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
        getLoaderManager().initLoader(0, null, this);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        long groupId = mItems.get(position).getGroupId();
        String groupName = mItems.get(position).toString();
        Intent intent = new Intent(this, GroupMemberListActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("group_name", groupName);
        bundle.putLong("group_id", groupId);
        intent.putExtras(bundle);
        startActivity(intent);
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
                + "(SELECT " + ContactsContract.RawContacts._ID
                + " FROM raw_contacts"
                + " WHERE " + ContactsContract.RawContacts.DELETED + "= 0 AND "  + ContactsContract.RawContacts._ID + " IN "
                + "(SELECT " + ContactsContract.Data.RAW_CONTACT_ID
                + " FROM data"
                + " WHERE data.data1 = ? ) ) )";

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
            long[] gIds = null;

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
            actionBarView.setTitle(getString(R.string.group_member_title));
            ImageView backView = new ImageView(this);
            backView.setImageResource(R.drawable.actionbar_back_selector);
            actionBarView.addLeftItem(backView);
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
            GroupSelectionItem item = getItem(position);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.group_list_item, parent, false);
            }
            ((TextView)convertView.findViewById(R.id.name)).setText(item.toString());
            return convertView;
        }
    }

}
