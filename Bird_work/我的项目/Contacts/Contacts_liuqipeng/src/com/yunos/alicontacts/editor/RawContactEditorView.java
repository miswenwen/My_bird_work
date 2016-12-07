/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.yunos.alicontacts.editor;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.yunos.alicontacts.GroupMetaDataLoader;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.model.RawContact;
import com.yunos.alicontacts.model.RawContactDelta;
import com.yunos.alicontacts.model.RawContactDelta.ValuesDelta;
import com.yunos.alicontacts.model.RawContactModifier;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.model.account.AccountType.EditType;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.alicontacts.model.dataitem.DataKind;

import hwdroid.widget.ActionSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view that provides all the editor interaction for a specific
 * {@link Contacts} represented through an {@link RawContactDelta}. Callers can
 * reuse this view and quickly rebuild its contents through
 * {@link #setState(RawContactDelta, AccountType, ViewIdGenerator)}.
 * <p>
 * Internal updates are performed against {@link ValuesDelta} so that the
 * source {@link RawContact} can be swapped out. Any state-based changes, such as
 * adding {@link Data} rows or changing {@link EditType}, are performed through
 * {@link RawContactModifier} to ensure that {@link AccountType} are enforced.
 */
public class RawContactEditorView extends BaseRawContactEditorView {
    //private static final String TAG = RawContactEditorView.class.getSimpleName();

    private LayoutInflater mInflater;

    private StructuredNameEditorView mName;
    //private PhoneticNameEditorView mPhoneticName;
    private GroupMembershipView mGroupMembershipView;
    private RingtoneView mRingtoneView;

    private ViewGroup mFields;

    private ImageView mAccountIcon;
    private TextView mAccountTypeTextView;
    private TextView mAccountNameTextView;
    private AccountWithDataSet mAccount = null;

    private TextView mAddField;

    private long mRawContactId = -1;
    private List<GroupMetaDataLoader.LoadedGroup> mGroupMetaData;
    private DataKind mGroupMembershipKind;
    private RawContactDelta mState;

    //private boolean mPhoneticNameAdded;

    //private Listener mListener;

//    public interface Listener {
//        void onRingtoneRequest();
//        void onStarred(boolean starred);
//    }

//    public void setListener(Listener listener) {
//        mListener = listener;
//    }

    public RawContactEditorView(Context context) {
        super(context);
    }

    public RawContactEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        View view = getPhotoEditor();
        if (view != null) {
            view.setEnabled(enabled);
        }

        if (mName != null) {
            mName.setEnabled(enabled);
        }

        if (mFields != null) {
            int count = mFields.getChildCount();
            for (int i = 0; i < count; i++) {
                mFields.getChildAt(i).setEnabled(enabled);
            }
        }

        if (mGroupMembershipView != null) {
            mGroupMembershipView.setEnabled(enabled);
        }

        if (mRingtoneView != null) {
            mRingtoneView.setEnabled(enabled);
        }

        mAddField.setEnabled(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mName = (StructuredNameEditorView)findViewById(R.id.edit_name);
        mName.setDeletable(false);

        mFields = (ViewGroup)findViewById(R.id.sect_fields);

        mAccountIcon = (ImageView) findViewById(R.id.account_icon);
        mAccountTypeTextView = (TextView) findViewById(R.id.account_type);
        mAccountNameTextView = (TextView) findViewById(R.id.account_name);

        mGroupMembershipView = (GroupMembershipView) findViewById(R.id.group_membership);

        mAddField = (TextView) findViewById(R.id.button_add_field);
        mAddField.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // showAddInformationPopupWindow();
                if (mAddField != null) {
                    mAddField.setEnabled(false);
                }
                showAddInformationPopupDialog(v);
            }
        });

        mRingtoneView = (RingtoneView) findViewById(R.id.ringtone_editor);
        mRingtoneView.prepareView();

    }

    /**
     * Set the internal state for this view, given a current
     * {@link RawContactDelta} state and the {@link AccountType} that
     * apply to that state.
     */
    @Override
    public void setState(RawContactDelta state, AccountType type, ViewIdGenerator vig,
            boolean isProfile) {
        mState = state;

        fillAccountFromState(state);

        // Remove any existing sections
        mFields.removeAllViews();

        // Bail if invalid state or account type
        if (state == null || type == null) return;

        setId(vig.getId(state, null, null, ViewIdGenerator.NO_VIEW_INDEX));

        // Make sure we have a StructuredName and Organization
        RawContactModifier.ensureKindExists(state, type, StructuredName.CONTENT_ITEM_TYPE);
        RawContactModifier.ensureKindExists(state, type, Phone.CONTENT_ITEM_TYPE);
        RawContactModifier.ensureKindExists(state, type, Email.CONTENT_ITEM_TYPE);

        mRawContactId = state.getRawContactId();

        // Fill in the account info
        if (isProfile) {
            String accountName = state.getAccountName();
            if (TextUtils.isEmpty(accountName)) {
                mAccountNameTextView.setVisibility(View.GONE);
                mAccountTypeTextView.setText(R.string.local_profile_title);
            } else {
                CharSequence accountType = type.getDisplayLabel(mContext);
                mAccountTypeTextView.setText(mContext.getString(R.string.external_profile_title,
                        accountType));
                mAccountNameTextView.setText(accountName);
            }

            mGroupMembershipView.setVisibility(View.GONE);
            mRingtoneView.setVisibility(View.GONE);
        } else {
            String accountName = state.getAccountName();
            CharSequence accountType = type.getDisplayLabel(mContext);
            if (TextUtils.isEmpty(accountType)) {
                accountType = mContext.getString(R.string.account_phone);
            }
            if (!TextUtils.isEmpty(accountName)) {
                mAccountNameTextView.setVisibility(View.VISIBLE);
                mAccountNameTextView.setText(accountName);
            } else {
                // Hide this view so the other text view will be centered vertically
                mAccountNameTextView.setVisibility(View.GONE);
            }
            mAccountTypeTextView.setText(
                    mContext.getString(R.string.account_type_format, accountType));
        }
        mAccountIcon.setImageDrawable(type.getDisplayIcon(mContext));

        // Show photo editor when supported
        RawContactModifier.ensureKindExists(state, type, Photo.CONTENT_ITEM_TYPE);
        setHasPhotoEditor((type.getKindForMimetype(Photo.CONTENT_ITEM_TYPE) != null));
        getPhotoEditor().setEnabled(isEnabled());
        mName.setEnabled(isEnabled());

        // Show and hide the appropriate views
        mFields.setVisibility(View.VISIBLE);
        mName.setVisibility(View.VISIBLE);

        mGroupMembershipKind = type.getKindForMimetype(GroupMembership.CONTENT_ITEM_TYPE);
        if ((mGroupMembershipKind != null) && (!isProfile)) {
            mGroupMembershipView.setKind(mGroupMembershipKind);
            mGroupMembershipView.setEnabled(isEnabled());
        }

        // Create editor sections for each possible data kind
        ArrayList<DataKind> kinds = type.getSortedDataKinds();
        for (DataKind kind : kinds) {
            // Skip kind of not editable
            if (!kind.editable) continue;

            final String mimeType = kind.mimeType;
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Handle special case editor for structured name
                final ValuesDelta primary = state.getPrimaryEntry(mimeType);
                mName.setValues(
                        type.getKindForMimetype(mimeType),
                        primary, state, false, vig);
            } else if (Photo.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Handle special case editor for photos
                final ValuesDelta primary = state.getPrimaryEntry(mimeType);
                getPhotoEditor().setValues(kind, primary, state, false, vig);
            } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                if (!isProfile) {
                    mGroupMembershipView.setState(state);
                }
            } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Create the organization section
                final KindSectionView section = (KindSectionView) mInflater.inflate(
                        R.layout.item_kind_section, mFields, false);
                //section.setTitleVisible(true);
                section.setEnabled(isEnabled());
                section.setState(kind, state, false, vig);

                mFields.addView(section);
            } else {
                // Otherwise use generic section-based editors
                if (kind.fieldList == null) continue;
                final KindSectionView section = (KindSectionView)mInflater.inflate(
                        R.layout.item_kind_section, mFields, false);
                section.setEnabled(isEnabled());
                section.setState(kind, state, false, vig);
                mFields.addView(section);
            }
        }

        final int sectionCount = getSectionViewsWithoutFields().size();
        mAddField.setVisibility(sectionCount > 0 ? View.VISIBLE : View.GONE);
        mAddField.setEnabled(isEnabled());
    }

    @Override
    public void setGroupMetaData(Cursor groupMetaData) {
        mGroupMetaData = GroupMetaDataLoader.LoadedGroup.filterAccountFromCursor(groupMetaData, mAccount);
        if (mGroupMembershipView != null) {
            mGroupMembershipView.setGroupMetaData(mGroupMetaData);
        }
    }

    /**
     * Returns the default group (e.g. "My Contacts") for the current raw contact's
     * account.  Returns -1 if there is no such group.
     */
    /*private long getDefaultGroupId() {
//        String accountType = mState.getAccountType();
//        String accountName = mState.getAccountName();
        String accountDataSet = mState.getDataSet();
        mGroupMetaData.moveToPosition(-1);
        while (mGroupMetaData.moveToNext()) {
//            String name = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_NAME);
//            String type = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            String dataSet = mGroupMetaData.getString(GroupMetaDataLoader.DATA_SET);
//            if (name.equals(accountName) && type.equals(accountType)
//                    && Objects.equal(dataSet, accountDataSet)) {
            if (Objects.equal(dataSet, accountDataSet)) {
                long groupId = mGroupMetaData.getLong(GroupMetaDataLoader.GROUP_ID);
                if (!mGroupMetaData.isNull(GroupMetaDataLoader.AUTO_ADD)
                            && mGroupMetaData.getInt(GroupMetaDataLoader.AUTO_ADD) != 0) {
                    return groupId;
                }
            }
        }
        return -1;
    }*/

    public TextFieldsEditorView getNameEditor() {
        return mName;
    }

    @Override
    public long getRawContactId() {
        return mRawContactId;
    }

    /**
     * Return a list of KindSectionViews that have no fields yet...
     * these are candidates to have fields added in
     * {@link #showAddInformationPopupWindow()}
     */
    private ArrayList<KindSectionView> getSectionViewsWithoutFields() {
        final ArrayList<KindSectionView> fields =
                new ArrayList<KindSectionView>(mFields.getChildCount());
        for (int i = 0; i < mFields.getChildCount(); i++) {
            View child = mFields.getChildAt(i);
            if (child instanceof KindSectionView) {
                final KindSectionView sectionView = (KindSectionView) child;
                // If the section is already visible (has 1 or more editors), then don't offer the
                // option to add this type of field in the popup menu
                if (sectionView.getEditorCount() > 0) {
                    continue;
                }
                DataKind kind = sectionView.getKind();
                // not a list and already exists? ignore
                if ((kind.typeOverallMax == 1) && sectionView.getEditorCount() != 0) {
                    continue;
                }
                if (DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME.equals(kind.mimeType)) {
                    continue;
                }

                fields.add(sectionView);
            }
        }
        return fields;
    }

    private void showAddInformationPopupDialog(View v) {
        final ArrayList<KindSectionView> fields = getSectionViewsWithoutFields();
        if(fields == null || fields.isEmpty()) {
            return;
        }

        int size = fields.size();

        ArrayList<String> items_str = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            items_str.add(fields.get(i).getTitle());
        }

        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
        }
        final ActionSheet actionSheet = new ActionSheet(getContext());
//        actionSheet.setTitle(getContext().getString(R.string.add_field));
        actionSheet.setCommonButtons(items_str, null, null, new ActionSheet.CommonButtonListener() {
            @Override
            public void onDismiss(ActionSheet arg0) {
                if (mAddField != null) {
                    mAddField.setEnabled(true);
                }
            }

            @Override
            public void onClick(int which) {
                final KindSectionView view = fields.get(which);
                if (!DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(view.getKind().mimeType)) {
                    view.addItem(true);
                }

                // If this was the last section without an entry, we just added one, and therefore
                // there's no reason to show the button.
                if (fields.size() == 1) {
                    mAddField.setVisibility(View.GONE);
                }
            }
        });
        actionSheet.show(v);
    }

    public RingtoneView getRingtoneView() {
        return mRingtoneView;
    }

    private void fillAccountFromState(RawContactDelta state) {
        mAccount = new AccountWithDataSet(state.getAccountName(), state.getAccountType(), state.getDataSet());
    }

}
