package com.example.liuapidemos.drawlinecanvasdrawline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawLineCanvasDrawLineView extends View {
	int preX, preY;
	int currentX, currentY;
	Bitmap mBitmap;
	Canvas mCanvas;
	Paint paint;

	public DrawLineCanvasDrawLineView(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.BLUE);
		paint.setStrokeWidth(5);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		if (mBitmap == null) {
			mBitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			mCanvas=new Canvas(mBitmap);
		}
		canvas.drawBitmap(mBitmap, 0, 0, paint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		int x = (int) event.getX();
		int y = (int) event.getY();
			switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
preX=x;
preY=y;
			break;
		case MotionEvent.ACTION_MOVE:
			currentX=x;
	      currentY=y;
	     mCanvas.drawLine(preX, preY, currentX, currentY, paint);
	     this.invalidate();
	 	preX=currentX;
		preY=currentY;
			break;
		case MotionEvent.ACTION_UP:
		 invalidate();
			break;
		default:
			break;
		}
	
		return true;
	}
}
