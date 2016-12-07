package com.android.deskclock.timer;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.AlarmFeatureOption;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.R;
import com.android.deskclock.SetGroupView;

import android.R.color;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.timer.TimerClockView;
import com.android.deskclock.timer.TimerFragment.ClickAction;
import android.widget.NumberPicker;
import hwdroid.util.DialogUtils;
import com.aliyun.ams.ta.TA;
import android.graphics.Typeface;
import com.android.deskclock.Log;
import java.lang.reflect.Method;

public class TimerGroupView extends LinearLayout implements OnClickListener{

    public Context mContext;
    public AttributeSet mAttrs;
    public LayoutInflater mFactory;
    public TimerClockView mTimerClockView;
    public TextView mTimeText;
    //public TextView mDoneText;
    public TimerFragment mTimerFragment;
    public ImageView circle;

    private NumberPicker mHourNp;
    private NumberPicker mMinNp;
    private NumberPicker mSecondNp;
    private FrameLayout footer;
    private FrameLayout clockView;
    //private FrameLayout mDustWallpaperView;
    private View v;
    private View mTimerMainView;
    private View mTimerSetView;
    private TextView mTimerTextView;
    //private View footer_divider;
    private ImageView mMinutes1;
    private ImageView mMinutes2;
    private ImageView mMinutes3;
    private ImageView mMinutes4;
    private TextView mMinutes_10;
    private TextView mMinutes_15;
    private TextView mMinutes_30;
    private TextView mMinutes_45;
    private AnimationDrawable animation_10min;
    private AnimationDrawable animation_15min;
    private AnimationDrawable animation_30min;
    private AnimationDrawable animation_45min;
    private boolean mMinutesSelect1 = false;
    private boolean mMinutesSelect2 = false;
    private boolean mMinutesSelect3 = false;
    private boolean mMinutesSelect4 = false;
    private SoundPool mSoundPool;
    private int mSetQuickTimeStreamID;
    private SharedPreferences mPrefs;
    private static final String SELECT_TIME = "quickSettingsTime";

    public TimerGroupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAttrs = attrs;
        mFactory = LayoutInflater.from(context);
        v = LayoutInflater.from(context).inflate(R.layout.timer_group, null, false);
        mTimerMainView = v.findViewById(R.id.main);
        mTimerSetView = v.findViewById(R.id.timer_set);
        mTimerTextView = (TextView) v.findViewById(R.id.timer_text_top);
        mMinutes1 = (ImageView) v.findViewById(R.id.image_1);
        mMinutes2 = (ImageView) v.findViewById(R.id.image_2);
        mMinutes3 = (ImageView) v.findViewById(R.id.image_3);
        mMinutes4 = (ImageView) v.findViewById(R.id.image_4);
        mMinutes_10 = (TextView) v.findViewById(R.id.minutes_10);
        mMinutes_15 = (TextView) v.findViewById(R.id.minutes_15);
        mMinutes_30 = (TextView) v.findViewById(R.id.minutes_30);
        mMinutes_45 = (TextView) v.findViewById(R.id.minutes_45);
        animation_10min = (AnimationDrawable) mMinutes1.getDrawable();
        animation_15min = (AnimationDrawable) mMinutes2.getDrawable();
        animation_30min = (AnimationDrawable) mMinutes3.getDrawable();
        animation_45min = (AnimationDrawable) mMinutes4.getDrawable();
        resetAnimation();
        mMinutes1.setOnClickListener(this);
        mMinutes2.setOnClickListener(this);
        mMinutes3.setOnClickListener(this);
        mMinutes4.setOnClickListener(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        //clockView = (FrameLayout) v.findViewById(R.id.clock_view);
        //circle = (ImageView) v.findViewById(R.id.image_circle);
        mTimerClockView = (TimerClockView) v.findViewById(R.id.timer_circle);
        //mDustWallpaperView = (FrameLayout) v.findViewById(R.id.dustWallpaper);
        mTimeText = (TextView) v.findViewById(R.id.timer_textClock);
        Typeface dinpro = Typeface.createFromAsset(mContext.getAssets(), "fonts/DINPro/DINPro-Light.otf");
        mTimeText.getPaint().setFakeBoldText(true);
        mTimeText.setTypeface(dinpro);
        //mTimeText.setText("00:00:00");
        //mTimeText.setBackgroundColor(Color.argb(0, 0, 0, 0));
        //mTimeText.setSingleLine(true);
        //mTimeText.setGravity(Gravity.CENTER);
        /*mTimeText.setOnClickListener(new OnClickListener() {
           @Override
           public void onClick(View arg0) {
           if (mTimerFragment.mTimerObjTag != null &&
                       (mTimerFragment.mTimerObjTag.mState == TimerObj.STATE_RUNNING 
                       || mTimerFragment.mTimerObjTag.mState == TimerObj.STATE_TIMESUP
                       || mTimerFragment.mTimerObjTag.mState == TimerObj.STATE_DONE)) {
                    return;
                }
                TA.getInstance().getDefaultTracker().commitEvent("Page_TimerFragment",
                        2101, "control_setTimewithPicker", null, null, null);
                int seconds, minutes, hours;
                seconds = (int)mTimerClockView.getTime();
                minutes = seconds / 60;
                seconds = seconds - minutes * 60;
                hours = minutes / 60;
                minutes = minutes - hours * 60;
                if (hours > 999) {
                    hours = 0;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                View view = mFactory.inflate(R.layout.timer_number_picker_dialog, null);
                builder.setView(view).setTitle("00:00:00");
                builder.setNegativeButton(R.string.add_alarm_page_cacnel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton(R.string.add_alarm_page_confirm, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int which) {
                         mHourNp.clearFocus();
                         mMinNp.clearFocus();
                         mSecondNp.clearFocus();
                         setTime((mSecondNp.getValue()+mMinNp.getValue()*60+mHourNp.getValue()*3600)*1000);
                         dialog.dismiss();
                     }
                });
                final AlertDialog dialog = builder.create();
                DialogUtils.fromBottomToTop(dialog);
                dialog.show();
                
                mHourNp = (NumberPicker) view.findViewById(R.id.hours_picker);
                mHourNp.setMinValue(0);
                mHourNp.setMaxValue(99);
                mHourNp.setValue(hours);
                mHourNp.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        String hourStr = mHourNp.getValue()>9 ? Integer.toString(mHourNp.getValue()) : "0"+Integer.toString(mHourNp.getValue());
                        String minStr = mMinNp.getValue()>9 ? Integer.toString(mMinNp.getValue()) : "0"+Integer.toString(mMinNp.getValue());
                        String secStr = mSecondNp.getValue()>9 ? Integer.toString(mSecondNp.getValue()) : "0"+Integer.toString(mSecondNp.getValue());
                        dialog.setTitle(hourStr+":"+minStr+":"+secStr);
                    }
                });
                mMinNp = (NumberPicker) view.findViewById(R.id.mins_picker);
                mMinNp.setMinValue(0);
                mMinNp.setMaxValue(59);
                mMinNp.setValue(minutes);
                mMinNp.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        String hourStr = mHourNp.getValue()>9 ? Integer.toString(mHourNp.getValue()) : "0"+Integer.toString(mHourNp.getValue());
                        String minStr = mMinNp.getValue()>9 ? Integer.toString(mMinNp.getValue()) : "0"+Integer.toString(mMinNp.getValue());
                        String secStr = mSecondNp.getValue()>9 ? Integer.toString(mSecondNp.getValue()) : "0"+Integer.toString(mSecondNp.getValue());
                        dialog.setTitle(hourStr+":"+minStr+":"+secStr);
                    }
                });
                mSecondNp = (NumberPicker) view.findViewById(R.id.seconds_picker);
                mSecondNp.setMinValue(0);
                mSecondNp.setMaxValue(59);
                mSecondNp.setValue(seconds);
                mSecondNp.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        String hourStr = mHourNp.getValue()>9 ? Integer.toString(mHourNp.getValue()) : "0"+Integer.toString(mHourNp.getValue());
                        String minStr = mMinNp.getValue()>9 ? Integer.toString(mMinNp.getValue()) : "0"+Integer.toString(mMinNp.getValue());
                        String secStr = mSecondNp.getValue()>9 ? Integer.toString(mSecondNp.getValue()) : "0"+Integer.toString(mSecondNp.getValue());
                        dialog.setTitle(hourStr+":"+minStr+":"+secStr);
                    }
                });

                String hourStr = mHourNp.getValue()>9 ? Integer.toString(mHourNp.getValue()) : "0"+Integer.toString(mHourNp.getValue());
                String minStr = mMinNp.getValue()>9 ? Integer.toString(mMinNp.getValue()) : "0"+Integer.toString(mMinNp.getValue());
                String secStr = mSecondNp.getValue()>9 ? Integer.toString(mSecondNp.getValue()) : "0"+Integer.toString(mSecondNp.getValue());
                dialog.setTitle(hourStr+":"+minStr+":"+secStr);
            }
        });*/

        mTimerClockView.SetTimeTextView(mTimeText);

        //mDoneText = (TextView) v.findViewById(R.id.timer_done_textClock);
        //mTimeText.setBackgroundColor(Color.argb(0, 0, 0, 0));

        footer = (FrameLayout) v.findViewById(R.id.footer_button);
        //footer_divider =(View) v.findViewById(R.id.footer_divider);
        addView(v, 0);
    }

    public long getTime() {
        return mTimerClockView.getTime();
    }
    
    public void setFragment(TimerFragment fragment) {
        mTimerFragment = fragment;
        /*if (mTimerFragment.isFullScreen || (mTimerFragment.mTimerObjTag != null && mTimerFragment.mTimerObjTag.mState == TimerObj.STATE_TIMESUP)) {
            mDoneText.setVisibility(View.GONE);
        } else {
            mDoneText.setVisibility(View.GONE);
        }*/

        if (!mTimerFragment.isFullScreen) {
//liuqipeng 注释
            //footer.setBackgroundColor(color.white);
//liuqipeng 
            //footer_divider.setVisibility(View.VISIBLE);
            //circle.setVisibility(GONE);
            mTimerClockView.setBackGroundColor(color.white);
            mTimerClockView.setBackGroundColor(color.transparent);
            //mDustWallpaperView.setVisibility(View.GONE);
            final float scale = getResources().getDisplayMetrics().density;
            /*FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) clockView.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL;
            lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, (int) (40*scale+0.5f));
            clockView.setLayoutParams(lp);*/
        } else {
//liuqipeng 注释
            //footer.setBackgroundColor(color.transparent);
//liuqipeng 
            //footer_divider.setVisibility(View.INVISIBLE);
            //circle.setVisibility(VISIBLE);
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
            Drawable wallpaperDrawable = wallpaperManager.getDrawable();
            v.setBackground(wallpaperDrawable);
            //mDustWallpaperView.setVisibility(View.VISIBLE);
            mTimerClockView.setBackGroundColor(color.transparent);

            final float scale = getResources().getDisplayMetrics().density;
            /*FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) clockView.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL;
            lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, (int) (40*scale+0.5f));
            clockView.setLayoutParams(lp);*/
        }

        mTimerClockView.setGroupView(this);
    }

    public void setTime(long time){
        mTimerClockView.setTime(time);
    }

    public void startIntervalAnimation(long time){
        mTimerClockView.startIntervalAnimation(time);
    }

    public void startGuide(){
        mTimerClockView.startGuide();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
        super.onLayout(changed, l, t, r, b);
        for(int index = 0; index < getChildCount(); index++){
            View v = getChildAt(index);
            v.layout(l, t, r, b);
            v.measure(r-l, b-t);
        }
    }

    private boolean isTouchSoundEnabled() {
        /*try{
              Object obj = mContext.getSystemService(Context.AUDIOPROFILE_SERVICE);
              Class execClass = obj.getClass();
              Method getActiveProfileKeyM = execClass.getMethod("getActiveProfileKey");
              String key = (String) getActiveProfileKeyM.invoke(obj);
              Method getSoundEffectEnabledM = execClass.getMethod("getSoundEffectEnabled", String.class);
              return (Boolean) getSoundEffectEnabledM.invoke(obj, key);
         }
        catch (Exception e) {
            Log.e("" + e.toString(), e);
            return false;
       }*/
       return false;
      }

    @Override
    public void onClick(View v) {
        if (isMinutesSelect()) {
            resetDrawable();
        }
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            if (mSoundPool != null && isTouchSoundEnabled()) {
                mSoundPool.play(mSetQuickTimeStreamID, 1, 1, 0, 0, 1);
            }
        }
        else {
            if (mSoundPool != null) {
                mSoundPool.play(mSetQuickTimeStreamID, 1, 1, 0, 0, 1);
            }
        }
        SharedPreferences.Editor editor = mPrefs.edit();
        switch(v.getId()) {
        case R.id.image_1:
            editor.putInt(SELECT_TIME, 10);
            startIntervalAnimation(10 * 60 * 1000);
            mMinutesSelect1 = true;
            animation_10min.start();
            break;
        case R.id.image_2:
            editor.putInt(SELECT_TIME, 15);
            startIntervalAnimation(15 * 60 * 1000);
            mMinutesSelect2 = true;
            animation_15min.start();
            break;
        case R.id.image_3:
            editor.putInt(SELECT_TIME, 30);
            startIntervalAnimation(30 * 60 * 1000);
            mMinutesSelect3 = true;
            animation_30min.start();
            break;
        case R.id.image_4:
            editor.putInt(SELECT_TIME, 45);
            startIntervalAnimation(45 * 60 * 1000);
            mMinutesSelect4 = true;
            animation_45min.start();
            break;
        }
        editor.apply();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mMinutes_10.setText(R.string.timer_10minutes);
        mMinutes_15.setText(R.string.timer_15minutes);
        mMinutes_30.setText(R.string.timer_30minutes);
        mMinutes_45.setText(R.string.timer_45minutes);
    }

    public void onSelectTime() {
        int selectTime = mPrefs.getInt(SELECT_TIME, 0);
        switch (selectTime) {
            case 10:
                mMinutesSelect1 = true;
                animation_10min.start();
                break;
            case 15:
                mMinutesSelect2 = true;
                animation_15min.start();
                break;
            case 30:
                mMinutesSelect3 = true;
                animation_30min.start();
                break;
            case 45:
                mMinutesSelect4 = true;
                animation_45min.start();
                break;
        }
    }
    
    public void resetDrawable() {
        Log.d("resetDrawable");
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(SELECT_TIME, 0);
        editor.apply();
        mMinutesSelect1 = false;
        mMinutesSelect2 = false;
        mMinutesSelect3 = false;
        mMinutesSelect4 = false;
        resetAnimation();
    }

    private void resetAnimation() {
        animation_10min.selectDrawable(0);
        animation_10min.stop();
        animation_15min.selectDrawable(0);
        animation_15min.stop();
        animation_30min.selectDrawable(0);
        animation_30min.stop();
        animation_45min.selectDrawable(0);
        animation_45min.stop();
    }

    public boolean isMinutesSelect() {
        return mMinutesSelect1 || mMinutesSelect2 || mMinutesSelect3 || mMinutesSelect4;
    }

    public void setDrawableEnable(boolean bEnable) {
        mMinutes1.setEnabled(bEnable);
        mMinutes2.setEnabled(bEnable);
        mMinutes3.setEnabled(bEnable);
        mMinutes4.setEnabled(bEnable);
    }

    public void setClickSound (SoundPool soundPool, int setQuickTimeStreamID) {
        mSetQuickTimeStreamID = setQuickTimeStreamID;
        mSoundPool = soundPool;
    }

    public View getTimerGroupView() {
        return mTimerMainView;
    }

    public View getTimerSetView() {
        return mTimerSetView;
    }

    public View getTimerTextView() {
        return mTimerTextView;
    }
}
