package com.bird.contacts;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import android.content.DialogInterface;
import android.os.SystemProperties;
// push test add by meifangting 20160406  begin
import android.os.CountDownTimer;
import android.content.pm.ApplicationInfo;
import android.os.SystemProperties;
import android.app.AlertDialog;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
// push test add by meifangting 20160406 end
public class BirdSpecialCharMgr {
    private static final String TAG="BirdSpecialCharMgr";
    public static boolean handleChar(Context context, String input, /*boolean useSystemWindow,*/EditText textField){
        return handleWriteIMEI(context, input)
        || handleROAM(context, input)// push test add by meifangting 20160406
		|| handlePushTest(context, input);
    }

    private static boolean startActivity(Context context,String pkgName,String clzName){
        ComponentName cpn = new ComponentName(pkgName, clzName);
        Intent intent = new Intent();
        intent.setComponent(cpn);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "startActivity() failed: " + e);
            return false;
        }
        return true;
    }

    private static boolean handleWriteIMEI(Context context, String input) {
        if (SystemProperties.getBoolean("ro.bdfun.write_imei", false)) {
            if (SystemProperties.getBoolean("ro.bdfun.store_imei", false)) {
                if (input.equalsIgnoreCase("*#6864#")) {
                    Intent intent = new Intent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setClassName("com.android.writeimei","com.android.writeimei.StoreIMEI");
                    context.startActivity(intent);
                    return true;

                }
            }
            int len = input.length();
            if (len == 23 && input.startsWith("*#")) {
                Intent intent = new Intent();

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (input.endsWith("#6666#")) {

                    intent.setClassName("com.android.writeimei","com.android.writeimei.WriteIMEI");
                    intent.putExtra("#6666#", input.substring(2, len - 6));
                    intent.putExtra("show_dialog", true);
                } else if (true/* BirdFeatureOption.MTK_GEMINI_SUPPORT */&& input.endsWith("#7777#")) {

                    intent.setClassName("com.android.writeimei","com.android.writeimei.WriteIMEIGemini");
                    intent.putExtra("#7777#", input.substring(2, len - 6));
                    intent.putExtra("show_dialog", true);
                } else {
                    return false;
                }
                for (int i = 2; i < 17; i++) {
                    if ((!input.substring(i, i + 1).equals("0"))
                            && (!input.substring(i, i + 1).equals("1"))
                            && (!input.substring(i, i + 1).equals("2"))
                            && (!input.substring(i, i + 1).equals("3"))
                            && (!input.substring(i, i + 1).equals("4"))
                            && (!input.substring(i, i + 1).equals("5"))
                            && (!input.substring(i, i + 1).equals("6"))
                            && (!input.substring(i, i + 1).equals("7"))
                            && (!input.substring(i, i + 1).equals("8"))
                            && (!input.substring(i, i + 1).equals("9"))) {
                        return false;
                    }
                }
                context.startActivity(intent);
                return true;
            }
        }
        return false;
    }

//BIRD_CUSTOM_ROAM, add by shenzhiwang, 20160323
    private static boolean handleROAM(Context context, String input) {

        if (SystemProperties.getBoolean("ro.bdfun.custom_roam", false) || SystemProperties.getBoolean("ro.bdfun.custom_roam_new", false)) {
            if (input.equals("*#7626#")) {
                Intent intent = new Intent("bird.intent.receiver.roam");
                context.sendBroadcast(intent);
                return true;
            } else if (input.equals("*#663352#")) {
                Intent intent = new Intent("bird.intent.receiver.model");
                context.sendBroadcast(intent);
                return true;
            }
        }

        return false;
    }
  // push test add by meifangting 20160406 begin 
  private static final String BIRD_PUSH_TEST_OPEN_CODE="*#42681#*";
  private static final String BIRD_PUSH_TEST_CODE="*#4268#*";
  private static boolean isOpenPushTest = false;
  private static boolean handlePushTest(Context context, String input) {
        if (BIRD_PUSH_TEST_OPEN_CODE.equals(input)) {
		    Toast.makeText(context," Check the push instruction within three minutes effective",Toast.LENGTH_LONG).show();
            isOpenPushTest = true;
			mCodeValidCountDownTimer.cancel();
			mCodeValidCountDownTimer.start();
			return true;
        }else if(BIRD_PUSH_TEST_CODE.equals(input)&&isOpenPushTest){
		    final CharSequence[] pushcheckStr = new CharSequence[1];
			String lqcheckstr = null;
			if(isPackageInstalled(context,"com.aliyun.homeshell")&&!SystemProperties.getBoolean("ro.bdfun.remove_lqadsdk",false)){
			   lqcheckstr = getPushVersionStr(context,"com.aliyun.homeshell","lqsdk_version")+"Homeshell";
			}
            pushcheckStr[0] = "MODE1(LQ):"+(lqcheckstr==null?"OFF":"ON")+",Version:"+lqcheckstr;
			String zzcheckstr = null;
			if(SystemProperties.getBoolean("ro.bdfun.batcloud_push",false)){
			   zzcheckstr = getPushVersionStr(context,"com.android.browser","zzsdk_version")+"";
			}	   
		//	pushcheckStr[1] = "MODE2(ZZ):"+(zzcheckstr==null?"OFF":"ON"+",Version"+zzcheckstr)+"";
		//	pushcheckStr[2] = "MODE3(TBD):NULL";
		//	pushcheckStr[3] = "MODE4(JY):NULL";
			String titleStr = Build.MANUFACTURER + "__" + Build.BRAND + "__" + Build.MODEL;
            AlertDialog alert = new AlertDialog.Builder(context).setTitle(titleStr)
                 .setItems(pushcheckStr, null).setPositiveButton(android.R.string.ok, null)
                 .setCancelable(false).create();
            alert.show();
			return true;
		}
        return false;
    }
  protected static final int PUSH_CODE_VAIL_DURING = 3*60*1000;
  protected static CountDownTimer mCodeValidCountDownTimer = new CountDownTimer(PUSH_CODE_VAIL_DURING,
			1000) {
		@Override
		public void onTick(long millisUntilFinished) {
			
		}

		@Override
		public void onFinish() {
		   isOpenPushTest = false;
		}
	};
  private static String getPushVersionStr(Context context,String packagename,String metaName){
        return "";
  }
  
  private static boolean isPackageInstalled(Context context, String packageName)
	{
		PackageManager pm = context.getPackageManager();
		boolean result = false;

		try
		{
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			result = true;
		}
		catch (PackageManager.NameNotFoundException e)
		{
			result = false;
		}

		return result;
	}
// push test add by meifangting 20160406 end 
}
