/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.yunos.alicontacts;
//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
import com.bird.contacts.BirdFeatureOption;
//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
import android.app.ActivityThread;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/* YUNOS BEGIN PB */
//##module [Smart Gesture] ##BugID:168585
//##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
//##description: Support Single & Dual Orient Smart Gesture based on proximity
import com.aliyunos.smartgesture.SmartGestureDetector;
import com.aliyunos.utils.Features;
/* YUNOS END PB */
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.activities.ContactDetailActivity;
import com.yunos.alicontacts.activities.ContactSelectionActivity;
import com.yunos.alicontacts.database.CallDetailQuery;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.database.CallLogManager.CallLogChangeListener;
import com.yunos.alicontacts.database.tables.CallsTable;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.dialpad.calllog.CallDetailHistoryAdapter;
import com.yunos.alicontacts.dialpad.calllog.CallLogAdapter;
import com.yunos.alicontacts.dialpad.calllog.CallTypeHelper;
import com.yunos.alicontacts.dialpad.calllog.CallerViewQuery;
import com.yunos.alicontacts.dialpad.calllog.ContactInfo;
import com.yunos.alicontacts.dialpad.calllog.ContactInfoHelper;
import com.yunos.alicontacts.dialpad.calllog.PhoneNumberHelper;
import com.yunos.alicontacts.dialpad.calllog.PhoneNumberInfo;
import com.yunos.alicontacts.list.AccountFilterManager;
import com.yunos.alicontacts.platform.PDUtils;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.AliTextUtils;
import com.yunos.alicontacts.util.AsyncTaskExecutor;
import com.yunos.alicontacts.util.AsyncTaskExecutors;
import com.yunos.alicontacts.util.ClipboardUtils;
import com.yunos.alicontacts.util.Constants;
import com.yunos.alicontacts.util.ContactsTextUtils;
import com.yunos.alicontacts.util.FeatureOptionAssistant;
import com.yunos.alicontacts.util.YunOSFeatureHelper;
import com.yunos.alicontacts.widget.aui.PopMenu;
import com.yunos.alicontacts.widget.aui.PopMenu.OnPopMenuListener;
import com.yunos.common.DebugLog;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionSheet;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the details of a specific call log entry.
 * <p>
 * This activity can be either started with the URI of a single call log entry,
 * or with the {@link #EXTRA_CALL_LOG_IDS} extra to specify a group of call log
 * entries.
 */
public class CallDetailActivity extends BaseActivity implements OnClickListener{
    private static final String TAG = "CallDetail";
    public static final String CALL_ORIGIN_CALL_DETAIL_ACTIVITY = "com.yunos.alicontacts.CallDetailActivity";

    /** The enumeration of {@link AsyncTask} objects used in this class. */
    public enum Tasks {
        MARK_VOICEMAIL_READ,
        DELETE_VOICEMAIL_AND_FINISH,
        REMOVE_FROM_CALL_LOG_AND_FINISH,
        UPDATE_PHONE_CALL_DETAILS,
    }

    /** A long array extra containing ids of call log entries to display. */
    public static final String EXTRA_CALL_LOG_IDS = "EXTRA_CALL_LOG_IDS";

    public static final String EXTRA_MATCH_PHONE_NUMBER = "match_phone_number";
    /**
     * If we are started with a voicemail, we'll find the uri to play with this
     * extra.
     */
    public static final String EXTRA_VOICEMAIL_URI = "EXTRA_VOICEMAIL_URI";
    /**
     * If we should immediately start playback of the voicemail, this extra will
     * be set to true.
     */
    public static final String EXTRA_VOICEMAIL_START_PLAYBACK = "EXTRA_VOICEMAIL_START_PLAYBACK";
    /** If the activity was triggered from a notification. */
    public static final String EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION";

    public static final String EXTRA_NEED_UPDATE_CALLLOG = "EXTRA_NEED_UPDATE_CALLLOG";

    private static final String[] RECORD_PATH_PROJECTION = { CallerViewQuery.COLUMN_PHONE_RECORD_PATH };

    private CallTypeHelper mCallTypeHelper;
    private AsyncTaskExecutor mAsyncTaskExecutor;

    private String mNumber = null;
    private String mLocation = null;
    private String mContactUri;

    private TextView mNameTextView;
    private TextView mLocationTextView;
    private PopMenu mPopMenu;

    /* package */LayoutInflater mInflater;
    /* package */Resources mResources;

    /* YUNOS BEGIN PB */
    //##module [Smart Gesture] ##BugID:168585
    //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
    //##description: Support Single & Dual Orient Smart Gesture based on proximity
    private SmartGestureDetector mSmartGestureDetector;
    /*YUNOS END PB*/

    /** Helper to load contact photos. */
    private ContactPhotoManager mContactPhotoManager;
    private ImageView mImagePortrait;
    private TextView mTextPortrait;
    private TextView mTvNumber;
    private ImageButton mVideoCallButton;
    private View mHeaderIconView;
    private long[] mCallLogIds;

    ActionSheet mPopupDialog;
    AlertDialog mConfirmDeleteDialog;
    private String mTagName = null;
    private int mMarkedCount = -1;

    private static final int FOOTER_ID_CREATE_NEW_CONTACT = 0;
    private static final int FOOTER_ID_ADD_TO_EXISTING_CONTACT = 1;
    private static final int FOOTER_ID_ADD_BLACKLIST = 2;
    private static final int FOOTER_ID_REMOVE = 3;
    private static final int FOOTER_ID_MARK = 4;

    private boolean mIsAContact = false;
    private boolean isInBlackList = false;
    private StringBuffer mIdsBuffer;

    private CallLogManager mCallLogManager = null;
    private CallLogChangeListener mCallLogChangeListener = null;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setActivityContentView(R.layout.ali_call_detail);
        mAsyncTaskExecutor = AsyncTaskExecutors.createThreadPoolExecutor();
        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mResources = getResources();
        mCallLogManager = CallLogManager.getInstance(ActivityThread.currentApplication());
        mCallTypeHelper = new CallTypeHelper(getResources());

        mImagePortrait = (ImageView) findViewById(R.id.photo);
        mTextPortrait = (TextView) findViewById(R.id.text_photo);
        mTvNumber = (TextView) findViewById(R.id.calllog_detail_number);
        mTvNumber.setOnClickListener(this);
        findViewById(R.id.calllog_detail_sms).setOnClickListener(this);
        findViewById(R.id.calllog_detail_call).setOnClickListener(this);
        mVideoCallButton = (ImageButton) findViewById(R.id.calllog_detail_videocall);
        mVideoCallButton.setOnClickListener(this);
        findViewById(R.id.navigate_detail_activity_btn).setOnClickListener(this);
        mHeaderIconView = findViewById(R.id.static_photo_container);

        mContactPhotoManager = ContactPhotoManager.getInstance(this);
        configureActionBar();
        final Intent intent = getIntent();
        mNumber = intent.getStringExtra(EXTRA_MATCH_PHONE_NUMBER);
        if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            closeSystemDialogs();
        }

        initNumberLongClickAction();
        initListeners(intent);
    }

    private void initListeners(Intent intent) {
        mCallLogChangeListener = new CallLogChangeListener() {
            @Override
            public void onCallLogChange(int changedPart) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateData(getCallLogEntryIds());
                    }
                });
            }
        };
        mCallLogManager.registCallsTableChangeListener(mCallLogChangeListener);
        if (intent.getBooleanExtra(EXTRA_NEED_UPDATE_CALLLOG, false)) {
            CallLogAdapter.setCursorDataChangeListener(new CallLogAdapter.DataChangeListener() {
                @Override
                public void onCallLogDataAdded(String number, long callId) {
                    if (mNumber != null && !mNumber.equals(number)) {
                        CallLogAdapter.setCursorDataChangeListener(null);
                    } else {
                        if (!checkCallIDExist(callId)) {
                            updateData(getCallLogEntryIds(callId));
                        }
                    }
                }
            });
        }

        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
        if (Features.SUPPORT_SINGLE_ORIENT_GESTURE || Features.SUPPORT_DUAL_ORIENT_GESTURE) {
            mSmartGestureDetector = new SmartGestureDetector(this,
                new SmartGestureDetector.OnSmartGestureListener(){
                    @Override
                    public void onNext() {
                        /*YUNOS BEGIN PB*/
                        //##date 2016-03-11  ## Author:xiongchao.lxc@alibaba-inc.om
                        //##BugID:7990395:don't dial when the phone is in use
                        if(!PDUtils.isPhoneIdle()) {
                            Log.d(TAG, "Rushon --> phone is in use , skip !!!");
                            return;
                        }
                        /*YUNOS END PB*/
                        Log.d(TAG, "Rushon --> NGuesture On Next");
                        makeCall();
                    }

                    @Override
                    public void onPrev() {
                    }
                });
            mSmartGestureDetector.setMode(SmartGestureDetector.MODE_PROXIMITY_FALLING, 1000);
        }
        /*YUNOS END PB*/
    }

    private void callNumber() {
        if(!mNumber.isEmpty()){
            Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mNumber));
            startActivity(dialIntent);
        }
    }

    private void initPopupDialog() {
        mPopupDialog = new ActionSheet(this);
        List<String> items = new ArrayList<String>(5);
        items.add(getString(R.string.call_other));

        int[] table = new int[] {
                0, 1
        };
        items.add(getString(R.string.copy_text));

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
                        makeCall();
                        UsageReporter.onClick(CallDetailActivity.this, null,
                                UsageReporter.CallDetailPage.CALL_DETAL_LC_MAKE_CALL);
                        break;
                    case 1: // copy text.
                        ClipboardUtils.copyText(CallDetailActivity.this, null, mTvNumber.getText(), true);
                        UsageReporter.onClick(CallDetailActivity.this, null,
                                UsageReporter.CallDetailPage.CALL_DETAIL_LC_TO_COPY);
                        break;
                    default:
                        Log.d(TAG, "PopupMenu onClick(), case default, do nothing!");
                        break;
                }
            }
        }
        ;
        mPopupDialog.setCommonButtons(items, null, null, new PopupMenuClickListener(table));
    }

    private void initNumberLongClickAction() {
        mTvNumber.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!PhoneNumberHelper.canPlaceCallsTo(mNumber)) {
                    // the non-callable number shall be safe to print in log.
                    Log.i(TAG, "number.onLongClick: number is not callable: "+mNumber);
                    return false;
                }
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                initPopupDialog();
                mPopupDialog.show(mTvNumber);
                return false;
            }
        });
    }

    private void initPopMenu() {
        if (mPopMenu == null) {
            mPopMenu = PopMenu.build(this, Gravity.TOP);
            mPopMenu.setOnIemClickListener(mPopMenuItemClick);
        }
        mPopMenu.clean();
        if (PhoneNumberHelper.canPlaceCallsTo(mNumber)) {
            if (!mIsAContact) {
                mPopMenu.addItem(FOOTER_ID_CREATE_NEW_CONTACT, getString(R.string.save_as_new_contact_for_dialpad));
                mPopMenu.addItem(FOOTER_ID_ADD_TO_EXISTING_CONTACT, getString(R.string.add_to_existing_contact_for_dialpad));
                if (!FeatureOptionAssistant.isInternationalSupportted()) {
                    mPopMenu.addItem(FOOTER_ID_MARK, getString(R.string.call_detail_mark_tag));
                }
            }

            if(AliCallLogExtensionHelper.PLATFORM_YUNOS) {
                int resId = R.string.calllog_blacklist_add;
                isInBlackList = YunOSFeatureHelper.isBlack(this, mNumber);
                if(isInBlackList) {
                    resId = R.string.calllog_blacklist_remove;
                }
                mPopMenu.addItem(FOOTER_ID_ADD_BLACKLIST, getString(resId));

            }
        }
        mPopMenu.addItem(FOOTER_ID_REMOVE, getString(R.string.calllog_delete));
    }

    private OnPopMenuListener mPopMenuItemClick = new OnPopMenuListener() {

        @Override
        public void onMenuItemClick(int what) {
            switch (what) {
                case FOOTER_ID_CREATE_NEW_CONTACT:
                    addNewContact();
                    UsageReporter.onClick(CallDetailActivity.this, null,
                            UsageReporter.DialpadPage.DP_ADD_CONTACT_FROM_DETAIL);
                    break;
                case FOOTER_ID_ADD_TO_EXISTING_CONTACT:
                    addToExistingContact();
                    UsageReporter.onClick(CallDetailActivity.this, null,
                            UsageReporter.DialpadPage.DP_ADD_EXISTING_CONTACT_FROM_DETAIL);
                    break;
                case FOOTER_ID_ADD_BLACKLIST:
                    handleBlackList();
                    break;
                case FOOTER_ID_REMOVE:
                    showRemoveDialog();
                    break;
                case FOOTER_ID_MARK:
                    mark();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     *
     */
    private void updateBlackListMenuItem() {
        if (AliCallLogExtensionHelper.PLATFORM_YUNOS && mPopMenu != null) {
            int resId = R.string.calllog_blacklist_add;
            isInBlackList = YunOSFeatureHelper.isBlack(this, mNumber);
            if (isInBlackList) {
                resId = R.string.calllog_blacklist_remove;
            }
            mPopMenu.updateItem(FOOTER_ID_ADD_BLACKLIST, getString(resId));

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // to resolve back from "add contacts" activity animation problem.
        // the animation is set by previous activity.
        // overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
        updateData(getCallLogEntryIds());
        SimContactUtils.observeVolteAttachChangeByPlatform(getApplicationContext(), mVolteAttachHandler, MSG_VOLTE_ATTACH_STATE_CHANGED);
        UsageReporter.onResume(this, null);
        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
        if (Features.SUPPORT_SINGLE_ORIENT_GESTURE || Features.SUPPORT_DUAL_ORIENT_GESTURE)
        {
            if (android.os.SystemProperties.getBoolean("persist.sys.ng_autodial", false)) {
                Log.d(TAG,"Rushon --> Contacts NGuesture start!");
                if(mSmartGestureDetector!=null) mSmartGestureDetector.start();
            }
        }
        /*YUNOS END PB */
    }

    @Override
    protected void onStop() {
        super.onStop();
        if ((mPopMenu != null) && mPopMenu.isShowing()) {
            mPopMenu.hide();
        }
        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
        if (Features.SUPPORT_SINGLE_ORIENT_GESTURE || Features.SUPPORT_DUAL_ORIENT_GESTURE)
        {
            if (android.os.SystemProperties.getBoolean("persist.sys.ng_autodial", false)) {
                Log.d(TAG,"Rushon --> Contacts NGuesture stop!");
                if(mSmartGestureDetector!=null) mSmartGestureDetector.stop();
            }
        }
        /*YUNOS END PB */
    }

    private Uri getVoicemailUri() {
        return getIntent().getParcelableExtra(EXTRA_VOICEMAIL_URI);
    }

    /**
     * Returns the list of URIs to show.
     * <p>
     * There are two ways the URIs can be provided to the activity: as the data
     * on the intent, or as a list of ids in the call log added as an extra on
     * the URI.
     * <p>
     * If both are available, the data on the intent takes precedence.
     */
    private long[] getCallLogEntryIds() {
        if (mCallLogIds == null) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                long id = -1;
                try {
                    id = ContentUris.parseId(uri);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "getCallLogEntryIds: can NOT parse id from uri: "+uri, nfe);
                    return null;
                }
                mCallLogIds = new long[] { id };
            } else {
                long[] ids = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);
                if (ids == null) {
                    return null;
                }
                mCallLogIds = new long[ids.length];
                System.arraycopy(ids, 0, mCallLogIds, 0, ids.length);
            }
        }
        return mCallLogIds;
    }

    private boolean checkCallIDExist(long callId) {
        long[] needCheckIdsList = null;
        if (mCallLogIds == null) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                try {
                    return callId == ContentUris.parseId(uri);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "checkCallIDExist: can NOT parse id from uri: "+uri, nfe);
                    return false;
                }
            }
            needCheckIdsList = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);
        } else {
            needCheckIdsList = mCallLogIds;
        }
        for (int index = 0; index < needCheckIdsList.length; ++index) {
            if (needCheckIdsList[index] == callId) {
                return true;
            }
        }
        return false;
    }

    private long[] getCallLogEntryIds(long newCallId) {
        if (mCallLogIds == null) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                long id = -1;
                try {
                    id = ContentUris.parseId(uri);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "getCallLogEntryIds(long): can NOT parse id from uri: "+uri, nfe);
                    return null;
                }
                mCallLogIds = new long[] { newCallId, id };
            } else {
                long[] ids = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);
                int length = ids == null ? 0 : ids.length;
                mCallLogIds = new long[length + 1];
                if (ids != null) {
                    System.arraycopy(ids, 0, mCallLogIds, 1, length);
                }
                mCallLogIds[0] = newCallId;
            }
        } else {
            int length = mCallLogIds.length;
            long[] tempIds = new long[length + 1];
            System.arraycopy(mCallLogIds, 0, tempIds, 1, length);
            tempIds[0] = newCallId;
            mCallLogIds = tempIds;
        }
        return mCallLogIds;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                // Make sure phone isn't already busy before starting direct
                // call
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                /* YUNOS BEGIN */
                // ##description:BugID:61122:
                // rootcause: mNumber may be null
                // solution: Add null protection for mNumber
                // ##date: 2013-11-5
                // ##author: fangjun.lin@aliyun-inc.com
                if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE && mNumber != null) {
                    /* YUNOS END */
                    startActivity(CallUtil.getCallIntent(this,Uri.fromParts(Constants.SCHEME_TEL, mNumber, null)));
                    return true;
                }
                break;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * check if record path is empty, update cache and database
     * @param callId
     * @param context
     * @return true if update empty record path
     */
    private boolean updateRecordPathIfEmpty(long callId, Context context) {
        String selection = " _id = " + callId;
        Cursor cursor = mCallLogManager.queryAliCalls(RECORD_PATH_PROJECTION, selection, null,
                Calls.DEFAULT_SORT_ORDER);
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                return false;
            }
            String recordPath = cursor.getString(0);
            if (!TextUtils.isEmpty(recordPath) && phoneRecordPathEmpty(recordPath)) {
                ContentValues values = new ContentValues(1);
                values.putNull(CallsTable.COLUMN_PHONE_RECORD_PATH);
                mCallLogManager.updateCallLog(values, CallsTable.COLUMN_ID + "=" + callId, null);
                return true;
            } else {
                return false;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean phoneRecordPathEmpty(String path) {
        File dir = new File(path);
        FilenameFilter recordFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".m4a") || filename.endsWith(".amr") || filename.endsWith(".3gpp");
            }
        };

        return !dir.exists() || !dir.isDirectory() || (dir.list(recordFilter).length == 0);
    }

    /**
     * Update user interface with details of given call.
     *
     * @param callUris URIs into {@link CallLog.Calls} of the calls to be
     *            displayed
     */
    private void updateData(final long[] callIds) {
        if ((callIds == null) && (mNumber == null)) {
            Log.w(TAG, "updateData: no uri or number to query, quit.");
            finish();
            return;
        }
        class UpdateContactDetailsTask extends AsyncTask<Void, Void, PhoneCallDetails[]> {
            @Override
            public PhoneCallDetails[] doInBackground(Void... params) {
                int numCalls = callIds == null ? 0 : callIds.length;
                PhoneCallDetails[] details;
                try {
                    // If we don't pass call ids as parameter to this activity,
                    // then we shall specify a number to query call logs.
                    // Otherwise, we can show nothing in this page.
                    details = getPhoneCallDetailsForNumberOrIds(numCalls == 0 ? mNumber : null, mCallLogIds);
                    if ((details == null) || (details.length == 0)) {
                        Log.w(TAG, "UpdateContactDetailsTask.doInBackground: can not query details.");
                        return null;
                    }
                    // maybe some call ids can not be found in query result,
                    // so make numCalls reflect the actual calls count.
                    numCalls = details.length;

                    if (mCallLogIds == null) {
                        mCallLogIds = new long[numCalls];
                    }
                    for (int index = 0; index < numCalls; ++index) {
                        // the mCallUri is constructed by alicontacts,
                        // so we can make sure there is no NumberFormatException.
                        long callId = ContentUris.parseId(details[index].callUri);
                        if (mCallLogIds[index] == 0) {
                            mCallLogIds[index] = callId;
                        }
                        if (updateRecordPathIfEmpty(callId, getApplicationContext())) {
                            details[index].mPhoneRecordPath = null;
                        }
                    }
                    return details;
                } catch (IllegalArgumentException e) {
                    // Something went wrong reading in our primary data.
                    Log.w(TAG, "UpdateContactDetailsTask.doInBackground: invalid URI starting call details", e);
                    return null;
                }
            }

            @Override
            public void onPostExecute(PhoneCallDetails[] details) {
                if ((details == null) || (details.length == 0)) {
                    finish();
                    return;
                }

                mLocation = details[0].numberInfo.location;
                PhoneCallDetails firstDetails = details[0];

                mNumber = firstDetails.number.toString();
                AliCallLogExtensionHelper.log(TAG, "onPostExecute mNumber " + AliTextUtils.desensitizeNumber(mNumber));
                //setup header info
                setUpHeaderInfo(firstDetails);

                setSpecialNumberLabel();
                mVideoCallButton.setVisibility(
                        isVideoEnabled() && AliCallLogExtensionHelper.canPlaceVolteVideoCallByNumber(mNumber)
                        ? View.VISIBLE : View.GONE);

                ListView historyList = (ListView) findViewById(R.id.history);
                CallDetailHistoryAdapter historyAdapter = new CallDetailHistoryAdapter(CallDetailActivity.this, mInflater,
                        mCallTypeHelper, details);
                historyList.setAdapter(historyAdapter);
            }
        }
        mAsyncTaskExecutor.submit(Tasks.UPDATE_PHONE_CALL_DETAILS, new UpdateContactDetailsTask());
    }

    private void setSpecialNumberLabel() {
        int resId = AliCallLogExtensionHelper.getSpecialNumber(mNumber);
        if(resId > 0) {
            mTvNumber.setText(resId);
        } else {
            mTvNumber.setText(ContactsUtils.formatPhoneNumberWithCurrentCountryIso(
                    mNumber, getApplicationContext()));
        }
    }

    private void setUpHeaderInfo(PhoneCallDetails firstDetails) {
        ContactInfo cachedContact = ContactInfoHelper.getInstance(getApplicationContext())
                .getAndCacheContactInfo(firstDetails.number.toString(), firstDetails.countryIso, firstDetails.contact);
        boolean isShowHeadIcon = false;
        if (TextUtils.isEmpty(cachedContact.name)) {
            if (!FeatureOptionAssistant.isInternationalSupportted() && cachedContact.lookupUri == null
                    && !TextUtils.isEmpty(firstDetails.numberInfo.shopName)) {
                mNameTextView.setText(firstDetails.numberInfo.shopName);
            } else {
                mNameTextView.setText(ContactsUtils.formatPhoneNumberWithCurrentCountryIso(
                        mNumber, getApplicationContext()));
            }
        } else {
            mNameTextView.setText(cachedContact.name);
            isShowHeadIcon = true;
        }
        mNameTextView.setSelected(true);

        if (!FeatureOptionAssistant.isInternationalSupportted()) {
            mTagName = firstDetails.numberInfo.tagName;
            mMarkedCount = firstDetails.numberInfo.markedCount;
            updateSubTitle(mTagName, mMarkedCount);
        }

        if (isShowHeadIcon) {
            mTextPortrait.setVisibility(View.GONE);
            if (cachedContact.photoUri == null) {
                String nameStr = ContactsTextUtils.getPortraitText(cachedContact.name);
                if (!ContactsTextUtils.STRING_EMPTY.equals(nameStr)) {
                    int color = ContactsTextUtils.getColor(nameStr);
                    //mHeaderIconView.setBackgroundResource(color);
                    //mActionBarView.setBackgroundResource(color);
                    //setSystemBar(color);
                    mTextPortrait.setText(nameStr);
                    mTextPortrait.setVisibility(View.VISIBLE);
                    //mPortraitBorder.setImageResource(R.drawable.contact_detail_avatar_border2);
                    Drawable drawable = getResources().getDrawable(R.drawable.portrait_background);
                    drawable.setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);
                    mTextPortrait.setBackground(drawable);
                } else {
                    mTextPortrait.setVisibility(View.GONE);
                    mImagePortrait.setImageResource(ContactPhotoManager.getDefaultAvatarResId(false, false));
                }
            } else {
                mTextPortrait.setVisibility(View.GONE);
                mContactPhotoManager.loadPhoto(mImagePortrait, Uri.parse(cachedContact.photoUri), mImagePortrait.getWidth(), true);
            }
            mHeaderIconView.setVisibility(View.VISIBLE);
        } else {
            mHeaderIconView.setVisibility(View.GONE);
        }
    }

    private String getCalllogSelectionWithCallIds(long[] callIds) {
        if (callIds != null && callIds.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(' ').append(CallerViewQuery.CALL_ID).append(" IN (");
            for (int i = 0; i < callIds.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('\'').append(callIds[i]).append('\'');
            }
            sb.append(')');
            return sb.toString();
        }
        return null;
    }

    private String getCalllogSelectionWithNumber(String number) {
        return CallerViewQuery.COLUMN_NUMBER + "=?";
    }

    /**
     * Query call details by given number or ids.
     * If the given numberToQuery is not null, then we query all call logs for the number.
     * If no number is given, then we query call logs specified by callIds.
     * @param numberToQuery
     * @param callIds
     * @return
     */
    private PhoneCallDetails[] getPhoneCallDetailsForNumberOrIds(String numberToQuery, long[] callIds) {
        int slotId = -1;
        String selection;
        String[] selectionArgs = null;
        if (TextUtils.isEmpty(numberToQuery)) {
            selection = getCalllogSelectionWithCallIds(callIds);
        } else {
            selection = getCalllogSelectionWithNumber(numberToQuery);
            selectionArgs = new String[] { numberToQuery };
        }
        Cursor callCursor = CallLogManager.getInstance(ActivityThread.currentApplication()).queryAliCalls(getCallLogDetailProjection(), selection, selectionArgs, Calls.DEFAULT_SORT_ORDER);
        ContactInfo contact = null;
        PhoneNumberInfo numberInfo = null;
        try {
            if (callCursor == null || !callCursor.moveToFirst()) {
                throw new IllegalArgumentException("Cannot find content: for "+selection);
            }
            /* YUNOS BEGIN */
            // ##description: BugID:5238992: improve query calllogs
            // ##date: 2014-11-12
            // ##author: changjun.bcj@alibaba-inc.com
            int count = callCursor.getCount();
            PhoneCallDetails[] phoneCalls = new PhoneCallDetails[count];
            for (int i = 0; i < count; i++) {
                // Read call log specifics.
                String number = callCursor.getString(CallDetailQuery.NUMBER_COLUMN_INDEX);
                long date = callCursor.getLong(CallDetailQuery.DATE_COLUMN_INDEX);
                long duration = callCursor.getLong(CallDetailQuery.DURATION_COLUMN_INDEX);
                int callType = callCursor.getInt(CallDetailQuery.CALL_TYPE_COLUMN_INDEX);
                int callFeatures = callCursor.getInt(CallDetailQuery.CALL_FEATURES_COLUMN_INDEX);
                if (SimUtil.MULTISIM_ENABLE) {
                    int subId = callCursor.getInt(CallDetailQuery.SUB_COLUMN_INDEX);
                    slotId = SimUtil.getSlotId(subId);
                }
                long ringTime = callCursor.getLong(CallDetailQuery.RING_TIME_COLUMN_INDEX);

                String countryIso = callCursor.getString(CallDetailQuery.COUNTRY_ISO_COLUMN_INDEX);
                long callsId = callCursor.getLong(CallDetailQuery.ID_COLUMN_INDEX);

                if (i == 0) {
                    contact = ContactInfo.fromLocalCallDetailCursor(callCursor);
                    numberInfo = PhoneNumberInfo.fromLocalCallDetailCursor(callCursor);
                    mContactUri = contact.lookupUri;
                    mIsAContact = (mContactUri != null);
                    setupHeaderIconClickListener();
                }

                PhoneCallDetails phoneCallDetails = new PhoneCallDetails(
                        Uri.withAppendedPath(Calls.CONTENT_URI, String.valueOf(callsId)), slotId,
                        number, countryIso, callType, callFeatures, date, duration, ringTime,
                        contact, numberInfo);

                String phoneRecordPath = callCursor.getString(CallDetailQuery.RECORD_PATH_COLUMN_INDEX);
                if (!TextUtils.isEmpty(phoneRecordPath)) {
                    phoneCallDetails.mPhoneRecordPath = phoneRecordPath;
                }
                phoneCalls[i] = phoneCallDetails;
                callCursor.moveToNext();
            }
            /* YUNOS END */
            return phoneCalls;
        } finally {
            if (callCursor != null) {
                callCursor.close();
            }
        }
    }

    private void setupHeaderIconClickListener() {
        if (mIsAContact) {
            mHeaderIconView.setOnClickListener(this);
        } else {
            mHeaderIconView.setOnClickListener(null);
        }
    }

    static final class ViewEntry {
        public final String text;
        public final Intent primaryIntent;
        /** The description for accessibility of the primary action. */
        public final String primaryDescription;

        public CharSequence label = null;
        /** Icon for the secondary action. */
        public int secondaryIcon = 0;
        /**
         * Intent for the secondary action. If not null, an icon must be
         * defined.
         */
        public Intent secondaryIntent = null;
        /** The description for accessibility of the secondary action. */
        public String secondaryDescription = null;

        public ViewEntry(String text, Intent intent, String description) {
            this.text = text;
            primaryIntent = intent;
            primaryDescription = description;
        }

        public void setSecondaryAction(int icon, Intent intent, String description) {
            secondaryIcon = icon;
            secondaryIntent = intent;
            secondaryDescription = description;
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onHomeSelected();
                return true;
            }
            // All the options menu items are handled by onMenu... methods.
            default:
                throw new IllegalArgumentException();
        }
    }

    private void configureActionBar() {
        findViewById(R.id.back_key).setOnClickListener(this);
        findViewById(R.id.pop_menu).setOnClickListener(this);
        mNameTextView = (TextView) findViewById(R.id.call_detail_name_id);
        mLocationTextView = (TextView) findViewById(R.id.call_detail_location_id);
    }

    /** Invoked when the user presses the home button in the action bar. */
    private void onHomeSelected() {
        /*
         * Intent intent = new Intent(Intent.ACTION_VIEW, Calls.CONTENT_URI); //
         * This will open the call log even if the detail view has been opened
         * directly. intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
         * startActivity(intent);
         */
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SimContactUtils.unObserveVolteAttachChangedByPlatform(getApplicationContext());
        UsageReporter.onPause(this, null);
    }

    private void closeSystemDialogs() {
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    private String[] getCallLogDetailProjection() {
        if(SimUtil.MULTISIM_ENABLE) {
            return CallDetailQuery.CALL_LOG_PROJECTION_MULTISIM;
        }else {
            return CallDetailQuery.CALL_LOG_PROJECTION;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deinitListeners();
        mPopupDialog = null;
        if (mConfirmDeleteDialog != null) {
            mConfirmDeleteDialog.dismiss();
            mConfirmDeleteDialog = null;
        }
        AccountFilterManager.getInstance(this).dismissAccountSelectDialog();
    }

    private void deinitListeners() {
        mCallLogManager.unRegistCallsTableChangeListener(mCallLogChangeListener);
        CallLogAdapter.setCursorDataChangeListener(null);
    }

    private void addNewContact() {
        AccountFilterManager.getInstance(this)
                .createNewContactWithPhoneNumberOrEmailAsync(
                        this,
                        mNumber,
                        null,
                        AccountFilterManager.INVALID_REQUEST_CODE);
    }

    private void mark() {
        UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.CLICK_MARK_STRANGE_CALL);
        popupMarkDialog();
    }

    private void popupMarkDialog() {
        Intent intent = new Intent(CallDetailTagDialogActivity.ACTION_MARK_TAG);
        intent.setClass(getApplicationContext(), CallDetailTagDialogActivity.class);
        intent.putExtra(CallDetailTagDialogActivity.EXTRA_KEY_NUMBER, mNumber);
        intent.putExtra(CallDetailTagDialogActivity.EXTRA_KEY_TAG_NAME, mTagName);
        intent.putExtra(CallDetailTagDialogActivity.EXTRA_KEY_MARKED_COUNT, mMarkedCount);
		//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
		if(BirdFeatureOption.BIRD_DOOV_INCALL_MARK_NUMBER){
        String srcActName="CallDetailActivity";
        intent.putExtra("SrcActName", srcActName);
		}
		//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
        this.startActivity(intent);
    }

    private void showRemoveDialog() {
        if(mIdsBuffer == null) {
            mIdsBuffer = new StringBuffer();
        } else {
            mIdsBuffer.delete(0, mIdsBuffer.length());
        }
        for (long callLogID : mCallLogIds) {
            if (callLogID > 0) {
                mIdsBuffer.append(callLogID).append(',');
            }
        }
        mIdsBuffer.deleteCharAt(mIdsBuffer.length() - 1);

        AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setMessage(getString(R.string.confirm_delete));
        build.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AsyncTask<String, String, Integer>() {

                    @Override
                    protected Integer doInBackground(String... params) {
                        DebugLog.d(TAG, "doInBackground:");
                        int deleteCount = getApplicationContext().getContentResolver().delete(Calls.CONTENT_URI,
                                Calls._ID + " IN (" + params[0] + ")", null);
                        UsageReporter.onClick(CallDetailActivity.this, null,
                                UsageReporter.DialpadPage.DP_DELETE_CALLLOG_FROM_DETAIL);
                        return deleteCount;
                    }

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        DebugLog.d(TAG, "onPreExecute:");
                        // mProgressDialog = new
                        // ProgressDialog(CallDetailActivity.this);
                        // mProgressDialog.setCancelable(false);
                        // mProgressDialog
                        // .setMessage(getString(R.string.calllog_delete_dialog_msg));
                        // mProgressDialog
                        // .setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        // mProgressDialog.show();
                    }

                    @Override
                    protected void onPostExecute(Integer result) {
                        super.onPostExecute(result);
                        DebugLog.d(TAG, "onPostExecute:");
                        mIdsBuffer.delete(0, mIdsBuffer.length());
                        // if (mProgressDialog != null/*
                        // * &&
                        // * mProgressDialog
                        // * .
                        // * isShowing(
                        // * ) FIXME by
                        // * interface
                        // * xgy
                        // */) {
                        // // mProgressDialog.cancel();
                        // mProgressDialog.dismiss();
                        // mProgressDialog = null;
                        // }

                        if (result <= 0) {
                            Toast.makeText(getApplicationContext(), getString(R.string.calllog_delete_fail), Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.calllog_delete_success),
                                    Toast.LENGTH_SHORT).show();
                            CallDetailActivity.this.finish();
                        }
                        // mCallLogAdapter.forceUpdateData();
                    }
                    /* YUNOS BEGIN */
                    // ##description: BugID:59477: rootcause:
                    // android4.0之后的AysncTask.execute是串行运行的，
                    // 没有并行执行，当后台有线程在跑的时候，当前的AsyncTask会等待直到前面的线程执行完成。所以应该让AsyncTask以并行方式执行。
                    // ##date: 2013-10-30
                    // ##author: fangjun.lin@aliyun-inc.com
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mIdsBuffer.toString());
                /* YUNOS END */
            }
        });
        build.setNegativeButton(R.string.no, null);
        mConfirmDeleteDialog = build.create();
        mConfirmDeleteDialog.show();
        mConfirmDeleteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    protected boolean[] mChooseType = new boolean[] {
            true, true
    };

    private void handleBlackList(){
        if(AliCallLogExtensionHelper.PLATFORM_YUNOS) {
            boolean result = false;
            if(isInBlackList) {
                result = YunOSFeatureHelper.removeBlack(this, mNumber);
                if (result) {
                    isInBlackList = false;
                    Toast.makeText(getApplicationContext(), R.string.contact_detail_removeFromBlackListOK,
                            Toast.LENGTH_SHORT).show();
                    updateBlackListMenuItem();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.contact_detail_removeFromBlackListFail,
                            Toast.LENGTH_SHORT).show();
                }
                UsageReporter.onClick(this, null, UsageReporter.DialpadPage.DP_BLACK_DELETE_FROM_DETAIL);
            } else {
                int type = 3;// type 3: block call and sms
                result = YunOSFeatureHelper.addBlack(this, mNumber, type);
                if (result) {
                    isInBlackList = true;
                    Toast.makeText(getApplicationContext(), R.string.contact_detail_addToBlackListOK,
                            Toast.LENGTH_SHORT).show();
                    updateBlackListMenuItem();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.contact_detail_addToBlackListFail,
                            Toast.LENGTH_SHORT).show();
                }
                UsageReporter.onClick(this, null, UsageReporter.DialpadPage.DP_BLACK_ADD_FROM_DETAIL);
            }
        }
    }

    private void addToExistingContact() {
        Intent createIntent2 = ContactsUtils.getInsertContactIntent(getApplicationContext(), ContactSelectionActivity.class,
                mNumber);
        startActivity(createIntent2);
    }

    private void makeCall() {
        if (AliCallLogExtensionHelper.PLATFORM_YUNOS) {
            AliCallLogExtensionHelper.makeCall(getApplicationContext(), mNumber, SimUtil.INVALID_SLOT_ID);
        } else {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", mNumber, null));
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View view) {
        if(mNumber == null) {
            return;
        }
        switch (view.getId()) {
            case R.id.back_key:
                finish();
                break;
            case R.id.pop_menu:
                initPopMenu();
                mPopMenu.show();
                break;
            case R.id.calllog_detail_sms:
                AliCallLogExtensionHelper.makeSms(this,mNumber);
                UsageReporter.onClick(this, null, UsageReporter.DialpadPage.DP_SMS_FROM_DETAIL);
                break;
            case R.id.calllog_detail_call:
            case R.id.calllog_detail_number:
                if ((mPopupDialog != null) && mPopupDialog.isShowing()) {
                    Log.i(TAG, "onClick: popup for number is showing.");
                    break;
                }
                AliCallLogExtensionHelper.makeCall(getApplicationContext(), mNumber, SimUtil.INVALID_SLOT_ID);

                UsageReporter.onClick(this, null, UsageReporter.DialpadPage.DP_MO_FROM_DETAIL);
                break;
            case R.id.navigate_detail_activity_btn:
                if (mContactUri != null) {
                    Intent intent2 = ContactsUtils.getViewContactIntent(getApplicationContext(), ContactDetailActivity.class,
                            Uri.parse(mContactUri));
                    startActivity(intent2);
                }
                break;
            case R.id.calllog_detail_videocall:
                Intent videoCallIntent = null;
                if (isVideoEnabled() && AliCallLogExtensionHelper.canPlaceVolteVideoCallByNumber(mNumber)) {
                    videoCallIntent = CallUtil.getVideoCallIntent(getApplicationContext(), mNumber, CALL_ORIGIN_CALL_DETAIL_ACTIVITY);
               /*YunOS BEGIN PB*/
              //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
              //##BugID:(8438185) ##date:2016-6-23 09:00
              //##description:intent null exception
                    try {
                        startActivity(videoCallIntent);
                    } catch (Exception e) {
                        Log.d(TAG, "onClick() exception ,e:"+e);
                    }


                /*YUNOS END PB*/
                }
                UsageReporter.onClick(this, null, UsageReporter.DialpadPage.DP_VIDEO_CALL_FROM_DETAIL);
                break;
            default:
                Log.d(TAG, "onClick(), case default, do nothing!");
                break;
        }
    }

    private void updateSubTitle(String tagName, int markedCount) {
        StringBuffer sb = new StringBuffer();
        if (!TextUtils.isEmpty(mLocation)) {
            sb.append(mLocation);
        }

        // Set tagType view
        if (!mIsAContact) {
            if (!TextUtils.isEmpty(tagName)) {
                if (!TextUtils.isEmpty(sb.toString())) {
                    sb.append(CallLogAdapter.TAG_DIVIDER);
                }
                if (markedCount > 1) {
                    tagName = getString(R.string.system_tag_type_parameter, markedCount, tagName);
                }
                sb.append(tagName);
            }
        }

        mLocationTextView.setText(sb.toString());
        if (sb.length() > 0) {
            mLocationTextView.setVisibility(View.VISIBLE);
        } else {
            mLocationTextView.setVisibility(View.GONE);
        }
    }

    private static final int MSG_VOLTE_ATTACH_STATE_CHANGED = 100;
    private boolean mIsStayVolte = false;

    // TODO: platform dependent logic shall be put in SimContactUtils.
    private boolean isVideoEnabled() {
        final Context context = getApplicationContext();
        if (SimUtil.IS_PLATFORM_MTK || SimUtil.IS_PLATFORM_QCOMM) {
            return SimUtil.isVideoCallEnabled(context);
        } else if (SimUtil.IS_PLATFORM_SPREADTRUM) {
            return mIsStayVolte && SimUtil.isVideoCallEnabled(context);
        }
        return false;
    }

    private void updateVideoCallButton() {
        final boolean isVideoEnabled = isVideoEnabled();
        final boolean canVideoCallToNumber = AliCallLogExtensionHelper.canPlaceVolteVideoCallByNumber(mNumber);
        Log.i(TAG, "updateVideoCallButton: video enabled="+isVideoEnabled+"; canVideoCallToNumber="+canVideoCallToNumber);
        mVideoCallButton.setVisibility(isVideoEnabled && canVideoCallToNumber ? View.VISIBLE : View.GONE);
    }

    private VolteAttachHandler mVolteAttachHandler = new VolteAttachHandler(this);
    private static class VolteAttachHandler extends Handler {
        final CallDetailActivity mActivity;
        public VolteAttachHandler(CallDetailActivity activity) {
            mActivity = activity;
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_VOLTE_ATTACH_STATE_CHANGED) {
                mActivity.mIsStayVolte = msg.arg2 == 1;
                mActivity.updateVideoCallButton();
            }
        }
    }

}
