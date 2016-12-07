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
package com.yunos.yundroid.widget.item;

import android.content.Context;
import android.view.ViewGroup;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.itemview.ItemView;

public class HeaderTextItem extends TextItem {

    public String mHeaderText;
    
    public HeaderTextItem() {
    	super();
    }
    
    public HeaderTextItem(String text) {
        super(text);
    }   
    
    public HeaderTextItem(String headedText, String text) {
        super(text);
        mHeaderText = headedText;
    }
    
    public HeaderTextItem(String headedText, String text, boolean dividerVisible) {
        super(text);
        mHeaderText = headedText;
        mDividerVisible = dividerVisible;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
    	ItemView view = createCellFromXml(context, R.layout.gd_header_text_item_view, parent);
    	view.prepareItemView();
    	return view;
    }
}
