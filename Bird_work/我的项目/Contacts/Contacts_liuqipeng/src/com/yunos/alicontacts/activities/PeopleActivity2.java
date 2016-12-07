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

package com.yunos.alicontacts.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.aliyun.ams.systembar.SystemBarColorManager;
/* YUNOS BEGIN */
//##modules(AliContacts) ##author: hongwei.zhw
//##BugID:(8161644) ##date:2016.4.18
//##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
import android.os.Build;
/* YUNOS END */
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ActionBarAdapter2.TabState;
import com.yunos.alicontacts.appwidget.GadgetProvider;
import com.yunos.alicontacts.dialpad.CallLogFragment;
import com.yunos.alicontacts.dialpad.DialpadFragment;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.interactions.ContactDeletionInteraction;
import com.yunos.alicontacts.interactions.PhoneNumberInteraction2;
import com.yunos.alicontacts.list.AccountFilterManager;
import com.yunos.alicontacts.list.ContactEntryListFragment;
import com.yunos.alicontacts.list.ContactListFilter;
import com.yunos.alicontacts.list.ContactListFilterController;
import com.yunos.alicontacts.list.ContactsIntentResolver;
import com.yunos.alicontacts.list.ContactsRequest;
import com.yunos.alicontacts.list.CustomContactListFilterActivity;
import com.yunos.alicontacts.list.DefaultContactBrowseListFragment;
import com.yunos.alicontacts.list.DirectoryListLoader;
import com.yunos.alicontacts.list.OnContactBrowserActionListener;
import com.yunos.alicontacts.preference.ContactsSettingActivity;
import com.yunos.alicontacts.preference.PhoneSettingActivity;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.Constants;
import com.yunos.alicontacts.util.FeatureOptionAssistant;
import com.yunos.alicontacts.util.PhoneCapabilityTester;
import com.yunos.alicontacts.util.PreferencesUtils;
import com.yunos.alicontacts.util.YunOSFeatureHelper;
import com.yunos.alicontacts.util.preloadcontact.PreloadContactUtil;
import com.yunos.alicontacts.widget.aui.PopMenu;
import com.yunos.common.UiTools;
import com.yunos.common.UsageReporter;
import com.yunos.yundroid.widget.CustomViewPager;

import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentActivity;
import yunos.support.v4.app.FragmentManager;
import yunos.support.v4.app.FragmentPagerAdapter;
import yunos.support.v4.app.FragmentTransaction;
import yunos.support.v4.view.PagerAdapter;
import yunos.support.v4.view.ViewPager;
import yunos.support.v4.view.ViewPager.OnPageChangeListener;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays a list to browse contacts. For xlarge screens, this also displays a
 * detail-pane on the right.
 */
public class PeopleActivity2 extends FragmentActivity implements ActionBarAdapter2.Listener,
        ContactListFilterController.ContactListFilterListener, PopMenu.OnPopMenuListener {

    private static final String TAG = "PeopleActivity2";
    // The log with TMPTAG will be deleted several days later.
    // Use it just for finding convenient;
    private static final String TMPTAG = "PERFORMANCE";

    private final String CALLLOG_TAG = "tab-pager-calllog";
    private final String ALL_TAG = "tab-pager-all";
    private final String DIALPAD_TAG = "tab-pager-dialpad";

    private static final int TAB_SCROLL_DURATION = 200;

    private static final String SECURITYCENTER_PACKAGE = "com.aliyun.SecurityCenter";
    private static final String SECURITYCENTER_CLASS = "com.aliyun.SecurityCenter.harassintercept.HarassInterceptionActivity";

    // These values needs to start at 2. See {@link ContactEntryListFragment}.
    private static final int SUBACTIVITY_NEW_CONTACT = 2;
    private static final int SUBACTIVITY_EDIT_CONTACT = 3;
    // private static final int SUBACTIVITY_NEW_GROUP = 4;
    // private static final int SUBACTIVITY_EDIT_GROUP = 5;
    // private static final int SUBACTIVITY_ACCOUNT_FILTER = 6;

    // footer bar button ID
    private static final int FOOTER_BUTTON_CONTACTS_NEW = 334;
    public static final int FOOTER_BUTTON_CONTACTS_DELETE = 335;
    private static final int FOOTER_BUTTON_CONTACTS_SYNC = 336;
    private static final int FOOTER_BUTTON_CONTACTS_SETTINGS = 337;
    private static final int FOOTER_BUTTON_CONTACTS_GROUP = 339;
    private static final int FOOTER_BUTTON_CONTACTS_ACCOUNTS_FILTER = 340;

    public static final int FOOTER_BUTTON_CALL_LOG_DELETE = 400;
    public static final int FOOTER_BUTTON_CALL_LOG_CONFERENCE_CALL = 401;
    public static final int FOOTER_BUTTON_CALL_LOG_FILTER = 402;
    public static final int FOOTER_BUTTON_CALL_LOG_BACKUP = 403;
    public static final int FOOTER_BUTTON_CALL_LOG_INTERCEPT = 404;
    public static final int FOOTER_BUTTON_CALL_LOG_SETTING = 405;

    public static final String OPEN_CALL_BLOCK_PAGE_TYPE = "page_type";
    public static final String OPEN_CALL_BLOCK_CALL = "call";

    //for cloud call
    private static final String CLOUD_CALL_BEEN_CALLED_KEY = "cloud_call_has_been_called";
    private static final String CLOUD_CALL_ACTION = "com.yunos.yuntelmembercenter.action.LAUNCH_MEMBER_CENTER";

    // Extra name for amount of contacts
    public static final String EXTRA_CONTACTS_AMOUNT = "contacts_amount";
    public static final int CONTACTS_AMOUNT_DEFAULT_VALUE = -1;

    private static final int[] TAB_LABEL_IDS = new int[] {
            R.string.contactsDialpadLabel, R.string.contactsAllLabel, R.string.contactsYellowpageLabel
    };
    /**
     * This flag is used to control the first time load after cold start. The
     * contacts list only loads a small part of all contacts for quick display
     * on cold start. After the first time load finished, the contacts list
     * loads all data.
     */
    public static boolean sFirstLoad = true;

    private ContactsIntentResolver mIntentResolver;
    private ContactsRequest mRequest;

    private ActionBarAdapter2 mActionBarAdapter;

    private View mTitleView;
    private TextView mDialpadTitle;
    private TextView mContactTtitle;

    //for cloud call
    private ImageView mCloudCallBtn;
    private ImageView mCloudCallBtnRedDot;
    //this flag avoid so many times call mDefaultPrefs.getBoolean...
    private boolean mCloudBtnHasBeenClicked = false;

    private ContactListFilterController mContactListFilterController;

    private SharedPreferences mDefaultPrefs;

    private static final long UPDATE_CYCLE = 1 * 24 * 60 * 60 * 1000L;
    private long mLastUpdateTime = 0;

    /**
     * Showing a list of Contacts. Also used for showing search results in
     * search mode.
     */
    private DefaultContactBrowseListFragment mAllFragment;
    private CallLogFragment mCallLogFragment;
    private DialpadFragment mDialpadFragment;
    private static final String EXTRA_KEY_SELECTED_GROUP = "selected_group_uri";
    private Parcelable mGroupUri;

    /**
     * If {@link #configureFragments(boolean)} is already called. Used to avoid
     * calling it twice in {@link #onStart}. (This initialization only needs to
     * be done once in onStart() when the Activity was just created from scratch
     * -- i.e. onCreate() was just called)
     */
    private boolean mFragmentInitialized;

    /**
     * Whether or not the current contact filter is valid or not. We need to do
     * a check on start of the app to verify that the user is not in single
     * contact mode. If so, we should dynamically change the filter, unless the
     * incoming intent specifically requested a contact that should be displayed
     * in that mode.
     */
    private boolean mCurrentFilterIsValid;

    // Flag for two panes or one pane
    private boolean mIsUsingTwoPanes;

    private PopMenu mCallLogPopMenu;
    private PopMenu mContactPopMenu;

    /** ViewPager for swipe, used only on the phone (i.e. one-pane mode) */
    private CustomViewPager mTabPager;
    private TabPagerAdapter mTabPagerAdapter;
    PeopleTabScroller mTabScroller;

    /** Sequential ID assigned to each instance; used for logging */
    private final int mInstanceId;
    private static final AtomicInteger sNextInstanceId = new AtomicInteger();

    private SystemBarColorManager mSystemBarColorManager;
    /** check OS update service. */
    YunOSFeatureHelper.OSUpdateCheckProxy mOSUpdate;
    private static final String YUNOS_OS_LAST_UPDATE_TIME = "lastUpdateTime";

    /**
     * Copied from PhoneApp. See comments in Phone app for more detail.
     */
    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";

    public PeopleActivity2() {
        mInstanceId = sNextInstanceId.getAndIncrement();
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        // Shown on logcat
        return String.format("%s@%d", getClass().getSimpleName(), mInstanceId);
    }

    private BroadcastReceiver mHomeKeyDismissDialogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDialpadFragment != null) {
                mDialpadFragment.clearSearchItemLongClickMenu();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedState) {
        Log.d(TMPTAG, "PeopleActivity2 onCreate called");
        // if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
        // Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate start");
        // }
        super.onCreate(savedState);
        mDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.people_content_normal);
        // set the first load flag to true every time onCreate,
        // because the activity might be killed by systemui or security center.
        // and next time, we still need it to be true.
        sFirstLoad = true;

        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeKeyDismissDialogReceiver, filter);

        if (!processIntent(false)) {
            finish();
            Log.e(TAG, "processIntent invalid intent, return!");
            return;
        }

        mIsUsingTwoPanes = PhoneCapabilityTester.isUsingTwoPanes(this);

        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.checkFilterValidity(false);
        mContactListFilterController.addListener(this);

        createViewsAndFragments(savedState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // Make sure this new just call one time, or will add many views to decorview.
        mSystemBarColorManager = new SystemBarColorManager(this);
        mSystemBarColorManager.setViewFitsSystemWindows(this, false);

        setSystembarColor(getResources().getColor(R.color.title_color), getResources().getBoolean(R.bool.contact_dark_mode));
        Log.d(TMPTAG, "PeopleActivity2 onCreate end");
    }

    private void setSystembarColor(int color, boolean isSearch) {
        mSystemBarColorManager.setStatusBarColor(color);
        mSystemBarColorManager.setStatusBarDarkMode(this, isSearch);
    }

    private void checkDialpadFrament() {
        if (mTabToOpen == TabState.CALLLOG) {
            Intent newIntent = getIntent();
            Uri uri = newIntent.getData();
            if (uri != null && Constants.SCHEME_TEL.equals(uri.getScheme())) {
                String data = uri.getSchemeSpecificPart();
                if (!TextUtils.isEmpty(data)) {
                    setDialpadFragmentNumber(data);
                    return;
                }
            }
        }
        //hideDialpadFragment();
    }

    public void setDialpadFragmentNumber(String number) {
        showDialpadFragment();
        mDialpadFragment.setPhoneNumber(number);
    }

    public CallLogFragment getCallLogFragment() {
        return mCallLogFragment;
    }

    public void hideDialpadFragment() {
        // YunOS BEGIN PB
        // ##module:(Contacts)  ##author:shihuai.wg@alibaba-inc.com
        // ##BugID:(8580981)  ##date:2016-07-22
        if (mDialpadFragment != null && !PeopleActivity2.this.isDestroyed()) {
        // YunOS END PB
            mDialpadFragment.clearPhoneNumber();
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            trans.hide(mDialpadFragment);
            trans.commitAllowingStateLoss();
        }
    }

    public void showDialpadFragment() {
        if (mDialpadFragment == null) {
            mDialpadFragment = new DialpadFragment();
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            trans.add(R.id.people_root_layout, mDialpadFragment, DIALPAD_TAG);
            trans.commitAllowingStateLoss();
            return;
        }
        if (mDialpadFragment.isVisible()) {
            return;
        }
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        trans.show(mDialpadFragment);
        trans.commitAllowingStateLoss();
    }

    private void startConferenceCall() {
        Intent intent = new Intent(ConferenceRecipientsPickerActivity.ACTION_CONFERENCE_RECIPIENTS_PICKER);
        intent.setClass(getApplicationContext(), ConferenceRecipientsPickerActivity.class);
        startActivity(intent);
    }

    private void openCallIntercept() {
        Intent i = new Intent();
        i.setClassName(SECURITYCENTER_PACKAGE, SECURITYCENTER_CLASS);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(OPEN_CALL_BLOCK_PAGE_TYPE, OPEN_CALL_BLOCK_CALL);
        startActivity(i);
        UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.Click_MENU_Harassing_Phone_Calls);
    }

    private void startDialerSettingsActivity() {
        Intent intent = new Intent(this, PhoneSettingActivity.class);
        this.startActivity(intent);
        UsageReporter.onClick(null, DialpadFragment.TAG, UsageReporter.DialpadPage.SETTING_FROM_DIALPAD);
    }

    @Override
    public void onMenuItemClick(int id) {
        hideDialpadFragment();
        switch (id) {
            case FOOTER_BUTTON_CALL_LOG_DELETE:
                if (mCallLogFragment != null) {
                    mCallLogFragment.deleteCallLog();
                }
                break;
            case FOOTER_BUTTON_CALL_LOG_CONFERENCE_CALL:
                startConferenceCall();
                UsageReporter.onClick(null, DialpadFragment.TAG, UsageReporter.DialpadPage.DP_ENTER_CONFERENCE_CALL);
                break;
            case FOOTER_BUTTON_CALL_LOG_FILTER:
                if (mCallLogFragment != null) {
                    mCallLogFragment.popupFilterDialog();
                    UsageReporter.onClick(null, DialpadFragment.TAG, UsageReporter.DialpadPage.FILTER_CALL_LOG);
                }
                break;
            case FOOTER_BUTTON_CALL_LOG_BACKUP:
                if (mCallLogFragment != null) {
                    mCallLogFragment.syncCallLog();
                    UsageReporter.onClick(null, DialpadFragment.TAG, UsageReporter.DialpadPage.BACKUP_CALL_LOG);
                }
                break;
            case FOOTER_BUTTON_CALL_LOG_INTERCEPT:
                openCallIntercept();
                break;
            case FOOTER_BUTTON_CALL_LOG_SETTING:
                // Launch call settings activity
                startDialerSettingsActivity();
                break;
            case FOOTER_BUTTON_CONTACTS_SETTINGS:
                startSettingActivity();
                break;
            case FOOTER_BUTTON_CONTACTS_NEW:
                AccountFilterManager.getInstance(this)
                        .createNewContactWithPhoneNumberOrEmailAsync(
                                this,
                                null,
                                null,
                                AccountFilterManager.INVALID_REQUEST_CODE);
                UsageReporter.onClick(null, DefaultContactBrowseListFragment.TAG,
                        UsageReporter.ContactsListPage.ACTION_LIST_NEW_CONTACT);
                break;
            case FOOTER_BUTTON_CONTACTS_DELETE:
                if (mAllFragment != null) {
                    mAllFragment.deleteContacts();
                    UsageReporter.onClick(null, DefaultContactBrowseListFragment.TAG,
                            UsageReporter.ContactsListPage.FOOT_DEL_CL);
                }
                break;
            case FOOTER_BUTTON_CONTACTS_SYNC:
                if (mAllFragment != null) {
                    mAllFragment.syncContacts();
                    UsageReporter.onClick(null, DefaultContactBrowseListFragment.TAG,
                            UsageReporter.ContactsListPage.FOOT_SYNC_CL);
                }
                break;
            case FOOTER_BUTTON_CONTACTS_GROUP:
                if (mAllFragment != null) {
                    mAllFragment.handlerGroupFunctionBtn();
                }
                break;
            case FOOTER_BUTTON_CONTACTS_ACCOUNTS_FILTER:
                startAccountAndGroupFilterActivity();
                UsageReporter.onClick(null, DefaultContactBrowseListFragment.TAG,
                        UsageReporter.ContactsListPage.FOOT_ACCOUNT_FILTER_CL);
                break;
            default:
                Log.e(TAG, "ERROR: OnContactsFooterBarButtonClicked(), unhandled button clicked");
                break;
        }
    }

    private void startSettingActivity() {
        Intent intent = new Intent(ContactsSettingActivity.CONTACTS_SETTINGS_ACTION);
        startActivity(intent);
        UsageReporter.onClick(null, DefaultContactBrowseListFragment.TAG, UsageReporter.ContactsListPage.FOOT_SET_CL);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TMPTAG, "PeopleActivity2 onNewIntent called");
        setIntent(intent);
        if (!processIntent(true)) {
            finish();
            return;
        }

        // This function initialize just sets mSearchMode, mQueryString and
        // mCurrentTab
        // configureFragments function will set mSearchMode
        // selectTabToOpen function will set mCurrentTb
        // configureFragments will handle mQueryString
        // mActionBarAdapter.initialize(null, mRequest);

        mContactListFilterController.checkFilterValidity(false);
        mCurrentFilterIsValid = true;
        // Re-configure fragments.
        configureFragments(true /* from request */);
        checkDialpadFrament();

        selectTabToOpen(mTabToOpen, true);
        Log.d(TMPTAG, "PeopleActivity2 onNewIntent end");
    }

    /**
     * Resolve the intent and initialize {@link #mRequest}, and launch another
     * activity if redirect is needed.
     *
     * @param forNewIntent set true if it's called from
     *            {@link #onNewIntent(Intent)}.
     * @return {@code true} if {@link PeopleActivity} should continue running.
     *         {@code false} if it shouldn't, in which case the caller should
     *         finish() itself and shouldn't do farther initialization.
     */
    private boolean processIntent(boolean forNewIntent) {
        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());

        if (getIntent().getBooleanExtra(GadgetProvider.EXTRA_FROM_GADGET, false)) {
            UsageReporter.onClick(null, GadgetProvider.TAG,
                    UsageReporter.GadgetPage.GADGET_PHONE_ENTER_DIALPAD);
            getIntent().removeExtra(GadgetProvider.EXTRA_FROM_GADGET);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, this + " processIntent: forNewIntent=" + forNewIntent + " intent=" + getIntent() + " request="
                    + mRequest);
        }
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            return false;
        }

        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            return false;
        }

        if (mRequest.getActionCode() == ContactsRequest.ACTION_VIEW_CONTACT && !mIsUsingTwoPanes) {
            redirect = new Intent(this, ContactDetailActivity.class);
            redirect.setAction(Intent.ACTION_VIEW);
            redirect.setData(mRequest.getContactUri());
            startActivity(redirect);
            return false;
        }
        return true;
    }

    private void updateTitle() {
        int defaultColor = getResources().getColor(R.color.people_title_txt_color_disable);
        int whiteColor = getResources().getColor(R.color.people_title_txt_color_normal);
        mDialpadTitle.setTextColor(defaultColor);
        mContactTtitle.setTextColor(defaultColor);
        if (mTabPager.getCurrentItem() == TabState.CALLLOG) {
            mDialpadTitle.setTextColor(whiteColor);
        } else if (mTabPager.getCurrentItem() == TabState.ALL) {
            mContactTtitle.setTextColor(whiteColor);
        }
    }

    private OnClickListener mTitleClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.dialpad_title:
                    mTabPager.setCurrentItem(TabState.CALLLOG);
                    break;
                case R.id.contact_title:
                    mTabPager.setCurrentItem(TabState.ALL);
                    break;
            }
        }
    };

    private void initCloudCallButton() {
        ViewStub cb = (ViewStub) findViewById(R.id.cloud_call_btn);
        mCloudCallBtn = (ImageView) cb.inflate();
        if (!mDefaultPrefs.getBoolean(CLOUD_CALL_BEEN_CALLED_KEY, false)) {
            ViewStub cbd = (ViewStub) findViewById(R.id.cloud_call_btn_red_dot);
            mCloudCallBtnRedDot = (ImageView) cbd.inflate();
        } else {
            mCloudBtnHasBeenClicked = true;
        }
        mCloudCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CLOUD_CALL_ACTION);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "Cloud call failed, yuntelephony not found!", e);
                }
                if (!mCloudBtnHasBeenClicked) {
                    if (mCloudCallBtnRedDot != null) {
                        mCloudCallBtnRedDot.setVisibility(View.GONE);
                    }
                    SharedPreferences.Editor editor = mDefaultPrefs.edit();
                    editor.putBoolean(CLOUD_CALL_BEEN_CALLED_KEY, true);
                    editor.apply();
                }
                mCloudBtnHasBeenClicked = true;
                UsageReporter.onClick(null, DialpadFragment.TAG, UsageReporter.DialpadPage.CLOUD_CALL);
            }
        });
    }

    private void createViewsAndFragments(Bundle savedState) {
        final FragmentManager fragmentManager = getSupportFragmentManager();

        // Hide all tabs (the current tab will later be reshown once a tab is
        // selected)
        final FragmentTransaction transaction = fragmentManager.beginTransaction();

        // if (mIsUsingTwoPanes) {
        // mAllFragment = (DefaultContactBrowseListFragment)
        // fragmentManager.findFragmentById(R.id.all_fragment);//.getFragment(R.id.all_fragment);
        // } else {
        if (!mIsUsingTwoPanes) {
            mTabPager = (CustomViewPager) this.findViewById(R.id.tab_pager);
            mTabPagerAdapter = new TabPagerAdapter(fragmentManager);
            mTabPager.setAdapter(mTabPagerAdapter);

            setTabPagerScroller();
            mTabScroller.setDuration(TAB_SCROLL_DURATION);

            //for cloud call
            if (FeatureOptionAssistant.isCloudCallSupportted()) {
                initCloudCallButton();
            }

            mTitleView = findViewById(R.id.header_id);
            mDialpadTitle = (TextView) findViewById(R.id.dialpad_title);
            mDialpadTitle.setOnClickListener(mTitleClick);
            mContactTtitle = (TextView) findViewById(R.id.contact_title);
            mContactTtitle.setOnClickListener(mTitleClick);
            findViewById(R.id.menu_id).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mTabPager.getCurrentItem() == TabState.CALLLOG) {
                        boolean supportConfCall = SimUtil.isVolteEnhancedConfCallSupport(getApplicationContext());
                        if (mCallLogPopMenu == null) {
                            Context context = PeopleActivity2.this;
                            mCallLogPopMenu = PopMenu.build(context, Gravity.TOP);
                            mCallLogPopMenu.setOnIemClickListener(PeopleActivity2.this);
                            mCallLogPopMenu.addItem(FOOTER_BUTTON_CALL_LOG_DELETE, context.getString(R.string.delete));
                            if (supportConfCall) {
                                mCallLogPopMenu.addItem(FOOTER_BUTTON_CALL_LOG_CONFERENCE_CALL,
                                        context.getString(R.string.conference_call_title));
                            }
                            mCallLogPopMenu.addItem(FOOTER_BUTTON_CALL_LOG_FILTER,
                                    context.getString(R.string.dialpad_footer_filter));
                            /* YUNOS BEGIN */
                            //##modules(AliContacts) ##author: hongwei.zhw
                            //##BugID:(8161644) ##date:2016.4.18
                            //##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
                            if (!Build.YUNOS_CARRIER_CMCC){
                                mCallLogPopMenu.addItem(FOOTER_BUTTON_CALL_LOG_BACKUP,
                                        context.getString(R.string.dialpad_footer_backup));
                            }
                            /* YUNOS END */
                            if (AliCallLogExtensionHelper.PLATFORM_YUNOS) {
                                mCallLogPopMenu.addItem(FOOTER_BUTTON_CALL_LOG_INTERCEPT, context.getString(R.string.call_block));
                            }
                            mCallLogPopMenu.addItem(FOOTER_BUTTON_CALL_LOG_SETTING,
                                    context.getString(R.string.dialpad_footer_settings));
                            mCallLogFragment.updateDialpadFooterMenu();
                        }
                        if (supportConfCall) {
                            mCallLogPopMenu.setEnable(FOOTER_BUTTON_CALL_LOG_CONFERENCE_CALL,
                                    SimUtil.isVolteEnabled(getApplicationContext()));
                        }
                        mCallLogPopMenu.show();
                    } else if (mTabPager.getCurrentItem() == TabState.ALL) {
                        if (mContactPopMenu == null) {
                            mContactPopMenu = PopMenu.build(PeopleActivity2.this, Gravity.TOP);
                            mContactPopMenu.setOnIemClickListener(PeopleActivity2.this);
                        }
                        Context context = PeopleActivity2.this;
                        mContactPopMenu.clean();
                        mContactPopMenu.addItem(FOOTER_BUTTON_CONTACTS_NEW, getString(R.string.new_contact));
                        // mContactPopMenu.addItem(FOOTER_BUTTON_CONTACTS_SHOW_GROUP,
                        // getString(R.string.display_group_contact));
                        if (mAllFragment.isShowGroup()) {
                            if (mGroupUri != null) {
                                mContactPopMenu.addItem(FOOTER_BUTTON_CONTACTS_GROUP,
                                        context.getString(R.string.GroupFounctions));
                            } else {
                                mContactPopMenu.addItem(FOOTER_BUTTON_CONTACTS_GROUP,
                                        getString(R.string.GroupFounctions));
                            }
                        } else {
                            mContactPopMenu.addItem(FOOTER_BUTTON_CONTACTS_DELETE,
                                    getString(R.string.contacts_footer_batch_delete));
                            /* YUNOS BEGIN */
                            //##modules(AliContacts) ##author: hongwei.zhw
                            //##BugID:(8161644) ##date:2016.4.18
                            //##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
                            if (!Build.YUNOS_CARRIER_CMCC){
                                mContactPopMenu.addItem(FOOTER_BUTTON_CONTACTS_SYNC,
                                        getString(R.string.contacts_footer_sync));
                            }
                            /* YUNOS END */
                            mContactPopMenu.addItem(FOOTER_BUTTON_CONTACTS_ACCOUNTS_FILTER,
                                    getString(R.string.activity_title_contacts_filter));
                            mContactPopMenu.addItem(FOOTER_BUTTON_CONTACTS_SETTINGS,
                                    getString(R.string.contacts_footer_settings));
                        }
                        mAllFragment.updatePopMenuItem();
                        mContactPopMenu.show();
                    }
                }
            });

            // Configure action bar
            mTabPager.setOnPageChangeListener(mPageChangeListener);
            mActionBarAdapter = new ActionBarAdapter2(this, this, mPageChangeListener, mTabPager);
            mActionBarAdapter.initialize(savedState, mRequest);

            // TODO: consider if we can remove these sentences
            // Can we find the three fragments here????
            mCallLogFragment = (CallLogFragment) fragmentManager.findFragmentByTag(CALLLOG_TAG);
            mAllFragment = (DefaultContactBrowseListFragment) fragmentManager.findFragmentByTag(ALL_TAG);
            Fragment dialFrament = fragmentManager.findFragmentByTag(DIALPAD_TAG);
            if (dialFrament != null) {
                mDialpadFragment = (DialpadFragment) dialFrament;
            }

            if (mAllFragment == null) {
                Log.d(TMPTAG, "PeopleActivity2 need new fragments");
                mCallLogFragment = new CallLogFragment();
                mAllFragment = new DefaultContactBrowseListFragment();

                transaction.add(R.id.tab_pager, mCallLogFragment, CALLLOG_TAG);
                transaction.add(R.id.tab_pager, mAllFragment, ALL_TAG);
            } else {
                Log.d(TMPTAG, "PeopleActivity2 createViewsAndFragments: strange........");
            }
            mAllFragment.setParentViewPager(mTabPager);
            mAllFragment.setOnContactListActionListener(new ContactBrowserActionListener());

            findViewById(R.id.dialpad_button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDialpadFragment();
                }
            });
        }

        // Call this function to get the tab to open
        configTabToOpenFromRequest();

        Log.d(TMPTAG, "PeopleActivity2 createViewsAndFragments mTabToOpen=" + mTabToOpen);
        // Just two entries, one is DIALPAD, another is CONTACT
        if (mTabToOpen == TabState.CALLLOG) {
            transaction.show(mCallLogFragment);
            transaction.hide(mAllFragment);
        } else {
            transaction.hide(mCallLogFragment);
            transaction.show(mAllFragment);
        }

        transaction.commitAllowingStateLoss();
        checkDialpadFrament();
        fragmentManager.executePendingTransactions();
        Log.d(TMPTAG, "PeopleActivity2 createViewsAndFragments end");
    }

    public DialpadFragment getDialpadFragment() {
        return mDialpadFragment;
    }

    public View getTabIndicator() {
        return mTitleView;
    }

    private OnPageChangeListener mPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            onPageItemSeleted(position);
            updateTitle();
        }

    };

    private void onPageItemSeleted(int position) {
        Log.d(TMPTAG, "PeopleActivity2 onPageItemSeleted position " + position);
        if (mTabPagerAdapter.isSearchMode()) {
            return;
        }

        mActionBarAdapter.saveCurrentTab(position);

        // updateFooterBarMenu(position);

        if (position != TabState.CALLLOG) {
            if (mCallLogFragment != null) {
                mCallLogFragment.backToAllRecords();
            }
        }
    }

    public void setMenuItemEnable(int id, boolean enable) {
        switch (id) {
            case FOOTER_BUTTON_CALL_LOG_DELETE:
            case FOOTER_BUTTON_CALL_LOG_FILTER:
            case FOOTER_BUTTON_CALL_LOG_BACKUP:
                if (mCallLogPopMenu != null) {
                    mCallLogPopMenu.setEnable(id, enable);
                }
                break;
            case FOOTER_BUTTON_CONTACTS_DELETE:
                if (mContactPopMenu != null) {
                    mContactPopMenu.setEnable(id, enable);
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        Log.d(TMPTAG, "PeopleActivity2 onStart called");
        if (!mFragmentInitialized) {
            mFragmentInitialized = true;
            /*
             * Configure fragments if we haven't. Note it's a one-shot
             * initialization, so we want to do this in {@link #onCreate}.
             * However, because this method may indirectly touch views in
             * fragments but fragments created in {@link #configureContentView}
             * using a {@link FragmentTransaction} will NOT have views until
             * {@link Activity#onCreate} finishes (they would if they were
             * inflated from a layout), we need to do it here in {@link
             * #onStart()}. (When {@link Fragment#onCreateView} is called is
             * different in the former case and in the latter case,
             * unfortunately.) Also, we skip most of the work in it if the
             * activity is a re-created one. (so the argument.)
             */
            configureFragments(true);

            Log.d(TMPTAG, "PeopleActivity2 onStart 1");

            selectTabToOpen(mTabToOpen, false);

            // The activity is cold launch, we transfer the flag to
            // corresponding fragment
            switch (mTabToOpen) {
                case TabState.CALLLOG:
                    mCallLogFragment.setColdLaunched(true);
                    break;
                case TabState.ALL:
                    mAllFragment.setColdLaunched(true);
                    break;
                default:
                    break;
            }
        } else if (mIsUsingTwoPanes && !mCurrentFilterIsValid) {
            // We only want to do the filter check in onStart for wide screen
            // devices where it
            // is often possible to get into single contact mode. Only do this
            // check if
            // the filter hasn't already been set properly (i.e. onCreate or
            // onActivityResult).

            // Since there is only one {@link ContactListFilterController}
            // across multiple
            // activity instances, make sure the filter controller is in sync
            // withthe current
            // contact list fragment filter.
            mContactListFilterController.setContactListFilter(mAllFragment.getFilter(), true);
            mContactListFilterController.checkFilterValidity(true);
            mCurrentFilterIsValid = true;
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TMPTAG, "PeopleActivity2 onResume called");
        super.onResume();

        // Re-register the listener, which may have been cleared when
        // onSaveInstanceState was
        // called. See also: onSaveInstanceState
        mActionBarAdapter.setListener(this);

        // Current tab may have changed since the last onSaveInstanceState().
        // Make sure the actual contents match the tab.
        updateFragmentsVisibility();

        doPeriodicCheck();
        Log.d(TMPTAG, "PeopleActivity2 onResume end");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCurrentFilterIsValid = false;

        if (mOSUpdate != null) {
            mOSUpdate.stopCheck();
            mOSUpdate = null;
        }

        if (mActionBarAdapter.getCurrentTab() == TabState.CALLLOG) {
            if (mCallLogFragment != null) {
                mCallLogFragment.backToAllRecords();
                mCallLogFragment.clearDialog();
            }
        }
        if (mCallLogPopMenu != null) {
            mCallLogPopMenu.immediateDismiss();
        }
        if (mContactPopMenu != null) {
            mContactPopMenu.immediateDismiss();
        }
        if (mAllFragment != null) {
            mAllFragment.clearDialog();
        }
        // if (mFooterBarMenu != null && mFooterBarMenu.isShown()) {
        // mFooterBarMenu.dismissPopup();
        // }
    }

    @Override
    protected void onDestroy() {
        Log.d(TMPTAG, "PeopleActivity2 onDestroy called");
        mFragmentInitialized = false;

        // Some of variables will be null if this Activity redirects Intent.
        // See also onCreate() or other methods called during the Activity's
        // initialization.
        if (mActionBarAdapter != null) {
            mActionBarAdapter.setListener(null);
        }
        if (mContactListFilterController != null) {
            mContactListFilterController.removeListener(this);
        }
        super.onDestroy();
        unregisterReceiver(mHomeKeyDismissDialogReceiver);
        AccountFilterManager.getInstance(this).dismissAccountSelectDialog();
        Log.d(TMPTAG, "PeopleActivity2 onDestroy end");
    }

    private void doPeriodicCheck() {
        // check os update
        checkUpdate();
        PreloadContactUtil.checkPreloadContact(getApplicationContext(), mDefaultPrefs);
    }

    private void checkUpdate() {
        if (mLastUpdateTime == 0) {
            mLastUpdateTime = mDefaultPrefs.getLong(YUNOS_OS_LAST_UPDATE_TIME, 0);
        }

        long now = System.currentTimeMillis();
        Log.d(TAG, "checkUpdate() now:" + now + ", mLastUpdateTime:" + mLastUpdateTime);
        if (now - mLastUpdateTime >= UPDATE_CYCLE) {
            startOSUpdateCheck();

            PreferencesUtils.commitLongSharedPreferencesInBackground(mDefaultPrefs, YUNOS_OS_LAST_UPDATE_TIME, now);
            mLastUpdateTime = now;
        }
    }

    // Get the tab to open from the request
    // Note: This function must be called after processIntent function
    private void configTabToOpenFromRequest() {
        int actionCode = mRequest.getActionCode();
        switch (actionCode) {
            case ContactsRequest.ACTION_ALL_CONTACTS:
            case ContactsRequest.ACTION_CONTACTS_WITH_PHONES:
            case ContactsRequest.ACTION_FREQUENT:
            case ContactsRequest.ACTION_STREQUENT:
            case ContactsRequest.ACTION_STARRED:
            case ContactsRequest.ACTION_VIEW_CONTACT:
            case ContactsRequest.ACTION_GROUP:
                mTabToOpen = TabState.ALL;
                break;
            case ContactsRequest.ACTION_VIEW_DIALPAD:
                mTabToOpen = TabState.CALLLOG;
                break;
            // Default, open the contact?
            default:
                Log.e(TAG, "configTabToOpenFromRequest: mRequest.getActionCode=" + actionCode);
                mTabToOpen = TabState.ALL;
                break;
        }
    }

    private void configureFragments(boolean fromRequest) {
        if (fromRequest) {
            ContactListFilter filter = null;
            int actionCode = mRequest.getActionCode();
            boolean searchMode = mRequest.isSearchMode();
            String queryString = mRequest.getQueryString();
            switch (actionCode) {
                case ContactsRequest.ACTION_ALL_CONTACTS:
                    filter = ContactListFilter
                            .createFilterWithType(ContactListFilter.FILTER_TYPE_CUSTOM);
                    break;
                case ContactsRequest.ACTION_CONTACTS_WITH_PHONES:
                    filter = ContactListFilter.createFilterWithType(ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY);
                    break;
                default:
                    break;
            }
            configTabToOpenFromRequest();

            if (filter != null) {
                mContactListFilterController.setContactListFilter(filter, false);
                searchMode = false;
            }

            if (mRequest.getContactUri() != null) {
                searchMode = false;
            }

            // It looks like some intents can query some string, so show the
            // query string here.
            // But I didn't see such intent until now. And we did't handle the
            // query string in
            // current code. Just keep the origin logic.
            // Maybe we need handle this case in the future.
            // TODO
            if (searchMode && !TextUtils.isEmpty(queryString)) {
                mActionBarAdapter.setQueryString(queryString);
            }

            mActionBarAdapter.setSearchMode(searchMode);

            configureContactListFragmentForRequest();
        }

        configureContactListFragment();
    }

    public void selectTabToOpen(final int tab, boolean afterInitilized) {
        final int currentTab = mActionBarAdapter.getCurrentTab();
        Log.d(TMPTAG, "PeopleActivity2 selectTabToOpen called, currenttab=" + currentTab + ", tabToOpen=" + tab);

        if (mTabPagerAdapter != null && mTabPagerAdapter.isSearchMode()) {
            // If the activity is launched from dialpad or contacts, the yellow
            // page search mode will be reset.
            if (mAllFragment != null && mTabPagerAdapter.mSearchTab == TabState.ALL) {
                mAllFragment.backToNormalState();
            }
        }

        if ((currentTab == tab) && afterInitilized) {
            Log.d(TMPTAG, "PeopleActivity2 selectTabToOpen return here");
            return;
        }

        // Switch current tab to this tab
        // Needn't display animation in this case, so set the duration to 0
        mTabScroller.setDuration(0);
        mTabPager.setCurrentItem(tab);
        mActionBarAdapter.saveCurrentTab(tab);
        // Restore the animation
        mTabScroller.setDuration(TAB_SCROLL_DURATION);
    }

    // This function will be called when the hot fragment is ready
    // It will do other fragment initial, createview, start, resume...
    public void postOtherFragmentInitialStart(final int tab) {
        Log.d(TMPTAG, "PeopleActivity2 postOtherFragmentInitialStart tab = " + tab);
        switch (tab) {
            case TabState.ALL:
                mCallLogFragment.postInitialStart();
                break;
            case TabState.CALLLOG:
                mAllFragment.postInitialStart();
                break;
        }
    }

    public int getCurrentTab() {
        return mActionBarAdapter.getCurrentTab();
    }

    @Override
    public void onContactListFilterChanged() {
        if (mAllFragment == null || !mAllFragment.isAdded()) {
            return;
        }

        mAllFragment.setFilter(mContactListFilterController.getFilter());
    }

    /**
     * Handler for action bar actions.
     */
    @Override
    public void onAction(int action) {
        switch (action) {
            case ActionBarAdapter2.Listener.Action.START_SEARCH_MODE:
                // Tell the fragments that we're in the search mode
                configureFragments(false /* from request */);
                updateFragmentsVisibility();
                break;
            case ActionBarAdapter2.Listener.Action.STOP_SEARCH_MODE:
                setQueryTextToFragment("");
                updateFragmentsVisibility();
                break;
            case ActionBarAdapter2.Listener.Action.CHANGE_SEARCH_QUERY:
                final String queryString = mActionBarAdapter.getQueryString();
                setQueryTextToFragment(queryString);
                break;
            default:
                throw new IllegalStateException("Unkonwn ActionBarAdapter2 action: " + action);
        }
    }

    // Never called, this is a nouse interface. :(
    @Override
    public void onSelectedTabChanged() {
    }

    /**
     * Updates the fragment/view visibility according to the current mode, such
     * as {@link ActionBarAdapter#isSearchMode()} and
     * {@link ActionBarAdapter#getCurrentTab()}.
     */
    private void updateFragmentsVisibility() {
        // TODO, strange logic.
        // tab = mActionBarAdapter.getCurrentTab()
        // mActionBarAdapter.getCurrentTab() != tab)
        // I can't understand........., keep old logic
        int tab = mActionBarAdapter.getCurrentTab();
        // We use ViewPager on 1-pane.
        if (!mIsUsingTwoPanes) {
            if (!mActionBarAdapter.isSearchMode()) {
                // No smooth scrolling if quitting from the search mode.
                if (mTabPager.getCurrentItem() != tab) {
                    mTabPager.setCurrentItem(tab);
                }
            }
        }
    }

    private void setQueryTextToFragment(String query) {
        mAllFragment.setQueryString(query, true);
    }

    private void configureContactListFragmentForRequest() {
        Uri contactUri = mRequest.getContactUri();
        if (contactUri != null) {
            // For an incoming request, explicitly require a selection if we are
            // on 2-pane UI,
            // (i.e. even if we view the same selected contact, the contact may
            // no longer be
            // in the list, so we must refresh the list).
            if (mIsUsingTwoPanes) {
                mAllFragment.setSelectionRequired(true);
            }
            mAllFragment.setSelectedContactUri(contactUri);
        }

        mAllFragment.setFilter(mContactListFilterController.getFilter());
        setQueryTextToFragment(mActionBarAdapter.getQueryString());

        if (mRequest.isDirectorySearchEnabled()) {
            mAllFragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DEFAULT);
        } else {
            mAllFragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
        }
    }

    private void configureContactListFragment() {

        // Filter may be changed when this Activity is in background.
        mAllFragment.setFilter(mContactListFilterController.getFilter());

        mAllFragment.setVerticalScrollbarPosition(mIsUsingTwoPanes ? View.SCROLLBAR_POSITION_LEFT
                : View.SCROLLBAR_POSITION_RIGHT);
        mAllFragment.setSelectionVisible(mIsUsingTwoPanes);
        mAllFragment.setQuickContactEnabled(!mIsUsingTwoPanes);
    }

    private final class ContactBrowserActionListener implements OnContactBrowserActionListener {
        ContactBrowserActionListener() {
        }

        @Override
        public void onSelectionChange() {
        }

        @Override
        public void onViewContactAction(Uri contactLookupUri, boolean isSimContact) {
            Intent intent = new Intent(Intent.ACTION_VIEW, contactLookupUri);
            intent.setClass(PeopleActivity2.this, ContactDetailActivity.class);
            intent.putExtra(ContactDetailActivity.EXTRA_KEY_IS_SIM_CONTACT, isSimContact);
            startActivity(intent);
        }

        @Override
        public void onCreateNewContactAction() {
            if (isFinishing() || isDestroyed()) {
                Log.w(TAG, "onCreateNewContactAction: activity is not active. quit.");
                return;
            }
            AccountFilterManager.getInstance(PeopleActivity2.this)
                    .createNewContactWithPhoneNumberOrEmailAsync(
                            PeopleActivity2.this,
                            null,
                            null,
                            AccountFilterManager.INVALID_REQUEST_CODE);
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.setClass(PeopleActivity2.this, ContactEditorActivity.class);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivityForResult(intent, SUBACTIVITY_EDIT_CONTACT);
        }

        @Override
        public void onAddToFavoritesAction(Uri contactUri) {
            ContentValues values = new ContentValues(1);
            values.put(Contacts.STARRED, 1);
            getContentResolver().update(contactUri, values, null, null);
        }

        @Override
        public void onRemoveFromFavoritesAction(Uri contactUri) {
            ContentValues values = new ContentValues(1);
            values.put(Contacts.STARRED, 0);
            getContentResolver().update(contactUri, values, null, null);
        }

        @Override
        public void onCallContactAction(Uri contactUri) {
            PhoneNumberInteraction2.startInteractionForPhoneCall(PeopleActivity2.this, contactUri);
        }

        @Override
        public void onSmsContactAction(Uri contactUri) {
            PhoneNumberInteraction2.startInteractionForTextMessage(PeopleActivity2.this, contactUri);
        }

        @Override
        public void onDeleteContactAction(Uri contactUri) {
            ContactDeletionInteraction.start(PeopleActivity2.this, contactUri, false);
        }

        @Override
        public void onFinishAction() {
            resetOnQuit();
        }

        @Override
        public void onInvalidSelection() {
            ContactListFilter filter;
            ContactListFilter currentFilter = mAllFragment.getFilter();
            if (currentFilter != null && currentFilter.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
                filter = ContactListFilter.createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS);
                mAllFragment.setFilter(filter);
            } else {
                filter = ContactListFilter.createFilterWithType(ContactListFilter.FILTER_TYPE_SINGLE_CONTACT);
                mAllFragment.setFilter(filter, false);
            }
            mContactListFilterController.setContactListFilter(filter, true);
        }

        @Override
        public void onGetContactCount(int count) {
            SimContactUtils.setPhoneContactCount(count);
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SUBACTIVITY_NEW_CONTACT:
            case SUBACTIVITY_EDIT_CONTACT: {
                if (resultCode == RESULT_OK && mIsUsingTwoPanes) {
                    mRequest.setActionCode(ContactsRequest.ACTION_VIEW_CONTACT);
                    mAllFragment.setSelectionRequired(true);
                    mAllFragment.setSelectedContactUri(data.getData());
                    // Suppress IME if in search mode
                    if (mActionBarAdapter != null) {
                        mActionBarAdapter.clearFocusOnSearchView();
                    }
                    // No need to change the contact filter
                    mCurrentFilterIsValid = true;
                }
                break;
            }

            /*case SUBACTIVITY_NEW_GROUP:
            case SUBACTIVITY_EDIT_GROUP: {
                if (resultCode == RESULT_OK && mIsUsingTwoPanes) {
                    mRequest.setActionCode(ContactsRequest.ACTION_GROUP);
                    // mGroupsFragment.setSelectedUri(data.getData());
                }
                break;
            }*/

            // TODO: Using the new startActivityWithResultFromFragment API this
            // should not be needed
            // anymore
            case ContactEntryListFragment.ACTIVITY_REQUEST_CODE_PICKER:
                if (resultCode == RESULT_OK) {
                    mAllFragment.onPickerResult(data);
                }
                break;

            // case CooTekPhoneService.REQUEST_CODE_RELOAD:
            // if (resultCode == RESULT_OK) {
            // if (mYpMainFragment != null) {
            // mYpMainFragment.reload();
            // }
            // }
            // break;

            case CallLogFragment.QUICK_CALL_ACTIVITY_REQUEST_CODE:
                if (resultCode == RESULT_OK && mDialpadFragment != null) {
                    mDialpadFragment.updateMatchAdapterQuickCallItem();
                }
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {

            case KeyEvent.KEYCODE_DEL: {
                if (deleteSelection()) {
                    return true;
                }
                break;
            }
            default: {
                // Bring up the search UI if the user starts typing
                final int unicodeChar = event.getUnicodeChar();
                if ((unicodeChar != 0)
                // If COMBINING_ACCENT is set, it's not a unicode
                // character.
                        && ((unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0) && !Character.isWhitespace(unicodeChar)) {
                    String query = new String(new int[] {
                        unicodeChar
                    }, 0, 1);
                    if (!mActionBarAdapter.isSearchMode()) {
                        mActionBarAdapter.setQueryString(query);
                        mActionBarAdapter.setSearchMode(true);
                        return true;
                    }
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        resetOnQuit();
        // YUNOS BEGIN
        // Description: if dialpad is show then hidden dialpad
        // author:changjun.bcj
        // date:2015-10-16
        if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
            if (mDialpadFragment.isShowDialpad() && !TextUtils.isEmpty(mDialpadFragment.getPhoneNumber())) {
                mDialpadFragment.hiddenDialpad(true);
            } else {
                hideDialpadFragment();
            }
            return;
        }
        // END

        if (isTaskRoot() && UiTools.isFromHomeShell(getActivityToken(), getIntent())) {
            Log.i(TAG, "onBackPressed: switch to home shell.");
            UiTools.switchToHomeShell(this);
        } else {
            Log.i(TAG, "onBackPressed: finish.");
            //super.onBackPressed();
            /*
             * Fix APR bug : id is 5931068.
             * call super.onBackPressed() will call FragmentManagerImpl.popBackStackImmediate(),
             * but PeopleActivity do not need pop up back fragment when press back key.
             * Just finish() iteself.
             */
            finish();
        }
    }

    private void resetOnQuit() {
        if (mActionBarAdapter.isSearchMode()) {
            mActionBarAdapter.setSearchMode(false);
        }
    }

    private boolean deleteSelection() {
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mActionBarAdapter.onSaveInstanceState(outState);

        // Clear the listener to make sure we don't get callbacks after
        // onSaveInstanceState,
        // in order to avoid doing fragment transactions after it.
        // TODO Figure out a better way to deal with the issue.
        mActionBarAdapter.setListener(null);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // In our own lifecycle, the focus is saved and restore but later taken
        // away by the
        // ViewPager. As a hack, we force focus on the SearchView if we know
        // that we are searching.
        // This fixes the keyboard going away on screen rotation
        if (mActionBarAdapter.isSearchMode()) {
            mActionBarAdapter.setFocusOnSearchView();
        }

        mGroupUri = savedInstanceState.getParcelable(EXTRA_KEY_SELECTED_GROUP);
    }

    public DefaultContactBrowseListFragment getListFragment() {
        return mAllFragment;
    }

    private int mTabToOpen = -1;

    public boolean onHandleActionBarSearchFocusStatus(boolean hasFocus) {
        return false;
    }

    public void setSearchMode(boolean searchMode, int tabState) {
        mTabPagerAdapter.setSearchMode(searchMode, tabState);
        if (searchMode) {
            mTabPager.setScanScroll(false);
            setSystembarColor(getResources().getColor(R.color.aui_bg_color_white), searchMode);
            hideDialpadFragment();
        } else {
            mTabPager.setScanScroll(true);
            setSystembarColor(getResources().getColor(R.color.title_color), getResources().getBoolean(R.bool.contact_dark_mode));
        }
    }

    public boolean onHandleActionBarSearchChanged(CharSequence query) {
        int currentTab = mActionBarAdapter.getCurrentTab();
        if (currentTab == TabState.ALL) {
            if (!mActionBarAdapter.isSearchMode() && !TextUtils.isEmpty(query)) {
                mActionBarAdapter.setSearchMode(true);
            } else if (!TextUtils.isEmpty(query)) {
                mActionBarAdapter.setSearchMode(false);
                return true;
            }
            return mActionBarAdapter.onQueryTextChange(query.toString());
        }
        return false;
    }

    /* YUNOS BEGIN */
    // ## description: TabPage for dev_3.0
    // @author: fangjun.lin@alibaba-inc.com
    // @date: 2014/02/10
    /**
     * Adapter for the {@link ViewPager}. Unlike {@link FragmentPagerAdapter},
     * {@link #instantiateItem} returns existing fragments, and
     * {@link #instantiateItem}/ {@link #destroyItem} show/hide fragments
     * instead of attaching/detaching. In search mode, we always show the "all"
     * fragment, and disable the swipe. We change the number of items to 1 to
     * disable the swipe. TODO figure out a more straight way to disable swipe.
     */
    private class TabPagerAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;

        private boolean mTabPagerAdapterSearchMode;

        // To distinguish current search mode:
        // if mSearchTab==TabState.All, Contacts list search mode;
        // if mSearchTab==TabState.YellowPage, Yellow page search mode.
        private int mSearchTab = -1;

        private Fragment mCurrentPrimaryItem;

        public TabPagerAdapter(FragmentManager fm) {
            super();
            mFragmentManager = fm;
        }

        public boolean isSearchMode() {
            return mTabPagerAdapterSearchMode;
        }

        public void setSearchMode(boolean searchMode, int tabState) {
            if (searchMode == mTabPagerAdapterSearchMode) {
                if (mSearchTab != tabState) {
                    Log.w(TAG, "Error:setSearchMode: mSearchTab=" + mSearchTab + ",tabState:" + tabState);
                }
                return;
            }
            mSearchTab = tabState;
            mTabPagerAdapterSearchMode = searchMode;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return TabState.COUNT;
        }

        /** Gets called when the number of items changes. */
        @Override
        public int getItemPosition(Object object) {
            if (object == mCallLogFragment) {
                return TabState.CALLLOG;
            }
            if (object == mAllFragment) {
                return TabState.ALL;
            }
            return POSITION_NONE;
        }

        @Override
        public void startUpdate(ViewGroup container) {
        }

        private Fragment getFragment(int position) {
            if (position == TabState.CALLLOG) {
                return mCallLogFragment;
            } else if (position == TabState.ALL) {
                return mAllFragment;
            }

            throw new IllegalArgumentException("position: " + position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            mCurTransaction.show(f);

            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.hide((Fragment) object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                    if ((mDialpadFragment == null) || TextUtils.isEmpty(mDialpadFragment.getPhoneNumber())) {
                        hideDialpadFragment();
                    }
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                    UsageReporter.onPause(null, mCurrentPrimaryItem.getClass().getSimpleName());
                }
                // If switch to current tab, needs to be refreshed.
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);

                    UsageReporter.onResume(null, fragment.getClass().getSimpleName());
                }

                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title = PeopleActivity2.this.getResources().getString(TAB_LABEL_IDS[position]);
            return title;
        }
    }

    /* YUNOS END */

    /*
     * private OnSearchChangeListener mSearchChangeListener = new
     * OnSearchChangeListener() {
     * @Override public void onSearchChanged(String query) {
     * onHandleActionBarSearchChanged(query); } };
     */

    // It will be call when slide finger on dialpad
    public void setCurrentTab(int tab) {
        mTabPager.setCurrentItem(tab);
    }

    private void setTabPagerScroller() {
        try {
            Field mScroller = null;
            mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true);
            mTabScroller = new PeopleTabScroller(mTabPager.getContext(), sInterpolator);
            mScroller.set(mTabPager, mTabScroller);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "setTabPagerScroller: NoSuchFieldException e = " + e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "setTabPagerScroller: IllegalArgumentException e = " + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "setTabPagerScroller: IllegalAccessException e = " + e);
        }
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            // _o(t) = t * t * ((tension + 1) * t + tension)
            // o(t) = _o(t - 1) + 1
            t -= 1.0f;
            return t * t * t + 1.0f;
        }
    };

    private void startOSUpdateCheck() {
        if (mOSUpdate != null) {
            return;
        }
        mOSUpdate = new YunOSFeatureHelper.OSUpdateCheckProxy(this);
        mOSUpdate.bindOSUpdateCheckService();
    }

    private void startAccountAndGroupFilterActivity() {
        Intent intent = new Intent(this, CustomContactListFilterActivity.class);
        startActivity(intent);
    }

    public void notifyCallLogChangedToDialpadFragment() {
        if ((mDialpadFragment == null) || (!mDialpadFragment.isVisible())) {
            Log.i(TAG, "notifyCallLogChangedToDialpadFragment: dialpad fragment is null or not visible.");
            return;
        }
        mDialpadFragment.onSearchTableChanged();
    }

}
