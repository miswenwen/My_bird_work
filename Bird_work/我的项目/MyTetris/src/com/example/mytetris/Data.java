package com.example.mytetris;

import java.util.Random;

import android.R.integer;
import android.util.Log;

public class Data {
	int a;
	float b;
	int master2;
	int liuqipeng1;
   int  liuqipeng2;
	final static int game_row = 20;// 游戏运行的行数
	final static int game_col = 10;// 游戏运行的列数
	final static int style = 28;// 方块的类型数
	public static int[][] m_screen = new int[game_row][game_col];// 屏幕数组
	public static int[][] m_screen_a = new int[game_row][game_col];
	public static int shift_x = 3, shift_y = 0;// 方块左偏移与右偏移,下偏移
	public static int k = 0;// 方块类型
	public static int kk = 0;// 预览方块类型
	static int line = 0;
	static int score = 0; // 分数、等级
	static String speed_level = "easy";// 初始速度等级设定
	static int speed_number = 0;// 游戏难度设置公式用
	Random m_Random;// 随机函数，随即方块类型用

	Data() {
		screenInit();// 游戏区域背景方块初始化
		m_Random = new Random();
		k = m_Random.nextInt(28);
		kk = m_Random.nextInt(28);
		score = 0;
		line = 0;
		shift_x = 3;
		shift_y = 0;
		speed_number = 0;
		speed_level = "easy";
	}

	public void screenInit() {// 初始化m_screen
		for (int i = 0; i < game_row; i++) {
			for (int j = 0; j < game_col; j++) {
				m_screen[i][j] = 0;
				m_screen_a[i][j]=0;
			}
		}
	}

	public void goDown() {// 方块下降
		// TODO Auto-generated method stub
		for (int i = 0; i < Data.game_row; i++) {
			for (int j = 0; j < Data.game_col; j++) {
				Data.m_screen_a[i][j] = 0;
			}
		}
		if (canGoDown()) {
			shift_y++;
		} else {
			saveScreen();// 方块不能下降时，保存到m_screen
			//m_screen_a=m_screen.clone();
			for (int i = 0; i < game_row; i++) {
				for (int j = 0; j < game_col; j++) {
					m_screen_a[i][j]=m_screen[i][j];
				}}
			clearLine();// 尝试清行
			shift_x = 3;
			shift_y = 0;
			k = kk;
			kk = m_Random.nextInt(28);

		}
	}

	private void saveScreen() {// 方块不能下降时，保存到m_screen
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (Shape.state[k][i][j] == 1) {
					m_screen[i + shift_y][j + shift_x] = 1;
					
				}
			}
		}

		// TODO Auto-generated method stub

	}

	public void goRight() {// 方块右移
		if (canGoRight()) {
			shift_x++;
		}

	}

	public void goLeft() {// 方块左移
		if (canGoLeft()) {
			shift_x--;
		}

	}

	public void rotate() {// 方块旋转
		int shap = (k) / 4;// 判断形状
		int order = (k) % 4;// 判断状态
		if (order == 3) {
			order = 0;
		} else {
			order = order + 1;
		}
		int k_turn = shap * 4 + order;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (Shape.state[k_turn][i][j] != 0) {
					if ((i + shift_y) > 19 || (j + shift_x) < 0
							|| (j + shift_x) > 9) {
						return;
					} else if (m_screen[i + shift_y][j + shift_x] != 0) {
						return;
					}
				}
			}
		}
		k = k_turn;
	}

	public boolean canGoLeft() {// 方块能否左移
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (Shape.state[k][i][j] != 0) {
					if ((j + shift_x - 1) < 0) {
						return false;
					} else if (m_screen[i + shift_y][j + shift_x - 1] != 0) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public boolean canGoRight() {// 方块能否右移
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (Shape.state[k][i][j] != 0) {
					if ((j + shift_x + 1) >= game_col) {
						return false;
					} else if (m_screen[i + shift_y][j + shift_x + 1] != 0) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public boolean canGoDown() {// 方块能否下移
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (Shape.state[k][i][j] == 1) {
					if ((i + shift_y + 1) < game_row) {
						if (m_screen[i + shift_y + 1][j + shift_x] == 1)
							return false;
					} else {
						return false;
					}
		
				}

			}
		}
		return true;

	}

	public void clearLine() {//清行与算分 
		for (int i = shift_y; i < game_row; i++) {
			if (canClearLine(i)) {
				for (int j = i; j > 3; j--) {
					for (int k = 0; k < game_col; k++) {
						m_screen[j][k] = m_screen[j - 1][k];
											}
				}
				line++;
				if (line <= 2) {//每消除若干行难度上升
					score = line * 10;

				} else if (line > 2 & line <= 4) {
					speed_level = "normal";
					score = score + 30;
					speed_number = 8;
				} else if (line > 4) {
					speed_level = "hard";
					score = score + 50;
					speed_number = 14;
				}

			}
		}
	}

	
	public boolean canClearLine(int i) {//能否清行

		for (int j = 0; j < game_col; j++) {
			if (m_screen[i][j] == 0)
				return false;
		}
		return true;

	}

	public boolean isGameOver() {// 判断游戏是否结束
		for (int i = 0; i < game_col; i++) {
			if (m_screen[3][i] != 0) {
				return true;
			}
		}
		return false;
	}
}
