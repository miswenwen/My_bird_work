/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.view.animation.RotateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView.ScaleType;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.AnimationSet;
import java.util.Timer;
import java.util.TimerTask;
import android.text.format.Time;
import android.widget.ToggleButton;
//liuqipeng
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentManager;
import yunos.support.v4.app.FragmentTransaction;
import yunos.support.v4.app.LoaderManager;
import yunos.support.v4.content.CursorLoader;
import yunos.support.v4.content.Loader;
import yunos.support.v4.widget.CursorAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;

import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.aliyun.ams.ta.TA;
//import com.android.datetimepicker.time.RadialPickerLayout;
//import com.android.datetimepicker.ticom.mediatek.deskclock.extme.TimePickerDialog;
import android.app.TimePickerDialog;
//import com.android.deskclock.RefreshableView.PullToRefreshListener;
import com.android.deskclock.alarms.AlarmActivity;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.alarms.PowerOffAlarm;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.widget.ActionableToastBar;
import com.android.deskclock.widget.TextTime;
import com.android.deskclock.SetGroupView;
import com.android.deskclock.AlarmClockFragment.AlarmItemAdapter.ItemHolder;

import java.io.File;
import java.lang.reflect.Field;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionSheet;
import hwdroid.widget.ActionSheet.ActionButton;

/**
 * AlarmClock application.
 */
public class AlarmClockFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        View.OnTouchListener, RefreshableView.ListViewRefreshListener
        {
    private static final float EXPAND_DECELERATION = 1f;
    private static final float COLLAPSE_DECELERATION = 0.7f;
    private static final int ANIMATION_DURATION = 300;
    private static final String KEY_EXPANDED_IDS = "expandedIds";
    private static final String KEY_REPEAT_CHECKED_IDS = "repeatCheckedIds";
    private static final String KEY_RINGTONE_TITLE_CACHE = "ringtoneTitleCache";
    private static final String KEY_SELECTED_ALARMS = "selectedAlarms";
    private static final String KEY_DELETED_ALARM = "deletedAlarm";
    private static final String KEY_UNDO_SHOWING = "undoShowing";
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private static final String KEY_SELECTED_ALARM = "selectedAlarm";
    private static final String KEY_DELETE_CONFIRMATION = "deleteConfirmation";
    public static final int REQUEST_ADD_ALARM = 1;
    public static final int REQUEST_UPDATE_ALARM = 2;
    private static final String KEY_DEFAULT_RINGTONE = "default_ringtone";

    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    //private RefreshableView mAlarmListHeader;
    private TextView mAlarmListHeaderText;
    private int mHeaderHeight;
    private ListView mAlarmsList;
    private AlarmItemAdapter mAdapter;

    private TextView mEmptyView;
//liuqipeng begin
	private View mDragParent;
	private TextView mCurrentTime;
	private Handler mHandler;
	private String time;
	private ImageView mDotIndex;
	private ImageView mHourIndex;
	private ImageView mMinuteIndex;
    private Handler handler;
	private Matrix mMatrix;
	private Bitmap bitmap;
	float mHour;
	float mMinute;
//liuqipeng
    private View mSeparator;
    private View mDragView;
    private RefreshableView mRefreshableView;
    private MyAnalogClock mMyAnalogClock;
    private View mAddAlarmButton;
    private View mAlarmsView;
    private View mTimelineLayout;
    private AlarmTimelineView mTimelineView;
    private View mFooterView;
    private View mAlarmMainView;
    private View mAlarmListView;
    private View mAlarmItemView;
    //private ImageButton mDeleteAlarmButton;
    //private ImageButton mSettingsAlarmButtom;
    public Bundle mRingtoneTitleCache; // Key: ringtone uri, value: ringtone title
    public boolean mFromCancel;
    private ActionableToastBar mUndoBar;
    private View mUndoFrame;

    public Alarm mSelectedAlarm;
    public long mScrollToAlarmId = -1;

    private Loader mCursorLoader = null;

    // Saved states for undo
    private Alarm mDeletedAlarm;
    private Alarm mAddedAlarm;
    private boolean mUndoShowing = false;
    private boolean enableOnTimeSetListener = false;

    private Animator mFadeIn;
    private Animator mFadeOut;

    private Interpolator mExpandInterpolator;
    private Interpolator mCollapseInterpolator;

    private int mTimelineViewWidth;
    private int mUndoBarInitialMargin;
    //private PullToRefreshListener mPulldownListener;
    private Typeface mDinproType;
    private Context mContext;

    // pull down states
    private final static int NONE_PULL_ADD = 0;   //正常状态
    private final static int ENTER_PULL_ADD = 1;  //进入下拉刷新状态
    private final static int OVER_PULL_ADD = 2;   //进入松手刷新状态
    private final static int EXIT_PULL_ADD = 3;   //松手后反弹后加载状态
    private int mCurrentScrollState  = -1;
    private int mPullAddState = 0;                //记录刷新状态
    private static AlarmClockFragment mInstance = null;
    private boolean mFromPageTransition;
    // Cached layout positions of items in listview prior to add/removal of alarm item
    private ConcurrentHashMap<Long, Integer> mItemIdTopMap = new ConcurrentHashMap<Long, Integer>();
    public AlarmClockFragment() {
        mInstance = this;
        // Basic provider required by Fragment.java
    }

    public static AlarmClockFragment getInstance(){
        if(mInstance == null) {
            mInstance = new AlarmClockFragment();
        }
        return mInstance;
    }
    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Log.d("AlarmClockFragment onCreat start");
        mCursorLoader = getLoaderManager().initLoader(0, null, this);
        mDinproType = Typeface.createFromAsset(getActivity().getAssets(), "fonts/DINPro/DINPro-Light.otf");
        ///M: get default alarm ringtone from the preference,
        // if there was no this item, just save system alarm ringtone to preference @{
        if (TextUtils.isEmpty(getDefaultRingtone(getActivity()))) {
            setSystemAlarmRingtoneToPref();
        }
        Log.d("AlarmClockFragment onCreat end");
    }

    private void deleteAlarms() {
        this.startActivity(new Intent(this.getActivity().getApplicationContext(), DeleteAlarmActivity.class));
    }
//liuqipeng  begin 格式化小时，分钟，秒
	public String formatTime(int num){
		String s=null;
		if(num<10){
		s="0"+num;}
		else{
		s=""+num;}
		return s;
		}
	public void enableClockAnimation(){
		mMyAnalogClock.enableClockAnimation();
	}

	public void startAnimation(){
//时针动画     
	mHour=(mMyAnalogClock.getmHour())/ 6.0f * 180;
	Animation mHourRotate=new RotateAnimation(mHour,mHour+360f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
	mHourRotate.setDuration(1500);
	Animation mHourAlpha=AnimationUtils.loadAnimation(getActivity(), R.anim.analog_hour);
	AnimationSet setHour = new AnimationSet(true);
	setHour.addAnimation(mHourRotate);
	setHour.addAnimation(mHourAlpha);
	mHourIndex.startAnimation(setHour);
//分针动画
	mMinute=(mMyAnalogClock.getmMinutes())/ 30.0f  * 180;
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
            Bundle savedState) {
        Log.d("AlarmClockFragment onCreateView start");
        // Inflate the layout for this fragment
        final ViewGroup v = (ViewGroup)inflater.inflate(R.layout.alarm_clock, container, false);

        //long[] expandedIds = null;
        long[] repeatCheckedIds = null;
        long[] selectedAlarms = null;
        Bundle previousDayMap = null;
        if (savedState != null) {
            //expandedIds = savedState.getLongArray(KEY_EXPANDED_IDS);
            repeatCheckedIds = savedState.getLongArray(KEY_REPEAT_CHECKED_IDS);
            mRingtoneTitleCache = savedState.getBundle(KEY_RINGTONE_TITLE_CACHE);
            mDeletedAlarm = savedState.getParcelable(KEY_DELETED_ALARM);
            selectedAlarms = savedState.getLongArray(KEY_SELECTED_ALARMS);
            previousDayMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
            mSelectedAlarm = savedState.getParcelable(KEY_SELECTED_ALARM);
        }
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            if (mRingtoneTitleCache == null) {
                mRingtoneTitleCache = new Bundle();
            }
        }

        //mExpandInterpolator = new DecelerateInterpolator(EXPAND_DECELERATION);
        //mCollapseInterpolator = new DecelerateInterpolator(COLLAPSE_DECELERATION);

        //TODO penglei : add head layout for the list
        /*mAlarmListHeader = (RefreshableView) v.findViewById(R.id.pull_to_refresh_head);
        mPulldownListener = new PullToRefreshListener() {
            @Override
            public void onRefresh() {
                TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment",
                        2101, "Button_pullToAddAlarm", null, null, null);
                hideUndoBar(true, null);
                startCreatingAlarm();
                mAlarmListHeader.finishRefreshing();
            }
        };

        mAlarmListHeader.setOnRefreshListener(mPulldownListener, 0);*/
        mAddAlarmButton = v.findViewById(R.id.add_Alarm);
        mAddAlarmButton.setEnabled(false);
        mAddAlarmButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment",
                         2101, "button_addAlarm", null, null, null);
                startCreatingAlarm();
				Log.i("ClockDataStorage,you clicked addAlarm_btn");//liuqipeng add
            }
        });
        /*mDeleteAlarmButton = (ImageButton) v.findViewById(R.id.delete_Alarm);
        mDeleteAlarmButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment",
                        2101, "button_deleteAlarm", null, null, null);
                deleteAlarms();
            }
        });*/

        /*mSettingsAlarmButtom = (ImageButton) v.findViewById(R.id.menu_button_alarmpage);
        mSettingsAlarmButtom.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment",2101, "Page_AlarmClockFragment_Button_SettingAlarmClock", null, null, null);
                settingsAlarm();
            }
        });*/

/*        mAddAlarmButton = (ImageButton) v.findViewById(R.id.alarm_add_alarm_icon);
        mAddAlarmButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideUndoBar(true, null);
                startCreatingAlarm();
            }
        });*/
        // For landscape, put the add button on the right and the menu in the actionbar.
        /*FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) mAddAlarmButton.getLayoutParams();
        boolean isLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            layoutParams.gravity = Gravity.END;
        } else {
            layoutParams.gravity = Gravity.CENTER;
        }
        mAddAlarmButton.setLayoutParams(layoutParams);*/

//liuqipeng begin
		mDotIndex=(ImageView)v.findViewById(R.id.analog_dot_index);
		mHourIndex=(ImageView)v.findViewById(R.id.analog_hour_index);
		mMinuteIndex=(ImageView)v.findViewById(R.id.analog_minute_index);
		mCurrentTime=(TextView)v.findViewById(R.id.current_time);
		mDragParent=v.findViewById(R.id.drag_view_parent);
		mHandler = new Handler() {
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
				mHandler.sendEmptyMessage(0);
			}
		}, 50, 50);           
//liuqipeng
        mEmptyView = (TextView) v.findViewById(R.id.alarms_empty_view);
        mSeparator = v.findViewById(R.id.separator);
        mDragView = v.findViewById(R.id.drag_view);
        mAlarmMainView = v.findViewById(R.id.main);
        mAlarmListView = v.findViewById(R.id.alarm_listview);
        mMyAnalogClock = (MyAnalogClock) v.findViewById(R.id.timezone_clock);
        mRefreshableView = (RefreshableView) v.findViewById(R.id.pull_to_refresh_head);
        mRefreshableView.setListViewRefreshListener(this);
        /*mEmptyView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startCreatingAlLog.arm();
            }
        });*/
        mAlarmsList = (ListView) v.findViewById(R.id.alarms_list);
        mAlarmsList.setOverScrollMode(2);
        //View alarmFrame = inflater.inflate(R.layout.main_alarm_frame, mAlarmsList, false);
        //mAlarmsList.addHeaderView(alarmFrame, null, false);

        /*mFadeIn = AnimatorInflater.loadAnimator(getActivity(), R.anim.fade_in);
        mFadeIn.setDuration(ANIMATION_DURATION);
        mFadeIn.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                mEmptyView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Do nothing.
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Do nothing.
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Do nothing.
            }
        });
        mFadeIn.setTarget(mEmptyView);
        mFadeOut = AnimatorInflater.loadAnimator(getActivity(), R.anim.fade_out);
        mFadeOut.setDuration(ANIMATION_DURATION);
        mFadeOut.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                mEmptyView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
                // Do nothing.
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                mEmptyView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
                // Do nothing.
            }
        });
        mFadeOut.setTarget(mEmptyView);*/
//        mAlarmsView = v.findViewById(R.id.alarm_layout);
//        mTimelineLayout = v.findViewById(R.id.timeline_layout);

/*        mUndoBar = (ActionableToastBar) v.findViewById(R.id.undo_bar);
        mUndoBarInitialMargin = getActivity().getResources()
                .getDimensionPixelOffset(R.dimen.alarm_undo_bar_horizontal_margin);
        mUndoFrame = v.findViewById(R.id.undo_frame);
        mUndoFrame.setOnTouchListener(this);*/

        /*mFooterView = v.findViewById(R.id.alarms_footer_view);
        mFooterView.setOnTouchListener(this);*/

        // Timeline layout only exists in tablet landscape mode for now.
//        if (mTimelineLayout != null) {
//            mTimelineView = (AlarmTimelineView) v.findViewById(R.id.alarm_timeline_view);
//            mTimelineViewWidth = getActivity().getResources()
//                    .getDimensionPixelOffset(R.dimen.alarm_timeline_layout_width);
//        }

        mAdapter = new AlarmItemAdapter(getActivity(),
                 repeatCheckedIds, selectedAlarms, previousDayMap, mAlarmsList);
        mAdapter.registerDataSetObserver(new DataSetObserver() {

            private int prevAdapterCount = -1;

            @Override
            public void onChanged() {

                final int count = mAdapter.getCount();
                if (mDeletedAlarm != null && prevAdapterCount > count) {
                    //showUndoBar();
                }

                // If there are no alarms in the adapter...
                if (count == 0) {
                    //mAddAlarmButton.setBackgroundResource(R.drawable.main_button_red);

                    // ...and if there exists a timeline view (currently only in tablet landscape)
                    if (mTimelineLayout != null && mAlarmsView != null) {

//                        // ...and if the previous adapter had alarms (indicating a removal)...
//                        if (prevAdapterCount > 0) {
//
//                            // Then animate in the "no alarms" icon...
//                            mFadeIn.start();
//
//                            // and animate out the alarm timeline view, expanding the width of the
//                            // alarms list / undo bar.
//                            mTimelineLayout.setVisibility(View.VISIBLE);
//                            ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f)
//                                    .setDuration(ANIMATION_DURATION);
//                            animator.setInterpolator(mCollapseInterpolator);
//                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                                @Override
//                                public void onAnimationUpdate(ValueAnimator animator) {
//                                    Float value = (Float) animator.getAnimatedValue();
//                                    int currentTimelineWidth = (int) (value * mTimelineViewWidth);
//                                    float rightOffset = mTimelineViewWidth * (1 - value);
//                                    mTimelineLayout.setTranslationX(rightOffset);
//                                    mTimelineLayout.setAlpha(value);
//                                    mTimelineLayout.requestLayout();
//                                   /* setUndoBarRightMargin(currentTimelineWidth
//                                            + mUndoBarInitialMargin);*/
//                                }
//                            });
//                            animator.addListener(new AnimatorListener() {
//
//                                @Override
//                                public void onAnimationCancel(Animator animation) {
//                                    // Do nothing.
//                                }
//
//                                @Override
//                                public void onAnimationEnd(Animator animation) {
//                                    mTimelineView.setIsAnimatingOut(false);
//                                }
//
//                                @Override
//                                public void onAnimationRepeat(Animator animation) {
//                                    // Do nothing.
//                                }
//
//                                @Override
//                                public void onAnimationStart(Animator animation) {
//                                    mTimelineView.setIsAnimatingOut(true);
//                                }
//
//                            });
//                            animator.start();
//                        } else {
//                            // If the previous adapter did not have alarms, no animation needed,
//                            // just hide the timeline view and show the "no alarms" icon.
//                            mTimelineLayout.setVisibility(View.GONE);
//                            mEmptyView.setVisibility(View.VISIBLE);
//                            mDeleteAlarmButton.setEnabled(false);
//                            //setUndoBarRightMargin(mUndoBarInitialMargin);
//                        }
                    } else {

                        // If there is no timeline view, just show the "no alarms" icon.
                        mEmptyView.setVisibility(View.VISIBLE);
                        mSeparator.setVisibility(View.GONE);
//liuqipeng 注销掉
                        //mAlarmsList.setVisibility(View.GONE);
//liuqipeng
                        mDragView.setVisibility(View.GONE);
//liuqipeng begin 新增
						mDragParent.setVisibility(View.GONE);
//liuqipeng
                        mRefreshableView.showHeader(true);
                        mAddAlarmButton.setEnabled(true);
                        //mDeleteAlarmButton.setEnabled(false);
                    }
                } else {

                    // Otherwise, if the adapter DOES contain alarms...
                    //mAddAlarmButton.setBackgroundResource(R.drawable.main_button_normal);

                    // ...and if there exists a timeline view (currently in tablet landscape mode)
                    if (mTimelineLayout != null && mAlarmsView != null) {
//
//                        mTimelineLayout.setVisibility(View.VISIBLE);
//                        // ...and if the previous adapter did not have alarms (indicating an add)
//                        if (prevAdapterCount == 0) {
//
//                            // Then, animate to hide the "no alarms" icon...
//                            mFadeOut.start();
//
//                            // and animate to show the timeline view, reducing the width of the
//                            // alarms list / undo bar.
//                            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
//                                    .setDuration(ANIMATION_DURATION);
//                            animator.setInterpolator(mExpandInterpolator);
//                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                                @Override
//                                public void onAnimationUpdate(ValueAnimator animator) {
//                                    Float value = (Float) animator.getAnimatedValue();
//                                    int currentTimelineWidth = (int) (value * mTimelineViewWidth);
//                                    float rightOffset = mTimelineViewWidth * (1 - value);
//                                    mTimelineLayout.setTranslationX(rightOffset);
//                                    mTimelineLayout.setAlpha(value);
//                                    mTimelineLayout.requestLayout();
//                                    ((FrameLayout.LayoutParams) mAlarmsView.getLayoutParams())
//                                        .setMargins(0, 0, (int) -rightOffset, 0);
//                                    mAlarmsView.requestLayout();
//                                    //setUndoBarRightMargin(currentTimelineWidth
//                                            //+ mUndoBarInitialMargin);
//                                }
//                            });
//                            animator.start();
//                        } else {
//                            mTimelineLayout.setVisibility(View.VISIBLE);
//                            mEmptyView.setVisibility(View.GONE);
//                            mDeleteAlarmButton.setEnabled(true);
//                            //setUndoBarRightMargin(mUndoBarInitialMargin + mTimelineViewWidth);
//                        }
                    } else {

                        // If there is no timeline view, just hide the "no alarms" icon.
                        mEmptyView.setVisibility(View.GONE);
                        mDragView.setVisibility(View.VISIBLE);
//liuqipeng begin　新增
						mDragParent.setVisibility(View.VISIBLE);
//liuqipeng
                        mSeparator.setVisibility(View.VISIBLE);
                        mAlarmsList.setVisibility(View.VISIBLE);
                        //mDeleteAlarmButton.setEnabled(true);
                    }
                }

                // Cache this adapter's count for when the adapter changes.
                prevAdapterCount = count;
                super.onChanged();
            }
        });
        if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
            if (mRingtoneTitleCache == null) {
                mRingtoneTitleCache = new Bundle();
            }
        }
        View footerView = inflater.inflate(R.layout.blank_footer_view, mAlarmsList, false);
        mAlarmsList.addFooterView(footerView,null,false);
        mAlarmsList.setAdapter(mAdapter);
        //mAlarmsList.setVerticalScrollBarEnabled(true);
        //mAlarmsList.setOnCreateContextMenuListener(this);

        //if (mUndoShowing) {
            //showUndoBar();
        //}
        mFromPageTransition = true;
        Log.d("AlarmClockFragment onCreateView end");
        //mSetAlarmPage = (SetGroupView)v.findViewById(R.id.fragment_set_alarm);
        //mSetAlarmPage.setVisibility(View.INVISIBLE);
        //mSetAlarmPage.setFragment(this);
        return v;
    }


    public void removeAnalogClock() {
        if (mMyAnalogClock != null && mMyAnalogClock.getVisibility() == View.VISIBLE) {
            mMyAnalogClock.setVisibility(View.INVISIBLE);
        }
    }

    public void showAnalogClock() {
        if (mMyAnalogClock != null && mMyAnalogClock.getVisibility() != View.VISIBLE) {
            mMyAnalogClock.setVisibility(View.VISIBLE);
        }
    }

    private void measureView(View v){
        ViewGroup.LayoutParams p  = v.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
                    MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
        }
        v.measure(childWidthSpec, childHeightSpec);
    }
    /*private void setUndoBarRightMargin(int margin) {
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) mUndoBar.getLayoutParams();
        ((FrameLayout.LayoutParams) mUndoBar.getLayoutParams())
            .setMargins(params.leftMargin, params.topMargin, margin, params.bottomMargin);
        mUndoBar.requestLayout();
    }*/

    @Override
    public void onResume() {
        super.onResume();
        Log.d("AlarmClockFragment onResume start");
        // Check if another app asked us to create a blank new alarm.
        final Intent intent = getActivity().getIntent();
        if (intent.hasExtra(ALARM_CREATE_NEW_INTENT_EXTRA)) {
            if (intent.getBooleanExtra(ALARM_CREATE_NEW_INTENT_EXTRA, false)) {
                // An external app asked us to create a blank alarm.
                startCreatingAlarm();
            }

            // Remove the CREATE_NEW extra now that we've processed it.
            intent.removeExtra(ALARM_CREATE_NEW_INTENT_EXTRA);
        } else if (intent.hasExtra(SCROLL_TO_ALARM_INTENT_EXTRA)) {
            long alarmId = intent.getLongExtra(SCROLL_TO_ALARM_INTENT_EXTRA, Alarm.INVALID_ID);
            if (alarmId != Alarm.INVALID_ID) {
                mScrollToAlarmId = alarmId;
                if (mCursorLoader != null && mCursorLoader.isStarted()) {
                    // We need to force a reload here to make sure we have the latest view
                    // of the data to scroll to.
                    mCursorLoader.forceLoad();
                }
            }

            // Remove the SCROLL_TO_ALARM extra now that we've processed it.
            intent.removeExtra(SCROLL_TO_ALARM_INTENT_EXTRA);
        }

        // Make sure to use the child FragmentManager. We have to use that one for the
        // case where an intent comes in telling the activity to load the timepicker,
        // which means we have to use that one everywhere so that the fragment can get
        // correctly picked up here if it's open.
        /*TimePickerDialog tpd = (TimePickerDialog) getChildFragmentManager().
                findFragmentByTag(AlarmUtils.FRAG_TAG_TIME_PICKER);
        if (tpd != null) {
            // The dialog is already open so we need to set the listener again.
            tpd.setOnTimeSetListener(this);
        }*/

        DeskClock deskClock = (DeskClock) this.getActivity();
        deskClock.mViewPager.setScrollable(true);
        mMyAnalogClock.updateTimeIndicator(true);
        if(mFromPageTransition){
            mAlarmsList.setSelection(0);
            mFromPageTransition = false;
        }
        Log.d("AlarmClockFragment onResume end");
    }

    private void hideUndoBar(boolean animate, MotionEvent event) {
        /*if (mUndoBar != null) {
            mUndoFrame.setVisibility(View.GONE);
            if (event != null && mUndoBar.isEventInToastBar(event)) {
                // Avoid touches inside the undo bar.
                return;
            }
            mUndoBar.hide(animate);
        }
        mDeletedAlarm = null;
        mUndoShowing = false;*/
    }

    private void showUndoBar() {
        /*mUndoFrame.setVisibility(View.VISIBLE);
        mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
            @Override
            public void onActionClicked() {
                asyncAddAlarm(mDeletedAlarm);
                mDeletedAlarm = null;
                mUndoShowing = false;
            }
        }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo, true);*/
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mEmptyView.setText(R.string.no_alarms);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putLongArray(KEY_EXPANDED_IDS, mAdapter.getExpandedArray());
        //outState.putLongArray(KEY_REPEAT_CHECKED_IDS, mAdapter.getRepeatArray());
        //outState.putLongArray(KEY_SELECTED_ALARMS, mAdapter.getSelectedAlarmsArray());
        outState.putBundle(KEY_RINGTONE_TITLE_CACHE, mRingtoneTitleCache);
        outState.putParcelable(KEY_DELETED_ALARM, mDeletedAlarm);
        //outState.putBoolean(KEY_UNDO_SHOWING, mUndoShowing);
        outState.putBundle(KEY_PREVIOUS_DAY_MAP, mAdapter.getPreviousDaysOfWeekMap());
        outState.putParcelable(KEY_SELECTED_ALARM, mSelectedAlarm);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the user places the app in the background by pressing "home",
        // dismiss the toast bar. However, since there is no way to determine if
        // home was pressed, just dismiss any existing toast bar when restarting
        // the app.
        //hideUndoBar(false, null);
        mMyAnalogClock.updateTimeIndicator(false);
    }

    // Callback used by TimePickerDialog

    public void setNewAlarm(Alarm alarm, int hourOfDay, int minute, Uri alert, boolean vibrate, DaysOfWeek daysOfWeek, String label) {
        if (alarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            Alarm a = new Alarm();
            a.alert = alert;

            if (a.alert == null) {
                a.alert = RingtoneManager.getActualDefaultRingtoneUri(getActivity(),RingtoneManager.TYPE_ALARM);
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
            asyncAddAlarm(a);
        } else {
            alarm.hour = hourOfDay;
            alarm.minutes = minute;
            alarm.alert = alert;
            alarm.vibrate = vibrate;
            alarm.label = label;
            alarm.daysOfWeek = daysOfWeek;
            alarm.enabled = true;
            mScrollToAlarmId = alarm.id;
            asyncUpdateAlarm(alarm, true);
            mSelectedAlarm = null;
        }
    }

    private void showLabelDialog(final Alarm alarm) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(alarm, alarm.label, getTag());
        ft.add(newFragment, "label_dialog");
        ft.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
    }

    public void setLabel(Alarm alarm, String label) {
        alarm.label = label;
        asyncUpdateAlarm(alarm, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor data) {
        mAdapter.swapCursor(data);
        if (mRefreshableView != null && !mRefreshableView.mDisPlayTwoItem && mScrollToAlarmId != -1) {
            scrollToAlarm(mScrollToAlarmId);
            mScrollToAlarmId = -1;
        }
    }

    /**
     * Scroll to alarm with given alarm id.
     *
     * @param alarmId The alarm id to scroll to.
     */
    private void scrollToAlarm(long alarmId) {
        int alarmPosition = -1;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            long id = mAdapter.getItemId(i);
            if (id == alarmId) {
                alarmPosition = i;
                break;
            }
        }

        if (alarmPosition >= 0) {
            mAdapter.setNewAlarm(alarmId);
            mAlarmsList.smoothScrollToPositionFromTop(alarmPosition, 0);
        } else {
            // Trying to display a deleted alarm should only happen from a missed notification for
            // an alarm that has been marked deleted after use.
            Toast toast = Toast.makeText(mContext, R.string.missed_alarm_has_been_deleted,
                    Toast.LENGTH_LONG);
            ToastMaster.setToast(toast);
            toast.show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    private void launchRingTonePicker(Alarm alarm) {
        mSelectedAlarm = alarm;
        Uri oldRingtone = Alarm.NO_RINGTONE_URI.equals(alarm.alert) ? null : alarm.alert;
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, oldRingtone);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        startActivityForResult(intent, REQUEST_ADD_ALARM);
    }

    private void saveRingtoneUri(Intent intent) {
        Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) {
            uri = Alarm.NO_RINGTONE_URI;
        }
        mSelectedAlarm.alert = uri;

        // Save the last selected ringtone as the default for new alarms
        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
            ///M: Don't set the ringtone to the system, just to the preference @{
            //RingtoneManager.setActualDefaultRingtoneUri(
            //        getActivity(), RingtoneManager.TYPE_ALARM, uri);
            setDefaultRingtone(uri.toString());
            ///@}
            Log.v("saveRingtoneUri = " + uri.toString());
        }
        asyncUpdateAlarm(mSelectedAlarm, false);
    }

    public class AlarmItemAdapter extends CursorAdapter {
        private static final int EXPAND_DURATION = 300;
        private static final int COLLAPSE_DURATION = 250;

        private final Context mContext;
        private final LayoutInflater mFactory;
        private final String[] mShortWeekDayStrings;
        private final String[] mLongWeekDayStrings;
        private final int mColorLit;
        private final int mColorDim;
        // private final int mBackgroundColorExpanded;
        // private final int mBackgroundColor;
        private final Typeface mRobotoNormal;
        private final Typeface mRobotoBold;
        private final ListView mList;

        //private final HashSet<Long> mExpanded = new HashSet<Long>();
        private final HashSet<Long> mRepeatChecked = new HashSet<Long>();
        private final HashSet<Long> mSelectedAlarms = new HashSet<Long>();
        private Bundle mPreviousDaysOfWeekMap = new Bundle();

        private final boolean mHasVibrator;
        private final int mCollapseExpandHeight;

        // This determines the order in which it is shown and processed in the UI.
        private final int[] DAY_ORDER = new int[] {
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
        };

        public class ItemHolder {

            // views for optimization
            FrameLayout alarmItem;
            TextTime clock;
//liuqipeng begin
			//Switch onoff;
            ToggleButton onoff;
//liuqipeng
            TextView daysOfWeek;
            TextView label;
            //ImageView delete;
            //View expandArea;
            View summary;
            //TextView clickableLabel;
            //CheckBox repeat;
            FrameLayout repeatDays;
            //CheckBox vibrate;
            //TextView ringtone;
            //View hairLine;
            //View arrow;
            //View collapseExpandArea;
            //View footerFiller;

            // Other states
            Alarm alarm;
            LinearLayout onoffSwitch;
        }

        // Used for scrolling an expanded item in the list to make sure it is fully visible.
        private long mScrollAlarmId = -1;
        private final Runnable mScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (mScrollAlarmId != -1) {
                    View v = getViewById(mScrollAlarmId);
                    if (v != null) {
                        Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        mList.requestChildRectangleOnScreen(v, rect, false);
                    }
                    mScrollAlarmId = -1;
                }
            }
        };

        public AlarmItemAdapter(Context context, long[] repeatCheckedIds,
                long[] selectedAlarms, Bundle previousDaysOfWeekMap, ListView list) {
            super(context, null, 0);
            mContext = context;
            mFactory = LayoutInflater.from(context);
            mList = list;

            DateFormatSymbols dfs = new DateFormatSymbols();
            mShortWeekDayStrings = dfs.getShortWeekdays();
            mLongWeekDayStrings = dfs.getWeekdays();

            Resources res = mContext.getResources();
            mColorLit = res.getColor(R.color.aui_green);
            mColorDim = res.getColor(R.color.grey);
            // mBackgroundColorExpanded = res.getColor(R.color.alarm_whiteish);
            // mBackgroundColor = res.getColor(R.color.white);
            mRobotoBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mRobotoNormal = Typeface.create("sans-serif-condensed", Typeface.NORMAL);

            //if (expandedIds != null) {
              //  buildHashSetFromArray(expandedIds, mExpanded);
            //}
            if (repeatCheckedIds != null) {
                buildHashSetFromArray(repeatCheckedIds, mRepeatChecked);
            }
            if (previousDaysOfWeekMap != null) {
                mPreviousDaysOfWeekMap = previousDaysOfWeekMap;
            }
            if (selectedAlarms != null) {
                buildHashSetFromArray(selectedAlarms, mSelectedAlarms);
            }

            mHasVibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .hasVibrator();

            mCollapseExpandHeight = (int) res.getDimension(R.dimen.collapse_expand_height);
        }

        public void removeSelectedId(long id) {
            mSelectedAlarms.remove(id);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.v("position="+position);
            if (!getCursor().moveToPosition(position)) {
                // May happen if the last alarm was deleted and the cursor refreshed while the
                // list is updated.
                Log.v("couldn't move cursor to position " + position);
                return null;
            }
            Cursor cursor = getCursor();
            View v;
            if (convertView == null) {
                v = newView(mContext, getCursor(), parent);
            } else {
                // TODO temporary hack to prevent the convertView from not having stuff we need.
                boolean badConvertView = convertView.findViewById(R.id.digital_clock) == null;
                // Do a translation check to test for animation. Change this to something more
                // reliable and robust in the future.
                if (convertView.getTranslationX() != 0 || convertView.getTranslationY() != 0 ||
                        badConvertView) {
                    ///M: Before create a new view, reset it to null and unregister observer @{
                    //((ItemHolder) convertView.getTag()).clock.unregisterObserver();
                    convertView = null;
                    ///@}
                    // view was animated, reset
                    v = newView(mContext, getCursor(), parent);
                } else {
                    v = convertView;
                }
            }
            if(cursor.isAfterLast())
            {
                return null;
            }
            bindView(v, mContext, cursor);
            ItemHolder holder = (ItemHolder) v.getTag();

            if(cursor.isLast()||(position==2)){
                mAddAlarmButton.setEnabled(true);
            }
            // We need the footer for the last element of the array to allow the user to scroll
            // the item beyond the bottom button bar, which obscures the view.
            //holder.footerFiller.setVisibility(position < getCount() - 1 ? View.GONE : View.VISIBLE);

            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mFactory.inflate(R.layout.alarm_time, parent, false);
            setNewHolder(view);
            return view;
        }

        /**
         * In addition to changing the data set for the alarm list, swapCursor is now also
         * responsible for preparing the list view's pre-draw operation for any animations that
         * need to occur if an alarm was removed or added.
         */
        @Override
        public synchronized Cursor swapCursor(Cursor cursor) {
            Cursor c = super.swapCursor(cursor);

            if (mItemIdTopMap.isEmpty() && mAddedAlarm == null) {
                return c;
            }

            final ListView list = mAlarmsList;
            final ViewTreeObserver observer = list.getViewTreeObserver();

            /*
             * Add a pre-draw listener to the observer to prepare for any possible animations to
             * the alarms within the list view.  The animations will occur if an alarm has been
             * removed or added.
             *
             * For alarm removal, the remaining children should all retain their initial starting
             * positions, and transition to their new positions.
             *
             * For alarm addition, the other children should all retain their initial starting
             * positions, transition to their new positions, and at the end of that transition, the
             * newly added alarm should appear in the designated space.
             */
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                private View mAddedView;

                @Override
                public boolean onPreDraw() {
                    // Remove the pre-draw listener, as this only needs to occur once.
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }
                    boolean firstAnimation = true;
                    int firstVisiblePosition = list.getFirstVisiblePosition();

                    // Iterate through the children to prepare the add/remove animation.
                    for (int i = 0; i< list.getChildCount(); i++) {
                        final View child = list.getChildAt(i);

                        int position = firstVisiblePosition + i;
                        long itemId = mAdapter.getItemId(position);

                        // If this is the added alarm, set it invisible for now, and animate later.
                        if (mAddedAlarm != null && itemId == mAddedAlarm.id) {
                            mAddedView = child;
                            mAddedView.setAlpha(0.0f);
                            continue;
                        }

                        // The cached starting position of the child view.
                        Integer startTop = mItemIdTopMap.get(itemId);
                        // The new starting position of the child view.
                        int top = child.getTop();

                        // If there is no cached starting position, determine whether the item has
                        // come from the top of bottom of the list view.
                        if (startTop == null) {
                            int childHeight = child.getHeight() + list.getDividerHeight();
                            startTop = top + (i > 0 ? childHeight : -childHeight);
                        }

                        Log.d("Start Top: " + startTop + ", Top: " + top);
                        // If the starting position of the child view is different from the
                        // current position, animate the child.
                        if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(ANIMATION_DURATION).translationY(0);
                            final View addedView = mAddedView;
                            if (firstAnimation) {

                                // If this is the first child being animated, then after the
                                // animation is complete, and animate in the added alarm (if one
                                // exists).
                                child.animate().withEndAction(new Runnable() {

                                    @Override
                                    public void run() {
                                        // If there was an added view, animate it in after
                                        // the other views have animated.
                                        if (addedView != null) {
                                            addedView.animate().alpha(1.0f)
                                                .setDuration(ANIMATION_DURATION)
                                                .withEndAction(new Runnable() {

                                                    @Override
                                                    public void run() {
                                                        // Re-enable the list after the add
                                                        // animation is complete.
                                                        list.setEnabled(true);
                                                    }

                                                });
                                        } else {
                                            // Re-enable the list after animations are complete.
                                            list.setEnabled(true);
                                        }
                                    }

                                });
                                firstAnimation = false;
                            }
                        }
                    }

                    // If there were no child views (outside of a possible added view)
                    // that require animation...
                    if (firstAnimation) {
                        if (mAddedView != null) {
                            // If there is an added view, prepare animation for the added view.
                            Log.d("Animating added view...");
                            mAddedView.animate().alpha(1.0f)
                                .setDuration(ANIMATION_DURATION)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Re-enable the list after animations are complete.
                                        list.setEnabled(true);
                                    }
                                });
                        } else {
                            // Re-enable the list after animations are complete.
                            list.setEnabled(true);
                        }
                    }

                    mAddedAlarm = null;
                    mItemIdTopMap.clear();
                    return true;
                }
            });
            return c;
        }

        private void setNewHolder(View view) {
            // standard view holder optimization
            final ItemHolder holder = new ItemHolder();
            holder.alarmItem = (FrameLayout) view.findViewById(R.id.alarm_item);
            holder.clock = (TextTime) view.findViewById(R.id.digital_clock);
            holder.clock.setTypeface(mDinproType);
            holder.onoff = (ToggleButton) view.findViewById(R.id.onoff);
            holder.onoff.setTypeface(mRobotoNormal);
            holder.onoffSwitch = (LinearLayout) view.findViewById(R.id.onoff_switch);

            holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
            holder.label = (TextView) view.findViewById(R.id.label);
            //holder.delete = (ImageView) view.findViewById(R.id.delete);
            holder.summary = view.findViewById(R.id.summary);
            //holder.expandArea = view.findViewById(R.id.expand_area);
            //holder.hairLine = view.findViewById(R.id.hairline);
            //holder.arrow = view.findViewById(R.id.arrow);
            //holder.repeat = (CheckBox) view.findViewById(R.id.repeat_onoff);
            //holder.clickableLabel = (TextView) view.findViewById(R.id.edit_label);
            holder.repeatDays = (FrameLayout) view.findViewById(R.id.repeat_days);
            //holder.collapseExpandArea = view.findViewById(R.id.collapse_expand);
            /*holder.footerFiller = view.findViewById(R.id.alarm_footer_filler);
            holder.footerFiller.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // Do nothing.
                }
            });*/

            //holder.vibrate = (CheckBox) view.findViewById(R.id.vibrate_onoff);
            //holder.ringtone = (TextView) view.findViewById(R.id.choose_ringtone);

            view.setTag(holder);
        }

        @Override
        public void bindView(final View view, Context context, final Cursor cursor) {
            final Alarm alarm = new Alarm(cursor);
			Log.i("ClockTrigger,get alarm(added or updated)");//liuqipeng add
            Object tag = view.getTag();
            if (tag == null) {
                // The view was converted but somehow lost its tag.
                setNewHolder(view);
                tag = view.getTag();
            }
            final ItemHolder itemHolder = (ItemHolder) tag;
            itemHolder.alarm = alarm;

            // We must unset the listener first because this maybe a recycled view so changing the
            // state would affect the wrong alarm.
            itemHolder.onoff.setOnCheckedChangeListener(null);
            itemHolder.onoff.setChecked(alarm.enabled);

            if (mSelectedAlarms.contains(itemHolder.alarm.id)) {
                // itemHolder.alarmItem.setBackgroundColor(mBackgroundColorExpanded);
                setItemAlpha(itemHolder, true);
                itemHolder.onoff.setEnabled(false);
            } else {
                itemHolder.onoff.setEnabled(true);
                // itemHolder.alarmItem.setBackgroundColor(mBackgroundColor);
                setItemAlpha(itemHolder, itemHolder.onoff.isChecked());
            }
            itemHolder.clock.setFormat(
                    (int)mContext.getResources().getDimension(R.dimen.alarm_label_size));
            itemHolder.clock.setTime(alarm.hour, alarm.minutes);
			Log.i("ClockTrigger,alarm.hour:"+alarm.hour+"alarm.minutes:"+alarm.minutes+"alarm.enabled:"+alarm.enabled);//liuqipeng add
            /*itemHolder.clock.setClickable(true);
            itemHolder.clock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSelectedAlarm = itemHolder.alarm;
                    showSetAlarmPage(itemHolder);
                    expandAlarm(itemHolder, true);
                    itemHolder.alarmItem.post(mScrollRunnable);
                }
            });*/

            final CompoundButton.OnCheckedChangeListener onOffListener =
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton,
                                boolean checked) {
                            if (checked != alarm.enabled) {
                                setItemAlpha(itemHolder, checked);
                                alarm.enabled = checked;
                                asyncUpdateAlarm(alarm, alarm.enabled);
                			}
                        }
                    };

            itemHolder.onoff.setOnCheckedChangeListener(onOffListener);
            itemHolder.onoffSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean bChecked = itemHolder.onoff.isChecked();
                    itemHolder.onoff.setChecked(!bChecked);
                    /*bChecked = itemHolder.onoff.isChecked();
                    if (bChecked != alarm.enabled) {
                        setItemAlpha(itemHolder, bChecked);
                        alarm.enabled = bChecked;
                        asyncUpdateAlarm(alarm, alarm.enabled);
                    }*/
                }
            });

            //boolean expanded = isAlarmExpanded(alarm);
            //itemHolder.expandArea.setVisibility(expanded? View.VISIBLE : View.GONE);
            //itemHolder.summary.setVisibility(expanded? View.GONE : View.VISIBLE);

            String labelSpace = "";
            if (alarm.label != null && alarm.label.length() != 0) {
//liuqipeng begin 注释一行　新增多行
                //itemHolder.label.setText(alarm.label);
				if(alarm.enabled){
				itemHolder.label.setText("开启");					
				}
				else{
				itemHolder.label.setText("未开启");
				}
//liuqipeng
                itemHolder.label.setVisibility(View.VISIBLE);
                itemHolder.label.setContentDescription(
                        mContext.getResources().getString(R.string.label_description) + " "
                        + alarm.label);
//liuqipeng begin
                //labelSpace = "  ";
				labelSpace = " /";
//liuqipeng
                /*itemHolder.label.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        expandAlarm(itemHolder, true);
                        itemHolder.alarmItem.post(mScrollRunnable);
                    }
                });*/
            } else {
                itemHolder.label.setVisibility(View.GONE);
            }

            // Set the repeat text or leave it blank if it does not repeat.
            final String daysOfWeekStr =
                    alarm.daysOfWeek.toString(AlarmClockFragment.this.getActivity(), false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                itemHolder.daysOfWeek.setText(daysOfWeekStr + labelSpace);
                itemHolder.daysOfWeek.setContentDescription(alarm.daysOfWeek.toAccessibilityString(
                        AlarmClockFragment.this.getActivity()));
                itemHolder.daysOfWeek.setVisibility(View.VISIBLE);
                /*itemHolder.daysOfWeek.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        expandAlarm(itemHolder, true);
                        itemHolder.alarmItem.post(mScrollRunnable);
                    }
                });*/

            } else {
                itemHolder.daysOfWeek.setVisibility(View.GONE);
            }

            //if (expanded) {
              //  expandAlarm(itemHolder, false);
            //} else {
                collapseAlarm(itemHolder, false);
            //}

            itemHolder.alarmItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSelectedAlarm = itemHolder.alarm;
                    showSetAlarmPage(itemHolder);
//                    if (isAlarmExpanded(alarm)) {
//                        collapseAlarm(itemHolder, true);
//                    } else {
//                        expandAlarm(itemHolder, true);
//                    }
                }
            });
            itemHolder.alarmItem.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment",
                            2101, "button_deleteAlarm", null, null, null);
                    ActionSheet actionSheet = new ActionSheet(mContext);
                    ArrayList<String> items = new ArrayList<String>();
                    items.add(mContext.getResources().getString(R.string.diag_edit));
                    items.add(mContext.getResources().getString(R.string.diag_delete));
                    actionSheet.setCommonButtons(items);
                    actionSheet.setCommonButtonListener(new ActionSheet.CommonButtonListener() {
                        @Override
                        public void onClick(int which) {
                            switch (which) {
                            case 0:
                                mSelectedAlarm = itemHolder.alarm;
                                showSetAlarmPage(itemHolder);
                                break;
                            case 1:
                                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                builder.setMessage(mContext.getResources().getString(R.string.alarm_delete_msg));
                                builder.setPositiveButton(mContext.getResources().getString(R.string.diag_delete),
                                        new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final AsyncTask<Void, Void, AlarmInstance> deleteTask = new AsyncTask<Void, Void, AlarmInstance>() {
                                            @Override
                                            protected AlarmInstance doInBackground(Void... parameters) {
                                                        mSelectedAlarm = itemHolder.alarm;
                                                        AlarmStateManager.deleteAllInstances(mContext, mSelectedAlarm.id);
                                                        Alarm.deleteAlarm(mContext.getContentResolver(), mSelectedAlarm.id);
                                                return null;
                                            }
                                        };
                                        deleteTask.execute();
                                    }
                                });
                                builder.setNegativeButton(mContext.getResources().getString(R.string.add_alarm_page_cacnel), null);
                                AlertDialog dialog = builder.create();
                                dialog.show();
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                        mContext.getResources().getColor(R.color.text_color_warning_selector));
                                break;
                            default:
                            }
                        }
                        @Override
                        public void onDismiss(ActionSheet arg0) {
                        }
                    });
                    actionSheet.show(itemHolder.alarmItem);
                    return true;
                }
            });
        }

        /*private void bindExpandArea(final ItemHolder itemHolder, final Alarm alarm) {
            // Views in here are not bound until the item is expanded.

            if (alarm.label != null && alarm.label.length() > 0) {
                itemHolder.clickableLabel.setText(alarm.label);
                itemHolder.clickableLabel.setTextColor(mColorLit);
            } else {
                itemHolder.clickableLabel.setText(R.string.label);
                itemHolder.clickableLabel.setTextColor(mColorDim);
            }
            itemHolder.clickableLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLabelDialog(alarm);
                }
            });

            if (mRepeatChecked.contains(alarm.id) || itemHolder.alarm.daysOfWeek.isRepeating()) {
                itemHolder.repeat.setChecked(true);
                itemHolder.repeatDays.setVisibility(View.VISIBLE);
            } else {
                itemHolder.repeat.setChecked(false);
                itemHolder.repeatDays.setVisibility(View.GONE);
            }

            if (!mHasVibrator) {
                itemHolder.vibrate.setVisibility(View.INVISIBLE);
            } else {
                itemHolder.vibrate.setVisibility(View.VISIBLE);
                if (!alarm.vibrate) {
                    itemHolder.vibrate.setChecked(false);
                    itemHolder.vibrate.setTextColor(mColorDim);
                } else {
                    itemHolder.vibrate.setChecked(true);
                    itemHolder.vibrate.setTextColor(mColorLit);
                }
            }

            itemHolder.vibrate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final boolean checked = ((CheckBox) v).isChecked();
                    if (checked) {
                        itemHolder.vibrate.setTextColor(mColorLit);
                    } else {
                        itemHolder.vibrate.setTextColor(mColorDim);
                    }
                    alarm.vibrate = checked;
                    asyncUpdateAlarm(alarm, false);
                }
            });

            final String ringtoneTitle;
            if (Alarm.NO_RINGTONE_URI.equals(alarm.alert)) {
                ringtoneTitle = mContext.getResources().getString(R.string.silent_alarm_summary);
                Log.v("NoRingtone uri, silent");
            } else {
                if (!isRingtoneExisted(getActivity(), alarm.alert.toString())) {
                    alarm.alert = RingtoneManager.getActualDefaultRingtoneUri(getActivity(),
                            RingtoneManager.TYPE_ALARM);
                    Log.v("ringtone not exist, use default ringtone");
                }
                ringtoneTitle = getRingToneTitle(alarm.alert);
            }
            Log.v("AlarmClockFragment ringtone title = " + ringtoneTitle);
            itemHolder.ringtone.setText(ringtoneTitle);
            itemHolder.ringtone.setContentDescription(
                    mContext.getResources().getString(R.string.ringtone_description) + " "
                            + ringtoneTitle);
            itemHolder.ringtone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchRingTonePicker(alarm);
                }
            });
        }*/

        // Sets the alpha of the item except the on/off switch. This gives a visual effect
        // for enabled/disabled alarm while leaving the on/off switch more visible
        private void setItemAlpha(ItemHolder holder, boolean enabled) {
            float alpha = enabled ? 1f : 0.5f;
            holder.clock.setAlpha(alpha);
            holder.summary.setAlpha(alpha);
            //holder.expandArea.setAlpha(alpha);
            //holder.delete.setAlpha(alpha);
            holder.daysOfWeek.setAlpha(alpha);
        }

        public void toggleSelectState(View v) {
            // long press could be on the parent view or one of its childs, so find the parent view
            v = getTopParent(v);
            if (v != null) {
                long id = ((ItemHolder)v.getTag()).alarm.id;
                if (mSelectedAlarms.contains(id)) {
                    mSelectedAlarms.remove(id);
                } else {
                    mSelectedAlarms.add(id);
                }
            }
        }

        private View getTopParent(View v) {
            while (v != null && v.getId() != R.id.alarm_item) {
                v = (View) v.getParent();
            }
            return v;
        }

        public int getSelectedItemsNum() {
            return mSelectedAlarms.size();
        }

        /**
         * Does a read-through cache for ringtone titles.
         *
         * @param uri The uri of the ringtone.
         * @return The ringtone title. {@literal null} if no matching ringtone found.
         */
        private String getRingToneTitle(Uri uri) {
            // Try the cache first
            String title = mRingtoneTitleCache.getString(uri.toString());
            if (title == null) {
                // This is slow because a media player is created during Ringtone object creation.
                Ringtone ringTone = RingtoneManager.getRingtone(mContext, uri);
                title = ringTone.getTitle(mContext);
                if (title != null) {
                    mRingtoneTitleCache.putString(uri.toString(), title);
                }
            }
            return title;
        }

        public void setNewAlarm(long alarmId) {
            //mExpanded.add(alarmId);
        }

        /**
         * Expands the alarm for editing.
         *
         * @param itemHolder The item holder instance.
         */
        public void showSetAlarmPage(final ItemHolder itemHolder) {
            Intent updateIntent= new Intent(mContext, SetAlarmActivity.class);
            Bundle mBundle = new Bundle();
            mBundle.putParcelable("alarm_data", itemHolder.alarm);
            mBundle.putParcelable("ringtone_cache", mRingtoneTitleCache);
            updateIntent.putExtras(mBundle);
            mFromCancel = false;
            if (mInstance == null) {
                mInstance = getInstance();
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isShowSettingAlarm", true);
            editor.apply();
            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                ((Activity) mContext).startActivityForResult(updateIntent, REQUEST_UPDATE_ALARM);
            } else {
                ((Activity) mContext).startActivityForResult(updateIntent, REQUEST_UPDATE_ALARM);
            }
/*            mSetAlarmPage.setVisibility(View.VISIBLE);
            mSetAlarmPage.setFooterButtonEnable();
            mSetAlarmPage.resetView();
            mSetAlarmPage.setView(itemHolder.alarm);*/
        }

        private void expandAlarm(final ItemHolder itemHolder, boolean animate) {
//            mExpanded.add(itemHolder.alarm.id);
//            bindExpandArea(itemHolder, itemHolder.alarm);
//            // Scroll the view to make sure it is fully viewed
//            mScrollAlarmId = itemHolder.alarm.id;
//
//            // Save the starting height so we can animate from this value.
//            final int startingHeight = itemHolder.alarmItem.getHeight();
//
//            // Set the expand area to visible so we can measure the height to animate to.
//            itemHolder.alarmItem.setBackgroundColor(mBackgroundColorExpanded);
//            itemHolder.expandArea.setVisibility(View.VISIBLE);
//
//            if (!animate) {
//                // Set the "end" layout and don't do the animation.
//                itemHolder.arrow.setRotation(180);
//                // We need to translate the hairline up, so the height of the collapseArea
//                // needs to be measured to know how high to translate it.
//                final ViewTreeObserver observer = mAlarmsList.getViewTreeObserver();
//                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                    @Override
//                    public boolean onPreDraw() {
//                        // We don't want to continue getting called for every listview drawing.
//                        if (observer.isAlive()) {
//                            observer.removeOnPreDrawListener(this);
//                        }
//                        int hairlineHeight = itemHolder.hairLine.getHeight();
//                        int collapseHeight =
//                                itemHolder.collapseExpandArea.getHeight() - hairlineHeight;
//                        itemHolder.hairLine.setTranslationY(-collapseHeight);
//                        return true;
//                    }
//                });
//                return;
//            }
//
//            // Add an onPreDrawListener, which gets called after measurement but before the draw.
//            // This way we can check the height we need to animate to before any drawing.
//            // Note the series of events:
//            //  * expandArea is set to VISIBLE, which causes a layout pass
//            //  * the view is measured, and our onPreDrawListener is called
//            //  * we set up the animation using the start and end values.
//            //  * the height is set back to the starting point so it can be animated down.
//            //  * request another layout pass.
//            //  * return false so that onDraw() is not called for the single frame before
//            //    the animations have started.
//            final ViewTreeObserver observer = mAlarmsList.getViewTreeObserver();
//            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                @Override
//                public boolean onPreDraw() {
//                    // We don't want to continue getting called for every listview drawing.
//                    if (observer.isAlive()) {
//                        observer.removeOnPreDrawListener(this);
//                    }
//                    // Calculate some values to help with the animation.
//                    final int endingHeight = itemHolder.alarmItem.getHeight();
//                    final int distance = endingHeight - startingHeight;
//                    final int collapseHeight = itemHolder.collapseExpandArea.getHeight();
//                    int hairlineHeight = itemHolder.hairLine.getHeight();
//                    final int hairlineDistance = collapseHeight - hairlineHeight;
//
//                    // Set the height back to the start state of the animation.
//                    itemHolder.alarmItem.getLayoutParams().height = startingHeight;
//                    // To allow the expandArea to glide in with the expansion animation, set a
//                    // negative top margin, which will animate down to a margin of 0 as the height
//                    // is increased.
//                    // Note that we need to maintain the bottom margin as a fixed value (instead of
//                    // just using a listview, to allow for a flatter hierarchy) to fit the bottom
//                    // bar underneath.
//                    FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
//                            itemHolder.expandArea.getLayoutParams();
//                    expandParams.setMargins(0, -distance, 0, collapseHeight);
//                    itemHolder.alarmItem.requestLayout();
//
//                    // Set up the animator to animate the expansion.
//                    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
//                            .setDuration(EXPAND_DURATION);
//                    animator.setInterpolator(mExpandInterpolator);
//                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                        @Override
//                        public void onAnimationUpdate(ValueAnimator animator) {
//                            Float value = (Float) animator.getAnimatedValue();
//
//                            // For each value from 0 to 1, animate the various parts of the layout.
//                            itemHolder.alarmItem.getLayoutParams().height =
//                                    (int) (value * distance + startingHeight);
//                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
//                                    itemHolder.expandArea.getLayoutParams();
//                            expandParams.setMargins(
//                                    0, (int) -((1 - value) * distance), 0, collapseHeight);
//                            itemHolder.arrow.setRotation(180 * value);
//                            itemHolder.hairLine.setTranslationY(-hairlineDistance * value);
//                            itemHolder.summary.setAlpha(1 - value);
//
//                            itemHolder.alarmItem.requestLayout();
//                        }
//                    });
//                    // Set everything to their final values when the animation's done.
//                    animator.addListener(new AnimatorListener() {
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            // Set it back to wrap content since we'd explicitly set the height.
//                            itemHolder.alarmItem.getLayoutParams().height =
//                                    LayoutParams.WRAP_CONTENT;
//                            itemHolder.arrow.setRotation(180);
//                            itemHolder.hairLine.setTranslationY(-hairlineDistance);
//                            itemHolder.summary.setVisibility(View.GONE);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//                            // TODO we may have to deal with cancelations of the animation.
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) { }
//                        @Override
//                        public void onAnimationStart(Animator animation) { }
//                    });
//                    animator.start();
//
//                    // Return false so this draw does not occur to prevent the final frame from
//                    // being drawn for the single frame before the animations start.
//                    return false;
//                }
//            });
        }

        //private boolean isAlarmExpanded(Alarm alarm) {
            //return mExpanded.contains(alarm.id);
        //}

        private void collapseAlarm(final ItemHolder itemHolder, boolean animate) {
//            mExpanded.remove(itemHolder.alarm.id);
//
//            // Save the starting height so we can animate from this value.
//            final int startingHeight = itemHolder.alarmItem.getHeight();
//
//            // Set the expand area to gone so we can measure the height to animate to.
//            itemHolder.alarmItem.setBackgroundResource(mBackgroundColor);
//            itemHolder.expandArea.setVisibility(View.GONE);
//
//            if (!animate) {
//                // Set the "end" layout and don't do the animation.
//                itemHolder.arrow.setRotation(0);
//                itemHolder.hairLine.setTranslationY(0);
//                return;
//            }
//
//            // Add an onPreDrawListener, which gets called after measurement but before the draw.
//            // This way we can check the height we need to animate to before any drawing.
//            // Note the series of events:
//            //  * expandArea is set to GONE, which causes a layout pass
//            //  * the view is measured, and our onPreDrawListener is called
//            //  * we set up the animation using the start and end values.
//            //  * expandArea is set to VISIBLE again so it can be shown animating.
//            //  * request another layout pass.
//            //  * return false so that onDraw() is not called for the single frame before
//            //    the animations have started.
//            final ViewTreeObserver observer = mAlarmsList.getViewTreeObserver();
//            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                @Override
//                public boolean onPreDraw() {
//                    if (observer.isAlive()) {
//                        observer.removeOnPreDrawListener(this);
//                    }
//
//                    // Calculate some values to help with the animation.
//                    final int endingHeight = itemHolder.alarmItem.getHeight();
//                    final int distance = endingHeight - startingHeight;
//                    int hairlineHeight = itemHolder.hairLine.getHeight();
//                    final int hairlineDistance = mCollapseExpandHeight - hairlineHeight;
//
//                    // Re-set the visibilities for the start state of the animation.
//                    itemHolder.expandArea.setVisibility(View.VISIBLE);
//                    itemHolder.summary.setVisibility(View.VISIBLE);
//                    itemHolder.summary.setAlpha(1);
//
//                    // Set up the animator to animate the expansion.
//                    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
//                            .setDuration(COLLAPSE_DURATION);
//                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                        @Override
//                        public void onAnimationUpdate(ValueAnimator animator) {
//                            Float value = (Float) animator.getAnimatedValue();
//
//                            // For each value from 0 to 1, animate the various parts of the layout.
//                            itemHolder.alarmItem.getLayoutParams().height =
//                                    (int) (value * distance + startingHeight);
//                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
//                                    itemHolder.expandArea.getLayoutParams();
//                            expandParams.setMargins(
//                                    0, (int) (value * distance), 0, mCollapseExpandHeight);
//                            itemHolder.arrow.setRotation(180 * (1 - value));
//                            itemHolder.hairLine.setTranslationY(-hairlineDistance * (1 - value));
//                            itemHolder.summary.setAlpha(value);
//
//                            itemHolder.alarmItem.requestLayout();
//                        }
//                    });
//                    animator.setInterpolator(mCollapseInterpolator);
//                    // Set everything to their final values when the animation's done.
//                    animator.addListener(new AnimatorListener() {
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            // Set it back to wrap content since we'd explicitly set the height.
//                            itemHolder.alarmItem.getLayoutParams().height =
//                                    LayoutParams.WRAP_CONTENT;
//
//                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
//                                    itemHolder.expandArea.getLayoutParams();
//                            expandParams.setMargins(0, 0, 0, mCollapseExpandHeight);
//
//                            itemHolder.expandArea.setVisibility(View.GONE);
//                            itemHolder.arrow.setRotation(0);
//                            itemHolder.hairLine.setTranslationY(0);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//                            // TODO we may have to deal with cancelations of the animation.
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) { }
//                        @Override
//                        public void onAnimationStart(Animator animation) { }
//                    });
//                    animator.start();
//
//                    return false;
//                }
//            });
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        private View getViewById(long id) {
            for (int i = 0; i < mList.getCount(); i++) {
                View v = mList.getChildAt(i);
                if (v != null) {
                    ItemHolder h = (ItemHolder)(v.getTag());
                    if (h != null && h.alarm.id == id) {
                        return v;
                    }
                }
            }
            return null;
        }
/*
        public long[] getExpandedArray() {
            int index = 0;
            long[] ids = new long[mExpanded.size()];
            for (long id : mExpanded) {
                ids[index] = id;
                index++;
            }
            return ids;
        }
*/
        public long[] getSelectedAlarmsArray() {
            int index = 0;
            long[] ids = new long[mSelectedAlarms.size()];
            for (long id : mSelectedAlarms) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public long[] getRepeatArray() {
            int index = 0;
            long[] ids = new long[mRepeatChecked.size()];
            for (long id : mRepeatChecked) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public Bundle getPreviousDaysOfWeekMap() {
            return mPreviousDaysOfWeekMap;
        }

        private void buildHashSetFromArray(long[] ids, HashSet<Long> set) {
            for (long id : ids) {
                set.add(id);
            }
        }
    }

    private void startCreatingAlarm() {
        Intent updateIntent= new Intent(mContext, SetAlarmActivity.class);
        Bundle mBundle = new Bundle();
        mBundle.putParcelable("ringtone_cache", mRingtoneTitleCache);
        updateIntent.putExtras(mBundle);
        updateIntent.putExtra("from_cancel", mFromCancel);
        mFromCancel = false;
        if (mInstance == null) {
            mInstance = getInstance();
        }
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            ((Activity) mContext).startActivityForResult(updateIntent, REQUEST_ADD_ALARM);
        } else {
            ((Activity) mContext).startActivityForResult(updateIntent, REQUEST_ADD_ALARM);
        }
        //((Activity)mContext).overridePendingTransition(0,0);
        /*        mSelectedAlarm = null;
        mSetAlarmPage.setVisibility(View.VISIBLE);
        mSetAlarmPage.setFooterButtonEnable();
        mSetAlarmPage.resetView();*/
    }

    private void settingsAlarm() {
        startActivity(new Intent(this.getActivity().getApplicationContext(), SettingsActivity.class));
    }

    public void hideSetAlarmPage(){
        //mSetAlarmPage.setVisibility(View.GONE);
    }

    private static AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver cr = context.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance(),context);
        newInstance = AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
		Log.i("ClockTrigger,Register instance to state manager");//liuqipeng add
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    private void asyncDeleteAlarm(final Alarm alarm, final View viewToRemove) {
        final Context context = mContext;

        final AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
            @Override
            public synchronized void onPreExecute() {
                if (viewToRemove == null) {
                    return;
                }
                // The alarm list needs to be disabled until the animation finishes to prevent
                // possible concurrency issues.  It becomes re-enabled after the animations have
                // completed.
                //mAlarmsList.setEnabled(false);

                // Store all of the current list view item positions in memory for animation.
                final ListView list = mAlarmsList;
                if (list != null) {
                    int firstVisiblePosition = list.getFirstVisiblePosition();
                    for (int i=0; i<list.getChildCount(); i++) {
                        View child = list.getChildAt(i);
                        if (child != viewToRemove) {
                            int position = firstVisiblePosition + i;
                            long itemId = mAdapter.getItemId(position);
                            mItemIdTopMap.put(itemId, child.getTop());
                        }
                    }
                }
            }

            @Override
            protected Void doInBackground(Void... parameters) {
                // Activity may be closed at this point , make sure data is still valid
                if (context != null && alarm != null) {
                    ContentResolver cr = context.getContentResolver();
                    AlarmStateManager.deleteAllInstances(context, alarm.id);
                    Alarm.deleteAlarm(cr, alarm.id);
                }
                return null;
            }
        };
        mUndoShowing = true;
        deleteTask.execute();
    }

    public void asyncAddAlarm(final Alarm alarm) {
        final Context context = mContext;

        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            public synchronized void onPreExecute() {
                final ListView list = mAlarmsList;
                if(mAdapter.getCount() > 1){
                    mRefreshableView.showHeader(false);
                    mRefreshableView.mCommonList.setScrollable(true);
                }
                if(mAdapter.getCount() >= 0){
                    Utils.notify2EnableCloudService(mContext);
                }
                // The alarm list needs to be disabled until the animation finishes to prevent
                // possible concurrency issues.  It becomes re-enabled after the animations have
                // completed.
                //mAlarmsList.setEnabled(false);

                // Store all of the current list view item positions in memory for animation.
                if (list != null) {
                    int firstVisiblePosition = list.getFirstVisiblePosition();
                    for (int i=0; i<list.getChildCount(); i++) {
                        View child = list.getChildAt(i);
                        int position = firstVisiblePosition + i;
                        long itemId = mAdapter.getItemId(position);
                        mItemIdTopMap.put(itemId, child.getTop());
                    }
                } else {
                    Log.i("Something is wrong, cause the list is null?");
                }
            }

            @Override
            protected AlarmInstance doInBackground(Void... parameters) {
                if (context != null && alarm != null) {
                    ContentResolver cr = context.getContentResolver();

                    // Add alarm to db
                    Alarm newAlarm = Alarm.addAlarm(cr, alarm);
					Log.i("ClockTrigger,asyncAddAlarm,doInBackground");//liuqipeng add
                    mScrollToAlarmId = newAlarm.id;
                    // Create and add instance to db
                    if (newAlarm.enabled) {
					Log.i("ClockTrigger,newAlarm.enabled"+newAlarm.enabled+"Create and add instance to db");//liuqipeng add
                        return setupAlarmInstance(context, newAlarm);
                    }
                } else {
                    Log.i("Something is wrong, cause the context is null?");
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (instance != null) {
                    Log.d("Alarm is successfully added. add this to check if the notification has problem.");
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }
        };
        updateTask.execute();
    }

    public void asyncUpdateAlarm(final Alarm alarm, final boolean popToast) {
        final Context context = mContext;

        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            protected AlarmInstance doInBackground(Void ... parameters) {
                if (context != null && alarm != null) {
                    ContentResolver cr = context.getContentResolver();

                    // Dismiss all old instances
                    AlarmStateManager.deleteAllInstances(context, alarm.id);

                    // Update alarm
                    Alarm.updateAlarm(cr, alarm);
                    if (alarm.enabled) {
                        return setupAlarmInstance(context, alarm);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (popToast && instance != null) {
                    Log.d("Alarm is successfully updated. add this to check if the notification has problem.");
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }
        };
        updateTask.execute();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //hideUndoBar(true, event);
        return false;
    }

    /**
     * M: Set the system default Alarm Ringtone,
     * then save it as the Clock internal used ringtone.
     */
    public void setSystemAlarmRingtoneToPref() {
        Uri systemDefaultRingtone = RingtoneManager.getActualDefaultRingtoneUri(mContext,
                RingtoneManager.TYPE_ALARM);
        if (systemDefaultRingtone != null)
        setDefaultRingtone(systemDefaultRingtone.toString());
        Log.v("setSystemAlarmRingtone: " + systemDefaultRingtone);
    }

    /**
     * M: Set the internal used default Ringtones
     */
    public void setDefaultRingtone(String defaultRingtone) {
        if (TextUtils.isEmpty(defaultRingtone)) {
            Log.e("setDefaultRingtone fail");
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_DEFAULT_RINGTONE, defaultRingtone);
        editor.apply();
        Log.v("Set default ringtone to preference" + defaultRingtone);
    }

    /**
     * M: Get the internal used default Ringtones
     */
    public static String getDefaultRingtone(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultRingtone = prefs.getString(KEY_DEFAULT_RINGTONE, "");
        Log.v("Get default ringtone from preference " + defaultRingtone);
        return defaultRingtone;
    }

    /**
     *M: to check if the ringtone media file is removed from SD-card or not.
     * @param ringtone
     * @return
     */
    public static boolean isRingtoneExisted(Context ctx, String ringtone) {
        boolean result = false;
        if (ringtone != null) {
            if (ringtone.contains("internal")) {
                return true;
            }
            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                String path = PowerOffAlarm.getRingtonePath(ctx, ringtone);
                Log.v("isRingtoneExisted ringtone path = " + path);
                if (!TextUtils.isEmpty(path)) {
                    result = new File(path).exists();
                }
            }
        }
        Log.v("isRingtoneExisted = " + result + ", ringtone:" + ringtone);
        return result;
    }

    public void onDetach() {
        if(AlarmActivity.mRecentAPP){
            Log.i("onDetach kill");
            AlarmInstance ai = AlarmActivity.getAlarmInstance();
            AlarmStateManager.setDismissState(mContext, ai);
            AlarmActivity.mRecentAPP = false;
        }
        super.onDetach();
        mContext = null;
        mInstance = null;
        Log.i("alarm clock detached from Deskclock.");
        Log.d("AlarmClockFragment detach the last pointer is : " + this);
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void onAttach(Activity m_Activity) {
        super.onAttach(m_Activity);
        mContext = m_Activity;
        mInstance = this;
        Log.i("alarm clock attached to Deskclock.");
        Log.d("AlarmClockFragment attach the last pointer is : " + this);
    }

    public void refreashView() {
        if (mRefreshableView != null) {
            mRefreshableView.refreashView();
        }
    }

    public boolean getRefreshableView() {
        if (mRefreshableView != null) {
            return mRefreshableView.mDisPlayTwoItem;
        }
        return false;
    }

    public void onAnimationUpdate(float positionOffset) {
        ArgbEvaluator evaluator = new ArgbEvaluator();
//liuqipeng begin
        //int evaluate = (Integer) evaluator.evaluate(positionOffset, 0XFF357ECE, 0XFF38BA78);
		int evaluate = (Integer) evaluator.evaluate(positionOffset, 0XFF01ab4a, 0XFF009a96);
//liuqipeng
        if (mAlarmMainView != null) {
            mAlarmMainView.setBackgroundColor(evaluate);
        }
//liuqipeng　注释掉
/*
        if (mAlarmListView != null) {
            mAlarmListView.setBackgroundColor(evaluate);
        }
*/
//liuqipeng
    }

    public void onListViewRefreshListener() {
        mAlarmsList.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    public void BuryInformation(){
        new Thread(new Runnable() {
              int ENABLED_INDEX = 4;
              int alarmcount = 0;
              int alarmvalidcount = 0;

              @Override
              public void run() {
                  try {
                     if (mAdapter == null) {
                      return;
                   }
                   Cursor cursor = mAdapter.getCursor();
                   if (cursor == null) {
                       return;
                   }
                   if (cursor.moveToFirst()) {
                   do {
                       alarmcount = alarmcount + 1;
                       if (cursor.getInt(ENABLED_INDEX) == 1)
                           alarmvalidcount = alarmvalidcount + 1;
                   } while (cursor.moveToNext());
                   }
                       Log.d("alarmcount =" + alarmcount + "alarmvalidcount =" + alarmvalidcount);
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
                   HashMap<String, String> lMaps = new HashMap<String, String>();
                   lMaps.put("alarm_count", String.valueOf(alarmcount));
                   lMaps.put("alarm_validcount", String.valueOf(alarmvalidcount));
                   TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment_Alarm_List_Details", lMaps);
                   }
              }).start();
    }

}
