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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.Log;
import com.android.deskclock.provider.AlarmInstance;
import com.android.internal.telephony.ITelephony;

//import com.mediatek.common.featureoption.FeatureOption;
//import com.mediatek.telephony.TelephonyManagerEx;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.media.AudioManager;
import com.android.deskclock.AlarmFeatureOption;
import com.android.deskclock.ClassUtils;

import android.os.Handler;



/**
 * This service is in charge of starting/stoping the alarm. It will bring up and manage the
 * {@link AlarmActivity} as well as {@link AlarmKlaxon}.
 */
public class AlarmService extends Service {
    // A public action send by AlarmService when the alarm has started.
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";

    // A public action sent by AlarmService when the alarm has stopped for any reason.
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

    // Private action used to start an alarm with this service.
    public static final String START_ALARM_ACTION = "START_ALARM";
    private static final String DEFAULT_SNOOZE_MINUTES = "10";

    // Private action used to stop an alarm with this service.
    public static final String STOP_ALARM_ACTION = "STOP_ALARM";

    /// M: Stop the alarm alert when the device shut down.
    public static final String PRE_SHUTDOWN_ACTION = "android.intent.action.ACTION_PRE_SHUTDOWN";

    /// M: Power off alarm start and stop deskclock play ringtone. @{
    private static final String NORMAL_SHUTDOWN_ACTION = "android.intent.action.normal.shutdown";
    private static final String ALARM_REQUEST_SHUTDOWN_ACTION = "android.intent.action.ACTION_ALARM_REQUEST_SHUTDOWN";

    private static final String POWER_OFF_ALARM_START_ACITION = "com.android.deskclock.START_ALARM";
    private static final String POWER_OFF_ALARM_POWER_ON_ACITION = "com.android.deskclock.POWER_ON_ALARM";
    private static final String POWER_OFF_ALARM_DISMISS_ACITION = "com.android.deskclock.DISMISS_ALARM";
    public static final String POWER_OFF_ALARM_SNOOZE_ACITION = "com.android.deskclock.SNOOZE_ALARM";
    public static final String POWER_OFF_ALARM_MISSED_ACITION = "com.android.deskclock.MISSED_ALARM";
    /// @}
    public static int sSnoozeInterval = 0;

    private Handler mH = new Handler();

    private final BroadcastReceiver mStopPlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("AlarmService mStopPlayReceiver: " + intent.getAction());
            if (mCurrentAlarm == null) {
                Log.v("mStopPlayReceiver mCurrentAlarm is null, just return");
                return;
            }
            /// M: Send by the PowerOffAlarm AlarmAlertFullScreen, user drag the icon or time out
            if (intent.getAction().equals(POWER_OFF_ALARM_SNOOZE_ACITION)) {
                sSnoozeInterval = (int) (intent.getIntExtra("snooze_interval", 600000) / 60000);
                Log.v("sSnoozeInterval = " + sSnoozeInterval);
                AlarmStateManager.setSnoozeState(context, mCurrentAlarm);
                shutDown(context);
            } else if (intent.getAction().equals(POWER_OFF_ALARM_MISSED_ACITION)) {
                AlarmStateManager.setMissedStateForPoweroffAlarm(context, mCurrentAlarm);
                shutDown(context);
            } else {
                /// M: Power on action or pre_shutdown, so set dismiss state and don't shut down
                AlarmStateManager.setDismissState(context, mCurrentAlarm);
                /// M: Send by the PowerOffAlarm AlarmAlertFullScreen, set dismiss state and shut down
                if (intent.getAction().equals(POWER_OFF_ALARM_DISMISS_ACITION)) {
                    shutDown(context);
                }
            }
        }
    };

    /*YUNOS BEGIN*/
    // ##Module(Alarm) ##Author:nuanyi.lh ##Date:2016.4.22
    // ##BugID:8065967:Solve the problem of alarm still vibrating when shutdown and alarming again when reboot.
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("AlarmService mShutdownReceiver: " + intent.getAction());
            if (mCurrentAlarm == null) {
                Log.v("mShutdownReceiver mCurrentAlarm is null, just return");
                return;
            }
            if (intent.getAction().equals(Intent.ACTION_SHUTDOWN) || intent.getAction().equals(Intent.ACTION_REBOOT)) {
                AlarmStateManager.setDismissState(context, mCurrentAlarm);
            }
        }
    };
    /*YUNOS END*/
    /**
     * Utility method to help start alarm properly. If alarm is already firing, it
     * will mark it as missed and start the new one.
     *
     * @param context application context
     * @param instance to trigger alarm
     */
    public static void startAlarm(Context context, AlarmInstance instance) {
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(START_ALARM_ACTION);

        Log.i("why did this start twice ?");
        // Maintain a cpu wake lock until the service can get it
        AlarmAlertWakeLock.acquireCpuWakeLock(context);
        context.startService(intent);
    }

    /**
     * Utility method to help stop an alarm properly. Nothing will happen, if alarm is not firing
     * or using a different instance.
     *
     * @param context application context
     * @param instance you are trying to stop
     */
    public static void stopAlarm(Context context, AlarmInstance instance) {
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(STOP_ALARM_ACTION);

        // We don't need a wake lock here, since we are trying to kill an alarm
        context.startService(intent);
    }

    /// M: Gemini support 4 sim card @{
    private static final int GIMINI_SIM_1 = 0;
    private static final int GIMINI_SIM_2 = 1;
    private static final int GIMINI_SIM_3 = 2;
    private static final int GIMINI_SIM_4 = 3;
    private ITelephony mTelephonyService;
    private TelephonyManager mTelephonyManager;
    //private TelephonyManagerEx mTelephonyManagerEx;
    /// @}
    private int mInitialCallState;
    private AlarmInstance mInstance = null;
    private AlarmInstance mCurrentAlarm = null;
    private AlarmInstance mInstanceAlarm = null;
    private Context mContext = null;

    @SuppressWarnings("PMD")
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            int newPhoneState = mInitialCallState;
            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                if (mTelephonyService != null) {
                    try {
                        Object object = ClassUtils.invokeMethod(mTelephonyService, "getPreciseCallState");
                        newPhoneState = Integer.parseInt(String.valueOf(object));;
                    } catch (Exception ex) {
                        newPhoneState = mTelephonyManager.getCallState();
                        Log.v("Catch exception when getPreciseCallState: ex = "
                                + ex.getMessage());
                    }
                } else {
                    newPhoneState = mTelephonyManager.getCallState();
                }
            } else {
                newPhoneState = mTelephonyManager.getCallState();
            }
            Log.v("mTelephonyManager.getCallState() = " + newPhoneState);
            if (mCurrentAlarm == null) {
                Log.v("onStateChange mCurrentAlarm is null, just return");
                return;
            }
            // The user might already be in a call when the alarm fires. When
            // we register onCallStateChanged, we get the initial in-call state
            // which kills the alarm. Check against the initial call state so
            // we don't kill the alarm during a call.
            if (state != TelephonyManager.CALL_STATE_IDLE
                    && mInitialCallState == TelephonyManager.CALL_STATE_IDLE) {
                Log.v("AlarmService onCallStateChanged sendBroadcast to Missed alarm");
                sendBroadcast(AlarmStateManager.createStateChangeIntent(AlarmService.this,
                        "AlarmService", mCurrentAlarm, AlarmInstance.MISSED_STATE));
            }

            /// M: If the state change to CALL_STATE_IDLE, it means the user havn't in the call @{
            if (newPhoneState == TelephonyManager.CALL_STATE_IDLE
                    && state == TelephonyManager.CALL_STATE_IDLE && state != mInitialCallState) {
                Log.v("AlarmService onCallStateChanged user hung up the phone");
                /// M: If the alarm has been dismissed by user, shouldn't restart the alarm
                if (mInstanceAlarm.mAlarmState == AlarmInstance.FIRED_STATE) {
                    Log.v("AlarmService AlarmFiredState startAlarm");
                    mCurrentAlarm = null;
                    startAlarm(mContext, mInstanceAlarm);
                }
            }
            Log.v("AlarmService onCallStateChanged state = " + state
                    + ", mInitialCallState = " + mInitialCallState);
            /// @}
        }
    };

    private void startAlarmKlaxon(AlarmInstance instance) {
        Log.v("AlarmService.start with instance: " + instance.mId);

        boolean stopPrev = false;
        if (mCurrentAlarm != null) {
            AlarmStateManager.setMissedState(this, mCurrentAlarm);
            stopCurrentAlarm();
            stopPrev = true;
        }

        AlarmAlertWakeLock.acquireCpuWakeLock(this);
        mCurrentAlarm = instance;
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            initTelephonyService();
        }
        /// M: Check if the device is gemini supported 4 sim card
        //if (FeatureOption.MTK_GEMINI_SUPPORT) {
        //    mTelephonyManagerEx.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE
        //            | PhoneStateListener.LISTEN_SERVICE_STATE, GIMINI_SIM_1);
        //    mTelephonyManagerEx.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE
        //            | PhoneStateListener.LISTEN_SERVICE_STATE, GIMINI_SIM_2);
       //} else {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
            mInitialCallState = mTelephonyManager.getCallState();
        }
       //}
        boolean inCall = mInitialCallState != TelephonyManager.CALL_STATE_IDLE;
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            /// M: If boot from power off alarm, don't show the notification and alarmActivity @{
            if (!PowerOffAlarm.bootFromPoweroffAlarm()) {
            /* M:If user is in call, just show high priority notification,
             * otherwise show Alarm Notification with AlarmActivity
             */
                if (AlarmKlaxon.CMCC_CUSTOM && inCall) {
                    mInstanceAlarm = mCurrentAlarm;
                    AlarmNotifications.showHighPriorityNotification(this, mCurrentAlarm);
                } else {
                    /*YUNOS BEGIN PB*/
                    //##module: AliDeskClock ##author: haibo.yhb@alibaba-inc.com
                    //##BugID:(6237980) ##date: 2015-08-13 11:10:10
                    //##description: Avoid race condition between stop previous
                    //##            alarm and start of current alarm
                    if (stopPrev) {
                       mH.postDelayed(new Runnable() {
                          public void run() {
                              AlarmNotifications.showAlarmNotification(AlarmService.this, mCurrentAlarm);
                          }
                       }, 200);
                    } else {
                       AlarmNotifications.showAlarmNotification(AlarmService.this, mCurrentAlarm);
                    }
                    /*YUNOS END PB*/
                }
                mInstanceAlarm = mCurrentAlarm;
                //AlarmNotifications.showAlarmNotification(this, mCurrentAlarm);
            }/// @}
        } else {
            /*if (inCall) {
                //mInstanceAlarm = mCurrentAlarm;
                AlarmNotifications.showHighPriorityNotification(this, mCurrentAlarm);
            } else {*/
                /*YUNOS BEGIN PB*/
                //##module: AliDeskClock ##author: haibo.yhb@alibaba-inc.com
                //##BugID:(6237980) ##date: 2015-08-13 11:10:10
                //##description: Avoid race condition between stop previous
                //##            alarm and start of current alarm
                if (stopPrev) {
                    mH.postDelayed(new Runnable() {
                       public void run() {
                          AlarmNotifications.showAlarmNotification(AlarmService.this, mCurrentAlarm);
                       }
                    }, 200);
                } else {
                    AlarmNotifications.showAlarmNotification(AlarmService.this, mCurrentAlarm);
                }
                /*YUNOS END PB*/
            mInstanceAlarm = mCurrentAlarm;
        }

        AlarmKlaxon.start(this, mCurrentAlarm, inCall);
        sendBroadcast(new Intent(ALARM_ALERT_ACTION));
    }

    private void stopCurrentAlarm() {
        if (mCurrentAlarm == null) {
            Log.v("There is no current alarm to stop");
            return;
        }

        Log.v("AlarmService.stop with instance: " + mCurrentAlarm.mId);
        AlarmKlaxon.stop(this);
        /// M: Stop listening for incoming calls.
        /*if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mTelephonyManagerEx.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE, GIMINI_SIM_1);
            mTelephonyManagerEx.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE, GIMINI_SIM_2);
            mTelephonyManagerEx.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE, GIMINI_SIM_3);
            mTelephonyManagerEx.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE, GIMINI_SIM_4);
        } else {*/
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        //}

        sendBroadcast(new Intent(ALARM_DONE_ACTION));
        mCurrentAlarm = null;
        AlarmAlertWakeLock.releaseCpuLock();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            /// M: Add for gemini supported, TelephoneyManager just support 2 sim card @{
            mTelephonyService = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            //mTelephonyManagerEx = TelephonyManagerEx.getDefault();
            /// @}
        }
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mContext = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("pengelei AlarmService.onStartCommand() with intent: " + intent.toString());
        long instanceId = -1;
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            /// M: check if it's boot from power off alarm or not
            boolean isAlarmBoot = intent.getBooleanExtra("isAlarmBoot", false);
            Log.v("AlarmService isAlarmBoot = " + isAlarmBoot);
            IntentFilter filter = new IntentFilter();
            if (PowerOffAlarm.bootFromPoweroffAlarm()) {
                /// M: add the power off alarm snooze\dismiss\power_on action @{
                filter.addAction(POWER_OFF_ALARM_POWER_ON_ACITION);
                filter.addAction(POWER_OFF_ALARM_SNOOZE_ACITION);
                filter.addAction(POWER_OFF_ALARM_DISMISS_ACITION);
                filter.addAction(POWER_OFF_ALARM_MISSED_ACITION);
            } else {
                /// M: add for DeskClock to dismiss the alarm when preShutDown
                filter.addAction(PRE_SHUTDOWN_ACTION);
            }
            registerReceiver(mStopPlayReceiver, filter);
            /// @}
            if (!isAlarmBoot) {
                instanceId = AlarmInstance.getId(intent.getData());
            }
            Log.v("AlarmService instanceId = " + instanceId);
            if (START_ALARM_ACTION.equals(intent.getAction())
                || POWER_OFF_ALARM_START_ACITION.equals(intent.getAction())) {

                /// M: check if it's boot from power off alarm or not @{
                if (isAlarmBoot) {
                    mInstance = AlarmStateManager.getNearestAlarm(mContext);
                    if (mInstance != null) {
                        AlarmStateManager.setFiredState(mContext, mInstance);
                    }
                /// @}
                } else {
                    ContentResolver cr = this.getContentResolver();
                    mInstance = AlarmInstance.getInstance(cr, instanceId);
                }
                Log.v("AlarmService instance = " + mInstance);

                if (mInstance == null) {
                    Log.e("No instance found to start alarm: " + instanceId);
                    if (mCurrentAlarm != null) {
                        // Only release lock if we are not firing alarm
                        AlarmAlertWakeLock.releaseCpuLock();
                    }
                    return Service.START_NOT_STICKY;
                } else if (mCurrentAlarm != null) {
                    if (mCurrentAlarm.mId == mInstance.mId) {
                        Log.e("Alarm already started for instance: " + instanceId);
                        return Service.START_NOT_STICKY;
                    } else if (mCurrentAlarm.getAlarmTime().getTimeInMillis()
                            == mInstance.getAlarmTime().getTimeInMillis()) {
                        Log.v("The same time alarm playing, so missed this instance");
                        AlarmStateManager.setMissedState(mContext, mInstance);
                        return Service.START_NOT_STICKY;
                    }
                }
                /// M: PowerOffAlarm start and change the label @{
                if (PowerOffAlarm.bootFromPoweroffAlarm()) {
                    updatePoweroffAlarmLabel(this, mInstance.mLabel);
                }
                /// @}
                startAlarmKlaxon(mInstance);
            } else if (STOP_ALARM_ACTION.equals(intent.getAction())) {
                Log.v("get the broadcast to stop the ringtone");
                if (mCurrentAlarm != null && mCurrentAlarm.mId != instanceId) {
                    Log.e("Can't stop alarm for instance: " + instanceId +
                        " because current alarm is: " + mCurrentAlarm.mId);
                    return Service.START_NOT_STICKY;
                }
                stopSelf();
            }
        } else {
            /*YUNOS BEGIN*/
            // ##Module(Alarm) ##Author:nuanyi.lh ##Date:2016.4.22
            // ##BugID:8065967:Solve the problem of alarm still vibrating when shutdown and alarming again when reboot.
            if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_SHUTDOWN);
                filter.addAction(Intent.ACTION_REBOOT);
                registerReceiver(mShutdownReceiver, filter);
            }
            /*YUNOS END*/
            instanceId = AlarmInstance.getId(intent.getData());
            Log.v("AlarmService instanceId = " + instanceId);
            if (START_ALARM_ACTION.equals(intent.getAction())) {

                ContentResolver cr = this.getContentResolver();
                mInstance = AlarmInstance.getInstance(cr, instanceId);
                Log.v("AlarmService instance = " + mInstance);
                if (mInstance == null) {
                    Log.e("No instance found to start alarm: " + instanceId);
                    if (mCurrentAlarm != null) {
                        // Only release lock if we are not firing alarm
                        AlarmAlertWakeLock.releaseCpuLock();
                    }
                    return Service.START_NOT_STICKY;
                } else if (mCurrentAlarm != null) {
                    if (mCurrentAlarm.mId == mInstance.mId) {
                        Log.e("Alarm already started for instance: " + instanceId);
                        return Service.START_NOT_STICKY;
                    } else if (mCurrentAlarm.getAlarmTime().getTimeInMillis()
                            == mInstance.getAlarmTime().getTimeInMillis()) {
                        Log.v("The same time alarm playing, so missed this instance");
                        AlarmStateManager.setMissedState(mContext, mInstance);
                        return Service.START_NOT_STICKY;
                    }
                }
                startAlarmKlaxon(mInstance);
            } else if (STOP_ALARM_ACTION.equals(intent.getAction())) {
                Log.v("get the broadcast to stop the ringtone");
                if (mCurrentAlarm != null && mCurrentAlarm.mId != instanceId) {
                    Log.e("Can't stop alarm for instance: " + instanceId +
                        " because current alarm is: " + mCurrentAlarm.mId);
                    return Service.START_NOT_STICKY;
                }
                stopSelf();
            }
        }


        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v("AlarmService.onDestroy() called");
        stopCurrentAlarm();
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            /// M: unregister the power off alarm snooze\dismiss\power_on receiver @{
            unregisterReceiver(mStopPlayReceiver);
            /// @}
        /*YUNOS BEGIN*/
        // ##Module(Alarm) ##Author:nuanyi.lh ##Date:2016.4.22
        // ##BugID:8065967:Solve the problem of alarm still vibrating when shutdown and alarming again when reboot.
        } else if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
            unregisterReceiver(mShutdownReceiver);
        }
        /*YUNOS END*/
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**M: @{
     * update power off alarm label
     */
    private void updatePoweroffAlarmLabel(Context context, String label) {
        Intent intent = new Intent("update.power.off.alarm.label");
        intent.putExtra("label", (label == null ? "" : label));
        context.sendBroadcast(intent);
    }

    /**M: @{
     * shut down the device
     */
    private void shutDown(Context context) {
        // send normal shutdown broadcast
        Intent shutdownIntent = new Intent(NORMAL_SHUTDOWN_ACTION);
        context.sendBroadcast(shutdownIntent);

        // shutdown the device
        Intent intent = new Intent(ALARM_REQUEST_SHUTDOWN_ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /// M: Record the initial call state.
    private void initTelephonyService() {
        if (mTelephonyService == null) {
            mInitialCallState = mTelephonyManager.getCallState();
        } else {
            try {
                Object object = ClassUtils.invokeMethod(mTelephonyService, "getPreciseCallState");
                mInitialCallState = Integer.parseInt(String.valueOf(object));
            } catch (Exception ex) {
                mInitialCallState = mTelephonyManager.getCallState();
                Log.v("Catch exception when getPreciseCallState: ex = "
                    + ex.getMessage());
            }
        }
    }
}
