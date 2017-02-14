package com.example.liuapidemos.broadcast;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.example.liuapidemos.R;

public class BroadcastSecondActivity extends BaseActivity {
	Button mButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.broadcast_second);
		mButton = (Button) findViewById(R.id.send);
		mButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent mIntent = new Intent("shit");
sendBroadcast(mIntent);
			}
		});
	}
}
