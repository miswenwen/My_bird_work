
package com.yunos.alicontacts.sim;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.yunos.alicontacts.platform.PDUtils;

/**
 * FIXME need to implement on Spreadtrum platform.
 * <p>
 */

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

        return true;
    }

    @Override
    public int getDefaultVoiceSlotId() {
        long subId = SubscriptionManager.getDefaultVoiceSubId();
        return getSlotIdBySubId((int) subId);
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
        SubscriptionInfo subInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            return subInfo.getDisplayName().toString();
        }

        return null;
    }

    @Override
    public String getSimCardDisplayName(Context context, int slotId) {
        final int subId = getSubIdBySlotId(slotId);
        SubscriptionInfo subInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(subId);
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
        int[] temp = SubscriptionManager.getSubId(slotId);
        if (temp == null) {
            return null;
        }
        int[] ret = new int[temp.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (int)(temp[i]);
        }
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
        return (int) SubscriptionManager.getDefaultSubId();
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
        Log.d(TAG, "[Spreadtrum]---" + msg);
    }

    @Override
    public boolean isVideoCallEnabled(Context context) {
        boolean result = SystemProperties.getBoolean("persist.sys.support.vt", false);
        Log.i(TAG, "isVideoCallEnabled: read from config "+result);
        return result;
    }

    //#<!-- [[ YunOS BEGIN PB
    //##module:()  ##author:xiuneng.wpf@alibaba-inc.com
    //##BugID:(6737283)  ##date:2015-12-09 20:25 -->
    //##Desc: Volte
    public boolean isVolteEnabled(Context context) {
        return TelephonyManager.from(context).isVolteEnabled();
    }

    @Override
    public boolean isVolteEnhancedConfCallSupport(Context context) {
        // TODO: need to check if the device support conference call.
        return true;
    }

    VolteAttachStateListener mVolteListener = null;
    @Override
    public void observeVolteAttachChanged(Context context, Handler handler, int what) {
        int slotId = TelephonyManager.from(context).getPrimaryCard();
        int subId = getSubIdBySlotId(slotId);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            mVolteListener = new VolteAttachStateListener(subId, handler, what);
            tm.listen(mVolteListener,
                    PhoneStateListener.LISTEN_SERVICE_STATE | PhoneStateListener.LISTEN_VOLTE_STATE);
        }
    }

    public void unObserveVolteAttachChanged(Context context) {
        if (mVolteListener != null) {
            TelephonyManager.getDefault().listen(mVolteListener, PhoneStateListener.LISTEN_NONE);
            mVolteListener = null;
        }
    }

    class VolteAttachStateListener extends PhoneStateListener {
        private Handler mHandler = null;
        private int mWhat;

        private boolean mIsAttachVolte = false;
        private boolean mIsAttachLte = false;
        private boolean mImsInited = false;
        private boolean mSrvStateInited =false;

        public VolteAttachStateListener(int subId, Handler handler, int what) {
            super(subId);
            mHandler = handler;
            mWhat = what;
        }

        @Override
        public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
            mIsAttachVolte = (serviceState.getSrvccState() == VoLteServiceState.IMS_REG_STATE_REGISTERED);
            mImsInited = true;
            if (mSrvStateInited && mImsInited) {
                Message msg = mHandler.obtainMessage(mWhat);
                msg.arg1 = mSubId;
                msg.arg2 = mIsAttachLte && mIsAttachVolte ? 1 : 0;
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            mIsAttachLte = (serviceState.getVoiceNetworkType() == TelephonyManager.NETWORK_TYPE_LTE
                    || serviceState.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE);
            mSrvStateInited = true;
            if (mSrvStateInited && mImsInited) {
                Message msg = mHandler.obtainMessage(mWhat);
                msg.arg1 = mSubId;
                msg.arg2 = mIsAttachLte && mIsAttachVolte ? 1 : 0;
                mHandler.sendMessage(msg);
            }
        }
    }
    //#<!-- YunOS END PB ]] -->

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
