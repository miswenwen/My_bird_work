package com.example.liuapidemos.smallball;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import com.example.liuapidemos.R;

public class SmallBallMainActivity extends Activity {
	SmallBallView myView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.smallball_main);
		myView = (SmallBallView) findViewById(R.id.shit);
		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				myView.postInvalidate();
			}
		}, 200, 10);

	}

}
