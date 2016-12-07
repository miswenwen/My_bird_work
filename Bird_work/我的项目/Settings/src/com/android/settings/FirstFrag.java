/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;
import java.util.ArrayList;
import java.util.List;
import android.view.View.OnClickListener;
import android.net.ConnectivityManager;
import android.app.Fragment;
import android.util.Log;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.content.res.Resources;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.notification.NotificationSettings;
import com.android.settings.Settings.NotificationSettingsActivity;
import com.android.settings.SecuritySettings;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.Settings.InputMethodAndLanguageSettingsActivity;
import com.android.settings.DateTimeSettings;
import com.android.settings.Settings.DateTimeSettingsActivity;
public class FirstFrag extends Fragment{

	private WifiManager wifiManager;
	private Switch wlanSwitch;
	private Switch bluetoothSwitch;
	private BluetoothAdapter bluetoothAdapter;
	private	View view;
	private ArrayList<ListItem> mList=new ArrayList<ListItem>();
	private IntentFilter intentFilter;
	private BluetoothChangeReceiver bluetoothChangeReceiver;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {	
		view=inflater.inflate(R.layout.page_one, container, false);
		initWlan();
		initBlueTooth();
		intentFilter = new IntentFilter();
		bluetoothChangeReceiver=new BluetoothChangeReceiver();//注意要实例化，用之前
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		getActivity().registerReceiver(bluetoothChangeReceiver, intentFilter);
		initArrayListData();
		ListView mListView=(ListView)view.findViewById(R.id.set_list);
		MyAdapter mAdapter=new MyAdapter(getActivity(),R.layout.first_frag_item,mList);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
				Intent intent=null;
				switch (position) {
				case 0:
				   intent=new Intent(getActivity(),WifiSettings.class); 
				   startActivity(intent);
					break;
				case 1:
				   intent=new Intent(getActivity(),BluetoothSettings.class); 
				   startActivity(intent);				
					break;
				case 2:
				   intent=new Intent(getActivity(),DisplaySettings.class); 
				   startActivity(intent);			
					break;
				case 3:
				   intent=new Intent(getActivity(),NotificationSettingsActivity.class); 
				   startActivity(intent);			
					break;
				case 4:
				   intent=new Intent(getActivity(),SecuritySettings.class); 
				   startActivity(intent);			
					break;
				case 5:
				   intent=new Intent(getActivity(),InputMethodAndLanguageSettingsActivity.class); 
				   startActivity(intent);			
					break;
				case 6:
				   intent=new Intent(getActivity(),DateTimeSettingsActivity.class); 
				   startActivity(intent);			
					break;
				default:
					break;
				}
			}
		});

		/*final ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
		viewTreeObserver.addOnWindowFocusChangeListener(new ViewTreeObserver.OnWindowFocusChangeListener() {
			@Override
			public void onWindowFocusChanged(final boolean hasFocus) {
				if(hasFocus){
					wlanSwitch.setChecked(wifiManager.isWifiEnabled());
					bluetoothSwitch.setChecked(bluetoothAdapter.isEnabled());
				}
			}
		});*/
		return view;	
	}
	private void initWlan(){
		wlanSwitch=(Switch)view.findViewById(R.id.wlan_switch);
		wifiManager=(WifiManager)(getActivity().getSystemService(Context.WIFI_SERVICE));
		wlanSwitch.setChecked(wifiManager.isWifiEnabled());
		wlanSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				wifiManager.setWifiEnabled(isChecked);  			
			}
		});
	}
	private void initBlueTooth(){
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		bluetoothSwitch=(Switch)view.findViewById(R.id.bluetooth_switch);
		bluetoothSwitch.setChecked(bluetoothAdapter.isEnabled());
		bluetoothSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					bluetoothAdapter.enable();  
				}
				else{
					bluetoothAdapter.disable();
				}			
			}
		});
	}
	private void initArrayListData(){
	Resources res=getResources();
	mList.add(new ListItem(res.getString(R.string.wifi_settings_title),R.drawable.ic_settings_wireless));
	mList.add(new ListItem(res.getString(R.string.bluetooth_settings_title),R.drawable.ic_settings_bluetooth));
	mList.add(new ListItem(res.getString(R.string.display_settings),R.drawable.ic_settings_display));
	mList.add(new ListItem(res.getString(R.string.notification_settings),R.drawable.ic_settings_notifications));
	mList.add(new ListItem(res.getString(R.string.security_settings_title),R.drawable.ic_settings_security));
	mList.add(new ListItem(res.getString(R.string.language_settings),R.drawable.ic_settings_language));	
	mList.add(new ListItem(res.getString(R.string.date_and_time_settings_title),R.drawable.ic_settings_date_time));		
	}
	class ListItem{
		private String name;
		private int imageId;
		public ListItem(String name, int imageId) {
		this.name = name;
		this.imageId = imageId;
		}
		public String getName() {
		return name;
		}
		public int getImageId() {
		return imageId;
		}
	
	}
	class MyAdapter extends ArrayAdapter<ListItem> {
		private int resourceId;
		public MyAdapter(Context context, int textViewResourceId,
		List<ListItem> objects) {
		super(context, textViewResourceId, objects);
		resourceId = textViewResourceId;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
		ListItem item = getItem(position); 
		View view = LayoutInflater.from(getContext()).inflate(resourceId, null);
		ImageView itemImage = (ImageView) view.findViewById(R.id.iv_item);
		TextView itemName = (TextView) view.findViewById(R.id.tv_item);
		itemImage.setImageResource(item.getImageId());
		itemName.setText(item.getName());
		return view;
		}
	}
	class BluetoothChangeReceiver extends BroadcastReceiver {
	@Override
		public void onReceive(Context context, Intent intent) {
			if((intent.getAction()).equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
				if(bluetoothAdapter.getState()==BluetoothAdapter.STATE_TURNING_ON){
					bluetoothSwitch.setChecked(true);
				}
				else if(bluetoothAdapter.getState()==BluetoothAdapter.STATE_TURNING_OFF){
					bluetoothSwitch.setChecked(false);
				}
			}
			else{
				//wlanSwitch.setChecked(wifiManager.isWifiEnabled());
				//Log.e("wifiliuqipeng",""+wifiManager.isWifiEnabled());
				//Toast.makeText(getActivity(),""+wifiManager.isWifiEnabled(),0).show();
				if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING)
				wlanSwitch.setChecked(true);
				else if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING){
				wlanSwitch.setChecked(false);
				}
			}
		}
	}
	public void onDestroy() {
	super.onDestroy();
	getActivity().unregisterReceiver(bluetoothChangeReceiver);
	}



}
