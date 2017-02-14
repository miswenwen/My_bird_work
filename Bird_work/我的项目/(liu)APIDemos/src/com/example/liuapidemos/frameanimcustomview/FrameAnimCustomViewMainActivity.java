package com.example.liuapidemos.frameanimcustomview;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import com.example.liuapidemos.R;

public class FrameAnimCustomViewMainActivity extends Activity {
FrameAnimCustomView myView;
FrameAnimCustomView myView1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.frameanimcustomview_main);
		myView=(FrameAnimCustomView)findViewById(R.id.shit);
		new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
				myView.postInvalidate();
				// TODO Auto-generated method stub
				
			}
		}, 200, 300);
		myView1=(FrameAnimCustomView)findViewById(R.id.shit1);
		new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
				myView1.postInvalidate();
				// TODO Auto-generated method stub
				
			}
		}, 200, 75);
	}

}
