
package com.yunos.alicontacts.sim;

import android.app.ActivityThread;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.util.Log;

import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.IccConstants;
import com.yunos.alicontacts.platform.PDUtils;

import java.util.List;

public class VendorSimImpl implements ICrossPlatformSim {

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
        boolean hasAnr = canSaveAnr(slotId);
        log("is2gSim(), slotId="+slotId+"; hasAnr="+hasAnr);
        return !hasAnr;
    }

    @Override
    public int getDefaultVoiceSlotId() {
        Application app = ActivityThread.currentApplication();
        if (app == null) {
            Log.w(TAG, "getDefaultVoiceSlotId: can NOT determine default voice slot id at this time. Assume always ask.");
            return SimUtil.SIM_DEFAULT_ALWAYS_ASK;
        }
        TelecomManager tm = (TelecomManager) app.getSystemService(Context.TELECOM_SERVICE);
        PhoneAccountHandle defCallAccount = tm.getUserSelectedOutgoingPhoneAccount();
        if (defCallAccount == null) {
            Log.i(TAG, "getDefaultVoiceSlotId: default call account is null.");
            return SimUtil.SIM_DEFAULT_ALWAYS_ASK;
        }
        final int subId = SubscriptionManager.getDefaultVoiceSubId();
        Log.i(TAG, "getDefaultVoiceSlotId: subId=" + subId);
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
        final SubscriptionInfo subInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            return subInfo.getDisplayName().toString();
        }

        return null;
    }

    @Override
    public String getSimCardDisplayName(Context context, int slotId) {
        final int subId = getSubIdBySlotId(slotId);
        final SubscriptionInfo subInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(subId);
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
        final int[] temp = SubscriptionManager.getSubId(slotId);
        if (temp == null) {
            return null;
        }
        final int[] ret = new int[temp.length];
        System.arraycopy(temp, 0, ret, 0, ret.length);
        return ret;
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

    @Override
    public void log(String msg) {
        Log.d(TAG, "[Qualcomm]---" + msg);
    }

    private int getAnrCount(int slot) {
        int anrCount = 0;
        int[] subId = SubscriptionManager.getSubId(slot);
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                if (subId != null && (subId.length > 0)
                        && TelephonyManager.getDefault().isMultiSimEnabled()) {
                    anrCount = iccIpb.getAnrCountUsingSubId(subId[0]);
                } else {
                    anrCount = iccIpb.getAnrCount();
                }
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "[Qualcomm]getAnrCount: got exception.", ex);
        }
        return anrCount;
    }

    private boolean canSaveAnr(int slot) {
        return getAnrCount(slot) > 0;
    }

    /* YUNOS BEGIN PB */
    //##email:caixiang.zcx@alibaba-inc.com
    //##BugID:(8206447) ##date:2016/05/12
    //##description:get the capacity of sim card
    @Override
    public int getMSimCardMaxCount(int slotId) {
        int adnCount = 0;
        int[] subId = SubscriptionManager.getSubId(slotId);
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));

            if (iccIpb != null) {
                if (subId != null
                        && TelephonyManager.getDefault().isMultiSimEnabled()) {
                    List<AdnRecord> list = iccIpb.getAdnRecordsInEfForSubscriber(
                            subId[0], IccConstants.EF_ADN);
                    if (null != list) {
                        adnCount = list.size();
                    }
                } else {
                    List<AdnRecord> list = iccIpb
                            .getAdnRecordsInEf(IccConstants.EF_ADN);
                    if (null != list) {
                        adnCount = list.size();
                    }
                }
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to IIccPhoneBookMSim", ex);
        }
        return adnCount;
    }
    /* YUNOS END PB */


    @Override
    public int getSpareEmailCount(int slotId) {
        int emailCount = 0;
        int[] subId = SubscriptionManager.getSubId(slotId);
        try {
            IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(ServiceManager
                    .getService("simphonebook"));
            if (iccIpb != null) {
                if (subId != null
                        && TelephonyManager.getDefault().isMultiSimEnabled()) {
                    emailCount = iccIpb.getSpareEmailCountUsingSubId(subId[0]);
                } else {
                    emailCount = iccIpb.getSpareEmailCount();
                }
            }
        } catch (RemoteException ex) {
            // ignore it
        } catch (SecurityException ex) {
            Log.i(TAG, ex.toString());
        } catch (Exception ex) {
        }

        return emailCount;
    }

    @Override
    public boolean isVideoCallEnabled(Context context) {
        Log.i(TAG, "isVideoCallEnabled: [Qualcomm]");
        if (!SystemProperties.get("ro.qcom_volte_support","0").equals("1")) {
            Log.i(TAG, "isVideoCallEnabled: not configured.");
            return false;
        }
        TelecomManager telecommMgr = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if (telecommMgr == null) {
            Log.i(TAG, "isVideoCallEnabled: TelecomManager is not available.");
            return false;
        }
        List<PhoneAccountHandle> phoneAccountHandles = telecommMgr.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle handle : phoneAccountHandles) {
            final PhoneAccount phoneAccount = telecommMgr.getPhoneAccount(handle);
            if (hasCapability(phoneAccount, PhoneAccount.CAPABILITY_VIDEO_CALLING)) {
                Log.i(TAG, "isVideoCallEnabled: supported.");
                return true;
            }
        }
        Log.i(TAG, "isVideoCallEnabled: no capable phone account.");
        return false;
    }

    private static boolean hasCapability(PhoneAccount phoneAccount, int capability) {
        return (phoneAccount != null) &&
                ((phoneAccount.getCapabilities() & capability) == capability);
    }

    //#<!-- [[ YunOS BEGIN PB
    //##module:()  ##author:xiuneng.wpf@alibaba-inc.com
    //##BugID:(6737283)  ##date:2015-12-09 20:25 -->
    //##Desc: Volte
    @Override
    public boolean isVolteEnabled(Context context) {
        // FIXME: 15-12-10 please use setting -> mobileNetworkSetting -> Volte toggle replace this
        return true;
    }

    @Override
    public boolean isVolteEnhancedConfCallSupport(Context context) {
        // TODO: need to check if the device support conference call.
        return false;
    }

    VolteAttachStateListener[] mVolteListener = new VolteAttachStateListener[2];
    @Override
    public void observeVolteAttachChanged(Context context, Handler handler, int what) {
        observeVolteAttachChangedOnSlot(context, handler, what, SimUtil.SLOT_ID_1);
        if (isMultiSimEnable()) {
            observeVolteAttachChangedOnSlot(context, handler, what, SimUtil.SLOT_ID_2);
        }
    }

    private void observeVolteAttachChangedOnSlot(Context context, Handler handler, int what, int slotId) {
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if((subIds != null) && (subIds.length > 0) && SubscriptionManager.isValidSubscriptionId(subIds[0])) {
            mVolteListener[slotId] = new VolteAttachStateListener(subIds[0], handler, what);
            TelephonyManager.getDefault().listen(mVolteListener[slotId],
                    PhoneStateListener.LISTEN_SERVICE_STATE | PhoneStateListener.LISTEN_VOLTE_STATE);
        }
    }

    @Override
    public void unObserveVolteAttachChanged(Context context) {
        TelephonyManager.getDefault().listen(mVolteListener[SimUtil.SLOT_ID_1], PhoneStateListener.LISTEN_NONE);
        mVolteListener[SimUtil.SLOT_ID_1] = null;
        if (isMultiSimEnable()) {
            TelephonyManager.getDefault().listen(mVolteListener[SimUtil.SLOT_ID_2], PhoneStateListener.LISTEN_NONE);
            mVolteListener[SimUtil.SLOT_ID_2] = null;
        }
    }

    private class VolteAttachStateListener extends PhoneStateListener {
        // FIXME: IMS_REGISTERED and IMS_UNREGISTERED shall be defined in VoLteServiceState,
        // but in QCom M, the constants are not ported yet.
        // So temporary define them here and remove them after VoLteServiceState ready.
        public static final int IMS_REGISTERED = 4;
        public static final int IMS_UNREGISTERED = 5;

        private Handler mHandler = null;
        private int mWhat;
        private boolean mIsAttachVolte = false;
        private boolean mIsAttachLte = false;

        public VolteAttachStateListener(int subId, Handler handler, int what) {
            super(subId);
            mHandler = handler;
            mWhat = what;
        }

        @Override
        public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
            if (SubscriptionManager.getDefaultDataSubId() == mSubId) {
                mIsAttachVolte = (serviceState.getSrvccState() == /*VoLteServiceState.*/IMS_REGISTERED);
                Message msg = mHandler.obtainMessage(mWhat);
                msg.arg1 = mSubId;
                msg.arg2 = mIsAttachLte && mIsAttachVolte ? 1 : 0;
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (SubscriptionManager.getDefaultDataSubId() == mSubId) {
                mIsAttachLte = (serviceState.getVoiceNetworkType() == TelephonyManager.NETWORK_TYPE_LTE
                        || serviceState.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE);
                Message msg = mHandler.obtainMessage(mWhat);
                msg.arg1 = mSubId;
                msg.arg2 = mIsAttachLte && mIsAttachVolte ? 1 : 0;
                mHandler.sendMessage(msg);
            }
        }

    }
    //#<!-- YunOS END PB ]] -->
}
