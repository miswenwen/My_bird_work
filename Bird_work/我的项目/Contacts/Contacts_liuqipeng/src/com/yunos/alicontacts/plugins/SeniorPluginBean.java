package com.yunos.alicontacts.plugins;

import com.yunos.alicontacts.model.Contact;

import android.content.Context;
import android.view.View;

public abstract class SeniorPluginBean extends PluginBean {

	protected View mView;
	protected Context mContext;
	protected Contact mData;

	public View getView() {
		return mView;
	}

	public void setView(View view) {
		this.mView = view;
	}

	public abstract void genView(Context context, Contact data);
}
