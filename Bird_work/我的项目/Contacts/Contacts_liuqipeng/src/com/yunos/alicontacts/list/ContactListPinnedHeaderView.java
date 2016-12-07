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

package com.yunos.alicontacts.list;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;

/**
 * A custom view for the pinned section header shown at the top of the contact list.
 */
public class ContactListPinnedHeaderView extends LinearLayout {
	
	public static ContactListPinnedHeaderView newView(Context context) {
		 return (ContactListPinnedHeaderView)LayoutInflater.from(context).inflate(R.layout.contact_list_pinned_header_view, null, false);
	}

    protected final Context mContext;   
    
    TextView mHeaderTextView;

    public ContactListPinnedHeaderView(Context context) {
        this(context, null);
    }
    public ContactListPinnedHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    
    public void prepareView() {
        mHeaderTextView = (TextView)this.findViewById(R.id.xx_separator_text);    	
    }

    /**
     * Sets section header or makes it invisible if the title is null.
     */
    public void setSectionHeader(String title) {
        if (!TextUtils.isEmpty(title)) {
        	if(!title.equals(mHeaderTextView.getText())) {
        	    mHeaderTextView.setText(title);
        	    mHeaderTextView.setVisibility(View.VISIBLE);
        	}
        } else {
        	mHeaderTextView.setVisibility(View.GONE);
        }
    }

    public void setCountView(String count) {
    }
}
