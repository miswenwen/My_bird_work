/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunos.alicontacts;

import android.app.Application;
import android.app.QueuedWork;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.RemoteViews;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.dialpad.CallLogFragment;
///bird: TASK #7674 add by lichengfeng 20160603 begin
import com.yunos.alicontacts.servicehotline.ServiceHotlineManager;
///bird: TASK #7674 add by lichengfeng 20160603 end
import com.yunos.alicontacts.group.GroupManager;
import com.yunos.alicontacts.list.AccountFilterManager;
import com.yunos.alicontacts.list.ContactListFilterController;
import com.yunos.alicontacts.list.DefaultContactBrowseListFragment;
import com.yunos.alicontacts.model.AccountTypeManager;
import com.yunos.alicontacts.plugins.PluginManager;
import com.yunos.alicontacts.plugins.PluginPlatformPrefs;
import com.yunos.alicontacts.sim.SimContactLoadService;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.test.InjectedServices;
import com.yunos.alicontacts.util.ActionPullParser;
import com.yunos.alicontacts.util.ContactPair;
import com.yunos.common.UsageReporter;

import yunos.support.v4.content.AsyncTaskLoader;
import yunos.ui.util.DynColorSetting;
import yunos.ui.util.ReflectHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public final class ContactsApplication extends Application {
    private static final String TAG = "ContactsApplication";
    private static final String TMPTAG = "PERFORMANCE";

    private static final String LOW_RAM_PROPERTY_NAME = "ro.config.low_ram";
    private static final int LOW_RAM_IMAGE_MEMORY_CACHE_SIZE_PERCENTAGE = 5;

    public static final boolean IS_LOW_MEMORY_DEVICE = "true".equals(SystemProperties.get(LOW_RAM_PROPERTY_NAME));
    public static final String LAST_REPORT_TIME_PREFS_NAME = "last_report_time";

    private static final long DELAYED_INITIALIZER_DELAY_TIME = 1000;

    private static InjectedServices sInjectedServices;
    private AccountTypeManager mAccountTypeManager;
    private ContactPhotoManager mContactPhotoManager;
    private ContactListFilterController mContactListFilterController;

    // dyncolor
    private DynColorSetting mDynColorSetting;
    private ArrayList<ContactPair<String, Integer>> mDynColorList1 = new ArrayList<ContactPair<String, Integer>>();
    private ArrayList<ContactPair<String, Integer>> mDynColorList2 = new ArrayList<ContactPair<String, Integer>>();

    private List<ActionPullParser.Customer> customerEngineList;

    @VisibleForTesting
    public static void injectServices(InjectedServices services) {
        sInjectedServices = services;
    }

    public static InjectedServices getInjectedServices() {
        return sInjectedServices;
    }

    public List getCustomerEngineList(){
        if(customerEngineList != null){
            return customerEngineList.size() > 0 ? customerEngineList : null;
        }

        return null;
    }

    @Override
    public ContentResolver getContentResolver() {
        if (sInjectedServices != null) {
            ContentResolver resolver = sInjectedServices.getContentResolver();
            if (resolver != null) {
                return resolver;
            }
        }
        return super.getContentResolver();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (sInjectedServices != null) {
            SharedPreferences prefs = sInjectedServices.getSharedPreferences();
            if (prefs != null) {
                return prefs;
            }
        }

        return super.getSharedPreferences(name, mode);
    }

    @Override
    public Object getSystemService(String name) {
        if (sInjectedServices != null) {
            Object service = sInjectedServices.getSystemService(name);
            if (service != null) {
                return service;
            }
        }

        if (AccountTypeManager.ACCOUNT_TYPE_SERVICE.equals(name)) {
            if (mAccountTypeManager == null) {
                mAccountTypeManager = AccountTypeManager.createAccountTypeManager(this);
            }
            return mAccountTypeManager;
        }

        if (ContactPhotoManager.CONTACT_PHOTO_SERVICE.equals(name)) {
            if (mContactPhotoManager == null) {
                mContactPhotoManager = ContactPhotoManager.createContactPhotoManager(this);
                registerComponentCallbacks(mContactPhotoManager);
                mContactPhotoManager.preloadPhotosInBackground();
            }
            return mContactPhotoManager;
        }

        if (ContactListFilterController.CONTACT_LIST_FILTER_SERVICE.equals(name)) {
            if (mContactListFilterController == null) {
                mContactListFilterController = ContactListFilterController
                        .createContactListFilterController(this);
            }
            return mContactListFilterController;
        }

        return super.getSystemService(name);
    }

    private void initDynColor() {
        mDynColorList1.clear();
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_COLOR, R.color.title_color));
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_TEXT_COLOR,
                R.color.activity_header_text_color));
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_TEXT_COLOR,
                R.color.people_title_txt_color_normal));
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_TEXT_COLOR_UNCHECKED,
                R.color.people_title_txt_color_disable));
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_WIDGET_NORMAL,
                R.color.header_widget_color_normal));
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_WIDGET_PRESSED,
                R.color.header_widget_color_pressed));
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_COLOR,
                R.color.header_widget_color_disable));
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_COLOR,
                R.color.appwidget_btn_bg_color_normal));
        mDynColorList1.add(ContactPair.create(DynColorSetting.HEADER_COLOR,
                R.color.appwidget_bg));

        mDynColorList2.clear();
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY,
                R.color.fish_eye_bg_color));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY_DARK,
                R.color.fish_eye_press_color));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY,
                R.color.contact_editor_listview_divider_selected));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY,
                R.color.match_contacts_hilite_color));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY,
                R.color.contact_create_image_color));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY_DARK,
                R.color.contact_create_image_press_color));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY,
                R.color.edge_effect_color));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY,
                R.color.contact_search_activity_btn_normal));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY_DARK,
                R.color.contact_search_activity_btn_press));
        mDynColorList2.add(ContactPair.create(DynColorSetting.HW_COLOR_PRIMARY_DISABLED,
                R.color.contact_search_activity_btn_disable));
    }

    @Override
    public void onCreate() {
        Log.v(TMPTAG, "ContactsApplication onCreate called");
        super.onCreate();

        // NOTE: ModernAsyncTask.sHandler need to be initialized in main thread.
        // In the BackgroundInitializerThread, we will start AsyncTaskLoader,
        // which will create instance of ModernAsyncTask.
        // If this is the first time we create ModernAsyncTask,
        // then the sHandler will run in wrong thread.
        // So we need to make sure ModernAsyncTask is loaded before BackgroundInitializerThread started.
        // But ModernAsyncTask is package scope, we have to initialize it via AsyncTaskLoader.
        // AsyncTaskLoader.forceLoad() will create an instance of ModernAsyncTask,
        // which can ensure the ModernAsyncTask.sHandler is initialized.
        AsyncTaskLoader<Void> uglyLoader = new AsyncTaskLoader<Void>(this) {
            @Override
            public Void loadInBackground() {
                // do nothing, just to make sure ModernAsyncTask.sHandler is created in main thread.
                return null;
            }
        };
        uglyLoader.forceLoad();

        // IMPORTANT: create CallLogManager as early as possible.
        CallLogManager.getInstance(this);
        // Perform the initialization that doesn't have to finish immediately.
        // We use an async task here just to avoid creating a new thread.
        new BackgroundInitializerThread1().start();
        new BackgroundInitializerThread2().start();

        // only check once for this application
        PluginPlatformPrefs.checkOperator();

        UsageReporter.init(getApplicationContext());
        Log.v(TMPTAG, "DynColor start");
        initDynColor();
        mDynColorSetting = new DynColorSetting(getResources().getConfiguration());
        DynColorSetting.setColorIDReady(getResources(), this.getPackageName());
        overlayDynColorRes(getResources(), mDynColorSetting, this.getPackageName());
        Log.v(TMPTAG, "DynColor end");

        Log.v(TMPTAG, "ContactsApplication end");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!mDynColorSetting.isSameColorMap(newConfig)) {
            mDynColorSetting.updateColorMap(newConfig);
            overlayDynColorRes(getResources(), mDynColorSetting, this.getPackageName());
        }
    }

    public void overlayDynColorRes(Resources res, DynColorSetting dynColorSetting, String pakName) {
        DynColorSetting.clearNewResArray(res);
        if (!dynColorSetting.isRestoreMode()) {
            SparseIntArray newcolors = new SparseIntArray();
            for (ContactPair<String, Integer> cPair : mDynColorList1) {
                int defColor = res.getColor(cPair.second);
                int dynColor = dynColorSetting.getColorValue(cPair.first, defColor);
                newcolors.put(cPair.second, dynColor);
            }
            for (ContactPair<String, Integer> cPair : mDynColorList2) {
                int defColor = res.getColor(cPair.second);
                int dynColor = dynColorSetting.getColorValue(cPair.first, defColor);
                if (DynColorSetting.isGreyColor(dynColor)) {
                    dynColor = defColor;
                }
                newcolors.put(cPair.second, dynColor);
            }
            int mode = dynColorSetting.getDarkMode(res.getBoolean(R.bool.contact_dark_mode));
            newcolors.put(R.bool.contact_dark_mode, mode);
            DynColorSetting.setNewResArray(res,
                    DynColorSetting.setPrimaryColor(res, dynColorSetting, newcolors, pakName));
            setDynColorsArray(newcolors);
        }
    }

    private class BackgroundInitializerThread1 extends Thread {
        @Override
        public void run() {
            CallLogFragment.warmUp();
            DefaultContactBrowseListFragment.warmUp();
            Context context = ContactsApplication.this;
            // Warm up the preferences, the account type manager and the
            // contacts provider.
            PreferenceManager.getDefaultSharedPreferences(context);
            getContentResolver().getType(ContentUris.withAppendedId(Contacts.CONTENT_URI, 1));

            ActionPullParser mPullParse = new ActionPullParser();
            customerEngineList = mPullParse.getUserList(ActionPullParser.ENGINEERING_ORDER_FILE_PATH);

            initImageLoader(context);
        }

    }

    private class BackgroundInitializerThread2 extends Thread {
        @Override
        public void run() {
            boolean isYunOS = SimUtil.IS_YUNOS;
            Context context = ContactsApplication.this;
            AccountTypeManager.getInstance(context);
            AccountFilterManager afm = AccountFilterManager.getInstance(getApplicationContext());

            if (isYunOS) {
                PluginManager.getInstance(context).initPlugins();
            }

            GroupManager.getInstance(context).checkDefaultGroupsOnInitial();

			///bird: TASK #7674 add by lichengfeng 20160603 begin
            // YunOS BEGIN PB
            // ##module:(Contacts)  ##author:shihuai.wg@alibaba-inc.com
            // ##BugID:(6499111)  ##date:2015-10-13
            ServiceHotlineManager.getInstance(getApplicationContext()).checkServiceHotline();
            // YunOS END PB
			///bird: TASK #7674 add by lichengfeng 20160603 end
			
            // Now do something that might have impact to foreground thread,
            // so run it a bit later to let startup process finished.
            try {
                Thread.sleep(DELAYED_INITIALIZER_DELAY_TIME);
            } catch (InterruptedException ie) {
                // ignore.
            }
            afm.loadAccountList(null);
            ensureSimContactCache();
            checkPeriodUsageReporter();
        }
        private void ensureSimContactCache() {
            if (!SimUtil.IS_YUNOS) {
                return;
            }
            if (SimUtil.MULTISIM_ENABLE) {
                checkToLoadSimContact(SimUtil.SLOT_ID_1);
                checkToLoadSimContact(SimUtil.SLOT_ID_2);
            } else {
                checkToLoadSimContact(SimUtil.SLOT_ID_1);
            }
        }
        private void checkToLoadSimContact(int slotId) {
            Context context = getApplicationContext();
            // If the app is launched by sim status change broadcast,
            // then the receiver will also start the service.
            // The below code is executed after a small delay,
            // so the receiver might already start the service.
            // Here we only start service when the count value is NOT_LOADED_COUNT.
            boolean neverLoaded = SimContactLoadService.getSimLoadedCount(slotId) == SimContactLoadService.NOT_LOADED_COUNT;
            boolean active = SimUtil.MULTISIM_ENABLE ? SimUtil.isSimAvailable(slotId) : SimUtil.isSimAvailable();
            if (neverLoaded && active) {
                SimContactLoadService.setSimScheduleLoading(slotId);
                Intent intent = new Intent(SimContactLoadService.ACTION_LOAD_SIM_CONTACTS_TO_PHONE_DB);
                intent.setClass(context, SimContactLoadService.class);
                intent.putExtra(SimContactLoadService.INTENT_KEY_SLOT_ID, slotId);
                QueuedWork.waitToFinish();
                context.startService(intent);
            } else if (!active) {
                Intent intent = new Intent(SimContactLoadService.ACTION_DELETE_SIM_CONTACTS_FROM_PHONE_DB);
                intent.setClass(context, SimContactLoadService.class);
                intent.putExtra(SimContactLoadService.INTENT_KEY_SLOT_ID, slotId);
                QueuedWork.waitToFinish();
                context.startService(intent);
            }
        }

        private void checkPeriodUsageReporter() {
            long now = System.currentTimeMillis();
            Log.i(TAG, "checkPeriodUsageReporter: now="+now);
            checkCustomEventReport(UsageReporter.AccountsSupport.EVENT_ID_LOCAL_ACCOUNT_CHECKED,
                    UsageReporter.AccountsSupport.REPORT_INTERVAL, now);
            checkCustomEventReport(UsageReporter.AccountsSupport.EVENT_ID_SIM_ACCOUNT_CHECKED,
                    UsageReporter.AccountsSupport.REPORT_INTERVAL, now);
            checkCustomEventReport(UsageReporter.AccountsSupport.EVENT_ID_OTHER_ACCOUNTS_CHECKED,
                    UsageReporter.AccountsSupport.REPORT_INTERVAL, now);
        }
        private void checkCustomEventReport(String name, long interval, long now) {
            SharedPreferences reportTime = getSharedPreferences(LAST_REPORT_TIME_PREFS_NAME, Context.MODE_PRIVATE);
            long lastReportTime = reportTime.getLong(name, -1);
            if ((now - lastReportTime) < interval) {
                Log.i(TAG, "checkCustomEventReport: interval not reached. quit.");
                return;
            }
            HashMap<String, String> report = UsageReporter.deserializeForPeriodReport(ContactsApplication.this, name);
            if ((report != null) && (report.size() > 0)) {
                UsageReporter.commitEvent(name, report);
                SharedPreferences.Editor editor = reportTime.edit();
                editor.putLong(name, now);
                // Avoid to use Editor.apply() in background thread.
                // https://k3.alibaba-inc.com/issue/8144090?versionId=1262816
                editor.commit();
            }
        }

    }

    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you
        // may tune some of them,
        // or you can create default configuration by
        // ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2).denyCacheImageMultipleSizesInMemory()
                .discCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO);
        if (IS_LOW_MEMORY_DEVICE) {
            // By default, the ImageLoader uses 1/8 of available memory for
            // cache.
            // In low ram device, we want to use less memory for cache.
            Log.i(TAG, "initImageLoader: low ram device, use "
                    + LOW_RAM_IMAGE_MEMORY_CACHE_SIZE_PERCENTAGE
                    + "% available memory for image cache.");
            builder.memoryCacheSizePercentage(LOW_RAM_IMAGE_MEMORY_CACHE_SIZE_PERCENTAGE);
        }
        try {
            ImageLoaderConfiguration config = builder.build();
            // Initialize ImageLoader with configuration.
            ImageLoader.getInstance().init(config);
        } catch (IllegalArgumentException iae) {
            // The ImageLoader will call Context.getCacheDir() and getCacheDir() will try to
            // create a directory on file system. On fail, getCacheDir() will return fail,
            // and cause IllegalArgumentException in ImageLoader.
            // We can ignore the failure on ImageLoader init in this case.
            // Refer to bug https://k3.alibaba-inc.com/issue/7747746?versionId=1049281
            // for detail info about why catch the exception here.
            Log.e(TAG, "initImageLoader: failed to init ImageLoader.", iae);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        clearImageMemoryCache();
    }

    private void clearImageMemoryCache() {
        ImageLoader imageLoader = ImageLoader.getInstance();
        if (imageLoader.isInited()) {
            Log.i(TAG, "clearImageMemoryCache: call imageLoader.clearMemoryCache().");
            imageLoader.clearMemoryCache();
        }
    }

    /**
     * For appwidget
     */
    private static SparseIntArray sDynColorsArray = null;

    private static synchronized SparseIntArray getDynColorsArray() {
        return sDynColorsArray;
    }

    private static synchronized void setDynColorsArray(SparseIntArray dynColors) {
        sDynColorsArray = dynColors;
    }

    public static void overlayDyncColorResIfSupport(RemoteViews remoteViews) {
        if (DynColorSetting.isSupportDynColor()) {
            overlayDyncColorRes(remoteViews);
        }
    }

    private static void overlayDyncColorRes(RemoteViews remoteViews) {
        ReflectHelper.RemoteViews.setDynColorArray(remoteViews, getDynColorsArray());
        ReflectHelper.RemoteViews.setThemeID(remoteViews, R.style.PeopleNoActionBarTheme);
    }
}
