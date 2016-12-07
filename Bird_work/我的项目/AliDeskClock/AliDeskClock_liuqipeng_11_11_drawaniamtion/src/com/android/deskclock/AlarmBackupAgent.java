package com.android.deskclock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.Context;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;


import com.android.deskclock.alarms.AlarmStateManager;
//import com.aliyun.theme.common.util.StringUtils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CityObj;


/**
 * Data back up and restore
 *
 * @author liuxiao.lx
 *
 * @update lei.penglei 2014.4.16 mail: lei.penglei@alibaba-inc.com
 */
public class AlarmBackupAgent extends BackupAgent {

    private static final String ALI_CLOCKLABLE_BACKUP_KEY = "ALI_CLOCKLABLE_BACKUP_KEY";
    private static final String ALI_ALARMCLOCK_BACKUP_KEY = "ALI_ALARMCLOCK_BACKUP_KEY";
    private static final String TAG = "AlarmBackupAgent";
    private static final int specialTag = 13000;
    private List<CityObj> mUserSelectedCities;
    private List<String> mUserSelectedKeys;
    private List<Alarm> allAlarms;
    private boolean isOlderVersionData = false;
    private boolean mDisplayLable = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) throws IOException {
        // DO Backup
        Log.i("alarm","backup start now");

        ByteArrayOutputStream bufLableStream = new ByteArrayOutputStream();
        DataOutputStream outLableWriter = new DataOutputStream(bufLableStream);
        outLableWriter.writeBoolean(true);
        byte[] bufferLable = bufLableStream.toByteArray();
        data.writeEntityHeader(ALI_CLOCKLABLE_BACKUP_KEY, bufferLable.length);
        data.writeEntityData(bufferLable, bufferLable.length);
        outLableWriter.close();

        ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
        DataOutputStream outWriter = new DataOutputStream(bufStream);

        // The order of backing up MUST be the same as restoring
        backupWorldClockInfo(outWriter);
        backupAlarmInfo(outWriter);

        // Send the data to the Backup Manager via the BackupDataOutput
        byte[] buffer = bufStream.toByteArray();
        outWriter.close();

        // Whether data changed
        FileInputStream instream = new FileInputStream(
                oldState.getFileDescriptor());
        DataInputStream in = new DataInputStream(instream);
        String dataMd5 = md5(buffer);
        try {
            String oldStateMd5 = in.readUTF();
            in.close();
            //if (!StringUtils.isEmpty(oldStateMd5)
            //		&& !StringUtils.isEmpty(dataMd5)
            if (!(oldStateMd5 == null || oldStateMd5.length() == 0)
                    && !(dataMd5 == null || dataMd5.length() == 0)
                    && !oldStateMd5.equalsIgnoreCase(dataMd5)) {
                performBackup(data, buffer, newState, dataMd5);
            } else {
                Log.d(TAG, String.format(
                        "Don't Back up, Old state MD5:[%s], Data MD5:[%s]",
                        oldStateMd5, dataMd5));
                setNewState(newState, dataMd5);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Test Back up MD5 Error");
            performBackup(data, buffer, newState, dataMd5);
        }
        Log.i("alarm","backup finish now");
    }

    /**
     * Perform back up and set newState
     * @param data
     * @param buffer
     * @param newState
     * @param dataMd5
     * @throws IOException
     */
    private void performBackup(BackupDataOutput data, byte[] buffer,
            ParcelFileDescriptor newState, String dataMd5) throws IOException {
        performBackup(data, buffer);
        setNewState(newState, dataMd5);
    }

    /**
     *
     * @param data
     * @param buffer
     * @throws IOException
     */
    private void performBackup(BackupDataOutput data, byte[] buffer)
            throws IOException {
        Log.d(TAG, "Performing BACKUP!");
        int len = buffer.length;
        data.writeEntityHeader(ALI_ALARMCLOCK_BACKUP_KEY, len);
        data.writeEntityData(buffer, len);
    }

    /**
     * Put md5 into newState
     *
     * @param newState
     * @param dataMd5
     * @throws IOException
     */
    private void setNewState(ParcelFileDescriptor newState, String dataMd5)
            throws IOException {
        FileOutputStream outstream = new FileOutputStream(
                newState.getFileDescriptor());
        DataOutputStream out = new DataOutputStream(outstream);
        out.writeUTF(dataMd5);
        out.close();
    }

    /**
     * Write world clock information
     *
     * @param outWriter
     * @throws IOException
     */
    private void backupWorldClockInfo(DataOutputStream outWriter)
            throws IOException {
        mUserSelectedCities = hashmapToList(Cities.readCitiesFromSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(this)));
        //special flag for alarm to seperate the data from 2.7 alarm data
        int cityNum = mUserSelectedCities.size();
        outWriter.writeInt(specialTag);
        outWriter.writeInt(cityNum);
        for (CityObj city:mUserSelectedCities) {
            outWriter.writeUTF(city.mCityName);
            outWriter.writeUTF(city.mTimeZone);
            outWriter.writeUTF(city.mCityId);
        }
    }

    private List<CityObj> hashmapToList(HashMap<String, CityObj> map) {
        mUserSelectedKeys = new ArrayList<String>();
        List<CityObj> listValue = new ArrayList<CityObj>();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            mUserSelectedKeys.add(key);
            listValue.add(map.get(key));
        }
        return listValue;
    }

    private HashMap<String, CityObj> listToHashmap(List<String> listkey, List<CityObj> listValue) {
        HashMap<String, CityObj> result = new HashMap<String, CityObj>();
        for(int i = 0; i < listkey.size(); i++) {
            result.put(listkey.get(i), listValue.get(i));
        }
        return result;
    }

    /**
     * Write alarm information
     *
     * @param outWriter
     * @throws IOException
     */
    private void backupAlarmInfo(DataOutputStream outWriter) throws IOException {
        String[] selectionArgs = {};
        allAlarms = Alarm.getAlarms(getContentResolver(), null, selectionArgs);
        int alarmNum = allAlarms.size();
        outWriter.writeInt(alarmNum);
        for (Alarm alarm:allAlarms) {
            Log.d(TAG,
                    String.format(
                            "Set backup data, Time zone:[%s,%s,%s,%s,%s]",
                            alarm.daysOfWeek, alarm.enabled,
                            alarm.hour, alarm.minutes, alarm.alert));
            outWriter.writeInt(alarm.daysOfWeek.getBitSet());
            outWriter.writeBoolean(alarm.enabled);
            outWriter.writeBoolean(alarm.vibrate);
            outWriter.writeInt(alarm.hour);
            outWriter.writeInt(alarm.minutes);
            outWriter.writeUTF(alarm.label.toString());
            outWriter.writeUTF(alarm.alert.toString());
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
        ParcelFileDescriptor newState) throws IOException {
        Log.e("alarm","start restore now");
        while (data.readNextHeader()) {
            Log.e("alarm","start restore now readNextHeader");
            String key = data.getKey();
            Log.d(TAG,"key = "+key);
            int dataSize = data.getDataSize();
            if (ALI_CLOCKLABLE_BACKUP_KEY.equals(key)) {
                byte[] dataBuf = new byte[dataSize];
                data.readEntityData(dataBuf, 0, dataSize);
                ByteArrayInputStream baStream = new ByteArrayInputStream(
                        dataBuf);
                DataInputStream in = new DataInputStream(baStream);
                mDisplayLable = in.readBoolean();
                Log.d(TAG,"mDisplayLable = "+mDisplayLable);
                in.close();
            } else if (ALI_ALARMCLOCK_BACKUP_KEY.equals(key)) {
                byte[] dataBuf = new byte[dataSize];
                data.readEntityData(dataBuf, 0, dataSize);
                ByteArrayInputStream baStream = new ByteArrayInputStream(
                        dataBuf);
                DataInputStream in = new DataInputStream(baStream);

                restoreWorldClockInfo(in);
                if (isOlderVersionData) {
                    restoreAlarmInfoOlderVersion(in);
                } else {
                    restoreAlarmInfo(in);
                }

                in.close();
            } else {
                data.skipEntityData();
            }
        }
        Log.i("AlarmClock","finish restore now");
    }

    /**
     * Read and set world clock information
     * @param in
     * @throws IOException
     */
    private void restoreWorldClockInfoOlderVersion(DataInputStream in, int worldClockNum) throws IOException {
        // do nothing bug consume the data
        for (int i = 0; i < worldClockNum; i++) {
            String timeZoneId = in.readUTF();
            Log.d(TAG, String.format("Performing RESTORE! Time zone:[%s]",
                    timeZoneId));
        }
    }

    /**
     * Read and set world clock information
     *
     * @param in
     * @throws IOException
     */
    private void restoreWorldClockInfo(DataInputStream in) throws IOException {
        int worldClockNum = in.readInt();
        if (specialTag != worldClockNum) {
            isOlderVersionData = true;
            restoreWorldClockInfoOlderVersion(in, worldClockNum);
            return ;
        }

        /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isFirstTimeLoaded", false);
        prefs.edit();
        editor.apply();
        boolean isFirstTimeloaded = prefs.getBoolean("isFirstTimeLoaded", true);*/
        mUserSelectedCities = hashmapToList(Cities.readCitiesFromSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(this)));

        Iterator<CityObj> itr = mUserSelectedCities.iterator();
        Iterator<String> itrString = mUserSelectedKeys.iterator();
        while (itr.hasNext()) {
            CityObj i = itr.next();
            String s = itrString.next();
            itr.remove();
            itrString.remove();
        }

        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()),
                listToHashmap(mUserSelectedKeys, mUserSelectedCities));

        // the real worldClockNum
        worldClockNum = in.readInt();
        Log.e("alarm","start restore world count"+worldClockNum);
        HashMap<String, CityObj> selectedCities = Cities.readCitiesFromSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(this));
        for (int i = 0; i < worldClockNum; i++) {
            CityObj c = new CityObj(in.readUTF(),in.readUTF(),in.readUTF());
            selectedCities.put(c.mCityId, c);
        }
        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this), selectedCities);
    }

    /**
     * Read and set alarm information
     *
     * @param in
     * @throws IOException
     */
    private void restoreAlarmInfo(DataInputStream in) throws IOException {
        // Clear old alarms
        String[] selectionArgs = {};
        allAlarms = Alarm.getAlarms(getContentResolver(), null, selectionArgs);
        for (Alarm alarm:allAlarms) {
            Alarm.deleteAlarm(getContentResolver(), alarm.id);
        }

        int alarmNum = in.readInt();
        Log.i("AlarmClock","start restore alarm count"+alarmNum);
        for (int i = 0; i < alarmNum; i++) {
            Alarm alarm = new Alarm();
            alarm.daysOfWeek = new DaysOfWeek(in.readInt());
            alarm.enabled = in.readBoolean();
            alarm.vibrate = in.readBoolean();
            alarm.hour = in.readInt();
            alarm.minutes = in.readInt();
            if (mDisplayLable) {
                alarm.label = in.readUTF();
            }
            alarm.alert = Uri.parse(in.readUTF());
            Log.d(TAG,
            String.format(
                    "Performing RESTORE! Alarm:[%s,%s,%s,%s,%s]",
                     alarm.daysOfWeek, alarm.enabled,
                     alarm.hour, alarm.minutes, alarm.alert));
            Alarm.addAlarm(getContentResolver(), alarm);
            if (alarm.enabled) {
                setupAlarmInstance(getApplicationContext(), alarm);
            }
        }
    }

    /*
     * restore from 2.7 or older version
     */
    private void restoreAlarmInfoOlderVersion(DataInputStream in) throws IOException {
        // Clear old alarms
        String[] selectionArgs = {};
        int m_daysofweek;
        boolean m_enable;
        boolean m_missed;
        int m_hour;
        int m_minute;
        Uri m_ringtone;
        int m_snooze;
        int m_volume;
        String m_ringtonepath;
        long m_snoozeTime;

        allAlarms = Alarm.getAlarms(getContentResolver(), null, selectionArgs);
        for (Alarm alarm:allAlarms) {
            Alarm.deleteAlarm(getContentResolver(), alarm.id);
        }

        int alarmNum = in.readInt();
        Log.i("AlarmClock","start restore alarm count"+alarmNum);
        for (int i = 0; i < alarmNum; i++) {
            Alarm alarm = new Alarm();
            m_daysofweek = in.readInt();
            m_enable = in.readBoolean();
            m_missed = in.readBoolean();
            m_hour = in.readInt();
            m_minute = in.readInt();
            m_ringtone = Uri.parse(in.readUTF());
            m_snooze = in.readInt();
            m_volume = in.readInt();
            m_ringtonepath = in.readUTF();
            m_snoozeTime = in.readLong();

            //usefull data for alarm3.0
            alarm.daysOfWeek = new DaysOfWeek(m_daysofweek);
            alarm.enabled = m_enable;
            alarm.vibrate = true;
            alarm.hour = m_hour;
            alarm.minutes = m_minute;
            //check the file if exist , if is there, use it , else use default.
//            File file = new File(m_ringtonepath);
//            if (file.exists()) {
//                alarm.alert = Uri.fromFile(file);
//            } else {
            //use default ringtone
            m_ringtone = RingtoneManager.getActualDefaultRingtoneUri(this,
                    RingtoneManager.TYPE_ALARM);
            /*if (m_ringtone == null) {
                m_ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }*/
            //check the file isExist.
            String audio_path = getRealPathFromURI(m_ringtone);
            if (audio_path != null) {
                File audioFile = new File(audio_path);
                if (!audioFile.exists()) {
                    Log.i("AlarmClock", "file is not exist, set a static uri");
                    //m_ringtone = Uri.parse("content://settings/system/alarm_alert");
                    String sUri = Settings.System.getString(getApplicationContext().getContentResolver(),
                                                     Settings.System.SYSTEM_ALARM_ALERT);
                    if(sUri != null){
                         m_ringtone = Uri.parse(sUri);
                    }
                    else{
                         Log.i("AlarmClock", "media scan error");
                         m_ringtone = Uri.parse("content://media/internal/audio/media/14");
                    }
                }
            } else {
                //m_ringtone = Uri.parse("content://media/internal/audio/media/14");
                    String sUri = Settings.System.getString(getApplicationContext().getContentResolver(),
                                                     Settings.System.SYSTEM_ALARM_ALERT);
                    if(sUri != null){
                         m_ringtone = Uri.parse(sUri);
                    }
                    else{
                         Log.i("AlarmClock", "media scan error");
                         m_ringtone = Uri.parse("content://media/internal/audio/media/14");
                    }
            }
            if (m_ringtone.equals(Uri.parse("content://settings/system/alarm_alert")) || m_ringtone == null) {
                if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
                    Object object = ClassUtils.invokeMethod(RingtoneManager.class, "getDefaultRingtoneUri", AlarmBackupAgent.class,
                    String.class, this, RingtoneManager.TYPE_ALARM);
                    m_ringtone = (Uri) object;
                } else {
                    m_ringtone = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
                }
            }
            alarm.alert = m_ringtone;
        //}
            Log.d(TAG,
                String.format(
                    "Performing RESTORE! Alarm:[%s,%s,%s,%s,%s]",
                     alarm.daysOfWeek, alarm.enabled,
                     alarm.hour, alarm.minutes, alarm.alert));
            Alarm.addAlarm(getContentResolver(), alarm);
            if (alarm.enabled) {
                setupAlarmInstance(getApplicationContext(), alarm);
            }
        }

    }

    private AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver cr = context.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance(),context);
        newInstance = AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result = null;
        Context mContext = getApplicationContext();
        Cursor cursor = mContext.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            int index = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
            Log.i("AlarmClock", "the index of the DATA :" + index);
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
        Log.i("AlarmBackUpAgent", "the real audio file's name is :" + result);
        Log.i("AlarmBackUpAgent", "the audio's uri is : " + contentURI.toString());
        return result;
    }

    /**
     * MD5
     *
     * @param str
     * @return
     */
    public static String md5(byte[] byteArray) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        byte[] md5Bytes = md5.digest(byteArray);

        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

}
