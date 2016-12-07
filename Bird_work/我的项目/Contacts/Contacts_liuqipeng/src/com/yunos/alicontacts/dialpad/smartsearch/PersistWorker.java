package com.yunos.alicontacts.dialpad.smartsearch;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yunos.alicontacts.ContactsApplication;
import com.yunos.alicontacts.dialpad.smartsearch.ContactInMemoryProto.ContactInMemoryList;
import com.yunos.alicontacts.dialpad.smartsearch.ContactInMemoryProto.ContactMemoryItem;
import com.yunos.alicontacts.dialpad.smartsearch.ContactInMemoryProto.RetArray;
import com.yunos.alicontacts.dialpad.smartsearch.SearchResult.SearchResultBuilder;
import com.yunos.alicontacts.util.FeatureOptionAssistant;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class PersistWorker {
    public static final String PERSIST_FILE_NAME = "cim.dat";
    private static final String TAG = "PersistWorker";

    private static final String KEY_PROTOBUF_VERSION = "key_protobuf_version";

    /**
     * This is the threshold to decide whether to cheat the VM.
     * For large amount of contact data, we want to cheat the VM to call GC efficiently.
     * In default generating tables progress,
     * the GC releases memory at teens of KB in the beginning,
     * and grow to a bit more than 1MB on the end for 10000 contacts
     * (about 20000 cursor count for one name and one number case).
     * To let VM GC release more than 1MB memory at the beginning,
     * we cheat VM with allocate a large amount of memory and then release it after a while.
     * The cheat trick makes VM call GC less times.
     * I have some tests on variable contacts scale.
     * It seems if the contact number is 3500 (7000 cursor count for one name and one number case),
     * the cheat trick will reduce about 20% of the time cost for generating tables.
     * On 2014-07-18: We have extra ~1000 yellow page contacts need to be put in the search table.
     * So when the cursor count is 5000, then we need to play the trick. */
    private static final int CURSOR_COUNT_THRESHOLD_TO_CHEAT_VM_GC = 5000;
    /** This is the amount of memory we used to cheat the VM. It seems 1M memory cheat work fine. */
    private static final int CHEAT_VM_GC_MEMORY_SIZE = 1024 * 1024;

    //ProtoBuf data version 2: Add ContactName field
    //ProtoBuf data version 3: Exclude phone_des part from yellow page searching.
    private static final int PROTO_BUF_VERSION = 3;

    private Context mContext;
    private ContentResolver mResolver;
    private SharedPreferences mPrefs;

    private PinyinSearch mPinyinSearch;

    /**
     * A flag to indicate if the contacts has too long name strings.
     * If there are too many contacts have very long name,
     * then we will use a lot of memory to cache name data for smart search.
     * In large data case, the app will be killed for low memory.
     * To avoid the app to be killed,
     * we will disable smart search for large data case.
     */
    private boolean mTooLargeNameData = false;
    /**
     * The memory used by name data is O(n*n) by the individual length.
     * So we assume the threshold for long name is 12 characters,
     * (13 is proven to be too large, and 12 is proven OK)
     * and assume the threshold for max contacts is 10000,
     * then we got the large data threshold is 12 * 12 * 10000.
     * For low memory device, we use a small threshold.
     */
    private static final int LARGE_NAME_DATA_THRESHOLD
            = ContactsApplication.IS_LOW_MEMORY_DEVICE ?
                    10 * 10 * 10000 : 12 * 12 * 10000;

    public PersistWorker(Context context) {
        mContext = context;
        mResolver = context.getContentResolver();
        mPinyinSearch = PinyinSearch.getInstance(context);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        upgradedProtobufFile(context);
    }

    public static final String[] phoneProjection = new String[] {
        Phone.NUMBER, Data.DISPLAY_NAME, Data.RAW_CONTACT_ID, Data.DATA_VERSION, Data.MIMETYPE, Data.CONTACT_ID
    };
    public static final int PHONE_PROJECTION_NUMBER = 0;

    public static final int PHONE_PROJECTION_NAME = 1;

    public static final int PHONE_PROJECTION_RAWCONTACTID = 2;

    public static final int PHONE_PROJECTION_VERSION = 3;

    public static final int PHONE_PROJECTION_MIMETYPE = 4;

    public static final int PHONE_PROJECTION_CONTACT_ID = 5;

    private static final String sQueryPhoneWhere
            = "(" + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
                    + " OR " + Data.MIMETYPE + "='"  + StructuredName.CONTENT_ITEM_TYPE
                    + "') AND " + Data.IN_VISIBLE_GROUP + "=1";

    private Cursor queryAllPhone() {
        return mResolver.query(Data.CONTENT_URI, phoneProjection, sQueryPhoneWhere, null, Data.RAW_CONTACT_ID);
    }

    public final void doChangesList(Bundle bundle, NameConvertWorker convertWorker) {
        Cursor contacts = null;
        try {
            deserialize(convertWorker);
            convertWorker.checkPause();
            long beforeQueryPhone = System.nanoTime();
            // The time that generating the search table might be long,
            // so we need to close the cursor as early as possible.
            // The cursor contacts will be closed once it is fully read (in generateSearchTable).
            contacts = queryAllPhone();

            long durationQuery = (System.nanoTime()-beforeQueryPhone)/1000;
            Log.d(TAG, "doChangesList: Phone Query time ="+durationQuery);

            if (contacts == null || contacts.getCount() <= 0) {
                if (contacts != null) {
                    contacts.close();
                }
                generateSearchTable(null, convertWorker);
                return;
            }

            convertWorker.checkPause();
            long beforeGenerateSearchTable = System.nanoTime();
            generateSearchTable(contacts, convertWorker);
            long durationGenerateSearchTable = (System.nanoTime()-beforeGenerateSearchTable)/1000;
            Log.d(TAG,"doChangesList: Generate Search Table time="+durationGenerateSearchTable);

        }catch(Exception e){
            Log.e(TAG, "doChangesList Error:" + e.getLocalizedMessage(), e);
        }
    }

    public static final String[] RAWID_VERSION = new String[]{
        RawContacts.VERSION
    };

    public static final String[] RAW_CONTACTID = new String[] {
        RawContacts.CONTACT_ID
    };

    public long getVersion(long rawContactId) {
        long version = -1;
        Cursor c = null;

        try {
            c = mResolver.query(RawContacts.CONTENT_URI,
                    RAWID_VERSION, RAWID_CLAUSE, new String[] {
                    String.valueOf(rawContactId)
            }, null);

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                version = c.getLong(0);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return version;
    }


    public static final String RAWID_CLAUSE = RawContacts._ID + "=?";

    private void cleanProtobufData(Context context) {
        Log.d(TAG, "cleanProtobufData: begin.");
        ContactInMemoryList.Builder cimListNew_empty = ContactInMemoryList.newBuilder();
        FileOutputStream output = null;
        try {
            output = context.openFileOutput(PersistWorker.PERSIST_FILE_NAME,
                    Context.MODE_PRIVATE);
            cimListNew_empty.build().writeTo(output);
        } catch (IOException e2) {
            Log.e(TAG, "cleanPhonebufData: "+e2.getMessage(), e2);
        } finally {
            closeCloseable(output);
        }
    }

    private String regularPhoneNumberForKey(String phoneNumber) {
        char[] carray = phoneNumber.toCharArray();
        int len = carray.length;
        int read = 0, write = 0;
        char c;
        while (read < len) {
            c = carray[read];
            if (       ((c >= '0') && (c <= '9'))
                    || (c == '+')
                    || (c == '*')
                    || (c == '#')
                    || (c == ';')
                    || (c == ',')) {
                if (read != write) {
                    carray[write] = c;
                }
                write++;
            }
            read++;
        }
        return new String(carray, 0, write);
    }

    private void readContactsCursor(Cursor cursor, ArrayList<ContactFromCursor> contacts) {
        try {
            long begin = System.currentTimeMillis();
            int nameDataAmount = 0;
            // All the variables with current- prefix, mean that the variables keep the value only in current round of the loop.
            // All the variables with last- prefix, mean that the variables keep the value cross rounds of the loop.
            long lastRawContactId = -1, lastNameVersion = -1;
            long currentRawContactId;
            long currentContactId;
            String lastName = null;
            int lastNameLen = 0;
            String currentPhoneNumber, currentMimeType;
            cursor.moveToPosition(-1);
            ContactFromCursor processContact = null;
            boolean isNameRecord;
            while (cursor.moveToNext()) {
                // raw contact id and mime type are required by each round in the loop.
                // other columns are only needed by name record or number record,
                // so read them when they are required.
                currentRawContactId = cursor.getLong(PHONE_PROJECTION_RAWCONTACTID);
                currentContactId = cursor.getLong(PHONE_PROJECTION_CONTACT_ID);
                currentMimeType = cursor.getString(PHONE_PROJECTION_MIMETYPE);
                isNameRecord = StructuredName.CONTENT_ITEM_TYPE.equals(currentMimeType);

                // For a name record, we will read the name and name version.
                // For a phone number record, we will read the phone number
                // and put the number to a ContactFromCursor object.
                // The name record and phone number record are connected with raw contact id.
                if (isNameRecord) {
                    lastNameVersion = cursor.getLong(PHONE_PROJECTION_VERSION);
                    lastName = cursor.getString(PHONE_PROJECTION_NAME);
                    if (lastName == null) {
                        lastName = "";
                    }
                    lastNameLen = lastName.length();
                } else {
                    currentPhoneNumber = regularPhoneNumberForKey(cursor.getString(PHONE_PROJECTION_NUMBER));
                    if (TextUtils.isEmpty(currentPhoneNumber)) {
                        // When import from 3G sim card,
                        // an empty number will be saved if the contact has only one number.
                        // Because we are searching in dialpad, so skip the empty number record.
                        continue;
                    }
                    processContact = new ContactFromCursor();
                    processContact.rawContactId = currentRawContactId;
                    processContact.contactId = currentContactId;
                    processContact.number = currentPhoneNumber;
                    contacts.add(processContact);
                }

                if (lastRawContactId != currentRawContactId) {
                    // the raw contact id changes,
                    // we start a new contact info in this block
                    lastRawContactId = currentRawContactId;
                    if (!isNameRecord) {
                        lastNameVersion = -1;
                    }
                } else {
                    // raw contact id keeps the same,
                    // we process another record of the same raw contact.
                    if (isNameRecord) {
                        // we get the name record in this round,
                        // look back to set name and name version for early records.
                        int lookBackOffset = 0, fromIndex = contacts.size() - 1;
                        ContactFromCursor lookBackContact;
                        while (lookBackOffset <= fromIndex) {
                            lookBackContact = contacts.get(fromIndex - lookBackOffset);
                            if (lookBackContact.rawContactId != currentRawContactId) {
                                break;
                            }
                            lookBackContact.nameVersion = lastNameVersion;
                            lookBackContact.name = lastName;
                            nameDataAmount += lastNameLen * lastNameLen;
                            lookBackOffset++;
                        }
                    } else if (lastNameVersion != -1) {
                        // we got the name record in early round,
                        // use the name and name version from early round.
                        processContact.nameVersion = lastNameVersion;
                        processContact.name = lastName;
                        nameDataAmount += lastNameLen * lastNameLen;
                    }
                }
            }
            if (nameDataAmount > LARGE_NAME_DATA_THRESHOLD) {
                mTooLargeNameData = true;
            }
            long end = System.currentTimeMillis();
            Log.d(TAG, "readContactsCursor: read cursor to contacts time cost: "+(end-begin)+" ms.");
        } finally {
            // The cursor is not necessary during the remainder steps of generating the search table.
            // So close it as early as possible.
            cursor.close();
        }
    }

    // Only use the StringBuilder in generate table methods,
    // so that we can make sure there is no multi-thread issue.
    private StringBuilder mSbForTempStrInGenTable = new StringBuilder(64);
    private void generateSearchTable(Cursor contacts, NameConvertWorker convertWorker) {
        long start = System.nanoTime();
        phoneNumberlist.clear();
        PinyinSearch.initHanziPinyinForAllChars(mContext);
        if (contacts != null) {
            if (!generateSearchTableForContacts(contacts, convertWorker)) {
                mPinyinSearch.getSearchTableShadow().clear();
                mPinyinSearch.getSearchMapShadow().clear();
                return;
            }
        }

        // for uniform query of number.
        /*if (!FeatureOptionAssistant.isInternationalSupportted()) {
            if (!generateSearchTableForYellowPage(convertWorker)) {
                mPinyinSearch.getSearchTableShadow().clear();
                mPinyinSearch.getSearchMapShadow().clear();
                return;
            }
        }*/

        // Put search table and search map from shadow to real map
        mPinyinSearch.swapSearchTable();
        mPinyinSearch.getSearchMapShadow().transferDataTo(mPinyinSearch.getSearchMap());
        mPinyinSearch.notifySearchTableChanged();
        mPinyinSearch.getSearchTableShadow().clear();
        mPinyinSearch.getSearchMapShadow().clear();

        long duration = (System.nanoTime() - start) / 1000;
        Log.d(TAG, "generateSearchTable: Time for generating search table from cursor=" + duration);

        convertWorker.checkPause();
        // Write back
        serialize(convertWorker);
    }

    /**
     * Put the phone contacts (name and phone) into smart search table (temporary table).
     * @param contacts
     * @param convertWorker
     * @return if the contacts are fully put into the smart search table (temporary table).
     */
    private boolean generateSearchTableForContacts(Cursor contacts, NameConvertWorker convertWorker) {

        int count = contacts.getCount();
        /* The contactsList contains only phone number records (no name record),
         * but the cursor has both name and phone number records.
         * Assume we have 50% contacts have one number, and another 50% have two numbers,
         * then for 10 contacts, we will get count 25 and 15 of them are put in the list.
         * If we have more accurate data, then the factor can be adjusted to proper value. */
        ArrayList<ContactFromCursor> contactsList = new ArrayList<ContactFromCursor>(count * 3 / 5);

        // The tempByteArray is a trick to Lemur VM.
        // For large amount of contacts, on the beginning of generating tables,
        // the GC amount is about teens of KB.
        // With the generating table progress, the GC amount grows to more than 1MB.
        // To improve the GC efficiency, we cheat the Lemur VM that we have large amount of memory to working on.
        // So that the GC amount will keep over 1MB from the beginning of generating tables.
        // Thus we can get less GC times.
        byte[] tempByteArray = null;
        try {
            if (count > CURSOR_COUNT_THRESHOLD_TO_CHEAT_VM_GC) {
                tempByteArray = new byte[CHEAT_VM_GC_MEMORY_SIZE];
                // NOTE: Do NOT delete the following log.
                //       The purpose of the output is to let the compiler
                //       NOT optimize the tempbyteArray by using it in somewhere.
                Log.d(TAG, "generateSearchTable: we have large amount ("+count+") of contacts data. Alloc "+tempByteArray.length+" bytes to cheat VM for better GC performance.");
            }
        } catch (Throwable th) {
            // ignore;
            Log.e(TAG, "generateSearchTableForContacts() throw Throwable", th);
        }

        readContactsCursor(contacts, contactsList);

        // When we go through all contacs in readContactsCursor(),
        // we will set mTooLargeNameData if necessary.
        // So check this after readContactsCursor().
        if (!FeatureOptionAssistant.isInternationalSupportted()) {
            if (mTooLargeNameData) {
                // Actually, we have the ability to provide the yellow page
                // search in dialpad, but to avoid confusing the user,
                // we return false to provide nothing in dialpad search.
                return false;
            }
        }

        tempByteArray = null;
        for (ContactFromCursor c : contactsList) {
            if (PinyinSearch.mPersistInterrupted) {
                return false;
            }
            convertWorker.checkPause();

            String key = makeKeyForContactFromCursor(c);

            // The empty phone number records are filtered out in readContactsCursor.
            // So we only filter empty name here, and all numbers are added to
            // phone number list.
            if (!TextUtils.isEmpty(c.name)) {
                ContactInMemory memory = mPinyinSearch.getSearchTable().get(key);
                if (memory == null || c.nameVersion != memory.version) {
                    mPinyinSearch.initSearchTable(c.name, c.name, c.rawContactId, c.number, c.nameVersion, key,
                            mContext);
                } else {
                    mPinyinSearch.getSearchTableShadow().put(key, memory);
                    mPinyinSearch.addToSearchMap(mPinyinSearch.getSearchMapShadow(),
                            memory.retArray, key);
                }
            }

            // Add into phone number search list
            addToPhoneNumberList(key, c.number);
        }
        return true;
    }

    private String makeKeyForContactFromCursor(ContactFromCursor c) {
        mSbForTempStrInGenTable.setLength(0);
        mSbForTempStrInGenTable.append(c.rawContactId).append(PinyinSearch.KEY_SPLIT).append(c.number);
        return mSbForTempStrInGenTable.toString();
    }

    /**
     * Put the yellow page (name and phone) into smart search table (temporary table).
     * @param convertWorker
     * @return if the yellow page are fully put into the smart search table (temporary table).
     */
    private boolean generateSearchTableForYellowPage(NameConvertWorker convertWorker) {
        // If YellowPageSearcher is ready, add YellowPage Contacts into search
        // table
//        if (yps.isReady.get()) {// use uniform query
//            CopyOnWriteArrayList<YellowPageContact> ypList = YellowPageSearcher.getYpList();
//            for (YellowPageContact ypc : ypList) {
//                if (PinyinSearch.mPersistInterrupted) {
//                    return false;
//                }
//                convertWorker.checkPause();
//
//                mSbForTempStrInGenTable.setLength(0);
//                mSbForTempStrInGenTable.append('-').append(ypc.businessID)
//                .append(PinyinSearch.KEY_SPLIT).append(ypc.phoneNumber);
//                String key = mSbForTempStrInGenTable.toString();
//
//                if (!TextUtils.isEmpty(ypc.company)) {
//                    ContactInMemory memory = mPinyinSearch.getSearchTable().get(key);
//                    if (memory == null) {
//                        String searchName = ypc.company;
//                        String displayName = ypc.company;
//                        if (!TextUtils.isEmpty(ypc.desc)) {
//                            mSbForTempStrInGenTable.setLength(0);
//                            mSbForTempStrInGenTable.append(ypc.company).append('(').append(ypc.desc).append(')');
//                            displayName = mSbForTempStrInGenTable.toString();
//                        }
//                        mPinyinSearch.initSearchTable(searchName, displayName,
//                                ypc.businessID, ypc.phoneNumber, 0, key,
//                                mContext);
//                    } else {
//                        mPinyinSearch.getSearchTableShadow().put(key, memory);
//                        mPinyinSearch.addToSearchMap(
//                                mPinyinSearch.getSearchMapShadow(),
//                                memory.retArray, key);
//                    }
//                }
//            }
//        }
        return true;
    }

    /**
     * This function is used for put key into phone number search list
     * @param key
     */
    private void addToPhoneNumberList(String key, String phoneNumber) {
        ContactKey ck = new ContactKey();
        ck.key = key;
        ck.phoneNumber = phoneNumber;
        phoneNumberlist.add(ck);
    }

    /**
     * The deserialize() only needs to be called on cold start.
     * Once the search tables in memory are filled, then the data in memory is up to date.
     * And does not need to be reloaded from disk on contacts change.
     */
    private static boolean deserialized = false;
    private void deserialize(NameConvertWorker convertWorker){
        if (deserialized) {
            mPinyinSearch.getSearchTableShadow().clear();
            mPinyinSearch.getSearchMapShadow().clear();
            return;
        }
        long start = System.nanoTime();
        ContactInMemoryList.Builder cimList = null;
        FileInputStream input = null;
        try {
            cimList = ContactInMemoryList.newBuilder();
            input = mContext.openFileInput(PERSIST_FILE_NAME);
            cimList.mergeFrom(input);
        } catch(InvalidProtocolBufferException ipbe){
            mContext.deleteFile(PERSIST_FILE_NAME);
        }
        catch (Exception e) {
            Log.e(TAG, "deserialize: "+PERSIST_FILE_NAME + " has not found", e);
        } finally {
            closeCloseable(input);
        }

        long duration = (System.nanoTime() - start) / 1000;
        Log.d(TAG, "deserialize: Time for read from ProtoBuf=" + duration+"us");
        start = System.nanoTime();

        mPinyinSearch.getSearchMapShadow().clear();
        ConcurrentHashMap<String, ContactInMemory> searchTableShadow = mPinyinSearch
                .getSearchTableShadow();

        convertWorker.checkPause();
        // Read serialized data from File
        for (ContactMemoryItem cmi : cimList.getContactMemoryItemList()) {
            ContactInMemory memory = mPinyinSearch.newContactInMemory();
            memory.pinyinData = cmi.getPinyinData();
            memory.allFistCharacter = cmi.getAllFistCharacter();
            memory.length = cmi.getLength();
            memory.version = cmi.getVersion();
            memory.contactName = cmi.getContactName();
            memory.phoneNumber = cmi.getPhoneNumber();
            memory.area = cmi.getArea();
            memory.retArray = new ArrayList<String>();
            for(RetArray ai:cmi.getRetArrayList()){
                memory.retArray.add(ai.getPinyin());
            }

            String key = cmi.getKey();
            searchTableShadow.put(key, memory);
            mPinyinSearch.addToSearchMap(mPinyinSearch.getSearchMapShadow(),
                    memory.retArray, key);
            convertWorker.checkPause();
        }

        duration = (System.nanoTime() - start) / 1000;
        Log.d(TAG, "Time for putting serialized data into search table=" + duration);
        long lMoveTableTime = System.nanoTime();
        mPinyinSearch.swapSearchTable();
        mPinyinSearch.getSearchMapShadow().transferDataTo(mPinyinSearch.getSearchMap());
        if (!mPinyinSearch.isReady()) {
            mPinyinSearch.setReady(true);
        }
        Log.d(TAG, "Deserialize move table cost:"+(System.nanoTime()-lMoveTableTime)/1000+"us");

        convertWorker.checkPause();

        //Clear shadow table
        mPinyinSearch.getSearchTableShadow().clear();
        mPinyinSearch.getSearchMapShadow().clear();
        deserialized = true;
    }

    public static String getPhoneNumberFromKey(String key){
        int splitPos = key.indexOf(PinyinSearch.KEY_SPLIT);
        if ((splitPos < 0) || ((splitPos+1) == key.length())) {
            Log.e(TAG, "getPhoneNumberFromKey: Something is wrong!!! Got invalid key, no split or no number: \""+key+"\".");
            return "";
        }
        return key.substring(splitPos+1);
    }

    public static String getRawContactidFromKey(String key) {
        int splitPos = key.indexOf(PinyinSearch.KEY_SPLIT);
        if (splitPos <= 0) {
            Log.e(TAG, "getRawContactidFromKey: Something is wrong!!! Got invalid key, no split or no raw contact id: \""+key+"\".");
            return "";
        }
        return key.substring(0, splitPos);
    }

    private void serialize(NameConvertWorker convertWorker){
        long start = System.nanoTime();

        //Serialize search table into file

        FileOutputStream output = null;
        try {
            ContactInMemoryList.Builder cimListNew = ContactInMemoryList
                    .newBuilder();
            Set<HashMap.Entry<String, ContactInMemory>> set = mPinyinSearch
                    .getSearchTable().entrySet();
            Iterator<Entry<String, ContactInMemory>> it = set.iterator();
            convertWorker.checkPause();
            while (it.hasNext()) {
                Entry<String, ContactInMemory> entry = it.next();
                ContactInMemory cimTemp = entry.getValue();
                ArrayList<String> pinyinList = cimTemp.retArray;
                ContactMemoryItem.Builder cmiBuilder = ContactMemoryItem
                        .newBuilder();
                for (String py : pinyinList) {
                    RetArray.Builder raBuilder = RetArray.newBuilder();
                    raBuilder.setPinyin(py);
                    cmiBuilder.addRetArray(raBuilder.build());
                }
                cmiBuilder.setKey(entry.getKey());
                cmiBuilder.setPinyinData(cimTemp.pinyinData);
                cmiBuilder.setAllFistCharacter(cimTemp.allFistCharacter);
                cmiBuilder.setLength(cimTemp.length);
                cmiBuilder.setVersion(cimTemp.version);
                cmiBuilder.setContactName(cimTemp.contactName);
                cmiBuilder.setPhoneNumber(cimTemp.phoneNumber);
                cmiBuilder.setArea(cimTemp.area);

                cimListNew.addContactMemoryItem(cmiBuilder.build());
                convertWorker.checkPause();
            }
            output = mContext.openFileOutput(
                    PersistWorker.PERSIST_FILE_NAME, Context.MODE_PRIVATE);
            cimListNew.build().writeTo(output);
            long duration = (System.nanoTime() - start) / 1000;
            Log.d(TAG, "serialize: Time for saving protobuf to disk=" + duration);
        } catch (Exception e) {
            Log.e(TAG, "serialize: " + e.getMessage(), e);
        } finally {
            closeCloseable(output);
        }
    }

    public static synchronized void phoneMatch(String searchCase, Context context, SearchResultBuilder builder, HashSet<String> matchedNumbers) {
        Log.d(TAG,"phoneMatch: search string is: "+searchCase);

        //Return empty list if search text is empty or phone number cache is not ready.
        if (TextUtils.isEmpty(searchCase) || phoneNumberlist == null) {
            Log.e(TAG, "phoneMatch: Search Number or phoneNumberList is null");
            return;
        }

        int searchLen = searchCase.length();
        boolean isSearchResultsEnough = false;
        for (ContactKey contactKey : phoneNumberlist) {
            int matchPos = contactKey.phoneNumber.indexOf(searchCase);
            if ((matchPos < 0) || builder.isKeyExists(contactKey.key)) {
                continue;
            }

            MatchResult mr = new MatchResult();
            mr.type = MatchResult.TYPE_CONTACTS;
            mr.matchPart = MatchResult.MATCH_PART_PHONE_NUMBER;
            mr.key = contactKey.key;
            mr.phoneNumber = contactKey.phoneNumber;
            mr.setNumberMatchRange(matchPos, searchLen);
            mr.calculateWeight();
            builder.addMatchResult(mr);
            if (!isSearchResultsEnough) {
                matchedNumbers.add(mr.phoneNumber);
                isSearchResultsEnough = ContactsSearchEngine.isSearchResultsEnough(searchLen, builder.getMatchResultCount());
            }
        }
    }

    private static void closeCloseable(Closeable c) {
        if (c != null) {
            try { c.close(); } catch (Exception e) {
                Log.e(TAG, "closeCloseable() throw Exception", e);
            }
        }
    }

    private int getSavedProtobufVersion() {
        int version = mPrefs.getInt(KEY_PROTOBUF_VERSION, 0);
        return version;
    }

    private void saveProtobufVersion(int version) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(KEY_PROTOBUF_VERSION, version);
        editor.commit();
    }

    //Check the current protobuf version, if version is updated, then clean all buffer data.
    private void upgradedProtobufFile(Context context) {
        int savedVersion = this.getSavedProtobufVersion();
        Log.d(TAG, "LFJ upgradedProtobufFiles: savedVersion = " + savedVersion + " PROTO_BUF_VERSION = "
                + PROTO_BUF_VERSION);
        if (PROTO_BUF_VERSION != savedVersion) {
            cleanProtobufData(context);
            saveProtobufVersion(PROTO_BUF_VERSION);
        }
    }

    static CopyOnWriteArrayList<ContactKey> phoneNumberlist = new CopyOnWriteArrayList<ContactKey>();
}
