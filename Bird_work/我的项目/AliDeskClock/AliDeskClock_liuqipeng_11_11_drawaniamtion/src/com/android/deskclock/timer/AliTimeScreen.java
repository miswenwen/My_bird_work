
package com.android.deskclock.timer;

import aliyun.aml.ScreenContext;
import aliyun.aml.ScreenElementRoot.OnExternCommandListener;
import aliyun.aml.elements.ScreenElement;
import aliyun.aml.util.Utils;
import aliyun.aml.util.ZipResourceLoader;
import aliyun.content.res.ThemeResources;
import aliyun.v3.widget.FancyElementRoot;
import aliyun.v3.widget.FancyElementRootFactory;
import aliyun.v3.widget.FancyScaleView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.SystemProperties;
import android.graphics.drawable.Drawable;
import aliyun.v3.lockscreen.CoverDrawable;

import org.w3c.dom.Element;

import com.android.deskclock.AlarmFeatureOption;
import com.android.deskclock.Log;
import com.android.deskclock.alarms.AlarmActivity;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import static aliyun.content.res.ThemeConstants.CONFIG_THEME_CUSTOM_PATH;
import static aliyun.content.res.ThemeConstants.CONFIG_THEME_DEFAULT_PATH;

public class AliTimeScreen extends FrameLayout {

    // AlarmActivity listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    private static final int DEFAULT_SNOOZE_MINUTES = 10;
    // AlarmActivity listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
    private  static final String POWER_OFF_ACTION = "android.intent.action.ACTION_SHUTDOWN";
    public static final String TIME_DISMISS_ACTION = "com.android.deskclock.TIME_DISMISS";
    public static final String TIME_FINISH_ACTION = "com.android.deskclock.TIME_FINISH";
    private AlarmElementRoot mRoot;
    private FancyScaleView mAlarmView;
    private Context mContext;
    //private String mSnoozeText;
    private static final String COMMAND_PAUSE = "pause";
    private static final String COMMAND_RESUME = "resume";
    private static final String CONFIG_FILE_NAME = "timestyle";
    private static final int MSG_DISMISS = 1;
    private static final int MSG_SNOOZE = 2;
    private static final int MSG_POWER_OFF = 3;

    public static final String PROP_POWERON_ALERT = "persist.env.deskclock.poalert";
    private static final String FIRST_ALARM_FLAG = "first_alarm";
    private ZipFile mZipFile;
    public static final int BACKGROUND_COLOR = 0x33000000;

    public AliTimeScreen(Context context) {
        super(context);
        init();
    }

    public AliTimeScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AliTimeScreen(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        Context context = getContext();
        mContext = context;
        mRoot = new AlarmElementRoot(FancyElementRootFactory.createContext(
                new FancyElementRootFactory.Parameter(context, getResourceLoader(CONFIG_FILE_NAME)).setHandler(mHandler)));
        mRoot.load();
        mAlarmView = new FancyScaleView(context, mRoot);
        if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
            if(isPowerOffAlarm(context)){
                Utils.putVariableNumber("power_off_alarm", mRoot.getVariables(), Double.valueOf(1));
            }
        }
        addView(mAlarmView);
        setupWallpaper();
        /*YUNOS BEGIN PB*/
        // ##Module:(AliDeskClock) ##author:xy83652@alibaba-inc.com
        // ##BugID:(7869088) ##Date:2016/03/01
        // ##Description:make AliTimeScreen Full screen
        Point size = new Point();
        if (context instanceof Activity) {
            ((Activity) context).getWindowManager().getDefaultDisplay().getRealSize(size);
            mRoot.setScreenSize(size.x, size.y);
        }
        /* YUNOS END PB */
    }

    protected void setupWallpaper() {
        Drawable wallpaper = null;
        wallpaper = ThemeResources.getLockWallpaperCache(getContext(), BACKGROUND_COLOR);
        if (wallpaper == null){
            wallpaper = new CoverDrawable(BACKGROUND_COLOR);
        }
        setBackground(wallpaper);
    }

    public static boolean isPowerOffAlarm(Context context) {
        boolean isPoAlarm = false;
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        isPoAlarm = prefs.getBoolean(FIRST_ALARM_FLAG, false);
        if (isPoAlarm == true) {
            saveFirstAlarm(prefs, false);
        }
        return isPoAlarm;
    }

    /**
     * Save flag for first Alarm
     */
    public static void saveFirstAlarm(SharedPreferences prefs,Boolean isFirstAlarm) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FIRST_ALARM_FLAG,isFirstAlarm);
        editor.apply();
    }

    //TODO penglei : this may be error.
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i("the message arg 1 :" + msg.arg1 + ", arg2 :" + msg.arg2);
            onPause();
            Intent alarmIntent = new Intent();
            switch (msg.what) {
                case MSG_DISMISS:
                    //mAlarmView.getViewRootImpl().cancelInvalidate(mAlarmView);
                    // Toast.makeText(getContext(), "dismiss", Toast.LENGTH_SHORT).show();
                    alarmIntent.setAction(TIME_DISMISS_ACTION);
                    break;
                case MSG_SNOOZE:
                    int interval = msg.arg1;
                    interval = interval/60000;
                    if (interval == 0) {
                        interval = DEFAULT_SNOOZE_MINUTES;
                    }
                    //mAlarmView.getViewRootImpl().cancelInvalidate(mAlarmView);
                    // Toast.makeText(getContext(), "snooze : " + interval, Toast.LENGTH_SHORT).show();
                    alarmIntent.setAction(ALARM_SNOOZE_ACTION);
                    alarmIntent.putExtra("snoozeTime", interval);
                    break;
                case MSG_POWER_OFF:
                    if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM) {
                        alarmIntent.setAction(ALARM_DISMISS_ACTION);
                        alarmIntent.putExtra("isShutDown", true);
                        Log.i("power off action is send");
                    }
                    break;
                default:
                    break;
            }
            ((Activity)getContext()).sendBroadcast(alarmIntent);
        }
    };

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cleanUp();
    }

    private static class AlarmElementRoot extends FancyElementRoot implements OnExternCommandListener {

        public AlarmElementRoot(ScreenContext c) {
            super(c);
            setOnExternCommandListener(this);
        }

        public void onUIInteractive(ScreenElement ele, String action) {
            String name = ele.getName();
            if (action.equals("up")) {
                if ("snooze_alarm".equals(name))
                    getContext().getHandler().obtainMessage(MSG_SNOOZE).sendToTarget();
                else if ("dismiss_alarm".equals(name) || "power_on".equals(name))
                    getContext().getHandler().obtainMessage(MSG_DISMISS).sendToTarget();
                else if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM && "power_off".equals(name))
                    getContext().getHandler().obtainMessage(MSG_POWER_OFF).sendToTarget();
            } else if (action.equals("launch")) {
                if ("snooze_endpoint".equals(name)) {
                    getContext().getHandler().obtainMessage(MSG_SNOOZE).sendToTarget();
                } else if ("dismiss_endpoint".equals(name)) {
                    getContext().getHandler().obtainMessage(MSG_DISMISS).sendToTarget();
                } else if (AlarmFeatureOption.YUNOS_QCOM_PLATFORM && "poweroff_endpoint".equals(name)) {
                    getContext().getHandler().obtainMessage(MSG_POWER_OFF).sendToTarget();
                }
            }
        }

        public void onCommand(String command, Double para1, String para2) {
            if ("snooze_alarm-test".equals(command)) {
                Message msg = getContext().getHandler().obtainMessage(MSG_DISMISS);
                if (para1 != null)
                    msg.arg1 = para1.intValue();
                    msg.sendToTarget();
            } 
        }
    }

    public ZipResourceLoader getResourceLoader(String filename) {
        mZipFile = ThemeResources.getValidZipFile(CONFIG_THEME_DEFAULT_PATH + filename,
                CONFIG_THEME_CUSTOM_PATH + filename, ZipResourceLoader.MANIFEST_FILE_NAME);
        return new ZipResourceLoader(mZipFile, null, null);
    }

    public void cleanUp() {
        mHandler.removeCallbacksAndMessages(null);
        mAlarmView.cleanUp(false);
        if (mZipFile != null) {
            try {
                mZipFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mZipFile = null;
        }
    }

    public void onPause() {
        mAlarmView.onPause();
        mRoot.onCommand(COMMAND_PAUSE);
    }

    public void onResume() {
        mAlarmView.onResume();
        mRoot.onCommand(COMMAND_RESUME);
        //aliyun.aml.util.Utils.putVariableString("snooze_message", mScreenContext.mVariables,
        //        mSnoozeText);
        //String label = ((AlarmActivity) mContext).getAlarmInstance().mLabel;
        //Utils.putVariableString("alarm_label", mScreenContext.mVariables, label);
    }
}
