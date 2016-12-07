package com.android.deskclock;

import hwdroid.app.HWListActivity;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import hwdroid.widget.ItemCursorAdapter;
import hwdroid.widget.FooterBar.FooterBarButton;
import hwdroid.widget.FooterBar.FooterBarType.OnFooterItemClick;
import hwdroid.widget.item.Item;
import hwdroid.widget.item.Text2Item;
import hwdroid.widget.item.Item.Type;
import hwdroid.widget.itemview.ItemView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.android.deskclock.R.string;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CityObj;

import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.hardware.Camera.Size;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;

public class DeleteClockActivity extends HWListActivity {
    private ItemCursorAdapter mAdapter;
    private Cursor mCursor;
    private List<CityObj> mUserSelectedCities;
    private List<String> mUserSelectedKeys;
    private HashMap<String, CityObj> citiesSelectedResult;
    private ListView mList;
    private static final String[] PROJECT = {
            "_id", "TEXT", "SUBTEXT"
    };

    private CheckBox mAllCheckBox;
    private boolean mIsSwitchChecked = true;
    private FooterBarButton mFooterBarButton;
    private int mButtonItemId = 0;

    private String mOK ;
    private String mCancel;
    private String mAMString;
    private String mPMString;
    private String mSelectedString;
    private Context mContext;
    private AudioManager am;
    private HashMap<String, String> mCityNameMap = new HashMap<String, String>();
    private CityObj[] mCities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        //mUserSelectedCities = Alarm.getAlarms(getContentResolver(), null, null);
        mUserSelectedCities = new ArrayList<CityObj>();
        mUserSelectedKeys = new ArrayList<String>();
        hashmapToList(Cities.readCitiesFromSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(this)));
        // Load the cities from xml.
        mCities = Utils.loadCitiesFromXml(this);

        // Reload the city name map with the recently parsed city names of the currently
        // selected language for use with selected cities.
        mCityNameMap.clear();
        for (CityObj city : mCities) {
            mCityNameMap.put(city.mCityId, city.mCityName);
        }
        // Re-organize the selected cities into an array.
        for (CityObj city : mUserSelectedCities) {
            String newCityName = mCityNameMap.get(city.mCityId);
            if (newCityName != null) {
                city.mCityName = newCityName;
            }
        }

        showBackKey(true);
        mCursor = createDummyCursor();
        mAdapter = new DeleteItemCursorAdapter(this, mCursor, Item.Type.CHECK_MODE);
        mOK = getString(R.string.add_alarm_page_confirm);
        mCancel = getString(R.string.add_alarm_page_cacnel);
        mSelectedString = getString(R.string.alarms_selected);
        mAMString = getString(R.string.clock_am);
        mPMString = getString(R.string.clock_pm);
        setListAdapter(mAdapter);
        mList = getListView();
        mAllCheckBox = new CheckBox(this);
        mAllCheckBox.setClickable(false);

        this.setRightWidgetView(mAllCheckBox);
        this.setRightWidgetClickListener(new OnRightWidgetItemClick() {

            @Override
            public void onRightWidgetItemClick() {
                TA.getInstance().getDefaultTracker().commitEvent("Page_DeleteClocks",
                        2101, "Button_SelectAll", null, null, null);
                mAllCheckBox.setChecked(!mAllCheckBox.isChecked());
                mAdapter.doAllSelected(mAllCheckBox.isChecked());
                mAdapter.notifyDataSetChanged();
                setTitle2(String.format(mSelectedString, mAdapter.getSelectedCounts()));

            }});
        mFooterBarButton = new FooterBarButton(this);
        mFooterBarButton.setOnFooterItemClick(new OnFooterItemClick() {
            @Override
            public void onFooterItemClick(View view, int id) {
                switch(id) {
                    case 0:
                        finish();
                        break;
                    case 1:
                        if (null != mUserSelectedCities && null != mUserSelectedKeys) {
                            Iterator<CityObj> itr = mUserSelectedCities.iterator();
                            Iterator<String> itrString = mUserSelectedKeys.iterator();
                            int currentIndex = 0;
                            while (itr.hasNext() && itrString.hasNext()) {
                                CityObj i = itr.next();
                                String s = itrString.next();
                                if (mAdapter.getSelectedStatus(currentIndex)) {
                                    itr.remove();
                                    itrString.remove();
                                }
                                currentIndex++;
                            }
                        }
                        break;
                    default:
                        break;
                }
                finish();
            }});
        
        mFooterBarButton.addItem(mButtonItemId++, mCancel);
        mFooterBarButton.addItem(mButtonItemId++, mOK);

        mFooterBarButton.updateItems(); 
        
        getFooterBarImpl().addView(mFooterBarButton);   
        getFooterBarImpl().setVisibility(View.VISIBLE);
        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null == TA.getInstance() || null == TA.getInstance().getDefaultTracker()) {
            StatConfig.getInstance().setContext(this.getApplicationContext());
            StatConfig.getInstance().turnOnDebug();
            Tracker lTracker = TA.getInstance().getTracker("21736479");
            lTracker.setAppKey("21736479");
            TA.getInstance().setDefaultTracker(lTracker);
        }
        TA.getInstance().getDefaultTracker().pageEnter("Page_DeleteClockActivity");
    }

    private void hashmapToList(HashMap<String, CityObj> map) {
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            mUserSelectedKeys.add(key);
            mUserSelectedCities.add(map.get(key));
        }
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        TA.getInstance().getDefaultTracker().pageLeave("Page_DeleteClockActivity_leave");
        citiesSelectedResult = listToHashmap(mUserSelectedKeys, mUserSelectedCities);
        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()),
                citiesSelectedResult);
        BackupManager.dataChanged(mContext.getPackageName());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUserSelectedCities.clear();
        mUserSelectedKeys.clear();
        citiesSelectedResult.clear();
        mCityNameMap.clear();
        mCities = null;
    }

    private HashMap<String, CityObj> listToHashmap(List<String> listkey, List<CityObj> listValue) {
        HashMap<String, CityObj> result = new HashMap<String, CityObj>();
        for(int i = 0; i < listkey.size(); i++) {
            result.put(listkey.get(i), listValue.get(i));
        }
        return result;
    }

    private Cursor createDummyCursor() {
        MatrixCursor cursor = new MatrixCursor(PROJECT);
        for(int i = 0; i < mUserSelectedCities.size(); i++) {
            cursor.addRow(new Object[]{String.valueOf(i),
                     mUserSelectedCities.get(i).mCityName,
                     null});
        }
        return cursor;
    }

    private class DeleteItemCursorAdapter extends ItemCursorAdapter {
        public DeleteItemCursorAdapter(Context context, Cursor c, Type type) {
            super(context, c, type);

        }

        @Override
        public Item setupItem(Context context, Cursor cursor) {
            return new Text2Item(cursor.getString(1), cursor.getString(2));
        }
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        ItemView view = (ItemView) v;

        Item item = (Item) v.getTag();
        item.setChecked(!item.isChecked());
        view.setObject(item);
        ItemCursorAdapter adapter = (ItemCursorAdapter)l.getAdapter();
        adapter.setSelectedItem(position);
        
        setTitle2(String.format(mSelectedString, adapter.getSelectedCounts()));
        
        if(adapter.getSelectedCounts() == adapter.getCount()) {
            if(!mAllCheckBox.isChecked()) {
                mIsSwitchChecked = false;
                mAllCheckBox.setChecked(true);
                
            }
        } else {
            if(mAllCheckBox.isChecked()) {
                mIsSwitchChecked = false;
                mAllCheckBox.setChecked(false);
                
            }
        }
    }
}
