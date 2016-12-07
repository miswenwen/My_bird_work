/*
 * Copyright (C) 2009 The Android Open Source Project
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


import android.widget.BaseAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;




public class GridViewAdapter extends BaseAdapter {
		private Context mContext;
		private CharSequence[] mItems;
        private int resourceId;
		private LayoutInflater mInflater;	
		public GridViewAdapter(Context context,int resource,CharSequence[] items) {
			mContext = context;
			resourceId=resource;
			mItems=items;
			mInflater=LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return 6;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View arg1, ViewGroup arg2) {
			View view=mInflater.inflate(resourceId, null);
		    Button mButton=(Button)view.findViewById(R.id.mark_button);
			mButton.setText(mItems[position]);		
			switch (position) {
			case 0:
				mButton.setBackground(mContext.getResources().getDrawable(
						R.drawable.contact_mark_dlg_haras_btn));			
				break;
			case 1:
				mButton.setBackground(mContext.getResources().getDrawable(
						R.drawable.contact_mark_dlg_fraud_btn));			
				break;
			case 2:
				mButton.setBackground(mContext.getResources().getDrawable(
						R.drawable.contact_mark_dlg_house_btn));
				break;
			case 3:
				mButton.setBackground(mContext.getResources().getDrawable(
						R.drawable.contact_mark_dlg_delivery_btn));
				break;
			case 4:
				mButton.setBackground(mContext.getResources().getDrawable(
						R.drawable.contact_mark_dlg_advertisment_btn));
				break;
			case 5:
				mButton.setBackground(mContext.getResources().getDrawable(
						R.drawable.contact_mark_dlg_custom_btn));
				break;
			default:
				break;
			}
			return view;
		}
}


