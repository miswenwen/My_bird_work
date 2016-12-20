package com.example.liuactivitylog;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
	// MyView myView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("MainActivity", "onCreate");
		setContentView(R.layout.activity_main);
		MyView myView = (MyView) findViewById(R.id.view);
//		 new Thread(new Runnable() {
//		
//		 @Override
//		 public void run() {
//		 // TODO Auto-generated method stub
//		 try {
//		 Thread.sleep(1000);
//		 finish();
//		 } catch (InterruptedException e) {
//		 // TODO Auto-generated catch block
//		 e.printStackTrace();
//		 }
//		 myView.postInvalidate();
//		 }
//		 }).start();


	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Log.e("MainActivity", "onStart");
		
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.e("MainActivity", "onResume");
		
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.e("MainActivity", "onStop");
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		Log.e("MainActivity", "onRestart");
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.e("MainActivity", "onDestroy");
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.e("MainActivity", "onPause");
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		Log.e("MainActivity", "onWindowFocusChanged:" + hasFocus);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		Log.e("MainActivity", "onSaveInstanceState");
	}

	@Override
	protected void onPostResume() {
		// TODO Auto-generated method stub
		super.onPostResume();
		Log.e("MainActivity", "onPostResume");
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
		Log.e("MainActivity", "onRestoreInstanceState");
	}
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		Log.e("MainActivity", "onBackPressed");
	}
	@Override
	//android:configChanges="orientation|screenSize" AndroidManifest中增加这句后才会调用，防止重新创建Activity
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.e("MainActivity", "onConfigurationChanged");
	}
	@Override
	public void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		Log.e("MainActivity", "onAttachedToWindow");
	}
	@Override
	public void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		Log.e("MainActivity", "onDetachedFromWindow");
	}
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onPostCreate(savedInstanceState);
		Log.e("MainActivity", "onPostCreate");
	}
}
