package com.example.mytetris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class GameActivity extends Activity {
	 ArrayList<Integer> myscore;
	boolean isstop = false;// 暂停判断标志位
	boolean ishighscore=true;//是否入围高分榜标志位
	Data data ;
	Timer timer;
	Handler m_Handler ;
	GameView m_GameView ;
	PreviewView m_PreviewView;
	TextView m_score ;
	TextView m_spd ;
	Button button_Right;
	Button button_Left;
	Button button_Rotate;
	Button button_Down;
	Button button_Pause;
	int count = 0;
	static final int MSG_START = 1;// 每隔一定时间发送的消息
	SharedPreferences m_sp = null;// 存储最高分
   

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.game);
		myscore=new ArrayList<Integer>();
     	initTools();
	}

	public void initTools() {
	
		data = new Data();
		m_GameView = (GameView) findViewById(R.id.gameview);
		m_PreviewView = (PreviewView) findViewById(R.id.preview);
		m_score = (TextView) findViewById(R.id.score);
		m_spd = (TextView) findViewById(R.id.speed_level);
		button_Left = (Button) findViewById(R.id.b1);
		button_Rotate = (Button) findViewById(R.id.b2);
		button_Down = (Button) findViewById(R.id.b3);
		button_Right = (Button) findViewById(R.id.b4);
		button_Pause = (Button) findViewById(R.id.pause_game);
		
		m_score.setText(getString(R.string.score )+ Data.score);
		m_spd.setText(getString(R.string.speed) + Data.speed_level);
		m_Handler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case MSG_START:
					if (count % (20 - Data.speed_number) == 0) {
						data.goDown();
						m_score.setText(getString(R.string.score ) + Data.score);
						m_spd.setText(getString(R.string.speed) + Data.speed_level);
						
					
					}
				
					m_GameView.invalidate();
					m_PreviewView.invalidate();
					
					if (data.isGameOver()) {
						onGameOver();
					}
				}
			}

		};
		timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				if (isstop == false) {
					m_Handler.sendEmptyMessage(MSG_START);
					count++;
				}
			}
		}, 1000, 50);
		button_Pause.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (isstop == false) {
					isstop = true;
					button_Pause.setText(R.string.resume);
				} else {
					isstop = false;
					button_Pause.setText(R.string.pause);
				}
			}
		});
		button_Left.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (isstop == false)
					data.goLeft();
				// TODO Auto-generated method stub

			}
		});
		button_Right.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (isstop == false)
					data.goRight();
				// TODO Auto-generated method stub

			}
		});
		button_Down.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (isstop == false)
					data.goDown();
				// TODO Auto-generated method stub

			}
		});
		button_Rotate.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				// TODO Auto-generated method stub
				if (isstop == false)
					data.rotate();
			}
		});
	}

	protected void onGameOver() {// 处理游戏结束事件,弹出对话框提示
		timer.cancel();
		ArrayList<Integer> myscore_a=new ArrayList<Integer>();
		m_sp=getSharedPreferences("my_score", MODE_PRIVATE);
		for(int i=0;i<m_sp.getInt("total",0);i++){
			myscore_a.add(m_sp.getInt("rank"+i, -1));
		} 
		myscore_a.add(Data.score);
		Collections.sort(myscore_a);
        Collections.reverse(myscore_a);
        if(myscore_a.size()==11){
        
        	if(myscore_a.get(10)==Data.score){
        	        	ishighscore=false;}
        	      	
        }
	
	
		AlertDialog.Builder bulider = new AlertDialog.Builder(this);// 设置游戏结束对话框
		if(ishighscore==true){
			bulider.setMessage(R.string.game_over);
		}
		else{
			bulider.setMessage(R.string.game_over_a);
		}
		
		bulider.setTitle(R.string.end);
		bulider.setPositiveButton(R.string.restart,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						initTools();
					}
				});
		bulider.setNegativeButton(R.string.exit,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						finish();
					}
				});
		bulider.create();
		bulider.show();
	}

	protected void onDestroy() {// 释放资源
		super.onDestroy();
		timer.cancel();
		m_sp=getSharedPreferences("my_score", MODE_PRIVATE);
		SharedPreferences.Editor editor=m_sp.edit();
		for(int i=0;i<m_sp.getInt("total",0);i++){
			myscore.add(m_sp.getInt("rank"+i, -1));
		} 
		myscore.add(Data.score);
		Collections.sort(myscore);
        Collections.reverse(myscore);
        if(myscore.size()==11){
        	myscore.remove(10);
        }
		for(int i=0;i<myscore.size();i++){	
			editor.putInt("rank"+i, myscore.get(i));
									}
		editor.putInt("total", myscore.size());//存储高分数个数
		editor.commit();

}
}