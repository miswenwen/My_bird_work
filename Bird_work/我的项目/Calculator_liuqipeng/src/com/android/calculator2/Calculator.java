/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroupOverlay;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.android.calculator2.CalculatorEditText.OnTextSizeChangeListener;
import com.android.calculator2.CalculatorExpressionEvaluator.EvaluateCallback;
//liuqipeng add
import android.widget.Toast;
//liuqipeng
public class Calculator extends Activity
        implements OnTextSizeChangeListener, EvaluateCallback, OnLongClickListener {

    private static final String NAME = Calculator.class.getName();//Calculator.class返回Class对象。Class 类用来描述类本身。每个 Class 对象都包含了某个类的描述数据，包括这个类有哪些成员、方法等等。

    // instance state keys
    private static final String KEY_CURRENT_STATE = NAME + "_currentState";
    private static final String KEY_CURRENT_EXPRESSION = NAME + "_currentExpression";

    /**
     * Constant for an invalid resource id.
     */
    public static final int INVALID_RES_ID = -1;

    private enum CalculatorState {
        INPUT, EVALUATE, RESULT, ERROR
    }
	//public interface TextWatcher extends NoCopySpan {}接口
    private final TextWatcher mFormulaTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            setState(CalculatorState.INPUT);
            mEvaluator.evaluate(editable, Calculator.this);
        }
    };
	/*   public interface OnKeyListener {
		    boolean onKey(View v, int keyCode, KeyEvent event);
		}
	这个OnKeyListener事件只能在物理键盘或者模拟器旁边的键盘起作用，如果使用系统自带的软键盘输入的话，监听器就像聋子一样，对你的软键盘点击操作毫无表示，除了del键、退格键、空格键有事件产生 "it will not properly work with the Virtual Keyboard"
	如果想监听Ed itText里面的文本改变事件，可以使用addTextChangedListner监听器！
	*/
	//liuqipeng add 注销掉
	/*
    private final OnKeyListener mFormulaOnKeyListener = new OnKeyListener() {
        @Override
		//keyCode是什么？
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        onEquals();
                    }
                    // ignore all other actions
                    return true;
            }
            return false;
        }
    };
	*/
	//liuqipeng end

	/*
	内部类
	public interface Editable
	extends CharSequence, GetChars, Spannable, Appendable
	{
	    public static class Factory {
		    private static Editable.Factory sInstance = new Editable.Factory();
		    public static Editable.Factory getInstance() {
		        return sInstance;
		    }
		    public Editable newEditable(CharSequence source) {
		        return new SpannableStringBuilder(source);
		    }
    	}
	}
	*/
    private final Editable.Factory mFormulaEditableFactory = new Editable.Factory() {
        @Override
        public Editable newEditable(CharSequence source) {
            final boolean isEdited = mCurrentState == CalculatorState.INPUT
                    || mCurrentState == CalculatorState.ERROR;
            return new CalculatorExpressionBuilder(source, mTokenizer, isEdited);
        }
    };

    private CalculatorState mCurrentState;//前面定义的枚举类，INPUT, EVALUATE, RESULT, ERROR
    private CalculatorExpressionTokenizer mTokenizer;
    private CalculatorExpressionEvaluator mEvaluator;

    private View mDisplayView;
	//public class CalculatorEditText extends EditText {}
    private CalculatorEditText mFormulaEditText;//公式显示栏　formula(公式)
    private CalculatorEditText mResultEditText;//结果显示栏
    private ViewPager mPadViewPager;
    private View mDeleteButton;
    private View mClearButton;
    private View mEqualButton;
	//liuqipeng add
	private boolean BirdFeature=true;
	private Button mPlusBtn;
	private Button mSubBtn;
	private Button mDivBtn;
	private Button mMulBtn;
	private Button mEqualBtn;
	//liuqipeng end

    private Animator mCurrentAnimator;//public abstract class Animator implements Cloneable {}属性动画

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		/*	还能这么用呢
			res/values-land目录下layout.xml文件
			<resources>
				<item name="activity_calculator" type="layout">@layout/activity_calculator_land</item>
			</resources>
			res/values-port目录下layout.xml文件
			<resources>
				<item name="activity_calculator" type="layout">@layout/activity_calculator_port</item>
			</resources>		
		*/
        setContentView(R.layout.activity_calculator);
		//liuqipeng add
		mPlusBtn=(Button)findViewById(R.id.plus_btn);
		mSubBtn=(Button)findViewById(R.id.sub_btn);
		mDivBtn=(Button)findViewById(R.id.div_btn);
		mMulBtn=(Button)findViewById(R.id.mul_btn);
		mEqualBtn=(Button)findViewById(R.id.equal_btn);
		//liuqipeng end
        mDisplayView = findViewById(R.id.display);
        mFormulaEditText = (CalculatorEditText) findViewById(R.id.formula);
        mResultEditText = (CalculatorEditText) findViewById(R.id.result);
        mPadViewPager = (ViewPager) findViewById(R.id.pad_pager);//横屏不报空指针？AndroidManifest没有锁定竖屏啊
        mDeleteButton = findViewById(R.id.del);
        mClearButton = findViewById(R.id.clr);
		/*
			"="先在数字layout中找，如果没找到或者找到了但是这个控件不可见(hide or invisible)
			"="这个按钮就用加减乘除layout的等号。
			问题:如果一个容器被设置为不可见，容器中的子控件获取的Visible状态是什么？和容器同步吗？
		*/
        mEqualButton = findViewById(R.id.pad_numeric).findViewById(R.id.eq);
        if (mEqualButton == null || mEqualButton.getVisibility() != View.VISIBLE) {
            mEqualButton = findViewById(R.id.pad_operator).findViewById(R.id.eq);
        }

        mTokenizer = new CalculatorExpressionTokenizer(this);
        mEvaluator = new CalculatorExpressionEvaluator(mTokenizer);
		/*
		    public static final Bundle EMPTY;
			static final Parcel EMPTY_PARCEL;
			静态初始化块？
			static {
				EMPTY = new Bundle();
				EMPTY.mMap = ArrayMap.EMPTY;
				EMPTY_PARCEL = BaseBundle.EMPTY_PARCEL;
			}
		*/
        savedInstanceState = savedInstanceState == null ? Bundle.EMPTY : savedInstanceState;
		/*编译java文件时，java编译器会自动帮助我们在枚举类中生成这个方法values()。
		  例如：
			enum Color{//枚举常量
			red,green,black
			}
			Color[] colors=Color.values();
			ordinal()是枚举类的方法。
			此方法返回的枚举常量的序数。
			下面这句代码相当于就是选择一个数组中索引值对应的值。然后setState.
		*/
        setState(CalculatorState.values()[
                savedInstanceState.getInt(KEY_CURRENT_STATE, CalculatorState.INPUT.ordinal())]);
		//屏幕方向变化后会重新创建Activity，为了前后显示一致
        mFormulaEditText.setText(mTokenizer.getLocalizedExpression(
                savedInstanceState.getString(KEY_CURRENT_EXPRESSION, "")));
        mEvaluator.evaluate(mFormulaEditText.getText(), this);
		/*
		public final void setEditableFactory(Editable.Factory factory) {
        mEditableFactory = factory;
        setText(mText);
   		 }
		*/
        mFormulaEditText.setEditableFactory(mFormulaEditableFactory);
        mFormulaEditText.addTextChangedListener(mFormulaTextWatcher);
		//liuqipeng add 注销掉
        //mFormulaEditText.setOnKeyListener(mFormulaOnKeyListener);
		//liuqipeng 
		//这个是CalculatorEditText中新增的
        mFormulaEditText.setOnTextSizeChangeListener(this);
		//删除按钮添加了长按监听调用onClear()
        mDeleteButton.setOnLongClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before it is serialized.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.end();
        }

        super.onSaveInstanceState(outState);
		//屏幕方向切换的时候调用这个方法，Bundle中保存数据，一个是当前状态，一个是显示内容，onCreate方法中再把数据取出来
        outState.putInt(KEY_CURRENT_STATE, mCurrentState.ordinal());
        outState.putString(KEY_CURRENT_EXPRESSION,
                mTokenizer.getNormalizedExpression(mFormulaEditText.getText().toString()));
    }

    private void setState(CalculatorState state) {
        if (mCurrentState != state) {
            mCurrentState = state;

            if (state == CalculatorState.RESULT || state == CalculatorState.ERROR) {
                mDeleteButton.setVisibility(View.GONE);
                mClearButton.setVisibility(View.VISIBLE);
            } else {
                mDeleteButton.setVisibility(View.VISIBLE);
                mClearButton.setVisibility(View.GONE);
            }

            if (state == CalculatorState.ERROR) {
                final int errorColor = getResources().getColor(R.color.calculator_error_color);
                mFormulaEditText.setTextColor(errorColor);
                mResultEditText.setTextColor(errorColor);
				//状态栏颜色也变，有点意思
                getWindow().setStatusBarColor(errorColor);
            } else {
                mFormulaEditText.setTextColor(
                        getResources().getColor(R.color.display_formula_text_color));
                mResultEditText.setTextColor(
                        getResources().getColor(R.color.display_result_text_color));
                getWindow().setStatusBarColor(
                        getResources().getColor(R.color.calculator_accent_color));
            }
        }
    }
	//按返回键，如果在０，直接退出。如果在１，则返回０．
    @Override
    public void onBackPressed() {
        if (mPadViewPager == null || mPadViewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first pad (or the pad is not paged),
            // allow the system to handle the Back button.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous pad.
            mPadViewPager.setCurrentItem(mPadViewPager.getCurrentItem() - 1);
        }
    }
	/*此方法是activity的方法,当此activity在栈顶时，用户对手机：触屏点击，按home，back，menu键都会触发此方法。
	注：下拉statubar,旋转屏幕,锁屏，不会触发此方法.
	*/
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before the pending user interaction is handled.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.end();
        }
    }
//liuqipeng add
	public void setPressedFalse(final Button mButton){
		mButton.postDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				mButton.setPressed(false);
			}
		}, 50);
	}
	//该方法貌似在实体按键放下和松开的时候一共会调用两次
	public boolean dispatchKeyEvent(KeyEvent event) {
	  if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
		if(event.getAction()==KeyEvent.ACTION_DOWN){
			mEqualBtn.setPressed(true);
		}
		else if(event.getAction()==KeyEvent.ACTION_UP){
			mEqualBtn.performClick();
			setPressedFalse(mEqualBtn);
		}
		return true;
	  }
	  return super.dispatchKeyEvent(event);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode<=KeyEvent.KEYCODE_9&&keyCode>=KeyEvent.KEYCODE_0){
			mFormulaEditText.append(Integer.toString(keyCode-7));
		}
		else{
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_DOWN:
				mSubBtn.setPressed(true);
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:
				mPlusBtn.setPressed(true);
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				mMulBtn.setPressed(true);
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				mDivBtn.setPressed(true);
				return true;
			//系统输入法优先弹出来了，导致mEqualBtn的点击效果没了，去dispatchKeyEvent中去规避。
			case KeyEvent.KEYCODE_DPAD_CENTER:
				mEqualBtn.setPressed(true);
				return true;
			//回退键盘，当最后一个字符被删除，再点击的时候退出程序
			case KeyEvent.KEYCODE_BACK:
				onDelete();
				return true;		
			//拨号
			case KeyEvent.KEYCODE_CALL:
				return true;
			case KeyEvent.KEYCODE_ENDCALL:
				return true;
			//"#"
			case KeyEvent.KEYCODE_POUND:
				return true;
			//"*"
			case KeyEvent.KEYCODE_STAR:
				return true;
			default:
				break;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_DOWN:
			mSubBtn.performClick();
			setPressedFalse(mSubBtn);
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			mPlusBtn.performClick();
			setPressedFalse(mPlusBtn);
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			mMulBtn.performClick();
			setPressedFalse(mMulBtn);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			mDivBtn.performClick();
			setPressedFalse(mDivBtn);
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			mEqualBtn.performClick();
			setPressedFalse(mEqualBtn);
			return true;
		//拨号
		case KeyEvent.KEYCODE_CALL:
			return true;
		case KeyEvent.KEYCODE_ENDCALL:
			return true;
		//"#"
		case KeyEvent.KEYCODE_POUND:
			return true;
		//"*"
		case KeyEvent.KEYCODE_STAR:
			return true;
		default:
			break;
		}
		return super.onKeyUp(keyCode, event);
	}
//liuqipeng end
	//点击事件在布局中Button android:onClick="onButtonClick"
    public void onButtonClick(View view) {
        switch (view.getId()) {
//liuqipeng add
            case R.id.equal_btn:
                onEquals();
                break;
            case R.id.plus_btn:
                mFormulaEditText.append("+");
                break;
            case R.id.mul_btn:
                mFormulaEditText.append("×");
                break;
            case R.id.div_btn:
                mFormulaEditText.append("÷");
                break;
            case R.id.sub_btn:
                mFormulaEditText.append("−");
                break;
//liuqipeng end
            case R.id.eq:
                onEquals();
                break;
            case R.id.del:
                onDelete();
                break;
            case R.id.clr:
                onClear();
                break;
            case R.id.fun_cos:
            case R.id.fun_ln:
            case R.id.fun_log:
            case R.id.fun_sin:
            case R.id.fun_tan:
                // Add left parenthesis after functions.
                mFormulaEditText.append(((Button) view).getText() + "(");
                break;
            default:
				//没看到Button对象setText啊，getText()获取的什么？
				//原来在容器CalculatorNumericPadLayout里面设置的。
                mFormulaEditText.append(((Button) view).getText());
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.del) {
            onClear();
            return true;
        }
        return false;
    }

    @Override
    public void onEvaluate(String expr, String result, int errorResourceId) {
        if (mCurrentState == CalculatorState.INPUT) {
            mResultEditText.setText(result);
        } else if (errorResourceId != INVALID_RES_ID) {
            onError(errorResourceId);
        } else if (!TextUtils.isEmpty(result)) {
            onResult(result);
        } else if (mCurrentState == CalculatorState.EVALUATE) {
            // The current expression cannot be evaluated -> return to the input state.
            setState(CalculatorState.INPUT);
        }

        mFormulaEditText.requestFocus();
    }

    @Override
    public void onTextSizeChanged(final TextView textView, float oldSize) {
        if (mCurrentState != CalculatorState.INPUT) {
            // Only animate text changes that occur from user input.
            return;
        }

        // Calculate the values needed to perform the scale and translation animations,
        // maintaining the same apparent baseline for the displayed text.
        final float textScale = oldSize / textView.getTextSize();
        final float translationX = (1.0f - textScale) *
                (textView.getWidth() / 2.0f - textView.getPaddingEnd());
        final float translationY = (1.0f - textScale) *
                (textView.getHeight() / 2.0f - textView.getPaddingBottom());

        final AnimatorSet animatorSet = new AnimatorSet();
		//textView缩放动画和平移动画
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(textView, View.SCALE_X, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.SCALE_Y, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_X, translationX, 0.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_Y, translationY, 0.0f));
        animatorSet.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void onEquals() {
        if (mCurrentState == CalculatorState.INPUT) {
            setState(CalculatorState.EVALUATE);
            mEvaluator.evaluate(mFormulaEditText.getText(), this);
        }
    }

    private void onDelete() {
        // Delete works like backspace; remove the last character from the expression.
        final Editable formulaText = mFormulaEditText.getEditableText();
        final int formulaLength = formulaText.length();
        if (formulaLength > 0) {
            formulaText.delete(formulaLength - 1, formulaLength);
        }
		//liuqipeng add
		else{
			if(BirdFeature){
				finish();
			}
		}
		//liuqipeng
    }
	//总之就是个动画效果
    private void reveal(View sourceView, int colorRes, AnimatorListener listener) {
        final ViewGroupOverlay groupOverlay =
                (ViewGroupOverlay) getWindow().getDecorView().getOverlay();

        final Rect displayRect = new Rect();
        mDisplayView.getGlobalVisibleRect(displayRect);

        // Make reveal cover the display and status bar.
        final View revealView = new View(this);
        revealView.setBottom(displayRect.bottom);
        revealView.setLeft(displayRect.left);
        revealView.setRight(displayRect.right);
        revealView.setBackgroundColor(getResources().getColor(colorRes));
        groupOverlay.add(revealView);
		/*int[] location = new  int[2] ;
		view.getLocationInWindow(location); //获取在当前窗口内的绝对坐标
		view.getLocationOnScreen(location);//获取在整个屏幕内的绝对坐标
		location [0]--->x坐标,location [1]--->y坐标
		*/
        final int[] clearLocation = new int[2];
        sourceView.getLocationInWindow(clearLocation);
        clearLocation[0] += sourceView.getWidth() / 2;
        clearLocation[1] += sourceView.getHeight() / 2;

        final int revealCenterX = clearLocation[0] - revealView.getLeft();
        final int revealCenterY = clearLocation[1] - revealView.getTop();

        final double x1_2 = Math.pow(revealView.getLeft() - revealCenterX, 2);
        final double x2_2 = Math.pow(revealView.getRight() - revealCenterX, 2);
        final double y_2 = Math.pow(revealView.getTop() - revealCenterY, 2);
        final float revealRadius = (float) Math.max(Math.sqrt(x1_2 + y_2), Math.sqrt(x2_2 + y_2));

        final Animator revealAnimator =
                ViewAnimationUtils.createCircularReveal(revealView,
                        revealCenterX, revealCenterY, 0.0f, revealRadius);
        revealAnimator.setDuration(
                getResources().getInteger(android.R.integer.config_longAnimTime));

        final Animator alphaAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, 0.0f);
        alphaAnimator.setDuration(
                getResources().getInteger(android.R.integer.config_mediumAnimTime));
        alphaAnimator.addListener(listener);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(revealAnimator).before(alphaAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                groupOverlay.remove(revealView);
                mCurrentAnimator = null;
            }
        });

        mCurrentAnimator = animatorSet;
        animatorSet.start();
    }

    private void onClear() {
        if (TextUtils.isEmpty(mFormulaEditText.getText())) {
            return;
        }

        final View sourceView = mClearButton.getVisibility() == View.VISIBLE
                ? mClearButton : mDeleteButton;
        reveal(sourceView, R.color.calculator_accent_color, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFormulaEditText.getEditableText().clear();
            }
        });
    }

    private void onError(final int errorResourceId) {
        if (mCurrentState != CalculatorState.EVALUATE) {
            // Only animate error on evaluate.
            mResultEditText.setText(errorResourceId);
            return;
        }

        reveal(mEqualButton, R.color.calculator_error_color, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setState(CalculatorState.ERROR);
                mResultEditText.setText(errorResourceId);
            }
        });
    }

    private void onResult(final String result) {
        // Calculate the values needed to perform the scale and translation animations,
        // accounting for how the scale will affect the final position of the text.
        final float resultScale =
                mFormulaEditText.getVariableTextSize(result) / mResultEditText.getTextSize();
        final float resultTranslationX = (1.0f - resultScale) *
                (mResultEditText.getWidth() / 2.0f - mResultEditText.getPaddingEnd());
        final float resultTranslationY = (1.0f - resultScale) *
                (mResultEditText.getHeight() / 2.0f - mResultEditText.getPaddingBottom()) +
                (mFormulaEditText.getBottom() - mResultEditText.getBottom()) +
                (mResultEditText.getPaddingBottom() - mFormulaEditText.getPaddingBottom());
        final float formulaTranslationY = -mFormulaEditText.getBottom();

        // Use a value animator to fade to the final text color over the course of the animation.
        final int resultTextColor = mResultEditText.getCurrentTextColor();
        final int formulaTextColor = mFormulaEditText.getCurrentTextColor();
        final ValueAnimator textColorAnimator =
                ValueAnimator.ofObject(new ArgbEvaluator(), resultTextColor, formulaTextColor);
        textColorAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mResultEditText.setTextColor((int) valueAnimator.getAnimatedValue());
            }
        });

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                textColorAnimator,
                ObjectAnimator.ofFloat(mResultEditText, View.SCALE_X, resultScale),
                ObjectAnimator.ofFloat(mResultEditText, View.SCALE_Y, resultScale),
                ObjectAnimator.ofFloat(mResultEditText, View.TRANSLATION_X, resultTranslationX),
                ObjectAnimator.ofFloat(mResultEditText, View.TRANSLATION_Y, resultTranslationY),
                ObjectAnimator.ofFloat(mFormulaEditText, View.TRANSLATION_Y, formulaTranslationY));
        animatorSet.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mResultEditText.setText(result);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset all of the values modified during the animation.
                mResultEditText.setTextColor(resultTextColor);
                mResultEditText.setScaleX(1.0f);
                mResultEditText.setScaleY(1.0f);
                mResultEditText.setTranslationX(0.0f);
                mResultEditText.setTranslationY(0.0f);
                mFormulaEditText.setTranslationY(0.0f);

                // Finally update the formula to use the current result.
                mFormulaEditText.setText(result);
                setState(CalculatorState.RESULT);

                mCurrentAnimator = null;
            }
        });

        mCurrentAnimator = animatorSet;
        animatorSet.start();
    }
}
