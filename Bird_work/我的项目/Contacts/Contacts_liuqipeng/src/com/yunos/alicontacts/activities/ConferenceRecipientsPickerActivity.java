
package com.yunos.alicontacts.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Toast;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.conference.QueryPhoneNumberHandler;
import com.yunos.alicontacts.conference.Recipient;
import com.yunos.alicontacts.conference.RecipientsListAdapter;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.list.PhoneNumberMultiplePickerFragment;
import com.yunos.alicontacts.util.Constants;
import com.yunos.common.UiTools;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ConferenceRecipientsPickerActivity extends ListActivity {
    private static final String TAG = "ConferenceRecipientsPickerActivity";

    public static final String ACTION_CONFERENCE_RECIPIENTS_PICKER
            = "com.yunos.alicontacts.action.CONFERENCE_RECIPIENTS_PICKER";
    public static final int MAX_RECIPIENTS_COUNT = 5;

    /**
     * When we want to add more recipients to a conference call,
     * send an intent to this activity with following extra data.
     * The data is an array of strings, that represent the existing numbers
     * in current conference call.
     */
    private static final String EXTRA_KEY_EXISTING_RECIPIENTS_NUMBER
            = "extra.existing.recipients.number";
    /**
     * When we have selected new recipients for current conference call,
     * we will put the new numbers in the result intent with following key.
     * The data is an array of strings.
     */
    private static final String RESULT_EXTRA_KEY_NEW_RECIPIENTS_NUMBER
            = "extra.new.recipients.number";

    private static final int REQ_CODE_PICK_CONTACT_PHONE_NUMBER = 100;
    private static final String QUERY_PHONE_NUMBER_THREAD_NAME = "conference_query_phone_number_thread";

    private ImageView mBackIcon;
    private ImageView mAddNumberIcon;
    private ImageView mAddContactIcon;
    private ListView mListView;
    private Button mMakeCallButton;

    private OnClickListener mControlsOnClickListener;

    private AlertDialog mAddNumberDialog;
    private EditText mAddNumberText = null;
    private DialogInterface.OnClickListener mAddNumberDialogListener = null;

    private RecipientsListAdapter mAdapter;
    private boolean mIsAddToCall = false;

    private HandlerThread mQueryPhoneNumberHandlerThread;
    private QueryPhoneNumberHandler mQueryPhoneNumberHandler;

    /*
     * When make conference call, we want this activity finish after new activity covers it.
     * So we have to call finish() after onPause(), e.g. onStop().
     */
    private boolean mFinishOnStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conference_recipients_picker);
        initControls();
        initAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAddNumberDialog != null) {
            mAddNumberDialog.dismiss();
            mAddNumberDialog = null;
        }
        if (mQueryPhoneNumberHandlerThread != null) {
            mQueryPhoneNumberHandlerThread.quit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFinishOnStop) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_CODE_PICK_CONTACT_PHONE_NUMBER) {
            Log.w(TAG, "onActivityResult: invalid reqCode="+requestCode);
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            Log.i(TAG, "onActivityResult: result is not OK. "+resultCode);
            return;
        }
        Uri[] result = getUrisFromNumberPickResult(data);
        queryNumberInfoForUris(result);
    }

    public void enableMakeCall(boolean enabled) {
        mMakeCallButton.setEnabled(enabled);
    }

    public void setRemoveClickListener(ImageView removeIcon) {
        removeIcon.setOnClickListener(mControlsOnClickListener);
    }

    /**
     * This is called when query phone number info completed.
     * This method shall be run on main thread.
     * @param recipients
     */
    public void onQueryNumberInfoCompleted(Recipient[] recipients) {
        if (isFinishing() || isDestroyed()) {
            Log.i(TAG, "onQueryNumberInfoCompleted: activity is not active.");
            return;
        }
        mAdapter.mergeContactRecipients(recipients);
        queryLocationForRecipients();
    }

    /**
     * This is called when query location completed.
     * This method shall be run on main thread.
     */
    public void onQueryLocationCompleted() {
        if (isFinishing() || isDestroyed()) {
            Log.i(TAG, "onQueryLocationCompleted: activity is not active.");
            return;
        }
        mAdapter.notifyDataSetChanged();
    }

    private void initControls() {
        initControlsOnClickListener();

        mBackIcon = (ImageView) findViewById(R.id.back_ico);
        mBackIcon.setOnClickListener(mControlsOnClickListener);
        mAddNumberIcon = (ImageView) findViewById(R.id.add_phone_number);
        mAddNumberIcon.setOnClickListener(mControlsOnClickListener);
        mAddContactIcon = (ImageView) findViewById(R.id.add_contact);
        mAddContactIcon.setOnClickListener(mControlsOnClickListener);

        mListView = getListView();

        mMakeCallButton = (Button) findViewById(R.id.btn_make_call);
        mMakeCallButton.setEnabled(false);
        mMakeCallButton.setOnClickListener(mControlsOnClickListener);

        setSystembarColor(getResources().getColor(R.color.title_color), false);
    }

    private void initControlsOnClickListener() {
        mControlsOnClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if ((id != R.id.back_ico) && hasPendingChanges()) {
                    Toast.makeText(getApplicationContext(), R.string.conference_call_pending_recipients_changes,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (id) {
                case R.id.back_ico:
                    Log.i(TAG, "mControlsOnClickListener.onClick: back clicked, quit.");
                    finish();
                    break;
                case R.id.add_phone_number:
                    Log.i(TAG, "mControlsOnClickListener.onClick: add number clicked.");
                    handleAddPhoneNumber();
                    break;
                case R.id.add_contact:
                    Log.i(TAG, "mControlsOnClickListener.onClick: add contact clicked.");
                    handleAddContact();
                    break;
                case R.id.btn_make_call:
                    Log.i(TAG, "mControlsOnClickListener.onClick: make call clicked.");
                    handleMakeConferenceCall();
                    break;
                case R.id.remove_icon:
                    Log.i(TAG, "mControlsOnClickListener.onClick: remove clicked.");
                    handleRemoveRecipient((Recipient)v.getTag());
                    break;
                default:
                    Log.w(TAG, "mControlsOnClickListener.onClick: unrecognized view "+id);
                    break;
                }
            }
        };
    }

    /**
     * When we want to modify the recipients list, we shall make sure there are
     * no pending changes to the recipients.
     * Or we will prepare the new modify operations based on old data and
     * apply the operations based on the result of pending changes.
     * In this case, we might get a wrong result.
     * @return
     */
    private boolean hasPendingChanges() {
        if (mQueryPhoneNumberHandler == null) {
            return false;
        }
        boolean result = mQueryPhoneNumberHandler.isWorkingOnMessage(QueryPhoneNumberHandler.WHAT_QUERY_PHONE_NAMES)
                || mQueryPhoneNumberHandler.isWorkingOnMessage(QueryPhoneNumberHandler.WHAT_QUERY_PHONE_URIS)
                || mQueryPhoneNumberHandler.hasMessages(QueryPhoneNumberHandler.WHAT_QUERY_PHONE_NAMES)
                || mQueryPhoneNumberHandler.hasMessages(QueryPhoneNumberHandler.WHAT_QUERY_PHONE_URIS);
        Log.i(TAG, "hasPendingChanges: result is "+result);
        return result;
    }

    private void setSystembarColor(int color, boolean showActionBar) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, showActionBar);
        systemBarManager.setStatusBarColor(color);
    }

    private void initAdapter() {
        Intent intent = getIntent();
        List<String> pinnedNumbers = intent == null ?
                null : intent.getStringArrayListExtra(EXTRA_KEY_EXISTING_RECIPIENTS_NUMBER);
        int count = pinnedNumbers == null ? 0 : pinnedNumbers.size();
        if (count >= MAX_RECIPIENTS_COUNT) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.toast_conference_recipients_full, MAX_RECIPIENTS_COUNT),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayList<Recipient> recipients = new ArrayList<Recipient>(count);
        HashSet<String> pinnedNumbersSet = new HashSet<String>(count);
        if (count > 0) {
            mIsAddToCall = true;
            mMakeCallButton.setText(R.string.conference_call_add_to_call);
            for (String number : pinnedNumbers) {
                Recipient recipient = new Recipient(null, null, number);
                if (!TextUtils.isEmpty(recipient.formattedNumber)) {
                    recipients.add(recipient);
                    pinnedNumbersSet.add(recipient.formattedNumber);
                }
            }
        }
        mAdapter = new RecipientsListAdapter(recipients, pinnedNumbersSet, this);
        mListView.setAdapter(mAdapter);
        if (count > 0) {
            // the queries must be put after we initialize mAdapter.
            queryLocationForRecipients();
            queryNameForNumberRecipients();
        }
    }

    private void handleAddPhoneNumber() {
        if (mAdapter.getCount() >= MAX_RECIPIENTS_COUNT) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.toast_conference_recipients_full, MAX_RECIPIENTS_COUNT),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if ((mAddNumberDialog != null) && mAddNumberDialog.isShowing()) {
            mAddNumberDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mAddNumberDialog = builder.setCancelable(true)
                .setPositiveButton(R.string.yes, getAddNumberDialogListener())
                .setNegativeButton(R.string.no, getAddNumberDialogListener())
                .create();

        prepareAddNumberTextView();
        mAddNumberDialog.setView(mAddNumberText);
        mAddNumberText.requestFocus();
        mAddNumberDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mAddNumberDialog.show();
    }

    private EditText prepareAddNumberTextView() {
        if (mAddNumberText == null) {
            mAddNumberText = new EditText(this);
            mAddNumberText.setSingleLine();
            mAddNumberText.setFocusable(true);
            mAddNumberText.setFocusableInTouchMode(true);
            mAddNumberText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            //UiTools.addDialerListenerToEditText(mAddNumberText);
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            final int margin = getResources().getDimensionPixelSize(R.dimen.custom_tag_dialog_edit_margin_left_right);
            p.gravity = Gravity.CENTER;
            p.setMarginStart(margin);
            p.setMarginEnd(margin);
            mAddNumberText.setLayoutParams(p);
        }
        mAddNumberText.setText("");
        // we reuse the EditText from ex-dialog, so we have to remove the EditText
        // from its parent in ex-dialog. Otherwise, crash.
        ViewParent parent = mAddNumberText.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(mAddNumberText);
        }
        return mAddNumberText;
    }

    private DialogInterface.OnClickListener getAddNumberDialogListener() {
        if (mAddNumberDialogListener == null) {
            mAddNumberDialogListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        Log.i(TAG, "mAddNumberDialogListener.onClick: add number.");
                        String number = mAddNumberText.getText().toString();
                        mAdapter.addPhoneNumber(number);
                        queryLocationForRecipients();
                        queryNameForNumberRecipients();
                    }
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    UiTools.closeSoftInput(ConferenceRecipientsPickerActivity.this);
                }
            };
        }
        return mAddNumberDialogListener;
    }

    private void handleAddContact() {
        Parcelable[] existingPhoneUris = mAdapter.getSelectedPhoneNumbersForPickContactsIntent();
        int remainder = MAX_RECIPIENTS_COUNT - (mAdapter.getCount() - existingPhoneUris.length);
        Log.i(TAG, "handleAddContact: remainder="+remainder);
        if (remainder <= 0) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.toast_conference_recipients_full, MAX_RECIPIENTS_COUNT),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        long[] pinnedNumberDataIds = mAdapter.getPinnedNumberDataIds();
        Intent intent = new Intent(GroupContactSelectionActivity.ACTION_PICK_MULTIPLE);
        intent.setClass(getApplicationContext(), GroupContactSelectionActivity.class);
        intent.putExtra(GroupContactSelectionActivity.PICK_CONTENT, GroupContactSelectionActivity.PICK_PHONE_NUMBER);
        intent.putExtra(GroupContactSelectionActivity.PICK_RECIPIENT_LIMIT, remainder);
        intent.putExtra(Constants.EXTRA_PHONE_URIS, existingPhoneUris);
        intent.putExtra(PhoneNumberMultiplePickerFragment.EXTRA_KEY_PINNED_DATA_IDS, pinnedNumberDataIds);
        startActivityForResult(intent, REQ_CODE_PICK_CONTACT_PHONE_NUMBER);
    }

    private void handleRemoveRecipient(Recipient recipient) {
        mAdapter.removeRecipient(recipient);
    }

    private void handleMakeConferenceCall() {
        ArrayList<String> numbers = mAdapter.getNumbersForMakeConferenceCall();
        if (mIsAddToCall) {
            Intent data = new Intent();
            data.putExtra(RESULT_EXTRA_KEY_NEW_RECIPIENTS_NUMBER, numbers);
            setResult(RESULT_OK, data);
            finish();
        } else {
            AliCallLogExtensionHelper.makeConferenceCall(this, numbers);
            mFinishOnStop = true;
        }
    }

    private Uri[] getUrisFromNumberPickResult(Intent data) {
        Bundle extras = data.getExtras();
        if (extras == null) {
            Log.i(TAG, "getUrisFromNumberPickResult: no extra in result.");
            return new Uri[0];
        } else {
            Parcelable[] phoneUrisArray = extras.getParcelableArray(Constants.EXTRA_PHONE_URIS);
            if (phoneUrisArray == null) {
                Log.i(TAG, "getUrisFromNumberPickResult: no data specified in result.");
                return new Uri[0];
            }
            int len = phoneUrisArray.length;
            if (len <= 0) {
                Log.i(TAG, "getUrisFromNumberPickResult: empty data specified in result.");
                return new Uri[0];
            }
            Log.i(TAG, "getUrisFromNumberPickResult: result data size="+len);
            Uri[] result = new Uri[len];
            System.arraycopy(phoneUrisArray, 0, result, 0, len);
            return result;
        }
    }

    private void queryNumberInfoForUris(Uri[] uris) {
        initQueryPhoneNumberHandler();
        Message msg = mQueryPhoneNumberHandler.obtainMessage(QueryPhoneNumberHandler.WHAT_QUERY_PHONE_URIS);
        msg.obj = uris;
        mQueryPhoneNumberHandler.sendMessage(msg);
    }

    private void queryLocationForRecipients() {
        Recipient[] recipients = mAdapter.getAllRecipients();
        if (!checkMissingLocation(recipients)) {
            Log.i(TAG, "queryLocationForRecipients: all recipients have location, skip query.");
            return;
        }
        initQueryPhoneNumberHandler();
        Message msg = mQueryPhoneNumberHandler.obtainMessage(QueryPhoneNumberHandler.WHAT_QUERY_LOCATIONS);
        msg.obj = recipients;
        mQueryPhoneNumberHandler.sendMessage(msg);
    }

    private boolean checkMissingLocation(Recipient[] recipients) {
        for (Recipient recipient : recipients) {
            if (recipient.getLocation() == null) {
                return true;
            }
        }
        return false;
    }

    private void queryNameForNumberRecipients() {
        Recipient[] recipients = mAdapter.getAllRecipients();
        if (!checkMissingName(recipients)) {
            Log.i(TAG, "queryNameForNumberRecipients: all recipients have name, skip query.");
            return;
        }
        initQueryPhoneNumberHandler();
        Message msg = mQueryPhoneNumberHandler.obtainMessage(QueryPhoneNumberHandler.WHAT_QUERY_PHONE_NAMES);
        msg.obj = recipients;
        mQueryPhoneNumberHandler.sendMessage(msg);
    }

    private boolean checkMissingName(Recipient[] recipients) {
        for (Recipient recipient : recipients) {
            if (TextUtils.isEmpty(recipient.name)) {
                return true;
            }
        }
        return false;
    }

    private void initQueryPhoneNumberHandler() {
        if (mQueryPhoneNumberHandler != null) {
            return;
        }
        mQueryPhoneNumberHandlerThread = new HandlerThread(QUERY_PHONE_NUMBER_THREAD_NAME);
        mQueryPhoneNumberHandlerThread.start();
        mQueryPhoneNumberHandler = new QueryPhoneNumberHandler(this, mQueryPhoneNumberHandlerThread);
    }
}
