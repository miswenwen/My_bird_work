
package com.yunos.alicontacts.sim;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.telephony.TelephonyManagerEx;
import com.yunos.alicontacts.platform.PDUtils;

import java.lang.reflect.Field;
import java.util.List;

public class VendorSimImpl implements ICrossPlatformSim {
    private static final String MTK_TELEPHONY_SERVICE_EX = "phoneEx";
    private static TelephonyManager sTelephonyManager = TelephonyManager.getDefault();

    @Override
    public boolean isMultiSimEnable() {
        return sTelephonyManager.isMultiSimEnabled();
    }

    @Override
    public boolean hasIccCard() {
        return sTelephonyManager.hasIccCard();
    }

    @Override
    public boolean hasIccCard(int slotId) {
        return sTelephonyManager.hasIccCard(slotId);
    }

    @Override
    public boolean isSimReady() {
        return sTelephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    @Override
    public boolean isSimReady(int slotId) {
        return sTelephonyManager.getSimState(slotId) == TelephonyManager.SIM_STATE_READY;
    }

    @Override
    public boolean isSubActive() {
        return hasIccCard() && isSimReady() && isRadioOn();
    }

    @Override
    public boolean isSubActive(int slotId) {
        return hasIccCard(slotId) && isSimReady(slotId) && isRadioOn(slotId);
    }

    @Override
    public boolean isCardEnable() {
        return isSubActive();
    }

    @Override
    public boolean isCardEnable(int slotId) {
        return isSubActive(slotId);
    }

    @Override
    public boolean isSimAvailable() {
        return hasIccCard() && isSimReady();
    }

    @Override
    public boolean isSimAvailable(int slotId) {
        return hasIccCard(slotId) && isSimReady(slotId);
    }

    @Override
    public int getSimState() {
        return sTelephonyManager.getSimState();
    }

    @Override
    public int getSimState(int slotId) {
        return sTelephonyManager.getSimState(slotId);
    }

    @Override
    public int getActiveSimCount(Context context) {
        final int count = SubscriptionManager.from(context).getActiveSubscriptionInfoCount();
        int activeSimCount = 0;
        for (int i = 0; i < count; i++) {
            if (isSubActive(i)) {
                activeSimCount++;
            }
        }

        log("getActiveSimCount() activeSimCount:" + activeSimCount);
        return activeSimCount;
    }

    @Override
    public boolean isNoSimCard() {
        final int count = sTelephonyManager.getPhoneCount();
        for (int i = 0; i < count; i++) {
            if (hasIccCard(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean is2gSim() {
        log("is2gSim() single card");
        return is2gSim(SLOT_ID_1);
    }

    @Override
    public boolean is2gSim(int slotId) {

        boolean is2g = true;

        final int subId = getSubIdBySlotId(slotId);
        log("is2gSim() slotId:" + slotId + ", subId:" + subId);

        final ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(ServiceManager
                .getService(MTK_TELEPHONY_SERVICE_EX));
        if (iTel == null) {
            log("is2gSim() iTel == null");
            return is2g;
        }

        try {
            String iccType = iTel.getIccCardType(subId);
            log("is2gSim() iccType:" + iccType + " on sub " + subId);
            if (SimType.SIM_TYPE_USIM_TAG.equals(iccType)
                    || SimType.SIM_TYPE_UIM_TAG.equals(iccType)
                    || SimType.SIM_TYPE_UICC_TAG.equals(iccType)) {
                is2g = false;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "is2gSim() RemoteException subId:" + subId, e);
        }

        log("is2gSim() result:" + is2g);

        return is2g;

    }

    @Override
    public int getDefaultVoiceSlotId() {
        int subId = SubscriptionManager.getDefaultVoiceSubId();
        return getSlotIdBySubId(subId);
    }

    @Override
    public String getNetworkOperatorName(Context context) {
        return TelephonyManager.from(context).getNetworkOperatorName();
    }

    @Override
    public String getNetworkOperatorName(Context context, int slotId) {
        final int subId = getSubIdBySlotId(slotId);
        return TelephonyManager.from(context).getNetworkOperatorName(subId);
    }

    @Override
    public String getSimOperatorName(Context context) {
        return TelephonyManager.from(context).getSimOperatorName();
    }

    @Override
    public String getSimOperatorName(Context context, int slotId) {
        final int subId = getSubIdBySlotId(slotId);
        return TelephonyManager.from(context).getSimOperatorNameForSubscription(subId);
    }

    @Override
    public String getSimCardDisplayName(Context context) {
        final int subId = getSubIdBySlotId(sTelephonyManager.getDefaultSim());
        SubscriptionInfo subInfo = SubscriptionManager.from(context).getSubscriptionInfo(subId);
        if (subInfo != null) {
            return subInfo.getDisplayName().toString();
        }

        return null;
    }

    @Override
    public String getSimCardDisplayName(Context context, int slotId) {
        final int subId = getSubIdBySlotId(slotId);
        SubscriptionInfo subInfo = SubscriptionManager.from(context).getSubscriptionInfo(subId);
        if (subInfo != null) {
            return subInfo.getDisplayName().toString();
        }

        return null;
    }

    @Override
    public String getSimSerialNumber() {
        return sTelephonyManager.getSimSerialNumber();
    }

    @Override
    public String getSimSerialNumber(int slotId) {
        final int subId = getSubIdBySlotId(slotId);

        return sTelephonyManager.getSimSerialNumber(subId);
    }

    @Override
    public int getSlotIdBySubId(int subId) {
        return SubscriptionManager.getSlotId(subId);
    }

    @Override
    public int[] getSubIdsBySlotId(int slotId) {
        return SubscriptionManager.getSubId(slotId);
    }

    @Override
    public int getSubIdBySlotId(int slotId) {
        final int[] subIds = getSubIdsBySlotId(slotId);
        if (subIds != null && subIds.length > 0) {
            return subIds[0];
        }

        log("getSubId() DEFAULT_SUB_ID by slotId:" + slotId);
        return DEFAULT_SUB_ID;
    }

    @Override
    public int getDefaultSubId() {
        return SubscriptionManager.getDefaultSubId();
    }

    @Override
    public boolean isRadioOn() {
        return sTelephonyManager.isRadioOn();
    }

    @Override
    public boolean isRadioOn(int slotId) {
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            Log.e(TAG, "isRadioOn() iTel is NULL!!!");
            return false;
        }

        int subId = getSubIdBySlotId(slotId);
        boolean isRadioOn = PDUtils.isRadioOnForSubscriber(iTel, subId);
        return isRadioOn;
    }

    @Override
    public Cursor query(Context context, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (context == null) {
            Log.e(TAG, "query() context is null!!!");
            return null;
        }

        final ContentResolver resolver = context.getContentResolver();
        return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public Uri insert(Context context, Uri uri, ContentValues values) {
        if (context == null) {
            Log.e(TAG, "insert() context is null!!!");
            return null;
        }

        final ContentResolver resolver = context.getContentResolver();
        return resolver.insert(uri, values);
    }

    @Override
    public int delete(Context context, Uri uri, String where, String[] whereArgs) {
        if (context == null) {
            Log.e(TAG, "delete() context is null!!!");
            return 0;
        }

        final ContentResolver resolver = context.getContentResolver();
        return resolver.delete(uri, where, whereArgs);
    }

    @Override
    public int update(Context context, Uri uri, ContentValues values, String where,
            String[] selectionArgs) {
        if (context == null) {
            Log.e(TAG, "update() context is null!!!");
            return 0;
        }

        final ContentResolver resolver = context.getContentResolver();
        return resolver.update(uri, values, where, selectionArgs);
    }

    public static final boolean MTK_VILTE_SUPPORT
            = SystemProperties.get("ro.mtk_vilte_support").equals("1");

    @Override
    public boolean isVideoCallEnabled(Context context) {
        TelecomManager telecommMgr = (TelecomManager)
            context.getSystemService(Context.TELECOM_SERVICE);
        if (telecommMgr == null) {
            return false;
        }
        List<PhoneAccountHandle> accountHandles = telecommMgr.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle accountHandle : accountHandles) {
            PhoneAccount account = telecommMgr.getPhoneAccount(accountHandle);
            if (account != null && account.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING)) {
                return true;
            }
        }
        return false;
    }

    /*YunOS BEGIN PB*/
    //##module:()  ##author:qiming.cx@alibaba-inc.com
    //##BugID:(6798543)  ##date:2015-12-14 20:25 -->
    //##Desc: Volte
    public static final boolean MTK_IMS_SUPPORT
            = SystemProperties.get("ro.mtk_ims_support").equals("1");
    public static final boolean MTK_VOLTE_SUPPORT
            = SystemProperties.get("ro.mtk_volte_support").equals("1");
    public static final boolean MTK_ENHANCE_VOLTE_CONF_CALL = true;

    @Override
    public boolean isVolteEnhancedConfCallSupport(Context context) {
        return MTK_ENHANCE_VOLTE_CONF_CALL && MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;
    }

    public static boolean isImsEnabled(Context context) {
        boolean result =
                1 == Settings.Global.getInt(context.getContentResolver(), Settings.Global.ENHANCED_4G_MODE_ENABLED, 0);
        Log.i(TAG, "isImsEnabled: result="+result);
        return result;
    }

    @Override
    public boolean isVolteEnabled(Context context) {
        boolean result = isImsEnabled(context)
                && isVolteEnhancedConfCallSupport(context)
                && isVoLTEConfCallEnable(context);
        Log.i(TAG, "isVolteEnabled: result="+result);
        return result;
    }

    private static int mVolteConferenceEnhancedCapabilityFlag = 0;
    public static int getConferenceCapabilityFlag() {
        if (mVolteConferenceEnhancedCapabilityFlag == 0) {
            Class<PhoneAccount> paCls = PhoneAccount.class;
            Field volteConferenceEnhancedCapabilityFlag;
            /* PhoneAccount.CAPABILITY_VOLTE_CONFERENCE_ENHANCED for sdk ver 23,
             * PhoneAccount.CAPABILITY_VOLTE_ENHANCED_CONFERENCE for sdk ver 22.
             * Not supported below sdk ver 22. */
            try {
                if (Build.VERSION.SDK_INT < 23) {
                    volteConferenceEnhancedCapabilityFlag = paCls.getField("CAPABILITY_VOLTE_ENHANCED_CONFERENCE");
                } else {
                    volteConferenceEnhancedCapabilityFlag = paCls.getField("CAPABILITY_VOLTE_CONFERENCE_ENHANCED");
                }
                mVolteConferenceEnhancedCapabilityFlag = volteConferenceEnhancedCapabilityFlag.getInt(null);
            } catch (NoSuchFieldException e) {
                // If this happen, then the framework must update the code of PhoneAccount.
                Log.e(TAG, "getConferenceCapabilityFlag: cannot find proper field in PhoneAccount. ver="+Build.VERSION.SDK_INT, e);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                // won't happen.
                Log.e(TAG, "getConferenceCapabilityFlag: cannot access specified field in PhoneAccount. ver="+Build.VERSION.SDK_INT, e);
            }
        }
        Log.i(TAG, "getConferenceCapabilityFlag: return "+mVolteConferenceEnhancedCapabilityFlag);
        return mVolteConferenceEnhancedCapabilityFlag;
    }

    public static boolean isVoLTEConfCallEnable(Context context) {
        final TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        List<PhoneAccount> phoneAccouts = telecomManager.getAllPhoneAccounts();
        for (PhoneAccount phoneAccount : phoneAccouts) {
            if (phoneAccount.hasCapabilities(getConferenceCapabilityFlag())) {
                /// M:for ALPS02085376, need to judge if network type is LTE, because IMS may register
                // at GSM network, and this time can not make enhance conference call. @{
                PhoneAccountHandle handle = phoneAccount.getAccountHandle();
                if (handle != null) {
                    int subId = PDUtils.getSubIdFromPhoneAccountHandle(context, handle);
                    int slotId = SubscriptionManager.getSlotId(subId);
                    int type = TelephonyManagerEx.getDefault().getNetworkType(slotId);
                    Log.d(TAG, "isVoLTEConfCallEnable: slotId = " + slotId + ", type = " + type);
                    if (TelephonyManager.NETWORK_TYPE_LTE == type) {
                        return true;
                    }
                }
                /// @}
            }
        }
        Log.d(TAG, "isVoLTEConfCallEnable, return false at last.");
        return false;
    }

    @Override
    public void observeVolteAttachChanged(Context context, Handler handler, int what) {
        SubscriptionManager subMgr = SubscriptionManager.from(context);
        int[] subIds = subMgr.getActiveSubscriptionIdList();
        boolean regState = false;

        if(subIds != null) {
            for (int i = 0; i < subIds.length; i++) {
                try {
                    int imsState = ImsManager.getInstance(context, subIds[i]).getImsState();
                    if (imsState == PhoneConstants.IMS_STATE_ENABLE) {
                        regState = true;
                        break;
                    }
                } catch (ImsException e) {
                    Log.e(TAG, "ObserveVolteAttachChanged: got exception.", e);
                }
            }
        }

        Message msg = handler.obtainMessage(what);
        msg.arg1 = -1;
        msg.arg2 = regState ? 1 : 0;
        handler.sendMessage(msg);
    }

    @Override
    public void unObserveVolteAttachChanged(Context context) {
        // nothing to unobserve.
    }
    /* YunOS END PB */

    @Override
    public void log(String msg) {
        Log.d(TAG, "[MediaTek]---" + msg);
    }

    /* YUNOS BEGIN PB */
    //##email:caixiang.zcx@alibaba-inc.com
    //##BugID:(8206447) ##date:2016/05/12
    //##description:get the capacity of sim card
    @Override
    public int getMSimCardMaxCount(int slotId) {
        return 500;
    }
    @Override
    public int getSpareEmailCount(int slotId) {
        return 500;
    }
    /* YUNOS END PB */
}
