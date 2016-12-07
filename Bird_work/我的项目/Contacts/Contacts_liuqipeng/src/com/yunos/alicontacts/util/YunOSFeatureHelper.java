package com.yunos.alicontacts.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.aliyun.ams.secure.BlackHelper;
import com.aliyun.ams.secure.SecureManager;
import com.aliyun.osupdate.IOsUpdateServiceForApp;

public class YunOSFeatureHelper {

    private static final String TAG = "YunOSFeatureHelper";

    private static final String PRIVACY_SPACE_PACKAGE = "com.aliyun.SecurityCenter";
    private static final String PRIVACY_SPACE_CLASS = "com.aliyun.SecurityCenter.PrivacySpace.PrivacySpaceHome";

    public static boolean isBlack(Context context, String number) {
        BlackHelper bh = BlackHelper.get(context);
        if (bh == null) {
            Log.e(TAG, "isBlack: Cannot get instance for BlackHelper with context "+context);
            return false;
        }
        boolean result = bh.isBlack(number);
        Log.i(TAG, "isBlack: result="+result);
        return result;
    }

    public static boolean addBlack(Context context, String number, int type) {
        BlackHelper bh = BlackHelper.get(context);
        if (bh == null) {
            Log.e(TAG, "addBlack: Cannot get instance for BlackHelper with context "+context);
            return false;
        }
        boolean result = bh.addBlack(number, null, type);
        Log.i(TAG, "addBlack: result="+result);
        return result;
    }

    public static boolean removeBlack(Context context, String number) {
        BlackHelper bh = BlackHelper.get(context);
        if (bh == null) {
            Log.e(TAG, "removeBlack: Cannot get instance for BlackHelper with context "+context);
            return false;
        }
        boolean result = bh.removeBlack(number);
        Log.i(TAG, "removeBlack: result="+result);
        return result;
    }

    public static boolean reportCallType(Context context,
            String address, long date, long time, int type, boolean addBlack, String numDesc) {
        SecureManager sm = SecureManager.get(context);
        if (sm == null) {
            Log.i(TAG, "reportCallType: Cannot get instance for SecureManager with context "+context);
            return false;
        }
        sm.reportCallType(address, date, time, type, addBlack, numDesc);
        Log.i(TAG, "reportCallType: reported call type.");
        return true;
    }

    public static boolean reportSms(Context context,
            String address, String body, long date, int type, boolean addBlack, String numDesc) {
        SecureManager sm = SecureManager.get(context);
        if (sm == null) {
            Log.i(TAG, "reportSms: Cannot get instance for SecureManager with context "+context);
            return false;
        }
        sm.reportSmsType(address, body, date, type, addBlack, numDesc);
        Log.i(TAG, "reportSms: reported sms.");
        return true;
    }

    public static boolean addNumbersToPrivateContact(Context context, String[] numbers, String name) {
        boolean success = false;
        SecureManager sm = SecureManager.get(context);
        if (sm == null) {
            Log.i(TAG, "addNumbersToPrivateContact: Cannot get instance for SecureManager with context "+context);
            return false;
        }
        if (sm.isPrivacyOpen()) {
            int result = sm.addPrivacyContact(numbers, name);
            success = result == 0;
            Log.i(TAG, "addNumbersToPrivateContact: result from secure manager is "+result);
        } else {
            Log.d(TAG, "addNumbersToPrivateContact: privacy is not open.");
            startPrivacySpaceHome(context, numbers, name);
            success = false;
        }
        return success;
    }

    private static void startPrivacySpaceHome(Context context, String[] numbers, String name) {
        Intent intent = new Intent();
        intent.setClassName(PRIVACY_SPACE_PACKAGE, PRIVACY_SPACE_CLASS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SecureManager.SEC_EXTRA_TYPE, SecureManager.SEC_EXTRA_TYPE_COMM);
        intent.putExtra(SecureManager.SEC_EXTRA_OP, SecureManager.SEC_EXTRA_OP_ADD);
        intent.putExtra(SecureManager.SEC_EXTRA_NAME, name);
        intent.putExtra(SecureManager.SEC_EXTRA_MULTI_NUMBER, numbers);
        Log.d(TAG, "startPrivacySpaceHome: startActivity PrivacySpaceHome.");
        context.startActivity(intent);
    }

    private static final String YUNOS_ALICONTACTS_PACKAGE_NAME = "com.yunos.alicontacts";
    private static final String YUNOS_OS_UPDATE_SERVICE_ACTION = "OsUpdateServiceForApp";
    private static final String YUNOS_OS_UPDATE_SERVICE_PACKAGE_NAME = "com.aliyun.fota";
    private static final String YUNOS_OS_UPDATE_SERVICE_CLASS_NAME = "com.aliyun.fota.osupdateservice.OsUpdateServiceForApp";

    /**
     * This class is used to isolate the YunOS interface from normal Contacts code.
     * It starts the service to check application update.
     */
    public static class OSUpdateCheckProxy {
        private final Context mContext;
        private ServiceConnection mConnection;
        private IOsUpdateServiceForApp mService;

        public OSUpdateCheckProxy(Context context) {
            mContext = context;
        }

        public void bindOSUpdateCheckService() {
            if (mConnection == null) {
                mConnection = new ServiceConnection() {

                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        long start = System.currentTimeMillis();
                        try {
                            mService = IOsUpdateServiceForApp.Stub.asInterface(service);
                            if (mService != null) {
                                mService.checkUpdate(YUNOS_ALICONTACTS_PACKAGE_NAME);
                            }
                            Log.d(TAG, "[bindOSUpdateCheckService] onServiceConnected() time-consuming:"
                                    + (System.currentTimeMillis() - start));
                        } catch (Exception e) {
                            Log.e(TAG, "[bindOSUpdateCheckService] onServiceConnected()", e);
                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        mService = null;
                        Log.d(TAG, "[bindOSUpdateCheckService] onServiceDisconnected()");
                    }

                };
            }

            try {
                Intent intent = new Intent(YUNOS_OS_UPDATE_SERVICE_ACTION);
                intent.setClassName(YUNOS_OS_UPDATE_SERVICE_PACKAGE_NAME,
                        YUNOS_OS_UPDATE_SERVICE_CLASS_NAME);
                boolean result = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                Log.i(TAG, "bindOSUpdateCheckService: bind result="+result);
            } catch (Exception e) {
                Log.e(TAG, "[bindOSUpdateCheckService] bindService", e);
            }
        }

        public void stopCheck() {
            Log.d(TAG, "OSUpdateCheckProxy.stopCheck:");
            if (mConnection != null && mService != null) {
                mContext.unbindService(mConnection);
                mConnection = null;
            }
        }

    }

}
