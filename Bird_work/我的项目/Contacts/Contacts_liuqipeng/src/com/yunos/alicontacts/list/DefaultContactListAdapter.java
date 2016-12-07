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

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.SearchSnippets;
import android.text.TextUtils;
import android.view.View;

import com.yunos.alicontacts.activities.PeopleActivity2;
import com.yunos.alicontacts.preference.ContactsPreferences;
import com.yunos.yundroid.widget.itemview.ItemView;

import yunos.support.v4.app.Fragment;
import yunos.support.v4.content.CursorLoader;

import java.util.ArrayList;
import java.util.List;
/// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
import android.provider.ContactsContract.RawContacts;
///@}
/**
 * A cursor adapter for the {@link ContactsContract.Contacts#CONTENT_TYPE} content type.
 */
public class DefaultContactListAdapter extends ContactListAdapter {
    private static final String TAG = "DefaultContactListAdapter";

    public static final char SNIPPET_START_MATCH = '\u0001';
    public static final char SNIPPET_END_MATCH = '\u0001';
    public static final String SNIPPET_ELLIPSIS = "\u2026";
    public static final int SNIPPET_MAX_TOKENS = 5;

    private String mGroupMembersIds;
    private boolean mShowGroupState;
    private boolean mAddGroupMemState;
    private Context mContext;
    public static final int FIRST_LOAD_COUNT = 20;
    public static final String SNIPPET_ARGS = SNIPPET_START_MATCH + "," + SNIPPET_END_MATCH + ","
            + SNIPPET_ELLIPSIS + "," + SNIPPET_MAX_TOKENS;

    public DefaultContactListAdapter(Context context) {
        this(context, null);
        mShowGroupState = false;
        mContext = context;
    }

    public DefaultContactListAdapter(Context context, Fragment fg) {
        super(context);
        //mFragment = fg;
        mShowGroupState = false;
        mContext = context;
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        if (loader instanceof FavoritesAndContactsLoader) {
            ((FavoritesAndContactsLoader) loader).setShowGroup(isShowGroupState());
            ((FavoritesAndContactsLoader) loader).setSearchMode(isSearchMode());
        }

        ContactListFilter filter = getFilter();
        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (TextUtils.isEmpty(query)) {
                // Regardless of the directory, we don't want anything returned,
                // so let's just send a "nothing" query to the local directory.
                loader.setUri(Contacts.CONTENT_URI);
                loader.setProjection(getProjection(false));
                loader.setSelection("0");
            } else {
                Builder builder = Contacts.CONTENT_FILTER_URI.buildUpon();
                builder.appendPath(query);      // Builder will encode the query
                builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(directoryId));
                if (directoryId != Directory.DEFAULT && directoryId != Directory.LOCAL_INVISIBLE) {
                    builder.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                            String.valueOf(getDirectoryResultLimit()));
                }
                builder.appendQueryParameter(SearchSnippets.SNIPPET_ARGS_PARAM_KEY,
                        SNIPPET_ARGS);
                builder.appendQueryParameter(SearchSnippets.DEFERRED_SNIPPETING_KEY,"1");
                loader.setUri(builder.build());
                loader.setProjection(getProjection(true));
            }
        } else {
            configureUri(loader, directoryId, filter);
            loader.setProjection(getProjection(false));
            configureSelection(loader, directoryId, filter);
        }

        String sortOrder;
        if (getSortOrder() == ContactsPreferences.SORT_ORDER_PRIMARY) {
            sortOrder = Contacts.SORT_KEY_PRIMARY;
        } else {
            sortOrder = Contacts.SORT_KEY_ALTERNATIVE;
        }

        loader.setSortOrder(sortOrder);

        updateLoaderSelections(loader);
    }

    protected void configureUri(CursorLoader loader, long directoryId, ContactListFilter filter) {
        Uri uri = Contacts.CONTENT_URI;
        if (filter != null && filter.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
            String lookupKey = getSelectedContactLookupKey();
            if (lookupKey != null) {
                uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey);
            } else {
                uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, getSelectedContactId());
            }
        }

        if (directoryId == Directory.DEFAULT && isSectionHeaderDisplayEnabled()) {
            uri = buildSectionIndexerUri(uri);
        }

        // The "All accounts" filter is the same as the entire contents of Directory.DEFAULT
        if (filter != null
                && filter.filterType != ContactListFilter.FILTER_TYPE_CUSTOM
                && filter.filterType != ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
            final Uri.Builder builder = uri.buildUpon();
            builder.appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT));
            if (filter.filterType == ContactListFilter.FILTER_TYPE_ACCOUNT) {
                filter.addAccountQueryParameterToUrl(builder);
            }
            uri = builder.build();
        }
        if (PeopleActivity2.sFirstLoad) {
            uri = uri.buildUpon().appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, String.valueOf(FIRST_LOAD_COUNT)).build();
        }
        loader.setUri(uri);
    }

    private void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter) {
        if (filter == null) {
            return;
        }

        if (directoryId != Directory.DEFAULT) {
            return;
        }

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<String>();

        switch (filter.filterType) {
            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS: {
                // We have already added directory=0 to the URI, which takes care of this
                // filter
                break;
            }
            case ContactListFilter.FILTER_TYPE_SINGLE_CONTACT: {
                // We have already added the lookup key to the URI, which takes care of this
                // filter
                break;
            }
            case ContactListFilter.FILTER_TYPE_STARRED: {
                selection.append(Contacts.STARRED).append("!=0");
                break;
            }
            case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY: {
                selection.append(Contacts.HAS_PHONE_NUMBER).append("=1");
                break;
            }
            case ContactListFilter.FILTER_TYPE_CUSTOM: {
                selection.append(Contacts.IN_VISIBLE_GROUP).append("=1");
                if (isCustomFilterForPhoneNumbersOnly()) {
                    selection.append(" AND ").append(Contacts.HAS_PHONE_NUMBER).append("=1");
                }
                break;
            }
            case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                // We use query parameters for account filter, so no selection to add here.
                break;
            }
        }
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[selectionArgs.size()]));
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, final int position) {
        ViewHolder viewHolder = (ViewHolder) itemView.getTag();
        // cursor for add group member and contact query do not have starred column.
        if (!mAddGroupMemState) {
            viewHolder.starred = cursor.getInt(cursor.getColumnIndex(Contacts.STARRED)) == 1;
            viewHolder.name = cursor.getString(ContactQuery.CONTACT_DISPLAY_NAME);

            /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
            viewHolder.sdn = cursor.getInt(ContactQuery.IS_SDN_CONTACT);
            /// @}
        }

        final ItemView view = (ItemView) viewHolder.view;
        if (TextUtils.isEmpty(viewHolder.name)) {
            viewHolder.name = mUnknownNameText.toString();
        }
        //view.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);
        /*if (isSelectionVisible()) {
            //view.setActivated(isSelectedContact(partition, cursor));
        }*/

        bindSectionHeaderAndDivider(view, position, cursor);

//        if (isQuickContactEnabled()) {//here isQuickContactEnabled() is always false,delete if.
//            bindQuickContact(view, partition, cursor, ContactQuery.CONTACT_PHOTO_ID,
//                    ContactQuery.CONTACT_PHOTO_URI, ContactQuery.CONTACT_ID,
//                    ContactQuery.CONTACT_LOOKUP_KEY);
//        } else {
//            if (getDisplayPhotos()) {
        bindPhoto(view, partition, cursor);
//            }
//        }

        bindName(view, cursor);
        bindPresenceAndStatusMessage(view, cursor);
        //aliyun os feature. if thie contact is starred, show starred icon.
        bindStarred(view, cursor);
        bindCheckBox(view, position);

        if (isSearchMode()) {
            bindSearchSnippet(view, cursor);
        } /*else {
            //view.setSnippet(null);
        }*/
    }

    private boolean isCustomFilterForPhoneNumbersOnly() {
        // TODO: this flag should not be stored in shared prefs.  It needs to be in the db.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean(ContactsPreferences.PREF_DISPLAY_ONLY_PHONES,
                ContactsPreferences.PREF_DISPLAY_ONLY_PHONES_DEFAULT);
    }

    public interface Listener {
        public abstract class Action {
            public static final int START_SLIDE_MODE= 0;
            public static final int STOP_SLIDE_MODE = 1;
        }
        void onAction(int action);
    }

    /*public void setListner(Listener l) {
        mListener = l;
    }*/

    public void setGroupMembersIds(String ids, boolean  show) {
        mShowGroupState = show;
        mGroupMembersIds = ids;
    }

    public void setShowGroupState(boolean  show) {
        mShowGroupState = show;
    }

    public boolean isShowGroupState() {
        return mShowGroupState;
    }

    public void updateAddGroupMemFilterIDs(String ids, boolean  show) {
        mAddGroupMemState = show;
        mGroupMembersIds = ids;
    }

    private void updateLoaderSelections(CursorLoader loader) {
        String selection = loader.getSelection();

        if (mShowGroupState) {
            if (!TextUtils.isEmpty(selection)) {
                selection += " AND " + Contacts._ID + " IN (" + mGroupMembersIds + ")";
            } else {
                selection = Contacts._ID + " IN (" + mGroupMembersIds + ")";
            }
        }

        if (mAddGroupMemState) {
            if (!TextUtils.isEmpty(selection)) {
                selection += " AND " + ContactsContract.RawContacts.ACCOUNT_TYPE + "!=\"com.android.contact.sim\" AND " + Contacts._ID + " NOT IN (" + mGroupMembersIds + ")";
            } else {
                selection = ContactsContract.RawContacts.ACCOUNT_TYPE + "!=\"com.android.contact.sim\" AND " + Contacts._ID + " NOT IN (" + mGroupMembersIds + ")";
            }
		}

        /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
        if(!bReadOnlyContactVisible){
            if (!TextUtils.isEmpty(selection)) {
                selection = "(" + selection + ") AND " + NOT_READ_ONLY_CONTACTS;
            } else {
                selection =  NOT_READ_ONLY_CONTACTS;
            }
		}
		///@}
        loader.setSelection(selection);
    }

    /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
    private static final String NOT_READ_ONLY_CONTACTS = RawContacts.IS_SDN_CONTACT + "!=-2";
    private boolean bReadOnlyContactVisible = true;
    public void setReadOnlyContactsVisibility(boolean visible){
        bReadOnlyContactVisible = visible;
    }
    /// @}
}
