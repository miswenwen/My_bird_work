package com.example.mytetris;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class MenuActivity extends Activity{
	Button button_newgame;//新游戏
	Button button_highscore;//高分榜
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.menu);
		button_newgame=(Button)findViewById(R.id.new_game);
		button_highscore=(Button)findViewById(R.id.high_score);
		button_newgame.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0){
				Intent intent=new Intent(MenuActivity.this,GameActivity.class);
				startActivity(intent);
			}
		});
		button_highscore.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0){
				Intent intent=new Intent(MenuActivity.this,HighScoreActivity.class);
				startActivity(intent);	
}
		});
	
}
}
