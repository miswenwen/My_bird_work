package com.example.liuapidemos.valueanimator;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.example.liuapidemos.R;

public class ValueAnimatorMainActivity extends Activity {
	Button mButton;
	Button mButton2;
	AnimatorSet mSet;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.value_animator_activity_main);
		mButton = (Button) findViewById(R.id.button1);
		mButton2 = (Button) findViewById(R.id.button2);
		/*
		 * ValueAnimator继承自Animator
		 * ObjectAnimator继承自ValueAnimator
		 */
		ValueAnimator mAnimator = ObjectAnimator.ofInt(mButton,
				"backgroundColor", 0xffff8080, 0xff8080ff);
		Log.e("getDuration", "" + mAnimator.getDuration());// 默认300ms
		mAnimator.setDuration(3000);
		mAnimator.setEvaluator(new ArgbEvaluator());
		mAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mAnimator.setRepeatMode(ValueAnimator.REVERSE);
		mAnimator.start();

		mSet = new AnimatorSet();
		Log.e("getDuration", "" + mSet.getDuration());// 默认-1ms
		/*
		 * 平移:translationX,translationY
		 * 旋转:rotation,rotationX,rotationY
		 * 缩放:scaleX,scaleY.实测scale无效
		 * 透明度:alpha
		 */
		mSet.playTogether(
				ObjectAnimator.ofFloat(mButton2, "rotation", 0, 3600),
				ObjectAnimator.ofFloat(mButton2, "translationX", 0, 360),
				ObjectAnimator.ofFloat(mButton2, "scaleX", 1,3,1),
				ObjectAnimator.ofFloat(mButton2, "scaleY", 1,3,1),
				ObjectAnimator.ofFloat(mButton2, "alpha", 1,0.25f,1));
		mSet.setDuration(10000).start();
		Log.e("getDuration", "" + mSet.getDuration());// 10000
	}

}
