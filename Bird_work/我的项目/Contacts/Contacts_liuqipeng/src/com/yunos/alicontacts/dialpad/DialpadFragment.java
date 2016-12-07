
package com.yunos.alicontacts.dialpad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog.Calls;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yunos.alicontacts.CallUtil;
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.SpecialCharSequenceMgr;
import com.yunos.alicontacts.activities.ActionBarAdapter2.TabState;
import com.yunos.alicontacts.activities.ContactSelectionActivity;
import com.yunos.alicontacts.activities.PeopleActivity2;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.dialpad.calllog.CallLogAdapter;
import com.yunos.alicontacts.dialpad.smartsearch.ContactsSearchEngine;
import com.yunos.alicontacts.dialpad.smartsearch.NameConvertWorker;
import com.yunos.alicontacts.dialpad.smartsearch.OnQueryCompleteListener;
import com.yunos.alicontacts.dialpad.smartsearch.PinyinSearchStateChangeListener;
import com.yunos.alicontacts.dialpad.smartsearch.SearchResult;
import com.yunos.alicontacts.list.AccountFilterManager;
import com.yunos.alicontacts.sim.ICrossPlatformSim;
import com.yunos.alicontacts.sim.SimStateReceiver;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.Constants;
import com.yunos.alicontacts.widget.AutoScrollListView;
import com.yunos.common.UsageReporter;
import com.yunos.yundroid.widget.ItemAdapter;
import com.yunos.yundroid.widget.item.HeaderTextItem;
import com.yunos.yundroid.widget.item.TextItem;

import dalvik.system.VMRuntime;

import yunos.support.v4.app.Fragment;

import java.util.concurrent.atomic.AtomicBoolean;

public class DialpadFragment extends Fragment implements CallLogAdapter.DialListener,
        DialpadFooterView.OnDailpadFooterBarViewClickListener, OnQueryCompleteListener, PinyinSearchStateChangeListener {

    public static final String TAG = "DialpadFragment";
    public static final String CLASSNAME = "DialpadFragment";

    private static final int FUNCITION_SAVE_AS_NEW_CONTACT_ID = 0;
    private static final int FUNCITION_SAVE_AS_EXIST_CONTACT_ID = 1;
    private static final int FUNCITION_MAKE_VIDEO_CALL_ID = 2;
    private static final int FUNCITION_SEND_MESSAGE_ID = 3;
    private static final int FUNCITION_ITEM_INDEX_MAKE_VIDEO_CALL = 2;

    private static final int VIEW_EMPTY = 1;
    private static final int VIEW_SEARCH_RESULT = 2;
    private static final int VIEW_FUNCTION_LIST = 3;

    /**
     * Animation that slides in.
     */
    private Animation mSlideIn;

    /**
     * Animation that slides out.
     */
    private Animation mSlideOut;
    private boolean mIsDialpadShow = true;

    // Flag for clear Phone number after dial
    private boolean mClearPhoneNumberFlag = false;

    private Handler mHandler = new Handler();

    private RelativeLayout mEditorField;
    private EditText mEditText;
    private ImageView mDeleteButton;
    private AutoScaleTextSizeWatcher mAutoScaleTextSizeWatcher;

    private LinearLayout mListContainer;
    private LinearLayout mTopLayout;
    private FrameLayout mListFrameLayout;
    private LinearLayout mDialpadContainerLayout;
    private DialpadView mDialpadView;
    private TextView mTopTextView;

    private String mCachedPhoneNumber;

    private boolean mFlightMode;

    private boolean mMultiSimMode = SimUtil.MULTISIM_ENABLE;
    private AtomicBoolean mSim1Enabled;
    private AtomicBoolean mSim2Enabled;
    private AtomicBoolean mSimAvailable;
    private AtomicBoolean mSim1Available;
    private AtomicBoolean mSim2Available;

    // search
    private ContactsSearchEngine mSmartSearch;
    private MatchContactsListAdapter mMatchContactsListAdapter;
    private ListView mFunctionListView;
    private ItemAdapter mFunctionListAdapter;
    private TextItem mVideoItem;
    private AutoScrollListView mListViewForSearch;

    private class DialpadSimListener
            implements SimStateReceiver.SimStateListener {
        @Override
        public void onSimStateChanged(int slot, String state) {
            updateDialButton();
            if (SimUtil.DUMMY_SLOT_FOR_AIRPLANE_MODE_ON == slot) {
                mFlightMode = true;
            } else if (SimUtil.DUMMY_SLOT_FOR_AIRPLANE_MODE_OFF == slot) {
                mFlightMode = false;
            } else if ((SimUtil.SLOT_ID_1 == slot) && (mPhone1Monitor != null)) {
                mPhone1Monitor.refreshListen();
            } else if ((SimUtil.SLOT_ID_2 == slot) && (mPhone2Monitor != null)) {
                mPhone2Monitor.refreshListen();
            }
            updateEditTextInfo();
        }
    }

    /**
     * This class monitors the phone state change on specified slot.
     * Because the PhoneStateListener only listen for given subId.
     * The subId can be changed when we hot remove a SIM and insert another.
     * So we have to listen another subId when we detect a change on subId.
     * Methods refreshListen() and stopListen() must be called from main thread.
     */
    private class DialpadPhoneStateMonitor {

        private final TelephonyManager mTM;
        private final int mSlotId;
        private int mListenSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        private PhoneStateListener mPhoneStateListener = null;
        public DialpadPhoneStateMonitor(TelephonyManager tm, int slotId) {
            mTM = tm;
            mSlotId = slotId;
        }

        public void refreshListen() {
            boolean hasIccCard = SimUtil.MULTISIM_ENABLE ?
                    SimUtil.hasIccCard(mSlotId) : SimUtil.hasIccCard();
            if (!hasIccCard) {
                Log.i(TAG, "DialpadPhoneStateMonitor.refreshListen: no card for slot "+mSlotId);
                cancelListen();
                mListenSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                return;
            }
            int subId = SimUtil.MULTISIM_ENABLE ?
                    SimUtil.getSubId(mSlotId) : SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
            if ((mListenSubId == subId) && (mPhoneStateListener != null)) {
                Log.i(TAG, "DialpadPhoneStateMonitor.refreshListen: quit for already listen slot="+mSlotId+"; sub="+mListenSubId);
                return;
            }
            cancelListen();
            mListenSubId = subId;
            Log.i(TAG, "DialpadPhoneStateMonitor.refreshListen: create listener for slot="+mSlotId+"; sub="+mListenSubId);
            // TODO: need to test on single SIM phone to check if default sub id works.
            mPhoneStateListener = new PhoneStateListener(mListenSubId) {
                private static final int INVALID_VOICE_REG_STATE = -1;
                private int mLastVoiceRegState = INVALID_VOICE_REG_STATE;
                @Override
                public void onServiceStateChanged(ServiceState state) {
                    super.onServiceStateChanged(state);
                    int voiceRegState = state == null ? INVALID_VOICE_REG_STATE : state.getVoiceRegState();
                    Log.i(TAG, "onServiceStateChanged: slot="+mSlotId+"; voiceRegState="+voiceRegState+"; mLastVoiceRegState="+mLastVoiceRegState);
                    if (voiceRegState == mLastVoiceRegState) {
                        return;
                    }
                    mLastVoiceRegState = voiceRegState;
                    updateDialButton();
                    updateEditTextInfo();
                }
            };
            mTM.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }

        public void stopListen() {
            cancelListen();
        }

        private void cancelListen() {
            if (mPhoneStateListener != null) {
                Log.i(TAG, "DialpadPhoneStateMonitor.cancelListen: remove existing listen on slot="+mSlotId+"; sub="+mListenSubId);
                mTM.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener = null;
            }
        }

    }

    private DialpadSimListener mSimStateChangeListener = new DialpadSimListener();
    private DialpadPhoneStateMonitor mPhone1Monitor = null;
    private DialpadPhoneStateMonitor mPhone2Monitor = null;

    AnimationListener mSlideOutListener = new AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        };

        @Override
        public void onAnimationRepeat(Animation animation) {
        };

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mDialpadContainerLayout != null) {
                mDialpadContainerLayout.setVisibility(View.GONE);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        registerSimAndPhoneListeners(activity);

        mMatchContactsListAdapter = new MatchContactsListAdapter(activity);
        mMatchContactsListAdapter.setShowLongClickActionMenuListener(mShowLongListener);
        mMatchContactsListAdapter.setOnCallLogDeletedListener(new MatchContactsListAdapter.CallLogDeletedListener() {
            @Override
            public void onCallLogDeleteFinish(final String phoneNumber) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        activity.getContentResolver().delete(Calls.CONTENT_URI,
                                Calls.NUMBER + "=?", new String[] { phoneNumber });
                        if (mDialpadView == null) {
                            Log.w(TAG, "onCallLogDeleteFinish: mDialpadView is null.");
                            return;
                        }
                        String number = mDialpadView.getPhoneNumber();
                        if (TextUtils.isEmpty(number)) {
                            return;
                        }
                        mHandler.post(mTextChangeRunable);
                    }
                }).start();
            }
        });
        mMatchContactsListAdapter.setDialListener(this);
        mMatchContactsListAdapter.setOnSpeedDialListener(mSpeedDialListener);

        mSim1Enabled = new AtomicBoolean(false);
        mSim2Enabled = new AtomicBoolean(false);
        mSimAvailable = new AtomicBoolean(false);
        mSim1Available = new AtomicBoolean(false);
        mSim2Available = new AtomicBoolean(false);

        if (mSmartSearch == null) {
            mSmartSearch = new ContactsSearchEngine(activity, DialpadFragment.this, DialpadFragment.this);
        }
        initAnimation();
    };

    private void registerSimAndPhoneListeners(Activity activity) {
        SimStateReceiver.registSimStateListener(mSimStateChangeListener);
        TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        mPhone1Monitor = new DialpadPhoneStateMonitor(tm, SimUtil.SLOT_ID_1);
        mPhone1Monitor.refreshListen();
        if (SimUtil.MULTISIM_ENABLE) {
            mPhone2Monitor = new DialpadPhoneStateMonitor(tm, SimUtil.SLOT_ID_2);
            mPhone2Monitor.refreshListen();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.ali_dialpad_fragment, container, false);
        v.findViewById(R.id.dialpad_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialpad(true);
            }
        });

        mListContainer = (LinearLayout) v.findViewById(R.id.list_container);
        mTopLayout = (LinearLayout) v.findViewById(R.id.top_input_field);
        mListFrameLayout = (FrameLayout) v.findViewById(R.id.list_layout);
        mDialpadContainerLayout = (LinearLayout) v.findViewById(R.id.dialpad_container_id);
        mTopTextView = (TextView) v.findViewById(R.id.top_number_text_view);
        mEditorField = (RelativeLayout) v.findViewById(R.id.input_field);
        mEditText = (EditText) v.findViewById(R.id.input_edit_view);
        mDeleteButton = (ImageView) v.findViewById(R.id.btn_delete);

        initDialpadView(v);
        initEditorField();

        if (!TextUtils.isEmpty(mCachedPhoneNumber)) {
            setPhoneNumber(mCachedPhoneNumber);
            mCachedPhoneNumber = null;
        }
        return v;
    }

    private void initDialpadView(View fragmentView) {
        mDialpadView = (DialpadView) fragmentView.findViewById(R.id.dialpad_view);
        fragmentView.findViewById(R.id.back_dialpad_id).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    ((PeopleActivity2) getActivity()).hideDialpadFragment();
                }
            }
        });
        fragmentView.findViewById(R.id.dialpad_hidden).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (getActivity() != null && TextUtils.isEmpty(mDialpadView.getPhoneNumber())) {
                    ((PeopleActivity2) getActivity()).hideDialpadFragment();
                } else {
                    hiddenDialpad(true);
                }
            }
        });

        mDialpadView.setParentFragment(this);
        mDialpadView.createDailpadView();
        mDialpadView.setOnDailpadFooterBarViewClickListener(this);
        mDialpadView.setEditTextView(mEditText);
        //mDialpadView.setAutoScaleTextSizeWatcher(mAutoScaleTextSizeWatcher);
        mDialpadView.setOnSpeedDialListener(mSpeedDialListener);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() != null && getActivity().getIntent() != null && TextUtils.isEmpty(mDialpadView.getPhoneNumber())) {
            Intent intent = getActivity().getIntent();
            Uri uri = intent.getData();
            if (uri != null && Constants.SCHEME_TEL.equals(uri.getScheme())) {
                String data = uri.getSchemeSpecificPart();
                if (!TextUtils.isEmpty(data)) {
                    setPhoneNumber(data);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSimAndPhoneListeners();
        clearSearchItemLongClickMenu();
        if (mSmartSearch != null) {
            mSmartSearch.destroy();
            mSmartSearch = null;
        }
    }

    private void unregisterSimAndPhoneListeners() {
        SimStateReceiver.unregistSimStateListener(mSimStateChangeListener);
        if (mPhone1Monitor != null) {
            mPhone1Monitor.stopListen();
        }
        if (mPhone2Monitor != null) {
            mPhone2Monitor.stopListen();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAirPlaneMode();
        updateDialButton();
        updateEditTextInfo();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Flag for dial and clean phone number,
        // clean it and hide dialpad fragment on onStop() phase.
        if (mClearPhoneNumberFlag) {
            setPhoneNumber("");
            Activity activity = getActivity();
            if (activity instanceof PeopleActivity2) {
                ((PeopleActivity2) activity).hideDialpadFragment();
                ((PeopleActivity2) activity).selectTabToOpen(TabState.CALLLOG, true);
            }
            mClearPhoneNumberFlag = false;
        }

    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        if (mDialpadView != null) {
//            mDialpadView.releaseToneGenerator();
//        }
//    }

    @Override
    public void onSearchTableChanged() {
        if (mDialpadView == null) {
            Log.w(TAG, "onSearchTableChanged: mDialpadView is null.");
            return;
        }
        mHandler.post(mTextChangeRunable);
    }

    @Override
    public void onFooterBarClick(int id) {
        switch (id) {
            case ID_BTN_SYNC:
                syncCallLog();
                return;
            case ID_BTN_SWITCH:
                showDialpad(true);
                return;
            case ID_BTN_SIM:
                call(-1);
                return;
            case ID_BTN_SIM1:
                call(SimUtil.SLOT_ID_1);
                return;
            case ID_BTN_SIM2:
                call(SimUtil.SLOT_ID_2);
                return;
        }
    }

    @Override
    public void callNumberDirectly(String number) {
        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "onListViewItemClicked: activity is null, maybe quiting.");
            return;
        }

        // here should not check sim state, always use Setting's default card to call.
        AliCallLogExtensionHelper.makeCall(activity.getApplicationContext(), number, SimUtil.INVALID_SLOT_ID);
        mClearPhoneNumberFlag = true;
    }

    @Override
    public void editBeforeCall(String number) {
        if (mEditText != null && mEditorField != null) {
            this.mEditText.setText(number);
            mEditorField.requestFocus();
            mEditText.setCursorVisible(true);
            mEditText.setSelection(0);
            showDialpad(true);
        }
    }

    private Runnable mTextChangeRunable = new Runnable() {

        @Override
        public void run() {
            phoneNumberChanged(getPhoneNumber());
        }
    };

    private CallLogAdapter.ShowLongClickActionMenuListener mShowLongListener = new CallLogAdapter.ShowLongClickActionMenuListener() {
        @Override
        public void onShowMenu() {
            hiddenDialpad(true);
        }
    };

    private void call(int slotId) {
        if (mDialpadView == null) {
            Log.w(TAG, "call: mDialpadView is null.");
            return;
        }

        if (TextUtils.isEmpty(mDialpadView.getPhoneNumber())) {
            String number = "";
            if (getActivity() != null && getActivity() instanceof PeopleActivity2) {
                CallLogFragment callLogFragment = ((PeopleActivity2) getActivity()).getCallLogFragment();
                number = callLogFragment.getLastCallLog();
            }

            if (TextUtils.isEmpty(number)) {
                return;
            } else {
                setPhoneNumber(number);
                return;
            }
        }

        // send usage report
        switch (slotId) {
            case 0:
                UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.DP_MO_FROM_SIMCARD1);
                break;
            case 1:
                UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.DP_MO_FROM_SIMCARD2);
                break;
            default:
                UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.DP_MO_FROM_SIMCARD);
                break;
        }

        dialButtonPressed(slotId);
        return;
    }

    private void dialButtonPressed(int slotId) {
        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "dialButtonPressed: activity is null. maybe quiting.");
            return;
        }
        Context appContext = activity.getApplicationContext();
        if (mDialpadView == null) {
            Log.w(TAG, "dialButtonPressed: mDialpadView is null.");
            return;
        }
        String number = mDialpadView.getPhoneNumber();
        AliCallLogExtensionHelper.makeCall(appContext, number, slotId);

        mClearPhoneNumberFlag = true;
    }

    private void syncCallLog() {
        Intent intent = new Intent("com.aliyun.xiaoyunmi.action.SELECT_SYNC");
        intent.setData(Uri.parse("yunmi://sync_select/?type=calllog"));
        intent.putExtra("wificheck", false);
        intent.putExtra("synctype", "backup");
        startActivity(intent);
    }

    private void makeVideoCall() {
        Activity activity = getActivity();
        if ((mDialpadView != null) && (activity != null)) {
            Intent videoCallIntent = CallUtil.getVideoCallIntent(
                    activity, mDialpadView.getPhoneNumber(), activity.getClass().getCanonicalName());
            getActivity().startActivity(videoCallIntent);
            UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.DP_LC_MAKE_VIDEO_CALL);
            mClearPhoneNumberFlag = true;
        }
    }

    private void sendSms() {
        if (mDialpadView != null) {
            AliCallLogExtensionHelper.makeSms(getActivity(), mDialpadView.getPhoneNumber());
            UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.DP_SMS_FROM_SEARCH);
        }
    }

    private void addToExistingContact() {
        if (mDialpadView != null) {
            Intent createIntent2 = ContactsUtils.getInsertContactIntent(getActivity(), ContactSelectionActivity.class,
                    mDialpadView.getPhoneNumber());
            startActivity(createIntent2);
            UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.DP_ADD_EXISTING_CONTACT_FROM_SEARCH);
        }
    }

    private void addNewContact() {
        String phoneNumber = mDialpadView == null ? null : mDialpadView.getPhoneNumber();
        if (!TextUtils.isEmpty(phoneNumber)) {
            Activity activity = getActivity();
            if (activity != null) {
                AccountFilterManager.getInstance(activity)
                        .createNewContactWithPhoneNumberOrEmailAsync(
                                activity,
                                phoneNumber,
                                null,
                                AccountFilterManager.INVALID_REQUEST_CODE);
            }
            UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.DP_ADD_CONTACT_FROM_SEARCH);
        }
    }

    @Override
    public void onQueryComplete(SearchResult result) {
        if (mDialpadView == null) {
            Log.w(TAG, "onQueryComplete: mDialpadView is null.");
            return;
        }
        if (getActivity() == null) {
            Log.w(TAG, "onQueryComplete: getActivity is null.");
            return;
        }

        try {
            if (result == null || result.getResultCount() == 0) {
                if (mDialpadView.getVisibility() == View.VISIBLE && mDialpadView.getPhoneNumber().length() > 0) {
                    showView(VIEW_FUNCTION_LIST);
                } else {
                    showView(VIEW_SEARCH_RESULT);
                }
                mMatchContactsListAdapter
                        .setInputStringAndMatchResult(mDialpadView.getPhoneNumber(), SearchResult.EMPTY_RESULT);
                mMatchContactsListAdapter.notifyDataSetChanged();
            } else {
                showView(VIEW_SEARCH_RESULT);
                if (!result.mSearchText.equals(mMatchContactsListAdapter.getSearchResult().mSearchText)) {
                    mListViewForSearch.requestPositionToScreen(0, false);
                }
                mMatchContactsListAdapter.setInputStringAndMatchResult(mDialpadView.getPhoneNumber(), result);
                mMatchContactsListAdapter.notifyDataSetChanged();
            }

            VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);
        } catch (IllegalStateException ise) {
            Log.e("Rufeng", ise.getLocalizedMessage(), ise);
        }
    }

    private void initEditorField() {
        final Context context = this.getActivity();
        if (context == null) {
            Log.w(TAG, "initEditorField: getActivity() is null.");
            return;
        }
        if (mEditText == null || mDeleteButton == null) {
            Log.w(TAG, "initEditorField: mEditText is null or mDeleteButton is null.");
            return;
        }
        Resources r = getResources();
        mAutoScaleTextSizeWatcher = new AutoScaleTextSizeWatcher(mEditText);
        mAutoScaleTextSizeWatcher.setAutoScaleParameters(r.getDimensionPixelSize(R.dimen.dialpad_digits_text_size_min),
                r.getDimensionPixelSize(R.dimen.dialpad_digits_text_size),
                r.getDimensionPixelSize(R.dimen.dialpad_digits_text_size_delta),
                r.getDimensionPixelSize(R.dimen.dialpad_digits_width));

        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                phoneNumberChanged(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        mEditText.setCursorVisible(false);
        mEditText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mEditText != null) {
                    mEditText.setCursorVisible(true);
                }
            }
        });

        mEditText.addTextChangedListener(mAutoScaleTextSizeWatcher);

        /*
         * if (!SweetNumberUtil.supportSweetNumber(getActivity())) {
         * mDeleteButton.setOnClickListener(new OnClickListener() {
         * @Override public void onClick(View v) {
         * mAutoScaleTextSizeWatcher.trigger(true); clearEditText();
         * updateEditTextInfo(); } }); mDeleteButton.setPadding(0, 0, 0, 0); }
         * else {
         */
        mDeleteButton.setImageResource(R.drawable.dial_delete_button);
        mDeleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAutoScaleTextSizeWatcher != null) {
                    mAutoScaleTextSizeWatcher.trigger(true);
                }
                if (mDialpadView != null) {
                    mDialpadView.deleteButtonClicked();
                } else {
                    Log.d(TAG, "mDeleteButton.setOnLongClickListener dialpadview is null.");
                }
            }
        });

        mDeleteButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mDialpadView != null) {
                    mDialpadView.deleteButtonLongClicked();
                } else {
                    Log.d(TAG, "mDeleteButton.setOnLongClickListener dialpadview is null.");
                }
                return true;
            }
        });
        // }
    }

    private void showView(int which) {
        if (which == VIEW_EMPTY) {
            if (mListViewForSearch != null) {
                mListViewForSearch.setVisibility(View.GONE);
            }
            if (mFunctionListView != null) {
                mFunctionListView.setVisibility(View.GONE);
            }
            return;
        }
        initSearchListView();
        initFunctionList();
        if (which == VIEW_SEARCH_RESULT) {
            mListViewForSearch.setVisibility(View.VISIBLE);
        } else {
            mListViewForSearch.setVisibility(View.GONE);
        }
        if (which == VIEW_FUNCTION_LIST) {
            updateFunctionList();
            mFunctionListView.setVisibility(View.VISIBLE);
        } else {
            mFunctionListView.setVisibility(View.GONE);
        }
    }

    private View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return null;
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    private void initSearchListView() {
        if (mListViewForSearch != null) {
            return;
        }
        mListViewForSearch = new AutoScrollListView(getActivity());
        ViewGroup.LayoutParams lpList = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mListViewForSearch.setBackgroundResource(R.color.dialpad_calllog_background);
        mListViewForSearch.setLayoutParams(lpList);
        //mListViewForSearch.setOverScrollMode(ListView.OVER_SCROLL_NEVER);
        mListViewForSearch.setAdapter(mMatchContactsListAdapter);
        mListViewForSearch.setOnItemLongClickListener(mMatchContactsListAdapter.mItemLongClickListener);
        mListViewForSearch.setOnItemClickListener(mMatchContactsListAdapter.itemClickListener);
        mListViewForSearch.setOnScrollListener(mOnScrollListener);
        mListViewForSearch.setDivider(getResources().getDrawable(R.drawable.listview_margin_divider, null));
        mListViewForSearch.setDividerHeight(1);
        mListViewForSearch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int count = mMatchContactsListAdapter.getCount();
                View v = getViewByPosition(count - 1, mListViewForSearch);
                if (count == 0 || v != null && event.getAction() == MotionEvent.ACTION_MOVE && event.getY() > (v.getBottom())) {
                    hiddenDialpad(true);
                    return true;
                }
                return false;
            }
        });
        mListFrameLayout.addView(mListViewForSearch);
    }

    private void initFunctionList() {
        if (mFunctionListView != null) {
            return;
        }
        Activity activity = getActivity();
        mFunctionListView = new ListView(activity);
        ViewGroup.LayoutParams lpList = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mFunctionListView.setLayoutParams(lpList);
        mFunctionListView.setBackgroundResource(R.color.dialpad_calllog_background);
        mFunctionListView.setOnScrollListener(mOnScrollListener);
        mFunctionListView.setDivider(getResources().getDrawable(R.drawable.listview_margin_divider, null));
        mFunctionListView.setDividerHeight(1);

        mFunctionListAdapter = new ItemAdapter(activity);
        mFunctionListAdapter.add(createTextItem(R.string.save_as_new_contact_for_dialpad), FUNCITION_SAVE_AS_NEW_CONTACT_ID);
        mFunctionListAdapter.add(createTextItem(R.string.add_to_existing_contact_for_dialpad), FUNCITION_SAVE_AS_EXIST_CONTACT_ID);
        mVideoItem = createTextItem(R.string.make_video_call);
        mFunctionListAdapter.add(mVideoItem, FUNCITION_MAKE_VIDEO_CALL_ID);
        mFunctionListAdapter.add(createTextItem(R.string.menu_sendTextMessage), FUNCITION_SEND_MESSAGE_ID);
        mFunctionListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch ((int) id) {
                    case FUNCITION_SAVE_AS_NEW_CONTACT_ID:
                        addNewContact();
                        break;
                    case FUNCITION_SEND_MESSAGE_ID:
                        sendSms();
                        break;
                    case FUNCITION_SAVE_AS_EXIST_CONTACT_ID:
                        addToExistingContact();
                        break;
                    case FUNCITION_MAKE_VIDEO_CALL_ID:
                        makeVideoCall();
                        break;
                    default:
                        Log.e(TAG, "showFunctionList: setOnItemClickListener, cannot handle default error.");
                        break;
                }
            }
        });
        mFunctionListView.setAdapter(mFunctionListAdapter);
        mListFrameLayout.addView(mFunctionListView);
    }

    /**
     * <p>This method must be called after initFunctionList().</p>
     * <p>In QCom implementation, the video call enabled status might change.
     * So check it every time show the function list.</p>
     */
    public void updateFunctionList() {
        long id = mFunctionListAdapter.getItemId(FUNCITION_ITEM_INDEX_MAKE_VIDEO_CALL);
        if (SimUtil.isVideoCallEnabled(getActivity())) {
            if (id != FUNCITION_MAKE_VIDEO_CALL_ID) {
                mFunctionListAdapter.insert(mVideoItem, FUNCITION_ITEM_INDEX_MAKE_VIDEO_CALL);
            }
        } else {
            if (id == FUNCITION_MAKE_VIDEO_CALL_ID) {
                mFunctionListAdapter.remove(mVideoItem);
            }
        }
        mFunctionListAdapter.notifyDataSetChanged();
    }

    private TextItem createTextItem(int stringId) {
        final HeaderTextItem textItem = new HeaderTextItem("", getString(stringId), false);
        return textItem;
    }

    private OnScrollListener mOnScrollListener = new OnScrollListener() {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            switch (scrollState) {
                case OnScrollListener.SCROLL_STATE_IDLE:
                    NameConvertWorker.resume();
                    break;
                case OnScrollListener.SCROLL_STATE_FLING:
                case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                    if (view == mListViewForSearch) {
                        hiddenDialpad(true);
                    }
                    NameConvertWorker.pause();
                    break;
                default:
                    return;
            }
        }
    };
    private DialpadView.OnSpeedDialListener mSpeedDialListener = new DialpadView.OnSpeedDialListener() {
        @Override
        public void onSpeedDial(String number, int defaultSim) {
            Activity activity = getActivity();
            if (activity == null) {
                Log.w(TAG, "onSpeedDial: activity is null. maybe quiting.");
            }
            Context appContext = activity.getApplicationContext();
            int simid = SimUtil.INVALID_SLOT_ID;
            if (mMultiSimMode) {
                boolean sim1State = (mSim1Available.get() && mSim1Enabled.get());
                boolean sim2State = (mSim2Available.get() && mSim2Enabled.get());
                if (sim1State && sim2State) {
                    simid = defaultSim;
                } else {
                    if (sim1State)
                        simid = SimUtil.SLOT_ID_1;
                    if (sim2State)
                        simid = SimUtil.SLOT_ID_2;
                }
            }

            AliCallLogExtensionHelper.makeCall(appContext, number, simid);

            if (mDialpadView != null) {
                mDialpadView.setPhoneNumber("");
            }
            mClearPhoneNumberFlag = true;
        }
    };

    private void checkAirPlaneMode() {
        if (getActivity() != null) {
            mFlightMode = SimUtil.isAirplaneModeOn(getActivity());
        }
    }

    public void updateMatchAdapterQuickCallItem() {
        if (mMatchContactsListAdapter != null) {
            mMatchContactsListAdapter.updateQuickCallItem();
        }
    }

    public void setPhoneNumber(String number) {
        if (mDialpadView != null) {
            mDialpadView.setPhoneNumber(number);
        } else {
            mCachedPhoneNumber = number;
        }
        // We set an empty phone number and the dialpad is hidden.
        // In this case, we need to hide the whole DialpadFragment.
        if (TextUtils.isEmpty(number) && (!mIsDialpadShow)) {
            Activity activity = getActivity();
            if (activity instanceof PeopleActivity2) {
                ((PeopleActivity2) activity).hideDialpadFragment();
            }
        }
    }

    // This function is called when slide finger on dialpad
    /*
     * public void scrollViewPager() { this.clearEditText(); Activity activity =
     * getActivity(); if (activity instanceof PeopleActivity2) {
     * ((PeopleActivity2) activity).setCurrentTab(1); } showDialpad(false,
     * false); }
     */

    public void clearSearchItemLongClickMenu() {
        if (mMatchContactsListAdapter != null) {
            mMatchContactsListAdapter.clearPopupDialog();
        }
    }

    @Override
    public void onStateChanged(boolean isReady) {
        if (mDialpadView == null) {
            Log.w(TAG, "onStateChanged: mDialpadView is null.");
            return;
        }
        if (isReady) {
            mHandler.post(mTextChangeRunable);
        }
    }

    private void updateDialButton() {
        checkSimNetStatus();
        setSimMode();
    }

    private void checkSimNetStatus() {
        Context context = getActivity();
        if (context == null) {
            return;
        }

        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (mMultiSimMode) {
            mSim1Available.set(SimUtil.hasIccCard(SimUtil.SLOT_ID_1));
            mSim2Available.set(SimUtil.hasIccCard(SimUtil.SLOT_ID_2));

            boolean isSim1Enabled = SimUtil.isCardEnable(SimUtil.SLOT_ID_1);
            boolean isSim2Enabled = SimUtil.isCardEnable(SimUtil.SLOT_ID_2);

            //#<!-- [[ YunOS BEGIN PB
            //##module:()  ##author:xiuneng.wpf@alibaba-inc.com
            //##BugID:(7820417)  ##date:2016-01-28 16:18 -->
            boolean isSim1Idle = isIdle(tm, SimUtil.SLOT_ID_1);
            boolean isSim2Idle = isIdle(tm, SimUtil.SLOT_ID_2);

            // NOTE:
            // Currently, YunOS only support "Dual SIM Dual Standby" (not "Dual SIM Dual Active").
            // If sim1 is in call, then the dialpad shall only add a number to current call
            // and can not make a new call with sim2.
            // So we can not enable sim2 button in this case, and vice versa.
            mSim1Enabled.set(isSim1Enabled && isSim2Idle);
            mSim2Enabled.set(isSim2Enabled && isSim1Idle);
            //#<!-- YunOS END PB ]] -->
        } else {
            if (tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT) {
                mSimAvailable.set(false);
            } else {
                mSimAvailable.set(true);
            }
        }
    }

    private boolean isIdle(TelephonyManager tm, int slotId) {
        boolean result = false;
        int subId = SimUtil.getSubId(slotId);
        if (subId != ICrossPlatformSim.DEFAULT_SUB_ID) {
            result = tm.getCallState(subId) == TelephonyManager.CALL_STATE_IDLE;
        }
        return result;
    }

    private void setSimMode() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mSim1Available == null || mSim1Enabled == null || mSim2Available == null || mSim2Enabled == null
                || mSimAvailable == null) {
            return;
        }

        if (mDialpadView == null) {
            Log.w(TAG, "setSimMode: mDialpadView is null.");
            return;
        }

        long slotId = SimUtil.getDefaultVoiceSlotId();
        boolean sim1State = (mSim1Available.get() && mSim1Enabled.get());
        boolean sim2State = (mSim2Available.get() && mSim2Enabled.get());
        boolean simState = mSimAvailable.get();

        // not set dial default card
        if (slotId == SimUtil.SIM_DEFAULT_ALWAYS_ASK || slotId == SimUtil.SIM_DEFAULT_NO_SET) {
            /*
             * draw dual card when has 2 sim slot and at least one card
             * available
             */
            mDialpadView.setFooterBarSimMode(mMultiSimMode && (sim1State || sim2State), mMultiSimMode ? sim1State : simState,
                    sim2State);
            return;
        }
        // already set dial default card
        else {
            // set displayed card to default card.
            // sim1 available and default set to sim1
            boolean sim1State_isDisplayed = sim1State && (slotId == SimUtil.SLOT_ID_1);
            // sim2 available and default set to sim2
            boolean sim2State_isDisplayed = sim2State && (slotId == SimUtil.SLOT_ID_2);
            // if available card not set to default(all false), reset sim state
            // to show available card.
            if ((!sim1State_isDisplayed) && (!sim2State_isDisplayed)) {
                sim1State_isDisplayed = sim1State;
                sim2State_isDisplayed = sim2State;
            }
            // mDialpadFooterBar.setFooterBarSimMode(mMultiSimMode,
            // slotId == SimUtil.SUBSCRIPTION_1, slotId ==
            // SimUtil.SUBSCRIPTION_2);

            /*
             * draw dual card when has 2 sim slot and at least one card
             * available
             */
            mDialpadView.setFooterBarSimMode(mMultiSimMode && (sim1State || sim2State), mMultiSimMode ? sim1State_isDisplayed
                    : simState, sim2State_isDisplayed);
        }
    }

    private void updateEditTextInfo() {
        if (mSim1Available == null || mSim1Enabled == null || mSim2Available == null || mSim2Enabled == null
                || mSimAvailable == null) {
            return;
        }

        if (mDialpadView == null) {
            Log.w(TAG, "updateEditTextInfo: mDialpadView is null.");
            return;
        }

        boolean hasTextInfo = false;
        int textInfoRes = 0;
        if (mFlightMode) {
            hasTextInfo = true;
            textInfoRes = R.string.dialpad_info_airplane_mode_on;
        } else if ((!mSim1Available.get() || !mSim1Enabled.get()) && (!mSim2Available.get() || !mSim2Enabled.get())
                && !mSimAvailable.get()) {
            /* YUNOS BEGIN PB */
            //##email:caixiang.zcx@alibaba-inc.com
            //##BugID:(8107821) ##date:2016/04/05
            //##description:not show dialpad information when no card
            if (!Build.YUNOS_CARRIER_CUCC) {
                hasTextInfo = true;
                textInfoRes = R.string.dialpad_info_nosim;
            }
            /* YUNOS END PB */
        }
        mDialpadView.setInfoText(hasTextInfo, textInfoRes);
    }

    private void phoneNumberChanged(String number) {
        if ((mDialpadView == null) || (mSmartSearch == null)) {
            Log.w(TAG, "phoneNumberChanged: mDialpadView or mSmartSearch is null.");
            return;
        }

        if (SpecialCharSequenceMgr.handleChars(getActivity(), number)) {
            // A special sequence was entered, clear the digits
            setPhoneNumber("");
            mListContainer.setVisibility(View.GONE);
            return;
        }
        mListContainer.setVisibility(TextUtils.isEmpty(number) ? View.GONE : View.VISIBLE);
        mTopTextView.setText(number);
        if (TextUtils.isEmpty(number)) {
            showView(VIEW_EMPTY);
            return;
        }
        mSmartSearch.sendSearchMessage(number);
    }

    public void clearPhoneNumber() {
        if (mEditText != null) {
            mEditText.setText("");
            mEditText.setCursorVisible(false);
        }
    }

    public String getPhoneNumber() {
        if (!TextUtils.isEmpty(mCachedPhoneNumber)) {
            return mCachedPhoneNumber;
        }
        return mDialpadView == null ? null : mDialpadView.getPhoneNumber();
    }

    private void initAnimation() {
        if (mSlideIn == null) {
            mSlideIn = AnimationUtils.loadAnimation(getActivity(), R.anim.dialpad_slide_in_bottom);
        }
        if (mSlideOut == null) {
            mSlideOut = AnimationUtils.loadAnimation(getActivity(), R.anim.dialpad_slide_out_bottom);
            mSlideOut.setAnimationListener(mSlideOutListener);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (getActivity() == null) {
            return;
        }
        if (hidden) {
            hiddenDialpad(true);
        } else {
            showDialpad(true);
        }
    }

    public boolean isShowDialpad() {
        return mIsDialpadShow;
    }

    public void showDialpad(boolean animate) {
        if (mIsDialpadShow) {
            return;
        }
        mIsDialpadShow = true;
        if (animate) {
            mDialpadContainerLayout.startAnimation(mSlideIn);
        }
        mDialpadContainerLayout.setVisibility(View.VISIBLE);
        mTopLayout.setVisibility(View.GONE);
    }

    public void hiddenDialpad(boolean animate) {
        if (!mIsDialpadShow || mDialpadContainerLayout == null) {
            return;
        }
        mIsDialpadShow = false;
        if (animate) {
            mDialpadContainerLayout.startAnimation(mSlideOut);
        } else {
            mDialpadContainerLayout.setVisibility(View.GONE);
        }
        mTopLayout.setVisibility(View.VISIBLE);
    }
}
