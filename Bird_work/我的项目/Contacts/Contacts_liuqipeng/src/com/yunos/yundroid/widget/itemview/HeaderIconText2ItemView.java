/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
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
package com.yunos.yundroid.widget.itemview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.item.HeaderIconText2Item;
import com.yunos.yundroid.widget.item.Item;

public class HeaderIconText2ItemView extends HeaderIconTextItemView {

    protected TextView mSubTitleView;

    public HeaderIconText2ItemView(Context context) {
        this(context, null);
    }

    public HeaderIconText2ItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prepareItemView() {
    	super.prepareItemView();
        mSubTitleView = (TextView) findViewById(R.id.gd_subtext);
    }

    public void setObject(Item object) {
        final HeaderIconText2Item item = (HeaderIconText2Item) object;
        setHeaderTextView(item.mHeaderText);
        setTextView(item.mText);
        setSubtextView(item.subtext);
        setIcon(item.mIcon);
    }  
   
    
    public void setSubtextView(String text) {
    	mSubTitleView.setText(text);
    }    

	@Override
	public void setCheckBox(boolean status) {
		// TODO Auto-generated method stub
		
	}    
}
