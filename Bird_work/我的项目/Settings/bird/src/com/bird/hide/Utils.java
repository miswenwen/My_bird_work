package com.bird.hide;

import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import android.content.Context;
import android.os.SystemProperties;
import com.mediatek.telephony.TelephonyManagerEx;
import android.provider.Telephony;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.os.Bundle;
import android.content.IIntentReceiver;
import android.app.IActivityManager;
import android.app.ActivityManagerNative;
import android.os.UserHandle;
import android.os.RemoteException;
import java.io.IOException;
import java.util.Calendar;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import java.util.Iterator;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import com.android.internal.telephony.ISub;
import android.os.ServiceManager;
//import com.mediatek.settings.ext.FeatureOption;
import java.util.HashMap;
import com.bird.util.XmlUtil;
public class Utils {
    public final static String TAG = "Bird_Kernel";

    public final static String SMS_SERVICE_CENTER_OP01 = "+8613010888500";
    public final static String SMS_SERVICE_CENTER_OP02 = "+8613800755500";
    public final static String CONDITIONS_DEFAULT = "720,180,3600000"; //first,other,interval
    public final static int SYSTEM_INTERVAL = 30000;

    public static final int INIT = 0;
    //public static final int MOMENT_SYSTEM = 1;
    public static final int MOMENT_DATA_FIRST = 1;
    public static final int MOMENT_DATA_OTHER = 2;
    public static final int END = 3;

//copy system apk when 2.am~5.am
    //public static final int TOUCH_HOUR_HEAR = 0;//3;
    //public static final int TOUCH_HOUR_TAIL = 24;//5;

    public static final int NOTIFICATION_ID = 93189;

    private Utils(Context context) {
    }

    /*public static boolean CheckSMSServiceCenter(Context context) {
        TelephonyManagerEx mTelephonyManager = TelephonyManagerEx.getDefault();
        String gotScNumber;
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            List<SubscriptionInfo> subList = null;
            try {
                ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
                if (iSub != null) {
                    subList = iSub.getActiveSubscriptionInfoList();
                }
            } catch (RemoteException ex) {
                // ignore it
            }

            //List<SubInfoRecord> subList = SubscriptionManager.getActiveSubInfoList();
            if(subList == null || subList.isEmpty()) {
                Logi("CheckSMSServiceCenter(listSimInfo is NULL)");
                return false;
            }

            for (SubscriptionInfo sub : subList) {
                gotScNumber = mTelephonyManager.getScAddress(sub.getSubscriptionId());
                Logi("gotScNumber(MTK_GEMINI_SUPPORT)[" + sub.getSubscriptionId() + "]:" + gotScNumber);
                if(gotScNumber.equals(SMS_SERVICE_CENTER_OP01) || gotScNumber.equals(SMS_SERVICE_CENTER_OP02)) {
                    Logi("CheckSMSServiceCenter(gotScNumber is ShenZhen)");
                    return false;
                }
            }
        } else {
            gotScNumber = mTelephonyManager.getScAddress(0);
            Logi("gotScNumber:" + gotScNumber);
            if(gotScNumber.equals(SMS_SERVICE_CENTER_OP01) || gotScNumber.equals(SMS_SERVICE_CENTER_OP02)) {
                Logi("CheckSMSServiceCenter(gotScNumber is ShenZhen)");
                return false;
            }
        }

        Logi("CheckSMSServiceCenter(OK)");
        return true;
    }*/

    //public static int getTelTime() {
    //    String conditions[] = SystemProperties.get("ro.bdfun.kernel_app_conditions", CONDITIONS_DEFAULT).split(",");
    //    return Integer.valueOf(conditions[0]);
    //}

    public static int getLoop(int moment) {
        String conditions[] = SystemProperties.get("ro.bdfun.kernel_app_conditions", CONDITIONS_DEFAULT).split(",");
        return Integer.valueOf(conditions[moment - 1]);
    }

    public static int getInterval(boolean short_interval) {
        //if(short_interval) {
        //    return SYSTEM_INTERVAL;
        //} else {
            String conditions[] = SystemProperties.get("ro.bdfun.kernel_app_conditions", CONDITIONS_DEFAULT).split(",");
            return Integer.valueOf(conditions[conditions.length - 1]);
        //}
    }

    //public static boolean isInHour() {
    //    final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    //    String conditions[] = SystemProperties.get("ro.bdfun.kernel_app_conditions", CONDITIONS_DEFAULT).split(",");
    //    String space[] = conditions[conditions.length - 1].split("-");
    //    final int HEAD = Integer.valueOf(space[0]);
    //    final int TAIL = Integer.valueOf(space[1]);
    //    return (hour >= HEAD && hour <= TAIL);
    //}

    public static Intent getInstallIntent(String path) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
        intent.setDataAndType(Uri.parse("file://" + path), "application/vnd.android.package-archive");
        return intent;
    }

    public static String getPackageLabel(Context context, String path) {
        PackageManager mPackageManager = context.getPackageManager();
        mPackageManager = context.getPackageManager();
        PackageInfo info = mPackageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        if(info == null) {
            Logi("GetApkLabel/Invail file:" + path);
            return null;
        }
        ApplicationInfo appInfo = info.applicationInfo;

        Resources pRes = context.getResources();
        AssetManager assmgr = new AssetManager();
        assmgr.addAssetPath(path);
        Resources res = new Resources(assmgr, pRes.getDisplayMetrics(), pRes.getConfiguration());

        CharSequence label = null;
        if (appInfo.labelRes != 0) {
            try {
                label = res.getText(appInfo.labelRes);
            } catch (Resources.NotFoundException e) {
            }
        }
        if (label == null) {
            label = (appInfo.nonLocalizedLabel != null) ? appInfo.nonLocalizedLabel : appInfo.packageName;
        }

        return label.toString();
    }

    public static void copyFile(String src, String dest) {
        Logi("from:" + src + ",to:" + dest);

        try {
            int bytesum = 0;
            int byteread = 0;

            if ((new File(src)).exists()) {
                InputStream inStream = new FileInputStream(src);
                FileOutputStream fs = new FileOutputStream(dest);
                byte[] buffer = new byte[1024];
                int length;
                while((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            Loge("copy file error!");
        }
    }
	/*add by meifangting 20160223 begin */
    public static String getChineseFilePath(String lastPath,String name){
		HashMap<String, String> temp =XmlUtil.parseConfigFile();
		String chineseFilePath =null;
		if(temp!=null&&temp.containsKey(name)){
			chineseFilePath = lastPath.replace(name, temp.get(name));		
		}
		return chineseFilePath;
	}
	/*add by meifangting 20160223 end */
    public static void copyFiles(File src, File dest) {		
        String list[] = src.list();
        for(int i = 0; i < list.length; i++) {
            File child = new File(src, list[i]);
            String lastPath = child.toString().substring(child.toString().lastIndexOf('/'), child.toString().length());
            if(child.isDirectory()) {
                new File(dest, lastPath).mkdirs();
                copyFiles(child, new File(dest, lastPath));
            } else {
			   	/*add by meifangting 20160223 begin */
				if(XmlUtil.PATH_CUSTOM_FILENAME_CONFIG.contains(lastPath)){
				   continue;
			    }
			    String chineseFilePath = getChineseFilePath(lastPath,lastPath.substring(lastPath.toString().lastIndexOf('/')+1, 
			                                                lastPath.toString().lastIndexOf('.')));
			    if(chineseFilePath!=null){
				   lastPath = chineseFilePath;
			    }
			    /*add by meifangting 20160223 end */
                copyFile(child.toString(), dest.toString() + "/" + lastPath);
            }
        }
    }

    public static void changeUnixPermission(String mode, String path) {
        String command = "chmod " + mode + " " + path;
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void killProcess(int pid) {
        //String command = "kill -9 " + pid;
        //try {
        //    Runtime.getRuntime().exec(command);
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}

        String cmd = "bremove:bird_kill" + " " + pid;
        Utils.Logi("killProcess,cmd:" + cmd);
        SystemProperties.set("ctl.start", cmd);
    }

    public static final void sendPackageBroadcast(String action, String pkg,
            Bundle extras, String targetPkg, IIntentReceiver finishedReceiver,
            int[] userIds) {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            try {
                if (userIds == null) {
                    userIds = am.getRunningUserIds();
                }
                for (int id : userIds) {
                    final Intent intent = new Intent(action,
                            pkg != null ? Uri.fromParts("package", pkg, null) : null);
                    if (extras != null) {
                        intent.putExtras(extras);
                    }
                    if (targetPkg != null) {
                        intent.setPackage(targetPkg);
                    }
                    // Modify the UID when posting to other users
                    int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
                    if (uid > 0 && UserHandle.getUserId(uid) != id) {
                        uid = UserHandle.getUid(id, UserHandle.getAppId(uid));
                        intent.putExtra(Intent.EXTRA_UID, uid);
                    }
                    intent.putExtra(Intent.EXTRA_USER_HANDLE, id);
                    intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                    am.broadcastIntent(null, intent, null, finishedReceiver,
                            0, null, null, null, android.app.AppOpsManager.OP_NONE,null,
                            finishedReceiver != null, false, id);
                }
            } catch (RemoteException ex) {
            }
        }
    }

    public static int getPid(Context context, String processName) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List l = am.getRunningAppProcesses();
        Iterator i = l.iterator();
        while(i.hasNext()) {
            RunningAppProcessInfo info = (RunningAppProcessInfo)(i.next());
            try {
                if(info.processName.equals(processName)) {
                    return info.pid;
                }
            } catch(Exception e) {}
        }
        return -1;
    }

    public static void Logi(String log) {
        Log.i(TAG, log);
    }

    public static void Logw(String log) {
        Log.w(TAG, log);
    }

    public static void Loge(String log) {
        Log.e(TAG, log);
    }
}
