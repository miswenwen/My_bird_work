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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.Settings;

import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.AlarmFeatureOption;
import com.android.deskclock.ClassUtils;

//import com.mediatek.deskclock.ext.ICMCCSpecialSpecExtension;
//import com.mediatek.pluginmanager.Plugin;
//import com.mediatek.pluginmanager.PluginManager;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import android.content.Intent;


/**
 * Manages playing ringtone and vibrating the device.
 */
public class AlarmKlaxon {
    private static final long[] VIBRATE_PATTERN = new long[] { 500, 500 };
    private static final int VIBRATE_LENGTH = 500;
    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;
    private static boolean sStarted = false;
    private static MediaPlayer sMediaPlayer = null;
    public static final boolean CMCC_CUSTOM = "CMCC".equals(SystemProperties.get("ro.yunos.carrier.custom"));
    //private static ICMCCSpecialSpecExtension sICMCCSpecialSpecExtension;

    public static final String ACTION_ENABLE_CLOUD_ALARM = "com.yunos.sync.manager.action.ENABLE_CLOUD_ALARM";
    public static final String ACTION_DISABLE_CLOUD_ALARM = "com.yunos.sync.manager.action.DISABLE_CLOUD_ALARM";
    public static final String EXTRA_KEY_FROM_PACKAGE = "key_from_package";
    public static final String EXTRA_KEY_FROM_AUTHORITY = "key_from_authority";
    private static boolean sCloudAlarm = false;
    private static int ringValCloudOrg;
    private static final String CLOUND_ALARM_AUDIO_PATH = "sdcard/xiaoyun/audio/background";
    public static void stop(Context context) {
        Log.v("AlarmKlaxon.stop()");

        if (sStarted) {
            if(sCloudAlarm && !PowerOffAlarm.bootFromPoweroffAlarm()){
                Intent i = new Intent();
                i.setAction(ACTION_DISABLE_CLOUD_ALARM);
                i.setPackage("com.yunos.assistant");
                i.putExtra(EXTRA_KEY_FROM_PACKAGE, context.getPackageName());
                i.putExtra(EXTRA_KEY_FROM_AUTHORITY, "com.yunos.assistant.cloudalarm");
                i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                context.sendBroadcast(i);
                sCloudAlarm = false;
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if ((audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0)&&(!AlarmFeatureOption.YUNOS_QCOM_PLATFORM)) {
                    Log.v("ringValCloudOrg= "+ ringValCloudOrg + AlarmFeatureOption.YUNOS_QCOM_PLATFORM);
                    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                        am.setStreamVolume(AudioManager.STREAM_ALARM, ringValCloudOrg, AudioManager.ADJUST_SAME);
                    } else {
                        am.setStreamVolume(AudioManager.STREAM_ALARM, ringValCloudOrg, AudioManager.FLAG_ALLOW_RINGER_MODES);
                    }
                }
            }
            sStarted = false;
            // Stop audio playing
            if (sMediaPlayer != null) {
                sMediaPlayer.stop();
                AudioManager audioManager = (AudioManager)
                        context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(null);
                sMediaPlayer.release();
                sMediaPlayer = null;
            }

            ((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
        }
    }

    @SuppressWarnings("PMD")
    public static void start(final Context context, AlarmInstance instance,
            boolean inTelephoneCall) {
        Log.v("AlarmKlaxon.start()");
        // Make sure we are stop before starting
        stop(context);

        /*
         * A: For CMCC, if in call state, just do nothing and return;
         */
        if (inTelephoneCall) {
            if (CMCC_CUSTOM) {
                Log.v("For CMCC, should not affect the call, and do nothing but return.");
                return;
            }
        }
        /*
         * M: If in call state, just vibrate the phone, and don't start the alarm.
         * For CMCC, should not affect the call.
         * vibrate VIBRATE_LENGTH milliseconds. @{
         */
        /*if (inTelephoneCall) {
          ///M: easy porting @{
            if (sICMCCSpecialSpecExtension == null) {
                PluginManager<ICMCCSpecialSpecExtension> pm
                        = PluginManager.<ICMCCSpecialSpecExtension>create(context,
                                ICMCCSpecialSpecExtension.class.getName());
                for (int i = 0,count = pm.getPluginCount();i < count;i++) {
                    Plugin<ICMCCSpecialSpecExtension> plugin = pm.getPlugin(i);
                    try {
                        ICMCCSpecialSpecExtension ext = plugin.createObject();
                        if (ext != null) {
                            sICMCCSpecialSpecExtension = ext;
                            break;
                        }
                    } catch (Plugin.ObjectCreationException ex) {
                        Log.e("can not create plugin object!");
                        ex.printStackTrace();
                    }
                }
            }
            if (sICMCCSpecialSpecExtension != null &&
                    sICMCCSpecialSpecExtension.isCMCCSpecialSpec()) {
                Log.v("CMCC special spec : do not vibrate when in call state ");
            } else {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    Log.v("vibrator starts,and vibrates:" + VIBRATE_LENGTH + " ms");
                    vibrator.vibrate(VIBRATE_LENGTH);
                }
            }
            return;
        }*////@}
        Log.e("mRingtone = " + instance.mRingtone.toString());
        sCloudAlarm = false;
        if(instance.mRingtone.toString().equals(CLOUND_ALARM_AUDIO_PATH) && (!PowerOffAlarm.bootFromPoweroffAlarm())){
            Intent i = new Intent();
            i.setAction(ACTION_ENABLE_CLOUD_ALARM);
            i.setPackage("com.yunos.assistant");
            i.putExtra(EXTRA_KEY_FROM_PACKAGE, context.getPackageName());
            i.putExtra(EXTRA_KEY_FROM_AUTHORITY, "com.yunos.assistant.cloudalarm");
            sCloudAlarm = true;
            context.sendBroadcast(i);
            sMediaPlayer = new MediaPlayer();
            sMediaPlayer.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("Error occurred while playing audio. Stopping AlarmKlaxon.");
                    AlarmKlaxon.stop(context);
                    return true;
                }
            });
            try{
                File file = new File(CLOUND_ALARM_AUDIO_PATH);
                if (!file.exists()) {
                    Uri alarmNoise = Uri.parse(Settings.System.getString(context.getContentResolver(),
                                                     Settings.System.SYSTEM_ALARM_ALERT));
                    if (!AlarmInstance.NO_RINGTONE_URI.equals(alarmNoise)){
                        if (alarmNoise == null) {
                            alarmNoise = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                            if (Log.LOGV) {
                                Log.v("Using default alarm: " + alarmNoise.toString());
                            }
                        }
                    }
                    if (inTelephoneCall) {
                        Log.v("Using the in-call alarm");
                        sMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                        setDataSourceFromResource(context, sMediaPlayer, R.raw.in_call_alarm);
                    } else {
                        sMediaPlayer.setDataSource(context, alarmNoise);
                    }
                }
                else{
                    sMediaPlayer.setDataSource(CLOUND_ALARM_AUDIO_PATH);
                }
                startCloudAlarm(context, sMediaPlayer);
            }
            catch(IOException ex0){
                Log.e("Failed to play the user ringtone==============");
            }
        }


    //if(!sCloudAlarm || PowerOffAlarm.bootFromPoweroffAlarm())
    else{
        /*YUNOS BEGIN*/
        // ##author:nuanyi.lh@alibaba-inc.com ##date:2016.4.26
        // ##BugID:8178843:Resolve the problem of ringtone inconsistent with default after selecting ring on sdcard which uninstalled then.
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM || AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
        /*YUNOS END*/
            //check the file isExist.
            String audio_path = getRealPathFromURI(context, instance.mRingtone);
            if (audio_path != null) {
                File file = new File(audio_path);
                if (!file.exists()) {
                    String sUri = Settings.System.getString(context.getContentResolver(),
                                                     Settings.System.SYSTEM_ALARM_ALERT);
                    if(sUri != null){
                         instance.mRingtone = Uri.parse(sUri);
                    }
                    else{
                         Log.i("media scan error");
                         instance.mRingtone = Uri.parse("content://media/internal/audio/media/14");
                    }
                }
            } else {
                String sUri = Settings.System.getString(context.getContentResolver(),
                                                     Settings.System.SYSTEM_ALARM_ALERT);
                if(sUri != null){
                     instance.mRingtone = Uri.parse(sUri);
                }
                else{
                     Log.i("media scan error");
                     instance.mRingtone = Uri.parse("content://media/internal/audio/media/14");
                }
            }
        }

        if (!AlarmInstance.NO_RINGTONE_URI.equals(instance.mRingtone)) {
            Uri alarmNoise = instance.mRingtone;
            // Fall back on the default alarm if the database does not have an
            // alarm stored.
            if (alarmNoise == null) {
                alarmNoise = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if(alarmNoise == null){
                    alarmNoise = Uri.parse("content://media/internal/audio/media/14");
                }
                if (Log.LOGV) {
                    Log.v("Using default alarm: " + alarmNoise.toString());
                }
            }

            // TODO: Reuse mMediaPlayer instead of creating a new one and/or use RingtoneManager.
            sMediaPlayer = new MediaPlayer();
            sMediaPlayer.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("Error occurred while playing audio. Stopping AlarmKlaxon.");
                    AlarmKlaxon.stop(context);
                    return true;
                }
            });

            try {
                if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                    ///M: if boot from power off alarm and use external ringtone,
                    //just use the backup ringtone to play @{
                    if (PowerOffAlarm.bootFromPoweroffAlarm()
                            && PowerOffAlarm.getNearestAlarmWithExternalRingtone(
                                    context, instance) != null) {
                        setBackupRingtoneToPlay(context);
                    ///@}
                    } else {
                        if (inTelephoneCall) {
                            Log.v("Using the in-call alarm");
                            sMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                            setDataSourceFromResource(context, sMediaPlayer, R.raw.in_call_alarm);
                        } else {
                            sMediaPlayer.setDataSource(context, alarmNoise);
                        }
                    }
                } else {
                    // Check if we are in a call. If we are, use the in-call alarm
                    // resource at a low volume to not disrupt the call.
                    if (inTelephoneCall) {
                        Log.v("Using the in-call alarm");
                        sMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                        setDataSourceFromResource(context, sMediaPlayer, R.raw.in_call_alarm);
                    } else {
                        sMediaPlayer.setDataSource(context, alarmNoise);
                    }
                }
                startAlarm(context, sMediaPlayer);
            } catch (IOException ex1) {
                Log.e("Failed to play the user ringtone", ex1);
                // The alarmNoise may be on the sd card which could be busy right
                // now. Use the default ringtone.
                try {
                    // Must reset the media player to clear the error state.
                    sMediaPlayer.reset();
                    ///M: change the fallback ringtone to defualt
                    Uri defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    sMediaPlayer.setDataSource(context, defaultRingtone);
                    startAlarm(context, sMediaPlayer);
                } catch (IOException ex2) {
                    // At this point we just don't play anything.
                    Log.e("Failed to play the default ringtone", ex2);
                    try {
                        // Must reset the media player to clear the error state.
                        sMediaPlayer.reset();
                        ///M: default ringtone play error, use the fallback ringtone
                        setDataSourceFromResource(context, sMediaPlayer, R.raw.fallbackring);
                        startAlarm(context, sMediaPlayer);
                    } catch (IOException ex3) {
                        // At this point we just don't play anything.
                        Log.e("Failed to play fallback ringtone", ex3);
                    }
                }
            }
        }
    }
        if (instance.mVibrate) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_PATTERN, 0);
        }

        sStarted = true;
    }

    private static void startCloudAlarm(final Context context, MediaPlayer player) throws IOException {
        Log.v("startAlarm, check StreamVolume and requestAudioFocus");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0 (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            ringValCloudOrg = am.getStreamVolume(AudioManager.STREAM_ALARM);
            if(!AlarmFeatureOption.YUNOS_QCOM_PLATFORM){
                if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, 1, AudioManager.ADJUST_SAME);
                    } else {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, 1, AudioManager.FLAG_ALLOW_RINGER_MODES);
                }
            }

            Log.i("ringValCloudOrg is"+ringValCloudOrg);
            int ringValCloud = ringValCloudOrg - 5;
            if(ringValCloud < 1){
                ringValCloud = 1;
            }
            final int ringVal = ringValCloud;
            Log.i("ringVal2 is"+ringVal);
            Timer timer = new Timer();
            long timeStep = (long)(15000/ringVal);
            for (int i=0; i < ringVal; i++) {
                TimerTask task = new TimerTask(){
                    public void run(){
                        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                        if ((am.getStreamVolume(AudioManager.STREAM_ALARM) < ringVal)&&(!AlarmFeatureOption.YUNOS_QCOM_PLATFORM)) {
                            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                                am.adjustStreamVolume(AudioManager.STREAM_ALARM,AudioManager.ADJUST_RAISE,AudioManager.ADJUST_SAME);
                            } else {
                                am.adjustStreamVolume(AudioManager.STREAM_ALARM,AudioManager.ADJUST_RAISE,AudioManager.FLAG_ALLOW_RINGER_MODES);
                            }
                        }
                    }
                };
                timer.schedule(task, i * timeStep);
            }

            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            Log.i("ringVal3 is"+ringVal);
            audioManager.requestAudioFocus(null,
            AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            player.start();
			Log.i("ClockShutdown,AlarmKlaxon MediaPlayer.start() )");//liuqipeng add
            Log.d("Play successful, StreamVolume != 0");
        }
    }

    // Do the common stuff when starting the alarm.
    private static void startAlarm(final Context context, MediaPlayer player) throws IOException {
        Log.v("startAlarm, check StreamVolume and requestAudioFocus");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0 (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            final int ringVal = am.getStreamVolume(AudioManager.STREAM_ALARM);
            /*YUNOS BEGIN*/
            // ##author:nuanyi.lh@alibaba-inc.com ##date:2016.5.10
            // ##BugID:8223541:Sound changed from meeting/silent to general after alarming.
            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM || AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, 1, AudioManager.ADJUST_SAME);
            } else {
                am.setStreamVolume(AudioManager.STREAM_ALARM, 1, AudioManager.FLAG_ALLOW_RINGER_MODES);
            }
            /*YUNOS END*/

            Log.i("ringVal is"+ringVal);
            Timer timer = new Timer();
            long timeStep = (long)(10000/ringVal);
            for (int i=0; i < ringVal; i++) {
                TimerTask task = new TimerTask(){
                    public void run(){
                        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                        if (am.getStreamVolume(AudioManager.STREAM_ALARM) < ringVal) {
                            /*YUNOS BEGIN*/
                            // ##author:nuanyi.lh@alibaba-inc.com ##date:2016.5.10
                            // ##BugID:8223541:Sound changed from meeting/silent to general after alarming.
                            if (AlarmFeatureOption.YUNOS_MTK_PLATFORM || AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
                                am.adjustStreamVolume(AudioManager.STREAM_ALARM,AudioManager.ADJUST_RAISE,AudioManager.ADJUST_SAME);
                            } else {
                                am.adjustStreamVolume(AudioManager.STREAM_ALARM,AudioManager.ADJUST_RAISE,AudioManager.FLAG_ALLOW_RINGER_MODES);
                            }
                            /*YUNOS END*/
                        }
                    }
                };
                timer.schedule(task, i * timeStep);
            }

            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
			Log.i("ClockShutdown,AlarmKlaxon MediaPlayer.setLooping(true)");//liuqipeng add
            player.prepare();
            audioManager.requestAudioFocus(null,
                    AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            player.start();
			Log.i("ClockShutdown,AlarmKlaxon MediaPlayer.start() )");//liuqipeng add
            Log.d("Play successful, StreamVolume != 0");
        }
    }

    private static String getRealPathFromURI(final Context context, Uri contentURI) {
        String result = null;
        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            int index = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
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
        return result;
    }

    private static void setDataSourceFromResource(Context context, MediaPlayer player, int res)
            throws IOException {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }

    /** M: @{
     * set the backup ringtone for power off alarm to play
     */
    private static void setBackupRingtoneToPlay(Context context) throws IOException {
        String ringtonePath = null;
        java.io.File dir = context.getFilesDir();
        Log.v("base dir: " + dir.getAbsolutePath());
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            ringtonePath = files[0].getAbsolutePath();
        }
        Log.v("setBackupRingtoneToPlay ringtone: " + ringtonePath);
        if (!TextUtils.isEmpty(ringtonePath)) {
            File file = new File(ringtonePath);
            if (file != null && file.exists() && file.getTotalSpace() > 0) {
                java.io.FileInputStream fis = null;
                try {
                    fis = new java.io.FileInputStream(file);
                    sMediaPlayer.setDataSource(fis.getFD());
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            }
        }
    }
}
