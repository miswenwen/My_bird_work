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

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Parsed form of the intent sent to the Contacts application.
 */
public class ContactsRequest implements Parcelable {

    /** Default mode: browse contacts */
    public static final int ACTION_DEFAULT = 10;

    /** Show all contacts */
    public static final int ACTION_ALL_CONTACTS = 15;

    /** Show all contacts with phone numbers */
    public static final int ACTION_CONTACTS_WITH_PHONES = 17;

    /** Show contents of a specific group */
    public static final int ACTION_GROUP = 20;

    /** Show all starred contacts */
    public static final int ACTION_STARRED = 30;

    /** Show frequently contacted contacts */
    public static final int ACTION_FREQUENT = 40;

    /** Show starred and the frequent */
    public static final int ACTION_STREQUENT = 50;

    /** Show all contacts and pick them when clicking */
    public static final int ACTION_PICK_CONTACT = 60;

    /** Show all contacts as well as the option to create a new one */
    public static final int ACTION_PICK_OR_CREATE_CONTACT = 70;

    /** Show all contacts and pick them for edit when clicking, and allow creating a new contact */
    public static final int ACTION_INSERT_OR_EDIT_CONTACT = 80;

    /** Show all phone numbers and pick them when clicking */
    public static final int ACTION_PICK_PHONE = 90;

    /** Show all postal addresses and pick them when clicking */
    public static final int ACTION_PICK_POSTAL = 100;

    /** Show all postal addresses and pick them when clicking */
    public static final int ACTION_PICK_EMAIL = 105;

    /** Show all contacts and create a shortcut for the picked contact */
    public static final int ACTION_CREATE_SHORTCUT_CONTACT = 110;

    /** Show all phone numbers and create a call shortcut for the picked number */
    public static final int ACTION_CREATE_SHORTCUT_CALL = 120;

    /** Show all phone numbers and create an SMS shortcut for the picked number */
    public static final int ACTION_CREATE_SHORTCUT_SMS = 130;

    /** Show all contacts and activate the specified one */
    public static final int ACTION_VIEW_CONTACT = 140;

    public static final int ACTION_VIEW_DIALPAD = 141;

    public static final int ACTION_PICK_MULTIPLE_CONTACT_TO_DELETE = 150;
    public static final int ACTION_PICK_MULTIPLE_PHONE_NUMBER = 152;
    public static final int ACTION_PICK_MULTIPLE_CONTACT_ADD_TO_GROUP = 153;
    public static final int ACTION_PICK_MULTIPLE_CONTACT_RM_FROM_GROUP = 154;
    public static final int ACTION_PICK_MULTIPLE_PHONE_NUMBER_IN_GOURP = 155;
/*YunOS BEGIN PB*/
//##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
//##BugID:(8466294) ##date:2016-7-22 09:00
//##description:suppot export some contacts to vcard
    public static final int ACTION_PICK_MULTIPLE_CONTACT_TO_EXPORT = ACTION_PICK_MULTIPLE_PHONE_NUMBER_IN_GOURP + 1;
/*YUNOS END PB*/
    
    private boolean mValid = true;
    private int mActionCode = ACTION_DEFAULT;
    private Intent mRedirectIntent;
    private CharSequence mTitle;
    private boolean mSearchMode;
    private String mQueryString;
    //private boolean mLegacyCompatibilityMode;
    private boolean mDirectorySearchEnabled = true;
    private Uri mContactUri;
    private Parcelable[] mContactUriArray;
    private long mGroupID;
    private String[] mGroupMemIDs;
    private int mPosition;
    //private String mCityGroupName;

    @Override
    public String toString() {
        return "{ContactsRequest:mValid=" + mValid
                + " mActionCode=" + mActionCode
                + " mRedirectIntent=" + mRedirectIntent
                + " mTitle=" + mTitle
                + " mSearchMode=" + mSearchMode
                + " mQueryString=" + mQueryString
                //+ " mLegacyCompatibilityMode=" + mLegacyCompatibilityMode
                + " mDirectorySearchEnabled=" + mDirectorySearchEnabled
                + " mContactUri=" + mContactUri
                + " mPosition=" + mPosition
                + "}";
    }

    /**
     * Copies all fields.
     */
    public void copyFrom(ContactsRequest request) {
        mValid = request.mValid;
        mActionCode = request.mActionCode;
        mRedirectIntent = request.mRedirectIntent;
        mTitle = request.mTitle;
        mSearchMode = request.mSearchMode;
        mQueryString = request.mQueryString;
        //mLegacyCompatibilityMode = request.mLegacyCompatibilityMode;
        mDirectorySearchEnabled = request.mDirectorySearchEnabled;
        mContactUri = request.mContactUri;
        mPosition = request.mPosition;
    }

    public static Parcelable.Creator<ContactsRequest> CREATOR = new Creator<ContactsRequest>() {

        @Override
        public ContactsRequest[] newArray(int size) {
            return new ContactsRequest[size];
        }

        @Override
        public ContactsRequest createFromParcel(Parcel source) {
            ClassLoader classLoader = this.getClass().getClassLoader();
            ContactsRequest request = new ContactsRequest();
            request.mValid = source.readInt() != 0;
            request.mActionCode = source.readInt();
            request.mRedirectIntent = source.readParcelable(classLoader);
            request.mTitle = source.readString();
            request.mSearchMode = source.readInt() != 0;
            request.mQueryString = source.readString();
            //request.mLegacyCompatibilityMode  = source.readInt() != 0;
            request.mDirectorySearchEnabled = source.readInt() != 0;
            request.mContactUri = source.readParcelable(classLoader);
            request.mPosition = source.readInt();
            return request;
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mValid ? 1 : 0);
        dest.writeInt(mActionCode);
        dest.writeParcelable(mRedirectIntent, 0);
        dest.writeString(mTitle.toString());
        dest.writeInt(mSearchMode ? 1 : 0);
        dest.writeString(mQueryString);
        //dest.writeInt(mLegacyCompatibilityMode ? 1 : 0);
        dest.writeInt(mDirectorySearchEnabled ? 1 : 0);
        dest.writeParcelable(mContactUri, 0);
        dest.writeInt(mPosition);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isValid() {
        return mValid;
    }

    public void setValid(boolean flag) {
        mValid = flag;
    }

    public Intent getRedirectIntent() {
        return mRedirectIntent;
    }

    public void setRedirectIntent(Intent intent) {
        mRedirectIntent = intent;
    }

    public void setActivityTitle(CharSequence title) {
        mTitle = title;
    }

    public CharSequence getActivityTitle() {
        return mTitle;
    }

    public int getActionCode() {
        return mActionCode;
    }

    public void setActionCode(int actionCode) {
        mActionCode = actionCode;
    }

    public boolean isSearchMode() {
        return mSearchMode;
    }

    public void setSearchMode(boolean flag) {
        mSearchMode = flag;
    }

    public String getQueryString() {
        return mQueryString;
    }

    public void setQueryString(String string) {
        mQueryString = string;
    }

    /*public boolean isLegacyCompatibilityMode() {
        return mLegacyCompatibilityMode;
    }

    public void setLegacyCompatibilityMode(boolean flag) {
        mLegacyCompatibilityMode = flag;
    }*/

    /**
     * Determines whether this search request should include directories or
     * is limited to local contacts only.
     */
    public boolean isDirectorySearchEnabled() {
        return mDirectorySearchEnabled;
    }

    public void setDirectorySearchEnabled(boolean flag) {
        mDirectorySearchEnabled = flag;
    }

    public Uri getContactUri() {
        return mContactUri;
    }

    public void setContactUri(Uri contactUri) {
        this.mContactUri = contactUri;
    }

    public Parcelable[] getContactUriArray() {
        return mContactUriArray;
    }

    public void setContactUriArray(Parcelable[] contactUriArray) {
        this.mContactUriArray = contactUriArray;
    }

    public long getGroupID() {
        return mGroupID;
    }

    public void setGroupID(long groupID) {
        this.mGroupID = groupID;
    }

    public String[] getGroupMemIDs() {
        return mGroupMemIDs;
    }

    public void setGroupMemIDs(String[] ids) {
        this.mGroupMemIDs = ids;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        this.mPosition = position;
    }
}
