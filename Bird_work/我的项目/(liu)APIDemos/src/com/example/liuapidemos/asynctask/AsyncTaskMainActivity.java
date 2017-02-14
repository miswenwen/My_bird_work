package com.example.liuapidemos.asynctask;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.liuapidemos.R;

public class AsyncTaskMainActivity extends Activity {
	Button mButton;
	ProgressBar mProgressBar;
	TextView mTextView1;
	TextView mTextView2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.asynctask_main);
		mButton = (Button) findViewById(R.id.my_button);
		mProgressBar = (ProgressBar) findViewById(R.id.progress);
		mTextView1 = (TextView) findViewById(R.id.textview1);
		mTextView2 = (TextView) findViewById(R.id.textview2);
		mButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				MyAsyncTask myAsyncTask = new MyAsyncTask();
				myAsyncTask.execute(1314);// 这里的execute()传递进去的参数就是doInBackground中的参数。理论上什么类型都行？
			}
		});
	}

	class MyAsyncTask extends AsyncTask<Integer, Integer, String> {
		@Override
		protected void onPreExecute() {

		};

		@Override
		protected void onProgressUpdate(Integer... values) {// 这里是数组，类型由doInBackground传递过来。doInBackground中每调用一次publishProgress()方法就执行一次
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
			mProgressBar.setProgress(values[0]);

			mTextView1.setText("现在的进度为" + values[0] + "%");
		}

		protected String doInBackground(Integer... mm) {
			// TODO Auto-generated method stub
			int a = mProgressBar.getProgress();
			int i;
			for (i = a; i <= 100; i += 10) {
				publishProgress(i);
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			return mm[0].intValue() + "sb";
		}

		@Override
		protected void onPostExecute(String result) {// 这里的不是数组，doInBackground这个方法执行完后传递过来。
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mTextView2.setText(result);
		}
	}

}