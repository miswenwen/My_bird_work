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

package com.android.deskclock.timer;
//liuqipeng begin
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
//liuqipeng
import android.R.color;
import android.app.Activity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.SoundPool;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.aliyun.ams.ta.TA;
import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.DeskClock;
import com.android.deskclock.ClockFragment;
//import com.android.deskclock.DeskClock.OnTapListener;
import yunos.support.v4.app.Fragment;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.R;
import com.android.deskclock.SetGroupView;
import com.android.deskclock.TimerSetupView;
import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import com.android.deskclock.widget.sgv.SgvAnimationHelper.AnimationIn;
import com.android.deskclock.widget.sgv.SgvAnimationHelper.AnimationOut;
import com.android.deskclock.widget.sgv.StaggeredGridView;
import com.android.deskclock.widget.sgv.GridAdapter;
import com.android.deskclock.timer.TimerGroupView;

public class TimerFragment extends Fragment
        implements OnClickListener, OnSharedPreferenceChangeListener, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "TimerFragment";
    private static final String KEY_SETUP_SELECTED = "_setup_selected";
    private static final String KEY_ENTRY_STATE = "entry_state";
    public static final String GOTO_SETUP_VIEW = "deskclock.timers.gotosetup";

    public boolean isFullScreen = false;
    //public ImageButton mAddSecondBtn;
    //public ImageButton mAddSecondBtn2;
    public View mStartBtn;
    public View mStopBtn;
    //public ImageButton mStopBtn2;
    public View mCancelBtn;
    //public TextView mAddSecondTitle;
    //public TextView mAddSecondTitle2;
//liuqipeng begin
    //public ImageView mStartTitle;
	public TextView mStartTitle;
//liuqipeng
//liuqipeng begin
    //public ImageView mStopTitle;
	public TextView mStopTitle;
//liuqipeng
    //public TextView mStopTitle2;
//liuqipeng begin
    //public ImageView mCancelTitle;
	public TextView mCancelTitle;
//liuqipeng
    public TimerObj mTimerObjTag = null;
    private Bundle mViewState = null;
    private StaggeredGridView mTimersList;
    public TimerGroupView mTimerGroupView;
    private View mTimersListPage;
    private int mColumnCount;
    private Button mCancel, mStart;
    private View mSeperator;
    private ImageButton mAddTimer;
    private View mTimerFooter;
    private TimerSetupView mTimerSetup;
    private TimersListAdapter mAdapter;
    private boolean mTicking = false;
    private SharedPreferences mPrefs;
    private NotificationManager mNotificationManager;
    private OnEmptyListListener mOnEmptyListListener;
    private View mLastVisibleView = null;  // used to decide if to set the view or animate to it.
    private static TimerFragment mInstance = null;
    private MediaPlayer mMediaPlayer;
    private SoundPool mSoundPoolRun;
    private SoundPool mSoundPoolStart;
    private int mStreamID;
    private int mSetQuickTimeStreamID;
    private Context mTimerContext;
    private boolean mPlaySound = false;
    private AudioManager mAudioManager;
    private boolean mFirstPlay = true;
    //private boolean mPauseStatus = false;
    private boolean mFirstIn = true;
//liuqipeng begin
	public ListView mListView;
	public MyAdapter myAdapter;
	public ArrayList<Boolean> mList = new ArrayList<Boolean>();
//liuqipeng

    public TimerFragment() {
        mInstance = this;
    }

    public class ClickAction {
        public static final int ACTION_STOP = 1;
        public static final int ACTION_PLUS_ONE = 2;
        public static final int ACTION_DELETE = 3;

        public int mAction;
        public TimerObj mTimer;

        public ClickAction(int action, TimerObj t) {
            mAction = action;
            mTimer = t;
        }
    }

    // Container Activity that requests TIMESUP_MODE must implement this interface
    public interface OnEmptyListListener {
        public void onEmptyList();
        public void onListChanged();
    }

    TimersListAdapter createAdapter(Context context, SharedPreferences prefs) {
        if (mOnEmptyListListener == null) {
            return new TimersListAdapter(context, prefs);
        } else {
            return new TimesUpListAdapter(context, prefs);
        }
    }

    class TimersListAdapter extends GridAdapter {

        ArrayList<TimerObj> mTimers = new ArrayList<TimerObj> ();
        Context mContext;
        SharedPreferences mmPrefs;

        public TimersListAdapter(Context context, SharedPreferences prefs) {
            mContext = context;
            mmPrefs = prefs;
        }

        @Override
        public int getCount() {
            return mTimers.size();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public TimerObj getItem(int p) {
            return mTimers.get(p);
        }

        @Override
        public long getItemId(int p) {
            if (p >= 0 && p < mTimers.size()) {
                return mTimers.get(p).mTimerId;
            }
            return 0;
        }

        public void deleteTimer(int id) {
            for (int i = 0; i < mTimers.size(); i++) {
                TimerObj t = mTimers.get(i);

                if (t.mTimerId == id) {
                    if (t.mView != null) {
                        ((TimerListItem) t.mView).stop();
                    }
                    t.deleteFromSharedPref(mmPrefs);
                    mTimers.remove(i);
                    if (mTimers.size() == 1 && mColumnCount > 1) {
                        // If we're going from two timers to one (in the same row), we don't want to
                        // animate the translation because we're changing the layout params span
                        // from 1 to 2, and the animation doesn't handle that very well. So instead,
                        // just fade out and in.
                        mTimersList.setAnimationMode(AnimationIn.FADE, AnimationOut.FADE);
                    } else {
                        mTimersList.setAnimationMode(
                                AnimationIn.FLY_IN_NEW_VIEWS, AnimationOut.FADE);
                    }
                    notifyDataSetChanged();
                    return;
                }
            }
        }

        protected int findTimerPositionById(int id) {
            for (int i = 0; i < mTimers.size(); i++) {
                TimerObj t = mTimers.get(i);
                if (t.mTimerId == id) {
                    return i;
                }
            }
            return -1;
        }

        public void removeTimer(TimerObj timerObj) {
            int position = findTimerPositionById(timerObj.mTimerId);
            if (position >= 0) {
                mTimers.remove(position);
                notifyDataSetChanged();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TimerListItem v = new TimerListItem (mContext); // TODO: Need to recycle convertView.

            final TimerObj o = (TimerObj)getItem(position);
            o.mView = v;
            long timeLeft =  o.updateTimeLeft(false);
            boolean drawRed = o.mState != TimerObj.STATE_RESTART;
            v.set(o.mOriginalLength, timeLeft, drawRed);
            v.setTime(timeLeft, true);
            switch (o.mState) {
            case TimerObj.STATE_RUNNING:
                v.start();
                break;
            case TimerObj.STATE_TIMESUP:
                v.timesUp();
                break;
            case TimerObj.STATE_DONE:
                v.done();
                break;
            default:
                break;
            }

            // Timer text serves as a virtual start/stop button.
            final CountingTimerView countingTimerView = (CountingTimerView)
                    v.findViewById(R.id.timer_time_text);
            countingTimerView.registerVirtualButtonAction(new Runnable() {
                @Override
                public void run() {
                    TimerFragment.this.onClickHelper(
                            new ClickAction(ClickAction.ACTION_STOP, o));
                }
            });

            ImageButton delete = (ImageButton)v.findViewById(R.id.timer_delete);
            delete.setOnClickListener(TimerFragment.this);
            delete.setTag(new ClickAction(ClickAction.ACTION_DELETE, o));
            ImageButton leftButton = (ImageButton)v. findViewById(R.id.timer_plus_one);
            leftButton.setOnClickListener(TimerFragment.this);
            leftButton.setTag(new ClickAction(ClickAction.ACTION_PLUS_ONE, o));
            TextView stop = (TextView)v. findViewById(R.id.timer_stop);
            stop.setTag(new ClickAction(ClickAction.ACTION_STOP, o));
            TimerFragment.this.setTimerButtons(o);

            v.setBackgroundColor(getResources().getColor(R.color.white));
            countingTimerView.registerStopTextView(stop);
            CircleButtonsLayout circleLayout =
                    (CircleButtonsLayout)v.findViewById(R.id.timer_circle);
            circleLayout.setCircleTimerViewIds(
                    R.id.timer_time, R.id.timer_plus_one, R.id.timer_delete, R.id.timer_stop,
                    R.dimen.plusone_reset_button_padding, R.dimen.delete_button_padding,
                    R.id.timer_label, R.id.timer_label_text);

            FrameLayout label = (FrameLayout)v. findViewById(R.id.timer_label);
            ImageButton labelIcon = (ImageButton)v. findViewById(R.id.timer_label_icon);
            TextView labelText = (TextView)v. findViewById(R.id.timer_label_text);
            if (o.mLabel.equals("")) {
                labelText.setVisibility(View.GONE);
                labelIcon.setVisibility(View.VISIBLE);
            } else {
                labelText.setText(o.mLabel);
                labelText.setVisibility(View.VISIBLE);
                labelIcon.setVisibility(View.GONE);
            }
            if (getActivity() instanceof DeskClock) {
//                label.setOnTouchListener(new OnTapListener(getActivity(), labelText) {
//                    @Override
//                    protected void processClick(View v) {
//                        onLabelPressed(o);
//                    }
//                });
            } else {
                labelIcon.setVisibility(View.INVISIBLE);
            }
            return v;
        }

        @Override
        public int getItemColumnSpan(Object item, int position) {
            // This returns the width for a specified position. If we only have one item, have it
            // span all columns so that it's centered. Otherwise, all timers should just span one.
            if (getCount() == 1) {
                return mColumnCount;
            } else {
                return 1;
            }
        }

        public void addTimer(TimerObj t) {
            mTimers.add(0, t);
            sort();
        }

        public void onSaveInstanceState(Bundle outState) {
            TimerObj.putTimersInSharedPrefs(mmPrefs, mTimers);
        }

        public void onRestoreInstanceState(Bundle outState) {
            Log.d(TAG, "onRestoreInstanceState");
            TimerObj.getTimersFromSharedPrefs(mmPrefs, mTimers);
            mTimerObjTag = TimerObj.getTagTimersFromSharedPrefs(mmPrefs, true);
            sort();
        }

        public void saveGlobalState() {
            TimerObj.putTimersInSharedPrefs(mmPrefs, mTimers);
        }

        public void sort() {
            if (getCount() > 0) {
                Collections.sort(mTimers, mTimersCompare);
                notifyDataSetChanged();
            }
        }

        private final Comparator<TimerObj> mTimersCompare = new Comparator<TimerObj>() {
            static final int BUZZING = 0;
            static final int IN_USE = 1;
            static final int NOT_USED = 2;

            protected int getSection(TimerObj timerObj) {
                switch (timerObj.mState) {
                    case TimerObj.STATE_TIMESUP:
                        return BUZZING;
                    case TimerObj.STATE_RUNNING:
                    case TimerObj.STATE_STOPPED:
                        return IN_USE;
                    default:
                        return NOT_USED;
                }
            }

            @Override
            public int compare(TimerObj o1, TimerObj o2) {
                int section1 = getSection(o1);
                int section2 = getSection(o2);
                if (section1 != section2) {
                    return (section1 < section2) ? -1 : 1;
                } else if (section1 == BUZZING || section1 == IN_USE) {
                    return (o1.mTimeLeft < o2.mTimeLeft) ? -1 : 1;
                } else {
                    return (o1.mSetupLength < o2.mSetupLength) ? -1 : 1;
                }
            }
        };
    }

    class TimesUpListAdapter extends TimersListAdapter {

        public TimesUpListAdapter(Context context, SharedPreferences prefs) {
            super(context, prefs);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // This adapter has a data subset and never updates entire database
            // Individual timers are updated in button handlers.
        }

        @Override
        public void saveGlobalState() {
            // This adapter has a data subset and never updates entire database
            // Individual timers are updated in button handlers.
        }

        @Override
        public void onRestoreInstanceState(Bundle outState) {
            // This adapter loads a subset
            TimerObj.getTimersFromSharedPrefs(mmPrefs, mTimers, TimerObj.STATE_TIMESUP);
            mTimerObjTag = TimerObj.getTagTimersFromSharedPrefs(mmPrefs, true);
            if (getCount() == 0) {
                mOnEmptyListListener.onEmptyList();
            } else {
                Collections.sort(mTimers, new Comparator<TimerObj>() {
                    @Override
                    public int compare(TimerObj o1, TimerObj o2) {
                        return (o1.mTimeLeft <  o2.mTimeLeft) ? -1 : 1;
                    }
                });
            }
        }
    }

    private long mLastTimeLeft = 0;

    private final Runnable mClockTick = new Runnable() {
        boolean mVisible = true;
        final static int TIME_PERIOD_MS = 1000;
        final static int SPLIT = TIME_PERIOD_MS / 2;

        @Override
        public void run() {
            // Setup for blinking
            boolean visible = Utils.getTimeNow() % TIME_PERIOD_MS < SPLIT;
            boolean toggle = mVisible != visible;
            mVisible = visible;
            if (mTimerObjTag != null) {
                if (mTimerObjTag.mState == TimerObj.STATE_RUNNING || mTimerObjTag.mState == TimerObj.STATE_TIMESUP) {
                    mPlaySound = mPrefs.getBoolean("mTimerFragmentPlayRunSound", true);
                    if(mFirstIn && mPlaySound){
                        mFirstPlay = true;
                        mFirstIn = false;
                    }
                    else if(!mPlaySound){
                       mFirstIn = true;
                    }
                    long timeLeft = mTimerObjTag.updateTimeLeft(false);
                    if (mLastTimeLeft != 0 && mLastTimeLeft/1000 != timeLeft/1000) {
                        if (mSoundPoolRun!= null && mPlaySound) {
                            mSoundPoolRun.play(mStreamID, 1, 1, 0, 0, 1);
                            if (mFirstPlay) {
                                mFirstPlay = false;
                                startTimerSound(mSoundPoolRun);
                            }
                        } else {
                            if (mAudioManager != null) {
                                mAudioManager.abandonAudioFocus(TimerFragment.this);
                            }
                        }
                    }
                    mLastTimeLeft = timeLeft;

                    mTimerGroupView.setTime(timeLeft);
                }
                if (mTimerObjTag.updateTimeLeft(false) <= 0 && mTimerObjTag.mState != TimerObj.STATE_DONE
                        && mTimerObjTag.mState != TimerObj.STATE_RESTART && mTimerObjTag.mState != TimerObj.STATE_DELETED) {
                        mTimerObjTag.mState = TimerObj.STATE_TIMESUP;
                }
            }
            for (int i = 0; i < mAdapter.getCount(); i ++) {
                TimerObj t = mAdapter.getItem(i);
                if (t.mState == TimerObj.STATE_RUNNING || t.mState == TimerObj.STATE_TIMESUP) {
                    long timeLeft = t.updateTimeLeft(false);
                    if (t.mView != null) {
                        ((TimerListItem)(t.mView)).setTime(timeLeft, false);
                        // Update button every 1/2 second
                        if (toggle) {
                            ImageButton leftButton = (ImageButton)
                                  t.mView.findViewById(R.id.timer_plus_one);
                            leftButton.setEnabled(canAddMinute(t));
                        }
                    }
                }
                if (t.mTimeLeft <= 0 && t.mState != TimerObj.STATE_DONE
                        && t.mState != TimerObj.STATE_RESTART) {
                    t.mState = TimerObj.STATE_TIMESUP;
                    TimerFragment.this.setTimerButtons(t);
                    if (t.mView != null) {
                        ((TimerListItem)(t.mView)).timesUp();
                    }
                }

                // The blinking
                if (toggle && t.mView != null) {
                    if (t.mState == TimerObj.STATE_TIMESUP) {
                        ((TimerListItem)(t.mView)).setCircleBlink(mVisible);
                    }
                    if (t.mState == TimerObj.STATE_STOPPED) {
                        ((TimerListItem)(t.mView)).setTextBlink(mVisible);
                    }
                }
            }
            mTimersList.postDelayed(mClockTick, 20);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Cache instance data and consume in first call to setupPage()
        if (savedInstanceState != null) {
            mViewState = savedInstanceState;
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG,"TimerFragment - onCreateView");
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.timer_fragment, container, false);
        mTimerContext = getActivity();
        mSoundPoolRun = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        mSoundPoolStart= new SoundPool(4, AudioManager.STREAM_SYSTEM, 0);
        mStreamID = mSoundPoolRun.load(mTimerContext, R.raw.timer_run, 1);
        mSetQuickTimeStreamID = mSoundPoolStart.load(mTimerContext, R.raw.stopwatch_start, 1);
        mAudioManager = (AudioManager)mTimerContext.getSystemService(Context.AUDIO_SERVICE);
        // Handle arguments from parent
//liuqipeng begin
		mListView = (ListView)v.findViewById(R.id.listview);
		for (int i = 0; i < 3; i++) {
			mList.add(false);
		}
		myAdapter = new MyAdapter(mList);
		mListView.setAdapter(myAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				for(int i=0;i<3;i++){
					if(i==position){
						mList.set(i, !((ToggleButton)view.findViewById(R.id.timer_togglebtn)).isChecked());
					}
					else {
						mList.set(i, false);
					}
					if(mList.get(position))
					{
						switch (position) {
							case 0:
								mTimerGroupView.startIntervalAnimation(10 * 60 * 1000);
								break;
							case 1:
								mTimerGroupView.startIntervalAnimation(15 * 60 * 1000);
								break;
							case 2:
								mTimerGroupView.startIntervalAnimation(30 * 60 * 1000);
								break;
							default:
								break;
							}
					
					}
					else{
					mTimerGroupView.startIntervalAnimation(0);
					}
				}
				myAdapter.notifyDataSetChanged();
			}
		});
//liuqipeng 
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(Timers.TIMESUP_MODE)) {
            if (bundle.getBoolean(Timers.TIMESUP_MODE, false)) {
                try {
                    mOnEmptyListListener = (OnEmptyListListener) getActivity();
                } catch (ClassCastException e) {
                    Log.wtf(TAG, getActivity().toString() + " must implement OnEmptyListListener");
                }
            }
        }

        mTimerGroupView = (TimerGroupView) v.findViewById(R.id.fragment_timer_group);
        mTimerGroupView.setBackgroundColor(Color.WHITE);
        mTimerGroupView.setFragment(this);
        mTimerGroupView.setClickSound(mSoundPoolStart, mSetQuickTimeStreamID);
        /*mAddSecondBtn = (ImageButton)v.findViewById(R.id.add_second_btn);
        mAddSecondTitle = (TextView)v.findViewById(R.id.add_second_title);
        mAddSecondBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddSecondBtnClick();
            }
        });

        mAddSecondBtn2 = (ImageButton) v.findViewById(R.id.add_second_btn2);
        mAddSecondTitle2 = (TextView) v.findViewById(R.id.add_second_title2);
        mAddSecondBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddSecondBtnClick();
            }
        });*/

        mStartBtn = v.findViewById(R.id.timer_start_btn);
//liuqipeng begin
        //mStartTitle = (ImageView)v.findViewById(R.id.timer_start_title);
		mStartTitle = (TextView)v.findViewById(R.id.timer_start_title);
//liuqipeng
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long timerLength = mTimerGroupView.getTime();
                if (timerLength == 0) {
                    return;
                }
                if (mTimerObjTag != null ) {
                    TimerFragment.this.onClickHelper(
                            new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
                }
                TA.getInstance().getDefaultTracker().commitEvent("Page_TimerFragment",
                        2101, "button_start", null, null, null);
                TimerObj t = new TimerObj(timerLength * 1000);
                t.isTag = true;
                t.mDeleteAfterUse = true;
                t.mState = TimerObj.STATE_RUNNING;
                mAdapter.addTimer(t);
                updateTimersState(t, Timers.START_TIMER);
                gotoTimersView();
                mTimerSetup.reset();
                mTimersList.setFirstPositionAndOffsets(
                        mAdapter.findTimerPositionById(t.mTimerId), 0);
                SharedPreferences.Editor editor = mPrefs.edit();

                if (mTimerObjTag == null &&  mPrefs.getLong("timerSetTime",0) == 0) {
                    editor.putLong("timerSetTime", t.mSetupLength);
                    editor.putLong("timerStopTimer", 0);
                    editor.apply();
                } else {
                    if (mPrefs.getLong("timerSetTime",0) != 0) {
                        editor.putLong("timerSetTime", mPrefs.getLong("timerSetTime", t.mSetupLength) - mPrefs.getLong("timerStopTimer", 0) + mTimerGroupView.getTime()*1000);
                        editor.putLong("timerStopTimer", 0);
                        editor.apply();
                    }
                }

                mTimerObjTag = t;
                mStartBtn.setVisibility(View.GONE);
                //mAddSecondBtn.setVisibility(View.VISIBLE);
                mStopBtn.setVisibility(View.VISIBLE);
                //mStopBtn.setBackgroundResource(R.drawable.clock_stop_bg);//注释　liuqipeng
                mCancelBtn.setVisibility(View.VISIBLE);
                mStartTitle.setVisibility(View.GONE);
                //mAddSecondTitle.setVisibility(View.VISIBLE);
                mStopTitle.setVisibility(View.VISIBLE);
//liuqipeng 注释
                //mStopTitle.setBackgroundResource(R.drawable.clock_stop);
//liuqipeng
                mCancelTitle.setVisibility(View.VISIBLE);

                int buttonMargin = (int) (getResources().getDimension(R.dimen.button_left_margin));
                ObjectAnimator stopTransAnimator = ObjectAnimator.ofFloat(mStopBtn, "translationX", buttonMargin, 0);
                stopTransAnimator.setDuration(350);
                stopTransAnimator.start();
                ObjectAnimator cancelTransAnimator = ObjectAnimator.ofFloat(mCancelBtn, "translationX", -buttonMargin, 0);
                cancelTransAnimator.setDuration(350);
                cancelTransAnimator.start();

                mTimerGroupView.setDrawableEnable(false);
                editor.putBoolean("isRunning", true);
                editor.apply();
                setTimeDoneView();
//liuqipeng begin
				mListView.setEnabled(false);
//liuqipeng
            }
        });

        mStopBtn = v.findViewById(R.id.timer_stop_btn);
//liuqipeng begin
        //mStopTitle = (ImageView) v.findViewById(R.id.timer_stop_title);
		mStopTitle = (TextView) v.findViewById(R.id.timer_stop_title);
//liuqipeng
        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopBtnClick(false);
//liuqipeng begin
				if((mStopTitle.getText()).equals(getResources().getString(R.string.timer_stop))){
				mStopTitle.setText(getResources().getString(R.string.timer_continue));
				}
				else{
				mStopTitle.setText(getResources().getString(R.string.timer_stop));
				}
//liuqipeng
            }
        });

        /*mStopBtn2 = (ImageButton)v.findViewById(R.id.timer_stop_btn2);
        mStopTitle2 = (TextView)v.findViewById(R.id.timer_stop_title2);
        mStopBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopBtnClick(true);
                mTimerGroupView.setDrawableEnable(true);
            }
        });*/

        mCancelBtn = v.findViewById(R.id.timer_cancel_btn);
//liuqipeng begin
        //mCancelTitle = (ImageView) v.findViewById(R.id.timer_cancel_title);
		mCancelTitle = (TextView) v.findViewById(R.id.timer_cancel_title);
//liuqipeng
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimerObjTag != null) {
                    TimerFragment.this.onClickHelper(
                            new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
                    mTimerObjTag = null;
                }
                TA.getInstance().getDefaultTracker().commitEvent("Page_TimerFragment",
                        2101, "button_cancel", null, null, null);
                mTimerGroupView.setTime(0);
                mStartBtn.setVisibility(View.VISIBLE);
                mStopBtn.setVisibility(View.GONE);
                mCancelBtn.setVisibility(View.GONE);

                mStartTitle.setVisibility(View.VISIBLE);
                mStopTitle.setVisibility(View.GONE);
                mCancelTitle.setVisibility(View.GONE);

                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean("isRunning", false);
                editor.putBoolean("isPauseStatus", false);
                editor.putLong("timerStopTimer", 0);
                editor.putLong("timerSetTime", 0);
                editor.apply();
                setTimeDoneView();
                mTimerGroupView.setDrawableEnable(true);
                mTimerGroupView.resetDrawable();
//liuqipeng begin
				invalidateListView();
				mListView.setEnabled(true);
//liuqipeng
            }
        });

        if (mTimerObjTag != null) {
            mStartBtn.setVisibility(View.GONE);
            //mAddSecondBtn.setVisibility(View.VISIBLE);
            mStopBtn.setVisibility(View.VISIBLE);
            mCancelBtn.setVisibility(View.VISIBLE);
            mStartTitle.setVisibility(View.GONE);
            //mAddSecondTitle.setVisibility(View.VISIBLE);
            mStopTitle.setVisibility(View.VISIBLE);
            mCancelTitle.setVisibility(View.VISIBLE);
        } else {
            mStartBtn.setVisibility(View.VISIBLE);
            //mAddSecondBtn.setVisibility(View.GONE);
            mStopBtn.setVisibility(View.INVISIBLE);
            mCancelBtn.setVisibility(View.GONE);
            mStartTitle.setVisibility(View.VISIBLE);
            //mAddSecondTitle.setVisibility(View.GONE);
            mStopTitle.setVisibility(View.GONE);
            mCancelTitle.setVisibility(View.GONE);
        }

        mTimersList = (StaggeredGridView) v.findViewById(R.id.timers_list);
        // For tablets in landscape, the count will be 2. All else will be 1.
        mColumnCount = getResources().getInteger(R.integer.timer_column_count);
        mTimersList.setColumnCount(mColumnCount);
        // Set this to true; otherwise adding new views to the end of the list won't cause
        // everything above it to be filled in correctly.
        mTimersList.setGuardAgainstJaggedEdges(true);

        mTimersListPage = v.findViewById(R.id.timers_list_page);
        mTimerSetup = (TimerSetupView)v.findViewById(R.id.timer_setup);
        mSeperator = v.findViewById(R.id.timer_button_sep);
        mCancel = (Button)v.findViewById(R.id.timer_cancel);
        mCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getCount() != 0) {
                    gotoTimersView();
                }
            }
        });
        mStart = (Button)v.findViewById(R.id.timer_start);
        mStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // New timer create if timer length is not zero
                // Create a new timer object to track the timer and
                // switch to the timers view.
                int timerLength = mTimerSetup.getTime();
                if (timerLength == 0) {
                    return;
                }
                TimerObj t = new TimerObj(timerLength * 1000);
                t.mState = TimerObj.STATE_RUNNING;
                mAdapter.addTimer(t);
                updateTimersState(t, Timers.START_TIMER);
                gotoTimersView();
                mTimerSetup.reset(); // Make sure the setup is cleared for next time

                mTimersList.setFirstPositionAndOffsets(
                        mAdapter.findTimerPositionById(t.mTimerId), 0);
            }

        });
        mTimerSetup.registerStartButton(mStart);
        mAddTimer = (ImageButton)v.findViewById(R.id.timer_add_timer);
        mAddTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimerSetup.reset();
                gotoSetupView();
            }

        });

        // Put it on the right for landscape, left for portrait.
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) mAddTimer.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams.gravity = Gravity.END;
        } else {
            layoutParams.gravity = Gravity.CENTER;
        }
        mAddTimer.setLayoutParams(layoutParams);

        mTimerFooter = v.findViewById(R.id.timer_footer);
        mTimerFooter.setVisibility(mOnEmptyListListener == null ? View.VISIBLE : View.GONE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mNotificationManager = (NotificationManager)
                getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        Log.d(TAG,"TimerFragment - onCreateView end");
//liuqipeng begin
		mStartBtn.setOnTouchListener(new OnTouchListener() {
		
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				mStartTitle.setAlpha(0.5f);
					break;
				case MotionEvent.ACTION_MOVE:
			
					break;
				case MotionEvent.ACTION_UP:
				mStartTitle.setAlpha(1.0f);
					break;					
				default:
				mStartTitle.setAlpha(1.0f);
					break;
				}
				return false;
			}
		});
		mCancelBtn.setOnTouchListener(new OnTouchListener() {
		
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				mCancelTitle.setAlpha(0.5f);
					break;
				case MotionEvent.ACTION_MOVE:
			
					break;
				case MotionEvent.ACTION_UP:
				mCancelTitle.setAlpha(1.0f);
					break;					
				default:
				mCancelTitle.setAlpha(1.0f);
					break;
				}
				return false;
			}
		});
		mStopBtn.setOnTouchListener(new OnTouchListener() {
		
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				mStopTitle.setAlpha(0.5f);
					break;
				case MotionEvent.ACTION_MOVE:
			
					break;
				case MotionEvent.ACTION_UP:
				mStopTitle.setAlpha(1.0f);
					break;					
				default:
				mStopTitle.setAlpha(1.0f);
					break;
				}
				return false;
			}
		});
//liuqipeng
        return v;
    }

    private void startTimerSound(SoundPool player) {
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(
                    this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    private void onAddSecondBtnClick () {
        TA.getInstance().getDefaultTracker().commitEvent("Page_TimerFragment",
                2101, "button_add15'", null, null, null);
        long timerLength = mTimerGroupView.getTime();
        if (mTimerObjTag != null && mTimerObjTag.mState == TimerObj.STATE_TIMESUP) {
            TimerFragment.this.onClickHelper(new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
            mTimerGroupView.setTime(15000);
            timerLength = mTimerGroupView.getTime();
            TimerObj t = new TimerObj(timerLength * 1000);
            t.isTag = true;
            t.mDeleteAfterUse = true;
            t.mState = TimerObj.STATE_RUNNING;
            mAdapter.addTimer(t);
            updateTimersState(t, Timers.START_TIMER);
            gotoTimersView();
            mTimerSetup.reset();
            mTimersList.setFirstPositionAndOffsets(
                    mAdapter.findTimerPositionById(t.mTimerId), 0);
            SharedPreferences.Editor editor = mPrefs.edit();

            if (mTimerObjTag == null &&  mPrefs.getLong("timerSetTime",0) == 0) {
                editor.putLong("timerSetTime", t.mSetupLength);
                editor.putLong("timerStopTimer", 0);
                editor.apply();
            } else {
                if (mPrefs.getLong("timerSetTime",0) != 0) {
                    editor.putLong("timerSetTime", mPrefs.getLong("timerSetTime", t.mSetupLength) - mPrefs.getLong("timerStopTimer", 0) + mTimerGroupView.getTime()*1000);
                    editor.putLong("timerStopTimer", 0);
                    editor.apply();
                }
            }

            mTimerObjTag = t;
            mStartBtn.setVisibility(View.GONE);
            //mAddSecondBtn.setVisibility(View.VISIBLE);
            mStopBtn.setVisibility(View.VISIBLE);
            mCancelBtn.setVisibility(View.VISIBLE);

            //mAddSecondTitle.setVisibility(View.VISIBLE);
            mStopTitle.setVisibility(View.VISIBLE);
            mCancelTitle.setVisibility(View.VISIBLE);

            //mAddSecondBtn2.setVisibility(View.INVISIBLE);
            //mAddSecondTitle2.setVisibility(View.INVISIBLE);
            //mStopBtn2.setVisibility(View.INVISIBLE);
            //mStopTitle2.setVisibility(View.INVISIBLE);

            editor.putBoolean("isRunning", true);
            editor.apply();
            return;
        }

        if (mTimerObjTag != null) {
            mTimerGroupView.setTime(mTimerGroupView.getTime()*1000+15000);
            timerLength = mTimerGroupView.getTime();
            TimerFragment.this.onClickHelper(
                    new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
            mTimerObjTag = null;
            if (timerLength == 0) {
                return;
            }

            TimerObj t = new TimerObj(timerLength * 1000);
            t.isTag = true;
            t.mDeleteAfterUse = true;
            t.mState = TimerObj.STATE_RUNNING;
            mAdapter.addTimer(t);
            updateTimersState(t, Timers.START_TIMER);
            gotoTimersView();
            mTimerSetup.reset();
            mTimersList.setFirstPositionAndOffsets(
                    mAdapter.findTimerPositionById(t.mTimerId), 0);
            mTimerObjTag = t;
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putLong("timerSetTime", mPrefs.getLong("timerSetTime", 0) + 15000);
            editor.putLong("timerStopTimer", 0);
            editor.apply();
        } else {
            mTimerGroupView.setTime(mTimerGroupView.getTime()*1000+15000);
        }
    }

    private void onStopBtnClick(boolean flag) {
        /*if (mTimerObjTag == null) {
            return;
        }*/
        TA.getInstance().getDefaultTracker().commitEvent("Page_TimerFragment",
                2101, "button_stop", null, null, null);
        SharedPreferences.Editor editor = mPrefs.edit();
        if (mTimerObjTag != null && mTimerObjTag.mState == TimerObj.STATE_TIMESUP) {
            mTimerGroupView.setTime(0);
            mStartBtn.setVisibility(View.VISIBLE);
            //mAddSecondBtn.setVisibility(View.GONE);
            mStopBtn.setVisibility(View.GONE);
            mCancelBtn.setVisibility(View.GONE);

            mStartTitle.setVisibility(View.VISIBLE);
            //mAddSecondTitle.setVisibility(View.GONE);
            mStopTitle.setVisibility(View.GONE);
            mCancelTitle.setVisibility(View.GONE);
            //mTimerGroupView.mDoneText.setVisibility(View.GONE);

            //mAddSecondBtn2.setVisibility(View.INVISIBLE);
            //mAddSecondTitle2.setVisibility(View.INVISIBLE);
            //mStopBtn2.setVisibility(View.INVISIBLE);
            //mStopTitle2.setVisibility(View.INVISIBLE);

            editor.putBoolean("isRunning", false);
            editor.apply();
            editor.putLong("timerStopTimer", mTimerGroupView.getTime()*1000);
            editor.apply();
            TimerFragment.this.onClickHelper(
                    new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
            mTimerObjTag = null;
            if (flag) {
                setTimeDoneView();
            }
        } else {
            long timerLength = mTimerGroupView.getTime();
            if (timerLength <= 1) {
                return;
            }
            //mStartBtn.setVisibility(View.VISIBLE);
            //mAddSecondBtn.setVisibility(View.VISIBLE);
            //mStopBtn.setVisibility(View.GONE);
            //mStopBtn2.setVisibility(View.GONE);
            mCancelBtn.setVisibility(View.VISIBLE);
            //mStartTitle.setVisibility(View.VISIBLE);
            //mAddSecondTitle.setVisibility(View.VISIBLE);
            //mStopTitle.setVisibility(View.GONE);
            //mStopTitle2.setVisibility(View.GONE);
            mCancelTitle.setVisibility(View.VISIBLE);
            Log.d(TAG,"isRunning = "+mPrefs.getBoolean("isRunning",false));
            if (mPrefs.getBoolean("isRunning",false)) {
                //mStopTitle.setBackgroundResource(R.drawable.clock_star);//注释　liuqipeng
                //mStopBtn.setBackgroundResource(R.drawable.clock_star_bg);//注释　liuqipeng
                editor.putLong("timerStopTimer", mTimerGroupView.getTime()*1000);
                editor.apply();
                editor.putBoolean("isRunning", false);
                editor.putBoolean("isPauseStatus", true);
                editor.apply();
                TimerFragment.this.onClickHelper(
                        new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
                mTimerObjTag = null;
                mTimerGroupView.setDrawableEnable(false);
                //mPauseStatus = true;
            } else {
                //mPauseStatus = false;
                if (mTimerObjTag != null ) {
                    TimerFragment.this.onClickHelper(
                            new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
                }
                TA.getInstance().getDefaultTracker().commitEvent("Page_TimerFragment",
                        2101, "button_start", null, null, null);
                TimerObj t = new TimerObj(timerLength * 1000);
                t.isTag = true;
                t.mDeleteAfterUse = true;
                t.mState = TimerObj.STATE_RUNNING;
                mAdapter.addTimer(t);
                updateTimersState(t, Timers.START_TIMER);
                gotoTimersView();
                mTimerSetup.reset();
                mTimersList.setFirstPositionAndOffsets(
                        mAdapter.findTimerPositionById(t.mTimerId), 0);
                
                if (mTimerObjTag == null &&  mPrefs.getLong("timerSetTime",0) == 0) {
                    editor.putLong("timerSetTime", t.mSetupLength);
                    editor.putLong("timerStopTimer", 0);
                    editor.apply();
                } else {
                    if (mPrefs.getLong("timerSetTime",0) != 0) {
                        editor.putLong("timerSetTime", mPrefs.getLong("timerSetTime", t.mSetupLength) - mPrefs.getLong("timerStopTimer", 0) + mTimerGroupView.getTime()*1000);
                        editor.putLong("timerStopTimer", 0);
                        editor.apply();
                    }
                }

                mTimerObjTag = t;
                //mStopTitle.setBackgroundResource(R.drawable.clock_stop);//liuqipeng 注释
                //mStopBtn.setBackgroundResource(R.drawable.clock_stop_bg);//注释　liuqipeng
                mTimerGroupView.setDrawableEnable(false);
                editor.putBoolean("isRunning", true);
                editor.putBoolean("isPauseStatus", false);
                editor.apply();
                setTimeDoneView();
            }
            //editor.putBoolean("isRunning", true);
            //editor.apply();
        }
        //editor.putLong("timerStopTimer", mTimerGroupView.getTime()*1000);
        //editor.apply();
        
        //TimerFragment.this.onClickHelper(
        //        new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
        //mTimerObjTag = null;
        //if (flag) {
        //    setTimeDoneView();
        //}
    }

    @Override
    public void onDestroyView() {
        mViewState = new Bundle();
        saveViewState(mViewState);
        stop();
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLastTimeLeft = 0;
        doOnResume();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void doOnResume() {

        Intent newIntent = null;
        if (getActivity() instanceof DeskClock) {
            DeskClock activity = (DeskClock) getActivity();
            //activity.registerPageChangedListener(this);
            newIntent = activity.getIntent();
        }
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        mAdapter = createAdapter(getActivity(), mPrefs);
        mAdapter.onRestoreInstanceState(null);

        LayoutParams params;
        float dividerHeight = getResources().getDimension(R.dimen.timer_divider_height);
        if (getActivity() instanceof DeskClock) {
            // If this is a DeskClock fragment (i.e. not a FullScreenTimerAlert), add a footer to
            // the bottom of the list so that it can scroll underneath the bottom button bar.
            // StaggeredGridView doesn't support a footer view, but GridAdapter does, so this
            // can't happen until the Adapter itself is instantiated.

            mTimerGroupView.mTimerClockView.mListener.onProgressChange(mTimerGroupView.mTimerClockView, mTimerGroupView.mTimerClockView.getProgress());
            mTimerGroupView.mTimerClockView.invalidate();

            View footerView = getActivity().getLayoutInflater().inflate(
                    R.layout.blank_footer_view, mTimersList, false);
            params = footerView.getLayoutParams();
            params.height -= dividerHeight;
            footerView.setLayoutParams(params);
            footerView.setBackgroundResource(R.color.blackish);
            mAdapter.setFooterView(footerView);
        }

        if (mPrefs.getBoolean(Timers.FROM_NOTIFICATION, false)) {
            // Clear the flag set in the notification because the adapter was just
            // created and is thus in sync with the database
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(Timers.FROM_NOTIFICATION, false);
            editor.apply();
        }
        if (mPrefs.getBoolean(Timers.FROM_ALERT, false)) {
            // Clear the flag set in the alert because the adapter was just
            // created and is thus in sync with the database
            
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(Timers.FROM_ALERT, false);
            editor.apply();
        }

        mTimersList.setAdapter(mAdapter);
        mLastVisibleView = null;   // Force a non animation setting of the view
        // View was hidden in onPause, make sure it is visible now.
        /*View v = getView();
        if (v != null) {
            getView().setVisibility(View.VISIBLE);
        }*/

        if (newIntent != null) {
            processIntent(newIntent);
        }
        if (mTimerObjTag != null) {
            if (mTimerObjTag.mState == TimerObj.STATE_TIMESUP){
                mTimerGroupView.setTime(0);
                mStartBtn.setVisibility(View.VISIBLE);
                mStopBtn.setVisibility(View.GONE);
                mCancelBtn.setVisibility(View.GONE);
                mStartTitle.setVisibility(View.VISIBLE);
                mStopTitle.setVisibility(View.GONE);
                mCancelTitle.setVisibility(View.GONE);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean("isRunning", false);
                editor.apply();
                TimerFragment.this.onClickHelper(
                        new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
                Intent timeIntent = new Intent();
                timeIntent.setAction(AliTimeScreen.TIME_FINISH_ACTION);
                getActivity().sendBroadcast(timeIntent);
                mTimerObjTag = null;
                mTimerGroupView.resetDrawable();
                mTimerGroupView.setDrawableEnable(true);
            } else {
                mTimerGroupView.setTime(mPrefs.getLong("setTime", 0)*1000);
                if (mPrefs.getBoolean("isRunning", false)) {
                    gotoTimersView();
                    mStartBtn.setVisibility(View.GONE);
                    //mAddSecondBtn.setVisibility(View.VISIBLE);
                    mStopBtn.setVisibility(View.VISIBLE);
                    mCancelBtn.setVisibility(View.VISIBLE);
                    mStartTitle.setVisibility(View.GONE);
                    //mAddSecondTitle.setVisibility(View.VISIBLE);
                    mStopTitle.setVisibility(View.VISIBLE);
                    mCancelTitle.setVisibility(View.VISIBLE);
                    mTimerGroupView.setDrawableEnable(false);
                } else {
                    /*mStartBtn.setVisibility(View.VISIBLE);
                    //mAddSecondBtn.setVisibility(View.GONE);
                    mStopBtn.setVisibility(View.GONE);
                    mCancelBtn.setVisibility(View.GONE);
                    mStartTitle.setVisibility(View.VISIBLE);
                    //mAddSecondTitle.setVisibility(View.GONE);
                    mStopTitle.setVisibility(View.GONE);
                    mCancelTitle.setVisibility(View.GONE);
                    mTimerGroupView.setDrawableEnable(true);*/
                    boolean mPauseStatus = mPrefs.getBoolean("isPauseStatus", false);
                    if (!mPauseStatus) {
                        mStartBtn.setVisibility(View.VISIBLE);
                        //mAddSecondBtn.setVisibility(View.GONE);
                        mStopBtn.setVisibility(View.GONE);
                        //mStopBtn.setBackgroundResource(R.drawable.clock_stop_bg);//注释　liuqipeng
                        mCancelBtn.setVisibility(View.GONE);
                        mStartTitle.setVisibility(View.VISIBLE);
                        //mAddSecondTitle.setVisibility(View.GONE);
                        mStopTitle.setVisibility(View.GONE);
                        //mStopTitle.setBackgroundResource(R.drawable.clock_stop);//liuqipeng 注释
                        mCancelTitle.setVisibility(View.GONE);
                        mTimerGroupView.setDrawableEnable(true);
                    } else {
                        mStartBtn.setVisibility(View.GONE);
                        mStopBtn.setVisibility(View.VISIBLE);
                        //mStopBtn.setBackgroundResource(R.drawable.clock_star_bg);//注释　liuqipeng
                        mCancelBtn.setVisibility(View.VISIBLE);
                        mStartTitle.setVisibility(View.GONE);
                        mStopTitle.setVisibility(View.VISIBLE);
                        //mStopTitle.setBackgroundResource(R.drawable.clock_star);//liuqipeng 注释
                        mCancelTitle.setVisibility(View.VISIBLE);
                        mTimerGroupView.setDrawableEnable(true);
                    }
                }
            }
            /*gotoTimersView();
            if (mPrefs.getBoolean("isRunning", false)) {
                mStartBtn.setVisibility(View.GONE);
                //mAddSecondBtn.setVisibility(View.VISIBLE);
                mStopBtn.setVisibility(View.VISIBLE);
                mCancelBtn.setVisibility(View.VISIBLE);
                mStartTitle.setVisibility(View.GONE);
                //mAddSecondTitle.setVisibility(View.VISIBLE);
                mStopTitle.setVisibility(View.VISIBLE);
                mCancelTitle.setVisibility(View.VISIBLE);
                mTimerGroupView.setDrawableEnable(false);
            } else {
                
                mStartBtn.setVisibility(View.VISIBLE);
                //mAddSecondBtn.setVisibility(View.GONE);
                mStopBtn.setVisibility(View.GONE);
                mCancelBtn.setVisibility(View.GONE);
                mStartTitle.setVisibility(View.VISIBLE);
                //mAddSecondTitle.setVisibility(View.GONE);
                mStopTitle.setVisibility(View.GONE);
                mCancelTitle.setVisibility(View.GONE);
                mTimerGroupView.setDrawableEnable(true);
            }*/
        } else {
            if (mPrefs.getBoolean("isRunning", false)) {
                mStartBtn.setVisibility(View.VISIBLE);
                //mAddSecondBtn.setVisibility(View.VISIBLE);
                mStopBtn.setVisibility(View.GONE);
                mCancelBtn.setVisibility(View.VISIBLE);
                mStartTitle.setVisibility(View.VISIBLE);
                //mAddSecondTitle.setVisibility(View.VISIBLE);
                mStopTitle.setVisibility(View.GONE);
                mCancelTitle.setVisibility(View.VISIBLE);
                mTimerGroupView.setTime(mPrefs.getLong("setTime", 0)*1000);
                mTimerGroupView.setDrawableEnable(false);
            } else {
                boolean mPauseStatus = mPrefs.getBoolean("isPauseStatus", false);
                if (!mPauseStatus) {
                    mStartBtn.setVisibility(View.VISIBLE);
                    //mAddSecondBtn.setVisibility(View.GONE);
                    mStopBtn.setVisibility(View.GONE);
                    //mStopBtn.setBackgroundResource(R.drawable.clock_stop_bg);//注释　liuqipeng
                    mCancelBtn.setVisibility(View.GONE);
                    mStartTitle.setVisibility(View.VISIBLE);
                    //mAddSecondTitle.setVisibility(View.GONE);
                    mStopTitle.setVisibility(View.GONE);
                    //mStopTitle.setBackgroundResource(R.drawable.clock_stop);//liuqipeng 注释
                    mCancelTitle.setVisibility(View.GONE);
                    mTimerGroupView.setDrawableEnable(true);
                } else {
                    mStartBtn.setVisibility(View.GONE);
                    mStopBtn.setVisibility(View.VISIBLE);
                    //mStopBtn.setBackgroundResource(R.drawable.clock_star_bg);//注释　liuqipeng
                    mCancelBtn.setVisibility(View.VISIBLE);
                    mStartTitle.setVisibility(View.GONE);
                    mStopTitle.setVisibility(View.VISIBLE);
                    //mStopTitle.setBackgroundResource(R.drawable.clock_star);//liuqipeng 注释
                    mCancelTitle.setVisibility(View.VISIBLE);
                    mTimerGroupView.setDrawableEnable(false);
                }
                mTimerGroupView.setTime(mPrefs.getLong("setTime", 0)*1000);
                //mTimerGroupView.setDrawableEnable(true);//liuqipeng 注释
                long timerLength = mTimerGroupView.getTime();
                Log.d(TAG,"timerLength = "+timerLength);
                if (timerLength == 0) {
                    mTimerGroupView.resetDrawable();
                }
            }
        }
        mTimerGroupView.onSelectTime();
        /*long timeLeft = 1;
        if (mTimerObjTag != null) {
            timeLeft = mTimerObjTag.updateTimeLeft(false);
        }
        if ((mTimerObjTag != null && mTimerObjTag.mState == TimerObj.STATE_TIMESUP) || timeLeft < 0) {
            setTimeupView();
            if (!isFullScreen) {
                //mAddSecondBtn2.setImageResource(R.drawable.aui_btn_add_time);
                //mStopBtn2.setImageResource(R.drawable.aui_btn_stop);
                //mAddSecondTitle2.setTextColor(Color.BLACK);
                //mStopTitle2.setTextColor(Color.BLACK);
                //mAddSecondBtn.setAlpha((float) 1.0);
                mStopBtn.setAlpha((float) 1.0);
            }
        } else {
            this.mTimerGroupView.setTime(mPrefs.getLong("setTime", 0)*1000);
            //setTimeDoneView();
            //mAddSecondBtn2.setVisibility(View.GONE);
            //mAddSecondTitle2.setVisibility(View.GONE);
            //mStopBtn2.setVisibility(View.GONE);
            //mStopTitle2.setVisibility(View.GONE);
        }*/
    }

    private void setTimeupView() {
        mTimerGroupView.setBackgroundColor(color.white);
        SharedPreferences.Editor editor = mPrefs.edit();
        long seconds, minutes, hours;
        seconds = mPrefs.getLong("timerSetTime", 0) / 1000;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 999) {
            hours = 0;
        }
        editor.putLong("timerSetTime", 0);
        editor.putLong("timerStopTimer", 0);
        String hStr = hours!=0?(hours+getResources().getString(R.string.hours_label_description)):"";
        String mStr = minutes!=0?(minutes+getResources().getString(R.string.minutes_label_description)):"";
        String sStr = seconds!=0?(seconds+getResources().getString(R.string.seconds_label_description)):"";

        //this.mTimerGroupView.mDoneText.setText(hStr + mStr + sStr +getResources().getString(R.string.timer_times_up));

        editor.putBoolean("isRunning", false);
        editor.putBoolean("isTimeUp",true);
        editor.putLong("setTime", 0);
        editor.apply();

        mStartBtn.setVisibility(View.GONE);
        //mAddSecondBtn.setVisibility(View.VISIBLE);
        mStopBtn.setVisibility(View.VISIBLE);
        mCancelBtn.setVisibility(View.GONE);
        mStartTitle.setVisibility(View.GONE);
        //mAddSecondTitle.setVisibility(View.VISIBLE);
        mStopTitle.setVisibility(View.VISIBLE);
        mCancelTitle.setVisibility(View.GONE);
        //this.mTimerGroupView.mDoneText.setVisibility(View.GONE);
        //mAddSecondBtn.setAlpha((float) 0.5);
        mStopBtn.setAlpha((float) 0.5);

        //TODO: change the display from transation to change visibility
        //final float scale = getResources().getDisplayMetrics().density;
        //FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mAddSecondBtn.getLayoutParams();
        //lp.setMarginStart((int) (100*scale+0.5f));
        //mAddSecondBtn.setLayoutParams(lp);
        //mAddSecondBtn.setVisibility(View.GONE);
        //mAddSecondTitle.setVisibility(View.GONE);
        //mAddSecondBtn2.setVisibility(View.VISIBLE);
        //mAddSecondTitle2.setVisibility(View.VISIBLE);

        //lp = (FrameLayout.LayoutParams) mStopBtn.getLayoutParams();
        //lp.setMarginStart((int) (205*scale+0.5f));
        //lp.gravity = Gravity.CENTER_VERTICAL;
        //mStopBtn.setLayoutParams(lp);
        mStopBtn.setVisibility(View.GONE);
        mStopTitle.setVisibility(View.GONE);
        //mStopBtn2.setVisibility(View.VISIBLE);
        //mStopTitle2.setVisibility(View.VISIBLE);

        //lp = (FrameLayout.LayoutParams) mAddSecondTitle.getLayoutParams();
        //lp.setMargins((int) (100*scale+0.5f), lp.topMargin, lp.rightMargin, lp.bottomMargin);
        //mAddSecondTitle.setLayoutParams(lp);

        //lp = (FrameLayout.LayoutParams) mStopTitle.getLayoutParams();
        //lp.setMargins((int) (205*scale+0.5f), lp.topMargin, lp.rightMargin, lp.bottomMargin);
        //lp.gravity = Gravity.BOTTOM;
        //mStopTitle.setLayoutParams(lp);
    }

    private void setTimeDoneView() {
        //TODO: change the display from transation to change visibility, don't forget the click function.
/*        mAddSecondBtn.setAlpha((float) 1.0);
        mStopBtn.setAlpha((float) 1.0);
        mTimerGroupView.setBackgroundColor(color.transparent);
        this.mTimerGroupView.mDoneText.setVisibility(View.GONE);

        final float scale = getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mAddSecondBtn.getLayoutParams();
        lp.setMarginStart((int) (80*scale+0.5f));
        //mAddSecondBtn.setLayoutParams(lp);
        
        lp = (FrameLayout.LayoutParams) mStopBtn.getLayoutParams();
        lp.setMarginStart((int) (16*scale+0.5f));
        lp.gravity = Gravity.CENTER;
        //mStopBtn.setLayoutParams(lp);

        lp = (FrameLayout.LayoutParams) mAddSecondTitle.getLayoutParams();
        lp.setMargins((int) (80*scale+0.5f), lp.topMargin, lp.rightMargin, lp.bottomMargin);
        //mAddSecondTitle.setLayoutParams(lp);
        
        lp = (FrameLayout.LayoutParams) mStopTitle.getLayoutParams();
        lp.setMargins(0, lp.topMargin, lp.rightMargin, lp.bottomMargin);
        lp.gravity = Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM;*/
        //mStopTitle.setLayoutParams(lp);

        //mAddSecondBtn.setVisibility(View.VISIBLE);
        //mAddSecondTitle.setVisibility(View.VISIBLE);
        //mAddSecondBtn2.setVisibility(View.GONE);
        //mAddSecondTitle2.setVisibility(View.GONE);

        mStopBtn.setVisibility(View.VISIBLE);
        mStopTitle.setVisibility(View.VISIBLE);
        //mStopBtn2.setVisibility(View.GONE);
        //mStopTitle2.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        if (mPrefs != null && mTimerGroupView != null) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putLong("setTime", mTimerGroupView.getTime());
            editor.apply();
        }
        super.onDestroy();
    }

    @Override
    public void onPause() {
        /*SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong("setTime", mTimerGroupView.getTime());
        editor.apply();*/

        /*if (getActivity() instanceof DeskClock) {
            ((DeskClock)getActivity()).unregisterPageChangedListener(this);
        }*/
        super.onPause();
        /*stopClockTicks();
        if (mAdapter != null) {
            mAdapter.saveGlobalState ();
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);*/
        // This is called because the lock screen was activated, the window stay
        // active under it and when we unlock the screen, we see the old time for
        // a fraction of a second.
        /*View v = getView();
        if (v != null) {
            v.setVisibility(View.INVISIBLE);
        }*/
    }

    @Override
    public void onStop() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong("setTime", mTimerGroupView.getTime());
        editor.apply();

        /*if (getActivity() instanceof DeskClock) {
            ((DeskClock)getActivity()).unregisterPageChangedListener(this);
        }*/
        stopClockTicks();
        if (mAdapter != null) {
            mAdapter.saveGlobalState ();
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    /*@Override
    public void onPageChanged(int page) {
        if (page == DeskClock.TIMER_TAB_INDEX && mAdapter != null) {
            mAdapter.sort();
        }
    }*/

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            mAdapter.onSaveInstanceState (outState);
        }
        if (mTimerSetup != null) {
            saveViewState(outState);
        } else if (mViewState != null) {
            outState.putAll(mViewState);
        }
    }

    private void saveViewState(Bundle outState) {
        outState.putBoolean(KEY_SETUP_SELECTED, mTimerSetup.getVisibility() == View.VISIBLE);
        mTimerSetup.saveEntryState(outState, KEY_ENTRY_STATE);
    }

    public void setPage() {
        boolean switchToSetupView;
        if (mViewState != null) {
            switchToSetupView = mViewState.getBoolean(KEY_SETUP_SELECTED, false);
            mTimerSetup.restoreEntryState(mViewState, KEY_ENTRY_STATE);
            mViewState = null;
        } else {
            switchToSetupView = mAdapter.getCount() == 0;
        }
        if (switchToSetupView) {
            gotoSetupView();
        } else {
            gotoTimersView();
        }
    }

    public void stopAllTimesUpTimers() {
        boolean notifyChange = false;
        //  To avoid race conditions where a timer was dismissed and it is still in the timers list
        // and can be picked again, create a temporary list of timers to be removed first and
        // then removed them one by one
        mTimerObjTag = TimerObj.getTagTimersFromSharedPrefs(mAdapter.mmPrefs, true);
        if (mTimerObjTag != null) {
            if (mTimerObjTag != null && mTimerObjTag.mState == TimerObj.STATE_TIMESUP) {
                notifyChange = true;
                mTimerGroupView.setTime(0);
                mStartBtn.setVisibility(View.VISIBLE);
                //mAddSecondBtn.setVisibility(View.GONE);
                mStopBtn.setVisibility(View.INVISIBLE);
                mCancelBtn.setVisibility(View.GONE);

                mStartTitle.setVisibility(View.VISIBLE);
                //mAddSecondTitle.setVisibility(View.GONE);
                mStopTitle.setVisibility(View.GONE);
                mCancelTitle.setVisibility(View.GONE);
            } else {
                mStartBtn.setVisibility(View.VISIBLE);
                //mAddSecondBtn.setVisibility(View.VISIBLE);
                mStopBtn.setVisibility(View.INVISIBLE);
                mCancelBtn.setVisibility(View.VISIBLE);

                mStartTitle.setVisibility(View.VISIBLE);
                //mAddSecondTitle.setVisibility(View.VISIBLE);
                mStopTitle.setVisibility(View.GONE);
                mCancelTitle.setVisibility(View.VISIBLE);
            }

            TimerFragment.this.onClickHelper(
                    new ClickAction(ClickAction.ACTION_DELETE, mTimerObjTag));
            mTimerObjTag = null;
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean("isRunning", false);
            editor.putLong("timerStopTimer", 0);
            editor.putLong("timerSetTime", 0);
            editor.putLong("setTime", 0);
            editor.apply();
            mTimerGroupView.setTime(0);
            mTimerGroupView.resetDrawable();
            mTimerGroupView.setDrawableEnable(true);
        }

        LinkedList<TimerObj> timesupTimers = new LinkedList<TimerObj>();
        for (int i = 0; i  < mAdapter.getCount(); i ++) {
            TimerObj timerObj = mAdapter.getItem(i);
            if (timerObj.mState == TimerObj.STATE_TIMESUP) {
                timesupTimers.addFirst(timerObj);
                notifyChange = true;
            }
        }

        while (timesupTimers.size() > 0) {
            onStopButtonPressed(timesupTimers.remove());
        }

        if (notifyChange) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(Timers.FROM_ALERT, true);
            editor.apply();
        }
    }

    private void gotoSetupView() {
        if (mLastVisibleView == null || mLastVisibleView.getId() == R.id.timer_setup) {
            mTimerSetup.setVisibility(View.VISIBLE);
            mTimerSetup.setScaleX(1f);
            mTimersListPage.setVisibility(View.GONE);
        } else {
            // Animate
            ObjectAnimator a = ObjectAnimator.ofFloat(mTimersListPage, View.SCALE_X, 1f, 0f);
            a.setInterpolator(new AccelerateInterpolator());
            a.setDuration(125);
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mTimersListPage.setVisibility(View.GONE);
                    mTimerSetup.setScaleX(0);
                    mTimerSetup.setVisibility(View.VISIBLE);
                    ObjectAnimator b = ObjectAnimator.ofFloat(mTimerSetup, View.SCALE_X, 0f, 1f);
                    b.setInterpolator(new DecelerateInterpolator());
                    b.setDuration(225);
                    b.start();
                }
            });
            a.start();

        }
        stopClockTicks();
        if (mAdapter.getCount() == 0) {
            mCancel.setVisibility(View.GONE);
            mSeperator.setVisibility(View.GONE);
        } else {
            mSeperator.setVisibility(View.VISIBLE);
            mCancel.setVisibility(View.VISIBLE);
        }
        mTimerSetup.updateStartButton();
        mTimerSetup.updateDeleteButton();
        mLastVisibleView = mTimerSetup;
    }
    private void gotoTimersView() {
        if (mLastVisibleView == null || mLastVisibleView.getId() == R.id.timers_list_page) {
            mTimerSetup.setVisibility(View.GONE);
            mTimersListPage.setVisibility(View.VISIBLE);
            mTimersListPage.setScaleX(1f);
        } else {
            // Animate
            ObjectAnimator a = ObjectAnimator.ofFloat(mTimerSetup, View.SCALE_X, 1f, 0f);
            a.setInterpolator(new AccelerateInterpolator());
            a.setDuration(125);
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mTimerSetup.setVisibility(View.GONE);
                    mTimersListPage.setScaleX(0);
                    mTimersListPage.setVisibility(View.VISIBLE);
                    ObjectAnimator b =
                            ObjectAnimator.ofFloat(mTimersListPage, View.SCALE_X, 0f, 1f);
                    b.setInterpolator(new DecelerateInterpolator());
                    b.setDuration(225);
                    b.start();
                }
            });
            a.start();
        }
        startClockTicks();
        mLastVisibleView = mTimersListPage;
    }

    @Override
    public void onClick(View v) {
        ClickAction tag = (ClickAction) v.getTag();
        onClickHelper(tag);
    }

    public void onClickHelper(com.android.deskclock.timer.TimerReceiver.ClickAction clickAction) {
        switch (clickAction.mAction) {
            case ClickAction.ACTION_DELETE:
                final TimerObj t = clickAction.mTimer;
                if (t.mState == TimerObj.STATE_TIMESUP) {
                    cancelTimerNotification(t.mTimerId);
                }
                // Tell receiver the timer was deleted.
                // It will stop all activity related to the
                // timer
                t.mState = TimerObj.STATE_DELETED;
                updateTimersState(t, Timers.DELETE_TIMER);
                mTimerObjTag = null;
                break;
            case ClickAction.ACTION_PLUS_ONE:
                onPlusOneButtonPressed(clickAction.mTimer);
                setTimerButtons(clickAction.mTimer);
                break;
            case ClickAction.ACTION_STOP:
                onStopButtonPressed(clickAction.mTimer);
                setTimerButtons(clickAction.mTimer);
                break;
            default:
                break;
        }
    }

        public void onClickHelper(ClickAction clickAction) {
            switch (clickAction.mAction) {
                case ClickAction.ACTION_DELETE:
                    final TimerObj t = clickAction.mTimer;
                    if (t.mState == TimerObj.STATE_TIMESUP) {
                        cancelTimerNotification(t.mTimerId);
                    }
                    // Tell receiver the timer was deleted.
                    // It will stop all activity related to the
                    // timer
                    t.mState = TimerObj.STATE_DELETED;
                    updateTimersState(t, Timers.DELETE_TIMER);
                    mTimerObjTag = null;
                    break;
                case ClickAction.ACTION_PLUS_ONE:
                    onPlusOneButtonPressed(clickAction.mTimer);
                    setTimerButtons(clickAction.mTimer);
                    break;
                case ClickAction.ACTION_STOP:
                    onStopButtonPressed(clickAction.mTimer);
                    setTimerButtons(clickAction.mTimer);
                    break;
                default:
                    break;
            }
    }

    private void onPlusOneButtonPressed(TimerObj t) {
        switch(t.mState) {
            case TimerObj.STATE_RUNNING:
                 t.addTime(TimerObj.MINUTE_IN_MILLIS);
                 long timeLeft = t.updateTimeLeft(false);
                 ((TimerListItem)(t.mView)).setTime(timeLeft, false);
                 ((TimerListItem)(t.mView)).setLength(timeLeft);
                 mAdapter.notifyDataSetChanged();
                 updateTimersState(t, Timers.TIMER_UPDATE);
                break;
            case TimerObj.STATE_TIMESUP:
                // +1 min when the time is up will restart the timer with 1 minute left.
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = Utils.getTimeNow();
                t.mTimeLeft = t. mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
                ((TimerListItem)t.mView).setTime(t.mTimeLeft, false);
                ((TimerListItem)t.mView).set(t.mOriginalLength, t.mTimeLeft, true);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.TIMER_RESET);
                updateTimersState(t, Timers.START_TIMER);
                updateTimesUpMode(t);
                cancelTimerNotification(t.mTimerId);
                break;
            case TimerObj.STATE_STOPPED:
            case TimerObj.STATE_DONE:
                t.mState = TimerObj.STATE_RESTART;
                t.mTimeLeft = t. mOriginalLength = t.mSetupLength;
                ((TimerListItem)t.mView).stop();
                ((TimerListItem)t.mView).setTime(t.mTimeLeft, false);
                ((TimerListItem)t.mView).set(t.mOriginalLength, t.mTimeLeft, false);
                updateTimersState(t, Timers.TIMER_RESET);
                break;
            default:
                break;
        }
    }

    private void onStopButtonPressed(TimerObj t) {
        switch(t.mState) {
            case TimerObj.STATE_RUNNING:
                // Stop timer and save the remaining time of the timer
                t.mState = TimerObj.STATE_STOPPED;
                ((TimerListItem) t.mView).pause();
                t.updateTimeLeft(true);
                updateTimersState(t, Timers.TIMER_STOP);
                break;
            case TimerObj.STATE_STOPPED:
                // Reset the remaining time and continue timer
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.START_TIMER);
                break;
            case TimerObj.STATE_TIMESUP:
                if (t.mDeleteAfterUse) {
                    cancelTimerNotification(t.mTimerId);
                    // Tell receiver the timer was deleted.
                    // It will stop all activity related to the
                    // timer
                    t.mState = TimerObj.STATE_DELETED;
                    updateTimersState(t, Timers.DELETE_TIMER);
                } else {
                    t.mState = TimerObj.STATE_DONE;
                    // Used in a context where the timer could be off-screen and without a view
                    if (t.mView != null) {
                        ((TimerListItem) t.mView).done();
                    }
                    updateTimersState(t, Timers.TIMER_DONE);
                    cancelTimerNotification(t.mTimerId);
                    updateTimesUpMode(t);
                }
                break;
            case TimerObj.STATE_DONE:
                break;
            case TimerObj.STATE_RESTART:
                t.mState = TimerObj.STATE_RUNNING;
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                ((TimerListItem) t.mView).start();
                updateTimersState(t, Timers.START_TIMER);
                break;
            default:
                break;
        }
    }

    private void deleteTimer(TimerObj t) {
        mAdapter.deleteTimer(t.mTimerId);
        mTimersList.setSelectionToTop();
        if (mAdapter.getCount() == 0) {
            if (mOnEmptyListListener == null) {
                mTimerSetup.reset();
                gotoSetupView();
            } else {
                mOnEmptyListListener.onEmptyList();
            }
        }
    }

    private void onLabelPressed(TimerObj t) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(t, t.mLabel, getTag());
        newFragment.show(ft, "label_dialog");
    }

    public void setLabel(TimerObj timer, String label) {
        mAdapter.getItem(mAdapter.findTimerPositionById(timer.mTimerId)).mLabel = label;
        updateTimersState(timer, Timers.TIMER_UPDATE);
        // Make sure the new label is visible.
        mAdapter.notifyDataSetChanged();
    }

    private void setTimerButtons(TimerObj t) {
        Context a = getActivity();
        if (a == null || t == null || t.mView == null) {
            return;
        }
        ImageButton leftButton = (ImageButton) t.mView.findViewById(R.id.timer_plus_one);
        CountingTimerView countingTimerView = (CountingTimerView)
                t.mView.findViewById(R.id.timer_time_text);
        TextView stop = (TextView) t.mView.findViewById(R.id.timer_stop);
        ImageButton delete = (ImageButton) t.mView.findViewById(R.id.timer_delete);
        // Make sure the delete button is visible in case the view is recycled.
        delete.setVisibility(View.VISIBLE);

        Resources r = a.getResources();
        switch (t.mState) {
            case TimerObj.STATE_RUNNING:
                // left button is +1m
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setContentDescription(r.getString(R.string.timer_plus_one));
                leftButton.setImageResource(R.drawable.ic_plusone);
                leftButton.setEnabled(canAddMinute(t));
                stop.setVisibility(View.VISIBLE);
                stop.setContentDescription(r.getString(R.string.timer_stop));
                stop.setText(R.string.timer_stop);
                stop.setTextColor(Color.BLACK);
                countingTimerView.setVirtualButtonEnabled(true);
                break;
            case TimerObj.STATE_STOPPED:
                // left button is reset
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setContentDescription(r.getString(R.string.timer_reset));
                leftButton.setImageResource(R.drawable.ic_reset);
                leftButton.setEnabled(true);
                stop.setVisibility(View.VISIBLE);
                stop.setContentDescription(r.getString(R.string.timer_start));
                stop.setText(R.string.timer_start);
                stop.setTextColor(Color.BLACK);
                countingTimerView.setVirtualButtonEnabled(true);
                break;
            case TimerObj.STATE_TIMESUP:
                // left button is +1m
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setContentDescription(r.getString(R.string.timer_plus_one));
                leftButton.setImageResource(R.drawable.ic_plusone);
                leftButton.setEnabled(true);
                stop.setVisibility(View.VISIBLE);
                stop.setContentDescription(r.getString(R.string.timer_stop));
                // If the timer is deleted after use , show "done" instead of "stop" on the button
                // and hide the delete button since pressing done will delete the timer
                stop.setText(t.mDeleteAfterUse ? R.string.timer_done : R.string.timer_stop);
                stop.setTextColor(Color.BLACK);
                delete.setVisibility(t.mDeleteAfterUse ? View.INVISIBLE : View.VISIBLE);
                countingTimerView.setVirtualButtonEnabled(true);
                break;
            case TimerObj.STATE_DONE:
                // left button is reset
                leftButton.setVisibility(View.VISIBLE);
                leftButton.setContentDescription(r.getString(R.string.timer_reset));
                leftButton.setImageResource(R.drawable.ic_reset);
                leftButton.setEnabled(true);
                stop.setVisibility(View.INVISIBLE);
                countingTimerView.setVirtualButtonEnabled(false);
                break;
            case TimerObj.STATE_RESTART:
                leftButton.setVisibility(View.INVISIBLE);
                leftButton.setEnabled(true);
                stop.setVisibility(View.VISIBLE);
                stop.setContentDescription(r.getString(R.string.timer_start));
                stop.setText(R.string.timer_start);
                stop.setTextColor(Color.BLACK);
                countingTimerView.setVirtualButtonEnabled(true);
                break;
            default:
                break;
        }
    }

    // Starts the ticks that animate the timers.
    private void startClockTicks() {
        if (mPlaySound) {
            mFirstPlay = true;
        }
        mTimersList.postDelayed(mClockTick, 20);
        mTicking = true;
    }

    // Stops the ticks that animate the timers.
    private void stopClockTicks() {
        if (mPlaySound) {
            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(this);
            }
        }
        if (mTicking) {
            mTimersList.removeCallbacks(mClockTick);
            mTicking = false;
        }
    }

    private boolean canAddMinute(TimerObj t) {
        return TimerObj.MAX_TIMER_LENGTH - t.mTimeLeft > TimerObj.MINUTE_IN_MILLIS ? true : false;
    }

    public void updateTimersState(TimerObj t, String action) {
        if (Timers.DELETE_TIMER.equals(action)) {
            deleteTimer(t);
        } else {
            t.writeToSharedPref(mPrefs);
        }
        Intent i = new Intent();
        i.setAction(action);
        i.putExtra(Timers.TIMER_INTENT_EXTRA, t.mTimerId);
        // Make sure the receiver is getting the intent ASAP.
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        getActivity().sendBroadcast(i);
    }

    public void cancelTimerNotification(int timerId) {
        mNotificationManager.cancel(timerId);
    }

    public void updateTimesUpMode(TimerObj timerObj) {
        if (mOnEmptyListListener != null && timerObj.mState != TimerObj.STATE_TIMESUP) {
            mAdapter.removeTimer(timerObj);
            if (mAdapter.getCount() == 0) {
                mOnEmptyListListener.onEmptyList();
            } else {
                mOnEmptyListListener.onListChanged();
            }
        }
    }

    public void restartAdapter() {
        mAdapter = createAdapter(getActivity(), mPrefs);
        mAdapter.onRestoreInstanceState(null);
    }

    // Process extras that were sent to the app and were intended for the timer
    // fragment
    public void processIntent(Intent intent) {
        // switch to timer setup view
        if (intent.getBooleanExtra(GOTO_SETUP_VIEW, false)) {
            gotoSetupView();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(mPrefs)) {
            if ((key.equals(Timers.FROM_ALERT) && prefs.getBoolean(Timers.FROM_ALERT, false))
                    || (key.equals(Timers.FROM_NOTIFICATION)
                    && prefs.getBoolean(Timers.FROM_NOTIFICATION, false))) {
                // The data-changed flag was set in the alert or notification so the adapter needs
                // to re-sync with the database
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(key, false);
                editor.apply();
                mAdapter = createAdapter(getActivity(), mPrefs);
                mAdapter.onRestoreInstanceState(null);
                mTimersList.setAdapter(mAdapter);
            }
        }
    }

    public static TimerFragment getInstance() {
        if(mInstance == null) {
            mInstance = new TimerFragment();
        }
        return mInstance;
    }

    public void removeCircleLayout() {
        if (mTimerGroupView != null) {
            if (mTimerGroupView.getTimerSetView() != null) {
                mTimerGroupView.getTimerSetView().setVisibility(View.INVISIBLE);
            }
            if (mTimerGroupView.getTimerTextView() != null) {
                mTimerGroupView.getTimerTextView().setVisibility(View.INVISIBLE);
            }
        }
    }

    public void onDetach() {
        super.onDetach();
        mTimerContext = null;
        mInstance = null;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mTimerContext = activity;
        mInstance = this;
    }

    public void showCircleLayout() {
        if (mTimerGroupView != null) {
            if (mTimerGroupView.getTimerSetView() != null) {
                mTimerGroupView.getTimerSetView().setVisibility(View.VISIBLE);
            }
            if (mTimerGroupView.getTimerTextView() != null) {
//liuqipeng 注销掉
               // mTimerGroupView.getTimerTextView().setVisibility(View.VISIBLE);
//liuqipeng 
            }
        }
    }

    public void onAnimationUpdate(float positionOffset, boolean isToClock) {
        ArgbEvaluator evaluator = new ArgbEvaluator();
        int evaluate = isToClock ? (Integer) evaluator.evaluate(positionOffset, 0XFF38BA78, 0XFF624BC6)
                : (Integer) evaluator.evaluate(positionOffset, 0XFF624BC6, 0XFF00B1B1);
        if (mTimerGroupView != null) {
            mTimerGroupView.getTimerGroupView().setBackgroundColor(evaluate);
        }
    }
//liuqipeng begin
    public void onAnimationUpdate(float positionOffset) {
        ArgbEvaluator evaluator = new ArgbEvaluator();
        int evaluate = (Integer) evaluator.evaluate(positionOffset, 0XFF0093d1, 0XFF624BC6);
        if (mTimerGroupView != null) {
            mTimerGroupView.getTimerGroupView().setBackgroundColor(evaluate);
        }
    }
	public void invalidateListView(){
		for(int i=0;i<3;i++){
			mList.set(i,false);
		}
		myAdapter.notifyDataSetChanged();
	}
	public boolean isListViewItemSelected(){
	return mList.get(0)||mList.get(1)||mList.get(2);
	}
//liuqipeng
//liuqipeng begin
		class MyAdapter extends BaseAdapter {
		ArrayList<Boolean> mArrayList;
		public MyAdapter(ArrayList<Boolean> list) {
			super();
			mArrayList=list;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 3;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			Log.e("getView调用中", ""+position);
			LayoutInflater mInflater=getActivity().getLayoutInflater();
			View view=mInflater.inflate(R.layout.listview_item, null);
			TextView mTextView=(TextView)view.findViewById(R.id.timer_tv);
			ToggleButton mButton=(ToggleButton)view.findViewById(R.id.timer_togglebtn);
			switch (position) {
				case 0:
					mTextView.setText("煮鸡蛋");
					break;
				case 1:
					mTextView.setText("敷面膜");
					break;
				case 2:
					mTextView.setText("睡午觉");
					break;
				default:
					break;
			}
			mButton.setChecked(mArrayList.get(position));
			return view;
		}
	}
//liuqipeng

    public void startGuide() {
        if (mTimerGroupView != null) {
            mTimerGroupView.startGuide();
        }
    }

    public void setEnabled(boolean enabled) {
        mStartBtn.setEnabled(enabled);
        mStartTitle.setEnabled(enabled);
        mStopBtn.setEnabled(enabled);
        mCancelBtn.setEnabled(enabled);
    }

    @Override
    public void onAudioFocusChange(int arg0) {
            // TODO Auto-generated method stub

    }
/*
    public void setPlayRunSound(boolean playSound) {
        mPlaySound = playSound;
        if (mPlaySound) {
            mFirstPlay = true;
        }
    }
*/
    public void stop() {
        Log.v(TAG, "TimerFragment.stop()");
        // Stop audio playing
        if (mSoundPoolRun!= null && mSoundPoolStart!= null) {
            mSoundPoolRun.stop(mStreamID);
            mSoundPoolStart.stop(mSetQuickTimeStreamID);
            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(this);
            }
            mSoundPoolRun.release();
            mSoundPoolStart.release();
            mSoundPoolRun= null;
            mSoundPoolStart=null;
        }
    }
}
