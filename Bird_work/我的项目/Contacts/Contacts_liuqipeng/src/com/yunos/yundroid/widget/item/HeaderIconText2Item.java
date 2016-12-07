package com.yunos.yundroid.widget.item;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.itemview.ItemView;

public class HeaderIconText2Item extends HeaderText2Item {

    public ImageView mIcon;
    
    public HeaderIconText2Item() {
    	super();
    }
    
    public HeaderIconText2Item(ImageView icon, String headText, String text,String subTitle) {
        super(headText, text, subTitle);
        mIcon = icon;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
    	ItemView view = createCellFromXml(context, R.layout.gd_header_icon_text2_item_view, parent);
    	view.prepareItemView();
    	return view;
    }
}
