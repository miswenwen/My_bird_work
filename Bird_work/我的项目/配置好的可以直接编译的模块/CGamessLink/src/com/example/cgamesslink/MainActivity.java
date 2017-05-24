package com.example.cgamesslink;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent= new Intent();          
		  intent.setAction("android.intent.action.VIEW");      
		  Uri content_url = Uri.parse("http://www.baidu.com");     
		  intent.setData(content_url);    
		  startActivity(intent);  
		  finish();
	}
}
