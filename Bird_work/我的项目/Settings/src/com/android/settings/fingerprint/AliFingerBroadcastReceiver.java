package com.android.settings.fingerprint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AliFingerBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if(packageName != null && !packageName.isEmpty()) {
                AliFingerprintUtils.CheckAndDeleteFingerquickSettingByPackageName(context, packageName);
            }
        }
    }

}

