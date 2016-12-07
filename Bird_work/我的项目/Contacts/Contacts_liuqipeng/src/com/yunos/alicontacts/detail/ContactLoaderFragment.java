/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.yunos.alicontacts.ContactSaveService;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ContactDetailActivity;
import com.yunos.alicontacts.activities.ContactDetailActivity.FragmentKeyListener;
import com.yunos.alicontacts.activities.JoinContactActivity;
import com.yunos.alicontacts.aliutil.libcore.util.Objects;
import com.yunos.alicontacts.model.Contact;
import com.yunos.alicontacts.model.ContactLoader;
import com.yunos.alicontacts.util.PhoneCapabilityTester;

import yunos.support.v4.app.Fragment;
import yunos.support.v4.content.Loader;
//import com.yunos.alicontacts.editor.RingtoneUtils;

/**
 * This is an invisible worker {@link Fragment} that loads the contact details for the contact card.
 * The data is then passed to the listener, who can then pass the data to other {@link View}s.
 */
public class ContactLoaderFragment extends Fragment implements FragmentKeyListener {

    private static final String TAG = ContactLoaderFragment.class.getSimpleName();
    private static final String PERFORMANCE_TAG = "Performance";
    private static final boolean DEBUG = true;

    /** Intent action and extra definition for share center activity*/
    public static final String ACTION_SHOW_SHARE_UI = "android.intent.action.SHOW_SHARE_UI";
    public static final String EXTRA_SHARE_INTENT = "android.intent.extra.SHARE_INTENT";
    public static final String EXTRA_SHARE_CONTACT_LOOKUP_URI = "com.aliyun.sharecenter.EXTRA_CONTACT_LOOKUP_URI";
    public static final String EXTRA_SHARE_CONTACT_NAME = "com.aliyun.sharecenter.EXTRA_CONTACT_NAME";
    public static final String EXTRA_SHARE_CONTACT_TELEPHONE = "com.aliyun.sharecenter.EXTRA_CONTACT_TELEPHONE";

//    private boolean mOptionsMenuOptions;
//    private boolean mOptionsMenuEditable;
//    private boolean mOptionsMenuShareable;
//    private boolean mOptionsMenuCanCreateShortcut;

    private static final int REQUEST_CODE_JOIN = 0;

    /**
     * This is a listener to the {@link ContactLoaderFragment} and will be notified when the
     * contact details have finished loading or if the user selects any menu options.
     */
    public static interface ContactLoaderFragmentListener {
        /**
         * Contact was not found, so somehow close this fragment. This is raised after a contact
         * is removed via Menu/Delete
         */
        public void onContactNotFound();

//        /**
//         * Contact details have finished loading.
//         */
//        public void onDetailsLoaded(Contact result);

        /**
         * User decided to go to Edit-Mode
         */
        public void onEditRequested(Uri lookupUri);

        /**
         * User decided to delete the contact
         */
        public void onDeleteRequested(Uri lookupUri);

    }

    private static final int LOADER_DETAILS = 1;

    private static final String KEY_CONTACT_URI = "contactUri";
//    private static final String LOADER_ARG_CONTACT_URI = "contactUri";

    private Context mContext;
    private Uri mLookupUri;
    private ContactLoaderFragmentListener mListener;

    private Contact mContactData;

    public ContactLoaderFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.d(PERFORMANCE_TAG, "onCreate on loader fragment.");
        if (savedInstanceState != null) {
            mLookupUri = savedInstanceState.getParcelable(KEY_CONTACT_URI);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_CONTACT_URI, mLookupUri);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        // setHasOptionsMenu(true);
        // This is an invisible view.  This fragment is declared in a layout, so it can't be
        // "viewless".  (i.e. can't return null here.)
        // See also the comment in the layout file.
        return inflater.inflate(R.layout.contact_detail_loader_fragment, container, false);
    }
//
//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        if(DEBUG) Log.d(PERFORMANCE_TAG, "initLoader on loader fragment.");
//        if (mLookupUri != null) {
//            Bundle args = new Bundle();
//            args.putParcelable(LOADER_ARG_CONTACT_URI, mLookupUri);
//            getLoaderManager().initLoader(LOADER_DETAILS, args, mDetailLoaderListener);
//        }
//    }
//
    public void setLoadUri(Uri lookupUri) {
        if (Objects.equal(lookupUri, mLookupUri)) {
            // Same URI, no need to load the data again
            return;
        }

        mLookupUri = lookupUri;
//        if (mLookupUri == null) {
//            getLoaderManager().destroyLoader(LOADER_DETAILS);
//            mContactData = null;
//            if (mListener != null) {
//                mListener.onDetailsLoaded(mContactData);
//            }
//
//        } else if (getActivity() != null) {
//            Bundle args = new Bundle();
//            args.putParcelable(LOADER_ARG_CONTACT_URI, mLookupUri);
//            getLoaderManager().restartLoader(LOADER_DETAILS, args, mDetailLoaderListener);
//        }
    }

    public void setListener(ContactLoaderFragmentListener value) {
        mListener = value;
    }

    public void setContactData(Contact contactData) {
        mContactData = contactData;
    }
//
//    /**
//     * The listener for the detail loader
//     */
//    private final LoaderManager.LoaderCallbacks<Contact> mDetailLoaderListener =
//            new LoaderCallbacks<Contact>() {
//        @Override
//        public Loader<Contact> onCreateLoader(int id, Bundle args) {
//            Uri lookupUri = args.getParcelable(LOADER_ARG_CONTACT_URI);
//            return new ContactLoader(mContext, lookupUri, true /* loadGroupMetaData */,
//                    true /* loadStreamItems */, true /* load invitable account types */,
//                    true /* postViewNotification */, true /* computeFormattedPhoneNumber */);
//        }
//
//        @Override
//        public void onLoadFinished(Loader<Contact> loader, Contact data) {
//            if(DEBUG) Log.d(PERFORMANCE_TAG, "onLoadFinished on loader fragment.");
//            if (!mLookupUri.equals(data.getRequestedUri())) {
//                Log.e(TAG, "Different URI: requested=" + mLookupUri + "  actual=" + data);
//                return;
//            }
//
//            if (data.isError()) {
//                // This happens in a monkey case, which starts a background sync together with contacts app.
//                // so the case might cause ContactLoaderUtils.ensureIsContactUri() gets wrong type
//                // of normal contact uri like "content://com.android.contacts/contacts/lookup/xxx/yyy".
//                // It is most probably because the background sync changes the contact,
//                // when ContactLoaderUtils.ensureIsContactUri() trying to get type of the contact uri.
//                // In this scenario, we just behavior as the contact is not found.
//                Log.w(TAG, "onLoadFinished: Failed to load contact.", data.getException());
//                mContactData = null;
//            } else if (data.isNotFound()) {
//                Log.i(TAG, "No contact found: " + ((ContactLoader)loader).getLookupUri());
//                mContactData = null;
//            } else {
//                mContactData = data;
//            }
//
//            if (mListener != null) {
//                if (mContactData == null) {
//                    mListener.onContactNotFound();
//                } else {
//                    mListener.onDetailsLoaded(mContactData);
//                }
//            }
//            // Make sure the options menu is setup correctly with the loaded data.
//            // if (getActivity() != null) getActivity().invalidateOptionsMenu();
//        }
//
//        @Override
//        public void onLoaderReset(Loader<Contact> loader) {}
//    };
//
//    @Override
//    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
//        //inflater.inflate(R.menu.view_contact, menu);
//    }
//
//    public boolean isOptionsMenuChanged() {
//        return mOptionsMenuOptions != isContactOptionsChangeEnabled()
//                || mOptionsMenuEditable != isContactEditable()
//                || mOptionsMenuShareable != isContactShareable()
//                || mOptionsMenuCanCreateShortcut != isContactCanCreateShortcut();
//    }
//
//    @Override
//    public void onPrepareOptionsMenu(Menu menu) {
//    }

    public boolean isContactOptionsChangeEnabled() {
        return mContactData != null && !mContactData.isDirectoryEntry()
                && PhoneCapabilityTester.isPhone(mContext);
    }

    public boolean isContactEditable() {
        return mContactData != null && !mContactData.isDirectoryEntry();
    }

    public boolean isContactShareable() {
        return mContactData != null && !mContactData.isDirectoryEntry();
    }

    public boolean isContactCanCreateShortcut() {
        return mContactData != null && !mContactData.isUserProfile()
                && !mContactData.isDirectoryEntry();
    }

    public void doEdit() {
        if (mListener != null) mListener.onEditRequested(mLookupUri);
    }

    public void doAddFavorite() {
        if(mContactData != null && mLookupUri != null) {
            boolean isStarred = mContactData.getStarred();
            Intent intent = ContactSaveService.createSetStarredIntent(
                    mContext, mLookupUri, !isStarred);
            mContext.startService(intent);
        }
    }


    public void doDelete() {
        if (mListener != null) mListener.onDeleteRequested(mLookupUri);
    }

    public void doShare(long contactId,boolean bToMms) {
        final String lookupKey = mContactData.getLookupKey();
        Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKey);
        Intent intent;
        if (bToMms) {
            intent = new Intent("android.intent.action.SENDTOMMS");
        } else {
            intent = new Intent(Intent.ACTION_SEND);
        }
        if (mContactData.isUserProfile()) {
            // User is sharing the profile.  We don't want to force the receiver to have
            // the highly-privileged READ_PROFILE permission, so we need to request a
            // pre-authorized URI from the provider.
            shareUri = getPreAuthorizedUri(shareUri);
        }

        intent.putExtra("user_profile", mContactData.isUserProfile());
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);
        intent.putExtra("contact_id", contactId);
        intent.putExtra( "filter_str_flag",true);

        Intent shareIntent = null;
        if (bToMms) {
            // Launch chooser to share contact via sms
            final CharSequence chooseTitle = mContext.getText(R.string.share_via);
            shareIntent = Intent.createChooser(intent, chooseTitle);
        } else {
            shareIntent = new Intent(ACTION_SHOW_SHARE_UI);
            shareIntent.putExtra(EXTRA_SHARE_INTENT, intent);
            shareIntent.putExtra(EXTRA_SHARE_CONTACT_LOOKUP_URI, mLookupUri);
            shareIntent.putExtra(EXTRA_SHARE_CONTACT_NAME, mContactData.getDisplayName());
            ContactDetailActivity parentActivity = (ContactDetailActivity)getActivity();
            if (parentActivity != null) {
                shareIntent.putExtra(EXTRA_SHARE_CONTACT_TELEPHONE,
                        parentActivity.getFirstPhoneNumber());
            }
        }

        Log.d(TAG, "intent for share button clicked :" + intent + ", and bToMms = " + bToMms);

        try {
            mContext.startActivity(shareIntent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(mContext, R.string.share_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Calls into the contacts provider to get a pre-authorized version of the given URI.
     */
    private Uri getPreAuthorizedUri(Uri uri) {
        Bundle uriBundle = new Bundle();
        uriBundle.putParcelable(ContactsContract.Authorization.KEY_URI_TO_AUTHORIZE, uri);
        Bundle authResponse = mContext.getContentResolver().call(
                ContactsContract.AUTHORITY_URI,
                ContactsContract.Authorization.AUTHORIZATION_METHOD,
                null,
                uriBundle);
        if (authResponse != null) {
            return (Uri) authResponse.getParcelable(
                    ContactsContract.Authorization.KEY_AUTHORIZED_URI);
        } else {
            return uri;
        }
    }

    @Override
    public boolean handleKeyDown(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL: {
                if (mListener != null) mListener.onDeleteRequested(mLookupUri);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_JOIN: {
                // Ignore failed requests
                if (resultCode != Activity.RESULT_OK) return;
                if (data != null) {
                    final long contactId = ContentUris.parseId(data.getData());
                    joinAggregate(contactId);
                }
                break;
            }
        }
    }

    /** Toggles whether to load stream items. Just for debugging */
    public void toggleLoadStreamItems() {
        Loader<Contact> loaderObj = getLoaderManager().getLoader(LOADER_DETAILS);
        ContactLoader loader = (ContactLoader) loaderObj;
        loader.setLoadStreamItems(!loader.getLoadStreamItems());
    }

    /** Returns whether to load stream items. Just for debugging */
    public boolean getLoadStreamItems() {
        Loader<Contact> loaderObj = getLoaderManager().getLoader(LOADER_DETAILS);
        ContactLoader loader = (ContactLoader) loaderObj;
        return loader != null && loader.getLoadStreamItems();
    }

    /**
     * Shows a list of aggregates that can be joined into the currently viewed aggregate.
     *
     * @param contactLookupUri the fresh URI for the currently edited contact (after saving it)
     */
    public void showJoinAggregateActivity() {
        if (mLookupUri == null) {
            return;
        }

        long contactIdForJoin = ContentUris.parseId(mLookupUri);
        final Intent intent = new Intent(JoinContactActivity.JOIN_CONTACT);
        intent.putExtra(JoinContactActivity.EXTRA_TARGET_CONTACT_ID, contactIdForJoin);
        startActivityForResult(intent, REQUEST_CODE_JOIN);
    }

    /**
     * Performs aggregation with the contact selected by the user from suggestions or A-Z list.
     */
    private void joinAggregate(final long contactId) {
        long contactIdForJoin = ContentUris.parseId(mLookupUri);
        Intent intent = ContactSaveService.createJoinContactsIntent(mContext,contactIdForJoin,
                contactId, true,
                ContactDetailActivity.class, ContactDetailActivity.ACTION_JOIN_COMPLETED);
        mContext.startService(intent);
    }
}
