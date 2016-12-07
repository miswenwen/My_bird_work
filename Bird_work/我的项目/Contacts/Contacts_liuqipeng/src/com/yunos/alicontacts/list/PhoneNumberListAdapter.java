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
package com.yunos.alicontacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Callable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.preference.ContactsPreferences;
import com.yunos.alicontacts.util.ContactsTextUtils;
import com.yunos.yundroid.widget.item.HeaderIconText2Item;
import com.yunos.yundroid.widget.itemview.HeaderIconTextItemView;
import com.yunos.yundroid.widget.itemview.ItemView;

import yunos.support.v4.content.CursorLoader;

/**
 * A cursor adapter for the {@link Phone#CONTENT_ITEM_TYPE} and
 * {@link SipAddress#CONTENT_ITEM_TYPE}.
 *
 * By default this adapter just handles phone numbers. When {@link #setUseCallableUri(boolean)} is
 * called with "true", this adapter starts handling SIP addresses too, by using {@link Callable}
 * API instead of {@link Phone}.
 */
public class PhoneNumberListAdapter extends ContactEntryListAdapter {
    private static final String TAG = PhoneNumberListAdapter.class.getSimpleName();

    protected static class PhoneQuery {
        private static final String[] PROJECTION_PRIMARY = new String[] {
            Phone._ID,                          // 0
            Phone.TYPE,                         // 1
            Phone.LABEL,                        // 2
            Phone.NUMBER,                       // 3
            Phone.CONTACT_ID,                   // 4
            Phone.LOOKUP_KEY,                   // 5
            Phone.PHOTO_ID,                     // 6
            Phone.DISPLAY_NAME_PRIMARY,         // 7
            Phone.PHOTO_THUMBNAIL_URI           // 8
        };

        private static final String[] PROJECTION_ALTERNATIVE = new String[] {
            Phone._ID,                          // 0
            Phone.TYPE,                         // 1
            Phone.LABEL,                        // 2
            Phone.NUMBER,                       // 3
            Phone.CONTACT_ID,                   // 4
            Phone.LOOKUP_KEY,                   // 5
            Phone.PHOTO_ID,                     // 6
            Phone.DISPLAY_NAME_ALTERNATIVE,     // 7
            Phone.PHOTO_THUMBNAIL_URI           // 8
        };

        public static final int PHONE_ID           = 0;
        public static final int PHONE_TYPE         = 1;
        public static final int PHONE_LABEL        = 2;
        public static final int PHONE_NUMBER       = 3;
        public static final int PHONE_CONTACT_ID   = 4;
        public static final int PHONE_LOOKUP_KEY   = 5;
        public static final int PHONE_PHOTO_ID     = 6;
        public static final int PHONE_DISPLAY_NAME = 7;
        public static final int PHONE_PHOTO_URI    = 8;
    }

    protected final CharSequence mUnknownNameText;

    private boolean mUseCallableUri;

    public PhoneNumberListAdapter(Context context) {
        super(context);
        setDefaultFilterHeaderText(R.string.list_filter_phones);
        mUnknownNameText = context.getText(android.R.string.unknownName);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        if (directoryId != Directory.DEFAULT) {
            Log.w(TAG, "PhoneNumberListAdapter is not ready for non-default directory ID ("
                    + "directoryId: " + directoryId + ")");
        }

        final Builder builder;
        if (isSearchMode()) {
            final Uri baseUri =
                    mUseCallableUri ? ContactsContract.CommonDataKinds.Callable.CONTENT_FILTER_URI : Phone.CONTENT_FILTER_URI;
            builder = baseUri.buildUpon();
            final String query = getQueryString();
            if (TextUtils.isEmpty(query)) {
                builder.appendPath("");
            } else {
                builder.appendPath(query);      // Builder will encode the query
            }
            builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                    String.valueOf(directoryId));
        } else {
            final Uri baseUri = mUseCallableUri ? ContactsContract.CommonDataKinds.Callable.CONTENT_URI : Phone.CONTENT_URI;
            builder = baseUri.buildUpon().appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT));
            if (isSectionHeaderDisplayEnabled()) {
                builder.appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true");
            }
        }

        // Remove duplicates when it is possible.
        builder.appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true");
        loader.setUri(builder.build());
        loader.setSelection(Phone.CONTACT_ID + " IN visible_contacts");

        // TODO a projection that includes the search snippet
        loader.setProjection(getProjection());

        if (getSortOrder() == ContactsPreferences.SORT_ORDER_PRIMARY) {
            loader.setSortOrder(Phone.SORT_KEY_PRIMARY);
        } else {
            loader.setSortOrder(Phone.SORT_KEY_ALTERNATIVE);
        }
    }

    private String[] getProjection() {
        if (getContactNameDisplayOrder() == ContactsPreferences.DISPLAY_ORDER_PRIMARY) {
            return PhoneQuery.PROJECTION_PRIMARY;
        }
        return PhoneQuery.PROJECTION_ALTERNATIVE;
    }

    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(PhoneQuery.PHONE_DISPLAY_NAME);
    }

    /**
     * Builds a {@link Data#CONTENT_URI} for the given cursor position.
     *
     * @return Uri for the data. may be null if the cursor is not ready.
     */
    public Uri getDataUri(int position) {
        Cursor cursor = ((Cursor)getItem(position));
        if (cursor != null) {
            long id = cursor.getLong(PhoneQuery.PHONE_ID);
            return ContentUris.withAppendedId(Data.CONTENT_URI, id);
        } else {
            Log.w(TAG, "Cursor was null in getDataUri() call. Returning null instead.");
            return null;
        }
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position,
            ViewGroup parent) {
        HeaderIconTextItemView itemView = createItemView(context, parent);
        itemView.setTag(itemView);

        if (!getDisplayPhotos()) {
            itemView.removeIcon();
        } else {
            itemView.showIcon();
        }

        return itemView;
    }

    /**
     * This method creates a view instance for the list.
     * The sub class shall create the correct type of view for its own list.
     * @param context
     * @param parent
     * @return
     */
    protected HeaderIconTextItemView createItemView(Context context, ViewGroup parent) {
        HeaderIconText2Item item = new HeaderIconText2Item();
        HeaderIconTextItemView itemView = (HeaderIconTextItemView) item.newView(context, parent);
        return itemView;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        HeaderIconTextItemView view = (HeaderIconTextItemView)itemView;

        bindSectionHeaderAndDivider(view, position);
        bindName(view, cursor);
        bindPhoto(view, partition ,cursor);
        bindPhoneNumber(view, cursor);
    }

    protected void bindPhoneNumber(HeaderIconTextItemView view, Cursor cursor) {
        CharSequence label = null;
        if (!cursor.isNull(PhoneQuery.PHONE_TYPE)) {
            final int type = cursor.getInt(PhoneQuery.PHONE_TYPE);
            final String customLabel = cursor.getString(PhoneQuery.PHONE_LABEL);

            // TODO cache
            label = Phone.getTypeLabel(getContext().getResources(), type, customLabel);
        }
        String phoneNumber = ContactsUtils.formatPhoneNumberWithCurrentCountryIso(
                cursor.getString(PhoneQuery.PHONE_NUMBER), getContext());
        if (TextUtils.isEmpty(label)) {
            view.setSubtextView(phoneNumber);
        } else {
            view.setSubtextView(label + "  " +phoneNumber);
        }
    }

    protected void bindSectionHeaderAndDivider(ItemView view, int position) {
        if (isSectionHeaderDisplayEnabled()) {
            Placement placement = getItemPlacementInSection(position);
            view.setHeaderTextView(placement.sectionHeader);
        } else {
            view.setHeaderTextView("");
        }
    }

    protected void bindName(final ItemView view, Cursor cursor) {
        //view.showDisplayName(cursor, PhoneQuery.PHONE_DISPLAY_NAME, getContactNameDisplayOrder());
        // Note: we don't show phonetic names any more (see issue 5265330)

        CharSequence name = cursor.getString(PhoneQuery.PHONE_DISPLAY_NAME);
        if (TextUtils.isEmpty(name)) {
            view.setTextView(mUnknownNameText.toString());
        } else {
            view.setTextView(name.toString());
        }

        // when highlight position is equal to current cursor position, set text highlight color, or default.
        if (cursor.getPosition() == getHiliteTopPosition()) {
            view.setTextViewColor(mContext.getResources().getColor(R.color.match_contacts_hilite_color));
        } else {
            view.setTextViewDefaultAppearance(R.style.ContactsTextView);
        }
    }

    protected void bindPhoto(final ItemView view, int partitionIndex, Cursor cursor) {
        if (!isPhotoSupported(partitionIndex)) {
            return;
        }

        // Head Icon display or not.
        if (!getDisplayPhotos()) {
            view.removeIcon();
            return;
        } else {
            view.showIcon();
        }

        // Set the photo, if available
        long photoId = 0;
        if (!cursor.isNull(PhoneQuery.PHONE_PHOTO_ID)) {
            photoId = cursor.getLong(PhoneQuery.PHONE_PHOTO_ID);
        }

        ImageView v = new ImageView(mContext);

        if (photoId != 0) {
            getPhotoLoader().loadThumbnail(v, photoId, false);
        } else {
            final String photoUriString = cursor.getString(PhoneQuery.PHONE_PHOTO_URI);
            final Uri photoUri = (photoUriString == null) ? null : Uri.parse(photoUriString);
            if (photoUri != null) {
                getPhotoLoader().loadDirectoryPhoto(v, photoUri, false);
            } else {
                String nameStr = ContactsTextUtils.getPortraitText(cursor.getString(PhoneQuery.PHONE_DISPLAY_NAME));
                if (ContactsTextUtils.STRING_EMPTY.equals(nameStr)) {
                    getPhotoLoader().loadDirectoryPhoto(v, photoUri, false);
                } else {
                    view.setIcon(nameStr, ContactsTextUtils.getColor(nameStr));
                    return;
                }
            }
        }
        view.setIcon(v);
    }

    public void setUseCallableUri(boolean useCallableUri) {
        mUseCallableUri = useCallableUri;
    }

    public boolean usesCallableUri() {
        return mUseCallableUri;
    }
}
