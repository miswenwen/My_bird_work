package com.android.deskclock.stopwatch;
//liuqipeng begin
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
//liuqipeng
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import yunos.support.v4.app.Fragment;
import yunos.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import com.aliyun.ams.ta.TA;
import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.CircleTimerView;
import com.android.deskclock.DeskClock;
import com.android.deskclock.StopWatchRefreshableView;

import yunos.support.v4.app.Fragment;
import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.R.color;
import com.android.deskclock.timer.CountingTimerView;
import com.android.deskclock.ToastMaster;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Typeface;
import android.widget.Toast;

public class StopwatchFragment extends Fragment
        implements OnSharedPreferenceChangeListener, AudioManager.OnAudioFocusChangeListener, StopWatchRefreshableView.ListViewRefreshListener {
    private static final boolean DEBUG = true;

    private static final String TAG = "StopwatchFragment";
    int mState = Stopwatches.STOPWATCH_RESET;

    // Stopwatch views that are accessed by the activity
    private View mLeftButton;
//liuqipeng begin
    //private ImageView mLeftText;
	private TextView mLeftText;
//liuqipeng 
    private View mCenterButton;
//liuqipeng begin
    //private ImageView mCenterText;
	private TextView mCenterText;
//liuqipeng 
    private View mRightButton;
//liuqipeng begin
    //private ImageView mRightText;
	private TextView mRightText;
//liuqipeng 
    //private TextView mCenterButton;
    private CircleTimerView mTime;
    private CountingTimerView mTimeText;
    private ListView mLapsList;
    private StopWatchRefreshableView mRefreshableView;
    private View mDragView;
    private View mSeparator;
    private View mStopwatchMainView;
    private View mStopwatchListView;
    private View mTimerText;
    private View mTimerSet;
    //private ImageButton mShareButton;
    //private ListPopupWindow mSharePopup;
    private WakeLock mWakeLock;
    //private CircleButtonsLayout mCircleLayout;

    // Animation constants and objects
    private LayoutTransition mLayoutTransition;
    private LayoutTransition mCircleLayoutTransition;
    private boolean mSpacersUsed;
    private static StopwatchFragment mInstance = null;


    // Used for calculating the time from the start taking into account the pause times
    long mStartTime = 0;
    long mAccumulatedTime = 0;
    private String mStartText;
    private String mStopText;
    private String mLapText;
    private String mResumeText;
    private String mResetText;
    private Activity mActivity;

    private ColorStateList mColorStateList;
    private ColorStateList mRedColorStateList;
    private SoundPool mSoundPool;
    private int mStreamID;
    private int mLapStreamID;
    private long mLastTotalTime = 0;
    private boolean mPlaySound = false;
    private AudioManager mAudioManager;
    private boolean mFirstPlay = true;
    private boolean mFirstIn = true;
    private int mPlayStreamID = 0;
    private boolean mFromPageTransition;
    // Lap information
    class Lap {

        Lap (long time, long total) {
            mLapTime = time;
            mTotalTime = total;
        }
        public long mLapTime;
        public long mTotalTime;

        public void updateView() {
            View lapInfo = mLapsList.findViewWithTag(this);
            if (lapInfo != null) {
                mLapsAdapter.setTimeText(lapInfo, this);
            }
        }
    }

    // Adapter for the ListView that shows the lap times.
    class LapsListAdapter extends BaseAdapter {

        ArrayList<Lap> mLaps = new ArrayList<Lap>();
        private final LayoutInflater mInflater;
        // private final int mBackgroundColor;
        private final String[] mFormats;
        private final String[] mLapFormatSet;
        // Size of this array must match the size of formats
        private final long[] mThresholds = {
                10 * DateUtils.MINUTE_IN_MILLIS, // < 10 minutes
                DateUtils.HOUR_IN_MILLIS, // < 1 hour
                10 * DateUtils.HOUR_IN_MILLIS, // < 10 hours
                100 * DateUtils.HOUR_IN_MILLIS, // < 100 hours
                1000 * DateUtils.HOUR_IN_MILLIS // < 1000 hours
        };
        private int mLapIndex = 0;
        private int mTotalIndex = 0;
        private String mLapFormat;
        private Typeface dinpro;

        public LapsListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            dinpro = Typeface.createFromAsset(context.getAssets(), "fonts/DINPro/DINPro-Light.otf");
            // mBackgroundColor = getResources().getColor(R.color.white);
            mFormats = context.getResources().getStringArray(R.array.stopwatch_format_set);
            mLapFormatSet = context.getResources().getStringArray(R.array.sw_lap_number_set);
            updateLapFormat();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mLaps.size() == 0 || position >= mLaps.size()) {
                return null;
            }
            Lap lap = getItem(position);
            View lapInfo;
            if (convertView != null) {
                lapInfo = convertView;
            } else {
                lapInfo = mInflater.inflate(R.layout.lap_view, parent, false);
                // lapInfo.setBackgroundColor(mBackgroundColor);
            }
            lapInfo.setTag(lap);
            TextView count = (TextView)lapInfo.findViewById(R.id.lap_number);
            count.setTypeface(dinpro);
            count.setText(String.format(mLapFormat, mLaps.size() - position));
            setTimeText(lapInfo, lap);

            return lapInfo;
        }

        private int countHighLightShowIndex (SpannableString target) {
            for(int i = 0 ; i < target.length(); i++) {
                //Log.e("the element of the target at index : " + i + " is :" + target.charAt(i));
                if( target.charAt(i) != '0' && target.charAt(i) != ' ' && target.charAt(i) != '.') {
                    return i;
                }
            }
            return 0;
        }

        protected void setTimeText(View lapInfo, Lap lap) {
            TextView lapTime = (TextView)lapInfo.findViewById(R.id.lap_time);
            TextView totalTime = (TextView)lapInfo.findViewById(R.id.lap_total);
            lapTime.setTypeface(dinpro);
            totalTime.setTypeface(dinpro);
            // TODO penglei :set the highLight TextColor.
            SpannableString spanableLapTime = new SpannableString(Stopwatches.formatTimeText(lap.mLapTime, mFormats[mLapIndex]));
//liuqipeng begin 
            //spanableLapTime.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.lap_view_text_color)), 0, spanableLapTime.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanableLapTime.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.lap_time_text_color)), 0, spanableLapTime.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//liuqipeng
            SpannableString spanableTotalTime = new SpannableString(Stopwatches.formatTimeText(lap.mTotalTime, mFormats[mTotalIndex]));
            spanableTotalTime.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.lap_view_text_color)), 0, spanableTotalTime.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            lapTime.setText(spanableLapTime);
            totalTime.setText(spanableTotalTime);
        }

        @Override
        public int getCount() {
            return mLaps.size();
        }

        @Override
        public Lap getItem(int position) {
            if (mLaps.size() == 0 || position >= mLaps.size()) {
                return null;
            }
            return mLaps.get(position);
        }

        private void updateLapFormat() {
            // Note Stopwatches.MAX_LAPS < 100
            mLapFormat = mLapFormatSet[mLaps.size() < 10 ? 0 : 1];
        }

        private void resetTimeFormats() {
            mLapIndex = mTotalIndex = 0;
        }

        /**
         * A lap is printed into two columns: the total time and the lap time. To make this print
         * as pretty as possible, multiple formats were created which minimize the width of the
         * print. As the total or lap time exceed the limit of that format, this code updates
         * the format used for the total and/or lap times.
         *
         * @param lap to measure
         * @return true if this lap exceeded either threshold and a format was updated.
         */
        public boolean updateTimeFormats(Lap lap) {
            boolean formatChanged = false;
            while (mLapIndex + 1 < mThresholds.length && lap.mLapTime >= mThresholds[mLapIndex]) {
                mLapIndex++;
                formatChanged = true;
            }
            while (mTotalIndex + 1 < mThresholds.length && 
                lap.mTotalTime >= mThresholds[mTotalIndex]) {
                mTotalIndex++;
                formatChanged = true;
            }
            Log.v("mLapIndex = " + mLapIndex + " mTotalIndex = " + mTotalIndex);
            return formatChanged;
        }

        public void addLap(Lap l) {
            mLaps.add(0, l);
            // for efficiency caller also calls notifyDataSetChanged()
        }

        public void clearLaps() {
            mLaps.clear();
            updateLapFormat();
            resetTimeFormats();
            notifyDataSetChanged();
        }

        // Helper function used to get the lap data to be stored in the activity's bundle
        public long [] getLapTimes() {
            int size = mLaps.size();
            if (size == 0) {
                return null;
            }
            long [] laps = new long[size];
            for (int i = 0; i < size; i ++) {
                laps[i] = mLaps.get(i).mTotalTime;
            }
            return laps;
        }

        // Helper function to restore adapter's data from the activity's bundle
        public void setLapTimes(long [] laps) {
            if (laps == null || laps.length == 0) {
                return;
            }

            int size = laps.length;
            mLaps.clear();
            for (long lap : laps) {
                mLaps.add(new Lap(lap, 0));
            }
            long totalTime = 0;
            for (int i = size -1; i >= 0; i --) {
                totalTime += laps[i];
                mLaps.get(i).mTotalTime = totalTime;
                updateTimeFormats(mLaps.get(i));
            }
            updateLapFormat();
            showLaps();
            notifyDataSetChanged();
        }
    }

    LapsListAdapter mLapsAdapter;

    public StopwatchFragment() {
        mInstance = this;
    }

    private void setBtnVisibity() {
        Resources r = getResources();
        switch (mState) {
            case 0:
                mCenterButton.setVisibility(View.VISIBLE);
                mCenterText.setVisibility(View.VISIBLE);
//liuqipeng begin 注释掉
                //mCenterText.setBackgroundResource(R.drawable.clock_star);
                mLeftButton.setVisibility(View.GONE);
                //mLeftButton.setBackgroundResource(R.drawable.clock_stop_bg);
                mLeftText.setVisibility(View.GONE);
                //mLeftText.setBackgroundResource(R.drawable.clock_stop);
                mRightButton.setVisibility(View.GONE);
                //mRightButton.setBackgroundResource(R.drawable.clock_meter_bg);
                mRightText.setVisibility(View.GONE);
                //mRightText.setBackgroundResource(R.drawable.clock_meter);
//liuqipeng begin
                break;
            case 1:
                mCenterButton.setVisibility(View.GONE);
                mCenterText.setVisibility(View.GONE);
//liuqipeng begin 注释掉
                //mCenterText.setBackgroundResource(R.drawable.clock_star);
                mLeftButton.setVisibility(View.VISIBLE);
                //mLeftButton.setBackgroundResource(R.drawable.clock_stop_bg);
                mLeftText.setVisibility(View.VISIBLE);
				mLeftText.setText(getString(R.string.stopclock_stop));//新增
                //mLeftText.setBackgroundResource(R.drawable.clock_stop);
                mRightButton.setVisibility(View.VISIBLE);
                //mRightButton.setBackgroundResource(R.drawable.clock_meter_bg);
                mRightText.setVisibility(View.VISIBLE);
				mRightText.setText(getString(R.string.stopclock_count));//新增
                //mRightText.setBackgroundResource(R.drawable.clock_meter);
//liuqipeng 
                break;
            case 2:
//liuqipeng begin 注释掉
                mCenterButton.setVisibility(View.GONE);
                mCenterText.setVisibility(View.GONE);
                mLeftButton.setVisibility(View.VISIBLE);
                //mLeftButton.setBackgroundResource(R.drawable.clock_star_bg);
                mLeftText.setVisibility(View.VISIBLE);
				mLeftText.setText(getString(R.string.stopclock_continue));//新增
                //mLeftText.setBackgroundResource(R.drawable.clock_star);
                mRightButton.setVisibility(View.VISIBLE);
                //mRightButton.setBackgroundResource(R.drawable.clock_cancel_bg);
                mRightText.setVisibility(View.VISIBLE);
				mRightText.setText(getString(R.string.stopclock_reset));//新增
                //mRightText.setBackgroundResource(R.drawable.clock_cancel);
//liuqipeng
                break;
            default:
                Log.wtf("Illegal state " + mState
                        + " while pressing the right stopwatch button");
                break;
        }
    }

    private void rightButtonAction() {
        long time = Utils.getTimeNow();
        Context context = getActivity().getApplicationContext();
        Intent intent = new Intent(context, StopwatchService.class);
        intent.putExtra(Stopwatches.MESSAGE_TIME, time);
        intent.putExtra(Stopwatches.SHOW_NOTIF, false);
        switch (mState) {
            case Stopwatches.STOPWATCH_RUNNING:
                // do stop
                long curTime = Utils.getTimeNow();
                mAccumulatedTime += (curTime - mStartTime);
                doStop();
                intent.setAction(Stopwatches.STOP_STOPWATCH);
                context.startService(intent);
                releaseWakeLock();
                break;
            case Stopwatches.STOPWATCH_RESET:
            case Stopwatches.STOPWATCH_STOPPED:
                // do start
                doStart(time);
                intent.setAction(Stopwatches.START_STOPWATCH);
                context.startService(intent);
                acquireWakeLock();
                break;
            default:
                Log.wtf("Illegal state " + mState
                        + " while pressing the right stopwatch button");
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d("StopwatchFragment - onCreateView");
        ViewGroup v = (ViewGroup)inflater.inflate(R.layout.stopwatch_fragment, container, false);
        mActivity = this.getActivity();
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mStreamID = mSoundPool.load(mActivity, R.raw.stopwatch_run, 1);
        mLapStreamID = mSoundPool.load(mActivity, R.raw.stopwatch_start, 1);
        mAudioManager = (AudioManager)mActivity.getSystemService(Context.AUDIO_SERVICE);
        mStartText = getString(R.string.timer_start);
        mStopText = getString(R.string.stopwatch_stop_button_text);
        mLapText = getString(R.string.stopwatch_lap_button_text);
        mResumeText = getString(R.string.stopwatch_resume_button_text);
        mResetText = getString(R.string.stopwatch_reset_button_text);

        mColorStateList = getResources().getColorStateList(R.color.textview_color);
        mRedColorStateList = getResources().getColorStateList(R.color.textview_red_color);

        mLeftButton = v.findViewById(R.id.stopwatch_left_button);
        mLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rightButtonAction();
                setBtnVisibity();
                if (mState == 2) {
                    TA.getInstance().getDefaultTracker().commitEvent("Page_StopwatchFragment",
                            2101, "button_stop", null, null, null);
                    if (!mRightButton.isEnabled()) {
                        mRightButton.setEnabled(true);
                        mRightButton.setAlpha(255);
                    }
                } else if (mState == 1) {
                    TA.getInstance().getDefaultTracker().commitEvent("Page_StopwatchFragment",
                            2101, "button_resume", null, null, null);
                    if (reachedMaxLaps()  && mRightButton.isEnabled()) {
                        //mRightButton.setEnabled(false);
                        //mRightButton.setAlpha(125);
                    }
                }
            }
        });
//liuqipeng begin
        mLeftText = (TextView)v.findViewById(R.id.stopwatch_left_button_text);
//liuqipeng
        mCenterButton = v.findViewById(R.id.startwatch_btn);
        mCenterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                rightButtonAction();
                setBtnVisibity();
                TA.getInstance().getDefaultTracker().commitEvent("Page_StopwatchFragment",
                        2101, "button_start", null, null, null);

                int buttonMargin = (int) (getResources().getDimension(R.dimen.button_left_margin));
                ObjectAnimator leftTransAnimator = ObjectAnimator.ofFloat(mLeftButton, "translationX", buttonMargin, 0);
                leftTransAnimator.setDuration(350);
                leftTransAnimator.start();
                ObjectAnimator rightTransAnimator = ObjectAnimator.ofFloat(mRightButton, "translationX", -buttonMargin, 0);
                rightTransAnimator.setDuration(350);
                rightTransAnimator.start();
            }
        });
//liuqipeng begin
        //mCenterText = (ImageView)v.findViewById(R.id.startwatch_text);
		mCenterText = (TextView)v.findViewById(R.id.startwatch_text);
        mRightButton = v.findViewById(R.id.reset_btn);
//liuqipeng
        mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                long time = Utils.getTimeNow();
                Context context = getActivity().getApplicationContext();
                Intent intent = new Intent(context, StopwatchService.class);
                intent.putExtra(Stopwatches.MESSAGE_TIME, time);
                intent.putExtra(Stopwatches.SHOW_NOTIF, false);
                switch (mState) {
                    case Stopwatches.STOPWATCH_RUNNING:
                        // Save lap time
                        if (mSoundPool != null) {
                            mSoundPool.play(mLapStreamID, 1, 1, 0, 0, 1);
                        }
                        if(!reachedMaxLaps()){
                            addLapTime(time);
                            doLap();
                            intent.setAction(Stopwatches.LAP_STOPWATCH);
                            context.startService(intent);
                        } else {
                           // mRightButton.setEnabled(false);
                           // mRightButton.setAlpha(125);
                            String maxtime = context.getResources().getString(R.string.max_stopwatch_time);
                            Toast toast = Toast.makeText(context, maxtime, Toast.LENGTH_LONG);
                            ToastMaster.setToast(toast);
                            toast.show();
                        }
                        break;
                    case Stopwatches.STOPWATCH_STOPPED:
                        // do reset
                        doReset();
                        intent.setAction(Stopwatches.RESET_STOPWATCH);
                        context.startService(intent);
                        releaseWakeLock();
                        break;
                    default:
                        // Happens in monkey tests
                        Log.i("Illegal state " + mState
                                + " while pressing the left stopwatch button");
                        break;
                }
                setBtnVisibity();
                if (mState == 1) {
                    //this will cause Application operating lag
                    TA.getInstance().getDefaultTracker().commitEvent("Page_StopwatchFragment",
                            2101, "button_lap", null, null, null);
                } else if (mState == 0) {
                    TA.getInstance().getDefaultTracker().commitEvent("Page_StopwatchFragment",
                            2101, "button_reset", null, null, null);
                }
            }
        });
//liuqipeng begin
       // mRightText = (ImageView)v.findViewById(R.id.reset_btn_text);
		mRightText = (TextView)v.findViewById(R.id.reset_btn_text);
//liuqipeng

        // FIX ME : use CircleTimerView's function but not show it.
        mTime = (CircleTimerView)v.findViewById(R.id.stopwatch_time);
        //mTime.setVisibility(View.GONE);
        mTimeText = (CountingTimerView)v.findViewById(R.id.stopwatch_time_text);
        mLapsList = (ListView)v.findViewById(R.id.laps_list);
        mLapsList.setOverScrollMode(2);

        mDragView = v.findViewById(R.id.drag_view);
        mSeparator = v.findViewById(R.id.separator);
        mStopwatchMainView = v.findViewById(R.id.stopwatch_mainview);
        mStopwatchListView = v.findViewById(R.id.stopwatch_listview);
        mTimerText = v.findViewById(R.id.timer_text_top);
        mTimerSet = v.findViewById(R.id.timer_set);
        mRefreshableView = (StopWatchRefreshableView) v.findViewById(R.id.pull_to_refresh_head);
        mRefreshableView.setListViewRefreshListener(this);
        View footerView = inflater.inflate(R.layout.blank_footer_view, mLapsList, false);
        mLapsList.addFooterView(footerView,null,false);
        mLapsAdapter = new LapsListAdapter(getActivity());
        mLapsList.setAdapter(mLapsAdapter);

        // Timer text serves as a virtual start/stop button.
        mTimeText.registerVirtualButtonAction(new Runnable() {
            @Override
            public void run() {
                //rightButtonAction();
            }
        });
        // TODO penglei: add button for the stop;
        //mTimeText.registerStopTextView(mCenterButton);
        mTimeText.setVirtualButtonEnabled(true);

/*        mCircleLayout = (CircleButtonsLayout)v.findViewById(R.id.stopwatch_circle);
        mCircleLayout.setCircleTimerViewIds(R.id.stopwatch_time, R.id.stopwatch_left_button,
                R.id.stopwatch_share_button, R.id.stopwatch_stop,
                R.dimen.plusone_reset_button_padding, R.dimen.share_button_padding,
                0, 0); *//** No label for a stopwatch**/

        // Animation setup
        mLayoutTransition = new LayoutTransition();
        mCircleLayoutTransition = new LayoutTransition();

        // The CircleButtonsLayout only needs to undertake location changes
        mCircleLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        mCircleLayoutTransition.disableTransitionType(LayoutTransition.APPEARING);
        mCircleLayoutTransition.disableTransitionType(LayoutTransition.DISAPPEARING);
        mCircleLayoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        mCircleLayoutTransition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        mCircleLayoutTransition.setAnimateParentHierarchy(false);

        // These spacers assist in keeping the size of CircleButtonsLayout constant
        mSpacersUsed = true;
        // Listener to invoke extra animation within the laps-list
        mLayoutTransition.addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition transition, ViewGroup container,
                                        View view, int transitionType) {
                if (view == mLapsList) {
                    if (transitionType == LayoutTransition.DISAPPEARING) {
                        if (DEBUG) Log.v("StopwatchFragment.start laps-list disappearing");
                        boolean shiftX = view.getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_LANDSCAPE;
                        int first = mLapsList.getFirstVisiblePosition();
                        int last = mLapsList.getLastVisiblePosition();
                        // Ensure index range will not cause a divide by zero
                        if (last < first) {
                            last = first;
                        }
                        long duration = transition.getDuration(LayoutTransition.DISAPPEARING);
                        long offset = duration / (last - first + 1) / 5;
                        for (int visibleIndex = first; visibleIndex <= last; visibleIndex++) {
                            View lapView = mLapsList.getChildAt(visibleIndex - first);
                            if (lapView != null) {
                                float toXValue = shiftX ? 1.0f * (visibleIndex - first + 1) : 0;
                                float toYValue = shiftX ? 0 : 4.0f * (visibleIndex - first + 1);
                                        TranslateAnimation animation = new TranslateAnimation(
                                        Animation.RELATIVE_TO_SELF, 0,
                                        Animation.RELATIVE_TO_SELF, toXValue,
                                        Animation.RELATIVE_TO_SELF, 0,
                                        Animation.RELATIVE_TO_SELF, toYValue);
                                animation.setStartOffset((last - visibleIndex) * offset);
                                animation.setDuration(duration);
                                lapView.startAnimation(animation);
                            }
                        }
                    }
                }
            }

            @Override
            public void endTransition(LayoutTransition transition, ViewGroup container,
                                      View view, int transitionType) {
                if (transitionType == LayoutTransition.DISAPPEARING) {
                    if (DEBUG) Log.v("StopwatchFragment.end laps-list disappearing");
                    int last = mLapsList.getLastVisiblePosition();
                    for (int visibleIndex = mLapsList.getFirstVisiblePosition();
                         visibleIndex <= last; visibleIndex++) {
                        View lapView = mLapsList.getChildAt(visibleIndex);
                        if (lapView != null) {
                            Animation animation = lapView.getAnimation();
                            if (animation != null) {
                                animation.cancel();
                            }
                        }
                    }
                }
            }
        });
        mFromPageTransition = true;
        Log.d("StopwatchFragment - onCreateView end");
//liuqipeng begin
			mCenterButton.setOnTouchListener(new OnTouchListener() {
			
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					mCenterText.setAlpha(0.5f);
						break;
					case MotionEvent.ACTION_MOVE:
				
						break;
					case MotionEvent.ACTION_UP:
					mCenterText.setAlpha(1.0f);
						break;					
					default:
					mCenterText.setAlpha(1.0f);
						break;
					}
					return false;
				}
			});
			mLeftButton.setOnTouchListener(new OnTouchListener() {
			
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					mLeftText.setAlpha(0.5f);
						break;
					case MotionEvent.ACTION_MOVE:
				
						break;
					case MotionEvent.ACTION_UP:
					mLeftText.setAlpha(1.0f);
						break;					
					default:
					mLeftText.setAlpha(1.0f);
						break;
					}
					return false;
				}
			});
			mRightButton.setOnTouchListener(new OnTouchListener() {
			
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					mRightText.setAlpha(0.5f);
						break;
					case MotionEvent.ACTION_MOVE:
				
						break;
					case MotionEvent.ACTION_UP:
					mRightText.setAlpha(1.0f);
						break;					
					default:
					mRightText.setAlpha(1.0f);
						break;
					}
					return false;
				}
			});
//liuqipeng
        return v;
    }

    private void startStopwatchSound(SoundPool player) {
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(
                        this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    public void stop() {
        Log.v("StopwatchFragment.stop()");
        // Stop audio playing
        if (mSoundPool != null) {
            mSoundPool.stop(mStreamID);
            mSoundPool.stop(mLapStreamID);
            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(this);
            }
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    /**
     * Make the final display setup.
     *
     * If the fragment is starting with an existing list of laps, shows the laps list and if the
     * spacers around the clock exist, hide them. If there are not laps at the start, hide the laps
     * list and show the clock spacers if they exist.
     */
    @Override
    public void onStart() {
        super.onStart();
        mLastTotalTime = 0;
        boolean lapsVisible = mLapsAdapter.getCount() > 0;
//liuqipeng begin
        //mLapsList.setVisibility(lapsVisible ? View.VISIBLE : View.GONE);
		if(lapsVisible)
		mLapsList.setVisibility(View.VISIBLE);
//liuqipeng
        ((ViewGroup)getView()).setLayoutTransition(mLayoutTransition);
        //mCircleLayout.setLayoutTransition(mCircleLayoutTransition);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        readFromSharedPref(prefs);
        mTime.readFromSharedPref(prefs, "sw");
        mTime.postInvalidate();

        setButtons(mState);
        // init btn visibility
        setBtnVisibity();
        mTime.setTime(mAccumulatedTime);
        mTimeText.setTime(mAccumulatedTime, true, true);
        if (mState == Stopwatches.STOPWATCH_RUNNING) {
            acquireWakeLock();
            startUpdateThread();
        } else if (mState == Stopwatches.STOPWATCH_STOPPED && mAccumulatedTime != 0) {
            //mTimeText.blinkTimeStr(false);
        }
        showLaps();
    }

    @Override
    public void onResume() {
        /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        readFromSharedPref(prefs);
        mTime.readFromSharedPref(prefs, "sw");
        mTime.postInvalidate();

        setButtons(mState);
        // init btn visibility
        setBtnVisibity();
        mTime.setTime(mAccumulatedTime);
        mTimeText.setTime(mAccumulatedTime, true, true);
        if (mState == Stopwatches.STOPWATCH_RUNNING) {
            acquireWakeLock();
            startUpdateThread();
        } else if (mState == Stopwatches.STOPWATCH_STOPPED && mAccumulatedTime != 0) {
            //mTimeText.blinkTimeStr(false);
        }
        showLaps();*/
        //((DeskClock)getActivity()).registerPageChangedListener(this);
        // View was hidden in onPause, make sure it is visible now.
        /*View v = getView();
        if (v != null) {
            v.setVisibility(View.VISIBLE);
        }*/
        if(mFromPageTransition){
            mLapsList.setSelection(0);
            mFromPageTransition = false;
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        // This is called because the lock screen was activated, the window stay
        // active under it and when we unlock the screen, we see the old time for
        // a fraction of a second.

        /*View v = getView();
        if (v != null) {
            v.setVisibility(View.INVISIBLE);
        }*/

        /*if (mState == Stopwatches.STOPWATCH_RUNNING) {
            stopUpdateThread();
        }
        // The stopwatch must keep running even if the user closes the app so save stopwatch state
        // in shared prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        writeToSharedPref(prefs);
        mTime.writeToSharedPref(prefs, "sw");
        //mTimeText.blinkTimeStr(false);
        if (mSharePopup != null) {
            mSharePopup.dismiss();
            mSharePopup = null;
        }
        //((DeskClock)getActivity()).unregisterPageChangedListener(this);
        releaseWakeLock();*/
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mState == Stopwatches.STOPWATCH_RUNNING) {
            stopUpdateThread();
        }
        // The stopwatch must keep running even if the user closes the app so save stopwatch state
        // in shared prefs
        Log.d("onStop() - Utils.sShutDown = "+Utils.sShutDown);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        if (!Utils.sShutDown) {
            writeToSharedPref(prefs);
            mTime.writeToSharedPref(prefs, "sw");
        }
        Utils.sShutDown = false;
        //mTimeText.blinkTimeStr(false);
        /*if (mSharePopup != null) {
            mSharePopup.dismiss();
            mSharePopup = null;
        }*/
        //((DeskClock)getActivity()).unregisterPageChangedListener(this);
        releaseWakeLock();
        super.onStop();
    }

    /*@Override
    public void onPageChanged(int page) {
        if (page == DeskClock.STOPWATCH_TAB_INDEX && mState == Stopwatches.STOPWATCH_RUNNING) {
            acquireWakeLock();
        } else {
            releaseWakeLock();
        }
    }*/

    private void doStop() {
        if (DEBUG) Log.v("StopwatchFragment.doStop");
        stopUpdateThread();
        mTime.pauseIntervalAnimation();
        mTimeText.setTime(mAccumulatedTime, true, true);
        mTimeText.blinkTimeStr(false);
        //updateCurrentLap(mAccumulatedTime);
        setButtons(Stopwatches.STOPWATCH_STOPPED);
        mState = Stopwatches.STOPWATCH_STOPPED;
    }

    private void doStart(long time) {
        if (DEBUG) Log.v("StopwatchFragment.doStart");
        mStartTime = time;
        startUpdateThread();
        mTimeText.blinkTimeStr(false);
        if (mTime.isAnimating()) {
            mTime.startIntervalAnimation();
        }
        setButtons(Stopwatches.STOPWATCH_RUNNING);
        mState = Stopwatches.STOPWATCH_RUNNING;
    }

    private void doLap() {
        if (DEBUG) Log.v("StopwatchFragment.doLap");
        showLaps();
        setButtons(Stopwatches.STOPWATCH_RUNNING);
    }

    private void doReset() {
        if (DEBUG) Log.v("StopwatchFragment.doReset");
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        Utils.clearSwSharedPref(prefs);
        mTime.clearSharedPref(prefs, "sw");
        mAccumulatedTime = 0;
        mLapsAdapter.clearLaps();
        showLaps();
        mTime.stopIntervalAnimation();
        mTime.reset();
        mTimeText.setTime(mAccumulatedTime, true, true);
        mTimeText.blinkTimeStr(false);
        setButtons(Stopwatches.STOPWATCH_RESET);
        mState = Stopwatches.STOPWATCH_RESET;
    }

    private void showShareButton(boolean show) {
/*        if (mShareButton != null) {
            mShareButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            mShareButton.setEnabled(show);
        }*/
    }

    private void showSharePopup() {
        Intent intent = getShareIntent();

        Activity parent = getActivity();
        PackageManager packageManager = parent.getPackageManager();

        // Get a list of sharable options.
        List<ResolveInfo> shareOptions = packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (shareOptions.size() == 0) {
            return;
        }
        ArrayList<CharSequence> shareOptionTitles = new ArrayList<CharSequence>();
        ArrayList<Drawable> shareOptionIcons = new ArrayList<Drawable>();
        ArrayList<CharSequence> shareOptionThreeTitles = new ArrayList<CharSequence>();
        ArrayList<Drawable> shareOptionThreeIcons = new ArrayList<Drawable>();
        ArrayList<String> shareOptionPackageNames = new ArrayList<String>();
        ArrayList<String> shareOptionClassNames = new ArrayList<String>();

        for (int option_i = 0; option_i < shareOptions.size(); option_i++) {
            ResolveInfo option = shareOptions.get(option_i);
            CharSequence label = option.loadLabel(packageManager);
            Drawable icon = option.loadIcon(packageManager);
            shareOptionTitles.add(label);
            shareOptionIcons.add(icon);
            if (shareOptions.size() > 4 && option_i < 3) {
                shareOptionThreeTitles.add(label);
                shareOptionThreeIcons.add(icon);
            }
            shareOptionPackageNames.add(option.activityInfo.packageName);
            shareOptionClassNames.add(option.activityInfo.name);
        }
        if (shareOptionTitles.size() > 4) {
            shareOptionThreeTitles.add(getResources().getString(R.string.see_all));
            shareOptionThreeIcons.add(getResources().getDrawable(android.R.color.transparent));
        }

/*        if (mSharePopup != null) {
            mSharePopup.dismiss();
            mSharePopup = null;
        }
        mSharePopup = new ListPopupWindow(parent);
        //mSharePopup.setAnchorView(mShareButton);
        mSharePopup.setModal(true);*/
        // This adapter to show the rest will be used to quickly repopulate if "See all..." is hit.
        /*
        ImageLabelAdapter showAllAdapter = new ImageLabelAdapter(parent,
                R.layout.popup_window_item, shareOptionTitles, shareOptionIcons,
                shareOptionPackageNames, shareOptionClassNames);
                */
/*        if (shareOptionTitles.size() > 4) {
            mSharePopup.setAdapter(new ImageLabelAdapter(parent, R.layout.popup_window_item,
                    shareOptionThreeTitles, shareOptionThreeIcons, shareOptionPackageNames,
                    shareOptionClassNames, showAllAdapter));
        } else {
            mSharePopup.setAdapter(showAllAdapter);
        }

        mSharePopup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CharSequence label = ((TextView) view.findViewById(R.id.title)).getText();
                if (label.equals(getResources().getString(R.string.see_all))) {
                    mSharePopup.setAdapter(
                            ((ImageLabelAdapter) parent.getAdapter()).getShowAllAdapter());
                    mSharePopup.show();
                    return;
                }

                Intent intent = getShareIntent();
                ImageLabelAdapter adapter = (ImageLabelAdapter) parent.getAdapter();
                String packageName = adapter.getPackageName(position);
                String className = adapter.getClassName(position);
                intent.setClassName(packageName, className);
                startActivity(intent);
            }
        });
        mSharePopup.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                mSharePopup = null;
            }
        });
        mSharePopup.setWidth((int) getResources().getDimension(R.dimen.popup_window_width));
        mSharePopup.show();*/
    }

    private Intent getShareIntent() {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intent.EXTRA_SUBJECT,
                Stopwatches.getShareTitle(getActivity().getApplicationContext()));
        intent.putExtra(Intent.EXTRA_TEXT, Stopwatches.buildShareResults(
                getActivity().getApplicationContext(), mTimeText.getTimeString(),
                getLapShareTimes(mLapsAdapter.getLapTimes())));
        return intent;
    }

    /** Turn laps as they would be saved in prefs into format for sharing. **/
    private long[] getLapShareTimes(long[] input) {
        if (input == null) {
            return null;
        }

        int numLaps = input.length;
        long[] output = new long[numLaps];
        long prevLapElapsedTime = 0;
        for (int lap_i = numLaps - 1; lap_i >= 0; lap_i--) {
            long lap = input[lap_i];
            Log.v("lap "+lap_i+": "+lap);
            output[lap_i] = lap - prevLapElapsedTime;
            prevLapElapsedTime = lap;
        }
        return output;
    }

    /***
     * Update the buttons on the stopwatch according to the watch's state
     */
    private void setButtons(int state) {
//        switch (state) {
//            case Stopwatches.STOPWATCH_RESET:
//                setButton(mLeftButton, R.string.sw_lap_button, R.drawable.ic_lap, false,
//                        View.VISIBLE);
//                setStartStopText(mCircleLayout, mCenterButton, R.string.sw_start_button);
//                showShareButton(false);
//                break;
//            case Stopwatches.STOPWATCH_RUNNING:
//                setButton(mLeftButton, R.string.sw_lap_button, R.drawable.ic_lap,
//                        !reachedMaxLaps(), View.VISIBLE);
//                setStartStopText(mCircleLayout, mCenterButton, R.string.sw_stop_button);
//                showShareButton(false);
//                break;
//            case Stopwatches.STOPWATCH_STOPPED:
//                setButton(mLeftButton, R.string.sw_reset_button, R.drawable.ic_reset, true,
//                        View.VISIBLE);
//                setStartStopText(mCircleLayout, mCenterButton, R.string.sw_start_button);
//                showShareButton(true);
//                break;
//            default:
//                break;
//        }
    }
    private boolean reachedMaxLaps() {
        return mLapsAdapter.getCount() >= Stopwatches.MAX_LAPS;
    }

    /***
     * Set a single button with the string and states provided.
     * @param b - Button view to update
     * @param text - Text in button
     * @param enabled - enable/disables the button
     * @param visibility - Show/hide the button
     */
    private void setButton(
            ImageButton b, int text, int drawableId, boolean enabled, int visibility) {
        b.setContentDescription(getActivity().getResources().getString(text));
        b.setImageResource(drawableId);
        b.setVisibility(visibility);
        b.setEnabled(enabled);
    }

    /**
     * Update the Start/Stop text. The button is within a view group with a transition that
     * is needed to animate the button moving. The transition also animates the the text changing,
     * but that animation does not provide a good look and feel. Temporarily disable the view group
     * transition while the text is changing and restore it afterwards.
     *
     * @param parent   - View Group holding the start/stop button
     * @param textView - The start/stop button
     * @param text     - Start or Stop id
     */
    private void setStartStopText(final ViewGroup parent, TextView textView, int text) {
        final LayoutTransition layoutTransition = parent.getLayoutTransition();
        // Tap into the parent layout->draw flow just before the draw
        ViewTreeObserver viewTreeObserver = parent.getViewTreeObserver();
        if (viewTreeObserver != null) {
            viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                /**
                 * Re-establish the transition handler
                 * Remove this listener
                 *
                 * @return true so that onDraw() is called
                 */
                @Override
                public boolean onPreDraw() {
                    parent.setLayoutTransition(layoutTransition);
                    ViewTreeObserver viewTreeObserver = parent.getViewTreeObserver();
                    if (viewTreeObserver != null) {
                        viewTreeObserver.removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
        }
        // Remove the transition while the text is updated
        parent.setLayoutTransition(null);

        String textStr = getActivity().getResources().getString(text);
        textView.setText(textStr);
        textView.setContentDescription(textStr);
    }

    /***
     * Handle action when user presses the lap button
     * @param time - in hundredth of a second
     */
    private void addLapTime(long time) {
        // The total elapsed time
        final long curTime = time - mStartTime + mAccumulatedTime;
        int size = mLapsAdapter.getCount();
        if (size == 0) {
            // Create and add the first lap
            Lap firstLap = new Lap(curTime, curTime);
            mLapsAdapter.addLap(firstLap);
            // Create the first active lap
            //mLapsAdapter.addLap(new Lap(0, curTime));
            // Update the interval on the clock and check the lap and total time formatting
            mTime.setIntervalTime(curTime);
            mLapsAdapter.updateTimeFormats(firstLap);
        } else {
            // Finish active lap
            final long lapTime = curTime - mLapsAdapter.getItem(0).mTotalTime;
            //mLapsAdapter.getItem(0).mLapTime = lapTime;
            //mLapsAdapter.getItem(0).mTotalTime = curTime;
            // Create a new active lap
            //mLapsAdapter.addLap(new Lap(0, curTime));
            Lap lap = new Lap(lapTime, curTime);
            mLapsAdapter.addLap(lap);
            // Update marker on clock and check that formatting for the lap number
            mTime.setMarkerTime(lapTime);
            mLapsAdapter.updateTimeFormats(lap);
            mLapsAdapter.updateLapFormat();
        }
        // Repaint the laps list
        mLapsAdapter.notifyDataSetChanged();

        // Start lap animation starting from the second lap
        mTime.stopIntervalAnimation();
        if (!reachedMaxLaps()) {
            mTime.startIntervalAnimation();
        }
    }

    private void updateCurrentLap(long totalTime) {
        // There are either 0, 2 or more Laps in the list See {@link #addLapTime}
        if (mLapsAdapter.getCount() > 0) {
            Lap curLap = mLapsAdapter.getItem(0);
            curLap.mLapTime = totalTime - mLapsAdapter.getItem(1).mTotalTime;
            curLap.mTotalTime = totalTime;
            // If this lap has caused a change in the format for total and/or lap time, all of
            // the rows need a fresh print. The simplest way to refresh all of the rows is
            // calling notifyDataSetChanged.
            if (mLapsAdapter.updateTimeFormats(curLap)) {
                mLapsAdapter.notifyDataSetChanged();
            } else {
                curLap.updateView();
            }
        }
    }

    /**
     * Show or hide the laps-list
     */
    private void showLaps() {
        if (DEBUG) Log.v(String.format("StopwatchFragment.showLaps: count=%d",
                mLapsAdapter.getCount()));

        boolean lapsVisible = mLapsAdapter.getCount() > 0;

        // Layout change animations will start upon the first add/hide view. Temporarily disable
        // the layout transition animation for the spacers, make the changes, then re-enable
        // the animation for the add/hide laps-list
        if (mSpacersUsed) {
            int spacersVisibility = lapsVisible ? View.GONE : View.VISIBLE;
            ViewGroup rootView = (ViewGroup) getView();
            if (rootView != null) {
                rootView.setLayoutTransition(null);
                rootView.setLayoutTransition(mLayoutTransition);
            }
        }

        if (lapsVisible) {
            // There are laps - show the laps-list
            // No delay for the CircleButtonsLayout changes - start immediately so that the
            // circle has shifted before the laps-list starts appearing.
            mCircleLayoutTransition.setStartDelay(LayoutTransition.CHANGING, 0);

            mLapsList.setVisibility(View.VISIBLE);
            mDragView.setVisibility(View.VISIBLE);
            mSeparator.setVisibility(View.VISIBLE);
        } else {
            // There are no laps - hide the laps list

            // Delay the CircleButtonsLayout animation until after the laps-list disappears
            long startDelay = mLayoutTransition.getStartDelay(LayoutTransition.DISAPPEARING) +
                    mLayoutTransition.getDuration(LayoutTransition.DISAPPEARING);
            mCircleLayoutTransition.setStartDelay(LayoutTransition.CHANGING, startDelay);
//liuqipeng 注释掉
            //mLapsList.setVisibility(View.GONE);
            //mDragView.setVisibility(View.GONE);
//liuqipeng 
            mSeparator.setVisibility(View.GONE);
            mRefreshableView.showHeader(true);
        }
    }

    private void startUpdateThread() {
        if (mPlaySound) {
            mFirstPlay = true;
        }
        mTime.post(mTimeUpdateThread);
    }

    private void stopUpdateThread() {
        if (mPlaySound) {
            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(this);
            }
        }
        mTime.removeCallbacks(mTimeUpdateThread);
        mSoundPool.pause(mPlayStreamID);
    }

    Runnable mTimeUpdateThread = new Runnable() {
        @Override
        public void run() {
            long curTime = Utils.getTimeNow();
            long totalTime = mAccumulatedTime + (curTime - mStartTime);
            if (mTime != null) {
                mTime.setTime(totalTime);
                mTimeText.setTime(totalTime, true, true);
            }
            SharedPreferences prefs1 = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mPlaySound = prefs1.getBoolean("mStopwatchFragmentPlayRunSound", true);
            if(mFirstIn && mPlaySound){
                mFirstPlay = true;
                mFirstIn = false;
            }
            else if(!mPlaySound){
                mFirstIn = true;
            }

            if (mLastTotalTime != 0 && mLastTotalTime/1000 != totalTime/1000) {
                if (mSoundPool != null && mPlaySound) {
                    mPlayStreamID = mSoundPool.play(mStreamID, 1, 1, 0, 0, 1);
                    if (mFirstPlay) {
                        mFirstPlay = false;
                        startStopwatchSound(mSoundPool);
                    }
                } else {
                    if (mAudioManager != null) {
                        mAudioManager.abandonAudioFocus(StopwatchFragment.this);
                    }
                }
            }
            mLastTotalTime = totalTime;
            /*if (mLapsAdapter.getCount() > 0) {
                updateCurrentLap(totalTime);
            }*/
            mTime.postDelayed(mTimeUpdateThread, 10);
        }
    };

    private void writeToSharedPref(SharedPreferences prefs) {
        Log.d("mStartTime = "+mStartTime+", mAccumulatedTime = "+mAccumulatedTime+", mState = "+mState);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong (Stopwatches.PREF_START_TIME, mStartTime);
        editor.putLong (Stopwatches.PREF_ACCUM_TIME, mAccumulatedTime);
        editor.putInt (Stopwatches.PREF_STATE, mState);
        if (mLapsAdapter != null) {
            long [] laps = mLapsAdapter.getLapTimes();
            if (laps != null) {
                editor.putInt (Stopwatches.PREF_LAP_NUM, laps.length);
                for (int i = 0; i < laps.length; i++) {
                    String key = Stopwatches.PREF_LAP_TIME + Integer.toString(laps.length - i);
                    editor.putLong (key, laps[i]);
                }
            }
        }
        if (mState == Stopwatches.STOPWATCH_RUNNING) {
            editor.putLong(Stopwatches.NOTIF_CLOCK_BASE, mStartTime-mAccumulatedTime);
            editor.putLong(Stopwatches.NOTIF_CLOCK_ELAPSED, -1);
            editor.putBoolean(Stopwatches.NOTIF_CLOCK_RUNNING, true);
        } else if (mState == Stopwatches.STOPWATCH_STOPPED) {
            editor.putLong(Stopwatches.NOTIF_CLOCK_ELAPSED, mAccumulatedTime);
            editor.putLong(Stopwatches.NOTIF_CLOCK_BASE, -1);
            editor.putBoolean(Stopwatches.NOTIF_CLOCK_RUNNING, false);
        } else if (mState == Stopwatches.STOPWATCH_RESET) {
            editor.remove(Stopwatches.NOTIF_CLOCK_BASE);
            editor.remove(Stopwatches.NOTIF_CLOCK_RUNNING);
            editor.remove(Stopwatches.NOTIF_CLOCK_ELAPSED);
        }
        editor.putBoolean(Stopwatches.PREF_UPDATE_CIRCLE, false);
        editor.apply();
    }

    private void readFromSharedPref(SharedPreferences prefs) {
        mStartTime = prefs.getLong(Stopwatches.PREF_START_TIME, 0);
        mAccumulatedTime = prefs.getLong(Stopwatches.PREF_ACCUM_TIME, 0);
        mState = prefs.getInt(Stopwatches.PREF_STATE, Stopwatches.STOPWATCH_RESET);
        int numLaps = prefs.getInt(Stopwatches.PREF_LAP_NUM, Stopwatches.STOPWATCH_RESET);
        if (mLapsAdapter != null) {
            long[] oldLaps = mLapsAdapter.getLapTimes();
            if (oldLaps == null || oldLaps.length < numLaps) {
                long[] laps = new long[numLaps];
                long prevLapElapsedTime = 0;
                for (int lap_i = 0; lap_i < numLaps; lap_i++) {
                    String key = Stopwatches.PREF_LAP_TIME + Integer.toString(lap_i + 1);
                    long lap = prefs.getLong(key, 0);
                    laps[numLaps - lap_i - 1] = lap - prevLapElapsedTime;
                    prevLapElapsedTime = lap;
                }
                mLapsAdapter.setLapTimes(laps);
            }
        }
        if (prefs.getBoolean(Stopwatches.PREF_UPDATE_CIRCLE, true)) {
            if (mState == Stopwatches.STOPWATCH_STOPPED) {
                doStop();
            } else if (mState == Stopwatches.STOPWATCH_RUNNING) {
                doStart(mStartTime);
            } else if (mState == Stopwatches.STOPWATCH_RESET) {
                doReset();
            }
        }
    }

    public class ImageLabelAdapter extends ArrayAdapter<CharSequence> {
        private final ArrayList<CharSequence> mStrings;
        private final ArrayList<Drawable> mDrawables;
        private final ArrayList<String> mPackageNames;
        private final ArrayList<String> mClassNames;
        private ImageLabelAdapter mShowAllAdapter;

        public ImageLabelAdapter(Context context, int textViewResourceId,
                ArrayList<CharSequence> strings, ArrayList<Drawable> drawables,
                ArrayList<String> packageNames, ArrayList<String> classNames) {
            super(context, textViewResourceId, strings);
            mStrings = strings;
            mDrawables = drawables;
            mPackageNames = packageNames;
            mClassNames = classNames;
        }

        // Use this constructor if showing a "see all" option, to pass in the adapter
        // that will be needed to quickly show all the remaining options.
        public ImageLabelAdapter(Context context, int textViewResourceId,
                ArrayList<CharSequence> strings, ArrayList<Drawable> drawables,
                ArrayList<String> packageNames, ArrayList<String> classNames,
                ImageLabelAdapter showAllAdapter) {
            super(context, textViewResourceId, strings);
            mStrings = strings;
            mDrawables = drawables;
            mPackageNames = packageNames;
            mClassNames = classNames;
            mShowAllAdapter = showAllAdapter;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater li = getActivity().getLayoutInflater();
            View row = li.inflate(R.layout.popup_window_item, parent, false);
            ((TextView) row.findViewById(R.id.title)).setText(
                    mStrings.get(position));
            row.findViewById(R.id.icon).setBackground(mDrawables.get(position));
            return row;
        }

        public String getPackageName(int position) {
            return mPackageNames.get(position);
        }

        public String getClassName(int position) {
            return mClassNames.get(position);
        }

        public ImageLabelAdapter getShowAllAdapter() {
            return mShowAllAdapter;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if(getActivity()==null){
            return;
        }
        if (prefs.equals(PreferenceManager.getDefaultSharedPreferences(getActivity()))) {
            if (! (key.equals(Stopwatches.PREF_LAP_NUM) ||
                    key.startsWith(Stopwatches.PREF_LAP_TIME))) {
                readFromSharedPref(prefs);
                if (prefs.getBoolean(Stopwatches.PREF_UPDATE_CIRCLE, true)) {
                    mTime.readFromSharedPref(prefs, "sw");
                }
            }
        }
    }

    // Used to keeps screen on when stopwatch is running.

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            final PowerManager pm =
                    (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            mWakeLock.setReferenceCounted(false);
        }
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public static StopwatchFragment getInstance() {
        if (mInstance == null) {
            mInstance = new StopwatchFragment();
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
        mActivity = null;
        mInstance = null;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mInstance = this;
    }

    public void removeTimerSet() {
        if (mTimerSet != null && mTimerSet.getVisibility() == View.VISIBLE) {
            mTimerSet.setVisibility(View.INVISIBLE);
        }
        if (mTimerText != null && mTimerText.getVisibility() == View.VISIBLE) {
            mTimerText.setVisibility(View.INVISIBLE);
        }
    }

    public void showTimerSet() {
        if (mTimerSet != null && mTimerSet.getVisibility() != View.VISIBLE) {
            mTimerSet.setVisibility(View.VISIBLE);
        }
        if (mTimerText != null && mTimerText.getVisibility() != View.VISIBLE) {
            mTimerText.setVisibility(View.VISIBLE);
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
        int evaluate = (Integer) evaluator.evaluate(positionOffset, 0XFF624BC6, 0XFF00B1B1);
        if (mStopwatchMainView != null) {
            mStopwatchMainView.setBackgroundColor(evaluate);
        }
        if (mStopwatchListView != null) {
            mStopwatchListView.setBackgroundColor(evaluate);
        }

    }
//liuqipeng
    public void onAnimationUpdate(float positionOffset,boolean isToClock) {
        ArgbEvaluator evaluator = new ArgbEvaluator();
        int evaluate = isToClock ? (Integer) evaluator.evaluate(positionOffset, 0XFF009a96, 0XFF0093d1)
                : (Integer) evaluator.evaluate(positionOffset, 0XFF0093d1, 0XFF624BC6);
        if (mStopwatchMainView != null) {
            mStopwatchMainView.setBackgroundColor(evaluate);
        }
    }	
//liuqipeng

    public void onListViewRefreshListener() {
        mLapsList.setAdapter(mLapsAdapter);
        mLapsAdapter.notifyDataSetChanged();
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
    @Override
    public void onDestroyView() {
        stop();
        super.onDestroyView();
    }
}
