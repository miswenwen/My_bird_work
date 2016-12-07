package com.android.deskclock;

import hwdroid.app.HWActivity;
import hwdroid.widget.ActionBar.ActionBarView;
import hwdroid.widget.ActionBar.ActionBarView.OnLeftWidgetItemClick;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import hwdroid.widget.ItemCursorAdapter;
import hwdroid.widget.FooterBar.FooterBarButton;
import hwdroid.widget.FooterBar.FooterBarType.OnFooterItemClick;
import hwdroid.widget.item.Item;
import hwdroid.widget.item.Text2Item;
import hwdroid.widget.item.Item.Type;
import hwdroid.widget.itemview.ItemView;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.android.deskclock.R;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.holiday.HolidayParser;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.widget.sgv.OverScrollerSGV;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CityObj;

import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.hardware.Camera.Size;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;

public class SetAlarmActivity extends HWActivity {

    private static final String KEY_RINGTONE_TITLE_CACHE = "ringtoneTitleCache";
    private static final String KEY_REPEAT_STATE = "repeatState";
    private static final String KEY_REPEAT_CHECKED = "repeatChecked";
    private static final String KEY_UPDATE_ALARM = "updatealarm";
    private static final int REQUEST_CODE_RINGTONE = 1;
    private boolean mIsSwitchChecked = true;
    private FooterBarButton mFooterBarButton;
    private int mButtonItemId = 0;
    private Alarm mAlarmReturned = null;

    private String mOK ;
    private String mCancel;
    private Context mContext;
    private SetGroupView mSetAlarmPage;
    public Bundle mRingtoneTitleCache;
    public boolean mFromCancel;
    private boolean isUpdatingAlarm = false;
    private Handler mHandler = new Handler();
    private boolean isSaveBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setActivityContentView(R.layout.set_alarm_activity);
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        if (null != savedInstanceState) {
            mRingtoneTitleCache = savedInstanceState.getBundle(KEY_RINGTONE_TITLE_CACHE);
       }

        if (mRingtoneTitleCache == null) {
            mRingtoneTitleCache = new Bundle();
        }
        registerReceiver(homePressReceiver, homeFilter);
        showBackKey(true);
        setTitle2(getResources().getString(R.string.set_alarm_title));
        Alarm alarmData = (Alarm)getIntent().getParcelableExtra("alarm_data");
        mRingtoneTitleCache = getIntent().getParcelableExtra("ringtone_cache");
        mFromCancel = getIntent().getBooleanExtra("from_cancel", false);
        mSetAlarmPage = (SetGroupView)findViewById(R.id.fragment_set_alarm);
        mSetAlarmPage.setFragment(this,mHandler,alarmData);
        if (null != savedInstanceState) {
            mSetAlarmPage.mDaysOfWeek_bak = (DaysOfWeek)savedInstanceState.getParcelable(KEY_REPEAT_STATE);
            mSetAlarmPage.mRepeatChecked_bak = (boolean)savedInstanceState.getBoolean(KEY_REPEAT_CHECKED);
            isUpdatingAlarm = (boolean)savedInstanceState.getBoolean(KEY_REPEAT_CHECKED);
            mSetAlarmPage.recoverRepeatView(alarmData,mHandler);
        }
        //mSetAlarmPage.resetView();
        if (null != alarmData && null == savedInstanceState) {
            isUpdatingAlarm = true;
            mSetAlarmPage.setView(alarmData,mHandler);
            mAlarmReturned = alarmData;
        }
        mSetAlarmPage.updateTimePicker();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        systemBarManager.setViewFitsSystemWindows(this, true);
//liuqipeng begin
        //systemBarManager.setStatusBarColor(mContext.getResources().getColor(R.color.alarm_clock_bg));
		systemBarManager.setStatusBarColor(mContext.getResources().getColor(R.color.alarmclock_new_bg));	
//liuqipeng

        ImageView mCancelBtn = new ImageView(this);
        mCancelBtn.setImageResource(R.drawable.ic_cancel);
        this.setLeftWidgetView(mCancelBtn);
        this.setLeftWidgetClickListener(new OnLeftWidgetItemClick() {
            @Override
            public void onLeftWidgetItemClick() {
                cancelAddNewAlarm();
                finish();
            }
        });
        ImageView mDoneBtn = new ImageView(this);
        mDoneBtn.setImageResource(R.drawable.ic_done);
        this.setRightWidgetView(mDoneBtn);
        this.setRightWidgetClickListener(new OnRightWidgetItemClick() {
            @Override
            public void onRightWidgetItemClick() {
				Log.i("ClockDataStorage","you clicked RightDoneBtn");//liuqipeng add
                addNewAlarm();	
                finish();
            }
        });
        ActionBarView actionBarView = this.getActionBarView();
        if (null != actionBarView) {
            actionBarView.setTitleColor(getResources().getColor(
                    R.color.header_title_color));
            if(isUpdatingAlarm){
                actionBarView.setTitle(getResources().getString(R.string.edit_alarm_title));
            }else{
                actionBarView.setTitle(getResources().getString(R.string.set_alarm_title));
            }
        }
//liuqipeng begin	
        //this.setActionBarBackgroudResource(R.color.alarm_clock_bg);
		this.setActionBarBackgroudResource(R.color.alarmclock_new_bg);
//liuqipeng
    }

    @Override
    public void onPause() {
        super.onPause();
        //TA.getInstance().getDefaultTracker().pageLeave("Page_AddAlarms_leave");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSetAlarmPage != null) {
            mHandler.removeCallbacks(mSetAlarmPage.mTimeUpdateThread);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (null == TA.getInstance() || null == TA.getInstance().getDefaultTracker()) {
            StatConfig.getInstance().setContext(this.getApplicationContext());
            StatConfig.getInstance().turnOnDebug();
            Tracker lTracker = TA.getInstance().getTracker("21736479");
            lTracker.setAppKey("21736479");
            TA.getInstance().setDefaultTracker(lTracker);
        }
        if (mSetAlarmPage != null) {
            mHandler.removeCallbacks(mSetAlarmPage.mTimeUpdateThread);
            mHandler.post(mSetAlarmPage.mTimeUpdateThread);
        }
        //TA.getInstance().getDefaultTracker().pageEnter("Page_AddAlarms");
    }

    private void setReturnAlarm () {
        if(mSetAlarmPage.mCheckBox_repeat.isChecked()&&mSetAlarmPage.mCheckBox_legal.isChecked()){
            for (int i = 0; i < 7; i++) {
                int day = mSetAlarmPage.DAY_ORDER[i];
                mSetAlarmPage.mDaysOfWeek.setDaysOfWeek(true, day);
                mSetAlarmPage.mDaysOfWeek.mBitSet |= (1 << 7);
            }
        }
        else if(mSetAlarmPage.mCheckBox_repeat.isChecked())
        {
            mSetAlarmPage.mDaysOfWeek.mBitSet &= ~(1 << 7);
            for (int i = 0; i < 7; i++) {
                final boolean checked = mSetAlarmPage.repeatDaysArray[i];
                int day = mSetAlarmPage.DAY_ORDER[i];
                mSetAlarmPage.mDaysOfWeek.setDaysOfWeek(checked, day);
            }
        }
        else{
                mSetAlarmPage.mDaysOfWeek.mBitSet &= ~(1 << 7);
                for (int i = 0; i < 7; i++) {
                int day = mSetAlarmPage.DAY_ORDER[i];
                mSetAlarmPage.mDaysOfWeek.setDaysOfWeek(false, day);
            }
        }
        Log.e("AlarmClock", "mSetAlarmPage.mDaysOfWeek.getBitSet="+mSetAlarmPage.mDaysOfWeek.getBitSet());
        int h = mSetAlarmPage.mHour;
        
        /*if (!mSetAlarmPage.mSetAlarmView.isAm) {
            if (h == 12) {
                h = 12;
            } else {
                h = h+12;
            }
        } else {
            if (h == 12) {
                h = 0;
            }
        }*/
        mAlarmReturned = setNewAlarm(mSetAlarmPage.mAlarm, h, mSetAlarmPage.mMin, mSetAlarmPage.mAlert,
                mSetAlarmPage.mVibrate,
                mSetAlarmPage.mDaysOfWeek,
                mSetAlarmPage.mTitleText.getText().toString());
    }


    private void saveRepeatState(){
        if(mSetAlarmPage.mCheckBox_repeat.isChecked()&&mSetAlarmPage.mCheckBox_legal.isChecked()){
            for (int i = 0; i < 7; i++) {
                int day = mSetAlarmPage.DAY_ORDER[i];
                mSetAlarmPage.mDaysOfWeek_bak.setDaysOfWeek(true, day);
                mSetAlarmPage.mDaysOfWeek_bak.mBitSet |= (1 << 7);
            }
        }
        else if(mSetAlarmPage.mCheckBox_repeat.isChecked())
        {
            mSetAlarmPage.mDaysOfWeek_bak.mBitSet &= ~(1 << 7);
            for (int i = 0; i < 7; i++) {
                final boolean checked = mSetAlarmPage.repeatDaysArray[i];
                int day = mSetAlarmPage.DAY_ORDER[i];
                mSetAlarmPage.mDaysOfWeek_bak.setDaysOfWeek(checked, day);
            }
        }
        else{
                mSetAlarmPage.mDaysOfWeek_bak.mBitSet &= ~(1 << 7);
                for (int i = 0; i < 7; i++) {
                int day = mSetAlarmPage.DAY_ORDER[i];
                mSetAlarmPage.mDaysOfWeek_bak.setDaysOfWeek(false, day);
            }
        }
        Log.e("AlarmClock", "mSetAlarmPage.mDaysOfWeek_bak.getBitSet="+mSetAlarmPage.mDaysOfWeek_bak.getBitSet());
    }

    private void addNewAlarm () {
        setReturnAlarm();
        //FIX ME , create alarm with this data
        Intent alarmChangeIntent = new Intent();
        Bundle dataChangeBundle = new Bundle();

        HashMap<String, String> lMaps = new HashMap<String, String>();
        String alarmtime = String.valueOf(mAlarmReturned.hour)+":"+String.valueOf(mAlarmReturned.minutes);
        lMaps.put("alarm_time", alarmtime);

        String alarmrepeat = mAlarmReturned.daysOfWeek.toString(mContext, true);
        lMaps.put("alarm_repeat", alarmrepeat);
        String alarmalert = mAlarmReturned.alert.toString();
        lMaps.put("alarm_alert", alarmalert);
        String alarmvibrate = String.valueOf(mAlarmReturned.vibrate);
        lMaps.put("alarm_vibrate", alarmvibrate);
        String alarmlabel = mAlarmReturned.label;
        lMaps.put("alarm_label", alarmlabel);
        String alarmtitle = mSetAlarmPage.mRingSelectText.getText().toString();//mSetAlarmPage.getRingToneTitle(mAlarmReturned.alert);
        lMaps.put("alarm_title", alarmtitle);
        //lMaps.put("from_xiaoyun", "false");
        TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment", 19999, "Page_AlarmClockFragment_New_Alarm_Details", null, null, lMaps);
        Log.i("AlarmClock", "alarmtime = "+alarmtime+" alarm_repeat ="+alarmrepeat+" alarm_alert="+alarmalert
            +" alarm_vibrate =" + alarmvibrate + " alarm_label =" +alarmlabel + "alarm_title ="+alarmtitle);
		Log.i("ClockDataStorage","alarmtime = "+alarmtime+" alarm_repeat ="+alarmrepeat+" alarm_alert="+alarmalert
            +" alarm_vibrate =" + alarmvibrate + " alarm_label =" +alarmlabel + "alarm_title ="+alarmtitle);//liuqipeng add

        dataChangeBundle.putParcelable("alarm_changed", mAlarmReturned);
        dataChangeBundle.putParcelable("ringtone_cache", mRingtoneTitleCache);
		Log.i("ClockDataStorage","dataChangeBundle add data with key---alarm_changed,key---ringtone_cache");//liuqipeng add
        alarmChangeIntent.putExtras(dataChangeBundle);
		Log.i("ClockDataStorage","alarmChangeIntent add dataChangeBundle");//liuqipeng add
        ((Activity)mContext).setResult(Activity.RESULT_OK, alarmChangeIntent);
		Log.i("ClockDataStorage","return to last Activity(DeskClock) with Data in alarmChangeIntent");//liuqipeng add
    }

    private void cancelAddNewAlarm () {
        setReturnAlarm();
        //FIX ME , create alarm with this data
        Intent alarmChangeIntent = new Intent();
        Bundle dataChangeBundle = new Bundle();

        dataChangeBundle.putParcelable("ringtone_cache", mRingtoneTitleCache);
        alarmChangeIntent.putExtras(dataChangeBundle);
        ((Activity)mContext).setResult(Activity.RESULT_CANCELED, alarmChangeIntent);
    }

    public Alarm setNewAlarm(Alarm alarm, int hourOfDay, int minute, Uri alert, boolean vibrate, DaysOfWeek daysOfWeek, String label) {
        Alarm a = new Alarm();
        if (alarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            Log.i("AlarmClock", "alarm is null in setNewAlarm.");
            a.alert = alert;

            if (a.alert == null) {
                a.alert = RingtoneManager.getActualDefaultRingtoneUri(mContext,RingtoneManager.TYPE_ALARM);
            }
            if (a.alert == null) {
                a.alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
            if (a.alert == null) {
                a.alert = Uri.parse("content://settings/system/alarm_alert");
            }
            a.hour = hourOfDay;
            a.minutes = minute;
            a.vibrate = vibrate;
            a.label = label;
            a.daysOfWeek = daysOfWeek;
            a.enabled = true;
            return a;
        } else {
            Log.i("AlarmClock", "alarm is not null in setNewAlarm.");
            alarm.hour = hourOfDay;
            alarm.minutes = minute;
            alarm.alert = alert;
            alarm.vibrate = vibrate;
            alarm.label = label;
            alarm.daysOfWeek = daysOfWeek;
            alarm.enabled = true;
            a = alarm;
        }
        return a;
    }

    private static AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver cr = context.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance(),context);
        newInstance = AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    private final BroadcastReceiver homePressReceiver = new BroadcastReceiver() {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason != null && reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                    if (isUpdatingAlarm) {
                        setReturnAlarm();
                        ContentResolver cr = mContext.getContentResolver();

                        // Dismiss all old instances
                        AlarmStateManager.deleteAllInstances(mContext, mAlarmReturned.id);

                        // Update alarm
                        Alarm.updateAlarm(cr, mAlarmReturned);
                        if (mAlarmReturned.enabled) {
                            setupAlarmInstance(mContext, mAlarmReturned);
                        }
                    }
                    finish();
                }
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
     Log.e("AlarmClock", "onSaveInstanceState to ringtone picker");
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_RINGTONE_TITLE_CACHE, mRingtoneTitleCache);
        saveRepeatState();
        outState.putParcelable(KEY_REPEAT_STATE, mSetAlarmPage.mDaysOfWeek_bak);
        outState.putBoolean(KEY_REPEAT_CHECKED, mSetAlarmPage.mCheckBox_repeat.isChecked());
        outState.putBoolean(KEY_UPDATE_ALARM, isUpdatingAlarm);
    }

    @Override
    protected void onDestroy() {
        if(homePressReceiver != null){
            try {
                unregisterReceiver(homePressReceiver);
            } catch (Exception e) {
                Log.e("AlarmClock", "unregisterReceiver error reason:" + e);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_RINGTONE:
                    mSetAlarmPage.mRingtoneItem.setClickable(true);
                    mSetAlarmPage.mRingSelectText.setClickable(true);
                    mSetAlarmPage.saveRingtoneUri(data, mRingtoneTitleCache);
                    break;
                default:
                    Log.w("AlarmClock", "Unhandled request code in onActivityResult: " + requestCode);
                    break;
            }
        }
        else if(resultCode == Activity.RESULT_CANCELED){
            switch (requestCode) {
            case REQUEST_CODE_RINGTONE:
                mSetAlarmPage.mRingtoneItem.setClickable(true);
                mSetAlarmPage.mRingSelectText.setClickable(true);
                if(mSetAlarmPage.mFromCloudAlarm){
                    Log.w("AlarmClock", "mSetAlarmPage.mFromCloudAlarm");
                    mSetAlarmPage.saveNoRingtoneUri(data, mRingtoneTitleCache);
                    mSetAlarmPage.mFromCloudAlarm = false;
                }
                Log.w("AlarmClock", "mSetAlarmPage.mAlert =" +mSetAlarmPage.mAlert.toString());
                break;
            default:
                Log.w("AlarmClock", "Unhandled request code in onActivityResult: " + requestCode);
                break;
            }
        }
     }
}
