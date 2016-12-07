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

package com.yunos.alicontacts;

import android.annotation.NonNull;
import android.net.Uri;
import android.provider.CallLog.Calls;

import com.yunos.alicontacts.dialpad.calllog.ContactInfo;
import com.yunos.alicontacts.dialpad.calllog.PhoneNumberInfo;
import com.yunos.alicontacts.util.AliTextUtils;

/**
 * The details of a phone call to be shown in the UI.
 */
public class PhoneCallDetails {
    /** The number of the other party involved in the call. */
    public final CharSequence number;
    /** The country corresponding with the phone number. */
    public final String countryIso;
    /**
     * The type of calls, as defined in the call log table, e.g.,
     * {@link Calls#INCOMING_TYPE}.
     */
    public final int callType;
    /**
     * Call features for VoLTE.
     */
    public final int callFeatures;
    /** The date of the call, in milliseconds since the epoch. */
    public final long date;
    /** The duration of the call in milliseconds, or 0 for missed calls. */
    public final long duration;
    // Recording call path, added by fangjun.lin
    public String mPhoneRecordPath;
    public final int slotId;
    public final Uri callUri;
    public final long ringTime;

    /** The contact info resolved from calls table. Can NOT be null. */
    public final ContactInfo contact;

    public final PhoneNumberInfo numberInfo;

    /** Create the details for a call with a number associated with a contact. */
    public PhoneCallDetails(Uri callUri, int slotId, CharSequence number,
            String countryIso, int callType, int callFeatures, long date, long duration, long ringTime,
            @NonNull ContactInfo contact, @NonNull PhoneNumberInfo numberInfo) {
        this.number = number;
        this.callUri = callUri;
        this.slotId = slotId;
        this.countryIso = countryIso;
        this.callType = callType;
        this.callFeatures = callFeatures;
        this.date = date;
        this.duration = duration;
        this.ringTime = ringTime;
        this.contact = contact;
        this.numberInfo = numberInfo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("PhoneCallDetails [number=").append(AliTextUtils.desensitizeNumber(number))
                .append(", countryIso=").append(countryIso)
                .append(", callType=").append(callType)
                .append(", callFeatures=").append(callFeatures)
                .append(", date=").append(date)
                .append(", duration=").append(duration)
                .append(", subId=").append(slotId)
                .append(", mPhoneRecordPath=").append(mPhoneRecordPath)
                .append(", contact=").append(contact)
                .append(", numberInfo=").append(numberInfo)
                .append(']');
        return sb.toString();
    }

}
