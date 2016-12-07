package com.yunos.yundroid.widget.item;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.itemview.ItemView;

public class HeaderIconText2CBItem extends HeaderIconText2Item {
	
	public boolean mCheckBox;
	
    public HeaderIconText2CBItem() {
    	super();
    }
    
    public HeaderIconText2CBItem(ImageView view, String headText, String text,String subTitle, boolean checkBox) {
        super(view, headText, text, subTitle);
        mCheckBox = checkBox;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
    	ItemView view = createCellFromXml(context, R.layout.gd_header_icon_text2_cb_item_view, parent);
    	view.prepareItemView();
    	return view;
    }

}
