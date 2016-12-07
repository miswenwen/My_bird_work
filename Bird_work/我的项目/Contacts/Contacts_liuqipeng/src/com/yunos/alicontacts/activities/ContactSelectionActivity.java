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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract.Intents.Insert;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.yunos.alicontacts.ContactPickerActivity;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.list.AccountFilterManager;
import com.yunos.alicontacts.list.ContactEntryListFragment;
import com.yunos.alicontacts.list.ContactListFilter;
import com.yunos.alicontacts.list.ContactPickerFragment;
import com.yunos.alicontacts.list.ContactsIntentResolver;
import com.yunos.alicontacts.list.ContactsRequest;
import com.yunos.alicontacts.list.DirectoryListLoader;
import com.yunos.alicontacts.list.EmailAddressPickerFragment;
import com.yunos.alicontacts.list.MultiSelectPeopleFragment;
import com.yunos.alicontacts.list.OnContactPickerActionListener;
import com.yunos.alicontacts.list.OnEmailAddressPickerActionListener;
import com.yunos.alicontacts.list.OnPhoneNumberPickerActionListener;
import com.yunos.alicontacts.list.OnPostalAddressPickerActionListener;
import com.yunos.alicontacts.list.PhoneNumberMultiplePickerFragment;
import com.yunos.alicontacts.list.PhoneNumberPickerFragment;
import com.yunos.alicontacts.list.PostalAddressPickerFragment;
import com.yunos.alicontacts.sim.SimContactCache;
import com.yunos.alicontacts.sim.SimContactEditorFragment;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.widget.ContextMenuAdapter;
import com.yunos.common.UsageReporter;

import hwdroid.widget.searchview.SearchView;
import hwdroid.widget.searchview.SearchView.SearchViewListener;
import yunos.support.v4.app.Fragment;

/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
public class ContactSelectionActivity extends ContactPickerActivity
        implements SearchViewListener,
                 OnFocusChangeListener{
    private static final String TAG = "ContactSelectionActivity";

    public static final String ACTION_PICK_MULTIPLE = "android.intent.action.PICK_MULTIPLE";
    public static final String PICK_CONTENT = "pick_content";
    public static final String PICK_CONTACT_TO_DELETE = "pick_contact_to_delete";
    public static final String PICK_CONTACT_ADD_TO_GROUP = "pick_contact_add_to_group";
    public static final String PICK_CONTACT_IN_GROUP_TO_RM = "pick_contact_in_group_to_rm";
    /*YunOS BEGIN PB*/
    //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
    //##BugID:(8466294) ##date:2016-7-22 09:00
    //##description:suppot export some contacts to vcard
    public static final String PICK_CONTACT_TO_EXPORT = "pick_contact_to_export";
    /*YUNOS END PB*/

    public static final String EXTRA_GROUP_MEM_IDS = "group_mem_ids";
    public static final String EXTRA_GROUP_ID = "group_id";
    public static final String EXTRA_LIST_POSITION = "extra_list_position";
    public static final String KEY_EXTRA_CURRENT_FILTER = "currentFilter";

    private static final int SUBACTIVITY_ADD_TO_EXISTING_CONTACT = 0;

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    // Delay to allow the UI to settle before making search view visible
    private static final int FOCUS_DELAY = 200;

    private ContactsIntentResolver mIntentResolver;
    protected ContactEntryListFragment<?> mListFragment;

    private int mActionCode = -1;

    private ContactsRequest mRequest;
    private SearchView mSearchView;

    /**
     * Can be null. If null, the "Create New Contact" button should be on the menu.
     */
    // private View mCreateNewContactButton;

    private ContactListFilter mCurrentFilter;

    public ContactSelectionActivity() {
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
        setActivityContentView(R.layout.contact_picker);
        configureActivityTitle();

        mCurrentFilter = getIntent().getParcelableExtra(KEY_EXTRA_CURRENT_FILTER);

        if (mActionCode != mRequest.getActionCode()) {
            mActionCode = mRequest.getActionCode();
            configureListFragment();
            // Workaround, if we can't handle this action code, just return quietly.
            if (mActionCode == ContactsRequest.ACTION_DEFAULT) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }
        prepareSearchViewAndActionBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UsageReporter.onResume(this, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UsageReporter.onPause(this, null);
    }

    @Override
    public void addFooterView(View v) {
        mRootLyaout.addView(v);
    }

    private void prepareSearchViewAndActionBar() {
        // Postal address pickers (and legacy pickers) don't support search, so just show
        // "HomeAsUp" button and title.
        showBackKey(true);
        if (mRequest.getActionCode() == ContactsRequest.ACTION_PICK_POSTAL) {
            final ActionBar actionBar = this.getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
            }
            return;
        }

        mSearchView = (SearchView) findViewById(R.id.search_view);
        mSearchView.setQueryHint(getString(R.string.hint_findContacts));
        mSearchView.setSearchViewListener(this);
        mSearchView.setAnchorView(mActionBarView);
        mSearchView.setBackgroundColor(getResources().getColor(R.color.aui_bg_color_white));

        // This is a hack to prevent the search view from grabbing focus
        // at this point. If search view were visible, it would always grabs focus
        // because it is the first focusable widget in the window.
        mSearchView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSearchView.setVisibility(View.VISIBLE);
            }
        }, FOCUS_DELAY);
        //}

        // Clear focus and suppress keyboard show-up.
        // actionBar.mSearchView.clearFocus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If we want "Create New Contact" button but there's no such a button in the layout,
        // try showing a menu for it.
//      ZH:Do not need to create option menu.
//        if (shouldShowCreateNewContactButton() && mCreateNewContactButton == null) {
//            MenuInflater inflater = getMenuInflater();
//            inflater.inflate(R.menu.contact_picker_options, menu);
//        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
    }

    private void configureActivityTitle() {
        if (mRequest.getActivityTitle() != null) {
            setTitle(mRequest.getActivityTitle());
            return;
        }

        int actionCode = mRequest.getActionCode();
        switch (actionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                setTitle2(getResources().getString(R.string.contactPickerActivityTitle));
                break;
            }

            case ContactsRequest.ACTION_PICK_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                setTitle(R.string.shortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                setTitle(R.string.callShortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                setTitle(R.string.messageShortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
        }
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        switch (mActionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setEditMode(true);
                /*YunOS BEGIN PB*/
                //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
                //##BugID:(8240454) ##date:2016-5-13 09:00
                //##description:when add contacts from mms,can not create new contact
                fragment.setCreateContactEnabled(true);
                /*YUNOS END PB*/
                fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
                getFooterBarImpl().setVisibility(View.GONE);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                getFooterBarImpl().setVisibility(View.GONE);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                getFooterBarImpl().setVisibility(View.GONE);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                getFooterBarImpl().setVisibility(View.GONE);
                fragment.setShortcutRequested(true);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                PhoneNumberPickerFragment fragment = new PhoneNumberPickerFragment();
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                mListFragment = new EmailAddressPickerFragment();
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                PhoneNumberPickerFragment fragment = new PhoneNumberPickerFragment();
                fragment.setShortcutAction(Intent.ACTION_CALL);

                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                PhoneNumberPickerFragment fragment = new PhoneNumberPickerFragment();
                fragment.setShortcutAction(Intent.ACTION_SENDTO);

                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                PostalAddressPickerFragment fragment = new PostalAddressPickerFragment();
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_DELETE: {
                MultiSelectPeopleFragment fragment = MultiSelectPeopleFragment.newInstance(
                        mActionCode, mRequest.getPosition());
                fragment.setFilter(mCurrentFilter);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_MULTIPLE_PHONE_NUMBER: {
                PhoneNumberMultiplePickerFragment fragment = new PhoneNumberMultiplePickerFragment();
                Parcelable[] uris = mRequest.getContactUriArray();
                fragment.setSelectedUris(uris);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_MULTIPLE_PHONE_NUMBER_IN_GOURP: {
                PhoneNumberMultiplePickerFragment fragment = new PhoneNumberMultiplePickerFragment();
                String IDs = mRequest.getQueryString();

                fragment.setGroupIDsFilter(IDs);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_ADD_TO_GROUP: {
                MultiSelectPeopleFragment fragment = MultiSelectPeopleFragment.newInstance(
                        mActionCode, 0);
                fragment.setFilter(mCurrentFilter);
                String[] IDs = mRequest.getGroupMemIDs();
                long groupID = mRequest.getGroupID();

                fragment.setupGroups(IDs, groupID);
                mListFragment = fragment;
                break;
            }
            /*YunOS BEGIN PB*/
            //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
            //##BugID:(8466294) ##date:2016-7-22 09:00
            //##description:suppot export some contacts to vcard
            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_RM_FROM_GROUP: {
                MultiSelectPeopleFragment fragment = MultiSelectPeopleFragment.newInstance(
                        mActionCode, 0);
                String[] IDs = mRequest.getGroupMemIDs();
                long groupID = mRequest.getGroupID();
                fragment.setFilter(mCurrentFilter);
                fragment.setupGroups(IDs, groupID);
                mListFragment = fragment;
                break;
            }
            /*YUNOS END PB*/
            case ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_EXPORT: {
                MultiSelectPeopleFragment fragment = MultiSelectPeopleFragment.newInstance(
                        mActionCode, mRequest.getPosition());
                fragment.setFilter(mCurrentFilter);
                mListFragment = fragment;
                break;
            }
            
            default:
                // throw new IllegalStateException("Invalid action code: " + mActionCode);
                // For Bug 5227006, we can't throw exception here, just return. Workaround
                mActionCode = ContactsRequest.ACTION_DEFAULT;
                return;
        }

        mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

        getSupportFragmentManager().beginTransaction().replace(R.id.list_container, mListFragment)
                .commitAllowingStateLoss();
    }

    public void setupActionListener() {
        if (mListFragment instanceof ContactPickerFragment) {
            ((ContactPickerFragment) mListFragment).setOnContactPickerActionListener(
                    new ContactPickerActionListener());
        } else if (mListFragment instanceof PhoneNumberPickerFragment) {
            ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberPickerActionListener(
                    new PhoneNumberPickerActionListener());
        } else if (mListFragment instanceof PostalAddressPickerFragment) {
            ((PostalAddressPickerFragment) mListFragment).setOnPostalAddressPickerActionListener(
                    new PostalAddressPickerActionListener());
        } else if (mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) mListFragment).setOnEmailAddressPickerActionListener(
                    new EmailAddressPickerActionListener());
        } else if (mListFragment instanceof MultiSelectPeopleFragment
                    || mListFragment instanceof PhoneNumberMultiplePickerFragment) {
                return;
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
    }

    private final class ContactPickerActionListener implements OnContactPickerActionListener {
        @Override
        public void onCreateNewContactAction() {
            startCreateNewContactActivity();
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri, long nameRawContactId) {
            Bundle extras = getIntent().getExtras();
            if (SimContactUtils.canAddToSimContact(extras)) {

                SimContactCache.SimContact cachedSimContact
                        = SimContactCache.getSimContactByRawContactIdWithoutSimId(nameRawContactId);
                if (cachedSimContact != null) {
                    startEditSimContact(cachedSimContact, extras);
                } else {
                    startEditPhoneContact(contactLookupUri, extras);
                }
            } else {
                startEditPhoneContact(contactLookupUri, extras);
            }
        }

        @Override
        public void onPickContactAction(Uri contactUri) {
            returnPickerResult(contactUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        private void startEditPhoneContact(Uri contactLookupUri, Bundle extras) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.setClass(ContactSelectionActivity.this, ContactEditorActivity.class);
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivityForResult(intent, SUBACTIVITY_ADD_TO_EXISTING_CONTACT);
        }

        private void startEditSimContact(SimContactCache.SimContact simContact, Bundle extras) {
            int slotId = simContact.slotId;
            boolean is2g;
            if (SimUtil.MULTISIM_ENABLE) {
                is2g = SimUtil.is2gSim(slotId);
            } else {
                is2g = SimUtil.is2gSim();
            }
            String extraPhone = extras.getString(Insert.PHONE);
            String extraEmail = extras.getString(Insert.EMAIL);
            boolean noSpace = false;
            if (is2g) {
                if ((!TextUtils.isEmpty(extraPhone))
                        && (!TextUtils.isEmpty(simContact.number))) {
                    noSpace = true;
                } else if (!TextUtils.isEmpty(extraEmail)) {
                    noSpace = true;
                }
            } else {
                if ((!TextUtils.isEmpty(extraPhone))
                        && (!TextUtils.isEmpty(simContact.number))
                        && (!TextUtils.isEmpty(simContact.anrs))) {
                    noSpace = true;
                } else if ((!TextUtils.isEmpty(extraEmail))
                        && (!TextUtils.isEmpty(simContact.emails))) {
                    noSpace = true;
                }
            }
            if (noSpace) {
                Toast.makeText(
                        ContactSelectionActivity.this,
                        getString(R.string.sim_contact_item_full, simContact.name),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = SimContactUtils.makeEditIntentFromCachedSimContact(simContact);
            intent.putExtra(SimContactEditorFragment.INTENT_EXTRA_PHONE_NUMBER, extraPhone);
            intent.putExtra(SimContactEditorFragment.INTENT_EXTRA_EMAIL, extraEmail);
            startActivityForResult(intent, SUBACTIVITY_ADD_TO_EXISTING_CONTACT);
        }

    }

    private final class PhoneNumberPickerActionListener implements
            OnPhoneNumberPickerActionListener {
        @Override
        public void onPickPhoneNumberAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        @Override
        public void onHomeInActionBarSelected() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class PostalAddressPickerActionListener implements
            OnPostalAddressPickerActionListener {
        @Override
        public void onPickPostalAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    private final class EmailAddressPickerActionListener implements
            OnEmailAddressPickerActionListener {
        @Override
        public void onPickEmailAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuAdapter menuAdapter = mListFragment.getContextMenuAdapter();
        if (menuAdapter != null) {
            return menuAdapter.onContextItemSelected(item);
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void startOutAnimation(int time) {
        if (getActionBar() != null) {
            getActionBar().show();
        }
        restoreSearchBar();
    }

    @Override
    public void startInAnimation(int time) {
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        setSearchBar();
    }

    @Override
    public void doTextChanged(CharSequence s) {
        mListFragment.setQueryString(s.toString(), true);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        switch (view.getId()) {
            case R.id.search_view: {
                if (hasFocus) {
                    showInputMethod(mSearchView.findFocus());
                }
            }
        }
    }

    public void returnPickerResult(Uri data) {
        Intent intent = new Intent();
        intent.setData(data);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void startCreateNewContactActivity() {
        Bundle extras = getIntent().getExtras();
        if (SimContactUtils.canAddToSimContact(extras)) {
            AccountFilterManager.getInstance(this)
                    .createNewContactWithPhoneNumberOrEmailAsync(
                            this,
                            extras.getString(Insert.PHONE),
                            extras.getString(Insert.EMAIL),
                            SUBACTIVITY_ADD_TO_EXISTING_CONTACT);
            return;
        }
        AccountFilterManager.getInstance(this)
                .createNewPhoneContactWithExtrasAsync(this, extras, SUBACTIVITY_ADD_TO_EXISTING_CONTACT);
    }

    private void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(view, 0)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SUBACTIVITY_ADD_TO_EXISTING_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                finish();
            }
        }

    }
}
