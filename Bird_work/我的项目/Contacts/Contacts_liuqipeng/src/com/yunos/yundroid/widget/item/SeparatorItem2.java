package com.yunos.yundroid.widget.item;

import android.content.Context;
import android.view.ViewGroup;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.itemview.ItemView;

public class SeparatorItem2 extends TextItem {

	/**
	 * @hide
	 */
	public SeparatorItem2() {
		this(null);
	}

	/**
	 * Construct a SeparatorItem made of the given text
	 * 
	 * @param text
	 *            The text for this SeparatorItem
	 */
	public SeparatorItem2(String text) {
		super(text);
		enabled = false;
	}

	@Override
	public ItemView newView(Context context, ViewGroup parent) {
		ItemView view = createCellFromXml(context,
				R.layout.gd_separator_item_view2, parent);
		view.prepareItemView();
		return view;
	}

}
