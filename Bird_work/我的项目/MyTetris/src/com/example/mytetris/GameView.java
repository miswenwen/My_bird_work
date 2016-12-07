package com.example.mytetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GameView extends View {

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	
		paintLine(canvas);
		paintBlock(canvas);
	}

	public void paintLine(Canvas canvas) {// 画游戏进行的外框
		float height = getHeight();
		float width = getWidth();
		float x_part = width / 10.0f;
		float y_part = height / 20.0f;
		Paint p = new Paint();
		
		for (int i = 0; i <= 10; i++) {// 画11根竖线
			canvas.drawLine(i * x_part, 0, i * x_part, height, p);
		}
		for (int i = 0; i <= 20; i++) {// 画21根横线
			canvas.drawLine(0, i * y_part, width, i * y_part, p);
		}
		canvas.drawLine(10 * x_part - 0.25f, 0, 10 * x_part - 0.25f, height, p);// 由于浮点数计算丢失
																				// 会导致上面横线竖线超过边界看不到此处补上
		canvas.drawLine(0, 20 * y_part - 0.25f, width, 20 * y_part - 0.25f, p);
	}

	public void paintBlock(Canvas canvas) {
		float height = getHeight();
		float width = getWidth();
		float x_part = width / 10.0f;
		float y_part = height / 20.0f;
		Paint p = new Paint();
		p.setStrokeJoin(Paint.Join.ROUND);
		for (int i = 0; i < 4; i++) {// 画移动方块
			for (int j = 0; j < 4; j++) {
				if (Shape.state[Data.k][i][j] != 0) {
					canvas.drawRect((j + Data.shift_x) * x_part,
							(i + Data.shift_y) * y_part, (j + Data.shift_x + 1)
									* x_part, (i + 1 + Data.shift_y) * y_part,
							p);
				}
			}
		}

		for (int i = 0; i < Data.game_row; i++) {// 画底下固定的方块
			for (int j = 0; j < Data.game_col; j++) {
				if (Data.m_screen[i][j] == 1) {
					canvas.drawRect((j) * x_part - 1, (i) * y_part - 1, (j + 1)
							* x_part + 1, (i + 1) * y_part + 1, p);
				}
			}
		}


		for (int j = 0; j < Data.game_col; j++) {
			int y = Data.shift_y;
			for (int i = y; i < Data.game_row; i++) {
				if (isClearLine(i)) {
					Log.e("a", "run");
					p.setColor(Color.RED);
					canvas.drawRect((j) * x_part - 1, (i) * y_part - 1, (j + 1)
							* x_part + 1, (i + 1) * y_part + 1, p);

				}
			}
		}
		

	}

	public boolean isClearLine(int i) {
		for (int j = 0; j < Data.game_col; j++) {
			if (Data.m_screen_a[i][j] == 0)
				return false;
		}
		return true;

	}
}