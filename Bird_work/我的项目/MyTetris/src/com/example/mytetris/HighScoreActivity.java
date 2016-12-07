package com.example.mytetris;

import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class HighScoreActivity extends Activity {
	ArrayList<Integer> test = new ArrayList<Integer>();
	ArrayAdapter<Integer> adapter;
	SharedPreferences score;
	SharedPreferences.Editor editor;
	ListView listView;
	Button clear;//清空历史数据

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.highscore);
		clear = (Button) findViewById(R.id.clear);
		score = getSharedPreferences("my_score", MODE_PRIVATE);
		int score_total=score.getInt("total", 0);//分数总个数
		for (int i = 0; i < score_total; i++) {
			int x = score.getInt("rank" + i, -1);
			test.add(x);
		}
		adapter = new ArrayAdapter<Integer>(HighScoreActivity.this,
				android.R.layout.simple_list_item_1, test);
		listView = (ListView) findViewById(R.id.list_view);
		listView.setAdapter(adapter);
		clear.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				editor = score.edit();
				editor.clear();//清空SharedPreferences
				editor.commit();
				test.clear();//清空数组
				adapter.notifyDataSetChanged();//adapter内容改变后，刷新ListView
			}
		});
	}
}
