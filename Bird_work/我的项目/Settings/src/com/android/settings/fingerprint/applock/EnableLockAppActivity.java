package com.android.settings.fingerprint.applock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.RelativeLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settings.fingerprint.support.AppData;
import com.android.settings.fingerprint.support.PackagesConstant;
import com.android.settings.fingerprint.support.PreferenceUtils;

import com.android.settings.R;

import java.util.regex.*; 

public class EnableLockAppActivity extends Activity {
	
	private static final String TAG = "EnableLockAppActivity";

	private static CommonAdapter<AppInfo> mAdapter;
	private static ListView mAppsListView;
	private List<AppInfo> mAppsData = null;
	private PackageManager mPackageManager;

	private static ProgressBar mProgressBar = null;
	private Switch mAppSwictch = null;
	
    private static Context mContext = null;
    			
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = this;
		
		setContentView(R.layout.activity_enablelock_apps);

        ActionBar bar = getActionBar();
        if (bar != null) {
			bar.setElevation(0);
        }
        mAppsData = new ArrayList<AppInfo>();
        
		String packagenameString = getPackageName();

		mAppSwictch = (Switch) findViewById(R.id.id_manage_applock_off_on_sw);
		//mAppSwictch.setOnClickListener(switchClickListener);
		mAppSwictch.setOnCheckedChangeListener(appLockCheckedChangeListener);
		
		mProgressBar = (ProgressBar) findViewById(R.id.id_enablelock_apps_pb);
		mPackageManager = getPackageManager();

		mAppsListView = (ListView) findViewById(R.id.id_lv_main);

		mAdapter = new CommonAdapter<AppInfo>(EnableLockAppActivity.this, mAppsData, R.layout.lockapp_item_list) {

			@Override
			public void convert(ViewHolder holder, final AppInfo item) {
				holder.setText(R.id.id_item_appname_tv, item.appName);
				holder.setImageDrawable(R.id.id_item_appicon_iv, item.appIcon);
				
				final Switch cb = holder.getView(R.id.id_item_needlock_cb);
				cb.setChecked(item.isChecked());
				cb.setEnabled(PreferenceUtils.isSettingFpAppLockOn());
				cb.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {	
						item.setChecked(cb.isChecked());
						String lastPkgName = Settings.System.getString(AppData.getContext().getContentResolver(), PackagesConstant.SETTINGS_LAST_LOCK_APP_PACKAGENAME);
						if (lastPkgName.equals(item.packageName)) {
							if (!cb.isChecked()) {
								Settings.System.putString(AppData.getContext().getContentResolver(), PackagesConstant.SETTINGS_LAST_LOCK_APP_PACKAGENAME,"");
							} else {
								Settings.System.putString(AppData.getContext().getContentResolver(), PackagesConstant.SETTINGS_LAST_LOCK_APP_PACKAGENAME,item.packageName);
							}
						}
					}		
				});
				
				RelativeLayout container = (RelativeLayout) holder.getView(R.id.id_app_item_container);
				container.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mAppSwictch.isChecked()) {
						    cb.setChecked(!item.isChecked());
							item.setChecked(cb.isChecked());
						}
					}
				});
			}
		};

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				queryAppInfo();
			}
		}, 500);

		mAppSwictch.setChecked(PreferenceUtils.isSettingFpAppLockOn());
	}
	
	private OnCheckedChangeListener appLockCheckedChangeListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			Log.e(TAG, " appLockCheckedChangeListener"+mAppSwictch.isChecked());
			PreferenceUtils.setSettingFpAppLockOffOn(mAppSwictch.isChecked());
			mAdapter.notifyDataSetChanged();

		}
	};
	
    private boolean mFlag = false;
	private static Handler mHandler = new Handler();

    public void queryAppInfo() {
    	String[] exclueApps = getResources().getStringArray(R.array.exclude_app);
    	
		List<String> existLockAppsList = new ArrayList<String>();
		List<PackageInfo> allPackages = getPackageManager().getInstalledPackages(0);

		StringBuilder stringBuilder = new StringBuilder();
		String appString = Settings.System.getString(getContentResolver(), PackagesConstant.SETTINGS_NEEDLOCK_APP_PACKAGENAMES);
		
		if (appString != null) {
			String[] appsStrings = appString.split("\\|");
			existLockAppsList = Arrays.asList(appsStrings);
		}
		
        PackageManager pm = this.getPackageManager(); 
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
 
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        Pattern p = Pattern.compile("^com.google.android"); 
        if (mAppsData != null) {
        	mAppsData.clear();
            for (ResolveInfo reInfo : resolveInfos) {
                String activityName = reInfo.activityInfo.name;
                String pkgName = reInfo.activityInfo.packageName;
                String appLabel = (String) reInfo.loadLabel(pm);
                Drawable icon = reInfo.loadIcon(pm); 
                
                

                AppInfo appInfo = new AppInfo();
                appInfo.packageName = pkgName;
                appInfo.appName = appLabel;
                appInfo.appIcon = (icon);

    			if (existLockAppsList.contains(pkgName)) {
    				appInfo.setChecked(true);
    			}
    			Matcher m = p.matcher(pkgName);
				android.util.Log.i(TAG, "pkgName = " + pkgName);
    			if (!pkgName.equals(PackagesConstant.FINGERPRINTUNLCOK_PACKAGENAME) 
						&& !pkgName.equals("com.bird.flashlight")
						&& !pkgName.equals("com.android.providers.downloads.ui")
						&& !pkgName.equals("com.android.deskclock")
						&& !pkgName.equals("com.bird.assistant")
						&& !pkgName.equals("com.bird.cleantask")
                        && !pkgName.equals("com.bird.xuan")
						&& !pkgName.equals("com.android.vending") // google play stores
						&& !m.find()) {
    				int mSamePkgName = 0;
    				for (int i = 0; i < mAppsData.size(); i++) {
						if (mAppsData.get(i).packageName.equals(pkgName)) {
						    mAppsData.get(i).appName = mAppsData.get(i).appName + " , " + appLabel;
							mSamePkgName += 1;
							break;
						}
					}
    				if (mSamePkgName == 0) {
    					mAppsData.add(appInfo);
					}
				}
            }
        }
        
        mFlag = true;
        
        mProgressBar.setVisibility(View.GONE);
		mAppsListView.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();

        new SaveLockAppsThread().start();

        finish();
	}

	private class SaveLockAppsThread extends Thread {
		@Override
		public void run() {
	        if (mFlag) {
		        if (mAppsData != null) {
                    Settings.System.putString(mContext.getContentResolver(),PackagesConstant.SETTINGS_NEEDLOCK_APP_PACKAGENAMES, null);
			        StringBuilder stringBuilder = new StringBuilder();
			        for (int i = 0; i < mAppsData.size(); i++) {
				        if (mAppsData.get(i).isChecked) {
					        stringBuilder.append(mAppsData.get(i).packageName + "|");
					        Log.v(TAG, "tmpInfo : " + stringBuilder.toString());
					        Settings.System.putString(mContext.getContentResolver(), PackagesConstant.SETTINGS_NEEDLOCK_APP_PACKAGENAMES, stringBuilder.toString());
				        }
			        }
		        }
            }
		}
	}

	class AppInfo {
		public String appName = "";
		public String packageName = "";
		public String versionName = "";
		public int versionCode = 0;
		public Drawable appIcon = null;
		private boolean isChecked = false;

		public boolean isChecked() {
			return isChecked;
		}

		public void setChecked(boolean check) {
			isChecked = check;
		}

		public void print() {
			Log.v(TAG, "Name:" + appName + " |  Package:" + packageName);
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	setResult(111);
        	PreferenceUtils.enablePwd(false);
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			setResult(111);
			PreferenceUtils.enablePwd(false);
			finish();
			break;

		default:
			break;
		}

		return true;
	}


}
