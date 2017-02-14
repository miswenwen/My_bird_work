package com.example.liuapidemos.frameanimcustomview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import com.example.liuapidemos.R;

public class FrameAnimCustomView extends View{
int num=0;
	public FrameAnimCustomView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
@Override
protected void onDraw(Canvas canvas) {
	// TODO Auto-generated method stub
	super.onDraw(canvas);
	Bitmap mBitmap2=BitmapFactory.decodeResource(getResources(), R.drawable.frame_anim_custom_view_pic1);
	int width=mBitmap2.getWidth();
	int height=mBitmap2.getHeight();
	int ww=width/7;
	canvas.clipRect(0, 0, ww, height);
	canvas.drawBitmap(mBitmap2, -num*ww, 0, null);
	// TODO Auto-generated method stub
	num++;
	if(num==7){
		num=0;
	}
}
}
