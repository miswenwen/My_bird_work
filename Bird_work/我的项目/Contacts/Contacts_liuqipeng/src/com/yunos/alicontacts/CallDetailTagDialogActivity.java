
package com.yunos.alicontacts;
//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
import com.yunos.alicontacts.list.ContactBrowseListFragment;
import android.view.View.OnClickListener;
import com.yunos.alicontacts.list.AccountFilterManager;
import android.widget.CheckBox;
import android.content.SharedPreferences.Editor;
import com.bird.contacts.BirdFeatureOption;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import com.yunos.alicontacts.activities.ContactSelectionActivity;
import java.util.List;
//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.PhoneLookup;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.database.util.NumberServiceHelper;
import com.yunos.alicontacts.platform.PDUtils;
import com.yunos.alicontacts.preference.DialerSettingActivity;
import com.yunos.alicontacts.util.AliTextUtils;
import com.yunos.alicontacts.util.FeatureOptionAssistant;
import com.yunos.alicontacts.util.YunOSFeatureHelper;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.Dialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.DialogInterface.OnCancelListener;

public class CallDetailTagDialogActivity extends Activity {
    private static final String TAG = "CallDetailTagDialogActivity";

    public String mTagNameHarassing;
    public String mTagNameSuspectedFraud;
    public String mTagNameInsuranceSales;
    public String mTagNameFinancialManagement;
    public String mTagNameRealEstate;
    public String mTagNameRecruit;
    public String mTagNameAdvertising;
    public String mTagNameRoomNumber;
    public String mTagNameExpressService;

    /**
     * The Action is for Mms and call log details page, which is popuped from
     * button pressed.
     */
    public static final String ACTION_MARK_TAG = "com.yunos.alicontacts.action.MARK_TAG";
    /**
     * The Action is for Phone. When the strange number is not marked, this
     * dialog activity will be popped up when one call is hung up.
     */
    public static final String ACTION_AUTO_MARK_TAG = "com.yunos.alicontacts.action.AUTO_MARK_TAG";
    /**
     * Extra number, which is needed to by marked.
     */
    public static final String EXTRA_KEY_NUMBER = "extra_key_number";
    /**
     * Extra tag type. If Mms or call log details to pop up this dialog to mark
     * a strange number, it need to check the number is marked or not, if
     * marked, reMarkDialog will be popped up.
     */
    public static final String EXTRA_KEY_TAG_NAME = "extra_key_tag_name";
    /**
     * Extra marked count. value -1 for user tag, non-negative for system tag.
     */
    public static final String EXTRA_KEY_MARKED_COUNT = "extra_key_marked_count";
    /**
     * The dialer settings shared prefs file.
     */
    public static final String DIALER_SETTINGS = "dialer_settings";
    /**
     * The result value back from this dialog activity. Mms or call log details
     * activity startActivityForResult() this activity, it can get tag_type and
     * tag_name when this activity is finished.
     */
    public static final String RESULT_EXTRA_KEY_TAG_NAME = "result_extra_key_tag_name";

    /**
     * The result value back from this dialog activity. If the user revoked a custom tag name,
     * this activity might return the system tag name and marked count.
     */
    public static final String RESULT_EXTRA_KEY_MARKED_COUNT = "result_extra_key_marked_count";

    /**
     * This dialog will be auto dismissed in 10s in markTagDialog when it is
     * popped up by Phone.
     */
    public static final int DISMISS_DELAY_TIME = 10 * 1000;

    /**
     * The max length of number in dialog title.
     * For the pattern: +86 <area_code> <phone_number>,
     * <area_code> is 2-3 digits,
     * <phone_number> is up to 10 digits (400-numbers).
     */
    private static final int MAX_NUMBER_LENGTH_IN_DIALOG_TITLE = 16;

    private String mNumber = null;
    private LayoutInflater mInflater;
    private Dialog mTagDialog;

    private AlertDialog mCustomTagDialog;
    private View mCustomTagView;

    private EditText mCustomTagEdit;

    // delay 300ms finish activity
    private final int FINISH_DELAY_TIME = 300;

    private String mAction = null;

    private AlertDialog mReMarkTagDialog;

    private String mTagName = null;
    private int mMarkedCount = -1;
	//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
	private CheckBox mDontRemindChk=null;
	private Button mNewContactBtn;
	private Button mExistingContactBtn;
	private String mIntentFromWhichActivity;
	private boolean mIntentIsNotFromCallDetailActivity=true;
	private GridView mMarkDlgGridView;
	//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTagNameHarassing = getString(R.string.tag_name_harassing);
        mTagNameSuspectedFraud = getString(R.string.tag_name_suspected_fraud);
        mTagNameInsuranceSales = getString(R.string.tag_name_insurance_sales);
        mTagNameFinancialManagement = getString(R.string.tag_name_financial_management);
        mTagNameRealEstate = getString(R.string.tag_name_real_estate);
        mTagNameRecruit = getString(R.string.tag_name_recruit);
        mTagNameAdvertising = getString(R.string.tag_name_advertising);
        mTagNameRoomNumber = getString(R.string.tag_name_room_number);
        mTagNameExpressService = getString(R.string.tag_name_express_service);

        Intent intent = this.getIntent();
        if (!initIntent(intent) || FeatureOptionAssistant.isInternationalSupportted()) {
            finish();
            return;
        }

        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mark();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UsageReporter.onResume(this, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
/*YunOS BEGIN PB*/
//##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
//##BugID:(8296205) ##date:2016-5-27 09:00
//##description:the mark dialog can not touch.
        setIntent(intent);
 /*YUNOS END PB*/        
        removeDelayedFinishRunable();
        if (!initIntent(intent)) {
            finish();
            return;
        }

        mark();
    }

    @Override
    protected void onPause() {
        super.onPause();
        UsageReporter.onPause(this, null);
    }

    @Override
    protected void onDestroy() {
        removeDelayedFinishRunable();
        dismissAllDialogs();
        super.onDestroy();
    }
    private void dismissAllDialogs() {
        if (mTagDialog != null) {
            mTagDialog.dismiss();
            mTagDialog = null;
        }
        if (mReMarkTagDialog != null) {
            mReMarkTagDialog.dismiss();
            mReMarkTagDialog = null;
        }
        if (mCustomTagDialog != null) {
            mCustomTagDialog.dismiss();
            mCustomTagDialog = null;
        }
    }

    private boolean initIntent(Intent intent) {
        mAction = intent.getAction();
		//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER adde by liuqipeng 20160918 start
		if(BirdFeatureOption.BIRD_DOOV_INCALL_MARK_NUMBER) {
	        mIntentFromWhichActivity=intent.getStringExtra("SrcActName");
			//if(mIntentFromWhichActivity!=null) {
			//	mIntentIsNotFromCallDetailActivity=false;
			//}
		}
		//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
        if (!ACTION_MARK_TAG.equals(mAction) && !ACTION_AUTO_MARK_TAG.equals(mAction)) {
            Log.e(TAG, "initIntent: unknown action, action is " + mAction);
            return false;
        }

        mNumber = intent.getStringExtra(EXTRA_KEY_NUMBER);
        if (TextUtils.isEmpty(mNumber)) {
            Log.e(TAG, "initIntent: mNumber is null");
            return false;
        }
        mNumber = mNumber.replace(" ", "").replace("-", "");

        mTagName = intent.getStringExtra(EXTRA_KEY_TAG_NAME);
        mMarkedCount = intent.getIntExtra(EXTRA_KEY_MARKED_COUNT, -1);

        if (ACTION_AUTO_MARK_TAG.equals(mAction)) {
            if (skipPrompt()) {
                Log.i(TAG, "initIntent: isNotPrompt");
                return false;
            }
            delayFinishActivity(DISMISS_DELAY_TIME);
        }

        return true;
    }

    private void removeDelayedFinishRunable() {
        mHandler.removeCallbacks(mDelayFinishActivityRunable);
    }

    private Runnable mDelayFinishActivityRunable = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    private void setResultFinish(String tagName, int markedCount) {
        Intent data = new Intent();
        data.putExtra(RESULT_EXTRA_KEY_TAG_NAME, tagName);
        data.putExtra(RESULT_EXTRA_KEY_MARKED_COUNT, markedCount);
        setResult(Activity.RESULT_OK, data);
        Log.d(TAG, "setResultFinish: tagName = " + tagName+"; markedCount="+markedCount);
        finish();
    }

    private String makeTitle() {
	//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER modified by liuqipeng 20160918 start
        String title = getString(R.string.mark_dialog_title);
		if(BirdFeatureOption.BIRD_DOOV_INCALL_MARK_NUMBER&&mIntentIsNotFromCallDetailActivity){
			title = getString(R.string.mark_dialog_title_new);
		}
	//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
        String strippedNumber = ContactsUtils.formatPhoneNumberWithCurrentCountryIso(mNumber, getApplicationContext());
        int len = strippedNumber.length();
        if (len > MAX_NUMBER_LENGTH_IN_DIALOG_TITLE) {
            strippedNumber = "..." + strippedNumber.substring(len - MAX_NUMBER_LENGTH_IN_DIALOG_TITLE, len);
        }
        title = String.format(title, strippedNumber);
        return title;
    }

    private void popupMarkDialog() {
        if (mTagDialog == null) {
            String title = makeTitle();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            CharSequence[] items = getResources().getTextArray(R.array.mark_tag_type_items);
			//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
			if(BirdFeatureOption.BIRD_DOOV_INCALL_MARK_NUMBER&&mIntentIsNotFromCallDetailActivity){
				builder.setCancelable(true);
				builder.setTitle(title);
				LayoutInflater mInflater = LayoutInflater.from(this);
				View mView = mInflater.inflate(R.layout.contact_mark_dlg_custom_view, null);
				mNewContactBtn=(Button)mView.findViewById(R.id.new_contact_btn);
				mExistingContactBtn=(Button)mView.findViewById(R.id.existing_contact_btn);
				mMarkDlgGridView=(GridView)mView.findViewById(R.id.mark_gridview);
				CharSequence[] gvItems = getResources().getTextArray(R.array.mark_tag_type_items_new);
				GridViewAdapter mAdapter=new GridViewAdapter(CallDetailTagDialogActivity.this, R.layout.item_custom_gridview,gvItems);
				mMarkDlgGridView.setAdapter(mAdapter);
				mMarkDlgGridView.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position,
							long id) {
						switch (position) {
							case 0:
								reportCallTypeAsync(true, mTagNameHarassing);
								UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
										UsageReporter.DialpadPage.MARK_AS_HARASSING_CALL);
								break;
							case 1:
								reportCallTypeAsync(true, mTagNameSuspectedFraud);
								UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
										UsageReporter.DialpadPage.MARK_AS_FRAUD_CALL);
								break;
							case 2:
								reportCallTypeAsync(false, mTagNameRealEstate);
								UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
										UsageReporter.DialpadPage.MARK_AS_HOUSE_AGENT_CALL);
								break;
							case 3:
							reportCallTypeAsync(false, mTagNameExpressService);
							UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
									UsageReporter.DialpadPage.MARK_AS_EXPRESS_CALL);
								break;
							case 4:
								reportCallTypeAsync(false, mTagNameAdvertising);
								UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
										UsageReporter.DialpadPage.MARK_AS_AD_PROMOTION_CALL);
								break;
							case 5:
								popupMarkCustomTagDialog();
								UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
										UsageReporter.DialpadPage.MARK_AS_CUSTOMED_CALL);
								break;
							default:
								break;
						}		
					}
				});
			mDontRemindChk=(CheckBox)mView.findViewById(R.id.dont_remind_chk);
			builder.setView(mView);
		}
		else{
		//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
            builder.setCancelable(true).setTitle(title).setItems(items, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int position) {
                    switch (position) {
                        case 0: // harassing
                            reportCallTypeAsync(true, mTagNameHarassing);
                            UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
                                    UsageReporter.DialpadPage.MARK_AS_HARASSING_CALL);
                            break;
                        case 1: // fraud
                            reportCallTypeAsync(true, mTagNameSuspectedFraud);
                            UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
                                    UsageReporter.DialpadPage.MARK_AS_FRAUD_CALL);
                            break;
                        case 2: // ad
                            reportCallTypeAsync(false, mTagNameAdvertising);
                            UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
                                    UsageReporter.DialpadPage.MARK_AS_AD_PROMOTION_CALL);
                            break;
                        case 3: // house agent
                            reportCallTypeAsync(false, mTagNameRealEstate);
                            UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
                                    UsageReporter.DialpadPage.MARK_AS_HOUSE_AGENT_CALL);
                            break;
                        case 4: // express
                            reportCallTypeAsync(false, mTagNameExpressService);
                            UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
                                    UsageReporter.DialpadPage.MARK_AS_EXPRESS_CALL);
                            break;
                        case 5: // custom
                            popupMarkCustomTagDialog();
                            UsageReporter.onClick(null, CallDetailTagDialogActivity.TAG,
                                    UsageReporter.DialpadPage.MARK_AS_CUSTOMED_CALL);
                            break;
                        default:
                            break;
                    }
                    if (mTagDialog != null) {
                        mTagDialog.dismiss();
                    }
                }

            });
		}

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    delayFinishActivity(FINISH_DELAY_TIME);
					//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
					if(BirdFeatureOption.BIRD_DOOV_INCALL_MARK_NUMBER&&mIntentIsNotFromCallDetailActivity){
						if(mDontRemindChk.isChecked()){
							boolean test=false;
							SharedPreferences mPrefs=getSharedPreferences(DIALER_SETTINGS, Context.MODE_PRIVATE);
							Editor editor = mPrefs.edit();
							editor.putBoolean(DialerSettingActivity.PREFS_KEY_AUTO_MARK, test);
							editor.apply();
						}
					}
					//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end
                }
            }).setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface arg0) {
                    delayFinishActivity(FINISH_DELAY_TIME);
               }
            });

            mTagDialog = builder.create();
			//@ {bird:BIRD_DOOV_INCALL_MARK_NUMBER added by liuqipeng 20160918 start
			if(BirdFeatureOption.BIRD_DOOV_INCALL_MARK_NUMBER&&mIntentIsNotFromCallDetailActivity){
				mNewContactBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
					 AccountFilterManager.getInstance(CallDetailTagDialogActivity.this)
				        .createNewContactWithPhoneNumberOrEmailAsync(
				                CallDetailTagDialogActivity.this,
				                mNumber,
				                null,
				                AccountFilterManager.INVALID_REQUEST_CODE);
					UsageReporter.onClick(CallDetailTagDialogActivity.this, null,
				                    UsageReporter.DialpadPage.DP_ADD_CONTACT_FROM_DETAIL);
					}
				});
				mExistingContactBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
					Intent createIntent2 = ContactsUtils.getInsertContactIntent(getApplicationContext(), ContactSelectionActivity.class,mNumber);
					startActivity(createIntent2);
					UsageReporter.onClick(CallDetailTagDialogActivity.this, null,
							        UsageReporter.DialpadPage.DP_ADD_EXISTING_CONTACT_FROM_DETAIL);
					finish();
					}
				});
			}
				//@ }bird:BIRD_DOOV_INCALL_MARK_NUMBER end

        }


        mTagDialog.show();
    }

    private void delayFinishActivity(int delayTime) {
        mHandler.removeCallbacks(mDelayFinishActivityRunable);
        mHandler.postDelayed(mDelayFinishActivityRunable, delayTime);
    }

    private void createOrUpdateCustomTagView() {
        if (mCustomTagView != null) {
            return;
        }
        mCustomTagView = mInflater.inflate(R.layout.custom_tag_dialog_view, null, false);
        mCustomTagEdit = (EditText) mCustomTagView.findViewById(R.id.edit_tag);
        mCustomTagEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null) {
                    updateCustomTagDialogConfirmBtn(s.toString());
                }
            }

        });
    }

    private void updateCustomTagDialogConfirmBtn(String text) {
        if (mCustomTagDialog != null) {
            Button postiveBtn = mCustomTagDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (postiveBtn != null) {
                if (TextUtils.isEmpty(text != null ? text.trim() : null)) {
                    postiveBtn.setEnabled(false);
                } else {
                    postiveBtn.setEnabled(true);
                }
            }
        }
    }

    private void popupMarkCustomTagDialog() {
        if (mCustomTagDialog == null) {
            createOrUpdateCustomTagView();
            String title = makeTitle();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mCustomTagDialog = builder.setCancelable(true).setTitle(title).setView(mCustomTagView)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mCustomTagEdit != null) {
                                String tagName = mCustomTagEdit.getText().toString();
                                if (tagName != null) {
                                    tagName = tagName.trim();
                                }
                                if (!TextUtils.isEmpty(tagName)) {
                                    reportCallTypeAsync(false, tagName);
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(CallDetailTagDialogActivity.this, R.string.custom_tag_empty_error,
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    }).setOnCancelListener(new OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface arg0) {
                            finish();
                        }
                    }).create();
        }

        removeDelayedFinishRunable();
        if (mCustomTagEdit != null) {
            String initTagText = "";
            mCustomTagEdit.setText(initTagText);
            mCustomTagEdit.setFocusableInTouchMode(true);
            mCustomTagEdit.requestFocus();
        }
        mCustomTagDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mCustomTagDialog.show();
        updateCustomTagDialogConfirmBtn(null);
    }

    private void reportCallTypeAsync(final boolean addBlack, final String tagName) {
        if (TextUtils.isEmpty(mNumber)) {
            Log.w(TAG, "reportCallTypeAsync: mNumber is empty.");
            return;
        }
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return NumberServiceHelper.markNumberWithRetryInBackground(getApplicationContext(), mNumber, tagName);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!result) {
                    Toast.makeText(getApplicationContext(), R.string.update_mark_tag_failed, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    if (addBlack) {
                        addBlacklist();
                    } else {
                        removeFromBlacklist(false);
                    }
                    // marked count -1 means user selected/customized tag.
                    setResultFinish(tagName, -1);
                }
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean skipPrompt() {
        SharedPreferences preferences = getSharedPreferences(DIALER_SETTINGS, Context.MODE_PRIVATE);
        boolean isAutoMark = preferences.getBoolean(DialerSettingActivity.PREFS_KEY_AUTO_MARK, true);
        if (!isAutoMark) {
            Log.i(TAG, "skipPrompt: auto mark is off.");
            return true;
        }
        if (!PDUtils.isPhoneIdle()) {
            Log.i(TAG, "skipPrompt: phone is in call.");
            return true;
        }
        if (isContactNumber()) {
            Log.i(TAG, "skipPrompt: contact number.");
            return true;
        }
        if (isMarkedOrYp()) {
            Log.i(TAG, "skipPrompt: already marked or is Yp.");
            return true;
        }
        return false;
    }

    private boolean isContactNumber() {
        boolean found = false;
        ContentResolver resolver = getContentResolver();
        Cursor c = null;
        try {
            Uri uri = PhoneLookup.CONTENT_FILTER_URI.buildUpon().appendPath(mNumber).build();
            c = resolver.query(uri, new String[] {
                PhoneLookup._ID
            }, null, null, null);
            int count = c == null ? -1 : c.getCount();
            found = count > 0;
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return found;
    }

    private boolean isMarkedOrYp() {
        boolean markedOrYp = false;
        Cursor c = null;
        try {
            ContentResolver resolver = getContentResolver();
            Uri queryUri = NumberServiceHelper.getSingleNumberInfoQueryForNumber(mNumber);
            Log.d(TAG, "isMarkedOrYp() uniform query up. mNumber = " + AliTextUtils.desensitizeNumber(mNumber));
            c = resolver.query(queryUri, null, null, null, null);
            Log.d(TAG, "isMarkedOrYp() uniform query down. cursor = " + c);
            if ((c != null) && c.moveToFirst()) {
                String serverTag = c.getString(NumberServiceHelper.SINGLE_NUMINFO_COLUMN_SERVER_TAG_NAME);
                String userTag = c.getString(NumberServiceHelper.SINGLE_NUMINFO_COLUMN_USER_TAG_NAME);
                String ypName = c.getString(NumberServiceHelper.SINGLE_NUMINFO_COLUMN_YP_NAME);
                if (!(TextUtils.isEmpty(serverTag) && TextUtils.isEmpty(userTag)
                        && TextUtils.isEmpty(ypName))) {
                    markedOrYp = true;
                }
                Log.d(TAG, "isMarkedOrYp() uniform query number serverTag = "
                        + serverTag + ", userTag = " + userTag + ", ypName = " + ypName);
            }
        } catch (SQLiteException sqle) {
            Log.e(TAG, "isMarkedOrYp: failed to query number info.", sqle);
        } finally {
             if (c != null) {
                c.close();
            }
        }
        return markedOrYp;
    }

    private void mark() {
        dismissAllDialogs();
        // If it has not tag info, then we need pop mark dialog.
        // If it has a positive marked count, then we do not have revoke mark in ReMarkDialog,
        // so we go directly to custom mark dialog.
        if (TextUtils.isEmpty(mTagName) || (mMarkedCount >= 0)) {
            popupMarkDialog();
        } else {
            popupReMarkDialog();
        }
    }

    private void popupReMarkDialog() {
        if (mReMarkTagDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            CharSequence[] items = {
                    getString(R.string.remark_dialog_remark), getString(R.string.remark_dialog_cancel)
            };
            builder.setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int which) {
                    switch (which) {
                        case 0: // remark tag
                            UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.CLICK_MARK_STRANGE_CALL_MARK_AGAIN);
                            popupMarkDialog();
                            break;
                        case 1: // revoke tag
                            UsageReporter.onClick(null, TAG, UsageReporter.DialpadPage.CLICK_MARK_STRANGE_CALL_CANCLE_MARK);
                            revokeMarkTagAsync();
                            break;
                        default:
                            break;
                    }

                }

            });

            builder.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface arg0) {
                    delayFinishActivity(FINISH_DELAY_TIME);
                }
            });

            mReMarkTagDialog = builder.create();
        }

        mReMarkTagDialog.show();
    }

    private void removeFromBlacklist(final boolean revoke) {
        if (TextUtils.isEmpty(mNumber)) {
            Log.d(TAG, "removeFromBlacklist: mNumber is empty.");
            return;
        }
        boolean isInBlackList = YunOSFeatureHelper.isBlack(this, mNumber);
        if (isInBlackList) {
            boolean result = YunOSFeatureHelper.removeBlack(this, mNumber);
            Log.d(TAG, "removeFromBlacklist: mNumber="+AliTextUtils.desensitizeNumber(mNumber)+"; result="+result);
            if (result) {
                Toast.makeText(getApplicationContext(),
                        revoke ? R.string.revoke_tag_removeFromBlackListOK
                               : R.string.mark_tag_removeFromBlackListOK,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addBlacklist() {
        if (TextUtils.isEmpty(mNumber)) {
            Log.d(TAG, "addBlacklist: mNumber is empty.");
            return;
        }

        boolean isInBlackList = YunOSFeatureHelper.isBlack(this, mNumber);
        if (!isInBlackList) {
            boolean result = YunOSFeatureHelper.addBlack(this, mNumber, 3);
            Log.d(TAG, "addBlacklist: mNumber=" + AliTextUtils.desensitizeNumber(mNumber)+"; result="+result);
            if (result) {
                Toast.makeText(getApplicationContext(), R.string.mark_tag_addToBlackListOK,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void revokeMarkTagAsync() {
        if (TextUtils.isEmpty(mNumber)) {
            Log.e(TAG, "revokeMarkTagAsync: mNumber is empty.");
            return;
        }
        AsyncTask<Void, Void, TagInfo> task = new AsyncTask<Void, Void, TagInfo>() {
            @Override
            protected TagInfo doInBackground(Void... params) {
                return revokeMarkTagInBackground();
            }

            @Override
            protected void onPostExecute(TagInfo result) {
                if (result == null) {
                    Log.i(TAG, "revokeMarkTagAsync.onPostExecute: null result, quit.");
                    finish();
                    return;
                }
                Log.i(TAG, "revokeMarkTagAsync.onPostExecute: result="+result);
                // When tag type is TAG_TYPE_HARASSING or TAG_TYPE_SUSPECTED_FRAUD, need
                // to check mNumber is in black list or not. If in black list, need to
                // remove the number from black.
                if (mTagNameHarassing.equals(mTagName) || mTagNameSuspectedFraud.equals(mTagName)) {
                    removeFromBlacklist(true);
                }
                setResultFinish(result.name, result.count);
            }

        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private TagInfo revokeMarkTagInBackground() {
        if (isFinishing() || isDestroyed()) {
            Log.i(TAG, "revokeMarkTagInBackground: activity is not active, quit.");
            return null;
        }

        ContentResolver resolver = getContentResolver();
        NumberServiceHelper.markNumberWithRetryInBackground(getApplicationContext(), mNumber, null);

        // Query new tag info, because when we revoke a user defined tag,
        // we might get a system defined tag.
        return getTagInfoFromNumberService(resolver, mNumber);
    }

    private TagInfo getTagInfoFromNumberService(ContentResolver resolver, String number) {
        Uri queryUri = NumberServiceHelper.getSingleNumberInfoQueryForNumber(number);
        Cursor c = null;
        try {
            c = resolver.query(queryUri, null, null, null, null);
            if ((c != null) && c.moveToFirst()) {
                String serverTag = c.getString(NumberServiceHelper.SINGLE_NUMINFO_COLUMN_SERVER_TAG_NAME);
                int markedCount = c.getInt(NumberServiceHelper.SINGLE_NUMINFO_COLUMN_MARKED_COUNT);
                String userTag = c.getString(NumberServiceHelper.SINGLE_NUMINFO_COLUMN_USER_TAG_NAME);
                if (TextUtils.isEmpty(userTag)) {
                    return new TagInfo(serverTag, markedCount);
                } else {
                    return new TagInfo(userTag, -1);
                }
            }
        } catch (SQLiteException sqle) {
            Log.e(TAG, "getTagInfoFromNumberService: failed to query number info.", sqle);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return new TagInfo("", -1);
    }

    private static class TagInfo {
        public final String name;
        public final int count;

        public TagInfo(String name, int count) {
            this.name = name;
            this.count = count;
        }

    }
}
