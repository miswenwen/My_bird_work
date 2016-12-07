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
 * limitations under the License
 */
package com.yunos.alicontacts;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;

import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.model.account.AccountWithDataSet;

import yunos.support.v4.content.CursorLoader;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Group meta-data loader. Loads all groups or just a single group from the
 * database (if given a {@link Uri}).
 */
public final class GroupMetaDataLoader extends CursorLoader {

    public static final String GROUPS_SORT_ORDER = Groups._ID + " COLLATE LOCALIZED ASC";

    private final static String[] COLUMNS = new String[] {
        Groups.ACCOUNT_NAME,
        Groups.ACCOUNT_TYPE,
        Groups.DATA_SET,
        Groups._ID,
        Groups.TITLE,
        Groups.GROUP_IS_READ_ONLY,
    };

    public final static int ACCOUNT_NAME = 0;
    public final static int ACCOUNT_TYPE = 1;
    public final static int DATA_SET = 2;
    public final static int GROUP_ID = 3;
    public final static int TITLE = 4;
    public final static int IS_READ_ONLY = 5;

    public static final class LoadedGroup {
        public final String mAccountName;
        public final String mAccountType;
        public final String mDataSet;
        public final long mId;
        public final String mTitle;
        public final boolean mReadOnly;

        public LoadedGroup(
                String accountName, String accountType, String dataSet,
                long id, String title,
                boolean readonly) {
            mAccountName = accountName;
            mAccountType = accountType;
            mDataSet = dataSet;
            mId = id;
            mTitle = title;
            mReadOnly = readonly;
        }

        public static LoadedGroup createInstanceFromCursor(Cursor cursor) {
            return new LoadedGroup(
                    cursor.getString(ACCOUNT_NAME),
                    cursor.getString(ACCOUNT_TYPE),
                    cursor.getString(DATA_SET),
                    cursor.getLong(GROUP_ID),
                    cursor.getString(TITLE),
                    (!cursor.isNull(IS_READ_ONLY)) && (cursor.getInt(IS_READ_ONLY) != 0));
        }

        public static ArrayList<LoadedGroup> createArrayFromCursor(Cursor cursor) {
            ArrayList<LoadedGroup> result = new ArrayList<LoadedGroup>();
            int count = cursor == null ? -1 : cursor.getCount();
            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                result.add(createInstanceFromCursor(cursor));
            }
            return result;
        }

        public static ArrayList<LoadedGroup> filterAccountFromCursor(Cursor cursor, AccountWithDataSet account) {
            ArrayList<LoadedGroup> result = createArrayFromCursor(cursor);
            Iterator<LoadedGroup> iter = result.iterator();
            while (iter.hasNext()) {
                LoadedGroup group = iter.next();
                if (!group.matchAccount(account)) {
                    iter.remove();
                }
            }
            return result;
        }

        public boolean matchAccount(AccountWithDataSet account) {
            boolean same = TextUtils.equals(mAccountName, account.name)
                    && TextUtils.equals(mAccountType, account.type)
                    && TextUtils.equals(mDataSet, account.dataSet);
            if (same) {
                return true;
            }
            // consider local account and yunos account are the same,
            // so local account contacts can share groups with yunos account contacts.
            if (AccountType.LOCAL_ACCOUNT_TYPE.equals(account.type)
                    && AccountType.YUNOS_ACCOUNT_TYPE.equals(mAccountType)) {
                return true;
            }
            if (AccountType.YUNOS_ACCOUNT_TYPE.equals(account.type)
                    && AccountType.LOCAL_ACCOUNT_TYPE.equals(mAccountType)) {
                return true;
            }
            return false;
        }

    }

    public GroupMetaDataLoader(Context context, Uri groupUri) {
//        super(context, ensureIsGroupUri(groupUri), COLUMNS, Groups.ACCOUNT_TYPE + " NOT NULL AND "
//                + Groups.ACCOUNT_NAME + " NOT NULL", null, null);
        super(context, ensureIsGroupUri(groupUri), COLUMNS, Groups.DELETED + "=0", null, null);
        setSortOrder(GROUPS_SORT_ORDER);
    }

    /**
     * Ensures that this is a valid group URI. If invalid, then an exception is
     * thrown. Otherwise, the original URI is returned.
     */
    private static Uri ensureIsGroupUri(final Uri groupUri) {
        // TODO: Fix ContactsProvider2 getType method to resolve the group Uris
        if (groupUri == null) {
            throw new IllegalArgumentException("Uri must not be null");
        }
        if (!groupUri.toString().startsWith(Groups.CONTENT_URI.toString())) {
            throw new IllegalArgumentException("Invalid group Uri: " + groupUri);
        }
        return groupUri;
    }
}
