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
 * limitations under the License
 */

package com.yunos.alicontacts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.yunos.alicontacts.platform.PDConstants;
import com.yunos.alicontacts.platform.PDUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.PhoneAccountUtils;

import java.util.List;
import java.util.Locale;

/**
 * Utilities related to calls.
 */
public class CallUtil {

    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_MAILTO = "mailto";

    private static final String TAG = "CallUtil";

    private static final int DIAL_NUMBER_INTENT_NORMAL = 0;
    private static final int DIAL_NUMBER_INTENT_IP = 1;
    private static final int DIAL_NUMBER_INTENT_VIDEO = 2;

    /// M: VOLTE IMS Call feature.
    private static final int DIAL_NUMBER_INTENT_IMS = 4;

    public static final String EXTRA_IS_VIDEO_CALL = "com.android.phone.extra.video";
    public static final String EXTRA_IS_IP_DIAL = "com.android.phone.extra.ip";
    /// M: VOLTE IMS Call feature.
    public static final String EXTRA_IS_IMS_CALL = "com.mediatek.phone.extra.ims";

    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";

    /**
     * Return an Intent for making a phone call. Scheme (e.g. tel, sip) will be determined
     * automatically.
     */
    public static Intent getCallIntent(Context context, String number) {
        return getCallIntent(context, number, null, null);
    }

    /**
     * Return an Intent for making a phone call. A given Uri will be used as is (without any
     * sanity check).
     */
    public static Intent getCallIntent(Context context, Uri uri) {
        return getCallIntent(context, uri, null, null);
    }

    /**
     * A variant of {@link #getCallIntent(String)} but also accept a call origin.
     * For more information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(Context context, String number, String callOrigin) {
        return getCallIntent(context, getCallUri(number), callOrigin, null);
    }

    /**
     * A variant of {@link #getCallIntent(String)} but also include {@code Account}.
     */
    public static Intent getCallIntent(Context context, String number, PhoneAccountHandle accountHandle) {
        return getCallIntent(context, number, null, accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also include {@code Account}.
     */
    public static Intent getCallIntent(Context context, Uri uri, PhoneAccountHandle accountHandle) {
        return getCallIntent(context, uri, null, accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(String, String)} but also include {@code Account}.
     */
    public static Intent getCallIntent(
            Context context, String number, String callOrigin, PhoneAccountHandle accountHandle) {
        return getCallIntent(context, getCallUri(number), callOrigin, accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also accept a call
     * origin and {@code Account}.
     * For more information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(
            Context context, Uri uri, String callOrigin, PhoneAccountHandle accountHandle) {
        return getCallIntent(context, uri, callOrigin, accountHandle,
                PDConstants.VIDEO_PROFILE_STATE_AUDIO_ONLY);
    }

    /**
     * A variant of {@link #getCallIntent(String, String)} for starting a video call.
     */
    public static Intent getVideoCallIntent(Context context, String number, String callOrigin) {
        return getCallIntent(context, getCallUri(number), callOrigin, null,
                PDConstants.VIDEO_PROFILE_STATE_BIDIRECTIONAL);
    }

    /**
     * A variant of {@link #getCallIntent(String, String, android.telecom.PhoneAccountHandle)} for
     * starting a video call.
     */
    public static Intent getVideoCallIntent(
            Context context, String number, String callOrigin, PhoneAccountHandle accountHandle) {
        return getCallIntent(context, getCallUri(number), callOrigin, accountHandle,
                PDConstants.VIDEO_PROFILE_STATE_BIDIRECTIONAL);
    }

    /**
     * A variant of {@link #getCallIntent(String, String, android.telecom.PhoneAccountHandle)} for
     * starting a video call.
     */
    public static Intent getVideoCallIntent(Context context, String number, PhoneAccountHandle accountHandle) {
        return getVideoCallIntent(context, number, null, accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also accept a call
     * origin and {@code Account} and {@code VideoCallProfile} state.
     * For more information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(Context context, Uri uri, String callOrigin, PhoneAccountHandle accountHandle,
            int videoState) {
        final Intent intent = new Intent(Intent.ACTION_CALL, uri);
        intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, videoState);
        if (callOrigin != null) {
            intent.putExtra(EXTRA_CALL_ORIGIN, callOrigin);
        }
        if (accountHandle != null) {
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
        } else {
            if (context == null) {
                Log.e(TAG, "getCallIntent() context is NULL!!!");
                return intent;
            }

            int defaultVoiceSlotId = SimUtil.getDefaultVoiceSlotId();
            if (defaultVoiceSlotId != SimUtil.SIM_DEFAULT_NO_SET && defaultVoiceSlotId != SimUtil.SIM_DEFAULT_ALWAYS_ASK) {
                int subId = SimUtil.INVALID_SUB_ID;
                if (SimUtil.MULTISIM_ENABLE) {
                    subId = SimUtil.getSubId(defaultVoiceSlotId);
                } else {
                    subId = SimUtil.getDefaultSubId();
                }

                if (subId != SimUtil.INVALID_SUB_ID) {
                    final List<PhoneAccountHandle> accounts = PhoneAccountUtils.getEnabledSubscriptionAccounts(context);
                    for (PhoneAccountHandle account : accounts) {
                        int accountId = PDUtils.getSubIdFromPhoneAccountHandle(context, account);
                        Log.d(TAG, "getCallIntent() accountId:" + accountId + ", subId:" + subId + ", defaultVoiceSlotId:"
                                + defaultVoiceSlotId + ", uri:" + uri);

                        if (accountId == subId) {
                            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account);
                            break;
                        }
                    }
                }
            }
        }

        return intent;
    }

    public static Intent getCallIntent(Context context, Uri uri, String callOrigin, int type) {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (callOrigin != null) {
            intent.putExtra(EXTRA_CALL_ORIGIN, callOrigin);
        }
        if ((type & DIAL_NUMBER_INTENT_IP) != 0) {
            intent.putExtra(EXTRA_IS_IP_DIAL, true);
        }

        if ((type & DIAL_NUMBER_INTENT_VIDEO) != 0) {
            intent.putExtra(EXTRA_IS_VIDEO_CALL, true);
        }

        /** M: VOLTE IMS Call feature. @{ */
        if ((type & DIAL_NUMBER_INTENT_IMS) != 0) {
            Log.d(TAG, "VOLTE Ims Call put extra 'com.mediatek.phone.extra.ims' true.");
            intent.putExtra(EXTRA_IS_IMS_CALL, true);
        }
        /** @} */
        return intent;
    }

    /**
     * Return Uri with an appropriate scheme, accepting both SIP and usual phone call
     * numbers.
     */
    public static Uri getCallUri(String number) {
        if (PhoneNumberUtils.isUriNumber(number)) {
             return Uri.fromParts(PhoneAccount.SCHEME_SIP, number, null);
        }
        return Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
     }

    public static String getUsernameFromUriNumber(String number) {
        // The delimiter between username and domain name can be
        // either "@" or "%40" (the URI-escaped equivalent.)
        int delimiterIndex = number.indexOf('@');
        if (delimiterIndex < 0) {
            delimiterIndex = number.indexOf("%40");
        }
        if (delimiterIndex < 0) {
            Log.w(TAG,
            "getUsernameFromUriNumber: no delimiter found in SIP addr '" + number + "'");
            return number;
        }
        return number.substring(0, delimiterIndex);
    }

    /**
    * @return The ISO 3166-1 two letters country code of the country the user
    *         is in based on the network location. If the network location does not exist, fall
    *         back to the locale setting.
    */
    public static String getCurrentCountryIso(Context context, Locale locale) {
         // Without framework function calls, this seems to be the most accurate location service
         // we can rely on.
         final TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
         String countryIso = telephonyManager.getNetworkCountryIso().toUpperCase();

         if (countryIso == null) {
             countryIso = locale.getCountry();
             Log.w(TAG, "No CountryDetector; falling back to countryIso based on locale: "
                             + countryIso);
         }
         return countryIso;
    }

}
