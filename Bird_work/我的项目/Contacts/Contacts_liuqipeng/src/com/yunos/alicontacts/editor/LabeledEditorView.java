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

package com.yunos.alicontacts.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Contacts.Data;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ContactEditorActivity;
import com.yunos.alicontacts.aliutil.provider.AliContactsContract.CommonDataKinds.Sns;
import com.yunos.alicontacts.model.RawContactDelta;
import com.yunos.alicontacts.model.RawContactDelta.ValuesDelta;
import com.yunos.alicontacts.model.RawContactModifier;
import com.yunos.alicontacts.model.account.AccountType.EditType;
import com.yunos.alicontacts.model.dataitem.DataKind;
import com.yunos.alicontacts.util.DialogManager;
import com.yunos.alicontacts.util.DialogManager.DialogShowingView;
import com.yunos.common.UsageReporter;

import hwdroid.widget.ActionSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for editors that handles labels and values. Uses
 * {@link ValuesDelta} to read any existing {@link RawContact} values, and to
 * correctly write any changes values.
 */
public abstract class LabeledEditorView extends LinearLayout implements Editor, DialogShowingView {
    protected static final String DIALOG_ID_KEY = "dialog_id";
    private static final int DIALOG_ID_CUSTOM = 1;

    private static final String TAG = "LabeledEditorView";

    private static final int INPUT_TYPE_CUSTOM = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS;

    private View mLabel;
    private TextView mLabelText;
    private ImageView mLabelIcon;
    private ActionSheet mSpinnerDialog;
    private hwdroid.dialog.AlertDialog mCustomLabelDialog;
    private EditText mCustomLabelEditText;
    private CharSequence[] mStr_items;
    //private EditTypeAdapter mEditTypeAdapter;
    private EditTypeManager mEditTypeManager;
    //private View mDeleteContainer;
    private ImageView mDelete;
    //private ImageView mKindTypeIcon;

    private DataKind mKind;
    private ValuesDelta mEntry;
    private RawContactDelta mState;
    private boolean mReadOnly;
    private boolean mWasEmpty = true;
    private boolean mIsDeletable = true;
    private boolean mIsAttachedToWindow;

    private EditType mType;

    private ViewIdGenerator mViewIdGenerator;
    private DialogManager mDialogManager = null;
    protected EditorListener mListener;
    protected int mMinLineItemHeight;

    protected Context mContext;

    /**
     * A marker in the spinner adapter of the currently selected custom type.
     */
    public static final EditType CUSTOM_SELECTION = new EditType(0, 0);

    private ActionSheet.SingleChoiceListener mSpinnerListener =
            new ActionSheet.SingleChoiceListener() {
                @Override
                public void onDismiss(ActionSheet as) {
                }

                @Override
                public void onClick(int pos) {
                    Log.i(TAG,"mSpinnerListener#onclick() : which = " + pos);
                    onTypeSelectionChange(pos);
                }
            };

    /*private OnItemSelectedListener mSpinnerListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(
                AdapterView<?> parent, View view, int position, long id) {
            onTypeSelectionChange(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };*/
    private  String mPreMineType;

    public LabeledEditorView(Context context) {
        super(context);
        init(context);
    }

    public LabeledEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LabeledEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mMinLineItemHeight = context.getResources().getDimensionPixelSize(
                R.dimen.editor_min_line_item_height);
    }

    private void showSpinnerDialog(View v) {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
        }
        if (mStr_items != null) {

            String title = TextUtils.isEmpty(mLabelText.getText()) ? mStr_items[0].toString() : mLabelText.getText().toString();
            int pos = 0;
            //TODO:Remove When AUI ready.
            String[] item_str = new String[mStr_items.length];
            for (int i = 0; i < mStr_items.length; i++) {
                item_str[i] = mStr_items[i].toString();
                if (title.equals(item_str[i])) {
                    pos = i;
                }
            }

            if (mSpinnerDialog == null) {
                mSpinnerDialog  = new ActionSheet(v.getContext());
            }
            mSpinnerDialog.setSingleChoiceItems(item_str, pos, mSpinnerListener);
            mSpinnerDialog.showWithDialog();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {

        mLabel = (View) findViewById(R.id.spinner);
        // Turn off the Spinner's own state management. We do this ourselves on rotation
        if (mLabel != null) {
            mLabel.setId(View.NO_ID);
            mLabelText = (TextView) findViewById(R.id.spinner_textview);
            mLabelIcon = (ImageView) findViewById(R.id.spinner_icon);
            //mLabel.setOnItemSelectedListener(mSpinnerListener);
            mLabel.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // popup dialog from bottom if it has been created
                    showSpinnerDialog(v);
                }
            });
        }

        //mKindTypeIcon = (ImageView) findViewById(R.id.kind_section_type_icon);
        mDelete = (ImageView) findViewById(R.id.delete_button);
        //mDeleteContainer = findViewById(R.id.delete_button_container);
        if (mDelete != null) {
            mDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // defer removal of this button so that the pressed state is visible shortly
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {

                            if(!mIsDeletable) {
                                return;
                            }
                            // Don't do anything if the view is no longer attached to the window
                            // (This check is needed because when this {@link Runnable} is executed,
                            // we can't guarantee the view is still valid.
                            if (!mIsAttachedToWindow) {
                                return;
                            }
                            // Send the delete request to the listener (which will in turn call
                            // deleteEditor() on this view if the deletion is valid - i.e. this is not
                            // the last {@link Editor} in the section).
                            if (mListener != null) {
                                sendReport(false);
                                mListener.onDeleteRequested(LabeledEditorView.this);
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Keep track of when the view is attached or detached from the window, so we know it's
        // safe to remove views (in case the user requests to delete this editor).
        mIsAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachedToWindow = false;
        if (mSpinnerDialog != null) {
            mSpinnerDialog.dismiss();
            mSpinnerDialog = null;
        }
        if (mCustomLabelDialog != null) {
            mCustomLabelDialog.dismiss();
            mCustomLabelDialog = null;
        }
    }

    @Override
    public void deleteEditor() {
        // Keep around in model, but mark as deleted
        mEntry.markDeleted();

        // Remove the view
        EditorAnimator.getInstance().removeEditorView(this);
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    public int getBaseline(int row) {
        if (row == 0 && mLabel != null) {
            return mLabel.getBaseline();
        }
        return -1;
    }

    /**
     * Configures the visibility of the type label button and enables or disables it properly.
     */
    /*private void setupLabelButton(boolean shouldExist) {
        if (shouldExist) {
            mLabel.setEnabled(!mReadOnly && isEnabled());
            mLabel.setVisibility(View.VISIBLE);
        } else {
            mLabel.setVisibility(View.GONE);
        }
    }
*/
    /**
     * Configures the visibility of the "delete" button and enables or disables it properly.
     */
    private void setupDeleteButton() {
        //mDeleteContainer.setVisibility(View.VISIBLE);
        if(mDelete == null) {
            return;
        }
        if (mIsDeletable) {
            mDelete.setVisibility(View.VISIBLE);
            mDelete.setEnabled(!mReadOnly && isEnabled());
        } else {
            mDelete.setVisibility(View.GONE);
        }
    }

    public void setDeleteButtonVisible(boolean visible) {
        if (mIsDeletable) {
            //mDeleteContainer.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            mDelete.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    //added by xiaodong.lxd
//    public void setKindTypeImageVisible(boolean visible, String mimeType) {
////        Log.d(TAG, "sxsexe----> setKindTypeImageVisible mimeType " + mimeType);
//        mKindTypeIcon.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
//        if(Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_call);
//        } else if(StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_address);
//        } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_message);
//        } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_work);
//        }  else if(Im.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_chat);
//        } else if (Note.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_remark);
//        } else if (Website.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_web);
//        } else if (Event.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_birthday);
//        } else if (Sns.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_community);
//        }/* else if (Event.CONTENT_ITEM_TYPE.equals(mimeType)) {
//            mKindTypeIcon.setBackgroundResource(R.drawable.ic_list_birthday);
//        }*/ else {
//            mKindTypeIcon.setVisibility(View.GONE);
//        }
//    }

    protected void onOptionalFieldVisibilityChange() {
        if (mListener != null) {
            mListener.onRequest(EditorListener.EDITOR_FORM_CHANGED);
        }
    }

    @Override
    public void setEditorListener(EditorListener listener) {
        mListener = listener;
    }

    @Override
    public void setDeletable(boolean deletable) {
        mIsDeletable = deletable;
        setupDeleteButton();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mLabel != null) {
            mLabel.setEnabled(!mReadOnly && enabled);
        }
        if (mDelete != null) {
            mDelete.setEnabled(!mReadOnly && enabled);
        }
    }

    /*public Spinner getLabel() {
        return mLabel;
    }*/

    public ImageView getDelete() {
        return mDelete;
    }

    protected DataKind getKind() {
        return mKind;
    }

    protected ValuesDelta getEntry() {
        return mEntry;
    }

    protected EditType getType() {
        return mType;
    }

    /**
     * Build the current label state based on selected {@link EditType} and
     * possible custom label string.
     */
    private void rebuildLabel() {
        Log.i(TAG, "rebuildLabel() ; create adpater");
        // create new adpater when rebuild label, for possible new custom type
        //mEditTypeAdapter = new EditTypeAdapter(mContext);
        //mLabel.setAdapter(mEditTypeAdapter);
        if(mEditTypeManager == null)
            mEditTypeManager = new EditTypeManager();
        mEditTypeManager.refreshList();

        CharSequence[] str_items= mEditTypeManager.getTypeCharSequence();
        /*if(str_items == null) {
            // no edit type
        } else if(str_items.length == 1) {
            // only have one type, such as memo. event, orgnaize
        } else {*/
        if(str_items != null && str_items.length != 1) {
            // normal case : set spinner dialog and listener

            if (mSpinnerDialog != null) {
                mSpinnerDialog.dismiss();
                mSpinnerDialog = null;
            }
            mStr_items = str_items;
            Log.i(TAG, "rebuildLabel() -- have builder dialog : " + mSpinnerDialog + ", mType = " + mType);
            if(mType == null) {
            // maybe null when sync from cloud or form vcf file???
            return;
            }

            if(mEditTypeManager.hasCustomSelection()) {
                mLabelText.setText(mEntry.getAsString(mType.customColumn));
            } else {
                mLabelText.setText(mContext.getString(mType.labelRes));
            }
        }

    }

    private class EditTypeManager {
        private boolean mHasCustomSelection;
        private ArrayList<CharSequence> mTypeStringList = new ArrayList<CharSequence>();
        private ArrayList<EditType> mEditTypeList = new ArrayList<EditType>();

        public EditTypeManager() {
            //rebuildTypeList();
        }

        private void rebuildTypeList() {
            mEditTypeList.clear();
            mTypeStringList.clear();
            mHasCustomSelection = false;

            if (mType != null && mType.customColumn != null) {
                // Use custom label string when present
                final String customText = mEntry.getAsString(mType.customColumn);
                if (customText != null) {
                    mEditTypeList.add(CUSTOM_SELECTION);
                    mTypeStringList.add(mEntry.getAsString(mType.customColumn));
                    mHasCustomSelection = true;
                }
            }

            ArrayList<EditType> validList = RawContactModifier.getValidTypes(mState, mKind, mType);
            mEditTypeList.addAll(validList);

            // get string list from EditType list
            for (EditType editType : validList) {
                mTypeStringList.add(getContext().getString(editType.labelRes));
            }

        }


        public boolean hasCustomSelection() {
            return mHasCustomSelection;
        }

        public void refreshList() {
            rebuildTypeList();
        }

        public EditType getEditTypeAtPos(int pos) {
            if(mEditTypeList != null && mEditTypeList.size() >= pos) {
                return mEditTypeList.get(pos);
            }

            return null;
        }

        public CharSequence[] getTypeCharSequence() {
            if(mTypeStringList != null) {
                CharSequence[] seqence = new CharSequence[mTypeStringList.size()];
                return mTypeStringList.toArray(seqence);
            }

            return null;
        }
    }


    @Override
    public void onFieldChanged(String column, String value) {
        if (!isFieldChanged(column, value)) {
            return;
        }

        // Field changes are saved directly
        saveValue(column, value);

        // Notify listener if applicable
        notifyEditorListener();
    }

    protected void saveValue(String column, String value) {
        mEntry.put(column, value);
        sendReport(true);
    }

    /**
     * if isEditing is true, means we're editing some one item, or if it is
     * false, means we're deleting some one item.
     */
    private void sendReport(boolean isEditing) {
        if (mEntry == null) {
            Log.e(TAG, "sendReport isEditing:" + isEditing + ", mEntry is NULL!!!");
            return;
        }
        final String mimeType = mEntry.getAsString(Data.MIMETYPE);
        if (mPreMineType != null && mPreMineType.equals(mimeType) && isEditing) {
            return;
        }
        mPreMineType = mimeType;

        String message = null;
        if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = UsageReporter.ContactsEditPage.EDITOR_NAME_INPUT;
        } else if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_CALL_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_CALL_INPUT;
        } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_EMAIL_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_EMAIL_INPUT;
        } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_ADDRESS_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_ADDRESS_INPUT;
        } else if (Im.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_IM_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_IM_INPUT;
        } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_ORGANIZATION_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_ORGANIZATION_INPUT;
        } else if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_NICKNAME_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_NICKNAME_INPUT;
        } else if (Note.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_NOTE_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_NOTE_INPUT;
        } else if (Website.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_WEB_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_WEB_INPUT;
        } else if (Sns.CONTENT_ITEM_TYPE.equals(mimeType)) {
            message = isEditing ? UsageReporter.ContactsEditPage.EDITOR_SNS_INPUT :
                    UsageReporter.ContactsEditPage.DELETE_SNS_INPUT;
        } else {
            return;
        }

        UsageReporter.onClick(ContactEditorActivity.class, message);
    }

    protected void notifyEditorListener() {
        if (mListener != null) {
            mListener.onRequest(EditorListener.FIELD_CHANGED);
        }

        boolean isEmpty = isEmpty();
        if (mWasEmpty != isEmpty) {
            if (isEmpty) {
                if (mListener != null) {
                    mListener.onRequest(EditorListener.FIELD_TURNED_EMPTY);
                }
                if (mIsDeletable) mDelete.setVisibility(View.GONE);
            } else {
                if (mListener != null) {
                    mListener.onRequest(EditorListener.FIELD_TURNED_NON_EMPTY);
                }
                if (mIsDeletable) mDelete.setVisibility(View.VISIBLE);
            }
            mWasEmpty = isEmpty;
        }
    }

    protected boolean isFieldChanged(String column, String value) {
        final String dbValue = mEntry.getAsString(column);
        // nullable fields (e.g. Middle Name) are usually represented as empty columns,
        // so lets treat null and empty space equivalently here
        final String dbValueNoNull = dbValue == null ? "" : dbValue;
        final String valueNoNull = value == null ? "" : value;
        return !TextUtils.equals(dbValueNoNull, valueNoNull);
    }

    protected void rebuildValues() {
        setValues(mKind, mEntry, mState, mReadOnly, mViewIdGenerator);
    }

    /**
     * Prepare this editor using the given {@link DataKind} for defining
     * structure and {@link ValuesDelta} describing the content to edit.
     */
    @Override
    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly,
            ViewIdGenerator vig) {
//        Log.d(TAG, "sxsexe----> setValues ");
        mKind = kind;
        mEntry = entry;
        mState = state;
        mReadOnly = readOnly;
        mViewIdGenerator = vig;
        setId(vig.getId(state, kind, entry, ViewIdGenerator.NO_VIEW_INDEX));

        if (!entry.isVisible()) {
            // Hide ourselves entirely if deleted
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        // Display label selector if multiple types available
        final boolean hasTypes = RawContactModifier.hasEditTypes(kind);
        //setupLabelButton(hasTypes);
        //mLabel.setEnabled(!readOnly && isEnabled());
        if (hasTypes) {
            mLabel.setEnabled(!mReadOnly && isEnabled());
            mLabel.setVisibility(View.VISIBLE);
            mLabelText.setTextColor(mContext.getResources().getColor(R.color.aui_bg_color_dark));
            mType = RawContactModifier.getCurrentType(entry, kind);
            rebuildLabel();
        } else if(mLabel != null) {
            //mLabel.setEnabled(false);
            if( mKind.mimeType.equals(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME)) {
                mLabel.setVisibility(View.GONE);
            } else {
                mLabelText.setText(mKind.titleRes);
                mLabelIcon.setVisibility(View.GONE);
                mLabel.setVisibility(View.VISIBLE);
            }
        }
    }

    public ValuesDelta getValues() {
        return mEntry;
    }

    /**
     * Prepare dialog for entering a custom label. The input value is trimmed: white spaces before
     * and after the input text is removed.
     * <p>
     * If the final value is empty, this change request is ignored;
     * no empty text is allowed in any custom label.
     */
    private Dialog createCustomDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final LayoutInflater layoutInflater = LayoutInflater.from(builder.getContext());
        builder.setTitle(R.string.customLabelPickerTitle);

        final View view = layoutInflater.inflate(R.layout.contact_editor_label_name_dialog, null);
        final EditText editText = (EditText) view.findViewById(R.id.custom_dialog_content);
        editText.setInputType(INPUT_TYPE_CUSTOM);
        editText.setSaveEnabled(true);

        builder.setView(view);
        editText.requestFocus();

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String customText = editText.getText().toString().trim();
                if (ContactsUtils.isGraphic(customText)) {
                    final List<EditType> allTypes =
                            RawContactModifier.getValidTypes(mState, mKind, null);
                    mType = null;
                    for (EditType editType : allTypes) {
                        if (editType.customColumn != null) {
                            mType = editType;
                            break;
                        }
                    }
                    if (mType == null) return;

                    mEntry.put(mKind.typeColumn, mType.rawValue);
                    mEntry.put(mType.customColumn, customText);
                    rebuildLabel();
                    requestFocusForFirstEditField();
                    onLabelRebuilt();
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                updateCustomDialogOkButtonState(dialog, editText);
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCustomDialogOkButtonState(dialog, editText);
            }
        });
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return dialog;
    }

    /* package */ void updateCustomDialogOkButtonState(AlertDialog dialog, EditText editText) {
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okButton.setEnabled(!TextUtils.isEmpty(editText.getText().toString().trim()));
    }

    /**
     * Called after the label has changed (either chosen from the list or entered in the Dialog)
     */
    protected void onLabelRebuilt() {
    }

    protected void onTypeSelectionChange(int position) {
        EditType selected = mEditTypeManager.getEditTypeAtPos(position);
        // See if the selection has in fact changed
        if (mEditTypeManager.hasCustomSelection() && selected == CUSTOM_SELECTION) {
            return;
        }

        if (mType == selected && mType.customColumn == null) {
            return;
        }

        if (selected.customColumn != null) {
            //showDialog(DIALOG_ID_CUSTOM);
            showCustomLabelDialog();
        } else {
            // User picked type, and we're sure it's ok to actually write the entry.
            mType = selected;
            mEntry.put(mKind.typeColumn, mType.rawValue);
            rebuildLabel();
            requestFocusForFirstEditField();
            onLabelRebuilt();
        }
    }

    private void showCustomLabelDialog() {
        if (mCustomLabelDialog == null) {
            final hwdroid.dialog.AlertDialog.Builder builder = new hwdroid.dialog.AlertDialog.Builder(mContext);
            final LayoutInflater layoutInflater = LayoutInflater.from(builder.getContext());
            builder.setTitle(R.string.customLabelPickerTitle);

            final View view = layoutInflater.inflate(R.layout.contact_editor_label_name_dialog, null);
            mCustomLabelEditText = (EditText) view.findViewById(R.id.custom_dialog_content);
            mCustomLabelEditText.setInputType(INPUT_TYPE_CUSTOM);
            mCustomLabelEditText.setSaveEnabled(true);

            builder.setView(view);

            builder.setPositiveButton(android.R.string.ok, new hwdroid.dialog.DialogInterface.OnClickListener() {

                @Override
                public void onClick(hwdroid.dialog.DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    final String customText = mCustomLabelEditText.getText().toString().trim();
                    if (ContactsUtils.isGraphic(customText)) {
                        final List<EditType> allTypes = RawContactModifier.getValidTypes(mState, mKind, null);
                        mType = null;
                        for (EditType editType : allTypes) {
                            if (editType.customColumn != null) {
                                mType = editType;
                                break;
                            }
                        }
                        if (mType == null)
                            return;
                        mEntry.put(mKind.typeColumn, mType.rawValue);
                        mEntry.put(mType.customColumn, customText);
                        rebuildLabel();
                        requestFocusForFirstEditField();
                        onLabelRebuilt();
                    } else {
                        rebuildLabel();
                    }
                    dialog.dismiss();
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new hwdroid.dialog.DialogInterface.OnClickListener() {
                @Override
                public void onClick(hwdroid.dialog.DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                    rebuildLabel();
                }
            });
            mCustomLabelDialog = builder.create();
            mCustomLabelDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        mCustomLabelEditText.setText("");
        mCustomLabelEditText.requestFocus();
        mCustomLabelDialog.show();
    }

    /* package */
    void showDialog(int bundleDialogId) {
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_ID_KEY, bundleDialogId);
        getDialogManager().showDialogInView(this, bundle);
    }

    private DialogManager getDialogManager() {
        if (mDialogManager == null) {
            Context context = getContext();
            if (!(context instanceof DialogManager.DialogShowingViewActivity)) {
                throw new IllegalStateException(
                        "View must be hosted in an Activity that implements " +
                        "DialogManager.DialogShowingViewActivity");
            }
            mDialogManager = ((DialogManager.DialogShowingViewActivity)context).getDialogManager();
        }
        return mDialogManager;
    }

    @Override
    public Dialog createDialog(Bundle bundle) {
        if (bundle == null) throw new IllegalArgumentException("bundle must not be null");
        int dialogId = bundle.getInt(DIALOG_ID_KEY);
        switch (dialogId) {
            case DIALOG_ID_CUSTOM:
                return createCustomDialog();
            default:
                throw new IllegalArgumentException("Invalid dialogId: " + dialogId);
        }
    }

    protected abstract void requestFocusForFirstEditField();
    /*
    private class EditTypeAdapter extends ArrayAdapter<EditType> {
        private final LayoutInflater mInflater;
        private boolean mHasCustomSelection;
        private int mTextColor;

        public EditTypeAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (mType != null && mType.customColumn != null) {

                // Use custom label string when present
                final String customText = mEntry.getAsString(mType.customColumn);
                if (customText != null) {
                    add(CUSTOM_SELECTION);
                    mHasCustomSelection = true;
                }
            }

            addAll(RawContactModifier.getValidTypes(mState, mKind, mType));

            mTextColor = context.getResources().getColor(R.color.aui_secondary_txt_color_grey);
        }

        public boolean hasCustomSelection() {
            return mHasCustomSelection;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(
                    position, convertView, parent, android.R.layout.simple_spinner_item);
                    //position, convertView, parent, R.layout.contact_spinner_dropdown_item);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(
                    //position, convertView, parent, R.layout.contact_spinner_dropdown_item);
                    position, convertView, parent, R.layout.contact_spinner_dropdown_item);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent,
                int resource) {
            TextView textView;

            if (convertView == null) {
                textView = (TextView) mInflater.inflate(resource, parent, false);
                if(resource == android.R.layout.simple_spinner_item) {
                    textView.setAllCaps(true);
                    textView.setGravity(Gravity.CENTER);
                    //textView.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
                    textView.setPadding(10, 10, 10, 10);
                    textView.setTextColor(mTextColor);
                    textView.setEllipsize(TruncateAt.END);
                }
            } else {
                textView = (TextView) convertView;
            }

            EditType type = getItem(position);
            String text;
            if (type == CUSTOM_SELECTION) {
                text = mEntry.getAsString(mType.customColumn);
            } else {
                text = getContext().getString(type.labelRes);
            }
            textView.setText(text);
            return textView;
        }
    }*/

    public void setWasEmpty(boolean wasEmpty) {
        mWasEmpty = wasEmpty;
    }
}
