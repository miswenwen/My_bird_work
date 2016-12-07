package com.example.liuunitconvert;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {
Button mLenghtButton;
Button mAreaButton;
Button mVolumeButton;
Button mTemButton;
Button mSpeedButton;
Button mTimeButton;
Button mMassButton;
Button mBigScaleButton;
Button mSchemicalButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mLenghtButton=(Button)findViewById(R.id.button1);
		mAreaButton=(Button)findViewById(R.id.button2);
		mVolumeButton=(Button)findViewById(R.id.button3);
		mTemButton=(Button)findViewById(R.id.button4);
		mSpeedButton=(Button)findViewById(R.id.button5);
		mTimeButton=(Button)findViewById(R.id.button6);
		mMassButton=(Button)findViewById(R.id.button7);
		mLenghtButton.setOnClickListener(this);
		mAreaButton.setOnClickListener(this);
		mVolumeButton.setOnClickListener(this);
		mTemButton.setOnClickListener(this);
		mSpeedButton.setOnClickListener(this);
		mTimeButton.setOnClickListener(this);
		mMassButton.setOnClickListener(this);

		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Intent mIntent = null;
		switch (v.getId()) {
		case R.id.button1:
			mIntent = new Intent(MainActivity.this, UnitConvertActivity.class);
			mIntent.putExtra("UnitType", UnitConvertUtil.Length);
			break;
		case R.id.button2:
			mIntent = new Intent(MainActivity.this, UnitConvertActivity.class);
			mIntent.putExtra("UnitType", UnitConvertUtil.Area);
			break;
		case R.id.button3:
			mIntent = new Intent(MainActivity.this, UnitConvertActivity.class);
			mIntent.putExtra("UnitType", UnitConvertUtil.Volume);
			break;
		case R.id.button4:
			mIntent = new Intent(MainActivity.this, UnitConvertActivity.class);
			mIntent.putExtra("UnitType", UnitConvertUtil.Temperature);

			break;
		case R.id.button5:
			mIntent = new Intent(MainActivity.this, UnitConvertActivity.class);
			mIntent.putExtra("UnitType", UnitConvertUtil.Speed);

			break;
		case R.id.button6:
			mIntent = new Intent(MainActivity.this, UnitConvertActivity.class);
			mIntent.putExtra("UnitType", UnitConvertUtil.Time);

			break;
		case R.id.button7:
			mIntent = new Intent(MainActivity.this, UnitConvertActivity.class);
			mIntent.putExtra("UnitType", UnitConvertUtil.Mass);
			break;
		case R.id.button8:
			mIntent = new Intent(MainActivity.this, UpperNumActivity.class);
			break;

		case R.id.button9:
			mIntent = new Intent(MainActivity.this, SciCalculatorActivity.class);
			break;
		default:
			break;
		}
		startActivity(mIntent);
	}

}
