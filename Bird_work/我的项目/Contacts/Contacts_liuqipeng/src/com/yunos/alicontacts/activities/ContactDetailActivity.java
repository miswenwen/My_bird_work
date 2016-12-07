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


import android.app.Activity;
import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Data;
import android.provider.ContactsContract.StreamItemPhotos;
import android.provider.ContactsContract.StreamItems;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.yunos.alicontacts.ContactSaveService;
import com.yunos.alicontacts.GroupMetaData;
import com.yunos.alicontacts.GroupMetaDataLoader;
import com.yunos.alicontacts.NfcHandler;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.detail.ContactDetailFragment;
import com.yunos.alicontacts.detail.ContactLoaderFragment;
import com.yunos.alicontacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.yunos.alicontacts.interactions.ContactDeletionInteraction;
import com.yunos.alicontacts.interfaces.AliContactsPluginInterface;
import com.yunos.alicontacts.list.ShortcutIntentBuilder;
import com.yunos.alicontacts.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.yunos.alicontacts.model.AccountTypeManager;
import com.yunos.alicontacts.model.Contact;
import com.yunos.alicontacts.model.ContactLoader;
import com.yunos.alicontacts.model.ContactLoader.ContactQuery;
import com.yunos.alicontacts.model.ContactLoader.DirectoryQuery;
import com.yunos.alicontacts.model.ContactLoader.GroupQuery;
import com.yunos.alicontacts.model.RawContact;
import com.yunos.alicontacts.model.RawContactDelta;
import com.yunos.alicontacts.model.RawContactDeltaList;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.model.account.AccountTypeWithDataSet;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.alicontacts.model.dataitem.DataItem;
import com.yunos.alicontacts.model.dataitem.PhoneDataItem;
import com.yunos.alicontacts.model.dataitem.PhotoDataItem;
import com.yunos.alicontacts.plugins.PluginBean;
import com.yunos.alicontacts.plugins.PluginManager;
import com.yunos.alicontacts.preference.ContactsSettingActivity;
import com.yunos.alicontacts.sim.SimContactCache;
import com.yunos.alicontacts.sim.SimContactEditorFragment;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.util.Constants;
import com.yunos.alicontacts.util.ContactLoaderUtils;
import com.yunos.alicontacts.util.DataStatus;
import com.yunos.alicontacts.util.StreamItemEntry;
import com.yunos.alicontacts.util.StreamItemPhotoEntry;
import com.yunos.alicontacts.util.YunOSFeatureHelper;
import com.yunos.alicontacts.weibo.WeiboFinals;
import com.yunos.alicontacts.widget.aui.PopMenu;
import com.yunos.alicontacts.widget.aui.PopMenu.OnPopMenuListener;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import yunos.support.v4.app.Fragment;
import yunos.support.v4.util.LongSparseArray;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ContactDetailActivity extends BaseFragmentActivity implements OnClickListener {
    private static final String TAG = "ContactDetailActivity";
    private static final String PERFORMANCE_TAG = "Performance";
    private static final boolean DEBUG = true;

    public static final String CALL_ORIGIN_CALL_DETAIL_ACTIVITY = "com.yunos.alicontacts.activities.ContactDetailActivity";
    /** Shows a toogle button for hiding/showing updates. Don't submit with true */
    // private static final boolean DEBUG_TRANSITIONS = false;
    private static final int ID_FOOTER_ICON_SHARE = 1;
    private static final int ID_FOOTER_ICON_DELETE = 2;
    // private static final int ID_FOOTER_ICON_JOIN = 3;
    private static final int ID_SHARE_TO_MMS = 5;
    // private static final int ID_FOOTER_ICON_CANCEL =5;
    private static final int ID_FOOTER_ICON_PRIVATE = 6;
    private static final int ID_FOOTER_ADD_TO_DESK = 7;

    public static final String ACTION_JOIN_COMPLETED = "joinCompleted";

    public static final String EXTRA_KEY_IS_SIM_CONTACT = "isSimContact";
    private static final String KEY_MIMETYPE = "mimetype";
    private static final String CONTACT_URI_BEGIN = "content://com.android.contacts/contacts/lookup/";
    public static final String EXTRA_DETAIL_DATA = "detailData";
    public static final String EXTRA_DETAIL_IS_PROFILE = "detailIsProfile";
    private static final String DATA_COLUMN_NAME = "data1";
    public static final String INTENT_KEY_FORWARD_RESULT = "forward_result";

    private static final int LOAD_CONTACT_MSG = 10;

    private Contact mContactData;
    private Uri mLookupUri;
    private boolean mIsProfile;
    private static final String PROFILE_LOOKUP_KEY = "profile";

    AlertDialog mStarDialog;
    private ContactLoaderFragment mLoaderFragment;
    private ContactDetailFragment mDetailFragment;
    private ImageView mFavoriteView;
    private ImageView mQRCodeView;
    private View mActionBarView;
    private PopMenu mMenuDialog;
    private PluginBean mBarCodePlugin;

    private boolean mIsStarred = false;

    private int mToastTextWhenNoContact = -1;
    LoadContactThread mLoadContactThread;
    ContentResolver mContentResolver;
    private ContactContentObserver mContactContentObserver;

    private CallLogManager mCallLogManager;
    private AliCallLogChangeListener mCallLogListener;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LOAD_CONTACT_MSG) {
                Contact result = (Contact) msg.obj;
                loadFinished(result);
            }
        }
    };

    private class ContactContentObserver extends ContentObserver {
        ContactContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mLoadContactThread != null && mLoadContactThread.isAlive()) {
                mLoadContactThread.interrupt();
                mLoadContactThread = null;
            }
            mLoadContactThread = new LoadContactThread("LoadContactDetail-Thread");
            mLoadContactThread.start();
        }
    }

    private class AliCallLogChangeListener implements CallLogManager.CallLogChangeListener {
        @Override
        public void onCallLogChange(int changedPart) {
            Log.i(TAG, "AliCallLogChangeListener.onCallLogChange:");
            if ((changedPart & CHANGE_PART_CALL_LOG) == 0) {
                Log.i(TAG, "AliCallLogChangeListener.onCallLogChange: no call log change, ignore.");
                return;
            }
            if (mDetailFragment != null) {
                mDetailFragment.startLoadingCallLogs();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (DEBUG)
            Log.d(PERFORMANCE_TAG, "onCreate on detail activity start");
        Intent intent = getIntent();
        mLookupUri = intent == null ? null : intent.getData();
        if (mLookupUri == null) {
            Log.e(TAG, "mLookupUri is null, finish activity!!!");
            finish();
            return;
        }
        mContentResolver = getContentResolver();
        showBackKey(true);
        setActivityContentView(R.layout.contact_detail_activity);

        mDetailFragment = (ContactDetailFragment) getSupportFragmentManager().findFragmentById(R.id.detail_fragment);
        mDetailFragment.setListener(mContactDetailFragmentListener);
        mActionBarView = findViewById(R.id.actionbar_header);
        loadDataFromExtra(intent);
        if (intent.getBooleanExtra(ShortcutIntentBuilder.NEED_TOAST_WHEN＿CONTACT_NOT_FOUND, false)) {
            mToastTextWhenNoContact = R.string.invalidContactMessage;
        }
        mLoadContactThread = new LoadContactThread("LoadContactDetail-Thread");
        mLoadContactThread.start();
        mContactContentObserver = new ContactContentObserver(mHandler);
        try {
            mContentResolver.registerContentObserver(mLookupUri, false, mContactContentObserver);
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, "Got IllegalArgumentException in registerContentObserver.", iae);
            Toast.makeText(getApplicationContext(), R.string.contact_detail_invalid_input_data, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mCallLogListener = new AliCallLogChangeListener();
        mCallLogManager = CallLogManager.getInstance(ActivityThread.currentApplication());
        mCallLogManager.registCallsTableChangeListener(mCallLogListener);
        mCallLogManager.requestSyncCalllogsByInit();
        String lookupUriString = mLookupUri.toString();
        if (!TextUtils.isEmpty(lookupUriString)) {
            mIsProfile = lookupUriString.contains(PROFILE_LOOKUP_KEY);
        }

        findViewById(R.id.back_ico).setOnClickListener(this);
        findViewById(R.id.menu_id).setOnClickListener(this);
        findViewById(R.id.edit_id).setOnClickListener(this);
        mQRCodeView = (ImageView) findViewById(R.id.qrcode_id);
        mQRCodeView.setOnClickListener(this);
        mFavoriteView = (ImageView) findViewById(R.id.favorite_id);
        mFavoriteView.setOnClickListener(this);
        if ((!mIsProfile) && (!intent.getBooleanExtra(EXTRA_KEY_IS_SIM_CONTACT, false))) {
            mFavoriteView.setVisibility(View.VISIBLE);
        }
        initQRCode();
        NfcHandler.register(this, mDetailFragment);
        if (intent.hasExtra(INTENT_KEY_FORWARD_RESULT)) {
            setResult(intent.getIntExtra(INTENT_KEY_FORWARD_RESULT, Activity.RESULT_CANCELED));
        }
        if (DEBUG) Log.d(PERFORMANCE_TAG, "onCreate on detail activity end");
    }

    @Override
    protected void setSystemBar(int color, boolean hasActionbar, boolean isDarkMode) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (mSystemBarColorManager == null) {
            mSystemBarColorManager = new SystemBarColorManager(this);
        }
        mSystemBarColorManager.setViewFitsSystemWindows(this, hasActionbar);
        mSystemBarColorManager.setStatusBarColor(getResources().getColor(color));
        mSystemBarColorManager.setStatusBarDarkMode(this, false);
    }

    @Override
    public void onClick(View v) {
        int clickId = v.getId();
        if ((mContactData == null) && (clickId != R.id.back_ico)) {
            Log.w(TAG, "onClick: contact data is null. Probably onLoadFinished is not called yet.");
            return;
        }
        switch (clickId) {
            case R.id.back_ico:
                finish();
                break;
            case R.id.favorite_id:
                boolean isAutoFavoriteContacts = ContactsSettingActivity.readBooleanFromDefaultSharedPreference(this,
                        ContactsSettingActivity.CONTACT_AUTO_FAVORITE_PREFERENCE,
                        ContactsSettingActivity.DEFAULT_AUTO_FAVORITE_ON_OFF);
                // TODO update favorite
                if (isAutoFavoriteContacts) {
                    AlertDialog.Builder build = new AlertDialog.Builder(this);
                    String title = null;
                    String msg = null;
                    if (mIsStarred) {
                        // strings for cancel starring
                        title = getString(R.string.contacts_detail_cancel_star);
                        msg = getString(R.string.auto_favorite_tip_for_cancel_favorites);
                    } else {
                        title = getString(R.string.description_star);
                        msg = getString(R.string.auto_favorite_tip);
                    }
                    build.setTitle(title);
                    build.setMessage(msg);
                    build.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ContactsSettingActivity.writeBooleanToDefaultSharedPreference(ContactDetailActivity.this,
                                    ContactsSettingActivity.CONTACT_AUTO_FAVORITE_PREFERENCE, false);
                            operateFavorite();
                        }
                    });
                    build.setNegativeButton(R.string.no, null);
                    mStarDialog = build.create();
                    mStarDialog.show();
                } else {
                    operateFavorite();
                }
                break;
            case R.id.menu_id:
                showMenu();
                break;
            case R.id.edit_id:
                if (mContactData.isSimAccountType()) {
                    startEditSimContactActivity();
                } else {
                    mLoaderFragment.doEdit();
                }
                UsageReporter.onClick(this, null, UsageReporter.ContactsDetailPage.DETAL_EDIT_BUTTON_CLICKED);
                break;
            case R.id.qrcode_id:
                if (mBarCodePlugin != null && mBarCodePlugin.isFullScreen()) {
                    Intent intent = new Intent(mBarCodePlugin.getDetailAction());
                    final String lookupKey = mContactData.getLookupKey();
                    Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKey);

                    intent.putExtra(AliContactsPluginInterface.CONTACT_ID, mContactData.getId());
                    intent.putExtra(AliContactsPluginInterface.CONTACT_NAME, mContactData.getDisplayName());

                    if (mContactData.isUserProfile()) {
                        intent.putExtra(AliContactsPluginInterface.CONTACT_VACRD, (Uri) null);
                    } else {
                        intent.putExtra(AliContactsPluginInterface.CONTACT_VACRD, shareUri);
                    }

                    startActivity(intent);
                    UsageReporter.onClick(this, null, UsageReporter.ContactsDetailPage.DETAL_BARCODE_CLICKED);
                }
                break;
        }
    }

    private void initQRCode() {
        PluginManager pluginManager = PluginManager.getInstance(this);
        List<PluginBean> plugins = pluginManager.getLayoutPlugins();
        for (PluginBean plugin : plugins) {
            if (plugin.getPackageName().equals(PluginManager.PLUGIN_PACKAGE_NAME_QRCODE)) {
                mBarCodePlugin = plugin;
                break;
            }
        }
        if (mBarCodePlugin == null) {
            mQRCodeView.setVisibility(View.GONE);
        }
    }

    public void updateTitleColor(int color) {
        mActionBarView.setBackgroundResource(color);
        setSystemBar(color, getActionBar() != null, false);
    }

    private void loadDataFromExtra(Intent intent) {
        RawContactDeltaList data = (RawContactDeltaList) intent.getParcelableExtra(EXTRA_DETAIL_DATA);
        if (data != null) {
            Log.d(TAG, "loadDataFromExtra start.");
            String displayName = null;
            String firstNumber = null;
            final long directoryId = Directory.DEFAULT;
            String url = mLookupUri.toString();
            String[] datas = (url.substring(CONTACT_URI_BEGIN.length(), url.length())).split("/");
            final long contactId = Long.parseLong(datas[1]);
            final String lookupKey = datas[0];
            final long nameRawContactId = contactId;
            final int displayNameSource = DisplayNameSources.STRUCTURED_NAME;
            ImmutableList.Builder<RawContact> rawContactsBuilder = new ImmutableList.Builder<RawContact>();

            ContentValues rawContactsCV = new ContentValues();
            rawContactsCV.put(RawContacts._ID, contactId);
            RawContact rawContact = new RawContact(this, rawContactsCV);
            rawContactsBuilder.add(rawContact);

            int i = 1;
            for (RawContactDelta rcd : data) { // here just get the
                                               // ContentValues to use.
                ArrayList<ContentValues> cvList = rcd.getContentValues();
                for (ContentValues cv : cvList) {
                    if (cv.containsKey(KEY_MIMETYPE)) {
                        String mimeType = cv.getAsString(KEY_MIMETYPE);
                        if (ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                            displayName = cv.getAsString(DATA_COLUMN_NAME);
                        } else if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                            cv.put(Data._ID, i);// Here just use i as data ID,
                                                // because if use real ID of
                                                // column, we need query
                                                // database.
                            rawContact.addDataItemValues(cv);
                            i++;
                            if (firstNumber == null) {
                                firstNumber = cv.getAsString(DATA_COLUMN_NAME);
                            }
                        }
                        Log.d(TAG, "loadDataFromExtra to add cv " + cv);
                    }
                }
            }
            if (displayName == null) {
                displayName = firstNumber;
            }
            Uri lookupUri;
            if (directoryId == Directory.DEFAULT || directoryId == Directory.LOCAL_INVISIBLE) {
                lookupUri = ContentUris.withAppendedId(Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey), contactId);
            } else {
                lookupUri = mLookupUri;
            }
            Contact contact = new Contact(mLookupUri, mLookupUri, lookupUri, directoryId, lookupKey, contactId,
                    nameRawContactId, displayNameSource, -1, null, displayName, displayName, displayName, false, null, false,
                    null, intent.getBooleanExtra(EXTRA_DETAIL_IS_PROFILE, false));
            contact.setRawContacts(rawContactsBuilder.build());
            loadPhotoBinaryData(contact);
            contact.setStreamItems(new ImmutableList.Builder<StreamItemEntry>().build());
            computeFormattedPhoneNumbers(contact);
            loadInvitableAccountTypes(contact);
            mDetailFragment.setData(contact);
            Log.d(TAG, "loadDataFromExtra end.");
        }
    }

    // Here use thread instead of ContactLoader to save run times of get contact
    // detail data.
    // It is for performance optimization.
    private class LoadContactThread extends Thread {
        private String name;

        public LoadContactThread(String name) {
            super();
            this.name = name;
        }

        @Override
        public void run() {
            Log.d(PERFORMANCE_TAG, "LoadContactThread load start.");
            Contact result = loadInBackground(mLookupUri);
            Log.d(PERFORMANCE_TAG, "LoadContactThread load end.");
            Message msg = mHandler.obtainMessage();
            msg.obj = result;
            msg.what = LOAD_CONTACT_MSG;
            mHandler.sendMessage(msg);
        }
    }

    public Contact loadInBackground(Uri lookUpUri) {
        Log.d("PERFORMANCE", "ContactLoader loadInBackground() start");
        try {
            final ContentResolver resolver = mContentResolver;
            final Uri uriCurrentFormat = ContactLoaderUtils.ensureIsContactUri(resolver, lookUpUri);
            // Is this the same Uri as what we had before already? In that case,
            // reuse that result
            final Contact result;
            result = loadContactEntity(resolver, uriCurrentFormat);
            if (result.isLoaded()) {
                if (result.isDirectoryEntry()) {
                    loadDirectoryMetaData(result);
                } else {
                    if (result.getGroupMetaData() == null) {
                        loadGroupMetaData(result);
                    }
                }
                if (result.getStreamItems() == null) {
                    loadStreamItems(result);
                }
                computeFormattedPhoneNumbers(result);
                loadPhotoBinaryData(result);

                // Note ME profile should never have "Add connection"
                if (result.getInvitableAccountTypes() == null) {
                    loadInvitableAccountTypes(result);
                }
            }
            Log.d("PERFORMANCE", "ContactLoader loadInBackground() end");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error loading the contact: " + lookUpUri, e);
            return Contact.forError(lookUpUri, e);
        }
    }

    public void loadFinished(Contact data) {
        if (DEBUG)
            Log.d(PERFORMANCE_TAG, "onLoadFinished on detail fragment.");
        if (isFinishing() || data == null || !mLookupUri.equals(data.getRequestedUri())) {
            Log.e(TAG, "Different URI: requested=" + mLookupUri + "  actual=" + data);
            return;
        }

        if (data.isError()) {
            /**
             * This happens in a monkey case, which starts a background sync
             * together with contacts app. so the case might cause
             * ContactLoaderUtils.ensureIsContactUri() gets wrong type of normal
             * contact uri like
             * "content://com.android.contacts/contacts/lookup/xxx/yyy". It is
             * most probably because the background sync changes the contact,
             * when ContactLoaderUtils.ensureIsContactUri() trying to get type
             * of the contact uri. In this scenario, we just behavior as the
             * contact is not found.
             */
            Log.w(TAG, "onLoadFinished: Failed to load contact.", data.getException());
            mContactData = null;
        } else if (data.isNotFound()) {
            Log.i(TAG, "No contact found: " + mLookupUri);
            mContactData = null;
        } else {
            mContactData = data;
        }

        /// bird: TASK #7674,custom contacts readonly attr,chengting,@20160301 {
        if(mContactData != null) {
            Log.d(TAG,"cting,loadFinished,readonly is "+mContactData.isReadOnly());
            findViewById(R.id.edit_id).setVisibility(mContactData.isReadOnly() ? View.GONE : View.VISIBLE);
        }
        /// @}

        if (mLoaderFragmentListener != null) {
            if (mContactData == null) {
                mLoaderFragmentListener.onContactNotFound();
            } else {
                mDetailFragment.setData(mContactData);
                mLoaderFragment.setContactData(mContactData);
                updateFooterBar(mContactData, mContactData.getStarred());
                // mLoaderFragmentListener.onDetailsLoaded(mContactData);
            }
        }
        if (mToastTextWhenNoContact != -1) {
            mToastTextWhenNoContact = R.string.contact_deleted_message;
        }
    }

    private void loadInvitableAccountTypes(Contact contactData) {
        final ImmutableList.Builder<AccountType> resultListBuilder = new ImmutableList.Builder<AccountType>();
        if (!contactData.isUserProfile()) {
            Map<AccountTypeWithDataSet, AccountType> invitables = AccountTypeManager.getInstance(this)
                    .getUsableInvitableAccountTypes();
            if (!invitables.isEmpty()) {
                final Map<AccountTypeWithDataSet, AccountType> resultMap = Maps.newHashMap(invitables);

                // Remove the ones that already have a raw contact in the
                // current contact
                for (RawContact rawContact : contactData.getRawContacts()) {
                    final AccountTypeWithDataSet type = AccountTypeWithDataSet.get(rawContact.getAccountTypeString(),
                            rawContact.getDataSet());
                    resultMap.remove(type);
                }

                resultListBuilder.addAll(resultMap.values());
            }
        }

        // Set to mInvitableAccountTypes
        contactData.setInvitableAccountTypes(resultListBuilder.build());
    }

    private void loadPhotoBinaryData(Contact contactData) {

        // If we have a photo URI, try loading that first.
        String photoUri = contactData.getPhotoUri();
        if (photoUri != null) {
            try {
                AssetFileDescriptor fd = mContentResolver.openAssetFileDescriptor(Uri.parse(photoUri), "r");
                byte[] buffer = new byte[16 * 1024];
                FileInputStream fis = fd.createInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    int size;
                    while ((size = fis.read(buffer)) != -1) {
                        baos.write(buffer, 0, size);
                    }
                    contactData.setPhotoBinaryData(baos.toByteArray());
                } finally {
                    fis.close();
                    fd.close();
                }
                return;
            } catch (IOException ioe) {
                Log.e(TAG, "loadPhotoBinaryData() throw IOException", ioe);
            }
        }

        // If we couldn't load from a file, fall back to the data blob.
        final long photoId = contactData.getPhotoId();
        if (photoId <= 0) {
            // No photo ID
            return;
        }

        for (RawContact rawContact : contactData.getRawContacts()) {
            for (DataItem dataItem : rawContact.getDataItems()) {
                if (dataItem.getId() == photoId) {
                    if (!(dataItem instanceof PhotoDataItem)) {
                        break;
                    }

                    final PhotoDataItem photo = (PhotoDataItem) dataItem;
                    contactData.setPhotoBinaryData(photo.getPhoto());
                    break;
                }
            }
        }
    }

    private void computeFormattedPhoneNumbers(Contact contactData) {
        final ImmutableList<RawContact> rawContacts = contactData.getRawContacts();
        final int rawContactCount = rawContacts.size();
        for (int rawContactIndex = 0; rawContactIndex < rawContactCount; rawContactIndex++) {
            final RawContact rawContact = rawContacts.get(rawContactIndex);
            final List<DataItem> dataItems = rawContact.getDataItems();
            final int dataCount = dataItems.size();
            for (int dataIndex = 0; dataIndex < dataCount; dataIndex++) {
                final DataItem dataItem = dataItems.get(dataIndex);
                if (dataItem instanceof PhoneDataItem) {
                    final PhoneDataItem phoneDataItem = (PhoneDataItem) dataItem;
                    phoneDataItem.computeFormattedPhoneNumber(this);
                }
            }
        }
    }

    private Contact loadContactEntity(ContentResolver resolver, Uri contactUri) {
        Uri entityUri = Uri.withAppendedPath(contactUri, Contacts.Entity.CONTENT_DIRECTORY);
        Cursor cursor = null;
        cursor = resolver.query(entityUri, ContactQuery.COLUMNS, null, null, Contacts.Entity.RAW_CONTACT_ID);
        if (cursor == null) {
            Log.e(TAG, "No cursor returned in loadContactEntity");
            return Contact.forNotFound(mLookupUri);
        }

        try {
            if (!cursor.moveToFirst()) {
                cursor.close();
                return Contact.forNotFound(mLookupUri);
            }

            // Create the loaded contact starting with the header data.
            Contact contact = loadContactHeaderData(cursor, contactUri);

            // Fill in the raw contacts, which is wrapped in an Entity and any
            // status data. Initially, result has empty entities and statuses.
            long currentRawContactId = -1;
            RawContact rawContact = null;
            ImmutableList.Builder<RawContact> rawContactsBuilder = new ImmutableList.Builder<RawContact>();
            ImmutableMap.Builder<Long, DataStatus> statusesBuilder = new ImmutableMap.Builder<Long, DataStatus>();
            do {
                long rawContactId = cursor.getLong(ContactQuery.RAW_CONTACT_ID);
                if (rawContactId != currentRawContactId) {
                    // First time to see this raw contact id, so create a new
                    // entity, and
                    // add it to the result's entities.
                    currentRawContactId = rawContactId;
                    rawContact = new RawContact(this, loadRawContactValues(cursor));
                    rawContactsBuilder.add(rawContact);
                }
                if (!cursor.isNull(ContactQuery.DATA_ID)) {
                    ContentValues data = loadDataValues(cursor);
                    rawContact.addDataItemValues(data);

                    if (!cursor.isNull(ContactQuery.PRESENCE) || !cursor.isNull(ContactQuery.STATUS)) {
                        final DataStatus status = new DataStatus(cursor);
                        final long dataId = cursor.getLong(ContactQuery.DATA_ID);
                        statusesBuilder.put(dataId, status);
                    }
                }
            } while (cursor.moveToNext());

            contact.setRawContacts(rawContactsBuilder.build());
            contact.setStatuses(statusesBuilder.build());

            return contact;
        } finally {
            cursor.close();
        }
    }

    private void loadDirectoryMetaData(Contact result) {
        long directoryId = result.getDirectoryId();

        Cursor cursor = mContentResolver.query(ContentUris.withAppendedId(Directory.CONTENT_URI, directoryId),
                ContactLoader.DirectoryQuery.COLUMNS, null, null, null);
        if (cursor == null) {
            return;
        }
        try {
            if (cursor.moveToFirst()) {
                final String displayName = cursor.getString(DirectoryQuery.DISPLAY_NAME);
                final String packageName = cursor.getString(DirectoryQuery.PACKAGE_NAME);
                final int typeResourceId = cursor.getInt(DirectoryQuery.TYPE_RESOURCE_ID);
                final String accountType = cursor.getString(DirectoryQuery.ACCOUNT_TYPE);
                final String accountName = cursor.getString(DirectoryQuery.ACCOUNT_NAME);
                final int exportSupport = cursor.getInt(DirectoryQuery.EXPORT_SUPPORT);
                String directoryType = null;
                if (!TextUtils.isEmpty(packageName)) {
                    PackageManager pm = getPackageManager();
                    try {
                        Resources resources = pm.getResourcesForApplication(packageName);
                        directoryType = resources.getString(typeResourceId);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Contact directory resource not found: " + packageName + "." + typeResourceId);
                    }
                }

                result.setDirectoryMetaData(displayName, directoryType, accountType, accountName, exportSupport);
            }
        } finally {
            cursor.close();
        }
    }

    private void loadGroupMetaData(Contact result) {
        StringBuilder selection = new StringBuilder();
        ArrayList<String> selectionArgs = new ArrayList<String>();
        for (RawContact rawContact : result.getRawContacts()) {
            final String accountName = rawContact.getAccountName();
            final String accountType = rawContact.getAccountTypeString();
            final String dataSet = rawContact.getDataSet();
            if (accountName != null && accountType != null) {
                if (selection.length() != 0) {
                    selection.append(" OR ");
                }
                selection.append("(" + Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?");
                selectionArgs.add(accountName);
                selectionArgs.add(accountType);

                if (dataSet != null) {
                    selection.append(" AND " + Groups.DATA_SET + "=?");
                    selectionArgs.add(dataSet);
                } else {
                    selection.append(" AND " + Groups.DATA_SET + " IS NULL");
                }
                selection.append(')');
            }
        }

        final ImmutableList.Builder<GroupMetaData> groupListBuilder = new ImmutableList.Builder<GroupMetaData>();
        final Cursor cursor = mContentResolver.query(Groups.CONTENT_URI, ContactLoader.GroupQuery.COLUMNS,
                selection.toString(), selectionArgs.toArray(new String[selectionArgs.size()]),
                GroupMetaDataLoader.GROUPS_SORT_ORDER);
        try {
            while (cursor.moveToNext()) {
                final String accountName = cursor.getString(GroupQuery.ACCOUNT_NAME);
                final String accountType = cursor.getString(GroupQuery.ACCOUNT_TYPE);
                final String dataSet = cursor.getString(GroupQuery.DATA_SET);
                final long groupId = cursor.getLong(GroupQuery.ID);
                final String title = cursor.getString(GroupQuery.TITLE);

                groupListBuilder.add(new GroupMetaData(accountName, accountType, dataSet, groupId, title));
            }
        } finally {
            if (null != cursor)
                cursor.close();
        }
        result.setGroupMetaData(groupListBuilder.build());
    }

    private void loadStreamItems(Contact result) {
        final Cursor cursor = mContentResolver.query(Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(result.getLookupKey())
                .appendPath(Contacts.StreamItems.CONTENT_DIRECTORY).build(), null, null, null, null);
        final LongSparseArray<StreamItemEntry> streamItemsById = new LongSparseArray<StreamItemEntry>();
        final ArrayList<StreamItemEntry> streamItems = new ArrayList<StreamItemEntry>();
        try {
            while (cursor.moveToNext()) {
                StreamItemEntry streamItem = new StreamItemEntry(cursor);
                streamItemsById.put(streamItem.getId(), streamItem);
                streamItems.add(streamItem);
            }
        } finally {
            cursor.close();
        }

        // Pre-decode all HTMLs
        final long start = System.currentTimeMillis();
        for (StreamItemEntry streamItem : streamItems) {
            streamItem.decodeHtml(this);
        }
        final long end = System.currentTimeMillis();
        if (DEBUG) {
            Log.d(TAG, "Decoded HTML for " + streamItems.size() + " items, took " + (end - start) + " ms");
        }

        // Now retrieve any photo records associated with the stream items.
        if (!streamItems.isEmpty()) {
            if (result.isUserProfile()) {
                // If the stream items we're loading are for the profile, we
                // can't bulk-load the
                // stream items with a custom selection.
                for (StreamItemEntry entry : streamItems) {
                    Cursor siCursor = mContentResolver.query(Uri.withAppendedPath(
                            ContentUris.withAppendedId(StreamItems.CONTENT_URI, entry.getId()),
                            StreamItems.StreamItemPhotos.CONTENT_DIRECTORY), null, null, null, null);
                    try {
                        while (siCursor.moveToNext()) {
                            entry.addPhoto(new StreamItemPhotoEntry(siCursor));
                        }
                    } finally {
                        siCursor.close();
                    }
                }
            } else {
                String[] streamItemIdArr = new String[streamItems.size()];
                StringBuilder streamItemPhotoSelection = new StringBuilder();
                streamItemPhotoSelection.append(StreamItemPhotos.STREAM_ITEM_ID).append(" IN (");
                for (int i = 0; i < streamItems.size(); i++) {
                    if (i > 0) {
                        streamItemPhotoSelection.append(',');
                    }
                    streamItemPhotoSelection.append('?');
                    streamItemIdArr[i] = String.valueOf(streamItems.get(i).getId());
                }
                streamItemPhotoSelection.append(')');
                Cursor sipCursor = mContentResolver.query(StreamItems.CONTENT_PHOTO_URI, null,
                        streamItemPhotoSelection.toString(), streamItemIdArr, StreamItemPhotos.STREAM_ITEM_ID);
                try {
                    while (sipCursor.moveToNext()) {
                        long streamItemId = sipCursor.getLong(sipCursor.getColumnIndex(StreamItemPhotos.STREAM_ITEM_ID));
                        StreamItemEntry streamItem = streamItemsById.get(streamItemId);
                        streamItem.addPhoto(new StreamItemPhotoEntry(sipCursor));
                    }
                } finally {
                    sipCursor.close();
                }
            }
        }

        // Set the sorted stream items on the result.
        Collections.sort(streamItems);
        result.setStreamItems(new ImmutableList.Builder<StreamItemEntry>().addAll(streamItems.iterator()).build());
    }

    private ContentValues loadDataValues(Cursor cursor) {
        ContentValues cv = new ContentValues();

        cv.put(Data._ID, cursor.getLong(ContactQuery.DATA_ID));

        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA1);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA2);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA3);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA4);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA5);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA6);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA7);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA8);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA9);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA10);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA11);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA12);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA13);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA14);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA15);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA_SYNC1);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA_SYNC2);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA_SYNC3);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA_SYNC4);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA_VERSION);
        cursorColumnToContentValues(cursor, cv, ContactQuery.IS_PRIMARY);
        cursorColumnToContentValues(cursor, cv, ContactQuery.IS_SUPERPRIMARY);
        cursorColumnToContentValues(cursor, cv, ContactQuery.MIMETYPE);
        cursorColumnToContentValues(cursor, cv, ContactQuery.RES_PACKAGE);
        cursorColumnToContentValues(cursor, cv, ContactQuery.GROUP_SOURCE_ID);
        cursorColumnToContentValues(cursor, cv, ContactQuery.CHAT_CAPABILITY);

        return cv;
    }

    private ContentValues loadRawContactValues(Cursor cursor) {
        ContentValues cv = new ContentValues();

        cv.put(RawContacts._ID, cursor.getLong(ContactQuery.RAW_CONTACT_ID));

        cursorColumnToContentValues(cursor, cv, ContactQuery.ACCOUNT_NAME);
        cursorColumnToContentValues(cursor, cv, ContactQuery.ACCOUNT_TYPE);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DATA_SET);
        cursorColumnToContentValues(cursor, cv, ContactQuery.ACCOUNT_TYPE_AND_DATA_SET);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DIRTY);
        cursorColumnToContentValues(cursor, cv, ContactQuery.VERSION);
        cursorColumnToContentValues(cursor, cv, ContactQuery.SOURCE_ID);
        cursorColumnToContentValues(cursor, cv, ContactQuery.SYNC1);
        cursorColumnToContentValues(cursor, cv, ContactQuery.SYNC2);
        cursorColumnToContentValues(cursor, cv, ContactQuery.SYNC3);
        cursorColumnToContentValues(cursor, cv, ContactQuery.SYNC4);
        cursorColumnToContentValues(cursor, cv, ContactQuery.DELETED);
        cursorColumnToContentValues(cursor, cv, ContactQuery.CONTACT_ID);
        cursorColumnToContentValues(cursor, cv, ContactQuery.STARRED);
        // name_verified is deprecated
        //cursorColumnToContentValues(cursor, cv, ContactQuery.NAME_VERIFIED);

        return cv;
    }

    private void cursorColumnToContentValues(Cursor cursor, ContentValues values, int index) {
        switch (cursor.getType(index)) {
            case Cursor.FIELD_TYPE_NULL:
                // don't put anything in the content values
                break;
            case Cursor.FIELD_TYPE_INTEGER:
                values.put(ContactQuery.COLUMNS[index], cursor.getLong(index));
                break;
            case Cursor.FIELD_TYPE_STRING:
                values.put(ContactQuery.COLUMNS[index], cursor.getString(index));
                break;
            case Cursor.FIELD_TYPE_BLOB:
                values.put(ContactQuery.COLUMNS[index], cursor.getBlob(index));
                break;
            default:
                throw new IllegalStateException("Invalid or unhandled data type");
        }
    }

    private Contact loadContactHeaderData(final Cursor cursor, Uri contactUri) {
        final String directoryParameter = contactUri.getQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY);
        final long directoryId = directoryParameter == null ? Directory.DEFAULT : Long.parseLong(directoryParameter);
        final long contactId = cursor.getLong(ContactQuery.CONTACT_ID);
        final String lookupKey = cursor.getString(ContactQuery.LOOKUP_KEY);
        final long nameRawContactId = cursor.getLong(ContactQuery.NAME_RAW_CONTACT_ID);
        final int displayNameSource = cursor.getInt(ContactQuery.DISPLAY_NAME_SOURCE);
        final String displayName = cursor.getString(ContactQuery.DISPLAY_NAME);
        final String altDisplayName = cursor.getString(ContactQuery.ALT_DISPLAY_NAME);
        final String phoneticName = cursor.getString(ContactQuery.PHONETIC_NAME);
        final long photoId = cursor.getLong(ContactQuery.PHOTO_ID);
        final String photoUri = cursor.getString(ContactQuery.PHOTO_URI);
        final boolean starred = cursor.getInt(ContactQuery.STARRED) != 0;
        final Integer presence = cursor.isNull(ContactQuery.CONTACT_PRESENCE) ? null : cursor
                .getInt(ContactQuery.CONTACT_PRESENCE);
        final boolean sendToVoicemail = cursor.getInt(ContactQuery.SEND_TO_VOICEMAIL) == 1;
        final String customRingtone = cursor.getString(ContactQuery.CUSTOM_RINGTONE);
        final boolean isUserProfile = cursor.getInt(ContactQuery.IS_USER_PROFILE) == 1;

        Uri lookupUri;
        if (directoryId == Directory.DEFAULT || directoryId == Directory.LOCAL_INVISIBLE) {
            lookupUri = ContentUris.withAppendedId(Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey), contactId);
        } else {
            lookupUri = contactUri;
        }

        /// bird: TASK #7674,custom contacts readonly attr,chengting,@20160301 {
        boolean readonly = cursor.getInt(ContactQuery.INDEX_IS_SDN_CONTACT)==-2;
        Log.d(TAG,"cting,loadContactHeaderData,readonly="+readonly);
        /// @}
        return new Contact(mLookupUri, contactUri, lookupUri, directoryId, lookupKey, contactId, nameRawContactId,
                displayNameSource, photoId, photoUri, displayName, altDisplayName, phoneticName, starred, presence,
                /// bird: TASK #7674,custom contacts readonly attr,chengting,@20160301 {
                sendToVoicemail, customRingtone, isUserProfile, readonly);
                /// @}
    }

    @Override
    protected void onResume() {
        super.onResume();
        UsageReporter.onResume(this, null);
        if (DEBUG)
            Log.d(PERFORMANCE_TAG, "onResume on detail activity");
    }

    @Override
    protected void onPause() {
        super.onPause();
        UsageReporter.onPause(this, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if ((mMenuDialog != null) && mMenuDialog.isShowing()) {
            mMenuDialog.hide();
        }
    }

    private void showMenu() {
        if (mContactData == null) {
            Log.w(TAG, "showMenu: contact is not loaded yet. quit.");
            return;
        }
        if (mMenuDialog == null) {
            mMenuDialog = PopMenu.build(this, Gravity.TOP);
            mMenuDialog.setOnIemClickListener(mOnMenuItemClick);
            /// bird: TASK #7674,custom contacts readonly attr,chengting,@20160301 {
            if(mContactData != null && !mContactData.isReadOnly()) {
                Log.d(TAG,"cting,showMenu, not read only,show delete menu");
                mMenuDialog.addItem(ID_FOOTER_ICON_DELETE, getString(R.string.delete));
            }
            /// @}
            mMenuDialog.addItem(ID_SHARE_TO_MMS, getString(R.string.send_to_mms));
            mMenuDialog.addItem(ID_FOOTER_ICON_SHARE, getString(R.string.share));
            if ((!mIsProfile) && (!mContactData.isSimAccountType())) {
                if(SystemProperties.get("ro.yunos.support.privacy_space", "yes").equals("yes")){
                    mMenuDialog.addItem(ID_FOOTER_ICON_PRIVATE, getString(R.string.footer_btn_add_private));
                }
                mMenuDialog.addItem(ID_FOOTER_ADD_TO_DESK, getString(R.string.send_to_desk_top));
            }
        }
        mMenuDialog.show();
    }

    private void updateFooterBar(Contact contactData, boolean isStared) {
        if (contactData == null) {
            Log.e(TAG, "updateFooterBar() contactData is null!!!");
            return;
        }

        if (!mIsProfile && !contactData.isSimAccountType()) {
            if (isStared == mIsStarred) {
                Log.d(TAG, "updateFooterBar() isStared is not changed, do not refresh footerbar.");
                return;
            }
            mIsStarred = isStared;

            if (mIsStarred) {
                mFavoriteView.setImageResource(R.drawable.ic_cancel_collect);
            } else {
                mFavoriteView.setImageResource(R.drawable.ic_collect);
            }
        } else {
            mFavoriteView.setVisibility(View.GONE);
            if ((mMenuDialog != null) && contactData.isSimAccountType()) {
                mMenuDialog.removeItem(ID_FOOTER_ICON_PRIVATE);
                mMenuDialog.removeItem(ID_FOOTER_ADD_TO_DESK);
            }
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactLoaderFragment) {
            mLoaderFragment = (ContactLoaderFragment) fragment;
            mLoaderFragment.setListener(mLoaderFragmentListener);
            mLoaderFragment.setLoadUri(getIntent().getData());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // First check if the {@link ContactLoaderFragment} can handle the key
        if (mLoaderFragment != null && mLoaderFragment.handleKeyDown(keyCode))
            return true;

        // In the last case, give the key event to the superclass.
        return super.onKeyDown(keyCode, event);
    }

    private final ContactLoaderFragmentListener mLoaderFragmentListener = new ContactLoaderFragmentListener() {
        @Override
        public void onContactNotFound() {
            if (mToastTextWhenNoContact != -1) {
                Toast.makeText(getApplicationContext(), mToastTextWhenNoContact, Toast.LENGTH_LONG).show();
            }

            finish();
        }

        @Override
        public void onEditRequested(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.setClass(ContactDetailActivity.this, ContactEditorActivity.class);
            intent.putExtra(ContactEditorActivity.INTENT_KEY_NOT_VIEW_DETAIL_ON_SAVE_COMPLETED, true);
            // Don't finish the detail activity after launching the
            // editor because when the
            // editor is done, we will still want to show the updated
            // contact details using
            // this activity.
            startActivity(intent);
        }

        @Override
        public void onDeleteRequested(Uri contactUri) {
            ContactDeletionInteraction.start(ContactDetailActivity.this, contactUri, true);
        }
    };

    private final ContactDetailFragment.Listener mContactDetailFragmentListener = new ContactDetailFragment.Listener() {
        @Override
        public void onItemClicked(Intent intent) {
            if (intent == null) {
                return;
            }
            if (getPackageManager().resolveActivity(intent, 0) != null) {
                Log.i(TAG, "activity found, now start activity");
                startActivity(intent);
            } else {
                Log.e(TAG, "No activity found for intent: " + intent);

                // If WEIBO client is not installed, go to browser showing weibo
                // page.
                String scheme = intent.getScheme();
                if (WeiboFinals.CLIENT_SCHEME.equals(scheme)) {
                    Intent intent2 = new Intent();
                    intent2.setAction(Intent.ACTION_VIEW);
                    String uid = intent.getStringExtra(WeiboFinals.EXTRA_KEY_WEIBO_PROFILE_UID);
                    if (uid == null) {
                        String url = WeiboFinals.BROWSER_PROFILE_URL_DEFAULT;

                        String name = intent.getStringExtra(WeiboFinals.EXTRA_KEY_WEIBO_PROFILE_NAME);
                        if (name != null) {
                            url = String.format(WeiboFinals.BROWSER_PROFILE_URL_NAME, name);
                        }
                        Log.i(TAG, "onItemClicked: launch browser url = " + url);
                        intent2.setData(Uri.parse(url));
                        if (getPackageManager().resolveActivity(intent2, 0) != null) {
                            startActivity(intent2);
                        } else {
                            Log.e(TAG, "No activity found for intent: " + intent2);
                        }
                    } else {
                        String url = String.format(WeiboFinals.BROWSER_PROFILE_URL_ID, uid);
                        Log.i(TAG, "onItemClicked: launch browser url = " + url);
                        intent2.setData(Uri.parse(url));
                        if (getPackageManager().resolveActivity(intent2, 0) != null) {
                            startActivity(intent2);
                        } else {
                            Log.e(TAG, "No activity found for intent: " + intent2);
                        }
                    }
                }
            }
        }

        @Override
        public void onCreateRawContactRequested(ArrayList<ContentValues> values, AccountWithDataSet account) {
            Toast.makeText(ContactDetailActivity.this, R.string.toast_making_personal_copy, Toast.LENGTH_LONG).show();
            Intent serviceIntent = ContactSaveService.createNewRawContactIntent(ContactDetailActivity.this, values, account,
                    ContactDetailActivity.class, Intent.ACTION_VIEW);
            startService(serviceIntent);

        }
    };

    /**
     * This interface should be implemented by {@link Fragment}s within this
     * activity so that the activity can determine whether the currently
     * displayed view is handling the key event or not.
     */
    public interface FragmentKeyListener {
        /**
         * Returns true if the key down event will be handled by the
         * implementing class, or false otherwise.
         */
        public boolean handleKeyDown(int keyCode);
    }

    private OnPopMenuListener mOnMenuItemClick = new OnPopMenuListener() {

        @Override
        public void onMenuItemClick(int what) {
            onHandleItemClick(what);
        }
    };

    public boolean onHandleItemClick(int id) {
        if (mLoaderFragment == null || mContactData == null) {
            Log.e(TAG, "onHandleFooterBarItemClick mLoaderFragment is null or mContactData is null");
            return true;
        }

        switch (id) {
            case ID_FOOTER_ICON_SHARE:
                if (mContactData != null) {
                    mLoaderFragment.doShare(mContactData.getId(), false);
                }
                UsageReporter.onClick(this, null, UsageReporter.ContactsDetailPage.DETAL_SHARE_BUTTON_CLICKED);
                break;
            case ID_FOOTER_ICON_PRIVATE:
                addToPrivateContact();
                UsageReporter.onClick(this, null, UsageReporter.ContactsDetailPage.DETAL_PRIVATE_BUTTON_CLICKED);
                break;
            case ID_FOOTER_ICON_DELETE:
				// bird: BUG #15552, [Delete contacts while deleting contacts shortcut],add by zhouzheng,20160908 begin @{
 				 ShortcutIntentBuilder detelebuilder = new ShortcutIntentBuilder(this, new OnShortcutIntentCreatedListener() {
	                   @Override
	                   public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
	                       shortcutIntent.setAction("com.aliyun.homeshell.action.UNINSTALL_SHORTCUT");
	                      sendBroadcast(shortcutIntent);
	                   }
	               });
 				 detelebuilder.createContactShortcutIntent(getIntent().getData(), true);
 				//  @ } bird: BUG #15552 ,[Delete contacts while deleting contacts shortcut],add by zhouzheng,20160908 end 
                mLoaderFragment.doDelete();
                UsageReporter.onClick(this, null, UsageReporter.ContactsDetailPage.DETAL_DELETE_BUTTON_CONFIRMED);
                break;
            case ID_SHARE_TO_MMS:
                if (mContactData != null) {
                    mLoaderFragment.doShare(mContactData.getId(), true);
                }
                UsageReporter.onClick(this, null,
                        UsageReporter.ContactsDetailPage.DETAL_SMS_SHARE_BUTTON_CLICKED);
                break;
            case ID_FOOTER_ADD_TO_DESK:
                ShortcutIntentBuilder builder = new ShortcutIntentBuilder(this, new OnShortcutIntentCreatedListener() {
                    @Override
                    public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
                        shortcutIntent.setAction(Constants.ACTION_INSTALL_SHORTCUT);
                        sendBroadcast(shortcutIntent);
                    }
                });
                builder.createContactShortcutIntent(getIntent().getData(), true);
                UsageReporter.onClick(null, TAG, UsageReporter.ContactsDetailPage.DETAL_LC_SEND_DESKTOP);
                break;
            default:
                break;
        }

        return true;
    }

    private void operateFavorite() {
        mLoaderFragment.doAddFavorite();
        updateFooterBar(mContactData, !mIsStarred);

        UsageReporter.onClick(this, null, mIsStarred ? UsageReporter.ContactsDetailPage.DETAL_STARRED_CLICKED
                : UsageReporter.ContactsDetailPage.DETAL_CANCEL_STARRED_CLICKED);
    }

    private void addToPrivateContact() {
        if (mContactData != null) {
            String name = mContactData.getDisplayName();

            ArrayList<String> numberList = null;
            if (mDetailFragment != null) {
                numberList = mDetailFragment.getContactNumberList();
            }

            if (numberList == null || numberList.isEmpty()) {
                Log.e(TAG, "addToPrivateContact: number list is empty.");
                Toast.makeText(this, R.string.private_error_no_number, Toast.LENGTH_SHORT).show();
            } else {
                String[] numbers = new String[numberList.size()];
                numberList.toArray(numbers);
                boolean success = YunOSFeatureHelper.addNumbersToPrivateContact(this, numbers, name);
                if (success) {
                    Toast.makeText(this, R.string.private_added_success, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "addToPrivateContact: addBatchNumberToPrivacy success = " + success);
            }
        } else {
            Log.e(TAG, "addToPrivateContact: mContactData is null");
        }
    }

    private void startEditSimContactActivity() {
        long rawContactId = mContactData.getSimAccountRawContactId();
        Log.i(TAG, "startEditSimContactActivity: rawContactId="+rawContactId);

        SimContactCache.SimContact cached = rawContactId == -1
                ? null : SimContactCache.getSimContactByRawContactIdWithoutSimId(rawContactId);
        if (cached == null) {
            Toast.makeText(this, R.string.sim_contacts_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = SimContactUtils.makeEditIntentFromCachedSimContact(cached);
        intent.putExtra(SimContactEditorFragment.INTENT_EXTRA_NOT_VIEW_DETAIL_ON_SAVE_COMPLETED, true);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (mLoadContactThread != null && mLoadContactThread.isAlive()) {
            mLoadContactThread.interrupt();
            mLoadContactThread = null;
        }

        if (mContactContentObserver != null) {
            mContentResolver.unregisterContentObserver(mContactContentObserver);
            mContactContentObserver = null;
        }

        if (mCallLogManager != null) {
            mCallLogManager.unRegistCallsTableChangeListener(mCallLogListener);
        }

        if (mStarDialog != null) {
            mStarDialog.dismiss();
            mStarDialog = null;
        }

        super.onDestroy();
    }

    /**
     * Added for Share Center. Get first phone number for this contact. If not
     * exist, return null.
     */
    public String getFirstPhoneNumber() {
        if (mDetailFragment == null) {
            Log.e(TAG, "getFirstPhoneNumber() mDetailFragment is null, return null.");
            return null;
        }

        final ArrayList<String> numberList = mDetailFragment.getContactNumberList();
        if (numberList == null || numberList.isEmpty()) {
            return null;
        }

        return numberList.get(0);
    }
}
