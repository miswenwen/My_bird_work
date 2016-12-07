package com.yunos.alicontacts.sim;

import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.yunos.alicontacts.util.AliTextUtils;

import java.util.ArrayList;
import java.util.List;

public class SimContactCache {

    public static class SimContact {
        public final short slotId;
        public final short simIndex;
        public final String name;
        public final String number;
        public final String anrs;
        public final String emails;
        public long rawContactId = -1;

        public static void updateRawContactIdsWithResults(
                SparseArray<SimContact> simContacts, ContentProviderResult[] results) {
            int count = simContacts.size();
            for (int i = 0; i < count; i++) {
                int key = simContacts.keyAt(i);
                SimContact value = simContacts.valueAt(i);
                if (results.length > key) {
                    value.updateRawContactIdWithResult(results[key]);
                }
            }
        }

        public SimContact(short slotId, short simIndex, String name, String number, String anr, String emails) {
            this.slotId = slotId;
            this.simIndex = simIndex;
            this.name = name;
            this.number = number;
            this.anrs = TextUtils.isEmpty(anr) ? null : anr;
            this.emails = TextUtils.isEmpty(emails) ? null : emails;
        }

        public SimContact(short slotId, short simIndex, String name, String number) {
            this(slotId, simIndex, name, number, null, null);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SimContact:{slotId=").append(slotId)
              .append(";simIndex=").append(simIndex)
              .append(";name=\"").append(name)
              .append("\";number=\"").append(AliTextUtils.desensitizeNumber(number))
              .append("\";anrs=\"").append(AliTextUtils.desensitizeNumber(anrs))
              .append("\";emails=\"").append(emails)
              .append("\";rawContactId=").append(rawContactId)
              .append(";}");
            return sb.toString();
        }

        public ContentValues toContentValues() {
            ContentValues values = new ContentValues();
            if (SimUtil.IS_PLATFORM_MTK || SimUtil.IS_PLATFORM_SPREADTRUM) {
                // it is required to cast to int when put to ContentValues.
                values.put(SimUtil.SIM_INDEX, (int) simIndex);
            }
            /* YUNOS BEGIN PB */
            //##email:caixiang.zcx@alibaba-inc.com
            //##BugID:(8526695) ##date:2016/07/27
            //##description:when name or number is null,name or number will became a string "null"
            if (!TextUtils.isEmpty(name)) {
                values.put(SimUtil.SIM_NAME, name);
            }
            if (!TextUtils.isEmpty(number)) {
                values.put(SimUtil.SIM_NUMBER, number);
            }
            /* YUNOS END PB */
            if (!TextUtils.isEmpty(anrs)) {
                values.put(SimUtil.SIM_ANR, anrs);
            }
            if (!TextUtils.isEmpty(emails)) {
                values.put(SimUtil.SIM_EMAILS, emails);
            }
            return values;
        }

        public boolean contentEquals(SimContact another) {
            return TextUtils.equals(name, another.name)
                    && TextUtils.equals(number, another.number)
                    && TextUtils.equals(anrs, another.anrs)
                    && TextUtils.equals(emails, another.emails);
        }

        public boolean sameContent(String name, String number, String anr, String emails) {
            return sameStringLoosely(this.name, name)
                    && sameStringLoosely(this.number, number)
                    && sameStringLoosely(this.anrs, anr)
                    && sameStringLoosely(this.emails, emails);
        }

        /**
         * Compare 2 strings. "" and null are treated as the same.
         * @param str1
         * @param str2
         * @return
         */
        public static boolean sameStringLoosely(String str1, String str2) {
            if (TextUtils.isEmpty(str1)) {
                return TextUtils.isEmpty(str2);
            }
            return str1.equals(str2);
        }

        public boolean updateRawContactIdWithResult(ContentProviderResult result) {
            if ((result == null) || (result.uri == null)) {
                return false;
            }
            String idStr = result.uri.getLastPathSegment();
            try {
                rawContactId = Long.parseLong(idStr);
                return true;
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "updateRawContactIdWithResult: wront id "+idStr, nfe);
            }
            return false;
        }

    }

    private static final String DUMP_TAG = "DUMP_SimContactCache";
    private static final String TAG = "SimContactCache";

    private static final String[] DATA_PROJECTION_FOR_SIM_CONTACTS = {
        Data.RAW_CONTACT_ID, Data.MIMETYPE, Data.DATA1, Data.DATA2
    };
    private static final int COL_RAW_CONTACT_ID = 0;
    private static final int COL_MIMETYPE = 1;
    private static final int COL_DATA1_DATA = 2;
    private static final int COL_DATA2_TYPE = 3;

    private static final String SIM_CONTACTS_DATA_ORDERBY = Data.RAW_CONTACT_ID + " asc";

    private static final SimContactCache[] sCaches
            = new SimContactCache[SimUtil.MULTISIM_ENABLE ? 2 : 1];

    private List<SimContact> mSimContacts;

    public SimContactCache(int initCapacity) {
        mSimContacts = new ArrayList<SimContact>(initCapacity);
    }

    private SimContactCache(SimContactCache dup) {
        mSimContacts = new ArrayList<SimContact>(dup.mSimContacts);
    }

    public static SimContactCache getCache(int slotId) {
        SimContactCache cache = null;
        switch (slotId) {
            case SimUtil.SLOT_ID_1:
                cache = sCaches[0];
                break;
            case SimUtil.SLOT_ID_2:
                cache = sCaches[1];
                break;
            default:
                Log.e(TAG, "getCache: Invalid slotId "+slotId);
                break;
        }
        return cache;
    }

    public static void clearCache(int slotId) {
        switch (slotId) {
            case SimUtil.SLOT_ID_1:
                sCaches[0] = null;
                break;
            case SimUtil.SLOT_ID_2:
                sCaches[1] = null;
                break;
            default:
                Log.e(TAG, "clearCache: Invalid slotId "+slotId);
                break;
        }
    }

    public static void putCache(int slotId, SimContactCache cache) {
        switch (slotId) {
            case SimUtil.SLOT_ID_1:
                sCaches[0] = cache;
                break;
            case SimUtil.SLOT_ID_2:
                sCaches[1] = cache;
                break;
            default:
                Log.e(TAG, "putCache: Invalid slotId "+slotId);
                break;
        }
    }

    public static SimContactCache buildCacheFromSimCursor(Cursor simCursor, int slotId) {
        int count = simCursor == null ? 0 : simCursor.getCount();
        SimContactCache cache = new SimContactCache(count);
        if (count <= 0) {
            return cache;
        }
        int simIndex;
        String name, number, anrs = null, emails = null;
        int colCount = simCursor.getColumnCount();
        simCursor.moveToPosition(-1);
        for (int i = 0; i < count; i++) {
            simCursor.moveToNext();
            if (SimUtil.IS_PLATFORM_MTK || SimUtil.IS_PLATFORM_SPREADTRUM) {
                simIndex = simCursor.getInt(SimUtil.SIM_INDEX_COLUMN);
            } else {
                simIndex = i+1;
            }
            name = simCursor.getString(SimUtil.SIM_NAME_COLUMN);
            number = simCursor.getString(SimUtil.SIM_NUMBER_COLUMN);
            if (SimUtil.SIM_ANR_COLUMN < colCount) {
                anrs = simCursor.getString(SimUtil.SIM_ANR_COLUMN);
            }
            if (SimUtil.SIM_EMAILS_COLUMN < colCount) {
                emails = simCursor.getString(SimUtil.SIM_EMAILS_COLUMN);
            }
            cache.mSimContacts.add(new SimContact((short) slotId, (short) simIndex, name, number, anrs, emails));
        }
        return cache;
    }

    public static SimContactCache buildCacheFromPhoneDb(ContentResolver resolver, int slotId) {
        final String selection = SimContactLoadService.getSimContactsSelection(slotId);
        Cursor dataCursor = null;
        SimContact contact;
        SimContactCache cache = new SimContactCache(250); // half of 3g/4g capacity.
        try {
            dataCursor = resolver.query(
                    Data.CONTENT_URI,
                    DATA_PROJECTION_FOR_SIM_CONTACTS,
                    selection,
                    null,
                    SIM_CONTACTS_DATA_ORDERBY);
            if (dataCursor == null) {
                return null;
            }
            while ((contact = buildSimContactFromDataCursor(dataCursor, slotId)) != null) {
                Log.i(TAG, "buildCacheFromPhoneDb: add raw contact id "+contact.rawContactId);
                cache.mSimContacts.add(contact);
            }
        } finally {
            if (dataCursor != null) {
                dataCursor.close();
            }
        }
        return cache;
    }

    private static SimContact buildSimContactFromDataCursor(Cursor cursor, int slotId) {
        long rawContactId = -1;
        String name = null, number = null, anrs = null, emails = null;
        String mimetype, data;
        boolean hasData = false;
        while (cursor.moveToNext()) {
            if (rawContactId == -1) {
                rawContactId = cursor.getLong(COL_RAW_CONTACT_ID);
            } else if (rawContactId != cursor.getLong(COL_RAW_CONTACT_ID)) {
                cursor.moveToPrevious();
                break;
            }
            mimetype = cursor.getString(COL_MIMETYPE);
            data = cursor.getString(COL_DATA1_DATA);
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimetype)) {
                name = data;
                hasData = true;
            } else if (Phone.CONTENT_ITEM_TYPE.equals(mimetype)) {
                int type = cursor.getInt(COL_DATA2_TYPE);
                if (Phone.TYPE_MOBILE == type) {
                    number = data;
                    hasData = true;
                } else if (Phone.TYPE_HOME == type) {
                    anrs = data;
                    hasData = true;
                } else {
                    Log.w(TAG, "buildSimContactFromDataCursor: unrecognized phone type "+type);
                }
            } else if (Email.CONTENT_ITEM_TYPE.equals(mimetype)) {
                emails = data;
                hasData = true;
            } else {
                Log.w(TAG, "buildSimContactFromDataCursor: unrecognized mimetype "+mimetype);
            }
        }
        if (hasData) {
            SimContact contact = new SimContact((short) slotId, (short) -1, name, number, anrs, emails);
            contact.rawContactId = rawContactId;
            return contact;
        }
        return null;
    }

    public static SimContact getSimContactByRawContactIdWithoutSimId(long rawContactId) {
        for (SimContactCache cache : sCaches) {
            if (cache == null) {
                continue;
            }
            SimContact result = cache.getSimContactByRawContactId(rawContactId);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public static boolean deleteSimContact(SimContact contact) {
        int slotId = contact.slotId;
        SimContactCache cache = SimContactCache.getCache(slotId);
        if (cache == null) {
            return false;
        }
        return cache.deleteSimContactInternal(contact);
    }

    public static boolean insertSimContact(
            long rawContactId,
            int slotId,
            int simIndex,
            String name,
            String number,
            String anrs,
            String emails) {
        SimContactCache cache = getCache(slotId);
        if (cache == null) {
            return false;
        }
        SimContact contact = new SimContact((short) slotId, (short) simIndex, name, number, anrs, emails);
        contact.rawContactId = rawContactId;
        cache.addSimContactInternal(contact);
        return true;
    }

    public static boolean updateSimContact(
            long rawContactId,
            int slotId,
            int simIndex,
            ContentValues before,
            ContentValues after) {
        SimContactCache cache = getCache(slotId);
        if (cache == null) {
            return false;
        }
        SimContact contact = null;
        List<SimContact> working = cache.mSimContacts;
        synchronized (working) {
            int count = working.size();
            int pos = -1;
            for (int i = 0; i < count; i++) {
                SimContact temp = working.get(i);
                if (temp.rawContactId == rawContactId) {
                    contact = temp;
                    pos = i;
                    break;
                }
            }
            if (contact == null) {
                return false;
            }
            if (SimUtil.MULTISIM_ENABLE && (contact.slotId != slotId)) {
                return false;
            }
            if ((SimUtil.IS_PLATFORM_MTK || SimUtil.IS_PLATFORM_SPREADTRUM)
                    && (contact.simIndex != simIndex)) {
                return false;
            }
            boolean same = contact.sameContent(
                    before.getAsString(SimUtil.SIM_NAME),
                    before.getAsString(SimUtil.SIM_NUMBER),
                    before.getAsString(SimUtil.SIM_ANR),
                    before.getAsString(SimUtil.SIM_EMAILS));
            if (!same) {
                return false;
            }
            working.remove(pos);
            contact = new SimContact(
                    (short) slotId,
                    (short) simIndex,
                    after.getAsString(SimUtil.SIM_NAME),
                    after.getAsString(SimUtil.SIM_NUMBER),
                    after.getAsString(SimUtil.SIM_ANR),
                    after.getAsString(SimUtil.SIM_EMAILS));
            contact.rawContactId = rawContactId;
            working.add(pos, contact);
        }
        return true;
    }

    public SimContactCache duplicate() {
        SimContactCache dup;
        List<SimContact> working = mSimContacts;
        synchronized (working) {
            dup = new SimContactCache(this);
        }
        return dup;
    }

    public int getCount() {
        return mSimContacts.size();
    }

    public SimContact getSimContact(int position) {
        if (position < mSimContacts.size()) {
            return mSimContacts.get(position);
        }
        return null;
    }

    public SimContact getSimContactByRawContactId(long rawContactId) {
        List<SimContact> working = mSimContacts;
        synchronized (working) {
            for (SimContact contact : working) {
                if (contact.rawContactId == rawContactId) {
                    return contact;
                }
            }
        }
        return null;
    }

    public SimContact getSimContact(String name, String number, String anr, String emails) {
        List<SimContact> working = mSimContacts;
        synchronized (working) {
            for (SimContact contact : working) {
                if (contact.sameContent(name, number, anr, emails)) {
                    return contact;
                }
            }
        }
        return null;
    }

    public static void dumpCache(int slotId) {
        SimContactCache cache = getCache(slotId);
        if (cache == null) {
            Log.i(DUMP_TAG, "dumpCache: null for slotId "+slotId);
            return;
        }
        Log.i(DUMP_TAG, "dumpCache: dump for slotId: "+slotId+"; cache="+cache);
        cache.dump();
    }

    public void dump() {
        int count = mSimContacts.size();
        Log.i(DUMP_TAG, "dump: size="+count);
        for (int i = 0; i < count; i++) {
            SimContact contact = mSimContacts.get(i);
            Log.i(DUMP_TAG, "dump: ["+i+"]:"+contact);
        }
    }

    private boolean deleteSimContactInternal(SimContact contact) {
        boolean found = false;
        List<SimContact> working = mSimContacts;
        synchronized (working) {
            int count = working.size();
            for (int i = 0; i < count; i++) {
                SimContact contactForComp = working.get(i);
                if (SimUtil.IS_PLATFORM_MTK || SimUtil.IS_PLATFORM_SPREADTRUM) {
                    if (contactForComp.simIndex == contact.simIndex) {
                        Log.i(TAG, "deleteSimContactInternal: found sim contact "+contact+" in cache, delete.");
                        working.remove(i);
                        found = true;
                        break;
                    }
                } else {
                    if ((contactForComp.rawContactId == contact.rawContactId) && contactForComp.equals(contact)) {
                        Log.i(TAG, "deleteSimContactInternal: found sim contact "+contactForComp+" in cache, delete.");
                        working.remove(i);
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    private void addSimContactInternal(SimContact contact) {
        mSimContacts.add(contact);
    }

}
