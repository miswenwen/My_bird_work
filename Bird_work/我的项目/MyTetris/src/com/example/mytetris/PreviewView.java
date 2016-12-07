package com.example.mytetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PreviewView extends View {

	public PreviewView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Paint p = new Paint();
		
		float height = getHeight();
		float width = getWidth();
		float x_part = width / 6.0f;
		float y_part = height / 6.0f;
		for (int i = 0; i <= 6; i++) {// 画竖线
			canvas.drawLine(i * x_part, 0.0f, i * x_part, height, p);
		}
		for (int j = 0; j <= 6; j++) {// 画横线
			canvas.drawLine(0.0f, j * y_part, width, j * y_part, p);
		}
		canvas.drawLine(6 * x_part - 0.25f, 0.0f, 6 * x_part - 0.25f, height, p);// 浮点数计算丢失会导致最后一条线画不出来，这里补上
		canvas.drawLine(0.0f, 6 * y_part - 0.25f, width, 6 * y_part - 0.25f, p);
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (Shape.state[Data.kk][i][j] == 1) {
					canvas.drawRect((j + 1) * x_part, (i + 1) * y_part, (j + 2)
							* x_part, (i + 2) * y_part, p);
				}
			}
		}

	}
}
