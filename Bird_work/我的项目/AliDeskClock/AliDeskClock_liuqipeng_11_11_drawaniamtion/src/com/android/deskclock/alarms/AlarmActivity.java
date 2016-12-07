/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.deskclock.alarms;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import yunos.support.v4.app.FragmentActivity;
import yunos.support.v4.app.FragmentManager;
import yunos.support.v4.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.AlarmUtils;
import com.android.deskclock.AliAlarmScreen;
import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.multiwaveview.GlowPadView;
import com.android.deskclock.AlarmFragment;
import com.android.deskclock.AlarmFeatureOption;

import com.aliyun.ams.systembar.SystemBarColorManager;
import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;

import java.lang.reflect.Method;
/**
 * Alarm activity that pops up a visible indicator when the alarm goes off.
 */
public class AlarmActivity extends FragmentActivity {
    // AlarmActivity listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    // These defaults must match the values in res/xml/settings.xml
    private static final String DEFAULT_SNOOZE_MINUTES = "10";
    private final int SNOOZE_TIME = 10;
    private boolean mHasSnoozeDisplay = false;
    // AlarmActivity listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;
    private Context mContext;

    private static AlarmInstance mInstance;
    private int mVolumeBehavior;
    //private GlowPadView mGlowPadView;
    private AudioManager mAudioManager;
    public static boolean mRecentAPP;

    public static AlarmInstance getAlarmInstance() {
        return mInstance;
    }

    //private GlowPadController glowPadController = new GlowPadController();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("AlarmActivity - Broadcast Receiver - " + action);
            if (action.equals(ALARM_SNOOZE_ACTION)) {
                int snoozeTime = intent.getIntExtra("snoozeTime", SNOOZE_TIME);
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString("SnoozeTime", String.valueOf(snoozeTime)).commit();
                TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmAlert",
                        2101, "Button_Snooze", null, null, null);
                snooze();
            } else if (action.equals(ALARM_DISMISS_ACTION)) {
                TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmAlert",
                        2101, "Button_Dismiss", null, null, null);
                dismiss();
                if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
                    boolean isShutDown = intent.getBooleanExtra("isShutDown", false);
                    if (isShutDown) {
                        final Handler shutDown = new Handler();
                        shutDown.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                shutdown();
                            }
                        }, 500);
                    }
                }
            } else if (action.equals(AlarmService.ALARM_DONE_ACTION)) {
                finish();
            } else {
                Log.i("Unknown broadcast in AlarmActivity: " + action);
            }
        }
    };

    private void shutdown() {
        try {
            //»ñµÃServiceManagerÀà
            Class<?> ServiceManager = Class
                .forName("android.os.ServiceManager");

            //»ñµÃServiceManagerµÄgetService·½·¨
            Method getService = ServiceManager.getMethod("getService", java.lang.String.class);

            //µ÷ÓÃgetService»ñÈ¡RemoteService
            Object oRemoteService = getService.invoke(null,Context.POWER_SERVICE);

            //»ñµÃIPowerManager.StubÀà
            Class<?> cStub = Class
                .forName("android.os.IPowerManager$Stub");
            //»ñµÃasInterface·½·¨
            Method asInterface = cStub.getMethod("asInterface", android.os.IBinder.class);
            //µ÷ÓÃasInterface·½·¨»ñÈ¡IPowerManager¶ÔÏó
            Object oIPowerManager = asInterface.invoke(null, oRemoteService);
            //»ñµÃshutdown()·½·¨
            Method shutdown = oIPowerManager.getClass().getMethod("shutdown",boolean.class,boolean.class);
            //µ÷ÓÃshutdown()·½·¨
            shutdown.invoke(oIPowerManager,false,true);
       } catch (Exception e) {
            Log.e("" + e.toString(), e);
       }
    }

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmAlerting",2101, "Page_AlarmAlert_Button_Press_PowerKey_or_VolumeKey_to_Snooze", null, null, null);
                snooze();
            }
        }
    };
    
    private final BroadcastReceiver homePressReceiver = new BroadcastReceiver() {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        final String SYSTEM_DIALOG_REASON_RECENT_APP = "recentapps";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                Log.i("CLOSE_SYSTEM_DIALOGS reason is :" + reason);
                if (reason != null && reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                    snooze();
                }
                if (reason != null && reason.equals(SYSTEM_DIALOG_REASON_RECENT_APP)) {
                    Log.i("SYSTEM_DIALOG_REASON_RECENT_APP");
                    mRecentAPP =  true;
                }
            }
        }
    };

    private void snooze() {
        AlarmStateManager.setSnoozeState(this, mInstance);
    }

    private void dismiss() {
        AlarmStateManager.setDismissState(this, mInstance);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = this;

        //getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED,FLAG_HOMEKEY_DISPATCHED);

        long instanceId = AlarmInstance.getId(getIntent().getData());
        mInstance = AlarmInstance.getInstance(this.getContentResolver(), instanceId);
        Log.v("Displaying alarm for instance: " + mInstance);
        if (mInstance == null) {
            // The alarm got deleted before the activity got created, so just finish()
            Log.v("Error displaying alarm for intent: " + getIntent());
            finish();
            return;
        }

        // Get the volume/camera button behavior setting
        final String vol = SettingsActivity.DEFAULT_VOLUME_BEHAVIOR;

        mVolumeBehavior = Integer.parseInt(vol);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //SystemBarColorManager systemBarManager = new SystemBarColorManager(this);
        //systemBarManager.setViewFitsSystemWindows(this, false);
        //systemBarManager.setStatusBarColor(mContext.getResources().getColor(R.color.alarm_clock_bg));

        // In order to allow tablets to freely rotate and phones to stick
        // with "nosensor" (use default device orientation) we have to have
        // the manifest start with an orientation of unspecified" and only limit
        // to "nosensor" for phones. Otherwise we get behavior like in b/8728671
        // where tablets start off in their default orientation and then are
        // able to freely rotate.
        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
        //updateLayout();
        FrameLayout layout = new FrameLayout(this);
        layout.setId(android.R.id.custom);
        setContentView(layout);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(android.R.id.custom, new AlarmFragment());
        transaction.commit();

        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(homePressReceiver, homeFilter);
        final IntentFilter powerfilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mBatInfoReceiver, powerfilter);
        // Register to get the alarm done/snooze/dismiss intent.
        final IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);

    }


    private void updateTitle() {
        final String titleText = mInstance.getLabelOrDefault(this);
        //TextView tv = (TextView)findViewById(R.id.alertTitle);
        //tv.setText(titleText);
        super.setTitle(titleText);
    }

    private void updateLayout() {
        final LayoutInflater inflater = LayoutInflater.from(this);
        //final View view = inflater.inflate(R.layout.alarm_alert, null);
        final View view = new AliAlarmScreen(this.getApplicationContext());
        view.setId(android.R.id.custom);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(view);
        //updateTitle();
        //Utils.setTimeFormat((TextClock)(view.findViewById(R.id.digitalClock)),
                //(int)getResources().getDimension(R.dimen.bottom_text_size));

        // Setup GlowPadController
        //mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
        //mGlowPadView.setOnTriggerListener(glowPadController);
        //glowPadController.startPinger();
    }

    private void ping() {
        //mGlowPadView.ping();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
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
        TA.getInstance().getDefaultTracker().pageEnter("Page_AlarmAlert");
    }

    @Override
    protected void onPause() {
        super.onPause();
        TA.getInstance().getDefaultTracker().pageLeave("Page_AlarmAlert_leave");
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        Log.v("AlarmActivity - dispatchKeyEvent - " + event.getKeyCode());
        switch (event.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Log.i("dispatchKeyEvent" + event.getKeyCode());
                if(!mHasSnoozeDisplay){
                    mHasSnoozeDisplay = true;
                    TA.getInstance().getDefaultTracker().commitEvent("Page_AlarmAlerting",2101, "Page_AlarmAlert_Button_Press_PowerKey_or_VolumeKey_to_Snooze", null, null, null);
                    snooze();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
                break;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }
    
    @Override
    protected void onDestroy() {
        if(null != mBatInfoReceiver){
            try {
                unregisterReceiver(mBatInfoReceiver);
            } catch (Exception e) {
                Log.e("unregisterReceiver mBatInfoReceiver failure :"+e.getCause());
            }
        }

        if(null != homePressReceiver){
            try {
                unregisterReceiver(homePressReceiver);
            } catch (Exception e) {
                Log.e("unregisterReceiver homePressReceiver failure :"+e.getCause());
            }
        }

        if(null != mReceiver) {
            try {
                unregisterReceiver(mReceiver);
            } catch (Exception e) {
                Log.e("unregisterReceiver mReceiver failure" + e.getCause());
            }
        }
        mHasSnoozeDisplay = false;
        super.onDestroy();
    }
}
