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

package com.yunos.alicontacts.list;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.GroupMemberLoader;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.activities.BaseActivity.OnAllCheckedListener;
import com.yunos.alicontacts.activities.BaseFragmentActivity;
import com.yunos.alicontacts.database.util.NumberNormalizeUtil;
import com.yunos.alicontacts.preference.ContactsSettingActivity;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.util.Constants;
import com.yunos.yundroid.widget.item.HeaderIconTextCBItem;
import com.yunos.yundroid.widget.itemview.HeaderIconTextCBItemView;
import com.yunos.yundroid.widget.itemview.HeaderIconTextItemView;
import com.yunos.yundroid.widget.itemview.ItemView;

import hwdroid.widget.searchview.SearchView;
import yunos.support.v4.app.LoaderManager;
import yunos.support.v4.app.LoaderManager.LoaderCallbacks;
import yunos.support.v4.content.CursorLoader;
import yunos.support.v4.content.Loader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class PhoneNumberMultiplePickerFragment extends
        ContactEntryListFragment<ContactEntryListAdapter> {
    private static final String TAG = "PhoneNumberMultiplePickerFragment";
    private static final String KEY_CHECKED_ID_LIST = "checkedIdsList";
    public static final String KEY_MAX_PICK_NUMBER = "maxPickNumber";
    public static final String EXTRA_KEY_PINNED_DATA_IDS = "pinned_data_ids";

    private int mCount;
    private int mCheckedCount;

    // set of ids of contacts which user has selected in any group
    //or from caller app
    private final HashSet<Long> mSelectedIds =  new HashSet<Long>();
    private final HashSet<Long> mPinnedIds = new HashSet<Long>();
    private final HashSet<Uri> mUnrecognizedUirs = new HashSet<Uri>();

    private String mGroupMemIds;
    private boolean mFilter;
    //private boolean mAllChecked;
    private onPhoneListActionListener mListener;
    private TextView mDoneBtn;
    private View mFooter;
    private Parcelable mGroupUri;

    public static final int MAX_PICK_NUMBER = 3000;
    private static final int MAX_INCOMING_UNRECOGNIZED_URIS = 100;
    private int mMaxPickNumber = MAX_PICK_NUMBER;

    private SearchView mSearchView;

    public void setMaxPickNumber(int maxPickNumber) {
        if (maxPickNumber > MAX_PICK_NUMBER) {
            maxPickNumber = MAX_PICK_NUMBER;
        } else if (maxPickNumber < 1) {
            maxPickNumber = 1;
        }
        mMaxPickNumber = maxPickNumber;
    }

    public void setSelectedUris(Parcelable[] uris) {
        mSelectedIds.clear();
        mUnrecognizedUirs.clear();
        if(uris == null) {
            Log.i(TAG, "setSelectedUris: null uris.");
            return;
        }
        int len = uris.length;
        for (int i = 0; i < len; i++) {
            /**
             * Bug 165915 : APR to throw Exception.
             * "uris" may have null elements, which is get through intent extra.
             */
            Uri uri = (Uri)uris[i];
            if (uri == null) {
                continue;
            }
            long id = -1;
            try {
                id = ContentUris.parseId(uri);
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "setSelectedUris: no id in uri "+uri, nfe);
            }
            if (id != -1) {
                mSelectedIds.add(id);
            } else {
                mUnrecognizedUirs.add(uri);
            }
        }
    }

    public void setPinnedIds(long[] ids) {
        mPinnedIds.clear();
        if (ids == null) {
            return;
        }
        for (long id : ids) {
            mPinnedIds.add(id);
        }
    }

    public void setGroupIDsFilter(String ids) {
        mGroupMemIds = ids;
        mFilter = true;
        //mAllChecked = true;
    }

    public PhoneNumberMultiplePickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DATA_SHORTCUT);
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        PhoneNumberMultipleListAdapter adapter = new PhoneNumberMultipleListAdapter(
                getActivity());
        adapter.setDisplayPhotos(ContactsSettingActivity.readShowContactsHeadIconPreference(getActivity()));
        if (mFilter) {
            //Log.v(TAG,"createListAdapter() : mGroupMemIds = " + mGroupMemIds);
            adapter.setGroupMembersIds(mGroupMemIds, true);
        }

        return adapter;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateFooterBar();
        ((BaseActivity) getActivity()).showAllCheckBox(new OnAllCheckedListener() {

            @Override
            public void onAllChecked(boolean checked) {
                selectedAll(checked);
            }
        });

        updateCheckStatus();
        setHomeText(getString(R.string.contactsGroupsLabel));
    }

    private SearchView.SearchViewListener mQueryTextListener = new SearchView.SearchViewListener() {
        @Override
        public void startOutAnimation(int time) {
            ActionBar actionBar = getActivity().getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            ((BaseFragmentActivity) getActivity()).restoreSearchBar();
        }

        @Override
        public void startInAnimation(int time) {
            ActionBar actionBar = getActivity().getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
            ((BaseFragmentActivity) getActivity()).setSearchBar();
        }

        @Override
        public void doTextChanged(CharSequence s) {
            setQueryString(s.toString(), true);
        }

    };
    private void updateFooterBar() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.done_footer_item, null);
        mFooter = v;
        mDoneBtn = (TextView) v.findViewById(R.id.footer_text_id);
        mDoneBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                doSetResult();
            }
        });
        ((BaseFragmentActivity) getActivity()).addFooterView(v);
        mDoneBtn.setEnabled(false);
    }

    private void doSetResult() {
        int size = mSelectedIds.size();
        int unrecognizedSize = mUnrecognizedUirs.size();
        Uri[] uris = new Uri[size + unrecognizedSize];
        Iterator<Long> iter = mSelectedIds.iterator();
        int i = 0;
        while (iter.hasNext()) {
            Long id = iter.next();
            uris[i++] = ContentUris.withAppendedId(Phone.CONTENT_URI, id);
        }

        for (Uri uri : mUnrecognizedUirs) {
            uris[i++] = uri;
        }

        if ((size + unrecognizedSize) > mMaxPickNumber) {
            Toast.makeText(getActivity(), getString(R.string.too_many_recipients, mMaxPickNumber), Toast.LENGTH_LONG).show();
            return;
        }
        Intent res = new Intent();
        if (mFilter) {
            res.setClassName(ContactsUtils.MMS_PACKAGE, ContactsUtils.MMS_COMPOSE_ACTIVITY_NAME);
            res.setAction(Intent.ACTION_SEND);
            res.putExtra(Constants.EXTRA_PHONE_URIS, uris);
            getActivity().startActivity(res);
        } else {
            res.putExtra(Constants.EXTRA_PHONE_URIS, uris);
            getActivity().setResult(Activity.RESULT_OK, res);
        }
        getActivity().finish();
    }

    private class PhoneNumberMultipleListAdapter extends PhoneNumberListAdapter {

        private String mGroupMembersIds;
        private boolean mShowGroupState;

        public PhoneNumberMultipleListAdapter(Context context) {
            super(context);
        }

        @Override
        public void configureLoader(CursorLoader loader, long directoryId) {
            super.configureLoader(loader, directoryId);
            updateLoaderSelections(loader);
        }

        @Override
        protected HeaderIconTextItemView createItemView(Context context, ViewGroup parent) {
            HeaderIconTextCBItem item = new HeaderIconTextCBItem();
            HeaderIconTextCBItemView itemView = (HeaderIconTextCBItemView) item.newView(context, parent);
            return itemView;
        }

        @Override
        protected void bindView(View itemView, int partition, Cursor cursor, final int position) {
            super.bindView(itemView, partition, cursor, position);
            HeaderIconTextItemView view = (HeaderIconTextItemView)itemView;
            bindCheckBox(view, position);
        }

        @Override
        protected void bindPhoneNumber(HeaderIconTextItemView view, Cursor cursor) {

            CharSequence number = cursor.getString(PhoneQuery.PHONE_NUMBER);
            if (TextUtils.isEmpty(number)) {
                view.setSubtextView(mUnknownNameText.toString());
            } else {
                view.setSubtextView(ContactsUtils.formatPhoneNumberWithCurrentCountryIso(number.toString(), getContext()));
            }
        }

        protected void bindCheckBox(ItemView view, int pos) {
            int position = pos;
            if (isSearchMode()) {
                // if search mode position 0 is search result item
                position++;
            }
            long id = getAdapter().getItemId(position);
            if (mSelectedIds.contains(id)) {
                view.setCheckBox(true);
            } else {
                view.setCheckBox(false);
            }
        }

        public void setGroupMembersIds(String ids, boolean show) {
            mShowGroupState = show;
            mGroupMembersIds = ids;
        }

        public void setShowGroupState(boolean show) {
            mShowGroupState = show;
        }

        private void updateLoaderSelections(CursorLoader loader) {
            if (mShowGroupState) {
                String selection = loader.getSelection();
                StringBuilder sb = new StringBuilder(32);
                if (!TextUtils.isEmpty(selection)) {
                    sb.append(selection).append(" AND ");
                }
                sb.append(Phone.CONTACT_ID).append(" IN (").append(mGroupMembersIds).append(')');
                loader.setSelection(sb.toString());
            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onItemClick(parent, view, position, id);

        ItemView newCache = (ItemView) view.getTag();
        if (newCache == null) {
            // We called setTag() in newView() and pass a non-null object.
            // But the bug [ https://k3.alibaba-inc.com/issue/8002514?versionId=1169325 ]
            // show that we still can get null tag here.
            Log.e(TAG, "onItemClick: ERROR, can not getTag from view["+view+"].");
            return;
        }
        Long idObj = Long.valueOf(id);
        if (mPinnedIds.contains(idObj)) {
            Toast.makeText(getContext(), R.string.uncheck_pinned_recipients, Toast.LENGTH_SHORT).show();
            return;
        }
        if(mSelectedIds.contains(idObj)){
            mCheckedCount--;
            mSelectedIds.remove(idObj);
            newCache.setCheckBox(false);
        } else {
            if ((mCheckedCount + mUnrecognizedUirs.size()) == mMaxPickNumber) {
                Toast.makeText(getActivity(), getString(R.string.too_many_recipients, mMaxPickNumber), Toast.LENGTH_LONG).show();
                return;
            }
            mCheckedCount++;
            mSelectedIds.add(idObj);
            newCache.setCheckBox(true);
        }
        updateCheckStatus();
    }

    @Override
    protected void onItemClick(int position, long id) {

    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, container, false);
    }

    @Override
    protected void showCount(int partitionIndex, Cursor data) {
        super.showCount(partitionIndex, data);

        mCount = (data == null ? 0 : data.getCount());
        Log.d(TAG, "showCount: mCount = " + mCount);

        if (mListener != null) {
            //mListener.onGetContactCount(mCount);
            int phone_contacts = SimContactUtils.getPhoneContactCount();
            mListener.onGetContactCount(mCount > phone_contacts ? mCount : phone_contacts);
        }

        TextView empty = (TextView)(getRootView().findViewById(R.id.empty));
        empty.setVisibility(mCount == 0 ? View.VISIBLE : View.GONE);
        mCheckedCount = 0;

        //setupCheckList(mCount);
        for (int i = 0; i < mCount; i++) {
            int position = i;
            if (isSearchMode()) {
                // if search mode position 0 is search result item
                position++;
            }
            long contactId = getAdapter().getItemId(position);
            if (mSelectedIds.contains(contactId)) {
                mCheckedCount++;
            }
        }
        updateCheckStatus();
    }

//    private void setupCheckList(int counter) {
//        mCheckedList = new boolean[counter];
//        mIdList = new long[counter];
//    }
//
//    public boolean[] getCheckedList() {
//        return mCheckedList;
//    }
//
//    public long[] getIdList() {
//        return mIdList;
//    }

    private void updateCheckStatus() {
        BaseActivity activity = (BaseActivity) getActivity();

        if (activity != null) {
            int allCheckedCount =  mSelectedIds.size();
            mDoneBtn.setEnabled(allCheckedCount > 0);
            activity.setTitle2(getResources().getString(R.string.contact_picker_title, mCheckedCount));
            if (mCount == 0) {
                activity.setAllCheckBoxChecked(false);
                activity.setAllCheckBoxEnabled(false);
            } else {
                activity.setAllCheckBoxChecked(mCheckedCount == mCount);
                activity.setAllCheckBoxEnabled(true);
            }
            mFooter.setVisibility(isSearchMode() ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void selectedAll(final boolean checked) {
        ContactEntryListAdapter adapter = getAdapter();
        if(adapter == null){
            return;
        }

        if (checked && ((mCount + mUnrecognizedUirs.size()) > mMaxPickNumber)) {
            Toast.makeText(getActivity(), getString(R.string.too_many_recipients, mMaxPickNumber), Toast.LENGTH_LONG).show();
        } else {
            int reachPinnedCount = 0;
            for (int i = 0/* 1 */; i < mCount; i++) {
                // Update already checked ids set.
                // mSelectedIds contains all ids of selected contact for ALL contacts.
                // when check/uncheck under any group, only part of mSelectedIds
                // will be affected. So every id will be handled here one by one.
                Long id = adapter.getItemId(i);
                if (checked) {
                    mSelectedIds.add(id);
                } else {
                    if (mPinnedIds.contains(id)) {
                        reachPinnedCount++;
                    } else {
                        mSelectedIds.remove(id);
                    }
                }
            }
            mCheckedCount = checked ? mCount : 0;
            if (reachPinnedCount > 0) {
                mCheckedCount = reachPinnedCount;
                Toast.makeText(getActivity(), R.string.pinned_recipients_left_checked, Toast.LENGTH_LONG).show();
            }
        }
        // check all
        updateCheckStatus();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            long[] checkedBackupList = savedState.getLongArray(KEY_CHECKED_ID_LIST);
            if (checkedBackupList != null) {
                int len = checkedBackupList.length;
                for (int i = 0; i < len; i++) {
                    mSelectedIds.add(checkedBackupList[i]);
                }
            }
            // YUNOS BEGIN
            // BugID:5777482
            // Description: restore maxpicknumber.
            // author:changjun.bcj@alibaba-inc.com
            // date:2015-03-05
            int maxCount = savedState.getInt(KEY_MAX_PICK_NUMBER);
            if (maxCount > 0) {
                mMaxPickNumber = maxCount;
            }
            // YUNOS END
        }
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        initHeaderView();
    }

    @Override
    public void onResume() {
        super.onResume();
        getAdapter().setDisplayPhotos(ContactsSettingActivity.readShowContactsHeadIconPreference(getActivity()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "onSaveInstanceState");
        int len = mSelectedIds.size();
        if(len > 0) {
            long[] backupCheckedIdList = new long[len];
            Iterator<Long> iter = mSelectedIds.iterator();
            int i = 0;
            while(iter.hasNext() && i < len) {
                backupCheckedIdList[i] = iter.next();
                i++;
            }
            outState.putLongArray(KEY_CHECKED_ID_LIST, backupCheckedIdList);
        }
        // YUNOS BEGIN
        // BugID:5777482
        // Description: save maxpicknumber.
        // author:changjun.bcj@alibaba-inc.com
        // date:2015-03-05
        outState.putInt(KEY_MAX_PICK_NUMBER, mMaxPickNumber);
        // YUNOS END
    }

    public void setHomeText(String text) {
       // (mActionBar.getHomeButton()).setText(text);
    }

    public interface onPhoneListActionListener{
        void onGetContactCount(int count);
    }
    public void setOnPhoneListActionListener(onPhoneListActionListener listener) {
        mListener = listener;
    }

    // add viewGroup operations
    public void viewGroupAction(Uri groupUri) {
        mGroupUri = groupUri;
        Log.v(TAG,"viewGroupAction() : " + groupUri);
        if (groupUri != null) {
            mGroupId = ContentUris.parseId(groupUri);
            getLoaderManager().restartLoader(LOADER_MEMBERS, null, mGroupMemberListLoaderListener);
        } else {
            ((PhoneNumberMultipleListAdapter) getAdapter()).setShowGroupState(false);
            getLoaderManager().destroyLoader(LOADER_MEMBERS);
            reloadData();
        }
    }

    private static final int LOADER_MEMBERS = 10;
    private long mGroupId ;

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            Activity activity = getActivity();
            if(activity == null) {
                Log.e(TAG, "ERROR!!! The fragment is not attached. getActivity() is null.");
                return null;
            }
            return GroupMemberLoader.constructLoaderForGroupDetailQuery(getActivity(), mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            // 4 digit and one comma for one id on the average
            StringBuilder memberContactIDs = new StringBuilder(data.getCount() * 5);

            while(data.moveToNext()) {
                memberContactIDs.append(data.getLong(GroupMemberLoader.GroupDetailQuery.CONTACT_ID));
                memberContactIDs.append(',');
            }
            //delete the last ","
            if (data.getCount() > 0 && memberContactIDs.length() > 0) {
                memberContactIDs.deleteCharAt(memberContactIDs.length() - 1);
            }

            ((PhoneNumberMultipleListAdapter) getAdapter()).setGroupMembersIds(memberContactIDs.toString(), true);
            reloadData();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    private void initHeaderView() {
        ViewStub vs = (ViewStub) getRootView().findViewById(R.id.contacts_list_header);
        if (vs == null) {
            return;
        }

        View searchHeaderView = vs.inflate();

        mSearchView = (SearchView)searchHeaderView.findViewById(R.id.search_view);
        mSearchView.setAnchorView(((BaseActivity)getActivity()).getActionBarView());
        mSearchView.setSearchViewListener(mQueryTextListener);
        mSearchView.setBackgroundColor(getActivity().getResources().getColor(R.color.aui_bg_color_white));
    }

    private String mQueryString;

    public boolean queryTextChange(String queryString) {
        if (queryString.equals(mQueryString)) {
            return false;
        }
        mQueryString = queryString;
        setQueryString(mQueryString, true);

        return true;
    }

    public String getGroupNameById(long groupId) {
        Uri uri = Groups.CONTENT_URI;
        String where = Groups._ID + "=?";
        String[] whereParams = new String[] {
            Long.toString(groupId)
        };
        String[] selectColumns = {
            Groups.TITLE
        };
        Cursor c = this.getActivity().getContentResolver()
                .query(uri, selectColumns, where, whereParams, null);

        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
            return null;
        } finally {
            c.close();
        }
    }

    public void setGroupUri(Parcelable uri) {
        mGroupUri = uri;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if ((mSelectedIds.size() + mUnrecognizedUirs.size()) > mMaxPickNumber) {
            // Do not respond to the buggy caller.
            Log.w(TAG, "setSelectedUris: too many selected uris in input. quit.");
            getActivity().finish();
        }
        Intent intent = activity.getIntent();
        if ((intent != null) && intent.hasExtra(EXTRA_KEY_PINNED_DATA_IDS)) {
            long[] pinnedIds = intent.getLongArrayExtra(EXTRA_KEY_PINNED_DATA_IDS);
            setPinnedIds(pinnedIds);
        }
        if (mGroupUri != null) {
            this.viewGroupAction((Uri) mGroupUri);
        }
    }

    @Override
    protected void startLoading() {
        final HashMap<String, Uri> unrecognizedTelUrisMap = parseUnrecognizedUris();
        int unrecognizedCount = unrecognizedTelUrisMap == null ? -1 : unrecognizedTelUrisMap.size();
        Log.i(TAG, "startLoading: unrecognizedCount="+unrecognizedCount);
        if ((unrecognizedCount > 0) && (unrecognizedCount <= MAX_INCOMING_UNRECOGNIZED_URIS)) {
            // before really loading, we shall query the unrecognized numbers first.
            queryUnrecognizedTelUrisAsync(unrecognizedTelUrisMap);
        } else {
            reallyStartLoading();
        }
    }

    private void reallyStartLoading() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "reallyStartLoading: activity is null.");
            return;
        }
        super.startLoading();
    }

    private HashMap<String, Uri> parseUnrecognizedUris() {
        if (mUnrecognizedUirs.isEmpty()) {
            return null;
        }
        if (mUnrecognizedUirs.size() > MAX_INCOMING_UNRECOGNIZED_URIS) {
            Toast.makeText(getContext(), R.string.too_many_unrecognized_numbers, Toast.LENGTH_SHORT).show();
            return null;
        }
        HashMap<String, Uri> result = new HashMap<String, Uri>(mUnrecognizedUirs.size());
        for (Uri uri : mUnrecognizedUirs) {
            // uri should not be null, nulls are filtered out while adding into mUnrecognizedUirs.
            String scheme = uri.getScheme();
            if (!Constants.SCHEME_TEL.equals(scheme)) {
                continue;
            }
            String number = NumberNormalizeUtil.normalizeNumber(uri.getSchemeSpecificPart(), true);
            if (TextUtils.isEmpty(number)) {
                continue;
            }
            result.put(number, uri);
        }
        return result;
    }

    private void queryUnrecognizedTelUrisAsync(final HashMap<String, Uri> numbersToQuery) {
        AsyncTask<String, Void, Map<String, Uri>> queryTask = new AsyncTask<String, Void, Map<String, Uri>>() {
            @Override
            protected Map<String, Uri> doInBackground(String... numbers) {
                Log.i(TAG, "queryUnrecognizedTelUrisAsync.queryTask.doInBackground: numbers count="+numbers.length);
                return queryPhoneNumbersToPhoneUri(numbers);
            }

            @Override
            protected void onPostExecute(Map<String, Uri> result) {
                int size = result == null ? -1 : result.size();
                Log.i(TAG, "queryUnrecognizedTelUrisAsync.queryTask.onPostExecute: result size="+size);
                if (size > 0) {
                    for (String number : result.keySet()) {
                        Uri uri = result.get(number);
                        Uri uriToRemove = numbersToQuery.get(number);
                        mUnrecognizedUirs.remove(uriToRemove);
                        // the uri is generated in queryPhoneNumberToPhoneUri(),
                        // it won't be a bad uri, the id part is guaranteed.
                        mSelectedIds.add(ContentUris.parseId(uri));
                    }
                }
                if (mUnrecognizedUirs.size() > 0) {
                    Toast.makeText(getContext(), R.string.unrecognized_numbers_in_uris, Toast.LENGTH_SHORT).show();
                }
                reallyStartLoading();
            }

        };
        String[] numbers = numbersToQuery.keySet().toArray(new String[numbersToQuery.size()]);
        queryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, numbers);
    }

    private Map<String, Uri> queryPhoneNumbersToPhoneUri(String[] numbers) {
        Map<String, Uri> result = new HashMap<String, Uri>(numbers.length);
        Context context = getContext();
        ContentResolver resolver = context == null ? null : context.getContentResolver();
        if (resolver == null) {
            Log.w(TAG, "queryPhoneNumbersToPhoneUri: got null resolver, context="+context);
            return result;
        }
        for (String number : numbers) {
            Uri uri = queryPhoneNumberToPhoneUri(number);
            if (uri != null) {
                result.put(number, uri);
            }
        }
        return result;
    }

    private static final String[] PHONE_NUMBER_NAME_PROJECTION = new String[] {
        "data_id",
    };
    private Uri queryPhoneNumberToPhoneUri(String number) {
        Uri queryUri = PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendEncodedPath(Uri.encode(number))
                .appendQueryParameter(Constants.PHONE_LOOKUP_QUERY_PARAM_IN_VISIBLE_CONTACTS, "true")
                .build();
        long id;
        Uri phoneUri = null;
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver().query(
                    queryUri,
                    PHONE_NUMBER_NAME_PROJECTION,
                    null,
                    null,
                    null);
            if ((cursor != null) && cursor.moveToFirst()) {
                id = cursor.getLong(0);
                phoneUri = ContentUris.withAppendedId(Phone.CONTENT_URI, id);
                return phoneUri;
            }
        } catch (SQLiteException sqle) {
            Log.e(TAG, "queryPhoneNumberToPhoneUri: got exception.", sqle);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

}
