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
 * limitations under the License
 */

package com.yunos.alicontacts.detail;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.StatusUpdates;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.collect.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.yunos.alicontacts.CallUtil;
import com.yunos.alicontacts.Collapser;
import com.yunos.alicontacts.Collapser.Collapsible;
import com.yunos.alicontacts.ContactPresenceIconUtil;
import com.yunos.alicontacts.ContactSaveService;
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.GroupMetaData;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.TypePrecedence;
import com.yunos.alicontacts.activities.ContactDetailActivity;
import com.yunos.alicontacts.activities.ContactDetailActivity.FragmentKeyListener;
import com.yunos.alicontacts.aliutil.provider.AliContactsContract.CommonDataKinds.Sns;
import com.yunos.alicontacts.database.CallDetailQuery;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.database.tables.CallsTable;
import com.yunos.alicontacts.database.util.NumberNormalizeUtil;
import com.yunos.alicontacts.database.util.NumberServiceHelper;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.dialpad.calllog.CallLogRecordActivity;
import com.yunos.alicontacts.dialpad.calllog.CallerViewQuery;
import com.yunos.alicontacts.editor.Editor.EditorListener;
import com.yunos.alicontacts.editor.GroupMembershipView;
import com.yunos.alicontacts.editor.RingtoneUtils;
import com.yunos.alicontacts.editor.SelectAccountDialogFragment;
import com.yunos.alicontacts.model.Contact;
import com.yunos.alicontacts.model.RawContact;
import com.yunos.alicontacts.model.RawContactDeltaList;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.model.account.AccountType.EditType;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.alicontacts.model.dataitem.DataItem;
import com.yunos.alicontacts.model.dataitem.DataKind;
import com.yunos.alicontacts.model.dataitem.EmailDataItem;
import com.yunos.alicontacts.model.dataitem.EventDataItem;
import com.yunos.alicontacts.model.dataitem.GroupMembershipDataItem;
import com.yunos.alicontacts.model.dataitem.ImDataItem;
import com.yunos.alicontacts.model.dataitem.NicknameDataItem;
import com.yunos.alicontacts.model.dataitem.NoteDataItem;
import com.yunos.alicontacts.model.dataitem.OrganizationDataItem;
import com.yunos.alicontacts.model.dataitem.PhoneDataItem;
import com.yunos.alicontacts.model.dataitem.RelationDataItem;
import com.yunos.alicontacts.model.dataitem.SipAddressDataItem;
import com.yunos.alicontacts.model.dataitem.SnsDataItem;
import com.yunos.alicontacts.model.dataitem.StructuredNameDataItem;
import com.yunos.alicontacts.model.dataitem.StructuredPostalDataItem;
import com.yunos.alicontacts.model.dataitem.WebsiteDataItem;
import com.yunos.alicontacts.platform.PDUtils;
import com.yunos.alicontacts.plugins.PluginPlatformPrefs;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.AliTextUtils;
import com.yunos.alicontacts.util.ClipboardUtils;
import com.yunos.alicontacts.util.Constants;
import com.yunos.alicontacts.util.ContactsTextUtils;
import com.yunos.alicontacts.util.DataStatus;
import com.yunos.alicontacts.util.DateUtils;
import com.yunos.alicontacts.util.FeatureOptionAssistant;
import com.yunos.alicontacts.util.StructuredPostalUtils;
import com.yunos.alicontacts.weibo.WeiboFinals;
import com.yunos.common.DebugLog;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionSheet;
import yunos.support.v4.app.Fragment;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* YUNOS BEGIN PB */
//##module [Smart Gesture] ##BugID:168585
//##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
//##description: Support Single & Dual Orient Smart Gesture based on proximity
import com.aliyunos.smartgesture.SmartGestureDetector;
import com.aliyunos.utils.Features;
/* YUNOS END PB */

public class ContactDetailFragment extends Fragment implements FragmentKeyListener, SelectAccountDialogFragment.Listener,
        OnItemClickListener, OnLongClickListener {

    private static final String PERFORMANCE_TAG = "Performance";
    private static final boolean DEBUG = true;
    private static final String TAG = "ContactDetailFragment";
    private static final String ONLY_YEAR_FORMAT = "yyyy";
    private static final String MONTH_DAY_FORMAT = "MM-dd ";
    private static final String FULL_FORMAT = "yyyy-MM-dd ";

    private static final boolean sMultiSimEnable = SimUtil.MULTISIM_ENABLE;
    private interface ContextMenuIds {
        static final int COPY_TEXT = 0;
        static final int CLEAR_DEFAULT = 1;
        static final int SET_DEFAULT = 2;
    }

    private static final String KEY_CONTACT_URI = "contactUri";
    private static final String KEY_LIST_STATE = "liststate";
    private static final String ONLY_FOR_REPORT_FLAG = "forreport";

    /**
     * This flag is set when report type is triggered by a long click operation.
     * Because the flag is set in the intent of an entry,
     * and the entry might be accessed in future operation.
     * So the flag is set on long click listener,
     * and once the long click report is committed, the flag must be cleared.
     * Otherwise, the following click might be reported as long click.
     */
    private static final int LONG_CLICK_REPORT_TYPE = 0x10000;
    private static final int REPORT_PHONE = 0x01;
    private static final int REPORT_SMS = 0x02;
    private static final int REPORT_SINA_WEIBO = 0x03;
    private static final int REPORT_VIDEO_CALL = 0x04;
    private static final int REPORT_CUSTOM_DATA = 0x0FFFF;
    private static final String REPORT_CUSTOM_SOURCE_KEY = "source";
    // The prefix text won't be sent to report, because the prefix is too common in the types data.
    private static final String REPORT_CUSTOM_SOURCE_VALUE_PREVIX = "vnd.android.cursor.item/";

    private ContactDetailActivity mActivity;
    private Context mContext;
    private View mView;
    private Uri mLookupUri;
    private Listener mListener;

    private Contact mContactData;
    private ListView mListView;
    private ViewAdapter mAdapter;
    private Uri mPrimaryPhoneUri = null;
    public Uri mCurrentPhotoUri;
    private PhotoHandler mCurrentPhotoHandler;
    private int mCallCount;

    private Cursor mCalls;
    private ArrayList<String> mLastNumberList;
    private static String mPhoneNumber;

    private final ContactDetailPhotoSetter mPhotoSetter = new ContactDetailPhotoSetter();

    private String mDefaultCountryIso;
    private boolean mContactHasSocialUpdates;

    /* YUNOS BEGIN PB */
    //##module [Smart Gesture] ##BugID:168585
    //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
    //##description: Support Single & Dual Orient Smart Gesture based on proximity
    private SmartGestureDetector mSmartGestureDetector;
    /*YUNOS END PB*/
    private ArrayList<String> mNumberList = new ArrayList<String>();
    /**
     * Device capability: Set during buildEntries and used in the long-press
     * context menu
     */
    private boolean mHasPhone;

    /**
     * Device capability: Set during buildEntries and used in the long-press
     * context menu
     */
    private boolean mHasSms;

    /**
     * Device capability: Set during buildEntries and used in the long-press
     * context menu
     */
    private boolean mHasSip;

    /**
     * The view shown if the detail list is empty. We set this to the list view
     * when first bind the adapter, so that it won't be shown while we're
     * loading data.
     */
    private View mEmptyView;

    /**
     * Saved state of the {@link ListView}. This must be saved and applied to
     * the {@ListView} only when the adapter has been populated
     * again.
     */
    private Parcelable mListState;

    /**
     * Lists of specific types of entries to be shown in contact details.
     */
    private ArrayList<DetailViewEntry> mPhoneEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mSmsEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mEmailEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mOrganizationEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mPostalEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mImEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mNicknameEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mGroupEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mRelationEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mNoteEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mWebsiteEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mSipEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mEventEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mSnsEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<DetailViewEntry> mRingtoneEntries = new ArrayList<DetailViewEntry>();
    private ArrayList<ViewEntry> mCalllogEntries = new ArrayList<ViewEntry>();
    private final Map<AccountType, List<DetailViewEntry>> mOtherEntriesMap = new HashMap<AccountType, List<DetailViewEntry>>();
    private ArrayList<ViewEntry> mAllEntries = new ArrayList<ViewEntry>();
    private LayoutInflater mInflater;

    private boolean mIsUniqueNumber;
    private boolean mIsUniqueEmail;

    private ListPopupWindow mPopup;

    private enum CalllogViewState {
        SINGLELINE, MULTILINE
    }

    private CalllogViewState mCalllogViewState = CalllogViewState.SINGLELINE;

    private static final int REQUEST_CODE_RECORD_PATH = 10;

    /**
     * This is to forward touch events to the list view to enable users to
     * scroll the list view from the blank area underneath the static photo when
     * the layout with static photo is used.
     */
    private OnTouchListener mForwardTouchToListView = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mListView != null) {
                mListView.dispatchTouchEvent(event);
                return true;
            }
            return false;
        }
    };

    /**
     * This is to forward drag events to the list view to enable users to scroll
     * the list view from the blank area underneath the static photo when the
     * layout with static photo is used.
     */
    private OnDragListener mForwardDragToListView = new OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (mListView != null) {
                mListView.dispatchDragEvent(event);
                return true;
            }
            return false;
        }
    };

    public ContactDetailFragment() {
        // Explicit constructor for inflation
    }

    private static void sendReport(Context context, Intent intent) {
        String message = null;
        HashMap<String, String> param = null;
        int reportType = intent.getIntExtra(ONLY_FOR_REPORT_FLAG, 0);
        boolean fromLongClick = (reportType & LONG_CLICK_REPORT_TYPE) != 0;
        reportType &= ~LONG_CLICK_REPORT_TYPE;
        intent.putExtra(ONLY_FOR_REPORT_FLAG, reportType);
        if (reportType == REPORT_PHONE) {
            if (fromLongClick) {
                message = UsageReporter.ContactsDetailPage.DETAL_LC_MAKE_CALL;
            } else {
                message = UsageReporter.ContactsDetailPage.DETAL_CALL_ICON_CLICKED;
            }
        } else if (reportType == REPORT_VIDEO_CALL) {
            message = UsageReporter.ContactsDetailPage.DETAL_VIDEO_CALL_ICON_CLICKED;
        } else if (reportType == REPORT_SMS) {
            message = UsageReporter.ContactsDetailPage.DETAL_MESSAGE_ICON_CLICKED;
        } else if (reportType == REPORT_SINA_WEIBO) {
            message = UsageReporter.ContactsDetailPage.DETAL_SINA_WEIBO_CLICKED;
        } else if (reportType == REPORT_CUSTOM_DATA) {
            message = UsageReporter.ContactsDetailPage.DETAL_THIRD_PARTY_IM_CLICKED;
            String source = intent.getType();
            if (source != null) {
                if (source.startsWith(REPORT_CUSTOM_SOURCE_VALUE_PREVIX)) {
                    source = source.substring(REPORT_CUSTOM_SOURCE_VALUE_PREVIX.length());
                }
                param = new HashMap<String, String>();
                param.put(REPORT_CUSTOM_SOURCE_KEY, source);
            }
        } else {
            return;
        }
        if (context != null && context instanceof Activity) {
            UsageReporter.onClick((Activity) context, null, message, param);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(PERFORMANCE_TAG, "onCreate on detail fragment.");
        if (savedInstanceState != null) {
            mLookupUri = savedInstanceState.getParcelable(KEY_CONTACT_URI);
            mListState = savedInstanceState.getParcelable(KEY_LIST_STATE);
        }

        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
        if (Features.SUPPORT_SINGLE_ORIENT_GESTURE || Features.SUPPORT_DUAL_ORIENT_GESTURE) {
            mSmartGestureDetector = new SmartGestureDetector(mContext,
                new SmartGestureDetector.OnSmartGestureListener(){
                   public void onNext() {
                       /*YUNOS BEGIN PB*/
                       //##date 2016-03-11  ## Author:xiongchao.lxc@alibaba-inc.om
                       //##BugID:7990395:don't dial when the phone is in use
                       if(!PDUtils.isPhoneIdle()) {
                           Log.d(TAG, "Rushon --> phone is in use , skip !!!");
                           return;
                       }
                       /*YUNOS END PB*/
                      Log.d(TAG,"Rushon --> NGuesture On Next");
                      callNumber();
                   }
                   public void onPrev() {}
                });
            mSmartGestureDetector.setMode(SmartGestureDetector.MODE_PROXIMITY_FALLING, 1000);
        }
        /*YUNOS END PB*/
    }

    /* YUNOS BEGIN PB */
    //##module [Smart Gesture] ##BugID:168585
    //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
    //##description: Support Single & Dual Orient Smart Gesture based on proximity
    private void callNumber() {
        if(mPhoneNumber != null && !mPhoneNumber.isEmpty()){
            Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mPhoneNumber));
            mContext.startActivity(dialIntent);
        }
    }
    /*YUNOS END PB*/

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"Rushon --> onStop");
        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
        if (Features.SUPPORT_SINGLE_ORIENT_GESTURE || Features.SUPPORT_DUAL_ORIENT_GESTURE)
        {
            if (android.os.SystemProperties.getBoolean("persist.sys.ng_autodial", false)) {
                Log.d(TAG,"Rushon --> Contact Detail fragment NGuesture stop!");
                if(mSmartGestureDetector!=null) mSmartGestureDetector.stop();
            }
        }
        /*YUNOS END PB */
        if (mCurrentPhotoHandler != null) {
            mCurrentPhotoHandler.cancel();
        }
        if (DEBUG)
            Log.d(PERFORMANCE_TAG, "onStop on detail fragment.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCalls != null) {
            mCalls.close();
            mCalls = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_CONTACT_URI, mLookupUri);
        if (mListView != null) {
            outState.putParcelable(KEY_LIST_STATE, mListView.onSaveInstanceState());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SimContactUtils.observeVolteAttachChangeByPlatform(mContext, mVolteAttachHandler, MSG_VOLTE_ATTACH_STATE_CHANGED);
        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
        if (Features.SUPPORT_SINGLE_ORIENT_GESTURE || Features.SUPPORT_DUAL_ORIENT_GESTURE)
        {
            if (android.os.SystemProperties.getBoolean("persist.sys.ng_autodial", false)) {
                Log.d(TAG,"Rushon --> Contact Detail Fragment NGuesture start!");
                if(mSmartGestureDetector!=null) mSmartGestureDetector.start();
            }
        }
        /*YUNOS END PB */
    }

    @Override
    public void onPause() {
        dismissPopupIfShown();
        super.onPause();
        SimContactUtils.unObserveVolteAttachChangedByPlatform(mContext);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (ContactDetailActivity) activity;
        mContext = activity;
        mDefaultCountryIso = ContactsUtils.getCurrentCountryIso(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mView = inflater.inflate(R.layout.contact_detail_fragment, container, false);
        /**
         * Set the touch and drag listener to forward the event to the mListView
         * so that vertical scrolling can happen from outside of the list view.
         */
        mView.setOnTouchListener(mForwardTouchToListView);
        mView.setOnDragListener(mForwardDragToListView);

        mInflater = inflater;

        mListView = (ListView) mView.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        mListView.setItemsCanFocus(true);

        /**
         * Don't set it to mListView yet. We do so later when we bind the
         * adapter.
         */
        mEmptyView = mView.findViewById(android.R.id.empty);
        return mView;
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    protected Listener getListener() {
        return mListener;
    }

    protected Contact getContactData() {
        return mContactData;
    }

    public Uri getUri() {
        return mLookupUri;
    }

    public void setData(Contact result) {
        mContactData = result;
        if (result != null) {
            mLookupUri = result.getLookupUri();
        }

        bindData();
    }

    protected void bindData() {
        if (DEBUG)
            Log.d(PERFORMANCE_TAG, "bindData on detail fragment start.");
        if (mActivity == null || mView == null) {
            Log.e(TAG, "bindData() mActivity or mView is null!!!");
            return;
        }

        if (mContactData == null) {
            Log.w(TAG, "bindData() mContactData is still null!!!");
            mListView.setEmptyView(mEmptyView);
            return;
        }

        // Figure out if the contact has social updates or not
        mContactHasSocialUpdates = !mContactData.getStreamItems().isEmpty();

        // Scan plugins
        // updatePluginView();

        // Build up the contact entries
        buildEntries();

        // Collapse similar data items for select {@link DataKind}s.
        Collapser.collapseList(mPhoneEntries);
        Collapser.collapseList(mSmsEntries);
        Collapser.collapseList(mEmailEntries);
        Collapser.collapseList(mPostalEntries);
        Collapser.collapseList(mImEntries);
        Collapser.collapseList(mEventEntries);
        Collapser.collapseList(mSnsEntries);
        Collapser.collapseList(mRingtoneEntries);

        mIsUniqueNumber = mPhoneEntries.size() == 1;
        mIsUniqueEmail = mEmailEntries.size() == 1;

        // Make one aggregated list of all entries for display to the user.
        setupFlattenedList();

        // can start to sync calllogs
        loadCalllogs();

        if (mAdapter == null) {
            mAdapter = new ViewAdapter();
            mListView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }

        // Restore {@link ListView} state if applicable because the adapter is
        // now populated.
        if (mListState != null) {
            mListView.onRestoreInstanceState(mListState);
            mListState = null;
        }

        if (DEBUG)
            Log.d(PERFORMANCE_TAG, "bindData on detail fragment end.");
    }

    /**
     * Build up the entries to display on the screen.
     */
    private final void buildEntries() {
        mHasPhone = true;// PhoneCapabilityTester.isPhone(mContext);
        mHasSms = true;// PhoneCapabilityTester.isSmsIntentRegistered(mContext);
        mHasSip = false; // PhoneCapabilityTester.isSipPhone(mContext);

        // Clear out the old entries
        clearAllEntries();

        mPrimaryPhoneUri = null;

        // Build up method entries
        if (mContactData == null) {
            return;
        }

        ArrayList<String> tmpNumbers = new ArrayList<String>();
        mNoteEntries.clear();

        LongSparseArray<GroupMembershipDataItem> membershipGroups = new LongSparseArray<GroupMembershipDataItem>();
        for (RawContact rawContact : mContactData.getRawContacts()) {
            final long rawContactId = rawContact.getId();
            // List<DataItem> items = rawContact.getDataItems();
            for (DataItem dataItem : rawContact.getDataItems()) {
                dataItem.setRawContactId(rawContactId);

                if (dataItem.getMimeType() == null)
                    continue;

                if (dataItem instanceof GroupMembershipDataItem) {
                    GroupMembershipDataItem groupMembership = (GroupMembershipDataItem) dataItem;
                    Long groupId = groupMembership.getGroupRowId();
                    if (groupId != null) {
                        membershipGroups.put(groupId, groupMembership);
                    }
                    continue;
                }

                final DataKind kind = dataItem.getDataKind();
                if (kind == null)
                    continue;

                final DetailViewEntry entry = DetailViewEntry.fromValues(mContext, dataItem, mContactData.isDirectoryEntry(),
                        mContactData.getDirectoryId());
                entry.maxLines = kind.maxLinesForDisplay;

                final boolean hasData = !TextUtils.isEmpty(entry.data);
                final boolean isSuperPrimary = dataItem.isSuperPrimary();

                if (dataItem instanceof StructuredNameDataItem) {
                    // Always ignore the name. It is shown in the header if set
                } else if (dataItem instanceof PhoneDataItem && hasData) {
                    PhoneDataItem phone = (PhoneDataItem) dataItem;
                    // Build phone entries
                    entry.data = phone.getFormattedPhoneNumber();
                    // entry.icon = R.drawable.ic_list_call;
                    tmpNumbers.add(entry.data);
                    // add home address of specific number
                    if (entry.mimetype.equals(Phone.CONTENT_ITEM_TYPE) && !FeatureOptionAssistant.isInternationalSupportted()) {
                        if (!TextUtils.isEmpty(entry.data)) {
                            Uri queryUri = NumberServiceHelper.getSingleLocationQueryUriForNumber(entry.data);
                            Cursor cursor = null;
                            try {
                                Log.d(TAG, "buildEntries() : uniform query up. number = " + entry.data);
                                cursor = mContext.getContentResolver().query(queryUri, null, null, null, null);
                                Log.d(TAG, "buildEntries() : uniform query down.cursor = " + cursor);
                                if (cursor != null && cursor.moveToFirst()) {
                                    String province = cursor.getString(NumberServiceHelper.LOCATION_SINGLE_COLUMN_PROVINCE);
                                    String area = cursor.getString(NumberServiceHelper.LOCATION_SINGLE_COLUMN_AREA);
                                    String location = AliTextUtils.makeLocation(province, area);
                                    Log.d(TAG, "buildEntries: location=" + location);
                                    if (!TextUtils.isEmpty(location)) {
                                        entry.typeString = entry.typeString + "  " + location;
                                    }
                                }
                            } catch (SQLiteException sqle) {
                                Log.e(TAG, "buildEntries: failed to query location.", sqle);
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                    }
                    String trimSpaceNumber = NumberNormalizeUtil.trimAllWhiteSpace(entry.data);
                    if (trimSpaceNumber != null && !trimSpaceNumber.isEmpty()) {
                        mPhoneNumber = trimSpaceNumber;
                    }
                    final Intent phoneIntent = mHasPhone ? CallUtil.getCallIntent(mContext,trimSpaceNumber) : null;

                    phoneIntent.putExtra(ONLY_FOR_REPORT_FLAG, REPORT_PHONE);

                    final Intent smsIntent = mHasSms ? new Intent() : null;
                    if (mHasSms) {
                        smsIntent.setClassName(ContactsUtils.MMS_PACKAGE, ContactsUtils.MMS_COMPOSE_ACTIVITY_NAME);
                        smsIntent.setData(Uri.fromParts(Constants.SCHEME_SMSTO, trimSpaceNumber, null));
                        smsIntent.putExtra(ONLY_FOR_REPORT_FLAG, REPORT_SMS);
                    }

                    // Configure Icons and Intents.
                    if (mHasPhone && mHasSms) {
                        entry.intent = phoneIntent;
                        entry.secondaryIntent = smsIntent;
                        entry.secondaryActionIcon = kind.iconAltRes;
                        entry.secondaryActionDescription = kind.iconAltDescriptionRes;

                        // third action is phone icon
                        entry.thirdIntent = phoneIntent;
                        entry.thirdActionIcon = R.drawable.contacts_detail_phone_button;
                    } else if (mHasPhone) {
                        entry.intent = phoneIntent;
                    } else if (mHasSms) {
                        entry.intent = smsIntent;
                    } else {
                        entry.intent = null;
                    }

                    final Intent videoCallIntent;
                    entry.fourthActionIcon = R.drawable.ic_detail_facetime;
                    if (isVideoEnabled() && AliCallLogExtensionHelper.canPlaceVolteVideoCallByNumber(trimSpaceNumber)) {
                        videoCallIntent = mHasPhone ? CallUtil.getVideoCallIntent(mContext, trimSpaceNumber, ContactDetailActivity.CALL_ORIGIN_CALL_DETAIL_ACTIVITY): null;
                        videoCallIntent.putExtra(ONLY_FOR_REPORT_FLAG, REPORT_VIDEO_CALL);
                    } else {
                        videoCallIntent = null;
                    }
                    entry.fourthIntent = videoCallIntent;

                    // Remember super-primary phone
                    if (isSuperPrimary)
                        mPrimaryPhoneUri = entry.uri;

                    entry.isPrimary = isSuperPrimary;

                    // If the entry is a primary entry, then render it first in
                    // the view.
                    if (entry.isPrimary) {
                        // add to beginning of list so that this phone number
                        // shows up first
                        mPhoneEntries.add(0, entry);
                    } else {
                        // add to end of list
                        mPhoneEntries.add(entry);
                    }
                } else if (dataItem instanceof EmailDataItem && hasData) {
                    // Build email entries
                    // entry.icon = R.drawable.ic_list_message;
                    entry.intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(Constants.SCHEME_MAILTO, entry.data, null));
                    entry.isPrimary = isSuperPrimary;
                    // If entry is a primary entry, then render it first in the
                    // view.
                    if (entry.isPrimary) {
                        mEmailEntries.add(0, entry);
                    } else {
                        mEmailEntries.add(entry);
                    }

                    // When Email rows have status, create additional Im row
                    final DataStatus status = mContactData.getStatuses().get(entry.id);
                    if (status != null) {
                        EmailDataItem email = (EmailDataItem) dataItem;
                        ImDataItem im = ImDataItem.createFromEmail(email);

                        final DetailViewEntry imEntry = DetailViewEntry.fromValues(mContext, im,
                                mContactData.isDirectoryEntry(), mContactData.getDirectoryId());
                        buildImActions(mContext, imEntry, im);
                        imEntry.setPresence(status.getPresence());
                        imEntry.maxLines = kind.maxLinesForDisplay;
                        mImEntries.add(imEntry);
                    }
                } else if (dataItem instanceof StructuredPostalDataItem && hasData) {
                    // Build postal entries
                    // entry.icon = R.drawable.ic_list_address;
                    entry.intent = StructuredPostalUtils.getViewPostalAddressIntent(entry.data);
                    mPostalEntries.add(entry);
                } else if (dataItem instanceof ImDataItem && hasData) {
                    // Build IM entries
                    buildImActions(mContext, entry, (ImDataItem) dataItem);

                    // Apply presence when available
                    final DataStatus status = mContactData.getStatuses().get(entry.id);
                    if (status != null) {
                        entry.setPresence(status.getPresence());
                    }
                    mImEntries.add(entry);
                } else if (dataItem instanceof OrganizationDataItem) {
                    entry.uri = null;
                    mOrganizationEntries.add(entry);
                } else if (dataItem instanceof NicknameDataItem && hasData) {
                    // Build nickname entries
                    final boolean isNameRawContact = (mContactData.getNameRawContactId() == rawContactId);

                    final boolean duplicatesTitle = isNameRawContact
                            && mContactData.getDisplayNameSource() == DisplayNameSources.NICKNAME;

                    if (!duplicatesTitle) {
                        entry.uri = null;
                        mNicknameEntries.add(entry);
                    }
                } else if (dataItem instanceof NoteDataItem && hasData) {
                    // Build note entries
                    entry.uri = null;
                    mNoteEntries.add(entry);
                } else if (dataItem instanceof WebsiteDataItem && hasData) {
                    // Build Website entries
                    entry.uri = null;
                    try {
                        WebAddress webAddress = new WebAddress(entry.data);
                        entry.intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress.toString()));
                    } catch (ParseException e) {
                        Log.e(TAG, "Couldn't parse website: " + entry.data);
                    }
                    mWebsiteEntries.add(entry);
                } else if (dataItem instanceof SipAddressDataItem && hasData) {
                    // Build SipAddress entries
                    // entry.icon = R.drawable.ic_list_address;
                    entry.uri = null;
                    if (mHasSip) {
                        entry.intent = CallUtil.getCallIntent(mContext, Uri.fromParts(
                            Constants.SCHEME_SIP, entry.data, null));
                    } else {
                        entry.intent = null;
                    }
                    mSipEntries.add(entry);
                    // TODO: Now that SipAddress is in its own list of entries
                    // (instead of grouped in mOtherEntries), consider
                    // repositioning it right under the phone number.
                    // (Then, we'd also update FallbackAccountType.java to set
                    // secondary=false for this field, and tweak the weight
                    // of its DataKind.)
                } else if (dataItem instanceof EventDataItem && hasData) {
                    entry.data = DateUtils.formatDate(mContext, entry.data);
                    entry.uri = null;
                    mEventEntries.add(entry);
                } else if (dataItem instanceof RelationDataItem && hasData) {
                    entry.intent = new Intent(Intent.ACTION_SEARCH);
                    entry.intent.putExtra(SearchManager.QUERY, entry.data);
                    entry.intent.setType(Contacts.CONTENT_TYPE);
                    mRelationEntries.add(entry);
                } /* added by xiaodong.lxd */else if (dataItem instanceof SnsDataItem && hasData) {
                    SnsDataItem item = (SnsDataItem) dataItem;
                    // currently only sina weibo will link the url except in International version
                    // YUNOS BEGIN
                    // BugID:5860462
                    // Description: remove weibo plugin
                    // author:changjun.bcj
                    // date:2015-03-25
                    if (!FeatureOptionAssistant.isInternationalSupportted()
                            && item.getKindTypeColumn() == Sns.COMMUNITY_MICROBLOG_SINA) {
                        String uid = item.getUID();
                        String nickname = entry.data;
                        entry.secondaryActionIcon = R.drawable.ic_weibo;
                        entry.secondaryActionDescription = R.string.weibo_info_title;
                        String uri = "";
                        if (TextUtils.isEmpty(uid)) {
                            uri = String.format(WeiboFinals.CLIENT_VIEW_PROFILE_URI_1, nickname);
                        } else {
                            uri = String.format(WeiboFinals.CLIENT_VIEW_PROFILE_URI_2, uid, nickname);
                        }
                        entry.intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        entry.intent.putExtra(WeiboFinals.EXTRA_KEY_WEIBO_PROFILE_UID, uid);
                        entry.intent.putExtra(WeiboFinals.EXTRA_KEY_WEIBO_PROFILE_NAME, nickname);
                        entry.intent.putExtra(ONLY_FOR_REPORT_FLAG, REPORT_SINA_WEIBO);
                        entry.secondaryIntent = entry.intent;
                        mSnsEntries.add(entry);
                    } else {
                        entry.uri = null;
                        mSnsEntries.add(entry);
                    }
                    //YUNOS END
                } else {
                    // Handle showing custom rows
                    entry.intent = new Intent(Intent.ACTION_VIEW);
                    entry.intent.setDataAndType(entry.uri, entry.mimetype);
                    entry.intent.putExtra(ONLY_FOR_REPORT_FLAG, REPORT_CUSTOM_DATA);

                    entry.data = dataItem.buildDataString();

                    if (!TextUtils.isEmpty(entry.data)) {
                        // If the account type exists in the hash map, add it as
                        // another entry for
                        // that account type
                        AccountType type = dataItem.getAccountType();
                        if (mOtherEntriesMap.containsKey(type)) {
                            List<DetailViewEntry> listEntries = mOtherEntriesMap.get(type);
                            listEntries.add(entry);
                        } else {
                            // Otherwise create a new list with the entry and
                            // add it to the hash map
                            List<DetailViewEntry> listEntries = new ArrayList<DetailViewEntry>();
                            listEntries.add(entry);
                            mOtherEntriesMap.put(type, listEntries);
                        }
                    }
                }

                if (mContactData != null && mContactData.isUserProfile()) {
                    entry.intent = null;
                    entry.secondaryIntent = null;
                    entry.thirdIntent = null;
                    entry.fourthIntent = null;
                }
            }
        }

        synchronized (mNumberList) {
            mNumberList.clear();
            mNumberList.addAll(tmpNumbers);
        }

        handleMembershipGroups(membershipGroups);

        String ringtoneUriString = mContactData.getCustomRingtone();
        if (!TextUtils.isEmpty(ringtoneUriString)) {
            DetailViewEntry entry = new DetailViewEntry();
            entry.kind = mContext.getString(R.string.contact_edit_ringtone_type);

            final String ringtoneName = RingtoneUtils.getRingtoneTitle(mContext, Uri.parse(ringtoneUriString));
            entry.data = ringtoneName;
            mRingtoneEntries.add(entry);
        }
    }

    private void handleMembershipGroups(LongSparseArray<GroupMembershipDataItem> membershipGroups) {
        List<GroupMetaData> groupMetaData = mContactData.getGroupMetaData();
        if ((membershipGroups.size() == 0) || (groupMetaData == null) || groupMetaData.isEmpty()) {
            return;
        }
        StringBuilder titles = new StringBuilder();
        int metadataIdx = 0, membershipIdx = 0;
        int metadataCount = groupMetaData.size(), membershipCount = membershipGroups.size();
        long metadataGroupId, membershipGroupId;
        while ((metadataIdx < metadataCount) && (membershipIdx < membershipCount)) {
            metadataGroupId = groupMetaData.get(metadataIdx).getGroupId();
            membershipGroupId = membershipGroups.keyAt(membershipIdx);

            if (metadataGroupId < membershipGroupId) {
                metadataIdx++;
            } else if (metadataGroupId > membershipGroupId) {
                membershipIdx++;
            } else {
                String title = groupMetaData.get(metadataIdx).getTitle();
                if (!TextUtils.isEmpty(title)) {
                    if (titles.length() > 0) {
                        titles.append(", ");
                    }
                    titles.append(title);
                    if (titles.length() > GroupMembershipView.GROURP_MEMBERSHIP_TEXT_MAX_DISPLAY_LENGTH) {
                        break;
                    }
                }
                metadataIdx++;
                membershipIdx++;
            }
        }
        if (titles.length() == 0) {
            return;
        }
        DetailViewEntry entry = new DetailViewEntry();
        entry.mimetype = GroupMembership.MIMETYPE;
        entry.kind = mContext.getString(R.string.groupsLabel);
        entry.data = titles.toString();
        mGroupEntries.add(entry);
    }

    /**
     * Collapse all contact detail entries into one aggregated list with a
     * {@link HeaderViewEntry} at the top.
     */
    private void setupFlattenedList() {
        // All contacts should have a header view (even if there is no data for
        // the contact).
        mAllEntries.add(new HeaderViewEntry());

        // addPhoneticName();

        flattenList(mPhoneEntries);
        flattenList(mSmsEntries);
        flattenList(mEmailEntries);

        addOrganizationEntries();

        flattenList(mImEntries);
        flattenList(mNicknameEntries);
        flattenList(mWebsiteEntries);

        addNetworks();

        flattenList(mSipEntries);
        flattenList(mPostalEntries);
        flattenList(mEventEntries);
        flattenList(mGroupEntries);
        flattenList(mRelationEntries);
        flattenList(mSnsEntries);
        // flattenList(mNoteEntries);
        flattenList(mRingtoneEntries);

        // add operation buttons item
        // if (!FeatureOptionAssistant.isInternationalSupportted()) {
        // // International Version does not support alipay
        // addAlipayPluginEntry();
        // }
    }

    /**
     * Add phonetic name (if applicable) to the aggregated list of contact
     * details. This has to be done manually because phonetic name doesn't have
     * a mimetype or action intent.
     */
    // private void addPhoneticName() {
    // String phoneticName = ContactDetailDisplayUtils.getPhoneticName(mContext,
    // mContactData);
    // if (TextUtils.isEmpty(phoneticName)) {
    // return;
    // }
    //
    // // Add a title
    // String phoneticNameKindTitle =
    // mContext.getString(R.string.name_phonetic);
    // // mAllEntries.add(new
    // // KindTitleViewEntry(phoneticNameKindTitle.toUpperCase()));
    //
    // // Add the phonetic name
    // final DetailViewEntry entry = new DetailViewEntry();
    // entry.kind = phoneticNameKindTitle;
    // entry.data = phoneticName;
    // mAllEntries.add(entry);
    // }

    /*
     * private void addAlipayPluginEntry() { if (mAlipayPluginView == null) {
     * Log.e(TAG, "addAlipayPluginEntry() mAlipayPluginView is null!!!");
     * return; } final AlipayPluginViewEntry entry = new
     * AlipayPluginViewEntry(); mAllEntries.add(entry); }
     */

    private void addOrganizationEntries() {
        String company = ContactDetailDisplayUtils.getCompany(mContext, mContactData);
        if (TextUtils.isEmpty(company)) {
            return;
        }

        // Add a title
        String organizationKindTitle = mContext.getString(R.string.organizationLabelsGroup);
        // mAllEntries.add(new
        // KindTitleViewEntry(organizationKindTitle.toUpperCase()));

        // Add the phonetic name
        final DetailViewEntry entry = new DetailViewEntry();
        entry.kind = organizationKindTitle;
        entry.data = company;
        mAllEntries.add(entry);
    }

    /**
     * Add attribution and other third-party entries (if applicable) under the
     * "networks" section of the aggregated list of contact details. This has to
     * be done manually because the attribution does not have a mimetype and the
     * third-party entries don't have actually belong to the same
     * {@link DataKind}.
     */
    private void addNetworks() {
        String attribution = ContactDetailDisplayUtils.getAttribution(mContext, mContactData);
        boolean hasAttribution = !TextUtils.isEmpty(attribution);
        int networksCount = mOtherEntriesMap.keySet().size();

        // Note: invitableCount will always be 0 for me profile. (ContactLoader
        // won't set
        // invitable types for me profile.)
        int invitableCount = mContactData.getInvitableAccountTypes().size();
        if (!hasAttribution && networksCount == 0 && invitableCount == 0) {
            return;
        }

        // Add a title
        String networkKindTitle = mContext.getString(R.string.connections);
        // mAllEntries.add(new
        // KindTitleViewEntry(networkKindTitle.toUpperCase()));

        // Add the attribution if applicable
        if (hasAttribution) {
            final DetailViewEntry entry = new DetailViewEntry();
            entry.kind = networkKindTitle;
            entry.data = attribution;
            mAllEntries.add(entry);

            // Add a divider below the attribution if there are network details
            // that will follow
            if (networksCount > 0) {
                mAllEntries.add(new SeparatorViewEntry());
            }
        }

        // Add the other entries from third parties
        for (AccountType accountType : mOtherEntriesMap.keySet()) {

            // Add a title for each third party app
            // mAllEntries.add(new NetworkTitleViewEntry(mContext,
            // accountType));
            final CharSequence type = (accountType == null) ? null : accountType.getDisplayLabel(mContext);
            String typeString = null;
            if (type != null) {
                typeString = type.toString();
            }
            for (DetailViewEntry detailEntry : mOtherEntriesMap.get(accountType)) {
                // Add indented separator
                // SeparatorViewEntry separatorEntry = new SeparatorViewEntry();
                // separatorEntry.setIsInSubSection(true);
                // mAllEntries.add(separatorEntry);

                // Add indented detail
                detailEntry.typeString = typeString;
                detailEntry.setIsInSubSection(true);
                mAllEntries.add(detailEntry);
            }
        }

        mOtherEntriesMap.clear();

        // Add the "More networks" button, which opens the invitable account
        // type list popup.
        if (invitableCount > 0) {
            addMoreNetworks();
        }
    }

    /**
     * Add the "More networks" entry. When clicked, show a popup containing a
     * list of invitable account types.
     */
    private void addMoreNetworks() {
        // First, prepare for the popup.

        // Adapter for the list popup.
        final InvitableAccountTypesAdapter popupAdapter = new InvitableAccountTypesAdapter(mContext, mContactData);

        // Listener called when a popup item is clicked.
        final AdapterView.OnItemClickListener popupItemListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null && mContactData != null) {
                    mListener.onItemClicked(ContactsUtils.getInvitableIntent(popupAdapter.getItem(position) /*
                                                                                                             * account
                                                                                                             * type
                                                                                                             */,
                            mContactData.getLookupUri()));
                }
            }
        };

        // Then create the click listener for the "More network" entry. Open the
        // popup.
        View.OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                showListPopup(v, popupAdapter, popupItemListener);
            }
        };

        // Finally create the entry.
        mAllEntries.add(new AddConnectionViewEntry(mContext, onClickListener));
    }

    /**
     * Iterate through {@link DetailViewEntry} in the given list and add it to a
     * list of all entries. Add a {@link KindTitleViewEntry} at the start if the
     * length of the list is not 0. Add {@link SeparatorViewEntry}s as dividers
     * as appropriate. Clear the original list.
     */
    private void flattenList(ArrayList<DetailViewEntry> entries) {
        int count = entries.size();

        // Add all the data entries for this kind
        for (int i = 0; i < count; i++) {
            mAllEntries.add(entries.get(i));
        }

        // Clear old list because it's not needed anymore.
        entries.clear();
    }

    /**
     * Writes the Instant Messaging action into the given entry value.
     */
    @VisibleForTesting
    public static void buildImActions(Context context, DetailViewEntry entry, ImDataItem im) {
        final boolean isEmail = im.isCreatedFromEmail();

        if (!isEmail && !im.isProtocolValid()) {
            return;
        }

        final String data = im.getData();
        if (TextUtils.isEmpty(data)) {
            return;
        }

        final int protocol = isEmail ? Im.PROTOCOL_GOOGLE_TALK : im.getProtocol();

        if (protocol == Im.PROTOCOL_GOOGLE_TALK) {
            final int chatCapability = im.getChatCapability();
            entry.chatCapability = chatCapability;
            entry.typeString = Im.getProtocolLabel(context.getResources(), Im.PROTOCOL_GOOGLE_TALK, null).toString();
            if ((chatCapability & Im.CAPABILITY_HAS_CAMERA) != 0) {
                entry.intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?message"));
                entry.secondaryIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?call"));
            } else if ((chatCapability & Im.CAPABILITY_HAS_VOICE) != 0) {
                // Allow Talking and Texting
                entry.intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?message"));
                entry.secondaryIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?call"));
            } else {
                entry.intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?message"));
            }
        } else {
            // Build an IM Intent
            String host = im.getCustomProtocol();

            if (protocol != Im.PROTOCOL_CUSTOM) {
                // Try bringing in a well-known host for specific protocols
                host = ContactsUtils.lookupProviderNameFromId(protocol);
            }

            if (!TextUtils.isEmpty(host)) {
                final String authority = host.toLowerCase(Locale.US);
                final Uri imUri = new Uri.Builder().scheme(Constants.SCHEME_IMTO).authority(authority).appendPath(data).build();
                entry.intent = new Intent(Intent.ACTION_SENDTO, imUri);
            }
        }
    }

    /**
     * Show a list popup. Used for "popup-able" entry, such as "More networks".
     */
    private void showListPopup(View anchorView, ListAdapter adapter, final AdapterView.OnItemClickListener onItemClickListener) {
        dismissPopupIfShown();
        mPopup = new ListPopupWindow(mContext, null);
        mPopup.setAnchorView(anchorView);
        mPopup.setWidth(anchorView.getWidth());
        mPopup.setAdapter(adapter);
        mPopup.setModal(true);

        // We need to wrap the passed onItemClickListener here, so that we can
        // dismiss() the
        // popup afterwards. Otherwise we could directly use the passed
        // listener.
        mPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onItemClickListener.onItemClick(parent, view, position, id);
                dismissPopupIfShown();
            }
        });
        mPopup.show();
    }

    private void dismissPopupIfShown() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
        mPopup = null;
    }

    /**
     * Base class for an item in the {@link ViewAdapter} list of data, which is
     * supplied to the {@link ListView}.
     */
    static class ViewEntry {
        private final int viewTypeForAdapter;
        protected long id = -1;
        /** Whether or not the entry can be focused on or not. */
        protected boolean isEnabled = false;

        ViewEntry(int viewType) {
            viewTypeForAdapter = viewType;
        }

        int getViewType() {
            return viewTypeForAdapter;
        }

        long getId() {
            return id;
        }

        boolean isEnabled() {
            return isEnabled;
        }

        /**
         * Called when the entry is clicked. Only {@link #isEnabled} entries can
         * get clicked.
         *
         * @param clickedView {@link View} that was clicked (Used, for example,
         *            as the anchor view for a popup.)
         * @param fragmentListener {@link Listener} set to
         *            {@link ContactDetailFragment}
         */
        public void click(View clickedView, Listener fragmentListener) {
        }
    }

    /**
     * Header item in the {@link ViewAdapter} list of data.
     */
    private static class HeaderViewEntry extends ViewEntry {

        HeaderViewEntry() {
            super(ViewAdapter.VIEW_TYPE_HEADER_ENTRY);
        }

    }

    /**
     * Separator between items of the same {@link DataKind} in the
     * {@link ViewAdapter} list of data.
     */
    private static class SeparatorViewEntry extends ViewEntry {

        /**
         * Whether or not the entry is in a subsection (if true then the
         * contents will be indented to the right)
         */
        // private boolean mIsInSubSection = false;

        SeparatorViewEntry() {
            super(ViewAdapter.VIEW_TYPE_SEPARATOR_ENTRY);
        }

        // public void setIsInSubSection(boolean isInSubSection) {
        // // mIsInSubSection = isInSubSection;
        // }

        /*
         * public boolean isInSubSection() { return mIsInSubSection; }
         */
    }

    /**
     * Title entry for items of the same {@link DataKind} in the
     * {@link ViewAdapter} list of data.
     */
    private static class KindTitleViewEntry extends ViewEntry {

        private final String mTitle;

        KindTitleViewEntry(String titleText) {
            super(ViewAdapter.VIEW_TYPE_KIND_TITLE_ENTRY);
            mTitle = titleText;
        }

        public String getTitle() {
            return mTitle;
        }
    }

    /**
     * Operation Buttons at the end of {@link ViewAdapter} list of data.
     */
    /*
     * private static class AlipayPluginViewEntry extends ViewEntry {
     * AlipayPluginViewEntry() {
     * super(ViewAdapter.VIEW_TYPE_APLIPAY_PLUGIN_ENTRY); } }
     */

    /**
     * A title for a section of contact details from a single 3rd party network.
     */
    private static class NetworkTitleViewEntry extends ViewEntry {
        // private final Drawable mIcon;
        private final CharSequence mLabel;

        public NetworkTitleViewEntry(Context context, AccountType type) {
            super(ViewAdapter.VIEW_TYPE_NETWORK_TITLE_ENTRY);
            // this.mIcon = type.getDisplayIcon(context);
            this.mLabel = type.getDisplayLabel(context);
            this.isEnabled = false;
        }

        // public Drawable getIcon() {
        // return mIcon;
        // }

        public CharSequence getLabel() {
            return mLabel;
        }
    }

    /**
     * This is used for the "Add Connections" entry.
     */
    private static class AddConnectionViewEntry extends ViewEntry {
        // private final Drawable mIcon;
        private final CharSequence mLabel;
        private final View.OnClickListener mOnClickListener;

        private AddConnectionViewEntry(Context context, View.OnClickListener onClickListener) {
            super(ViewAdapter.VIEW_TYPE_ADD_CONNECTION_ENTRY);
            // this.mIcon = context.getResources().getDrawable(
            // R.drawable.ic_menu_add_field_holo_light);
            this.mLabel = context.getString(R.string.add_connection_button);
            this.mOnClickListener = onClickListener;
            this.isEnabled = true;
        }

        @Override
        public void click(View clickedView, Listener fragmentListener) {
            if (mOnClickListener == null)
                return;
            mOnClickListener.onClick(clickedView);
        }

        /*
         * public Drawable getIcon() { return mIcon; }
         */

        public CharSequence getLabel() {
            return mLabel;
        }
    }

    /**
     * An item with a single detail for a contact in the {@link ViewAdapter}
     * list of data.
     */
    static class DetailViewEntry extends ViewEntry implements Collapsible<DetailViewEntry> {
        // TODO: Make getters/setters for these fields
        public int type = -1;
        public String kind;
        public String typeString;
        public String data;
        public Uri uri;
        public int maxLines = 1;
        public String mimetype;
        // public int icon = -1;

        public Context context = null;
        public boolean isPrimary = false;

        public Intent intent;
        public int secondaryActionIcon = -1;
        public int secondaryActionDescription = -1;
        public Intent secondaryIntent = null;

        public int thirdActionIcon = -1;
        public int thirdActionDescription = -1;
        public Intent thirdIntent = null;

        public int fourthActionIcon = -1;
        public int fourthActionDescription = -1;
        public Intent fourthIntent = null;

        public ArrayList<Long> ids = new ArrayList<Long>();
        public int collapseCount = 0;

        public int presence = -1;
        public int chatCapability = 0;

        private boolean mIsInSubSection = false;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(512);
            sb.append("== DetailViewEntry ==\n  type: ").append(type);
            sb.append("\n  kind: ").append(kind);
            sb.append("\n  typeString: ").append(typeString);
            sb.append("\n  data: ").append(data);
            // sb.append("\n  icon: ").append(icon);
            if (uri == null) {
                sb.append("\n  uri: null ");
            } else {
                sb.append("\n  uri: ").append(uri.toString());
            }
            sb.append("\n  maxLines: ").append(maxLines);
            sb.append("\n  mimetype: ").append(mimetype);
            sb.append("\n  isPrimary: ").append(isPrimary ? "true" : "false");
            sb.append("\n  secondaryActionIcon: ").append(secondaryActionIcon);
            sb.append("\n  secondaryActionDescription: ").append(secondaryActionDescription);
            if (intent == null) {
                sb.append("\n  intent: (null) ");
            } else {
                sb.append("\n  intent: ").append(intent.toString());
            }
            if (secondaryIntent == null) {
                sb.append("\n  secondaryIntent: (null)");
            } else {
                sb.append("\n  secondaryIntent: ").append(secondaryIntent.toString());
            }
            sb.append("\n  ids: ").append(Iterables.toString(ids));
            sb.append("\n  collapseCount: ").append(collapseCount);
            sb.append("\n  presence: ").append(presence);
            sb.append("\n  chatCapability: ").append(chatCapability);
            sb.append("\n  mIsInSubsection: ").append(mIsInSubSection ? "true" : "false");
            return sb.toString();
        }

        DetailViewEntry() {
            super(ViewAdapter.VIEW_TYPE_DETAIL_ENTRY);
            isEnabled = true;
        }

        /**
         * Build new {@link DetailViewEntry} and populate from the given values.
         */
        public static DetailViewEntry fromValues(Context context, DataItem item, boolean isDirectoryEntry, long directoryId) {
            final DetailViewEntry entry = new DetailViewEntry();
            entry.id = item.getId();
            entry.context = context;
            entry.uri = ContentUris.withAppendedId(Data.CONTENT_URI, entry.id);
            if (isDirectoryEntry) {
                entry.uri = entry.uri.buildUpon()
                        .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(directoryId)).build();
            }
            entry.mimetype = item.getMimeType();
            entry.kind = item.getKindString();
            entry.data = item.buildDataString();

            if (item.hasKindTypeColumn()) {
                entry.type = item.getKindTypeColumn();

                // get type string
                entry.typeString = "";
                for (EditType type : item.getDataKind().typeList) {
                    if (type.rawValue == entry.type) {
                        if (type.customColumn == null) {
                            // Non-custom type. Get its description from the
                            // resource
                            entry.typeString = context.getString(type.labelRes);
                        } else {
                            // Custom type. Read it from the database
                            entry.typeString = item.getContentValues().getAsString(type.customColumn);
                        }
                        break;
                    }
                }
            } else {
                entry.typeString = "";
            }

            return entry;
        }

        public void setPresence(int presence) {
            this.presence = presence;
        }

        public void setIsInSubSection(boolean isInSubSection) {
            mIsInSubSection = isInSubSection;
        }

        public boolean isInSubSection() {
            return mIsInSubSection;
        }

        @Override
        public boolean collapseWith(DetailViewEntry entry) {
            // assert equal collapse keys
            if (!shouldCollapseWith(entry)) {
                return false;
            }

            // Choose the label associated with the highest type precedence.
            if (TypePrecedence.getTypePrecedence(mimetype, type) > TypePrecedence.getTypePrecedence(entry.mimetype, entry.type)) {
                type = entry.type;
                kind = entry.kind;
                typeString = entry.typeString;
            }

            // Choose the max of the maxLines and maxLabelLines values.
            maxLines = Math.max(maxLines, entry.maxLines);

            // Choose the presence with the highest precedence.
            if (StatusUpdates.getPresencePrecedence(presence) < StatusUpdates.getPresencePrecedence(entry.presence)) {
                presence = entry.presence;
            }

            // If any of the collapsed entries are primary make the whole thing
            // primary.
            isPrimary = entry.isPrimary ? true : isPrimary;

            // uri, and contactdId, shouldn't make a difference. Just keep the
            // original.

            // Keep track of all the ids that have been collapsed with this one.
            ids.add(entry.getId());
            collapseCount++;
            return true;
        }

        @Override
        public boolean shouldCollapseWith(DetailViewEntry entry) {
            if (entry == null) {
                return false;
            }

            if (!ContactsUtils.shouldCollapse(mimetype, data, entry.mimetype, entry.data)) {
                return false;
            }

            if (!TextUtils.equals(mimetype, entry.mimetype) || type != entry.type
                    || !ContactsUtils.areIntentActionEqual(intent, entry.intent)
                    || !ContactsUtils.areIntentActionEqual(secondaryIntent, entry.secondaryIntent)) {
                return false;
            }

            return true;
        }

        @Override
        public void click(View clickedView, Listener fragmentListener) {
            if (fragmentListener == null || intent == null)
                return;
            fragmentListener.onItemClicked(intent);
            sendReport(clickedView.getContext(), intent);
        }
    }

    static class CalllogTitleViewEntry extends ViewEntry {
        public CalllogTitleViewEntry() {
            super(ViewAdapter.VIEW_TYPE_CALLLOG_TITLE_ENTRY);
        }
    }

    static class ClearCalllogItemEntry extends ViewEntry {
        public ClearCalllogItemEntry() {
            super(ViewAdapter.VIEW_TYPE_CLEAR_CALLLOG_ITEM_ENTRY);
        }
    }

    static class CalllogItemViewEntry extends ViewEntry {
        private int mCalllogIndex;
        private int mCalllogTotal;
        public final long mCalllogId;
        public final long mDate;
        public final long mDuration;
        public final String mNumber;
        public final int mCallType;
        public final int mCallFeatures;
        public final String mCountryIso;
        public final int mSubId;
        public final Uri mCallUri;

        public final String mLocation;
        public final long mRingTime;
        public final String mPhoneRecordPath;

        public CalllogItemViewEntry(final int index, final int total, long id, long date, long duration, String number,
                int type, int callFeatures, String countryIso, int subId,
                String province, String area, long ringTime, String phoneRecordPath) {
            super(ViewAdapter.VIEW_TYPE_CALLLOG_ITEM_ENTRY);
            mCalllogIndex = index;
            mCalllogTotal = total;
            mCalllogId = id;
            mDate = date;
            mDuration = duration;
            mNumber = number;
            mCallType = type;
            mCallFeatures = callFeatures;
            mCountryIso = countryIso;
            mSubId = subId;
            mLocation = AliTextUtils.makeLocation(province, area);
            mRingTime = ringTime;
            mPhoneRecordPath = phoneRecordPath;
            mCallUri = ContentUris.withAppendedId(Calls.CONTENT_URI, mCalllogId);
        }

        public int getCalllogIndex() {
            return mCalllogIndex;
        }

        public int getCalllogTotal() {
            return mCalllogTotal;
        }

        public long getCalllogId() {
            return mCalllogId;
        }
    }

    /**
     * Cache of the children views for a view that displays a header view entry.
     */
    private static class HeaderViewCache {
        // public final TextView displayNameView;
        public final ImageView photoView;
        public final TextView textPortrait;
        public final ImageView portraitBorder;
        // public final ImageView starredView;
        public final int layoutResourceId;
        // public final ImageView favorateView;
        public TextView mNameView;
        public TextView mCommentsView;

        public HeaderViewCache(View view, int layoutResourceInflated) {
            // displayNameView = (TextView) view.findViewById(R.id.name);
            photoView = (ImageView) view.findViewById(R.id.photo);
            // starredView = (ImageView) view.findViewById(R.id.star);
            layoutResourceId = layoutResourceInflated;
            // favorateView = (ImageView) view.findViewById(R.id.favorate);
            // TODO
            // barcodePluginView = view.findViewById(R.id.barcode);
            mNameView = (TextView) view.findViewById(R.id.contact_name_id);
            mCommentsView = (TextView) view.findViewById(R.id.contact_comments);
            textPortrait = (TextView) view.findViewById(R.id.text_photo);
            portraitBorder = (ImageView) view.findViewById(R.id.portrait_border);
        }
    }

    private static class KindTitleViewCache {
        public final TextView titleView;
        public final LinearLayout iconView;

        public KindTitleViewCache(View view) {
            titleView = (TextView) view.findViewById(R.id.title);
            iconView = (LinearLayout) view.findViewById(R.id.title_icon);
        }
    }

    /**
     * Cache of the children views for a view that displays a
     * {@link NetworkTitleViewEntry}
     */
    private static class NetworkTitleViewCache {
        public final TextView name;

        // public final ImageView icon;

        public NetworkTitleViewCache(View view) {
            name = (TextView) view.findViewById(R.id.network_title);
            // icon = (ImageView) view.findViewById(R.id.network_icon);
        }
    }

    /**
     * Cache of the children views for a view that displays a
     * {@link AddConnectionViewEntry}
     */
    private static class AddConnectionViewCache {
        public final TextView name;
        // public final ImageView icon;
        public final View primaryActionView;

        public AddConnectionViewCache(View view) {
            name = (TextView) view.findViewById(R.id.add_connection_label);
            // icon = (ImageView) view.findViewById(R.id.add_connection_icon);
            primaryActionView = view.findViewById(R.id.primary_action_view);
        }
    }

    /**
     * Cache of the children views of a contact detail entry represented by a
     * {@link DetailViewEntry}
     */
    private static class DetailViewCache {
        public final TextView type;
        public final TextView data;
        public final ImageView presenceIcon;
        public final ImageView secondaryActionButton;
        public final ImageView thirdActionButton;
        public final ImageView fourthActionButton;
        public final View actionsViewContainer;
        public final View primaryIndicator;

        public DetailViewCache(View view,
                OnClickListener primaryActionClickListener,
                OnClickListener secondaryActionClickListener,
                OnClickListener thirdActionClickListener,
                OnClickListener fourthActionClickListener) {

            type = (TextView) view.findViewById(R.id.type);
            data = (TextView) view.findViewById(R.id.data);
            primaryIndicator = view.findViewById(R.id.primary_indicator);
            presenceIcon = (ImageView) view.findViewById(R.id.presence_icon);

            actionsViewContainer = view.findViewById(R.id.actions_view_container);
            actionsViewContainer.setOnClickListener(primaryActionClickListener);
            // primaryActionView = view.findViewById(R.id.primary_action_view);

            secondaryActionButton = (ImageView) view.findViewById(R.id.secondary_action_button);
            secondaryActionButton.setOnClickListener(secondaryActionClickListener);

            thirdActionButton = (ImageView) view.findViewById(R.id.third_action_button);
            thirdActionButton.setOnClickListener(thirdActionClickListener);

            fourthActionButton = (ImageView) view.findViewById(R.id.fourth_action_button);
            fourthActionButton.setOnClickListener(fourthActionClickListener);

        }
    }

    private final class ViewAdapter extends BaseAdapter {

        public static final int VIEW_TYPE_DETAIL_ENTRY = 0;
        public static final int VIEW_TYPE_HEADER_ENTRY = 1;
        public static final int VIEW_TYPE_KIND_TITLE_ENTRY = 2;
        public static final int VIEW_TYPE_NETWORK_TITLE_ENTRY = 3;
        public static final int VIEW_TYPE_ADD_CONNECTION_ENTRY = 4;
        public static final int VIEW_TYPE_SEPARATOR_ENTRY = 5;
        public static final int VIEW_TYPE_CALLLOG_TITLE_ENTRY = 6;
        public static final int VIEW_TYPE_CALLLOG_ITEM_ENTRY = 7;
        // public static final int VIEW_TYPE_APLIPAY_PLUGIN_ENTRY = 8;
        public static final int VIEW_TYPE_CLEAR_CALLLOG_ITEM_ENTRY = 9;
        private static final int VIEW_TYPE_COUNT = 10;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            if (DEBUG)
                Log.d(PERFORMANCE_TAG, "getView on detail fragment position:" + position + ", viewType:" + viewType);
            switch (viewType) {
                case VIEW_TYPE_HEADER_ENTRY:
                    return getHeaderEntryView(convertView, parent);
                case VIEW_TYPE_SEPARATOR_ENTRY:
                    return getSeparatorEntryView(position, convertView, parent);
                case VIEW_TYPE_KIND_TITLE_ENTRY:
                    return getKindTitleEntryView(position, convertView, parent);
                case VIEW_TYPE_DETAIL_ENTRY:
                    return getDetailEntryView(position, convertView, parent);
                case VIEW_TYPE_NETWORK_TITLE_ENTRY:
                    return getNetworkTitleEntryView(position, convertView, parent);
                case VIEW_TYPE_ADD_CONNECTION_ENTRY:
                    return getAddConnectionEntryView(position, convertView, parent);
                /*case VIEW_TYPE_APLIPAY_PLUGIN_ENTRY:
                    return getAlipayPluginEntryView(position, convertView, parent);*/
                case VIEW_TYPE_CALLLOG_TITLE_ENTRY:
                    return getCalllogTitleEntryView();
                case VIEW_TYPE_CALLLOG_ITEM_ENTRY:
                    return getCalllogItemEntryView(position, convertView, parent);
                case VIEW_TYPE_CLEAR_CALLLOG_ITEM_ENTRY:
                    return getClearCalllogItemView(position, convertView, parent);
                default:
                    throw new IllegalStateException("Invalid view type ID " + getItemViewType(position));
            }
        }

        private View getHeaderEntryView(View convertView, ViewGroup parent) {
            final int desiredLayoutResourceId = mContactHasSocialUpdates ? R.layout.detail_header_contact_with_updates
                    : R.layout.photo_selector_view;
            View result = null;
            HeaderViewCache viewCache = null;

            // Only use convertView if it has the same layout resource ID as the
            // one desired
            // (the two can be different on wide 2-pane screens where the detail
            // fragment is reused
            // for many different contacts that do and do not have social
            // updates).
            if (convertView != null) {
                viewCache = (HeaderViewCache) convertView.getTag();
                if (viewCache.layoutResourceId == desiredLayoutResourceId) {
                    result = convertView;
                }
            }

            // Otherwise inflate a new header view and create a new view cache.
            if (result == null) {
                result = mInflater.inflate(desiredLayoutResourceId, parent, false);
                viewCache = new HeaderViewCache(result, desiredLayoutResourceId);
                result.setTag(viewCache);
            }

            // ContactDetailDisplayUtils.setDisplayName(mContext, mContactData,
            // viewCache.displayNameView);

            // Set the photo if it should be displayed
            int color = R.color.portrait_default;
            if (viewCache.photoView != null) {
                mPhotoSetter.setupContactPhoto(mContactData, viewCache.photoView);
                viewCache.photoView.setVisibility(View.VISIBLE);
                viewCache.textPortrait.setVisibility(View.GONE);
                viewCache.portraitBorder.setImageResource(R.drawable.contact_detail_avatar_border);
                if (mContactData.isWritableContact(mContext) && (!mContactData.isSimAccountType())) {
                    viewCache.photoView.setOnClickListener(mPhotoViewClickListener);
                }
            }

            if (viewCache.mNameView != null && mContactData != null) {
                viewCache.mNameView.setText(mContactData.getDisplayName());
                viewCache.mNameView.setSelected(true);
                if (mContactData.getPhotoUri() == null) {
                    String nameStr = ContactsTextUtils.getPortraitText(mContactData.getDisplayName());
                    if (!ContactsTextUtils.STRING_EMPTY.equals(nameStr)) {
                        color = ContactsTextUtils.getColor(nameStr);
                        viewCache.photoView.setVisibility(View.GONE);
                        viewCache.textPortrait.setVisibility(View.VISIBLE);
                        viewCache.portraitBorder.setImageResource(R.drawable.contact_detail_avatar_border2);
                        viewCache.textPortrait.setText(nameStr);
                        if (mContactData.isWritableContact(mContext) && (!mContactData.isSimAccountType())) {
                            viewCache.textPortrait.setOnClickListener(mPhotoViewClickListener);
                        }
                    }
                }
            }

            // Contact comments (notes).
            if (viewCache.mCommentsView != null) {
                if (mNoteEntries.size() > 0) {
                    viewCache.mCommentsView.setText(mNoteEntries.get(0).data);
                    viewCache.mCommentsView.setVisibility(View.VISIBLE);
                } else {
                    viewCache.mCommentsView.setText("");
                    viewCache.mCommentsView.setVisibility(View.GONE);
                }
            }

            result.setBackgroundResource(color);
            if (getActivity() != null && getActivity() instanceof ContactDetailActivity) {
                ((ContactDetailActivity) getActivity()).updateTitleColor(color);
            }

            return result;
        }

        private OnClickListener mPhotoViewClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                createPhotoHandler(v);
                if (mCurrentPhotoHandler != null) {
                    mCurrentPhotoHandler.onClick(v);
                }
            }
        };

        private View getSeparatorEntryView(int position, View convertView, ViewGroup parent) {

            return (convertView != null) ? convertView : mInflater.inflate(R.layout.contact_detail_separator_entry_view,
                    parent, false);
        }

        private View getKindTitleEntryView(int position, View convertView, ViewGroup parent) {
            final KindTitleViewEntry entry = (KindTitleViewEntry) getItem(position);
            final View result;
            final KindTitleViewCache viewCache;

            if (convertView != null) {
                result = convertView;
                viewCache = (KindTitleViewCache) result.getTag();
            } else {
                result = mInflater.inflate(R.layout.list_separator, parent, false);
                viewCache = new KindTitleViewCache(result);
                result.setTag(viewCache);
            }

            viewCache.titleView.setText(entry.getTitle());

            if (entry.getTitle() != null) {
                if (entry.getTitle().equals(mContext.getString(R.string.phoneLabelsGroup))) { // 电话
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_call);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.emailLabelsGroup))) {
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_message);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.imLabelsGroup))) {
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_chat);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.postalLabelsGroup))) {
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_address);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.label_notes))) {
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_remark);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.label_sip_address))) { //
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_work);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.websiteLabelsGroup))) { // 网站
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_web);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.organizationLabelsGroup))) {
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_work);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.eventTypeBirthday))) {
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_birthday);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.snsLabelsGroup))) {
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_web);
                    viewCache.titleView.setVisibility(View.GONE);
                } else if (entry.getTitle().equals(mContext.getString(R.string.groupsLabel))) {
                    viewCache.iconView.setBackgroundResource(R.drawable.ic_list_group);
                    viewCache.titleView.setVisibility(View.GONE);
                } /*
                   * else { // viewCache.iconView.setLayoutParams(new //
                   * LinearLayout.LayoutParams(0,0)); }
                   */
            }
            return result;
        }

        private View getNetworkTitleEntryView(int position, View convertView, ViewGroup parent) {
            final NetworkTitleViewEntry entry = (NetworkTitleViewEntry) getItem(position);
            final View result;
            final NetworkTitleViewCache viewCache;

            if (convertView != null) {
                result = convertView;
                viewCache = (NetworkTitleViewCache) result.getTag();
            } else {
                result = mInflater.inflate(R.layout.contact_detail_network_title_entry_view, parent, false);
                viewCache = new NetworkTitleViewCache(result);
                result.setTag(viewCache);
            }

            viewCache.name.setText(entry.getLabel());
            // viewCache.icon.setImageDrawable(entry.getIcon());

            return result;
        }

        private View getAddConnectionEntryView(int position, View convertView, ViewGroup parent) {
            final AddConnectionViewEntry entry = (AddConnectionViewEntry) getItem(position);
            final View result;
            final AddConnectionViewCache viewCache;

            if (convertView != null) {
                result = convertView;
                viewCache = (AddConnectionViewCache) result.getTag();
            } else {
                result = mInflater.inflate(R.layout.contact_detail_add_connection_entry_view, parent, false);
                viewCache = new AddConnectionViewCache(result);
                result.setTag(viewCache);
            }
            viewCache.name.setText(entry.getLabel());
            // viewCache.icon.setImageDrawable(entry.getIcon());
            viewCache.primaryActionView.setOnClickListener(entry.mOnClickListener);

            return result;
        }

        /*private View getAlipayPluginEntryView(int position, View convertView, ViewGroup parent) {
            / *
             * final View result = (convertView != null) ? convertView :
             * mInflater.inflate(R.layout.detail_alipay_plugin_view, parent,
             * false); return result;
             * /
            return mAlipayPluginView;
        }*/

        private View getDetailEntryView(int position, View convertView, ViewGroup parent) {
            final DetailViewEntry entry = (DetailViewEntry) getItem(position);
            final View v;
            final DetailViewCache viewCache;

            // Check to see if we can reuse convertView
            if (convertView != null) {
                v = convertView;
                viewCache = (DetailViewCache) v.getTag();
            } else {
                // Create a new view if needed
                v = mInflater.inflate(R.layout.contact_detail_list_item, parent, false);

                // Cache the children
                viewCache = new DetailViewCache(v, mPrimaryActionClickListener, mSecondaryActionClickListener,
                        mThirdActionClickListener, mFourthActionClickListener);
                v.setTag(viewCache);
            }

            bindDetailView(position, v, entry);
            return v;
        }

        private View getCalllogTitleEntryView() {
            final View titleView = mInflater.inflate(R.layout.ali_contacts_detail_calllog_title, null);
            final TextView countTxt = (TextView) titleView.findViewById(R.id.call_count);
            if (mCallCount > 0) {
                String countStr = mCallCount == 1 ? getResources().getString(R.string.recent_one_call_count) : String.format(
                        getResources().getString(R.string.recent_call_count), mCallCount);
                countTxt.setText(countStr);
            } else {
                countTxt.setText("");
            }
            return titleView;
        }

        private View getClearCalllogItemView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.ali_contacts_detail_clear_calllog, null);
                final Button delete = (Button) convertView.findViewById(R.id.clear_calllog);
                delete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View vc) {
                        AlertDialog.Builder build = new AlertDialog.Builder(mContext);
                        build.setMessage(mContext.getString(R.string.confirm_delete_detail_calllog));
                        build.setPositiveButton(R.string.contact_detail_calllog_clear, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteCalllogs(mContext);
                                UsageReporter.onClick(mActivity, null, UsageReporter.ContactsDetailPage.DETAL_DELETE_CALLLOG);
                            }
                        });
                        build.setNegativeButton(R.string.no, null);
                        AlertDialog dialog = build.create();
                        dialog.show();
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                    }
                });
            }
            return convertView;
        }

        private View getCalllogItemEntryView(int position, View convertView, ViewGroup parent) {
            final CalllogItemViewEntry calllogItemEntry = (CalllogItemViewEntry) mAllEntries.get(position);
            CalllogItemViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.ali_call_detail_history_item_with_icon, parent, false);
                holder = new CalllogItemViewHolder();
                holder.callTypeTextView = (TextView) convertView.findViewById(R.id.call_type_text);
                holder.tvDuration = (TextView) convertView.findViewById(R.id.call_detail_listitem_duration);
                holder.tvDate = (TextView) convertView.findViewById(R.id.call_detail_listitem_calldate);
                // holder.ivTypeIcon =
                // (ImageView)convertView.findViewById(R.id.call_detail_listitem_typeicon);
                holder.recordBtn = (ImageView) convertView.findViewById(R.id.call_detail_listitem_record_btn);
                holder.outputImg = (ImageView) convertView.findViewById(R.id.call_detail_listitem_out_put);
                convertView.setTag(holder);
            } else {
                holder = (CalllogItemViewHolder) convertView.getTag();
            }

            int callType = calllogItemEntry.mCallType;
            int callFeatures = calllogItemEntry.mCallFeatures;
            if ((callFeatures & CallerViewQuery.CALL_FEATURES_BIT_VIDEO) != 0) {
                int pxFor8dp = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
                holder.callTypeTextView.setCompoundDrawablePadding(pxFor8dp);
                holder.callTypeTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, R.drawable.ic_callhistory_facetime_normal, 0);
            } else if ((callFeatures & CallerViewQuery.CALL_FEATURES_BIT_HD) != 0) {
                int pxFor8dp = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
                holder.callTypeTextView.setCompoundDrawablePadding(pxFor8dp);
                holder.callTypeTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, R.drawable.ic_callhistory_hd_normal, 0);
            } else {
                holder.callTypeTextView.setCompoundDrawablePadding(0);
                holder.callTypeTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
            }

            DebugLog.d(TAG, "phoneRecordPath = " + calllogItemEntry.mPhoneRecordPath);
            if (TextUtils.isEmpty(calllogItemEntry.mPhoneRecordPath)) {
                holder.recordBtn.setVisibility(View.INVISIBLE);
            } else {
                holder.recordBtn.setVisibility(View.VISIBLE);
                holder.recordBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, CallLogRecordActivity.class);
                        intent.putExtra(CallLogRecordActivity.EXTRA_RECORD_PATH, calllogItemEntry.mPhoneRecordPath);
                        intent.putExtra(CallLogRecordActivity.EXTRA_CALL_URI, calllogItemEntry.mCallUri);
                        ContactDetailFragment.this.startActivityForResult(intent, REQUEST_CODE_RECORD_PATH);
                        UsageReporter.onClick(mActivity, null, UsageReporter.DialpadPage.CALL_RECORD);
                    }
                });
            }

            holder.tvDuration.setVisibility(View.VISIBLE);
            holder.tvDuration.setText(
                    DateUtils.getCalllogItemDurationString(mContext, callType, calllogItemEntry.mDuration));

            CharSequence dateValue;
            String timeFormat = DateFormat.getTimeFormatString(mContext);
            if (AliCallLogExtensionHelper.dateFormat(ONLY_YEAR_FORMAT, calllogItemEntry.mDate).equals(
                    AliCallLogExtensionHelper.dateFormat(ONLY_YEAR_FORMAT, System.currentTimeMillis()))) {
                dateValue = AliCallLogExtensionHelper.dateFormat(MONTH_DAY_FORMAT, timeFormat, calllogItemEntry.mDate);
            } else {
                dateValue = AliCallLogExtensionHelper.dateFormat(FULL_FORMAT, timeFormat, calllogItemEntry.mDate);
            }

            holder.tvDate.setText(dateValue);

            if (callType == Calls.MISSED_TYPE) {
                holder.tvDuration.setVisibility(View.GONE);
            } else {
                holder.tvDuration.setVisibility(View.VISIBLE);
            }
            if (PluginPlatformPrefs.isCMCC()) {
                holder.tvDuration.setVisibility(View.GONE);
            }

            int simId = -1;
            if (sMultiSimEnable) {
                simId = SimUtil.getSlotId(calllogItemEntry.mSubId);
            }
            bindCallTypeView(holder.callTypeTextView, callType, simId, calllogItemEntry.mRingTime);

            final int index = calllogItemEntry.getCalllogIndex();
            if (0 == index && 1 < calllogItemEntry.getCalllogTotal()) {
                holder.outputImg.setVisibility(View.VISIBLE);
                holder.outputImg.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCalllogViewState = CalllogViewState.MULTILINE == mCalllogViewState ? CalllogViewState.SINGLELINE
                                : CalllogViewState.MULTILINE;
                        mAllEntries.removeAll(mCalllogEntries);
                        flattenCalllogList();
                    }
                });
                holder.outputImg
                        .setImageResource(CalllogViewState.SINGLELINE == mCalllogViewState ? R.drawable.dial_history_conceal_normal
                                : R.drawable.dial_history_unfold_normal);
            } else {
                holder.outputImg.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }

        private void bindCallTypeView(TextView textview, int type, int simId, long ringTime) {
            if (type == Calls.MISSED_TYPE) {
                // Check isRingOnce
                String format2 = mContext.getString(R.string.ringing_time);
                if (ringTime == 0) {
                    String format = mContext.getString(R.string.calllog_detail_calltype_missed);
                    String defaultFormat = mContext.getString(R.string.missed_call);
                    textview.setText(AliCallLogExtensionHelper.formatCallTypeLabel(mContext, format, defaultFormat, simId));
                } else {
                    textview.setText(AliCallLogExtensionHelper.formatCallTypeLabel(mContext, format2, simId, ringTime));
                }
                textview.setTextColor(mContext.getResources().getColor(R.drawable.aui_ic_color_red_to_white));
            } else if (type == Calls.INCOMING_TYPE) {
                String format = mContext.getString(R.string.calllog_detail_calltype_in);
                String defaultFormat = mContext.getString(R.string.incoming_call);
                textview.setText(AliCallLogExtensionHelper.formatCallTypeLabel(mContext, format, defaultFormat, simId));
                textview.setTextColor(mContext.getResources().getColor(R.color.call_detail_history_item_txt_color));
            } else if (type == Calls.OUTGOING_TYPE) {
                String format = mContext.getString(R.string.calllog_detail_calltype_out);
                String defaultFormat = mContext.getString(R.string.outgoing_call);
                textview.setText(AliCallLogExtensionHelper.formatCallTypeLabel(mContext, format, defaultFormat, simId));
                textview.setTextColor(mContext.getResources().getColor(R.color.call_detail_history_item_txt_color));
            }
        }

        private void bindDetailView(int position, View view, final DetailViewEntry entry) {
            final Resources resources = mContext.getResources();
            DetailViewCache views = (DetailViewCache) view.getTag();

            if (!TextUtils.isEmpty(entry.typeString)) {
                views.type.setText(entry.typeString);
                views.type.setVisibility(View.VISIBLE);
            } else if (entry.kind != null) {
                views.type.setText(entry.kind);
                views.type.setVisibility(View.VISIBLE);
            }

            views.data.setText(entry.data);
            setMaxLines(views.data, entry.maxLines);

            // Set the default contact method
            views.primaryIndicator.setVisibility(entry.isPrimary ? View.VISIBLE : View.GONE);

            // Set the presence icon
            final Drawable presenceIcon = ContactPresenceIconUtil.getPresenceIcon(mContext, entry.presence);
            final ImageView presenceIconView = views.presenceIcon;
            if (presenceIcon != null) {
                presenceIconView.setImageDrawable(presenceIcon);
                presenceIconView.setVisibility(View.VISIBLE);
            } else {
                presenceIconView.setVisibility(View.GONE);
            }

            final ActionsViewContainer actionsButtonContainer = (ActionsViewContainer) views.actionsViewContainer;
            actionsButtonContainer.setTag(entry);
            actionsButtonContainer.setPosition(position);
            // mSelectedPosition = position;
            // registerForContextMenu(actionsButtonContainer); //commented by
            // xiaodong.lxd
            if (!entry.isInSubSection()) {
                actionsButtonContainer.setOnLongClickListener(ContactDetailFragment.this);
            }

            // Set the secondary action button
            final ImageView secondaryActionView = views.secondaryActionButton;
            int secondaryActionIconId = -1;
            String secondaryActionDescription = null;
            if (entry.secondaryActionIcon != -1) {
                secondaryActionIconId = entry.secondaryActionIcon;
                secondaryActionDescription = resources.getString(entry.secondaryActionDescription);
            } else if ((entry.chatCapability & Im.CAPABILITY_HAS_CAMERA) != 0) {
                secondaryActionIconId = R.drawable.sym_action_videochat_holo_light;
                secondaryActionDescription = resources.getString(R.string.video_chat);
            } else if ((entry.chatCapability & Im.CAPABILITY_HAS_VOICE) != 0) {
                secondaryActionIconId = R.drawable.sym_action_audiochat_holo_light;
                secondaryActionDescription = resources.getString(R.string.audio_chat);
            }

            if (entry.secondaryIntent != null && secondaryActionIconId != -1) {
                secondaryActionView.setImageResource(secondaryActionIconId);
                secondaryActionView.setContentDescription(secondaryActionDescription);
                secondaryActionView.setTag(entry);
                secondaryActionView.setVisibility(View.VISIBLE);
            } else {
                secondaryActionView.setVisibility(View.GONE);
            }

            final ImageView thirdActionView = views.thirdActionButton;
            if (entry.thirdIntent != null && entry.thirdActionIcon != -1) {
                thirdActionView.setImageResource(entry.thirdActionIcon);
                thirdActionView.setTag(entry);
                thirdActionView.setVisibility(View.VISIBLE);
            } else {
                thirdActionView.setVisibility(View.GONE);
            }

            final ImageView fourthActionView = views.fourthActionButton;
            if (entry.fourthIntent != null && entry.fourthActionIcon != -1) {
                fourthActionView.setImageResource(entry.fourthActionIcon);
                fourthActionView.setTag(entry);
                fourthActionView.setVisibility(View.VISIBLE);
            } else {
                fourthActionView.setVisibility(View.GONE);
            }

        }

        private void setMaxLines(TextView textView, int maxLines) {
            if (maxLines == 1) {
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                textView.setSingleLine(false);
                textView.setMaxLines(maxLines);
                textView.setEllipsize(null);
            }
        }

        // TODO: maybe we can implement the four OnClickListener into one.
        private final OnClickListener mPrimaryActionClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "mPrimaryActionClickListener click!");
                if (mListener == null)
                    return;
                final ViewEntry entry = (ViewEntry) view.getTag();
                if (entry == null)
                    return;
                entry.click(view, mListener);
            }
        };

        private final OnClickListener mSecondaryActionClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "mSecondaryActionClickListener click!");
                if (mListener == null)
                    return;
                if (view == null)
                    return;
                final ViewEntry entry = (ViewEntry) view.getTag();
                if (!(entry instanceof DetailViewEntry))
                    return;
                final DetailViewEntry detailViewEntry = (DetailViewEntry) entry;
                final Intent intent = detailViewEntry.secondaryIntent;
                if (intent == null)
                    return;
                mListener.onItemClicked(intent);
                sendReport(mContext, intent);
            }
        };

        private final OnClickListener mThirdActionClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "mThirdActionClickListener click!");
                if (mListener == null)
                    return;
                if (view == null)
                    return;
                final ViewEntry entry = (ViewEntry) view.getTag();
                if (!(entry instanceof DetailViewEntry))
                    return;
                final DetailViewEntry detailViewEntry = (DetailViewEntry) entry;
                final Intent intent = detailViewEntry.thirdIntent;
                if (intent == null) {
                    return;
                }
                mListener.onItemClicked(intent);
                sendReport(mContext, intent);
            }
        };

        private final OnClickListener mFourthActionClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "mFourthActionClickListener click!");
                if (mListener == null)
                    return;
                if (view == null)
                    return;
                final ViewEntry entry = (ViewEntry) view.getTag();
                if (!(entry instanceof DetailViewEntry))
                    return;
                final DetailViewEntry detailViewEntry = (DetailViewEntry) entry;
                final Intent intent = detailViewEntry.fourthIntent;
                if (intent == null) {
                    return;
                }
                mListener.onItemClicked(intent);
                sendReport(mContext, intent);
            }
        };

        @Override
        public int getCount() {
            return mAllEntries.size();
        }

        @Override
        public ViewEntry getItem(int position) {
            int count = getCount();
            if (position >= count) {
                Log.e(TAG, String.format("getItem position=%s Count=%s", position, count));
                return null;
            }
            return mAllEntries.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return mAllEntries.get(position).getViewType();
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public long getItemId(int position) {
            final ViewEntry entry = mAllEntries.get(position);
            if (entry != null) {
                return entry.getId();
            }
            return -1;
        }

        @Override
        public boolean areAllItemsEnabled() {
            // Header will always be an item that is not enabled.
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }
    }

    @Override
    public void onAccountSelectorCancelled() {
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        createCopy(account);
    }

    private void createCopy(AccountWithDataSet account) {
        if (mListener != null) {
            mListener.onCreateRawContactRequested(mContactData.getContentValues(), account);
        }
    }

    /**
     * Default (fallback) list item click listener. Note the click event for
     * DetailViewEntry is caught by individual views in the list item view to
     * distinguish the primary action and the secondary action, so this method
     * won't be invoked for that. (The listener is set in the bindview in the
     * adapter) This listener is used for other kind of entries.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener == null)
            return;
        final ViewEntry entry = mAdapter.getItem(position);
        if (entry == null)
            return;
        entry.click(view, mListener);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        DetailViewEntry selectedEntry = (DetailViewEntry) mAllEntries.get(info.position);

        menu.setHeaderTitle(selectedEntry.data);
        menu.add(ContextMenu.NONE, ContextMenuIds.COPY_TEXT, ContextMenu.NONE, getString(R.string.copy_text));

        String selectedMimeType = selectedEntry.mimetype;

        // Defaults to true will only enable the detail to be copied to the
        // clipboard.
        boolean isUniqueMimeType = true;

        // Only allow primary support for Phone and Email content types
        if (Phone.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
            isUniqueMimeType = mIsUniqueNumber;
        } else if (Email.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
            isUniqueMimeType = mIsUniqueEmail;
        }

        // Checking for previously set default
        if (selectedEntry.isPrimary) {
            menu.add(ContextMenu.NONE, ContextMenuIds.CLEAR_DEFAULT, ContextMenu.NONE, getString(R.string.clear_default));
        } else if (!isUniqueMimeType) {
            menu.add(ContextMenu.NONE, ContextMenuIds.SET_DEFAULT, ContextMenu.NONE, getString(R.string.set_default));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo;
        try {
            menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case ContextMenuIds.COPY_TEXT:
                copyToClipboard(menuInfo.position);
                return true;
            case ContextMenuIds.SET_DEFAULT:
                setDefaultContactMethod(mListView.getItemIdAtPosition(menuInfo.position));
                return true;
            case ContextMenuIds.CLEAR_DEFAULT:
                clearDefaultContactMethod(mListView.getItemIdAtPosition(menuInfo.position));
                return true;
            default:
                throw new IllegalArgumentException("Unknown menu option " + item.getItemId());
        }
    }

    private void setDefaultContactMethod(long id) {
        Intent setIntent = ContactSaveService.createSetSuperPrimaryIntent(mContext, id);
        mContext.startService(setIntent);
        UsageReporter.onClick(null, TAG, UsageReporter.ContactsDetailPage.DETAL_LC_SET_DEFAULT_NUMBER);
    }

    private void clearDefaultContactMethod(long id) {
        Intent clearIntent = ContactSaveService.createClearPrimaryIntent(mContext, id);
        mContext.startService(clearIntent);
        UsageReporter.onClick(null, TAG, UsageReporter.ContactsDetailPage.DETAL_LC_CLEAR_DEFAULT_NUMBER);
    }

    private void copyToClipboard(int viewEntryPosition) {
        // Getting the text to copied
        DetailViewEntry detailViewEntry = (DetailViewEntry) mAllEntries.get(viewEntryPosition);
        CharSequence textToCopy = detailViewEntry.data;

        // Checking for empty string
        if (TextUtils.isEmpty(textToCopy))
            return;

        ClipboardUtils.copyText(getActivity(), detailViewEntry.typeString, textToCopy, true);
    }

    private void copyToClipboard(CharSequence textToCopy, String typeString) {
        // Checking for empty string
        if (TextUtils.isEmpty(textToCopy))
            return;
        ClipboardUtils.copyText(getActivity(), typeString, textToCopy, true);
        UsageReporter.onClick(null, TAG, UsageReporter.ContactsDetailPage.DETAL_LC_COPY_NUMBER);
    }

    @Override
    public boolean handleKeyDown(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_CALL) {
            int index = mListView.getSelectedItemPosition();
            if (index != -1) {
                return false;
            } else if (mPrimaryPhoneUri != null) {
                // There isn't anything selected, call the default number
                mContext.startActivity(CallUtil.getCallIntent(mContext, mPrimaryPhoneUri));
                return true;
            }
        }

        return false;
    }

    public static interface Listener {
        /**
         * User clicked a single item (e.g. mail). The intent passed in could be
         * null.
         */
        public void onItemClicked(Intent intent);

        /**
         * User requested creation of a new contact with the specified values.
         *
         * @param values ContentValues containing data rows for the new contact.
         * @param account Account where the new contact should be created.
         */
        public void onCreateRawContactRequested(ArrayList<ContentValues> values, AccountWithDataSet account);
    }

    /**
     * Adapter for the invitable account types; used for the invitable account
     * type list popup.
     */
    private final static class InvitableAccountTypesAdapter extends BaseAdapter {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private final ArrayList<AccountType> mAccountTypes;

        public InvitableAccountTypesAdapter(Context context, Contact contactData) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            final List<AccountType> types = contactData.getInvitableAccountTypes();
            mAccountTypes = new ArrayList<AccountType>(types.size());

            for (int i = 0; i < types.size(); i++) {
                mAccountTypes.add(types.get(i));
            }

            Collections.sort(mAccountTypes, new AccountType.DisplayLabelComparator(mContext));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View resultView = (convertView != null) ? convertView : mInflater.inflate(
                    R.layout.account_selector_list_item, parent, false);

            final TextView text1 = (TextView) resultView.findViewById(android.R.id.text1);
            final TextView text2 = (TextView) resultView.findViewById(android.R.id.text2);
            final ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);

            final AccountType accountType = mAccountTypes.get(position);

            CharSequence action = accountType.getInviteContactActionLabel(mContext);
            CharSequence label = accountType.getDisplayLabel(mContext);
            if (TextUtils.isEmpty(action)) {
                text1.setText(label);
                text2.setVisibility(View.GONE);
            } else {
                text1.setText(action);
                text2.setVisibility(View.VISIBLE);
                text2.setText(label);
            }
            icon.setImageDrawable(accountType.getDisplayIcon(mContext));

            return resultView;
        }

        @Override
        public int getCount() {
            return mAccountTypes.size();
        }

        @Override
        public AccountType getItem(int position) {
            return mAccountTypes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    @Override
    public boolean onLongClick(final View v) {
        final Activity activity = getActivity();
        ActionsViewContainer actionsViewContainer = (ActionsViewContainer) v;

        final DetailViewEntry entry = (DetailViewEntry) actionsViewContainer.getTag();
        final String typeString = entry.typeString;
        boolean isUniqueMimeType = true;
        final int position = actionsViewContainer.getPosition();
        final ActionSheet actionSheet = new ActionSheet(activity);
        String selectedMimeType = entry.mimetype;
        boolean isPhoneType = false;

        // Only allow primary support for Phone and Email content types
        if (Phone.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
            isPhoneType = true;
            isUniqueMimeType = mIsUniqueNumber;
        } else if (Email.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
            isUniqueMimeType = mIsUniqueEmail;
        }
        final boolean isUniqueMimeTypeFinal = isUniqueMimeType;
        List<String> items = new ArrayList<String>(5);
        int[] table = null;

        class PopupMenuClickListener implements ActionSheet.CommonButtonListener {
            private int[] mIndexToReal;

            public PopupMenuClickListener(int[] table) {
                mIndexToReal = table;
            }

            @Override
            public void onDismiss(ActionSheet arg0) {
            }

            @Override
            public void onClick(int which) {
                int what = mIndexToReal[which];
                switch (what) {
                    case 0: // Make common call.
                        makeCallByLongClick(v, entry);
                        break;
                    case 4: // copy text.
                        copyToClipboard(entry.data, typeString);
                        break;
                    case 5: // set or clear default number.
                        if (entry.isPrimary) {
                            clearDefaultContactMethod(mListView.getItemIdAtPosition(position));
                        } else if (!isUniqueMimeTypeFinal) {
                            setDefaultContactMethod(mListView.getItemIdAtPosition(position));
                        }
                        break;
                    default:
                        Log.d(TAG, "PopupMenu onClick(), case default, do nothing!");
                        break;
                }
            }

            private void makeCallByLongClick(View v, DetailViewEntry entry) {
                Intent intent = entry.intent;
                if (intent == null) {
                    return;
                }
                int reportType = intent.getIntExtra(ONLY_FOR_REPORT_FLAG, 0);
                intent.putExtra(ONLY_FOR_REPORT_FLAG, reportType | LONG_CLICK_REPORT_TYPE);
                entry.click(v, mListener);
            }
        };

        boolean isUserProfile = mContactData != null && mContactData.isUserProfile();
        if (isPhoneType && !isUserProfile) {
            items.add(getString(R.string.call_other));
            table = new int[] {
                    0, 4, 5
            };
        } else {
            table = new int[] {
                    4, 5
            };
        }

        items.add(getString(R.string.copy_text));
        // Per YaoWei's comments in bug http://k3.alibaba-inc.com/issue/6506061?versionId=1169325
        // we do NOT set title for action sheet any more.
        // actionSheet.setTitle(entry.data);
        if (entry.isPrimary) {
            items.add(getString(R.string.clear_default));
        } else if (!isUniqueMimeType) {
            items.add(getString(R.string.set_default));
        }

        actionSheet.setCommonButtons(items, null, null, new PopupMenuClickListener(table));
        actionSheet.show(v);
        return true;
    }

    private void genCalllogViewEntries(final Cursor cursor) {
        mCallCount = 0;
        mAllEntries.removeAll(mCalllogEntries);
        mCalllogEntries.clear();

        if (cursor == null || !cursor.moveToFirst()) {
            return;
        }

        mCalllogEntries.add(new CalllogTitleViewEntry());
        final int count = cursor.getCount();

        CalllogItemViewEntry entry;
        for (int i = 0; i < count; ++i) {
            entry = new CalllogItemViewEntry(i, count,
                    cursor.getLong(CallDetailQuery.ID_COLUMN_INDEX),
                    cursor.getLong(CallDetailQuery.DATE_COLUMN_INDEX),
                    cursor.getLong(CallDetailQuery.DURATION_COLUMN_INDEX),
                    cursor.getString(CallDetailQuery.NUMBER_COLUMN_INDEX),
                    cursor.getInt(CallDetailQuery.CALL_TYPE_COLUMN_INDEX),
                    cursor.getInt(CallDetailQuery.CALL_FEATURES_COLUMN_INDEX),
                    cursor.getString(CallDetailQuery.COUNTRY_ISO_COLUMN_INDEX),
                    SimUtil.MULTISIM_ENABLE ? cursor.getInt(CallDetailQuery.SUB_COLUMN_INDEX) : -1,
                    cursor.getString(CallDetailQuery.LOC_PROVINCE_COLUMN_INDEX),
                    cursor.getString(CallDetailQuery.LOC_AREA_COLUMN_INDEX),
                    cursor.getLong(CallDetailQuery.RING_TIME_COLUMN_INDEX),
                    cursor.getString(CallDetailQuery.RECORD_PATH_COLUMN_INDEX));
            if (isCurrTimePeriod(cursor.getLong(cursor.getColumnIndex(Calls.DATE)))) {
                mCallCount++;
            }
            mCalllogEntries.add(entry);
            mCalllogEntries.add(new SeparatorViewEntry());
            cursor.moveToNext();
        }
        if (count > 0) {
            mCalllogEntries.add(new ClearCalllogItemEntry());
            mCalllogEntries.add(new SeparatorViewEntry());
        }
    }

    // month unint
    private boolean isCurrTimePeriod(long milliseconds) {
        Calendar earlierCal = Calendar.getInstance();
        earlierCal.add(Calendar.DATE, -30);
        Date currDate = Calendar.getInstance().getTime();
        Date earlierDate = earlierCal.getTime();
        Date callDate = new Date(milliseconds);
        boolean after = callDate.after(earlierDate);
        boolean before = callDate.before(currDate);
        if (after && before) {
            return true;
        }
        return false;
    }

    private void resetCalllogViewState() {
        mCalllogViewState = CalllogViewState.SINGLELINE;
    }

    private void flattenCalllogList() {
        if (!mCalllogEntries.isEmpty()) {
            // find position to insert call log views: if last view is alipay
            // plugin, above it.
            // int maxIndex = mAllEntries.size() - 1;
            // int type = mAllEntries.get(maxIndex).getViewType();

            // final int placeToInsert = (ViewAdapter.VIEW_TYPE_APLIPAY_PLUGIN_ENTRY == type) ? maxIndex : maxIndex + 1;
            final int placeToInsert = mAllEntries.size();
            switch (mCalllogViewState) {
                case SINGLELINE:
                    mAllEntries.addAll(placeToInsert, mCalllogEntries.subList(0, 3));
                    break;
                case MULTILINE:
                    mAllEntries.addAll(placeToInsert, mCalllogEntries);
                    break;
                default:
                    break;
            }

        }
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

    }

    private void loadCalllogs() {
        boolean update = false;
        ArrayList<String> numbers = getContactNumberList();
        if (mLastNumberList == null) {
            mLastNumberList = Lists.newArrayList();
            update = true;
        } else {
            if (mLastNumberList.size() != numbers.size()) {
                update = true;
            } else {
                for (String number : mLastNumberList) {
                    if (!numbers.contains(number)) {
                        update = true;
                        break;
                    }
                }
            }
        }

        if (update) {
            mLastNumberList.clear();
            mLastNumberList.addAll(mNumberList);
        }

        if (mCalls == null || update) {
            startLoadingCallLogsInternal(numbers);
        } else {
            genCalllogViewEntries(mCalls);
            flattenCalllogList();
        }
    }

    private void deleteCalllogs(final Context context) {
        if (mCalllogEntries.isEmpty() || null == context) {
            return;
        }
        final int count = mCalllogEntries.size();
        final StringBuilder ids = new StringBuilder();
        ids.append(Calls._ID).append(" IN (");
        ViewEntry item;
        for (int i = 0; i < count; ++i) {
            item = mCalllogEntries.get(i);
            if (ViewAdapter.VIEW_TYPE_CALLLOG_ITEM_ENTRY != item.getViewType()) {
                continue;
            }
            ids.append(((CalllogItemViewEntry) item).getCalllogId()).append(',');
        }
        ids.deleteCharAt(ids.length() - 1).append(')');

        new Thread() {
            @Override
            public void run() {
                // TODO: it is possible that delete operations fail, e.g. disk
                // full,
                // so we'd better to catch SqliteFullException and load call
                // logs after delete.
                context.getContentResolver().delete(Calls.CONTENT_URI, ids.toString(), null);
            }
        }.start();

        mAllEntries.removeAll(mCalllogEntries);
        mCalllogEntries.clear();
        resetCalllogViewState();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

    }

    // YUNOS BEGIN
    // Description:BugID:5450902:APR NULL Exception
    // author:changjun.bcj
    // date:2014/11/13
    private void clearAllEntries() {
        mAllEntries.clear();
        mPhoneNumber = null;
        // if (mAdapter != null) {
        // mAdapter.notifyDataSetChanged();
        // }
    }
    // YUNOS END

    public static class CalllogItemViewHolder {
        TextView callTypeTextView;
        TextView tvDuration;
        TextView tvDate;
        ImageView recordBtn;
        ImageView outputImg;
    }

    private String[] getCalllogItemDetailProjection() {
        if (SimUtil.MULTISIM_ENABLE) {
            return CallDetailQuery.CALL_LOG_PROJECTION_MULTISIM;
        } else {
            return CallDetailQuery.CALL_LOG_PROJECTION;
        }
    }

    private void createPhotoHandler(View photoView) {
        if (mCurrentPhotoHandler != null) {
            return;
        }
        mCurrentPhotoHandler = new PhotoHandler(mContext, photoView, mContactData.createRawContactDeltaList());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CODE_RECORD_PATH == requestCode && Activity.RESULT_OK == resultCode && mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            return;
        }

        if (mCurrentPhotoHandler != null && mCurrentPhotoHandler.handlePhotoActivityResult(requestCode, resultCode, data)) {
            // mSubActivityInProgress = false;
            return;
        }
    }

    /**
     * Get cached number list of the contact.
     *
     * @return
     */
    public ArrayList<String> getContactNumberList() {
        ArrayList<String> result = new ArrayList<String>();
        synchronized (mNumberList) {
            result.addAll(mNumberList);
        }
        return result;
    }

    /**
     * Custom photo handler for the editor. The inner listener that this creates
     * also has a reference to the editor and acts as an {@link EditorListener},
     * and uses that editor to hold state information in several of the listener
     * methods.
     */
    private final class PhotoHandler extends PhotoSelectionHandler {

        // final long mRawContactId;
        private final PhotoActionListener mPhotoEditorListener;

        public PhotoHandler(Context context, View photo, RawContactDeltaList state) {
            super(context, photo, false, state);
            mPhotoEditorListener = new PhotoEditorListener();
        }

        @Override
        public PhotoActionListener getListener() {
            return mPhotoEditorListener;
        }

        @Override
        public void startPhotoActivity(Intent intent, int requestCode, Uri photoUri) {
            mCurrentPhotoUri = photoUri;
            ContactDetailFragment.this.startActivityForResult(intent, requestCode);
        }

        private final class PhotoEditorListener extends PhotoSelectionHandler.PhotoActionListener {

            @Override
            public void onPhotoSelected(Uri uri) throws FileNotFoundException {
                if (mContactData == null || getActivity() == null) {
                    // monkey crash
                    return;
                }

                RawContactDeltaList delta = getDeltaForAttachingPhotoToContact();
                long rawContactId = getWritableEntityId();

                Intent intent = ContactSaveService.createSaveContactIntent(mContext, delta, "", 0,
                        mContactData.isUserProfile(), null, null, rawContactId, uri, null, false);
                getActivity().startService(intent);
            }

            @Override
            public Uri getCurrentPhotoUri() {
                return mCurrentPhotoUri;
            }

            @Override
            public void onPhotoSelectionDismissed() {
                // Nothing to do.
            }

            @Override
            public void onPhotoSelected(Bitmap bitmap, Uri uri) {
                RawContactDeltaList delta = getDeltaForAttachingPhotoToContact();
                long rawContactId = getWritableEntityId();
                Intent intent = ContactSaveService.createSaveContactIntent(mContext, delta, "", 0,
                        mContactData.isUserProfile(), null, null, rawContactId, uri, null, false);
                getActivity().startService(intent);
            }
        }

        @Override
        public Activity getAttachedActivity() {
            return getActivity();
        }
    }

    /**
     * append numbers in where clause.
     * @param clause The sql where clause to append with the IN sub-clause.
     * @param values The values in the candidates for IN.
     */
    private void appendNumbersInWhereClause(StringBuilder clause, List<String> numberList) {
        StringBuilder subClause = new StringBuilder();
        int len = numberList.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                subClause.append(',');
            }
            String trimPhoneNum = NumberNormalizeUtil.trimHyphenAndSpaceInNumberString(numberList.get(i));
            subClause.append('\'').append(trimPhoneNum.replace("'", "''")).append('\'');
        }
        clause.append(CallsTable.COLUMN_NUMBER + " IN (").append(subClause).append(") OR ")
              .append(CallsTable.COLUMN_NORMALIZED_NUM + " IN (").append(subClause).append(')');
    }

    public void startLoadingCallLogs() {
        startLoadingCallLogsInternal(getContactNumberList());
    }

    private void startLoadingCallLogsInternal(ArrayList<String> numbers) {
        if (mContactData == null) {
            Log.e(TAG, "startLoadingCallLogsInternal: data is null.");
            return;
        }
        if (numbers.isEmpty()) {
            Log.e(TAG, "startLoadingCallLogsInternal: empty numbers.");
            return;
        }
        if (getActivity() == null) {
            Log.e(TAG, "startLoadingCallLogsInternal: activity is null.");
            return;
        }
        final StringBuilder selection = new StringBuilder();
        appendNumbersInWhereClause(selection, numbers);
        // TODO: To load call logs in thread pool,
        // we have a risk that more than one queries are executing at the same time.
        // The query issued later might return earlier.
        new LoadCallLogTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selection.toString());
    }

    private class LoadCallLogTask extends AsyncTask <String, Void, Cursor>{

        @Override
        protected Cursor doInBackground(String... params) {
            Cursor cursor = null;

            try {
                cursor = CallLogManager.getInstance(ActivityThread.currentApplication())
                        .queryAliCalls(getCalllogItemDetailProjection(), params[0],
                                null, Calls.DEFAULT_SORT_ORDER);
            } catch (Exception e) {
                Log.e(TAG, "LoadCallLogTask Exception", e);
            }

            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            if (result == null) {
                Log.e(TAG, "LoadCallLogTask callLog is null.");
                return;
            }
            if (mCalls != null) {
                mCalls.close();
            }

            mCalls = result;
            genCalllogViewEntries(result);
            flattenCalllogList();
        }
    }

    private static final int MSG_VOLTE_ATTACH_STATE_CHANGED = 100;
    private boolean mIsStayVolte = false;

    private boolean isVideoEnabled() {
        Context context = getActivity();
        if (context == null) {
            return false;
        }
        if (SimUtil.IS_PLATFORM_MTK || SimUtil.IS_PLATFORM_QCOMM) {
            return SimUtil.isVideoCallEnabled(context);
        } else if (SimUtil.IS_PLATFORM_SPREADTRUM) {
            return mIsStayVolte && SimUtil.isVideoCallEnabled(context);
        }
        return false;
    }

    private void updateVideoCallButton() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private VolteAttachHandler mVolteAttachHandler = new VolteAttachHandler(this);
    private static class VolteAttachHandler extends Handler {
        final ContactDetailFragment mFragment;
        public VolteAttachHandler(ContactDetailFragment fragment) {
            mFragment = fragment;
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_VOLTE_ATTACH_STATE_CHANGED) {
                boolean needUpdate = mFragment.mIsStayVolte != (msg.arg2 == 1);
                mFragment.mIsStayVolte = msg.arg2 == 1;
                if (needUpdate) {
                    mFragment.updateVideoCallButton();
                }
            }
        }
    }

}
