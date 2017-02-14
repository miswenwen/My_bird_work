package com.example.liuapidemos.database;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.liuapidemos.R;

public class DatabaseMainActivity extends Activity {
	private DatabaseHelper dbHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.database_main);
		dbHelper = new DatabaseHelper(this, "BookStore.db", null, 1);
		Button createDatabase = (Button) findViewById(R.id.create_database);
		createDatabase.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(dbHelper.getReadableDatabase()!=null){
					Toast.makeText(DatabaseMainActivity.this, "already created", 1).show();
				}
				dbHelper.getWritableDatabase();
			}
		});
	}
}
