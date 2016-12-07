/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.yunos.alicontacts.util;

import android.content.ComponentName;
import android.content.Context;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Methods to help extract {@code PhoneAccount} information from database and Telecom sources
 */
public class PhoneAccountUtils {
    /**
     * Generate account info from data in Telecom database
     */
    public static PhoneAccountHandle getAccount(String componentString,
            String accountId) {
        if (TextUtils.isEmpty(componentString) || TextUtils.isEmpty(accountId)) {
            return null;
        }
        final ComponentName componentName = ComponentName.unflattenFromString(componentString);
        return new PhoneAccountHandle(componentName, accountId);
    }

    /**
     * Generate account label from data in Telecom database
     */
    public static String getAccountLabel(Context context, PhoneAccountHandle phoneAccount) {
        final PhoneAccount account = getAccountOrNull(context, phoneAccount);
        if (account == null) {
            return null;
        }
        return account.getLabel().toString();
    }

    /**
     * Generate account number from data in Telecom database usually a number
     * associated with the account
     */
    public static String getAccountNumber(Context context, PhoneAccountHandle phoneAccount) {
        final PhoneAccount account = getAccountOrNull(context, phoneAccount);
        if (account == null) {
            return null;
        }
        return account.getAddress().getSchemeSpecificPart();
    }

    /**
     * Retrieve the account metadata, but if the account does not exist or the
     * device has only a single registered and enabled account, return null.
     */
    private static PhoneAccount getAccountOrNull(Context context, PhoneAccountHandle phoneAccount) {
        final TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        final PhoneAccount account = telecomManager.getPhoneAccount(phoneAccount);
        final boolean hasMultipleCallCapableAccounts = telecomManager.getCallCapablePhoneAccounts().size() > 1;
        if ((account == null) || (!hasMultipleCallCapableAccounts)) {
            return null;
        }
        return account;
    }

    /**
     * Retrieve the enabled account.
     */
    public static List<PhoneAccountHandle> getEnabledPhoneAccounts(Context context) {
        final TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        final List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
        return accounts;
    }

    /**
     * Retrieve the enabled Subscription account metadata.
     */
    public static List<PhoneAccountHandle> getEnabledSubscriptionAccounts(Context context) {
        final TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        List<PhoneAccountHandle> subs = new ArrayList<PhoneAccountHandle>();
        final List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle account : accounts) {
            if (isSubScriptionAccount(context, account)) {
                subs.add(account);
            }
        }
        return subs;
    }

    /**
     * returns whether the given PhoneAccountHandle belongs to a PSTN account,
     * which is associated with SIM subscription.
     */
    public static boolean isSubScriptionAccount(Context context, PhoneAccountHandle phoneAccount) {
        if (phoneAccount != null) {
            final TelecomManager telecomManager = (TelecomManager) context
                    .getSystemService(Context.TELECOM_SERVICE);
            final PhoneAccount account = telecomManager.getPhoneAccount(phoneAccount);
            if (account != null
                    && (account.getCapabilities() & PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) != 0) {
                return true;
            }
        }
        return false;
    }
}
