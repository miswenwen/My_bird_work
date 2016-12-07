
package com.yunos.common;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.yunos.alicontacts.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class UiTools {
    private static final String TAG = "AliContacts-UiTools";
    private static final int BUFFER_SIZE = 1024 * 1024 * 8;

    // TODO: to 200kuan or OEM team,
    // Please change the package name if the device uses a different homeshell.
    private static final String HOME_SHELL_PACKAGE = "com.aliyun.homeshell";
    private static final String RECENT_TASKS_PACKAGE = "com.android.systemui";

    /**
     * Check if an activity is launched by homeshell (or recent tasks).
     * This is typically used to check if the activity shall return to homeshell.
     * @param activityToken This is a token to identify the activity,
     *                       which we want to check if it is launched by homeshell.
     *                       It can be read via Activity.getActivityToken().
     * @param intent The latest handle intent of the activity.
     * @return If the activity is launched by homeshell (or recent tasks).
     */
    public static boolean isFromHomeShell(IBinder activityToken, Intent intent) {
        String fromPkg = null;
        try {
            fromPkg = ActivityManagerNative.getDefault().getLaunchedFromPackage(activityToken);
        } catch (RemoteException re) {
            Log.i(TAG, "isFromHomeShell: got remote exception", re);
            return false;
        }
        if ((!HOME_SHELL_PACKAGE.equals(fromPkg)) && (!RECENT_TASKS_PACKAGE.equals(fromPkg))) {
            return false;
        }
        String action = intent.getAction();
        Set<String> categories = intent.getCategories();
        Log.i(TAG, "isFromHomeShell: fromPkg="+fromPkg+"; action="+action+"; categories="+categories);
        // Most of the time, we start contacts from home shell or recent tasks.
        if (Intent.ACTION_MAIN.equals(action)
                && (categories != null) && (categories.contains(Intent.CATEGORY_LAUNCHER))) {
            Log.i(TAG, "isFromHomeShell: return true.");
            return true;
        }
        return false;
    }

    /**
     * Change the foreground display to homeshell.
     * If failed to launch homeshell, then quit the current activity.
     * @param activity The current activity.
     */
    public static void switchToHomeShell(Activity activity) {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            homeIntent.setPackage(HOME_SHELL_PACKAGE);
            activity.startActivity(homeIntent);
        } catch (ActivityNotFoundException e) {
            // in some 200kuan devices, the home shell might be replaced.
            // if HOME_SHELL_PACKAGE is not updated,
            // then we will get exception on such devices,
            // and we just quit the activity in this case.
            activity.finish();
        }
    }

    public static int getResourdIdByResourdName(Context context, String ResName) {
        int resourceId = 0;
        try {
            Field field = R.drawable.class.getField(ResName);
            field.setAccessible(true);

            try {
                resourceId = field.getInt(null);
            } catch (IllegalArgumentException e) {
                Log.e("Linc", "IllegalArgumentException:" + e.toString());
            } catch (IllegalAccessException e) {
                Log.e("Linc", "IllegalAccessException:" + e.toString());
            }
        } catch (NoSuchFieldException e) {
            Log.e("Linc", "NoSuchFieldException:" + e.toString());
        }
        return resourceId;
    }

    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public static Bitmap Bytes2Bitmap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    public static int upZipFile(File zipFile, String folderPath) throws ZipException, IOException {
        // public static void upZipFile() throws Exception{
        ZipFile zfile = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> zList = zfile.entries();
        ZipEntry ze = null;
        byte[] buf = new byte[1024];
        while (zList.hasMoreElements()) {
            ze = zList.nextElement();
            String zeName = ze.getName();
            if (!isValidNameFromZip(zeName)) {
                Log.w(TAG, "unZipFile: got invalid name <"+zeName+">, skip.");
                continue;
            }
            if (ze.isDirectory()) {
                Log.d("upZipFile", "zeName = " + zeName);
                String dirstr = folderPath + zeName;
                // dirstr.trim();
                dirstr = new String(dirstr.getBytes("8859_1"), "GB2312");
                Log.d("upZipFile", "str = " + dirstr);
                File f = new File(dirstr);
                f.mkdir();
                continue;
            }
            Log.d("upZipFile", "zeName = " + zeName);
            OutputStream os = null;
            InputStream is = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(getRealFileName(folderPath, zeName)));
                is = new BufferedInputStream(zfile.getInputStream(ze));
                int readLen = 0;
                while ((readLen = is.read(buf, 0, 1024)) != -1) {
                    os.write(buf, 0, readLen);
                }
            } finally {
                closeCloseable(is);
                closeCloseable(os);
            }
        }
        zfile.close();
        Log.d("Linc", "upZipFile finished!");
        return 0;
    }

    public static File getRealFileName(String baseDir, String absFileName) {
        String[] dirs = absFileName.split("/");
        File ret = new File(baseDir);
        String substr = null;
        if (dirs.length > 1) {
            for (int i = 0; i < dirs.length - 1; i++) {
                substr = dirs[i];
                try {
                    // substr.trim();
                    substr = new String(substr.getBytes("8859_1"), "GB2312");

                } catch (UnsupportedEncodingException e) {
                    Log.e("xiaomin", e.getLocalizedMessage());
                }
                ret = new File(ret, substr);

            }
            Log.d("upZipFile", "1ret = " + ret);
            if (!ret.exists())
                ret.mkdirs();
            substr = dirs[dirs.length - 1];
            try {
                // substr.trim();
                substr = new String(substr.getBytes("8859_1"), "GB2312");
                Log.d("upZipFile", "substr = " + substr);
            } catch (UnsupportedEncodingException e) {
                Log.e("xiaomin", e.getLocalizedMessage());
            }

            ret = new File(ret, substr);
            Log.d("upZipFile", "2ret = " + ret);
            return ret;
        }
        return ret;
    }

    /**
     * unzip a zip/jar archive to destination path
     *
     * @param zipFileString archive file name
     * @param outPathString destination path
     */
    public static boolean unZipFolder(InputStream inputStream, String outPathString) {
        ZipInputStream inZip = null;
        FileOutputStream out = null;
        try {
            // If outPathString folder does not exist, create it first
            File dbDir = new File(outPathString);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            inZip = new ZipInputStream(inputStream);
            ZipEntry zipEntry;
            String szName = "";
            while ((zipEntry = inZip.getNextEntry()) != null) {
                szName = zipEntry.getName();
                if (!isValidNameFromZip(szName)) {
                    Log.w(TAG, "unZipFolder: got invalid name <"+szName+">, skip.");
                    continue;
                }
                if (zipEntry.isDirectory()) {
                    // get the folder name of the widget
                    szName = szName.substring(0, szName.length() - 1);
                    File folder = new File(outPathString + File.separator + szName);
                    folder.mkdirs();
                } else {
                    File file = new File(outPathString + File.separator + szName);
                    file.createNewFile();
                    // get the output stream of the file
                    try {
                        out = new FileOutputStream(file);
                        int len;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        // read (len) bytes into buffer
                        while ((len = inZip.read(buffer)) != -1) {
                            // write (len) byte from buffer at the position 0
                            out.write(buffer, 0, len);
                            out.flush();
                        }
                    } finally {
                        closeCloseable(out);
                        out = null;
                    }
                }
            }// end of while
        } catch (Exception e) {
            DebugLog.d(TAG, "unZipFolder error:" + e.getLocalizedMessage());
            return false;
        } finally {
            closeCloseable(inZip);
        }
        return true;
    }

    private static void closeCloseable(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e1) {
                DebugLog.d(TAG, e1.getLocalizedMessage());
            }
        }
    }

    /**
     * getIMEI
     *
     * @param context - Context
     * @return
     */
    public static String getIMEI(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            String meid = telephonyManager.getDeviceId();
            return meid;
        } else {
            return "unknown";
        }
    }

    private static boolean isValidNameFromZip(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        // 1. Not allowed: specify absolute path from root (/).
        // 2. Not allowed: go to parent directory.
        if (name.startsWith("/") || name.startsWith("../") || name.contains("/../") || name.endsWith("/..")) {
            return false;
        }
        return true;
    }

    public static void closeSoftInput(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && (activity.getCurrentFocus() != null)) {
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static void addDialerListenerToEditText(EditText edit) {
        if (edit != null) {
            InputFilter[] origFilters = edit.getFilters();
            InputFilter extraFilter = DialerKeyListener.getInstance();
            InputFilter[] moreFilters;
            if ((origFilters == null) || (origFilters.length == 0)) {
                moreFilters = new InputFilter[] { extraFilter };
            } else {
                moreFilters = new InputFilter[origFilters.length + 1];
                System.arraycopy(origFilters, 0, moreFilters, 0, origFilters.length);
                moreFilters[origFilters.length] = extraFilter;
            }
            edit.setFilters(moreFilters);
        }
    }

}
