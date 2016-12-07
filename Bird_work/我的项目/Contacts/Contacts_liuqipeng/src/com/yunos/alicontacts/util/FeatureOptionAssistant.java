package com.yunos.alicontacts.util;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.ContactsApplication;
import com.yunos.alicontacts.sim.SimUtil;

/**
 * Add this class for getting yunos config for contact.
 * @author Aliyunos
 *
 */
public class FeatureOptionAssistant {
    private static final String TAG = "FeatureOptionAssistant";

    /**
     * In some features, codebase will have some default settings,
     * but the customers might have different requirements,
     * e.g. the default value for display SIM contacts.
     * From the history, some customers want to display SIM contacts by default, some not.
     * So we use a system property to control the default values of such settings.<p>
     * But we do not want to create too many system properties (because system properties have a limit),
     * so we design to put several settings in one property.
     * The length of system property value is 92,
     * so don't put a large amount of settings in one property.<p>
     * The content of this property is like: key1:value1;key2:value2;...;keyn:valuen.
     * Currently, only one key is supported: sim_contacts.<br>
     * "sim_contacts:1" means display SIM contacts by default.<br>
     * "sim_contacts:0" (or any non-1 values) means do NOT display SIM contacts by default.<p>
     * If we want to more custom options, we can add other keys here.
     */
    public static final String SYS_PROP_KEY_CUSTOM_OPTIONS = "ro.yunos.contacts.options";
    public static final String CUSTOM_OPTIONS_KEY_VALUE_PAIR_SEPARATOR = ";";
    public static final String OPTION_KEY_SIM_CONTACTS = "sim_contacts";

    private static String sCustomOptionValue = null;
    private static boolean sDefaultShowSimContacts = true;

    private synchronized static void loadCustomOptions() {
        if (sCustomOptionValue != null) {
            return;
        }
        sCustomOptionValue = SystemProperties.get(SYS_PROP_KEY_CUSTOM_OPTIONS, "");
        if (sCustomOptionValue == null) {
            sCustomOptionValue = "";
            return;
        }
        String[] optArray = sCustomOptionValue.split(CUSTOM_OPTIONS_KEY_VALUE_PAIR_SEPARATOR);
        if (optArray == null) {
            return;
        }
        int sepPos;
        String key, val;
        for (String optToken : optArray) {
            if (TextUtils.isEmpty(optToken)) {
                continue;
            }
            sepPos = optToken.indexOf(':');
            if ((sepPos < 0) || (sepPos == (optToken.length() - 1))) {
                continue;
            }
            key = optToken.substring(0, sepPos);
            val = optToken.substring(sepPos + 1);
            switch (key) {
            case OPTION_KEY_SIM_CONTACTS:
                sDefaultShowSimContacts = "1".equals(val);
                break;
            default:
                Log.w(TAG, "loadCustomOptions: malformed option "+optToken);
                break;
            }
        }
    }

    public static boolean isDefaultShowSimContacts() {
        loadCustomOptions();
        return sDefaultShowSimContacts;
    }

    /**
     * Some customer wants to show only one IMEI on dual card phone, when dial *#06#.
     * Because the contacts app is not open source to customer, so we deliver the apk,
     * and the customer has to change the behavior via a system property.
     */
    public static final String SYS_PROP_KEY_IMEI_COUNT = "ro.yunos.imei_show_count";

    /** For International Support start. *{ */
    private static String sInternationalSupportted;
    private static boolean sIsInternationalSupportted;

    // This method used for International Support check.
    public static boolean isInternationalSupportted() {
        if (sInternationalSupportted == null) {
            sInternationalSupportted = SystemProperties.get("ro.yunos.international", "false");
            sIsInternationalSupportted = "true".equals(sInternationalSupportted);
            Log.d(TAG, "sIsInternationalSupportted is " +sIsInternationalSupportted);
        }
        return sIsInternationalSupportted;
    }
    /** For International Support end. }* */

    /** For CloudCall Support start. *{ */
    private static String sCloudCallSupportted;
    private static boolean sIsCloudCallSupportted;

    // This method used for International Support check.
    public static boolean isCloudCallSupportted() {
        if (sCloudCallSupportted == null) {
            sCloudCallSupportted = SystemProperties.get("ro.yunos.support.yuntelephony", "no");
            sIsCloudCallSupportted = "yes".equals(sCloudCallSupportted);
            Log.d(TAG, "sIsCloudCallSupportted is " +sIsCloudCallSupportted);
        }
        return sIsCloudCallSupportted;
    }
    /** For CloudCall Support end. }* */

    /* Determine how many IMEIs to be displayed. - begin */
    private static int sImeiShowCount = -1;
    public static int getImeiShowCount() {
        if (sImeiShowCount < 0) {
            sImeiShowCount = SystemProperties.getInt(SYS_PROP_KEY_IMEI_COUNT, SimUtil.MULTISIM_ENABLE ? 2 : 1);
            if (sImeiShowCount <= 0) {
                sImeiShowCount = SimUtil.MULTISIM_ENABLE ? 2 : 1;
            }
        }
        return sImeiShowCount;
    }
    /* Determine how many IMEIs to be displayed. - end */

    /**
     * The contacts list displays favorite and frequent contacts, and it can be disabled in settings.
     * For low-end devices, we want it be closed by default.
     */
    public static final boolean DEFAULT_DISPLAY_FAVORITES_IN_CONTACTS_LIST
            = !ContactsApplication.IS_LOW_MEMORY_DEVICE;

    /**
     * For low-end devices, the 2nd level fish eye is disabled for better performance.
     */
    public static final boolean DISPLAY_FISH_EYE_2ND_LEVEL
            = !ContactsApplication.IS_LOW_MEMORY_DEVICE;

    /**
     * show contact portrait in setting activity.
     */
    // YUNOS BEGIN
    // modules(Contacts):BugID:6327019:[CUCC]show contact portrait for CUCC
    // date:2016-05-12
    // author:haitao.wht@alibaba-inc.com
//    public static final boolean DEFAULT_PHOTO_ON_OFF
//            = !ContactsApplication.IS_LOW_MEMORY_DEVICE;

    public static final boolean DEFAULT_PHOTO_ON_OFF
            = !ContactsApplication.IS_LOW_MEMORY_DEVICE || android.os.Build.YUNOS_CARRIER_CUCC;
    // YUNOS END
}
