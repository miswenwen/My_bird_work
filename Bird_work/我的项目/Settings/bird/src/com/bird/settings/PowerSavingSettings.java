package com.bird.settings;


import android.provider.Settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.CheckBoxPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.settings.FeatureOption;

import java.util.ArrayList;
import java.util.List;
import com.android.settings.search.Indexable;
import android.os.Bundle;
import android.content.res.Resources;
import android.os.UserHandle;
import android.content.IntentFilter;
//bird
import com.bird.settings.ProximityPreference;
import com.bird.settings.BirdFeatureOption;
import com.android.settings.widget.SwitchBar;
import android.widget.Switch;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.telephony.TelephonyManager;
import android.os.PowerManager;
import android.bluetooth.BluetoothAdapter;

import android.database.ContentObserver;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.Intent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import java.lang.Runnable;
import android.app.Service;
import android.os.IBinder;
public class PowerSavingSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
          private PowerSavingEnabler mPowerSavingEnabler;
          private CheckBoxPreference mCloseWifi;
          private CheckBoxPreference mCloseAutoBrightness;
          private CheckBoxPreference mCloseBluetooth;
          private CheckBoxPreference mCloseDataConn;
          private CheckBoxPreference mCloseGPS;
          private CheckBoxPreference mOpenBatterySaver;
          private ArrayList<Preference> mPreferenceList;

          private static final String TAG = "PowerSavingSettings";

          private static final String WIFI_CHECKBOX_KEY = "wifi_checkbox";
          private static final String AUTOBRIGHTNESS_CHECKBOX_KEY = "autobrightness_checkbox";
          private static final String BLUETOOTH_CHECKBOX_KEY = "bluetooth_checkbox";
          private static final String DATA_CONN_CHECKBOX_KEY = "data_connection_checkbox";
          private static final String GPS_CHECKBOX_KEY = "gps_checkbox";
          private static final String BATTERY_SAVER_CHECKBOX_KEY = "battery_saver_checkbox";

          private static final boolean MTK_BG_POWER_SAVING_SUPPORT = SystemProperties.get("ro.mtk_bg_power_saving_support").equals("1");
        	private static final boolean MTK_BG_POWER_SAVING_UI_SUPPORT = SystemProperties.get("ro.mtk_bg_power_saving_ui").equals("1");

          private WifiManager mWifiManager;
          private BluetoothAdapter mBluetoothAdapter;
          private TelephonyManager mTelephonyManager;
          private PowerManager mPowerManager;
          private Context mContext;

          private static final int MSG_POWER_SAVING_CHANGE = 1;
          @Override
          public void onStart() {
              super.onStart();

              // On/off switch is hidden for Setup Wizard (returns null)
              mPowerSavingEnabler = createEnabler();
          }
          PowerSavingEnabler createEnabler() {
              final SettingsActivity activity = (SettingsActivity) getActivity();
              return new PowerSavingEnabler(activity, activity.getSwitchBar());
          }
          @Override
        protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return 1;
        }
          @Override
          public void onCreate(Bundle savedInstanceState) {
              super.onCreate(savedInstanceState);
              addPreferencesFromResource(R.xml.powersaving_settings);

              mPreferenceList = new ArrayList<Preference>();
              mCloseWifi = (CheckBoxPreference) findPreference(WIFI_CHECKBOX_KEY);
              mCloseAutoBrightness = (CheckBoxPreference) findPreference(AUTOBRIGHTNESS_CHECKBOX_KEY);
              mCloseBluetooth = (CheckBoxPreference) findPreference(BLUETOOTH_CHECKBOX_KEY);
              mCloseDataConn = (CheckBoxPreference) findPreference(DATA_CONN_CHECKBOX_KEY);
              mCloseGPS = (CheckBoxPreference) findPreference(GPS_CHECKBOX_KEY);
              mOpenBatterySaver = (CheckBoxPreference) findPreference(BATTERY_SAVER_CHECKBOX_KEY);

              mPreferenceList.add(mCloseWifi);
              mPreferenceList.add(mCloseAutoBrightness);
              mPreferenceList.add(mCloseBluetooth);
              mPreferenceList.add(mCloseDataConn);
              mPreferenceList.add(mCloseGPS);
              mPreferenceList.add(mOpenBatterySaver);
              setupPreference();

              mContext = getActivity();
              mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
              mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
              mPowerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
              mTelephonyManager = TelephonyManager.from(mContext);
          }
          @Override
          public void onResume() {
              super.onResume();
              getActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.BIRD_POWER_SAVING_MODE),
                      false, mPowerSavingObserver);
          }
          @Override
          public void onPause(){
              super.onPause();
              getActivity().getContentResolver().unregisterContentObserver(mPowerSavingObserver);
          }
          @Override
          public void onDestroyView() {
              super.onDestroyView();
              if (mPowerSavingEnabler != null) {
                  mPowerSavingEnabler.teardownSwitchBar();
              }
              mPreferenceList.clear();
          }
          private ContentObserver mPowerSavingObserver = new ContentObserver(new Handler()) {
              @Override
              public void onChange(boolean selfChange) {
                  Log.d(TAG,"onChange____");
                  mPowerSavingEnabler.setSwitchBarChecked(readPowerSavingMode(mContext));
                  mContext.startService(new Intent(mContext, PowerSavingSettings.PowerSavingService.class));
              }
          };

          private void setupPreference(){
              for(Preference preference: mPreferenceList){
                  preference.setOnPreferenceChangeListener(this);
              }
          }

          private void enableAll(boolean isEnable){
              for(Preference preference: mPreferenceList){
                  preference.setEnabled(isEnable);
              }
          }
          @Override
          public boolean onPreferenceChange(Preference preference, Object objValue) {
              final String key = preference.getKey();
              final Boolean value = (Boolean)objValue;
              if (WIFI_CHECKBOX_KEY.equals(key)) {
                  if(mPowerSavingEnabler.isSwitchBarChecked() && value){
                	  if (mWifiManager != null ) {
						if (mWifiManager.isWifiEnabled()) {
							mWifiManager.setWifiEnabled(false);
						}
					}
                  }
                  return true;
              }else if (AUTOBRIGHTNESS_CHECKBOX_KEY.equals(key)) {
                  if(mPowerSavingEnabler.isSwitchBarChecked() && value){
                      Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                      int min = mPowerManager.getMinimumScreenBrightnessSetting();
                      Settings.System.putIntForUser(mContext.getContentResolver(),
                          Settings.System.SCREEN_BRIGHTNESS, min,UserHandle.USER_CURRENT);
                  }
                  return true;
              }else if (BLUETOOTH_CHECKBOX_KEY.equals(key)) {
                  if(mPowerSavingEnabler.isSwitchBarChecked() && value){
                      if(mBluetoothAdapter != null)
                          mBluetoothAdapter.disable();
                  }
                  return true;
              }else if (DATA_CONN_CHECKBOX_KEY.equals(key)) {
                  if(mPowerSavingEnabler.isSwitchBarChecked() && value){
                      mTelephonyManager.setDataEnabled(false);
                  }
                  return true;
              }else if (GPS_CHECKBOX_KEY.equals(key)) {
                  if(mPowerSavingEnabler.isSwitchBarChecked() && value){
                      Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),LocationManager.GPS_PROVIDER, false);
                      Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
                  }
                  return true;
              }else if (BATTERY_SAVER_CHECKBOX_KEY.equals(key)) {
                  if(mPowerSavingEnabler.isSwitchBarChecked()){
                      if(MTK_BG_POWER_SAVING_SUPPORT && MTK_BG_POWER_SAVING_UI_SUPPORT)
                          Settings.System.putInt(mContext.getContentResolver(),
                              Settings.System.BG_POWER_SAVING_ENABLE, value ? 1 : 0);
                      if(mPowerManager != null)
                          mPowerManager.setPowerSaveMode(value);
                  }
                  return true;
              }
              return false;
          }

          public static void writePowerSavingMode(Context context,boolean isOpen){
            int newMode = isOpen ? Settings.System.BIRD_POWER_SAVING_ON : Settings.System.BIRD_POWER_SAVING_OFF;
            Settings.System.putInt(context.getContentResolver(),Settings.System.BIRD_POWER_SAVING_MODE, newMode);
          }
          public static void writePowerSavingMode(Context context,int newMode){
            if(newMode != Settings.System.BIRD_POWER_SAVING_ON && newMode != Settings.System.BIRD_POWER_SAVING_OFF)
                return;
            Settings.System.putInt(context.getContentResolver(),Settings.System.BIRD_POWER_SAVING_MODE, newMode);
          }
          public static boolean readPowerSavingMode(Context context){
              int powerSavingMode = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.BIRD_POWER_SAVING_MODE, Settings.System.BIRD_POWER_SAVING_OFF);
              return powerSavingMode == Settings.System.BIRD_POWER_SAVING_ON;
          }
          public static class PowerSavingService extends Service {
        	  private static WifiManager mWifiManager;
              private static BluetoothAdapter mBluetoothAdapter;
              private static TelephonyManager mTelephonyManager;
              private static PowerManager mPowerManager;
              private Handler mHandler = new Handler();
              private static final String ORIGIN_BRIGHTNESS = "origin_brightness";
              private Runnable mModeChangeRunnable = new Runnable(){
                  @Override
                  public void run() {
                      boolean isOpen = readPowerSavingMode(getApplicationContext());
                      doPowerSaving(getApplicationContext(),isOpen);
                  }
              };
              @Override
              public void onCreate() {
                  super.onCreate();
                  if (mWifiManager == null) {
  					mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
  				}
              }

              @Override
              public int onStartCommand(Intent intent, int flags, int startId) {
                  Log.d(TAG,"onStartCommand____");
                  mHandler.post(mModeChangeRunnable);
                  return super.onStartCommand(intent, flags, startId);
              }

              @Override
              public void onDestroy() {
                  super.onDestroy();
              }

              @Override
              public IBinder onBind(Intent intent) {
                  return null;
              }

              public static void doPowerSaving(Context context,boolean isOpen){
            	  
                  if(mBluetoothAdapter == null)
                      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                  if(mPowerManager == null)
                      mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                  if(mTelephonyManager == null)
                      mTelephonyManager = TelephonyManager.from(context);

                  SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                  boolean closeWifi = sharedPreferences.getBoolean(WIFI_CHECKBOX_KEY,true);
                  boolean closeAutoBrightness = sharedPreferences.getBoolean(AUTOBRIGHTNESS_CHECKBOX_KEY,true);
                  boolean closeBluetooth = sharedPreferences.getBoolean(BLUETOOTH_CHECKBOX_KEY,true);
                  boolean closeDataConn = sharedPreferences.getBoolean(DATA_CONN_CHECKBOX_KEY,true);
                  boolean closeGps = sharedPreferences.getBoolean(GPS_CHECKBOX_KEY,true);
                  boolean openBatterySaver = sharedPreferences.getBoolean(BATTERY_SAVER_CHECKBOX_KEY,true);

                  if(isOpen){
                	  //wifi
                	  if (mWifiManager != null && closeWifi) {
                		  if (mWifiManager.isWifiEnabled()) {
                			  mWifiManager.setWifiEnabled(false);
						}
					}
                      //bluetooth
                      if(mBluetoothAdapter != null && closeBluetooth)
                          mBluetoothAdapter.disable();
                      //gps
                      if(closeGps){
                          Settings.Secure.setLocationProviderEnabled(context.getContentResolver(),LocationManager.GPS_PROVIDER, false);
                          Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
                      }
                      //data
                      if(mTelephonyManager != null && closeDataConn)
                          mTelephonyManager.setDataEnabled(false);
                      //autobrightness
                      if(closeAutoBrightness){
                    	  int min = mPowerManager.getMinimumScreenBrightnessSetting();
                    	  
                    	  int originBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, min);
                    	  if (originBrightness == min) {
  							return;
  						}
                    	  Settings.System.putInt(context.getContentResolver(), ORIGIN_BRIGHTNESS, originBrightness);
                          Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                          Settings.System.putIntForUser(context.getContentResolver(),
                              Settings.System.SCREEN_BRIGHTNESS, min,UserHandle.USER_CURRENT);
                      }
                      //save power mode
                      if(openBatterySaver){
                          if(MTK_BG_POWER_SAVING_SUPPORT && MTK_BG_POWER_SAVING_UI_SUPPORT)
                              Settings.System.putInt(context.getContentResolver(),
                                  Settings.System.BG_POWER_SAVING_ENABLE, 1);
                          if(mPowerManager != null)
                              mPowerManager.setPowerSaveMode(true);
                      }
                  }else{
                      if(MTK_BG_POWER_SAVING_SUPPORT && MTK_BG_POWER_SAVING_UI_SUPPORT)
                          Settings.System.putInt(context.getContentResolver(),
                              Settings.System.BG_POWER_SAVING_ENABLE, 0);
                      if(mPowerManager != null)
                          mPowerManager.setPowerSaveMode(false);
                      int min = mPowerManager.getMinimumScreenBrightnessSetting();
                	  int originBrightness = Settings.System.getInt(context.getContentResolver(), ORIGIN_BRIGHTNESS, min);
                	  Settings.System.putIntForUser(context.getContentResolver(),
                              Settings.System.SCREEN_BRIGHTNESS, originBrightness,UserHandle.USER_CURRENT);
                  }
              }
          }

          class PowerSavingEnabler implements SwitchBar.OnSwitchChangeListener  {
              private static final String TAG = "PowerSavingEnabler";
              private Context mContext;
              private SwitchBar mSwitchBar;
              private boolean mLastCheck = false;
              private boolean mListeningToOnSwitchChange = false;

              public PowerSavingEnabler(Context context, SwitchBar switchBar) {
                  mContext = context;
                  mSwitchBar = switchBar;
                  setupSwitchBar();
              }

              public void setupSwitchBar() {
                  if (!mListeningToOnSwitchChange) {
                      mSwitchBar.addOnSwitchChangeListener(this);
                      mListeningToOnSwitchChange = true;
                  }
                  setSwitchBarChecked(readPowerSavingMode(mContext));
                  mSwitchBar.show();
              }

              public void teardownSwitchBar() {
                  if (mListeningToOnSwitchChange) {
                      mSwitchBar.removeOnSwitchChangeListener(this);
                      mListeningToOnSwitchChange = false;
                  }
                  mSwitchBar.hide();
              }

              @Override
              public void onSwitchChanged(Switch switchView, boolean isChecked) {
                  writePowerSavingMode(mContext,isChecked);
                  enableAll(isChecked);
              }
              public void setSwitchBarChecked(boolean checked) {
                  mSwitchBar.setChecked(checked);
                  enableAll(checked);
              }
              public boolean isSwitchBarChecked(){
                  return mSwitchBar.isChecked();
              }
        }
}
