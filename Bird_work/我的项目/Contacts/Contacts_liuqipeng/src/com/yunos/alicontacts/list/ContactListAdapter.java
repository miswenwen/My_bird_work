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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.preference.ContactsPreferences;
import com.yunos.alicontacts.sim.SimContactCache;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.ContactsTextUtils;
import com.yunos.yundroid.widget.item.HeaderIconTextItem;
import com.yunos.yundroid.widget.itemview.HeaderIconTextItemView;
import com.yunos.yundroid.widget.itemview.ItemView;

//bird #7674
import android.provider.ContactsContract.RawContacts;
/**
 * A cursor adapter for the {@link ContactsContract.Contacts#CONTENT_TYPE} content type.
 * Also includes support for including the {@link ContactsContract.Profile} record in the
 * list.
 */
public abstract class ContactListAdapter extends ContactEntryListAdapter {

    protected static class ContactQuery {
        public static final String COLUMN_ACCOUNT_NAME = "account_name";
        public static final String COLUMN_ACCOUNT_TYPE = "account_type";
        private static final String[] CONTACT_PROJECTION_PRIMARY = new String[] {
                Contacts._ID,                       // 0
                Contacts.DISPLAY_NAME_PRIMARY,      // 1
                Contacts.PHOTO_ID,                  // 2
                Contacts.PHOTO_THUMBNAIL_URI,       // 3
                Contacts.LOOKUP_KEY,                // 4
                Contacts.STARRED,                   // 5
                Contacts.NAME_RAW_CONTACT_ID,        // 6
                COLUMN_ACCOUNT_NAME,                  // 7
                COLUMN_ACCOUNT_TYPE                   // 8
                /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
				,RawContacts.IS_SDN_CONTACT
                /// @}				
        };

        private static final String[] CONTACT_PROJECTION_ALTERNATIVE = new String[] {
                Contacts._ID,                       // 0
                Contacts.DISPLAY_NAME_ALTERNATIVE,  // 1
                Contacts.PHOTO_ID,                  // 2
                Contacts.PHOTO_THUMBNAIL_URI,       // 3
                Contacts.LOOKUP_KEY,                // 4
                Contacts.STARRED,                   // 5
                Contacts.NAME_RAW_CONTACT_ID,        // 6
                COLUMN_ACCOUNT_NAME,                  // 7
                COLUMN_ACCOUNT_TYPE                   // 8
                /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
                ,RawContacts.IS_SDN_CONTACT
                /// @}
        };

        public static final int CONTACT_ID                  = 0;
        public static final int CONTACT_DISPLAY_NAME        = 1;
        public static final int CONTACT_PHOTO_ID            = 2;
        public static final int CONTACT_PHOTO_URI           = 3;
        public static final int CONTACT_LOOKUP_KEY          = 4;
        public static final int CONTACT_STARRED             = 5;
        public static final int CONTACT_NAME_RAW_CONTACT_ID = 6;
        public static final int CONTACT_NAME_RAW_CONTACT_ACCOUNT_NAME = 7;
        public static final int CONTACT_NAME_RAW_CONTACT_ACCOUNT_TYPE = 8;
		
        /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
        public static final int IS_SDN_CONTACT = 9;
        /// @}		
    }

    protected CharSequence mUnknownNameText;

    private long mSelectedContactDirectoryId;
    private String mSelectedContactLookupKey;
    private long mSelectedContactId;

    // private boolean mIsShowFavoriteContacts = false;
    // private int mFavoriteContactsOffset;
    //
    // public void setShowFavoriteContacts(boolean showFavoriteContacts){
    // mIsShowFavoriteContacts = showFavoriteContacts;
    // }

    public ContactListAdapter(Context context) {
        super(context);

        mUnknownNameText = context.getText(R.string.missing_name);
    }

    public CharSequence getUnknownNameText() {
        return mUnknownNameText;
    }

    public long getSelectedContactDirectoryId() {
        return mSelectedContactDirectoryId;
    }

    public String getSelectedContactLookupKey() {
        return mSelectedContactLookupKey;
    }

    public long getSelectedContactId() {
        return mSelectedContactId;
    }

    public void setSelectedContact(long selectedDirectoryId, String lookupKey, long contactId) {
        mSelectedContactDirectoryId = selectedDirectoryId;
        mSelectedContactLookupKey = lookupKey;
        mSelectedContactId = contactId;
    }

    protected static Uri buildSectionIndexerUri(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true").build();
    }

    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(ContactQuery.CONTACT_DISPLAY_NAME);
    }

    public long getContactId(int position) {
        Cursor item = (Cursor)getItem(position);
        return item.getLong(ContactQuery.CONTACT_ID);
    }

    public long getRawContactId(int position) {
        Cursor item = (Cursor)getItem(position);
        return item.getLong(ContactQuery.CONTACT_NAME_RAW_CONTACT_ID);
    }

    public boolean isSimContact(int position) {
        Cursor item = (Cursor)getItem(position);
        if (item == null) {
            return false;
        }
        String accountType = item.getString(ContactQuery.CONTACT_NAME_RAW_CONTACT_ACCOUNT_TYPE);
        return SimContactUtils.SIM_ACCOUNT_TYPE.equals(accountType);
    }

    /**
     * Builds the {@link Contacts#CONTENT_LOOKUP_URI} for the given
     * {@link ListView} position.
     */
    public Uri getContactUri(int position) {
        int partitionIndex = getPartitionForPosition(position);
        Cursor item = (Cursor)getItem(position);
        return item != null ? getContactUri(partitionIndex, item) : null;
    }

    public Uri getContactUri(int partitionIndex, Cursor cursor) {
        long contactId = cursor.getLong(ContactQuery.CONTACT_ID);
        String lookupKey = cursor.getString(ContactQuery.CONTACT_LOOKUP_KEY);
        Uri uri = Contacts.getLookupUri(contactId, lookupKey);
        long directoryId = ((DirectoryPartition)getPartition(partitionIndex)).getDirectoryId();
        if (directoryId != Directory.DEFAULT) {
            uri = uri.buildUpon().appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(directoryId)).build();
        }
        return uri;
    }

    /**
     * Returns true if the specified contact is selected in the list. For a
     * contact to be shown as selected, we need both the directory and and the
     * lookup key to be the same. We are paying no attention to the contactId,
     * because it is volatile, especially in the case of directories.
     */
    public boolean isSelectedContact(int partitionIndex, Cursor cursor) {
        long directoryId = ((DirectoryPartition)getPartition(partitionIndex)).getDirectoryId();
        if (getSelectedContactDirectoryId() != directoryId) {
            return false;
        }
        String lookupKey = getSelectedContactLookupKey();
        if (lookupKey != null && TextUtils.equals(lookupKey,
                cursor.getString(ContactQuery.CONTACT_LOOKUP_KEY))) {
            return true;
        }

        return directoryId != Directory.DEFAULT && directoryId != Directory.LOCAL_INVISIBLE
                && getSelectedContactId() == cursor.getLong(ContactQuery.CONTACT_ID);
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position,
            ViewGroup parent) {
        //ContactListItemView view = new ContactListItemView(context, null);
        HeaderIconTextItem item = new HeaderIconTextItem();
        HeaderIconTextItemView itemView = (HeaderIconTextItemView)item.newView(context, parent);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.view = itemView;
        itemView.setTag(viewHolder);

        if(!getDisplayPhotos()){
            itemView.removeIcon();
        } else{
            itemView.showIcon();
        }

        //ContactListItemView2 viewItem = new ContactListItemView2(context, parent);
        //viewItem.setUnknownNameText(mUnknownNameText);
        //view.setQuickContactEnabled(isQuickContactEnabled());
        //view.setActivatedStateSupported(isSelectionVisible());
        //return view;
        //viewItem.getMainView().setTag(viewItem);
        return itemView;
    }

    protected void bindSectionHeaderAndDivider(ItemView view, int position,
            Cursor cursor) {

        // if(position == 0 & mFavoriteContactsOffset>0){
        // view.setHeaderTextView("★");
        // return;
        // }
        //
        // if(position < mFavoriteContactsOffset){
        // view.setHeaderTextView("");
        // return;
        // }

        if (isSectionHeaderDisplayEnabled()) {
            // position -= mFavoriteContactsOffset;
            Placement placement = getItemPlacementInSection(position);

            // First position, set the contacts number string
            //if (position == 0 && cursor.getInt(ContactQuery.CONTACT_IS_USER_PROFILE) == 1) {
            //    view.setCountView(getContactsCount());
            //} else {
            //    view.setCountView(null);
            //}
            //view.setSectionHeader(placement.sectionHeader);
            //view.setDividerVisible(!placement.lastInSection);
            view.setHeaderTextView(placement.sectionHeader);
            //view.setTypeTextViewText(placement.sectionHeader);
            //view.setTypeTextViewVisible(placement.firstInSection);
            //view.setTypeTextViewVisible(true);
            //view.setDividerImageViewVisible(!placement.lastInSection);
        } else {
            view.setHeaderTextView("");
            //view.setSectionHeader(null);
            //view.setDividerVisible(true);
            //view.setCountView(null);
            //view.setTypeTextViewVisible(false);
            //view.setDividerImageViewVisible(true);
        }
    }

    protected void bindPhoto(final ItemView view, int partitionIndex, Cursor cursor) {
        if (!isPhotoSupported(partitionIndex)) {
            //view.removePhotoView();
            return;
        }

        //Head Icon display or not.
        if(!getDisplayPhotos()){
            view.removeIcon();
            return;
        }
        else{
            view.showIcon();
        }

        // Set the photo, if available
        long photoId = 0;
        if (!cursor.isNull(ContactQuery.CONTACT_PHOTO_ID)) {
            photoId = cursor.getLong(ContactQuery.CONTACT_PHOTO_ID);
        }

        ImageView v = new ImageView(mContext);

        if (photoId != 0) {
            getPhotoLoader().loadThumbnail(v, photoId, false);
        } else {
            final String photoUriString = cursor.getString(ContactQuery.CONTACT_PHOTO_URI);

            final Uri photoUri = (photoUriString == null) ? null : Uri.parse(photoUriString);
            if (photoUri != null) {
                getPhotoLoader().loadDirectoryPhoto(v, photoUri, false);
            } else {
                CharSequence name = cursor.getString(ContactQuery.CONTACT_DISPLAY_NAME);
                String nameStr = ContactsTextUtils.getPortraitText(name);
                if (ContactsTextUtils.STRING_EMPTY.equals(nameStr)) {
                    setDefaultAvator(v, cursor);
                } else {
                    view.setIcon(nameStr, ContactsTextUtils.getColor(nameStr));
                    setSimIndicator(cursor, view);
                    return;
                }
            }
        }
        view.setIcon(v);
        setSimIndicator(cursor, view);
    }

    private void setDefaultAvator(ImageView image, Cursor cursor) {
        long rawContactId = cursor.isNull(ContactQuery.CONTACT_NAME_RAW_CONTACT_ID) ?
                -1 : cursor.getLong(ContactQuery.CONTACT_NAME_RAW_CONTACT_ID);
        SimContactCache.SimContact simContact = SimContactCache.getSimContactByRawContactIdWithoutSimId(rawContactId);
        int subId = simContact == null ? -1 : simContact.slotId;
        int resId = R.drawable.contact_list_avatar_border_acquiesce;
        if (subId == SimUtil.SLOT_ID_1) {
            resId = R.drawable.contact_detail_avatar_border_acquiesce_card1;
        } else if (subId == SimUtil.SLOT_ID_2) {
            resId = R.drawable.contact_detail_avatar_border_acquiesce_card2;
        }
        image.setImageResource(resId);
    }

    private void setSimIndicator(Cursor cursor, ItemView view) {
        String accountType = cursor.getString(ContactQuery.CONTACT_NAME_RAW_CONTACT_ACCOUNT_TYPE);
        String accountName = cursor.getString(ContactQuery.CONTACT_NAME_RAW_CONTACT_ACCOUNT_NAME);
        int resId = -1;
        if (!SimContactUtils.SIM_ACCOUNT_TYPE.equals(accountType)) {
            view.setRBIconIndicator(null);
            return;
        } else if (SimContactUtils.SIM_ACCOUNT_NAME_SIM1.equals(accountName)) {
            resId = R.drawable.contact_avatar_acquiesce_subscript_sim1;
        } else if (SimContactUtils.SIM_ACCOUNT_NAME_SIM2.equals(accountName)) {
            resId = R.drawable.contact_avatar_acquiesce_subscript_sim2;
        } else if (SimContactUtils.SIM_ACCOUNT_NAME.equals(accountName)) {
            resId = R.drawable.contact_avatar_acquiesce_subscript_sim;
        } else {
            view.setRBIconIndicator(null);
            return;
        }
        ImageView ind = new ImageView(mContext);
        ind.setImageResource(resId);
        view.setRBIconIndicator(ind);
    }

    protected void bindName(final ItemView view, Cursor cursor) {

        CharSequence name = cursor.getString(ContactQuery.CONTACT_DISPLAY_NAME);
        if (TextUtils.isEmpty(name)) {
            view.setTextView(mUnknownNameText.toString());
        } else {
            view.setTextView(name.toString());
        }

        // when highlight position is equal to current cursor position, set text highlight color, or default.
        if (cursor.getPosition() == getHiliteTopPosition()) {
            view.setTextViewColor(mContext.getResources().getColor(R.color.match_contacts_hilite_color));
        } else {
            view.setTextViewColor(mContext.getResources().getColor(R.color.aui_primary_txt_color_black));
        }
    }

    protected void bindStarred(final ItemView view, Cursor cursor) {
        /*
        view.showStarred(
                cursor, ContactQuery.CONTACT_STAREED);
                */
    }

    protected void bindCheckBox(final ItemView view, int pos) {

    }

    protected void bindPresenceAndStatusMessage(final ItemView view, Cursor cursor) {
        /*
        view.showPresenceAndStatusMessage(cursor, ContactQuery.CONTACT_PRESENCE_STATUS,
                ContactQuery.CONTACT_CONTACT_STATUS);
                */
    }

    protected void bindSearchSnippet(final ItemView view, Cursor cursor) {
        /*
        view.showSnippet(cursor, ContactQuery.CONTACT_SNIPPET);
        */
    }

    public int getSelectedContactPosition() {
        if (mSelectedContactLookupKey == null && mSelectedContactId == 0) {
            return -1;
        }

        Cursor cursor = null;
        int partitionIndex = -1;
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            DirectoryPartition partition = (DirectoryPartition) getPartition(i);
            if (partition.getDirectoryId() == mSelectedContactDirectoryId) {
                partitionIndex = i;
                break;
            }
        }
        if (partitionIndex == -1) {
            return -1;
        }

        cursor = getCursor(partitionIndex);
        if (cursor == null) {
            return -1;
        }

        cursor.moveToPosition(-1);      // Reset cursor
        int offset = -1;
        while (cursor.moveToNext()) {
            if (mSelectedContactLookupKey != null) {
                String lookupKey = cursor.getString(ContactQuery.CONTACT_LOOKUP_KEY);
                if (mSelectedContactLookupKey.equals(lookupKey)) {
                    offset = cursor.getPosition();
                    break;
                }
            }
            if (mSelectedContactId != 0 && (mSelectedContactDirectoryId == Directory.DEFAULT
                    || mSelectedContactDirectoryId == Directory.LOCAL_INVISIBLE)) {
                long contactId = cursor.getLong(ContactQuery.CONTACT_ID);
                if (contactId == mSelectedContactId) {
                    offset = cursor.getPosition();
                    break;
                }
            }
        }
        if (offset == -1) {
            return -1;
        }

        int position = getPositionForPartition(partitionIndex) + offset;
        if (hasHeader(partitionIndex)) {
            position++;
        }
        return position;
    }

    public boolean hasValidSelection() {
        return getSelectedContactPosition() != -1;
    }

    public Uri getFirstContactUri() {
        int partitionCount = getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            DirectoryPartition partition = (DirectoryPartition) getPartition(i);
            if (partition.isLoading()) {
                continue;
            }

            Cursor cursor = getCursor(i);
            if (cursor != null && cursor.moveToFirst()) {
                return getContactUri(i, cursor);
            }
        }

        return null;
    }

    // @Override
    // public void changeCursor(int partitionIndex, Cursor cursor) {
    // super.changeCursor(partitionIndex, cursor);
    //
    // // Check if a profile exists
    // if (cursor != null && cursor.getCount() > 0) {
    // cursor.moveToFirst();
    // setProfileExists(/*cursor.getInt(ContactQuery.CONTACT_IS_USER_PROFILE) ==
    // 1*/false);
    // }
    // if(mIsShowFavoriteContacts){
    // mFavoriteContactsOffset =
    // ContactEntryListFragment.getFavoriteContactsCount();
    // }else{
    // mFavoriteContactsOffset = 0;
    // }
    // }

    /**
     * @return Projection useful for children.
     */
    protected final String[] getProjection(boolean forSearch) {
        final int sortOrder = getContactNameDisplayOrder();
        String[] project;

        /* Because CONTACT_PROJECTION_XXX and FILTER_PROJECTION_XXX are the same.
         * so only need to check sort order.
        if (forSearch) {
            if (sortOrder == ContactsPreferences.DISPLAY_ORDER_PRIMARY) {
                project = ContactQuery.FILTER_PROJECTION_PRIMARY;
            } else {
                project = ContactQuery.FILTER_PROJECTION_ALTERNATIVE;
            }
        } else {
            if (sortOrder == ContactsPreferences.DISPLAY_ORDER_PRIMARY) {
                project = ContactQuery.CONTACT_PROJECTION_PRIMARY;
            } else {
                project = ContactQuery.CONTACT_PROJECTION_ALTERNATIVE;
            }
        }*/
        if (sortOrder == ContactsPreferences.DISPLAY_ORDER_PRIMARY) {
            project = ContactQuery.CONTACT_PROJECTION_PRIMARY;
        } else {
            project = ContactQuery.CONTACT_PROJECTION_ALTERNATIVE;
        }

        return project;
    }

    public class ViewHolder {
        public View view;
        public boolean starred;
        String name;
        String sectionHeaderName;
        /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
        int sdn=-1;
        public boolean isReadonly(){
            return sdn == -2;
        }
        /// @}
    }
}
