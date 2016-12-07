
package com.yunos.alicontacts.sim;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public final class SimUtil {

    private static final String TAG = "SimUtil";

    /**
     * Platform type.
     */
    public static final int PLATFORM_UNKNOWN = -1;
    public static final int PLATFORM_QCOMM = 0;
    public static final int PLATFORM_MTK = 1;
    public static final int PLATFORM_SPREADTRUM = 2;
    public static final int PLATFORM_MARVELL = 3;

    /**
     * Platform hardware.
     */
    private static final String UNKNOWN = "unknown";
    private static final String HARDWARE_QCOMM = "qcom";
    private static final String HARDWARE_MTK = "mt";
    private static final String HARDWARE_SPREADTRUM = "sc";
    private static final String HARDWARE_MARVELL = "marvell";
    private static final String HARDWARE_MARVELL_PXA = "pxa";

    /**
     * Platform name.
     */
    private static final String PLATFORM_NAME_QCOMM = "QUALCOMM";
    private static final String PLATFORM_NAME_MTK = "MTK";
    private static final String PLATFORM_NAME_SPREADTRUM = "SPREADTRUM";
    private static final String PLATFORM_NAME_MARVELL = "MARVELL";

    private static String sHardWare = null;
    private static String sPlatformName = null;

    /**
     * IS_YUNOS means phone device is running on YunOS, maybe on MTK,
     * Qcomm,SP,or other platform, they are all YunOS, and it will return true.
     * if phone device is running on original Android device, it will return
     * false.
     */
    public static final boolean IS_YUNOS = isYunOS();

    /**
     * PLATFORM means device platform, such as MTK, Qcomm, Sp, etc.
     */
    public static final int PLATFORM = getPlatformType();

    /**
     * Judge if it is MTK Platform.
     */
    public static final boolean IS_PLATFORM_MTK = (PLATFORM == PLATFORM_MTK);

    /**
     * Judge if it is Qcomm Platform.
     */
    public static final boolean IS_PLATFORM_QCOMM = (PLATFORM == PLATFORM_QCOMM);

    /**
     * Judge if it is Spreadtrum Platform.
     */
    public static final boolean IS_PLATFORM_SPREADTRUM = (PLATFORM == PLATFORM_SPREADTRUM);

    /**
     * Judge if it is Spreadtrum Platform.
     */
    public static final boolean IS_PLATFORM_MARVELL = (PLATFORM == PLATFORM_MARVELL);

    public static final String ACTION_FROM = "action_from";
    public static final int ACTION_FROM_IMPORT_CONTACTS = 0;
    public static final int ACTION_FROM_EXPORT_CONTACTS = 1;
    public static final int ACTION_FROM_VIEW = 2;

    public static final String SUBSCRIPTION_KEY = "subscription";
    public static final String SLOT_KEY = "slot";
    public static final int SLOT_ID_1 = 0;
    public static final int SLOT_ID_2 = 1;
    /** An invalid slot identifier */
    public static final int INVALID_SLOT_ID = -1000; // SubscriptionManager.INVALID_SLOT_ID;
    /** Indicates the caller wants the default slot id. */
    public static final int DEFAULT_SLOT_ID = Integer.MAX_VALUE; // SubscriptionManager.DEFAULT_SLOT_ID;
    /** An invalid subscription identifier */
    public static final int INVALID_SUB_ID = -1000; // SubscriptionManager.INVALID_SUB_ID;
    /** Indicates the caller wants the default sub id. */
    public static final int DEFAULT_SUB_ID = Integer.MAX_VALUE; // SubscriptionManager.DEFAULT_SUB_ID;

    public static final int DUMMY_SLOT_FOR_AIRPLANE_MODE_ON = -100;
    public static final int DUMMY_SLOT_FOR_AIRPLANE_MODE_OFF = -101;

    /**
     * {@value SIM_DEFAULT_NO_SET} means "voice call" options in settings->dual
     * card settings is not set or set "Always ask". {@value
     * SIM_DEFAULT_ALWAYS_ASK} means "voice call" is set to "Always ask".
     *
     * @see SubscriptionManager#INVALID_SUB_ID and
     *      {@value SubscriptionManager#ASK_USER_SUB_ID}
     */
    public static final int SIM_DEFAULT_NO_SET = -1000; // SubscriptionManager.INVALID_SUB_ID;
    public static final int SIM_DEFAULT_ALWAYS_ASK = -1001; // SubscriptionManager.ASK_USER_SUB_ID;

    /**
     * SIM insert ERROR return type.
     */
    public static final int ERROR_SIM_NO_ERROR = 1;
    public static final int ERROR_SIM_UNKNOWN = 0;
    public static final int ERROR_SIM_NUMBER_TOO_LONG = -1;
    public static final int ERROR_SIM_TEXT_TOO_LONG = -2;
    public static final int ERROR_SIM_STORAGE_FULL = -3;
    public static final int ERROR_SIM_NOT_READY = -4;
    public static final int ERROR_SIM_PASSWORD_ERROR = -5;
    public static final int ERROR_SIM_ANR_TOO_LONG = -6;
    public static final int ERROR_SIM_GENERIC_FAILURE = -10;
    public static final int ERROR_SIM_ADN_LIST_NOT_EXIST = -11;
    public static final int ERROR_SIM_EMAIL_FULL = -12;
    public static final int ERROR_SIM_EMAIL_TOO_LONG = -13;
    public static final int ERROR_SIM_ANR_SAVE_FAILURE = -14;
    public static final int ERROR_SIM_WRONG_ADN_FORMAT = -15;

    /** Flag to mark delete sim contacts tags. */
    private static final int SIM_KEY_FLAG_INIT = 0x00;
    private static final int SIM_KEY_FLAG_INDEX = 0x01;
    private static final int SIM_KEY_FLAG_NAME = 0x02;
    private static final int SIM_KEY_FLAG_NUMBER = 0x04;
    private static final int SIM_KEY_FLAG_ADDITIONAL = 0x08;
    private static final int SIM_KEY_FLAG_EMAILS = 0x10;
    private static final int SIM_KEY_FLAG_PIN2 = 0x20;
    private static final String SIM_STR_PIN2 = "pin2";

    /**
     * SIM access URI for different platform.
     */
    // FIXME: These kind of platforms needs to adapter these values as below
    // except MTK platform.
    /** Qcomm SIM URI */
    private static final Uri QCOMM_DUAL_SIM_ADN_URI = Uri.parse("content://icc/adn/subId/");
    private static final Uri QCOMM_DUAL_SIM_PBR_URI = Uri.parse("content://icc/adn/subId/");
    private static final Uri QCOMM_SIM_ADN_URI = Uri.parse("content://icc/adn");
    private static final Uri QCOMM_SIM_PBR_URI = Uri.parse("content://icc/adn");

    /** MTK SIM URI */
    private static final Uri MTK_DUAL_SIM_ADN_URI = Uri.parse("content://icc/adn/subId/");
    private static final Uri MTK_DUAL_SIM_PBR_URI = Uri.parse("content://icc/pbr/subId/");
    private static final Uri MTK_SIM_ADN_URI = Uri.parse("content://icc/adn");
    private static final Uri MTK_SIM_PBR_URI = Uri.parse("content://icc/pbr");

    /** Spreadtrum SIM URI */
    private static final Uri SP_DUAL_SIM_ADN_URI = Uri.parse("content://icc/adn/subId/");
    private static final Uri SP_SIM_ADN_URI = Uri.parse("content://icc/adn");
    private static final Uri SP_SIM_PBR_URI = Uri.parse("content://icc/pbr");

    /** Qcomm SIM Projection */
    private static final String[] QCOMM_SIM_PROJECTION = new String[] {
            "name", "number", "emails", "anrs", "_id"
    };

    /** MTK SIM Projection */
    private static final String[] MTK_SIM_PROJECTION = new String[] {
            "index", "name", "number", "emails", "additionalNumber", "groupIds", "_id", "aas",
            "sne",
    };

    /** Spreadtrum SIM Projection */
    private static final String[] SPREADTRUM_SIM_PROJECTION = new String[] {
            "tag", "number", "emails", "anr", "aas", "sne", "grp", "gas", "sim_index"
    };

    private static final int MTK_SIM_INDEX_COLUMN = 0;
    private static final int MTK_SIM_NAME_COLUMN = 1;
    private static final int MTK_SIM_NUMBER_COLUMN = 2;
    private static final int MTK_SIM_EMAILS_COLUMN = 3;
    private static final int MTK_SIM_ANR_COLUMN = 4;
    private static final int MTK_SIM_ID_COLUMN = 6;

    private static final int QCOMM_SIM_INDEX_COLUMN = -1;
    private static final int QCOMM_SIM_NAME_COLUMN = 0;
    private static final int QCOMM_SIM_NUMBER_COLUMN = 1;
    private static final int QCOMM_SIM_EMAILS_COLUMN = 2;
    private static final int QCOMM_SIM_ANR_COLUMN = 3;
    private static final int QCOMM_SIM_ID_COLUMN = 4;

    private static final int SPREADTRUM_SIM_INDEX_COLUMN = 8;
    private static final int SPREADTRUM_SIM_NAME_COLUMN = 0;
    private static final int SPREADTRUM_SIM_NUMBER_COLUMN = 1;
    private static final int SPREADTRUM_SIM_EMAILS_COLUMN = 2;
    private static final int SPREADTRUM_SIM_ANR_COLUMN = 3;
    private static final int SPREADTRUM_SIM_ID_COLUMN = -1;

    /**
     * SIM_XXX_COLUMN is used to get SIM records. SIM_XXX is used to set
     * ContentValues tags for SIM.
     */
    public static final int SIM_INDEX_COLUMN;
    public static final int SIM_NAME_COLUMN;
    public static final int SIM_NUMBER_COLUMN;
    public static final int SIM_ANR_COLUMN;
    public static final int SIM_EMAILS_COLUMN;
    public static final int SIM_ID_COLUMN;

    public static final String SIM_INDEX;
    public static final String SIM_NAME;
    public static final String SIM_NUMBER;
    public static final String SIM_ANR;
    public static final String SIM_EMAILS;
    public static final String SIM_NEW_NAME;
    public static final String SIM_NEW_NUMBER;
    public static final String SIM_NEW_ANR;
    public static final String SIM_NEW_EMAILS;

    /**
     * SIM_PROJECTION used to get SIM projection when query sim card contacts.
     */
    public static final String[] SIM_PROJECTION;

    /**
     * Calls table simId column name.
     */
    public static final String CALLS_TABLE_SUBSCRIPTION_COLUMN_NAME;

    /** SIM state changed key, used to mark slot. */
    public static final String SIM_STATE_CHANGE_SLOT_KEY;

    private static final ICrossPlatformSim sSimInstance;

    // FIXME: These kind of platforms needs to adapter these values as below
    // except MTK platform.
    static {
        switch (PLATFORM) {
            case PLATFORM_MTK:
                SIM_PROJECTION = MTK_SIM_PROJECTION;

                SIM_INDEX_COLUMN = MTK_SIM_INDEX_COLUMN;
                SIM_NAME_COLUMN = MTK_SIM_NAME_COLUMN;
                SIM_NUMBER_COLUMN = MTK_SIM_NUMBER_COLUMN;
                SIM_ANR_COLUMN = MTK_SIM_ANR_COLUMN;
                SIM_EMAILS_COLUMN = MTK_SIM_EMAILS_COLUMN;
                SIM_ID_COLUMN = MTK_SIM_ID_COLUMN;

                SIM_INDEX = "index";
                SIM_NAME = "tag";
                SIM_NUMBER = "number";
                SIM_ANR = "anr";
                SIM_EMAILS = "emails";
                SIM_NEW_NAME = "newTag";
                SIM_NEW_NUMBER = "newNumber";
                SIM_NEW_ANR = "newAnr";
                SIM_NEW_EMAILS = "newEmails";

                CALLS_TABLE_SUBSCRIPTION_COLUMN_NAME = "subscription_id";

                SIM_STATE_CHANGE_SLOT_KEY = "slot";

                break;
            case PLATFORM_QCOMM:
                SIM_PROJECTION = QCOMM_SIM_PROJECTION;

                SIM_INDEX_COLUMN = QCOMM_SIM_INDEX_COLUMN;
                SIM_NAME_COLUMN = QCOMM_SIM_NAME_COLUMN;
                SIM_NUMBER_COLUMN = QCOMM_SIM_NUMBER_COLUMN;
                SIM_ANR_COLUMN = QCOMM_SIM_ANR_COLUMN;
                SIM_EMAILS_COLUMN = QCOMM_SIM_EMAILS_COLUMN;
                SIM_ID_COLUMN = QCOMM_SIM_ID_COLUMN;

                SIM_INDEX = "index";
                SIM_NAME = "tag";
                SIM_NUMBER = "number";
                SIM_ANR = "anrs";
                SIM_EMAILS = "emails";
                SIM_NEW_NAME = "newTag";
                SIM_NEW_NUMBER = "newNumber";
                SIM_NEW_ANR = "newAnrs";
                SIM_NEW_EMAILS = "newEmails";

                CALLS_TABLE_SUBSCRIPTION_COLUMN_NAME = "subscription_id";

                SIM_STATE_CHANGE_SLOT_KEY = "slot";

                break;
            case PLATFORM_MARVELL:
                SIM_PROJECTION = null;

                SIM_INDEX_COLUMN = -1;
                SIM_NAME_COLUMN = 0;
                SIM_NUMBER_COLUMN = 1;
                SIM_ANR_COLUMN = 3;
                SIM_EMAILS_COLUMN = 2;
                SIM_ID_COLUMN = 4;

                SIM_INDEX = "index";
                SIM_NAME = "tag";
                SIM_NUMBER = "number";
                SIM_ANR = "anrs";
                SIM_EMAILS = "emails";
                SIM_NEW_NAME = "newTag";
                SIM_NEW_NUMBER = "newNumber";
                SIM_NEW_ANR = "newAnrs";
                SIM_NEW_EMAILS = "newEmails";

                CALLS_TABLE_SUBSCRIPTION_COLUMN_NAME = "sim_id";

                SIM_STATE_CHANGE_SLOT_KEY = "slot";

                break;

            case PLATFORM_SPREADTRUM:
                SIM_PROJECTION = SPREADTRUM_SIM_PROJECTION;

                SIM_INDEX_COLUMN = SPREADTRUM_SIM_INDEX_COLUMN;
                SIM_NAME_COLUMN = SPREADTRUM_SIM_NAME_COLUMN;
                SIM_NUMBER_COLUMN = SPREADTRUM_SIM_NUMBER_COLUMN;
                SIM_ANR_COLUMN = SPREADTRUM_SIM_ANR_COLUMN;
                SIM_EMAILS_COLUMN = SPREADTRUM_SIM_EMAILS_COLUMN;
                SIM_ID_COLUMN = SPREADTRUM_SIM_ID_COLUMN;

                SIM_INDEX = "index";
                SIM_NAME = "tag";
                SIM_NUMBER = "number";
                SIM_ANR = "anr";
                SIM_EMAILS = "email";
                SIM_NEW_NAME = "newTag";
                SIM_NEW_NUMBER = "newNumber";
                SIM_NEW_ANR = "newAnr";
                SIM_NEW_EMAILS = "newEmail";

                CALLS_TABLE_SUBSCRIPTION_COLUMN_NAME = "subscription_id";

                SIM_STATE_CHANGE_SLOT_KEY = "slot";

                break;
            case PLATFORM_UNKNOWN:
            default:
                SIM_PROJECTION = MTK_SIM_PROJECTION;

                SIM_INDEX_COLUMN = MTK_SIM_INDEX_COLUMN;
                SIM_NAME_COLUMN = MTK_SIM_NAME_COLUMN;
                SIM_NUMBER_COLUMN = MTK_SIM_NUMBER_COLUMN;
                SIM_ANR_COLUMN = MTK_SIM_ANR_COLUMN;
                SIM_EMAILS_COLUMN = MTK_SIM_EMAILS_COLUMN;
                SIM_ID_COLUMN = MTK_SIM_ID_COLUMN;

                SIM_INDEX = "index";
                SIM_NAME = "tag";
                SIM_NUMBER = "number";
                SIM_ANR = "anr";
                SIM_EMAILS = "emails";
                SIM_NEW_NAME = "newTag";
                SIM_NEW_NUMBER = "newNumber";
                SIM_NEW_ANR = "newAnr";
                SIM_NEW_EMAILS = "newEmails";

                CALLS_TABLE_SUBSCRIPTION_COLUMN_NAME = "subscription_id";

                SIM_STATE_CHANGE_SLOT_KEY = "slot";

                break;
        }
        // FIXME: in codebase, we only have mtk/qcom/sprd implementations.
        // For other vendor support, refer to aliyunos/packages/apps/Contacts/vendors/readme.txt
        sSimInstance = new VendorSimImpl();
    };

    /**
     * MULTISIM_ENABLE returns true when device supports dual sim card standby.
     */
    public static final boolean MULTISIM_ENABLE = isMultiSimEnable();

    /**
     * SIM type.
     */
    public interface SimType {
        String SIM_TYPE_SIM_TAG = "SIM";
        String SIM_TYPE_USIM_TAG = "USIM";
        String SIM_TYPE_UIM_TAG = "UIM";
        String SIM_TYPE_UICC_TAG = "CSIM"; // UICC TYPE

        int SIM_TYPE_SIM = 0;
        int SIM_TYPE_USIM = 1;
        int SIM_TYPE_UIM = 2;
        int SIM_TYPE_UNKNOWN = -1;
    }

    public static final String MTK_TELEPHONY_SERVICE_EX = "phoneEx";

    /**
     * isYunOS means phone device is running on YunOS, maybe on MTK, Qcomm,SP,or
     * other platform, they are all YunOS, and it will return true. if phone
     * device is running on original Android device, it will return false.
     */
    private static boolean isYunOS() {
        return !"0.0.0".equals(SystemProperties.get("ro.yunos.version", "0.0.0"));
    }

    public static boolean isMultiSimEnable() {
        if (sSimInstance != null) {
            return sSimInstance.isMultiSimEnable();
        }

        return false;
    }

    public static boolean isAirplaneModeOn(Context context) {
        final boolean result;
        result = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;

        log("isAirplaneModeOn() :" + result);
        return result;
    }

    public static boolean hasIccCard() {
        if (sSimInstance != null) {
            return sSimInstance.hasIccCard();
        }

        return false;
    }

    public static boolean hasIccCard(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.hasIccCard(slotId);
        }

        return false;
    }

    public static boolean isSubActive() {
        if (sSimInstance != null) {
            return sSimInstance.isSubActive();
        }

        return false;
    }

    public static boolean isSubActive(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.isSubActive(slotId);
        }

        return false;
    }

    public static boolean isCardEnable() {
        if (sSimInstance != null) {
            return sSimInstance.isCardEnable();
        }

        return false;
    }

    public static boolean isCardEnable(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.isCardEnable(slotId);
        }

        return false;
    }

    public static boolean isSimAvailable() {
        if (sSimInstance != null) {
            return sSimInstance.isSimAvailable();
        }

        return false;
    }

    public static boolean isSimAvailable(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.isSimAvailable(slotId);
        }

        return false;
    }

    public static int getSimState() {
        if (sSimInstance != null) {
            return sSimInstance.getSimState();
        }

        return TelephonyManager.SIM_STATE_UNKNOWN;
    }

    public static int getSimState(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.getSimState(slotId);
        }

        return TelephonyManager.SIM_STATE_UNKNOWN;
    }

    public static boolean isSimReady() {
        if (sSimInstance != null) {
            return sSimInstance.isSimReady();
        }

        return false;
    }

    public static boolean isSimReady(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.isSimReady(slotId);
        }

        return false;
    }

    public static int getActiveSimCount(Context context) {
        if (sSimInstance != null) {
            return sSimInstance.getActiveSimCount(context);
        }

        return 0;
    }

    public static boolean isNoSimCard() {
        if (sSimInstance != null) {
            return sSimInstance.isNoSimCard();
        }

        return false;
    }

    public static boolean is2gSim() {
        if (sSimInstance != null) {
            return sSimInstance.is2gSim();
        }

        return false;
    }

    public static boolean is2gSim(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.is2gSim(slotId);
        }

        return false;
    }

    public static int getDefaultVoiceSlotId() {
        if (sSimInstance != null) {
            return sSimInstance.getDefaultVoiceSlotId();
        }

        return 0;
    }

    public static String getNetworkOperatorName(Context context) {
        if (sSimInstance != null) {
            return sSimInstance.getNetworkOperatorName(context);
        }

        return null;
    }

    public static String getNetworkOperatorName(Context context, int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.getNetworkOperatorName(context, slotId);
        }

        return null;
    }

    public static String getSimOperatorName(Context context) {
        if (sSimInstance != null) {
            return sSimInstance.getSimOperatorName(context);
        }

        return null;
    }

    public static String getSimOperatorName(Context context, int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.getSimOperatorName(context, slotId);
        }

        return null;
    }

    public static String getSimCardDisplayName(Context context) {
        if (sSimInstance != null) {
            return sSimInstance.getSimCardDisplayName(context);
        }

        return null;
    }

    public static String getSimCardDisplayName(Context context, int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.getSimCardDisplayName(context, slotId);
        }

        return null;
    }

    public static String getSimSerialNumber() {
        if (sSimInstance != null) {
            return sSimInstance.getSimSerialNumber();
        }

        return null;
    }

    public static String getSimSerialNumber(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.getSimSerialNumber(slotId);
        }

        return null;
    }

    public static int getSlotId(int subId) {
        if (sSimInstance != null) {
            return sSimInstance.getSlotIdBySubId(subId);
        }

        return 0;
    }

    public static int[] getSubIds(int slotId) {

        if (sSimInstance != null) {
            return sSimInstance.getSubIdsBySlotId(slotId);
        }

        return new int[] {
                1, 2
        };
    }

    public static int getSubId(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.getSubIdBySlotId(slotId);
        }

        return 1;
    }

    public static int getDefaultSubId() {
        if (sSimInstance != null) {
            return sSimInstance.getDefaultSubId();
        }

        return 1;
    }

    public static boolean isRadioOn() {
        if (sSimInstance != null) {
            return sSimInstance.isRadioOn();
        }

        return false;
    }

    public static boolean isRadioOn(int slotId) {
        if (sSimInstance != null) {
            return sSimInstance.isRadioOn(slotId);
        }

        return false;
    }

    /**
     * Original interface for query.
     */
    public static Cursor query(Context context, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (sSimInstance != null) {
            return sSimInstance
                    .query(context, uri, projection, selection, selectionArgs, sortOrder);
        }

        return null;
    }

    /**
     * Original interface for insert.
     */
    public static Uri insert(Context context, Uri uri, ContentValues values) {
        if (sSimInstance != null) {
            return sSimInstance.insert(context, uri, values);
        }

        return null;
    }

    /**
     * Original interface for delete.
     */
    public static int delete(Context context, Uri uri, String where, String[] whereArgs) {
        if (sSimInstance != null) {
            return sSimInstance.delete(context, uri, where, whereArgs);
        }

        return 0;
    }

    /**
     * Original interface for update.
     */
    public static int update(Context context, Uri uri, ContentValues values, String where,
            String[] selectionArgs) {
        if (sSimInstance != null) {
            return sSimInstance.update(context, uri, values, where, selectionArgs);
        }

        return 0;
    }

    /**
     * query SIM card contacts in SIM.
     */
    public static Cursor query(Context context) {
        return query(context, SLOT_ID_1);
    }

    /**
     * query SIM card contacts in SIM.
     */
    public static Cursor query(Context context, int slotId) {
        final Uri uri = getUri(slotId);
        log("query() uri:" + uri + ", on slot " + slotId);
        return query(context, uri, null, null, null, null);
    }

    /**
     * insert SIM card contacts in SIM.
     */
    public static Uri insert(Context context, ContentValues values) {
        return insert(context, SLOT_ID_1, values);
    }

    /**
     * delete SIM card contacts in SIM.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public static Uri insert(Context context, int slotId, ContentValues values) {
        final Uri uri = getUri(slotId);
        Uri result = insert(context, uri, values);
        log("insert() uri:" + uri + ", on slot " + slotId + ", values:" + values + ", result:"
                + result);
        return result;
    }

    /**
     * delete SIM card contacts in SIM.
     *
     * @param values means the sim contacts tags will be deleted.
     */
    public static int delete(Context context, ContentValues values) {
        StringBuilder where = new StringBuilder();

        if (checkDeleteContentValues(values, where) != ERROR_SIM_NO_ERROR) {
            log("delete() values:" + values + " FAIL!");
            return SimUtil.ERROR_SIM_UNKNOWN;
        }

        final Uri uri = getUri();
        int result = delete(context, uri, where.toString(), null);
        log("delete() uri:" + uri + ", result:" + result);
        return result;
    }

    /**
     * delete SIM card contacts in SIM.
     *
     * @param values means the sim contacts tags will be deleted.
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public static int delete(Context context, int slotId, ContentValues values) {
        StringBuilder where = new StringBuilder();

        if (checkDeleteContentValues(values, where) != ERROR_SIM_NO_ERROR) {
            log("delete() values:" + values + " FAIL!");
            return SimUtil.ERROR_SIM_UNKNOWN;
        }

        final Uri uri = getUri(slotId);
        int result = delete(context, uri, where.toString(), null);
        log("delete() uri:" + uri + ", on slot " + slotId + ", result:" + result);
        return result;
    }

    /**
     * update SIM card contacts in SIM.
     */
    public static int update(Context context, ContentValues values) {
        return update(context, SLOT_ID_1, values);
    }

    /**
     * update SIM card contacts in SIM.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public static int update(Context context, int slotId, ContentValues values) {
        final Uri uri = getUri(slotId);
        int result = update(context, uri, values, null, null);
        log("update() uri:" + uri + ", on slot " + slotId + ", result:" + result);
        return result;
    }

    /**
     * get sim card ANR uri.
     */
    public static Uri getUri() {
        return getUri(SLOT_ID_1);
    }

    /**
     * get SIM card ANR URI.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public static Uri getUri(int slotId) {
        if (MULTISIM_ENABLE) {
            final int subId = getSubId(slotId);
            if (is2gSim(slotId)) {
                switch (PLATFORM) {
                    case PLATFORM_MTK: {
                        return ContentUris.withAppendedId(MTK_DUAL_SIM_ADN_URI, subId);
                    }
                    case PLATFORM_QCOMM: {
                        return ContentUris.withAppendedId(QCOMM_DUAL_SIM_ADN_URI, subId);
                    }
                    case PLATFORM_SPREADTRUM:
                        return ContentUris.withAppendedId(SP_DUAL_SIM_ADN_URI, subId);
                    case PLATFORM_MARVELL:
                        // FIXME: add Marvell URI.
                    default:
                        Log.e(TAG, "getUri() dual GSM unknown platform:" + PLATFORM);
                        break;
                }
            } else {
                switch (PLATFORM) {
                    case PLATFORM_MTK: {
                        return ContentUris.withAppendedId(MTK_DUAL_SIM_PBR_URI, subId);
                    }
                    case PLATFORM_QCOMM: {
                        return ContentUris.withAppendedId(QCOMM_DUAL_SIM_PBR_URI, subId);
                    }
                    case PLATFORM_SPREADTRUM:
                        return ContentUris.withAppendedId(SP_DUAL_SIM_ADN_URI, subId);
                    case PLATFORM_MARVELL:
                        // FIXME: add Marvell URI.
                    default:
                        Log.e(TAG, "getUri() dual USIM unknown platform:" + PLATFORM);
                        break;
                }
            }
        } else {
            if (is2gSim()) {
                switch (PLATFORM) {
                    case PLATFORM_MTK:
                        return MTK_SIM_ADN_URI;
                    case PLATFORM_QCOMM:
                        return QCOMM_SIM_ADN_URI;
                    case PLATFORM_SPREADTRUM:
                        return SP_SIM_ADN_URI;
                    case PLATFORM_MARVELL:
                        // FIXME: add Marvell URI.
                    default:
                        Log.e(TAG, "getUri() single GSM unknown platform:" + PLATFORM);
                        break;
                }
            } else {
                switch (PLATFORM) {
                    case PLATFORM_MTK:
                        return MTK_SIM_PBR_URI;
                    case PLATFORM_QCOMM:
                        return QCOMM_SIM_PBR_URI;
                    case PLATFORM_SPREADTRUM:
                        return SP_SIM_PBR_URI;
                    case PLATFORM_MARVELL:
                        // FIXME: add Marvell URI.
                    default:
                        Log.e(TAG, "getUri() single USIM unknown platform:" + PLATFORM);
                        break;
                }
            }
        }

        return null;
    }

    public static boolean isVideoCallEnabled(Context context) {
        if (sSimInstance != null) {
            return sSimInstance.isVideoCallEnabled(context);
        }
        return false;
    }

    public static boolean isVolteEnabled(Context context) {
        if (sSimInstance != null) {
            return sSimInstance.isVolteEnabled(context);
        }
        return false;
    }

    public static boolean isVolteEnhancedConfCallSupport(Context context) {
        if (sSimInstance != null) {
            return sSimInstance.isVolteEnhancedConfCallSupport(context);
        }
        return false;
    }

    // TODO: This model only supports one handler to be registered.
    public static void observeVolteAttachChanged(Context context, Handler handler, int what) {
        if (sSimInstance != null) {
            sSimInstance.observeVolteAttachChanged(context, handler, what);
        }
    }

    public static void unObserveVolteAttachChanged(Context context) {
        if (sSimInstance != null) {
            sSimInstance.unObserveVolteAttachChanged(context);
        }
    }

    private static int checkDeleteContentValues(ContentValues values, StringBuilder where) {
        int flag = SIM_KEY_FLAG_INIT;
        if (values != null) {
            if (values.containsKey(SIM_INDEX)) {
                where.append(SIM_INDEX + "=" + values.getAsInteger(SIM_INDEX));
                flag |= SIM_KEY_FLAG_INDEX;
            }

            if (values.containsKey(SIM_NAME)) {
                if (flag > SIM_KEY_FLAG_INIT) {
                    where.append(" AND ");
                }
                where.append(SIM_NAME + "='" + values.getAsString(SIM_NAME) + "'");
                flag |= SIM_KEY_FLAG_NAME;
            }

            if (values.containsKey(SIM_NUMBER)) {
                if (flag > SIM_KEY_FLAG_INIT) {
                    where.append(" AND ");
                }
                where.append(SIM_NUMBER + "='" + values.getAsString(SIM_NUMBER) + "'");
                flag |= SIM_KEY_FLAG_NUMBER;
            }

            if (values.containsKey(SIM_ANR)) {
                if (flag > SIM_KEY_FLAG_INIT) {
                    where.append(" AND ");
                }
                where.append(SIM_ANR + "='" + values.getAsString(SIM_ANR) + "'");
                flag |= SIM_KEY_FLAG_ADDITIONAL;
            }

            if (values.containsKey(SIM_EMAILS)) {
                if (flag > SIM_KEY_FLAG_INIT) {
                    where.append(" AND ");
                }
                where.append(SIM_EMAILS + "='" + values.getAsString(SIM_EMAILS) + "'");
                flag |= SIM_KEY_FLAG_EMAILS;
            }

            if (values.containsKey(SIM_STR_PIN2)) {
                if (flag > SIM_KEY_FLAG_INIT) {
                    where.append(" AND ");
                }
                where.append("pin2 = " + values.getAsString(SIM_STR_PIN2) + "'");
                flag |= SIM_KEY_FLAG_PIN2;
            }

        }

        return ERROR_SIM_NO_ERROR;
    }

    /**
     * it returns platform type.
     *
     * @return SimUtil.PLATFORM_UNKNOWN, SimUtil.PLATFORM_QCOMM,
     *         SimUtil.PLATFORM_MTK, SimUtil.PLATFORM_SPREADTRUM,
     *         SimUtil.PLATFORM_MARVELL
     */
    private static int getPlatformType() {
        int platform = PLATFORM_UNKNOWN;
        if (sHardWare == null) {
            sHardWare = SystemProperties.get("ro.hardware", UNKNOWN);
            log("getPlatformType() hardware:" + sHardWare);
        }

        if (sHardWare.contains(HARDWARE_QCOMM)) {
            platform = PLATFORM_QCOMM;
        } else if (sHardWare.contains(HARDWARE_MTK)) {
            platform = PLATFORM_MTK;
        } else if (sHardWare.contains(HARDWARE_SPREADTRUM)) {
            platform = PLATFORM_SPREADTRUM;
        } else if (sHardWare.contains(HARDWARE_MARVELL) || sHardWare.contains(HARDWARE_MARVELL_PXA)) {
            platform = PLATFORM_MARVELL;
        }

        if (platform == PLATFORM_UNKNOWN) {
            if (sPlatformName == null) {
                sPlatformName = SystemProperties.get("ro.yunos.platform", UNKNOWN);
                log("getPlatformType() platformName:" + sPlatformName);
            }

            if (sPlatformName.equalsIgnoreCase(PLATFORM_NAME_QCOMM)) {
                platform = PLATFORM_QCOMM;
            } else if (sPlatformName.equalsIgnoreCase(PLATFORM_NAME_MTK)) {
                platform = PLATFORM_MTK;
            } else if (sPlatformName.equalsIgnoreCase(PLATFORM_NAME_SPREADTRUM)) {
                platform = PLATFORM_SPREADTRUM;
            } else if (sPlatformName.equalsIgnoreCase(PLATFORM_NAME_MARVELL)) {
                platform = PLATFORM_MARVELL;
            }

        }

        log("getPlatformType() platformType:" + platform);
        return platform;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    /* YUNOS BEGIN PB */
    //##email:caixiang.zcx@alibaba-inc.com
    //##BugID:(8206447) ##date:2016/05/11
    //##description:get the capacity of sim card
    public static int getMSimCardMaxCount(int soltId) {
        if (sSimInstance != null) {
            return sSimInstance.getMSimCardMaxCount(soltId);
        }
        return 0;
    }

    public static int getSpareEmailCount(int soltId) {
        if (sSimInstance != null) {
            return sSimInstance.getSpareEmailCount(soltId);
        }
        return 0;
    }
    /* YUNOS END PB */
}
