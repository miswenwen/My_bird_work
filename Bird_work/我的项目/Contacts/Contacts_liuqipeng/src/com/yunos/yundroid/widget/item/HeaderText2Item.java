package com.yunos.yundroid.widget.item;

import android.content.Context;
import android.view.ViewGroup;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.itemview.ItemView;

public class HeaderText2Item extends SubtextItem {

    public String mHeaderText;
    private ItemView mView;
    
    public HeaderText2Item() {
    	super();
    }
    
    public HeaderText2Item(String headText, String text,String subTitle) {
        super(text, subTitle);
        mHeaderText = headText;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
    	mView = createCellFromXml(context, R.layout.gd_header_text2_item_view, parent);
    	mView.prepareItemView();
    	return mView;
    }
    
    public ItemView getView() {
    	return mView; 
    }
}

