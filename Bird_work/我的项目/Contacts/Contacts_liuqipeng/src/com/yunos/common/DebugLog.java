package com.yunos.common;

import android.util.Log;

public class DebugLog {
	private static final boolean DEBUG = true;
	private static final boolean VERBOSE = false;
	
    public static void d(String Tag, String logContent) {
        if(DEBUG) {
            Log.d(Tag, logContent);
        }
    }
    
    public static void v(String Tag, String logContent) {
        if(VERBOSE) {
            Log.d(Tag, logContent);
        }
    }
}
