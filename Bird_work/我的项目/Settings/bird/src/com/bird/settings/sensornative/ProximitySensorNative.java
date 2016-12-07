package com.bird.settings.sensornative;

public class ProximitySensorNative {
	static {
		System.loadLibrary("proximityjni");
	}
	public native static boolean calibrateSensor();
	/*static boolean calibrateSensor(){
		return true;
	}*/
}
