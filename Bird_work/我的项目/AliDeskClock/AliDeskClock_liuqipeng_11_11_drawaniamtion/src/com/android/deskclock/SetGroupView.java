package com.android.deskclock;

import java.io.File;
import java.lang.reflect.Field;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.DaysOfWeek;
import android.view.inputmethod.InputMethodManager;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentTransaction;

import hwdroid.widget.ActionSheet;
import hwdroid.widget.ActionSheet.ActionButton;
import android.text.InputFilter;
import android.widget.CheckBox;
/* YUNOS BEGIN PB */
//Desc:BugID:6275499:optimize app resource by getIdentifier
//use getIdentifier API for get correct resource index
//##Date: 2015-08-04 ##Author:liuyun.lh
import android.content.res.Resources;
/* YUNOS END PB */

public class SetGroupView extends LinearLayout implements TimePickerDialog.OnTimeSetListener, OnTimeChangedListener {
    public class AlarmTimePickerDialog extends TimePickerDialog {

        public AlarmTimePickerDialog(Context context, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
            super(context, callBack, hourOfDay, minute, is24HourView);
            // TODO Auto-generated constructor stub
        }

        public AlarmTimePickerDialog(Context context, int theme, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
            super(context, theme, callBack, hourOfDay, minute, is24HourView);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            // TODO Auto-generated method stub

            int h = hourOfDay;

            if (hourOfDay > 12) {
                h = hourOfDay - 12;
            } else if (hourOfDay == 12) {
                h = hourOfDay;
            } else {
                h = hourOfDay;
            }

            super.onTimeChanged(view, hourOfDay, minute);
            if (DateFormat.is24HourFormat(mContext)) {
                setTitle(((hourOfDay < 10) ? "0" : "") + hourOfDay + ":" + ((minute < 10) ? "0" : "") + minute);
            } else {
                setTitle(h + ":" + ((minute < 10) ? "0" : "") + minute);
            }
        }
    }

    public Context mContext;
    private SetAlarmActivity mSetAlarmActivity;
    public LayoutInflater mFactory;
    public AlarmClockFragment mAlarmFragment;
    public FrameLayout mRingtoneItem;
    private FrameLayout mLabelItem;
    private FrameLayout mVibrateItem;
    public Alarm mAlarm = null;
    public int mHour = 0;
    public int mMin = 0;
    public Uri mAlert;
    public boolean mVibrate = true;
    public DaysOfWeek mDaysOfWeek = new DaysOfWeek(0);
    public DaysOfWeek mDaysOfWeek_bak = new DaysOfWeek(0);
    public boolean mRepeatChecked_bak = false;
    public TextView mRingSelectText;
    public Button mAgreeBtn;
    public Button mCancelBtn;
    public TextView mTimeText;
    public TextView mAm;
    // public Button mPm;
    public TextView mTitleText;
    // public Switch mVibrateSwitch;
    public FrameLayout mRepeatDays;
    public TextView repeatDaysText;
    public boolean setFromTimePickNow = false;
    private Handler mHandler = null;
    private String mTitle = null;
    private TimePicker mTimePicker;
    // private TextView mTimepickertitle;
    private Calendar mCalendar;
    public boolean mInRepeatType;
    public FrameLayout mNumofweek;
    public FrameLayout mLegalWorkday;
    public CheckBox mCheckBox_repeat;
    private CheckBox mCheckBox_vibrate;
    public CheckBox mCheckBox_legal;

    private ArrayList<Button> mButtonList;
    private OnClickListener mDayofWeekButtonListener;

    private Boolean mChecked_repeat = false;
    private Boolean mChecked_legal = false;
    private View mSeparator5;
    private View mSeparator6;
    private View mSeparator7;
    private Resources res;

    // private int mHourOfDay;
    // private int mMinute;
    public static final String CLOUND_ALARM_AUDIO_PATH = "sdcard/xiaoyun/audio/background";

    private boolean mCloudAlarm;
    public boolean mFromCloudAlarm;
    private Uri mDefaultRingtoneUri;
    private Runnable mRunnableNoRingTone = new Runnable() {

        @Override
        public void run() {
            /* YUNOS BEGIN PB */
            //Desc:BugID:6275499:optimize app resource by getIdentifier
            //use getIdentifier API for get correct resource index
            //##Date: 2015-08-04 ##Author:liuyun.lh
            int string_ringtone_unknown = Resources.getSystem().getIdentifier("ringtone_unknown", "string", "android");
            mRingSelectText.setText(mContext.getString(string_ringtone_unknown));//com.android.internal.R.string.ringtone_unknown
            /* YUNOS END PB */
        }
    };
    private Runnable mRunnableRingTone = new Runnable() {

        @Override
        public void run() {
            mRingSelectText.setText(mTitle);
        }
    };

    public Runnable mTimeUpdateThread = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(mTimeUpdateThread);
            mCalendar = Calendar.getInstance();
            if (mCalendar.get(Calendar.SECOND) == 0) {
                // mHourOfDay = mTimePicker.getCurrentHour();
                // mMinute = mTimePicker.getCurrentMinute();
                // setTimepickerTitle(mHourOfDay, mMinute);
                setTimepickerTitle(mHour, mMin);
            }
            mHandler.postDelayed(mTimeUpdateThread, 500);
        }
    };

    private final String[] mShortWeekDayStrings;
    private final String[] mWeekStrings;/*
                                         * = new String[] { "日", "一", "二", "三", "四", "五", "六" };
                                         */
    private String mDefaultLabelString;
    private final String[] mLongWeekDayStrings;
    public final int[] DAY_ORDER = new int[] { Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, };

    private void setButtonStatus(Button button, boolean enabled, boolean checked) {
        if (enabled) {
            if (checked) {
                button.setBackground(res.getDrawable(R.drawable.btnshape_green));
                button.setTextColor(res.getColor(R.color.repeat_white));
            } else {
                button.setBackground(res.getDrawable(R.drawable.btnshape_white));
                button.setTextColor(res.getColor(R.color.repeat_green));
            }
            button.setClickable(true);
        } else {
            button.setBackground(res.getDrawable(R.drawable.btnshape_grey));
            button.setTextColor(res.getColor(R.color.repeat_disable_text));
            button.setClickable(false);
        }
    }

    private void disalbeAllButton() {
        for (int i = 0; i < 7; i++) {
            Button button = mButtonList.get(i);
            setButtonStatus(button, false, false);
        }
    }

    public SetGroupView(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;
        res = mContext.getResources();
        mDefaultLabelString = getResources().getString(R.string.default_label);
        mWeekStrings = context.getResources().getStringArray(R.array.set_alarm_week_strings);
        mFactory = LayoutInflater.from(context);
        View v = LayoutInflater.from(context).inflate(R.layout.alarm_fragment_set_alarm, null, true);
        // mTimepickertitle = (TextView) v.findViewById(R.id.timepickertitle);
        mTimePicker = (TimePicker) v.findViewById(R.id.timePicker);
        // mSetAlarmView = (SetAlarmView) v.findViewById(R.id.circle_time_set);
        // mSetAlarmView.mSetGroup = this;

        // mTimeText = (TextView) v.findViewById(R.id.textClock);
        // mTimeText.setText("00:00");
        // mTimeText.setBackgroundColor(Color.argb(0, 0, 0, 0));
        // mTimeText.setSingleLine(true);
        /*
         * mTimeText.setOnClickListener(new OnClickListener() {
         * 
         * @Override public void onClick(View arg0) { if (((Activity)mContext).isFinishing()) { return ; }
         * TA.getInstance().getDefaultTracker().commitEvent("Page_AddAlarms", 2101, "Button_setAlarmWithPicker", null, null, null); int h = mHour; if
         * (!mSetAlarmView.isAm) { if (h == 12) { h = 12; } else { h = h+12; } } else { if (h == 12) { h = 0; } } AlarmTimePickerDialog dialog = new
         * AlarmTimePickerDialog(mContext, SetGroupView.this, h, mMin, DateFormat.is24HourFormat(mContext)); if (DateFormat.is24HourFormat(mContext))
         * { dialog.setTitle(((h<10)?"0":"") + h+":"+((mMin<10)?"0":"")+mMin); } else { dialog.setTitle(mHour+":"+((mMin<10)?"0":"")+mMin); }
         * dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.add_alarm_page_cacnel), new
         * DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) { setFromTimePickNow = false; dialog.dismiss();
         * } }); dialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.add_alarm_page_confirm), new
         * DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) { setFromTimePickNow = true; dialog.dismiss(); }
         * }); DialogUtils.fromBottomToTop(dialog); dialog.show(); } });
         */
        // mAm = (TextView) v.findViewById(R.id.am);
        /*
         * mAm.setOnClickListener(new OnClickListener() {
         * 
         * @Override public void onClick(View arg0) { TA.getInstance().getDefaultTracker().commitEvent("Page_AddAlarms", 2101, "Button_AMorPM", null,
         * null, null); mSetAlarmView.isAm = !mSetAlarmView.isAm; setAMorPMtext(mSetAlarmView.isAm); } });
         */

        // mSetAlarmView.SetTimeTextView(mTimeText);

        DateFormatSymbols dfs = new DateFormatSymbols();
        mShortWeekDayStrings = dfs.getShortWeekdays();
        mLongWeekDayStrings = dfs.getWeekdays();

        final String mOK = mContext.getString(R.string.add_alarm_page_confirm);
        final String mCancel = mContext.getString(R.string.add_alarm_page_cacnel);
        final String mRepeatTitle = mContext.getResources().getString(R.string.alarm_repeat);
        // Build button for each day.
        // repeatDaysText = (TextView)v.findViewById(R.id.repeat_days_choosed);
        mButtonList = new ArrayList<Button>();
        Button button;

        button = (Button) v.findViewById(R.id.roundBtn1);
        mButtonList.add(button);
        button = (Button) v.findViewById(R.id.roundBtn2);
        mButtonList.add(button);
        button = (Button) v.findViewById(R.id.roundBtn3);
        mButtonList.add(button);
        button = (Button) v.findViewById(R.id.roundBtn4);
        mButtonList.add(button);
        button = (Button) v.findViewById(R.id.roundBtn5);
        mButtonList.add(button);
        button = (Button) v.findViewById(R.id.roundBtn6);
        mButtonList.add(button);
        button = (Button) v.findViewById(R.id.roundBtn7);
        mButtonList.add(button);

        mDayofWeekButtonListener = new OnClickListener() {

            @Override
            public void onClick(View view) {
                for (int i = 0; i < 7; i++) {
                    Button button = mButtonList.get(i);
                    if (view == button) {
                        if (!repeatDaysArray[i]) {
                            setButtonStatus(button, true, true);
                            repeatDaysArray[i] = true;
                        } else {
                            setButtonStatus(button, true, false);
                            repeatDaysArray[i] = false;
                        }
                    }
                }

            }

        };

        for (int i = 0; i < 7; i++) {
            Button dowButton = mButtonList.get(i);
            dowButton.setOnClickListener(mDayofWeekButtonListener);
        }

        mCheckBox_legal = (CheckBox) v.findViewById(R.id.check_legal);
        mCheckBox_legal.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mCheckBox_legal.isChecked()) {
                    disalbeAllButton();
                } else {
                    for (int i = 0; i < 7; i++) {
                        Button button = mButtonList.get(i);
                        setButtonStatus(button, false, false);
                        if (repeatDaysArray[i]) {
                            setButtonStatus(button, true, true);
                        } else {
                            setButtonStatus(button, true, false);
                        }
                    }
                }
            }
        });

        mLegalWorkday = (FrameLayout) v.findViewById(R.id.legalworkday);
        mLegalWorkday.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mChecked_legal = !mCheckBox_legal.isChecked();
                mCheckBox_legal.setChecked(mChecked_legal);
                if (mCheckBox_legal.isChecked()) {
                    disalbeAllButton();
                } else {
                    for (int i = 0; i < 7; i++) {
                        Button button = mButtonList.get(i);
                        setButtonStatus(button, false, false);
                        if (repeatDaysArray[i]) {
                            setButtonStatus(button, true, true);
                        } else {
                            setButtonStatus(button, true, false);
                        }
                    }
                }
            }
        });

        mNumofweek = (FrameLayout) v.findViewById(R.id.numofweek);
        mSeparator5 = v.findViewById(R.id.separator5);
        mSeparator6 = v.findViewById(R.id.separator6);
        mSeparator7 = v.findViewById(R.id.separator7);
        mCheckBox_repeat = (CheckBox) v.findViewById(R.id.check_repeat);
        mCheckBox_repeat.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                int visible;
                if (mCheckBox_repeat.isChecked()) {
                    visible = View.VISIBLE;
                    mSeparator5.setVisibility(View.INVISIBLE);
                    //setDefaultWorkingDay();
                } else {
                    visible = View.INVISIBLE;
                    mSeparator5.setVisibility(View.VISIBLE);
                }
                mNumofweek.setVisibility(visible);
                if(DeskClock.mCalendarDataSupport){
                    mLegalWorkday.setVisibility(visible);
                    mSeparator7.setVisibility(visible);
                }
                mSeparator6.setVisibility(visible);
            }
        });
        mRepeatDays = (FrameLayout) v.findViewById(R.id.repeat_days);
        mRepeatDays.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mChecked_repeat = !mCheckBox_repeat.isChecked();
                mCheckBox_repeat.setChecked(mChecked_repeat);
                int visible;
                if (mChecked_repeat) {
                    visible = View.VISIBLE;
                    mSeparator5.setVisibility(View.INVISIBLE);
                    //setDefaultWorkingDay();
                } else {
                    visible = View.INVISIBLE;
                    mSeparator5.setVisibility(View.VISIBLE);
                }
                mNumofweek.setVisibility(visible);
                if(DeskClock.mCalendarDataSupport){
                    mLegalWorkday.setVisibility(visible);
                    mSeparator7.setVisibility(visible);
                }
                mSeparator6.setVisibility(visible);
                /*
                mRepeatDays.setClickable(false);
                repeatDaysText.setClickable(false);
                mInRepeatType = true;
                Intent repeatIntent= new Intent(mContext, RepeatActivity.class);
                repeatIntent.putExtra("repeat", repeatDaysText.getText());
                repeatIntent.putExtra("repeatDaysArray", repeatDaysArray);
                if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                    mContext.startActivityForResult(repeatIntent, REQUEST_REPEAT_TYPE);
                } else {
                    ((Activity) mContext).startActivityForResult(repeatIntent, REQUEST_REPEAT_TYPE);
                }
                */
			}
        });

        mRingSelectText = (TextView) v.findViewById(R.id.music_choosed);
        mRingSelectText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRingtoneItem.setClickable(false);
                mRingSelectText.setClickable(false);
                TA.getInstance().getDefaultTracker().commitEvent("Page_AddAlarms", 2101, "Button_Ringtone", null, null, null);
                ActionSheet actionSheet = new ActionSheet(mContext);
                ArrayList<String> items = new ArrayList<String>();
                /*YUNOS BEGIN CMCC*/
                //Desc:BugID:8159491:remove cloud alarm entry for CMCC spec
                //Author:chao.lc@alibaba-inc.com Date: 4/20/16 4:01 PM
                if (!Build.YUNOS_CARRIER_CMCC) {
                    items.add(mContext.getResources().getString(R.string.cloud_alarm));
                }
                /*YUNOS END CMCC*/
                items.add(mContext.getResources().getString(R.string.local_alarm));
                actionSheet.setCommonButtons(items);
                actionSheet.setCommonButtonListener(new ActionSheet.CommonButtonListener() {
                    @Override
                    public void onClick(int which) {
                        switch (which) {
                                case 0:
                                    /*YUNOS BEGIN CMCC*/
                                    //Desc:BugID:8159491:remove cloud alarm entry for CMCC spec
                                    //Author:chao.lc@alibaba-inc.com Date: 4/20/16 4:01 PM
                                    if (!Build.YUNOS_CARRIER_CMCC) {
                                        TA.getInstance().getDefaultTracker()
                                                .commitEvent("Page_AlarmClockFragment", 2101, "Button-Long_Press_to_yun_Alarm", null, null, null);
                                        mRingtoneItem.setClickable(true);
                                        mRingSelectText.setClickable(true);
                                        mRingSelectText.setText(R.string.cloud_title);
                                        mAlert = Uri.parse(CLOUND_ALARM_AUDIO_PATH);// NEED URI
                                        Log.i("AlarmClock", "mAlert1 = " + mAlert.toString());
                                    } else {
                                        TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment",
                                                2101, "Button-Long_Press_to_native_Alarm", null, null, null);
                                        Log.i("AlarmClock", "mAlert2 = " + mAlert.toString());
                                        if (mAlert.toString().equals(CLOUND_ALARM_AUDIO_PATH)) {
                                            mFromCloudAlarm = true;
                                            mAlert = Uri.parse("content://media/internal/audio/media/14");
                                        }
                                        launchRingTonePicker(mAlert);

                                    }
                                     /*YUNOS END CMCC*/
                            break;
                        case 1:
                            TA.getInstance().getDefaultTracker()
                                    .commitEvent("Page_AlarmClockFragment", 2101, "Button-Long_Press_to_native_Alarm", null, null, null);
                            Log.i("AlarmClock", "mAlert2 = " + mAlert.toString());
                            if (mAlert.toString().equals(CLOUND_ALARM_AUDIO_PATH)) {
                                mFromCloudAlarm = true;
                                /*YUNOS BEGIN PB*/
                                    //##module: AliDeskClock  ##author: haibo.yhb@alibaba-inc.com
                                    //##BugID:(7775907) ##date: 2016-02-02 16:31:00
                                    //##description: Remove hardcode default ringtone URI
                                    mAlert = mDefaultRingtoneUri;
                                    /*YUNOS END PB*/
                            }
                            launchRingTonePicker(mAlert);
                            break;
                        default:
                        }
                    }

                    @Override
                    public void onDismiss(ActionSheet arg0) {
                        mRingtoneItem.setClickable(true);
                        mRingSelectText.setClickable(true);
                    }
                });
                actionSheet.show(mRingtoneItem);
                return;
            }
        });

        mTitleText = (TextView) v.findViewById(R.id.alarm_label);
        mTitleText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mTitleText.setClickable(false);
                mLabelItem.setClickable(false);
                if (((Activity) mContext).isFinishing()) {
                    return;
                }
                TA.getInstance().getDefaultTracker().commitEvent("Page_AddAlarms", 2101, "Button_AddAlarmLabel", null, null, null);
                LayoutInflater factory = LayoutInflater.from(mContext);
                final View textInputView = factory.inflate(R.layout.alert_dialog_input, null);
                final EditText inputServer = (EditText) textInputView.findViewById(R.id.text_edit);
                inputServer.setFilters(new InputFilter[] { new InputFilter.LengthFilter(200) });
                inputServer.setText(mTitleText.getText().toString());
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        mTitleText.setClickable(true);
                        mLabelItem.setClickable(true);
                    }
                });
                builder.setTitle(R.string.label).setView(textInputView).setNegativeButton(R.string.add_alarm_page_cacnel, null);
                builder.setPositiveButton(R.string.add_alarm_page_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String text = inputServer.getText().toString();
                        mTitleText.setText(text);
                        if (mTitleText.getText().toString().length() == 0) {
                            mTitleText.setText(mDefaultLabelString);
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                inputServer.requestFocus();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!mTitleText.isClickable()) {
                            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                    }
                }, 500);

                Log.i("OnClickListener", "vibrateView clicked");
            }

        });

        mRingtoneItem = (FrameLayout) v.findViewById(R.id.ringtong_item);
        mRingtoneItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRingtoneItem.setClickable(false);
                mRingSelectText.setClickable(false);
                TA.getInstance().getDefaultTracker().commitEvent("Page_AddAlarms", 2101, "Button_Ringtone", null, null, null);
                ActionSheet actionSheet = new ActionSheet(mContext);
                ArrayList<String> items = new ArrayList<String>();
                /*YUNOS BEGIN CMCC*/
                //Desc:BugID:8159491:remove cloud alarm entry for CMCC spec
                //Author:chao.lc@alibaba-inc.com Date: 4/20/16 4:01 PM
                if (!Build.YUNOS_CARRIER_CMCC) {
                    items.add(mContext.getResources().getString(R.string.cloud_alarm));
                }
                /*YUNOS END CMCC*/
                items.add(mContext.getResources().getString(R.string.local_alarm));
                actionSheet.setCommonButtons(items);
                actionSheet.setCommonButtonListener(new ActionSheet.CommonButtonListener() {
                    @Override
                    public void onClick(int which) {
                        switch (which) {
                                case 0:
                                    /*YUNOS BEGIN CMCC*/
                                    //Desc:BugID:8159491:remove cloud alarm entry for CMCC spec
                                    //Author:chao.lc@alibaba-inc.com Date: 4/20/16 4:01 PM
                                    if (!Build.YUNOS_CARRIER_CMCC) {
                                        TA.getInstance().getDefaultTracker()
                                                .commitEvent("Page_AlarmClockFragment", 2101, "Button-Long_Press_to_yun_Alarm", null, null, null);
                                        mRingtoneItem.setClickable(true);
                                        mRingSelectText.setClickable(true);
                                        mRingSelectText.setText(R.string.cloud_title);
                                        mAlert = Uri.parse(CLOUND_ALARM_AUDIO_PATH);// NEED URI
                                        Log.i("AlarmClock", "mAlert1 = " + mAlert.toString());
                                    } else {
                                        TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment",
                                                2101, "Button-Long_Press_to_native_Alarm", null, null, null);
                                        Log.i("AlarmClock", "mAlert2 = " + mAlert.toString());
                                        if (mAlert.toString().equals(CLOUND_ALARM_AUDIO_PATH)) {
                                            mFromCloudAlarm = true;
                                            mAlert = Uri.parse("content://media/internal/audio/media/14");
                                        }
                                        launchRingTonePicker(mAlert);
                                    }
                                    /*YUNOS END CMCC*/
                            break;
                        case 1:
                            TA.getInstance().getDefaultTracker()
                                    .commitEvent("Page_AlarmClockFragment", 2101, "Button-Long_Press_to_native_Alarm", null, null, null);
                            Log.i("AlarmClock", "mAlert2 = " + mAlert.toString());
                            if (mAlert.toString().equals(CLOUND_ALARM_AUDIO_PATH)) {
                                mFromCloudAlarm = true;
                                /*YUNOS BEGIN PB*/
                                    //##module: AliDeskClock  ##author: haibo.yhb@alibaba-inc.com
                                    //##BugID:(7775907) ##date: 2016-02-02 16:31:00
                                    //##description: Remove hardcode default ringtone URI
                                    mAlert = mDefaultRingtoneUri;
                                    /*YUNOS END PB*/
                            }
                            launchRingTonePicker(mAlert);
                            break;
                        default:
                        }
                    }

                    @Override
                    public void onDismiss(ActionSheet arg0) {
                        mRingtoneItem.setClickable(true);
                        mRingSelectText.setClickable(true);
                    }
                });
                actionSheet.show(mRingtoneItem);
                return;
            }
        });

        mLabelItem = (FrameLayout) v.findViewById(R.id.label_item);
        mLabelItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLabelItem.setClickable(false);
                mTitleText.setClickable(false);
                if (((Activity) mContext).isFinishing()) {
                    return;
                }
                TA.getInstance().getDefaultTracker().commitEvent("Page_AddAlarms", 2101, "Button_AddAlarmLabel", null, null, null);
                LayoutInflater factory = LayoutInflater.from(mContext);
                final View textInputView = factory.inflate(R.layout.alert_dialog_input, null);
                final EditText inputServer = (EditText) textInputView.findViewById(R.id.text_edit);
                inputServer.setFilters(new InputFilter[] { new InputFilter.LengthFilter(200) });
                inputServer.setText(mTitleText.getText().toString());
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        mTitleText.setClickable(true);
                        mLabelItem.setClickable(true);
                    }
                });
                builder.setTitle(R.string.label).setView(textInputView).setNegativeButton(R.string.add_alarm_page_cacnel, null);
                builder.setPositiveButton(R.string.add_alarm_page_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String text = inputServer.getText().toString();
                        mTitleText.setText(text);
                        if (mTitleText.getText().toString().length() == 0) {
                            mTitleText.setText(mDefaultLabelString);
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                inputServer.requestFocus();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!mTitleText.isClickable()) {
                            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                    }
                }, 500);

                Log.i("OnClickListener", "vibrateView clicked");
            }
        });

        mVibrateItem = (FrameLayout) v.findViewById(R.id.vibrate_item);
        mVibrateItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mVibrate = !mVibrate;
                mCheckBox_vibrate.setChecked(mVibrate);
            }
        });

        mCheckBox_vibrate = (CheckBox) v.findViewById(R.id.check_vibrate);
        mCheckBox_vibrate.setChecked(mVibrate);
        mCheckBox_vibrate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HashMap<String, String> lMaps = new HashMap<String, String>();
                lMaps.put("from_page", "Page_AddAlarms");
                if (isChecked) {
                    lMaps.put("switch_vibrate_result", "true");
                } else {
                    lMaps.put("switch_vibrate_result", "false");
                }
                if (null == TA.getInstance() || null == TA.getInstance().getDefaultTracker()) {
                    StatConfig.getInstance().setContext(mContext);
                    StatConfig.getInstance().turnOnDebug();
                    Tracker lTracker = TA.getInstance().getTracker("21736479");
                    lTracker.setAppKey("21736479");
                    TA.getInstance().setDefaultTracker(lTracker);
                }
                TA.getInstance().getDefaultTracker().commitEvent("Page_AddAlarms_Switch_Vibrate", lMaps);
                mVibrate = isChecked;
            }
        });

        addView(v, 0);
        /*YUNOS BEGIN PB*/
        //##module: AliDeskClock  ##author: haibo.yhb@alibaba-inc.com
        //##BugID:(7775907) ##date: 2016-02-02 16:31:00
        //##description: Remove hardcode default ringtone URI
        mDefaultRingtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
        if(mDefaultRingtoneUri == null) {
            Log.w("AlarmClock", "no default alarm ringtone");
            mDefaultRingtoneUri = Uri.parse("content://media/internal/audio/media/14");
        }
        /*YUNOS END PB*/
    }

    public void setFooterButtonEnable() {
        mAgreeBtn.setEnabled(true);
        mCancelBtn.setEnabled(true);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }

    public void setAMorPMtext(boolean flag) {
        if (flag) {
            // mAm.setText(mContext.getResources().getString(R.string.clock_am));
        } else {
            // mAm.setText(mContext.getResources().getString(R.string.clock_pm));
        }
    }


    public void recoverRepeatView(Alarm alarm, Handler handler){
        if (alarm != null) {
            mAlarm = alarm;
            mHour = alarm.hour;
            mHandler = handler;
            mMin = alarm.minutes;
            mAlert = alarm.alert;
            mCloudAlarm = false;
            if (mAlert.toString().equals(CLOUND_ALARM_AUDIO_PATH)) {
                mCloudAlarm = true;
            }
            Log.i("AlarmClock", "recoverRepeatView");
            new Thread(new Runnable() {

                @Override
                public void run() {
                    if (mCloudAlarm) {
                        if (Alarm.NO_RINGTONE_URI.equals(mAlert)) {
                            mHandler.removeCallbacks(mRunnableNoRingTone);
                            mHandler.post(mRunnableNoRingTone);
                        } else {
                            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                                mTitle = mContext.getResources().getString(R.string.cloud_title);
                            } else {
                                Ringtone ringTone = RingtoneManager.getRingtone(mContext, mAlert);
                                mTitle = mContext.getResources().getString(R.string.cloud_title);
                            }
                            mHandler.removeCallbacks(mRunnableRingTone);
                            mHandler.post(mRunnableRingTone);
                        }
                        return;
                    }
                    if (mAlert == null) {
                        mAlert = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM);
                    }

                    // check the file isExist.
                    String audio_path = getRealPathFromURI(mAlert);
                    if (audio_path != null) {
                        File file = new File(audio_path);
                        if (!file.exists()) {
                            Log.i("AlarmClock", "file is not exist, set a static uri");
                            // mAlert = Uri.parse("content://settings/system/alarm_alert");
                            String sUri = Settings.System.getString(mContext.getContentResolver(), Settings.System.SYSTEM_ALARM_ALERT);
                            if (sUri != null) {
                                mAlert = Uri.parse(sUri);
                            } else {
                                Log.i("AlarmClock", "media scan error");
                                mAlert = Uri.parse("content://media/internal/audio/media/14");
                            }
                        }
                    } else {
                        // mAlert = Uri.parse("content://media/internal/audio/media/14");
                        String sUri = Settings.System.getString(mContext.getContentResolver(), Settings.System.SYSTEM_ALARM_ALERT);
                        if (sUri != null) {
                            mAlert = Uri.parse(sUri);
                        } else {
                            Log.i("AlarmClock", "media scan error");
                            mAlert = Uri.parse("content://media/internal/audio/media/14");
                        }
                    }

                    /*
                     * if (mAlert == null) { if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) { Object object =
                     * ClassUtils.invokeMethod(RingtoneManager.class, "getDefaultRingtoneUri", Context.class, String.class, mContext,
                     * RingtoneManager.TYPE_ALARM); mAlert = (Uri) object; } else { mAlert = RingtoneManager.getActualDefaultRingtoneUri(mContext,
                     * RingtoneManager.TYPE_ALARM); } }
                     */

                    if (Alarm.NO_RINGTONE_URI.equals(mAlert)) {
                        mHandler.removeCallbacks(mRunnableNoRingTone);
                        mHandler.post(mRunnableNoRingTone);
                    } else {
                        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                            mTitle = getRingToneTitle(mAlert);//liuqipeng  还没修改
                        } else {
                            Ringtone ringTone = RingtoneManager.getRingtone(mContext, mAlert);
                            mTitle = ringTone.getTitle(mContext);
                        }
                        mHandler.removeCallbacks(mRunnableRingTone);
                        mHandler.post(mRunnableRingTone);
                    }
                }
            }).start();

            mVibrate = alarm.vibrate;
            mCheckBox_vibrate.setChecked(mVibrate);

            if (alarm.label != null && alarm.label != "" && alarm.label.length() != 0) {
                mTitleText.setText(alarm.label);
            } else {
                mTitleText.setText(mDefaultLabelString);
            }
        }
        if (mDaysOfWeek_bak.getBitSet() == mDaysOfWeek_bak.LEGAL_DAYS) {
            disalbeAllButton();
            mCheckBox_repeat.setChecked(true);
            mCheckBox_legal.setChecked(true);
            mNumofweek.setVisibility(View.VISIBLE);
            if(DeskClock.mCalendarDataSupport){
                mLegalWorkday.setVisibility(View.VISIBLE);
                mSeparator7.setVisibility(View.VISIBLE);
            }
            mSeparator5.setVisibility(View.INVISIBLE);
            mSeparator6.setVisibility(View.VISIBLE);
        } else {
            boolean re = false;
            HashSet<Integer> setDays = mDaysOfWeek_bak.getSetDays();
            for (int i = 0; i < 7; i++) {
                if (setDays.contains(DAY_ORDER[i])) {
                    Log.d("AlarmClock", "i = " + i);
                    re = true;
                    repeatDaysArray[i] = true;
                    Button button = mButtonList.get(i);
                    setButtonStatus(button, true, true);
                }
            }

            if (mRepeatChecked_bak) {
                mCheckBox_repeat.setChecked(true);
                mNumofweek.setVisibility(View.VISIBLE);
                if(DeskClock.mCalendarDataSupport){
                    mLegalWorkday.setVisibility(View.VISIBLE);
                    mSeparator7.setVisibility(View.VISIBLE);
                }
                mSeparator5.setVisibility(View.INVISIBLE);
                mSeparator6.setVisibility(View.VISIBLE);
            } else {
                mSeparator5.setVisibility(View.VISIBLE);
            }
        }
    }

    public void setView(Alarm alarm, Handler handler){
        mAlarm = alarm;
        mHour = alarm.hour;
        mHandler = handler;
        /*if (alarm.hour > 12) {
            mHour = alarm.hour -12;
            //mSetAlarmView.isAm = false;
        } else if (alarm.hour == 12) {
            mHour = alarm.hour;
            //mSetAlarmView.isAm = true;
        } else {
            mHour = alarm.hour;
            //mSetAlarmView.isAm = true;
        }*/

        // boolean flag = !mSetAlarmView.isAm;
        // setAMorPMtext(flag);
        // mAm.setSelected(!mSetAlarmView.isAm);
        // mPm.setSelected(mSetAlarmView.isAm);

        // mAm.setBackground(getResources().getDrawable(mSetAlarmView.isAm?R.drawable.clock_btn_repeat_pressed:R.drawable.clock_btn_repeat_normal));
        // mAm.setTextColor(mSetAlarmView.isAm?Color.WHITE:Color.GRAY);

        // mPm.setBackground(getResources().getDrawable(mSetAlarmView.isAm?R.drawable.clock_btn_repeat_normal:R.drawable.clock_btn_repeat_pressed));
        // mPm.setTextColor(mSetAlarmView.isAm?Color.GRAY:Color.WHITE);

        mMin = alarm.minutes;
        // mSetAlarmView.setView(mHour, mMin);

        mAlert = alarm.alert;
        mCloudAlarm = false;
        if (mAlert.toString().equals(CLOUND_ALARM_AUDIO_PATH)) {
            mCloudAlarm = true;
        }
        Log.i("AlarmClock", "setView");
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (mCloudAlarm) {
                    if (Alarm.NO_RINGTONE_URI.equals(mAlert)) {
                        mHandler.removeCallbacks(mRunnableNoRingTone);
                        mHandler.post(mRunnableNoRingTone);
                    } else {
                        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                            mTitle = mContext.getResources().getString(R.string.cloud_title);
                        } else {
                            Ringtone ringTone = RingtoneManager.getRingtone(mContext, mAlert);
                            mTitle = mContext.getResources().getString(R.string.cloud_title);
                        }
                        mHandler.removeCallbacks(mRunnableRingTone);
                        mHandler.post(mRunnableRingTone);
                    }
                    return;
                }
                if (mAlert == null) {
                    mAlert = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM);
                }

                // check the file isExist.
                String audio_path = getRealPathFromURI(mAlert);
                if (audio_path != null) {
                    File file = new File(audio_path);
                    if (!file.exists()) {
                        Log.i("AlarmClock", "file is not exist, set a static uri");
                        // mAlert = Uri.parse("content://settings/system/alarm_alert");
                        String sUri = Settings.System.getString(mContext.getContentResolver(), Settings.System.SYSTEM_ALARM_ALERT);
                        if (sUri != null) {
                            mAlert = Uri.parse(sUri);
                        } else {
                            Log.i("AlarmClock", "media scan error");
                            /*YUNOS BEGIN PB*/
                            //##module: AliDeskClock  ##author: haibo.yhb@alibaba-inc.com
                            //##BugID:(7775907) ##date: 2016-02-02 16:31:00
                            //##description: Remove hardcode default ringtone URI
                            mAlert = mDefaultRingtoneUri;
                            /*YUNOS END PB*/
                        }
                    }
                } else {
                    // mAlert = Uri.parse("content://media/internal/audio/media/14");
                    String sUri = Settings.System.getString(mContext.getContentResolver(), Settings.System.SYSTEM_ALARM_ALERT);
                    if (sUri != null) {
                        mAlert = Uri.parse(sUri);
                    } else {
                        Log.i("AlarmClock", "media scan error");
                        /*YUNOS BEGIN PB*/
                        //##module: AliDeskClock  ##author: haibo.yhb@alibaba-inc.com
                        //##BugID:(7775907) ##date: 2016-02-02 16:31:00
                        //##description: Remove hardcode default ringtone URI
                        mAlert = mDefaultRingtoneUri;
                        /*YUNOS END PB*/
                    }
                }

                /*if (mAlert == null) {
                    if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                        Object object = ClassUtils.invokeMethod(RingtoneManager.class, "getDefaultRingtoneUri", Context.class,
                        String.class, mContext, RingtoneManager.TYPE_ALARM);
                        mAlert = (Uri) object;
                    } else {
                        mAlert = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM);
                    }
                }*/

                if (Alarm.NO_RINGTONE_URI.equals(mAlert)) {
                    mHandler.removeCallbacks(mRunnableNoRingTone);
                    mHandler.post(mRunnableNoRingTone);
                } else {
                    if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                        mTitle = getRingToneTitle(mAlert);
                    } else {
                        Ringtone ringTone = RingtoneManager.getRingtone(mContext, mAlert);
                        mTitle = ringTone.getTitle(mContext);
                    }
                    mHandler.removeCallbacks(mRunnableRingTone);
                    mHandler.post(mRunnableRingTone);
                }
            }
        }).start();

        mVibrate = alarm.vibrate;
        mCheckBox_vibrate.setChecked(mVibrate);

        if (alarm.label != null && alarm.label != "" && alarm.label.length() != 0) {
            mTitleText.setText(alarm.label);
        } else {
            mTitleText.setText(mDefaultLabelString);
        }
        Log.d("AlarmClock", "mContext.getResources().getDisplayMetrics().widthPixels = " + mContext.getResources().getDisplayMetrics().widthPixels);
        Log.d("AlarmClock", "mContext.getResources().getDisplayMetrics().heightPixels = " + mContext.getResources().getDisplayMetrics().heightPixels);

        Log.d("AlarmClock", "mContext.getResources().getDisplayMetrics().xdpi = " + mContext.getResources().getDisplayMetrics().xdpi);
        Log.d("AlarmClock", "mContext.getResources().getDisplayMetrics().ydpi = " + mContext.getResources().getDisplayMetrics().ydpi);
        mDaysOfWeek = alarm.daysOfWeek;
        if (mDaysOfWeek.getBitSet() == mDaysOfWeek.LEGAL_DAYS) {
            disalbeAllButton();
            mCheckBox_repeat.setChecked(true);
            mCheckBox_legal.setChecked(true);
            mNumofweek.setVisibility(View.VISIBLE);
            if(DeskClock.mCalendarDataSupport){
                mLegalWorkday.setVisibility(View.VISIBLE);
                mSeparator7.setVisibility(View.VISIBLE);
            }
            mSeparator5.setVisibility(View.INVISIBLE);
            mSeparator6.setVisibility(View.VISIBLE);
        } else {
            boolean re = false;
            HashSet<Integer> setDays = mDaysOfWeek.getSetDays();
            for (int i = 0; i < 7; i++) {
                if (setDays.contains(DAY_ORDER[i])) {
                    Log.d("AlarmClock", "i = " + i);
                    re = true;
                    repeatDaysArray[i] = true;
                    Button button = mButtonList.get(i);
                    setButtonStatus(button, true, true);
                }
            }

            if (re) {
                mCheckBox_repeat.setChecked(true);
                mNumofweek.setVisibility(View.VISIBLE);
                if(DeskClock.mCalendarDataSupport){
                    mLegalWorkday.setVisibility(View.VISIBLE);
                    mSeparator7.setVisibility(View.VISIBLE);
                }
                mSeparator5.setVisibility(View.INVISIBLE);
                mSeparator6.setVisibility(View.VISIBLE);
            } else {
                mSeparator5.setVisibility(View.VISIBLE);
            }
        }

        // final String daysOfWeekStr =
        // mDaysOfWeek.toString(mContext, false);
        // repeatDaysText.setText(daysOfWeekStr);
    }

    EditText mHourEditText;
    EditText mMinuteEditText;
    EditText mAmPmEditText;
    Boolean mIsAm = true;

    public void updateTimePicker() {
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(mContext));
        mTimePicker.setCurrentHour(mHour);
        mTimePicker.setCurrentMinute(mMin);
		Log.d("ClockDataStorage","set timepicker current time hour:"+mHour+"min:"+"mMin");//liuqipeng add
        mTimePicker.setOnTimeChangedListener(this);
        // mTimePicker.setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS);
        try {
            Object Obj = null;
            Class A = ClassUtils.loadClass("android.widget.TimePickerSpinnerDelegate");
            Field mTimePickerDelegate = mTimePicker.getClass().getDeclaredField("mDelegate");
            if (mTimePickerDelegate != null) {
                mTimePickerDelegate.setAccessible(true);
                Obj = mTimePickerDelegate.get(mTimePicker);
            }
            Field mHourSpinnerInput = A.getDeclaredField("mHourSpinnerInput");
            Field mMinuteSpinnerInput = A.getDeclaredField("mMinuteSpinnerInput");
            Field mAmPmSpinnerInput = A.getDeclaredField("mAmPmSpinnerInput");

            if (mHourSpinnerInput != null && Obj != null) {
                mHourSpinnerInput.setAccessible(true);
                mHourEditText = (EditText) mHourSpinnerInput.get(Obj);
                mHourEditText.setFocusable(false);
            }
            if (mMinuteSpinnerInput != null && Obj != null) {
                mMinuteSpinnerInput.setAccessible(true);
                mMinuteEditText = (EditText) mMinuteSpinnerInput.get(Obj);
                mMinuteEditText.setFocusable(false);
            }
            if (mAmPmSpinnerInput != null && Obj != null) {
                mAmPmSpinnerInput.setAccessible(true);
                mAmPmEditText = (EditText) mAmPmSpinnerInput.get(Obj);
                mAmPmEditText.setFocusable(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // mHourEditText.addTextChangedListener(mHourWatcher);
        // mMinuteEditText.addTextChangedListener(mMinuteWatcher);
        // mAmPmEditText.addTextChangedListener(mWatcher);

        // mHourOfDay = mTimePicker.getCurrentHour();
        // mMinute = mTimePicker.getCurrentMinute();
        // setTimepickerTitle(mHourOfDay, mMinute);
        setTimepickerTitle(mHour, mMin);
    }

    private TextWatcher mHourWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // TODO Auto-generated method stub
            if (!TextUtils.isEmpty(s)) {
                mHandler.removeCallbacks(mTimeUpdateThread);
                mHandler.postDelayed(mTimeUpdateThread, 500);
                mHour = Integer.valueOf(s.toString()).intValue();
                if (!mIsAm && !DateFormat.is24HourFormat(mContext)) {
                    mHour += 12;
                }
                int mHourOfDay = mTimePicker.getCurrentHour();
                int mMinute = mTimePicker.getCurrentMinute();
                setTimepickerTitle(mHour, mMin);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // TODO Auto-generated method stub
        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub
        }
    };

    private TextWatcher mMinuteWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // TODO Auto-generated method stub
            Log.d("randy", "s222222222222222 = " + s);
            if (!TextUtils.isEmpty(s)) {
                mHandler.removeCallbacks(mTimeUpdateThread);
                mHandler.postDelayed(mTimeUpdateThread, 500);
                mMin = Integer.valueOf(s.toString()).intValue();
                setTimepickerTitle(mHour, mMin);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // TODO Auto-generated method stub
        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub
        }
    };

    public boolean[] repeatDaysArray = { false, false, false, false, false, false, false };
    private boolean[] tempResult = { false, false, false, false, false, false, false };

    public void setFragment(SetAlarmActivity setAlarmActivity, Handler handler, Alarm alarmData) {
        mSetAlarmActivity = setAlarmActivity;
        mHandler = handler;
        mHandler.removeCallbacks(mTimeUpdateThread);
        mHandler.post(mTimeUpdateThread);
        // mTimeText.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "fonts/DINPro/DINPro-Light.otf"));

        // TODO : keep this to control the display of the TextView
        // if (DateFormat.is24HourFormat(mContext)) {
        // mAm.setVisibility(View.GONE);
        // } else {
        // mAm.setVisibility(View.VISIBLE);
        // }

        // mSetAlarmView.resetView();
        Log.i("AlarmClock", "setFragment");
        Time t = new Time();
        t.setToNow();
        mHour = t.hour;
        mMin = t.minute;
        mAlarm = null;

        /*if (t.hour > 12) {
            mHour = t.hour -12;
            //mSetAlarmView.isAm = false;
        } else if (t.hour == 12) {
            mHour = t.hour;
            //mSetAlarmView.isAm = true;
        } else {
            mHour = t.hour;
            //mSetAlarmView.isAm = true;
        }*/
        //mHour = t.hour;
        //boolean flag = !mSetAlarmView.isAm;
        //setAMorPMtext(flag);

        // mSetAlarmView.setView(mHour, mMin);
        if (alarmData == null) {
            new Thread(new Runnable() {
                @Override
                public void run(){
                    //if (mSetAlarmActivity.mFromCancel) {
                        //Log.i("AlarmClock", "mFromCancel");
                        // mAlert =
                        // Uri.parse("content://media/internal/audio/media/14");
                        String sUri = Settings.System.getString(
                                mContext.getContentResolver(),
                                Settings.System.SYSTEM_ALARM_ALERT);
                        if (sUri != null) {
                            mAlert = Uri.parse(sUri);
                        } else {
                            Log.i("AlarmClock", "media scan error");
                            /* YUNOS BEGIN PB */
                            // ##module: AliDeskClock ##author:
                            // haibo.yhb@alibaba-inc.com
                            // ##BugID:(7775907) ##date: 2016-02-02 16:31:00
                            // ##description: Remove hardcode default ringtone
                            // URI
                            mAlert = mDefaultRingtoneUri;
                            mSetAlarmActivity.mFromCancel = false;
                            /* YUNOS END PB */
                        }
                    //}
					/* 
					else {
                        mAlert = RingtoneManager.getActualDefaultRingtoneUri(
                                mContext, RingtoneManager.TYPE_ALARM);
                        // check the file isExist.
                        String audio_path = getRealPathFromURI(mAlert);
                        if (audio_path != null) {
                            Log.i("AlarmClock", "mFromCancel and audio_path is not null");
                            File file = new File(audio_path);
                            if (!file.exists()) {
                                Log.i("AlarmClock",
                                        "file is not exist, set a static uri");
                                // mAlert =
                                // Uri.parse("content://settings/system/alarm_alert");
                                String sUri = Settings.System.getString(
                                        mContext.getContentResolver(),
                                        Settings.System.SYSTEM_ALARM_ALERT);
                                if (sUri != null) {
                                    mAlert = Uri.parse(sUri);
                                } else {
                                    Log.i("AlarmClock", "media scan error");
                                    // ##module: AliDeskClock ##author:
                                    // haibo.yhb@alibaba-inc.com
                                    // ##BugID:(7775907) ##date: 2016-02-02
                                    // 16:31:00
                                    // ##description: Remove hardcode default
                                    // ringtone URI
                                    mAlert = mDefaultRingtoneUri;
                                }
                            }
                        } else {
                            Log.i("AlarmClock", "mFromCancel and audio_path is null");
                            // mAlert =
                            // Uri.parse("content://media/internal/audio/media/14");
                            String sUri = Settings.System.getString(
                                    mContext.getContentResolver(),
                                    Settings.System.SYSTEM_ALARM_ALERT);
                            if (sUri != null) {
                                mAlert = Uri.parse(sUri);
                            } else {
                                Log.i("AlarmClock", "media scan error");
                                // ##module: AliDeskClock ##author:
                                // haibo.yhb@alibaba-inc.com
                                // ##BugID:(7775907) ##date: 2016-02-02 16:31:00
                                // ##description: Remove hardcode default
                                // ringtone URI
                                mAlert = mDefaultRingtoneUri;
                            }
                        }
                    }*/
                    /*
                     * if (mAlert.equals(Uri.parse(
                     * "content://settings/system/alarm_alert")) || mAlert ==
                     * null) { if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                     * Object object =
                     * ClassUtils.invokeMethod(RingtoneManager.class,
                     * "getDefaultRingtoneUri", Context.class, String.class,
                     * mContext, RingtoneManager.TYPE_ALARM); mAlert = (Uri)
                     * object; } else { mAlert =
                     * RingtoneManager.getActualDefaultRingtoneUri(mContext,
                     * RingtoneManager.TYPE_ALARM); } }
                     */
                    if (Alarm.NO_RINGTONE_URI.equals(mAlert)) {
                        mHandler.removeCallbacks(mRunnableNoRingTone);
                        mHandler.post(mRunnableNoRingTone);
                    } else {
                        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                            mTitle = getRingToneTitle(mAlert);
                        } else {
                            Ringtone ringTone = RingtoneManager.getRingtone(
                                    mContext, mAlert);
                            mTitle = ringTone.getTitle(mContext);
                        }
                        mHandler.removeCallbacks(mRunnableRingTone);
                        mHandler.post(mRunnableRingTone);
                    }
            }}).start();
        }
        for (int i = 0; i < 7; i++) {
            int day = DAY_ORDER[i];
            mDaysOfWeek.setDaysOfWeek(false, day);
            repeatDaysArray[i] = false;
        }
        //repeatDaysText.setText("");
/*        for (int i = 0; i < 7; i++) {
            mDayButton[i].setChecked(false);
            int day = DAY_ORDER[i];
            mDaysOfWeek.setDaysOfWeek(false, day);
        }*/
        mVibrate = true;
        mCheckBox_vibrate.setChecked(mVibrate);
        mTitleText.setText(mDefaultLabelString);
    }

    public String getRingToneTitle(Uri uri) {
        // Try the cache first
        String title = null;
        // if (mSetAlarmActivity != null && mSetAlarmActivity.mRingtoneTitleCache != null && uri != null) {
        // title = mSetAlarmActivity.mRingtoneTitleCache.getString(uri.toString());
        // }
        // if (title == null) {
        // This is slow because a media player is created during Ringtone object creation.
        Ringtone ringTone = RingtoneManager.getRingtone(mContext, uri);
        if (ringTone == null) {
            return null;
        }
        title = ringTone.getTitle(mContext);
        if (title != null && mSetAlarmActivity != null && mSetAlarmActivity.mRingtoneTitleCache != null && uri != null) {
            mSetAlarmActivity.mRingtoneTitleCache.putString(uri.toString(), title);
        }
        // }
        return title;
    }

    public String getRingToneTitle(Uri uri, Bundle cache) {
        Ringtone ringTone = RingtoneManager.getRingtone(mContext, uri);
        String title = ringTone.getTitle(mContext);
        if (title != null && cache != null && uri != null) {
            cache.putString(uri.toString(), title);
        }
        Log.i("alarm", "title=" + title);
        return title;
    }

    private void launchRingTonePicker(Uri ringtoneAlert) {
        Uri oldRingtone = Alarm.NO_RINGTONE_URI.equals(ringtoneAlert) ? null : ringtoneAlert;
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, oldRingtone);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            ((Activity) mContext).startActivityForResult(intent, 1);
        } else {
            ((Activity) mContext).startActivityForResult(intent, 1);
        }
    }

    public void saveNoRingtoneUri(Intent intent, Bundle cache) {
        // Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        // if (uri != null) {
        // mAlert = uri;
        // }
        Uri uri = mAlert;
        Log.i("AlarmClock", "uri :" + uri.toString());

        // Save the last selected ringtone as the default for new alarms
        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
            RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM, uri);
        }

        if (Alarm.NO_RINGTONE_URI.equals(mAlert)) {
            /* YUNOS BEGIN PB */
            //Desc:BugID:6275499:optimize app resource by getIdentifier
            //use getIdentifier API for get correct resource index
            //##Date: 2015-08-04 ##Author:liuyun.lh
            int string_ringtone_unknown = Resources.getSystem().getIdentifier("ringtone_unknown", "string", "android");
            mRingSelectText.setText(mContext.getString(string_ringtone_unknown));//com.android.internal.R.string.ringtone_unknown
            /* YUNOS END PB */
        } else {
            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                mRingSelectText.setText(getRingToneTitle(mAlert));
            } else {
                mRingSelectText.setText(getRingToneTitle(mAlert, cache));
            }
        }
    }

    public void saveRingtoneUri(Intent intent, Bundle cache) {
        Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri != null) {
            mAlert = uri;
        }

        // Save the last selected ringtone as the default for new alarms
        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
            RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM, uri);
        }

        if (Alarm.NO_RINGTONE_URI.equals(mAlert)) {
            /* YUNOS BEGIN PB */
            //Desc:BugID:6275499:optimize app resource by getIdentifier
            //use getIdentifier API for get correct resource index
            //##Date: 2015-08-04 ##Author:liuyun.lh
            int string_ringtone_unknown = Resources.getSystem().getIdentifier("ringtone_unknown", "string", "android");
            mRingSelectText.setText(mContext.getString(string_ringtone_unknown));//com.android.internal.R.string.ringtone_unknown
            /* YUNOS END PB */
        } else {
            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                mRingSelectText.setText(getRingToneTitle(mAlert));
            } else {
                mRingSelectText.setText(getRingToneTitle(mAlert, cache));
            }
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        if (null == contentURI) {
            return null;
        }
        String result = null;
        Cursor cursor = mContext.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            int index = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
            Log.i("AlarmClock", "the index of the DATA :" + index);
            // check the init cursor index
            if (cursor.moveToFirst() && index != -1) {
                result = cursor.getString(index);
            }
            /*if (result == null) {
                index = cursor.getColumnIndex("value");
                if (cursor.moveToFirst() && index != -1) {
                    result = cursor.getString(index);
                }
            }*/
            cursor.close();
        }
        Log.i("AlarmClock", "the real audio file's name is :" + result);
        Log.i("AlarmClock", "the audio's uri is : " + contentURI.toString());
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        for (int index = 0; index < getChildCount(); index++) {
            View v = getChildAt(index);
            v.measure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        for (int index = 0; index < getChildCount(); index++) {
            View v = getChildAt(index);
            v.layout(l, t, r, b);
            v.measure(r - l, b - t);
        }
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // TODO Auto-generated method stub
        if (setFromTimePickNow != true) {
            return;
        }
        /*if (hourOfDay > 12) {
            mHour = hourOfDay -12;
            //mSetAlarmView.isAm = false;
        } else if (hourOfDay == 12) {
            mHour = hourOfDay;
            //mSetAlarmView.isAm = true;
        } else {*/
        mHour = hourOfDay;
        // mSetAlarmView.isAm = true;
        // }

        // setAMorPMtext(mSetAlarmView.isAm);

        mMin = minute;
        // mSetAlarmView.setView(mHour, mMin);
        setFromTimePickNow = false;
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        // TODO Auto-generated method stub
        mHandler.removeCallbacks(mTimeUpdateThread);
        mHandler.postDelayed(mTimeUpdateThread, 500);
        setTimepickerTitle(hourOfDay, minute);

        if (hourOfDay >= 12) {
            mIsAm = false;
        } else {
            mIsAm = true;
        }
        /*if (hourOfDay > 12) {
            h = hourOfDay -12;
        } else if (hourOfDay == 12) {
            h = hourOfDay;
        } else {
            h = hourOfDay;
        }*/

        mMin = minute;
        mHour = hourOfDay;
        // if (DateFormat.is24HourFormat(mContext)) {
        // setTitle(((hourOfDay<10)?"0":"") + hourOfDay+":"+((minute<10)?"0":"")+minute);
        // } else {
        // setTitle(h+":"+((minute<10)?"0":"")+minute);
        // }
    }

    private void setTimepickerTitle(int hourOfDay, int minute) {
        mCalendar = Calendar.getInstance();
        long currentTimeMillis = mCalendar.getTimeInMillis();
        // if(hourOfDay < mCalendar.get(Calendar.HOUR_OF_DAY)||
        // ((hourOfDay == mCalendar.get(Calendar.HOUR_OF_DAY))&& (minute <= mCalendar.get(Calendar.MINUTE))))
        // hourOfDay += 24;
        DaysOfWeek tmpDaysOfWeek = new DaysOfWeek(mDaysOfWeek.getBitSet());
        int day = tmpDaysOfWeek.calculateDaysToNextAlarm(mCalendar);
        if (day < 0) {
            if (hourOfDay < mCalendar.get(Calendar.HOUR_OF_DAY)
                    || ((hourOfDay == mCalendar.get(Calendar.HOUR_OF_DAY)) && (minute <= mCalendar.get(Calendar.MINUTE))))
                hourOfDay += 24;
        } else if (day == 0) {
            tmpDaysOfWeek.setDaysOfWeek(false, mCalendar.get(Calendar.DAY_OF_WEEK));
            if (hourOfDay < mCalendar.get(Calendar.HOUR_OF_DAY)
                    || ((hourOfDay == mCalendar.get(Calendar.HOUR_OF_DAY)) && (minute <= mCalendar.get(Calendar.MINUTE)))) {
                if (!tmpDaysOfWeek.isRepeating())
                    hourOfDay += 7 * 24;
                else
                    hourOfDay += tmpDaysOfWeek.calculateDaysToNextAlarm(mCalendar) * 24;
            }
        } else {
            hourOfDay += day * 24;
        }
        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mCalendar.set(Calendar.MINUTE, minute);
        mCalendar.set(Calendar.SECOND, 0);
        mCalendar.set(Calendar.MILLISECOND, 0);
        long timeInMillis = mCalendar.getTimeInMillis();
        String toastText = formatText(mContext, timeInMillis, currentTimeMillis);
        // mTimepickertitle.setText(toastText);
    }

    private static String formatText(Context context, long timeInMillis, long currentTimeMillis) {
        long delta = timeInMillis - currentTimeMillis;
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" : (days == 1) ? context.getString(R.string.day) : context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" : (minutes == 1) ? context.getString(R.string.minute) : context.getString(R.string.minutes,
                Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" : (hours == 1) ? context.getString(R.string.hour) : context
                .getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) | (dispHour ? 2 : 0) | (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set_tmp);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    private void setDefaultWorkingDay() {
        boolean hasChecked = false;
        for (int i = 0; i < 7; i++) {
            if (repeatDaysArray[i]) {
                hasChecked = true;
            }
        }
        if (!hasChecked) {
            // enable from Mon to Fri
            for (int i = 1; i < 6; i++) {
                Button button = mButtonList.get(i);
                setButtonStatus(button, true, true);
                repeatDaysArray[i] = true;
            }
        }
    }
}
