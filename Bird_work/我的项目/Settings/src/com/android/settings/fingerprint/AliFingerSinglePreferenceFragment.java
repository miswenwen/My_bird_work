package com.android.settings.fingerprint;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;

//import hwdroid.preference.ActionListPreference;
import android.preference.ListPreference;

import android.util.Base64;
import android.util.Log;
import com.android.settings.R;
import android.view.View;
import android.content.Intent;
import android.database.Cursor;

import android.provider.ContactsContract.Data;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import java.util.ArrayList;
import android.provider.Settings;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.Selection;

import android.content.Context;
import android.app.FragmentTransaction;

import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.text.TextUtils;
import java.util.List;

import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;
import android.os.CancellationSignal;

import android.app.admin.DevicePolicyManager;

import android.app.Activity;
import android.os.Handler;

import android.app.DialogFragment;
import android.app.Dialog;
import android.widget.EditText;
import android.view.WindowManager;

public class AliFingerSinglePreferenceFragment  extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

	protected static final int RESULT_FIRST_USER = 1;
	/**
	* Used by the choose fingerprint wizard to indicate the wizard is
	* finished, and each activity in the wizard should finish.
	* <p>
	* Previously, each activity in the wizard would finish itself after
	* starting the next activity. However, this leads to broken 'Back'
	* behavior. So, now an activity does not finish itself until it gets this
	* result.
	*/
	protected static final int RESULT_FINISHED = RESULT_FIRST_USER;

	/**
	* Used by the enrolling screen during setup wizard to skip over setting up fingerprint, which
	* will be useful if the user accidentally entered this flow.
	*/
	protected static final int RESULT_SKIP = RESULT_FIRST_USER + 1;

	/**
	* Like {@link #RESULT_FINISHED} except this one indicates enrollment failed because the
	* device was left idle. This is used to clear the credential token to require the user to
	* re-enter their pin/pattern/password before continuing.
	*/
	protected static final int RESULT_TIMEOUT = RESULT_FIRST_USER + 2;				
	
	private static final String TAG = "AliFingerSinglePreferenceFragment";

	public static final int FINGERQUICK_RETURNCODE_SELECT_CONTACT  = 0;
	public static final int FINGERQUICK_RETURNCODE_SELECT_APP  = 1;
	public static final int FINGERQUICK_RETURNCODE_REENROLL = 2;

	private static final String ALI_KEY_FINGEPRINT_NAME = "ali_key_fingerprint_name";
	private static final String ALI_KEY_FINGERQUICK_TYPE = "ali_key_fingerquick_type";
	private static final String ALI_KEY_FINGERQUICK_NUMBER = "fingerquick_phone_number";
	private static final String ALI_KEY_FINGERQUICK_STARTAPP = "fingerquick_settings_start_app";
	private static final String ALI_KEY_FINGERQUICK_SETTINGS = "fingerquick_settings_category";
	private static final String ALI_KEY_FINGERQUICK_UNLOCK_TIP = "fingerquick_unlock_tip";
	private static final String ALI_KEY_FINGEPRINT_REENROLL = "ali_key_fingerprint_reenroll";
	private static final String ALI_KEY_FINGEPRINT_DELETE = "ali_key_fingerprint_delete";
	
	
	private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
	private static final int MSG_FINGER_AUTH_SUCCESS = 1001;
	private static final int MSG_FINGER_AUTH_FAIL = 1002;
	private static final int MSG_FINGER_AUTH_ERROR = 1003;
	private static final int MSG_FINGER_AUTH_HELP = 1004;		
	
	private Preference mFingerprintNamePref;
	private ListPreference mFingerquickTypePref;
	private /*EditText*/Preference mFingerquickNumberPref;
	private Preference mFingerquickStartAppPref;
	private PreferenceCategory mFingerquickTargetCate;
	private Preference mFingerquickUnlockTip;
	private Preference mFingerPrintReenroll;
	private Preference mFingerPrintDelete;

	private String[] mFingerquickTypeList;

	private int mOperationFromDB;
	private String mTargetFromDB;
	private String mDataFromDB;

	private int mOperationFromUser;
	private String mTargetNumberFromUser;
	private String mTargetAppFromUser;
	private String mTargetActivityFromUser;
	
	private CancellationSignal mFingerprintCancel;		
	
	private boolean mInFingerprintLockout;		
	private byte[] mToken;
	private boolean mLaunchedConfirm;
	
	private static final long LOCKOUT_DURATION = 30000; // time we have to wait for fp to reset, ms		

	Fingerprint mFingerprint = null;
	private FingerprintManager mFingerprintManager;
	private static Context mContext;
	public static final int RESULT_OK = -1;
	
	private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";
	private static final int CONFIRM_REQUEST = 101;
	private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

	private static final int ADD_FINGERPRINT_REQUEST = 10;		

	private static final int MSG_START_SELECT_ACTIVITY = 1000;
	private boolean mSelectActivityShowing;// avoid to repeatly open
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_START_SELECT_ACTIVITY:
					if(mSelectActivityShowing) {
						return;
					}
					if (msg.arg1 == AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT) {
						mSelectActivityShowing = true;
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_PICK);
						intent.setType(Phone.CONTENT_TYPE/*"vnd.android.cursor.dir/phone_v2"*/);
						if (null == intent.resolveActivity(mContext.getPackageManager())) {
							Log.d(TAG, "no match activity for this intent" + intent.toString());
						} else {
							AliFingerSinglePreferenceFragment.this.startActivityForResult(intent, FINGERQUICK_RETURNCODE_SELECT_CONTACT);
						}
					} else if(msg.arg1 == AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP) {
						
						mSelectActivityShowing = true;
						Intent intent = new Intent(mContext, AliFingerQuickAppList.class);
						intent.putExtra("fingerid", mFingerprint.getFingerId());
						intent.putExtra("target", mTargetAppFromUser);
						intent.putExtra("activity", mTargetActivityFromUser);
						AliFingerSinglePreferenceFragment.this.startActivityForResult(intent, FINGERQUICK_RETURNCODE_SELECT_APP);
						
					}
					break;
				default:
					break;
			}
			super.handleMessage(msg);
		}
	};
		

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mToken = getActivity().getIntent().getByteArrayExtra(
			ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);

		
		initData();
		mContext = getActivity().getApplicationContext();
		mFingerprintManager = (FingerprintManager) mContext.getSystemService(
			Context.FINGERPRINT_SERVICE);
		
		addPreferencesFromResource(R.xml.ali_finger_single_manage);

		mFingerprintNamePref = (Preference) findPreference(ALI_KEY_FINGEPRINT_NAME);
		mFingerprintNamePref.setTitle(mFingerprint.getName().toString());
		mFingerprintNamePref.setOnPreferenceClickListener(this);
		
		mFingerPrintReenroll = (Preference) findPreference(ALI_KEY_FINGEPRINT_REENROLL);
		mFingerPrintReenroll.setOnPreferenceClickListener(this);

		mFingerquickTypeList = getResources().getStringArray(R.array.fingerquick_type_list);
		mFingerquickTypePref = (ListPreference) findPreference(ALI_KEY_FINGERQUICK_TYPE);
		mFingerquickTypePref.setOnPreferenceChangeListener(this);

		mFingerquickNumberPref = (Preference) findPreference(ALI_KEY_FINGERQUICK_NUMBER);
		mFingerquickNumberPref.setOnPreferenceClickListener(this);

		mFingerquickStartAppPref = (Preference) findPreference(ALI_KEY_FINGERQUICK_STARTAPP);
		mFingerquickStartAppPref.setOnPreferenceClickListener(this);

		mFingerquickTargetCate = (PreferenceCategory) findPreference(ALI_KEY_FINGERQUICK_SETTINGS);
		mFingerquickUnlockTip = (Preference) findPreference(ALI_KEY_FINGERQUICK_UNLOCK_TIP);
		
		mFingerPrintDelete = (Preference) findPreference(ALI_KEY_FINGEPRINT_DELETE);
		mFingerPrintDelete.setOnPreferenceClickListener(this);
		
		mSelectActivityShowing = false;
		
	}
	
	private void initData() {
		mFingerprint = (Fingerprint) getActivity().getIntent().getExtras().getParcelable(AliFingerprintSettingsPreference.FINGERPRINT);
		if(mFingerprint == null) getActivity().finish();
	}		

	@Override
	public void onResume() {
		super.onResume();
		initDataFromDB();
		switchPreferenceDisplay(mOperationFromUser);
		if(mOperationFromUser == AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE) {
			mFingerquickTypePref.setValueIndex(2);
		} else {
			mFingerquickTypePref.setValueIndex(mOperationFromUser);
		}
	}
	@Override
	public void onPause() {
		super.onPause();
		//disable save when onPause, other save only when target changed
		if(mOperationFromUser == AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE) {
			syncDatatoDB();
		}			
	}
		
	boolean DEBUG = true;
	
	public class RenameDeleteDialog extends DialogFragment {
		
			private static final int RENAME_DIALOG = 0;
			private static final int DELETE_DIALOG = 1;
			
			private final Context mContext;
            private Fingerprint mFp;
            private EditText mDialogTextField;
            private String mFingerName;
            private Boolean mTextHadFocus;
            private int mTextSelectionStart;
            private int mTextSelectionEnd;
			private int mDialogType;
			
            public RenameDeleteDialog(Context context, int dialogType) {
                mContext = context;
				mDialogType = dialogType;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
				if (savedInstanceState != null && mDialogType == RENAME_DIALOG) {
					mFingerName = savedInstanceState.getString("fingerName");
					mTextHadFocus = savedInstanceState.getBoolean("textHadFocus");
					mTextSelectionStart = savedInstanceState.getInt("startSelection");
					mTextSelectionEnd = savedInstanceState.getInt("endSelection");
				}
				switch (mDialogType) {
					case RENAME_DIALOG: // rename fingerprint dialog
						final AlertDialog alertRenameDialog = new AlertDialog.Builder(getActivity())
								.setView(R.layout.fingerprint_rename_dialog)
								.setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												final String newName =
														mDialogTextField.getText().toString();
												final CharSequence name = mFp.getName();
												if (!newName.equals(name)) {
													if (DEBUG) {
														Log.v(TAG, "rename " + name + " to " + newName);
													}
													MetricsLogger.action(getContext(),
															MetricsLogger.ACTION_FINGERPRINT_RENAME,
															mFp.getFingerId());
													AliFingerSinglePreferenceFragment parent
															= (AliFingerSinglePreferenceFragment)
															getTargetFragment();
													if (parent.isFingerNameAvailabled(newName)) {
														parent.renameFingerPrint(mFp.getFingerId(),
															newName);
													} else {
														showToast(R.string.fingerprint_rename_repeatename);
													}
												}
												dialog.dismiss();
											}
										})
								.setNegativeButton(android.R.string.cancel,                                
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												dialog.dismiss();
											}
										}).create();
						alertRenameDialog.setOnShowListener(new DialogInterface.OnShowListener() {
							@Override
							public void onShow(DialogInterface dialog) {
								mDialogTextField = (EditText) alertRenameDialog.findViewById(
										R.id.fingerprint_rename_field);
								CharSequence name = mFingerName == null ? mFp.getName() : mFingerName;
								mDialogTextField.setText(name);
								if (mTextHadFocus == null) {
									mDialogTextField.selectAll();
								} else {
									mDialogTextField.setSelection(mTextSelectionStart, mTextSelectionEnd);
								}
							}
						});
						if (mTextHadFocus == null || mTextHadFocus) {
							// Request the IME
							alertRenameDialog.getWindow().setSoftInputMode(
									WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						}
						return alertRenameDialog;
					case DELETE_DIALOG: // delete fingerprint dialog
						final AlertDialog alertDeleteDialog = new AlertDialog.Builder(getActivity())
								.setTitle(getString(R.string.fingerprint_rename_deleteconfirm)+mFp.getName()+getString(R.string.fingerprint_rename_deleteconfirm1))
								.setPositiveButton(
										R.string.security_settings_fingerprint_enroll_dialog_delete,
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												onDeleteClick(dialog);
											}
										})
								.setNegativeButton(android.R.string.cancel,                                
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												dialog.dismiss();
											}
										}).create();
						return alertDeleteDialog;
				}
				return null;
            }
			
			@Override
            public void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
				if (mDialogType == RENAME_DIALOG) {
					if (mDialogTextField != null) {
						outState.putString("fingerName", mDialogTextField.getText().toString());
						outState.putBoolean("textHadFocus", mDialogTextField.hasFocus());
						outState.putInt("startSelection", mDialogTextField.getSelectionStart());
						outState.putInt("endSelection", mDialogTextField.getSelectionEnd());
					}
				}
            }

            private void onDeleteClick(DialogInterface dialog) {
                if (DEBUG) Log.v(TAG, "DeleteDialog Removing fpId=" + mFp.getFingerId());
                MetricsLogger.action(getContext(), MetricsLogger.ACTION_FINGERPRINT_DELETE,
                        mFp.getFingerId());
                AliFingerSinglePreferenceFragment parent
                        = (AliFingerSinglePreferenceFragment) getTargetFragment();
                if (parent.mFingerprintManager.getEnrolledFingerprints().size() > 1) {
                    parent.deleteFingerPrint(mFp);
                } else {
                    ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                    Bundle args = new Bundle();
                    args.putParcelable("fingerprint", mFp);
                    lastDeleteDialog.setArguments(args);
                    lastDeleteDialog.setTargetFragment(getTargetFragment(), 0);
                    lastDeleteDialog.show(getFragmentManager(),
                            ConfirmLastDeleteDialog.class.getName());
                }
                dialog.dismiss();
            }
        }

        public static class ConfirmLastDeleteDialog extends DialogFragment {

            private Fingerprint mFp;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.fingerprint_last_delete_title)
                        .setMessage(R.string.fingerprint_last_delete_message)
                        .setPositiveButton(R.string.fingerprint_last_delete_confirm,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        AliFingerSinglePreferenceFragment parent
                                                = (AliFingerSinglePreferenceFragment) getTargetFragment();
                                        parent.deleteFingerPrint(mFp);
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                return alertDialog;
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
			android.util.Log.i("lcf_finger","onPreferenceClick preference key is " + preference.getKey());
            if(ALI_KEY_FINGEPRINT_NAME.equals(preference.getKey())) {
				showRenameDeleteDialog(RenameDeleteDialog.RENAME_DIALOG);
            } else if (ALI_KEY_FINGERQUICK_STARTAPP.equals(preference.getKey())) {
                startSelectActivity(AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP, false);
            } else if(ALI_KEY_FINGERQUICK_NUMBER.equals(preference.getKey())) {
                startSelectActivity(AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT, false);
            } else if(ALI_KEY_FINGEPRINT_REENROLL.equals(preference.getKey())) {
				android.util.Log.i("lcf_finger","onPreferenceClick ALI_KEY_FINGEPRINT_REENROLL");
                preReenroll();
            } else if(ALI_KEY_FINGEPRINT_DELETE.equals(preference.getKey())) {
				showRenameDeleteDialog(RenameDeleteDialog.DELETE_DIALOG);
            }
            return true;
        }
		
		private void showRenameDeleteDialog(int dialogType) {
			android.util.Log.i("lcf_finger","onPreferenceClick showRenameDeleteDialog dialogType: "+dialogType);
			RenameDeleteDialog renameDeleteDialog = new RenameDeleteDialog(mContext, dialogType);
			Bundle args = new Bundle();
			args.putParcelable("fingerprint", mFingerprint);
			renameDeleteDialog.setArguments(args);
			renameDeleteDialog.setTargetFragment(this, 0);
			renameDeleteDialog.show(getFragmentManager(), RenameDeleteDialog.class.getName());
		}
		
        private void startSelectActivity(int type, boolean delay) {
            mHandler.removeMessages(MSG_START_SELECT_ACTIVITY);
            Message msg = mHandler.obtainMessage(MSG_START_SELECT_ACTIVITY);
            msg.arg1 = type;
            if (delay) {
                mHandler.sendMessageDelayed(msg, 50);
            } else {
                mHandler.sendMessage(msg);
            }
        }		
		
        private void disableFinger() {
            List<Fingerprint> enrolledlists = mFingerprintManager.getEnrolledFingerprints();
            if(enrolledlists == null || enrolledlists.size() < 1) {
                Settings.Global.putInt(getActivity().getContentResolver(),"fingerprint_unlock",AliFingerprintUtils.FINGERPRINT_CLOSE_UNLOCK);
            }
        }
		
        private void preReenroll() {
			deleteFingerPrint(mFingerprint , FINGERPRINT_REENROLL_STATE_RUNNING);
            disableFinger();
        }

		private static final int FINGERPRINT_REENROLL_STATE_STOPPED = 0;
		private static final int FINGERPRINT_REENROLL_STATE_RUNNING = 1;
		
		private int mFingerprintReenrollRunningState = FINGERPRINT_REENROLL_STATE_STOPPED;
		
		private void setFingerprintReenrollRunningState(int fingerprintReenrollRunningState) {
			mFingerprintReenrollRunningState = fingerprintReenrollRunningState;
		}
		
		private void deleteFingerPrint(Fingerprint fingerPrint , int fingerprintReenrollRunningState) {
			setFingerprintReenrollRunningState(fingerprintReenrollRunningState);
			deleteFingerPrint(fingerPrint);
		}		
		
		private void deleteFingerPrint(Fingerprint fingerPrint) {
			mFingerprintManager.remove(fingerPrint, mRemoveCallback);
		}
		
		private RemovalCallback mRemoveCallback = new RemovalCallback() {
			@Override
			public void onRemovalSucceeded(Fingerprint fingerprint) {
				android.util.Log.d(TAG,"onRemovalSucceeded mFingerprintReenrollRunningState: "+mFingerprintReenrollRunningState); 
				if (mFingerprintReenrollRunningState == FINGERPRINT_REENROLL_STATE_RUNNING) {
					startReenroll();
				} else {
					AliFingerprintUtils.deleteFingerQuickSetting(mContext, mFingerprint.getFingerId());
					getActivity().finish();
				}
			}

			@Override
			public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
				android.util.Log.d(TAG,"onRemovalError");
				showToast(errString);
			}
		};
		
        private void startReenroll() {
			android.util.Log.d(TAG,"startReenroll"); 
            Intent intent = new Intent();
            intent.setClassName("com.android.settings",
                    FingerprintEnrollEnrolling.class.getName());
			android.util.Log.d(TAG,"startReenroll mToken == null is "+(mToken==null)); 
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
			intent.putExtra(FingerprintEnrollBase.FINGERPRINT_REENROLL_NAME,mFingerprint.getName().toString());
            AliFingerSinglePreferenceFragment.this.startActivityForResult(intent, FINGERQUICK_RETURNCODE_REENROLL);
        }
		
        public void onActivityResult (int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.i(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
            if(requestCode == FINGERQUICK_RETURNCODE_SELECT_APP) {
                mSelectActivityShowing = false;
                if(data == null) {
                    return;
                }
                if(!data.hasExtra("packageName")){
                    return;
                }
                mTargetAppFromUser = data.getStringExtra("packageName");
                mTargetActivityFromUser = data.getStringExtra("activityName");
                syncDatatoDB();
            } else if(requestCode == FINGERQUICK_RETURNCODE_SELECT_CONTACT) {
                mSelectActivityShowing = false;
                if (resultCode == RESULT_OK) {
                    if(data == null) {
                        return;
                    }
                    Uri contactUri = data.getData();
                    String phoneNumber = null;
                    Cursor dataCursor = null;
                    try {
                        dataCursor = mContext.getContentResolver().query(contactUri, new String[] { Data.DATA1 }, null, null, null);
                        if (dataCursor != null && dataCursor.moveToFirst()) {
                            phoneNumber = dataCursor.getString(0);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "dataCursor get exception", e);
                    } finally {
                        if (dataCursor != null) {
                            dataCursor.close();
                        }
                    }
                    if(phoneNumber != null && !phoneNumber.isEmpty()) {
                        mTargetNumberFromUser = phoneNumber;
                        updateView();
                        syncDatatoDB();
                    }
                }
            } else if(requestCode == FINGERQUICK_RETURNCODE_REENROLL) {
				setFingerprintReenrollRunningState(FINGERPRINT_REENROLL_STATE_STOPPED);
         	    if (resultCode == 1) {
                    android.util.Log.e(TAG, "FINGERQUICK_RETURNCODE_REENROLL OK");
                } else {
					AliFingerprintUtils.deleteFingerQuickSetting(mContext, mFingerprint.getFingerId());
                   android.util.Log.e(TAG, "FINGERQUICK_RETURNCODE_REENROLL NOK");
          	    }
				getActivity().finish();
          	}
        }
		
        private void initDataFromDB() {
            Cursor cursor = AliFingerprintUtils.getFingerQuickSetting(mContext, mFingerprint.getFingerId());
            if(cursor == null) {
                mOperationFromDB = AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE;
                mTargetFromDB = "";
                mDataFromDB = "";
            } else {
                mOperationFromDB = cursor.getInt(cursor.getColumnIndex(AliFingerprintUtils.COLUMN_OPERATION));
                mTargetFromDB = cursor.getString(cursor.getColumnIndex(AliFingerprintUtils.COLUMN_TARGET));
                mDataFromDB = cursor.getString(cursor.getColumnIndex(AliFingerprintUtils.COLUMN_DATA));
                cursor.close();
            }
            mOperationFromUser = mOperationFromDB;
            mTargetNumberFromUser = null;
            mTargetAppFromUser = null;
            mTargetActivityFromUser = null;
            if(mOperationFromUser == AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT) {
                mTargetNumberFromUser = mTargetFromDB;
            } else if(mOperationFromUser == AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP) {
                mTargetAppFromUser = mTargetFromDB;
                mTargetActivityFromUser = mDataFromDB;
            }
        }
		
        /*sync data to db, and try to enable fingerprint unlock screen*/
        private void syncDatatoDB() {
            if(mOperationFromDB == mOperationFromUser) {
                if(mOperationFromDB == AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE
                        || (mOperationFromDB == AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT && mTargetFromDB == mTargetNumberFromUser)
                        || (mOperationFromDB == AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP && mTargetFromDB == mTargetAppFromUser && mDataFromDB == mTargetActivityFromUser)) {
                    return;
                }
            }
            mOperationFromDB = mOperationFromUser;
            switch(mOperationFromUser) {
                case AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE:
                    mTargetFromDB = "";
                    mDataFromDB = "";
                    break;
                case AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT:
                    mTargetFromDB = (mTargetNumberFromUser == null) ? "" : mTargetNumberFromUser;
                    break;
                case AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP:
                    mTargetFromDB = (mTargetAppFromUser == null) ? "" : mTargetAppFromUser;
                    mDataFromDB = (mTargetActivityFromUser == null) ? "" : mTargetActivityFromUser;
                    break;
            }
            AliFingerprintUtils.setFingerQuickSetting(mContext, mFingerprint.getFingerId(), mOperationFromDB, mTargetFromDB, mDataFromDB);
            checkAndEnableFingerprintUnlockScreen();
        }
		
        private void checkAndEnableFingerprintUnlockScreen() {
            if(mOperationFromDB != AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE
                && Settings.Global.getInt(getActivity().getContentResolver(), "fingerprint_unlock",0) != 1) {
                Settings.Global.putInt(getActivity().getContentResolver(),"fingerprint_unlock", 1);
                mFingerquickTargetCate.removePreference(mFingerquickUnlockTip);
            }
        }		
		
        /*update summary according to setting*/
        private void updateView() {
			android.util.Log.e(TAG, "updateView mOperationFromUser: "+mOperationFromUser);
            if(mOperationFromUser == AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT) {
                if(mTargetNumberFromUser == null || mTargetNumberFromUser.isEmpty()) {
                    mFingerquickNumberPref.setSummary(getString(R.string.data_usage_not_set));
                } else {
                    String displayName = AliFingerprintUtils.getContactNameByPhoneNumber(mContext, mTargetNumberFromUser);
                    if(displayName != null && !displayName.isEmpty()) {
                        mFingerquickNumberPref.setSummary(mTargetNumberFromUser + "(" + displayName + ")");
                    } else {
                        mFingerquickNumberPref.setSummary(mTargetNumberFromUser);
                    }
                }

            } else if(mOperationFromUser == AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP) {
                String appName = AliFingerprintUtils.getApplicationName(mContext, mTargetAppFromUser, mTargetActivityFromUser);
                mFingerquickStartAppPref.setSummary((appName != null) ? appName : getString(R.string.data_usage_not_set));
            }

            if(mOperationFromUser != AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE && Settings.Global.getInt(getActivity().getContentResolver(), "fingerprint_unlock",0) != 1) {
                mFingerquickTargetCate.addPreference(mFingerquickUnlockTip);
            } else {
                mFingerquickTargetCate.removePreference(mFingerquickUnlockTip);
            }
        }		

        @Override
        public boolean onPreferenceChange(Preference preference, Object type) {
             if (ALI_KEY_FINGERQUICK_TYPE.equals(preference.getKey())) {
                if(type == null) return true;
                Log.e(TAG, "onPreferenceChange return type " + type);
                if("2".equals(type)) {
                    mOperationFromUser = AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE;
                    mHandler.removeMessages(MSG_START_SELECT_ACTIVITY);
                } else if("0".equals(type)) {
                    mOperationFromUser = AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT;
                    startSelectActivity(AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT, true);
                } else if("1".equals(type)) {
                    mOperationFromUser = AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP;
                    startSelectActivity(AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP, true);
                }
                if(mOperationFromUser == AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE) {
                    switchPreferenceDisplay(mOperationFromUser);//if switch to dial or startapp, jump to select activity, do not need switch
                    syncDatatoDB();
                 }
             } else if(ALI_KEY_FINGERQUICK_NUMBER.equals(preference.getKey())) {
             } else if(ALI_KEY_FINGERQUICK_STARTAPP.equals(preference.getKey())) {
             }
            return true;
        }
		
        private void switchPreferenceDisplay(int state) {
            switch(state) {
                case AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE:
                    mFingerquickTypePref.setSummary(getString(R.string.fingerprint_click_setting));
                    mFingerquickTargetCate.removePreference(mFingerquickNumberPref);
                    mFingerquickTargetCate.removePreference(mFingerquickStartAppPref);
                    break;
                case AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT:
                    mFingerquickTypePref.setSummary(mFingerquickTypeList[state]);
                    mFingerquickTargetCate.removePreference(mFingerquickStartAppPref);
                    mFingerquickTargetCate.addPreference(mFingerquickNumberPref);
                    break;
                case AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP:
                    mFingerquickTypePref.setSummary(mFingerquickTypeList[state]);
                    mFingerquickTargetCate.removePreference(mFingerquickNumberPref);
                    mFingerquickTargetCate.addPreference(mFingerquickStartAppPref);
                    break;
                default:
                    break;
            }
            updateView();
        }		
		
        private void renameFingerPrint(int fingerId, String newName) {			
			mFingerprintManager.rename(mFingerprint.getFingerId(), newName);
			mFingerprintNamePref.setTitle(newName);
			updateFingerprint(mFingerprint);
			((AliFingerSingleSetting)getActivity()).setTitle(newName);
        }
		
        private boolean isFingerNameAvailabled(String fingerName) {
            if (null != mFingerprintManager && null != fingerName && !fingerName.equals("")) {
                final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
                final int fingerprintCount = items.size();
                for (int i = 0; i < fingerprintCount; i++) {
                    final Fingerprint item = items.get(i);
                    if (fingerName.equals(item.getName())) {
                        return false;
                    }
                }
            } else {
				return false;
			}
            return true;
        }
        private void updateFingerprint(Fingerprint fingerprint) {
            if (null != mFingerprintManager) {
                final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
                final int fingerprintCount = items.size();
                for (int i = 0; i < fingerprintCount; i++) {
                    final Fingerprint item = items.get(i);
                    if (item.getFingerId() == mFingerprint.getFingerId()) {
                        mFingerprint = item;
                    }
                }
            }
        }		
		
		@Override
		protected int getMetricsCategory() {
			return MetricsLogger.FINGERPRINT;
		}
		
		Toast mToast = null;
		public void showToast(String text) {
			if (mToast == null) {
				mToast = Toast.makeText(mContext, text,Toast.LENGTH_SHORT);
			} else {
				mToast.setText(text);  
				mToast.setDuration(Toast.LENGTH_SHORT);
			}
			mToast.show();
		}
		
		public void showToast(int stringID) {
			String text = getString(stringID);
			showToast(text);
		}
		
		public void showToast(CharSequence str) {
			showToast(str.toString());
		}
		
        @Override
        public void onDestroy() {
            super.onDestroy();
        }		
}