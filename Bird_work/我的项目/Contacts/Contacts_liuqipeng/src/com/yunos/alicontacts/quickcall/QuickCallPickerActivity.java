
package com.yunos.alicontacts.quickcall;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import android.provider.ContactsContract.Data;

import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.activities.ContactEditorActivity;
import com.yunos.alicontacts.sim.SimUtil;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.DialogInterface.OnDismissListener;
import hwdroid.dialog.DialogInterface.OnKeyListener;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class QuickCallPickerActivity extends BaseActivity {
    private static final String TAG = "QuickCallPickerActivity";

    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_INSERT_CONTACT = 2;
    private static final int OPTION_ITEM_SIM_1 = 0;
    private static final int OPTION_ITEM_SIM_2 = 1;

    int mPosition;
    int mPicMethod;
    private boolean mNeedSetDefaultSim;
    private int mDefaultSim = -1;
    private QuickCallSetting mQuickCallSetting;
    private AlertDialog mDialog;
    private AlertDialog mInputPhoneNumberDialog;
    private  EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mQuickCallSetting = QuickCallSetting.getQuickCallInstance(this.getApplicationContext());
        if (SimUtil.MULTISIM_ENABLE) {
            boolean mIsSim1Ready = SimUtil.isSimAvailable(SimUtil.SLOT_ID_1);
            boolean mIsSim2Ready = SimUtil.isSimAvailable(SimUtil.SLOT_ID_2);
            mNeedSetDefaultSim = mIsSim1Ready && mIsSim2Ready;
            if (!mNeedSetDefaultSim) {
                if (mIsSim1Ready)
                    mDefaultSim = SimUtil.SLOT_ID_1;
                if (mIsSim2Ready)
                    mDefaultSim = SimUtil.SLOT_ID_2;
            }
        }

        Bundle b = getIntent().getExtras();
Log.d(TAG,"onCreate b :"+b);
        if (b != null) {
            /* YUNOS BEGIN PB */
            //##email:caixiang.zcx@alibaba-inc.com
            //##BugID:(5883538) ##date:2015/05/11
            //##description:add edit contact function when set the quick call
            mPicMethod = b.getInt(QuickCallSetting.PICMETHOD, 0);
            mPosition = b.getInt(QuickCallSetting.EXTRAPOS, 0);
            if (mPosition >= KeyEvent.KEYCODE_1 && mPosition <= KeyEvent.KEYCODE_9) {
             Log.d(TAG,"mPicMethod:"+mPicMethod);
                if (mPicMethod == 0) {
                    pickContact();
                } else {
                    inputContactPhoneNumberDialog();
	            }
            } else {
                finish();
            }
            /* YUNOS END PB */
        } else {
            finish();
        }
    }

    private void pickContact() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType(Phone.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_CONTACT);
    }

    /* YUNOS BEGIN PB */
    //##email:caixiang.zcx@alibaba-inc.com
    //##BugID:(5883538) ##date:2015/05/11
    //##description:add edit contact function when set the quick call
    private void inputContactPhoneNumberDialog() {
        mEditText = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        String msg = getResources().getString(R.string.input_phone_number_dialog_title);
        builder.setTitle(msg);
        builder.setView(mEditText);
        mEditText.setInputType(InputType.TYPE_CLASS_PHONE);
        // YunOS BEGIN PB
        // ##module:(Contacts)  ##author:shihuai.wg@alibaba-inc.com
        // ##BugID:(6726815)  ##date:2015-12-04
        mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        // YunOS END PB

        builder.setNegativeButton(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                // TODO Auto-generated method stub
                if (mInputPhoneNumberDialog != null) {
                    mInputPhoneNumberDialog.dismiss();
                }
                QuickCallPickerActivity.this.finish();
            }
        });
        builder.setPositiveButton(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                processPhoneNumber(mEditText.getText().toString());
            }
        });
        mInputPhoneNumberDialog = builder.create();
        mInputPhoneNumberDialog.show();

        //#<!-- [[ YunOS BEGIN PB
        //##module:()  ##author:xiuneng.wpf@alibaba-inc.com
        //##BugID:(6649477)  ##date:2015-11-26 12:43 -->
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                QuickCallPickerActivity.this.showKeyboard();
            }
        }, 200);
        //#<!-- YunOS END PB ]] -->

    }

    //#<!-- [[ YunOS BEGIN PB
    //##module:()  ##author:xiuneng.wpf@alibaba-inc.com
    //##BugID:(6649477)  ##date:2015-11-26 12:44 -->
    void showKeyboard() {
        if (mEditText != null) {
            mEditText.setFocusable(true);
            mEditText.setFocusableInTouchMode(true);
            mEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mEditText, 0);
        }
    }
    //#<!-- YunOS END PB ]] -->

    /* YUNOS END PB */

    protected void onActivityResult(int requesetCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requesetCode == REQUEST_CONTACT) {
            Uri contactUri = data.getData();
            String phoneNumber = null;
            Cursor dataCursor = null;
            try {
                dataCursor = getContentResolver().query(contactUri, new String[] {
                    Data.DATA1
                }, null, null, null);
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

            if (!TextUtils.isEmpty(phoneNumber)) {
                if (mQuickCallSetting.hasQuickCallByNumber(phoneNumber, mPosition)) {
                    String confirmationMsg = getApplicationContext().getResources().getString(
                            R.string.quickCall_numberAlreadyExists,
                            ContactsUtils.formatPhoneNumberWithCurrentCountryIso(phoneNumber, getApplicationContext()));
                    Toast toast = Toast.makeText(getApplicationContext(), confirmationMsg,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    mQuickCallSetting.addQuickDialSetting(mPosition, phoneNumber);
                    String confirmationMsg = getApplicationContext().getResources().getString(
                            R.string.quickCall_addConfirmation,
                            ContactsUtils.formatPhoneNumberWithCurrentCountryIso(phoneNumber, getApplicationContext()),
                            String.valueOf(mPosition - KeyEvent.KEYCODE_0));
                    Toast toast = Toast.makeText(getApplicationContext(), confirmationMsg,
                            Toast.LENGTH_SHORT);
                    toast.show();

                    if (mNeedSetDefaultSim) {
                        showChooseSimDialog();
                    } else {
                        mQuickCallSetting.setDefaultQuickDialSim(mPosition, mDefaultSim);
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            }
        } else {
            finish();
        }
    }

    private void processPhoneNumber(String phoneNumber) {
	    if (!TextUtils.isEmpty(phoneNumber)) {
	        if (mQuickCallSetting.hasQuickCallByNumber(phoneNumber, mPosition)) {
	            String confirmationMsg = getApplicationContext().getResources().getString(
	                    R.string.quickCall_numberAlreadyExists,
	                    ContactsUtils.formatPhoneNumberWithCurrentCountryIso(phoneNumber, getApplicationContext()));
	            Toast toast = Toast.makeText(getApplicationContext(), confirmationMsg,
	                    Toast.LENGTH_SHORT);
	            toast.show();
	            setResult(RESULT_OK);
	            finish();
	        } else {
	            mQuickCallSetting.addQuickDialSetting(mPosition, phoneNumber);
	            String confirmationMsg = getApplicationContext().getResources().getString(
	                    R.string.quickCall_addConfirmation,
	                    ContactsUtils.formatPhoneNumberWithCurrentCountryIso(phoneNumber, getApplicationContext()),
	                    String.valueOf(mPosition - KeyEvent.KEYCODE_0));
	            Toast toast = Toast.makeText(getApplicationContext(), confirmationMsg,
	                    Toast.LENGTH_SHORT);
	            toast.show();
	            if (mNeedSetDefaultSim) {
	                showChooseSimDialog();
	            } else {
	                mQuickCallSetting.setDefaultQuickDialSim(mPosition, mDefaultSim);
	                setResult(RESULT_OK);
	                finish();
	            }
	        }
	    }else {
	        QuickCallPickerActivity.this.finish();
	    }
    }

    private void showChooseSimDialog() {
        if (mDialog == null) {
            Context context = getApplicationContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String[] options = new String[2];
            String name1 = SimUtil.getSimCardDisplayName(context, SimUtil.SLOT_ID_1);
            options[0] = (name1 == null) ? "" : name1;
            String name2 = SimUtil.getSimCardDisplayName(context, SimUtil.SLOT_ID_2);
            options[1] = (name2 == null) ? "" : name2;

            ChooseSimDialogAdapter adapter = new ChooseSimDialogAdapter(this, options);
            ListView list = new ListView(this);
            list.setAdapter(adapter);
            list.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int simCard = -1;
                    if (position == 0) {
                        simCard = SimUtil.SLOT_ID_1;
                    } else if (position == 1) {
                        simCard = SimUtil.SLOT_ID_2;
                    }
                    if (simCard != -1) {
                        mQuickCallSetting.setDefaultQuickDialSim(mPosition, simCard);
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }
                    }
                }
            });
            builder.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent keyEvent) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        mQuickCallSetting.setDefaultQuickDialSim(mPosition, -1);
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }
                        QuickCallPickerActivity.this.finish();
                        return true;
                    }
                    return false;
                }
            });
            builder.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface arg0) {
                    mDialog = null;
                    QuickCallPickerActivity.this.finish();
                }
            });
            builder.setView(list);
            mDialog = builder.create();
        }
        String title = getResources().getString(R.string.quick_call_choose_sim,
                String.valueOf(mPosition - KeyEvent.KEYCODE_0));
        mDialog.setTitle(title);
        mDialog.show();
        setResult(RESULT_OK);
    }

    private class ChooseSimDialogAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<ItemTextType> mItems;

        public ChooseSimDialogAdapter(Context context, String[] items) {
            mContext = context;
            if (items != null) {
                int size = items.length;
                mItems = new ArrayList<ItemTextType>(items.length);
                for (int i = 0; i < size; i++) {
                    ItemTextType item = new ItemTextType();
                    item.id = i;
                    item.text = items[i];
                    mItems.add(item);
                }
            }
        }

        public View newView() {
            ViewHolder holder = new ViewHolder();

            View view = View.inflate(mContext, R.layout.create_contact_dialog_item_view_single_line, null);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.name = (TextView) view.findViewById(R.id.name);

            view.setTag(holder);
            return view;
        }

        public View bindView(View view, ItemTextType item) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.name.setText(item.text);
            int id = item.id;
            if (id == OPTION_ITEM_SIM_1) {
                holder.icon.setBackgroundResource(R.drawable.contacts_settings_sim1);
            } else if (id == OPTION_ITEM_SIM_2) {
                holder.icon.setBackgroundResource(R.drawable.contacts_settings_sim2);
            }
            return view;
        }

        @Override
        public int getCount() {
            if (mItems == null) {
                return 0;
            } else {
                return mItems.size();
            }
        }

        @Override
        public Object getItem(int position) {
            if (mItems == null) {
                return null;
            } else {
                return mItems.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = newView();
            }
            bindView(convertView, mItems.get(position));
            return convertView;
        }
    }

    class ViewHolder {
        ImageView icon;
        TextView name;
    }

    class ItemTextType {
        int id;
        String text;
    }
}
