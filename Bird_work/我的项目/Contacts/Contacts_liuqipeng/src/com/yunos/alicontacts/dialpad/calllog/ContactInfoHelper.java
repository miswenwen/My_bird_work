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

import android.annotation.NonNull;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.database.CallLogManager.CallLogChangeListener;
import com.yunos.alicontacts.database.tables.CallsTable;
import com.yunos.alicontacts.platform.PDConstants;
import com.yunos.alicontacts.util.AliTextUtils;
import com.yunos.alicontacts.util.ExpirableCache;
import com.yunos.alicontacts.util.ExpirableCache.CachedValue;

import java.util.LinkedList;

/**
 * Utility class to look up the contact information for a given number.
 */
public class ContactInfoHelper {
    private static final String TAG = "ContactInfoHelper";

    /** The size of the cache of contact info. */
    private static final int CONTACT_INFO_CACHE_SIZE = 100;

    private static ContactInfoHelper sInstance = null;
    private final Context mContext;

    private ContactInfoQueryThread mQueryThread = null;

    /**
     * <p>List of requests to update contact details.</p>
     * <p>Each request is made of a phone number to look up, and the contact info
     * currently stored in the call log for this number.
     * The requests are added when displaying the contacts and are processed
     * by a background thread.</p>
     */
    private final LinkedList<ContactInfoRequest> mContactInfoRequests
            = new LinkedList<ContactInfoRequest>();

    /**
     * A cache of the contact details for the phone numbers in the call log.
     * The content of the cache is expired (but not purged) whenever the contact data changes.
     * The key is number with the country in which the call was placed or received.
     */
    private ExpirableCache<NumberWithCountryIso, ContactInfo> mContactInfoCache
            = ExpirableCache.create(CONTACT_INFO_CACHE_SIZE);

    private ContactInfoHelper(Context context) {
        mContext = context;
    }

    /**
     * @param context The context will be referenced, so use ApplicationContext.
     * @return
     */
    public synchronized static ContactInfoHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ContactInfoHelper(context);
        }
        return sInstance;
    }

    /**
     * Returns the contact information for the given number.
     * <p>
     * If the number does not match any contact, returns a contact info containing only the number
     * and the formatted number.
     * <p>
     * If an error occurs during the lookup, it returns null.
     *
     * @param number the number to look up
     * @param countryIso the country associated with this number
     */
    public ContactInfo lookupNumber(String number, String countryIso) {
        final ContactInfo info;

        // Determine the contact info.
        if (PhoneNumberUtils.isUriNumber(number)) {
            // This "number" is really a SIP address.
            ContactInfo sipInfo = queryContactInfoForSipAddress(number);
            if (sipInfo == null || sipInfo == ContactInfo.EMPTY) {
                // Check whether the "username" part of the SIP address is
                // actually the phone number of a contact.
                String username = PhoneNumberUtils.getUsernameFromUriNumber(number);
                if (PhoneNumberUtils.isGlobalPhoneNumber(username)) {
                    sipInfo = queryContactInfoForPhoneNumber(username, countryIso);
                }
            }
            info = sipInfo;
        } else {
            // Look for a contact that has the given phone number.
            ContactInfo phoneInfo = queryContactInfoForPhoneNumber(number, countryIso);

            /*
             * by Ali.Xulun
             * Currently yunos don't support Internet call. Ignore it for speed optimization.
             *
            if (phoneInfo == null || phoneInfo == ContactInfo.EMPTY) {
                // Check whether the phone number has been saved as an "Internet call" number.
                phoneInfo = queryContactInfoForSipAddress(number);
            }*/
            info = phoneInfo;
        }

        final ContactInfo updatedInfo;
        if (info == null) {
            // The lookup failed.
            updatedInfo = null;
        } else {
            // If we did not find a matching contact, generate an empty contact info for the number.
            if (info == ContactInfo.EMPTY) {
                // Did not find a matching contact.
                updatedInfo = new ContactInfo();
                updatedInfo.number = number;
                updatedInfo.formattedNumber = formatPhoneNumber(number, null, countryIso);
            } else {
                updatedInfo = info;
            }
        }
        return updatedInfo;
    }

    /**
     * Looks up a contact using the given URI.
     * <p>
     * It returns null if an error occurs, {@link ContactInfo#EMPTY} if no matching contact is
     * found, or the {@link ContactInfo} for the given contact.
     * <p>
     * The {@link ContactInfo#formattedNumber} field is always set to {@code null} in the returned
     * value.
     */
    private ContactInfo lookupContactFromUri(Uri uri) {
        final ContactInfo info;
        Cursor phonesCursor =
                mContext.getContentResolver().query(
                        uri, PhoneQuery._PROJECTION, null, null, null);

        if (phonesCursor != null) {
            try {
                if (phonesCursor.moveToFirst()) {
                    info = new ContactInfo();
                    long contactId = phonesCursor.getLong(PhoneQuery.PERSON_ID);
                    String lookupKey = phonesCursor.getString(PhoneQuery.LOOKUP_KEY);
                    Uri lookupUri = Contacts.getLookupUri(contactId, lookupKey);
                    info.lookupUri = lookupUri == null ? null : lookupUri.toString();
                    info.name = phonesCursor.getString(PhoneQuery.NAME);
                    info.type = phonesCursor.getInt(PhoneQuery.PHONE_TYPE);
                    info.label = phonesCursor.getString(PhoneQuery.LABEL);
                    info.number = phonesCursor.getString(PhoneQuery.MATCHED_NUMBER);
                    info.normalizedNumber = phonesCursor.getString(PhoneQuery.NORMALIZED_NUMBER);
                    info.photoId = phonesCursor.getLong(PhoneQuery.PHOTO_ID);
                    info.photoUri = phonesCursor.getString(PhoneQuery.PHOTO_URI);
                    info.formattedNumber = null;
                } else {
                    info = ContactInfo.EMPTY;
                }
            } finally {
                phonesCursor.close();
            }
        } else {
            // Failed to fetch the data, ignore this request.
            info = null;
        }
        return info;
    }

    /**
     * Determines the contact information for the given SIP address.
     * <p>
     * It returns the contact info if found.
     * <p>
     * If no contact corresponds to the given SIP address, returns {@link ContactInfo#EMPTY}.
     * <p>
     * If the lookup fails for some other reason, it returns null.
     */
    private ContactInfo queryContactInfoForSipAddress(String sipAddress) {
        // "contactNumber" is a SIP address, so use the PhoneLookup table with the SIP parameter.
        Uri.Builder uriBuilder = PhoneLookup.CONTENT_FILTER_URI.buildUpon();
        uriBuilder.appendPath(Uri.encode(sipAddress));
        uriBuilder.appendQueryParameter(ContactsContract.PhoneLookup.QUERY_PARAMETER_SIP_ADDRESS, "1");
        return lookupContactFromUri(uriBuilder.build());
    }

    /**
     * Determines the contact information for the given phone number.
     * <p>
     * It returns the contact info if found.
     * <p>
     * If no contact corresponds to the given phone number, returns {@link ContactInfo#EMPTY}.
     * <p>
     * If the lookup fails for some other reason, it returns null.
     */
    private ContactInfo queryContactInfoForPhoneNumber(String number, String countryIso) {
        String contactNumber = number;
        if (!TextUtils.isEmpty(countryIso)) {
            // Normalize the number: this is needed because the PhoneLookup query below does not
            // accept a country code as an input.
            String numberE164 = PhoneNumberUtils.formatNumberToE164(number, countryIso);
            if (!TextUtils.isEmpty(numberE164)) {
                // Only use it if the number could be formatted to E164.
                contactNumber = numberE164;
            }
        }

        // The "contactNumber" is a regular phone number, so use the PhoneLookup table.
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contactNumber));
        ContactInfo info = lookupContactFromUri(uri);
        if (info != null && info != ContactInfo.EMPTY) {
            info.formattedNumber = formatPhoneNumber(number, null, countryIso);
        }
        return info;
    }

    /**
     * Format the given phone number
     *
     * @param number the number to be formatted.
     * @param normalizedNumber the normalized number of the given number.
     * @param countryIso the ISO 3166-1 two letters country code, the country's
     *        convention will be used to format the number if the normalized
     *        phone is null.
     *
     * @return the formatted number, or the given number if it was formatted.
     */
    private String formatPhoneNumber(String number, String normalizedNumber,
            String countryIso) {
        if (TextUtils.isEmpty(number)) {
            return "";
        }
        // If "number" is really a SIP address, don't try to do any formatting at all.
        if (PhoneNumberUtils.isUriNumber(number)) {
            return number;
        }
        return PhoneNumberUtils.formatNumber(number, normalizedNumber, countryIso);
    }

    public void expireAllContactInfoCache() {
        mContactInfoCache.expireAll();
    }

    /**
     * If we have no cached ContactInfo, then we will return null.
     * If we have a cached ContactInfo, we will return it even it is expired.
     * Most of the time, the expired value might be correct.
     * If there is no cached or the cache is expired, we will request for a new query.
     * @return The return value shall not be null.
     * If there is no cached value, then the localCallsContactInfo will be returned.
     */
    public ContactInfo getAndCacheContactInfo(String number, String countryIso, @NonNull ContactInfo localCallsContactInfo) {
        if (!PhoneNumberHelper.canPlaceCallsTo(number)) {
            Log.i(TAG, "getAndCacheContactInfo: invalid number:"+number);
            return localCallsContactInfo;
        }
        NumberWithCountryIso key = new NumberWithCountryIso(number, countryIso);
        CachedValue<ContactInfo> cached = mContactInfoCache.getCachedValue(key);
        ContactInfo cachedInfo = null;
        if (cached == null) {
            enqueueQueryContactInfoRequest(key, localCallsContactInfo);
        } else {
            if (cached.isExpired()) {
                enqueueQueryContactInfoRequest(key, localCallsContactInfo);
            }
            cachedInfo = cached.getValue();
        }
        if (cachedInfo == null) {
            return localCallsContactInfo;
        } else {
            return cachedInfo;
        }
    }

    /**
     * Queries the appropriate content provider for the contact associated with the number.
     * Upon completion it also updates the cache in the call log, if it is different
     * from {@code callLogInfo}.
     * The number might be either a SIP address or a phone number.
     * It returns true if it updated the content of the cache and we should therefore
     * tell the view to update its content.
     */
    private void queryContactInfo(@NonNull ContactInfoRequest req) {
        final ContactInfo info = lookupNumber(req.number.number, req.number.countryIso);
        Log.i(TAG, "queryContactInfo: req="+req+"; result="+info);

        putToContactInfoCache(req.number, info);
        updateCallLogContactInfoCache(req, info);
    }

    private void putToContactInfoCache(@NonNull NumberWithCountryIso key, ContactInfo info) {
        synchronized (mContactInfoCache) {
            mContactInfoCache.put(key, info);
        }
    }

    private void enqueueQueryContactInfoRequest(NumberWithCountryIso number, ContactInfo cachedInfo) {
        ContactInfoRequest req = new ContactInfoRequest(number, cachedInfo);
        synchronized (mContactInfoRequests) {
            mContactInfoRequests.add(req);
            mContactInfoRequests.notifyAll();
            if (mQueryThread == null) {
                mQueryThread = new ContactInfoQueryThread();
                mQueryThread.start();
            }
        }
    }

    /**
     * Stores the updated contact info in the call log if it is different from
     * the current one.
     */
    private void updateCallLogContactInfoCache(@NonNull ContactInfoRequest req, ContactInfo updatedInfo) {
        if (!PhoneNumberHelper.canPlaceCallsTo(req.number.number)) {
            return;
        }
        ContactInfo existingInfo = req.cachedInfo;
        if (existingInfo == null) {
            existingInfo = ContactInfo.EMPTY;
        }
        final ContentValues values = new ContentValues();
        boolean needsUpdate = false;

        if (!AliTextUtils.equalsLoosely(updatedInfo.name, existingInfo.name)) {
            values.put(Calls.CACHED_NAME, updatedInfo.name);
            needsUpdate = true;
        }

        if (updatedInfo.type != existingInfo.type) {
            values.put(Calls.CACHED_NUMBER_TYPE, updatedInfo.type);
            needsUpdate = true;
        }

        if (!AliTextUtils.equalsLoosely(updatedInfo.label, existingInfo.label)) {
            values.put(Calls.CACHED_NUMBER_LABEL, updatedInfo.label);
            needsUpdate = true;
        }
        if (!AliTextUtils.equalsLoosely(updatedInfo.lookupUri, existingInfo.lookupUri)) {
            values.put(Calls.CACHED_LOOKUP_URI, updatedInfo.lookupUri);
            needsUpdate = true;
        }
        if (!AliTextUtils.equalsLoosely(updatedInfo.normalizedNumber, existingInfo.normalizedNumber)) {
            values.put(Calls.CACHED_NORMALIZED_NUMBER, updatedInfo.normalizedNumber);
            needsUpdate = true;
        }
        if (!AliTextUtils.equalsLoosely(updatedInfo.number, existingInfo.number)) {
            values.put(Calls.CACHED_MATCHED_NUMBER, updatedInfo.number);
            needsUpdate = true;
        }
        if (updatedInfo.photoId != existingInfo.photoId) {
            values.put(Calls.CACHED_PHOTO_ID, updatedInfo.photoId);
            needsUpdate = true;
        }
        if (!AliTextUtils.equalsLoosely(updatedInfo.photoUri, existingInfo.photoUri)) {
            // The photo_uri column only exists in api level 23 and later.
            if (Build.VERSION.SDK_INT >= 23) {
                String photoUri = updatedInfo.photoUri == null ? null : updatedInfo.photoUri.toString();
                values.put(PDConstants.CALLS_TABLE_COLUMN_PHOTO_URI, photoUri);
            }
            needsUpdate = true;
        }
        // NOTE: do NOT compare formatted_number.
        // The formatted_number in local calls table has different meaning as remote calls table.
        // And the formatted_number is not used in display.

        if (!needsUpdate) {
            return;
        }

        try {
            String selection;
            String[] selectionArgs;
            String number = req.number.number;
            if (req.number.countryIso == null) {
                selection = "(" + Calls.NUMBER + " = ? OR " + Calls.CACHED_FORMATTED_NUMBER
                        + " = ? OR " + Calls.CACHED_NORMALIZED_NUMBER + " = ? " + ") AND "
                        + Calls.COUNTRY_ISO + " IS NULL";
                selectionArgs = new String[] { number, number, number };
            } else {
                selection = "(" + Calls.NUMBER + " = ? OR " + Calls.CACHED_FORMATTED_NUMBER
                        + " = ? OR " + Calls.CACHED_NORMALIZED_NUMBER + " = ? " + ") AND "
                        + Calls.COUNTRY_ISO + " = ?";
                selectionArgs = new String[] { number, number, number, req.number.countryIso };
            }

            // If we have only photo_uri changed in api level 22, we will have an empty values.
            // the update will report exception on empty values.
            // The values and the opBuilder have the same content,
            // so use the values.size() to check if the opBuilder has any update.
            if (values.size() > 0) {
                mContext.getContentResolver().update(
                        Calls.CONTENT_URI, values, selection, selectionArgs);
            }

            // Also update the calls table in local db here,
            // so that in next sync caused by remote calls change,
            // we might have nothing to write and avoid to notify data set change.
            // In api level 22, we do NOT have photo_uri in remote calls,
            // but we do have it in local calls, so try to put it in the values.
            if ((Build.VERSION.SDK_INT < 23)
                    && (!AliTextUtils.equalsLoosely(updatedInfo.photoUri, existingInfo.photoUri))) {
                values.put(CallsTable.COLUMN_PHOTO_URI,
                        updatedInfo.photoUri == null ? null : updatedInfo.photoUri.toString());
            }
            values.remove(Calls.CACHED_FORMATTED_NUMBER);
            if (values.size() > 0) {
                CallLogManager clm = CallLogManager.getInstance(mContext);
                clm.updateLocalCalls(values, selection, selectionArgs);
                clm.notifyCallsTableChange(CallLogChangeListener.CHANGE_PART_CONTACT_INFO);
            }
        } catch (SQLiteFullException sfe) {
            Log.e(TAG, "Storage full!", sfe);
        }
    }

    /**
     * <p>Stores a phone number of a call with the country code where it originally occurred.</p>
     * <p>Note the country does not necessarily specifies the country of the phone number itself,
     * but it is the country in which the user was in when the call was placed or received.</p>
     */
    private static class NumberWithCountryIso {
        public final String number;
        public final String countryIso;

        public NumberWithCountryIso(String number, String countryIso) {
            this.number = number;
            this.countryIso = countryIso;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NumberWithCountryIso)) {
                return false;
            }
            NumberWithCountryIso other = (NumberWithCountryIso) o;
            return AliTextUtils.equalsLoosely(number, other.number)
                    && AliTextUtils.equalsLoosely(countryIso, other.countryIso);
        }

        @Override
        public int hashCode() {
            return (number == null ? 0 : number.hashCode())
                    ^ (countryIso == null ? 0 : countryIso.hashCode());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(100);
            sb.append("NumberWithCountryIso:{number:\"").append(AliTextUtils.desensitizeNumber(number))
              .append("\"; countryIso:\"").append(countryIso).append("\"}");
            return sb.toString();
        }
    }

    /**
     * A request for contact details for the given number.
     */
    private static final class ContactInfoRequest {
        /** The number to look-up. */
        public final NumberWithCountryIso number;
        /** The cached contact information stored in the call log. */
        public final ContactInfo cachedInfo;

        public ContactInfoRequest(@NonNull NumberWithCountryIso number, @NonNull ContactInfo cachedInfo) {
            this.number = number;
            this.cachedInfo = cachedInfo;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ContactInfoRequest)) {
                return false;
            }
            ContactInfoRequest other = (ContactInfoRequest) obj;
            if (!number.equals(other.number)) {
                return false;
            }
            if (!cachedInfo.equals(other.cachedInfo)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + cachedInfo.hashCode();
            result = prime * result + number.hashCode();
            return result;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(200);
            sb.append("ContactInfoRequest:{number:").append(number.toString())
              .append("\"; cachedInfo:").append(cachedInfo.toString())
              .append('}');
            return sb.toString();
        }

    }

    /**
     * Handles requests for contact name and number type.
     * Queries the appropriate content provider for the contact associated with the number.
     * Upon completion it also updates the cache in the call log, if it is different
     * from {@code callLogInfo}.
     * The number might be either a SIP address or a phone number.
     * It returns true if it updated the content of the cache and we should therefore
     * tell the view to update its content.
     */
    private class ContactInfoQueryThread extends Thread {
        public ContactInfoQueryThread() {
            super("ContactInfoQueryThread");
        }

        @Override
        public void run() {
            while (true) {
                ContactInfoRequest req = null;
                synchronized (mContactInfoRequests) {
                    if (mContactInfoRequests.isEmpty()) {
                        try {
                            mContactInfoRequests.wait();
                        } catch (InterruptedException e) {
                            Log.i(TAG, "ContactInfoQueryThread interrupted during waiting next request.");
                        }
                    } else {
                        req = mContactInfoRequests.removeFirst();
                    }
                }

                if (req != null) {
                    queryContactInfo(req);
                }
            }
        }
    }

}
