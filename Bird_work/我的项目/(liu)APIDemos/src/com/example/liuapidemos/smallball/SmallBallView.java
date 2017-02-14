package com.example.liuapidemos.smallball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SmallBallView extends View {
	int x = 50;
	int width;
	boolean direction = true;

	public SmallBallView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		Paint mPaint = new Paint();
		mPaint.setColor(Color.RED);
		canvas.drawCircle(x, 100, 50, mPaint);
		width = getMeasuredWidth();
		if (x <= 50) {
			direction = true;
		}
		if (x >= width - 50) {
			direction = false;
		}
		x = direction ? x + 5 : x - 5;
	}

}
