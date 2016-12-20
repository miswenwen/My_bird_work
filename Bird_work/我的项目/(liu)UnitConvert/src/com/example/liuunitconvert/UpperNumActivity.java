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
		StatusBarUtil.setStatusBar(this);
		// 获取控件实例
		mSwitchButton = (ImageButton) findViewById(R.id.switch_btn);
		mInputText = (TextView) findViewById(R.id.input_num_tv);
		mResultText = (TextView) findViewById(R.id.result_num_tv);
		mInputText.setText(inputNum);
		BigDecimal numberOfMoney = new BigDecimal(inputNum);
		result =number2CNMontrayUnit(numberOfMoney);
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
			result = number2CNMontrayUnit(numberOfMoney);
			mResultText.setText(result);
		} catch (Exception e) {
			mResultText.setText("Error");
		}

	}

	private String number2CNMontrayUnit(BigDecimal numberOfMoney) {
		// TODO Auto-generated method stub
		 /**
	     * 汉语中数字大写
	     */
	    final String[] CN_UPPER_NUMBER = mResources.getStringArray(R.array.cn_upper_num);
	    /**
	     * 汉语中货币单位大写，这样的设计类似于占位符
	     */
	    final String[] CN_UPPER_MONETRAY_UNIT = mResources.getStringArray(R.array.cn_upper_monetray_unit);
	    /**
	     * 特殊字符：整
	     */
	    final String CN_FULL = mResources.getString(R.string.cn_full);
	    /**
	     * 特殊字符：负
	     */
	    final String CN_NEGATIVE =mResources.getString(R.string.cn_negative);
	    /**
	     * 金额的精度，默认值为2
	     */
	    final int MONEY_PRECISION = 2;
	    /**
	     * 特殊字符：零元整
	     */
	    final String CN_ZEOR_FULL = mResources.getString(R.string.cn_zero_full)+ CN_FULL;
	    StringBuffer sb = new StringBuffer();
        // -1, 0, or 1 as the value of this BigDecimal is negative, zero, or
        // positive.
        int signum = numberOfMoney.signum();
        // 零元整的情况
        if (signum == 0) {
            return CN_ZEOR_FULL;
        }
        //这里会进行金额的四舍五入
        long number = numberOfMoney.movePointRight(MONEY_PRECISION)
                .setScale(0, 4).abs().longValue();
        // 得到小数点后两位值
        long scale = number % 100;
        int numUnit = 0;
        int numIndex = 0;
        boolean getZero = false;
        // 判断最后两位数，一共有四中情况：00 = 0, 01 = 1, 10, 11
        if (!(scale > 0)) {
            numIndex = 2;
            number = number / 100;
            getZero = true;
        }
        if ((scale > 0) && (!(scale % 10 > 0))) {
            numIndex = 1;
            number = number / 10;
            getZero = true;
        }
        int zeroSize = 0;
        while (true) {
            if (number <= 0) {
                break;
            }
            // 每次获取到最后一个数
            numUnit = (int) (number % 10);
            if (numUnit > 0) {
                if ((numIndex == 9) && (zeroSize >= 3)) {
                    sb.insert(0, CN_UPPER_MONETRAY_UNIT[6]);
                }
                if ((numIndex == 13) && (zeroSize >= 3)) {
                    sb.insert(0, CN_UPPER_MONETRAY_UNIT[10]);
                }
                sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
                sb.insert(0, CN_UPPER_NUMBER[numUnit]);
                getZero = false;
                zeroSize = 0;
            } else {
                ++zeroSize;
                if (!(getZero)) {
                    sb.insert(0, CN_UPPER_NUMBER[numUnit]);
                }
                if (numIndex == 2) {
                    if (number > 0) {
                        sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
                    }
                } else if (((numIndex - 2) % 4 == 0) && (number % 1000 > 0)) {
                    sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
                }
                getZero = true;
            }
            // 让number每次都去掉最后一个数
            number = number / 10;
            ++numIndex;
        }
        // 如果signum == -1，则说明输入的数字为负数，就在最前面追加特殊字符：负
        if (signum == -1) {
            sb.insert(0, CN_NEGATIVE);
        }
        // 输入的数字小数点后两位为"00"的情况，则要在最后追加特殊字符：整
        if (!(scale > 0)) {
            sb.append(CN_FULL);
        }
        return sb.toString();
	}
}
