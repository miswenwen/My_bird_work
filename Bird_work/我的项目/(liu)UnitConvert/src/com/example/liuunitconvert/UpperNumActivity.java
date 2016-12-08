package com.example.liuunitconvert;

import java.math.BigDecimal;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class UpperNumActivity extends Activity implements OnClickListener {
	private ImageButton mSwitchButton;
	private TextView mInputText;
	private TextView mResultText;
	private String inputNum = "0";
	private Resources mResources;
	private boolean dotNeverClick = true;
	private String result = "";
	boolean canNotContinueInput;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.capital_convert);
		mResources = getResources();
		// 设置沉浸式状态栏
		setStatusBar();
		// 获取控件实例
		mSwitchButton = (ImageButton) findViewById(R.id.switch_btn);
		mInputText = (TextView) findViewById(R.id.input_num_tv);
		mResultText = (TextView) findViewById(R.id.result_num_tv);
		mInputText.setText(inputNum);
		BigDecimal numberOfMoney = new BigDecimal(inputNum);
		result = NumberToCn.number2CNMontrayUnit(numberOfMoney);
		mResultText.setText(result);
		final TypedArray convertButtons = mResources
				.obtainTypedArray(R.array.unit_convert_buttons);
		for (int i = 0; i < convertButtons.length(); i++) {
			ImageButton mButton = (ImageButton) findViewById(convertButtons
					.getResourceId(i, 0));
			mButton.setOnClickListener(this);
		}
		convertButtons.recycle();
		mSwitchButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
	}

	private void setStatusBar() {
		// TODO Auto-generated method stub
		// 首先使 ChildView 不预留空间
		Window window = this.getWindow();
		ViewGroup mContentView = (ViewGroup) findViewById(Window.ID_ANDROID_CONTENT);
		View mChildView = mContentView.getChildAt(0);
		if (mChildView != null) {
			ViewCompat.setFitsSystemWindows(mChildView, false);
		}

		int statusBarHeight = getStatusBarHeight();
		// 需要设置这个 flag 才能设置状态栏
		window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		// 避免多次调用该方法时,多次移除了 View
		if (mChildView != null && mChildView.getLayoutParams() != null
				&& mChildView.getLayoutParams().height == statusBarHeight) {
			// 移除假的 View.
			mContentView.removeView(mChildView);
			mChildView = mContentView.getChildAt(0);
		}
		if (mChildView != null) {
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mChildView
					.getLayoutParams();
			// 清除 ChildView 的 marginTop 属性
			if (lp != null && lp.topMargin >= statusBarHeight) {
				lp.topMargin -= statusBarHeight;
				mChildView.setLayoutParams(lp);
			}
		}
	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height",
				"dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (inputNum.equals("0")) {
			inputNum = "";
		}
		canNotContinueInput = inputNum.matches("^[0-9]+\\.[0-9]{2}$")
				|| inputNum.matches("^[0-9]{16}");
		switch (v.getId()) {
		case R.id.digit_0:
			if (!canNotContinueInput)
				inputNum = inputNum + "0";
			break;
		case R.id.digit_1:
			if (!canNotContinueInput)
				inputNum = inputNum + "1";
			break;
		case R.id.digit_2:
			if (!canNotContinueInput)
				inputNum = inputNum + "2";
			break;
		case R.id.digit_3:
			if (!canNotContinueInput)
				inputNum = inputNum + "3";
			break;
		case R.id.digit_4:
			if (!canNotContinueInput)
				inputNum = inputNum + "4";
			break;
		case R.id.digit_5:
			if (!canNotContinueInput)
				inputNum = inputNum + "5";
			break;
		case R.id.digit_6:
			if (!canNotContinueInput)
				inputNum = inputNum + "6";
			break;
		case R.id.digit_7:
			if (!canNotContinueInput)
				inputNum = inputNum + "7";
			break;
		case R.id.digit_8:
			if (!canNotContinueInput)
				inputNum = inputNum + "8";
			break;

		case R.id.digit_9:
			if (!canNotContinueInput)
				inputNum = inputNum + "9";
			break;
		case R.id.digit_dot:
			if (inputNum.equals("")) {
				inputNum = "0.";
				dotNeverClick = false;
			} else if (dotNeverClick) {
				inputNum = inputNum + ".";
				dotNeverClick = false;
			}
			break;
		case R.id.func_clear:
			inputNum = "0";
			dotNeverClick = true;
			break;
		case R.id.func_del:
			if (inputNum.length() > 0 && !inputNum.equals("0")) {
				if (inputNum.charAt(inputNum.length() - 1) == '.') {
					dotNeverClick = true;
				}
				inputNum = inputNum.substring(0, inputNum.length() - 1);
				if (inputNum.equals("")) {
					inputNum = "0";
				}
			} else {
				inputNum = "0";
			}
			break;
		default:
			break;
		}
		mInputText.setText(inputNum);
		BigDecimal numberOfMoney = new BigDecimal(inputNum);
		try {
			result = NumberToCn.number2CNMontrayUnit(numberOfMoney);
			mResultText.setText(result);
		} catch (Exception e) {
			mResultText.setText("Error");
		}

	}
}
