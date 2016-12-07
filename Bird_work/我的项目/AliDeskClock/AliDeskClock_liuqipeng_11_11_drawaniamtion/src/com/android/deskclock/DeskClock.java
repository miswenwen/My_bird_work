/*
 * Copyright (C) 2009 The Android Open Source Project
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
import com.android.deskclock.timer.CountingTimerView;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.util.Log;
//liuqipeng
import hwdroid.widget.ItemAdapter;
import hwdroid.widget.FooterBar.FooterBarButton;
import hwdroid.widget.FooterBar.FooterBarMenu;
import hwdroid.widget.FooterBar.FooterBarView;
import hwdroid.widget.FooterBar.FooterBarType.OnFooterItemClick;
import hwdroid.widget.indicator.HWTabPageSimpleIndicator;
import hwdroid.widget.item.Item;
import hwdroid.widget.itemview.ItemView;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.backup.BackupManager;
import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentManager;
import yunos.support.v4.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.DeletedContacts;
import yunos.support.v4.app.FragmentPagerAdapter;
import yunos.support.v4.view.ViewPager;
import yunos.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.stopwatch.Stopwatches;
import com.android.deskclock.timer.TimerFragment;
import com.android.deskclock.timer.TimerObj;
import com.android.deskclock.timer.Timers;
import com.android.deskclock.worldclock.CitiesActivity;
import com.android.deskclock.holiday.HolidayParser;

import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;

import java.nio.channels.AlreadyConnectedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.HashMap;
import yunos.support.v4.widget.EdgeEffectCompat;
import java.lang.reflect.Field;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.Exception;


/**
 * DeskClock clock view for desk docks.
 */
public class DeskClock extends BaseFragmentActivity implements LabelDialogFragment.TimerLabelDialogHandler,
            LabelDialogFragment.AlarmLabelDialogHandler ,ViewPager.OnPageChangeListener{
    private static final boolean DEBUG = true;

    private static final String LOG_TAG = "DeskClock";

    // Alarm action for midnight (so we can update the date display).
    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final String KEY_CLOCK_STATE = "clock_state";

    public static final String SELECT_TAB_INTENT_EXTRA = "deskclock.select.tab";

    private ActionBar mActionBar;
    private Tab mAlarmTab;
    private Tab mClockTab;
    private Tab mTimerTab;
    private Tab mStopwatchTab;
    private Menu mMenu;
    private Context mContext;

    public AlarmViewPager mViewPager;
    private SampleFragmentPagerAdapter mTabsAdapter;
    private FooterBarView mFooterBarButton;
    private int mButtonItemId = 0;
    //private boolean mCoolOpen = true;

    public static final int ALARM_TAB_INDEX = 0;
    public static final int CLOCK_TAB_INDEX = 1;
//liuqipeng begin
	//public static final int TIMER_TAB_INDEX = 2;
    //public static final int STOPWATCH_TAB_INDEX = 3;
    public static final int TIMER_TAB_INDEX = 3;
    public static final int STOPWATCH_TAB_INDEX = 2;
//liuqipeng
    // Tabs indices are switched for right-to-left since there is no
    // native support for RTL in the ViewPager.
    public static final int RTL_ALARM_TAB_INDEX = 3;
    public static final int RTL_CLOCK_TAB_INDEX = 2;
    public static final int RTL_TIMER_TAB_INDEX = 1;
    public static final int RTL_STOPWATCH_TAB_INDEX = 0;

    private int mSelectedTab;
    private String[] CONTENT ;

    private AlarmClockFragment mAlarmFragment;
    private ClockFragment mClockFragment;
    private TimerFragment mTimerFragment;
    private StopwatchFragment mStopwatchFragment;
    private MyAnalogClock mMyAnalogClock;
    private WorldMyAnalogClock mWorldMyAnalogClock;
    private ImageView mTimerView;
    private View mStopwatchView;
//liuqipeng begin
	private CircleTimerView mCircleTimerView;
	private CountingTimerView mCountingTimerView;
	//public static boolean nowDrawClockAnimation=true;
	public static boolean nowDrawWorldAnimation=true;
	private Handler updateAnalogAnim;
	private Handler updateWorldAnim;
//liuqipeng

    private AudioManager am;
    private AlarmObserver mAlarmObserver;

    private TextView mTabAlarm;
    private TextView mTabClock;
    private TextView mTabTimer;
    private TextView mTabStopwatch;

    private boolean mFirstStop = true;

    private EdgeEffectCompat mLeftEdge;
    private EdgeEffectCompat mRightEdge;
    public static HolidayParser parser;
    public static boolean mCalendarDataSupport = false;

    // Position that view pager has scrolled, get value from method
    // onPageScrolled(position, offset, offsetPixels). It is used
    // to select tab.
    private float mScrolledPosition;

    private static Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            //TODO penglei : keep this for futher use.
        }
    };

    /**
     * Observer for any changes to the alarms in the content provider.
     */
    private class AlarmObserver extends ContentObserver {

        public AlarmObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean changed) {
            BackupManager.dataChanged(mContext.getPackageName());
            Log.e(LOG_TAG, "the date change has been captured.");
        }

        @Override
        public void onChange(boolean changed, Uri uri) {
            onChange(changed);
        }
    }

    class SampleFragmentPagerAdapter extends FragmentPagerAdapter {
        public SampleFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    return AlarmClockFragment.getInstance();
                case 1:
                    return ClockFragment.getInstance();
//liuqipeng begin
                /*case 2:
                    return TimerFragment.getInstance();
                case 3:
                    return StopwatchFragment.getInstance();*/
                case 3:
                    return TimerFragment.getInstance();
                case 2:
                    return StopwatchFragment.getInstance();
//liuqipeng
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return CONTENT[position % CONTENT.length];
        }

        @Override
        public int getCount() {
          return CONTENT.length;
        }
    }

    private String TAG = "AlarmClock";
    private int lastPosition = -1;
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mScrolledPosition = position + positionOffset;

        // do transition of tabs
//liuqipeng begin
/*
        if(mLeftEdge != null && mRightEdge != null){
            mLeftEdge.finish();
            mRightEdge.finish();
            mLeftEdge.setSize(0, 0);
            mRightEdge.setSize(0, 0);
        }
        if (position == ALARM_TAB_INDEX) {
            mAlarmFragment.onAnimationUpdate(positionOffset);
            mClockFragment.onAnimationUpdate(positionOffset, true);
            if (mAlarmFragment.getRefreshableView()
                    && mClockFragment.getRefreshableView()) {
                if (positionOffsetPixels > 0) {
                    mMyAnalogClock.setVisibility(View.VISIBLE);
                    mMyAnalogClock.setAlpha(1.0f - positionOffset);
                    mWorldMyAnalogClock.setVisibility(View.VISIBLE);
                    mWorldMyAnalogClock.setAlpha(positionOffset);
                    mAlarmFragment.removeAnalogClock();
                    mClockFragment.removeWorldAnalogClock();
                } else {
                    setClockViewGone();
                    mAlarmFragment.showAnalogClock();
                    mClockFragment.showWorldAnalogClock();
                }
            }

        } else if (position == CLOCK_TAB_INDEX) {
            mClockFragment.onAnimationUpdate(positionOffset, false);
            mTimerFragment.onAnimationUpdate(positionOffset, true);
            if (mClockFragment.getRefreshableView()) {
                if (positionOffsetPixels > 0) {
                    mWorldMyAnalogClock.setVisibility(View.VISIBLE);
                    mWorldMyAnalogClock.setAlpha(1.0f - positionOffset);
                    mTimerView.setVisibility(View.VISIBLE);
                    mTimerView.setAlpha(positionOffset);
                    mClockFragment.removeWorldAnalogClock();
                    mTimerFragment.removeCircleLayout();
                } else {
                    setClockViewGone();
                    mAlarmFragment.showAnalogClock();
                    mClockFragment.showWorldAnalogClock();
                    mTimerFragment.showCircleLayout();
                }
            }
        } else if (position == TIMER_TAB_INDEX) {
            mTimerFragment.onAnimationUpdate(positionOffset, false);
            mStopwatchFragment.onAnimationUpdate(positionOffset);
            if (mStopwatchFragment.getRefreshableView()) {
                if (positionOffsetPixels > 0) {
                    mTimerView.setVisibility(View.VISIBLE);
                    mTimerView.setAlpha(1.0f - positionOffset);
                    mStopwatchView.setVisibility(View.VISIBLE);
                    mStopwatchView.setAlpha(positionOffset);
                    mTimerFragment.removeCircleLayout();
                    mStopwatchFragment.removeTimerSet();
                } else {
                    setClockViewGone();
                    mTimerFragment.showCircleLayout();
                }
            }
            if (positionOffsetPixels == 0) {
                mWorldMyAnalogClock.setVisibility(View.GONE);
                mTimerFragment.showCircleLayout();
            }
        } else {
            if (mStopwatchFragment.getRefreshableView()) {
                setClockViewGone();
                mTimerFragment.showCircleLayout();
                mStopwatchFragment.showTimerSet();
            }

        }
*/
        if(mLeftEdge != null && mRightEdge != null){
            mLeftEdge.finish();
            mRightEdge.finish();
            mLeftEdge.setSize(0, 0);
            mRightEdge.setSize(0, 0);
        }
        if (position == ALARM_TAB_INDEX) {
			resetClockView();
            mAlarmFragment.onAnimationUpdate(positionOffset);
            mClockFragment.onAnimationUpdate(positionOffset, true);
 			if (mAlarmFragment.getRefreshableView()
                    && mClockFragment.getRefreshableView()) {
                if (positionOffset>0.0f) {
					mMyAnalogClock.invalidate();
                    mMyAnalogClock.setVisibility(View.VISIBLE);
                    mMyAnalogClock.setAlpha(1.0f - positionOffset);
                    mWorldMyAnalogClock.setVisibility(View.VISIBLE);
                    mWorldMyAnalogClock.setAlpha(positionOffset);
                    mAlarmFragment.removeAnalogClock();
                    mClockFragment.removeWorldAnalogClock();
                } else {
					resetClockView();
                }
            }
        } else if (position == CLOCK_TAB_INDEX) {
			resetClockView();
            mClockFragment.onAnimationUpdate(positionOffset, false);
            mStopwatchFragment.onAnimationUpdate(positionOffset, true);
            if (mClockFragment.getRefreshableView()
                    && mStopwatchFragment.getRefreshableView()) {
                if (positionOffset>0.0f) {
                    mWorldMyAnalogClock.setVisibility(View.VISIBLE);
                    mWorldMyAnalogClock.setAlpha(1.0f - positionOffset);
                    mStopwatchView.setVisibility(View.VISIBLE);
                    mStopwatchView.setAlpha(positionOffset);
					mCircleTimerView.setAlpha(positionOffset);
					mCircleTimerView.invalidate();
                    mClockFragment.removeWorldAnalogClock();
                    mStopwatchFragment.removeTimerSet();
                } else {
					resetClockView();
                }
            }
        } else if (position == STOPWATCH_TAB_INDEX) {
			resetClockView();
            mStopwatchFragment.onAnimationUpdate(positionOffset,false);
			mTimerFragment.onAnimationUpdate(positionOffset);
 			if (mStopwatchFragment.getRefreshableView()) {
                if (positionOffset>0.0f) {
                    mStopwatchView.setVisibility(View.VISIBLE);
                    mStopwatchView.setAlpha(1.0f - positionOffset);
					mCircleTimerView.setAlpha(1.0f-positionOffset);
					mCircleTimerView.invalidate();
                    mTimerView.setVisibility(View.VISIBLE);
                    mTimerView.setAlpha(positionOffset);
                    mStopwatchFragment.removeTimerSet();
					mTimerFragment.removeCircleLayout();
                } else {
					resetClockView();
                }
            }
        }else if (position == TIMER_TAB_INDEX){
			resetClockView();
		}
//liuqipeng
    }

    @Override
    public void onPageSelected(int position) {
        Log.i(TAG, "this onPageSelected event and position is :" + position);
        resetClockView();
        lastPosition = PageEnterTracker(lastPosition, position);
        switch (position) {
        case 0:
            setTabAlarmText();
//liuqipeng begin
			mMyAnalogClock.enableClockAnimation();
			mAlarmFragment.enableClockAnimation();
//liuqipeng
            break;
        case 1:
            setTabClockText();
//liuqipeng begin
			mWorldMyAnalogClock.enableClockAnimation();
			mClockFragment.enableClockAnimation();	
//liuqipeng
            break;
//liuqipeng begin
        /*case 2:
            setTabTimerText();
            break;
        case 3:
            setTabStopwatchText();*/
        case 3:
            setTabTimerText();
            break;
        case 2:
            setTabStopwatchText();
//liuqipeng
            break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        /*if (mCoolOpen == true) {
            mCoolOpen = false;
            mViewPager.setOffscreenPageLimit(3);//this keep the other 3 fragment in stack
        }*/
    }

    private void setClockViewGone() {
        if (mMyAnalogClock != null) {
            mMyAnalogClock.setVisibility(View.GONE);
        } else {
            Log.e(LOG_TAG, "mMyAnalogClock is null");
        }
        if (mWorldMyAnalogClock != null) {
            mWorldMyAnalogClock.setVisibility(View.GONE);
        } else {
            Log.e(LOG_TAG, "mWorldMyAnalogClock is null");
        }
        if (mTimerView != null) {
            mTimerView.setVisibility(View.GONE);
        } else {
            Log.e(LOG_TAG, "mTimerView is null");
        }
        if (mStopwatchView != null) {
            mStopwatchView.setVisibility(View.GONE);
        } else {
            Log.e(LOG_TAG, "mStopwatchView is null");
        }
    }
    private void resetClockView() {
        setClockViewGone();
        if (mAlarmFragment != null) {
            mAlarmFragment.showAnalogClock();
        } else {
            Log.e(LOG_TAG, "mAlarmFragment is null");
        }
        if (mClockFragment != null) {
            mClockFragment.showWorldAnalogClock();
        } else {
            Log.e(LOG_TAG, "mClockFragment is null");
        }
        if (mTimerFragment != null) {
            mTimerFragment.showCircleLayout();
        } else {
            Log.e(LOG_TAG, "mTimerFragment is null");
        }
        if (mStopwatchFragment != null) {
            mStopwatchFragment.showTimerSet();
        } else {
            Log.e(LOG_TAG, "mStopwatchFragment is null");
        }
    }

    private int PageEnterTracker(int olderPosition, int index) {
        Log.i(TAG, "olderPosition is :" + olderPosition + "and  index is : " + index);
        SharedPreferences prefs1 = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor1 = prefs1.edit();
        switch(olderPosition) {
            case 0:
                TA.getInstance().getDefaultTracker().pageLeave("Page_AlarmClockFragment_leave");
                AlarmClockFragment.getInstance().refreashView();
                break;
            case 1:
                TA.getInstance().getDefaultTracker().pageLeave("Page_ClockFragment_leave");
                ClockFragment.getInstance().refreashView();
                break;
//liuqipeng begin
            /*case 2:
                TA.getInstance().getDefaultTracker().pageLeave("Page_TimerFragment_leave");
                break;
            case 3:
                TA.getInstance().getDefaultTracker().pageLeave("Page_StopwatchFragment_leave");
                StopwatchFragment.getInstance().refreashView();
                break;*/
            case 3:
                TA.getInstance().getDefaultTracker().pageLeave("Page_TimerFragment_leave");
                break;
            case 2:
                TA.getInstance().getDefaultTracker().pageLeave("Page_StopwatchFragment_leave");
                StopwatchFragment.getInstance().refreashView();
                break;
//liuqipeng
            default:
                break;
        }

        switch (index) {
            case 0:
                TA.getInstance().getDefaultTracker().pageEnter("Page_AlarmClockFragment");
                //TimerFragment.getInstance().setPlayRunSound(false);
                //StopwatchFragment.getInstance().setPlayRunSound(false);
                editor1.putBoolean("mTimerFragmentPlayRunSound", false);
                editor1.putBoolean("mStopwatchFragmentPlayRunSound", false);
                editor1.apply();
                break;
            case 1:
                TA.getInstance().getDefaultTracker().pageEnter("Page_ClockFragment");
                //TimerFragment.getInstance().setPlayRunSound(false);
                //StopwatchFragment.getInstance().setPlayRunSound(false);
                editor1.putBoolean("mTimerFragmentPlayRunSound", false);
                editor1.putBoolean("mStopwatchFragmentPlayRunSound", false);
                editor1.apply();
                break;
//liuqipeng begin
            /*case 2:
                TA.getInstance().getDefaultTracker().pageEnter("Page_TimerFragment");
                //TimerFragment.getInstance().setPlayRunSound(true);
                //StopwatchFragment.getInstance().setPlayRunSound(false);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                boolean isFirstTimeGuide = prefs.getBoolean("isFirstTimeGuide", true);
                if(isFirstTimeGuide){
                    TimerFragment.getInstance().startGuide();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isFirstTimeGuide", false);
                    prefs.edit();
                    editor.apply();
                }
                editor1.putBoolean("mTimerFragmentPlayRunSound", true);
                editor1.putBoolean("mStopwatchFragmentPlayRunSound", false);
                editor1.apply();
                setTabTimerText();
                break;
            case 3:
                TA.getInstance().getDefaultTracker().pageEnter("Page_StopwatchFragment");
                //TimerFragment.getInstance().setPlayRunSound(false);
                //StopwatchFragment.getInstance().setPlayRunSound(true);
                editor1.putBoolean("mTimerFragmentPlayRunSound", false);
                editor1.putBoolean("mStopwatchFragmentPlayRunSound", true);
                editor1.apply();
                setTabStopwatchText();
                break;
            default:
                break;*/
            case 3:
                TA.getInstance().getDefaultTracker().pageEnter("Page_TimerFragment");
                //TimerFragment.getInstance().setPlayRunSound(true);
                //StopwatchFragment.getInstance().setPlayRunSound(false);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                boolean isFirstTimeGuide = prefs.getBoolean("isFirstTimeGuide", true);
                if(isFirstTimeGuide){
                    TimerFragment.getInstance().startGuide();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isFirstTimeGuide", false);
                    prefs.edit();
                    editor.apply();
                }
                editor1.putBoolean("mTimerFragmentPlayRunSound", true);
                editor1.putBoolean("mStopwatchFragmentPlayRunSound", false);
                editor1.apply();
                setTabTimerText();
                break;
            case 2:
                TA.getInstance().getDefaultTracker().pageEnter("Page_StopwatchFragment");
                //TimerFragment.getInstance().setPlayRunSound(false);
                //StopwatchFragment.getInstance().setPlayRunSound(true);
                editor1.putBoolean("mTimerFragmentPlayRunSound", false);
                editor1.putBoolean("mStopwatchFragmentPlayRunSound", true);
                editor1.apply();
                setTabStopwatchText();
                break;
            default:
                break;
//liuqipeng
        }
        olderPosition = index;
        return olderPosition;
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        if (DEBUG) Log.d(LOG_TAG, "onNewIntent with intent: " + newIntent);

        // update our intent so that we can consult it to determine whether or
        // not the most recent launch was via a dock event
        setIntent(newIntent);

        // Timer receiver may ask to go to the timers fragment if a timer expired.
        int tab = newIntent.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
        if (tab != -1) {
            if (mActionBar != null) {
                //mActionBar.setSelectedNavigationItem(tab);
                mViewPager.setCurrentItem(tab);
                mTabsAdapter.getItem(tab);
                lastPosition = tab;
                mSelectedTab = tab;
            }
        }
    }

    private void initViews() {
        if (mTabsAdapter == null) {
            //mViewPager = new ViewPager(this);
            mViewPager = (AlarmViewPager) findViewById(R.id.desk_clock_pager);
            mViewPager.setId(R.id.desk_clock_pager);
            // Keep all four tabs to minimize jank.
            mViewPager.setOffscreenPageLimit(3);
            mViewPager.setBackgroundColor(Color.WHITE);
            //mTabsAdapter = new TabsAdapter(this, mViewPager);
            //createTabs(mSelectedTab);
            showBackKey(true);
        }
        //setContentView(mViewPager);
        mViewPager.setAdapter(mTabsAdapter);
        mActionBar.setSelectedNavigationItem(mSelectedTab);
    }
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ItemView view = (ItemView) v;
        ItemAdapter adapter = (ItemAdapter)l.getAdapter();
        Item item = (Item)adapter.getItem(position);
        item.setChecked(!item.isChecked());
        view.setObject(item);
    }

    private void createTabs(int selectedIndex) {
        mActionBar = getActionBar();

        if (mActionBar != null) {
            mActionBar.setDisplayOptions(0);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            //mActionBar.setDisplayShowTitleEnabled(false);
            mActionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
            // TODO penglei: replace the setIcon by setText, and use R.string to instead.
            mAlarmTab = mActionBar.newTab();
            //mAlarmTab.setIcon(R.drawable.alarm_tab);
            mAlarmTab.setText("闹钟");
            mAlarmTab.setContentDescription(R.string.menu_alarm);
            //mTabsAdapter.addTab(mAlarmTab, AlarmClockFragment.class, ALARM_TAB_INDEX);

            mClockTab = mActionBar.newTab();
            //mClockTab.setIcon(R.drawable.clock_tab);
            mClockTab.setText("世界时钟");
            mClockTab.setContentDescription(R.string.menu_clock);
            //mTabsAdapter.addTab(mClockTab, ClockFragment.class, CLOCK_TAB_INDEX);

            mTimerTab = mActionBar.newTab();
            //mTimerTab.setIcon(R.drawable.timer_tab);
            mTimerTab.setText("计时器");
            mTimerTab.setContentDescription(R.string.menu_timer);
            //mTabsAdapter.addTab(mTimerTab, TimerFragment.class, TIMER_TAB_INDEX);

            mStopwatchTab = mActionBar.newTab();
            //mStopwatchTab.setIcon(R.drawable.stopwatch_tab);
            mStopwatchTab.setText("秒表");
            mStopwatchTab.setContentDescription(R.string.menu_stopwatch);
            //mTabsAdapter.addTab(mStopwatchTab, StopwatchFragment.class,STOPWATCH_TAB_INDEX);

            mActionBar.setSelectedNavigationItem(selectedIndex);
            //mTabsAdapter.notifySelectedPage(selectedIndex);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        Log.i("AlarmClock", "onCreate begin");

        try {
            super.onCreate(icicle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StatConfig.getInstance().setContext(this.getApplicationContext());
        StatConfig.getInstance().turnOnDebug();

        Tracker lTracker = TA.getInstance().getTracker("21736479");
        lTracker.setAppKey("21736479");
        TA.getInstance().setDefaultTracker(lTracker);


        CONTENT = getResources().getStringArray(R.array.desk_clock_tab_names);
        mSelectedTab = ALARM_TAB_INDEX;
        if (icicle != null) {
            mSelectedTab = icicle.getInt(KEY_SELECTED_TAB, ALARM_TAB_INDEX);
        }
        try {
            setActivityContentView(R.layout.desk_clock);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.showBackKey(false);

        getFooterBarImpl().setVisibility(View.GONE);
        mTabsAdapter = new SampleFragmentPagerAdapter(getSupportFragmentManager());

        mActionBar = getActionBar();
        mViewPager = (AlarmViewPager) findViewById(R.id.desk_clock_pager);
        mViewPager.setAdapter(mTabsAdapter);
        mViewPager.setOffscreenPageLimit(0);
        mViewPager.setScrollable(true);
        HWTabPageSimpleIndicator indicator = new HWTabPageSimpleIndicator(this);
        indicator.setViewPager(mViewPager);

        try {
            Field rightEdgeField = mViewPager.getClass().getSuperclass().getDeclaredField("mRightEdge");
            Field leftEdgeField = mViewPager.getClass().getSuperclass().getDeclaredField("mLeftEdge");
            if(leftEdgeField != null ){
                leftEdgeField.setAccessible(true);
                mLeftEdge = (EdgeEffectCompat) leftEdgeField.get(mViewPager);
            }
            if(rightEdgeField != null){
                rightEdgeField.setAccessible(true);
                mRightEdge = (EdgeEffectCompat) rightEdgeField.get(mViewPager);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(getActionBarView() != null) {
            getActionBarView().setTabPageIndicator(indicator);
        }

        //setTabPageIndicator(indicator);
        indicator.setOnPageChangeListener(this);
        setHomeTimeZone();

        // We need to update the system next alarm time on app startup because the
        // user might have clear our data.
        mContext = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                AlarmStateManager.updateNextAlarm(DeskClock.this);

                int version = -1;
                String dataStr = "";
                Bundle data = mContext.getContentResolver().call(HolidayProviderContract.CONTENT_URI,
                        HolidayProviderContract.METHOD_READ_HOLIDAY_DATA, null, null);
                if (data != null) {
                    mCalendarDataSupport = true;
                    version = data.getInt(HolidayProviderContract.KEY_HOLIDAY_VERSION, -1);
                    dataStr = data.getString(HolidayProviderContract.KEY_HOLIDAY_DATA);
                    if (DEBUG)
                        Log.e("AlarmClock", "query Holiday1: " + version + ", " + dataStr);
                 }else{
                    mCalendarDataSupport = false;
                }
                Log.e("AlarmClock", "query Holiday2: " + version + ", " + dataStr);

                try {
                    InputStream inputStream = new ByteArrayInputStream(dataStr.getBytes());
                    parser = new HolidayParser();
                    parser.parse(inputStream);
                    parser.save(mContext, version);
                } catch (Exception e) {
                    Log.e("AlarmClock", "parseResourceFile(holiday) parse failed, delete ");
                }
            }
        }).start();
        //am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        //register contentObserver for alarms
        mAlarmObserver = new AlarmObserver(mHandler);
        getContentResolver().registerContentObserver(Alarm.CONTENT_URI, true, mAlarmObserver);
        Intent i = getIntent();
        if (i != null) {
            int tab = i.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
            if (tab != -1) {
                mViewPager.setCurrentItem(tab);
                mSelectedTab = tab;
                lastPosition = mSelectedTab;
            }
        }

        mMyAnalogClock = (MyAnalogClock) findViewById(R.id.clock_dial);
        mWorldMyAnalogClock = (WorldMyAnalogClock) findViewById(R.id.worldclock_dial);
        mTimerView = (ImageView) findViewById(R.id.timer_dial);
        mStopwatchView = findViewById(R.id.stopwatch_dial);
//liuqipeng begin
		mCircleTimerView=(CircleTimerView)findViewById(R.id.my_circle_timer_view);
		mCountingTimerView=(CountingTimerView)findViewById(R.id.stopwatch_time_text1);
		mCountingTimerView.setTime(0, true, true);
//liuqipeng

        // alarm indicator
        mTabAlarm = (TextView) findViewById(R.id.alarm_id);
        mTabAlarm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScrolledPosition == ALARM_TAB_INDEX) {
                    Log.d(TAG, "View pager has been already in alarm fragment.");
                    return;
                }
                mViewPager.setCurrentItem(ALARM_TAB_INDEX);
                mSelectedTab = ALARM_TAB_INDEX;
                lastPosition = mSelectedTab;
                setTabAlarmText();
            }
        });
        // clock indicator
        mTabClock = (TextView) findViewById(R.id.worldclock_id);
        mTabClock.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScrolledPosition == CLOCK_TAB_INDEX) {
                    Log.d(TAG, "View pager has been in clock fragment already.");
                    return;
                }
                mViewPager.setCurrentItem(CLOCK_TAB_INDEX);
                mSelectedTab = CLOCK_TAB_INDEX;
                lastPosition = mSelectedTab;
                setTabClockText();
            }
        });
        // timer indicator
        mTabTimer = (TextView) findViewById(R.id.timer_id);
        mTabTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScrolledPosition == TIMER_TAB_INDEX) {
                    Log.d(TAG, "View pager has been in timer fragment already.");
                    return;
                }
                mViewPager.setCurrentItem(TIMER_TAB_INDEX);
                mSelectedTab = TIMER_TAB_INDEX;
                lastPosition = mSelectedTab;
                setTabTimerText();
            }
        });
        // stopwatch indicator
        mTabStopwatch = (TextView) findViewById(R.id.stopwatch_id);
        mTabStopwatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScrolledPosition == STOPWATCH_TAB_INDEX) {
                    Log.d(TAG, "View pager has been in stopwatch fragment already.");
                    return;
                }
                mViewPager.setCurrentItem(STOPWATCH_TAB_INDEX);
                mSelectedTab = STOPWATCH_TAB_INDEX;
                lastPosition = mSelectedTab;
                setTabStopwatchText();
            }
        });
    }

    private void setTabAlarmText() {
        if (mTabAlarm != null) { mTabAlarm.setTextColor(getResources().getColor(R.color.white)); }
        if (mTabClock != null) { mTabClock.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabTimer != null) { mTabTimer.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabStopwatch != null) { mTabStopwatch.setTextColor(getResources().getColor(R.color.clock_gray)); }
    }

    private void setTabClockText() {
        if (mTabAlarm != null) { mTabAlarm.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabClock != null) { mTabClock.setTextColor(getResources().getColor(R.color.white)); }
        if (mTabTimer != null) { mTabTimer.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabStopwatch != null) { mTabStopwatch.setTextColor(getResources().getColor(R.color.clock_gray)); }
    }

    private void setTabTimerText() {
        if (mTabAlarm != null) { mTabAlarm.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabClock != null) { mTabClock.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabTimer != null) { mTabTimer.setTextColor(getResources().getColor(R.color.white)); }
        if (mTabStopwatch != null) { mTabStopwatch.setTextColor(getResources().getColor(R.color.clock_gray)); }
    }

    private void setTabStopwatchText() {
        if (mTabAlarm != null) { mTabAlarm.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabClock != null) { mTabClock.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabTimer != null) { mTabTimer.setTextColor(getResources().getColor(R.color.clock_gray)); }
        if (mTabStopwatch != null) { mTabStopwatch.setTextColor(getResources().getColor(R.color.white)); }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        getContentResolver().unregisterContentObserver(mAlarmObserver);
        super.onDestroy();
    }

    @Override
    public void onStop() {
    if(mFirstStop == true){
        mFirstStop = false;
        if(mAlarmFragment != null){
            mAlarmFragment.BuryInformation();
        }
    }
    super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("AlarmClock", "onResume begin");

        if (null == TA.getInstance() || null == TA.getInstance().getDefaultTracker()) {
            StatConfig.getInstance().setContext(this.getApplicationContext());
            StatConfig.getInstance().turnOnDebug();
            Tracker lTracker = TA.getInstance().getTracker("21736479");
            lTracker.setAppKey("21736479");
            TA.getInstance().setDefaultTracker(lTracker);
        }

        mAlarmFragment = AlarmClockFragment.getInstance();
        mClockFragment = ClockFragment.getInstance();
        mTimerFragment = TimerFragment.getInstance();
        mStopwatchFragment = StopwatchFragment.getInstance();

        // We only want to show notifications for stopwatch/timer when the app is closed so
        // that we don't have to worry about keeping the notifications in perfect sync with
        // the app.

        new Thread(new Runnable() {
            @Override
            public void run() {
            Intent stopwatchIntent = new Intent(getApplicationContext(), StopwatchService.class);
            stopwatchIntent.setAction(Stopwatches.KILL_NOTIF);
            startService(stopwatchIntent);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Timers.NOTIF_APP_OPEN, true);
            editor.apply();
            //TODO penglei : strangely this will keep the screen on all the time,fix it later.
            Intent timerIntent = new Intent();
            timerIntent.setAction(Timers.NOTIF_IN_USE_CANCEL);
            sendBroadcast(timerIntent);
            }
        }).start();

            Log.i("AlarmClock", "onResume end");

        lastPosition = PageEnterTracker(-1, mSelectedTab);
    }

    @Override
    public void onPause() {
        Intent intent = new Intent(getApplicationContext(), StopwatchService.class);
        intent.setAction(Stopwatches.SHOW_NOTIF);
        startService(intent);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Timers.NOTIF_APP_OPEN, false);
        editor.apply();
        //TODO penglei: fix me ,this may keep the screen on all the time,fix it later
        Utils.showInUseNotifications(this);

        PageEnterTracker(lastPosition, -1);
        mSelectedTab = lastPosition;
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        mAlarmFragment = AlarmClockFragment.getInstance();
        mClockFragment = ClockFragment.getInstance();
        mTimerFragment = TimerFragment.getInstance();
        mStopwatchFragment = StopwatchFragment.getInstance();

        Log.i("AlarmClock", "onActivityResult is called." + requestCode + "and " + resultCode);
        if (requestCode == mClockFragment.REQUEST_ADD_WORLDCLOCK) {
            if (resultCode == Activity.RESULT_OK){
                Log.i("AlarmClock", "WorldClock added.");
                mClockFragment.asyncAddClock();
            }
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            Alarm alarmData = data.getParcelableExtra("alarm_changed");
			Log.i("ClockTrigger","get alarmData with key---alarm_changed");//liuqipeng add
            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                Bundle ringtoneCache = data.getParcelableExtra("ringtone_cache");
				Log.i("ClockTrigger","get ringtoneCache with key---ringtone_cache");//liuqipeng add
                mAlarmFragment.mRingtoneTitleCache = ringtoneCache;
            }
            switch (requestCode) {
                case AlarmClockFragment.REQUEST_ADD_ALARM:
                    Log.i("AlarmClock", "alarm added.");
					Log.i("ClockTrigger","alarm added.");//liuqipeng add
                    mAlarmFragment.asyncAddAlarm(alarmData);
                    break;
                case AlarmClockFragment.REQUEST_UPDATE_ALARM:
                    Log.i("AlarmClock", "alarm update.");
					Log.i("ClockTrigger","alarm update.");//liuqipeng add
                    mAlarmFragment.mScrollToAlarmId = alarmData.id;
                    mAlarmFragment.asyncUpdateAlarm(alarmData, true);
                    mAlarmFragment.mSelectedAlarm = null;
                    break;

                default:
                    Log.w("AlarmClock", "Unhandled request code in onActivityResult: " + requestCode);
            }
        } else if (AlarmFeatureOption.YUNOS_MTK_PLATFORM && resultCode == Activity.RESULT_CANCELED){
            mAlarmFragment.mFromCancel = true;
            if (data != null) {
                Bundle ringtoneCache = data.getParcelableExtra("ringtone_cache");
                if (ringtoneCache != null) {
                    mAlarmFragment.mRingtoneTitleCache = ringtoneCache;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
        } catch (Exception e) {
            e.printStackTrace();
        }
        outState.putInt(KEY_SELECTED_TAB, mActionBar.getSelectedNavigationIndex());
    }

    /*public void clockButtonsOnClick(View v) {
        if (v == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.cities_button:
                TA.getInstance().getDefaultTracker().commitEvent("Page_ClockFragment",2101, "Page_ClockFragment_Button_AddClock", null, null, null);
                startActivity(new Intent(this, CitiesActivity.class));
                break;
            case R.id.delete_cities_button:
                TA.getInstance().getDefaultTracker().commitEvent("Page_ClockFragment",2101, "Page_ClockFragment_Button_DeleteClock", null, null, null);
                startActivity(new Intent(this, DeleteClockActivity.class));
                break;
            case R.id.menu_button:
                TA.getInstance().getDefaultTracker().commitEvent("Page_ClockFragment",2101, "Page_ClockFragment_Button_SettingClock", null, null, null);
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.menu_button_alarmpage:
                TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmClockFragment",2101, "Page_AlarmClockFragment_Button_SettingAlarmClock", null, null, null);
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                break;
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // We only want to show it as a menu in landscape, and only for clock/alarm fragment.
        mMenu = menu;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mActionBar.getSelectedNavigationIndex() == ALARM_TAB_INDEX ||
                    mActionBar.getSelectedNavigationIndex() == CLOCK_TAB_INDEX) {
                // Clear the menu so that it doesn't get duplicate items in case onCreateOptionsMenu
                // was called multiple times.
                menu.clear();
                getMenuInflater().inflate(R.menu.desk_clock_menu, menu);
            }
            // Always return true for landscape, regardless of whether we've inflated the menu, so
            // that when we switch tabs this method will get called and we can inflate the menu.
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //updateMenu(menu);
        return true;
    }

    private void updateMenu(Menu menu) {
        // Hide "help" if we don't have a URI for it.
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }

//        // Hide "lights out" for timer.
//        MenuItem nightMode = menu.findItem(R.id.menu_item_night_mode);
//        if (mActionBar.getSelectedNavigationIndex() == ALARM_TAB_INDEX) {
//            nightMode.setVisible(false);
//        } else if (mActionBar.getSelectedNavigationIndex() == CLOCK_TAB_INDEX) {
//            nightMode.setVisible(true);
//        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (processMenuClick(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean processMenuClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                startActivity(new Intent(DeskClock.this, SettingsActivity.class));
                return true;
/*            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i != null) {
                    try {
                        startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        // No activity found to match the intent - ignore
                    }
                }
                return true;
            case R.id.menu_item_night_mode:
                startActivity(new Intent(DeskClock.this, ScreensaverActivity.class));*/
            default:
                break;
        }
        return true;
    }

    /***
     * Insert the local time zone as the Home Time Zone if one is not set
     */
    private void setHomeTimeZone() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String homeTimeZone = prefs.getString(SettingsActivity.KEY_HOME_TZ, "");
        if (!homeTimeZone.isEmpty()) {
            return;
        }
        homeTimeZone = TimeZone.getDefault().getID();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SettingsActivity.KEY_HOME_TZ, homeTimeZone);
        editor.apply();
        Log.v(LOG_TAG, "Setting home time zone to " + homeTimeZone);
    }

    /*public void registerPageChangedListener(DeskClockFragment frag) {
        if (mTabsAdapter != null) {
            //mTabsAdapter.registerPageChangedListener(frag);
        }
    }*/

    /*public void unregisterPageChangedListener(DeskClockFragment frag) {
        if (mTabsAdapter != null) {
            //mTabsAdapter.unregisterPageChangedListener(frag);
        }
    }*/

    /***
     * Adapter for wrapping together the ActionBar's tab with the ViewPager
     */

//    private class TabsAdapter extends FragmentPagerAdapter
//            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
//
//        private static final String KEY_TAB_POSITION = "tab_position";
//
//        final class TabInfo {
//            private final Class<?> clss;
//            private final Bundle args;
//
//            TabInfo(Class<?> _class, int position) {
//                clss = _class;
//                args = new Bundle();
//                args.putInt(KEY_TAB_POSITION, position);
//            }
//
//            public int getPosition() {
//                return args.getInt(KEY_TAB_POSITION, 0);
//            }
//        }
//
//        private final ArrayList<TabInfo> mTabs = new ArrayList <TabInfo>();
//        ActionBar mMainActionBar;
//        Context mContext;
//        ViewPager mPager;
//        // Used for doing callbacks to fragments.
//        HashSet<String> mFragmentTags = new HashSet<String>();
//
//        public TabsAdapter(Activity activity, ViewPager pager) {
//            super(activity.getFragmentManager());
//            mContext = activity;
//            mMainActionBar = activity.getActionBar();
//            mPager = pager;
//            mPager.setAdapter(this);
//            mPager.setOnPageChangeListener(this);
//        }
//
//        @Override
//        public Fragment getItem(int position) {
//            TabInfo info = mTabs.get(getRtlPosition(position));
//            DeskClockFragment f = (DeskClockFragment) Fragment.instantiate(
//                    mContext, info.clss.getName(), info.args);
//            return f;
//        }
//
//        @Override
//        public int getCount() {
//            return mTabs.size();
//        }
//
//        public void addTab(ActionBar.Tab tab, Class<?> clss, int position) {
//            TabInfo info = new TabInfo(clss, position);
//            tab.setTag(info);
//            tab.setTabListener(this);
//            mTabs.add(info);
//            mMainActionBar.addTab(tab);
//            notifyDataSetChanged();
//        }
//
//        @Override
//        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//            // Do nothing
//        }
//
//        @Override
//        public void onPageSelected(int position) {
//            // Set the page before doing the menu so that onCreateOptionsMenu knows what page it is.
//            mMainActionBar.setSelectedNavigationItem(getRtlPosition(position));
//            notifyPageChanged(position);
//
//            // Only show the overflow menu for alarm and world clock.
//            if (mMenu != null) {
//                // Make sure the menu's been initialized.
//                if (position == ALARM_TAB_INDEX || position == CLOCK_TAB_INDEX) {
//                    mMenu.setGroupVisible(R.id.menu_items, true);
//                    onCreateOptionsMenu(mMenu);
//                } else {
//                    mMenu.setGroupVisible(R.id.menu_items, false);
//                }
//            }
//        }
//
//        @Override
//        public void onPageScrollStateChanged(int state) {
//            // Do nothing
//        }
//
//        @Override
//        public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
//            // Do nothing
//        }
//
//        @Override
//        public void onTabSelected(Tab tab, FragmentTransaction ft) {
//            TabInfo info = (TabInfo)tab.getTag();
//            int position = info.getPosition();
//            mPager.setCurrentItem(getRtlPosition(position));
//        }
//
//        @Override
//        public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
//            // Do nothing
//        }
//
//        public void notifySelectedPage(int page) {
//            notifyPageChanged(page);
//        }
//
//        private void notifyPageChanged(int newPage) {
//            for (String tag : mFragmentTags) {
//                final FragmentManager fm = getFragmentManager();
//                DeskClockFragment f = (DeskClockFragment) fm.findFragmentByTag(tag);
//                if (f != null) {
//                    f.onPageChanged(newPage);
//                }
//            }
//        }
//
//        public void registerPageChangedListener(DeskClockFragment frag) {
//            String tag = frag.getTag();
//            if (mFragmentTags.contains(tag)) {
//                Log.wtf(LOG_TAG, "Trying to add an existing fragment " + tag);
//            } else {
//                mFragmentTags.add(frag.getTag());
//            }
//            // Since registering a listener by the fragment is done sometimes after the page
//            // was already changed, make sure the fragment gets the current page
//            frag.onPageChanged(mMainActionBar.getSelectedNavigationIndex());
//        }
//
//        public void unregisterPageChangedListener(DeskClockFragment frag) {
//            mFragmentTags.remove(frag.getTag());
//        }
//
//        private boolean isRtl() {
//            return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
//                    View.LAYOUT_DIRECTION_RTL;
//        }
//
//        private int getRtlPosition(int position) {
//            if (isRtl()) {
//                switch (position) {
//                    case TIMER_TAB_INDEX:
//                        return RTL_TIMER_TAB_INDEX;
//                    case CLOCK_TAB_INDEX:
//                        return RTL_CLOCK_TAB_INDEX;
//                    case STOPWATCH_TAB_INDEX:
//                        return RTL_STOPWATCH_TAB_INDEX;
//                    case ALARM_TAB_INDEX:
//                        return RTL_ALARM_TAB_INDEX;
//                    default:
//                        break;
//                }
//            }
//            return position;
//        }
//
//        @Override
//        public void onTabReselected(Tab arg0,
//                android.app.FragmentTransaction arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onTabSelected(Tab arg0, android.app.FragmentTransaction arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onTabUnselected(Tab arg0,
//                android.app.FragmentTransaction arg1) {
//            // TODO Auto-generated method stub
//
//        }
//    }
//
    public static abstract class OnTapListener implements OnTouchListener {
        private float mLastTouchX;
        private float mLastTouchY;
        private long mLastTouchTime;
        private final TextView mMakePressedTextView;
        private final int mPressedColor, mGrayColor;
        private final float MAX_MOVEMENT_ALLOWED = 20;
        private final long MAX_TIME_ALLOWED = 500;

        public OnTapListener(Activity activity, TextView makePressedView) {
            mMakePressedTextView = makePressedView;
            mPressedColor = activity.getResources().getColor(Utils.getPressedColorId());
            mGrayColor = activity.getResources().getColor(Utils.getGrayColorId());
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    mLastTouchTime = Utils.getTimeNow();
                    mLastTouchX = e.getX();
                    mLastTouchY = e.getY();
                    if (mMakePressedTextView != null) {
                        mMakePressedTextView.setTextColor(mPressedColor);
                    }
                    break;
                case (MotionEvent.ACTION_UP):
                    float xDiff = Math.abs(e.getX()-mLastTouchX);
                    float yDiff = Math.abs(e.getY()-mLastTouchY);
                    long timeDiff = (Utils.getTimeNow() - mLastTouchTime);
                    if (xDiff < MAX_MOVEMENT_ALLOWED && yDiff < MAX_MOVEMENT_ALLOWED
                            && timeDiff < MAX_TIME_ALLOWED) {
                        if (mMakePressedTextView != null) {
                            v = mMakePressedTextView;
                        }
                        processClick(v);
                        resetValues();
                        return true;
                    }
                    resetValues();
                    break;
                case (MotionEvent.ACTION_MOVE):
                    xDiff = Math.abs(e.getX()-mLastTouchX);
                    yDiff = Math.abs(e.getY()-mLastTouchY);
                    if (xDiff >= MAX_MOVEMENT_ALLOWED || yDiff >= MAX_MOVEMENT_ALLOWED) {
                        resetValues();
                    }
                    break;
                default:
                    resetValues();
            }
            return false;
        }

        private void resetValues() {
            mLastTouchX = -1*MAX_MOVEMENT_ALLOWED + 1;
            mLastTouchY = -1*MAX_MOVEMENT_ALLOWED + 1;
            mLastTouchTime = -1*MAX_TIME_ALLOWED + 1;
            if (mMakePressedTextView != null) {
                mMakePressedTextView.setTextColor(mGrayColor);
            }
        }

        protected abstract void processClick(View v);
    }

    /** Called by the LabelDialogFormat class after the dialog is finished. **/
    @Override
    public void onDialogLabelSet(TimerObj timer, String label, String tag) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(tag);
        if (frag instanceof TimerFragment) {
            ((TimerFragment) frag).setLabel(timer, label);
        }
    }

    /** Called by the LabelDialogFormat class after the dialog is finished. **/
    @Override
    public void onDialogLabelSet(Alarm alarm, String label, String tag) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(tag);
        if (frag instanceof AlarmClockFragment) {
            ((AlarmClockFragment) frag).setLabel(alarm, label);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Handle key down and key up on a few of the system keys.
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        switch (event.getKeyCode()) {
        // Volume keys and camera keys stop all the timers
        case KeyEvent.KEYCODE_BACK:
            if (up && isTaskRoot()) {
                moveTaskToBack(false);
                return true;
            } else {
                return super.dispatchKeyEvent(event);
            }
        default:
            break;
        }
        return super.dispatchKeyEvent(event);
    }
}
