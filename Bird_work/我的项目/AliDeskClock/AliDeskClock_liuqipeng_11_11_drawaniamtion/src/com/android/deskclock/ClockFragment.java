/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.deskclock;
//liuqipeng begin
import android.widget.ImageView;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.AnimationSet;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import java.util.Timer;
import java.util.TimerTask;
import android.text.format.Time;
//liuqipeng
import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;
import com.aliyun.ams.ta.TA;
import com.android.deskclock.alarms.AlarmNotifications;
import com.android.deskclock.worldclock.CitiesActivity;
import com.android.deskclock.worldclock.WorldClockAdapter;
import yunos.support.v4.app.Fragment;

/**
 * Fragment that shows  the clock (analog or digital), the next alarm info and the world clock.
 */
public class ClockFragment extends Fragment implements OnSharedPreferenceChangeListener, WorldClockRefreshableView.ListViewRefreshListener, 
                                WorldClockAdapter.OnShowOrHideListViewListener {

    private static final String BUTTONS_HIDDEN_KEY = "buttons_hidden";
    private final static String TAG = "ClockFragment";
//liuqipeng begin
	private ImageView mDotIndex;
	private ImageView mHourIndex;
	private ImageView mMinuteIndex;
	private float mHour;
	private float mMinute;
	private View mDragViewParent;
	private TextView mCurrentTime;
	private Handler mTimeHandler;
	private String time;
//liuqipeng
    private boolean mButtonsHidden = false;
    private View mDigitalClock;
    private WorldClockAdapter mAdapter;
    private ListView mList;
    private SharedPreferences mPrefs;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private String mDefaultClockStyle;
    private String mClockStyle;
    private static ClockFragment mInstance = null;
    //private ImageButton mDeleteBtn;
    private View mAddButton;
    private TextView mEmptyView;
    private View mSeparator;
    private View mDragView;
    private View mClockMainView;
    private View mClockListView;
    private WorldClockRefreshableView mRefreshableView;
    private WorldMyAnalogClock mWorldMyAnalogClock;
    //private ImageButton mSettingsButtom;
    private int mLayoutDirection = 0;
    private boolean mFirstStartFragment = false;
    private Context mContext;
    private boolean mBackFromAddNewCity = false;
    public static final int REQUEST_ADD_WORLDCLOCK = 3;
    private boolean mFromPageTransition;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean changed = action.equals(Intent.ACTION_TIME_CHANGED)
                    || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                    || action.equals(Intent.ACTION_LOCALE_CHANGED);
            if (changed) {
                //Utils.updateDate(mDateFormat, mDateFormatForAccessibility,mClockFrame);
                if (mAdapter != null) {
                    // *CHANGED may modify the need for showing the Home City
                    if (mAdapter.hasHomeCity() != mAdapter.needHomeCity()) {
                        mAdapter.reloadData(context);
                    } else {
                        mAdapter.notifyDataSetChanged();
                    }
                    // Locale change: update digital clock format and
                    // reload the cities list with new localized names
                    if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                        /*if (mDigitalClock != null) {
                            Utils.setTimeFormat(
                                   (TextClock)(mDigitalClock.findViewById(R.id.digital_clock)),
                                   (int)context.getResources().
                                           getDimension(R.dimen.bottom_text_size));
                        }*/
                        mAdapter.loadCitiesDb(context);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
            }
            /*if (changed || action.equals(AlarmNotifications.SYSTEM_ALARM_CHANGE_ACTION)) {
                Utils.refreshAlarm(getActivity(), mClockFrame);
            }*/
        }
    };

    private final Handler mHandler = new Handler();

    /*private final ContentObserver mAlarmObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            Utils.refreshAlarm(ClockFragment.this.getActivity(), mClockFrame);
        }
    };*/

    // Thread that runs on every quarter-hour and refreshes the date.
    private final Runnable mQuarterHourUpdater = new Runnable() {
        @Override
        public void run() {
            // Update the main and world clock dates
            //Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
            Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        }
    };

    public ClockFragment() {
        mInstance = this;
    }
//liuqipeng begin格式化小时，分钟，秒
	public String formatTime(int num){
		String s=null;
		if(num<10){
		s="0"+num;}
		else{
		s=""+num;}
		return s;
		}
	public void enableClockAnimation(){
		mWorldMyAnalogClock.enableClockAnimation();
	}
	public void startAnimation(){
//时针动画  分成两组第一组0~180度，alpha1.0~0.0 第二组180~360 alpha0.0~1.0   
		mHour=(mWorldMyAnalogClock.getmHour())/ 6.0f * 180;
		Animation mHourRotate=new RotateAnimation(mHour,mHour	+360f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
		mHourRotate.setDuration(1500);
		Animation mHourAlpha=AnimationUtils.loadAnimation(getActivity(), R.anim.analog_hour);
		AnimationSet setHour = new AnimationSet(true);
		setHour.addAnimation(mHourRotate);
		setHour.addAnimation(mHourAlpha);
		mHourIndex.startAnimation(setHour);
//分针动画
		mMinute=(mWorldMyAnalogClock.getmMinutes())/ 30.0f  * 180;
		RotateAnimation mMinuteRotate=new RotateAnimation(mMinute,mMinute+360f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
		mMinuteRotate.setDuration(1500);
		Animation mMinuteAlpha=AnimationUtils.loadAnimation(getActivity(), R.anim.analog_hour);
		AnimationSet setMinute = new AnimationSet(true);
		setMinute.addAnimation(mMinuteRotate);
		setMinute.addAnimation(mMinuteAlpha);
		mMinuteIndex.startAnimation(setMinute);
//dot动画
		Animation mDotAlpha=AnimationUtils.loadAnimation(getActivity(), R.anim.analog_hour);
		mDotIndex.startAnimation(mDotAlpha);
		mHourRotate.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
			}
		
			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
			
			}
		
			@Override
			public void onAnimationEnd(Animation animation) {
			}
		});
	}

	public void clearAllAnimation(){
		mHourIndex.clearAnimation();
		mMinuteIndex.clearAnimation();
		mDotIndex.clearAnimation();
	}
//liuqipeng
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle icicle) {
        Log.d("ClockFragment onCreateView start");
        // Inflate the layout for this fragment
        mContext = getActivity();
        mLayoutDirection = getResources().getConfiguration().getLayoutDirection();
        View v = inflater.inflate(R.layout.clock_fragment, container, false);
        if (icicle != null) {
            mButtonsHidden = icicle.getBoolean(BUTTONS_HIDDEN_KEY, false);
        }
        mList = (ListView)v.findViewById(R.id.cities);
        mList.setOverScrollMode(2);
        mList.setDivider(null);

        /*mDeleteBtn = (ImageButton)v.findViewById(R.id.delete_cities_button);
        mDeleteBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                TA.getInstance().getDefaultTracker().commitEvent("Page_ClockFragment",2101, "Page_ClockFragment_Button_DeleteClock", null, null, null);
                deleteClock();
            }
        });*/
        mAddButton = v.findViewById(R.id.cities_button);
        mAddButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                TA.getInstance().getDefaultTracker().commitEvent("Page_ClockFragment",2101, "Page_ClockFragment_Button_AddClock", null, null, null);
                startAddClock();
            }
        });

        mEmptyView = (TextView) v.findViewById(R.id.clock_empty_view);
        mSeparator = v.findViewById(R.id.separator);
        mDragView = v.findViewById(R.id.drag_view);
        mClockMainView = v.findViewById(R.id.clock_mainview);
        mClockListView = v.findViewById(R.id.clock_listview);
        mWorldMyAnalogClock = (WorldMyAnalogClock) v.findViewById(R.id.timezone_clock);
        mRefreshableView = (WorldClockRefreshableView) v.findViewById(R.id.pull_to_refresh_head);
        mRefreshableView.setListViewRefreshListener(this);
//liuqipeng begin
		mDotIndex=(ImageView)v.findViewById(R.id.world_dot_index);
		mHourIndex=(ImageView)v.findViewById(R.id.world_hour_index);
		mMinuteIndex=(ImageView)v.findViewById(R.id.world_minute_index);
		mDragViewParent=v.findViewById(R.id.drag_view_parent);
		mCurrentTime=(TextView)v.findViewById(R.id.current_time);
		mTimeHandler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				Time mTime = new Time();
				mTime.setToNow();
				int hour = mTime.hour;
				int minute = mTime.minute;
				int second = mTime.second;
				time = formatTime(hour) + ":" + formatTime(minute) + ":" + formatTime(second);
				mCurrentTime.setText(time);
				if(!getRefreshableView()){
				mDragView.setVisibility(View.GONE);
				mCurrentTime.setVisibility(View.VISIBLE);
				}
				else{
				mCurrentTime.setVisibility(View.GONE);
				mDragView.setVisibility(View.VISIBLE);
				}

			}
		};
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				mTimeHandler.sendEmptyMessage(0);
			}
		}, 50, 50);
//liuqipeng
        /*mSettingsButtom = (ImageButton)v.findViewById(R.id.menu_button);
        mSettingsButtom.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                TA.getInstance().getDefaultTracker().commitEvent("Page_ClockFragment",2101, "Page_ClockFragment_Button_SettingClock", null, null, null);
                settingsClock();
            }
        });*/

        /*OnTouchListener longPressNightMode = new OnTouchListener() {
            private float mMaxMovementAllowed = -1;
            private int mLongPressTimeout = -1;
            private float mLastTouchX, mLastTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mMaxMovementAllowed == -1) {
                    mMaxMovementAllowed = ViewConfiguration.get(getActivity()).getScaledTouchSlop();
                    mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
                }

                switch (event.getAction()) {
                    case (MotionEvent.ACTION_DOWN):
                        long time = Utils.getTimeNow();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(getActivity(), ScreensaverActivity.class));
                            }
                        }, mLongPressTimeout);
                        mLastTouchX = event.getX();
                        mLastTouchY = event.getY();
                        return true;
                    case (MotionEvent.ACTION_MOVE):
                        float xDiff = Math.abs(event.getX()-mLastTouchX);
                        float yDiff = Math.abs(event.getY()-mLastTouchY);
                        if (xDiff >= mMaxMovementAllowed || yDiff >= mMaxMovementAllowed) {
                            mHandler.removeCallbacksAndMessages(null);
                        }
                        break;
                    default:
                        mHandler.removeCallbacksAndMessages(null);
                }
                return false;
            }
        };*/

        // On tablet landscape, the clock frame will be a distinct view. Otherwise, it'll be added
        // on as a header to the main listview.
        //mClockFrame = inflater.inflate(R.layout.main_clock_frame, mList, false);
        //mClockFrame = v.findViewById(R.id.main_clock_frame);
        
        //mList.addHeaderView(mClockFrame, null, false);
        // The main clock frame needs its own touch listener for night mode now.
        //v.setOnTouchListener(longPressNightMode);

        //mList.setOnTouchListener(longPressNightMode);

        // If the current layout has a fake overflow menu button, let the parent
        // activity set up its click and touch listeners.

        //mDigitalClock = mClockFrame.findViewById(R.id.digital_clock);
        //Typeface dinpro = Typeface.createFromAsset(getActivity().getAssets(), "fonts/DINPro/DINPro-Light.otf");
        //((TextClock)(mDigitalClock.findViewById(R.id.digital_clock))).setTypeface(dinpro);
        //Utils.setHeadViewTimeFormat((TextClock)(mDigitalClock.findViewById(R.id.digital_clock)),
        //        (int)getResources().getDimension(R.dimen.bottom_text_size));
        View footerView = inflater.inflate(R.layout.blank_footer_view, mList, false);
        mList.addFooterView(footerView,null,false);
        mAdapter = new WorldClockAdapter(getActivity(),mList);
        mList.setAdapter(mAdapter);
        mAdapter.setOnShowOrHideListViewListener(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //mDefaultClockStyle = getActivity().getResources().getString(R.string.default_clock_style);
        mFirstStartFragment = true;
        mFromPageTransition = true;
        Log.d("ClockFragment onCreateView end");
        return v;
    }

    @Override
    public void onResume () {
        super.onResume();
        Log.d("ClockFragment onResume start");
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
        Activity activity = getActivity();
        Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        // Besides monitoring when quarter-hour changes, monitor other actions that
        // effect clock time
        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmNotifications.SYSTEM_ALARM_CHANGE_ACTION);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        activity.registerReceiver(mIntentReceiver, filter);
        // Resume can invoked after changing the cities list or a change in locale
        if (mAdapter != null && !mFirstStartFragment) {
            mAdapter.loadCitiesDb(activity);
            mAdapter.reloadData(activity);
        } else {
            mFirstStartFragment = false;
        }
        // Resume can invoked after changing the clock style.
        //View clockView = Utils.setClockStyle(activity, mDigitalClock,
        //        SettingsActivity.KEY_CLOCK_STYLE);
        mClockStyle = Utils.CLOCK_TYPE_DIGITAL;

        if (mAdapter.needHomeCity()){
            if(mAdapter.getCount() < 2) {
                //mDeleteBtn.setEnabled(false);
            } else {
                //mDeleteBtn.setEnabled(true);
            }
        } else {
            if(mAdapter.getCount() < 1) {
                mEmptyView.setVisibility(View.VISIBLE);
                mDragView.setVisibility(View.GONE);
//liuqipeng begin 新增		
				mDragViewParent.setVisibility(View.GONE);
//liuqipeng
                mSeparator.setVisibility(View.GONE);
//liuqipeng begin 注释掉
                //mList.setVisibility(View.GONE);
//liuqipeng
                //mDeleteBtn.setEnabled(false);
                mRefreshableView.showHeader(true);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mDragView.setVisibility(View.VISIBLE);
//liuqipeng begin 新增		
				mDragViewParent.setVisibility(View.VISIBLE);
//liuqipeng
                mSeparator.setVisibility(View.VISIBLE);
                mList.setVisibility(View.VISIBLE);
                //mDeleteBtn.setEnabled(true);
            }
        }

       if(mBackFromAddNewCity && mAdapter.getCount() > 2){
           mRefreshableView.showHeader(false);
           if(mRefreshableView.mCommonList!=null){
               mRefreshableView.mCommonList.setScrollable(true);
           }
           else{
               Log.d("mRefreshableView.mCommonList is null");
           }
        }
       if(mBackFromAddNewCity && mAdapter.getCount() >= 1){
           Utils.notify2EnableCloudService(mContext);
        }
        mBackFromAddNewCity = false;
        // Center the main clock frame if cities are empty.
//        if (getView().findViewById(R.id.main_clock_left_pane) != null && mAdapter.getCount() == 0) {
//            mList.setVisibility(View.GONE);
//        } else {
            //mList.setVisibility(View.VISIBLE);
//        }
        mAdapter.notifyDataSetChanged();
        //Utils.updateDate(mDateFormat, mDateFormatForAccessibility,mClockFrame);
        //Utils.refreshAlarm(activity, mClockFrame);
        //activity.getContentResolver().registerContentObserver(
        //        Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED),
        //        false,
        //        mAlarmObserver);
        if(mWorldMyAnalogClock != null){
            mWorldMyAnalogClock.updateTimeIndicator(true);
        }
        if(mFromPageTransition) {
            mList.setSelection(0);
            mFromPageTransition = false;
        }
        Log.d("ClockFragment onResume end");
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mEmptyView.setText(R.string.system_time);
    }

    public void removeWorldAnalogClock() {
        if (mWorldMyAnalogClock != null && mWorldMyAnalogClock.getVisibility() == View.VISIBLE) {
            mWorldMyAnalogClock.setVisibility(View.INVISIBLE);
        }
    }

    public void showWorldAnalogClock() {
        if (mWorldMyAnalogClock != null && mWorldMyAnalogClock.getVisibility() != View.VISIBLE) {
            mWorldMyAnalogClock.setVisibility(View.VISIBLE);
        }
    }

    private void deleteClock() {
        startActivity(new Intent(this.getActivity().getApplicationContext(), DeleteClockActivity.class));
    }

    private void startAddClock() {
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            ((Activity) mContext).startActivityForResult(new Intent(this.getActivity().getApplicationContext(), CitiesActivity.class), REQUEST_ADD_WORLDCLOCK);
        } else {
           ((Activity) mContext).startActivityForResult(new Intent(this.getActivity().getApplicationContext(), CitiesActivity.class), REQUEST_ADD_WORLDCLOCK);
        }
    }

    private void settingsClock() {
        startActivity(new Intent(this.getActivity().getApplicationContext(), SettingsActivity.class));
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        Utils.cancelQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        Activity activity = getActivity();
        activity.unregisterReceiver(mIntentReceiver);
        mWorldMyAnalogClock.updateTimeIndicator(false);
        //activity.getContentResolver().unregisterContentObserver(mAlarmObserver);
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        outState.putBoolean(BUTTONS_HIDDEN_KEY, mButtonsHidden);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        //if (key == SettingsActivity.KEY_CLOCK_STYLE) {
            //mClockStyle = prefs.getString(SettingsActivity.KEY_CLOCK_STYLE, mDefaultClockStyle);
            //mAdapter.notifyDataSetChanged();
        //}
    }

    public static ClockFragment getInstance(){
        if(mInstance == null) {
            mInstance = new ClockFragment();
        }
        return mInstance;
    }

    public void refreashView() {
        if (mRefreshableView != null) {
            mRefreshableView.refreashView();
        }
    }

    public void onDetach() {
        super.onDetach();
        mContext = null;
        mInstance = null;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mInstance = this;
    }

    public boolean getRefreshableView() {
        if (mRefreshableView != null) {
            return mRefreshableView.mDisPlayTwoItem;
        }
        return false;
    }

    public void onAnimationUpdate(float positionOffset, boolean isToAlarm) {
        ArgbEvaluator evaluator = new ArgbEvaluator();
//liuqipeng begin
        //int evaluate = isToAlarm ? (Integer) evaluator.evaluate(positionOffset, 0XFF357ECE, 0XFF38BA78)
        //        : (Integer) evaluator.evaluate(positionOffset, 0XFF38BA78, 0XFF624BC6);
  
        int evaluate = isToAlarm ? (Integer) evaluator.evaluate(positionOffset, 0XFF01ab4a, 0XFF009a96)
                : (Integer) evaluator.evaluate(positionOffset,0XFF009a96, 0XFF0093d1);
//liuqipeng
        if (mClockMainView != null) {
            mClockMainView.setBackgroundColor(evaluate);
        }
//liuqipeng begin 注释掉
/*
        if (mClockListView != null) {
            mClockListView.setBackgroundColor(evaluate);
        }
*/
//liuqipeng
    }

    @Override
    public void onListViewRefreshListener() {
        // TODO Auto-generated method stub
        mList.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onShowOrHideListView() {
        if(mAdapter.getCount() < 1) {
            mEmptyView.setVisibility(View.VISIBLE);
            mDragView.setVisibility(View.GONE);
//liuqipeng begin add		
			mDragViewParent.setVisibility(View.GONE);
//liuqipeng
            mSeparator.setVisibility(View.GONE);
//liuqipeng begin 注释掉
                //mList.setVisibility(View.GONE);
//liuqipeng
            mRefreshableView.showHeader(true);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mDragView.setVisibility(View.VISIBLE);
//liuqipeng begin add		
			mDragViewParent.setVisibility(View.VISIBLE);
//liuqipeng
            mSeparator.setVisibility(View.VISIBLE);
            mList.setVisibility(View.VISIBLE);
        }
    }

    public void asyncAddClock() {
        mBackFromAddNewCity = true;
    }
}
