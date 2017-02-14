package com.example.liuapidemos.digitalclock;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.widget.TextView;

import com.example.liuapidemos.R;

@SuppressWarnings("deprecation")
public class DigitalClockMainActivity extends Activity {
	TextView mTextView;
	Handler mHandler;
	String time;
	Timer mTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.digital_clock_activity_main);
		mTextView = (TextView) findViewById(R.id.tv);
		mHandler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				Time mTime = new Time();
				mTime.setToNow();
				int hour = mTime.hour;
				int minute = mTime.minute;
				int second = mTime.second;
				int year = mTime.year;
				int month = mTime.month + 1;
				int day = mTime.monthDay;
				time = year + "年" + month + "月" + day + "日" + hour + ":"
						+ minute + ":" + second + "";
				 Log.e("", mTime.getCurrentTimezone()+"");
				 Log.e("", "现在是一年中的第" + mTime.yearDay + "天");
				 Log.e("","现在是一年中的第" + mTime.getWeekNumber() + "周");
				 Log.e("", "现在是星期" + mTime.weekDay);
				 Log.e("",mTime.format("%Y-%m-%d %H:%M:%S"));
				 Log.e("", time+"");
				mTextView.setText(time);
				// // 获取Calendar实例
				// Calendar calendar = Calendar.getInstance();
				// // 输出日期信息，还有许多常量字段，我就不再写出来了
				// Log.e("",
				// calendar.get(Calendar.YEAR) + "年"
				// + calendar.get(Calendar.MONTH) + "月"
				// + calendar.get(Calendar.DAY_OF_MONTH) + "日"
				// + calendar.get(Calendar.HOUR_OF_DAY) + "时"
				// + calendar.get(Calendar.MINUTE) + "分"
				// + calendar.get(Calendar.SECOND) + "秒"
				// + "\n今天是星期"
				// + calendar.get(Calendar.DAY_OF_WEEK) + "是今年的第"
				// + calendar.get(Calendar.WEEK_OF_YEAR) + "周");
				// // 格式化输出日期,在这个方法中，时间显示是12小时制的，如果需要显示24小时制，只需将hh换成kk
				// Log.e("",
				// DateFormat.format("yyyy-MM-dd kk:mm:ss",
				// calendar.getTime()).toString());
				// Log.e("", "" + calendar.getTime().getTime());
				// Log.e("", "" + System.currentTimeMillis());
			}

		};
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mHandler.sendEmptyMessage(0);
			}
		}, 50, 50);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mTimer.cancel();// 注意回收
	}
}
