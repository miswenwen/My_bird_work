package com.android.deskclock;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.SparseIntArray;

import yunos.ui.util.DynColorSetting;

public class AliDeskClockApp extends Application {

    private DynColorSetting mDynColorSetting;

    @Override
    public void onCreate() {
        mDynColorSetting = new DynColorSetting(getResources().getConfiguration());
        DynColorSetting.setColorIDReady(getResources(), getPackageName());
        overlayDynColorRes(getResources(), mDynColorSetting);
        super.onCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!mDynColorSetting.isSameColorMap(newConfig)) {
            mDynColorSetting.updateColorMap(newConfig);
            overlayDynColorRes(getResources(), mDynColorSetting);
        }
        super.onConfigurationChanged(newConfig);
    }

    public void overlayDynColorRes(Resources res, DynColorSetting dynColorSetting) {
        DynColorSetting.clearNewResArray(res);
        if (!dynColorSetting.isRestoreMode()) {
            SparseIntArray newColors = new SparseIntArray();
            DynColorSetting.setNewAUIDynColorRes(this, dynColorSetting, newColors);
        }
    }
}
