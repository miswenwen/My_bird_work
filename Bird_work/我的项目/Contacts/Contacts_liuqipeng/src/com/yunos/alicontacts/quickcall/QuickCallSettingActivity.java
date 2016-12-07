
package com.yunos.alicontacts.quickcall;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yunos.alicontacts.ContactPhotoManager;
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.ContactsTextUtils;
import com.yunos.common.UsageReporter;

import hwdroid.widget.ActionSheet;

import java.util.ArrayList;
import java.util.List;

public class QuickCallSettingActivity extends BaseActivity {

    public static final String TAG = "QucikCallSettingActivity";

    private GridView mGridView;
    private CallNumberAdapter mAdapter;
    private int mCurrKeyCode;

    private static final int DELETE_QUCIK_NUMBER = 0;
    private static final int EDIT_QUICK_NUMBER = 1;

    private static final int REQ_CODE_PICK_CONTACT = 1200;

    private QuickCallSetting mQuickCallSetting;
    private boolean mIsMultiSim;
    private Thread mUpdateThread;
    private volatile boolean mStopped;
    private boolean mWaitingForPick = false;

    private SparseArray<Pair<String, String>> mKeyMap = new SparseArray<Pair<String, String>>(9);

    private AlertDialog mQuickCallSettingDialog;

    private static final int[] mImageRes = new int[] {
            R.drawable.ic_quickdail_nub1, R.drawable.ic_quickdail_nub2,
            R.drawable.ic_quickdail_nub3, R.drawable.ic_quickdail_nub4,
            R.drawable.ic_quickdail_nub5, R.drawable.ic_quickdail_nub6,
            R.drawable.ic_quickdail_nub7, R.drawable.ic_quickdail_nub8,
            R.drawable.ic_quickdail_nub9
    };

    private static final int[] mKeyCodeArray = new int[] {
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9
    };

    private List<String> mMenuItems;

    private static class ViewHolder {
        TextView quickCallPosView;
        ImageView defaultSim;
        ImageView portraitView;
        TextView portraitTextView;
        TextView quickCallName;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle2(getString(R.string.quick_call_setting));
        setActivityContentView(R.layout.ali_quick_call);
        showBackKey(true);

        mQuickCallSetting = QuickCallSetting.getQuickCallInstance(this);
        mMenuItems = new ArrayList<String>();
        mMenuItems.add(getString(R.string.menu_deleteContact));
        mMenuItems.add(getString(R.string.menu_editContact));

        mIsMultiSim = SimUtil.MULTISIM_ENABLE;

        mAdapter = new CallNumberAdapter(this);
        mAdapter.setPhotoLoader(ContactPhotoManager.getInstance(this));
        mGridView = (GridView) findViewById(R.id.quick_call_gridview);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(mGridViewItemClick);
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < mKeyCodeArray.length; i++) {
                if (mStopped) {
                    return;
                }
                String number = mQuickCallSetting.getPhoneNumber(mKeyCodeArray[i]);
                if (!TextUtils.isEmpty(number)) {
                    Cursor phoneCursor = null;
                    Uri phoneUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                            Uri.encode(number));
                    try {
                        phoneCursor = getContentResolver().query(phoneUri, new String[] {
                                Phone.DISPLAY_NAME, Contacts.PHOTO_URI
                        }, null, null, null);

                        synchronized (mKeyMap) {
                            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                                Pair<String, String> value = Pair.create("", "");
                                value = Pair.create(phoneCursor.getString(0),
                                        phoneCursor.getString(1));
                                mKeyMap.put(mKeyCodeArray[i], value);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "phone query exception", e);
                    } finally {
                        if (phoneCursor != null) {
                            phoneCursor.close();
                        }
                    }
                }
            }
            QuickCallSettingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mAdapter != null) {
                        mAdapter.updateItem();
                    }
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mStopped = false;
        // After pick one contact need to refresh
        mUpdateThread = new Thread(mRunnable);
        mUpdateThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mStopped = true;
        if (mUpdateThread != null && mUpdateThread.isAlive()) {
            mUpdateThread.interrupt();
            mUpdateThread = null;
        }
    }

    private void showDialog() {
        String title = getString(R.string.quick_call_edit_title,(mCurrKeyCode - KeyEvent.KEYCODE_0));
        ActionSheet actionSheet = new ActionSheet(this);
        actionSheet.setOutsideTouchable(true);
        actionSheet.setTitle(title);
        actionSheet.setCommonButtons(mMenuItems, null, null, new ActionSheet.CommonButtonListener() {

            @Override
            public void onDismiss(ActionSheet arg0) {
            }

            @Override
            public void onClick(int which) {
                switch (which) {
                    case DELETE_QUCIK_NUMBER:
                        mQuickCallSetting.deleteQuickDialSetting(mCurrKeyCode);
                        synchronized (mKeyMap) {
                            mKeyMap.remove(mCurrKeyCode);
                        }
                        String confirmationMsg = getString(R.string.quick_call_deleteConfirmation,
                                String.valueOf(mCurrKeyCode - KeyEvent.KEYCODE_0));
                        Toast toast = Toast.makeText(getApplicationContext(), confirmationMsg,
                                Toast.LENGTH_SHORT);
                        toast.show();

                        mAdapter.updateItem();
                        UsageReporter.onClick(null, TAG, UsageReporter.ContactsSettingsPage.QUICK_CALL_DELETE);
                        break;

                    case EDIT_QUICK_NUMBER:
                      //  pickContact();
                       showQuickCallSettingChooseDialog();
                        UsageReporter.onClick(null, TAG, UsageReporter.ContactsSettingsPage.QUICK_CALL_MODIFY);
                        break;

                    default:
                        break;
                }
            }
        });
        actionSheet.show(mGridView);
    }

    private void pickContact(int picmethod) {
        if (mWaitingForPick) {
            Log.i(TAG, "pickContact: waiting for previous pick, ignore.");
            return;
        }
        mWaitingForPick = true;
        Intent intent = new Intent(this, QuickCallPickerActivity.class);
        intent.putExtra(QuickCallSetting.PICMETHOD, picmethod);
        intent.putExtra(QuickCallSetting.EXTRAPOS, mCurrKeyCode);
        startActivityForResult(intent, REQ_CODE_PICK_CONTACT);
    }

    private void showQuickCallSettingChooseDialog() {

        String[] chooseItems = {
                getResources().getString(R.string.pick_exit_contacts),
                getResources().getString(R.string.pick_edit_contacts)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        String msg = getResources().getString(R.string.quick_call_setting_tip,
                mCurrKeyCode - KeyEvent.KEYCODE_0);
        builder.setTitle(msg);
        builder.setItems(chooseItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                pickContact(which);
            }
        });
        builder.setNegativeButton(null);
        mQuickCallSettingDialog = builder.create();
        mQuickCallSettingDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mWaitingForPick = false;
    }

    private OnItemClickListener mGridViewItemClick = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mCurrKeyCode = ((CallNumberAdapter) parent.getAdapter()).getItem(position);
            String phoneNum = mQuickCallSetting.getPhoneNumber(mCurrKeyCode);
            if (TextUtils.isEmpty(phoneNum)) {
                /* YUNOS BEGIN PB */
                // ##email:caixiang.zcx@alibaba-inc.com
                // ##BugID:(5883538) ##date:2015/05/26
                // ##description:add edit contact function when set the quick call(QuickCallSettingActivity)
                // pickContact();
                showQuickCallSettingChooseDialog();
                /* YUNOS END PB */
            } else {
                showDialog();
            }
        }
    };

    private class CallNumberAdapter extends BaseAdapter {
        private Context mContext;
        private ContactPhotoManager mPhotoManager;

        public CallNumberAdapter(Context context) {
            mContext = context;
        }

        public void setPhotoLoader(ContactPhotoManager photoLoader) {
            mPhotoManager = photoLoader;
        }

        public void updateItem() {
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mKeyCodeArray.length;
        }

        @Override
        public Integer getItem(int position) {
            return mKeyCodeArray[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.ali_quick_call_item,
                        parent, false);
                holder.quickCallPosView = (TextView) convertView
                        .findViewById(R.id.quick_contact_label);
                holder.defaultSim = (ImageView) convertView
                        .findViewById(R.id.quick_contact_sim_icon);
                holder.portraitView = (ImageView) convertView
                        .findViewById(R.id.quick_contact_portrait);
                holder.portraitTextView = (TextView) convertView
                        .findViewById(R.id.quick_contact_portrait_txt);
                holder.quickCallName = (TextView) convertView
                        .findViewById(R.id.quick_keyboard_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            int keyBoardNum = mKeyCodeArray[position] - KeyEvent.KEYCODE_0;
            holder.quickCallPosView.setText(String.valueOf(keyBoardNum));

            holder.defaultSim.setVisibility(View.INVISIBLE);

            String phoneNumber = mQuickCallSetting.getPhoneNumber(mKeyCodeArray[position]);
            if (TextUtils.isEmpty(phoneNumber)) {
                holder.quickCallPosView.setVisibility(View.INVISIBLE);
                holder.portraitView.setImageResource(mImageRes[position]);
                holder.quickCallName.setText(mContext.getString(R.string.addquickCall));
                holder.portraitTextView.setVisibility(View.GONE);
            } else {
                holder.quickCallPosView.setVisibility(View.VISIBLE);
                holder.quickCallPosView.setText(String.valueOf(position + 1));
                Pair<String, String> numberAndName;
                synchronized (mKeyMap) {
                    numberAndName = mKeyMap.get(mKeyCodeArray[position]);
                }
                String name = (numberAndName != null) ? numberAndName.first : null;
                if (TextUtils.isEmpty(name)) {
                    name = ContactsUtils.formatPhoneNumberWithCurrentCountryIso(phoneNumber, mContext);
                }
                holder.quickCallName.setText(name);

                String photoString = (numberAndName != null) ? numberAndName.second : null;
                if (TextUtils.isEmpty(photoString)) {
                    String nameStr = ContactsTextUtils.getPortraitText(name);
                    if (!ContactsTextUtils.STRING_EMPTY.equals(nameStr)) {
                        int colorBg = ContactsTextUtils.getColorBg(nameStr);
                        holder.portraitTextView.setText(nameStr);
                        holder.portraitTextView.setVisibility(View.VISIBLE);
                        holder.portraitView.setImageResource(colorBg);
                    } else {
                        holder.portraitView.setImageResource(R.drawable.contact_detail_avatar_border_acquiesce);
                        holder.portraitTextView.setVisibility(View.GONE);
                    }
                } else {
                    Uri photoUri = Uri.parse(photoString);
                    if (mPhotoManager != null) {
                        mPhotoManager.loadDirectoryPhoto(holder.portraitView, photoUri, false);
                    }
                    holder.portraitTextView.setVisibility(View.GONE);
                }
                if (mIsMultiSim) {
                    int defaultsim = mQuickCallSetting
                            .getDefaultQuickDialSim(mKeyCodeArray[position]);
                    if (defaultsim == SimUtil.SLOT_ID_1) {
                        holder.defaultSim.setVisibility(View.VISIBLE);
                        holder.defaultSim.setImageResource(R.drawable.ic_card1_normal);
                    } else if (defaultsim == SimUtil.SLOT_ID_2) {
                        holder.defaultSim.setVisibility(View.VISIBLE);
                        holder.defaultSim.setImageResource(R.drawable.ic_card2_normal);
                    }
                }
            }
            holder.quickCallName.setSelected(true);
            return convertView;
        }
    }
}
