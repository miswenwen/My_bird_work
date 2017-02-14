package com.example.liuapidemos.timerpicker;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.example.liuapidemos.R;
import com.example.liuapidemos.timerpicker.TimerPickerView.onSelectListener;



/**
 * 更多详解见博客http://blog.csdn.net/zhongkejingwang/article/details/38513301
 * 
 * @author chenjing
 * 
 */
public class TimerPickerMainActivity extends Activity
{

	TimerPickerView minute_pv;
	TimerPickerView second_pv;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timer_picker_activity_main);
		minute_pv = (TimerPickerView) findViewById(R.id.minute_pv);
		second_pv = (TimerPickerView) findViewById(R.id.second_pv);
		List<String> data = new ArrayList<String>();
		List<String> seconds = new ArrayList<String>();
		for (int i = 0; i < 10; i++)
		{
			data.add("0" + i);
		}
		for (int i = 0; i < 60; i++)
		{
			seconds.add(i < 10 ? "0" + i : "" + i);
		}
		minute_pv.setData(data);
		minute_pv.setOnSelectListener(new onSelectListener()
		{

			@Override
			public void onSelect(String text)
			{
				Toast.makeText(TimerPickerMainActivity.this, "选择了 " + text + " 分",
						Toast.LENGTH_SHORT).show();
			}
		});
		second_pv.setData(seconds);
		second_pv.setOnSelectListener(new onSelectListener()
		{

			@Override
			public void onSelect(String text)
			{
				Toast.makeText(TimerPickerMainActivity.this, "选择了 " + text + " 秒",
						Toast.LENGTH_SHORT).show();
			}
		});
		minute_pv.setSelected(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
