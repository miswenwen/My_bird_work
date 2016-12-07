/**
* @Title:GroupMemberListActivity.java
* @Package:com.yunos.alicontacts.group
* @Description:manage group member
* @author:shihuai.wg@alibaba-inc.com
* @date:2016-04-21
* @version V1.0
*/

package com.yunos.alicontacts.group;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.yunos.alicontacts.activities.ContactDetailActivity;
import com.yunos.alicontacts.activities.ContactSelectionActivity;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.widget.aui.PopMenu;
import com.yunos.alicontacts.widget.aui.PopMenu.OnPopMenuListener;

import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;




public class GroupMemberListActivity extends ListActivity {
    private static final String TAG = "GroupMemberListActivity";

    private static final int ID_FOOTER_ICON_ADD = 1;
    private static final int ID_FOOTER_ICON_DELETE = 2;

    public static final int MEM_CONTACT_ID = 0;
    public static final int MEM_RAW_CONTACT_ID = 1;
    public static final int MEM_ID_COUNT = 2;
    private String[] mMemIDs = new String[MEM_ID_COUNT];

    private ListAdapter mAdapter;
    private Long mGroupId;
    private String mGroupName;

    private PopMenu mMenuDialog;

    private Cursor mListCursor;

    ActionBarView mActionBarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bunde = getIntent().getExtras();
        if (bunde != null) {
            mGroupId = bunde.getLong("group_id");
            mGroupName = bunde.getString("group_name");
        }

        initActionBar();
        setContentView(R.layout.group_member_list);

        setupListView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Filter filter = mAdapter.getFilter();
        filter.filter("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mListCursor != null) {
            mListCursor.close();
        }
    }

    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Uri uri = Contacts.getLookupUri(mListCursor.getLong(2), mListCursor.getString(4));
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(GroupMemberListActivity.this, ContactDetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void setupListView(){
        final ListView list = getListView();
        list.setOnCreateContextMenuListener(this);

        mAdapter = new ListAdapter(this);
        setListAdapter(mAdapter);

        // We manually save/restore the listview state
        list.setSaveEnabled(false);
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (mActionBarView == null) {
                mActionBarView = new ActionBarView(this);
            }
            actionBar.setCustomView(mActionBarView);
            actionBar.setDisplayShowCustomEnabled(true);
            mActionBarView.setTitleColor(getResources().getColor(R.color.aui_primary_txt_color_white));
            mActionBarView.setBackgroundColor(getResources().getColor(R.color.title_color));
            mActionBarView.setTitle(mGroupName);
            ImageView backView = new ImageView(this);
            backView.setImageResource(R.drawable.actionbar_back_selector);
            mActionBarView.addLeftItem(backView);
            mActionBarView.setOnLeftWidgetItemClickListener(new OnLeftWidgetItemClick() {
                @Override
                public void onLeftWidgetItemClick() {
                    finish();
                }
            });

            ImageView sendView = new ImageView(this);
            sendView.setImageResource(R.drawable.ic_pop_menu_normal);
            mActionBarView.addRightItem(sendView);
            mActionBarView.setOnRightWidgetItemClickListener(new OnRightWidgetItemClick() {
                @Override
                public void onRightWidgetItemClick() {
                    showMenu();
                }
            });
        }

        setSystembarColor(getResources().getColor(R.color.title_color), actionBar != null);
    }

    private void showMenu() {
        mMenuDialog = PopMenu.build(this, Gravity.TOP);
        mMenuDialog.setOnIemClickListener(mOnMenuItemClick);
        mMenuDialog.addItem(ID_FOOTER_ICON_ADD, getString(R.string.add_group_members));
        if (!isListEmpty()) {
            mMenuDialog.addItem(ID_FOOTER_ICON_DELETE, getString(R.string.rm_group_members));
        }
        mMenuDialog.show();
    }

    private OnPopMenuListener mOnMenuItemClick = new OnPopMenuListener() {

        @Override
        public void onMenuItemClick(int id) {
            switch (id) {
            case ID_FOOTER_ICON_ADD:
                addGroupMembers();
                break;
            case ID_FOOTER_ICON_DELETE:
                rmGroupMembers();
                break;
            }
        }
    };

    private boolean isListEmpty() {
        return TextUtils.isEmpty(mMemIDs[MEM_CONTACT_ID]);
    }

    public void addGroupMembers() {
        Intent intent = new Intent(ContactSelectionActivity.ACTION_PICK_MULTIPLE);
        intent.putExtra(ContactSelectionActivity.PICK_CONTENT, ContactSelectionActivity.PICK_CONTACT_ADD_TO_GROUP);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_MEM_IDS, mMemIDs);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_ID, mGroupId);
        startActivity(intent);
    }

    public void rmGroupMembers() {
        Intent intent = new Intent(ContactSelectionActivity.ACTION_PICK_MULTIPLE);
        intent.putExtra(ContactSelectionActivity.PICK_CONTENT, ContactSelectionActivity.PICK_CONTACT_IN_GROUP_TO_RM);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_MEM_IDS, mMemIDs);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_ID, mGroupId);
        startActivity(intent);
    }

    private void setSystembarColor(int color, boolean showActionBar) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, showActionBar);
        systemBarManager.setStatusBarColor(color);
    }


    private final class ListAdapter extends CursorAdapter {
        private LayoutInflater mInflater;
        public ListAdapter(Context context) {
            super(context, null, false);
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        protected void onContentChanged() {
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView( position,  convertView,  parent);
            return view;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.group_member_list_item, null);  
            setChildView(view, cursor);
            return view;
        }


        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            setChildView(view, cursor);
        }

        public void setChildView(View view, Cursor cursor) {
            ((TextView)view.findViewById(R.id.name)).setText(cursor.getString(1));
        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public Object getItem(int pos) {
            return super.getItem(pos);
        }

        @Override
        public long getItemId(int pos) {
            return super.getItemId(pos);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            Cursor cursor = doFilter(constraint.toString());
            return cursor;
        }

		Cursor doFilter(String filter) {
            Cursor cursor = getContentResolver().query(Data.CONTENT_URI, 
                new String[] {Data._ID, Data.DISPLAY_NAME, Data.CONTACT_ID, Data.RAW_CONTACT_ID, Data.LOOKUP_KEY},
                Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?" , 
                new String[] {GroupMembership.CONTENT_ITEM_TYPE, String.valueOf(mGroupId)}, Data.DISPLAY_NAME);

            StringBuilder contactIDs = new StringBuilder("");
            StringBuilder rawContactIDs = new StringBuilder("");

            for (int i=0; i<cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                contactIDs.append(cursor.getLong(2));
                contactIDs.append(',');
                rawContactIDs.append(cursor.getLong(3));
                rawContactIDs.append(',');
            }

            if (cursor.getCount() > 0 && contactIDs.length() > 0) {
                contactIDs.deleteCharAt(contactIDs.length() - 1);
                rawContactIDs.deleteCharAt(rawContactIDs.length() - 1);
            }

            mMemIDs[MEM_CONTACT_ID] = contactIDs.toString();
            mMemIDs[MEM_RAW_CONTACT_ID] = rawContactIDs.toString();

            mListCursor = cursor;
            mActionBarView.setTitle(mGroupName + "(" + mListCursor.getCount() + ")");

			return cursor;
		}
    }

}
