package com.yunos.alicontacts.plugins;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources.NotFoundException;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PluginManager {

    private static final String TAG = "PluginManager";

    public static final String PLUGIN_PACKAGE_NAME_QRCODE =
            "com.yunos.alicontacts.plugin.glassky.main";

    public static final String PLUGIN_QUERY_INTENT_ACTION = "com.yunos.alicontacts.plugin.action.query_plugin";
    public static final String PLUGIN_PACKAGE_NAME_PREFIX = "com.yunos.alicontacts.plugin";

    private static PluginManager _instance = null;

    public static PluginManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new PluginManager(context.getApplicationContext());
        }
        return _instance;
    }

    private Context mContext;
    private PluginHandler mHandler;
    private List<PluginBean> mCurrentPlugins;

    private PluginManager(Context context) {
        mContext = context;
        mHandler = new PluginHandler();
        mCurrentPlugins = new ArrayList<PluginBean>();
    }

    public synchronized void initPlugins() {

        findPlugins();
        if (mCurrentPlugins.isEmpty())
            return;

        for (PluginBean plugin : mCurrentPlugins) {
            try {
                String name = plugin.getPackageName() + ".PluginApplication";
                Log.i(TAG, "initPlugins " + name);
                final Context context = mContext.createPackageContext(
                        plugin.getPackageName(), Context.CONTEXT_INCLUDE_CODE
                                | Context.CONTEXT_IGNORE_SECURITY);
                if (context == null) {
                    Log.e(TAG, "context == null failed ");
                    return;
                }
                Class<?> demo = Class.forName(name, false,
                        context.getClassLoader());

                Method method = demo.getMethod("init", Context.class,
                        String.class);
                method.invoke(demo.newInstance(), mContext, mHandler.getClass()
                        .getName());

                String detailAction = plugin.getPackageName()
                        + PluginHandler.DETAIL_PLUGINS_ACTION;
                plugin.setDetailAction(detailAction);

                String settingAction = plugin.getPackageName()
                        + PluginHandler.SETTING_PLIUGINS_ACTION;
                plugin.setSettingAction(settingAction);

                method = demo.getMethod("isFullScreen");
                Object result = method.invoke(demo.newInstance());
                if (result != null) {
                    plugin.setFullScreen((Boolean) result);
                }

                method = demo.getMethod("isLayoutPlugin");
                result = method.invoke(demo.newInstance());
                if (result != null && (Boolean) result) {
                    plugin.setLayout((Boolean) result);
                }
            } catch (Exception e) {
                Log.e(TAG, "initPlugins() IOC failed ", e);
                //e.printStackTrace();
            }

        }

        Log.i(TAG, "initPlugins IOC success");
    }

    public boolean isActiveLayoutPlugin(PluginBean plugin) {
        if (!mCurrentPlugins.contains(plugin)) {
            Log.e(TAG,
                    "isActiveLayoutPlugin no this plugin "
                            + plugin.getPackageName());
            return false;
        }

        try {
            Context context = mContext.createPackageContext(
                    plugin.getPackageName(), Context.CONTEXT_IGNORE_SECURITY
                            | Context.CONTEXT_INCLUDE_CODE);
            SharedPreferences prefs = context
                    .getSharedPreferences(PluginHandler.PLUGIN_PREFS_NAME,
                            Context.MODE_MULTI_PROCESS);
            if (prefs.contains(PluginHandler.PLUGIN_TOOGLE_KEY)) {
                Boolean ret = prefs.getBoolean(PluginHandler.PLUGIN_TOOGLE_KEY,
                        true);
                Log.i(TAG, "isActiveLayoutPlugin get ret " + ret);
                return ret;
            } else {
                Log.i(TAG, "isActiveLayoutPlugin no sp");
                return true;
            }

        } catch (NameNotFoundException e) {
            //e.printStackTrace();
            Log.e(TAG, "isActiveLayoutPlugin throw NameNotFoundException ", e);
        }

        return true;

//      try {
//          String name = plugin.getPackageName() + ".PluginApplication";
//          final Context context = mContext.createPackageContext(
//                  plugin.getPackageName(), Context.CONTEXT_INCLUDE_CODE
//                          | Context.CONTEXT_IGNORE_SECURITY);
//          if (context == null) {
//              Log.e(TAG, "context == null failed ");
//              return false;
//          }
//          Class<?> demo = Class
//                  .forName(name, false, context.getClassLoader());
//
//          Method method = demo.getMethod("isLayoutPlugin");
//          Object result = method.invoke(demo.newInstance());
//          if (result == null || !(Boolean) result) {
//              Log.e(TAG, "isActiveLayoutPlugin IOC, no layout");
//              return false;
//          }
//
//          method = demo.getMethod("isActive", Context.class);
//          result = method.invoke(demo.newInstance(), context);
//          if (result != null) {
//              Log.i(TAG, "isActiveLayoutPlugin IOC return "
//                      + (Boolean) result);
//              return (Boolean) result;
//          }
//      }
//
//      catch (Exception e) {
//          Log.e(TAG, "isActiveLayoutPlugin IOC failed " + e.getMessage());
//          e.printStackTrace();
//          return false;
//      }
//
//      Log.e(TAG, "isActiveLayoutPlugin IOC return false");
//      return false;
    }

    public String getPluginLabel(PluginBean plugin) {
        try {
            if (!mCurrentPlugins.contains(plugin)) {
                Log.e(TAG,
                        "getPluginLabel no this plugin "
                                + plugin.getPackageName());
                return "";
            }

            final Context context = mContext.createPackageContext(
                    plugin.getPackageName(), Context.CONTEXT_INCLUDE_CODE
                            | Context.CONTEXT_IGNORE_SECURITY);
            if (context == null) {
                Log.e(TAG, "context == null failed ");
                return "";
            }

            // get plugin class R
            Class demo = context.getClassLoader().loadClass(
                    plugin.getPackageName() + ".R");
            String label = context.getResources().getString(
                    getResourseIdByName(demo, "string", "app_name"));
            return label;

        } catch (NameNotFoundException e) {
            //e.printStackTrace();
            Log.e(TAG, "getPluginLabel() throw NameNotFoundException", e);
        } catch (NotFoundException e) {
            //e.printStackTrace();
            Log.e(TAG, "getPluginLabel() throw NotFoundException", e);
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
            Log.e(TAG, "getPluginLabel() throw ClassNotFoundException", e);
        }

        return "";
    }

    public List<PluginBean> getLayoutPlugins() {
        List<PluginBean> plugins = new ArrayList<PluginBean>();

        for (int i = 0; i < mCurrentPlugins.size(); i++) {
            if (mCurrentPlugins.get(i).isLayout())
                plugins.add(mCurrentPlugins.get(i));
        }

        return plugins;
    }

    public static boolean findPlugin(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pkgs = pm
                .getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo pkg : pkgs) {
            if (packageName.equals(pkg.packageName))
                return true;
        }
        return false;
    }

    private void findPlugins() {
        mCurrentPlugins.clear();

        try {
            PackageManager pm = mContext.getPackageManager();
            Intent queryIntent = new Intent(PLUGIN_QUERY_INTENT_ACTION);
            List<ResolveInfo> activities = pm.queryIntentActivities(queryIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo ri : activities) {
                ActivityInfo ai = ri.activityInfo;
                String pkgName = ai == null ? null : ai.packageName;
                if (TextUtils.isEmpty(pkgName) || (!pkgName.startsWith(PLUGIN_PACKAGE_NAME_PREFIX))) {
                    continue;
                }
                PluginBean plug = new PluginBean();
                plug.setPackageName(pkgName);
                mCurrentPlugins.add(plug);
            }
        } catch (Exception e) {
            // RemoteException is not declared by PackageManager.getInstalledPackages(),
            // but actually, it will throw such exception, e.g. bug 5423327.
            // To avoid crash when too many apps are installed,
            // we down grade the plugin features here.
            Log.e(TAG, "Got exception in querying plugins.", e);
        }
        return;
    }

    private int getResourseIdByName(Class clazz, String className, String name) {
        int id = 0;
        try {
            // get R.java static internal class
            Class[] classes = clazz.getClasses();
            Class desireClass = null;
            for (int i = 0; i < classes.length; i++) {
                // find class
                if (classes[i].getName().split("\\$")[1].equals(className)) {
                    desireClass = classes[i];
                    break;
                }
            }
            if (desireClass != null) {
                // get resource id
                id = desireClass.getField(name).getInt(desireClass);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getResourseIdByName() throw IllegalArgumentException", e);
            //e.printStackTrace();
        } catch (SecurityException e) {
            //e.printStackTrace();
            Log.e(TAG, "getResourseIdByName() throw SecurityException", e);
        } catch (IllegalAccessException e) {
            //e.printStackTrace();
            Log.e(TAG, "getResourseIdByName() throw IllegalAccessException", e);
        } catch (NoSuchFieldException e) {
            //e.printStackTrace();
            Log.e(TAG, "getResourseIdByName() throw NoSuchFieldException", e);
        }

        return id;
    }

}
