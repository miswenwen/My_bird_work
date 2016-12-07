
package com.yunos.alicontacts.dialpad.calllog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.aliutil.android.telephony.AliCallerInfo;
import com.yunos.alicontacts.database.util.NumberNormalizeUtil;
import com.yunos.alicontacts.platform.PDUtils;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.AliTextUtils;
import com.yunos.alicontacts.util.PhoneAccountUtils;
import com.yunos.common.UsageReporter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AliCallLogExtensionHelper {

    private static final String TAG = "AliCallLogExtensionHelper";

    private static final boolean DEBUG = false;

    public static final boolean PLATFORM_YUNOS = SimUtil.IS_YUNOS;

    private static final int COMMON_SIM_1 = SimUtil.SLOT_ID_1;
    private static final int COMMON_SIM_2 = SimUtil.SLOT_ID_2;

    public static void log(String Tag, String logContent) {
        if (DEBUG) {
            Log.d(Tag, logContent);
        }
    }

    /**
     * @param callType
     * @param simId 0:card1 1:card2
     * @return
     */
    public static int getCallTypeImageid(int callType, int simId) {
        int tempImageId = -1;
        if (simId == -1) {
            switch (callType) {
                case Calls.INCOMING_TYPE:
                    tempImageId = R.drawable.calllog_type_in;
                    break;
                case Calls.MISSED_TYPE:
                    tempImageId = R.drawable.calllog_type_missed;
                    break;
                case Calls.OUTGOING_TYPE:
                    tempImageId = R.drawable.calllog_type_out;
                    break;
                default:
                    Log.e(TAG, "unknown call type " + callType);
                    break;
            }
        } else {
            switch (callType) {
                case Calls.INCOMING_TYPE:
                    tempImageId = simId == 0 ? R.drawable.calllog_double_1_type_in : R.drawable.calllog_double_2_type_in;
                    break;
                case Calls.MISSED_TYPE:
                    tempImageId = simId == 0 ? R.drawable.calllog_double_1_type_missed
                            : R.drawable.calllog_double_2_type_missed;
                    break;
                case Calls.OUTGOING_TYPE:
                    tempImageId = simId == 0 ? R.drawable.calllog_double_1_type_out : R.drawable.calllog_double_2_type_out;
                    break;
                default:
                    Log.e(TAG, "unknown call type " + callType);
                    break;
            }
        }

        return tempImageId;
    }

    public static String formatCallTypeLabel(Context context, String dualCardFormat, String defaultFormat, int simId) {
        String label = "";

        if (simId == -1) {
            if (defaultFormat == null) {
                label = String.format(dualCardFormat, "");
            } else {
                label = defaultFormat;
            }
        } else if (simId == COMMON_SIM_1) {
            label = String.format(dualCardFormat, context.getString(R.string.call_sim_1));
        } else if (simId == COMMON_SIM_2) {
            label = String.format(dualCardFormat, context.getString(R.string.call_sim_2));
        }

        return label;
    }

    public static String formatCallTypeLabel(Context context, int simId, String ringTimeFormat, long ring_time) {
        StringBuilder label = new StringBuilder();

        if (simId == -1) {
        } else if (simId == COMMON_SIM_1) {
            label.append(context.getString(R.string.call_sim_1));
        } else if (simId == COMMON_SIM_2) {
            label.append(context.getString(R.string.call_sim_2));
        }

        label.append(String.format(ringTimeFormat, String.valueOf(ring_time)));

        return label.toString();
    }

    public static String formatCallTypeLabel(Context context, String ringTimeFormat, int simId, long ring_time) {
        StringBuilder label = new StringBuilder();
        String simCard = "";

        if (simId == -1) {
            simCard = "";
        } else if (simId == COMMON_SIM_1) {
            simCard = context.getString(R.string.call_sim_1);
        } else if (simId == COMMON_SIM_2) {
            simCard = context.getString(R.string.call_sim_2);
        }

        label.append(String.format(ringTimeFormat, simCard, String.valueOf(ring_time)));

        return label.toString();
    }

    public static String formatCallTypeLabel(Context context, String ringTimeFormat, int simId) {
        StringBuilder label = new StringBuilder();
        label.append(String.format(ringTimeFormat, String.valueOf(simId)));

        return label.toString();
    }

    public static CharSequence dateFormat(CharSequence formate, long time) {
        return DateFormat.format(formate, new Date(time));
    }

    public static CharSequence dateFormat(CharSequence dateFormat, CharSequence timeFormat, long time) {
        return DateFormat.format(String.format("%s%s", dateFormat, timeFormat), new Date(time));
    }

    public static int getSpecialNumber(String number) {

        if (number.isEmpty() || AliCallerInfo.UNKNOWN_NUMBER.equals(number)) {
            return R.string.call_number_unknown_number;
        } else if (AliCallerInfo.PRIVATE_NUMBER.equals(number)) {
            return R.string.call_number_private_number;
        } else if (AliCallerInfo.PAYPHONE_NUMBER.equals(number)) {
            return R.string.call_number_payed_number;
        } else {
            return -1;
        }
    }

    public static void reportUsage(Activity activity, String value) {
        UsageReporter.onClick(activity, null, value);
    }

    public static void reportUsage(Class<? extends Activity> activityClass, String value) {
        UsageReporter.onClick(activityClass, value);
    }

    public static void makeSms(Context context, String number) {
        if (context != null && getSpecialNumber(number) < 0) {
            Intent smsIntent = ContactsUtils.getSendSmsIntent(number);
            context.startActivity(smsIntent);
        }
    }

    /**
     * makeCall is an interface that start an intent to make call.
     *
     * @param context passed by application context.
     * @param number passed by dialing number.
     * @param slotId passed by slotId, if slotId is {@link SimUtil.SLOT_ID_1} or
     *            {@link SimUtil.SLOT_ID_2}, we make call by slot1 or slot2, if
     *            slotId passed by {@link SimUtil.INVALID_SLOT_ID}, we make call
     *            by the default voice slotId which is set in DualSIM settings
     *            in Settings. If the default voice slotId is
     *            {@link SimUtil.SIM_DEFAULT_NO_SET or
     *            SimUtil.SIM_DEFAULT_ALWAYS_ASK}, Phone module will pop up a
     *            SIM account dialog to choose SIM card.
     */
    public static void makeCall(Context context, String number, int slotId) {
        if (TextUtils.isEmpty(number)) {
            Log.d(TAG, "makeCall() number is NULL!!!");
            return;
        }

        if (PLATFORM_YUNOS) {
            final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel",
                    number, null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (slotId == SimUtil.INVALID_SLOT_ID) {
                int defaultVoiceSlotId = SimUtil.getDefaultVoiceSlotId();
                if (defaultVoiceSlotId != SimUtil.SIM_DEFAULT_NO_SET && defaultVoiceSlotId != SimUtil.SIM_DEFAULT_ALWAYS_ASK) {
                    slotId = defaultVoiceSlotId;
                }
            }

            long subId = SimUtil.INVALID_SUB_ID;
            if (slotId != SimUtil.INVALID_SLOT_ID) {
                if (SimUtil.MULTISIM_ENABLE) {
                    subId = SimUtil.getSubId(slotId);
                } else {
                    subId = SimUtil.getDefaultSubId();
                }

                if (subId != SimUtil.INVALID_SUB_ID) {
                    final List<PhoneAccountHandle> accounts = PhoneAccountUtils
                            .getEnabledSubscriptionAccounts(context);
                    for (PhoneAccountHandle account : accounts) {
                        int accountId = PDUtils.getSubIdFromPhoneAccountHandle(context, account);
                        Log.d(TAG, "makeCall() accountId:" + accountId + ", subId:" + subId
                                + ", slotId:" + slotId + ", number:" + number);

                        if (accountId == subId) {
                            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account);
                            break;
                        }
                    }
                }
            }
            /*YUNOS BEGIN*/
            //Desc:BugID:8271331:improve incallui MO call performance
            //TelecomManager.placeCall is more efficient than startActivity
            //Author:chao.lc@alibaba-inc.com Date: 5/20/16 4:07 PM
            PDUtils.placeCall(context,intent);
            /*YUNOS END*/

        } else {
            final Intent intent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null));
            context.startActivity(intent);
        }
    }

    public static void makeConferenceCall(Context context, ArrayList<String> numbers) {
        Intent confCallIntent = SimContactUtils.makeConferenceCallIntent(context, numbers);
        if (confCallIntent == null) {
            Log.w(TAG, "makeConferenceCall: got null intent, conference call not supported.");
            return;
        }
        confCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.i(TAG, "makeConferenceCall: start activity for conference call.");
        context.startActivity(confCallIntent);
    }

    /**
     * Judge if we can place VoLTE video call to a number, by the number itself.
     * @param number
     * @return
     */
    public static boolean canPlaceVolteVideoCallByNumber(String number) {
        // Currently, only filter landlines of China mainland.
        // For other numbers, e.g. mobile numbers in China or foreign numbers,
        // we do not limit the VoLTE capability.
        // We don't need normalizeNumber(), we only need to strip non-digit chars here.
        String formatNumber = NumberNormalizeUtil.formatNumber(number);
        if (NumberNormalizeUtil.sTeleNumberPattern.matcher(formatNumber).find()) {
            Log.i(TAG, "canPlaceVolteVideoCallByNumber: return false for "+AliTextUtils.desensitizeNumber(number));
            return false;
        }
        Log.i(TAG, "canPlaceVolteVideoCallByNumber: return true for "+AliTextUtils.desensitizeNumber(number));
        return true;
    }

}
