package com.example.liuapidemos.checkboxtogglebtnswitchdefault;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.liuapidemos.R;

public class CheckBoxToggleBtnSwitchDefaultMainActivity extends Activity {
	// //对控件对象进行声明
	private TextView textView;
	private CheckBox checkbox1;
	private CheckBox checkbox2;
	Switch mSwitch;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.checkboxtogglebtnswitchdefault_main);

		// 通过控件的ID来得到代表控件的对象
		textView = (TextView) findViewById(R.id.text_view);
		checkbox1 = (CheckBox) findViewById(R.id.cb1);
		checkbox2 = (CheckBox) findViewById(R.id.cb2);
		mSwitch = (Switch) findViewById(R.id.switch1);
		mSwitch.setOnClickListener(new OnClickListener() {

			@SuppressLint("NewApi")
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (mSwitch.isChecked()) {
					Toast.makeText(CheckBoxToggleBtnSwitchDefaultMainActivity.this, "switch open",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(CheckBoxToggleBtnSwitchDefaultMainActivity.this, "switch off",
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		// 为第一个 CheckBox 注册监听
		checkbox1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// 如果第一个 CheckBox 被选中
				if (isChecked == true) {
					textView.setText("CheckBox选中北京");
				}
			}
		});

		// 为第二个 CheckBox 注册监听
		checkbox2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// 如果第二个 CheckBox 被选中
				if (isChecked == true) {
					textView.setText("CheckBox选中上海");
				}
			}
		});
	}
}