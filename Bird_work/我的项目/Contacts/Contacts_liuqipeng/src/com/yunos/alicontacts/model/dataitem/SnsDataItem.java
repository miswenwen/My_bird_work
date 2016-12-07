/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.yunos.alicontacts.model.dataitem;

import android.content.ContentValues;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

import com.yunos.alicontacts.aliutil.provider.AliContactsContract.CommonDataKinds.Sns;
import com.yunos.alicontacts.model.RawContact;

/**
 * Represents an sns data item, wrapping the columns in
 * {@link ContactsContract.CommonDataKinds.Sns}.
 */
public class SnsDataItem extends DataItem {

    /* package */ SnsDataItem(RawContact rawContact, ContentValues values) {
        super(rawContact, values);
    }

    public String getDisplayName() {
        return getContentValues().getAsString(Contacts.DISPLAY_NAME);
    }

    public String getData() {
        return getContentValues().getAsString(Sns.DATA);
    }

    /**
     * Get UID for sns item.
     * @return uid
     */
    public String getUID() {
        return getContentValues().getAsString(Sns.UID);
    }

    /**
     * Values is one of Email.TYPE_*
     */
    public int getType() {
        return getContentValues().getAsInteger(Sns.TYPE);
    }

    public String getLabel() {
        return getContentValues().getAsString(Sns.LABEL);
    }
}
