package com.example.liuapidemos.broadcast;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.liuapidemos.R;

public class BroadcastMainActivity extends BaseActivity {
	EditText accoutEdit;
	EditText passwordEdit;
	Button login;
	MyBroadcastReceiver myBroadcastReceiver;
	IntentFilter mFilter;

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.broadcast_main);
		accoutEdit = (EditText) findViewById(R.id.account);
		passwordEdit = (EditText) findViewById(R.id.password);
		login = (Button) findViewById(R.id.login);
		login.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				String accout = accoutEdit.getText().toString();
				String paasword = passwordEdit.getText().toString();
				if (accout.equals("liuqipeng") && paasword.equals("123456")) {
					Intent mIntent = new Intent(BroadcastMainActivity.this,
							BroadcastSecondActivity.class);
					startActivity(mIntent);
				} else {
					Toast.makeText(BroadcastMainActivity.this, "error,input again",
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
