
package com.yunos.alicontacts.sim;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.activities.ContactDetailActivity;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import yunos.support.v4.app.Fragment;

public class SimContactEditorFragment extends Fragment {
    private static final String TAG = "SimContactEditorFragment";

    public static final String INTENT_EXTRA_PHONE_NUMBER = "extra_phone_number";
    public static final String INTENT_EXTRA_EMAIL = "extra_email";
    public static final String INTENT_EXTRA_NOT_VIEW_DETAIL_ON_SAVE_COMPLETED = "not_view_detail_on_save";

    private static final int SAVE_SIM_CONTACTS_SUCCESS = 0;
    private static final int SAVE_SIM_CONTACTS_FAIL = 1;

    private BaseActivity mActivity;
    private Context mContext;
    private String mAction;
    private int mSlot;
    private int mIndexInSim;
    private boolean mIs2gSim;
    private long mRawContactId;

    private EditText mNameView;
    private EditText mNumberView;
    private EditText mAnrView;
    private EditText mEmailView;

    private String mName;
    private String mNumber;
    private String mAnr;
    private String mEmail;

    private ImageView mSaveView;
    private AlertDialog mDropConfirmDialog;
    private boolean mNotViewDetailOnSaveCompleted;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (BaseActivity) activity;
        mContext = mActivity.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = mActivity.getIntent();

        mAction = intent.getAction();
        mRawContactId = intent.getLongExtra(Data.RAW_CONTACT_ID, -1);
        if (SimUtil.MULTISIM_ENABLE) {
            mSlot = intent.getIntExtra(SimUtil.SLOT_KEY, SimUtil.SLOT_ID_1);
            mIs2gSim = SimUtil.is2gSim(mSlot);
        } else {
            mIs2gSim = SimUtil.is2gSim();
        }

        if (SimContactUtils.ACTION_EDITOR_SIM_CONTACTS.equals(mAction)) {
            mIndexInSim = intent.getIntExtra(SimUtil.SIM_INDEX, -1);
            mName = intent.getStringExtra(SimUtil.SIM_NAME);

            if (!mIs2gSim) {
                mAnr = intent.getStringExtra(SimUtil.SIM_ANR);
                mEmail = intent.getStringExtra(SimUtil.SIM_EMAILS);
            }
        }

        mNumber = intent.getStringExtra(SimUtil.SIM_NUMBER);
        mNotViewDetailOnSaveCompleted = intent.getBooleanExtra(
                INTENT_EXTRA_NOT_VIEW_DETAIL_ON_SAVE_COMPLETED, false);

        initActionBarView();

        Log.d(TAG, "onCreate() intent:" + intent + ", mSubscription:" + mSlot
                + ", mIs2gSim:" + mIs2gSim);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sim_contact_editor, container, false);

        final ImageView photoView = (ImageView) view.findViewById(R.id.photo);
        if (photoView != null) {
            if (SimUtil.MULTISIM_ENABLE) {
                photoView
                        .setImageResource((mSlot == SimUtil.SLOT_ID_1) ? R.drawable.contact_detail_avatar_border_acquiesce_card1
                                : R.drawable.contact_detail_avatar_border_acquiesce_card2);
            } else {
                photoView.setImageResource(R.drawable.contact_detail_avatar_border_acquiesce_sim);
            }
        }

        mNameView = (EditText) view.findViewById(R.id.name_content);
        mNameView.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(SimContactUtils.SIM_NAME_MAX_EN_LENGTH)
        });

        if (ContactsUtils.isGraphic(mName)) {
            mNameView.setText(mName);
            mNameView.setSelection(mName.length());
        }

        final View numberContainerView = view.findViewById(R.id.number_container);
        mNumberView = (EditText) numberContainerView.findViewById(R.id.content);
        mNumberView.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(SimContactUtils.SIM_NUMBER_MAX_LENGTH)
        });
        if (!TextUtils.isEmpty(mNumber)) {
            mNumberView.setText(mNumber);
        }

        if (!mIs2gSim) {
            final View usimContainerView = view.findViewById(R.id.usim_container);
            if (usimContainerView != null) {
                usimContainerView.setVisibility(View.VISIBLE);
                final View anrContainerView = usimContainerView.findViewById(R.id.anr_container);
                TextView phoneType = (TextView) usimContainerView.findViewById(R.id.type_name);
                phoneType.setText(R.string.imTypeHome);
                mAnrView = (EditText) anrContainerView.findViewById(R.id.content);
                mAnrView.setFilters(new InputFilter[] {
                        new InputFilter.LengthFilter(SimContactUtils.SIM_NUMBER_MAX_LENGTH)
                });

                if (!TextUtils.isEmpty(mAnr)) {
                    mAnrView.setText(mAnr);
                }

                final View emailContainerView = usimContainerView
                        .findViewById(R.id.email_container);
                final TextView typeView = (TextView) emailContainerView
                        .findViewById(R.id.type_name);
                typeView.setText(R.string.email);
                mEmailView = (EditText) emailContainerView.findViewById(R.id.content);
                mEmailView.setHint(R.string.email);
                mEmailView.setInputType(EditorInfo.TYPE_CLASS_TEXT
                        | EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                mEmailView.setFilters(new InputFilter[] {
                        new InputFilter.LengthFilter(SimContactUtils.SIM_EMAIL_MAX_LENGTH)
                });
                if (!TextUtils.isEmpty(mEmail)) {
                    mEmailView.setText(mEmail);
                }

            }
        }

        fillExtraData();
        return view;
    }

    private void fillExtraData() {
        Intent intent = mActivity == null ? null : mActivity.getIntent();
        if (intent == null) {
            Log.w(TAG, "fillExtraData: null intent, skip.");
            return;
        }
        String extraPhoneNumber = intent.getStringExtra(INTENT_EXTRA_PHONE_NUMBER);
        if (!TextUtils.isEmpty(extraPhoneNumber)) {
            if (TextUtils.isEmpty(mNumber)) {
                mNumberView.setText(extraPhoneNumber);
            } else if ((!mIs2gSim) && TextUtils.isEmpty(mAnr)) {
                mAnrView.setText(extraPhoneNumber);
            } else {
                Toast.makeText(mContext,
                        getString(R.string.sim_contact_item_full_no_space_for_number, extraPhoneNumber),
                        Toast.LENGTH_SHORT).show();
            }
        }
        String extraEmail = intent.getStringExtra(INTENT_EXTRA_EMAIL);
        if (!TextUtils.isEmpty(extraEmail)) {
            if ((!mIs2gSim) && TextUtils.isEmpty(mEmail)) {
                mEmailView.setText(extraEmail);
            } else {
                Toast.makeText(mContext,
                        getString(R.string.sim_contact_item_full_no_space_for_email, extraEmail),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDropConfirmDialog != null) {
            mDropConfirmDialog.dismiss();
            mDropConfirmDialog = null;
        }
    }

    private void initActionBarView() {
        int titleRes;
        if (SimContactUtils.ACTION_EDITOR_SIM_CONTACTS.equals(mAction)) {
            titleRes = R.string.edit_sim_contact;
        } else {
            titleRes = R.string.create_sim_contact;
        }
        ActionBar actionBar = mActivity.getActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(R.layout.contacts_actionbar_cancel_done);
            actionBar.setDisplayShowCustomEnabled(true);
            ImageView cancelView = (ImageView) mActivity.findViewById(R.id.cancel);
            mSaveView = (ImageView) mActivity.findViewById(R.id.save);
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v == null) {
                        return;
                    }
                    onHandleFooterBarItemClick(v.getId());
                }
            };
            cancelView.setOnClickListener(listener);
            mSaveView.setOnClickListener(listener);

            TextView titleView = (TextView) mActivity.findViewById(R.id.title);
            titleView.setText(titleRes);
        }
    }

    public boolean onHandleFooterBarItemClick(int id) {
        switch (id) {
            case R.id.cancel:
                onCancel();
                break;
            case R.id.save:
/*YunOS BEGIN PB*/
//##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
//##BugID:(8352838) ##date:2016-2-2 09:00
//##description:show toast to customer when card is out.
            	if(!SimUtil.isCardEnable(mSlot)){
            		Toast.makeText(mContext, R.string.sim_error_not_ready, Toast.LENGTH_SHORT).show();
            	}else{
            		onSave();
            	}
/*YUNOS END PB*/
                break;
            default:
                break;
        }
        return true;
    }

    public void onCancel() {
        if (mActivity == null) {
            Log.e(TAG, "onCancel() mActivity or mFooterBarButton is NULL!!!");
            return;
        }

        if (checkSimContentChanged()) {
            AlertDialog.Builder build = new AlertDialog.Builder(mActivity);
            build.setTitle(mContext.getString(R.string.drop_edit_change));
            build.setMessage(mContext.getString(R.string.confirm_drop_edit_change));
            build.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mActivity.setResult(Activity.RESULT_CANCELED);
                    mActivity.finish();
                }
            });
            build.setNegativeButton(R.string.no, null);
            mDropConfirmDialog = build.create();
            mDropConfirmDialog.show();
        } else {
            mActivity.setResult(Activity.RESULT_CANCELED);
            mActivity.finish();
        }

    }

    private boolean checkSimContentChanged() {
        if (mNameView != null) {
            String newName = mNameView.getText().toString();
            if ((mName == null && ContactsUtils.isGraphic(newName))
                    || (mName != null && !mName.equals(newName))) {
                return true;
            }
        }

        if (mNumberView != null) {
            String newNumber = mNumberView.getText().toString();
            if ((mNumber == null && !TextUtils.isEmpty(newNumber))
                    || (mNumber != null && !mNumber.equals(newNumber))) {
                return true;
            }
        }

        if (!mIs2gSim) {
            if (mAnrView != null) {
                String newAnr = mAnrView.getText().toString();
                if ((mAnr == null && !TextUtils.isEmpty(newAnr))
                        || (mAnr != null && !mAnr.equals(newAnr))) {
                    return true;
                }
            }

            if (mEmailView != null) {
                String newEmail = mEmailView.getText().toString();
                if ((mEmail == null && !TextUtils.isEmpty(newEmail))
                        || (mEmail != null && !mEmail.equals(newEmail))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void onSave() {
        if (!checkSimContentValues()) {
            return;
        }

        setSaveButtonEnable(false);

        ContentValues values = createSimContentValues();
        new SaveSimThread(values).start();
    }

    private void setSaveButtonEnable(boolean enabled) {
        if (mSaveView != null) {
            mSaveView.setEnabled(enabled);
        }
    }

    private boolean checkSimContentValues() {
        if (mNameView != null && !ContactsUtils.isGraphic(mNameView.getText())) {
            Toast.makeText(mContext, R.string.contact_save_name_empty, Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        /* YUNOS BEGIN PB */
        //##email:caixiang.zcx@alibaba-inc.com
        //##BugID:(8200701) ##date:2016/05/13
        //##description:limit the length of sim contact name on qcom plateform
        if (SimUtil.IS_PLATFORM_QCOMM) {
            String name = mNameView.getText().toString().trim();
            if (SimContactUtils.hasMultiByteChar(name) && name.length() > SimContactUtils.SIM_NAME_MAX_CN_LENGTH) {
                Toast.makeText(mContext, R.string.sim_error_name_too_long, Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
        }
        /* YUNOS END PB */

        boolean isNumberEmpty = true;
        if (mNumberView != null && !TextUtils.isEmpty(mNumberView.getText())) {
            isNumberEmpty = false;
        }

        if (!mIs2gSim) {
            if (isNumberEmpty && mAnrView != null && !TextUtils.isEmpty(mAnrView.getText())) {
                isNumberEmpty = false;
            }

            if (mEmailView != null) {
                String email = mEmailView.getText().toString();
                if (!TextUtils.isEmpty(email)) {
                    if (!SimContactUtils.isEmailValid(email)) {
                        Toast.makeText(mContext, R.string.email_invalid, Toast.LENGTH_SHORT)
                                .show();
                        return false;
                    }

                    return true;
                }
            }
        }

        if (isNumberEmpty) {
            Toast.makeText(mContext, R.string.sim_contact_number_empty, Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        return true;
    }

    private ContentValues createSimContentValues() {
        ContentValues values = new ContentValues();

        if (SimContactUtils.ACTION_INSERT_SIM_CONTACTS.equals(mAction)) {
            if (mNameView != null) {
                String name = mNameView.getText().toString();
                values.put(SimUtil.SIM_NAME, name);
            }

            if (mNumberView != null) {
                String number = mNumberView.getText().toString();
                values.put(SimUtil.SIM_NUMBER, PhoneNumberUtils.stripSeparators(number));
            }

            if (!mIs2gSim) {
                if (mAnrView != null) {
                    String anrs = mAnrView.getText().toString();
                    values.put(SimUtil.SIM_ANR, PhoneNumberUtils.stripSeparators(anrs));
                }

                if (mEmailView != null) {
                    String emails = mEmailView.getText().toString();
                    values.put(SimUtil.SIM_EMAILS, emails);
                }
            }

        } else if (SimContactUtils.ACTION_EDITOR_SIM_CONTACTS.equals(mAction)) {
            if (mNameView != null) {
                String name = mNameView.getText().toString();
                values.put(SimUtil.SIM_NEW_NAME, name);
                values.put(SimUtil.SIM_NAME, mName);
            }

            if (mNumberView != null) {
                String number = mNumberView.getText().toString();
                values.put(SimUtil.SIM_NEW_NUMBER, PhoneNumberUtils.stripSeparators(number));
                values.put(SimUtil.SIM_NUMBER, mNumber);
            }

            if (!mIs2gSim) {
                if (mAnrView != null) {
                    String anrs = mAnrView.getText().toString();
                    values.put(SimUtil.SIM_NEW_ANR, PhoneNumberUtils.stripSeparators(anrs));
                    values.put(SimUtil.SIM_ANR, mAnr);
                }

                if (mEmailView != null) {
                    String emails = mEmailView.getText().toString();
                    values.put(SimUtil.SIM_NEW_EMAILS, emails);
                    values.put(SimUtil.SIM_EMAILS, mEmail);
                }
            }

            if (SimUtil.IS_PLATFORM_MTK) {
                values.put(SimUtil.SIM_INDEX, mIndexInSim);
            }
        }

        Log.d(TAG, "createSimContentValues() values size:" + values.size());

        return values;
    }

    private class SaveSimThread extends Thread {
        private ContentValues mValues;

        public SaveSimThread(ContentValues values) {
            mValues = values;
        }

        @Override
        public void run() {

            try {
                if (SimContactUtils.ACTION_INSERT_SIM_CONTACTS.equals(mAction)) {
                    final Uri resultUri;
                    if (SimUtil.MULTISIM_ENABLE) {
                        resultUri = SimUtil.insert(mContext, mSlot, mValues);
                    } else {
                        resultUri = SimUtil.insert(mContext, mValues);
                    }

                    if (resultUri == null) {
                        Log.d(TAG, "[SaveSimThread] resultUri is NULL!!!");
                        /* YUNOS BEGIN PB */
                        //##email:caixiang.zcx@alibaba-inc.com
                        //##BugID:(8206307) ##date:2016/05/13
                        //##description:show message when sim storage full on qcom plateform
                        if (SimUtil.IS_PLATFORM_QCOMM
                                && (SimContactUtils.getSimContactsCount(mContext, mSlot) >= SimUtil.getMSimCardMaxCount(mSlot))) {
                            Message msg = mHandler.obtainMessage(SAVE_SIM_CONTACTS_FAIL);
                            msg.arg1 = SimUtil.ERROR_SIM_STORAGE_FULL;
                            mHandler.sendMessage(msg);
                            return;
                        }
                        /* YUNOS END PB */
                        Message msg = mHandler.obtainMessage(SAVE_SIM_CONTACTS_FAIL);
                        msg.arg1 = SimUtil.ERROR_SIM_GENERIC_FAILURE;
                        mHandler.sendMessage(msg);
                        return;
                    }

                    int errorPosition = resultUri.toString().indexOf(
                            SimContactUtils.SAVE_SIM_CONTACTS_ERROR);
                    Log.d(TAG, "[SaveSimThread] resultUri:" + resultUri);
                    if (errorPosition == -1) {
                        if (SimUtil.IS_YUNOS) {
                            insertToPhoneDb(mValues, resultUri);
                        }

                        mHandler.sendEmptyMessage(SAVE_SIM_CONTACTS_SUCCESS);
                        mActivity.setResult(Activity.RESULT_OK);
                    } else {
                        Message msg = mHandler.obtainMessage(SAVE_SIM_CONTACTS_FAIL);
                        String error = resultUri.getLastPathSegment();
                        int errorType = SimUtil.ERROR_SIM_GENERIC_FAILURE;

                        try {
                            errorType = Integer.valueOf(error);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "[SaveSimThread] NumberFormatException:", e);
                        } finally {
                            msg.arg1 = errorType;
                            mHandler.sendMessage(msg);
                        }
                    }

                } else if (SimContactUtils.ACTION_EDITOR_SIM_CONTACTS.equals(mAction)) {
                    final int result;
                    if (SimUtil.MULTISIM_ENABLE) {
                        result = SimUtil.update(mContext, mSlot, mValues);
                    } else {
                        result = SimUtil.update(mContext, mValues);
                    }

                    Log.d(TAG, "[SaveSimThread] result:" + result);
                    if (result > 0) {
                        if (SimUtil.IS_YUNOS) {
                            updatePhoneDb(mValues);
                        }

                        mHandler.sendEmptyMessage(SAVE_SIM_CONTACTS_SUCCESS);
                    } else {
                        Message msg = mHandler.obtainMessage(SAVE_SIM_CONTACTS_FAIL);
                        msg.arg1 = result;
                        mHandler.sendMessage(msg);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "[SaveSimThread]", e);
            }
        }

        private void insertToPhoneDb(ContentValues values, Uri simResultUri) {
            String name = values.getAsString(SimUtil.SIM_NAME);
            String phoneNumber = values.getAsString(SimUtil.SIM_NUMBER);
            String anr = values.getAsString(SimUtil.SIM_ANR);
            String email = values.getAsString(SimUtil.SIM_EMAILS);
            int simIndex = (int) ContentUris.parseId(simResultUri);
            long rawContactId = SimContactUtils.insertSimContactToPhoneDb(
                    mContext, name, phoneNumber, anr, email, mSlot);
            boolean cacheUpdated = false;
            if (rawContactId > 0) {
                viewSavedContact(rawContactId);
                cacheUpdated = SimContactCache.insertSimContact(
                        rawContactId, mSlot, simIndex, name, phoneNumber, anr, email);
            }
            if (!cacheUpdated) {
                Intent intent = new Intent(SimContactLoadService.ACTION_LOAD_SIM_CONTACTS_TO_PHONE_DB);
                intent.setClass(mActivity.getApplicationContext(), SimContactLoadService.class);
                intent.putExtra(SimContactLoadService.INTENT_KEY_SLOT_ID, mSlot);
                mActivity.startService(intent);
            }
        }

        private void updatePhoneDb(ContentValues values) {
            String name = values.getAsString(SimUtil.SIM_NAME);
            String phoneNumber = values.getAsString(SimUtil.SIM_NUMBER);
            String anr = values.getAsString(SimUtil.SIM_ANR);
            String email = values.getAsString(SimUtil.SIM_EMAILS);

            ContentValues before = new ContentValues();
            ContentValues after = new ContentValues();
            before.put(SimUtil.SIM_NAME, name);
            before.put(SimUtil.SIM_NUMBER, phoneNumber);
            before.put(SimUtil.SIM_ANR, anr);
            before.put(SimUtil.SIM_EMAILS, email);

            String newName = values.getAsString(SimUtil.SIM_NEW_NAME);
            String newPhoneNumber = values.getAsString(SimUtil.SIM_NEW_NUMBER);
            String newAnr = values.getAsString(SimUtil.SIM_NEW_ANR);
            String newEmail = values.getAsString(SimUtil.SIM_NEW_EMAILS);
            after.put(SimUtil.SIM_NAME, newName);
            after.put(SimUtil.SIM_NUMBER, newPhoneNumber);
            after.put(SimUtil.SIM_ANR, newAnr);
            after.put(SimUtil.SIM_EMAILS, newEmail);

            // If update phone db failed, or update cache failed,
            // we will start service to load sim contacts again.
            boolean result = SimContactUtils.actuallyUpdateOneSimContactForEditor(
                    mContext.getContentResolver(), before, after, mRawContactId, mSlot);
            if (result) {
                viewSavedContact(mRawContactId);
            }
            boolean cacheUpdated = result ? SimContactCache.updateSimContact(
                    mRawContactId, mSlot, mIndexInSim, before, after) : false;
            //SimContactCache.dumpCache(mSubscription);
            if (!cacheUpdated) {
                Intent intent = new Intent(SimContactLoadService.ACTION_LOAD_SIM_CONTACTS_TO_PHONE_DB);
                intent.setClass(mActivity.getApplicationContext(), SimContactLoadService.class);
                intent.putExtra(SimContactLoadService.INTENT_KEY_SLOT_ID, mSlot);
                mActivity.startService(intent);
            }
        }

        private void viewSavedContact(long rawContactId) {
            if (mNotViewDetailOnSaveCompleted) {
                return;
            }
            final Uri rawContactUri = Uri.withAppendedPath(RawContacts.CONTENT_URI, String.valueOf(rawContactId));
            final Uri contactLookupUri = RawContacts.getContactLookupUri(mActivity.getContentResolver(), rawContactUri);
            Intent resultIntent = new Intent();
            resultIntent.setClass(getActivity(), ContactDetailActivity.class);
            resultIntent.setAction(Intent.ACTION_VIEW);
            resultIntent.putExtra(ContactDetailActivity.EXTRA_KEY_IS_SIM_CONTACT, true);
            resultIntent.setData(contactLookupUri);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            resultIntent.putExtra(ContactDetailActivity.INTENT_KEY_FORWARD_RESULT, Activity.RESULT_OK);
            mActivity.startActivity(resultIntent);
        }

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if ((mActivity == null) || mActivity.isFinishing() || mActivity.isDestroyed()) {
                Log.e(TAG, "handleMessage() activity is null or not active, activity="
                        + mActivity + "; msg:" + msg);
                return;
            }
            switch (msg.what) {
                case SAVE_SIM_CONTACTS_SUCCESS:
                    Log.d(TAG, "SAVE_SIM_CONTACTS_SUCCESS");
                    Toast.makeText(mContext, R.string.save_success, Toast.LENGTH_SHORT).show();
                    if (mActivity != null) {
                        mActivity.setResult(Activity.RESULT_OK);
                        mActivity.finish();
                    }
                    break;
                case SAVE_SIM_CONTACTS_FAIL:
                    Log.d(TAG, "SAVE_SIM_CONTACTS_FAIL");
                    final int errorType = msg.arg1;
                    processError(errorType);

                    setSaveButtonEnable(true);

                    break;
                default:
                    Log.d(TAG, "handleMessage default message!!!");
                    break;
            }
        }
    };

    private void processError(int errorType) {
        final String errorTips;
        switch (errorType) {
            case SimUtil.ERROR_SIM_UNKNOWN: // 0
                if (SimUtil.isAirplaneModeOn(mContext)
                        || (SimUtil.MULTISIM_ENABLE ? (!SimUtil.isSimAvailable(mSlot)) : (!SimUtil.isSimAvailable()))
                        || (SimContactLoadService.getSimLoadedCount(mSlot) <= 0)) {
                    errorTips = getString(R.string.sim_error_not_ready);
                } else {
                    errorTips = getString(R.string.sim_error_unknown);
                }
                break;
            case SimUtil.ERROR_SIM_NUMBER_TOO_LONG: // -1
                errorTips = getString(R.string.sim_error_number_too_long);
                break;
            case SimUtil.ERROR_SIM_TEXT_TOO_LONG: // -2
                errorTips = getString(R.string.sim_error_name_too_long);
                break;
            case SimUtil.ERROR_SIM_STORAGE_FULL: // -3
                if (SimContactLoadService.getSimLoadedCount(mSlot) <= 0) {
                    errorTips = getString(R.string.sim_error_not_ready);
                } else {
                    errorTips = getString(R.string.sim_error_storage_full);
                }
                break;
            case SimUtil.ERROR_SIM_NOT_READY: // -4
                errorTips = getString(R.string.sim_error_not_ready);
                break;
            case SimUtil.ERROR_SIM_PASSWORD_ERROR: // -5
                errorTips = getString(R.string.sim_error_password_error);
                break;
            case SimUtil.ERROR_SIM_ANR_TOO_LONG: // -6
                errorTips = getString(R.string.sim_error_anr_too_long);
                break;
            case SimUtil.ERROR_SIM_GENERIC_FAILURE: // -10
                if (SimUtil.isAirplaneModeOn(mContext)
                        || (SimUtil.MULTISIM_ENABLE ? (!SimUtil.isSimAvailable(mSlot)) : (!SimUtil.isSimAvailable()))
                        || (SimContactLoadService.getSimLoadedCount(mSlot) <= 0)) {
                    errorTips = getString(R.string.sim_error_not_ready);
                } else {
                    if (SimContactUtils.ACTION_INSERT_SIM_CONTACTS.equals(mAction)) {
                        errorTips = getString(R.string.insert_sim_contact_failed);
                    } else {
                        errorTips = getString(R.string.update_sim_contact_failed);
                    }
                }
                break;
            case SimUtil.ERROR_SIM_ADN_LIST_NOT_EXIST: // -11
                errorTips = getString(R.string.sim_error_adn_list_not_exist);
                break;
            case SimUtil.ERROR_SIM_EMAIL_FULL: // -12
                errorTips = getString(R.string.sim_error_email_full);
                break;
            case SimUtil.ERROR_SIM_EMAIL_TOO_LONG: // -13
                errorTips = getString(R.string.sim_error_email_too_long);
                break;
            case SimUtil.ERROR_SIM_ANR_SAVE_FAILURE: // -14
                errorTips = getString(R.string.sim_error_generic_error);
                break;
            default:
                Log.e(TAG, "save sim contact fail, error!!!");
                return;
        }

        Toast.makeText(mContext, errorTips, Toast.LENGTH_SHORT).show();
    }

}
