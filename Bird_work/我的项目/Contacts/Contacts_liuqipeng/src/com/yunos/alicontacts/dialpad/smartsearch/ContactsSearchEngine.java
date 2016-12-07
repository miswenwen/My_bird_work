
package com.yunos.alicontacts.dialpad.smartsearch;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;

import com.yunos.alicontacts.database.AliContactsDatabaseHelper.CallerViewColumns;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.dialpad.smartsearch.SearchResult.SearchResultBuilder;
import com.yunos.alicontacts.quickcall.QuickCallSetting;
import com.yunos.alicontacts.util.AliTextUtils;

import dalvik.system.VMRuntime;

import java.lang.ref.WeakReference;
import java.util.HashSet;

public class ContactsSearchEngine {
    private static final String TAG = "ContactsSearchEngine";

    private static final int SEARCH_TOKEN = 1;

    private static final int UPDATE_TOKEN = 2;

    private SearchHandler mSearchHandler;

    private final Object mLock = new Object();

    private volatile OnQueryCompleteListener mListener;

    private SparseIntArray mVisibleContacts;

    private Activity mActivity;

    private PinyinSearch mPinyinSearch;

    public static long lStartTime = 0;

    //If search result is more than SEARCH_THRESHOLD, then stop search next.
    public static final int SEARCH_THRESHOLD = 20;
    // If search string length is not long enough,
    // then we will stop search next when search result is more than SEARCH_THRESHOLD.
    public static final int SEARCH_STRING_LENGTH_THRESHOLD = 4;

    public ContactsSearchEngine(Activity activity,
            OnQueryCompleteListener listener,
            PinyinSearchStateChangeListener pyStateListener) {
        mListener = listener;
        mActivity = activity;
        mVisibleContacts = new SparseIntArray();

        mPinyinSearch = PinyinSearch.getInstance(mActivity);
        mPinyinSearch.setOnStateChangeListener(pyStateListener);
    }

    /**
     * Pause smart search engine
     */
    public void pause() {
        // do nothing
    }

    /**
     * Resume smart search engine
     */
    public void resume() {
    }

    /**
     * Release resource
     */
    public void destroy() {

        mVisibleContacts.clear();
        mPinyinSearch.setOnStateChangeListener(null);

        synchronized (mLock) {
            if (mSearchHandler != null) {
                mSearchHandler.removeMessages(UPDATE_TOKEN);
                mSearchHandler.removeMessages(SEARCH_TOKEN);
                mSearchHandler.getLooper().quit();
                mSearchHandler = null;
            }
        }
        mListener = null;
    }

    public void searchImpl(String searchText) {
        lStartTime = System.nanoTime();

        VMRuntime.getRuntime().setTargetHeapUtilization(0.2f);
        SearchResultBuilder builder = doSearch(searchText);
        final SearchResult result = builder.buildResult();

        if (mActivity == null) {
            return;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mListener == null) {
                    return;
                }
                mListener.onQueryComplete(result);
            }
        });
    }

    public static boolean isSearchResultsEnough(int searchLength, int resultsCount) {
        /*
         * If the search result has already exceed SEARCH_THRESHOLD,
         * and the search string length less than SEARCH_STRING_LENGTH_THRESHOLD
         * then don't search phone number and call log.
         * Too much search result is meaningless in this case.
         */
        return    (resultsCount > SEARCH_THRESHOLD)
                && (searchLength < SEARCH_STRING_LENGTH_THRESHOLD);
    }

    private SearchResultBuilder doSearch(String searchText) {
        SearchResultBuilder builder = new SearchResultBuilder(searchText);

        final String searchText2 = searchText.replaceAll("[^0-9+,;*#]", "");
        if(searchText.length()<1 || !mPinyinSearch.isReady())
            return builder;

        // 0. search speed call number
        char keyChar = searchText.charAt(0);
        if (searchText.length() == 1 && keyChar >= '1' && keyChar <= '9') {
            builder.addMatchResult(searchQuickCall(keyChar));
        }

        // the matchedNumbers stores the phone numbers that are already found.
        // it is used to avoid duplicated items to be displayed in the result.
        HashSet<String> matchedNumbers = new HashSet<String>(1024);
        // 1. search phone contact name and yellow page contact name.
        PinyinSearch.initHanziPinyinForAllChars(mActivity);
        try {
            mPinyinSearch.searchByT9(searchText, builder, matchedNumbers);
        } catch (Exception e) {
            Log.e(TAG, "doSearch: got exception: "+e.getLocalizedMessage(), e);
            return builder;
        }

        int matchedCount = builder.getMatchResultCount();
        if (isSearchResultsEnough(searchText.length(), matchedCount)
                || PinyinSearch.mHaveNewInput) {
            return builder;
        }

        // 2. search phone contact number.
        phoneMatch(searchText2, mActivity, builder, matchedNumbers);

        // for uniform query of number.
        // 3. search yellow page contact number.
        /*if (!FeatureOptionAssistant.isInternationalSupportted() && yps.isReady.get()) {
            yps.phoneMatchAll(searchText2, builder, matchedNumbers);

        }*/
        matchedCount = builder.getMatchResultCount();
        if (isSearchResultsEnough(searchText2.length(), matchedCount)
                || PinyinSearch.mHaveNewInput) {
            return builder;
        }


        // 4. search call log number.
        callLogMatch(searchText2, builder, matchedNumbers);

        return builder;
    }

    private MatchResult searchQuickCall(char keyChar){
        MatchResult mr = new MatchResult();
        mr.type = MatchResult.TYPE_QUICK_CALL;

        QuickCallSetting quickSetting = QuickCallSetting.getQuickCallInstance(mActivity);
        Pair<String,String> nameAndNumber = quickSetting.getNameAndPhoneNumber(mActivity, keyChar);
        mr.key = String.valueOf(keyChar);
        mr.name = nameAndNumber.first;
        mr.phoneNumber = nameAndNumber.second;
        return mr;
    }

    public static String[] phoneProj = new String[] {
        Phone.RAW_CONTACT_ID, Phone.NUMBER
    };

    private static final String[] CALLS_PROJECTION = new String[] {
        CallerViewColumns.CALL_ID,
        CallerViewColumns.COLUMN_NUMBER,
        CallerViewColumns.COLUMN_LOC_PROVINCE,
        CallerViewColumns.COLUMN_LOC_AREA,
        CallerViewColumns.COLUMN_SHOP_NAME
    };

    private static final int CALLS_COLUMN_INDEX_CALL_ID = 0;
    private static final int CALLS_COLUMN_INDEX_NUMBER = 1;
    private static final int CALLS_COLUMN_INDEX_PROVINCE = 2;
    private static final int CALLS_COLUMN_INDEX_AREA = 3;
    private static final int CALLS_COLUMN_INDEX_SHOP_NAME = 4;

    public void phoneMatch(String searchCase, Context context, SearchResultBuilder builder, HashSet<String> matchedNumbers) {
        PersistWorker.phoneMatch(searchCase, context, builder, matchedNumbers);
    }

    public void callLogMatch(String searchCase, SearchResultBuilder builder, HashSet<String> matchedNumbers) {
        if (TextUtils.isEmpty(searchCase)) {
            return;
        }
        int searchLen = searchCase.length();
        StringBuilder sb = new StringBuilder();
        sb.append(CallerViewColumns.COLUMN_NUMBER).append(" LIKE '%");
        // searchCase is processed, and contains no special chars.
        // so no need to escape.
        sb.append(searchCase);
        sb.append("%'");

        Cursor callLogCursor = CallLogManager.getInstance(ActivityThread.currentApplication())
                .queryAliCalls(CALLS_PROJECTION, sb.toString(), null, null);

        if (callLogCursor != null) {
            try {
                while (callLogCursor != null && callLogCursor.moveToNext()) {
                    String phoneNumber = callLogCursor.getString(CALLS_COLUMN_INDEX_NUMBER);
                    if (matchedNumbers.contains(phoneNumber)) {
                        continue;
                    }
                    matchedNumbers.add(phoneNumber);

                    String key = PinyinSearch.KEY_SPLIT + phoneNumber;
                    MatchResult mr = new MatchResult();
                    mr.type = MatchResult.TYPE_CALLOG;
                    mr.matchPart = MatchResult.MATCH_PART_PHONE_NUMBER;
                    mr.key = key;
                    mr.phoneNumber = phoneNumber;
                    mr.databaseID = callLogCursor.getLong(CALLS_COLUMN_INDEX_CALL_ID);
                    mr.setNumberMatchRange(phoneNumber.indexOf(searchCase), searchLen);
                    mr.name = callLogCursor.getString(CALLS_COLUMN_INDEX_SHOP_NAME);
                    mr.calculateWeight();
                    String province = callLogCursor.getString(CALLS_COLUMN_INDEX_PROVINCE);
                    String area = callLogCursor.getString(CALLS_COLUMN_INDEX_AREA);
                    mr.mLocation = AliTextUtils.makeLocation(province, area);
                    builder.addMatchResult(mr);
                }
            } finally {
                callLogCursor.close();
            }
        }
    }

    /**
     * Send search message
     *
     * @param key the String that is the key of searching
     */
    public void sendSearchMessage(String key) {
        synchronized (mLock) {
            if (!initSearchHandler()) {
                return;
            }
            Message msg = mSearchHandler.obtainMessage(SEARCH_TOKEN);
            msg.obj = key;

            PinyinSearch.setHaveNewInput(true);
            mSearchHandler.removeCallbacksAndMessages(null);
            mSearchHandler.sendMessage(msg);
        }
    }

    /**
     * Send update search scope message
     */
    private boolean initSearchHandler() {
        if (mSearchHandler == null) {
            HandlerThread thread = new HandlerThread(TAG,
                    android.os.Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();

            final Looper looper = thread.getLooper();
            if (looper == null) {
                return false;
            }
            mSearchHandler = new SearchHandler(looper, this);
        }

        return true;
    }

    /** The handler class that receive and handle message */
    private static class SearchHandler extends Handler {
        private WeakReference<ContactsSearchEngine> mWeakRef;

        public SearchHandler(final Looper looper, ContactsSearchEngine engine) {
            super(looper);
            mWeakRef = new WeakReference<ContactsSearchEngine>(engine);
        }

        @Override
        public void handleMessage(Message msg) {
            ContactsSearchEngine engine = mWeakRef.get();
            if (engine == null) {
                return;
            }

            int what = msg.what;
            switch (what) {
                case SEARCH_TOKEN:
                    String key = (String) msg.obj;
                    PinyinSearch.setHaveNewInput(false);
                    engine.searchImpl(key);
                    break;
                case UPDATE_TOKEN:
                    break;
                default:
                    Log.w(TAG, "SearchHandler.handleMessage: unrecognized what "+what);
                    break;
            }
        }
    }

}
