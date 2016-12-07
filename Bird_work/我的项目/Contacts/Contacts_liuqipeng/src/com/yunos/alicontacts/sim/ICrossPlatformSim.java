
package com.yunos.alicontacts.sim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

public interface ICrossPlatformSim {
    public static final String TAG = "SimUtil";

    public static final int SLOT_ID_1 = 0;
    public static final int SLOT_ID_2 = 1;
    /** An invalid slot identifier */
    public static final int INVALID_SLOT_ID = -1000; // SubscriptionManager.INVALID_SLOT_ID;
    /** Indicates the caller wants the default slot id. */
    public static final int DEFAULT_SLOT_ID = Integer.MAX_VALUE; // SubscriptionManager.DEFAULT_SLOT_ID;
    /** An invalid subscription identifier */
    public static final int INVALID_SUB_ID = -1000; // SubscriptionManager.INVALID_SUB_ID;
    /** Indicates the caller wants the default sub id. */
    public static final int DEFAULT_SUB_ID = Integer.MAX_VALUE; // SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;

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

    /**
     * isMultiSimEnable returns true when device supports dual SIM card standby.
     */
    public boolean isMultiSimEnable();

    /**
     * hasIccCard() means SIM card is inserted or not. if inserted, return true,
     * if not, return false.
     */
    public boolean hasIccCard();

    /**
     * hasIccCard() means SIM card is inserted or not. if inserted, return true,
     * if not, return false.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public boolean hasIccCard(int slotId);

    /**
     * it returns true if SIM is ready.
     */
    public boolean isSimReady();

    /**
     * it returns true if SIM is ready.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public boolean isSimReady(int slotId);

    /**
     * isSubActive() means Radio is ON and SIM is ready. if both are true,
     * return true, or return false.
     */
    public boolean isSubActive();

    /**
     * isSubActive() means Radio is ON and SIM is ready. if both are true,
     * return true, or return false.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public boolean isSubActive(int slotId);

    /**
     * isCardEnable() means insert SIM card and isSubActive() is true. if both
     * are true, return true, or return false.
     */
    public boolean isCardEnable();

    /**
     * isCardEnable() means insert SIM card and isSubActive() is true. if both
     * are true, return true, or return false.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public boolean isCardEnable(int slotId);

    /**
     * isSimAvailable() return true when SIM state is NOT ABSENT and SIM is NOT
     * DEACTIVATED, or return false.
     */
    public boolean isSimAvailable();

    /**
     * isSimAvailable() return true when SIM state is NOT ABSENT and SIM is NOT
     * DEACTIVATED, or return false.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public boolean isSimAvailable(int slotId);

    /**
     * getSimState() return SIM state.
     *
     * @return SimUtil.SIM_STATE_UNKNOWN, SimUtil.SIM_STATE_ABSENT,
     *         SimUtil.SIM_STATE_PIN_REQUIRED, SimUtil.SIM_STATE_PUK_REQUIRED,
     *         SimUtil.SIM_STATE_NETWORK_LOCKED, SimUtil.SIM_STATE_READY
     */
    public int getSimState();

    /**
     * getSimState() return SIM state.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     * @return SimUtil.SIM_STATE_UNKNOWN, SimUtil.SIM_STATE_ABSENT,
     *         SimUtil.SIM_STATE_PIN_REQUIRED, SimUtil.SIM_STATE_PUK_REQUIRED,
     *         SimUtil.SIM_STATE_NETWORK_LOCKED, SimUtil.SIM_STATE_READY
     */
    public int getSimState(int slotId);

    /**
     * activeSimCount() means the counts of SIM card is isSubActive(). that is,
     * the count of active SIM card.
     */
    public int getActiveSimCount(Context context);

    /**
     * isNoSimCard() means not SIM card.
     */
    public boolean isNoSimCard();

    /**
     * is2gSim() means SIM card type is not USIM, and it return true, if is USIM
     * type, return false. default return true.
     */
    public boolean is2gSim();

    /**
     * is2gSim() means SIM card type is not USIM, and it return true, if is USIM
     * type, return false. default return true.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public boolean is2gSim(int slotId);

    /**
     * it return the default call slot id, it may be SIM1/SIM2, it decides which
     * SIM card you choose in Settings application. if choose SIM1, when you
     * have a outgoing call, it dials by SIM1, not SIM2.
     */
    public int getDefaultVoiceSlotId();

    /**
     * it retrieve network operator name.
     */
    public String getNetworkOperatorName(Context context);

    /**
     * it retrieve network operator name.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public String getNetworkOperatorName(Context context, int slotId);

    /**
     * it retrieve SIM operator name.
     */
    public String getSimOperatorName(Context context);

    /**
     * it retrieve SIM operator name.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public String getSimOperatorName(Context context, int slotId);

    /**
     * it retrieve SIM card display name.
     */
    public String getSimCardDisplayName(Context context);

    /**
     * it retrieve SIM card display name.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public String getSimCardDisplayName(Context context, int slotId);

    /**
     * it retrieve SIM icc id.
     */
    public String getSimSerialNumber();

    /**
     * it retrieve SIM icc id.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public String getSimSerialNumber(int slotId);

    /**
     * get slotId by subId.
     */
    public int getSlotIdBySubId(int subId);

    /**
     * get subIds by slotId.
     */
    public int[] getSubIdsBySlotId(int slotId);

    /**
     * get subId by slotId.
     */
    public int getSubIdBySlotId(int slotId);

    /**
     * get default subId,always used when single card phone.
     */
    public int getDefaultSubId();

    /**
     * isRadioOn() means Radio is on or off.
     */
    public boolean isRadioOn();

    /**
     * isRadioOn() means Radio is on or off.
     *
     * @param slotId means sim1 or sim2, we can use SimUtil.SLOT_ID_1 or
     *            SimUtil.SLOT_ID_2
     */
    public boolean isRadioOn(int slotId);

    /**
     * Original interface for query.
     */
    public Cursor query(Context context, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder);

    /**
     * Original interface for insert.
     */
    public Uri insert(Context context, Uri uri, ContentValues values);

    /**
     * Original interface for delete.
     */
    public int delete(Context context, Uri uri, String where, String[] whereArgs);

    /**
     * Original interface for update.
     */
    public int update(Context context, Uri uri, ContentValues values, String where,
            String[] selectionArgs);

    public void log(String msg);

    public boolean isVolteEnhancedConfCallSupport(Context context);
    public boolean isVideoCallEnabled(Context context);

    //#<!-- [[ YunOS BEGIN PB
    //##module:()  ##author:xiuneng.wpf@alibaba-inc.com
    //##BugID:(6737283)  ##date:2015-12-09 20:25 -->
    //##Desc: Volte
    public void observeVolteAttachChanged(Context context, Handler handler, int what);
    public void unObserveVolteAttachChanged(Context context);
    public boolean isVolteEnabled(Context context);
    //#<!-- YunOS END PB ]] -->

    /* YUNOS BEGIN PB */
    //##email:caixiang.zcx@alibaba-inc.com
    //##BugID:(8206447) ##date:2016/05/12
    //##description:get the capacity of sim card
    public int getMSimCardMaxCount(int slotId);

    public int getSpareEmailCount(int slotId);
    /* YUNOS END PB */
}
