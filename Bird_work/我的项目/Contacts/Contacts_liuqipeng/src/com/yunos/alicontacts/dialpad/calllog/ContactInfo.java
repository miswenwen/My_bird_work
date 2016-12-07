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

package com.yunos.alicontacts.dialpad.calllog;

import android.database.Cursor;
import android.text.TextUtils;

import com.yunos.alicontacts.database.CallDetailQuery;
import com.yunos.alicontacts.util.AliTextUtils;

/**
 * Information for a contact as needed by the Call Log.
 */
public final class ContactInfo {
    public String lookupUri;
    public String name;
    public int type;
    public String label;
    /** The number field is matched_number column in calls db. */
    public String number;
    public String formattedNumber;
    public String normalizedNumber;
    /** The photo for the contact, if available. */
    public long photoId;
    /** The high-res photo for the contact, if available. */
    public String photoUri;

    public final static ContactInfo EMPTY = new ContactInfo();

    @Override
    public int hashCode() {
        // Uses only name and contactUri to determine hashcode.
        // This should be sufficient to have a reasonable distribution of hash codes.
        // Moreover, there should be no two people with the same lookupUri.
        final int prime = 31;
        int result = 1;
        result = prime * result + ((number == null) ? 0 : number.hashCode());
        result = prime * result + ((lookupUri == null) ? 0 : lookupUri.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ContactInfo)) return false;
        ContactInfo other = (ContactInfo) obj;
        if (!TextUtils.equals(number, other.number)) return false;
        if (!TextUtils.equals(lookupUri, other.lookupUri)) return false;
        if (!TextUtils.equals(name, other.name)) return false;
        if (type != other.type) return false;
        if (!TextUtils.equals(label, other.label)) return false;
        if (!TextUtils.equals(formattedNumber, other.formattedNumber)) return false;
        if (!TextUtils.equals(normalizedNumber, other.normalizedNumber)) return false;
        if (photoId != other.photoId) return false;
        if (!TextUtils.equals(photoUri, other.photoUri)) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("ContactInfo:{number=\"").append(AliTextUtils.desensitizeNumber(number))
          .append("\"; lookupUri=\"").append(lookupUri)
          .append("\"; name=\"").append(name)
          .append("\"; type=").append(type)
          .append("; label=\"").append(label)
          .append("\"; photoId=").append(photoId)
          .append("; photoUri=\"").append(photoUri)
          .append("\"}");
        return sb.toString();
    }

    public static ContactInfo fromLocalCallsCursor(Cursor c) {
        ContactInfo info = new ContactInfo();
        info.number = c.getString(CallerViewQuery.MATCHED_NUM);
        info.lookupUri = c.getString(CallerViewQuery.LOOKUP_URI);
        info.name = c.getString(CallerViewQuery.NAME);
        info.type = c.getInt(CallerViewQuery.NUMBER_TYPE);
        info.label = c.getString(CallerViewQuery.NUMBER_LABEL);
        info.photoId = c.getLong(CallerViewQuery.PHOTO_ID);
        info.photoUri = c.getString(CallerViewQuery.PHOTO_URI);
        info.normalizedNumber = c.getString(CallerViewQuery.NORMALIZED_NUM);
        info.formattedNumber = c.getString(CallerViewQuery.FORMATTED_NUM);
        return info;
    }

    public static ContactInfo fromLocalCallDetailCursor(Cursor c) {
        ContactInfo info = new ContactInfo();
        info.number = c.getString(CallDetailQuery.CONTACT_MATCHED_NUM_COLUMN_INDEX);
        info.lookupUri = c.getString(CallDetailQuery.CONTACT_LOOKUP_URI_COLUMN_INDEX);
        info.name = c.getString(CallDetailQuery.CONTACT_NAME_COLUMN_INDEX);
        info.type = c.getInt(CallDetailQuery.CONTACT_NUMBER_TYPE_COLUMN_INDEX);
        info.label = c.getString(CallDetailQuery.CONTACT_NUMBER_LABEL_COLUMN_INDEX);
        info.photoId = c.getLong(CallDetailQuery.CONTACT_PHOTO_ID_COLUMN_INDEX);
        info.photoUri = c.getString(CallDetailQuery.CONTACT_PHOTO_URI_COLUMN_INDEX);
        info.normalizedNumber = c.getString(CallDetailQuery.NORMALIZED_NUM_COLUMN_INDEX);
        info.formattedNumber = c.getString(CallDetailQuery.FORMATTED_NUM_COLUMN_INDEX);
        return info;
    }

}
