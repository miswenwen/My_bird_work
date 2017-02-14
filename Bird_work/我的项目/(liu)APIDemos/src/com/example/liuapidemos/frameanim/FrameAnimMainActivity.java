package com.example.liuapidemos.frameanim;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.example.liuapidemos.R;
public class FrameAnimMainActivity extends Activity {
Animation mAnimation1;
ImageView mImageView;
ImageView mImageView1;
AnimationDrawable mAnimationDrawable;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.frame_anim_main);
		mAnimation1=AnimationUtils.loadAnimation(this, R.anim.frame_anim_rotata_alpha);
		mImageView=(ImageView)findViewById(R.id.my_image);
		mImageView1=(ImageView)findViewById(R.id.my_image1);
		mImageView.setAnimation(mAnimation1);
	//		mAnimation1.setFillAfter(true);
		mAnimationDrawable=(AnimationDrawable)mImageView1.getDrawable();
		mAnimationDrawable.start();  

	}

}
