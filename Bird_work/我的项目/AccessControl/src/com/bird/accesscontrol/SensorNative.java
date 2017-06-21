/**
 * Copyright 2011 Fingerprint Cards AB
 */
package com.bird.accesscontrol;

import java.io.IOException;
import java.lang.InterruptedException;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * API for handling the fpc1080 / BTP and biometric algorithm.
 * Currently the beta version of "30 degree" algorithm (BLApi) is used.
 * @author fpc
 *
 */
public final class SensorNative {
	
	private static final String TAG = "SensorNative";
	static {
		System.loadLibrary("fpc1080_jni");
		Init();
	} 
	
	/**
	 *  Enrolment / Verification was successful
	 */
	public static final int MSG_SUCCESSFUL = 0;
	/**
	 * Enrolment / Verification has failed
	 */
	public static final int MSG_FAILURE = 1;
	/**
	 * User performed a good swipe of the finger print sensor
	 */
	public static final int MSG_GOOD_SWIPE = 2;
	/**
	 * User performed a swipe of questionable quality
	 */
	public static final int MSG_QUESTIONABLE_SWIPE = 3;
	/**
	 * The user has placed a finger on the sensor / begun swipe
	 */
	public static final int MSG_FINGER_PRESENT = 4;
	/**
	 * Waiting for user to interact with sensor.
	 */
	public static final int MSG_WAITING_FOR_SWIPE = 5;
	/**
	 * Processing of image data has begun
	 */
	public static final int MSG_PROCESSING = 6;
	/**
	 * The user is currently swiping their finger over the sensor
	 */
	public static final int MSG_SWIPE_PROGRESS = 7;
	
	/**
	 * During enrolment process the user swiped the wrong finger: a finger that was already
	 * registered, or in a sequence of swipes a finger that was different from the previous ones.
	 */
	public static final int MSG_WRONG_FINGER = 8;
	
	public static final int SWIPE_GOOD = 0;
	public static final int SWIPE_TOO_FAST = 1;
	public static final int SWIPE_TOO_MUCH_ANGLE = 2;
	public static final int SWIPE_TOO_SHORT = 3;
	public static final int SWIPE_NOT_CENTERED = 4;
	
	public static final String[] SWIPE_QUALITY_STRINGS = {
		"good swipe",
		"too fast",
		"too skewed",
		"too short",
		"not centered",
	};
	
	private static Thread mThread;
	private static Thread mPollingThread;
	
	private SensorNative() {}
	
	private static void exitThreads() throws InterruptedException {
		if (mThread != null) {
			SetShouldExit(true);
			mThread.join();
			mThread = null;
		}
		exitPollingThread();
	}
	
	private static void exitPollingThread() {
		if (mPollingThread != null) {
			mPollingThread.interrupt();
			mPollingThread = null;
		}
	}
	
	private static class PollTask implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					return;
				}
				if (Thread.currentThread().isInterrupted())
					return;
				
				mCallback.getLooper().getThread().isAlive();
				mCallback.sendMessage(mCallback.obtainMessage(MSG_SWIPE_PROGRESS, GetY(), 0));
			}	
		}
	}
	
	public static synchronized void startEnrol(Handler callback, final int finger) {
		startEnrol(callback, null, finger);
	}
	
	public static synchronized void startEnrol(Handler callback, final String fileName) {
		startEnrol(callback, fileName, 0);
	}
	
	/**
	 * Start asynchronous enrolment procedure.
	 * Asynchronously captures a set of swipes from the sensor and generates a finger print template. The procedure
	 * can be stopped with {@link abort()}.
	 * @param callback a Handler to receive updates from the enrolment procedure.
	 * the procedure is complete when {@link MSG_SUCCESSFUL} or 
	 * {@link MSG_FAILURE} is sent to the handler.
	 * @param fileName the absolute path to the location of the template data. If no template data exists it will
	 * be created.
	 * @param fIndex the slot in the template data to enrol to.
	 */
	public static synchronized void startEnrol(Handler callback, final String fileName, final int fIndex) {	
		try {
			exitThreads();
		} catch (InterruptedException e) {
			Log.e(TAG,"interrupted while waiting for threads to finish.");
			e.printStackTrace();
			return;
		}

		mCallback = callback;
		SetShouldExit(false);
		mPollingThread = new Thread(new PollTask());
		mThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				boolean result = false;
				try {
					
					result = EnrolNative(fileName, fIndex);
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					exitPollingThread();
					return;
				}
				exitPollingThread();
				Message m = mCallback.obtainMessage(result ? MSG_SUCCESSFUL : MSG_FAILURE, fIndex, 0);
				mCallback.sendMessage(m);
				mCallback = null;
			}
		});
		mThread.start();
	}
	
	/**
	 * Start asynchronous verification procedure.
	 * Asynchronously captures one swipe from the sensor and matches it with the template data pointed to by fileNames.
	 * can be stopped with {@link abort()}.
	 * @param callback a Handler to receive updates from the verification procedure.
	 * the procedure is complete when {@link MSG_SUCCESSFUL} or 
	 * {@link MSG_FAILURE} is sent to the handler. When successful the index in the template data that was matched is
	 * returned as arg1 of the Message object.
	 * @param fileNames the absolute path to the location of the template data.
	 */
	public static synchronized void startVerify(Handler callback, final String[] fileNames) {
		try {
			exitThreads();
		} catch (InterruptedException e) {
			Log.e(TAG,"interrupted while waiting for threads to finish.");
			e.printStackTrace();
			return;
		}
		
		for (String s : fileNames) {
			Log.d(TAG, "verify cand: " + s);
		}
		mCallback = callback;
		SetShouldExit(false);
		mPollingThread = new Thread(new PollTask());
		mThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Log.d(TAG, "mThread.run()");
				int result = -1;
				try {				
					result = VerifyNative(fileNames);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					exitPollingThread();
					return;
				}
				exitPollingThread();
				if (result < 0) {
					mCallback.sendEmptyMessage(MSG_FAILURE);
				} else {
					int winningIndex = DataUtil.templateNameToIndex(fileNames[result]);
					Message m = mCallback.obtainMessage(MSG_SUCCESSFUL, winningIndex, 0);
					mCallback.sendMessage(m);
				}
				mCallback = null;
			}
		});
		mThread.start();
	}
	
	/**
	 * abort enrolment / verification procedure.
	 */
	public static synchronized void abort() {
		Log.d(TAG, "abort() called");
		try {
			exitThreads();
		} catch (InterruptedException e) {
			Log.e(TAG,"interrupted while waiting for threads to finish.");
			e.printStackTrace();
			return;
		}
	}
	
	private static Handler mCallback;
	

	/**
	 * called from native library.
	 */
	private static void notifyWaiting() {
		if (mCallback == null)
			return;
		mCallback.sendEmptyMessage(MSG_WAITING_FOR_SWIPE);
	}
	/**
	 * called from native library
	 */
	private static void notifyFingerPresent() {
		if (mCallback == null)
			return;
		mCallback.sendEmptyMessage(MSG_FINGER_PRESENT);
		mPollingThread.start();
	}
	/**
	 * called from native library.
	 */
	private static void notifyProcessing() {
		if (mCallback == null)
			return;
		
		exitPollingThread();
		mPollingThread = new Thread(new PollTask());
		mCallback.sendEmptyMessage(MSG_PROCESSING);
	}
	/**
	 * called from native library.
	 */
	private static void notifyGoodSwipe() {
		mCallback.sendEmptyMessage(MSG_GOOD_SWIPE);
	}

	/**
	 * called from native library.
	 */
	private static void notifyQuestionalbeSwipe() {
		mCallback.sendEmptyMessage(MSG_QUESTIONABLE_SWIPE);
	}
	
	/**
	 * called from native library.
	 */
	private static void notifyWrongFinger() {
		mCallback.sendEmptyMessage(MSG_WRONG_FINGER);
	}
	/**
	 *  initialize native class
	 */

	private native static void Init();
	/**
	 * set the exit condition flag for native library
	 * @param should_exit set to true if native function should return.
	 */
	public native static void SetShouldExit(boolean should_exit);
	
	/**
	 * set sensor ADC gain parameter
	 * @param gain ADC gain parameter
	 */
	public native static void SetAdcGain(byte gain);
	/**
	 * set sensor ADC offset parameter
	 * @param offset ADC offset parameter
	 */
	public native static void SetAdcOffset(byte offset);
	/**
	 * set sensor ADC pixel setup parameter
	 * @param pixel_setup ADC pixel setup parameter
	 */
	public native static void SetAdcPixelSetup(byte pixel_setup);


	/**
	 * Capture images from the sensor and enrol to a template slot (30 degree algorithm)
	 * @param fileName absolute path pointing to the file that should / already contains template data.
	 * @param index the index in the template data to place the template (0 - 4). 
	 * @return true if successful.
	 * @throws IOException when communication with the sensor has failed.
	 */
	private native static boolean EnrolNative(String fileName, int index) throws IOException, InterruptedException;	
	
	/**
	 * Capture images from the sensor and match against template data.
	 * @param fileName absolute path the file containing template data.
	 * @return the zero based index of the successful match or -1 if no match was possible.
	 * @throws IOException when communication with the sensor has failed.
	 */
	private native static int VerifyNative(String[] fileNames) throws IOException , InterruptedException;

	/**
	 * get the global Y coordinate of ongoing swipe.
	 * @return global y value.
	 */
	public native static int GetY();
	/**
	 * get the global x coordinate of ongoing swipe.
	 * @return global x value.
	 */
	public native static int GetX();
	/**
	 * currently not implemented
	 */
	public native static boolean CaptureTest(Bitmap bitmap) throws IOException , InterruptedException;
	/**
	 * Set the security level of 30 degree algorithm.
	 * @param threshold security level (0 - 7)
	 */
	public native static void SetSecurityThreshold(int threshold);
	
	/**
	 * Tests the sensor pixel array (production test), and returns true if the pixel array is ok.
	 * This function is only appropriate to use during production testing.
	 * @return true if the sensor passed the test.
	 */
	public native static boolean PixelTest() throws IOException;
	
	/**
	 * Captures a swipe and evaluates if it was a good swipe.
	 * @return integer array [swipe status, swipe speed (cm/s)]
	 * @throws IOException sensor communication failed.
	 * @throws InterruptedException process was interrupted.
	 */
	public native static int SwipeTest() throws IOException, InterruptedException;
	
	/**
	 * Sensor self test.
	 * Make the sensor perform a self test to verify that it is functional.
	 * @return true if the sensor is working properly.
	 */
	public static native boolean SelfTest();
	
	public native static boolean CaptureSingle(Bitmap bitmap) throws IOException , InterruptedException;
}
