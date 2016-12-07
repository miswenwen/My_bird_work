package com.yunos.alicontacts.platform;

import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract.Contacts.AggregationSuggestions;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.yunos.alicontacts.dialpad.calllog.CallLogQuery;

/**
 * This class holds some platform dependent utils.
 * For different platforms, e.g. platform 22 and platform 23,
 * some system APIs might have different signature, e.g. the number of parameters,
 * or the type of parameters.
 * So we need to call APIs with different parameters in different platforms.
 */
public final class PDUtils {

    private static final String TAG = "PDUtils";

    private PDUtils() {}

    public static boolean isPhoneIdle() {
        boolean idle = false;
        ITelephony phone = ITelephony.Stub.asInterface(
                ServiceManager.checkService(Context.TELEPHONY_SERVICE));
        if (phone != null) {
            try {
                idle = phone.isIdle(ActivityThread.currentPackageName());
            } catch (RemoteException re) {
                Log.e(TAG, "isPhoneIdle: got RemoteException.", re);
            }
        }
        return idle;
    }

    public static boolean isRadioOnForSubscriber(ITelephony tel, int subId) {
        boolean result = false;
        try {
            result = tel.isRadioOnForSubscriber(subId, ActivityThread.currentPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "isRadioOnForSubscriber: got exception.", e);
        }
        return result;
    }

    /**
     * Add a name to be used when searching for aggregation suggestions.
     * @param builder
     * @param name
     */
    public static void addNameToAggregationSuggestionsBuilder(
            AggregationSuggestions.Builder builder, String name) {
        builder.addNameParameter(name);
    }

    /**
     * In platform 22, PhoneAccountHandle.getId() returns subId for SIM card.
     * In platform 23, PhoneAccountHandle.getId() returns ICC id for SIM card.
     * We want subId here, so in platform 22, we return getId(),
     * and in platform 23, we need to call TelephonyManager.getSubIdForPhoneAccount().
     * @param context
     * @param account
     * @return
     */
    public static int getSubIdFromPhoneAccountHandle(Context context, PhoneAccountHandle account) {
        TelecomManager tcm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        TelephonyManager tem = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (tcm != null && tem != null) {
            PhoneAccount phoneAccount = tcm.getPhoneAccount(account);
            if (phoneAccount != null) {
                subId = tem.getSubIdForPhoneAccount(phoneAccount);
            }
        }
        Log.i(TAG, "getSubIdFromPhoneAccountHandle: account="+account+"; subId="+subId);
        return subId;
    }

    /**
     * In api level 23, the calls table from ContactsProvider has column photo_uri.
     * We return the column value in this case.
     * In api level 22, there is no photo_uri. We return null in this case.
     * @param c
     * @return
     */
    public static String getPhotoUriFromCallLogQuery(Cursor c) {
        return c.getString(CallLogQuery.CACHED_PHOTO_URI);
    }

    /**
     * Places a new outgoing call to the provided address using the system telecom service with
     * the specified extras.
     * adapter TelecomManger.placeCall method In platform 23
     * @param context
     * @param intent
     */
    public static void placeCall(Context context,Intent intent) {
        if(context == null || intent == null) return;
        final TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        tm.placeCall(intent.getData(), intent.getExtras());
    }

}
