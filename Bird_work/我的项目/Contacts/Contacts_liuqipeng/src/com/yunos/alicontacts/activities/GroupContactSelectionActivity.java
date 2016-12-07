/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.ContentUris;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract.Groups;
import android.util.Log;
import android.view.View;

import com.yunos.alicontacts.ContactPickerActivity;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.dialpad.smartsearch.NameConvertWorker;
import com.yunos.alicontacts.list.ContactEntryListFragment;
import com.yunos.alicontacts.list.ContactsIntentResolver;
import com.yunos.alicontacts.list.ContactsRequest;
import com.yunos.alicontacts.list.PhoneNumberMultiplePickerFragment;
import com.yunos.alicontacts.list.PhoneNumberMultiplePickerFragment.onPhoneListActionListener;

import yunos.support.v4.app.Fragment;


/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
public class GroupContactSelectionActivity extends ContactPickerActivity implements onPhoneListActionListener {

    private static final String TAG = "GroupContactSelectionActivity";

    public static final String ACTION_PICK_MULTIPLE = "android.intent.action.PICK_MULTIPLE_GROUP";
    public static final String PICK_RECIPIENT_LIMIT = "pick_contacts_limit";
    public static final String PICK_CONTENT = "pick_content";
    public static final String PICK_PHONE_NUMBER = "pick_phone_number";
    public static final String PICK_PHONE_NUMBER_IN_GROUP = "pick_phone_number_in_group";
    public static final String PICK_CONTACT_TO_DELETE = "pick_contact_to_delete";
    public static final String PICK_CONTACT_TO_STARRED = "pick_contact_to_starred";
    public static final String PICK_CONTACT_ADD_TO_GROUP = "pick_contact_add_to_group";
    public static final String PICK_CONTACT_IN_GROUP_TO_RM = "pick_contact_in_group_to_rm";
    public static final String EXTRA_STARRED_URIS = "extra_starred_uris";
    public static final String EXTRA_GROUP_MEM_IDS = "group_mem_ids";
    public static final String EXTRA_GROUP_ID = "group_id";

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    private ContactsIntentResolver mIntentResolver;
    protected ContactEntryListFragment<?> mListFragment;

    private int mActionCode = -1;
    //private GroupBrowseListFragment mGroupsFragment;
    //private Parcelable mGroupUri;
    //private SlidingMenu mSlidingMenu;
    //private long mGroupID = -1;
    //private String mIDs;

    private ContactsRequest mRequest;

    //private Handler mHandler = new Handler();

    public GroupContactSelectionActivity() {
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactEntryListFragment<?>) {
            mListFragment = (ContactEntryListFragment<?>) fragment;
            setupActionListener();
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            mActionCode = savedState.getInt(KEY_ACTION_CODE);
        }

        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            finish();
            return;
        }

        NameConvertWorker.getInstance().init(this);
        setActivityContentView(R.layout.contact_picker);

        if (mActionCode != mRequest.getActionCode()) {
            mActionCode = mRequest.getActionCode();
            configureListFragment();
        }

        findViewById(R.id.search_view).setVisibility(View.GONE);
//        setupSlidingMenu();
        showBackKey(true);
    }

    @Override
    public void addFooterView(View v) {
        mRootLyaout.addView(v);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        int appMaxPicknumber = this.getIntent().getIntExtra(PICK_RECIPIENT_LIMIT,
                PhoneNumberMultiplePickerFragment.MAX_PICK_NUMBER);
        switch (mActionCode) {
            case ContactsRequest.ACTION_PICK_MULTIPLE_PHONE_NUMBER: {
                PhoneNumberMultiplePickerFragment fragment = new PhoneNumberMultiplePickerFragment();
                fragment.setMaxPickNumber(appMaxPicknumber);
                Parcelable[] uris = mRequest.getContactUriArray();
                fragment.setSelectedUris(uris);
                fragment.setOnPhoneListActionListener(this);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_MULTIPLE_PHONE_NUMBER_IN_GOURP: {
                PhoneNumberMultiplePickerFragment fragment = new PhoneNumberMultiplePickerFragment();
                fragment.setMaxPickNumber(appMaxPicknumber);

                final String IDs = mRequest.getQueryString();
                final long groupID = mRequest.getGroupID();

                final Parcelable groupUri = ContentUris.withAppendedId(Groups.CONTENT_URI, groupID);
                fragment.setOnPhoneListActionListener(this);
                if (groupID != -1) {
                    //fragment.setGroupIDsFilter(mIDs);
                    fragment.setGroupUri(groupUri);
                }
                fragment.setGroupIDsFilter(IDs);
                mListFragment = fragment;

                break;
            }

            default:
                // throw new IllegalStateException("Invalid action code: " + mActionCode);
                Log.e(TAG, "Invalid action code: " + mActionCode);
                finish();
                return;
        }

        mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

        getSupportFragmentManager().beginTransaction().replace(R.id.list_container, mListFragment)
                .commitAllowingStateLoss();
    }

    public void setupActionListener() {

        if (mListFragment instanceof PhoneNumberMultiplePickerFragment) {
                return;
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
    }

//
//    private void setupSlidingMenu() {
//        // set the SlidingMenu
//        mGroupsFragment = new GroupBrowseListFragment();
//      mGroupsFragment.setListener(new GroupBrowserActionListener());
//
//        if (mGroupUri != null) {
//          mGroupsFragment.setSelectedUri((Uri)mGroupUri);
//        }
//        mGroupsFragment.setSelectionVisible(true);
//        setBehindContentView(R.layout.menu_frame);
//        getSupportFragmentManager()
//        .beginTransaction()
//        .replace(R.id.menu_frame, mGroupsFragment)
//        .commit();
//
//      mSlidingMenu = getSlidingMenu();
//      mSlidingMenu.setOnOpenedListener(this);
//      mSlidingMenu.setOnClosedListener(this);
//      mSlidingMenu.setSlidingEnabled(false);
//    }

//    public boolean onHandleActionBarHomeClick() {
//        if (this.getResources().getBoolean(R.bool.config_support_group)) {
//          mGroupsFragment.hideFooterBar();
//          toggle();
//        }
//        return true;
//    }

    @Override
    public void onGetContactCount(int count) {
//      mGroupsFragment.updateGroupListFirstCount(count);
    }

}
