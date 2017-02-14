package com.example.liuapidemos.drawlinebypath;

import android.R.integer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawLinePathView extends View {
Path path;//要意识到一条路径对应一条线，多条路径对应多条线。
int preX,preY;
Paint  paint;
Bitmap mBitmap;
Canvas mCanvas;

	public DrawLinePathView(Context context, AttributeSet attrs) {
		super(context, attrs);
		path=new Path();
		paint=new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5);
		
		// TODO Auto-generated constructor stub
	}
@Override
protected void onDraw(Canvas canvas) {
	// TODO Auto-generated method stub
	super.onDraw(canvas);
	if(mBitmap==null){
		int width=getMeasuredWidth();
		int height=getMeasuredHeight();
		mBitmap=Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		mCanvas=new Canvas(mBitmap);
	}
	canvas.drawBitmap(mBitmap, 0, 0, null);//每ACTION_UP一次，记录一次。
	canvas.drawPath(path, paint);//不断调用，整个图形是许多小段组成的
}
@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
	int x=(int)event.getX();
	int y=(int)event.getY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			path.reset();//清空所有已经画过的path至原始状态
			preX=x;
			preY=y;
			path.moveTo(x, y);
			break;
		case MotionEvent.ACTION_MOVE:
			 int	controlX	=	(x	+	preX)	/	2;	
				
			 int	controlY	=	(y	+	preY)	/	2;	
				
			 path.quadTo(controlX,	controlY,	x,	y);	

			invalidate();
			preX=x;
			preY=y;
			break;
		case MotionEvent.ACTION_UP:
			 //手指松开后将最终的绘图结果绘制在 bitmapBuffer 中,同时绘制到 View 上
			mCanvas.drawPath(path, paint);
			invalidate();
			break;
		default:
			break;
		}
		return true;
	}

}
