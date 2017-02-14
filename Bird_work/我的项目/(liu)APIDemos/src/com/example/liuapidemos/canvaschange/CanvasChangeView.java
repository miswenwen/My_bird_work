package com.example.liuapidemos.canvaschange;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.example.liuapidemos.R;

public class CanvasChangeView extends View {
int num=0;
	public CanvasChangeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
		p.setStyle(Style.STROKE);
		p.setColor(Color.RED);
		canvas.save();
		for (int i = 0; i < 8; i++) {
			canvas.drawRect(10, 10, 50, 50, p);
		canvas.translate(10, 10);
		
		}
		canvas.restore();
		
		canvas.translate(0, 200);
		canvas.save();
		for (int i = 0; i < 8; i++) {
			canvas.drawCircle(100, 100, 90, p);
		canvas.scale(0.9f, 0.9f, 50, 50);
		
		}
		canvas.restore();
	
		canvas.translate(0, 200);
		canvas.save();
		canvas.drawCircle(100, 100, 90, p);
		for (int i = 0; i < 12; i++) {
			canvas.drawLine(150, 100, 190, 100, p)
;		canvas.rotate(30, 100, 100);
				}
		canvas.restore();
		
		canvas.translate(0, 200);
		canvas.save();
		for (int i = 0; i < 8; i++) {
			canvas.drawRect(0, 0, 200, 200, p);
		canvas.scale(0.9f, 0.9f, 100, 100);
		
		}
		canvas.restore();
		canvas.translate(0, 200);
		canvas.save();
		Bitmap mBitmap=BitmapFactory.decodeResource(getResources(), R.drawable.canvas_change_pic1);
		Path mPath=new Path();
		p.setColor(Color.BLUE);
		p.setStrokeWidth(15);
		p.setAlpha(100);//一般是float类型就是0.0f到1.0f.而int类型是0~255
		canvas.drawCircle(200, 200, 150, p);
		mPath.addCircle(200, 200, 150, Path.Direction.CCW);
		canvas.clipPath(mPath);
		canvas.drawBitmap(mBitmap, 0, 0, null);
		canvas.restore();
		
	}

}
