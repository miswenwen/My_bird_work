
package com.yunos.alicontacts.sim;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;

public class SimContactLoadService extends IntentService {
    private static final String TAG = "SimContactLoad";

    public static final String ACTION_LOAD_SIM_CONTACTS_TO_PHONE_DB
            = "com.yunos.alicontacts.ACTION_LOAD_SIM_CONTACTS_TO_PHONE_DB";

    public static final String ACTION_DELETE_SIM_CONTACTS_FROM_PHONE_DB
            = "com.yunos.alicontacts.ACTION_DELETE_SIM_CONTACTS_FROM_PHONE_DB";

    public static final String INTENT_KEY_SLOT_ID = "slot_id";

    /**
     * Query all SIM contacts in database (single SIM).
     */
    private static final String SIM_DATABASE_SELECTION =
            RawContacts.ACCOUNT_NAME + "='" + SimContactUtils.SIM_ACCOUNT_NAME + "' AND "
            + RawContacts.ACCOUNT_TYPE + "='" + SimContactUtils.SIM_ACCOUNT_TYPE + "'";

    /**
     * Query all SIM1 contacts in database only in Dual-SIM mode.
     */
    private static final String SIM_DATABASE_SELECTION_ON_SUB0 =
            RawContacts.ACCOUNT_NAME + "='" + SimContactUtils.SIM_ACCOUNT_NAME_SIM1 + "' AND "
            + RawContacts.ACCOUNT_TYPE + "='" + SimContactUtils.SIM_ACCOUNT_TYPE + "'";

    /**
     * Query all SIM2 contacts in database only in Dual-SIM mode.
     */
    private static final String SIM_DATABASE_SELECTION_ON_SUB1 =
            RawContacts.ACCOUNT_NAME + "='" + SimContactUtils.SIM_ACCOUNT_NAME_SIM2 + "' AND "
            + RawContacts.ACCOUNT_TYPE + "='" + SimContactUtils.SIM_ACCOUNT_TYPE + "'";

    /** the sim contact count for never loaded and scheduled a load. -1 is used for null cursor result, so start from -2 here. */
    public static final int SCHEDULE_LOAD_COUNT = -2;
    public static final int NOT_LOADED_COUNT = -3;
    private static int[] sSimLoadedCount = new int[] {
        NOT_LOADED_COUNT, NOT_LOADED_COUNT
    };

    private static final int BATCH_COUNT = 40;

    public SimContactLoadService() {
        super(TAG);
        setIntentRedelivery(true);
    }

    public static void notifyReloadSimContacts(Context context, int slot) {
        Log.i(TAG, "notifyReloadSimContacts: slot="+slot);
        Intent intent = new Intent(ACTION_LOAD_SIM_CONTACTS_TO_PHONE_DB);
        intent.setClass(context.getApplicationContext(), SimContactLoadService.class);
        intent.putExtra(INTENT_KEY_SLOT_ID, slot);
        context.startService(intent);
    }

    public static int getSimLoadedCount(int slot) {
        if (slot < SimUtil.SLOT_ID_1) {
            slot = SimUtil.SLOT_ID_1; // for SimUtil.SINGLE_SIM_CARD_MODEL_DEFAULT_SLOT_ID
        }
        return sSimLoadedCount[slot];
    }

    public static void setSimScheduleLoading(int slot) {
        setSimLoadedCount(slot, SCHEDULE_LOAD_COUNT);
    }

    private static void setSimLoadedCount(int slot, int loadedCount) {
        if (slot < 0) {
            slot = 0;
        }
        sSimLoadedCount[slot] = loadedCount;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        int slotId = SimUtil.INVALID_SLOT_ID;
        int subId = SimUtil.INVALID_SUB_ID;
        // FIXME: The extra key should be changed to correct value.
        slotId = intent.getIntExtra(INTENT_KEY_SLOT_ID, SimUtil.INVALID_SLOT_ID);
        subId = intent.getIntExtra(SimContactUtils.SUBSCRIPTION_KEY, SimUtil.INVALID_SUB_ID);

        log("onHandleIntent() action:" + action + " on slot:" + slotId + ", subId:" + subId);
        if (slotId == SimUtil.INVALID_SLOT_ID && subId != SimUtil.INVALID_SUB_ID) {
            slotId = SimUtil.getSlotId(subId);
            log("onHandleIntent() change slotId:" + slotId + " by subId:" + subId);
            if (slotId == SimUtil.INVALID_SLOT_ID) {
                Log.e(TAG, "onHandleIntent() slotId is invalid!");
                return;
            }
        }

        if (ACTION_LOAD_SIM_CONTACTS_TO_PHONE_DB.equals(action)) {
            Log.i(TAG, "onHandleIntent: load on slotId "+slotId);
            handleSimLoad(slotId);
        } else if (ACTION_DELETE_SIM_CONTACTS_FROM_PHONE_DB.equals(action)) {
            Log.i(TAG, "onHandleIntent: delete on slotId "+slotId);
            deleteSimContactsInDB(slotId);
            setSimLoadedCount(slotId, 0);
        }
    }

    /**
     * SIM LOAD PROCESS:
     * <p>
     * step1: Query SIM records.
     * <p>
     * step1.1: If total count of SIM records(in short "simCount") is greater
     * than zero, goto step2.
     * <p>
     * step1.2: If simCount is equal to zero, re-query SIM records
     * {@link SIM_RELOAD_MAX_TIMES} times, if simCount is still equal to zero,
     * delete all SIM contacts in database. goto step4.
     * <p>
     * step2: query SIM contacts in database.
     * <p>
     * step2.1: If total count of SIM contacts(in short "dbCount") is equal to
     * zero, import all SIM records directly, then goto step4.
     * <p>
     * step2.2: If dbCount is greater than zero, we compare simCount to dbCount.
     * <p>
     * step2.2.1: If simCount is not equal to dbCount, we delete all SIM
     * contacts in database and import all SIM records , then goto step4.
     * <p>
     * step2.2.2: If simCount is equal to dbCount, we compare the first value of
     * simCount(in short "simValue") in cursor to dbCount(in short "dbValue"),
     * if simValue is not equal to dbValue, delete all SIM contacts in database
     * and then import all SIM records, if simValue is equal to dbValue, goto
     * step3.
     * <p>
     * step3: Update SIM records to SIM contacts one by one.
     * <p>
     * step4. SIM load process end.
     */
    private void handleSimLoad(final int slotId) {
        log("handleSimLoad() on slot" + slotId);

        Cursor simCursor = null;
        try {
            if (SimUtil.MULTISIM_ENABLE) {
                simCursor = SimUtil.query(this, slotId);
            } else {
                simCursor = SimUtil.query(this);
            }

            final int simCount = (simCursor == null) ? -1 : simCursor.getCount();
            if (simCount > 0) {
                SimContactCache oldCache = getSimContactsFromDbAsCache(slotId);
                int oldCacheCount = oldCache == null ? -1 : oldCache.getCount();
                SimContactCache newCache = SimContactCache.buildCacheFromSimCursor(simCursor, slotId);

                if (oldCacheCount > 0) {
                    if ((oldCacheCount != simCount)
                            || !compareFirstSimContactValue(oldCache, newCache, slotId)) {
                        log("handleSimLoad() delete all SIM contacts in database and import from SIM on slotId" + slotId);
                        // 1. delete SIM contacts in database.
                        deleteSimContactsInDB(slotId);
                        SimContactCache.clearCache(slotId);
                        // 2. import SIM records.
                        insertSimRecordsToDB(newCache, 0, slotId);
                        SimContactCache.putCache(slotId, newCache);
                    } else {
                        log("handleSimLoad() update SIM contacts in database on slotId" + slotId);
                        updateSimContactInPhoneDatabase(oldCache, newCache, slotId);
                        SimContactCache.putCache(slotId, newCache);
                    }
                } else {
                    log("handleSimLoad() import from SIM directly on slotId" + slotId);
                    insertSimRecordsToDB(newCache, 0, slotId);
                    SimContactCache.putCache(slotId, newCache);
                }

            } else {
                log("handleSimLoad() has not SIM records on slotId" + slotId);
                deleteSimContactsInDB(slotId);
                SimContactCache.clearCache(slotId);
            }

            setSimLoadedCount(slotId, simCount);
            log("handleSimLoad() SIM load completely on slotId" + slotId);
        } catch (Exception e) {
            Log.e(TAG, "handleSimLoad() Exception on slotId" + slotId, e);
        } finally {
            if (simCursor != null) {
                simCursor.close();
            }
        }

    }

    private boolean compareFirstSimContactValue(final SimContactCache oldCache,
            final SimContactCache newCache, final int slotId) {
        SimContactCache.SimContact firstInOldCache = oldCache.getSimContact(0);
        if (firstInOldCache == null) {
            return false;
        }
        SimContactCache.SimContact firstInNewCache = newCache.getSimContact(0);
        long rawContactId = firstInOldCache.rawContactId;
        log("compareFirstSimContactValue() rawContactId:" + rawContactId + " on slotId" + slotId);
        ContentValues cacheValues = firstInOldCache.toContentValues();
        ContentValues simValues = firstInNewCache.toContentValues();
        // NOTE: do not compare sim index, because we can not get sim index from db
        cacheValues.remove(SimUtil.SIM_INDEX);
        simValues.remove(SimUtil.SIM_INDEX);

        if (!cacheValues.equals(simValues)) {
            return false;
        }

        return true;
    }

    private void updateSimContactInPhoneDatabase(final SimContactCache oldCache, final SimContactCache newCache, final int slotId) {
        int oldCount = oldCache == null ? -1 : oldCache.getCount();
        int newCount = newCache == null ? -1 : newCache.getCount();
        log("updateSimContactInPhoneDatabase() on slotId " + slotId+"; oldCount="+oldCount+"; newCount="+newCount);
        if ((oldCount <= 0) && (newCount <= 0)) {
            Log.e(TAG, "updateSimContactInPhoneDatabase() no sim contact on slotId" + slotId);
            setSimLoadedCount(slotId, newCount);
            return;
        }

        final ContentResolver resolver = getContentResolver();

        int oldPos = 0, newPos = 0;
        try {
            /**
             * Compare SIM records with SIM contacts one by one.
             */
            while ((oldPos < oldCount) && (newPos < newCount)) {
                SimContactCache.SimContact oldSimContact = oldCache.getSimContact(oldPos++);
                SimContactCache.SimContact newSimContact = newCache.getSimContact(newPos++);

                if (!oldSimContact.contentEquals(newSimContact)) {
                    // IMPORTANT:
                    // Delete old record and then insert new record.
                    // Do ***NOT*** update old record.
                    // In some cases, sim raw_contact will be aggregated with other raw_contact.
                    // When reload sim contacts, this sim contact in the same sequence position
                    // might not be actually the same sim contact in previous reload.
                    // They just in the same list position.
                    // But shall not aggregate to the same raw_contact.
                    resolver.delete(
                            Uri.withAppendedPath(RawContacts.CONTENT_URI,
                                    String.valueOf(oldSimContact.rawContactId)),
                            null, null);
                    newSimContact.rawContactId = SimContactUtils.insertSimContactToPhoneDb(this, newSimContact.name, newSimContact.number, newSimContact.anrs, newSimContact.emails, slotId);
                } else {
                    newSimContact.rawContactId = oldSimContact.rawContactId;
                }

            }

            log("updateSimContactInPhoneDatabase() newPos:" + newPos + ", oldPos:" + oldPos + ", on slotId" + slotId);

            /**
             * If SIM contacts count is more than SIM records, delete the left
             * SIM contacts in database. Or import the left SIM records to
             * database.
             */
            if (oldPos < oldCount) {
                /** delete the left SIM contacts in database */
                deleteRemainderSimContactsInCache(oldCache, oldPos, resolver);
            } else if (newPos < newCount) {
                /** import the left sim contacts to database */
                insertSimRecordsToDB(newCache, newPos, slotId);
            }

        } catch (Exception e) {
            Log.e(TAG, "updateSimContactInPhoneDatabase() Exception on slotId" + slotId, e);
        }

    }

    private void deleteRemainderSimContactsInCache(SimContactCache cache, int pos, ContentResolver resolver) {
        StringBuilder where = new StringBuilder();
        where.append(RawContacts._ID + " IN (");
        int count = cache.getCount();
        for (int i = pos; i < count; i++) {
            SimContactCache.SimContact contact = cache.getSimContact(i);
            if (i != pos) {
                where.append(',');
            }
            where.append(contact.rawContactId);
        }
        where.append(')');
        resolver.delete(RawContacts.CONTENT_URI, where.toString(), null);
    }

    private void insertSimRecordsToDB(SimContactCache cache, int startPosition, int slotId) {
        int cacheCount = cache == null ? -1 : cache.getCount();
        if ((cacheCount <= 0) || (cacheCount <= startPosition)) {
            Log.e(TAG, "insertSimRecordsToDB() no cached data on slotId" + slotId);
            return;
        }

        boolean is2g = true;
        //byte[] photo = null;
        if (SimUtil.MULTISIM_ENABLE) {
            is2g = SimUtil.is2gSim(slotId);
            //photo = (slotId == SimUtil.SUBSCRIPTION_1) ? SimContactUtils.getSIM1Photo(this) : SimContactUtils.getSIM2Photo(this);
        } else {
            is2g = SimUtil.is2gSim();
            //photo = SimContactUtils.getSIMPhoto(this);
        }

        log("insertSimRecordsToDB() startPosition:" + startPosition + " on slotId:" + slotId
                + ", is2g:" + is2g);

        final String accountName;
        if (SimUtil.MULTISIM_ENABLE) {
            accountName = (slotId == SimUtil.SLOT_ID_1) ?
                      SimContactUtils.SIM_ACCOUNT_NAME_SIM1
                    : SimContactUtils.SIM_ACCOUNT_NAME_SIM2;
        } else {
            accountName = SimContactUtils.SIM_ACCOUNT_NAME;
        }
        int position = startPosition;
        int opCount = 0;
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        SparseArray<SimContactCache.SimContact> operationSimContacts = new SparseArray<SimContactCache.SimContact>(BATCH_COUNT);
        try {
            while (position < cacheCount) {
                SimContactCache.SimContact simContact = cache.getSimContact(position++);
                // for raw contact id in results.
                int backReferenceIndex = operationList.size();
                operationSimContacts.put(backReferenceIndex, simContact);

                SimContactUtils.buildInsertSimContactToDbOps(operationList,
                        accountName, simContact.name, simContact.number, simContact.anrs, simContact.emails);

                opCount++;
                // the position is incremented at getSimContact, so if it is the last record,
                // position shall equal to cacheCount.
                if ((opCount >= BATCH_COUNT) || (position == cacheCount)) {
                    try {
                        ContentProviderResult[] results
                                = getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
                        SimContactCache.SimContact.updateRawContactIdsWithResults(operationSimContacts, results);
                    } catch (RemoteException e) {
                        Log.e(TAG, "RemoteException:", e);
                    } catch (OperationApplicationException e) {
                        Log.e(TAG, "OperationApplicationException:", e);
                    } finally {
                        operationList.clear();
                        operationSimContacts.clear();
                        opCount = 0;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "insertSimRecordsToDB() Exception:", e);
        }
    }

    private void deleteSimContactsInDB(int slotId) {
        log("deleteSimContactsInDB() on slot" + slotId);

        final String selection = getSimContactsSelection(slotId);
        try {
            getContentResolver().delete(RawContacts.CONTENT_URI, selection, null);
        } catch (IllegalStateException e) {
            Log.e(TAG, "deleteSimContactsInDB IllegalStateException", e);
        }
    }

    public static String getSimContactsSelection(final int slotId) {
        final String selection;
        if (SimUtil.MULTISIM_ENABLE) {
            selection = (slotId == SimUtil.SLOT_ID_1) ? SIM_DATABASE_SELECTION_ON_SUB0
                    : SIM_DATABASE_SELECTION_ON_SUB1;
        } else {
            selection = SIM_DATABASE_SELECTION;

        }

        return selection;
    }

    private SimContactCache getSimContactsFromDbAsCache(int subId) {
        return SimContactCache.buildCacheFromPhoneDb(getContentResolver(), subId);
    }

    private void log(String msg) {
        Log.d(TAG, "[LoadService] " + msg);
    }
}
