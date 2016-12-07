/*
 * Copyright Statement:
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.android.deskclock.alarms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.android.deskclock.Log;
import com.android.deskclock.provider.AlarmInstance;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("PMD")
public class PowerOffAlarm {

    /**M: @{
     * Whether this boot is from power off alarm or schedule power on or normal boot.
     * @return
     */
    public static boolean bootFromPoweroffAlarm() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        boolean ret = (bootReason != null && bootReason.equals("1")) ? true : false;
        Log.v("bootFromPoweroffAlarm ret is " + ret);
        return ret;
    }

    /**M: @{
     * Whether the device was Unencrypted.
     */
    static boolean deviceUnencrypted() {
        return "unencrypted".equals(SystemProperties.get("ro.crypto.state"));
    }

    /**M: @{
     * copy ringtone music file to local from sd-card, to avoid power-off alarm
     * could not load the user set ringtone music. if have existed not the same
     * ringtone based on the file name then delete and copy the new one
     * there.
     */
    @SuppressWarnings("PMD")
    static void backupRingtoneForPoweroffAlarm(final Context ctx, final AlarmInstance nextAlarm) {
        Log.v("backupRingtoneForPoweroffalarm ...... ");
        new Thread() {
            public void run() {
                String filepath = null;
                File existedRingtone = null;
                File files = ctx.getFilesDir();
                String nextRingtone = null;
                nextRingtone = getNearestAlarmWithExternalRingtone(ctx, nextAlarm);
                Log.v("nextRingtone: " + nextRingtone);
                if (nextRingtone != null) {
                    if (files.isDirectory() && files.list().length == 1) {
                        for (File item : files.listFiles()) {
                            existedRingtone = item;
                        }
                    }
                    String existedRingtoneName = existedRingtone == null ? null : existedRingtone.getName();
                    Log.v("existedRingtoneName: " + existedRingtoneName);
                    if (!TextUtils.isEmpty(nextRingtone) && (existedRingtoneName == null
                            || !TextUtils.isEmpty(existedRingtoneName)
                                    && !nextRingtone.contains(existedRingtoneName))) {
                        if (existedRingtone != null && !existedRingtone.delete()) {
                            Log.v("delete existedRingtone error");
                        }
                        filepath = getRingtonePath(ctx, nextRingtone);
                        if (filepath != null) {
                            // copy from sd-card to local files directory.
                            String target = files.getAbsolutePath()
                                    + File.separator
                                    + nextRingtone.substring(nextRingtone
                                            .lastIndexOf(File.separator) + 1);
                            try {
                                copyFile(filepath, target);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }.start();
    }

    /**M: @{
     * get the next will play alarm, whose ringtone is from external storage
     */
    public static String getNearestAlarmWithExternalRingtone(Context context, AlarmInstance nextAlarm) {
        String alert = null;
        if (nextAlarm != null && nextAlarm.mRingtone != null
                    && nextAlarm.mRingtone.toString().contains("external")) {
                alert = nextAlarm.mRingtone.toString();
            }
        return alert;
    }

    /**M: @{
     * get RingtonePath
     */
    public static String getRingtonePath(final Context mContext, final String alarmRingtone) {
        final ContentResolver cr = mContext.getContentResolver();
        String filepath = null;
        Log.v("alarmRingtone: " + alarmRingtone);
        if (!TextUtils.isEmpty(alarmRingtone)) {
                Cursor c = null;
                try {
                    c = cr.query(Uri.parse(alarmRingtone), null,
                            null, null, null);
                    if (c != null && c.moveToFirst()) {
                        filepath = c.getString(1);
                    }
                } catch (SQLiteException e) {
                    Log.v("database operation error: " + e.getMessage());
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
        }
        return filepath;
    }

    /**M: @{
     * copy one file from source to target
     * @param from source
     * @param to   target
     */
    private static int copyFile(String from, String to) throws IOException {
        Log.v("source: " + from + "  target: " + to);
        int result = 0;
        if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
            result = -1;
        }
        Log.v("media mounted: " + Environment.getExternalStorageState());
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            java.io.InputStream fis = null;
            java.io.OutputStream fos = null;
            try {
                fos = new java.io.FileOutputStream(to);
                try {
                    fis = new java.io.FileInputStream(from);
                    byte bt[] = new byte[1024];
                    int c;
                    while ((c = fis.read(bt)) > 0) {
                        fos.write(bt, 0, c);
                    }
                    fos.flush();
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.v("copy ringtone file error: " + e.toString());
                result = -1;
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }
        return result;
    }

}
