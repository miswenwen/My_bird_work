package com.android.deskclock;

import java.util.List;
import java.util.Locale;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;

import hwdroid.app.HWListActivity;
import hwdroid.widget.ItemCursorAdapter;
import hwdroid.widget.ActionBar.ActionBarView.OnRightWidgetItemClick;
import hwdroid.widget.FooterBar.FooterBarButton;
import hwdroid.widget.FooterBar.FooterBarType.OnFooterItemClick;
import hwdroid.widget.item.Item;
import hwdroid.widget.item.Text2Item;
import hwdroid.widget.item.Item.Type;
import hwdroid.widget.itemview.ItemView;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.os.AsyncTask;
import com.android.deskclock.provider.AlarmInstance;


import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;

public class DeleteAlarmActivity extends HWListActivity {
    private ItemCursorAdapter mAdapter;
    private Cursor mCursor;
    private List<Alarm> allAlarms;
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
    private AudioManager am;
    private Context mContext;

    private boolean mDelete = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mAMString = getString(R.string.clock_am);
        mPMString = getString(R.string.clock_pm);
        String[] selectionArgs = {};
        allAlarms = Alarm.getAlarms(getContentResolver(), null, selectionArgs);
        mDelete = false;
        showBackKey(true);
        mCursor = createDummyCursor();
        mAdapter = new DeleteItemCursorAdapter(this, mCursor, Item.Type.CHECK_MODE);
        mOK = getString(R.string.add_alarm_page_confirm);
        mCancel = getString(R.string.add_alarm_page_cacnel);
        mSelectedString = getString(R.string.alarms_selected);
        setListAdapter(mAdapter);
        mList = getListView();
        mAllCheckBox = new CheckBox(this);
        mAllCheckBox.setClickable(false);

        this.setRightWidgetView(mAllCheckBox);
        this.setRightWidgetClickListener(new OnRightWidgetItemClick() {

            @Override
            public void onRightWidgetItemClick() {
                TA.getInstance().getDefaultTracker().commitEvent("Page_DeleteAlarms",
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
                        //delete();
                        mDelete = true;
                        final AsyncTask<Void, Void, AlarmInstance> deleteTask = new AsyncTask<Void, Void, AlarmInstance>() {
                            @Override
                            protected AlarmInstance doInBackground(Void... parameters) {
                                for(int i = 0; i< allAlarms.size(); i++) {
                                    if (mAdapter.getSelectedStatus(i)){
                                        AlarmStateManager.deleteAllInstances(mContext, allAlarms.get(i).id);
                                        Alarm.deleteAlarm(mContext.getContentResolver(), allAlarms.get(i).id);
                                    }
                                }
                                allAlarms.clear();
                                return null;
                            }
                        };
                        deleteTask.execute();
                        finish();
                        break;
                    default:
                        break;
                }
            }});

        mFooterBarButton.addItem(mButtonItemId++, mCancel);
        mFooterBarButton.addItem(mButtonItemId++, mOK);

        mFooterBarButton.updateItems();

        getFooterBarImpl().addView(mFooterBarButton);
        getFooterBarImpl().setVisibility(View.VISIBLE);
        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onResume(){
        super.onResume();
        if (null == TA.getInstance() || null == TA.getInstance().getDefaultTracker()) {
            StatConfig.getInstance().setContext(this.getApplicationContext());
            StatConfig.getInstance().turnOnDebug();
            Tracker lTracker = TA.getInstance().getTracker("21736479");
            lTracker.setAppKey("21736479");
            TA.getInstance().setDefaultTracker(lTracker);
        }
        TA.getInstance().getDefaultTracker().pageEnter("Page_DeleteAlarmActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();
        TA.getInstance().getDefaultTracker().pageLeave("Page_DeleteAlarmActivity_leave");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mDelete) {
            allAlarms.clear();
        }
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    private Cursor createDummyCursor() {
        MatrixCursor cursor = new MatrixCursor(PROJECT);
        for(int i = 0; i < allAlarms.size(); i ++) {
            cursor.addRow(new Object[]{String.valueOf(i),
                    createStringFromAlarm(allAlarms.get(i)),
                    allAlarms.get(i).label
                     + " " + allAlarms.get(i).daysOfWeek.toString(getApplicationContext(), false)});
        }
        return cursor;
    }

    private String createStringFromAlarm(Alarm alarm) {
        String hour = null;
        String minute = null;
        String result = "";
        if (!DateFormat.is24HourFormat(getApplicationContext())) {
            if (alarm.hour < 12) {
                result = mAMString.toUpperCase();
            } else {
                result = mPMString.toUpperCase();
            }

            if (alarm.hour > 12) {
                alarm.hour = alarm.hour - 12;
            }

            if (alarm.hour == 0) {
                alarm.hour = 12;
            }
        }

        hour = (alarm.hour < 10 ? "0"+ String.valueOf(alarm.hour) : String.valueOf(alarm.hour));
        minute = (alarm.minutes < 10 ? "0" + String.valueOf(alarm.minutes) : String.valueOf(alarm.minutes));

        if (Locale.getDefault().getCountry().equals("US")) {
            result = hour + " : " + minute +  " " + result;
        } else {
            result = result +  " " + hour + " : " + minute;
        }

        return result;
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
