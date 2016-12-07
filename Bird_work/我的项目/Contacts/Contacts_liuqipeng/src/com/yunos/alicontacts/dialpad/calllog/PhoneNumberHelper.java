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

import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.yunos.alicontacts.aliutil.android.telephony.AliCallerInfo;

/**
 * Helper for formatting and managing phone numbers.
 */
public class PhoneNumberHelper {

    /** Returns true if it is possible to place a call to the given number. */
    public static boolean canPlaceCallsTo(CharSequence number) {
        return !(TextUtils.isEmpty(number)
                || number.equals(AliCallerInfo.UNKNOWN_NUMBER)
                || number.equals(AliCallerInfo.PRIVATE_NUMBER)
                || number.equals(AliCallerInfo.PAYPHONE_NUMBER));
    }

    /** Returns true if it is possible to send an SMS to the given number. */
    public static boolean canSendSmsTo(CharSequence number) {
        return canPlaceCallsTo(number) && !isVoicemailNumber(number) && !isSipNumber(number);
    }

    /**
     * Returns true if the given number is the number of the configured voicemail.
     * To be able to mock-out this, it is not a static method.
     */
    public static boolean isVoicemailNumber(CharSequence number) {
        // return PhoneNumberUtils.isVoiceMailNumber(number.toString());
        return false;
    }

    /**
     * Returns true if the given number is a SIP address.
     * To be able to mock-out this, it is not a static method.
     */
    public static boolean isSipNumber(CharSequence number) {
        return PhoneNumberUtils.isUriNumber(number.toString());
    }
}
