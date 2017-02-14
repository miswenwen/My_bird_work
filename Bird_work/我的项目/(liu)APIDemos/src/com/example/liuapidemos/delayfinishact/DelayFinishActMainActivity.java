package com.example.liuapidemos.delayfinishact;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.liuapidemos.R;

public class DelayFinishActMainActivity extends Activity {
	Button mButton1;
	Button mButton2;
	Button mButton3;
	Button mButton4;
	Button mButton5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.delay_finish_act_activity_main);
		mButton1 = (Button) findViewById(R.id.button1);
		mButton2 = (Button) findViewById(R.id.button2);
		mButton3 = (Button) findViewById(R.id.button3);
		mButton4 = (Button) findViewById(R.id.button4);
		mButton5 = (Button) findViewById(R.id.button5);
		// 直接跳转
		mButton1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent mIntent = new Intent(DelayFinishActMainActivity.this, DelayFinishActSecAct.class);
				startActivity(mIntent);
			}
		});
		// 开子线程，停子线程
		mButton2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Intent mIntent = new Intent(DelayFinishActMainActivity.this,
								DelayFinishActSecAct.class);
						startActivity(mIntent);
					}
				}).start();

			}
		});
		// 停主线程，这种写法有ｂｕｇ，主线程停不了
		mButton3.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Intent mIntent = new Intent(DelayFinishActMainActivity.this, DelayFinishActSecAct.class);
				startActivity(mIntent);
			}
		});
		// new Handler().postDelay(Runnable,long)
		mButton4.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				new Handler().postDelayed(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Intent mIntent = new Intent(DelayFinishActMainActivity.this,
								DelayFinishActSecAct.class);
						startActivity(mIntent);
					}
				}, 3000);
			}
		});
		//new Timer.schedual()
		mButton5.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Timer mTimer = new Timer();
				mTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Intent mIntent = new Intent(DelayFinishActMainActivity.this,
								DelayFinishActSecAct.class);
						startActivity(mIntent);
					}
				}, 3000);
			}
		});
	}

}
