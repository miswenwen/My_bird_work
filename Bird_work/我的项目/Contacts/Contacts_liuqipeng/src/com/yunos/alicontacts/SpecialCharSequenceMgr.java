/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.yunos.alicontacts;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.ActionPullParser;
import com.bird.contacts.BirdSpecialCharMgr; //add by lichengfeng for task #7692 
import com.yunos.alicontacts.util.FeatureOptionAssistant;

/*add by lichengfeng show MEID,IMEI1,IEMI2 in c2k version @20160224 begin */
import com.android.internal.telephony.PhoneFactory;
//import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;
/*add by lichengfeng show MEID,IMEI1,IEMI2 in c2k version @20160224 end */
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.internal.telephony.PhoneConstants;

import java.util.List;

/**
 * Helper class to listen for some magic character sequences
 * that are handled specially by the dialer.
 *
 * Note the Phone app also handles these sequences too (in a couple of
 * relativly obscure places in the UI), so there's a separate version of
 * this class under apps/Phone.
 *
 * TODO: there's lots of duplicated code between this class and the
 * corresponding class under apps/Phone.  Let's figure out a way to
 * unify these two classes (in the framework? in a common shared library?)
 */
public class SpecialCharSequenceMgr {
    private static final String TAG = "SpecialCharSequenceMgr";
    private static final String MMI_IMEI_DISPLAY = "*#06#";
    private static final String EXTERNAL_VERSION_DISPLAY = "*#166*#";
    private static final String INTERNAL_VERSION_DISPLAY = "*#29826633#";

    /**
     * Remembers the previous {@link QueryHandler} and cancel the operation when needed, to
     * prevent possible crash.
     *
     * QueryHandler may call {@link ProgressDialog#dismiss()} when the screen is already gone,
     * which will cause the app crash. This variable enables the class to prevent the crash
     * on {@link #cleanup()}.
     *
     * TODO: Remove this and replace it (and {@link #cleanup()}) with better implementation.
     * One complication is that we have SpecialCharSequencMgr in Phone package too, which has
     * *slightly* different implementation. Note that Phone package doesn't have this problem,
     * so the class on Phone side doesn't have this functionality.
     * Fundamental fix would be to have one shared implementation and resolve this corner case more
     * gracefully.
     */
    private static QueryHandler sPreviousAdnQueryHandler;

    /**
     * Broadcast Action: A "secret code" has been entered in the dialer. Secret codes are
     * of the form *#*#<code>#*#*. The intent will have the data URI:</p>
     *
     * <p><code>android_secret_code://&lt;code&gt;</code></p>
     */
    // This constant is copied from com.android.internal.telephony.TelephonyIntents.
    public static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";

    private static final String MMI_USB_PORT_SWITCH = "*#061#";
    // Add by NTD, to fix bug 4446
    private static final String MMI_STEP_ONE = "*#360#";
    private static final String MMI_STEP_TWO = "*#363#";
    private static final String MMI_WATCH_DATA = "*#368#";
    private static final String MMI_STEP_ONE_RESULT = "*#360*0#";
    private static final String MMI_STEP_TWO_RESULT = "*#363*0#";
    private static final String FACTORY_RESET = "*#366#*";
    // Add by NTD, to fix bug 6394
    private static final String NTD_SETTINGS = "*#370#*";
    // End by NTD, to fix bug 6394
    private static final String FACTORY_INFORMATION_DISPLAY = "*#0000#";
    // End by NTD, to fix bug 4446
    private static final String FACTORY_ITEM_INFO_DISPLAY = "*#94683#";

    private static final String MMI_STEP_THREE = "*#369#";
    private static final String MMI_STEP_THREE_RESULT = "*#369*0#";

    /** This class is never instantiated. */
    private SpecialCharSequenceMgr() {
    }

    public static boolean handleChars(Context context, String input, EditText textField) {
        return handleChars(context, input, false, textField);
    }

    public static boolean handleChars(Context context, String input) {
        return handleChars(context, input, false, null);
    }

    static boolean handleChars(Context context, String input, boolean useSystemWindow,
            EditText textField) {

        //get rid of the separators so that the string gets parsed correctly
        String dialString = PhoneNumberUtils.stripSeparators(input);

        if (handleEMSecretCode(context, dialString)
                || handleIMEIDisplay(context, dialString, useSystemWindow)
                || handlePinEntry(context, dialString)
                /*|| handleAdnEntry(context, dialString, textField) lxd commented*/
				//add by lichengfeng for task #7692  20160523 begin
        		|| BirdSpecialCharMgr.handleChar(context, input, textField)
        		//add by lichengfeng for task #7692 20160523 end
                || handleSecretCode(context, dialString)
                || handleFactoryInfoEntry(context, dialString)
                || handleLaunchNTDSettings(context, dialString)
                || handleMMIOneEntry(context, dialString)
                || handleMMITwoEntry(context, dialString)
                || handleMMIThreeEntry(context, dialString)
                || handleMMIOneResultEntry(context, dialString)
                || handleMMITwoResultEntry(context, dialString)
                || handleMMIThreeResultEntry(context, dialString)
                || handleMMIWatchDataEntry(context, dialString)
                || handleFactoryResetEntry(context,dialString)
                || handleFactoryItemInfoEntry(context, dialString)
                || handleUSBPortEntry(context, dialString)) {
            return true;
        }

        return false;
    }

    /*YUNOS BEGIN*/
    //modules(Phone):[ (bug 52263)]  *#66# not enter factory mode
    //date:20130917,author:xuefei.xxf@alibaba-inc.com
    static boolean handleEMSecretCode(Context context, String input) {
        int len = input.length();


        ContactsApplication CA = (ContactsApplication)context.getApplicationContext();
        if(CA == null)
               return false;

        /*YUNOS BEGIN*/
        //modules(Contacts): [bug 127610 ]for customize engineer mode !
        //date 2014-6-13 author : xiongchao.lxc@alibaba-inc.com


        List<ActionPullParser.Customer> engineList = CA.getCustomerEngineList();
        if(engineList != null){
            for (ActionPullParser.Customer customer : engineList){
                    if(input.equals(customer.getcommandId()) && len > 4
                        && !TextUtils.isEmpty(customer.getPackageName())
                        && !TextUtils.isEmpty(customer.getClassName())){
                    Intent intent = new Intent();
                    intent.setClassName(customer.getPackageName() , customer.getClassName());
                    context.startActivity(intent);
                    return true;
                }else if(input.equals(customer.getcommandId()) && len > 4 && (customer.getSecretCodeName()!=null)){
                    Intent intent = new Intent(SECRET_CODE_ACTION, Uri.parse(customer.getSecretCodeName()));
                    context.sendBroadcast(intent);
                    return true;
                }else if(input.equals(customer.getcommandId()) && len > 4 && (customer.getActionName()!=null)){
                    if(customer.getExtraName() != null){
                        Intent intent = new Intent(customer.getActionName());
                        String value = customer.getExtraValue();
                        intent.putExtra(customer.getExtraName(),value == null ? customer.getcommandId() : value);
                        context.sendBroadcast(intent);
                        return true;
                    }else{
                        Intent intent = new Intent(customer.getActionName());
                        context.sendBroadcast(intent);
                        return true;
                    }
                }
            }
        }
        /*YUNOS END*/

        return false;
    }
    /*YUNOS END*/

    /**
     * Cleanup everything around this class. Must be run inside the main thread.
     *
     * This should be called when the screen becomes background.
     */
    public static void cleanup() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.wtf(TAG, "cleanup() is called outside the main thread");
            return;
        }

        if (sPreviousAdnQueryHandler != null) {
            sPreviousAdnQueryHandler.cancel();
            sPreviousAdnQueryHandler = null;
        }
    }

    /**
     * Handles secret codes to launch arbitrary activities in the form of *#*#<code>#*#*.
     * If a secret code is encountered an Intent is started with the android_secret_code://<code>
     * URI.
     *
     * @param context the context to use
     * @param input the text to check for a secret code in
     * @return true if a secret code was encountered
     */
    static boolean handleSecretCode(Context context, String input) {
        // Secret codes are in the form *#*#<code>#*#*
        int len = input.length();
        if (len > 8 && input.startsWith("*#*#") && input.endsWith("#*#*")) {
            Intent intent = new Intent(SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://" + input.substring(4, len - 4)));
            context.sendBroadcast(intent);
            return true;
        }

        return false;
    }

    /**
     * Handle ADN requests by filling in the SIM contact number into the requested
     * EditText.
     *
     * This code works alongside the Asynchronous query handler {@link QueryHandler}
     * and query cancel handler implemented in {@link SimContactQueryCookie}.
     */
    static boolean handleAdnEntry(Context context, String input, EditText textField) {
        /* ADN entries are of the form "N(N)(N)#" */

//        TelephonyManager telephonyManager =
//                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //if (telephonyManager == null
        //        || !TelephonyCapabilities.supportsAdn(telephonyManager.getCurrentPhoneType())) {
        //    return false;
        //}

        // if the phone is keyguard-restricted, then just ignore this
        // input.  We want to make sure that sim card contacts are NOT
        // exposed unless the phone is unlocked, and this code can be
        // accessed from the emergency dialer.
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.inKeyguardRestrictedInputMode()) {
            return false;
        }

        int len = input.length();
        if ((len > 1) && (len < 5) && (input.endsWith("#"))) {
            try {
                // get the ordinal number of the sim contact
                int index = Integer.parseInt(input.substring(0, len-1));

                // The original code that navigated to a SIM Contacts list view did not
                // highlight the requested contact correctly, a requirement for PTCRB
                // certification.  This behaviour is consistent with the UI paradigm
                // for touch-enabled lists, so it does not make sense to try to work
                // around it.  Instead we fill in the the requested phone number into
                // the dialer text field.

                // create the async query handler
                QueryHandler handler = new QueryHandler (context.getContentResolver());

                // create the cookie object
                SimContactQueryCookie sc = new SimContactQueryCookie(index - 1, handler,
                        ADN_QUERY_TOKEN);

                // setup the cookie fields
                sc.contactNum = index - 1;
                sc.setTextField(textField);

                // create the progress dialog
                sc.progressDialog = new ProgressDialog(context);
                sc.progressDialog.setTitle(R.string.simContacts_title);
                sc.progressDialog.setMessage(context.getText(R.string.simContacts_emptyLoading));
                sc.progressDialog.setIndeterminate(true);
                sc.progressDialog.setCancelable(true);
                sc.progressDialog.setOnCancelListener(sc);
                sc.progressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

                // display the progress dialog
                sc.progressDialog.show();

                // run the query.
                handler.startQuery(ADN_QUERY_TOKEN, sc, Uri.parse("content://icc/adn"),
                        new String[]{ADN_PHONE_NUMBER_COLUMN_NAME}, null, null, null);

                if (sPreviousAdnQueryHandler != null) {
                    // It is harmless to call cancel() even after the handler's gone.
                    sPreviousAdnQueryHandler.cancel();
                }
                sPreviousAdnQueryHandler = handler;
                return true;
            } catch (NumberFormatException ex) {
                // Ignore
                Log.e(TAG, "handleAdnEntry() parse number exceptions", ex);
            }
        }
        return false;
    }

    /*
     * Add by NTD, to fix bug 4446
     */
    static boolean handleMMIOneEntry(Context context, String input){
        if(input.equals(MMI_STEP_ONE)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("vendor.zhntd.factorytest", "vendor.zhntd.factorytest.FactoryTestActivity");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("nv_index", 1);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleMMIOneEntry", e);
                return false;
            }
        }
        return false;
    }

    /*
     * Add by NTD, to fix bug 4446
     */
    static boolean handleMMITwoEntry(Context context, String input){
        if(input.equals(MMI_STEP_TWO)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("vendor.zhntd.factorytest", "vendor.zhntd.factorytest.FactoryTestActivity");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("nv_index", 2);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleMMITwoEntry", e);
                return false;
            }
        }
        return false;
    }

    static boolean handleMMIWatchDataEntry(Context context, String input) {
        if (input.equals(MMI_WATCH_DATA)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.watchsmart.wdmmi.ui",
                        "com.watchsmart.wdmmi.ui.MainActivity");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleMMIWatchDataEntry", e);
                return false;
            }
        }
        return false;
    }

    /*
     * Add by NTD, to fix bug 4446
     */
    static boolean handleMMIOneResultEntry(Context context, String input){
        if(input.equals(MMI_STEP_ONE_RESULT)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("vendor.zhntd.factorytest", "vendor.zhntd.factorytest.FactoryTestResult");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("nv_index", 1);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleMMIOneResultEntry ", e);
                return false;
            }
        }
        return false;
    }

    /*
     * Add by NTD, to fix bug 4446
     */
    static boolean handleMMITwoResultEntry(Context context, String input){
        if(input.equals(MMI_STEP_TWO_RESULT)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("vendor.zhntd.factorytest", "vendor.zhntd.factorytest.FactoryTestResult");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("nv_index", 2);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleMMITwoResultEntry", e);
                return false;
            }
        }
        return false;
    }

    /*
     * Add by NTD, to fix bug 4446
     */
    static boolean handleFactoryResetEntry(Context context, String input) {
        if (input.equals(FACTORY_RESET)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("vendor.zhntd.factorytest",
                        "vendor.zhntd.factorytest.MasterClearActivity");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleFactoryResetEntry ", e);
                return false;
            }
        }
        return false;
    }

    /*
     * Add by NTD, to fix bug 4446
     */
    static boolean handleFactoryInfoEntry(Context context, String input) {
        if(input.equals(FACTORY_INFORMATION_DISPLAY)) {
            try {
                Intent intent = new Intent("vendor.zhntd.factorytest.factorynvresult");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleFactoryInfoEntry ", e);
                return false;
            }
        }
        return false;
    }

    /*
     * Add by NTD, to fix bug 6394
     */
    static boolean handleLaunchNTDSettings(Context context, String input) {
        if (input.equals(NTD_SETTINGS)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.zhntd.settings",
                        "com.zhntd.settings.SettingsActivity");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleLaunchNTDSettings ", e);
                return false;
            }
        }
        return false;
    }

    static boolean handleFactoryItemInfoEntry(Context context, String input) {
        if(input.equals(FACTORY_ITEM_INFO_DISPLAY)) {
            try {
                Intent intent = new Intent("vendor.zhntd.factorytest.factoryitemnvresult");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleFactoryItemInfoEntry", e);
                return false;
            }
        }
        return false;
    }

    static boolean handleUSBPortEntry(Context context, String input) {
        if (input.equals(MMI_USB_PORT_SWITCH)) {
            try {
                Intent intent = new Intent("android.intent.action.USB_PORT_SWITCH");
                //intent.addCategory(Intent.CATEGORY_DEFAULT);
                context.sendBroadcast(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "!!! sendBroadcast exception. handleUSBPortEntry ", e);
                return false;
            }
        }
        return false;
    }

    static boolean handleMMIThreeEntry(Context context, String input){
        if(input.equals(MMI_STEP_THREE)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("vendor.zhntd.factorytest", "vendor.zhntd.factorytest.FactoryTestActivity");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("nv_index", 3);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleMMIThreeEntry ", e);
                return false;
            }
        }
        return false;
    }

    static boolean handleMMIThreeResultEntry(Context context, String input){
        if(input.equals(MMI_STEP_THREE_RESULT)) {
            try {
                Intent intent = new Intent();
                intent.setClassName("vendor.zhntd.factorytest", "vendor.zhntd.factorytest.FactoryTestResult");
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("nv_index", 3);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "!!! start activity exception. handleMMIThreeResultEntry " ,e);
                return false;
            }
        }
        return false;
    }

    static boolean handlePinEntry(Context context, String input) {
        if ((input.startsWith("**04") || input.startsWith("**05")) && input.endsWith("#")) {
            try {
                //return ITelephony.Stub.asInterface(ServiceManager.getService("phone"))
                //        .handlePinMmi(input);
            } catch (Exception e) {
                Log.e(TAG, "Failed to handlePinMmi due to remote exception");
                return false;
            }
        }
        return false;
    }

    static boolean handleIMEIDisplay(Context context, String input, boolean useSystemWindow) {
        if(context == null) {
            // bugid:125998 -- maybe null in monkey test.
            // This happen under very special case.
            Log.e(TAG,"handleIMEIDisplay() -- context is null");
            return false;
        }
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null && input != null && MMI_IMEI_DISPLAY.equals(input)) {
            //#<!-- [[ YunOS BEGIN PB
            //##module:()  ##author:xiuneng.wpf@alibaba-inc.com
            //##BugID:(7866567)  ##date:2016-02-18 17:18 -->
            showIMEIPanel(context, useSystemWindow, telephonyManager);
            return true;
            //#<!-- YunOS END PB ]] -->

        }

        if (EXTERNAL_VERSION_DISPLAY.equals(input)) {
            showExternalVersion(context);
            return true;
        } else if (INTERNAL_VERSION_DISPLAY.equals(input)) {
            showInternalVersion(context);
            return true;
        }

        return false;
    }

    // TODO: Combine showIMEIPanel() and showMEIDPanel() into a single
    // generic "showDeviceIdPanel()" method, like in the apps/Phone
    // version of SpecialCharSequenceMgr.java.  (This will require moving
    // the phone app's TelephonyCapabilities.getDeviceIdLabel() method
    // into the telephony framework, though.)

    private static void showIMEIPanel(Context context, boolean useSystemWindow,
            TelephonyManager telephonyManager) {
        String title = "";
        String imeiStr = "";
		boolean isShowIMEI = true; //add by lichengfeng fix *#06# error @20160321 begin 
        if (telephonyManager.getPhoneCount() <= 1) {
            if(telephonyManager.getCurrentPhoneType() == TelephonyManager.PHONE_TYPE_CDMA){
                title = "MEID: ";
            } else {
                title = "IMEI: ";
            }
            imeiStr = title + telephonyManager.getDeviceId();
        } else {
			/*add by lichengfeng show MEID,IMEI1,IEMI2 in c2k version @20160224 begin */
			int mtkC2kSupport_id = Integer.valueOf(SystemProperties.get("ro.mtk_c2k_support","0"));
			android.util.Log.i("lcf_contact"," mtkC2kSupport_id: "+mtkC2kSupport_id);
			if (mtkC2kSupport_id == 1) { //c2k version
			   /* add by lichengfeng fix *#06# error @20160321 begin */
				isShowIMEI = false;
				Intent showMEIDIntent = new Intent("bird.intent.action.ShowMEID");
				//showMEIDIntent.putExtra("imeiStr",imeiStr);
				//showMEIDIntent.setClassName("com.yunos.alicontacts","com.yunos.alicontacts.activities.ShowMEIDActivity");
				//context.startActivity(showMEIDIntent);
				context.sendBroadcast(showMEIDIntent);
			  /* add by lichengfeng fix *#06# error @20160321 end */
			  /*String mImei1 = telephonyManager.getDeviceId(0);
		    if (mImei1 == null) {
		     	mImei1 = " ";
		    }
		    String mImei2 = telephonyManager.getDeviceId(1);
	    	if (mImei2 == null) {
			      mImei2 = " ";
		    }
			  imeiStr += "MEID: "+ getDeviceId(context)+"\nIMEI1: "+mImei1+"\nIMEI2: "+mImei2;*/
		    
			} else { // not c2k version
				for (int i = 0; i < telephonyManager.getPhoneCount(); i++) {
					int[] subIds = SubscriptionManager.getSubId(i);
					if(telephonyManager.getCurrentPhoneType(subIds[0]) == TelephonyManager.PHONE_TYPE_CDMA) {
						title = "MEID" + (i+1) + ": ";
					} else {
						title = "IMEI" + (i+1) + ": ";
					}
					imeiStr = imeiStr + title + telephonyManager.getDeviceId(i);
					if(i < telephonyManager.getPhoneCount() -1) {
						imeiStr = imeiStr + "\n";
					}
				}
			}
			/*add by lichengfeng show MEID,IMEI1,IEMI2 in c2k version @20160224 end */		
        }
        //#<!-- YunOS END PB ]] -->
		
		///add by lichengfeng fix *#06# error @20160321{
		if (isShowIMEI) {
		///@}
	        new AlertDialog.Builder(context)
                .setTitle(R.string.imei)
                .setMessage(imeiStr)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
		///add by lichengfeng fix *#06# error @20160321{
		}
		///@}
    }

    private static void showMEIDPanel(Context context, boolean useSystemWindow,
            TelephonyManager telephonyManager) {
        String meidStr = telephonyManager.getDeviceId();

        new AlertDialog.Builder(context)
                .setTitle(R.string.meid)
                .setMessage(meidStr)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    static private void showExternalVersion(Context context) {
        String externalVersion = SystemProperties.get("ro.build.display.id", "");

        new AlertDialog.Builder(context)
                .setTitle(R.string.external_version)
                .setMessage(externalVersion)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    static private void showInternalVersion(Context context) {
        String internalVersion = SystemProperties.get("ckt.internal.version", "");

        new AlertDialog.Builder(context)
                .setTitle(R.string.internal_version)
                .setMessage(internalVersion)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    /*******
     * This code is used to handle SIM Contact queries
     *******/
    private static final String ADN_PHONE_NUMBER_COLUMN_NAME = "number";
    private static final String ADN_NAME_COLUMN_NAME = "name";
    private static final int ADN_QUERY_TOKEN = -1;

    /**
     * Cookie object that contains everything we need to communicate to the
     * handler's onQuery Complete, as well as what we need in order to cancel
     * the query (if requested).
     *
     * Note, access to the textField field is going to be synchronized, because
     * the user can request a cancel at any time through the UI.
     */
    private static class SimContactQueryCookie implements DialogInterface.OnCancelListener{
        public ProgressDialog progressDialog;
        public int contactNum;

        // Used to identify the query request.
        private int mToken;
        private QueryHandler mHandler;

        // The text field we're going to update
        private EditText textField;

        public SimContactQueryCookie(int number, QueryHandler handler, int token) {
            contactNum = number;
            mHandler = handler;
            mToken = token;
        }

        /**
         * Synchronized getter for the EditText.
         */
        public synchronized EditText getTextField() {
            return textField;
        }

        /**
         * Synchronized setter for the EditText.
         */
        public synchronized void setTextField(EditText text) {
            textField = text;
        }

        /**
         * Cancel the ADN query by stopping the operation and signaling
         * the cookie that a cancel request is made.
         */
        @Override
        public synchronized void onCancel(DialogInterface dialog) {
            // close the progress dialog
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

            // setting the textfield to null ensures that the UI does NOT get
            // updated.
            textField = null;

            // Cancel the operation if possible.
            mHandler.cancelOperation(mToken);
        }
    }

    /**
     * Asynchronous query handler that services requests to look up ADNs
     *
     * Queries originate from {@link handleAdnEntry}.
     */
    private static class QueryHandler extends AsyncQueryHandler {

        private boolean mCanceled;

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /**
         * Override basic onQueryComplete to fill in the textfield when
         * we're handed the ADN cursor.
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            sPreviousAdnQueryHandler = null;
            if (mCanceled) {
                return;
            }

            SimContactQueryCookie sc = (SimContactQueryCookie) cookie;

            // close the progress dialog.
            sc.progressDialog.dismiss();

            // get the EditText to update or see if the request was cancelled.
            EditText text = sc.getTextField();

            // if the textview is valid, and the cursor is valid and postionable
            // on the Nth number, then we update the text field and display a
            // toast indicating the caller name.
            if ((c != null) && (text != null) && (c.moveToPosition(sc.contactNum))) {
                String name = c.getString(c.getColumnIndexOrThrow(ADN_NAME_COLUMN_NAME));
                String number = c.getString(c.getColumnIndexOrThrow(ADN_PHONE_NUMBER_COLUMN_NAME));

                // fill the text in.
                text.getText().replace(0, 0, number);

                // display the name as a toast
                Context context = sc.progressDialog.getContext();
                name = context.getString(R.string.menu_callNumber, name);
                Toast.makeText(context, name, Toast.LENGTH_SHORT)
                    .show();
            }
        }

        public void cancel() {
            mCanceled = true;
            // Ask AsyncQueryHandler to cancel the whole request. This will fails when the
            // query already started.
            cancelOperation(ADN_QUERY_TOKEN);
        }
    }
}
