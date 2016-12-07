/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunos.alicontacts.list;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.yunos.alicontacts.ContactListEmptyView;
import com.yunos.alicontacts.ContactPhotoManager;
import com.yunos.alicontacts.ContactsDataCache;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.PeopleActivity2;
import com.yunos.alicontacts.aliutil.android.common.widget.CompositeCursorAdapter.Partition;
import com.yunos.alicontacts.dialpad.smartsearch.NameConvertWorker;
import com.yunos.alicontacts.list.MultiSelectPeopleFragment.ContactBatchDeleteStatusListener;
import com.yunos.alicontacts.list.fisheye.FishEyeContactsCache;
import com.yunos.alicontacts.list.fisheye.FishEyeData;
import com.yunos.alicontacts.preference.ContactsPreferences;
import com.yunos.alicontacts.preference.ContactsSettingActivity;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimStateReceiver;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.FeatureOptionAssistant;
import com.yunos.alicontacts.widget.ContextMenuAdapter;
import com.yunos.alicontacts.widget.IndexerListAdapter;
import com.yunos.common.UsageReporter;
import com.yunos.yundroid.widget.FishEyeView;
import com.yunos.yundroid.widget.FishEyeView.FishEyeOrientation;

import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.LoaderManager.LoaderCallbacks;
import yunos.support.v4.content.CursorLoader;
import yunos.support.v4.content.Loader;
import yunos.support.v4.view.ViewPager;

import java.util.HashMap;
import java.util.Set;

/**
 * Common base class for various contact-related list fragments.
 */
public abstract class ContactEntryListFragment<T extends ContactEntryListAdapter> extends Fragment
        implements OnItemClickListener, OnItemLongClickListener, OnScrollListener, OnFocusChangeListener, OnTouchListener,
        FishEyeView.OnChildClickListener, LoaderCallbacks<Cursor>, SimStateReceiver.SimStateListener {
    private static final String TAG = "ContactEntryListFragment";

    /**
     * If there are too many contacts, then we will get timeout in prepare fisheye data.
     * To avoid ANR, we will disable fisheye on large amount of contacts.
     * If the number of contacts is larger than threshold, then no fisheye is displayed.
     * For performance test, we will have a bit more than 3000 contacts.
     * I make about 1k buffer for full function in performance test.
     * Why 4093? No special reason, I just get a prime number less than 4Ki.
     */
    public static final int THRESHOLD_FOR_NO_FISHEYE = 4093;

    /**
     * To avoid contacts list cannot be refreshed when contacts deleted at lock
     * srceen state. Use shared prefrence key for listening contacts deleting
     * completed. When contacts deleted completed, the contacts list will reload
     * data.
     */
    public static final String KEY_PREFS_CONTACTS_CHANGED = "key_prefs_contacts_changed";

    // TODO: Make this protected. This should not be used from the
    // PeopleActivity but
    // instead use the new startActivityWithResultFromFragment API
    public static final int ACTIVITY_REQUEST_CODE_PICKER = 1;

    private static final String KEY_LIST_STATE = "liststate";
    private static final String KEY_SECTION_HEADER_DISPLAY_ENABLED = "sectionHeaderDisplayEnabled";
    private static final String KEY_PHOTO_LOADER_ENABLED = "photoLoaderEnabled";
    private static final String KEY_QUICK_CONTACT_ENABLED = "quickContactEnabled";
    private static final String KEY_SEARCH_MODE = "searchMode";
    private static final String KEY_VISIBLE_SCROLLBAR_ENABLED = "visibleScrollbarEnabled";
    private static final String KEY_SCROLLBAR_POSITION = "scrollbarPosition";
    private static final String KEY_QUERY_STRING = "queryString";
    private static final String KEY_DIRECTORY_SEARCH_MODE = "directorySearchMode";
    private static final String KEY_SELECTION_VISIBLE = "selectionVisible";
    //private static final String KEY_REQUEST = "request";
    private static final String KEY_DARK_THEME = "darkTheme";
    //private static final String KEY_LEGACY_COMPATIBILITY = "legacyCompatibility";
    private static final String KEY_DIRECTORY_RESULT_LIMIT = "directoryResultLimit";

    private static final String DIRECTORY_ID_ARG_KEY = "directoryId";

    public static final int DIRECTORY_LOADER_ID = -1;

    private static final int DIRECTORY_SEARCH_DELAY_MILLIS = 300;
    /* directory search message */
    private static final int DIRECTORY_SEARCH_MESSAGE = 1;
    /* refresh fisheye ui */
    private static final int FISHEYE_CREATE_TEXTHOLDER_MESSAGE = 2;
    /* Used to fetch fishdata message */
    private static final int FISHEYEDATA_MESSAGE = 3;

    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    private boolean mSectionHeaderDisplayEnabled;
    private boolean mFishEyeDisplayEnabled;
    private boolean mPhotoLoaderEnabled;
    private boolean mQuickContactEnabled = true;
    private boolean mSearchMode;
    private boolean mVisibleScrollbarEnabled;
    private int mVerticalScrollbarPosition = View.SCROLLBAR_POSITION_RIGHT;
    private String mQueryString;
    private int mDirectorySearchMode = DirectoryListLoader.SEARCH_MODE_NONE;
    private boolean mSelectionVisible;
    //private boolean mLegacyCompatibility;

    private boolean mOnCreateFlag;

    private LayoutInflater mInflater = null;
    private View mDeleteProgress = null;

    private static final int WHAT_SHOW_DELETE_CONTACTS_PROGRESS = 1;
    private static final int WHAT_HIDE_DELETE_CONTACTS_PROGRESS = 2;
    private static class ContactListHandler extends Handler {
        private final ContactEntryListFragment<?> mFragment;
        public ContactListHandler(ContactEntryListFragment<?> fragment) {
            mFragment = fragment;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case WHAT_SHOW_DELETE_CONTACTS_PROGRESS:
                mFragment.showDeleteContactsProgress(true);
                break;
            case WHAT_HIDE_DELETE_CONTACTS_PROGRESS:
                mFragment.showDeleteContactsProgress(false);
                break;
            default:
                break;
            }
        }
    };

    private ContactListHandler mHandler;

    protected boolean mShowDeleteIndicator = true;
    private ContactBatchDeleteStatusListener mBatchDeleteStatusListener
            = new ContactBatchDeleteStatusListener() {
        @Override
        public void onDeleteStatusChanged(boolean isDeleting) {
            Log.i(TAG, "onDeleteStatusChanged: isDeleting="+isDeleting);
            if (!mShowDeleteIndicator) {
                Log.i(TAG, "onDeleteStatusChanged: this fragment is set to not show delete indicator.");
                return;
            }
            // Run the core logic in main thread.
            mHandler.sendEmptyMessage(
                    isDeleting ? WHAT_SHOW_DELETE_CONTACTS_PROGRESS
                            : WHAT_HIDE_DELETE_CONTACTS_PROGRESS);
        }
    };

    private T mAdapter;
    private View mView;
    private ListView mListView;
    private FishEyeView mFishEye = null;

    /**
     * Used for keeping track of the scroll state of the list.
     */
    private Parcelable mListState;

    private int mDisplayOrder;
    private int mSortOrder;
    private int mDirectoryResultLimit = DEFAULT_DIRECTORY_RESULT_LIMIT;

    private ContextMenuAdapter mContextMenuAdapter;
    private ContactPhotoManager mPhotoManager;
    private ContactListEmptyView mEmptyView;
    private ContactsPreferences mContactsPrefs;
    private ContactsDataCache mContactsCache;
    private FishEyeData mFishData;

    private boolean mForceLoad;

    private boolean mDarkTheme;

    private static final int STATUS_NOT_LOADED = 0;
    private static final int STATUS_LOADING = 1;
    private static final int STATUS_LOADED = 2;

    private static final String[] FISH_EYE_INDEX = new String[] {
            "★", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
            "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };
    //private String[] mEnableFishEyeChar;

    protected static int mHeaderViewHeight = 0;

    private int dip2px(Context context, int resId) {
        if (context != null) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return 0;
    }

    private int mDirectoryListStatus = STATUS_NOT_LOADED;

    private static int mFavoriteContactsCount = 0;

    public static void setFavoriteContactsOffset(int offset) {
        mFavoriteContactsCount = offset;
    }

    public static int getFavoriteContactsCount() {
        return mFavoriteContactsCount;
    }

    /**
     * Indicates whether we are doing the initial complete load of data (false)
     * or a refresh caused by a change notification (true)
     */
    private boolean mLoadPriorityDirectoriesOnly;

    private Context mContext;

    private HashMap<String, Integer> mSectionMap;

    protected int mListPosition;

    private Handler mRefreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DIRECTORY_SEARCH_MESSAGE) {
                loadDirectoryPartition(msg.arg1, (DirectoryPartition) msg.obj);
            } else if (msg.what == FISHEYE_CREATE_TEXTHOLDER_MESSAGE) {
                createFishEyeTextHolder(msg.arg1);
            }
        }
    };

    private HandlerThread mFishEyeHandlerThread;
    private FishEyeHandler mFishEyeHandler;

    protected abstract View inflateView(LayoutInflater inflater, ViewGroup container);

    protected abstract T createListAdapter();

    /**
     * @param position Please note that the position is already adjusted for
     *            header views, so "0" means the first list item below header
     *            views.
     */
    protected abstract void onItemClick(int position, long id);

    public abstract void selectedAll(boolean all);

    /**
     * BUGID:102506 FishEyeData doing on work thread.
     */
    private class FishEyeHandler extends Handler {

        public FishEyeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (mContext == null) {
                Log.w(TAG, "FishEyeHandler/handlerMessage: mContext is null.");
                return;
            }

            if (msg.what != FISHEYEDATA_MESSAGE) {
                Log.w(TAG, "FishEyeHandler/handlerMessage: unhandled message: " + msg.what);
                return;
            }

            FishEyeContactsCache cache = (FishEyeContactsCache) msg.obj;
            int favoriteCount = getFavoriteContactsCount();

            Log.d(TAG, "FishEyeHandler/handlerMessage: cache="+cache
                    +"; favoriteCount="+favoriteCount);
            mFishData.setFishData(cache, favoriteCount);
            if (cache != null) {
                // The getFishEyeData() generates the internal index map for each name.
                mFishData.getFishEyeData();
            }

            // It need to create text holders in fisheye view for the first time
            if (mFishEye != null && !mFishEye.isTextHoldersAdded()) {
                Message refreshMsg = mRefreshHandler
                        .obtainMessage(FISHEYE_CREATE_TEXTHOLDER_MESSAGE);
                refreshMsg.arg1 = msg.arg1; // arg1 is contact count

                mRefreshHandler.sendMessage(refreshMsg);
            }
        }
    }

    private void createFishEyeTextHolder(int count) {
        if (mFishEye == null && count > 0) {
            initFishEye();
        }

        if (mFishEye != null) {
            mFishEye.createTextHolder();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i(TAG, "onAttach: activity="+activity);
        setContext(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // NOTE: Add log to track bug https://k3.alibaba-inc.com/issue/7857068?versionId=1169325
        // do NOT remove this method.
        Log.i(TAG, "onDetach:");
    }

    /**
     * Sets a context for the fragment in the unit test environment.
     */
    public void setContext(Context context) {
        mContext = context;
        configurePhotoLoader();
    }

    public Context getContext() {
        return mContext;
    }

    public T getAdapter() {
        return mAdapter;
    }

    // @Override
    // public View getView() {
    // return mView;
    // }

    public View getRootView() {
        return mView;
    }

    public ListView getListView() {
        return mListView;
    }

    public ContactListEmptyView getEmptyView() {
        return mEmptyView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SECTION_HEADER_DISPLAY_ENABLED, mSectionHeaderDisplayEnabled);
        outState.putBoolean(KEY_PHOTO_LOADER_ENABLED, mPhotoLoaderEnabled);
        outState.putBoolean(KEY_QUICK_CONTACT_ENABLED, mQuickContactEnabled);
        outState.putBoolean(KEY_SEARCH_MODE, mSearchMode);
        outState.putBoolean(KEY_VISIBLE_SCROLLBAR_ENABLED, mVisibleScrollbarEnabled);
        outState.putInt(KEY_SCROLLBAR_POSITION, mVerticalScrollbarPosition);
        outState.putInt(KEY_DIRECTORY_SEARCH_MODE, mDirectorySearchMode);
        outState.putBoolean(KEY_SELECTION_VISIBLE, mSelectionVisible);
        //outState.putBoolean(KEY_LEGACY_COMPATIBILITY, mLegacyCompatibility);
        outState.putString(KEY_QUERY_STRING, mQueryString);
        outState.putInt(KEY_DIRECTORY_RESULT_LIMIT, mDirectoryResultLimit);
        outState.putBoolean(KEY_DARK_THEME, mDarkTheme);

        if (mListView != null) {
            outState.putParcelable(KEY_LIST_STATE, mListView.onSaveInstanceState());
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        mOnCreateFlag = true;
        super.onCreate(savedState);
        mContactsPrefs = new ContactsPreferences(mContext);
        restoreSavedState(savedState);
        mFishEyeHandlerThread = new HandlerThread("FishEyeData-Thread");
        mFishEyeHandlerThread.start();
        mFishEyeHandler = new FishEyeHandler(mFishEyeHandlerThread.getLooper());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.registerOnSharedPreferenceChangeListener(mOnOffPreference);
        mContactsCache = new ContactsDataCache();
        mFishData = new FishEyeData(mContext);
        //mEnableFishEyeChar = FishEyeData.getFishEyeIndexChar(mContext);

        SimStateReceiver.registSimStateListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFishEyeHandlerThread != null) {
            mFishEyeHandlerThread.quit();
        }
        if (mContext != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.unregisterOnSharedPreferenceChangeListener(mOnOffPreference);
        }
        mContactsCache.clean();

        SimStateReceiver.unregistSimStateListener(this);
        MultiSelectPeopleFragment.removeContactBatchDeleteStatusListener(mBatchDeleteStatusListener);
    }

    public void restoreSavedState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        mSectionHeaderDisplayEnabled = savedState.getBoolean(KEY_SECTION_HEADER_DISPLAY_ENABLED);
        mPhotoLoaderEnabled = savedState.getBoolean(KEY_PHOTO_LOADER_ENABLED);
        mQuickContactEnabled = savedState.getBoolean(KEY_QUICK_CONTACT_ENABLED);
        mSearchMode = savedState.getBoolean(KEY_SEARCH_MODE);
        mVisibleScrollbarEnabled = savedState.getBoolean(KEY_VISIBLE_SCROLLBAR_ENABLED);
        mVerticalScrollbarPosition = savedState.getInt(KEY_SCROLLBAR_POSITION);
        mDirectorySearchMode = savedState.getInt(KEY_DIRECTORY_SEARCH_MODE);
        mSelectionVisible = savedState.getBoolean(KEY_SELECTION_VISIBLE);
        //mLegacyCompatibility = savedState.getBoolean(KEY_LEGACY_COMPATIBILITY);
        mQueryString = savedState.getString(KEY_QUERY_STRING);
        mDirectoryResultLimit = savedState.getInt(KEY_DIRECTORY_RESULT_LIMIT);
        mDarkTheme = savedState.getBoolean(KEY_DARK_THEME);

        // Retrieve list state. This will be applied in onLoadFinished
        mListState = savedState.getParcelable(KEY_LIST_STATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        // mContactsPrefs.registerChangeListener(mPreferencesChangeListener);
        //
        // mForceLoad = loadPreferences();
        // mDirectoryListStatus = STATUS_NOT_LOADED;
        // mLoadPriorityDirectoriesOnly = true;
        // startLoading();
        // if (mOnCreateFlag && mAdapter instanceof ContactListAdapter) {
        // Cursor cursor =
        // ContactsDataCacheUtil.getContactListCursor(getActivity());
        // if (cursor != null && cursor.getCount() > 0) {
        // mAdapter.changeCursor(0,cursor);
        // showCount(0, cursor);
        // }
        // mOnCreateFlag = false;
        // }

        doStart();
    }

    protected void doStart() {
        mContactsPrefs.registerChangeListener(mPreferencesChangeListener);
        mForceLoad = loadPreferences();
        mDirectoryListStatus = STATUS_NOT_LOADED;
        mLoadPriorityDirectoriesOnly = true;
        startLoading();
        if (mOnCreateFlag && mAdapter instanceof ContactListAdapter) {
            mOnCreateFlag = false;
        }
    }

    protected void startLoading() {
        if (mAdapter == null) {
            // The method was called before the fragment was started
            return;
        }

        configureAdapter();
        int partitionCount = mAdapter.getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            Partition partition = mAdapter.getPartition(i);
            if (partition instanceof DirectoryPartition) {
                DirectoryPartition directoryPartition = (DirectoryPartition) partition;
                if (directoryPartition.getStatus() == DirectoryPartition.STATUS_NOT_LOADED) {
                    if (directoryPartition.isPriorityDirectory() || !mLoadPriorityDirectoriesOnly) {
                        startLoadingDirectoryPartition(i);
                    }
                }
            } else {
                getLoaderManager().initLoader(i, null, this);
            }
        }

        // Next time this method is called, we should start loading non-priority
        // directories
        mLoadPriorityDirectoriesOnly = false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == DIRECTORY_LOADER_ID) {
            DirectoryListLoader loader = new DirectoryListLoader(mContext);
            mAdapter.configureDirectoryLoader(loader);
            return loader;
        } else {
            CursorLoader loader = createCursorLoader();
            long directoryId = args != null && args.containsKey(DIRECTORY_ID_ARG_KEY) ? args
                    .getLong(DIRECTORY_ID_ARG_KEY) : Directory.DEFAULT;
            mAdapter.configureLoader(loader, directoryId);
            return loader;
        }
    }

    public CursorLoader createCursorLoader() {
        return new CursorLoader(mContext, null, null, null, null, null);
    }

    private void startLoadingDirectoryPartition(int partitionIndex) {
        DirectoryPartition partition = (DirectoryPartition) mAdapter.getPartition(partitionIndex);
        partition.setStatus(DirectoryPartition.STATUS_LOADING);
        long directoryId = partition.getDirectoryId();
        if (mForceLoad) {
            if (directoryId == Directory.DEFAULT) {
                loadDirectoryPartition(partitionIndex, partition);
            } else {
                loadDirectoryPartitionDelayed(partitionIndex, partition);
            }
        } /*else {
            // Bundle args = new Bundle();
            // args.putLong(DIRECTORY_ID_ARG_KEY, directoryId);
            // getLoaderManager().initLoader(partitionIndex, args, this);
        }*/
    }

    /**
     * Queues up a delayed request to search the specified directory. Since
     * directory search will likely introduce a lot of network traffic, we want
     * to wait for a pause in the user's typing before sending a directory
     * request.
     */
    private void loadDirectoryPartitionDelayed(int partitionIndex, DirectoryPartition partition) {
        mRefreshHandler.removeMessages(DIRECTORY_SEARCH_MESSAGE, partition);
        Message msg = mRefreshHandler.obtainMessage(DIRECTORY_SEARCH_MESSAGE, partitionIndex, 0,
                partition);
        mRefreshHandler.sendMessageDelayed(msg, DIRECTORY_SEARCH_DELAY_MILLIS);
    }

    /**
     * Loads the directory partition.
     */
    protected void loadDirectoryPartition(int partitionIndex, DirectoryPartition partition) {
        Bundle args = new Bundle();
        args.putLong(DIRECTORY_ID_ARG_KEY, partition.getDirectoryId());
        getLoaderManager().restartLoader(partitionIndex, args, this);
    }

    /**
     * Cancels all queued directory loading requests.
     */
    private void removePendingDirectorySearchRequests() {
        mRefreshHandler.removeMessages(DIRECTORY_SEARCH_MESSAGE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "Linc onLoadFinished start");
        Activity activity = getActivity();
        // Only check null here, do NOT check isFinishing() and isDestroyed().
        // refer to https://k3.alibaba-inc.com/issue/7857068?versionId=1169325 for detail.
        if (activity == null) {
            Log.i(TAG, "onLoadFinished: activity is null.");
            return;
        }

        int loaderId = loader.getId();
        if (loaderId == DIRECTORY_LOADER_ID) {
            mDirectoryListStatus = STATUS_LOADED;
            mAdapter.changeDirectories(data);
            startLoading();
        } else {
            onPartitionLoaded(loaderId, data);
            if (isSearchMode()) {
                int directorySearchMode = getDirectorySearchMode();
                if (directorySearchMode != DirectoryListLoader.SEARCH_MODE_NONE) {
                    if (mDirectoryListStatus == STATUS_NOT_LOADED) {
                        mDirectoryListStatus = STATUS_LOADING;
                        getLoaderManager().initLoader(DIRECTORY_LOADER_ID, null, this);
                    } else {
                        startLoading();
                    }
                }
            } else {
                mDirectoryListStatus = STATUS_NOT_LOADED;
                getLoaderManager().destroyLoader(DIRECTORY_LOADER_ID);
            }
        }
        updateFishEyeData(data);

        if (PeopleActivity2.sFirstLoad && (getActivity() instanceof PeopleActivity2)) {
            PeopleActivity2.sFirstLoad = false;
            reloadData();
        }
        // int favoriteCount = getFavoriteContactsCount();
        // if(mAdapter instanceof PhoneNumberListAdapter2){
        // FishEyeData.getInstance(mAdapter.getCursor(0),PhoneNumberListAdapter2.PhoneQuery.PHONE_DISPLAY_NAME,
        // favoriteCount).getXingList('#');
        // }else{
        // FishEyeData.getInstance(mAdapter.getCursor(0),ContactQuery.CONTACT_DISPLAY_NAME,
        // favoriteCount).getXingList('#');
        // }
    }

    protected void updateFishEyeData(Cursor data) {
        mSectionMap = ((IndexerListAdapter) mAdapter).getSectionMap();
        int contactCount = data == null ? -1 : data.getCount();
        if (contactCount > 0) {
            initFishEye();
        }
        if (mFishEye != null) {
            // YUNOS BEGIN
            // Description:invisible fisheye when in search mode.
            // author:changjun.bcj
            // date:2014/07/30
            if (isSearchMode() || contactCount <= 0) {
                mFishEye.setVisibility(View.GONE);
            } else {
                mFishEye.setVisibility(View.VISIBLE);
                boolean[] mask = getEnableMask();
                // mFishEye.setAlphaIndex(mEnableFishEyeChar);
                mFishEye.updateAlphaEnableStates(mask);
                // FishEyeData.saveFishEyeIndexChar(mContext,
                // mEnableFishEyeChar);

                // if first load, only 20 contacts are loaded,
                // don't update fisheye with partial data.
                if (!(PeopleActivity2.sFirstLoad && (getActivity() instanceof PeopleActivity2))) {
                    if ((!FeatureOptionAssistant.DISPLAY_FISH_EYE_2ND_LEVEL)
                            || (contactCount > THRESHOLD_FOR_NO_FISHEYE)) {
                        Log.d(TAG, "updateFishEyeData: too many contacts, disable fish eye for performance.");
                        mContactsCache.clearContactsListCache();
                    } else {
                        int nameColumnIndex = mAdapter.getCursor(0).getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY);
                        if (nameColumnIndex < 0) {
                            nameColumnIndex = mAdapter.getCursor(0).getColumnIndex(Contacts.DISPLAY_NAME_ALTERNATIVE);
                        }
                        // If we still has a negative index here, then we must have a
                        // wrong projection,
                        // we want to know which adapter to check (the configureLoader
                        // sets the projection).
                        if (nameColumnIndex < 0) {
                            Log.e(TAG, "updateFishEyeData: Invalid nameColumnIndex, mAdapter=" + mAdapter);
                            return;
                        }
                        Log.d(TAG, "updateFishEyeData: cursor=" + mAdapter.getCursor(0) + "; nameColumnIndex=" + nameColumnIndex);
                        mContactsCache.cacheContactsListCursor(mAdapter.getCursor(0), nameColumnIndex);
                    }
                    mFishEyeHandler.removeMessages(FISHEYEDATA_MESSAGE);
                    Message msg = mFishEyeHandler.obtainMessage(FISHEYEDATA_MESSAGE, mContactsCache.getContactsListCache());
                    msg.arg1 = contactCount;
                    mFishEyeHandler.sendMessage(msg);
                }
            }
            // YUNOS END
        }
    }

    private boolean[] getEnableMask() {
        boolean[] mask = new boolean[FISH_EYE_INDEX.length];
        HashMap<String, Integer> map = ((IndexerListAdapter) mAdapter).getSectionMap();
        if (map == null) {
            //mEnableFishEyeChar = new String[]{};
            return mask;
        }

        //mask[0] = hasFavoriteList();

        Set<String> sections = map.keySet();

        //ArrayList<String> charArray = new ArrayList<String>();
        for (int i = 0; i < FISH_EYE_INDEX.length; i++) {
            mask[i] = (sections.contains(FISH_EYE_INDEX[i])) ? true : false;
           /* if(mask[i]){
                charArray.add(FISH_EYE_INDEX[i]);
            }*/
        }
        /*
        if(charArray.size() > 0){
            mEnableFishEyeChar = charArray.toArray(new String[charArray.size()]);
        }
        */
        return mask;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    protected void onPartitionLoaded(int partitionIndex, Cursor data) {
        if (partitionIndex >= mAdapter.getPartitionCount()) {
            // When we get unsolicited data, ignore it. This could happen
            // when we are switching from search mode to the default mode.
            return;
        }

        mAdapter.changeCursor(partitionIndex, data);
        showCount(partitionIndex, data);

        if (!isLoading()) {
            completeRestoreInstanceState();
        }
    }

    public boolean isLoading() {
        if (mAdapter != null && mAdapter.isLoading()) {
            return true;
        }

        if (isLoadingDirectoryList()) {
            return true;
        }

        return false;
    }

    public boolean isLoadingDirectoryList() {
        return isSearchMode()
                && getDirectorySearchMode() != DirectoryListLoader.SEARCH_MODE_NONE
                && (mDirectoryListStatus == STATUS_NOT_LOADED || mDirectoryListStatus == STATUS_LOADING);
    }

    @Override
    public void onStop() {
        super.onStop();
        mContactsPrefs.unregisterChangeListener();
        // mAdapter.clearPartitions();
    }

    protected void reloadData() {
        if (getActivity() == null) {
            Log.w(TAG, "reloadData: getActivity() is null.");
            return;
        }
        removePendingDirectorySearchRequests();
        mAdapter.onDataReload();
        mLoadPriorityDirectoriesOnly = true;
        mForceLoad = true;
        startLoading();
    }

    /**
     * Configures the empty view. It is called when we are about to populate the
     * list with an empty cursor.
     */
    protected void prepareEmptyView() {
    }

    /**
     * Shows the count of entries included in the list. The default
     * implementation does nothing.
     */
    protected void showCount(int partitionIndex, Cursor data) {
    }

    /**
     * Provides logic that dismisses this fragment. The default implementation
     * does nothing.
     */
    protected void finish() {
    }

    public void setFishEyesDisplayEnabled(boolean flag) {
        mFishEyeDisplayEnabled = flag;
    }

    public void setSectionHeaderDisplayEnabled(boolean flag) {
        if (mSectionHeaderDisplayEnabled != flag) {
            mSectionHeaderDisplayEnabled = flag;
            if (mAdapter != null) {
                mAdapter.setSectionHeaderDisplayEnabled(flag);
            }
            // configureVerticalScrollbar();

            setFishEyesDisplayEnabled(mSectionHeaderDisplayEnabled);
        }
    }

    public boolean isSectionHeaderDisplayEnabled() {
        return mSectionHeaderDisplayEnabled;
    }

    public void setVisibleScrollbarEnabled(boolean flag) {
        if (mVisibleScrollbarEnabled != flag) {
            mVisibleScrollbarEnabled = flag;
            // configureVerticalScrollbar();
        }
    }

    public boolean isVisibleScrollbarEnabled() {
        return mVisibleScrollbarEnabled;
    }

    public void setVerticalScrollbarPosition(int position) {
        if (mVerticalScrollbarPosition != position) {
            mVerticalScrollbarPosition = position;
            // configureVerticalScrollbar();
        }
    }

    /*private void configureVerticalScrollbar() {
        boolean hasScrollbar = isVisibleScrollbarEnabled() && isSectionHeaderDisplayEnabled();
        hasScrollbar = false;

        if (mListView != null) {
            mListView.setFastScrollEnabled(hasScrollbar);
            mListView.setFastScrollAlwaysVisible(hasScrollbar);
            mListView.setVerticalScrollbarPosition(mVerticalScrollbarPosition);
            mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
            int leftPadding = 0;
            int rightPadding = 0;
            if (mVerticalScrollbarPosition == View.SCROLLBAR_POSITION_LEFT) {
                leftPadding = mContext.getResources().getDimensionPixelOffset(
                        R.dimen.list_visible_scrollbar_padding);
            } else {
                rightPadding = mContext.getResources().getDimensionPixelOffset(
                        R.dimen.list_visible_scrollbar_padding);
            }
            mListView.setPadding(leftPadding, mListView.getPaddingTop(), rightPadding,
                    mListView.getPaddingBottom());
        }
    }*/

    public void setPhotoLoaderEnabled(boolean flag) {
        mPhotoLoaderEnabled = flag;
        configurePhotoLoader();
    }

    public boolean isPhotoLoaderEnabled() {
        return mPhotoLoaderEnabled;
    }

    /**
     * Returns true if the list is supposed to visually highlight the selected
     * item.
     */
    public boolean isSelectionVisible() {
        return mSelectionVisible;
    }

    public void setSelectionVisible(boolean flag) {
        this.mSelectionVisible = flag;
    }

    public void setQuickContactEnabled(boolean flag) {
        this.mQuickContactEnabled = flag;
    }

    /**
     * Enter/exit search mode. By design, a fragment enters search mode only
     * when it has a non-empty query text, so the mode must be tightly related
     * to the current query. For this reason this method must only be called by
     * {@link #setQueryString}. Also note this method doesn't call
     * {@link #reloadData()}; {@link #setQueryString} does it.
     */
    protected void setSearchMode(boolean flag) {
        if (mSearchMode != flag) {
            mSearchMode = flag;
            setSectionHeaderDisplayEnabled(!mSearchMode);
            setSelectionVisible(!mSearchMode);

            if (!flag) {
                mDirectoryListStatus = STATUS_NOT_LOADED;
                getLoaderManager().destroyLoader(DIRECTORY_LOADER_ID);
            }

            if (mAdapter != null) {
                mAdapter.setPinnedPartitionHeadersEnabled(false);
                mAdapter.setSearchMode(flag);

                mAdapter.clearPartitions();
                if (!flag) {
                    // If we are switching from search to regular display,
                    // remove all directory
                    // partitions after default one, assuming they are remote
                    // directories which
                    // should be cleaned up on exiting the search mode.
                    mAdapter.removeDirectoriesAfterDefault();
                }
                mAdapter.configureDefaultPartition(false, flag);
            }

            /*if (mListView != null) {
                // mListView.setFastScrollEnabled(!flag);
            }*/
        }
    }

    public final boolean isSearchMode() {
        return mSearchMode;
    }

    public final String getQueryString() {
        return mQueryString;
    }

    public void setQueryString(String queryString, boolean delaySelection) {
        // Normalize the empty query.
        if (TextUtils.isEmpty(queryString))
            queryString = null;

        if (!TextUtils.equals(mQueryString, queryString)) {
            mQueryString = queryString;
            setSearchMode(!TextUtils.isEmpty(mQueryString));

            if (mAdapter != null) {
                mAdapter.setQueryString(queryString);
                reloadData();
            }
        }
    }

    public int getDirectorySearchMode() {
        return mDirectorySearchMode;
    }

    public void setDirectorySearchMode(int mode) {
        mDirectorySearchMode = mode;
    }

    /*public boolean isLegacyCompatibilityMode() {
        return mLegacyCompatibility;
    }

    public void setLegacyCompatibilityMode(boolean flag) {
        mLegacyCompatibility = flag;
    }*/

    protected int getContactNameDisplayOrder() {
        return mDisplayOrder;
    }

    protected void setContactNameDisplayOrder(int displayOrder) {
        mDisplayOrder = displayOrder;
        if (mAdapter != null) {
            mAdapter.setContactNameDisplayOrder(displayOrder);
        }
    }

    public int getSortOrder() {
        return mSortOrder;
    }

    public void setSortOrder(int sortOrder) {
        mSortOrder = sortOrder;
        if (mAdapter != null) {
            mAdapter.setSortOrder(sortOrder);
        }
    }

    public void setDirectoryResultLimit(int limit) {
        mDirectoryResultLimit = limit;
    }

    public void setContextMenuAdapter(ContextMenuAdapter adapter) {
        mContextMenuAdapter = adapter;
        if (mListView != null) {
            mListView.setOnCreateContextMenuListener(adapter);
        }
    }

    public ContextMenuAdapter getContextMenuAdapter() {
        return mContextMenuAdapter;
    }

    protected boolean loadPreferences() {
        boolean changed = false;
        if (getContactNameDisplayOrder() != mContactsPrefs.getDisplayOrder()) {
            setContactNameDisplayOrder(mContactsPrefs.getDisplayOrder());
            changed = true;
        }

        if (getSortOrder() != mContactsPrefs.getSortOrder()) {
            setSortOrder(mContactsPrefs.getSortOrder());
            changed = true;
        }

        return changed;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        onCreateView(inflater, container);

        mAdapter = createListAdapter();

        boolean searchMode = isSearchMode();
        mAdapter.setSearchMode(searchMode);
        mAdapter.configureDefaultPartition(false, searchMode);
        mAdapter.setPhotoLoader(mPhotoManager);
        mListView.setAdapter(mAdapter);

        if (!isSearchMode()) {
            mListView.setFocusableInTouchMode(true);
            mListView.requestFocus();
        }

        return mView;
    }

    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        mView = inflateView(inflater, container);

        mListView = (ListView) mView.findViewById(android.R.id.list);
        if (mListView == null) {
            throw new RuntimeException("Your content must have a ListView whose id attribute is "
                    + "'android.R.id.list'");
        }

        mListView.setFastScrollEnabled(false);
        mListView.setFastScrollAlwaysVisible(false);
        mListView.setFadingEdgeLength(0);
        mListView.setOverScrollMode(ListView.OVER_SCROLL_NEVER);

        View emptyView = mView.findViewById(android.R.id.empty);
        if (emptyView != null) {
            mListView.setEmptyView(emptyView);
            if (emptyView instanceof ContactListEmptyView) {
                mEmptyView = (ContactListEmptyView) emptyView;
            }
        }

        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnFocusChangeListener(this);
        mListView.setOnTouchListener(this);
        // mListView.setFastScrollEnabled(!isSearchMode());

        mListView.setFooterDividersEnabled(false);

        // We manually save/restore the listview state
        mListView.setSaveEnabled(false);

        if (mContextMenuAdapter != null) {
            mListView.setOnCreateContextMenuListener(mContextMenuAdapter);
        }

        mHandler = new ContactListHandler(this);
        showDeleteContactsProgress(MultiSelectPeopleFragment.isDeletingContacts());
        MultiSelectPeopleFragment.addContactBatchDeleteStatusListener(mBatchDeleteStatusListener);

        configurePhotoLoader();
    }

    private void showDeleteContactsProgress(boolean display) {
        Log.i(TAG, "showDeleteContactsProgress: display="+display);
        if (getActivity() == null) {
            Log.w(TAG, "showDeleteContactsProgress: fragment is detached. quit.");
            return;
        }
        if (!(mView instanceof ViewGroup)) {
            Log.w(TAG, "showDeleteContactsProgress: mView is not a ViewGroup. quit.");
            return;
        }
        ViewGroup container = (ViewGroup) mView;
        if (display) {
            if ((mDeleteProgress != null) || (mInflater == null)) {
                return;
            }
            mDeleteProgress = mInflater.inflate(R.layout.delete_progress_in_contact_list, container, false);
            container.addView(mDeleteProgress, 0);
        } else {
            if (mDeleteProgress == null) {
                return;
            }
            container.removeView(mDeleteProgress);
            mDeleteProgress = null;
        }
    }

    private void initFishEye() {
        if (mFishEye != null) {
            return;
        }
        mHeaderViewHeight = dip2px(mContext, R.dimen.pinned_header_list_view_height);

        mFishEye = (FishEyeView) mView.findViewById(R.id.fisheye);
        mFishEye.initOrientation();
        mFishEye.setAlphaIndex(FISH_EYE_INDEX);
        setFishEyeMargin(FishEyeView.orientation);
        mFishEye.setParentViewPager(mParentViewPager);
        if (mFishEye != null) {
            mFishEye.setAdapter(new FishEyeView.FishEyeViewAdapter() {

                @Override
                public void onFishEyeViewCreate(FishEyeView fisheye) {
                    setFishEyeSize(fisheye);
                }

            });
            // mFishEye.updateAlphaEnableStates(ContactsDataCacheUtil.getFishEyeMask(getActivity()));
            mFishEye.setOnChildClickListener(this);

            if (mFishEyeDisplayEnabled) {
                mFishEye.setVisibility(View.VISIBLE);
            } else {
                mFishEye.setVisibility(View.GONE);
            }
        }
    }

    private void setFishEyeSize(FishEyeView fisheye) {

        //DisplayMetrics metric = new DisplayMetrics();
        //getActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
        fisheye.setDepth(FishEyeData.FISHEYE_DEPTH);
        /*
        DisplayMetrics metric = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
        int barWidth = getResources().getDimensionPixelSize(
                R.dimen.fisheye_alpha_bar_width);
        int barHeight = getResources().getDimensionPixelSize(
                R.dimen.fisheye_alpha_bar_height);

        fisheye.setDepth(FeatureOptionAssistant.isInternationalSupportted() ? 2 : 3);
        //fisheye.setAlphaIndex(mEnableFishEyeChar);
        fisheye.setAlphaBarSize(barWidth, barHeight);
        fisheye.set1stLevelSize(
                getResources().getDimensionPixelSize(
                        R.dimen.fisheye_1st_bar_width), getResources()
                        .getDimensionPixelSize(R.dimen.fisheye_1st_bar_height));
        final int windowWidth = metric.widthPixels;
        int textHolderW = windowWidth * 9 / 10;
        int textHolderH = LayoutParams.WRAP_CONTENT;
        if (FishEyeView.orientation == FishEyeOrientation.Vertical) {
            textHolderW = LayoutParams.WRAP_CONTENT;
            textHolderH = windowWidth * 9 / 10;
        }
        fisheye.set2ndLevelSize(textHolderW, textHolderH);
        */
    }

    @Override
    public void onChildClick(int[] partition) {
        if (mSectionMap == null) {
            Log.i(TAG, "onChildClick: quit for section map is null.");
            return;
        }

        // Cursor cPart = mContactsCache.getContactListCursor();
        // int iDisplayName = ContactQuery.CONTACT_DISPLAY_NAME;
        String[] data = null;
        int topPadding = 0;
        int topPosition = 0;
        if (partition.length == 1) {
            String star = FISH_EYE_INDEX[0];
            if (star.equals(FISH_EYE_INDEX[partition[0]])) {
                topPosition = 0;
            } else {
                Integer pos = mSectionMap.get(FISH_EYE_INDEX[partition[0]]);
                if (pos != null) {
                    topPosition = pos;
                }
                // Toast.makeText(getActivity(), position +":"+partition ,
                // Toast.LENGTH_SHORT).show();
                // topPosition = position +
                // getListStartOffset_FishEye_1stLayer();
                if (mFishData.isReady()) {
                    char c = FISH_EYE_INDEX[partition[0]].charAt(0);

                    data = mFishData.getXingList(c);
                }
            }
            UsageReporter.onClick(null, DefaultContactBrowseListFragment.class.getSimpleName(),
                    UsageReporter.ContactsListPage.FISH_EYE_1ST);
        } else if (partition.length == 2) {
            if (!mFishData.isReady()) {
                Log.i(TAG, "onChildClick: partition.length is 2, fishEyeData is not ready.");
                return;
            }

            char c = FISH_EYE_INDEX[partition[0]].charAt(0);
            int pos2 = partition[1];

            topPosition = mFishData.getXingPos(c, pos2);
            data = mFishData.getMingList(c, pos2);
            if (!isSectionFirstPosition(topPosition)) {
                topPadding = mHeaderViewHeight;
            }
            UsageReporter.onClick(null, DefaultContactBrowseListFragment.class.getSimpleName(),
                    UsageReporter.ContactsListPage.FISH_EYE_2ND);
        } else if (partition.length == 3) {
            if (!mFishData.isReady()) {
                Log.i(TAG, "onChildClick: partition.length is 3, fishEyeData is not ready.");
                return;
            }

            char c = FISH_EYE_INDEX[partition[0]].charAt(0);
            int pos2 = partition[1];
            int pos3 = partition[2];
            topPosition = mFishData.getMingPos(c, pos2, pos3);
            if (!isSectionFirstPosition(topPosition)) {
                topPadding = mHeaderViewHeight;
            }
            data = null;
            UsageReporter.onClick(null, DefaultContactBrowseListFragment.class.getSimpleName(),
                    UsageReporter.ContactsListPage.FISH_EYE_3RD);
        }

        // Highlight the top position list item text color.
        if (mAdapter != null) {
            mAdapter.setHiliteTopPosition(topPosition);
            mAdapter.notifyDataSetChanged();
        }
        mListPosition = topPosition;
        mListView.setSelectionFromTop(topPosition, topPadding);
        mFishEye.onNotifyDataDone(0, partition, data);
    }
    @Override
    public void onChildClear() {
        if(mAdapter != null && mAdapter.getHiliteTopPosition() != -1){
            mAdapter.setHiliteTopPosition(-1);
            mAdapter.notifyDataSetChanged();
        }
    }
    protected boolean isSectionFirstPosition(int position) {
        boolean ret = false;
        if(mAdapter == null || position < 0 || position >= mAdapter.getCount()){
            return ret;
        }
        int sectionIdx = mAdapter.getSectionForPosition(position);
        int sectionFirstPosition = mAdapter.getPositionForSection(sectionIdx);
        if(sectionFirstPosition == position){
            ret = true;
        }
        return ret;
    }
    protected int getListStartOffset_FishEye_1stLayer() {
        if (hasFavoriteList()) {
            return getFavoriteContactsCount();
        }
        return 0;
    }

    protected int getListStartOffset_FishEye_2nd_3rd_Layer() {
        return 0;
    }

    protected void configurePhotoLoader() {
        if (isPhotoLoaderEnabled() && mContext != null) {
            if (mPhotoManager == null) {
                mPhotoManager = ContactPhotoManager.getInstance(mContext);
            }
            if (mListView != null) {
                mListView.setOnScrollListener(this);
            }
            if (mAdapter != null) {
                mAdapter.setPhotoLoader(mPhotoManager);
            }
        }
    }

    protected void configureAdapter() {
        if (mAdapter == null) {
            return;
        }

        //mAdapter.setQuickContactEnabled(mQuickContactEnabled);
        mAdapter.setQueryString(mQueryString);
        mAdapter.setDirectorySearchMode(mDirectorySearchMode);
        mAdapter.setPinnedPartitionHeadersEnabled(false);
        mAdapter.setContactNameDisplayOrder(mDisplayOrder);
        mAdapter.setSortOrder(mSortOrder);
        mAdapter.setSectionHeaderDisplayEnabled(mSectionHeaderDisplayEnabled);
        mAdapter.setSelectionVisible(mSelectionVisible);
        mAdapter.setDirectoryResultLimit(mDirectoryResultLimit);
        //mAdapter.setDarkTheme(mDarkTheme);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                // Save the current position
                mListPosition = view.getFirstVisiblePosition() - getFavoriteContactsCount();
                if (mListPosition < 0) {
                    mListPosition = 0;
                }

                Log.d(TAG, "[ListPos]mListPostion(1)=" + mListPosition);
                if (isPhotoLoaderEnabled()) {
                    mPhotoManager.resume();
                }
                NameConvertWorker.resume();
                break;
            case OnScrollListener.SCROLL_STATE_FLING:
            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                if (isPhotoLoaderEnabled()) {
                    mPhotoManager.pause();
                }
                NameConvertWorker.pause();
                break;
        }
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mPhotoManager.pause();
        } else if (isPhotoLoaderEnabled()) {
            mPhotoManager.resume();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        hideSoftKeyboard();

        int adjPosition = position - mListView.getHeaderViewsCount();
        if (adjPosition >= 0) {
            onItemClick(adjPosition, id);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        onItemLongClick(view, position, id);
        return true;
    }

    public void onItemLongClick(View v, int position, long id) {
        // need override by child to do something
    }

    private void hideSoftKeyboard() {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mListView.getWindowToken(), 0);
    }

    /**
     * Dismisses the soft keyboard when the list takes focus.
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == mListView && hasFocus) {
            hideSoftKeyboard();
        }
    }

    /**
     * Dismisses the soft keyboard when the list is touched.
     */
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (view == mListView) {
            hideSoftKeyboard();
            if (mFishEye != null) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    mFishEye.reset();
                }
            }
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        removePendingDirectorySearchRequests();
        if (mFishEye != null) {
            mFishEye.reset();
        }
    }

    /**
     * Restore the list state after the adapter is populated.
     */
    protected void completeRestoreInstanceState() {
        if (mListState != null) {
            mListView.onRestoreInstanceState(mListState);
            mListState = null;
        }
    }
    protected void setEmptyText(int resourceId) {
        TextView empty = (TextView) getEmptyView().findViewById(R.id.emptyText);
        empty.setText(mContext.getText(resourceId));
        empty.setVisibility(View.VISIBLE);
    }

    // TODO redesign into an async task or loader
    protected boolean isSyncActive() {
        /*
         * Account[] accounts = AccountManager.get(mContext).getAccounts(); if
         * (accounts != null && accounts.length > 0) { IContentService
         * contentService = ContentResolver.getContentService(); for (Account
         * account : accounts) { try { if (contentService.isSyncActive(account,
         * ContactsContract.AUTHORITY)) { return true; } } catch
         * (RemoteException e) { Log.e(TAG, "Could not get the sync status"); }
         * } }
         */
        return false;
    }

    protected boolean hasIccCard() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.hasIccCard();
    }

//    public void setDarkTheme(boolean value) {
//        mDarkTheme = value;
//        if (mAdapter != null)
//            mAdapter.setDarkTheme(value);
//    }

    public boolean hasFavoriteList() {
        return false;
    }

    /**
     * Processes a result returned by the contact picker.
     */
    public void onPickerResult(Intent data) {
        throw new UnsupportedOperationException("Picker result handler is not implemented.");
    }

    private ContactsPreferences.ChangeListener mPreferencesChangeListener = new ContactsPreferences.ChangeListener() {
        @Override
        public void onChange() {
            loadPreferences();
            reloadData();
        }
    };

    private ViewPager mParentViewPager;

    public void setParentViewPager(ViewPager pager) {
        mParentViewPager = pager;
    }

    private OnSharedPreferenceChangeListener mOnOffPreference = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.i(TAG, "onSharedPreferenceChanged: changed, key = " + key);
            Activity activity = getActivity();
            if ((activity == null) || activity.isFinishing() || activity.isDestroyed()) {
                Log.w(TAG, "mOnOffPreference.onSharedPreferenceChanged: activity is null or not active, quit.");
                return;
            }
            if (ContactsSettingActivity.CONTACT_PHOTO_ONOFF_PREFERENCE.equals(key)
                    || SimContactUtils.KEY_SIM1_ICC_READY.equals(key)
                    || SimContactUtils.KEY_SIM2_ICC_READY.equals(key)
                    || KEY_PREFS_CONTACTS_CHANGED.equals(key)) {
                reloadData();
            } else if (key.equals(ContactsSettingActivity.CONTACT_FISH_EYES_ORIENTATION_PREFERENCE)) {
                boolean value = sharedPreferences.getBoolean(key, false);
                if (value) {
                    changeFishOrientation(FishEyeOrientation.Vertical);
                } else {
                    changeFishOrientation(FishEyeOrientation.Horizon);
                }
            }
        }
    };

    private void changeFishOrientation(FishEyeOrientation orientation) {
        if(mFishEye == null){
            FishEyeView.orientation = orientation;
            return;
        }
        setFishEyeMargin(orientation);
        mFishEye.setOrientation(orientation);
        setFishEyeSize(mFishEye);
        mFishEye.changeFishEyeOrientation(orientation);
    }

    private void setFishEyeMargin(FishEyeOrientation orientation) {
        LayoutParams lp = (FrameLayout.LayoutParams) mListView
                .getLayoutParams();
        if (orientation == FishEyeOrientation.Vertical) {
            lp.rightMargin = getResources().getDimensionPixelSize(
                    R.dimen.fisheye_alpha_bar_width);
            lp.bottomMargin = 0;
            mFishEye.setGravity(Gravity.END);
        } else {
            lp.rightMargin = 0;
            lp.bottomMargin = getResources().getDimensionPixelSize(
                    R.dimen.fisheye_alpha_bar_height);
            mFishEye.setGravity(Gravity.BOTTOM);
        }
        mListView.setLayoutParams(lp);
    }

    /*public void setFishEyeBottom(int bottom){
        if(mFishEye != null && FishEyeView.orientation == FishEyeOrientation.Vertical){
            LayoutParams lp = (FrameLayout.LayoutParams) mFishEye.getLayoutParams();
            lp.bottomMargin = bottom;
        }
    }*/

    @Override
    public void onSimStateChanged(int slot, String state) {
        final Activity targetActivity = getActivity();
        if (targetActivity != null && SimUtil.IS_YUNOS) {
            reloadData();
        }
    }
}
