
package com.yunos.alicontacts.dialpad;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ActionBarAdapter2.TabState;
import com.yunos.alicontacts.activities.PeopleActivity2;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.dialpad.calllog.CallLogAdapter;
import com.yunos.alicontacts.dialpad.calllog.CallLogMultiSelectActivity;
import com.yunos.alicontacts.dialpad.calllog.CallLogQueryHandler;
import com.yunos.alicontacts.dialpad.smartsearch.NameConvertWorker;
import com.yunos.alicontacts.plugins.PluginPlatformPrefs;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.common.UsageReporter;

import hwdroid.widget.ActionSheet;
import hwdroid.widget.ActionSheet.SingleChoiceListener;
import yunos.support.v4.app.Fragment;

import java.util.Arrays;

public class CallLogFragment extends Fragment implements
        CallLogAdapter.DialListener, CallLogQueryHandler.Listener {

    public static final int QUICK_CALL_ACTIVITY_REQUEST_CODE = 7;

    // Here use class.getSimpleName() for usage reporter name.
    public static final String USAGE_REPORTER_NAME = DialpadFragment.class.getSimpleName();
    public static final String CLASSNAME = "CallLogFragment";
    public static final String TAG = CLASSNAME;
    // The log with TMPTAG will be deleted several days later.
    // Use it just for finding convenient;
    private static final String TMPTAG = "PERFORMANCE";

    private ListView mListView;
    private LinearLayout mEmptyView;
    private FrameLayout mContent;

    private OnTouchListener mListTouchListener;

    private CallLogAdapter mCallLogAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;

    // This function can't call twice.
    private boolean mDidCreateView = false;
    private View mDialpadViewRoot;

    private int mFilterType = CallLogQueryHandler.QUERY_CALLS_ALL;

    private Handler mHandler = new Handler();

    // Indicate whether this fragment is launched from desktop for the first
    // tab.
    private boolean mIsColdLaunched;
    // Indicate whether there are some functions need be called delayed
    // We can't use mIsColdLaunched, because it is one-shot.
    private boolean mHaveDelayedCall;

    // Indicate whether fetched call log data from database
    private boolean mFetchedCallLogData;
    // Indicate whether setUserVisibleHint called
    private boolean mCalledUserVisibleHint;

    // Indicate whether need to be refreshed. when this fragment is stopped,
    // then switched from all people fragment, it should be refreshed in
    // setUserVisibleHint()
    private boolean mIsNeedRefresh = false;

    private CallLogManager mCallLogManager;
    private AliCallLogChangeListener mCallLogListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TMPTAG, "CallLogFragment onCreate called");
        super.onCreate(savedInstanceState);
        mCallLogManager = CallLogManager.getInstance(ActivityThread.currentApplication());
        mCallLogQueryHandler = new CallLogQueryHandler(CallLogFragment.this);

        mCallLogListener = new AliCallLogChangeListener();
        mCallLogManager.registCallsTableChangeListener(mCallLogListener);

        Log.i(TMPTAG, "CallLogFragment onCreate end");
    }

    private void cancelMissedCallNotifications() {
        try {
            Log.i(TAG, "cancelMissedCallNotifications: ");
            Activity activity = getActivity();
            if (activity == null) {
                Log.w(TAG, "cancelMissedCallNotifications: activity is null.");
                return;
            }
            KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
            boolean keyguardOn = keyguardManager.inKeyguardRestrictedInputMode();
            if (keyguardOn) {
                Log.i(TAG, "cancelMissedCallNotifications: keyguard on, skip cancel missed call notification.");
                return;
            }
            TelecomManager telecomManager = (TelecomManager) activity.getSystemService(Context.TELECOM_SERVICE);
            telecomManager.cancelMissedCallsNotification();
        } catch (Exception e) {
            // from bug: 6407690, we might get exception here for unknown reason.
            Log.e(TAG, "cancelMissedCallNotifications: got exception.", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TMPTAG, "CallLogFragment onDestroy called");
        mIsColdLaunched = false;
        mIsNeedRefresh = false;
        mCallLogManager.unRegistCallsTableChangeListener(mCallLogListener);
        mHandler.removeCallbacksAndMessages(null);
        if (mCallLogAdapter != null) {
            // BugID:61842:StrictMode policy violation: com.yunos.alicontacts
            // release resources when quit
            mCallLogAdapter.releaseCursor();
        }

        super.onDestroy();
        Log.i(TMPTAG, "CallLogFragment onDestroy end");
    }

    public void setColdLaunched(boolean coldLaunch) {
        mIsColdLaunched = coldLaunch;
    }

    public void clearDialog() {
        if (mCallLogAdapter != null) {
            mCallLogAdapter.clearDialog();
        }
    }

    @Override
    public void onStop() {
        // bug 59150. missed call notification not cleared when entering this
        // view from LockScreen.
        // cancel missed call notification in case onwindowfocuschanged not call
        // to resume.
        super.onStop();

        if (this.isCurrentTab()) {
            cancelMissedCallNotifications();
        }
        if (mFilterDialog != null) {
            mFilterDialog.dismiss();
            mFilterDialog = null;
        }
        mIsNeedRefresh = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        UsageReporter.onPause(null, USAGE_REPORTER_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TMPTAG, "CallLogFragment onCreateView called");
        mDialpadViewRoot = inflater.inflate(R.layout.ali_calllog_fragment, container, false);
        Log.i(TMPTAG, "CallLogFragment onCreateView, mIsColdLaunched = " + mIsColdLaunched);
        if (mIsColdLaunched) {
            Log.i(TMPTAG, "CallLogFragment onCreateView, call initDialpadView");
            doCreateView();
            doStart();
            showView();
        } else {
            // Set the flag, we will do it later
            mHaveDelayedCall = true;
        }
        Log.i(TMPTAG, "CallLogFragment onCreateView end");
        return mDialpadViewRoot;
    }

    private void doCreateView() {
        if (mDidCreateView) {
            return;
        }
        mContent = (FrameLayout) mDialpadViewRoot.findViewById(R.id.content);

        mCallLogAdapter = new CallLogAdapter(getActivity());
        mCallLogAdapter.setMultSimEnabled(SimUtil.MULTISIM_ENABLE);
        mCallLogAdapter.setModeCurrent(CallLogAdapter.MODE_VIEW);
        mCallLogAdapter.setDialListener(this);
        mDidCreateView = true;
    }

    private boolean isCurrentTab() {
        Activity activity = getActivity();
        if (!(activity instanceof PeopleActivity2)) {
            return false;
        }

        int tab = ((PeopleActivity2) activity).getCurrentTab();
        boolean result = (tab == TabState.CALLLOG);
        return result;
    }

    /**
     * To display a specific view in the main display area. Other views in the
     * same place will be hidden.
     *
     * @param which Specify a view to display. It is one of the VIEW_XXX
     *            constants.
     */
    private void showView() {
        if (mListView == null) {
            initListView();
        }
        mListView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCallsFetched(Cursor combinedCursor) {
        if (mCallLogAdapter != null) {
            mCallLogAdapter.setLoading(false);
        }

        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return false;
        }

        // TODO
        // if (mDialpadView != null &&
        // !TextUtils.isEmpty(mDialpadView.getPhoneNumber())) {
        // mNeedRefreshCallLogOnQuitSearch = true;
        // return false;
        // }

        // In this function, we got the data. So means delayed call and onStart
        // had called.
        // If CallLogFragment is the first fragment when startup
        // we all should change cursor here
        // else
        // onStart was called after the other fragment(CallLogFragment) was
        // ready(visible).
        // so call change cursor here didn't effect the startup time.
        Log.i(TMPTAG, "CallLogFragment onCallsFetched will call change cursor");
        if (mCallLogAdapter != null) {
            mCallLogAdapter.changeCursor(combinedCursor);
        }

        updateDialpadFooterMenu();
        updateNoCallLogView();

        mFetchedCallLogData = true;
        if (mCalledUserVisibleHint) {
            // If the fragment is visible to user, that means we did all thing.
            // so post other
            // fragments runnable to init.
            if (mIsColdLaunched) {
                mIsColdLaunched = false;
                ((PeopleActivity2) activity).postOtherFragmentInitialStart(TabState.CALLLOG);
            }
            // The two flags are one-shot, just to post other fragments
            // runnable.
            // So reset them here.
            mFetchedCallLogData = false;
            mCalledUserVisibleHint = false;
        }
        return true;
    }

    @Override
    public void onStart() {
        Log.i(TMPTAG, "CallLogFragment onStart called, mIsColdLaunched=" + mIsColdLaunched);
        super.onStart();

        // Call doStart when cold launch. we needn't call doStart everytime,
        // because
        // ContentObserver onChange will query the new data.
        // If we didn't call doStart here, it will be called later by
        // postInitialStart();
        if (mIsColdLaunched) {
            doStart();
            mIsNeedRefresh = false;
            // Start sync thread to sync calls table with ali calls table.
            requestSyncCalllogs();
            Log.i(TMPTAG, "CallLogFragment onStart, call doStart, startSyncCalllogsThread");
        }

        Log.i(TMPTAG, "CallLogFragment onStart end");
    }

    private void startCallsQuery() {
        if (mCallLogAdapter != null && !mCallLogAdapter.isLoading()) {
            mCallLogAdapter.setLoading(true);
            mCallLogQueryHandler.queryCalls(mFilterType);
        }
    }

    @Override
    public void onResume() {
        Log.i(TMPTAG, "CallLogFragment onResume called enter, mIsColdLaunched=" + mIsColdLaunched);
        super.onResume();

        // If cold launch, we need do everything
        if (mIsColdLaunched) {
            doResume();
            Log.i(TMPTAG, "CallLogFragment onResume, checkNewIntent, doResume");
            UsageReporter.onResume(null, USAGE_REPORTER_NAME);
        } else {
            if (isCurrentTab()) {
                // do resume action only when this fragment is current tab.
                doResume();
                mIsNeedRefresh = false;

                // we need update the calllog time, just send a message to a
                // lowest thread
                if (mCallLogAdapter != null) {
                    mCallLogAdapter.forceUpdateData();
                }

                Log.i(TMPTAG, "CallLogFragment onResume 2");
                UsageReporter.onResume(null, USAGE_REPORTER_NAME);
            }
        }
        Log.i(TMPTAG, "CallLogFragment onResume end");
    }

    private void doStart() {
        Log.i(TMPTAG, "CallLogFragment startCallsQuery called");
        startCallsQuery();
    }

    private void doResume() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cancelMissedCallNotifications();
            }
        }, 1000);

        backToAllRecords();
    }

    public void backToAllRecords() {
        if (mFilterType != CallLogQueryHandler.QUERY_CALLS_ALL) {
            this.filterCallRecorder(CallLogQueryHandler.QUERY_CALLS_ALL);
            if (mFilterDialog != null) {
                mFilterDialog.dismiss();
                mFilterDialog = null;
            }
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
    }

    @Override
    public void editBeforeCall(String number) {
        // The call log might have no number info, e.g. the user does NOT subscribe
        // caller id service in the pay plan. So we have to ignore empty number here.
        if ((getActivity() != null) && (!TextUtils.isEmpty(number))) {
            ((PeopleActivity2) getActivity()).setDialpadFragmentNumber(number);
        }
    }

    public String getLastCallLog() {
        return mCallLogAdapter == null ? null : mCallLogAdapter.queryLastOutCall();
    }

    public void deleteCallLog() {
        if (mCallLogAdapter == null || mCallLogAdapter.getCount() == 0) {
            Toast.makeText(this.getActivity(), R.string.dialpad_callog_empty_text, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getActivity(), CallLogMultiSelectActivity.class);
        intent.putExtra(CallLogMultiSelectActivity.EXTRA_KEY_FILTER_TYPE, mFilterType);
        startActivity(intent);

        UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.DP_ENTER_CALLLOG_DELETE);
    }

    public void syncCallLog() {
        Intent intent = new Intent("com.aliyun.xiaoyunmi.action.SELECT_SYNC");
        intent.setData(Uri.parse("yunmi://sync_select/?type=calllog"));
        intent.putExtra("wificheck", false);
        intent.putExtra("synctype", "backup");
        startActivity(intent);
    }

    private void startNameConvertWorker() {
        Activity activity = getActivity();
        if (activity != null) {
            NameConvertWorker.getInstance().init(activity);
        }
    }

    private class AliCallLogChangeListener implements CallLogManager.CallLogChangeListener {
        @Override
        public void onCallLogChange(int changedPart) {
            Log.d(TAG, "AliCallLogChangeListener onCallLogChange.");
            startCallsQuery();
            notifyCallLogChange();
        }
    }

    private void notifyCallLogChange() {
        Activity activity = getActivity();
        if (!(activity instanceof PeopleActivity2)) {
            Log.w(TAG, "notifyCallLogChange: not attached to PeopleActivity2. Shall not happen.");
            return;
        }
        ((PeopleActivity2) activity).notifyCallLogChangedToDialpadFragment();
    }

    private void initListView() {
        mListView = new ListView(getActivity());
        ViewGroup.LayoutParams lpList = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mListView.setLayoutParams(lpList);
        mListTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideDialpadFragment();
                return false;
            }
        };
        mListView.setOnTouchListener(mListTouchListener);
        mListView.setAdapter(mCallLogAdapter);
        mListView.setOnItemLongClickListener(mCallLogAdapter.mItemLongClickListener);
        mListView.setOnItemClickListener(mCallLogAdapter.itemClickListener);
        mListView.setOnScrollListener(mOnScrollListener);
        mListView.setDivider(getResources().getDrawable(R.drawable.listview_margin_divider, null));
        mListView.setDividerHeight(1);

        mContent.addView(mListView, mContent.getChildCount() - 1);
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
                    NameConvertWorker.pause();
                    break;
                default:
                    return;
            }
        }
    };

    private void hideDialpadFragment() {
        Activity activity = getActivity();
        if (!(activity instanceof PeopleActivity2)) {
            Log.w(TAG, "hideDialpadFragment: not attached to PeopleActivity2. Shall not happen.");
            return;
        }
        ((PeopleActivity2) activity).hideDialpadFragment();
    }

    private ActionSheet mFilterDialog;

    public void popupFilterDialog() {
        final Activity activity = this.getActivity();
        if (activity == null) {
            Log.e(TAG, "popupFilterDialog: activity is null. ERROR!");
            return;
        }

        mFilterDialog = new ActionSheet(activity);
        mFilterDialog.setTitle(activity.getString(R.string.dialpad_footer_filter));
        CharSequence[] origin_items = getResources().getTextArray(R.array.dialpad_records_filter_items);
        CharSequence[] items;
        if (PluginPlatformPrefs.isCMCC() && SimUtil.MULTISIM_ENABLE) {
            items = Arrays.copyOf(origin_items, 6);
        } else {
            // not sim1 and sim2 filter type
            items = Arrays.copyOf(origin_items, 4);
        }

        // TODO:Remove When AUI ready.
        String[] item_str = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            item_str[i] = items[i].toString();
        }

        int checked = 0;
        switch (mFilterType) {
            case CallLogQueryHandler.QUERY_CALLS_ALL:
                checked = 0;
                break;
            case CallLogQueryHandler.QUERY_CALLS_MISSED:
                checked = 1;
                break;
            case CallLogQueryHandler.QUERY_CALLS_INCOMING:
                checked = 2;
                break;
            case CallLogQueryHandler.QUERY_CALLS_OUTGOING:
                checked = 3;
                break;
            // cmcc customization
            case CallLogQueryHandler.QUERY_CALLS_SIM1:
                checked = 4;
                break;
            case CallLogQueryHandler.QUERY_CALLS_SIM2:
                checked = 5;
                break;
            default:
                break;
        }
        mFilterDialog.setSingleChoiceItems(item_str, checked, new SingleChoiceListener() {

            @Override
            public void onDismiss(ActionSheet actionSheet) {
            }

            @Override
            public void onClick(int position) {
                switch (position) {
                    case 0: // all calls
                        filterCallRecorder(CallLogQueryHandler.QUERY_CALLS_ALL);
                        UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.FILTER_CALL_LOG_ALL);
                        break;
                    case 1: // missed calls
                        filterCallRecorder(CallLogQueryHandler.QUERY_CALLS_MISSED);
                        UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.FILTER_CALL_LOG_MISSED);
                        break;
                    case 2: // incoming calls
                        filterCallRecorder(CallLogQueryHandler.QUERY_CALLS_INCOMING);
                        UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.FILTER_CALL_LOG_INCOMING);
                        break;
                    case 3: // outgoing calls
                        filterCallRecorder(CallLogQueryHandler.QUERY_CALLS_OUTGOING);
                        UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.FILTER_CALL_LOG_OUTGOING);
                        break;
                    // cmcc customization
                    case 4: // SIM1
                        filterCallRecorder(CallLogQueryHandler.QUERY_CALLS_SIM1);
                        break;
                    case 5: // SIM2
                        filterCallRecorder(CallLogQueryHandler.QUERY_CALLS_SIM2);
                        break;
                    default:
                        break;
                }

            }
        });

        mFilterDialog.showWithDialog();
    }

    // call recorder filter
    public void filterCallRecorder(int type) {
        if (mFilterType == type) {
            return;
        }

        mFilterType = type;
        mCallLogQueryHandler.queryCalls(type);
    }

    private void initEmptyView() {
        ViewStub emptyStub = (ViewStub) mDialpadViewRoot.findViewById(R.id.empty_view);
        if (emptyStub != null) {
            mEmptyView = (LinearLayout) emptyStub.inflate();
        }
    }

    private void updateNoCallLogView() {
        // here we just make sure when we want to show emptyView it be
        // initialized if can.
        if (mCallLogAdapter != null) {
            int count = mCallLogAdapter.getCount();
            if (count == 0) {
                if (mEmptyView == null) {
                    initEmptyView();
                }
                if (mEmptyView != null) {
                    mEmptyView.setVisibility(View.VISIBLE);
                }
            } else if (mEmptyView != null) {
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }

    public void updateDialpadFooterMenu() {
        PeopleActivity2 activity = (PeopleActivity2) getActivity();
        if (activity == null) {
            return;
        }
        boolean enable = (mCallLogAdapter == null || mCallLogAdapter.getCount() == 0) ? false : true;
        activity.setMenuItemEnable(PeopleActivity2.FOOTER_BUTTON_CALL_LOG_DELETE, enable);
        if (mFilterType == CallLogQueryHandler.QUERY_CALLS_ALL) {
            activity.setMenuItemEnable(PeopleActivity2.FOOTER_BUTTON_CALL_LOG_FILTER, enable);
            activity.setMenuItemEnable(PeopleActivity2.FOOTER_BUTTON_CALL_LOG_BACKUP, enable);
        }

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        // When this fragment's visibility is changed, it will be invoked.
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TMPTAG, "CallLogFragment setUserVisibleHint called, isVisibleToUser=" + isVisibleToUser);
        if (isVisibleToUser) {
            startNameConvertWorker();

            Activity activity = getActivity();
            if (!(activity instanceof PeopleActivity2)) {
                Log.i(TMPTAG, "CallLogFragment setUserVisibleHint return because activity");
                return;
            }

            mCalledUserVisibleHint = true;
            if (mFetchedCallLogData) {
                // if mFetchedCallLogData is true, it means we got data before
                // the fragment
                // is visible to user, we did all things(onCreateView, onStart,
                // onResume,
                // onCallsFetched), so post other fragments runnablt to init.
                // else, we will wait for query return, then post other
                // fragments runnable to init.
                if (mIsColdLaunched) {
                    mIsColdLaunched = false;
                    ((PeopleActivity2) activity).postOtherFragmentInitialStart(TabState.CALLLOG);
                }
                // The two flags are one-shot flag, just to post other fragments
                // runnable
                // So reset them here
                mCalledUserVisibleHint = false;
                mFetchedCallLogData = false;
            }

            // If have delayed call or need refresh or orinitial start is
            // failed,
            // it need to reload contacts data as well
            // Add mDidInitDialpadView condition for bug 5228095, from the
            // phenomenon, we didn't
            // call following functions when the fragment is visible to user
            if (mHaveDelayedCall || mIsNeedRefresh || mInitialStartFailed || !mDidCreateView) {
                doCreateView();
                if (mInitialStartFailed) {
                    // Start sync thread to sync calls table with ali calls
                    // table.
                    Log.i(TAG, "[CALL] startSyncCalllogsThread start 1");
                    requestSyncCalllogs();
                }
                showView();
                // Call the absent function here, must following the createview,
                // onstart, onresume sequence
                doStart();
                doResume();
                mInitialStartFailed = false;
                mIsNeedRefresh = false;
                mHaveDelayedCall = false;
            }

            // we need update the calllog time, just send a message to a lowest
            // thread
            mCallLogAdapter.forceUpdateData();
        }
    }

    // If getActivity() return null, initial start is failed, the first loading
    // step will be skipped.
    // It need to reload data in setUserVisibleHint(), when user switch
    // to dialpad tab.
    private boolean mInitialStartFailed;
    private Runnable mInitialStart = new Runnable() {
        @Override
        public void run() {
            Log.i(TMPTAG, "CallLogFragment mInitialStart called");
            if (getActivity() == null) {
                Log.i(TMPTAG, "mInitialStart/run: getActivity() is null.");
                mInitialStartFailed = true;
                return;
            }
            // Start sync thread to sync calls table with ali calls table.
            Log.i(TMPTAG, "CallLogFragment will call ....");
            doCreateView();
            showView();
            doStart();
            requestSyncCalllogs();
            doResume();
            mHaveDelayedCall = false;
        }
    };

    /**
     * Post to initialize, if contacts fragment launched from desktop.
     */
    public void postInitialStart() {
        mHandler.removeCallbacks(mInitialStart);
        mHandler.postDelayed(mInitialStart, 0);
    }

    public void requestSyncCalllogs() {
        mCallLogManager.requestSyncCalllogsByInit();
    }

    /**
     * Called at very start of launch app, to make sure the class loader has
     * loaded the class. If the class is loaded in background before first
     * create instance, the creation time in main thread will be reduced
     * significantly.
     */
    public static void warmUp() {
        // do nothing;
    }

}
