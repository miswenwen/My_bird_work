package com.example.mytetris;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

public class MainActivity extends Activity {
int  a=5;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
		Thread x = new Thread(new Runnable() {// 游戏启动后停一会进入游戏

					@Override
					public void run() {
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Intent intent = new Intent();
						intent.setClass(MainActivity.this, MenuActivity.class);
						startActivity(intent);
					}
				});
		x.start();
	}
	protected void onStop() {
		super.onStop();
		finish();
	}
}
