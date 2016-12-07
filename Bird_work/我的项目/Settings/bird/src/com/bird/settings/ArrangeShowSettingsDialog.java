/*
 * Copyright (C) 2014 The Android Open Source Project
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

 package com.bird.settings;


 import android.provider.Settings;

 import android.app.Activity;
 import android.app.Dialog;
 import android.content.Context;
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
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.app.DialogFragment;
 import android.content.DialogInterface;
 import android.view.View;
public class ArrangeShowSettingsDialog {
      public static final String SETTINGS_ARRANGE_TYPE = "settings_arrange_type";
      public static final int SETTINGS_ARRANGE_TYPE_LIST = 0;
      public static final int SETTINGS_ARRANGE_TYPE_GRID = 1;
      private static final int SETTINGS_ARRANGE_TYPE_NUM = 2;

      public static void showDialog(final Context context){
          String[] sArrangeType = new String []{context.getString(R.string.arrange_show_list),
                                           context.getString(R.string.arrange_show_grid)};
          assert(sArrangeType.length == SETTINGS_ARRANGE_TYPE_NUM); // jiali modify, notice: DoNot put sArrangeType to private static final. http://112.124.14.158/bug-view-14334.html
          final int currentValue = getCurrentArrangeType(context);

          AlertDialog alertDialog = new AlertDialog.Builder(context).
          setTitle(context.getString(R.string.arrange_show_settings)).
          setSingleChoiceItems(sArrangeType, currentValue, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                    android.util.Log.d("qinzhifeng","onClick_____which___"+which);
                    if(which < SETTINGS_ARRANGE_TYPE_NUM && which != currentValue)
                        Settings.System.putInt(context.getContentResolver(), SETTINGS_ARRANGE_TYPE, which);
                    dialog.dismiss();
              }
          }).
          setNegativeButton(R.string.dlg_cancel, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                  // TODO Auto-generated method stub
              }
          }).create();

          alertDialog.show();
      }
      public static int getCurrentArrangeType (Context context){
          int currentValue = Settings.System.getInt(context.getContentResolver(), SETTINGS_ARRANGE_TYPE,SETTINGS_ARRANGE_TYPE_LIST);
          return currentValue;
      }
}
