package com.example.liuapidemos.draggablewindow;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.Toast;

import com.example.liuapidemos.R;

public class DraggableWindowMainActivity extends Activity {
	Button mButton;
	WindowManager.LayoutParams mLayoutParams;
	WindowManager manager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.draggable_window_activity_main);
		mButton = new Button(this);
		mButton.setText("fuck");
		mButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
			Toast.makeText(DraggableWindowMainActivity.this, "you clicked btn", 0).show();
			}
		});
		manager = getWindowManager();
		mLayoutParams = new WindowManager.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0,
				PixelFormat.TRANSPARENT);
		/*
		 * flags一定要设置，一般是FLAG_NOT_TOUCH_MODEL.在此模式下，系统会将
		 * 当前Window区域外的单击事件传递给底层的Window,当前Window区域以内的单击事件则自己处理。
		 */
		//mLayoutParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL;
		mLayoutParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL|LayoutParams.FLAG_NOT_FOCUSABLE;
		mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		mLayoutParams.x = 100;
		mLayoutParams.y = 300;
		manager.addView(mButton, mLayoutParams);
		mButton.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				int width=v.getMeasuredWidth();
				int height=v.getMeasuredHeight();
				int x = (int) event.getRawX();
				int y = (int) event.getRawY();
				switch (event.getAction()) {

				case MotionEvent.ACTION_DOWN:
					break;
				case MotionEvent.ACTION_MOVE:
					mLayoutParams.x = x-width/2;
					mLayoutParams.y = y-height;
					manager.updateViewLayout(mButton, mLayoutParams);
					break;
				case MotionEvent.ACTION_UP:
					break;

				default:
					break;
				}
				return false;
			}
		});
	}
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		Toast.makeText(this, "backpressed", 0).show();
	}
}
