/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.deskclock.timer;

import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentActivity;
import yunos.support.v4.app.FragmentManager;
import yunos.support.v4.app.FragmentTransaction;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;

/**
 * Timer alarm alert: pops visible indicator. This activity is the version which
 * shows over the lock screen.
 * This activity re-uses TimerFragment GUI
 */
public class TimerAlertFullScreen extends FragmentActivity {

    private static final String TAG = "TimerAlertFullScreen";
    private static final String FRAGMENT = "timer";
    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;
    //private TimerFragment mTimerFragment;

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                stopAllTimesUpTimers();
            }
        }
    };

    private final BroadcastReceiver homePressReceiver = new BroadcastReceiver() {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason != null && reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                    Log.e("alarm", "home pressed");
                    stopAllTimesUpTimers();
                }
            }
        }
    };
    
    private final BroadcastReceiver mTimeFullScreenPressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (AliTimeScreen.TIME_DISMISS_ACTION.equals(action)) {
                stopAllTimesUpTimers();
            }
        }
    };

    private final BroadcastReceiver mTimeFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (AliTimeScreen.TIME_FINISH_ACTION.equals(action)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED,FLAG_HOMEKEY_DISPATCHED);
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(homePressReceiver, homeFilter);
        IntentFilter powerfilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mBatInfoReceiver, powerfilter);
        
        IntentFilter timeFilter = new IntentFilter(AliTimeScreen.TIME_DISMISS_ACTION);
        registerReceiver(mTimeFullScreenPressReceiver, timeFilter);

        IntentFilter timeFinishFilter = new IntentFilter(AliTimeScreen.TIME_FINISH_ACTION);
        registerReceiver(mTimeFinishReceiver, timeFinishFilter);
        //setContentView(R.layout.timer_alert_full_screen);
        FrameLayout layout = new FrameLayout(this);
        layout.setId(android.R.id.custom);
        setContentView(layout);
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        // Turn on the screen unless we are being launched from the AlarmAlert
        // subclass as a result of the screen turning off.
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
		/*YUNOS BEGIN PB*/
        // ##Module:(AliDeskClock) ##author:xy83652@alibaba-inc.com
        // ##BugID:(7869088) ##date:2016/03/01
        // ##Description:window changed with navigation bar
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //WindowManager.LayoutParams.FLAG_FULLSCREEN);


        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        /*YUNOS END PB*/

        // Don't create overlapping fragments.
        if (getFragment() == null) {
        	//mTimerFragment = TimerFragment.getInstance();
        	//mTimerFragment.isFullScreen = true;
            // Create fragment and give it an argument to only show
            // timers in STATE_TIMESUP state
            //Bundle args = new Bundle();
            //args.putBoolean(Timers.TIMESUP_MODE, true);

            //mTimerFragment.setArguments(args);

            // Add the fragment to the 'fragment_container' FrameLayout
            //FragmentManager manager =  getSupportFragmentManager();
            //FragmentTransaction transaction = manager.beginTransaction();
            //transaction.add(R.id.fragment_container, mTimerFragment, FRAGMENT).commit();*/
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
			//transaction.add(R.id.fragment_container, new TimeFullScreenFragment());
            transaction.replace(android.R.id.custom, new TimeFullScreenFragment());
            transaction.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null == TA.getInstance() || null == TA.getInstance().getDefaultTracker()) {
            StatConfig.getInstance().setContext(this.getApplicationContext());
            StatConfig.getInstance().turnOnDebug();
            Tracker lTracker = TA.getInstance().getTracker("21736479");
            lTracker.setAppKey("21736479");
            TA.getInstance().setDefaultTracker(lTracker);
        }
        // Only show notifications for times-up when this activity closed.
        //Utils.cancelTimesUpNotifications(this);
    }

    @Override
    public void onPause() {
        //Utils.showTimesUpNotifications(this);

        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Handle key down and key up on a few of the system keys.
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        switch (event.getKeyCode()) {
        // Volume keys and camera keys stop all the timers
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (up) {
                stopAllTimesUpTimers();
            }
            return true;
        case KeyEvent.KEYCODE_VOLUME_MUTE:
        case KeyEvent.KEYCODE_CAMERA:
        case KeyEvent.KEYCODE_FOCUS:
            if (up) {
                stopAllTimesUpTimers();
            }
            return true;
        case KeyEvent.KEYCODE_BACK:
        return false;
        default:
            break;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * this is called when a second timer is triggered while a previous alert
     * window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        /*TimerFragment timerFragment = getFragment();
        if (timerFragment != null) {
            timerFragment.restartAdapter();
        }*/
        super.onNewIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //ViewGroup viewContainer = (ViewGroup)findViewById(R.id.fragment_container);
        //viewContainer.requestLayout();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private SharedPreferences mPrefs;
    private NotificationManager mNotificationManager;
    protected void stopAllTimesUpTimers() {
        /*if (mTimerFragment != null) {
        	mTimerFragment.stopAllTimesUpTimers();
        }*/
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mPrefs != null) {
            TimerObj mTimerObjTag = TimerObj.getTagTimersFromSharedPrefs(mPrefs, true);
            if (mTimerObjTag != null) {
                if (mTimerObjTag.mState == TimerObj.STATE_TIMESUP) {
                    cancelTimerNotification(mTimerObjTag.mTimerId);
                }
                // Tell receiver the timer was deleted.
                // It will stop all activity related to the
                // timer
                mTimerObjTag.mState = TimerObj.STATE_DELETED;
                updateTimersState(mTimerObjTag, Timers.DELETE_TIMER);
            }
            mTimerObjTag = null;
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean("isRunning", false);
            editor.putLong("timerStopTimer", 0);
            editor.putLong("timerSetTime", 0);
            editor.putLong("setTime", 0);
            editor.apply();
        }
        finish();
    }
    
    public void cancelTimerNotification(int timerId) {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(timerId);
        }
    }
    
    public void updateTimersState(TimerObj t, String action) {
        if (Timers.DELETE_TIMER.equals(action)) {
            t.deleteFromSharedPref(mPrefs);
        } else {
            t.writeToSharedPref(mPrefs);
        }
        Intent i = new Intent();
        i.setAction(action);
        i.putExtra(Timers.TIMER_INTENT_EXTRA, t.mTimerId);
        // Make sure the receiver is getting the intent ASAP.
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        sendBroadcast(i);
    }

    private TimerFragment getFragment() {
        //return (TimerFragment) getFragmentManager().findFragmentByTag(FRAGMENT);
        return null;
    }

    protected void onDestroy() {
        if(mBatInfoReceiver != null){
            try {
                unregisterReceiver(mBatInfoReceiver);
            } catch (Exception e) {

            }
        }

        if(homePressReceiver != null){
            try {
                unregisterReceiver(homePressReceiver);
            } catch (Exception e) {

            }
        }
        
        if(mTimeFullScreenPressReceiver != null){
            try {
                unregisterReceiver(mTimeFullScreenPressReceiver);
            } catch (Exception e) {

            }
        }

        if(mTimeFinishReceiver != null){
            try {
                unregisterReceiver(mTimeFinishReceiver);
            } catch (Exception e) {

            }
        }
        super.onDestroy();
    }
}
