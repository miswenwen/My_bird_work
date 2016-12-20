package com.example.liuactivitylog;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class MyView extends View{
	public MyView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	public MyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		Log.e("MyView", "MyView");
	}
	public MyView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	// TODO Auto-generated method stub
	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	Log.e("MyView", "onMeasure");
}
@Override
protected void onDraw(Canvas canvas) {
	// TODO Auto-generated method stub
	super.onDraw(canvas);
	Log.e("MyView", "onDraw");
}
@Override
protected void onAttachedToWindow() {
	// TODO Auto-generated method stub
	super.onAttachedToWindow();
	Log.e("MyView", "onAttachedToWindow");
}
@Override
protected void onDetachedFromWindow() {
	// TODO Auto-generated method stub
	super.onDetachedFromWindow();
	Log.e("MyView", "onDetachedFromWindow");
}
@Override
protected void onLayout(boolean changed, int left, int top, int right,
		int bottom) {
	// TODO Auto-generated method stub
	super.onLayout(changed, left, top, right, bottom);
	Log.e("MyView", "onLayout");
}
@Override
protected void onFinishInflate() {
	// TODO Auto-generated method stub
	super.onFinishInflate();
	Log.e("MyView", "onFinishInflate");
}
@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	// TODO Auto-generated method stub
	super.onSizeChanged(w, h, oldw, oldh);
	Log.e("MyView", "onSizeChanged");
}
@Override
protected Parcelable onSaveInstanceState() {
	// TODO Auto-generated method stub
	Log.e("MyView", "onSaveInstanceState");
	return super.onSaveInstanceState();

}
@Override
protected void onRestoreInstanceState(Parcelable state) {
	// TODO Auto-generated method stub
	super.onRestoreInstanceState(state);
	Log.e("MyView", "onRestoreInstanceState");
}
@Override
protected void onConfigurationChanged(Configuration newConfig) {
	// TODO Auto-generated method stub
	super.onConfigurationChanged(newConfig);
	Log.e("MyView", "onConfigurationChanged");
}
@Override
protected void onFocusChanged(boolean gainFocus, int direction,
		Rect previouslyFocusedRect) {
	// TODO Auto-generated method stub
	super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	Log.e("MyView", "onFocusChanged:"+gainFocus);
}
@Override
public void onWindowFocusChanged(boolean hasWindowFocus) {
	// TODO Auto-generated method stub
	super.onWindowFocusChanged(hasWindowFocus);
	Log.e("MyView", "onWindowFocusChanged:"+hasWindowFocus);
}
@Override
protected void onWindowVisibilityChanged(int visibility) {
	// TODO Auto-generated method stub
	super.onWindowVisibilityChanged(visibility);
	Log.e("MyView", "onWindowVisibilityChanged:"+visibility);
}
}
