
package com.yunos.alicontacts.database;

import android.annotation.NonNull;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.database.tables.CallerNumberTable;
import com.yunos.alicontacts.database.tables.CallsTable;
import com.yunos.alicontacts.database.util.NumberNormalizeUtil;
import com.yunos.alicontacts.database.util.NumberServiceHelper;
import com.yunos.alicontacts.database.util.SqliteUtil;
import com.yunos.alicontacts.dialpad.calllog.CallLogQuery;
import com.yunos.alicontacts.dialpad.calllog.CallerViewQuery;
import com.yunos.alicontacts.dialpad.calllog.ContactInfo;
import com.yunos.alicontacts.dialpad.calllog.ContactInfoHelper;
import com.yunos.alicontacts.dialpad.calllog.PhoneNumberHelper;
import com.yunos.alicontacts.dialpad.calllog.SyncCallLogHandler;
import com.yunos.alicontacts.platform.PDUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.AliTextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public final class CallLogManager {
    private static final String TAG = "CallLogManager";
    private AliContactsDatabaseHelper mDbHelper = null;
    private static CallLogManager sCallLogManager = null;
    public static final int BULK_INSERTS_PER_YIELD_POINT = 50;
    public static final int BATCH_QUERY_LIMIT_BY_NUMBER_SERVICE = 100;
    private static final int MAX_SINGLE_NUMBER_LENGTH_FOR_NUMBER_SERVICE = 20;
    private static final String DELETE_WHERE_ID = CallsTable.COLUMN_ID + "=?";

    // insert new caller number item SQLiteStatement
    private static final String INSERT_CALLER_NUMBER_TABLE = "insert or replace into "
            + CallerNumberTable.TABLE_CALLER_NUMBER + " ("
            + CallerNumberTable.COLUMN_FORMATTED_NUMBER + "," + CallerNumberTable.COLUMN_SHOP_NAME
            + "," + CallerNumberTable.COLUMN_LOC_PROVINCE + "," + CallerNumberTable.COLUMN_LOC_AREA
            + "," + CallerNumberTable.COLUMN_TAG_NAME + "," + CallerNumberTable.COLUMN_MARKED_COUNT
            + "," + CallerNumberTable.COLUMN_SHOP_ID + ","
            + CallerNumberTable.COLUMN_SHOP_LOGO_NAME + ") values(?,?,?,?,?,?,?,?)";

    private static final String[] LOCAL_CALLER_NUMBER_PROJECTION = new String[] {
        CallerNumberTable.COLUMN_FORMATTED_NUMBER,
        CallerNumberTable.COLUMN_SHOP_NAME,
        CallerNumberTable.COLUMN_SHOP_ID,
        CallerNumberTable.COLUMN_SHOP_LOGO_NAME,
        CallerNumberTable.COLUMN_TAG_NAME,
        CallerNumberTable.COLUMN_MARKED_COUNT,
        CallerNumberTable.COLUMN_LOC_PROVINCE,
        CallerNumberTable.COLUMN_LOC_AREA };
    private static final int LOCAL_CALLER_NUMBER_FORMATTED_NUMBER = 0;
    private static final int LOCAL_CALLER_NUMBER_YP_NAME = 1;
    private static final int LOCAL_CALLER_NUMBER_SHOP_ID = 2;
    private static final int LOCAL_CALLER_NUMBER_SHOP_LOGO = 3;
    private static final int LOCAL_CALLER_NUMBER_TAG_NAME = 4;
    private static final int LOCAL_CALLER_NUMBER_MARKED_COUNT = 5;
    private static final int LOCAL_CALLER_NUMBER_PROVINCE = 6;
    private static final int LOCAL_CALLER_NUMBER_AREA = 7;

    private static SyncCallLogHandler sSyncCallsHandler = null;

    /*
     * We might have two kinds of data changed notify from NumberService:
     * 1) the whole table is changed, e.g. NumberService has a batch update from server;
     * 2) a single number is changed, e.g. NumberService queries a new number from server.
     * For the first case, we will refresh the full caller_number table with RefreshCallerNumberTableTask.
     * But if the data change notify is sent several times, we shall make sure the last one is handled,
     * and previous times are handled as less as possible.
     * For the second case, we will update the caller_number table item with UpdateCallerNumberTask.
     * If the data change notify is sent one by one, we shall put all the single number in a queue,
     * and handle all of them one after another.
     * Don't process the numbers in multiple threads, it might take too many threads.
     * And the single number update is faster than full table refresh.
     * If the single number update comes later and finishes earlier,
     * then the full table refresh might overwrite the data from single number update.
     * So it is important to keep write operations to caller_number table in sequence.
     */
    private class RefreshCallerNumberTableTask implements Runnable {
        private boolean mWorking = true;
        private final HashSet<String> mNumbers = new HashSet<String>();
        public RefreshCallerNumberTableTask(String number) {
            mNumbers.add(number);
        }
        @Override
        public void run() {
            HashSet<String> numbers;
            while (true) {
                synchronized (mNumbers) {
                    if (mNumbers.isEmpty()) {
                        Log.i(TAG, "RefreshCallerNumberTableTask.run: refresh finished.");
                        mWorking = false;
                        mRefreshCallerNumberTableTask = null;
                        return;
                    }
                    if (mNumbers.contains(null)) {
                        numbers = null;
                    } else {
                        numbers = new HashSet<String>(mNumbers);
                    }
                    mNumbers.clear();
                }
                try {
                    boolean updated;
                    // NOTE: Do NOT put the follow code in the above lock block.
                    if (numbers == null) {
                        Log.i(TAG, "RefreshCallerNumberTableTask.run: one more table refresh needed.");
                        updated = refreshCallerNumberTable();
                    } else {
                        Log.i(TAG, "RefreshCallerNumberTableTask.run: one more number update needed.");
                        updated = updateCallerNumberTable(numbers, null);
                    }
                    Log.i(TAG, "RefreshCallerNumberTableTask.run: updated="+updated);
                    if (updated) {
                        notifyCallsTableChange(CallLogChangeListener.CHANGE_PART_NUMBER_INFO);
                    }
                } catch (SQLiteException e) {
                    // In case of db(disk) full, do not block make phone call.
                    Log.e(TAG, "RefreshCallerNumberTableTask.run: got exception.", e);
                }
            }
        }

        public boolean addNumber(String number) {
            synchronized (mNumbers) {
                if (!mWorking) {
                    return false;
                }
                if (TextUtils.isEmpty(number)) {
                    // We need a full table refresh, any single number update is useless.
                    mNumbers.clear();
                    mNumbers.add(null);
                } else if (!mNumbers.contains(null)) {
                    // If we have a full table refresh in queue, then we don't need one more single number update.
                    mNumbers.add(number);
                }
                return true;
            }
        }
    }
    private RefreshCallerNumberTableTask mRefreshCallerNumberTableTask = null;

    /**
     * Here is ali call log changes listener, the @mCallLogChangeListeners will
     * hold them, when ali data base changes all UI need refresh regist this
     * listener will be called.
     * @author xinxin.lxx
     */
    public interface CallLogChangeListener {
        /** The call log data is changed. */
        public static final int CHANGE_PART_CALL_LOG = 0x00000001;
        /** The contact info of the caller number is changed. */
        public static final int CHANGE_PART_CONTACT_INFO = 0x00000002;
        /** The number service info of the caller number is changed. */
        public static final int CHANGE_PART_NUMBER_INFO = 0x00000004;

        /**
         * NOTE: This method might be called on non-UI thread.
         * @param changedPart What part of data is changed.
         * Maybe a combination of CHANGE_PART_XXX contacts.
         */
        public void onCallLogChange(int changedPart);
    }

    private ArrayList<CallLogChangeListener> mCallLogChangeListeners = new ArrayList<CallLogChangeListener>(
            5);

    public void registCallsTableChangeListener(CallLogChangeListener l) {
        if (l == null) {
            return;
        }
        synchronized (mCallLogChangeListeners) {
            mCallLogChangeListeners.add(l);
            Log.d(TAG, "registCallsTableChangeListener: listeners " + mCallLogChangeListeners.size());
        }
    }

    public void unRegistCallsTableChangeListener(CallLogChangeListener l) {
        synchronized (mCallLogChangeListeners) {
            mCallLogChangeListeners.remove(l);
            Log.d(TAG, "unRegistCallsTableChangeListener: listeners " + mCallLogChangeListeners.size());
        }
    }

    public void notifyCallsTableChange(int changedPart) {
        CallLogChangeListener[] listeners;
        synchronized (mCallLogChangeListeners) {
            listeners = mCallLogChangeListeners.toArray(new CallLogChangeListener[mCallLogChangeListeners.size()]);
        }
        Log.d(TAG, "notifyCallsTableChange: listeners " + listeners.length);
        for (CallLogChangeListener l : listeners) {
            l.onCallLogChange(changedPart);
        }
    }

    private Context mContext = null;

    private class ContactsProviderObserver extends ContentObserver {
        private static final String CALL_LOG_URI_HOST = "call_log";
        private static final String CONTACTS_URI_HOST = "com.android.contacts";

        public ContactsProviderObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "ContactsProviderObserver.onChange: uri="+uri);
            if (uri == null) {
                return;
            }
            String host = uri.getHost();
            if (CALL_LOG_URI_HOST.equals(host)) {
                requestSyncCalllogsInternal();
            } else if (CONTACTS_URI_HOST.equals(host)) {
                requestUpdateContactInfo();
            }
        }
    }

    private class NumberServiceDatabaseObserver extends ContentObserver {
        public NumberServiceDatabaseObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "NumberServiceDatabaseObserver changed, selfChage="+selfChange+"; uri="+uri);
            String uriStr = uri == null ? null : uri.toString();
            if (uriStr == null) {
                refreshCallerNumberTableAsync(null);
                return;
            }
            String number = null;
            if (uriStr.startsWith(NumberServiceHelper.SINGLE_NUMINFO_QUERY_URI_STR+"/")) {
                number = uri.getLastPathSegment();
                if (!TextUtils.isEmpty(number)) {
                    number = NumberNormalizeUtil.normalizeNumber(number, true);
                }
            }
            if (TextUtils.isEmpty(number)) {
                // something unknown is changed.
                // if the number is "", we still want a null to put in the queue.
                refreshCallerNumberTableAsync(null);
            } else {
                // a single number is changed.
                refreshCallerNumberTableAsync(number);
            }
        }
    }

    private CallLogManager(Context context) {
        mContext = context;
        mDbHelper = AliContactsDatabaseHelper.getInstance(context);

        ContentResolver resolver = mContext.getContentResolver();
        ContactsProviderObserver observer = new ContactsProviderObserver();
        resolver.registerContentObserver(Calls.CONTENT_URI, true, observer);
        resolver.registerContentObserver(ContactsContract.Data.CONTENT_URI, true, observer);
        resolver.registerContentObserver(NumberServiceHelper.SINGLE_NUMINFO_QUERY_URI, true,
                new NumberServiceDatabaseObserver());
    }

    public static synchronized CallLogManager getInstance(Context context) {
        if (sCallLogManager == null) {
            sCallLogManager = new CallLogManager(context);
            sSyncCallsHandler = SyncCallLogHandler.createHandler(context);
        }

        return sCallLogManager;
    }

    private static boolean sInitedCallLogs = false;
    public void requestSyncCalllogsByInit() {
        if (!sInitedCallLogs) {
            Log.i(TAG, "requestSyncCalllogsByInit:");
            requestSyncCalllogsInternal();
            sInitedCallLogs = true;
        }
    }

    private void requestSyncCalllogsInternal() {
        sSyncCallsHandler.removeMessages(SyncCallLogHandler.WHAT_SYNC_CALL_LOG);
        sSyncCallsHandler.sendEmptyMessage(SyncCallLogHandler.WHAT_SYNC_CALL_LOG);
    }

    public void insertCall(Cursor cursor) {
        if (cursor != null) {
            ContentValues cv = new ContentValues(21);

            getCallLogInfo(cursor, cv);
            fillPhotoUriForLocalCalls(cv, null);
            insertCall(cv);
        }
    }

    public void insertCall(ContentValues cv) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.insertWithOnConflict(CallsTable.TABLE_CALLS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int updateLocalCalls(ContentValues cv, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        return db.update(CallsTable.TABLE_CALLS, cv, selection, selectionArgs);
    }

    private void refreshCallerNumberTableAsync(String number) {
        // Put mRefreshCallerNumberTableTask in a temp reference,
        // to avoid AsyncTask sets it to null after check null here.
        // If AsyncTask sets mRefreshCallerNumberTableTask to null,
        // then we must get false on addNumber(), and we will fall to new task below.
        RefreshCallerNumberTableTask tmpTask = mRefreshCallerNumberTableTask;
        if ((tmpTask != null) && tmpTask.addNumber(number)) {
            Log.i(TAG, "refreshCallerNumberTableAsync: a refresh is working.");
            return;
        }
        Log.i(TAG, "refreshCallerNumberTableAsync: start refresh worker.");
        mRefreshCallerNumberTableTask = new RefreshCallerNumberTableTask(number);
        AsyncTask.THREAD_POOL_EXECUTOR.execute(mRefreshCallerNumberTableTask);
    }

    private boolean refreshCallerNumberTable() {
        Log.d(TAG, "refreshCallerNumberTable.");
        Cursor cursor = null;
        try {
            cursor = queryAliCallerNumber(
                    new String[] { CallerNumberTable.COLUMN_FORMATTED_NUMBER }, null, null, null);
            int count = cursor == null ? -1 : cursor.getCount();
            Log.d(TAG, "refreshCallerNumberTable count :" + count);
            if (count <= 0) {
                return false;
            }

            HashSet<String> numbers = new HashSet<String>(count);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                numbers.add(cursor.getString(0));
            }

            return updateCallerNumberTable(numbers, null);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Query the numbers in numbersToAdd from NumberService and insert the result into caller_number.
     * Delete the obsoleted numbers in numbersToAdd from caller_number.
     * @param numbersToAdd A set of numbers that need to be inserted to caller_number. Cannot be null.
     * @return If the used data in caller_number table is updated.
     */
    public boolean updateCallerNumber(@NonNull HashSet<String> numbersToAdd) {
        Log.i(TAG, "updateCallerNumber: ");
        boolean result = false;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Cursor cursor = null;
        HashSet<String> numbersToDelete = new HashSet<String>();
        try {
            cursor = db.query(CallerNumberTable.TABLE_CALLER_NUMBER, new String[] {
                CallerNumberTable.COLUMN_FORMATTED_NUMBER
            }, null, null, null, null, null);
            if (cursor != null) {
                ArrayList<String> listCallerNumber = new ArrayList<String>(cursor.getCount());
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    String formattedNum = cursor.getString(0);
                    listCallerNumber.add(formattedNum);
                }
                calculateDeleteList(numbersToAdd, listCallerNumber, numbersToDelete);
                result = updateCallerNumberTable(numbersToAdd, numbersToDelete);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "updateCallerNumber: got error.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    public void removeTagInCallerNumber(String number) {
        Log.i(TAG, "removeTagInCallerNumber: ");

        ContentValues values = new ContentValues(2);
        values.put(CallerNumberTable.COLUMN_TAG_NAME, (String) null);
        values.put(CallerNumberTable.COLUMN_MARKED_COUNT, 0);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = db.update(CallerNumberTable.TABLE_CALLER_NUMBER,
                values,
                CallerNumberTable.COLUMN_FORMATTED_NUMBER + "=?",
                new String[] { NumberNormalizeUtil.normalizeNumber(number, true) });
        Log.i(TAG, "removeTagInCallerNumber: update count="+count);
    }

    /**
     * <p>NOTE: This method will create a db transaction.</p>
     * Query numbers in numbersToAdd from NumberService, and insert the result into caller_number.
     * Delete numbersToDelete and obsoleted numbers in numbersToAdd from caller_number.
     * @param numbersToAdd A set of numbers that need to be inserted to caller_number. Cannot be null.
     * @param numbersToDelete A set of numbers that need to be deleted from caller_number. Can be null.
     * @return If the used data in caller_number table is updated.
     */
    private boolean updateCallerNumberTable(
            @NonNull HashSet<String> numbersToAdd,
            HashSet<String> numbersToDelete) {
        Log.d(TAG, "updateCallerNumberTable called.");
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentResolver resolver = mContext.getContentResolver();

        boolean updated = false;
        ArrayList<NumberInfo> results = new ArrayList<NumberInfo>(numbersToAdd.size());
        if (numbersToDelete == null) {
            numbersToDelete = new HashSet<String>();
        }

        try {
            queryNumberInfoFromNumberService(resolver, numbersToAdd, results, numbersToDelete);
        } catch (SQLiteException sqle) {
            Log.e(TAG, "updateCallerNumberTable: Failed to query number info from NumberService.", sqle);
        }

        if ((!results.isEmpty()) || (!numbersToDelete.isEmpty())) {
            SQLiteStatement insertStatement = db.compileStatement(INSERT_CALLER_NUMBER_TABLE);
            db.beginTransaction();
            try {
                if ((!numbersToDelete.isEmpty())
                        && (deleteNumbersInCallerNumberTable(db, numbersToDelete) > 0)) {
                    updated = true;
                }
                if (!results.isEmpty()) {
                    addNumExInTransaction(results, insertStatement, resolver);
                    updated = true;
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        return updated;
    }

    private void queryNumberInfoFromNumberService(
            ContentResolver resolver,
            HashSet<String> totalNumbers,
            ArrayList<NumberInfo> results,
            HashSet<String> obsoletedNumbers) {
        if (totalNumbers.isEmpty()) {
            Log.d(TAG, "queryNumberInfoFromNumberService: empty numbers, quit.");
            return;
        }
        HashMap<String, NumberInfo> localNumbers = readLocalNumberInfo(totalNumbers);
        ArrayList<HashSet<String>> numbersList = splitNumbers(totalNumbers);
        for (HashSet<String> numbers : numbersList) {
            Uri batchUri = NumberServiceHelper.getBatchNumInfoQueryInfoForNumbers(numbers.toString());

            Cursor cursor = null;
            try {
                Log.d(TAG, "queryNumberInfoFromNumberService() numberservice query up. numbers count=" + numbers.size());
                cursor = resolver.query(batchUri, null, null, null, null);
                Log.d(TAG, "queryNumberInfoFromNumberService() numberservice query down. cursor = " + cursor);
                if (cursor != null) {
                    NumberInfo numInfo;
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        numInfo = NumberInfo.buildFromNumberServiceCursor(cursor);
                        if (!numInfo.equals(localNumbers.get(numInfo.formattedNumber))) {
                            results.add(numInfo);
                        }
                        numbers.remove(numInfo.formattedNumber);
                    }
                    obsoletedNumbers.addAll(numbers);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private HashMap<String, NumberInfo> readLocalNumberInfo(@NonNull HashSet<String> totalNumbers) {
        HashMap<String, NumberInfo> localResults = new HashMap<String, NumberInfo>(totalNumbers.size());
        String[] numbersArray = totalNumbers.toArray(new String[totalNumbers.size()]);
        StringBuilder selection = new StringBuilder(totalNumbers.size() << 4);
        SqliteUtil.appendInClause(selection, CallerNumberTable.COLUMN_FORMATTED_NUMBER, numbersArray);
        Cursor c = null;
        try {
            c = queryAliCallerNumber(LOCAL_CALLER_NUMBER_PROJECTION, selection.toString(), null, null);
            if ((c == null) || (c.getCount() == 0)) {
                return localResults;
            }
            while (c.moveToNext()) {
                NumberInfo info = NumberInfo.buildFromLocalNumberCursor(c);
                localResults.put(info.formattedNumber, info);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return localResults;
    }

    private int deleteNumbersInCallerNumberTable(SQLiteDatabase db, HashSet<String> numbersToDelete) {
        int result = 0;
        if (numbersToDelete != null && !numbersToDelete.isEmpty()) {
            Log.d(TAG, "deleteNumbersInCallerNumberTable: deleteNums = " + numbersToDelete.size());
            StringBuilder where = new StringBuilder();
            SqliteUtil.appendInClause(where,
                    CallerNumberTable.COLUMN_FORMATTED_NUMBER,
                    numbersToDelete.toArray(new String[numbersToDelete.size()]));
            result = db.delete(CallerNumberTable.TABLE_CALLER_NUMBER,
                    where.toString(),
                    null);
        }
        Log.d(TAG, "deleteNumbersInCallerNumberTable: delete count "+result);
        return result;
    }

    private void calculateDeleteList(HashSet<String> numbersToAdd, ArrayList<String> callerNumbers,
            HashSet<String> numbersToDelete) {
        for (String num : callerNumbers) {
            if (!numbersToAdd.contains(num)) {
                numbersToDelete.add(num);
            }
        }
    }

    private void addNumExInTransaction(ArrayList<NumberInfo> totalNumbers, SQLiteStatement statement,
            ContentResolver resolver) {
        for (NumberInfo number : totalNumbers) {
            bindCallerNumberInfo(statement, number);
            statement.executeInsert();
        }
    }

    /**
     * The NumberService has a limit in batch query.
     * The numbers in the batch uri is up to BATCH_QUERY_LIMIT_BY_NUMBER_SERVICE.
     * If we have more numbers, we have to split the set into several subsets,
     * that each contains no more than BATCH_QUERY_LIMIT_BY_NUMBER_SERVICE numbers.
     * @param numbers The full set.
     * @return A list of subsets.
     */
    private ArrayList<HashSet<String>> splitNumbers(HashSet<String> numbers) {
        int count = numbers.size();
        int listSize = (count + BATCH_QUERY_LIMIT_BY_NUMBER_SERVICE - 1) / BATCH_QUERY_LIMIT_BY_NUMBER_SERVICE;
        ArrayList<HashSet<String>> result = new ArrayList<HashSet<String>>(listSize);
        HashSet<String> subset = null;
        int subcount = 0;
        for (String number : numbers) {
            // some carriers do not provide free calling number display service,
            // so we might get empty number here.
            if (TextUtils.isEmpty(number)) {
                continue;
            }
            if (subcount == 0) {
                subset = new HashSet<String>(BATCH_QUERY_LIMIT_BY_NUMBER_SERVICE);
                result.add(subset);
            }
            if (number.length() > MAX_SINGLE_NUMBER_LENGTH_FOR_NUMBER_SERVICE) {
                number = number.substring(0, MAX_SINGLE_NUMBER_LENGTH_FOR_NUMBER_SERVICE);
            }
            subset.add(number);
            subcount++;
            if (subcount == BATCH_QUERY_LIMIT_BY_NUMBER_SERVICE) {
                subcount = 0;
            }
        }
        return result;
    }

    /**
     * Bind fields of insert statement to caller_number table.
     * @param statement The insert statement.
     * @param number The data read from NumberService.
     */
    private void bindCallerNumberInfo(SQLiteStatement statement, NumberInfo number) {
        Log.d(TAG, "bindCallerNumberInfo: number=" + number);

        statement.bindString(1, number.formattedNumber);
        statement.bindString(2, number.ypName);
        statement.bindString(3, number.province);
        statement.bindString(4, number.area);
        statement.bindString(5, number.tag);
        statement.bindLong(6, number.markedCount);
        statement.bindLong(7, number.shopId);
        statement.bindString(8, number.logoUri);
    }

    private static String getStringFromCursorNotNull(Cursor cursor, int index) {
        String result = cursor.getString(index);
        return result == null ? "" : result;
    }

    private String getCallLogInfo(Cursor cursor, ContentValues cv) {
        final String currentNumber = cursor.getString(CallLogQuery.NUMBER);
        final long callId = cursor.getLong(CallLogQuery.ID);
        final long callDate = cursor.getLong(CallLogQuery.DATE);
        final long callDuration = cursor.getLong(CallLogQuery.DURATION);
        final int callType = cursor.getInt(CallLogQuery.CALL_TYPE);
        final int features = cursor.getInt(CallLogQuery.FEATURES);
        final int callIsRead = cursor.getInt(CallLogQuery.IS_READ);

        final String callCountryIso = cursor.getString(CallLogQuery.COUNTRY_ISO);
        final String callVoiceMailUri = cursor.getString(CallLogQuery.VOICEMAIL_URI);
        final String callGeocodedLocation = cursor.getString(CallLogQuery.GEOCODED_LOCATION);
        final String callName = cursor.getString(CallLogQuery.CACHED_NAME);
        final int callNumberType = cursor.getInt(CallLogQuery.CACHED_NUMBER_TYPE);
        final String callNumberLabel = cursor.getString(CallLogQuery.CACHED_NUMBER_LABEL);
        final String callLookupURI = cursor.getString(CallLogQuery.CACHED_LOOKUP_URI);
        final String callMatchedNum = cursor.getString(CallLogQuery.CACHED_MATCHED_NUMBER);
        final String callNormalNum = cursor.getString(CallLogQuery.CACHED_NORMALIZED_NUMBER);
        final long callPhotoId = cursor.getLong(CallLogQuery.CACHED_PHOTO_ID);
        final String callPhotoUri = PDUtils.getPhotoUriFromCallLogQuery(cursor);
        final int ringTime = cursor.getInt(CallLogQuery.RING_TIME);
        final String phoneRecordPath = cursor.getString(CallLogQuery.PHONE_RECORD_PATH);
        final String callFormatNum = NumberNormalizeUtil.normalizeNumber(currentNumber, true);
        final int isNew = cursor.getInt(CallLogQuery.NEW);

        cv.put(CallsTable.COLUMN_ID, callId);
        cv.put(CallsTable.COLUMN_NUMBER, currentNumber);
        cv.put(CallsTable.COLUMN_DATE, callDate);
        cv.put(CallsTable.COLUMN_DURATION, callDuration);
        cv.put(CallsTable.COLUMN_TYPE, callType);
        cv.put(CallsTable.COLUMN_FEATURES, features);
        cv.put(CallsTable.COLUMN_IS_READ, callIsRead);

        cv.put(CallsTable.COLUMN_COUNTRY_ISO, callCountryIso);
        cv.put(CallsTable.COLUMN_VOICEMAIL_URI, callVoiceMailUri);
        cv.put(CallsTable.COLUMN_LOCATION, callGeocodedLocation);
        cv.put(CallsTable.COLUMN_NAME, callName);
        cv.put(CallsTable.COLUMN_NUMBER_TYPE, callNumberType);
        cv.put(CallsTable.COLUMN_NUMBER_LABEL, callNumberLabel);
        cv.put(CallsTable.COLUMN_LOOKUP_URI, callLookupURI);
        cv.put(CallsTable.COLUMN_MATCHED_NUM, callMatchedNum);
        cv.put(CallsTable.COLUMN_NORMALIZED_NUM, callNormalNum);
        cv.put(CallsTable.COLUMN_PHOTO_ID, callPhotoId);
        cv.put(CallsTable.COLUMN_PHOTO_URI, callPhotoUri);
        cv.put(CallsTable.COLUMN_FORMATTED_NUM, callFormatNum);
        cv.put(CallsTable.COLUMN_RING_TIME, ringTime);
        cv.put(CallsTable.COLUMN_PHONE_RECORD_PATH, phoneRecordPath);
        cv.put(CallsTable.COLUMN_NEW, isNew);

        if (SimUtil.MULTISIM_ENABLE) {
            final int subId = cursor.getInt(CallLogQuery.SUB_ID);
            cv.put(CallsTable.COLUMN_SIMID, subId);
        }

        return callFormatNum;
    }

    /**
     * When insert new call to local calls table, we need to query photo_uri for
     * platform with api level 22 or lower.
     * Because the remote calls table does not provide photo_uri in old version.
     * @param cv
     * @param cachedPhotoUri This is used to reduce query in batch insert mode.
     */
    private void fillPhotoUriForLocalCalls(ContentValues cv, HashMap<String, String> cachedPhotoUri) {
        if (Build.VERSION.SDK_INT >= 23) {
            return;
        }
        String number = cv.getAsString(CallsTable.COLUMN_NUMBER);
        if (!PhoneNumberHelper.canPlaceCallsTo(number)) {
            return;
        }
        String countryIso = cv.getAsString(CallsTable.COLUMN_COUNTRY_ISO);
        String key = number + "|" + countryIso;
        if ((cachedPhotoUri != null) && cachedPhotoUri.containsKey(key)) {
            cv.put(CallsTable.COLUMN_PHOTO_URI, cachedPhotoUri.get(key));
            return;
        }
        ContactInfo info = ContactInfoHelper.getInstance(mContext).lookupNumber(number, countryIso);
        String photoUri = info == null ? null : info.photoUri;
        cv.put(CallsTable.COLUMN_PHOTO_URI, photoUri);
        if (cachedPhotoUri != null) {
            cachedPhotoUri.put(key, photoUri);
        }
    }

    public void insertCalls(Cursor cursor) {
        Log.d(TAG, "Enter CallLogManager.insertCalls");
        if (cursor != null) {
            int count = cursor.getCount();

            Log.d(TAG, "Calls from CP is:" + count);

            if (count == 0) {
                return;
            }
            HashSet<String> numbers = bulkInsertCalls(cursor);
            boolean updated = updateCallerNumberTable(numbers, null);
            if (updated) {
                notifyCallsTableChange(
                        CallLogChangeListener.CHANGE_PART_CALL_LOG
                        | CallLogChangeListener.CHANGE_PART_NUMBER_INFO);
            } else {
                notifyCallsTableChange(CallLogChangeListener.CHANGE_PART_CALL_LOG);
            }
        }
    }

    /**
     * NOTE: This method will create a db transaction.
     * @param cursor The calls from ContactsProvider.
     * @return The formatted_number of inserted calls.
     */
    private HashSet<String> bulkInsertCalls(Cursor cursor) {
        ContentValues cv = new ContentValues(21);
        HashSet<String> numbers = new HashSet<String>();
        HashMap<String, String> cachedPhotoUri = new HashMap<String, String>();
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        int opCount = 0;
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                cv.clear();
                String formattedNumber = getCallLogInfo(cursor, cv);
                if (!numbers.contains(formattedNumber)) {
                    numbers.add(formattedNumber);
                }
                fillPhotoUriForLocalCalls(cv, cachedPhotoUri);
                db.insertWithOnConflict(CallsTable.TABLE_CALLS, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);

                if (++opCount >= BULK_INSERTS_PER_YIELD_POINT) {
                    opCount = 0;
                    try {
                        db.yieldIfContendedSafely();
                    } catch (RuntimeException re) {
                        Log.e(TAG, "[bulkInsertCalls] yield transaction failed!", re);
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return numbers;
    }

    public int removeCall(long callID) {
        SQLiteDatabase db = null;
        int result = -1;
        db = mDbHelper.getWritableDatabase();
        result = db.delete(CallsTable.TABLE_CALLS, DELETE_WHERE_ID, new String[] {
                    String.valueOf(callID)
                });
        notifyCallsTableChange(CallLogChangeListener.CHANGE_PART_CALL_LOG);
        return result;
    }

    public void clearCalls() {
        SQLiteDatabase db = null;
        db = mDbHelper.getWritableDatabase();
        db.delete(CallsTable.TABLE_CALLS, null, null);
        db.delete(CallerNumberTable.TABLE_CALLER_NUMBER, null, null);
        notifyCallsTableChange(CallLogChangeListener.CHANGE_PART_CALL_LOG | CallLogChangeListener.CHANGE_PART_NUMBER_INFO);
    }

    /**
     * Get call log from AlicontactProvider calls table.
     * @param table
     * @param columns
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param orderBy
     * @return
     */
    public Cursor queryAliCalls(String[] columns, String selection, String[] selectionArgs,
            String orderBy) {
        if (!AliContactsDatabaseHelper.sIsDatabaseReady.get()) {
            Log.e(TAG, "[query] error, database is not ready.");
            return null;
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(CallerViewQuery.TABLE_VIEW_CALLER);

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, columns, selection, selectionArgs, null, null, orderBy);
        return c;
    }

    private Cursor queryAliCallerNumber(String[] columns, String selection, String[] selectionArgs,
            String orderBy) {
        if (!AliContactsDatabaseHelper.sIsDatabaseReady.get()) {
            Log.e(TAG, "[query] error, database is not ready.");
            return null;
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(CallerNumberTable.TABLE_CALLER_NUMBER);

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, columns, selection, selectionArgs, null, null, orderBy);
        return c;
    }

    public void updateCallLog(ContentValues cv, String where, String[] args) {
        try {
            mContext.getContentResolver().update(Calls.CONTENT_URI, cv, where, args);
        } catch (SQLiteException e) {
            Log.e(TAG, "updateCallLog error.", e);
        }
    }

    public void requestUpdateContactInfo() {
        Log.i(TAG, "requestUpdateContactInfo:");
        ContactInfoHelper.getInstance(mContext).expireAllContactInfoCache();
        notifyCallsTableChange(CallLogChangeListener.CHANGE_PART_CONTACT_INFO);
    }

    private static class NumberInfo {
        public final String formattedNumber;
        public final String ypName;
        public final String province;
        public final String area;
        public final String tag;
        public final int markedCount;
        public final long shopId;
        public final String logoUri;

        public NumberInfo(
                String formattedNumber,
                String ypName,
                String province,
                String area,
                String tag,
                int markedCount,
                long shopId,
                String logoUri) {
            this.formattedNumber = formattedNumber;
            this.ypName = ypName;
            this.province = province;
            this.area = area;
            this.tag = tag;
            this.markedCount = markedCount;
            this.shopId = shopId;
            this.logoUri = logoUri;
        }

        public static NumberInfo buildFromNumberServiceCursor(Cursor cursor) {

            String formattedNumber = NumberNormalizeUtil.normalizeNumber(cursor.getString(NumberServiceHelper.BATCH_NUMINFO_COLUMN_NUMBER), true);
            String ypName = getStringFromCursorNotNull(cursor, NumberServiceHelper.BATCH_NUMINFO_COLUMN_YP_NAME);
            String province = getStringFromCursorNotNull(cursor, NumberServiceHelper.BATCH_NUMINFO_COLUMN_PROVINCE);
            String area = getStringFromCursorNotNull(cursor, NumberServiceHelper.BATCH_NUMINFO_COLUMN_AREA);
            String serverTag = getStringFromCursorNotNull(cursor, NumberServiceHelper.BATCH_NUMINFO_COLUMN_SERVER_TAG_NAME);
            String userTag = getStringFromCursorNotNull(cursor, NumberServiceHelper.BATCH_NUMINFO_COLUMN_USER_TAG_NAME);
            int markedCount = cursor.getInt(NumberServiceHelper.BATCH_NUMINFO_COLUMN_MARK_TAG_COUNT);
            if (markedCount < 0) {
                markedCount = 0;
            }
            long shopId = cursor.getLong(NumberServiceHelper.BATCH_NUMINFO_COLUMN_YP_SHOP_ID);
            String logoUri = getStringFromCursorNotNull(cursor, NumberServiceHelper.BATCH_NUMINFO_COLUMN_YP_LOGO_URI);
            boolean noUserTag = TextUtils.isEmpty(userTag);

            return new NumberInfo(
                    formattedNumber,
                    ypName,
                    province,
                    area,
                    noUserTag ? serverTag : userTag, // userTag has higher priority
                    noUserTag ? markedCount : -1, // userTag does not need markedCount
                    shopId,
                    logoUri);
        }

        public static NumberInfo buildFromLocalNumberCursor(Cursor cursor) {
            String formattedNumber = getStringFromCursorNotNull(cursor, LOCAL_CALLER_NUMBER_FORMATTED_NUMBER);
            String ypName = getStringFromCursorNotNull(cursor, LOCAL_CALLER_NUMBER_YP_NAME);
            String province = getStringFromCursorNotNull(cursor, LOCAL_CALLER_NUMBER_PROVINCE);
            String area = getStringFromCursorNotNull(cursor, LOCAL_CALLER_NUMBER_AREA);
            String tag = getStringFromCursorNotNull(cursor, LOCAL_CALLER_NUMBER_TAG_NAME);
            int markedCount = cursor.getInt(LOCAL_CALLER_NUMBER_MARKED_COUNT);
            long shopId = cursor.getLong(LOCAL_CALLER_NUMBER_SHOP_ID);
            String logoUri = getStringFromCursorNotNull(cursor, LOCAL_CALLER_NUMBER_SHOP_LOGO);

            return new NumberInfo(
                    formattedNumber,
                    ypName,
                    province,
                    area,
                    tag,
                    markedCount,
                    shopId,
                    logoUri);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("NumberInfo:{number=\'").append(AliTextUtils.desensitizeNumber(formattedNumber))
              .append("\'; name=\'").append(ypName)
              .append("\'; province=\'").append(province)
              .append("\'; area=\'").append(area)
              .append("\'; tag=\'").append(tag)
              .append("\'; marked=").append(markedCount)
              .append("; shopId=").append(shopId)
              .append("; logoUri=\'").append(logoUri)
              .append("\'}");
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NumberInfo)) {
                return false;
            }
            NumberInfo info = (NumberInfo) obj;
            return AliTextUtils.equalsLoosely(formattedNumber, info.formattedNumber)
                    && (markedCount == info.markedCount)
                    && AliTextUtils.equalsLoosely(tag, info.tag)
                    && AliTextUtils.equalsLoosely(ypName, info.ypName)
                    && AliTextUtils.equalsLoosely(logoUri, info.logoUri)
                    && (shopId == info.shopId)
                    // the location info is most likely stable during all the time.
                    && AliTextUtils.equalsLoosely(province, info.province)
                    && AliTextUtils.equalsLoosely(area, info.area);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((formattedNumber == null) ? 0 : formattedNumber.hashCode());
            result = prime * result + ((ypName == null) ? 0 : ypName.hashCode());
            result = prime * result + ((tag == null) ? 0 : tag.hashCode());
            result = prime * result + markedCount;
            result = prime * result + (int) (shopId & 0xFFFFFFFF);
            result = prime * result + (int) ((shopId >> 32) & 0xFFFFFFFF);
            result = prime * result + ((logoUri == null) ? 0 : logoUri.hashCode());
            result = prime * result + ((province == null) ? 0 : province.hashCode());
            result = prime * result + ((area == null) ? 0 : area.hashCode());
            return result;
        }

    }

}
