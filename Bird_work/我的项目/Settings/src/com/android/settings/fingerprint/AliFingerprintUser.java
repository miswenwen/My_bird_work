package com.android.settings.fingerprint;

import android.hardware.fingerprint.Fingerprint;

public class AliFingerprintUser {

    public static final boolean DEBUG = true;
    
    private Fingerprint mFingerprint;
    private int mFingerQuickOperation;
    private String mFingerQuickTarget;
    private String mFingerQuickTargetData;
    public AliFingerprintUser(Fingerprint fingerprint) {
    	mFingerprint = fingerprint;
    }

    public void setFingerPrint(Fingerprint fingerprint) {
    	mFingerprint = fingerprint;
    }
    public Fingerprint getFingerPrint() {
    	return mFingerprint;
    } 
    public void setFingerQuickOperation(int operation) {
        if(operation == AliFingerprintUtils.FINGERQUICK_TYPE_DIALOUT
                || operation == AliFingerprintUtils.FINGERQUICK_TYPE_STARTAPP
                || operation == AliFingerprintUtils.FINGERQUICK_TYPE_FINGERPAY){
            mFingerQuickOperation = operation;
        } else {
            mFingerQuickOperation = AliFingerprintUtils.FINGERQUICK_TYPE_DISABLE;
        }
    }
    public void setFingerQuickTarget(String target) {
        mFingerQuickTarget = target;
    }
    public void setFingerQuickTargetData(String data) {
        mFingerQuickTargetData = data;
    }
    public int getFingerQuickOperation() {
        return mFingerQuickOperation;
    }
    public String getFingerQuickTarget() {
        return mFingerQuickTarget;
    }
    public String getFingerQuickTargetData() {
        return mFingerQuickTargetData;
    }

}
