package com.android.settings.fingerprint;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.ListView;
import android.os.AsyncTask;
import java.io.Serializable;
import com.android.settings.R;
import android.content.ComponentName;

import android.util.LruCache;
import com.android.settings.fingerprint.applock.CommonAdapter;
import com.android.settings.fingerprint.applock.ViewHolder;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ProgressBar;
import android.app.Activity;

public class AliFingerQuickAppList extends Activity {
    private final String TAG = "AliFingerQuickAppList";
    private static final int STARTPAGE_SIZE = 10;
    private static final int INIT_PKG = 1;
	private static final int HIDE_PROGRESSBAR = 2;
    private Context mContext;
    private PackageManager mPkgManager = null;
    private static AppInfo[] startPageAppInfo;
    private boolean isFirstEnter = true;
    private ImageCache mImageCache;
//    private GetAppListTask mTask;
    private List<AppInfo> mAppInfoList;
//    private List<Item> mFirstPageAppList;
    private boolean mHasFewApp = false;
	
	private static ProgressBar mProgressBar = null;
	private static ListView mAppsListView;
	private static CommonAdapter<AppInfo> mAdapter;

    private int mFingerId;
    private String mTargetApp;
    private String mTargetActivity;
    //private final ComponentName[] mPriorityAppList = {new ComponentName("com.yunos.vui", "com.yunos.vui.MainActivity")};
    private static final String[] sShieldList = {
            "com.aliyun.homeshell.Launcher",
            "com.yunos.theme.thememanager.ThemeManagerSlideActivity",
            "com.android.speechrecorder.SpeechRecorderActivity",
            "com.android.stk.StkLauncherActivity",
            "com.aliyun.mobile.ime.AImeSettingsAct",
            "com.android.voicedialer.VoiceDialerActivity",
            "com.aliyun.fota.FotaSystemInformation",
            "com.android.deskclock.DeskClock",
            "com.android.settings.Settings"};

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if ((msg == null) || isFinishing())
                return;

            switch (msg.what) {
            case INIT_PKG:
/*
                if (!mHasFewApp && mFirstPageAppList != null) {
                    ItemAdapter adapter = new ItemAdapter(mContext, mFirstPageAppList);
                    setListAdapter(adapter);
                }
                mTask = new GetAppListTask();
                mTask.execute();
*/
                break;
			case HIDE_PROGRESSBAR:
				mProgressBar.setVisibility(View.GONE);
				mAppsListView.setAdapter(mAdapter);
				mAdapter.notifyDataSetChanged();
			break;
            default:
                break;
            }
        }
    };
/*
    class GetAppListTask extends AsyncTask<Void, Void, Void> {
        List<Item> mAppList;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mAppInfoList != null) {
                mAppList = setLockListItem(mAppInfoList);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ItemAdapter adapter = new ItemAdapter(mContext, mAppList);
            setListAdapter(adapter);
            isFirstEnter = false;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

    }
*/
    public static class AppInfo {
        String packageName;
        String appName;
        String activityName;
        Drawable icon;
        boolean isSecret;
        boolean isPriority;
    }

    private static boolean isAppShielded(String s) {
        if (s == null)
            return false;
        if (s.startsWith("com.aliyun.SecurityCenter"))
            return true;

        int len = sShieldList.length;
        for (int index = 0; index < len; index++) {
            if (sShieldList[index].equals(s)) {
                return true;
            }
        }
        return false;
    }
/*
    private void tryAddPriorityApp(List<AppInfo> appInfoList) {
        int len = mPriorityAppList.length;
        for (int index = 0; index < len; index++) {
            boolean hasExisted = false;
            for (AppInfo ai : appInfoList) {
                if(ai.packageName.equals(mPriorityAppList[index].getPackageName())
                        && ai.activityName.equals(mPriorityAppList[index].getClassName())) {
                    ai.isPriority = true;
                    hasExisted = true;
                    break;
                }
            }
            if (!hasExisted) {
                AppInfo info = new AppInfo();
                info.packageName = mPriorityAppList[index].getPackageName();
                info.activityName = mPriorityAppList[index].getClassName();
                info.appName = AliFingerprintUtils.getApplicationName(mContext, info.packageName, info.activityName);
                if (mTargetApp != null && mTargetApp.equals(info.packageName)
                        && mTargetActivity != null && mTargetActivity.equals(info.activityName)) {
                    info.isSecret = true;
                } else {
                    info.isSecret = false;
                }
                info.isPriority = true;
                appInfoList.add(info);
            }
        }
    }
*/
    /*private String getHumanReadableName(List<ResolveInfo> apps, String pkgName, String actName) {
        StringBuffer buffer = new StringBuffer();
        for (ResolveInfo ri : apps) {
            if (ri.activityInfo.packageName.equals(pkgName) && ) {
                buffer.append(ri.loadLabel(mPkgManager));
                buffer.append(",");
            }
        }
        return buffer.substring(0, buffer.length() - 1);
    }*/
    private void getAppList() {
//        mFirstPageAppList.clear();
        mAppInfoList.clear();
        final List<ResolveInfo> apps = AliFingerprintUtils.getAllPackages(mPkgManager);
        for (ResolveInfo pi : apps) {
            if (isAppShielded(pi.activityInfo.name)) {
                continue;
            }
            AppInfo info = new AppInfo();
            info.packageName = pi.activityInfo.packageName;
            info.activityName = pi.activityInfo.name;
            info.appName = pi.activityInfo.loadLabel(mPkgManager).toString();
			
            CacheInfo cacheInfo = mImageCache.getFromCache(info.activityName);
            try {
                if (cacheInfo != null) {
                    info.icon = mPkgManager.getActivityIcon(new ComponentName(info.packageName, info.activityName));
                } else {
                    CacheInfo newcache = new CacheInfo();
                    Drawable value = mPkgManager.getActivityIcon(new ComponentName(info.packageName, info.activityName));
                    newcache.setIcon(value);
                    newcache.setAppName(info.appName);
                    info.icon = value;
                    mImageCache.add2Cache(info.activityName, newcache);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "add image to cache error");
            }			
			
            if (mTargetApp != null && mTargetApp.equals(pi.activityInfo.packageName)
                    && mTargetActivity != null && mTargetActivity.equals(pi.activityInfo.name)) {
                info.isSecret = true;
            } else {
                info.isSecret = false;
            }
            info.isPriority = false;
			Log.e("lcf_finger", " info.appName==> "+info.appName);
            mAppInfoList.add(info);
        }
		//Log.e("lcf_finger", "tryAddPriorityApp mAppInfoList.size(): "+mAppInfoList.size());
        //tryAddPriorityApp(mAppInfoList);
        Collections.sort(mAppInfoList, ALPHA_COMPARATOR_ALIYUN);
		sendMessageToHandler(HIDE_PROGRESSBAR);
/*
        if (mAppInfoList != null && STARTPAGE_SIZE < mAppInfoList.size()) {
            for (int i = 0; i < STARTPAGE_SIZE; i++) {
                startPageAppInfo[i] = mAppInfoList.get(i);
            }
            mFirstPageAppList = setLockListItem(Arrays.asList(startPageAppInfo));
            mHasFewApp = false;
        } else {
            mHasFewApp = true;
        }
*/
    }
/*
    private List<Item> setLockListItem(List<AppInfo> list) {
        List<Item> itemList = new ArrayList<Item>();
        for (AppInfo info : list) {
            DrawableText2Item item = null;
            Drawable lock;

            boolean isContained = false;
            for (Item it : itemList) {
                AppInfo ai = (AppInfo) it.getTag();
                if (ai != null && ai.packageName.equals(info.packageName) && ai.activityName.equals(info.activityName))
                    isContained = true;
            }

            if (isContained) {
                continue;
            }

            if (info.isSecret == true) {
                lock = getResources().getDrawable(
                        R.drawable.app_lists_selected);
            } else {
                lock = null;
            }

            CacheInfo cacheInfo = mImageCache.getFromCache(info.activityName);
            try {
                if (cacheInfo != null) {
                    item = new DrawableText2Item(cacheInfo.getAppName(), null, cacheInfo.getIcon(), lock, ImageView.ScaleType.FIT_XY);
                } else {
                    CacheInfo newcache = new CacheInfo();
                    Drawable value = mPkgManager.getActivityIcon(new ComponentName(info.packageName, info.activityName));
                    newcache.setIcon(value);
                    newcache.setAppName(info.appName);
                    item = new DrawableText2Item(info.appName, null, value, lock, ImageView.ScaleType.FIT_XY);
                    mImageCache.add2Cache(info.activityName, newcache);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "add image to cache error");
            }
            if(item != null) {
                item.setTag(info);
                itemList.add(item);
            }
        }
        return itemList;
    }
*/
    public final Comparator<AppInfo> ALPHA_COMPARATOR_ALIYUN = new Comparator<AppInfo>() {
        private final Collator sCollator = Collator.getInstance();

        public final int compare(AppInfo a, AppInfo b) {
            if (a == null || b == null)
                return -1;

            if (a.appName == null)
                return -1;

            if (b.appName == null)
                return 1;

            if(a.isSecret && !b.isSecret) {
                return -1;
            } else if (b.isSecret && !a.isSecret) {
                return 1;
            }

            if(a.isPriority && !b.isPriority) {
                return -1;
            } else if (b.isPriority && !a.isPriority) {
                return 1;
            }

            return sCollator.compare(trimNonBreakingSpace(a.appName.trim()),
                    trimNonBreakingSpace(b.appName.trim()));
        }

        public String trimNonBreakingSpace(String str) {
            if (str == null || str.length() == 0) {
                return str;
            }
            char[] value = str.toCharArray();
            int start = 0;
            int end = value.length - 1;
            while ((start <= end) && (value[start] == (char) 160)) {
                start++;
            }
            if (start == 0) {
                return str;
            }
            return new String(value, start, end - start + 1);
        }
    };
/*
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ItemAdapter adapter = (ItemAdapter) l.getAdapter();
        DrawableText2Item item = (DrawableText2Item) adapter.getItem(position);
        AppInfo ai = (AppInfo) item.getTag();

        if (ai == null) {
            return;
        }

        boolean isChecked = ai.isSecret;
        if (!isChecked) {
            Intent intent = new Intent();
            intent.putExtra("packageName", ai.packageName);
            intent.putExtra("activityName", ai.activityName);
            intent.putExtra("appName", ai.appName);
            setResult(0, intent);
            finish();
            /*ai.isSecret = true;
            item.rightDrawable = getResources().getDrawable(
                    R.drawable.ic_fingerprint_next);*/
/*        }

        ItemView view = (ItemView) v;
        item.setTag(ai);
        view.setObject(item);
    }
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
		
		setContentView(R.layout.fingerprint_quickapp_list);
		
        Intent intent = getIntent();
        if(intent == null) return;
        mFingerId = intent.getIntExtra("fingerid", 0);
        mTargetApp = intent.getStringExtra("target");
        mTargetActivity = intent.getStringExtra("activity");

        setTitle(getResources().getString(R.string.fingerquick_select_app));
        startPageAppInfo = new AppInfo[STARTPAGE_SIZE];
        mImageCache = ImageCache.getInstance();
        mPkgManager = getPackageManager();
        mAppInfoList = new ArrayList<AppInfo>();
//        mFirstPageAppList = new ArrayList<Item>();
		
		mProgressBar = (ProgressBar) findViewById(R.id.id_enablelock_apps_pb);
		mAppsListView = (ListView) findViewById(R.id.id_lv_main);
		mAdapter = new CommonAdapter<AppInfo>(AliFingerQuickAppList.this, mAppInfoList, R.layout.bird_fingerprint_quickapplist_item) {
			@Override
			public void convert(ViewHolder holder, final AppInfo item) {
				holder.setText(R.id.id_item_appname_tv, item.appName);
				holder.setImageDrawable(R.id.id_item_appicon_iv, item.icon);
			}
		};
		
		mAppsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				android.util.Log.i("lcf_finger","setOnItemClickListener arg2: " + arg2);
				CommonAdapter<AppInfo> adapter = (CommonAdapter) mAppsListView.getAdapter();
				AppInfo ai = (AppInfo) adapter.getItem(arg2);
				
				if (ai == null) {
					return;
				}

				boolean isChecked = ai.isSecret;
				if (!isChecked) {
					Intent intent = new Intent();
					intent.putExtra("packageName", ai.packageName);
					intent.putExtra("activityName", ai.activityName);
					intent.putExtra("appName", ai.appName);
					setResult(0, intent);
					AliFingerQuickAppList.this.finish();
				}
			}
		});		
    }

    private class GetListLoaderThread extends Thread {
        public GetListLoaderThread() {
            super();
            setPriority(Thread.MIN_PRIORITY);
        }

        public void run() {
            getAppList();
/*
            if (mFirstPageAppList == null)
                return;

            sendMessageToHandler(INIT_PKG);
*/
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        isFirstEnter = true;
        GetListLoaderThread thread = new GetListLoaderThread();
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
/*
        if (mTask != null)
            mTask.cancel(true);
*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void sendMessageToHandler(int msgId) {
        mHandler.sendEmptyMessage(msgId);
    }

/*************************************************************************/
    public class CacheInfo implements Serializable {

        private static final long serialVersionUID = 1L;
        private String appName;
        private String packageName;
        private Drawable icon;

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public Drawable getIcon() {
            return icon;
        }

        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

    }
/*************************************************************************/
    private static class ImageCache {

        private static final int MAX_CACHE_SIZE = 100;

        public LruCache<String, CacheInfo> mMemoryCache;

        private static ImageCache sInstance;

        public static ImageCache getInstance() {
            synchronized (ImageCache.class) {
                if (sInstance == null) {
                    sInstance = new ImageCache();
                }
                return sInstance;
            }
        }

        private ImageCache() {
            init();
        }

        private void init() {
            mMemoryCache = new LruCache<String, CacheInfo>(MAX_CACHE_SIZE);
        }

        public void add2Cache(String data, CacheInfo value) {
            try {
                mMemoryCache.put(data, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public CacheInfo getFromCache(String data) {
            CacheInfo cacheInfo = null;
            try {
                cacheInfo = mMemoryCache.get(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return cacheInfo;
        }

        public CacheInfo removeFromCache(String data) {
            CacheInfo cacheInfo = null;
            try {
                cacheInfo = mMemoryCache.remove(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return cacheInfo;
        }

        public void removeAllCache() {
            try {
                mMemoryCache.evictAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

