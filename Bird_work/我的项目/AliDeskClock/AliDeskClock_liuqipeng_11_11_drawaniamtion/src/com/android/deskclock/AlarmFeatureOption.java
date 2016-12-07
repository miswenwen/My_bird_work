package com.android.deskclock;

import android.os.SystemProperties;
import android.util.Log;

public class AlarmFeatureOption {

    private static final String TAG = "AlarmFeatureOption";
    private static final String QCOM = "qcom";
    private static final String NEXUS5 = "hammerhead";
    private static final String MTK = "mt";
    private static final String SPREADTRUM = "sp";
    private static final String sHardWare = SystemProperties.get("ro.hardware");
    public static final boolean YUNOS_MTK_PLATFORM = isMtkPlatform();
    public static final boolean YUNOS_QCOM_PLATFORM = isQcomPlatform();
    private static boolean isMtkPlatform() {
        Log.d(TAG,"sHardWare-isMtkPlatform = "+sHardWare + ", sHardWare.contains(MTK) = "+sHardWare.contains(MTK));
        return sHardWare != null && sHardWare.contains(MTK);
    }

    private static boolean isQcomPlatform() {
        Log.d(TAG,"sHardWare-isQcomPlatform = "+sHardWare + ", sHardWare.contains(QCOM) = "+sHardWare.contains(QCOM)
            +", sHardWare.contains(NEXUS5) = "+sHardWare.contains(NEXUS5));
        return sHardWare != null && (sHardWare.contains(QCOM) || sHardWare.contains(NEXUS5));
    }
}
