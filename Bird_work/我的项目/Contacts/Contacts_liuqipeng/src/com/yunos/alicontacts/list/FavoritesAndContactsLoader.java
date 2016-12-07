/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.google.common.collect.Lists;
import com.yunos.alicontacts.preference.ContactsSettingActivity;

import yunos.support.v4.content.CursorLoader;

import java.util.List;

/**
 * This class defines method to load favorite contacts.
 * The favorite contacts contains the starred contacts and recommended contacts.
 * Because the limit of recommended contacts is different in this app and provider,
 * so we need to process the query result from provider.
 * The derived classes can use the loadFavoriteContacts() to get processed contacts cursor.
 * This class does NOT define the loadInBackground(),
 * the derived classes need to implement this method.
 */
public class FavoritesAndContactsLoader extends CursorLoader {

    private static final String TAG = "FavoritesAndContactsLoader";

    /**
     * The number of recommended contacts is up to 5.
     */
    public static final int MAX_NON_STARRED_FAVORITE_CONTACTS_COUNT = 5;

    protected String[] mProjection;
    protected Context mContext;
    protected boolean mSearchMode = false;
    private boolean mShowGroup;

    public FavoritesAndContactsLoader(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void setProjection(String[] projection) {
        super.setProjection(projection);
        mProjection = projection;
    }

    public void setShowGroup(boolean flag) {
        mShowGroup = flag;
    }

    public void setSearchMode(boolean searchMode) {
        mSearchMode = searchMode;
    }


    public Cursor cloneCursor(Cursor cursor,int cloneCount){
        if(cursor == null){
            return cursor;
        }
        MatrixCursor matrix = new MatrixCursor(mProjection);
        try{
            int count = cloneCount > cursor.getCount() ? cursor.getCount() : cloneCount;
            Object[] row = new Object[mProjection.length];
            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                for (int colIndex = 0; colIndex < row.length; colIndex++) {
                    row[colIndex] = cursor.getString(colIndex);
                }
                matrix.addRow(row);
            }

        }catch(Exception e){
            Log.e(TAG, "clone cursor error!",e);
        }
        return matrix;
    }

    @Override
    public Cursor loadInBackground() {
        //reset setFavoriteContactsOffset
        ContactEntryListFragment.setFavoriteContactsOffset(0);

        // First load the profile, if enabled.
        List<Cursor> cursors = Lists.newArrayList();

        // only load favorite contacts for phone contacts. If sim contacts is
        // only shown in main page, do not show favorite contacts
        boolean isShowFavoriteContacts = getIsShowFavoriteContactsFromContactSharedPreferences();
        if (isShowFavoriteContacts && !mSearchMode && !mShowGroup) {
            cursors.add(loadFavoriteContacts());
        }

        final Cursor contactsCursor = super.loadInBackground();
        if (cursors.isEmpty()) {
            return contactsCursor;
        }
        cursors.add(contactsCursor);
        return new MergeCursor(cursors.toArray(new Cursor[cursors.size()])) {
            @Override
            public Bundle getExtras() {
                // CursorLoader.loadInBackground()有可能返回null，
                // MergeCursor能够处理输入为null的情形，
                // 这里在输入null时返回空数据。
                if (contactsCursor == null) {
                    return Bundle.EMPTY;
                }
                // Need to get the extras from the contacts cursor.
                return contactsCursor.getExtras();
            }
        };
    }

    protected MatrixCursor loadFavoriteContacts() {
        Cursor cursor = null;
        int favoriteContactsCount = 0;
        boolean isAutoFavoriteContacts = getIsAutoFavoriteContactsFromContactSharedPreferences();
        if(isAutoFavoriteContacts){ //auto recommend favorite contacts
            cursor = makeAutoRecommendFavoriteContactsCursor();
            favoriteContactsCount = getAutoRecommendFavoriteContactsCount(cursor);
        } else {
            String selection = Contacts.STARRED + "=1 AND " + Contacts._ID + " IN visible_contacts";
            cursor = getContext().getContentResolver().query(Contacts.CONTENT_URI, mProjection,
                    selection, null, Contacts.SORT_KEY_PRIMARY);
            favoriteContactsCount = cursor == null ? 0 : cursor.getCount();
        }

        ContactEntryListFragment.setFavoriteContactsOffset(favoriteContactsCount);

        try {
            MatrixCursor matrix = new MatrixCursor(mProjection);
            Object[] row = new Object[mProjection.length];
            // do NOT go through all the record in the cursor,
            // we only need starred contacts + up to 5 auto recommended contacts
            // if the auto recommend option is turned on.
            for (int rowIndex = 0; rowIndex < favoriteContactsCount; rowIndex++) {
                cursor.moveToPosition(rowIndex);
                for (int colIndex = 0; colIndex < row.length; colIndex++) {
                    row[colIndex] = cursor.getString(colIndex);
                }
                matrix.addRow(row);
            }
            return matrix;
        } finally {
            if(cursor != null){
                cursor.close();
            }else{
                Log.w("BaseFavoritesAndContactsLoader",
                        "[LongTermTrackingLog] loadFavoriteContacts(), in finally, cursor = null");
            }
        }
    }

    private Cursor makeAutoRecommendFavoriteContactsCursor() {
        String[] projection = mProjection == null ? mProjection
                : new String[mProjection.length + 1];
        if (mProjection != null) {
            System.arraycopy(mProjection, 0, projection, 0, mProjection.length);
            projection[mProjection.length] = Contacts.STARRED;
        }

        return getContext().getContentResolver().query(Contacts.CONTENT_STREQUENT_URI, projection,
                null, null, null);
    }

    private int getAutoRecommendFavoriteContactsCount(Cursor cursor) {
        if(cursor == null){
            Log.w("BaseFavoritesAndContactsLoader",
                    "[LongTermTrackingLog] getAutoRecommendFavoriteContactsCount(), cursor = null");
            return 0;
        }
        int rowCount = cursor.getCount();
        if (rowCount <= MAX_NON_STARRED_FAVORITE_CONTACTS_COUNT) {
            return rowCount;
        }
        int starredColumnIndex = cursor.getColumnIndex(Contacts.STARRED);;

        // before use binary search, we have to know the first and last value.
        int lastStarred = 0;
        int firstUnStarred = rowCount - 1;
        cursor.moveToPosition(lastStarred);
        if (cursor.getInt(starredColumnIndex) == 0) {
            return MAX_NON_STARRED_FAVORITE_CONTACTS_COUNT;
        }
        cursor.moveToPosition(firstUnStarred);
        if (cursor.getInt(starredColumnIndex) != 0) {
            return rowCount;
        }
        // search the first not starred row.
        int checkRow, starred;
        while (true) {
            checkRow = (lastStarred + firstUnStarred) >> 1;
            cursor.moveToPosition(checkRow);
            starred = cursor.getInt(starredColumnIndex);
            if (starred == 0) {
                firstUnStarred = checkRow;
            } else {
                lastStarred = checkRow;
            }
            if ((lastStarred + 1) == firstUnStarred) {
                break;
            }
        }
        cursor.moveToPosition(-1);
        int effectiveRowCount = firstUnStarred + MAX_NON_STARRED_FAVORITE_CONTACTS_COUNT;
        if (effectiveRowCount > rowCount) {
            effectiveRowCount = rowCount;
        }
        return effectiveRowCount;
    }

    public boolean getIsShowFavoriteContactsFromContactSharedPreferences() {
        return ContactsSettingActivity.readBooleanFromDefaultSharedPreference(
                mContext,
                ContactsSettingActivity.CONTACT_DISPLAY_FAVORITE_PREFERENCE,
                ContactsSettingActivity.DEFAULT_DISPLAY_FAVORITE_ON_OFF);
    }

    public boolean getIsAutoFavoriteContactsFromContactSharedPreferences() {
        return ContactsSettingActivity.readBooleanFromDefaultSharedPreference(
                mContext,
                ContactsSettingActivity.CONTACT_AUTO_FAVORITE_PREFERENCE,
                ContactsSettingActivity.DEFAULT_AUTO_FAVORITE_ON_OFF);
    }

}
