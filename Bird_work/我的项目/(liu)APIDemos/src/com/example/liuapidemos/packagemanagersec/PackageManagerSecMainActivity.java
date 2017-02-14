package com.example.liuapidemos.packagemanagersec;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.Loader.ForceLoadContentObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import com.example.liuapidemos.R;

public class PackageManagerSecMainActivity extends Activity {
	private List<PackagerManagerSecAppInfo> appInfos = new ArrayList<PackagerManagerSecAppInfo>();
	private List<PackagerManagerSecAppInfo> appInfos1=new ArrayList<PackagerManagerSecAppInfo>(); //use in search()
	List<ApplicationInfo> applicationInfos;
	List<ResolveInfo> resolveInfos;
	EditText appName;
	PackageManager pm;
	InputMethodManager imm;
	String input;
	Button checkButton;
	Button currentInterfaceButton;
	Button DesktopButton;
	Button InputMethodButton;
	PackagerManagerSecAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.package_manager_sec_activity_main);
		appName = (EditText) findViewById(R.id.editText1);
		checkButton = (Button) findViewById(R.id.check);
		currentInterfaceButton = (Button) findViewById(R.id.currentInterface);
		DesktopButton = (Button) findViewById(R.id.desktop);
		InputMethodButton = (Button) findViewById(R.id.inputMethod);
		pm = getPackageManager();
		imm = (InputMethodManager) getSystemService(PackageManagerSecMainActivity.INPUT_METHOD_SERVICE);
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		resolveInfos = pm.queryIntentActivities(mainIntent, 0);
		applicationInfos = pm
				.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		adapter = new PackagerManagerSecAdapter(PackageManagerSecMainActivity.this, R.layout.package_manager_sec_appinfo_item,
				appInfos);
		ListView listView = (ListView) findViewById(R.id.listView1);
		listView.setAdapter(adapter);

		checkButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				search();
				adapter.notifyDataSetChanged();
			}
		});
		currentInterfaceButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				getCurrentInterfaceApp();
				adapter.notifyDataSetChanged();
			}
		});
		DesktopButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try {
					getDesktopApp();
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				adapter.notifyDataSetChanged();
			}
		});
		InputMethodButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					getInputMethod();
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				adapter.notifyDataSetChanged();
			}
		});
		appName.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable arg0) {//reponse for text changed
				// TODO Auto-generated method stub
				search();
				adapter.notifyDataSetChanged();
			}
		});
	}

	public void search() {// get app packagename,apk location in mobile phone
//		appInfos.clear();
		input = appName.getText().toString();
		 if(appInfos1.size()==0){
		for (PackagerManagerSecAppInfo appInfo : appInfos) {
						appInfos1.add(appInfo);
			}
		 }
		appInfos.clear();
		for (PackagerManagerSecAppInfo appInfo : appInfos1) {
			if (((String) appInfo.getAppLabel()).contains(input)){
				appInfos.add(appInfo);
			}
		}
	}

	public void getCurrentInterfaceApp() {// find the package name/ classname current interface
		appInfos.clear();
		appInfos1.clear();
		PackagerManagerSecAppInfo mAppInfo = null;
	}

	public void getDesktopApp() throws NameNotFoundException {// //apps with icon on desktop--packagename / classname
		
		appInfos.clear();
		appInfos1.clear();
		int i=0;
		for (ResolveInfo reInfo : resolveInfos) {
			String className = reInfo.activityInfo.name; 
			String pkgName = reInfo.activityInfo.packageName; 
			String appLabel = (String) reInfo.loadLabel(pm);
			// Log.e("shit", appLabel);
			ApplicationInfo mApplicationInfo = pm
					.getApplicationInfo(pkgName, 0);//get ApplicationInfo through packagename in order to get apk sourceDir
			String sourceDir = mApplicationInfo.sourceDir;
			Drawable appIcon = reInfo.loadIcon(pm); 
			PackagerManagerSecAppInfo appInfo = new PackagerManagerSecAppInfo(appLabel, appIcon, pkgName,
					className, sourceDir);
			appInfos.add(appInfo); 
			i++;
		}
		Log.e("app number","num"+i );
	}

	public void getInputMethod() throws NameNotFoundException {//find the package name / service name of all the input methods in the cell phone.
		appInfos.clear();
		appInfos1.clear();
		int i=0;
		List<InputMethodInfo> methodList = imm.getInputMethodList();
		for (InputMethodInfo inputInfo : methodList) {
			String className = inputInfo.getServiceName();
			String pkgName = inputInfo.getPackageName(); 
			String appLabel = (String) inputInfo.loadLabel(pm); 
			ApplicationInfo mApplicationInfo = pm
					.getApplicationInfo(pkgName, 0);//get ApplicationInfo through packagename in order to get apk sourceDir
			String sourceDir = mApplicationInfo.sourceDir;
			Drawable appIcon = inputInfo.loadIcon(pm); 
			PackagerManagerSecAppInfo appInfo = new PackagerManagerSecAppInfo(appLabel, appIcon, pkgName,
					className, sourceDir);
			appInfos.add(appInfo); 
			i++;
		}
		Log.e("input method number","num"+i );
	}
}
