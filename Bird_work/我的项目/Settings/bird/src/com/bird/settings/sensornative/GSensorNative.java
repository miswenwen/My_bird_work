package com.bird.settings.sensornative;

public class GSensorNative {
	static {
		System.loadLibrary("gsensor_jni");
	}
	public static native boolean opendev();
	public static native boolean closedev();
	public static native boolean gsensor_calibration();
}


