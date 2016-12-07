/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.yunos.alicontacts.editor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.google.common.base.Objects;
import com.yunos.alicontacts.ContactSaveService;
import com.yunos.alicontacts.GroupMetaDataLoader;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ContactEditorActivity;
import com.yunos.alicontacts.group.GroupManagementActivity;
import com.yunos.alicontacts.group.GroupManager;
import com.yunos.alicontacts.model.RawContactDelta;
import com.yunos.alicontacts.model.RawContactDelta.ValuesDelta;
import com.yunos.alicontacts.model.RawContactModifier;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.alicontacts.model.dataitem.DataKind;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * An editor for group membership. Displays the current group membership list
 * and brings up a dialog to change it.
 */
public class GroupMembershipView extends LinearLayout implements OnClickListener {
    public static final int GROURP_MEMBERSHIP_TEXT_MAX_DISPLAY_LENGTH = 1024;

    private static final String TAG = "GroupMembershipView";
    // I think assign an invalid item group id greater than 0 will make mistakes.
    public static final int CREATE_NEW_GROUP_GROUP_ID = -100;
    private static final int GROUP_MANAGEMENT_GROUP_ID = -101;
    private static final int USELESS_GROUP_ID_COUNT_IN_MANAGEMENT_PAGE = 2;

    private HashSet<Long> mCheckedBackupSet;

    public static final class GroupSelectionItem implements Parcelable {
        private final long mGroupId;
        private final String mTitle;
        private boolean mChecked;

        public GroupSelectionItem(long groupId, String title, boolean checked) {
            this.mGroupId = groupId;
            this.mTitle = title;
            mChecked = checked;
        }

        public long getGroupId() {
            return mGroupId;
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
        }

        @Override
        public String toString() {
            return mTitle;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mTitle);
            dest.writeLong(mGroupId);
            dest.writeInt(mChecked ? 1 : 0);
        }

        public GroupSelectionItem(Parcel src) {
            mTitle = src.readString();
            mGroupId = src.readLong();
            mChecked = (src.readInt() == 1);
        }

        public static final Parcelable.Creator<GroupSelectionItem> CREATOR = new Parcelable.Creator<GroupSelectionItem>() {

            @Override
            public GroupSelectionItem createFromParcel(Parcel source) {
                return new GroupSelectionItem(source);
            }

            @Override
            public GroupSelectionItem[] newArray(int size) {
                return new GroupSelectionItem[size];
            }
        };

    }

    private RawContactDelta mState;
    private LongSparseArray<GroupMetaDataLoader.LoadedGroup> mGroupMetaData;
    private String mDataSet;
    private TextView mGroupList;
    private ArrayList<GroupSelectionItem> mDataList;
    private AlertDialog groupMembershipDlg;
    private AlertDialog mCreateGrpDlg;
    private EditText mGrpNameField;
    private String mGrpLabel;
    private DataKind mKind;
    private GroupMembershipDialogAdapter mGroupMemberAdapter;

    private Context mContext;

    public GroupMembershipView(Context context) {
        super(context);
        mContext = context;
    }

    public GroupMembershipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mGroupList = (TextView) findViewById(R.id.group_list);
        setOnClickListener(this);
    }

    public void setKind(DataKind kind) {
        mKind = kind;
    }

    public void setGroupMetaData(List<GroupMetaDataLoader.LoadedGroup> groupMetaData) {
        int count = groupMetaData.size();
        mGroupMetaData = new LongSparseArray<GroupMetaDataLoader.LoadedGroup>(count);
        for (GroupMetaDataLoader.LoadedGroup group : groupMetaData) {
            mGroupMetaData.put(group.mId, group);
        }
        updateView();
        updateGroupMemberAdapter();
    }

    public void setState(RawContactDelta state) {
        mState = state;
        mDataSet = mState.getDataSet();
        updateView();
    }

    private void updateView() {
        StringBuilder titles = new StringBuilder();
        GroupMetaDataLoader.LoadedGroup[] groups = getMembershipGroupsForDisplay();
        for (GroupMetaDataLoader.LoadedGroup group : groups) {
            String title = group.mTitle;
            if (!TextUtils.isEmpty(title)) {
                if (titles.length() != 0) {
                    titles.append(", ");
                }
                titles.append(title);
                if (titles.length() > GROURP_MEMBERSHIP_TEXT_MAX_DISPLAY_LENGTH) {
                    break;
                }
            }
        }

        if (titles.length() == 0) {
            mGroupList.setText(null);
        } else {
            mGroupList.setText(titles);
        }
        setVisibility(VISIBLE);
    }

    private GroupMetaDataLoader.LoadedGroup[] getMembershipGroupsForDisplay() {
        ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
        int count = entries == null ? -1 : entries.size();
        if ((mGroupMetaData == null) || (count <= 0)) {
            return new GroupMetaDataLoader.LoadedGroup[0];
        }
        List<GroupMetaDataLoader.LoadedGroup> resultList = new ArrayList<GroupMetaDataLoader.LoadedGroup>(count);
        for (ValuesDelta values : entries) {
            if (values.isDelete()) {
                continue;
            }
            Long id = values.getGroupRowId();
            if (id == null) {
                continue;
            }
            GroupMetaDataLoader.LoadedGroup group = mGroupMetaData.get(id);
            if ((group == null) || (!Objects.equal(group.mDataSet, mDataSet))) {
                continue;
            }
            resultList.add(group);
        }
        GroupMetaDataLoader.LoadedGroup[] result
                = resultList.toArray(new GroupMetaDataLoader.LoadedGroup[resultList.size()]);
        // sort this array to display the membership groups in the same order as groups list.
        Arrays.sort(result, new Comparator<GroupMetaDataLoader.LoadedGroup>() {
            @Override
            public int compare(GroupMetaDataLoader.LoadedGroup g1, GroupMetaDataLoader.LoadedGroup g2) {
                long result = g1.mId - g2.mId;
                if (result > 0) {
                    return 1;
                } else if (result < 0) {
                    return -1;
                }
                return 0;
            }
        });
        return result;
    }

    private void closeSoftInput() {
        InputMethodManager imm;
        imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && ((Activity) mContext).getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(((Activity) mContext).getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onClick(View v) {
        closeSoftInput();

        showgroupMembershipDlg();
        UsageReporter.onClick(ContactEditorActivity.class, UsageReporter.ContactsEditPage.EDITOR_GROUP_BUTTON_CLICKED);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (groupMembershipDlg != null) {
            groupMembershipDlg.dismiss();
            groupMembershipDlg = null;
        }
    }

    private boolean isGroupChecked(long groupId) {
        int count = mDataList.size();
        for (int i = 0; i < count; i++) {
            GroupSelectionItem item = mDataList.get(i);
            if (groupId == item.getGroupId()) {
                return item.isChecked();
            }
        }
        return false;
    }

    private boolean hasMembership(long groupId) {
        ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
        if (entries != null) {
            for (ValuesDelta values : entries) {
                if (!values.isDelete()) {
                    Long id = values.getGroupRowId();
                    if (id != null && id == groupId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void createNewGroup() {
        if (mCreateGrpDlg != null && mCreateGrpDlg.isShowing()) {
            mCreateGrpDlg.dismiss();
            mCreateGrpDlg = null;
        }

        mGrpNameField = null;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mGrpNameField = (EditText) inflater.inflate(R.layout.create_group_edit, null);
        showUpdateGroupDlg(R.string.insertGroupDescription, null, mGrpNameField, onCreateGroupYesListener);
        UsageReporter.onClick(ContactEditorActivity.class, UsageReporter.GroupListPage.CREATE_GROUP);
    }

    private void showUpdateGroupDlg(int titleRid, CharSequence content, EditText edit,
            DialogInterface.OnClickListener listener) {

        edit.setText(content);
        if (content != null)
            edit.setSelection(content.length());
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int margin = getResources().getDimensionPixelSize(R.dimen.custom_tag_dialog_edit_margin_left_right);
        p.gravity = Gravity.CENTER;
        p.setMarginStart(margin);
        p.setMarginEnd(margin);
        edit.setLayoutParams(p);
        edit.setFocusable(true);
        edit.setFocusableInTouchMode(true);
        edit.requestFocus();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mCreateGrpDlg = builder.setCancelable(true).setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, onCreateGroupNoListener).create();
        builder.setView(edit);
        mCreateGrpDlg.setTitle(titleRid);
        mCreateGrpDlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mCreateGrpDlg.show();
    }

    private void updateSystemStatusBar() {
        groupMembershipDlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(groupMembershipDlg.getWindow());
        systemBarManager.setViewFitsSystemWindows(groupMembershipDlg.getWindow(), false);
        systemBarManager.setStatusBarColor(getResources().getColor(R.color.title_color));
        systemBarManager.setStatusBarDarkMode(groupMembershipDlg.getWindow(), getResources().getBoolean(R.bool.contact_dark_mode));
    }

    private void updateGroupMemberAdapter() {
        mDataList = new ArrayList<GroupSelectionItem>();

        ValuesDelta[] membershipGroups = getMembershipGroupsSelect();
        int membershipGroupsIdx = 0;
        // YunOS BEGIN PB
        // ##module:(Contacts)  ##author:shihuai.wg@alibaba-inc.com
        // ##BugID:(8035411)  ##date:2016-03-29
        if (mGroupMetaData == null) {
            return;
        }
        // YunOS END PB
        int groupCount = mGroupMetaData.size();
        for (int i = 0; i < groupCount; i++) {
            GroupMetaDataLoader.LoadedGroup group = mGroupMetaData.valueAt(i);
            String dataSet = group.mDataSet;
            if (Objects.equal(dataSet, mDataSet)) {
                long groupId = group.mId;
                String title = group.mTitle;
                boolean checked = false;
                while (membershipGroupsIdx < membershipGroups.length) {
                    long membershipGroupId = membershipGroups[membershipGroupsIdx].getGroupRowId();
                    if (membershipGroupId < groupId) {
                        membershipGroupsIdx++;
                    } else if (membershipGroupId == groupId) {
                        checked = true;
                        membershipGroupsIdx++;
                        break;
                    } else {
                        break;
                    }
                }
                mDataList.add(new GroupSelectionItem(groupId, title, checked));
            }
        }

        mDataList.add(new GroupSelectionItem(CREATE_NEW_GROUP_GROUP_ID, mContext
                .getString(R.string.create_group_item_label), false));
        mDataList.add(new GroupSelectionItem(GROUP_MANAGEMENT_GROUP_ID, mContext.getString(R.string.group_management),
                false));

        int count = mDataList.size();
        if (mCheckedBackupSet != null) {
            for (int i = 0; i < count; i++) {
                mDataList.get(i).setChecked(mCheckedBackupSet.contains(Long.valueOf(mDataList.get(i).mGroupId)));
            }
        }

        mCheckedBackupSet = new HashSet<Long>(count);
        int size = mDataList.size();
        for (int i = 0; i < size; i++) {
            if (mDataList.get(i).isChecked()) {
                mCheckedBackupSet.add(Long.valueOf(mDataList.get(i).mGroupId));
            }
        }

        if (mGroupMemberAdapter == null) {
            mGroupMemberAdapter = new GroupMembershipDialogAdapter(mDataList);
        } else {
            mGroupMemberAdapter.updateData(mDataList);
        }
    }

    private ValuesDelta[] getMembershipGroupsSelect() {
        ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
        int count = entries == null ? -1 : entries.size();
        if (count <= 0) {
            return new ValuesDelta[0];
        }
        List<ValuesDelta> resultList = new ArrayList<ValuesDelta>(count);
        for (ValuesDelta values : entries) {
            if (values.isDelete() || (values.getGroupRowId() == null)) {
                // 3rd party apps might write invalid data.data1 for group membership row.
                // we have to prevent crash in this case.
                continue;
            }
            resultList.add(values);
        }
        ValuesDelta[] result = resultList.toArray(new ValuesDelta[resultList.size()]);
        // as we read mGroupMetaData in id "ASC" order, so we keep this result in the same order,
        // so that we can do some optimize in get checked items.
        Arrays.sort(result, new Comparator<ValuesDelta>() {
            @Override
            public int compare(ValuesDelta v1, ValuesDelta v2) {
                // id1 and id2 won't get null pointer, because we have skipped null ids above.
                long id1 = v1.getGroupRowId().longValue();
                long id2 = v2.getGroupRowId().longValue();
                long result = id1 - id2;
                if (result > 0) {
                    return 1;
                } else if (result < 0) {
                    return -1;
                }
                return 0;
            }
        });
        return result;
    }

    private void showgroupMembershipDlg() {
        if (mGroupMetaData == null) {
            Toast.makeText(mContext, R.string.group_metadata_not_loaded, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "showgroupMembershipDlg: group meta data is not loaded. quit.");
            return;
        }
        if (groupMembershipDlg != null && groupMembershipDlg.isShowing()) {
            groupMembershipDlg.dismiss();
            groupMembershipDlg = null;
        }

        updateGroupMemberAdapter();

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.group_membership_choose_dialog_view, null);

        ImageView cancelView = (ImageView) contentView.findViewById(R.id.cancel);
        TextView titleView = (TextView) contentView.findViewById(R.id.title);
        titleView.setText(R.string.choose_group);
        ImageView doneView = (ImageView) contentView.findViewById(R.id.save);
        cancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (groupMembershipDlg != null) {
                    groupMembershipDlg.dismiss();
                    groupMembershipDlg = null;
                }
                if (mCheckedBackupSet != null) {
                    mCheckedBackupSet = null;
                }
                closeSoftInput();
            }
        });

        doneView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
                if (entries != null) {
                    for (ValuesDelta entry : entries) {
                        if (!entry.isDelete()) {
                            Long groupId = entry.getGroupRowId();
                            if (groupId != null && !isGroupChecked(groupId)) {
                                entry.markDeleted();
                            }
                        }
                    }
                }

                // Now add the newly selected items
                int count = mDataList.size();

                for (int i = 0; i < count; i++) {
                    GroupSelectionItem item = mDataList.get(i);
                    long groupId = item.getGroupId();
                    if (item.isChecked() && !hasMembership(groupId)) {
                        ValuesDelta entry = RawContactModifier.insertChild(mState, mKind);
                        entry.setGroupRowId(groupId);
                    }
                }
                updateView();

                if (groupMembershipDlg != null) {
                    groupMembershipDlg.dismiss();
                    groupMembershipDlg = null;
                }
                if (mCheckedBackupSet != null) {
                    mCheckedBackupSet = null;
                }
                closeSoftInput();
            }
        });

        ListView list = (ListView) contentView.findViewById(R.id.choose_list);
        list.setAdapter(mGroupMemberAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> list, View view, int position, long id) {
                if (groupMembershipDlg == null) {
                    return;
                }

                GroupSelectionItem item = mDataList.get(position);

                if (item == null) {
                    Log.e(TAG, "onItemClick() item is null!!!");
                    return;
                }

                final long groupId = item.getGroupId();

                if (groupId == CREATE_NEW_GROUP_GROUP_ID) {
                    item.setChecked(false);
                    createNewGroup();
                } else if (groupId == GROUP_MANAGEMENT_GROUP_ID) {
                    launchGroupManagement();
                } else {
                    GroupItemHolder holder = (GroupItemHolder) view.getTag();
                    item.setChecked(!item.isChecked());

                    if (holder != null) {
                        holder.check.setChecked(item.isChecked());
                    }

                    if (mDataList.get(position).isChecked()) {
                        mCheckedBackupSet.add(Long.valueOf(mDataList.get(position).mGroupId));
                    } else {
                        mCheckedBackupSet.remove(Long.valueOf(mDataList.get(position).mGroupId));
                    }
                }

            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        groupMembershipDlg = builder.setView(contentView).setOnCancelListener(onChooseGroupCancelListener).create();
        ViewGroup group = (ViewGroup) groupMembershipDlg.getWindow().findViewById(android.R.id.content);
        View v = group.getChildAt(0);
        v.setBackgroundColor(getResources().getColor(R.color.group_membership_background_color));
        v.setPadding(0, 0, 0, 0);
        groupMembershipDlg.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        updateSystemStatusBar();
        group.setPadding(0, 0, 0, 0);
        groupMembershipDlg.show();
    }

    /**
     * Fix bug : 5595026. When user press back key or touch outside dialog
     * window to back to parent window, mCheckedBackupList should be cleared
     * because if user show this dialog again later, that new dialog may use
     * this information to set which items should be checked.
     */
    private DialogInterface.OnCancelListener onChooseGroupCancelListener = new DialogInterface.OnCancelListener() {

        @Override
        public void onCancel(DialogInterface arg0) {
            if (mCheckedBackupSet != null) {
                mCheckedBackupSet = null;
            }
            closeSoftInput();
        }

    };

    private DialogInterface.OnClickListener onCreateGroupYesListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {

            if (mGrpNameField != null) {
                mGrpLabel = mGrpNameField.getText().toString();
            } else {
                Log.e(TAG, "onCreateGroupYesListener: onClick mGrpNameField is null. ERROR!!!");
            }

            if (mGrpLabel != null && (mGrpLabel.length() > 0) && (mGrpLabel.trim().length() > 0)) {
                GroupManager groupManager = GroupManager.getInstance(mContext);
                if (groupManager.hasGroup(mGrpLabel)) {
                    Toast.makeText(mContext, R.string.create_same_name_group_error, Toast.LENGTH_LONG).show();
                    return;
                }

                AccountWithDataSet account = null;
                if (mState != null) {
                    String accountName = mState.getAccountName();
                    String accountType = mState.getAccountType();

                    if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                        account = new AccountWithDataSet(accountName, accountType, mState.getDataSet());
                    }
                }

                Intent intent = ContactSaveService.createNewGroupIntent(mContext, account, mGrpLabel, null, null, null);
                mContext.startService(intent);
            } else {
                Toast.makeText(mContext, R.string.no_group_name, Toast.LENGTH_LONG).show();
                return;
            }

            if (dialog != null) {
                dialog.dismiss();
            }
            closeSoftInput();

        }
    };

    private DialogInterface.OnClickListener onCreateGroupNoListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (dialog != null) {
                dialog.dismiss();
            }
            closeSoftInput();

        }
    };

    private class GroupMembershipDialogAdapter extends BaseAdapter {
        private ArrayList<GroupSelectionItem> mItems;

        public GroupMembershipDialogAdapter(ArrayList<GroupSelectionItem> items) {
            mItems = items;
        }

        public void updateData(ArrayList<GroupSelectionItem> data) {
            mItems = data;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mItems == null ? 0 : mItems.size();
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
        public View getView(int position, View convertView, ViewGroup parent) {
            final GroupSelectionItem item = getItem(position);
            if (item == null) {
                Log.e(TAG, "getView() item is null!!!");
                return null;
            }

            final int groupId = (int) item.mGroupId;
            switch (groupId) {
                case GROUP_MANAGEMENT_GROUP_ID: {
                    GroupButtonHolder buttonHolder = null;
                    if (convertView == null || !(convertView instanceof LinearLayout)) {
                        convertView = View.inflate(mContext, R.layout.group_text_item_view, null);
                        buttonHolder = new GroupButtonHolder(convertView);
                        convertView.setTag(buttonHolder);
                    } else {
                        buttonHolder = (GroupButtonHolder) convertView.getTag();
                    }
                    buttonHolder.text.setBackground(null);
                    buttonHolder.text.setText(R.string.group_management);
                    break;
                }
                case CREATE_NEW_GROUP_GROUP_ID: {
                    GroupButtonHolder buttonHolder = null;
                    if (convertView == null || !(convertView instanceof LinearLayout)) {
                        convertView = View.inflate(mContext, R.layout.group_text_item_view, null);
                        buttonHolder = new GroupButtonHolder(convertView);
                        convertView.setTag(buttonHolder);
                    } else {
                        buttonHolder = (GroupButtonHolder) convertView.getTag();
                    }
                    buttonHolder.text.setBackground(null);
                    buttonHolder.text.setText(R.string.create_group_item_label);
                    break;
                }
                default:
                    GroupItemHolder itmHolder = null;
                    if (convertView == null || !(convertView instanceof RelativeLayout)) {
                        convertView = View.inflate(mContext, R.layout.group_membership_dialog_item, null);
                        itmHolder = new GroupItemHolder(convertView);
                        convertView.setTag(itmHolder);
                    } else {
                        itmHolder = (GroupItemHolder) convertView.getTag();
                    }

                    itmHolder.name.setText(item.mTitle);

                    itmHolder.check.setVisibility(View.VISIBLE);
                    itmHolder.add.setVisibility(View.GONE);
                    itmHolder.check.setChecked(mDataList.get(position).isChecked());
                    break;
            }

            return convertView;
        }
    }

    static class GroupItemHolder {
        final TextView name;
        final CheckBox check;
        final ImageView add;

        public GroupItemHolder(View view) {
            name = (TextView) view.findViewById(R.id.name);
            check = (CheckBox) view.findViewById(R.id.checkbox);
            add = (ImageView) view.findViewById(R.id.add);
        }
    }

    static class GroupButtonHolder {
        final TextView text;

        public GroupButtonHolder(View view) {
            text = (TextView) view.findViewById(R.id.group_text_item);
        }
    }

    private void launchGroupManagement() {
        if (mContext == null) {
            Log.e(TAG, "launchGroupManagement() mContext is null!!!");
            return;
        }

        Intent intent = new Intent(GroupManagementActivity.ACTION_GROUP_MANAGEMENT);
        intent.setClass(mContext, GroupManagementActivity.class);

        try {
            if (mDataList != null) {
                ArrayList<GroupSelectionItem> listItems = (ArrayList<GroupSelectionItem>) mDataList.clone();

                int index = listItems.size() - 1;
                int count = 0; // remove two useless group item data;
                while (index >= 0 && index < listItems.size()) {
                    GroupSelectionItem item = listItems.get(index);
                    if (item != null
                            && (item.mGroupId == GROUP_MANAGEMENT_GROUP_ID)) {
                        listItems.remove(index);
                        count++;
                    }

                    index--;

                    if (USELESS_GROUP_ID_COUNT_IN_MANAGEMENT_PAGE == count) {
                        break;
                    }
                }

                int extraDataKey = GroupManagementActivity.ExtraGroupDataManager.putExtraGroupData(listItems);
                intent.putExtra(GroupManagementActivity.EXTRA_GROUP_DATA_KEY, extraDataKey);
            }

            intent.putExtra(GroupManagementActivity.EXTRA_STATE, mState);

            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "launchGroupManagement() Exception", e);
        }
    }
}
