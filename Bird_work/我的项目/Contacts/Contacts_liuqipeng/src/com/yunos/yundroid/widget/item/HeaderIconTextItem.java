package com.yunos.yundroid.widget.item;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.itemview.ItemView;

public class HeaderIconTextItem extends HeaderTextItem {

    public ImageView mIcon;
    
    public HeaderIconTextItem() {
    	super();
    }
    
    public HeaderIconTextItem(ImageView icon, String headText, String text) {
        super(headText, text);
        mIcon = icon;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
    	ItemView view = createCellFromXml(context, R.layout.gd_header_icon_text_item_view, parent);
    	view.prepareItemView();
    	return view;
    }
}
