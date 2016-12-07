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

package com.yunos.alicontacts.activities;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.yunos.alicontacts.ContactSaveService;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.editor.ContactEditorFragment;
import com.yunos.alicontacts.editor.ContactEditorFragment.SaveMode;
import com.yunos.alicontacts.list.AccountFilterManager;
import com.yunos.alicontacts.model.AccountTypeManager;
import com.yunos.alicontacts.model.RawContactDeltaList;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.alicontacts.platform.PDConstants;
import com.yunos.alicontacts.util.DialogManager;
import com.yunos.common.UsageReporter;

import yunos.support.v4.app.FragmentActivity;
import yunos.support.v4.app.FragmentManager;
import yunos.support.v4.app.FragmentTransaction;

import java.util.ArrayList;

public class ContactEditorActivity extends FragmentActivity
        implements DialogManager.DialogShowingViewActivity, ContactSaveService.Listener{
    private static final String TAG = "ContactEditorActivity";
    private static final String PERFORMANCE_TAG = "Performance";
    private static final boolean DEBUG = true;

    public static final String ACTION_JOIN_COMPLETED = "joinCompleted";
    public static final String ACTION_SAVE_COMPLETED = "saveCompleted";

    /**
     * Boolean intent key that specifies that this activity should finish itself
     * (instead of launching a new view intent) after the editor changes have been
     * saved.
     */
    public static final String INTENT_KEY_NOT_VIEW_DETAIL_ON_SAVE_COMPLETED =
            "notViewDetailOnSaveCompleted";

    private ContactEditorFragment mFragment;
    //private boolean mFinishActivityOnSaveCompleted;

    private DialogManager mDialogManager = new DialogManager(this);

    private long[] mDeleteItems;

    private boolean mInSelectingAccount = false;

    @Override
    public void onCreate(Bundle savedState) {
        if(DEBUG) Log.d(PERFORMANCE_TAG, "ContactEditorActivity.onCreate() in.");
        super.onCreate(savedState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (ACTION_SAVE_COMPLETED.equals(action) || action == null) {
            finish();
            return;
        }
        if(DEBUG) Log.d(PERFORMANCE_TAG, "ContactEditorActivity.onCreate() before setContentView().");
        setContentView(R.layout.contact_editor_activity);
        if(DEBUG) Log.d(PERFORMANCE_TAG, "ContactEditorActivity.onCreate() after setContentView().");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        //systemBarManager.setViewFitsSystemWindows(this, getActionBar() != null);
        systemBarManager.setStatusBarColor(getResources().getColor(R.color.title_color));
        systemBarManager.setStatusBarDarkMode(this, getResources().getBoolean(R.bool.contact_dark_mode));

        Bundle extras = intent.getExtras();
        if (Intent.ACTION_INSERT.equals(action)
                && ((extras == null) || (!extras.containsKey(PDConstants.INTENTS_INSERT_EXTRA_ACCOUNT)))) {
            mInSelectingAccount = true;
        } else {
            doCreate(intent);
        }
        setupActionBar(action);
        if(DEBUG) Log.d(PERFORMANCE_TAG, "ContactEditorActivity.onCreate() out.");
    }

    public void doCreate(Intent intent) {
        String action = intent.getAction();

        // Get delete list from intent, which Duplicate Remove plugin want to
        // delete if user save new merged contact
        mDeleteItems = intent.getLongArrayExtra("delete_list");

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        mFragment = new ContactEditorFragment();
        mFragment.setListener(mFragmentListener);
        Uri uri = Intent.ACTION_EDIT.equals(action) ? intent.getData() : null;
        mFragment.load(action, uri, intent.getExtras());
        mFragment.setNotViewDetailOnSaveCompleted(
                intent.getBooleanExtra(INTENT_KEY_NOT_VIEW_DETAIL_ON_SAVE_COMPLETED, false));

        transaction.add(R.id.editor_container, mFragment, "contact_editor_fragment");
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();

        //setupActionBar();
        mInSelectingAccount = false;
        ContactSaveService.registerListener(this);
    }

    private void setupActionBar(String action) {
        ActionBar actionBar = this.getActionBar();
        if (actionBar != null) {

            actionBar.setCustomView(R.layout.contacts_actionbar_cancel_done);
            actionBar.setDisplayShowCustomEnabled(true);
            if (Intent.ACTION_INSERT.equals(action)) {
                ((TextView)actionBar.getCustomView().findViewById(R.id.title))
                        .setText(R.string.insertContactDescription);
            }

            actionBar.getCustomView().findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mFragment != null) {
                        mFragment.handleCancelSaveClick();
                    } else {
                        finish();
                    }
                }
            });
            actionBar.getCustomView().findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mFragment != null) {
                        mFragment.handleSaveClick();
                    } else {
                        finish();
                    }
                }
            });
        }
    }

    @Override
    public void onServiceCompleted(Intent callbackIntent) {
        if(DEBUG) Log.d(PERFORMANCE_TAG, "invoke onServiceCompleted on editor activity.");
        onNewIntent(callbackIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContactSaveService.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        if(DEBUG) Log.d(PERFORMANCE_TAG, "onResume() in.");
        super.onResume();
        if (!mInSelectingAccount) {
            doResume();
        }
        if(DEBUG) Log.d(PERFORMANCE_TAG, "onResume() out.");
    }

    public void doResume() {
        UsageReporter.onResume(this, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UsageReporter.onPause(this, null);
    }

    @Override
    protected void onStart() {
        if(DEBUG) Log.d(PERFORMANCE_TAG, "onStart() in.");
        super.onStart();
        if (mInSelectingAccount) {
            Intent intent = getIntent();
            Bundle extras = intent == null ? new Bundle() : intent.getExtras();
            AccountFilterManager.getInstance(this).createNewContactInContactEditor(this, extras);
        }
        if(DEBUG) Log.d(PERFORMANCE_TAG, "onStart() out.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NOTE: Because the AccountFilterManager is single instance.
        // When this activity is in background, the account select dialog might be used
        // by other activity and dismissed. So we need to dismiss the dialog and
        // re-create the dialog on next display of this activity.
        if (mInSelectingAccount) {
            AccountFilterManager.getInstance(this).dismissAccountSelectDialog();
            // In case someone add logic after this if-statement someday, force return here.
            return;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mFragment == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            mFragment.setIntentExtras(intent.getExtras());
        } else if (ACTION_SAVE_COMPLETED.equals(action)) {
            RawContactDeltaList data = (RawContactDeltaList) intent.getParcelableExtra(ContactDetailActivity.EXTRA_DETAIL_DATA);
            if (data != null) {
                mFragment.setDeliverState(data);
            }
            mFragment.onSaveCompleted(true,
                    intent.getIntExtra(ContactEditorFragment.SAVE_MODE_EXTRA_KEY, SaveMode.CLOSE),
                    intent.getBooleanExtra(ContactSaveService.EXTRA_SAVE_SUCCEEDED, false),
                    intent.getData());

            // delete contacts after duplicate remove plugin merge related contacts
            // Log.v(TAG, "mDeleteItems : " + mDeleteItems);
            if (mDeleteItems != null && mDeleteItems.length > 0) {
                deleteContactItems();
            }
        } else if (ACTION_JOIN_COMPLETED.equals(action)) {
            mFragment.onJoinCompleted(intent.getData());
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id))
            return mDialogManager.onCreateDialog(id, args);

        // Nobody knows about the Dialog
        Log.w(TAG, "Unknown dialog requested, id: " + id + ", args: " + args);
        return null;
    }

    @Override
    public void onBackPressed() {
        if (mInSelectingAccount) {
            AccountFilterManager.getInstance(this).dismissAccountSelectDialog();
            finish();
            return;
        }
        if (mFragment != null) {
            mFragment.handleCancelSave();
        }
    }

    private final ContactEditorFragment.Listener mFragmentListener =
            new ContactEditorFragment.Listener() {
        @Override
        public void onReverted() {
            finish();
        }

        @Override
        public void onSaveFinished(final Intent resultIntent) {
            if (resultIntent != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(DEBUG) Log.d(PERFORMANCE_TAG, "invoke onSaveFinished and start detail activity on editor activity.");
                        startActivity(resultIntent);
                        finish();
                    }
                });
            } else {
                finish();
            }
        }

        @Override
        public void onContactSplit(Uri newLookupUri) {
            finish();
        }

        @Override
        public void onContactNotFound() {
            finish();
        }

        @Override
        public void onCustomCreateContactActivityRequested(AccountWithDataSet account,
                Bundle intentExtras) {
            final AccountTypeManager accountTypes =
                    AccountTypeManager.getInstance(ContactEditorActivity.this);
            final AccountType accountType = accountTypes.getAccountType(
                    account.type, account.dataSet);

            Intent intent = new Intent();
            intent.setClassName(accountType.syncAdapterPackageName,
                    accountType.getCreateContactActivityClassName());
            intent.setAction(Intent.ACTION_INSERT);
            intent.setType(Contacts.CONTENT_ITEM_TYPE);
            if (intentExtras != null) {
                intent.putExtras(intentExtras);
            }
            intent.putExtra(RawContacts.ACCOUNT_NAME, account.name);
            intent.putExtra(RawContacts.ACCOUNT_TYPE, account.type);
            intent.putExtra(RawContacts.DATA_SET, account.dataSet);
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);
            finish();
        }

        @Override
        public void onCustomEditContactActivityRequested(AccountWithDataSet account,
                Uri rawContactUri, Bundle intentExtras, boolean redirect) {
            final AccountTypeManager accountTypes =
                    AccountTypeManager.getInstance(ContactEditorActivity.this);
            final AccountType accountType = accountTypes.getAccountType(
                    account.type, account.dataSet);

            Intent intent = new Intent();
            intent.setClassName(accountType.syncAdapterPackageName,
                    accountType.getEditContactActivityClassName());
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(rawContactUri);
            if (intentExtras != null) {
                intent.putExtras(intentExtras);
            }

            if (redirect) {
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                startActivity(intent);
                finish();
            } else {
                startActivity(intent);
            }
        }

        @Override
        public void onEditOtherContactRequested(Uri contactLookupUri,
                ArrayList<ContentValues> contentValues) {
//          Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
//          intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
//                  | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
//          intent.putExtra(ContactEditorFragment.INTENT_EXTRA_ADD_TO_DEFAULT_DIRECTORY, "");
//
//          //Pass on all the data that has been entered so far
//          if (contentValues != null && !contentValues.isEmpty()) {
//              intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, contentValues);
//          }

          //startActivity(intent);
          //finish();

            // if ContactActivity use "SingleTop" launch mode, start a ContactActivity and finish
            // itself will operate on the same activity. So we instead reload new contact in current
            // fragment.
            Bundle extras = new Bundle();
            // Pass on all the data that has been entered so far
            if (contentValues != null && !contentValues.isEmpty()) {
                extras.putParcelableArrayList(ContactsContract.Intents.Insert.DATA,
                        contentValues);
            }
            // load a new contact in current ContactEditorFragment,
            // with extras which may contain more contacts data for adding to new contact.
            mFragment.load(Intent.ACTION_EDIT, contactLookupUri, extras);
            mFragment.restartContactLoader();
        }

    };

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    private void deleteContactItems() {
        if (mDeleteItems == null || mDeleteItems.length <= 0) {
            return;
        }

        int len = mDeleteItems.length;
        for (int i = 0; i < len; i++) {
            long id = mDeleteItems[i];
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
            startService(ContactSaveService.createDeleteContactIntent(this, uri));
        }

        mDeleteItems = null;
    }
}
