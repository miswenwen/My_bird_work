package com.example.liuapidemos.handler;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.liuapidemos.R;

public class HandlerMainActivity extends Activity implements OnClickListener {
	Button mButton;
	TextView mTextView;
	Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handler_main);
		mButton = (Button) findViewById(R.id.change_text);
		mTextView = (TextView) findViewById(R.id.text);
		mButton.setOnClickListener(this);
		mHandler = new Handler() {
			@Override
			// handleMessage(Message msg)源码中是个空方法
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				super.handleMessage(msg);
				switch (msg.what) {
				case 1:
					mTextView.setText("shit");
					break;

				default:
					break;
				}
			}
		};
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.change_text:
			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					// xxx;//执行耗时程序
					Message message = new Message();
					message.what = 1;
					mHandler.sendMessage(message);
				}
			}).start();
			break;

		default:
			break;
		}
	}
}