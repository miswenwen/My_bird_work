/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.deskclock.worldclock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextClock;
import android.widget.TextView;
import android.os.AsyncTask;

import com.android.deskclock.AnalogClock;
import com.android.deskclock.DeleteClockActivity;
import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.aliyun.ams.ta.TA;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import com.android.deskclock.AlarmFeatureOption;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import android.graphics.Typeface;
import android.app.backup.BackupManager;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.graphics.Color;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionSheet;
import hwdroid.widget.ActionSheet.ActionButton;

public class WorldClockAdapter extends BaseAdapter {
    protected Object [] mCitiesList;
    protected Object [] mCitiesList_bak;
    private Typeface mDinproType;
    private final LayoutInflater mInflater;
    private final Context mContext;
    private final Collator mCollator = Collator.getInstance();
    protected HashMap<String, CityObj> mCitiesDb = new HashMap<String, CityObj>();
    protected HashMap<String, CityObj> mDefaultHashmap = new HashMap<String, CityObj>();
    private int mClocksPerRow;
    private List<CityObj> mUserSelectedCities;
    private List<String> mUserSelectedKeys;
    private HashMap<String, CityObj> citiesSelectedResult;
    private ListView mListView;
    private boolean mDelete;

    public WorldClockAdapter(Context context, ListView listview) {
        super();
        mContext = context;
        loadData(context);
        loadCitiesDb(context);
        mInflater = LayoutInflater.from(context);
        mClocksPerRow = context.getResources().getInteger(R.integer.world_clocks_per_row);
        mDinproType = Typeface.createFromAsset(context.getAssets(), "fonts/DINPro/DINPro-Light.otf");
        mListView = listview;
        mUserSelectedCities = new ArrayList<CityObj>();
        mUserSelectedKeys = new ArrayList<String>();
        mDelete = false;
    }

    private OnShowOrHideListViewListener mOnShowOrHideListViewListener;
    public interface OnShowOrHideListViewListener{
        void onShowOrHideListView();
    }

    public void setOnShowOrHideListViewListener(OnShowOrHideListViewListener lisener){
        mOnShowOrHideListViewListener = lisener;
    }

    private void hashmapToList(HashMap<String, CityObj> map) {
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            mUserSelectedKeys.add(key);
            mUserSelectedCities.add(map.get(key));
        }
    }

    private HashMap<String, CityObj> listToHashmap(List<String> listkey, List<CityObj> listValue) {
        HashMap<String, CityObj> result = new HashMap<String, CityObj>();
        for(int i = 0; i < listkey.size(); i++) {
            result.put(listkey.get(i), listValue.get(i));
        }
        return result;
    }

    public void reloadData(Context context) {
        loadData(context);
        notifyDataSetChanged();
    }

    private void showorHideClock() {
        if (mOnShowOrHideListViewListener != null) {
            mOnShowOrHideListViewListener.onShowOrHideListView();
        }
    }

    public void loadData(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        //boolean isFirstTimeloaded = prefs.getBoolean("isFirstTimeLoaded", true);
        //mClockStyle = prefs.getString(SettingsActivity.KEY_CLOCK_STYLE,
        //        mContext.getResources().getString(R.string.default_clock_style));
        mCitiesList = Cities.readCitiesFromSharedPrefs(prefs).values().toArray();
        /*if(isFirstTimeloaded){
            //TODO penglei : add the 2 cities for default.
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isFirstTimeLoaded", false);
            prefs.edit();
            editor.apply();
            CityObj[] cities = Utils.loadCitiesFromXml(context);
            mCitiesList = new Object[2];
            mDefaultHashmap.clear();
            for (CityObj city : cities) {
                // cityId for NewYork
                if(city.mCityId.equals("C178")){
                    mCitiesList[0] = (Object)city;
                    mDefaultHashmap.put(city.mCityId, city);
                }
                // cityId for london
                if(city.mCityId.equals("C78")) {
                    mCitiesList[1] = (Object)city;
                    mDefaultHashmap.put(city.mCityId, city);
                }
            }

            Cities.saveCitiesToSharedPrefs(prefs,
                    mDefaultHashmap);
        }*/
        mCitiesList_bak = mCitiesList.clone();
        sortList();
        mCitiesList = addHomeCity();
    }

    public void loadCitiesDb(Context context) {
        mCitiesDb.clear();
        // Read the cities DB so that the names and timezones will be taken from the DB
        // and not from the selected list so that change of locale or changes in the DB will
        // be reflected.
        CityObj[] cities = Utils.loadCitiesFromXml(context);
        if (cities != null) {
            for (int i = 0; i < cities.length; i ++) {
                mCitiesDb.put(cities[i].mCityId, cities [i]);
            }
        }
    }

    /***
     * Adds the home city as the first item of the adapter if the feature is on and the device time
     * zone is different from the home time zone that was set by the user.
     * return the list of cities.
     */
    private Object[] addHomeCity() {
        if (needHomeCity()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String homeTZ = sharedPref.getString(SettingsActivity.KEY_HOME_TZ, "");
            CityObj c = new CityObj(
                    mContext.getResources().getString(R.string.home_label), homeTZ, null);
            Object[] temp = new Object[mCitiesList.length + 1];
            temp[0] = c;
            for (int i = 0; i < mCitiesList.length; i++) {
                temp[i + 1] = mCitiesList[i];
            }
            return temp;
        } else {
            return mCitiesList;
        }
    }

    public void updateHomeLabel(Context context) {
        // Update the "home" label if the home time zone clock is shown
        if (needHomeCity() && mCitiesList.length > 0) {
            ((CityObj) mCitiesList[0]).mCityName =
                    context.getResources().getString(R.string.home_label);
        }
    }

    public boolean needHomeCity() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (sharedPref.getBoolean(SettingsActivity.KEY_AUTO_HOME_CLOCK, false)) {
            String homeTZ = sharedPref.getString(
                    SettingsActivity.KEY_HOME_TZ, TimeZone.getDefault().getID());
            final Date now = new Date();
            return TimeZone.getTimeZone(homeTZ).getOffset(now.getTime())
                    != TimeZone.getDefault().getOffset(now.getTime());
        } else {
            return false;
        }
    }

    public boolean hasHomeCity() {
        return (mCitiesList != null) && mCitiesList.length > 0
                && ((CityObj) mCitiesList[0]).mCityId == null;
    }

    private void sortList() {
        final Date now = new Date();

        // Sort by the Offset from GMT taking DST into account
        // and if the same sort by City Name
        Arrays.sort(mCitiesList, new Comparator<Object>() {
            private int safeCityNameCompare(CityObj city1, CityObj city2) {
                if (city1.mCityName == null && city2.mCityName == null) {
                    return 0;
                } else if (city1.mCityName == null) {
                    return -1;
                } else if (city2.mCityName == null) {
                    return 1;
                } else {
                    return mCollator.compare(city1.mCityName, city2.mCityName);
                }
            }

            @Override
            public int compare(Object object1, Object object2) {
                CityObj city1 = (CityObj) object1;
                CityObj city2 = (CityObj) object2;
                if (city1.mTimeZone == null && city2.mTimeZone == null) {
                    return safeCityNameCompare(city1, city2);
                } else if (city1.mTimeZone == null) {
                    return -1;
                } else if (city2.mTimeZone == null) {
                    return 1;
                }

                int gmOffset1 = TimeZone.getTimeZone(city1.mTimeZone).getOffset(now.getTime());
                int gmOffset2 = TimeZone.getTimeZone(city2.mTimeZone).getOffset(now.getTime());
                if (gmOffset1 == gmOffset2) {
                    return safeCityNameCompare(city1, city2);
                } else {
                    return gmOffset1 - gmOffset2;
                }
            }
        });
    }

    @Override
    public int getCount() {
        if (mClocksPerRow == 1) {
            // In the special case where we have only 1 clock per view.
            return mCitiesList.length;
        }

        // Otherwise, each item in the list holds 1 or 2 clocks
        return (mCitiesList.length  + 1);
    }

    @Override
    public Object getItem(int p) {
        return null;
    }

    @Override
    public long getItemId(int p) {
        return p;
    }

    @Override
    public boolean isEnabled(int p) {
        return false;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        // Index in cities list
        final int index = position * mClocksPerRow;
        if (index < 0 || index >= mCitiesList.length) {
            return null;
        }
        if (view == null) {
            view = mInflater.inflate(R.layout.world_clock_list_item, parent, false);
        }
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TA.getInstance().getDefaultTracker().commitEvent("Page_ClockFragment",2101, "Page_ClockFragment_Button_DeleteClock", null, null, null);
                if(mDelete) {
                   return false;
                }
                ActionSheet actionSheet = new ActionSheet(mContext);
                ArrayList<String> items = new ArrayList<String>();
                items.add(mContext.getResources().getString(R.string.diag_delete));
                actionSheet.setCommonButtons(items);
                actionSheet.setCommonButtonListener(new ActionSheet.CommonButtonListener() {
                    @Override
                    public void onClick(int which) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setMessage(mContext.getResources().getString(R.string.clock_delete_msg));
                        builder.setPositiveButton(mContext.getResources().getString(R.string.diag_remove),
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final AsyncTask<Void, Void, String> deleteTask = new AsyncTask<Void, Void, String>() {
                                    @Override
                                    protected String doInBackground(Void... parameters) {
                                        mDelete = true;
                                        mUserSelectedCities.clear();
                                        mUserSelectedKeys.clear();
                                        hashmapToList(Cities.readCitiesFromSharedPrefs(
                                        PreferenceManager.getDefaultSharedPreferences(mContext)));
                                        for(int i = 0; i < mCitiesList_bak.length; i++)
                                        {
                                            if(((CityObj)mCitiesList_bak[i]).mCityId == ((CityObj)mCitiesList[index]).mCityId)
                                            {
                                                mUserSelectedCities.remove(i);
                                                mUserSelectedKeys.remove(i);
                                                break;
                                            }
                                        }
                                        citiesSelectedResult = listToHashmap(mUserSelectedKeys, mUserSelectedCities);
                                        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(mContext),citiesSelectedResult);
                                        BackupManager.dataChanged(mContext.getPackageName());
                                        return null;
                                    }
                                    @Override
                                    protected void onPostExecute(String result) {
                                        mUserSelectedCities.clear();
                                        mUserSelectedKeys.clear();
                                        reloadData(mContext);
                                        mDelete = false;
                                        showorHideClock();
                                    }
                                };
                                deleteTask.execute();
                            }
                        });
                        builder.setNegativeButton(mContext.getResources().getString(R.string.add_alarm_page_cacnel), null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                mContext.getResources().getColor(R.color.text_color_warning_selector));
                    }
                    @Override
                    public void onDismiss(ActionSheet arg0) {
                    }
                });
                actionSheet.show(mListView);
                return true;
            }
        });
        // The world clock list item can hold two world clocks
        //View rightClock = view.findViewById(R.id.city_right);
        updateView(view.findViewById(R.id.city_left), (CityObj)mCitiesList[index]);
        /*if (rightClock != null) {
            // rightClock may be null (landscape phone layout has only one clock per row) so only
            // process it if it exists.
            if (index + 1 < mCitiesList.length) {
                rightClock.setVisibility(View.VISIBLE);
                updateView(rightClock, (CityObj)mCitiesList[index + 1]);
            } else {
                // To make sure the spacing is right , make sure that the right clock style is
                // selected even if the clock is invisible.
                View dclock = rightClock.findViewById(R.id.digital_clock);
                View aclock = rightClock.findViewById(R.id.analog_clock);
                if (mClockStyle.equals("analog")) {
                    dclock.setVisibility(View.GONE);
                    aclock.setVisibility(View.INVISIBLE);
                } else {
                    dclock.setVisibility(View.INVISIBLE);
                    aclock.setVisibility(View.GONE);
                }
                // If there's only the one item, center it. If there are other items in the list,
                // keep it side-aligned.
                rightClock.setVisibility(View.GONE);
                if (getCount() == 1) {
                    rightClock.setVisibility(View.GONE);
                } else {
                    rightClock.setVisibility(View.INVISIBLE);
                }
            }
        }*/
        return view;
    }
    
    private void deleteClock() {
        mContext.startActivity(new Intent(mContext.getApplicationContext(), DeleteClockActivity.class));
    }

    private void updateView(View clock, CityObj cityObj) {
        View nameLayout= clock.findViewById(R.id.city_name_layout);
        TextView name = (TextView)(clock.findViewById(R.id.city_name));
        //TextView dayOfWeek = (TextView)(clock.findViewById(R.id.city_day));
        TextView date = (TextView)(clock.findViewById(R.id.date));
        TextClock dclock = (TextClock)(clock.findViewById(R.id.digital_clock));
        dclock.setTypeface(mDinproType);
        View separator = clock.findViewById(R.id.separator);

        dclock.setVisibility(View.VISIBLE);
        separator.setVisibility(View.VISIBLE);
        dclock.setTimeZone(cityObj.mTimeZone);
        Utils.setTimeFormat(dclock,
                (int)mContext.getResources().getDimension(R.dimen.label_font_size));

        CityObj cityInDb = mCitiesDb.get(cityObj.mCityId);
        // Home city or city not in DB , use data from the save selected cities list
        name.setText(Utils.getCityName(cityObj, cityInDb));

        final Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getDefault());
        //int myDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        // Get timezone from cities DB if available
        String cityTZ = (cityInDb != null) ? cityInDb.mTimeZone : cityObj.mTimeZone;
        now.setTimeZone(TimeZone.getTimeZone(cityTZ));
        //int cityDayOfWeek = now.get(Calendar.DAY_OF_WEEK);

        Date nowDate = new Date();
        final Locale l = Locale.getDefault();
        String fmt;
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            fmt = DateFormat.getBestDateTimePattern(l, "MMMMd");
        } else {
            fmt = "MMMM d";
            Log.i("getBestDateTimePattern3 for MMMMd" + fmt);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(fmt, l);
        sdf.setTimeZone(TimeZone.getTimeZone(cityTZ));
        date.setText(sdf.format(nowDate));
        date.setVisibility(View.VISIBLE);
        if (AlarmFeatureOption.YUNOS_MTK_PLATFORM) {
            fmt = DateFormat.getBestDateTimePattern(l, "MMMMd");
        } else {
            fmt = "MMMM d";
            Log.i("getBestDateTimePattern3 for MMMMd" + fmt);
        }

        sdf = new SimpleDateFormat(fmt, l);
        sdf.setTimeZone(TimeZone.getTimeZone(cityTZ));
        date.setContentDescription(sdf.format(nowDate));
        /*
        if (myDayOfWeek != cityDayOfWeek) {
            dayOfWeek.setText(now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
            date.setText(date.getText()+ " " + now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
            dayOfWeek.setVisibility(View.GONE);
        } else {
            dayOfWeek.setVisibility(View.GONE);
        }*/
    }
}
