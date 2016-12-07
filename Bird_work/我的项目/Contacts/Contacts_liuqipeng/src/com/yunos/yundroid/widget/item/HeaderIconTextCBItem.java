package com.yunos.yundroid.widget.item;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.itemview.ItemView;

public class HeaderIconTextCBItem extends HeaderIconTextItem {
	
	public boolean mCheckBox;
	
    public HeaderIconTextCBItem() {
    	super();
    }
    
    public HeaderIconTextCBItem(ImageView view, String headText, String text, boolean checkBox) {
        super(view, headText, text);
        mCheckBox = checkBox;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
    	ItemView view = createCellFromXml(context, R.layout.gd_header_icon_text_cb_item_view, parent);
    	view.prepareItemView();
    	return view;
    }

}
